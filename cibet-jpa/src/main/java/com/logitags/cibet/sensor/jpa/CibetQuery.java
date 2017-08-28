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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.common.Actuator;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.control.Controller;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.resource.ParameterSequenceComparator;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.ResourceParameter;

public class CibetQuery implements Query {

   private Log log = LogFactory.getLog(CibetQuery.class);

   private static final String PARAMETER_NAME_PREFIX = "JPA-";

   private static final String SENSOR_NAME = "JPAQUERY";

   private QueryType queryType;

   private String queryToken;

   protected Object additionalQueryValue;

   private CibetEntityManager entityManager;

   private Map<String, ResourceParameter> parameters = new TreeMap<String, ResourceParameter>();

   private Query query;

   public CibetQuery(Query q, String qt, CibetEntityManager em, QueryType queryType) {
      this(q, qt, em, queryType, null);
   }

   public CibetQuery(Query q, String qt, CibetEntityManager em, QueryType queryType, Object additionalValue) {
      this.query = q;
      this.queryToken = qt;
      this.entityManager = em;
      this.queryType = queryType;
      this.additionalQueryValue = additionalValue;
   }

   @Override
   public List getResultList() {
      boolean startManaging = true;
      EventMetadata metadata = null;
      EventResult thisResult = null;

      try {
         startManaging = Context.start();

         metadata = before(QueryExecutionType.JPA_GET_RESULTLIST, ControlEvent.SELECT);
         thisResult = Context.internalRequestScope().registerEventResult(new EventResult(SENSOR_NAME, metadata));

         List<?> result = new ArrayList<Object>();
         try {
            for (Actuator actuator : metadata.getActuators()) {
               actuator.beforeEvent(metadata);
            }

            if (metadata.getExecutionStatus() == ExecutionStatus.EXECUTING) {
               metadata.setExecutionStatus(ExecutionStatus.EXECUTED);
               if (!Context.requestScope().isPlaying()) {
                  result = query.getResultList();
                  for (Object object : result) {
                     if (object != null && entityManager.isLoadEager()
                           && (object.getClass().getAnnotation(Embeddable.class) != null
                                 || object.getClass().getAnnotation(Entity.class) != null)) {
                        CibetUtil.loadLazyEntities(object, object.getClass());
                        List<Object> references = new ArrayList<Object>();
                        references.add(object);
                        CibetUtil.deepDetach(object, references);
                     }
                  }
               }
            }

         } catch (Throwable e) {
            log.error(e.getMessage(), e);
            metadata.setExecutionStatus(ExecutionStatus.ERROR);
            Context.requestScope().setRemark(e.getMessage());
            metadata.setException(e);
         }

         metadata.getResource().setResultObject(result);
         for (Actuator actuator : metadata.getActuators()) {
            actuator.afterEvent(metadata);
         }

         return (List) metadata.getResource().getResultObject();
      } finally {
         entityManager.doFinally(startManaging, metadata, thisResult);
      }
   }

