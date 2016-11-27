/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2014 Dr. Wolfgang Winter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************
 */
package com.logitags.cibet.diff;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.danielbechler.diff.access.PropertyAwareAccessor;
import de.danielbechler.diff.instantiation.TypeInfo;
import de.danielbechler.diff.introspection.Introspector;
import de.danielbechler.diff.introspection.PropertyAccessor;
import de.danielbechler.util.Assert;
import de.danielbechler.util.Exceptions;

public class EntityIntrospector implements Introspector {

   private static Log log = LogFactory.getLog(EntityIntrospector.class);

   @Override
   public TypeInfo introspect(Class<?> type) {
      Assert.notNull(type, "type");
      try {
         return internalIntrospect(type);
      } catch (final IntrospectionException e) {
         throw Exceptions.escalate(e);
      }
   }

   private TypeInfo internalIntrospect(final Class<?> type) throws IntrospectionException {
      final TypeInfo typeInfo = new TypeInfo(type);
      BeanInfo beanInfo = java.beans.Introspector.getBeanInfo(type);
      final PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();
      for (final PropertyDescriptor descriptor : descriptors) {
         // if (log.isDebugEnabled()) {
         // log.debug(descriptor);
         // }
         if (shouldSkip(descriptor)) {
            continue;
         }

         if (include(type, descriptor)) {
            final PropertyAwareAccessor accessor = new PropertyAccessor(descriptor.getName(),
                  descriptor.getReadMethod(), descriptor.getWriteMethod());
            typeInfo.addPropertyAccessor(accessor);
         }
      }
      return typeInfo;
   }

   private static boolean shouldSkip(final PropertyDescriptor descriptor) {
      if (descriptor.getName().equals("class")) // Java & Groovy
      {
         return true;
      }
      if (descriptor.getName().equals("metaClass")) // Groovy
      {
         return true;
      }
      if (descriptor.getReadMethod() == null) {
         return true;
      }
      return false;
   }

   private Field findField(final Class<?> type, final String propertyName) {
      String fieldName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
      Class<?> clazz = type;
      while (clazz != null) {
         try {
            return clazz.getDeclaredField(fieldName);
         } catch (NoSuchFieldException e) {
         }
         clazz = clazz.getSuperclass();
      }
      log.info("No Java Bean property: No such field " + fieldName + " in " + type + " class hierarchy");
      return null;
   }

   private boolean include(Class<?> type, PropertyDescriptor descriptor) {
      Field field = findField(type, descriptor.getName());
      if (field == null) {
         return false;
      }
      if (hasAnnotation(Version.class, descriptor.getReadMethod(), field)) {
         return false;
      }
      if (hasAnnotation(Transient.class, descriptor.getReadMethod(), field)) {
         return false;
      }
      if (Modifier.isStatic(field.getModifiers())) {
         return false;
      }
      if (Modifier.isTransient(field.getModifiers())) {
         return false;
      }
      return true;
   }

   private boolean hasAnnotation(Class<? extends Annotation> annotationClass, Method readMethod, Field field) {
      Annotation ann = readMethod.getAnnotation(annotationClass);
      if (ann != null) {
         return true;
      }
      if (field == null) return false;
      ann = field.getAnnotation(annotationClass);
      return (ann != null);
   }

}
