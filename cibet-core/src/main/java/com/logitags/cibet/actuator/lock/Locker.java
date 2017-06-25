package com.logitags.cibet.actuator.lock;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.persistence.TypedQuery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.actuator.common.DeniedException;
import com.logitags.cibet.actuator.common.InvalidUserException;
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
   public static synchronized Controllable lock(Object entity, ControlEvent event, String remark)
         throws AlreadyLockedException {
      isLocked(entity, event);
      JpaResource resource = new JpaResource(entity);
      Context.internalRequestScope().getEntityManager().persist(resource);
      Controllable lo = createLockedObject(resource, event, remark);
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
   public static synchronized Controllable lock(Class<?> cl, ControlEvent event, String remark)
         throws AlreadyLockedException {
      isLocked(cl.getName(), event);
      JpaResource resource = new JpaResource(cl, JpaResource.CLASSLOCK);
      Context.internalRequestScope().getEntityManager().persist(resource);
      Controllable lo = createLockedObject(resource, event, remark);
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
   public static synchronized Controllable lock(Class<?> cl, String method, ControlEvent event, String remark)
         throws AlreadyLockedException {
      if (cl.isInterface()) {
         String msg = "The Class argument must not be an interface. " + "Use the Class of the implementing class!";
         throw new IllegalArgumentException(msg);
      }

      isLocked(cl, method, event);
      MethodResource resource = new MethodResource();
      resource.setTarget(cl.getName());
      resource.setMethod(method);
      Context.internalRequestScope().getEntityManager().persist(resource);

      Controllable lo = createLockedObject(resource, event, remark);
      Context.internalRequestScope().getEntityManager().persist(lo);
      return lo;
   }

   /**
    * locks the target for control event like INVOKE or REDO. Only the actual user can access the target when locked.
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
   public static synchronized Controllable lock(String url, HttpMethod method, ControlEvent event, String remark)
         throws AlreadyLockedException {
      isLocked(url, event);

      HttpRequestResource resource = new HttpRequestResource();
      resource.setTarget(url);
      resource.setMethod(method == null ? null : method.name());
      Context.internalRequestScope().getEntityManager().persist(resource);

      Controllable lo = createLockedObject(resource, event, remark);
      Context.internalRequestScope().getEntityManager().persist(lo);
      return lo;
   }

   /**
    * locks the target for control event like INVOKE or REDO. Only the actual user can access the target when locked.
    * target is a table name.
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
   public static synchronized Controllable lock(String tableName, String id, ControlEvent event, String remark)
         throws AlreadyLockedException {
      isLocked(tableName, id, event);
      JdbcResource resource = new JdbcResource();
      resource.setTarget(tableName);
      resource.setPrimaryKeyId(id == null ? JpaResource.CLASSLOCK : id);
      Context.internalRequestScope().getEntityManager().persist(resource);

      Controllable lo = createLockedObject(resource, event, remark);
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
   public static synchronized void unlock(Controllable lo, String remark) {
      if (lo == null) {
         throw new IllegalArgumentException("Controllable must not be null");
      }
      lo.setExecutionStatus(ExecutionStatus.UNLOCKED);
      lo.setReleaseDate(new Date());
      lo.setReleaseUser(Context.internalSessionScope().getUser());
      lo.setReleaseRemark(remark);
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
   public static synchronized void unlockStrict(Controllable lo, String remark) {
      if (lo == null) {
         throw new IllegalArgumentException("Controllable must not be null");
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
   public static List<Controllable> loadLockedObjects() {
      TypedQuery<Controllable> query = Context.internalRequestScope().getEntityManager()
            .createNamedQuery(Controllable.SEL_LOCKED_ALL, Controllable.class);
      query.setParameter("tenant", Context.internalSessionScope().getTenant());
      return query.getResultList();
   }

   /**
    * returns all LockedObjects in state LOCKED of the actual tenant and the given target.
    * 
    * @param target
    * @return
    */
   public static List<Controllable> loadLockedObjects(String target) {
      TypedQuery<Controllable> query = Context.internalRequestScope().getEntityManager()
            .createNamedQuery(Controllable.SEL_LOCKED_BY_TARGETTYPE, Controllable.class);
      query.setParameter("tenant", Context.internalSessionScope().getTenant());
      query.setParameter("targettype", target);
      return query.getResultList();
   }

   /**
    * returns all LockedObjects in state LOCKED of the actual tenant and the given user.
    * 
    * @param user
    * @return
    */
   public static List<Controllable> loadLockedObjectsByUser(String user) {
      TypedQuery<Controllable> query = Context.internalRequestScope().getEntityManager()
            .createNamedQuery(Controllable.SEL_LOCKED_BY_USER, Controllable.class);
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
   public static List<Controllable> loadLockedObjects(Class<?> cl, String method) {
      TypedQuery<Controllable> query = Context.internalRequestScope().getEntityManager()
            .createNamedQuery(Controllable.SEL_LOCKED_BY_TARGETTYPE, Controllable.class);
      query.setParameter("tenant", Context.internalSessionScope().getTenant());
      query.setParameter("targettype", cl.getName());
      List<Controllable> list = query.getResultList();
      Iterator<Controllable> iter = list.iterator();
      while (iter.hasNext()) {
         Controllable co = iter.next();
         if (co.getResource() instanceof MethodResource) {
            String resMethod = ((MethodResource) co.getResource()).getMethod();
            if ((method == null && resMethod != null) || (method != null && !method.equals(resMethod))) {
               iter.remove();
            }
         } else if (co.getResource() instanceof HttpRequestResource) {
            String resMethod = ((HttpRequestResource) co.getResource()).getMethod();
            if ((method == null && resMethod != null) || (method != null && !method.equals(resMethod))) {
               iter.remove();
            }

         } else {
            iter.remove();
         }
      }
      return list;
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

      List<Controllable> list = loadLockedObjects(targetType);
      if (list.isEmpty()) {
         return;
      }
      for (Controllable dc : list) {
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

   protected static void isLocked(String target, ControlEvent event) throws AlreadyLockedException {
      if (target == null) {
         throw new IllegalArgumentException("parameter target in method LockerImpl.isLocked() cannot be null");
      }
      List<Controllable> list = loadLockedObjects(target);
      if (list.isEmpty()) {
         return;
      }
      for (Controllable lo : list) {
         if ((event.isChildOf(lo.getControlEvent()) || lo.getControlEvent().isChildOf(event))) {
            if (log.isDebugEnabled()) {
               log.debug("target " + target + " is already locked for event " + event + " by: " + lo);
            }
            throw new AlreadyLockedException(lo);
         }
      }
   }

   protected static void isLocked(Class<?> cl, String method, ControlEvent event) throws AlreadyLockedException {
      if (method == null) {
         throw new IllegalArgumentException("parameter method in method LockerImpl.isLocked() cannot be null");
      }
      List<Controllable> list = loadLockedObjects(cl, method);
      if (list.isEmpty()) {
         return;
      }
      for (Controllable lo : list) {
         if ((event.isChildOf(lo.getControlEvent()) || lo.getControlEvent().isChildOf(event))) {
            if (log.isDebugEnabled()) {
               log.debug("Method " + method.toString() + " is already locked for event " + event + " by: " + lo);
            }
            throw new AlreadyLockedException(lo);
         }
      }
   }

   private static Controllable createLockedObject(Resource resource, ControlEvent event, String remark) {
      if (Context.internalSessionScope().getUser() == null) {
         throw new InvalidUserException("Failed to execute lock operation: No user in Cibet context");
      }

      Controllable lo = new Controllable();
      lo.setCreateDate(new Date());
      lo.setCreateUser(Context.internalSessionScope().getUser());
      lo.setControlEvent(event);
      lo.setCreateRemark(remark);
      lo.setExecutionStatus(ExecutionStatus.LOCKED);
      lo.setResource(resource);
      lo.setTenant(Context.internalSessionScope().getTenant());
      lo.setActuator(LockActuator.DEFAULTNAME);
      return lo;
   }

}
