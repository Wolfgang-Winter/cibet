package com.logitags.cibet.sensor.common;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import javax.interceptor.InvocationContext;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.aspectj.lang.ProceedingJoinPoint;

import com.logitags.cibet.core.CEntityManager;
import com.logitags.cibet.core.EventMetadata;

public interface SensorExecutor {

   public abstract void proceed(EventMetadata metadata, InvocationContext invocationCtx) throws Exception;

   public abstract void invoke(EventMetadata metadata, Object object, Method method, Object[] args)
         throws IllegalAccessException, IllegalArgumentException, InvocationTargetException;

   public abstract void proceed(EventMetadata metadata, ProceedingJoinPoint joinPoint) throws Throwable;

   public abstract void doChain(EventMetadata metadata, ServletRequest req, HttpServletResponse resp, FilterChain chain)
         throws IOException, ServletException;

   public abstract void executeSql(EventMetadata metadata, Statement statement, String sql, Object addValue)
         throws SQLException;

   public abstract void executeSqlUpdate(EventMetadata metadata, Statement statement, String sql, Object addValue)
         throws SQLException;

   public abstract void executeSql(EventMetadata metadata, PreparedStatement statement, String sql) throws SQLException;

   public abstract void executeSqlUpdate(EventMetadata metadata, PreparedStatement statement, String sql)
         throws SQLException;

   public abstract void jpaResultListQuery(EventMetadata metadata, Query query, CEntityManager entityManager);

   public abstract void jpaSingleResultQuery(EventMetadata metadata, Query query, CEntityManager entityManager);

   public abstract void jpaUpdateQuery(EventMetadata metadata, Query query);

   public abstract void jpaFind(EventMetadata metadata, EntityManager entityManager, Class<?> clazz, Object id,
         LockModeType lockMode, Map<String, Object> props, boolean loadEager);

   public abstract void jpaMerge(EventMetadata metadata, EntityManager entityManager, Object obj);

   public abstract void jpaPersist(EventMetadata metadata, EntityManager entityManager, Object obj);

   public abstract void jpaRemove(EventMetadata metadata, EntityManager entityManager, Object obj);

}