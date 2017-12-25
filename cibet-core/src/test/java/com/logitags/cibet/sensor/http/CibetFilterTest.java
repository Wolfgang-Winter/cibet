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
package com.logitags.cibet.sensor.http;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.servlet.FilterChain;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.jndi.EjbLookup;
import com.logitags.cibet.resource.Resource;

@RunWith(MockitoJUnitRunner.class)
public class CibetFilterTest {

   private static Logger log = Logger.getLogger(CibetFilterTest.class);

   @Mock
   private HttpServletResponse httpResponse;

   @Mock
   private HttpServletRequest httpRequest;

   @Mock
   private HttpSession httpSession;

   @Mock
   private ServletInputStream servletInputStream;

   @Mock
   private FilterChain filterChain;

   @Mock
   private EntityManager em;

   @Mock
   private EntityTransaction emTrans;

   private void initCibetContext() {
      Mockito.when(em.getTransaction()).thenReturn(emTrans);
      Context.internalRequestScope().setEntityManager(em);
   }

   private void initHttpServletRequest() throws IOException {
      Mockito.when(servletInputStream.read()).thenReturn(25, 34, 45, 5, 34, -1);
      Mockito.when(httpRequest.getInputStream()).thenReturn(servletInputStream);
      Mockito.when(httpRequest.getRequestURL()).thenReturn(new StringBuffer("mock request URL"));
      Mockito.when(httpRequest.getMethod()).thenReturn("GET");

      Mockito.when(httpRequest.getSession()).thenReturn(httpSession);

      Vector<String> v = new Vector<String>();
      v.add("HTTP_X_FORWARDED_FOR".toLowerCase());
      v.add(Headers.CIBET_CONTROLEVENT.name());
      v.add("CIBET_xxxx,multi-heaqder");

      // Enumeration<Object> headerNames = new StringTokenizer(
      // "HTTP_X_FORWARDED_FOR".toLowerCase() + ","
      // + HttpRequestInvoker.CONTROLEVENT.name()
      // + ",CIBET_xxxx,multi-heaqder", ",");
      Mockito.when(httpRequest.getHeaderNames()).thenReturn(v.elements());

      // Enumeration<Object> header1 = new StringTokenizer("192.168.4.1", ",");
      Vector<String> v1 = new Vector<String>();
      v1.add("192.168.4.1");
      Mockito.when(httpRequest.getHeaders("HTTP_X_FORWARDED_FOR".toLowerCase())).thenReturn(v1.elements());

      // Enumeration<Object> header2 = new StringTokenizer(
      // ControlEvent.DC_REDO.name(), ",");
      Vector<String> v2 = new Vector<String>();
      v2.add(ControlEvent.REDO.name());
      Mockito.when(httpRequest.getHeaders(Headers.CIBET_CONTROLEVENT.name())).thenReturn(v2.elements());

      // Enumeration<Object> header3 = new StringTokenizer("192.168.4.1", ",");
      Vector<String> v3 = new Vector<String>();
      v3.add("192.168.4.1");
      Mockito.when(httpRequest.getHeaders("CIBET_xxxx")).thenReturn(v3.elements());

      // Enumeration<Object> header4 = new StringTokenizer("teil1,teil2", ",");
      Vector<String> v4 = new Vector<String>();
      v4.add("teil1");
      v4.add("teil2");
      Mockito.when(httpRequest.getHeaders("multi-heaqder")).thenReturn(v4.elements());

      Mockito.when(httpRequest.getHeader(Headers.CIBET_CASEID.name())).thenReturn("caseId123");

      Mockito.when(httpRequest.getHeader(Headers.CIBET_REMARK.name())).thenReturn("rermark555");

      // Enumeration<Object> attributeNames = new StringTokenizer("att1", ",");
      Vector<String> v5 = new Vector<String>();
      v5.add("att1");

      Mockito.when(httpRequest.getAttributeNames()).thenReturn(v5.elements());
      Mockito.when(httpRequest.getAttribute("att1")).thenReturn(new Date());

      Map<String, String[]> map = new HashMap<String, String[]>();
      map.put("Vorname", new String[] { "Klaus" });
      map.put("andere", new String[] { "Willi", "Gerd" });
      Mockito.when(httpRequest.getParameterMap()).thenReturn(map);
   }

   @Test
   public void doFilter() throws Exception {
      log.debug("start test doFilter");
      initCibetContext();
      initHttpServletRequest();
      CibetFilter fi = new CibetFilter();
      Mockito.when(httpRequest.getRequestURI()).thenReturn("xxxx");
      fi.doFilter(httpRequest, httpResponse, filterChain);

      Assert.assertNull(Context.requestScope().getCaseId());
      Assert.assertNull(Context.requestScope().getRemark());
   }

   @Test
   public void doFilterNoEntityManager() throws Exception {
      log.debug("start test doFilterNoEntityManager");
      EjbLookup.clearCache();
      Context.internalRequestScope().setEntityManager(null);
      initHttpServletRequest();
      CibetFilter fi = new CibetFilter();
      Mockito.when(httpRequest.getRequestURI()).thenReturn("xxxx");
      fi.doFilter(httpRequest, httpResponse, filterChain);

   }

   @Test
   public void addBody() throws Exception {
      initHttpServletRequest();
      HttpRequestData reqdata = new HttpRequestData(httpRequest);
      HttpRequestResource resource = new HttpRequestResource("", "", reqdata);

      CibetFilter filter = new CibetFilter();
      Method m = filter.getClass().getDeclaredMethod("addBody", ServletRequest.class, Resource.class);
      m.setAccessible(true);
      m.invoke(filter, httpRequest, resource);
      Assert.assertEquals(1, resource.getParameters().size());
      byte[] bytes = (byte[]) resource.getParameters().iterator().next().getUnencodedValue();
      Assert.assertEquals(5, bytes.length);
      Assert.assertEquals(25, bytes[0]);
      Assert.assertEquals(34, bytes[1]);
      Assert.assertEquals(45, bytes[2]);
      Assert.assertEquals(5, bytes[3]);
      Assert.assertEquals(34, bytes[4]);
   }
}
