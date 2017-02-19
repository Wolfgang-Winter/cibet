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

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.cibethelper.entities.TEntity;
import com.logitags.cibet.context.Context;

@RunWith(MockitoJUnitRunner.class)
public class CibetEntityManagerTest {

   private static Logger log = Logger.getLogger(CibetEntityManagerTest.class);

   @Mock
   protected EntityManager em;

   private EntityManager cib;

   @Before
   public void initTest() {
      cib = new CibetEntityManager(em, false);
      Context.requestScope().setEntityManager(em);
   }

   @Test
   public void createNamedQuery() {
      Query q = cib.createNamedQuery("xx");
      Assert.assertTrue(q instanceof CibetQuery);
   }

   @Test
   public void createTypedNamedQuery() {
      Query q = cib.createNamedQuery("xx", TEntity.class);
      Assert.assertTrue(q instanceof CibetTypedQuery);
   }

   @Test
   public void createTypedQuery() {
      Query q = cib.createQuery("xx", TEntity.class);
      Assert.assertTrue(q instanceof CibetTypedQuery);
   }

   @Test
   public void createNativeQuery() {
      Query q = cib.createNativeQuery("xx");
      Assert.assertTrue(q instanceof CibetQuery);
   }

   @Test
   public void createNativeQuery2Params() {
      Query q = cib.createNativeQuery("xx", "yy");
      Assert.assertTrue(q instanceof CibetQuery);
   }

   @Test
   public void createNativeQueryClassParam() {
      Query q = cib.createNativeQuery("xx", String.class);
      Assert.assertTrue(q instanceof CibetQuery);
   }

   @Test
   public void createQuery() {
      Query q = cib.createQuery("xx");
      Assert.assertTrue(q instanceof CibetQuery);
   }

   @Test
   public void find() {
      TEntity te = new TEntity("Hansi", 99, "owned by x");
      te.setId(5);
      Mockito.when(em.find(TEntity.class, (Long) 5l)).thenReturn(te);

      TEntity result = cib.find(TEntity.class, 5l);
      Assert.assertNotNull(result);
      Assert.assertEquals(99, result.getCounter());
   }

   @Test
   public void findWithCast() {
      TEntity te = new TEntity("Hansi", 99, "owned by x");
      te.setId(5);
      Mockito.when(em.find(TEntity.class, (Long) 5l)).thenReturn(te);

      TEntity result = cib.find(TEntity.class, 5l);
      Assert.assertNotNull(result);
      Assert.assertEquals(99, result.getCounter());
   }

   @Test
   public void merge() {
      log.info("start merge");
      TEntity te = new TEntity("Hansi", 99, "owned by x");
      te.setId(5);
      Mockito.when(em.merge(te)).thenReturn(te);
      TEntity res = cib.merge(te);
      Assert.assertNotNull(res);
      Assert.assertEquals(te, res);
      log.info("stop merge");
   }

   @Test(expected = IllegalArgumentException.class)
   public void mergeNull() {
      log.info("start merge null");
      TEntity res = cib.merge(null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void persistNull() {
      log.info("start persist null");
      cib.persist(null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void removeNull() {
      log.info("start remove null");
      cib.remove(null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void constructorCibetEntityManager1() {
      new CibetEntityManager(null, null, true);
   }

   @Test(expected = IllegalArgumentException.class)
   public void constructorCibetEntityManager2() {
      new CibetEntityManager(null, em, true);
   }

   @Test
   public void find2() {
      TEntity te = new TEntity("Hansi", 99, "owned by x");
      te.setId(5);
      Map<String, Object> map = new HashMap<String, Object>();

      Mockito.when(em.find(TEntity.class, (Long) 5l, map)).thenReturn(te);

      TEntity result = cib.find(TEntity.class, 5l, map);
      Assert.assertNotNull(result);
      Assert.assertEquals(99, result.getCounter());

   }

   @Test
   public void find3() {
      TEntity te = new TEntity("Hansi", 99, "owned by x");
      te.setId(5);

      Mockito.when(em.find(TEntity.class, (Long) 5l, LockModeType.OPTIMISTIC)).thenReturn(te);

      TEntity result = cib.find(TEntity.class, 5l, LockModeType.OPTIMISTIC);
      Assert.assertNotNull(result);
      Assert.assertEquals(99, result.getCounter());
   }

   @Test
   public void find4() {
      TEntity te = new TEntity("Hansi", 99, "owned by x");
      te.setId(5);
      Map<String, Object> map = new HashMap<String, Object>();

      Mockito.when(em.find(TEntity.class, (Long) 5l, LockModeType.OPTIMISTIC, map)).thenReturn(te);

      TEntity result = cib.find(TEntity.class, 5l, LockModeType.OPTIMISTIC, map);
      Assert.assertNotNull(result);
      Assert.assertEquals(99, result.getCounter());

   }

}
