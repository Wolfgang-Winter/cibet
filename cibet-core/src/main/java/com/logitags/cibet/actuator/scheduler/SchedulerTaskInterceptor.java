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
package com.logitags.cibet.actuator.scheduler;

import com.logitags.cibet.actuator.dc.DcControllable;

/**
 * This interface is called back by the SchedulerTask implementation before and after executing the scheduled task
 * 
 * @author Wolfgang
 * 
 */
public interface SchedulerTaskInterceptor {

   /**
    * callback function executed before the scheduled business case is executed by the batch process.
    * 
    * @param dc
    *           Object that contains the business case data (resource) and other metadata
    * @throws RejectException
    *            thrown to indicate that the scheduled business case shall be not executed. The DcControllable object
    *            will be set to status REJECTED
    */
   void beforeTask(DcControllable dc) throws RejectException;

   /**
    * callback function executed after the scheduled business case is executed by the batch process.
    * 
    * @param dc
    *           Object that contains the business case data (resource) and other metadata
    */
   void afterTask(DcControllable dc);

}
