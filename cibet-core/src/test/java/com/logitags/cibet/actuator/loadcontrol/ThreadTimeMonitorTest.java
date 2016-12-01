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

import com.cibethelper.loadcontrol.DummyMonitor;
import com.cibethelper.loadcontrol.MonitorTestClass;
import com.cibethelper.loadcontrol.TAlarmExecution;
import com.logitags.cibet.config.Configuration;

public class ThreadTimeMonitorTest {

   private static Logger log = Logger.getLogger(ThreadTimeMonitorTest.class);

   private static final String SP = "com.logitags.cibet.actuator.loadcontrol.x";

   public static AtomicInteger counter = new AtomicInteger(0);
   public static AtomicInteger customCounter = new AtomicInteger(0);

   @BeforeClass
   public static void beforeClass() throws Exception {
      Configuration.instance().close();
      Field f = Configuration.class.getDeclaredField("instance");
      f.setAccessible(true);
      f.set(null, null);
      Configuration.instance();
   }

   @Test
   public void monitor() throws Exception {
      log.debug("start ThreadTimeMonitorTest monitor()");
      LoadControlActuator act = (LoadControlActuator) Configuration.instance()
            .getActuator(LoadControlActuator.DEFAULTNAME);
      try {
         act.getThreadTimeMonitor().setStatus(MonitorStatus.ON);

         MonitorTestClass mon = new MonitorTestClass();
         for (int i = 1; i < 19; i++) {
            mon.cibetCalc(100000, null);
         }

         int avCpu = act.getThreadTimeMonitor().getAverageThreadCpuTime(SP);
         log.debug("average CPU time =" + avCpu);
         Assert.assertTrue(avCpu > 0);
         int avUser = act.getThreadTimeMonitor().getAverageThreadUserTime(SP);
         log.debug("average User time =" + avUser);
         Assert.assertTrue(avUser > 0);

         int value = act.getThreadTimeMonitor().getMaximumThreadCpuTime(SP);
         log.debug("MaximumThreadCpuTime =" + value);
         Assert.assertTrue(value > avCpu);

         value = act.getThreadTimeMonitor().getMaximumThreadUserTime(SP);
         log.debug("MaximumThreadUserTime =" + value);
         Assert.assertTrue(value > avUser);

         value = act.getThreadTimeMonitor().getMinimumThreadCpuTime(SP);
         log.debug("MinimumThreadCpuTime =" + value);
         Assert.assertTrue(value > 0);
         Assert.assertTrue(value < avCpu);

         value = act.getThreadTimeMonitor().getMinimumThreadUserTime(SP);
         log.debug("MinimumThreadUserTime =" + value);
         Assert.assertTrue(value > 0);
         Assert.assertTrue(value < avUser);

      } finally {
         act.getThreadTimeMonitor().setStatus(MonitorStatus.OFF);
      }
   }

   @Test
   public void alarmCpuTime() throws Exception {
      log.debug("start ThreadTimeMonitorTest alarmCpuTime()");
      LoadControlActuator act = (LoadControlActuator) Configuration.instance()
            .getActuator(LoadControlActuator.DEFAULTNAME);
      try {
         act.getThreadTimeMonitor().reset(SP);
         act.getThreadTimeMonitor().setStatus(MonitorStatus.ON);
         act.getThreadTimeMonitor().setAlarmCpuTimeThreshold("20");
         act.setLoadControlCallback(new TAlarmExecution());

         MonitorTestClass mon = new MonitorTestClass();

         for (int i = 1; i < 19; i++) {
            mon.cibetCalc(100000, null);
         }
         log.info("CurrentThreadCpuTime: " + act.getThreadTimeMonitor().getCurrentThreadCpuTime(SP));
         log.debug("average CPU time =" + act.getThreadTimeMonitor().getAverageThreadCpuTime(SP));
         Assert.assertEquals(0, counter.get());

         for (int i = 1; i < 5; i++) {
            mon.cibetCalc(200000, null);
            log.info("CurrentThreadCpuTime: " + act.getThreadTimeMonitor().getCurrentThreadCpuTime(SP));
         }
         Assert.assertEquals(3, counter.get());

      } finally {
         counter = new AtomicInteger(0);
         act.getThreadTimeMonitor().setAlarmCpuTimeThreshold("-1");
         act.getThreadTimeMonitor().setStatus(MonitorStatus.OFF);
      }
   }

