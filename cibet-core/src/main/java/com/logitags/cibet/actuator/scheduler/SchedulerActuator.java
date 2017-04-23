/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2014 Dr. Wolfgang Winter
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
package com.logitags.cibet.actuator.scheduler;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.common.InvalidUserException;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.actuator.dc.FourEyesActuator;
import com.logitags.cibet.actuator.dc.ResourceApplyException;
import com.logitags.cibet.actuator.dc.UnapprovedResourceException;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalRequestScope;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.diff.Difference;
import com.logitags.cibet.jndi.EjbLookup;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.PersistenceUtil;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.jpa.JpaResourceHandler;
import com.logitags.cibet.sensor.jpa.JpaUpdateResourceHandler;

public class SchedulerActuator extends FourEyesActuator {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private transient Log log = LogFactory.getLog(ArchiveActuator.class);

   public static final String DEFAULTNAME = "SCHEDULER";
   public static final String ORIGINAL_OBJECT = "__ORIGINAL_OBJECT";

   private boolean throwScheduledException = false;

   /**
    * when true, the scheduled date is removed from the context after this actuator has been applied
    */
   private boolean autoRemoveScheduledDate = true;

   /**
    * First time at which task is to be executed. Default is the following 2 am.
    */
   private Date timerStart;

   /**
    * the time in seconds between successive task executions for executing scheduled business cases. Default is one day
    * (86400 sec)
    */
   private long timerPeriod = 86400;

   private transient SchedulerTask schedulerTask;

   /**
    * jndi name of the JDBC Datasource in the java:comp/env/jdbc namespace. This parameter must be set when resources
    * are controlled by JDBC sensor
    */
   private String datasource;

   /**
    * persistence context reference name of the EntityManager. This parameter must be set in JavaEE when resources
    * controlled by JPA or JPAQUERY sensor.
    */
   private String persistenceContextReference;

   /**
    * persistence unit name of the EntityManager. This parameter must be set in JavaSE when resources controlled by JPA
    * or JPAQUERY sensor.
    */
   private String persistenceUnit;

   /**
    * callback class executed in the batch before and after the scheduled business case.
    */
   private transient SchedulerTaskInterceptor batchInterceptor;

   public SchedulerActuator() {
      this(DEFAULTNAME);
   }

