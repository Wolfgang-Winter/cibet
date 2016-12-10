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
package com.logitags.cibet.actuator.springsecurity;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;

import com.cibethelper.SpringExampleBean;
import com.cibethelper.SpringTestBase;
import com.cibethelper.SpringTestInterface;
import com.cibethelper.entities.TComplexEntity;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalSessionScope;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.resource.HttpRequestData;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.sensor.ejb.EjbResourceHandler;
import com.logitags.cibet.sensor.http.HttpRequestResourceHandler;
import com.logitags.cibet.sensor.pojo.MethodResourceHandler;

import junit.framework.Assert;

/**
 * add -javaagent:${project_loc}\..\cibet-material\technics\aspectjweaver-1.6.9. jar to java command
 */
public class SpringSecurityActuatorTest extends SpringTestBase {

   private static Logger log = Logger.getLogger(SpringSecurityActuatorTest.class);

   @Test
   public void fixRule() throws Exception {
      SpringSecurityActuator act = new SpringSecurityActuator();
      Method m = act.getClass().getDeclaredMethod("fixRule", new Class[] { String.class });
      m.setAccessible(true);

      Object[] params = new Object[] { "hasRole('TELLER')" };
      String result = (String) m.invoke(act, params);
      log.debug(result);
      Assert.assertEquals("hasRole('TELLER')", result);

      params = new Object[] { "hasRole(    'TELLER' )" };
      result = (String) m.invoke(act, params);
      log.debug(result);
      Assert.assertEquals("hasRole('TELLER')", result);

      String rule = "hasRole('TELLER') or hASRole(\"ROLE_HASE\")";
      result = (String) m.invoke(act, new Object[] { rule });
      Assert.assertEquals("hasRole('TELLER') or hasRole('ROLE_HASE')", result);

      rule = "vorher hasRole('TELLER') or hASRole(\"ROLE_HASE\") nachher";
      result = (String) m.invoke(act, new Object[] { rule });
      Assert.assertEquals("vorher hasRole('TELLER') or hasRole('ROLE_HASE') nachher", result);

      rule = "vorher hasAnyRole('WERNER', 'HEINZ')";
      result = (String) m.invoke(act, new Object[] { rule });
      Assert.assertEquals("vorher hasAnyRole('WERNER', 'HEINZ')", result);

      rule = "vorher hasAnyRole(WERNER, HEINZ) or hasAnyRole('LILLI,Michi')";
      result = (String) m.invoke(act, new Object[] { rule });
      Assert.assertEquals("vorher hasAnyRole('WERNER', 'HEINZ') or hasAnyRole('LILLI', 'Michi')", result);

      rule = "vorher hasAnyRole(WERNER, HEINZ) or hasAnyRole('LILLI,Michi') nachher";
      result = (String) m.invoke(act, new Object[] { rule });
      Assert.assertEquals("vorher hasAnyRole('WERNER', 'HEINZ') or hasAnyRole('LILLI', 'Michi') nachher", result);

      rule = "vorher hasAnyRole(WERNER, HEINZ) or vorher and hasRole('TELLER') or hasAnyRole('LILLI,Michi') nachher";
      result = (String) m.invoke(act, new Object[] { rule });
      Assert.assertEquals(
            "vorher hasAnyRole('WERNER', 'HEINZ') or vorher and hasRole('TELLER') or hasAnyRole('LILLI', 'Michi') nachher",
            result);

      rule = "vorher hasAnyRole(  'WERNER', \"HEINZ\" ) or vorher";
      result = (String) m.invoke(act, new Object[] { rule });
      log.debug(result);
      Assert.assertEquals("vorher hasAnyRole('WERNER', 'HEINZ') or vorher", result);

      rule = null;
      result = (String) m.invoke(act, new Object[] { rule });
      log.debug(result);
      Assert.assertNull(result);
   }

   @Test
   public void invoke() throws Exception {
      ApplicationContext ctx = initContext("spring-context.xml");
      listBeansInContext(ctx);

      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, "setCompValue", ControlEvent.INVOKE);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");

      authenticate("WALTER");

