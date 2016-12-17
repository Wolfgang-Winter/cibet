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

import com.cibethelper.base.CoreTestBase;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalRequestScope;

public class AuthenticationProviderTest extends CoreTestBase {

   @Test
   public void httpRequestNull() {
      EEAuthenticationProvider prov = new EEAuthenticationProvider();
      Assert.assertNull(prov.getUsername());
      Assert.assertEquals(AbstractAuthenticationProvider.DEFAULT_TENANT, prov.getTenant());
   }

   @Test
   public void httpRequestNoPrincipal() {
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

}
