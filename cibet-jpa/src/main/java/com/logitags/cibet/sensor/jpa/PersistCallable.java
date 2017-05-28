package com.logitags.cibet.sensor.jpa;

import java.util.concurrent.Callable;

import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.core.AnnotationUtil;
import com.logitags.cibet.core.EventMetadata;

public class PersistCallable implements Callable<Void> {

   private Log log = LogFactory.getLog(PersistCallable.class);

   private EventMetadata metadata;

   private EntityManager entityManager;

   private Object entity;

   public PersistCallable(EventMetadata md, EntityManager em, Object obj) {
      metadata = md;
      entityManager = em;
      entity = obj;
   }

   @Override
   public Void call() throws Exception {
      log.debug("start call for entity " + entity);
      entityManager.persist(entity);
      log.debug(entity);
      if (Thread.interrupted()) {
         log.debug("thread is interrupted");
         return null;
      }
      // refresh the object into the resource:
      entityManager.flush();
      ((JpaResource) metadata.getResource()).setPrimaryKeyObject(AnnotationUtil.primaryKeyAsObject(entity));
      metadata.getResource().setObject(entity);
      return null;
   }

}
