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

import java.sql.DriverPropertyInfo;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class CibetDriverTest {

   private static Logger log = Logger.getLogger(CibetDriverTest.class);

   @Test
   public void connectError1() throws SQLException {
      CibetDriver driver = new CibetDriver();
      try {
         driver.connect("jdbc:cibet:cccc", null);
         Assert.fail();
      } catch (CibetJdbcException e) {
         Assert.assertTrue(e.getMessage().startsWith(
               "Failed to parse JDBC connection URL"));
      }
   }

   @Test
   public void connectError2() throws SQLException {
      log.debug("start test connectError2()");
      CibetDriver driver = new CibetDriver();
      Assert.assertEquals(1, driver.getMajorVersion());
      Assert.assertEquals(1, driver.getMajorVersion());
      try {
         driver.connect("jdbc:cibet:cccc:sdfsfdsdf", null);
         Assert.fail();
      } catch (CibetJdbcException e) {
         Assert.assertTrue(e.getMessage().startsWith(
               "Failed to instantiate JDBC driver"));
      }
   }

   @Test
   public void versions() throws SQLException {
      CibetDriver driver = new CibetDriver();
      Assert.assertEquals(1, driver.getMajorVersion());
      Assert.assertEquals(0, driver.getMinorVersion());
   }

   @Test
   public void getPropertyInfo() throws SQLException {
      CibetDriver driver = new CibetDriver();
      DriverPropertyInfo[] info = driver.getPropertyInfo("", null);
      Assert.assertEquals(0, info.length);
   }

}
