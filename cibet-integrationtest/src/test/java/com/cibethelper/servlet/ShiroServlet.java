/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 *******************************************************************************
 */
package com.cibethelper.servlet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;

import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.shiro.ShiroService;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalSessionScope;

public class ShiroServlet extends HttpServlet {

   private static Logger log = Logger.getLogger(ShiroServlet.class);
   /**
    * 
    */
   private static final long serialVersionUID = -2876846209111174152L;

   private static javax.naming.Context context;

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
    * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest ,
    * javax.servlet.http.HttpServletResponse)
    */
   @Override
   protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

      log.info("call TestServlet");

      log.debug("contxtPath: " + req.getContextPath());
      log.debug("pathInfo: " + req.getPathInfo());
      log.debug("remoteUser: " + req.getRemoteUser());
      log.debug("requestURI: " + req.getRequestURI());
      log.debug("requestURL: " + req.getRequestURL());
      log.debug("querystring: " + req.getQueryString());
      log.debug("servletPath: " + req.getServletPath());
      log.debug("localAddr: " + req.getLocalAddr());
      log.debug("localName: " + req.getLocalName());
      log.debug("localPort: " + req.getLocalPort());
      log.debug("remoteAddr: " + req.getRemoteAddr());
      log.debug("remoteHost: " + req.getRemoteHost());
      log.debug("remoteport: " + req.getRemotePort());
      log.debug("servername: " + req.getServerName());
      log.debug("serverport: " + req.getServerPort());

      if (req.getServletPath().equals("/testInvoke")) {
         doTestInvoke(req, resp);
      } else if (req.getServletPath().equals("/loginShiro")) {
         loginShiro(req, resp);
      } else if (req.getServletPath().equals("/login")) {
         login(req, resp);
      } else if (req.getServletPath().equals("/afterLogin")) {
         afterLogin(req, resp);
      } else if (req.getServletPath().equals("/logout")) {
         logout(req, resp);
      } else if (req.getServletPath().equals("/persist")) {
         persist(req, resp);
      } else if (req.getServletPath().equals("/persist2")) {
         persist2(req, resp);

      } else if (req.getServletPath().equals("/ts/shiro1")) {
         shiro1(req, resp);
      } else if (req.getServletPath().equals("/ts/shiroSecond")) {
         shiroSecond(req, resp);
      } else if (req.getServletPath().equals("/ts/shiro8")) {
         PrintWriter writer = resp.getWriter();
         writer.print("SHIRO8 back");
         writer.close();
      } else if (req.getServletPath().equals("/ts/shiro9")) {
         PrintWriter writer = resp.getWriter();
         writer.print("SHIRO9 back");
         writer.close();
      } else if (req.getServletPath().equals("/notif")) {
         doNotification(req, resp);
      } else if (req.getServletPath().equals("/ts/exception")) {
         log.debug("throw Exception");
         throw new ServletException("deliberately thrown exception!");
      } else {
         log.debug("User: " + Context.sessionScope().getUser());
         StringBuffer sb = new StringBuffer();
         for (Object key : req.getParameterMap().keySet()) {
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

         String rollback = req.getParameter("rollback");
         if (rollback != null && "true".equals(rollback)) {
            Context.requestScope().setRollbackOnly(true);
            log.debug("set rollbackOnly to " + Context.requestScope().getRollbackOnly());
         }

         // body
         sb.append(",body=");
         InputStream in = req.getInputStream();
         log.debug("available body bytes: " + in.available());
         log.debug("content-length: " + req.getContentLength());

         ByteArrayOutputStream bos = new ByteArrayOutputStream();

         int i = in.read();
         while (i != -1) {
            bos.write(i);
            i = in.read();
         }

         byte[] bytes = bos.toByteArray();

         log.debug("actual bytes read: " + bytes.length);
         log.debug("bytes: " + Arrays.toString(bytes));
         String s = new String(bytes, "UTF-8");
         log.debug("s=" + s);
         sb.append(s);

         in.close();
         log.debug(sb);

         PrintWriter writer = resp.getWriter();
         writer.print("Hallo here we are: " + sb.toString());
         writer.close();
      }
   }

   protected void persist(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
   }

   protected void persist2(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
   }

   private String loginShiro(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      log.debug("Shiro login to session: " + req.getSession().getId());

      Subject subject = SecurityUtils.getSubject();
      String token = req.getParameter("shiroUser");
      int index = token.indexOf(":");
      String user = token.substring(0, index);
      String password = token.substring(index + 1);
      log.info("authenticate Shiro: " + user);

      AuthenticationToken auth = new UsernamePasswordToken(user, password);
      subject.login(auth);

      if (req.getParameter("TENANT") != null) {
         Context.sessionScope().setTenant(req.getParameter("TENANT"));
      }

      String msg = "Shiro logged in user " + Context.sessionScope().getUser();
      log.debug(msg);
      return msg;
   }

   private void doNotification(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      log.info("doNotification called");
      StringBuffer b = new StringBuffer();
      Enumeration<String> names = req.getParameterNames();
      while (names.hasMoreElements()) {
         String name = names.nextElement();
         b.append(name);
         b.append("=");
         b.append(req.getParameter(name));
         b.append(";");
      }
      log.info(b.toString());

      String tempDir = System.getProperty("java.io.tmpdir");
      if (!tempDir.endsWith(File.separator)) {
         tempDir = tempDir + File.separator;
      }

      File file = new File(tempDir + "httpNotification.tmp");
      FileWriter out = new FileWriter(file);
      out.write(b.toString());
      out.close();
      log.info("write file to " + tempDir + "httpNotification.tmp");

      PrintWriter writer = resp.getWriter();
      writer.print("Thanks for notification: " + b.toString());
      writer.close();
   }

   private void shiroSecond(HttpServletRequest req, HttpServletResponse resp) {
      UsernamePasswordToken tok = new UsernamePasswordToken("darkhelmet", "ludicrousspeed");
      ShiroService.logonSecondUser(tok);
   }

   private void shiro1(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      Subject su = SecurityUtils.getSubject();
      log.info("isAuthenticated: " + su.isAuthenticated());
      log.info("name: " + su.getPrincipal());
      log.info("SecondUser: " + Context.sessionScope().getSecondUser());
      log.info(
            "Second Principal: " + Context.internalSessionScope().getProperty(InternalSessionScope.SECOND_PRINCIPAL));
      String secPrincipal = "";
      if (Context.internalSessionScope().getProperty(InternalSessionScope.SECOND_PRINCIPAL) != null) {
         Subject sec = (Subject) Context.internalSessionScope().getProperty(InternalSessionScope.SECOND_PRINCIPAL);
         secPrincipal = (String) sec.getPrincipal();
         log.info("sec isAuthenticated: " + sec.isAuthenticated());
      }

      String sb = su.isAuthenticated() + " " + su.getPrincipal() + " | " + Context.sessionScope().getSecondUser() + " "
            + secPrincipal;

      PrintWriter writer = resp.getWriter();
      writer.print("SHIRO: " + sb);
      writer.close();
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

   private void login(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      log.debug("login to session: " + req.getSession());
      log.debug("login to session id: " + req.getSession().getId());
      log.debug("User: " + Context.sessionScope().getUser());

      Context.sessionScope().setUser("Fred2");
      Context.sessionScope().setTenant("Freds tenant");
      Context.sessionScope().setProperty("Freds", new TEntity("freds name", 4, "freds owner"));

      PrintWriter writer = resp.getWriter();
      writer.print("Login done");
      writer.close();
   }

   private void afterLogin(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      log.debug("after login to session: " + req.getSession().getId());
      log.debug("User: " + Context.sessionScope().getUser());
      TEntity te = (TEntity) Context.sessionScope().getProperty("Freds");

      StringBuffer b = new StringBuffer();
      b.append(Context.sessionScope().getUser());
      b.append(Context.sessionScope().getTenant());
      b.append(te == null ? "-" : te.getNameValue());
      b.append(te == null ? "-" : te.getCounter());

      PrintWriter writer = resp.getWriter();
      writer.print("after Login: " + b);
      writer.close();
   }

   protected void logout(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      log.debug("invalidate session: " + req.getSession().getId());
      log.debug("User: " + Context.sessionScope().getUser());
      SecurityUtils.getSubject().logout();
      req.getSession().invalidate();
      Context.internalRequestScope().clear();
      Context.internalSessionScope().clear();

      PrintWriter writer = resp.getWriter();
      writer.print("Logout done");
      writer.close();
   }

   /*
    * (non-Javadoc)
    * 
    * @see javax.servlet.GenericServlet#destroy()
    */
   @Override
   public void destroy() {
      log.debug("undeploy()");
   }

   public static javax.naming.Context getInitialContext() {
      if (context == null) {
         log.info("initialise InitialContext");
         Properties properties = new Properties();
         InputStream in = Thread.currentThread().getContextClassLoader()
               .getResourceAsStream("jndi-for-openejb.properties");

         log.debug("in = " + in);
         if (in != null) {
            try {
               properties.load(in);
               for (Object key : properties.keySet()) {
                  log.debug(key + "=" + properties.get(key));
               }

            } catch (IOException e) {
               log.error(e.getMessage(), e);
               throw new RuntimeException(e);
            }
         }

         try {
            context = new InitialContext(properties);
            // context = new InitialContext();
            Hashtable<?, ?> props = context.getEnvironment();
            for (Object key : props.keySet()) {
               log.debug(key + "=" + props.get(key));
            }

         } catch (NamingException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
         }
      }
      return context;
   }

   /*
    * (non-Javadoc)
    * 
    * @see javax.servlet.http.HttpServlet#doDelete(javax.servlet.http.HttpServletRequest ,
    * javax.servlet.http.HttpServletResponse)
    */
   @Override
   protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      doPost(req, resp);
   }

   /*
    * (non-Javadoc)
    * 
    * @see javax.servlet.http.HttpServlet#doHead(javax.servlet.http.HttpServletRequest ,
    * javax.servlet.http.HttpServletResponse)
    */
   @Override
   protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      doPost(req, resp);
   }

   /*
    * (non-Javadoc)
    * 
    * @see javax.servlet.http.HttpServlet#doOptions(javax.servlet.http.HttpServletRequest ,
    * javax.servlet.http.HttpServletResponse)
    */
   @Override
   protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      doPost(req, resp);
   }

   /*
    * (non-Javadoc)
    * 
    * @see javax.servlet.http.HttpServlet#doPut(javax.servlet.http.HttpServletRequest ,
    * javax.servlet.http.HttpServletResponse)
    */
   @Override
   protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      doPost(req, resp);
   }

   /*
    * (non-Javadoc)
    * 
    * @see javax.servlet.http.HttpServlet#doTrace(javax.servlet.http.HttpServletRequest ,
    * javax.servlet.http.HttpServletResponse)
    */
   @Override
   protected void doTrace(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      log.info("TRACE called");
      doPost(req, resp);
   }

}
