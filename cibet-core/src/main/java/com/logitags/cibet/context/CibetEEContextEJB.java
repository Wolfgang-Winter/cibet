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
package com.logitags.cibet.context;

import java.security.Principal;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.common.Actuator;
import com.logitags.cibet.core.EventMetadata;

@Stateless
@Local(CibetEEContext.class)
public class CibetEEContextEJB implements CibetEEContext {

   private Log log = LogFactory.getLog(CibetEEContextEJB.class);

   // @PersistenceUnit(unitName = "Cibet")
   // private EntityManagerFactory EMF;

   @Resource
   private SessionContext ctx;

   @Override
   public void beforeEvent(EventMetadata ctx) {
      log.debug("ActuatorInvokerEJB beforeInvoke");
      for (Actuator actuator : ctx.getActuators()) {
         actuator.beforeEvent(ctx);
      }
   }

   @Override
   public void afterEvent(EventMetadata ctx) {
      log.debug("ActuatorInvokerEJB afterInvoke");
      for (Actuator actuator : ctx.getActuators()) {
         actuator.afterEvent(ctx);
      }
   }

   @Override
   public boolean setEntityManagerIntoContext() {
      // if (EMF != null) {
      // EntityManager entityManager = EMF.createEntityManager();
      // Context.requestScope().setProperty(InternalRequestScope.ENTITYMANAGER_TYPE,
      // EntityManagerType.JTA);
      // Context.internalRequestScope().setEntityManager(entityManager);
      // log.debug("EntityManager created from injected EntityManagerFactory in
      // EJB");
      // return true;
      // } else {
      return false;
      // }
   }

   public void setCallerPrincipalNameIntoContext() {
      if (ctx != null) {
         try {
            Principal p = ctx.getCallerPrincipal();
            String name = p.getName();
            log.debug("ctx.getCallerPrincipal(): " + name);
            Context.internalRequestScope().setProperty(InternalRequestScope.CALLER_PRINCIPAL_NAME, name);
         } catch (IllegalStateException e) {
            log.debug("Cannot get CallerPrincipalName: " + e.getMessage());
         }
      } else {
         log.debug("Cannot get CallerPrincipalName: SessionContext is null");
      }
   }

}
