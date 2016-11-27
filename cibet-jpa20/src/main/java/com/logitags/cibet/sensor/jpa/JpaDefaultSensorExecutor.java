package com.logitags.cibet.sensor.jpa;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;

import com.logitags.cibet.core.EventMetadata;

public class JpaDefaultSensorExecutor implements JpaSensorExecutor {

   @Override
   public void resultListQuery(EventMetadata metadata, Query query, CibetEntityManager entityManager) {
      // TODO Auto-generated method stub

   }

   @Override
   public void singleResultQuery(EventMetadata metadata, Query query, CibetEntityManager entityManager) {
      // TODO Auto-generated method stub

   }

   @Override
   public void updateQuery(EventMetadata metadata, Query query) {
      // TODO Auto-generated method stub

   }

   @Override
   public void find(EventMetadata metadata, EntityManager entityManager, Class<?> clazz, Object id,
         LockModeType lockMode, Map<String, Object> props, boolean loadEager) {
      // TODO Auto-generated method stub

   }

   @Override
   public void merge(EventMetadata metadata, EntityManager entityManager, Object obj) {
      // TODO Auto-generated method stub

   }

   @Override
   public void persist(EventMetadata metadata, EntityManager entityManager, Object obj) throws Exception {
      PersistCallable callable = new PersistCallable(metadata, entityManager, obj);
      callable.call();
   }

   @Override
   public void remove(EventMetadata metadata, EntityManager entityManager, Object obj) {
      // TODO Auto-generated method stub

   }

}
