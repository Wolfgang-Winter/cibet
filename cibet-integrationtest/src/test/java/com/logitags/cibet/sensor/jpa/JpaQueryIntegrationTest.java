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
package com.logitags.cibet.sensor.jpa;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.cibethelper.base.DBHelper;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.archive.ArchiveLoader;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.actuator.dc.FourEyesActuator;
import com.logitags.cibet.actuator.dc.ResourceApplyException;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.resource.Resource;

/**
 * tests CibetEntityManager with Archive and FourEyes- actuators.
 * 
 * @author test
 * 
 */
public class JpaQueryIntegrationTest extends DBHelper {

   private static Logger log = Logger.getLogger(JpaQueryIntegrationTest.class);

   private Setpoint sp = null;

   @After
   public void afterJpaQueryIntegrationTest() {
      if (sp != null) {
         Configuration.instance().unregisterSetpoint(sp.getId());
      }
   }

   @Test
   public void namedQuery() throws ResourceApplyException {
      log.info("start namedQuery()");
      sp = registerSetpoint(TEntity.SEL_BY_OWNER, ArchiveActuator.DEFAULTNAME, ControlEvent.INVOKE);

      persistTEntity();

      Query q = applEman.createNamedQuery(TEntity.SEL_BY_OWNER);
      q.setParameter("owner", TENANT);
      List<TEntity> tlist = q.getResultList();
      Assert.assertEquals(1, tlist.size());

      log.debug("entity.getId(): " + tlist.get(0).getId());

      List<Archive> list = ArchiveLoader.loadArchives(TEntity.SEL_BY_OWNER);
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Resource r = ar.getResource();
      Assert.assertEquals(TEntity.SEL_BY_OWNER, r.getTargetType());
      Assert.assertEquals(3, r.getParameters().size());
      log.debug("now redo");
      List<TEntity> tlist2 = (List<TEntity>) ar.redo("do it");
      Assert.assertEquals(1, tlist2.size());
      Assert.assertEquals(tlist.get(0).getId(), tlist2.get(0).getId());

      TEntity te2 = (TEntity) q.getSingleResult();
      Assert.assertNotNull(te2);
      Assert.assertEquals(tlist.get(0).getId(), te2.getId());
      list = ArchiveLoader.loadArchives(TEntity.SEL_BY_OWNER);
      Assert.assertEquals(2, list.size());
   }

   @Test
   public void query() throws ResourceApplyException {
      log.info("start query()");
      String qq = "SELECT a FROM TEntity a WHERE a.owner = :owner AND a.xdate = :today";
      sp = registerSetpoint(qq, ArchiveActuator.DEFAULTNAME, ControlEvent.INVOKE);

      Date now = new Date();
      persistTEntity(5, now, null);

      Query q = applEman.createQuery(qq);
      q.setParameter("owner", TENANT);
      q.setParameter("today", now, TemporalType.DATE);
      List<TEntity> tlist = q.getResultList();
      Assert.assertEquals(1, tlist.size());

      log.debug("entity.getId(): " + tlist.get(0).getId());

      List<Archive> list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Resource r = ar.getResource();
      Assert.assertEquals(qq, r.getTargetType());
      Assert.assertEquals(4, r.getParameters().size());

      log.debug("now redo");
      List<TEntity> tlist2 = (List<TEntity>) ar.redo("do it");
      Assert.assertEquals(1, tlist2.size());
      Assert.assertEquals(tlist.get(0).getId(), tlist2.get(0).getId());

      TEntity te2 = (TEntity) q.getSingleResult();
      Assert.assertNotNull(te2);
      Assert.assertEquals(tlist.get(0).getId(), te2.getId());
      list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(2, list.size());
   }

   @Test
   public void queryExecuteUpdate() throws ResourceApplyException {
      log.info("start queryExecuteUpdate()");
      String qq = "DELETE FROM TEntity a WHERE a.owner = :owner AND a.xCaltimestamp = :today";
      sp = registerSetpoint(qq, ArchiveActuator.DEFAULTNAME, ControlEvent.INVOKE);

      Date now = new Date();
      Calendar cal = Calendar.getInstance();
      persistTEntity(5, now, cal);

      Query q = applEman.createQuery(qq);
      q.setParameter("owner", TENANT);
      q.setParameter("today", cal, TemporalType.TIMESTAMP);
      int count = q.executeUpdate();
      Assert.assertEquals(1, count);

      Query q2 = applEman.createNamedQuery(TEntity.SEL_BY_OWNER);
      q2.setParameter("owner", TENANT);
      List<TEntity> tlist = q2.getResultList();
      Assert.assertEquals(0, tlist.size());

      List<Archive> list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Resource r = ar.getResource();
      Assert.assertEquals(qq, r.getTargetType());
      Assert.assertEquals(4, r.getParameters().size());

      log.debug("now redo");
      int count2 = (int) ar.redo("do it");
      Assert.assertEquals(0, count2);
   }

