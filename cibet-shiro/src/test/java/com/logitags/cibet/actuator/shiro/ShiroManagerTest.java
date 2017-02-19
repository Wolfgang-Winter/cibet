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

import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.Factory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cibethelper.DummySecurityManager;
import com.cibethelper.ShiroTestBase;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalSessionScope;

/**
 * add -javaagent:${project_loc}\..\cibet-material\technics\aspectjweaver-1.6.9. jar to java command
 */
public class ShiroManagerTest {

   private static Logger log = Logger.getLogger(ShiroManagerTest.class);

   private static SecurityManager securityManager;

   @BeforeClass
   public static void initShiro() {
      Factory<SecurityManager> factory = new IniSecurityManagerFactory("classpath:shiro.init");
      securityManager = factory.getInstance();
      SecurityUtils.setSecurityManager(securityManager);
   }

   @Test
   public void logon() throws Exception {
      ShiroTestBase.authenticateShiro("lonestarr", "vespa");

      AuthenticationToken token = new UsernamePasswordToken("admin", "secret");
      ShiroService.logonSecondUser(token);

      Subject mainUser = SecurityUtils.getSubject();
      Assert.assertEquals("lonestarr", mainUser.getPrincipal());
      Assert.assertTrue(mainUser.hasRole("goodguy"));

      Subject secondUser = (Subject) Context.internalSessionScope().getProperty(InternalSessionScope.SECOND_PRINCIPAL);
      Assert.assertEquals("admin", Context.sessionScope().getSecondUser());
      Assert.assertEquals("admin", secondUser.getPrincipal());
      Assert.assertTrue(secondUser.hasRole("admin"));
      Assert.assertTrue(secondUser.isPermitted("ggg"));

      ShiroService.logoffSecondUser();
      Assert.assertNull(Context.sessionScope().getSecondUser());
      Assert.assertNull(Context.internalSessionScope().getProperty(InternalSessionScope.SECOND_PRINCIPAL));
   }

   @Test(expected = ClassCastException.class)
   public void customSecurityManager() throws Exception {
      try {
         SecurityUtils.setSecurityManager(new DummySecurityManager());

         AuthenticationToken token = new UsernamePasswordToken("admin", "secret");
         ShiroService.logonSecondUser(token);
      } finally {
         SecurityUtils.setSecurityManager(securityManager);
      }
   }

}
