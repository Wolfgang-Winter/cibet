package com.logitags.cibet.actuator.loadcontrol;

import java.io.Serializable;
import java.lang.reflect.Field;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * abstract super class of all Monitor implementations.
 * 
 * @author User
 *
 */
public abstract class AbstractMonitor implements Monitor, Serializable {

   private static Log log = LogFactory.getLog(AbstractMonitor.class);

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   protected static final long LEARNING_PHASE = 20;

   protected MonitorStatus status = MonitorStatus.OFF;

   /**
    * returns the simple class name of the Monitor.
    */
   @Override
   public String getName() {
      return this.getClass().getSimpleName();
   }

   /**
    * empty method.
    */
   @Override
   public void close() {
   }

   /**
    * returns the Monitor status
    */
   @Override
   public MonitorStatus getStatus() {
      return status;
   }

   /**
    * sets the Monitor status.
    */
   @Override
   public void setStatus(MonitorStatus mode) {
      if (status == MonitorStatus.NOT_SUPPORTED) return;
      if (mode == MonitorStatus.NOT_SUPPORTED) mode = MonitorStatus.OFF;
      log.info("Set " + getName() + ".status to " + mode);
      status = mode;
   }

   /**
    * resolves the value to either a percent or absolute value. Sets the value either to a field valueNamePercent or
    * valueNameAbs by reflection.
    * 
    * @param monitor
    *           the object where to set the resolved value.
    * @param valueName
    *           field name of the String value
    * @param value
    *           value as String
    */
   protected void resolveValue(Monitor monitor, String valueName, String value) {
      if (status == MonitorStatus.NOT_SUPPORTED) return;

      try {
         Field setValueField = monitor.getClass().getDeclaredField(valueName);
         setValueField.setAccessible(true);
         Field setValuePercentageField = monitor.getClass().getDeclaredField(valueName + "Percent");
         setValuePercentageField.setAccessible(true);
         Field setValueMsField = monitor.getClass().getDeclaredField(valueName + "Abs");
         setValueMsField.setAccessible(true);

         if (value == null) {
            setValueField.set(monitor, (String) null);
            setValuePercentageField.set(monitor, -1);
            setValueMsField.set(monitor, -1);
         } else if (value.trim().endsWith("%")) {
            value = value.trim();
            Number d = null;
            d = Double.valueOf(value.substring(0, value.length() - 1)) / 100;
            if (d.doubleValue() < 0 || d.doubleValue() > 100) {
               throw new IllegalArgumentException("value for " + monitor.getClass().getSimpleName() + "." + valueName
                     + " must be between 0 and 100%");
            }

            setValueField.set(monitor, value);
            setValueMsField.set(monitor, -1);
            setValuePercentageField.set(monitor, d);

         } else {
            value = value.trim();
            Number d = null;
            d = Double.valueOf(value);
            setValueMsField.set(monitor, d);
            setValuePercentageField.set(monitor, -1);
            setValueField.set(monitor, value);
         }
         log.info("Set " + monitor.getClass().getSimpleName() + "." + valueName + " to " + value);
      } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
         log.error(e.getMessage(), e);
         throw new RuntimeException(e);
      }
   }

}
