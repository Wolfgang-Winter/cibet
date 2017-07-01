package com.logitags.cibet.sensor.ejb;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.interceptor.InvocationContext;

import com.logitags.cibet.core.EventMetadata;

public class EjbDefaultSensorExecutor implements EjbSensorExecutor, Serializable {

   @Override
   public void proceed(EventMetadata metadata, InvocationContext invocationCtx) throws Throwable {
      CibetInterceptorCallable callable = new CibetInterceptorCallable(metadata, invocationCtx);
      callable.call();
   }

   @Override
   public void invoke(EventMetadata metadata, Object object, Method method, Object[] args)
         throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
      // TODO Auto-generated method stub

   }

}
