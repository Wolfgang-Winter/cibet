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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.junit.Test;

import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalRequestScope;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.resource.ParameterSequenceComparator;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.ResourceParameter;

public class HttpRequestInvokerTest {

   private static Log log = LogFactory.getLog(HttpRequestInvokerTest.class);

   private ResourceParameter createParameter(String name, Object value) throws IOException {
      ResourceParameter ar1 = new ResourceParameter();
      ar1.setClassname(value.getClass().getName());
      ar1.setName(name);
      ar1.setParameterType(ParameterType.HTTP_PARAMETER);
      ar1.setUnencodedValue(value);
      return ar1;
   }

   @Test(expected = IllegalArgumentException.class)
   public void invokeNullTarget() throws Exception {
      String tenant = "test requestNullTarget";
      log.info("start " + tenant);
      HttpRequestInvoker inv = new HttpRequestInvoker();
      inv.execute(null, null, "POst", null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void invokeNullMethod() throws Exception {
      String tenant = "test requestNullMethod";
      log.info("start " + tenant);
      HttpRequestInvoker inv = new HttpRequestInvoker();
      inv.execute(null, "xx", null, null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void invokeUnknownMethod() throws Exception {
      String tenant = "test requestUnknownMethod";
      log.info("start " + tenant);
      HttpRequestInvoker inv = new HttpRequestInvoker();
      inv.execute(null, "xx", "???", null);
   }

   @Test(expected = RuntimeException.class)
   public void invokeInvalidParamTypePOST() throws Exception {
      String tenant = "test requestInvalidParamTypePoST";
      log.info("start " + tenant);
      HttpRequestInvoker inv = new HttpRequestInvoker();
      Set<ResourceParameter> params = new TreeSet<ResourceParameter>(new ParameterSequenceComparator());
      params.add(createParameter("Ente1", new Integer(6)));
      inv.execute(null, "xx", "POst", params);
   }

   @Test(expected = RuntimeException.class)
   public void invokeInvalidParamTypeGET() throws Exception {
      String tenant = "test requestInvalidParamTypeGET";
      log.info("start " + tenant);
      HttpRequestInvoker inv = new HttpRequestInvoker();
      Set<ResourceParameter> params = new TreeSet<ResourceParameter>(new ParameterSequenceComparator());
      params.add(createParameter("Ente1", new Integer(6)));
      inv.execute(null, "xx", "GET", params);
   }

   @Test
   public void invokeOk() throws Exception {
      HttpRequestInvoker inv = (HttpRequestInvoker) HttpRequestInvoker.createInstance();
      List<ResourceParameter> params = new LinkedList<ResourceParameter>();
      params.add(createParameter("content-length", "25"));
      params.add(createParameter("content-type", "25"));
      params.add(createParameter("content-md5", "25"));

      Context.requestScope().setProperty(InternalRequestScope.CONTROLEVENT, ControlEvent.DELETE);
      Context.requestScope().setCaseId("case23");
      Context.requestScope().setRemark("remarl");

      HttpResponse response = (HttpResponse) inv.execute(null, "http://www.google.de", "GET", null);
      log.debug(response);
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
   }

}
