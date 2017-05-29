/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 *******************************************************************************
 */
package com.logitags.cibet.sensor.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClients;

import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.config.ProxyConfig;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalRequestScope;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.common.Invoker;

public class HttpRequestInvoker implements Invoker {

   private static Log log = LogFactory.getLog(HttpRequestInvoker.class);

   private static Invoker instance = null;

   public static synchronized Invoker createInstance() {
      if (instance == null) {
         instance = new HttpRequestInvoker();
      }
      return instance;
   }

   @Override
   public Object execute(String parameter, String targetType, String methodName, Set<ResourceParameter> parameters)
         throws Exception {
      if (targetType == null) {
         throw new IllegalArgumentException("targetType must not be null");
      }
      if (methodName == null) {
         throw new IllegalArgumentException("methodName must not be null");
      }

      String url = createQuerystringURL(targetType, parameters);
      HttpRequestBase request = null;
      if ("DELETE".equals(methodName.toUpperCase())) {
         request = new HttpDelete(url);
      } else if (HttpMethod.GET.name().equals(methodName.toUpperCase())) {
         request = new HttpGet(url);
      } else if (HttpMethod.HEAD.name().equals(methodName.toUpperCase())) {
         request = new HttpHead(url);
      } else if (HttpMethod.OPTIONS.name().equals(methodName.toUpperCase())) {
         request = new HttpOptions(url);
      } else if (HttpMethod.POST.name().equals(methodName.toUpperCase())) {
         request = new HttpPost(url);
         addBodyEntity((HttpEntityEnclosingRequestBase) request, parameters);
      } else if (HttpMethod.PUT.name().equals(methodName.toUpperCase())) {
         request = new HttpPut(url);
         addBodyEntity((HttpEntityEnclosingRequestBase) request, parameters);
      } else if (HttpMethod.TRACE.name().equals(methodName.toUpperCase())) {
         request = new HttpTrace(url);
      } else {
         throw new IllegalArgumentException("Failed to invoke " + targetType
               + ": methodName must be one of DELETE, GET, HEAD, " + "OPTIONS, POST, PUT or TRACE");
      }

      ProxyConfig proxyConfig = null;
      if (parameters != null) {
         for (ResourceParameter param : parameters) {
            if (ProxyConfig.PROXYCONFIG.equals(param.getName())) {
               proxyConfig = (ProxyConfig) param.getUnencodedValue();
               log.debug("++ set proxyConfig " + proxyConfig);
               continue;
            }
            if (param.getParameterType() != ParameterType.HTTP_HEADER) {
               continue;
            }
            if (param.getName().toLowerCase().equals("content-length")) {
               continue;
            }
            if (param.getName().toLowerCase().equals("content-type")) {
               // continue;
            }
            if (param.getName().toLowerCase().equals("content-md5")) {
               continue;
            }

            if ("[Ljava.lang.String;".equals(param.getClassname())) {
               String[] strValues = (String[]) param.getUnencodedValue();
               for (String s : strValues) {
                  request.addHeader(param.getName(), s);
               }
            } else {
               request.addHeader(param.getName(), (String) param.getUnencodedValue());
            }
         }
      }

      // set Cibet headers
      if (proxyConfig != null) {
         String cibetCtx;
         DcControllable dc = (DcControllable) Context.requestScope().getProperty(InternalRequestScope.DCCONTROLLABLE);
         if (dc != null) {
            // remove it to respect header max length
            Context.requestScope().removeProperty(InternalRequestScope.DCCONTROLLABLE);
            cibetCtx = Context.encodeContext();
            Context.requestScope().setProperty(InternalRequestScope.DCCONTROLLABLE, dc);
         } else {
            cibetCtx = Context.encodeContext();
         }
         request.addHeader(Headers.CIBET_CONTEXT.name(), cibetCtx);
      } else {
         if (Context.internalRequestScope().getProperty(InternalRequestScope.CONTROLEVENT) != null) {
            request.addHeader(Headers.CIBET_CONTROLEVENT.name(),
                  Context.internalRequestScope().getProperty(InternalRequestScope.CONTROLEVENT).toString());
         }
         if (Context.internalRequestScope().getCaseId() != null) {
            request.addHeader(Headers.CIBET_CASEID.name(), Context.internalRequestScope().getCaseId());
         }
         if (Context.internalRequestScope().getRemark() != null) {
            request.addHeader(Headers.CIBET_REMARK.name(), Context.internalRequestScope().getRemark());
         }
         if (Context.internalRequestScope().getScheduledDate() != null) {
            request.addHeader(Headers.CIBET_SCHEDULEDDATE.name(),
                  Long.toString(Context.internalRequestScope().getScheduledDate().getTime()));
         }
         if (Context.requestScope().isPlaying()) {
            request.addHeader(Headers.CIBET_PLAYINGMODE.name(), "true");
         }
      }

      return sendHttpRequest(request, targetType, methodName, proxyConfig);
   }

