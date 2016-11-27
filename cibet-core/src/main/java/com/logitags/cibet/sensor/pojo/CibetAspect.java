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
package com.logitags.cibet.sensor.pojo;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 *
 */
@Aspect
public class CibetAspect extends AbstractAspect {

   @Pointcut(value = "execution(public * *.*(..)) && @within(interceptor)", argNames = "interceptor")
   void typeCibetInterceptor(
         com.logitags.cibet.sensor.pojo.CibetIntercept interceptor) {
   }

   @Pointcut(value = "execution(public * *.*(..)) && @annotation(interceptor)", argNames = "interceptor")
   void methodCibetInterceptor(
         com.logitags.cibet.sensor.pojo.CibetIntercept interceptor) {
   }

   @Around(value = "methodCibetInterceptor(interceptor)", argNames = "thisJoinPoint, interceptor")
   public Object interceptMethod(ProceedingJoinPoint thisJoinPoint,
         com.logitags.cibet.sensor.pojo.CibetIntercept interceptor)
         throws Throwable {
      return doIntercept(thisJoinPoint, interceptor.factoryClass(),
            interceptor.param());
   }

   @Around(value = "typeCibetInterceptor(interceptor)", argNames = "thisJoinPoint, interceptor")
   public Object intercept(ProceedingJoinPoint thisJoinPoint,
         com.logitags.cibet.sensor.pojo.CibetIntercept interceptor)
         throws Throwable {
      return doIntercept(thisJoinPoint, interceptor.factoryClass(),
            interceptor.param());
   }

}
