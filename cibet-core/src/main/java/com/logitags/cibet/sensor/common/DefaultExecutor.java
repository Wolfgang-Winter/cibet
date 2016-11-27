package com.logitags.cibet.sensor.common;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.interceptor.InvocationContext;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;

import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.AnnotationUtil;
import com.logitags.cibet.core.CEntityManager;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.sensor.ejb.CibetInterceptorCallable;

public class DefaultExecutor implements SensorExecutor {

   private Log log = LogFactory.getLog(DefaultExecutor.class);

   /*
    * (non-Javadoc)
    * 
    * @see
    * com.logitags.cibet.sensor.common.SensorExecutor#proceed(com.logitags.cibet
    * .core.EventMetadata, javax.interceptor.InvocationContext)
    */
   @Override
   public void proceed(EventMetadata metadata, InvocationContext invocationCtx) throws Exception {
      CibetInterceptorCallable callable = new CibetInterceptorCallable(metadata, invocationCtx);
      callable.call();
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * com.logitags.cibet.sensor.common.SensorExecutor#invoke(com.logitags.cibet.
    * core.EventMetadata, java.lang.Object, java.lang.reflect.Method,
    * java.lang.Object[])
    */
   @Override
   public void invoke(EventMetadata metadata, Object object, Method method, Object[] args)
         throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
      if (!Context.requestScope().isPlaying()) {
         Object result = method.invoke(object, args);
         metadata.getResource().setResultObject(result);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * com.logitags.cibet.sensor.common.SensorExecutor#proceed(com.logitags.cibet
    * .core.EventMetadata, org.aspectj.lang.ProceedingJoinPoint)
    */
   @Override
   public void proceed(EventMetadata metadata, ProceedingJoinPoint joinPoint) throws Throwable {
      if (!Context.requestScope().isPlaying()) {
         Object result = joinPoint.proceed();
         metadata.getResource().setResultObject(result);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * com.logitags.cibet.sensor.common.SensorExecutor#doChain(com.logitags.cibet
    * .core.EventMetadata, javax.servlet.ServletRequest,
    * javax.servlet.http.HttpServletResponse, javax.servlet.FilterChain)
    */
   @Override
   public void doChain(EventMetadata metadata, ServletRequest req, HttpServletResponse resp, FilterChain chain)
         throws IOException, ServletException {
      if (!Context.requestScope().isPlaying()) {
         chain.doFilter(req, resp);
         metadata.getResource().setResultObject(resp.getStatus());
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * com.logitags.cibet.sensor.common.SensorExecutor#executeSql(com.logitags.
    * cibet.core.EventMetadata, java.sql.Statement, java.lang.String,
    * java.lang.Object)
    */
   @Override
   public void executeSql(EventMetadata metadata, Statement statement, String sql, Object addValue)
         throws SQLException {
      boolean result = false;
      if (!Context.requestScope().isPlaying()) {
         if (addValue == null) {
            result = statement.execute(sql);
         } else if (addValue instanceof Integer) {
            result = statement.execute(sql, (int) addValue);
         } else if (addValue instanceof int[]) {
            result = statement.execute(sql, (int[]) addValue);
         } else if (addValue instanceof String[]) {
            result = statement.execute(sql, (String[]) addValue);
         }

         if (log.isDebugEnabled()) {
            log.debug(sql + " |result: " + result);
         }
      }
      metadata.getResource().setResultObject(result);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.sensor.common.SensorExecutor#executeSqlUpdate(com.
    * logitags.cibet.core.EventMetadata, java.sql.Statement, java.lang.String,
    * java.lang.Object)
    */
   @Override
   public void executeSqlUpdate(EventMetadata metadata, Statement statement, String sql, Object addValue)
         throws SQLException {
      int count = 0;
      if (!Context.requestScope().isPlaying()) {
         if (addValue == null) {
            count = statement.executeUpdate(sql);
         } else if (addValue instanceof Integer) {
            count = statement.executeUpdate(sql, (int) addValue);
         } else if (addValue instanceof int[]) {
            count = statement.executeUpdate(sql, (int[]) addValue);
         } else if (addValue instanceof String[]) {
            count = statement.executeUpdate(sql, (String[]) addValue);
         }

         if (log.isDebugEnabled()) {
            log.debug(sql + " |result: " + count);
         }
      }
      metadata.getResource().setResultObject(count);
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * com.logitags.cibet.sensor.common.SensorExecutor#executeSql(com.logitags.
    * cibet.core.EventMetadata, java.sql.PreparedStatement, java.lang.String)
    */
   @Override
   public void executeSql(EventMetadata metadata, PreparedStatement statement, String sql) throws SQLException {
      boolean result = false;
      if (!Context.requestScope().isPlaying()) {
         result = statement.execute();
         if (log.isDebugEnabled()) {
            log.debug(sql + " |result: " + result);
         }
      }
      metadata.getResource().setResultObject(result);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.sensor.common.SensorExecutor#executeSqlUpdate(com.
    * logitags.cibet.core.EventMetadata, java.sql.PreparedStatement,
    * java.lang.String)
    */
   @Override
   public void executeSqlUpdate(EventMetadata metadata, PreparedStatement statement, String sql) throws SQLException {
      int count = 0;
      if (!Context.requestScope().isPlaying()) {
         count = statement.executeUpdate();
         if (log.isDebugEnabled()) {
            log.debug(sql + " |result: " + count);
         }
      }
      metadata.getResource().setResultObject(count);
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * com.logitags.cibet.sensor.common.SensorExecutor#jpaResultListQuery(com.
    * logitags.cibet.core.EventMetadata, javax.persistence.Query,
    * com.logitags.cibet.sensor.jpa.CibetEntityManager)
    */
   @Override
   public void jpaResultListQuery(EventMetadata metadata, Query query, CEntityManager entityManager) {
      List<?> result = new ArrayList<Object>();

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
      metadata.getResource().setResultObject(result);
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * com.logitags.cibet.sensor.common.SensorExecutor#jpaSingleResultQuery(com.
    * logitags.cibet.core.EventMetadata, javax.persistence.Query,
    * com.logitags.cibet.sensor.jpa.CibetEntityManager)
    */
   @Override
   public void jpaSingleResultQuery(EventMetadata metadata, Query query, CEntityManager entityManager) {
      if (!Context.requestScope().isPlaying()) {
         Object result = query.getSingleResult();
         if (result != null && entityManager.isLoadEager() && (result.getClass().getAnnotation(Embeddable.class) != null
               || result.getClass().getAnnotation(Entity.class) != null)) {
            CibetUtil.loadLazyEntities(result, result.getClass());
            List<Object> references = new ArrayList<Object>();
            references.add(result);
            CibetUtil.deepDetach(result, references);
         }
         metadata.getResource().setResultObject(result);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.sensor.common.SensorExecutor#jpaUpdateQuery(com.
    * logitags.cibet.core.EventMetadata, javax.persistence.Query)
    */
   @Override
   public void jpaUpdateQuery(EventMetadata metadata, Query query) {
      int result = 0;
      if (!Context.requestScope().isPlaying()) {
         result = query.executeUpdate();
      }
      metadata.getResource().setResultObject(result);
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * com.logitags.cibet.sensor.common.SensorExecutor#jpaFind(com.logitags.cibet
    * .core.EventMetadata, javax.persistence.EntityManager, java.lang.Class,
    * java.lang.Object, javax.persistence.LockModeType, java.util.Map, boolean)
    */
   @Override
   public void jpaFind(EventMetadata metadata, EntityManager entityManager, Class<?> clazz, Object id,
         LockModeType lockMode, Map<String, Object> props, boolean loadEager) {
      if (!Context.requestScope().isPlaying()) {
         Object obj = localFind(entityManager, clazz, id, lockMode, props);
         if (obj != null && loadEager) {
            CibetUtil.loadLazyEntities(obj, clazz);
            List<Object> references = new ArrayList<Object>();
            references.add(obj);
            CibetUtil.deepDetach(obj, references);
         }
         metadata.getResource().setResultObject(obj);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * com.logitags.cibet.sensor.common.SensorExecutor#jpaMerge(com.logitags.
    * cibet.core.EventMetadata, javax.persistence.EntityManager,
    * java.lang.Object)
    */
   @Override
   public void jpaMerge(EventMetadata metadata, EntityManager entityManager, Object obj) {
      Object ret = entityManager.merge(obj);
      // refresh the object into the resource:
      metadata.getResource().setObject(ret);
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * com.logitags.cibet.sensor.common.SensorExecutor#jpaPersist(com.logitags.
    * cibet.core.EventMetadata, javax.persistence.EntityManager,
    * java.lang.Object)
    */
   @Override
   public void jpaPersist(EventMetadata metadata, EntityManager entityManager, Object obj) {
      if (!Context.requestScope().isPlaying()) {
         entityManager.persist(obj);
         // refresh the object into the resource:
         entityManager.flush();
         metadata.getResource().setPrimaryKeyObject(AnnotationUtil.primaryKeyAsObject(obj));
         metadata.getResource().setObject(obj);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * com.logitags.cibet.sensor.common.SensorExecutor#jpaRemove(com.logitags.
    * cibet.core.EventMetadata, javax.persistence.EntityManager,
    * java.lang.Object)
    */
   @Override
   public void jpaRemove(EventMetadata metadata, EntityManager entityManager, Object obj) {
      if (!Context.requestScope().isPlaying()) {
         if (!entityManager.contains(obj)) {
            obj = entityManager.merge(obj);
         }
         entityManager.remove(obj);
      }
   }

   private <T> T localFind(EntityManager entityManager, Class<T> clazz, Object id, LockModeType lockMode,
         Map<String, Object> props) {
      if (lockMode == null) {
         if (props == null) {
            return entityManager.find(clazz, id);
         } else {
            return entityManager.find(clazz, id, props);
         }
      } else {
         if (props == null) {
            return entityManager.find(clazz, id, lockMode);
         } else {
            return entityManager.find(clazz, id, lockMode, props);
         }
      }
   }

}
