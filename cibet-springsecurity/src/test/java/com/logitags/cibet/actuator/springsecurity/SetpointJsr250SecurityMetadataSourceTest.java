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

import java.util.Collection;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.annotation.Jsr250SecurityConfig;
import org.springframework.security.core.context.SecurityContextHolder;

import com.cibethelper.SpringTestBase;
import com.cibethelper.entities.TComplexEntity;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.core.ControlEvent;

public class SetpointJsr250SecurityMetadataSourceTest extends SpringTestBase {

   private static Logger log = Logger.getLogger(SetpointJsr250SecurityMetadataSourceTest.class);

   @After
   public void after() {
      SecurityContextHolder.getContext().setAuthentication(null);
   }

   @Test
   public void getAttributes1() {
      SetpointJsr250SecurityMetadataSource mds = (SetpointJsr250SecurityMetadataSource) SetpointJsr250SecurityMetadataSource
            .instance();
      CibetMethodInvocation mi = new CibetMethodInvocation(null, null, null, null, null);
      mi.addRule("DenyAll", "{\"ROLE_ADMIN\"}");

      Collection<ConfigAttribute> attributes = mds.getAttributes(mi);
      Assert.assertEquals(1, attributes.size());
      Assert.assertEquals(Jsr250SecurityConfig.class, attributes.iterator().next().getClass());
      Jsr250SecurityConfig sc = (Jsr250SecurityConfig) attributes.iterator().next();
      Assert.assertEquals(Jsr250SecurityConfig.DENY_ALL_ATTRIBUTE, sc);
   }

   @Test
   public void getAttributes2() {
      SetpointJsr250SecurityMetadataSource mds = (SetpointJsr250SecurityMetadataSource) SetpointJsr250SecurityMetadataSource
            .instance();
      CibetMethodInvocation mi = new CibetMethodInvocation(null, null, null, null, null);
      mi.addRule("PermitAll", "{\"ROLE_ADMIN\"}");

      Collection<ConfigAttribute> attributes = mds.getAttributes(mi);
      Assert.assertEquals(1, attributes.size());
      Assert.assertEquals(Jsr250SecurityConfig.class, attributes.iterator().next().getClass());
      Jsr250SecurityConfig sc = (Jsr250SecurityConfig) attributes.iterator().next();
      Assert.assertEquals(Jsr250SecurityConfig.PERMIT_ALL_ATTRIBUTE, sc);
   }

   @Test
   public void getAttributes3() {
      SetpointJsr250SecurityMetadataSource mds = (SetpointJsr250SecurityMetadataSource) SetpointJsr250SecurityMetadataSource
            .instance();
      CibetMethodInvocation mi = new CibetMethodInvocation(null, null, null, null, null);
      mi.addRule("RolesAllowed", "{\"ROLE_ADMIN\"}");

      Collection<ConfigAttribute> attributes = mds.getAttributes(mi);
      Assert.assertEquals(1, attributes.size());
      Assert.assertEquals(Jsr250SecurityConfig.class, attributes.iterator().next().getClass());
      Jsr250SecurityConfig sc = (Jsr250SecurityConfig) attributes.iterator().next();
      Assert.assertEquals("ROLE_ADMIN", sc.getAttribute());
   }

   @Test
   public void invokeDeny() {
      initContext("spring-context_jsr250.xml");
      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, "setCompValue", ControlEvent.INVOKE);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setDenyAll(true);

      authenticate("ROLE_ERNST");

      TComplexEntity ent1 = new TComplexEntity();
      ent1.setCompValue(22);
      Assert.assertEquals(0, ent1.getCompValue());
      initConfiguration("cibet-config.xml");
   }

   @Test
   public void invokePermit() {
      initContext("spring-context_jsr250.xml");
      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, "setCompValue", ControlEvent.INVOKE);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPermitAll(false);
      authenticate("ROLE_ERNST");

      TComplexEntity ent1 = new TComplexEntity();
      ent1.setCompValue(22);
      Assert.assertEquals(22, ent1.getCompValue());
      initConfiguration("cibet-config.xml");
   }

   @Test
   public void invokeRolesAllowed() {
      initContext("spring-context_jsr250.xml");
      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, "setCompValue", ControlEvent.INVOKE);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setRolesAllowed("'LOLLO,Henry'");
      authenticate("ROLE_ERNST");

      TComplexEntity ent1 = new TComplexEntity();
      ent1.setCompValue(22);
      Assert.assertEquals(0, ent1.getCompValue());

      authenticate("LOLLO");

      TComplexEntity ent2 = new TComplexEntity();
      ent2.setCompValue(78);
      Assert.assertEquals(78, ent2.getCompValue());
      initConfiguration("cibet-config.xml");
   }

}
