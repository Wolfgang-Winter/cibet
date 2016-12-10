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

import org.junit.Test;
import org.springframework.security.access.method.MethodSecurityMetadataSource;
import org.springframework.security.access.prepost.PrePostAnnotationSecurityMetadataSource;

public class CibetDelegatingMethodSecurityMetadataSourceTest {

   @Test
   public void afterPropertiesSet() throws Exception {
      CibetDelegatingMethodSecurityMetadataSource ms = new CibetDelegatingMethodSecurityMetadataSource();
      MethodSecurityMetadataSource msm = SetpointExpressionSecurityMetadataSource
            .instance();
      ms.setOriginalMetadataSource(msm);
      ms.afterPropertiesSet();
   }

   @Test(expected = IllegalArgumentException.class)
   public void getAttributes() throws Exception {
      CibetDelegatingMethodSecurityMetadataSource ms = new CibetDelegatingMethodSecurityMetadataSource();
      MethodSecurityMetadataSource msm = new PrePostAnnotationSecurityMetadataSource(
            null);
      ms.setOriginalMetadataSource(msm);
      ms.getAttributes("nixi");
   }

}
