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
package com.logitags.cibet.core;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.authentication.AbstractAuthenticationProvider;
import com.logitags.cibet.context.Context;

/**
 * Represents the results of an event execution. If a setpoint matches an
 * instance of EventResult is stored in CibetContext. It contains all the
 * details of the applied setpoint.
 * 
 * @author Wolfgang
 * 
 */
@Entity
@Table(name = "CIB_EVENTRESULT")
public class EventResult implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 731538174490706806L;

   private static Log log = LogFactory.getLog(EventResult.class);

   @Id
   private String eventResultId;

   /**
    * the sensor that detected the event.
    */
   @Column(length = 50)
   private String sensor;

   /**
    * comma separated list of the applied actuator names
    */
   private String actuators;

   /**
    * comma separated list of applied setpoints.
    */
   private String setpoints;

   @Temporal(TemporalType.TIMESTAMP)
   private Date executionTime = new Date();

   /**
    * the matching event
    */
   @Enumerated(EnumType.STRING)
   @Column(length = 50)
   private ControlEvent event;

   /**
    * result of the event execution.
    */
   @Enumerated(EnumType.STRING)
   @Column(length = 50)
   private ExecutionStatus executionStatus = ExecutionStatus.EXECUTING;

   /**
    * if during execution of this event a second event is controlled by a
    * setpoint, the result of the second event is added to this list. Empty
    * list, if there is no child controlled event
    */
   @OneToMany(mappedBy = "parentResult", cascade = { CascadeType.ALL })
   private List<EventResult> childResults = new ArrayList<EventResult>();

   /**
    * if this event is executed during execution of another parent event
    * controlled by a setpoint, the result of the parent event is stored in this
    * property. Null, if there is no parent controlled event
    */
   @OneToOne(cascade = { CascadeType.ALL })
   @JoinColumn(name = "PARENTRESULT_ID")
   private EventResult parentResult;

   /**
    * the current user of this event
    */
   @Column(name = "TRACK_USER", length = 50)
   private String user;

   /**
    * the current tenant of this event
    */
   @Column(name = "TRACK_TENANT")
   private String tenant;

   @Column(name = "RESOURC")
   private String resource;

   @Column(length = 60)
   private String caseId;

   public EventResult() {
      log.debug("call EventResult constructor");
   }

   public EventResult(String sensor, EventMetadata metadata) {
      this.sensor = sensor;
      if (metadata != null) {
         resource = metadata.getResource().getResourceHandler().toString();
         this.event = metadata.getControlEvent();
         this.actuators = metadata.getActuatorNames();
         this.setpoints = metadata.getSetpointIds();
         if (actuators != null && actuators.length() > 0) {
            this.caseId = metadata.getCaseId();
         }
      }
      user = Context.internalSessionScope().getUser();
      if (!AbstractAuthenticationProvider.DEFAULT_TENANT.equals(Context.internalSessionScope().getTenant())) {
         tenant = Context.internalSessionScope().getTenant();
      }
   }

   public EventResult(EventResult r) {
      actuators = r.actuators;
      childResults = r.childResults;
      event = r.event;
      executionStatus = r.executionStatus;
      executionTime = r.executionTime;
      parentResult = r.parentResult;
      resource = r.resource;
      sensor = r.sensor;
      setpoints = r.setpoints;
      tenant = r.tenant;
      user = Context.internalSessionScope().getUser();
   }

   @PrePersist
   protected void prePersist() {
      eventResultId = UUID.randomUUID().toString();
   }

   private String toStringBasic() {
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
      StringBuffer b = new StringBuffer();
      b.append("executionTime: ");
      b.append(formatter.format(executionTime));
      b.append("; caseId: ");
      b.append(caseId);
      b.append("; sensor: ");
      b.append(sensor);
      b.append("; event: ");
      b.append(event);
      b.append("; user: ");
      b.append(user);
      if (tenant != null) {
         b.append("; tenant: ");
         b.append(tenant);
      }

      b.append("; resource: [");
      b.append(resource);
      b.append("]; executionStatus: ");
      b.append(executionStatus);
      b.append("; setpoints: ");
      b.append(setpoints);
      b.append("; actuators: ");
      b.append(actuators);
      return b.toString();
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append(toStringBasic());
      b.append("\nparent: ");
      b.append(parentResult == null ? "null" : parentResult.toStringBasic());
      b.append("\nchildren:\n");
      b.append(childToString("   "));
      return b.toString();
   }

   private String childToString(String tab) {
      StringBuffer b = new StringBuffer();

      for (EventResult child : getChildResults()) {
         b.append(tab);
         b.append(child.toStringBasic());
         b.append("\n");
         b.append(child.childToString(tab + "   "));
      }

      return b.toString();
   }

   public EventResult getLastExecutingEventResult() {
      EventResult thisOne = null;
      if (isExecuting())
         thisOne = this;
      for (EventResult child : getChildResults()) {
         EventResult nextChild = child.getLastExecutingEventResult();
         if (nextChild != null)
            thisOne = nextChild;
      }
      return thisOne;
   }

   public EventResult getFirstExecutedEventResult() {
      if (!isExecuting()) {
         return this;
      } else {
         EventResult thisOne = null;
         for (EventResult child : getChildResults()) {
            EventResult nextChild = child.getFirstExecutedEventResult();
            if (nextChild != null)
               thisOne = nextChild;
         }
         return thisOne;
      }
   }

   /**
    * @return the sensor
    */
   public String getSensor() {
      return sensor;
   }

   /**
    * @param sensor
    *           the sensor to set
    */
   public void setSensor(String sensor) {
      this.sensor = sensor;
   }

   /**
    * @return the actuators
    */
   public String getActuators() {
      return actuators;
   }

   /**
    * @param actuators
    *           the actuators to set
    */
   public void setActuators(String actuators) {
      this.actuators = actuators;
   }

   /**
    * @return the executionTime
    */
   public Date getExecutionTime() {
      return executionTime;
   }

   /**
    * @param executionTime
    *           the executionTime to set
    */
   public void setExecutionTime(Date executionTime) {
      this.executionTime = executionTime;
   }

   /**
    * @return the event
    */
   public ControlEvent getEvent() {
      return event;
   }

   /**
    * @param event
    *           the event to set
    */
   public void setEvent(ControlEvent event) {
      this.event = event;
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
    * @return the parentResult
    */
   public EventResult getParentResult() {
      return parentResult;
   }

   /**
    * @param parentResult
    *           the parentResult to set
    */
   public void setParentResult(EventResult parentResult) {
      this.parentResult = parentResult;
   }

   /**
    * @return the user
    */
   public String getUser() {
      return user;
   }

   /**
    * @param user
    *           the user to set
    */
   public void setUser(String user) {
      this.user = user;
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

   public boolean isExecuting() {
      return executionStatus == ExecutionStatus.EXECUTING;
   }

   /**
    * @return the childResults
    */
   public List<EventResult> getChildResults() {
      return childResults;
   }

   /**
    * @param childResults
    *           the childResults to set
    */
   public void setChildResults(List<EventResult> childResults) {
      this.childResults = childResults;
   }

   /**
    * @return the setpoints
    */
   public String getSetpoints() {
      return setpoints;
   }

   /**
    * @param setpoints
    *           the setpoints to set
    */
   public void setSetpoints(String setpoints) {
      this.setpoints = setpoints;
   }

   /**
    * @return the eventResultId
    */
   public String getEventResultId() {
      return eventResultId;
   }

   /**
    * @param eventResultId
    *           the eventResultId to set
    */
   public void setEventResultId(String eventResultId) {
      this.eventResultId = eventResultId;
   }

   /**
    * @return the resource
    */
   public String getResource() {
      return resource;
   }

   /**
    * @param resource
    *           the resource to set
    */
   public void setResource(String resource) {
      this.resource = resource;
   }

   /**
    * @return the caseId
    */
   public String getCaseId() {
      return caseId;
   }

   /**
    * @param caseId
    *           the caseId to set
    */
   public void setCaseId(String caseId) {
      this.caseId = caseId;
   }

}
