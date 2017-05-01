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

package com.logitags.cibet.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Transient;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.input.ClassLoaderObjectInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.context.Context;
import com.logitags.cibet.diff.Difference;
import com.logitags.cibet.diff.EntityIntrospector;
import com.logitags.cibet.diff.PrimitiveArrayDifferFactory;
import com.logitags.cibet.diff.ToListPrintingVisitor;
import com.logitags.cibet.resource.Resource;

import de.danielbechler.diff.ObjectDiffer;
import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;

/**
 * Utility methods
 */
public class CibetUtil {

   /**
    * logger for tracing
    */
   private static Log log = LogFactory.getLog(CibetUtil.class);

   public static final Pattern classNamePattern = Pattern.compile("^(\\[+)([BZCDFIJSL]{1})(.*?);?");

   private static ObjectDiffer objectDiffer;

   /**
    * decodes a byte stream to an object
    * 
    * @param enc
    * @return
    */
   public static Object decode(byte[] enc) {
      try {
         if (enc == null)
            return null;
         ByteArrayInputStream in = new ByteArrayInputStream(enc);
         ObjectInputStream instream = new ObjectInputStream(in);
         return instream.readObject();
      } catch (Exception e) {
         throw new RuntimeException(e.getMessage(), e);
      }
   }

   /**
    * decodes the serialized byte stream to the object using the specified classloader for loading the class. This
    * method may be used for migration of serialized objects between different class versions.
    * 
    * @param classLoader
    * @param enc
    * @return
    */
   public static Object decode(ClassLoader classLoader, byte[] enc) {
      ClassLoaderObjectInputStream instream = null;
      try {
         if (enc == null)
            return null;
         ByteArrayInputStream in = new ByteArrayInputStream(enc);
         instream = new ClassLoaderObjectInputStream(classLoader, in);
         return instream.readObject();
      } catch (Exception e) {
         throw new RuntimeException(e.getMessage(), e);
      } finally {
         if (instream != null)
            try {
               instream.close();
            } catch (IOException e) {
               log.error(e.getMessage(), e);
            }
      }
   }

   /**
    * encodes an Object into a byte stream.
    * 
    * @param obj
    * @return
    */
   public static byte[] encode(Object obj) throws IOException {
      if (obj == null)
         return null;
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(output);
      out.writeObject(obj);
      byte[] result = output.toByteArray();
      return result;
   }

   /**
    * checks if the entity and all its properties are completely loaded from the database and all lazy properties are
    * initialised.
    * 
    * @param em
    *           EntityManager for loading the entity
    * @param entity
    * @return
    */
   public static boolean isLoaded(EntityManager em, Object entity) {
      if (entity == null) {
         return true;
      }

      log.debug("check load state!");
      if (!em.contains(entity)) {
         log.debug("Entity is not in persistence context. Cannot check load state");
         // return true;
      }

      PersistenceUnitUtil util = em.getEntityManagerFactory().getPersistenceUnitUtil();
      if (util == null) {
         log.debug("no PersistenceUnitUtil");
         return true;
      }

      boolean loadstate = true;
      Class<?> clazz = entity.getClass();
      while (clazz != null) {
         Field[] fields = clazz.getDeclaredFields();
         for (Field f : fields) {
            if (Modifier.isStatic(f.getModifiers()))
               continue;
            if (Modifier.isTransient(f.getModifiers()))
               continue;
            if (AnnotationUtil.isAnnotationPresent(clazz, f, Transient.class))
               continue;
            boolean loaded = util.isLoaded(entity, f.getName());
            log.debug(f.getName() + " [" + f.getType() + "] loaded: " + loaded);
            if (!loaded)
               loadstate = false;
         }
         clazz = clazz.getSuperclass();
      }

      return loadstate;
   }

   /**
    * checks if the entity and all its properties are completely loaded from the database and all lazy properties are
    * initialised.
    * 
    * @param entity
    * @return
    */
   public static boolean isLoaded(Object entity) {
      return isLoaded(Context.internalRequestScope().getApplicationEntityManager(), entity);
   }

