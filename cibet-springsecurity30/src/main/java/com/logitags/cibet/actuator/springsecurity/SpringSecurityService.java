/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 *******************************************************************************
 */
package com.logitags.cibet.actuator.springsecurity;

import java.io.IOException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalSessionScope;
import com.logitags.cibet.core.CibetUtil;

/**
 * Default implementation of the interface
 */
@Component
// @CibetContext
public class SpringSecurityService implements ApplicationContextAware {

   private static Log log = LogFactory.getLog(SpringSecurityService.class);

   private ApplicationContext context;

   /**
    * authenticates a second user while the main user is already authenticated. The authentication information for the
    * second user are not stored in the security context but in Cibet context. A second authentication is necessary for
    * the Two-man-rule actuator.
    * 
    * @param auth
    *           Credentials of the second user
    * @throws AuthenticationException
    *            in case of error
    */
   public void logonSecondUser(Authentication auth) throws AuthenticationException {
      try {
         AuthenticationManager authManager = context.getBean(ProviderManager.class);
         Authentication result = authManager.authenticate(auth);
         Context.internalSessionScope().setSecondUser(result.getName());
         Context.internalSessionScope().setProperty(InternalSessionScope.SECOND_PRINCIPAL, result);
         if (log.isDebugEnabled()) {
            log.debug("User " + result.getName() + " is successfully authenticated");
         }
      } catch (NoSuchBeanDefinitionException e1) {
         String msg = "Failed to authenticate second user: "
               + "Failed to find a ProviderManager bean in Spring context. Configure Spring context correctly: "
               + e1.getMessage();
         log.error(msg);
         throw new RuntimeException(msg, e1);
      }
   }

   /**
    * removes the authentication credentials of the second user from Cibet context.
    */
   public void logoffSecondUser() {
      Context.internalSessionScope().setSecondUser(null);
      Context.internalSessionScope().setProperty(InternalSessionScope.SECOND_PRINCIPAL, null);
   }

   @Override
   public void setApplicationContext(ApplicationContext ctx) throws BeansException {
      context = ctx;
   }

   /**
    * encodes the Spring SecurityContext into a Base64 String.
    * 
    * @return security context
    */
   public String securityContext() {
      SecurityContext sc = SecurityContextHolder.getContext();
      try {
         byte[] bytes = CibetUtil.encode(sc);
         return Base64.encodeBase64String(bytes);
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new RuntimeException(e);
      }
   }

}
