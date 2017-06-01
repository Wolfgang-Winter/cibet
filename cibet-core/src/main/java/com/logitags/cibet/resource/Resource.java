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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.script.ScriptEngine;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.dc.ResourceApplyException;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.security.SecurityProvider;

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
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "CIB_RESOURCE")
@DiscriminatorColumn(name = "RESOURCETYPE")
public abstract class Resource implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 6897300124770713334L;

   private static Log log = LogFactory.getLog(Resource.class);

   @Id
   private String resourceId;

   /**
    * A unique identifier for this resource. The resource-specific classes define how this Id is created
    */
   private String uniqueId;

   /**
    * the type of the target of the resource. Could be a class name, a URL, a table name, an entity name etc.
    */
   private String target;

   /**
    * Encoded concrete object of this resource. In case of JPA, this is the encoded persisted object.
    */
   @Lob
   private byte[] targetObject;

   /**
    * Concrete object of this resource. In case of JPA, this is the unencoded persisted object.
    */
   @Transient
   protected transient Object unencodedTargetObject;

   /**
    * Encoded result of the event execution. In case of a method invocation it is either null, if the invoked method
    * returns void, or the encoded return value of the invoked method.
    */
   @Lob
   private byte[] result;

   /**
    * Unencoded result of the event execution. In case of a method invocation it is either null, if the invoked method
    * returns void, or the return value of the invoked method.
    */
   @Transient
   private transient Object resultObject;

   /**
    * method parameters or http attributes and parameters
    */
   @OneToMany(cascade = { CascadeType.ALL }, fetch = FetchType.EAGER)
   @JoinColumn(name = "RESOURCEID")
   private Set<ResourceParameter> parameters = new TreeSet<ResourceParameter>(new ParameterSequenceComparator());

   /**
    * flag if the target, the result and the ResourceParameter values of Resource are encrypted.
    */
   private boolean encrypted = false;

   /**
    * reference to the secret/key used for encryption or message digest generation.
    */
   private String keyReference;

   /**
    * Resources in archives and Controllables can be grouped. For JPA resources the group id is per default
    * 'target'-'primaryKeyId'. The groupId can always be overwritten by users by setting a groupId into the request
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
      unencodedTargetObject = copy.getUnencodedTargetObject();
      setResult(copy.getResult());
      resultObject = copy.getResultObject();
      setTarget(copy.getTarget());
      setTargetObject(copy.getTargetObject());
      setEncrypted(copy.encrypted);
      setKeyReference(copy.getKeyReference());
      Set<ResourceParameter> clonedList = new TreeSet<ResourceParameter>(new ParameterSequenceComparator());
      for (ResourceParameter rp : copy.getParameters()) {
         ResourceParameter p = new ResourceParameter(rp);
         clonedList.add(p);
      }
      setParameters(clonedList);
      setGroupId(copy.getGroupId());
   }

   @PrePersist
   public void prePersist() {
      if (resourceId == null) {
         resourceId = UUID.randomUUID().toString();
      }
   }

   /**
    * creates a unique Id of the Resource.
    * 
    * @return
    */
   public abstract String createUniqueId();

   /**
    * applies the action on the object controlled by this object. Release, Reject, Redo, Restore.
    * 
    * @return
    * @throws ResourceApplyException
    */
   public abstract Object apply(ControlEvent event) throws ResourceApplyException;

   /**
    * put any attributes into the script engine context. These can be evaluated by ConditionControl
    * 
    * @param engine
    */
   public abstract void fillContext(ScriptEngine engine);

   /**
    * constructs the Resource- specific group id.
    */
   public void createGroupId() {
      if (Context.requestScope().getGroupId() != null) {
         setGroupId(Context.requestScope().getGroupId());
      }
   }

   /**
    * gets the attributes of this class for posting http or email notification. Keys are the attribute names.
    * 
    * @return
    */
   public abstract Map<String, Object> getNotificationAttributes();

   protected void resolveTarget(Object o) {
      if (o instanceof Class<?>) {
         setTarget(((Class<?>) o).getName());
      } else if (o != null) {
         setTarget(o.getClass().getName());
      } else {
         setTarget(null);
      }
   }

   /**
    * concatenates the values for creating the checkSum.
    */
   public String createCheckSumString() {
      Base64 b64 = new Base64();

      StringBuffer b = new StringBuffer();
      b.append(target == null ? "" : target);
      b.append(targetObject == null ? "" : b64.encodeToString(targetObject));
      b.append(result == null ? "" : b64.encodeToString(result));
      b.append(encrypted);
      b.append(keyReference == null ? "" : keyReference);
      b.append(getUniqueId());

      for (ResourceParameter param : parameters) {
         b.append(param.getClassname());
         b.append(b64.encodeToString(param.getEncodedValue()));
      }
      return b.toString();
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("id: ");
      b.append(resourceId);
      b.append(", target: ");
      b.append(target);
      b.append(", encrypted: ");
      b.append(encrypted);
      // do not log ResourceParameter, if they are encrypted, they cannot be displayed.
      // for (ResourceParameter p : getParameters()) {
      // b.append("\n");
      // b.append(p);
      // }
      return b.toString();
   }

   /**
    * the type of the target of the resource. Could be a class name, a URL, a table name, an entity name etc.
    * 
    * @return the target
    */
   public String getTarget() {
      return target;
   }

   /**
    * the type of the target of the resource. Could be a class name, a URL, a table name, an entity name etc.
    * 
    * @param target
    *           the target to set
    */
   public void setTarget(String target) {
      this.target = target;
   }

   /**
    * Encoded concrete object of this resource. In case of JPA, this is the encoded persisted object.
    *
    * @return the target
    */
   public byte[] getTargetObject() {
      return targetObject;
   }

   /**
    * Encoded concrete object of this resource. In case of JPA, this is the encoded persisted object.
    *
    * @param target
    *           the target to set
    */
   public void setTargetObject(byte[] target) {
      this.targetObject = target;
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
   public Set<ResourceParameter> getParameters() {
      Set<ResourceParameter> ps = new TreeSet<ResourceParameter>(new ParameterSequenceComparator());
      ps.addAll(parameters);
      return ps;
   }

   /**
    * method parameters or http attributes and parameters
    * 
    * @param name
    * @return
    */
   public ResourceParameter getParameter(String name) {
      for (ResourceParameter rp : parameters) {
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
   public void setParameters(Set<ResourceParameter> parameters) {
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
      int newSize = parameters.size() + 1;
      if (value == null) {
         parameters.add(new ResourceParameter(key, String.class.getName(), null, type, newSize));
      } else {
         parameters.add(new ResourceParameter(key, value.getClass().getName(), value, type, newSize));
      }
   }

   public void addParameter(ResourceParameter p) {
      parameters.add(p);
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
    * Concrete object of this resource. In case of JPA, this is the unencoded persisted object.
    *
    * @return the object
    */
   public Object getUnencodedTargetObject() {
      if (unencodedTargetObject == null) {
         unencodedTargetObject = CibetUtil.decode(targetObject);
      }
      return unencodedTargetObject;
   }

   /**
    * Concrete object of this resource. In case of JPA, this is the unencoded persisted object.
    *
    * @param o
    *           the object to set
    */
   public void setUnencodedTargetObject(Object o) {
      this.unencodedTargetObject = o;
      try {
         targetObject = CibetUtil.encode(o);
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new IllegalArgumentException(e);
      }
   }

   /**
    * encrypts target, result and the ResourceParameter values.
    * 
    */
   public void encrypt() {
      if (!encrypted) {
         log.debug("encrypt resource " + getTarget());
         SecurityProvider secProvider = Configuration.instance().getSecurityProvider();
         setKeyReference(secProvider.getCurrentSecretKey());
         setEncrypted(true);
         setTargetObject(secProvider.encrypt(getTargetObject()));
         setResult(secProvider.encrypt(getResult()));
         for (ResourceParameter param : parameters) {
            param.setEncodedValue(secProvider.encrypt(param.getEncodedValue()));
         }
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
      setTargetObject(secProvider.decrypt(getTargetObject(), getKeyReference()));
      setResult(secProvider.decrypt(getResult(), getKeyReference()));
      if (parameters != null) {
         for (ResourceParameter param : parameters) {
            Context.internalRequestScope().getEntityManager().detach(param);
            param.setEncodedValue(secProvider.decrypt(param.getEncodedValue(), getKeyReference()));
         }
      }
      log.debug("decrypted resource " + getTarget());
      return true;
   }

   /**
    * The invoker history, from where the current control event was called. In case of a http request this is the IP
    * address from where the request was sent including proxy IPs. In case of a method invocation or similar, this is
    * the command stacktrace
    * 
    * @return the ipInvoker
    */
   public Object getInvoker() {
      StackTraceElement[] traces = Thread.currentThread().getStackTrace();
      return traces;
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
         uniqueId = createUniqueId();
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

   public String getResourceId() {
      return resourceId;
   }

   public void setResourceId(String resourceId) {
      this.resourceId = resourceId;
   }

}
