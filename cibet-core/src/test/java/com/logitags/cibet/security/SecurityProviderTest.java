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
package com.logitags.cibet.security;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.logitags.cibet.security.DefaultSecurityProvider;

public class SecurityProviderTest {

   private static Logger log = Logger.getLogger(SecurityProviderTest.class);

   @Test
   public void createMessageDigest() {
      log.debug("start createMessageDigest");

      DefaultSecurityProvider sec = new DefaultSecurityProvider();
      try {
         sec.createMessageDigest("", "2");
      } catch (RuntimeException e) {
         Assert.assertEquals(
               "MessageDigest creation error: No secret registered under ID 2",
               e.getMessage());
      }
   }

   @Test
   public void encrypt() {
      log.debug("start encrypt");

      DefaultSecurityProvider sec = new DefaultSecurityProvider();
      sec.setCurrentSecretKey("22");
      try {
         sec.encrypt(new byte[0]);
      } catch (RuntimeException e) {
         Assert.assertEquals(
               "Encryption error: No secret key defined for key reference 22",
               e.getMessage());
      }
   }

   @Test
   public void decrypt() {
      log.debug("start decrypt");

      DefaultSecurityProvider sec = new DefaultSecurityProvider();
      try {
         sec.decrypt(new byte[0], "33");
      } catch (RuntimeException e) {
         Assert.assertEquals(
               "Encryption error: No secret key defined for key reference 33",
               e.getMessage());
      }
   }

   @Test
   public void getCurrentSecretKey() {
      log.debug("start getCurrentSecretKey");

      DefaultSecurityProvider sec = new DefaultSecurityProvider();
      sec.setCurrentSecretKey(null);
      Assert.assertEquals("1", sec.getCurrentSecretKey());
   }

}
