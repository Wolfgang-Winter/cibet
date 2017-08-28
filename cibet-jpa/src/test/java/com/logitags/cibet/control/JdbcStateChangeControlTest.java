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
import com.logitags.cibet.sensor.jdbc.driver.JdbcResource;
import com.logitags.cibet.sensor.jdbc.driver.SqlParser;

@RunWith(MockitoJUnitRunner.class)
public class JdbcStateChangeControlTest extends CoreTestBase {

   private static Logger log = Logger.getLogger(JdbcStateChangeControlTest.class);

   @Mock
   private EntityManager em;

   @Mock
   private EntityManager em2;

   @Test
   public void evaluateExcludeJdbc() throws Exception {
      log.info("start evaluateExcludeJdbc()");
      List<Setpoint> spB = new ArrayList<Setpoint>();
      Setpoint sp = new Setpoint("jdbc1");
      sp.addStateChangeIncludes("voodoo");
      spB.add(sp);

      String sql = "Update cuba SET ddss= 4, voodoo='huch' WHERE hinz=1234";
      SqlParser parser = new SqlParser(null, sql);
      JdbcResource res = new JdbcResource(parser.getSql(), parser.getTarget(), parser.getPrimaryKey(), null);
      EventMetadata md = new EventMetadata("JDBC", ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, spB, new StateChangeControl());
      Assert.assertEquals(1, list.size());
   }

   @Test
   public void evaluateExcludeJdbc2() throws Exception {
      log.info("start evaluateExcludeJdbc2()");
      List<Setpoint> spB = new ArrayList<Setpoint>();
      Setpoint sp = new Setpoint("jdbc1");
      sp.addStateChangeExcludes("voodoo", "ddss");
      spB.add(sp);

      String sql = "Update cuba SET ddss= 4, voodoo='huch' WHERE hinz=1234";
      SqlParser parser = new SqlParser(null, sql);
      JdbcResource res = new JdbcResource(parser.getSql(), parser.getTarget(), parser.getPrimaryKey(), null);
      EventMetadata md = new EventMetadata("JDBC", ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, spB, new StateChangeControl());
      Assert.assertEquals(0, list.size());
   }

   @Test
   public void evaluateIncludeJdbc() throws Exception {
      log.info("start evaluateIncludeJdbc()");
      List<Setpoint> spB = new ArrayList<Setpoint>();
      Setpoint sp = new Setpoint("jdbc1");
      sp.addStateChangeIncludes("voodoo");
      spB.add(sp);

      String sql = "Update cuba SET ddss= 4, voodoo='huch',voodo=99 WHERE hinz=1234";
      SqlParser parser = new SqlParser(null, sql);
      JdbcResource res = new JdbcResource(parser.getSql(), parser.getTarget(), parser.getPrimaryKey(), null);
      EventMetadata md = new EventMetadata("JDBC", ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, spB, new StateChangeControl());
      Assert.assertEquals(1, list.size());
   }

   @Test
   public void evaluateIncludeJdbc2() throws Exception {
      log.info("start evaluateIncludeJdbc2()");
      List<Setpoint> spB = new ArrayList<Setpoint>();
      Setpoint sp = new Setpoint("jdbc1");
      sp.addStateChangeIncludes("voodoo");
      spB.add(sp);

      String sql = "Update cuba SET ddss= 4, voodo=99 WHERE hinz=1234";
      SqlParser parser = new SqlParser(null, sql);
      JdbcResource res = new JdbcResource(parser.getSql(), parser.getTarget(), parser.getPrimaryKey(), null);
      EventMetadata md = new EventMetadata("JDBC", ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, spB, new StateChangeControl());
      Assert.assertEquals(0, list.size());
   }

}
