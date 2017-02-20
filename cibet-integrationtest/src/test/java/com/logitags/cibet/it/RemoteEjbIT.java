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
package com.logitags.cibet.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.ldap.LdapName;

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
import com.logitags.cibet.config.ConfigurationService;
import com.logitags.cibet.context.InitializationService;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.common.Invoker;
import com.logitags.cibet.sensor.ejb.CibetRemoteContext;
import com.logitags.cibet.sensor.ejb.CibetRemoteContextFactory;
import com.logitags.cibet.sensor.ejb.RemoteEJBInvoker;
import com.logitags.cibet.sensor.ejb.RemoteEjbInvocationHandler;

@RunWith(Arquillian.class)
public class RemoteEjbIT extends AbstractArquillian {

   private static Logger log = Logger.getLogger(RemoteEjbIT.class);

   @Deployment(testable = false)
   public static WebArchive createDeployment() {
      String warName = RemoteEjbIT.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("it/web2.xml");

      archive.addClasses(AbstractArquillian.class, CoreTestBase.class, AbstractTEntity.class, TEntity.class,
            TComplexEntity.class, TComplexEntity2.class, ITComplexEntity.class, TCompareEntity.class, RemoteEJB.class,
            RemoteEJBImpl.class, SimpleEjb.class);

      File[] cibet = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-jpa").withTransitivity()
            .asFile();
      archive.addAsLibraries(cibet);

      archive.addAsWebInfResource("META-INF/persistence-it-derby.xml", "classes/META-INF/persistence.xml");
      archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

      log.debug(archive.toString(true));
      archive.as(ZipExporter.class).exportTo(new File("target/" + warName), true);

      return archive;
   }

   @Before
   public void beforeRemoteEjbIT() {
      log.debug("execute before()");
      new ConfigurationService().initialise();
      InitializationService.instance().startContext();
      com.logitags.cibet.context.Context.sessionScope().setUser(USER);
      com.logitags.cibet.context.Context.sessionScope().setTenant(TENANT);
      log.debug("end execute before()");
   }

   @After
   public void afterAbstractArquillian() throws Exception {
      InitializationService.instance().endContext();
   }

   @Test
   public void executeLocalRemoteEJBInvoker() throws Exception {
      log.debug("start executeLocalRemoteEJBInvoker()");
      List<ResourceParameter> params = new ArrayList<>();

      URL url = Thread.currentThread().getContextClassLoader().getResource("jndi_.properties");
      log.debug("url=" + url);
      Properties properties = new Properties();
      properties.load(url.openStream());

      ResourceParameter rp = new ResourceParameter(RemoteEjbInvocationHandler.JNDI_NAME, String.class.getName(),
            "RemoteEjbIT/RemoteEJBImpl!com.cibethelper.ejb.RemoteEJB", ParameterType.INTERNAL_PARAMETER, 1);
      params.add(rp);
      ResourceParameter rp1 = new ResourceParameter("param1", String.class.getName(), "myParam1",
            ParameterType.METHOD_PARAMETER, 2);
      params.add(rp1);
      ResourceParameter rp2 = new ResourceParameter("param2", int.class.getName(), 15, ParameterType.METHOD_PARAMETER,
            3);
      params.add(rp2);
      ResourceParameter rp3 = new ResourceParameter("param3", int.class.getName(), 34, ParameterType.METHOD_PARAMETER,
            4);
      params.add(rp3);
      ResourceParameter rp4 = new ResourceParameter("param4", byte[].class.getName(), "wossel".getBytes(),
            ParameterType.METHOD_PARAMETER, 5);
      params.add(rp4);
      TEntity entity = new TEntity("ja", 88, "mys");
      ResourceParameter rp5 = new ResourceParameter("param5", TEntity.class.getName(), entity,
            ParameterType.METHOD_PARAMETER, 6);
      params.add(rp5);
      ResourceParameter rp6 = new ResourceParameter("param6", Long.class.getName(), 900L,
            ParameterType.METHOD_PARAMETER, 7);
      params.add(rp6);
      ResourceParameter rp7 = new ResourceParameter(RemoteEjbInvocationHandler.JNDI_CONTEXT, Properties.class.getName(),
            properties, ParameterType.INTERNAL_PARAMETER, 8);
      params.add(rp7);

      Invoker invoker = RemoteEJBInvoker.createInstance();
      List<Object> result = (List<Object>) invoker.execute(null, null, "testInvoke", params);
      assertNotNull(result);
      assertEquals(6, result.size());
   }

