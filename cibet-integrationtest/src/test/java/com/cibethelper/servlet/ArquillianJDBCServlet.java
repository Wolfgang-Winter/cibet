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

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;

import com.cibethelper.ejb.OutService;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.context.Context;

public class ArquillianJDBCServlet extends HttpServlet {

   private static Logger log = Logger.getLogger(ArquillianJDBCServlet.class);
   /**
    * 
    */
   private static final long serialVersionUID = -2876846209111174152L;

   @Resource(name = "jdbc/Application")
   private DataSource appDataSource;

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
      log.info("call Arquillian1Servlet");

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

      try {
         if (req.getRequestURI().endsWith("login.cibet")) {
            login(req, resp);
         } else if (req.getRequestURI().endsWith("persist.cibet")) {
            persist(resp);
         } else if (req.getRequestURI().endsWith("logout.cibet")) {
            logout(req, resp);
         } else if (req.getRequestURI().endsWith("showSession.cibet")) {
            showSession(req, resp);
         } else if (req.getRequestURI().endsWith("loginShiro.cibet")) {
            loginShiro(req, resp);
         } else if (req.getRequestURI().endsWith("logThis.cibet")) {
            logThis(req, resp);
         } else if (req.getRequestURI().endsWith("logThis.url")) {
            logThis(req, resp);
         } else {
            String msg = "ERROR: no functionality found!!";
            PrintWriter writer = resp.getWriter();
            writer.print("ee done: " + msg);
            writer.close();
         }

      } catch (Exception e) {
         log.error(e.getMessage(), e);
         throw new ServletException(e);
      }
   }

   private void logThis(HttpServletRequest req, HttpServletResponse resp) throws Exception {
      OutService service = new OutService();
      String message = req.getParameter("MSG");
      String answer = service.logThis(message);
      PrintWriter writer = resp.getWriter();
      writer.print("Answer: " + answer);
      writer.close();
   }

   private void persist(HttpServletResponse resp) throws Exception {
      Connection con = appDataSource.getConnection();
      log.debug("SQL connection created: " + con);
      con.setAutoCommit(false);
      PreparedStatement st = con
            .prepareStatement("INSERT INTO cib_testentity (nameValue,counter,userid,owner) VALUES (?,?,?,?)");
      st.setString(1, "arq-JDBC-EM-Provider");
      st.setInt(2, 45);
      st.setString(3, Context.sessionScope().getUser());
      st.setString(4, "Hindiman");
      int count = st.executeUpdate();
      log.debug("after persist count=" + count);

      con.commit();
      con.close();

      log.info("TEntity persisted");
      PrintWriter writer = resp.getWriter();
      writer.print("TEntity persist");
      writer.close();
   }

   private void login(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      log.debug("login to session: " + req.getSession().getId());
      String user = req.getParameter("USER");
      String sec = req.getParameter("second");

      if (req.getParameter("tenant") != null) {
         Context.sessionScope().setTenant(req.getParameter("tenant"));
      }

      String response;
      if (sec == null) {
         Context.sessionScope().setUser(user);
         response = "Login done for user " + user;
      } else {
         Context.sessionScope().setSecondUser(user);
         response = "second Login done for user " + user;
      }

      Context.sessionScope().setProperty("Freds", new TEntity("freds name", 4, "freds owner"));

      log.debug("logged in user " + Context.sessionScope().getUser());
      PrintWriter writer = resp.getWriter();
      writer.print(response);
      writer.close();
   }

   private void loginShiro(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      log.debug("Shiro login to session: " + req.getSession().getId());

      try {
         Subject currentUser = SecurityUtils.getSubject();
         UsernamePasswordToken token = new UsernamePasswordToken(req.getParameter("USER"), "passwd");
         token.setRememberMe(true);
         currentUser.login(token);

         if (req.getParameter("Freds") != null) {
            Context.sessionScope().setProperty("Freds", new TEntity("freds name", 8, "freds owner"));
         }

         if (req.getParameter("tenant") != null) {
            Context.sessionScope().setTenant(req.getParameter("tenant"));
         }

      } catch (UnknownAccountException e) {
         // username wasn't in the system, show them an error message?
         log.error("Authentication failed: " + e.getMessage());
         throw new ServletException(e);
      } catch (IncorrectCredentialsException e) {
         // password didn't match, try again?
         log.error("Authentication failed: " + e.getMessage());
         throw new ServletException(e);

      } catch (LockedAccountException e) {
         // account for that username is locked - can't login. Show them a
         // message?
         log.error("Authentication failed: " + e.getMessage());
         throw new ServletException(e);
      } catch (AuthenticationException e) {
         // unexpected condition - error?
         log.error("Authentication failed: " + e.getMessage());
         throw new ServletException(e);
      }

      log.debug("logged in user " + Context.sessionScope().getUser());
      PrintWriter writer = resp.getWriter();
      writer.print("Login done for user " + Context.sessionScope().getUser());
      writer.close();
   }

   private void showSession(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      log.debug("show session: " + req.getSession().getId());
      log.debug("User: " + Context.sessionScope().getUser());
      TEntity te = (TEntity) Context.sessionScope().getProperty("Freds");

      StringBuffer b = new StringBuffer();
      b.append("User: " + Context.sessionScope().getUser());
      b.append("; Tenant: " + Context.sessionScope().getTenant() + "; ");
      b.append(te == null ? " -" : te.getNameValue());
      b.append(te == null ? " -" : te.getCounter());

      PrintWriter writer = resp.getWriter();
      writer.print("after Login: " + b);
      writer.close();
   }

   private void logout(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      log.debug("User: " + Context.sessionScope().getUser());
      Subject currentUser = SecurityUtils.getSubject();
      currentUser.logout();

      log.debug("invalidate session: " + req.getSession().getId());
      req.getSession().invalidate();

      PrintWriter writer = resp.getWriter();
      writer.print("Logout done");
      writer.close();
   }

   /*
    * (non-Javadoc)
    * 
    * @see javax.servlet.GenericServlet#init()
    */
   @Override
   public void init() throws ServletException {
      super.init();
      log.debug("appDataSource: " + appDataSource);
   }

}
