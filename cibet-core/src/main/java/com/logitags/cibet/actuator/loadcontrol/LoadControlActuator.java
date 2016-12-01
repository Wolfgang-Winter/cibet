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

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.common.AbstractActuator;
import com.logitags.cibet.actuator.common.Actuator;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.ExecutionStatus;
import com.udojava.jmx.wrapper.JMXBeanWrapper;

/**
 * Controls load on an application per setpoint. Load is controlled in three ways:
 * <p>
 * alarm - if a threshold is exceeded, an alarm is raised
 * <p>
 * valve - if a threshold is exceeded, the request is throttled
 * <P>
 * shed - if a threshold is exceeded the request is shed
 * <p>
 * Load is monitored by several different monitors.
 * 
 * @author Wolfgang
 *
 */
public class LoadControlActuator extends AbstractActuator {

   /**
   * 
   */
   private static final long serialVersionUID = 1L;

   private static Log log = LogFactory.getLog(LoadControlActuator.class);

   private static final String JMX_OBJECTNAME_PREFIX = Configuration.JMX_BASE + ":type=LoadControlActuator,app="
         + Configuration.getApplicationName() + ",name=";

   private static final String CURRENTSETPOINTID = "__CURRENTSETPOINTID";

   public static final String DEFAULTNAME = "LOADCONTROL";

   private ResponseTimeMonitor responseTimeMonitor = new ResponseTimeMonitor();
   private MemoryMonitor memoryMonitor = new MemoryMonitor();
   private ThreadCountMonitor threadCountMonitor = new ThreadCountMonitor();
   private CpuLoadMonitor cpuLoadMonitor = new CpuLoadMonitor();
   private ThreadTimeMonitor threadTimeMonitor = new ThreadTimeMonitor();
   private ThreadContentionMonitor threadContentionMonitor = new ThreadContentionMonitor();
   // private SteppingThroughputMonitor throughputMonitor = new
   // SteppingThroughputMonitor();
   private ThroughputMonitor throughputMonitor = new ThroughputMonitor(this);
   private FileDescriptorMonitor fileDescriptorMonitor = new FileDescriptorMonitor();

   private Monitor[] customMonitors = new Monitor[0];

   private Date startTime;

   private List<String> setpointIds = Collections.synchronizedList(new ArrayList<String>());

   private transient LoadControlCallback loadControlCallback;

   /**
    * denied requests
    */
   private Map<String, AtomicLong> shed = Collections.synchronizedMap(new HashMap<String, AtomicLong>());

   /**
    * accepted requests
    */
   private Map<String, AtomicLong> accepted = Collections.synchronizedMap(new HashMap<String, AtomicLong>());

   private Map<String, AtomicLong> firstHitTime = Collections.synchronizedMap(new HashMap<String, AtomicLong>());
   private Map<String, AtomicLong> lastHitTime = Collections.synchronizedMap(new HashMap<String, AtomicLong>());

   public LoadControlActuator() {
      this(DEFAULTNAME);
   }

