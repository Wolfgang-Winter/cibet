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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.web.access.expression.CibetWebExpressionConfigAttribute;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;

public class CibetFilterInvocationSecurityMetadataSource extends
      SetpointSecuredSecurityMetadataSource implements
      FilterInvocationSecurityMetadataSource {

   private static Log log = LogFactory
         .getLog(CibetFilterInvocationSecurityMetadataSource.class);

   private FilterInvocationSecurityMetadataSource originalMetadataSource;

   public CibetFilterInvocationSecurityMetadataSource(
         FilterInvocationSecurityMetadataSource s) {
      originalMetadataSource = s;
   }

   @Override
   public Collection<ConfigAttribute> getAllConfigAttributes() {
      return originalMetadataSource.getAllConfigAttributes();
   }

   @Override
   public Collection<ConfigAttribute> getAttributes(Object obj)
         throws IllegalArgumentException {
      if (obj instanceof CibetFilterInvocation) {
         CibetFilterInvocation fi = (CibetFilterInvocation) obj;
         return getCibetAttributes(fi);
      } else {
         return originalMetadataSource.getAttributes(obj);
      }
   }

   @Override
   public boolean supports(Class<?> clazz) {
      return originalMetadataSource.supports(clazz);
   }

   /**
    * @return the originalMetadataSource
    */
   public FilterInvocationSecurityMetadataSource getOriginalMetadataSource() {
      return originalMetadataSource;
   }

   protected Collection<ConfigAttribute> getCibetAttributes(
         CibetFilterInvocation fi) {
      if (fi.getAccessRule() != null) {
         // SecurityConfig
         return createConfigAttributeList(fi.getAccessRule());
      } else {
         // Expression
         ExpressionParser parser = new SpelExpressionParser();
         try {
            Expression ex = parser
                  .parseExpression(fi.getAccessRuleExpression());
            Collection<ConfigAttribute> list = new ArrayList<ConfigAttribute>();
            list.add(new CibetWebExpressionConfigAttribute(ex));
            log.debug("parsed expression: " + ex);
            return list;
         } catch (ParseException e) {
            throw new IllegalArgumentException("Failed to parse expression '"
                  + fi.getAccessRuleExpression() + "'");
         }
      }
   }

}
