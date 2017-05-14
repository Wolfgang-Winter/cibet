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
import java.net.URL;

import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.logitags.jmeter.FallbackResponseCallback;
import com.logitags.jmeter.JMEntity;
import com.logitags.jmeter.JMeterTestServlet;
import com.logitags.jmeter.LoggingCallback;
import com.logitags.jmeter.MonitorEjb;
import com.logitags.jmeter.MonitorTestClass;

/**
 * This tutorial requires a jmeter 2.13 installation. Adjust the path in constant JMETER_HOME
 * 
 * @author Wolfgang
 *
 */
@RunWith(Arquillian.class)
public class Tutorial6 {

   private static Logger log = Logger.getLogger(Tutorial6.class);

   private static final String JMETER_HOME = "D:/Java/apache-jmeter-2.13";

   @Deployment(testable = false)
   public static WebArchive createDeployment() {
      String warName = Tutorial6.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("tutorial/jmeter/web.xml");

      archive.addClasses(JMEntity.class, JMeterTestServlet.class, MonitorEjb.class, MonitorTestClass.class);

      File[] cibet = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-jpa").withTransitivity()
            .asFile();
      archive.addAsLibraries(cibet);

      JavaArchive jarArchive = ShrinkWrap.create(JavaArchive.class, "fallbacks.jar");
      jarArchive.addClasses(FallbackResponseCallback.class, LoggingCallback.class);
      archive.addAsLibraries(jarArchive);

      archive.addAsWebResource("tutorial/jmeter/index.html", "index.html");
      archive.addAsWebInfResource("tutorial/jmeter/persistence.xml", "classes/META-INF/persistence.xml");
      archive.addAsWebInfResource("tutorial/jmeter/aop.xml", "classes/META-INF/aop.xml");
      archive.addAsWebInfResource("tutorial/jmeter/tutorial6-cibet-config.xml", "classes/cibet-config.xml");
      archive.addAsWebInfResource("tutorial/jmeter/ejb-jar.xml", "ejb-jar.xml");

      log.debug(archive.toString(true));
      archive.as(ZipExporter.class).exportTo(new File("target/" + warName), true);

      return archive;
   }

   @Test
   public void loadControl1() throws Exception {
      log.info("start loadControl1()");

      // Initialize Properties, logging, locale, etc.
      JMeterUtils.loadJMeterProperties(JMETER_HOME + "/bin/jmeter.properties");
      JMeterUtils.setJMeterHome(JMETER_HOME);
      JMeterUtils.initLocale();

      // Initialize JMeter SaveService
      SaveService.loadProperties();

      // Load existing .jmx Test Plan
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      URL url = classLoader.getResource("tutorial/jmeter/Tutorial6.jmx");
      File file = new File(url.toURI());
      HashTree testPlanTree = SaveService.loadTree(file);

      // Run JMeter Test
      StandardJMeterEngine jmeter = new StandardJMeterEngine();
      jmeter.configure(testPlanTree);
      jmeter.run();
   }

}
