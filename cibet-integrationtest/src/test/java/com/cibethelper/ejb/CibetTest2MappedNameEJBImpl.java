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
import java.util.Random;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;

import org.apache.log4j.Logger;

import com.logitags.cibet.sensor.ejb.CibetInterceptor;

@Stateless(name = "xx", mappedName = "CibetTest2EJBImpl_w_m")
@Remote
@Interceptors(CibetInterceptor.class)
public class CibetTest2MappedNameEJBImpl implements CibetTest2EJB {

   private static Logger log = Logger.getLogger(CibetTest2MappedNameEJBImpl.class);

   public List<Object> testInvoke2(String str1) {
      throw new RuntimeException("This is a artificial Exception invoked");
   }

   @Override
   public String longCalculation(int loops) {
      log.info("start longCalculation");
      long start = System.currentTimeMillis();
      Random rnd = new Random(start);
      for (int i = 1; i < loops; i++) {
         double d = rnd.nextDouble();
         Math.atan(Math.atan(Math.atan(Math.atan(Math.atan(Math.atan(Math.atan(Math.atan(Math.atan(d)))))))));
      }

      long end = System.currentTimeMillis();
      long duration = end - start;
      log.info("duration: " + duration);
      return "DURATIONRESULT=" + String.valueOf(duration);
   }

}
