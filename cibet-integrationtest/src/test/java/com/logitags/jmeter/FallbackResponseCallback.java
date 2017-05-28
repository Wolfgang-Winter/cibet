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

import java.io.IOException;
import java.io.PrintWriter;

import com.logitags.cibet.actuator.loadcontrol.LoadControlCallback;
import com.logitags.cibet.actuator.loadcontrol.LoadControlData;
import com.logitags.cibet.sensor.http.HttpRequestResource;

public class FallbackResponseCallback implements LoadControlCallback {

   @Override
   public void onShed(LoadControlData lcData) {
      StringBuffer b = new StringBuffer();
      b.append("<html><head>");
      b.append("</head><body></p></p><h1>Sorry! The system is currently under heavy load. Execution of test ");
      b.append(((HttpRequestResource) lcData.getResource()).getHttpRequest().getParameter("test"));
      b.append(" with loop count = ");
      b.append(((HttpRequestResource) lcData.getResource()).getHttpRequest().getParameter("loops"));
      b.append(" could not be executed<br><br>");
      b.append("Please try again later");
      b.append("</h1></body></html>");

      try {
         PrintWriter writer = ((HttpRequestResource) lcData.getResource()).getHttpResponse().getWriter();
         writer.print(b.toString());
         writer.close();
      } catch (IOException e) {
         throw new RuntimeException(e);
      }

   }

   @Override
   public void onAlarm(LoadControlData loadControlData) {
      // TODO Auto-generated method stub

   }

   @Override
   public void onThrottled(LoadControlData loadControlData) {
      // TODO Auto-generated method stub

   }

   @Override
   public void onAccepted(LoadControlData loadControlData) {
      // TODO Auto-generated method stub

   }

}
