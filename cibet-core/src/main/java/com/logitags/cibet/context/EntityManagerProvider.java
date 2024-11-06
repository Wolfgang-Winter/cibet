package com.logitags.cibet.context;

import javax.enterprise.context.Dependent;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Dependent
public class EntityManagerProvider {

   @PersistenceContext(unitName = "Cibet")
   private EntityManager entityManager;

   public EntityManager getEntityManager() {
      return entityManager;
   }
}
