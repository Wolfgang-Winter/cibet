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
package com.logitags.cibet.authentication;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.config.Configuration;

public class ChainedAuthenticationProvider extends AbstractAuthenticationProvider {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private static Log log = LogFactory.getLog(Configuration.class);

   private List<AuthenticationProvider> providerChain = new LinkedList<>();

   public ChainedAuthenticationProvider() {
   }

   public ChainedAuthenticationProvider(ChainedAuthenticationProvider chprov) {
      providerChain = new LinkedList<>(chprov.getProviderChain());
   }

   @Override
   public String getUsername() {
      for (AuthenticationProvider provider : providerChain) {
         String username = provider.getUsername();
         if (username != null) {
            log.debug(provider.getClass().getSimpleName() + " resolves user " + username);
            return username;
         }
      }
      log.debug("ChainedAuthenticationProvider resolves user null");
      return null;
   }

   @Override
   public String getTenant() {
      for (AuthenticationProvider provider : providerChain) {
         String tenant = provider.getTenant();
         if (!DEFAULT_TENANT.equals(tenant)) {
            log.debug(provider.getClass().getSimpleName() + " resolves tenant " + tenant);
            return tenant;
         }
      }
      log.debug("ChainedAuthenticationProvider resolves default tenant " + DEFAULT_TENANT);
      return DEFAULT_TENANT;
   }

   @Override
   public String getUserAddress() {
      for (AuthenticationProvider provider : providerChain) {
         if (provider.getUserAddress() != null) {
            log.debug(provider.getClass().getSimpleName() + " resolves user address " + provider.getUserAddress());
            return provider.getUserAddress();
         }
      }
      return null;
   }

   public List<AuthenticationProvider> getProviderChain() {
      return providerChain;
   }

   @Override
   public SecurityContext initSecurityContext(String securityContextInfo) {
      for (AuthenticationProvider provider : providerChain) {
         SecurityContext secCtx = provider.initSecurityContext(securityContextInfo);
         if (secCtx != null && secCtx.isInitialised()) {
            log.debug(provider.getClass().getSimpleName() + " inits " + secCtx);
            return secCtx;
         }
      }

      return null;
   }

   @Override
   public void stopSecurityContext(SecurityContext secCtx) {
      for (AuthenticationProvider provider : providerChain) {
         provider.stopSecurityContext(secCtx);
      }
   }

   @Override
   public String createSecurityContextHeader() {
      StringBuffer b = new StringBuffer();
      for (AuthenticationProvider provider : providerChain) {
         String secCtx = provider.createSecurityContextHeader();
         if (secCtx != null) {
            if (b.length() > 0) {
               b.append("&");
            }
            b.append(secCtx);
         }
      }
      return b.toString();
   }

}
