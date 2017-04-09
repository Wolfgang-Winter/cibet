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
package com.logitags.cibet.sensor.jpa;

import java.util.List;

import javax.persistence.PersistenceException;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import com.cibethelper.base.DBHelper;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.archive.ArchiveLoader;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.resource.Resource;

public class ProviderIntegrationTest extends DBHelper {

   private static Logger log = Logger.getLogger(ProviderIntegrationTest.class);

   private Setpoint sp = null;

   @After
   public void afterProviderIntegrationTest() {
      if (sp != null) {
         Configuration.instance().unregisterSetpoint(sp.getId());
      }
   }

   @Test
   public void createEntityManagerFactory() {
      log.debug("start createEntityManagerFactory()");
      Assert.assertTrue(fac instanceof CibetEntityManagerFactory);

      Assert.assertTrue(applEman instanceof CibetEntityManager);

      Context.sessionScope().setUser("xx");

      sp = registerSetpoint(TEntity.class.getName(), ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT,
            ControlEvent.UPDATE, ControlEvent.DELETE);

      TEntity entity = createTEntity(33, "unter");
      applEman.persist(entity);

      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull(selEnt);
      Assert.assertEquals(33, selEnt.getCounter());

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(entity.getClass().getName(),
            String.valueOf(entity.getId()));
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Resource resource = ar.getResource();
      Assert.assertEquals(TEntity.class.getName(), resource.getTargetType());
      TEntity en = (TEntity) resource.getObject();
      Assert.assertTrue("expected: " + entity.getId() + ", actual: " + en.getId(), en.getId() == entity.getId());
   }

   /**
    * javax.persistence.PersistenceException: org.hibernate.PersistentObjectException: detached entity passed to
    * persist: com.cibethelper.entities.TEntity
    */
   @Test(expected = PersistenceException.class)
   public void createEntityManagerFactoryException() {
      // Eclipselink is tolerant here
      Assume.assumeFalse(GLASSFISH.equals(APPSERVER));
      log.debug("start createEntityManagerFactoryException()");

      TEntity entity = createTEntity(33, "unter");
      entity.setId(1);
      applEman.persist(entity);
   }

   // @Test
   // public void persistEJB() throws Exception {
   // log.debug("start persistEJB()");
   // EmbeddedOpenEjb container = new EmbeddedOpenEjb();
   // log.debug("current container IC: " + container.getInitialContext());
   // if (container.getInitialContext() != null) {
   // log.debug("execute only as single junit test");
   // return;
   // }
   //
   // EjbLookup.setJndiPropertiesFilename("jndi-for-openejb-providertest.properties");
   // registerSetpoint(TEntity.class.getName(), ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT, ControlEvent.UPDATE,
   // ControlEvent.DELETE);
   // Context.sessionScope().setUser("xx");
   //
   // EjbLookup.clearCache();
   // CibetEEContext invoker = EjbLookup.lookupEjb(null, CibetEEContextEJB.class);
   // invoker.setEntityManagerIntoContext();
   //
   // TEntity entity = createTEntity(33, "unter");
   // CibetProviderEJB ejb = EjbLookup.lookupEjb(null, CibetProviderEJBImpl.class);
   // ejb.persist(entity);
   // EjbLookup.setJndiPropertiesFilename("jndi.properties");
   //
   // Query q = cibetEM.createNamedQuery(Archive.SEL_ALL);
   // List<Archive> list = q.getResultList();
   // Assert.assertEquals(2, list.size());
   //
   // q = cibetEM.createQuery("SELECT a FROM TEntity a");
   // List<TEntity> list1 = q.getResultList();
   // Assert.assertEquals(1, list1.size());
   // }
}
