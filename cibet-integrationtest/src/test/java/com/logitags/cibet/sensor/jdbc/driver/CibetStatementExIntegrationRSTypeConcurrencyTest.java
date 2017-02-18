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

import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;
import org.junit.Assert;

public class CibetStatementExIntegrationRSTypeConcurrencyTest extends CibetStatementExUpIntegrationTest {

   private static Logger log = Logger.getLogger(CibetStatementExIntegrationRSTypeConcurrencyTest.class);

   protected void execute(String sql, int expectedCount) throws Exception {
      log.debug("execute statement with columnNames");
      Statement st = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
      st.execute(sql);
   }

   protected void checkResult(Object res) {
      Assert.assertEquals(Boolean.class, res.getClass());
      Assert.assertEquals(false, res);
   }

}
