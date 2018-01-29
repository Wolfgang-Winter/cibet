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
package com.logitags.cibet.actuator.history;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.cibethelper.base.DBHelper;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.archive.ArchiveLoader;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.diff.Difference;
import com.logitags.cibet.diff.DifferenceType;
import com.logitags.cibet.sensor.jpa.CibetEntityManager;

public class HistoryIntegrationTest extends DBHelper {

   private static Logger log = Logger.getLogger(HistoryIntegrationTest.class);

   private Setpoint sp1;

   @Test
   public void historyTest1() {
      log.info("start historyTest1()");
      sp1 = registerSetpoint(TComplexEntity.class, HistoryActuator.DEFAULTNAME, ControlEvent.INSERT,
            ControlEvent.UPDATE, ControlEvent.DELETE, ControlEvent.SELECT);

      try {
         TComplexEntity ce = createTComplexEntity();
         applEman.persist(ce);
         applEman.flush();
         applEman.clear();
         long id = ce.getId();

         // applEman.getTransaction().commit();
         // Context.end();
         // Context.start();
         // applEman.getTransaction().begin();

         String query = "select h from History h where h.primaryKeyId = '" + id + "' order by h.createDate";
         EntityManager cibem = Context.internalRequestScope().getEntityManager();
         TypedQuery<History> q = cibem.createQuery(query, History.class);
         List<History> list = q.getResultList();

         Assert.assertEquals(1, list.size());
         log.debug("history:: " + list.get(0));

         if (1 == 1) return;

         TComplexEntity selEnt = applEman.find(TComplexEntity.class, id);
         selEnt.setCompValue(14);
         selEnt = applEman.merge(selEnt);
         applEman.flush();
         applEman.clear();

         TComplexEntity selEnt2 = applEman.find(TComplexEntity.class, ce.getId());
         TEntity e8 = new TEntity("val8", 8, TENANT);
         selEnt2.addLazyList(e8);
         selEnt2 = applEman.merge(selEnt2);
         applEman.flush();
         applEman.clear();

         resetContext();

         TComplexEntity selEnt3 = applEman.find(TComplexEntity.class, ce.getId());
         selEnt3.setOwner("Klaus");
         selEnt3.setCompValue(552);
         selEnt3 = applEman.merge(selEnt3);
         applEman.flush();
         applEman.clear();

         applEman.remove(selEnt3);
         applEman.flush();
         applEman.clear();

         resetContext();
         ((CibetEntityManager) applEman).setLoadEager(false);

         log.debug("now check differences");
         List<Archive> ali = ArchiveLoader.loadArchivesByPrimaryKeyId(TComplexEntity.class.getName(), id);
         Map<Archive, List<Difference>> map = ArchiveLoader.analyzeDifferences(ali);
         Assert.assertEquals(5, map.size());

         Iterator<Archive> iter = map.keySet().iterator();
         iter.next();

         log.debug("now restore");
         TComplexEntity selEnt4 = (TComplexEntity) iter.next().restore(applEman, "Soll: 14");
         applEman.flush();
         applEman.clear();
         Assert.assertEquals(14, selEnt4.getCompValue());

         Context.internalRequestScope().getOrCreateEntityManager(false).getTransaction().commit();
         Context.internalRequestScope().getOrCreateEntityManager(false).clear();
         Context.internalRequestScope().getOrCreateEntityManager(false).getTransaction().begin();
         applEman.getTransaction().commit();
         applEman.getTransaction().begin();

         log.debug("now load 2.");
         ali = ArchiveLoader.loadArchivesByPrimaryKeyId(TComplexEntity.class.getName(), id);
         map = ArchiveLoader.analyzeDifferences(ali);
         Assert.assertEquals(5, map.size());
         Iterator<Entry<Archive, List<Difference>>> iter2 = map.entrySet().iterator();
         Entry<Archive, List<Difference>> e = iter2.next();
         Assert.assertEquals(ControlEvent.INSERT, e.getKey().getControlEvent());
         Assert.assertEquals(0, e.getValue().size());

         e = iter2.next();
         Assert.assertEquals(ControlEvent.UPDATE, e.getKey().getControlEvent());
         Assert.assertEquals(1, e.getValue().size());
         Assert.assertEquals(14, e.getValue().get(0).getNewValue());

         e = iter2.next();
         Assert.assertEquals(ControlEvent.UPDATE, e.getKey().getControlEvent());
         Assert.assertEquals(1, e.getValue().size());
         Assert.assertEquals("lazyList", e.getValue().get(0).getPropertyName());
         Assert.assertEquals(DifferenceType.ADDED, e.getValue().get(0).getDifferenceType());

         e = iter2.next();
         Assert.assertEquals(ControlEvent.UPDATE, e.getKey().getControlEvent());
         Assert.assertEquals(2, e.getValue().size());
         Assert.assertEquals(DifferenceType.MODIFIED, e.getValue().get(0).getDifferenceType());
         Assert.assertEquals(DifferenceType.MODIFIED, e.getValue().get(1).getDifferenceType());

         e = iter2.next();
         Assert.assertEquals(ControlEvent.DELETE, e.getKey().getControlEvent());
         Assert.assertEquals(0, e.getValue().size());

      } finally {
         Configuration.instance().unregisterSetpoint(sp1.getConfigName(), sp1.getId());
      }
   }

}
