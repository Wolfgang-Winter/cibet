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

import com.logitags.cibet.authentication.AuthenticationProvider;
import com.logitags.cibet.authentication.ChainedAuthenticationProvider;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.core.CEntityManager;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.jndi.EjbLookup;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.net.URLEncoder;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import javax.enterprise.inject.spi.CDI;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
      initialize();
   }

   /**
    * creates the EntityManager for Cibet entities. Either EE or SE.
    * 
    */
   static EntityManager getOrCreateEntityManagers() {
      EntityManager entityManager = internalRequestScope().getEntityManager();
      if (entityManager != null) {
         log.debug("EntityManager found in CibetContext: " + entityManager);
         if (requestScope().getProperty("__ENTITYMANAGER_TYPE") == EntityManagerType.RESOURCE_LOCAL && entityManager.isOpen() && !entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().begin();
         }

         return entityManager;
      } else {
         if (EMF != null) {
            entityManager = EMF.createEntityManager();
            if (entityManager instanceof CEntityManager) {
               entityManager = ((CEntityManager)entityManager).getNativeEntityManager();
            }

            requestScope().setProperty("__ENTITYMANAGER_TYPE", EntityManagerType.RESOURCE_LOCAL);
            internalRequestScope().setEntityManager(entityManager);
            entityManager.getTransaction().begin();
            log.debug("EntityManager created from resource-local EntityManagerFactory");
         } else {
            try {
               log.debug("Try EntityManager from CDI bean");
               EntityManagerProvider emProvider = (EntityManagerProvider) CDI.current().select(EntityManagerProvider.class, new Annotation[0]).get();
               entityManager = emProvider.getEntityManager();
               if (entityManager != null) {
                  internalRequestScope().setEntityManager(entityManager);
                  if (requestScope().getProperty("__ENTITYMANAGER_TYPE") == null) {
                     requestScope().setProperty("__ENTITYMANAGER_TYPE", EntityManagerType.JTA);
                  }

                  log.debug("EntityManager created from CDI bean");
               }
            }catch(Exception e){
               log.warn("get EntityManager from CDI Context didn't work");
            }
         }

         if (entityManager == null) {
            try {
               InitialContext context = new InitialContext();
               EntityManagerFactory containerEmf = (EntityManagerFactory)context.lookup("java:comp/env/Cibet");
               entityManager = containerEmf.createEntityManager();
               if (requestScope().getProperty("__ENTITYMANAGER_TYPE") == null) {
                  requestScope().setProperty("__ENTITYMANAGER_TYPE", EntityManagerType.JTA);
               }

               internalRequestScope().setEntityManager(entityManager);
               log.debug("EE EntityManager created from JNDI EntityManagerFactory");
            } catch (NamingException var3) {
               log.info("\n-----------------------------\nEntityManagerFactory for JTA persistence unit Cibet could not be created. If this is NOT a Java EE application this is not an error. Otherwise, the EntityManagerFactory must be made available in JNDI like\n<persistence-unit-ref>\n  <persistence-unit-ref-name>java:comp/env/Cibet</persistence-unit-ref-name>\n  <persistence-unit-name>Cibet</persistence-unit-name>\n</persistence-unit-ref>\n[Original error message: " + var3.getMessage() + "]\n-----------------------------");
            }
         }

         return entityManager;
      }
   }

   private static void initialize() {
      log.info("initialize Cibet context");
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
         Throwable ex = e;
         StringBuffer err = new StringBuffer(e.getMessage());
         while (ex.getCause() != null) {
            err.append(err);
            err.append(" / ");
            err.append(ex.getCause().getMessage());
            ex = ex.getCause();
         }

         EMF = null;
         log.info("\n-----------------------------\n" + "EntityManagerFactory for resource-local persistence unit "
               + LOCAL_PERSISTENCEUNIT + " could not be created. If this is a Java EE application this is NOT an error."
               + "\nWill try to create EntityManagerFactory for JTA persistence unit Cibet later\n[Original error message: "
               + err + "]\n-----------------------------");
      }

   }

   /**
    * starts the Cibet context
    * 
    * @return true if the Context is freshly started by this method call, false if the Context has already been started
    *         before
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
    * @return true if the Context is freshly started by this method call, false if the Context has already been started
    *         before
    */
   public static boolean start(String ejbJndiname, AuthenticationProvider... authProviders) {
      boolean startManaging = false;
      if (!Context.internalRequestScope().isManaged()) {
         log.debug("start fresh new Cibet context");

         // CibetUtil.logStackTrace();

         Context.internalSessionScope().clear();
         Context.internalRequestScope().clear();

         if (ejbJndiname != null) {
            Context.requestScope().setProperty(InternalRequestScope.CONTEXTEJB_JNDINAME, ejbJndiname);
         }
         CibetEEContext ejb = EjbLookup.lookupEjb(ejbJndiname, CibetEEContextEJB.class);
         if (ejb != null) {
            ejb.setCallerPrincipalNameIntoContext();
         }

         Context.internalRequestScope().setManaged(true);
         startManaging = true;
      } else {
         log.debug("join Cibet context");
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

      if (!isEMInitialized) {
         getOrCreateEntityManagers();
         isEMInitialized = true;
      }

      Configuration.instance();
      return startManaging;
   }

   public static void end() {
      // CibetUtil.logStackTrace();

      try {
         EntityManager em = Context.internalRequestScope().getEntityManager();
         if (em != null && em.isOpen()) {
            if (Context.requestScope()
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
               } catch (IllegalStateException e) {
                  // this is a JTA EntityManager
                  log.debug(e.getMessage());
               }
            }

            try {
               log.debug("close EM");
               em.close();
            } catch (IllegalStateException e) {
               log.warn(e.getMessage());
            }
         }
      } finally {
         Context.internalSessionScope().clear();
         Context.internalRequestScope().clear();
         isEMInitialized = false;
         log.debug("Cibet Context ended");
      }
   }

   public static void close() {
      log.debug("close Cibet context");
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
         String secCtx = internalRequestScope().getAuthenticationProvider().createSecurityContextHeader();
         if (secCtx != null && secCtx.length() > 0) {
            b.append(secCtx);
         }

         Iterator var2 = internalSessionScope().getProperties().entrySet().iterator();

         Map.Entry entry;
         byte[] bytes;
         String encodedValue;
         while(var2.hasNext()) {
            entry = (Map.Entry)var2.next();
            if (entry.getValue() != null && entry.getValue() instanceof Serializable) {
               bytes = CibetUtil.encode(entry.getValue());
               encodedValue = Base64.encodeBase64String(bytes);
               if (b.length() > 0) {
                  b.append("&");
               }

               b.append("CS_");
               b.append((String)entry.getKey());
               b.append("=");
               b.append(URLEncoder.encode(encodedValue, "UTF-8"));
               log.debug("encode session value " + (String)entry.getKey() + " = " + entry.getValue());
            }
         }

         var2 = internalRequestScope().getProperties().entrySet().iterator();

         while(var2.hasNext()) {
            entry = (Map.Entry)var2.next();
            if (entry.getValue() != null && entry.getValue() instanceof Serializable && !(entry.getValue() instanceof EntityManager)) {
               bytes = CibetUtil.encode(entry.getValue());
               encodedValue = Base64.encodeBase64String(bytes);
               if (b.length() > 0) {
                  b.append("&");
               }

               b.append("CR_");
               b.append((String)entry.getKey());
               b.append("=");
               b.append(URLEncoder.encode(encodedValue, "UTF-8"));
               log.debug("encode request value " + (String)entry.getKey() + " = " + entry.getValue());
            }
         }

         log.debug("contextHeader: " + b.toString());
         if (b.length() > 8190) {
            log.warn("\nThe Cibet context header value has a length of " + b.length() + "!\nThis may exceed the maximum allowed length of HTTP header fields of some web servers. If you encounter errors when sending HTTP requests with the Cibet context header set, try to increase the maximum header length in the web server configuration\n");
         }

         return b.toString();
      } catch (UnsupportedEncodingException var6) {
         log.error(var6.getMessage(), var6);
         throw new RuntimeException(var6);
      } catch (IOException var7) {
         log.error(var7.getMessage(), var7);
         throw new RuntimeException(var7);
      }
   }

   static {
      initialize();
   }

}
