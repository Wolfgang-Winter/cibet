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
package com.logitags.cibet.authentication;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.ShiroException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;
import org.apache.shiro.util.ThreadState;

import com.logitags.cibet.context.Context;

/**
 * This implementation retrieves the user name from Apache Shiro Subject object.
 * 
 * @author Wolfgang
 * 
 */
public class ShiroAuthenticationProvider extends AbstractAuthenticationProvider {

   /**
    * 
    */
   private static final long serialVersionUID = 1453508795058244261L;
   private static Log log = LogFactory.getLog(ShiroAuthenticationProvider.class);

   @Override
   public String getUsername() {
      String username = null;
      Subject currentUser;
      try {
         currentUser = SecurityUtils.getSubject();
         if (currentUser.isAuthenticated()) {
            username = currentUser.getPrincipal() == null ? null : currentUser.getPrincipal().toString();
         }

      } catch (ShiroException e) {
         log.warn(e.getMessage());
      }

      return username;
   }

   @Override
   public SecurityContext initSecurityContext(String securityContextInfo) {
      if (securityContextInfo == null || !securityContextInfo.startsWith(this.getClass().getSimpleName())) {
         return new SecurityContext(false);
      }
      String ssid = securityContextInfo.substring(this.getClass().getSimpleName().length());
      if (ssid != null) {
         Subject subject = new Subject.Builder().sessionId(ssid).buildSubject();
         if (subject.getPrincipal() != null) {
            Context.sessionScope().setUser(subject.getPrincipal().toString());
            ThreadState threadState = new SubjectThreadState(subject);
            threadState.bind();
            log.debug("init Shiro security context for user " + subject.getPrincipal().toString());
            return new SecurityContext(true, threadState);
         }
      }

      return new SecurityContext(false);
   }

   @Override
   public void stopSecurityContext(SecurityContext secCtx) {
      if (secCtx != null && secCtx.getAnyContext() instanceof ThreadState) {
         ((ThreadState) secCtx.getAnyContext()).clear();
      }
   }

   @Override
   public String createSecurityContextHeader() {
      Subject currentUser = SecurityUtils.getSubject();
      String ssid = currentUser.getSession().getId().toString();
      try {
         return Context.PARAM_SECURITYCONTEXT + this.getClass().getSimpleName() + "="
               + URLEncoder.encode(this.getClass().getSimpleName() + ssid, "UTF-8");
      } catch (UnsupportedEncodingException e) {
         log.error(e.getMessage(), e);
         throw new RuntimeException(e);
      }
   }

}
