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
package com.logitags.cibet.sensor.http;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NamingException;
import javax.persistence.EntityManager;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.impl.ProxyUtils;

import com.logitags.cibet.actuator.common.Actuator;
import com.logitags.cibet.authentication.SecurityContext;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.ProxyConfig;
import com.logitags.cibet.config.ProxyConfig.ProxyMode;
import com.logitags.cibet.context.CibetEEContext;
import com.logitags.cibet.context.CibetEEContextEJB;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.EntityManagerType;
import com.logitags.cibet.context.InternalRequestScope;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.jndi.EjbLookup;
import com.logitags.cibet.resource.ParameterType;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpHeaders.Values;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

public class CibetProxy extends HttpFiltersAdapter {

   private static Log log = LogFactory.getLog(CibetProxy.class);

   public static final String SENSOR_NAME = "HTTP-PROXY";

   private ProxyConfig proxyConfig;

   private EventMetadata metadata;

   private EventResult eventResult;

   private Map<String, Object> requestContext = new HashMap<>();
   private Map<String, Object> sessionContext = new HashMap<>();

   private boolean skip = false;
   private boolean newEM = false;

   public CibetProxy(HttpRequest originalRequest, ChannelHandlerContext ctx, ProxyConfig config) {
      super(originalRequest, ctx);
      proxyConfig = config;
      log.debug("CibetProxy constructor");
   }

   public CibetProxy(HttpRequest originalRequest) {
      super(originalRequest);
      log.debug("CibetProxy constructor");
   }

   private void log(Object httpObject, String origin) {
      if (log.isDebugEnabled()) {

         StringBuffer msg = new StringBuffer("*****" + origin);
         if (httpObject != null) {
            if (httpObject instanceof FullHttpRequest) {
               FullHttpRequest full = (FullHttpRequest) httpObject;
               msg = msg.append("\n" + full.getMethod() + " " + full.getUri() + " " + full.getProtocolVersion());
               for (Entry<String, String> header : full.headers().entries()) {
                  msg.append("\n" + header.getKey() + "=" + header.getValue());
               }

               msg.append("\n" + "content: " + content(full.content()));

            } else {
               msg.append("\n" + httpObject.getClass() + ": " + httpObject);
            }

         }
         msg = msg.append("\n*****");
         log.debug(msg.toString());
      }
   }

   private String content(ByteBuf buffer) {
      ByteBuf dup = buffer.duplicate();
      int readableBytes = dup.readableBytes();
      log.debug(readableBytes + " readable Bytes");
      byte[] bytes = new byte[readableBytes];
      int counter = 0;
      while (dup.isReadable()) {
         bytes[counter] = dup.readByte();
         counter++;
      }

      try {
         return new String(bytes, "UTF-8");
      } catch (UnsupportedEncodingException e) {
         log.error(e.getMessage(), e);
         return "";
      }
   }

   protected boolean isExclude(String uri) {
      for (Pattern pattern : proxyConfig.getExcludePattern()) {
         Matcher m = pattern.matcher(uri);
         if (m.matches()) {
            log.debug("skip " + uri);
            return true;
         }
      }
      return false;
   }

   private HttpResponse evalProceedStatus(FullHttpRequest request, EventMetadata metadata) {
      HttpResponseStatus httpStatus = null;
      switch (metadata.getExecutionStatus()) {
      case EXECUTING:
      case EXECUTED:
         if (Context.requestScope().isPlaying()) {
            httpStatus = HttpResponseStatus.OK;
            break;
         } else {
            return null;
         }

      case DENIED:
         httpStatus = HttpResponseStatus.FORBIDDEN;
         break;

      case FIRST_POSTPONED:
      case POSTPONED:
      case SCHEDULED:
         httpStatus = HttpResponseStatus.ACCEPTED;
         break;

      case ERROR:
         httpStatus = (HttpResponseStatus) Context.requestScope().getProperty(InternalRequestScope.HTTPRESPONSESTATUS);
         if (httpStatus == null) {
            httpStatus = HttpResponseStatus.BAD_REQUEST;
         }
         break;

      default:
         String err = "Execution status " + metadata.getExecutionStatus()
               + " should not be possible in CibetProxy.requestPre";
         log.error(err);
         throw new RuntimeException(err);
      }

      log.info(metadata.getResource().getTargetType() + ": --> " + httpStatus);
      HttpResponse response = new DefaultHttpResponse(request.getProtocolVersion(), httpStatus);
      response.headers().set(Names.CONNECTION, Values.CLOSE);
      return response;
   }

