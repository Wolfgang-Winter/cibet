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
import java.util.Set;
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
 * evaluates JavaScript conditions. The following variables can be used in conditions:
 * <p>
 * USER: the actual user id
 * <p>
 * PROPERTIES: map of application defined properties from CibetContext
 * <p>
 * EVENT: the actual ControlEvent
 * <p>
 * #simple class name#: the actual object
 * <p>
 * PARAM0, PARAM1 .. PARAMn: the method parameter objects in declared order (only method level conditions)
 */
public class ConditionControl implements Control {

   private static Log log = LogFactory.getLog(ConditionControl.class);

   public static final String NAME = "condition";

   private static final String SCRIPTENGINE_KEY = "__SCRIPENGINE";

   private static ScriptEngineFactory scriptFac;

   private static Pattern attributesPattern = Pattern.compile("(\\$\\w+)");

   private List<String> resolveAttributes(String condition) {
      List<String> attributes = new ArrayList<String>();
      if (condition == null || condition.length() == 0) return attributes;
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
      if (scriptFac == null) initScriptEngineFactory();
      ScriptEngine engine = scriptFac.getScriptEngine();
      engine.put("$REQUESTSCOPE", Context.requestScope());
      engine.put("$SESSIONSCOPE", Context.sessionScope());
      engine.put("$APPLICATIONSCOPE", Context.applicationScope());
      engine.put("$EVENT", metadata.getControlEvent());

      metadata.getResource().fillContext(engine);
      metadata.getProperties().put(SCRIPTENGINE_KEY, engine);
      return engine;
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

   @Override
   public Boolean evaluate(Set<String> values, EventMetadata metadata) {
      if (metadata == null) {
         String msg = "failed to execute condition evaluation: metadata is null";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }

      if (values == null || values.isEmpty()) return null;

      ScriptEngine engine = (ScriptEngine) metadata.getProperties().get(SCRIPTENGINE_KEY);
      if (engine == null) {
         createScriptEngine(metadata);
         engine = (ScriptEngine) metadata.getProperties().get(SCRIPTENGINE_KEY);
      }

      for (String condition : values) {
         List<String> attributes = resolveAttributes(condition);
         for (String attr : attributes) {
            if (!engine.getBindings(ScriptContext.ENGINE_SCOPE).containsKey(attr)) {
               String err = "Failes condition '" + condition + "' contains attribute '" + attr
                     + "' which is not in the script engine context for evaluating resource " + metadata.getResource();
               log.warn(err);
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
            if ((Boolean) result == true) {
               return (Boolean) result;
            }

         } catch (ScriptException e) {
            String msg = "failed to execute Condition evaluation: Java script error: " + e.getMessage();
            log.error(msg, e);
            throw new RuntimeException(msg, e);
         }
      }
      return false;
   }

}
