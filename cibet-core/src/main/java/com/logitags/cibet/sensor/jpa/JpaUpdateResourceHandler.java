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
package com.logitags.cibet.sensor.jpa;

import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.dc.ResourceApplyException;
import com.logitags.cibet.actuator.scheduler.SchedulerActuator;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalRequestScope;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.resource.ResourceParameter;

import de.danielbechler.diff.ObjectMerger;

public class JpaUpdateResourceHandler extends JpaResourceHandler {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private static Log log = LogFactory.getLog(JpaUpdateResourceHandler.class);

   public JpaUpdateResourceHandler(Resource res) {
      super(res);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.sensor.jpa.JpaResourceHandler#apply(com.logitags.cibet.core.ControlEvent)
    */
   @Override
   public Object apply(ControlEvent event) throws ResourceApplyException {
      EntityManager appEM = Context.internalRequestScope().getApplicationEntityManager();
      Class<?> clazz;
      try {
         clazz = Class.forName(resource.getTargetType());
      } catch (Exception e) {
         String err = "Failed to apply scheduled update of class " + resource.getTargetType() + ": " + e.getMessage();
         throw new ResourceApplyException(err, e);
      }

      ControlEvent original = (ControlEvent) Context.internalRequestScope()
            .getProperty(InternalRequestScope.CONTROLEVENT);
      Context.internalRequestScope().removeProperty(InternalRequestScope.CONTROLEVENT);
      Object head = appEM.find(clazz, resource.getPrimaryKeyObject());
      if (head == null) {
         String err = "Failed to find entity " + clazz + " with ID " + resource.getPrimaryKeyObject()
               + " in database for executing scheduled update";
         throw new ResourceApplyException(err);
      }

      CibetUtil.loadLazyEntities(head, head.getClass());
      ResourceParameter headRP = new ResourceParameter(SchedulerActuator.ORIGINAL_OBJECT, head.getClass().getName(),
            head, ParameterType.INTERNAL_PARAMETER, resource.getParameters().size() + 1);
      resource.getParameters().add(headRP);

      ResourceParameter rp = resource.getParameter(SchedulerActuator.CLEANOBJECT);
      if (rp == null) {
         String err = "Failed to find base entity of " + clazz + " with ID " + resource.getPrimaryKeyObject()
               + " for executing scheduled update";
         throw new RuntimeException(err);
      }

      try {
         ObjectMerger merger = new ObjectMerger(CibetUtil.getObjectDiffer());
         if (log.isDebugEnabled()) {
            log.debug("start merging");
            // log.debug("work: " + resource.getObject());
            log.debug("base: " + rp.getUnencodedValue());
            log.debug("head: " + head);
         }
         head = merger.merge(resource.getObject(), rp.getUnencodedValue(), head);
         if (log.isDebugEnabled()) {
            log.debug("end merging");
            log.debug("head: " + head);
         }

         Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, original);
         Object newHead = appEM.merge(head);
         log.debug("new head: " + newHead + "-" + newHead.hashCode());
         return newHead;
      } finally {
         Context.internalRequestScope().removeProperty(InternalRequestScope.CONTROLEVENT);
      }
   }

}
