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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.authentication.AbstractAuthenticationProvider;
import com.logitags.cibet.authentication.AuthenticationProvider;

/**
 * thread safe session context.
 * 
 * @author Wolfgang
 * 
 */
public class SessionScopeContext implements InternalSessionScope {

   private static Log log = LogFactory.getLog(SessionScopeContext.class);

   private ThreadLocalMap tlm = new ThreadLocalMap();

   private InheritableThreadLocal<HttpServletRequest> httpRequest = new InheritableThreadLocal<HttpServletRequest>();

   private static final String USER = "__USER";
   private static final String USER_ADDRESS = "__USER_ADDRESS";
   private static final String SECOND_USER = "__SECOND_USER";
   private static final String TENANT = "__TENANT";
   private static final String APPROVAL_ADDRESS = "__APPROVAL_ADDRESS";
   private static final String APPROVAL_USER = "__APPROVAL_USER";

   SessionScopeContext() {
   }

   @Override
   public void setUser(String userId) {
      setProperty(USER, userId);
   }

   @Override
   public String getUser() {
      String user = (String) getProperty(USER);
      if (user == null) {
         AuthenticationProvider authProv = Context.internalRequestScope().getAuthenticationProvider();
         if (authProv != null) {
            user = authProv.getUsername();
            if (user != null) {
               setProperty(USER, user);
            }
         }
      }
      return user;
   }

   @Override
   public String getUserAddress() {
      String addr = (String) getProperty(USER_ADDRESS);
      if (addr == null) {
         AuthenticationProvider authProv = Context.internalRequestScope().getAuthenticationProvider();
         if (authProv != null) {
            addr = authProv.getUserAddress();
            if (addr != null) {
               setProperty(USER_ADDRESS, addr);
            }
         }
      }
      return addr;
   }

   @Override
   public void setUserAddress(String address) {
      setProperty(USER_ADDRESS, address);
   }

   @Override
   public void setSecondUser(String user) {
      setProperty(SECOND_USER, user);
      if (user == null) {
         setProperty(SECOND_PRINCIPAL, null);
      }
   }

   @Override
   public String getSecondUser() {
      return (String) getProperty(SECOND_USER);
   }

   @Override
   public void setTenant(String tenant) {
      setProperty(TENANT, tenant);
   }

   @Override
   public String getTenant() {
      String owner = (String) getProperty(TENANT);
      if (AbstractAuthenticationProvider.DEFAULT_TENANT.equals(owner) || owner == null || owner.length() == 0) {
         AuthenticationProvider authProv = Context.internalRequestScope().getAuthenticationProvider();
         if (authProv != null) {
            owner = authProv.getTenant();
            if (owner != null) {
               setProperty(TENANT, owner);
            }
         }
      }
      return owner;
   }

   @Override
   public void setProperty(String key, Object value) {
      getProperties().put(key, value);

      HttpServletRequest req = httpRequest.get();
      if (req == null)
         return;
      HttpSession ses = req.getSession();
      if (ses != null) {
         log.debug("update HttpSession: " + key);
         @SuppressWarnings("unchecked")
         Map<String, Object> map = (Map<String, Object>) ses.getAttribute(KEY_SESSION_PROPERTIES);
         if (map == null) {
            map = new HashMap<String, Object>();
            ses.setAttribute(KEY_SESSION_PROPERTIES, map);
         }
         map.put(key, value);
      }
   }

   @Override
   public void removeProperty(String key) {
      getProperties().remove(key);

      HttpServletRequest req = httpRequest.get();
      if (req == null)
         return;
      HttpSession ses = req.getSession();
      if (ses != null) {
         log.debug("remove from HttpSession: " + key);
         @SuppressWarnings("unchecked")
         Map<String, Object> map = (Map<String, Object>) ses.getAttribute(KEY_SESSION_PROPERTIES);
         if (map != null) {
            map.remove(key);
         }
      }
   }

   @Override
   public Object getProperty(String key) {
      HashMap<String, Object> ht = tlm.get();
      if (ht != null && key != null) {
         return ht.get(key);
      } else {
         return null;
      }
   }

   @Override
   public void clear() {
      tlm.remove();
      httpRequest.remove();
   }

   @Override
   public HashMap<String, Object> getProperties() {
      HashMap<String, Object> ht = tlm.get();
      if (ht == null) {
         ht = new HashMap<String, Object>();
         tlm.set(ht);
      }
      return ht;
   }

   @Override
   public void setHttpRequest(HttpServletRequest req) {
      log.debug("set HttpServletRequest: " + req);
      httpRequest.set(req);
   }

   @Override
   public HttpServletRequest getHttpRequest() {
      return httpRequest.get();
   }

   @Override
   public String getApprovalUser() {
      return (String) getProperty(APPROVAL_USER);
   }

   @Override
   public void setApprovalUser(String appUser) {
      setProperty(APPROVAL_USER, appUser);
   }

   @Override
   public String getApprovalAddress() {
      return (String) getProperty(APPROVAL_ADDRESS);
   }

   @Override
   public void setApprovalAddress(String appUser) {
      setProperty(APPROVAL_ADDRESS, appUser);
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("Session context");
      for (Entry<String, Object> e : getProperties().entrySet()) {
         b.append("\n");
         b.append(e.getKey());
         b.append(" = ");
         b.append(e.getValue());
      }
      return b.toString();
   }

}
