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

/**
 * An interface to provide user name of the logged in user and tenant in a tenant- specific application-
 * 
 * @author Wolfgang
 * 
 */
public interface AuthenticationProvider {

   /**
    * return the user name of the logged in user. Returns null if no logged in user.
    * 
    * @return user name
    */
   String getUsername();

   /**
    * return the current tenant if any, otherwise null.
    * 
    * @return tenant
    */
   String getTenant();

   /**
    * return the address of the logged in user. This could be an email address or an sms address or anything that can be
    * used to notify the user during dual control processes. The address must be understood by the Notification
    * implementation.
    * 
    * @return user address
    */
   String getUserAddress();

   /**
    * initialise the security context for authentication and authorisation. This method is called when a security
    * context must be propagated from one thread to another one, see CibetProxy. It depends on the used security
    * framework and its AuthenticationProvider implementation how this is achieved.
    * 
    * @param securityContextInfo
    *           the header from a request which could include information about the security context.
    * @return SecurityContext
    */
   SecurityContext initSecurityContext(String securityContextInfo);

   /**
    * stops the security context for authentication and authorisation in the current thread and releases any resources.
    * The parameter could be null.
    * 
    * @param secCtx
    *           security context
    */
   void stopSecurityContext(SecurityContext secCtx);

   /**
    * creates an HTTP header that contains the security information depending on the used AuthenticationProvider
    * implementation.
    * 
    * @return header
    */
   String createSecurityContextHeader();

}
