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
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.OperatingSystemMXBean;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.udojava.jmx.wrapper.JMXBean;
import com.udojava.jmx.wrapper.JMXBeanAttribute;

@JMXBean(description = "JMX bean for monitoring VM common properties", sorted = true)
public class VMLoadControlJMXBean {

   private static Log log = LogFactory.getLog(VMLoadControlJMXBean.class);

   private static final String SYSTEMATTRIBUTE = "SystemCpuLoad";
   private static final String PROCESSATTRIBUTE = "ProcessCpuLoad";
   private static final String FILEDESCRIPTORATTRIBUTE = "OpenFileDescriptorCount";
   private static final String MAXFILEDESCRIPTORATTRIBUTE = "MaxFileDescriptorCount";
   private static final MBeanServer MBEANSERVER = ManagementFactory.getPlatformMBeanServer();

   private static MemoryPoolMXBean tenuredGenPool;

   private static Boolean processCpuLoadAvailable = null;
   /**
    * 0: not available, 1: loadAverage, 2: attribute
    */
   private static int systemCpuLoadAvailable = -1;

   private static boolean fileDescriptorCountAvailable = false;

   static {
      tenuredGenPool();
      isProcessCpuLoadAvailable();
      systemCpuLoadAvailable();
      resolveFileDescriptorCount();
   }

   public VMLoadControlJMXBean() {
   }

   /**
    * @return the tenuredGenPool
    */
   public static MemoryPoolMXBean getTenuredGenPool() {
      return tenuredGenPool;
   }

