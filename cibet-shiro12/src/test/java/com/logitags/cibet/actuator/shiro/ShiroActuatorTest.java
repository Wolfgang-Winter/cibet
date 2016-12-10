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
package com.logitags.cibet.actuator.shiro;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.Factory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cibethelper.ShiroTestBase;
import com.cibethelper.base.CoreTestBase;
import com.cibethelper.base.CoreTestService;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.common.DeniedException;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InitializationService;
import com.logitags.cibet.context.InternalSessionScope;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.sensor.http.HttpRequestResourceHandler;
import com.logitags.cibet.sensor.jpa.JpaResourceHandler;

import junit.framework.Assert;

/**
 * add -javaagent:${project_loc}\..\cibet-material\technics\aspectjweaver-1.6.9. jar to java command
 */
public class ShiroActuatorTest extends CoreTestBase {

   private static Logger log = Logger.getLogger(ShiroActuatorTest.class);

   @BeforeClass
   public static void initShiro() {
      log.debug("start ShiroActuatorTest @BeforeClass");
      Factory<SecurityManager> factory = new IniSecurityManagerFactory("classpath:shiro.init");
      SecurityManager securityManager = factory.getInstance();
      SecurityUtils.setSecurityManager(securityManager);
      log.debug("end ShiroActuatorTest @BeforeClass");
   }

   @Before
   public void before() {
      log.info("BEFORE TEST");
      InitializationService.instance().startContext();
      initConfiguration("x");
   }

   @Test
   public void invoke() throws Exception {
      CoreTestService.registerSetpoint(TComplexEntity.class, ShiroActuator.DEFAULTNAME, "setCompValue",
            ControlEvent.INVOKE);

      ShiroActuator act = (ShiroActuator) Configuration.instance().getActuator(ShiroActuator.DEFAULTNAME);
      Collection<String> col = new ArrayList<String>();
      col.add("goodguy");
      col.add("schwartz");
      act.setHasAllRoles(col);

      ShiroTestBase.authenticateShiro("lonestarr", "vespa");

      TComplexEntity ent1 = new TComplexEntity();
      ent1.setCompValue(22);
      Assert.assertEquals(22, ent1.getCompValue());
   }

   @Test
   public void invokeDenied() {
      log.debug("start invokeDenied()");
      CoreTestService.registerSetpoint(TComplexEntity.class, ShiroActuator.DEFAULTNAME, "setCompValue",
            ControlEvent.INVOKE);

      ShiroActuator act = (ShiroActuator) Configuration.instance().getActuator(ShiroActuator.DEFAULTNAME);
      Collection<String> col = new ArrayList<String>();
      col.add("nott");
      act.setHasAllRoles(col);

      ShiroTestBase.authenticateShiro("lonestarr", "vespa");

      TComplexEntity ent1 = new TComplexEntity();
      ent1.setCompValue(22);
      Assert.assertEquals(0, ent1.getCompValue());
   }

   @Test
   public void invokeDenied2() {
      CoreTestService.registerSetpoint(TComplexEntity.class, ShiroActuator.DEFAULTNAME, "setAndGetCompValue",
            ControlEvent.INVOKE);

      ShiroActuator act = (ShiroActuator) Configuration.instance().getActuator(ShiroActuator.DEFAULTNAME);
      Collection<String> col = new ArrayList<String>();
      col.add("nott");
      act.setHasAllRoles(col);
      act.setThrowDeniedException(true);

      Subject subject = SecurityUtils.getSubject();
      subject.logout();

      TComplexEntity ent1 = new TComplexEntity();
      try {
         ent1.setAndGetCompValue(22);
         Assert.fail();
      } catch (DeniedException e) {
      }
      Assert.assertEquals(0, ent1.getCompValue());
   }

   @Test
   public void beforeHttpNoRuleShiro() {
      log.debug("start beforeHttpNoRuleShiro");
      ShiroActuator act = new ShiroActuator();
      Resource res = new Resource(HttpRequestResourceHandler.class, "http://localhost/dom", "GET", null);
      EventMetadata ctx = new EventMetadata(ControlEvent.DELETE, res);
      act.beforeEvent(ctx);
      Assert.assertNull(ctx.getException());
      Assert.assertEquals(ExecutionStatus.EXECUTING, ctx.getExecutionStatus());
      // Assert.assertEquals(ExecutionStatus.EXECUTED, Context.requestScope()
      // .getExecutedEventResult().getExecutionStatus());
   }

