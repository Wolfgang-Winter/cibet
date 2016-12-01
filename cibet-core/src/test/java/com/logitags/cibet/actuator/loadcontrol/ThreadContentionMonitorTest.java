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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cibethelper.loadcontrol.MonitorTestClass;
import com.cibethelper.loadcontrol.TAlarmExecution;
import com.logitags.cibet.config.Configuration;

public class ThreadContentionMonitorTest {

   private static Logger log = Logger.getLogger(ThreadContentionMonitorTest.class);

   private static final String SP = "com.logitags.cibet.actuator.loadcontrol.x";

   private long average;
   private long current;

   public static AtomicInteger counter = new AtomicInteger(0);

   @BeforeClass
   public static void beforeClass() throws Exception {
      Configuration.instance().close();
      Field f = Configuration.class.getDeclaredField("instance");
      f.setAccessible(true);
      f.set(null, null);
      Configuration.instance();
   }

   private class ThreadExecution extends Thread {

      public void run() {
         LoadControlActuator act = (LoadControlActuator) Configuration.instance()
               .getActuator(LoadControlActuator.DEFAULTNAME);

         MonitorTestClass mon = new MonitorTestClass();
         mon.cibetCalcSync(3000000, "");
         average = act.getThreadContentionMonitor().getAverageBlockedTime(SP);
         // current = act.getThreadContentionMonitor().getCurrentBlockedTime("method-1");
         current = act.getThreadContentionMonitor().getCurrentBlockedTime(SP);
         log.info("average=" + average);
         log.info("current=" + current);
      }

   }

   @Test
   public void monitor() throws Exception {
      log.debug("start monitor()");
      LoadControlActuator act = (LoadControlActuator) Configuration.instance()
            .getActuator(LoadControlActuator.DEFAULTNAME);
      try {
         act.getThreadContentionMonitor().setStatus(MonitorStatus.ON);

         int nbr = 10;
         List<ThreadExecution> tlist = new ArrayList<ThreadExecution>();
         for (int i = 0; i < nbr; i++) {
            ThreadExecution t = new ThreadExecution();
            tlist.add(t);
         }

         log.info("start threads");
         for (ThreadExecution te : tlist) {
            te.start();
         }
         // Thread.sleep(500);
         log.info("join threads");
         for (ThreadExecution te : tlist) {
            te.join();
         }
         log.info("threads joined");
         Assert.assertTrue(average > 10);
         Assert.assertTrue(current > 10);

      } finally {
         act.getThreadContentionMonitor().setStatus(MonitorStatus.OFF);
      }
   }

   @Test
   public void alarm() throws Exception {
      log.debug("start alarm()");
      LoadControlActuator act = (LoadControlActuator) Configuration.instance()
            .getActuator(LoadControlActuator.DEFAULTNAME);
      try {
         act.getThreadContentionMonitor().setStatus(MonitorStatus.ON);
         act.getThreadCountMonitor().setStatus(MonitorStatus.OFF);
         act.getThreadContentionMonitor().setAlarmThreshold("1000");
         act.setLoadControlCallback(new TAlarmExecution());

         MonitorTestClass mon = new MonitorTestClass();
         for (int i = 0; i < 20; i++) {
            mon.cibetCalcSync(1000000, "");
         }

         int nbr = 10;
         List<ThreadExecution> tlist = new ArrayList<ThreadExecution>();
         for (int i = 0; i < nbr; i++) {
            ThreadExecution t = new ThreadExecution();
            tlist.add(t);
         }

         log.info("start threads");
         for (ThreadExecution te : tlist) {
            te.start();
         }
         log.info("join threads");
         for (ThreadExecution te : tlist) {
            te.join();
         }
         log.info("threads joined");

         Assert.assertTrue(average > 10);
         Assert.assertTrue(current > 10);
         log.info("counter=" + counter.get());
         Assert.assertEquals(10, counter.get());

         for (int i = 0; i < 10; i++) {
            mon.cibetCalcSync(500000, "");
         }

         log.info("second round");
         tlist = new ArrayList<ThreadExecution>();
         for (int i = 0; i < nbr; i++) {
            ThreadExecution t = new ThreadExecution();
            tlist.add(t);
         }
         for (ThreadExecution te : tlist) {
            te.start();
         }
         log.info("join threads");
         for (ThreadExecution te : tlist) {
            te.join();
         }
         log.info("threads joined");
         log.info("counter=" + counter.get());
         Assert.assertEquals(20, counter.get());

      } finally {
         act.getThreadContentionMonitor().setAlarmThreshold("-1");
         act.setLoadControlCallback(null);
         act.getThreadContentionMonitor().setStatus(MonitorStatus.OFF);
         counter.set(0);
      }
   }

   @Test
   public void shed() throws Exception {
      log.debug("start shed()");
      LoadControlActuator act = (LoadControlActuator) Configuration.instance()
            .getActuator(LoadControlActuator.DEFAULTNAME);
      try {
         act.getThreadContentionMonitor().setStatus(MonitorStatus.ON);
         act.getThreadCountMonitor().setStatus(MonitorStatus.OFF);
         act.getThreadContentionMonitor().setShedThreshold("500");
         act.setLoadControlCallback(new TAlarmExecution());

         MonitorTestClass mon = new MonitorTestClass();
         for (int i = 0; i < 20; i++) {
            mon.cibetCalcSync(50000, "");
         }

         int nbr = 30;
         List<ThreadExecution> tlist = new ArrayList<ThreadExecution>();
         for (int i = 0; i < nbr; i++) {
            ThreadExecution t = new ThreadExecution();
            tlist.add(t);
         }

         log.info("start threads");
         for (ThreadExecution te : tlist) {
            te.start();
            Thread.sleep(100);
         }
         log.info("join threads");
         for (ThreadExecution te : tlist) {
            te.join();
         }
         log.info("threads joined");

         Assert.assertTrue(average > 10);
         Assert.assertTrue(current > 10);
         log.info("counter=" + counter.get());
         Assert.assertTrue(counter.get() > 4);

      } finally {
         act.getThreadContentionMonitor().setShedThreshold("-1");
         act.setLoadControlCallback(null);
         act.getThreadContentionMonitor().setStatus(MonitorStatus.OFF);
         counter.set(0);
      }
   }

}
