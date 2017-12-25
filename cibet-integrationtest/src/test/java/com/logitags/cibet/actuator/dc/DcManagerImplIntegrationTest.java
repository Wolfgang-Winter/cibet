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
package com.logitags.cibet.actuator.dc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.persistence.Query;
import javax.persistence.TransactionRequiredException;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.cibethelper.base.DBHelper;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TComplexEntity2;
import com.cibethelper.entities.TEntity;
import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.actuator.common.InvalidUserException;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.ConfigurationService;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.diff.Difference;
import com.logitags.cibet.diff.DifferenceType;

public class DcManagerImplIntegrationTest extends DBHelper {

   private static Logger log = Logger.getLogger(DcManagerImplIntegrationTest.class);

   @After
   public void subDoAfter() {
      log.info("subDoAfter");
      new ConfigurationService().initialise();
   }

   @Test
   public void findUnreleased() {
      log.info("start findUnreleased()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT);

      persistTEntity();

      List<Controllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());

      l = DcLoader.findUnreleased(TEntity.class.getName());
      Assert.assertEquals(1, l.size());
   }

   @Test
   public void releasePersist() throws Exception {
      log.info("start releasePersist()");

      registerSetpoint(TEntity.class, FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT, ControlEvent.RELEASE);
      Context.requestScope().setRemark("created");

      TEntity ent = persistTEntity();
      Assert.assertEquals(0, ent.getId());

      List<Controllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());
      Controllable co = l.get(0);
      Assert.assertEquals("created", co.getCreateRemark());

