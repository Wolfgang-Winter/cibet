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

public class CibetStatementExUpIntegrationColumnIndexesTest extends CibetStatementExUpIntegrationTest {

   private static Logger log = Logger.getLogger(CibetStatementExUpIntegrationColumnIndexesTest.class);

   protected void execute(String sql, int expectedCount) throws Exception {
      log.debug("execute statement with columnIndexes");
      Statement st = connection.createStatement();
      int count = st.executeUpdate(sql, new int[] { 1 });
      Assert.assertEquals(expectedCount, count);
   }

}
