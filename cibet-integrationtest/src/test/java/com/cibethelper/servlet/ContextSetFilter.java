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
import java.lang.reflect.Field;
import java.util.Iterator;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.expression.WebExpressionVoter;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.cibethelper.SpringTestAuthenticationManager;
import com.logitags.cibet.actuator.springsecurity.SpringSecurityActuator;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalSessionScope;

public class ContextSetFilter implements Filter {

   private static Logger log = Logger.getLogger(ContextSetFilter.class);

   private static final String KEY_PROPERTIES = "__CIBET_PROPERTIES";

   public static WebApplicationContext springWebContext;

   private ApplicationContext springTempContext;

   private static WebExpressionVoter webVoter;

   @Override
   public void destroy() {
   }

   @Override
   public void doFilter(ServletRequest req, ServletResponse resp, FilterChain arg2)
         throws IOException, ServletException {
      log.debug("call ContextSetFilter.doFilter");

      // if (req.getParameter("user") != null) {
      // if ("LOGIN".equals(req.getParameter("user"))) {
      // Context.sessionScope().setUser(null);
      // ((HttpServletRequest) req).login("Jesofi", "pwd");
      // } else if ("NO_USER".equals(req.getParameter("user"))) {
      // Context.sessionScope().setUser(null);
      // } else {
      // Context.sessionScope().setUser(req.getParameter("user"));
      // }
      // } else {
      // Context.sessionScope().setUser("USER");
      // }
      // if (req.getParameter("tenant") != null) {
      // if (!"NO_TENANT".equals(req.getParameter("tenant"))) {
      // Context.sessionScope().setTenant(req.getParameter("tenant"));
      // }
      // } else {
      // Context.sessionScope().setTenant("testTenant");
      // }

      // if (req.getParameter("secondUser") != null) {
      // log.debug("set secondUser= " + req.getParameter("secondUser"));
      // if (req.getParameter("secondUser").equals("NULL")) {
      // Context.sessionScope().setSecondUser(null);
      // } else {
      // Context.sessionScope().setSecondUser(req.getParameter("secondUser"));
      // }
      // }
      //
      String[] roles = req.getParameterValues("role");
      if (roles != null) {
         authenticate(roles);
      }
      //
      // if (req.getParameter("shiroUser") != null) {
      // // WebEnvironment web = WebUtils.getRequiredWebEnvironment(req
      // // .getServletContext());
      // // Subject subject = new WebSubject.Builder(web.getSecurityManager(),
      // // req, arg1).buildWebSubject();
      // Subject subject = SecurityUtils.getSubject();
      // authenticateShiro(subject, req.getParameter("shiroUser"));
      // }
      //
      // String[] roles2 = req.getParameterValues("secondRole");
      // if (roles2 != null) {
      // authenticateSecond(roles2);
      // }

      String attribute = req.getParameter("attribute");
      if (attribute != null) {
         req.setAttribute("com.logitags.att", attribute.getBytes("UTF-8"));
      }

      doConfig(req, resp);

      // setSpringContext();

      try {
         arg2.doFilter(req, resp);
      } finally {
         // resetSpringContext();
         // if (Context.requestScope().getProperty(InternalRequestScope.ENTITYMANAGER_TYPE) != EntityManagerType.JTA
         // && Context.internalRequestScope().getNullableEntityManager() != null
         // && Context.internalRequestScope().getEntityManager().getTransaction().isActive()) {
         // if (Context.requestScope().getRollbackOnly()) {
         // Context.internalRequestScope().getEntityManager().getTransaction().rollback();
         // } else {
         // Context.internalRequestScope().getEntityManager().getTransaction().commit();
         // }
         // }

         log.debug("ContextSetFilter finished");
      }
   }

