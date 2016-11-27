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
package com.logitags.cibet.actuator.shiro;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;

import com.logitags.cibet.actuator.common.AbstractActuator;
import com.logitags.cibet.actuator.common.DeniedException;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalSessionScope;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.ExecutionStatus;

/**
 * Implements authorization of events by Apache Shiro.
 */
public class ShiroActuator extends AbstractActuator {

   /**
    * 
    */
   private static final long serialVersionUID = -8446298508040477937L;

   private static Log log = LogFactory.getLog(ShiroActuator.class);

   public static final String DEFAULTNAME = "SHIRO";

   /**
    * authorized if the Subject is assigned all of the specified roles.
    */
   private Collection<String> hasAllRoles = new ArrayList<String>();

   /**
    * authorized if the Subject is permitted all of the specified String permissions.
    */
   private String[] isPermittedAll;

   /**
    * The requiresAuthentication property requires the current Subject to have been authenticated during their current
    * session for the given class/instance/method to be accessed or invoked.
    */
   private Boolean requiresAuthentication;

   /**
    * The requiresGuest property requires the current Subject to be a "guest", that is, they are not authenticated or
    * remembered from a previous session for the given class/instance/method to be accessed or invoked.
    */
   private Boolean requiresGuest;

   /**
    * The requiresUser property requires the current Subject to be an application user for the given
    * class/instance/method to be accessed or invoked. An 'application user' is defined as a Subject that has a known
    * identity, either known due to being authenticated during the current session or remembered from 'RememberMe'
    * services from a previous session.
    */
   private Boolean requiresUser;

   private Class<? extends DeniedException> deniedExceptionType;

   /**
    * throw an DeniedException if event is not authorized.
    */
   private boolean throwDeniedException = false;

   /**
    * if set to true, this actuator is applied on the second user in a two-man-rule dual control event.
    */
   private boolean secondPrincipal = false;

   public ShiroActuator() {
      setName(DEFAULTNAME);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.core.AbstractActuator#beforeEvent(com.logitags.cibet .core.EventMetadata)
    */
   @Override
   public void beforeEvent(EventMetadata ctx) {
      if (hasAllRoles.isEmpty() && isPermittedAll == null && requiresAuthentication == null && requiresGuest == null
            && requiresUser == null) {
         log.warn("no access rules defined");
         return;
      }

      Subject currentUser = null;
      if (secondPrincipal) {
         currentUser = (Subject) Context.internalSessionScope().getProperty(InternalSessionScope.SECOND_PRINCIPAL);
         if (currentUser == null) {
            log.warn("No Shiro Subject object found in CibetContext.getSecondPrincipal()");
            handleDeniedException(ctx);
            return;
         }
      } else {
         currentUser = SecurityUtils.getSubject();
      }
      log.debug("authorize user " + currentUser.getPrincipal());

      if (requiresAuthentication != null && requiresAuthentication && !currentUser.isAuthenticated()) {
         handleDeniedException(ctx);
         return;
      }

      if (requiresGuest != null && requiresGuest) {
         PrincipalCollection principals = currentUser.getPrincipals();
         if (principals != null && !principals.isEmpty()) {
            handleDeniedException(ctx);
            return;
         }
      }

      if (requiresUser != null && requiresUser) {
         PrincipalCollection principals = currentUser.getPrincipals();
         if (principals == null || principals.isEmpty()) {
            handleDeniedException(ctx);
            return;
         }
      }

      if (!hasAllRoles.isEmpty() && !currentUser.hasAllRoles(hasAllRoles)) {
         handleDeniedException(ctx);
         return;
      }

      if (isPermittedAll != null && !currentUser.isPermittedAll(isPermittedAll)) {
         handleDeniedException(ctx);
         return;
      }

      log.debug("Access granted for user " + currentUser.getPrincipal());
   }

   private void handleDeniedException(EventMetadata ctx) {
      ctx.setExecutionStatus(ExecutionStatus.DENIED);
      String deniedUser = null;
      if (secondPrincipal) {
         log.warn("Access denied for user " + Context.internalSessionScope().getSecondUser());
         deniedUser = Context.internalSessionScope().getSecondUser();
      } else {
         log.warn("Access denied for user " + Context.internalSessionScope().getUser());
         deniedUser = Context.internalSessionScope().getUser();
      }

      if (throwDeniedException) {
         try {
            Constructor<? extends DeniedException> constr = deniedExceptionType.getConstructor(String.class,
                  String.class);
            DeniedException ex = constr.newInstance("Access denied", deniedUser);
            ctx.setException(ex);

         } catch (InstantiationException e) {
            throw new RuntimeException(e);
         } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
         } catch (NoSuchMethodException ie) {
            throw new RuntimeException(ie);
         } catch (InvocationTargetException ie) {
            throw new RuntimeException(ie);
         }
      }
   }

