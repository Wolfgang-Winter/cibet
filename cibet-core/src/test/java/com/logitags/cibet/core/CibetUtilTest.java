package com.logitags.cibet.core;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.cibethelper.entities.Syntetic2Entity;
import com.cibethelper.entities.TCompareEntity;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.diff.Difference;
import com.logitags.cibet.diff.DifferenceType;

import de.danielbechler.diff.ObjectDiffer;
import de.danielbechler.diff.ObjectMerger;

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

public class CibetUtilTest {

   private static Logger log = Logger.getLogger(CibetUtilTest.class);

   @Test
   public void testDecodeByteNull() {
      Object o = CibetUtil.decode(null);
      Assert.assertNull(o);
   }

   @Test
   public void encodeByteNull() throws IOException {
      byte[] bytes = CibetUtil.encode(null);
      Assert.assertNull(bytes);
   }

   @Test
   public void encode2ByteDecodeByteOk() throws IOException {
      TEntity ent = new TEntity();
      ent.setCounter(12);
      ent.setOwner("me");
      byte[] bytes = CibetUtil.encode(ent);
      Assert.assertNotNull(bytes);

      Object o = CibetUtil.decode(bytes);
      Assert.assertTrue(o instanceof TEntity);
      TEntity ent2 = (TEntity) o;
      Assert.assertEquals(12, ent2.getCounter());
      Assert.assertEquals("me", ent2.getOwner());
   }

   @Test
   public void encode2ByteDecodeByteError() throws IOException {
      TEntity ent = new TEntity();
      ent.setCounter(12);
      ent.setOwner("me");
      byte[] bytes = CibetUtil.encode(ent);
      Assert.assertNotNull(bytes);
      bytes[0] = 23;

      try {
         CibetUtil.decode(bytes);
         Assert.fail();
      } catch (RuntimeException e) {
         Assert.assertNotNull(e.getCause());
         Assert.assertTrue(e.getCause() instanceof StreamCorruptedException);
         Assert.assertEquals("invalid stream header: 17ED0005", e.getCause().getMessage());
      }
   }

   @Test
   public void arrayClassForName() {
      Class<?> clazz = CibetUtil.arrayClassForName(null);
      Assert.assertNull(clazz);

      clazz = CibetUtil.arrayClassForName("[Z");
      Assert.assertEquals(boolean.class, clazz);

      clazz = CibetUtil.arrayClassForName("[D");
      Assert.assertEquals(double.class, clazz);

      clazz = CibetUtil.arrayClassForName("[F");
      Assert.assertEquals(float.class, clazz);

      clazz = CibetUtil.arrayClassForName("[S");
      Assert.assertEquals(short.class, clazz);

      clazz = CibetUtil.arrayClassForName("[L" + TEntity.class.getName() + ";");
      Assert.assertEquals(TEntity.class, clazz);

      clazz = CibetUtil.arrayClassForName("[[[J");
      Assert.assertEquals(long.class, clazz);

      try {
         clazz = CibetUtil.arrayClassForName("S");
         Assert.fail();
      } catch (Exception e) {
      }

      try {
         clazz = CibetUtil.arrayClassForName("[Lcom.logitags.cibet.TE");
         Assert.fail();
      } catch (Exception e) {
         Assert.assertTrue(e.getCause().getClass() == ClassNotFoundException.class);
      }
   }

   @Test
   public void loadPropertiesFileNotFound() {
      Properties p = CibetUtil.loadProperties("xxx");
      Assert.assertNull(p);
   }

   @Test(expected = IllegalArgumentException.class)
   public void loadPropertiesNullFile() {
      CibetUtil.loadProperties(null);
   }

