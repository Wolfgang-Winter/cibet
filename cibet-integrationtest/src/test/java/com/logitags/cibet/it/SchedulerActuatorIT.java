/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2012 Dr. Wolfgang Winter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************
 */
package com.logitags.cibet.it;

import java.io.File;
import java.util.List;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.cibethelper.base.DBHelper;
import com.cibethelper.ejb.RemoteEJB;
import com.cibethelper.ejb.RemoteEJBImpl;
import com.cibethelper.ejb.SimpleEjb;
import com.cibethelper.entities.AbstractTEntity;
import com.cibethelper.entities.ITComplexEntity;
import com.cibethelper.entities.TCompareEntity;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TComplexEntity2;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.actuator.scheduler.SchedulerActuator;
import com.logitags.cibet.actuator.scheduler.SchedulerLoader;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;

@RunWith(Arquillian.class)
public class SchedulerActuatorIT extends DBHelper {

   private static Logger log = Logger.getLogger(SchedulerActuatorIT.class);

   @Deployment(testable = false)
   public static WebArchive createDeployment() {
      String warName = SchedulerActuatorIT.class.getSimpleName() + ".war";

      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("it/web2.xml");

      archive.addClasses(AbstractTEntity.class, TEntity.class, TComplexEntity.class, TComplexEntity2.class,
            ITComplexEntity.class, TCompareEntity.class, RemoteEJB.class, RemoteEJBImpl.class, SimpleEjb.class);

      File[] cibet = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-jpa20").withTransitivity()
            .asFile();
      archive.addAsLibraries(cibet);

      archive.addAsWebInfResource("META-INF/persistence-it-derby.xml", "classes/META-INF/persistence.xml");
      archive.addAsWebInfResource("it/config_scheduler.xml", "classes/cibet-config.xml");
      archive.addAsWebInfResource("it/ejb-jar.xml", "ejb-jar.xml");

      log.debug(archive.toString(true));
      archive.as(ZipExporter.class).exportTo(new File("target/" + warName), true);

      return archive;
   }

   private InitialContext getInitialContext() throws NamingException {
      Properties props = new Properties();
      props.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
      props.put(javax.naming.Context.PROVIDER_URL, "remote://localhost:4447");
      props.put("jboss.naming.client.ejb.context", true);

      props.put(javax.naming.Context.SECURITY_PRINCIPAL, "Mutzi1");
      props.put(javax.naming.Context.SECURITY_CREDENTIALS, "passss1234!");
      return new InitialContext(props);
   }

   @Test
   public void testInvoke() throws Exception {
      log.debug("start testInvoke()");
      TEntity te = new TEntity("myName", 45, "winter");

      RemoteEJB remoteEjb = (RemoteEJB) getInitialContext()
            .lookup("SchedulerActuatorIT/RemoteEJBImpl!com.cibethelper.ejb.RemoteEJB");
      TEntity te2 = remoteEjb.persist(te);
      log.debug(te2);
      Assert.assertTrue(te2.getId() == 0);

      List<DcControllable> list = SchedulerLoader.findScheduled();
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("ANONYMOUS", list.get(0).getCreateUser());
      Query q = applEman.createQuery("SELECT e FROM TEntity e");
      List<TEntity> tlist = q.getResultList();
      Assert.assertEquals(0, tlist.size());

      log.debug("-------------------- sleep");
      Thread.sleep(8000);
      log.debug("--------------- after TimerTask");

      Context.internalRequestScope().getEntityManager().clear();

      list = SchedulerLoader.findScheduled();
      Assert.assertEquals(0, list.size());

      list = SchedulerLoader.loadByUser("ANONYMOUS");
      Assert.assertEquals(1, list.size());
      DcControllable co = list.get(0);
      Assert.assertEquals("SCHEDULER-1", co.getActuator());
      Assert.assertEquals(ExecutionStatus.EXECUTED, co.getExecutionStatus());
      Assert.assertNotNull(co.getExecutionDate());
      Assert.assertNotNull(co.getResource().getPrimaryKeyId());

      tlist = q.getResultList();
      Assert.assertEquals(1, tlist.size());
   }

