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

package com.logitags.cibet.actuator.archive;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import javax.persistence.AssociationOverride;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.dc.ResourceApplyException;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.context.CibetContext;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalRequestScope;
import com.logitags.cibet.core.AnnotationNotFoundException;
import com.logitags.cibet.core.AnnotationUtil;
import com.logitags.cibet.core.CEntityManager;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.security.SecurityProvider;

/**
 * archived entity or method invocation action.
 */
@Entity
@Table(name = "CIB_ARCHIVE")
@NamedQueries({ @NamedQuery(name = Archive.SEL_ALL, query = "SELECT a FROM Archive a ORDER BY a.createDate"),
      @NamedQuery(name = Archive.SEL_BY_PRIMARYKEYID, query = "SELECT a FROM Archive a WHERE a.resource.targetType = :targetType AND a.resource.primaryKeyId = :primaryKeyId ORDER BY a.createDate"),
      @NamedQuery(name = Archive.SEL_BY_GROUPID, query = "SELECT a FROM Archive a WHERE a.resource.groupId = :groupId ORDER BY a.createDate"),
      @NamedQuery(name = Archive.SEL_ALL_BY_TENANT, query = "SELECT a FROM Archive a WHERE a.tenant LIKE :tenant ORDER BY a.createDate"),
      @NamedQuery(name = Archive.SEL_ALL_BY_CASEID, query = "SELECT a FROM Archive a WHERE a.tenant LIKE :tenant AND a.caseId = :caseId ORDER BY a.createDate"),
      @NamedQuery(name = Archive.SEL_ALL_BY_CASEID_NO_TENANT, query = "SELECT a FROM Archive a WHERE a.caseId = :caseId ORDER BY a.createDate"),
      @NamedQuery(name = Archive.SEL_BY_METHODNAME, query = "SELECT a FROM Archive a WHERE a.tenant = :tenant AND a.resource.targetType = :objectClass AND a.resource.method = :methodName ORDER BY a.createDate"),
      @NamedQuery(name = Archive.SEL_BY_METHODNAME_NO_TENANT, query = "SELECT a FROM Archive a WHERE a.resource.targetType = :objectClass AND a.resource.method = :methodName ORDER BY a.createDate"),
      @NamedQuery(name = Archive.SEL_ALL_BY_CLASS, query = "SELECT a FROM Archive a WHERE a.tenant LIKE :tenant AND a.resource.targetType = :targetType ORDER BY a.createDate"),
      @NamedQuery(name = Archive.SEL_ALL_BY_CLASS_NO_TENANT, query = "SELECT a FROM Archive a WHERE a.resource.targetType = :targetType ORDER BY a.createDate") })
public class Archive implements Serializable {

   private static final long serialVersionUID = 1L;

   private static Log log = LogFactory.getLog(Archive.class);

   /**
    * named query
    */
   public final static String SEL_ALL = "ARCHIVE_SEL_ALL";

   /**
    * named query
    */
   public final static String SEL_ALL_BY_TENANT = "ARCHIVE_SEL_ALL_BY_TENANT";

   /**
    * named query
    */
   public final static String SEL_ALL_BY_CLASS = "ARCHIVE_SEL_ALL_BY_CLASS";
   public final static String SEL_ALL_BY_CLASS_NO_TENANT = "ARCHIVE_SEL_ALL_BY_CLASS_NO_TENANT";

   /**
    * named query
    */
   public final static String SEL_ALL_BY_CASEID = "ARCHIVE_SEL_ALL_BY_CASEID";
   public final static String SEL_ALL_BY_CASEID_NO_TENANT = "ARCHIVE_SEL_ALL_BY_CASEID_NO_TENANT";

   /**
    * named query
    */
   public final static String SEL_BY_METHODNAME = "ARCHIVE_SEL_BY_METHODNAME";
   public final static String SEL_BY_METHODNAME_NO_TENANT = "ARCHIVE_SEL_BY_METHODNAME_NO_TENANT";

   /**
    * named query
    */
   public static final String SEL_BY_PRIMARYKEYID = "ARCHIVE_SEL_BY_PRIMARYKEYID";
   public final static String SEL_BY_GROUPID = "com.logitags.cibet.actuator.archive.Archive.SEL_BY_GROUPID";

   /**
    * unique ID
    * 
    */
   @Id
   private String archiveId;

   /**
    * optional comment by the user who is responsible for this Archive creation. (e.g. why a controlled object has been
    * rejected)
    */
   private String remark;

   /**
    * message digest over the Archive data.
    */
   private String checksum;

   /**
    * the type of event that is requested on the resource.
    */
   @Column(length = 50)
   @Enumerated(EnumType.STRING)
   private ControlEvent controlEvent;

   /**
    * user id who initiated the control event
    */
   @Column(length = 50)
   private String createUser;

