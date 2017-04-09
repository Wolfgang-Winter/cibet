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
package com.cibethelper.base;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.AfterClass;

import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TComplexEntity2;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.common.Actuator;
import com.logitags.cibet.actuator.dc.FourEyesActuator;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.ConfigurationService;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.core.ControlEvent;

public abstract class CoreTestBase {

   private static Logger log = Logger.getLogger(CoreTestBase.class);

   protected static final String TENANT = "testTenant";

   protected static final String USER = "USER";

   protected static final String JBOSS = "JBoss";
   protected static final String TOMEE = "Tomee";
   protected static final String GLASSFISH = "Glassfish";

   protected static String APPSERVER;
   protected static String HTTPURL;
   protected static String HTTPSURL;

   @AfterClass
   public static void afterClass() throws Exception {
      initConfiguration("cibet-config.xml");
   }

   protected static String initConfiguration(String configName) {
      log.debug("++initConfiguration " + configName);
      try {
         Field FILENAME = Configuration.class.getDeclaredField("CONFIGURATION_FILENAME");
         FILENAME.setAccessible(true);
         FILENAME.set(null, configName);
      } catch (Exception e) {
         throw new RuntimeException(e);
      }

      ConfigurationService confMan = new ConfigurationService();
      String res = confMan.initialise();
      log.debug("end ++initConfiguration: " + res);
      return res;
   }

   protected Setpoint registerSetpoint(Class<?> clazz, String act, String methodName, ControlEvent... events) {
      Setpoint sp = registerSetpoint(clazz, act, events);
      sp.setMethod(methodName);
      return sp;
   }

   protected Setpoint registerSetpoint(Class<?> clazz, String act, ControlEvent... events) {
      return registerSetpoint(clazz.getName(), act, events);
   }

   protected Setpoint registerSetpoint(String clazz, String act, ControlEvent... events) {
      Setpoint sp = new Setpoint(String.valueOf(new Date().getTime()), null);
      sp.setTarget(clazz);
      List<String> evl = new ArrayList<String>();
      for (ControlEvent ce : events) {
         evl.add(ce.name());
      }
      sp.setEvent(evl.toArray(new String[0]));
      Configuration cman = Configuration.instance();
      sp.addActuator(cman.getActuator(act));
      cman.registerSetpoint(sp);
      return sp;
   }

   protected Setpoint registerSetpoint(String clazz, List<String> acts, ControlEvent... events) {
      Setpoint sp = new Setpoint(String.valueOf(new Date().getTime()), null);
      if (clazz != null) {
         sp.setTarget(clazz);
      }
      List<String> evl = new ArrayList<String>();
      for (ControlEvent ce : events) {
         evl.add(ce.name());
      }
      sp.setEvent(evl.toArray(new String[0]));
      Configuration cman = Configuration.instance();
      for (String scheme : acts) {
         sp.addActuator(cman.getActuator(scheme));
      }
      cman.registerSetpoint(sp);
      return sp;
   }

   protected Setpoint registerSetpoint(Class<?> clazz, List<String> acts, String methodName, ControlEvent... events) {
      Setpoint sp = registerSetpoint(clazz.getName(), acts, events);
      sp.setMethod(methodName);
      return sp;
   }

   protected Setpoint registerSetpoint(String target, String method, List<String> acts, ControlEvent... events) {
      Setpoint sp = registerSetpoint(target, acts, events);
      sp.setMethod(method);
      return sp;
   }

   public void registerSetpoint(Class<?> clazz, List<String> schemes, String methodName, String jndiName,
         ControlEvent... events) {
      registerSetpoint(clazz, schemes, methodName, events);

      Configuration cman = Configuration.instance();
      for (String scheme : schemes) {
         Actuator act = cman.getActuator(scheme);
         if (act instanceof FourEyesActuator) {
            ((FourEyesActuator) act).setJndiName(jndiName);
         }
         if (act instanceof ArchiveActuator) {
            ((ArchiveActuator) act).setJndiName(jndiName);
         }
      }
   }

   protected TEntity createTEntity(int counter, String name) {
      TEntity te = new TEntity();
      te.setCounter(counter);
      te.setNameValue(name);
      te.setOwner(TENANT);
      return te;
   }

   protected TComplexEntity createTComplexEntity() {
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

   protected TComplexEntity2 createTComplexEntity2() {
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

}
