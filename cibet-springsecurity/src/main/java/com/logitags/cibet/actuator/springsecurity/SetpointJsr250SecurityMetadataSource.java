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
import org.springframework.security.access.annotation.Jsr250SecurityConfig;
import org.springframework.security.access.method.MethodSecurityMetadataSource;

/**
 *
 */
public class SetpointJsr250SecurityMetadataSource implements MethodSecurityMetadataSource {

   private static Log log = LogFactory.getLog(SetpointJsr250SecurityMetadataSource.class);

   private String defaultRolePrefix = "ROLE_";

   private static MethodSecurityMetadataSource instance;

   public static synchronized MethodSecurityMetadataSource instance() {
      if (instance == null) {
         instance = new SetpointJsr250SecurityMetadataSource();
      }
      return instance;
   }

   protected SetpointJsr250SecurityMetadataSource() {
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.springframework.security.access.method.MethodSecurityMetadataSource
    * #getAttributes(java.lang.reflect.Method, java.lang.Class)
    */
   public Collection<ConfigAttribute> getAttributes(Method arg0, Class<?> arg1) {
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @seeorg.springframework.security.access.SecurityMetadataSource# getAllConfigAttributes()
    */
   public Collection<ConfigAttribute> getAllConfigAttributes() {
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.springframework.security.access.SecurityMetadataSource#getAttributes (java.lang.Object)
    */
   public Collection<ConfigAttribute> getAttributes(Object obj) throws IllegalArgumentException {
      CibetMethodInvocation mi = (CibetMethodInvocation) obj;
      Collection<ConfigAttribute> list = new ArrayList<ConfigAttribute>();

      if (mi.getRules().containsKey(CibetMethodInvocation.JSR250_DENYALL_RULE)) {
         list.add(Jsr250SecurityConfig.DENY_ALL_ATTRIBUTE);

      } else if (mi.getRules().containsKey(CibetMethodInvocation.JSR250_PERMITALL_RULE)) {
         list.add(Jsr250SecurityConfig.PERMIT_ALL_ATTRIBUTE);

      } else if (mi.getRules().containsKey(CibetMethodInvocation.JSR250_ROLESALLOWED_RULE)) {
         String rule = mi.getRule(CibetMethodInvocation.JSR250_ROLESALLOWED_RULE);
         rule = rule.replaceAll("[\"'\\{\\}]", "");
         StringTokenizer tok = new StringTokenizer(rule, ",");
         while (tok.hasMoreTokens()) {
            String token = tok.nextToken().trim();
            String role = getRoleWithDefaultPrefix(token);
            list.add(new Jsr250SecurityConfig(role));
         }
      }

      if (list.isEmpty()) {
         // There is no meta-data so return
         log.debug("No Jsr250 expression found");
         return null;
      }

      return list;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.springframework.security.access.SecurityMetadataSource#supports(java .lang.Class)
    */
   public boolean supports(Class<?> cl) {
      return MethodInvocation.class.isAssignableFrom(cl);
   }

   private String getRoleWithDefaultPrefix(String role) {
      if (role == null) {
         return role;
      }
      if (defaultRolePrefix == null || defaultRolePrefix.length() == 0) {
         return role;
      }
      if (role.startsWith(defaultRolePrefix)) {
         return role;
      }
      return defaultRolePrefix + role;
   }

}
