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
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cibethelper.loadcontrol.MonitorTestClass;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.context.CibetContext;
import com.logitags.cibet.context.Context;

public class CpuLoadMonitorTest {

   private static Logger log = Logger.getLogger(CpuLoadMonitorTest.class);

   private static final String SP = "SP2-javaMethod";

   public static AtomicInteger processCounter = new AtomicInteger(0);
   public static AtomicInteger systemCounter = new AtomicInteger(0);
   public static AtomicInteger shedCounter = new AtomicInteger(0);

   private VMLoadControlJMXBean vm = new VMLoadControlJMXBean();

   @BeforeClass
   public static void beforeClass() throws Exception {
      Configuration.instance().close();
      Field f = Configuration.class.getDeclaredField("instance");
      f.setAccessible(true);
      f.set(null, null);
      Configuration.instance();
   }

   @Test
   public void showSystemLoad() throws Exception {
      log.debug("start showSystemLoad(");
      double load = vm.getProcessCpuLoad();
      MonitorTestClass mon = new MonitorTestClass();
      mon.cibetCalc(1000000, null);
      log.debug("process load=" + load);
      load = vm.getSystemCpuLoad();
      Assert.assertTrue(load > 0);
      log.debug("system load=" + load);
   }

   @Test
   public void processAlarm() throws Exception {
      log.debug("start processAlarm()");
      LoadControlActuator act = (LoadControlActuator) Configuration.instance()
            .getActuator(LoadControlActuator.DEFAULTNAME);
      try {
         act.getCpuLoadMonitor().setStatus(MonitorStatus.ON);
         act.setLoadControlCallback(new TAlarmExecution());
         act.getCpuLoadMonitor().setProcessAlarmThreshold("5% ");
         act.getCpuLoadMonitor().setSystemAlarmThreshold("5% ");

         MonitorTestClass mon = new MonitorTestClass();
         for (int i = 1; i < 5; i++) {
            String res = mon.cibetCalc(1000000, null);
            Assert.assertNotNull(res);
            log.debug("process load=" + vm.getProcessCpuLoad());
            log.debug("system load=" + vm.getSystemCpuLoad());
         }
         // Assert.assertEquals(1, processCounter.get());
         Assert.assertEquals(4, systemCounter.get());
      } finally {
         processCounter.set(0);
         systemCounter.set(0);
         act.getCpuLoadMonitor().setProcessAlarmThreshold("-1");
         act.getCpuLoadMonitor().setSystemAlarmThreshold("-1");
         act.getCpuLoadMonitor().setStatus(MonitorStatus.OFF);
      }
   }

   @Test
   public void processShed() throws Exception {
      log.debug("start processShed()");
      LoadControlActuator act = (LoadControlActuator) Configuration.instance()
            .getActuator(LoadControlActuator.DEFAULTNAME);
      try {
         act.getCpuLoadMonitor().setStatus(MonitorStatus.ON);
         act.setLoadControlCallback(new TAlarmExecution());
         act.getCpuLoadMonitor().setProcessAlarmThreshold("5% ");
         act.getCpuLoadMonitor().setSystemAlarmThreshold("5% ");
         act.getCpuLoadMonitor().setProcessShedThreshold("15% ");
         act.getCpuLoadMonitor().setSystemShedThreshold("15% ");

         int max = 10;
         MonitorTestClass mon = new MonitorTestClass();
         for (int i = 0; i < max; i++) {
            String res = mon.cibetCalc(1000000, null);
            log.debug("process load=" + vm.getProcessCpuLoad());
            log.debug("system load=" + vm.getSystemCpuLoad());
            log.debug(res);
            Assert.assertNull(res);
         }
         Assert.assertEquals(10, shedCounter.get());
      } finally {
         processCounter.set(0);
         systemCounter.set(0);
         act.getCpuLoadMonitor().setProcessAlarmThreshold("-1 ");
         act.getCpuLoadMonitor().setSystemAlarmThreshold("-1");
         act.getCpuLoadMonitor().setProcessShedThreshold("-1");
         act.getCpuLoadMonitor().setSystemShedThreshold("-1");
         act.getCpuLoadMonitor().setStatus(MonitorStatus.OFF);
      }
   }

   @Test
   @CibetContext
   public void processValve() throws Exception {
      log.debug("start processValve()");
      LoadControlActuator act = (LoadControlActuator) Configuration.instance()
            .getActuator(LoadControlActuator.DEFAULTNAME);
      try {
         act.getCpuLoadMonitor().setStatus(MonitorStatus.ON);
         act.setLoadControlCallback(new TAlarmExecution());
         act.getCpuLoadMonitor().setProcessValveThreshold("25%");

         int max = 10;
         MonitorTestClass mon = new MonitorTestClass();
         for (int i = 0; i < max; i++) {
            String res = mon.cibetCalc(1000000, null);
            log.debug("EventResult: " + Context.requestScope().getExecutedEventResult());
            log.debug("process load=" + vm.getProcessCpuLoad());
            log.debug("system load=" + vm.getSystemCpuLoad());
            log.debug("ThrottleCount: " + act.getCpuLoadMonitor().getThrottleCount(SP));
            Assert.assertNotNull(res);
         }
         Assert.assertTrue(processCounter.get() > 0);
      } finally {
         processCounter.set(0);
         systemCounter.set(0);
         act.getCpuLoadMonitor().setProcessValveThreshold("-1");
         act.getCpuLoadMonitor().setStatus(MonitorStatus.OFF);
      }
   }

}
