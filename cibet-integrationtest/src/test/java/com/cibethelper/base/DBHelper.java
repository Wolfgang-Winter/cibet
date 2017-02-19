/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2016 Dr. Wolfgang Winter
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
package com.cibethelper.base;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import com.cibethelper.SpringTestAuthenticationManager;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TComplexEntity2;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.common.PostponedException;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.actuator.lock.LockedObject;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InitializationService;
import com.logitags.cibet.context.InternalSessionScope;

public class DBHelper extends CoreTestBase {

   private static Logger log = Logger.getLogger(DBHelper.class);

   public static EntityManager applEman;
   public static EntityManagerFactory fac;

   @BeforeClass
   public static void beforeClass() throws Exception {
      log.debug("beforeClass");
      fac = Persistence.createEntityManagerFactory("localTest");
      applEman = fac.createEntityManager();
   }

   @Before
   public void doBefore() throws Exception {
      log.debug("DBHelper.doBefore");
      InitializationService.instance().startContext();
      applEman.getTransaction().begin();
      Context.sessionScope().setUser(USER);
      Context.sessionScope().setTenant(TENANT);
   }

   @After
   public void doAfter() {
      log.debug("DBHelper:doAfter()");
      if (applEman.getTransaction().isActive()) {
         applEman.getTransaction().rollback();
      }

      applEman.getTransaction().begin();

      Context.internalRequestScope().getEntityManager().flush();
      Query q = applEman.createNamedQuery(TComplexEntity.SEL_ALL);
      List<TComplexEntity> l = q.getResultList();
      for (TComplexEntity tComplexEntity : l) {
         applEman.remove(tComplexEntity);
      }

      Query q1 = applEman.createNamedQuery(TComplexEntity2.SEL_ALL);
      List<TComplexEntity2> l1 = q1.getResultList();
      for (TComplexEntity2 tComplexEntity : l1) {
         applEman.remove(tComplexEntity);
      }

      Query q2 = applEman.createNamedQuery(TEntity.DEL_ALL);
      q2.executeUpdate();

      Query q3 = Context.internalRequestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL);
      List<Archive> alist = q3.getResultList();
      for (Archive ar : alist) {
         Context.internalRequestScope().getEntityManager().remove(ar);
      }

      Query q4 = Context.internalRequestScope().getEntityManager().createQuery("select d from DcControllable d");
      List<DcControllable> dclist = q4.getResultList();
      for (DcControllable dc : dclist) {
         Context.internalRequestScope().getEntityManager().remove(dc);
      }

      Query q5 = Context.internalRequestScope().getEntityManager().createQuery("SELECT a FROM LockedObject a");
      Iterator<LockedObject> itLO = q5.getResultList().iterator();
      while (itLO.hasNext()) {
         Context.internalRequestScope().getEntityManager().remove(itLO.next());
      }

      applEman.getTransaction().commit();
      InitializationService.instance().endContext();
   }

   protected TEntity persistTEntity() {
      TEntity te = createTEntity(5, "valuexx");
      try {
         applEman.persist(te);
      } catch (PostponedException e) {
      }
      applEman.flush();
      applEman.clear();
      return te;
   }

   protected TEntity persistTEntityWithoutClear() {
      TEntity te = createTEntity(5, "valuexx");
      try {
         applEman.persist(te);
      } catch (PostponedException e) {
      }
      applEman.flush();
      return te;
   }

   protected TEntity persistTEntity(int counter, Date now, Calendar cal) {
      TEntity entity = new TEntity();
      entity.setCounter(counter);
      entity.setNameValue("valuexx");
      entity.setOwner(TENANT);
      entity.setXdate(now);
      entity.setXCaldate(cal);
      entity.setXCaltimestamp(cal);
      entity.setXtime(now);
      entity.setXtimestamp(now);
      try {
         applEman.persist(entity);
      } catch (PostponedException e) {
      }
      applEman.flush();
      applEman.clear();
      return entity;
   }

   protected void authenticate(String... roles) throws AuthenticationException {
      SpringTestAuthenticationManager authManager = new SpringTestAuthenticationManager();
      for (String role : roles) {
         authManager.addAuthority(role);
      }

      Authentication request = new UsernamePasswordAuthenticationToken("test", "test");
      Authentication result = authManager.authenticate(request);
      SecurityContextHolder.getContext().setAuthentication(result);
   }

   protected void authenticateSecond(String... roles) {
      log.debug("authenticate second");
      SpringTestAuthenticationManager authManager = new SpringTestAuthenticationManager();
      for (String role : roles) {
         if ("NULL".equals(role)) {
            log.debug("set secondRole = null");
            Context.internalSessionScope().setProperty(InternalSessionScope.SECOND_PRINCIPAL, null);
         } else {
            authManager.addAuthority(role);
         }
      }

      try {
         Authentication request = new UsernamePasswordAuthenticationToken("test", "test");
         Authentication result = authManager.authenticate(request);
         Context.internalSessionScope().setProperty(InternalSessionScope.SECOND_PRINCIPAL, result);
      } catch (AuthenticationException e) {
         log.error("Authentication failed: " + e.getMessage());
      }
   }

}