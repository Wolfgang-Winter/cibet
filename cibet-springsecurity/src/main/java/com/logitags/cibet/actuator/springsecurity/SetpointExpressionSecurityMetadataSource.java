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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.expression.method.PostCibetConfigAttribute;
import org.springframework.security.access.expression.method.PreCibetConfigAttribute;
import org.springframework.security.access.method.MethodSecurityMetadataSource;
import org.springframework.security.access.prepost.PostInvocationAttribute;
import org.springframework.security.access.prepost.PreInvocationAttribute;

/**
 *
 */
public class SetpointExpressionSecurityMetadataSource implements
      MethodSecurityMetadataSource {

   private static Log log = LogFactory
         .getLog(SetpointExpressionSecurityMetadataSource.class);

   private static Pattern valuePattern = Pattern.compile(".*value=\"(.*?)\".*");
   private static Pattern filterTargetPattern = Pattern
         .compile(".*filterTarget=\"(.*?)\".*");

   private static MethodSecurityMetadataSource instance;

   public static synchronized MethodSecurityMetadataSource instance() {
      if (instance == null) {
         instance = new SetpointExpressionSecurityMetadataSource();
      }
      return instance;
   }

   protected SetpointExpressionSecurityMetadataSource() {
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
      if (mi.getRule(CibetMethodInvocation.PREAUTHORIZE_RULE) == null
            && mi.getRule(CibetMethodInvocation.PREFILTER_RULE) == null
            && mi.getRule(CibetMethodInvocation.POSTAUTHORIZE_RULE) == null
            && mi.getRule(CibetMethodInvocation.POSTFILTER_RULE) == null) {
         // There is no meta-data so return
         log.debug("No expression annotations found");
         return null;
      }

      Collection<ConfigAttribute> list = new ArrayList<ConfigAttribute>();

      ConfigAttribute preAttr = createPreInvocationAttribute(
            mi.getRule(CibetMethodInvocation.PREFILTER_RULE),
            mi.getRule(CibetMethodInvocation.PREAUTHORIZE_RULE));
      list.add(preAttr);

      ConfigAttribute postAttr = createPostInvocationAttribute(
            mi.getRule(CibetMethodInvocation.POSTFILTER_RULE),
            mi.getRule(CibetMethodInvocation.POSTAUTHORIZE_RULE));
      if (postAttr != null) {
         list.add(postAttr);
      }

      return list.isEmpty() ? null : list;
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

   protected PreInvocationAttribute createPreInvocationAttribute(
         String preFilter, String preAuthorize) {
      try {
         log.debug("createPreInvocationAttribute from " + preFilter + " and "
               + preAuthorize);
         String preAuthValue = parseValueParameter(preAuthorize);
         String preFilterValue = parseValueParameter(preFilter);
         String preFilterTarget = parseFilterTargetParameter(preFilter);

         ExpressionParser parser = new SpelExpressionParser();
         Expression preAuthorizeExpression = preAuthorize == null ? parser
               .parseExpression("permitAll") : parser
               .parseExpression(preAuthValue);
         log.debug(preAuthorizeExpression);

         Expression preFilterExpression = preFilter == null ? null : parser
               .parseExpression(preFilterValue);

         return new PreCibetConfigAttribute(preFilterExpression,
               preFilterTarget, preAuthorizeExpression);
      } catch (ParseException e) {
         throw new IllegalArgumentException("Failed to parse expression '"
               + e.getExpressionString() + "'", e);
      }
   }

   protected PostInvocationAttribute createPostInvocationAttribute(
         String postFilter, String postAuthorize) {
      try {
         log.debug("createPostInvocationAttribute from filter " + postFilter
               + " and rule " + postAuthorize);
         String postAuthValue = parseValueParameter(postAuthorize);
         String postFilterValue = parseValueParameter(postFilter);

         ExpressionParser parser = new SpelExpressionParser();
         Expression postAuthorizeExpression = postAuthorize == null ? null
               : parser.parseExpression(postAuthValue);
         Expression postFilterExpression = postFilter == null ? null : parser
               .parseExpression(postFilterValue);

         if (postFilterExpression != null || postAuthorizeExpression != null) {
            return new PostCibetConfigAttribute(postFilterExpression,
                  postAuthorizeExpression);
         }
      } catch (ParseException e) {
         throw new IllegalArgumentException("Failed to parse expression '"
               + e.getExpressionString() + "'", e);
      }

      return null;
   }

   private String parseValueParameter(String token) {
      if (token == null) return null;
      Matcher m = valuePattern.matcher(token);
      if (m.matches()) {
         return m.group(1);
      } else {
         return token;
      }
   }

   private String parseFilterTargetParameter(String token) {
      if (token == null) return null;
      Matcher m = filterTargetPattern.matcher(token);
      if (m.matches()) {
         return m.group(1);
      } else {
         return "";
      }
   }

}
