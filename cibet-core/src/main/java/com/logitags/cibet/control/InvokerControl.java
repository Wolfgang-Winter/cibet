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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
public class InvokerControl implements Serializable, Control {

   /**
    * 
    */
   private static final long serialVersionUID = 2188075469227544287L;

   private static Log log = LogFactory.getLog(InvokerControl.class);

   public static final String NAME = "invoker";

   private static final String STRING_INVOKER_KEY = "__STRING_INVOKER";

   private boolean isConstrained(Set<String> constraints, StackTraceElement[] traces) {
      for (String clude : constraints) {
         Set<String> parsedConstraints = parse(clude);
         for (String parsedConstraint : parsedConstraints) {
            for (StackTraceElement trace : traces) {
               if (log.isDebugEnabled()) {
                  log.debug(trace.getClassName() + "." + trace.getMethodName());
               }

               if (parsedConstraint.equals(trace.getClassName())) {
                  // all methods of a class
                  return true;

               } else if (parsedConstraint.endsWith("*")) {
                  // all methods of all classes of a package
                  String pack = parsedConstraint.substring(0, parsedConstraint.length() - 1);
                  if (trace.getClassName().startsWith(pack)) {
                     return true;
                  }

               } else {
                  // a method of a class
                  int index = parsedConstraint.lastIndexOf(".");
                  if (index < 0) {
                     if (parsedConstraint.equals(trace.getMethodName())) {
                        // a method
                        return true;
                     } else {
                        continue;
                     }
                  }
                  String classname = parsedConstraint.substring(0, index);
                  String methodname = parsedConstraint.substring(index + 1);
                  if (methodname.endsWith("()")) {
                     methodname = methodname.substring(0, methodname.length() - 2);
                  }
                  if (classname.equals(trace.getClassName()) && methodname.equals(trace.getMethodName())) {
                     return true;
                  }
               }
            }
         }
      }
      return false;
   }

   private Set<String> parse(String configValue) {
      Set<String> valueList = new HashSet<>();
      if (configValue == null || configValue.length() == 0) return valueList;
      StringTokenizer tok = new StringTokenizer(configValue, ",;");
      while (tok.hasMoreTokens()) {
         String t = tok.nextToken().trim();
         valueList.add(t);
      }
      return valueList;
   }

   private boolean isConstrained(Set<String> constraints, List<String> ips) {
      for (String clude : constraints) {
         Set<String> parsedConstraints = parse(clude);
         for (String parsedConstraint : parsedConstraints) {
            for (String ip : ips) {
               if (parsedConstraint.equals(ip)) {
                  return true;

               } else if (parsedConstraint.endsWith("*")) {
                  String pack = parsedConstraint.substring(0, parsedConstraint.length() - 1);
                  if (ip.startsWith(pack)) {
                     return true;
                  }
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

   @Override
   public Boolean evaluate(Set<String> values, EventMetadata metadata) {
      if (metadata == null) {
         String msg = "failed to execute invoker evaluation: metadata is null";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }

      if (values == null || values.isEmpty()) return null;

      Object invoker = metadata.getResource().getInvoker();
      if (invoker instanceof StackTraceElement[]) {
         return isConstrained(values, (StackTraceElement[]) invoker);
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

         return isConstrained(values, ips);
      } else {
         String msg = "failed to execute Invoker evaluation: metaData.invoker class "
               + (invoker == null ? "null" : invoker.getClass().getName()) + " is unknown";
         log.error(msg);
         throw new RuntimeException(msg);
      }
   }

}
