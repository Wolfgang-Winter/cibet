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
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

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
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.archive.ArchiveLoader;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.actuator.dc.FourEyesActuator;
import com.logitags.cibet.actuator.dc.SixEyesActuator;
import com.logitags.cibet.actuator.info.InfoLogActuator;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InitializationService;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.diff.Difference;
import com.logitags.cibet.diff.DifferenceType;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.security.DefaultSecurityProvider;
import com.logitags.cibet.security.SecurityProvider;
import com.logitags.cibet.sensor.jpa.JpaResourceHandler;

/**
 * ArchiveManagerImplIntegrationTest
 * 
 * @author Wolfgang
 * 
 */
@RunWith(Arquillian.class)
public class ArchiveContextInterceptorIT extends AbstractArquillian {

   private static Logger log = Logger.getLogger(ArchiveContextInterceptorIT.class);

   @EJB(beanName = "RemoteEJBImpl")
   private RemoteEJB remoteEjb;

   private Setpoint sp = null;

   @Deployment
   public static WebArchive createDeployment() {
      String warName = ArchiveContextInterceptorIT.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("it/web2.xml");

      archive.addClasses(AbstractArquillian.class, CoreTestBase.class, TEntity.class, TComplexEntity.class,
            ITComplexEntity.class, AbstractTEntity.class, TCompareEntity.class, TComplexEntity2.class,
            ArquillianTestServlet1.class, RemoteEJB.class, RemoteEJBImpl.class, SecuredRemoteEJBImpl.class,
            SimpleEjb.class);

      File[] cibet = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-jpa").withTransitivity()
            .asFile();
      archive.addAsLibraries(cibet);

      archive.addAsWebInfResource("META-INF/persistence-it.xml", "classes/META-INF/persistence.xml");
      archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
      archive.addAsWebInfResource("config_2.xml", "classes/cibet-config.xml");

      log.debug(archive.toString(true));
      archive.as(ZipExporter.class).exportTo(new File("target/" + warName), true);

      return archive;
   }

   @Before
   public void beforeArchiveContextInterceptorIT() {
      InitializationService.instance().startContext();
      Context.sessionScope().setUser(USER);
      Context.sessionScope().setTenant(TENANT);
      cman = Configuration.instance();
   }

   @After
   public void afterArchiveContextInterceptorIT() {
      InitializationService.instance().endContext();
      // new ConfigurationService().initialise();
      if (sp != null) {
         cman.unregisterSetpoint(sp.getId());
      }

   }

   private void testDifference(List<Difference> comps) {
      Assert.assertEquals(1, comps.size());
      Difference cou = comps.get(0);

      Assert.assertTrue(cou.getPropertyName().equals("lazyList"));
      Assert.assertTrue(cou.getDifferenceType() == DifferenceType.ADDED);
      Assert.assertTrue(cou.getOldValue() == null);
      Assert.assertTrue(cou.getNewValue() instanceof TEntity);
      Assert.assertEquals("Karl", ((TEntity) cou.getNewValue()).getNameValue());
   }

   private Archive createArchive(int archiveId, int lastArchiveId) {
      SecurityProvider secProvider = cman.getSecurityProvider();
      Archive a = new Archive();
      a.setArchiveId(UUID.randomUUID().toString());
      a.setCaseId(UUID.randomUUID().toString());
      a.setControlEvent(ControlEvent.INSERT);
      a.setExecutionStatus(ExecutionStatus.EXECUTED);
      a.setCreateDate(today);
      a.setCreateUser("user1");
      a.setTenant(Context.sessionScope().getTenant());
      a.setRemark("remark");

      Resource res = new Resource();
      res.setPrimaryKeyId(String.valueOf(archiveId + 10));
      res.setTargetType(TEntity.class.getName());
      res.setResourceHandlerClass(JpaResourceHandler.class.getName());
      res.setEncrypted(true);
      res.setKeyReference(secProvider.getCurrentSecretKey());
      a.setResource(res);

      a.createChecksum();
      log.debug(a.getArchiveId() + ": checkSumString = '" + a.getChecksum() + "'");
      String cs = secProvider.createMessageDigest(a.getChecksum(), secProvider.getCurrentSecretKey());
      a.setChecksum(cs);
      return a;
   }

   private void doReleaseUpdate(List<String> schemes) throws Exception {
      sp = registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.UPDATE, ControlEvent.RELEASE_UPDATE);

      TComplexEntity ce = createTComplexEntity();
      persist(ce);

      ut.begin();
      TComplexEntity tce = applEman.find(TComplexEntity.class, ce.getId());
      Assert.assertNotNull(tce);
      Assert.assertEquals(3, tce.getLazyList().size());
      TEntity t1 = tce.getLazyList().iterator().next();
      tce.getLazyList().remove(t1);
      tce.setCompValue(122);
      tce = applEman.merge(tce);
      ut.commit();

