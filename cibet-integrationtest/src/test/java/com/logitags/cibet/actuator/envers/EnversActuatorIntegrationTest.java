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

import java.net.URL;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditQuery;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cibethelper.AuditedTComplexEntity;
import com.cibethelper.AuditedTEntity;
import com.cibethelper.base.DBHelper;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.archive.ArchiveLoader;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.resource.ResourceParameter;

public class EnversActuatorIntegrationTest extends DBHelper {

   private static Logger log = Logger.getLogger(EnversActuatorIntegrationTest.class);

   @BeforeClass
   public static void subBeforeClass() throws Exception {
      log.debug("subBeforeClass");
      URL url = Thread.currentThread().getContextClassLoader().getResource("jndi_.properties");
      Properties properties = new Properties();
      properties.load(url.openStream());
      if (properties.getProperty(javax.naming.Context.INITIAL_CONTEXT_FACTORY).contains("openejb")) {
         APPSERVER = TOMEE;
      } else if (properties.getProperty(javax.naming.Context.INITIAL_CONTEXT_FACTORY).contains("jboss")) {
         APPSERVER = JBOSS;
      }

      initConfiguration("config_envers1.xml");
   }

   @Before
   public void before() {
      Assume.assumeTrue(JBOSS.equals(APPSERVER));
   }

   @After
   public void subDoAfter() {
      log.info("subDoAfter");
      deleteAud();
   }

   protected AuditedTEntity createAuditedTEntity(int counter, String name) {
      AuditedTEntity te = new AuditedTEntity();
      te.setCounter(counter);
      te.setNameValue(name);
      te.setOwner(TENANT);
      return te;
   }

   protected AuditedTComplexEntity createAuditedTComplexEntity() {
      AuditedTEntity e1 = new AuditedTEntity("val3", 3, TENANT);
      AuditedTEntity e2 = new AuditedTEntity("val4", 4, TENANT);
      AuditedTEntity e3 = new AuditedTEntity("val5", 5, TENANT);
      AuditedTEntity e4 = new AuditedTEntity("val6", 6, TENANT);
      AuditedTEntity e5 = new AuditedTEntity("val7", 7, TENANT);

      Set<AuditedTEntity> lazyList = new LinkedHashSet<AuditedTEntity>();
      lazyList.add(e2);
      lazyList.add(e3);
      Set<AuditedTEntity> eagerList = new LinkedHashSet<AuditedTEntity>();
      eagerList.add(e4);
      eagerList.add(e5);

      AuditedTComplexEntity ce = new AuditedTComplexEntity();
      ce.setCompValue(12);
      ce.setTen(e1);
      ce.setOwner(TENANT);
      ce.setEagerList(eagerList);
      ce.setLazyList(lazyList);
      ce.addLazyList(createAuditedTEntity(6, "Hase6"));

      return ce;
   }

   private void deleteAud() {
      Query q = applEman.createNativeQuery("DELETE FROM CIB_COMPLEXTESTENTITY_AUD");
      q.executeUpdate();
      q = applEman.createNativeQuery("DELETE FROM CIB_TCOMPLEXENTITY_EAGER_AUD");
      q.executeUpdate();
      q = applEman.createNativeQuery("DELETE FROM CIB_TCOMPLEXENTITY_LAZY_AUD");
      q.executeUpdate();
      q = applEman.createNativeQuery("DELETE FROM CIB_TESTENTITY_AUD");
      q.executeUpdate();
      q = applEman.createNativeQuery("DELETE FROM REVINFO");
      q.executeUpdate();

      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
   }

   @Test
   public void persist1() throws Exception {
      log.info("start persist1()");

      AuditedTComplexEntity ce = createAuditedTComplexEntity();
      applEman.persist(ce);
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      List<Archive> archList = ArchiveLoader.loadArchivesByPrimaryKeyId(AuditedTComplexEntity.class.getName(),
            String.valueOf(ce.getId()));
      Assert.assertEquals(1, archList.size());
      Resource res0 = archList.get(0).getResource();
      Assert.assertEquals(2, res0.getParameters().size());
      ResourceParameter rp1 = res0.getParameters().iterator().next();
      ResourceParameter rp2 = res0.getParameters().iterator().next();
      Assert.assertTrue("compValue".equals(rp1.getName()) || "owner".equals(rp1.getName()));
      Assert.assertTrue("compValue".equals(rp2.getName()) || "owner".equals(rp2.getName()));

      AuditReader ar = AuditReaderFactory.get(applEman);
      AuditQuery query = ar.createQuery().forRevisionsOfEntity(AuditedTComplexEntity.class, true, true);
      List<AuditedTComplexEntity> resList = query.getResultList();
      Assert.assertEquals(1, resList.size());
      AuditedTComplexEntity ce2 = resList.get(0);
      Assert.assertEquals(2, ce2.getEagerList().size());
   }

   @Test
   public void persist2() throws Exception {
      log.info("start persist2()");
      AuditedTComplexEntity ce = createAuditedTComplexEntity();
      applEman.persist(ce);
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      List<Archive> archList = ArchiveLoader.loadArchivesByPrimaryKeyId(AuditedTComplexEntity.class.getName(),
            String.valueOf(ce.getId()));
      Assert.assertEquals(0, archList.size());

      AuditReader ar = AuditReaderFactory.get(applEman);
      AuditQuery query = ar.createQuery().forRevisionsOfEntity(AuditedTComplexEntity.class, true, true);
      List<AuditedTComplexEntity> resList = query.getResultList();
      Assert.assertEquals(0, resList.size());
   }

   @Test
   public void delete1() throws Exception {
      log.info("start delete1()");
      AuditedTComplexEntity ce = createAuditedTComplexEntity();
      applEman.persist(ce);
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      applEman.remove(ce);
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      AuditReader ar = AuditReaderFactory.get(applEman);
      AuditQuery query = ar.createQuery().forRevisionsOfEntity(AuditedTComplexEntity.class, true, true);
      List<AuditedTComplexEntity> resList = query.getResultList();
      Assert.assertEquals(1, resList.size());
      AuditedTComplexEntity ce2 = resList.get(0);
      log.debug(ce2);
      Assert.assertEquals(2, ce2.getEagerList().size());
   }

   @Test
   public void update1() throws Exception {
      log.info("start update1()");
      AuditedTComplexEntity ce = createAuditedTComplexEntity();
      applEman.persist(ce);
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      ce.setOwner("new owner");
      ce = applEman.merge(ce);
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      AuditReader ar = AuditReaderFactory.get(applEman);
      AuditQuery query = ar.createQuery().forRevisionsOfEntity(AuditedTComplexEntity.class, true, true);
      List<AuditedTComplexEntity> resList = query.getResultList();
      Assert.assertEquals(1, resList.size());

      AuditedTComplexEntity ce2 = resList.get(0);
      Assert.assertEquals("new owner", ce2.getOwner());
   }

}
