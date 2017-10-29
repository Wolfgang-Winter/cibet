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
package com.logitags.cibet.actuator.owner;

import java.lang.reflect.Method;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.cibethelper.entities.owner.Company;
import com.cibethelper.entities.owner.Merchant;
import com.cibethelper.entities.owner.Merchant2;
import com.cibethelper.entities.owner.Merchant3;
import com.cibethelper.entities.owner.Merchant4;
import com.cibethelper.entities.owner.Product;
import com.cibethelper.entities.owner.Product2;
import com.cibethelper.entities.owner.Transaction;

public class OwnerTest {

   private static Logger log = Logger.getLogger(OwnerTest.class);

   @Test
   public void testOwner1() throws Exception {
      Merchant m1 = new Merchant();
      m1.setId(1);
      m1.setTenant("Ralf");

      Merchant m2 = new Merchant();
      m2.setId(2);
      m2.setParent(m1);
      m2.setTenant("Ralf");

      OwnerCheckActuator actuator = new OwnerCheckActuator();

      Method m = OwnerCheckActuator.class.getDeclaredMethod("buildOwnerString", Object.class);
      m.setAccessible(true);
      String token = (String) m.invoke(actuator, m2);
      log.info("owner token = " + token);
      Assert.assertEquals("Ralf|1|2", token);

      token = (String) m.invoke(actuator, m1);
      log.info("owner token = " + token);
      Assert.assertEquals("Ralf|1", token);
   }

   @Test
   public void testOwner2() throws Exception {
      Product p1 = new Product();
      p1.setId("pp3");
      p1.setTenant("Ralf");

      OwnerCheckActuator actuator = new OwnerCheckActuator();

      Method m = OwnerCheckActuator.class.getDeclaredMethod("buildOwnerString", Object.class);
      m.setAccessible(true);
      String token = (String) m.invoke(actuator, p1);
      log.info("owner token = " + token);
      Assert.assertEquals("Ralf|pp3", token);
   }

   @Test
   public void testOwner3() throws Exception {
      Product2 p1 = new Product2();
      p1.setId("pp3");
      p1.setTenant(7);

      OwnerCheckActuator actuator = new OwnerCheckActuator();

      Method m = OwnerCheckActuator.class.getDeclaredMethod("buildOwnerString", Object.class);
      m.setAccessible(true);
      String token = (String) m.invoke(actuator, p1);
      log.info("owner token = " + token);
      Assert.assertEquals("7|pp3", token);
   }

   @Test
   public void testOwner4() throws Exception {
      Company c = new Company();
      c.setId("c1");
      c.setTenant("Ralf");

      Merchant2 m1 = new Merchant2();
      m1.setId(1);
      m1.setTenant("Ralf");
      m1.setCompany(c);

      Merchant2 m2 = new Merchant2();
      m2.setId(2);
      m2.setParent(m1);
      m2.setTenant("Ralf");

      Merchant2 m3 = new Merchant2();
      m3.setId(3);
      m3.setParent(m2);
      m3.setTenant("Ralf");
      m3.setCompany(c);

      OwnerCheckActuator actuator = new OwnerCheckActuator();

      Method m = OwnerCheckActuator.class.getDeclaredMethod("buildOwnerString", Object.class);
      m.setAccessible(true);
      String token = (String) m.invoke(actuator, m3);
      log.info("owner token = " + token);
      Assert.assertEquals("Ralf|c1|1|2|3", token);
   }

   @Test
   public void testOwner5() throws Exception {
      Merchant3 m1 = new Merchant3(1, "ten1", "DE");
      Merchant3 m2 = new Merchant3(2, "ten1", null);

      m1.setParent(m2);

      Transaction txn = new Transaction("3", "ten1");
      txn.setMerchant(m1);

      OwnerCheckActuator actuator = new OwnerCheckActuator();

      Method m = OwnerCheckActuator.class.getDeclaredMethod("buildOwnerString", Object.class);
      m.setAccessible(true);
      String token = (String) m.invoke(actuator, txn);
      log.info("owner token = " + token);
      Assert.assertEquals("ten1|2|DE|1|3", token);

      token = (String) m.invoke(actuator, m2);
      log.info("owner token = " + token);
      Assert.assertEquals("ten1|2", token);

      token = (String) m.invoke(actuator, m1);
      log.info("owner token = " + token);
      Assert.assertEquals("ten1|2|DE|1", token);
   }

   @Test
   public void testOwner6() throws Exception {
      Merchant4 m1 = new Merchant4();
      m1.setId(1);

      Merchant4 m2 = new Merchant4();
      m2.setId(2);
      m2.setParent(m1);

      OwnerCheckActuator actuator = new OwnerCheckActuator();

      Method m = OwnerCheckActuator.class.getDeclaredMethod("buildOwnerString", Object.class);
      m.setAccessible(true);
      String token = (String) m.invoke(actuator, m1);
      log.info("owner token = " + token);
      Assert.assertEquals("1", token);

      token = (String) m.invoke(actuator, m2);
      log.info("owner token = " + token);
      Assert.assertEquals("1|2", token);
   }

}
