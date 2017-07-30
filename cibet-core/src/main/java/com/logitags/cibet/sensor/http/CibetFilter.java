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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.common.Actuator;
import com.logitags.cibet.authentication.AuthenticationProvider;
import com.logitags.cibet.authentication.IPAuthenticationProvider;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.context.CibetContextFilter;
import com.logitags.cibet.context.CibetEEContext;
import com.logitags.cibet.context.CibetEEContextEJB;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.jndi.EjbLookup;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.resource.ResourceParameter;

public class CibetFilter extends CibetContextFilter implements Filter {

   private static Log log = LogFactory.getLog(CibetFilter.class);

   public static final String SENSOR_NAME = "HTTP-FILTER";

   /**
    * Allows controlling of anonymous http requests. User is remote IP/port in this case.
    */
   @Override
   public void init(FilterConfig config) throws ServletException {
      super.init(config);
   }

   public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
         throws IOException, ServletException {
      if (isExclude(req)) {
         chain.doFilter(req, resp);
         return;
      }

      HttpServletRequest request = (HttpServletRequest) req;
      HttpServletResponse response = (HttpServletResponse) resp;

      if (log.isDebugEnabled()) {
         log.debug("execute CibetContextFilter URL " + ((HttpServletRequest) req).getRequestURL());
         log.debug("remote Address: " + req.getRemoteAddr());
         log.debug("remote host: " + req.getRemoteHost());
         log.debug("remote port: " + req.getRemotePort());
         log.debug("remote user: " + request.getRemoteUser());
         log.debug("principal: " + request.getUserPrincipal());
      }

      AuthenticationProvider auth = null;
      boolean startManaging = true;
      EventMetadata metadata = null;
      EventResult thisResult = null;
      if (allowAnonymous) {
         auth = new IPAuthenticationProvider();
      }

      try {
         startManaging = Context.start(EJB_JNDINAME, auth);
         fillCibetContext(request);

         // set header remark and caseid
         String caseid = request.getHeader(Headers.CIBET_CASEID.name());
         if (caseid != null) {
            Context.internalRequestScope().setCaseId(caseid);
         }
         String remark = request.getHeader(Headers.CIBET_REMARK.name());
         if (remark != null) {
            Context.internalRequestScope().setRemark(remark);
         }
         String playMode = request.getHeader(Headers.CIBET_PLAYINGMODE.name());
         if (playMode != null) {
            Context.requestScope().startPlay();
         }
         String scheduledDate = request.getHeader(Headers.CIBET_SCHEDULEDDATE.name());
         if (scheduledDate != null) {
            Date date = new Date();
            date.setTime(Long.parseLong(scheduledDate));
            Context.internalRequestScope().setScheduledDate(date);
         }

         metadata = createEventMetadata(request, response);
         Configuration.instance().getController().evaluate(metadata);
         thisResult = Context.internalRequestScope().registerEventResult(new EventResult(SENSOR_NAME, metadata));

         CibetEEContext ejb = null;
         try {
            ejb = before(metadata);

            if (metadata.getExecutionStatus() == ExecutionStatus.EXECUTING) {
               metadata.setExecutionStatus(ExecutionStatus.EXECUTED);
               if (Context.internalRequestScope().isPostponed()) {
                  thisResult.setExecutionStatus(ExecutionStatus.POSTPONED);
               } else {
                  thisResult.setExecutionStatus(metadata.getExecutionStatus());
               }

               // set header here before response is comitted
               addEventResultHeader(response);
               if (!Context.requestScope().isPlaying()) {
                  chain.doFilter(req, response);
                  metadata.getResource().setResultObject(response.getStatus());
               }
            }

            log.debug("after chain.doFilter");

            if (metadata.getExecutionStatus() == ExecutionStatus.POSTPONED) {
               addBody(req, metadata.getResource());
            }

         } catch (Throwable e) {
            log.error(e.getMessage(), e);
            metadata.setExecutionStatus(ExecutionStatus.ERROR);
            Context.requestScope().setRemark(e.getMessage());
            metadata.setException(e);
         }

         try {
            after(ejb, metadata);
         } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServletException(e);
         }

      } finally {
         if (metadata != null && thisResult != null) {
            thisResult.setExecutionStatus(metadata.getExecutionStatus());
         }
         addEventResultHeader(response);

         if (startManaging) {
            Context.end();
         } else {
            Context.internalRequestScope().getAuthenticationProvider().getProviderChain().remove(auth);
            if (metadata != null && metadata.getExecutionStatus() == ExecutionStatus.ERROR) {
               Context.requestScope().setRemark(null);
            }
         }
      }

