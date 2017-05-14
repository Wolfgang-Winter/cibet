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
package com.logitags.jmeter;

import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;

@Stateless
public class MonitorEjb {

   private static Logger log = Logger.getLogger(MonitorEjb.class);

   @PersistenceContext(unitName = "jmeterPU")
   private EntityManager em;

   public String calc(int count, String param2) {
      log.debug("call ejb method calc");
      MonitorTestClass testClass = new MonitorTestClass();
      return testClass.calc(count, param2);
   }

   public String io(int count, String param2) {
      log.debug("call ejb method io");
      MonitorTestClass testClass = new MonitorTestClass();
      return testClass.io(count, param2);
   }

   public String insert(int count, String param2) {
      Random rnd = new Random(new Date().getTime());

      for (int i = 1; i < count; i++) {
         String str1 = String.valueOf(rnd.nextLong());
         String str2 = String.valueOf(rnd.nextLong());
         JMEntity e = new JMEntity(str1, i, str2);
         em.persist(e);
      }
      return count + " JMEntity objects persisted";
   }

   public String cibetSelect(int count, String param2) {
      return select(count, param2);
   }

   public String select(int count, String param2) {
      Random rnd = new Random(new Date().getTime());
      int i1 = rnd.nextInt(999);
      int i2 = rnd.nextInt(999);

      TypedQuery<JMEntity> q = em.createNamedQuery(JMEntity.SEL, JMEntity.class);
      q.setParameter("nameValue", "%" + i1 + "%");
      q.setParameter("owner", "%" + i2 + "%");

      List<JMEntity> list = q.getResultList();
      String str = "";
      for (JMEntity jm : list) {
         str += jm.getOwner().substring(0, 2);
      }
      return list.size() + " JMEntity objects selected with nameValue like " + i1 + " and owner like " + i2 + ": "
            + str;
   }

   public String cibetCalc(int count, String param2) {
      log.debug("call ejb method cibetCalc");
      MonitorTestClass testClass = new MonitorTestClass();
      return testClass.cibetCalc(count, param2);
   }

   public String cibetIo(int count, String param2) {
      log.debug("call ejb method cibetIo");
      MonitorTestClass testClass = new MonitorTestClass();
      return testClass.cibetIo(count, param2);
   }

}
