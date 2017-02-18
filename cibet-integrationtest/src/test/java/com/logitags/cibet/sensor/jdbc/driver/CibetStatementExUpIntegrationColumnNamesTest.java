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

public class CibetStatementExUpIntegrationColumnNamesTest extends CibetStatementExUpIntegrationTest {

   private static Logger log = Logger.getLogger(CibetStatementExUpIntegrationColumnNamesTest.class);

   protected void execute(String sql, int expectedCount) throws Exception {
      log.debug("execute statement with columnNames");
      Statement st = connection.createStatement();
      int count = st.executeUpdate(sql, new String[] { "id" });
      Assert.assertEquals(expectedCount, count);
   }

}
