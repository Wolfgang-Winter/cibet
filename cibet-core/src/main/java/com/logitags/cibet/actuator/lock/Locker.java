package com.logitags.cibet.actuator.lock;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.common.DeniedException;
import com.logitags.cibet.actuator.common.InvalidUserException;
import com.logitags.cibet.context.CibetContext;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.AnnotationUtil;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ControlEvent;

/**
 * Methods for locking and unlocking of artifacts like classes, objects, methods and URLs. A user can reserve an
 * artifact so that only he can execute events on it.
 * 
 */
@CibetContext
public abstract class Locker {

   /**
    * 
    */
   private static Log log = LogFactory.getLog(Locker.class);

   /**
    * locks the given entity for the given control event for the actual user. Only the actual user can execute the event
    * when locked. Events are UPDATE, RESTORE and DELETE
    * 
    * @param entity
    * @param event
    * @param remark
    * 
    * @return the created LockedObject or null if could not be locked
    * @throws AlreadyLockedException
    *            if object is already locked.
    */
   public static synchronized LockedObject lock(Object entity, ControlEvent event, String remark)
         throws AlreadyLockedException {
      isLocked(entity, event);
      LockedObject lo = createLockedObject(entity.getClass().getName(), event, remark);
      String objectId = AnnotationUtil.primaryKeyAsString(entity);
      lo.setObjectId(objectId);
      try {
         lo.setObject(CibetUtil.encode(entity));
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new RuntimeException(e.getMessage(), e);
      }
      Context.internalRequestScope().getEntityManager().persist(lo);
      return lo;
   }

   /**
    * locks all objects of the given class for the given control event like PERSIST, UPDATE, RESTORE, DELETE for the
    * actual user. Only the actual user can execute the event when locked. In case of REDO or INVOKE event all public
    * methods of the class are locked.
    * 
    * @param cl
    * @param event
    * @param remark
    * @return the created LockedObject or null if could not be locked
    * @throws AlreadyLockedException
    *            if object is already locked.
    */
   public static synchronized LockedObject lock(Class<?> cl, ControlEvent event, String remark)
         throws AlreadyLockedException {
      isLocked(cl.getName(), event);
      LockedObject lo = createLockedObject(cl.getName(), event, remark);
      Context.internalRequestScope().getEntityManager().persist(lo);
      return lo;
   }

   /**
    * locks the given method of the declaring class for control event like INVOKE or REDO. Only the actual user can
    * invoke the method when locked. If the declaring class implements an interface the Method object must be taken from
    * the implementing class, not from the interface.
    * 
    * @param cl
    * @param method
    * @param event
    * @param remark
    * @return
    * @throws AlreadyLockedException
    */
   public static synchronized LockedObject lock(Class<?> cl, String method, ControlEvent event, String remark)
         throws AlreadyLockedException {
      if (cl.isInterface()) {
         String msg = "The Class argument must not be an interface. " + "Use the Class of the implementing class!";
         throw new IllegalArgumentException(msg);
      }

      isLocked(cl, method, event);
      LockedObject lo = createLockedObject(cl.getName(), event, remark);
      lo.setMethod(method);
      Context.internalRequestScope().getEntityManager().persist(lo);
      return lo;
   }

   /**
    * locks the targetType for control event like INVOKE or REDO. Only the actual user can access the targetType when
    * locked. targetType could be an URL or a table name.
    * 
    * @param targetType
    *           URL or table name
    * @param event
    * @param remark
    * @return the created LockedObject or null if could not be locked
    * @throws AlreadyLockedException
    *            if object is already locked.
    */
   public static synchronized LockedObject lock(String targetType, ControlEvent event, String remark)
         throws AlreadyLockedException {
      isLocked(targetType, event);
      LockedObject lo = createLockedObject(targetType, event, remark);
      Context.internalRequestScope().getEntityManager().persist(lo);
      return lo;
   }

   /**
    * locks the targetType for control event like INVOKE or REDO. Only the actual user can access the targetType when
    * locked. targetType could be a table name.
    * 
    * @param tableName
    * @param id
    *           the unique id of the target type. Could be a primary key of a table row.
    * @param event
    * @param remark
    * @return the created LockedObject or null if could not be locked
    * @throws AlreadyLockedException
    *            if object is already locked.
    */
   public static synchronized LockedObject lock(String tableName, String id, ControlEvent event, String remark)
         throws AlreadyLockedException {
      isLocked(tableName, id, event);
      LockedObject lo = createLockedObject(tableName, event, remark);
      lo.setObjectId(id);
      Context.internalRequestScope().getEntityManager().persist(lo);
      return lo;
   }

