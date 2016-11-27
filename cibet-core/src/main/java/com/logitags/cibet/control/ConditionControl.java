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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.EventMetadata;

/**
 * evaluates JavaScript conditions. The following variables can be used in
 * conditions:
 * <p>
 * USER: the actual user id
 * <p>
 * PROPERTIES: map of application defined properties from CibetContext
 * <p>
 * EVENT: the actual ControlEvent
 * <p>
 * #simple class name#: the actual object
 * <p>
 * PARAM0, PARAM1 .. PARAMn: the method parameter objects in declared order
 * (only method level conditions)
 */
public class ConditionControl implements Control {

   private static Log log = LogFactory.getLog(ConditionControl.class);

   public static final String NAME = "condition";

   private static final String SCRIPTENGINE_KEY = "__SCRIPENGINE";

   private static ScriptEngineFactory scriptFac;

   private static Pattern attributesPattern = Pattern.compile("(\\$\\w+)");

   private List<String> resolveAttributes(String condition) {
      List<String> attributes = new ArrayList<String>();
      if (condition == null || condition.length() == 0)
         return attributes;
      log.debug("resolve condition " + condition);

      Matcher m = attributesPattern.matcher(condition);
      while (m.find()) {
         String attr = m.group(1);
         log.debug("found attribute " + attr);
         attributes.add(attr);
      }
      return attributes;
   }

   protected ScriptEngine createScriptEngine(EventMetadata metadata) {
      if (scriptFac == null)
         initScriptEngineFactory();
      ScriptEngine engine = scriptFac.getScriptEngine();
      engine.put("$REQUESTSCOPE", Context.requestScope());
      engine.put("$SESSIONSCOPE", Context.sessionScope());
      engine.put("$APPLICATIONSCOPE", Context.applicationScope());
      engine.put("$EVENT", metadata.getControlEvent());

      metadata.getResource().getResourceHandler().fillContext(engine);
      metadata.getProperties().put(SCRIPTENGINE_KEY, engine);
      return engine;
   }

   @Override
   public boolean hasControlValue(Object controlValue) {
      String str = (String) controlValue;
      return str != null && str.length() > 0;
   }

   private synchronized void initScriptEngineFactory() {
      ScriptEngineManager mgr = new ScriptEngineManager();
      ScriptEngine jsEngine = mgr.getEngineByName("JavaScript");
      if (jsEngine == null) {
         String msg = "Failed to initialise ConditionControl: "
               + "no script engine implementation for 'JavaScript' found";
         log.fatal(msg);
         throw new RuntimeException(msg);
      }
      scriptFac = jsEngine.getFactory();
      log.info("init Script Engine " + scriptFac.getEngineName() + " version " + scriptFac.getEngineVersion());
   }

   @Override
   public String getName() {
      return NAME;
   }

   /**
    * Returns a String value. If configValue does not start with file: returns
    * configValue. Otherwise tries to load the script file from classpath or
    * from an absolute position.
    * 
    * @see com.logitags.cibet.control.AbstractControl#resolve(java.lang .String)
    */
   @Override
   public Object resolve(String configValue) {
      log.debug("resolve condition config value: " + configValue);
      if (configValue == null || !configValue.startsWith("file:")) {
         return configValue;
      } else {
         // 1. as URI
         try {
            String uriFilename = configValue.replace('\\', '/');
            URI uri = new URI(uriFilename);
            URL url = uri.toURL();
            if (url != null) {
               String script = loadFromURL(url, uriFilename);
               if (script != null) {
                  return script;
               }
            }

         } catch (MalformedURLException e) {
            log.warn(e.getMessage());
         } catch (URISyntaxException e) {
            log.warn(e.getMessage());
         }

         // 2. from classpath
         String cpFilename = configValue.substring(5);
         ClassLoader cloader = Thread.currentThread().getContextClassLoader();
         URL url = cloader.getResource(cpFilename);
         if (url == null) {
            String msg = "Failed to load script file " + cpFilename + " from classpath. File not found";
            log.error(msg);
            throw new IllegalArgumentException(msg);
         }
         return loadFromURL(url, cpFilename);
      }
   }

   private String loadFromURL(URL url, String filename) {
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

   @Override
   public boolean evaluate(Object controlValue, EventMetadata metadata) {
      if (metadata == null) {
         String msg = "failed to execute condition evaluation: metadata is null";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }

      String condition = (String) controlValue;

      ScriptEngine engine = (ScriptEngine) metadata.getProperties().get(SCRIPTENGINE_KEY);
      if (engine == null) {
         createScriptEngine(metadata);
         engine = (ScriptEngine) metadata.getProperties().get(SCRIPTENGINE_KEY);
      }

      List<String> attributes = resolveAttributes(condition);
      for (String attr : attributes) {
         if (!engine.getBindings(ScriptContext.ENGINE_SCOPE).containsKey(attr)) {
            log.warn("Condition '" + condition + "' contains attribute '" + attr
                  + "' which is not in the script engine context for evaluating resource " + metadata.getResource());
            return false;
         }
      }

      try {
         Object result = engine.eval(condition);
         if (result == null || !(result instanceof Boolean)) {
            String msg = "failed to execute Condition evaluation: condition must return a Boolean value";
            log.error(msg);
            throw new RuntimeException(msg);
         }
         return (Boolean) result;

      } catch (ScriptException e) {
         String msg = "failed to execute Condition evaluation: Java script error: " + e.getMessage();
         log.error(msg, e);
         throw new RuntimeException(msg, e);
      }
   }

}