   @Test
   public void queryMaxResult() throws ResourceApplyException {
      log.info("start queryMaxResult()");
      String qq1 = "SELECT a FROM TEntity a WHERE a.owner = :owner AND a.xCaldate = :today order by a.counter";
      String qq = "SELECT a FROM TEntity a*";
      sp = registerSetpoint(qq, ArchiveActuator.DEFAULTNAME, ControlEvent.INVOKE);

      Date now = new Date();
      Calendar cal = Calendar.getInstance();
      for (int i = 0; i < 50; i++) {
         persistTEntity(i, now, cal);
      }

      Query q = applEman.createQuery(qq1);
      q.setParameter("owner", TENANT);
      q.setParameter("today", cal, TemporalType.DATE);
      q.setFirstResult(10);
      q.setMaxResults(10);
      q.setHint("hint1", "?");
      List<TEntity> tlist = q.getResultList();
      Assert.assertEquals(10, tlist.size());
      for (TEntity te : tlist) {
         log.debug(te);
         Assert.assertTrue(te.getCounter() > 9);
         Assert.assertTrue(te.getCounter() < 20);
      }

      List<Archive> list = ArchiveLoader.loadArchives(qq1);
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Resource r = ar.getResource();
      Assert.assertEquals(qq1, r.getTargetType());
      Assert.assertEquals(7, r.getParameters().size());

      log.debug("now redo");
      List<TEntity> tlist2 = (List<TEntity>) ar.redo("do it");
      Assert.assertEquals(10, tlist2.size());
      for (TEntity te : tlist2) {
         log.debug(te);
         Assert.assertTrue(te.getCounter() > 9);
         Assert.assertTrue(te.getCounter() < 20);
      }
   }

   @Test
   public void queryLockDate() throws Exception {
      log.info("start queryLockDate()");
      String qq1 = "SELECT a FROM TComplexEntity a WHERE a.owner = :owner order by a.compValue";
      String qq = "SELECT a FROM TComplexEntity a*";
      sp = registerSetpoint(qq, ArchiveActuator.DEFAULTNAME, ControlEvent.INVOKE);

      for (int i = 0; i < 50; i++) {
         TComplexEntity tc = createTComplexEntity();
         tc.setCompValue(i);
         applEman.persist(tc);
         applEman.getTransaction().commit();
         applEman.getTransaction().begin();
         applEman.clear();
      }

      log.info("queryLockDate() now select");
      Query q = applEman.createQuery(qq1);
      q.setParameter("owner", TENANT);
      q.setFirstResult(10);
      q.setLockMode(LockModeType.OPTIMISTIC);
      q.setFlushMode(FlushModeType.AUTO);
      List<TComplexEntity> tlist = q.getResultList();
      Assert.assertEquals(40, tlist.size());
      for (TComplexEntity te : tlist) {
         log.debug(te);
         Assert.assertTrue(te.getCompValue() > 9);
      }

      List<Archive> list = ArchiveLoader.loadArchives(qq1);
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Resource r = ar.getResource();
      Assert.assertEquals(qq1, r.getTargetType());
      Assert.assertEquals(6, r.getParameters().size());

      log.debug("queryLockDate() now redo");
      List<TComplexEntity> tlist2 = (List<TComplexEntity>) ar.redo("do it");
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      Assert.assertEquals(40, tlist2.size());
      for (TComplexEntity te : tlist2) {
         log.debug(te);
         Assert.assertTrue(te.getCompValue() > 9);
      }
   }