   @Test
   public void executeRemoteEJBInvokerNoContext() throws Exception {
      log.debug("start executeRemoteEJBInvokerNoContext()");
      List<ResourceParameter> params = new ArrayList<>();
      Invoker invoker = RemoteEJBInvoker.createInstance();
      try {
         List<Object> result = (List<Object>) invoker.execute(null, null, "testInvoke", params);
         fail();
      } catch (Exception e) {
         assertEquals("Failed to find resource parameter " + RemoteEjbInvocationHandler.JNDI_CONTEXT, e.getMessage());
      }
   }

   @Test
   public void executeRemoteEJBInvokerNoJndiname() throws Exception {
      log.debug("start executeRemoteEJBInvokerNoJndiname()");
      List<ResourceParameter> params = new ArrayList<>();
      ResourceParameter rp7 = new ResourceParameter(RemoteEjbInvocationHandler.JNDI_CONTEXT, Properties.class.getName(),
            new Properties(), ParameterType.INTERNAL_PARAMETER, 8);
      params.add(rp7);

      Invoker invoker = RemoteEJBInvoker.createInstance();
      try {
         List<Object> result = (List<Object>) invoker.execute(null, null, "testInvoke", params);
         fail();
      } catch (Exception e) {
         assertEquals("Failed to find resource parameter " + RemoteEjbInvocationHandler.JNDI_NAME, e.getMessage());
      }
   }

   @Test
   public void invokeRemoteEJBHandler() throws Throwable {
      log.debug("start invokeRemoteEJBHandler()");
      TEntity entity = new TEntity("ja", 88, "mys");
      Object[] params = new Object[] { "kll", 15, 34, "wossel".getBytes(), entity, new Long(900L) };

      URL url = Thread.currentThread().getContextClassLoader().getResource("jndi_.properties");
      log.debug("url=" + url);
      Properties properties = new Properties();
      properties.load(url.openStream());

      Method m = RemoteEJBImpl.class.getMethod("testInvoke", String.class, int.class, int.class, byte[].class,
            TEntity.class, Long.class);
      Constructor<RemoteEjbInvocationHandler> constr = RemoteEjbInvocationHandler.class
            .getDeclaredConstructor(Object.class, Hashtable.class, String.class);
      constr.setAccessible(true);

      RemoteEjbInvocationHandler handler = constr.newInstance(new RemoteEJBImpl(), properties, "x");
      List<Object> result = (List<Object>) handler.invoke(new Object(), m, params);
      assertNotNull(result);
      assertEquals(6, result.size());
   }

   @Test
   public void invokeRemoteEJBHandlerName() throws Throwable {
      log.debug("start invokeRemoteEJBHandlerName()");
      TEntity entity = new TEntity("ja", 88, "mys");
      Object[] params = new Object[] { "kll", 15, 34, "wossel".getBytes(), entity, new Long(900L) };

      URL url = Thread.currentThread().getContextClassLoader().getResource("jndi_.properties");
      log.debug("url=" + url);
      Properties properties = new Properties();
      properties.load(url.openStream());

      Method m = RemoteEJBImpl.class.getMethod("testInvoke", String.class, int.class, int.class, byte[].class,
            TEntity.class, Long.class);
      Name name = new LdapName("CN=Steve Kille,O=Isode Limited,C=GB");

      Constructor<RemoteEjbInvocationHandler> constr = RemoteEjbInvocationHandler.class
            .getDeclaredConstructor(Object.class, Hashtable.class, Name.class);
      constr.setAccessible(true);
      RemoteEjbInvocationHandler handler = constr.newInstance(new RemoteEJBImpl(), properties, name);

      List<Object> result = (List<Object>) handler.invoke(new Object(), m, params);
      assertNotNull(result);
      assertEquals(6, result.size());
   }

