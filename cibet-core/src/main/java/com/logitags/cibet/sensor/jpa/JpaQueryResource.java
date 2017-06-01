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

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.script.ScriptEngine;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.dc.ResourceApplyException;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.resource.ResourceParameter;

@Entity
@DiscriminatorValue(value = "JpaQueryResource")
public class JpaQueryResource extends Resource {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private static Log log = LogFactory.getLog(JpaQueryResource.class);

   public JpaQueryResource() {
   }

   /**
    * constructor for JPA Query sensor
    * 
    * @param rh
    * @param queryToken
    * @param params
    */
   public JpaQueryResource(String queryToken, Set<ResourceParameter> params) {
      setTarget(queryToken);
      if (params != null) {
         setParameters(params);
      }
   }

   /**
    * copy constructor
    * 
    * @param copy
    */
   public JpaQueryResource(JpaQueryResource copy) {
      super(copy);
   }

   @Override
   public void fillContext(ScriptEngine engine) {
      engine.put("$TARGET", getTarget());
   }

   @Override
   public Map<String, Object> getNotificationAttributes() {
      Map<String, Object> map = new HashMap<>();
      map.put("target", getTarget());
      return map;
   }

   @Override
   public Object apply(ControlEvent event) throws ResourceApplyException {
      EntityManager em = Context.internalRequestScope().getApplicationEntityManager();

      QueryType queryType = null;
      Object addValue = null;
      QueryExecutionType queryExecution = null;
      for (ResourceParameter par : getParameters()) {
         if (par.getParameterType() == ParameterType.JPA_QUERY_TYPE) {
            queryType = (QueryType) par.getUnencodedValue();
         }
         if (par.getParameterType() == ParameterType.JPA_QUERY_ADDITIONAL_VALUE) {
            addValue = par.getUnencodedValue();
         }
         if (par.getParameterType() == ParameterType.JPA_STATEMENT_TYPE) {
            queryExecution = (QueryExecutionType) par.getUnencodedValue();
         }

      }
      if (queryType == null) {
         String err = "Failed to retrieve Query type from the ResourceParameters. No parameter of type "
               + ParameterType.JPA_QUERY_TYPE + " is present";
         throw new RuntimeException(err);
      }
      if (queryExecution == null) {
         String err = "Failed to retrieve Query Execution type from the ResourceParameters. No parameter of type "
               + ParameterType.JPA_STATEMENT_TYPE + " is present";
         throw new RuntimeException(err);
      }

      // create query
      Query query = null;
      switch (queryType) {
      case NAMED_QUERY:
         query = em.createNamedQuery(getTarget());
         break;
      case NATIVE_MAPPED_QUERY:
         query = em.createNativeQuery(getTarget(), (String) addValue);
         break;
      case NATIVE_QUERY:
         query = em.createNativeQuery(getTarget());
         break;
      case NAMED_TYPED_QUERY:
         query = em.createNamedQuery(getTarget(), (Class<?>) addValue);
         break;
      case NATIVE_TYPED_QUERY:
         query = em.createNativeQuery(getTarget(), (Class<?>) addValue);
         break;
      case QUERY:
         query = em.createQuery(getTarget());
         break;
      case TYPED_QUERY:
         query = em.createQuery(getTarget(), (Class<?>) addValue);
         break;
      case CRITERIA_QUERY:
         String err = "<T> TypedQuery<T> createQuery(CriteriaQuery<T> arg0) is not controlled by Cibet";
         log.error(err);
         throw new IllegalStateException(err);
      }

      // set parameters
      for (ResourceParameter par : getParameters()) {
         setParameter(query, par);
      }

      // execute query
      Object result = null;
      switch (queryExecution) {
      case JPA_EXECUTE_UPDATE:
         result = query.executeUpdate();
         break;
      case JPA_GET_RESULTLIST:
         result = query.getResultList();
         break;
      case JPA_GET_SINGLE_RESULT:
         result = query.getSingleResult();
      }

      return result;
   }

   /**
    * concatenates the values for creating the checkSum.
    */
   public String createCheckSumString() {
      StringBuffer b = new StringBuffer(super.createCheckSumString());
      return b.toString();
   }

   @Override
   public String createUniqueId() {
      Base64 b64 = new Base64();
      StringBuffer b = new StringBuffer();
      b.append(getTarget());

      for (ResourceParameter param : getParameters()) {
         b.append(b64.encodeToString(param.getEncodedValue()));
      }
      return DigestUtils.sha256Hex(b.toString());
   }

   private void setParameter(Query query, ResourceParameter par) {
      switch (par.getParameterType()) {
      case JPA_INDEXED_CALENDAR_PARAMETER:
         query.setParameter(par.getSequence(), (Calendar) par.getUnencodedValue(),
               TemporalType.valueOf(par.getClassname()));
         break;
      case JPA_INDEXED_PARAMETER:
         query.setParameter(par.getSequence(), par.getUnencodedValue());
         break;
      case JPA_INDEXED_DATE_PARAMETER:
         query.setParameter(par.getSequence(), (Date) par.getUnencodedValue(),
               TemporalType.valueOf(par.getClassname()));
         break;
      case JPA_NAMED_CALENDAR_PARAMETER:
         query.setParameter(par.getName(), (Calendar) par.getUnencodedValue(),
               TemporalType.valueOf(par.getClassname()));
         break;
      case JPA_NAMED_DATE_PARAMETER:
         query.setParameter(par.getName(), (Date) par.getUnencodedValue(), TemporalType.valueOf(par.getClassname()));
         break;
      case JPA_NAMED_PARAMETER:
         query.setParameter(par.getName(), par.getUnencodedValue());
         break;
      case JPA_FIRST_RESULT:
         query.setFirstResult((int) par.getUnencodedValue());
         break;
      case JPA_FLUSH_MODE:
         query.setFlushMode((FlushModeType) par.getUnencodedValue());
         break;
      case JPA_HINT:
         query.setHint(par.getName(), par.getUnencodedValue());
         break;
      case JPA_LOCKMODETYPE:
         query.setLockMode((LockModeType) par.getUnencodedValue());
         break;
      case JPA_MAX_RESULT:
         query.setMaxResults((int) par.getUnencodedValue());
         break;
      default:
         log.debug(
               "Ignoring other parameter of type " + par.getParameterType() + " with value " + par.getUnencodedValue());
      }
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("[JpaQueryResource] ");
      b.append(super.toString());
      return b.toString();
   }

}
