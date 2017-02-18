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

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.cibethelper.base.JdbcHelper;
import com.logitags.cibet.core.ControlEvent;

public class SqlParserIntegrationTest extends JdbcHelper {

   private static Logger log = Logger.getLogger(SqlParserIntegrationTest.class);

   @Test
   public void parseInsertUpdateColumns() throws Exception {
      log.debug("start test parseInsertUpdateColumns()");

      SqlParser parser = new SqlParser(connection, "INSert into cib_testentity  values" + "(123,  'Kassa');");
      List<SqlParameter> l = parser.getInsertUpdateColumns();
      Assert.assertEquals(2, l.size());
      Iterator<SqlParameter> it = l.iterator();
      SqlParameter e = it.next();
      Assert.assertEquals("id", e.getColumn().toLowerCase());
      Assert.assertEquals(123L, e.getValue());

      e = it.next();
      Assert.assertEquals("namevalue", e.getColumn().toLowerCase());
      Assert.assertEquals("Kassa", e.getValue());
   }

   @Test
   public void parsePrimaryKey() throws Exception {
      log.debug("start test parsePrimaryKey()");

      SqlParser parser = new SqlParser(connection,
            "Update cib_testentity  set counter=123, owner='Jung'" + " where id=123 ;");
      SqlParameter e = parser.getPrimaryKey();
      Assert.assertEquals("id", e.getColumn().toLowerCase());
      Assert.assertEquals(123l, e.getValue());
   }

   @Test
   public void parseControlEvent() throws Exception {
      log.debug("start test parseControlEvent() Integration");
      SqlParser parser = new SqlParser(connection, "DELETE FROM\n cib_testentity \nwhere ID=7 ");
      Assert.assertEquals(ControlEvent.DELETE, parser.getControlEvent());

      parser = new SqlParser(connection, " UpdATE\n\ncib_testentity  SET Vacation = Vacation * 1.25 Where id= 87 ; ");
      Assert.assertEquals(ControlEvent.UPDATE, parser.getControlEvent());
   }

}
