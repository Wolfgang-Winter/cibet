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
package com.logitags.cibet.config;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.littleshoot.proxy.ChainedProxyManager;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.logitags.cibet.actuator.common.Actuator;
import com.logitags.cibet.actuator.loadcontrol.LoadControlCallback;
import com.logitags.cibet.actuator.loadcontrol.Monitor;
import com.logitags.cibet.actuator.loadcontrol.MonitorStatus;
import com.logitags.cibet.actuator.owner.OwnerCheckCallback;
import com.logitags.cibet.actuator.scheduler.SchedulerTaskInterceptor;
import com.logitags.cibet.authentication.AuthenticationProvider;
import com.logitags.cibet.authentication.ChainedAuthenticationProvider;
import com.logitags.cibet.authentication.EEAuthenticationProvider;
import com.logitags.cibet.authentication.HttpHeaderAuthenticationProvider;
import com.logitags.cibet.authentication.HttpRequestAuthenticationProvider;
import com.logitags.cibet.authentication.HttpSessionAuthenticationProvider;
import com.logitags.cibet.bindings.ActuatorBinding;
import com.logitags.cibet.bindings.Cibet;
import com.logitags.cibet.bindings.ClassDefBinding;
import com.logitags.cibet.bindings.ControlDefBinding;
import com.logitags.cibet.bindings.CustomControlBinding;
import com.logitags.cibet.bindings.InExBinding;
import com.logitags.cibet.bindings.ObjectFactory;
import com.logitags.cibet.bindings.PropertiesBinding;
import com.logitags.cibet.bindings.SetpointActuatorBinding;
import com.logitags.cibet.bindings.SetpointBinding;
import com.logitags.cibet.config.ProxyConfig.ProxyMode;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalRequestScope;
import com.logitags.cibet.control.ConcreteControl;
import com.logitags.cibet.control.ConditionControl;
import com.logitags.cibet.control.Control;
import com.logitags.cibet.control.Controller;
import com.logitags.cibet.control.EventControl;
import com.logitags.cibet.control.InvokerControl;
import com.logitags.cibet.control.MethodControl;
import com.logitags.cibet.control.StateChangeControl;
import com.logitags.cibet.control.TargetControl;
import com.logitags.cibet.control.TenantControl;
import com.logitags.cibet.notification.NotificationProvider;
import com.logitags.cibet.security.DefaultSecurityProvider;
import com.logitags.cibet.security.SecurityProvider;
import com.logitags.cibet.sensor.http.ChainedProxyManagerImpl;
import com.logitags.cibet.sensor.http.CibetHttpFiltersSource;
import com.logitags.cibet.sensor.http.CibetMitmManager;
import com.udojava.jmx.wrapper.JMXBeanWrapper;

/**
 * Manages the overall configuration of Cibet framework. Loads and caches ControlConfiguration objects from
 * cibet-config.xml.
 */
public class Configuration {

   /**
    * logger for tracing
    */
   private static Log log = LogFactory.getLog(Configuration.class);

   private static String CONFIGURATION_FILENAME = "cibet-config.xml";

   private static String CONFIGURATION_XSD_FILENAME = "cibet-config_1.4.xsd";

   public static final String JMX_BASE = "com.logitags.cibet";
   private static final String JMX_OBJECTNAME = JMX_BASE + ":type=Configuration,app=" + getApplicationName();

   public static final String PROXY_PREFIX = "cibet.proxy.";
   private static final String PROXY_MODE = "cibet.proxy.mode";
   private static final String PROXY_PORT = "cibet.proxy.port";
   private static final String PROXY_CHAINEDPROXYHOST = "cibet.proxy.chainedProxyHost";
   private static final String PROXY_CHAINEDPROXYPORT = "cibet.proxy.chainedProxyPort";
   private static final String PROXY_BUFFERSIZE = "cibet.proxy.bufferSize";
   private static final String PROXY_TIMEOUT = "cibet.proxy.timeout";
   private static final String PROXY_CLIENT_KEYSTORE = "cibet.proxy.clientKeystore";
   private static final String PROXY_CLIENT_KEYSTOREPASSWORD = "cibet.proxy.clientKeystorePassword";
   private static final String PROXY_EXCLUDES = "cibet.proxy.excludes";

   private static final String TAG_INCLUDE = "include";
   private static final String TAG_EXCLUDE = "exclude";

   private static Map<String, Actuator> actuators = Collections.synchronizedMap(new LinkedHashMap<String, Actuator>());

   private static Map<String, Control> controls = Collections.synchronizedMap(new LinkedHashMap<String, Control>());

   private static Map<String, Setpoint> setpoints = Collections.synchronizedMap(new LinkedHashMap<String, Setpoint>());

   private ChainedAuthenticationProvider chainedAuthenticationProvider = new ChainedAuthenticationProvider();

   private static String applicationName;

   private NotificationProvider notificationProvider;

   private SecurityProvider securityProvider = new DefaultSecurityProvider();

   public Map<String, HttpProxyServer> proxyServers = new HashMap<>();

   private Map<String, ProxyConfig> proxyConfigs = new HashMap<>();

   private static Configuration instance;

