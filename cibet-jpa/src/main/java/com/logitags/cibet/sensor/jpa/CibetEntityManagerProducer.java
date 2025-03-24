package com.logitags.cibet.sensor.jpa;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.context.EntityManagerType;

public class CibetEntityManagerProducer {

	private Log log = LogFactory.getLog(CibetEntityManagerProducer.class);

	private static final String EAGERLOADING_PROPERTY = "com.logitags.cibet.persistence.loadEager";

	private static Map<String, EntityManagerType> types = new ConcurrentHashMap<>();
	private static Map<String, Boolean> loadEagers = new ConcurrentHashMap<>();


	protected CibetEntityManagerProducer() {
	}

	protected EntityManager getCibetEntityManager(EntityManager providedEM, String persistenceUnit) {
		EntityManagerType type = entityManagerType(providedEM, persistenceUnit);
		boolean loadEager = loadEager(providedEM, persistenceUnit);

		CibetEntityManagerFactory EMF = new CibetEntityManagerFactory(providedEM.getEntityManagerFactory(), loadEager,
				type, persistenceUnit);

		CibetEntityManager cibEM = new CibetEntityManager(EMF, providedEM, loadEager, persistenceUnit);

		return cibEM;
	}

	private EntityManagerType entityManagerType(EntityManager em, String persistenceUnit) {
		if (types.get(persistenceUnit) == null) {
			// test if JTA or resource-local persistenceunit
			try {
				em.getTransaction();
				types.put(persistenceUnit, EntityManagerType.RESOURCE_LOCAL);
			} catch (IllegalStateException e) {
				types.put(persistenceUnit, EntityManagerType.JTA);
			}
		}
		return types.get(persistenceUnit);
	}

	private boolean loadEager(EntityManager em, String persistenceUnit) {
		if (loadEagers.get(persistenceUnit) == null) {
			loadEagers.put(persistenceUnit, isLoadEager(em.getEntityManagerFactory().getProperties()));
		}
		return loadEagers.get(persistenceUnit);
	}

	private boolean isLoadEager(Map<String, Object> map) {
		boolean bool = false;
		String eager = (String) map.get(EAGERLOADING_PROPERTY);
		if (eager != null) {
			bool = Boolean.parseBoolean(eager);
			log.debug("load eager = " + bool);

		}

		return bool;
	}

}
