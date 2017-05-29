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
public class Tutorial2 {

   private static Logger log = Logger.getLogger(Tutorial2.class);

   private static final String BASEURL = "http://localhost:8788/Tutorial2";

   @Deployment(testable = false)
   public static WebArchive createDeployment() {
      String warName = Tutorial2.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("tutorial/tut2.webxml");

      archive.addClasses(TutorialServlet2.class);

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

   protected String readResponseBody(HttpResponse response) throws Exception {
      // Read the response body.
      HttpEntity entity = response.getEntity();
      InputStream instream = null;
      try {
         if (entity != null) {
            instream = entity.getContent();

            BufferedReader reader = new BufferedReader(new InputStreamReader(instream, "UTF-8"));
            String body = reader.readLine();
            log.info("response body=" + body);
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
   public void archive2() throws Exception {
      log.info("start archive2()");

      CloseableHttpClient client = HttpClients.createDefault();
      HttpGet get = new HttpGet(BASEURL + "/call?param=sniff");
      HttpResponse response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      String info = readResponseBody(response);

      // load the Archive
      get = new HttpGet(BASEURL + "/loadArchive?expected=1");
      response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);
   }

   @Test
   public void fourEyes1() throws Exception {
      log.info("start fourEyes1()");

      CloseableHttpClient client = HttpClients.createDefault();

      // For dual control a user must be authenticated
      HttpGet get = new HttpGet(BASEURL + "/simpleLogin?user=Nobby");
      HttpResponse response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);

      // call Java method changeConfig in Servlet. It will not be executed and result is null
      get = new HttpGet(BASEURL + "/changeConfig?param=doItLater");
      response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      String info = readResponseBody(response);
      Assert.assertEquals("/Tutorial2/changeConfig request executed with response info null", info);

      // load and check Controllable
      get = new HttpGet(BASEURL + "/loadDc?expected=1");
      response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);

      // another user must release the method call
      get = new HttpGet(BASEURL + "/simpleLogin?user=ReleaseUser");
      response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);

      // now release
      get = new HttpGet(BASEURL + "/release");
      response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      info = readResponseBody(response);
      Assert.assertEquals(
            "method " + TutorialServlet2.class.getSimpleName() + ".changeConfig() called with parameter doItLater",
            info);
   }

   @Test
   public void fourEyes2() throws Exception {
      log.info("start fourEyes2()");

      CloseableHttpClient client = HttpClients.createDefault();

      // For dual control a user must be authenticated
      HttpGet get = new HttpGet(BASEURL + "/simpleLogin?user=Rudi");
      HttpResponse response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);

      // call URL. It will not be executed and http result code is 202 (SC_ACCEPTED)
      get = new HttpGet(BASEURL + "/secured?param=requestLater");
      response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_ACCEPTED, response.getStatusLine().getStatusCode());

      // load and check Controllable
      get = new HttpGet(BASEURL + "/loadDc?expected=1");
      response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);

      // another user must release the method call
      get = new HttpGet(BASEURL + "/simpleLogin?user=AnotherUser");
      response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);

      // now release
      get = new HttpGet(BASEURL + "/releaseHttp");
      response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      String info = readResponseBody(response);
      Assert.assertEquals("/Tutorial2/secured request executed with response info method "
            + TutorialServlet2.class.getSimpleName() + ".secured() called with parameter requestLater", info);
   }

}
