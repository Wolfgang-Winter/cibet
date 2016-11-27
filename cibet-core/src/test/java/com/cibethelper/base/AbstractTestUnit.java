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
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.junit.AfterClass;
import org.junit.Before;

import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TComplexEntity2;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.ConfigurationService;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;

public abstract class AbstractTestUnit {

   private static Logger log = Logger.getLogger(AbstractTestUnit.class);

   protected static String TENANT = "testTenant";

   protected static final String USER = "USER";

   protected static final String JDBC_SUFFIX = "-CibetDriver";

   protected static Configuration cman;

   protected boolean skip = false;

   protected static final String SEL_DCCONTROLLABLE = "SELECT c FROM DcControllable c WHERE c.executionStatus = com.logitags.cibet.core.ExecutionStatus.POSTPONED";

   @Before
   public void initConfiguration() throws Exception {
      log.debug("start initConfiguration()");
      // Configuration.instance().stopProxies();
      // resetConfigFilename();
      Context.internalSessionScope().clear();
      Context.internalRequestScope().clear();

      Field instance = Configuration.class.getDeclaredField("instance");
      instance.setAccessible(true);
      instance.set(null, null);
      cman = Configuration.instance();

      // if (cman == null) {
      // cman = Configuration.instance();
      // } else {
      // cman.initialise();
      // }
      Context.internalRequestScope().setManaged(true);
      log.info("finished before test");
   }

   @AfterClass
   public static void resetConfigFilename() throws Exception {
      log.debug("do after: resetConfigFilename");
      Field FILENAME = Configuration.class.getDeclaredField("CONFIGURATION_FILENAME");
      FILENAME.setAccessible(true);
      FILENAME.set(null, "cibet-config.xml");
      Context.internalSessionScope().clear();
      Context.internalRequestScope().clear();
   }

   protected String init(String configName) throws Exception {
      Field FILENAME = Configuration.class.getDeclaredField("CONFIGURATION_FILENAME");
      FILENAME.setAccessible(true);
      FILENAME.set(null, configName);

      ConfigurationService confMan = new ConfigurationService();

      return confMan.initialise();
   }

   protected void authenticateShiro(String user, String password) {
      AuthenticationToken token = new UsernamePasswordToken(user, password);
      Subject subject = SecurityUtils.getSubject();
      subject.login(token);
   }

   public void registerSetpoint(Class<?> clazz, String act, String methodName, ControlEvent... events) {
      Setpoint sp = registerSetpoint(clazz, act, events);
      sp.setMethod(methodName);
   }

   public Setpoint registerSetpoint(Class<?> clazz, String act, ControlEvent... events) {
      return registerSetpoint(clazz.getName(), act, events);
   }

   public Setpoint registerSetpoint(String clazz, String act, ControlEvent... events) {
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

   public Setpoint registerSetpoint(String clazz, List<String> acts, ControlEvent... events) {
      Setpoint sp = new Setpoint(String.valueOf(new Date().getTime()), null);
      sp.setTarget(clazz);
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

   protected TEntity createTEntity(int counter, String name) {
      TEntity te = new TEntity();
      te.setCounter(counter);
      te.setNameValue(name);
      te.setOwner(name);
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