   @Test
   public void compare() {
      List<Difference> list = new ArrayList<Difference>();
      TEntity t1 = new TEntity("Stung1", 12, "owner1");
      TEntity t2 = new TEntity("Stung1", 13, "owner1");
      TComplexEntity t3 = new TComplexEntity();
      try {
         CibetUtil.compare(t1, t3);
         Assert.fail();
      } catch (IllegalArgumentException e) {
         // log.error(e.getMessage(), e);
      }

      try {
         CibetUtil.compare(t1, null);
         Assert.fail();
      } catch (IllegalArgumentException e) {
      }

      list = CibetUtil.compare(t2, t1);
      Assert.assertTrue(list.size() == 1);
      Difference difObj = list.get(0);
      log.debug(difObj);
      Assert.assertEquals("counter", difObj.getPropertyName());
      Assert.assertTrue(difObj.getDifferenceType() == DifferenceType.MODIFIED);
      Assert.assertEquals(13, difObj.getNewValue());
      Assert.assertEquals(12, difObj.getOldValue());

      t2 = new TEntity("Stung2", 12, "owner1");
      list = CibetUtil.compare(t2, t1);
      Assert.assertTrue(list.size() == 1);
      difObj = list.get(0);
      log.debug(difObj);
      Assert.assertTrue(difObj.getPropertyName().equals("nameValue"));
      Assert.assertTrue(difObj.getDifferenceType() == DifferenceType.MODIFIED);
      Assert.assertTrue(difObj.getNewValue().equals("Stung2"));
      Assert.assertTrue(difObj.getOldValue().equals("Stung1"));

      t2 = new TEntity(null, 12, "owner1");
      list = CibetUtil.compare(t2, t1);
      Assert.assertTrue(list.size() == 1);
      difObj = list.get(0);
      log.debug(difObj);
      Assert.assertTrue(difObj.getPropertyName().equals("nameValue"));
      Assert.assertTrue(difObj.getDifferenceType() == DifferenceType.REMOVED);
      Assert.assertTrue(difObj.getNewValue() == null);
      Assert.assertTrue(difObj.getOldValue().equals("Stung1"));

      t1 = new TEntity(null, 12, "owner1");
      t2 = new TEntity(null, 12, "owner1");
      list = CibetUtil.compare(t2, t1);
      Assert.assertEquals(0, list.size());

      t1 = new TEntity(null, 12, "owner1");
      t2 = new TEntity("x", 12, "owner1");
      list = CibetUtil.compare(t2, t1);
      Assert.assertTrue(list.size() == 1);
      difObj = list.get(0);
      log.debug(difObj);
      Assert.assertTrue(difObj.getPropertyName().equals("nameValue"));
      Assert.assertTrue(difObj.getDifferenceType() == DifferenceType.ADDED);
      Assert.assertTrue(difObj.getNewValue().equals("x"));
      Assert.assertTrue(difObj.getOldValue() == null);
   }

   @Test
   public void compare2() {
      log.debug("start compare2");
      List<Difference> list = new ArrayList<Difference>();
      TEntity t1 = new TEntity("Stung1", 12, "owner1");
      TEntity t2 = new TEntity("Stung1", 13, "owner1");
      TComplexEntity t3 = new TComplexEntity();
      TComplexEntity t4 = new TComplexEntity();

      list = CibetUtil.compare(t3, t4);
      Assert.assertTrue(list.size() == 0);

      t3.setTen(t1);
      list = CibetUtil.compare(t4, t3);
      Assert.assertEquals(1, list.size());
      Difference difObj = list.get(0);
      log.debug(difObj);
      Assert.assertTrue(difObj.getPropertyName().equals("ten"));
      Assert.assertTrue(difObj.getDifferenceType() == DifferenceType.REMOVED);
      Assert.assertTrue(difObj.getNewValue() == null);
      Assert.assertTrue(difObj.getOldValue().equals(t1));

      t3.setTen(null);
      t4.setTen(t2);
      list = CibetUtil.compare(t4, t3);
      Assert.assertTrue(list.size() == 1);
      difObj = list.get(0);
      log.debug(difObj);
      Assert.assertTrue(difObj.getPropertyName().equals("ten"));
      Assert.assertTrue(difObj.getDifferenceType() == DifferenceType.ADDED);
      Assert.assertTrue(difObj.getNewValue().equals(t2));
      Assert.assertTrue(difObj.getOldValue() == null);

      t3.setTen(t2);
      list = CibetUtil.compare(t3, t4);
      Assert.assertTrue(list.size() == 0);

      t3.setTen(t1);
      list = CibetUtil.compare(t4, t3);
      Assert.assertTrue(list.size() == 1);
      difObj = list.get(0);
      log.debug(difObj);
      Assert.assertTrue(difObj.getPropertyName().equals("counter"));
      Assert.assertTrue(difObj.getDifferenceType() == DifferenceType.MODIFIED);
      Assert.assertTrue(difObj.getNewValue().equals(new Integer(13)));
      Assert.assertTrue(difObj.getOldValue().equals(new Integer(12)));
   }

