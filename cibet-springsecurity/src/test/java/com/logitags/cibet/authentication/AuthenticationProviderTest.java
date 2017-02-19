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

import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.cibethelper.SpringTestAuthenticationManager;
import com.cibethelper.base.CoreTestBase;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.context.Context;

public class AuthenticationProviderTest extends CoreTestBase {

   @Test
   public void spring() {
      SecurityContextHolder.getContext().setAuthentication(null);
      SpringSecurityAuthenticationProvider prov = new SpringSecurityAuthenticationProvider();
      Assert.assertNull(prov.getUsername());
      Assert.assertEquals(AbstractAuthenticationProvider.DEFAULT_TENANT, prov.getTenant());
   }

   @Test
   public void springLoggedIn() {
      SpringTestAuthenticationManager authManager = new SpringTestAuthenticationManager();
      Authentication request = new UsernamePasswordAuthenticationToken("test", "test");
      Authentication result = authManager.authenticate(request);
      SecurityContextHolder.getContext().setAuthentication(result);

      SpringSecurityAuthenticationProvider prov = new SpringSecurityAuthenticationProvider();
      Assert.assertEquals("test", prov.getUsername());
   }

   @Test
   public void config() {
      Context.sessionScope().setTenant(null);
      SpringTestAuthenticationManager authManager = new SpringTestAuthenticationManager();
      Authentication request = new UsernamePasswordAuthenticationToken("test3", "test3");
      Authentication result = authManager.authenticate(request);
      SecurityContextHolder.getContext().setAuthentication(result);

      Configuration.instance().registerAuthenticationProvider(new SpringSecurityAuthenticationProvider());

      Assert.assertEquals("test3", Context.sessionScope().getUser());
      Assert.assertEquals(AbstractAuthenticationProvider.DEFAULT_TENANT, Context.sessionScope().getTenant());
   }

   @Test
   public void configFromFile() throws Exception {
      try {
         Context.sessionScope().setTenant(null);
         Context.sessionScope().setUser(null);
         initConfiguration("config_authenticationprovider.xml");
         SpringTestAuthenticationManager authManager = new SpringTestAuthenticationManager();
         Authentication request = new UsernamePasswordAuthenticationToken("test4", "test4");
         Authentication result = authManager.authenticate(request);
         SecurityContextHolder.getContext().setAuthentication(result);

         Assert.assertEquals("test4", Context.sessionScope().getUser());
      } finally {
         initConfiguration("cibet-config.xml");
      }
   }

}
