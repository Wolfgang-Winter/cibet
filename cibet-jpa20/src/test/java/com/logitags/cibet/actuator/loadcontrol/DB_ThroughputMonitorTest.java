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
package com.logitags.cibet.actuator.loadcontrol;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cibethelper.entities.DBTAlarmExecution;
import com.cibethelper.loadcontrol.MonitorTestClass;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.context.CibetContext;

public class DB_ThroughputMonitorTest {

   private static Logger log = Logger.getLogger(DB_ThroughputMonitorTest.class);

   private static final String SP = "com.logitags.cibet.actuator.loadcontrol.x";

   public static AtomicInteger throttleCounter = new AtomicInteger(0);
   public static AtomicInteger alarmCounter = new AtomicInteger(0);
   public static AtomicInteger shedCounter = new AtomicInteger(0);

   private LoadControlActuator act = (LoadControlActuator) Configuration.instance()
         .getActuator(LoadControlActuator.DEFAULTNAME);
   private static MonitorTestClass mon = new MonitorTestClass();

   @BeforeClass
   public static void beforeClass() throws Exception {
      Configuration.instance().close();
      Field f = Configuration.class.getDeclaredField("instance");
      f.setAccessible(true);
      f.set(null, null);
      Configuration.instance();

      log.debug("xx1");
      mon.insertJMEntity(3000);
      log.debug("xx1");

   }

   @AfterClass
   public static void afterClass() {
      mon.deleteJMEntity();
   }

   @Test
   public void monitor() throws Exception {
      log.debug("start monitor throughput(");
      act.getThroughputMonitor().setStatus(MonitorStatus.ON);
      for (int i = 1; i < 10; i++) {
         mon.cibetSelect(1000000, null);
      }
      int th = act.getThroughputMonitor().getThroughput(SP);
      log.debug("throughput=" + th);
      Assert.assertTrue(th > 0);
   }

   @Test
   public void processAlarm() throws Exception {
      log.debug("start throughput processAlarm()");
      try {
         act.getThroughputMonitor().setStatus(MonitorStatus.ON);
         act.setLoadControlCallback(new DBTAlarmExecution());
         act.getThroughputMonitor().setAlarmThreshold(5);

         for (int i = 1; i < 15; i++) {
            mon.cibetSelect(1000000, null);
            int th = act.getThroughputMonitor().getThroughput(SP);
            log.debug("throughput=" + th);
         }

         log.info("alarmCounter=" + alarmCounter.get());
         Assert.assertTrue(alarmCounter.get() > 5);
      } finally {
         alarmCounter.set(0);
         act.getThroughputMonitor().setAlarmThreshold(-1);
         act.getThroughputMonitor().setStatus(MonitorStatus.OFF);
      }
   }

   @Test
   public void processShed() throws Exception {
      log.debug("start throughput processShed()");
      try {
         act.getThroughputMonitor().setStatus(MonitorStatus.ON);
         act.setLoadControlCallback(new DBTAlarmExecution());
         act.getThroughputMonitor().setShedThreshold(7);

         for (int i = 1; i < 15; i++) {
            mon.cibetSelect(1000000, null);
            int th = act.getThroughputMonitor().getThroughput(SP);
            log.debug("throughput=" + th);
         }

         log.info("shedCounter=" + shedCounter.get());
         Assert.assertTrue(shedCounter.get() > 5);

      } finally {
         shedCounter.set(0);
         act.getThroughputMonitor().setShedThreshold(-1);
         act.getThroughputMonitor().setStatus(MonitorStatus.OFF);
      }
   }

   @Test
   @CibetContext
   public void processValve() throws Exception {
      log.debug("start throughput processValve()");
      try {
         act.getThroughputMonitor().setStatus(MonitorStatus.ON);
         act.setLoadControlCallback(new DBTAlarmExecution());
         act.getThroughputMonitor().setValveThreshold(7);
         act.getThroughputMonitor().setThrottleMaxTime(2000);

         for (int i = 1; i < 25; i++) {
            log.debug("beforeselect");
            mon.cibetSelect(1000000, null);
            log.debug("afterselect");
            int th = act.getThroughputMonitor().getThroughput(SP);
            log.debug("throughput=" + th);
         }

         log.info("throttleCounter=" + throttleCounter.get());
         log.info("shedCounter=" + shedCounter.get());
         Assert.assertTrue(throttleCounter.get() > 2);
         Assert.assertEquals(0, shedCounter.get());

      } catch (Exception e) {
         log.error(e.getMessage(), e);

      } finally {
         shedCounter.set(0);
         throttleCounter.set(0);
         act.getThroughputMonitor().setValveThreshold(-1);
         act.getThroughputMonitor().setStatus(MonitorStatus.OFF);
      }
   }

}