   @Test
   public void compare3() {
      log.debug("start compare3");
      List<Difference> list = new ArrayList<Difference>();
      TCompareEntity t1 = new TCompareEntity("Stung1", 12, "owner1");
      TCompareEntity t2 = new TCompareEntity("Stung1", 12, "owner1");

      TEntity e1 = new TEntity("Karl", 5, "Putz");
      TEntity e2 = new TEntity("Karl2", 5, "Putz");
      TEntity e3 = new TEntity("Karl3", 5, "Putz");
      TEntity e4 = new TEntity("Karl4", 5, "Putz");

      list = CibetUtil.compare(t1, t2);
      Assert.assertTrue(list.size() == 0);

      int[] iarray = new int[] { 1, 2, 3, 4 };
      t1.setIntArray(iarray);
      t2.setIntArray(null);
      list = CibetUtil.compare(t1, t2);
      Assert.assertEquals(1, list.size());
      Difference difObj = list.get(0);
      log.debug(difObj);
      Assert.assertTrue(difObj.getPropertyPath().equals("/intArray"));
      Assert.assertTrue(difObj.getDifferenceType() == DifferenceType.ADDED);
      Assert.assertTrue(difObj.getOldValue() == null);
      int[] iarray2;// = (int[]) difObj.getOldValue();

      list = CibetUtil.compare(t2, t1);
      Assert.assertEquals(1, list.size());
      difObj = list.get(0);
      log.debug(difObj);
      Assert.assertTrue(difObj.getPropertyPath().equals("/intArray"));
      Assert.assertTrue(difObj.getDifferenceType() == DifferenceType.REMOVED);
      Assert.assertTrue(difObj.getNewValue() == null);

      t2.setIntArray(new int[] { 1, 2, 3 });
      list = CibetUtil.compare(t1, t2);
      Assert.assertTrue(list.size() == 1);
      difObj = list.get(0);
      log.debug(difObj);
      Assert.assertTrue(difObj.getPropertyName().equals("intArray"));
      Assert.assertTrue(difObj.getDifferenceType() == DifferenceType.MODIFIED);
      Assert.assertTrue(Arrays.equals((int[]) difObj.getNewValue(), new int[] { 1, 2, 3, 4 }));
      Assert.assertTrue(Arrays.equals((int[]) difObj.getOldValue(), new int[] { 1, 2, 3 }));

      t2.setIntArray(new int[] { 1, 2, 3, 4 });
      list = CibetUtil.compare(t1, t2);
      Assert.assertTrue(list.size() == 0);

      t2.setIntArray(new int[] { 1, 2, 3, 4, 5 });
      list = CibetUtil.compare(t1, t2);
      Assert.assertTrue(list.size() == 1);
      difObj = list.get(0);
      log.debug(difObj);
      Assert.assertTrue(difObj.getPropertyName().equals("intArray"));
      Assert.assertTrue(difObj.getDifferenceType() == DifferenceType.MODIFIED);

      t2.setIntArray(new int[] { 5, 2, 3, 4 });
      list = CibetUtil.compare(t1, t2);
      Assert.assertEquals(1, list.size());
      difObj = list.get(0);
      log.debug(difObj);
      Assert.assertTrue(difObj.getPropertyName().equals("intArray"));
      Assert.assertTrue(difObj.getDifferenceType() == DifferenceType.MODIFIED);

      t2.setIntArray(new int[] { 1, 2, 3, 4 });
      t1.setEntList(Arrays.asList(e1, e2, e3));
      t2.setEntList(Arrays.asList(e1, e2, e3, e4));
      list = CibetUtil.compare(t1, t2);
      Assert.assertTrue(list.size() == 1);
      difObj = list.get(0);
      log.debug(difObj);
      Assert.assertTrue(difObj.getPropertyName().equals("entList"));
      Assert.assertTrue(difObj.getDifferenceType() == DifferenceType.REMOVED);
      Assert.assertEquals(e4, difObj.getOldValue());
      Assert.assertTrue(difObj.getNewValue() == null);

      t1.setEntList(Arrays.asList(e1, e2, e3, e4));
      t2.setEntList(Arrays.asList(e1, e2, e3));
      list = CibetUtil.compare(t1, t2);
      Assert.assertTrue(list.size() == 1);
      difObj = list.get(0);
      log.debug(difObj);
      Assert.assertTrue(difObj.getPropertyName().equals("entList"));
      Assert.assertTrue(difObj.getDifferenceType() == DifferenceType.ADDED);
      Assert.assertEquals(e4, difObj.getNewValue());
      Assert.assertTrue(difObj.getOldValue() == null);

      t1.setEntList(Arrays.asList(e1, e4, e3));
      t2.setEntList(Arrays.asList(e1, e2, e3));
      list = CibetUtil.compare(t1, t2);
      Assert.assertTrue(list.size() == 2);
      difObj = list.get(0);
      log.debug(difObj);
      Assert.assertTrue(difObj.getPropertyName().equals("entList"));
      Assert.assertTrue(difObj.getDifferenceType() == DifferenceType.ADDED);
      Assert.assertTrue(difObj.getNewValue().equals(e4));
      Assert.assertTrue(difObj.getOldValue() == null);
      difObj = list.get(1);
      log.debug(difObj);
      Assert.assertTrue(difObj.getPropertyName().equals("entList"));
      Assert.assertTrue(difObj.getDifferenceType() == DifferenceType.REMOVED);
      Assert.assertTrue(difObj.getOldValue().equals(e2));
      Assert.assertTrue(difObj.getNewValue() == null);

      t1.setEntList(Arrays.asList(e1, e2, e3, e4));
      t2.setEntList(Arrays.asList(e1, e4, e3, e2));
      list = CibetUtil.compare(t1, t2);
      Assert.assertTrue(list.size() == 0);
   }