      TComplexEntity ent1 = new TComplexEntity();
      ent1.setCompValue(22);
      Assert.assertEquals(22, ent1.getCompValue());
   }

   private void initPreAuthorize(String preAuth, String authenticate) {
      initContext("spring-context.xml");
      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, "setCompValue", ControlEvent.INVOKE);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize(preAuth);
      authenticate(authenticate);
   }

   @Test
   public void invokeDenied() {
      initContext("spring-context.xml");
      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, "setCompValue", ControlEvent.INVOKE);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz, 'Udo[REDO]')");
      authenticate("WALTER[REDO]");

      TComplexEntity ent1 = new TComplexEntity();
      ent1.setCompValue(22);
      Assert.assertEquals(0, ent1.getCompValue());
   }

   @Test
   public void springAuthOk() {
      ApplicationContext context = initContext("spring-context.xml");

      authenticate("ROLE_WALTER");

      SpringTestInterface bean = context.getBean("MySpringExampleBean", SpringExampleBean.class);
      String five = bean.giveFive();
      Assert.assertEquals("Five", five);
   }

   @Test(expected = AccessDeniedException.class)
   public void springAuthDenied() {
      ApplicationContext context = initContext("spring-context.xml");

      authenticate("ROLE_WILLE");

      SpringTestInterface bean = context.getBean("MySpringExampleBean", SpringExampleBean.class);
      bean.giveFive();
   }

   @Test
   public void invokePostDenied() {
      log.info("invokePostDenied()");
      initContext("spring-context.xml");
      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, "setAndGetCompValue",
            ControlEvent.INVOKE);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPostAuthorize("hasAnyRole('Heinz, 'Udo[REDO]')");

      authenticate("WALTER[REDO]");

      TComplexEntity ent1 = new TComplexEntity();
      int res = ent1.setAndGetCompValue(22);
      Assert.assertEquals(22, ent1.getCompValue());
      Assert.assertEquals(0, res);
   }

   @Test
   public void invokePostDeniedNotAuthenticated() {
      log.info("invokePostDeniedNotAuthenticated()");
      initContext("spring-context.xml");
      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, "setAndGetCompValue",
            ControlEvent.INVOKE);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPostAuthorize("hasAnyRole('Heinz, 'Udo[REDO]')");

      SecurityContextHolder.clearContext();
      SecurityContextHolder.createEmptyContext();

      TComplexEntity ent1 = new TComplexEntity();
      int res = ent1.setAndGetCompValue(22);
      Assert.assertEquals(22, ent1.getCompValue());
      Assert.assertEquals(0, res);
   }

   @Test
   public void invokePostOk() {
      initContext("spring-context.xml");
      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, "setAndGetCompValue",
            ControlEvent.INVOKE);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPostAuthorize("hasAnyRole('Heinz, 'Udo[REDO]')");

      authenticate("Heinz");

      TComplexEntity ent1 = new TComplexEntity();
      int res = ent1.setAndGetCompValue(22);
      Assert.assertEquals(22, ent1.getCompValue());
      Assert.assertEquals(22, res);
   }

   @Test
   public void invokePostFilter() {
      initContext("spring-context.xml");
      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, "giveCollection", ControlEvent.INVOKE);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPostFilter("filterObject == 'eins'");

      authenticate("HeinzI");

      TComplexEntity ent1 = new TComplexEntity();
      List<String> res = ent1.giveCollection();
      Assert.assertEquals(1, res.size());
      Assert.assertTrue(res.contains("eins"));
   }

   @Test
   public void invokePreFilter() {
      log.debug("invokePreFilter()");
      initContext("spring-context.xml");
      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, "giveCollection2",
            ControlEvent.INVOKE);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreFilter("value=\"filterObject == 'eins' or filterObject == 'zwei'\", filterTarget=\"in\"");

      authenticate("HeinzI");

      List<String> l = new ArrayList<String>();
      l.add("eins");
      l.add("zwei");
      l.add("drei");
      l.add("vier");
      TComplexEntity ent1 = new TComplexEntity();
      List<String> res = ent1.giveCollection2(l);
      Assert.assertEquals(2, res.size());
      Assert.assertTrue(res.contains("eins"));
      Assert.assertTrue(res.contains("zwei"));
   }

   @Test
   public void invokePreFilterOneCollection() {
      initContext("spring-context.xml");
      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, "giveCollection2",
            ControlEvent.INVOKE);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreFilter("value=\"filterObject == 'eins' or filterObject == 'zwei'\"");

      authenticate("HeinzI");

      List<String> l = new ArrayList<String>();
      l.add("eins");
      l.add("zwei");
      l.add("drei");
      l.add("vier");
      TComplexEntity ent1 = new TComplexEntity();
      List<String> res = ent1.giveCollection2(l);
      Assert.assertEquals(2, res.size());
      Assert.assertTrue(res.contains("eins"));
      Assert.assertTrue(res.contains("zwei"));
   }

   @Test
   public void invokePreFilter2Collections() {
      initContext("spring-context.xml");
      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, "giveCollection3",
            ControlEvent.INVOKE);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreFilter("value=\"filterObject == 'eins' or filterObject == 'zwei'\", filterTarget=\"in\"");

      authenticate("HeinzI");

      List<String> l = new ArrayList<String>();
      l.add("eins");
      l.add("zwei");
      l.add("drei");
      l.add("vier");
      List<String> l2 = new ArrayList<String>();
      l2.add("e");
      l2.add("z");
      l2.add("d");
      l2.add("v");
      TComplexEntity ent1 = new TComplexEntity();
      List<String> res = ent1.giveCollection3(l, l2);
      Assert.assertEquals(2, res.size());
      Assert.assertTrue(res.contains("eins"));
      Assert.assertTrue(res.contains("zwei"));
   }

   @Test
   public void invokePreFilter2CollectionsDenied() throws Exception {
      initContext("spring-context.xml");

      SpringSecurityActuator act = new SpringSecurityActuator();
      act.setPostFilter("filterObject == 'eins'");
      Setpoint sp = new Setpoint("xx", null);
      sp.addActuator(act);

      TComplexEntity ent1 = new TComplexEntity();
      Method m = TComplexEntity.class.getMethod("giveCollection");
      Resource resource = new Resource(MethodResourceHandler.class, ent1, m, null);
      EventMetadata metadata = new EventMetadata(ControlEvent.INVOKE, resource);
      metadata.addSetpoint(sp);

      act.afterInvoke(metadata);
      Assert.assertEquals(ExecutionStatus.DENIED, metadata.getExecutionStatus());
   }

   @Test
   public void invokePreFilter2CollectionsOk() throws Exception {
      initContext("spring-context.xml");

      authenticate("Heinz");

      SpringSecurityActuator act = new SpringSecurityActuator();
      act.setPostFilter("filterObject == 'eins'");
      Setpoint sp = new Setpoint("xx", null);
      sp.addActuator(act);

      TComplexEntity ent1 = new TComplexEntity();
      Method m = TComplexEntity.class.getMethod("giveCollection");
      Resource resource = new Resource(MethodResourceHandler.class, ent1, m, null);
      EventMetadata metadata = new EventMetadata(ControlEvent.INVOKE, resource);
      metadata.addSetpoint(sp);

      act.afterInvoke(metadata);
      Assert.assertEquals(ExecutionStatus.EXECUTING, metadata.getExecutionStatus());
   }

   @Test
   public void invokeAfterNoRules() throws Exception {
      log.debug("start invokeAfterNoRules()");
      initContext("spring-context.xml");

      SpringSecurityActuator act = new SpringSecurityActuator();
      Setpoint sp = new Setpoint("xx", null);
      sp.addActuator(act);

      authenticate("Heinz");

      TComplexEntity ent1 = new TComplexEntity();
      Method m = TComplexEntity.class.getMethod("giveCollection");

      Resource resource = new Resource(MethodResourceHandler.class, ent1, m, null);
      EventMetadata metadata = new EventMetadata(ControlEvent.INVOKE, resource);
      metadata.addSetpoint(sp);
      act.afterInvoke(metadata);
      // Assert.assertEquals(ExecutionStatus.EXECUTED,
      // Context.requestScope().getExecutedEventResult()
      // .getExecutionStatus());
      Assert.assertEquals(ExecutionStatus.EXECUTING, metadata.getExecutionStatus());
   }

   @Test
   public void invokeBeforeNoRules() throws Exception {
      log.debug("start invokeBeforeNoRules()");
      initContext("spring-context.xml");

      SpringSecurityActuator act = new SpringSecurityActuator();
      Setpoint sp = new Setpoint("xx", null);
      sp.addActuator(act);

      authenticate("Heinz");

      TComplexEntity ent1 = new TComplexEntity();
      Method m = TComplexEntity.class.getMethod("giveCollection");
      Resource resource = new Resource(EjbResourceHandler.class, ent1, m, null);
      EventMetadata ctx = new EventMetadata(ControlEvent.INVOKE, resource);
      ctx.addSetpoint(sp);

      act.beforeEvent(ctx);
      // Method beforeM =
      // SpringSecurityActuator.class.getDeclaredMethod("before",
      // EventMetadata.class);
      // beforeM.setAccessible(true);
      // beforeM.invoke(act, ctx);
      // Assert.assertEquals(ExecutionStatus.EXECUTED,
      // Context.requestScope().getExecutedEventResult()
      // .getExecutionStatus());
      Assert.assertEquals(ExecutionStatus.EXECUTING, ctx.getExecutionStatus());
   }

   @Test
   public void invokeTwoActuators() throws Exception {
      initContext("spring-context.xml");

      SpringSecurityActuator act1 = new SpringSecurityActuator();
      act1.setName("SPRING1");
      act1.setPreAuthorize("hasAnyRole('Heinz, 'Udo[REDO]')");
      Configuration.instance().registerActuator(act1);
      registerSetpoint(TComplexEntity.class, "SPRING1", "setAndGetCompValue", ControlEvent.INVOKE);
      Thread.sleep(50);
      SpringSecurityActuator act2 = new SpringSecurityActuator();
      act2.setName("SPRING2");
      act2.setPreAuthorize("hasAnyRole('Fritz, 'Hase[REDO]')");
      Configuration.instance().registerActuator(act2);
      registerSetpoint(TComplexEntity.class, "SPRING2", "setAndGetCompValue", ControlEvent.INVOKE);

      // allow actuator1
      authenticate("Heinz", "Fritz");
      TComplexEntity ent1 = new TComplexEntity();
      int res = ent1.setAndGetCompValue(99);
      Assert.assertEquals(99, ent1.getCompValue());
      Assert.assertEquals(99, res);

      // allow actuator2
      SecurityContextHolder.clearContext();
      authenticate("Fritz");
      TComplexEntity ent2 = new TComplexEntity();
      res = ent2.setAndGetCompValue(456);
      Assert.assertEquals(0, ent2.getCompValue());
      Assert.assertEquals(0, res);
   }

   @Test
   public void invokePreFilterActuatorProperties() {
      initContext("spring-context.xml");

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance().getActuator("SPRING_SECURITY");
      act.setPreFilter("value=\"filterObject == 'eins' or filterObject == 'zwei'\", filterTarget=\"in\"");
      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, "giveCollection2",
            ControlEvent.INVOKE);

      authenticate("Heinz", "Fritz");

      List<String> l = new ArrayList<String>();
      l.add("eins");
      l.add("zwei");
      l.add("drei");
      l.add("vier");
      TComplexEntity ent1 = new TComplexEntity();
      List<String> res = ent1.giveCollection2(l);
      Assert.assertEquals(2, res.size());
      Assert.assertTrue(res.contains("eins"));
      Assert.assertTrue(res.contains("zwei"));
   }

   @Test(expected = RuntimeException.class)
   public void checkUrlAccessExpressionError() throws Exception {
      log.debug("start checkUrlAccessExpressionError");
      initContext("http-spring-NoContext.xml");

      SpringSecurityActuator act = new SpringSecurityActuator();
      Method m = act.getClass().getDeclaredMethod("checkUrlAccessExpression");
      m.setAccessible(true);
      try {
         m.invoke(act);
      } catch (InvocationTargetException e) {
         throw (Exception) e.getCause();
      }
   }

   @Test
   public void checkUrlAccessExpressionWithExpression() throws Exception {
      log.debug("start checkUrlAccessExpressionWithExpression");
      initContext("spring-context-http-Expression.xml");
      SpringSecurityActuator act = new SpringSecurityActuator();
      Method m = act.getClass().getDeclaredMethod("checkUrlAccessExpression");
      m.setAccessible(true);
      m.invoke(act);
      Field f = act.getClass().getDeclaredField("urlAccessExpression");
      f.setAccessible(true);
      Assert.assertEquals(true, f.getBoolean(act));
   }

   @Test
   public void checkUrlAccessExpression() throws Exception {
      log.debug("start checkUrlAccessExpression");
      initContext("spring-context-http.xml");
      SpringSecurityActuator act = new SpringSecurityActuator();
      Method m = act.getClass().getDeclaredMethod("checkUrlAccessExpression");
      m.setAccessible(true);
      m.invoke(act);
      Field f = act.getClass().getDeclaredField("urlAccessExpression");
      f.setAccessible(true);
      Assert.assertEquals(false, f.getBoolean(act));
   }

   @Test
   public void beforeHttpNoRule() {
      log.debug("start beforeHttpNoRule");
      SpringSecurityActuator act = new SpringSecurityActuator();
      Resource resource = new Resource(HttpRequestResourceHandler.class, "http://localhost/dom", "GET", null);
      EventMetadata ctx = new EventMetadata(ControlEvent.INVOKE, resource);
      act.beforeEvent(ctx);
   }

   @Test
   public void beforeHttpNoFilterSecurityInterceptor() {
      log.debug("start beforeHttpNoFilterSecurityInterceptor");
      initContext("spring-context.xml");
      SpringSecurityActuator act = new SpringSecurityActuator();
      act.setUrlAccess("ROLE_USER");
      HttpRequestData rdata = new HttpRequestData("http://localhost/dom", null, "http://localhost/dom");

      Resource resource = new Resource(HttpRequestResourceHandler.class, "http://localhost/dom", "GET", rdata);
      EventMetadata ctx = new EventMetadata(ControlEvent.INVOKE, resource);
      try {
         act.beforeEvent(ctx);
         Assert.fail();
      } catch (Exception e) {
         log.debug(e.getMessage(), e);
         Assert.assertTrue(
               e.getMessage().startsWith("Failed to find a FilterSecurityInterceptor bean in Spring context"));
      }
   }

   @Test
   public void beforeHttpNoMetadataSource() {
      log.debug("start beforeHttpNoMetadataSource");
      ApplicationContext context = initContext("spring-context-http-Expression.xml");
      SpringSecurityActuator act = new SpringSecurityActuator();
      act.setUrlAccess("ROLE_USER");

      HttpRequestData rdata = new HttpRequestData("http://localhost/dom", null, "http://localhost/dom");
      Resource resource = new Resource(HttpRequestResourceHandler.class, "http://localhost/dom", "GET", rdata);
      EventMetadata ctx = new EventMetadata(ControlEvent.INVOKE, resource);

      FilterSecurityInterceptor interceptor = context.getBean(FilterSecurityInterceptor.class);
      interceptor.setSecurityMetadataSource(null);
      try {
         act.beforeEvent(ctx);
         Assert.fail();
      } catch (Exception e) {
         log.debug(e.getMessage(), e);
         Assert.assertTrue(e.getMessage().startsWith("Configuration error: FilterSecurityInterceptor bean"));
      }
   }

   @Test
   public void beforeHttp() {
      log.debug("start beforeHttp");
      initContext("spring-context-http.xml");
      SpringSecurityActuator act = new SpringSecurityActuator();
      act.setUrlAccess("ROLE_USER");

      HttpRequestData rdata = new HttpRequestData("http://localhost/dom", null, "http://localhost/dom");
      Resource resource = new Resource(HttpRequestResourceHandler.class, "http://localhost/dom", "GET", rdata);
      EventMetadata ctx = new EventMetadata(ControlEvent.INVOKE, resource);
      act.beforeEvent(ctx);
      Assert.assertEquals(ExecutionStatus.DENIED, ctx.getExecutionStatus());
   }

   @Test
   public void beforeHttpExpression() {
      log.debug("start beforeHttpExpression");
      initContext("spring-context-http-Expression.xml");
      authenticate("Heinz", "Fritz");

      SpringSecurityActuator act = new SpringSecurityActuator();
      act.setUrlAccess("hasRole ( ROLE_USER) ");

      HttpRequestData rdata = new HttpRequestData("http://localhost/dom", null, "http://localhost/dom");
      Resource resource = new Resource(HttpRequestResourceHandler.class, "http://localhost/dom", "GET", rdata);
      EventMetadata ctx = new EventMetadata(ControlEvent.INVOKE, resource);
      act.beforeEvent(ctx);
      Assert.assertEquals(ExecutionStatus.DENIED, ctx.getExecutionStatus());
   }

   @Test
   public void beforeHttpExpressionNoCredentials() {
      log.debug("start beforeHttpExpressionNoCredentials");
      initContext("spring-context-http-Expression.xml");
      SpringSecurityActuator act = new SpringSecurityActuator();
      act.setUrlAccess("hasRole ( ROLE_USER) ");

      HttpRequestData rdata = new HttpRequestData("http://localhost/dom", null, "http://localhost/dom");
      Resource resource = new Resource(HttpRequestResourceHandler.class, "http://localhost/dom", "GET", rdata);
      EventMetadata ctx = new EventMetadata(ControlEvent.INVOKE, resource);
      act.beforeEvent(ctx);
      Assert.assertEquals(ExecutionStatus.DENIED, ctx.getExecutionStatus());
   }

   @Test
   public void beforeHttpExpressionOkay() {
      log.debug("start beforeHttpExpressionOkay");
      authenticate("charlie");
      initContext("spring-context-http-Expression.xml");
      SpringSecurityActuator act = new SpringSecurityActuator();
      act.setUrlAccess("hasRole ( charlie) ");

      HttpRequestData rdata = new HttpRequestData("http://localhost/dom", null, "http://localhost/dom");
      Resource resource = new Resource(HttpRequestResourceHandler.class, "http://localhost/dom", "GET", rdata);
      EventMetadata ctx = new EventMetadata(ControlEvent.INVOKE, resource);
      act.beforeEvent(ctx);
      Assert.assertEquals(ExecutionStatus.EXECUTING, ctx.getExecutionStatus());
   }

   @Test
   public void authSecondDeny() throws Exception {
      log.debug("start authSecondDenied");
      ApplicationContext ctx = initContext("spring-context-auth.xml");
      listBeansInContext(ctx);

      Authentication auth = new UsernamePasswordAuthenticationToken("Kurt", "xx");

      // SpringSecurityService ssman = new DefaultSpringSecurityService();
      SpringSecurityService ssman = ctx.getBean(SpringSecurityService.class);
      try {
         ssman.logonSecondUser(auth);
         Assert.fail();
      } catch (BadCredentialsException e) {
      }
   }

   @Test
   public void authSecondAllow() throws Exception {
      log.debug("start authSecondAllowed");
      ApplicationContext ctx = initContext("spring-context-auth.xml");
      listBeansInContext(ctx);

      Authentication auth = new UsernamePasswordAuthenticationToken("Kurt", "Kurt");

      ctx.getBean(SpringSecurityService.class);
      SpringSecurityService ssman = ctx.getBean(SpringSecurityService.class);
      ssman.logonSecondUser(auth);
      Assert.assertEquals("Kurt", Context.sessionScope().getSecondUser());
      Assert.assertNotNull(Context.internalSessionScope().getProperty(InternalSessionScope.SECOND_PRINCIPAL));

      ssman.logoffSecondUser();
      Assert.assertNull(Context.internalSessionScope().getProperty(InternalSessionScope.SECOND_PRINCIPAL));
      Assert.assertNull(Context.sessionScope().getSecondUser());
   }

}
