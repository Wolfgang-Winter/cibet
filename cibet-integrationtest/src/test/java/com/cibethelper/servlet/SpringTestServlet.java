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
package com.cibethelper.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.expression.WebExpressionVoter;

import com.cibethelper.SpringTestAuthenticationManager;
import com.cibethelper.ejb.RemoteEJB;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.springsecurity.SpringSecurityService;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.ProxyConfig;
import com.logitags.cibet.config.ProxyConfig.ProxyMode;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalSessionScope;

public class SpringTestServlet extends HttpServlet {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private static Logger log = Logger.getLogger(SpringTestServlet.class);

   @PersistenceContext(unitName = "APPL-UNIT")
   protected EntityManager appEM;

   @Resource
   protected UserTransaction ut;

   @EJB
   private RemoteEJB ejb;

   private static WebExpressionVoter webVoter;

   /*
    * (non-Javadoc)
    * 
    * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest ,
    * javax.servlet.http.HttpServletResponse)
    */
   @Override
   protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      doPost(req, resp);
   }

   /*
    * (non-Javadoc)
    * 
    * @see javax.servlet.http.HttpServlet#doHead(javax.servlet.http.HttpServletRequest,
    * javax.servlet.http.HttpServletResponse)
    */
   @Override
   protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      doPost(req, resp);
   }

   /*
    * (non-Javadoc)
    * 
    * @see javax.servlet.http.HttpServlet#doPut(javax.servlet.http.HttpServletRequest,
    * javax.servlet.http.HttpServletResponse)
    */
   @Override
   protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      doPost(req, resp);
   }

   /*
    * (non-Javadoc)
    * 
    * @see javax.servlet.http.HttpServlet#doDelete(javax.servlet.http.HttpServletRequest,
    * javax.servlet.http.HttpServletResponse)
    */
   @Override
   protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      doPost(req, resp);
   }

   /*
    * (non-Javadoc)
    * 
    * @see javax.servlet.http.HttpServlet#doOptions(javax.servlet.http.HttpServletRequest,
    * javax.servlet.http.HttpServletResponse)
    */
   @Override
   protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      doPost(req, resp);
   }

   /*
    * (non-Javadoc)
    * 
    * @see javax.servlet.http.HttpServlet#doTrace(javax.servlet.http.HttpServletRequest,
    * javax.servlet.http.HttpServletResponse)
    */
   @Override
   protected void doTrace(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      doPost(req, resp);
   }

   /*
    * (non-Javadoc)
    * 
    * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest ,
    * javax.servlet.http.HttpServletResponse)
    */
   @Override
   protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      log.info("\n----------------------\ncall ArquillianTestServlet1 [" + req.getServletPath()
            + "]\n----------------------");

      String msg = "OK message: " + concatParameters(req);

      if (req.getServletPath().equals("/test/setuser")) {
         msg = setUser(req, resp);
      } else if (req.getServletPath().equals("/test/spring/loginSpring")) {
         msg = loginSpring(req, resp);
      } else if (req.getServletPath().equals("/test/spring/logoffSpring")) {
         logoffSpring(req, resp);
      } else if (req.getServletPath().equals("/test/spring/loginSpringSecond")) {
         msg = loginSpringSecond(req, resp);
      } else if (req.getServletPath().equals("/test/parallel")) {
         msg = parallel(req, resp);
      } else if (req.getServletPath().equals("/test/parallel2")) {
         msg = parallel(req, resp);
      } else if (req.getServletPath().equals("/test/callEjb")) {
         msg = callRemoteEjb(req, resp);
      } else if (req.getServletPath().equals("/test/proxy")) {
         msg = proxy(req, resp);
      } else if (req.getServletPath().equals("/test/timeout")) {
         msg = timeout(req, resp);
      } else if (req.getServletPath().equals("/test/testInvoke")) {
         doTestInvoke(req, resp);
      } else if (req.getServletPath().equals("/test/exception")) {
         log.debug("throw Exception");
         throw new ServletException("deliberately thrown exception!");
      }

      PrintWriter writer = resp.getWriter();
      writer.print(msg);
      writer.close();
   }

   private String timeout(HttpServletRequest req, HttpServletResponse resp) {
      int timeout = Integer.parseInt(req.getParameter("TIMEOUT"));
      try {
         Thread.sleep(timeout);
      } catch (InterruptedException e) {
         log.error(e.getMessage(), e);
      }
      return "timout of " + timeout + " millis";
   }

   private String proxy(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
      ProxyConfig config = new ProxyConfig();
      config.setMode(ProxyMode.MITM);
      config.setPort(10112);
      Configuration.instance().startProxy(config);

      try {
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

         HttpPost method = createHttpPost("https://localhost:8743/LittleProxyTest");
         HttpResponse response = client.execute(method);
         log.debug("STATUS: " + response.getStatusLine().getStatusCode());
         log.debug("Response Headers:");
         Header[] headers = response.getAllHeaders();
         for (Header h : headers) {
            log.debug(h.getName() + "=" + h.getValue());
         }

         readResponseBody(response);
      } catch (Exception e) {
         log.error(e.getMessage(), e);
         throw new ServletException(e);
      }
      return null;
   }

   private void logoffSpring(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      SpringSecurityService service = new SpringSecurityService();
      service.logoffSecondUser();
      SecurityContextHolder.clearContext();
   }

   private String loginSpring(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      log.debug("Spring login to session: " + req.getSession().getId());

      try {
         String user = req.getParameter("USER");
         if (user == null) {
            // SecurityContextHolder.getContext().setAuthentication(null);
         } else {
            Collection<GrantedAuthority> authList = new ArrayList<>();
            authList.add(new GrantedAuthorityImpl(req.getParameter("ROLE")));
            Authentication request = new UsernamePasswordAuthenticationToken(user, "FIXED-PW", authList);
            SecurityContextHolder.getContext().setAuthentication(request);
         }

         if (req.getParameter("TENANT") != null) {
            Context.sessionScope().setTenant(req.getParameter("TENANT"));
         }

         if (req.getParameter("secondUser") != null) {
            log.debug("set secondUser= " + req.getParameter("secondUser"));
            if (req.getParameter("secondUser").equals("NULL")) {
               Context.sessionScope().setSecondUser(null);
            } else {
               Context.sessionScope().setSecondUser(req.getParameter("secondUser"));
            }
         }

         String[] roles2 = req.getParameterValues("secondRole");
         if (roles2 != null) {
            authenticateSecond(roles2);
         }

      } catch (AuthenticationException e) {
         log.error("Authentication failed: " + e.getMessage(), e);
      }

      String msg = "Spring logged in user " + Context.sessionScope().getUser();
      log.debug(msg);
      return msg;
   }

   private String loginSpringSecond(HttpServletRequest req, HttpServletResponse resp)
         throws ServletException, IOException {
      log.debug("Spring (SECOND) login to session: " + req.getSession().getId());

      if (req.getParameter("USER") == null) {
         Context.sessionScope().setSecondUser(null);
      } else {
         Context.sessionScope().setSecondUser(req.getParameter("USER"));
      }

      try {
         SpringTestAuthenticationManager authManager = new SpringTestAuthenticationManager();

         if (req.getParameter("ROLE") == null) {
            log.debug("set secondRole = null");
            Context.internalSessionScope().setProperty(InternalSessionScope.SECOND_PRINCIPAL, null);
         } else {
            authManager.addAuthority(req.getParameter("ROLE"));
         }

         if (req.getParameter("USER") != null) {
            try {
               Authentication request = new UsernamePasswordAuthenticationToken(req.getParameter("USER"),
                     req.getParameter("USER"));
               Authentication result = authManager.authenticate(request);
               Context.internalSessionScope().setProperty(InternalSessionScope.SECOND_PRINCIPAL, result);
            } catch (AuthenticationException e) {
               log.error("Authentication failed: " + e.getMessage());
               Assert.fail();
            }
         }

      } catch (AuthenticationException e) {
         log.error("Authentication failed: " + e.getMessage(), e);
      }

      String msg = "Spring SECOND logged in user " + Context.sessionScope().getSecondUser();
      log.debug(msg);
      return msg;
   }

   private String setUser(HttpServletRequest req, HttpServletResponse resp) {
      String user = req.getParameter("USER");
      String tenant = req.getParameter("TENANT");
      String msg = "set user " + user + ", and tenant " + tenant;
      log.info(msg);
      Context.sessionScope().setUser(user);
      if (tenant != null) {
         Context.sessionScope().setTenant(tenant);
      }
      return msg;
   }

   private StringBuffer concatParameters(HttpServletRequest req) {
      StringBuffer sb = new StringBuffer();
      TreeSet<String> tset = new TreeSet<>(req.getParameterMap().keySet());
      for (String key : tset) {
         sb.append(";");
         sb.append(key);
         sb.append("=");
         String[] values = (String[]) req.getParameterMap().get(key);
         if (values != null && values.length == 1) {
            log.debug("receive parameter " + key + " value: " + values[0]);
            sb.append(values[0]);
         } else {
            boolean first = true;
            StringBuffer b = new StringBuffer();
            for (String value : values) {
               if (!first) {
                  b.append("|||");
               } else {
                  first = false;
               }
               b.append(value);
            }
            log.debug("receive multiparameter " + key + " value: " + b.toString());
            sb.append(b.toString());
         }
      }
      return sb;
   }

   private String parallel(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      TEntity te = new TEntity("Hansi name", 34, "hansis owner");
      te.setCounter(Integer.parseInt(req.getParameter("counter")) + 1);

      if (!Context.requestScope().isPostponed()) {
         log.debug("store " + te);

         try {
            ut.begin();
            appEM.persist(te);
            ut.commit();
            return "Persist done, counter: " + te.getCounter();
         } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IOException(e);
         }

      } else {
         log.debug("no store " + te);
         return "NO Persist, counter: " + te.getCounter();
      }
   }

   private void doTestInvoke(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      log.debug("servlet testInvoke called");
      Map<String, String> params = new TreeMap<String, String>();

      for (Object key : req.getParameterMap().keySet()) {
         String[] values = (String[]) req.getParameterMap().get(key);
         if (values != null && values.length == 1) {
            log.debug("parameter: " + key + "=" + values[0]);
            params.put((String) key, values[0]);

         } else {
            log.debug("add multiparameter " + key);
            String valBuffer = "";
            for (String value : values) {
               valBuffer = valBuffer + value;
               valBuffer = valBuffer + "|";
            }
            params.put((String) key, valBuffer);
         }
      }

      StringBuffer b = new StringBuffer();
      for (String key : params.keySet()) {
         b.append(key);
         b.append("=");
         b.append(params.get(key));
         b.append(" ; ");
      }

      // headers
      b.append("HEADERS: ");
      Enumeration<String> headers = req.getHeaderNames();
      while (headers.hasMoreElements()) {
         String headerName = headers.nextElement();
         Enumeration<String> header = req.getHeaders(headerName);
         List<String> headerValues = new ArrayList<String>();
         while (header.hasMoreElements()) {
            headerValues.add(header.nextElement());
         }
         if (headerValues.size() == 1) {
            b.append(headerName);
            b.append(" = ");
            b.append(headerValues.get(0));
            b.append(" ; ");
         } else {
            for (String v : headerValues) {
               b.append(headerName);
               b.append(" = ");
               b.append(v);
               b.append(" ; ");
            }
         }
      }

      log.debug(b);
      PrintWriter writer = resp.getWriter();
      writer.print("TestInvoke done: " + b);
      writer.close();
   }

   private String callRemoteEjb(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      log.debug("persist first TEntity");
      TEntity te = new TEntity("�sal", 372, "loster");
      ejb.persist(te);
      log.debug("persist second TEntity");
      TEntity te2 = new TEntity("�sal2", 3722, "loster2");
      ejb.persist(te2);
      return "OKAY";
   }

   private HttpPost createHttpPost(String baseURL) throws Exception {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      InputStream stream = loader.getResourceAsStream("cibet-config.xml");
      String in = IOUtils.toString(stream, "UTF-8");

      List<NameValuePair> formparams = new ArrayList<NameValuePair>();
      formparams.add(new BasicNameValuePair("act", "aschenfels"));
      formparams.add(new BasicNameValuePair("dubi2", "Klassenmann"));
      formparams.add(new BasicNameValuePair("longText", in));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
      entity.setChunked(true);
      HttpPost method = new HttpPost(baseURL + "/test/setuser?USER=Willi&TENANT=TENANT");
      method.setEntity(entity);
      method.addHeader("cibettestheader", "xxxxxxxxxxxxxx");
      return method;
   }

   protected String readResponseBody(HttpResponse response) throws Exception {
      // Read the response body.
      HttpEntity entity = response.getEntity();
      InputStream instream = null;
      try {
         if (entity != null) {
            instream = entity.getContent();

            BufferedReader reader = new BufferedReader(new InputStreamReader(instream));
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

   private void authenticate(String[] roles) {
      log.debug("authenticate");
      SpringTestAuthenticationManager authManager = new SpringTestAuthenticationManager();
      for (String role : roles) {
         authManager.addAuthority(role);
      }

      try {
         Authentication request = new UsernamePasswordAuthenticationToken("test", "test");
         Authentication result = authManager.authenticate(request);
         SecurityContextHolder.getContext().setAuthentication(result);
      } catch (AuthenticationException e) {
         log.error("Authentication failed: " + e.getMessage(), e);
         throw new RuntimeException(e);
      }
   }

   private void authenticateSecond(String[] roles) {
      log.debug("authenticate second");
      SpringTestAuthenticationManager authManager = new SpringTestAuthenticationManager();
      for (String role : roles) {
         if ("NULL".equals(role)) {
            log.debug("set secondRole = null");
            Context.internalSessionScope().setProperty(InternalSessionScope.SECOND_PRINCIPAL, null);
         } else {
            authManager.addAuthority(role);
         }
      }

      try {
         Authentication request = new UsernamePasswordAuthenticationToken("test", "test");
         Authentication result = authManager.authenticate(request);
         Context.internalSessionScope().setProperty(InternalSessionScope.SECOND_PRINCIPAL, result);
      } catch (AuthenticationException e) {
         log.error("Authentication failed: " + e.getMessage());
         Assert.fail();
      }
   }

}
