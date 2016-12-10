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

import java.lang.reflect.Field;
import java.security.Principal;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.Factory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.cibethelper.SpringTestAuthenticationManager;
import com.cibethelper.base.CoreTestBase;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalRequestScope;

public class AuthenticationProviderTest extends CoreTestBase {

   class MockPrincipal implements Principal {

      @Override
      public String getName() {
         return "Hans";
      }
   }

   @Before
   public void clearSecurity() {
      Subject subject = SecurityUtils.getSubject();
      subject.logout();
   }

   @BeforeClass
   public static void initShiro() throws Exception {
      Factory<SecurityManager> factory = new IniSecurityManagerFactory("classpath:shiro.init");
      SecurityManager securityManager = factory.getInstance();
      SecurityUtils.setSecurityManager(securityManager);
      Context.sessionScope().setUser(null);
   }

   protected void init(String configName) throws Exception {
      Field FILENAME = Configuration.class.getDeclaredField("CONFIGURATION_FILENAME");
      FILENAME.setAccessible(true);
      FILENAME.set(null, configName);

      Configuration.instance().initialise();
      FILENAME.set(null, "cibet-config.xml");
   }

   @Test
   public void httpRequestNull() {
      // Context.internalSessionScope().setHttpRequest(null);
      EEAuthenticationProvider prov = new EEAuthenticationProvider();
      Assert.assertNull(prov.getUsername());
      Assert.assertEquals(AbstractAuthenticationProvider.DEFAULT_TENANT, prov.getTenant());
   }

   @Test
   public void httpRequestNoPrincipal() {
      // DummyServletRequest req = new DummyServletRequest();
      // Context.internalSessionScope().setHttpRequest(req);
      EEAuthenticationProvider prov = new EEAuthenticationProvider();
      Assert.assertNull(prov.getUsername());
   }

   @Test
   public void httpRequestPrincipal() {
      Context.internalRequestScope().setProperty(InternalRequestScope.CALLER_PRINCIPAL_NAME, "Hans");
      EEAuthenticationProvider prov = new EEAuthenticationProvider();
      Assert.assertEquals("Hans", prov.getUsername());
      Context.internalRequestScope().removeProperty(InternalRequestScope.CALLER_PRINCIPAL_NAME);
   }

   @Test
   public void shiro() {
      Subject subject = SecurityUtils.getSubject();
      subject.logout();
      ShiroAuthenticationProvider prov = new ShiroAuthenticationProvider();
      Assert.assertNull(prov.getUsername());
      Assert.assertEquals(AbstractAuthenticationProvider.DEFAULT_TENANT, prov.getTenant());
   }

   @Test
   public void shiroLoggedin() {
      AuthenticationToken token = new UsernamePasswordToken("lonestarr", "vespa");
      Subject subject = SecurityUtils.getSubject();
      subject.login(token);

      ShiroAuthenticationProvider prov = new ShiroAuthenticationProvider();
      Assert.assertEquals("lonestarr", prov.getUsername());
   }

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
      Context.sessionScope().setTenant(null);
      Context.sessionScope().setUser(null);
      init("config_authenticationprovider.xml");
      SpringTestAuthenticationManager authManager = new SpringTestAuthenticationManager();
      Authentication request = new UsernamePasswordAuthenticationToken("test4", "test4");
      Authentication result = authManager.authenticate(request);
      SecurityContextHolder.getContext().setAuthentication(result);

      Assert.assertEquals("test4", Context.sessionScope().getUser());
   }

}
