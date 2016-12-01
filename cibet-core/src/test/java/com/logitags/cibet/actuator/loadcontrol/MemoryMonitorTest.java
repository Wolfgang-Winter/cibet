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
import com.cibethelper.loadcontrol.TAlarmExecution;
import com.logitags.cibet.config.Configuration;

public class MemoryMonitorTest {

   private static Logger log = Logger.getLogger(MemoryMonitorTest.class);

   private static final String SP = "SP2-javaMethod";

   public static AtomicInteger shedCounter = new AtomicInteger(0);
   public static AtomicInteger valveCounter = new AtomicInteger(0);
   public static LoadControlData data;

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
   public void show() throws Exception {
      log.debug("start show()");
      MonitorTestClass mon = new MonitorTestClass();
      mon.cibetMem2(10000, null);

      double load = vm.getHeapMemoryUsagePercent();
      log.debug("memory load=" + load);
      Assert.assertTrue(load > 0);
      load = vm.getTenuredGenCollectionUsagePercent();
      log.debug("memory load=" + load);
      Assert.assertTrue(load >= 0);
      load = vm.getTenuredGenUsagePercent();
      log.debug("memory load=" + load);
      Assert.assertTrue(load > 0);
      load = vm.getHeapMemoryUsage();
      log.debug("memory load=" + load);
      Assert.assertTrue(load > 0);
      load = vm.getTenuredGenCollectionUsage();
      log.debug("memory load=" + load);
      Assert.assertTrue(load >= 0);
      load = vm.getTenuredGenUsage();
      log.debug("memory load=" + load);
      Assert.assertTrue(load > 0);
   }

   @Test
   public void processShed() throws Exception {
      log.debug("start processShed()");
      LoadControlActuator act = (LoadControlActuator) Configuration.instance()
            .getActuator(LoadControlActuator.DEFAULTNAME);
      try {
         MonitorTestClass mon = new MonitorTestClass();
         mon.cibetMem2(30000, null);

         act.getMemoryMonitor().setStatus(MonitorStatus.ON);
         act.setLoadControlCallback(new TAlarmExecution());
         act.getMemoryMonitor().setCollectionUsageShedThreshold("4% ");
         act.getMemoryMonitor().setUsageShedThreshold("4% ");

         int max = 6;
         for (int i = 0; i < max; i++) {
            log.debug("vm.getHeapMemoryUsagePercent()=" + vm.getHeapMemoryUsagePercent());
            log.debug("vm.getTenuredGenCollectionUsagePercent()=" + vm.getTenuredGenCollectionUsagePercent());
            log.debug("vm.getTenuredGenUsagePercent()=" + vm.getTenuredGenUsagePercent());
            String res = mon.cibetMem2(10000, null);
            Assert.assertNull(res);
         }
         log.debug("shedCounter=" + shedCounter.get());
         Assert.assertEquals(6, shedCounter.get());
      } finally {
         shedCounter.set(0);
         act.getMemoryMonitor().setCollectionUsageShedThreshold("-1 ");
         act.getMemoryMonitor().setUsageShedThreshold("-1");
         act.getMemoryMonitor().setStatus(MonitorStatus.OFF);
      }
   }

   @Test
   public void valve() throws Exception {
      log.debug("start valve()");
      LoadControlActuator act = (LoadControlActuator) Configuration.instance()
            .getActuator(LoadControlActuator.DEFAULTNAME);
      try {
         MonitorTestClass mon = new MonitorTestClass();
         mon.cibetMem2(30000, null);

         act.getMemoryMonitor().setStatus(MonitorStatus.ON);
         act.setLoadControlCallback(new TAlarmExecution());
         act.getMemoryMonitor().setCollectionUsageValveThreshold("4% ");
         act.getMemoryMonitor().setUsageValveThreshold("4% ");

         int max = 6;
         for (int i = 0; i < max; i++) {
            log.debug("vm.getHeapMemoryUsagePercent()=" + vm.getHeapMemoryUsagePercent());
            log.debug("vm.getTenuredGenCollectionUsagePercent()=" + vm.getTenuredGenCollectionUsagePercent());
            log.debug("vm.getTenuredGenUsagePercent()=" + vm.getTenuredGenUsagePercent());
            log.debug("ThrottleCount: " + act.getMemoryMonitor().getThrottleCount(SP));
            String res = mon.cibetMem2(10000, null);
            Assert.assertNull(res);
            Assert.assertTrue(data.getThrottleTime() > 500);

            data = null;
         }
         log.debug("shedCounter=" + shedCounter.get());
         Assert.assertEquals(6, shedCounter.get());
      } finally {
         valveCounter.set(0);
         act.getMemoryMonitor().setCollectionUsageValveThreshold("-1 ");
         act.getMemoryMonitor().setUsageValveThreshold("-1");
         act.getMemoryMonitor().setStatus(MonitorStatus.OFF);
      }
   }

   @Test
   public void alarm() throws Exception {
      log.debug("start alarm()");
      LoadControlActuator act = (LoadControlActuator) Configuration.instance()
            .getActuator(LoadControlActuator.DEFAULTNAME);
      try {
         MonitorTestClass mon = new MonitorTestClass();
         mon.cibetMem2(30000, null);

         act.getMemoryMonitor().setStatus(MonitorStatus.ON);
         act.setLoadControlCallback(new TAlarmExecution());
         act.getMemoryMonitor().setCollectionUsageAlarmThreshold("4% ");
         act.getMemoryMonitor().setUsageAlarmThreshold("4% ");

         int max = 4;
         for (int i = 0; i < max; i++) {
            log.debug("vm.getHeapMemoryUsagePercent()=" + vm.getHeapMemoryUsagePercent());
            log.debug("vm.getTenuredGenCollectionUsagePercent()=" + vm.getTenuredGenCollectionUsagePercent());
            log.debug("vm.getTenuredGenUsagePercent()=" + vm.getTenuredGenUsagePercent());
            String res = mon.cibetMem2(10000, null);
            Assert.assertNotNull(res);
         }
         log.debug("valveCounter=" + valveCounter.get());
         Assert.assertTrue(valveCounter.get() >= 2);
      } finally {
         shedCounter.set(0);
         valveCounter.set(0);
         act.getMemoryMonitor().setCollectionUsageAlarmThreshold("-1 ");
         act.getMemoryMonitor().setUsageAlarmThreshold("-1");
         act.getMemoryMonitor().setStatus(MonitorStatus.OFF);
      }
   }

}
