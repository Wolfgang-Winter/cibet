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

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.persistence.EntityManager;
import javax.persistence.TransactionRequiredException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.authentication.ChainedAuthenticationProvider;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.core.CEntityManager;
import com.logitags.cibet.core.CibetException;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.sensor.http.Headers;

public class RequestScopeContext implements InternalRequestScope {

   private static Log log = LogFactory.getLog(RequestScopeContext.class);

   private static final String CIBET_ENTITYMANAGER = "__CIBET_ENTITYMANAGER";
   private static final String SECOND_APPLICATION_ENTITYMANAGER = "__SEC_APPLICATION_ENTITYMANAGER";
   private static final String APPLICATION_ENTITYMANAGER = "__APPLICATION_ENTITYMANAGER";
   private static final String SCHEDULED_FIELD = "__SCHEDULED_FIELD";

   public static final String ROLLBACKONLY = "CIBET_ROLLBACKONLY";

   private ThreadLocalMap tlm = new ThreadLocalMap();

   /**
    * returns the first EventResult object not in status EXECUTING within the EventResult tree. Returns null if no event
    * or the event is still executing.
    * 
    * @return
    */
   @Override
   public EventResult getExecutedEventResult() {
      EventResult parent = (EventResult) getProperty(EVENTRESULT);
      return parent == null ? null : parent.getFirstExecutedEventResult();
   }

   @Override
   public void setRollbackOnly(boolean b) {
      setProperty(ROLLBACKONLY, b);
   }

   @Override
   public boolean getRollbackOnly() {
      Boolean rbo = (Boolean) getProperty(ROLLBACKONLY);
      if (rbo != null) {
         return rbo;
      }

      HttpServletRequest req = Context.internalSessionScope().getHttpRequest();
      if (req != null) {
         HttpSession session = req.getSession(false);
         if (session != null) {
            String rb = (String) session.getAttribute(ROLLBACKONLY);
            if (rb != null) {
               return Boolean.valueOf(rb);
            }
         }
      }
      return false;
   }

   /**
    * set the applications EntityManager instance for the Cibet entities.
    * 
    * @param manager
    */
   @Override
   public void setEntityManager(EntityManager manager) {
      setProperty(CIBET_ENTITYMANAGER, manager);

      if (manager != null && getProperty(ENTITYMANAGER_TYPE) == null) {
         try {
            manager.getTransaction();
            setProperty(ENTITYMANAGER_TYPE, EntityManagerType.RESOURCE_LOCAL);

         } catch (IllegalStateException e) {
            setProperty(ENTITYMANAGER_TYPE, EntityManagerType.JTA);
         }
      }

      if (log.isDebugEnabled()) {
         if (manager instanceof CEntityManager) {
            log.debug("set " + getProperty(ENTITYMANAGER_TYPE) + " Cibet EntityManager in CibetContext: " + manager
                  + " [" + ((CEntityManager) manager).getNativeEntityManager() + "]");
         } else {
            log.debug("set " + getProperty(ENTITYMANAGER_TYPE) + " Cibet EntityManager in CibetContext: " + manager);
         }
      }

   }

   /**
    * Return the EntityManager instance for persistence of the Cibet entities.
    * 
    * @return
    * @throws CibetException
    *            if no EntityManager set in CibetContext
    */
   @Override
   public EntityManager getOrCreateEntityManager(boolean transacted) {
      EntityManager manager = (EntityManager) getProperty(CIBET_ENTITYMANAGER);
      if (manager == null) {
         manager = Context.getOrCreateEntityManagers();
         if (manager == null) {
            throw new CibetException("No Cibet EntityManager set in CibetContext");
         }
      }
      if (log.isDebugEnabled()) {
         log.debug("get Cibet EntityManager from CibetContext: " + manager);
      }

      EntityManagerType emType = (EntityManagerType) getProperty(ENTITYMANAGER_TYPE);
      if (EntityManagerType.JTA == emType) {
         try {
            manager.joinTransaction();
            log.debug("... and join JTA transaction");
         } catch (TransactionRequiredException e) {
            log.info("... but cannot join transaction: " + e.getMessage());
            if (transacted) {
               throw e;
            }
         }
      }

      // if (transacted && (manager instanceof CEntityManager && ((CEntityManager) manager).supportsTransactions()
      // || !(manager instanceof CEntityManager)) && !manager.isJoinedToTransaction()) {
      // // if (transacted && manager instanceof CEntityManager && ((CEntityManager) manager).supportsTransactions()
      // // && !manager.isJoinedToTransaction()) {
      // throw new TransactionRequiredException();
      // }
      return manager;
   }

   /**
    * Return the Cibet EntityManager instance. Could be null, if not set in context.
    * 
    * @return
    */
   @Override
   public EntityManager getEntityManager() {
      EntityManager manager = (EntityManager) getProperty(CIBET_ENTITYMANAGER);
      if (log.isDebugEnabled()) {
         log.debug("get Cibet EntityManager from CibetContext: " + manager);
      }
      return manager;
   }

