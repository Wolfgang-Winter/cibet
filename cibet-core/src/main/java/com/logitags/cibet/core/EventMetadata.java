package com.logitags.cibet.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.common.Actuator;
import com.logitags.cibet.config.ProxyConfig;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalRequestScope;
import com.logitags.cibet.resource.Resource;

/**
 * A class that transports the Resource data and all metadata through the execution cycle of a sensor. There is one
 * EventMetadata object per event.
 * 
 * @author Wolfgang
 * 
 */
public class EventMetadata implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 1436876984763645342L;

   private static Log log = LogFactory.getLog(EventMetadata.class);

   protected ControlEvent controlEvent;

   /**
    * shall the event be executed?
    */
   private ExecutionStatus executionStatus = ExecutionStatus.EXECUTING;

   private Throwable exception;

   /**
    * unique ID for the case. The case can be a series of events belonging together like 4-eyes request and release.
    */
   private String caseId;

   /**
    * list of setpoints that will be applied on the current business case.
    */
   private List<Setpoint> setpoints = new ArrayList<Setpoint>();

   /**
    * list of actuator instances to be applied on this business process.
    * 
    */
   private Map<String, Actuator> actuators = new LinkedHashMap<String, Actuator>();

   /**
    * any additional properties for this business case that could be used by actuators.
    */
   private Map<String, Object> properties = new HashMap<String, Object>();

   private Resource resource;

   private ProxyConfig proxyConfig;

   public EventMetadata(ControlEvent event, Resource r) {
      controlEvent = event;
      resource = r;
      resolveExecutionStatus();
      resolveCaseId();
   }

   private void resolveExecutionStatus() {
      if (getControlEvent() == null)
         return;

      switch (getControlEvent()) {
      case RELEASE:
      case RELEASE_DELETE:
      case RELEASE_INSERT:
      case RELEASE_INVOKE:
      case RELEASE_UPDATE:
      case RELEASE_SELECT:
      case RESTORE:
      case RESTORE_INSERT:
      case RESTORE_UPDATE:
      case REDO:
         Context.internalRequestScope().removeProperty(InternalRequestScope.CONTROLEVENT);
         break;

      case REJECT:
      case REJECT_DELETE:
      case REJECT_INSERT:
      case REJECT_INVOKE:
      case REJECT_UPDATE:
      case REJECT_SELECT:
         executionStatus = ExecutionStatus.REJECTED;
         // remove here in case of chained controlling
         Context.internalRequestScope().removeProperty(InternalRequestScope.CONTROLEVENT);
         break;

      case PASSBACK:
      case PASSBACK_DELETE:
      case PASSBACK_INSERT:
      case PASSBACK_INVOKE:
      case PASSBACK_SELECT:
      case PASSBACK_UPDATE:
         executionStatus = ExecutionStatus.PASSEDBACK;
         // remove here in case of chained controlling
         Context.internalRequestScope().removeProperty(InternalRequestScope.CONTROLEVENT);
         break;

      case FIRST_RELEASE:
      case FIRST_RELEASE_DELETE:
      case FIRST_RELEASE_INSERT:
      case FIRST_RELEASE_INVOKE:
      case FIRST_RELEASE_UPDATE:
      case FIRST_RELEASE_SELECT:
         executionStatus = ExecutionStatus.POSTPONED;
         Context.internalRequestScope().removeProperty(InternalRequestScope.CONTROLEVENT);
         break;

      default:
         break;
      }
   }

   private void resolveCaseId() {
      String uuid = Context.internalRequestScope().getCaseId();
      if (uuid != null) {
         log.debug("retrieve caseId from CibetContext for EventContext " + uuid);
         caseId = uuid;
         // remove here in case of chained controlling
         // Context.internalRequestScope().removeProperty(InternalRequestScope.CASEID);
      }
   }

   /**
    * @return the controlEvent of the current business case
    */
   public ControlEvent getControlEvent() {
      return controlEvent;
   }

   /**
    * @return the execution status of the current business case
    */
   public ExecutionStatus getExecutionStatus() {
      return executionStatus;
   }

   /**
    * @param status
    *           the execution status to set
    */
   public void setExecutionStatus(ExecutionStatus status) {
      this.executionStatus = status;
   }

   /**
    * @return the caseId. A unique identifier for a sequence of mating business cases on the same resource, for example
    *         a dual control postponed event and the mating release event.
    */
   public String getCaseId() {
      if (caseId == null) {
         caseId = UUID.randomUUID().toString();
      }
      return caseId;
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("controlEvent=");
      b.append(controlEvent);
      b.append(" ; caseId=");
      b.append(caseId);
      b.append(" ; executionStatus=");
      b.append(executionStatus);
      b.append(" ; Resource [");
      b.append(resource);
      b.append("]");
      b.append("\n------> Applied Setpoints: ");
      b.append(getSetpointIds());
      b.append(" ; applied Actuators: ");
      b.append(getActuatorNames());
      return b.toString();
   }

   /**
    * If an exception has been caught before it is thrown here. Evaluates the EventExecution status for logging only.
    * 
    * @throws Exception
    */
   public void evaluateEventExecuteStatus() throws Exception {
      if (log.isDebugEnabled()) {
         if (executionStatus == ExecutionStatus.DENIED) {
            log.debug("event has been intercepted. Access is denied");
         } else if (executionStatus == ExecutionStatus.POSTPONED) {
            log.debug("event has been intercepted. Access is postponed due to dual control event");
         } else if (executionStatus == ExecutionStatus.SCHEDULED) {
            log.debug("event has been intercepted. Access is scheduled due to a scheduler event");
         }
      }

      if (exception != null) {
         if (exception instanceof Exception) {
            throw (Exception) exception;
         } else {
            throw new Exception(exception);
         }
      }
   }

   /**
    * @return an Exception if in the sensor execution an Exception has been caught.
    */
   public Throwable getException() {
      return exception;
   }

   /**
    * set the Exception if in the sensor execution one has been caught. It will be thrown at the end of the sensor
    * execution.
    * 
    * @param ex
    *           the cibetException to set
    */
   public void setException(Throwable ex) {
      this.exception = ex;
   }

   /**
    * @return the resource which is controlled
    */
   public Resource getResource() {
      return resource;
   }

   /**
    * @param resource
    *           the resource which is controlled
    */
   public void setResource(Resource resource) {
      this.resource = resource;
   }

   /**
    * If this EventMetadata is used in the HTTP-CLIENT sensor, the ProxyConfig contains the proxy configuration.
    * Otherwise null.
    * 
    * @return the proxyConfig
    */
   public ProxyConfig getProxyConfig() {
      return proxyConfig;
   }

   /**
    * If this EventMetadata is used in the HTTP-CLIENT sensor, the ProxyConfig contains the proxy configuration.
    * Otherwise null.
    * 
    * @param proxyConfig
    *           the proxyConfig to set
    */
   public void setProxyConfig(ProxyConfig proxyConfig) {
      this.proxyConfig = proxyConfig;
   }

   /**
    * Return list of Actuator instances to be applied on this business process.
    * 
    * @return the Actuators
    */
   public List<Actuator> getActuators() {
      return new ArrayList<Actuator>(actuators.values());
   }

   /**
    * returns the names of the actuator instances that will be applied on this business process, separated by comma.
    * 
    * @return
    */
   public String getActuatorNames() {
      boolean first = true;
      StringBuffer b = new StringBuffer();
      for (String actName : actuators.keySet()) {
         if (!first) {
            b.append(", ");
         } else {
            first = false;
         }
         b.append(actName);
      }

      return b.toString();
   }

   /**
    * Return any additional properties.
    * 
    * @return the properties
    */
   public Map<String, Object> getProperties() {
      return properties;
   }

   /**
    * Set any additional properties.
    * 
    * @param properties
    *           the properties to set
    */
   public void setProperties(Map<String, Object> properties) {
      this.properties = properties;
   }

   /**
    * returns the list of setpoints that will be applied on the current business case.
    * 
    * @return the setpoints
    */
   public List<Setpoint> getSetpoints() {
      return setpoints;
   }

   /**
    * adds a setpoint and puts the actuators of this setpoint into the actuator list of this class.
    * 
    * @param sp
    */
   public void addSetpoint(Setpoint sp) {
      setpoints.add(sp);
      for (Actuator act : sp.getActuators()) {
         if (actuators.containsKey(act.getName())) {
            log.warn("Actuator " + act.getName() + " configured in Setpoint " + sp.getId()
                  + " is already configured from another Setpoint for this business case. It will be applied one time only!");
         }
         actuators.put(act.getName(), act);
      }
   }

   /**
    * returns the setpoint ids separated by comma
    * 
    * @return
    */
   public String getSetpointIds() {
      boolean first = true;
      StringBuffer b = new StringBuffer();
      for (Setpoint sp : setpoints) {
         if (first) {
            first = false;
         } else {
            b.append(", ");
         }
         b.append(sp.getId());
      }
      return b.toString();
   }

}
