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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.actuator.lock.LockedObject;
import com.logitags.cibet.context.CibetContext;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.EventResult;

public class TutorialServlet5 extends HttpServlet {

   private static Logger log = Logger.getLogger(TutorialServlet5.class);
   /**
    * 
    */
   private static final long serialVersionUID = -2876846209111174152L;

   private EntityManagerFactory emfac;

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
         if (req.getRequestURI().endsWith("persist")) {
            persist(req, resp);
         } else if (req.getRequestURI().endsWith("loadPerson")) {
            loadPerson(req, resp);
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

   }

   @CibetContext
   public void persist(HttpServletRequest req, HttpServletResponse resp) throws Exception {
      log.debug("start persist()");
      Person person = new Person("Walter");
      person.getAddresses().add(new Address("Hamburg"));
      person.getAddresses().add(new Address("Aachen"));

      // req.getSession().setAttribute("CIBET_USER", "Mausi");
      // req.getSession().setAttribute("CIBET_SCHEDULEDDATE", 3);
      // req.getSession().setAttribute("CIBET_SCHEDULEDFIELD", Calendar.SECOND);
      // req.getSession().setAttribute("CIBET_REMARK", "This is scheduled");
      Context.requestScope().setScheduledDate(Calendar.SECOND, 3);
      Context.requestScope().setRemark("This is scheduled");
      Context.sessionScope().setUser("Mausi");

      EntityManager em = emfac.createEntityManager();
      em.getTransaction().begin();
      em.persist(person);
      em.getTransaction().commit();

      EventResult er = Context.requestScope().getExecutedEventResult();
      log.info("execution status is " + er.getExecutionStatus());

      String info = "Person persisted with id " + person.getPersonId();
      log.info(info);
      PrintWriter writer = resp.getWriter();
      writer.print(er.getExecutionStatus().name());
      writer.close();
   }

   private void loadPerson(HttpServletRequest req, HttpServletResponse resp) throws Exception {
      int expected = Integer.valueOf(req.getParameter("expected"));

      EntityManager em = emfac.createEntityManager();
      Query q = em.createQuery("select p from Person p");

      Person person = null;
      try {
         person = (Person) q.getSingleResult();
      } catch (NoResultException e) {
         if (expected != 0) {
            throw e;
         }
      }
      log.info(person);

      String response = "no Person found";
      if (person != null) {
         response = person.toString().replaceAll("\n", " ; ");
         log.info(person);
      }
      PrintWriter writer = resp.getWriter();
      writer.print(response);
      writer.close();
   }

   @Override
   public void init() throws ServletException {
      emfac = Persistence.createEntityManagerFactory("APPL-UNIT");
      super.init();
   }

}