   public static synchronized Configuration instance() {
      if (instance == null) {
         instance = new Configuration();
      }
      return instance;
   }

   private Configuration() {
      initialise();
   }

   public static String getApplicationName() {
      if (applicationName == null) {
         String appName = null;
         try {
            appName = (String) new InitialContext().lookup("java:app/ModuleName");
         } catch (NamingException e) {
            log.info("Failed to load java:app/ModuleName from jndi: " + e.getMessage());
         }

         if (appName == null || appName.length() == 0) {
            try {
               appName = (String) new InitialContext().lookup("java:app/AppName");
            } catch (NamingException e) {
               log.info("Failed to load java:app/AppName from jndi: " + e.getMessage());
            }
         }

         if (appName != null && appName.length() > 0) {
            applicationName = appName;
         } else {
            applicationName = "";
         }
      }
      return applicationName;
   }

   /**
    * loads control configuration from cibet-config.xml and caches them.
    * 
    * @see com.logitags.cibet.config.ConfigurationService#initialise()
    */
   public synchronized void initialise() {
      log.info("initialise Configuration " + this);

      try {
         MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
         ObjectName oname = new ObjectName(JMX_OBJECTNAME);
         if (mbs.isRegistered(oname)) {
            log.info("MBean " + oname.getCanonicalName() + " already registered");
         } else {
            log.info("start MBeanServer ...");
            ConfigurationService mbean = new ConfigurationService();
            JMXBeanWrapper wrappedBean = new JMXBeanWrapper(mbean);
            mbs.registerMBean(wrappedBean, oname);
            log.info("MBeanServer started and MBean " + oname.getCanonicalName() + " registered");
         }

      } catch (Exception e) {
         log.warn("Failed to register ConfigurationService MBean: " + e.getMessage(), e);
      }

      List<Cibet> cibets = readConfigurationFiles();

      initActuators(cibets);
      initControls(cibets);
      reinitAuthenticationProvider(cibets);
      reinitNotificationProvider(cibets);
      reinitSecurityProvider(cibets);
      reinitSetpoints(cibets);
      stopProxies();
      startProxyOverrideWithSystemConfig(proxyConfigs);

      for (Actuator act : actuators.values()) {
         act.init(this);
      }

      log.debug("initialisation finished");
   }

   /**
    * makes closing tasks on shutdown
    */
   public synchronized void close() {
      for (Actuator act : actuators.values()) {
         act.close();
      }
      stopProxies();

      try {
         MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
         ObjectName oname = new ObjectName(JMX_OBJECTNAME);
         if (mbs.isRegistered(oname)) {
            mbs.unregisterMBean(oname);
            log.info("unregister MBean " + oname.getCanonicalName());
         }

      } catch (Exception e) {
         log.warn("Failed to unregister ConfigurationService MBean: " + e.getMessage(), e);
      }
   }

   /**
    * Reinitializes the AuthenticationProvider by detecting the used security framework and re-reading the configuration
    * file.
    * 
    * @param cibets
    *           list of cibet configurations
    */
   public synchronized void reinitAuthenticationProvider(List<Cibet> cibets) {
      chainedAuthenticationProvider = new ChainedAuthenticationProvider();
      initAuthenticationProvider();
      if (cibets == null)
         cibets = readConfigurationFiles();
      if (cibets == null)
         return;
      for (Cibet cibet : cibets) {
         registerAuthenticationProviderFromBinding(cibet.getAuthenticationProvider());
      }
      Context.requestScope().removeProperty(InternalRequestScope.AUTHENTICATIONPROVIDER);
   }

   /**
    * Reinitializes the NotificationProvider by re-reading the configuration file.
    * 
    * @param cibets
    *           list of cibet configurations
    */
   public synchronized void reinitNotificationProvider(List<Cibet> cibets) {
      notificationProvider = null;
      if (cibets == null)
         cibets = readConfigurationFiles();
      if (cibets == null)
         return;
      for (Cibet cibet : cibets) {
         registerNotificationProviderFromBinding(cibet.getNotificationProvider());
      }
   }

   /**
    * Reinitializes the SecurityProvider by re-reading the configuration file.
    * 
    * @param cibets
    *           list of cibet configurations
    */
   public synchronized void reinitSecurityProvider(List<Cibet> cibets) {
      securityProvider = new DefaultSecurityProvider();
      if (cibets == null)
         cibets = readConfigurationFiles();
      if (cibets == null)
         return;
      for (Cibet cibet : cibets) {
         registerSecurityProviderFromBinding(cibet.getSecurityProvider());
      }
   }

   /**
    * Reinitializes the Actuators by re-reading the configuration file.
    */
   public synchronized void reinitActuators() {
      initActuators(null);
      reinitSetpoints(null);
      for (Actuator act : actuators.values()) {
         act.init(this);
      }
   }

   private void initActuators(List<Cibet> cibets) {
      initBuiltInActuators();
      if (cibets == null)
         cibets = readConfigurationFiles();
      if (cibets == null)
         return;
      for (Cibet cibet : cibets) {
         for (ActuatorBinding bin : cibet.getActuator()) {
            registerActuatorFromBinding(bin);
         }
      }
   }

