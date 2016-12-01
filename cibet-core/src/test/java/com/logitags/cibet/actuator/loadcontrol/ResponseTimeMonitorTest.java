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

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cibethelper.loadcontrol.MonitorTestClass;
import com.cibethelper.loadcontrol.TAlarmExecution;
import com.logitags.cibet.config.Configuration;

public class ResponseTimeMonitorTest {

   private static Logger log = Logger.getLogger(ResponseTimeMonitorTest.class);

   private static final String JMX_OBJECTNAME_PREFIX = Configuration.JMX_BASE + ":type=LoadControlActuator,app="
         + Configuration.getApplicationName() + ",name=";

   private static final String SP = "com.logitags.cibet.actuator.loadcontrol.x";

   public static AtomicInteger counter = new AtomicInteger(0);
   public static AtomicInteger thresholdExceed = new AtomicInteger(0);
   public static AtomicInteger shedCounter = new AtomicInteger(0);

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
      log.debug("start monitor()");
      LoadControlActuator act = (LoadControlActuator) Configuration.instance()
            .getActuator(LoadControlActuator.DEFAULTNAME);
      try {
         act.getResponseTimeMonitor().setStatus(MonitorStatus.ON);

         MonitorTestClass mon = new MonitorTestClass();
         mon.cibetCalc(100000, null);
         long responsetime = act.getResponseTimeMonitor().getCurrentResponseTime(SP);
         log.debug("responsetime =" + responsetime);
         Assert.assertTrue(responsetime > 0);

         MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
         String nam = "com.logitags.cibet:app=,name=LOADCONTROL,setpoint=" + SP + ",type=LoadControlActuator";
         ObjectName oname = new ObjectName(nam);
         Assert.assertTrue(mbs.isRegistered(oname));

      } finally {
         act.getResponseTimeMonitor().setStatus(MonitorStatus.OFF);
      }
   }

   @Test
   public void alarm() throws Exception {
      log.debug("start alarm()");
      LoadControlActuator act = (LoadControlActuator) Configuration.instance()
            .getActuator(LoadControlActuator.DEFAULTNAME);
      try {
         act.getResponseTimeMonitor().reset(SP);
         act.getResponseTimeMonitor().setStatus(MonitorStatus.ON);
         act.getResponseTimeMonitor().setAlarmThreshold("100");
         act.setLoadControlCallback(new TAlarmExecution());

         MonitorTestClass mon = new MonitorTestClass();

         for (int i = 1; i < 19; i++) {
            mon.cibetCalc(100000, null);
         }
         Assert.assertEquals(0, counter.get());
         long responsetime = act.getResponseTimeMonitor().getCurrentResponseTime(SP);
         log.debug("responsetime =" + responsetime);
         Assert.assertTrue(responsetime > 0);

         for (int i = 1; i < 5; i++) {
            mon.cibetCalc(500000, null);
         }
         Assert.assertEquals(3, counter.get());
         Assert.assertEquals(1, thresholdExceed.get());

      } finally {
         counter = new AtomicInteger(0);
         thresholdExceed = new AtomicInteger(0);
         act.getResponseTimeMonitor().setAlarmThreshold("-1");
         act.getResponseTimeMonitor().setStatus(MonitorStatus.OFF);
         act.getResponseTimeMonitor().reset(SP);
      }
   }

   @Test
   public void alarmPercent() throws Exception {
      log.debug("start alarmPercent()");
      LoadControlActuator act = (LoadControlActuator) Configuration.instance()
            .getActuator(LoadControlActuator.DEFAULTNAME);
      try {
         act.getResponseTimeMonitor().reset(SP);
         act.getResponseTimeMonitor().setStatus(MonitorStatus.ON);
         act.getResponseTimeMonitor().setAlarmThreshold("50%");
         act.setLoadControlCallback(new TAlarmExecution());

         MonitorTestClass mon = new MonitorTestClass();

         for (int i = 1; i < 19; i++) {
            mon.cibetCalc(100000, null);
         }
         Assert.assertEquals(0, counter.get());
         long responsetime = act.getResponseTimeMonitor().getCurrentResponseTime(SP);
         log.debug("responsetime =" + responsetime);
         Assert.assertTrue(responsetime > 0);

         for (int i = 1; i < 5; i++) {
            mon.cibetCalc(100000, null);
         }
         Assert.assertEquals(3, counter.get());
         Assert.assertEquals(1, thresholdExceed.get());
      } finally {
         counter = new AtomicInteger(0);
         thresholdExceed = new AtomicInteger(0);
         act.getResponseTimeMonitor().setAlarmThreshold("-1");
         act.getResponseTimeMonitor().setStatus(MonitorStatus.OFF);
         act.getResponseTimeMonitor().reset(SP);
      }
   }

   @Test
   public void shed() throws Exception {
      log.debug("start shed()");
      LoadControlActuator act = (LoadControlActuator) Configuration.instance()
            .getActuator(LoadControlActuator.DEFAULTNAME);
      try {
         act.getResponseTimeMonitor().reset(SP);
         act.getResponseTimeMonitor().setStatus(MonitorStatus.ON);
         act.getResponseTimeMonitor().setShedThreshold("100");
         act.getResponseTimeMonitor().setShedTime("100%");
         act.setLoadControlCallback(new TAlarmExecution());

         MonitorTestClass mon = new MonitorTestClass();

         for (int i = 1; i < 20; i++) {
            mon.cibetCalc(100000, null);
            log.debug("responsetime =" + act.getResponseTimeMonitor().getCurrentResponseTime(SP));
         }
         Assert.assertEquals(0, shedCounter.get());
         long responsetime = act.getResponseTimeMonitor().getCurrentResponseTime(SP);
         log.debug("responsetime =" + responsetime);
         Assert.assertTrue(responsetime > 0);
         log.debug("average responsetime =" + act.getResponseTimeMonitor().getAverageResponseTime(SP));

         for (int i = 1; i < 5; i++) {
            mon.cibetCalc(500000, null);
            log.debug("responsetime =" + act.getResponseTimeMonitor().getCurrentResponseTime(SP));
         }
         Assert.assertEquals(3, shedCounter.get());
      } finally {
         shedCounter = new AtomicInteger(0);
         act.getResponseTimeMonitor().setShedThreshold("-1");
         act.getResponseTimeMonitor().setShedTime("1000");
         act.getResponseTimeMonitor().setStatus(MonitorStatus.OFF);
         act.getResponseTimeMonitor().reset(SP);
      }
   }

   @Test
   public void shedPercent() throws Exception {
      log.debug("start shedPercent()");
      LoadControlActuator act = (LoadControlActuator) Configuration.instance()
            .getActuator(LoadControlActuator.DEFAULTNAME);
      try {
         act.getResponseTimeMonitor().reset(SP);
         act.getResponseTimeMonitor().setStatus(MonitorStatus.ON);
         act.getResponseTimeMonitor().setShedThreshold("50%");
         act.setLoadControlCallback(new TAlarmExecution());

         MonitorTestClass mon = new MonitorTestClass();

         for (int i = 1; i < 20; i++) {
            mon.cibetCalc(100000, null);
            log.debug("responsetime =" + act.getResponseTimeMonitor().getCurrentResponseTime(SP));
         }
         Assert.assertEquals(0, shedCounter.get());
         long responsetime = act.getResponseTimeMonitor().getCurrentResponseTime(SP);
         log.debug("responsetime =" + responsetime);
         Assert.assertTrue(responsetime > 0);
         log.debug("average responsetime =" + act.getResponseTimeMonitor().getAverageResponseTime(SP));

         for (int i = 1; i < 5; i++) {
            mon.cibetCalc(100000, null);
            log.debug("responsetime =" + act.getResponseTimeMonitor().getCurrentResponseTime(SP));
         }
         Assert.assertEquals(3, shedCounter.get());

      } finally {
         shedCounter = new AtomicInteger(0);
         act.getResponseTimeMonitor().setShedThreshold("-1");
         act.getResponseTimeMonitor().setStatus(MonitorStatus.OFF);
         act.getResponseTimeMonitor().reset(SP);
      }
   }

}