   public SchedulerActuator(String name) {
      setName(name);
      Calendar cal = Calendar.getInstance();
      cal.set(Calendar.HOUR_OF_DAY, 2);
      cal.set(Calendar.MINUTE, 0);
      cal.set(Calendar.SECOND, 0);
      if (cal.getTime().before(new Date())) {
         cal.add(Calendar.DATE, 1);
      }
      timerStart = cal.getTime();
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.core.AbstractActuator#beforeEvent(com.logitags.cibet .core.EventMetadata)
    */
   @Override
   public void beforeEvent(EventMetadata ctx) throws InvalidDateException {
      if (ctx.getExecutionStatus() == ExecutionStatus.DENIED) {
         log.info("EventProceedStatus is DENIED. Skip beforeEvent of " + this.getClass().getSimpleName());
         return;
      }

      switch (ctx.getControlEvent()) {
      case UPDATE:
         if (Context.requestScope().getScheduledDate() != null && isLoadEager()) {
            loadEager(ctx);
         }

      case DELETE:
      case RESTORE_UPDATE:
      case INVOKE:
      case REDO:
         if (Context.requestScope().getScheduledDate() != null) {
            checkScheduledResource(ctx);
         }
         // no break, fall through

      case INSERT:
      case SELECT:
      case RESTORE_INSERT:
         Date scheddate = Context.requestScope().getScheduledDate();
         if (scheddate != null && scheddate.before(new Date())) {
            String err = "Invalid scheduled date " + scheddate + ". Must be in the future";
            log.warn(err);
            throw new InvalidDateException(err);
         }

         // fall through

      case RELEASE_DELETE:
      case RELEASE_UPDATE:
      case RELEASE_INVOKE:
      case RELEASE_INSERT:
      case RELEASE_SELECT:
         // no check if scheduled date is in the future. If so will be
         // executed instantly
         if (ctx.getExecutionStatus() == ExecutionStatus.EXECUTING
               && Context.requestScope().getScheduledDate() != null) {
            ctx.setExecutionStatus(ExecutionStatus.SCHEDULED);
         }

      default:
         break;
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.core.AbstractActuator#afterEvent(com.logitags.cibet .core.EventMetadata)
    */
   @Override
   public void afterEvent(EventMetadata ctx) {
      if (ctx.getExecutionStatus() != ExecutionStatus.SCHEDULED) {
         log.info("EventProceedStatus is " + ctx.getExecutionStatus() + ". Skip afterEvent of "
               + this.getClass().getSimpleName());
         return;
      }

      if (Context.requestScope().isPlaying()) {
         return;
      }

      DcControllable dcObj = null;
      switch (ctx.getControlEvent()) {

      case UPDATE:
         dcObj = createControlledObject(ctx.getControlEvent(), ctx);
         storeCleanResource(dcObj.getResource());
         break;

      case DELETE:
      case SELECT:
         dcObj = createControlledObject(ctx.getControlEvent(), ctx);
         break;

      case INSERT:
      case RESTORE_INSERT:
         dcObj = createControlledObject(ControlEvent.INSERT, ctx);
         break;

      case RESTORE_UPDATE:
         dcObj = createControlledObject(ControlEvent.UPDATE, ctx);
         break;

      case INVOKE:
      case REDO:
         dcObj = createControlledObject(ControlEvent.INVOKE, ctx);
         break;

      case RELEASE_UPDATE:
         DcControllable dc = (DcControllable) Context.internalRequestScope()
               .getProperty(InternalRequestScope.DCCONTROLLABLE);
         if (dc == null) {
            String err = "Internal error: no DcControllable object found in internal context";
            log.error(err);
            throw new RuntimeException(err);
         }

         dc.setExecutionStatus(ExecutionStatus.SCHEDULED);
         dc.setActuator(getName());
         addStoredProperties(dc.getResource(), getStoredProperties());

         if (ctx.getResource().getResourceHandler() instanceof JpaResourceHandler) {
            dc.getResource().setResourceHandlerClass(JpaUpdateResourceHandler.class.getName());
         }
         storeCleanResource(dc.getResource());

         break;

      case RELEASE_INSERT:
      case RELEASE_SELECT:
      case RELEASE_DELETE:
      case RELEASE_INVOKE:
         dc = (DcControllable) Context.internalRequestScope().getProperty(InternalRequestScope.DCCONTROLLABLE);
         if (dc == null) {
            String err = "Internal error: no DcControllable object found in internal context";
            log.error(err);
            throw new RuntimeException(err);
         }

         dc.setExecutionStatus(ExecutionStatus.SCHEDULED);
         dc.setActuator(getName());
         addStoredProperties(dc.getResource(), getStoredProperties());
         break;

      default:
         log.debug("nothing to schedule with control event " + ctx.getControlEvent());
      }

      if (autoRemoveScheduledDate) {
         log.debug("remove scheduled date");
         Context.requestScope().setScheduledDate(null);
      }

      if (dcObj != null) {
         if (isEncrypt()) {
            dcObj.encrypt();
         }
         log.debug("persist scheduled DcControllable");
         Context.internalRequestScope().getEntityManager().persist(dcObj);

         if (isThrowPostponedException()) {
            try {
               ctx.setException(postponedExceptionType.newInstance());
            } catch (InstantiationException e) {
               throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
               throw new RuntimeException(e);
            }
         }

      }
   }

   protected void setActuatorSpecificProperties(DcControllable dc) {
      dc.setExecutionStatus(ExecutionStatus.SCHEDULED);

      if (JpaResourceHandler.class.getName().equals(dc.getResource().getResourceHandlerClass())
            && dc.getControlEvent() == ControlEvent.UPDATE) {
         dc.getResource().setResourceHandlerClass(JpaUpdateResourceHandler.class.getName());
      }
   }

   protected void checkScheduledResource(EventMetadata ctx) {
      Map<DcControllable, List<Difference>> scheduledResources = new HashMap<DcControllable, List<Difference>>();

      Resource resource = ctx.getResource();
      Query q = Context.internalRequestScope().getEntityManager().createNamedQuery(DcControllable.SEL_BY_UNIQUEID);
      q.setParameter("uniqueId", resource.getUniqueId());
      List<DcControllable> list = (List<DcControllable>) q.getResultList();
      for (DcControllable dc : list) {
         switch (dc.getExecutionStatus()) {
         case FIRST_POSTPONED:
         case FIRST_RELEASED:
         case PASSEDBACK:
         case POSTPONED:
            String msg = "An unreleased business case with ID " + dc.getDcControllableId() + " and status "
                  + dc.getExecutionStatus() + " exists already for this resource of type " + resource.getTargetType()
                  + ". This business case must be released or rejected first.";
            log.info(msg);
            throw new UnapprovedResourceException(msg, dc);
         case SCHEDULED:
            if (isThrowScheduledException() && !Context.requestScope().isIgnoreScheduledException()) {
               List<Difference> difList = null;
               if (dc.getControlEvent() == ControlEvent.UPDATE) {
                  ResourceParameter rp = dc.getResource().getParameter(SchedulerActuator.CLEANOBJECT);
                  if (rp == null) {
                     String err = "Failed to find base entity of " + dc.getResource().getTargetType() + " with ID "
                           + dc.getResource().getPrimaryKeyObject() + " in DcControllable " + dc;
                     log.error(err);
                     throw new RuntimeException(err);
                  }
                  difList = CibetUtil.compare(dc.getResource().getObject(), rp.getUnencodedValue());
               }
               scheduledResources.put(dc, difList);
            }
            break;
         default:
         }
      }

      if (!scheduledResources.isEmpty()) {
         throw new ScheduledException(scheduledResources);
      }
   }

   @Override
   protected void doRelease(DcControllable co) {
      EventResult eventResult = Context.internalRequestScope().getExecutedEventResult();
      if (eventResult == null) {
         eventResult = new EventResult();
         eventResult.setExecutionStatus(ExecutionStatus.EXECUTED);
         Context.internalRequestScope().registerEventResult(eventResult);
      }
      ExecutionStatus status = eventResult.getExecutionStatus();
      if (status == ExecutionStatus.EXECUTED || status == ExecutionStatus.SCHEDULED) {
         co.setExecutionStatus(ExecutionStatus.EXECUTED);
         co.setApprovalDate(new Date());
         co.setApprovalUser(Context.internalSessionScope().getUser());
         co.setApprovalRemark(Context.internalRequestScope().getRemark());
         co.setExecutionDate(co.getApprovalDate());

         if (isEncrypt()) {
            co.encrypt();
         }
         Context.internalRequestScope().getEntityManager().merge(co);
      }
   }

   private void storeCleanResource(Resource resource) {
      Object cleanResource = PersistenceUtil.getCleanResource(resource);
      if (cleanResource != null) {
         log.debug("store clean object");
         ResourceParameter propertyResParam = new ResourceParameter(CLEANOBJECT, cleanResource.getClass().getName(),
               cleanResource, ParameterType.INTERNAL_PARAMETER, resource.getParameters().size() + 1);
         resource.getParameters().add(propertyResParam);
      }
   }

   /**
    * @return the autoRemoveScheduledDate
    */
   public boolean isAutoRemoveScheduledDate() {
      return autoRemoveScheduledDate;
   }

   /**
    * @param autoRemoveScheduledDate
    *           the autoRemoveScheduledDate to set
    */
   public void setAutoRemoveScheduledDate(boolean autoRemoveScheduledDate) {
      this.autoRemoveScheduledDate = autoRemoveScheduledDate;
   }

   /**
    * @return the timerStart
    */
   public Date getTimerStart() {
      return timerStart;
   }

   /**
    * @param timerStart
    *           the timerStart to set
    */
   public void setTimerStart(Date timerStart) {
      this.timerStart = timerStart;
      init(null);
   }

   /**
    * @return the timerPeriod
    */
   public long getTimerPeriod() {
      return timerPeriod;
   }

   /**
    * @param timerPeriod
    *           the timerPeriod to set
    */
   public void setTimerPeriod(long timerPeriod) {
      this.timerPeriod = timerPeriod;
      init(null);
   }

   /**
    * initializes the timer for executing scheduled business cases
    * 
    * @see com.logitags.cibet.actuator.common.AbstractActuator#init(com.logitags.cibet.config.Configuration)
    */
   @SuppressWarnings("unchecked")
   @Override
   public void init(Configuration configuration) {
      if (timerPeriod == 0 || timerStart == null) {
         log.info("timerPeriod is 0 or timerStart not set. Timer not started");
         return;
      }

      int isSet = 0;
      isSet = isSet + datasource != null ? 1 : 0;
      isSet = isSet + persistenceContextReference != null ? 1 : 0;
      isSet = isSet + persistenceUnit != null ? 1 : 0;
      if (isSet > 1) {
         String err = "Only one of parameters datasource, persistenceContextReference, persistenceUnit may be set. Actually set: ["
               + datasource + "/ " + persistenceContextReference + "/ " + persistenceUnit + "]";
         log.error(err);
         throw new IllegalArgumentException(err);
      }

      close();

      SchedulerTimerConfig config = null;
      if (datasource != null) {
         config = new SchedulerTimerConfig(getName(), datasource);
         schedulerTask = new JdbcSchedulerTask();

      } else if (persistenceContextReference != null) {
         config = new SchedulerTimerConfig(getName(), persistenceContextReference);
         try {
            Class<SchedulerTask> clazz = (Class<SchedulerTask>) Class
                  .forName("com.logitags.cibet.actuator.scheduler.EESchedulerTask");
            schedulerTask = EjbLookup.lookupEjb(null, clazz);
            if (schedulerTask == null) {
               String err = "failed to lookup EESchedulerTask EJB in JNDI. Cannot start Timer for SchedulerActuator";
               log.error(err);
               return;
            }
         } catch (ClassNotFoundException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
         }

      } else {
         config = new SchedulerTimerConfig(getName(), persistenceUnit);
         schedulerTask = new SESchedulerTask();

      }

      schedulerTask.startTimer(config, timerStart, timerPeriod);
   }

   protected void checkRejectExecutionStatus(DcControllable co) throws ResourceApplyException {
      if (co.getExecutionStatus() != ExecutionStatus.SCHEDULED) {
         String err = "Failed to reject DcControllable with ID " + co.getDcControllableId()
               + ": should be in status SCHEDULED but is in status " + co.getExecutionStatus();
         log.warn(err);
         throw new ResourceApplyException(err);
      }
   }

   protected void checkExecutionStatus(String action, DcControllable co) throws ResourceApplyException {
      if (co.getExecutionStatus() != ExecutionStatus.SCHEDULED) {
         String err = "Failed to " + action + " DcControllable with ID " + co.getDcControllableId()
               + ": should be in status SCHEDULED but is in status " + co.getExecutionStatus();
         log.warn(err);
         throw new ResourceApplyException(err);
      }
   }

   protected void checkApprovalUserId(DcControllable co) throws InvalidUserException {
      String approvalUserId = Context.internalSessionScope().getUser();
      if (approvalUserId == null) {
         String msg = "Release without user id not possible. No user set in Context!";
         log.error(msg);
         throw new InvalidUserException(msg);
      }

      if (co.getApprovalUser() != null && !co.getApprovalUser().equals(approvalUserId)) {
         String msg = "release failed: Only user " + co.getApprovalUser()
               + " is allowed to release DcControllable with ID " + co.getDcControllableId();
         log.error(msg);
         throw new InvalidUserException(msg);
      }
   }

   /**
    * cancels the timer for executing scheduled business cases
    * 
    * @see com.logitags.cibet.actuator.common.AbstractActuator#close()
    */
   @Override
   public void close() {
      if (schedulerTask == null) {
         return;
      }
      try {
         schedulerTask.stopTimer(getName());
      } catch (Exception e) {
         log.warn(e.getMessage());
      }
      schedulerTask = null;
   }

   /**
    * @return the datasource
    */
   public String getDatasource() {
      return datasource;
   }

   /**
    * @param datasource
    *           the datasource to set
    */
   public void setDatasource(String datasource) {
      this.datasource = datasource;
      init(null);
   }

   /**
    * @return the persistenceContextReference
    */
   public String getPersistenceContextReference() {
      return persistenceContextReference;
   }

   /**
    * @param persistenceContextReference
    *           the persistenceContextReference to set
    */
   public void setPersistenceContextReference(String persistenceContextReference) {
      this.persistenceContextReference = persistenceContextReference;
      init(null);
   }

   /**
    * @return the persistenceUnit
    */
   public String getPersistenceUnit() {
      return persistenceUnit;
   }

   /**
    * @param persistenceUnit
    *           the persistenceUnit to set
    */
   public void setPersistenceUnit(String persistenceUnit) {
      this.persistenceUnit = persistenceUnit;
      init(null);
   }

   /**
    * @return the throwScheduledException
    */
   public boolean isThrowScheduledException() {
      return throwScheduledException;
   }

   /**
    * @param ex
    *           true if ScheduledException shall be thrown
    */
   public void setThrowScheduledException(boolean ex) {
      this.throwScheduledException = ex;
   }

   /**
    * @return the interceptor
    */
   public SchedulerTaskInterceptor getBatchInterceptor() {
      return batchInterceptor;
   }

   /**
    * @param interceptor
    *           the interceptor to set
    */
   public void setBatchInterceptor(SchedulerTaskInterceptor interceptor) {
      this.batchInterceptor = interceptor;
   }

}