   private EventMetadata createEventMetadata(FullHttpRequest request) {
      String querystring = null;
      String requestURI = request.getUri();
      int index = request.getUri().indexOf("?");
      if (index > 0) {
         requestURI = request.getUri().substring(0, index);
         querystring = request.getUri().substring(index + 1);
      }

      if (ProxyUtils.isCONNECT(request)) {

      }
      String host = request.headers().get("host").toLowerCase();
      String targetUrl = requestURI;
      // if (!requestURI.startsWith("http") &&
      // !requestURI.toLowerCase().startsWith(host)) {
      // targetUrl = host + requestURI;
      // }

      if (!requestURI.startsWith("http://") && !requestURI.startsWith("https://")
            && !requestURI.toLowerCase().startsWith(host)) {
         targetUrl = "https://" + host + requestURI;
      }

      String methodName = request.getMethod().toString();

      ControlEvent controlEvent;
      if (Context.requestScope().getProperty(InternalRequestScope.CONTROLEVENT) != null) {
         controlEvent = (ControlEvent) Context.requestScope().getProperty(InternalRequestScope.CONTROLEVENT);
      } else if (request.headers().get(Headers.CIBET_CONTROLEVENT.name()) != null) {
         controlEvent = ControlEvent.valueOf(request.headers().get(Headers.CIBET_CONTROLEVENT.name()));

      } else {
         controlEvent = ControlEvent.INVOKE;
      }

      if (log.isDebugEnabled()) {
         log.debug("control " + controlEvent + " of " + methodName + " " + targetUrl + " for tenant "
               + Context.internalSessionScope().getTenant());
      }

      HttpRequestData httpData = new HttpRequestData(host, querystring, requestURI);
      HttpRequestResource resource = new HttpRequestResource(targetUrl, methodName, httpData);
      addHeaders(request.headers(), resource);

      String contentType = request.headers().get("Content-Type");
      addParameters(querystring, content(request.content()), contentType, resource);

      resource.setInvoker("localhost");
      EventMetadata metadata = new EventMetadata(controlEvent, resource);
      metadata.setProxyConfig(proxyConfig);
      return metadata;
   }

   private void addParameters(String querystring, String body, String contentType, HttpRequestResource resource) {
      if (querystring != null) {
         Map<String, List<String>> params = splitQuery(querystring);
         for (Entry<String, List<String>> entry : params.entrySet()) {
            if (entry.getValue() != null && entry.getValue().size() == 1) {
               resource.addParameter(entry.getKey(), entry.getValue().get(0), ParameterType.HTTP_PARAMETER);
               log.debug(entry.getKey() + " = " + entry.getValue().get(0));
            } else {
               resource.addParameter(entry.getKey(), entry.getValue(), ParameterType.HTTP_PARAMETER);
               if (log.isDebugEnabled()) {
                  for (String v : entry.getValue()) {
                     log.debug(entry.getKey() + " = " + v);
                  }
               }
            }
         }
      }

      if (body != null && body.length() > 0) {
         if (contentType.toLowerCase().startsWith(Values.APPLICATION_X_WWW_FORM_URLENCODED)) {
            Map<String, List<String>> params = splitQuery(body);
            for (Entry<String, List<String>> entry : params.entrySet()) {
               if (entry.getValue() != null && entry.getValue().size() == 1) {
                  resource.addParameter(entry.getKey(), entry.getValue().get(0), ParameterType.HTTP_PARAMETER);
                  log.debug(entry.getKey() + " = " + entry.getValue().get(0));
               } else {
                  resource.addParameter(entry.getKey(), entry.getValue(), ParameterType.HTTP_PARAMETER);
                  if (log.isDebugEnabled()) {
                     for (String v : entry.getValue()) {
                        log.debug(entry.getKey() + " = " + v);
                     }
                  }
               }
            }

         } else {
            resource.addParameter("__HTTP_BODY", body, ParameterType.HTTP_BODY);
            log.debug("body length = " + body.length());
         }
      }
   }

