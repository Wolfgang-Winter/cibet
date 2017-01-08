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
package com.logitags.cibet.core;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.logitags.cibet.actuator.common.DeniedException;
import com.logitags.cibet.actuator.common.PostponedEjbException;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalRequestScope;
import com.logitags.cibet.resource.HttpRequestData;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.http.HttpRequestResourceHandler;
import com.logitags.cibet.sensor.jpa.JpaResourceHandler;
import com.logitags.cibet.sensor.pojo.MethodResourceHandler;

public class EventMetadataTest {

   @Test
   public void getInvoker() {
      Resource res = new Resource(JpaResourceHandler.class, null);
      Object o = res.getInvoker();
      Assert.assertTrue(o instanceof StackTraceElement[]);
      Assert.assertTrue(((StackTraceElement[]) o).length > 0);
   }

   @Test
   public void getTargetType() {
      Resource res = new Resource(JpaResourceHandler.class, EventMetadataTest.class, (Method) null, null);
      String targetType = res.getTargetType();
      Assert.assertEquals(EventMetadataTest.class.getName(), targetType);

      res = new Resource(JpaResourceHandler.class, new EventMetadataTest(), (Method) null, null);
      targetType = res.getTargetType();
      Assert.assertEquals(EventMetadataTest.class.getName(), targetType);

      res = new Resource(JpaResourceHandler.class, null, (Method) null, null);
      targetType = res.getTargetType();
      Assert.assertEquals(null, targetType);
   }

   @Test
   public void initEventMetadataDefault() {
      Context.internalRequestScope().setCaseId("AAA");

      Resource res = new Resource(MethodResourceHandler.class, EventMetadataTest.class, (Method) null, null);
      EventMetadata cem = new EventMetadata(ControlEvent.DELETE, res);
      Assert.assertTrue(cem.getCaseId().equals("AAA"));
      Assert.assertEquals(ExecutionStatus.EXECUTING, cem.getExecutionStatus());
   }

   @Test
   public void initEventMetadataReject() {
      Context.internalRequestScope().setCaseId("BBB");

      Resource res = new Resource(MethodResourceHandler.class, EventMetadataTest.class, (Method) null, null);
      EventMetadata cem = new EventMetadata(ControlEvent.REJECT, res);
      Assert.assertTrue(cem.getCaseId().equals("BBB"));
      Assert.assertNotNull(Context.internalRequestScope().getCaseId());
      Assert.assertNull(Context.requestScope().getRemark());
      Assert.assertNull(Context.internalRequestScope().getProperty(InternalRequestScope.CONTROLEVENT));
   }

   @Test(expected = DeniedException.class)
   public void evaluateEventExecuteStatusDeniedWithException() throws Throwable {
      Resource res = new Resource(MethodResourceHandler.class, EventMetadataTest.class, (Method) null, null);
      EventMetadata cem = new EventMetadata(ControlEvent.REJECT, res);
      res.setResultObject("res");
      cem.setException(new DeniedException("ex"));
      cem.setExecutionStatus(ExecutionStatus.DENIED);
      cem.evaluateEventExecuteStatus();
   }

   @Test
   public void evaluateEventExecuteStatusPostponed() throws Throwable {
      Resource res = new Resource(MethodResourceHandler.class, EventMetadataTest.class, (Method) null, null);
      EventMetadata cem = new EventMetadata(ControlEvent.REJECT, res);
      res.setResultObject("res");
      cem.setExecutionStatus(ExecutionStatus.POSTPONED);
      cem.evaluateEventExecuteStatus();
      Assert.assertEquals("res", res.getResultObject());
   }

   @Test(expected = PostponedEjbException.class)
   public void evaluateEventExecuteStatusPostponedWithException() throws Throwable {
      Resource res = new Resource(MethodResourceHandler.class, EventMetadataTest.class, (Method) null, null);
      EventMetadata cem = new EventMetadata(ControlEvent.REJECT, res);
      res.setResultObject("res");
      cem.setException(new PostponedEjbException());
      cem.setExecutionStatus(ExecutionStatus.POSTPONED);
      cem.evaluateEventExecuteStatus();
   }

   @Test(expected = IllegalArgumentException.class)
   public void addParameterNullKey() throws IOException {
      Resource res = new Resource(HttpRequestResourceHandler.class, (String) null, (String) null,
            (HttpRequestData) null);
      res.addParameter(null, "Hase", ParameterType.HTTP_ATTRIBUTE);
   }

   @Test
   public void addParameter() throws IOException {
      Resource res = new Resource(HttpRequestResourceHandler.class, (String) null, (String) null,
            (HttpRequestData) null);
      res.addParameter("k1", "Hase", ParameterType.HTTP_ATTRIBUTE);
      res.addParameter("k2", "Igel", ParameterType.HTTP_HEADER);
      res.addParameter("k3", null, ParameterType.HTTP_ATTRIBUTE);

      List<ResourceParameter> params = res.getParameters();
      Assert.assertEquals(3, params.size());
      Assert.assertEquals("k1", params.get(0).getName());
      Assert.assertEquals("k2", params.get(1).getName());
      Assert.assertEquals("k3", params.get(2).getName());
      Assert.assertEquals(1, params.get(0).getSequence());
      Assert.assertEquals(2, params.get(1).getSequence());
      Assert.assertEquals(3, params.get(2).getSequence());
   }

}
