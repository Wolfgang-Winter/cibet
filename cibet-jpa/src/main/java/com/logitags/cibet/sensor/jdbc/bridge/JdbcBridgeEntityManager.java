/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 *******************************************************************************
 */
package com.logitags.cibet.sensor.jdbc.bridge;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.Id;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.metamodel.Metamodel;
import javax.sql.DataSource;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.core.AnnotationNotFoundException;
import com.logitags.cibet.core.AnnotationUtil;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.sensor.jdbc.def.ArchiveDefinition;
import com.logitags.cibet.sensor.jdbc.def.ControllableDefinition;
import com.logitags.cibet.sensor.jdbc.def.EntityDefinition;
import com.logitags.cibet.sensor.jdbc.def.EventResultDefinition;
import com.logitags.cibet.sensor.jdbc.def.PseudoEntityDefinition;
import com.logitags.cibet.sensor.jdbc.def.ResourceDefinition;
import com.logitags.cibet.sensor.jdbc.driver.CibetConnection;
import com.logitags.cibet.sensor.jdbc.driver.CibetJdbcException;
import com.logitags.cibet.sensor.jdbc.driver.JdbcResource;

/**
 * An EntityManager that translates from JPA API to JDBC API. It is not for general use. It can only be used for
 * internal Cibet database operations.
 * 
 */
public class JdbcBridgeEntityManager implements EntityManager {

   /**
    * logger for tracing
    */
   private Log log = LogFactory.getLog(JdbcBridgeEntityManager.class);

   private static Map<Class<?>, EntityDefinition> entityDefinitions = Collections
         .synchronizedMap(new HashMap<Class<?>, EntityDefinition>());

   private static Map<String, EntityDefinition> queryDefinitions = Collections
         .synchronizedMap(new HashMap<String, EntityDefinition>());

   private boolean isRegistered = false;

   /**
    * this connection is not committed and not closed within Cibet classes
    */
   private Connection jdbcConnection;

   /**
    * Connections from this datasource are not committed within Cibet classes but closed. It is assumed that commit is
    * done by a container.
    */
   private DataSource datasource;

   private EntityManagerFactory entityManagerFactory;

   /**
    * add an EntityDefinition implementation for the given class.
    * 
    * @param clazz
    * @param inDef
    */
   public static void registerEntityDefinition(Class<?> clazz, EntityDefinition inDef) {
      if (clazz == null || inDef == null) {
         throw new IllegalArgumentException("Class or EntityDefinition is null");
      }
      entityDefinitions.put(clazz, inDef);
      for (String queryName : inDef.getQueries().keySet()) {
         queryDefinitions.put(queryName, inDef);
      }
   }

   /**
    * @return the queryDefinitions
    */
   public static Map<String, EntityDefinition> getQueryDefinitions() {
      return queryDefinitions;
   }

   /**
    * 
    * @param conn
    *           the database connection that is used in the current transaction. The connection will not be closed by
    *           this class.
    */
   public JdbcBridgeEntityManager(Connection conn) {
      checkConnection(conn);
      jdbcConnection = conn;
      init();
   }

   /**
    * 
    * @param ds
    *           a DataSource that is used in the current transaction
    */
   public JdbcBridgeEntityManager(DataSource ds) {
      if (ds == null) {
         String msg = "Failed to instantiate JdbcEntityManager: DataSource is null";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }
      datasource = ds;
      init();
   }