   /**
    * Date when the user initiated the control event
    */
   @Temporal(TemporalType.TIMESTAMP)
   private Date createDate;

   /**
    * tenant
    */
   private String tenant;

   /**
    * unique id that identifies the case. A case consists of related dual control data, INVOKE ... event and
    * RELEASE/REJECT events on the same object / method invocation.
    */
   @Column(length = 60)
   private String caseId;

   /**
    * the execution status of the business case.
    */
   @Enumerated(EnumType.STRING)
   @Column(length = 50)
   private ExecutionStatus executionStatus;

   @Embedded
   // @AssociationOverride(name = "parameters", joinTable = @JoinTable(name =
   // "CIB_ARCHIVEPARAMETER", joinColumns = @JoinColumn(name = "ARCHIVEID"),
   // inverseJoinColumns = @JoinColumn(name = "PARAMETERID")))
   // @AssociationOverride(name = "parameters", joinTable = @JoinTable(name =
   // "CIB_ARCHIVEPARAMETER", joinColumns = @JoinColumn(name = "ARCHIVEID",
   // referencedColumnName = "archiveId"), inverseJoinColumns = @JoinColumn(name
   // = "PARAMETERID", referencedColumnName = "parameterId", unique = true)))
   @AssociationOverride(name = "parameters", joinColumns = @JoinColumn(name = "archiveId", referencedColumnName = "archiveId"))
   private Resource resource;

   @Version
   private int version;

   @PrePersist
   public void prePersist() {
      if (resource != null) {
         resource.getUniqueId();
         if (resource.getGroupId() == null) {
            if (Context.requestScope().getGroupId() != null) {
               resource.setGroupId(Context.requestScope().getGroupId());
            } else if (resource.getPrimaryKeyId() != null) {
               resource.setGroupId(resource.getTargetType() + "-" + resource.getPrimaryKeyId());
            }
         }
      }

      createDate = new Date();
      archiveId = UUID.randomUUID().toString();
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append(this.getClass().getName());
      b.append("\nid = ");
      b.append(archiveId);
      b.append("\ncase id = ");
      b.append(caseId);
      b.append("\ncontrolEvent = ");
      b.append(controlEvent);
      b.append("\ncreateDate = ");
      b.append(createDate);
      b.append("\ncreateUser = ");
      b.append(createUser);
      b.append("\ntenant = ");
      b.append(tenant);
      b.append("\nexecutionStatus = ");
      b.append(executionStatus);
      b.append("\nRESOURCE: ");
      b.append(resource);

      return b.toString();
   }

   public void decrypt() {
      if (getResource().isEncrypted()) {
         log.debug("decrypt Archive");
         // OpenJPA workaround:
         getResource().getParameters().size();
         Context.internalRequestScope().getEntityManager().detach(this);
         getResource().decrypt();
      }
   }

   public void encrypt() {
      log.debug("encrypt Archive");
      getResource().encrypt();
   }

   /**
    * concatenates the Archive values for the checkSum.
    * 
    * @return check sum String
    */
   private String createCheckSumString() {
      SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
      StringBuffer b = new StringBuffer();
      b.append(getControlEvent());
      b.append(getCreateDate() != null ? dateFormat.format(getCreateDate()) : "NULL");
      b.append(getCreateUser());
      b.append(getCaseId());
      if (getTenant() != null)
         b.append(getTenant());
      if (remark != null)
         b.append(remark);
      b.append(executionStatus == null ? "" : executionStatus);

      if (resource != null) {
         b.append(resource.createCheckSumString());
      }
      return b.toString();
   }

   public void createChecksum() {
      SecurityProvider secProvider = Configuration.instance().getSecurityProvider();
      String key = secProvider.getCurrentSecretKey();
      getResource().setKeyReference(key);
      // createCheckSumString
      String checkSumString = createCheckSumString();
      // calculateCheckSum
      String checksum = secProvider.createMessageDigest(checkSumString, key);
      setChecksum(checksum);
   }

   public boolean checkChecksum() {
      SecurityProvider secProvider = Configuration.instance().getSecurityProvider();
      String checkSumString = createCheckSumString();
      log.debug(getArchiveId() + ": checkSumString = '" + checkSumString + "'");
      String checksum = secProvider.createMessageDigest(checkSumString, getResource().getKeyReference());
      return checksum.equals(getChecksum());
   }