   public LoadControlActuator(String name) {
      setName(name);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.actuator.AbstractActuator#init()
    */
   @Override
   public void init(Configuration config) {
      log.info("init LoadControlActuator");
      startTime = new Date();
      registerCommonJMXBean();

      for (Setpoint sp : config.getSetpoints()) {
         for (Actuator act : sp.getActuators()) {
            if (act.getName().equals(this.getName())) {
               log.info("init " + this.getName() + " with setpoint " + sp.getId());
               setpointIds.add(sp.getId());

               resetAcceptCounter(sp.getId());
               cpuLoadMonitor.reset(sp.getId());
               responseTimeMonitor.reset(sp.getId());
               memoryMonitor.reset(sp.getId());
               threadTimeMonitor.reset(sp.getId());
               threadContentionMonitor.reset(sp.getId());
               threadCountMonitor.reset(sp.getId());
               // throughputMonitor.reset(sp.getId());
               throughputMonitor.reset(sp.getId());
               fileDescriptorMonitor.reset(sp.getId());

               for (Monitor monitor : customMonitors) {
                  monitor.reset(sp.getId());
               }

               registerJMXBean(sp.getId());
               break;
            }
         }
      }
   }

   private void registerJMXBean(String setpointId) {
      log.info("register LoadControl MBean for Setpoint " + setpointId);
      try {
         MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
         ObjectName oname = new ObjectName(JMX_OBJECTNAME_PREFIX + this.getName() + ",setpoint=" + setpointId);
         if (mbs.isRegistered(oname)) {
            log.info("MBean " + oname.getCanonicalName() + " already registered");
         } else {
            LoadControlJMXBean mbean = new LoadControlJMXBean(this, setpointId);
            JMXBeanWrapper wrappedBean = new JMXBeanWrapper(mbean);
            mbs.registerMBean(wrappedBean, oname);
            log.info("MBean " + oname.getCanonicalName() + " registered");
         }

      } catch (Exception e) {
         log.warn("Failed to register LoadControlJMXBean MBean: " + e.getMessage(), e);
      }
   }

   private void registerCommonJMXBean() {
      log.info("register common LoadControl MBean");
      try {
         MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
         ObjectName oname = new ObjectName(JMX_OBJECTNAME_PREFIX + this.getName());
         if (mbs.isRegistered(oname)) {
            log.info("MBean " + oname.getCanonicalName() + " already registered");
         } else {
            CommonLoadControlJMXBean mbean = new CommonLoadControlJMXBean(this);
            JMXBeanWrapper wrappedBean = new JMXBeanWrapper(mbean);
            mbs.registerMBean(wrappedBean, oname);
            log.info("MBean " + oname.getCanonicalName() + " registered");
         }

         oname = new ObjectName(Configuration.JMX_BASE + ":type=LoadControlActuator,name=VM");
         if (mbs.isRegistered(oname)) {
            log.info("MBean " + oname.getCanonicalName() + " already registered");
         } else {
            VMLoadControlJMXBean vmJmxBean = new VMLoadControlJMXBean();
            JMXBeanWrapper wrappedBean = new JMXBeanWrapper(vmJmxBean);
            mbs.registerMBean(wrappedBean, oname);
            log.info("MBean " + oname.getCanonicalName() + " registered");
         }

      } catch (Exception e) {
         log.warn("Failed to register MBean: " + e.getMessage(), e);
      }
   }

   /*
    * 
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.actuator.AbstractActuator#close()
    */
   @Override
   public void close() {
      threadContentionMonitor.close();
      threadTimeMonitor.close();

      for (Monitor monitor : customMonitors) {
         monitor.close();
      }

      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

      try {
         ObjectName oname = new ObjectName(JMX_OBJECTNAME_PREFIX + this.getName());
         if (mbs.isRegistered(oname)) {
            mbs.unregisterMBean(oname);
            log.info("unregister CommonLoadControlJMXBean MBean " + oname.getCanonicalName());
         }

      } catch (Exception e) {
         log.warn("Failed to unregister LoadControlJMXBean MBean: " + e.getMessage(), e);
      }

      try {
         ObjectName oname = new ObjectName(Configuration.JMX_BASE + ":type=LoadControlActuator,name=VM");
         if (mbs.isRegistered(oname)) {
            mbs.unregisterMBean(oname);
            log.info("unregister VMLoadControlJMXBean MBean " + oname.getCanonicalName());
         }

      } catch (Exception e) {
         log.warn("Failed to unregister LoadControlJMXBean MBean: " + e.getMessage(), e);
      }

      for (String spId : setpointIds) {
         try {
            ObjectName oname = new ObjectName(JMX_OBJECTNAME_PREFIX + this.getName() + ",setpoint=" + spId);
            if (mbs.isRegistered(oname)) {
               mbs.unregisterMBean(oname);
               log.info("unregister MBean " + oname.getCanonicalName());
            }

         } catch (Exception e) {
            log.warn("Failed to unregister LoadControlJMXBean MBean: " + e.getMessage(), e);
         }
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.actuator.AbstractActuator#beforeEvent(com.logitags. cibet.core.EventMetadata)
    */
   @Override
   public void beforeEvent(EventMetadata ctx) {
      if (ctx.getExecutionStatus() == ExecutionStatus.DENIED) {
         log.debug("ExecutionStatus is DENIED. Skip beforeEvent of " + this.getClass().getSimpleName());
         return;
      } else {
         log.debug("LoadControlActuator beforeEvent()");
      }

      String currentSetpointId = "?";
      for (Setpoint sp : ctx.getSetpoints()) {
         if (setpointIds.contains(sp.getId())) {
            currentSetpointId = sp.getId();
            long timestamp = System.currentTimeMillis();
            if (!firstHitTime.containsKey(sp.getId())) {
               firstHitTime.put(sp.getId(), new AtomicLong(timestamp));
            }
            lastHitTime.get(sp.getId()).set(timestamp);
            break;
         }
      }
      ctx.getProperties().put(CURRENTSETPOINTID, currentSetpointId);

      MonitorResult result = monitorsBeforeEvent(ctx, currentSetpointId);
      if (result == MonitorResult.SHED) {
         shed.get(currentSetpointId).getAndIncrement();
         ctx.setExecutionStatus(ExecutionStatus.SHED);

      } else {
         accepted.get(currentSetpointId).getAndIncrement();

         if (loadControlCallback != null) {
            LoadControlData lcdata = new LoadControlData(currentSetpointId, ctx.getResource(), ctx.getControlEvent(),
                  getName(), MonitorResult.PASSED);
            loadControlCallback.onAccepted(lcdata);
         }
      }
   }

   /**
    * Executed after the business case. The afterEvent methods of all Monitors are called. This method can be
    * overwritten to add own Monitor implementations.
    * 
    * @see com.logitags.cibet.actuator.common.AbstractActuator#afterEvent(com.logitags.cibet.core.EventMetadata)
    */
   @Override
   public void afterEvent(EventMetadata ctx) {
      String currentSetpointId = (String) ctx.getProperties().get(CURRENTSETPOINTID);

      // decrement counter
      threadCountMonitor.afterEvent(loadControlCallback, ctx, currentSetpointId);

      if (ctx.getExecutionStatus() == ExecutionStatus.DENIED) {
         log.debug("ExecutionStatus is DENIED. Skip afterEvent of " + this.getClass().getSimpleName());
         return;
      }

      threadTimeMonitor.afterEvent(loadControlCallback, ctx, currentSetpointId);
      responseTimeMonitor.afterEvent(loadControlCallback, ctx, currentSetpointId);
      threadContentionMonitor.afterEvent(loadControlCallback, ctx, currentSetpointId);
      throughputMonitor.afterEvent(loadControlCallback, ctx, currentSetpointId);
      for (Monitor monitor : customMonitors) {
         monitor.afterEvent(loadControlCallback, ctx, currentSetpointId);
      }
   }

   /**
    * executes the beforeEvent methods of all Monitors. This method must be overwritten to add own Monitor
    * implementations.
    * 
    * @param ctx
    *           EventMetadata
    * @param currentSetpointId
    *           current setpoint id
    * @return result
    */
   protected MonitorResult monitorsBeforeEvent(EventMetadata ctx, String currentSetpointId) {
      MonitorResult result = MonitorResult.PASSED;
      result = threadTimeMonitor.beforeEvent(result, loadControlCallback, ctx, currentSetpointId);
      result = threadCountMonitor.beforeEvent(result, loadControlCallback, ctx, currentSetpointId);
      result = responseTimeMonitor.beforeEvent(result, loadControlCallback, ctx, currentSetpointId);
      result = threadContentionMonitor.beforeEvent(result, loadControlCallback, ctx, currentSetpointId);
      result = memoryMonitor.beforeEvent(result, loadControlCallback, ctx, currentSetpointId);
      result = cpuLoadMonitor.beforeEvent(result, loadControlCallback, ctx, currentSetpointId);
      // result = throughputMonitor.beforeEvent(result, callback, ctx,
      // currentSetpointId);
      result = throughputMonitor.beforeEvent(result, loadControlCallback, ctx, currentSetpointId);
      result = fileDescriptorMonitor.beforeEvent(result, loadControlCallback, ctx, currentSetpointId);

      for (Monitor monitor : customMonitors) {
         result = monitor.beforeEvent(result, loadControlCallback, ctx, currentSetpointId);
         if (result == MonitorResult.SHED) {
            break;
         }
      }

      return result;
   }

   /**
    * @return the startTime
    */
   public Date getStartTime() {
      return startTime;
   }

   /**
    * @param setpointId
    *           setpoint
    * @return the denied count for the setpoint
    */
   public AtomicLong getShed(String setpointId) {
      return shed.get(setpointId);
   }

   /**
    * @param setpointId
    * @return the accepted counter for the given setpointId
    */
   public AtomicLong getAccepted(String setpointId) {
      return accepted.get(setpointId);
   }

   /**
    * @return the loadControlCallback
    */
   public LoadControlCallback getLoadControlCallback() {
      return loadControlCallback;
   }

   /**
    * @param impl
    *           the loadControlCallback to set
    */
   public void setLoadControlCallback(LoadControlCallback impl) {
      this.loadControlCallback = impl;
   }

   public void resetAcceptCounter(String setpointId) {
      accepted.put(setpointId, new AtomicLong(0));
      shed.put(setpointId, new AtomicLong(0));
      firstHitTime.remove(setpointId);
      lastHitTime.put(setpointId, new AtomicLong(0));
   }

   /**
    * @param setpointId
    *           setpoint id
    * @return the firstHitTime for the given setpointId
    */
   public AtomicLong getFirstHitTime(String setpointId) {
      return firstHitTime.get(setpointId);
   }

   /**
    * @param setpointId
    *           setpoint id
    * @return the lastHitTime counter for the given setpointId
    */
   public AtomicLong getLastHitTime(String setpointId) {
      return lastHitTime.get(setpointId);
   }

   /**
    * @return the responseTimeMonitor
    */
   public ResponseTimeMonitor getResponseTimeMonitor() {
      return responseTimeMonitor;
   }

   /**
    * @return the memoryMonitor
    */
   public MemoryMonitor getMemoryMonitor() {
      return memoryMonitor;
   }

   /**
    * @return the threadCountMonitor
    */
   public ThreadCountMonitor getThreadCountMonitor() {
      return threadCountMonitor;
   }

   /**
    * @return the cpuLoadMonitor
    */
   public CpuLoadMonitor getCpuLoadMonitor() {
      return cpuLoadMonitor;
   }

   /**
    * @return the threadTimeMonitor
    */
   public ThreadTimeMonitor getThreadTimeMonitor() {
      return threadTimeMonitor;
   }

   /**
    * @return the threadContentionMonitor
    */
   public ThreadContentionMonitor getThreadContentionMonitor() {
      return threadContentionMonitor;
   }

   /**
    * @return the customMonitors
    */
   public Monitor[] getCustomMonitors() {
      return customMonitors;
   }

   /**
    * returns the monitor with the given name
    * 
    * @param name
    * @return Monitor or null if not existing
    */
   public Monitor getCustomMonitor(String name) {
      if (name == null) {
         throw new IllegalArgumentException("Parameter name must not be null");
      }
      for (Monitor m : customMonitors) {
         if (name.equals(m.getName())) {
            return m;
         }
      }
      return null;
   }

   /**
    * @param customMonitorInstances
    *           the customMonitors to set
    */
   public void setCustomMonitors(Monitor[] customMonitorInstances) {
      this.customMonitors = customMonitorInstances;
   }

   /**
    * @return the continuousThroughputMonitor
    */
   public ThroughputMonitor getThroughputMonitor() {
      return throughputMonitor;
   }

   /**
    * @return the fileDescriptorMonitor
    */
   public FileDescriptorMonitor getFileDescriptorMonitor() {
      return fileDescriptorMonitor;
   }

}
