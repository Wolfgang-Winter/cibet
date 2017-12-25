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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.cibethelper.entities.TEntity;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;

public class JpaControlPTTest {

   private static Logger log = Logger.getLogger(JpaControlPTTest.class);

   protected static EntityManager em;

   private EntityManager cib;

   private TEntity te = new TEntity("Hansi", 99, "owned by x");

   private int nbr = 30;

   public JpaControlPTTest() {
      log.debug("constructor called");
      em = null;
      try {
         init();
      } catch (Exception e) {
         log.error(e.getMessage(), e);
         throw new RuntimeException(e);
      }
   }

   private void init() throws Exception {
      te.setId(5);

      em = Mockito.mock(EntityManager.class);
      Mockito.when(em.merge(te)).thenReturn(te);

      Context.internalRequestScope().setEntityManager(em);
      Context.sessionScope().setUser("USER");
      Context.sessionScope().setTenant("TENANT");
      Configuration cm = Configuration.instance();
      List<Setpoint> spList = cm.getSetpoints();
      for (Setpoint sp : spList) {
         log.debug("unregister setpoint " + sp.getId());
         cm.unregisterSetpoint(null, sp.getId());
      }
      log.debug("ConfigurationManager contains " + cm.getSetpoints().size() + " setpoints");

      for (int i = 0; i < nbr; i++) {

         List<String> schemes = new ArrayList<String>();
         registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE);
         Thread.sleep(5);
      }
   }

   @Test
   public void testMerge() {
      log.debug("start merge");
      cib = new CibetEntityManager(em, false);
      TEntity res = cib.merge(te);
      Assert.assertEquals(te, res);
   }

   private Setpoint registerSetpoint(String clazz, List<String> acts, ControlEvent... events) {
      Setpoint sp = new Setpoint(String.valueOf(new Date().getTime()));
      sp.addTargetIncludes(clazz);
      for (ControlEvent ce : events) {
         sp.addEventIncludes(ce);
      }
      sp.addInvokerIncludes("com.logitags.cibet.*");
      sp.addMethodIncludes("*");
      sp.addTenantIncludes("TENANT");
      Configuration cman = Configuration.instance();
      for (String scheme : acts) {
         sp.addActuator(cman.getActuator(scheme));
      }
      cman.registerSetpoint(sp);
      return sp;
   }

}