   /**
    * unlocks the given locked object. Any user can unlock the object. The LockedObject record is removed
    * 
    * @param lo
    */
   public static synchronized void removeLock(LockedObject lo) {
      if (lo == null) {
         throw new IllegalArgumentException("LockedObject must not be null");
      }
      if (!Context.internalRequestScope().getEntityManager().contains(lo)) {
         lo = Context.internalRequestScope().getEntityManager().merge(lo);
      }
      Context.internalRequestScope().getEntityManager().remove(lo);
      log.debug("lock removed: " + lo.getLockedObjectId());
   }

   /**
    * unlocks the given locked object. Only the user who locked the class can unlock the object. The LockedObject record
    * is removed
    * 
    * @param lo
    * @throws DeniedException
    *            when the actual user is not allowed to unlock.
    */
   public static synchronized void removeLockStrict(LockedObject lo) {
      if (lo == null) {
         throw new IllegalArgumentException("LockedObject must not be null");
      }
      if (!lo.getLockedBy().equals(Context.internalSessionScope().getUser())) {
         String msg = Context.internalSessionScope().getUser() + " is not allowed to unlock LockedObject "
               + lo.getLockedObjectId() + " locked by " + lo.getLockedBy();
         log.warn(msg);
         throw new DeniedException(msg, Context.internalSessionScope().getUser());
      }
      removeLock(lo);
   }

   /**
    * unlocks the given locked object. Any user can unlock. The LockedObject record is kept in datastore and status is
    * changed to unlocked.
    * 
    * @param lo
    * @param remark
    *           optional unlock remark
    */
   public static synchronized void unlock(LockedObject lo, String remark) {
      if (lo == null) {
         throw new IllegalArgumentException("LockedObject must not be null");
      }
      lo.setLockState(LockState.UNLOCKED);
      lo.setUnlockDate(new Date());
      lo.setUnlockedBy(Context.internalSessionScope().getUser());
      lo.setUnlockRemark(remark);
      lo = Context.internalRequestScope().getEntityManager().merge(lo);
      log.debug("unlocked: " + lo.getLockedObjectId());
   }

   /**
    * unlocks the given locked object. Only the user who locked the class can unlock. The LockedObject record is kept in
    * datastore and status is changed to unlocked.
    * 
    * @param lo
    * @param remark
    *           optional unlock remark
    * @throws DeniedException
    *            when the actual user is not allowed to unlock.
    */
   public static synchronized void unlockStrict(LockedObject lo, String remark) {
      if (lo == null) {
         throw new IllegalArgumentException("LockedObject must not be null");
      }
      if (!lo.getLockedBy().equals(Context.internalSessionScope().getUser())) {
         String msg = Context.internalSessionScope().getUser() + " is not allowed to unlock LockedObject "
               + lo.getLockedObjectId() + " locked by " + lo.getLockedBy();
         log.warn(msg);
         throw new DeniedException(msg, Context.internalSessionScope().getUser());
      }
      unlock(lo, remark);
   }

   /**
    * returns all LockedObject in state LOCKED of the actual tenant.
    * 
    * @return
    */
   public static List<LockedObject> loadLockedObjects() {
      Query query = Context.internalRequestScope().getEntityManager().createNamedQuery(LockedObject.SEL_LOCKED_ALL);
      query.setParameter("tenant", Context.internalSessionScope().getTenant());
      return (List<LockedObject>) query.getResultList();
   }

   /**
    * returns all LockedObjects in state LOCKED of the actual tenant and the given targetType.
    * 
    * @param targetType
    * @return
    */
   public static List<LockedObject> loadLockedObjects(String targetType) {
      Query query = Context.internalRequestScope().getEntityManager()
            .createNamedQuery(LockedObject.SEL_LOCKED_BY_TARGETTYPE);
      query.setParameter("tenant", Context.internalSessionScope().getTenant());
      query.setParameter("targetType", targetType);
      return (List<LockedObject>) query.getResultList();
   }

