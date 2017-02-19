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
import org.springframework.security.access.expression.method.PostCibetConfigAttribute;
import org.springframework.security.access.expression.method.PreCibetConfigAttribute;

import com.cibethelper.SpringTestBase;
import com.cibethelper.entities.TComplexEntity;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.core.ControlEvent;

public class SetpointExpressionSecurityMetadataSourceTest extends SpringTestBase {

   private static Logger log = Logger.getLogger(SetpointExpressionSecurityMetadataSourceTest.class);

   @Test
   public void getAttributes1() {
      SetpointExpressionSecurityMetadataSource mds = (SetpointExpressionSecurityMetadataSource) SetpointExpressionSecurityMetadataSource
            .instance();
      CibetMethodInvocation mi = new CibetMethodInvocation(null, null, null, null, null);

      Collection<ConfigAttribute> attributes = mds.getAttributes(mi);
      Assert.assertNull(attributes);
   }

   @Test
   public void getAttributes2() throws Exception {
      SetpointExpressionSecurityMetadataSource mds = (SetpointExpressionSecurityMetadataSource) SetpointExpressionSecurityMetadataSource
            .instance();
      CibetMethodInvocation mi = new CibetMethodInvocation(null, null, null, null, null);
      mi.addRule(CibetMethodInvocation.POSTAUTHORIZE_RULE, "hasRole( 'WALTER')");

      Collection<ConfigAttribute> attributes = mds.getAttributes(mi);
      Assert.assertEquals(2, attributes.size());
      Iterator<ConfigAttribute> it = attributes.iterator();

      PreCibetConfigAttribute sc1 = (PreCibetConfigAttribute) it.next();
      PostCibetConfigAttribute sc2 = (PostCibetConfigAttribute) it.next();
      Assert.assertNotNull(sc1);
      Assert.assertNotNull(sc2);
   }

   @Test
   public void getAttributes3() throws Exception {
      SetpointExpressionSecurityMetadataSource mds = (SetpointExpressionSecurityMetadataSource) SetpointExpressionSecurityMetadataSource
            .instance();
      CibetMethodInvocation mi = new CibetMethodInvocation(null, null, null, null, null);
      mi.addRule(CibetMethodInvocation.PREFILTER_RULE,
            "value=\"filterObject == 'eins' or filterObject == 'zwei'\", filterTarget=\"in\"");

      Collection<ConfigAttribute> attributes = mds.getAttributes(mi);
      Assert.assertEquals(1, attributes.size());
      Iterator<ConfigAttribute> it = attributes.iterator();

      PreCibetConfigAttribute sc1 = (PreCibetConfigAttribute) it.next();
      Assert.assertNotNull(sc1);
   }

   @Test
   public void invokePermitAllThreeEnabled() throws Exception {
      log.debug("start invokePermitAllThreeEnabled()");
      initContext("spring-context_three.xml");

      authenticate("ROLE_ERNST");

      TComplexEntity ent1 = new TComplexEntity();
      ent1.setCompValue(22);
      Assert.assertEquals(22, ent1.getCompValue());
   }

   @Test
   public void invokeDenyAllThreeEnabled() throws Exception {
      log.debug("start invokeDenyAllThreeEnabled()");
      initContext("spring-context_three.xml");
      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, "setCompValue", ControlEvent.INVOKE);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setRolesAllowed("'LOLLO,Henry'");

      authenticate("ROLE_ERNST");

      TComplexEntity ent1 = new TComplexEntity();
      ent1.setCompValue(22);
      Assert.assertEquals(0, ent1.getCompValue());
   }

}
