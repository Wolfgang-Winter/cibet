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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.apache.log4j.Logger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.archive.ArchiveLoader;
import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.actuator.common.DeniedEjbException;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.actuator.lock.AlreadyLockedException;
import com.logitags.cibet.actuator.lock.Locker;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.sensor.pojo.CibetIntercept;

public class TutorialServlet1 extends HttpServlet {

   private static Logger log = Logger.getLogger(TutorialServlet1.class);
   /**
    * 
    */
   private static final long serialVersionUID = -2876846209111174152L;

   @Resource
   private UserTransaction ut;

   @PersistenceContext(unitName = "APPL-UNIT")
   private EntityManager applEman;

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
         String msg = "";
         if (req.getRequestURI().endsWith("persist")) {
            try {
               persist(req, resp);
            } catch (DeniedEjbException e1) {
               log.warn(e1.getMessage());
               msg = "invoke of method persist denied";
            }
         } else if (req.getRequestURI().endsWith("loadPersonArchive")) {
            loadPersonArchive(req, resp);
         } else if (req.getRequestURI().endsWith("clean")) {
            clean(req, resp);
         } else if (req.getRequestURI().endsWith("loginSpring")) {
            msg = loginSpring(req, resp);
         } else if (req.getRequestURI().endsWith("logoffSpring")) {
            msg = logoffSpring();
         } else if (req.getRequestURI().endsWith("release")) {
            release(req, resp);
         } else if (req.getRequestURI().endsWith("loadPerson")) {
            loadPerson(req, resp);
         } else if (req.getRequestURI().endsWith("findPerson")) {
            findPerson(req, resp);
         } else if (req.getRequestURI().endsWith("batch")) {
            batch(req, resp);
         } else if (req.getRequestURI().endsWith("unlock")) {
            unlock(req, resp);
         } else {
            msg = "ERROR: no functionality found!!";
         }

         PrintWriter writer = resp.getWriter();
         writer.print(msg);
         writer.close();

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

      Query q4 = Context.requestScope().getEntityManager().createQuery("select d from Controllable d");
      List<Controllable> dclist = q4.getResultList();
      for (Controllable dc : dclist) {
         Context.requestScope().getEntityManager().remove(dc);
      }

      // Query q5 = Context.requestScope().getEntityManager().createQuery("SELECT a FROM LockedObject a");
      // Iterator<LockedObject> itLO = q5.getResultList().iterator();
      // while (itLO.hasNext()) {
      // Context.requestScope().getEntityManager().remove(itLO.next());
      // }

      Query q6 = Context.requestScope().getEntityManager().createQuery("SELECT a FROM EventResult a");
      Iterator<EventResult> itEV = q6.getResultList().iterator();
      while (itEV.hasNext()) {
         Context.requestScope().getEntityManager().remove(itEV.next());
      }

