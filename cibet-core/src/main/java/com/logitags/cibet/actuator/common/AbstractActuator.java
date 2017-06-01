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
package com.logitags.cibet.actuator.common;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.jdbc.driver.JdbcResource;
import com.logitags.cibet.sensor.jpa.JpaResource;

/**
 * abstract implementation of Actuator that does nothing. Inherit custom Actuator implementations from this class to
 * hide future interface enhancements.
 */
public abstract class AbstractActuator implements Actuator, Serializable {

   private transient Log log = LogFactory.getLog(AbstractActuator.class);

   /**
    * 
    */
   private static final long serialVersionUID = 8913249567914997747L;

   private String name;

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.Controller#getSchemeName()
    */
   public String getName() {
      return name;
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.Controller#setName(java.lang.String)
    */
   public void setName(String name) {
      this.name = name;
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if (obj == null || obj.getClass() != this.getClass()) {
         return false;
      }
      return ((Actuator) obj).getName().equals(getName());
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      return getName().hashCode();
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.core.Actuator#beforeEvent(com.logitags.cibet.core. EventMetadata)
    */
   @Override
   public void beforeEvent(EventMetadata ctx) {
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.core.Actuator#afterEvent(com.logitags.cibet.core. EventMetadata)
    */
   @Override
   public void afterEvent(EventMetadata ctx) {
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.core.Actuator#init()
    */
   @Override
   public void init(Configuration config) {
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.core.Actuator#close()
    */
   @Override
   public void close() {
   }

   /**
    * 
    * Sets the PostposedException class either to com.logitags.cibet.actuator.common.PostponedEjbException or to
    * com.logitags.cibet.actuator.common.PostponedException. The EJB variant is annotated with ApplicationException to
    * prevent transaction rollback. The Exception classes are not used explicitly to prevent dependencies to JavaEE
    * classes if we are not in a JavaEE environment.
    * 
    * @return resolved class type
    */
   protected Class<? extends PostponedException> resolvePostponedExceptionType() {
      try {
         Class.forName("javax.ejb.ApplicationException");
         log.debug("we are in EJB environment");
         try {
            return (Class<? extends PostponedException>) Class
                  .forName("com.logitags.cibet.actuator.common.PostponedEjbException");
         } catch (Exception e1) {
            log.error(e1.getMessage(), e1);
            throw new RuntimeException(e1);
         }
      } catch (ClassNotFoundException e) {
         log.debug("we are in non-EJB environment");
         try {
            return PostponedException.class;
         } catch (Exception e1) {
            log.error(e1.getMessage(), e1);
            throw new RuntimeException(e1);
         }
      }
   }

   /**
    * Sets the DeniedException class either to com.logitags.cibet.actuator.common.DeniedEjbException or to
    * com.logitags.cibet.actuator.common.DeniedException. The EJB variant is annotated with ApplicationException to
    * prevent transaction rollback. The Exception classes are not used explicitly to prevent dependencies to JavaEE
    * classes if we are not in a JavaEE environment.
    * 
    * @return resolved class type
    */
   protected Class<? extends DeniedException> resolveDeniedExceptionType() {
      try {
         Class.forName("javax.ejb.ApplicationException");
         log.debug("we are in EJB environment");
         try {
            return (Class<? extends DeniedException>) Class
                  .forName("com.logitags.cibet.actuator.common.DeniedEjbException");
         } catch (Exception e1) {
            log.error(e1.getMessage(), e1);
            throw new RuntimeException(e1);
         }
      } catch (ClassNotFoundException e) {
         log.debug("we are in non-EJB environment");
         try {
            return DeniedException.class;
         } catch (Exception e1) {
            log.error(e1.getMessage(), e1);
            throw new RuntimeException(e1);
         }
      }
   }

   /**
    * loads all lazy properties of an entity and detaches all properties from the persistence context.
    * 
    * @param metadata
    *           EventMetadata object
    */
   protected void loadEager(EventMetadata metadata) {
      if (metadata.getResource() instanceof JpaResource && !(metadata.getResource() instanceof JdbcResource)
            && !(metadata.getResource().getUnencodedTargetObject() instanceof Class)) {

         JpaResource jpar = (JpaResource) metadata.getResource();
         if (!jpar.isEagerLoadedAndDetached()) {
            log.debug("start loadEager");
            Object resourceObject = jpar.getUnencodedTargetObject();
            CibetUtil.loadLazyEntities(resourceObject, resourceObject.getClass());
            List<Object> references = new ArrayList<Object>();
            references.add(resourceObject);
            CibetUtil.deepDetach(resourceObject, references);
            jpar.setUnencodedTargetObject(resourceObject);
            jpar.setEagerLoadedAndDetached(true);
            log.debug("end loadEager");
         }
      }
   }

   protected void addStoredProperties(Resource resource, Collection<String> storedProperties) {
      if (storedProperties != null) {
         Object target = null;
         if (resource.getUnencodedTargetObject() instanceof Class) {
            if (resource.getResultObject() != null) {
               target = resource.getResultObject();
            }

         } else {
            target = resource.getUnencodedTargetObject();
         }

         if (target != null) {
            for (String prop : storedProperties) {
               String getter = "get" + prop.substring(0, 1).toUpperCase() + prop.substring(1);
               try {
                  Method m = target.getClass().getMethod(getter);
                  Object property = m.invoke(target);
                  ResourceParameter propertyResParam = new ResourceParameter(prop, m.getReturnType().getName(),
                        property, ParameterType.ENTITY_PROPERTY, resource.getParameters().size() + 1);
                  if (property != null) {
                     // set value for search queries
                     propertyResParam.setStringValue(property.toString());
                  }

                  resource.addParameter(propertyResParam);
               } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                     | InvocationTargetException e) {
                  log.error("Ignore storing of entity attribute " + prop + ": " + e.getMessage());
               }
            }
         }
      }
   }

}
