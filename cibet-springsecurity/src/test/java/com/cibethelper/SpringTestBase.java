/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2016 Dr. Wolfgang Winter
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
package com.cibethelper;

import java.lang.reflect.Field;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.SecurityMetadataSource;
import org.springframework.security.access.intercept.aopalliance.MethodSecurityInterceptor;
import org.springframework.security.access.method.MethodSecurityMetadataSource;
import org.springframework.security.access.prepost.PreInvocationAuthorizationAdvice;
import org.springframework.security.access.prepost.PreInvocationAuthorizationAdviceVoter;
import org.springframework.security.access.vote.AbstractAccessDecisionManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import com.cibethelper.base.CoreTestBase;
import com.logitags.cibet.context.Context;

public abstract class SpringTestBase extends CoreTestBase {

   private static Logger log = Logger.getLogger(SpringTestBase.class);

   @Before
   public void before() {
      log.info("BEFORE TEST");
      Context.start();
      initConfiguration("x");
   }

   @After
   public void after() {
      log.info("AFTER TEST");
      Context.internalRequestScope().setRollbackOnly(true);
      Context.end();
      SecurityContextHolder.getContext().setAuthentication(null);
   }

   protected void authenticate(String... roles) throws AuthenticationException {
      SpringTestAuthenticationManager authManager = new SpringTestAuthenticationManager();
      for (String role : roles) {
         if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
         }
         authManager.addAuthority(role);
      }

      Authentication request = new UsernamePasswordAuthenticationToken("test", "test");
      Authentication result = authManager.authenticate(request);
      SecurityContextHolder.getContext().setAuthentication(result);
   }

   protected ApplicationContext initContext(String configFile) {
      ApplicationContext context = new ClassPathXmlApplicationContext(new String[] { configFile });
      log.debug(context.getBeanDefinitionCount() + " beans in context");
      return context;
   }

   protected void listBeansInContext(ApplicationContext context) throws Exception {
      log.debug(context.getBeanDefinitionCount() + " beans in context");
      String[] names = context.getBeanDefinitionNames();
      for (String name : names) {
         log.debug("Bean: " + name + ", class: " + context.getBean(name));
         // String[] aliases = context.getAliases(name);
         // log.debug("Aliases:");
         // for (String alias : aliases) {
         // log.debug(alias);
         // }
         // log.debug("\n");
      }
      MethodSecurityInterceptor interceptor = (MethodSecurityInterceptor) context
            .getBean(MethodSecurityInterceptor.class);
      SecurityMetadataSource source = interceptor.getSecurityMetadataSource();
      AbstractAccessDecisionManager decManager = (AbstractAccessDecisionManager) interceptor.getAccessDecisionManager();
      log.debug("decisionManager: " + decManager);
      log.debug("AfterInvocationManager: " + interceptor.getAfterInvocationManager());
      log.debug("AuthenticationManager: " + interceptor.getAuthenticationManager());
      log.debug("RunAsManager: " + interceptor.getRunAsManager());
      log.debug("SecureObjectClass: " + interceptor.getSecureObjectClass());
      log.debug("SecurityMetadataSource: " + source);
      log.debug("AllConfigAttributes size: " + source.getAllConfigAttributes().size());
      log.debug("methodSecurityMetadataSources in DelegatingMethodSecurityMetadataSource:");
      Field f1 = source.getClass().getDeclaredField("methodSecurityMetadataSources");
      f1.setAccessible(true);
      List<MethodSecurityMetadataSource> list = (List<MethodSecurityMetadataSource>) f1.get(source);
      for (MethodSecurityMetadataSource msms : list) {
         log.debug(msms.getClass().getName());
      }

      log.debug("Voters in DecisionManager:");
      for (AccessDecisionVoter voter : decManager.getDecisionVoters()) {
         log.debug(voter.getClass().getName());
         if ("org.springframework.security.access.prepost.PreInvocationAuthorizationAdviceVoter"
               .equals(voter.getClass().getName())) {
            PreInvocationAuthorizationAdviceVoter preVot = (PreInvocationAuthorizationAdviceVoter) voter;
            Field f = preVot.getClass().getDeclaredField("preAdvice");
            f.setAccessible(true);
            PreInvocationAuthorizationAdvice advice = (PreInvocationAuthorizationAdvice) f.get(preVot);
            log.debug("PreInvocationAuthorizationAdvice class: " + advice.getClass().getName());
         }
      }
   }

}
