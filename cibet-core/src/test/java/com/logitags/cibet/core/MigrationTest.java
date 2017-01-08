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
package com.logitags.cibet.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class MigrationTest {

   private static Logger log = Logger.getLogger(MigrationTest.class);

   private static Date now = new Date(444444);
   private static Calendar calNow = Calendar.getInstance();

   private byte[] encodeVersion1() throws Exception {
      URL url = Thread.currentThread().getContextClassLoader().getResource("migrationtest/migtest1.jar");
      URLClassLoader clsLoader = URLClassLoader.newInstance(new URL[] { url });

      Class<?> cls = clsLoader.loadClass("com.logitags.cibet.migration.Migrant");
      Object obj1 = cls.newInstance();
      Method method = cls.getMethod("setLonger", long.class);
      method.invoke(obj1, 15l);
      Field field = cls.getField("hang");
      field.set(null, "Walheim");
      method = cls.getMethod("setDate", Date.class);
      method.invoke(obj1, now);
      method = cls.getMethod("setIntParam", int.class);
      method.invoke(obj1, 56);
      method = cls.getMethod("setTheId", long.class);
      method.invoke(obj1, 777l);

      Class<?> depcls = clsLoader.loadClass("com.logitags.cibet.migration.DependentBean");
      Object dep = depcls.newInstance();
      method = depcls.getMethod("setTheId", long.class);
      method.invoke(dep, 345l);
      method = depcls.getMethod("setDepString", String.class);
      method.invoke(dep, "Hallo");

      method = cls.getMethod("setDependendBean", depcls);
      method.invoke(obj1, dep);

      log.debug(obj1);
      byte[] bytes = CibetUtil.encode(obj1);
      return bytes;
   }

   private byte[] encodeVersion2() throws Exception {
      URL url = Thread.currentThread().getContextClassLoader().getResource("migrationtest/migtest2.jar");
      URLClassLoader clsLoader = URLClassLoader.newInstance(new URL[] { url });

      Class<?> cls = clsLoader.loadClass("com.logitags.cibet.migration.Migrant");
      Object obj1 = cls.newInstance();
      Method method = cls.getMethod("setLonger", long.class);
      method.invoke(obj1, 15l);
      method = cls.getMethod("setHang", String.class);
      method.invoke(obj1, "K�ln");
      method = cls.getMethod("setDate", Date.class);
      method.invoke(obj1, now);
      method = cls.getMethod("setIntParam", int.class);
      method.invoke(obj1, 56);
      method = cls.getMethod("setTheId", long.class);
      method.invoke(obj1, 777l);
      method = cls.getMethod("setNewProp", short.class);
      method.invoke(obj1, (short) 23);

      Class<?> depcls = clsLoader.loadClass("com.logitags.cibet.migration.DependentBean");
      Object dep = depcls.newInstance();
      method = depcls.getMethod("setTheId", long.class);
      method.invoke(dep, 345l);
      method = depcls.getMethod("setDepString", String.class);
      method.invoke(dep, "Hallo");

      method = cls.getMethod("setDependendBean", depcls);
      method.invoke(obj1, dep);

      log.debug(obj1);
      byte[] bytes = CibetUtil.encode(obj1);
      return bytes;
   }

   private byte[] encodeVersion3() throws Exception {
      URL url = Thread.currentThread().getContextClassLoader().getResource("migrationtest/migtest3.jar");
      URLClassLoader clsLoader = URLClassLoader.newInstance(new URL[] { url });
      Class<?> cls = clsLoader.loadClass("com.logitags.cibet.migration.Migrant");
      Object obj1 = cls.newInstance();
      Method method = cls.getMethod("setLonger", long.class);
      method.invoke(obj1, 15l);
      method = cls.getMethod("setHang", String.class);
      method.invoke(obj1, "K�ln");
      method = cls.getMethod("setDate", Date.class);
      method.invoke(obj1, now);
      method = cls.getMethod("setIntParam", int.class);
      method.invoke(obj1, 56);
      method = cls.getMethod("setTheId", long.class);
      method.invoke(obj1, 777l);

      Class<?> depcls = clsLoader.loadClass("com.logitags.cibet.migration.DependentBean");
      Object dep = depcls.newInstance();
      method = depcls.getMethod("setTheId", long.class);
      method.invoke(dep, 345l);
      method = depcls.getMethod("setDepString", String.class);
      method.invoke(dep, "Hallo");

      method = cls.getMethod("setDependendBean", depcls);
      method.invoke(obj1, dep);

      log.debug(obj1);
      byte[] bytes = CibetUtil.encode(obj1);
      return bytes;
   }

   private byte[] encodeVersion4() throws Exception {
      URL url = Thread.currentThread().getContextClassLoader().getResource("migrationtest/migtest4.jar");
      URLClassLoader clsLoader = URLClassLoader.newInstance(new URL[] { url });
      Class<?> cls = clsLoader.loadClass("com.logitags.cibet.migration.Migrant");
      Object obj1 = cls.newInstance();
      Method method = cls.getMethod("setLonger", long.class);
      method.invoke(obj1, 16l);
      method = cls.getMethod("setHang", String.class);
      method.invoke(obj1, "lobb");
      method = cls.getMethod("setDate", Date.class);
      method.invoke(obj1, now);
      method = cls.getMethod("setIntParam", int.class);
      method.invoke(obj1, 67);
      method = cls.getMethod("setTheId", long.class);
      method.invoke(obj1, 123l);
      method = cls.getMethod("setNewProp", short.class);
      method.invoke(obj1, (short) 24);
      method = cls.getMethod("setTransiInt", int.class);
      method.invoke(obj1, 688);
      method = cls.getMethod("setOffermann", String.class);
      method.invoke(obj1, "j�ger");

      Class<?> depcls = clsLoader.loadClass("com.logitags.cibet.migration.DependentBean");
      Object dep = depcls.newInstance();
      method = depcls.getMethod("setTheId", long.class);
      method.invoke(dep, 345l);
      method = depcls.getMethod("setDepString", String.class);
      method.invoke(dep, "Hallo");

      method = cls.getMethod("setDependendBean", depcls);
      method.invoke(obj1, dep);

      Object dep2 = depcls.newInstance();
      method = depcls.getMethod("setTheId", long.class);
      method.invoke(dep2, 346l);
      method = depcls.getMethod("setDepString", String.class);
      method.invoke(dep2, "Hallo2");
      Object dep3 = depcls.newInstance();
      method = depcls.getMethod("setTheId", long.class);
      method.invoke(dep3, 347l);
      method = depcls.getMethod("setDepString", String.class);
      method.invoke(dep3, "Hallo3");
      method = cls.getMethod("getDeplist");
      List l = (List) method.invoke(obj1);
      l.add(dep2);
      l.add(dep3);

      log.debug(obj1);
      byte[] bytes = CibetUtil.encode(obj1);
      return bytes;
   }

   private byte[] encodeVersion5() throws Exception {
      calNow.setTime(now);
      URL url = Thread.currentThread().getContextClassLoader().getResource("migrationtest/migtest5.jar");
      URLClassLoader clsLoader = URLClassLoader.newInstance(new URL[] { url });
      Class<?> cls = clsLoader.loadClass("com.logitags.cibet.migration.Migrant");
      Object obj1 = cls.newInstance();
      Method method = cls.getMethod("setLonger", String.class);
      method.invoke(obj1, "Haste");
      method = cls.getMethod("setHang", String.class);
      method.invoke(obj1, "lobb");
      method = cls.getMethod("setDate", Calendar.class);
      method.invoke(obj1, calNow);
      method = cls.getMethod("setIntParam", int.class);
      method.invoke(obj1, 67);
      method = cls.getMethod("setTheId", long.class);
      method.invoke(obj1, 123l);
      method = cls.getMethod("setNewProp", short.class);
      method.invoke(obj1, (short) 24);
      method = cls.getMethod("setTransiInt", int.class);
      method.invoke(obj1, 688);
      method = cls.getMethod("setOffermann", String.class);
      method.invoke(obj1, "j�ger");

      Class<?> depcls = clsLoader.loadClass("com.logitags.cibet.migration.DependentBean");
      Object dep = depcls.newInstance();
      method = depcls.getMethod("setTheId", long.class);
      method.invoke(dep, 345l);
      method = depcls.getMethod("setDepString", String.class);
      method.invoke(dep, "Hallo");

      method = cls.getMethod("setDependendBean", depcls);
      method.invoke(obj1, dep);

      Object dep2 = depcls.newInstance();
      method = depcls.getMethod("setTheId", long.class);
      method.invoke(dep2, 346l);
      method = depcls.getMethod("setDepString", String.class);
      method.invoke(dep2, "Hallo2");
      Object dep3 = depcls.newInstance();
      method = depcls.getMethod("setTheId", long.class);
      method.invoke(dep3, 347l);
      method = depcls.getMethod("setDepString", String.class);
      method.invoke(dep3, "Hallo3");
      method = cls.getMethod("getDeplist");
      List l = (List) method.invoke(obj1);
      l.add(dep2);
      l.add(dep3);

      log.debug(obj1);
      byte[] bytes = CibetUtil.encode(obj1);
      return bytes;
   }

   /**
    * a property is added and a property is changed from static to non-static: The new and non-static properties have
    * default value.
    * 
    * @throws Exception
    */
   @Test
   public void test() throws Exception {
      log.debug("create version 1 object");
      byte[] bytes1 = encodeVersion1();

      log.debug("decode version 2 object");
      URL url = Thread.currentThread().getContextClassLoader().getResource("migrationtest/migtest2.jar");
      URLClassLoader clsLoader2 = URLClassLoader.newInstance(new URL[] { url });

      Class<?> cls2 = clsLoader2.loadClass("com.logitags.cibet.migration.Migrant");

      Object obj2 = CibetUtil.decode(clsLoader2, bytes1);
      log.debug(obj2);
      Method method = obj2.getClass().getMethod("toString");
      Object res = method.invoke(obj2);
      Assert.assertEquals(
            "com.logitags.cibet.migration.Migrant, date:Thu Jan 01 01:07:24 CET 1970, intParam:56, theId:777, newProp:0, dependendBean: || com.logitags.cibet.migration.DependentBeandepString:Hallo, theId:345 || com.logitags.cibet.migration.Migrant, non-static hang:null, longer:15",
            res);
   }

   /**
    * a property is removed and a property is changed from non-static to static: The removed property is discarded and
    * the static properties has default value.
    * 
    * @throws Exception
    */
   @Test
   public void test2() throws Exception {
      log.debug("create version 2 object");
      byte[] bytes = encodeVersion2();

      log.debug("decode version 1 object");
      URL url = Thread.currentThread().getContextClassLoader().getResource("migrationtest/migtest1.jar");
      URLClassLoader clsLoader1 = URLClassLoader.newInstance(new URL[] { url });

      Object obj2 = CibetUtil.decode(clsLoader1, bytes);
      log.debug(obj2);
      Method method = obj2.getClass().getMethod("toString");
      Object res = method.invoke(obj2);
      Assert.assertEquals(
            "com.logitags.cibet.migration.Migrant, date:Thu Jan 01 01:07:24 CET 1970, intParam:56, theId:777, dependendBean: || com.logitags.cibet.migration.DependentBeandepString:Hallo, theId:345 || com.logitags.cibet.migration.Migrant, static hang:null, longer:15",
            res);
   }

   /**
    * Class changes base class: The properties of the new base class are all default values. Properties of old base
    * class are lost.
    * 
    * @throws Exception
    */
   @Test
   public void test3() throws Exception {
      log.debug("create version 2 object");
      byte[] bytes = encodeVersion2();

      log.debug("decode version 3 object");
      URL url = Thread.currentThread().getContextClassLoader().getResource("migrationtest/migtest3.jar");
      URLClassLoader clsLoader = URLClassLoader.newInstance(new URL[] { url });

      Object obj = CibetUtil.decode(clsLoader, bytes);
      log.debug(obj);
      Method method = obj.getClass().getMethod("toString");
      Object res = method.invoke(obj);
      Assert.assertEquals(
            "com.logitags.cibet.migration.Migrant, date:null, intParam:0, theId:0, dependendBean:null || com.logitags.cibet.migration.Migrant, non-static hang:K�ln, longer:15",
            res);
   }

   /**
    * a property is added and a property is changed from static to non-static. Class inherits from additional
    * MiddleBean:
    * <p>
    * The new and non-static properties have default value. The properties of the new middle class are all default
    * values.
    * 
    * @throws Exception
    */
   @Test
   public void test4() throws Exception {
      log.debug("create version 1 object");
      byte[] bytes = encodeVersion1();

      log.debug("decode version 4 object");
      URL url = Thread.currentThread().getContextClassLoader().getResource("migrationtest/migtest4.jar");
      URLClassLoader clsLoader = URLClassLoader.newInstance(new URL[] { url });

      Object obj = CibetUtil.decode(clsLoader, bytes);
      Method method = obj.getClass().getMethod("toString");
      Object res = method.invoke(obj);
      log.debug(res);
      Assert.assertEquals(
            "BasicMigrateBean, date:Thu Jan 01 01:07:24 CET 1970, intParam:56, theId:777, newProp:0, dependendBean: || com.logitags.cibet.migration.DependentBeandepString:Hallo, theId:345 || MiddleBean, offermann:null, transiInt:0, depList.size:NULL || com.logitags.cibet.migration.Migrant, non-static hang:null, longer:15",
            res);
   }

   /**
    * Middle inherited class is removed:
    * <p>
    * The values of the removed class are lost. The values of the new base class are lost.
    * 
    * @throws Exception
    */
   @Test
   public void test5() throws Exception {
      log.debug("create version 4 object");
      byte[] bytes = encodeVersion4();

      log.debug("decode version 3 object");
      URL url = Thread.currentThread().getContextClassLoader().getResource("migrationtest/migtest3.jar");
      URLClassLoader clsLoader = URLClassLoader.newInstance(new URL[] { url });

      Object obj = CibetUtil.decode(clsLoader, bytes);
      Method method = obj.getClass().getMethod("toString");
      Object res = method.invoke(obj);
      log.debug(res);
      Assert.assertEquals(
            "com.logitags.cibet.migration.Migrant, date:null, intParam:0, theId:0, dependendBean:null || com.logitags.cibet.migration.Migrant, non-static hang:lobb, longer:16",
            res);
   }

   /**
    * data types changed:
    * <p>
    * java.io.InvalidClassException: com.logitags.cibet.migration.Migrant; incompatible types for field longer
    * 
    * @throws Exception
    */
   @Test(expected = RuntimeException.class)
   public void test6() throws Exception {
      log.debug("create version 4 object");
      byte[] bytes = encodeVersion4();

      log.debug("decode version 5 object");
      URL url = Thread.currentThread().getContextClassLoader().getResource("migrationtest/migtest5.jar");
      URLClassLoader clsLoader = URLClassLoader.newInstance(new URL[] { url });

      CibetUtil.decode(clsLoader, bytes);
   }

}