   @Override
   public Object getSingleResult() {
      boolean startManaging = true;
      EventMetadata metadata = null;
      EventResult thisResult = null;

      try {
         startManaging = Context.start();

         metadata = before(QueryExecutionType.JPA_GET_SINGLE_RESULT, ControlEvent.SELECT);
         thisResult = Context.internalRequestScope().registerEventResult(new EventResult(SENSOR_NAME, metadata));

         Object result = null;
         try {
            for (Actuator actuator : metadata.getActuators()) {
               actuator.beforeEvent(metadata);
            }

            if (metadata.getExecutionStatus() == ExecutionStatus.EXECUTING) {
               metadata.setExecutionStatus(ExecutionStatus.EXECUTED);
               if (!Context.requestScope().isPlaying()) {
                  result = query.getSingleResult();
                  if (log.isDebugEnabled()) {
                     log.debug(queryToken + " |result: " + result);
                  }
                  if (result != null && entityManager.isLoadEager()
                        && (result.getClass().getAnnotation(Embeddable.class) != null
                              || result.getClass().getAnnotation(Entity.class) != null)) {
                     CibetUtil.loadLazyEntities(result, result.getClass());
                     List<Object> references = new ArrayList<Object>();
                     references.add(result);
                     CibetUtil.deepDetach(result, references);
                  }
               }
            }

         } catch (NoResultException e) {
            log.info(e.getMessage());
            // no result is not an error
            metadata.setExecutionStatus(ExecutionStatus.EXECUTED);
            Context.requestScope().setRemark(e.getMessage());
            metadata.setException(e);

         } catch (Throwable e) {
            log.error(e.getMessage(), e);
            metadata.setExecutionStatus(ExecutionStatus.ERROR);
            Context.requestScope().setRemark(e.getMessage());
            metadata.setException(e);
         }

         metadata.getResource().setResultObject(result);

         for (Actuator actuator : metadata.getActuators()) {
            actuator.afterEvent(metadata);
         }

      } finally {
         entityManager.doFinally(startManaging, metadata, thisResult);
      }

      if (metadata.getResource().getResultObject() == null) {
         throw new NoResultException("Query is postponed or executed in Play mode");
      } else {
         return metadata.getResource().getResultObject();
      }
   }

   @Override
   public int executeUpdate() {
      boolean startManaging = true;
      EventMetadata metadata = null;
      EventResult thisResult = null;

      try {
         startManaging = Context.start();

         metadata = before(QueryExecutionType.JPA_EXECUTE_UPDATE, ControlEvent.UPDATEQUERY);
         thisResult = Context.internalRequestScope().registerEventResult(new EventResult(SENSOR_NAME, metadata));

         int result = 0;
         try {
            for (Actuator actuator : metadata.getActuators()) {
               actuator.beforeEvent(metadata);
            }

            if (metadata.getExecutionStatus() == ExecutionStatus.EXECUTING) {
               metadata.setExecutionStatus(ExecutionStatus.EXECUTED);
               if (!Context.requestScope().isPlaying()) {
                  result = query.executeUpdate();
                  if (log.isDebugEnabled()) {
                     log.debug(queryToken + " |result: " + result);
                  }
               }
            }

         } catch (Throwable e) {
            log.error(e.getMessage(), e);
            metadata.setExecutionStatus(ExecutionStatus.ERROR);
            Context.requestScope().setRemark(e.getMessage());
            metadata.setException(e);
         }

         metadata.getResource().setResultObject(result);
         for (Actuator actuator : metadata.getActuators()) {
            actuator.afterEvent(metadata);
         }

         return (int) metadata.getResource().getResultObject();
      } finally {
         entityManager.doFinally(startManaging, metadata, thisResult);
      }
   }

   @Override
   public int getFirstResult() {
      return query.getFirstResult();
   }

   @Override
   public FlushModeType getFlushMode() {
      return query.getFlushMode();
   }

   @Override
   public Map<String, Object> getHints() {
      return query.getHints();
   }

   @Override
   public LockModeType getLockMode() {
      return query.getLockMode();
   }

   @Override
   public int getMaxResults() {
      return query.getMaxResults();
   }

   @Override
   public Parameter<?> getParameter(String arg0) {
      return query.getParameter(arg0);
   }

   @Override
   public Parameter<?> getParameter(int arg0) {
      return query.getParameter(arg0);
   }

   @Override
   public <T> Parameter<T> getParameter(String arg0, Class<T> arg1) {
      return query.getParameter(arg0, arg1);
   }

   @Override
   public <T> Parameter<T> getParameter(int arg0, Class<T> arg1) {
      return query.getParameter(arg0, arg1);
   }

   @Override
   public <T> T getParameterValue(Parameter<T> arg0) {
      return query.getParameterValue(arg0);
   }

   @Override
   public Object getParameterValue(String arg0) {
      return query.getParameterValue(arg0);
   }

