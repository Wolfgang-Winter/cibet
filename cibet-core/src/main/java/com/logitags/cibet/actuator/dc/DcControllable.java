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

package com.logitags.cibet.actuator.dc;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.AssociationOverride;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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

import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.diff.Difference;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.resource.ResourceParameter;

/**
 * The pre-release state of a controlled object. This can be an entity or a method invocation.
 */
@Entity
@Table(name = "CIB_DCCONTROLLABLE")
@NamedQueries({
      @NamedQuery(name = DcControllable.SEL_BY_TENANT_CLASS, query = "SELECT a FROM DcControllable a WHERE a.tenant LIKE :tenant AND a.resource.targetType = :oclass AND (a.executionStatus = com.logitags.cibet.core.ExecutionStatus.POSTPONED OR a.executionStatus = com.logitags.cibet.core.ExecutionStatus.FIRST_POSTPONED OR a.executionStatus = com.logitags.cibet.core.ExecutionStatus.FIRST_RELEASED)"),
      @NamedQuery(name = DcControllable.SEL_BY_CLASS, query = "SELECT a FROM DcControllable a WHERE a.resource.targetType = :oclass AND (a.executionStatus = com.logitags.cibet.core.ExecutionStatus.POSTPONED OR a.executionStatus = com.logitags.cibet.core.ExecutionStatus.FIRST_POSTPONED OR a.executionStatus = com.logitags.cibet.core.ExecutionStatus.FIRST_RELEASED)"),
      @NamedQuery(name = DcControllable.SEL_BY_TENANT, query = "SELECT a FROM DcControllable a WHERE a.tenant LIKE :tenant AND (a.executionStatus = com.logitags.cibet.core.ExecutionStatus.POSTPONED OR a.executionStatus = com.logitags.cibet.core.ExecutionStatus.FIRST_POSTPONED OR a.executionStatus = com.logitags.cibet.core.ExecutionStatus.FIRST_RELEASED)"),
      @NamedQuery(name = DcControllable.SEL_BY_GROUPID, query = "SELECT a FROM DcControllable a WHERE a.resource.groupId = :groupId"),
      @NamedQuery(name = DcControllable.SEL_ALL, query = "SELECT a FROM DcControllable a WHERE (a.executionStatus = com.logitags.cibet.core.ExecutionStatus.POSTPONED OR a.executionStatus = com.logitags.cibet.core.ExecutionStatus.FIRST_POSTPONED OR a.executionStatus = com.logitags.cibet.core.ExecutionStatus.FIRST_RELEASED)"),
      @NamedQuery(name = DcControllable.SEL_BY_ID_CLASS, query = "SELECT ob FROM DcControllable ob WHERE ob.resource.primaryKeyId = :objectId AND ob.resource.targetType = :objectClass AND (ob.executionStatus = com.logitags.cibet.core.ExecutionStatus.POSTPONED OR ob.executionStatus = com.logitags.cibet.core.ExecutionStatus.FIRST_POSTPONED OR ob.executionStatus = com.logitags.cibet.core.ExecutionStatus.FIRST_RELEASED)"),
      @NamedQuery(name = DcControllable.SEL_BY_UNIQUEID, query = "SELECT d FROM DcControllable d WHERE d.resource.uniqueId = :uniqueId ORDER BY d.createDate"),
      @NamedQuery(name = DcControllable.SEL_BY_USER, query = "SELECT d FROM DcControllable d WHERE d.createUser = :user AND d.tenant=:tenant"),
      @NamedQuery(name = DcControllable.SEL_BY_USER_NO_TENANT, query = "SELECT d FROM DcControllable d WHERE d.createUser = :user"),
      @NamedQuery(name = DcControllable.SEL_BY_CASEID, query = "SELECT a FROM DcControllable a WHERE a.tenant LIKE :tenant AND a.caseId = :caseId ORDER BY a.createDate"),
      @NamedQuery(name = DcControllable.SEL_BY_CASEID_NO_TENANT, query = "SELECT a FROM DcControllable a WHERE a.caseId = :caseId ORDER BY a.createDate"),
      @NamedQuery(name = DcControllable.SEL_SCHED_BY_TENANT, query = "SELECT a FROM DcControllable a WHERE a.tenant LIKE :tenant AND a.executionStatus = com.logitags.cibet.core.ExecutionStatus.SCHEDULED"),
      @NamedQuery(name = DcControllable.SEL_SCHED, query = "SELECT a FROM DcControllable a WHERE a.executionStatus = com.logitags.cibet.core.ExecutionStatus.SCHEDULED"),
      @NamedQuery(name = DcControllable.SEL_SCHED_BY_TARGETTYPE, query = "SELECT a FROM DcControllable a WHERE a.tenant LIKE :tenant AND a.resource.targetType = :oclass AND a.executionStatus = com.logitags.cibet.core.ExecutionStatus.SCHEDULED"),
      @NamedQuery(name = DcControllable.SEL_SCHED_BY_TARGETTYPE_NO_TENANT, query = "SELECT a FROM DcControllable a WHERE a.resource.targetType = :oclass AND a.executionStatus = com.logitags.cibet.core.ExecutionStatus.SCHEDULED"),
      @NamedQuery(name = DcControllable.SEL_SCHED_BY_DATE, query = "SELECT a FROM DcControllable a WHERE a.actuator = :actuator AND a.executionStatus = com.logitags.cibet.core.ExecutionStatus.SCHEDULED AND a.scheduledDate <= :currentDate") })
