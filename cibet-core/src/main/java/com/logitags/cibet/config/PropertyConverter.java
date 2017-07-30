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
package com.logitags.cibet.config;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.beanutils.Converter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.loadcontrol.LoadControlCallback;
import com.logitags.cibet.actuator.loadcontrol.Monitor;
import com.logitags.cibet.actuator.loadcontrol.MonitorStatus;
import com.logitags.cibet.actuator.owner.OwnerCheckCallback;
import com.logitags.cibet.actuator.scheduler.SchedulerTaskInterceptor;

public class PropertyConverter implements Converter {

   private static Log log = LogFactory.getLog(PropertyConverter.class);

   private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

   @Override
   public Object convert(Class type, Object value) {
      if (type == Collection.class) {
         // Shiro.hasAllRoles
         Collection<String> coll = new ArrayList<String>();
         if (value == null) return coll;
         String har = (String) value;
         StringTokenizer tokenizer = new StringTokenizer(har, ",;");
         while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken().trim();
            log.debug("add property " + token);
            coll.add(token);
         }
         return coll;

      } else if (type == String[].class) {
         // isPermittedAll
         if (value == null) return value;
         String har = (String) value;
         List<String> list = new ArrayList<String>();
         StringTokenizer tokenizer = new StringTokenizer(har, ";");
         while (tokenizer.hasMoreTokens()) {
            String perm = tokenizer.nextToken().trim();
            log.debug("add permision " + perm);
            list.add(perm);
         }
         String[] arr = list.toArray(new String[0]);
         return arr;

      } else if (type == Boolean.class) {
         if (value == null) {
            return Boolean.TRUE;
         } else {
            return Boolean.valueOf((String) value);
         }

      } else if (type == boolean.class) {
         if (value == null) {
            return Boolean.TRUE.booleanValue();
         } else {
            return Boolean.valueOf((String) value).booleanValue();
         }

      } else if (type == Date.class) {
         String strValue = (String) value;
         if (value == null) {
            return null;
         } else if (strValue.trim().startsWith("+")) {
            int sec = Integer.parseInt(strValue.trim().substring(1).trim());
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.SECOND, sec);
            return cal.getTime();
         } else {
            try {
               return dateFormat.parse(strValue);
            } catch (ParseException e) {
               log.error(e.getMessage(), e);
               throw new RuntimeException(value + " has wrong format. Should be yyyy.MM.dd HH:mm:ss");
            }
         }

      } else if (type == MonitorStatus.class) {
         String strValue = (String) value;
         if (strValue.equalsIgnoreCase(MonitorStatus.ON.name())) {
            return MonitorStatus.ON;
         } else if (strValue.equalsIgnoreCase(MonitorStatus.OFF.name())) {
            return MonitorStatus.OFF;
         } else {
            throw new RuntimeException(value + " MonitorMode must be " + MonitorStatus.OFF + " or " + MonitorStatus.ON);
         }

      } else if (type == Class.class) {
         if (value == null) return value;
         try {
            return Class.forName((String) value);
         } catch (ClassNotFoundException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
         }

      } else if (SchedulerTaskInterceptor.class.isAssignableFrom(type)) {
         if (value == null) throw new RuntimeException(
               "<NULL> not allowed for property to set a SchedulerTaskInterceptor implementation class name");
         try {
            Class<SchedulerTaskInterceptor> clazz = (Class<SchedulerTaskInterceptor>) Class.forName((String) value);
            return clazz.newInstance();
         } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
         }

      } else if (LoadControlCallback.class.isAssignableFrom(type)) {
         if (value == null) throw new RuntimeException(
               "<NULL> not allowed for property to set a LoadControlCallback implementation class name");
         try {
            Class<LoadControlCallback> clazz = (Class<LoadControlCallback>) Class.forName((String) value);
            return clazz.newInstance();
         } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
         }

      } else if (OwnerCheckCallback.class.isAssignableFrom(type)) {
         if (value == null) throw new RuntimeException(
               "<NULL> not allowed for property to set a OwnerCheckCallback implementation class name");
         try {
            Class<OwnerCheckCallback> clazz = (Class<OwnerCheckCallback>) Class.forName((String) value);
            return clazz.newInstance();
         } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
         }

      } else if (type == Monitor[].class) {
         String har = (String) value;
         List<Monitor> list = new ArrayList<Monitor>();
         StringTokenizer tokenizer = new StringTokenizer(har, ",;");
         while (tokenizer.hasMoreTokens()) {
            String mon = tokenizer.nextToken().trim();
            try {
               Class<Monitor> clazz = (Class<Monitor>) Class.forName(mon);
               list.add(clazz.newInstance());
            } catch (Exception e) {
               log.error(e.getMessage(), e);
               throw new RuntimeException(e);
            }
         }
         Monitor[] arr = list.toArray(new Monitor[0]);
         return arr;

      } else {
         throw new RuntimeException("No converter algorithm implemented for class " + type);
      }
   }

}
