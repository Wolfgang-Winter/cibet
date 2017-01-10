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

import javax.ejb.Stateful;
import javax.interceptor.Interceptors;

import org.apache.log4j.Logger;

import com.logitags.cibet.sensor.ejb.CibetInterceptor;

// mappedName for Glassfish
//@Stateful(name = "com.logitags.cibet.helper.CibetTestStatefulEJBImpl", mappedName = "CibetTestStatefulEJBImpl_MN")
@Stateful(name = "com.cibethelper.ejb.CibetTestStatefulEJB")
@Interceptors(CibetInterceptor.class)
public class CibetTestStatefulEJB {

   private static Logger log = Logger.getLogger(CibetTestStatefulEJB.class);

   public List<Object> testInvoke2(String str1) {
      throw new RuntimeException("This is a artificial Exception invoked");
   }

   public String longCalculation(int loops) {
      return null;
   }

}
