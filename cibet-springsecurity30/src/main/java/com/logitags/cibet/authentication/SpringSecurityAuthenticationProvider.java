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

import java.io.IOException;
import java.net.URLEncoder;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.CibetUtil;

/**
 * This implementation retrieves the user name from Spring Security Authentication object.
 * 
 * @author Wolfgang
 * 
 */
public class SpringSecurityAuthenticationProvider extends AbstractAuthenticationProvider {

   /**
    * 
    */
   private static final long serialVersionUID = -1442045050695847249L;

   private static Log log = LogFactory.getLog(SpringSecurityAuthenticationProvider.class);

   @Override
   public String getUsername() {
      String username = null;
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      log.debug("authentication:" + authentication);
      if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
         username = authentication.getName();
      }
      return username;
   }

   @Override
   public SecurityContext initSecurityContext(String securityContextInfo) {
      if (securityContextInfo == null || !securityContextInfo.startsWith(this.getClass().getSimpleName())) {
         return new SecurityContext(false);
      }
      String sc = securityContextInfo.substring(this.getClass().getSimpleName().length());
      if (sc != null) {
         try {
            byte[] bytes = Base64.decodeBase64(sc);
            org.springframework.security.core.context.SecurityContext springContext = (org.springframework.security.core.context.SecurityContext) CibetUtil
                  .decode(bytes);
            SecurityContextHolder.setContext(springContext);
            log.debug("init SpringSecurity security context for user "
                  + SecurityContextHolder.getContext().getAuthentication());
            return new SecurityContext(true, springContext);
         } catch (Exception e) {
            log.info(e.getMessage(), e);
         }
      }

      return new SecurityContext(false);
   }

   @Override
   public void stopSecurityContext(SecurityContext secCtx) {
      if (secCtx != null && secCtx.getAnyContext() instanceof org.springframework.security.core.context.SecurityContext) {
         SecurityContextHolder.clearContext();
      }
   }

   @Override
   public String createSecurityContextHeader() {
      org.springframework.security.core.context.SecurityContext sc = SecurityContextHolder.getContext();
      try {
         byte[] bytes = CibetUtil.encode(sc);
         return Context.PARAM_SECURITYCONTEXT + this.getClass().getSimpleName() + "="
               + URLEncoder.encode(this.getClass().getSimpleName() + Base64.encodeBase64String(bytes), "UTF-8");
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new RuntimeException(e);
      }
   }

}
