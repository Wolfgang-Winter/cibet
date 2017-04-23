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
import org.jboss.shrinkwrap.resolver.api.maven.ScopeType;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenDependencies;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class Tutorial3 {

   private static Logger log = Logger.getLogger(Tutorial3.class);

   private static final String BASEURL = "http://localhost:8788/" + Tutorial3.class.getSimpleName();

   @Deployment(testable = false)
   public static WebArchive createDeployment() {
      String warName = Tutorial3.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("tutorial/tut3.webxml");

      archive.addClasses(Person.class, Address.class, TutorialServlet3.class);

      File[] cibet = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-jpa").withoutTransitivity()
            .asFile();
      archive.addAsLibraries(cibet);

      File[] shiro = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-shiro").withTransitivity()
            .asFile();
      archive.addAsLibraries(shiro);

      File[] shiro1 = Maven.resolver()
            .addDependencies(MavenDependencies.createDependency("org.apache.shiro:shiro-web:1.2.2", ScopeType.COMPILE,
                  false, MavenDependencies.createExclusion("org.slf4j:slf4j-api")))
            .resolve().withTransitivity().asFile();
      archive.addAsLibraries(shiro1);

      archive.addAsWebInfResource("tutorial/persistence-it1.xml", "classes/META-INF/persistence.xml");
      archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
      archive.addAsWebInfResource("shiro.ini", "classes/shiro.ini");
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
   public void twoManRule1() throws Exception {
      log.info("start twoManRule1()");

      CloseableHttpClient client = HttpClients.createDefault();

      // For dual control a user must be authenticated
      HttpGet get = new HttpGet(BASEURL + "/login?user=lonestarr&password=vespa");
      HttpResponse response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      String info = readResponseBody(response);
      Assert.assertEquals("Shiro logged in user lonestarr", info);

      // persist a Person
      get = new HttpGet(BASEURL + "/persist");
      response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      String id = readResponseBody(response);
      log.info("Person persisted with id " + id);

      // change state is not postponed
      get = new HttpGet(BASEURL + "/updateState?personId=" + URLEncoder.encode(id, "UTF-8") + "&state=BAD");
      response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      info = readResponseBody(response);
      Assert.assertEquals("Person updated state = BAD [EventResult: EXECUTED]", info);

      // change name is postponed
      get = new HttpGet(BASEURL + "/updateName?personId=" + URLEncoder.encode(id, "UTF-8") + "&name=Falstaff");
      response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      info = readResponseBody(response);
      Assert.assertEquals("Person updated name = Walter [EventResult: POSTPONED]", info);

      // a second user must login to release, user lonestarr remains logged in
      get = new HttpGet(BASEURL + "/loginSecondUser?user=darkhelmet&password=ludicrousspeed");
      response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      info = readResponseBody(response);
      Assert.assertEquals("Shiro logged in second user darkhelmet", info);

      // now release
      get = new HttpGet(BASEURL + "/release");
      response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      info = readResponseBody(response);
      Assert.assertEquals("Falstaff", info);
   }

}
