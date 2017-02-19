package com.logitags.cibet.sensor.jpa;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;

import com.logitags.cibet.core.EventMetadata;

public interface JpaSensorExecutor {

   void resultListQuery(EventMetadata metadata, Query query, CibetEntityManager entityManager);

   void singleResultQuery(EventMetadata metadata, Query query, CibetEntityManager entityManager);

   void updateQuery(EventMetadata metadata, Query query);

   void find(EventMetadata metadata, EntityManager entityManager, Class<?> clazz, Object id, LockModeType lockMode,
         Map<String, Object> props, boolean loadEager);

   void merge(EventMetadata metadata, EntityManager entityManager, Object obj);

   void persist(EventMetadata metadata, EntityManager entityManager, Object obj) throws Throwable;

   void remove(EventMetadata metadata, EntityManager entityManager, Object obj);

}
