/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 *******************************************************************************
 */
package com.logitags.cibet.it;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.cibethelper.base.CoreTestBase;
import com.cibethelper.ejb.CibetTestEJB;
import com.cibethelper.ejb.RemoteEJB;
import com.cibethelper.ejb.RemoteEJBImpl;
import com.cibethelper.ejb.SecuredRemoteEJBImpl;
import com.cibethelper.ejb.SimpleEjb;
import com.cibethelper.entities.AbstractTEntity;
import com.cibethelper.entities.ITComplexEntity;
import com.cibethelper.entities.TCompareEntity;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TComplexEntity2;
import com.cibethelper.entities.TEntity;
import com.cibethelper.servlet.ArquillianTestServlet1;
import com.logitags.cibet.actuator.common.Actuator;
import com.logitags.cibet.actuator.lock.AlreadyLockedException;
import com.logitags.cibet.actuator.lock.LockActuator;
import com.logitags.cibet.actuator.lock.LockState;
import com.logitags.cibet.actuator.lock.LockedObject;
import com.logitags.cibet.actuator.lock.Locker;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.ConfigurationService;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;

@RunWith(Arquillian.class)
public class LockerIT extends AbstractArquillian {

   private static Logger log = Logger.getLogger(LockerIT.class);

   @EJB
   private CibetTestEJB ejb;

   @Deployment
   public static WebArchive createDeployment() {
      String warName = LockerIT.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("it/web2.xml");

      archive.addClasses(AbstractArquillian.class, CoreTestBase.class, AbstractTEntity.class, TEntity.class,
            TComplexEntity.class, TComplexEntity2.class, ITComplexEntity.class, TCompareEntity.class,
            CibetTestEJB.class, ArquillianTestServlet1.class, RemoteEJB.class, RemoteEJBImpl.class,
            SecuredRemoteEJBImpl.class, SimpleEjb.class);

      File[] cibet = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-jpa").withTransitivity()
            .asFile();
      archive.addAsLibraries(cibet);

      archive.addAsWebInfResource("META-INF/persistence-it.xml", "classes/META-INF/persistence.xml");
      archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

      log.debug(archive.toString(true));
      archive.as(ZipExporter.class).exportTo(new File("target/" + warName), true);

      return archive;
   }

   @Before
   public void beforeLockerIT() {
      log.debug("execute before()");
      new ConfigurationService().initialise();
      Context.start();
      Context.sessionScope().setUser(USER);
      Context.sessionScope().setTenant(TENANT);
      log.debug("end execute before()");
   }

   @After
   public void afterLockerIT() {
      Context.end();
   }

   @Test
   public void lockSameUser() throws Exception {
      log.info("start lockSameUser()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      registerSetpoint(CibetTestEJB.class, schemes, "testInvoke", ControlEvent.INVOKE, ControlEvent.RELEASE_INVOKE);

      LockedObject lo = ejb.lockMethodFromClass();
      Assert.assertNotNull(lo);

      TEntity entity = createTEntity(5, "valuexx");
      byte[] bytes = "Pausenclown".getBytes();
      List<Object> list = ejb.testInvoke("Hals", -34, 456, bytes, entity, new Long(43));
      Assert.assertNotNull(list);
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
   }

   @Test(expected = AlreadyLockedException.class)
   public void lockTwice() throws Exception {
      log.info("start lockTwice()");
      LockedObject lo = ejb.lockMethodFromClass();
      Assert.assertNotNull(lo);
      lo = ejb.lockMethodFromClass();
   }

   @Test
   public void lockOtherUser() throws Exception {
      log.info("start lockOtherUser()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      registerSetpoint(CibetTestEJB.class, schemes, "testInvoke", ControlEvent.INVOKE, ControlEvent.RELEASE_INVOKE);

      LockedObject lo = ejb.lockMethodFromClass();
      Assert.assertNotNull(lo);

      Context.sessionScope().setUser("Hans");

      TEntity entity = createTEntity(5, "valuexx");
      byte[] bytes = "Pausenclown".getBytes();
      List<Object> list = ejb.testInvoke("Hals", -34, 456, bytes, entity, new Long(43));
      Assert.assertNull(list);
      EventResult ev = Context.requestScope().getExecutedEventResult();
      log.debug("EventResult=" + ev);
      Assert.assertEquals(ExecutionStatus.DENIED, ev.getExecutionStatus());
   }

   @Test(expected = IllegalArgumentException.class)
   public void lockMethodFromInterface() throws Exception {
      log.info("start lockMethodFromInterface()");
      Locker.lock(Actuator.class, "getName", ControlEvent.INVOKE, "testremark");
   }

   @Test
   public void lockAutomaticRemove() throws Exception {
      log.info("start lockAutomaticRemove()");
      LockActuator la = (LockActuator) Configuration.instance().getActuator(LockActuator.DEFAULTNAME);
      la.setAutomaticLockRemoval(true);

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      registerSetpoint(CibetTestEJB.class, schemes, "testInvoke", ControlEvent.INVOKE, ControlEvent.RELEASE_INVOKE);

      LockedObject lo = ejb.lockMethodFromClass();
      Assert.assertNotNull(lo);
      List<LockedObject> l2 = Locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());

      TEntity entity = createTEntity(5, "valuexx");
      byte[] bytes = "Pausenclown".getBytes();
      List<Object> list = ejb.testInvoke("Hals", -34, 456, bytes, entity, new Long(43));
      Assert.assertNotNull(list);
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      l2 = Locker.loadLockedObjects();
      Assert.assertEquals(0, l2.size());
   }

   @Test
   public void lockAutomaticUnlock() throws Exception {
      log.info("start lockAutomaticUnlock()");

      LockActuator la = (LockActuator) Configuration.instance().getActuator(LockActuator.DEFAULTNAME);
      la.setAutomaticUnlock(true);

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      registerSetpoint(CibetTestEJB.class, schemes, "testInvoke", ControlEvent.INVOKE, ControlEvent.RELEASE_INVOKE);

      LockedObject lo = ejb.lockMethodFromClass();
      Assert.assertNotNull(lo);
      List<LockedObject> l2 = Locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());
      Assert.assertEquals(LockState.LOCKED, l2.get(0).getLockState());

      TEntity entity = createTEntity(5, "valuexx");
      byte[] bytes = "Pausenclown".getBytes();
      List<Object> list = ejb.testInvoke("Hals", -34, 456, bytes, entity, new Long(43));
      Assert.assertNotNull(list);
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      Query q = Context.requestScope().getEntityManager().createNamedQuery(LockedObject.SEL_ALL);
      q.setParameter("tenant", TENANT);
      l2 = q.getResultList();
      Assert.assertEquals(1, l2.size());
      Assert.assertEquals(LockState.UNLOCKED, l2.get(0).getLockState());
   }

}