   @Test
   public void executeEjbTimerEncrypt() throws Exception {
      log.info("start executeEjbTimerEncrypt()");

      TEntity entity = new TEntity();
      entity.setCounter(5);
      entity.setNameValue("valuexx");
      entity.setOwner("theTenant");
      applEman.persist(entity);
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      RemoteEJB ejb = (RemoteEJB) getInitialContext()
            .lookup("SchedulerActuatorIT/RemoteEJBImpl!com.cibethelper.ejb.RemoteEJB");

      entity.setCounter(45);
      entity.setOwner("Newman");
      TEntity ent = ejb.update(entity);

      applEman.clear();
      TEntity te = applEman.find(TEntity.class, entity.getId());
      Assert.assertEquals(5, te.getCounter());
      Assert.assertEquals("theTenant", te.getOwner());

      List<DcControllable> l = SchedulerLoader.findScheduled();
      Assert.assertEquals(1, l.size());
      DcControllable co = l.get(0);
      Assert.assertEquals(2, co.getResource().getParameters().size());
      log.debug("PARAM1-- " + co.getResource().getParameters().get(0).getName());
      log.debug("PARAM2-- " + co.getResource().getParameters().get(1).getName());
      Assert.assertTrue("owner".equals(co.getResource().getParameters().get(0).getName())
            || "owner".equals(co.getResource().getParameters().get(1).getName()));
      log.debug(co.getScheduledDate());
      Assert.assertNotNull(co.getScheduledDate());
      log.debug("-------------------- sleep");
      Thread.sleep(8000);
      log.debug("--------------- after TimerTask");

      l = (List<DcControllable>) Context.requestScope().getEntityManager().createQuery("SELECT d FROM DcControllable d")
            .getResultList();
      Assert.assertEquals(1, l.size());
      co = l.get(0);
      Assert.assertEquals(3, co.getResource().getParameters().size());
      Assert.assertEquals(SchedulerActuator.ORIGINAL_OBJECT, co.getResource().getParameters().get(2).getName());

      Assert.assertEquals("SCHEDULER-2", co.getActuator());
      Assert.assertEquals(ExecutionStatus.EXECUTED, co.getExecutionStatus());
      Assert.assertNotNull(co.getExecutionDate());
      Assert.assertNotNull(co.getResource().getPrimaryKeyId());

      applEman.clear();
      te = applEman.find(TEntity.class, Long.parseLong(co.getResource().getPrimaryKeyId()));
      Assert.assertNotNull(te);
      Assert.assertEquals("Newman", te.getOwner());
      Assert.assertEquals(45, te.getCounter());
   }

   @Test
   public void executeEjbTimerInvoke() throws Exception {
      log.info("start executeEjbTimerInvoke()");

      RemoteEJB ejb = (RemoteEJB) getInitialContext()
            .lookup("SchedulerActuatorIT/RemoteEJBImpl!com.cibethelper.ejb.RemoteEJB");

      TEntity entity = new TEntity();
      entity.setCounter(5);
      entity.setNameValue("valuexx");
      entity.setOwner("theTenant");
      EventResult ev1 = ejb.callTransitiveEjb(entity);
      Assert.assertNotNull(ev1);
      log.debug(ev1);
      log.debug("Eventresult: " + Context.requestScope().getExecutedEventResult());

      List<DcControllable> l = SchedulerLoader.findScheduled();
      Assert.assertEquals(1, l.size());
      DcControllable co = l.get(0);
      log.debug(co.getScheduledDate());
      log.debug(co.getCreateUser());
      Assert.assertNotNull(co.getScheduledDate());
      log.debug("-------------------- sleep");
      Thread.sleep(8000);
      log.debug("--------------- after TimerTask");

      l = SchedulerLoader.loadByUser("ANONYMOUS");
      Assert.assertEquals(1, l.size());
      co = l.get(0);
      Assert.assertEquals("SCHEDULER-2", co.getActuator());
      Assert.assertEquals(ExecutionStatus.EXECUTED, co.getExecutionStatus());
      Assert.assertNotNull(co.getExecutionDate());

      List<TEntity> list = (List<TEntity>) applEman.createQuery("SELECT t FROM TEntity t").getResultList();
      Assert.assertEquals(1, list.size());
   }

   @Test
   public void executeEjbTimerJPAQuery() throws Exception {
      log.info("start executeEjbTimerJPAQuery()");

      RemoteEJB ejb = (RemoteEJB) getInitialContext()
            .lookup("SchedulerActuatorIT/RemoteEJBImpl!com.cibethelper.ejb.RemoteEJB");

      String qn = "INSERT INTO CIB_TESTENTITY(ID, NAMEVALUE, COUNTER) VALUES (?,?,?)";
      EventResult ev = ejb.executeUpdateQuery(qn, 456, "Felix", 1222);
      log.debug(ev);

      List<DcControllable> l = SchedulerLoader.findScheduled();
      Assert.assertEquals(1, l.size());
      DcControllable co = l.get(0);
      log.debug(co.getScheduledDate());
      Assert.assertNotNull(co.getScheduledDate());
      log.debug("-------------------- sleep");
      Thread.sleep(8000);
      log.debug("--------------- after TimerTask");

      l = SchedulerLoader.loadByUser("ANONYMOUS");
      Assert.assertEquals(1, l.size());
      co = l.get(0);
      Assert.assertEquals("SCHEDULER-2", co.getActuator());
      Assert.assertEquals(ExecutionStatus.EXECUTED, co.getExecutionStatus());
      Assert.assertNotNull(co.getExecutionDate());

      List<TEntity> list = (List<TEntity>) applEman.createQuery("SELECT t FROM TEntity t").getResultList();
      Assert.assertEquals(1, list.size());
   }

}
