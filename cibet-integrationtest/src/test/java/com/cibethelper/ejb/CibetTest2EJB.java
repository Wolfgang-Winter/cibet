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
package com.cibethelper.ejb;

import java.util.List;

public interface CibetTest2EJB {

	List<Object> testInvoke2(String str1);

	String longCalculation(int loops);
}
