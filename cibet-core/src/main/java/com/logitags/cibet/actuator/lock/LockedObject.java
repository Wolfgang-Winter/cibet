package com.logitags.cibet.actuator.lock;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ControlEvent;

/**
 * Represents an item that is locked for a control event.
 * 
 */
@Entity
@Table(name = "CIB_LOCKEDOBJECT")
@NamedQueries({
      @NamedQuery(name = LockedObject.SEL_ALL, query = "SELECT a FROM LockedObject a WHERE a.tenant = :tenant"),
      @NamedQuery(name = LockedObject.SEL_LOCKED_ALL, query = "SELECT a FROM LockedObject a WHERE a.tenant = :tenant AND a.lockState = com.logitags.cibet.actuator.lock.LockState.LOCKED"),
      @NamedQuery(name = LockedObject.SEL_LOCKED_BY_USER, query = "SELECT a FROM LockedObject a WHERE a.tenant = :tenant AND a.lockState = com.logitags.cibet.actuator.lock.LockState.LOCKED AND a.lockedBy = :user"),
      @NamedQuery(name = LockedObject.SEL_LOCKED_BY_TARGETTYPE, query = "SELECT a FROM LockedObject a WHERE a.tenant = :tenant AND a.targetType = :targetType AND a.lockState = com.logitags.cibet.actuator.lock.LockState.LOCKED"),
      @NamedQuery(name = LockedObject.SEL_LOCKED_BY_TARGETTYPE_METHOD, query = "SELECT a FROM LockedObject a WHERE a.tenant = :tenant AND a.targetType = :targetType AND a.method = :method AND a.lockState = com.logitags.cibet.actuator.lock.LockState.LOCKED") })
