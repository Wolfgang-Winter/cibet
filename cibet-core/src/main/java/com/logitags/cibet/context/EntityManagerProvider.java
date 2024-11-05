package com.logitags.cibet.context;

import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@RequestScoped
public class EntityManagerProvider {

   @PersistenceContext(unitName = "Cibet")
   private EntityManager entityManager;

   public EntityManager getEntityManager() {
      return entityManager;
   }

}
