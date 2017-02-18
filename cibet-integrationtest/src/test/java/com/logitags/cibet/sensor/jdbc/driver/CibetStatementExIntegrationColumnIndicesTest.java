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
package com.logitags.cibet.sensor.jdbc.driver;

import java.sql.Statement;

import org.apache.log4j.Logger;
import org.junit.Assert;

public class CibetStatementExIntegrationColumnIndicesTest extends CibetStatementExUpIntegrationTest {

   private static Logger log = Logger.getLogger(CibetStatementExIntegrationColumnIndicesTest.class);

   protected void execute(String sql, int expectedCount) throws Exception {
      log.debug("execute statement with columnNames");
      Statement st = connection.createStatement();
      st.execute(sql, new int[] { 1 });
   }

   protected void checkResult(Object res) {
      Assert.assertEquals(Boolean.class, res.getClass());
      Assert.assertEquals(false, res);
   }

}
