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
package com.logitags.cibet.http;

import java.io.File;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.realm.text.IniRealm;
import org.apache.shiro.subject.Subject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import com.cibethelper.SpringTestAuthenticationManager;
import com.cibethelper.base.DBHelper;
import com.cibethelper.ejb.RemoteEJB;
import com.cibethelper.ejb.RemoteEJBImpl;
import com.cibethelper.ejb.SimpleEjb;
import com.cibethelper.entities.AbstractTEntity;
import com.cibethelper.entities.ITComplexEntity;
import com.cibethelper.entities.TCompareEntity;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TComplexEntity2;
import com.cibethelper.entities.TEntity;
import com.cibethelper.servlet.ArquillianTestServlet1;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.actuator.dc.FourEyesActuator;
import com.logitags.cibet.actuator.scheduler.SchedulerActuator;
import com.logitags.cibet.actuator.shiro.ShiroActuator;
import com.logitags.cibet.actuator.springsecurity.SpringSecurityActuator;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.ProxyConfig;
import com.logitags.cibet.config.ProxyConfig.ProxyMode;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.it.AbstractArquillian;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.sensor.http.Headers;
import com.logitags.cibet.sensor.http.HttpRequestInvoker;

/**
 * 
 * @author Wolfgang
 * 
 */
@RunWith(Arquillian.class)
public class HttpLittleProxyIT extends AbstractArquillian {

   private static Logger log = Logger.getLogger(HttpLittleProxyIT.class);

   private EntityManager localEM;

   private Setpoint sp = null;

   @Deployment(testable = false)
   public static WebArchive createDeployment1() {
      String warName = HttpLittleProxyIT.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("it/web-shiro2.xml");

      archive.addClasses(AbstractTEntity.class, TEntity.class, TComplexEntity.class, TComplexEntity2.class,
            ITComplexEntity.class, TCompareEntity.class, ArquillianTestServlet1.class, RemoteEJB.class,
            RemoteEJBImpl.class, SimpleEjb.class);

      File[] cibet = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-jpa").withoutTransitivity()
            .asFile();
      archive.addAsLibraries(cibet);

      File[] shiro = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-shiro").withTransitivity()
            .asFile();
      archive.addAsLibraries(shiro);

      File[] shiroweb = Maven.resolver().loadPomFromFile("pom.xml").resolve("org.apache.shiro:shiro-web")
            .withTransitivity().asFile();
      archive.addAsLibraries(shiroweb);

      archive.addAsWebInfResource("META-INF/persistence-it.xml", "classes/META-INF/persistence.xml");
      archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
      archive.addAsWebInfResource("it/config_shiro2.xml", "classes/cibet-config.xml");
      archive.addAsWebInfResource("testTruststore.jks", "classes/testTruststore.jks");
      archive.addAsWebInfResource("shiro.ini", "shiro.ini");

      log.debug(archive.toString(true));
      archive.as(ZipExporter.class).exportTo(new File("target/" + warName), true);

      return archive;
   }

   @BeforeClass
   public static void beforeClassHttpLittleProxyShiroIT() throws Exception {
      DBHelper.beforeClass();
      Realm realm = new IniRealm("classpath:shiro.ini");
      SecurityManager securityManager = new DefaultSecurityManager(realm);
      // Make the SecurityManager instance available to the entire application via static memory:
      SecurityUtils.setSecurityManager(securityManager);
   }

   @AfterClass
   public static void afterClassHttpLittleProxyIT() {
      Configuration.instance().close();
   }

   @Override
   @After
   public void afterAbstractArquillian() throws Exception {
      log.info("execute sub-afterAbstractArquillian(");

      new DBHelper().doAfter();
      if (sp != null) {
         cman.unregisterSetpoint(sp.getId());
      }
   }

   @Before
   public void before() throws Exception {
      log.info("do before()");
      cman = Configuration.instance();
      new DBHelper().doBefore();
      localEM = Context.requestScope().getEntityManager();
   }

   protected void authenticate(String... roles) throws AuthenticationException {
      SpringTestAuthenticationManager authManager = new SpringTestAuthenticationManager();
      for (String role : roles) {
         authManager.addAuthority(role);
      }

      Authentication request = new UsernamePasswordAuthenticationToken("test", "test");
      Authentication result = authManager.authenticate(request);
      SecurityContextHolder.getContext().setAuthentication(result);
   }

   private DcControllable loadDcControllable(int expected) {
      DcControllable dc = null;
      Query q = localEM.createQuery("SELECT a FROM DcControllable a");
      List<DcControllable> list = q.getResultList();
      Assert.assertEquals(expected, list.size());
      if (expected == 1) {
         dc = list.get(0);
      }

      return dc;
   }

