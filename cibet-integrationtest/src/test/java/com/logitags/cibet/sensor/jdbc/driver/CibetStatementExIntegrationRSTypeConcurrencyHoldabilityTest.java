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

public class CibetStatementExIntegrationRSTypeConcurrencyHoldabilityTest extends CibetStatementExUpIntegrationTest {

   private static Logger log = Logger.getLogger(CibetStatementExIntegrationRSTypeConcurrencyHoldabilityTest.class);

   protected void execute(String sql, int expectedCount) throws Exception {
      log.debug("execute statement with columnNames");
      Statement st = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
            ResultSet.HOLD_CURSORS_OVER_COMMIT);
      st.execute(sql, Statement.RETURN_GENERATED_KEYS);
   }

   protected void checkResult(Object res) {
      Assert.assertEquals(Boolean.class, res.getClass());
      Assert.assertEquals(false, res);
   }

}