   /**
    * detaches the entity and all of its direct attributes and elements of Collection attributes that are Entities or
    * Embeddables.
    * 
    * @param entity
    * @param upReferences
    */
   public static void deepDetach(Object entity, List<Object> upReferences) {
      if (entity == null)
         return;
      Class<?> clazz = entity.getClass();
      log.debug("detach all properties of instance of " + clazz);
      EntityManager em = Context.internalRequestScope().getApplicationEntityManager();
      if (em.contains(entity)) {
         em.detach(entity);
      }
      while (clazz != null) {
         Field[] f = clazz.getDeclaredFields();
         for (Field field : f) {
            Class<?> type = field.getType();
            if (Collection.class.isAssignableFrom(type)) {
               String methodName = "get" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
               if (log.isDebugEnabled()) {
                  log.debug("found Collection type " + type.getName() + ", getter method = " + methodName);
               }
               try {
                  Method method = clazz.getDeclaredMethod(methodName, new Class<?>[] {});
                  Collection<?> collection = (Collection<?>) method.invoke(entity, new Object[] {});
                  if (collection != null) {
                     Iterator<?> it = collection.iterator();
                     while (it.hasNext()) {
                        Object colChild = it.next();
                        if (colChild != null && (colChild.getClass().isAnnotationPresent(Entity.class)
                              || colChild.getClass().isAnnotationPresent(Embeddable.class))) {
                           if (em.contains(colChild)) {
                              em.detach(colChild);
                           }
                           upReferences.add(colChild);
                           deepDetach(colChild, upReferences);
                        }
                     }
                  }
               } catch (NoSuchMethodException e) {
                  log.debug("no such method: " + e.getMessage());
               } catch (Exception e) {
                  log.warn(e.getMessage(), e);
               }
            } else if (type.isAnnotationPresent(Entity.class)
                  || type.getClass().isAnnotationPresent(Embeddable.class)) {
               String methodName = "get" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
               try {
                  Method method = clazz.getDeclaredMethod(methodName, new Class<?>[] {});
                  Object o = method.invoke(entity, new Object[] {});
                  if (o != null) {
                     if (em.contains(o)) {
                        em.detach(o);
                     }

                     boolean hasUpReference = false;
                     for (Object ref : upReferences) {
                        if (o == ref) {
                           hasUpReference = true;
                           break;
                        }
                     }
                     if (!hasUpReference) {
                        upReferences.add(o);
                        deepDetach(o, upReferences);
                     }
                  }
               } catch (NoSuchMethodException e) {
                  log.debug("no such method: " + e.getMessage());
               } catch (Exception e) {
                  log.warn(e.getMessage(), e);
               }
            }
         }
         clazz = clazz.getSuperclass();
      }
   }

   /**
    * loads eagerly associations of an entity which may be annotated with lazy fetch type. If the entity was loaded by
    * an EntityManager it could also be a proxy of the real object.
    * 
    * @param obj
    * @param clazz
    *           class of the object (the real object, not the proxy)
    */
   public static void loadLazyEntities(Object obj, Class<?> clazz) {
      log.debug("load lazy entities");
      List<Object> references = new ArrayList<Object>();
      references.add(obj);
      loadLazyEntities(obj, clazz, references);
   }