   /**
    * Reinitializes the Controls by re-reading the configuration file.
    * 
    * @param cibets
    *           list of cibet configurations
    */
   public synchronized void reinitControls(List<Cibet> cibets) {
      initControls(cibets);
      reinitSetpoints(cibets);
   }

   /**
    * Initializes the Controls by reading the configuration file.
    * 
    * @param cibets
    *           list of cibet configurations
    */
   private void initControls(List<Cibet> cibets) {
      controls.clear();
      initBuiltInControls();
      if (cibets == null)
         cibets = readConfigurationFiles();
      if (cibets == null)
         return;
      for (Cibet cibet : cibets) {
         for (ControlDefBinding cdef : cibet.getControl()) {
            registerControlDefFromBinding(cdef);
         }
      }
   }

   /**
    * Reinitializes the Setpoints by re-reading the configuration file.
    * 
    * @param cibets
    *           list of cibet configurations
    */
   public synchronized void reinitSetpoints(List<Cibet> cibets) {
      setpoints.clear();
      Set<String> configNames = new HashSet<>();

      if (cibets == null)
         cibets = readConfigurationFiles();
      if (cibets == null)
         return;
      for (Cibet cibet : cibets) {
         if (Setpoint.CODE_CONFIGNAME.equals(cibet.getName())) {
            String err = "Failed to read configuration files: Cibet configuration name " + Setpoint.CODE_CONFIGNAME
                  + " is reserved for configuration in code";
            throw new IllegalArgumentException(err);
         }
         if (configNames.contains(cibet.getName())) {
            String err = "Failed to read configuration files: Cibet configuration name " + cibet.getName()
                  + " is doubled";
            throw new IllegalArgumentException(err);
         }
         configNames.add(cibet.getName());

         for (SetpointBinding bin : cibet.getSetpoint()) {
            Setpoint sp = new Setpoint(bin.getId(), cibet.getName());
            resolveSetpoint(sp, bin);
            registerSetpoint(sp);
         }
      }

      // resolve extends
      for (Setpoint sp : setpoints.values()) {
         if (sp.getExtendsId() != null) {
            String extId = sp.getExtendsId();
            if (extId.indexOf("/") < 0) {
               extId = sp.getConfigName() + "/" + extId;
            }

            Setpoint parent = setpoints.get(extId);
            if (parent == null) {
               String msg = "Setpoint " + sp.getCombinedId() + " extends Setpoint " + extId
                     + " but this Setpoint is unknown";
               log.error(msg);
               throw new RuntimeException(msg);
            }
            sp.setExtends(parent);
         }
      }
   }

