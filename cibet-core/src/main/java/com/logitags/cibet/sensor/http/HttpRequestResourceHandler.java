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
package com.logitags.cibet.sensor.http;

import java.io.Serializable;
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
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.resource.ParameterNameComparator;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.resource.ResourceHandler;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.common.Invoker;

/**
 * This resource represents a http request. Target type is a URL.
 * 
 * @author Wolfgang
 * 
 */
public class HttpRequestResourceHandler implements Serializable, ResourceHandler {

   /**
    * 
    */
   private static final long serialVersionUID = -5065094824323159336L;

   private static Log log = LogFactory.getLog(HttpRequestResourceHandler.class);

   protected Resource resource;

   public HttpRequestResourceHandler(Resource res) {
      resource = res;
   }

   @Override
   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("[HttpRequestResource] targetType: ");
      b.append(resource.getTargetType());
      b.append(" ; method: ");
      b.append(resource.getMethod());
      return b.toString();
   }

   @Override
   public void fillContext(ScriptEngine engine) {
      engine.put("$TARGETTYPE", resource.getTargetType());

      Map<String, Object> httpHeaders = new HashMap<String, Object>();
      Map<String, Object> httpAttributes = new HashMap<String, Object>();
      Map<String, Object> httpparams = new HashMap<String, Object>();
      for (ResourceParameter param : resource.getParameters()) {
         if (param.getParameterType() == ParameterType.HTTP_ATTRIBUTE) {
            httpAttributes.put(param.getName(), param.getUnencodedValue());
         } else if (param.getParameterType() == ParameterType.HTTP_HEADER) {
            httpHeaders.put(param.getName(), param.getUnencodedValue());
         } else if (param.getParameterType() == ParameterType.HTTP_PARAMETER) {
            httpparams.put(param.getName(), param.getUnencodedValue());
         }
      }
      engine.put("$HTTPATTRIBUTES", httpAttributes);
      engine.put("$HTTPHEADERS", httpHeaders);
      engine.put("$HTTPPARAMETERS", httpparams);
   }

   @Override
   public Map<String, Object> getNotificationAttributes() {
      Map<String, Object> map = new HashMap<>();
      map.put("targetType", resource.getTargetType());
      map.put("method", resource.getMethod());
      return map;
   }

   @Override
   public Object apply(ControlEvent event) throws ResourceApplyException {
      try {
         List<ResourceParameter> paramList = new LinkedList<ResourceParameter>(resource.getParameters());
         Invoker invoker = HttpRequestInvoker.createInstance();
         return invoker.execute(null, resource.getTargetType(), resource.getMethod(), paramList);
      } catch (Exception e) {
         log.error(e.getMessage());
         throw new ResourceApplyException("Release of Method Invocation failed:\n" + toString(), e);
      }
   }

   @Override
   public String createUniqueId() {
      Base64 b64 = new Base64();
      StringBuffer b = new StringBuffer();
      b.append(resource.getTargetType());
      b.append(resource.getMethod());

      ParameterNameComparator comparator = new ParameterNameComparator();
      Collections.sort(resource.getParameters(), comparator);
      for (ResourceParameter param : resource.getParameters()) {
         if (param.getParameterType() == ParameterType.HTTP_PARAMETER
               || param.getParameterType() == ParameterType.HTTP_BODY) {
            b.append(b64.encodeToString(param.getEncodedValue()));
         }
      }
      return DigestUtils.sha256Hex(b.toString());
   }

}
