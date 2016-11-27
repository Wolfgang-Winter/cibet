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

import java.util.List;

import com.logitags.cibet.resource.ResourceParameter;

/**
 * interface to execute the event of an Archive or DcControllable.
 */
public interface Invoker {

   /**
    * 
    * @param parameter
    *           parameter which is necessary for construction of the object, e.g. constructor parameters. Optional
    * @param targetType
    *           target type
    * @param methodName
    *           method name
    * @param parameters
    *           parameters
    * @return result or null
    * @throws Exception
    *            in case of error
    */
   Object execute(String parameter, String targetType, String methodName, List<ResourceParameter> parameters)
         throws Exception;

}
