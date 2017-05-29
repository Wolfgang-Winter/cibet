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
package com.logitags.cibet.sensor.jpa;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.LockModeType;
import javax.persistence.Transient;
import javax.script.ScriptEngine;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.dc.ResourceApplyException;
import com.logitags.cibet.actuator.scheduler.SchedulerActuator;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalRequestScope;
import com.logitags.cibet.core.AnnotationUtil;
import com.logitags.cibet.core.CibetException;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.resource.ResourceParameter;

import de.danielbechler.diff.ObjectMerger;

@Entity
@DiscriminatorValue(value = "JpaResource")
public class JpaResource extends Resource {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private static Log log = LogFactory.getLog(JpaResource.class);

   public final static String CLASSLOCK = "CLASSLOCK";

   /**
    * the primary key of a JPA or JDBC resource in String format.
    */
   @Column(length = 50)
   private String primaryKeyId;

   /**
    * the primary key object of a JPA or JDBC resource.
    */
   @Transient
   protected Object primaryKeyObject;

   @Transient
   private boolean isEagerLoadedAndDetached;

   public JpaResource() {
   }

   /**
    * constructor used for JPA resources.
    * 
    * @param rh
    * @param entity
    */
   public JpaResource(Object entity) {
      setObject(entity);
      setPrimaryKeyObject(AnnotationUtil.primaryKeyAsObject(entity));
      resolveTargetType(entity);
   }

   /**
    * constructor for JPA SELECT resources
    * 
    * @param rh
    * @param target
    * @param primaryKey
    */
   public JpaResource(Class<?> target, Object primaryKey) {
      setObject(target);
      resolveTargetType(target);
      setPrimaryKeyObject(primaryKey);
   }

   /**
    * copy constructor
    * 
    * @param copy
    */
   public JpaResource(JpaResource copy) {
      super(copy);
      setPrimaryKeyId(copy.primaryKeyId);
      primaryKeyObject = copy.primaryKeyObject;
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
    * concatenates the values for creating the checkSum.
    */
   public String createCheckSumString() {
      StringBuffer b = new StringBuffer(super.createCheckSumString());
      b.append(primaryKeyId == null ? "" : primaryKeyId);
      return b.toString();
   }

   @Override
   public void fillContext(ScriptEngine engine) {
      engine.put("$TARGETTYPE", getTargetType());
      engine.put("$TARGET", getObject());

      // put object target also with simple classname
      if (!(getObject() instanceof String) && getObject() != null) {
         engine.put("$" + getObject().getClass().getSimpleName(), getObject());
      }
      engine.put("$PRIMARYKEY", getPrimaryKeyId());
   }

   @Override
   public Map<String, Object> getNotificationAttributes() {
      Map<String, Object> map = new HashMap<>();
      map.put("targetType", getTargetType());
      map.put("target", getObject());
      map.put("primaryKeyId", getPrimaryKeyId());
      return map;
   }

   @Override
   public Object apply(ControlEvent event) throws ResourceApplyException {
      ResourceParameter rp = getParameter(SchedulerActuator.CLEANOBJECT);
      if (rp != null) {
         // Update from SchedulerActuator
         return applySchedulerUpdate(event, rp);
      }

      try {
         EntityManager em = Context.internalRequestScope().getApplicationEntityManager();
         Object obj = getObject();

         switch (event) {
         case DELETE:
            em.remove(obj);
            break;
         case INSERT:
            em.persist(obj);
            setPrimaryKeyObject(AnnotationUtil.valueFromAnnotation(obj, Id.class));
            break;
         case UPDATE:
            obj = em.merge(obj);
            break;
         case SELECT:

            Map<String, Object> props = new HashMap<String, Object>();
            LockModeType lockMode = null;
            for (ResourceParameter param : getParameters()) {
               if (param.getParameterType() == ParameterType.JPA_HINT) {
                  props.put(param.getName(), param.getUnencodedValue());
               } else if (param.getParameterType() == ParameterType.JPA_LOCKMODETYPE) {
                  lockMode = (LockModeType) param.getUnencodedValue();
               } else {
                  throw new IllegalArgumentException(
                        param.getParameterType() + " ResourceParameter type is not supported for JPA SELECT events");
               }
            }

            if (lockMode == null) {
               if (props.isEmpty()) {
                  obj = em.find((Class<?>) getObject(), getPrimaryKeyObject());
               } else {
                  obj = em.find((Class<?>) getObject(), getPrimaryKeyObject(), props);
               }
            } else {
               if (props.isEmpty()) {
                  obj = em.find((Class<?>) getObject(), getPrimaryKeyObject(), lockMode);
               } else {
                  obj = em.find((Class<?>) getObject(), getPrimaryKeyObject(), lockMode, props);
               }
            }

            break;
         default:
            throw new RuntimeException("Invalid control event to apply: " + event);
         }
         return obj;
      } catch (CibetException e) {
         throw e;
      } catch (Exception e) {
         log.error(e.getMessage(), e);
         throw new ResourceApplyException("Release of Persistence action failed:\n" + toString(), e);
      }
   }

   public Object applySchedulerUpdate(ControlEvent event, ResourceParameter cleanResourceParam)
         throws ResourceApplyException {
      EntityManager appEM = Context.internalRequestScope().getApplicationEntityManager();
      Class<?> clazz;
      try {
         clazz = Class.forName(getTargetType());
      } catch (Exception e) {
         String err = "Failed to apply scheduled update of class " + getTargetType() + ": " + e.getMessage();
         throw new ResourceApplyException(err, e);
      }

      ControlEvent original = (ControlEvent) Context.internalRequestScope()
            .getProperty(InternalRequestScope.CONTROLEVENT);
      Context.internalRequestScope().removeProperty(InternalRequestScope.CONTROLEVENT);
      Object head = appEM.find(clazz, getPrimaryKeyObject());
      if (head == null) {
         String err = "Failed to find entity " + clazz + " with ID " + getPrimaryKeyObject()
               + " in database for executing scheduled update";
         throw new ResourceApplyException(err);
      }

      CibetUtil.loadLazyEntities(head, head.getClass());
      ResourceParameter headRP = new ResourceParameter(SchedulerActuator.ORIGINAL_OBJECT, head.getClass().getName(),
            head, ParameterType.INTERNAL_PARAMETER, getParameters().size() + 1);
      addParameter(headRP);

      try {
         ObjectMerger merger = new ObjectMerger(CibetUtil.getObjectDiffer());
         if (log.isDebugEnabled()) {
            log.debug("start merging");
            // log.debug("work: " + resource.getObject());
            log.debug("base: " + cleanResourceParam.getUnencodedValue());
            log.debug("head: " + head);
         }
         head = merger.merge(getObject(), cleanResourceParam.getUnencodedValue(), head);
         if (log.isDebugEnabled()) {
            log.debug("end merging");
            log.debug("head: " + head);
         }

         Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, original);
         Object newHead = appEM.merge(head);
         log.debug("new head: " + newHead + "-" + newHead.hashCode());
         return newHead;
      } finally {
         Context.internalRequestScope().removeProperty(InternalRequestScope.CONTROLEVENT);
      }
   }