   @Override
   public Object getParameterValue(int arg0) {
      return query.getParameterValue(arg0);
   }

   @Override
   public Set<Parameter<?>> getParameters() {
      return query.getParameters();
   }

   @Override
   public boolean isBound(Parameter<?> arg0) {
      return query.isBound(arg0);
   }

   @Override
   public Query setFirstResult(int firstResult) {
      query = query.setFirstResult(firstResult);

      ResourceParameter param = new ResourceParameter("firstResult", int.class.getName(), firstResult,
            ParameterType.JPA_FIRST_RESULT, ParameterType.JPA_FIRST_RESULT.hashCode());
      parameters.put(param.getName(), param);

      return this;
   }

   @Override
   public Query setFlushMode(FlushModeType mode) {
      query = query.setFlushMode(mode);

      ResourceParameter param = new ResourceParameter("flushMode", FlushModeType.class.getName(), mode,
            ParameterType.JPA_FLUSH_MODE, ParameterType.JPA_FLUSH_MODE.hashCode());
      parameters.put(param.getName(), param);

      return this;
   }

   @Override
   public Query setHint(String name, Object value) {
      query = query.setHint(name, value);

      ResourceParameter param = new ResourceParameter(name, Object.class.getName(), value, ParameterType.JPA_HINT,
            ParameterType.JPA_HINT.hashCode());
      parameters.put(param.getName(), param);

      return this;
   }

   @Override
   public Query setLockMode(LockModeType mode) {
      query = query.setLockMode(mode);

      ResourceParameter param = new ResourceParameter("lockMode", LockModeType.class.getName(), mode,
            ParameterType.JPA_LOCKMODETYPE, ParameterType.JPA_LOCKMODETYPE.hashCode());
      parameters.put(param.getName(), param);

      return this;
   }

   @Override
   public Query setMaxResults(int max) {
      query = query.setMaxResults(max);

      ResourceParameter param = new ResourceParameter("maxResult", int.class.getName(), max,
            ParameterType.JPA_MAX_RESULT, ParameterType.JPA_MAX_RESULT.hashCode());
      parameters.put(param.getName(), param);

      return this;
   }

   @Override
   public <T> Query setParameter(Parameter<T> par, T value) {
      query = query.setParameter(par, value);

      // try {
      // int position = -1;
      // String name = PARAMETER_NAME_PREFIX;
      // ParameterType ptype = ParameterType.JPA_NAMED_PARAMETER;
      // if (par.getPosition() != null) {
      // position = par.getPosition();
      // name = name + position;
      // ptype = ParameterType.JPA_INDEXED_PARAMETER;
      // } else if (par.getName() != null) {
      // name = par.getName();
      // }
      // ResourceParameter param = new ResourceParameter(name,
      // Object.class.getName(), value, ptype, position);
      // parameters.put(param.getName(), param);
      // } catch (IOException e) {
      // log.error(e.getMessage(), e);
      // throw new IllegalArgumentException(e.getMessage(), e);
      // }

      return this;
   }

   @Override
   public Query setParameter(String name, Object value) {
      query = query.setParameter(name, value);

      ResourceParameter param = new ResourceParameter(name, Object.class.getName(), value,
            ParameterType.JPA_NAMED_PARAMETER, name.hashCode());
      parameters.put(name, param);

      return this;
   }

   @Override
   public Query setParameter(int position, Object value) {
      query = query.setParameter(position, value);

      ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + position, Object.class.getName(), value,
            ParameterType.JPA_INDEXED_PARAMETER, position);
      parameters.put(param.getName(), param);