   @Test
   public void beforeHttpNoFilterSecurityInterceptorShiro() {
      log.debug("start beforeHttpNoFilterSecurityInterceptorShiro");
      Collection<String> col = new ArrayList<String>();
      col.add("ROLE_USER");
      ShiroActuator act = new ShiroActuator();
      act.setHasAllRoles(col);
      Resource res = new Resource(HttpRequestResourceHandler.class, "http://localhost/dom", "GET", null);
      EventMetadata ctx = new EventMetadata(ControlEvent.DELETE, res);

      act.beforeEvent(ctx);
      Assert.assertEquals(ExecutionStatus.DENIED, ctx.getExecutionStatus());
   }

   @Test
   public void isPermittedAll() throws Exception {
      log.debug("start isPermittedAll");
      CoreTestService.registerSetpoint(TComplexEntity.class, ShiroActuator.DEFAULTNAME, "setCompValue",
            ControlEvent.INVOKE);

      ShiroActuator act = (ShiroActuator) Configuration.instance().getActuator(ShiroActuator.DEFAULTNAME);
      String[] roles = new String[] { "lightsaber:activate:ll" };
      act.setIsPermittedAll(roles);

      ShiroTestBase.authenticateShiro("lonestarr", "vespa");

      TComplexEntity ent1 = new TComplexEntity();
      ent1.setCompValue(22);
      Assert.assertEquals(22, ent1.getCompValue());
   }

   @Test
   public void isPermittedAllTwoPermissions() throws Exception {
      log.debug("start isPermittedAllTwoPermissions");
      CoreTestService.registerSetpoint(TComplexEntity.class, ShiroActuator.DEFAULTNAME, "setCompValue",
            ControlEvent.INVOKE);

      ShiroActuator act = (ShiroActuator) Configuration.instance().getActuator(ShiroActuator.DEFAULTNAME);
      String[] roles = new String[] { "lightsaber:activate:ll", "lightsaber:delete" };
      act.setIsPermittedAll(roles);

      ShiroTestBase.authenticateShiro("lonestarr", "vespa");

      TComplexEntity ent1 = new TComplexEntity();
      ent1.setCompValue(22);
      Assert.assertEquals(22, ent1.getCompValue());
   }

   @Test
   public void isPermittedAllDenied() throws Exception {
      log.debug("start isPermittedAllDenied");
      CoreTestService.registerSetpoint(TComplexEntity.class, ShiroActuator.DEFAULTNAME, "setCompValue",
            ControlEvent.INVOKE);

      ShiroActuator act = (ShiroActuator) Configuration.instance().getActuator(ShiroActuator.DEFAULTNAME);
      String[] roles = new String[] { "lightsaber:activate:ll", "saber:delete" };
      act.setIsPermittedAll(roles);

      ShiroTestBase.authenticateShiro("lonestarr", "vespa");

      TComplexEntity ent1 = new TComplexEntity();
      ent1.setCompValue(22);
      Assert.assertEquals(0, ent1.getCompValue());
   }

   @Test
   public void requiresGuestDenied() throws Exception {
      CoreTestService.registerSetpoint(TComplexEntity.class, ShiroActuator.DEFAULTNAME, "setCompValue",
            ControlEvent.INVOKE);

      ShiroActuator act = (ShiroActuator) Configuration.instance().getActuator(ShiroActuator.DEFAULTNAME);
      act.setRequiresGuest(true);

      ShiroTestBase.authenticateShiro("lonestarr", "vespa");

      TComplexEntity ent1 = new TComplexEntity();
      ent1.setCompValue(22);
      Assert.assertEquals(0, ent1.getCompValue());
   }

   @Test
   public void requiresGuest() throws Exception {
      CoreTestService.registerSetpoint(TComplexEntity.class, ShiroActuator.DEFAULTNAME, "setCompValue",
            ControlEvent.INVOKE);

      ShiroActuator act = (ShiroActuator) Configuration.instance().getActuator(ShiroActuator.DEFAULTNAME);
      act.setRequiresGuest(true);

      Subject subject = SecurityUtils.getSubject();
      subject.logout();

      TComplexEntity ent1 = new TComplexEntity();
      ent1.setCompValue(22);
      Assert.assertEquals(22, ent1.getCompValue());
   }

