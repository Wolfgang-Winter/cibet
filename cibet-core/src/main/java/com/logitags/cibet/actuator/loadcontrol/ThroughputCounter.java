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

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Wolfgang
 *
 */
public class ThroughputCounter implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private AtomicInteger counter;

   private long timestamp;

   public ThroughputCounter(long ts) {
      timestamp = ts;
      counter = new AtomicInteger(0);
   }

   public ThroughputCounter(long ts, int count) {
      timestamp = ts;
      counter = new AtomicInteger(count);
   }

   /**
    * @return the counter
    */
   public int getCounter() {
      return counter.get();
   }

   /**
    * increments the counter
    * 
    * @return current count
    */
   public int incrementAndGet() {
      return counter.incrementAndGet();
   }

   /**
    * @return the timestamp
    */
   public long getTimestamp() {
      return timestamp;
   }

   public void setTimestamp(long ts) {
      timestamp = ts;
   }
}
