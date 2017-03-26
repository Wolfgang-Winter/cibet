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
package com.logitags.cibet.resource;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.AnnotationUtil;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.security.SecurityProvider;
import com.logitags.cibet.sensor.jdbc.driver.SqlParameter;

/**
 * Represents a resource that can by controlled by one of the sensors. The content, e.g. target type, of the Resource
 * object depends on the business case:<br>
 * JPA: entity class and primary key, query<br>
 * EJB, method call: class name, method name, method parameters<br>
 * HTTP request: HttpRequest, HttpResponse, URL, http parameters, http body<br>
 * JDBC: table name, SQL statement<br>
 * 
 * @author Wolfgang
 * 
 */
@Embeddable
public class Resource implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 6897300124770713334L;

   private static Log log = LogFactory.getLog(Resource.class);

   private static ParameterSequenceComparator comparator = new ParameterSequenceComparator();

   /**
    * A unique identifier for this resource. The resource-specific ResourceHandler classes define how this Id is created
    */
   private String uniqueId;

   /**
    * the type of the target of the resource. Could be a class name, a URL, a table name, an entity name etc.
    */
   private String targetType;

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
    * Encoded concrete object of this resource. In case of JPA, this is the encoded persisted object.
    */
   @Lob
   private byte[] target;

   /**
    * the primary key of a JPA or JDBC resource in String format.
    */
   @Column(length = 50)
   private String primaryKeyId;

   /**
    * Encoded result of the event execution. In case of a method invocation it is either null, if the invoked method
    * returns void, or the encoded return value of the invoked method.
    */
   @Lob
   private byte[] result;

   /**
    * method parameters or http attributes and parameters
    */
   @OneToMany(cascade = { CascadeType.ALL }, fetch = FetchType.EAGER)
   @JoinColumn(name = "dummy")
   // @JoinTable
   private List<ResourceParameter> parameters = new LinkedList<ResourceParameter>();

   /**
    * a handler to which Resource-specific logic is delegated.
    */
   private String resourceHandlerClass;

   /**
    * flag if the target, the result and the ResourceParameter values of Resource are encrypted.
    */
   private boolean encrypted = false;

   /**
    * reference to the secret/key used for encryption or message digest generation.
    */
   private String keyReference;

   /**
    * method name which is controlled
    */
   private String method;

   /**
    * Unencoded result of the event execution. In case of a method invocation it is either null, if the invoked method
    * returns void, or the return value of the invoked method.
    */
   @Transient
   private transient Object resultObject;

   /**
    * In case the Resource represents a method invocation, this is the Method reflection object.
    */
   @Transient
   private transient Method methodObject;

   /**
    * Concrete object of this resource. In case of JPA, this is the unencoded persisted object.
    */
   @Transient
   private transient Object object;

   /**
    * the primary key object of a JPA or JDBC resource.
    */
   @Transient
   private Object primaryKeyObject;

   /**
    * The invoker history, from where the current control event was called. In case of a http request this is the IP
    * address from where the request was sent including proxy IPs. In case of a method invocation or similar, this is
    * the command stacktrace
    */
   @Transient
   private Object invoker;

   /**
    * HTTP request data in case of http HTTP-FILTER requests, otherwise null.
    */
   @Transient
   private HttpRequestData httpRequestData;

   /**
    * a handler to which Resource-specific logic is delegated.
    */
   @Transient
   private transient ResourceHandler resourceHandler;

   @Transient
   private boolean isEagerLoadedAndDetached;

   /**
    * Resources in archives and DcControllables can be grouped. For JPA resources the group id is per default
    * 'targetType'-'primaryKeyId'. The groupId can always be overwritten by users by setting a groupId into the request
    * scope context.
    */
   private String groupId;

   /**
    * constructor used by JDBC EntityDefinition only.
    */
   public Resource() {
   }

   /**
    * copy constructor
    * 
    * @param copy
    */
   public Resource(Resource copy) {
      setUniqueId(copy.uniqueId);
      setInvokerClass(copy.invokerClass);
      setInvokerParam(copy.invokerParam);
      setInvoker(copy.invoker);
      object = copy.getObject();
      methodObject = copy.getMethodObject();
      setMethod(copy.getMethod());
      primaryKeyObject = copy.primaryKeyObject;
      setPrimaryKeyId(copy.primaryKeyId);
      setResourceHandlerClass(copy.resourceHandlerClass);
      setResult(copy.getResult());
      resultObject = copy.getResultObject();
      setTargetType(copy.getTargetType());
      setTarget(copy.getTarget());
      setEncrypted(copy.encrypted);
      setKeyReference(copy.getKeyReference());
      List<ResourceParameter> clonedList = new LinkedList<ResourceParameter>();
      for (ResourceParameter rp : copy.getParameters()) {
         ResourceParameter p = new ResourceParameter(rp);
         clonedList.add(p);
      }
      setParameters(clonedList);
      setGroupId(copy.getGroupId());
   }

   /**
    * constructor used for EJB and POJO resources
    * 
    * @param rh
    * @param targ
    * @param m
    * @param params
    */
   public Resource(Class<? extends ResourceHandler> rh, Object targ, Method m, List<ResourceParameter> params) {
      resourceHandlerClass = rh.getName();
      object = targ;
      setMethodObject(m);
      if (params != null) {
         parameters = params;
      }
      resolveTargetType(targ);
   }

   /**
    * constructor used for JPA resources.
    * 
    * @param rh
    * @param o
    */
   public Resource(Class<? extends ResourceHandler> rh, Object o) {
      resourceHandlerClass = rh.getName();
      setObject(o);
      setPrimaryKeyObject(AnnotationUtil.primaryKeyAsObject(o));
      resolveTargetType(o);
   }

   /**
    * constructor for JPA SELECT resources
    * 
    * @param rh
    * @param target
    * @param primaryKey
    */
   public Resource(Class<? extends ResourceHandler> rh, Class<?> target, Object primaryKey) {
      resourceHandlerClass = rh.getName();
      setObject(target);
      resolveTargetType(target);
      setPrimaryKeyObject(primaryKey);
   }

   /**
    * constructor used for JDBC resources
    * 
    * @param rh
    * @param sql
    * @param targetType
    * @param insUpdColumns
    * @param pk
    * @param params
    */
   public Resource(Class<? extends ResourceHandler> rh, String sql, String targetType, SqlParameter pk,
         List<ResourceParameter> params) {
      resourceHandlerClass = rh.getName();
      object = sql;
      try {
         setTarget(CibetUtil.encode(sql));
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new IllegalArgumentException(e);
      }
      setTargetType(targetType);
      setPrimaryKeyObject(pk);
      if (params != null) {
         parameters = params;
      }
   }

   /**
    * constructor for JPA Query sensor
    * 
    * @param rh
    * @param queryToken
    * @param params
    */
   public Resource(Class<? extends ResourceHandler> rh, String queryToken, List<ResourceParameter> params) {
      resourceHandlerClass = rh.getName();
      setObject(queryToken);
      setTargetType(queryToken);
      if (params != null) {
         parameters = params;
      }
   }

   /**
    * constructor used for http ServletFilter resources
    * 
    * @param rh
    * @param targ
    * @param meth
    * @param r
    * @param response
    */
   public Resource(Class<? extends ResourceHandler> rh, String targ, String meth, HttpRequestData r) {
      resourceHandlerClass = rh.getName();
      targetType = targ;
      method = meth;
      httpRequestData = r;
   }

   private void resolveTargetType(Object o) {
      if (o instanceof Class<?>) {
         setTargetType(((Class<?>) o).getName());
      } else if (o != null) {
         setTargetType(o.getClass().getName());
      } else {
         setTargetType(null);
      }
   }

   /**
    * returns a handler to which Resource-specific logic is delegated.
    */
   public ResourceHandler getResourceHandler() {
      if (resourceHandler == null) {
         if (resourceHandlerClass == null) {
            String err = "ResourceHandlerClass is null";
            log.warn(err);
            return null;
         }

         try {
            Class<? extends ResourceHandler> rh = (Class<? extends ResourceHandler>) Class
                  .forName(resourceHandlerClass);
            Constructor<? extends ResourceHandler> constr = rh.getConstructor(Resource.class);
            resourceHandler = constr.newInstance(this);
         } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
         }
      }
      return resourceHandler;
   }

   /**
    * concatenates the values for creating the checkSum.
    */
   public String createCheckSumString() {
      Base64 b64 = new Base64();

      StringBuffer b = new StringBuffer();
      b.append(targetType == null ? "" : targetType);
      b.append(target == null ? "" : b64.encodeToString(target));
      b.append(primaryKeyId == null ? "" : primaryKeyId);
      b.append(method == null ? "" : method);
      b.append(result == null ? "" : b64.encodeToString(result));
      b.append(invokerParam == null ? "" : invokerParam);
      b.append(encrypted);
      b.append(keyReference == null ? "" : keyReference);
      b.append(getUniqueId());

      Collections.sort(getParameters(), comparator);
      for (ResourceParameter param : getParameters()) {
         b.append(param.getClassname());
         b.append(b64.encodeToString(param.getEncodedValue()));
      }
      return b.toString();
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append(targetType);
      b.append("::");
      b.append(method);
      if (primaryKeyId != null) {
         b.append(" ; primaryKeyId: ");
         b.append(primaryKeyId);
      }
      b.append(" ; ");
      b.append(getResourceHandler().getClass().getSimpleName());
      return b.toString();
   }

   /**
    * the type of the target of the resource. Could be a class name, a URL, a table name, an entity name etc.
    * 
    * @return the targetType
    */
   public String getTargetType() {
      return targetType;
   }

   /**
    * the type of the target of the resource. Could be a class name, a URL, a table name, an entity name etc.
    * 
    * @param targetType
    *           the targetType to set
    */
   public void setTargetType(String targetType) {
      this.targetType = targetType;
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
    * Encoded concrete object of this resource. In case of JPA, this is the encoded persisted object.
    * 
    * @return the target
    */
   public byte[] getTarget() {
      return target;
   }

   /**
    * Encoded concrete object of this resource. In case of JPA, this is the encoded persisted object.
    * 
    * @param target
    *           the target to set
    */
   public void setTarget(byte[] target) {
      this.target = target;
   }

   /**
    * the primary key of a JPA or JDBC resource in String format.
    * 
    * @return the primaryKeyId
    */
   public String getPrimaryKeyId() {
      return primaryKeyId;
   }

   /**
    * the primary key of a JPA or JDBC resource in String format.
    * 
    * @param primaryKeyId
    *           the primaryKeyId to set
    */
   public void setPrimaryKeyId(String primaryKeyId) {
      this.primaryKeyId = primaryKeyId;
   }

   /**
    * Encoded result of the event execution. In case of a method invocation it is either null, if the invoked method
    * returns void, or the encoded return value of the invoked method.
    * 
    * @return the result
    */
   public byte[] getResult() {
      return result;
   }

   /**
    * Encoded result of the event execution. In case of a method invocation it is either null, if the invoked method
    * returns void, or the encoded return value of the invoked method.
    * 
    * @param result
    *           the result to set
    */
   public void setResult(byte[] result) {
      this.result = result;
   }

   /**
    * method parameters or http attributes and parameters
    * 
    * @return the parameters
    */
   public List<ResourceParameter> getParameters() {
      return parameters;
   }

   /**
    * method parameters or http attributes and parameters
    * 
    * @param name
    * @return
    */
   public ResourceParameter getParameter(String name) {
      for (ResourceParameter rp : getParameters()) {
         if (rp.getName().equals(name)) {
            return rp;
         }
      }
      return null;
   }

   /**
    * method parameters or http attributes and parameters
    * 
    * @param parameters
    *           the parameters to set
    */
   public void setParameters(List<ResourceParameter> parameters) {
      this.parameters = parameters;
   }

   /**
    * add a parameter to the list.
    * 
    * @param key
    * @param value
    * @param type
    */
   public void addParameter(String key, Object value, ParameterType type) {
      if (key == null) {
         throw new IllegalArgumentException("Failed to add parameter: key is null");
      }
      List<ResourceParameter> params = getParameters();
      if (value == null) {
         params.add(new ResourceParameter(key, String.class.getName(), null, type, params.size() + 1));
      } else {
         params.add(new ResourceParameter(key, value.getClass().getName(), value, type, params.size() + 1));
      }
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
    * Unencoded result of the event execution. In case of a method invocation it is either null, if the invoked method
    * returns void, or the return value of the invoked method.
    * 
    * @return the resultObject
    */
   public Object getResultObject() {
      if (resultObject == null && result != null) {
         resultObject = CibetUtil.decode(result);
      }
      return resultObject;
   }

   /**
    * Unencoded result of the event execution. In case of a method invocation it is either null, if the invoked method
    * returns void, or the return value of the invoked method.
    * 
    * @param ro
    *           the resultObject to set
    */
   public void setResultObject(Object ro) {
      this.resultObject = ro;
      try {
         result = CibetUtil.encode(ro);
      } catch (IOException e) {
         log.warn("Failed to encode method result object: " + e.getMessage(), e);
      }
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

   /**
    * Concrete object of this resource. In case of JPA, this is the unencoded persisted object.
    * 
    * @return the object
    */
   public Object getObject() {
      if (object == null) {
         object = CibetUtil.decode(target);
      }
      return object;
   }

   /**
    * Concrete object of this resource. In case of JPA, this is the unencoded persisted object.
    * 
    * @param o
    *           the object to set
    */
   public void setObject(Object o) {
      this.object = o;
      try {
         target = CibetUtil.encode(o);
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new IllegalArgumentException(e);
      }
   }

   /**
    * the primary key object of a JPA or JDBC resource.
    * 
    * @return the primaryKeyObject
    */
   public Object getPrimaryKeyObject() {
      if (primaryKeyObject == null) {
         if (getObject() instanceof Class<?>) {
            Class<?> pkTypeClass = AnnotationUtil.typeFromAnnotation((Class<?>) getObject(), Id.class);
            primaryKeyObject = cast(pkTypeClass, getPrimaryKeyId());

         } else if (!(getObject() instanceof String)) {
            primaryKeyObject = AnnotationUtil.primaryKeyAsObject(getObject());
         }
      }
      return primaryKeyObject;
   }

   private Object cast(Class<?> toClass, String obj) {
      if (toClass == String.class) {
         return obj;
      } else if (toClass == int.class) {
         return Integer.parseInt(obj);
      } else if (toClass == Integer.class) {
         return Integer.valueOf(obj);
      } else if (toClass == long.class) {
         return Long.parseLong(obj);
      } else if (toClass == Long.class) {
         return Long.valueOf(obj);
      } else if (toClass == short.class) {
         return Short.parseShort(obj);
      } else if (toClass == Short.class) {
         return Short.valueOf(obj);
      } else {
         throw new RuntimeException("Failed to cast " + obj + " into type " + toClass);
      }
   }

   /**
    * encrypts target, result and the ResourceParameter values.
    * 
    */
   public void encrypt() {
      log.debug("encrypt resource " + getTargetType());
      SecurityProvider secProvider = Configuration.instance().getSecurityProvider();
      setKeyReference(secProvider.getCurrentSecretKey());
      setEncrypted(true);
      setTarget(secProvider.encrypt(getTarget()));
      setResult(secProvider.encrypt(getResult()));
      for (ResourceParameter param : getParameters()) {
         param.setEncodedValue(secProvider.encrypt(param.getEncodedValue()));
      }
   }

   /**
    * decrypts target, result and the ResourceParameter values.
    * 
    */
   public boolean decrypt() {
      if (!isEncrypted())
         return false;
      SecurityProvider secProvider = Configuration.instance().getSecurityProvider();
      setTarget(secProvider.decrypt(getTarget(), getKeyReference()));
      setResult(secProvider.decrypt(getResult(), getKeyReference()));
      if (getParameters() != null) {
         for (ResourceParameter param : getParameters()) {
            Context.internalRequestScope().getEntityManager().detach(param);
            param.setEncodedValue(secProvider.decrypt(param.getEncodedValue(), getKeyReference()));
         }
      }
      log.debug("decrypted resource " + getTargetType());
      return true;
   }

   /**
    * the primary key object of a JPA or JDBC resource.
    * 
    * @param id
    *           the primaryKeyObject to set
    */
   public void setPrimaryKeyObject(Object id) {
      this.primaryKeyObject = id;
      if (id != null && id instanceof SqlParameter) {
         SqlParameter sqlId = (SqlParameter) id;
         primaryKeyId = sqlId.getValue() == null ? null : sqlId.getValue().toString();
      } else {
         primaryKeyId = id == null ? null : id.toString();
      }
   }

   /**
    * returns a handler to which Resource-specific logic is delegated.
    * 
    * @return the resourceHandler
    */
   public String getResourceHandlerClass() {
      return resourceHandlerClass;
   }

   /**
    * sets a handler to which Resource-specific logic is delegated.
    * 
    * @param resourceHandler
    *           the resourceHandler to set
    */
   public void setResourceHandlerClass(String resourceHandler) {
      this.resourceHandlerClass = resourceHandler;
      this.resourceHandler = null;
   }

   /**
    * The invoker history, from where the current control event was called. In case of a http request this is the IP
    * address from where the request was sent including proxy IPs. In case of a method invocation or similar, this is
    * the command stacktrace
    * 
    * @return the ipInvoker
    */
   public Object getInvoker() {
      if (invoker == null) {
         StackTraceElement[] traces = Thread.currentThread().getStackTrace();
         return traces;
      } else {
         return invoker;
      }
   }

   /**
    * The invoker history, from where the current control event was called. In case of a http request this is the IP
    * address from where the request was sent including proxy IPs. In case of a method invocation or similar, this is
    * the command stacktrace
    * 
    * @param ipInvoker
    *           the ipInvoker to set
    */
   public void setInvoker(Object ipInvoker) {
      this.invoker = ipInvoker;
   }

   /**
    * HTTP request data in case of http HTTP-FILTER requests, otherwise null.
    * 
    * @return the httpRequest
    */
   public HttpRequestData getHttpRequestData() {
      return httpRequestData;
   }

   /**
    * flag if the target, the result and the ResourceParameter values of Resource are encrypted.
    * 
    * @return the encrypted
    */
   public boolean isEncrypted() {
      return encrypted;
   }

   /**
    * flag if the target, the result and the ResourceParameter values of Resource are encrypted.
    * 
    * @param encrypted
    *           the encrypted to set
    */
   public void setEncrypted(boolean encrypted) {
      this.encrypted = encrypted;
   }

   /**
    * reference to the secret/key used for encryption or message digest generation.
    * 
    * @return the keyReference
    */
   public String getKeyReference() {
      return keyReference;
   }

   /**
    * reference to the secret/key used for encryption or message digest generation.
    * 
    * @param keyReference
    *           the keyReference to set
    */
   public void setKeyReference(String keyReference) {
      this.keyReference = keyReference;
   }

   /**
    * A unique identifier for this resource. The resource-specific ResourceHandler classes define how this Id is created
    * 
    * @return the uniqueId
    */
   public String getUniqueId() {
      if (uniqueId == null) {
         uniqueId = getResourceHandler().createUniqueId();
      }
      return uniqueId;
   }

   /**
    * A unique identifier for this resource. The resource-specific ResourceHandler classes define how this Id is created
    * 
    * @param uniqueId
    *           the uniqueId to set
    */
   public void setUniqueId(String uniqueId) {
      this.uniqueId = uniqueId;
   }

   /**
    * Flag for JPA resource. If this resource is a JPA entity, this flag signals if dependencies of the entity and the
    * entity itself are all eager loaded and detached.
    * 
    * @return the isEagerLoadedAndDetached
    */
   public boolean isEagerLoadedAndDetached() {
      return isEagerLoadedAndDetached;
   }

   /**
    * Flag for JPA resource. If this resource is a JPA entity, this flag signals if dependencies of the entity and the
    * entity itself are all eager loaded and detached.
    * 
    * @param isEagerLoadedAndDetached
    *           the isEagerLoadedAndDetached to set
    */
   public void setEagerLoadedAndDetached(boolean isEagerLoadedAndDetached) {
      this.isEagerLoadedAndDetached = isEagerLoadedAndDetached;
   }

   /**
    * @return the groupId
    */
   public String getGroupId() {
      return groupId;
   }

   /**
    * @param groupId
    *           the groupId to set
    */
   public void setGroupId(String groupId) {
      this.groupId = groupId;
   }

}
