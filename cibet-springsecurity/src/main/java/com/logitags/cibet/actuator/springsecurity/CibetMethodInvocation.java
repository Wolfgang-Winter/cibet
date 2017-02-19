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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.aopalliance.intercept.MethodInvocation;

/**
 *
 */
public class CibetMethodInvocation implements MethodInvocation {

	public static final String PREAUTHORIZE_RULE = "PreAuthorize";
	public static final String PREFILTER_RULE = "PreFilter";
	public static final String POSTAUTHORIZE_RULE = "PostAuthorize";
	public static final String POSTFILTER_RULE = "PostFilter";
	public static final String SECURED_RULE = "Secured";

	public static final String JSR250_DENYALL_RULE = "DenyAll";
	public static final String JSR250_PERMITALL_RULE = "PermitAll";
	public static final String JSR250_ROLESALLOWED_RULE = "RolesAllowed";

	private Object target;

	private String setpointFingerprint;

	private Object result;

	private Method method;

	private Object[] arguments;

	private Map<String, String> rules = new HashMap<String, String>();

	public CibetMethodInvocation(Object t, Method m, Object[] args, String sf,
	      Object r) {
		target = t;
		method = m;
		arguments = args == null ? new Object[0] : args;
		setpointFingerprint = sf;
		result = r;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.aopalliance.intercept.MethodInvocation#getMethod()
	 */
	public Method getMethod() {
		return method;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.aopalliance.intercept.Invocation#getArguments()
	 */
	public Object[] getArguments() {
		return arguments;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.aopalliance.intercept.Joinpoint#getStaticPart()
	 */
	public AccessibleObject getStaticPart() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.aopalliance.intercept.Joinpoint#getThis()
	 */
	public Object getThis() {
		return target;
	}

	/**
	 * does not execute the method. Returns always null.
	 * 
	 * @see org.aopalliance.intercept.Joinpoint#proceed()
	 */
	public Object proceed() throws Throwable {
		return result;
	}

	/**
	 * @return the result
	 */
	public Object getResult() {
		return result;
	}

	/**
	 * @return the setpointFingerprint
	 */
	public String getSetpointFingerprint() {
		return setpointFingerprint;
	}

	public void addRule(String key, String rule) {
		rules.put(key, rule);
	}

	public String getRule(String key) {
		return rules.get(key);
	}

	/**
	 * @return the rules
	 */
	public Map<String, String> getRules() {
		return rules;
	}

}
