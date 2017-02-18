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
package com.logitags.cibet.sensor.jpa;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Query;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.RequestScopeContext;

@RunWith(MockitoJUnitRunner.class)
public class CibetQueryTest {

   @Mock
   protected CibetEntityManager em;

   @Mock
   protected Query query;

   @AfterClass
   public static void doAfter() {
      Context.requestScope().setProperty(RequestScopeContext.EVENTRESULT, null);
   }

   @Test
   public void getResultList() {
      List<String> list = new ArrayList<String>();
      list.add("one");
      list.add("two");
      list.add("three");
      Mockito.when(query.getResultList()).thenReturn(list);

      CibetQuery cq = new CibetQuery(query, "", em, QueryType.NAMED_QUERY);
      List<String> result = cq.getResultList();
      Assert.assertNotNull(result);
      Assert.assertEquals(3, result.size());
      Assert.assertEquals("one", result.get(0));
      Assert.assertEquals("two", result.get(1));
      Assert.assertEquals("three", result.get(2));
   }

   @Test
   public void getSingleResult() {
      Context.requestScope().setEntityManager(em);

      List<String> list = new ArrayList<String>();
      list.add("one");
      list.add("two");
      list.add("three");
      Mockito.when(query.getSingleResult()).thenReturn(list);

      CibetQuery cq = new CibetQuery(query, "", em, QueryType.NAMED_QUERY);
      List<String> result = (List<String>) cq.getSingleResult();
      Assert.assertNotNull(result);
      Assert.assertEquals(3, result.size());
      Assert.assertEquals("one", result.get(0));
      Assert.assertEquals("two", result.get(1));
      Assert.assertEquals("three", result.get(2));
   }

}
