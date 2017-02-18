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
package com.logitags.cibet.sensor.jdbc.bridge;

import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.sensor.jpa.EmptyProviderUtil;
import com.logitags.cibet.sensor.jpa.Provider;

public class JdbcBridgeProvider extends Provider {

   private Log log = LogFactory.getLog(JdbcBridgeProvider.class);

   private static final String JAVAX_URL = "javax.persistence.jdbc.url";
   private static final String JAVAX_USER = "javax.persistence.jdbc.user";
   private static final String JAVAX_PASSWORD = "javax.persistence.jdbc.password";
   private static final String HIBERNATE_URL = "hibernate.connection.url";
   private static final String HIBERNATE_USER = "hibernate.connection.user";
   private static final String HIBERNATE_PASSWORD = "hibernate.connection.password";

   @Override
   public EntityManagerFactory createEntityManagerFactory(String unitName, Map map) {
      PersistenceUnitInfo info = createPersistenceUnitInfo(unitName);
      if (info == null) {
         log.info("no persistence unit found with name " + unitName);
         return null;
      }

      if (getClass().getName().equals(info.getPersistenceProviderClassName())
            || (map != null && getClass().getName().equals(map.get(PERSISTENCE_PROVIDER_PROPERTY)))) {
         log.info("create resource-local JdbcBridgeEntityManagerFactory");
         logInfo(info, map);

         return new JdbcBridgeEntityManagerFactory(resolveDataSource(info));

      } else {
         log.debug(this.getClass().getName() + " does not match provider for persistence unit " + unitName);
         return null;
      }
   }

   @Override
   public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map map) {
      log.info("create container JdbcBridgeEntityManagerFactory");
      logInfo(info, map);

      return new JdbcBridgeEntityManagerFactory(info.getJtaDataSource());

      // String err = this.getClass().getName() + " cannot be applied/injected in a container managed envronment.";
      // log.error(err);
      // throw new IllegalStateException(err);
   }

   @Override
   public ProviderUtil getProviderUtil() {
      return new EmptyProviderUtil();
   }

   private DataSource resolveDataSource(PersistenceUnitInfo info) {
      String url = null;
      String user = null;
      String password = null;

      if (info.getProperties().containsKey(HIBERNATE_URL)) {
         url = info.getProperties().getProperty(HIBERNATE_URL);
         user = info.getProperties().getProperty(HIBERNATE_USER);
         password = info.getProperties().getProperty(HIBERNATE_PASSWORD);
         return new DefaultDataSource(url, user, password);
      }

      if (info.getProperties().containsKey(JAVAX_URL)) {
         url = info.getProperties().getProperty(JAVAX_URL);
         user = info.getProperties().getProperty(JAVAX_USER);
         password = info.getProperties().getProperty(JAVAX_PASSWORD);
         return new DefaultDataSource(url, user, password);
      }

      return info.getNonJtaDataSource();
   }

}