   private List<Archive> checkArchive(String method, String url, int expected) throws Exception {
      log.debug("check...");
      Thread.sleep(300);

      List<Archive> list = null;
      for (int i = 1; i < 6; i++) {
         Query q = localEM.createQuery("SELECT a FROM Archive a " + " ORDER BY a.createDate ");
         list = q.getResultList();
         if (expected == list.size())
            break;

         log.debug("No result. Try query again: " + i);
         localEM.clear();
         Thread.sleep(400);
      }

      Assert.assertEquals(expected, list.size());
      if (expected > 0) {
         Archive ar = list.get(0);
         Resource res = ar.getResource();
         Assert.assertEquals(ControlEvent.INVOKE, ar.getControlEvent());
         Assert.assertEquals(url, res.getTargetType());
         Assert.assertEquals(method, res.getMethod());
         Assert.assertEquals(HttpRequestInvoker.class.getName(), res.getInvokerClass());
      }
      return list;
   }

   private void check0() throws Exception {
      Thread.sleep(350);
      Query q = localEM.createQuery("SELECT a FROM Archive a WHERE tenant = '" + TENANT + "'");
      List<Archive> list = q.getResultList();
      Assert.assertEquals(0, list.size());
   }

   private DcControllable checkDc(String target, String method, int count) throws Exception {
      log.debug("now check");

      List<DcControllable> list = null;
      for (int i = 1; i < 6; i++) {
         list = DcLoader.findUnreleased();
         if (1 == list.size())
            break;

         log.debug("No result. Try query again: " + i);
         Thread.sleep(400);
      }

      Assert.assertEquals(count, list.size());
      DcControllable ar = list.get(count - 1);
      Resource res = ar.getResource();
      Assert.assertEquals(ControlEvent.INVOKE, ar.getControlEvent());
      Assert.assertEquals(target, res.getTargetType());
      Assert.assertEquals(method, res.getMethod());
      Assert.assertEquals(HttpRequestInvoker.class.getName(), res.getInvokerClass());
      return ar;
   }

   private HttpPost createHttpPost(String baseURL) throws Exception {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      // InputStream stream = loader.getResourceAsStream("deploy/ejb3-interceptors-aop.xml");
      // String in = IOUtils.toString(stream, "UTF-8");

      String in = StringUtils.repeat("longText", 100);
      log.debug("in.length=" + in.length());

      List<NameValuePair> formparams = new ArrayList<NameValuePair>();
      formparams.add(new BasicNameValuePair("act", "aschenfels"));
      formparams.add(new BasicNameValuePair("dubi2", "Klassenmann"));
      formparams.add(new BasicNameValuePair("longText", in));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
      entity.setChunked(true);
      HttpPost method = new HttpPost(baseURL + "/test/setuser?USER=Willi&TENANT=" + TENANT);
      method.setEntity(entity);
      method.addHeader("cibettestheader", "xxxxxxxxxxxxxx");
      return method;
   }

   @Test
   public void littleProxy1() throws Exception {
      log.info("start littleProxy1()");
      Context.end();

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(getBaseURL() + "/test/setuser", schemes, ControlEvent.INVOKE);

      ProxyConfig config = new ProxyConfig();
      config.setMode(ProxyMode.MITM);
      config.setPort(10112);
      config.setName("proxyTest");
      Configuration.instance().startProxy(config);

      HttpHost proxy = new HttpHost("localhost", 10112);
      client = HttpClients.custom().setProxy(proxy).build();

      HttpPost method = createHttpPost(getBaseURL());
      HttpResponse response = client.execute(method);
      log.debug("STATUS: " + response.getStatusLine().getStatusCode());
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      String msg = readResponseBody(response);
      Assert.assertTrue(msg.equals("set user Willi, and tenant testTenant"));
      String ev = response.getFirstHeader("CIBET_EVENTRESULT").getValue();
      EventResult eventResult = CibetUtil.decodeEventResult(ev);
      log.debug(eventResult);
      Assert.assertEquals(ExecutionStatus.EXECUTED, eventResult.getExecutionStatus());
      Assert.assertEquals("HTTP-PROXY", eventResult.getSensor());
      Assert.assertEquals(1, eventResult.getChildResults().size());

      Context.start();
      localEM = Context.requestScope().getEntityManager();

      List<Archive> archives = checkArchive("POST", getBaseURL() + "/test/setuser", 2);
      Assert.assertEquals(archives.get(0).getCaseId(), archives.get(1).getCaseId());
   }

