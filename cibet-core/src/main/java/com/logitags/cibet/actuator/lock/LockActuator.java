package com.logitags.cibet.actuator.lock;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.common.AbstractActuator;
import com.logitags.cibet.actuator.common.DeniedException;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.resource.Resource;

public class LockActuator extends AbstractActuator {

   /**
    * 
    */
   private static final long serialVersionUID = -4248707126811129063L;

   private transient Log log = LogFactory.getLog(LockActuator.class);

   public static final String DEFAULTNAME = "LOCKER";

   public final static String CLASSLOCK = "CLASSLOCK";

   private boolean throwDeniedException = false;

   private boolean automaticLockRemoval = false;

   private boolean automaticUnlock = false;

   private Class<? extends DeniedException> deniedExceptionType;

   public LockActuator() {
      setName(DEFAULTNAME);
   }

   /**
    * Checks if the target object is locked by the given LockedObject. If the locked object has no objectId set (this is
    * the case when Release of an Insert event is locked) then the target class must implement the equals() method in
    * order to unambiguously identify the locked object and the target as identical.
    * 
    * @param target
    * @param objectId
    *           primary key of target
    * @param lo
    * @return
    */
   public static boolean isLocked(Object target, String objectId, LockedObject lo) {
      if (CLASSLOCK.equals(lo.getObjectId()))
         return true;
      if (lo.getObjectId() == null || lo.getObjectId().equals("0")) {
         // this is a release of an insert: has no primary key.
         Object obj = lo.getDecodedObject();
         if (obj == null) {
            String msg = "System error: If the object to lock has no primary key the object must be "
                  + "encoded into LockedObject and the equals() method must be implemented";
            throw new RuntimeException(msg);
         }
         return obj.equals(target);

      } else {
         return lo.getObjectId().equals(objectId);
      }
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
      List<LockedObject> list = Locker.loadLockedObjects(ctx.getResource().getTargetType());

      for (LockedObject lo : list) {
         if (isLocked(lo, ctx.getResource()) && ctx.getControlEvent().isChildOf(lo.getLockedEvent())) {
            if (!lo.getLockedBy().equals(Context.internalSessionScope().getUser())) {
               if (log.isDebugEnabled()) {
                  log.debug(ctx.getControlEvent() + " of " + ctx.getResource().getTargetType() + " and method "
                        + ctx.getResource().getMethod() + " is locked by: " + lo);
               }
               ctx.setExecutionStatus(ExecutionStatus.DENIED);
               initDeniedException(ctx);
               break;
            } else {
               if (!Context.requestScope().isPlaying()) {
                  // this is the user who set the lock
                  if (automaticLockRemoval) {
                     Locker.removeLock(lo);
                  } else if (automaticUnlock) {
                     Locker.unlock(lo, "automatic unlock by LockActuator");
                  }
               }
            }
         }
      }
   }

   private void checkBeforePersist(EventMetadata ctx) {
      List<LockedObject> list = Locker.loadLockedObjects(ctx.getResource().getTargetType());
      if (list.isEmpty()) {
         return;
      }

      String objectId = ctx.getResource().getPrimaryKeyObject().toString();
      for (LockedObject lo : list) {
         if (isLocked(ctx.getResource().getObject(), objectId, lo)
               && ctx.getControlEvent().isChildOf(lo.getLockedEvent())) {
            if (!lo.getLockedBy().equals(Context.internalSessionScope().getUser())) {
               if (log.isDebugEnabled()) {
                  log.debug(ctx.getControlEvent() + " of " + ctx.getResource().getTargetType() + " with ID " + objectId
                        + " is locked by: " + lo);
               }
               ctx.setExecutionStatus(ExecutionStatus.DENIED);
               initDeniedException(ctx);
               break;
            } else {
               if (!Context.requestScope().isPlaying()) {
                  // this is the user who set the lock
                  if (automaticLockRemoval) {
                     Locker.removeLock(lo);
                  } else if (automaticUnlock) {
                     Locker.unlock(lo, "automatic unlock by LockActuator");
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

   private boolean isLocked(LockedObject lo, Resource resource) {
      if (lo.getMethod() == null) {
         // this is invoke of a url
         return true;
      } else if (resource.getMethod().equals(lo.getMethod())) {
         // method invocation
         return true;
      } else {
         return false;
      }
   }

   /**
    * @return the automaticLockRemoval
    */
   public boolean isAutomaticLockRemoval() {
      return automaticLockRemoval;
   }

   /**
    * @param automaticL
    *           the automaticLockRemoval to set
    */
   public void setAutomaticLockRemoval(boolean automaticL) {
      this.automaticLockRemoval = automaticL;
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
