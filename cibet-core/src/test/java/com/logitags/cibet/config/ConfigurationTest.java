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
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.cibethelper.base.CoreTestBase;
import com.cibethelper.base.SubSub4EyesController;
import com.cibethelper.base.TrueCustomControl;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.common.Actuator;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.control.BooleanAttributedControlValue;
import com.logitags.cibet.control.ConditionControl;
import com.logitags.cibet.control.EventControl;
import com.logitags.cibet.control.InvokerControl;
import com.logitags.cibet.control.MethodControl;
import com.logitags.cibet.control.StateChangeControl;
import com.logitags.cibet.control.TargetControl;
import com.logitags.cibet.control.TenantControl;
import com.logitags.cibet.notification.EmailNotificationProvider;
import com.logitags.cibet.security.DefaultSecurityProvider;

public class ConfigurationTest extends CoreTestBase {

   private static Logger log = Logger.getLogger(ConfigurationTest.class);

   private String xsd;

   @After
   public void after() {
      log.debug("do after resetConfigFilename1");
      initConfiguration("cibet-config.xml");
   }

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

   private void initXSD(String configName) throws Exception {
      Field FILENAME = Configuration.class.getDeclaredField("CONFIGURATION_XSD_FILENAME");
      FILENAME.setAccessible(true);
      xsd = (String) FILENAME.get(null);
      FILENAME.set(null, configName);
   }

   @Test
   public void startLoadActuators1() throws Exception {
      log.info("start startLoadActuators1()");
      SubSub4EyesController controller = (SubSub4EyesController) Configuration.instance().getActuator("TESTSUBSUB");

      Assert.assertEquals(1234456789123L, controller.getDummy1());
      Assert.assertEquals(false, controller.isDummy2());
      Assert.assertEquals(new Integer(234), controller.getIntParamB());
      Assert.assertEquals(45.67, controller.getDoubleParam(), 0);
      Assert.assertEquals(new Double(34.22), controller.getDoubleParamB());
      Assert.assertTrue("Is: " + controller.getFloatParam(), 12.55f == controller.getFloatParam());
      Assert.assertEquals(new Float(102.456), controller.getFloatParamB());
      Assert.assertEquals("Hallo", controller.getStrParam());
      Assert.assertEquals(new Boolean(true), controller.getBoolParam());
      Assert.assertEquals(new Short((short) 12), controller.getShortParamB());
      Assert.assertEquals(12, controller.getShortParam());
      Assert.assertEquals(new Long(123456), controller.getLongParam());
   }

   @Test
   public void startConfigurationWithError1() throws Exception {
      log.info("start startConfigurationWithError1()");
      String result = initConfiguration("config_error_actuator.xml");
      log.debug("initresult= " + result);
      Assert.assertTrue(!"Configuration started successfully".equals(result));
      Assert.assertTrue(result.indexOf("Failed to set property not_present_attribute") > 0);

      SubSub4EyesController act = (SubSub4EyesController) Configuration.instance().getActuator("TESTSUBSUB");
      Assert.assertNotNull(act);
      Assert.assertEquals(1234456789123l, act.getDummy1());
      Assert.assertEquals(false, act.isDummy2());
      Assert.assertEquals(new Integer(234), act.getIntParamB());
      Assert.assertEquals(45.67, act.getDoubleParam(), 0);
      Assert.assertEquals(new Double(34.22), act.getDoubleParamB());
      Assert.assertEquals((float) 12.55, act.getFloatParam(), 0);
      Assert.assertEquals(new Float(102.456), act.getFloatParamB());
      Assert.assertEquals("Hallo", act.getStrParam());
      Assert.assertEquals(Boolean.TRUE, act.getBoolParam());
      Assert.assertEquals(new Short((short) 12), act.getShortParamB());
      Assert.assertEquals(new Long(123456l), act.getLongParam());
   }

   @Test
   public void startLoadActuators2() throws Exception {
      log.info("start startLoadActuators2()");
      ArchiveActuator arch = null;
      initConfiguration("config_actuator3.xml");

      arch = (ArchiveActuator) Configuration.instance().getActuator(ArchiveActuator.DEFAULTNAME);

      log.debug("StoredProperties().size: " + arch.getStoredProperties().size());

      Assert.assertEquals(true, arch.isIntegrityCheck());
      arch.setIntegrityCheck(false);
      Assert.assertEquals(false, arch.isIntegrityCheck());

      DefaultSecurityProvider sp = (DefaultSecurityProvider) Configuration.instance().getSecurityProvider();
      Assert.assertNotNull(sp);
      String currentRef = sp.getCurrentSecretKey();
      Assert.assertEquals("key1", currentRef);

      Assert.assertEquals(3, sp.getSecrets().size());
      Assert.assertEquals("2366Au37nBB.0ya?", sp.getSecrets().get("key1"));
      Actuator c2 = Configuration.instance().getActuator("Sub4EyesController");
      Assert.assertNotNull(c2);
   }

   @Test
   public void startWithoutConfigurationFile() throws Exception {
      String res = initConfiguration("no_file.xml");
      Assert.assertEquals("Configuration started successfully.", res);
   }

