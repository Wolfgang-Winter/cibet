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

import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cibethelper.base.DBHelper;
import com.cibethelper.entities.owner.Company;
import com.cibethelper.entities.owner.Merchant;
import com.cibethelper.entities.owner.Merchant2;
import com.cibethelper.entities.owner.Product;
import com.logitags.cibet.context.Context;

public class OwnerIntegrationTest extends DBHelper {

   private static Logger log = Logger.getLogger(OwnerIntegrationTest.class);

   public static String testString;

   @BeforeClass
   public static void beforeClassOwnerIntegrationTest() throws Exception {
      log.debug("OwnerIntegrationTest beforeClass");
      Merchant m1 = new Merchant();
      m1.setId(1);
      m1.setTenant("Ralf");

      Merchant m2 = new Merchant();
      m2.setId(2);
      m2.setParent(m1);
      m2.setTenant("Ralf");

      Product p1 = new Product();
      p1.setId("pp3");
      p1.setTenant("Ralf");

      Company c = new Company();
      c.setId("c1");
      c.setTenant("Ralf");

      Merchant2 m1a = new Merchant2();
      m1a.setId(3);
      m1a.setTenant("Ralf");
      m1a.setCompany(c);

      Merchant2 m2a = new Merchant2();
      m2a.setId(4);
      m2a.setParent(m1a);
      m2a.setTenant("Ralf");

      Merchant2 m3 = new Merchant2();
      m3.setId(5);
      m3.setParent(m2a);
      m3.setTenant("Ralf");
      m3.setCompany(c);

      Merchant mm1 = new Merchant();
      mm1.setId(10);
      mm1.setTenant("Werner");

      Merchant mm2 = new Merchant();
      mm2.setId(12);
      mm2.setParent(mm1);
      mm2.setTenant("Werner");

      Product pp1 = new Product();
      pp1.setId("pp30");
      pp1.setTenant("Werner");

      Company cc = new Company();
      cc.setId("c10");
      cc.setTenant("Werner");

      Merchant2 mm1a = new Merchant2();
      mm1a.setId(13);
      mm1a.setTenant("Werner");
      mm1a.setCompany(cc);

      Merchant2 mm2a = new Merchant2();
      mm2a.setId(14);
      mm2a.setParent(mm1a);
      mm2a.setTenant("Werner");

      Merchant2 mm3 = new Merchant2();
      mm3.setId(15);
      mm3.setParent(mm2a);
      mm3.setTenant("Werner");
      mm3.setCompany(cc);

      applEman.getTransaction().begin();
      applEman.persist(m1);
      applEman.persist(m2);
      applEman.persist(p1);
      applEman.persist(c);
      applEman.persist(m1a);
      applEman.persist(m2a);
      applEman.persist(mm1);
      applEman.persist(mm2);
      applEman.persist(pp1);
      applEman.persist(cc);
      applEman.persist(mm1a);
      applEman.persist(mm2a);
      applEman.getTransaction().commit();

      initConfiguration("cibet-config-owner.xml");
   }

   @AfterClass
   public static void doAfterOwnerIntegrationTest() throws Exception {
      log.debug("OwnerIntegrationTest: doAfter()");

      applEman.clear();
      applEman.getTransaction().begin();

      Query q = applEman.createQuery("delete from Company a");
      q.executeUpdate();

      q = applEman.createQuery("delete from Merchant a");
      q.executeUpdate();

      q = applEman.createQuery("delete from Merchant2 a");
      q.executeUpdate();

      q = applEman.createQuery("delete from Product a");
      q.executeUpdate();

      q = applEman.createQuery("delete from Product2 a");
      q.executeUpdate();

      applEman.getTransaction().commit();
   }

   @Test
   public void insert() {
      log.debug("call insert()");
      Context.sessionScope().setTenant("Ralf");
      Product p1 = new Product();
      p1.setId("np1");
      p1.setTenant("no");
      applEman.persist(p1);
      applEman.flush();

      TypedQuery<Company> q = applEman.createQuery("select a from Company a", Company.class);
      List<Company> list = q.getResultList();
      Assert.assertEquals(1, list.size());

      Context.sessionScope().setTenant(null);
      TypedQuery<Product> q1 = applEman.createQuery("select a from Product a", Product.class);
      List<Product> list1 = q1.getResultList();
      Assert.assertEquals(2, list1.size());
   }

