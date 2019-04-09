package com.logitags.cibet.sensor.jpa;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.metamodel.Metamodel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.circuitbreaker.CircuitBreakerActuator;
import com.logitags.cibet.actuator.common.Actuator;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.EntityManagerType;
import com.logitags.cibet.context.InternalRequestScope;
import com.logitags.cibet.control.Controller;
import com.logitags.cibet.core.AnnotationUtil;
import com.logitags.cibet.core.CEntityManager;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.resource.ParameterType;

public class CibetEntityManager implements EntityManager, CEntityManager {

	/**
	 * logger for tracing
	 */
	private Log log = LogFactory.getLog(CibetEntityManager.class);

	private static final String SENSOR_NAME = "JPA";

	private CibetEntityManagerFactory cibetEmFactory;

	private EntityManager nativeEntityManager;

	private EntityManager nativeEntityManager2;

	JpaSensorExecutor defaultExecutor = new JpaDefaultSensorExecutor();

	static JpaSensorExecutor calledExecutor = null;

	private boolean loadEager;

	public CibetEntityManager(CibetEntityManagerFactory fac, EntityManager em, boolean lEager) {
		this(em, lEager);
		log.debug("create CibetEntityManager with " + fac + ", " + em);
		if (fac == null) {
			throw new IllegalArgumentException("EntityManagerFactory must not be null");
		}
		cibetEmFactory = fac;

		nativeEntityManager2 = fac.getNativeEntityManagerFactory().createEntityManager();
		Context.internalRequestScope().setApplicationEntityManager2(nativeEntityManager2);
	}

	protected CibetEntityManager(EntityManager em, boolean loadEager) {
		if (em == null) {
			throw new IllegalArgumentException("EntityManager must not be null");
		}

		if (em instanceof CibetEntityManager) {
			nativeEntityManager = ((CibetEntityManager) em).getNativeEntityManager();
		} else {
			nativeEntityManager = em;
		}

		this.loadEager = loadEager;
		Context.internalRequestScope().setApplicationEntityManager(this);
	}

	@Override
	public void clear() {
		nativeEntityManager.clear();
	}

	@Override
	public void close() {
		if (log.isDebugEnabled()) {
			log.debug("close EntityManager " + this + " [" + nativeEntityManager + "]");
		}
		nativeEntityManager.close();
		EntityManager em2 = Context.internalRequestScope().getApplicationEntityManager2();
		if (em2 != null) {
			em2.close();
		}
		if (!Context.internalRequestScope().isManaged()) {
			Context.internalSessionScope().clear();
			Context.internalRequestScope().clear();
		}
	}

	@Override
	public boolean contains(Object arg0) {
		return nativeEntityManager.contains(arg0);
	}

	@Override
	public Query createNamedQuery(String name) {
		Query q = nativeEntityManager.createNamedQuery(name);
		return new CibetQuery(q, name, this, QueryType.NAMED_QUERY);
	}

