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
package com.logitags.cibet.control;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.cibethelper.base.CoreTestBase;
import com.cibethelper.base.Sub4EyesController;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.sensor.ejb.EjbResourceHandler;
import com.logitags.cibet.sensor.jpa.JpaResourceHandler;

public class TargetControlTest extends CoreTestBase {

   private static Logger log = Logger.getLogger(TargetControlTest.class);

   private List<Setpoint> evaluate(EventMetadata md, List<Setpoint> spoints) {
      Control eval = new TargetControl();
      List<Setpoint> list = new ArrayList<Setpoint>();
      for (Setpoint spi : spoints) {
         Map<String, Object> controlValues = new TreeMap<String, Object>(new ControlComparator());
         spi.getEffectiveControlValues(controlValues);
         Object value = controlValues.get("target");
         if (value == null) {
            list.add(spi);
         } else {
            boolean okay = eval.evaluate(value, md);
            if (okay) {
               list.add(spi);
            }
         }
      }
      return list;
   }

   @Test
   public void evaluateError() throws Exception {
      log.info("start evaluate()");
      String result = initConfiguration("config_tenant_event_target_error.xml");
      Assert.assertTrue(result.startsWith(
            "failed to start Configuration: Unparsable configuration value \"Hastela, VV \" , com.logitags.cibet.Nix, com.cibethelper.entities.TE;\" \\\"Hase\"\": no end quote found, end of String reached"));
   }

   @Test
   public void evaluate() throws Exception {
      log.info("start evaluate()");
      String result = initConfiguration("config_tenant_event_target.xml");

      List<Setpoint> spB = Configuration.instance().getSetpoints();

      Resource res = new Resource(EjbResourceHandler.class, "StringClass", (Method) null, null);
      EventMetadata md = new EventMetadata(ControlEvent.ALL, res);
      List<Setpoint> list = evaluate(md, spB);
      Assert.assertEquals(2, list.size());
      Assert.assertEquals("B1", list.get(0).getId());
      Assert.assertEquals("D", list.get(1).getId());

      res = new Resource(EjbResourceHandler.class, "String", (Method) null, null);
      md = new EventMetadata(ControlEvent.ALL, res);
      list = evaluate(md, spB);
      Assert.assertEquals(2, list.size());
      Assert.assertEquals("B1", list.get(0).getId());
      Assert.assertEquals("D", list.get(1).getId());

      res = new Resource(JpaResourceHandler.class, new TEntity());
      md = new EventMetadata(ControlEvent.ALL, res);
      list = evaluate(md, spB);
      Assert.assertEquals(5, list.size());
      Assert.assertEquals("B1", list.get(0).getId());
      Assert.assertEquals("B2", list.get(1).getId());
      Assert.assertEquals("B3", list.get(2).getId());
      Assert.assertEquals("C", list.get(3).getId());
      Assert.assertEquals("D", list.get(4).getId());

      res = new Resource(EjbResourceHandler.class, new Sub4EyesController(), (Method) null, null);
      md = new EventMetadata(ControlEvent.ALL, res);
      list = evaluate(md, spB);
      Assert.assertEquals(4, list.size());
      Assert.assertEquals("B1", list.get(0).getId());
      Assert.assertEquals("B2", list.get(1).getId());
      Assert.assertEquals("B3", list.get(2).getId());
      Assert.assertEquals("D", list.get(3).getId());
   }

   @SuppressWarnings("unchecked")
   @Test
   public void resolve() throws Exception {
      TargetControl ctrl = new TargetControl();
      List<String> list = (List<String>) ctrl.resolve("");
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("", list.get(0));

      list = (List<String>) ctrl.resolve(" AAA , BBB ");
      Assert.assertEquals(2, list.size());
      Assert.assertEquals("AAA", list.get(0));
      Assert.assertEquals("BBB", list.get(1));

      list = (List<String>) ctrl.resolve("*");
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("*", list.get(0));

      list = (List<String>) ctrl.resolve("\"haste noch , ass\"");
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("haste noch , ass", list.get(0));

      list = (List<String>) ctrl.resolve("\"haste noch , ass\\\"\"");
      Assert.assertEquals(1, list.size());
      log.debug(list.get(0));
      Assert.assertEquals("haste noch , ass\"", list.get(0));

      list = (List<String>) ctrl.resolve("\"\\\"haste noch , ass\\\"\" ; �sal");
      Assert.assertEquals(2, list.size());
      log.debug(list.get(0));
      Assert.assertEquals("\"haste noch , ass\"", list.get(0));
      Assert.assertEquals("�sal", list.get(1));

      list = (List<String>) ctrl.resolve("\"\\\"haste noch , ass\\\"\" ; \"�sal\"");
      Assert.assertEquals(2, list.size());
      log.debug(list.get(0));
      Assert.assertEquals("\"haste noch , ass\"", list.get(0));
      Assert.assertEquals("�sal", list.get(1));

      list = (List<String>) ctrl.resolve("\"\\\"haste noch , ass\\\"\" ; ss, \"�sal\"");
      Assert.assertEquals(3, list.size());
      Assert.assertEquals("\"haste noch , ass\"", list.get(0));
      Assert.assertEquals("ss", list.get(1));
      Assert.assertEquals("�sal", list.get(2));

      log.debug("test here");
      list = (List<String>) ctrl.resolve("com.nix,com.nix.*, com.logitags.cibet.*");
      Assert.assertEquals(3, list.size());
      Assert.assertEquals("com.nix", list.get(0));
      Assert.assertEquals("com.nix.*", list.get(1));
      Assert.assertEquals("com.logitags.cibet.*", list.get(2));
   }

}
