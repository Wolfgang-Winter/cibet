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
package com.logitags.cibet.actuator.archive;

import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.cibethelper.base.DBHelper;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TComplexEntity2;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.actuator.dc.FourEyesActuator;
import com.logitags.cibet.actuator.dc.ResourceApplyException;
import com.logitags.cibet.actuator.dc.SixEyesActuator;
import com.logitags.cibet.actuator.info.InfoLogActuator;
import com.logitags.cibet.config.Configuration;
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
import com.logitags.cibet.sensor.jpa.CibetEntityManager;
import com.logitags.cibet.sensor.jpa.JpaResourceHandler;

public class ArchiveManagerImplIntegrationTest extends DBHelper {

   private static Logger log = Logger.getLogger(ArchiveManagerImplIntegrationTest.class);

   private Date today = new Date();

   @After
   public void after() {
      log.debug("ArchiveManagerImplIntegrationTest:doAfter()");
      initConfiguration("cibet-config.xml");
   }

   private List<Archive> persistSomeArchives(int difference, EntityManager eman) {
      int nbr = 20;
      Archive a = null;
      for (int i = 0; i < nbr; i++) {
         a = createArchive("A" + i);
         insertNativeArchive(a, eman, JpaResourceHandler.class.getName());
      }
      eman.clear();

      log.info("checking ...");
      // Query q = eman
      // .createQuery("SELECT a FROM StateArchive a ORDER BY a.archiveId");
      Query q = eman.createNamedQuery(Archive.SEL_ALL);
      List<Archive> list = q.getResultList();
      Assert.assertTrue(!list.isEmpty());
      Assert.assertEquals("list size = " + list.size(), nbr + 1, list.size());
      return list;
   }

