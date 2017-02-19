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
package com.logitags.cibet.actuator.envers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.event.EnversIntegrator;
import org.hibernate.envers.event.EnversListenerDuplicationStrategy;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

public class CibetIntegrator extends EnversIntegrator {

   private transient Log log = LogFactory.getLog(CibetIntegrator.class);

   private AuditConfiguration enversConfiguration;

   @Override
   public void disintegrate(SessionFactoryImplementor arg0, SessionFactoryServiceRegistry arg1) {
      super.disintegrate(arg0, arg1);
   }

   @Override
   public void integrate(Configuration configuration, SessionFactoryImplementor sessionFactory,
         SessionFactoryServiceRegistry serviceRegistry) {
      log.info("start CibetIntegrator");
      final boolean autoRegister = ConfigurationHelper.getBoolean(AUTO_REGISTER, configuration.getProperties(), true);
      if (autoRegister) {
         log.debug("Skipping Cibet Envers listener auto registration");
         return;
      }

      log.info("CibetIntegrator registers Cibet Envers listeners");

      EventListenerRegistry listenerRegistry = serviceRegistry.getService(EventListenerRegistry.class);
      listenerRegistry.addDuplicationStrategy(EnversListenerDuplicationStrategy.INSTANCE);

      enversConfiguration = AuditConfiguration.getFor(configuration,
            serviceRegistry.getService(ClassLoaderService.class));

      if (enversConfiguration.getEntCfg().hasAuditedEntities()) {
         listenerRegistry.appendListeners(EventType.POST_DELETE, new CibetPostDeleteEventListener(enversConfiguration));
         listenerRegistry.appendListeners(EventType.POST_INSERT, new CibetPostInsertEventListener(enversConfiguration));
         listenerRegistry.appendListeners(EventType.POST_UPDATE, new CibetPostUpdateEventListener(enversConfiguration));
         listenerRegistry.appendListeners(EventType.POST_COLLECTION_RECREATE,
               new CibetPostCollectionRecreateEventListener(enversConfiguration));
         listenerRegistry.appendListeners(EventType.PRE_COLLECTION_REMOVE,
               new CibetPreCollectionRemoveEventListener(enversConfiguration));
         listenerRegistry.appendListeners(EventType.PRE_COLLECTION_UPDATE,
               new CibetPreCollectionUpdateEventListener(enversConfiguration));
      }
   }

   @Override
   public void integrate(MetadataImplementor arg0, SessionFactoryImplementor arg1, SessionFactoryServiceRegistry arg2) {
      if (enversConfiguration != null) {
         enversConfiguration.destroy();
      }
   }

}
