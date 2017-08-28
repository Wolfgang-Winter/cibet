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
package com.logitags.cibet.sensor.ejb;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.TreeSet;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.circuitbreaker.CircuitBreakerActuator;
import com.logitags.cibet.actuator.common.Actuator;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalRequestScope;
import com.logitags.cibet.control.Controller;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.resource.ParameterSequenceComparator;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.ResourceParameter;

/**
 * EJB interceptor that controls method invocation.
 */
public class CibetInterceptor implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private Log log = LogFactory.getLog(CibetInterceptor.class);

   private EjbSensorExecutor defaultExecutor = new EjbDefaultSensorExecutor();

   private static EjbSensorExecutor calledExecutor = null;

   public static final String SENSOR_NAME = "EJB";

   /**
    * controls method invocation.
    * 
    * @param ctx
    * @return
    * @throws Exception
    */
   @AroundInvoke
   public Object controlInvoke(InvocationContext ctx) throws Exception {
      String classname = ctx.getTarget().getClass().getName();
      Method method = ctx.getMethod();

      boolean startManaging = true;
      EventMetadata metadata = null;
      EventResult thisResult = null;

      try {
         startManaging = Context.start();
         ControlEvent controlEvent = controlEvent();

         if (log.isDebugEnabled()) {
            log.debug("control " + controlEvent + " of " + classname + "." + method.getName() + "() for tenant "
                  + Context.internalSessionScope().getTenant());
         }

         Set<ResourceParameter> params = new TreeSet<ResourceParameter>(new ParameterSequenceComparator());
         for (int i = 0; i < ctx.getMethod().getParameterTypes().length; i++) {
            params.add(new ResourceParameter("PARAM" + i, ctx.getMethod().getParameterTypes()[i].getName(),
                  ctx.getParameters()[i], ParameterType.METHOD_PARAMETER, i));
         }

         EjbResource resource = new EjbResource(ctx.getTarget(), method, params);
         resource.setInvokerClass(EJBInvoker.class.getName());
         metadata = new EventMetadata("EJB", controlEvent, resource);
         Controller.evaluate(metadata);
         thisResult = Context.internalRequestScope().registerEventResult(new EventResult(SENSOR_NAME, metadata));

         try {
            for (Actuator actuator : metadata.getActuators()) {
               actuator.beforeEvent(metadata);
            }

            if (metadata.getExecutionStatus() == ExecutionStatus.EXECUTING) {
               metadata.setExecutionStatus(ExecutionStatus.EXECUTED);
               if (!Context.requestScope().isPlaying()) {
                  // executor(metadata).proceed(metadata, ctx);
                  Object result = ctx.proceed();
                  log.debug("CI result=" + result);
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

   protected ControlEvent controlEvent() {
      ControlEvent ev = (ControlEvent) Context.internalRequestScope().getProperty(InternalRequestScope.CONTROLEVENT);
      if (ev != null) {
         Context.internalRequestScope().removeProperty(InternalRequestScope.CONTROLEVENT);
         return ev;
      } else {
         return ControlEvent.INVOKE;
      }
   }

   protected void doFinally(boolean startManaging, EventMetadata metadata, EventResult thisResult) {
      if (metadata != null && thisResult != null) {
         if (metadata.getExecutionStatus() == ExecutionStatus.EXECUTING) {
            thisResult.setExecutionStatus(ExecutionStatus.EXECUTED);
         } else {
            thisResult.setExecutionStatus(metadata.getExecutionStatus());
         }
      }

      if (startManaging) {
         Context.end();
      } else {
         if (metadata != null && metadata.getExecutionStatus() == ExecutionStatus.ERROR) {
            Context.requestScope().setRemark(null);
         }
      }
   }

   private EjbSensorExecutor executor(EventMetadata metadata) {
      if (metadata.getProperties().containsKey(CircuitBreakerActuator.TIMEOUT_KEY)) {
         if (calledExecutor == null) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            try {
               classLoader.loadClass("javax.enterprise.concurrent.ManagedExecutorService");
               // calledExecutor=;
            } catch (ClassNotFoundException e) {
               calledExecutor = new EjbCalledSensorExecutor();
            }
            log.info("instantiate " + calledExecutor.getClass().getSimpleName() + " as calledExecutor");

         }
         return calledExecutor;

      } else {
         return defaultExecutor;
      }
   }

}
