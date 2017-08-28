/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2012 Dr. Wolfgang Winter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************
 */
package com.logitags.cibet.sensor.pojo;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import com.logitags.cibet.actuator.common.Actuator;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.control.Controller;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.resource.ParameterSequenceComparator;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.common.Invoker;
import com.logitags.cibet.sensor.ejb.CibetInterceptor;
import com.logitags.cibet.sensor.ejb.EJBInvoker;
import com.logitags.cibet.sensor.ejb.EjbResource;

public abstract class AbstractAspect extends CibetInterceptor {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private Log log = LogFactory.getLog(AbstractAspect.class);

   private static final String SENSOR_NAME_POJO = "ASPECT";

   protected Object doIntercept(ProceedingJoinPoint thisJoinPoint, Class<? extends Invoker> factoryClass, String param)
         throws Throwable {
      String methodName = thisJoinPoint.getSignature().getName();
      Method method = ((MethodSignature) thisJoinPoint.getSignature()).getMethod();

      Object invokedObject = null;
      if (thisJoinPoint.getTarget() == null) {
         // static method
         if (method.getReturnType().isAssignableFrom(method.getDeclaringClass())
               && Modifier.isStatic(method.getModifiers())) {
            // Singleton method, do not control
            log.debug(
                  "no control of Singleton " + method.getDeclaringClass().getName() + "." + method.getName() + "()");
            return thisJoinPoint.proceed();
         }

         invokedObject = method.getDeclaringClass();
      } else {
         invokedObject = thisJoinPoint.getTarget();
      }

      boolean startManaging = true;
      EventMetadata metadata = null;
      EventResult thisResult = null;
      try {
         startManaging = Context.start();

         ControlEvent controlEvent = controlEvent();
         log.debug("controlEvent: " + controlEvent + " on method " + methodName);
         if (log.isDebugEnabled()) {
            log.debug("control " + controlEvent + " of " + method.getDeclaringClass().getName() + "." + method.getName()
                  + "() for tenant " + Context.internalSessionScope().getTenant());
            log.debug("interceptor param=" + param + ", factoryClass=" + factoryClass);
         }

         checkParam(param);

         Set<ResourceParameter> params = new TreeSet<ResourceParameter>(new ParameterSequenceComparator());
         for (int i = 0; i < method.getParameterTypes().length; i++) {
            params.add(new ResourceParameter("PARAM" + i, method.getParameterTypes()[i].getName(),
                  thisJoinPoint.getArgs()[i], ParameterType.METHOD_PARAMETER, i));
         }

         String sensorName;
         MethodResource resource;
         if (factoryClass == EJBInvoker.class) {
            sensorName = CibetInterceptor.SENSOR_NAME;
            resource = new EjbResource(invokedObject, method, params);
         } else {
            sensorName = SENSOR_NAME_POJO;
            resource = new MethodResource(invokedObject, method, params);
         }

         resource.setInvokerClass(factoryClass.getName());
         resource.setInvokerParam(param);
         metadata = new EventMetadata(sensorName, controlEvent, resource);
         Controller.evaluate(metadata);

         thisResult = Context.internalRequestScope().registerEventResult(new EventResult(sensorName, metadata));

         try {
            for (Actuator actuator : metadata.getActuators()) {
               actuator.beforeEvent(metadata);
            }

            if (metadata.getExecutionStatus() == ExecutionStatus.EXECUTING) {
               metadata.setExecutionStatus(ExecutionStatus.EXECUTED);
               if (!Context.requestScope().isPlaying()) {
                  Object result = thisJoinPoint.proceed();
                  resource.setResultObject(result);
               }
            }

         } catch (Throwable e) {
            log.error(e.getMessage(), e);
            metadata.setExecutionStatus(ExecutionStatus.ERROR);
            Context.requestScope().setRemark(e.getMessage());
            metadata.setException(e);
         }

         for (Actuator actuator : metadata.getActuators()) {
            actuator.afterEvent(metadata);
         }

         metadata.evaluateEventExecuteStatus();
         return resource.getResultObject();

      } finally {
         doFinally(startManaging, metadata, thisResult);
      }
   }

   private void checkParam(String param) {
      if (param == null || "".equals(param)) return;
      if (param.endsWith("()")) {
         if (param.lastIndexOf(".") < 0) {
            throw new IllegalArgumentException("param attribute of @CibetIntercept annotation must be of "
                  + "format 'classname' or 'classname.methodname()'");
         }
      }
   }

}