   @Test(expected = IllegalArgumentException.class)
   public void registerNullController() {
      Configuration.instance().registerActuator(null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void registerNullSetpopint() {
      Configuration.instance().registerSetpoint(null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void registerNullControl() {
      Configuration.instance().registerControl(null);
   }

   @Test
   public void start() throws Exception {
      log.info("start()");
      Context.sessionScope().setTenant(null);
      initConfiguration("config_parents.xml");
      Assert.assertEquals(5, Configuration.instance().getSetpoints().size());
      Setpoint sp = Configuration.instance().getSetpoints().get(0);
      Assert.assertEquals(1, sp.getActuators().size());
      Assert.assertEquals(1, ((List<String>) sp.getControlValue(TargetControl.NAME)).size());
      Assert.assertEquals(1, ((List<String>) sp.getControlValue(EventControl.NAME)).size());
      Assert.assertEquals(1,
            ((BooleanAttributedControlValue) sp.getControlValue(InvokerControl.NAME)).getValues().size());
      Assert.assertEquals(1, ((List<String>) sp.getControlValue(MethodControl.NAME)).size());
      Assert.assertEquals(1, ((List<String>) sp.getControlValue(TenantControl.NAME)).size());
      Assert.assertEquals(1,
            ((BooleanAttributedControlValue) sp.getControlValue(StateChangeControl.NAME)).getValues().size());
      Assert.assertEquals("", (String) sp.getControlValue(ConditionControl.NAME));
      Assert.assertEquals("", ((List<String>) sp.getControlValue(EventControl.NAME)).get(0));
      Assert.assertEquals("", ((List<String>) sp.getControlValue(TargetControl.NAME)).get(0));
      Assert.assertEquals("",
            ((BooleanAttributedControlValue) sp.getControlValue(InvokerControl.NAME)).getValues().get(0));
      Assert.assertEquals("", ((List<String>) sp.getControlValue(MethodControl.NAME)).get(0));
      Assert.assertEquals("", ((List<String>) sp.getControlValue(TenantControl.NAME)).get(0));
      Assert.assertEquals("",
            ((BooleanAttributedControlValue) sp.getControlValue(StateChangeControl.NAME)).getValues().get(0));
   }

   @Test
   public void initialise() throws Exception {
      log.info("start initialise()");
      try {
         initXSD("not-existing.xsd");
         Configuration.instance().initialise();
         Assert.fail();
      } catch (RuntimeException e) {
         log.debug(e.getMessage());
         Assert.assertTrue(e.getMessage().indexOf("not-existing.xsd not found in classpath") > -1);
      } finally {
         initXSD(xsd);
      }
   }

   @Test(expected = IllegalArgumentException.class)
   public void registerControl() {
      log.info("start registerControl()");
      TrueCustomControl control = new TrueCustomControl();
      control.setName(null);
      Configuration.instance().registerControl(control);
   }

   @Test(expected = IllegalArgumentException.class)
   public void registerControl2() {
      log.info("start registerControl2()");
      TrueCustomControl control = new TrueCustomControl();
      Configuration.instance().registerControl(control);
      Assert.assertNotNull(Configuration.instance().getControl("TRUE"));
      TrueCustomControl control2 = new TrueCustomControl();
      Configuration.instance().registerControl(control2);
   }

   @Test
   public void mbeanInitNotificationProvider() throws Exception {
      log.info("start mbeanInitNotificationProvider()");
      initConfiguration("config_actuator3.xml");
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      ObjectName oname = findObjectName();
      String result = (String) mbs.invoke(oname, "re-init NotificationProvider", null, null);
      Assert.assertEquals("NotificationProvider successfully reinitialized.", result);
      Assert.assertTrue(Configuration.instance().getNotificationProvider() instanceof EmailNotificationProvider);
   }

   @Test
   public void mbeanInitSecurityProvider() throws Exception {
      log.info("start mbeanInitSecurityProvider()");
      initConfiguration("config_actuator3.xml");
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      ObjectName oname = findObjectName();
      String result = (String) mbs.invoke(oname, "re-init SecurityProvider", null, null);
      Assert.assertEquals("SecurityProvider successfully reinitialized.", result);
      Assert.assertTrue(Configuration.instance().getSecurityProvider() instanceof DefaultSecurityProvider);
      Assert.assertEquals("key1", Configuration.instance().getSecurityProvider().getCurrentSecretKey());
   }

   @Test
   public void mbeanInitControls() throws Exception {
      log.info("start mbeanInitControls()");
      initConfiguration("config_actuator3.xml");
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      ObjectName oname = findObjectName();
      String result = (String) mbs.invoke(oname, "re-init Controls", null, null);
      Assert.assertEquals("Controls successfully reinitialized. Setpoints are also reinitialized", result);
      Assert.assertTrue(Configuration.instance().getControl("TRUE") instanceof TrueCustomControl);
      Assert.assertEquals("45", ((TrueCustomControl) Configuration.instance().getControl("TRUE")).getGaga());
   }

}
