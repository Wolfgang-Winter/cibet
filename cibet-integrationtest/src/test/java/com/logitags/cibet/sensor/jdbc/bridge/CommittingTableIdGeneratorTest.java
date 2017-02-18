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
package com.logitags.cibet.sensor.jdbc.bridge;

import javax.sql.DataSource;

import org.junit.Test;

import com.cibethelper.base.CibetTestDataSource;

public class CommittingTableIdGeneratorTest {

   @Test(expected = IllegalArgumentException.class)
   public void getInstanceDataSource() {
      DataSource ds = new CibetTestDataSource();
      CommittingTableIdGenerator.getInstance(ds);
   }

}
