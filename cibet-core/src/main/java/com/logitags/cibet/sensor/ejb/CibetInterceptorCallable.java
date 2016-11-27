package com.logitags.cibet.sensor.ejb;

import java.util.concurrent.Callable;

import javax.interceptor.InvocationContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.core.EventMetadata;

public class CibetInterceptorCallable implements Callable<Void> {

	private Log log = LogFactory.getLog(CibetInterceptorCallable.class);

	private EventMetadata eventMetadata;

	private InvocationContext invocationCtx;

	public CibetInterceptorCallable(EventMetadata metadata, InvocationContext ctx) {
		eventMetadata = metadata;
		invocationCtx = ctx;
	}

	@Override
	public Void call() throws Exception {
		Object result = invocationCtx.proceed();
		log.debug("CI result=" + result);
		eventMetadata.getResource().setResultObject(result);
		return null;
	}

}