   @Test
   public void queryNamedTimestampDate() throws ResourceApplyException {
      log.info("start queryNamedTimestampDate()");
      String qq = "SELECT a FROM TEntity a WHERE a.owner = ?1 AND a.xtimestamp = ?2";
      sp = registerSetpoint(qq, ArchiveActuator.DEFAULTNAME, ControlEvent.INVOKE);

      Date now = new Date();
      persistTEntity(5, now, null);

      Query q = applEman.createQuery(qq);
      q.setParameter(1, TENANT);
      q.setParameter(2, now, TemporalType.TIMESTAMP);
      List<TEntity> tlist = q.getResultList();
      Assert.assertEquals(1, tlist.size());

      log.debug("entity.getId(): " + tlist.get(0).getId());

      List<Archive> list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Resource r = ar.getResource();
      Assert.assertEquals(qq, r.getTargetType());
      Assert.assertEquals(4, r.getParameters().size());

      log.debug("now redo");
      List<TEntity> tlist2 = (List<TEntity>) ar.redo("do it");
      Assert.assertEquals(1, tlist2.size());
      Assert.assertEquals(tlist.get(0).getId(), tlist2.get(0).getId());

      TEntity te2 = (TEntity) q.getSingleResult();
      Assert.assertNotNull(te2);
      Assert.assertEquals(tlist.get(0).getId(), te2.getId());
      list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(2, list.size());
   }

   @Test
   public void queryNamedTimeDate() throws ResourceApplyException {
      log.info("start queryNamedTimeDate()");
      String qq = "SELECT a FROM TEntity a WHERE a.owner = ?1 AND a.xtime = ?2";
      sp = registerSetpoint(qq, ArchiveActuator.DEFAULTNAME, ControlEvent.INVOKE);

      Date now = new Date();
      persistTEntity(5, now, null);

      Query q = applEman.createQuery(qq);
      q.setParameter(1, TENANT);
      q.setParameter(2, now, TemporalType.TIME);
      List<TEntity> tlist = q.getResultList();
      Assert.assertEquals(1, tlist.size());

      log.debug("entity.getId(): " + tlist.get(0).getId());

      List<Archive> list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Resource r = ar.getResource();
      Assert.assertEquals(qq, r.getTargetType());
      Assert.assertEquals(4, r.getParameters().size());

      log.debug("now redo");
      List<TEntity> tlist2 = (List<TEntity>) ar.redo("do it");
      Assert.assertEquals(1, tlist2.size());
      Assert.assertEquals(tlist.get(0).getId(), tlist2.get(0).getId());

      TEntity te2 = (TEntity) q.getSingleResult();
      Assert.assertNotNull(te2);
      Assert.assertEquals(tlist.get(0).getId(), te2.getId());
      list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(2, list.size());
   }

   @Test
   public void queryNamedDate() throws ResourceApplyException {
      log.info("start queryNamedDate()");
      String qq = "SELECT a FROM TEntity a WHERE a.owner = ?1 AND a.xdate = ?2";
      sp = registerSetpoint(qq, ArchiveActuator.DEFAULTNAME, ControlEvent.INVOKE);

      Date now = new Date();
      persistTEntity(5, now, null);

      Query q = applEman.createQuery(qq);
      q.setParameter(1, TENANT);
      q.setParameter(2, now, TemporalType.DATE);
      List<TEntity> tlist = q.getResultList();
      Assert.assertEquals(1, tlist.size());

      log.debug("entity.getId(): " + tlist.get(0).getId());

      List<Archive> list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Resource r = ar.getResource();
      Assert.assertEquals(qq, r.getTargetType());
      Assert.assertEquals(4, r.getParameters().size());

      log.debug("now redo");
      List<TEntity> tlist2 = (List<TEntity>) ar.redo("do it");
      Assert.assertEquals(1, tlist2.size());
      Assert.assertEquals(tlist.get(0).getId(), tlist2.get(0).getId());

      TEntity te2 = (TEntity) q.getSingleResult();
      Assert.assertNotNull(te2);
      Assert.assertEquals(tlist.get(0).getId(), te2.getId());
      list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(2, list.size());
   }

