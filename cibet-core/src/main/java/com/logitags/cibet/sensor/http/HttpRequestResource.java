/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2016 Dr. Wolfgang Winter
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import javax.script.ScriptEngine;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.dc.ResourceApplyException;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.resource.ParameterSequenceComparator;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.common.Invoker;

@Entity
@DiscriminatorValue(value = "HttpResource")
public class HttpRequestResource extends Resource {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private static Log log = LogFactory.getLog(HttpRequestResource.class);

   /**
    * method name which is controlled
    */
   private String method;

   /**
    * HTTP request data in case of http HTTP-FILTER requests, otherwise null.
    */
   @Transient
   private HttpRequestData httpRequestData;

   /**
    * HTTP request in case of http HTTP-FILTER requests, otherwise null.
    */
   @Transient
   private transient HttpServletRequest httpRequest;

   /**
    * HTTP response in case of http HTTP-FILTER requests, otherwise null.
    */
   @Transient
   private transient HttpServletResponse httpResponse;

   /**
    * The invoker history, from where the current control event was called. In case of a http request this is the IP
    * address from where the request was sent including proxy IPs. In case of a method invocation or similar, this is
    * the command stacktrace
    */
   @Transient
   private String invoker;

   public HttpRequestResource() {
   }

   /**
    * constructor used for http proxy resources
    * 
    * @param url
    * @param meth
    * @param r
    */
   public HttpRequestResource(String url, String meth, HttpRequestData r) {
      setTarget(url);
      method = meth;
      httpRequestData = r;
   }

   /**
    * constructor used for http ServletFilter resources
    * 
    * @param url
    * @param meth
    * @param request
    * @param response
    */
   public HttpRequestResource(String url, String meth, HttpServletRequest request, HttpServletResponse response) {
      setTarget(url);
      method = meth;
      httpRequest = request;
      httpResponse = response;
   }

   /**
    * copy constructor
    * 
    * @param copy
    */
   public HttpRequestResource(HttpRequestResource copy) {
      super(copy);
      setInvoker(copy.invoker);
   }

   @Override
   public String createUniqueId() {
      Base64 b64 = new Base64();
      StringBuffer b = new StringBuffer();
      b.append(getTarget());
      b.append(getMethod());

      for (ResourceParameter param : getParameters()) {
         if (param.getParameterType() == ParameterType.HTTP_PARAMETER
               || param.getParameterType() == ParameterType.HTTP_BODY) {
            b.append(b64.encodeToString(param.getEncodedValue()));
         }
      }
      return DigestUtils.sha256Hex(b.toString());
   }

   @Override
   public void fillContext(ScriptEngine engine) {
      engine.put("$TARGET", getTarget());

      Map<String, Object> httpHeaders = new HashMap<String, Object>();
      Map<String, Object> httpAttributes = new HashMap<String, Object>();
      Map<String, Object> httpparams = new HashMap<String, Object>();
      for (ResourceParameter param : getParameters()) {
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
      map.put("target", getTarget());
      map.put("method", getMethod());
      return map;
   }

   @Override
   public Object apply(ControlEvent event) throws ResourceApplyException {
      try {
         Set<ResourceParameter> paramList = new TreeSet<ResourceParameter>(new ParameterSequenceComparator());
         paramList.addAll(getParameters());
         Invoker invoker = HttpRequestInvoker.createInstance();
         return invoker.execute(null, getTarget(), getMethod(), paramList);
      } catch (Exception e) {
         log.error(e.getMessage());
         throw new ResourceApplyException("Release of Method Invocation failed:\n" + toString(), e);
      }
   }

   /**
    * method name which is controlled
    * 
    * @return the method
    */
   public String getMethod() {
      return method;
   }

   /**
    * method name which is controlled
    * 
    * @param method
    *           the method to set
    */
   public void setMethod(String method) {
      this.method = method;
   }

   public HttpServletRequest getHttpRequest() {
      return httpRequest;
   }

   public HttpServletResponse getHttpResponse() {
      return httpResponse;
   }

   /**
    * HTTP request data in case of http HTTP-FILTER requests, otherwise null.
    * 
    * @return the httpRequest
    */
   public HttpRequestData getHttpRequestData() {
      return httpRequestData;
   }

   /**
    * The invoker history, from where the current control event was called. In case of a http request this is the IP
    * address from where the request was sent including proxy IPs.
    * 
    * @param ipInvoker
    *           the ipInvoker to set
    */
   public void setInvoker(String ipInvoker) {
      this.invoker = ipInvoker;
   }

   /**
    * The invoker history, from where the current control event was called. In case of a http request this is the IP
    * address from where the request was sent including proxy IPs.
    * 
    * @return the ipInvoker
    */
   public String getInvoker() {
      return invoker;
   }

   @Override
   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("[HttpRequestResource] ");
      b.append(super.toString());
      b.append(" ; method: ");
      b.append(getMethod());
      return b.toString();
   }

}
