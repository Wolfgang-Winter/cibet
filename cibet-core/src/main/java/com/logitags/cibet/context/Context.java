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
package com.logitags.cibet.context;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map.Entry;

import javax.persistence.EntityManager;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.core.CibetUtil;

/**
 * abstract class for retrieving Cibet contexts.
 * 
 * @author Wolfgang
 * 
 */
public abstract class Context {

   private static Log log = LogFactory.getLog(Context.class);

   public static final String PREFIX_REQUEST = "CR_";
   public static final String PREFIX_SESSION = "CS_";
   public static final String PARAM_SECURITYCONTEXT = "SEC_";

   private static InternalSessionScope sessionScope;

   private static InternalRequestScope requestScope;

   private static ApplicationScope applicationScope;

   /**
    * returns the application scope context.
    * 
    * @return
    */
   public static synchronized ApplicationScope applicationScope() {
      if (applicationScope == null) {
         applicationScope = new ApplicationScopeContext();
      }
      return applicationScope;
   }

   /**
    * returns the session scope context.
    * 
    * @return
    */
   public static synchronized SessionScope sessionScope() {
      if (sessionScope == null) {
         sessionScope = new SessionScopeContext();
      }
      return sessionScope;
   }

   /**
    * returns the request scope context.
    * 
    * @return
    */
   public static synchronized RequestScope requestScope() {
      if (requestScope == null) {
         requestScope = new RequestScopeContext();
      }
      return requestScope;
   }

   /**
    * returns the internal session scope context. Only called internally by the framework
    * 
    * @return
    */
   public static synchronized InternalSessionScope internalSessionScope() {
      if (sessionScope == null) {
         sessionScope = new SessionScopeContext();
      }
      return sessionScope;
   }

   /**
    * returns the internal request scope context. Only called internally by the framework
    * 
    * @return
    */
   public static synchronized InternalRequestScope internalRequestScope() {
      if (requestScope == null) {
         requestScope = new RequestScopeContext();
      }
      return requestScope;
   }

   /**
    * returns the encoded context as a String. This includes the request and session scope contexts and the security
    * context provided by Spring Security or Apache Shiro. The encoded context may be set as HTTP request header with
    * name CIBET_CONTEXT in order to transfer context from the application to the CibetProxy sensor.
    * 
    * @return
    */
   public static String encodeContext() {
      StringBuffer b = new StringBuffer();
      try {
         String secCtx = Context.internalRequestScope().getAuthenticationProvider().createSecurityContextHeader();
         if (secCtx != null && secCtx.length() > 0) {
            b.append(secCtx);
         }

         for (Entry<String, Object> entry : Context.internalSessionScope().getProperties().entrySet()) {
            if (entry.getValue() != null && entry.getValue() instanceof Serializable) {
               byte[] bytes = CibetUtil.encode(entry.getValue());
               String encodedValue = Base64.encodeBase64String(bytes);
               if (b.length() > 0) {
                  b.append("&");
               }
               b.append(PREFIX_SESSION);
               b.append(entry.getKey());
               b.append("=");
               b.append(URLEncoder.encode(encodedValue, "UTF-8"));
               log.debug("encode session value " + entry.getKey() + " = " + entry.getValue());
            }
         }

         for (Entry<String, Object> entry : Context.internalRequestScope().getProperties().entrySet()) {
            if (entry.getValue() != null && entry.getValue() instanceof Serializable
                  && !(entry.getValue() instanceof EntityManager)) {
               byte[] bytes = CibetUtil.encode(entry.getValue());
               String encodedValue = Base64.encodeBase64String(bytes);
               if (b.length() > 0) {
                  b.append("&");
               }
               b.append(PREFIX_REQUEST);
               b.append(entry.getKey());
               b.append("=");
               b.append(URLEncoder.encode(encodedValue, "UTF-8"));
               log.debug("encode request value " + entry.getKey() + " = " + entry.getValue());
            }
         }

         log.debug("contextHeader: " + b.toString());

         if (b.length() > 8190) {
            log.warn("\nThe Cibet context header value has a length of " + b.length()
                  + "!\nThis may exceed the maximum allowed length of HTTP header fields of some web servers. "
                  + "If you encounter errors when sending HTTP requests with the Cibet context header set, "
                  + "try to increase the maximum header length in the web server configuration\n");
         }

         return b.toString();
      } catch (UnsupportedEncodingException e) {
         log.error(e.getMessage(), e);
         throw new RuntimeException(e);
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new RuntimeException(e);
      }
   }

}
