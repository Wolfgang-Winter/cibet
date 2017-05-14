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
package com.logitags.jmeter;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;

public class CibetJMeterShedLoad implements JavaSamplerClient {

   private static Log log = LogFactory.getLog(CibetJMeterShedLoad.class);

   protected int threshhold;

   protected int sleepWhenUnsuccesful;

   protected String testcase;
   protected String param2;

   protected int loopCount;

   protected String name;

   @Override
   public Arguments getDefaultParameters() {
      Arguments params = new Arguments();
      params.addArgument("loopCount", "4000000");
      params.addArgument("name", "");
      params.addArgument("sleepWhenUnsuccesful", "3000");
      params.addArgument("testcase", "calculationTest");
      params.addArgument("param2", "");
      return params;
   }

   @Override
   public void setupTest(JavaSamplerContext ctx) {
      loopCount = ctx.getIntParameter("loopCount");
      name = ctx.getParameter("name");
      sleepWhenUnsuccesful = ctx.getIntParameter("sleepWhenUnsuccesful");
      testcase = ctx.getParameter("testcase");
      param2 = ctx.getParameter("param2");
      System.out.println("---- setup");
   }

   @Override
   public SampleResult runTest(JavaSamplerContext ctx) {
      System.out.println(Thread.currentThread() + " starts " + name + " (" + loopCount + ")");
      System.out.println(System.getProperty("java.io.tmpdir"));

      SampleResult result = new SampleResult();
      result.setSamplerData(name + " (" + loopCount + ")");
      result.setSampleLabel(name);

      result.sampleStart();
      MonitorTestClass heavy = new MonitorTestClass();
      try {
         Method method = heavy.getClass().getMethod(testcase, int.class, String.class);
         method.invoke(heavy, loopCount, param2);

         EventResult er = Context.requestScope().getExecutedEventResult();
         if (er == null) {
            System.out.println("No EventResult");
            result.setSuccessful(true);
         } else if (er.getExecutionStatus() == ExecutionStatus.EXECUTED) {
            result.setSuccessful(true);
         } else {
            System.out.println("EventResult " + er.getExecutionStatus());
            result.setSuccessful(false);
         }
      } catch (Exception e) {
         System.out.println(e.getMessage());
         StringWriter w = new StringWriter();
         PrintWriter printer = new PrintWriter(w);
         e.printStackTrace(printer);
         System.out.println(w.toString());
         result.setSuccessful(false);
         result.setResponseMessage(e.toString());
      } finally {
         result.sampleEnd();
      }

      if (!result.isSuccessful()) {
         try {
            Thread.sleep(sleepWhenUnsuccesful);
         } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
         }
      }

      System.out.println(Thread.currentThread() + " ends " + name + " (" + loopCount + ")");
      return result;
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * com.logitags.cibet.jmeter.JMeterShedLoad#teardownTest(org.apache.jmeter.protocol.java.sampler.JavaSamplerContext)
    */
   @Override
   public void teardownTest(JavaSamplerContext arg0) {
      File outDir = new File("D:\\Java\\apache-jmeter-2.13\\log2");
      for (File file : outDir.listFiles()) {
         file.delete();
      }
   }

}
