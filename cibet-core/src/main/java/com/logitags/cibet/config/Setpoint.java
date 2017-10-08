/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 *******************************************************************************
 */
/**
 * 
 */
package com.logitags.cibet.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.common.Actuator;
import com.logitags.cibet.control.ConcreteControl;
import com.logitags.cibet.control.ConcreteControlComparator;
import com.logitags.cibet.control.ConditionControl;
import com.logitags.cibet.control.Control;
import com.logitags.cibet.control.EventControl;
import com.logitags.cibet.control.InvokerControl;
import com.logitags.cibet.control.MethodControl;
import com.logitags.cibet.control.StateChangeControl;
import com.logitags.cibet.control.TargetControl;
import com.logitags.cibet.control.TenantControl;
import com.logitags.cibet.core.ControlEvent;

/**
 * Abstraction of the SetpointBinding class. Facilitates configuration of setpoints through API.
 */
public class Setpoint implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 5492620356502651461L;

   private static Log log = LogFactory.getLog(Setpoint.class);

   public static final String CODE_CONFIGNAME = "code";

   private String id;

   private Setpoint _extends;

   private String extendsId;

   /**
    * the config file name where this setpoint is defined. If defined in code defaults to code
    */
   private String configName = CODE_CONFIGNAME;

   private Map<String, ConcreteControl> controls = new HashMap<>();

   private List<Actuator> actuators = new ArrayList<>();

   public Setpoint(String id, Setpoint parent) {
      this(id);
      this._extends = parent;
      if (parent != null) {
         this.extendsId = parent.getId();
      } else {
         this.extendsId = null;
      }
   }

   public Setpoint(String id) {
      if (id == null || id.length() == 0) {
         throw new IllegalArgumentException("Failed to create Setpoint: Id is null or has zero length");
      }
      this.id = id;
   }

   public Setpoint(String id, String configName) {
      this(id);
      if (configName == null || configName.length() == 0) {
         throw new IllegalArgumentException("Failed to create Setpoint: configName is null or has zero length");
      }
      this.configName = configName;
   }

   private Control getControl(String name) {
      Control control = Configuration.instance().getControl(name);
      if (control == null) {
         String err = "No Control registered with name " + name;
         log.error(err);
         throw new RuntimeException(err);
      }
      return control;
   }

   /**
    * removes all registered Control values for inclusion
    */
   public Setpoint removeCustomControlIncludes(String controlName) {
      ConcreteControl cc = controls.get(controlName);
      if (cc != null) {
         cc.getIncludes().clear();
      }
      return this;
   }

   /**
    * removes all registered Control values for exclusion
    */
   public Setpoint removeCustomControlExcludes(String controlName) {
      ConcreteControl cc = controls.get(controlName);
      if (cc != null) {
         cc.getExcludes().clear();
      }
      return this;
   }

   /**
    * removes all registered ControlEvents for inclusion
    */
   public Setpoint removeEventIncludes() {
      return removeCustomControlIncludes(EventControl.NAME);
   }

   /**
    * removes all registered ControlEvents for exclusion
    */
   public Setpoint removeEventExcludes() {
      return removeCustomControlExcludes(EventControl.NAME);
   }

   /**
    * remove all include values of invoker control.
    * 
    * @return
    */
   public Setpoint removeInvokerIncludes() {
      return removeCustomControlIncludes(InvokerControl.NAME);
   }

   /**
    * remove all exclude values of invoker control.
    * 
    * @return
    */
   public Setpoint removeInvokerExcludes() {
      return removeCustomControlExcludes(InvokerControl.NAME);
   }

   /**
    * remove all include values of condition control.
    * 
    * @return
    */
   public Setpoint removeConditionIncludes() {
      return removeCustomControlIncludes(ConditionControl.NAME);
   }

   /**
    * remove all exclude values of Condition control.
    * 
    * @return
    */
   public Setpoint removeConditionExcludes() {
      return removeCustomControlExcludes(ConditionControl.NAME);
   }

   /**
    * remove all include values of Method control.
    * 
    * @return
    */
   public Setpoint removeMethodIncludes() {
      return removeCustomControlIncludes(MethodControl.NAME);
   }

   /**
    * remove all exclude values of Method control.
    * 
    * @return
    */
   public Setpoint removeMethodExcludes() {
      return removeCustomControlExcludes(MethodControl.NAME);
   }

   /**
    * remove all include values of StateChange control.
    * 
    * @return
    */
   public Setpoint removeStateChangeIncludes() {
      return removeCustomControlIncludes(StateChangeControl.NAME);
   }

   /**
    * remove all exclude values of StateChange control.
    * 
    * @return
    */
   public Setpoint removeStateChangeExcludes() {
      return removeCustomControlExcludes(StateChangeControl.NAME);
   }

   /**
    * remove all include values of Target control.
    * 
    * @return
    */
   public Setpoint removeTargetIncludes() {
      return removeCustomControlIncludes(TargetControl.NAME);
   }

   /**
    * remove all exclude values of Target control.
    * 
    * @return
    */
   public Setpoint removeTargetExcludes() {
      return removeCustomControlExcludes(TargetControl.NAME);
   }

   /**
    * remove all include values of Tenant control.
    * 
    * @return
    */
   public Setpoint removeTenantIncludes() {
      return removeCustomControlIncludes(TenantControl.NAME);
   }

   /**
    * remove all exclude values of Tenant control.
    * 
    * @return
    */
   public Setpoint removeTenantExcludes() {
      return removeCustomControlExcludes(TenantControl.NAME);
   }

   /**
    * adds the given event control values for inclusion.
    * 
    * @param events
    */
   public Setpoint addEventIncludes(ControlEvent... events) {
      if (events == null) {
         String msg = "failed to set event: NULL value not allowed";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }
      ConcreteControl cc = controls.get(EventControl.NAME);
      if (cc == null) {
         cc = new ConcreteControl(getControl(EventControl.NAME));
         controls.put(EventControl.NAME, cc);
      }
      for (ControlEvent event : events) {
         if (event != null) cc.getIncludes().add(event.name());
      }
      return this;
   }

   /**
    * adds the given event control values for exclusion.
    * 
    * @param events
    */
   public Setpoint addEventExcludes(ControlEvent... events) {
      if (events == null) {
         String msg = "failed to set event: NULL value not allowed";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }
      ConcreteControl cc = controls.get(EventControl.NAME);
      if (cc == null) {
         cc = new ConcreteControl(getControl(EventControl.NAME));
         controls.put(EventControl.NAME, cc);
      }
      for (ControlEvent event : events) {
         if (event != null) cc.getExcludes().add(event.name());
      }
      return this;
   }

   /**
    * adds the given values to a custom control control for inclusion.
    * 
    * @param events
    */
   public Setpoint addCustomControlIncludes(String controlName, String... values) {
      if (controlName == null || values == null) {
         String msg = "failed to set control values: NULL method parameter not allowed";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }
      ConcreteControl cc = controls.get(controlName);
      if (cc == null) {
         cc = new ConcreteControl(getControl(controlName));
         controls.put(controlName, cc);
      }
      for (String value : values) {
         if (value != null) cc.getIncludes().add(value);
      }
      return this;
   }

   /**
    * adds the given values to a custom control for exclusion.
    * 
    * @param events
    */
   public Setpoint addCustomControlExcludes(String controlName, String... values) {
      if (controlName == null || values == null) {
         String msg = "failed to set control values: NULL method parameter not allowed";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }
      ConcreteControl cc = controls.get(controlName);
      if (cc == null) {
         cc = new ConcreteControl(getControl(controlName));
         controls.put(controlName, cc);
      }
      for (String value : values) {
         if (value != null) cc.getExcludes().add(value);
      }
      return this;
   }

   /**
    * add the invoker control include values
    * 
    * @param values
    * @return
    */
   public Setpoint addInvokerIncludes(String... values) {
      return addCustomControlIncludes(InvokerControl.NAME, values);
   }

   /**
    * add the invoker control exclude values
    * 
    * @param values
    * @return
    */
   public Setpoint addInvokerExcludes(String... values) {
      return addCustomControlExcludes(InvokerControl.NAME, values);
   }

   /**
    * add the Condition control include values
    * 
    * @param values
    * @return
    */
   public Setpoint addConditionIncludes(String... values) {
      return addCustomControlIncludes(ConditionControl.NAME, values);
   }

   /**
    * add the Condition control exclude values
    * 
    * @param values
    * @return
    */
   public Setpoint addConditionExcludes(String... values) {
      return addCustomControlExcludes(ConditionControl.NAME, values);
   }

   /**
    * add the Method control include values
    * 
    * @param values
    * @return
    */
   public Setpoint addMethodIncludes(String... values) {
      return addCustomControlIncludes(MethodControl.NAME, values);
   }

   /**
    * add the Method control exclude values
    * 
    * @param values
    * @return
    */
   public Setpoint addMethodExcludes(String... values) {
      return addCustomControlExcludes(MethodControl.NAME, values);
   }

   /**
    * add the StateChange control include values
    * 
    * @param values
    * @return
    */
   public Setpoint addStateChangeIncludes(String... values) {
      return addCustomControlIncludes(StateChangeControl.NAME, values);
   }

   /**
    * add the StateChange control exclude values
    * 
    * @param values
    * @return
    */
   public Setpoint addStateChangeExcludes(String... values) {
      return addCustomControlExcludes(StateChangeControl.NAME, values);
   }

   /**
    * add the Target control include values
    * 
    * @param values
    * @return
    */
   public Setpoint addTargetIncludes(String... values) {
      return addCustomControlIncludes(TargetControl.NAME, values);
   }

   /**
    * add the Target control exclude values
    * 
    * @param values
    * @return
    */
   public Setpoint addTargetExcludes(String... values) {
      return addCustomControlExcludes(TargetControl.NAME, values);
   }

   /**
    * add the Tenant control include values
    * 
    * @param values
    * @return
    */
   public Setpoint addTenantIncludes(String... values) {
      return addCustomControlIncludes(TenantControl.NAME, values);
   }

   /**
    * add the Tenant control exclude values
    * 
    * @param values
    * @return
    */
   public Setpoint addTenantExcludes(String... values) {
      return addCustomControlExcludes(TenantControl.NAME, values);
   }

   /**
    * code registered actuators overwrite configuration
    */
   public List<Actuator> getActuators() {
      return actuators;
   }

   /**
    * 
    * 
    * @see com.logitags.cibet.bindings.SetpointBinding#getId()
    */
   public String getId() {
      return id;
   }

   public String getCombinedId() {
      return configName + "/" + id;
   }

   public void addActuator(Actuator... actuator) {
      if (actuator == null) {
         String msg = "failed to add actuator: NULL value not allowed";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }
      for (Actuator a : actuator) {
         if (a == null) {
            String msg = "failed to add actuator: NULL value not allowed";
            log.error(msg);
            throw new IllegalArgumentException(msg);
         }
         actuators.add(a);
      }
   }

   public boolean removeActuator(Actuator actuator) {
      return actuators.remove(actuator);
   }

   /**
    * @return the _extends
    */
   public Setpoint getExtends() {
      return _extends;
   }

   /**
    * @param _extends
    *           the _extends to set
    */
   public void setExtends(Setpoint _extends) {
      this._extends = _extends;
   }

   /**
    * @return the extendsId
    */
   public String getExtendsId() {
      return extendsId;
   }

   /**
    * 
    * @param id
    */
   public void setExtendsId(String id) {
      extendsId = id;
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("Setpoint ID: ");
      b.append(id);
      b.append(", extends: ");
      b.append(extendsId);

      for (ConcreteControl cc : controls.values()) {
         b.append(", ");
         b.append(cc);
      }

      b.append(", actuators: ");
      for (Actuator t : actuators) {
         b.append(" ");
         b.append(t.getName());
      }

      return b.toString();
   }

   /**
    * @return the controls
    */
   public Map<String, ConcreteControl> getControls() {
      return controls;
   }

   /**
    * @param controls
    *           the controls to set
    */
   public void setControls(Map<String, ConcreteControl> controls) {
      this.controls = controls;
   }

   /**
    * returns the control values including the inherited ones.
    * 
    */
   public Set<ConcreteControl> getEffectiveControls() {
      Set<ConcreteControl> effControls = new TreeSet<ConcreteControl>(
            new ConcreteControlComparator(Configuration.instance().getControlNames()));

      effControls.addAll(controls.values());

      if (_extends != null) {
         effControls.addAll(_extends.getEffectiveControls());
      }

      return effControls;
   }

   /**
    * @return the configName
    */
   public String getConfigName() {
      return configName;
   }

}
