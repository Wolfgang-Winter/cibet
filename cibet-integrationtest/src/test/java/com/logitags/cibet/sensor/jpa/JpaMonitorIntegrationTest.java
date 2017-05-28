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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.cibethelper.base.DBHelper;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.archive.ArchiveLoader;
import com.logitags.cibet.actuator.circuitbreaker.CircuitBreakerActuator;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.ExecutionStatus;

/**
 * tests CibetEntityManager with Archive and FourEyes- actuators.
 * 
 * @author test
 * 
 */
public class JpaMonitorIntegrationTest extends DBHelper {

   private static Logger log = Logger.getLogger(JpaMonitorIntegrationTest.class);

   // @Test
   public void persistNoTimeout() {
      log.info("start persistNoTimeout()");

      CircuitBreakerActuator cbreaker = (CircuitBreakerActuator) Configuration.instance()
            .getActuator(CircuitBreakerActuator.DEFAULTNAME);
      cbreaker.setTimeout(1000L);

      List<String> schemes = new ArrayList<String>();
      schemes.add(CircuitBreakerActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT);

      TEntity entity = persistTEntity();
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull(selEnt);

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(entity.getClass().getName(),
            String.valueOf(entity.getId()));
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      JpaResource res = (JpaResource) ar.getResource();
      Assert.assertEquals(ControlEvent.INSERT, ar.getControlEvent());
      Assert.assertEquals(String.valueOf(entity.getId()), res.getPrimaryKeyId());
   }

   @Ignore
   @Test
   public void persistTimeout() {
      log.info("start persistTimeout()");

      CircuitBreakerActuator cbreaker = (CircuitBreakerActuator) Configuration.instance()
            .getActuator(CircuitBreakerActuator.DEFAULTNAME);
      cbreaker.setTimeout(50L);

      List<String> schemes = new ArrayList<String>();
      schemes.add(CircuitBreakerActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT);

      TEntity entity = persistTEntity();
      // TEntity selEnt = cibetEman.find(TEntity.class, entity.getId());
      Assert.assertEquals(0, entity.getId());

      List<Archive> list = ArchiveLoader.loadArchives();
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      log.debug(ar);
      JpaResource res = (JpaResource) ar.getResource();
      log.debug(res);
      Assert.assertEquals(ControlEvent.INSERT, ar.getControlEvent());
      // Assert.assertEquals(String.valueOf(entity.getId()),
      // res.getPrimaryKeyId());
      Assert.assertEquals(ExecutionStatus.TIMEOUT, ar.getExecutionStatus());
   }

}
