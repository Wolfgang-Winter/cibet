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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;

import com.cibethelper.ejb.OutService;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TComplexEntity2;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.actuator.scheduler.SchedulerActuator;
import com.logitags.cibet.actuator.scheduler.SchedulerLoader;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.resource.Resource;

public class ArquillianSEServlet extends HttpServlet {

   private static Logger log = Logger.getLogger(ArquillianSEServlet.class);
   /**
    * 
    */
   private static final long serialVersionUID = -2876846209111174152L;

   private EntityManager cibet2;

   private String readResponseBody(HttpResponse response) throws IOException {
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
      }
   }

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
         } else if (req.getRequestURI().endsWith("releaseHttp.cibet")) {
            releaseHttp(resp);
         } else if (req.getRequestURI().endsWith("releasePersist.cibet")) {
            releasePersist(resp);
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
         } else if (req.getRequestURI().endsWith("schedule.cibet")) {
            schedule(req, resp);
         } else if (req.getRequestURI().endsWith("merge.cibet")) {
            merge(req, resp);
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

   private void merge(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      String load = req.getParameter("lazy");
      log.debug("merge chain with " + load);

      TComplexEntity2 tc = createTComplexEntity2();
      cibet2.getTransaction().begin();
      cibet2.persist(tc);
      cibet2.getTransaction().commit();

      cibet2.getTransaction().begin();
      cibet2.clear();
      TComplexEntity2 tc2;
      if (load == null) {
         tc2 = (TComplexEntity2) cibet2.find(TComplexEntity2.class, tc.getId());
      } else {
         tc2 = (TComplexEntity2) cibet2.find(TComplexEntity2.class, tc.getId());
         CibetUtil.loadLazyEntities(tc2, TComplexEntity2.class);
         cibet2.detach(tc2);
      }
      cibet2.getTransaction().commit();

      cibet2.getTransaction().begin();
      cibet2.clear();
      tc2.setCompValue(999);
      try {
         TComplexEntity2 tc3 = cibet2.merge(tc2);
      } catch (Exception e) {
         log.error(e.getMessage(), e);
      }
      cibet2.getTransaction().commit();

      log.debug("after merge");
      EntityManager emm = Context.requestScope().getEntityManager();
      emm.clear();
      Query q = emm.createQuery("select a from Archive a where a.tenant ='XYCompany' order by a.archiveId");
      List<Archive> list = q.getResultList();
      log.info(list.size() + " Archives loaded");
      Resource res1 = list.get(0).getResource();
      TComplexEntity2 tc4 = (TComplexEntity2) res1.getUnencodedTargetObject();
      CibetUtil.isLoaded(cibet2, tc4);

      if (list.size() > 1) {
         Resource res = list.get(1).getResource();
         log.debug("lazy Resource: " + res);
         TComplexEntity2 tc5 = (TComplexEntity2) res.getUnencodedTargetObject();
         CibetUtil.isLoaded(cibet2, tc5);
         log.debug("lazy size: " + tc5.getLazyList().size());
         if (tc5.getLazyList().size() > 0) {
            log.debug("TEntity counter: " + tc5.getLazyList().iterator().next().getCounter());
         }
      }
      log.debug("before commit");

      PrintWriter writer = resp.getWriter();
      writer.print("merge chain done");
      writer.close();
   }

   private void schedule(HttpServletRequest req, HttpServletResponse resp) throws Exception {
      try {
         // to avoid setpoint S25:
         Context.sessionScope().setProperty("Freds", new TEntity());

         Calendar cal = Calendar.getInstance();
         cal.add(Calendar.SECOND, 4);
         Configuration conf = Configuration.instance();
         SchedulerActuator act = (SchedulerActuator) conf.getActuator("schedEE");
         act.setTimerStart(cal.getTime());
         Context.sessionScope().setUser("WW");
         cibet2.clear();
         cibet2.getTransaction().begin();
         log.info("calling schedule");
         String id = req.getParameter("id");

         TEntity tc = cibet2.find(TEntity.class, Long.valueOf(id));

         cibet2.getTransaction().commit();
         cibet2.getTransaction().begin();

         log.debug("tc.getClass(): " + tc.getClass());
         cibet2.detach(tc);
         log.debug("tc.getClass(): " + tc.getClass());

         byte[] bytes = CibetUtil.encode(tc);
         tc = (TEntity) CibetUtil.decode(bytes);
         log.debug("tc.getClass(): " + tc.getClass());

         tc.setOwner("x");
         tc.setXdate(new Date());
         Context.requestScope().setScheduledDate(Calendar.SECOND, 3);
         tc = cibet2.merge(tc);
         cibet2.getTransaction().commit();
         Context.requestScope().getEntityManager().getTransaction().commit();
         Context.requestScope().getEntityManager().getTransaction().begin();
         cibet2.getTransaction().begin();

         cibet2.clear();
         Query q = cibet2.createQuery("select a from TEntity a");
         List<TEntity> list = q.getResultList();
         if (list.size() != 1) {
            throw new Exception("list size is " + list.size());
         }
         if (list.get(0).getXdate() != null) {
            throw new Exception("Xdate is not null" + list.get(0).getXdate());
         }
         if (!"owner1".equals(list.get(0).getOwner())) {
            throw new Exception("owner is not owner1: " + list.get(0).getOwner());
         }

         List<Controllable> dcs = SchedulerLoader.findScheduled();
         if (dcs.size() != 1) {
            throw new Exception("dclist size is " + list.size());
         }

         cibet2.clear();
         log.debug("-------------------- sleep");
         Thread.sleep(7000);
         log.debug("--------------- after TimerTask");
         q = cibet2.createQuery("select a from TEntity a");
         list = q.getResultList();
         if (list.size() != 1) {
            throw new Exception("list size is " + list.size());
         }
         if (list.get(0).getXdate() == null) {
            throw new Exception("getXdate is null");
         }
         if (!"x".equals(list.get(0).getOwner())) {
            throw new Exception("owner is not 'x' " + list.get(0).getOwner());
         }

         PrintWriter writer = resp.getWriter();
         writer.print("Answer: Okay!");
         writer.close();
         cibet2.getTransaction().commit();

      } catch (Throwable e) {
         log.error(e.getMessage(), e);
         cibet2.getTransaction().rollback();
         throw e;
      }
      Context.sessionScope().setUser(null);
   }

   private void schedule2(HttpServletRequest req, HttpServletResponse resp) throws Exception {
      try {
         Calendar cal = Calendar.getInstance();
         cal.add(Calendar.SECOND, 4);
         Configuration conf = Configuration.instance();
         SchedulerActuator act = (SchedulerActuator) conf.getActuator("schedEE");
         act.setTimerStart(cal.getTime());
         Context.sessionScope().setUser("WW");
         cibet2.getTransaction().begin();
         log.info("calling schedule");
         String id = req.getParameter("id");

         TComplexEntity tc = cibet2.find(TComplexEntity.class, Long.valueOf(id));
         TEntity t4 = new TEntity("Stung4", 4, "owner4");

         tc.getEagerList().add(t4);
         tc.setOwner("x");
         Context.requestScope().setScheduledDate(Calendar.SECOND, 3);
         tc = cibet2.merge(tc);
         cibet2.getTransaction().commit();

         cibet2.clear();
         Query q = cibet2.createQuery("select a from TComplexEntity a");
         List<TComplexEntity> list = q.getResultList();
         if (list.size() != 1) {
            throw new Exception("list size is " + list.size());
         }
         if (list.get(0).getEagerList().size() != 0) {
            throw new Exception("eager list size is " + list.get(0).getEagerList().size());
         }
         if (list.get(0).getOwner() != null) {
            throw new Exception("owner not null: " + list.get(0).getOwner());
         }

         List<Controllable> dcs = SchedulerLoader.findScheduled();
         if (dcs.size() != 1) {
            throw new Exception("dclist size is " + list.size());
         }

         cibet2.clear();
         log.debug("-------------------- sleep");
         Thread.sleep(7000);
         log.debug("--------------- after TimerTask");
         q = cibet2.createQuery("select a from TComplexEntity a");
         list = q.getResultList();
         if (list.size() != 1) {
            throw new Exception("list size is " + list.size());
         }
         if (list.get(0).getEagerList().size() != 1) {
            throw new Exception("eager list size is " + list.get(0).getEagerList().size());
         }
         if (!"x".equals(list.get(0).getOwner())) {
            throw new Exception("owner is not 'x' " + list.get(0).getOwner());
         }

         PrintWriter writer = resp.getWriter();
         writer.print("Answer: Okay!");
         writer.close();

      } catch (Throwable e) {
         log.error(e.getMessage(), e);
         cibet2.getTransaction().rollback();
         throw e;
      }
      Context.sessionScope().setUser(null);
   }

   private void releaseHttp(HttpServletResponse resp) throws Exception {
      try {
         cibet2.getTransaction().begin();
         List<Controllable> dcs = DcLoader.findUnreleased();
         if (dcs.size() != 1) {
            String msg = "Found " + dcs.size() + " Controllables instead of 1";
            log.error(msg);
            throw new Exception(msg);
         }

         HttpResponse response = (HttpResponse) dcs.get(0).release(cibet2, "very good!");
         log.info(response);
         String strResp = readResponseBody(response);
         log.debug("before commit");
         cibet2.getTransaction().commit();

         PrintWriter writer = resp.getWriter();
         writer.print(strResp);
         writer.close();

      } catch (Exception e) {
         log.error(e.getMessage(), e);
         cibet2.getTransaction().rollback();
         throw e;
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
      TEntity te = new TEntity("arq-SE-EM-Provider", 751, "Hindiman");

      cibet2.getTransaction().begin();
      try {
         cibet2.persist(te);
         log.debug("after persist");
         log.debug("before commit");
         cibet2.getTransaction().commit();
      } catch (Exception e) {
         log.error(e.getMessage(), e);
         cibet2.getTransaction().rollback();
         throw e;
      } finally {
         // log.debug("before commit");
         // cibet2.getTransaction().commit();
      }
      log.info("TEntity persisted with id " + te.getId());
      PrintWriter writer = resp.getWriter();
      writer.print("TEntity persist with ID: " + te.getId());
      writer.close();
   }

   private void releasePersist(HttpServletResponse resp) throws Exception {
      TEntity te = null;
      cibet2.getTransaction().begin();
      try {
         List<Controllable> dcs = DcLoader.findUnreleased();
         if (dcs.size() != 1) {
            String msg = "Found " + dcs.size() + " Controllables instead of 1";
            log.error(msg);
            throw new Exception(msg);
         }

         te = (TEntity) dcs.get(0).release(cibet2, "good!");
         log.debug("before commit");
         cibet2.getTransaction().commit();

      } catch (Exception e) {
         log.error(e.getMessage(), e);
         cibet2.getTransaction().rollback();
         throw e;
      }

      PrintWriter writer = resp.getWriter();
      if (te.getId() == 0) {
         log.info("TEntity not released");
         writer.print("TEntity not released");
      } else {
         log.info("TEntity released with id " + te.getId());
         writer.print("TEntity released with ID: " + te.getId());
      }
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

      log.info("create cibet2 EntityManager");
      cibet2 = Persistence.createEntityManagerFactory("APPL-UNIT").createEntityManager();
   }

   protected TComplexEntity2 createTComplexEntity2() {
      String TENANT = Context.sessionScope().getTenant();

      TEntity e1 = new TEntity("val3", 3, TENANT);
      TEntity e2 = new TEntity("val4", 4, TENANT);
      TEntity e3 = new TEntity("val5", 5, TENANT);
      TEntity e4 = new TEntity("val6", 6, TENANT);
      TEntity e5 = new TEntity("val7", 7, TENANT);

      Set<TEntity> lazyList = new LinkedHashSet<TEntity>();
      lazyList.add(e2);
      lazyList.add(e3);
      Set<TEntity> eagerList = new LinkedHashSet<TEntity>();
      eagerList.add(e4);
      eagerList.add(e5);

      TComplexEntity2 ce = new TComplexEntity2();
      ce.setCompValue(12);
      ce.setTen(e1);
      ce.setOwner(TENANT);
      ce.setEagerList(eagerList);
      ce.setLazyList(lazyList);
      ce.addLazyList(createTEntity(6, "Hase6"));

      return ce;
   }

   protected TEntity createTEntity(int counter, String name) {
      TEntity te = new TEntity();
      te.setCounter(counter);
      te.setNameValue(name);
      te.setOwner(name);
      return te;
   }

}