   private void doReleaseUpdate(List<String> schemes) throws Exception {
      registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.UPDATE, ControlEvent.RELEASE_UPDATE);
      Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());

      TComplexEntity ce = createTComplexEntity();
      applEman.persist(ce);
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      applEman.clear();

      TComplexEntity tce = applEman.find(TComplexEntity.class, ce.getId());
      Assert.assertNotNull(tce);
      Assert.assertEquals(3, tce.getLazyList().size());
      TEntity t1 = tce.getLazyList().iterator().next();
      tce.getLazyList().remove(t1);
      tce.setCompValue(122);

      tce = applEman.merge(tce);
      applEman.flush();
      applEman.clear();

      EventResult er = Context.requestScope().getExecutedEventResult();
      log.debug(er);
      Assert.assertNotNull(er);
      Assert.assertEquals(ExecutionStatus.POSTPONED, er.getExecutionStatus());
      // Assert.assertEquals(4, er.getChildResults().size());
      Assert.assertNull(er.getParentResult());
      Assert.assertEquals(ControlEvent.UPDATE, er.getEvent());

      List<DcControllable> l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      DcControllable co = l1.get(0);
      Context.sessionScope().setUser("tester2");
      co.release(applEman, "blabla");

      er = Context.requestScope().getExecutedEventResult();
      log.debug(er);
      Assert.assertNotNull(er);
      Assert.assertEquals(ExecutionStatus.EXECUTED, er.getExecutionStatus());
      // Assert.assertEquals(4, er.getChildResults().size());
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

   private void insertNativeArchive(Archive a, EntityManager eman, String resourceType) {
      Query q = eman.createNativeQuery("INSERT INTO CIB_ARCHIVE "
            + "(ARCHIVEID, CONTROLEVENT, EXECUTIONSTATUS, CREATEUSER, CREATEDATE, " + "TENANT, REMARK, CHECKSUM, "
            + "CASEID, TARGETTYPE, PRIMARYKEYID, VERSION, RESOURCEHANDLERCLASS, ENCRYPTED, KEYREFERENCE, UNIQUEID"
            + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?,1,?,?)");
      q.setParameter(1, a.getArchiveId());
      q.setParameter(2, a.getControlEvent().name());
      q.setParameter(3, a.getExecutionStatus().name());
      q.setParameter(4, a.getCreateUser());
      q.setParameter(5, a.getCreateDate());
      q.setParameter(6, a.getTenant());
      q.setParameter(7, a.getRemark());
      q.setParameter(8, a.getChecksum());
      q.setParameter(9, a.getCaseId());
      q.setParameter(10, a.getResource().getTargetType());
      q.setParameter(11, a.getResource().getPrimaryKeyId());
      q.setParameter(12, resourceType);
      q.setParameter(13, a.getResource().getKeyReference());
      q.setParameter(14, a.getResource().getUniqueId());
      q.executeUpdate();

      eman.flush();
   }

   private Archive createArchive(String archiveId) {
      SecurityProvider secProvider = Configuration.instance().getSecurityProvider();
      Archive a = new Archive();
      a.setArchiveId(archiveId);
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
      return a;
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

   @Test
   public void releasePersistLoadArchivesByCaseId() throws Exception {
      log.info("start releasePersistLoadArchivesByCaseId()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.RELEASE);

      TEntity te = persistTEntity();

      EventResult er = Context.requestScope().getExecutedEventResult();
      Assert.assertNotNull(er);
      Assert.assertEquals(ExecutionStatus.POSTPONED, er.getExecutionStatus());
      Assert.assertEquals(0, er.getChildResults().size());
      Assert.assertNull(er.getParentResult());
      Assert.assertEquals("FOUR_EYES, ARCHIVE", er.getActuators());

      List<Archive> alist = ArchiveLoader.loadArchives();
      Assert.assertTrue(alist.size() == 1);
      String caseId = alist.get(0).getCaseId();

      List<DcControllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());
      DcControllable co = l.get(0);
      Context.sessionScope().setUser("test2");
      co.release(Context.internalRequestScope().getApplicationEntityManager(), null);

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
   public void releaseRemove6Eyes() throws ResourceApplyException {
      log.info("start releaseRemove6Eyes()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(SixEyesActuator.DEFAULTNAME);
      schemes.add(InfoLogActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.DELETE, ControlEvent.RELEASE_DELETE,
            ControlEvent.FIRST_RELEASE_DELETE);

      TComplexEntity ce = createTComplexEntity();
      applEman.persist(ce);
      applEman.flush();
      applEman.clear();

      TComplexEntity tce = applEman.find(TComplexEntity.class, ce.getId());
      Assert.assertNotNull(tce);
      Assert.assertEquals(3, tce.getLazyList().size());

      applEman.remove(tce);
      applEman.flush();

      // first release
      List<DcControllable> l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      DcControllable co = l1.get(0);
      Context.sessionScope().setUser("tester2");
      co.release(Context.internalRequestScope().getApplicationEntityManager(), "blabla1");
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
      Context.sessionScope().setUser("tester3");
      co.release(Context.internalRequestScope().getApplicationEntityManager(), "blabla2");

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
   public void rejectRemove4Eyes() throws ResourceApplyException {
      log.info("start rejectRemove4Eyes()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.DELETE, ControlEvent.REJECT);

      TComplexEntity ce = createTComplexEntity();
      applEman.persist(ce);
      applEman.flush();
      applEman.clear();

      TComplexEntity tce = applEman.find(TComplexEntity.class, ce.getId());
      applEman.remove(tce);
      applEman.flush();
      applEman.clear();

      List<DcControllable> l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      DcControllable co = l1.get(0);
      co.reject(Context.internalRequestScope().getApplicationEntityManager(), "blabla1");
      applEman.flush();

      EventResult er = Context.internalRequestScope().getExecutedEventResult();
      log.debug(er);
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
   public void rejectPersist6Eyes() throws ResourceApplyException {
      log.info("start rejectPersist6Eyes()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(SixEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.REJECT_INSERT,
            ControlEvent.FIRST_RELEASE_INSERT);

      TComplexEntity ce = createTComplexEntity();
      applEman.persist(ce);
      applEman.flush();
      applEman.clear();

      EventResult er = Context.internalRequestScope().getExecutedEventResult();
      Assert.assertNotNull(er);
      log.debug(er);
      Assert.assertEquals(ExecutionStatus.FIRST_POSTPONED, er.getExecutionStatus());
      Assert.assertEquals(0, er.getChildResults().size());
      Assert.assertNull(er.getParentResult());
      Assert.assertEquals("SIX_EYES, ARCHIVE", er.getActuators());

      List<DcControllable> l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      DcControllable co = l1.get(0);
      Context.sessionScope().setUser("tester2");
      co.release(Context.internalRequestScope().getApplicationEntityManager(), "blabla1");
      applEman.flush();

      er = Context.internalRequestScope().getExecutedEventResult();
      Assert.assertNotNull(er);
      Assert.assertEquals(ExecutionStatus.FIRST_RELEASED, er.getExecutionStatus());
      Assert.assertEquals(0, er.getChildResults().size());
      Assert.assertNull(er.getParentResult());
      Assert.assertEquals("SIX_EYES, ARCHIVE", er.getActuators());

      List<Archive> list = ArchiveLoader.loadArchives(TComplexEntity.class.getName());
      Assert.assertEquals(2, list.size());
      Assert.assertEquals(ControlEvent.INSERT, list.get(0).getControlEvent());
      Assert.assertEquals(USER, list.get(0).getCreateUser());
      Assert.assertEquals(null, list.get(0).getRemark());
      Resource res0 = list.get(0).getResource();
      Assert.assertNotNull(res0.getTarget());

      Assert.assertEquals(ControlEvent.FIRST_RELEASE_INSERT, list.get(1).getControlEvent());
      Assert.assertEquals("tester2", list.get(1).getCreateUser());
      Assert.assertEquals("blabla1", list.get(1).getRemark());
      Resource res1 = list.get(1).getResource();
      Assert.assertNotNull(res1.getTarget());

      Context.sessionScope().setUser(USER);
      co.reject(Context.internalRequestScope().getApplicationEntityManager(), "blabla2");

      er = Context.internalRequestScope().getExecutedEventResult();
      Assert.assertNotNull(er);
      Assert.assertEquals(ExecutionStatus.REJECTED, er.getExecutionStatus());
      Assert.assertEquals(0, er.getChildResults().size());
      Assert.assertNull(er.getParentResult());
      Assert.assertEquals("SIX_EYES, ARCHIVE", er.getActuators());

      list = ArchiveLoader.loadArchives(TComplexEntity.class.getName());
      Assert.assertEquals(3, list.size());
      Assert.assertEquals(ControlEvent.REJECT_INSERT, list.get(2).getControlEvent());
      Assert.assertEquals(USER, list.get(2).getCreateUser());
      Assert.assertEquals("blabla2", list.get(2).getRemark());
      Resource res2 = list.get(2).getResource();
      Assert.assertNotNull(res2.getTarget());
   }

   @Test
   public void rejectRemove4EyesNoActuators() throws ResourceApplyException {
      log.info("start rejectRemove4EyesNoActuators()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.DELETE);

      TComplexEntity ce = createTComplexEntity();
      applEman.persist(ce);
      applEman.flush();
      applEman.clear();

      applEman.remove(ce);
      applEman.flush();
      applEman.clear();

      List<DcControllable> l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      DcControllable co = l1.get(0);
      co.reject(Context.internalRequestScope().getApplicationEntityManager(), "blabla1");

      List<Archive> list = ArchiveLoader.loadArchives(TComplexEntity.class.getName());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(ControlEvent.DELETE, list.get(0).getControlEvent());
      Assert.assertEquals(USER, list.get(0).getCreateUser());
      Assert.assertEquals(null, list.get(0).getRemark());
      Resource res0 = list.get(0).getResource();
      Assert.assertNotNull(res0.getTarget());
   }

   @Test
   public void checkArchiveIntegrityRecordsModified() {
      log.info("start checkArchiveIntegrityRecordsModified()");
      Archive a = createArchive("A1");
      a.setControlEvent(ControlEvent.DELETE);
      Assert.assertFalse(a.checkChecksum());
      a = createArchive("A2");
      a.setCreateUser("userNew");
      Assert.assertFalse(a.checkChecksum());
      a = createArchive("A3");
      a.setCaseId("neu");
      Assert.assertFalse(a.checkChecksum());
   }

   @Test
   public void compare() {
      log.info("start compare()");
      registerSetpoint(TComplexEntity.class, ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE);

      // 1. Archive
      TComplexEntity t1 = createTComplexEntity();
      applEman.persist(t1);
      applEman.flush();
      applEman.clear();

      // applEman.setLoadEager(true);
      TComplexEntity selEnt = applEman.find(TComplexEntity.class, t1.getId());
      Assert.assertNotNull("entity with id " + t1.getId() + " not found", selEnt);

      // 2. Archive
      selEnt.getLazyList().add(new TEntity("Karl", 5, "Putz"));
      applEman.merge(selEnt);
      applEman.flush();
      applEman.clear();

      // 3. Archive
      selEnt = applEman.find(TComplexEntity.class, t1.getId());
      selEnt.setCompValue(12);
      applEman.merge(selEnt);
      applEman.flush();
      applEman.clear();
      // applEman.setLoadEager(false);

      selEnt = applEman.find(TComplexEntity.class, t1.getId());
      Assert.assertNotNull("entity with id " + t1.getId() + " not found", selEnt);
      Assert.assertEquals(12, selEnt.getCompValue());

      List<Archive> archives = ArchiveLoader.loadArchives(TComplexEntity.class.getName());
      Assert.assertEquals(3, archives.size());
      Resource res0 = archives.get(0).getResource();
      Resource res1 = archives.get(1).getResource();
      Resource res2 = archives.get(2).getResource();
      log.debug("res0 Object: " + res0.getObject().getClass());
      log.debug("res1: " + res1.getObject().getClass());
      log.debug("res2: " + res2.getObject().getClass());

      List<Difference> comps = CibetUtil.compare(res2, res0);
      testDifference(comps);

      comps = CibetUtil.compare(selEnt, res0.getObject());
      testDifference(comps);

      Object ar1Obj = res0.getObject();
      Object ar2Obj = res2.getObject();
      comps = CibetUtil.compare(ar2Obj, ar1Obj);
      testDifference(comps);
   }

   @Test
   public void restoreTEntityWithArchiveWithRemove() {
      log.info("start restoreTEntityWithArchiveWithRemove()");
      registerSetpoint(TEntity.class, ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE, ControlEvent.RESTORE);

      TEntity entity = persistTEntity();

      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      selEnt.setCounter(14);
      selEnt = applEman.merge(selEnt);
      applEman.flush();

      applEman.remove(selEnt);
      applEman.flush();
      applEman.clear();

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(TEntity.class.getName(),
            String.valueOf(selEnt.getId()));
      Assert.assertEquals(3, list.size());

      TEntity selEnt2 = (TEntity) list.get(0).restore(applEman, "Soll: 14");
      applEman.flush();
      Assert.assertEquals(5, selEnt2.getCounter());

      List<Archive> list2 = ArchiveLoader.loadArchivesByCaseId(list.get(0).getCaseId());
      Assert.assertEquals(2, list2.size());
      Assert.assertEquals("Soll: 14", list2.get(1).getRemark());
      Assert.assertEquals(ControlEvent.RESTORE_INSERT, list2.get(1).getControlEvent());

      selEnt = applEman.find(TEntity.class, selEnt2.getId());
      Assert.assertEquals(5, selEnt.getCounter());
   }

   @Test
   public void restoreComplexWithArchiveWithRemove() {
      log.info("start restoreComplexWithArchiveWithRemove()");
      registerSetpoint(TComplexEntity.class, ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE, ControlEvent.RESTORE);

      TComplexEntity ce = createTComplexEntity();
      applEman.persist(ce);
      applEman.flush();
      applEman.clear();

      TComplexEntity selEnt = applEman.find(TComplexEntity.class, ce.getId());
      selEnt.setCompValue(14);
      selEnt = applEman.merge(selEnt);
      applEman.flush();

      applEman.remove(selEnt);
      applEman.flush();
      applEman.clear();

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(TComplexEntity.class.getName(),
            String.valueOf(selEnt.getId()));
      Assert.assertEquals(3, list.size());

      TComplexEntity selEnt2 = (TComplexEntity) list.get(0).restore(applEman, "Soll: 14");
      applEman.flush();
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
   public void restoreComplexWithArchiveWithUpdate() {
      log.info("start restoreComplexWithArchiveWithUpdate()");
      registerSetpoint(TComplexEntity.class, ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE, ControlEvent.RESTORE);

      TComplexEntity ce = createTComplexEntity();
      applEman.persist(ce);
      applEman.flush();
      applEman.clear();

      TComplexEntity selEnt = applEman.find(TComplexEntity.class, ce.getId());
      selEnt.setCompValue(14);
      selEnt = applEman.merge(selEnt);

      selEnt.setCompValue(18);
      selEnt = applEman.merge(selEnt);
      applEman.flush();
      applEman.clear();
      Context.requestScope().getEntityManager().clear();

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(TComplexEntity.class.getName(),
            String.valueOf(selEnt.getId()));
      Assert.assertEquals(3, list.size());

      TComplexEntity selEnt2 = (TComplexEntity) list.get(1).restore(applEman, "Soll: 14");

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
      registerSetpoint(TComplexEntity.class, ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE, ControlEvent.RESTORE);

      TComplexEntity ce = createTComplexEntity();
      applEman.persist(ce);
      applEman.flush();
      applEman.clear();

      TComplexEntity selEnt = applEman.find(TComplexEntity.class, ce.getId());
      selEnt.setCompValue(14);
      selEnt = applEman.merge(selEnt);
      applEman.flush();

      applEman.remove(selEnt);
      applEman.flush();

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE, ControlEvent.RESTORE);

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(TComplexEntity.class.getName(),
            String.valueOf(selEnt.getId()));
      Assert.assertEquals(3, list.size());

      log.debug("dtarch: " + list.get(0).toString());
      log.debug("dtarch: " + list.get(1).toString());
      log.debug("dtarch: " + list.get(2).toString());
      Object obj = list.get(0).restore(applEman, "Soll: 12");
      applEman.flush();
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

      // release
      Context.sessionScope().setUser("releaser");
      TComplexEntity result = (TComplexEntity) dcObj.release(applEman, null);
      applEman.flush();
      Assert.assertNotNull(result);
      log.debug(result);
      Assert.assertEquals(3, result.getLazyList().size());
      Assert.assertEquals(12, result.getCompValue());

      selEnt = applEman.find(TComplexEntity.class, result.getId());
      Assert.assertNotNull(selEnt);
      Assert.assertEquals(3, selEnt.getLazyList().size());
      Assert.assertEquals(12, selEnt.getCompValue());

      List<Archive> list3 = ArchiveLoader.loadArchives(TComplexEntity.class.getName());
      Assert.assertEquals(4, list3.size());
   }

   @Test
   public void restoreComplexWith4EyesWithUpdate() {
      log.info("start restoreComplexWith4EyesWithUpdate()");

      registerSetpoint(TComplexEntity.class, ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE, ControlEvent.RESTORE);

      TComplexEntity ce = createTComplexEntity();
      applEman.persist(ce);
      applEman.flush();
      // applEman.clear();

      TComplexEntity selEnt = applEman.find(TComplexEntity.class, ce.getId());
      selEnt.setCompValue(14);
      selEnt = applEman.merge(selEnt);
      applEman.flush();

      selEnt.setCompValue(18);
      selEnt = applEman.merge(selEnt);
      applEman.flush();

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE, ControlEvent.RESTORE);

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(TComplexEntity.class.getName(),
            String.valueOf(selEnt.getId()));
      Assert.assertEquals(3, list.size());

      TComplexEntity selEnt2 = (TComplexEntity) list.get(2).restore(applEman, "Soll: 14");
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
   }

   @Test
   public void checkIntegrityWith2KeyReferences() {
      log.info("start checkIntegrityWith2KeyReferences()");
      registerSetpoint(TEntity.class.getName(), ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE);

      Context.sessionScope().setTenant(TENANT);
      ArchiveActuator arch = (ArchiveActuator) Configuration.instance().getActuator(ArchiveActuator.DEFAULTNAME);
      arch.setIntegrityCheck(true);

      DefaultSecurityProvider secp = (DefaultSecurityProvider) Configuration.instance().getSecurityProvider();
      secp.setCurrentSecretKey("1");
      persistTEntity();

      secp.getSecrets().put("ll", "secret");
      secp.setCurrentSecretKey("ll");

      try {
         persistTEntity();

         Query q = applEman.createNamedQuery(TEntity.SEL_BY_OWNER);
         q.setParameter("owner", TENANT);
         List<TEntity> list = q.getResultList();
         Assert.assertEquals(2, list.size());

         List<Archive> list1 = ArchiveLoader.loadArchives();
         Assert.assertEquals(2, list1.size());
         Archive ar = list1.get(0);
         Assert.assertEquals("1", ar.getResource().getKeyReference());
         Archive ar2 = list1.get(1);
         Assert.assertEquals("ll", ar2.getResource().getKeyReference());

      } finally {
         secp.setCurrentSecretKey("1");
      }
   }

   @Test
   public void encryptArchiveInvalidKey() {
      log.info("start encryptArchiveInvalidKey()");

      registerSetpoint(TEntity.class.getName(), ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE);

      ArchiveActuator arch = (ArchiveActuator) Configuration.instance().getActuator(ArchiveActuator.DEFAULTNAME);
      arch.setIntegrityCheck(false);
      arch.setEncrypt(true);

      DefaultSecurityProvider secp = (DefaultSecurityProvider) Configuration.instance().getSecurityProvider();
      secp.getSecrets().put("ll", "secret");
      secp.setCurrentSecretKey("ll");

      try {
         persistTEntity();
         Assert.fail();
      } catch (RuntimeException e) {
         Assert.assertTrue(e.getCause() instanceof InvalidKeyException);
      } finally {
         secp.setCurrentSecretKey("1");
      }
   }

   @Test
   public void encryptArchive() {
      log.info("start encryptArchive()");

      registerSetpoint(TEntity.class.getName(), ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE);

      ArchiveActuator arch = (ArchiveActuator) Configuration.instance().getActuator(ArchiveActuator.DEFAULTNAME);
      arch.setIntegrityCheck(false);
      arch.setEncrypt(true);

      DefaultSecurityProvider secp = (DefaultSecurityProvider) Configuration.instance().getSecurityProvider();
      secp.setCurrentSecretKey("1");
      persistTEntity();

      secp.getSecrets().put("l3", "secret5566778890");
      secp.setCurrentSecretKey("l3");

      try {
         persistTEntity();

         Query q = applEman.createNamedQuery(TEntity.SEL_BY_OWNER);
         q.setParameter("owner", TENANT);
         List<TEntity> list = q.getResultList();
         Assert.assertEquals(2, list.size());

         List<Archive> list1 = ArchiveLoader.loadArchives();
         Assert.assertEquals(2, list1.size());
         Archive ar = list1.get(0);
         Assert.assertEquals("1", ar.getResource().getKeyReference());
         Assert.assertEquals(5, ((TEntity) ar.getResource().getObject()).getCounter());

         Archive ar2 = list1.get(1);
         Assert.assertEquals("l3", ar2.getResource().getKeyReference());
         Assert.assertEquals(5, ((TEntity) ar2.getResource().getObject()).getCounter());

      } finally {
         secp.setCurrentSecretKey("1");
      }
   }

   @Test
   public void loadArchivesWithDifferences() {
      log.info("start loadArchivesWithDifferences()");
      registerSetpoint(TComplexEntity.class, ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE, ControlEvent.RESTORE);

      TComplexEntity ce = createTComplexEntity();
      applEman.persist(ce);
      applEman.flush();
      applEman.clear();
      long id = ce.getId();

      ((CibetEntityManager) applEman).setLoadEager(true);
      TComplexEntity selEnt = applEman.find(TComplexEntity.class, id);
      selEnt.setCompValue(14);
      selEnt = applEman.merge(selEnt);
      applEman.flush();
      applEman.clear();

      TComplexEntity selEnt2 = applEman.find(TComplexEntity.class, ce.getId());
      TEntity e8 = new TEntity("val8", 8, TENANT);
      selEnt2.addLazyList(e8);
      selEnt2 = applEman.merge(selEnt2);
      applEman.flush();
      applEman.clear();

      TComplexEntity selEnt3 = applEman.find(TComplexEntity.class, ce.getId());
      selEnt3.setOwner("Klaus");
      selEnt3.setCompValue(552);
      selEnt3 = applEman.merge(selEnt3);
      applEman.flush();
      applEman.clear();

      applEman.remove(selEnt3);
      applEman.flush();
      applEman.clear();
      ((CibetEntityManager) applEman).setLoadEager(false);

      Context.requestScope().getEntityManager().getTransaction().commit();
      Context.requestScope().getEntityManager().getTransaction().begin();
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      log.debug("now check differences");
      List<Archive> ali = ArchiveLoader.loadArchivesByPrimaryKeyId(TComplexEntity.class.getName(), id);
      Map<Archive, List<Difference>> map = ArchiveLoader.analyzeDifferences(ali);
      Assert.assertEquals(5, map.size());

      Iterator<Archive> iter = map.keySet().iterator();
      iter.next();

      log.debug("now restore");
      TComplexEntity selEnt4 = (TComplexEntity) iter.next().restore(applEman, "Soll: 14");
      applEman.flush();
      applEman.clear();
      Assert.assertEquals(14, selEnt4.getCompValue());

      Context.requestScope().getEntityManager().getTransaction().commit();
      Context.requestScope().getEntityManager().clear();
      Context.requestScope().getEntityManager().getTransaction().begin();
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      log.debug("now load 2.");
      ali = ArchiveLoader.loadArchivesByPrimaryKeyId(TComplexEntity.class.getName(), id);
      map = ArchiveLoader.analyzeDifferences(ali);
      Assert.assertEquals(5, map.size());
      Iterator<Entry<Archive, List<Difference>>> iter2 = map.entrySet().iterator();
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

   @Test
   public void loadArchivesWithDifferencesLazy() {
      log.info("start loadArchivesWithDifferencesLazy()");
      TComplexEntity2 ce = createTComplexEntity2();
      ce.setOwner2("Lupo");
      TComplexEntity2 selEnt = null;
      registerSetpoint(TComplexEntity2.class, ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT, ControlEvent.UPDATE);

      applEman.persist(ce);
      applEman.flush();
      applEman.clear();

      selEnt = applEman.find(TComplexEntity2.class, ce.getId());
      selEnt.setCompValue(14);
      TEntity e8 = new TEntity("val8", 8, TENANT);
      selEnt.addLazyList(e8);
      selEnt = applEman.merge(selEnt);
      applEman.flush();
      // applEman.clear();

      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      // TComplexEntity selEnt2 = applEman.find(TComplexEntity.class, ce.getId());
      // TEntity e8 = new TEntity("val8", 8, TENANT);
      // selEnt2.addLazyList(e8);
      // selEnt2 = applEman.merge(selEnt2);
      // applEman.flush();
      // applEman.clear();

      log.debug("now compare");
      List<Archive> ali = ArchiveLoader.loadArchivesByPrimaryKeyId(TComplexEntity2.class.getName(), selEnt.getId());
      Map<Archive, List<Difference>> map = ArchiveLoader.analyzeDifferences(ali);
      Assert.assertEquals(2, map.size());

      Iterator<Archive> iter = map.keySet().iterator();
      Archive a = iter.next();
      Assert.assertEquals(0, map.get(a).size());
      a = iter.next();
      List<Difference> difs = map.get(a);
      Assert.assertEquals(2, difs.size());
   }

   @Test
   public void loadArchivesWithDifferencesLazy2() {
      log.info("start loadArchivesWithDifferencesLazy2()");
      TComplexEntity2 ce = createTComplexEntity2();
      ce.setOwner2("Lupo");
      TComplexEntity2 selEnt = null;
      registerSetpoint(TComplexEntity2.class, ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT, ControlEvent.UPDATE);

      applEman.persist(ce);
      applEman.flush();
      applEman.clear();

      selEnt = applEman.find(TComplexEntity2.class, ce.getId());
      selEnt.setCompValue(14);
      selEnt = applEman.merge(selEnt);
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      applEman.clear();

      selEnt = applEman.find(TComplexEntity2.class, ce.getId());
      selEnt.setOwner("Butze");
      selEnt = applEman.merge(selEnt);
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      applEman.clear();

      selEnt = applEman.find(TComplexEntity2.class, ce.getId());
      TEntity e8 = new TEntity("val8", 8, TENANT);
      selEnt.addLazyList(e8);
      selEnt = applEman.merge(selEnt);
      applEman.flush();
      // applEman.clear();

      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      // TComplexEntity selEnt2 = applEman.find(TComplexEntity.class, ce.getId());
      // TEntity e8 = new TEntity("val8", 8, TENANT);
      // selEnt2.addLazyList(e8);
      // selEnt2 = applEman.merge(selEnt2);
      // applEman.flush();
      // applEman.clear();

      log.debug("now compare");
      List<Archive> ali = ArchiveLoader.loadArchivesByPrimaryKeyId(TComplexEntity2.class.getName(), selEnt.getId());
      Map<Archive, List<Difference>> map = ArchiveLoader.analyzeDifferences(ali);
      Assert.assertEquals(4, map.size());

      Iterator<Archive> iter = map.keySet().iterator();
      Archive a = iter.next();

      Assert.assertEquals(0, map.get(a).size());
      Assert.assertEquals(ControlEvent.INSERT, a.getControlEvent());

      a = iter.next();
      Assert.assertEquals(1, map.get(a).size());
      Assert.assertEquals(ControlEvent.UPDATE, a.getControlEvent());

      a = iter.next();
      Assert.assertEquals(1, map.get(a).size());
      Assert.assertEquals(ControlEvent.UPDATE, a.getControlEvent());

      a = iter.next();
      Assert.assertEquals(1, map.get(a).size());
      Assert.assertEquals(ControlEvent.UPDATE, a.getControlEvent());
   }

   @Test
   public void testTargetRollback() throws Exception {
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT);

      persistTEntity();
      Context.requestScope().setRollbackOnly(true);
      applEman.getTransaction().rollback();
      applEman.getTransaction().begin();

      InitializationService.instance().endContext();
      InitializationService.instance().startContext();

      List<Archive> alist = ArchiveLoader.loadAllArchives();
      Assert.assertEquals(0, alist.size());
   }

}
