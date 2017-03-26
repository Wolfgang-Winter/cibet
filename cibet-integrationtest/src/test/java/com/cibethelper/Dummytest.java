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
package com.cibethelper;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import org.junit.Ignore;
import org.junit.Test;

import com.cibethelper.base.CoreTestBase;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.Te1;
import com.cibethelper.entities.Te2;

public class Dummytest extends CoreTestBase {

   @Ignore
   @Test
   public void test() {
      EntityManager em = Persistence.createEntityManagerFactory("localTest").createEntityManager();
      TComplexEntity ce = createTComplexEntity();
      em.getTransaction().begin();
      em.persist(ce);
      em.getTransaction().commit();
   }

   // @Test
   public void test2() {
      EntityManager em = Persistence.createEntityManagerFactory("localTest").createEntityManager();
      Te2 t2 = new Te2();
      t2.setCounter(15);
      Te2 t3 = new Te2();
      t3.setCounter(15);

      Te1 t1 = new Te1();
      t1.setCounter(20);
      t1.setTe2(t2);
      t1.getLazyList().add(t3);

      em.getTransaction().begin();
      em.persist(t1);
      em.getTransaction().commit();
   }

}
