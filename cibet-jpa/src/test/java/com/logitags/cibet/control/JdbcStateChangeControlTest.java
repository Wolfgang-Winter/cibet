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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.cibethelper.base.CoreTestBase;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.sensor.jdbc.driver.JdbcResourceHandler;
import com.logitags.cibet.sensor.jdbc.driver.SqlParser;

@RunWith(MockitoJUnitRunner.class)
public class JdbcStateChangeControlTest extends CoreTestBase {

   private static Logger log = Logger.getLogger(JdbcStateChangeControlTest.class);

   @Mock
   private EntityManager em;

   @Mock
   private EntityManager em2;

   private List<Setpoint> evaluate(EventMetadata md, List<Setpoint> spoints) {
      Control eval = new StateChangeControl();
      List<Setpoint> list = new ArrayList<Setpoint>();
      for (Setpoint spi : spoints) {
         Map<String, Object> controlValues = new TreeMap<String, Object>(new ControlComparator());
         spi.getEffectiveControlValues(controlValues);
         Object value = controlValues.get("stateChange");
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
   public void evaluateExcludeJdbc() throws Exception {
      log.info("start evaluateExcludeJdbc()");
      List<Setpoint> spB = new ArrayList<Setpoint>();
      Setpoint sp = new Setpoint("jdbc1");
      sp.setStateChange(true, "voodoo");
      spB.add(sp);

      String sql = "Update cuba SET ddss= 4, voodoo='huch' WHERE hinz=1234";
      SqlParser parser = new SqlParser(null, sql);
      Resource res = new Resource(JdbcResourceHandler.class, parser.getSql(), parser.getTargetType(),
            parser.getPrimaryKey(), null);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, spB);
      Assert.assertEquals(1, list.size());
   }

   @Test
   public void evaluateExcludeJdbc2() throws Exception {
      log.info("start evaluateExcludeJdbc2()");
      List<Setpoint> spB = new ArrayList<Setpoint>();
      Setpoint sp = new Setpoint("jdbc1");
      sp.setStateChange(true, "voodoo", "ddss");
      spB.add(sp);

      String sql = "Update cuba SET ddss= 4, voodoo='huch' WHERE hinz=1234";
      SqlParser parser = new SqlParser(null, sql);
      Resource res = new Resource(JdbcResourceHandler.class, parser.getSql(), parser.getTargetType(),
            parser.getPrimaryKey(), null);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, spB);
      Assert.assertEquals(0, list.size());
   }

   @Test
   public void evaluateIncludeJdbc() throws Exception {
      log.info("start evaluateIncludeJdbc()");
      List<Setpoint> spB = new ArrayList<Setpoint>();
      Setpoint sp = new Setpoint("jdbc1");
      sp.setStateChange(false, "voodoo");
      spB.add(sp);

      String sql = "Update cuba SET ddss= 4, voodoo='huch',voodo=99 WHERE hinz=1234";
      SqlParser parser = new SqlParser(null, sql);
      Resource res = new Resource(JdbcResourceHandler.class, parser.getSql(), parser.getTargetType(),
            parser.getPrimaryKey(), null);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, spB);
      Assert.assertEquals(1, list.size());
   }

   @Test
   public void evaluateIncludeJdbc2() throws Exception {
      log.info("start evaluateIncludeJdbc2()");
      List<Setpoint> spB = new ArrayList<Setpoint>();
      Setpoint sp = new Setpoint("jdbc1");
      sp.setStateChange(false, "voodoo");
      spB.add(sp);

      String sql = "Update cuba SET ddss= 4, voodo=99 WHERE hinz=1234";
      SqlParser parser = new SqlParser(null, sql);
      Resource res = new Resource(JdbcResourceHandler.class, parser.getSql(), parser.getTargetType(),
            parser.getPrimaryKey(), null);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, spB);
      Assert.assertEquals(0, list.size());
   }

}