      ut.commit();
   }

   @CibetIntercept
   public void persist(HttpServletRequest req, HttpServletResponse resp) throws Exception {
      Person person = new Person("Walter");
      person.getAddresses().add(new Address("Hamburg"));
      person.getAddresses().add(new Address("Aachen"));

      ut.begin();
      applEman.persist(person);
      ut.commit();
      String info = "Person persisted with id " + person.getPersonId();
      log.info(info);
      PrintWriter writer = resp.getWriter();
      writer.print("" + person.getPersonId());
      writer.close();
   }

   private void release(HttpServletRequest req, HttpServletResponse resp) throws Exception {
      log.debug("release");
      ut.begin();
      List<Controllable> dcList = DcLoader.findUnreleased(Person.class.getName());
      if (dcList.size() == 0) {
         throw new Exception("no Controllable found for Target Person");
      }
      Person person = (Person) dcList.get(0).release(applEman, "now released");
      ut.commit();
      String info = "Persistence of Person released with id " + person.getPersonId();
      log.info(info);
      PrintWriter writer = resp.getWriter();
      writer.print(person.getPersonId());
      writer.close();
   }

   private void loadPersonArchive(HttpServletRequest req, HttpServletResponse resp) throws Exception {
      String id = req.getParameter("id");
      int expected = Integer.valueOf(req.getParameter("expected"));
      List<Archive> archives = ArchiveLoader.loadArchivesByPrimaryKeyId(Person.class.getName(), id);
      if (archives.size() != expected) {
         throw new Exception("Archive list size is not " + expected + " but " + archives.size());
      }

      String response = "no Person Archive found";
      if (expected > 0) {
         Person person = (Person) archives.get(0).getResource().getUnencodedTargetObject();
         response = person.toString().replaceAll("\n", " ; ");
         log.info(person);
      }
      PrintWriter writer = resp.getWriter();
      writer.print(response);
      writer.close();
   }

   private void loadPerson(HttpServletRequest req, HttpServletResponse resp) throws Exception {
      int expected = Integer.valueOf(req.getParameter("expected"));

      Query q = applEman.createQuery("select p from Person p");

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

   private void findPerson(HttpServletRequest req, HttpServletResponse resp) throws Exception {
      String info = "select statement executed";
      try {
         Person p = applEman.find(Person.class, "xx");
      } catch (DeniedEjbException e) {
         log.warn(e.getMessage());
         info = "denied";
      }

      PrintWriter writer = resp.getWriter();
      writer.print(info);
      writer.close();
   }

   private String loginSpring(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      log.debug("Spring login to session: " + req.getSession().getId());

      logoffSpring();

      String tenant = req.getParameter("TENANT");
      String user = req.getParameter("USER");
      Collection<GrantedAuthority> authList = new ArrayList<>();
      authList.add(new SimpleGrantedAuthority("ROLE_" + req.getParameter("ROLE")));
      Authentication request = new UsernamePasswordAuthenticationToken(user, "FIXED-PW", authList);
      SecurityContextHolder.getContext().setAuthentication(request);

      log.debug(SecurityContextHolder.getContext().getAuthentication().getCredentials());
      for (GrantedAuthority g : SecurityContextHolder.getContext().getAuthentication().getAuthorities()) {
         log.debug(g);
      }
      req.getSession().setAttribute("CIBET_TENANT", tenant);

      String msg = "Spring logged in user " + Context.sessionScope().getUser() + ", tenant: "
            + Context.sessionScope().getTenant();
      log.debug(msg);
      return msg;
   }

   private void batch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
      try {
         // set user and tenant
         Context.sessionScope().setUser("batch");
         Context.sessionScope().setTenant("LockTest");
         ut.begin();
         Controllable lock1 = Locker.lock(Person.class, ControlEvent.SELECT, null);
         Controllable lock2 = Locker.lock(this.getClass(), "persist", ControlEvent.INVOKE, null);
         ut.commit();

         PrintWriter writer = resp.getWriter();
         writer.print(lock1.getControllableId() + ":" + lock2.getControllableId());
         writer.close();
      } catch (AlreadyLockedException e) {
         log.error(e.getMessage(), e);
      }

      // long running batch process here
      // ...
      // normally release the locks at the end
   }

   private void unlock(HttpServletRequest req, HttpServletResponse resp) throws Exception {
      String lock1Id = req.getParameter("LOCK1");
      String lock2Id = req.getParameter("LOCK2");

      ut.begin();
      Controllable lock1 = Context.requestScope().getEntityManager().find(Controllable.class, lock1Id);
      Locker.unlock(lock1, null);

      Controllable lock2 = Context.requestScope().getEntityManager().find(Controllable.class, lock2Id);
      Locker.unlock(lock2, null);
      ut.commit();

      PrintWriter writer = resp.getWriter();
      writer.print("unlocked completed");
      writer.close();
   }

   private String logoffSpring() throws ServletException, IOException {
      Context.sessionScope().setUser(null);
      SecurityContextHolder.clearContext();
      return "Spring logOff";
   }

   /*
    * (non-Javadoc)
    * 
    * @see javax.servlet.GenericServlet#init()
    */
   @Override
   public void init() throws ServletException {
      super.init();
      log.debug("init()");
      // System.setProperty("openejb.logger.external", "true");
      //
      // System.setProperty("spring.security.strategy", "MODE_GLOBAL");
      // System.setProperty("openejb.logger.external", "true");
      // SecurityContextHolder.setStrategyName("MODE_GLOBAL");
      //
      // WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
      // log.debug("set WebApplicationContext " + ctx + ", @" + ctx.hashCode());
      // log.debug(ctx.getBeanDefinitionCount() + " beans in context");
      // String[] names = ctx.getBeanDefinitionNames();
      // for (String name : names) {
      // log.debug("Bean: " + name + ", class: " + ctx.getBean(name));
      // if ("springSecurityActuator".equals(name)) {
      // SpringSecurityActuator act = (SpringSecurityActuator) ctx.getBean(name);
      // log.debug("act=" + act);
      // }
      // if (name.startsWith("org.springframework.security.access.vote.AffirmativeBased")) {
      // AffirmativeBased af = (AffirmativeBased) ctx.getBean(name);
      // log.debug("af=" + af);
      // for (AccessDecisionVoter v : af.getDecisionVoters()) {
      // log.debug("voter class: " + v.getClass().getName());
      // }
      // af.getDecisionVoters().add(new RoleVoter());
      // af.getDecisionVoters().add(new AuthenticatedVoter());
      // }
      // }

   }

}