   /**
    * constructs the Resource- specific group id. Concatenates targetType and primary key
    */
   @Override
   public void createGroupId() {
      if (getGroupId() != null && getGroupId().startsWith(getTargetType() + "-") && getPrimaryKeyId() != null) {
         setGroupId(getTargetType() + "-" + getPrimaryKeyId());

      } else if (Context.requestScope().getGroupId() != null) {
         setGroupId(Context.requestScope().getGroupId());

      } else if (getPrimaryKeyId() != null) {
         setGroupId(getTargetType() + "-" + getPrimaryKeyId());
      }
   }

   /**
    * Checks if the target object is locked by the given LockedObject. If the locked object has no objectId set (this is
    * the case when Release of an Insert event is locked) then the target class must implement the equals() method in
    * order to unambiguously identify the locked object and the target as identical.
    * 
    * @param target
    * @param objectId
    *           primary key of target
    * @param lo
    * @return
    */
   public boolean isLocked(Object target, String objectId) {
      if (CLASSLOCK.equals(primaryKeyId))
         return true;
      if (primaryKeyId == null || primaryKeyId.equals("0")) {
         // this is a release of an insert: has no primary key.
         Object obj = getObject();
         if (obj == null) {
            String msg = "System error: If the object to lock has no primary key the object must be "
                  + "encoded into LockedObject and the equals() method must be implemented";
            throw new RuntimeException(msg);
         }
         return obj.equals(target);

      } else {
         return primaryKeyId.equals(objectId);
      }
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("[");
      b.append(this.getClass().getSimpleName());
      b.append("] ");
      b.append(super.toString());
      if (primaryKeyId != null) {
         b.append(" ; primaryKeyId: ");
         b.append(primaryKeyId);
      }
      return b.toString();
   }

   @Override
   public String createUniqueId() {
      return DigestUtils.sha256Hex(getTargetType() + getPrimaryKeyId());
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

   /**
    * the primary key object of a JPA or JDBC resource.
    * 
    * @param id
    *           the primaryKeyObject to set
    */
   public void setPrimaryKeyObject(Object id) {
      this.primaryKeyObject = id;
      primaryKeyId = id == null ? null : id.toString();
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

}
