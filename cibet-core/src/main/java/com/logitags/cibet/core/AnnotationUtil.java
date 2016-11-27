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
/**
 * 
 */
package com.logitags.cibet.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.context.Context;

/**
 *
 */
public class AnnotationUtil {

   /**
    * logger for tracing
    */
   private static Log log = LogFactory.getLog(AnnotationUtil.class);

   /**
    * Return the value of an annotated field or getter method.
    * 
    * @param obj
    * @param annotationClass
    * @return
    * @throws AnnotationNotFoundException
    */
   public static Object valueFromAnnotation(Object obj, Class<? extends Annotation> annotationClass)
         throws AnnotationNotFoundException {
      try {
         return valueFromFieldAnnotation(obj, annotationClass);
      } catch (AnnotationNotFoundException e) {
         return valueFromMethodAnnotation(obj, annotationClass);
      }
   }

   /**
    * sets the value to the annotated field
    * 
    * @param obj
    * @param annotationClass
    * @param value
    * @throws AnnotationNotFoundException
    */
   public static void setValueFromAnnotation(Object obj, Class<? extends Annotation> annotationClass, Object value)
         throws AnnotationNotFoundException {
      try {
         Field field = fieldFromFieldAnnotation(obj.getClass(), annotationClass);
         field.setAccessible(true);
         field.set(obj, value);

      } catch (AnnotationNotFoundException e) {
         Method method = setterMethodFromMethodAnnotation(obj.getClass(), annotationClass);
         try {
            method.invoke(obj, value);
         } catch (Exception e1) {
            log.error(e1.getMessage(), e1);
            throw new RuntimeException(e1);
         }
      } catch (IllegalAccessException ie) {
         log.error(ie.getMessage(), ie);
         throw new RuntimeException(ie);
      }
   }

