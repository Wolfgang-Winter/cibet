package com.logitags.cibet.sensor.jpa;

import java.net.URL;
import java.util.List;
import java.util.Properties;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

public class DelegatingPersistenceUnitInfo implements PersistenceUnitInfo {

	private PersistenceUnitInfo delegate;

	private String persistenceProviderClassName;

	public DelegatingPersistenceUnitInfo(String persistenceProviderClassName,
	      PersistenceUnitInfo delegate) {
		this.persistenceProviderClassName = persistenceProviderClassName;
		if (delegate == null) {
			throw new IllegalArgumentException("delegate may not be null");
		}
		this.delegate = delegate;
	}

	public String getPersistenceProviderClassName() {
		return persistenceProviderClassName;
	}

	public String getPersistenceUnitName() {
		return delegate.getPersistenceUnitName();
	}

	public PersistenceUnitTransactionType getTransactionType() {
		return delegate.getTransactionType();
	}

	public DataSource getJtaDataSource() {
		return delegate.getJtaDataSource();
	}

	public DataSource getNonJtaDataSource() {
		return delegate.getNonJtaDataSource();
	}

	public List<String> getMappingFileNames() {
		return delegate.getMappingFileNames();
	}

	public List<URL> getJarFileUrls() {
		return delegate.getJarFileUrls();
	}

	public URL getPersistenceUnitRootUrl() {
		return delegate.getPersistenceUnitRootUrl();
	}

	public List<String> getManagedClassNames() {
		return delegate.getManagedClassNames();
	}

	public boolean excludeUnlistedClasses() {
		return delegate.excludeUnlistedClasses();
	}

	public SharedCacheMode getSharedCacheMode() {
		return delegate.getSharedCacheMode();
	}

	public ValidationMode getValidationMode() {
		return delegate.getValidationMode();
	}

	public Properties getProperties() {
		return delegate.getProperties();
	}

	public String getPersistenceXMLSchemaVersion() {
		return delegate.getPersistenceXMLSchemaVersion();
	}

	public ClassLoader getClassLoader() {
		return delegate.getClassLoader();
	}

	public void addTransformer(ClassTransformer transformer) {
		delegate.addTransformer(transformer);
	}

	public ClassLoader getNewTempClassLoader() {
		return delegate.getNewTempClassLoader();
	}

}