public class LockedObject implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = -4236881943166716313L;

   public static final String SEL_ALL = "com.logitags.cibet.actuator.lock.LockedObject.SEL_ALL";

   public static final String SEL_LOCKED_ALL = "com.logitags.cibet.actuator.lock.LockedObject.SEL_LOCKED_ALL";

   public static final String SEL_LOCKED_BY_TARGETTYPE = "com.logitags.cibet.actuator.lock.LockedObject.SEL_LOCKED_BY_TARGETTYPE";

   public static final String SEL_LOCKED_BY_TARGETTYPE_METHOD = "com.logitags.cibet.actuator.lock.LockedObject.SEL_LOCKED_BY_TARGETTYPE_METHOD";

   public static final String SEL_LOCKED_BY_USER = "com.logitags.cibet.actuator.lock.LockedObject.SEL_LOCKED_BY_USER";

   /**
    * unique ID
    */
   @Id
   private String lockedObjectId;

   /**
    * Could be a class name or a URL.
    */
   private String targetType;

   @Lob
   private byte[] object;

   /**
    * the unique ID of the object which is locked. If null, all objects of the given class are locked.
    */
   @Column(length = 50)
   private String objectId;

   private String tenant;

   /**
    * locked method. Can be null.
    */
   private String method;

   @Enumerated(EnumType.STRING)
   @Column(length = 50)
   private LockState lockState;

   @Temporal(TemporalType.TIMESTAMP)
   private Date lockDate;

   @Temporal(TemporalType.TIMESTAMP)
   private Date unlockDate;

   @Column(length = 50)
   private String lockedBy;

   @Column(length = 50)
   private String unlockedBy;

   @Enumerated(EnumType.STRING)
   @Column(length = 50)
   private ControlEvent lockedEvent;

   private String lockRemark;

   private String unlockRemark;

   @Version
   private int version;

   @PrePersist
   public void prePersist() {
      lockedObjectId = UUID.randomUUID().toString();
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("[LOCKEDOBJECT target type = ");
      b.append(targetType);
      b.append("\nobject id = ");
      b.append(objectId);
      b.append("\nmethod = ");
      b.append(method);
      b.append("\nlocked event = ");
      b.append(lockedEvent);
      b.append("\nlock date = ");
      b.append(lockDate);
      b.append("\nlocked by = ");
      b.append(lockedBy);
      b.append("\nlock remark = ");
      b.append(lockRemark);
      b.append("]");
      return b.toString();
   }

   /**
    * @return the lockedObjectId
    */
   public String getLockedObjectId() {
      return lockedObjectId;
   }

   /**
    * @param lockedObjectId
    *           the lockedObjectId to set
    */
   public void setLockedObjectId(String lockedObjectId) {
      this.lockedObjectId = lockedObjectId;
   }

   /**
    * @return the targetType
    */
   public String getTargetType() {
      return targetType;
   }

   /**
    * @param targetType
    *           the targetType to set
    */
   public void setTargetType(String targetType) {
      this.targetType = targetType;
   }

   /**
    * @return the objectId
    */
   public String getObjectId() {
      return objectId;
   }

   /**
    * @param objectId
    *           the objectId to set
    */
   public void setObjectId(String objectId) {
      this.objectId = objectId;
   }

   /**
    * @return the method
    */
   public String getMethod() {
      return method;
   }

   /**
    * @param method
    *           the method to set
    */
   public void setMethod(String method) {
      this.method = method;
   }

   /**
    * @return the lockState
    */
   public LockState getLockState() {
      return lockState;
   }

   /**
    * @param lockState
    *           the lockState to set
    */
   public void setLockState(LockState lockState) {
      this.lockState = lockState;
   }

   /**
    * @return the lockDate
    */
   public Date getLockDate() {
      return lockDate;
   }

   /**
    * @param lockDate
    *           the lockDate to set
    */
   public void setLockDate(Date lockDate) {
      this.lockDate = lockDate;
   }

   /**
    * @return the unlockDate
    */
   public Date getUnlockDate() {
      return unlockDate;
   }

   /**
    * @param unlockDate
    *           the unlockDate to set
    */
   public void setUnlockDate(Date unlockDate) {
      this.unlockDate = unlockDate;
   }

   /**
    * @return the lockedBy
    */
   public String getLockedBy() {
      return lockedBy;
   }

   /**
    * @param lockedBy
    *           the lockedBy to set
    */
   public void setLockedBy(String lockedBy) {
      this.lockedBy = lockedBy;
   }

   /**
    * @return the unlockedBy
    */
   public String getUnlockedBy() {
      return unlockedBy;
   }

   /**
    * @param unlockedBy
    *           the unlockedBy to set
    */
   public void setUnlockedBy(String unlockedBy) {
      this.unlockedBy = unlockedBy;
   }

   /**
    * @return the lockedEvent
    */
   public ControlEvent getLockedEvent() {
      return lockedEvent;
   }

   /**
    * @param lockedEvent
    *           the lockedEvent to set
    */
   public void setLockedEvent(ControlEvent lockedEvent) {
      this.lockedEvent = lockedEvent;
   }

   /**
    * @return the lockRemark
    */
   public String getLockRemark() {
      return lockRemark;
   }

   /**
    * @param lockRemark
    *           the lockRemark to set
    */
   public void setLockRemark(String lockRemark) {
      this.lockRemark = lockRemark;
   }

   /**
    * @return the unlockRemark
    */
   public String getUnlockRemark() {
      return unlockRemark;
   }

   /**
    * @param unlockRemark
    *           the unlockRemark to set
    */
   public void setUnlockRemark(String unlockRemark) {
      this.unlockRemark = unlockRemark;
   }

   /**
    * @return the tenant
    */
   public String getTenant() {
      return tenant;
   }

   /**
    * @param tenant
    *           the tenant to set
    */
   public void setTenant(String tenant) {
      this.tenant = tenant;
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
    * @return the object
    */
   public byte[] getObject() {
      return object;
   }

   /**
    * @param object
    *           the object to set
    */
   public void setObject(byte[] object) {
      this.object = object;
   }

   /**
    * Return the entity object on which a persistence action is to be performed
    * 
    * @return
    */
   public Object getDecodedObject() {
      return CibetUtil.decode(object);
   }

}