   @Test
   public void queryNamedCalDate() throws ResourceApplyException {
      log.info("start queryNamedCalDate()");
      String qq = "SELECT a FROM TEntity a WHERE a.owner = ?1 AND a.xCaldate = ?2";
      sp = registerSetpoint(qq, ArchiveActuator.DEFAULTNAME, ControlEvent.INVOKE);

      Date now = new Date();
      Calendar cal = Calendar.getInstance();
      persistTEntity(5, now, cal);

      Query q = applEman.createQuery(qq);
      q.setParameter(1, TENANT);
      q.setParameter(2, cal, TemporalType.DATE);
      List<TEntity> tlist = q.getResultList();
      Assert.assertEquals(1, tlist.size());

      log.debug("entity.getId(): " + tlist.get(0).getId());

      List<Archive> list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Resource r = ar.getResource();
      Assert.assertEquals(qq, r.getTargetType());
      Assert.assertEquals(4, r.getParameters().size());

      log.debug("now redo");
      List<TEntity> tlist2 = (List<TEntity>) ar.redo("do it");
      Assert.assertEquals(1, tlist2.size());
      Assert.assertEquals(tlist.get(0).getId(), tlist2.get(0).getId());

      TEntity te2 = (TEntity) q.getSingleResult();
      Assert.assertNotNull(te2);
      Assert.assertEquals(tlist.get(0).getId(), te2.getId());
      list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(2, list.size());
   }

   @Test
   public void queryNamedCalTimestamp() throws ResourceApplyException {
      log.info("start queryNamedCalTimestamp()");
      String qq = "SELECT a FROM TEntity a WHERE a.owner = ?1 AND a.xCaltimestamp = ?2";
      sp = registerSetpoint(qq, ArchiveActuator.DEFAULTNAME, ControlEvent.INVOKE);

      Date now = new Date();
      Calendar cal = Calendar.getInstance();
      persistTEntity(5, now, cal);

      Query q = applEman.createQuery(qq);
      q.setParameter(1, TENANT);
      q.setParameter(2, cal, TemporalType.TIMESTAMP);
      List<TEntity> tlist = q.getResultList();
      Assert.assertEquals(1, tlist.size());

      log.debug("entity.getId(): " + tlist.get(0).getId());

      List<Archive> list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Resource r = ar.getResource();
      Assert.assertEquals(qq, r.getTargetType());
      Assert.assertEquals(4, r.getParameters().size());

      log.debug("now redo");
      List<TEntity> tlist2 = (List<TEntity>) ar.redo("do it");
      Assert.assertEquals(1, tlist2.size());
      Assert.assertEquals(tlist.get(0).getId(), tlist2.get(0).getId());

      TEntity te2 = (TEntity) q.getSingleResult();
      Assert.assertNotNull(te2);
      Assert.assertEquals(tlist.get(0).getId(), te2.getId());
      list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(2, list.size());
   }

   @Test
   public void namedTypedQuery() throws ResourceApplyException {
      log.info("start namedTypedQuery()");

      sp = registerSetpoint(TEntity.SEL_BY_OWNER, ArchiveActuator.DEFAULTNAME, ControlEvent.INVOKE, ControlEvent.REDO);

      persistTEntity();

      TypedQuery<TEntity> q = applEman.createNamedQuery(TEntity.SEL_BY_OWNER, TEntity.class);
      q.setParameter("owner", TENANT);
      List<TEntity> tlist = q.getResultList();
      Assert.assertEquals(1, tlist.size());

      log.debug("entity.getId(): " + tlist.get(0).getId());

      List<Archive> list = ArchiveLoader.loadArchives(TEntity.SEL_BY_OWNER);
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Resource r = ar.getResource();
      Assert.assertEquals(TEntity.SEL_BY_OWNER, r.getTargetType());
      Assert.assertEquals(4, r.getParameters().size());
      log.debug("now redo");
      List<TEntity> tlist2 = (List<TEntity>) ar.redo("do it");
      Assert.assertEquals(1, tlist2.size());
      Assert.assertEquals(tlist.get(0).getId(), tlist2.get(0).getId());

      list = ArchiveLoader.loadArchives(TEntity.SEL_BY_OWNER);
      Assert.assertEquals(2, list.size());

      TEntity te2 = q.getSingleResult();
      Assert.assertNotNull(te2);
      Assert.assertEquals(tlist.get(0).getId(), te2.getId());
      list = ArchiveLoader.loadArchives(TEntity.SEL_BY_OWNER);
      Assert.assertEquals(3, list.size());
   }

