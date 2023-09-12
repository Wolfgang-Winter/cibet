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
/**
 * 
 */
package com.logitags.cibet.actuator.springsecurity;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.intercept.aopalliance.MethodSecurityInterceptor;
import org.springframework.security.access.method.MethodSecurityMetadataSource;
import org.springframework.security.access.vote.AbstractAccessDecisionManager;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.expression.WebExpressionVoter;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.stereotype.Component;

import com.logitags.cibet.actuator.common.AbstractActuator;
import com.logitags.cibet.actuator.common.DeniedException;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalSessionScope;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.http.HttpRequestResource;
import com.logitags.cibet.sensor.pojo.MethodResource;

/**
 *
 */
@Component
public class SpringSecurityActuator extends AbstractActuator implements ApplicationContextAware {

   /**
    * 
    */
   private static final long serialVersionUID = 6164954567965544101L;

   private static Log log = LogFactory.getLog(SpringSecurityActuator.class);

   private static Pattern hasRolePattern = Pattern.compile("hasRole\\s*\\(\\s*'?\"?(.*?)\"?'?\\s*\\)",
         Pattern.CASE_INSENSITIVE);
   private static Pattern hasAnyRolePattern = Pattern.compile("hasAnyRole\\s*\\((\\s*'?\"?.*?\"?'?\\s*)\\)",
         Pattern.CASE_INSENSITIVE);

   private static ApplicationContext context;

   private boolean throwDeniedException = false;

   private String preAuthorize;

   private String preFilter;

   private String postAuthorize;

   private String postFilter;

   private String secured;

   private Boolean denyAll = false;

   private Boolean permitAll = false;

   private String rolesAllowed;

   private String urlAccess;
   private boolean urlAccessExpression;

   private boolean secondPrincipal = false;

   public static final String DEFAULTNAME = "SPRING_SECURITY";

   public SpringSecurityActuator() {
      setName(DEFAULTNAME);
   }

