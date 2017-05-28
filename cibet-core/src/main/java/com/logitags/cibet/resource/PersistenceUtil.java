/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2016 Dr. Wolfgang Winter
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
package com.logitags.cibet.resource;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.AnnotationNotFoundException;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.diff.Difference;
import com.logitags.cibet.diff.DifferenceType;
import com.logitags.cibet.sensor.jdbc.driver.JdbcResource;
import com.logitags.cibet.sensor.jdbc.driver.SqlParameter;
import com.logitags.cibet.sensor.jdbc.driver.SqlParser;
import com.logitags.cibet.sensor.jpa.JpaResource;

public abstract class PersistenceUtil {

   private static Log log = LogFactory.getLog(PersistenceUtil.class);

   private static final String DIRTY_DIFFERENCES_KEY = "__DIRTY_DIFFERENCES";

   /**
    * returns the list of differences between an updated entity and the still unmodified version in the database.
    * 
    * @param metadata
    * @return
    */
   public static List<Difference> getDirtyUpdates(EventMetadata metadata) {
      List<Difference> diffs = (List<Difference>) metadata.getProperties().get(DIRTY_DIFFERENCES_KEY);
      if (diffs == null) {
         if (metadata.getResource().getObject() instanceof String) {
            // jdbc
            diffs = new ArrayList<Difference>();

            SqlParser parser = new SqlParser(null, (String) metadata.getResource().getObject());
            List<SqlParameter> setColumns = parser.getInsertUpdateColumns();

            for (SqlParameter par : setColumns) {
               Difference d = new Difference();
               d.setDifferenceType(DifferenceType.NOT_SPECIFIED);
               d.setNewValue(par.getValue());
               d.setPropertyName(par.getColumn());
               d.setCanonicalPath(par.getColumn());
               diffs.add(d);
            }

         } else {
            // jpa
            JpaResource jpar = (JpaResource) metadata.getResource();
            Object cleanObject = getCleanResource(jpar);
            if (cleanObject == null) {
               String msg = "failed to load object from database: no object of class "
                     + jpar.getObject().getClass().getName() + " with id " + jpar.getPrimaryKeyObject() + " found";
               log.error(msg);
               throw new IllegalStateException(msg);
            }
            diffs = CibetUtil.compare(jpar.getObject(), cleanObject);
         }
         metadata.getProperties().put(DIRTY_DIFFERENCES_KEY, diffs);
      }
      return diffs;
   }

   /**
    * loads the clean entity from the database.
    */
   public static Object getCleanResource(Resource resource) {
      if (resource instanceof JdbcResource) {
         return null;
      }

      JpaResource jpar = (JpaResource) resource;
      try {
         try {
            if (jpar.getPrimaryKeyObject() == null) {
               String msg = "failed to load object from database: no value for primary key found in persisted object "
                     + jpar.getObject();
               log.error(msg);
               throw new IllegalStateException(msg);
            }
         } catch (AnnotationNotFoundException e) {
            throw new IllegalStateException(e.getMessage(), e);
         }

         EntityManager internalEM = Context.internalRequestScope().getApplicationEntityManager2();
         if (internalEM == null) {
            String msg = "failed to load object from database: "
                  + "Set the internal EntityManager in CibetContext with "
                  + "method setInternalEntityManager(EntityManager manager)"
                  + ". This instance of EntityManager may not be the same " + "as the EntityManager set with method "
                  + "setEntityManager(EntityManager manager)";
            log.error(msg);
            throw new IllegalStateException(msg);
         }

         Object storedObject = internalEM.find(jpar.getObject().getClass(), jpar.getPrimaryKeyObject());
         if (storedObject == null) {
            String msg = "failed to load object from database: no object of class "
                  + jpar.getObject().getClass().getName() + " with id " + jpar.getPrimaryKeyObject() + " found";
            log.error(msg);
            throw new IllegalStateException(msg);
         }
         CibetUtil.loadLazyEntities(storedObject, jpar.getObject().getClass());

         return storedObject;

      } finally {
         EntityManager internalEM = Context.internalRequestScope().getApplicationEntityManager2();
         if (internalEM != null)
            internalEM.clear();
      }
   }

}