   @Test
   public void invokeRemoteEJBHandlerNoName() throws Throwable {
      log.debug("start invokeRemoteEJBHandlerNoName()");
      TEntity entity = new TEntity("ja", 88, "mys");
      Object[] params = new Object[] { "kll", 15, 34, "wossel".getBytes(), entity, new Long(900L) };

      URL url = Thread.currentThread().getContextClassLoader().getResource("jndi_.properties");
      log.debug("url=" + url);
      Properties properties = new Properties();
      properties.load(url.openStream());

      Method m = RemoteEJBImpl.class.getMethod("testInvoke", String.class, int.class, int.class, byte[].class,
            TEntity.class, Long.class);

      Constructor<RemoteEjbInvocationHandler> constr = RemoteEjbInvocationHandler.class
            .getDeclaredConstructor(Object.class, Hashtable.class, String.class);
      constr.setAccessible(true);
      RemoteEjbInvocationHandler handler = constr.newInstance(new RemoteEJBImpl(), properties, (String) null);

      try {
         List<Object> result = (List<Object>) handler.invoke(new Object(), m, params);
         fail();
      } catch (IllegalArgumentException e) {
      }
   }

   @Test
   public void getInitialContextNoNative() throws Throwable {
      log.debug("start getInitialContextNoNative()");
      URL url = Thread.currentThread().getContextClassLoader().getResource("jndi_.properties");
      Properties properties = new Properties();
      properties.load(url.openStream());

      CibetRemoteContextFactory fac = new CibetRemoteContextFactory();
      try {
         fac.getInitialContext(properties);
         fail();
      } catch (NamingException e) {
         assertEquals("Failed to find property " + CibetRemoteContext.NATIVE_INITIAL_CONTEXT_FACTORY
               + " in the environment properties of InitialContext", e.getMessage());
      }
   }

   @Test
   public void getInitialContext() throws Throwable {
      log.debug("start getInitialContext()");
      URL url = Thread.currentThread().getContextClassLoader().getResource("jndi_.properties");
      Properties properties = new Properties();
      properties.load(url.openStream());
      String cl = (String) properties.get(Context.INITIAL_CONTEXT_FACTORY);
      properties.put(CibetRemoteContext.NATIVE_INITIAL_CONTEXT_FACTORY, cl);

      CibetRemoteContextFactory fac = new CibetRemoteContextFactory();
      CibetRemoteContext ctx = (CibetRemoteContext) fac.getInitialContext(properties);
      assertNotNull(ctx);
   }

   @Test(expected = IllegalArgumentException.class)
   public void getInitialContextNull() throws Throwable {
      log.debug("start getInitialContextNull()");
      CibetRemoteContextFactory fac = new CibetRemoteContextFactory();
      fac.getInitialContext(null);
   }

   @Test
   public void lookup() throws Throwable {
      log.debug("start lookup()");
      URL url = Thread.currentThread().getContextClassLoader().getResource("jndi_.properties");
      Properties properties = new Properties();
      properties.load(url.openStream());
      String cl = (String) properties.get(Context.INITIAL_CONTEXT_FACTORY);
      log.debug("INITIAL_CONTEXT_FACTORY: " + cl);
      properties.put(CibetRemoteContext.NATIVE_INITIAL_CONTEXT_FACTORY, cl);

      CibetRemoteContextFactory fac = new CibetRemoteContextFactory();
      CibetRemoteContext ctx = (CibetRemoteContext) fac.getInitialContext(properties);
      assertNotNull(ctx);
      // RemoteEJB ejb = (RemoteEJB) ctx.lookup("jav:module/RemoteEJBImpl");
      RemoteEJB ejb = (RemoteEJB) ctx.lookup("RemoteEjbIT/RemoteEJBImpl!com.cibethelper.ejb.RemoteEJB");
      assertNotNull(ejb);
   }

}
