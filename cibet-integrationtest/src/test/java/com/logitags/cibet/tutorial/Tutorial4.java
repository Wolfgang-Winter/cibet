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
package com.logitags.cibet.tutorial;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Properties;

import javax.naming.InitialContext;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.logitags.cibet.context.Context;
import com.logitags.cibet.sensor.ejb.CibetRemoteContext;
import com.logitags.cibet.sensor.ejb.CibetRemoteContextFactory;

@RunWith(Arquillian.class)
public class Tutorial4 {

   private static Logger log = Logger.getLogger(Tutorial4.class);

   private static final String BASEURL = "http://localhost:8788/" + Tutorial4.class.getSimpleName();

   @Deployment(testable = false)
   public static WebArchive createDeployment() {
      String warName = Tutorial4.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("tutorial/tut4.webxml");

      archive.addClasses(SimpleRemoteEjb.class, SimpleRemoteEjbImpl.class, TutorialServlet2.class,
            SchedulerInterceptor.class);

      File[] cibet = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-core").withTransitivity()
            .asFile();
      archive.addAsLibraries(cibet);

      archive.addAsWebInfResource("tutorial/persistence-it2.xml", "classes/META-INF/persistence.xml");
      archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
      archive.addAsWebInfResource("tutorial/config.xml", "classes/cibet-config.xml");

      log.debug(archive.toString(true));
      archive.as(ZipExporter.class).exportTo(new File("target/" + warName), true);

      return archive;
   }

   @After
   public void doAfter() throws IOException {
      CloseableHttpClient client = HttpClients.createDefault();
      HttpGet get = new HttpGet(BASEURL + "/clean");
      client.execute(get);
   }

   @Test
   public void schedule1() throws Exception {
      log.info("start schedule1()");
      SimpleRemoteEjb ejb = lookup();
      String result = ejb.writeString("Hello Ejb");
      Assert.assertEquals(null, result);

      log.debug("-------------------- sleep");
      Thread.sleep(12000);
      log.debug("--------------- after TimerTask");
   }

   @Test
   public void schedule2() throws Exception {
      log.info("start schedule2()");
      Context.start();
      Context.requestScope().setScheduledDate(Calendar.SECOND, 3);
      log.debug("execute SchedulerInterceptor: set scheduled date to " + Context.requestScope().getScheduledDate());
      Context.sessionScope().setUser("Pittiplatsch");

      SimpleRemoteEjb ejb = lookupCibetProxy();
      String result = ejb.writeStringNoIntercept("Hello Ejb scheduled by client");
      Assert.assertEquals(null, result);
      Context.end();

      log.debug("-------------------- sleep");
      Thread.sleep(12000);
      log.debug("--------------- after TimerTask");
   }

   private SimpleRemoteEjb lookup() throws Exception {
      Properties properties = new Properties();
      properties.setProperty(javax.naming.Context.INITIAL_CONTEXT_FACTORY,
            "org.jboss.naming.remote.client.InitialContextFactory");
      properties.setProperty(javax.naming.Context.PROVIDER_URL, "remote://localhost:4447");
      properties.setProperty("jboss.naming.client.ejb.context", "true");
      javax.naming.Context ctx = new InitialContext(properties);

      String lookupName = this.getClass().getSimpleName()
            + "/SimpleRemoteEjbImpl!com.logitags.cibet.tutorial.SimpleRemoteEjb";
      SimpleRemoteEjb remoteEjb = (SimpleRemoteEjb) ctx.lookup(lookupName);
      return remoteEjb;
   }

   private SimpleRemoteEjb lookupCibetProxy() throws Exception {
      Properties properties = new Properties();
      properties.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, CibetRemoteContextFactory.class.getName());
      properties.put(CibetRemoteContext.NATIVE_INITIAL_CONTEXT_FACTORY,
            "org.jboss.naming.remote.client.InitialContextFactory");
      properties.setProperty(javax.naming.Context.PROVIDER_URL, "remote://localhost:4447");
      properties.setProperty("jboss.naming.client.ejb.context", "true");
      javax.naming.Context ctx = new InitialContext(properties);

      String lookupName = this.getClass().getSimpleName()
            + "/SimpleRemoteEjbImpl!com.logitags.cibet.tutorial.SimpleRemoteEjb";
      SimpleRemoteEjb remoteEjb = (SimpleRemoteEjb) ctx.lookup(lookupName);
      return remoteEjb;
   }

}
