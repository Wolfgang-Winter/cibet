package com.logitags.cibet.sensor.ejb;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.interceptor.InvocationContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.sensor.common.AbstractCalledSensorExecutor;

public class EjbCalledSensorExecutor extends AbstractCalledSensorExecutor
      implements EjbSensorExecutor {

	private Log log = LogFactory.getLog(EjbCalledSensorExecutor.class);

	@Override
	public void proceed(EventMetadata metadata, InvocationContext invocationCtx)
	      throws Throwable {
		call(metadata, new CibetInterceptorCallable(metadata, invocationCtx));
	}

	@Override
	public void invoke(EventMetadata metadata, Object object, Method method,
	      Object[] args) throws IllegalAccessException,
	      IllegalArgumentException, InvocationTargetException {
		// TODO Auto-generated method stub

	}

}