   /**
    * prevents cyclic dependencies
    * 
    * @param obj
    * @param reference
    */
   private static void loadLazyEntities(Object obj, Class<?> clazz, List<Object> upReferences) {
      if (obj == null)
         return;

      if (obj.getClass().getAnnotation(Embeddable.class) == null
            && obj.getClass().getAnnotation(Entity.class) == null) {
         return;
      }

      while (clazz != null) {
         Field[] f = clazz.getDeclaredFields();
         for (Field field : f) {
            if (Modifier.isStatic(field.getModifiers())) {
               continue;
            }
            if (Modifier.isTransient(field.getModifiers())) {
               continue;
            }
            if (field.getAnnotation(Transient.class) != null) {
               continue;
            }
            Class<?> type = field.getType();
            if (type == Date.class || type == Calendar.class) {
               continue;
            }
            if (Collection.class.isAssignableFrom(type)) {
               try {
                  String getterMethodName;
                  Method getterMethod;
                  getterMethodName = "get" + field.getName().substring(0, 1).toUpperCase()
                        + field.getName().substring(1);
                  getterMethod = clazz.getDeclaredMethod(getterMethodName, new Class<?>[] {});
                  if (getterMethod.isAnnotationPresent(Transient.class)) {
                     continue;
                  }
                  try {
                     String setterMethodName = "set" + field.getName().substring(0, 1).toUpperCase()
                           + field.getName().substring(1);
                     Method setterMethod = clazz.getDeclaredMethod(setterMethodName, type);
                     if (setterMethod.isAnnotationPresent(Transient.class)) {
                        continue;
                     }
                  } catch (NoSuchMethodException e) {
                     log.warn(e.getMessage());
                  }

                  if (log.isDebugEnabled()) {
                     log.debug("found Collection type " + type.getName() + ", getter method = " + getterMethodName);
                  }
                  Collection<?> collection = (Collection<?>) getterMethod.invoke(obj, new Object[] {});
                  if (collection != null) {
                     Iterator<?> it = collection.iterator();
                     while (it.hasNext()) {
                        Object colChild = it.next();
                        if (colChild != null) {
                           upReferences.add(colChild);
                           loadLazyEntities(colChild, colChild.getClass(), upReferences);
                        }
                     }
                  }
               } catch (NoSuchMethodException e) {
                  log.debug("no such method: " + e.getMessage());
               } catch (InvocationTargetException e) {
                  log.warn(e.getMessage(), e);
               } catch (IllegalAccessException e) {
                  log.warn(e.getMessage(), e);
               } catch (IllegalArgumentException e) {
                  log.warn(e.getMessage(), e);
               }
            } else if (!type.isArray() && !type.isEnum() && !type.isPrimitive()
                  && !String.class.isAssignableFrom(type)) {
               log.debug("call toString() of " + field.getName());
               try {
                  String getterMethodName = "get" + field.getName().substring(0, 1).toUpperCase()
                        + field.getName().substring(1);
                  Method getterMethod = clazz.getDeclaredMethod(getterMethodName, new Class<?>[] {});
                  if (getterMethod.isAnnotationPresent(Transient.class)) {
                     continue;
                  }

                  try {
                     String setterMethodName = "set" + field.getName().substring(0, 1).toUpperCase()
                           + field.getName().substring(1);
                     Method setterMethod = clazz.getDeclaredMethod(setterMethodName, type);
                     if (setterMethod.isAnnotationPresent(Transient.class)) {
                        continue;
                     }
                  } catch (NoSuchMethodException e) {
                     log.warn(e.getMessage());
                  }

                  Object o = getterMethod.invoke(obj, new Object[] {});
                  if (o != null) {
                     o.toString();

                     boolean hasUpReference = false;
                     for (Object ref : upReferences) {
                        if (o == ref) {
                           hasUpReference = true;
                           break;
                        }
                     }
                     if (!hasUpReference) {
                        upReferences.add(o);
                        loadLazyEntities(o, type, upReferences);
                     }
                  }
               } catch (NoSuchMethodException e) {
                  log.debug("no such method: " + e.getMessage());
               } catch (InvocationTargetException e) {
                  log.warn(e.getMessage(), e);
               } catch (IllegalAccessException e) {
                  log.warn(e.getMessage(), e);
               } catch (IllegalArgumentException e) {
                  log.warn(e.getMessage(), e);
               }
            }
         }
         clazz = clazz.getSuperclass();
      }
   }

   /**
    * determines the component class from an array class type.
    * 
    * @param classname
    *           an array class type
    * @return
    */
   public static Class<?> arrayClassForName(String classname) {
      try {
         if (classname == null) {
            return null;
         }

         Matcher m = classNamePattern.matcher(classname);
         if (m.matches()) {
            log.debug("matches array class");
            Class<?> componentClass = null;
            if ("B".equals(m.group(2))) {
               componentClass = byte.class;
            } else if ("Z".equals(m.group(2))) {
               componentClass = boolean.class;
            } else if ("C".equals(m.group(2))) {
               componentClass = char.class;
            } else if ("D".equals(m.group(2))) {
               componentClass = double.class;
            } else if ("F".equals(m.group(2))) {
               componentClass = float.class;
            } else if ("I".equals(m.group(2))) {
               componentClass = int.class;
            } else if ("J".equals(m.group(2))) {
               componentClass = long.class;
            } else if ("S".equals(m.group(2))) {
               componentClass = short.class;
            } else if ("L".equals(m.group(2))) {
               componentClass = Class.forName(m.group(3));
            }
            log.debug("componentClass = " + componentClass);
            return componentClass;

         } else {
            String msg = classname + " is not an array class name. Failed to create array component class ";
            log.error(msg);
            throw new RuntimeException(msg);
         }
      } catch (ClassNotFoundException e) {
         String msg = "Failed to create Class from type " + classname + ": " + e.getMessage();
         log.error(msg, e);
         throw new RuntimeException(msg, e);
      }
   }

