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

import java.security.Key;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This implementation creates a SHA256 MessageDigest. For encryption it uses a
 * AES algorithm.
 * 
 * @author Wolfgang
 * 
 */
public class DefaultSecurityProvider implements SecurityProvider {

   private Log log = LogFactory.getLog(DefaultSecurityProvider.class);

   private String currentSecretKey = "1";

   private Map<String, String> secrets = new HashMap<String, String>();

   public DefaultSecurityProvider() {
      secrets.put("1", "2366Au37nBB.0ya?");
   }

   @Override
   public String createMessageDigest(String input, String secretId) {
      if (input == null) return null;
      String secret = secrets.get(secretId);
      if (secret == null) {
         String err = "MessageDigest creation error: No secret registered under ID "
               + secretId;
         log.error(err);
         throw new RuntimeException(err);
      }

      input = input + secret;
      String checksum = DigestUtils.sha256Hex(input);
      log.debug("created MessageDigest: " + checksum);
      return checksum;
   }

   @Override
   public byte[] encrypt(byte[] input) {
      if (input == null) return null;

      String secretKey = secrets.get(getCurrentSecretKey());
      if (secretKey == null) {
         String err = "Encryption error: No secret key defined for key reference "
               + getCurrentSecretKey();
         log.error(err);
         throw new RuntimeException(err);
      }

      try {
         Key aesKey = new SecretKeySpec(secretKey.getBytes("UTF-8"), "AES");
         Cipher cipher = Cipher.getInstance("AES");
         cipher.init(Cipher.ENCRYPT_MODE, aesKey);
         byte[] encrypted = cipher.doFinal(input);
         return encrypted;
      } catch (Exception e) {
         log.error(e.getMessage(), e);
         throw new RuntimeException(e);
      }
   }

   @Override
   public byte[] decrypt(byte[] input, String secretId) {
      if (input == null) return null;

      String secretKey = secrets.get(secretId);
      if (secretKey == null) {
         String err = "Encryption error: No secret key defined for key reference "
               + secretId;
         log.error(err);
         throw new RuntimeException(err);
      }

      try {
         Key aesKey = new SecretKeySpec(secretKey.getBytes("UTF-8"), "AES");
         Cipher cipher = Cipher.getInstance("AES");
         cipher.init(Cipher.DECRYPT_MODE, aesKey);
         byte[] decrypted = cipher.doFinal(input);
         return decrypted;
      } catch (Exception e) {
         log.error(e.getMessage(), e);
         throw new RuntimeException(e);
      }
   }

   @Override
   public String getCurrentSecretKey() {
      if (currentSecretKey == null) {
         if (secrets.size() == 1) {
            currentSecretKey = secrets.keySet().iterator().next();
         }
      }
      if (currentSecretKey == null) {
         String err = "No current secret key ID configured in "
               + this.getClass().getName();
         log.error(err);
         throw new RuntimeException(err);
      }

      return currentSecretKey;
   }

   /**
    * @param currentSecretKey
    *           the currentSecretKey to set
    */
   public void setCurrentSecretKey(String currentSecretKey) {
      this.currentSecretKey = currentSecretKey;
   }

   /**
    * Returns a map with the key id (key reference) as key and the secret as
    * value.
    * 
    * @return the secrets
    */
   public Map<String, String> getSecrets() {
      return secrets;
   }

   /**
    * Sets a map with the key id (key reference) as key and the secret as value
    * 
    * @param secrets
    *           the secrets to set
    */
   public void setSecrets(Map<String, String> secrets) {
      this.secrets = secrets;
   }

}
