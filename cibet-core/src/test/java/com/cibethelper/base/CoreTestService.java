/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2016 Dr. Wolfgang Winter
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
package com.cibethelper.base;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TComplexEntity2;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.ConfigurationService;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.core.ControlEvent;

public abstract class CoreTestService {

   public static String TENANT = "testTenant";

   public static final String USER = "USER";

   public static String initConfiguration(String configName) throws Exception {
      Field FILENAME = Configuration.class.getDeclaredField("CONFIGURATION_FILENAME");
      FILENAME.setAccessible(true);
      FILENAME.set(null, configName);

      ConfigurationService confMan = new ConfigurationService();
      return confMan.initialise();
   }

   public static void registerSetpoint(Class<?> clazz, String act, String methodName, ControlEvent... events) {
      Setpoint sp = registerSetpoint(clazz, act, events);
      sp.setMethod(methodName);
   }

   public static Setpoint registerSetpoint(Class<?> clazz, String act, ControlEvent... events) {
      return registerSetpoint(clazz.getName(), act, events);
   }

   public static Setpoint registerSetpoint(String clazz, String act, ControlEvent... events) {
      Setpoint sp = new Setpoint(String.valueOf(new Date().getTime()), null);
      sp.setTarget(clazz);
      List<String> evl = new ArrayList<String>();
      for (ControlEvent ce : events) {
         evl.add(ce.name());
      }
      sp.setEvent(evl.toArray(new String[0]));
      Configuration cman = Configuration.instance();
      sp.addActuator(cman.getActuator(act));
      cman.registerSetpoint(sp);
      return sp;
   }

   public static Setpoint registerSetpoint(String clazz, List<String> acts, ControlEvent... events) {
      Setpoint sp = new Setpoint(String.valueOf(new Date().getTime()), null);
      sp.setTarget(clazz);
      List<String> evl = new ArrayList<String>();
      for (ControlEvent ce : events) {
         evl.add(ce.name());
      }
      sp.setEvent(evl.toArray(new String[0]));
      Configuration cman = Configuration.instance();
      for (String scheme : acts) {
         sp.addActuator(cman.getActuator(scheme));
      }
      cman.registerSetpoint(sp);
      return sp;
   }

   public static void registerSetpoint(Class<?> clazz, List<String> acts, String methodName, ControlEvent... events) {
      Setpoint sp = registerSetpoint(clazz.getName(), acts, events);
      sp.setMethod(methodName);
   }

   public static TEntity createTEntity(int counter, String name) {
      TEntity te = new TEntity();
      te.setCounter(counter);
      te.setNameValue(name);
      te.setOwner(name);
      return te;
   }

   public static TComplexEntity createTComplexEntity() {
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
      ce.setCompValue(12);
      ce.setTen(e1);
      ce.setOwner(TENANT);
      ce.setEagerList(eagerList);
      ce.setLazyList(lazyList);
      ce.addLazyList(createTEntity(6, "Hase6"));

      return ce;
   }

   public static TComplexEntity2 createTComplexEntity2() {
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

      TComplexEntity2 ce = new TComplexEntity2();
      ce.setCompValue(12);
      ce.setTen(e1);
      ce.setOwner(TENANT);
      ce.setEagerList(eagerList);
      ce.setLazyList(lazyList);
      ce.addLazyList(createTEntity(6, "Hase6"));

      return ce;
   }

}
