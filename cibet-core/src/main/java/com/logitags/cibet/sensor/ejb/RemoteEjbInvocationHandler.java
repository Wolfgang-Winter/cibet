/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2014 Dr. Wolfgang Winter
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
package com.logitags.cibet.sensor.ejb;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Set;
import java.util.TreeSet;

import javax.naming.Name;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.common.Actuator;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.resource.ParameterSequenceComparator;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.ResourceParameter;

public class RemoteEjbInvocationHandler extends CibetInterceptor implements InvocationHandler {

   private Log log = LogFactory.getLog(RemoteEjbInvocationHandler.class);

   private static final String SENSOR_NAME = "EJB-CLIENT";

   public static final String JNDI_CONTEXT = "__JNDI_CONTEXT";
   public static final String JNDI_NAME = "__JNDI_NAME";

   private Object originalProxy;

   private Hashtable<?, ?> environment;

   private String strName;

   private Name name;

   RemoteEjbInvocationHandler(Object orProxy, Hashtable<?, ?> env, String name) {
      this.originalProxy = orProxy;
      this.environment = env;
      this.strName = name;
   }

   RemoteEjbInvocationHandler(Object orProxy, Hashtable<?, ?> env, Name name) {
      this.originalProxy = orProxy;
      this.environment = env;
      this.name = name;
   }

   @Override
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      boolean startManaging = true;
      EventMetadata metadata = null;
      EventResult thisResult = null;
      try {
         startManaging = Context.start();
         ControlEvent controlEvent = controlEvent();

         Class<?> interf = originalProxy.getClass().getInterfaces()[0];
         if (log.isDebugEnabled()) {
            log.debug("control " + controlEvent + " of " + interf + "." + method.getName() + "() for tenant "
                  + Context.internalSessionScope().getTenant());
            log.debug("Proxy: " + originalProxy);
         }

         Set<ResourceParameter> params = new TreeSet<ResourceParameter>(new ParameterSequenceComparator());
         for (int i = 0; i < method.getParameterTypes().length; i++) {
            params.add(new ResourceParameter("PARAM" + i, method.getParameterTypes()[i].getName(), args[i],
                  ParameterType.METHOD_PARAMETER, i));
         }

         params.add(new ResourceParameter(JNDI_CONTEXT, Hashtable.class.getName(), environment,
               ParameterType.INTERNAL_PARAMETER, method.getParameterTypes().length));
         if (strName != null) {
            params.add(new ResourceParameter(JNDI_NAME, String.class.getName(), strName,
                  ParameterType.INTERNAL_PARAMETER, method.getParameterTypes().length + 1));

         } else if (name != null) {
            params.add(new ResourceParameter(JNDI_NAME, Name.class.getName(), name, ParameterType.INTERNAL_PARAMETER,
                  method.getParameterTypes().length + 1));

         } else {
            String err = "String name and Name for the JNDI lookup are both null.";
            log.error(err);
            throw new IllegalArgumentException(err);
         }

         EjbResource resource = new EjbResource(interf, method, params);
         resource.setInvokerClass(RemoteEJBInvoker.class.getName());
         metadata = new EventMetadata(SENSOR_NAME, controlEvent, resource);
         Configuration.instance().getController().evaluate(metadata);
         thisResult = Context.internalRequestScope().registerEventResult(new EventResult(SENSOR_NAME, metadata));

         try {
            for (Actuator actuator : metadata.getActuators()) {
               actuator.beforeEvent(metadata);
            }

            if (metadata.getExecutionStatus() == ExecutionStatus.EXECUTING) {
               metadata.setExecutionStatus(ExecutionStatus.EXECUTED);
               if (!Context.requestScope().isPlaying()) {
                  Object result = method.invoke(originalProxy, args);
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

}
