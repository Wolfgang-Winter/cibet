package com.logitags.cibet.actuator.lock;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.common.AbstractActuator;
import com.logitags.cibet.actuator.common.DeniedException;
import com.logitags.cibet.actuator.dc.DcControllable;
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

   private Class<? extends DeniedException> deniedExceptionType;

   public LockActuator() {
      setName(DEFAULTNAME);
   }

   public LockActuator(String name) {
      setName(name);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.core.AbstractActuator#beforeEvent(com.logitags.cibet .core.EventMetadata)
    */
   @Override
   public void beforeEvent(EventMetadata ctx) {
      if (ctx.getExecutionStatus() == ExecutionStatus.DENIED) {
         log.info("EventProceedStatus is DENIED. Skip LockActuator.beforeEvent");
         return;
      }

      switch (ctx.getControlEvent()) {
      case INVOKE:
      case RELEASE_INVOKE:
      case FIRST_RELEASE_INVOKE:
      case REJECT_INVOKE:
      case REDO:
      case SUBMIT_INVOKE:
      case PASSBACK_INVOKE:
         checkBeforeInvoke(ctx);
         break;

      default:
         checkBeforePersist(ctx);
      }
   }

   private void checkBeforeInvoke(EventMetadata ctx) {
      List<DcControllable> list = Locker.loadLockedObjects(ctx.getResource().getTargetType());

      for (DcControllable lo : list) {

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
      List<DcControllable> list = Locker.loadLockedObjects(ctx.getResource().getTargetType());
      if (list.isEmpty()) {
         return;
      }

      if (!(ctx.getResource() instanceof JpaResource)) {
         return;
      }

      JpaResource jpar = (JpaResource) ctx.getResource();
      String objectId = jpar.getPrimaryKeyObject().toString();
      for (DcControllable lo : list) {
         if (lo.getResource() instanceof JpaResource) {
            if (((JpaResource) lo.getResource()).isLocked(ctx.getResource().getObject(), objectId)
                  && ctx.getControlEvent().isChildOf(lo.getControlEvent())) {
               if (!lo.getCreateUser().equals(Context.internalSessionScope().getUser())) {
                  if (log.isDebugEnabled()) {
                     log.debug(ctx.getControlEvent() + " of " + ctx.getResource().getTargetType() + " with ID "
                           + objectId + " is locked by: " + lo);
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
         try {
            Constructor<? extends DeniedException> constr = deniedExceptionType.getConstructor(String.class,
                  String.class);
            DeniedException ex = constr.newInstance("Access denied", Context.internalSessionScope().getUser());
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
    * @param automaticU
    *           the automaticUnlock to set
    */
   public void setAutomaticUnlock(boolean automaticU) {
      this.automaticUnlock = automaticU;
   }

}