	@Override
	public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
		TypedQuery<T> q = nativeEntityManager.createNamedQuery(name, resultClass);
		return new CibetTypedQuery<T>(q, name, this, QueryType.NAMED_TYPED_QUERY, resultClass);
	}

	@Override
	public Query createNativeQuery(String sql) {
		Query q = nativeEntityManager.createNativeQuery(sql);
		return new CibetQuery(q, sql, this, QueryType.NATIVE_QUERY);
	}

	@Override
	public Query createNativeQuery(String sql, Class resultClass) {
		Query q = nativeEntityManager.createNativeQuery(sql, resultClass);
		return new CibetQuery(q, sql, this, QueryType.NATIVE_TYPED_QUERY, resultClass);
	}

	@Override
	public Query createNativeQuery(String sql, String resultSetMapping) {
		Query q = nativeEntityManager.createNativeQuery(sql, resultSetMapping);
		return new CibetQuery(q, sql, this, QueryType.NATIVE_MAPPED_QUERY, resultSetMapping);
	}

	@Override
	public Query createQuery(String qlString) {
		Query q = nativeEntityManager.createQuery(qlString);
		return new CibetQuery(q, qlString, this, QueryType.QUERY);
	}

	@Override
	public <T> TypedQuery<T> createQuery(CriteriaQuery<T> arg0) {
		TypedQuery<T> q = nativeEntityManager.createQuery(arg0);
		return new CibetTypedQuery<T>(q, "", this, QueryType.CRITERIA_QUERY, arg0.getResultType());
	}

	@Override
	public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
		TypedQuery<T> q = nativeEntityManager.createQuery(qlString, resultClass);
		return new CibetTypedQuery<T>(q, qlString, this, QueryType.TYPED_QUERY, resultClass);
	}

	@Override
	public void detach(Object arg0) {
		nativeEntityManager.detach(arg0);
	}

	void entityManagerIntoContext() {
		if (Context.internalRequestScope().getNullableApplicationEntityManager() == null) {
			Context.internalRequestScope().setApplicationEntityManager(this);
		}
		if (Context.internalRequestScope().getApplicationEntityManager2() == null) {
			Context.internalRequestScope().setApplicationEntityManager2(nativeEntityManager2);
		}
	}

	@Override
	public <T> T find(Class<T> clazz, Object id) {
		log.debug("finding entity with id = " + id);
		return find(clazz, id, null, null);
	}

	@Override
	public <T> T find(Class<T> clazz, Object id, Map<String, Object> props) {
		return find(clazz, id, null, props);
	}

	@Override
	public <T> T find(Class<T> arg0, Object id, LockModeType arg2) {
		return find(arg0, id, arg2, null);
	}

	@Override
	public <T> T find(Class<T> clazz, Object id, LockModeType lockMode, Map<String, Object> props) {
		boolean startManaging = true;
		EventMetadata metadata = null;
		EventResult thisResult = null;

		try {
			startManaging = Context.start();

			entityManagerIntoContext();
			ControlEvent controlEvent = controlEvent(ControlEvent.SELECT);
			JpaResource res = new JpaResource(clazz, id);
			if (props != null) {
				for (Map.Entry<String, Object> entry : props.entrySet()) {
					res.addParameter(entry.getKey(), entry.getValue(), ParameterType.JPA_HINT);
				}
			}

			if (lockMode != null) {
				res.addParameter("JPA_LOCKMODETYPE", lockMode, ParameterType.JPA_LOCKMODETYPE);
			}

			metadata = new EventMetadata(SENSOR_NAME, controlEvent, res);
			Controller.evaluate(metadata);
			thisResult = Context.internalRequestScope().registerEventResult(new EventResult(SENSOR_NAME, metadata));

			T obj = null;
			try {
				for (Actuator actuator : metadata.getActuators()) {
					actuator.beforeEvent(metadata);
				}
				if (metadata.getExecutionStatus() == ExecutionStatus.EXECUTING) {
					metadata.setExecutionStatus(ExecutionStatus.EXECUTED);
					if (!Context.requestScope().isPlaying()) {
						obj = localFind(clazz, id, lockMode, props);
						if (obj != null && loadEager) {
							CibetUtil.loadLazyEntities(obj, clazz);
							List<Object> references = new ArrayList<Object>();
							references.add(obj);
							CibetUtil.deepDetach(obj, references);
						}
						res.setResultObject(obj);
					}
				}

			} catch (Throwable e) {
				log.error(e.getMessage(), e);
				metadata.setExecutionStatus(ExecutionStatus.ERROR);
				Context.requestScope().addRemark(e.getMessage());
				metadata.setException(e);
			}

			for (Actuator actuator : metadata.getActuators()) {
				actuator.afterEvent(metadata);
			}

			// because of 2-man-rule do not return obj directly
			return (T) res.getResultObject();

		} finally {
			doFinally(startManaging, metadata, thisResult);
		}
	}

	private <T> T localFind(Class<T> clazz, Object id, LockModeType lockMode, Map<String, Object> props) {
		if (lockMode == null) {
			if (props == null) {
				return nativeEntityManager.find(clazz, id);
			} else {
				return nativeEntityManager.find(clazz, id, props);
			}
		} else {
			if (props == null) {
				return nativeEntityManager.find(clazz, id, lockMode);
			} else {
				return nativeEntityManager.find(clazz, id, lockMode, props);
			}
		}
	}

	@Override
	public void flush() {
		nativeEntityManager.flush();
	}

	@Override
	public CriteriaBuilder getCriteriaBuilder() {
		return nativeEntityManager.getCriteriaBuilder();
	}

	@Override
	public Object getDelegate() {
		return nativeEntityManager.getDelegate();
	}

	@Override
	public EntityManagerFactory getEntityManagerFactory() {
		return cibetEmFactory;
	}

	@Override
	public FlushModeType getFlushMode() {
		return nativeEntityManager.getFlushMode();
	}

	@Override
	public LockModeType getLockMode(Object arg0) {
		return nativeEntityManager.getLockMode(arg0);
	}

	@Override
	public Metamodel getMetamodel() {
		return nativeEntityManager.getMetamodel();
	}

	@Override
	public Map<String, Object> getProperties() {
		return nativeEntityManager.getProperties();
	}

	@Override
	public <T> T getReference(Class<T> arg0, Object arg1) {
		return nativeEntityManager.getReference(arg0, arg1);
	}

	@Override
	public EntityTransaction getTransaction() {
		return nativeEntityManager.getTransaction();
	}

	@Override
	public boolean isOpen() {
		return nativeEntityManager.isOpen();
	}

	@Override
	public void joinTransaction() {
		nativeEntityManager.joinTransaction();
	}

	@Override
	public void lock(Object arg0, LockModeType arg1) {
		nativeEntityManager.lock(arg0, arg1);
	}

	@Override
	public void lock(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
		nativeEntityManager.lock(arg0, arg1, arg2);
	}

	@Override
	public <T> T merge(T obj) {
		if (obj == null) {
			throw new IllegalArgumentException("object to merge is null");
		}

		boolean startManaging = true;
		EventMetadata metadata = null;
		EventResult thisResult = null;

		try {
			startManaging = Context.start();
			entityManagerIntoContext();
			ControlEvent controlEvent = controlEvent(ControlEvent.UPDATE);
			JpaResource res = new JpaResource(obj);
			metadata = new EventMetadata(SENSOR_NAME, controlEvent, res);
			Controller.evaluate(metadata);
			thisResult = Context.internalRequestScope().registerEventResult(new EventResult(SENSOR_NAME, metadata));

			try {
				for (Actuator actuator : metadata.getActuators()) {
					actuator.beforeEvent(metadata);
				}

				if (Context.requestScope().isPlaying()) {
					if (nativeEntityManager.isOpen()) {
						nativeEntityManager.detach(obj);
					}

				} else {
					if (metadata.getExecutionStatus() == ExecutionStatus.EXECUTING) {
						metadata.setExecutionStatus(ExecutionStatus.EXECUTED);
						T ret = nativeEntityManager.merge(obj);

						// refresh the object into the resource:
						nativeEntityManager.flush();
						res.setUnencodedTargetObject(ret);
					} else {
						if (nativeEntityManager.isOpen()) {
							nativeEntityManager.detach(obj);
						}
					}
				}

			} catch (Throwable e) {
				log.error(e.getMessage(), e);
				metadata.setExecutionStatus(ExecutionStatus.ERROR);
				Context.requestScope().addRemark(e.getMessage());
				metadata.setException(e);
			}

			for (Actuator actuator : metadata.getActuators()) {
				actuator.afterEvent(metadata);
			}

			return (T) res.getUnencodedTargetObject();
		} finally {
			doFinally(startManaging, metadata, thisResult);
		}
	}

	@Override
	public void persist(Object obj) {
		if (obj == null) {
			throw new IllegalArgumentException("object to persist is null");
		}

		if (nativeEntityManager.isOpen() && nativeEntityManager.contains(obj))
			return;
		boolean startManaging = true;
		EventMetadata metadata = null;
		EventResult thisResult = null;

		try {
			startManaging = Context.start();
			entityManagerIntoContext();
			ControlEvent controlEvent = controlEvent(ControlEvent.INSERT);
			JpaResource res = new JpaResource(obj);
			metadata = new EventMetadata(SENSOR_NAME, controlEvent, res);
			Controller.evaluate(metadata);
			thisResult = Context.internalRequestScope().registerEventResult(new EventResult(SENSOR_NAME, metadata));

			try {
				for (Actuator actuator : metadata.getActuators()) {
					actuator.beforeEvent(metadata);
				}
				if (metadata.getExecutionStatus() == ExecutionStatus.EXECUTING) {
					metadata.setExecutionStatus(ExecutionStatus.EXECUTED);
					if (!Context.requestScope().isPlaying()) {
						// executor(metadata).persist(metadata, nativeEntityManager,
						// obj);
						nativeEntityManager.persist(obj);
						// refresh the object into the resource:
						nativeEntityManager.flush();
						res.setPrimaryKeyObject(AnnotationUtil.primaryKeyAsObject(obj));
						res.setUnencodedTargetObject(obj);
						res.setUniqueId(res.createUniqueId());
					}
				}

			} catch (Throwable e) {
				log.error(e.getMessage(), e);
				metadata.setExecutionStatus(ExecutionStatus.ERROR);
				Context.requestScope().addRemark(e.getMessage());
				metadata.setException(e);
			}

			for (Actuator actuator : metadata.getActuators()) {
				actuator.afterEvent(metadata);
			}

		} finally {
			doFinally(startManaging, metadata, thisResult);
		}
	}

	@Override
	public void refresh(Object arg0) {
		if (contains(arg0)) {
			nativeEntityManager.refresh(arg0);
		} else {
			log.warn("Cannot refresh, entity not managed: " + arg0);
		}
	}

	@Override
	public void refresh(Object arg0, Map<String, Object> arg1) {
		nativeEntityManager.refresh(arg0, arg1);
	}

	@Override
	public void refresh(Object arg0, LockModeType arg1) {
		nativeEntityManager.refresh(arg0, arg1);
	}

	@Override
	public void refresh(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
		nativeEntityManager.refresh(arg0, arg1, arg2);
	}

	@Override
	public void remove(Object obj) {
		if (obj == null) {
			throw new IllegalArgumentException("object to remove is null");
		}

		boolean startManaging = true;
		EventMetadata metadata = null;
		EventResult thisResult = null;

		try {
			startManaging = Context.start();
			entityManagerIntoContext();
			ControlEvent controlEvent = controlEvent(ControlEvent.DELETE);
			JpaResource res = new JpaResource(obj);
			metadata = new EventMetadata(SENSOR_NAME, controlEvent, res);
			Controller.evaluate(metadata);
			thisResult = Context.internalRequestScope().registerEventResult(new EventResult(SENSOR_NAME, metadata));

			try {
				for (Actuator actuator : metadata.getActuators()) {
					actuator.beforeEvent(metadata);
				}

				if (metadata.getExecutionStatus() == ExecutionStatus.EXECUTING) {
					metadata.setExecutionStatus(ExecutionStatus.EXECUTED);
					if (!Context.requestScope().isPlaying()) {
						if (!nativeEntityManager.contains(obj)) {
							obj = nativeEntityManager.merge(obj);
						}
						nativeEntityManager.remove(obj);
					}
				}

			} catch (Throwable e) {
				log.error(e.getMessage(), e);
				metadata.setExecutionStatus(ExecutionStatus.ERROR);
				Context.requestScope().addRemark(e.getMessage());
				metadata.setException(e);
			}

			for (Actuator actuator : metadata.getActuators()) {
				actuator.afterEvent(metadata);
			}

		} finally {
			doFinally(startManaging, metadata, thisResult);
		}
	}

	@Override
	public void setFlushMode(FlushModeType arg0) {
		nativeEntityManager.setFlushMode(arg0);
	}

	@Override
	public void setProperty(String arg0, Object arg1) {
		nativeEntityManager.setProperty(arg0, arg1);
	}

	@Override
	public <T> T unwrap(Class<T> arg0) {
		return nativeEntityManager.unwrap(arg0);
	}

	ControlEvent controlEvent(ControlEvent ce) {
		ControlEvent ev = (ControlEvent) Context.internalRequestScope().getProperty(InternalRequestScope.CONTROLEVENT);
		if (ev != null) {
			Context.internalRequestScope().removeProperty(InternalRequestScope.CONTROLEVENT);
			return ev;
		} else {
			return ce;
		}
	}

	void doAfter(EventMetadata metadata, EventResult thisResult) {
		try {
			for (Actuator actuator : metadata.getActuators()) {
				actuator.afterEvent(metadata);
			}

			metadata.evaluateEventExecuteStatus();

		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			if (metadata.getExecutionStatus() == ExecutionStatus.ERROR) {
				Context.requestScope().setRemark(null);
			}

			if (metadata.getExecutionStatus() == ExecutionStatus.EXECUTING) {
				thisResult.setExecutionStatus(ExecutionStatus.EXECUTED);
			} else {
				thisResult.setExecutionStatus(metadata.getExecutionStatus());
			}

			if (!Context.internalRequestScope().isManaged()) {
				Context.internalSessionScope().clear();
				Context.internalRequestScope().clear();
				log.info("Cibet context closed");
			}
		}
	}

	void doFinally(boolean startManaging, EventMetadata metadata, EventResult thisResult) {
		try {
			if (metadata != null) {
				metadata.evaluateEventExecuteStatus();
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			if (metadata != null && thisResult != null) {
				if (metadata.getExecutionStatus() == ExecutionStatus.ERROR) {
					Context.requestScope().setRemark(null);
				}

				if (metadata.getExecutionStatus() == ExecutionStatus.EXECUTING) {
					thisResult.setExecutionStatus(ExecutionStatus.EXECUTED);
				} else {
					thisResult.setExecutionStatus(metadata.getExecutionStatus());
				}
			}

			if (startManaging) {
				Context.end();
			}
		}
	}

	JpaSensorExecutor executor(EventMetadata metadata) {
		if (metadata.getProperties().containsKey(CircuitBreakerActuator.TIMEOUT_KEY)) {
			if (calledExecutor == null) {
				ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
				try {
					classLoader.loadClass("javax.enterprise.concurrent.ManagedExecutorService");
					// calledExecutor=;
				} catch (ClassNotFoundException e) {
					calledExecutor = new JpaCalledSensorExecutor();
				}
				log.info("instantiate " + calledExecutor.getClass().getSimpleName() + " as calledExecutor");

			}
			return calledExecutor;

		} else {
			return defaultExecutor;
		}
	}

	/**
	 * @return the nativeEntityManager
	 */
	@Override
	public EntityManager getNativeEntityManager() {
		return nativeEntityManager;
	}

	/**
	 * @return the loadEager
	 */
	@Override
	public boolean isLoadEager() {
		return loadEager;
	}

	/**
	 * @param loadEager
	 *            the loadEager to set
	 */
	@Override
	public void setLoadEager(boolean loadEager) {
		this.loadEager = loadEager;
	}

	@Override
	public boolean supportsTransactions() {
		return true;
	}

	@Override
	public <T> EntityGraph<T> createEntityGraph(Class<T> arg0) {
		return nativeEntityManager.createEntityGraph(arg0);
	}

	@Override
	public EntityGraph<?> createEntityGraph(String arg0) {
		return nativeEntityManager.createEntityGraph(arg0);
	}

	@Override
	public StoredProcedureQuery createNamedStoredProcedureQuery(String arg0) {
		return nativeEntityManager.createStoredProcedureQuery(arg0);
	}

	@Override
	public Query createQuery(CriteriaUpdate arg0) {
		return nativeEntityManager.createQuery(arg0);
	}

	@Override
	public Query createQuery(CriteriaDelete arg0) {
		return nativeEntityManager.createQuery(arg0);
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String arg0) {
		return nativeEntityManager.createStoredProcedureQuery(arg0);
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String arg0, Class... arg1) {
		return nativeEntityManager.createStoredProcedureQuery(arg0, arg1);
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String arg0, String... arg1) {
		return nativeEntityManager.createStoredProcedureQuery(arg0, arg1);
	}

	@Override
	public EntityGraph<?> getEntityGraph(String arg0) {
		return nativeEntityManager.getEntityGraph(arg0);
	}

	@Override
	public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> arg0) {
		return nativeEntityManager.getEntityGraphs(arg0);
	}

	@Override
	public boolean isJoinedToTransaction() {
		return nativeEntityManager.isJoinedToTransaction();
	}

	@Override
	public EntityManagerType getEntityManagerType() {
		return cibetEmFactory.getEntityManagerType();
	}

}
