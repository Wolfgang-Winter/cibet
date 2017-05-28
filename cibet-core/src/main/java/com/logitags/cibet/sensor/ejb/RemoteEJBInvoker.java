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

import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.resource.ParameterSequenceComparator;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.common.Invoker;
import com.logitags.cibet.sensor.common.MethodInvoker;

/**
 * creates a remote EJB on which to invoke a method.
 * 
 */
public class RemoteEJBInvoker extends MethodInvoker implements Invoker {

   private Log log = LogFactory.getLog(RemoteEJBInvoker.class);

   private static Invoker instance = null;

   public static synchronized Invoker createInstance() {
      if (instance == null) {
         instance = new RemoteEJBInvoker();
      }
      return instance;
   }

   protected RemoteEJBInvoker() {
   }

   public Object execute(String parameter, String targetType, String methodName, Set<ResourceParameter> parameters)
         throws Exception {
      try {
         Set<ResourceParameter> methodParams = new TreeSet<ResourceParameter>(new ParameterSequenceComparator());
         methodParams.addAll(parameters);

         Hashtable<?, ?> environment = null;
         Object jndiName = null;
         Iterator<ResourceParameter> iter = methodParams.iterator();
         while (iter.hasNext()) {
            ResourceParameter param = iter.next();
            if (RemoteEjbInvocationHandler.JNDI_CONTEXT.equals(param.getName())) {
               environment = (Hashtable<?, ?>) param.getUnencodedValue();
               iter.remove();
            } else if (RemoteEjbInvocationHandler.JNDI_NAME.equals(param.getName())) {
               jndiName = param.getUnencodedValue();
               iter.remove();
            }
         }

         if (environment == null) {
            String err = "Failed to find resource parameter " + RemoteEjbInvocationHandler.JNDI_CONTEXT;
            throw new Exception(err);
         }
         if (jndiName == null) {
            String err = "Failed to find resource parameter " + RemoteEjbInvocationHandler.JNDI_NAME;
            throw new Exception(err);
         }

         Context ctx = new InitialContext(environment);
         Object ejb;
         if (jndiName instanceof Name) {
            ejb = ctx.lookup((Name) jndiName);
         } else {
            ejb = ctx.lookup((String) jndiName);
         }

         Class<?>[] params = getParamTypes(methodParams);
         Method method = ejb.getClass().getMethod(methodName, params);
         return method.invoke(ejb, getParamValues(methodParams));
      } catch (Exception e) {
         log.error(e.getMessage(), e);
         throw e;
      }
   }

   @Override
   protected <T> T createObject(String parameter, Class<T> clazz) {
      throw new RuntimeException("not implemented");
   }

}