      return this;
   }

   @Override
   public Query setParameter(Parameter<Calendar> par, Calendar value, TemporalType ttype) {
      query = query.setParameter(par, value, ttype);

      // try {
      // int position = -1;
      // String name = PARAMETER_NAME_PREFIX;
      // ParameterType ptype = ParameterType.JPA_NAMED_CALENDAR_PARAMETER;
      // if (par.getPosition() != null) {
      // position = par.getPosition();
      // name = name + position;
      // ptype = ParameterType.JPA_INDEXED_CALENDAR_PARAMETER;
      // } else if (par.getName() != null) {
      // name = par.getName();
      // }
      // ResourceParameter param = new ResourceParameter(name, ttype.name(),
      // value, ptype, position);
      // parameters.put(param.getName(), param);
      // } catch (IOException e) {
      // log.error(e.getMessage(), e);
      // throw new IllegalArgumentException(e.getMessage(), e);
      // }

      return this;
   }

   @Override
   public Query setParameter(Parameter<Date> par, Date value, TemporalType ttype) {
      query = query.setParameter(par, value, ttype);

      // try {
      // int position = -1;
      // String name = PARAMETER_NAME_PREFIX;
      // ParameterType ptype = ParameterType.JPA_NAMED_DATE_PARAMETER;
      // if (par.getPosition() != null) {
      // position = par.getPosition();
      // name = name + position;
      // ptype = ParameterType.JPA_INDEXED_DATE_PARAMETER;
      // } else if (par.getName() != null) {
      // name = par.getName();
      // }
      // ResourceParameter param = new ResourceParameter(name, ttype.name(),
      // value, ptype, position);
      // parameters.put(param.getName(), param);
      // } catch (IOException e) {
      // log.error(e.getMessage(), e);
      // throw new IllegalArgumentException(e.getMessage(), e);
      // }

      return this;
   }

   @Override
   public Query setParameter(String name, Calendar cal, TemporalType ttype) {
      query = query.setParameter(name, cal, ttype);

      ResourceParameter param = new ResourceParameter(name, ttype.name(), cal,
            ParameterType.JPA_NAMED_CALENDAR_PARAMETER, name.hashCode());
      parameters.put(param.getName(), param);

      return this;
   }

   @Override
   public Query setParameter(String name, Date date, TemporalType ttype) {
      query = query.setParameter(name, date, ttype);

      ResourceParameter param = new ResourceParameter(name, ttype.name(), date, ParameterType.JPA_NAMED_DATE_PARAMETER,
            name.hashCode());
      parameters.put(param.getName(), param);

      return this;
   }

   @Override
   public Query setParameter(int position, Calendar cal, TemporalType ttype) {
      query = query.setParameter(position, cal, ttype);

      ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + position, ttype.name(), cal,
            ParameterType.JPA_INDEXED_CALENDAR_PARAMETER, position);
      parameters.put(param.getName(), param);

      return this;
   }

   @Override
   public Query setParameter(int position, Date date, TemporalType ttype) {
      query = query.setParameter(position, date, ttype);

      ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + position, ttype.name(), date,
            ParameterType.JPA_INDEXED_DATE_PARAMETER, position);
      parameters.put(param.getName(), param);

      return this;
   }

   @Override
   public <T> T unwrap(Class<T> arg0) {
      return query.unwrap(arg0);
   }

   private EventMetadata before(QueryExecutionType executionType, ControlEvent event) {
      entityManager.entityManagerIntoContext();
      ControlEvent controlEvent = entityManager.controlEvent(event);

      Set<ResourceParameter> params = new TreeSet<ResourceParameter>(new ParameterSequenceComparator());
      params.addAll(parameters.values());
      JpaQueryResource res = new JpaQueryResource(queryToken, params);
      EventMetadata metadata = new EventMetadata(SENSOR_NAME, controlEvent, res);

      metadata.getResource().addParameter("StatementType", executionType, ParameterType.JPA_STATEMENT_TYPE);
      metadata.getResource().addParameter("QueryType", queryType, ParameterType.JPA_QUERY_TYPE);
      if (additionalQueryValue != null) {
         metadata.getResource().addParameter("AdditionalQueryParameter", additionalQueryValue,
               ParameterType.JPA_QUERY_ADDITIONAL_VALUE);
      }

      Controller.evaluate(metadata);

      return metadata;
   }

}
