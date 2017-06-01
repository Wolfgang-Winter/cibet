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

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cibethelper.base.JdbcHelper;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.sensor.jdbc.driver.JdbcResource;
import com.logitags.cibet.sensor.jdbc.driver.SqlParser;

/**
 *
 */
public class JdbcStateChangeIntegrationTest extends JdbcHelper {

   private Connection connection;

   @Before
   public void before() throws IOException, SQLException {
      connection = JdbcHelper.createConnection();
   }

   /**
    * logger for tracing
    */
   private static Logger log = Logger.getLogger(JdbcStateChangeIntegrationTest.class);

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
   public void evaluateExclude1u2() throws Exception {
      log.info("start jdbcEvaluateExclude1u2()");
      initConfiguration("config_stateChange1_invoker.xml");

      String sql = "update cib_testentity set counter=12 where id=77";
      SqlParser parser = new SqlParser(connection, sql);

      JdbcResource res = new JdbcResource(parser.getSql(), parser.getTarget(), parser.getPrimaryKey(), null);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("A", list.get(0).getId());

      sql = "update cib_testentity set namevalue='Jug' where id=77";
      parser = new SqlParser(connection, sql);
      res = new JdbcResource(parser.getSql(), parser.getTarget(), parser.getPrimaryKey(), null);
      md = new EventMetadata(ControlEvent.UPDATE, res);
      list = evaluate(md, Configuration.instance().getSetpoints());
      Assert.assertEquals(2, list.size());
      Assert.assertEquals("A", list.get(0).getId());
      Assert.assertEquals("C", list.get(1).getId());
   }

   @Test
   public void evaluateExclude2a() throws Exception {
      log.info("start jdbcEvaluateExclude2a()");
      initConfiguration("config_stateChange1_invoker.xml");

      String sql = "update cib_testentity set counter=12, namevalue='Jug' where id=77";
      SqlParser parser = new SqlParser(connection, sql);

      JdbcResource res = new JdbcResource(parser.getSql(), parser.getTarget(), parser.getPrimaryKey(), null);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints());
      Assert.assertEquals(2, list.size());
      Assert.assertEquals("A", list.get(0).getId());
      Assert.assertEquals("C", list.get(1).getId());
   }

   @Test
   public void evaluateIncludeSimpleAttribute() throws Exception {
      log.info("start jdbcEvaluateIncludeSimpleAttribute()");
      initConfiguration("config_condition_stateChange2_invoker_method.xml");

      String sql = "update cib_testentity set counter=12 where id=77";
      SqlParser parser = new SqlParser(connection, sql);

      JdbcResource res = new JdbcResource(parser.getSql(), parser.getTarget(), parser.getPrimaryKey(), null);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("C", list.get(0).getId());

      sql = "update cib_testentity set namevalue='Jug' where id=77";
      parser = new SqlParser(connection, sql);
      res = new JdbcResource(parser.getSql(), parser.getTarget(), parser.getPrimaryKey(), null);
      md = new EventMetadata(ControlEvent.UPDATE, res);
      list = evaluate(md, Configuration.instance().getSetpoints());
      Assert.assertEquals(0, list.size());
   }

}
