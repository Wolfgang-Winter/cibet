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
package com.logitags.cibet.security;

/**
 * Supplies methods for encryption and decryption of sensible data in Archive, DcControllable and ResourceParameter.
 * Supplies methods for creating a Message Digest.
 * 
 * @author Wolfgang
 * 
 */
public interface SecurityProvider {

   /**
    * creates a MessageDigest from the input. Uses the secret key that is registered under the secretId.
    * 
    * @param input
    * @param secretId
    * @return Message digest
    */
   String createMessageDigest(String input, String secretId);

   /**
    * returns the current valid key under which the secret/key is registered.
    * 
    * @return
    */
   String getCurrentSecretKey();

   /**
    * encrypts the input using the current key.
    * 
    * @param input
    *           unencrypted value
    * @return encrypted value
    */
   byte[] encrypt(byte[] input);

   /**
    * decrypts the input using the key that is registered under secretId.
    * 
    * @param input
    *           encrypted value
    * @param secretId
    *           secret key
    * @return unencrypted value
    */
   byte[] decrypt(byte[] input, String secretId);

}