      evalProceedStatus(metadata, response);
      log.debug("CibetFilter finished");
   }

   private void addEventResultHeader(HttpServletResponse response) {
      if (response.getHeader(Headers.CIBET_EVENTRESULT.name()) == null) {
         EventResult result = Context.requestScope().getExecutedEventResult();
         if (result != null) {
            try {
               String strResult = Base64.encodeBase64String(CibetUtil.encode(result));
               response.setHeader(Headers.CIBET_EVENTRESULT.name(), strResult);
            } catch (IOException e) {
               log.error(e.getMessage(), e);
               response.addHeader(Headers.CIBET_EVENTRESULT.name(), "ERROR: " + e.getMessage());
            }
         }
      }
   }

   private CibetEEContext before(EventMetadata metadata) throws ServletException {
      if (metadata.getActuators().isEmpty()) return null;

      CibetEEContext ejb = EjbLookup.lookupEjb(EJB_JNDINAME, CibetEEContextEJB.class);
      if (ejb != null) {
         ejb.beforeEvent(metadata);
         return ejb;
      } else {
         for (Actuator actuator : metadata.getActuators()) {
            actuator.beforeEvent(metadata);
         }
         return null;
      }

      // if
      // (Context.requestScope().getProperty(InternalRequestScope.ENTITYMANAGER_TYPE)
      // != EntityManagerType.JTA) {
      // for (Actuator actuator : metadata.getConfig().getActuators()) {
      // actuator.beforeEvent(metadata);
      // }
      // return null;
      //
      // } else {
      // CibetEEContext ejb = EjbLookup.lookupEjb(
      // (String)
      // Context.applicationScope().getProperty(CIBETEECONTEXTEJB_JNDINAME),
      // CibetEEContextEJB.class);
      // if (ejb != null) {
      // ejb.beforeEvent(metadata);
      // return ejb;
      // } else {
      // String msg = "Failed to lookup CibetEEContext EJB in JNDI";
      // log.error(msg);
      // throw new ServletException(msg);
      // }
      // }
   }

   private void after(CibetEEContext ejb, EventMetadata metadata) throws ServletException {
      if (metadata.getActuators().isEmpty()) return;
      if (ejb != null) {
         ejb.afterEvent(metadata);

      } else {
         for (Actuator actuator : metadata.getActuators()) {
            actuator.afterEvent(metadata);
         }
      }
   }

   private EventMetadata createEventMetadata(HttpServletRequest request, HttpServletResponse response) {
      String targetUrl = request.getRequestURL().toString();
      String methodName = request.getMethod();

      ControlEvent controlEvent = ControlEvent.INVOKE;
      String event = request.getHeader(Headers.CIBET_CONTROLEVENT.name());
      if (event != null) {
         controlEvent = ControlEvent.valueOf(event);
      }

      if (log.isDebugEnabled()) {
         log.debug("control " + controlEvent + " of " + targetUrl + " for tenant "
               + Context.internalSessionScope().getTenant() + ". Content-Type=" + request.getContentType());
      }

      HttpRequestResource resource = new HttpRequestResource(targetUrl, methodName, request, response);
      addParameters(request, resource);
      addHeaders(request, resource);
      addAttributes(request, resource);

      // set invoker
      String ipaddress = request.getHeader("HTTP_X_FORWARDED_FOR");
      if (ipaddress == null) {
         ipaddress = request.getRemoteAddr() + ", " + request.getRemoteHost();
      } else {
         ipaddress = ipaddress + ", " + request.getRemoteAddr() + ", " + request.getRemoteHost();
      }
      log.debug("INVOKER: " + ipaddress);
      resource.setInvoker(ipaddress);
      EventMetadata metadata = new EventMetadata(SENSOR_NAME, controlEvent, resource);
      return metadata;
   }

   private void addHeaders(HttpServletRequest request, Resource resource) {
      log.debug("HTTP HEADERS:");
      Enumeration<String> headers = request.getHeaderNames();
      while (headers.hasMoreElements()) {
         String headerName = headers.nextElement();
         if (headerName.toUpperCase().startsWith("CIBET_")) continue;
         headerName = headerName.toLowerCase();
         Enumeration<String> header = request.getHeaders(headerName);
         List<String> headerValues = new ArrayList<String>();
         while (header.hasMoreElements()) {
            headerValues.add(header.nextElement());
         }
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

   private void addAttributes(HttpServletRequest request, Resource resource) {
      log.debug("HTTP ATTRIBUTES:");
      Enumeration<String> attrs = request.getAttributeNames();
      while (attrs.hasMoreElements()) {
         String attrName = attrs.nextElement();
         Object attr = request.getAttribute(attrName);

         try {
            ResourceParameter param;
            if (attr == null) {
               param = new ResourceParameter(attrName, String.class.getName(), null, ParameterType.HTTP_ATTRIBUTE,
                     resource.getParameters().size());
            } else {
               param = new ResourceParameter(attrName, attr.getClass().getName(), attr, ParameterType.HTTP_ATTRIBUTE,
                     resource.getParameters().size());
            }

            // check if the object could be regenerated
            // (ClassNotFoundException?)
            CibetUtil.decode(param.getEncodedValue());
            resource.addParameter(param);
            log.debug(attrName + " = " + attr);
            continue;
         } catch (Exception e) {
            log.warn(e.getMessage() + "; Cause: " + e.getCause());
         }

         String strAttr = attr == null ? null : attr.toString();
         resource.addParameter(attrName, strAttr, ParameterType.HTTP_ATTRIBUTE);
         log.warn(
               "Object " + attr + " will not be archived by ARCHIVE or DC actuators. Instead the String representation "
                     + " will be archived");
      }
   }

   private void addParameters(HttpServletRequest request, Resource resource) {
      log.debug("HTTP PARAMETERS:");
      for (Object key : request.getParameterMap().keySet()) {
         String[] values = (String[]) request.getParameterMap().get(key);
         if (values != null && values.length == 1) {
            resource.addParameter((String) key, values[0], ParameterType.HTTP_PARAMETER);
            log.debug(key + " = " + values[0]);
         } else {
            resource.addParameter((String) key, values, ParameterType.HTTP_PARAMETER);
            if (log.isDebugEnabled()) {
               for (String v : values) {
                  log.debug(key + " = " + v);
               }
            }
         }
      }
   }

   private void addBody(ServletRequest req, Resource resource) throws IOException {
      ServletInputStream in = req.getInputStream();
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      int i = in.read();
      while (i != -1) {
         bos.write(i);
         i = in.read();
      }
      in.close();
      byte[] body = bos.toByteArray();
      if (body.length > 0) {
         resource.addParameter("__HTTP_BODY", body, ParameterType.HTTP_BODY);
         log.debug("body length = " + body.length);
      }
   }

   private void evalProceedStatus(EventMetadata metadata, HttpServletResponse response)
         throws IOException, ServletException {
      try {
         if (!response.isCommitted()) {
            if (metadata.getExecutionStatus() == ExecutionStatus.DENIED) {
               log.info("Request to URL " + metadata.getResource().getTarget() + " is in status "
                     + metadata.getExecutionStatus().name() + " and has been intercepted");
               // response.reset();
               response.sendError(HttpServletResponse.SC_FORBIDDEN);
            } else if (metadata.getExecutionStatus() == ExecutionStatus.POSTPONED) {
               log.info("Request to URL " + metadata.getResource().getTarget() + " is in status "
                     + metadata.getExecutionStatus().name() + " and has been intercepted");
               // response.reset();
               response.sendError(HttpServletResponse.SC_ACCEPTED);
            } else if (metadata.getExecutionStatus() == ExecutionStatus.SHED) {
               log.info("Request to URL " + metadata.getResource().getTarget() + " is in status "
                     + metadata.getExecutionStatus().name() + " and has been shed");
               response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            } else {
               metadata.evaluateEventExecuteStatus();
            }

         } else {
            metadata.evaluateEventExecuteStatus();
         }
      } catch (RuntimeException | ServletException | IOException e) {
         throw e;
      } catch (Throwable e) {
         throw new ServletException(e);
      }
   }

}
