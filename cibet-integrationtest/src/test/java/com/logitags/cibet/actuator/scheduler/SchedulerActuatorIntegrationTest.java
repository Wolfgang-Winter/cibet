/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2014 Dr. Wolfgang Winter
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
package com.logitags.cibet.actuator.scheduler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import com.cibethelper.SchedulerExceptionIntercept;
import com.cibethelper.base.DBHelper;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.archive.ArchiveLoader;
import com.logitags.cibet.actuator.common.PostponedException;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.actuator.dc.FourEyesActuator;
import com.logitags.cibet.actuator.dc.ResourceApplyException;
import com.logitags.cibet.actuator.dc.UnapprovedResourceException;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.ConfigurationService;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.diff.Difference;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.sensor.pojo.PojoInvoker;

/**
 * -javaagent:D:\Java\maven-repository\org\aspectj\aspectjweaver\1.8.8\aspectjweaver-1.8.8.jar
 * 
 * @author Wolfgang
 *
 */
public class SchedulerActuatorIntegrationTest extends DBHelper {

   private static Logger log = Logger.getLogger(SchedulerActuatorIntegrationTest.class);

   @AfterClass
   public static void afterClassSchedulerActuatorIntegrationTest() {
      Configuration.instance().close();
   }

   @After
   public void subDoAfter() {
      new ConfigurationService().initialise();
   }

   private void release() throws ResourceApplyException {
      Context.internalRequestScope().getEntityManager().clear();
      List<DcControllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());

      Resource res = l.get(0).getResource();
      log.debug("size: " + res.getParameters().size());