   /**
    * check if connection is not null and not closed.
    * 
    * @param conn
    */
   protected void checkConnection(Connection conn) {
      if (conn == null) {
         String msg = "Failed to instantiate JdbcEntityManager: Connection is null";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }

      try {
         if (conn.isClosed()) {
            String msg = "Failed to instantiate JdbcEntityManager: Connection is closed";
            log.error(msg);
            throw new IllegalArgumentException(msg);
         }
      } catch (SQLException e) {
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   /**
    * initializes and configures the definitions of the internal entities and named queries.
    * 
    */
   protected void init() {
      if (isRegistered)
         return;

      ArchiveDefinition arDef = ArchiveDefinition.getInstance();
      ControllableDefinition dcDef = ControllableDefinition.getInstance();
      // LockedObjectDefinition loDef = LockedObjectDefinition.getInstance();
      EventResultDefinition erDef = EventResultDefinition.getInstance();
      ResourceDefinition rDef = ResourceDefinition.getInstance();

      registerEntityDefinition(Archive.class, arDef);
      registerEntityDefinition(Controllable.class, dcDef);
      // registerEntityDefinition(LockedObject.class, loDef);
      registerEntityDefinition(EventResult.class, erDef);
      registerEntityDefinition(JdbcResource.class, rDef);
      isRegistered = true;
   }

   @Override
   public void clear() {
      log.warn("call to JdbcEntityManager.clear ignored");
   }

   @Override
   public void close() {
      try {
         if (jdbcConnection != null) {
            jdbcConnection.close();
            log.debug(jdbcConnection + " closed");
         }
      } catch (SQLException e) {
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public boolean contains(Object arg0) {
      log.warn("call to JdbcEntityManager.contains returns always false");
      return false;
   }

   @Override
   public Query createNamedQuery(String qname) {
      return createJdbcBridgeQuery(qname);
   }

   @Override
   public Query createNativeQuery(String nQuery) {
      if (!queryDefinitions.containsKey(nQuery)) {
         PseudoEntityDefinition pseudo = new PseudoEntityDefinition(nQuery);
         queryDefinitions.put(nQuery, pseudo);
      }

      return createJdbcBridgeQuery(nQuery);
   }

   /**
    * The class parameter is ignored.
    */
   @Override
   public Query createNativeQuery(String nQuery, Class clazz) {
      EntityDefinition def = entityDefinitions.get(clazz);
      if (def == null) {
         return createNativeQuery(nQuery);
      } else {
         queryDefinitions.put(nQuery, def);
         def.getQueries().put(nQuery, nQuery);
         return createJdbcBridgeQuery(nQuery);
      }
   }

   /**
    * The second String parameter is ignored.
    */
   @Override
   public Query createNativeQuery(String nQuery, String arg1) {
      return createNativeQuery(nQuery);
   }

   @Override
   public Query createQuery(String query) {
      return createJdbcBridgeQuery(query);
   }

   /**
    * Not used when JDBC sensor is not used. for JDBC EntityManager no update/delete/insert possible.
    */
   @Override
   public <T> T find(Class<T> clazz, Object primaryKey) {
      try {
         EntityDefinition def = entityDefinitions.get(clazz);
         if (def == null) {
            String msg = "Failed to find object: "
                  + "No EntityDefinition registered in JdbcBridgeEntityManager for entity " + clazz.getName();
            log.error(msg);
            throw new CibetJdbcException(msg);
         }

         Connection conn1 = getNativeConnection();
         try {
            return def.find(conn1, clazz, primaryKey);
         } catch (ClassCastException e) {
            throw new IllegalArgumentException(e);
         } finally {
            if (conn1 != null && datasource != null)
               finalizeConnection(conn1);
         }

      } catch (SQLException e) {
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public void flush() {
      log.warn("call to JdbcEntityManager.flush ignored");
   }

   /**
    * returns either the underlying database connection or the DataSource.
    */
   @Override
   public Object getDelegate() {
      if (jdbcConnection != null) {
         return jdbcConnection;
      } else {
         return datasource;
      }
   }

   @Override
   public FlushModeType getFlushMode() {
      log.warn("call to JdbcEntityManager.getFlushMode ignored");
      return null;
   }

   @Override
   public <T> T getReference(Class<T> arg0, Object arg1) {
      log.warn("call to JdbcEntityManager.getReference ignored");
      return null;
   }

   @Override
   public EntityTransaction getTransaction() {
      return new JdbcBridgeEntityTransaction(jdbcConnection);
   }

   @Override
   public boolean isOpen() {
      if (jdbcConnection == null)
         return false;
      try {
         return !jdbcConnection.isClosed();
      } catch (SQLException e) {
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public void joinTransaction() {
      log.warn("call to JdbcEntityManager.joinTransaction ignored");
   }

   @Override
   public void lock(Object arg0, LockModeType arg1) {
      log.warn("call to JdbcEntityManager.lock ignored");
   }

   @Override
   public <T> T merge(T obj) {
      if (obj == null) {
         throw new IllegalArgumentException("Failed to update object: object is null");
      }
      EntityDefinition def = entityDefinitions.get(obj.getClass());
      if (def == null) {
         String msg = "Failed to update object: "
               + "No EntityDefinition registered in JdbcBridgeEntityManager for entity " + obj.getClass();
         log.error(msg);
         throw new CibetJdbcException(msg);
      }

      try {
         Connection conn1 = getNativeConnection();
         try {
            return def.merge(conn1, obj);
         } finally {
            if (conn1 != null && datasource != null)
               finalizeConnection(conn1);
         }
      } catch (SQLException e) {
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public void persist(Object obj) {
      if (obj == null) {
         throw new IllegalArgumentException("Failed to persist object: object is null");
      }
      EntityDefinition def = entityDefinitions.get(obj.getClass());
      if (def == null) {
         String msg = "Failed to persist object: "
               + "No EntityDefinition registered in JdbcBridgeEntityManager for entity " + obj.getClass();
         log.error(msg);
         throw new CibetJdbcException(msg);
      }

      try {
         Connection conn1 = getNativeConnection();
         try {
            def.persist(conn1, obj);
         } finally {
            if (conn1 != null && datasource != null)
               finalizeConnection(conn1);
         }
      } catch (SQLException e) {
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public void refresh(Object obj) {
      try {
         Object id = AnnotationUtil.valueFromAnnotation(obj, Id.class);
         Object refreshed = find(obj.getClass(), id);
         if (refreshed != null) {
            BeanUtilsBean.getInstance().getConvertUtils().register(false, false, 0);
            BeanUtils.copyProperties(obj, refreshed);
         }
      } catch (AnnotationNotFoundException e) {
         log.warn("call to JdbcEntityManager.refresh ignored: " + e.getMessage());
      } catch (IllegalAccessException e) {
         log.warn("call to JdbcEntityManager.refresh ignored: " + e.getMessage());
      } catch (InvocationTargetException e) {
         log.warn("call to JdbcEntityManager.refresh ignored: " + e.getMessage());
      }
   }

   @Override
   public void remove(Object obj) {
      if (obj == null) {
         throw new IllegalArgumentException("Failed to remove object: object is null");
      }
      EntityDefinition def = entityDefinitions.get(obj.getClass());
      if (def == null) {
         String msg = "Failed to remove object: "
               + "No EntityDefinition registered in JdbcBridgeEntityManager for entity " + obj.getClass();
         log.error(msg);
         throw new CibetJdbcException(msg);
      }

      try {
         Connection conn = getNativeConnection();
         try {
            def.remove(conn, obj);
         } finally {
            if (conn != null && datasource != null)
               finalizeConnection(conn);
         }
      } catch (SQLException e) {
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public void setFlushMode(FlushModeType arg0) {
      log.warn("call to JdbcEntityManager.setFlushMode ignored");
   }

   private JdbcBridgeQuery createJdbcBridgeQuery(String queryName) {
      return new JdbcBridgeQuery(this, queryName);
   }

   public Connection getNativeConnection() throws SQLException {
      Connection con;
      if (datasource != null) {
         con = datasource.getConnection();
         log.debug("Connection=" + con + ", autocommit =" + con.getAutoCommit() + ", class= " + con.getClass());
      } else {
         con = jdbcConnection;
      }
      if (con instanceof CibetConnection) {
         return ((CibetConnection) con).getNativeConnection();
      } else {
         return con;
      }
   }

   public void finalizeConnection(Connection conn) throws SQLException {
      if (conn != null) {
         if (datasource != null) {
            log.debug("close now connection");
            conn.close();
         }
      }
   }

   public <T> TypedQuery<T> createNamedQuery(String queryName, Class<T> arg1) {
      return new JdbcBridgeTypedQuery<T>(this, queryName, arg1);
   }

   @Override
   public <T> TypedQuery<T> createQuery(CriteriaQuery<T> arg0) {
      log.warn("call to JdbcEntityManager.createQuery ignored");
      return new JdbcBridgeTypedQuery<T>(this, "", null);
   }

   @Override
   public <T> TypedQuery<T> createQuery(String arg0, Class<T> arg1) {
      return new JdbcBridgeTypedQuery<T>(this, arg0, arg1);
   }

   @Override
   public void detach(Object arg0) {
      log.warn("call to JdbcEntityManager.detach ignored");
   }

   @Override
   public <T> T find(Class<T> arg0, Object arg1, Map<String, Object> arg2) {
      log.warn("additional properties ignored");
      return find(arg0, arg1);
   }

   @Override
   public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2) {
      log.warn("LockModeType ignored");
      return find(arg0, arg1);
   }

   @Override
   public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2, Map<String, Object> arg3) {
      log.warn("LockModeType and additional properties ignored");
      return find(arg0, arg1);
   }

   @Override
   public CriteriaBuilder getCriteriaBuilder() {
      log.warn("call to JdbcEntityManager.getCriteriaBuilder ignored");
      return null;
   }

   @Override
   public EntityManagerFactory getEntityManagerFactory() {
      return entityManagerFactory;
   }

   public void setEntityManagerFactory(EntityManagerFactory emf) {
      entityManagerFactory = emf;
   }

   @Override
   public LockModeType getLockMode(Object arg0) {
      log.warn("call to JdbcEntityManager.getLockMode ignored");
      return null;
   }

   @Override
   public Metamodel getMetamodel() {
      log.warn("call to JdbcEntityManager.getMetamodel ignored");
      return null;
   }

   @Override
   public Map<String, Object> getProperties() {
      log.warn("call to JdbcEntityManager.getProperties ignored");
      return null;
   }

   @Override
   public void lock(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
      log.warn("call to JdbcEntityManager.lock ignored");
   }

   @Override
   public void refresh(Object arg0, Map<String, Object> arg1) {
      log.warn("call to JdbcEntityManager.refresh ignored");
   }

   @Override
   public void refresh(Object arg0, LockModeType arg1) {
      log.warn("call to JdbcEntityManager.refresh ignored");
   }

   @Override
   public void refresh(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
      log.warn("call to JdbcEntityManager.refresh ignored");
   }

   @Override
   public void setProperty(String arg0, Object arg1) {
      log.warn("call to JdbcEntityManager.setProperty ignored");
   }

   @Override
   public <T> T unwrap(Class<T> arg0) {
      log.warn("call to JdbcEntityManager.unwrap ignored");
      return null;
   }

}
