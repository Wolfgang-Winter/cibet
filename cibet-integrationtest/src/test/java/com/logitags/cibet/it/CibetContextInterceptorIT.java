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
package com.logitags.cibet.it;

import java.io.File;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.PropertiesBasedEJBClientConfiguration;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.jboss.ejb.client.remoting.ConfigBasedEJBClientContextSelector;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.cibethelper.base.CoreTestBase;
import com.cibethelper.base.NoControlActuator;
import com.cibethelper.base.SubArchiveController;
import com.cibethelper.ejb.Ejb2Service;
import com.cibethelper.ejb.EjbService;
import com.cibethelper.ejb.JBossEjbClientInterceptor;
import com.cibethelper.ejb.OutService;
import com.cibethelper.ejb.RemoteEJB;
import com.cibethelper.ejb.RemoteEJBImpl;
import com.cibethelper.ejb.SecuredRemoteEJBImpl;
import com.cibethelper.ejb.SimpleEjb;
import com.cibethelper.entities.AbstractTEntity;
import com.cibethelper.entities.ITComplexEntity;
import com.cibethelper.entities.TCompareEntity;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TComplexEntity2;
import com.cibethelper.entities.TEntity;
import com.cibethelper.servlet.GeneralServlet;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.archive.ArchiveLoader;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.actuator.dc.FourEyesActuator;
import com.logitags.cibet.authentication.AbstractAuthenticationProvider;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.sensor.ejb.CibetRemoteContext;
import com.logitags.cibet.sensor.ejb.CibetRemoteContextFactory;
import com.logitags.cibet.sensor.ejb.RemoteEJBInvoker;

@RunWith(Arquillian.class)
public class CibetContextInterceptorIT extends AbstractArquillian {

   private static Logger log = Logger.getLogger(CibetContextInterceptorIT.class);

   @Deployment(testable = false)
   public static WebArchive createDeployment() {
      String warName = CibetContextInterceptorIT.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("it/web-general.xml");

      archive.addClasses(AbstractArquillian.class, CoreTestBase.class, TEntity.class, TComplexEntity.class,
            ITComplexEntity.class, AbstractTEntity.class, TCompareEntity.class, GeneralServlet.class, OutService.class,
            EjbService.class, Ejb2Service.class, SubArchiveController.class, NoControlActuator.class,
            TComplexEntity2.class, RemoteEJB.class, RemoteEJBImpl.class, SecuredRemoteEJBImpl.class, SimpleEjb.class);

      File[] cibet = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-jpa").withoutTransitivity()
            .asFile();
      archive.addAsLibraries(cibet);
      File[] spring = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-springsecurity")
            .withTransitivity().asFile();
      archive.addAsLibraries(spring);

      archive.addAsWebInfResource("META-INF/persistence-it.xml", "classes/META-INF/persistence.xml");
      archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
      archive.addAsWebInfResource("spring-context_2.xml", "classes/spring-context.xml");
      archive.addAsWebInfResource("config_3.xml", "classes/cibet-config.xml");
      archive.addAsWebInfResource("it/jboss-ejb3.xml", "jboss-ejb3.xml");

      log.debug(archive.toString(true));
      archive.as(ZipExporter.class).exportTo(new File("target/" + warName), true);

      return archive;
   }

   @Before
   public void beforeCibetContextInterceptorIT() {
      Context.start();
   }

   @Override
   @After
   public void afterAbstractArquillian() throws Exception {
      log.info("execute sub-afterAbstractArquillian(");
      HttpGet method = new HttpGet(getBaseURL() + "/logout");
      client.execute(method);
      method.abort();

      method = new HttpGet(getBaseURL() + "/clean.cibet");
      client.execute(method);
      method.abort();

      Context.end();
   }

   private InitialContext getInitialContext() throws Exception {
      URL url = Thread.currentThread().getContextClassLoader().getResource("jndi_.properties");
      Properties properties = new Properties();
      properties.load(url.openStream());
      properties.put(javax.naming.Context.SECURITY_PRINCIPAL, "Mutzi1");
      properties.put(javax.naming.Context.SECURITY_CREDENTIALS, "passss1234!");
      InitialContext ctx = new InitialContext(properties);
      return ctx;
   }

   private InitialContext getProxyInitialContext() throws Exception {
      URL url = Thread.currentThread().getContextClassLoader().getResource("jndi_.properties");
      Properties properties = new Properties();
      properties.load(url.openStream());

      Object nativeFac = properties.get(javax.naming.Context.INITIAL_CONTEXT_FACTORY);
      log.debug("set native InitialContextFactory " + nativeFac);
      properties.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, CibetRemoteContextFactory.class.getName());
      properties.put(CibetRemoteContext.NATIVE_INITIAL_CONTEXT_FACTORY, nativeFac);
      properties.put(javax.naming.Context.SECURITY_PRINCIPAL, "Mutzi1");
      properties.put(javax.naming.Context.SECURITY_CREDENTIALS, "passss1234!");

