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
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.http.HttpRequestResource;
import com.logitags.cibet.sensor.pojo.MethodResource;

/**
 * evaluates the given method against configured setpoints. Accepts the following patterns as method name:
 * <p>
 * concrete method without parameters: doSomething()
 * <p>
 * method with parameters: doSomething(String, com.logitags.cibet.TEntity, int)
 * <p>
 * all overloaded methods: doSomething
 * <p>
 * all methods: *
 */
public class MethodControl implements Serializable, Control {

   /**
    * 
    */
   private static final long serialVersionUID = -6583802959373661240L;

   private static Log log = LogFactory.getLog(MethodControl.class);

   public static final String NAME = "method";

   @Override
   public String getName() {
      return NAME;
   }

   @Override
   public Boolean evaluate(Set<String> values, EventMetadata metadata) {
      if (metadata == null) {
         String msg = "failed to execute method evaluation: metadata is null";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }

      if (values == null || values.isEmpty()) return null;

      String method;
      if (metadata.getResource() instanceof MethodResource) {
         method = ((MethodResource) metadata.getResource()).getMethod();
      } else if (metadata.getResource() instanceof HttpRequestResource) {
         method = ((HttpRequestResource) metadata.getResource()).getMethod();

      } else {
         log.info("skip method evaluation. Not applicable for " + metadata.getResource().getClass().getSimpleName());
         return null;
      }

      if (method == null) {
         log.info("skip method evaluation: no method name given");
         return null;
      }

      for (String methodName : values) {
         if (methodName.length() == 0 || "*".equals(methodName)) {
            // all methods
            return true;

         } else if (method.equals(methodName)) {
            // all overloaded methods
            return true;

         } else if (methodName.endsWith("*")) {
            // method wildcard
            String pack = methodName.substring(0, methodName.length() - 1);
            if (method.startsWith(pack)) {
               return true;
            }

         } else if (methodName.endsWith("()")) {
            if (method.equals(methodName.substring(0, methodName.length() - 2))
                  && (metadata.getResource().getParameters() == null
                        || metadata.getResource().getParameters().isEmpty())) {
               // concrete method without parameters
               return true;
            }

         } else if (methodName.endsWith(")")) {
            // concrete method with parameters
            int index = methodName.indexOf("(");
            if (index < 0) {
               String msg = "failed to execute method evaluation: " + "Setpoint method name '" + methodName
                     + "' is invalid.";
               log.error(msg);
               throw new RuntimeException(msg);
            }
            if (!method.equals(methodName.substring(0, index))) continue;
            // check parameters
            StringTokenizer tok = new StringTokenizer(methodName.substring(index + 1, methodName.length() - 1), ",");
            boolean isEqual = true;
            for (ResourceParameter param : metadata.getResource().getParameters()) {
               if (param.getParameterType() != ParameterType.METHOD_PARAMETER) continue;
               if (!tok.hasMoreElements()) {
                  isEqual = false;
                  break;
               }

               String intClassName = internalClassNameForName(tok.nextToken().trim());
               log.debug("intClassName: " + intClassName + ", param.getUnencodedValue().getClass().getCanonicalName(): "
                     + param.getUnencodedValue().getClass().getCanonicalName());
               if (!param.getUnencodedValue().getClass().getCanonicalName().equals(intClassName)) {
                  isEqual = false;
                  break;
               }
            }
            if (tok.hasMoreElements()) {
               isEqual = false;
            }
            if (isEqual) {
               return true;
            }
         }
      }

      return false;
   }

   private String internalClassNameForName(String classname) {
      if (classname == null) {
         return null;
      } else if (classname.equals("String")) {
         return "java.lang.String";
      } else if (classname.equals("Date")) {
         return "java.util.Date";
      } else if (classname.equals("byte")) {
         return "java.lang.Byte";
      } else if (classname.equals("boolean")) {
         return "java.lang.Boolean";
      } else if (classname.equals("char")) {
         return "java.lang.Character";
      } else if (classname.equals("double")) {
         return "java.lang.Double";
      } else if (classname.equals("float")) {
         return "java.lang.Float";
      } else if (classname.equals("int")) {
         return "java.lang.Integer";
      } else if (classname.equals("long")) {
         return "java.lang.Long";
      } else if (classname.equals("short")) {
         return "java.lang.Short";
      }

      Matcher m = CibetUtil.classNamePattern.matcher(classname);
      if (m.matches()) {
         log.debug("matches array class");
         Class<?> componentClass = CibetUtil.arrayClassForName(classname);

         StringBuffer b = new StringBuffer();
         b.append(internalClassNameForName(componentClass.getName()));
         for (int i = 0; i < m.group(1).length(); i++) {
            b.append("[]");
         }
         return b.toString();
      }

      return classname;
   }

}
