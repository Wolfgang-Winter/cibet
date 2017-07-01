package com.logitags.cibet.sensor.jpa;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Provider implements PersistenceProvider {

   private Log log = LogFactory.getLog(Provider.class);

   protected static final String PERSISTENCE_PROVIDER_PROPERTY = "javax.persistence.provider";
   private static final String NATIVE_PERSISTENCE_PROVIDER_PROPERTY = "com.logitags.cibet.persistence.provider";
   private static final String ECLIPSELINK_PERSISTENCE_PROVIDER = "org.eclipse.persistence.jpa.PersistenceProvider";
   private static final String SECURE_ECLIPSELINK_PERSISTENCE_PROVIDER = "com.logitags.cibet.sensor.jpa.EclipselinkProvider";
   private static final String EAGERLOADING_PROPERTY = "com.logitags.cibet.persistence.loadEager";

   private static final String PERSISTENCE_FILE = "META-INF/persistence.xml";

   private PersistenceProvider persistenceProvider;

   @Override
   public ProviderUtil getProviderUtil() {
      if (persistenceProvider == null) {
         return new EmptyProviderUtil();
      }
      return persistenceProvider.getProviderUtil();
   }

   @Override
   public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map map) {
      log.info("createContainerEntityManagerFactory");
      logInfo(info, map);

      info = new DelegatingPersistenceUnitInfo(getNativePersistenceProviderClassName(info, map), info);

      boolean loadEager = isLoadEager(info, map);
      persistenceProvider = createNativePersistenceProvider(info, map);
      map = createPersistenceProviderProperty(map, persistenceProvider);
      EntityManagerFactory nativeEntityManagerFactory = persistenceProvider.createContainerEntityManagerFactory(info,
            map);
      return new CibetEntityManagerFactory(nativeEntityManagerFactory, loadEager);
   }

   @Override
   public EntityManagerFactory createEntityManagerFactory(String unitName, Map map) {
      PersistenceUnitInfo info = createPersistenceUnitInfo(unitName);
      if (info == null) {
         log.info("no persistence unit found with name " + unitName);
         return null;
      }

      if (getClass().getName().equals(info.getPersistenceProviderClassName())
            || (map != null && getClass().getName().equals(map.get(PERSISTENCE_PROVIDER_PROPERTY)))) {
         log.info("create resource_local EntityManagerFactory");
         logInfo(info, map);

         boolean loadEager = isLoadEager(info, map);
         persistenceProvider = createNativePersistenceProvider(info, map);
         map = createPersistenceProviderProperty(map, persistenceProvider);
         EntityManagerFactory nativeEntityManagerFactory = persistenceProvider.createEntityManagerFactory(unitName,
               map);
         return new CibetEntityManagerFactory(nativeEntityManagerFactory, loadEager);
      } else {
         log.debug(this.getClass().getName() + " does not match provider for persistence unit " + unitName);
         return null;
      }
   }

   protected PersistenceUnitInfo createPersistenceUnitInfo(String persistenceUnitName) {
      try {
         PersistenceXmlParser persistenceXmlParser = new PersistenceXmlParser();
         for (Enumeration<URL> persistenceFiles = Thread.currentThread().getContextClassLoader()
               .getResources(PERSISTENCE_FILE); persistenceFiles.hasMoreElements();) {
            URL persistenceFile = persistenceFiles.nextElement();
            persistenceXmlParser.parse(persistenceFile);
            if (persistenceXmlParser.containsPersistenceUnitInfo(persistenceUnitName)) {
               PersistenceUnitInfo persistenceUnitInfo = persistenceXmlParser
                     .getPersistenceUnitInfo(persistenceUnitName);
               return persistenceUnitInfo;
            }
         }
         return null;
      } catch (IOException e) {
         throw new PersistenceException(e);
      }
   }

   private PersistenceProvider createNativePersistenceProvider(PersistenceUnitInfo persistenceUnitInfo,
         Map<String, String> map) {
      try {
         String className = getNativePersistenceProviderClassName(persistenceUnitInfo, map);
         if (className == null || className.equals(Provider.class.getName())) {
            throw new PersistenceException(
                  "No persistence provider specified for " + CibetEntityManagerFactory.class.getName()
                        + ". Specify its class name via property \"" + NATIVE_PERSISTENCE_PROVIDER_PROPERTY + "\"");
         }

         Class<?> persistenceProviderClass = null;
         try {
            if (persistenceUnitInfo.getClassLoader() != null) {
               persistenceProviderClass = persistenceUnitInfo.getClassLoader().loadClass(className);
            }
         } catch (ClassNotFoundException e) {
            log.info(className + " class not found with " + persistenceUnitInfo.getClassLoader());
            persistenceProviderClass = Thread.currentThread().getContextClassLoader().loadClass(className);
         }

         if (persistenceProviderClass == null) {
            persistenceProviderClass = Thread.currentThread().getContextClassLoader().loadClass(className);
         }

         // Class<?> persistenceProviderClass = getClassLoader(persistenceUnitInfo).loadClass(className);
         PersistenceProvider provider = (PersistenceProvider) persistenceProviderClass.newInstance();
         return provider;
      } catch (InstantiationException e) {
         throw new PersistenceException(e);
      } catch (IllegalAccessException e) {
         throw new PersistenceException(e);
      } catch (ClassNotFoundException e) {
         throw new PersistenceException(e);
      }
   }

   private String getNativePersistenceProviderClassName(PersistenceUnitInfo persistenceUnitInfo,
         Map<String, String> map) {
      String className = getProperty(NATIVE_PERSISTENCE_PROVIDER_PROPERTY, persistenceUnitInfo, map);
      ;
      if (className == null && persistenceUnitInfo.getPersistenceProviderClassName() != null) {
         className = persistenceUnitInfo.getPersistenceProviderClassName();
      }
      if (ECLIPSELINK_PERSISTENCE_PROVIDER.equals(className)) {
         className = SECURE_ECLIPSELINK_PERSISTENCE_PROVIDER;
      }
      log.debug("native PersistenceProvider class name = " + className);
      return className;
   }

   private boolean isLoadEager(PersistenceUnitInfo persistenceUnitInfo, Map<String, String> map) {
      String loadEager = getProperty(EAGERLOADING_PROPERTY, persistenceUnitInfo, map);
      boolean loadEagerBool = false;
      if (loadEager != null) {
         loadEagerBool = Boolean.parseBoolean(loadEager);
      }
      log.debug("load eager = " + loadEagerBool);
      return loadEagerBool;
   }

   protected String getProperty(String propertyName, PersistenceUnitInfo persistenceUnitInfo, Map<String, String> map) {
      String prop = null;
      if (map != null) {
         prop = map.get(propertyName);
      }
      if (prop == null && persistenceUnitInfo.getProperties() != null) {
         prop = persistenceUnitInfo.getProperties().getProperty(propertyName);
      }

      log.debug(propertyName + " = " + prop);
      return prop;
   }

   private ClassLoader getClassLoader(PersistenceUnitInfo persistenceUnitInfo) {
      log.debug("ClassLoader: " + persistenceUnitInfo.getClassLoader());
      if (persistenceUnitInfo.getClassLoader() != null) {
         return persistenceUnitInfo.getClassLoader();
      }
      return Thread.currentThread().getContextClassLoader();
   }

   private Map<String, String> createPersistenceProviderProperty(Map<String, String> properties,
         PersistenceProvider persistenceProvider) {
      if (properties == null) {
         return Collections.singletonMap(PERSISTENCE_PROVIDER_PROPERTY, persistenceProvider.getClass().getName());
      } else {
         properties = new HashMap<String, String>(properties);
         properties.put(PERSISTENCE_PROVIDER_PROPERTY, persistenceProvider.getClass().getName());
         return properties;
      }
   }

   protected void logInfo(PersistenceUnitInfo info, Map map) {
      if (log.isDebugEnabled()) {
         if (map != null && map.size() > 0) {
            log.debug(this + " properties: ---------------------");
            Iterator<Map.Entry<?, ?>> it = map.entrySet().iterator();
            while (it.hasNext()) {
               Map.Entry<?, ?> entry = it.next();
               log.debug(entry.getKey() + " = " + entry.getValue());
            }
         }

         if (log.isDebugEnabled()) {
            log.debug("PersistenceUnitInfo properties: ---------------------");
            log.debug(info);
         }
      }

   }
}
