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
package com.logitags.cibet.control;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.core.EventMetadata;

/**
 * evaluates direct and indirect invoker of the current thread against configured setpoints. If one of the invokers
 * listed in excludeInvoker tag is detected, this setpoint is skipped. If one of the invokers listed in includeInvoker
 * tag is detected, this setpoint is executed. Attributes in excludeInvoker and includeInvoker tags are comma separated.
 * Invokers can be defined as follows:
 * <p>
 * all methods of concrete class: com.logitags.cibet.TEntity
 * <p>
 * all methods of all classes of the root package: *
 * <p>
 * all methods of all classes of a package: com.logitags.cibet.*
 * <p>
 * all methods of all classes of a package including subpackages: com.logitags.cibet.**
 * <p>
 * concrete method of concrete class (all overloaded methods): com.logitags.cibet.TEntity.doSomething()
 * <p>
 * excludeInvoker and includeInvoker tags in class constraint and method constraint elements are evaluated.
 * 
 */
public class InvokerControl extends AbstractControl {

   /**
    * 
    */
   private static final long serialVersionUID = 2188075469227544287L;

   private static Log log = LogFactory.getLog(InvokerControl.class);

   public static final String NAME = "invoker";

   private static final String STRING_INVOKER_KEY = "__STRING_INVOKER";

   private boolean isConstrained(List<String> constraints, StackTraceElement[] traces) {
      for (String clude : constraints) {
         for (StackTraceElement trace : traces) {
            if (clude.equals(trace.getClassName())) {
               // all methods of a class
               return true;

            } else if (clude.endsWith("*")) {
               // all methods of all classes of a package
               String pack = clude.substring(0, clude.length() - 1);
               if (trace.getClassName().startsWith(pack)) {
                  return true;
               }

            } else {
               // a method of a class
               int index = clude.lastIndexOf(".");
               if (index < 0) {
                  String msg = "failed to execute Invoker evaluation: " + "includeInvoker/excludeInvoker tag value "
                        + clude + " is invalid";
                  log.error(msg);
                  throw new RuntimeException(msg);
               }
               String classname = clude.substring(0, index);
               String methodname = clude.substring(index + 1, clude.length() - 2);
               if (methodname.endsWith("()")) {
                  methodname = methodname.substring(0, methodname.length() - 2);
               }
               if (classname.equals(trace.getClassName()) && methodname.equals(trace.getMethodName())) {
                  return true;
               }
            }
         }
      }
      return false;
   }

   private boolean isConstrained(List<String> constraints, List<String> ips) {
      for (String clude : constraints) {
         for (String ip : ips) {
            if (clude.equals(ip)) {
               return true;

            } else if (clude.endsWith("*")) {
               String pack = clude.substring(0, clude.length() - 1);
               if (ip.startsWith(pack)) {
                  return true;
               }
            }
         }
      }
      return false;
   }

   @Override
   public String getName() {
      return NAME;
   }

   private boolean evaluateStringInvoker(BooleanAttributedControlValue invokerValue, List<String> ips) {
      if (invokerValue.isBooleanValue()) {
         if (!isConstrained(invokerValue.getValues(), ips)) {
            return true;
         }
      } else {
         if (isConstrained(invokerValue.getValues(), ips)) {
            return true;
         }
      }

      return false;
   }

   private boolean evaluateTraceInvokers(BooleanAttributedControlValue controlValue, StackTraceElement[] traces) {
      if (log.isDebugEnabled()) {
         for (StackTraceElement trace : traces) {
            log.debug(trace.toString());
         }
      }

      if (controlValue.isBooleanValue()) {
         if (!isConstrained(controlValue.getValues(), traces)) {
            return true;
         }
      } else {
         if (isConstrained(controlValue.getValues(), traces)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean hasControlValue(Object cv) {
      BooleanAttributedControlValue value = (BooleanAttributedControlValue) cv;
      if (value == null || value.getValues().isEmpty()
            || (value.getValues().size() == 1 && value.getValues().get(0).length() == 0)) {
         return false;
      } else {
         return true;
      }
   }

   @Override
   public boolean evaluate(Object controlValue, EventMetadata metadata) {
      BooleanAttributedControlValue invokerValue = (BooleanAttributedControlValue) controlValue;

      Object invoker = metadata.getResource().getInvoker();
      if (invoker instanceof StackTraceElement[]) {
         return evaluateTraceInvokers(invokerValue, (StackTraceElement[]) invoker);
      } else if (invoker instanceof String) {
         List<String> ips = (List<String>) metadata.getProperties().get(STRING_INVOKER_KEY);
         if (ips == null) {
            ips = new ArrayList<String>();
            StringTokenizer tok = new StringTokenizer((String) invoker, ",");
            while (tok.hasMoreTokens()) {
               ips.add(tok.nextToken().trim());
            }
            metadata.getProperties().put(STRING_INVOKER_KEY, ips);
         }

         return evaluateStringInvoker(invokerValue, ips);
      } else {
         String msg = "failed to execute Invoker evaluation: metaData.invoker class "
               + (invoker == null ? "null" : invoker.getClass().getName()) + " is unknown";
         log.error(msg);
         throw new RuntimeException(msg);
      }
   }

}