   @Override
   public void init(FilterConfig fconfig) throws ServletException {
      // log.debug("call ContextSetFilter.init");
      // System.setProperty("openejb.logger.external", "true");
      //
      // System.setProperty("spring.security.strategy", "MODE_GLOBAL");
      // System.setProperty("openejb.logger.external", "true");
      // SecurityContextHolder.setStrategyName("MODE_GLOBAL");
      //
      // WebApplicationContext ctx = WebApplicationContextUtils
      // .getRequiredWebApplicationContext(fconfig.getServletContext());
      // log.debug("set WebApplicationContext " + ctx + ", @" + ctx.hashCode());
      // ContextSetFilter.springWebContext = ctx;
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
      // if (v instanceof WebExpressionVoter) {
      // webVoter = (WebExpressionVoter) v;
      // }
      // }
      // af.getDecisionVoters().add(new RoleVoter());
      // af.getDecisionVoters().add(new AuthenticatedVoter());
      // }
      // }
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
         log.error("Authentication failed: " + e.getMessage());
      }
   }

   // private void authenticateShiro(Subject subject, String token) {
   // int index = token.indexOf(":");
   // String user = token.substring(0, index);
   // String password = token.substring(index + 1);
   // log.info("authenticate Shiro: " + user);
   //
   // AuthenticationToken auth = new UsernamePasswordToken(user, password);
   // subject.login(auth);
   // }

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
      }
   }

   private void setSpringContext() throws ServletException {
      if (springWebContext == null)
         return;
      try {
         log.debug("setSpringContext");
         Field f = SpringSecurityActuator.class.getDeclaredField("context");
         f.setAccessible(true);
         ApplicationContext appCtx = (ApplicationContext) f.get(null);
         if (appCtx instanceof WebApplicationContext)
            return;

         springTempContext = appCtx;
         f.set(null, springWebContext);

      } catch (Exception e) {
         throw new ServletException(e);
      }
   }

   private void resetSpringContext() throws ServletException {
      if (springTempContext == null)
         return;
      try {
         Field f = SpringSecurityActuator.class.getDeclaredField("context");
         f.setAccessible(true);
         f.set(null, springTempContext);
         springTempContext = null;

      } catch (Exception e) {
         throw new ServletException(e);
      }
   }

   private void doConfig(ServletRequest req, ServletResponse resp) throws ServletException, IOException {
      log.debug("servlet config called");
      if ("1".equals(req.getParameter("expVoter"))) {
         // add WebExpressionVoter
         WebApplicationContext ctx = WebApplicationContextUtils
               .getRequiredWebApplicationContext(req.getServletContext());
         AffirmativeBased bean = ctx.getBean(AffirmativeBased.class);
         boolean isPresent = false;
         for (AccessDecisionVoter v : bean.getDecisionVoters()) {
            log.debug("voter class: " + v);
            if (v instanceof WebExpressionVoter) {
               isPresent = true;
               break;
            }
         }
         if (!isPresent) {
            bean.getDecisionVoters().add(webVoter);
         }

         // special for Jetty:
         try {
            Field f = SpringSecurityActuator.class.getDeclaredField("context");
            f.setAccessible(true);
            ApplicationContext appCtx = (ApplicationContext) f.get(null);
            bean = appCtx.getBean(AffirmativeBased.class);
            for (AccessDecisionVoter v : bean.getDecisionVoters()) {
               log.debug("voter class: " + v);
               if (v instanceof WebExpressionVoter) {
                  isPresent = true;
                  break;
               }
            }
            if (!isPresent) {
               bean.getDecisionVoters().add(webVoter);
            }

         } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServletException(e);
         }

      } else if ("0".equals(req.getParameter("expVoter"))) {
         // remove WebExpressionVoter
         WebApplicationContext ctx = WebApplicationContextUtils
               .getRequiredWebApplicationContext(req.getServletContext());
         AffirmativeBased bean = ctx.getBean(AffirmativeBased.class);
         Iterator<AccessDecisionVoter> it = bean.getDecisionVoters().iterator();
         while (it.hasNext()) {
            AccessDecisionVoter v = it.next();
            if (v instanceof WebExpressionVoter) {
               log.debug("remove voter class: " + v + " from AffirmativeBased " + bean);
               it.remove();
            }
         }

         // special for Jetty:
         try {
            Field f = SpringSecurityActuator.class.getDeclaredField("context");
            f.setAccessible(true);
            ApplicationContext appCtx = (ApplicationContext) f.get(null);
            bean = appCtx.getBean(AffirmativeBased.class);
            it = bean.getDecisionVoters().iterator();
            while (it.hasNext()) {
               AccessDecisionVoter v = it.next();
               if (v instanceof WebExpressionVoter) {
                  log.debug("remove voter class: " + v + " from AffirmativeBased " + bean);
                  it.remove();
               }
            }

         } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServletException(e);
         }
      }
   }

}
