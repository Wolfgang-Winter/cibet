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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.core.EventMetadata;

/**
 * evaluates Controls
 */
public abstract class Controller {

   private static Log log = LogFactory.getLog(Controller.class);

   /**
    * Evaluates the business case and detects setpoints and actuators to apply.
    * 
    * @param metadata
    */
   public static void evaluate(EventMetadata metadata) {
      if (metadata == null) {
         String msg = "failed to evaluate setpoints: metadata is null";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }

      if (metadata.getControlEvent() == null) {
         log.debug("event is not controlled by Cibet");
         return;
      }

      Configuration cman = Configuration.instance();

      for (Setpoint sp : cman.getSetpoints()) {
         boolean matches = true;

         for (ConcreteControl cc : sp.getEffectiveControls()) {
            // if (!cc.getIncludes().isEmpty()) {
            Boolean result = cc.getControl().evaluate(cc.getIncludes(), metadata);
            if (result == Boolean.FALSE) {
               matches = false;
               if (log.isDebugEnabled()) {
                  log.debug("Setpoint " + sp.getId() + " --> " + cc.getControl().getName() + " (includes) fails!");
               }
               break;
            }
            // }

            // if (!cc.getExcludes().isEmpty()) {
            result = cc.getControl().evaluate(cc.getExcludes(), metadata);
            if (result == Boolean.TRUE) {
               matches = false;
               if (log.isDebugEnabled()) {
                  log.debug("Setpoint " + sp.getId() + " --> " + cc.getControl().getName() + " (excludes) fails!");
               }
               break;
            }
            // }
         }

         if (matches) {
            metadata.addSetpoint(sp);
         }
      }

      if (log.isInfoEnabled()) {
         log.info(metadata);
      }
   }

   /**
    * The default implementation returns a List of String. The configValue is parsed into tokens separated by comma or
    * semicolon. If configValue is null the list is empty.
    * 
    */
   public static String resolve(String configValue) {
      if (configValue == null) return null;
      if (configValue.startsWith("file:")) {
         // 1. load as URI
         try {
            String uriFilename = configValue.replace('\\', '/');
            URI uri = new URI(uriFilename);
            URL url = uri.toURL();
            if (url != null) {
               String script = loadFromURL(url, uriFilename);
               if (script != null) {
                  return resolve(script);
               }
            }

         } catch (MalformedURLException e) {
            log.warn(e.getMessage());
         } catch (URISyntaxException e) {
            log.warn(e.getMessage());
         }

         // 2. load from classpath
         String cpFilename = configValue.substring(5);
         ClassLoader cloader = Thread.currentThread().getContextClassLoader();
         URL url = cloader.getResource(cpFilename);
         if (url == null) {
            String msg = "Failed to load script file " + configValue + " as URI or from classpath. File not found";
            log.error(msg);
            throw new IllegalArgumentException(msg);
         }
         String script = loadFromURL(url, cpFilename);
         return resolve(script);

      } else {
         return configValue.trim();
      }
   }

   private static String loadFromURL(URL url, String filename) {
      try {
         StringBuffer b = new StringBuffer();
         BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
         int ch = br.read();
         while (ch != -1) {
            b.append((char) ch);
            ch = br.read();
         }
         br.close();

         if (log.isDebugEnabled()) {
            log.debug("load script from " + filename + ":\n" + b.toString());
         }
         return b.toString();
      } catch (FileNotFoundException e) {
         log.warn(e.getMessage());
         return null;
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new RuntimeException(e);
      }
   }

}
