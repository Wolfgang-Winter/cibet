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
package com.logitags.cibet.sensor.ejb;

import java.lang.reflect.Method;

import javax.interceptor.InvocationContext;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.cibethelper.base.MockEjb;
import com.cibethelper.entities.TEntity;

@RunWith(MockitoJUnitRunner.class)
public class CibetInterceptorTest {

   @Mock
   private InvocationContext ctx;

   @Spy
   private MockEjb ejb = new MockEjb();

   @Test
   public void controlInvoke() throws Throwable {
      TEntity te = new TEntity("Hase", 77, "Hundeeigner");
      Mockito.doReturn(te).when(ejb).findTEntity(5);
      Method m = MockEjb.class.getMethod("findTEntity", long.class);
      Mockito.when(ctx.getTarget()).thenReturn(ejb);
      Mockito.when(ctx.getMethod()).thenReturn(m);
      Mockito.when(ctx.getParameters()).thenReturn(new Object[] { 5 });
      Mockito.when(ctx.proceed()).thenReturn(te);

      CibetInterceptor ceptor = new CibetInterceptor();
      Object result = ceptor.controlInvoke(ctx);
      Assert.assertNotNull(result);
      Assert.assertSame(te, result);
   }
}
