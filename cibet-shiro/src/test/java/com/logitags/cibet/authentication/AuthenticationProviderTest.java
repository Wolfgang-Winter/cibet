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

import com.cibethelper.base.CoreTestBase;
import com.logitags.cibet.context.Context;

public class AuthenticationProviderTest extends CoreTestBase {

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

}