   /**
    * redo the invocation with same parameters.
    * 
    * @param remark
    *           optional comment
    * @return result or null
    * @throws ResourceApplyException
    *            in case of error
    */
   @CibetContext
   public Object redo(String remark) throws ResourceApplyException {
      ControlEvent originalControlEvent = (ControlEvent) Context.internalRequestScope()
            .getProperty(InternalRequestScope.CONTROLEVENT);
      String originalCaseId = Context.internalRequestScope().getCaseId();
      String originalRemark = Context.internalRequestScope().getRemark();

      try {
         Context.internalRequestScope().setCaseId(getCaseId());
         if (remark != null) {
            Context.internalRequestScope().setRemark(remark);
         }
         Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, ControlEvent.REDO);

         return getResource().getResourceHandler().apply(ControlEvent.REDO);

      } finally {
         Context.internalRequestScope().setCaseId(originalCaseId);
         Context.internalRequestScope().setRemark(originalRemark);
         Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, originalControlEvent);
      }
   }

   /**
    * reverses the data modification action represented by this archive. If the event was an update, the old state is
    * restored. If the event has been removed, the object will be restored.
    * 
    * @param entityManager
    *           EntityManager for updating or inserting the restored object.
    * @param remark
    *           optional remark
    * @return the restored object
    * 
    */
   @CibetContext
   public Object restore(EntityManager entityManager, String remark) {
      Object obj = getResource().getObject();
      if (obj == null || !obj.getClass().isAnnotationPresent(Entity.class)) {
         String msg = "Failed to restore. Archive does not contain an archived JPA entity";
         log.error(msg);
         throw new IllegalStateException(msg);
      }

      ControlEvent originalControlEvent = (ControlEvent) Context.internalRequestScope()
            .getProperty(InternalRequestScope.CONTROLEVENT);
      String originalCaseId = Context.internalRequestScope().getCaseId();
      String originalRemark = Context.internalRequestScope().getRemark();

      try {
         // check if removed
         EntityManager localEM = entityManager;
         if (entityManager instanceof CEntityManager) {
            localEM = ((CEntityManager) entityManager).getNativeEntityManager();
         }

         Object objFromDb = localEM.find(obj.getClass(), getResource().getPrimaryKeyObject());
         // set after find(), otherwise case id is removed:
         Context.internalRequestScope().setCaseId(getCaseId());
         if (remark != null) {
            Context.internalRequestScope().setRemark(remark);
         }
         if (objFromDb == null) {
            // object has been removed, must be persisted again
            resetAllIdAndVersion(obj);
            Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, ControlEvent.RESTORE_INSERT);
            entityManager.persist(obj);
         } else {
            // object exists, must be merged
            // set version to avoid optimistic locking
            try {
               Object version = AnnotationUtil.valueFromAnnotation(objFromDb, Version.class);
               AnnotationUtil.setValueFromAnnotation(obj, Version.class, version);
            } catch (AnnotationNotFoundException e) {
               // ignore if entity has no @Version annotation
            }
            Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, ControlEvent.RESTORE_UPDATE);
            obj = entityManager.merge(obj);
         }

         if (Context.internalRequestScope().getExecutedEventResult().getExecutionStatus() != ExecutionStatus.EXECUTED) {
            obj = null;
         }

         return obj;
      } finally {
         Context.internalRequestScope().setCaseId(originalCaseId);
         Context.requestScope().setRemark(originalRemark);
         Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, originalControlEvent);
      }
   }

   private void resetAllIdAndVersion(Object obj) {
      if (obj == null)
         return;
      resetIdAndVersion(obj);

      Class<?> intClass = obj.getClass();
      while (intClass != null) {
         Field[] f = intClass.getDeclaredFields();
         for (Field field : f) {
            Class<?> type = field.getType();
            try {
               if (Collection.class.isAssignableFrom(type)) {
                  field.setAccessible(true);
                  Collection<Object> colField = (Collection<Object>) field.get(obj);
                  if (colField == null)
                     continue;
                  Iterator<Object> it = colField.iterator();
                  while (it.hasNext()) {
                     resetAllIdAndVersion(it.next());
                  }

               } else if (Map.class.isAssignableFrom(type)) {
                  field.setAccessible(true);
                  Map<Object, Object> map = (Map<Object, Object>) field.get(obj);
                  Iterator<Object> it = map.keySet().iterator();
                  while (it.hasNext()) {
                     resetAllIdAndVersion(it.next());
                  }
                  it = map.values().iterator();
                  while (it.hasNext()) {
                     resetAllIdAndVersion(it.next());
                  }

               } else if (type.isArray()) {
                  Class<?> fieldClass = CibetUtil.arrayClassForName(type.getName());
                  if (!fieldClass.isPrimitive() && (fieldClass.isAnnotationPresent(Entity.class)
                        || fieldClass.isAnnotationPresent(Embeddable.class))) {
                     field.setAccessible(true);
                     for (int i = 0; i < Array.getLength(field.get(obj)); i++) {
                        resetAllIdAndVersion(Array.get(field.get(obj), i));
                     }
                  }

               } else if (!type.isPrimitive()
                     && (type.isAnnotationPresent(Entity.class) || type.isAnnotationPresent(Embeddable.class))) {
                  field.setAccessible(true);
                  resetAllIdAndVersion(field.get(obj));
               }
            } catch (IllegalAccessException e) {
               String msg = "Failed to re-initialise ID attribute: " + e.getMessage();
               log.error(msg, e);
               throw new RuntimeException(msg, e);
            }
         }
         intClass = intClass.getSuperclass();
      }

   }

   private void resetIdAndVersion(Object obj) {
      boolean generatedId = AnnotationUtil.isAnnotationPresent(obj.getClass(), GeneratedValue.class);
      if (generatedId) {
         // set id == null or 0
         try {
            AnnotationUtil.setValueFromAnnotation(obj, Id.class, null);
         } catch (IllegalArgumentException e) {
            AnnotationUtil.setValueFromAnnotation(obj, Id.class, 0);
         }
      }

      try {
         try {
            AnnotationUtil.setValueFromAnnotation(obj, Version.class, null);
         } catch (IllegalArgumentException e) {
            AnnotationUtil.setValueFromAnnotation(obj, Version.class, 0);
         }
      } catch (AnnotationNotFoundException e) {
         // ignore if entity has no @Version annotation
      }
   }

   /**
    * Set unique ID
    * 
    * @param archiveId
    *           the new value of
    */
   public void setArchiveId(String archiveId) {
      this.archiveId = archiveId;
   }

   /**
    * Return unique ID
    * 
    * @return String
    */
   public String getArchiveId() {
      return this.archiveId;
   }

   /**
    * Set optional comment by the user who is responsible for this Archive creation. (e.g. why a controlled object has
    * been rejected)
    * 
    * @param comment
    *           the new value of optional comment
    */
   public void setRemark(String comment) {
      this.remark = comment;
   }

   /**
    * Return optional comment by the user who is responsible for this Archive creation. (e.g. why a controlled object
    * has been rejected)
    * 
    * @return String
    */
   public String getRemark() {
      return this.remark;
   }

   /**
    * Return message digest over the Archive data.
    * 
    * @return the checksum
    */
   public String getChecksum() {
      return checksum;
   }

   /**
    * Set message digest over the Archive data.
    * 
    * @param checksum
    *           the checksum to set
    */
   public void setChecksum(String checksum) {
      this.checksum = checksum;
   }

   /**
    * Set the type of action that is requested on the object
    * 
    * @param type
    *           the new value of the type of action that is requested on the object
    */
   public void setControlEvent(ControlEvent type) {
      this.controlEvent = type;
   }

   /**
    * Return the type of action that is requested on the object
    * 
    * @return enum
    */
   public ControlEvent getControlEvent() {
      return this.controlEvent;
   }

   /**
    * Set user id who edited or initiated the control request
    * 
    * @param userId
    *           the new value of user id
    */
   public void setCreateUser(String userId) {
      this.createUser = userId;
   }

   /**
    * Return user id who edited or initiated the control request
    * 
    * @return String
    */
   public String getCreateUser() {
      return this.createUser;
   }

   /**
    * Set date when the editing user requested the control action
    * 
    * @param date
    *           the new value of date
    */
   public void setCreateDate(Date date) {
      this.createDate = date;
   }

   /**
    * Return date when the editing user requested the control action
    * 
    * @return Date
    */
   public Date getCreateDate() {
      return this.createDate;
   }

   /**
    * Return tenant
    * 
    * @return the tenant
    */
   public String getTenant() {
      return tenant;
   }

   /**
    * Set tenant
    * 
    * @param tenant
    *           the tenant to set
    */
   public void setTenant(String tenant) {
      this.tenant = tenant;
   }

   /**
    * Returns a unique id that identifies the case. A case consists of related dual control data, INVOKE ... event and
    * RELEASE/REJECT events on the same object / method invocation.
    * 
    * @return the caseId
    */
   public String getCaseId() {
      return caseId;
   }

   /**
    * Set a unique id that identifies the case. A case consists of related dual control data, INVOKE ... event and
    * RELEASE/REJECT events on the same object / method invocation.
    * 
    * @param incidentId
    *           the incidentId to set
    */
   public void setCaseId(String incidentId) {
      this.caseId = incidentId;
   }

   /**
    * @return the executionStatus
    */
   public ExecutionStatus getExecutionStatus() {
      return executionStatus;
   }

   /**
    * @param executionStatus
    *           the executionStatus to set
    */
   public void setExecutionStatus(ExecutionStatus executionStatus) {
      this.executionStatus = executionStatus;
   }

   /**
    * @return the version
    */
   public int getVersion() {
      return version;
   }

   /**
    * @param version
    *           the version to set
    */
   public void setVersion(int version) {
      this.version = version;
   }

   /**
    * @return the resource
    */
   public Resource getResource() {
      return resource;
   }

   /**
    * @param resource
    *           the resource to set
    */
   public void setResource(Resource resource) {
      this.resource = resource;
   }

}
