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

import java.util.List;
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
public class MethodControl extends AbstractControl {

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
   protected void parseUnquotedValue(List<String> valueList, String configValue) {
      log.debug("resolve method config value: " + configValue);
      StringTokenizer tok = new StringTokenizer(configValue, ",;");
      while (tok.hasMoreTokens()) {
         String t = tok.nextToken().trim();
         if (t.indexOf("(") > -1) {
            StringBuffer b = new StringBuffer(t);
            while (!b.toString().endsWith(")")) {
               if (!tok.hasMoreTokens()) {
                  String msg = "Failed to parse method control value " + configValue;
                  log.fatal(msg);
                  throw new RuntimeException(msg);
               }
               b.append(", ");
               b.append(tok.nextToken().trim());
            }
            t = b.toString();
         }

         if (!valueList.contains(t)) {
            valueList.add(t);
         }
      }
   }

   @Override
   public boolean evaluate(Object controlValue, EventMetadata metadata) {
      String method;
      if (metadata.getResource() instanceof MethodResource) {
         method = ((MethodResource) metadata.getResource()).getMethod();
      } else if (metadata.getResource() instanceof HttpRequestResource) {
         method = ((HttpRequestResource) metadata.getResource()).getMethod();

      } else {
         log.info("skip method evaluation. Not applicable for " + metadata.getResource().getClass().getSimpleName());
         return true;
      }

      if (method == null) {
         log.info("skip method evaluation: no method name given");
         return true;
      }

      List<String> list = (List<String>) controlValue;

      for (String spMethodName : list) {
         if (spMethodName.length() == 0 || "*".equals(spMethodName)) {
            // all methods
            return true;

         } else if (method.equals(spMethodName)) {
            // all overloaded methods
            return true;

         } else if (spMethodName.endsWith("*")) {
            // method wildcard
            String pack = spMethodName.substring(0, spMethodName.length() - 1);
            if (method.startsWith(pack)) {
               return true;
            }

         } else if (spMethodName.endsWith("()")) {
            if (method.equals(spMethodName.substring(0, spMethodName.length() - 2))
                  && (metadata.getResource().getParameters() == null
                        || metadata.getResource().getParameters().isEmpty())) {
               // concrete method without parameters
               return true;
            }

         } else if (spMethodName.endsWith(")")) {
            // concrete method with parameters
            int index = spMethodName.indexOf("(");
            if (index < 0) {
               String msg = "failed to execute method evaluation: " + "Setpoint method name '" + spMethodName
                     + "' is invalid.";
               log.error(msg);
               throw new RuntimeException(msg);
            }
            if (!method.equals(spMethodName.substring(0, index)))
               continue;
            // check parameters
            StringTokenizer tok = new StringTokenizer(spMethodName.substring(index + 1, spMethodName.length() - 1),
                  ",");
            // if (tok.countTokens() != metadata.getResource().getParameters().size()) continue;
            boolean isEqual = true;
            for (ResourceParameter param : metadata.getResource().getParameters()) {
               if (param.getParameterType() != ParameterType.METHOD_PARAMETER)
                  continue;
               if (!tok.hasMoreElements()) {
                  isEqual = false;
                  break;
               }

               String intClassName = internalClassNameForName(tok.nextToken().trim());
               log.debug("intClassName:" + intClassName);
               log.debug("param.getUnencodedValue().getClass().getCanonicalName(): "
                     + param.getUnencodedValue().getClass().getCanonicalName());
               if (!param.getUnencodedValue().getClass().getCanonicalName().equals(intClassName)) {
                  isEqual = false;
                  break;
               }
            }
            if (tok.hasMoreElements()) {
               return false;
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
