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
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.common.DeniedException;
import com.logitags.cibet.actuator.common.PostponedEjbException;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalRequestScope;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.http.HttpRequestData;
import com.logitags.cibet.sensor.http.HttpRequestResource;
import com.logitags.cibet.sensor.jpa.JpaResource;
import com.logitags.cibet.sensor.pojo.MethodResource;

public class EventMetadataTest implements Serializable {

   @Test
   public void getInvoker() {
      JpaResource res = new JpaResource((Object) null);
      Object o = res.getInvoker();
      Assert.assertTrue(o instanceof StackTraceElement[]);
      Assert.assertTrue(((StackTraceElement[]) o).length > 0);
   }

   @Test
   public void getTargetType() {
      JpaResource res = new JpaResource(EventMetadataTest.class, null);
      String targetType = res.getTargetType();
      Assert.assertEquals(EventMetadataTest.class.getName(), targetType);

      res = new JpaResource(new TEntity());
      targetType = res.getTargetType();
      Assert.assertEquals(TEntity.class.getName(), targetType);

      res = new JpaResource(null, null);
      targetType = res.getTargetType();
      Assert.assertEquals(null, targetType);
   }

   @Test
   public void initEventMetadataDefault() {
      Context.internalRequestScope().setCaseId("AAA");

      MethodResource res = new MethodResource(EventMetadataTest.class, (Method) null, null);
      EventMetadata cem = new EventMetadata(ControlEvent.DELETE, res);
      Assert.assertTrue(cem.getCaseId().equals("AAA"));
      Assert.assertEquals(ExecutionStatus.EXECUTING, cem.getExecutionStatus());
   }

   @Test
   public void initEventMetadataReject() {
      Context.internalRequestScope().setCaseId("BBB");

      MethodResource res = new MethodResource(EventMetadataTest.class, (Method) null, null);
      EventMetadata cem = new EventMetadata(ControlEvent.REJECT, res);
      Assert.assertTrue(cem.getCaseId().equals("BBB"));
      Assert.assertNotNull(Context.internalRequestScope().getCaseId());
      Assert.assertNull(Context.requestScope().getRemark());
      Assert.assertNull(Context.internalRequestScope().getProperty(InternalRequestScope.CONTROLEVENT));
   }

   @Test(expected = DeniedException.class)
   public void evaluateEventExecuteStatusDeniedWithException() throws Throwable {
      MethodResource res = new MethodResource(EventMetadataTest.class, (Method) null, null);
      EventMetadata cem = new EventMetadata(ControlEvent.REJECT, res);
      res.setResultObject("res");
      cem.setException(new DeniedException("ex"));
      cem.setExecutionStatus(ExecutionStatus.DENIED);
      cem.evaluateEventExecuteStatus();
   }

   @Test
   public void evaluateEventExecuteStatusPostponed() throws Throwable {
      MethodResource res = new MethodResource(EventMetadataTest.class, (Method) null, null);
      EventMetadata cem = new EventMetadata(ControlEvent.REJECT, res);
      res.setResultObject("res");
      cem.setExecutionStatus(ExecutionStatus.POSTPONED);
      cem.evaluateEventExecuteStatus();
      Assert.assertEquals("res", res.getResultObject());
   }

   @Test(expected = PostponedEjbException.class)
   public void evaluateEventExecuteStatusPostponedWithException() throws Throwable {
      MethodResource res = new MethodResource(EventMetadataTest.class, (Method) null, null);
      EventMetadata cem = new EventMetadata(ControlEvent.REJECT, res);
      res.setResultObject("res");
      cem.setException(new PostponedEjbException());
      cem.setExecutionStatus(ExecutionStatus.POSTPONED);
      cem.evaluateEventExecuteStatus();
   }

   @Test(expected = IllegalArgumentException.class)
   public void addParameterNullKey() throws IOException {
      HttpRequestResource res = new HttpRequestResource((String) null, (String) null, (HttpRequestData) null);
      res.addParameter(null, "Hase", ParameterType.HTTP_ATTRIBUTE);
   }

   @Test
   public void addParameter() throws IOException {
      HttpRequestResource res = new HttpRequestResource((String) null, (String) null, (HttpRequestData) null);
      res.addParameter("k1", "Hase", ParameterType.HTTP_ATTRIBUTE);
      res.addParameter("k2", "Igel", ParameterType.HTTP_HEADER);
      res.addParameter("k3", null, ParameterType.HTTP_ATTRIBUTE);

      Set<ResourceParameter> params = res.getParameters();
      Assert.assertEquals(3, params.size());
      Iterator<ResourceParameter> iter = params.iterator();
      ResourceParameter p1 = iter.next();
      ResourceParameter p2 = iter.next();
      ResourceParameter p3 = iter.next();
      Assert.assertEquals("k1", p1.getName());
      Assert.assertEquals("k2", p2.getName());
      Assert.assertEquals("k3", p3.getName());
      Assert.assertEquals(1, p1.getSequence());
      Assert.assertEquals(2, p2.getSequence());
      Assert.assertEquals(3, p3.getSequence());
   }

}
