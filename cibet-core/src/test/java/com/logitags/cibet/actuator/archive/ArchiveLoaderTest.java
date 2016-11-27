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
package com.logitags.cibet.actuator.archive;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.cibethelper.entities.Syntetic1Entity;
import com.cibethelper.entities.Syntetic2Entity;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.diff.Difference;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.sensor.http.HttpRequestResourceHandler;
import com.logitags.cibet.sensor.jpa.JpaResourceHandler;

import junit.framework.Assert;

public class ArchiveLoaderTest {

   private static Logger log = Logger.getLogger(ArchiveLoaderTest.class);

   @Test
   public void initializeAllIdValues() throws Exception {
      log.info("start initializeAllIdValues");
      Syntetic1Entity syn1 = new Syntetic1Entity();
      TEntity t1 = new TEntity();
      t1.setCounter(5);
      t1.setId(5);
      TEntity t2 = new TEntity();
      t2.setCounter(6);
      t2.setId(6);
      TEntity t3 = new TEntity();
      t3.setCounter(7);
      t3.setId(7);
      TEntity t4 = new TEntity();
      t4.setCounter(8);
      t4.setId(8);

      TEntity[] tArray = { t1, t2 };
      syn1.setEntArray(tArray);
      syn1.setId(27);
      syn1.addMap("index1", t3);
      syn1.addMap("index2", t4);

      Method meth = Archive.class.getDeclaredMethod("initializeAllIdValues", Object.class);
      meth.setAccessible(true);
      meth.invoke(new Archive(), syn1);
      Assert.assertEquals(0, syn1.getId());
      Assert.assertEquals(0, syn1.getEntArray()[0].getId());
      Assert.assertEquals(0, syn1.getEntArray()[1].getId());
      Assert.assertEquals(5, syn1.getEntArray()[0].getCounter());
      Assert.assertEquals(6, syn1.getEntArray()[1].getCounter());
      Assert.assertEquals(0, syn1.getMap().get("index1").getId());
      Assert.assertEquals(0, syn1.getMap().get("index2").getId());
   }

   @Test
   public void initializeAllIdValues2() throws Exception {
      log.info("start initializeAllIdValues2");
      Syntetic2Entity syn1 = new Syntetic2Entity();
      syn1.setId("AAA");

      Method meth = Archive.class.getDeclaredMethod("initializeAllIdValues", Object.class);
      meth.setAccessible(true);
      meth.invoke(new Archive(), syn1);
      Assert.assertEquals(null, syn1.getId());
   }

   @Test
   public void analyzeDifferences() {
      log.info("start analyzeDifferences()");

      List<Archive> list = new ArrayList<Archive>();

      list.add(createArchive("1", 5, "t1", 3, ExecutionStatus.EXECUTED));
      list.add(createArchive("2", 6, "t2", 200, ExecutionStatus.EXECUTED));
      list.add(createArchive("3", 6, "t3", 201, ExecutionStatus.DENIED));
      list.add(createArchive("4", 7, "t4", 200, ExecutionStatus.EXECUTED));

      Resource r1 = new Resource();
      r1.setGroupId("Hase");
      r1.setObject("URL");
      r1.setResourceHandlerClass(HttpRequestResourceHandler.class.getName());

      Archive a1 = new Archive();
      a1.setArchiveId("5");
      a1.setResource(r1);
      a1.setExecutionStatus(ExecutionStatus.EXECUTED);
      setArchiveCreateDate(a1, 5);
      list.add(a1);

      Resource r2 = new Resource();
      r2.setGroupId("Hase");
      r2.setObject("URL2");
      r2.setResourceHandlerClass(HttpRequestResourceHandler.class.getName());

      Archive a2 = new Archive();
      a2.setArchiveId("6");
      a2.setResource(r2);
      a2.setExecutionStatus(ExecutionStatus.DENIED);
      setArchiveCreateDate(a2, 6);
      list.add(a2);

      list.add(createArchive("7", 8, "t5", 220, ExecutionStatus.EXECUTED));

      StringBuffer b = new StringBuffer();
      Map<Archive, List<Difference>> map = ArchiveLoader.analyzeDifferences(list);
      log.debug("map size: " + map.size());
      Iterator<Archive> it = map.keySet().iterator();
      while (it.hasNext()) {
         Archive a = it.next();
         List<Difference> difs = map.get(a);
         b.append(a.getResource().getResourceHandlerClass());
         b.append(" ");
         b.append(a.getExecutionStatus());
         b.append(" ");
         b.append(a.getResource().getObject());
         b.append("\nList size: ");
         b.append(difs.size());
         b.append(": ");
         for (Difference dif : difs) {
            b.append(dif);
            b.append("\n\t");
         }
         b.append("\n\n");
      }
      log.debug(b.toString());

      Assert.assertEquals(7, map.size());
      it = map.keySet().iterator();
      Archive a = it.next();
      Assert.assertEquals(0, map.get(a).size());

      a = it.next();
      Assert.assertEquals(3, map.get(a).size());

      a = it.next();
      Assert.assertEquals(2, map.get(a).size());

      a = it.next();
      Assert.assertEquals(2, map.get(a).size());

      a = it.next();
      Assert.assertEquals(0, map.get(a).size());

      a = it.next();
      Assert.assertEquals(0, map.get(a).size());

      a = it.next();
      Assert.assertEquals(3, map.get(a).size());
   }

