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

package com.logitags.cibet.sensor.common;

import java.util.Set;

import com.logitags.cibet.resource.ResourceParameter;

/**
 * interface to execute the event of an Archive or Controllable.
 */
public interface Invoker {

   /**
    * 
    * @param parameter
    *           parameter which is necessary for construction of the object, e.g. constructor parameters. Optional
    * @param target
    *           target
    * @param methodName
    *           method name
    * @param parameters
    *           parameters
    * @return result or null
    * @throws Exception
    *            in case of error
    */
   Object execute(String parameter, String target, String methodName, Set<ResourceParameter> parameters)
         throws Exception;

}
