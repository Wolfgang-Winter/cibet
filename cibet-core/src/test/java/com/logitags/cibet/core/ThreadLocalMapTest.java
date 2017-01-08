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

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import com.logitags.cibet.context.ThreadLocalMap;

public class ThreadLocalMapTest {

   @Test
   public void testChildValueNull() {
      ThreadLocalMap map = new ThreadLocalMap();
      HashMap<String, Object> ht3 = map.childValue(null);
      Assert.assertNull(ht3);
   }

   @Test
   public void testChildValueWithValue() {
      ThreadLocalMap map = new ThreadLocalMap();
      HashMap<String, Object> ht2 = new HashMap<String, Object>();
      ht2.put("key1", "value1");
      HashMap<String, Object> ht3 = map.childValue(ht2);
      Assert.assertNotSame(ht2, ht3);
      Assert.assertEquals(1, ht3.size());
      Assert.assertTrue(ht3.containsKey("key1"));
      Assert.assertTrue(ht3.containsValue("value1"));
   }

}
