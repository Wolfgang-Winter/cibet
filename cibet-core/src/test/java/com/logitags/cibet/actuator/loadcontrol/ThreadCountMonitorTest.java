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

public class ThreadCountMonitorTest {

   private static Logger log = Logger.getLogger(ThreadCountMonitorTest.class);

   private volatile long current;

   private static final String SP = "com.logitags.cibet.actuator.loadcontrol.x";

   public static AtomicInteger counter = new AtomicInteger(0);
   public static AtomicInteger shed = new AtomicInteger(0);
   public static AtomicInteger throttled = new AtomicInteger(0);

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
         mon.cibetCalc(800000, "");
         log.debug("after ThreadCount: " + act.getThreadCountMonitor().getThreadCount(SP));
         current = current + act.getThreadCountMonitor().getThreadCount(SP);
         log.debug("throttled: " + act.getThreadCountMonitor().getThrottleCount(SP));
         throttled.set(throttled.get() + act.getThreadCountMonitor().getThrottleCount(SP));
      }

   }

   @Test
   public void monitor() throws Exception {
      log.debug("start ThreadCountMonitorTest monitor()");
      LoadControlActuator act = (LoadControlActuator) Configuration.instance()
            .getActuator(LoadControlActuator.DEFAULTNAME);
      try {
         act.getThreadCountMonitor().setStatus(MonitorStatus.ON);
         act.getThreadCountMonitor().setShedThreshold(-1);

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
         log.info("threads joined: " + current);
         // soll max: 44
         Assert.assertTrue(current > 20);

      } finally {
         current = 0;
         act.getThreadCountMonitor().setStatus(MonitorStatus.OFF);
      }
   }

   @Test
   public void alarm() throws Exception {
      log.debug("start ThreadCountMonitorTest alarm()");
      LoadControlActuator act = (LoadControlActuator) Configuration.instance()
            .getActuator(LoadControlActuator.DEFAULTNAME);
      try {
         act.getThreadCountMonitor().setStatus(MonitorStatus.ON);
         act.getThreadCountMonitor().setAlarmThreshold(4);
         act.getThreadCountMonitor().setShedThreshold(-1);
         act.setLoadControlCallback(new TAlarmExecution());

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
         log.info("threads joined: " + current);

         Assert.assertTrue(current > 10);
         log.info("counter=" + counter.get());
         Assert.assertTrue(counter.get() > 0);
         // Assert.assertTrue(counter.get() < 3);

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
         Assert.assertTrue(counter.get() >= 2);

      } finally {
         current = 0;
         act.getThreadCountMonitor().setAlarmThreshold(-1);
         act.getThreadCountMonitor().setStatus(MonitorStatus.OFF);
         counter.set(0);
      }
   }

   @Test
   public void valve() throws Exception {
      log.debug("start ThreadCountMonitorTest valve()");
      LoadControlActuator act = (LoadControlActuator) Configuration.instance()
            .getActuator(LoadControlActuator.DEFAULTNAME);
      try {
         act.getThreadCountMonitor().setStatus(MonitorStatus.ON);
         act.getThreadCountMonitor().setValveThreshold(4);
         act.getThreadCountMonitor().setShedThreshold(-1);
         act.getThreadCountMonitor().setThrottleMaxTime(3000);
         act.setLoadControlCallback(new TAlarmExecution());

         int nbr = 10;
         List<ThreadExecution> tlist = new ArrayList<ThreadExecution>();
         for (int i = 0; i < nbr; i++) {
            ThreadExecution t = new ThreadExecution();
            tlist.add(t);
         }

         log.info("start threads");
         for (ThreadExecution te : tlist) {
            te.start();
            Thread.sleep(70);
         }
         log.info("join threads");
         for (ThreadExecution te : tlist) {
            // te.join();
         }
         Thread.sleep(5000);
         log.info("threads joined: " + current);

         log.info("throttled=" + counter.get());
         log.info("shed=" + shed.get());
         log.info("throttle queue count=" + throttled.get());
         Assert.assertTrue(counter.get() >= 1);
         Assert.assertTrue(throttled.get() >= 1);
      } finally {
         current = 0;
         act.getThreadCountMonitor().setValveThreshold(-1);
         act.getThreadCountMonitor().setStatus(MonitorStatus.OFF);
         counter.set(0);
         shed.set(0);
         throttled.set(0);
      }
   }

   @Test
   public void shed() throws Exception {
      log.debug("start ThreadCountMonitorTest shed()");
      LoadControlActuator act = (LoadControlActuator) Configuration.instance()
            .getActuator(LoadControlActuator.DEFAULTNAME);
      try {
         act.getThreadCountMonitor().setStatus(MonitorStatus.ON);
         act.getThreadCountMonitor().setShedThreshold(6);
         act.setLoadControlCallback(new TAlarmExecution());

         int nbr = 15;
         List<ThreadExecution> tlist = new ArrayList<ThreadExecution>();
         for (int i = 0; i < nbr; i++) {
            ThreadExecution t = new ThreadExecution();
            tlist.add(t);
         }

         log.info("start threads");
         for (ThreadExecution te : tlist) {
            te.start();
            // Thread.sleep(50);
         }
         log.info("join threads");
         for (ThreadExecution te : tlist) {
            te.join();
         }
         log.info("threads joined");

         Assert.assertTrue(current > 10);
         log.info("throttled=" + counter.get());
         log.info("shed=" + shed.get());

         Assert.assertEquals(0, counter.get());
         Assert.assertTrue(shed.get() > 3);

      } finally {
         act.getThreadCountMonitor().setShedThreshold(-1);
         act.getThreadCountMonitor().setStatus(MonitorStatus.OFF);
         counter.set(0);
         shed.set(0);
      }
   }

}
