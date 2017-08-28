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
package com.logitags.cibet.config;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.cibethelper.base.CoreTestBase;
import com.logitags.cibet.actuator.springsecurity.SpringSecurityActuator;
import com.logitags.cibet.authentication.AuthenticationProvider;
import com.logitags.cibet.authentication.ChainedAuthenticationProvider;
import com.logitags.cibet.authentication.SpringSecurityAuthenticationProvider;

public class ConfigurationTest extends CoreTestBase {

   private static Logger log = Logger.getLogger(ConfigurationTest.class);

   private ObjectName findObjectName() throws Exception {
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      Set<ObjectName> names = mbs.queryNames(null, null);
      log.info(names.size() + " MBeans found");
      for (ObjectName name : names) {
         log.debug("name.getCanonicalName(): " + name.getCanonicalName());
         if (name.getCanonicalName().startsWith("com.logitags.cibet:app=,type=Configuration")) {
            log.info("found Config MBean");
            return name;
         }
      }
      throw new Exception("Config MBean not found");
   }

   @Test
   public void loadScenario1() throws Exception {
      log.info("start loadScenario1()");
      initConfiguration("scenario1.xml");
      Assert.assertEquals(8, Configuration.instance().getSetpoints().size());
   }

   @Test
   public void loadScenario2() throws Exception {
      log.info("start loadScenario2()");
      initConfiguration("scenario2.xml");
      Assert.assertEquals(9, Configuration.instance().getSetpoints().size());
   }

   @Test
   public void mbeanInitAuthenticationProvider() throws Exception {
      log.info("start mbeanInitAuthenticationProvider()");
      initConfiguration("config_authenticationprovider.xml");
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      ObjectName oname = findObjectName();
      String result = (String) mbs.invoke(oname, "re-init AuthenticationProvider", null, null);
      Assert.assertEquals("AuthenticationProvider successfully reinitialized.", result);
      ChainedAuthenticationProvider authp = Configuration.instance().getAuthenticationProvider();
      List<AuthenticationProvider> list = authp.getProviderChain();
      boolean contains = false;
      for (AuthenticationProvider p : list) {
         if (p instanceof SpringSecurityAuthenticationProvider) {
            contains = true;
            break;
         }
      }
      Assert.assertTrue(contains);
   }

   @Test
   public void mbeanInitActuators() throws Exception {
      log.info("start mbeanInitActuators()");
      initConfiguration("scenario1.xml");
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      ObjectName oname = findObjectName();
      String result = (String) mbs.invoke(oname, "re-init Actuators", null, null);
      Assert.assertEquals("Actuators successfully reinitialized. Setpoints have also been reinitialized", result);
      Assert.assertTrue(
            Configuration.instance().getActuator("SPRINGSECURITY_RELEASER") instanceof SpringSecurityActuator);
      Assert.assertNotNull(Configuration.instance().getSetpoint("scenario1/D1"));
   }

   @Test
   public void mbeanInitSetpoints() throws Exception {
      log.info("start mbeanInitSetpoints()");
      initConfiguration("scenario1.xml");
      mbeanInitActuators();
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      ObjectName oname = findObjectName();
      String result = (String) mbs.invoke(oname, "re-init Setpoints", null, null);
      Assert.assertEquals("Setpoints successfully reinitialized.", result);
      Assert.assertEquals(8, Configuration.instance().getSetpoints().size());
   }

}