   @Test
   public void nativeQuery() throws ResourceApplyException {
      log.info("start nativeQuery()");
      String qq = "select COUNTER from CIB_TESTENTITY WHERE OWNER = ?1";
      sp = registerSetpoint(qq, ArchiveActuator.DEFAULTNAME, ControlEvent.INVOKE, ControlEvent.REDO);

      persistTEntity();

      Query q = applEman.createNativeQuery(qq);
      q.setParameter(1, TENANT);
      List<?> tlist = q.getResultList();
      Assert.assertEquals(1, tlist.size());
      Object count = tlist.get(0);
      if (count instanceof Integer) {
         Assert.assertEquals(5, (int) count);
      } else {
         Assert.assertEquals(5, ((BigDecimal) count).intValue());
      }

      List<Archive> list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Resource r = ar.getResource();
      Assert.assertEquals(qq, r.getTargetType());
      Assert.assertEquals(3, r.getParameters().size());
      log.debug("now redo");
      List<?> tlist2 = (List<?>) ar.redo("do it");
      Assert.assertEquals(1, tlist2.size());
      if (tlist2.get(0) instanceof BigDecimal) {
         Assert.assertEquals(5, ((BigDecimal) tlist2.get(0)).intValue());
      } else {
         Assert.assertEquals(5, ((Integer) tlist2.get(0)).intValue());
      }

      list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(2, list.size());

      Object te2 = q.getSingleResult();
      Assert.assertNotNull(te2);
      if (te2 instanceof Integer) {
         Assert.assertEquals(5, ((Integer) te2).intValue());
      } else {
         Assert.assertEquals(5, ((BigDecimal) te2).intValue());
      }

      list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(3, list.size());
   }

   @Test
   public void nativeTypedQuery() throws ResourceApplyException {
      log.info("start nativeTypedQuery()");
      String qq = "select * from CIB_TESTENTITY WHERE OWNER = ?1";
      sp = registerSetpoint(qq, ArchiveActuator.DEFAULTNAME, ControlEvent.INVOKE, ControlEvent.REDO);

      persistTEntity();

      Query q = applEman.createNativeQuery(qq, TEntity.class);
      q.setParameter(1, TENANT);
      List<TEntity> tlist = q.getResultList();
      Assert.assertEquals(1, tlist.size());
      Assert.assertTrue(tlist.get(0) instanceof TEntity);
      Assert.assertEquals(5, tlist.get(0).getCounter());

      List<Archive> list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Resource r = ar.getResource();
      Assert.assertEquals(qq, r.getTargetType());
      Assert.assertEquals(4, r.getParameters().size());
      log.debug("now redo");
      List<TEntity> tlist2 = (List<TEntity>) ar.redo("do it");
      Assert.assertEquals(1, tlist2.size());
      Assert.assertEquals(5, tlist2.get(0).getCounter());

      list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(2, list.size());

      TEntity te2 = (TEntity) q.getSingleResult();
      Assert.assertNotNull(te2);
      Assert.assertEquals(5, te2.getCounter());
      list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(3, list.size());
   }

   @Test
   public void nativeMappedQuery() throws ResourceApplyException {
      log.info("start nativeMappedQuery()");
      String qq = "select COUNTER AS mapped_counter, OWNER AS mapped_owner from CIB_TESTENTITY WHERE OWNER = ?1";
      sp = registerSetpoint("\"" + qq + "\"", ArchiveActuator.DEFAULTNAME, ControlEvent.INVOKE, ControlEvent.REDO);

      persistTEntity();

      Query q = applEman.createNativeQuery(qq, "TEntityRSMapping");
      q.setParameter(1, TENANT);
      List<Object[]> tlist = q.getResultList();
      Assert.assertEquals(1, tlist.size());
      Object[] objArray = tlist.get(0);
      for (int i = 0; i < objArray.length; i++) {
         log.debug(i + ": " + objArray[i]);
      }
      if (objArray[0] instanceof BigDecimal) {
         Assert.assertEquals(5, ((BigDecimal) objArray[0]).intValue());
      } else {
         Assert.assertEquals(5, objArray[0]);
      }

      Assert.assertEquals(TENANT, objArray[1]);

      List<Archive> list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Resource r = ar.getResource();
      Assert.assertEquals(qq, r.getTargetType());
      Assert.assertEquals(4, r.getParameters().size());
      log.debug("now redo");
      List<Object[]> tlist2 = (List<Object[]>) ar.redo("do it");
      Assert.assertEquals(1, tlist2.size());
      objArray = tlist2.get(0);
      if (objArray[0] instanceof BigDecimal) {
         Assert.assertEquals(5, ((BigDecimal) objArray[0]).intValue());
      } else {
         Assert.assertEquals(5, objArray[0]);
      }

      Assert.assertEquals(TENANT, objArray[1]);

      list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(2, list.size());

      Object[] te2 = (Object[]) q.getSingleResult();
      Assert.assertNotNull(te2);
      if (te2[0] instanceof BigDecimal) {
         Assert.assertEquals(5, ((BigDecimal) te2[0]).intValue());
      } else {
         Assert.assertEquals(5, te2[0]);
      }

      Assert.assertEquals(TENANT, te2[1]);
      list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(3, list.size());
   }