   @Test
	public void compare3a() {
		log.debug("start compare3a");
		List<Difference> list = new ArrayList<Difference>();
		TCompareEntity t1 = new TCompareEntity("Stung1", 12, "owner1");
		TCompareEntity t2 = new TCompareEntity("Stung1", 12, "owner1");

		TEntity e1 = new TEntity("Karl", 5, "Putz");
		TEntity e2 = new TEntity("Karl2", 5, "Putz");
		TEntity e3 = new TEntity("Karl3", 5, "Putz");
		TEntity e4 = new TEntity("Karl4", 5, "Putz");

		TEntity e11 = new TEntity("Karl", 5, "Putz");
		TEntity e22 = new TEntity("Karl2", 5, "Putz");
		TEntity e33 = new TEntity("Karl3", 5, "Putz");
		TEntity e44 = new TEntity("Karl4", 5, "Putz");

		t1.setEntList(Arrays.asList(e1, e2, e3, e4));
		t2.setEntList(Arrays.asList(e22, e33, e44, e11));
		list = CibetUtil.compare(t1, t2);
		Assert.assertTrue(list.size() == 0);
	}

	@Test
   public void compare4() {
      List<Difference> list = new ArrayList<Difference>();
      TCompareEntity t1 = new TCompareEntity("Stung1", 12, "owner1");
      TCompareEntity t2 = new TCompareEntity("Stung1", 12, "owner1");

      TEntity e1 = new TEntity("Karl", 5, "Putz");
      TEntity e2 = new TEntity("Karl2", 5, "Putz");

      t1.setTransi(5);
      t2.setTransi(6);
      list = CibetUtil.compare(t1, t2);
      Assert.assertTrue(list.size() == 0);

      t1.setSuperEnt(e1);
      t2.setSuperEnt(e2);
      t1.setCounter(6);
      t2.setCounter(7);

      list = CibetUtil.compare(t1, t2);
      Assert.assertTrue(list.size() == 2);
      Difference difObj = list.get(0);
      Assert.assertTrue(difObj.getPropertyName().equals("counter"));
      Assert.assertTrue(difObj.getDifferenceType() == DifferenceType.MODIFIED);
      Assert.assertEquals(7, difObj.getOldValue());
      Assert.assertEquals(6, difObj.getNewValue());
      difObj = list.get(1);
      Assert.assertTrue(difObj.getPropertyPath().equals("/superEnt/nameValue"));
      Assert.assertTrue(difObj.getDifferenceType() == DifferenceType.MODIFIED);
      Assert.assertTrue(difObj.getPropertyName().equals("nameValue"));
      Assert.assertTrue(difObj.getOldValue().equals("Karl2"));
      Assert.assertTrue(difObj.getNewValue().equals("Karl"));
   }

