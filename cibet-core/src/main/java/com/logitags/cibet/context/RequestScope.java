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
package com.logitags.cibet.context;

import java.util.Date;

import javax.persistence.EntityManager;

import com.logitags.cibet.core.CibetException;
import com.logitags.cibet.core.EventResult;

public interface RequestScope extends ApplicationScope {

   /**
    * returns the first EventResult object not in status EXECUTING within the EventResult tree. Returns null if no event
    * or the event is still executing.
    * 
    * @return
    */
   EventResult getExecutedEventResult();

   /**
    * enforce transaction rollback of resource-local Cibet EntityManager. Has no effect for JTA type Cibet EntityManager
    * 
    * @param b
    */
   void setRollbackOnly(boolean b);

   /**
    * Return the rollback only flag for Cibet resource-local EntityManager.
    * 
    * @return
    */
   boolean getRollbackOnly();

   /**
    * set the applications EntityManager instance for the Cibet entities.
    * 
    * @param manager
    */
   void setEntityManager(EntityManager manager);

   /**
    * Return the EntityManager instance for persistence of the Cibet entities.
    * 
    * @return
    * @throws CibetException
    *            if no EntityManager set in CibetContext
    */
   EntityManager getEntityManager();

   /**
    * set a remark of the creating, first approving or final approving user
    * 
    * @param remark
    */
   void setRemark(String remark);

   /**
    * get a remark of the creating, first approving or final approving user
    */
   String getRemark();

   /**
    * sets the execution mode to playing. That is, the following actions on resources are not executed, but only played
    * and evaluated as if executed. In this mode, all Cibet control effects that are applied can be determined in
    * advance. The playing mode is ended with method endPlay().
    */
   void startPlay();

   /**
    * ends the playing mode. The execution status and applied Setpoints and actuators if executed in real mode can be
    * obtained from the EventResult.
    * 
    * @return
    */
   EventResult stopPlay();

   /**
    * returns true if the application is in playing mode.
    * 
    * @return
    */
   boolean isPlaying();

   /**
    * return the case ID of the current event. Returns null if no case ID set in context
    * 
    * @return
    */
   String getCaseId();

   /**
    * set the case ID of the current event.
    * 
    * @param caseId
    */
   void setCaseId(String caseId);

   /**
    * returns true if the current event is postponed due to dual control. This method makes sense only within execution
    * of a method controlled by ParallelDcActuator. Outside of such a method, this method returns always false.
    * 
    * 
    * @return
    */
   boolean isPostponed() throws CibetException;

   /**
    * sets the date when a business case controlled by ScheduleActuator shall be executed. Date must be in the future.
    * If set to null the business case is executed directly.
    * 
    * @param date
    */
   void setScheduledDate(Date date);

   /**
    * sets the scheduled execution date relative to the current date. A business case controlled by ScheduleActuator
    * will be executed. Amount must be positive. If amount is set to 0 the business case is executed directly.
    * 
    * @param field
    *           the calendar field. see java.util.Calendar
    * @param amount
    *           the amount of date or time to be added to the field.
    */
   void setScheduledDate(int field, int amount);

   /**
    * returns the scheduled date. If a relative date has been set, returns the scheduled date calculated from the time
    * of calling this method.
    * 
    * @return the scheduled date. Null if no date is set or amount is 0.
    */
   Date getScheduledDate();

   /**
    * flag to signal that in SchedulerActuator a ScheduledException should be ignored. This should be set to true when a
    * business case shall be scheduled and another scheduled event exists already on that business case. If the second
    * event shall be scheduled nonetheless after inspection, this must be set to true.
    * 
    * @param ignore
    *           true if ScheduledException shall be ignored
    */
   void ignoreScheduledException(boolean ignore);

   /**
    * returns true if ScheduledException should be ignored. This can be set to true when a business case shall be
    * scheduled and another scheduled event exists already on that business case. If the second event shall be scheduled
    * nonetheless after inspection, this flag must be set to true.
    * 
    * @return
    */
   boolean isIgnoreScheduledException();

   /**
    * Resources in archives and Controllables can be grouped. For JPA resources the group id is per default
    * 'target'-'primaryKeyId'. The groupId can always be overwritten by users by setting a groupId into the request
    * scope context before one of the Archive-, Dc- or Scheduler actuators are applied.
    * 
    * @return
    */
   String getGroupId();

   /**
    * Resources in archives and Controllables can be grouped. For JPA resources the group id is per default
    * 'target'-'primaryKeyId'. The groupId can always be overwritten by users by setting a groupId into the request
    * scope context before one of the Archive-, Dc- or Scheduler actuators are applied.
    * 
    * @param groupId
    */
   void setGroupId(String groupId);

}
