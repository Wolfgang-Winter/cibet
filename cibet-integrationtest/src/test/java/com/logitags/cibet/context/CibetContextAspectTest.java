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
package com.logitags.cibet.context;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import com.cibethelper.CibetContextAspectTestHelper;
import com.cibethelper.base.CoreTestBase;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.actuator.common.PostponedException;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.actuator.dc.FourEyesActuator;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.jndi.EjbLookup;

public class CibetContextAspectTest extends CoreTestBase {

   private static Logger log = Logger.getLogger(CibetContextAspectTest.class);

   public static String ATENANT = "BANKXX";

   public static final String AUSER = "Ösal";

   private static EntityManager applEman;

   public static EntityManager getApplEman() {
      return applEman;
   }

   @BeforeClass
   public static void beforeClass() throws Exception {
      SecurityContextHolder.getContext().setAuthentication(null);
      try {
         Subject currentUser = SecurityUtils.getSubject();
         if (currentUser != null) {
            currentUser.logout();
         }
      } catch (Exception e) {
         // ignore
         // log.error(e.getMessage(), e);
      }

      EjbLookup.clearCache();
      EntityManagerFactory fac = Persistence.createEntityManagerFactory("localTest");
      applEman = fac.createEntityManager();
   }

   @Before
   public void doBefore() throws Exception {
      log.debug("CibetContextAspectTest.doBefore");
      Context.internalRequestScope().clear();
      Context.internalSessionScope().clear();
      applEman.getTransaction().begin();
   }

   @After
   public void doAfter() throws Exception {
      log.debug("do after");
      if (applEman.getTransaction().isActive()) {
         applEman.getTransaction().rollback();
      }
   }

   public TEntity persistTEntity() {
      TEntity entity = new TEntity();
      entity.setCounter(5);
      entity.setNameValue("valuexx");
      entity.setOwner(ATENANT);
      try {
         applEman.persist(entity);
      } catch (PostponedException e) {
      }
      applEman.flush();
      applEman.clear();
      return entity;
   }

   @Test
   public void testReleasePersistAspect() throws Exception {
      log.info("start testReleasePersistAspect()");
      Context.sessionScope().setUser(AUSER);
      Context.sessionScope().setTenant(ATENANT);
      Context.internalRequestScope().setManaged(true);
      registerSetpoint(TEntity.class.getName(), FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT,
            ControlEvent.RELEASE);
      releasePersistAspect();
      Context.end();
   }

   @Test
   public void testReleasePersistAspectAllowed() throws Exception {
      log.info("start testReleasePersistAspectAllowed()");
      registerSetpoint(TEntity.class.getName(), FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT,
            ControlEvent.RELEASE);
      releasePersistAspectAllowed();
   }

   @Test
   public void testReleasePersistAspectClassAspect() throws Exception {
      log.info("start testReleasePersistAspectClassAspect()");
      registerSetpoint(TEntity.class.getName(), FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT,
            ControlEvent.RELEASE);
      Context.sessionScope().setUser(AUSER);
      Context.internalRequestScope().setManaged(true);
      CibetContextAspectTestHelper helper = new CibetContextAspectTestHelper(this);
      helper.releasePersistAspect();
      Context.end();
   }

   @CibetContext(allowAnonymous = true)
   public void releasePersistAspectAllowed() throws Exception {
      log.info("start releasePersistAspectAllowed()");

      TEntity ent = persistTEntity();
      Assert.assertEquals(0, ent.getId());

      List<Controllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());
      Controllable co = l.get(0);
      Assert.assertEquals("ANONYMOUS", co.getCreateUser());

      Context.sessionScope().setUser("test2");
      Object res = co.release(applEman, null);
      Assert.assertNotNull(res);
      Assert.assertTrue(res instanceof TEntity);
      Assert.assertTrue(((TEntity) res).getId() != 0);

      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      Context.sessionScope().setUser("ANONYMOUS");
      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      TEntity te = applEman.find(TEntity.class, ((TEntity) res).getId());
      Assert.assertNotNull(te);
      Context.requestScope().setRollbackOnly(true);
   }

   @CibetContext
   public void releasePersistAspect() throws Exception {
      log.info("start releasePersistAspect()");

      TEntity ent = persistTEntity();
      Assert.assertEquals(0, ent.getId());

      List<Controllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());
      Controllable co = l.get(0);
      Assert.assertEquals(AUSER, co.getCreateUser());

      Context.sessionScope().setUser("test2");
      Object res = co.release(applEman, null);
      Assert.assertNotNull(res);
      Assert.assertTrue(res instanceof TEntity);
      Assert.assertTrue(((TEntity) res).getId() != 0);

      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      Context.sessionScope().setUser(AUSER);
      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      TEntity te = applEman.find(TEntity.class, ((TEntity) res).getId());
      Assert.assertNotNull(te);
      Context.requestScope().setRollbackOnly(true);
   }

}
