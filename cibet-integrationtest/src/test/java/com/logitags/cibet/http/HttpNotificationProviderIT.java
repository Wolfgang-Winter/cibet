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
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
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
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.cibethelper.entities.AbstractTEntity;
import com.cibethelper.entities.ITComplexEntity;
import com.cibethelper.entities.TCompareEntity;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TComplexEntity2;
import com.cibethelper.entities.TEntity;
import com.cibethelper.servlet.ShiroServlet;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.notification.HttpNotificationProvider;
import com.logitags.cibet.sensor.jpa.JpaResource;

@RunWith(Arquillian.class)
public class HttpNotificationProviderIT {

   private static Logger log = Logger.getLogger(HttpNotificationProviderIT.class);

   private static Calendar NOW = Calendar.getInstance();
   private static Calendar NOW_3 = Calendar.getInstance();
   private static Calendar NOW_5 = Calendar.getInstance();

   @Deployment(testable = false)
   public static WebArchive createDeployment() {
      String warName = HttpNotificationProviderIT.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("it/web-shiro.xml");

      archive.addClasses(AbstractTEntity.class, TEntity.class, TComplexEntity.class, TComplexEntity2.class,
            ITComplexEntity.class, TCompareEntity.class, ShiroServlet.class);

      File[] cibet = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-shiro").withTransitivity()
            .asFile();
      archive.addAsLibraries(cibet);

      File[] shiro1 = Maven.resolver()
            .addDependencies(MavenDependencies.createDependency("org.apache.shiro:shiro-web:1.2.2", ScopeType.COMPILE,
                  false, MavenDependencies.createExclusion("org.slf4j:slf4j-api")))
            .resolve().withTransitivity().asFile();
      archive.addAsLibraries(shiro1);

      archive.addAsWebInfResource("shiro.ini", "classes/shiro.ini");
      archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

      log.debug(archive.toString(true));
      archive.as(ZipExporter.class).exportTo(new File("target/" + warName), true);

      return archive;
   }

   @BeforeClass
   public static void setup() {
      NOW.set(Calendar.YEAR, 2013);
      NOW.set(Calendar.MONTH, 8);
      NOW.set(Calendar.DATE, 25);
      NOW.set(Calendar.HOUR_OF_DAY, 16);
      NOW.set(Calendar.MINUTE, 13);
      NOW.set(Calendar.SECOND, 22);
      NOW.set(Calendar.MILLISECOND, 0);

      NOW_3.setTime(NOW.getTime());
      NOW_3.add(Calendar.DATE, -3);

      NOW_5.setTime(NOW.getTime());
      NOW_5.add(Calendar.DATE, -5);
   }

   protected String getBaseURL() throws IOException {
      URL url = Thread.currentThread().getContextClassLoader().getResource("jndi_.properties");
      Properties properties = new Properties();
      properties.load(url.openStream());
      String httpUrl = properties.getProperty("http.url");
      return httpUrl + this.getClass().getSimpleName();
   }

   protected DcControllable createDcControllable(ExecutionStatus status) throws IOException {
      DcControllable c = new DcControllable();
      c.setActuator("FOUR_EYES");
      c.setApprovalAddress(getBaseURL() + "/notif?nottype=approv");
      c.setApprovalDate(NOW.getTime());
      c.setExecutionStatus(status);
      c.setApprovalUser("approvalUser");
      c.setCaseId("test-caseid");
      c.setControlEvent(ControlEvent.DELETE);
      c.setCreateAddress(getBaseURL() + "/notif?nottype=create");
      c.setCreateDate(NOW_5.getTime());
      c.setCreateUser("userId");
      c.setDcControllableId("123");
      c.setFirstApprovalAddress(getBaseURL() + "/notif?nottype=firstApp");
      c.setFirstApprovalDate(NOW_3.getTime());
      c.setFirstApprovalUser("firstApprovalUserId");

      JpaResource res = new JpaResource(new TEntity());
      c.setResource(res);
      c.setTenant("tenant");

      return c;
   }

   @Test
   public void notifyAssign() throws Exception {
      log.debug("start notifyAssign()");
      File file = null;
      try {
         DcControllable c = createDcControllable(ExecutionStatus.POSTPONED);
         HttpNotificationProvider prov = new HttpNotificationProvider();

         prov.notify(ExecutionStatus.POSTPONED, c);
         Thread.sleep(200);

         // JBoss: C:\Users\Wolfgang\AppData\Local\Temp\
         // Tomee: %CATALINA_BASE%\temp
         log.debug("java.io.tmpdir: " + System.getProperty("java.io.tmpdir"));

         file = new File(System.getProperty("java.io.tmpdir") + "httpNotification.tmp");
         Assert.assertTrue(file.exists());
         FileReader in = new FileReader(file);
         String result = IOUtils.toString(in);
         in.close();
         file.delete();
         log.debug(result);

         Set<String> list = new TreeSet<String>();
         StringTokenizer tok = new StringTokenizer(result, ";");
         while (tok.hasMoreTokens()) {
            list.add(tok.nextToken());
         }

         Assert.assertEquals(22, list.size());
         Iterator<String> iter = list.iterator();
         Assert.assertEquals("actuator=FOUR_EYES", iter.next());
         Assert.assertEquals("approvalAddress=" + getBaseURL() + "/notif?nottype=approv", iter.next());
         Assert.assertEquals("approvalDate=2013-09-25 16:13:22.000", iter.next());
         Assert.assertEquals("approvalRemark=", iter.next());
         Assert.assertEquals("approvalUser=approvalUser", iter.next());
         Assert.assertEquals("caseId=test-caseid", iter.next());
         Assert.assertEquals("controlEvent=DELETE", iter.next());
         Assert.assertEquals("createAddress=" + getBaseURL() + "/notif?nottype=create", iter.next());
         Assert.assertEquals("createDate=2013-09-20 16:13:22.000", iter.next());
         Assert.assertEquals("createRemark=", iter.next());
         Assert.assertEquals("createUser=userId", iter.next());
         Assert.assertEquals("dcControllableId=123", iter.next());
         Assert.assertEquals("firstApprovalAddress=" + getBaseURL() + "/notif?nottype=firstApp", iter.next());
         Assert.assertEquals("firstApprovalDate=2013-09-22 16:13:22.000", iter.next());
         Assert.assertEquals("firstApprovalRemark=", iter.next());
         Assert.assertEquals("firstApprovalUser=firstApprovalUserId", iter.next());
         Assert.assertEquals("notificationType=POSTPONED", iter.next());
         Assert.assertEquals("nottype=approv", iter.next());
         Assert.assertEquals("primaryKeyId=0", iter.next());
         String s = iter.next();
         log.debug("sss:" + s);
         Assert.assertEquals("target=TEntity id: 0, counter: 0, owner: null, xCaltimestamp: null", s);
         Assert.assertEquals("targetType=com.cibethelper.entities.TEntity", iter.next());
         Assert.assertEquals("tenant=tenant", iter.next());
      } finally {
         if (file != null && file.exists()) {
            file.delete();
         }
      }
   }

}
