/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2014 Dr. Wolfgang Winter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************
 */
package com.logitags.jmeter;

import static java.lang.Math.atan;
import static java.lang.Math.tan;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class MonitorTestClass {

   private static Logger log = Logger.getLogger(MonitorTestClass.class);

   private static AtomicLong atomic = new AtomicLong(0);
   private static Map<String, Long> syncMap = Collections.synchronizedMap(new HashMap<>());
   public static String FILENAME = System.getProperty("java.io.tmpdir") + "ioSync.log";
   private volatile static double randomDouble = 0.5;

   private static AtomicLong sleepStart = new AtomicLong(0);
   private static AtomicLong requestCount = new AtomicLong(0);

   private EntityManagerFactory fac = null;

   public MonitorTestClass() {
      syncMap.put("value", 1L);
   }

   public static synchronized void syncCalc(int count) {
      log.info("start syncCalc");
      MonitorTestClass tclass = new MonitorTestClass();
      tclass.calc(count, null);
   }

   public static synchronized void syncIo(int count) {
      log.info("start syncIo");
      MonitorTestClass tclass = new MonitorTestClass();
      tclass.io(count, null);
   }

   public static synchronized void syncCibetCalc(int count) {
      log.info("start syncCibetCalc");
      MonitorTestClass tclass = new MonitorTestClass();
      tclass.cibetCalc(count, null);
   }

   public static synchronized void syncCibetIo(int count) {
      log.info("start syncCibetIo");
      MonitorTestClass tclass = new MonitorTestClass();
      tclass.cibetIo(count, null);
   }

   public String calc(int count, String param2) {
      StringBuffer b = new StringBuffer();
      log.info("start calc() with " + count);
      Random random = new Random(new Date().getTime());

      for (int i = 0; i < count; i++) {
         double d = random.nextDouble();
         tan(atan(tan(atan(tan(atan(tan(atan(tan(atan(d))))))))));
      }

      // this will hang:
      // Pattern pattern = Pattern.compile("(\\s*\\w*\\.*)*;");
      // Matcher m = pattern.matcher("bla.foo.bloo.somestatic.blaaaaaat.blooo.foo.*;");
      // log.info("Matcher matches: " + m.matches());

      b.append("end calc()");
      log.info(b);
      return b.toString();
   }

   public String calc2(int count, String param2) {
      long sleepy = Long.valueOf(param2);

      StringBuffer b = new StringBuffer();
      log.info("start calc2() with " + count);
      Random random = new Random(new Date().getTime());

      int quot = 350;
      long currentCount = requestCount.incrementAndGet();
      // log.info("loop " + 1 + ", sleepcount=" + sleepcount);
      if (currentCount % quot == 0) {
         // log.info("set sleepcount to 500");
         sleepStart.set(System.currentTimeMillis());
         quot = quot + 50;
      }

      if (sleepStart.get() + 8000 > System.currentTimeMillis()) {
         // log.info("sleep now for " + sleepy);
         try {
            Thread.sleep(sleepy);
         } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
         }
      }

      for (int i = 1; i < count; i++) {
         double d = random.nextDouble();
         tan(atan(tan(atan(tan(atan(tan(atan(tan(atan(d))))))))));
      }

      b.append("end calc2()");
      log.info(b);
      return b.toString();
   }

   public String calcSync(int count, String param2) {
      StringBuffer b = new StringBuffer();
      log.info("start calcSync() [all-thread variable] with " + count);

      for (int i = 0; i < count; i++) {
         // long lo = atomic.getAndIncrement();
         long lo = syncMap.get("value");
         syncMap.put("value", lo++);
         // log.info("all-thread variable: " + lo);
      }

      b.append("end calcSync() [all-thread variable]");
      log.info(b);
      return b.toString();
   }

   public String calcSync2(int count, String param2) {
      StringBuffer b = new StringBuffer();
      Random random = new Random(new Date().getTime());
      double factor = Double.parseDouble(param2);
      double d = random.nextDouble();
      if (d < 0.3) {
         randomDouble = random.nextDouble();
      }
      if (randomDouble < factor) {
         return calcSync(count, param2);
      } else {
         log.info("start calcSync2() [local variable] with " + count);
         // calc(count * 100);
         long lo = 0;
         for (int i = 0; i < count; i++) {
            lo++;
            // log.info("local variable: " + lo);
         }
         b.append("end calcSync2() [local variable]");
         log.info(b);
         return b.toString();
      }
   }

   public String io(int count, String param2) {
      if (count > 10000) {
         throw new IllegalArgumentException("loop counter too big: " + count);
      }

      String str = "start io() with " + count;
      log.info(str);

      String uuid = UUID.randomUUID().toString();
      uuid = uuid.replaceAll("-", "");
      for (int i = 0; i < count; i++) {
         FileWriter writer = null;
         File file = new File(System.getProperty("java.io.tmpdir") + File.separator + uuid + i + ".log");
         try {
            writer = new FileWriter(file);
            IOUtils.write(str, writer);

         } catch (IOException e) {
            log.error(e.getMessage(), e);
         } finally {
            IOUtils.closeQuietly(writer);
            file.delete();
         }
      }

      log.info("end io()");
      return str;
   }

   public String ioSync(int count, String param2) {
      if (count > 10000) {
         throw new IllegalArgumentException("loop counter too big: " + count);
      }

      String str = "start ioSync() [all-thread file] with " + count + " writing file " + FILENAME;
      log.info(str);
      // File file = new File(FILENAME);
      for (int i = 0; i < count; i++) {
         log.info(i);
         // FileWriter writer = null;
         // FileReader reader = null;
         // try {
         // reader = new FileReader(file);
         // String inString = IOUtils.toString(reader);
         // IOUtils.closeQuietly(reader);
         //
         // writer = new FileWriter(file);
         // IOUtils.write(inString + "a", writer);
         //
         // } catch (IOException e) {
         // log.error(e.getMessage(), e);
         // } finally {
         // IOUtils.closeQuietly(reader);
         // IOUtils.closeQuietly(writer);
         // }
      }

      log.info("end ioSync() [all-thread file]");
      return str;
   }

   public String ioSync2(int count, String param2) {
      double factor = Double.parseDouble(param2);
      Random random = new Random(new Date().getTime());
      double d = random.nextDouble();
      if (d < 0.3) {
         randomDouble = random.nextDouble();
      }
      if (randomDouble < factor) {
         return ioSync(count, param2);
      } else {

         String filename = System.getProperty("java.io.tmpdir") + File.separator + UUID.randomUUID().toString()
               + ".log";
         String str = "start ioSync2() [local file] with " + count + " writing file " + filename;
         log.info(str);
         File file = new File(filename);
         if (!file.exists()) {
            try {
               file.createNewFile();
            } catch (IOException e) {
               log.error(e.getMessage(), e);
            }
         }
         for (int i = 0; i < count; i++) {
            FileWriter writer = null;
            FileReader reader = null;
            try {
               reader = new FileReader(file);
               writer = new FileWriter(file);
               String inString = "";
               List<String> list = IOUtils.readLines(reader);
               if (list.size() > 0) {
                  inString = list.get(0);
               }
               IOUtils.write(inString + "a", writer);

            } catch (IOException e) {
               log.error(e.getMessage(), e);
            } finally {
               IOUtils.closeQuietly(writer);
               IOUtils.closeQuietly(reader);
            }
         }

         file.delete();
         log.info("end ioSync2() [local file]");
         return str;
      }
   }

   public String ioLog(int count, String param2) {
      long sleepy = Long.valueOf(param2);
      String str = "start ioLog() [all-thread file] with " + count;
      log.info(str);
      for (int i = 0; i < count; i++) {
         log.info(i);
         try {
            Thread.sleep(sleepy);
         } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
         }
      }

      log.info("end ioLog() [all-thread file]");
      return str;
   }

   public String ioLogLocal(int count, String param2) {
      long sleepy = Long.valueOf(param2);
      String filename = System.getProperty("java.io.tmpdir") + File.separator + UUID.randomUUID().toString() + ".log";
      String str = "start ioLogLocal() [local file] with " + count + " writing file " + filename;
      log.info(str);
      File file = new File(filename);

      for (int i = 0; i < count; i++) {
         FileWriter writer = null;
         // FileReader reader = null;
         try {
            writer = new FileWriter(file);
            IOUtils.write(String.valueOf(i), writer);
            IOUtils.closeQuietly(writer);

            // reader = new FileReader(file);
            // IOUtils.toString(reader);
            // IOUtils.closeQuietly(reader);

            try {
               Thread.sleep(sleepy);
            } catch (InterruptedException e) {
               log.error(e.getMessage(), e);
            }

         } catch (IOException e) {
            log.error(e.getMessage(), e);
         } finally {
         }
      }

      try {
         Thread.sleep(5);
      } catch (InterruptedException e) {
         log.error(e.getMessage(), e);
      }
      file.delete();

      log.info("end ioSync2() [local file]");
      return str;

   }

   public String mem(int count, String param2) {
      log.debug("start mem()");
      List<String> list = new ArrayList<>();
      Random rnd = new Random();

      for (int i = 0; i < count; i++) {
         String str = String.valueOf(rnd.nextDouble());
         list.add(str);
      }
      log.debug("end domem()");
      return "";
   }

   public String mem2(int count, String param2) {
      log.debug("start mem2()");
      // big string so that it does not take to long
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < 50; i++) {
         sb.append("all work and no play... makes this a long string\n");
      }

      long t0 = 0;
      // I don't want the list resizing to interfere so initialize a big capacity
      // int capacity = 1000000;
      List<String> strings = new ArrayList<String>(count);
      for (int i = 0; i < count; i++) {
         // increase memory usage
         strings.add("randomString:" + Math.random() + sb);
         // and also make sure that we generate objects that need to be garbage collected to see the difference between
         // the 2 thresholds
         for (int j = 0; j < 10; j++) {
            strings.set((int) Math.floor(Math.random() * strings.size()), "randomString:" + Math.random() + sb);
         }
      }
      log.debug("end domem()");
      return "";
   }

   public String cibetCalc(int count, String param2) {
      return calc(count, param2);
   }

   public String cibetIo(int count, String param2) {
      return io(count, param2);
   }

   public String cibetMem(int count, String param2) {
      return mem(count, param2);
   }

   public String cibetMem2(int count, String param2) {
      return mem2(count, param2);
   }

   public String cibetCalcSync(int count, String param2) {
      return calcSync(count, param2);
   }

   public String cibetIoSync(int count, String param2) {
      return ioSync(count, param2);
   }

   public String cibetSelect(int count, String param2) {
      if (fac == null) {
         fac = Persistence.createEntityManagerFactory("CibetLocal-Derby");
      }

      EntityManager em = fac.createEntityManager();
      em.getTransaction().begin();

      Random rnd = new Random(new Date().getTime());
      int i1 = rnd.nextInt(999);
      int i2 = rnd.nextInt(999);

      TypedQuery<JMEntity> q = em.createNamedQuery(JMEntity.SEL, JMEntity.class);
      q.setParameter("nameValue", "%" + i1 + "%");
      q.setParameter("owner", "%" + i2 + "%");

      List<JMEntity> list = q.getResultList();
      String str = "";
      for (JMEntity jm : list) {
         str += jm.getOwner().substring(0, 2);
      }

      em.getTransaction().commit();
      em.close();
      return list.size() + " JMEntity objects selected with nameValue like " + i1 + " and owner like " + i2 + ": "
            + str;
   }

}