   @Test
   public void typedQuery() throws ResourceApplyException {
      log.info("start typedQuery()");
      String qq = "SELECT a FROM TEntity a WHERE a.owner = :owner";
      sp = registerSetpoint(qq, ArchiveActuator.DEFAULTNAME, ControlEvent.INVOKE, ControlEvent.REDO);

      persistTEntity();

      TypedQuery<TEntity> q = applEman.createQuery(qq, TEntity.class);
      q.setParameter("owner", TENANT);
      List<TEntity> tlist = q.getResultList();
      Assert.assertEquals(1, tlist.size());
      Assert.assertTrue(tlist.get(0) instanceof TEntity);
      Assert.assertEquals(5, tlist.get(0).getCounter());

      List<Archive> list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Resource r = ar.getResource();
      Assert.assertEquals(qq, r.getTargetType());
      Assert.assertEquals(4, r.getParameters().size());
      log.debug("now redo");
      List<TEntity> tlist2 = (List<TEntity>) ar.redo("do it");
      Assert.assertEquals(1, tlist2.size());
      Assert.assertEquals(5, tlist2.get(0).getCounter());

      list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(2, list.size());

      TEntity te2 = (TEntity) q.getSingleResult();
      Assert.assertNotNull(te2);
      Assert.assertEquals(5, te2.getCounter());
      list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(3, list.size());
   }

   @Test
   public void typedQuery4Eyes() throws ResourceApplyException {
      log.info("start typedQuery4Eyes()");
      String qq = "SELECT a FROM TEntity a WHERE a.owner = :owner";
      sp = registerSetpoint(qq, FourEyesActuator.DEFAULTNAME, ControlEvent.INVOKE, ControlEvent.RELEASE);

      persistTEntity();

      TypedQuery<TEntity> q = applEman.createQuery(qq, TEntity.class);
      q.setParameter("owner", TENANT);
      List<TEntity> tlist = q.getResultList();
      Assert.assertEquals(0, tlist.size());

      List<DcControllable> list = DcLoader.findUnreleased(qq);
      Assert.assertEquals(1, list.size());
      DcControllable ar = list.get(0);
      Resource r = ar.getResource();
      Assert.assertEquals(qq, r.getTargetType());
      Assert.assertEquals(4, r.getParameters().size());

      log.debug("now release");
      Context.sessionScope().setUser("other");
      List<TEntity> tlist2 = (List<TEntity>) ar.release(applEman, "okay");
      Assert.assertEquals(1, tlist2.size());
      Assert.assertEquals(5, tlist2.get(0).getCounter());

      list = list = DcLoader.findUnreleased(qq);
      Assert.assertEquals(0, list.size());
   }

   @Test
   public void typedQuery4EyesSingleResult() throws ResourceApplyException {
      log.info("start typedQuery4EyesSingleResult()");
      String qq = "SELECT a FROM TEntity a WHERE a.owner = :owner";
      sp = registerSetpoint(qq, FourEyesActuator.DEFAULTNAME, ControlEvent.INVOKE, ControlEvent.RELEASE);

      persistTEntity();

      TypedQuery<TEntity> q = applEman.createQuery(qq, TEntity.class);
      q.setParameter("owner", TENANT);
      try {
         q.getSingleResult();
         Assert.fail();
      } catch (NoResultException e) {
      }

      List<DcControllable> list = DcLoader.findUnreleased(qq);
      Assert.assertEquals(1, list.size());
      DcControllable ar = list.get(0);
      Resource r = ar.getResource();
      Assert.assertEquals(qq, r.getTargetType());
      Assert.assertEquals(4, r.getParameters().size());

      log.debug("now release");
      Context.sessionScope().setUser("other");
      TEntity te2 = (TEntity) ar.release(applEman, "okay");
      Assert.assertNotNull(te2);
      Assert.assertEquals(5, te2.getCounter());

      list = list = DcLoader.findUnreleased(qq);
      Assert.assertEquals(0, list.size());
   }

