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
package com.logitags.cibet.sensor.jpa;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.LockModeType;
import javax.script.ScriptEngine;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.dc.ResourceApplyException;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.AnnotationUtil;
import com.logitags.cibet.core.CibetException;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.resource.ResourceHandler;
import com.logitags.cibet.resource.ResourceParameter;

/**
 * This resource represents an object that is persisted, merged or removed from the database by a JPA EntityManager.
 * Target type is the class name.
 * 
 * @author Wolfgang
 * 
 */
public class JpaResourceHandler implements Serializable, ResourceHandler {

   /**
    * 
    */
   private static final long serialVersionUID = -7875259168730700530L;

   private static Log log = LogFactory.getLog(JpaResourceHandler.class);

   protected Resource resource;

   public JpaResourceHandler(Resource res) {
      resource = res;
   }

   @Override
   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("[JpaResource] targetType: ");
      b.append(resource.getTargetType());
      b.append(" ; primaryKeyId: ");
      b.append(resource.getPrimaryKeyId());
      return b.toString();
   }

   @Override
   public void fillContext(ScriptEngine engine) {
      engine.put("$TARGETTYPE", resource.getTargetType());
      engine.put("$TARGET", resource.getObject());

      // put object target also with simple classname
      if (!(resource.getObject() instanceof String) && resource.getObject() != null) {
         engine.put("$" + resource.getObject().getClass().getSimpleName(), resource.getObject());
      }
      engine.put("$PRIMARYKEY", resource.getPrimaryKeyId());
   }

   @Override
   public Map<String, Object> getNotificationAttributes() {
      Map<String, Object> map = new HashMap<>();
      map.put("targetType", resource.getTargetType());
      map.put("target", resource.getObject());
      map.put("primaryKeyId", resource.getPrimaryKeyId());
      return map;
   }

   @Override
   public Object apply(ControlEvent event) throws ResourceApplyException {
      try {
         EntityManager em = Context.internalRequestScope().getApplicationEntityManager();
         Object obj = resource.getObject();

         switch (event) {
         case DELETE:
            em.remove(obj);
            break;
         case INSERT:
            em.persist(obj);
            resource.setPrimaryKeyObject(AnnotationUtil.valueFromAnnotation(obj, Id.class));
            break;
         case UPDATE:
            obj = em.merge(obj);
            break;
         case SELECT:

            Map<String, Object> props = new HashMap<String, Object>();
            LockModeType lockMode = null;
            for (ResourceParameter param : resource.getParameters()) {
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
                  obj = em.find((Class<?>) resource.getObject(), resource.getPrimaryKeyObject());
               } else {
                  obj = em.find((Class<?>) resource.getObject(), resource.getPrimaryKeyObject(), props);
               }
            } else {
               if (props.isEmpty()) {
                  obj = em.find((Class<?>) resource.getObject(), resource.getPrimaryKeyObject(), lockMode);
               } else {
                  obj = em.find((Class<?>) resource.getObject(), resource.getPrimaryKeyObject(), lockMode, props);
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

   @Override
   public String createUniqueId() {
      return DigestUtils.sha256Hex(resource.getTargetType() + resource.getPrimaryKeyId());
   }

}
