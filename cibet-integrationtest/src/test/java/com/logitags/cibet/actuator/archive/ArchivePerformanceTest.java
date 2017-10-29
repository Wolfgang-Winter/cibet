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

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Test;

import com.cibethelper.base.DBHelper;
import com.cibethelper.entities.TComplexEntity;
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

      // for (int i = 1; i < 900; i++) {
      // TComplexEntity ce = createTComplexEntity();
      // applEman.persist(ce);
      // }
      //
      // applEman.getTransaction().commit();
      // applEman.getTransaction().begin();

      log.info("start measuring");
      long s1 = System.currentTimeMillis();
      List<Archive> archives = ArchiveLoader.loadAllArchives();
      log.info("list size: " + archives.size());

      // Map<Archive, List<Difference>> difs = ArchiveLoader.analyzeDifferences(archives);
      for (Archive v : archives) {
         for (ResourceParameter rp : v.getResource().getParameters()) {
            log.info(rp);
         }

      }
      long s2 = System.currentTimeMillis();
      long m = s2 - s1;
      log.info("stop measuring: " + m);
   }

}