   @Override
   public void setProperty(String key, Object value) {
      getProperties().put(key, value);
   }

   @Override
   public void removeProperty(String key) {
      getProperties().remove(key);
   }

   @Override
   public Object getProperty(String key) {
      HashMap<String, Object> ht = tlm.get();
      if (ht != null && key != null) {
         return ht.get(key);
      } else {
         return null;
      }
   }

   @Override
   public HashMap<String, Object> getProperties() {
      HashMap<String, Object> ht = tlm.get();
      if (ht == null) {
         ht = new HashMap<String, Object>();
         tlm.set(ht);
      }
      return ht;
   }

   @Override
   public void clear() {
      tlm.remove();
   }

   /**
    * registers the given new EventResult. It is either added as new root of the EventResult tree or added to the tail
    * of the childrens list of the last EventResult in status EXECUTING.
    * 
    * @param thisResult
    * @return
    */
   @Override
   public EventResult registerEventResult(EventResult thisResult) {
      EventResult parent = (EventResult) getProperty(EVENTRESULT);
      EventResult lastResult = parent == null ? null : parent.getLastExecutingEventResult();
      if (lastResult == null) {
         setProperty(EVENTRESULT, thisResult);
      } else {
         thisResult.setParentResult(lastResult);
         lastResult.getChildResults().add(thisResult);
      }
      return thisResult;
   }

   @Override
   public void setApplicationEntityManager2(EntityManager manager) {
      log.debug("set second application EntityManager in CibetContext: " + manager);
      if (manager instanceof CEntityManager) {
         manager = ((CEntityManager) manager).getNativeEntityManager();
         log.debug("set second application EntityManager from CibetEntityManager into CibetContext");
      }

      setProperty(SECOND_APPLICATION_ENTITYMANAGER, manager);
   }

   /**
    * Return the applications INTERNAL EntityManager instance or null if not set.
    * 
    * @return
    */
   @Override
   public EntityManager getApplicationEntityManager2() {
      EntityManager manager = (EntityManager) getProperty(SECOND_APPLICATION_ENTITYMANAGER);
      // log.debug("get second application EntityManager from CibetContext: " +
      // manager);
      return manager;
   }

   /**
    * sets the applications EntityManager for JPA sensors for the application entities.
    * 
    * @param manager
    */
   @Override
   public void setApplicationEntityManager(EntityManager manager) {
      if (log.isDebugEnabled()) {
         if (manager instanceof CEntityManager) {
            log.debug("set application EntityManager in CibetContext: " + manager + " ["
                  + ((CEntityManager) manager).getNativeEntityManager() + "]");
         } else {
            log.debug("set application EntityManager in CibetContext: " + manager);
         }
      }
      setProperty(APPLICATION_ENTITYMANAGER, manager);
   }

   /**
    * returns the applications EntityManager that is used to persist the applications entities (not Cibet entities)
    * 
    * @return
    * @throws CibetException
    *            if no EntityManager set in context
    */
   @Override
   public EntityManager getApplicationEntityManager() {
      EntityManager manager = (EntityManager) getProperty(APPLICATION_ENTITYMANAGER);
      if (manager == null) {
         throw new CibetException("No application EntityManager set in CibetContext");
      }
      if (log.isDebugEnabled()) {
         log.debug("get application EntityManager from CibetContext: " + manager);
      }
      return manager;
   }

   /**
    * returns the applications EntityManager that is used to persist the applications entities (not Cibet entities).
    * Returns null if no EntityManager set in context
    * 
    * @return
    */
   @Override
   public EntityManager getNullableApplicationEntityManager() {
      EntityManager manager = (EntityManager) getProperty(APPLICATION_ENTITYMANAGER);
      if (log.isDebugEnabled()) {
         log.debug("get application EntityManager from CibetContext: " + manager);
      }
      return manager;
   }

   @Override
   public void setRemark(String remark) {
      setProperty(REMARK, remark);
   }

   @Override
   public String getRemark() {
      String remark = (String) getProperty(REMARK);
      if (remark != null) {
         return remark;
      }

      HttpServletRequest req = Context.internalSessionScope().getHttpRequest();
      if (req != null) {
         HttpSession session = req.getSession(false);
         if (session != null) {
            remark = (String) session.getAttribute(Headers.CIBET_REMARK.name());
            if (remark != null) {
               return remark;
            }
         }
      }
      return null;
   }

   @Override
   public void startPlay() {
      setProperty(PLAYING_MODE, true);
   }

   @Override
   public EventResult stopPlay() {
      removeProperty(PLAYING_MODE);
      return getExecutedEventResult();
   }

   @Override
   public boolean isPlaying() {
      Boolean isPlaying = (Boolean) getProperty(PLAYING_MODE);
      if (isPlaying == null) return false;
      return isPlaying;
   }

   @Override
   public String getCaseId() {
      return (String) getProperty(CASEID);
   }

   @Override
   public void setCaseId(String caseId) {
      setProperty(CASEID, caseId);
   }