   /**
    * @return the hasAllRoles
    */
   public Collection<String> getHasAllRoles() {
      return hasAllRoles;
   }

   /**
    * @param hasAllRoles
    *           the hasAllRoles to set
    */
   public void setHasAllRoles(Collection<String> hasAllRoles) {
      this.hasAllRoles = hasAllRoles;
   }

   /**
    * @return the isPermittedAll
    */
   public String[] getIsPermittedAll() {
      return isPermittedAll;
   }

   /**
    * @param isPermittedAll
    *           the isPermittedAll to set
    */
   public void setIsPermittedAll(String[] isPermittedAll) {
      this.isPermittedAll = isPermittedAll;
   }

   /**
    * @return the requiresAuthentication
    */
   public Boolean getRequiresAuthentication() {
      return requiresAuthentication;
   }

   /**
    * @param req
    *           true if authentication is required
    */
   public void setRequiresAuthentication(Boolean req) {
      if (req == null) req = true;
      this.requiresAuthentication = req;
      log.debug("set requiresAuthentication: " + requiresAuthentication);
      if (req) {
         requiresGuest = false;
         requiresUser = false;
      }
   }

   /**
    * @return the requiresGuest
    */
   public Boolean getRequiresGuest() {
      return requiresGuest;
   }

   /**
    * @param req
    *           true if user must have guest role
    * 
    */
   public void setRequiresGuest(Boolean req) {
      if (req == null) req = true;
      this.requiresGuest = req;
      log.debug("set requiresGuest: " + requiresGuest);
      if (req) {
         requiresAuthentication = false;
         requiresUser = false;
      }
   }

   /**
    * @return the requiresUser
    */
   public Boolean getRequiresUser() {
      return requiresUser;
   }

   /**
    * @param req
    *           if true user must be in context
    * 
    */
   public void setRequiresUser(Boolean req) {
      if (req == null) req = true;
      this.requiresUser = req;
      log.debug("set requiresUser: " + requiresUser);
      if (req) {
         requiresAuthentication = false;
         requiresGuest = false;
      }
   }

   /**
    * @return the throwDeniedException
    */
   public boolean isThrowDeniedException() {
      return throwDeniedException;
   }

   /**
    * @param throwD
    *           true if DeniedException shall be thrown
    */
   public void setThrowDeniedException(boolean throwD) {
      this.throwDeniedException = throwD;
      if (this.throwDeniedException == true) {
         deniedExceptionType = resolveDeniedExceptionType();
      }
   }

   /**
    * @return the secondPrincipal
    */
   public boolean isSecondPrincipal() {
      return secondPrincipal;
   }

   /**
    * @param secP
    *           the secondPrincipal to set
    */
   public void setSecondPrincipal(boolean secP) {
      this.secondPrincipal = secP;
   }

   /**
    * @return the deniedExceptionType
    */
   public Class<? extends DeniedException> getDeniedExceptionType() {
      return deniedExceptionType;
   }

}
