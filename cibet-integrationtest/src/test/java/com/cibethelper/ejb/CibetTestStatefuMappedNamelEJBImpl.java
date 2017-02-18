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

import javax.ejb.Remote;
import javax.ejb.Stateful;
import javax.interceptor.Interceptors;

import org.apache.log4j.Logger;

import com.logitags.cibet.sensor.ejb.CibetInterceptor;

@Stateful(mappedName = "CibetTestStatefulEJBImpl with mapped name")
@Remote
@Interceptors(CibetInterceptor.class)
public class CibetTestStatefuMappedNamelEJBImpl implements CibetTest2EJB {

	private static Logger log = Logger
	      .getLogger(CibetTestStatefuMappedNamelEJBImpl.class);

	public List<Object> testInvoke2(String str1) {
		throw new RuntimeException("This is a artificial Exception invoked");
	}

	@Override
	public String longCalculation(int loops) {
		// TODO Auto-generated method stub
		return null;
	}

}