   /**
    * Tenured Space Pool can be determined by it being of type HEAP and by it
    * being possible to set the usage threshold.
    */
   private static synchronized void tenuredGenPool() {
      if (tenuredGenPool == null) {
         for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType() == MemoryType.HEAP && pool.isUsageThresholdSupported()
                  && pool.isCollectionUsageThresholdSupported()) {
               tenuredGenPool = pool;
               break;
            }
         }
      }
   }

   private static synchronized boolean isProcessCpuLoadAvailable() {
      if (processCpuLoadAvailable == null) {
         Object processLoad = retrieveLoadAttribute(PROCESSATTRIBUTE);
         if (processLoad == null) {
            log.warn("Unable to measure Process CPU load on this operating system and Java VM");
            processCpuLoadAvailable = false;

         } else {
            log.warn("Process load is measured with attribute " + PROCESSATTRIBUTE + " of OperatingSystemMXBean. "
                  + "This may be an expensive method dependend on the operating system and Java VM");
            processCpuLoadAvailable = true;
         }
      }
      return processCpuLoadAvailable;
   }

   private static synchronized int systemCpuLoadAvailable() {
      if (systemCpuLoadAvailable == -1) {
         Object systemLoad = retrieveLoadAttribute(SYSTEMATTRIBUTE);
         if (systemLoad == null) {
            OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
            double averageLoad = operatingSystemMXBean.getSystemLoadAverage();
            if (averageLoad >= 0) {
               log.info(
                     "System CPU load is measured with the Java method getSystemLoadAverage() of the OperatingSystemMXBean.");
               systemCpuLoadAvailable = 1;
            } else {
               log.warn("Unable to measure System CPU load on this operating system and Java VM");
               systemCpuLoadAvailable = 0;
            }

         } else {
            log.warn("System CPU load is measured with attribute " + SYSTEMATTRIBUTE + " of OperatingSystemMXBean. "
                  + "This may be an expensive method dependend on the operating system and Java VM");
            systemCpuLoadAvailable = 2;
         }
      }
      return systemCpuLoadAvailable;
   }

   private static synchronized void resolveFileDescriptorCount() {
      Object fdCount = retrieveLoadAttribute(FILEDESCRIPTORATTRIBUTE);
      Object maxFdCount = retrieveLoadAttribute(MAXFILEDESCRIPTORATTRIBUTE);
      if (fdCount == null || maxFdCount == null) {
         log.warn("Unable to monitor file descriptor count on this operating system and Java VM");

      } else {
         log.warn("File descriptor count is measured with attributes " + FILEDESCRIPTORATTRIBUTE + " and "
               + MAXFILEDESCRIPTORATTRIBUTE + " of OperatingSystemMXBean.");
         fileDescriptorCountAvailable = true;
      }
   }

   private static Object retrieveLoadAttribute(String name) {
      try {
         ObjectName operatingSystemObjectname = new ObjectName("java.lang:type=OperatingSystem");
         return MBEANSERVER.getAttribute(operatingSystemObjectname, name);

      } catch (MalformedObjectNameException e) {
         throw new RuntimeException(e);
      } catch (InstanceNotFoundException e) {
         throw new RuntimeException(e);
      } catch (ReflectionException e) {
         throw new RuntimeException(e);
      } catch (AttributeNotFoundException e) {
         return null;
      } catch (MBeanException e) {
         throw new RuntimeException(e);
      }
   }

   @JMXBeanAttribute(name = "CPU: System Load Average (%)", description = "the recent cpu usage for the whole system. -1 if not available", sortValue = "f1")
   public double getSystemCpuLoad() {
      switch (systemCpuLoadAvailable) {
      case 0:
         return -1;
      case 1:
         OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
         double load = operatingSystemMXBean.getSystemLoadAverage() * 100
               / operatingSystemMXBean.getAvailableProcessors();
         return load;

      case 2:
         return (double) retrieveLoadAttribute(SYSTEMATTRIBUTE) * 100;

      default:
         return -1;
      }
   }

   @JMXBeanAttribute(name = "CPU: Process Load Average (%)", description = "the recent cpu usage for the Java Virtual Machine process. -1 if not available", sortValue = "f2")
   public double getProcessCpuLoad() {
      if (processCpuLoadAvailable) {
         return (double) retrieveLoadAttribute(PROCESSATTRIBUTE) * 100;
      } else {
         return -1;
      }
   }

   @JMXBeanAttribute(name = "Memory: Heap Usage (kb)", description = "total heap memory usage in kilobyte", sortValue = "m1")
   public long getHeapMemoryUsage() {
      MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
      return memoryMXBean.getHeapMemoryUsage().getUsed() / 1024;
   }

   @JMXBeanAttribute(name = "Memory: Heap Usage (%)", description = "total heap memory usage in % of the max", sortValue = "m2")
   public double getHeapMemoryUsagePercent() {
      MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
      long max = memoryMXBean.getHeapMemoryUsage().getMax();
      if (max == -1) {
         return -1;
      } else {
         return 100d * memoryMXBean.getHeapMemoryUsage().getUsed() / max;
      }
   }

   @JMXBeanAttribute(name = "Memory: Tenured Gen Usage (kb)", description = "memory usage of the tenured generation pool in kb", sortValue = "m3")
   public long getTenuredGenUsage() {
      if (tenuredGenPool != null) {
         return tenuredGenPool.getUsage().getUsed() / 1024;
      } else {
         return -1;
      }
   }

   @JMXBeanAttribute(name = "Memory: Tenured Gen Usage (%)", description = "memory usage of the tenured generation pool in percent of the max", sortValue = "m4")
   public double getTenuredGenUsagePercent() {
      if (tenuredGenPool == null)
         return -1;
      long max = tenuredGenPool.getUsage().getMax();
      if (max == -1) {
         return -1;
      } else {
         return 100d * tenuredGenPool.getUsage().getUsed() / max;
      }
   }

   @JMXBeanAttribute(name = "Memory: Tenured Gen Collection Usage (kb)", description = "memory collection usage of the tenured generation pool in kb", sortValue = "m5")
   public long getTenuredGenCollectionUsage() {
      if (tenuredGenPool != null) {
         return tenuredGenPool.getCollectionUsage().getUsed() / 1024;
      } else {
         return -1;
      }
   }

   @JMXBeanAttribute(name = "Memory: Tenured Gen Collection Usage (%)", description = "memory collection usage of the tenured generation pool in percent", sortValue = "m6")
   public double getTenuredGenCollectionUsagePercent() {
      if (tenuredGenPool == null)
         return -1;
      long max = tenuredGenPool.getCollectionUsage().getMax();
      if (max == -1) {
         return -1;
      } else {
         return 100d * tenuredGenPool.getCollectionUsage().getUsed() / max;
      }
   }

   @JMXBeanAttribute(name = "OS: Open File Descriptors (abs)", description = "count of open file descriptors. -1 if not available", sortValue = "p1")
   public long getOpenFileDescriptors() {
      if (fileDescriptorCountAvailable) {
         return (long) retrieveLoadAttribute(FILEDESCRIPTORATTRIBUTE);
      } else {
         return -1;
      }
   }

   @JMXBeanAttribute(name = "OS: Open File Descriptors (%)", description = "open file descriptors as percent of the maximum available. -1 if not available", sortValue = "p2")
   public double getOpenFileDescriptorsPercent() {
      if (fileDescriptorCountAvailable) {
         long max = (long) retrieveLoadAttribute(MAXFILEDESCRIPTORATTRIBUTE);
         long count = (long) retrieveLoadAttribute(FILEDESCRIPTORATTRIBUTE);
         return 100d * count / max;
      } else {
         return -1;
      }
   }

}