   private List<Cibet> readConfigurationFiles() {
      List<InputStream> streams = null;
      List<Cibet> cibets = new ArrayList<>();

      try {
         streams = openConfigurationFiles();
         if (streams.isEmpty()) {
            return null;
         }

         ClassLoader loader = this.getClass().getClassLoader();
         if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
         }
         URL url = loader.getResource(CONFIGURATION_XSD_FILENAME);
         if (url == null) {
            String msg = CONFIGURATION_XSD_FILENAME + " not found in classpath";
            log.fatal(msg);
            throw new RuntimeException(msg);
         }
         SchemaFactory schemaFac = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
         Schema schema = schemaFac.newSchema(url);

         JAXBContext ctx = JAXBContext.newInstance(ObjectFactory.class);
         Unmarshaller unmarshaller = ctx.createUnmarshaller();
         unmarshaller.setSchema(schema);

         for (InputStream stream : streams) {
            Cibet cibet = (Cibet) unmarshaller.unmarshal(stream);
            cibets.add(cibet);
         }
         return cibets;

      } catch (JAXBException e) {
         log.error(e.getMessage(), e);
         throw new RuntimeException(e);
      } catch (SAXException e) {
         log.error(e.getMessage(), e);
         throw new RuntimeException(e);
      } finally {
         if (streams != null) {
            for (InputStream stream : streams) {
               try {
                  stream.close();
               } catch (IOException e) {
                  log.error(e.getMessage(), e);
               }
            }
         }
      }
   }

   /**
    * 
    * @param eval
    */
   public synchronized void registerControl(Control eval) {
      if (eval == null) {
         String msg = "failed to register Control: NULL value not allowed";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }

      if (eval.getName() == null || eval.getName().length() == 0) {
         String msg = eval.getClass().getName() + " has not a valid name: " + eval.getName();
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }

      if (controls.containsKey(eval.getName())) {
         String msg = "Cannot register Control " + eval.getClass().getName() + ". A Control of type "
               + controls.get(eval.getName()).getClass().getName() + " with name " + eval.getName()
               + " is already registered";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }

      log.info("register Control " + eval.getName());
      controls.put(eval.getName(), eval);
   }

   public Actuator getActuator(String name) {
      Actuator act = actuators.get(name);
      if (act != null) {
         return act;
      } else {
         String msg = "no Actuator registered with name " + name;
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }
   }

   public synchronized void registerActuator(Actuator act) {
      if (act == null) {
         String msg = "Failed to register actuator: NULL value not allowed";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }
      act.init(this);
      log.info("register actuator " + act.getClass().getName() + " under name " + act.getName());
      actuators.put(act.getName(), act);
   }

   public synchronized void registerSetpoint(Setpoint sp) {
      if (sp == null) {
         String msg = "failed to register Setpoint: NULL value not allowed";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }
      if (setpoints.containsKey(sp.getCombinedId())) {
         log.warn(
               "A Setpoint with ID " + sp.getCombinedId() + " exists, is already registered and will be overwritten");
      }
      log.info("register setpoint " + sp.getCombinedId());
      setpoints.put(sp.getCombinedId(), sp);
   }

   /**
    * removes the Setpoint with the given ID from the list of registered Setpoints.
    * 
    * @param setpointId
    */
   public synchronized void unregisterSetpoint(String configurationName, String setpointId) {
      if (configurationName == null) {
         configurationName = Setpoint.CODE_CONFIGNAME;
      }
      String name = configurationName + "/" + setpointId;
      Setpoint sp = setpoints.get(name);
      if (sp != null) {
         log.info("remove setpoint " + name);
         setpoints.remove(name);
      }
   }

   /**
    * removes the Control with the given name from the registered list of Controls.
    * 
    * @param name
    */
   public synchronized void unregisterControl(String name) {
      controls.remove(name);
   }

   public List<String> getControlNames() {
      return new LinkedList<String>(controls.keySet());
   }

   public Control getControl(String name) {
      return controls.get(name);
   }

   /**
    * @return the setpoints
    */
   public List<Setpoint> getSetpoints() {
      return new LinkedList<Setpoint>(setpoints.values());
   }

   /**
    * 
    * @param id
    *           combined configName + "/" + id
    * @return
    */
   public Setpoint getSetpoint(String id) {
      return setpoints.get(id);
   }

   /**
    * @return the authenticationProvider
    */
   public ChainedAuthenticationProvider getAuthenticationProvider() {
      return chainedAuthenticationProvider;
   }

   /**
    * @param provider
    *           the authenticationProvider to set
    */
   public synchronized void registerAuthenticationProvider(AuthenticationProvider provider) {
      chainedAuthenticationProvider.getProviderChain().add(0, provider);
   }

   /**
    * @return the notificationProvider
    */
   public NotificationProvider getNotificationProvider() {
      return notificationProvider;
   }

   /**
    * @param np
    *           the notificationProvider to set
    */
   public synchronized void registerNotificationProvider(NotificationProvider np) {
      notificationProvider = np;
   }

   /**
    * @return the securityService
    */
   public synchronized SecurityProvider getSecurityProvider() {
      if (securityProvider == null) {
         String err = "No SecurityProvider configured";
         log.error(err);
         throw new RuntimeException(err);
      }
      return securityProvider;
   }

   /**
    * @param securityService
    *           the securityService to set
    */
   public synchronized void registerSecurityProvider(SecurityProvider securityService) {
      this.securityProvider = securityService;
   }

   private String listToString(List<String> list) {
      StringBuffer b = new StringBuffer();
      b.append(" , ");
      for (String s : list) {
         b.append(" , ");
         b.append(s);
      }
      return b.toString().substring(1);
   }

   private void resolveSetpoint(Setpoint sp, SetpointBinding spb) {
      if (spb.getExtends() != null) {
         sp.setExtendsId(((SetpointBinding) spb.getExtends()).getId());
      }

      Map<String, ConcreteControl> controls = new HashMap<>();

      ConcreteControl cc = resolveInExType(spb.getControls().getTenant(), TenantControl.NAME);
      if (cc != null) {
         controls.put(cc.getControl().getName(), cc);
      }
      cc = resolveInExType(spb.getControls().getEvent(), EventControl.NAME);
      if (cc != null) {
         controls.put(cc.getControl().getName(), cc);
      }
      cc = resolveInExType(spb.getControls().getInvoker(), InvokerControl.NAME);
      if (cc != null) {
         controls.put(cc.getControl().getName(), cc);
      }
      cc = resolveInExType(spb.getControls().getMethod(), MethodControl.NAME);
      if (cc != null) {
         controls.put(cc.getControl().getName(), cc);
      }
      cc = resolveInExType(spb.getControls().getStateChange(), StateChangeControl.NAME);
      if (cc != null) {
         controls.put(cc.getControl().getName(), cc);
      }
      cc = resolveInExType(spb.getControls().getTarget(), TargetControl.NAME);
      if (cc != null) {
         controls.put(cc.getControl().getName(), cc);
      }
      cc = resolveInExType(spb.getControls().getCondition(), ConditionControl.NAME);
      if (cc != null) {
         controls.put(cc.getControl().getName(), cc);
      }

      if (spb.getControls().getCustomControls() != null) {
         for (CustomControlBinding custCB : spb.getControls().getCustomControls().getCustomControl()) {
            cc = resolveInExType(custCB, custCB.getName());
            if (cc != null) {
               controls.put(cc.getControl().getName(), cc);
            }
         }
      }

      sp.setControls(controls);

      for (SetpointActuatorBinding sab : spb.getActuator()) {
         Actuator act = getActuator(sab.getName());
         log.info("resolve actuator " + sab.getName());
         sp.getActuators().add(act);
      }
   }

   private ConcreteControl resolveInExType(InExBinding type, String controlName) {
      if (type != null) {
         ConcreteControl control = new ConcreteControl(getControl(controlName));
         for (JAXBElement<?> elem : type.getIncludeOrExclude()) {
            String tag = elem.getName().getLocalPart();
            if (TAG_INCLUDE.equals(tag)) {
               String resolvedValue = Controller.resolve((String) elem.getValue());
               control.getIncludes().add(resolvedValue);
               log.info("resolve " + controlName + " control include value: " + resolvedValue);

            } else if (TAG_EXCLUDE.equals(tag)) {
               String resolvedValue = Controller.resolve((String) elem.getValue());
               control.getExcludes().add(resolvedValue);
               log.info("resolve " + controlName + " control exclude value: " + resolvedValue);
            }
         }
         return control;
      }
      return null;
   }

   private List<InputStream> openConfigurationFiles() {
      List<InputStream> streams = new ArrayList<>();

      ClassLoader loader = this.getClass().getClassLoader();
      if (loader == null) {
         loader = Thread.currentThread().getContextClassLoader();
      }
      try {
         Enumeration<URL> urls = loader.getResources(CONFIGURATION_FILENAME);

         while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            log.info("reading " + CONFIGURATION_FILENAME + " configuration file from " + url);
            streams.add(url.openStream());
         }
      } catch (IOException e) {
         log.fatal(e.getMessage(), e);
         throw new RuntimeException(e);
      }
      if (streams.isEmpty()) {
         log.warn("no configuration file " + CONFIGURATION_FILENAME + " found");
      }
      return streams;
   }

   private void registerAuthenticationProviderFromBinding(ClassDefBinding cdef) {
      if (cdef == null)
         return;
      try {
         @SuppressWarnings("unchecked")
         Class<AuthenticationProvider> clazz = (Class<AuthenticationProvider>) Class.forName(cdef.getClazz());
         AuthenticationProvider provider = clazz.newInstance();
         fillProperties(provider, cdef.getProperties());
         registerAuthenticationProvider(provider);
         log.info("register " + clazz.getName() + " as AuthenticationProvider");

      } catch (ClassNotFoundException e) {
         throw new RuntimeException(e);
      } catch (InstantiationException e) {
         throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
         throw new RuntimeException(e);
      }
   }

   private void registerNotificationProviderFromBinding(ClassDefBinding cdef) {
      if (cdef == null)
         return;
      try {
         @SuppressWarnings("unchecked")
         Class<NotificationProvider> clazz = (Class<NotificationProvider>) Class.forName(cdef.getClazz());
         NotificationProvider provider = clazz.newInstance();
         registerNotificationProvider(provider);
         fillProperties(provider, cdef.getProperties());
         log.info("Overwrite NotificationProvider: register " + clazz.getName() + " as NotificationProvider");

      } catch (ClassNotFoundException e) {
         throw new RuntimeException(e);
      } catch (InstantiationException e) {
         throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
         throw new RuntimeException(e);
      }
   }

   private void registerSecurityProviderFromBinding(ClassDefBinding secProv) {
      if (secProv == null)
         return;
      try {
         @SuppressWarnings("unchecked")
         Class<SecurityProvider> clazz = (Class<SecurityProvider>) Class.forName(secProv.getClazz());
         SecurityProvider provider = clazz.newInstance();
         registerSecurityProvider(provider);
         fillProperties(provider, secProv.getProperties());
         log.info("register " + clazz.getName() + " as SecurityProvider");

      } catch (ClassNotFoundException e) {
         throw new RuntimeException(e);
      } catch (InstantiationException e) {
         throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
         throw new RuntimeException(e);
      }
   }

   private void registerControlDefFromBinding(ControlDefBinding cdef) {
      try {
         @SuppressWarnings("unchecked")
         Class<Control> clazz = (Class<Control>) Class.forName(cdef.getClazz());
         Control customControl = clazz.newInstance();
         if (!cdef.getName().equals(customControl.getName())) {
            String msg = "name attribute '" + cdef.getName()
                  + "' in control configuration must be equal to name property in "
                  + customControl.getClass().getSimpleName() + ".getName() '" + customControl.getName() + "'";
            log.error(msg);
            throw new RuntimeException(msg);
         }
         registerControl(customControl);
         fillProperties(customControl, cdef.getProperties());
      } catch (ClassNotFoundException e) {
         throw new RuntimeException(e);
      } catch (InstantiationException e) {
         throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * registers an actuator from the configuration file. Overwrites build-in actuators with the same name.
    * 
    * @param controller
    */
   private void registerActuatorFromBinding(ActuatorBinding binding) {
      if (binding.getName() == null) {
         String msg = "Failed to read configuration from file " + CONFIGURATION_FILENAME
               + ": actuator has no name element";
         log.fatal(msg);
         throw new RuntimeException(msg);
      }
      Actuator ctrl = null;
      String classname = binding.getClazz();
      if (classname == null) {
         ctrl = getActuator(binding.getName());
      } else {
         try {
            Class<?> clazz = Class.forName(classname);
            ctrl = (Actuator) clazz.newInstance();
            ctrl.setName(binding.getName());
            log.info("register actuator " + ctrl.getClass().getName() + " under name " + ctrl.getName());
            actuators.put(ctrl.getName(), ctrl);
         } catch (Exception e) {
            log.fatal(e.getMessage(), e);
            throw new RuntimeException(e);
         }
      }

      fillProperties(ctrl, binding.getProperties());
   }

   /**
    * instantiates the built in actuators.
    */
   private synchronized void initBuiltInActuators() {
      log.debug("init Actuators");

      for (Actuator act : actuators.values()) {
         act.close();
      }
      actuators.clear();

      PropertyConverter pc = new PropertyConverter();
      ConvertUtils.register(pc, String[].class);
      ConvertUtils.register(pc, Collection.class);
      ConvertUtils.register(pc, Boolean.class);
      ConvertUtils.register(pc, boolean.class);
      ConvertUtils.register(pc, Date.class);
      ConvertUtils.register(pc, Class.class);
      ConvertUtils.register(pc, MonitorStatus.class);
      ConvertUtils.register(pc, LoadControlCallback.class);
      ConvertUtils.register(pc, Monitor[].class);
      ConvertUtils.register(pc, SchedulerTaskInterceptor.class);
      ConvertUtils.register(pc, OwnerCheckCallback.class);

      ServiceLoader<Actuator> loader = ServiceLoader.load(Actuator.class);
      Iterator<Actuator> iter = loader.iterator();
      while (iter.hasNext()) {
         Actuator act = iter.next();
         log.info("register " + act.getClass() + " under name " + act.getName());
         actuators.put(act.getName(), act);
      }
   }

   /**
    * instantiates the built in actuators.
    */
   private synchronized void initAuthenticationProvider() {
      log.info("init AuthenticationProviders");

      AuthenticationProvider hprovider = new HttpHeaderAuthenticationProvider();
      chainedAuthenticationProvider.getProviderChain().add(hprovider);
      log.info("register " + HttpHeaderAuthenticationProvider.class.getSimpleName());

      ServiceLoader<AuthenticationProvider> loader = ServiceLoader.load(AuthenticationProvider.class);
      Iterator<AuthenticationProvider> iter = loader.iterator();
      while (iter.hasNext()) {
         AuthenticationProvider provider = iter.next();
         log.info("register " + provider.getClass());
         chainedAuthenticationProvider.getProviderChain().add(provider);
      }

      AuthenticationProvider provider = new EEAuthenticationProvider();
      chainedAuthenticationProvider.getProviderChain().add(provider);
      log.info("register " + EEAuthenticationProvider.class.getSimpleName());

      AuthenticationProvider httpProvider = new HttpRequestAuthenticationProvider();
      chainedAuthenticationProvider.getProviderChain().add(httpProvider);
      log.info("register " + HttpRequestAuthenticationProvider.class.getSimpleName());

      HttpSessionAuthenticationProvider sessionProvider = new HttpSessionAuthenticationProvider();
      chainedAuthenticationProvider.getProviderChain().add(sessionProvider);
      log.info("register " + HttpSessionAuthenticationProvider.class.getSimpleName());
   }

   /**
    * instantiates the built in Control classes.
    */
   private synchronized void initBuiltInControls() {
      log.debug("init Controls");
      controls.clear();
      controls.put(TenantControl.NAME, new TenantControl());
      controls.put(EventControl.NAME, new EventControl());
      controls.put(TargetControl.NAME, new TargetControl());
      controls.put(StateChangeControl.NAME, new StateChangeControl());
      controls.put(MethodControl.NAME, new MethodControl());
      controls.put(InvokerControl.NAME, new InvokerControl());
      controls.put(ConditionControl.NAME, new ConditionControl());
   }

   private void fillProperties(Object object, PropertiesBinding propBindings) {
      if (propBindings != null) {
         for (Element element : propBindings.getAny()) {
            Node node = element.getFirstChild();
            String value = null;
            if (node != null) {
               value = node.getNodeValue();
            }

            Map<String, String> attributeMap = new HashMap<String, String>();
            NamedNodeMap attributes = element.getAttributes();
            if (attributes != null) {
               for (int i = 0; i < attributes.getLength(); i++) {
                  Node n = attributes.item(i);
                  attributeMap.put(n.getNodeName(), n.getNodeValue());
               }
            }

            try {
               if (attributeMap.get("mapKey") != null) {
                  PropertyUtils.setMappedProperty(object, element.getTagName(), attributeMap.get("mapKey"), value);
                  if (attributeMap.get("current") != null && "true".equalsIgnoreCase(attributeMap.get("current"))) {
                     BeanUtils.getProperty(object, "currentSecretKey");
                     BeanUtils.setProperty(object, "currentSecretKey", attributeMap.get("mapKey"));
                  }
               } else if (element.getTagName().indexOf(".") > 0) {
                  int point = element.getTagName().indexOf(".");
                  String delegateName = element.getTagName().substring(0, point);
                  String paramName = element.getTagName().substring(point + 1);
                  String getDelegate = "get" + delegateName.substring(0, 1).toUpperCase() + delegateName.substring(1);
                  Method method = object.getClass().getMethod(getDelegate);
                  Object delegate = method.invoke(object);
                  BeanUtils.getProperty(delegate, paramName);
                  BeanUtils.setProperty(delegate, paramName, value);

               } else {
                  BeanUtils.getProperty(object, element.getTagName());
                  BeanUtils.setProperty(object, element.getTagName(), value);
               }
            } catch (IllegalAccessException e) {
               throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
               throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
               String msg = "Failed to set property " + element.getTagName() + " in " + object.getClass().getName()
                     + ": " + e.getMessage();
               log.error(msg, e);
               throw new RuntimeException(msg);
            }
         }
      }
   }

   public void stopProxies() {
      for (String name : proxyServers.keySet()) {
         log.info("stop proxy " + name);
         proxyServers.get(name).stop();
      }
      proxyServers.clear();
      proxyConfigs.clear();
   }

   /**
    * starts the proxy for the HTTP-CLIENT sensor with the given config.
    * 
    */
   public synchronized void startProxy(ProxyConfig pConfig) {
      if (pConfig == null) {
         String err = "ProxyConfig is null. Cannot start proxy";
         log.error(err);
         throw new IllegalArgumentException(err);
      }

      if (pConfig.getName() == null) {
         String err = "Proxy name is mandatory. Cannot start proxy";
         log.error(err);
         throw new IllegalArgumentException(err);
      }

      if (proxyServers.containsKey(pConfig.getName())) {
         log.info("stop proxy " + pConfig.getName());
         proxyServers.get(pConfig.getName()).stop();
      }

      if (pConfig.getMode() == ProxyMode.NO_PROXY) {
         log.info("NO_PROXY configuration. No proxy for HTTP-CLIENT is started");
         proxyServers.remove(pConfig.getName());
         proxyConfigs.remove(pConfig.getName());
         return;
      }
      log.info("start Proxyserver with " + pConfig);

      CibetHttpFiltersSource filterSource = new CibetHttpFiltersSource(pConfig);
      HttpProxyServerBootstrap bootstrap = DefaultHttpProxyServer.bootstrap().withPort(pConfig.getPort())
            .withFiltersSource(filterSource).withConnectTimeout(pConfig.getTimeout())
            .withIdleConnectionTimeout(pConfig.getTimeout() / 1000);

      if (pConfig.getMode() == ProxyMode.CHAINEDPROXY) {
         if (pConfig.getChainedProxyHost() != null) {
            ChainedProxyManager chainedProxy = new ChainedProxyManagerImpl(pConfig.getChainedProxyHost(),
                  pConfig.getChainedProxyPort());
            bootstrap.withChainProxyManager(chainedProxy);
         }

      } else {
         CibetMitmManager mitmManager = null;
         if (pConfig.getClientKeystore() != null) {
            mitmManager = new CibetMitmManager(pConfig.getClientKeystore(), pConfig.getClientKeystorePassword());
         } else {
            mitmManager = new CibetMitmManager();
         }
         bootstrap.withManInTheMiddle(mitmManager);
      }
      HttpProxyServer proxyServer = bootstrap.start();
      proxyServers.put(pConfig.getName(), proxyServer);

      proxyConfigs.put(pConfig.getName(), pConfig);
      log.debug("proxy " + pConfig.getName() + " started");
   }

   /**
    * starts the proxy for the HTTP(CLIENT) sensor. Optional System properties cibet.proxy.mode, cibet.proxy.port,
    * cibet.proxy.chainedProxyHost, cibet.proxy.chainedProxyPort, cibet.proxy.bufferSize, cibet.proxy.clientKeystore and
    * cibet.proxy.clientKeystorePassword override the values in the ProxyConfig parameter.
    * 
    * @param pconfigs
    *           map of configs
    */
   public synchronized void startProxyOverrideWithSystemConfig(Map<String, ProxyConfig> pconfigs) {
      log.debug("startProxyOverrideWithSystemConfig");
      Map<String, String> systemParams = new HashMap<>();
      for (Entry<Object, Object> entry : System.getProperties().entrySet()) {
         String key = (String) entry.getKey();
         if (key.startsWith(PROXY_PREFIX)) {
            systemParams.put(key, (String) entry.getValue());
         }
      }

      resolveProxyConfigParams(pconfigs, systemParams);
      for (ProxyConfig pconfig : pconfigs.values()) {
         startProxy(pconfig);
      }
   }

   public void resolveProxyConfigParams(Map<String, ProxyConfig> localProxyConfigs, Map<String, String> params) {
      for (Entry<String, String> entry : params.entrySet()) {
         if (entry.getKey().startsWith(PROXY_BUFFERSIZE)) {
            checkProxyParameter(entry.getKey(), PROXY_BUFFERSIZE.length());
            String name = entry.getKey().substring(PROXY_BUFFERSIZE.length() + 1);
            try {
               proxyConfig(localProxyConfigs, name).setBufferSize(Integer.parseInt(entry.getValue()));
            } catch (NumberFormatException e) {
               throw new NumberFormatException("Parameter " + PROXY_BUFFERSIZE + " must be numeric: " + e.getMessage());
            }

         } else if (entry.getKey().startsWith(PROXY_CHAINEDPROXYHOST)) {
            checkProxyParameter(entry.getKey(), PROXY_CHAINEDPROXYHOST.length());
            String name = entry.getKey().substring(PROXY_CHAINEDPROXYHOST.length() + 1);
            proxyConfig(localProxyConfigs, name).setChainedProxyHost(entry.getValue());

         } else if (entry.getKey().startsWith(PROXY_CHAINEDPROXYPORT)) {
            checkProxyParameter(entry.getKey(), PROXY_CHAINEDPROXYPORT.length());
            String name = entry.getKey().substring(PROXY_CHAINEDPROXYPORT.length() + 1);
            try {
               proxyConfig(localProxyConfigs, name).setChainedProxyPort(Integer.parseInt(entry.getValue()));
            } catch (NumberFormatException e) {
               throw new NumberFormatException(
                     "Parameter " + PROXY_CHAINEDPROXYPORT + " must be numeric: " + e.getMessage());
            }

         } else if (entry.getKey().startsWith(PROXY_CLIENT_KEYSTOREPASSWORD)) {
            checkProxyParameter(entry.getKey(), PROXY_CLIENT_KEYSTOREPASSWORD.length());
            String name = entry.getKey().substring(PROXY_CLIENT_KEYSTOREPASSWORD.length() + 1);
            proxyConfig(localProxyConfigs, name).setClientKeystorePassword(entry.getValue());

         } else if (entry.getKey().startsWith(PROXY_CLIENT_KEYSTORE)) {
            checkProxyParameter(entry.getKey(), PROXY_CLIENT_KEYSTORE.length());
            String name = entry.getKey().substring(PROXY_CLIENT_KEYSTORE.length() + 1);
            proxyConfig(localProxyConfigs, name).setClientKeystore(entry.getValue());

         } else if (entry.getKey().startsWith(PROXY_EXCLUDES)) {
            checkProxyParameter(entry.getKey(), PROXY_EXCLUDES.length());
            String name = entry.getKey().substring(PROXY_EXCLUDES.length() + 1);
            proxyConfig(localProxyConfigs, name).setExcludePattern(resolveExcludePatterns(entry.getValue()));

         } else if (entry.getKey().startsWith(PROXY_MODE)) {
            checkProxyParameter(entry.getKey(), PROXY_MODE.length());
            String name = entry.getKey().substring(PROXY_MODE.length() + 1);
            proxyConfig(localProxyConfigs, name).setMode(ProxyMode.valueOf(entry.getValue()));

         } else if (entry.getKey().startsWith(PROXY_PORT)) {
            checkProxyParameter(entry.getKey(), PROXY_PORT.length());
            String name = entry.getKey().substring(PROXY_PORT.length() + 1);
            try {
               proxyConfig(localProxyConfigs, name).setPort(Integer.parseInt(entry.getValue()));
            } catch (NumberFormatException e) {
               throw new NumberFormatException("Parameter " + PROXY_PORT + " must be numeric: " + e.getMessage());
            }

         } else if (entry.getKey().startsWith(PROXY_TIMEOUT)) {
            checkProxyParameter(entry.getKey(), PROXY_TIMEOUT.length());
            String name = entry.getKey().substring(PROXY_TIMEOUT.length() + 1);
            try {
               proxyConfig(localProxyConfigs, name).setTimeout(Integer.parseInt(entry.getValue()));
            } catch (NumberFormatException e) {
               throw new NumberFormatException("Parameter " + PROXY_TIMEOUT + " must be numeric: " + e.getMessage());
            }
         }
      }
   }

   private void checkProxyParameter(String proxyParam, int length) {
      if (proxyParam.length() <= length + 1) {
         throw new IllegalArgumentException(
               "Wrong parameter " + proxyParam + ": Must be " + proxyParam + ".<name of proxy>");
      }
   }

   private List<Pattern> resolveExcludePatterns(String excludes) {
      List<Pattern> patterns = new ArrayList<>();
      StringTokenizer tok = new StringTokenizer(excludes, ",");
      while (tok.hasMoreTokens()) {
         String exclude = tok.nextToken().trim();
         patterns.add(Pattern.compile(exclude));
      }
      return patterns;
   }

   private ProxyConfig proxyConfig(Map<String, ProxyConfig> localProxyConfigs, String name) {
      ProxyConfig pc = localProxyConfigs.get(name);
      if (pc == null) {
         pc = new ProxyConfig();
         pc.setName(name);
         localProxyConfigs.put(name, pc);
      }
      return pc;
   }

   /**
    * returns a list of the configured proxies.
    * 
    * @return
    */
   public String getProxies() {
      StringBuffer b = new StringBuffer();
      for (Entry<String, ProxyConfig> entry : proxyConfigs.entrySet()) {
         b.append(entry.getValue());
         b.append("running: ");
         b.append(proxyServers.containsKey(entry.getKey()) ? "true" : "false");
         b.append("\n");
      }
      return b.toString();
   }

}