   @Test
   public void requiresAuthDenied() throws Exception {
      CoreTestService.registerSetpoint(TComplexEntity.class, ShiroActuator.DEFAULTNAME, "setCompValue",
            ControlEvent.INVOKE);

      ShiroActuator act = (ShiroActuator) Configuration.instance().getActuator(ShiroActuator.DEFAULTNAME);
      act.setRequiresAuthentication(true);

      Subject subject = SecurityUtils.getSubject();
      subject.logout();

      TComplexEntity ent1 = new TComplexEntity();
      ent1.setCompValue(22);
      Assert.assertEquals(0, ent1.getCompValue());
   }

   @Test
   public void requiresAuth() throws Exception {
      CoreTestService.registerSetpoint(TComplexEntity.class, ShiroActuator.DEFAULTNAME, "setCompValue",
            ControlEvent.INVOKE);

      ShiroActuator act = (ShiroActuator) Configuration.instance().getActuator(ShiroActuator.DEFAULTNAME);
      act.setRequiresAuthentication(true);

      ShiroTestBase.authenticateShiro("lonestarr", "vespa");

      TComplexEntity ent1 = new TComplexEntity();
      ent1.setCompValue(22);
      Assert.assertEquals(22, ent1.getCompValue());
   }

   @Test
   public void requiresUserDenied() throws Exception {
      log.debug("start requiresUserDenied()");

      CoreTestService.registerSetpoint(TComplexEntity.class, ShiroActuator.DEFAULTNAME, "setCompValue",
            ControlEvent.INVOKE);

      ShiroActuator act = (ShiroActuator) Configuration.instance().getActuator(ShiroActuator.DEFAULTNAME);
      act.setRequiresUser(true);

      Subject subject = SecurityUtils.getSubject();
      subject.logout();

      TComplexEntity ent1 = new TComplexEntity();
      ent1.setCompValue(22);
      Assert.assertEquals(0, ent1.getCompValue());
   }

   @Test
   public void requiresUser() throws Exception {
      CoreTestService.registerSetpoint(TComplexEntity.class, ShiroActuator.DEFAULTNAME, "setCompValue",
            ControlEvent.INVOKE);

      ShiroActuator act = (ShiroActuator) Configuration.instance().getActuator(ShiroActuator.DEFAULTNAME);
      act.setRequiresUser(true);

      ShiroTestBase.authenticateShiro("lonestarr", "vespa");

      TComplexEntity ent1 = new TComplexEntity();
      ent1.setCompValue(22);
      Assert.assertEquals(22, ent1.getCompValue());
   }

   @Test
   public void noSecondPrincipal() throws Exception {
      ShiroActuator act = (ShiroActuator) Configuration.instance().getActuator(ShiroActuator.DEFAULTNAME);
      act.setSecondPrincipal(true);
      act.setRequiresUser(null);
      act.setThrowDeniedException(true);

      Resource res = new Resource(JpaResourceHandler.class, new TEntity());
      EventMetadata meta = new EventMetadata(ControlEvent.PERSIST, res);
      act.beforeEvent(meta);
      Assert.assertEquals(ExecutionStatus.DENIED, meta.getExecutionStatus());
   }

   @Test
   public void hasSecond() throws Exception {
      ShiroActuator act = (ShiroActuator) Configuration.instance().getActuator(ShiroActuator.DEFAULTNAME);
      act.setSecondPrincipal(true);
      act.setRequiresGuest(null);
      act.setThrowDeniedException(true);
      act.getHasAllRoles().clear();
      act.setIsPermittedAll(null);
      Context.sessionScope().setSecondUser("secondU");
      Context.internalSessionScope().setProperty(InternalSessionScope.SECOND_PRINCIPAL,
            new Subject.Builder().buildSubject());
      Resource res = new Resource(JpaResourceHandler.class, new TEntity());
      EventMetadata meta = new EventMetadata(ControlEvent.PERSIST, res);
      act.beforeEvent(meta);
      Assert.assertEquals(ExecutionStatus.EXECUTING, meta.getExecutionStatus());
   }

}