   @Test
   public void littleProxy1WithContext() throws Exception {
      log.info("start littleProxy1WithContext()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(getBaseURL() + "/test/setuser", schemes, ControlEvent.INVOKE);

      ProxyConfig config = new ProxyConfig();
      config.setMode(ProxyMode.MITM);
      config.setPort(10112);
      config.setName("proxyTest");
      Configuration.instance().startProxy(config);

      HttpHost proxy = new HttpHost("localhost", 10112);
      client = HttpClients.custom().setProxy(proxy).build();

      HttpPost method = createHttpPost(getBaseURL());
      HttpResponse response = client.execute(method);
      log.debug("STATUS: " + response.getStatusLine().getStatusCode());
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      String msg = readResponseBody(response);
      Assert.assertTrue(msg.equals("set user Willi, and tenant testTenant"));
      String ev = response.getFirstHeader("CIBET_EVENTRESULT").getValue();
      EventResult eventResult = CibetUtil.decodeEventResult(ev);
      log.debug(eventResult);
      Assert.assertEquals(ExecutionStatus.EXECUTED, eventResult.getExecutionStatus());
      Assert.assertEquals("HTTP-PROXY", eventResult.getSensor());
      Assert.assertEquals(1, eventResult.getChildResults().size());

      List<Archive> archives = checkArchive("POST", getBaseURL() + "/test/setuser", 2);
      Assert.assertEquals(archives.get(0).getCaseId(), archives.get(1).getCaseId());
   }

   @Test
   public void littleProxyPlay() throws Exception {
      log.info("start littleProxyPlay()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(getBaseURL() + "/test/setuser", schemes, ControlEvent.INVOKE);

      ProxyConfig config = new ProxyConfig();
      config.setMode(ProxyMode.MITM);
      config.setPort(10112);
      config.setName("proxyTest");
      Configuration.instance().startProxy(config);

      HttpHost proxy = new HttpHost("localhost", 10112);
      client = HttpClients.custom().setProxy(proxy).build();

      Context.start(null);
      Context.requestScope().startPlay();
      Context.sessionScope().setUser("Wolfgang");
      HttpPost method = createHttpPost(getBaseURL());
      String h = Context.encodeContext();
      method.addHeader(Headers.CIBET_CONTEXT.name(), h);

      HttpResponse response = client.execute(method);

      log.debug("STATUS: " + response.getStatusLine().getStatusCode());
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      String msg = readResponseBody(response);
      Assert.assertNull(msg);
      String ev = response.getFirstHeader("CIBET_EVENTRESULT").getValue();
      EventResult eventResult = CibetUtil.decodeEventResult(ev);
      log.debug(eventResult);
      Assert.assertEquals(ExecutionStatus.EXECUTED, eventResult.getExecutionStatus());
      Assert.assertEquals("HTTP-PROXY", eventResult.getSensor());
      Assert.assertEquals(0, eventResult.getChildResults().size());
      Assert.assertEquals("Wolfgang", eventResult.getUser());

      checkArchive("POST", getBaseURL() + "/test/setuser", 0);
   }

   @Test
   public void littleProxyShiroAllowed() throws Exception {
      log.info("start littleProxyShiroAllowed()");

      ShiroActuator shiro = new ShiroActuator();
      shiro.setName("shiro1");
      shiro.setIsPermittedAll(new String[] { "lightsaber:gogo" });
      Configuration.instance().registerActuator(shiro);

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add("shiro1");
      sp = registerSetpoint(getBaseURL() + "/test/setuser", schemes, ControlEvent.INVOKE);

      ProxyConfig config = new ProxyConfig();
      config.setMode(ProxyMode.MITM);
      config.setPort(10112);
      config.setName("proxyTest");
      Configuration.instance().startProxy(config);

      UsernamePasswordToken token = new UsernamePasswordToken("lonestarr", "vespa");
      Subject currentUser = SecurityUtils.getSubject();
      currentUser.login(token);
      log.debug("is permitted lightsaber:weild: " + currentUser.isPermitted("lightsaber:weild"));

      HttpHost proxy = new HttpHost("localhost", 10112);
      client = HttpClients.custom().setProxy(proxy).build();

      HttpPost method = createHttpPost(getBaseURL());
      String h = Context.encodeContext();
      method.addHeader(Headers.CIBET_CONTEXT.name(), h);

      HttpResponse response = client.execute(method);

      log.debug("STATUS: " + response.getStatusLine().getStatusCode());
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      String msg = readResponseBody(response);
      Assert.assertTrue(msg.equals("set user Willi, and tenant testTenant"));
      String ev = response.getFirstHeader("CIBET_EVENTRESULT").getValue();
      EventResult eventResult = CibetUtil.decodeEventResult(ev);
      log.debug(eventResult);
      Assert.assertEquals(ExecutionStatus.EXECUTED, eventResult.getExecutionStatus());
      Assert.assertEquals("HTTP-PROXY", eventResult.getSensor());
      Assert.assertEquals(1, eventResult.getChildResults().size());
      Assert.assertEquals("lonestarr", eventResult.getUser());

      List<Archive> ars = checkArchive("POST", getBaseURL() + "/test/setuser", 2);
      log.info("1. create date: " + ars.get(0).getCreateDate());
      log.info("2. create date: " + ars.get(1).getCreateDate());
      Assert.assertTrue("Willi".equals(ars.get(0).getCreateUser()) || "lonestarr".equals(ars.get(0).getCreateUser()));
      Assert.assertTrue("Willi".equals(ars.get(1).getCreateUser()) || "lonestarr".equals(ars.get(1).getCreateUser()));
      // Assert.assertEquals("lonestarr", ars.get(1).getCreateUser());
      currentUser.logout();
   }

   @Test
   public void littleProxyShiroDenied() throws Exception {
      log.info("start littleProxyShiroDenied()");

      ShiroActuator shiro = new ShiroActuator();
      shiro.setName("shiro1");
      shiro.setIsPermittedAll(new String[] { "not-lightsaber:gogo" });
      Configuration.instance().registerActuator(shiro);

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add("shiro1");
      sp = registerSetpoint(getBaseURL() + "/test/setuser", schemes, ControlEvent.INVOKE);

      ProxyConfig config = new ProxyConfig();
      config.setMode(ProxyMode.MITM);
      config.setPort(10112);
      config.setName("proxyTest");
      Configuration.instance().startProxy(config);

      UsernamePasswordToken token = new UsernamePasswordToken("lonestarr", "vespa");
      Subject currentUser = SecurityUtils.getSubject();
      currentUser.login(token);

      HttpHost proxy = new HttpHost("localhost", 10112);
      client = HttpClients.custom().setProxy(proxy).build();

      HttpPost method = createHttpPost(getBaseURL());
      String h = Context.encodeContext();
      method.addHeader(Headers.CIBET_CONTEXT.name(), h);

      HttpResponse response = client.execute(method);

      log.debug("STATUS: " + response.getStatusLine().getStatusCode());
      Assert.assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatusLine().getStatusCode());
      String msg = readResponseBody(response);
      Assert.assertNull(msg);
      String ev = response.getFirstHeader("CIBET_EVENTRESULT").getValue();
      EventResult eventResult = CibetUtil.decodeEventResult(ev);
      log.debug(eventResult);
      Assert.assertEquals(ExecutionStatus.DENIED, eventResult.getExecutionStatus());
      Assert.assertEquals("HTTP-PROXY", eventResult.getSensor());
      Assert.assertEquals(0, eventResult.getChildResults().size());
      Assert.assertEquals("lonestarr", eventResult.getUser());

      checkArchive("POST", getBaseURL() + "/test/setuser", 1);
      Configuration.instance().unregisterSetpoint(sp.getId());
      currentUser.logout();
   }

   @Test
   public void littleProxySpringDenied() throws Exception {
      log.info("start littleProxySpringDenied()");

      new ClassPathXmlApplicationContext(new String[] { "spring-context_2.xml" });

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setUrlAccess("hasRole('Heinz')");

      authenticate("NOTTY");
      Context.sessionScope().setUser("test");
      log.debug("AUTH: " + SecurityContextHolder.getContext().getAuthentication());

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(SpringSecurityActuator.DEFAULTNAME);
      sp = registerSetpoint(getBaseURL() + "/test/setuser", schemes, ControlEvent.INVOKE);

      ProxyConfig config = new ProxyConfig();
      config.setMode(ProxyMode.MITM);
      config.setPort(10112);
      config.setName("proxyTest");
      Configuration.instance().startProxy(config);

      HttpHost proxy = new HttpHost("localhost", 10112);
      client = HttpClients.custom().setProxy(proxy).build();

      HttpPost method = createHttpPost(getBaseURL());

      String h = Context.encodeContext();
      method.addHeader(Headers.CIBET_CONTEXT.name(), h);

      HttpResponse response = client.execute(method);

      log.debug("STATUS: " + response.getStatusLine().getStatusCode());
      Assert.assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatusLine().getStatusCode());
      String msg = readResponseBody(response);
      Assert.assertNull(msg);
      String ev = response.getFirstHeader("CIBET_EVENTRESULT").getValue();
      EventResult eventResult = CibetUtil.decodeEventResult(ev);
      log.debug(eventResult);
      Assert.assertEquals(ExecutionStatus.DENIED, eventResult.getExecutionStatus());
      Assert.assertEquals("HTTP-PROXY", eventResult.getSensor());
      Assert.assertEquals(0, eventResult.getChildResults().size());
      Assert.assertEquals("test", eventResult.getUser());

      checkArchive("POST", getBaseURL() + "/test/setuser", 1);
   }