public class DcControllable implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private static Log log = LogFactory.getLog(DcControllable.class);

   /**
    * named query
    */
   public final static String SEL_BY_TENANT_CLASS = "com.logitags.cibet.actuator.dc.DcControllable.DCCONTROLLABLE_SEL_BY_TENANT_CLASS";
   public final static String SEL_BY_CLASS = "com.logitags.cibet.actuator.dc.DcControllable.DCCONTROLLABLE_SEL_BY_CLASS";
   /**
    * named query
    */
   public final static String SEL_BY_TENANT = "com.logitags.cibet.actuator.dc.DcControllable.SEL_BY_TENANT";
   public final static String SEL_ALL = "com.logitags.cibet.actuator.dc.DcControllable.SEL_ALL";

   public final static String SEL_BY_ID_CLASS = "com.logitags.cibet.actuator.dc.DcControllable.SEL_BY_ID_CLASS";

   public final static String SEL_BY_CASEID = "com.logitags.cibet.actuator.dc.DcControllable.SEL_BY_CASEID";
   public final static String SEL_BY_CASEID_NO_TENANT = "com.logitags.cibet.actuator.dc.DcControllable.SEL_BY_CASEID_NO_TENANT";

   public final static String SEL_BY_UNIQUEID = "com.logitags.cibet.actuator.dc.DcControllable.SEL_BY_UNIQUEID";

   public final static String SEL_BY_USER = "com.logitags.cibet.actuator.dc.DcControllable.SEL_BY_USER";
   public final static String SEL_BY_USER_NO_TENANT = "com.logitags.cibet.actuator.dc.DcControllable.SEL_BY_USER_NO_TENANT";

   public final static String SEL_SCHED_BY_TENANT = "com.logitags.cibet.actuator.dc.DcControllable.SEL_SCHED_BY_TENANT";
   public final static String SEL_SCHED = "com.logitags.cibet.actuator.dc.DcControllable.SEL_SCHED";

   public final static String SEL_SCHED_BY_TARGETTYPE = "com.logitags.cibet.actuator.dc.DcControllable.SEL_SCHED_BY_TARGETTYPE";
   public final static String SEL_SCHED_BY_TARGETTYPE_NO_TENANT = "com.logitags.cibet.actuator.dc.DcControllable.SEL_SCHED_BY_TARGETTYPE_NO_TENANT";

   public final static String SEL_SCHED_BY_DATE = "com.logitags.cibet.actuator.dc.DcControllable.SEL_SCHED_BY_DATE";
   public final static String SEL_BY_GROUPID = "com.logitags.cibet.actuator.dc.DcControllable.SEL_BY_GROUPID";
   /**
    * unique ID
    */
   @Id
   private String dcControllableId;

   /**
    * user ID who did the first approval in a 6-eyes process. The controlled object must be approved by a third person.
    */
   @Column(name = "FIRSTAPPROVUSER", length = 50)
   private String firstApprovalUser;

   /**
    * Date when a user did the first approval in a 6-eyes process. The controlled object must be approved by a third
    * person.
    */
   @Temporal(TemporalType.TIMESTAMP)
   private Date firstApprovalDate;

   /**
    * the address data of the user who did the first approval in a 6-eyes process. Could be email address or sms number
    * etc.
    */
   @Column(name = "FIRSTAPPROVADDR")
   private String firstApprovalAddress;

   /**
    * remark of the first approving user
    */
   @Column(name = "FIRSTAPPROVREMARK")
   private String firstApprovalRemark;

   /**
    * user ID who did the final approval (release or reject) of a DC process.
    */
   @Column(length = 50)
   private String approvalUser;

   /**
    * the address data of the user who did the final approval in a DC process. Could be email address or sms number etc.
    */
   private String approvalAddress;

   /**
    * Date when a user did the final approval in a DC process.
    */
   @Temporal(TemporalType.TIMESTAMP)
   private Date approvalDate;

   /**
    * remark of the approving user
    */
   private String approvalRemark;

   /**
    * applied dc Actuator
    */
   @Column(length = 100)
   private String actuator;

   /**
    * the type of event that is requested on the object: - unspecified - new instantiation - data modification - entity
    * removal - method invocation
    */
   @Enumerated(EnumType.STRING)
   @Column(length = 50)
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
    * the address data of the user who initiated the control event in a DC process. Could be email address or sms number
    * etc.
    */
   private String createAddress;

   /**
    * remark of the creating user
    */
   private String createRemark;

   /**
    * Date when the business case is scheduled to be executed. If null executed when approved.
    */
   @Temporal(TemporalType.TIMESTAMP)
   private Date scheduledDate;

   /**
    * Date when the business case has been actually executed. With dual control actuators this is the approval date.
    * With ScheduledActuator this is the date when the schedule job has been run.
    */
   @Temporal(TemporalType.TIMESTAMP)
   private Date executionDate;

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

   @Version
   private int version;

   /**
    * status of the DC process.
    */
   @Enumerated(EnumType.STRING)
   @Column(length = 50)
   private ExecutionStatus executionStatus;

   @Embedded
   // @AssociationOverride(name = "parameters", joinTable = @JoinTable(name =
   // "CIB_DCPARAMETER", joinColumns = @JoinColumn(name = "DCCONTROLLABLEID"),
   // inverseJoinColumns = @JoinColumn(name = "PARAMETERID")))
   // @AssociationOverride(name = "parameters", joinTable = @JoinTable(name =
   // "CIB_DCPARAMETER", joinColumns = @JoinColumn(name = "DCCONTROLLABLEID",
   // referencedColumnName = "dcControllableId"), inverseJoinColumns =
   // @JoinColumn(name = "PARAMETERID", referencedColumnName = "parameterId",
   // unique = true)))
   @AssociationOverride(name = "parameters", joinColumns = @JoinColumn(name = "dcControllableId", referencedColumnName = "dcControllableId"))
   private Resource resource;

   @PrePersist
   protected void prePersist() {
      if (resource != null) {
         resource.getUniqueId();
         if (resource.getGroupId() == null) {
            if (Context.requestScope().getGroupId() != null) {
               resource.setGroupId(Context.requestScope().getGroupId());
            } else if (resource.getPrimaryKeyId() != null) {
               resource.setGroupId(resource.getObject() + "-" + resource.getPrimaryKeyId());
            }
         }
      }

      createDate = new Date();
      dcControllableId = UUID.randomUUID().toString();
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append(this.getClass().getName());
      b.append("\nid = ");
      b.append(dcControllableId);
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
      b.append("\nversion = ");
      b.append(version);
      b.append("\napprovalUser = ");
      b.append(approvalUser);
      b.append("\nexecutionStatus = ");
      b.append(executionStatus);
      b.append("\napprovalDate = ");
      b.append(approvalDate);
      b.append("\nscheduledDate = ");
      b.append(scheduledDate);
      b.append("\nactuator = ");
      b.append(actuator);

      return b.toString();
   }

   public void decrypt() {
      if (getResource().isEncrypted()) {
         log.debug("decrypt DcControllable");
         Context.internalRequestScope().getEntityManager().detach(this);
         getResource().decrypt();
      }
   }

   public void encrypt() {
      log.debug("encrypt DcControllable");
      getResource().encrypt();
   }

   /**
    * returns the list of differences for the modified attributes of the entity backed by this DcControllable. Only
    * applicable if the event is UPDATE
    * 
    * @param dc
    * @return
    */
   public List<Difference> getUpdateDifferences() {
      if (resource == null) {
         throw new IllegalStateException("DcControllable has no Resource");
      }
      ResourceParameter rp = resource.getParameter(FourEyesActuator.DIFFERENCES);
      if (rp == null) {
         String err = "Failed to find update differences of " + resource.getTargetType() + " with ID "
               + resource.getPrimaryKeyObject();
         log.error(err);
         throw new IllegalStateException(err);
      }
      return (List<Difference>) rp.getUnencodedValue();
   }

   /**
    * Set unique ID
    * 
    * @param objectId
    *           id the new value of unique ID
    */
   public void setDcControllableId(String objectId) {
      this.dcControllableId = objectId;
   }

   /**
    * Return unique ID
    * 
    * @return String
    */
   public String getDcControllableId() {
      return this.dcControllableId;
   }

   /**
    * Set user ID who did the first approval in a 6-eyes process. The controlled object must be approved by a third
    * person.
    * 
    * @param firstApprovalUserId
    *           the new value of user ID
    */
   public void setFirstApprovalUser(String firstApprovalUserId) {
      this.firstApprovalUser = firstApprovalUserId;
   }

   /**
    * Return user ID who did the first approval in a 6-eyes process. The controlled object must be approved by a third
    * person.
    * 
    * @return String
    */
   public String getFirstApprovalUser() {
      return this.firstApprovalUser;
   }

   /**
    * Set date when a user did the first approval in a 6-eyes process. The controlled object must be approved by a third
    * person.
    * 
    * @param firstApprovalDate
    *           the new value of date
    */
   public void setFirstApprovalDate(Date firstApprovalDate) {
      this.firstApprovalDate = firstApprovalDate;
   }

   /**
    * Return date when a user did the first approval in a 6-eyes process. The controlled object must be approved by a
    * third person.
    * 
    * @return Date
    */
   public Date getFirstApprovalDate() {
      return this.firstApprovalDate;
   }

   /**
    * Return the DC Actuator applied on this object
    * 
    * @return the Actuator
    */
   public String getActuator() {
      return actuator;
   }

   /**
    * Set the DC Actuator applied on this object
    * 
    * @param actuator
    *           the Actuator to set
    */
   public void setActuator(String actuator) {
      this.actuator = actuator;
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
    *           the caseId to set
    */
   public void setCaseId(String incidentId) {
      this.caseId = incidentId;
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
    * @return the firstApprovalAddress
    */
   public String getFirstApprovalAddress() {
      return firstApprovalAddress;
   }

   /**
    * @param firstApprovalAddress
    *           the firstApprovalAddress to set
    */
   public void setFirstApprovalAddress(String firstApprovalAddress) {
      this.firstApprovalAddress = firstApprovalAddress;
   }

   /**
    * @return the approvalUser
    */
   public String getApprovalUser() {
      return approvalUser;
   }

   /**
    * @param approvalUser
    *           the approvalUser to set
    */
   public void setApprovalUser(String approvalUser) {
      this.approvalUser = approvalUser;
   }

   /**
    * @return the approvalAddress
    */
   public String getApprovalAddress() {
      return approvalAddress;
   }

   /**
    * @param approvalAddress
    *           the approvalAddress to set
    */
   public void setApprovalAddress(String approvalAddress) {
      this.approvalAddress = approvalAddress;
   }

   /**
    * @return the approvalDate
    */
   public Date getApprovalDate() {
      return approvalDate;
   }

   /**
    * @param approvalDate
    *           the approvalDate to set
    */
   public void setApprovalDate(Date approvalDate) {
      this.approvalDate = approvalDate;
   }

   /**
    * @return the createAddress
    */
   public String getCreateAddress() {
      return createAddress;
   }

   /**
    * @param createAddress
    *           the createAddress to set
    */
   public void setCreateAddress(String createAddress) {
      this.createAddress = createAddress;
   }

   /**
    * @return the approvalStatus
    */
   public ExecutionStatus getExecutionStatus() {
      return executionStatus;
   }

   /**
    * @param approvalStatus
    *           the approvalStatus to set
    */
   public void setExecutionStatus(ExecutionStatus approvalStatus) {
      this.executionStatus = approvalStatus;
   }

   /**
    * @return the firstApprovalRemark
    */
   public String getFirstApprovalRemark() {
      return firstApprovalRemark;
   }

   /**
    * @param firstApprovalRemark
    *           the firstApprovalRemark to set
    */
   public void setFirstApprovalRemark(String firstApprovalRemark) {
      this.firstApprovalRemark = firstApprovalRemark;
   }

   /**
    * @return the approvalRemark
    */
   public String getApprovalRemark() {
      return approvalRemark;
   }

   /**
    * @param approvalRemark
    *           the approvalRemark to set
    */
   public void setApprovalRemark(String approvalRemark) {
      this.approvalRemark = approvalRemark;
   }

   /**
    * @return the createRemark
    */
   public String getCreateRemark() {
      return createRemark;
   }

   /**
    * @param createRemark
    *           the createRemark to set
    */
   public void setCreateRemark(String createRemark) {
      this.createRemark = createRemark;
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

   /**
    * @return the scheduledDate
    */
   public Date getScheduledDate() {
      return scheduledDate;
   }

   /**
    * @param scheduledDate
    *           the scheduledDate to set
    */
   public void setScheduledDate(Date scheduledDate) {
      this.scheduledDate = scheduledDate;
   }

   /**
    * @return the executionDate
    */
   public Date getExecutionDate() {
      return executionDate;
   }

   /**
    * @param executionDate
    *           the executionDate to set
    */
   public void setExecutionDate(Date executionDate) {
      this.executionDate = executionDate;
   }

}
