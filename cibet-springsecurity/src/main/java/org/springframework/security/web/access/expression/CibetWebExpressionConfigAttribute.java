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
package org.springframework.security.web.access.expression;

import org.springframework.expression.Expression;

public class CibetWebExpressionConfigAttribute extends WebExpressionConfigAttribute {

   /**
    * 
    */
   private static final long serialVersionUID = 1268575719783709427L;

   public CibetWebExpressionConfigAttribute(Expression authorizeExpression) {
      super(authorizeExpression, null);
   }

}
