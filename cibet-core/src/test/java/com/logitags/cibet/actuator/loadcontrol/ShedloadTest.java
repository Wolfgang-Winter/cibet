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
package com.logitags.cibet.actuator.loadcontrol;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.core.ControlEvent;

public class ShedloadTest {

   private static Logger log = Logger.getLogger(ShedloadTest.class);

   // @Test
   public void test() {
      LoadMeasure lm = new LoadMeasure();
      try {
         lm.start();

         try {
            Thread.sleep(3000);
         } catch (Exception ignored) {
         }
      } finally {
         lm.setActive(false);
      }

   }

   @Ignore
   @Test
   public void test2() {
      registerSetpoint(this.getClass().getName(), "LOADCONTROL", ControlEvent.PERSIST);

      do {

      } while (1 == 1);
      // List<G> list = new ArrayList<>();
      // list.add(new G("5"));
      // list.add(new G("1"));
      // list.add(new G("7"));
      // list.add(new G("6"));
      //
      // for (G g : list) {
      // log.debug(g.getStr());
      // }
      //
      // Collections.sort(list, new Comparator<G>() {
      //
      // @Override
      // public int compare(G o1, G o2) {
      // return o1.getStr().compareTo(o2.getStr());
      // }
      // });
      //
      // for (G g : list) {
      // log.debug(g.getStr());
      // }

   }

   private class G {
      private String str;

      public G(String name) {
         str = name;
      }

      /**
       * @return the str
       */
      public String getStr() {
         return str;
      }

      /**
       * @param str
       *           the str to set
       */
      public void setStr(String str) {
         this.str = str;
      }
   }

   private Setpoint registerSetpoint(String clazz, String act, ControlEvent... events) {
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

}
