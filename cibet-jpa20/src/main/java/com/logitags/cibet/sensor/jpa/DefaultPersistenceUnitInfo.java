package com.logitags.cibet.sensor.jpa;

import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

public class DefaultPersistenceUnitInfo implements PersistenceUnitInfo {

   private String persistenceUnitName;
   private String persistenceProviderClassName;
   private List<String> managedClassNames = new ArrayList<String>();
   private URL persistenceUnitRootUrl;
   private List<URL> jarFileUrls = new ArrayList<URL>();
   private List<String> mappingFileNames = new ArrayList<String>();
   private boolean excludeUnlistedClasses;
   private PersistenceUnitTransactionType persistenceUnitTransactionType;
   private String jtaDataSourceJndiName;
   private String nonJtaDataSourceJndiName;
   private DataSource jtaDataSource;
   private DataSource nonJtaDataSource;
   private Properties properties = new Properties();
   private ClassLoader classLoader;
   private ClassLoader tempClassLoader;
   private List<ClassTransformer> classTransformers = new ArrayList<ClassTransformer>();
   private ValidationMode validationMode;
   private SharedCacheMode sharedCacheMode;
   private String persistenceXMLSchemaVersion;

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("persistenceUnitName: ");
      b.append(persistenceUnitName);
      b.append("\npersistenceProviderClassName: ");
      b.append(persistenceProviderClassName);
      b.append("\npersistenceUnitRootUrl: ");
      b.append(persistenceUnitRootUrl);
      b.append("\npersistenceUnitTransactionType: ");
      b.append(persistenceUnitTransactionType);
      b.append("\njtaDataSource: ");
      b.append(jtaDataSourceJndiName);
      b.append("\nnonJtaDataSource: ");
      b.append(nonJtaDataSourceJndiName);

      Enumeration<?> en = getProperties().propertyNames();
      while (en.hasMoreElements()) {
         String key = (String) en.nextElement();
         b.append("\nProperty: ");
         b.append(key);
         b.append(" = ");
         b.append(getProperties().getProperty(key));
      }

      return b.toString();
   }

   public String getPersistenceUnitName() {
      return persistenceUnitName;
   }

   public void setPersistenceUnitName(String name) {
      persistenceUnitName = name;
   }

   public String getPersistenceProviderClassName() {
      return persistenceProviderClassName;
   }

   public void setPersistenceProviderClassName(String name) {
      persistenceProviderClassName = name;
   }

   public List<String> getManagedClassNames() {
      return managedClassNames;
   }

   public URL getPersistenceUnitRootUrl() {
      return persistenceUnitRootUrl;
   }

   public void setPersistenceUnitRootUrl(URL url) {
      persistenceUnitRootUrl = url;
   }

   public String getJtaDataSourceJndiName() {
      return jtaDataSourceJndiName;
   }

   public void setJtaDataSourceJndiName(String jndiName) {
      jtaDataSourceJndiName = jndiName;
   }

   public String getNonJtaDataSourceJndiName() {
      return nonJtaDataSourceJndiName;
   }

   public void setNonJtaDataSourceJndiName(String jndiName) {
      nonJtaDataSourceJndiName = jndiName;
   }

   public DataSource getJtaDataSource() {
      return jtaDataSource;
   }

   public PersistenceUnitTransactionType getTransactionType() {
      return persistenceUnitTransactionType;
   }

   public void setPersistenceUnitTransactionType(PersistenceUnitTransactionType type) {
      persistenceUnitTransactionType = type;
   }

   public void setJtaDataSource(DataSource dataSource) {
      jtaDataSource = dataSource;
   }

   public DataSource getNonJtaDataSource() {
      if (nonJtaDataSource == null) {
         if (nonJtaDataSourceJndiName == null) {
            throw new IllegalStateException("Missing element <" + PersistenceXmlHandler.NON_JTA_DATA_SOURCE_TAG
                  + "> in persistence unit CibetLocal");
         }

         try {
            Context ctx = new InitialContext();
            nonJtaDataSource = (DataSource) ctx.lookup("java:comp/env/" + nonJtaDataSourceJndiName);
         } catch (NamingException e) {
            throw new IllegalStateException(
                  "JNDI lookup failed of DataSource java:comp/env/" + nonJtaDataSourceJndiName + " as defined in <"
                        + PersistenceXmlHandler.NON_JTA_DATA_SOURCE_TAG + "> in persistence unit CibetLocal");
         }
      }
      return nonJtaDataSource;
   }

   public void setNonJtaDataSource(DataSource dataSource) {
      nonJtaDataSource = dataSource;
   }

   public Properties getProperties() {
      return properties;
   }

   public ClassLoader getClassLoader() {
      return classLoader;
   }

   public void setClassLoader(ClassLoader loader) {
      classLoader = loader;
   }

   public ClassLoader getNewTempClassLoader() {
      return tempClassLoader;
   }

   public void setNewTempClassLoader(ClassLoader tempLoader) {
      tempClassLoader = tempLoader;
   }

   public void addTransformer(ClassTransformer transformer) {
      classTransformers.add(transformer);
   }

   public SharedCacheMode getSharedCacheMode() {
      return sharedCacheMode;
   }

   public void setSharedCacheMode(SharedCacheMode mode) {
      sharedCacheMode = mode;
   }

   public ValidationMode getValidationMode() {
      return validationMode;
   }

   public void setValidationMode(ValidationMode mode) {
      validationMode = mode;
   }

   public String getPersistenceXMLSchemaVersion() {
      return persistenceXMLSchemaVersion;
   }

   public void setPersistenceXMLSchemaVersion(String version) {
      persistenceXMLSchemaVersion = version;
   }

   @Override
   public List<String> getMappingFileNames() {
      return mappingFileNames;
   }

   @Override
   public List<URL> getJarFileUrls() {
      return jarFileUrls;
   }

   @Override
   public boolean excludeUnlistedClasses() {
      return excludeUnlistedClasses;
   }

   public void setExcludeUnlistedClasses(boolean b) {
      excludeUnlistedClasses = b;
   }

}
