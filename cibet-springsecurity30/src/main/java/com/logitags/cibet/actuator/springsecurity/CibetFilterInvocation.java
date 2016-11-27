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

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.security.web.FilterInvocation;

public class CibetFilterInvocation extends FilterInvocation {

   public static final String URLACCESS_RULE = "urlAccess";

   private String accessRule;

   private String accessRuleExpression;

   public CibetFilterInvocation(ServletRequest request,
         ServletResponse response, FilterChain chain) {
      super(request, response, chain);
   }

   /**
    * @return the accessRule
    */
   public String getAccessRule() {
      return accessRule;
   }

   /**
    * @param accessRule
    *           the accessRule to set
    */
   public void setAccessRule(String accessRule) {
      this.accessRule = accessRule;
   }

   /**
    * @return the accessRuleExpression
    */
   public String getAccessRuleExpression() {
      return accessRuleExpression;
   }

   /**
    * @param accessRuleExpression
    *           the accessRuleExpression to set
    */
   public void setAccessRuleExpression(String accessRuleExpression) {
      this.accessRuleExpression = accessRuleExpression;
   }

}