   @Test
   public void compareMap() {
      List<Difference> list = new ArrayList<Difference>();
      TCompareEntity t1 = new TCompareEntity("Stung1", 12, "owner1");
      TCompareEntity t2 = new TCompareEntity("Stung1", 12, "owner1");

      TEntity e1 = new TEntity("Karl", 5, "Putz");
      TEntity e2 = new TEntity("Karl2", 5, "Putz");
      TEntity e3 = new TEntity("Karl3", 5, "Putz");
      TEntity e4 = new TEntity("Karl4", 5, "Putz");

      Map<String, TEntity> m1 = new HashMap<String, TEntity>();
      Map<String, TEntity> m2 = new HashMap<String, TEntity>();
      m1.put("e1", e1);
      m1.put("e2", e2);
      m1.put("e4", e4);
      m1.put("e3", e3);
      m2.put("e1", e1);
      m2.put("e2", e2);
      m2.put("e3", e3);
      m2.put("e4", e4);
      t1.setMap(m1);
      t2.setMap(m2);

      list = CibetUtil.compare(t1, t2);
      Assert.assertTrue(list.size() == 0);

      m1.remove("e2");
      list = CibetUtil.compare(t1, t2);
      Assert.assertTrue(list.size() == 1);
      Difference difObj = list.get(0);
      Assert.assertTrue(difObj.getPropertyName().equals("map"));
      Assert.assertTrue(difObj.getDifferenceType() == DifferenceType.REMOVED);
      Assert.assertTrue(difObj.getNewValue() == null);
      Assert.assertTrue(difObj.getOldValue().equals(e2));

      m1.put("e2", e2);
      m2.remove("e2");
      list = CibetUtil.compare(t1, t2);
      Assert.assertTrue(list.size() == 1);
      difObj = list.get(0);
      Assert.assertTrue(difObj.getPropertyName().equals("map"));
      Assert.assertTrue(difObj.getDifferenceType() == DifferenceType.ADDED);
      Assert.assertTrue(difObj.getOldValue() == null);
      Assert.assertTrue(difObj.getNewValue().equals(e2));

      m2.put("e2", e3);
      list = CibetUtil.compare(t1, t2);
      Assert.assertTrue(list.size() == 1);
      difObj = list.get(0);
      log.debug(difObj);
      Assert.assertTrue(difObj.getPropertyName().equals("nameValue"));
      Assert.assertTrue(difObj.getDifferenceType() == DifferenceType.MODIFIED);
      Assert.assertTrue(difObj.getOldValue().equals("Karl3"));
      Assert.assertTrue(difObj.getNewValue().equals("Karl2"));
      if (1 == 1) return;
   }

   @Test
   public void compareIntList() {
      List<Difference> list = new ArrayList<Difference>();
      Syntetic2Entity t1 = new Syntetic2Entity();
      Syntetic2Entity t2 = new Syntetic2Entity();

      List l1 = new ArrayList();
      l1.add(2);
      l1.add(4);
      l1.add(8);
      List l2 = new ArrayList();
      l2.add(2);
      l2.add(4);
      l2.add(7);

      t1.setIntList(l1);
      t2.setIntList(l2);

      list = CibetUtil.compare(t1, t2);
      Assert.assertTrue(list.size() == 2);
      Difference difObj = list.get(0);
      Assert.assertTrue(difObj.getPropertyName().equals("intList"));
      Assert.assertTrue(difObj.getDifferenceType() == DifferenceType.ADDED);
      difObj = list.get(1);
      Assert.assertTrue(difObj.getPropertyName().equals("intList"));
      Assert.assertTrue(difObj.getDifferenceType() == DifferenceType.REMOVED);
   }

