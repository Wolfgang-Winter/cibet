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
package com.logitags.jmeter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Random;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

@WebServlet("/")
public class JMeterTestServlet extends HttpServlet {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private static Logger log = Logger.getLogger(JMeterTestServlet.class);
   private static Logger TESTLOG = Logger.getLogger("TESTLOG");

   @EJB
   private MonitorEjb ejb;

   private Random random = new Random(new Date().getTime());

   @PersistenceContext(unitName = "jmeterPU")
   private EntityManager em;

   @Resource
   private UserTransaction ut;

   /*
    * (non-Javadoc)
    * 
    * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
    * javax.servlet.http.HttpServletResponse)
    */
   /*
    * (non-Javadoc)
    * 
    * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
    * javax.servlet.http.HttpServletResponse)
    */
   @Override
   protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      try {
         long start = System.currentTimeMillis();

         log.debug("execute JMeterTestServlet");
         if (req.getParameter("loops") == null) {
            resp.sendRedirect("help.html");
            return;
         }
         int loops = Integer.parseInt(req.getParameter("loops"));

         String param2 = req.getParameter("param2");

         if (req.getParameter("test") == null) {
            resp.sendRedirect("help.html");
            return;
         }
         String testMethod = req.getParameter("test");

         if (testMethod.startsWith("1")) {
            testMethod = testMethod.substring(1);
            doWork(20000);
            doIOWork(2000);
         }

         if (testMethod.startsWith("ejb")) {
            String tMethod = testMethod.substring(3, 4).toLowerCase() + testMethod.substring(4);
            log.debug("EJB test method " + tMethod);
            try {
               Method method = MonitorEjb.class.getMethod(tMethod, int.class, String.class);
               String result = (String) method.invoke(ejb, loops, param2);
               log.warn(result);
            } catch (Exception e) {
               log.error(e.getMessage(), e);
               throw new ServletException(e);
            }

         } else if (testMethod.startsWith("sync")) {
            // String tMethod = testMethod.substring(4, 5).toLowerCase() + testMethod.substring(5);
            try {
               Method method = MonitorTestClass.class.getMethod(testMethod, int.class);
               method.invoke(null, loops);
            } catch (Exception e) {
               log.error(e.getMessage(), e);
               throw new ServletException(e);
            }

         } else {
            try {
               MonitorTestClass mon = new MonitorTestClass();
               Method method = MonitorTestClass.class.getMethod(testMethod, int.class, String.class);
               method.invoke(mon, loops, param2);
            } catch (Exception e) {
               log.error(e.getMessage(), e);
               throw new ServletException(e);
            }
         }

         long end = System.currentTimeMillis();
         long duration = end - start;

         StringBuffer b = new StringBuffer();
         b.append("<html><head><style type=\"text/css\">h1 { color:rgb(");
         b.append(random.nextInt(255));
         b.append(",");
         b.append(random.nextInt(255));
         b.append(",");
         b.append(random.nextInt(255));
         b.append(")}</style></head><body></p></p><h1>  Execution of test ");
         b.append(testMethod);
         b.append(" with loop count = ");
         b.append(loops);
         b.append(" finished in ");
         b.append(duration);
         b.append(" ms");
         b.append("</h1></body></html>");

         PrintWriter writer = resp.getWriter();
         writer.print(b.toString());
         writer.close();
      } catch (Exception e) {
         log.error(e.getMessage(), e);
         throw e;
      }
   }

   @Override
   protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      doPost(req, resp);
   }

   private String doWork(int loops) {
      log.debug("start doWork");
      String str = "";
      for (int i = 0; i < loops; i++) {
         str += "a";
      }
      log.debug("end doWork");
      return str;
   }

   private String doIOWork(int loops) {
      log.debug("start doIOWork");
      for (int i = 0; i < loops; i++) {
         TESTLOG.info("doIOWork");
      }
      log.debug("start doIOWork");
      return "\ndoIOWork done\n";
   }

   /*
    * (non-Javadoc)
    * 
    * @see javax.servlet.GenericServlet#destroy()
    */
   @Override
   public void destroy() {
      File file = new File(MonitorTestClass.FILENAME);
      file.delete();

      // try {
      // ut.begin();
      // Query q = em.createNamedQuery(JMEntity.DEL_ALL);
      // q.executeUpdate();
      // ut.commit();
      // } catch (Exception e) {
      // log.error(e.getMessage(), e);
      // }
   }

   /*
    * (non-Javadoc)
    * 
    * @see javax.servlet.GenericServlet#init()
    */
   @Override
   public void init(ServletConfig config) throws ServletException {
      log.info("init file " + MonitorTestClass.FILENAME);
      File file = new File(MonitorTestClass.FILENAME);
      FileWriter writer = null;
      FileReader reader = null;
      if (!file.exists()) {
         try {
            boolean isOkay = file.createNewFile();
            log.info("create file " + MonitorTestClass.FILENAME + ": " + isOkay);
            writer = new FileWriter(file);
            IOUtils.write("ein String", writer);
            IOUtils.closeQuietly(writer);

            reader = new FileReader(file);
            String inString = IOUtils.readLines(reader).get(0);
            IOUtils.closeQuietly(reader);

         } catch (IOException e) {
            log.error(e.getMessage(), e);
         } finally {
            IOUtils.closeQuietly(writer);
         }
      }
   }

}