   public static Map<String, List<String>> splitQuery(String url) {
      try {
         final Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();
         final String[] pairs = url.split("&");
         for (String pair : pairs) {
            final int idx = pair.indexOf("=");
            final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
            if (!query_pairs.containsKey(key)) {
               query_pairs.put(key, new LinkedList<String>());
            }
            final String value = idx > 0 && pair.length() > idx + 1
                  ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
            query_pairs.get(key).add(value);
         }
         return query_pairs;
      } catch (UnsupportedEncodingException e) {
         throw new RuntimeException(e);
      }
   }

   private void addHeaders(HttpHeaders headers, HttpRequestResource resource) {
      log.debug("HTTP HEADERS:");
      for (String name : headers.names()) {
         if (name.toUpperCase().startsWith("CIBET_"))
            continue;
         String headerName = name.toLowerCase();
         List<String> headerValues = headers.getAll(name);
         if (headerValues.size() == 1) {
            log.debug(headerName + " = " + headerValues.get(0));
            resource.addParameter(headerName, headerValues.get(0), ParameterType.HTTP_HEADER);
         } else {
            if (log.isDebugEnabled()) {
               for (String v : headerValues) {
                  log.debug(headerName + " = " + v);
               }
            }
            String[] strValues = headerValues.toArray(new String[headerValues.size()]);
            resource.addParameter(headerName, strValues, ParameterType.HTTP_HEADER);
         }
      }
   }

   private void before(EventMetadata metadata) throws NamingException {
      if (metadata == null) {
         return;
      }
      if (metadata.getActuators().isEmpty())
         return;

      if (Context.requestScope().getProperty(InternalRequestScope.ENTITYMANAGER_TYPE) != EntityManagerType.JTA) {
         for (Actuator actuator : metadata.getActuators()) {
            actuator.beforeEvent(metadata);
         }

      } else {
         CibetEEContext ejb = EjbLookup.lookupEjb(proxyConfig.getCibetEEContextEJBJndiName(), CibetEEContextEJB.class);
         if (ejb != null) {
            ejb.beforeEvent(metadata);
         } else {
            String msg = "Failed to lookup CibetEEContext EJB in JNDI";
            log.error(msg);
            throw new NamingException(msg);
         }
      }
   }

   private void after(EventMetadata metadata) throws NamingException {
      if (metadata == null) {
         return;
      }
      if (metadata.getActuators().isEmpty())
         return;

      if (Context.requestScope().getProperty(InternalRequestScope.ENTITYMANAGER_TYPE) != EntityManagerType.JTA) {
         for (Actuator actuator : metadata.getActuators()) {
            actuator.afterEvent(metadata);
         }

      } else {
         CibetEEContext ejb = EjbLookup.lookupEjb(proxyConfig.getCibetEEContextEJBJndiName(), CibetEEContextEJB.class);
         if (ejb != null) {
            ejb.afterEvent(metadata);
         } else {
            String msg = "Failed to lookup CibetEEContext EJB in JNDI";
            log.error(msg);
            throw new NamingException(msg);
         }
      }
   }

   private void addEventResultHeader(HttpResponse response) {
      EventResult result = Context.requestScope().getExecutedEventResult();
      if (result != null) {
         String strExtResult = response.headers().get(Headers.CIBET_EVENTRESULT.name());
         if (strExtResult != null) {
            EventResult extResult = CibetUtil.decodeEventResult(strExtResult);
            extResult.setParentResult(result);
            result.getChildResults().add(extResult);
         }

         try {
            String strResult = Base64.encodeBase64String(CibetUtil.encode(result));
            HttpHeaders.setHeader(response, Headers.CIBET_EVENTRESULT.name(), strResult);
         } catch (IOException e) {
            log.error(e.getMessage(), e);
         }
      }
   }

