package com.logitags.cibet.sensor.jpa;

import java.util.Map;

import javax.persistence.Cache;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.context.EntityManagerType;

public class CibetEntityManagerFactory implements EntityManagerFactory {

   private static Log log = LogFactory.getLog(CibetEntityManagerFactory.class);

   private EntityManagerFactory nativeEntityManagerFactory;

   private boolean loadEager;

   private EntityManagerType entityManagerType;

   public CibetEntityManagerFactory(EntityManagerFactory nativeEMF, boolean lEager, EntityManagerType type) {
      if (nativeEMF == null) {
         throw new IllegalArgumentException("entityManagerFactory may not be null");
      }
      nativeEntityManagerFactory = nativeEMF;
      loadEager = lEager;
      entityManagerType = type;
   }

   @Override
   public void close() {
      nativeEntityManagerFactory.close();
   }

   @Override
   public EntityManager createEntityManager() {
      EntityManager em = nativeEntityManagerFactory.createEntityManager();
      log.debug("create new CibetEntityManager with native " + em);
      return new CibetEntityManager(this, em, loadEager);
   }

   @Override
   public EntityManager createEntityManager(Map arg0) {
      EntityManager em = nativeEntityManagerFactory.createEntityManager(arg0);
      log.debug("create new CibetEntityManager with native " + em);
      return new CibetEntityManager(this, em, loadEager);
   }

   @Override
   public boolean isOpen() {
      return nativeEntityManagerFactory.isOpen();
   }

   @Override
   public Cache getCache() {
      return nativeEntityManagerFactory.getCache();
   }

   @Override
   public CriteriaBuilder getCriteriaBuilder() {
      return nativeEntityManagerFactory.getCriteriaBuilder();
   }

   @Override
   public Metamodel getMetamodel() {
      return nativeEntityManagerFactory.getMetamodel();
   }

   @Override
   public PersistenceUnitUtil getPersistenceUnitUtil() {
      return nativeEntityManagerFactory.getPersistenceUnitUtil();
   }

   @Override
   public Map<String, Object> getProperties() {
      return nativeEntityManagerFactory.getProperties();
   }

   /**
    * @return the nativeEntityManagerFactory
    */
   public EntityManagerFactory getNativeEntityManagerFactory() {
      return nativeEntityManagerFactory;
   }

   @Override
   public <T> void addNamedEntityGraph(String arg0, EntityGraph<T> arg1) {
      nativeEntityManagerFactory.addNamedEntityGraph(arg0, arg1);
   }

   @Override
   public void addNamedQuery(String arg0, Query arg1) {
      nativeEntityManagerFactory.addNamedQuery(arg0, arg1);
   }

   @Override
   public EntityManager createEntityManager(SynchronizationType arg0) {
      EntityManager em = nativeEntityManagerFactory.createEntityManager(arg0);
      log.debug("create new CibetEntityManager with native " + em);
      return new CibetEntityManager(this, em, loadEager);
   }

   @Override
   public EntityManager createEntityManager(SynchronizationType arg0, Map arg1) {
      EntityManager em = nativeEntityManagerFactory.createEntityManager(arg0, arg1);
      log.debug("create new CibetEntityManager with native " + em);
      return new CibetEntityManager(this, em, loadEager);
   }

   @Override
   public <T> T unwrap(Class<T> arg0) {
      return nativeEntityManagerFactory.unwrap(arg0);
   }

   /**
    * @return the entityManagerType
    */
   public EntityManagerType getEntityManagerType() {
      return entityManagerType;
   }
}
