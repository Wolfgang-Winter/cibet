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
package com.logitags.cibet.sensor.ejb;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.dc.ResourceApplyException;
import com.logitags.cibet.core.CibetException;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.resource.ParameterSequenceComparator;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.resource.ResourceHandler;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.common.Invoker;

/**
 * This resource represents a method invocation. Target type is the class name.
 * 
 * @author Wolfgang
 * 
 */
public class EjbResourceHandler implements Serializable, ResourceHandler {

   /**
    * 
    */
   private static final long serialVersionUID = -5142727867428012361L;

   private static Log log = LogFactory.getLog(EjbResourceHandler.class);

   protected Resource resource;

   public EjbResourceHandler(Resource res) {
      resource = res;
   }

   @Override
   public void fillContext(ScriptEngine engine) {
      engine.put("$TARGETTYPE", resource.getTargetType());
      engine.put("$TARGET", resource.getObject());
      for (ResourceParameter param : resource.getParameters()) {
         if (param.getParameterType() == ParameterType.METHOD_PARAMETER) {
            engine.put("$" + param.getName(), param.getUnencodedValue());
         }
      }
   }

   @Override
   public Map<String, Object> getNotificationAttributes() {
      Map<String, Object> map = new HashMap<>();
      map.put("targetType", resource.getTargetType());
      map.put("method", resource.getMethod());
      map.put("resultObject", resource.getResultObject());
      return map;
   }

   @Override
   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("[EjbResource] targetType: ");
      b.append(resource.getTargetType());
      b.append(" ; method: ");
      b.append(resource.getMethod());
      return b.toString();
   }

   @Override
   public Object apply(ControlEvent event) throws ResourceApplyException {
      try {
         List<ResourceParameter> paramList = new LinkedList<ResourceParameter>(resource.getParameters());
         Class<? extends Invoker> facClass = (Class<? extends Invoker>) Class.forName(resource.getInvokerClass());
         Method createMethod = facClass.getMethod("createInstance");
         Invoker fac = (Invoker) createMethod.invoke(null);
         return fac.execute(resource.getInvokerParam(), resource.getTargetType(), resource.getMethod(), paramList);

      } catch (CibetException e) {
         throw e;
      } catch (InvocationTargetException e) {
         Throwable cause = e.getCause();
         log.debug("cause=" + cause);
         while (cause != null) {
            if (cause instanceof CibetException) {
               throw (CibetException) cause;
            }
            cause = cause.getCause();
         }
         // check EJBException cause but do not refer to EJBException
         cause = e.getCause();
         if (cause != null) {
            Throwable causedBy = null;
            try {
               Method m = cause.getClass().getMethod("getCausedByException");
               causedBy = (Throwable) m.invoke(cause);
            } catch (Exception e1) {
               log.debug("method getCausedByException() does not exist: " + e1.getMessage());
            }
            if (causedBy != null && causedBy instanceof CibetException) {
               throw (CibetException) causedBy;
            }
         }
         log.error(e.getMessage());
         throw new ResourceApplyException("Apply of Method Invocation failed:\n" + toString(), e);
      } catch (Exception e) {
         log.error(e.getMessage());
         throw new ResourceApplyException("Apply of Method Invocation failed:\n" + toString(), e);
      }
   }

   @Override
   public String createUniqueId() {
      Base64 b64 = new Base64();
      StringBuffer b = new StringBuffer();
      b.append(resource.getTargetType());
      b.append(resource.getMethod());

      ParameterSequenceComparator comparator = new ParameterSequenceComparator();
      Collections.sort(resource.getParameters(), comparator);
      for (ResourceParameter param : resource.getParameters()) {
         b.append(b64.encodeToString(param.getEncodedValue()));
      }
      return DigestUtils.sha256Hex(b.toString());
   }

}
