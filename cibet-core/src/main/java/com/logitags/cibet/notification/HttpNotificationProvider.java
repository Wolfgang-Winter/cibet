/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2012 Dr. Wolfgang Winter
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
package com.logitags.cibet.notification;

import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.core.ExecutionStatus;

/**
 * Implementation that sends a http POST request to an address. The properties of Controllable are sent as form
 * parameters in the body of the request.
 * 
 * @author Wolfgang
 * 
 */
public class HttpNotificationProvider implements NotificationProvider, Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 5919378610703485394L;

   /**
    * logger for tracing
    */
   private static Log log = LogFactory.getLog(HttpNotificationProvider.class);

   @Override
   public void notify(ExecutionStatus status, Controllable c) {
      List<NameValuePair> formparams = createNameValuePairs(c.getExecutionStatus(), c);

      String address = null;
      switch (c.getExecutionStatus()) {
      case POSTPONED:
         address = c.getApprovalAddress();
         break;
      case FIRST_POSTPONED:
         address = c.getFirstApprovalAddress();
         break;
      case FIRST_RELEASED:
      case REJECTED:
      case EXECUTED:
         address = c.getCreateAddress();
         break;
      default:
         log.info("no notification for event " + c.getExecutionStatus());
         return;
      }

      HttpResponse response = null;
      try {
         HttpClient client = HttpClientBuilder.create().build();
         UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
         HttpPost postMethod = new HttpPost(address);
         postMethod.setEntity(entity);
         response = client.execute(postMethod);
         if (log.isDebugEnabled()) {
            log.debug(response);
         }
      } catch (IOException e) {
         log.error("Failed to send " + c.getExecutionStatus() + " notification to " + address + ": " + e.getMessage(),
               e);
      }

      if (response == null) {
         log.error("Failed to send " + c.getExecutionStatus() + " notification to " + address + ": Response is NULL");
      } else if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
         log.error("Failed to send " + c.getExecutionStatus() + " notification to " + address
               + ": http response code is " + response.getStatusLine().getStatusCode());
      }
   }

   private List<NameValuePair> createNameValuePairs(ExecutionStatus type, Controllable c) {
      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

      List<NameValuePair> formparams = new ArrayList<NameValuePair>();
      formparams.add(new BasicNameValuePair("controllableId", String.valueOf(c.getControllableId())));
      formparams.add(new BasicNameValuePair("firstApprovalUser", c.getFirstApprovalUser()));
      if (c.getFirstApprovalDate() != null) {
         formparams.add(new BasicNameValuePair("firstApprovalDate", dateFormat.format(c.getFirstApprovalDate())));
      }
      formparams.add(new BasicNameValuePair("firstApprovalAddress", c.getFirstApprovalAddress()));
      formparams.add(new BasicNameValuePair("firstApprovalRemark", c.getFirstApprovalRemark()));

      formparams.add(new BasicNameValuePair("approvalUser", c.getApprovalUser()));
      formparams.add(new BasicNameValuePair("approvalAddress", c.getApprovalAddress()));
      if (c.getApprovalDate() != null) {
         formparams.add(new BasicNameValuePair("approvalDate", dateFormat.format(c.getApprovalDate())));
      }
      formparams.add(new BasicNameValuePair("approvalRemark", c.getApprovalRemark()));

      formparams.add(new BasicNameValuePair("actuator", c.getActuator()));
      formparams.add(new BasicNameValuePair("controlEvent", c.getControlEvent().name()));
      formparams.add(new BasicNameValuePair("createUser", c.getCreateUser()));
      if (c.getCreateDate() != null) {
         formparams.add(new BasicNameValuePair("createDate", dateFormat.format(c.getCreateDate())));
      }
      formparams.add(new BasicNameValuePair("createRemark", c.getCreateRemark()));

      formparams.add(new BasicNameValuePair("createAddress", c.getCreateAddress()));
      formparams.add(new BasicNameValuePair("tenant", c.getTenant()));
      formparams.add(new BasicNameValuePair("caseId", c.getCaseId()));

      for (Entry<String, Object> entry : c.getResource().getNotificationAttributes().entrySet()) {
         formparams.add(
               new BasicNameValuePair(entry.getKey(), entry.getValue() == null ? null : entry.getValue().toString()));
      }

      formparams.add(new BasicNameValuePair("notificationType", type.name()));
      return formparams;
   }

}
