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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.logitags.cibet.sensor.common.Invoker;

/**
 *
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
@Target(value = { ElementType.TYPE, ElementType.METHOD })
public @interface CibetIntercept {
   Class<? extends Invoker> factoryClass() default PojoInvoker.class;

   String param() default "";
}
