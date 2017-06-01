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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.cibethelper.base.NoControlActuator;
import com.cibethelper.base.SubArchiveController;
import com.cibethelper.ejb.Ejb2Service;
import com.cibethelper.ejb.EjbService;
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
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.sensor.http.Headers;

public class GeneralServlet extends HttpServlet {

   private static Logger log = Logger.getLogger(GeneralServlet.class);
   /**
    * 
    */
   private static final long serialVersionUID = -2876846209111174152L;

   @javax.annotation.Resource
   private UserTransaction ut;

   @PersistenceContext(unitName = "APPL-UNIT")
   private EntityManager cibet2;

   @PersistenceContext(unitName = "APPL-UNIT-EAGER")
   private EntityManager cibet2Eager;

   @EJB
   private EjbService ejbService;

   @EJB
   private Ejb2Service ejb2Service;

   private NoControlActuator ncaBean;

   private String readResponseBody(HttpResponse response) throws IOException {
      // Read the response body.
      HttpEntity entity = response.getEntity();
      InputStream instream = null;
      try {
         if (entity != null) {
            instream = entity.getContent();

            StringBuffer b = new StringBuffer();
            BufferedReader reader = new BufferedReader(new InputStreamReader(instream));

            String s = reader.readLine();
            while (s != null) {
               b.append(s);
               s = reader.readLine();
            }

            String body = b.toString();
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
         } else if (req.getRequestURI().endsWith("logThis.url")) {
            logThisEjb(resp);
         } else if (req.getRequestURI().endsWith("logout.cibet")) {
            logout(req, resp);
         } else if (req.getRequestURI().endsWith("showSession.cibet")) {
            showSession(req, resp);
         } else if (req.getRequestURI().endsWith("loginSpring.cibet")) {
            loginSpring(req, resp);
         } else if (req.getRequestURI().endsWith("logThis.cibet")) {
            logThis(req, resp);
         } else if (req.getRequestURI().endsWith("aspect.cibet")) {
            log.info("call aspect");
            aspect(req, resp);
         } else if (req.getRequestURI().endsWith("aspectEJB.cibet")) {
            log.info("call EJB aspect");
            ejbaspect(req, resp);
         } else if (req.getRequestURI().endsWith("aspectSpring.cibet")) {
            springaspect(req, resp);
         } else if (req.getRequestURI().endsWith("schedule.cibet")) {
            schedule(req, resp);
         } else if (req.getRequestURI().endsWith("merge.cibet")) {
            merge(req, resp);
         } else if (req.getRequestURI().endsWith("aspectPojo.cibet")) {
            aspectPojo(req, resp);
         } else if (req.getRequestURI().endsWith("proxy.cibet")) {
            proxy(req, resp);
         } else if (req.getRequestURI().endsWith("checkConfig.cibet")) {
            checkConfig(req, resp);
         } else if (req.getRequestURI().endsWith("clean.cibet")) {
            clean();
         } else if (req.getRequestURI().endsWith("execute.cibet")) {
            executeApplEmanQuery(req, resp);
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

   private void checkConfig(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      StringBuffer b = new StringBuffer();
      b.append("Configuration ");
      b.append(Configuration.instance());
      b.append("\n<br>");
      for (Setpoint s : Configuration.instance().getSetpoints()) {
         b.append(s.getId());
         b.append(", ");
      }

      PrintWriter writer = resp.getWriter();
      writer.print(b.toString());
      writer.close();
   }

   private void proxy(HttpServletRequest req, HttpServletResponse resp) throws Exception {
      HttpHost proxy = new HttpHost("localhost", 10113);
      HttpClient client = HttpClients.custom().setProxy(proxy).disableAutomaticRetries().build();

      String url = "http://httpbin.org/ip";
      HttpGet method = new HttpGet(url);

      String h = Context.encodeContext();
      method.addHeader(Headers.CIBET_CONTEXT.name(), h);

      HttpResponse response = client.execute(method);
      log.debug("STATUS: " + response.getStatusLine().getStatusCode());
      String msg = readResponseBody(response);

      PrintWriter writer = resp.getWriter();
      writer.print(msg);
      writer.close();
   }

   private void aspectPojo(HttpServletRequest req, HttpServletResponse resp) {
      log.info("call aspectPojo in Servlet");
      req.getSession().setAttribute("CIBET_REMARK", "Vögelein");
      ejb2Service.doIt("Josef");
   }

   private void schedule(HttpServletRequest req, HttpServletResponse resp) throws Exception {
      try {
         Calendar cal = Calendar.getInstance();
         cal.add(Calendar.SECOND, 7);
         Configuration conf = Configuration.instance();
         SchedulerActuator act = (SchedulerActuator) conf.getActuator("schedEE");
         act.setTimerStart(cal.getTime());
         Context.sessionScope().setUser("WW");
         ut.begin();
         log.info("calling schedule");
         String id = req.getParameter("id");

         TComplexEntity tc = cibet2Eager.find(TComplexEntity.class, Long.valueOf(id));
         TEntity t4 = new TEntity("Stung4", 4, "owner4");

         tc.getEagerList().add(t4);
         tc.setOwner("x");
         // Context.requestScope().setScheduledDate(Calendar.SECOND, 3);
         req.getSession().setAttribute("CIBET_SCHEDULEDDATE", 3);
         req.getSession().setAttribute("CIBET_SCHEDULEDFIELD", Calendar.SECOND);
         tc = cibet2.merge(tc);
         ut.commit();

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
         log.debug("Actuator: " + dcs.get(0).getActuator() + ", scheduled for: " + dcs.get(0).getScheduledDate());

         cibet2.clear();
         log.debug("-------------------- sleep");
         Thread.sleep(9000);
         log.debug("--------------- after TimerTask");

         cibet2.clear();
         TComplexEntity tcend = cibet2.find(TComplexEntity.class, Long.valueOf(id));

         q = cibet2.createQuery("select a from TComplexEntity a");
         list = q.getResultList();
         if (list.size() != 1) {
            throw new Exception("list size is " + list.size());
         }
         // if (list.get(0).getEagerList().size() != 1) {
         if (tcend.getEagerList().size() != 1) {
            throw new Exception("eager list size is " + list.get(0).getEagerList().size());
         }
         if (!"x".equals(tcend.getOwner())) {
            throw new Exception("owner is not 'x' " + list.get(0).getOwner());
         }

         Context.internalRequestScope().getEntityManager().clear();
         log.warn("after (sleep) ..................");
         Thread.sleep(5000);
         List<Controllable> dcList = DcLoader.loadByUser(Context.sessionScope().getUser());
         if (dcList.size() != 1) {
            throw new Exception("number of Controllable: " + dcList.size());
         }
         Controllable dc = dcList.get(0);
         if (dc.getExecutionDate() == null) {
            throw new Exception("executionDate is null");
         }
         if (dc.getExecutionStatus() != ExecutionStatus.EXECUTED) {
            throw new Exception("executionStatus is not EXECUTED: " + dc.getExecutionStatus());
         }

         PrintWriter writer = resp.getWriter();
         writer.print("Answer: Okay!");
         writer.close();

      } catch (Throwable e) {
         log.error(e.getMessage(), e);
         ut.rollback();
         throw e;
      }
      Context.sessionScope().setUser(null);
   }

   private void aspect(HttpServletRequest req, HttpServletResponse resp) throws Exception {
      try {
         ut.begin();

         SubArchiveController co = new SubArchiveController();
         log.info("calling aspect");
         PrintWriter writer = resp.getWriter();
         writer.print("ASPECT: " + co.getName());
         writer.close();
         ut.commit();

      } catch (Throwable e) {
         log.error(e.getMessage(), e);
         ut.rollback();
         throw e;
      }
   }

   private void ejbaspect(HttpServletRequest req, HttpServletResponse resp) throws Exception {
      try {
         log.info("calling EJB aspect");
         String answer = ejbService.logThisForAspect("Hallo EJB aspect Cibet");

         PrintWriter writer = resp.getWriter();
         writer.print("ASPECT: " + answer);
         writer.close();

      } catch (Throwable e) {
         log.error(e.getMessage(), e);
         throw e;
      }
   }

   private void springaspect(HttpServletRequest req, HttpServletResponse resp) throws Exception {
      try {
         ut.begin();
         log.info("calling spring aspect");
         log.debug("nca: " + ncaBean);

         String name = ncaBean.getNameForSpringTest();
         PrintWriter writer = resp.getWriter();
         writer.print("ASPECT: " + name);
         writer.close();
         ut.commit();

      } catch (Throwable e) {
         log.error(e.getMessage(), e);
         ut.rollback();
         throw e;
      }
   }

   private void releaseHttp(HttpServletResponse resp) throws Exception {
      try {
         ut.begin();
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
         ut.commit();

         PrintWriter writer = resp.getWriter();
         writer.print(strResp);
         writer.close();

      } catch (Exception e) {
         log.error(e.getMessage(), e);
         ut.rollback();
         throw e;
      }
   }

   private void logThisEjb(HttpServletResponse resp) throws Exception {
      String answer = ejbService.logThis("Hallo Cibet");
      PrintWriter writer = resp.getWriter();
      writer.print("Answer: " + answer);
      writer.close();
   }

   private void logThis(HttpServletRequest req, HttpServletResponse resp) throws Exception {
      log.debug("start logThis()");
      try {
         OutService service = new OutService();
         String message = req.getParameter("MSG");
         String answer = service.logThis(message);

         PrintWriter writer = resp.getWriter();
         writer.print("Answer: " + answer);
         writer.close();
      } catch (Throwable e) {
         log.error(e.getMessage(), e);
         throw e;
      }
   }

   private void persist(HttpServletResponse resp) throws Exception {
      TEntity te = new TEntity("arq-jtaEM-Provider", 455, "Muselmann");

      ut.begin();
      try {
         cibet2.persist(te);
         log.debug("after persist");
      } catch (Exception e) {
         log.error(e.getMessage(), e);
         throw e;
      } finally {
         log.debug("before commit");
         ut.commit();
      }
      log.info("TEntity persisted with id " + te.getId());
      PrintWriter writer = resp.getWriter();
      writer.print("TEntity persist with ID: " + te.getId());
      writer.close();
   }

   private void releasePersist(HttpServletResponse resp) throws Exception {
      Object te = null;
      ut.begin();
      try {
         List<Controllable> dcs = DcLoader.findUnreleased();
         if (dcs.size() != 1) {
            String msg = "Found " + dcs.size() + " Controllables instead of 1";
            log.error(msg);
            throw new Exception(msg);
         }

         te = dcs.get(0).release(cibet2, "good!");
         log.debug("before commit");
         ut.commit();

      } catch (Exception e) {
         log.error(e.getMessage(), e);
         ut.rollback();
         throw e;
      }

      PrintWriter writer = resp.getWriter();
      if (te instanceof TEntity) {
         TEntity te2 = (TEntity) te;
         if (te2.getId() == 0) {
            log.info("TEntity not released");
            writer.print("TEntity not released");
         } else {
            log.info("TEntity released with id " + te2.getId());
            writer.print("TEntity released with ID: " + te2.getId());
         }
      } else {
         writer.print("TComplexEntity2 released");
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

   private void loginSpring(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      log.debug("Spring login to session: " + req.getSession().getId());

      try {
         Collection<GrantedAuthority> authList = new ArrayList<>();
         authList.add(new GrantedAuthorityImpl(req.getParameter("ROLE")));
         Authentication request = new UsernamePasswordAuthenticationToken(req.getParameter("USER"), "FIXED-PW",
               authList);
         SecurityContextHolder.getContext().setAuthentication(request);

         if (req.getParameter("Freds") != null) {
            Context.sessionScope().setProperty("Freds", new TEntity("freds name", 8, "freds owner"));
         }

         if (req.getParameter("tenant") != null) {
            Context.sessionScope().setTenant(req.getParameter("tenant"));
         }

      } catch (AuthenticationException e) {
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
      log.debug("invalidate session: " + req.getSession().getId());
      req.getSession().invalidate();

      PrintWriter writer = resp.getWriter();
      writer.print("Logout done");
      writer.close();
   }

   private void merge(HttpServletRequest req, HttpServletResponse resp) throws Exception {
      String load = req.getParameter("lazy");
      log.debug("merge chain with " + load);

      TComplexEntity2 tc = createTComplexEntity2();
      tc = (TComplexEntity2) ejbService.persist(tc);

      TComplexEntity2 tc2;
      if (load == null) {
         ut.begin();
         tc2 = (TComplexEntity2) ejbService.find(TComplexEntity2.class, tc.getId());
         tc2.setCompValue(999);
         try {
            TComplexEntity2 tc3 = ejbService.merge(tc2);
         } catch (Exception e) {
            log.error(e.getMessage(), e);
         }
         ut.commit();
      } else {
         tc2 = (TComplexEntity2) ejbService.findAndLoadComplete(TComplexEntity2.class, tc.getId());
         tc2.setCompValue(999);
         try {
            TComplexEntity2 tc3 = ejbService.merge(tc2);
         } catch (Exception e) {
            log.error(e.getMessage(), e);
         }
      }

      log.debug("after merge");
      ut.begin();
      EntityManager emm = Context.requestScope().getEntityManager();
      emm.clear();
      Query q = emm.createQuery("select a from Archive a where a.tenant ='" + Context.sessionScope().getTenant()
            + "' order by a.archiveId");
      List<Archive> list = q.getResultList();
      log.info(list.size() + " Archives loaded");
      Resource res1 = list.get(0).getResource();
      TComplexEntity2 tc4 = (TComplexEntity2) res1.getUnencodedTargetObject();
      CibetUtil.isLoaded(cibet2, tc4);

      if (list.size() > 1) {
         Resource res = list.get(1).getResource();
         log.debug("lazy Resource: " + res);
         TComplexEntity2 tc5 = (TComplexEntity2) res.getUnencodedTargetObject();
         boolean loadState = CibetUtil.isLoaded(cibet2, tc5);
         // if (load == null && loadState == true) {
         // throw new Exception("loadState should be false");
         // }
         // log.debug("lazy size: " + tc5.getLazyList().size());
         // if (tc5.getLazyList().size() > 0) {
         // log.debug("TEntity counter: " + tc5.getLazyList().iterator().next().getCounter());
         // }
      }
      log.debug("before commit");
      ut.commit();

      PrintWriter writer = resp.getWriter();
      writer.print("merge chain done");
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

      WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());

      log.debug(ctx.getBeanDefinitionCount() + " beans are in context");
      String[] names = ctx.getBeanDefinitionNames();
      for (String name : names) {
         log.debug("Bean: " + name + ",         class: " + ctx.getBean(name));
      }

      ncaBean = ctx.getBean(NoControlActuator.class);
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

   protected TComplexEntity createTComplexEntity() {
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

      TComplexEntity ce = new TComplexEntity();
      ce.setCompValue(12);
      ce.setTen(e1);
      ce.setOwner(TENANT);
      ce.setEagerList(eagerList);
      ce.setLazyList(lazyList);
      ce.addLazyList(createTEntity(6, "Hase6"));

      return ce;
   }

   protected void clean() throws Exception {
      log.debug("GeneralServlet:clean()");

      ut.begin();

      Query q = cibet2.createNamedQuery(TComplexEntity.SEL_ALL);
      List<TComplexEntity> l = q.getResultList();
      for (TComplexEntity tComplexEntity : l) {
         cibet2.remove(tComplexEntity);
      }

      Query q1 = cibet2.createNamedQuery(TComplexEntity2.SEL_ALL);
      List<TComplexEntity2> l1 = q1.getResultList();
      for (TComplexEntity2 tComplexEntity : l1) {
         cibet2.remove(tComplexEntity);
      }

      Query q2 = cibet2.createNamedQuery(TEntity.DEL_ALL);
      q2.executeUpdate();

      Query q3 = Context.internalRequestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL);
      List<Archive> alist = q3.getResultList();
      for (Archive ar : alist) {
         Context.internalRequestScope().getEntityManager().remove(ar);
      }

      Query q4 = Context.internalRequestScope().getEntityManager().createQuery("select d from Controllable d");
      List<Controllable> dclist = q4.getResultList();
      for (Controllable dc : dclist) {
         Context.internalRequestScope().getEntityManager().remove(dc);
      }

      Query q6 = Context.internalRequestScope().getEntityManager().createQuery("SELECT a FROM EventResult a");
      Iterator<EventResult> itEV = q6.getResultList().iterator();
      while (itEV.hasNext()) {
         Context.internalRequestScope().getEntityManager().remove(itEV.next());
      }

      ut.commit();
   }

   protected void executeApplEmanQuery(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      String query = req.getParameter("query");

      Query q = cibet2.createQuery(query);
      TEntity t = (TEntity) q.getSingleResult();
      log.debug(t);
      PrintWriter writer = resp.getWriter();
      writer.print(t);
      writer.close();
   }

}