   @Test
   public void typedQuery4EyesNoSingleResult() throws ResourceApplyException {
      log.info("start typedQuery4EyesNoSingleResult()");
      String qq = "SELECT a FROM TEntity a WHERE a.owner = :owner";
      sp = registerSetpoint(qq, FourEyesActuator.DEFAULTNAME, ControlEvent.INVOKE, ControlEvent.RELEASE);

      TypedQuery<TEntity> q = applEman.createQuery(qq, TEntity.class);
      q.setParameter("owner", TENANT);
      try {
         q.getSingleResult();
         Assert.fail();
      } catch (NoResultException e) {
      }

      List<DcControllable> list = DcLoader.findUnreleased(qq);
      Assert.assertEquals(1, list.size());
      DcControllable ar = list.get(0);
      Resource r = ar.getResource();
      Assert.assertEquals(qq, r.getTargetType());
      Assert.assertEquals(4, r.getParameters().size());

      log.debug("now release");
      Context.sessionScope().setUser("other");
      try {
         ar.release(applEman, "okay");
         Assert.fail();
      } catch (NoResultException e) {
      }
   }

   @Test
   public void typedQuery4EyesMultiSingleResult() throws ResourceApplyException {
      log.info("start typedQuery4EyesMultiSingleResult()");
      String qq = "SELECT a FROM TEntity a WHERE a.owner = :owner";
      sp = registerSetpoint(qq, FourEyesActuator.DEFAULTNAME, ControlEvent.INVOKE, ControlEvent.RELEASE);

      persistTEntity();
      persistTEntity();

      TypedQuery<TEntity> q = applEman.createQuery(qq, TEntity.class);
      q.setParameter("owner", TENANT);
      try {
         q.getSingleResult();
         Assert.fail();
      } catch (NoResultException e) {
      }

      List<DcControllable> list = DcLoader.findUnreleased(qq);
      Assert.assertEquals(1, list.size());
      DcControllable ar = list.get(0);
      Resource r = ar.getResource();
      Assert.assertEquals(qq, r.getTargetType());
      Assert.assertEquals(4, r.getParameters().size());

      log.debug("now release");
      Context.sessionScope().setUser("other");
      try {
         ar.release(applEman, "okay");
         Assert.fail();
      } catch (NonUniqueResultException e) {
      }
   }

   @Test
   public void queryResultListWithException() throws ResourceApplyException {
      log.info("start queryResultListWithException()");
      String qq = "DELETE FROM TEntity a WHERE a.owner = :owner";
      sp = registerSetpoint(qq, ArchiveActuator.DEFAULTNAME, ControlEvent.INVOKE, ControlEvent.REDO);

      Query q = applEman.createQuery(qq);
      q.setParameter("owner", "nonexistant");
      try {
         q.getResultList();
         Assert.fail();
      } catch (IllegalStateException e) {
      }

      List<Archive> list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Assert.assertEquals(ExecutionStatus.ERROR, ar.getExecutionStatus());
      Resource r = ar.getResource();
      Assert.assertEquals(qq, r.getTargetType());
      Assert.assertEquals(3, r.getParameters().size());

      log.debug("now redo");
      try {
         ar.redo("do it");
         Assert.fail();
      } catch (IllegalStateException e) {
      }

      list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(2, list.size());
      ar = list.get(1);
      Assert.assertEquals(ExecutionStatus.ERROR, ar.getExecutionStatus());
   }

   @Test
   public void querySingleWithException() throws ResourceApplyException {
      log.info("start querySingleWithException()");
      String qq = "SELECT a FROM TEntity a WHERE a.owner = :owner";
      sp = registerSetpoint(qq, ArchiveActuator.DEFAULTNAME, ControlEvent.INVOKE, ControlEvent.REDO);

      Query q = applEman.createQuery(qq);
      q.setParameter("owner", "nonexistant");
      try {
         q.getSingleResult();
         Assert.fail();
      } catch (NoResultException e) {
      }

      List<Archive> list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Assert.assertEquals(ExecutionStatus.EXECUTED, ar.getExecutionStatus());
      Resource r = ar.getResource();
      Assert.assertEquals(qq, r.getTargetType());
      Assert.assertEquals(3, r.getParameters().size());

      log.debug("now redo");
      try {
         ar.redo("do it");
         Assert.fail();
      } catch (NoResultException e) {
      }

      list = ArchiveLoader.loadArchives(qq);
      Assert.assertEquals(2, list.size());
      ar = list.get(1);
      Assert.assertEquals(ExecutionStatus.EXECUTED, ar.getExecutionStatus());
   }

}