   @Test
   public void littleProxySpringAllowed() throws Exception {
      log.info("start littleProxySpringAllowed()");

      new ClassPathXmlApplicationContext(new String[] { "spring-context_2.xml" });

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setUrlAccess("hasRole('Heinz')");

      authenticate("Heinz");
      Context.sessionScope().setUser("test");
      log.debug("AUTH: " + SecurityContextHolder.getContext().getAuthentication());

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(SpringSecurityActuator.DEFAULTNAME);
      sp = registerSetpoint(getBaseURL() + "/test/setuser", schemes, ControlEvent.INVOKE);

      ProxyConfig config = new ProxyConfig();
      config.setMode(ProxyMode.MITM);
      config.setPort(10112);
      config.setName("proxyTest");
      Configuration.instance().startProxy(config);

      HttpHost proxy = new HttpHost("localhost", 10112);
      client = HttpClients.custom().setProxy(proxy).build();

      HttpPost method = createHttpPost(getBaseURL());

      String h = Context.encodeContext();
      method.addHeader(Headers.CIBET_CONTEXT.name(), h);

      HttpResponse response = client.execute(method);

      log.debug("STATUS: " + response.getStatusLine().getStatusCode());
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      String msg = readResponseBody(response);
      Assert.assertNotNull(msg);
      String ev = response.getFirstHeader("CIBET_EVENTRESULT").getValue();
      EventResult eventResult = CibetUtil.decodeEventResult(ev);
      log.debug(eventResult);
      Assert.assertEquals(ExecutionStatus.EXECUTED, eventResult.getExecutionStatus());
      Assert.assertEquals("HTTP-PROXY", eventResult.getSensor());
      Assert.assertEquals(1, eventResult.getChildResults().size());
      Assert.assertEquals("test", eventResult.getUser());

      checkArchive("POST", getBaseURL() + "/test/setuser", 2);
   }

