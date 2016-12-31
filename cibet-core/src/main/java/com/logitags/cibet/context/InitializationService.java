/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2014 Dr. Wolfgang Winter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************
 */
package com.logitags.cibet.context;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.authentication.AuthenticationProvider;
import com.logitags.cibet.authentication.ChainedAuthenticationProvider;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.core.CEntityManager;
import com.logitags.cibet.jndi.EjbLookup;

public class InitializationService {

   private static Log log = LogFactory.getLog(InitializationService.class);

   private static String LOCAL_PERSISTENCEUNIT = "CibetLocal";
   private static String EMF_JNDINAME = "java:comp/env/Cibet";

   private EntityManagerFactory EMF;

   private static InitializationService instance;

   private boolean isEMInitialized = false;

   public static synchronized InitializationService instance() {
      if (instance == null) {
         instance = new InitializationService();
      }
      return instance;
   }

   private InitializationService() {
      log.info("start Cibet InitializationService");
      try {
         EMF = Persistence.createEntityManagerFactory(LOCAL_PERSISTENCEUNIT);
         if (EMF != null) {
            log.info("EntityManagerFactory for resource-local persistence unit " + LOCAL_PERSISTENCEUNIT + " created: "
                  + EMF);
         } else {
            log.info(
                  "no EntityManagerFactory for resource-local persistence unit " + LOCAL_PERSISTENCEUNIT + " created!");
         }
      } catch (Exception e) {
         log.info("\n-----------------------------\n" + "EntityManagerFactory for resource-local persistence unit "
               + LOCAL_PERSISTENCEUNIT + " could not be created. If this is a Java EE application this is NOT an error."
               + "\nWill try to create EntityManagerFactory for JTA persistence unit Cibet later\n[Original error message: "
               + e.getMessage() + "]\n-----------------------------");
      }
   }

   /**
    * creates the EntityManager for Cibet entities. Either EE or SE.
    * 
    */
   public EntityManager getOrCreateEntityManagers() {
      EntityManager entityManager = Context.internalRequestScope().getNullableEntityManager();
      if (entityManager != null) {
         log.debug("EntityManager found in CibetContext: " + entityManager);
         if (Context.requestScope()
               .getProperty(InternalRequestScope.ENTITYMANAGER_TYPE) == EntityManagerType.RESOURCE_LOCAL
               && entityManager.isOpen() && !entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().begin();
         }
         return entityManager;
      }

      if (EMF != null) {
         entityManager = EMF.createEntityManager();
         if (entityManager instanceof CEntityManager) {
            entityManager = ((CEntityManager) entityManager).getNativeEntityManager();
         }
         Context.requestScope().setProperty(InternalRequestScope.ENTITYMANAGER_TYPE, EntityManagerType.RESOURCE_LOCAL);
         Context.internalRequestScope().setEntityManager(entityManager);
         entityManager.getTransaction().begin();
         log.debug("EntityManager created from resource-local EntityManagerFactory");
      } else {

         try {
            InitialContext context = new InitialContext();
            EntityManagerFactory containerEmf = (EntityManagerFactory) context.lookup(EMF_JNDINAME);
            entityManager = containerEmf.createEntityManager();
            Context.requestScope().setProperty(InternalRequestScope.ENTITYMANAGER_TYPE, EntityManagerType.JTA);
            Context.internalRequestScope().setEntityManager(entityManager);
            log.debug("EE EntityManager created from JNDI EntityManagerFactory");
            return entityManager;
         } catch (NamingException e) {
            log.info("\n-----------------------------\n"
                  + "EntityManagerFactory for JTA persistence unit Cibet could not be created. If this is NOT a Java EE application this "
                  + "is not an error. Otherwise, the EntityManagerFactory must be made available in JNDI like\n"
                  + "<persistence-unit-ref>\n" + "  <persistence-unit-ref-name>" + EMF_JNDINAME
                  + "</persistence-unit-ref-name>\n" + "  <persistence-unit-name>Cibet</persistence-unit-name>\n"
                  + "</persistence-unit-ref>" + "\n[Original error message: " + e.getMessage()
                  + "]\n-----------------------------");
         }

         // CibetEEContext ejb = EjbLookup.lookupEjb(
         // (String)
         // Context.requestScope().getProperty(InternalRequestScope.CONTEXTEJB_JNDINAME),
         // CibetEEContextEJB.class);
         // if (ejb != null) {
         // if (!ejb.setEntityManagerIntoContext()) {
         // log.warn("Failed to create EntityManager from JTA PersistenceUnit
         // Cibet."
         // + " Set an EntityManager manually into CibetContext!");
         // } else {
         // entityManager =
         // Context.internalRequestScope().getNullableEntityManager();
         // }
         // }
      }
      return entityManager;
   }