   @Test
   public void analyzeDifferences2() {
      log.info("start analyzeDifferences2()");
      List<Archive> list = new ArrayList<Archive>();

      Resource r1 = new Resource();
      r1.setGroupId("Hase");
      r1.setObject("URL");
      r1.setResourceHandlerClass(HttpRequestResourceHandler.class.getName());

      Archive a1 = new Archive();
      a1.setArchiveId("1");
      a1.setResource(r1);
      a1.setExecutionStatus(ExecutionStatus.EXECUTED);
      setArchiveCreateDate(a1, 1);
      list.add(a1);

      Resource r2 = new Resource();
      r2.setGroupId("Hase");
      r2.setObject("URL2");
      r2.setResourceHandlerClass(HttpRequestResourceHandler.class.getName());

      Archive a2 = new Archive();
      a2.setArchiveId("2");
      a2.setResource(r2);
      a2.setExecutionStatus(ExecutionStatus.DENIED);
      setArchiveCreateDate(a2, 2);
      list.add(a2);

      list.add(createArchive("3", 5, "t1", 3, ExecutionStatus.EXECUTED));
      list.add(createArchive("4", 6, "t2", 200, ExecutionStatus.EXECUTED));
      list.add(createArchive("5", 6, "t3", 201, ExecutionStatus.DENIED));
      list.add(createArchive("6", 7, "t4", 200, ExecutionStatus.EXECUTED));

      StringBuffer b = new StringBuffer();
      Map<Archive, List<Difference>> map = ArchiveLoader.analyzeDifferences(list);
      log.debug("map size: " + map.size());
      Iterator<Archive> it = map.keySet().iterator();
      while (it.hasNext()) {
         Archive a = it.next();
         List<Difference> difs = map.get(a);
         b.append(a.getResource().getResourceHandlerClass());
         b.append(" ");
         b.append(a.getExecutionStatus());
         b.append(" ");
         b.append(a.getResource().getObject());
         b.append("\nList size: ");
         b.append(difs.size());
         b.append(": ");
         for (Difference dif : difs) {
            b.append(dif);
            b.append("\n\t");
         }
         b.append("\n\n");
      }
      log.debug(b.toString());

      Assert.assertEquals(6, map.size());
      it = map.keySet().iterator();
      Archive a = it.next();
      Assert.assertEquals(0, map.get(a).size());

      a = it.next();
      Assert.assertEquals(0, map.get(a).size());

      a = it.next();
      Assert.assertEquals(0, map.get(a).size());

      a = it.next();
      Assert.assertEquals(3, map.get(a).size());

      a = it.next();
      Assert.assertEquals(2, map.get(a).size());

      a = it.next();
      Assert.assertEquals(2, map.get(a).size());
   }

   private Archive createArchive(String archiveId, int compValue, String owner, int counter, ExecutionStatus status) {
      TComplexEntity t1 = new TComplexEntity();
      t1.setCompValue(compValue);
      t1.setOwner(owner);
      TEntity e1 = new TEntity();
      e1.setCounter(counter);
      t1.setTen(e1);

      Resource r1 = new Resource();
      r1.setGroupId("Hase");
      r1.setObject(t1);
      r1.setResourceHandlerClass(JpaResourceHandler.class.getName());

      Archive a1 = new Archive();
      a1.setArchiveId(archiveId);
      a1.setResource(r1);
      a1.setExecutionStatus(status);
      setArchiveCreateDate(a1, Integer.valueOf(archiveId));
      return a1;
   }

   private void setArchiveCreateDate(Archive a, int days) {
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.YEAR, -1);
      cal.add(Calendar.DATE, days);
      a.setCreateDate(cal.getTime());
   }

}