   @Test
   public void littleProxy4Eyes() throws Exception {
      log.info("start littleProxy4Eyes()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      sp = registerSetpoint(getBaseURL() + "/test/setuser", schemes, ControlEvent.INVOKE);

      ProxyConfig config = new ProxyConfig();
      config.setMode(ProxyMode.MITM);
      config.setPort(10112);
      config.setName("proxyTest");
      Configuration.instance().startProxy(config);

      HttpHost proxy = new HttpHost("localhost", 10112);
      client = HttpClients.custom().setProxy(proxy).build();

      HttpPost method = createHttpPost(getBaseURL());

      Context.start(null);
      Context.sessionScope().setUser("Olbert");

      String h = Context.encodeContext();
      method.addHeader(Headers.CIBET_CONTEXT.name(), h);

      HttpResponse response = client.execute(method);

      log.debug("STATUS: " + response.getStatusLine().getStatusCode());
      Assert.assertEquals(HttpStatus.SC_ACCEPTED, response.getStatusLine().getStatusCode());
      String msg = readResponseBody(response);
      Assert.assertNull(msg);
      String ev = response.getFirstHeader("CIBET_EVENTRESULT").getValue();
      EventResult eventResult = CibetUtil.decodeEventResult(ev);
      log.debug(eventResult);
      Assert.assertEquals(ExecutionStatus.POSTPONED, eventResult.getExecutionStatus());
      Assert.assertEquals("HTTP-PROXY", eventResult.getSensor());
      Assert.assertEquals(0, eventResult.getChildResults().size());
      Assert.assertEquals("Olbert", eventResult.getUser());

      checkArchive("POST", getBaseURL() + "/test/setuser", 1);

      DcControllable dc = loadDcControllable(1);
      Assert.assertEquals(FourEyesActuator.DEFAULTNAME, dc.getActuator());
      Assert.assertEquals("Olbert", dc.getCreateUser());
      Assert.assertEquals(getBaseURL() + "/test/setuser", dc.getResource().getTargetType());

      log.debug("now release");
      // EntityManager em = emf.createEntityManager();
      // em.getTransaction().begin();
      Context.sessionScope().setUser("releasOL");

      response = (HttpResponse) dc.release("good");
      // em.getTransaction().

      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      msg = readResponseBody(response);
      Assert.assertNotNull(msg);
      ev = response.getFirstHeader("CIBET_EVENTRESULT").getValue();
      eventResult = CibetUtil.decodeEventResult(ev);
      log.debug(eventResult);
      Assert.assertEquals(ExecutionStatus.EXECUTED, eventResult.getExecutionStatus());
      Assert.assertEquals("HTTP-PROXY", eventResult.getSensor());
      Assert.assertEquals(1, eventResult.getChildResults().size());
      Assert.assertEquals("releasOL", eventResult.getUser());
      Assert.assertEquals("HTTP-FILTER", eventResult.getChildResults().get(0).getSensor());
      Assert.assertNull(eventResult.getChildResults().get(0).getUser());
      Context.end();
   }

   @Test
   public void littleProxy4Scheduled() throws Exception {
      log.info("start littleProxyScheduled()");
      Context.end();

      ProxyConfig config = new ProxyConfig();
      config.setMode(ProxyMode.MITM);
      config.setPort(10112);
      config.setName("proxyTest");
      Configuration.instance().startProxy(config);

      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.SECOND, 6);
      SchedulerActuator sa = new SchedulerActuator("sm1");
      sa.setTimerStart(cal.getTime());
      sa.setPersistenceUnit("localTest");
      sa.setAutoRemoveScheduledDate(true);
      Configuration.instance().registerActuator(sa);

      List<String> acts = new ArrayList<>();
      acts.add("sm1");
      acts.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(getBaseURL() + "/test/setuser", acts, ControlEvent.INVOKE, ControlEvent.RELEASE);

      HttpHost proxy = new HttpHost("localhost", 10112);
      client = HttpClients.custom().setProxy(proxy).build();

      HttpPost method = createHttpPost(getBaseURL());

      Context.start();
      localEM = Context.requestScope().getEntityManager();

      Context.sessionScope().setUser("Olbert");
      Context.requestScope().setRemark("created");
      Context.requestScope().setScheduledDate(Calendar.SECOND, 3);
      // log.debug("++ " + Context.requestScope());

      String h = Context.encodeContext();
      method.addHeader(Headers.CIBET_CONTEXT.name(), h);

      HttpResponse response = client.execute(method);

      log.debug("STATUS: " + response.getStatusLine().getStatusCode());
      Assert.assertEquals(HttpStatus.SC_ACCEPTED, response.getStatusLine().getStatusCode());
      String msg = readResponseBody(response);
      Assert.assertNull(msg);
      String ev = response.getFirstHeader("CIBET_EVENTRESULT").getValue();
      EventResult eventResult = CibetUtil.decodeEventResult(ev);
      log.debug(eventResult);
      Assert.assertEquals(ExecutionStatus.SCHEDULED, eventResult.getExecutionStatus());
      Assert.assertEquals("HTTP-PROXY", eventResult.getSensor());
      Assert.assertEquals(0, eventResult.getChildResults().size());
      Assert.assertEquals("Olbert", eventResult.getUser());

      // Context.end();
      // Context.start();
      // localEM = Context.requestScope().getEntityManager();

      checkArchive("POST", getBaseURL() + "/test/setuser", 1);

      DcControllable dc = loadDcControllable(1);
      Assert.assertEquals(ExecutionStatus.SCHEDULED, dc.getExecutionStatus());
      Assert.assertEquals("sm1", dc.getActuator());
      Assert.assertEquals("Olbert", dc.getCreateUser());
      Assert.assertEquals(getBaseURL() + "/test/setuser", dc.getResource().getTargetType());

      Context.end();

      log.debug("-------------------- sleep");
      Thread.sleep(10000);
      log.debug("--------------- after TimerTask");
      Context.internalRequestScope().getEntityManager().flush();

      // log.debug("-------------------- wait...");
      // Thread.sleep(150000);

      Context.start();
      localEM = Context.requestScope().getEntityManager();

      dc = loadDcControllable(1);
      Assert.assertEquals(ExecutionStatus.EXECUTED, dc.getExecutionStatus());

      List<Archive> arlist = checkArchive("POST", getBaseURL() + "/test/setuser", 3);
      // depends on who is quicker, client or server
      Assert.assertTrue(ControlEvent.RELEASE_INVOKE == arlist.get(1).getControlEvent()
            || ControlEvent.RELEASE_INVOKE == arlist.get(2).getControlEvent());
      Assert.assertEquals(ExecutionStatus.EXECUTED, arlist.get(1).getExecutionStatus());
      Assert.assertEquals(arlist.get(0).getCaseId(), arlist.get(1).getCaseId());
      Assert.assertEquals(arlist.get(0).getCaseId(), arlist.get(2).getCaseId());

   }

