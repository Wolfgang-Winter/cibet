package com.logitags.cibet.sensor.jpa;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.sensor.common.AbstractCalledSensorExecutor;

public class JpaCalledSensorExecutor extends AbstractCalledSensorExecutor implements JpaSensorExecutor {

   private Log log = LogFactory.getLog(JpaCalledSensorExecutor.class);

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
   public void persist(EventMetadata metadata, EntityManager entityManager, Object obj) throws Throwable {
      log.debug(obj);
      call(metadata, new PersistCallable(metadata, entityManager, obj));
      log.debug(obj);
   }

   @Override
   public void remove(EventMetadata metadata, EntityManager entityManager, Object obj) {
      // TODO Auto-generated method stub

   }

}
