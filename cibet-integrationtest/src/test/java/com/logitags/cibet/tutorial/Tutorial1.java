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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
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

@RunWith(Arquillian.class)
public class Tutorial1 {

   private static Logger log = Logger.getLogger(Tutorial1.class);

   private static final String BASEURL = "http://localhost:8788/" + Tutorial1.class.getSimpleName();

   @Deployment(testable = false)
   public static WebArchive createDeployment() {
      String warName = Tutorial1.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("tutorial/tut1.webxml");

      archive.addClasses(Person.class, Address.class, TutorialServlet1.class);

      File[] cibet = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-jpa").withoutTransitivity()
            .asFile();
      archive.addAsLibraries(cibet);

      File[] spring = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-springsecurity")
            .withTransitivity().asFile();
      archive.addAsLibraries(spring);

      archive.addAsWebInfResource("tutorial/persistence-it1.xml", "classes/META-INF/persistence.xml");
      archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
      archive.addAsWebInfResource("tutorial/config-spring.xml", "classes/cibet-config.xml");
      archive.addAsWebInfResource("spring-context_1.xml", "classes/spring-context.xml");

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

   protected String readResponseBody(HttpResponse response) throws Exception {
      // Read the response body.
      HttpEntity entity = response.getEntity();
      InputStream instream = null;
      try {
         if (entity != null) {
            instream = entity.getContent();

            BufferedReader reader = new BufferedReader(new InputStreamReader(instream, "UTF-8"));
            String body = reader.readLine();
            log.info("body=" + body);
            return body;
         } else {
            return null;
         }
      } catch (IOException ex) {
         // In case of an IOException the connection will be released
         // back to the connection manager automatically
         throw ex;

      } catch (RuntimeException ex) {
         // In case of an unexpected exception you may want to abort
         // the HTTP request in order to shut down the underlying
         // connection and release it back to the connection manager.
         throw ex;

      } finally {
         // Closing the input stream will trigger connection release
         if (instream != null)
            instream.close();
         Thread.sleep(100);
      }
   }

   @Test
   public void archive1() throws Exception {
      log.info("start archive1()");

      CloseableHttpClient client = HttpClients.createDefault();
      HttpGet get = new HttpGet(BASEURL + "/persist");
      HttpResponse response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      String id = readResponseBody(response);
      log.info("Person persisted with id " + id);

      // load the Archive
      get = new HttpGet(BASEURL + "/loadPersonArchive?id=" + URLEncoder.encode(id, "UTF-8") + "&expected=1");
      response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);
   }

   @Test
   public void spring1() throws Exception {
      log.info("start spring1()");

      // login
      CloseableHttpClient client = HttpClients.createDefault();
      HttpGet get = new HttpGet(BASEURL + "/loginSpring?USER=Abel&ROLE=persister&TENANT=SpringTest");
      HttpResponse response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);

      // persist
      get = new HttpGet(BASEURL + "/persist");
      response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      String id = readResponseBody(response);
      Assert.assertNotNull(id);

      // check that Person is not persisted
      get = new HttpGet(BASEURL + "/loadPerson?expected=0");
      response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      String answer = readResponseBody(response);
      Assert.assertEquals("no Person found", answer);

      // login release user
      get = new HttpGet(BASEURL + "/loginSpring?USER=Kain&ROLE=releaser&TENANT=SpringTest");
      response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);

      // release
      get = new HttpGet(BASEURL + "/release");
      response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);

      // check that Person is persisted now
      get = new HttpGet(BASEURL + "/loadPerson?expected=1");
      response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      answer = readResponseBody(response);
      Assert.assertNotEquals("no Person found", answer);

      // log off
      get = new HttpGet(BASEURL + "/logoffSpring");
      response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
   }

   @Test
   public void locker1() throws Exception {
      log.info("start locker1()");

      // start batch
      CloseableHttpClient client = HttpClients.createDefault();
      HttpGet get = new HttpGet(BASEURL + "/batch");
      HttpResponse response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      String answer = readResponseBody(response);
      String[] ids = answer.split(":");

      // now log in as another user
      get = new HttpGet(BASEURL + "/loginSpring?USER=Kain&ROLE=releaser&TENANT=LockTest");
      response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);

      // try to load a Person
      get = new HttpGet(BASEURL + "/findPerson");
      response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      answer = readResponseBody(response);
      Assert.assertEquals("denied", answer);

      // try to execute persist method
      get = new HttpGet(BASEURL + "/persist");
      response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      answer = readResponseBody(response);
      Assert.assertEquals("invoke of method persist denied", answer);

      // now unlock
      get = new HttpGet(BASEURL + "/unlock?LOCK1=" + ids[0] + "&LOCK2=" + ids[1]);
      response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      answer = readResponseBody(response);
      Assert.assertEquals("unlocked completed", answer);

      // try again to load a Person
      get = new HttpGet(BASEURL + "/findPerson");
      response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      answer = readResponseBody(response);
      Assert.assertNotEquals("denied", answer);
   }

}
