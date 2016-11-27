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
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * abstract implementation of Control interface that does nothing. Inherit
 * custom Control implementations from this class to hide future interface
 * enhancements.
 */
public abstract class AbstractControl implements Control, Serializable {

   private static Log log = LogFactory.getLog(AbstractControl.class);

   /**
    * 
    */
   private static final long serialVersionUID = -1277349105292516717L;

   public static final String CUSTOMCONTROL_TAGNAME = "customControl";

   /**
    * The default implementation returns a List of String. The configValue is
    * parsed into tokens separated by comma or semicolon. If configValue is null
    * the list is empty.
    * 
    * @see com.logitags.cibet.control.Control#resolve(java.lang.String)
    */
   @Override
   public Object resolve(String configValue) {
      log.debug("resolve " + getName() + " config value: " + configValue);
      List<String> valueList = new ArrayList<String>();
      if (configValue == null) return valueList;
      if (configValue.length() == 0) {
         valueList.add("");
         return valueList;
      }

      try {
         parseValue(valueList, configValue.trim());
      } catch (IllegalArgumentException e) {
         throw new IllegalArgumentException("Unparsable configuration value "
               + configValue + ": " + e.getMessage());
      }
      return valueList;
   }

   /**
    * The default implementation assumes controlValue is a list of String. It
    * returns true if the list is not null and is not empty.
    */
   @Override
   public boolean hasControlValue(Object controlValue) {
      List<String> list = (List<String>) controlValue;
      return list != null && !list.isEmpty() && (list.get(0).length() > 0);
   }

   private void parseValue(List<String> valueList, String configValue) {
      if (configValue.length() == 0) return;
      if (configValue.startsWith("\"")) {
         int index = searchEndQuote(configValue, 1);
         String t = configValue.substring(1, index);
         t = t.replaceAll("\\\\\"", "\"");
         log.debug("parse " + t);
         if (!valueList.contains(t)) {
            valueList.add(t);
         }
         configValue = configValue.substring(index + 1).trim();
         if (configValue.startsWith(",") || configValue.startsWith(";")) {
            configValue = configValue.substring(1).trim();
         }

         parseValue(valueList, configValue);

      } else {
         parseUnquotedValue(valueList, configValue);
      }
   }

   protected void parseUnquotedValue(List<String> valueList, String configValue) {
      StringTokenizer tok = new StringTokenizer(configValue, ",;");
      String t = tok.nextToken().trim();
      log.debug("parse " + t);
      if (t.length() > 0 && !valueList.contains(t)) {
         valueList.add(t);
      }

      configValue = configValue.substring(t.length()).trim();
      if (configValue.startsWith(",") || configValue.startsWith(";")) {
         configValue = configValue.substring(1).trim();
      }
      parseValue(valueList, configValue);
   }

   private int searchEndQuote(String configValue, int start) {
      if (start >= configValue.length()) {
         String err = "no end quote found, end of String reached";
         log.error(err);
         throw new IllegalArgumentException(err);
      }
      int index = configValue.indexOf("\"", start);
      if (index < 0) {
         String err = "no end quote found";
         log.error(err);
         throw new IllegalArgumentException(err);
      }
      char beforeIndex = configValue.charAt(index - 1);
      if (beforeIndex == 92) {
         return searchEndQuote(configValue, index + 1);
      } else {
         return index;
      }
   }

}