   /**
    * returns all LockedObjects in state LOCKED of the actual tenant and the given user.
    * 
    * @param user
    * @return
    */
   public static List<LockedObject> loadLockedObjectsByUser(String user) {
      Query query = Context.internalRequestScope().getEntityManager().createNamedQuery(LockedObject.SEL_LOCKED_BY_USER);
      query.setParameter("tenant", Context.internalSessionScope().getTenant());
      query.setParameter("user", user);
      return (List<LockedObject>) query.getResultList();
   }

   /**
    * returns all LockedObjects in state LOCKED of the actual tenant and the given method in the given class.
    * 
    * @param cl
    * @param method
    * @return
    */
   public static List<LockedObject> loadLockedObjects(Class<?> cl, String method) {
      TypedQuery<LockedObject> query = Context.internalRequestScope().getEntityManager()
            .createNamedQuery(LockedObject.SEL_LOCKED_BY_TARGETTYPE_METHOD, LockedObject.class);
      query.setParameter("tenant", Context.internalSessionScope().getTenant());
      query.setParameter("targetType", cl.getName());
      query.setParameter("method", method);
      return (List<LockedObject>) query.getResultList();
   }

   protected static void isLocked(Object obj, ControlEvent event) throws AlreadyLockedException {
      isLocked(obj, AnnotationUtil.primaryKeyAsString(obj), event);
   }

   protected static void isLocked(Object obj, String objectId, ControlEvent event) throws AlreadyLockedException {
      if (obj == null) {
         throw new IllegalArgumentException("parameter obj in method LockerImpl.isLocked() cannot be null");
      }
      List<LockedObject> list = loadLockedObjects(obj.getClass().getName());
      if (list.isEmpty()) {
         return;
      }
      for (LockedObject lo : list) {
         // if ((lo.getObjectId() == null || lo.getObjectId().equals(objectId))
         if (LockActuator.isLocked(obj, objectId, lo)
               && (event.isChildOf(lo.getLockedEvent()) || lo.getLockedEvent().isChildOf(event))) {
            if (log.isDebugEnabled()) {
               log.debug("Object " + obj + " is already locked for event " + event + " by: " + lo);
            }
            throw new AlreadyLockedException(lo);
         }
      }
   }

   protected static void isLocked(String targetType, ControlEvent event) throws AlreadyLockedException {
      if (targetType == null) {
         throw new IllegalArgumentException("parameter targetType in method LockerImpl.isLocked() cannot be null");
      }
      List<LockedObject> list = loadLockedObjects(targetType);
      if (list.isEmpty()) {
         return;
      }
      for (LockedObject lo : list) {
         if ((event.isChildOf(lo.getLockedEvent()) || lo.getLockedEvent().isChildOf(event))) {
            if (log.isDebugEnabled()) {
               log.debug("targetType " + targetType + " is already locked for event " + event + " by: " + lo);
            }
            throw new AlreadyLockedException(lo);
         }
      }
   }

   protected static void isLocked(Class<?> cl, String method, ControlEvent event) throws AlreadyLockedException {
      if (method == null) {
         throw new IllegalArgumentException("parameter method in method LockerImpl.isLocked() cannot be null");
      }
      List<LockedObject> list = loadLockedObjects(cl, method);
      if (list.isEmpty()) {
         return;
      }
      for (LockedObject lo : list) {
         if ((event.isChildOf(lo.getLockedEvent()) || lo.getLockedEvent().isChildOf(event))) {
            if (log.isDebugEnabled()) {
               log.debug("Method " + method.toString() + " is already locked for event " + event + " by: " + lo);
            }
            throw new AlreadyLockedException(lo);
         }
      }
   }

   private static LockedObject createLockedObject(String targetType, ControlEvent event, String remark) {
      if (Context.internalSessionScope().getUser() == null) {
         throw new InvalidUserException("Failed to execute lock operation: No user in Cibet context");
      }

      LockedObject lo = new LockedObject();
      lo.setLockDate(new Date());
      lo.setLockedBy(Context.internalSessionScope().getUser());
      lo.setLockedEvent(event);
      lo.setLockRemark(remark);
      lo.setLockState(LockState.LOCKED);
      lo.setTargetType(targetType);
      lo.setTenant(Context.internalSessionScope().getTenant());
      lo.setObjectId(LockActuator.CLASSLOCK);
      return lo;
   }

}
