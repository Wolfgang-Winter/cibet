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
package com.logitags.cibet.sensor.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.ProxyConfig;
import com.logitags.cibet.config.ProxyConfig.ProxyMode;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

public class LittleProxyTest {

   private static Logger log = Logger.getLogger(LittleProxyTest.class);

   private List<Exception> exceptions = Collections.synchronizedList(new ArrayList<Exception>());

   protected static final String TENANT = "testTenant";

   protected CloseableHttpClient client;

   private static HttpProxyServer PROXY;

   @AfterClass
   public static void doAfter() {
      if (PROXY != null) {
         PROXY.stop();
         PROXY = null;
         log.info("PROXY stopped");
      }
      Configuration.instance().close();
   }

   @After
   public void afterTest() {
      Configuration.instance().close();
   }

   private class ThreadExecution extends Thread {

      private int testNumber;

      public ThreadExecution(String name, int testnbr) {
         super(name);
         testNumber = testnbr;
      }

      public void run() {
         try {
            log.info("++++ run test nr " + testNumber + " in thread " + Thread.currentThread().getName());
            // if (1 == 1) return;
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
            CloseableHttpClient client = HttpClients.custom().setSSLSocketFactory(sslsf).setProxy(proxy)
                  .disableAutomaticRetries().build();

            HttpGet method = createHttpGetExtern("https://httpbin.org/ip");
            // HttpPost method = createHttpPost(getBaseSSLURL());
            HttpResponse response = client.execute(method);
            // log.debug("STATUS: " + response.getStatusLine().getStatusCode());
            String msg = readResponseBody(response);

            log.debug("2. run");
            HttpHost proxy2 = new HttpHost("localhost", 10112);
            CloseableHttpClient client2 = HttpClients.custom().setSSLSocketFactory(sslsf).setProxy(proxy2)
                  .disableAutomaticRetries().build();

            HttpGet method2 = createHttpGetExtern("https://httpbin.org/ip");
            // HttpPost method2 = createHttpPost(getBaseSSLURL());
            HttpResponse response2 = client2.execute(method2);
            // log.debug("STATUS: " + response.getStatusLine().getStatusCode());
            String msg2 = readResponseBody(response2);

         } catch (Exception e) {
            log.error(e.getMessage(), e);
            exceptions.add(e);
         }
      }
   }

