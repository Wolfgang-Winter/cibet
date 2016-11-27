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

import com.logitags.cibet.core.EventMetadata;

/**
 * A Monitor is able to measure and control a specific type of load on the application/system.
 * 
 * @author Wolfgang
 *
 */
public interface Monitor {

   /**
    * returns a unique name of the Monitor implementation.
    * 
    * @return name of the monitor
    */
   String getName();

   /**
    * executed in the sensor before the detected event.
    * 
    * @param currentResult
    *           the current aggregated result of the previous executed monitors
    * @param callback
    *           callback class
    * @param metadata
    *           the metadata of this event
    * @param currentSetpointId
    *           applied Setpoint id that is configured for the current event.
    * @return MonitorResult
    */
   MonitorResult beforeEvent(MonitorResult currentResult, LoadControlCallback callback, EventMetadata metadata,
         String currentSetpointId);

   /**
    * executed in the sensor after the detected event.
    * 
    * @param callback
    *           callback class
    * @param metadata
    *           the metadata of this event
    * @param currentSetpointId
    *           applied Setpoint id that is currently controlled by this Actuator instance.
    */
   void afterEvent(LoadControlCallback callback, EventMetadata metadata, String currentSetpointId);

   /**
    * resets all counters and measurements for this setpoint.
    * 
    * @param setpointId
    *           setpoint for which to reset
    */
   void reset(String setpointId);

   /**
    * executed when the LoadControlActuator is closed. Cleaning up if necessary.
    */
   void close();

   /**
    * returns the MonitorMode of the Monitor
    * 
    * @return MonitorStatus
    */
   MonitorStatus getStatus();

   /**
    * sets the MonitorMode of the Monitor
    * 
    * @param mode
    *           MonitorStatus to set
    */
   void setStatus(MonitorStatus mode);

}