   @Test
   public void littleProxyTimeout() throws Exception {
      log.info("start littleProxyTimeout()");

      String url = "http://httpbin.org/delay/4";
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(url, schemes, ControlEvent.INVOKE);

      ProxyConfig config = new ProxyConfig();
      config.setMode(ProxyMode.MITM);
      config.setPort(10112);
      config.setTimeout(2000);
      config.setName("proxyTest");
      Configuration.instance().startProxy(config);

      HttpHost proxy = new HttpHost("localhost", 10112);
      client = HttpClients.custom().setProxy(proxy).build();

      HttpGet method = new HttpGet(url);
      HttpResponse response = client.execute(method);
      log.debug("STATUS: " + response.getStatusLine().getStatusCode());
      Assert.assertEquals(HttpStatus.SC_GATEWAY_TIMEOUT, response.getStatusLine().getStatusCode());
      String msg = readResponseBody(response);
      String ev = response.getFirstHeader("CIBET_EVENTRESULT").getValue();
      EventResult eventResult = CibetUtil.decodeEventResult(ev);
      log.debug(eventResult);
      Assert.assertEquals(ExecutionStatus.ERROR, eventResult.getExecutionStatus());

      List<Archive> archives = checkArchive("GET", url, 1);
      Archive a = archives.get(0);
      log.debug(a);
      Assert.assertEquals(ExecutionStatus.ERROR, a.getExecutionStatus());
      Assert.assertEquals(HttpStatus.SC_GATEWAY_TIMEOUT, a.getResource().getResultObject());
   }

   @Test
   public void littleProxyNoSSLWrongURL() throws Exception {
      log.info("start littleProxyNoSSLWrongURL()");

      String url = "http://www.notexistingurl/LittleProxyTest";
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(url, schemes, ControlEvent.INVOKE);

      ProxyConfig config = new ProxyConfig();
      config.setMode(ProxyMode.MITM);
      config.setPort(10112);
      config.setName("proxyTest");
      Configuration.instance().startProxy(config);

      HttpHost proxy = new HttpHost("localhost", 10112);
      client = HttpClients.custom().setProxy(proxy).build();

      HttpGet method = new HttpGet(url);
      HttpResponse response = client.execute(method);
      log.debug("STATUS: " + response.getStatusLine().getStatusCode());
      Assert.assertEquals(HttpStatus.SC_BAD_GATEWAY, response.getStatusLine().getStatusCode());
      String msg = readResponseBody(response);
      String ev = response.getFirstHeader("CIBET_EVENTRESULT").getValue();
      EventResult eventResult = CibetUtil.decodeEventResult(ev);
      log.debug(eventResult);
      Assert.assertEquals(ExecutionStatus.ERROR, eventResult.getExecutionStatus());

      List<Archive> archives = checkArchive("GET", url, 1);
      Archive a = archives.get(0);
      log.debug(a);
      Assert.assertEquals(ExecutionStatus.ERROR, a.getExecutionStatus());
      Assert.assertEquals(HttpStatus.SC_BAD_GATEWAY, a.getResource().getResultObject());
   }

