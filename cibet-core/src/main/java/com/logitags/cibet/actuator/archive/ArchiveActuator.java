/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 *******************************************************************************
 */
/**
 * 
 */
package com.logitags.cibet.actuator.archive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.common.AbstractActuator;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.AnnotationUtil;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.sensor.ejb.EjbResource;
import com.logitags.cibet.sensor.jpa.JpaResource;

/**
 * creates an archive entry in the database.
 */
public class ArchiveActuator extends AbstractActuator {

   /**
    * 
    */
   private static final long serialVersionUID = -5221476168284114855L;

   private transient Log log = LogFactory.getLog(ArchiveActuator.class);

   public static final String DEFAULTNAME = "ARCHIVE";

   /**
    * optional parameter for EJB sensor. In case cibet could not determine the jndi name of the EJB to invoke a method
    * automatically it must be set explicitly here.
    */
   private String jndiName;

   /**
    * If set to true Cibet creates a message digest for each Archive entry in the database. Data integrity can be
    * checked this way.
    */
   private static boolean integrityCheck = false;

   /**
    * flag to encrypt target, result and parameter values of Resource.
    */
   private boolean encrypt = false;

   /**
    * flag if eager loading of a JPA resource is necessary before storing the Archive.
    */
   private boolean loadEager = true;

   /**
    * list of property names that will be stored as ResourceParameters with the Archive. Only applicable for PERSIST
    * events.
    */
   private Collection<String> storedProperties = new ArrayList<String>();

   public ArchiveActuator() {
      setName(DEFAULTNAME);
   }

   public ArchiveActuator(String name) {
      setName(name);
   }

   /**
    * If set to true Cibet creates a message digest for each Archive entry in the database. Data integrity can be
    * checked this way.
    * 
    * @return the integrityCheck
    */
   public synchronized boolean isIntegrityCheck() {
      return integrityCheck;
   }

   /**
    * If set to true Cibet creates a message digest for each Archive entry in the database. Data integrity can be
    * checked this way.
    * 
    * @param ic
    *           the integrityCheck flag to set
    */
   public synchronized void setIntegrityCheck(boolean ic) {
      integrityCheck = ic;
   }

   private String truncate255(String in) {
      if (in == null || in.length() <= 255) {
         return in;
      } else {
         return in.substring(0, 255);
      }
   }

   private Archive createArchive(EventMetadata ctx) {
      Archive arch = new Archive();
      arch.setRemark(truncate255(Context.requestScope().getRemark()));
      arch.setCaseId(ctx.getCaseId());
      arch.setCreateUser(Context.internalSessionScope().getUser());
      arch.setTenant(Context.internalSessionScope().getTenant());
      arch.setControlEvent(ctx.getControlEvent());
      arch.setExecutionStatus(ctx.getExecutionStatus());
      arch.setResource(ctx.getResource());

      if (ctx.getResource() instanceof EjbResource) {
         ((EjbResource) arch.getResource()).setInvokerParam(jndiName);
      }

      addStoredProperties(arch.getResource(), storedProperties);

      return arch;
   }

   private void updateObjectId(Resource res, String caseId) {
      if (!(res instanceof JpaResource)) {
         return;
      }
      JpaResource jpaRes = (JpaResource) res;

      if (jpaRes.getPrimaryKeyObject() == null) {
         String msg = "no value for primary key found in persisted object " + jpaRes.getUnencodedTargetObject();
         log.warn(msg);
         return;
      }

      // set primary key of previous archives of this business case
      Query q = Context.internalRequestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL_BY_CASEID);
      q.setParameter("tenant", Context.sessionScope().getTenant());
      q.setParameter("caseId", caseId);
      List<Archive> list = q.getResultList();
      for (Archive arch : list) {
         JpaResource resource = (JpaResource) arch.getResource();
         resource.decrypt();

         resource.setPrimaryKeyObject(jpaRes.getPrimaryKeyObject());
         if (!(resource.getUnencodedTargetObject() instanceof String)) {
            Object o = resource.getUnencodedTargetObject();
            AnnotationUtil.setValueFromAnnotation(o, Id.class, jpaRes.getPrimaryKeyObject());
            resource.setUnencodedTargetObject(o);
         }
         resource.setUniqueId(jpaRes.getUniqueId());
         update(arch);
         log.info(arch.getArchiveId() + " archive updated with objectId.");
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.actuator.AbstractActuator#beforeEvent(com.logitags. cibet.core.EventMetadata)
    */
   @Override
   public void beforeEvent(EventMetadata ctx) {
      log.debug("beforeEvent ArchiveActuator");
      if (isLoadEager()) {
         loadEager(ctx);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.core.Actuator#afterEvent(com.logitags.cibet.core. EventMetadata)
    */
   @Override
   public void afterEvent(EventMetadata ctx) {
      if (Context.requestScope().isPlaying()) {
         return;
      }

      if (ctx.getControlEvent() == ControlEvent.RELEASE_INSERT
            && ctx.getExecutionStatus() == ExecutionStatus.EXECUTED) {
         updateObjectId(ctx.getResource(), ctx.getCaseId());
      }

      Archive archive = createArchive(ctx);
      insert(archive);
   }

   private void update(Archive ar) {
      if (isIntegrityCheck()) {
         ar.createChecksum();
      }

      EntityManager em = Context.internalRequestScope().getEntityManager();
      if (isEncrypt()) {
         ar.getResource().encrypt();
         // } else {
         // ar.getResource().setEncrypted(false);
      }

      ar.setResource(em.merge(ar.getResource()));
      em.merge(ar);
   }

   private void insert(Archive ar) {
      if (isIntegrityCheck()) {
         ar.createChecksum();
      }

      EntityManager em = Context.internalRequestScope().getEntityManager();
      if (ar.getResource().getResourceId() == null) {
         if (isEncrypt()) {
            ar.getResource().encrypt();
         }

         em.persist(ar.getResource());
      }

      em.persist(ar);
      log.debug("created Archive with id " + ar.getArchiveId());
      em.flush();
   }

   /**
    * @return the jndiName
    */
   public String getJndiName() {
      return jndiName;
   }

   /**
    * @param jndiName
    *           the jndiName to set
    */
   public void setJndiName(String jndiName) {
      this.jndiName = jndiName;
   }

   /**
    * @return the encrypt
    */
   public boolean isEncrypt() {
      return encrypt;
   }

   /**
    * @param encrypt
    *           the encrypt to set
    */
   public void setEncrypt(boolean encrypt) {
      this.encrypt = encrypt;
   }

   /**
    * @return the storedProperties
    */
   public Collection<String> getStoredProperties() {
      return storedProperties;
   }

   /**
    * @param storedAttributes
    *           the storedProperties to set
    */
   public void setStoredProperties(Collection<String> storedAttributes) {
      this.storedProperties = storedAttributes;
   }

   /**
    * @return the loadEager
    */
   public boolean isLoadEager() {
      return loadEager;
   }

   /**
    * @param loadEager
    *           the loadEager to set
    */
   public void setLoadEager(boolean loadEager) {
      this.loadEager = loadEager;
   }

}
