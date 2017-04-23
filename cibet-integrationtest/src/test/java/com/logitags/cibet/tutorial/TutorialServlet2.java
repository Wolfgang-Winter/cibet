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
package com.logitags.cibet.tutorial;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Resource;
import javax.persistence.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.log4j.Logger;

import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.archive.ArchiveLoader;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.actuator.lock.LockedObject;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.pojo.CibetIntercept;

public class TutorialServlet2 extends HttpServlet {

   private static Logger log = Logger.getLogger(TutorialServlet2.class);
   /**
    * 
    */
   private static final long serialVersionUID = -2876846209111174152L;

   @Resource
   private UserTransaction ut;

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
      log.info("------------- call " + req.getRequestURI() + " ---------------");

      try {
         if (req.getRequestURI().endsWith("call")) {
            call(req, resp);
         } else if (req.getRequestURI().endsWith("loadArchive")) {
            loadArchive(req, resp);
         } else if (req.getRequestURI().endsWith("changeConfig")) {
            ut.begin();
            String info = changeConfig(req.getParameter("param"));
            ut.commit();
            PrintWriter writer = resp.getWriter();
            writer.print(req.getRequestURI() + " request executed with response info " + info);
            writer.close();
         } else if (req.getRequestURI().endsWith("secured")) {
            ut.begin();
            String info = secured(req.getParameter("param"));
            ut.commit();
            PrintWriter writer = resp.getWriter();
            writer.print(req.getRequestURI() + " request executed with response info " + info);
            writer.close();
         } else if (req.getRequestURI().endsWith("loadDc")) {
            loadDc(req, resp);
         } else if (req.getRequestURI().endsWith("simpleLogin")) {
            simpleLogin(req, resp);
         } else if (req.getRequestURI().endsWith("release")) {
            release(req, resp);
         } else if (req.getRequestURI().endsWith("releaseHttp")) {
            releaseHttp(req, resp);
         } else if (req.getRequestURI().endsWith("clean")) {
            clean(req, resp);
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

   private void clean(HttpServletRequest req, HttpServletResponse resp) throws Exception {
      log.debug("TutorialServlet:clean()");

      ut.begin();

      Query q3 = Context.requestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL);
      List<Archive> alist = q3.getResultList();
      for (Archive ar : alist) {
         Context.requestScope().getEntityManager().remove(ar);
      }

      Query q4 = Context.requestScope().getEntityManager().createQuery("select d from DcControllable d");
      List<DcControllable> dclist = q4.getResultList();
      for (DcControllable dc : dclist) {
         Context.requestScope().getEntityManager().remove(dc);
      }

      Query q5 = Context.requestScope().getEntityManager().createQuery("SELECT a FROM LockedObject a");
      Iterator<LockedObject> itLO = q5.getResultList().iterator();
      while (itLO.hasNext()) {
         Context.requestScope().getEntityManager().remove(itLO.next());
      }

      Query q6 = Context.requestScope().getEntityManager().createQuery("SELECT a FROM EventResult a");
      Iterator<EventResult> itEV = q6.getResultList().iterator();
      while (itEV.hasNext()) {
         Context.requestScope().getEntityManager().remove(itEV.next());
      }

      ut.commit();
   }

   private void call(HttpServletRequest req, HttpServletResponse resp) throws Exception {
      String param = req.getParameter("param");
      String info = this.getClass().getSimpleName() + " Servlet called with param " + param;
      log.info(info);
      PrintWriter writer = resp.getWriter();
      writer.print(info);
      writer.close();
   }

   private void loadArchive(HttpServletRequest req, HttpServletResponse resp) throws Exception {
      int expected = Integer.valueOf(req.getParameter("expected"));
      List<Archive> archives = ArchiveLoader.loadArchives("http://localhost:8788/Tutorial2/call");
      if (archives.size() != expected) {
         throw new Exception("Archive list size is not " + expected + " but " + archives.size());
      }

      String response = "no Archive found";
      if (expected > 0) {
         response = "http parameters:\n";
         for (ResourceParameter rp : archives.get(0).getResource().getParameters()) {
            response = response + rp + "\n";
         }
         log.info(response);
         response = response.toString().replaceAll("\n", " ; ");
      }
      PrintWriter writer = resp.getWriter();
      writer.print(response);
      writer.close();
   }

   /**
    * display login screen for user/password. Authenticate user and put him into session
    * 
    * @param req
    * @param resp
    * @throws IOException
    */
   private void simpleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      req.getSession().invalidate();
      String user = req.getParameter("user");
      String info = "user " + user + " is logged in";
      log.info(info);
      req.getSession().setAttribute("CIBET_USER", user);
      PrintWriter writer = resp.getWriter();
      writer.print(info);
      writer.close();
   }

   /**
    * CibetIntercept annotation requires that method is public
    * 
    * @param param
    */
   @CibetIntercept
   public String changeConfig(String param) {
      String info = "method " + this.getClass().getSimpleName() + ".changeConfig() called with parameter " + param;
      log.info(info);
      return info;
   }

   private void release(HttpServletRequest req, HttpServletResponse resp) throws Exception {
      List<DcControllable> dcList = DcLoader.findUnreleased();
      String info = (String) dcList.get(0).release("now method is called");
      PrintWriter writer = resp.getWriter();
      writer.print(info);
      writer.close();
   }

   private void releaseHttp(HttpServletRequest req, HttpServletResponse resp) throws Exception {
      List<DcControllable> dcList = DcLoader.findUnreleased();
      HttpResponse response = (HttpResponse) dcList.get(0).release("now method is called");

      PrintWriter writer = resp.getWriter();
      writer.print(readResponseBody(response));
      writer.close();
   }

   private void loadDc(HttpServletRequest req, HttpServletResponse resp) throws Exception {
      int expected = Integer.valueOf(req.getParameter("expected"));
      List<DcControllable> dcList = DcLoader.findUnreleased();
      if (dcList.size() != expected) {
         throw new Exception("DcControllable list size is not " + expected + " but " + dcList.size());
      }

      String response = "no DcControllable found";
      if (expected > 0) {
         response = dcList.get(0).toString();
         log.info(response);
         response = response.toString().replaceAll("\n", " ; ");
      }
      PrintWriter writer = resp.getWriter();
      writer.print(response);
      writer.close();
   }

   private String secured(String param) {
      String info = "method " + this.getClass().getSimpleName() + ".secured() called with parameter " + param;
      log.info(info);
      return info;
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

}