   /**
    * starts the Cibet context
    * 
    * @return
    */
   public boolean startContext() {
      return startContext(null);
   }

   /**
    * Clears ThreadLocals in case they have not been removed in thread before giving back to thread pool. Initializes
    * EntityManager and AuthenticationProvider.
    * 
    * @param ejbJndiname
    * @param authProviders
    * @return
    */
   public boolean startContext(String ejbJndiname, AuthenticationProvider... authProviders) {
      boolean startManaging = false;
      if (!Context.internalRequestScope().isManaged()) {
         log.info("start fresh new Cibet context");
         Context.internalSessionScope().clear();
         Context.internalRequestScope().clear();
         Context.internalRequestScope().setManaged(true);
         startManaging = true;
      }

      if (authProviders != null) {
         ChainedAuthenticationProvider requestScopeAuthProvider = Context.internalRequestScope()
               .getAuthenticationProvider();
         for (AuthenticationProvider a : authProviders) {
            if (a != null) {
               requestScopeAuthProvider.getProviderChain().add(a);
            }
         }
      }

      if (ejbJndiname != null) {
         Context.requestScope().setProperty(InternalRequestScope.CONTEXTEJB_JNDINAME, ejbJndiname);
      }
      CibetEEContext ejb = EjbLookup.lookupEjb(ejbJndiname, CibetEEContextEJB.class);
      if (ejb != null) {
         ejb.setCallerPrincipalNameIntoContext();
      }

      if (!isEMInitialized) {
         getOrCreateEntityManagers();
         isEMInitialized = true;
      }

      Configuration.instance();
      return startManaging;
   }

   public void endContext() {
      try {
         EntityManager em = Context.internalRequestScope().getNullableEntityManager();
         if (em != null && em.isOpen() && Context.requestScope()
               .getProperty(InternalRequestScope.ENTITYMANAGER_TYPE) == EntityManagerType.RESOURCE_LOCAL) {
            try {
               if (em.getTransaction().isActive()) {
                  if (Context.internalRequestScope().getRollbackOnly()) {
                     log.debug("rollback Cibet");
                     em.getTransaction().rollback();
                  } else {
                     log.debug("commit Cibet");
                     em.getTransaction().commit();
                  }
               }
               em.close();
            } catch (IllegalStateException e) {
               // this is a JTA EntityManager
               log.debug(e.getMessage());
            }
         }
      } finally {
         Context.internalSessionScope().clear();
         Context.internalRequestScope().clear();
         isEMInitialized = false;
         log.info("Cibet Context ended");
      }
   }

   public void close() {
      log.debug("close InitializationService");
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      // Loop through all drivers
      Enumeration<Driver> drivers = DriverManager.getDrivers();
      while (drivers.hasMoreElements()) {
         Driver driver = drivers.nextElement();
         if (driver.getClass().getClassLoader() == cl
               && driver.getClass().getName().equals("com.logitags.cibet.sensor.jdbc.driver.CibetDriver")) {
            // This driver was registered by the webapp's ClassLoader, so
            // deregister it:
            try {
               log.info("Deregistering Cibet JDBC driver");
               DriverManager.deregisterDriver(driver);
            } catch (SQLException ex) {
               log.error("Error deregistering Cibet JDBC driver", ex);
            }
         }
      }

      if (EMF != null) {
         EMF.close();
         EMF = null;
      }
   }

}