   private HttpPost createHttpPost(String baseURL) throws Exception {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      InputStream stream = loader.getResourceAsStream("config_actuator3.xml");
      String in = IOUtils.toString(stream, "UTF-8");

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

   private HttpGet createHttpGetExtern(String url) throws Exception {
      HttpGet method = new HttpGet(url + "?USER=Willi&TENANT=" + TENANT);
      method.addHeader("cibettestheader", "xxxxxxxxxxxxxx");
      return method;
   }

   protected String getBaseURL() {
      return "http://localhost:8788/LittleProxyTest";
   }

   protected String getBaseSSLURL() {
      return "https://localhost:8743/LittleProxyTest";
   }

   protected String getBaseSSLClientURL() {
      return "https://localhost:8753/LittleProxyTest";
   }

   protected String readResponseBody(HttpResponse response) throws Exception {
      Header[] headers = response.getAllHeaders();
      log.debug("HEADERS:");
      for (Header header : headers) {
         log.debug(header.getName() + " = " + header.getValue());
      }

      // Read the response body.
      HttpEntity entity = response.getEntity();
      InputStream instream = null;
      try {
         if (entity != null) {
            StringBuffer b = new StringBuffer();
            instream = entity.getContent();

            BufferedReader reader = new BufferedReader(new InputStreamReader(instream));
            String body = reader.readLine();
            while (body != null) {
               b.append(body);
               body = reader.readLine();
            }
            log.info("body=" + b.toString());
            return b.toString();
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

   private void startLittleProxyAsProxy() {
      log.info("start PROXY");
      PROXY = DefaultHttpProxyServer.bootstrap().withPort(10113).withFiltersSource(new HttpFiltersSourceAdapter() {
         public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
            return new HttpFiltersAdapter(originalRequest) {
               @Override
               public io.netty.handler.codec.http.HttpResponse clientToProxyRequest(HttpObject httpObject) {
                  log.info("clientToProxyRequest PROXY");

                  return null;
               }

               @Override
               public io.netty.handler.codec.http.HttpResponse proxyToServerRequest(HttpObject httpObject) {
                  log.info("proxyToServerRequest  PROXY");
                  return null;
               }

               @Override
               public HttpObject serverToProxyResponse(HttpObject httpObject) {
                  log.info("serverToProxyResponse PROXY");
                  // TODO: implement your filtering here
                  return httpObject;
               }

               @Override
               public HttpObject proxyToClientResponse(HttpObject httpObject) {
                  log.info("proxyToClientResponse PROXY");
                  // TODO: implement your filtering here
                  return httpObject;
               }
            };
         }
      }).start();
      log.info("littleProxy started");
   }

   /**
    * clientToProxyRequest: POST http://localhost:8788/LittleProxyTest/...?USER=Willi&amp;TENANT=testTenant HTTP/1.1<br>
    * proxyToServerConnectionQueued<br>
    * proxyToServerResolutionStarted<br>
    * proxyToServerResolutionSucceeded<br>
    * proxyToServerRequest: POST http://localhost:8788/LittleProxyTest/...Willi&amp;TENANT=testTenant HTTP/1.1 Via=1.1
    * Chef <br>
    * proxyToServerConnectionStarted<br>
    * proxyToServerConnectionSucceeded: io.netty.channel.DefaultChannelHandlerContext<br>
    * proxyToServerRequestSending<br>
    * proxyToServerRequestSent<br>
    * serverToProxyResponseReceiving<br>
    * serverToProxyResponse: io.netty.handler.codec.http.DefaultHttpResponse<br>
    * proxyToClientResponse: DefaultHttpResponse<br>
    * serverToProxyResponse: io.netty.handler.codec.http.DefaultLastHttpContent<br>
    * proxyToClientResponse DefaultLastHttpContent<br>
    * serverToProxyResponseReceived<br>
    * MITM = CHANEDPROXY
    * 
    * @throws Exception
    */
   @Test
   public void littleProxyNoSSL() throws Exception {
      log.info("start littleProxyNoSSL()");

      ProxyConfig config = new ProxyConfig();
      config.setMode(ProxyMode.MITM);
      config.setPort(10112);
      config.setName("p2");
      Configuration.instance().startProxy(config);

      HttpHost proxy = new HttpHost("localhost", 10112);
      client = HttpClients.custom().setProxy(proxy).build();

      // HttpPost method = createHttpPost(getBaseURL());
      HttpGet method = createHttpGetExtern("http://httpbin.org/ip");
      HttpResponse response = client.execute(method);
      log.debug("STATUS: " + response.getStatusLine().getStatusCode());
      String msg = readResponseBody(response);
      Assert.assertTrue(msg.startsWith("{  \"origin\": \""));
      String ev = response.getFirstHeader("CIBET_EVENTRESULT").getValue();
      EventResult eventResult = CibetUtil.decodeEventResult(ev);
      log.debug(eventResult);
      Assert.assertEquals(ExecutionStatus.EXECUTED, eventResult.getExecutionStatus());
      Assert.assertEquals("HTTP-PROXY", eventResult.getSensor());
   }

   /**
    * MITM:<br>
    * clientToProxyRequest: CONNECT localhost:8743 HTTP/1.1<br>
    * proxyToServerConnectionQueued<br>
    * proxyToServerResolutionStarted<br>
    * proxyToServerResolutionSucceeded<br>
    * proxyToServerRequest: CONNECT localhost:8743 HTTP/1.1 Via=1.1 Chef<br>
    * proxyToServerConnectionStarted<br>
    * proxyToServerConnectionSSLHandshakeStarted<br>
    * proxyToServerConnectionSucceeded: io.netty.channel.DefaultChannelHandlerContext<br>
    * clientToProxyRequest: POST /LittleProxyTest/test/setuser?USER=Willi&amp;TENANT=testTenant HTTP/1.1<br>
    * proxyToServerRequest: POST /LittleProxyTest/test/setuser?USER=Willi&amp;TENANT=testTenant HTTP/1.1 Via=1.1 Chef
    * <br>
    * proxyToServerRequestSending<br>
    * proxyToServerRequestSent<br>
    * serverToProxyResponseReceiving<br>
    * serverToProxyResponse: io.netty.handler.codec.http.DefaultHttpResponse<br>
    * proxyToClientResponse: DefaultHttpResponse<br>
    * serverToProxyResponse: io.netty.handler.codec.http.DefaultLastHttpContent<br>
    * proxyToClientResponse DefaultLastHttpContent<br>
    * serverToProxyResponseReceived<br>
    * CHAINEDPROXY:<br>
    * clientToProxyRequest: CONNECT localhost:8743 HTTP/1.1<br>
    * proxyToServerConnectionQueued<br>
    * proxyToServerResolutionStarted<br>
    * proxyToServerResolutionSucceeded<br>
    * proxyToServerRequest: CONNECT localhost:8743 HTTP/1.1 Via=1.1 Chef<br>
    * proxyToServerConnectionStarted<br>
    * 
    * @throws Exception
    */
   @Test
   public void littleProxySSL() throws Exception {
      log.info("start littleProxySSL()");
      System.setProperty("cibet.proxy.clientKeystore.tt1", "clientKeystore.jks");
      System.setProperty("cibet.proxy.clientKeystorePassword.tt1", "test");
      System.setProperty("cibet.proxy.port.tt1", "10115");
      System.setProperty("cibet.proxy.mode.tt1", ProxyMode.MITM.name());
      try {

         Configuration.instance().initialise();

         ProxyConfig config = new ProxyConfig();
         config.setMode(ProxyMode.MITM);
         config.setPort(10112);
         config.setName("p2");
         Configuration.instance().startProxy(config);

         String proxies = Configuration.instance().getProxies();
         Assert.assertTrue(proxies.indexOf("p2") > 0);
         Assert.assertTrue(proxies.indexOf("tt1") > 0);

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

         HttpGet method = createHttpGetExtern("https://httpbin.org/ip");
         // HttpPost method = createHttpPost(getBaseSSLURL());
         HttpResponse response = client.execute(method);
         log.debug("STATUS: " + response.getStatusLine().getStatusCode());
         String msg = readResponseBody(response);
         Assert.assertTrue(msg.startsWith("{  \"origin\": \""));
         String ev = response.getFirstHeader("CIBET_EVENTRESULT").getValue();
         EventResult eventResult = CibetUtil.decodeEventResult(ev);
         log.debug(eventResult);
         Assert.assertEquals(ExecutionStatus.EXECUTED, eventResult.getExecutionStatus());
         Assert.assertEquals("HTTP-PROXY", eventResult.getSensor());
      } finally {
         System.clearProperty("cibet.proxy.mode.tt1");
         System.clearProperty("cibet.proxy.clientKeystore.tt1");
         System.clearProperty("cibet.proxy.clientKeystorePassword.tt1");
         System.clearProperty("cibet.proxy.port.tt1");
      }
   }

   /**
    * like SSL
    * 
    * @throws Exception
    */
   @Test
   public void littleProxyWithClientAuth() throws Exception {
      log.info("start littleProxyWithClientAuth()");

      System.setProperty("cibet.proxy.clientKeystore.p1", "clientKeystore.jks");
      System.setProperty("cibet.proxy.clientKeystorePassword.p1", "test");
      System.setProperty("cibet.proxy.port.p1", "10112");
      System.setProperty("cibet.proxy.mode.p1", ProxyMode.MITM.name());
      try {
         Configuration.instance().initialise();

         ClassLoader loader = Thread.currentThread().getContextClassLoader();
         KeyStore truststore = KeyStore.getInstance("JKS");
         truststore.load(loader.getResourceAsStream("testTruststore.jks"), "test".toCharArray());
         TrustManagerFactory trustManagerFactory = TrustManagerFactory
               .getInstance(TrustManagerFactory.getDefaultAlgorithm());
         trustManagerFactory.init(truststore);

         KeyStore keystore = KeyStore.getInstance("JKS");
         keystore.load(loader.getResourceAsStream("clientKeystore.jks"), "test".toCharArray());
         KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
         keyManagerFactory.init(keystore, "test".toCharArray());

         SSLContext sslContext = SSLContext.getInstance("TLS");
         sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

         SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext,
               SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

         // System.setProperty("java.net.useSystemProxies", "true");
         // System.setProperty("https.proxyPort", "10112");
         // System.setProperty("https.proxyHost", "localhost");
         // System.setProperty("http.proxyPort", "10112");
         // System.setProperty("http.proxyHost", "localhost");
         HttpHost proxy = new HttpHost("localhost", 10112);
         client = HttpClients.custom().setSSLSocketFactory(sslsf).setProxy(proxy).build();
         // client = HttpClients.custom().setSSLSocketFactory(sslsf).useSystemProperties().build();

         // HttpPost method = createHttpPostExtern(getBaseSSLClientURL());
         HttpGet method = createHttpGetExtern("https://httpbin.org/ip");
         log.debug("send");
         HttpResponse response = client.execute(method);
         log.debug("STATUS: " + response.getStatusLine().getStatusCode());
         String msg = readResponseBody(response);
         Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

         Assert.assertTrue(msg.startsWith("{  \"origin\": \""));

         String ev = response.getFirstHeader("CIBET_EVENTRESULT").getValue();
         EventResult eventResult = CibetUtil.decodeEventResult(ev);
         log.debug(eventResult);
         Assert.assertEquals(ExecutionStatus.EXECUTED, eventResult.getExecutionStatus());
         Assert.assertEquals("HTTP-PROXY", eventResult.getSensor());

         method.abort();

         log.info("second run");
         method = createHttpGetExtern("https://httpbin.org/ip");
         response = client.execute(method);
         Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
         msg = readResponseBody(response);
         Assert.assertTrue(msg.startsWith("{  \"origin\": \""));
      } finally {
         System.clearProperty("cibet.proxy.clientKeystore.p1");
         System.clearProperty("cibet.proxy.clientKeystorePassword.p1");
         System.clearProperty("cibet.proxy.port.p1");
         System.clearProperty("cibet.proxy.mode.p1");
      }
   }

   /**
    * clientToProxyRequest: POST http://localhost:8788/LittleProxyTest/...?USER=Willi&amp;TENANT=testTenant HTTP/1.1<br>
    * proxyToServerConnectionQueued<br>
    * proxyToServerRequest: POST http://localhost:8788/LittleProxyTest/...Willi&amp;TENANT=testTenant HTTP/1.1 Via=1.1
    * Chef <br>
    * proxyToServerConnectionStarted<br>
    * proxyToServerConnectionSucceeded: io.netty.channel.DefaultChannelHandlerContext<br>
    * proxyToServerRequestSending<br>
    * proxyToServerRequestSent<br>
    * serverToProxyResponseReceiving<br>
    * serverToProxyResponse: io.netty.handler.codec.http.DefaultHttpResponse<br>
    * proxyToClientResponse: DefaultHttpResponse<br>
    * serverToProxyResponse: io.netty.handler.codec.http.DefaultLastHttpContent<br>
    * proxyToClientResponse DefaultLastHttpContent<br>
    * serverToProxyResponseReceived<br>
    * 
    * @throws Exception
    */
   @Test
   public void littleProxyChainedProxy() throws Exception {
      log.info("start littleProxyChainedProxy()");

      startLittleProxyAsProxy();

      ProxyConfig config = new ProxyConfig();
      config.setMode(ProxyMode.CHAINEDPROXY);
      config.setPort(10112);
      config.setChainedProxyHost("localhost");
      config.setChainedProxyPort(10113);
      config.setName("p2");
      Configuration.instance().startProxy(config);

      HttpHost proxy = new HttpHost("localhost", 10112);
      client = HttpClients.custom().setProxy(proxy).build();

      HttpGet method = createHttpGetExtern("http://httpbin.org/ip");
      HttpResponse response = client.execute(method);
      log.debug("STATUS: " + response.getStatusLine().getStatusCode());
      String msg = readResponseBody(response);
      Assert.assertTrue(msg.startsWith("{  \"origin\": \""));
      String ev = response.getFirstHeader("CIBET_EVENTRESULT").getValue();
      EventResult eventResult = CibetUtil.decodeEventResult(ev);
      log.debug(eventResult);
      Assert.assertEquals(ExecutionStatus.EXECUTED, eventResult.getExecutionStatus());
      Assert.assertEquals("HTTP-PROXY", eventResult.getSensor());
   }

   // @Ignore
   @Test
   public void multipleThreads() throws Exception {
      log.info("start multipleThreads()");
      exceptions.clear();
      ProxyConfig config = new ProxyConfig();
      config.setMode(ProxyMode.MITM);
      config.setPort(10112);
      config.setName("p2");
      Configuration.instance().startProxy(config);

      int nbr = 10;
      int delay = 0;

      List<ThreadExecution> tlist = new ArrayList<ThreadExecution>();
      for (int i = 0; i < nbr; i++) {
         ThreadExecution t = new ThreadExecution("thread-" + i, i);
         tlist.add(t);
      }

      log.info("start threads");
      for (ThreadExecution te : tlist) {
         te.start();
         Thread.sleep(delay);
      }
      Thread.sleep(500);
      log.info("join threads");
      for (ThreadExecution te : tlist) {
         te.join();
      }
      Thread.sleep(100);
      log.info("threads joined");
      Assert.assertEquals(0, exceptions.size());
   }

   /**
    * clientToProxyRequest: POST http://localhost:8788/LittleProxyTest/...?USER=Willi&amp;TENANT=testTenant HTTP/1.1<br>
    * proxyToServerConnectionQueued<br>
    * proxyToServerResolutionStarted<br>
    * proxyToServerResolutionSucceeded<br>
    * proxyToServerRequest: POST http://localhost:8788/LittleProxyTest/...Willi&amp;TENANT=testTenant HTTP/1.1 Via=1.1
    * Chef <br>
    * proxyToServerConnectionStarted<br>
    * proxyToClientResponse: DefaultHttpResponse HTTP/1.1 502 Bad Gateway<br>
    * proxyToServerConnectionFailed<br>
    * 
    * @throws Exception
    */
   @Test
   public void littleProxyNoSSLWrongPort() throws Exception {
      log.info("start littleProxyNoSSLWrongPort()");

      ProxyConfig config = new ProxyConfig();
      config.setMode(ProxyMode.MITM);
      config.setPort(10112);
      config.setName("p2");
      Configuration.instance().startProxy(config);

      HttpHost proxy = new HttpHost("localhost", 10112);
      client = HttpClients.custom().setProxy(proxy).build();

      HttpPost method = createHttpPost("http://localhost:8988/LittleProxyTest");
      HttpResponse response = client.execute(method);
      log.debug("STATUS: " + response.getStatusLine().getStatusCode());
      Assert.assertEquals(HttpResponseStatus.BAD_GATEWAY.code(), response.getStatusLine().getStatusCode());
      String msg = readResponseBody(response);
      log.debug(msg);
      String ev = response.getFirstHeader("CIBET_EVENTRESULT").getValue();
      EventResult eventResult = CibetUtil.decodeEventResult(ev);
      log.debug(eventResult);
      Assert.assertEquals(ExecutionStatus.ERROR, eventResult.getExecutionStatus());
      Assert.assertEquals("HTTP-PROXY", eventResult.getSensor());
   }

   /**
    * clientToProxyRequest: POST http://localhost:8788/LittleProxyTest/...?USER=Willi&amp;TENANT=testTenant HTTP/1.1<br>
    * proxyToServerConnectionQueued<br>
    * proxyToServerResolutionStarted<br>
    * proxyToServerResolutionSucceeded<br>
    * proxyToServerRequest: POST http://localhost:8788/LittleProxyTest/...Willi&amp;TENANT=testTenant HTTP/1.1 Via=1.1
    * Chef <br>
    * proxyToServerConnectionStarted<br>
    * proxyToServerConnectionSucceeded: io.netty.channel.DefaultChannelHandlerContext<br>
    * proxyToServerRequestSending<br>
    * proxyToServerRequestSent<br>
    * serverToProxyResponseReceiving<br>
    * serverToProxyResponse: io.netty.handler.codec.http.DefaultHttpResponse HTTP/1.1 404 Not Found<br>
    * proxyToClientResponse: DefaultHttpResponse HTTP/1.1 404 Not Found<br>
    * serverToProxyResponse: io.netty.handler.codec.http.DefaultHttpContent<br>
    * proxyToClientResponse DefaultHttpContent<br>
    * serverToProxyResponse: io.netty.handler.codec.http.DefaultLastHttpContent<br>
    * proxyToClientResponse DefaultLastHttpContent<br>
    * serverToProxyResponseReceived<br>
    * 
    * @throws Exception
    */
   @Test
   public void littleProxyNoSSLWrongContext() throws Exception {
      log.info("start littleProxyNoSSLWrongContext()");

      ProxyConfig config = new ProxyConfig();
      config.setMode(ProxyMode.CHAINEDPROXY);
      config.setPort(10112);
      config.setName("p2");
      Configuration.instance().startProxy(config);

      HttpHost proxy = new HttpHost("localhost", 10112);
      client = HttpClients.custom().setProxy(proxy).build();

      HttpGet method = createHttpGetExtern("http://httpbin.org/xxxx");
      HttpResponse response = client.execute(method);
      log.debug("STATUS: " + response.getStatusLine().getStatusCode());
      Assert.assertEquals(HttpResponseStatus.NOT_FOUND.code(), response.getStatusLine().getStatusCode());
      String msg = readResponseBody(response);
      log.debug(msg);
      String ev = response.getFirstHeader("CIBET_EVENTRESULT").getValue();
      EventResult eventResult = CibetUtil.decodeEventResult(ev);
      log.debug(eventResult);
      Assert.assertEquals(ExecutionStatus.ERROR, eventResult.getExecutionStatus());
      Assert.assertEquals("HTTP-PROXY", eventResult.getSensor());
   }

   /**
    * clientToProxyRequest: CONNECT localhost:8743 HTTP/1.1<br>
    * proxyToServerConnectionQueued<br>
    * proxyToServerResolutionStarted<br>
    * proxyToServerResolutionSucceeded<br>
    * proxyToServerRequest: CONNECT localhost:8743 HTTP/1.1 Via=1.1 Chef<br>
    * proxyToServerConnectionStarted<br>
    * proxyToClientResponse: DefaultHttpResponse HTTP/1.1 502 Bad Gateway<br>
    * proxyToServerConnectionFailed<br>
    * 
    * @throws Exception
    */
   @Test
   public void littleProxySSLWrongPort() throws Exception {
      log.info("start littleProxySSLWrongPort()");
      ProxyConfig config = new ProxyConfig();
      config.setMode(ProxyMode.MITM);
      config.setPort(10112);
      config.setName("p2");
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

      HttpGet method = createHttpGetExtern("https://httpbin.org:8081/ip");
      // HttpPost method = createHttpPost("https://localhost:8943/LittleProxyTest");
      HttpResponse response = client.execute(method);
      log.debug("STATUS: " + response.getStatusLine().getStatusCode());
      Assert.assertEquals(HttpResponseStatus.GATEWAY_TIMEOUT.code(), response.getStatusLine().getStatusCode());
      readResponseBody(response);
      String ev = response.getFirstHeader("CIBET_EVENTRESULT").getValue();
      EventResult eventResult = CibetUtil.decodeEventResult(ev);
      log.debug(eventResult);
      Assert.assertEquals(ExecutionStatus.ERROR, eventResult.getExecutionStatus());
      Assert.assertEquals("HTTP-PROXY", eventResult.getSensor());
   }

   /**
    * clientToProxyRequest: CONNECT localhost:8743 HTTP/1.1<br>
    * proxyToServerConnectionQueued<br>
    * proxyToServerResolutionStarted<br>
    * proxyToServerResolutionSucceeded<br>
    * proxyToServerRequest: CONNECT localhost:8743 HTTP/1.1 Via=1.1 Chef<br>
    * proxyToServerConnectionStarted<br>
    * proxyToServerConnectionSSLHandshakeStarted<br>
    * proxyToServerConnectionSucceeded: io.netty.channel.DefaultChannelHandlerContext<br>
    * clientToProxyRequest: POST /LittleProxyTest/test/setuser?USER=Willi&amp;TENANT=testTenant HTTP/1.1<br>
    * proxyToServerRequest: POST /LittleProxyTest/test/setuser?USER=Willi&amp;TENANT=testTenant HTTP/1.1 Via=1.1 Chef
    * <br>
    * proxyToServerRequestSending<br>
    * proxyToServerRequestSent<br>
    * serverToProxyResponseReceiving<br>
    * serverToProxyResponse: io.netty.handler.codec.http.DefaultHttpResponse HTTP/1.1 404 Not Found<br>
    * proxyToClientResponse: DefaultHttpResponse HTTP/1.1 404 Not Found<br>
    * serverToProxyResponse: io.netty.handler.codec.http.DefaultLastHttpContent<br>
    * proxyToClientResponse DefaultLastHttpContent<br>
    * serverToProxyResponseReceived<br>
    * 
    * ProxyMode.CHAINEDPROXY: no requestPre, requestPost, responsePre, responsePost
    * 
    * @throws Exception
    */
   @Test
   public void littleProxySSLWrongContext() throws Exception {
      log.info("start littleProxySSLWrongContext()");
      ProxyConfig config = new ProxyConfig();
      config.setMode(ProxyMode.MITM);
      config.setPort(10112);
      config.setName("p2");
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

      // HttpPost method = createHttpPost("https://localhost:8743/NOTEXISTING");
      HttpGet method = createHttpGetExtern("https://httpbin.org/xxxx");
      HttpResponse response = client.execute(method);
      log.debug("STATUS: " + response.getStatusLine().getStatusCode());
      Assert.assertEquals(HttpResponseStatus.NOT_FOUND.code(), response.getStatusLine().getStatusCode());
      readResponseBody(response);
      String ev = response.getFirstHeader("CIBET_EVENTRESULT").getValue();
      EventResult eventResult = CibetUtil.decodeEventResult(ev);
      log.debug(eventResult);
      Assert.assertEquals(ExecutionStatus.ERROR, eventResult.getExecutionStatus());
      Assert.assertEquals("HTTP-PROXY", eventResult.getSensor());
   }

   /**
    * clientToProxyRequest: GET http://httpbin.org/delay/4 HTTP/1.1<br>
    * proxyToServerConnectionQueued<br>
    * proxyToServerResolutionStarted<br>
    * proxyToServerResolutionSucceeded<br>
    * proxyToServerRequest: GET /delay/4 HTTP/1.1 Via=1.1 Chef<br>
    * proxyToServerConnectionStarted<br>
    * proxyToServerConnectionSucceeded: io.netty.channel.DefaultChannelHandlerContext<br>
    * proxyToServerRequestSending<br>
    * proxyToServerRequestSent<br>
    * serverToProxyResponseTimedOut<br>
    * proxyToClientResponse: io.netty.handler.codec.http.DefaultFullHttpResponse HTTP/1.1 504 Gateway Timeout<br>
    * 
    * @throws Exception
    */
   @Test
   public void littleProxyTimeout() throws Exception {
      log.info("start littleProxyTimeout()");
      ProxyConfig config = new ProxyConfig();
      config.setMode(ProxyMode.MITM);
      config.setPort(10112);
      config.setTimeout(2000);
      config.setName("p2");
      Configuration.instance().startProxy(config);

      HttpHost proxy = new HttpHost("localhost", 10112);
      client = HttpClients.custom().setProxy(proxy).disableAutomaticRetries().build();

      HttpGet method = new HttpGet("http://httpbin.org/delay/4");
      HttpResponse response = client.execute(method);
      log.debug("STATUS: " + response.getStatusLine().getStatusCode());
      Assert.assertEquals(HttpResponseStatus.GATEWAY_TIMEOUT.code(), response.getStatusLine().getStatusCode());
      readResponseBody(response);
      String ev = response.getFirstHeader("CIBET_EVENTRESULT").getValue();
      EventResult eventResult = CibetUtil.decodeEventResult(ev);
      log.debug(eventResult);
      Assert.assertEquals(ExecutionStatus.ERROR, eventResult.getExecutionStatus());
      Assert.assertEquals("HTTP-PROXY", eventResult.getSensor());
   }

   /**
    * clientToProxyRequest: POST http://www.notexistingurl/...test/setuser?USER=Willi&amp;TENANT=testTenant HTTP/1.1<br>
    * proxyToServerConnectionQueued<br>
    * proxyToServerResolutionStarted<br>
    * proxyToServerResolutionFailed<br>
    * proxyToClientResponse: DefaultHttpResponse HTTP/1.1 502 Bad Gateway<br>
    * 
    * @throws Exception
    */
   @Test
   public void littleProxyNoSSLWrongURL() throws Exception {
      log.info("start littleProxyNoSSLWrongURL()");

      ProxyConfig config = new ProxyConfig();
      config.setMode(ProxyMode.MITM);
      config.setPort(10112);
      config.setName("p2");
      Configuration.instance().startProxy(config);

      HttpHost proxy = new HttpHost("localhost", 10112);
      client = HttpClients.custom().setProxy(proxy).build();

      HttpPost method = createHttpPost("http://www.notExistingURL/LittleProxyTest");
      HttpResponse response = client.execute(method);
      log.debug("STATUS: " + response.getStatusLine().getStatusCode());
      Assert.assertEquals(HttpResponseStatus.BAD_GATEWAY.code(), response.getStatusLine().getStatusCode());
      String msg = readResponseBody(response);
      log.debug(msg);
      String ev = response.getFirstHeader("CIBET_EVENTRESULT").getValue();
      EventResult eventResult = CibetUtil.decodeEventResult(ev);
      log.debug(eventResult);
      Assert.assertEquals(ExecutionStatus.ERROR, eventResult.getExecutionStatus());
      Assert.assertEquals("HTTP-PROXY", eventResult.getSensor());
   }

   /**
    * clientToProxyRequest: POST http://10.255.255.1/LittleProxyTest/test/setuser?USER=Willi&amp;TENANT=testTenant
    * HTTP/1.1 <br>
    * proxyToServerConnectionQueued<br>
    * proxyToServerResolutionStarted<br>
    * proxyToServerResolutionSucceeded<br>
    * proxyToServerRequest: POST /LittleProxyTest/test/setuser?USER=Willi&amp;TENANT=testTenant HTTP/1.1 Via=1.1 Chef
    * <br>
    * proxyToServerConnectionStarted<br>
    * serverToProxyResponseTimedOut<br>
    * proxyToClientResponse: DefaultFullHttpResponse HTTP/1.1 504 Gateway Timeout<br>
    * 
    * @throws Exception
    */
   @Ignore
   @Test
   public void littleProxyFailedConnect() throws Exception {
      log.info("start littleProxyFailedConnect()");

      ProxyConfig config = new ProxyConfig();
      config.setMode(ProxyMode.MITM);
      config.setPort(10112);
      config.setName("p2");
      Configuration.instance().startProxy(config);

      HttpHost proxy = new HttpHost("localhost", 10112);
      client = HttpClients.custom().setProxy(proxy).build();

      HttpPost method = createHttpPost("http://10.255.255.1/LittleProxyTest");
      HttpResponse response = client.execute(method);
      log.debug("STATUS: " + response.getStatusLine().getStatusCode());
      Assert.assertEquals(HttpResponseStatus.GATEWAY_TIMEOUT.code(), response.getStatusLine().getStatusCode());
      String msg = readResponseBody(response);
      log.debug(msg);
      String ev = response.getFirstHeader("CIBET_EVENTRESULT").getValue();
      EventResult eventResult = CibetUtil.decodeEventResult(ev);
      log.debug(eventResult);
      Assert.assertEquals(ExecutionStatus.ERROR, eventResult.getExecutionStatus());
      Assert.assertEquals("HTTP-PROXY", eventResult.getSensor());
   }

}
