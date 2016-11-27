package com.logitags.cibet.sensor.ejb;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.interceptor.InvocationContext;

import com.logitags.cibet.core.EventMetadata;

public interface EjbSensorExecutor {

	/**
	 * execute the CibetInterceptor
	 * 
	 * @param metadata
	 * @param invocationCtx
	 * @throws Exception
	 */
	void proceed(EventMetadata metadata, InvocationContext invocationCtx)
	      throws Throwable;

	/**
	 * execute the RemoteEjbInvocationHandler
	 * 
	 * @param metadata
	 * @param object
	 * @param method
	 * @param args
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	void invoke(EventMetadata metadata, Object object, Method method,
	      Object[] args) throws IllegalAccessException,
	      IllegalArgumentException, InvocationTargetException;

}
