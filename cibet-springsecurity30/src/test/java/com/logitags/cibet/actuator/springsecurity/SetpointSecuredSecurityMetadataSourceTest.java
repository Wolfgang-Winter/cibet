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
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;

import com.cibethelper.SpringTestBase;
import com.cibethelper.entities.TComplexEntity;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.core.ControlEvent;

/**
 * add -javaagent:${project_loc}\..\cibet-material\technics\aspectjweaver-1.6.9. jar to java command
 */
public class SetpointSecuredSecurityMetadataSourceTest extends SpringTestBase {

   private static Logger log = Logger.getLogger(SetpointSecuredSecurityMetadataSourceTest.class);

   @Test
   public void getAttributes1() {
      SetpointSecuredSecurityMetadataSource mds = (SetpointSecuredSecurityMetadataSource) SetpointSecuredSecurityMetadataSource
            .instance();
      CibetMethodInvocation mi = new CibetMethodInvocation(null, null, null, null, null);
      mi.addRule("Secured", "' ROLE_ADMIN\", 'ROLE_DARM'}");

      Collection<ConfigAttribute> attributes = mds.getAttributes(mi);
      Assert.assertEquals(2, attributes.size());
      Assert.assertEquals(SecurityConfig.class, attributes.iterator().next().getClass());
      Iterator<ConfigAttribute> it = attributes.iterator();
      SecurityConfig sc1 = (SecurityConfig) it.next();
      Assert.assertEquals("ROLE_ADMIN", sc1.getAttribute());
      SecurityConfig sc2 = (SecurityConfig) it.next();
      Assert.assertEquals("ROLE_DARM", sc2.getAttribute());
   }

   @Test
   public void getAttributes2() {
      SetpointSecuredSecurityMetadataSource mds = (SetpointSecuredSecurityMetadataSource) SetpointSecuredSecurityMetadataSource
            .instance();
      CibetMethodInvocation mi = new CibetMethodInvocation(null, null, null, null, null);
      mi.addRule("Secured", "{\"ROLE_ADMIN\"}");

      Collection<ConfigAttribute> attributes = mds.getAttributes(mi);
      Assert.assertEquals(1, attributes.size());
      Assert.assertEquals(SecurityConfig.class, attributes.iterator().next().getClass());
      SecurityConfig sc = (SecurityConfig) attributes.iterator().next();
      Assert.assertEquals("ROLE_ADMIN", sc.getAttribute());
   }

   @Test
   public void getAttributes3() {
      SetpointSecuredSecurityMetadataSource mds = (SetpointSecuredSecurityMetadataSource) SetpointSecuredSecurityMetadataSource
            .instance();
      CibetMethodInvocation mi = new CibetMethodInvocation(null, null, null, null, null);

      Collection<ConfigAttribute> attributes = mds.getAttributes(mi);
      Assert.assertNull(attributes);
   }

   @Test
   public void invokeAllow() {
      initContext("spring-context_secured.xml");
      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, "setCompValue", ControlEvent.INVOKE);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setSecured("ROLE_WALTER");

      authenticate("ROLE_WALTER");

      TComplexEntity ent1 = new TComplexEntity();
      ent1.setCompValue(22);
      Assert.assertEquals(22, ent1.getCompValue());
   }

   @Test
   public void invokeDeny() {
      initContext("spring-context_secured.xml");
      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, "setCompValue", ControlEvent.INVOKE);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setSecured("ROLE_WALTER");

      authenticate("ROLE_ERNST");

      TComplexEntity ent1 = new TComplexEntity();
      ent1.setCompValue(22);
      Assert.assertEquals(0, ent1.getCompValue());
   }

   @Test
   public void invokeMultiRoles() {
      initContext("spring-context_secured.xml");
      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, "setCompValue", ControlEvent.INVOKE);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setSecured("{\"ROLE_WALTER\", 'ROLE_BEN'}");

      authenticate("ROLE_BEN");

      TComplexEntity ent1 = new TComplexEntity();
      ent1.setCompValue(33);
      Assert.assertEquals(33, ent1.getCompValue());
   }

   @Test
   public void invokeIsAuthenticated() throws Exception {
      initContext("spring-context_secured.xml");
      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, "setCompValue", ControlEvent.INVOKE);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setSecured("IS_AUTHENTICATED_ANONYMOUSLY");

      authenticate("ROLE_BEN");

      TComplexEntity ent1 = new TComplexEntity();
      ent1.setCompValue(44);
      Assert.assertEquals(44, ent1.getCompValue());
   }

   @Test
   public void invokeIsNotAuthenticated() {
      initContext("spring-context_secured.xml");
      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, "setCompValue", ControlEvent.INVOKE);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setSecured("IS_AUTHENTICATED_ANONYMOUSLY");

      TComplexEntity ent1 = new TComplexEntity();
      ent1.setCompValue(44);
      Assert.assertEquals(0, ent1.getCompValue());
   }

   @Test
   public void invokeNoRule() {
      initContext("spring-context_secured.xml");

      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, "setCompValue", ControlEvent.INVOKE);

      authenticate("ROLE_BEN");

      TComplexEntity ent1 = new TComplexEntity();
      ent1.setCompValue(43);
      Assert.assertEquals(43, ent1.getCompValue());
   }

}
