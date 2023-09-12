package com.logitags.cibet.actuator.lock;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.common.AbstractActuator;
import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.actuator.common.DeniedException;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.sensor.http.HttpRequestResource;
import com.logitags.cibet.sensor.jpa.JpaResource;
import com.logitags.cibet.sensor.pojo.MethodResource;

public class LockActuator extends AbstractActuator {

   /**
    * 
    */
   private static final long serialVersionUID = -4248707126811129063L;

   private transient Log log = LogFactory.getLog(LockActuator.class);

   public static final String DEFAULTNAME = "LOCKER";

   public final static String CLASSLOCK = "CLASSLOCK";

   private boolean throwDeniedException = false;

   private boolean automaticUnlock = false;

   public LockActuator() {
      setName(DEFAULTNAME);
   }

   public LockActuator(String name) {
      setName(name);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.core.AbstractActuator#beforeEvent(com.logitags.cibet
    * .core.EventMetadata)
    */
   @Override
   public void beforeEvent(EventMetadata ctx) {
      if (ctx.getExecutionStatus() == ExecutionStatus.DENIED) {
         log.info("EventProceedStatus is DENIED. Skip LockActuator.beforeEvent");
         return;
      }

      if (ctx.getResource() instanceof JpaResource) {
         checkBeforePersist(ctx);
      } else {
         checkBeforeInvoke(ctx);
      }
   }

   private void checkBeforeInvoke(EventMetadata ctx) {
      List<Controllable> list = Locker.loadLockedObjects(ctx.getResource().getTarget());

      for (Controllable lo : list) {

         if (isLocked(lo.getResource(), ctx.getResource()) && ctx.getControlEvent().isChildOf(lo.getControlEvent())) {
            if (!lo.getCreateUser().equals(Context.internalSessionScope().getUser())) {
               if (log.isDebugEnabled()) {
                  log.debug(ctx.getControlEvent() + " of resource " + ctx.getResource() + " is locked by: " + lo);
               }
               ctx.setExecutionStatus(ExecutionStatus.DENIED);
               initDeniedException(ctx);
               break;
            } else {
               if (!Context.requestScope().isPlaying()) {
                  // this is the user who set the lock
                  if (automaticUnlock) {
                     Locker.unlock(lo, "automatic unlock by LockActuator");
                  }
               }
            }

         }
      }
   }

   private void checkBeforePersist(EventMetadata ctx) {
      if (!(ctx.getResource() instanceof JpaResource)) {
         return;
      }

      List<Controllable> list = Locker.loadLockedObjects(ctx.getResource().getTarget());
      if (list.isEmpty()) {
         return;
      }

      JpaResource jpar = (JpaResource) ctx.getResource();
      String objectId = jpar.getPrimaryKeyObject().toString();
      for (Controllable lo : list) {
         if (lo.getResource() instanceof JpaResource) {
            if (((JpaResource) lo.getResource()).isLocked(ctx.getResource().getUnencodedTargetObject(), objectId)
                  && ctx.getControlEvent().isChildOf(lo.getControlEvent())) {
               if (!lo.getCreateUser().equals(Context.internalSessionScope().getUser())) {
                  if (log.isDebugEnabled()) {
                     log.debug(ctx.getControlEvent() + " of " + ctx.getResource().getTarget() + " with ID " + objectId
                           + " is locked by: " + lo);
                  }
                  ctx.setExecutionStatus(ExecutionStatus.DENIED);
                  initDeniedException(ctx);
                  break;
               } else {
                  if (!Context.requestScope().isPlaying()) {
                     // this is the user who set the lock
                     if (automaticUnlock) {
                        Locker.unlock(lo, "automatic unlock by LockActuator");
                     }
                  }
               }
            }
         }
      }
   }

   private void initDeniedException(EventMetadata ctx) {
      if (throwDeniedException) {
         DeniedException ex = new DeniedException("Access denied", Context.internalSessionScope().getUser());
         ctx.setException(ex);
      }
   }

   /**
    * @return the throwDeniedException
    */
   public boolean isThrowDeniedException() {
      return throwDeniedException;
   }

   /**
    * @param throwD true if DeniedException shall be thrown
    */
   public void setThrowDeniedException(boolean throwD) {
      this.throwDeniedException = throwD;
   }

   private boolean isLocked(Resource dcResource, Resource ctxResource) {
      String ctxMethod;
      if (ctxResource instanceof HttpRequestResource) {
         ctxMethod = ((HttpRequestResource) ctxResource).getMethod();
      } else if (ctxResource instanceof MethodResource) {
         ctxMethod = ((MethodResource) ctxResource).getMethod();
      } else {
         return false;
      }

      String dcMethod;
      if (dcResource instanceof HttpRequestResource) {
         dcMethod = ((HttpRequestResource) dcResource).getMethod();
      } else if (dcResource instanceof MethodResource) {
         dcMethod = ((MethodResource) dcResource).getMethod();
      } else {
         return false;
      }

      if (ctxMethod == null) {
         // this is invoke of a url
         return true;
      }

      if (ctxMethod.equals(dcMethod)) {
         // method invocation
         return true;
      }

      return false;
   }

   /**
    * @return the automaticUnlock
    */
   public boolean isAutomaticUnlock() {
      return automaticUnlock;
   }

   /**
    * @param automaticU the automaticUnlock to set
    */
   public void setAutomaticUnlock(boolean automaticU) {
      this.automaticUnlock = automaticU;
   }

}
