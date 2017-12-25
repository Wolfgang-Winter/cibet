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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.TypedQuery;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.RequestScopeContext;

@RunWith(MockitoJUnitRunner.class)
public class CibetTypedQueryTest {

   protected static String TENANT = "testTenant";

   @Mock
   protected CibetEntityManager em;

   @Mock
   protected TypedQuery<String> query;

   @Mock
   protected TypedQuery<List> query2;

   @Mock
   protected TypedQuery<TComplexEntity> query3;

   @Mock
   protected TypedQuery<Object> query4;

   @AfterClass
   public static void doAfter() {
      Context.requestScope().setProperty(RequestScopeContext.EVENTRESULT, null);
   }

   protected TComplexEntity createTComplexEntity(long id) {
      TEntity e1 = new TEntity("val3", 3, TENANT);
      TEntity e2 = new TEntity("val4", 4, TENANT);
      TEntity e3 = new TEntity("val5", 5, TENANT);
      TEntity e4 = new TEntity("val6", 6, TENANT);
      TEntity e5 = new TEntity("val7", 7, TENANT);

      Set<TEntity> lazyList = new LinkedHashSet<TEntity>();
      lazyList.add(e2);
      lazyList.add(e3);
      Set<TEntity> eagerList = new LinkedHashSet<TEntity>();
      eagerList.add(e4);
      eagerList.add(e5);

      TComplexEntity ce = new TComplexEntity();
      ce.setId(id);
      ce.setCompValue(12);
      ce.setTen(e1);
      ce.setOwner(TENANT);
      ce.setEagerList(eagerList);
      ce.setLazyList(lazyList);
      ce.addLazyList(createTEntity(6, "Hase6"));

      return ce;
   }

   protected TEntity createTEntity(int counter, String name) {
      TEntity te = new TEntity();
      te.setCounter(counter);
      te.setNameValue(name);
      te.setOwner(name);
      return te;
   }

   @Test
   public void getResultList() {
      List<String> list = new ArrayList<String>();
      list.add("one");
      list.add("two");
      list.add("three");
      Mockito.when(query.getResultList()).thenReturn(list);

      CibetTypedQuery<String> cq = new CibetTypedQuery<String>(query, "", em, QueryType.NAMED_TYPED_QUERY,
            String.class);
      List<String> result = cq.getResultList();
      Assert.assertNotNull(result);
      Assert.assertEquals(3, result.size());
      Assert.assertEquals("one", result.get(0));
      Assert.assertEquals("two", result.get(1));
      Assert.assertEquals("three", result.get(2));
   }

   @Test
   public void getSingleResult() {
      Context.internalRequestScope().setEntityManager(em);

      List<String> list = new ArrayList<String>();
      list.add("one");
      list.add("two");
      list.add("three");
      Mockito.when(query2.getSingleResult()).thenReturn(list);

      CibetTypedQuery<List> cq = new CibetTypedQuery<List>(query2, "", em, QueryType.NATIVE_MAPPED_QUERY, List.class);
      List<String> result = (List<String>) cq.getSingleResult();
      Assert.assertNotNull(result);
      Assert.assertEquals(3, result.size());
      Assert.assertEquals("one", result.get(0));
      Assert.assertEquals("two", result.get(1));
      Assert.assertEquals("three", result.get(2));
   }

   @Test
   public void getResultListTComplexEntity() {
      List<TComplexEntity> list = new ArrayList<TComplexEntity>();
      list.add(createTComplexEntity(45));
      list.add(createTComplexEntity(46));
      list.add(createTComplexEntity(47));
      Mockito.when(query3.getResultList()).thenReturn(list);

      CibetTypedQuery<TComplexEntity> cq = new CibetTypedQuery<TComplexEntity>(query3, "", em, QueryType.NATIVE_QUERY,
            TComplexEntity.class);
      List<TComplexEntity> result = cq.getResultList();
      Assert.assertNotNull(result);
      Assert.assertEquals(3, result.size());
      Assert.assertEquals(45, result.get(0).getId());
      Assert.assertEquals(46, result.get(1).getId());
      Assert.assertEquals(47, result.get(2).getId());
      // Mockito.verify(em).detach(result.get(0));
   }

   @Test
   public void getResultListTComplexEntityNull() {
      List<Object> list = new ArrayList<Object>();
      list.add(createTComplexEntity(45));
      list.add(createTComplexEntity(46));
      list.add(createTComplexEntity(47));
      Mockito.when(query4.getResultList()).thenReturn(list);

      CibetTypedQuery<Object> cq = new CibetTypedQuery<Object>(query4, "", em, QueryType.NAMED_QUERY, Object.class);
      List<Object> result = cq.getResultList();
      Assert.assertNotNull(result);
      Assert.assertEquals(3, result.size());
      Assert.assertEquals(45, ((TComplexEntity) result.get(0)).getId());
      Assert.assertEquals(46, ((TComplexEntity) result.get(1)).getId());
      Assert.assertEquals(47, ((TComplexEntity) result.get(2)).getId());
      // Mockito.verify(em).detach(result.get(0));
   }

}
