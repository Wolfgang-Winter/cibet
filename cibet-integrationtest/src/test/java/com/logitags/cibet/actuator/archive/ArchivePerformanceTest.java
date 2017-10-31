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
package com.logitags.cibet.actuator.archive;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Test;

import com.cibethelper.base.DBHelper;
import com.cibethelper.entities.TComplexEntity;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.resource.ResourceParameter;

public class ArchivePerformanceTest extends DBHelper {

   private static Logger log = Logger.getLogger(ArchivePerformanceTest.class);

   @After
   public void after() {
      log.debug("ArchiveManagerImplIntegrationTest:doAfter()");
      initConfiguration("cibet-config.xml");
   }

   // @Ignore
   @Test
   public void testPerformance() {
      log.info("start testPerformance()");
      registerSetpoint(TComplexEntity.class, ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT);

      // for (int i = 1; i < 2000; i++) {
      // TComplexEntity ce = createTComplexEntity();
      // applEman.persist(ce);
      // }
      //
      // applEman.getTransaction().commit();
      // applEman.getTransaction().begin();

      log.info("start measuring");
      long s1 = System.currentTimeMillis();
      List<?> archives = loadAllArchives2();

      // Map<Archive, List<Difference>> difs = ArchiveLoader.analyzeDifferences(archives);
      for (Object v : archives) {
         // Object[] objs = (Object[]) v;
         // log.info(objs.length + ", 1: " + objs[0] + ", 2:" + objs[1]);
         // JpaResource jp1 = (JpaResource) objs[0];
         // Archive a = (Archive) objs[1];
         Archive a = (Archive) v;
         log.info("resource: " + a.getResource());

         for (ResourceParameter rp : a.getResource().getParameters()) {
            log.info(rp);
         }

      }
      long s2 = System.currentTimeMillis();
      long m = s2 - s1;
      log.info("stop measuring: " + m);
   }

   private List<?> loadAllArchives() {
      Query query = Context.internalRequestScope().getEntityManager().createQuery(
            "SELECT  r, a FROM JpaResource r, Archive a WHERE a.resource = r and r.primaryKeyId = '7220' ORDER BY a.createDate");
      List<Archive> list = (List<Archive>) query.getResultList();
      // make distinct
      // Set<Archive> s = new LinkedHashSet<>(list);
      // List<Archive> list2 = new ArrayList<>(s);
      return list;
   }

   private List<?> loadAllArchives1() {
      Query query = Context.internalRequestScope().getEntityManager()
            .createQuery("SELECT  r, a FROM Archive a, JpaResource r LEFT JOIN FETCH r.parameters "
                  + "WHERE a.resource = r and r.primaryKeyId = '7220' ORDER BY a.createDate");
      List<Archive> list = (List<Archive>) query.getResultList();
      // make distinct
      // Set<Archive> s = new LinkedHashSet<>(list);
      // List<Archive> list2 = new ArrayList<>(s);
      return list;
   }

   private List<?> loadAllArchives2() {
      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(TComplexEntity.class.getName(), "7220");
      log.info("???? list size: " + list.size());
      // make distinct
      // Set<Archive> s = new LinkedHashSet<>(list);
      // List<Archive> list2 = new ArrayList<>(s);
      return list;
   }

   private List<Archive> loadArchivesByPrimaryKeyId(String target, Object primaryKeyId) {
      Query query = Context.internalRequestScope().getEntityManager().createNamedQuery(Archive.SEL_BY_PRIMARYKEYID);
      query.setParameter(1, target);
      if (primaryKeyId == null) {
         query.setParameter(2, null);
      } else {
         query.setParameter(2, primaryKeyId.toString());
      }
      List<Object[]> list = query.getResultList();
      log.info("???? list size: " + list.size());

      applEman.getTransaction().commit();
      applEman.clear();
      applEman.getTransaction().begin();

      Set<Archive> s = new LinkedHashSet<>();
      for (Object[] ob : list) {
         s.add((Archive) ob[0]);
      }

      return new ArrayList<>(s);
   }

}
