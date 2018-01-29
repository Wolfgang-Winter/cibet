/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2012 Dr. Wolfgang Winter
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
package com.logitags.cibet.http;

import java.io.File;
import java.util.Map.Entry;

import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TransactionRequiredException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.cibethelper.base.CoreTestBase;
import com.cibethelper.ejb.RemoteEJB;
import com.cibethelper.ejb.RemoteEJBImpl;
import com.cibethelper.ejb.SimpleEjb;
import com.cibethelper.entities.AbstractTEntity;
import com.cibethelper.entities.ITComplexEntity;
import com.cibethelper.entities.TCompareEntity;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TComplexEntity2;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.it.AbstractArquillian;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.sensor.jpa.JpaResource;

/**
 * 
 * @author Wolfgang
 * 
 */
@RunWith(Arquillian.class)
public class SimpleEclipseLinkTest {

   private static Logger log = Logger.getLogger(SimpleEclipseLinkTest.class);

   @PersistenceContext(unitName = "APPL-UNIT")
   protected EntityManager applEman;

   @PersistenceContext(unitName = "Cibet")
   protected EntityManager cibet;

   @javax.annotation.Resource
   protected UserTransaction ut;

   @Deployment(testable = true)
   public static WebArchive createDeployment() {
      String warName = SimpleEclipseLinkTest.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("it/web-simple.xml");

      archive.addClasses(AbstractArquillian.class, CoreTestBase.class, AbstractTEntity.class, TEntity.class,
            TComplexEntity.class, TComplexEntity2.class, ITComplexEntity.class, TCompareEntity.class, RemoteEJB.class,
            RemoteEJBImpl.class, SimpleEjb.class);

      File[] cibet = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-jpa").withoutTransitivity()
            .asFile();
      archive.addAsLibraries(cibet);
      File[] spring = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-springsecurity")
            .withTransitivity().asFile();
      archive.addAsLibraries(spring);

      archive.addAsWebInfResource("META-INF/persistence-it.xml", "classes/META-INF/persistence.xml");
      // archive.addAsWebInfResource("config_parallel.xml", "classes/cibet-config.xml");
      archive.addAsWebInfResource("config_2.xml", "classes/cibet-config.xml");
      archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
      archive.addAsWebInfResource("jndi_.properties", "classes/jndi_.properties");

      log.debug(archive.toString(true));
      archive.as(ZipExporter.class).exportTo(new File("target/" + warName), true);

      return archive;
   }

   @Before
   public void beforeSimpleEclipseLinkTest() throws SystemException {
      log.debug("execute before()");
   }

   @After
   public void afterSimpleEclipseLinkTest() throws SystemException {
      log.debug("afterSimpleEclipseLinkTest(");
   }

   /**
    * It can be a side effect of the jpa lazy. 7.0.4(-SNAPSHOT) should behave better thanks to
    * https://issues.apache.org/jira/browse/TOMEE-2075 One workaround is a @Singleton @Startup touching the entity
    * manager (or factory) in its @PostConstruct just to ensure it is loaded at startup.
    * 
    * @throws Exception
    */
   // @Ignore
   @Test
   public void test() throws Exception {
      log.info("start test()");

      // Configuration.instance();
      log.debug("--transaction status: " + ut.getStatus());
      // Context.start();
      // EntityManager cib = createEntityManager();
      // Context.internalRequestScope().setManaged(true);
      // Context.internalRequestScope().getEntityManager();
      // EntityManager cib = Context.internalRequestScope().getOrCreateEntityManager(false);

      ut.begin();

      cibet.joinTransaction();
      InitialContext context = new InitialContext();
      EntityManagerFactory containerEmf = (EntityManagerFactory) context.lookup("java:comp/env/Cibet");
      EntityManager entityManager = containerEmf.createEntityManager();

      Resource res = new JpaResource();
      entityManager.persist(res);

      ut.commit();

      try {
         entityManager.joinTransaction();
      } catch (TransactionRequiredException e) {
         log.info("... but cannot join transaction: " + e.getMessage());
      }

      ut.begin();
      log.debug("--transaction status: " + ut.getStatus());

      for (Entry<String, Object> entry : applEman.getProperties().entrySet()) {
         log.debug(entry.getKey() + " = " + entry.getValue());
      }

      // log.debug(applEman + " " + applEman.isJoinedToTransaction());
      // log.debug(cibet + " " + cibet.isJoinedToTransaction());
      //
      // cibet.joinTransaction();
      // applEman.joinTransaction();

      try {
         Query q = applEman.createNativeQuery("DELETE FROM CIB_COMPLEXTESTENTITY_AUD");
         int count = q.executeUpdate();
         log.debug(count + " rows deleted in CIB_COMPLEXTESTENTITY_AUD");
      } catch (Exception e) {
         log.error(e.getMessage());
      }

      // Query q = cibet.createNativeQuery("DELETE FROM CIB_RESOURCEPARAMETER");
      // int count = q.executeUpdate();
      // log.debug(count + " rows deleted in CIB_RESOURCEPARAMETER");

      ut.commit();
   }

}