   /**
    * Return the value type of an annotated field or getter method.
    * 
    * @param clazz
    * @param annotationClass
    * @return
    * @throws AnnotationNotFoundException
    */
   public static Class<?> typeFromAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClass)
         throws AnnotationNotFoundException {
      try {
         return typeFromFieldAnnotation(clazz, annotationClass);
      } catch (AnnotationNotFoundException e) {
         return typeFromMethodAnnotation(clazz, annotationClass);
      }
   }

   /**
    * checks if an annotation is present on the field or the getter method of that field. Checks also superclass
    * methods.
    * 
    * @param clazz
    * @param field
    * @param anClass
    * @return
    */
   public static boolean isAnnotationPresent(Class<?> clazz, Field field, Class<? extends Annotation> anClass) {
      if (field.isAnnotationPresent(anClass)) {
         log.debug(anClass.getName() + " annotation found on field " + field.getName());
         return true;
      }

      // check getter method
      Method m = null;
      try {
         String mName = "get" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
         m = clazz.getMethod(mName, (Class<?>[]) null);
      } catch (SecurityException e) {
         log.fatal(e.getMessage(), e);
         throw new RuntimeException(e);
      } catch (NoSuchMethodException e) {
         try {
            String mName = "is" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
            m = clazz.getMethod(mName, (Class<?>[]) null);
         } catch (SecurityException e1) {
            log.fatal(e.getMessage(), e);
            throw new RuntimeException(e);
         } catch (NoSuchMethodException e1) {
            log.info("no getter method found for field " + field.getName());
         }
      }

      if (m != null && m.isAnnotationPresent(anClass)) {
         log.debug(anClass.getName() + " annotation found on method " + m.getName());
         return true;
      }

      // check setter method
      try {
         String mName = "set" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
         m = clazz.getMethod(mName, field.getType());
      } catch (SecurityException e) {
         log.fatal(e.getMessage(), e);
         throw new RuntimeException(e);
      } catch (NoSuchMethodException e) {
         log.info("no setter method found for field " + field.getName());
         return false;
      }
      if (m != null && m.isAnnotationPresent(anClass)) {
         log.debug(anClass.getName() + " annotation found on method " + m.getName() + " of class " + clazz.getName());
         return true;
      } else {
         // log.debug(anClass.getName() + " annotation not present in " + clazz.getName());
         return false;
      }
   }

   /**
    * checks if the given annotation is in a field or getter/setter method of this class or its superclasses.
    * 
    * @param clazz
    * @param anClass
    * @return
    */
   public static boolean isAnnotationPresent(Class<?> clazz, Class<? extends Annotation> anClass) {
      try {
         fieldFromFieldAnnotation(clazz, anClass);
         return true;
      } catch (AnnotationNotFoundException e2) {
         try {
            setterMethodFromMethodAnnotation(clazz, anClass);
            return true;
         } catch (AnnotationNotFoundException e) {
            return false;
         }
      }
   }

   /**
    * Iterates over all methods and search for the given annotation including methods of parent classes
    * 
    * @param obj
    * @param annotationClass
    * @return the return value of the annotated method.
    * @throws AnnotationNotFoundException
    */
   private static Object valueFromMethodAnnotation(Object obj, Class<? extends Annotation> annotationClass)
         throws AnnotationNotFoundException {
      Class<?> clazz = obj.getClass();
      while (clazz != null) {
         Method[] m = clazz.getDeclaredMethods();
         for (Method method : m) {
            Annotation annotation = method.getAnnotation(annotationClass);
            if (annotation != null) {
               try {
                  Object value = (Object) method.invoke(obj, (Object[]) null);
                  log.debug("retrieved value from method annotation " + annotationClass.getName() + ": " + value);
                  return value;
               } catch (Exception e) {
                  log.error(e.getMessage(), e);
               }
            }
         }
         clazz = clazz.getSuperclass();
      }

      String msg = "No annotation " + annotationClass.getName() + " found in methods of class "
            + obj.getClass().getName();
      log.warn(msg);
      throw new AnnotationNotFoundException(msg);
   }

   private static Class<?> typeFromMethodAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClass)
         throws AnnotationNotFoundException {
      Class<?> intClass = clazz;
      while (intClass != null) {
         Method[] m = intClass.getDeclaredMethods();
         for (Method method : m) {
            Annotation annotation = method.getAnnotation(annotationClass);
            if (annotation != null) {
               Class<?> type = method.getReturnType();
               if (type == null) continue;
               log.debug("retrieved type from method annotation " + annotationClass.getName() + ": " + type);
               return type;
            }
         }
         intClass = intClass.getSuperclass();
      }

      String msg = "No annotation " + annotationClass.getName() + " found in methods of class " + clazz.getName();
      log.warn(msg);
      throw new AnnotationNotFoundException(msg);
   }

   private static Method setterMethodFromMethodAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClass)
         throws AnnotationNotFoundException {
      Class<?> intClass = clazz;
      while (intClass != null) {
         Method[] m = intClass.getDeclaredMethods();
         for (Method method : m) {
            Annotation annotation = method.getAnnotation(annotationClass);
            if (annotation != null) {
               String mName = null;
               try {
                  if (method.getName().startsWith("get")) {
                     Class<?> type = method.getReturnType();
                     mName = "set" + method.getName().substring(3);
                     method = intClass.getDeclaredMethod(mName, type);
                  } else if (method.getName().startsWith("is")) {
                     Class<?> type = method.getReturnType();
                     mName = "set" + method.getName().substring(2);
                     method = intClass.getDeclaredMethod(mName, type);
                  } else if (method.getName().startsWith("set")) {
                     // this is the setter method
                  } else {
                     String msg = "Failed to determine setter method from method " + method.getName()
                           + " for annotation " + annotationClass.getName() + " in class " + clazz.getName();
                     log.warn(msg);
                     throw new AnnotationNotFoundException(msg);
                  }
                  return method;
               } catch (NoSuchMethodException e) {
                  String msg = "Failed to find setter method " + mName + " from getter method " + method.getName()
                        + " in class " + intClass.getName();
                  log.fatal(msg, e);
                  throw new RuntimeException(e);
               }
            }
         }
         intClass = intClass.getSuperclass();
      }

      String msg = "No annotation " + annotationClass.getName() + " found in methods of class " + clazz.getName();
      log.warn(msg);
      throw new AnnotationNotFoundException(msg);
   }

   /**
    * Iterates over all fields and search for the annotation including fields of parent classes
    * 
    * @param obj
    * @param annotationClass
    * @return the attribute value of the annotated field.
    * @throws AnnotationNotFoundException
    */
   private static Object valueFromFieldAnnotation(Object obj, Class<? extends Annotation> annotationClass)
         throws AnnotationNotFoundException {
      Class<?> clazz = obj.getClass();
      while (clazz != null) {
         Field[] f = clazz.getDeclaredFields();
         for (Field field : f) {
            Object annotation = field.getAnnotation(annotationClass);
            if (annotation != null) {
               field.setAccessible(true);
               try {
                  Object id = (Object) field.get(obj);
                  log.debug("retrieved value from field annotation " + annotationClass.getName() + ": " + id);
                  return id;
               } catch (Exception e) {
                  log.error(e.getMessage(), e);
               }
            }
         }
         clazz = clazz.getSuperclass();
      }

      String msg = "No annotation " + annotationClass.getName() + " found in fields of class "
            + obj.getClass().getName();
      log.warn(msg);
      throw new AnnotationNotFoundException(msg);
   }

   /**
    * Iterates over all fields and search for the annotation including fields of parent classes
    * 
    * @param clazz
    * @param annotationClass
    * @return the value type of the annotated field
    * @throws AnnotationNotFoundException
    */
   private static Class<?> typeFromFieldAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClass)
         throws AnnotationNotFoundException {
      Field field = fieldFromFieldAnnotation(clazz, annotationClass);
      Class<?> type = field.getType();
      log.debug("retrieved type from field annotation " + annotationClass.getName() + ": " + type);
      return type;
   }

   private static Field fieldFromFieldAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClass)
         throws AnnotationNotFoundException {
      Class<?> intClass = clazz;
      while (intClass != null) {
         Field[] f = intClass.getDeclaredFields();
         for (Field field : f) {
            Annotation annotation = field.getAnnotation(annotationClass);
            if (annotation != null) {
               return field;
            }
         }
         intClass = intClass.getSuperclass();
      }

      String msg = "No annotation " + annotationClass.getName() + " found in fields of class " + clazz.getName();
      log.warn(msg);
      throw new AnnotationNotFoundException(msg);
   }

   /**
    * returns the objects ID as a String value.
    * 
    * @param target
    * @return ID or null
    */
   public static String primaryKeyAsString(Object target) {
      String objectId = null;
      Object primaryId = valueFromAnnotation(target, Id.class);
      if (primaryId != null) objectId = primaryId.toString();
      return objectId;
   }

   /**
    * returns the objects ID as a Object value. Native data types like int and long are casted to Integer and Long.
    * 
    * @param entity
    * @return
    * @throws AnnotationNotFoundException
    */
   public static Object primaryKeyAsObject(Object entity) throws AnnotationNotFoundException {
      if (entity == null) return null;
      try {
         Object id = valueFromAnnotation(entity, Id.class);
         if (id == null) {
            // OpenJPA returns proprietary object type
            EntityManager em = Context.internalRequestScope().getApplicationEntityManager();
            if (em.getEntityManagerFactory() == null || em.getEntityManagerFactory().getPersistenceUnitUtil() == null) {
               throw new CibetException("CibetContext.getApplicationEntityManager().getEntityManagerFactory() is null");
            }
            PersistenceUnitUtil puu = em.getEntityManagerFactory().getPersistenceUnitUtil();
            id = puu.getIdentifier(entity);
            log.debug("found primary key from PersistenceUnitUtil: " + id);
         } else {
            log.debug("found primary key from annotation: " + id);
         }
         return id;
      } catch (IllegalArgumentException e) {
         throw new AnnotationNotFoundException(e.getMessage());
      }
   }

   public static <T> String primaryKeyName(Class<T> clazz) {
      Metamodel metaModel = Context.internalRequestScope().getApplicationEntityManager().getMetamodel();
      EntityType<T> entityType = metaModel.entity(clazz);
      if (!entityType.hasSingleIdAttribute()) {
         String msg = "Composite persistence ID classes are not supported";
         throw new RuntimeException(msg);
      }
      Class<?> idClass = entityType.getIdType().getJavaType();
      log.debug("idClass: " + idClass.getName());
      String idName = entityType.getId(idClass).getName();
      log.debug("found id for class " + clazz.getName() + ": " + idName);
      return idName;
   }

   private static Object cast(Object value, Class<?> clazz) {
      Object res = null;
      if (clazz.equals(String.class)) {
         res = value.toString();
      } else if (clazz.equals(Long.class) || clazz.equals(long.class)) {
         String s = value.toString();
         res = Long.valueOf(s);
      } else if (clazz.equals(Integer.class) || clazz.equals(int.class)) {
         String s = value.toString();
         res = Integer.valueOf(s);
      } else if (clazz.equals(Short.class) || clazz.equals(short.class)) {
         String s = value.toString();
         res = Short.valueOf(s);
      }
      return res;
   }

}