      Context.sessionScope().setUser("test2");
      Object res = co.release(applEman, null);
      Assert.assertNotNull(res);
      Assert.assertTrue(res instanceof TEntity);
      Assert.assertTrue(((TEntity) res).getId() != 0);

      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      Context.sessionScope().setUser(USER);
      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      TEntity te = applEman.find(TEntity.class, ((TEntity) res).getId());
      Assert.assertNotNull(te);
      Context.requestScope().setRemark(null);
   }

   @Test
   public void releasePersistNoTransaction() throws Exception {
      log.info("start releasePersistNoTransaction()");

      registerSetpoint(TEntity.class, FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT, ControlEvent.RELEASE);
      Context.requestScope().setRemark("created");

      TEntity ent = persistTEntity();
      Assert.assertEquals(0, ent.getId());

      List<Controllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());
      Controllable co = l.get(0);
      Assert.assertEquals("created", co.getCreateRemark());
      Context.sessionScope().setUser("test2");

      applEman.getTransaction().commit();

      try {
         co.release(applEman, null);
         Assert.fail();
      } catch (TransactionRequiredException e) {
      }

      applEman.getTransaction().begin();
   }

   @Test
   public void releasePersistWithApproveUser() throws Exception {
      log.info("start releasePersistWithApproveUser()");

      registerSetpoint(TEntity.class, FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT, ControlEvent.RELEASE);

      Context.sessionScope().setApprovalUser("fizz");
      TEntity ent = persistTEntity();
      Assert.assertEquals(0, ent.getId());

      List<Controllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());
      Controllable co = l.get(0);
      Assert.assertEquals("fizz", co.getReleaseUser());

      Context.sessionScope().setUser("test2");
      try {
         co.release(applEman, null);
         Assert.fail();
      } catch (InvalidUserException e) {
      }

      Context.sessionScope().setUser("fizz");
      Object res = co.release(applEman, null);
      Context.sessionScope().setApprovalUser(null);
      Assert.assertNotNull(res);
      Assert.assertTrue(res instanceof TEntity);
      Assert.assertTrue(((TEntity) res).getId() != 0);

      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      Context.sessionScope().setUser(USER);
      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      TEntity te = applEman.find(TEntity.class, ((TEntity) res).getId());
      Assert.assertNotNull(te);
   }

   @Test(expected = InvalidUserException.class)
   public void releasePersistInvalidUser() throws ResourceApplyException {
      log.info("start releasePersistInvalidUser()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class, FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      persistTEntity();

      List<Controllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());
      Controllable co = l.get(0);
      co.release(applEman, null);
   }

   @Test
   public void releaseUpdate() throws Exception {
      log.info("start releaseUpdate()");
      Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());

      registerSetpoint(TComplexEntity.class, FourEyesActuator.DEFAULTNAME, ControlEvent.UPDATE,
            ControlEvent.RELEASE_UPDATE);

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

      applEman.merge(tce);
      applEman.flush();
      applEman.clear();

      List<Controllable> l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      Controllable co = l1.get(0);

      Context.sessionScope().setUser("tester2");
      co.release(applEman, "blabla");

      l1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, l1.size());

      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      applEman.clear();
      Query query = applEman.createNamedQuery(TComplexEntity.SEL_BY_OWNER);
      query.setParameter("owner", TENANT);
      List<TComplexEntity> l = query.getResultList();
      Assert.assertEquals(1, l.size());
      TComplexEntity tce4 = (TComplexEntity) query.getSingleResult();
      Assert.assertEquals(2, tce4.getLazyList().size());
      Assert.assertEquals(122, tce4.getCompValue());
   }

   @Test
   public void releaseRemove6Eyes() throws ResourceApplyException {
      log.info("start releaseRemove6Eyes()");

      registerSetpoint(TComplexEntity.class, SixEyesActuator.DEFAULTNAME, ControlEvent.DELETE,
            ControlEvent.RELEASE_DELETE, ControlEvent.FIRST_RELEASE_DELETE);

      TComplexEntity ce = createTComplexEntity();
      applEman.persist(ce);
      applEman.flush();
      applEman.clear();

      TComplexEntity tce = applEman.find(TComplexEntity.class, ce.getId());
      Assert.assertNotNull(tce);
      Assert.assertEquals(3, tce.getLazyList().size());

      Context.requestScope().setRemark("Heinz");
      applEman.remove(tce);
      applEman.flush();

      // first release
      List<Controllable> l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      Controllable co = l1.get(0);
      Context.sessionScope().setUser("tester2");
      Context.requestScope().setRemark("Manni");
      co.release(applEman, "blabla1");
      applEman.flush();
      applEman.clear();

      l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      co = l1.get(0);
      Assert.assertEquals("tester2", co.getFirstApprovalUser());
      Assert.assertEquals(ControlEvent.DELETE, co.getControlEvent());
      Assert.assertEquals(USER, co.getCreateUser());
      Assert.assertEquals("Heinz", co.getCreateRemark());
      Assert.assertEquals("blabla1", co.getFirstApprovalRemark());

      // still not removed
      Query query = applEman.createNamedQuery(TComplexEntity.SEL_BY_OWNER);
      query.setParameter("owner", TENANT);
      List<TComplexEntity> l = query.getResultList();
      Assert.assertEquals(1, l.size());

      // 2. release
      try {
         // invalid user
         co.release(applEman, "blabla2");
         Assert.fail();
      } catch (InvalidUserException e) {
      }

      try {
         // invalid user
         Context.sessionScope().setUser("tester2");
         co.release(applEman, "blabla2");
         Assert.fail();
      } catch (InvalidUserException e) {
      }

      Context.sessionScope().setUser("tester3");
      co.release(applEman, "blabla2");
      applEman.flush();

      // now it is removed
      applEman.clear();
      l = query.getResultList();
      Assert.assertEquals(0, l.size());
   }

   @Test
   public void releaseRemove6EyesWithApprovalUser() throws Exception {
      log.info("start releaseRemove6EyesWithApprovalUser()");

      registerSetpoint(TComplexEntity.class, SixEyesActuator.DEFAULTNAME, ControlEvent.DELETE,
            ControlEvent.RELEASE_DELETE, ControlEvent.FIRST_RELEASE_DELETE);

      TComplexEntity ce = createTComplexEntity();
      applEman.persist(ce);
      applEman.flush();
      applEman.clear();

      TComplexEntity tce = applEman.find(TComplexEntity.class, ce.getId());
      Assert.assertNotNull(tce);
      Assert.assertEquals(3, tce.getLazyList().size());

      Context.sessionScope().setApprovalUser("Wizzi");
      applEman.remove(tce);
      applEman.flush();

      // first release
      List<Controllable> l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      Controllable co = l1.get(0);
      Context.sessionScope().setUser("tester2");
      try {
         co.release(applEman, "blabla1");
         Assert.fail();
      } catch (InvalidUserException e1) {
      }

      Context.sessionScope().setUser("Wizzi");
      Context.sessionScope().setApprovalUser("M�ggel");
      co.release(applEman, "blabla1");
      applEman.flush();
      applEman.clear();

      l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      co = l1.get(0);
      Assert.assertEquals("Wizzi", co.getFirstApprovalUser());
      Assert.assertNotNull(co.getFirstApprovalDate());
      Assert.assertEquals(ControlEvent.DELETE, co.getControlEvent());
      Assert.assertEquals(USER, co.getCreateUser());

      // still not removed
      Query query = applEman.createNamedQuery(TComplexEntity.SEL_BY_OWNER);
      query.setParameter("owner", TENANT);
      List<TComplexEntity> l = query.getResultList();
      Assert.assertEquals(1, l.size());

      // 2. release
      Context.sessionScope().setUser("Nixxi");
      Context.sessionScope().setApprovalUser("Lobo");
      try {
         // invalid user
         co.release(applEman, "blabla2");
         Assert.fail();
      } catch (InvalidUserException e) {
      }

      Context.sessionScope().setUser("M�ggel");
      co.release(applEman, "blabla2");
      applEman.flush();

      // now it is removed
      applEman.clear();
      l = query.getResultList();
      Assert.assertEquals(0, l.size());
      Context.sessionScope().setApprovalUser(null);
   }

   @Test
   public void rejectRemove4Eyes() throws ResourceApplyException {
      log.info("start rejectRemove4Eyes()");

      registerSetpoint(TComplexEntity.class, FourEyesActuator.DEFAULTNAME, ControlEvent.DELETE, ControlEvent.REJECT);

      TComplexEntity ce = createTComplexEntity();
      applEman.persist(ce);
      applEman.flush();
      applEman.clear();

      TComplexEntity tce = applEman.find(TComplexEntity.class, ce.getId());
      applEman.remove(tce);
      applEman.flush();
      applEman.clear();

      List<Controllable> l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      Controllable co = l1.get(0);
      co.reject(applEman, "blabla1");
      applEman.flush();

      l1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, l1.size());

      Query query = applEman.createNamedQuery(TComplexEntity.SEL_BY_OWNER);
      query.setParameter("owner", TENANT);
      List<TComplexEntity> l = query.getResultList();
      Assert.assertEquals(1, l.size());
   }

   @Test
   public void rejectPersist6Eyes() throws ResourceApplyException {
      log.info("start rejectPersist6Eyes()");

      registerSetpoint(TComplexEntity.class, SixEyesActuator.DEFAULTNAME, ControlEvent.INSERT,
            ControlEvent.REJECT_INSERT, ControlEvent.FIRST_RELEASE_INSERT);

      TComplexEntity ce = createTComplexEntity();
      applEman.persist(ce);
      applEman.flush();
      applEman.clear();

      Query query = applEman.createNamedQuery(TComplexEntity.SEL_BY_OWNER);
      query.setParameter("owner", TENANT);
      List<TComplexEntity> l = query.getResultList();
      Assert.assertEquals(0, l.size());

      List<Controllable> l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      Controllable co = l1.get(0);
      Context.sessionScope().setUser("tester2");
      co.release(applEman, "blabla1");
      applEman.flush();

      l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      co = l1.get(0);
      Assert.assertEquals("tester2", co.getFirstApprovalUser());
      Assert.assertEquals(ControlEvent.INSERT, co.getControlEvent());
      Assert.assertEquals(USER, co.getCreateUser());

      l = query.getResultList();
      Assert.assertEquals(0, l.size());

      Context.sessionScope().setUser(USER);
      co.reject(applEman, "blabla2");

      l1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, l1.size());

      l = query.getResultList();
      Assert.assertEquals(0, l.size());
   }

   @Test
   public void rejectPersist6EyesWithApprovalUser() throws Exception {
      log.info("start rejectPersist6EyesWithApprovalUser()");

      registerSetpoint(TComplexEntity.class, SixEyesActuator.DEFAULTNAME, ControlEvent.INSERT,
            ControlEvent.REJECT_INSERT, ControlEvent.FIRST_RELEASE_INSERT);
      SimpleSmtpServer server = SimpleSmtpServer.start(8854);

      try {
         Context.sessionScope().setApprovalUser("fizz");
         TComplexEntity ce = createTComplexEntity();
         applEman.persist(ce);
         applEman.flush();
         applEman.clear();

         Query query = applEman.createNamedQuery(TComplexEntity.SEL_BY_OWNER);
         query.setParameter("owner", TENANT);
         List<TComplexEntity> l = query.getResultList();
         Assert.assertEquals(0, l.size());
         server.stop();
         Assert.assertEquals(0, server.getReceivedEmailSize());

         server = SimpleSmtpServer.start(8854);
         List<Controllable> l1 = DcLoader.findUnreleased();
         Assert.assertEquals(1, l1.size());
         Controllable co = l1.get(0);
         Context.sessionScope().setUser("fizz");
         Context.sessionScope().setApprovalUser("Muzzi");
         co.release(applEman, "blabla1");
         applEman.flush();
         server.stop();
         Assert.assertEquals(0, server.getReceivedEmailSize());

         l1 = DcLoader.findUnreleased();
         Assert.assertEquals(1, l1.size());
         co = l1.get(0);
         Assert.assertEquals("fizz", co.getFirstApprovalUser());
         Assert.assertEquals(ControlEvent.INSERT, co.getControlEvent());
         Assert.assertEquals(USER, co.getCreateUser());

         l = query.getResultList();
         Assert.assertEquals(0, l.size());

         Context.sessionScope().setUser("Wolli");
         try {
            co.reject("blabla2");
            Assert.fail();
         } catch (Exception e) {
         }

         server = SimpleSmtpServer.start(8854);
         Context.sessionScope().setUser(USER);
         co.reject(applEman, "blabla2");
         server.stop();
         Assert.assertEquals(0, server.getReceivedEmailSize());

         l1 = DcLoader.findUnreleased();
         Assert.assertEquals(0, l1.size());

         l = query.getResultList();
         Assert.assertEquals(0, l.size());
         Context.sessionScope().setApprovalUser(null);
      } finally {
         server.stop();
      }
   }

   @Test
   public void rejectPersist6EyesWithApprovalUserWithEmail() throws Exception {
      log.info("start rejectPersist6EyesWithApprovalUserWithEmail()");

      registerSetpoint(TComplexEntity.class, SixEyesActuator.DEFAULTNAME, ControlEvent.INSERT,
            ControlEvent.REJECT_INSERT, ControlEvent.FIRST_RELEASE_INSERT);
      SimpleSmtpServer server = SimpleSmtpServer.start(8854);

      try {
         Context.sessionScope().setUserAddress("USER@email.de");
         Context.sessionScope().setApprovalUser("fizz");
         Context.sessionScope().setApprovalAddress("fizz@email.de");
         TComplexEntity ce = createTComplexEntity();
         applEman.persist(ce);
         applEman.flush();
         applEman.clear();

         Query query = applEman.createNamedQuery(TComplexEntity.SEL_BY_OWNER);
         query.setParameter("owner", TENANT);
         List<TComplexEntity> l = query.getResultList();
         Assert.assertEquals(0, l.size());
         server.stop();
         Assert.assertEquals(1, server.getReceivedEmailSize());
         Iterator<SmtpMessage> emailIter = server.getReceivedEmail();
         SmtpMessage email = emailIter.next();
         Assert.assertEquals("Cibet Notification: FIRST_POSTPONED", email.getHeaderValue("Subject"));
         Assert.assertEquals("from@test.de", email.getHeaderValue("From"));
         Assert.assertEquals("fizz@email.de", email.getHeaderValue("To"));

         server = SimpleSmtpServer.start(8854);
         List<Controllable> l1 = DcLoader.findUnreleased();
         Assert.assertEquals(1, l1.size());
         Controllable co = l1.get(0);
         Context.sessionScope().setUser("fizz");
         Context.sessionScope().setApprovalUser("Muzzi");
         Context.sessionScope().setApprovalAddress("Muzzi@email.de");
         co.release(applEman, "blabla1");
         applEman.flush();
         server.stop();
         Assert.assertEquals(2, server.getReceivedEmailSize());
         emailIter = server.getReceivedEmail();
         while (emailIter.hasNext()) {
            email = emailIter.next();
            Assert.assertEquals("from@test.de", email.getHeaderValue("From"));
            if ("Cibet Notification: FIRST_RELEASED".equals(email.getHeaderValue("Subject"))) {
               Assert.assertEquals("USER@email.de", email.getHeaderValue("To"));

            } else if ("Cibet Notification: POSTPONED".equals(email.getHeaderValue("Subject"))) {
               Assert.assertEquals("Muzzi@email.de", email.getHeaderValue("To"));

            } else {
               Assert.fail();
            }
         }

         l1 = DcLoader.findUnreleased();
         Assert.assertEquals(1, l1.size());
         co = l1.get(0);
         Assert.assertEquals("fizz", co.getFirstApprovalUser());
         Assert.assertEquals(ControlEvent.INSERT, co.getControlEvent());
         Assert.assertEquals(USER, co.getCreateUser());

         l = query.getResultList();
         Assert.assertEquals(0, l.size());

         Context.sessionScope().setUser("Wolli");
         try {
            co.reject("blabla2");
            Assert.fail();
         } catch (Exception e) {
         }

         server = SimpleSmtpServer.start(8854);
         Context.sessionScope().setUser(USER);
         co.reject(applEman, "blabla2");
         server.stop();
         Assert.assertEquals(1, server.getReceivedEmailSize());
         emailIter = server.getReceivedEmail();
         email = emailIter.next();
         Assert.assertEquals("Cibet Notification: REJECTED", email.getHeaderValue("Subject"));
         Assert.assertEquals("from@test.de", email.getHeaderValue("From"));
         Assert.assertEquals("USER@email.de", email.getHeaderValue("To"));

         l1 = DcLoader.findUnreleased();
         Assert.assertEquals(0, l1.size());

         l = query.getResultList();
         Assert.assertEquals(0, l.size());
         Context.sessionScope().setApprovalUser(null);
      } finally {
         server.stop();
         Context.sessionScope().setUserAddress(null);
         Context.sessionScope().setApprovalAddress(null);
         Context.sessionScope().setApprovalUser(null);
      }
   }

   @Test
   public void rejectRemove4EyesNoActuators() throws ResourceApplyException {
      log.info("start rejectRemove4EyesNoActuators()");

      registerSetpoint(TComplexEntity.class, FourEyesActuator.DEFAULTNAME, ControlEvent.DELETE);

      TComplexEntity ce = createTComplexEntity();
      applEman.persist(ce);
      applEman.flush();
      applEman.clear();

      applEman.remove(ce);
      applEman.flush();
      applEman.clear();

      List<Controllable> l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      Controllable co = l1.get(0);
      co.reject(applEman, "blabla1");

      l1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, l1.size());

      Query query = applEman.createNamedQuery(TComplexEntity.SEL_BY_OWNER);
      query.setParameter("owner", TENANT);
      List<TComplexEntity> l = query.getResultList();
      Assert.assertEquals(1, l.size());
   }

   @Test(expected = IllegalArgumentException.class)
   public void rejectWithNoUser() throws ResourceApplyException {
      Context.sessionScope().setUser(null);
      Controllable sd = new Controllable();
      sd.setControlEvent(ControlEvent.DELETE);
      sd.reject("dfdf");
   }

   @Test(expected = InvalidUserException.class)
   public void reject6EyesWithNoUser() throws ResourceApplyException {
      Context.sessionScope().setUser(null);
      Controllable sd = new Controllable();
      sd.setControlEvent(ControlEvent.DELETE);
      sd.setActuator(SixEyesActuator.DEFAULTNAME);
      sd.setExecutionStatus(ExecutionStatus.FIRST_POSTPONED);
      sd.reject(applEman, "dfdf");
   }

   @Test(expected = InvalidUserException.class)
   public void reject6EyesInvalidUser() throws ResourceApplyException {
      Context.sessionScope().setUser("II");
      Controllable sd = new Controllable();
      sd.setControlEvent(ControlEvent.DELETE);
      sd.setActuator(SixEyesActuator.DEFAULTNAME);
      sd.setExecutionStatus(ExecutionStatus.FIRST_POSTPONED);
      sd.setFirstApprovalUser("nn");
      sd.reject(applEman, "dfdf");
   }

   @Test(expected = InvalidUserException.class)
   public void releaseWithNoUser() throws ResourceApplyException {
      Context.sessionScope().setUser(null);
      log.debug("CibetContext.getUser(): " + Context.sessionScope().getUser());
      Controllable sd = new Controllable();
      sd.setControlEvent(ControlEvent.DELETE);
      sd.setActuator(FourEyesActuator.DEFAULTNAME);
      sd.setExecutionStatus(ExecutionStatus.POSTPONED);
      sd.release(applEman, "dfdf");
   }

   @Test(expected = InvalidUserException.class)
   public void release6EyesWithNoUser() throws ResourceApplyException {
      Context.sessionScope().setUser(null);
      log.debug("CibetContext.getUser(): " + Context.sessionScope().getUser());
      Controllable sd = new Controllable();
      sd.setControlEvent(ControlEvent.DELETE);
      sd.setActuator(SixEyesActuator.DEFAULTNAME);
      sd.setExecutionStatus(ExecutionStatus.FIRST_POSTPONED);
      sd.release(applEman, "dfdf");
   }

   @Test(expected = InvalidUserException.class)
   public void release6EyesSameUser() throws ResourceApplyException {
      Context.sessionScope().setUser("US");
      Configuration.instance().registerAuthenticationProvider(null);
      log.debug("CibetContext.getUser(): " + Context.sessionScope().getUser());
      Controllable sd = new Controllable();
      sd.setControlEvent(ControlEvent.DELETE);
      sd.setActuator(SixEyesActuator.DEFAULTNAME);
      sd.setExecutionStatus(ExecutionStatus.FIRST_POSTPONED);
      sd.setCreateUser("US");
      sd.release(applEman, "dfdf");
   }

   @Test(expected = ResourceApplyException.class)
   public void release6EyesNotPostponed() throws ResourceApplyException {
      Context.sessionScope().setUser("xx");
      Configuration.instance().registerAuthenticationProvider(null);
      log.debug("CibetContext.getUser(): " + Context.sessionScope().getUser());
      Controllable sd = new Controllable();
      sd.setControlEvent(ControlEvent.DELETE);
      sd.setActuator(SixEyesActuator.DEFAULTNAME);
      sd.setExecutionStatus(ExecutionStatus.REJECTED);
      sd.release(applEman, "dfdf");
   }

   @Test(expected = ResourceApplyException.class)
   public void releaseWithWrongStatus() throws ResourceApplyException {
      Context.sessionScope().setUser(null);
      log.debug("CibetContext.getUser(): " + Context.sessionScope().getUser());
      Controllable sd = new Controllable();
      sd.setControlEvent(ControlEvent.DELETE);
      sd.setActuator(FourEyesActuator.DEFAULTNAME);
      sd.setExecutionStatus(ExecutionStatus.EXECUTED);
      sd.release(applEman, "dfdf");
   }

   @Test(expected = ResourceApplyException.class)
   public void releaseWithWrongStatus2ManRule() throws ResourceApplyException {
      Context.sessionScope().setUser(null);
      log.debug("CibetContext.getUser(): " + Context.sessionScope().getUser());
      Controllable sd = new Controllable();
      sd.setControlEvent(ControlEvent.DELETE);
      sd.setActuator(TwoManRuleActuator.DEFAULTNAME);
      sd.setExecutionStatus(ExecutionStatus.EXECUTED);
      sd.release(applEman, "dfdf");
   }

   @Test
   public void afterEventWithWrongEvent2ManRule() throws ResourceApplyException {
      EventMetadata md = new EventMetadata(ControlEvent.ALL, null);
      TwoManRuleActuator act = new TwoManRuleActuator();
      try {
         act.afterEvent(md);
         Assert.fail();
      } catch (RuntimeException e) {
         Assert.assertTrue(e.getMessage().endsWith("is an abstract ControlEvent"));
      }
   }

   @Test(expected = IllegalArgumentException.class)
   public void releaseInvalidEvent() throws ResourceApplyException {
      Controllable sd = new Controllable();
      sd.setControlEvent(ControlEvent.REDO);
      sd.setActuator(FourEyesActuator.DEFAULTNAME);
      sd.setExecutionStatus(ExecutionStatus.POSTPONED);
      sd.release(applEman, "dfdf");
   }

   @Test(expected = IllegalArgumentException.class)
   public void rejectInvalidEvent() throws ResourceApplyException {
      Context.sessionScope().setUser("tester");
      Controllable sd = new Controllable();
      sd.setControlEvent(ControlEvent.REDO);
      sd.setActuator(FourEyesActuator.DEFAULTNAME);
      sd.setExecutionStatus(ExecutionStatus.POSTPONED);
      sd.reject("dfdf");
   }

   @Test
   public void compare() {
      log.info("start compare()");
      Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());

      registerSetpoint(TEntity.class, FourEyesActuator.DEFAULTNAME, ControlEvent.UPDATE);

      TEntity entity = persistTEntity();
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull("entity with id " + entity.getId() + " not found", selEnt);
      Assert.assertEquals(5, selEnt.getCounter());

      selEnt.setCounter(12);
      selEnt.setNameValue("newYY");
      // applEman.getTransaction().begin();
      applEman.merge(selEnt);
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      applEman.clear();

      selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull("entity with id " + entity.getId() + " not found", selEnt);
      Assert.assertEquals(5, selEnt.getCounter());

      List<Controllable> unreleased = DcLoader.findUnreleased();
      Assert.assertTrue(unreleased.size() == 1);

      Controllable coObj = unreleased.get(0);
      List<Difference> list = DcLoader.differences(coObj);
      // dcManager.compare(Context.requestScope().getEntityManager(), coObj.getResource());
      Assert.assertTrue(list.size() == 2);
      Difference cou = list.get(0);
      Difference nv = list.get(1);
      if (list.get(0).getPropertyName().equals("counter")) {
         // okay
      } else if (list.get(0).getPropertyName().equals("nameValue")) {
         cou = list.get(1);
         nv = list.get(0);
      } else {
         Assert.fail();
      }
      Assert.assertEquals("counter", cou.getPropertyName());
      Assert.assertEquals(DifferenceType.MODIFIED, cou.getDifferenceType());
      Assert.assertEquals(new Integer(5), cou.getOldValue());
      Assert.assertEquals(new Integer(12), cou.getNewValue());

      Assert.assertEquals("nameValue", nv.getPropertyName());
      Assert.assertEquals(DifferenceType.MODIFIED, nv.getDifferenceType());
      Assert.assertEquals("valuexx", nv.getOldValue());
      Assert.assertEquals("newYY", nv.getNewValue());
   }

   @Test(expected = IllegalArgumentException.class)
   public void compareNotUpdatedObjects() {
      Controllable sd = new Controllable();
      sd.setControlEvent(ControlEvent.DELETE);
      DcLoader.differences(sd);
      // compare(Context.requestScope().getEntityManager(), sd.getResource());
   }

   @Test
   public void releaseSelect6Eyes() throws ResourceApplyException {
      log.info("start releaseSelect6Eyes()");

      registerSetpoint(TComplexEntity.class, SixEyesActuator.DEFAULTNAME, ControlEvent.SELECT,
            ControlEvent.RELEASE_SELECT, ControlEvent.FIRST_RELEASE_SELECT);

      TComplexEntity ce = createTComplexEntity();
      applEman.persist(ce);
      applEman.flush();
      applEman.clear();

      Context.requestScope().setRemark("Heinz");
      TComplexEntity tce = applEman.find(TComplexEntity.class, ce.getId());
      Assert.assertNull(tce);
      // Assert.assertEquals(3, tce.getLazyList().size());

      // first release
      List<Controllable> l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      Controllable co = l1.get(0);
      Context.sessionScope().setUser("tester2");
      Context.requestScope().setRemark("Manni");
      TComplexEntity tce2 = (TComplexEntity) co.release(applEman, "blabla1");
      Assert.assertNull(tce2);
      applEman.flush();
      applEman.clear();

      l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      co = l1.get(0);
      Assert.assertEquals("tester2", co.getFirstApprovalUser());
      Assert.assertEquals(ControlEvent.SELECT, co.getControlEvent());
      Assert.assertEquals(USER, co.getCreateUser());
      Assert.assertEquals("Heinz", co.getCreateRemark());
      Assert.assertEquals("blabla1", co.getFirstApprovalRemark());

      // 2. release
      try {
         // invalid user
         co.release(applEman, "blabla2");
         Assert.fail();
      } catch (InvalidUserException e) {
      }

      Context.sessionScope().setUser("tester3");
      TComplexEntity tce3 = (TComplexEntity) co.release(applEman, "blabla2");
      Assert.assertEquals(ce.getId(), tce3.getId());
      Assert.assertEquals(ce.getEagerList().size(), tce3.getEagerList().size());

   }

   @Test
   public void passBackPersist() throws Exception {
      log.info("start passBackPersist()");

      registerSetpoint(TEntity.class, FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT, ControlEvent.RELEASE,
            ControlEvent.PASSBACK, ControlEvent.SUBMIT);
      Context.requestScope().setRemark("created");

      TEntity ent = persistTEntity();
      Assert.assertEquals(0, ent.getId());

      List<Controllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());
      Controllable co = l.get(0);
      Assert.assertEquals("created", co.getCreateRemark());

      Context.sessionScope().setUser("test2");
      co.passBack(applEman, null);

      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      l = DcLoader.loadByUser(USER);
      Assert.assertEquals(1, l.size());
      Assert.assertEquals(ExecutionStatus.PASSEDBACK, l.get(0).getExecutionStatus());
      Assert.assertEquals("test2", l.get(0).getReleaseUser());

      Context.sessionScope().setUser("test3");
      try {
         l.get(0).submit(applEman, "2.submit");
         Assert.fail();
      } catch (InvalidUserException e) {
      }

      Context.sessionScope().setUser(USER);
      l.get(0).submit(applEman, "2.submit");
      l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());
      // Assert.assertEquals("2.submit", l.get(0).getCreateRemark());

      Context.sessionScope().setUser("fizz");
      Object res = l.get(0).release(applEman, "rel");
      TEntity te = applEman.find(TEntity.class, ((TEntity) res).getId());
      Assert.assertNotNull(te);
      Context.requestScope().setRemark(null);
   }

   @Test
   public void rejectUpdate4Eyes() throws ResourceApplyException {
      log.info("start rejectUpdate4Eyes()");
      if (Context.internalRequestScope().getApplicationEntityManager2() == null) {
         Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());
      }

      registerSetpoint(TComplexEntity2.class, FourEyesActuator.DEFAULTNAME, ControlEvent.UPDATE);

      TComplexEntity2 ce = createTComplexEntity2();
      applEman.persist(ce);

      applEman.getTransaction().commit();
      applEman.clear();
      applEman.getTransaction().begin();

      TComplexEntity2 tce = applEman.find(TComplexEntity2.class, ce.getId());
      tce.setOwner("newOwner");
      tce.getTen().setCounter(889);
      tce = applEman.merge(tce);

      applEman.getTransaction().commit();
      applEman.clear();
      applEman.getTransaction().begin();

      List<Controllable> l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      Controllable co = l1.get(0);
      log.debug("now reject");
      co.reject(applEman, "blabla1");

      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      l1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, l1.size());

      Query query = applEman.createNamedQuery(TComplexEntity2.SEL_BY_OWNER);
      query.setParameter("owner", TENANT);
      List<TComplexEntity> l = query.getResultList();
      Assert.assertEquals(1, l.size());
   }

}