   @Override
   public boolean isPostponed() throws CibetException {
      Boolean isPostponed = (Boolean) getProperty(IS_POSTPONED);
      if (isPostponed == null) {
         isPostponed = false;
      }
      return isPostponed;
   }

   @Override
   public void setAuditedByEnvers(boolean flag) {
      setProperty(AUDITED_BY_ENVERS, flag);
   }

   @Override
   public boolean isAuditedByEnvers() {
      Boolean rbo = (Boolean) getProperty(AUDITED_BY_ENVERS);
      return rbo == null ? false : rbo;
   }

   @Override
   public void setScheduledDate(Date date) {
      if (date != null && date.before(new Date())) {
         String err = "Scheduled date must be in the future";
         log.error(err);
         throw new IllegalArgumentException(err);
      }
      setProperty(SCHEDULED_DATE, date);
      setScheduledDateInHttpSession(date);
   }

   @Override
   public void setScheduledDate(int field, int amount) {
      if (amount < 0) {
         String err = "Scheduled amount must be greater or equal 0";
         log.error(err);
         throw new IllegalArgumentException(err);
      }
      setProperty(SCHEDULED_FIELD, field);
      setProperty(SCHEDULED_DATE, amount);
   }

   @Override
   public Date getScheduledDate() {
      Object o = getProperty(SCHEDULED_DATE);
      if (o == null) {
         return getScheduledDateFromHttpSession();
      } else if (o instanceof Date) {
         return (Date) o;
      } else {
         int amount = (Integer) o;
         int field = (Integer) getProperty(SCHEDULED_FIELD);
         Calendar cal = Calendar.getInstance();
         cal.add(field, amount);
         return cal.getTime();
      }
   }

   private void setScheduledDateInHttpSession(Date date) {
      HttpServletRequest req = Context.internalSessionScope().getHttpRequest();
      if (req != null) {
         HttpSession session = req.getSession(false);
         if (session != null) {
            if (date == null) {
               session.removeAttribute(Headers.CIBET_SCHEDULEDDATE.name());
               session.removeAttribute(Headers.CIBET_SCHEDULEDFIELD.name());
            } else {
               session.setAttribute(Headers.CIBET_SCHEDULEDDATE.name(), date);
            }
         }
      }
   }

   private Date getScheduledDateFromHttpSession() {
      HttpServletRequest req = Context.internalSessionScope().getHttpRequest();
      if (req != null) {
         HttpSession session = req.getSession(false);
         if (session != null) {
            Object date = session.getAttribute(Headers.CIBET_SCHEDULEDDATE.name());
            if (date != null) {
               if (date instanceof Date) {
                  return (Date) date;

               } else if (date instanceof Integer) {
                  Integer amount = (Integer) date;
                  Integer field = (Integer) session.getAttribute(Headers.CIBET_SCHEDULEDFIELD.name());
                  if (field == null) {
                     throw new IllegalArgumentException("http session attribute " + Headers.CIBET_SCHEDULEDFIELD
                           + " must not be null when " + Headers.CIBET_SCHEDULEDDATE + " is set");
                  }

                  Calendar cal = Calendar.getInstance();
                  cal.add(field, amount);
                  return cal.getTime();

               } else {
                  throw new IllegalArgumentException("http session attribute " + Headers.CIBET_SCHEDULEDDATE
                        + " must be of type Date or Integer but is " + date.getClass());
               }
            }
         }
      }
      return null;
   }

   public ChainedAuthenticationProvider getAuthenticationProvider() {
      ChainedAuthenticationProvider provider = (ChainedAuthenticationProvider) getProperty(AUTHENTICATIONPROVIDER);
      if (provider == null) {
         provider = new ChainedAuthenticationProvider(Configuration.instance().getAuthenticationProvider());
         setProperty(AUTHENTICATIONPROVIDER, provider);
      }
      return provider;
   }

   @Override
   public void ignoreScheduledException(boolean ignore) {
      setProperty(IGNORE_SCHEDULEDEXCEPTION, ignore);
   }

   @Override
   public boolean isIgnoreScheduledException() {
      Boolean ignore = (Boolean) getProperty(IGNORE_SCHEDULEDEXCEPTION);
      if (ignore == null) {
         return false;
      } else {
         return ignore;
      }
   }

   @Override
   public boolean isManaged() {
      Boolean rbo = (Boolean) getProperty(MANAGED);
      return rbo == null ? false : rbo;
   }

   @Override
   public void setManaged(boolean b) {
      setProperty(MANAGED, b);
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("Request context");
      for (Entry<String, Object> e : getProperties().entrySet()) {
         b.append("\n");
         b.append(e.getKey());
         b.append(" = ");
         b.append(e.getValue());
      }
      return b.toString();
   }

   @Override
   public String getGroupId() {
      return (String) getProperty(GROUP_ID);
   }

   @Override
   public void setGroupId(String groupId) {
      setProperty(GROUP_ID, groupId);
   }

}