   @Test
   public void merge1() {
      log.info("start merge1()");
      List<Difference> list = new ArrayList<Difference>();
      ObjectDiffer differ = CibetUtil.getObjectDiffer();
      ObjectMerger merger = new ObjectMerger(differ);

      TEntity t1 = new TEntity("Stung1", 12, "owner1");
      TEntity t2 = new TEntity("Stung1", 13, "owner1");
      TEntity t3 = new TEntity("Stung1", 17, "Willi");

      TComplexEntity base = new TComplexEntity();
      base.setId(20);
      base.setCompValue(45);
      base.setVersion(100);
      base.setTen(t1);

      TComplexEntity work = new TComplexEntity();
      work.setId(20);
      work.setCompValue(25);
      work.setTen(t2);

      TComplexEntity head = new TComplexEntity();
      head.setId(20);
      head.setCompValue(45);
      head.setVersion(50);
      head.setTen(t3);

      list = CibetUtil.compare(work, base);
      Assert.assertEquals(2, list.size());

      TComplexEntity merged = merger.merge(work, base, head);
      log.debug(merged);
      Assert.assertEquals(50, merged.getVersion());
      Assert.assertEquals(25, merged.getCompValue());
      Assert.assertEquals(20, merged.getId());
      Assert.assertEquals(13, merged.getTen().getCounter());
      Assert.assertEquals("Willi", merged.getTen().getOwner());
   }

   @Test
   public void merge2() {
      log.info("start merge2()");
      List<Difference> list = new ArrayList<Difference>();
      ObjectDiffer differ = CibetUtil.getObjectDiffer();
      ObjectMerger merger = new ObjectMerger(differ);

      TEntity t1 = new TEntity("Stung1", 12, "owner1");
      TEntity t2 = new TEntity("Stung1", 13, "owner1");
      TEntity t3 = new TEntity("Stung1", 17, "Willi");

      TComplexEntity base = new TComplexEntity();
      base.setId(20);
      base.setCompValue(45);
      base.setVersion(100);
      base.setTen(t1);
      base.getEagerList().add(t1);
      base.getEagerList().add(t2);

      TComplexEntity work = new TComplexEntity();
      work.setId(20);
      work.setCompValue(25);
      work.setTen(t2);

      TComplexEntity head = new TComplexEntity();
      head.setId(20);
      head.setCompValue(45);
      head.setTen(t3);
      head.getEagerList().add(t1);
      head.getEagerList().add(t2);

      list = CibetUtil.compare(work, base);
      Assert.assertEquals(4, list.size());

      TComplexEntity merged = merger.merge(work, base, head);
      log.debug(merged);
      Assert.assertEquals(0, merged.getVersion());
      Assert.assertEquals(25, merged.getCompValue());
      Assert.assertEquals(20, merged.getId());
      Assert.assertEquals(13, merged.getTen().getCounter());
      Assert.assertEquals("Willi", merged.getTen().getOwner());
      Assert.assertEquals(0, merged.getEagerList().size());
   }

   @Test
   public void merge3() {
      log.info("start merge3()");
      List<Difference> list = new ArrayList<Difference>();
      ObjectDiffer differ = CibetUtil.getObjectDiffer();
      ObjectMerger merger = new ObjectMerger(differ);

      TEntity t1 = new TEntity("Stung1", 12, "owner1");
      TEntity t2 = new TEntity("Stung1", 13, "owner1");
      TEntity t3 = new TEntity("Stung1", 17, "Willi");

      TComplexEntity base = new TComplexEntity();
      base.setId(20);
      base.setCompValue(45);
      base.setVersion(100);
      base.setTen(t1);
      base.getEagerList().add(t1);
      base.getEagerList().add(t2);

      TComplexEntity work = new TComplexEntity();
      work.setId(20);
      work.setCompValue(25);
      work.setTen(t2);
      work.getEagerList().add(t1);

      TComplexEntity head = new TComplexEntity();
      head.setId(20);
      head.setCompValue(45);
      head.setTen(t3);
      // if head list is NULL it is not instantiated
      head.setEagerList(null);

      list = CibetUtil.compare(work, base);
      Assert.assertEquals(3, list.size());

      log.debug("now merge");
      TComplexEntity merged = merger.merge(work, base, head);
      log.debug(merged);
      Assert.assertEquals(0, merged.getVersion());
      Assert.assertEquals(25, merged.getCompValue());
      Assert.assertEquals(20, merged.getId());
      Assert.assertEquals(13, merged.getTen().getCounter());
      Assert.assertEquals("Willi", merged.getTen().getOwner());
      Assert.assertNull(merged.getEagerList());
   }