   private HttpResponse sendHttpRequest(HttpRequestBase request, String targetType, String methodName,
         ProxyConfig proxyConfig) throws ClientProtocolException, IOException {
      HttpClient client = null;

      if (proxyConfig != null) {
         HttpHost proxy = new HttpHost("localhost", proxyConfig.getPort());
         client = HttpClients.custom().setProxy(proxy).build();

      } else {
         client = HttpClients.custom().build();
      }

      HttpResponse response = client.execute(request);
      if (log.isInfoEnabled()) {
         log.info("executed URL " + request.getURI() + " Response code: " + response.getStatusLine().getStatusCode());
      }

      Header eventResultHeader = response.getFirstHeader(Headers.CIBET_EVENTRESULT.name());
      if (eventResultHeader != null) {
         log.debug("retrieve EventResult from HttpResponse Header");
         String evReHeader = eventResultHeader.getValue();
         EventResult remoteResult = CibetUtil.decodeEventResult(evReHeader);
         Context.internalRequestScope().registerEventResult(new EventResult(remoteResult));
      } else {
         log.debug("create new local EventResult");
         HttpRequestResource resource = new HttpRequestResource(targetType, methodName, null);
         EventMetadata metadata = new EventMetadata(
               (ControlEvent) Context.internalRequestScope().getProperty(InternalRequestScope.CONTROLEVENT), resource);
         EventResult thisResult = Context.internalRequestScope()
               .registerEventResult(new EventResult(CibetFilter.SENSOR_NAME, metadata));

         // Consider if a Postponed or Denied result page is configured in
         // CibetFilter
         // CibetFilter is executed in a remote thread
         if (response.getStatusLine().getStatusCode() == HttpStatus.SC_ACCEPTED) {
            thisResult.setExecutionStatus(ExecutionStatus.POSTPONED);
         } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN) {
            thisResult.setExecutionStatus(ExecutionStatus.DENIED);
         } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_SERVICE_UNAVAILABLE) {
            thisResult.setExecutionStatus(ExecutionStatus.SHED);
         } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            thisResult.setExecutionStatus(ExecutionStatus.EXECUTED);
         } else {
            thisResult.setExecutionStatus(ExecutionStatus.ERROR);
         }
      }

      return response;
   }

   private String createQuerystringURL(String url, Set<ResourceParameter> parameters)
         throws UnsupportedEncodingException {
      if (parameters == null)
         return url;
      StringBuffer b = new StringBuffer();
      for (ResourceParameter param : parameters) {
         if (param.getParameterType() != ParameterType.HTTP_PARAMETER)
            continue;
         if ("java.lang.String".equals(param.getClassname())) {
            if (b.length() > 0)
               b.append("&");
            b.append(URLEncoder.encode(param.getName(), "UTF-8"));
            b.append("=");
            b.append(URLEncoder.encode((String) param.getUnencodedValue(), "UTF-8"));

         } else if ("[Ljava.lang.String;".equals(param.getClassname())) {
            for (String value : (String[]) param.getUnencodedValue()) {
               if (b.length() > 0)
                  b.append("&");
               b.append(URLEncoder.encode(param.getName(), "UTF-8"));
               b.append("=");
               b.append(URLEncoder.encode(value, "UTF-8"));
            }

         } else {
            String msg = "Can not handle http parameter of type " + param.getClassname();
            log.error(msg);
            throw new RuntimeException(msg);
         }
      }

      if (b.length() > 0) {
         url = url + "?" + b.toString();
      }
      log.debug(url);
      return url;
   }

   private void addBodyEntity(HttpEntityEnclosingRequestBase request, Set<ResourceParameter> parameters) {
      for (ResourceParameter param : parameters) {
         if (param.getParameterType() != ParameterType.HTTP_BODY)
            continue;
         log.debug("add body to request");
         ByteArrayEntity entity = new ByteArrayEntity((byte[]) param.getUnencodedValue());
         request.setEntity(entity);
         break;
      }
   }

}
