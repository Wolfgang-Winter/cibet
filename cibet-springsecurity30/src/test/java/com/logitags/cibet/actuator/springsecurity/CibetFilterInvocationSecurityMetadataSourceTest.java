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
import java.util.LinkedHashMap;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.web.access.expression.CibetWebExpressionConfigAttribute;
import org.springframework.security.web.access.intercept.DefaultFilterInvocationSecurityMetadataSource;
import org.springframework.security.web.access.intercept.RequestKey;

@RunWith(MockitoJUnitRunner.class)
public class CibetFilterInvocationSecurityMetadataSourceTest {

   @Mock
   private HttpServletRequest req;

   @Mock
   private HttpServletResponse resp;

   @Mock
   private FilterChain chain;

   @Test(expected = IllegalArgumentException.class)
   public void testgetAttributesFilter() throws Exception {
      CibetFilterInvocationSecurityMetadataSource fs = new CibetFilterInvocationSecurityMetadataSource(
            new DefaultFilterInvocationSecurityMetadataSource(null,
                  new LinkedHashMap<RequestKey, Collection<ConfigAttribute>>()));
      // CibetFilterInvocationSecurityMetadataSource fs = new CibetFilterInvocationSecurityMetadataSource(
      // new DefaultFilterInvocationSecurityMetadataSource(new LinkedHashMap()));
      fs.getAttributes("nixi");
   }

   @Test
   public void testgetCibetAttributesExpression() throws Exception {
      CibetFilterInvocationSecurityMetadataSource fs = new CibetFilterInvocationSecurityMetadataSource(
            new DefaultFilterInvocationSecurityMetadataSource(null,
                  new LinkedHashMap<RequestKey, Collection<ConfigAttribute>>()));
      // CibetFilterInvocationSecurityMetadataSource fs = new CibetFilterInvocationSecurityMetadataSource(
      // new DefaultFilterInvocationSecurityMetadataSource(new LinkedHashMap()));
      CibetFilterInvocation fi = new CibetFilterInvocation(req, resp, chain);
      fi.setAccessRuleExpression("hasRole('Walter')");
      Collection<ConfigAttribute> c = fs.getAttributes(fi);
      Assert.assertEquals(1, c.size());
      ConfigAttribute ca = c.iterator().next();
      Assert.assertTrue(ca instanceof CibetWebExpressionConfigAttribute);
   }

   @Test
   public void testgetCibetAttributes() throws Exception {
      CibetFilterInvocationSecurityMetadataSource fs = new CibetFilterInvocationSecurityMetadataSource(
            new DefaultFilterInvocationSecurityMetadataSource(null,
                  new LinkedHashMap<RequestKey, Collection<ConfigAttribute>>()));
      // CibetFilterInvocationSecurityMetadataSource fs = new CibetFilterInvocationSecurityMetadataSource(
      // new DefaultFilterInvocationSecurityMetadataSource(new LinkedHashMap()));
      CibetFilterInvocation fi = new CibetFilterInvocation(req, resp, chain);
      fi.setAccessRule("ROLE_VIEH");
      Collection<ConfigAttribute> c = fs.getAttributes(fi);
      Assert.assertEquals(1, c.size());
      ConfigAttribute ca = c.iterator().next();
      Assert.assertTrue(ca instanceof SecurityConfig);
   }

}