   @Test
   public void alarmUserTime() throws Exception {
      log.debug("start ThreadTimeMonitorTest alarmUserTime()");
      LoadControlActuator act = (LoadControlActuator) Configuration.instance()
            .getActuator(LoadControlActuator.DEFAULTNAME);
      try {
         act.getThreadTimeMonitor().reset(SP);
         act.getThreadTimeMonitor().setStatus(MonitorStatus.ON);
         act.getThreadTimeMonitor().setAlarmUserTimeThreshold("20");
         act.setLoadControlCallback(new TAlarmExecution());

         MonitorTestClass mon = new MonitorTestClass();

         for (int i = 1; i < 19; i++) {
            mon.cibetCalc(100000, null);
         }
         Assert.assertEquals(0, counter.get());

         for (int i = 1; i < 5; i++) {
            mon.cibetCalc(200000, null);
         }
         Assert.assertEquals(3, counter.get());

      } finally {
         counter = new AtomicInteger(0);
         act.getThreadTimeMonitor().setAlarmUserTimeThreshold("-1");
         act.getThreadTimeMonitor().setStatus(MonitorStatus.OFF);
      }
   }

   @Test
   public void alarmCpuTimePercent() throws Exception {
      log.debug("start ThreadTimeMonitorTest alarmCpuTimePercent()");
      LoadControlActuator act = (LoadControlActuator) Configuration.instance()
            .getActuator(LoadControlActuator.DEFAULTNAME);
      try {
         act.getThreadTimeMonitor().reset(SP);
         act.getThreadTimeMonitor().setStatus(MonitorStatus.ON);
         act.getThreadTimeMonitor().setAlarmCpuTimeThreshold("50%");
         act.setLoadControlCallback(new TAlarmExecution());

         MonitorTestClass mon = new MonitorTestClass();

         for (int i = 1; i < 19; i++) {
            mon.cibetCalc(100000, null);
         }
         Assert.assertEquals(0, counter.get());

         for (int i = 1; i < 5; i++) {
            mon.cibetCalc(200000, null);
         }
         Assert.assertEquals(3, counter.get());

      } finally {
         counter = new AtomicInteger(0);
         act.getThreadTimeMonitor().setAlarmCpuTimeThreshold("-1");
         act.getThreadTimeMonitor().setStatus(MonitorStatus.OFF);
         act.getThreadTimeMonitor().reset(SP);
      }
   }

   @Test
   public void alarmUserTimePercent() throws Exception {
      log.debug("start ThreadTimeMonitorTest alarmUserTimePercent()");
      LoadControlActuator act = (LoadControlActuator) Configuration.instance()
            .getActuator(LoadControlActuator.DEFAULTNAME);

      Assert.assertEquals(2, act.getCustomMonitors().length);
      for (Monitor m : act.getCustomMonitors()) {
         m.setStatus(MonitorStatus.ON);
      }
      Monitor m1 = act.getCustomMonitor(DummyMonitor.NAME);
      Assert.assertNotNull(m1);

      try {
         act.getThreadTimeMonitor().reset(SP);
         act.getThreadTimeMonitor().setStatus(MonitorStatus.ON);
         act.getThreadTimeMonitor().setAlarmUserTimeThreshold(" 50% ");
         act.setLoadControlCallback(new TAlarmExecution());

         MonitorTestClass mon = new MonitorTestClass();

         for (int i = 1; i < 19; i++) {
            mon.cibetCalc(100000, null);
         }
         Assert.assertEquals(0, counter.get());

         for (int i = 1; i < 5; i++) {
            mon.cibetCalc(200000, null);
         }
         Assert.assertEquals(3, counter.get());

         log.info("customCounter= " + customCounter.get());
         Assert.assertEquals(44, customCounter.get());

      } finally {
         counter = new AtomicInteger(0);
         act.getThreadTimeMonitor().setAlarmUserTimeThreshold("-1");
         act.getThreadTimeMonitor().setStatus(MonitorStatus.OFF);
         for (Monitor m : act.getCustomMonitors()) {
            m.setStatus(MonitorStatus.OFF);
         }
         customCounter.set(0);
      }
   }

}
