/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2014 Dr. Wolfgang Winter
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
package com.logitags.cibet.actuator.owner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.NoResultException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.common.AbstractActuator;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.AnnotationNotFoundException;
import com.logitags.cibet.core.AnnotationUtil;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.ExecutionStatus;

public class OwnerCheckActuator extends AbstractActuator {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private static Log log = LogFactory.getLog(OwnerCheckActuator.class);

   public static final String DEFAULTNAME = "OWNERCHECK";

   private Class<? extends Annotation> ownerAnnotation = Owner.class;

   private boolean throwWrongOwnerException = false;

   private transient OwnerCheckCallback ownerCheckCallback;

   public OwnerCheckActuator() {
      setName(DEFAULTNAME);
   }

   public OwnerCheckActuator(String name) {
      setName(name);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.actuator.common.AbstractActuator#beforeEvent(com.logitags.cibet.core.EventMetadata)
    */
   @Override
   public void beforeEvent(EventMetadata ctx) {
      if (ctx.getExecutionStatus() == ExecutionStatus.DENIED) {
         log.info("EventProceedStatus is DENIED. Skip afterEvent of " + this.getClass().getSimpleName());
         return;
      }

      if (ctx.getExecutionStatus() == ExecutionStatus.ERROR) {
         log.info("ERROR detected. Skip afterEvent of " + this.getClass().getSimpleName());
         return;
      }

      switch (ctx.getControlEvent()) {
      case DELETE:
      case INSERT:
      case UPDATE:
      case REJECT_DELETE:
      case REJECT_INSERT:
      case REJECT_UPDATE:
      case RELEASE_DELETE:
      case RELEASE_INSERT:
      case RELEASE_UPDATE:
      case FIRST_RELEASE_DELETE:
      case FIRST_RELEASE_INSERT:
      case FIRST_RELEASE_UPDATE:
      case PASSBACK_DELETE:
      case PASSBACK_INSERT:
      case PASSBACK_UPDATE:
      case SUBMIT_DELETE:
      case SUBMIT_INSERT:
      case SUBMIT_UPDATE:
      case RESTORE_INSERT:
      case RESTORE_UPDATE:
         Object entity = ctx.getResource().getUnencodedTargetObject();
         boolean isOkay = checkOwner(ctx, entity);
         if (!isOkay) {
            ctx.getResource().setResultObject(null);
            ctx.setExecutionStatus(ExecutionStatus.DENIED);
         }
         break;

      default:
         log.debug("before: skip " + ctx.getControlEvent() + " in " + this.getName());
         break;
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.actuator.common.AbstractActuator#afterEvent(com.logitags.cibet.core.EventMetadata)
    */
   @Override
   public void afterEvent(EventMetadata ctx) {
      if (ctx.getExecutionStatus() == ExecutionStatus.DENIED) {
         log.info("EventProceedStatus is DENIED. Skip afterEvent of " + this.getClass().getSimpleName());
         return;
      }

      if (ctx.getExecutionStatus() == ExecutionStatus.ERROR) {
         log.info("ERROR detected. Skip afterEvent of " + this.getClass().getSimpleName());
         return;
      }

      switch (ctx.getControlEvent()) {
      case SELECT:
      case REJECT_SELECT:
      case RELEASE_SELECT:
      case FIRST_RELEASE_SELECT:
      case PASSBACK_SELECT:
      case SUBMIT_SELECT:
         Object resultObject = ctx.getResource().getResultObject();

         if (resultObject instanceof List) {
            List<?> list = (List<?>) resultObject;
            Iterator<?> iter = list.iterator();
            while (iter.hasNext()) {
               Object item = iter.next();
               boolean isOkay = checkOwner(ctx, item);
               if (!isOkay) {
                  log.warn("remove from list: " + item);
                  iter.remove();
               }
            }

         } else {
            boolean isOkay = checkOwner(ctx, resultObject);
            if (!isOkay) {
               ctx.getResource().setResultObject(null);
               if (ctx.getException() == null && "JPAQUERY".equals(ctx.getSensor())) {
                  ctx.setException(new NoResultException());
               }
            }
         }

         break;

      default:
         log.debug("after: skip " + ctx.getControlEvent() + " in " + this.getName());
         break;
      }
   }

   private boolean checkOwner(EventMetadata ctx, Object resultObject) {
      String tenant = Context.sessionScope().getTenant();
      String owner = buildOwnerString(resultObject);
      if (owner == null) {
         return true;
      }
      if (!owner.startsWith(tenant)) {
         String err = "Owner Check failed for event " + ctx.getControlEvent() + " of resource [" + ctx.getResource()
               + "]: resource owner '" + owner + "' does not start with Cibet comtext owner '" + tenant + "'";
         log.warn(err);
         if (throwWrongOwnerException) {
            ctx.setException(new WrongOwnerException(resultObject, err, owner));
         }

         if (ownerCheckCallback != null) {
            ownerCheckCallback.onOwnerCheckFailed(tenant, resultObject, owner);
         }
         return false;
      }
      return true;
   }

   private String buildOwnerString(Object entity) {
      if (entity == null) return null;
      Deque<String> stack = new ArrayDeque<>();
      buildOwnerStack(stack, entity);
      if (stack.isEmpty()) return null;

      StringBuffer ownerString = new StringBuffer();
      Iterator<String> iter = stack.iterator();
      ownerString.append(iter.next());

      while (iter.hasNext()) {
         ownerString.append("|");
         ownerString.append(iter.next());
      }

      return ownerString.toString();
   }

   private void buildOwnerStack(Deque<String> stack, Object entity) {
      List<Object> owners = getAnnotatedValues(entity);
      if (owners.isEmpty()) {
         log.debug(entity.getClass().getName() + " has no Owner annotation");
         return;
      }

      // add @Id value
      try {
         String id = AnnotationUtil.primaryKeyAsString(entity);
         if (stack.contains(id)) {
            stack.remove(id);
         }
         stack.push(id);
      } catch (AnnotationNotFoundException e) {
         log.info(e.getMessage(), e);
      }

      for (Object owner : owners) {
         if (owner == null) continue;
         if (owner.getClass().isPrimitive() || owner instanceof String || owner instanceof Integer
               || owner instanceof Short || owner instanceof Long) {
            String id = owner.toString();
            if (stack.contains(id)) {
               stack.remove(id);
            }
            stack.push(id);

         } else {
            buildOwnerStack(stack, owner);
         }
      }
   }

   private List<Object> getAnnotatedValues(Object obj) {
      if (!ownerAnnotation.isAssignableFrom(Owner.class)) {
         return AnnotationUtil.getValuesOfAnnotatedFieldOrMethod(obj, ownerAnnotation);
      }

      Map<Integer, Object> map = new TreeMap<>();

      Class<?> clazz = obj.getClass();
      int sequence = 0;
      while (clazz != null) {
         Field[] f = clazz.getDeclaredFields();
         for (Field field : f) {
            Owner annotation = field.getAnnotation(Owner.class);
            if (annotation != null) {
               field.setAccessible(true);
               try {
                  Object value = field.get(obj);
                  if (annotation.value() >= 0) {
                     map.put(annotation.value(), value);
                  } else {
                     map.put(sequence, value);
                     sequence++;
                  }

                  log.debug("retrieved @Owner value from field " + field.getName() + ": " + value);
               } catch (Exception e) {
                  log.error(e.getMessage(), e);
               }
            }
         }
         clazz = clazz.getSuperclass();
      }

      clazz = obj.getClass();
      while (clazz != null) {
         Method[] m = clazz.getDeclaredMethods();
         for (Method method : m) {
            Owner annotation = method.getAnnotation(Owner.class);
            if (annotation != null) {
               try {
                  Object value = method.invoke(obj);
                  if (annotation.value() >= 0) {
                     map.put(annotation.value(), value);
                  } else {
                     map.put(sequence, value);
                     sequence++;
                  }

                  log.debug("retrieved @Owner value from method " + method.getName() + ": " + value);
               } catch (Exception e) {
                  log.error(e.getMessage(), e);
               }
            }
         }
         clazz = clazz.getSuperclass();
      }

      return new ArrayList<>(map.values());
   }

   /**
    * @return the ownerAnnotation
    */
   public Class<? extends Annotation> getOwnerAnnotation() {
      return ownerAnnotation;
   }

   /**
    * @param ownerAnnotation
    *           the ownerAnnotation to set
    */
   public void setOwnerAnnotation(Class<? extends Annotation> ownerAnnotation) {
      this.ownerAnnotation = ownerAnnotation;
   }

   /**
    * @return the throwWrongOwnerException
    */
   public boolean isThrowWrongOwnerException() {
      return throwWrongOwnerException;
   }

   /**
    * @param throwWrongOwnerException
    *           the throwWrongOwnerException to set
    */
   public void setThrowWrongOwnerException(boolean throwWrongOwnerException) {
      this.throwWrongOwnerException = throwWrongOwnerException;
   }

   /**
    * @return the ownerCheckCallback
    */
   public OwnerCheckCallback getOwnerCheckCallback() {
      return ownerCheckCallback;
   }

   /**
    * @param ownerCheckCallback
    *           the ownerCheckCallback to set
    */
   public void setOwnerCheckCallback(OwnerCheckCallback ownerCheckCallback) {
      this.ownerCheckCallback = ownerCheckCallback;
   }

}