   public SpringSecurityActuator(String name) {
      setName(name);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.core.AbstractActuator#beforeEvent(com.logitags.cibet
    * .core.EventMetadata)
    */
   @Override
   public void beforeEvent(EventMetadata ctx) {
      if (ctx.getResource() instanceof HttpRequestResource) {
         beforeHttp(ctx, (HttpRequestResource) ctx.getResource());
      } else {
         Object[] arguments = new Object[ctx.getResource().getParameters().size()];
         int i = 0;
         Iterator<ResourceParameter> iter = ctx.getResource().getParameters().iterator();
         while (iter.hasNext()) {
            arguments[i] = iter.next().getUnencodedValue();
            i++;
         }

         Method method = null;
         if (ctx.getResource() instanceof MethodResource) {
            method = ((MethodResource) ctx.getResource()).getMethodObject();
         }

         CibetMethodInvocation methodInv = new CibetMethodInvocation(ctx.getResource().getUnencodedTargetObject(),
               method, arguments, ctx.getSetpointIds() + this.getName(), null);
         before(ctx, methodInv);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.core.AbstractActuator#afterEvent(com.logitags.cibet
    * .core.EventMetadata)
    */
   @Override
   public void afterEvent(EventMetadata ctx) {
      if (ctx.getExecutionStatus() == ExecutionStatus.ERROR) {
         log.info("ERROR detected. Skip afterEvent of " + this.getClass().getSimpleName());
         return;
      }

      switch (ctx.getControlEvent()) {
      case INVOKE:
      case RELEASE_INVOKE:
      case FIRST_RELEASE_INVOKE:
      case REJECT_INVOKE:
      case REDO:
      case SUBMIT_INVOKE:
      case PASSBACK_INVOKE:
         afterInvoke(ctx);
         break;
      default:
         break;
      }
   }

   public void afterInvoke(EventMetadata ctx) {
      if (ctx.getResource() instanceof HttpRequestResource)
         return;

      // 1. if no Post-rules --> return
      if (postAuthorize == null && postFilter == null) {
         log.debug("no post-invocation rules defined");
         return;
      }

      // 2. Create CibetMethodInvocation
      Object[] arguments = new Object[ctx.getResource().getParameters().size()];
      int i = 0;
      Iterator<ResourceParameter> iter = ctx.getResource().getParameters().iterator();
      while (iter.hasNext()) {
         arguments[i] = iter.next().getUnencodedValue();
         i++;
      }

      Method method = null;
      if (ctx.getResource() instanceof MethodResource) {
         method = ((MethodResource) ctx.getResource()).getMethodObject();
      }

      CibetMethodInvocation methodInv = new CibetMethodInvocation(ctx.getResource().getUnencodedTargetObject(), method,
            arguments, ctx.getSetpointIds() + this.getName(), ctx.getResource().getResultObject());

      // 2.c set pre- and post access roles
      if (preAuthorize != null)
         methodInv.addRule(CibetMethodInvocation.PREAUTHORIZE_RULE, preAuthorize);
      if (preFilter != null)
         methodInv.addRule(CibetMethodInvocation.PREFILTER_RULE, preFilter);
      if (postAuthorize != null)
         methodInv.addRule(CibetMethodInvocation.POSTAUTHORIZE_RULE, postAuthorize);
      if (postFilter != null)
         methodInv.addRule(CibetMethodInvocation.POSTFILTER_RULE, postFilter);

      // 3. Get MethodSecurityInterceptor from Spring context
      MethodSecurityInterceptor interceptor;
      try {
         if (context == null) {
            throw new IllegalStateException(
                  "Failure in Spring configuration: org.springframework.context.ApplicationContext not injected in SpringSecurityActuator");
         }
         interceptor = context.getBean(MethodSecurityInterceptor.class);
      } catch (NoSuchBeanDefinitionException e1) {
         String msg = "Failed to find a MethodSecurityInterceptor bean in Spring context. Configure Spring context correctly: "
               + e1.getMessage();
         log.error(msg);
         throw new RuntimeException(msg, e1);
      }

      // 4. Set metaData class
      initInterceptor(interceptor);

      Authentication originalAuth = null;
      try {
         originalAuth = swapAuthentication();

         ctx.getResource().setResultObject(null);
         Object result = interceptor.invoke(methodInv);
         ctx.getResource().setResultObject(result);
         log.debug("Access granted after method");
      } catch (AccessDeniedException ae) {
         handleDeniedException(ctx, ae);
      } catch (AuthenticationCredentialsNotFoundException e) {
         handleDeniedException(ctx, e);
      } catch (Throwable te) {
         log.error(te.getMessage(), te);
         throw new RuntimeException(te.getMessage());
      } finally {
         if (originalAuth != null) {
            SecurityContextHolder.getContext().setAuthentication(originalAuth);
         }
      }
   }

   protected void initInterceptor(MethodSecurityInterceptor interceptor) {
      MethodSecurityMetadataSource metaDatasource = interceptor.getSecurityMetadataSource();
      if (metaDatasource == null) {
         String msg = "Configuration error: MethodSecurityInterceptor bean "
               + "has not set an instance SecurityMetadataSource";
         log.error(msg);
         throw new RuntimeException(msg);
      }
      if (metaDatasource instanceof CibetDelegatingMethodSecurityMetadataSource) {
         return;
      }
      CibetDelegatingMethodSecurityMetadataSource cibetDel = new CibetDelegatingMethodSecurityMetadataSource();
      cibetDel.setOriginalMetadataSource(metaDatasource);
      interceptor.setSecurityMetadataSource(cibetDel);
      log.debug("replace existing " + metaDatasource.getClass().getName()
            + " against CibetDelegatingMethodSecurityMetadataSource");
   }

   protected void initInterceptor(FilterSecurityInterceptor interceptor) {
      FilterInvocationSecurityMetadataSource metaDatasource = interceptor.getSecurityMetadataSource();
      if (metaDatasource == null) {
         String msg = "Configuration error: FilterSecurityInterceptor bean "
               + "has not set an instance SecurityMetadataSource";
         log.error(msg);
         throw new RuntimeException(msg);
      }
      if (metaDatasource instanceof CibetFilterInvocationSecurityMetadataSource) {
         return;
      }
      CibetFilterInvocationSecurityMetadataSource cibetDel = new CibetFilterInvocationSecurityMetadataSource(
            metaDatasource);
      interceptor.setSecurityMetadataSource(cibetDel);
      log.debug("replace existing " + metaDatasource.getClass().getName()
            + " against CibetFilterInvocationSecurityMetadataSource");
   }

   /**
    * Fixes comma errors
    * 
    * @param rule rule
    * @return fixed rule
    */
   protected String fixRule(String rule) {
      if (rule == null || rule.length() == 0)
         return rule;
      rule = rule.trim();

      Matcher m = hasAnyRolePattern.matcher(rule);
      int end = 0;
      StringBuffer sb = new StringBuffer();
      while (m.find()) {
         String group1 = m.group(1);
         group1 = group1.replaceAll("[\"']", "");
         sb.append(rule.substring(end, m.start(1)));

         StringBuffer sb2 = new StringBuffer();
         boolean first = true;
         StringTokenizer tok = new StringTokenizer(group1, ",");
         while (tok.hasMoreTokens()) {
            String token = tok.nextToken().trim();
            if (!first) {
               sb2.append(", ");
            } else {
               first = false;
            }
            sb2.append("'");
            sb2.append(token);
            sb2.append("'");
         }
         sb.append(sb2);
         end = m.end(1);
      }
      sb.append(rule.substring(end));

      m = hasRolePattern.matcher(sb.toString());
      String replacement = "hasRole('$1')";
      String s = m.replaceAll(replacement);
      log.debug(s);
      return s;
   }

   public void setApplicationContext(ApplicationContext ctx) throws BeansException {
      log.debug("setting context " + ctx);
      context = ctx;

   }

   /**
    * @return the throwDeniedException
    */
   public boolean isThrowDeniedException() {
      return throwDeniedException;
   }

   /**
    * @param throwD the throwDeniedException to set
    */
   public void setThrowDeniedException(boolean throwD) {
      this.throwDeniedException = throwD;
   }

   protected void before(EventMetadata ctx, CibetMethodInvocation methodInv) {
      // 1. if no Pre-rules --> return
      if (preAuthorize == null && preFilter == null && secured == null && denyAll == false && permitAll == false
            && rolesAllowed == null) {
         log.warn("no before access rules defined");
         return;
      }

      if (log.isDebugEnabled()) {
         log.debug("[secondPrincipal: " + secondPrincipal + "]\n" + CibetMethodInvocation.PREAUTHORIZE_RULE + ": "
               + preAuthorize + ", " + CibetMethodInvocation.PREFILTER_RULE + ": " + preFilter + ", "
               + CibetMethodInvocation.SECURED_RULE + ": " + secured + ", " + CibetMethodInvocation.JSR250_DENYALL_RULE
               + ": " + denyAll + ", " + CibetMethodInvocation.JSR250_PERMITALL_RULE + ": " + permitAll + ", "
               + CibetMethodInvocation.JSR250_ROLESALLOWED_RULE + ": " + rolesAllowed);
      }

      // 2. set pre- access roles in CibetContext
      if (preAuthorize != null)
         methodInv.addRule(CibetMethodInvocation.PREAUTHORIZE_RULE, preAuthorize);
      if (preFilter != null)
         methodInv.addRule(CibetMethodInvocation.PREFILTER_RULE, preFilter);
      if (secured != null)
         methodInv.addRule(CibetMethodInvocation.SECURED_RULE, secured);
      if (denyAll == true)
         methodInv.addRule(CibetMethodInvocation.JSR250_DENYALL_RULE, "true");
      if (permitAll == true)
         methodInv.addRule(CibetMethodInvocation.JSR250_PERMITALL_RULE, "true");
      if (rolesAllowed != null)
         methodInv.addRule(CibetMethodInvocation.JSR250_ROLESALLOWED_RULE, rolesAllowed);

      // 3. Get MethodSecurityInterceptor from Spring context
      MethodSecurityInterceptor interceptor;
      try {
         if (context == null) {
            throw new IllegalStateException(
                  "Failure in Spring configuration: org.springframework.context.ApplicationContext not injected in SpringSecurityActuator");
         }
         interceptor = context.getBean(MethodSecurityInterceptor.class);
      } catch (NoSuchBeanDefinitionException e1) {
         String msg = "Failed to find a MethodSecurityInterceptor bean in Spring context. Configure Spring context correctly: "
               + e1.getMessage();
         log.error(msg);
         throw new RuntimeException(msg, e1);
      }

      // 4. Set metaData class
      initInterceptor(interceptor);

      Authentication originalAuth = null;
      try {
         originalAuth = swapAuthentication();

         log.debug("before interceptor invoke");
         interceptor.invoke(methodInv);
         log.debug("Access granted");
      } catch (AccessDeniedException ae) {
         handleDeniedException(ctx, ae);

      } catch (AuthenticationCredentialsNotFoundException e) {
         handleDeniedException(ctx, e);

      } catch (Throwable te) {
         log.error(te.getMessage(), te);
         throw new RuntimeException(te.getMessage());
      } finally {
         if (originalAuth != null) {
            SecurityContextHolder.getContext().setAuthentication(originalAuth);
         }
      }
   }

   private void handleDeniedException(EventMetadata ctx, RuntimeException e) {
      ctx.setExecutionStatus(ExecutionStatus.DENIED);
      String deniedUser = null;
      if (secondPrincipal) {
         log.warn("Access denied for user " + Context.internalSessionScope().getSecondUser() + ": " + e.getMessage());
         deniedUser = Context.internalSessionScope().getSecondUser();
      } else {
         log.warn("Access denied for user " + Context.internalSessionScope().getUser() + ": " + e.getMessage());
         deniedUser = Context.internalSessionScope().getUser();
      }

      if (throwDeniedException) {
         DeniedException ex = new DeniedException(e.getMessage(), e, deniedUser);
         ctx.setException(ex);
      }
   }

   protected void beforeHttp(EventMetadata ctx, HttpRequestResource resource) {
      log.debug(this + ", context=" + context);
      // 1. if no rules --> return
      if (urlAccess == null) {
         log.debug("no access rules defined");
         return;
      }

      // 2. Create CibetFilterInvocation
      CibetFilterInvocation filterInv = new CibetFilterInvocation(
            new DummyServletRequest(resource.getHttpRequestData(), resource.getHttpRequest()),
            new DummyServletResponse(), new DummyFilterChain());

      if (urlAccessExpression) {
         log.debug("create CibetFilterInvocation with access expression rule: " + urlAccess);
         filterInv.setAccessRuleExpression(urlAccess);

      } else {
         log.debug("create CibetFilterInvocation with simple access rule: " + urlAccess);
         filterInv.setAccessRule(urlAccess);
      }

      // 3. Get MethodSecurityInterceptor from Spring context
      FilterSecurityInterceptor interceptor;
      try {
         if (context == null) {
            throw new IllegalStateException(
                  "Failure in Spring configuration: org.springframework.context.ApplicationContext not injected in SpringSecurityActuator");
         }
         interceptor = context.getBean(FilterSecurityInterceptor.class);
      } catch (NoSuchBeanDefinitionException e1) {
         String msg = "Failed to find a FilterSecurityInterceptor bean in Spring context. Configure Spring context correctly: "
               + e1.getMessage();
         log.error(msg);
         throw new RuntimeException(msg, e1);
      }

      // 4. Set metaData class
      initInterceptor(interceptor);

      Authentication originalAuth = null;
      try {
         originalAuth = swapAuthentication();

         log.debug("before interceptor invoke");
         interceptor.invoke(filterInv);
         log.debug("Access granted");
      } catch (AccessDeniedException ae) {
         handleDeniedException(ctx, ae);
      } catch (AuthenticationCredentialsNotFoundException e) {
         handleDeniedException(ctx, e);
      } catch (Throwable te) {
         log.error(te.getMessage(), te);
         throw new RuntimeException(te.getMessage());
      } finally {
         if (originalAuth != null) {
            SecurityContextHolder.getContext().setAuthentication(originalAuth);
         }
      }
   }

   /**
    * @return the preAuthorize
    */
   public String getPreAuthorize() {
      return preAuthorize;
   }

   /**
    * @param pa the preAuthorize to set
    */
   public void setPreAuthorize(String pa) {
      preAuthorize = fixRule(pa);
   }

   /**
    * @return the preFilter
    */
   public String getPreFilter() {
      return preFilter;
   }

   /**
    * @param pf the preFilter to set
    */
   public void setPreFilter(String pf) {
      preFilter = fixRule(pf);
   }

   /**
    * @return the postAuthorize
    */
   public String getPostAuthorize() {
      return postAuthorize;
   }

   /**
    * @param pa the postAuthorize to set
    */
   public void setPostAuthorize(String pa) {
      postAuthorize = fixRule(pa);
   }

   /**
    * @return the postFilter
    */
   public String getPostFilter() {
      return postFilter;
   }

   /**
    * @param pf the postFilter to set
    */
   public void setPostFilter(String pf) {
      postFilter = fixRule(pf);
   }

   /**
    * @return the secured
    */
   public String getSecured() {
      return secured;
   }

   /**
    * @param s the secured to set
    */
   public void setSecured(String s) {
      secured = s;
   }

   /**
    * @return the denyAll
    */
   public Boolean getDenyAll() {
      return denyAll;
   }

   /**
    * @param value the denyAll to set
    */
   public void setDenyAll(Boolean value) {
      if (value == null) {
         this.denyAll = true;
      } else {
         this.denyAll = value;
      }
      if (denyAll == true)
         permitAll = false;
   }

   /**
    * @return the permitAll
    */
   public Boolean getPermitAll() {
      return permitAll;
   }

   /**
    * @param pa the permitAll to set
    */
   public void setPermitAll(Boolean pa) {
      if (pa == null) {
         this.permitAll = true;
      } else {
         this.permitAll = pa;
      }
      if (permitAll == true)
         denyAll = false;
   }

   /**
    * @return the rolesAllowed
    */
   public String getRolesAllowed() {
      return rolesAllowed;
   }

   /**
    * @param ra the rolesAllowed to set
    */
   public void setRolesAllowed(String ra) {
      rolesAllowed = ra;
   }

   /**
    * @return the interceptUrlAccess
    */
   public String getUrlAccess() {
      return urlAccess;
   }

   /**
    * @param interceptUrlAccess the interceptUrlAccess to set
    */
   public void setUrlAccess(String interceptUrlAccess) {
      this.urlAccess = fixRule(interceptUrlAccess);
      checkUrlAccessExpression();
   }

   private void checkUrlAccessExpression() {
      log.debug("check if URL access expressions are allowed");
      String[] names = context.getBeanDefinitionNames();
      for (String name : names) {
         if (context.getBean(name) instanceof AbstractAccessDecisionManager) {
            for (AccessDecisionVoter v : ((AbstractAccessDecisionManager) context.getBean(name)).getDecisionVoters()) {
               if (v instanceof WebExpressionVoter) {
                  log.debug("parse urlAccess as EL expression");
                  urlAccessExpression = true;
                  return;
               }
            }
         }
      }

      log.debug("parse urlAccess as simple expression");
      urlAccessExpression = false;
   }

   private Authentication swapAuthentication() {
      if (secondPrincipal) {
         Object secP = Context.internalSessionScope().getProperty(InternalSessionScope.SECOND_PRINCIPAL);
         if (secP == null) {
            throw new AuthenticationCredentialsNotFoundException(
                  "No Authentication object found in CibetContext.getSecondPrincipal()");
         }

         if (!(secP instanceof Authentication)) {
            throw new AccessDeniedException("CibetContext.getSecondPrincipal() is expected to be of type "
                  + Authentication.class.getName() + " but is of type " + secP.getClass().getName());
         }

         log.debug("SpringSecurity actuator for second principal " + secP);
         Authentication auth = (Authentication) secP;
         Authentication original = SecurityContextHolder.getContext().getAuthentication();
         SecurityContextHolder.getContext().setAuthentication(auth);
         return original;
      }
      return null;
   }

   /**
    * @return the secondPrincipal
    */
   public boolean isSecondPrincipal() {
      return secondPrincipal;
   }

   /**
    * @param secondP the secondPrincipal to set
    */
   public void setSecondPrincipal(boolean secondP) {
      this.secondPrincipal = secondP;
   }

}
