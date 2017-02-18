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

// mappedName for Glassfish
@Stateful(name = "com.cibethelper.ejb.CibetTestStatefulEJB")
@Remote
@Interceptors(CibetInterceptor.class)
public class CibetTestStatefulEJB implements CibetTest2EJB {

   private static Logger log = Logger.getLogger(CibetTestStatefulEJB.class);

   public List<Object> testInvoke2(String str1) {
      throw new RuntimeException("This is a artificial Exception invoked");
   }

   public String longCalculation(int loops) {
      return null;
   }

}
