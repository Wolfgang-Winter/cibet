package com.logitags.cibet.context;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@ApplicationScoped
public class EntityManagerProvider {

   @PersistenceContext(unitName = "Cibet")
   private EntityManager entityManager;

   public EntityManager getEntityManager() {
      return entityManager;
   }
}