   public static ObjectDiffer getObjectDiffer() {
      if (objectDiffer == null) {
         objectDiffer = ObjectDifferBuilder.startBuilding().introspection()
               .setDefaultIntrospector(new EntityIntrospector()).and().differs()
               .register(new PrimitiveArrayDifferFactory()).build();
      }
      return objectDiffer;
   }

   /**
    * compares properties of two objects. Uses library java-object-diff. Skips static fields and
    * javax.persistence.Transient annotated fields (or methods). Considers also fields of super classes and transitive
    * fields from dependent objects. Fields of type array or Collection are compared member by member disregarding the
    * sequence.
    * 
    * @param work
    *           the new version
    * @param base
    *           the old version
    * @return
    */
   public static List<Difference> compare(Object work, Object base) {
      if (base == null || work == null) {
         String msg = "One or both of the objects to compare are null";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }

      loadLazyEntities(work, work.getClass());
      loadLazyEntities(base, base.getClass());

      // normalize by de-proxying
      try {
         if (work instanceof Serializable) {
            byte[] bytes = CibetUtil.encode(base);
            base = CibetUtil.decode(bytes);
            bytes = CibetUtil.encode(work);
            work = CibetUtil.decode(bytes);
         }
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new RuntimeException(e.getMessage(), e);
      }

      if (base.getClass() != work.getClass()) {
         String msg = "Failed to compare objects [" + work + "] ; [" + base + "]: Not of the same class";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }

      if (log.isDebugEnabled()) {
         log.debug("work: " + work);
         log.debug("base: " + base);
      }
      DiffNode root = getObjectDiffer().compare(work, base);

      ToListPrintingVisitor listVisitor = new ToListPrintingVisitor(work, base);
      root.visit(listVisitor);
      List<Difference> list = listVisitor.getDifferences();
      if (log.isDebugEnabled()) {
         log.debug("differences: " + list.size());
         for (Difference d : list) {
            log.debug(d);
         }
      }
      return list;
   }

   /**
    * convenient method
    * 
    * @param newResource
    * @param oldResource
    * @return
    */
   public static List<Difference> compare(Resource newResource, Resource oldResource) {
      if (newResource == null || oldResource == null) {
         throw new IllegalArgumentException("parameters must not be null");
      }
      return compare(newResource.getObject(), oldResource.getObject());
   }

   /**
    * compares two objects with equals() method
    * 
    * @param filename
    *           name of properties file
    * @return loaded properties
    */
   public static Properties loadProperties(String filename) {
      if (filename == null) {
         throw new IllegalArgumentException("file name is null");
      }
      log.debug("load properties file " + filename);
      InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
      if (in != null) {
         try {
            Properties properties = new Properties();
            properties.load(in);
            if (log.isDebugEnabled()) {
               for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                  log.debug(entry.getKey() + "=" + entry.getValue());
               }
            }
            return properties;
         } catch (IOException e) {
            String msg = "Failed to load " + filename + " from classpath: " + e.getMessage();
            log.error(msg, e);
            throw new RuntimeException(msg, e);
         }
      } else {
         log.warn(filename + " not found in classpath.");
         return null;
      }
   }

   /**
    * transforms the encoded EventResult into an EventResult object. The encoded EventResult of type String is
    * transmitted as header with key CIBET_EVENTRESULT in the http response when a http request is filtered by
    * CibetFilter.
    * 
    * @param encodedEventResult
    * @return
    */
   public static EventResult decodeEventResult(String encodedEventResult) {
      EventResult result = null;
      if (encodedEventResult != null) {
         byte[] bytes = Base64.decodeBase64(encodedEventResult);
         result = (EventResult) decode(bytes);
      }
      return result;
   }

}
