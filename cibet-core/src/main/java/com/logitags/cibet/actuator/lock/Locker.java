package com.logitags.cibet.actuator.lock;

import java.util.Date;
import java.util.List;

import javax.persistence.TypedQuery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.common.DeniedException;
import com.logitags.cibet.actuator.common.InvalidUserException;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.context.CibetContext;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.AnnotationUtil;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.sensor.http.HttpMethod;
import com.logitags.cibet.sensor.http.HttpRequestResource;
import com.logitags.cibet.sensor.jdbc.driver.JdbcResource;
import com.logitags.cibet.sensor.jpa.JpaResource;
import com.logitags.cibet.sensor.pojo.MethodResource;

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
   public static synchronized DcControllable lock(Object entity, ControlEvent event, String remark)
         throws AlreadyLockedException {
      isLocked(entity, event);
      JpaResource resource = new JpaResource(entity);
      Context.internalRequestScope().getEntityManager().persist(resource);
      DcControllable lo = createLockedObject(resource, event, remark);
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
   public static synchronized DcControllable lock(Class<?> cl, ControlEvent event, String remark)
         throws AlreadyLockedException {
      isLocked(cl.getName(), event);
      JpaResource resource = new JpaResource(cl, JpaResource.CLASSLOCK);
      Context.internalRequestScope().getEntityManager().persist(resource);
      DcControllable lo = createLockedObject(resource, event, remark);
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
   public static synchronized DcControllable lock(Class<?> cl, String method, ControlEvent event, String remark)
         throws AlreadyLockedException {
      if (cl.isInterface()) {
         String msg = "The Class argument must not be an interface. " + "Use the Class of the implementing class!";
         throw new IllegalArgumentException(msg);
      }

      isLocked(cl, method, event);
      MethodResource resource = new MethodResource();
      resource.setTargetType(cl.getName());
      resource.setMethod(method);
      Context.internalRequestScope().getEntityManager().persist(resource);

      DcControllable lo = createLockedObject(resource, event, remark);
      Context.internalRequestScope().getEntityManager().persist(lo);
      return lo;
   }

   /**
    * locks the targetType for control event like INVOKE or REDO. Only the actual user can access the targetType when
    * locked.
    * 
    * @param url
    *           URL
    * @param method
    * @param event
    * @param remark
    * @return the created LockedObject or null if could not be locked
    * @throws AlreadyLockedException
    *            if object is already locked.
    */
   public static synchronized DcControllable lock(String url, HttpMethod method, ControlEvent event, String remark)
         throws AlreadyLockedException {
      isLocked(url, event);

      HttpRequestResource resource = new HttpRequestResource();
      resource.setTargetType(url);
      resource.setMethod(method == null ? null : method.name());
      Context.internalRequestScope().getEntityManager().persist(resource);

      DcControllable lo = createLockedObject(resource, event, remark);
      Context.internalRequestScope().getEntityManager().persist(lo);
      return lo;
   }

   /**
    * locks the targetType for control event like INVOKE or REDO. Only the actual user can access the targetType when
    * locked. targetType is a table name.
    * 
    * @param tableName
    * @param id
    *           the unique id of the target type. Could be a primary key of a table row. If null, the whole table is
    *           locked
    * @param event
    * @param remark
    * @return the created LockedObject or null if could not be locked
    * @throws AlreadyLockedException
    *            if object is already locked.
    */
   public static synchronized DcControllable lock(String tableName, String id, ControlEvent event, String remark)
         throws AlreadyLockedException {
      isLocked(tableName, id, event);
      JdbcResource resource = new JdbcResource();
      resource.setTargetType(tableName);
      resource.setPrimaryKeyId(id == null ? JpaResource.CLASSLOCK : id);
      Context.internalRequestScope().getEntityManager().persist(resource);

      DcControllable lo = createLockedObject(resource, event, remark);
      Context.internalRequestScope().getEntityManager().persist(lo);
      return lo;
   }

   /**
    * unlocks the given locked object. Any user can unlock. The LockedObject record is kept in datastore and status is
    * changed to unlocked.
    * 
    * @param lo
    * @param remark
    *           optional unlock remark
    */
   public static synchronized void unlock(DcControllable lo, String remark) {
      if (lo == null) {
         throw new IllegalArgumentException("DcControllable must not be null");
      }
      lo.setExecutionStatus(ExecutionStatus.UNLOCKED);
      lo.setApprovalDate(new Date());
      lo.setApprovalUser(Context.internalSessionScope().getUser());
      lo.setApprovalRemark(remark);
      lo = Context.internalRequestScope().getEntityManager().merge(lo);
      log.debug("unlocked: " + lo);
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
   public static synchronized void unlockStrict(DcControllable lo, String remark) {
      if (lo == null) {
         throw new IllegalArgumentException("DcControllable must not be null");
      }
      if (!lo.getCreateUser().equals(Context.internalSessionScope().getUser())) {
         String msg = Context.internalSessionScope().getUser() + " is not allowed to unlock " + lo + " locked by "
               + lo.getCreateUser();
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
   public static List<DcControllable> loadLockedObjects() {
      TypedQuery<DcControllable> query = Context.internalRequestScope().getEntityManager()
            .createNamedQuery(DcControllable.SEL_LOCKED_ALL, DcControllable.class);
      query.setParameter("tenant", Context.internalSessionScope().getTenant());
      return query.getResultList();
   }

   /**
    * returns all LockedObjects in state LOCKED of the actual tenant and the given targetType.
    * 
    * @param targetType
    * @return
    */
   public static List<DcControllable> loadLockedObjects(String targetType) {
      TypedQuery<DcControllable> query = Context.internalRequestScope().getEntityManager()
            .createNamedQuery(DcControllable.SEL_LOCKED_BY_TARGETTYPE, DcControllable.class);
      query.setParameter("tenant", Context.internalSessionScope().getTenant());
      query.setParameter("targettype", targetType);
      return query.getResultList();
   }

   /**
    * returns all LockedObjects in state LOCKED of the actual tenant and the given user.
    * 
    * @param user
    * @return
    */
   public static List<DcControllable> loadLockedObjectsByUser(String user) {
      TypedQuery<DcControllable> query = Context.internalRequestScope().getEntityManager()
            .createNamedQuery(DcControllable.SEL_LOCKED_BY_USER, DcControllable.class);
      query.setParameter("tenant", Context.internalSessionScope().getTenant());
      query.setParameter("user", user);
      return query.getResultList();
   }

   /**
    * returns all LockedObjects in state LOCKED of the actual tenant and the given method in the given class.
    * 
    * @param cl
    * @param method
    * @return
    */
   public static List<DcControllable> loadLockedObjects(Class<?> cl, String method) {
      TypedQuery<DcControllable> query = Context.internalRequestScope().getEntityManager()
            .createNamedQuery(DcControllable.SEL_LOCKED_BY_TARGETTYPE_METHOD, DcControllable.class);
      query.setParameter("tenant", Context.internalSessionScope().getTenant());
      query.setParameter("targettype", cl.getName());
      query.setParameter("method", method);
      return query.getResultList();
   }

   protected static void isLocked(Object obj, ControlEvent event) throws AlreadyLockedException {
      isLocked(obj, AnnotationUtil.primaryKeyAsString(obj), event);
   }

   protected static void isLocked(Object obj, String objectId, ControlEvent event) throws AlreadyLockedException {
      if (obj == null) {
         throw new IllegalArgumentException("parameter obj in method LockerImpl.isLocked() cannot be null");
      }

      String targetType = obj.getClass().getName();
      if (obj instanceof String) {
         targetType = (String) obj;
      }

      List<DcControllable> list = loadLockedObjects(targetType);
      if (list.isEmpty()) {
         return;
      }
      for (DcControllable dc : list) {
         if (dc.getResource() instanceof JpaResource) {
            if (((JpaResource) dc.getResource()).isLocked(obj, objectId)
                  && (event.isChildOf(dc.getControlEvent()) || dc.getControlEvent().isChildOf(event))) {
               if (log.isDebugEnabled()) {
                  log.debug("Object " + obj + " is already locked for event " + event + " by: " + dc);
               }
               throw new AlreadyLockedException(dc);
            }
         }
      }
   }

   protected static void isLocked(String targetType, ControlEvent event) throws AlreadyLockedException {
      if (targetType == null) {
         throw new IllegalArgumentException("parameter targetType in method LockerImpl.isLocked() cannot be null");
      }
      List<DcControllable> list = loadLockedObjects(targetType);
      if (list.isEmpty()) {
         return;
      }
      for (DcControllable lo : list) {
         if ((event.isChildOf(lo.getControlEvent()) || lo.getControlEvent().isChildOf(event))) {
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
      List<DcControllable> list = loadLockedObjects(cl, method);
      if (list.isEmpty()) {
         return;
      }
      for (DcControllable lo : list) {
         if ((event.isChildOf(lo.getControlEvent()) || lo.getControlEvent().isChildOf(event))) {
            if (log.isDebugEnabled()) {
               log.debug("Method " + method.toString() + " is already locked for event " + event + " by: " + lo);
            }
            throw new AlreadyLockedException(lo);
         }
      }
   }

   private static DcControllable createLockedObject(Resource resource, ControlEvent event, String remark) {
      if (Context.internalSessionScope().getUser() == null) {
         throw new InvalidUserException("Failed to execute lock operation: No user in Cibet context");
      }

      DcControllable lo = new DcControllable();
      lo.setCreateDate(new Date());
      lo.setCreateUser(Context.internalSessionScope().getUser());
      lo.setControlEvent(event);
      lo.setCreateRemark(remark);
      lo.setExecutionStatus(ExecutionStatus.LOCKED);
      lo.setResource(resource);
      lo.setTenant(Context.internalSessionScope().getTenant());
      return lo;
   }

}
