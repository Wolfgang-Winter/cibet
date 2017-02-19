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
package com.logitags.cibet.sensor.pojo;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.cibethelper.base.StaticFactoryService;
import com.cibethelper.ejb.CibetTestEJB;
import com.cibethelper.entities.TEntity;

@RunWith(MockitoJUnitRunner.class)
public class CibetAspectTest {

   @Mock
   private ProceedingJoinPoint joinPoint;

   @Mock
   private MethodSignature signature;

   @Spy
   private CibetTestEJB ejb = new CibetTestEJB();

   @Test
   public void intercept() throws Throwable {
      TEntity te = new TEntity("Hase", 77, "Hundeeigner");
      Mockito.doReturn(te).when(ejb).findTEntity(5);
      Method m = CibetTestEJB.class.getMethod("findTEntity", long.class);
      Mockito.when(joinPoint.getSignature()).thenReturn(signature);
      Mockito.when(signature.getName()).thenReturn("find");
      Mockito.when(((MethodSignature) joinPoint.getSignature()).getMethod()).thenReturn(m);
      Mockito.when(joinPoint.getTarget()).thenReturn(ejb);

      Mockito.when(joinPoint.getArgs()).thenReturn(new Object[] { 5 });
      Mockito.when(joinPoint.proceed()).thenReturn(te);

      CibetIntercept ann = StaticFactoryService.class.getAnnotation(CibetIntercept.class);
      CibetAspect ceptor = new CibetAspect();
      Object result = ceptor.intercept(joinPoint, ann);
      Assert.assertNotNull(result);
      Assert.assertSame(te, result);
   }

}
