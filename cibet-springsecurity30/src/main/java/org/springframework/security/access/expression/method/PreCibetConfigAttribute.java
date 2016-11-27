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
package org.springframework.security.access.expression.method;

import org.springframework.expression.Expression;

public class PreCibetConfigAttribute extends PreInvocationExpressionAttribute {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1976045857367080771L;

	public PreCibetConfigAttribute(Expression filterExpression,
			String filterTarget, Expression authorizeExpression) {
		super(filterExpression, filterTarget, authorizeExpression);
	}

}