   private HttpObject stop(HttpObject httpObject) {
      if (httpObject != null && !(httpObject instanceof HttpResponse)) {
         return httpObject;
      }

      if (skip) {
         log.debug("skip Cibet afterEvent ...");
         return httpObject;
      } else {
         // dont execute 2 times for 1 request (timeout, bad gateway)
         skip = true;
      }

      boolean startManaging = true;
      try {
         log.debug("++ " + Context.requestScope());
         startManaging = Context.start();
         Context.internalRequestScope().getProperties().putAll(requestContext);
         Context.internalSessionScope().getProperties().putAll(sessionContext);

         if (httpObject != null) {
            HttpResponse fhr = (HttpResponse) httpObject;
            if (fhr.getStatus().code() >= 500) {
               metadata.setExecutionStatus(ExecutionStatus.ERROR);
            }
            metadata.getResource().setResultObject(fhr.getStatus().code());
         } else {
            metadata.getResource().setResultObject(HttpResponseStatus.OK.code());
         }

         after(metadata);
         log.debug("metadata.getExecutionStatus==" + metadata.getExecutionStatus());

      } catch (Exception e) {
         log.error(e.getMessage(), e);
         metadata.setExecutionStatus(ExecutionStatus.ERROR);
         if (httpObject != null) {
            ((HttpResponse) httpObject).setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
         }

      } finally {
         if (metadata != null && eventResult != null) {
            eventResult.setExecutionStatus(metadata.getExecutionStatus());

            if (httpObject != null) {
               HttpResponse defaultResponse = (HttpResponse) httpObject;
               if (defaultResponse.getStatus().code() >= 400 && defaultResponse.getStatus().code() != 403) {
                  eventResult.setExecutionStatus(ExecutionStatus.ERROR);
               }
               addEventResultHeader(defaultResponse);
            }
         }

         if (startManaging || newEM) {
            Context.end();
         }
      }

      return httpObject;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.littleshoot.proxy.HttpFiltersAdapter#clientToProxyRequest(io.netty .handler.codec.http.HttpObject)
    */
   @Override
   public HttpResponse clientToProxyRequest(HttpObject httpObject) {
      log(httpObject, "clientToProxyRequest");
      log.debug("++ " + Context.requestScope());

      if (ProxyUtils.isCONNECT(httpObject) && proxyConfig.getMode() == ProxyMode.CHAINEDPROXY) {
         log.debug("skip Cibet beforeEvent for CONNECT method in ProxyMode CHAINEDPROXY");
         skip = true;
         return null;
      }

      if (!(httpObject instanceof FullHttpRequest)) {
         log.warn("Proxy receives request of unsupported type " + httpObject);
         return null;
      }

      FullHttpRequest request = (FullHttpRequest) httpObject;
      if (isExclude(request.getUri())) {
         log.debug("skip Cibet beforeEvent for " + request.getUri());
         skip = true;
         return null;
      }

      List<SecurityContext> secCtxs = new ArrayList<>();
      boolean startManaging = true;

      try {
         secCtxs = parseHeaders(request.headers());
         if (Context.internalRequestScope().getNullableEntityManager() == null
               && Context.internalRequestScope().isManaged()) {
            newEM = true;
         }
         startManaging = Context.start();

         if (Context.requestScope().getCaseId() == null) {
            String caseid = UUID.randomUUID().toString();
            Context.requestScope().setCaseId(caseid);
         }
         request.headers().set(Headers.CIBET_CASEID.name(), Context.requestScope().getCaseId());

         metadata = createEventMetadata(request);
         Configuration.instance().getController().evaluate(metadata);
         eventResult = Context.internalRequestScope().registerEventResult(new EventResult(SENSOR_NAME, metadata));

         try {
            before(metadata);

            if (metadata.getExecutionStatus() == ExecutionStatus.EXECUTING) {
               metadata.setExecutionStatus(ExecutionStatus.EXECUTED);
            } else {
               eventResult.setExecutionStatus(metadata.getExecutionStatus());
            }

         } catch (Throwable e) {
            log.error(e.getMessage(), e);
            metadata.setExecutionStatus(ExecutionStatus.ERROR);
            Context.requestScope().setRemark(e.getMessage());
            metadata.setException(e);
         }

         HttpResponse resp = evalProceedStatus(request, metadata);
         return resp;

      } finally {
         storeContext();

         for (SecurityContext secCtx : secCtxs) {
            Context.internalRequestScope().getAuthenticationProvider().stopSecurityContext(secCtx);
         }
         if (startManaging || newEM) {
            Context.end();
         } else if (newEM) {

         }
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.littleshoot.proxy.HttpFiltersAdapter#serverToProxyResponseTimedOut()
    */
   @Override
   public void serverToProxyResponseTimedOut() {
      log(null, "serverToProxyResponseTimedOut");
      metadata.setExecutionStatus(ExecutionStatus.ERROR);
      super.serverToProxyResponseTimedOut();
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.littleshoot.proxy.HttpFiltersAdapter#proxyToClientResponse(io.netty .handler.codec.http.HttpObject)
    */
   @Override
   public HttpObject proxyToClientResponse(HttpObject httpObject) {
      log(httpObject, "proxyToClientResponse");
      return stop(httpObject);
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.littleshoot.proxy.HttpFiltersAdapter#proxyToServerResolutionFailed (java.lang.String)
    */
   @Override
   public void proxyToServerResolutionFailed(String hostAndPort) {
      log(null, "proxyToServerResolutionFailed");
      metadata.setExecutionStatus(ExecutionStatus.ERROR);
      super.proxyToServerResolutionFailed(hostAndPort);
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.littleshoot.proxy.HttpFiltersAdapter#proxyToServerConnectionFailed()
    */
   @Override
   public void proxyToServerConnectionFailed() {
      log(null, "proxyToServerConnectionFailed");
      metadata.setExecutionStatus(ExecutionStatus.ERROR);
      super.proxyToServerConnectionFailed();
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.littleshoot.proxy.HttpFiltersAdapter#proxyToServerConnectionSucceeded
    * (io.netty.channel.ChannelHandlerContext)
    */
   @Override
   public void proxyToServerConnectionSucceeded(ChannelHandlerContext serverCtx) {
      log(serverCtx, "proxyToServerConnectionSucceeded");
      if ("CONNECT".equals(((HttpRequestResource) metadata.getResource()).getMethod())) {
         stop(null);
      }
   }

   private List<SecurityContext> parseHeaders(HttpHeaders headers) {
      List<SecurityContext> secList = new ArrayList<>();
      String contextHeader = headers.get(Headers.CIBET_CONTEXT.name());
      if (contextHeader == null) {
         return secList;
      }

      try {
         Map<String, String> map = new HashMap<>();

         StringTokenizer tok = new StringTokenizer(contextHeader, "&");
         while (tok.hasMoreTokens()) {
            String pair = tok.nextToken();
            int equal = pair.indexOf("=");
            if (equal < 0) {
               String err = "Unparsable header " + Headers.CIBET_CONTEXT + ": " + contextHeader;
               log.error(err);
               throw new RuntimeException(err);
            }
            String key = pair.substring(0, equal);
            String value = pair.substring(equal + 1);
            map.put(key, URLDecoder.decode(value, "UTF-8"));
         }

         for (Entry<String, String> entry : map.entrySet()) {
            if (entry.getKey().startsWith(Context.PARAM_SECURITYCONTEXT)) {
               SecurityContext securityContext = Context.internalRequestScope().getAuthenticationProvider()
                     .initSecurityContext(entry.getValue());
               if (securityContext != null && securityContext.isInitialised()) {
                  secList.add(securityContext);
               }

            } else if (entry.getKey().startsWith(Context.PREFIX_REQUEST)) {
               byte[] bytes = Base64.decodeBase64(entry.getValue());
               Object obj = CibetUtil.decode(bytes);
               Context.internalRequestScope().setProperty(entry.getKey().substring(Context.PREFIX_REQUEST.length()),
                     obj);
               log.debug("set into request context " + entry.getKey().substring(Context.PREFIX_REQUEST.length()) + " = "
                     + obj);

            } else if (entry.getKey().startsWith(Context.PREFIX_SESSION)) {
               byte[] bytes = Base64.decodeBase64(entry.getValue());
               Object obj = CibetUtil.decode(bytes);
               Context.internalSessionScope().setProperty(entry.getKey().substring(Context.PREFIX_SESSION.length()),
                     obj);
               log.debug("set into session context " + entry.getKey().substring(Context.PREFIX_SESSION.length()) + " = "
                     + obj);
            }
         }
      } catch (UnsupportedEncodingException e) {
         log.error(e.getMessage(), e);
         throw new RuntimeException(e);
      }
      return secList;
   }

   private void storeContext() {
      for (Entry<String, Object> entry : Context.internalSessionScope().getProperties().entrySet()) {
         if (entry.getValue() != null && entry.getValue() instanceof Serializable) {
            sessionContext.put(entry.getKey(), entry.getValue());
         }
      }

      for (Entry<String, Object> entry : Context.internalRequestScope().getProperties().entrySet()) {
         if (entry.getValue() != null && entry.getValue() instanceof Serializable
               && !(entry.getValue() instanceof EntityManager)) {
            requestContext.put(entry.getKey(), entry.getValue());
         }
      }
   }
}
