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

/**
 * Defines Callback methods which are called when the business case is shed, throttled by a valve or an alarm threshold
 * is exceeded.
 * 
 */
public interface LoadControlCallback {

   /**
    * called when the business case is shed. A fallback implementation may be executed instead.
    * 
    * @param loadControlData
    *           metadata of the business case and the applied LoadControl feature
    */
   void onShed(LoadControlData loadControlData);

   /**
    * called when an alarm threshold was exceeded.
    * 
    * @param loadControlData
    *           metadata of the business case and the applied LoadControl feature
    */
   void onAlarm(LoadControlData loadControlData);

   /**
    * called when a valve throttled the request but let pass through eventually.
    * 
    * @param loadControlData
    *           metadata of the business case and the applied LoadControl feature
    */
   void onThrottled(LoadControlData loadControlData);

   /**
    * called when a request is accepted by all Monitors of LoadControlActuator.
    * 
    * @param loadControlData
    *           metadata of the business case and the applied LoadControl feature
    */
   void onAccepted(LoadControlData loadControlData);
}