      EventResult er = Context.requestScope().getExecutedEventResult();
      log.debug(er);
      Assert.assertNotNull(er);
      Assert.assertEquals(ExecutionStatus.POSTPONED, er.getExecutionStatus());
      Assert.assertNull(er.getParentResult());
      Assert.assertEquals(ControlEvent.UPDATE, er.getEvent());

      List<DcControllable> l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      DcControllable co = l1.get(0);
      Context.sessionScope().setUser("tester2");
      ut.begin();
      co.release(applEman, "blabla");
      ut.commit();

      er = Context.requestScope().getExecutedEventResult();
      log.debug(er);
      Assert.assertNotNull(er);
      Assert.assertEquals(ExecutionStatus.EXECUTED, er.getExecutionStatus());
      Assert.assertNull(er.getParentResult());
      Assert.assertEquals(ControlEvent.RELEASE_UPDATE, er.getEvent());

      l1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, l1.size());

      List<Archive> archList = ArchiveLoader.loadArchivesByPrimaryKeyId(TComplexEntity.class.getName(),
            String.valueOf(tce.getId()));
      Assert.assertEquals(2, archList.size());
      Resource res0 = archList.get(0).getResource();
      Resource res1 = archList.get(1).getResource();
      TComplexEntity ar1 = (TComplexEntity) res0.getObject();
      TComplexEntity ar2 = (TComplexEntity) res1.getObject();
      Assert.assertEquals(122, ar1.getCompValue());
      Assert.assertEquals(122, ar2.getCompValue());
      Assert.assertEquals(ControlEvent.UPDATE, archList.get(0).getControlEvent());
      Assert.assertEquals(ControlEvent.RELEASE_UPDATE, archList.get(1).getControlEvent());
   }

   private void release6Eyes(ControlEvent event1, ControlEvent event2, ControlEvent event3) throws Exception {
      EventResult er = Context.internalRequestScope().getExecutedEventResult();
      Assert.assertNotNull(er);
      Assert.assertEquals(ExecutionStatus.FIRST_POSTPONED, er.getExecutionStatus());
      Assert.assertEquals(0, er.getChildResults().size());
      Assert.assertNull(er.getParentResult());
      Assert.assertEquals("SIX_EYES, ARCHIVE", er.getActuators());

      List<DcControllable> l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      DcControllable co = l1.get(0);
      Context.sessionScope().setUser("tester2");
      ut.begin();
      co.release(applEman, "blabla1");
      ut.commit();

      er = Context.internalRequestScope().getExecutedEventResult();
      Assert.assertNotNull(er);
      Assert.assertEquals(ExecutionStatus.FIRST_RELEASED, er.getExecutionStatus());
      Assert.assertEquals(0, er.getChildResults().size());
      Assert.assertNull(er.getParentResult());
      Assert.assertEquals("SIX_EYES, ARCHIVE", er.getActuators());

      List<Archive> list = ArchiveLoader.loadArchives(TComplexEntity.class.getName());
      Assert.assertEquals(2, list.size());
      Assert.assertEquals(event1, list.get(0).getControlEvent());
      Assert.assertEquals(USER, list.get(0).getCreateUser());
      Assert.assertEquals(null, list.get(0).getRemark());
      Resource res0 = list.get(0).getResource();
      Assert.assertNotNull(res0.getTarget());

      Assert.assertEquals(event2, list.get(1).getControlEvent());
      Assert.assertEquals("tester2", list.get(1).getCreateUser());
      Assert.assertEquals("blabla1", list.get(1).getRemark());
      Resource res1 = list.get(1).getResource();
      Assert.assertNotNull(res1.getTarget());

      ut.begin();
      l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      co = l1.get(0);
      Context.sessionScope().setUser(USER);
      co.reject(applEman, "blabla2");
      ut.commit();

      er = Context.internalRequestScope().getExecutedEventResult();
      Assert.assertNotNull(er);
      Assert.assertEquals(ExecutionStatus.REJECTED, er.getExecutionStatus());
      Assert.assertEquals(0, er.getChildResults().size());
      Assert.assertNull(er.getParentResult());
      Assert.assertEquals("SIX_EYES, ARCHIVE", er.getActuators());

      list = ArchiveLoader.loadArchives(TComplexEntity.class.getName());
      Assert.assertEquals(3, list.size());
      Assert.assertEquals(event3, list.get(2).getControlEvent());
      Assert.assertEquals(USER, list.get(2).getCreateUser());
      Assert.assertEquals("blabla2", list.get(2).getRemark());
      Resource res2 = list.get(2).getResource();
      Assert.assertNotNull(res2.getTarget());
   }

   @Test
   public void testInvoke() throws Exception {
      log.debug("start testInvoke()");
      Context.sessionScope().setTenant("testTenant");
      Context.sessionScope().setUser(null);
      TEntity te = new TEntity("myName", 45, "winter");

      log.debug("remoteEjb: " + remoteEjb);
      TEntity te2 = remoteEjb.persist(te);
      log.debug(te2);
      Assert.assertTrue(te2.getId() != 0);

      ut.begin();
      Context.sessionScope().setTenant("testTenant");
      List<Archive> list = ArchiveLoader.loadArchives(TEntity.class.getName());
      ut.commit();
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("ANONYMOUS", list.get(0).getCreateUser());
      Context.sessionScope().setTenant(null);
      Context.requestScope().setScheduledDate(null);
   }

   @Test
   public void releasePersistLoadArchivesByCaseId() throws Exception {
      log.info("start releasePersistLoadArchivesByCaseId()");
      Context.requestScope().setScheduledDate(null);

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.RELEASE);
      TEntity entity = createTEntity(5, "valuexx");
      persist(entity);

      EventResult er = Context.requestScope().getExecutedEventResult();
      Assert.assertNotNull(er);
      Assert.assertEquals(ExecutionStatus.POSTPONED, er.getExecutionStatus());
      Assert.assertEquals(0, er.getChildResults().size());
      Assert.assertNull(er.getParentResult());
      Assert.assertEquals("ARCHIVE, FOUR_EYES", er.getActuators());

      List<Archive> alist = ArchiveLoader.loadArchives();
      Assert.assertTrue(alist.size() == 1);
      String caseId = alist.get(0).getCaseId();

      List<DcControllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());
      DcControllable co = l.get(0);
      Context.sessionScope().setUser("test2");
      ut.begin();
      try {
         co.release(applEman, null);

         er = Context.internalRequestScope().getExecutedEventResult();
         Assert.assertNotNull(er);
         Assert.assertEquals(ExecutionStatus.EXECUTED, er.getExecutionStatus());
         Assert.assertEquals(0, er.getChildResults().size());
         Assert.assertNull(er.getParentResult());
         Assert.assertEquals("FOUR_EYES, ARCHIVE", er.getActuators());

         Context.internalRequestScope().getEntityManager().clear();
         List<Archive> list = ArchiveLoader.loadArchives(TEntity.class.getName());
         Assert.assertEquals(2, list.size());
         Resource res0 = list.get(0).getResource();
         Resource res1 = list.get(1).getResource();

         Assert.assertEquals(res0.getPrimaryKeyId(), res1.getPrimaryKeyId());

         alist = ArchiveLoader.loadArchivesByCaseId(caseId);
         Assert.assertTrue(alist.size() == 2);
         ut.commit();
      } catch (Exception e) {
         log.error(e.getMessage(), e);
         ut.rollback();
         throw e;
      }
   }

   @Test
   public void releaseUpdate() throws Exception {
      log.info("start releaseUpdate()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      doReleaseUpdate(schemes);
   }

   @Test
   public void releaseUpdateReversedActuatorSequence() throws Exception {
      log.info("start releaseUpdate()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      doReleaseUpdate(schemes);
   }

   @Test
   public void releaseRemove6Eyes() throws Exception {
      log.info("start releaseRemove6Eyes()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(SixEyesActuator.DEFAULTNAME);
      schemes.add(InfoLogActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.DELETE, ControlEvent.RELEASE_DELETE,
            ControlEvent.FIRST_RELEASE_DELETE);

      TComplexEntity ce = createTComplexEntity();
      persist(ce);

      ut.begin();
      TComplexEntity tce = applEman.find(TComplexEntity.class, ce.getId());
      Assert.assertNotNull(tce);
      Assert.assertEquals(3, tce.getLazyList().size());
      applEman.remove(tce);
      ut.commit();

      // first release
      List<DcControllable> l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      DcControllable co = l1.get(0);
      Context.sessionScope().setUser("tester2");
      ut.begin();
      co.release(applEman, "blabla1");
      ut.commit();
      applEman.clear();

      List<Archive> list = ArchiveLoader.loadArchives(TComplexEntity.class.getName());
      Assert.assertEquals(2, list.size());
      Assert.assertEquals(ControlEvent.DELETE, list.get(0).getControlEvent());
      Assert.assertEquals(USER, list.get(0).getCreateUser());
      Assert.assertEquals(null, list.get(0).getRemark());
      Resource res0 = list.get(0).getResource();
      Assert.assertNotNull(res0.getTarget());

      Assert.assertEquals(ControlEvent.FIRST_RELEASE_DELETE, list.get(1).getControlEvent());
      Assert.assertEquals("tester2", list.get(1).getCreateUser());
      Assert.assertEquals("blabla1", list.get(1).getRemark());
      Resource res1 = list.get(1).getResource();
      Assert.assertNotNull(res1.getTarget());

      // 2. release
      l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      co = l1.get(0);
      Context.sessionScope().setUser("tester3");
      ut.begin();
      co.release(applEman, "blabla2");
      ut.commit();

      list = ArchiveLoader.loadArchives(TComplexEntity.class.getName());
      Assert.assertEquals(3, list.size());
      Resource res2 = list.get(2).getResource();
      Assert.assertNotNull(res2.getTarget());
      Object obj = res2.getObject();
      Assert.assertNotNull(obj);
      Assert.assertTrue(obj instanceof TComplexEntity);
      Assert.assertEquals(ControlEvent.RELEASE_DELETE, list.get(2).getControlEvent());
      Assert.assertEquals("tester3", list.get(2).getCreateUser());
      Assert.assertEquals("blabla2", list.get(2).getRemark());
   }

   @Test
   public void rejectRemove4Eyes() throws Exception {
      log.info("start rejectRemove4Eyes()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.DELETE, ControlEvent.REJECT);

      TComplexEntity ce = createTComplexEntity();
      persist(ce);

      ut.begin();
      TComplexEntity tce = applEman.find(TComplexEntity.class, ce.getId());
      applEman.remove(tce);
      ut.commit();

      List<DcControllable> l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      DcControllable co = l1.get(0);
      ut.begin();
      co.reject(applEman, "blabla1");
      ut.commit();

      EventResult er = Context.internalRequestScope().getExecutedEventResult();
      Assert.assertNotNull(er);
      Assert.assertEquals(ExecutionStatus.REJECTED, er.getExecutionStatus());
      Assert.assertEquals(0, er.getChildResults().size());
      Assert.assertNull(er.getParentResult());
      Assert.assertEquals("FOUR_EYES, ARCHIVE", er.getActuators());

      List<Archive> list = ArchiveLoader.loadArchives(TComplexEntity.class.getName());
      Assert.assertEquals(2, list.size());
      Assert.assertEquals(ControlEvent.DELETE, list.get(0).getControlEvent());
      Assert.assertEquals(USER, list.get(0).getCreateUser());
      Assert.assertEquals(null, list.get(0).getRemark());
      Resource res0 = list.get(0).getResource();
      Assert.assertNotNull(res0.getTarget());

      Assert.assertEquals(ControlEvent.REJECT_DELETE, list.get(1).getControlEvent());
      Assert.assertEquals(USER, list.get(1).getCreateUser());
      Assert.assertEquals("blabla1", list.get(1).getRemark());
      Resource res1 = list.get(1).getResource();
      Assert.assertNotNull(res1.getTarget());
   }

   @Test
   public void rejectPersist6Eyes() throws Exception {
      log.info("start rejectPersist6Eyes()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(SixEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.REJECT_INSERT,
            ControlEvent.FIRST_RELEASE_INSERT);

      TComplexEntity ce = createTComplexEntity();
      persist(ce);

      release6Eyes(ControlEvent.INSERT, ControlEvent.FIRST_RELEASE_INSERT, ControlEvent.REJECT_INSERT);
   }

   @Test
   public void rejectMerge6Eyes() throws Exception {
      log.info("start rejectMerge6Eyes()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(SixEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.UPDATE, ControlEvent.REJECT_UPDATE,
            ControlEvent.FIRST_RELEASE);

      TComplexEntity ce = createTComplexEntity();
      persist(ce);

      ce.setOwner("changedOwner");
      ce = merge(ce);

      release6Eyes(ControlEvent.UPDATE, ControlEvent.FIRST_RELEASE_UPDATE, ControlEvent.REJECT_UPDATE);
   }

   @Test
   public void rejectRemove6Eyes() throws Exception {
      log.info("start rejectRemove6Eyes()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(SixEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.DELETE, ControlEvent.REJECT_DELETE,
            ControlEvent.FIRST_RELEASE);

      TComplexEntity ce = createTComplexEntity();
      persist(ce);
      remove(ce);

      release6Eyes(ControlEvent.DELETE, ControlEvent.FIRST_RELEASE_DELETE, ControlEvent.REJECT_DELETE);
   }

   @Test
   public void rejectRemove4EyesNoActuators() throws Exception {
      log.info("start rejectRemove4EyesNoActuators()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.DELETE);

      TComplexEntity ce = createTComplexEntity();
      persist(ce);
      remove(ce);

      List<DcControllable> l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      DcControllable co = l1.get(0);
      ut.begin();
      co.reject(applEman, "blabla1");
      ut.commit();

      List<Archive> list = ArchiveLoader.loadArchives(TComplexEntity.class.getName());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(ControlEvent.DELETE, list.get(0).getControlEvent());
      Assert.assertEquals(USER, list.get(0).getCreateUser());
      Assert.assertEquals(null, list.get(0).getRemark());
      Resource res0 = list.get(0).getResource();
      Assert.assertNotNull(res0.getTarget());
   }

   @Test
   public void compare() throws Exception {
      log.info("start compare()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE);

      // 1. Archive
      TComplexEntity t1 = createTComplexEntity();
      persist(t1);

      ut.begin();
      TComplexEntity selEnt = applEman.find(TComplexEntity.class, t1.getId());
      Assert.assertNotNull("entity with id " + t1.getId() + " not found", selEnt);

      // 2. Archive
      selEnt.getLazyList().add(new TEntity("Karl", 5, "Putz"));
      applEman.merge(selEnt);
      applEman.flush();
      // ut.commit();

      // 3. Archive
      // ut.begin();
      selEnt = applEman.find(TComplexEntity.class, t1.getId());
      selEnt.setCompValue(12);
      applEman.merge(selEnt);
      applEman.flush();
      // ut.commit();

      selEnt = applEman.find(TComplexEntity.class, t1.getId());
      Assert.assertNotNull("entity with id " + t1.getId() + " not found", selEnt);
      Assert.assertEquals(12, selEnt.getCompValue());

      // ut.begin();
      List<Archive> archives = ArchiveLoader.loadArchives(TComplexEntity.class.getName());
      Assert.assertEquals(3, archives.size());
      Resource res0 = archives.get(0).getResource();
      Resource res1 = archives.get(1).getResource();
      Resource res2 = archives.get(2).getResource();
      log.debug("res0 Object: " + res0.getObject().getClass());
      log.debug("res1: " + res1.getObject().getClass());
      log.debug("res2: " + res2.getObject().getClass());

      log.debug("HERE 222");

      List<Difference> comps = CibetUtil.compare(res2, res0);
      applEman.flush();
      // ut.commit();
      testDifference(comps);

      comps = CibetUtil.compare(selEnt, res0.getObject());
      testDifference(comps);

      Object ar1Obj = res0.getObject();
      Object ar2Obj = res2.getObject();
      comps = CibetUtil.compare(ar2Obj, ar1Obj);
      testDifference(comps);
      ut.commit();
   }

   @Test
   public void restoreTEntityWithArchiveWithRemove() throws Exception {
      log.info("start restoreTEntityWithArchiveWithRemove()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE, ControlEvent.RESTORE);

      TEntity entity = createTEntity(28, "Ludwig");
      persist(entity);

      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      selEnt.setCounter(14);
      selEnt = merge(selEnt);

      remove(selEnt);
      applEman.clear();

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(TEntity.class.getName(),
            String.valueOf(selEnt.getId()));
      Assert.assertEquals(3, list.size());

      ut.begin();
      TEntity selEnt2 = (TEntity) list.get(0).restore(applEman, "Soll: 14");
      ut.commit();
      Assert.assertEquals(28, selEnt2.getCounter());

      List<Archive> list2 = ArchiveLoader.loadArchivesByCaseId(list.get(0).getCaseId());
      Assert.assertEquals(2, list2.size());
      Assert.assertEquals("Soll: 14", list2.get(1).getRemark());
      Assert.assertEquals(ControlEvent.RESTORE_INSERT, list2.get(1).getControlEvent());

      selEnt = applEman.find(TEntity.class, selEnt2.getId());
      Assert.assertEquals(28, selEnt.getCounter());
   }

   /**
    * Does not work together with Envers. Property name="hibernate.listeners.envers.autoRegister" value="false" must be
    * false in persistence.xml
    * 
    * @throws Exception
    */
   @Test
   public void restoreComplexWithArchiveWithRemove() throws Exception {
      log.info("start restoreComplexWithArchiveWithRemove()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE, ControlEvent.RESTORE);

      TComplexEntity ce = createTComplexEntity();
      persist(ce);

      ut.begin();
      TComplexEntity selEnt = applEman.find(TComplexEntity.class, ce.getId());
      selEnt.setCompValue(14);
      selEnt = applEman.merge(selEnt);
      applEman.flush();
      applEman.remove(selEnt);
      ut.commit();

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(TComplexEntity.class.getName(),
            String.valueOf(selEnt.getId()));
      Assert.assertEquals(3, list.size());

      ut.begin();
      TComplexEntity selEnt2 = (TComplexEntity) list.get(0).restore(applEman, "Soll: 14");
      ut.commit();
      applEman.clear();
      Assert.assertEquals(12, selEnt2.getCompValue());

      List<Archive> list2 = ArchiveLoader.loadArchivesByCaseId(list.get(0).getCaseId());
      Assert.assertEquals(2, list2.size());
      Assert.assertEquals("Soll: 14", list2.get(1).getRemark());
      Assert.assertEquals(ControlEvent.RESTORE_INSERT, list2.get(1).getControlEvent());

      Resource res1 = list2.get(1).getResource();
      TComplexEntity restored = (TComplexEntity) res1.getObject();
      Assert.assertEquals(12, restored.getCompValue());

      selEnt = applEman.find(TComplexEntity.class, selEnt2.getId());
      Assert.assertEquals(12, selEnt.getCompValue());
   }

   @Test
   public void restoreComplexWithArchiveWithUpdate() throws Exception {
      log.info("start restoreComplexWithArchiveWithUpdate()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE, ControlEvent.RESTORE);

      TComplexEntity ce = createTComplexEntity();
      persist(ce);

      ut.begin();
      TComplexEntity selEnt = applEman.find(TComplexEntity.class, ce.getId());
      selEnt.setCompValue(14);
      selEnt = applEman.merge(selEnt);
      applEman.flush();

      selEnt.setCompValue(18);
      selEnt = applEman.merge(selEnt);
      ut.commit();
      Context.internalRequestScope().getEntityManager().clear();

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(TComplexEntity.class.getName(),
            String.valueOf(selEnt.getId()));
      Assert.assertEquals(3, list.size());

      Assert.assertEquals(14, ((TComplexEntity) list.get(1).getResource().getObject()).getCompValue());

      ut.begin();
      TComplexEntity selEnt2 = (TComplexEntity) list.get(1).restore(applEman, "Soll: 14");
      ut.commit();
      Assert.assertEquals(14, selEnt2.getCompValue());

      List<Archive> list2 = ArchiveLoader.loadArchivesByCaseId(list.get(1).getCaseId());
      Assert.assertEquals(2, list2.size());
      Assert.assertEquals("Soll: 14", list2.get(1).getRemark());
      Assert.assertEquals(ControlEvent.RESTORE_UPDATE, list2.get(1).getControlEvent());

      selEnt = applEman.find(TComplexEntity.class, selEnt.getId());
      Assert.assertEquals(14, selEnt.getCompValue());
   }

   @Test
   public void restoreComplexWith4EyesWithRemove() throws Exception {
      log.info("start restoreComplexWith4EyesWithRemove()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE, ControlEvent.RESTORE);

      TComplexEntity ce = createTComplexEntity();
      persist(ce);
      applEman.clear();

      ut.begin();
      TComplexEntity selEnt = applEman.find(TComplexEntity.class, ce.getId());
      selEnt.setCompValue(14);
      selEnt = applEman.merge(selEnt);
      applEman.flush();
      applEman.remove(selEnt);
      ut.commit();

      Context.internalRequestScope().getEntityManager().clear();

      schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      Setpoint sp2 = registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE, ControlEvent.RESTORE);

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(TComplexEntity.class.getName(),
            String.valueOf(selEnt.getId()));
      Assert.assertEquals(3, list.size());

      log.debug("dtarch: " + list.get(0).toString());
      log.debug("dtarch: " + list.get(1).toString());
      log.debug("dtarch: " + list.get(2).toString());
      ut.begin();
      Object obj = list.get(0).restore(applEman, "Soll: 12");
      ut.commit();
      log.debug("restore result=" + obj);
      Assert.assertNull(obj);

      List<Archive> list2 = ArchiveLoader.loadArchivesByCaseId(list.get(0).getCaseId());
      Assert.assertEquals(2, list2.size());
      Assert.assertEquals("Soll: 12", list2.get(1).getRemark());
      Assert.assertEquals(ControlEvent.RESTORE_INSERT, list2.get(1).getControlEvent());

      Resource res0 = list.get(0).getResource();
      Object primaryKey = ((TComplexEntity) res0.getObject()).getId();
      selEnt = applEman.find(TComplexEntity.class, primaryKey);
      Assert.assertNull(selEnt);

      List<DcControllable> dcList = DcLoader.findUnreleased(TComplexEntity.class.getName());
      Assert.assertEquals(1, dcList.size());
      DcControllable dcObj = dcList.get(0);
      Assert.assertEquals(list.get(0).getCaseId(), dcObj.getCaseId());
      Assert.assertEquals(ControlEvent.INSERT, dcObj.getControlEvent());

      log.debug("now release");
      Context.sessionScope().setUser("releaser");
      ut.begin();
      TComplexEntity result = (TComplexEntity) dcObj.release(applEman, null);
      ut.commit();
      Assert.assertNotNull(result);
      log.debug(result);
      Assert.assertEquals(3, result.getLazyList().size());
      Assert.assertEquals(12, result.getCompValue());

      ut.begin();
      selEnt = applEman.find(TComplexEntity.class, result.getId());
      Assert.assertNotNull(selEnt);
      Assert.assertEquals(3, selEnt.getLazyList().size());
      Assert.assertEquals(12, selEnt.getCompValue());
      ut.commit();

      List<Archive> list3 = ArchiveLoader.loadArchives(TComplexEntity.class.getName());
      Assert.assertEquals(4, list3.size());
      cman.unregisterSetpoint(sp2.getId());
   }

   @Test
   public void restoreComplexWith4EyesWithUpdate() throws Exception {
      log.info("start restoreComplexWith4EyesWithUpdate()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE, ControlEvent.RESTORE);

      TComplexEntity ce = createTComplexEntity();
      persist(ce);

      ut.begin();
      TComplexEntity selEnt = applEman.find(TComplexEntity.class, ce.getId());
      selEnt.setCompValue(14);
      selEnt = applEman.merge(selEnt);
      applEman.flush();

      selEnt.setCompValue(18);
      selEnt = applEman.merge(selEnt);
      ut.commit();

      schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      Setpoint sp2 = registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE, ControlEvent.RESTORE);

      Context.internalRequestScope().getEntityManager().clear();
      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(TComplexEntity.class.getName(),
            String.valueOf(selEnt.getId()));
      Assert.assertEquals(3, list.size());

      ut.begin();
      TComplexEntity selEnt2 = (TComplexEntity) list.get(2).restore(applEman, "Soll: 14");
      ut.commit();
      Assert.assertNull(selEnt2);

      List<Archive> list2 = ArchiveLoader.loadArchivesByCaseId(list.get(2).getCaseId());
      Assert.assertEquals(2, list2.size());
      Assert.assertEquals("Soll: 14", list2.get(1).getRemark());
      Assert.assertEquals(ControlEvent.RESTORE_UPDATE, list2.get(1).getControlEvent());

      selEnt = applEman.find(TComplexEntity.class, selEnt.getId());
      Assert.assertEquals(18, selEnt.getCompValue());

      List<DcControllable> dcList = DcLoader.findUnreleased(TComplexEntity.class.getName());
      Assert.assertEquals(1, dcList.size());
      DcControllable dcObj = dcList.get(0);
      Assert.assertEquals(list.get(2).getCaseId(), dcObj.getCaseId());
      Assert.assertEquals(ControlEvent.UPDATE, dcObj.getControlEvent());
      cman.unregisterSetpoint(sp2.getId());
   }

   @Test
   public void encryptArchiveInvalidKey() throws Exception {
      log.info("start encryptArchiveInvalidKey()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE);

      ArchiveActuator arch = (ArchiveActuator) cman.getActuator(ArchiveActuator.DEFAULTNAME);
      arch.setIntegrityCheck(false);
      arch.setEncrypt(true);

      DefaultSecurityProvider secp = (DefaultSecurityProvider) cman.getSecurityProvider();
      secp.getSecrets().put("ll", "secret");
      secp.setCurrentSecretKey("ll");

      try {
         TEntity te2 = createTEntity(33, "Heinz");
         persist(te2);
         Assert.fail();
      } catch (RuntimeException e) {
         Assert.assertTrue(e.getCause() instanceof InvalidKeyException);
         ut.rollback();
      } finally {
         secp.setCurrentSecretKey("1");
      }
   }

   @Test
   public void encryptArchive() throws Exception {
      log.info("start encryptArchive()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE);

      ArchiveActuator arch = (ArchiveActuator) cman.getActuator(ArchiveActuator.DEFAULTNAME);
      arch.setIntegrityCheck(false);
      arch.setEncrypt(true);

      DefaultSecurityProvider secp = (DefaultSecurityProvider) cman.getSecurityProvider();
      secp.setCurrentSecretKey("1");
      TEntity te = createTEntity(33, "Heinz");
      persist(te);

      secp.getSecrets().put("l3", "secret5566778890");
      secp.setCurrentSecretKey("l3");

      try {
         TEntity te2 = createTEntity(34, "Heinz2");
         persist(te2);

         Query q = applEman.createNamedQuery(TEntity.SEL_BY_OWNER);
         q.setParameter("owner", TENANT);
         List<TEntity> list = q.getResultList();
         Assert.assertEquals(2, list.size());

         List<Archive> list1 = ArchiveLoader.loadArchives();
         Assert.assertEquals(2, list1.size());
         Archive ar = list1.get(0);
         Assert.assertEquals("1", ar.getResource().getKeyReference());
         Assert.assertEquals(33, ((TEntity) ar.getResource().getObject()).getCounter());

         Archive ar2 = list1.get(1);
         Assert.assertEquals("l3", ar2.getResource().getKeyReference());
         Assert.assertEquals(34, ((TEntity) ar2.getResource().getObject()).getCounter());

      } finally {
         secp.setCurrentSecretKey("1");
      }
   }

   @Test
   public void loadArchivesWithDifferences() throws Exception {
      log.info("start loadArchivesWithDifferences()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE, ControlEvent.RESTORE);

      TComplexEntity ce = createTComplexEntity();
      persist(ce);

      ut.begin();
      TComplexEntity selEnt = applEman.find(TComplexEntity.class, ce.getId());
      selEnt.setCompValue(14);
      selEnt = applEman.merge(selEnt);
      ut.commit();
      applEman.clear();

      ut.begin();
      TComplexEntity selEnt2 = applEman.find(TComplexEntity.class, ce.getId());
      TEntity e8 = new TEntity("val8", 8, TENANT);
      selEnt2.addLazyList(e8);
      selEnt2 = applEman.merge(selEnt2);
      ut.commit();
      applEman.clear();

      ut.begin();
      TComplexEntity selEnt3 = applEman.find(TComplexEntity.class, ce.getId());
      selEnt3.setOwner("Klaus");
      selEnt3.setCompValue(552);
      selEnt3 = applEman.merge(selEnt3);
      ut.commit();
      applEman.clear();

      remove(selEnt3);

      long id = selEnt.getId();
      log.debug("HERE 111");
      ut.begin();
      List<Archive> ali = ArchiveLoader.loadArchivesByPrimaryKeyId(TComplexEntity.class.getName(), id);
      Map<Archive, List<Difference>> map = ArchiveLoader.analyzeDifferences(ali);
      ut.commit();
      Assert.assertEquals(5, map.size());

      Iterator<Archive> iter = map.keySet().iterator();
      iter.next();

      log.debug("now restore " + selEnt.getId());
      ut.begin();
      TComplexEntity selEnt4 = (TComplexEntity) iter.next().restore(applEman, "Soll: 14");
      ut.commit();
      applEman.clear();
      Assert.assertEquals(14, selEnt4.getCompValue());

      Context.requestScope().getEntityManager().clear();
      ut.begin();
      ali = ArchiveLoader.loadArchivesByPrimaryKeyId(TComplexEntity.class.getName(), id);
      map = ArchiveLoader.analyzeDifferences(ali);
      ut.commit();
      // restored entity has a new ID, therefore not 6:
      Assert.assertEquals(5, map.size());
      Iterator<Entry<Archive, List<Difference>>> iter2 = map.entrySet().iterator();

      while (iter2.hasNext()) {
         Entry<Archive, List<Difference>> e = iter2.next();
         log.debug(e.getKey() + ":\n" + e.getValue().size() + "|" + e.getValue());
      }

      iter2 = map.entrySet().iterator();
      Entry<Archive, List<Difference>> e = iter2.next();
      Assert.assertEquals(ControlEvent.INSERT, e.getKey().getControlEvent());
      Assert.assertEquals(0, e.getValue().size());

      e = iter2.next();
      Assert.assertEquals(ControlEvent.UPDATE, e.getKey().getControlEvent());
      Assert.assertEquals(1, e.getValue().size());
      Assert.assertEquals(14, e.getValue().get(0).getNewValue());

      e = iter2.next();
      Assert.assertEquals(ControlEvent.UPDATE, e.getKey().getControlEvent());
      Assert.assertEquals(1, e.getValue().size());
      Assert.assertEquals("lazyList", e.getValue().get(0).getPropertyName());
      Assert.assertEquals(DifferenceType.ADDED, e.getValue().get(0).getDifferenceType());

      e = iter2.next();
      Assert.assertEquals(ControlEvent.UPDATE, e.getKey().getControlEvent());
      Assert.assertEquals(2, e.getValue().size());
      Assert.assertEquals(DifferenceType.MODIFIED, e.getValue().get(0).getDifferenceType());
      Assert.assertEquals(DifferenceType.MODIFIED, e.getValue().get(1).getDifferenceType());

      e = iter2.next();
      Assert.assertEquals(ControlEvent.DELETE, e.getKey().getControlEvent());
      Assert.assertEquals(0, e.getValue().size());
   }

}