   @Test
   public void insert2() {
      log.debug("call insert2()");
      Context.sessionScope().setTenant("Werner");
      Product p1 = new Product();
      p1.setId("np1");
      p1.setTenant("no");
      try {
         applEman.persist(p1);
         Assert.fail();
      } catch (WrongOwnerException e) {
      }

      applEman.clear();
      TypedQuery<Company> q = applEman.createQuery("select a from Company a", Company.class);
      try {
         q.getResultList();
         Assert.fail();
      } catch (WrongOwnerException e) {
      }
   }

   @Test
   public void findAndMerge() {
      log.debug("call findAndMerge()");
      Context.sessionScope().setTenant("Ralf");
      Product p1 = applEman.find(Product.class, "pp3");
      p1.setProductName("Wanze");
      applEman.merge(p1);

      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      applEman.clear();

      TypedQuery<Product> q1 = applEman.createQuery("select a from Product a where a.tenant = 'Ralf' and a.id = 'pp3'",
            Product.class);
      Product p = q1.getSingleResult();
      Assert.assertEquals("Wanze", p.getProductName());
   }

   @Test
   public void findAndMergeErr() {
      log.debug("call findAndMergeErr()");
      Context.sessionScope().setTenant("Werner");
      Product p1 = applEman.find(Product.class, "pp30");
      Context.sessionScope().setTenant("Ralf");
      p1.setProductName("Wanze");
      applEman.merge(p1);

      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      applEman.clear();

      TypedQuery<Product> q1 = applEman.createQuery("select a from Product a where a.id = 'pp30'", Product.class);
      Product p;
      try {
         p = q1.getSingleResult();
         Assert.fail();
      } catch (NoResultException e) {
      }

      Context.sessionScope().setTenant("Werner");
      p = q1.getSingleResult();
      Assert.assertNull(p.getProductName());
   }

   @Test
   public void deleteErr() {
      log.debug("call deleteErr()");
      Context.sessionScope().setTenant("Werner");
      Product p1 = applEman.find(Product.class, "pp30");
      Context.sessionScope().setTenant("Ralf");

      applEman.remove(p1);

      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      applEman.clear();

      Context.sessionScope().setTenant("Werner");
      TypedQuery<Product> q1 = applEman.createQuery("select a from Product a where a.id = 'pp30'", Product.class);
      Product p = q1.getSingleResult();
   }

   @Test(expected = WrongOwnerException.class)
   public void deleteErr2() {
      log.debug("call deleteErr2()");
      Context.sessionScope().setTenant("Ralf");
      Product p1 = applEman.find(Product.class, "pp3");
      Context.sessionScope().setTenant("Werner");

      applEman.remove(p1);
   }

   @Test(expected = WrongOwnerException.class)
   public void deleteErr3() {
      log.debug("call deleteErr3()");
      Context.sessionScope().setTenant("Werner");
      applEman.find(Product.class, "pp3");
   }

   @Test
   public void findMerchant() {
      log.debug("call findMerchant()");
      Context.sessionScope().setTenant("Werner|c10");
      Merchant2 m = applEman.find(Merchant2.class, 14L);
      Assert.assertNotNull(m);

      Context.sessionScope().setTenant("Werner|xx");
      try {
         applEman.find(Merchant2.class, 14L);
      } catch (WrongOwnerException e) {
      }
   }

   @Test
   public void findMerchant2() {
      log.debug("call findMerchant2()");
      Context.sessionScope().setTenant("Ralf|c1");
      Merchant2 m = applEman.find(Merchant2.class, 3L);
      Assert.assertNotNull(m);

      Context.sessionScope().setTenant("Ralf|xx");
      m = applEman.find(Merchant2.class, 3L);
      Assert.assertNull(m);
   }

   @Test
   public void findMerchant3() {
      log.debug("call findMerchant3()");
      Context.sessionScope().setTenant("Heinz");
      Merchant2 m = applEman.find(Merchant2.class, 3L);
      Assert.assertNull(m);
      Assert.assertNotNull(testString);

      testString = null;
   }

}
