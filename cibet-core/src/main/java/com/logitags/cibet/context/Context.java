/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2012 Dr. Wolfgang Winter
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

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Map.Entry;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.authentication.AuthenticationProvider;
import com.logitags.cibet.authentication.ChainedAuthenticationProvider;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.core.CEntityManager;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.jndi.EjbLookup;

/**
 * abstract class for retrieving Cibet contexts.
 * 
 * @author Wolfgang
 * 
 */
public abstract class Context {

   private static Log log = LogFactory.getLog(Context.class);

   public static final String PREFIX_REQUEST = "CR_";
   public static final String PREFIX_SESSION = "CS_";
   public static final String PARAM_SECURITYCONTEXT = "SEC_";

   private static InternalSessionScope sessionScope;

   private static InternalRequestScope requestScope;

   private static ApplicationScope applicationScope;

   private static String LOCAL_PERSISTENCEUNIT = "CibetLocal";
   private static final String EMF_JNDINAME = "java:comp/env/Cibet";

   private static EntityManagerFactory EMF;

   private static boolean isEMInitialized = false;

   static {
      log.info("start Cibet InitializationService");
      try {
         EMF = Persistence.createEntityManagerFactory(LOCAL_PERSISTENCEUNIT);
         if (EMF != null) {
            // OpenJPA instantiates an EMF even if there is no persistence.xml, therefore check:
            EMF.getProperties();
            log.info("EntityManagerFactory for resource-local persistence unit " + LOCAL_PERSISTENCEUNIT + " created: "
                  + EMF);
         } else {
            log.info(
                  "no EntityManagerFactory for resource-local persistence unit " + LOCAL_PERSISTENCEUNIT + " created!");
         }
      } catch (Exception e) {
         EMF = null;
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
   static EntityManager getOrCreateEntityManagers() {
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
            if (Context.requestScope().getProperty(InternalRequestScope.ENTITYMANAGER_TYPE) == null) {
               Context.requestScope().setProperty(InternalRequestScope.ENTITYMANAGER_TYPE, EntityManagerType.JTA);
            }
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
      }
      return entityManager;
   }

   /**
    * starts the Cibet context
    * 
    * @return
    */
   public static boolean start() {
      return start(null);
   }

   /**
    * Clears ThreadLocals in case they have not been removed in thread before giving back to thread pool. Initializes
    * EntityManager and AuthenticationProvider.
    * 
    * @param ejbJndiname
    * @param authProviders
    * @return
    */
   public static boolean start(String ejbJndiname, AuthenticationProvider... authProviders) {
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
               log.info("register AuthenticationProvider " + a.getClass());
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

   public static void end() {
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

   public static void close() {
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

   /**
    * returns the application scope context.
    * 
    * @return
    */
   public static synchronized ApplicationScope applicationScope() {
      if (applicationScope == null) {
         applicationScope = new ApplicationScopeContext();
      }
      return applicationScope;
   }

   /**
    * returns the session scope context.
    * 
    * @return
    */
   public static synchronized SessionScope sessionScope() {
      if (sessionScope == null) {
         sessionScope = new SessionScopeContext();
      }
      return sessionScope;
   }

   /**
    * returns the request scope context.
    * 
    * @return
    */
   public static synchronized RequestScope requestScope() {
      if (requestScope == null) {
         requestScope = new RequestScopeContext();
      }
      return requestScope;
   }

   /**
    * returns the internal session scope context. Only called internally by the framework
    * 
    * @return
    */
   public static synchronized InternalSessionScope internalSessionScope() {
      if (sessionScope == null) {
         sessionScope = new SessionScopeContext();
      }
      return sessionScope;
   }

   /**
    * returns the internal request scope context. Only called internally by the framework
    * 
    * @return
    */
   public static synchronized InternalRequestScope internalRequestScope() {
      if (requestScope == null) {
         requestScope = new RequestScopeContext();
      }
      return requestScope;
   }

   /**
    * returns the encoded context as a String. This includes the request and session scope contexts and the security
    * context provided by Spring Security or Apache Shiro. The encoded context may be set as HTTP request header with
    * name CIBET_CONTEXT in order to transfer context from the application to the CibetProxy sensor.
    * 
    * @return
    */
   public static String encodeContext() {
      StringBuffer b = new StringBuffer();
      try {
         String secCtx = Context.internalRequestScope().getAuthenticationProvider().createSecurityContextHeader();
         if (secCtx != null && secCtx.length() > 0) {
            b.append(secCtx);
         }

         for (Entry<String, Object> entry : Context.internalSessionScope().getProperties().entrySet()) {
            if (entry.getValue() != null && entry.getValue() instanceof Serializable) {
               byte[] bytes = CibetUtil.encode(entry.getValue());
               String encodedValue = Base64.encodeBase64String(bytes);
               if (b.length() > 0) {
                  b.append("&");
               }
               b.append(PREFIX_SESSION);
               b.append(entry.getKey());
               b.append("=");
               b.append(URLEncoder.encode(encodedValue, "UTF-8"));
               log.debug("encode session value " + entry.getKey() + " = " + entry.getValue());
            }
         }

         for (Entry<String, Object> entry : Context.internalRequestScope().getProperties().entrySet()) {
            if (entry.getValue() != null && entry.getValue() instanceof Serializable
                  && !(entry.getValue() instanceof EntityManager)) {
               byte[] bytes = CibetUtil.encode(entry.getValue());
               String encodedValue = Base64.encodeBase64String(bytes);
               if (b.length() > 0) {
                  b.append("&");
               }
               b.append(PREFIX_REQUEST);
               b.append(entry.getKey());
               b.append("=");
               b.append(URLEncoder.encode(encodedValue, "UTF-8"));
               log.debug("encode request value " + entry.getKey() + " = " + entry.getValue());
            }
         }

         log.debug("contextHeader: " + b.toString());

         if (b.length() > 8190) {
            log.warn("\nThe Cibet context header value has a length of " + b.length()
                  + "!\nThis may exceed the maximum allowed length of HTTP header fields of some web servers. "
                  + "If you encounter errors when sending HTTP requests with the Cibet context header set, "
                  + "try to increase the maximum header length in the web server configuration\n");
         }

         return b.toString();
      } catch (UnsupportedEncodingException e) {
         log.error(e.getMessage(), e);
         throw new RuntimeException(e);
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new RuntimeException(e);
      }
   }

}