   @Test
   public void failedConnect() throws Exception {
      log.info("start failedConnect()");

      String url = "http://10.255.255.1/LittleProxyTest";
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(url, schemes, ControlEvent.INVOKE);

      ProxyConfig config = new ProxyConfig();
      config.setMode(ProxyMode.MITM);
      config.setPort(10112);
      config.setName("proxyTest");
      Configuration.instance().startProxy(config);

      HttpHost proxy = new HttpHost("localhost", 10112);
      client = HttpClients.custom().setProxy(proxy).build();

      HttpGet method = new HttpGet(url);
      HttpResponse response = client.execute(method);
      log.debug("STATUS: " + response.getStatusLine().getStatusCode());
      Assert.assertEquals(HttpStatus.SC_GATEWAY_TIMEOUT, response.getStatusLine().getStatusCode());
      String msg = readResponseBody(response);
      String ev = response.getFirstHeader("CIBET_EVENTRESULT").getValue();
      EventResult eventResult = CibetUtil.decodeEventResult(ev);
      log.debug(eventResult);
      Assert.assertEquals(ExecutionStatus.ERROR, eventResult.getExecutionStatus());

      List<Archive> archives = checkArchive("GET", url, 1);
      Archive a = archives.get(0);
      log.debug(a);
      Assert.assertEquals(ExecutionStatus.ERROR, a.getExecutionStatus());
      Assert.assertEquals(HttpStatus.SC_GATEWAY_TIMEOUT, a.getResource().getResultObject());
   }

   @Test
   public void ssl() throws Exception {
      log.info("start ssl()");

      String url = "https://httpbin.org/ip";
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(url, schemes, ControlEvent.INVOKE);
      sp.setMethod("GET");

      ProxyConfig config = new ProxyConfig();
      config.setMode(ProxyMode.MITM);
      config.setPort(10112);
      config.setName("proxyTest");
      Configuration.instance().startProxy(config);

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

      HttpHost proxy = new HttpHost("localhost", 10112);
      client = HttpClients.custom().setSSLSocketFactory(sslsf).setProxy(proxy).disableAutomaticRetries().build();

      HttpGet method = new HttpGet(url);
      HttpResponse response = client.execute(method);
      log.debug("STATUS: " + response.getStatusLine().getStatusCode());
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      String msg = readResponseBody(response);
      String ev = response.getFirstHeader("CIBET_EVENTRESULT").getValue();
      EventResult eventResult = CibetUtil.decodeEventResult(ev);
      log.debug(eventResult);
      Assert.assertEquals(ExecutionStatus.EXECUTED, eventResult.getExecutionStatus());

      List<Archive> archives = checkArchive("GET", url, 1);
      Archive a = archives.get(0);
      log.debug(a);
      Assert.assertEquals(ExecutionStatus.EXECUTED, a.getExecutionStatus());
      Assert.assertEquals(HttpStatus.SC_OK, a.getResource().getResultObject());
   }

   @Test
   public void sslConnect() throws Exception {
      log.info("start sslConnect()");

      String url = "https://httpbin.org/ip";
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(url, schemes, ControlEvent.INVOKE);
      sp.setMethod("CONNECT");

      ProxyConfig config = new ProxyConfig();
      config.setMode(ProxyMode.MITM);
      config.setPort(10112);
      config.setName("proxyTest");
      Configuration.instance().startProxy(config);

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

      HttpHost proxy = new HttpHost("localhost", 10112);
      client = HttpClients.custom().setSSLSocketFactory(sslsf).setProxy(proxy).disableAutomaticRetries().build();

      HttpGet method = new HttpGet(url);
      HttpResponse response = client.execute(method);
      log.debug("STATUS: " + response.getStatusLine().getStatusCode());
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      String msg = readResponseBody(response);
      String ev = response.getFirstHeader("CIBET_EVENTRESULT").getValue();
      EventResult eventResult = CibetUtil.decodeEventResult(ev);
      log.debug(eventResult);
      Assert.assertEquals(ExecutionStatus.EXECUTED, eventResult.getExecutionStatus());

      List<Archive> archives = checkArchive("CONNECT", "httpbin.org:443", 1);
      Archive a = archives.get(0);
      log.debug(a);
      Assert.assertEquals(ExecutionStatus.EXECUTED, a.getExecutionStatus());
      Assert.assertEquals(HttpStatus.SC_OK, a.getResource().getResultObject());
   }

