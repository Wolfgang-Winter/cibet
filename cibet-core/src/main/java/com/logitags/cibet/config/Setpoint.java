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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.common.Actuator;
import com.logitags.cibet.control.BooleanAttributedControlValue;
import com.logitags.cibet.control.ConditionControl;
import com.logitags.cibet.control.Control;
import com.logitags.cibet.control.EventControl;
import com.logitags.cibet.control.InvokerControl;
import com.logitags.cibet.control.MethodControl;
import com.logitags.cibet.control.StateChangeControl;
import com.logitags.cibet.control.TargetControl;
import com.logitags.cibet.control.TenantControl;

/**
 * Abstraction of the SetpointBinding class. Facilitates configuration of setpoints through API.
 */
public class Setpoint implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 5492620356502651461L;

   private static Log log = LogFactory.getLog(Setpoint.class);

   private String id;

   private Setpoint _extends;

   private String extendsId;

   private Map<String, Object> controlValues = new HashMap<String, Object>();

   private List<Actuator> actuators = new ArrayList<Actuator>();

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
    * set the given event control values. eventList could be a String, an array of String or a String that contains
    * comma/semicolon separated events. The events replace already registered event values.
    * 
    * @param eventList
    */
   public void setEvent(String... eventList) {
      if (eventList == null) {
         String msg = "failed to set event: NULL value not allowed";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }
      controlValues.remove(EventControl.NAME);
      for (String event : eventList) {
         addControlValue(EventControl.NAME, event);
      }
   }

   /**
    * sets the resolved object for the given value as value for the Control with name controlName. An existing object is
    * replaced.
    * 
    * @param controlName
    * @param value
    */
   public void setCustomControl(String controlName, String value) {
      Control control = getControl(controlName);
      Object obj = control.resolve(value);
      controlValues.put(controlName, obj);
   }

   private void addControlValue(String controlName, String value) {
      if (value == null) {
         String msg = "failed to add " + controlName + " value: NULL value not allowed";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }

      Control control = getControl(controlName);

      List<String> list = (List<String>) controlValues.get(controlName);
      if (list == null) {
         controlValues.put(controlName, control.resolve(value));
      } else {
         controlValues.put(controlName, CollectionUtils.union(list, (List<String>) control.resolve(value)));
      }
   }

   /**
    * set the given invoker control values. inv could be a String, an array of String or a String that contains
    * comma/semicolon separated invokers. The invokers replace already registered invoker values. Default for isExclude
    * is false.
    * 
    * @param inv
    */
   public void setInvoker(String... inv) {
      setInvoker(false, inv);
   }

   /**
    * set the given invoker control values. inv could be a String, an array of String or a String that contains
    * comma/semicolon separated invokers. The invokers replace already registered invoker values.
    * 
    * @param isExclude
    * @param inv
    */
   public void setInvoker(boolean isExclude, String... inv) {
      if (inv == null) {
         String msg = "failed to set invoker: NULL value not allowed";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }
      controlValues.remove(InvokerControl.NAME);
      for (String s : inv) {
         addBooleanAttributedControlValue(InvokerControl.NAME, isExclude, s);
      }
   }

   private void addBooleanAttributedControlValue(String controlName, boolean bool, String value) {
      if (value == null) {
         String msg = "failed to add " + controlName + " value: NULL value not allowed";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }

      Control control = getControl(controlName);

      BooleanAttributedControlValue bav = (BooleanAttributedControlValue) controlValues.get(controlName);
      if (bav == null) {
         bav = new BooleanAttributedControlValue();
         bav.setBooleanValue(bool);
         bav.setValues((List<String>) control.resolve(value));
         controlValues.put(controlName, bav);
      } else {
         bav.setBooleanValue(bool);
         bav.setValues((List<String>) CollectionUtils.union(bav.getValues(), (List<String>) control.resolve(value)));
      }
   }

   /**
    * set the given StateChange control values. chg could be a String, an array of String or a String that contains
    * comma/semicolon separated StateChanges. The StateChanges replace already registered StateChange values. Default
    * for isExclude is false.
    * 
    * @param chg
    */
   public void setStateChange(String... chg) {
      setStateChange(false, chg);
   }

   /**
    * set the given StateChange control values. chg could be a String, an array of String or a String that contains
    * comma/semicolon separated StateChanges. The StateChanges replace already registered StateChange values.
    * 
    * @param isExclude
    * @param chg
    */
   public void setStateChange(boolean isExclude, String... chg) {
      if (chg == null) {
         String msg = "failed to set stateChange: NULL value not allowed";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }
      controlValues.remove(StateChangeControl.NAME);
      for (String s : chg) {
         addBooleanAttributedControlValue(StateChangeControl.NAME, isExclude, s);
      }
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
    * set the given target control values. target could be a String, an array of String or a String that contains
    * comma/semicolon separated targets. The targets replace already registered target values.
    * 
    * @param target
    *           classname
    */
   public void setTarget(String... target) {
      if (target == null) {
         String msg = "failed to set target: NULL value not allowed";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }
      controlValues.remove(TargetControl.NAME);
      for (String s : target) {
         addControlValue(TargetControl.NAME, s);
      }
   }

   /**
    * set the given method control values. methodname could be a String, an array of String or a String that contains
    * comma/semicolon separated methods. The methods replace already registered method values.
    * 
    * @param methodname
    */
   public void setMethod(String... methodname) {
      if (methodname == null) {
         String msg = "failed to set method: NULL value not allowed";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }
      controlValues.remove(MethodControl.NAME);
      for (String s : methodname) {
         addControlValue(MethodControl.NAME, s);
      }
   }

   /**
    * set the given tenant control values. tenant could be a String, an array of String or a String that contains
    * comma/semicolon separated tenants. The tenants replace already registered tenant values.
    * 
    * @param tenant
    */
   public void setTenant(String... tenant) {
      if (tenant == null) {
         String msg = "failed to set tenant: NULL value not allowed";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }
      controlValues.remove(TenantControl.NAME);
      for (String s : tenant) {
         addControlValue(TenantControl.NAME, s);
      }
   }

   /**
    * sets the resolved condition for the given value. An existing condition is replaced.
    * 
    * @param condition
    *           the condition to set
    */
   public void setCondition(String condition) {
      setCustomControl(ConditionControl.NAME, condition);
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
      b.append(", targets: ");
      List<String> targets = (List<String>) controlValues.get(TargetControl.NAME);
      if (targets != null) {
         for (String t : targets) {
            b.append(" ");
            b.append(t);
         }
      }
      b.append(", tenants: ");
      List<String> tenants = (List<String>) controlValues.get(TenantControl.NAME);
      if (tenants != null) {
         for (String t : tenants) {
            b.append(" ");
            b.append(t);
         }
      }
      b.append(", events: ");
      List<String> events = (List<String>) controlValues.get(EventControl.NAME);
      if (events != null) {
         for (String t : events) {
            b.append(" ");
            b.append(t);
         }
      }
      b.append(", condition: ");
      b.append(controlValues.get(ConditionControl.NAME));

      b.append(", invoker: ");
      BooleanAttributedControlValue bav = (BooleanAttributedControlValue) controlValues.get(InvokerControl.NAME);
      if (bav != null) {
         for (String t : bav.getValues()) {
            b.append(" ");
            b.append(t);
         }
         b.append(", exclude invoker: ");
         b.append(bav.isBooleanValue());
      }
      b.append(", methods: ");
      List<String> methods = (List<String>) controlValues.get(MethodControl.NAME);
      if (methods != null) {
         for (String t : methods) {
            b.append(" ");
            b.append(t);
         }
      }
      b.append(", stateChanges: ");
      bav = (BooleanAttributedControlValue) controlValues.get(StateChangeControl.NAME);
      if (bav != null) {
         for (String t : bav.getValues()) {
            b.append(" ");
            b.append(t);
         }
         b.append(", exclude stateChanges: ");
         b.append(bav.isBooleanValue());
      }
      b.append(", actuators: ");
      for (Actuator t : actuators) {
         b.append(" ");
         b.append(t.getName());
      }

      return b.toString();
   }

   /**
    * @return the controlValues
    */
   public Map<String, Object> getControlValues() {
      return controlValues;
   }

   /**
    * @param controlValues
    *           the controlValues to set
    */
   public void setControlValues(Map<String, Object> controlValues) {
      this.controlValues = controlValues;
   }

   public Object getControlValue(String name) {
      return controlValues.get(name);
   }

   public void removeControlValue(String name) {
      controlValues.remove(name);
   }

   /**
    * returns the control values including the inherited ones.
    * 
    */
   public void getEffectiveControlValues(Map<String, Object> map) {
      if (_extends != null) {
         _extends.getEffectiveControlValues(map);
      }
      map.putAll(controlValues);
   }

}
