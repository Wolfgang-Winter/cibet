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
package com.logitags.cibet.context;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.authentication.AuthenticationProvider;
import com.logitags.cibet.authentication.IPAuthenticationProvider;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.ProxyConfig;

public class CibetContextFilter implements Filter {

   private static Log log = LogFactory.getLog(CibetContextFilter.class);

   public static final String CIBETEECONTEXTEJB_JNDINAME = "EJB_JNDI_NAME";
   private static final String EXCLUDES = "excludes";

   protected String EJB_JNDINAME;

   private static final String ALLOW_ANONYMOUS = "allowAnonymous";

   private List<Pattern> excludePattern = new ArrayList<>();

   protected boolean allowAnonymous = false;

   @Override
   public void init(FilterConfig config) throws ServletException {
      EJB_JNDINAME = config.getInitParameter(CIBETEECONTEXTEJB_JNDINAME);
      if (EJB_JNDINAME != null) {
         log.debug("init CibetFilter " + this + " with EJB_JNDI_NAME =" + EJB_JNDINAME);
      } else {
         log.info("init CibetFilter " + this);
      }

      excludePattern = resolveExcludePatterns(config, EXCLUDES);

      String permit = config.getInitParameter(ALLOW_ANONYMOUS);
      if ("TRUE".equalsIgnoreCase(permit)) {
         log.info("allow anonymous access");
         allowAnonymous = true;
      }

      // init EMF
      try {
         this.getClass().getClassLoader().loadClass(Context.class.getName());
      } catch (ClassNotFoundException e) {
         log.error(e.getMessage(), e);
      }

      // initialize Configuration
      Map<String, ProxyConfig> proxyConfigs = loadProxyFilterConfig(config);
      Configuration.instance().startProxyOverrideWithSystemConfig(proxyConfigs);
   }

   @Override
   public void destroy() {
      Context.close();

      try {
         MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
         ObjectName oname = new ObjectName(Configuration.class.getPackage().getName() + ":type=ConfigurationService_"
               + Configuration.instance().getApplicationName());
         if (mbs.isRegistered(oname)) {
            mbs.unregisterMBean(oname);
            log.info("unregister MBean " + oname.getCanonicalName());
         }

      } catch (Exception e) {
         log.warn("Failed to unregister ConfigurationService MBean: " + e.getMessage(), e);
      }

      Configuration.instance().close();
   }

   @Override
   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
         throws IOException, ServletException {
      if (isExclude(request)) {
         chain.doFilter(request, response);
         return;
      }

      log.debug("execute CibetContextFilter URL " + ((HttpServletRequest) request).getRequestURL());
      AuthenticationProvider auth = null;
      boolean startManaging = true;
      if (allowAnonymous) {
         auth = new IPAuthenticationProvider();
      }

      try {
         startManaging = Context.start(EJB_JNDINAME, auth);
         fillCibetContext((HttpServletRequest) request);
         chain.doFilter(request, response);

         if (log.isDebugEnabled()) {
            HttpSession session = ((HttpServletRequest) request).getSession(false);
            if (session != null) {
               HashMap<String, Object> props = (HashMap<String, Object>) session
                     .getAttribute(SessionScopeContext.KEY_SESSION_PROPERTIES);
               if (props != null) {
                  log.debug("size: " + props.size());
                  for (Entry<String, Object> entry : props.entrySet()) {
                     log.debug(entry.getKey() + " = " + entry.getValue());
                  }
               }
            }
         }
      } finally {
         if (startManaging) {
            Context.end();
         } else {
            Context.internalRequestScope().getAuthenticationProvider().getProviderChain().remove(auth);
         }
      }
   }

   protected void fillCibetContext(HttpServletRequest httpReq) {
      Context.internalSessionScope().setHttpRequest(httpReq);

      HttpSession session = httpReq.getSession();
      HashMap<String, Object> props = (HashMap<String, Object>) session
            .getAttribute(SessionScopeContext.KEY_SESSION_PROPERTIES);
      if (props != null) {
         if (log.isDebugEnabled()) {
            log.debug("fill CibetContext from http session");
            log.debug("size: " + props.size());
            for (Entry<String, Object> entry : props.entrySet()) {
               log.debug(entry.getKey() + " = " + entry.getValue());
            }
         }
         Context.internalSessionScope().getProperties().putAll(props);
      }
   }

   protected boolean isExclude(ServletRequest request) {
      String uri = ((HttpServletRequest) request).getRequestURI().toString();
      for (Pattern pattern : excludePattern) {
         Matcher m = pattern.matcher(uri);
         if (m.matches()) {
            log.debug("skip " + uri);
            return true;
         }
      }
      return false;
   }

   private List<Pattern> resolveExcludePatterns(FilterConfig config, String param) {
      List<Pattern> patterns = new ArrayList<>();
      String excludes = config.getInitParameter(param);
      if (excludes != null) {
         StringTokenizer tok = new StringTokenizer(excludes, ",");
         while (tok.hasMoreTokens()) {
            String exclude = tok.nextToken().trim();
            patterns.add(Pattern.compile(exclude));
         }
      }
      return patterns;
   }

   private Map<String, ProxyConfig> loadProxyFilterConfig(FilterConfig config) throws ServletException {
      Map<String, ProxyConfig> proxyConfigs = new HashMap<>();
      Map<String, String> configMap = new HashMap<>();
      Enumeration<String> initKeys = config.getInitParameterNames();
      while (initKeys.hasMoreElements()) {
         String initKey = initKeys.nextElement();
         if (initKey.startsWith(Configuration.PROXY_PREFIX)) {
            configMap.put(initKey, config.getInitParameter(initKey));
         }
      }

      Configuration.instance().resolveProxyConfigParams(proxyConfigs, configMap);
      return proxyConfigs;
   }

}