   @Test
   public void sslConnectAndGet() throws Exception {
      log.info("start sslConnectAndGet()");

      String url = "https://httpbin.org/ip";
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(url, schemes, ControlEvent.INVOKE);
      sp.setMethod("CONNECT", "GET");

      ProxyConfig config = new ProxyConfig();
      config.setMode(ProxyMode.MITM);
      config.setPort(10112);
      config.setName("proxyTest");
      Configuration.instance().startProxy(config);

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

      HttpHost proxy = new HttpHost("localhost", 10112);
      client = HttpClients.custom().setSSLSocketFactory(sslsf).setProxy(proxy).disableAutomaticRetries().build();

      HttpGet method = new HttpGet(url);

      Context.internalRequestScope().setManaged(true);
      Context.sessionScope().setUser("Wolfgang");
      String h = Context.encodeContext();
      method.addHeader(Headers.CIBET_CONTEXT.name(), h);

      HttpResponse response = client.execute(method);
      log.debug("STATUS: " + response.getStatusLine().getStatusCode());
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      String msg = readResponseBody(response);
      String ev = response.getFirstHeader("CIBET_EVENTRESULT").getValue();
      EventResult eventResult = CibetUtil.decodeEventResult(ev);
      log.debug(eventResult);
      Assert.assertEquals(ExecutionStatus.EXECUTED, eventResult.getExecutionStatus());

      List<Archive> archives = checkArchive("CONNECT", "httpbin.org:443", 2);
      Archive a = archives.get(0);
      log.debug(a);
      Assert.assertEquals(ExecutionStatus.EXECUTED, a.getExecutionStatus());
      Assert.assertEquals(HttpStatus.SC_OK, a.getResource().getResultObject());

      Archive a2 = archives.get(1);
      log.debug(a2);
      Assert.assertEquals(ExecutionStatus.EXECUTED, a2.getExecutionStatus());
      Assert.assertEquals(HttpStatus.SC_OK, a2.getResource().getResultObject());
      Assert.assertEquals(url, a2.getResource().getTargetType());
      Assert.assertEquals("Wolfgang", a2.getCreateUser());
      Assert.assertEquals("GET", a2.getResource().getMethod());
   }

   @Test
   public void sslWrongPort() throws Exception {
      log.info("start sslWrongPort()");

      String url = "https://httpbin.org:8081/ip";
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(url, schemes, ControlEvent.INVOKE);
      sp.setMethod("CONNECT", "GET");

      ProxyConfig config = new ProxyConfig();
      config.setMode(ProxyMode.MITM);
      config.setPort(10112);
      config.setName("proxyTest");
      Configuration.instance().startProxy(config);

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

      HttpHost proxy = new HttpHost("localhost", 10112);
      client = HttpClients.custom().setSSLSocketFactory(sslsf).setProxy(proxy).disableAutomaticRetries().build();

      HttpGet method = new HttpGet(url);

      Context.internalRequestScope().setManaged(true);
      Context.sessionScope().setUser("Wolfgang");
      String h = Context.encodeContext();
      method.addHeader(Headers.CIBET_CONTEXT.name(), h);

      HttpResponse response = client.execute(method);
      log.debug("STATUS: " + response.getStatusLine().getStatusCode());
      Assert.assertTrue(HttpStatus.SC_BAD_GATEWAY == response.getStatusLine().getStatusCode()
            || HttpStatus.SC_GATEWAY_TIMEOUT == response.getStatusLine().getStatusCode());

      readResponseBody(response);
      String ev = response.getFirstHeader("CIBET_EVENTRESULT").getValue();
      EventResult eventResult = CibetUtil.decodeEventResult(ev);
      log.debug(eventResult);
      Assert.assertEquals(ExecutionStatus.ERROR, eventResult.getExecutionStatus());

      List<Archive> archives = checkArchive("CONNECT", "httpbin.org:8081", 1);
      Archive a = archives.get(0);
      log.debug(a);
      Assert.assertTrue(HttpStatus.SC_BAD_GATEWAY == (int) a.getResource().getResultObject()
            || HttpStatus.SC_GATEWAY_TIMEOUT == (int) a.getResource().getResultObject());
      // if (TOMEE.equals(APPSERVER)) {
      // Assert.assertEquals(HttpStatus.SC_BAD_GATEWAY, a.getResource().getResultObject());
      // } else {
      // Assert.assertEquals(HttpStatus.SC_GATEWAY_TIMEOUT, a.getResource().getResultObject());
      // }

      Assert.assertEquals(ExecutionStatus.ERROR, a.getExecutionStatus());
   }

   @Test
   public void proxy() throws Exception {
      log.info("start proxy()");
      client = HttpClients.createDefault();

      String url = getBaseURL() + "/test/proxy?url=";
      url = url + URLEncoder.encode(getBaseSSLURL(), "UTF-8");
      HttpGet method = new HttpGet(url);
      HttpResponse response = client.execute(method);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      List<Archive> archives = checkArchive("CONNECT", "localhost:8743", 3);
      String caseid1 = archives.get(0).getCaseId();
      String caseid2 = archives.get(1).getCaseId();
      String caseid3 = archives.get(2).getCaseId();
      Assert.assertEquals(caseid1, caseid2);
      Assert.assertEquals(caseid3, caseid2);
   }

}
