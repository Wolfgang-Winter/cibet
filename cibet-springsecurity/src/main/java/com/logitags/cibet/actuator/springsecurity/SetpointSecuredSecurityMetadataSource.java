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
/**
 * 
 */
package com.logitags.cibet.actuator.springsecurity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.access.method.MethodSecurityMetadataSource;

/**
 *
 */
public class SetpointSecuredSecurityMetadataSource implements
      MethodSecurityMetadataSource {

   private static Log log = LogFactory
         .getLog(SetpointSecuredSecurityMetadataSource.class);

   private static MethodSecurityMetadataSource instance;

   public static synchronized MethodSecurityMetadataSource instance() {
      if (instance == null) {
         instance = new SetpointSecuredSecurityMetadataSource();
      }
      return instance;
   }

   protected SetpointSecuredSecurityMetadataSource() {
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * org.springframework.security.access.method.MethodSecurityMetadataSource
    * #getAttributes(java.lang.reflect.Method, java.lang.Class)
    */
   public Collection<ConfigAttribute> getAttributes(Method arg0, Class<?> arg1) {
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @seeorg.springframework.security.access.SecurityMetadataSource#
    * getAllConfigAttributes()
    */
   public Collection<ConfigAttribute> getAllConfigAttributes() {
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * org.springframework.security.access.SecurityMetadataSource#getAttributes
    * (java.lang.Object)
    */
   public Collection<ConfigAttribute> getAttributes(Object obj)
         throws IllegalArgumentException {
      CibetMethodInvocation mi = (CibetMethodInvocation) obj;
      if (mi.getRule(CibetMethodInvocation.SECURED_RULE) == null) {
         // There is no meta-data so return
         log.debug("No Secured expression annotation found");
         return null;
      }

      return createConfigAttributeList(mi
            .getRule(CibetMethodInvocation.SECURED_RULE));
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * org.springframework.security.access.SecurityMetadataSource#supports(java
    * .lang.Class)
    */
   public boolean supports(Class<?> cl) {
      return MethodInvocation.class.isAssignableFrom(cl);
   }

   protected Collection<ConfigAttribute> createConfigAttributeList(String rule) {
      Collection<ConfigAttribute> list = new ArrayList<ConfigAttribute>();

      rule = rule.replaceAll("[\"'\\{\\}]", "");
      StringTokenizer tok = new StringTokenizer(rule, ",");
      while (tok.hasMoreTokens()) {
         String token = tok.nextToken().trim();
         list.add(new SecurityConfig(token));
      }
      return list;
   }

}