   @Test
   public void merge4() {
      log.info("start merge4()");
      List<Difference> list = new ArrayList<Difference>();
      ObjectDiffer differ = CibetUtil.getObjectDiffer();
      ObjectMerger merger = new ObjectMerger(differ);

      TEntity t1 = new TEntity("Stung1", 12, "owner1");
      TEntity t2 = new TEntity("Stung1", 13, "owner1");
      TEntity t3 = new TEntity("Stung1", 17, "Willi");

      TComplexEntity base = new TComplexEntity();
      base.setId(20);
      base.setCompValue(45);
      base.setVersion(100);
      base.setTen(t1);
      base.getEagerList().add(t1);
      base.getEagerList().add(t2);

      TComplexEntity work = new TComplexEntity();
      work.setId(20);
      work.setCompValue(45);
      work.setTen(t1);
      work.getEagerList().add(t1);

      TComplexEntity head = new TComplexEntity();
      head.setId(20);
      head.setCompValue(35);
      head.setTen(t3);
      head.getEagerList().add(t3);
      head.getEagerList().add(t2);

      list = CibetUtil.compare(work, base);
      Assert.assertEquals(1, list.size());

      log.debug("now merge");
      TComplexEntity merged = merger.merge(work, base, head);
      log.debug(merged);
      Assert.assertEquals(0, merged.getVersion());
      Assert.assertEquals(35, merged.getCompValue());
      Assert.assertEquals(20, merged.getId());
      Assert.assertEquals(17, merged.getTen().getCounter());
      Assert.assertEquals("Willi", merged.getTen().getOwner());
      Assert.assertEquals(1, merged.getEagerList().size());
      TEntity ent = merged.getEagerList().iterator().next();
      Assert.assertEquals("Willi", ent.getOwner());
      Assert.assertEquals(17, ent.getCounter());
   }

   @Test
   public void merge4a() {
      log.info("start merge4a()");
      List<Difference> list = new ArrayList<Difference>();
      ObjectDiffer differ = CibetUtil.getObjectDiffer();
      ObjectMerger merger = new ObjectMerger(differ);

      TEntity t1 = new TEntity("Stung1", 12, "owner1");
      TEntity t2 = new TEntity("Stung2", 13, "owner2");
      TEntity t3 = new TEntity("Stung3", 17, "Willi");

      TComplexEntity base = new TComplexEntity();
      base.getEagerList().add(t1);

      TComplexEntity work = new TComplexEntity();
      work.getEagerList().add(t2);

      TComplexEntity head = new TComplexEntity();
      head.getEagerList().add(t1);
      head.getEagerList().add(t3);
      head.getEagerList().add(t2);

      list = CibetUtil.compare(work, base);
      Assert.assertEquals(2, list.size());

      log.debug("now merge");
      TComplexEntity merged = merger.merge(work, base, head);
      log.debug(merged);
      Assert.assertEquals(2, merged.getEagerList().size());
      Iterator<TEntity> it = merged.getEagerList().iterator();
      TEntity ent = it.next();
      Assert.assertEquals("Willi", ent.getOwner());
      Assert.assertEquals(17, ent.getCounter());
      ent = it.next();
      Assert.assertEquals("owner2", ent.getOwner());
      Assert.assertEquals(13, ent.getCounter());
   }

   @Test
   public void merge5() {
      log.info("start merge5()");
      ObjectDiffer differ = CibetUtil.getObjectDiffer();
      ObjectMerger merger = new ObjectMerger(differ);

      TCompareEntity w = new TCompareEntity("Stung1", 12, "owner1");
      TCompareEntity b = new TCompareEntity("Stung1", 12, "owner1");
      TCompareEntity h = new TCompareEntity("Stung1", 12, "owner1");

      int[] iarray = new int[] { 1, 2, 3, 4 };
      int[] iarray2 = new int[] { 1, 2, 3, 4, 88 };
      b.setIntArray(iarray);
      w.setIntArray(null);
      TCompareEntity r = merger.merge(w, b, h);
      log.debug(r);
      Assert.assertNull(r.getIntArray());

      w.setIntArray(iarray2);
      r = merger.merge(w, b, h);
      log.debug(r);
      Assert.assertTrue(Arrays.equals(h.getIntArray(), iarray2));
   }

}
