/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2016 Dr. Wolfgang Winter
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import javax.script.ScriptEngine;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.dc.ResourceApplyException;
import com.logitags.cibet.core.CibetException;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.resource.ParameterSequenceComparator;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.common.Invoker;

@Entity
@DiscriminatorValue(value = "MethodResource")
public class MethodResource extends Resource {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private static Log log = LogFactory.getLog(MethodResource.class);

   /**
    * the Invoker implementation class that is capable to provide an instance of the object class for executing the
    * controlled event on it.
    */
   private String invokerClass;

   /**
    * optional JNDI name of EJB or factory constructor
    */
   private String invokerParam;

   /**
    * method name which is controlled
    */
   private String method;

   /**
    * In case the Resource represents a method invocation, this is the Method reflection object.
    */
   @Transient
   private transient Method methodObject;

   public MethodResource() {
   }

   /**
    * constructor used for EJB and POJO resources
    * 
    * @param invokedObject
    * @param m
    * @param params
    */
   public MethodResource(Object invokedObject, Method m, Set<ResourceParameter> params) {
      setMethodObject(m);
      object = invokedObject;
      if (params != null) {
         setParameters(params);
      }
      resolveTargetType(invokedObject);
   }

   /**
    * copy constructor
    * 
    * @param copy
    */
   public MethodResource(MethodResource copy) {
      super(copy);
      setInvokerClass(copy.invokerClass);
      setInvokerParam(copy.invokerParam);
      methodObject = copy.getMethodObject();
      setMethod(copy.getMethod());
   }

   /**
    * concatenates the values for creating the checkSum.
    */
   public String createCheckSumString() {
      StringBuffer b = new StringBuffer(super.createCheckSumString());
      b.append(invokerParam == null ? "" : invokerParam);
      b.append(method == null ? "" : method);
      return b.toString();
   }

   @Override
   public void fillContext(ScriptEngine engine) {
      engine.put("$TARGETTYPE", getTargetType());
      engine.put("$TARGET", getObject());
      for (ResourceParameter param : getParameters()) {
         if (param.getParameterType() == ParameterType.METHOD_PARAMETER) {
            engine.put("$" + param.getName(), param.getUnencodedValue());
         }
      }
   }

   @Override
   public Map<String, Object> getNotificationAttributes() {
      Map<String, Object> map = new HashMap<>();
      map.put("targetType", getTargetType());
      map.put("method", getMethod());
      map.put("resultObject", getResultObject());
      return map;
   }

   @Override
   public Object apply(ControlEvent event) throws ResourceApplyException {
      try {
         Set<ResourceParameter> paramList = new TreeSet<ResourceParameter>(new ParameterSequenceComparator());
         paramList.addAll(getParameters());
         Class<? extends Invoker> facClass = (Class<? extends Invoker>) Class.forName(getInvokerClass());
         Method createMethod = facClass.getMethod("createInstance");
         Invoker fac = (Invoker) createMethod.invoke(null);
         return fac.execute(getInvokerParam(), getTargetType(), getMethod(), paramList);

      } catch (CibetException e) {
         throw e;
      } catch (InvocationTargetException e) {
         Throwable cause = e.getCause();
         log.debug("cause=" + cause);
         while (cause != null) {
            if (cause instanceof CibetException) {
               throw (CibetException) cause;
            }
            cause = cause.getCause();
         }
         // check EJBException cause but do not refer to EJBException
         cause = e.getCause();
         if (cause != null) {
            Throwable causedBy = null;
            try {
               Method m = cause.getClass().getMethod("getCausedByException");
               causedBy = (Throwable) m.invoke(cause);
            } catch (Exception e1) {
               log.debug("method getCausedByException() does not exist: " + e1.getMessage());
            }
            if (causedBy != null && causedBy instanceof CibetException) {
               throw (CibetException) causedBy;
            }
         }
         log.error(e.getMessage());
         throw new ResourceApplyException("Apply of Method Invocation failed:\n" + toString(), e);
      } catch (Exception e) {
         log.error(e.getMessage());
         throw new ResourceApplyException("Apply of Method Invocation failed:\n" + toString(), e);
      }
   }

   /**
    * the Invoker implementation class that is capable to provide an instance of the object class for executing the
    * controlled event on it.
    * 
    * @return the invokerClass
    */
   public String getInvokerClass() {
      return invokerClass;
   }

   /**
    * the Invoker implementation class that is capable to provide an instance of the object class for executing the
    * controlled event on it.
    * 
    * @param invokerClass
    *           the invokerClass to set
    */
   public void setInvokerClass(String invokerClass) {
      this.invokerClass = invokerClass;
   }

   /**
    * optional JNDI name of EJB or factory constructor
    * 
    * @return the invokerParam
    */
   public String getInvokerParam() {
      return invokerParam;
   }

   /**
    * optional JNDI name of EJB or factory constructor
    * 
    * @param invokerParam
    *           the invokerParam to set
    */
   public void setInvokerParam(String invokerParam) {
      this.invokerParam = invokerParam;
   }

   /**
    * method name which is controlled
    * 
    * @return the method
    */
   public String getMethod() {
      return method;
   }

   /**
    * method name which is controlled
    * 
    * @param method
    *           the method to set
    */
   public void setMethod(String method) {
      this.method = method;
   }

   /**
    * In case the Resource represents a method invocation, this is the Method reflection object.
    * 
    * @return the methodObject
    */
   public Method getMethodObject() {
      return methodObject;
   }

   /**
    * In case the Resource represents a method invocation, this is the Method reflection object.
    * 
    * @param mo
    *           the methodObject to set
    */
   public void setMethodObject(Method mo) {
      this.methodObject = mo;
      if (mo == null) {
         method = null;
      } else {
         method = mo.getName();
      }
   }

   @Override
   public String createUniqueId() {
      Base64 b64 = new Base64();
      StringBuffer b = new StringBuffer();
      b.append(getTargetType());
      b.append(getMethod());

      for (ResourceParameter param : getParameters()) {
         b.append(b64.encodeToString(param.getEncodedValue()));
      }
      return DigestUtils.sha256Hex(b.toString());
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("[");
      b.append(this.getClass().getSimpleName());
      b.append("] ");
      b.append(super.toString());
      b.append(" ; method: ");
      b.append(getMethod());
      b.append(" ; invoker: ");
      b.append(invokerClass);
      return b.toString();
   }

}