      InitialContext ctx = new InitialContext(properties);
      return ctx;
   }

   @Test
   @InSequence(1)
   public void testInvokeRemote() throws Exception {
      log.debug("start testInvokeRemote()");
      TEntity te = new TEntity("myName", 45, "winter");

      String userName = "ANONYMOUS";
      String lookupName = CibetContextInterceptorIT.class.getSimpleName()
            + "/RemoteEJBImpl!com.cibethelper.ejb.RemoteEJB";
      if (APPSERVER.equals(TOMEE)) {
         lookupName = "global/" + lookupName;
         userName = "Mutzi1";
      }
      RemoteEJB remoteEjb = (RemoteEJB) getInitialContext().lookup(lookupName);
      TEntity te2 = remoteEjb.persist(te);
      log.debug(te2);
      Assert.assertTrue(te2.getId() != 0);

      List<Archive> list = ArchiveLoader.loadArchives(TEntity.class.getName());
      Assert.assertEquals(1, list.size());

      Assert.assertEquals(userName, list.get(0).getCreateUser());
      Context.end();
   }

   @Test
   @InSequence(2)
   public void testInvokeSecured() throws Exception {
      log.debug("start testInvokeSecured()");
      TEntity te = new TEntity("myName", 45, "winter");

      String lookupName = CibetContextInterceptorIT.class.getSimpleName()
            + "/SecuredRemoteEJBImpl!com.cibethelper.ejb.RemoteEJB";
      if (APPSERVER.equals(TOMEE)) {
         lookupName = "global/" + lookupName;
      }
      RemoteEJB remoteEjb = (RemoteEJB) getInitialContext().lookup(lookupName);

      TEntity te2 = remoteEjb.persist(te);
      log.debug(te2);
      Assert.assertTrue(te2.getId() != 0);

      List<Archive> list = ArchiveLoader.loadArchives(TEntity.class.getName());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("Mutzi1", list.get(0).getCreateUser());
      Context.end();
   }

   /**
    * JBoss specific!
    * 
    * @throws Exception
    */
   @Test
   @InSequence(3)
   public void testInvokeWithUser() throws Exception {
      log.debug("start testInvokeWithUser()");
      if (!APPSERVER.equals(JBOSS)) {
         return;
      }

      TEntity te = new TEntity("myName", 46, "winter");

      Properties ejbProperties = new Properties();
      ejbProperties.put("remote.connectionprovider.create.options.org.xnio.Options.SSL_ENABLED", "false");
      // ejbProperties.put(javax.naming.Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
      ejbProperties.put("remote.connections", "1");
      ejbProperties.put("remote.connection.1.host", "localhost");
      ejbProperties.put("remote.connection.1.port", "4447");
      ejbProperties.put("remote.connection.1.username", "ejbuser");
      ejbProperties.put("remote.connection.1.password", "ejbuser123!");

      EJBClientConfiguration ejbClientConfiguration = new PropertiesBasedEJBClientConfiguration(ejbProperties);
      ConfigBasedEJBClientContextSelector selector = new ConfigBasedEJBClientContextSelector(ejbClientConfiguration);
      EJBClientContext.setSelector(selector);
      JBossEjbClientInterceptor interc = new JBossEjbClientInterceptor();
      EJBClientContext.getCurrent().registerInterceptor(0, interc);

      StatelessEJBLocator<RemoteEJB> locator = new StatelessEJBLocator(RemoteEJB.class, "",
            CibetContextInterceptorIT.class.getSimpleName(), "RemoteEJBImpl", "");
      RemoteEJB remoteEjb = org.jboss.ejb.client.EJBClient.createProxy(locator);

      TEntity te2 = remoteEjb.persist(te);
      log.debug(te2);
      Assert.assertTrue(te2.getId() != 0);

      // user and tenant are set in JBossEjbClientInterceptor
      Context.sessionScope().setTenant("comp");
      List<Archive> list = ArchiveLoader.loadArchives(TEntity.class.getName());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("Ernst", list.get(0).getCreateUser());
      Assert.assertEquals(46, ((TEntity) list.get(0).getResource().getObject()).getCounter());
      Context.end();
      interc.setActive(false);
   }

   @Test
   @InSequence(4)
   public void testClientProxy() throws Exception {
      log.debug("start testClientProxy()");

      List<String> acts = new ArrayList<>();
      acts.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(RemoteEJB.class.getName(), acts, ControlEvent.INVOKE);

      TEntity te = new TEntity("myName", 45, "winter");
      Context.sessionScope().setTenant("cccomp");
      Context.sessionScope().setUser("Knacki");

      String lookupName = CibetContextInterceptorIT.class.getSimpleName()
            + "/RemoteEJBImpl!com.cibethelper.ejb.RemoteEJB";
      if (APPSERVER.equals(TOMEE)) {
         lookupName = "global/" + lookupName;
      }
      RemoteEJB remoteEjb = (RemoteEJB) getProxyInitialContext().lookup(lookupName);

      TEntity te2 = remoteEjb.persist(te);
      log.debug(te2);
      Assert.assertTrue(te2.getId() != 0);

      Context.sessionScope().setTenant("cccomp");
      List<Archive> list = ArchiveLoader.loadArchives(RemoteEJB.class.getName());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("Knacki", list.get(0).getCreateUser());
      Assert.assertEquals("cccomp", list.get(0).getTenant());
      Context.sessionScope().setTenant(AbstractAuthenticationProvider.DEFAULT_TENANT);
      Context.end();
   }

   @Test
   @InSequence(5)
   public void testClientProxyRelease() throws Exception {
      log.debug("start testClientProxyRelease()");
      List<String> acts = new ArrayList<>();
      acts.add(ArchiveActuator.DEFAULTNAME);
      acts.add(FourEyesActuator.DEFAULTNAME);
      Setpoint sp = registerSetpoint(RemoteEJB.class.getName(), acts, ControlEvent.INVOKE, ControlEvent.RELEASE);
      sp.setMethod("persist(com.cibethelper.entities.TEntity)");

      TEntity te = new TEntity("myName", 45, "winter");

      Context.sessionScope().setUser("klaus");

      String lookupName = CibetContextInterceptorIT.class.getSimpleName()
            + "/RemoteEJBImpl!com.cibethelper.ejb.RemoteEJB";
      if (APPSERVER.equals(TOMEE)) {
         lookupName = "global/" + lookupName;
      }
      RemoteEJB remoteEjb = (RemoteEJB) getProxyInitialContext().lookup(lookupName);

      remoteEjb.persist(te);
      EventResult er = Context.requestScope().getExecutedEventResult();
      Assert.assertEquals(ExecutionStatus.POSTPONED, er.getExecutionStatus());

      List<Archive> list = ArchiveLoader.loadArchives(RemoteEJB.class.getName());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("klaus", list.get(0).getCreateUser());
      Assert.assertEquals(ExecutionStatus.POSTPONED, list.get(0).getExecutionStatus());

      List<DcControllable> dlist = DcLoader.loadByUser("klaus");
      Assert.assertEquals(1, dlist.size());
      Assert.assertEquals(RemoteEJBInvoker.class.getName(), dlist.get(0).getResource().getInvokerClass());
      Assert.assertEquals(ExecutionStatus.POSTPONED, dlist.get(0).getExecutionStatus());

      log.info("now release");
      Context.sessionScope().setUser("Rele");
      TEntity te2 = (TEntity) dlist.get(0).release("Kala");

      Context.end();
      Context.start();

      Assert.assertNotNull(te2);
      Assert.assertTrue(te2.getId() != 0);

      list = ArchiveLoader.loadArchives(RemoteEJB.class.getName());
      Assert.assertEquals(2, list.size());
      Assert.assertEquals("Rele", list.get(1).getCreateUser());
      Assert.assertEquals(ExecutionStatus.EXECUTED, list.get(1).getExecutionStatus());

      dlist = DcLoader.loadByUser("klaus");
      Assert.assertEquals(1, dlist.size());
      Assert.assertEquals(ExecutionStatus.EXECUTED, dlist.get(0).getExecutionStatus());
      Assert.assertEquals("Rele", dlist.get(0).getApprovalUser());

      HttpGet method = new HttpGet(getBaseURL() + "/execute.cibet?query="
            + URLEncoder.encode("SELECT a FROM TEntity a WHERE a.owner = 'winter'", "UTF-8"));
      HttpResponse response = client.execute(method);
      String res = readResponseBody(response);
      // TEntity id: 22701, counter: 45, owner: winter, xCaltimestamp: null
      Assert.assertTrue(res.contains(", counter: 45, owner: winter, xCaltimestamp: null"));

      // List<TEntity> tlist = (List<TEntity>) dbHelper.select("SELECT a FROM TEntity a WHERE a.owner = 'winter'");
      // Assert.assertEquals(1, tlist.size());
   }

   @Test
   public void testPostponedSSL() throws Exception {
      log.info("start testPostponedSSL()");

      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      KeyStore truststore = KeyStore.getInstance("JKS");
      truststore.load(loader.getResourceAsStream("testTruststore.jks"), "test".toCharArray());

      TrustManagerFactory trustManagerFactory = TrustManagerFactory
            .getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(truststore);
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

      SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext,
            SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

      CloseableHttpClient sslclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();

      HttpGet method = new HttpGet(getBaseSSLURL() + "/login.cibet?USER=Willi&TENANT=" + TENANT);
      HttpResponse response = sslclient.execute(method);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      method.abort();

      log.debug("now the test");

      method = new HttpGet(getBaseSSLURL() + "/target1.url");
      response = sslclient.execute(method);
      Assert.assertEquals(HttpStatus.SC_ACCEPTED, response.getStatusLine().getStatusCode());
      String res = readResponseBody(response);
      // Assert.assertEquals("message: ", res);

      // check("GET", getBaseURL() + "/test/target1", 1);
   }

}