      Context.sessionScope().setUser("test2");
      l.get(0).release(Context.requestScope().getEntityManager(), null);
      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());
      Context.sessionScope().setUser(USER);
   }

   @Test
   public void noSchedule() throws Exception {
      log.info("start noSchedule()");

      registerSetpoint(TEntity.class, SchedulerActuator.DEFAULTNAME, ControlEvent.INSERT, ControlEvent.RELEASE);
      Context.requestScope().setRemark("created");

      TEntity ent = persistTEntity();
      Assert.assertTrue(ent.getId() != 0);

      TEntity te = applEman.find(TEntity.class, ent.getId());
      Assert.assertNotNull(te);
      Context.requestScope().setRemark(null);
   }

   @Test
   public void schedule() throws Exception {
      log.info("start schedule()");

      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.SECOND, 4);
      SchedulerActuator sa = new SchedulerActuator("sa3");
      sa.setTimerStart(cal.getTime());
      sa.setPersistenceUnit("localTest");
      Configuration.instance().registerActuator(sa);

      registerSetpoint(TEntity.class, "sa3", ControlEvent.INSERT, ControlEvent.RELEASE);
      Context.requestScope().setRemark("created");
      Context.requestScope().setScheduledDate(Calendar.SECOND, 2);

      TEntity ent = persistTEntity();

      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      Assert.assertEquals(0, ent.getId());

      List<DcControllable> l = SchedulerLoader.findScheduled(TEntity.class.getName());
      Assert.assertEquals(1, l.size());
      DcControllable co = l.get(0);
      Assert.assertEquals("created", co.getCreateRemark());
      Assert.assertEquals(ExecutionStatus.SCHEDULED, co.getExecutionStatus());
      log.debug(co.getScheduledDate());
      Assert.assertNotNull(co.getScheduledDate());

      Context.requestScope().getEntityManager().getTransaction().commit();
      Context.requestScope().getEntityManager().getTransaction().begin();
      Context.requestScope().getEntityManager().clear();

      log.debug("-------------------- sleep");
      Thread.sleep(10000);
      log.debug("--------------- after TimerTask");
      Context.internalRequestScope().getEntityManager().flush();
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      Query q = applEman.createQuery("SELECT t FROM TEntity t");
      List<TEntity> tlist = q.getResultList();
      Assert.assertEquals(1, tlist.size());
      applEman.remove(tlist.get(0));

      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      applEman.clear();

      l = SchedulerLoader.loadByUser(USER);
      Assert.assertEquals(1, l.size());
      co = l.get(0);
      Assert.assertEquals("sa3", co.getActuator());
      Assert.assertEquals(ExecutionStatus.EXECUTED, co.getExecutionStatus());
      Assert.assertNotNull(co.getExecutionDate());
      Assert.assertNotNull(co.getResource().getPrimaryKeyId());
   }

   @Test
   public void schedule2() throws Exception {
      log.info("start schedule2()");

      registerSetpoint(TEntity.class, SchedulerActuator.DEFAULTNAME, ControlEvent.INSERT, ControlEvent.RELEASE);
      Context.requestScope().setRemark("created");
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.DATE, 2);
      Context.requestScope().setScheduledDate(cal.getTime());

      TEntity ent = persistTEntity();
      Assert.assertEquals(0, ent.getId());

      List<DcControllable> l = SchedulerLoader.findScheduled();
      Assert.assertEquals(1, l.size());
      DcControllable co = l.get(0);
      Assert.assertEquals("created", co.getCreateRemark());
      log.debug(co.getScheduledDate());
      log.debug("uniqueId: " + co.getResource().getUniqueId());
      Assert.assertNotNull(co.getScheduledDate());

      Map<DcControllable, List<Difference>> map = SchedulerLoader.scheduledDifferences(ent);
      Assert.assertEquals(1, map.size());
      Entry<DcControllable, List<Difference>> entry = map.entrySet().iterator().next();
      Assert.assertNull(entry.getValue());
   }

   @Test
   public void schedule4EyesNoSched() throws Exception {
      log.info("start schedule4EyesNoSched()");

      List<String> acts = new ArrayList<>();
      acts.add(SchedulerActuator.DEFAULTNAME);
      acts.add(FourEyesActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), acts, ControlEvent.INSERT, ControlEvent.RELEASE);
      Context.requestScope().setRemark("created");

      TEntity ent = persistTEntity();
      Assert.assertEquals(0, ent.getId());

      List<DcControllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());
      DcControllable co = l.get(0);
      Assert.assertEquals("created", co.getCreateRemark());

      List<DcControllable> sl = SchedulerLoader.findScheduled();
      Assert.assertEquals(0, sl.size());

      Context.sessionScope().setUser("test2");
      Object res = co.release(applEman, null);
      Assert.assertNotNull(res);
      Assert.assertTrue(res instanceof TEntity);
      Assert.assertTrue(((TEntity) res).getId() != 0);

      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      sl = SchedulerLoader.findScheduled();
      Assert.assertEquals(0, sl.size());

      Context.sessionScope().setUser(USER);
      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      TEntity te = applEman.find(TEntity.class, ((TEntity) res).getId());
      Assert.assertNotNull(te);
   }

   @Test
   public void schedule4Eyes() throws Exception {
      log.info("start schedule4Eyes()");

      List<String> acts = new ArrayList<>();
      acts.add(SchedulerActuator.DEFAULTNAME);
      acts.add(FourEyesActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), acts, ControlEvent.INSERT, ControlEvent.RELEASE);
      Context.requestScope().setRemark("created");
      Context.requestScope().setScheduledDate(Calendar.DATE, 2);

      TEntity ent = persistTEntity();
      Assert.assertEquals(0, ent.getId());
      log.debug("Context.requestScope().getExecutedEventResult(): " + Context.requestScope().getExecutedEventResult());

      List<DcControllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());
      DcControllable co = l.get(0);
      Assert.assertEquals("created", co.getCreateRemark());

      List<DcControllable> sl = SchedulerLoader.findScheduled();
      Assert.assertEquals(0, sl.size());

      Context.sessionScope().setUser("test2");
      Object res = co.release(applEman, null);
      log.debug("Context.requestScope().getExecutedEventResult(): " + Context.requestScope().getExecutedEventResult());

      Assert.assertNotNull(res);
      Assert.assertTrue(res instanceof TEntity);
      Assert.assertTrue(((TEntity) res).getId() == 0);

      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      sl = SchedulerLoader.findScheduled();
      Assert.assertEquals(1, sl.size());
      Assert.assertEquals("SCHEDULER", sl.get(0).getActuator());

      Context.sessionScope().setUser(USER);
      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());
      sl = SchedulerLoader.findScheduled();
      Assert.assertEquals(1, sl.size());
   }

   @Test
   public void invoke4Eyes() throws Exception {
      log.info("start invoke4Eyes");
      Context.sessionScope().setTenant(TENANT);
      TComplexEntity ent1 = new TComplexEntity();
      ent1.setStatValue(55);

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(SchedulerActuator.DEFAULTNAME);
      registerSetpoint(TComplexEntity.class, schemes, "setStatValue", ControlEvent.INVOKE, ControlEvent.RELEASE_INVOKE);
      Context.requestScope().setScheduledDate(Calendar.DATE, 2);

      ent1.setStatValue(3434);
      applEman.flush();

      Query q = Context.requestScope().getEntityManager().createNamedQuery(DcControllable.SEL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      DcControllable co = (DcControllable) q.getSingleResult();
      Assert.assertNotNull(co);
      Resource res = co.getResource();
      Assert.assertEquals("setStatValue", res.getMethod());
      Assert.assertEquals(PojoInvoker.class.getName(), res.getInvokerClass());
      log.debug("size: " + res.getParameters().size());

      Assert.assertEquals(55, ent1.getStatValue());
      release();

      Assert.assertEquals(55, ent1.getStatValue());

      List<DcControllable> sl = SchedulerLoader.findScheduled();
      Assert.assertEquals(1, sl.size());
      Assert.assertEquals("SCHEDULER", sl.get(0).getActuator());
   }

   @Test
   public void scheduleDelete() throws Exception {
      log.info("start scheduleDelete()");

      registerSetpoint(TEntity.class, SchedulerActuator.DEFAULTNAME, ControlEvent.DELETE, ControlEvent.RELEASE);
      Context.requestScope().setRemark("created");
      Context.requestScope().setScheduledDate(Calendar.DATE, 2);

      TEntity ent = persistTEntity();
      Assert.assertTrue(ent.getId() != 0);

      List<DcControllable> l = SchedulerLoader.findScheduled();
      Assert.assertEquals(0, l.size());

      applEman.remove(ent);
      TEntity te = applEman.find(TEntity.class, ent.getId());
      Assert.assertNotNull(te);

      List<DcControllable> sl = SchedulerLoader.findScheduled();
      Assert.assertEquals(1, sl.size());
   }

   @Test
   public void scheduleDelete2() throws Exception {
      log.info("start scheduleDelete2()");
      Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());

      SchedulerActuator sched = (SchedulerActuator) Configuration.instance().getActuator(SchedulerActuator.DEFAULTNAME);
      sched.setAutoRemoveScheduledDate(false);
      registerSetpoint(TEntity.class, SchedulerActuator.DEFAULTNAME, ControlEvent.DELETE, ControlEvent.UPDATE,
            ControlEvent.RELEASE);
      Context.requestScope().setRemark("created");

      TEntity ent = persistTEntity();
      Assert.assertTrue(ent.getId() != 0);

      Context.requestScope().setScheduledDate(Calendar.DATE, 2);
      applEman.remove(ent);
      TEntity te = applEman.find(TEntity.class, ent.getId());
      Assert.assertNotNull(te);

      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      List<DcControllable> sl = SchedulerLoader.findScheduled();
      Assert.assertEquals(1, sl.size());

      ent.setCounter(67);
      try {
         applEman.merge(ent);
      } catch (UnapprovedResourceException e) {
         Assert.fail();
      }

      sl = SchedulerLoader.findScheduled();
      Assert.assertEquals(2, sl.size());
   }

   @Test
   public void scheduleUpdate() throws Exception {
      log.info("start scheduleUpdate()");
      Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());

      registerSetpoint(TEntity.class, SchedulerActuator.DEFAULTNAME, ControlEvent.UPDATE, ControlEvent.RELEASE);
      Context.requestScope().setRemark("created");

      Context.requestScope().setScheduledDate(Calendar.DATE, 2);

      TEntity ent = persistTEntity();
      Assert.assertTrue(ent.getId() != 0);

      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      List<DcControllable> l = SchedulerLoader.findScheduled();
      Assert.assertEquals(0, l.size());

      ent.setCounter(5656);
      ent = applEman.merge(ent);
      TEntity te = applEman.find(TEntity.class, ent.getId());
      Assert.assertNotNull(te);
      Assert.assertEquals(5, te.getCounter());

      List<DcControllable> sl = SchedulerLoader.findScheduled();
      Assert.assertEquals(1, sl.size());
   }

   @Test
   public void scheduleStoredProperties() throws Exception {
      log.info("start scheduleStoredProperties()");
      Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());

      List<String> sp1 = new ArrayList<>();
      sp1.add("nameValue");
      FourEyesActuator act4 = new FourEyesActuator("4e");
      act4.setStoredProperties(sp1);
      Configuration.instance().registerActuator(act4);

      List<String> sp2 = new ArrayList<>();
      sp2.add("counter");
      SchedulerActuator actS = new SchedulerActuator("sc");
      actS.setStoredProperties(sp2);
      Configuration.instance().registerActuator(actS);

      List<String> schemes = new ArrayList<String>();
      schemes.add("4e");
      schemes.add("sc");
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.UPDATE, ControlEvent.RELEASE);
      Context.requestScope().setRemark("created");
      Context.requestScope().setScheduledDate(Calendar.DATE, 2);

      TEntity ent = persistTEntity();
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      Assert.assertTrue(ent.getId() != 0);

      List<DcControllable> l = SchedulerLoader.findScheduled();
      Assert.assertEquals(0, l.size());

      ent.setCounter(5656);
      ent = applEman.merge(ent);
      applEman.flush();

      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      TEntity te = applEman.find(TEntity.class, ent.getId());
      Assert.assertNotNull(te);
      Assert.assertEquals(5, te.getCounter());

      Map<String, Object> properties = new HashMap<String, Object>();
      properties.put("nameValue", "valuexx");
      List<DcControllable> sl = SchedulerLoader.loadByProperties(TEntity.class, properties);
      Assert.assertEquals(1, sl.size());

      Context.sessionScope().setUser("test2");
      TEntity res = (TEntity) sl.get(0).release(applEman, null);

      Assert.assertNotNull(res);
      Assert.assertEquals(5, te.getCounter());

      // applEman.getTransaction().commit();
      // InitializationService.instance().endContext();

      // InitializationService.instance().endContext();
      // InitializationService.instance().startContext();
      // Context.sessionScope().setUser(USER);
      // Context.sessionScope().setTenant(TENANT);

      properties.put("counter", 5656);
      sl = DcLoader.loadByProperties(TEntity.class, properties);
      if (GLASSFISH.equals(APPSERVER)) {
         // bug in eclipselink: 2 ResourceParameters are added at release, but

         // Parameter name: nameValue, classname: java.lang.String, value: valuexx
         // Parameter name: __DIFFERENCES, classname: java.util.ArrayList, value: [propertyName=counter ;
         // propertyPath=/counter ; propertyType=int ; differenceType=MODIFIED ; oldValue=5 ; newValue=5656 ;
         // canonicalPath: counter]
         // added: Parameter name: counter, classname: int, value: 5656
         // added: Parameter name: __CLEAN_OBJECT, classname: com.cibethelper.entities.TEntity, value: TEntity id:
         // 19251, counter: 5, owner: testTenant, xCaltimestamp: null

         // UPDATE CIB_RESOURCEPARAMETER SET dcControllableId = ? WHERE (PARAMETERID = ?)
         // bind => [34107bd3-9793-4928-bc57-4d65c22001f2, null]
         // UPDATE CIB_RESOURCEPARAMETER SET dcControllableId = ? WHERE (PARAMETERID = ?)
         // bind => [34107bd3-9793-4928-bc57-4d65c22001f2, null]

         return;
      }
      Assert.assertEquals(1, sl.size());
      DcControllable sl1 = sl.get(0);
      Assert.assertEquals(ExecutionStatus.SCHEDULED, sl1.getExecutionStatus());

   }

   @Test
   public void scheduleMergeUpdate() throws Exception {
      log.info("start scheduleMergeUpdate()");
      Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());

      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.SECOND, 4);
      SchedulerActuator sa = new SchedulerActuator("sm1");
      sa.setTimerStart(cal.getTime());
      sa.setPersistenceUnit("localTest");
      sa.setThrowPostponedException(true);
      Configuration.instance().registerActuator(sa);

      registerSetpoint(TComplexEntity.class, "sm1", ControlEvent.UPDATE);
      Context.requestScope().setRemark("created");
      Context.requestScope().setScheduledDate(Calendar.SECOND, 2);

      TEntity t1 = new TEntity("Stung1", 1, "owner1");
      TEntity t2 = new TEntity("Stung2", 2, "owner2");
      TEntity t3 = new TEntity("Stung3", 3, "owner3");
      TEntity t4 = new TEntity("Stung4", 4, "owner4");

      TComplexEntity base = new TComplexEntity();
      base.setCompValue(45);
      base.setTen(t1);
      base.getLazyList().add(t2);
      base.getLazyList().add(t3);

      applEman.persist(base);

      applEman.getTransaction().commit();
      applEman.clear();
      applEman.getTransaction().begin();

      TComplexEntity base2 = applEman.find(TComplexEntity.class, base.getId());
      Assert.assertNotNull(base2);

      base2.getEagerList().add(t4);
      base2.setOwner("base2owner");
      try {
         base2 = applEman.merge(base2);
         Assert.fail();
      } catch (PostponedException e) {
      }

      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      Context.requestScope().getEntityManager().getTransaction().commit();
      Context.requestScope().getEntityManager().getTransaction().begin();

      List<DcControllable> l = SchedulerLoader.findScheduled();
      Assert.assertEquals(1, l.size());
      DcControllable co = l.get(0);
      Assert.assertEquals("created", co.getCreateRemark());
      Assert.assertEquals(ExecutionStatus.SCHEDULED, co.getExecutionStatus());
      log.debug(co.getScheduledDate());
      Assert.assertNotNull(co.getScheduledDate());

      Context.requestScope().getEntityManager().clear();

      log.debug("-------------------- sleep");
      Thread.sleep(10000);
      log.debug("--------------- after TimerTask");
      Context.internalRequestScope().getEntityManager().flush();
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      Query q = applEman.createQuery("SELECT t FROM TComplexEntity t");
      List<TComplexEntity> tlist = q.getResultList();
      Assert.assertEquals(1, tlist.size());
      TComplexEntity te2 = tlist.get(0);
      Assert.assertEquals(1, te2.getEagerList().size());
      Assert.assertEquals(2, te2.getLazyList().size());
      Assert.assertEquals("base2owner", te2.getOwner());

      applEman.remove(te2);
      applEman.flush();

      q = applEman.createQuery("SELECT t FROM TEntity t");
      List<TEntity> telist = q.getResultList();
      for (TEntity t : telist) {
         applEman.remove(t);
      }

      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      applEman.clear();

      l = SchedulerLoader.loadByUser(USER);
      Assert.assertEquals(1, l.size());
      co = l.get(0);
      Assert.assertEquals("sm1", co.getActuator());
      Assert.assertEquals(ExecutionStatus.EXECUTED, co.getExecutionStatus());
      Assert.assertNotNull(co.getExecutionDate());
      Assert.assertEquals(String.valueOf(te2.getId()), co.getResource().getPrimaryKeyId());
   }

   @Test
   public void scheduleMergeUpdate2() throws Exception {
      log.info("start scheduleMergeUpdate2()");
      Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());

      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.SECOND, 4);
      SchedulerActuator sa = new SchedulerActuator("sm1");
      sa.setTimerStart(cal.getTime());
      sa.setPersistenceUnit("localTest");
      sa.setAutoRemoveScheduledDate(true);
      Configuration.instance().registerActuator(sa);

      registerSetpoint(TComplexEntity.class, "sm1", ControlEvent.UPDATE);
      Context.requestScope().setRemark("created");
      Context.requestScope().setScheduledDate(Calendar.SECOND, 2);

      TEntity t1 = new TEntity("Stung1", 1, "owner1");
      TEntity t2 = new TEntity("Stung2", 2, "owner2");
      TEntity t3 = new TEntity("Stung3", 3, "owner3");
      TEntity t4 = new TEntity("Stung4", 4, "owner4");
      TEntity t5 = new TEntity("Stung5", 5, "owner5");

      TComplexEntity base = new TComplexEntity();
      base.setCompValue(45);
      base.setTen(t1);
      base.getLazyList().add(t2);
      base.getLazyList().add(t3);

      applEman.persist(base);
      long t1id = t1.getId();
      Assert.assertTrue(t1id != 0);

      applEman.getTransaction().commit();
      applEman.clear();
      applEman.getTransaction().begin();

      TComplexEntity base2 = applEman.find(TComplexEntity.class, base.getId());
      Assert.assertNotNull(base2);

      base2.getEagerList().add(t5);
      base2.setOwner("base2owner");
      base2 = applEman.merge(base2);
      EventResult result = Context.requestScope().getExecutedEventResult();
      Assert.assertEquals(ExecutionStatus.SCHEDULED, result.getExecutionStatus());

      applEman.getTransaction().commit();
      applEman.clear();
      applEman.getTransaction().begin();

      List<DcControllable> l = SchedulerLoader.findScheduled();
      Assert.assertEquals(1, l.size());
      DcControllable co = l.get(0);
      Assert.assertEquals("created", co.getCreateRemark());
      Assert.assertEquals(ExecutionStatus.SCHEDULED, co.getExecutionStatus());
      log.debug(co.getScheduledDate());
      Assert.assertNotNull(co.getScheduledDate());

      Map<DcControllable, List<Difference>> map = SchedulerLoader.scheduledDifferences(base2);
      Assert.assertEquals(1, map.size());
      Entry<DcControllable, List<Difference>> entry = map.entrySet().iterator().next();
      Assert.assertNotNull(entry.getValue());
      Assert.assertEquals(2, entry.getValue().size());

      TComplexEntity base3 = applEman.find(TComplexEntity.class, base.getId());
      Assert.assertNotNull(base3);
      base3.setTen(t4);
      base3.setStatValue(111);
      base3 = applEman.merge(base3);

      result = Context.requestScope().getExecutedEventResult();
      Assert.assertEquals(ExecutionStatus.EXECUTED, result.getExecutionStatus());

      l = SchedulerLoader.findScheduled();
      Assert.assertEquals(1, l.size());

      map = SchedulerLoader.scheduledDifferences(base2);
      Assert.assertEquals(1, map.size());

      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      applEman.clear();
      Context.requestScope().getEntityManager().getTransaction().commit();
      Context.requestScope().getEntityManager().getTransaction().begin();
      Context.requestScope().getEntityManager().clear();

      log.debug("-------------------- sleep");
      Thread.sleep(10000);
      log.debug("--------------- after TimerTask");
      Query q = applEman.createQuery("SELECT t FROM TComplexEntity t");
      List<TComplexEntity> tlist = q.getResultList();
      Assert.assertEquals(1, tlist.size());
      TComplexEntity te2 = tlist.get(0);
      Assert.assertEquals(1, te2.getEagerList().size());
      Assert.assertEquals(2, te2.getLazyList().size());
      Assert.assertEquals("base2owner", te2.getOwner());
      Assert.assertTrue(te2.getTen().getId() != t1id);
      Assert.assertEquals("owner4", te2.getTen().getOwner());
      Assert.assertEquals(111, te2.getStatValue());

      Context.internalRequestScope().getEntityManager().clear();

      l = SchedulerLoader.loadByUser(USER);
      Assert.assertEquals(1, l.size());
      co = l.get(0);
      Assert.assertEquals("sm1", co.getActuator());
      Assert.assertEquals(ExecutionStatus.EXECUTED, co.getExecutionStatus());
      Assert.assertNotNull(co.getExecutionDate());
      Assert.assertEquals(String.valueOf(te2.getId()), co.getResource().getPrimaryKeyId());
   }

   @Test
   public void scheduleUpdateScheduledException() throws Exception {
      log.info("start scheduleUpdateScheduledException()");
      Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());

      SchedulerActuator sa = new SchedulerActuator("sm8");
      sa.setPersistenceUnit("localTest");
      sa.setThrowPostponedException(true);
      sa.setThrowScheduledException(true);
      sa.setAutoRemoveScheduledDate(false);
      Configuration.instance().registerActuator(sa);

      registerSetpoint(TComplexEntity.class, "sm8", ControlEvent.UPDATE);
      Context.requestScope().setScheduledDate(Calendar.SECOND, 20);

      TEntity t1 = new TEntity("Stung1", 1, "owner1");
      TEntity t2 = new TEntity("Stung2", 2, "owner2");
      TEntity t3 = new TEntity("Stung3", 3, "owner3");
      TEntity t4 = new TEntity("Stung4", 4, "owner4");

      TComplexEntity base = new TComplexEntity();
      base.setCompValue(45);
      base.setTen(t1);
      base.getLazyList().add(t2);
      base.getLazyList().add(t3);

      applEman.persist(base);

      applEman.getTransaction().commit();
      applEman.clear();
      applEman.getTransaction().begin();

      TComplexEntity base2 = applEman.find(TComplexEntity.class, base.getId());
      Assert.assertNotNull(base2);

      base2.getEagerList().add(t4);
      base2.setOwner("base2owner");
      try {
         base2 = applEman.merge(base2);
         Assert.fail();
      } catch (PostponedException e) {
      }

      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      applEman.clear();
      TComplexEntity base3 = applEman.find(TComplexEntity.class, base.getId());

      log.debug("2. merge");
      base3.setCompValue(177);
      try {
         base3 = applEman.merge(base3);
         Assert.fail();
      } catch (ScheduledException e) {
         Assert.assertEquals(1, e.getScheduledDcControllables().size());
         DcControllable dc = e.getScheduledDcControllables().iterator().next();
         List<Difference> diffs = e.getDifferences(dc);
         for (Difference d : diffs) {
            log.debug("diffff: " + d);
         }
         Assert.assertEquals(2, diffs.size());
      }
   }

   @Test
   public void schedule4EyesWithTimerExecution() throws Exception {
      log.info("start schedule4EyesWithTimerExecution()");
      Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());

      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.SECOND, 4);
      SchedulerActuator sa = new SchedulerActuator("sm1");
      sa.setTimerStart(cal.getTime());
      sa.setPersistenceUnit("localTest");
      sa.setAutoRemoveScheduledDate(true);
      Configuration.instance().registerActuator(sa);

      List<String> acts = new ArrayList<>();
      acts.add("sm1");
      acts.add(FourEyesActuator.DEFAULTNAME);
      acts.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), acts, ControlEvent.UPDATE, ControlEvent.RELEASE);
      Context.requestScope().setRemark("created");
      Context.requestScope().setScheduledDate(Calendar.SECOND, 2);

      TEntity ent = persistTEntity();
      long t1id = ent.getId();
      Assert.assertTrue(t1id != 0);
      applEman.getTransaction().commit();
      applEman.clear();
      applEman.getTransaction().begin();

      ent.setCounter(12345);
      TEntity base2 = applEman.merge(ent);
      EventResult result = Context.requestScope().getExecutedEventResult();
      Assert.assertEquals(ExecutionStatus.POSTPONED, result.getExecutionStatus());

      List<DcControllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());
      DcControllable co = l.get(0);
      Assert.assertEquals("created", co.getCreateRemark());

      List<DcControllable> sl = SchedulerLoader.findScheduled();
      Assert.assertEquals(0, sl.size());

      Context.sessionScope().setUser("test2");
      Object res = co.release(applEman, null);
      log.debug("Context.requestScope().getExecutedEventResult(): " + Context.requestScope().getExecutedEventResult());

      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      Assert.assertNotNull(res);
      Assert.assertTrue(res instanceof TEntity);

      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      sl = SchedulerLoader.findScheduled();
      Assert.assertEquals(1, sl.size());
      Assert.assertEquals("sm1", sl.get(0).getActuator());

      Context.sessionScope().setUser(USER);
      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());
      sl = SchedulerLoader.findScheduled();
      Assert.assertEquals(1, sl.size());

      Context.requestScope().getEntityManager().getTransaction().commit();
      Context.requestScope().getEntityManager().getTransaction().begin();
      Context.requestScope().getEntityManager().clear();

      log.debug("-------------------- sleep");
      Thread.sleep(10000);
      log.debug("--------------- after TimerTask");
      Context.internalRequestScope().getEntityManager().flush();
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      applEman.clear();

      TEntity te = applEman.find(TEntity.class, t1id);
      Assert.assertNotNull(te);

      if (GLASSFISH.equals(APPSERVER)) {
         // EclipseLink bug, see comments in scheduleStoredProperties()
         return;
      }
      Assert.assertEquals(12345, te.getCounter());

      sl = SchedulerLoader.findScheduled();
      Assert.assertEquals(0, sl.size());

      sl = SchedulerLoader.loadByUser(USER);
      Assert.assertEquals(1, sl.size());
      co = sl.get(0);
      Assert.assertEquals("sm1", co.getActuator());
      Assert.assertEquals(ExecutionStatus.EXECUTED, co.getExecutionStatus());
      Assert.assertNotNull(co.getExecutionDate());

      List<Archive> archl = ArchiveLoader.loadArchives();
      Assert.assertEquals(3, archl.size());
   }

   @Test
   public void scheduleUpdateInterceptorException() throws Exception {
      log.info("start scheduleUpdateInterceptorException()");
      Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());

      SchedulerActuator sa = new SchedulerActuator("sm8");
      sa.setPersistenceUnit("localTest");
      sa.setThrowPostponedException(true);
      sa.setThrowScheduledException(true);
      sa.setAutoRemoveScheduledDate(false);
      sa.setBatchInterceptor(new SchedulerExceptionIntercept());
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.SECOND, 4);
      sa.setTimerStart(cal.getTime());
      Configuration.instance().registerActuator(sa);

      registerSetpoint(TComplexEntity.class, "sm8", ControlEvent.UPDATE);
      Context.requestScope().setScheduledDate(Calendar.SECOND, 2);

      TEntity t1 = new TEntity("Stung1", 1, "owner1");
      TEntity t2 = new TEntity("Stung2", 2, "owner2");
      TEntity t3 = new TEntity("Stung3", 3, "owner3");
      TEntity t4 = new TEntity("Stung4", 4, "owner4");

      TComplexEntity base = new TComplexEntity();
      base.setCompValue(45);
      base.setTen(t1);
      base.getLazyList().add(t2);
      base.getLazyList().add(t3);

      applEman.persist(base);

      applEman.getTransaction().commit();
      applEman.clear();
      applEman.getTransaction().begin();

      TComplexEntity base2 = applEman.find(TComplexEntity.class, base.getId());
      Assert.assertNotNull(base2);

      base2.getEagerList().add(t4);
      base2.setOwner("base2owner");
      try {
         base2 = applEman.merge(base2);
         Assert.fail();
      } catch (PostponedException e) {
      }

      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      Context.requestScope().getEntityManager().getTransaction().commit();
      Context.requestScope().getEntityManager().getTransaction().begin();

      log.debug("2. merge");
      base2.setCompValue(177);
      try {
         base2 = applEman.merge(base2);
         Assert.fail();
      } catch (ScheduledException e) {
      }

      log.debug("-------------------- sleep");
      Thread.sleep(10000);
      log.debug("--------------- after TimerTask");
      Context.internalRequestScope().getEntityManager().flush();
      Context.internalRequestScope().getEntityManager().clear();
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      applEman.clear();

      List<DcControllable> sl = SchedulerLoader.loadByUser(USER);
      Assert.assertEquals(1, sl.size());
      DcControllable co = sl.get(0);
      Assert.assertEquals("sm8", co.getActuator());
      Assert.assertEquals(ExecutionStatus.REJECTED, co.getExecutionStatus());
      Assert.assertNotNull(co.getExecutionDate());

      List<DcControllable> l = SchedulerLoader.findScheduled();
      Assert.assertEquals(0, l.size());
      co = sl.get(0);
      Context.internalRequestScope().getEntityManager().remove(co);
      Context.internalRequestScope().getEntityManager().getTransaction().commit();
      Context.internalRequestScope().getEntityManager().getTransaction().begin();

      Query q = applEman.createQuery("SELECT t FROM TComplexEntity t");
      List<TComplexEntity> tlist = q.getResultList();
      Assert.assertEquals(1, tlist.size());
   }

   @Test
   public void scheduleUpdateInterceptor() throws Exception {
      log.info("start scheduleUpdateInterceptor()");
      Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());

      SchedulerActuator sa = (SchedulerActuator) Configuration.instance().getActuator("SCHED1");
      sa.setPersistenceUnit("localTest");
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.SECOND, 4);
      sa.setTimerStart(cal.getTime());

      registerSetpoint(TComplexEntity.class, "SCHED1", ControlEvent.UPDATE);
      Context.requestScope().setScheduledDate(Calendar.SECOND, 2);

      TComplexEntity base = new TComplexEntity();
      base.setCompValue(45);
      applEman.persist(base);

      applEman.getTransaction().commit();
      applEman.clear();
      applEman.getTransaction().begin();

      TComplexEntity base2 = applEman.find(TComplexEntity.class, base.getId());
      Assert.assertNotNull(base2);

      base2.setOwner("base2owner");
      try {
         base2 = applEman.merge(base2);
         Assert.fail();
      } catch (PostponedException e) {
      }

      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      Context.requestScope().getEntityManager().getTransaction().commit();
      Context.requestScope().getEntityManager().getTransaction().begin();
      Context.requestScope().getEntityManager().clear();

      log.debug("-------------------- sleep");
      Thread.sleep(10000);
      log.debug("--------------- after TimerTask");
      Context.internalRequestScope().getEntityManager().flush();
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      applEman.clear();

      List<DcControllable> sl = SchedulerLoader.loadByUser(USER);
      Assert.assertEquals(1, sl.size());
      DcControllable co = sl.get(0);
      Assert.assertEquals("SCHED1", co.getActuator());
      Assert.assertEquals(ExecutionStatus.EXECUTED, co.getExecutionStatus());
      Assert.assertNotNull(co.getExecutionDate());

      List<TEntity> telist = applEman.createQuery("SELECT t FROM TEntity t").getResultList();
      Assert.assertEquals(2, telist.size());
      Assert.assertTrue("polo".equals(telist.get(0).getOwner()) || "polo2".equals(telist.get(0).getOwner()));
      Assert.assertTrue("polo".equals(telist.get(1).getOwner()) || "polo2".equals(telist.get(1).getOwner()));

      List<DcControllable> l = SchedulerLoader.findScheduled();
      Assert.assertEquals(0, l.size());
      co = sl.get(0);
      Context.internalRequestScope().getEntityManager().remove(co);
      Context.internalRequestScope().getEntityManager().getTransaction().commit();
      Context.internalRequestScope().getEntityManager().getTransaction().begin();

      Query q = applEman.createQuery("SELECT t FROM TComplexEntity t");
      List<TComplexEntity> tlist = q.getResultList();
      Assert.assertEquals(1, tlist.size());
   }

   @Test
   public void scheduleUpdateReject() throws Exception {
      log.info("start scheduleUpdateReject()");
      Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());

      SchedulerActuator sa = (SchedulerActuator) Configuration.instance().getActuator("SCHED1");
      sa.setPersistenceUnit("localTest");
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.SECOND, 8);
      sa.setTimerStart(cal.getTime());

      List<String> acts = new ArrayList<>();
      acts.add(ArchiveActuator.DEFAULTNAME);
      acts.add(sa.getName());
      registerSetpoint(TComplexEntity.class.getName(), acts, ControlEvent.UPDATE, ControlEvent.INSERT,
            ControlEvent.RELEASE, ControlEvent.REJECT);

      TComplexEntity base = new TComplexEntity();
      base.setCompValue(45);
      applEman.persist(base);

      applEman.getTransaction().commit();
      applEman.clear();
      applEman.getTransaction().begin();

      TComplexEntity base2 = applEman.find(TComplexEntity.class, base.getId());
      Assert.assertNotNull(base2);

      Context.requestScope().setScheduledDate(Calendar.SECOND, 4);
      base2.setOwner("base2owner");
      try {
         base2 = applEman.merge(base2);
         Assert.fail();
      } catch (PostponedException e) {
      }

      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      List<DcControllable> dcList = SchedulerLoader.findScheduled(TComplexEntity.class.getName());
      Assert.assertEquals(1, dcList.size());
      dcList.get(0).reject(applEman, "my rejection");

      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      applEman.clear();

      List<DcControllable> sl = SchedulerLoader.loadByUser(USER);
      Assert.assertEquals(1, sl.size());
      DcControllable co = sl.get(0);
      Assert.assertEquals("SCHED1", co.getActuator());
      Assert.assertEquals(ExecutionStatus.REJECTED, co.getExecutionStatus());
      Assert.assertNull(co.getExecutionDate());

      log.debug("-------------------- sleep");
      Thread.sleep(12000);
      log.debug("--------------- after TimerTask");
      Context.internalRequestScope().getEntityManager().flush();
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      applEman.clear();

      sl = SchedulerLoader.loadByUser(USER);
      Assert.assertEquals(1, sl.size());
      co = sl.get(0);
      Assert.assertEquals("SCHED1", co.getActuator());
      Assert.assertEquals(ExecutionStatus.REJECTED, co.getExecutionStatus());
      Assert.assertNull(co.getExecutionDate());

      List<Archive> archList = ArchiveLoader.loadArchives(TComplexEntity.class.getName());
      Assert.assertEquals(3, archList.size());
      Assert.assertEquals(ControlEvent.INSERT, archList.get(0).getControlEvent());
      Assert.assertEquals(ControlEvent.UPDATE, archList.get(1).getControlEvent());
      Assert.assertEquals(ControlEvent.REJECT_UPDATE, archList.get(2).getControlEvent());

      for (Archive ar : archList) {
         Context.internalRequestScope().getEntityManager().remove(ar);
      }

      List<DcControllable> l = SchedulerLoader.findScheduled();
      Assert.assertEquals(0, l.size());
      co = sl.get(0);
      Context.internalRequestScope().getEntityManager().remove(co);
      Context.internalRequestScope().getEntityManager().getTransaction().commit();
      Context.internalRequestScope().getEntityManager().getTransaction().begin();

      Query q = applEman.createQuery("SELECT t FROM TComplexEntity t");
      List<TComplexEntity> tlist = q.getResultList();
      Assert.assertEquals(1, tlist.size());
      TComplexEntity te2 = tlist.get(0);
      Assert.assertNull(te2.getOwner());
   }

   @Test
   public void scheduleUpdateRejectEntityManager() throws Exception {
      log.info("start scheduleUpdateRejectEntityManager()");
      Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());

      SchedulerActuator sa = (SchedulerActuator) Configuration.instance().getActuator("SCHED1");
      sa.setPersistenceUnit("localTest");
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.SECOND, 8);
      sa.setTimerStart(cal.getTime());

      List<String> acts = new ArrayList<>();
      acts.add(ArchiveActuator.DEFAULTNAME);
      acts.add(sa.getName());
      registerSetpoint(TComplexEntity.class.getName(), acts, ControlEvent.UPDATE, ControlEvent.INSERT,
            ControlEvent.RELEASE, ControlEvent.REJECT);

      TComplexEntity base = new TComplexEntity();
      base.setCompValue(45);
      applEman.persist(base);

      applEman.getTransaction().commit();
      applEman.clear();
      applEman.getTransaction().begin();

      TComplexEntity base2 = applEman.find(TComplexEntity.class, base.getId());
      Assert.assertNotNull(base2);

      Context.requestScope().setScheduledDate(Calendar.SECOND, 4);
      base2.setOwner("base2owner");
      try {
         base2 = applEman.merge(base2);
         Assert.fail();
      } catch (PostponedException e) {
      }

      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      List<DcControllable> dcList = SchedulerLoader.findScheduled(TComplexEntity.class.getName());
      Assert.assertEquals(1, dcList.size());
      dcList.get(0).reject(applEman, "my rejection");

      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      applEman.clear();

      List<DcControllable> sl = SchedulerLoader.loadByUser(USER);
      Assert.assertEquals(1, sl.size());
      DcControllable co = sl.get(0);
      Assert.assertEquals("SCHED1", co.getActuator());
      Assert.assertEquals(ExecutionStatus.REJECTED, co.getExecutionStatus());
      Assert.assertNull(co.getExecutionDate());

      log.debug("-------------------- sleep");
      Thread.sleep(12000);
      log.debug("--------------- after TimerTask");
      Context.internalRequestScope().getEntityManager().flush();
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      applEman.clear();

      sl = SchedulerLoader.loadByUser(USER);
      Assert.assertEquals(1, sl.size());
      co = sl.get(0);
      Assert.assertEquals("SCHED1", co.getActuator());
      Assert.assertEquals(ExecutionStatus.REJECTED, co.getExecutionStatus());
      Assert.assertNull(co.getExecutionDate());

      List<Archive> archList = ArchiveLoader.loadArchives(TComplexEntity.class.getName());
      Assert.assertEquals(3, archList.size());
      Assert.assertEquals(ControlEvent.INSERT, archList.get(0).getControlEvent());
      Assert.assertEquals(ControlEvent.UPDATE, archList.get(1).getControlEvent());
      Assert.assertEquals(ControlEvent.REJECT_UPDATE, archList.get(2).getControlEvent());

      for (Archive ar : archList) {
         Context.internalRequestScope().getEntityManager().remove(ar);
      }

      List<DcControllable> l = SchedulerLoader.findScheduled();
      Assert.assertEquals(0, l.size());
      co = sl.get(0);
      Context.internalRequestScope().getEntityManager().remove(co);
      Context.internalRequestScope().getEntityManager().getTransaction().commit();
      Context.internalRequestScope().getEntityManager().getTransaction().begin();

      Query q = applEman.createQuery("SELECT t FROM TComplexEntity t");
      List<TComplexEntity> tlist = q.getResultList();
      Assert.assertEquals(1, tlist.size());
      TComplexEntity te2 = tlist.get(0);
      Assert.assertNull(te2.getOwner());
   }

   @Test
   public void scheduleUpdateRelease() throws Exception {
      log.info("start scheduleUpdateRelease()");
      Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());

      SchedulerActuator sa = (SchedulerActuator) Configuration.instance().getActuator("SCHED1");
      sa.setPersistenceUnit("localTest");
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.SECOND, 8);
      // cal.add(Calendar.HOUR, 8);
      sa.setTimerStart(cal.getTime());

      List<String> acts = new ArrayList<>();
      acts.add(ArchiveActuator.DEFAULTNAME);
      acts.add(sa.getName());
      registerSetpoint(TComplexEntity.class.getName(), acts, ControlEvent.UPDATE, ControlEvent.INSERT,
            ControlEvent.RELEASE, ControlEvent.REJECT);

      TComplexEntity base = new TComplexEntity();
      base.setCompValue(45);
      applEman.persist(base);

      applEman.getTransaction().commit();
      applEman.clear();
      applEman.getTransaction().begin();

      TComplexEntity base2 = applEman.find(TComplexEntity.class, base.getId());
      Assert.assertNotNull(base2);

      Thread.sleep(100);
      Context.requestScope().setScheduledDate(Calendar.SECOND, 4);
      base2.setOwner("base2owner");
      try {
         base2 = applEman.merge(base2);
         Assert.fail();
      } catch (PostponedException e) {
      }

      Assert.assertNull(Context.requestScope().getScheduledDate());

      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      List<DcControllable> dcList = SchedulerLoader.findScheduled(TComplexEntity.class.getName());
      Assert.assertEquals(1, dcList.size());
      dcList.get(0).release(applEman, "my rejection");

      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      applEman.clear();

      List<DcControllable> sl = SchedulerLoader.loadByUser(USER);
      Assert.assertEquals(1, sl.size());
      DcControllable co = sl.get(0);
      Assert.assertEquals("SCHED1", co.getActuator());
      Assert.assertEquals(ExecutionStatus.EXECUTED, co.getExecutionStatus());
      Assert.assertNotNull(co.getExecutionDate());

      log.debug("-------------------- sleep");
      Thread.sleep(12000);
      log.debug("--------------- after TimerTask");
      Context.internalRequestScope().getEntityManager().flush();
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      applEman.clear();

      sl = SchedulerLoader.loadByUser(USER);
      Assert.assertEquals(1, sl.size());
      co = sl.get(0);
      Assert.assertEquals("SCHED1", co.getActuator());
      Assert.assertEquals(ExecutionStatus.EXECUTED, co.getExecutionStatus());
      Assert.assertNotNull(co.getExecutionDate());

      List<Archive> archList = ArchiveLoader.loadArchives(TComplexEntity.class.getName());
      Assert.assertEquals(3, archList.size());
      Assert.assertEquals(ControlEvent.INSERT, archList.get(0).getControlEvent());
      Assert.assertEquals(ControlEvent.UPDATE, archList.get(1).getControlEvent());
      Assert.assertEquals(ControlEvent.RELEASE_UPDATE, archList.get(2).getControlEvent());

      for (Archive ar : archList) {
         Context.internalRequestScope().getEntityManager().remove(ar);
      }

      List<DcControllable> l = SchedulerLoader.findScheduled();
      Assert.assertEquals(0, l.size());
      co = sl.get(0);
      Context.internalRequestScope().getEntityManager().remove(co);
      Context.internalRequestScope().getEntityManager().getTransaction().commit();
      Context.internalRequestScope().getEntityManager().getTransaction().begin();

      Query q = applEman.createQuery("SELECT t FROM TComplexEntity t");
      List<TComplexEntity> tlist = q.getResultList();
      Assert.assertEquals(1, tlist.size());
      TComplexEntity te2 = tlist.get(0);
      Assert.assertEquals("base2owner", te2.getOwner());
   }

}
