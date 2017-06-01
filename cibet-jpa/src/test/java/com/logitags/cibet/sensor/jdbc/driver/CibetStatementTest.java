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

public class CibetStatementTest {

   private static Logger log = Logger.getLogger(CibetStatementTest.class);

   private void testMatch(String sql, String expectedTableName) throws Exception {
      SqlParser parser = new SqlParser(null, sql);
      Assert.assertEquals(expectedTableName, parser.getTarget());
   }

   private void testparseUpdateColumns(String sql, String[] cols, String[] vals) throws Exception {
      SqlParser parser = new SqlParser(null, sql);
      List<SqlParameter> l = parser.getInsertUpdateColumns();
      Assert.assertEquals(cols.length, l.size());
      Iterator<SqlParameter> it = l.iterator();
      for (String col : cols) {
         Assert.assertEquals(col, it.next().getColumn());
      }

      Iterator<SqlParameter> it2 = l.iterator();
      for (String val : vals) {
         Assert.assertEquals(val, it2.next().getValue().toString());
      }
   }

   @Test
   public void parseTargetInsert() throws Exception {
      testMatch("INSERT INTO Prod (has, klas)Values(34,'ert') ", "Prod");
      testMatch(" INseRT  InTO Prod (has, klas) Values(34,'ert') ", "Prod");
      testMatch("INSERT INTO\n Prod(has, klas) Values\n(34,'ert') ", "Prod");
      testMatch("INSERT INTO\n Prod\t (has, klas)Values\n(34,'ert') ", "Prod");
      testMatch("INSERT INTO\n wasser.Prod   (has, klas)Values\n(34,'ert') ", "Prod");
      testMatch("INSERT INTO\n wasser.Prod  (has, klas) Values\n(34,'ert') ", "Prod");
      testMatch("INSERT\nINTO\n wasser.Prod (has, klas)  Values\n(34,'ert') ", "Prod");
      testMatch("INSERT\n\n  INTO\n wasser.Prod  (has, klas) Values\n(34,'ert') ", "Prod");
   }

   @Test
   public void parseTargetUpdate() throws Exception {
      testMatch("UPDATE Prod SET Vacation = Vacation * 1.25; ", "Prod");
      testMatch(" UpdATE\n\nProd  SET Vacation = Vacation * 1.25; ", "Prod");
      testMatch(" UpdATE\n\nProd\nSET Vacation = Vacation * 1.25; ", "Prod");
      testMatch("UpdATE \n\nw.Prod\n SET Vacation = Vacation * 1.25; ", "Prod");
   }

   @Test
   public void parseTargetDelete() throws Exception {
      SqlParser parser = new SqlParser(null, "DELETE FROM Prod  ");
      Assert.assertEquals("Prod", parser.getTarget());
      parser = new SqlParser(null, "DELETE FROM\n wasser.Prod   \n ");
      Assert.assertEquals("Prod", parser.getTarget());

      testMatch("DELETE FROM Prod ", "Prod");
      // testMatch("xDELETE FROM Prod Values() ", null);
      testMatch(" DeleTE FROM Prod where id=88", "Prod");
      testMatch("DELETE FROM\n Prod where id=88\n ", "Prod");
      testMatch("DELETE FROM\n Prod\t where id=88\n ", "Prod");
      testMatch("DELETE FROM\n wasser.Prod   where id=88\n ", "Prod");
      testMatch("DELETE  FROM\n wasser.Prod   \n ", "Prod");
      testMatch("DELETE\nFROM\n wasser.Prod   \n ", "Prod");
      testMatch("DELETE\n\n  FROM\n wasser.Prod   \n ", "Prod");
   }

   @Test(expected = CibetJdbcException.class)
   public void parseUpdateColumnsError() throws Exception {
      testparseUpdateColumns("UPDATE Prod SET id=1,name='klaus WHERE id=?", new String[] { "id", "name" },
            new String[] { "1", "'klaus'" });
   }

   @Test
   public void parseUpdateColumns() throws Exception {
      testparseUpdateColumns("UPDATE Prod SET id=1,name='klaus' WHERE id=?", new String[] { "id", "name" },
            new String[] { "1", "klaus" });

      testparseUpdateColumns("UPDATE Prod SET id=1, name='klaus', ctrct=\" where has\" ,date=21-3 WHERE id=?",
            new String[] { "id", "name", "ctrct", "date" }, new String[] { "1", "klaus", "\" where has\"", "21 - 3" });

      testparseUpdateColumns("UPDATE Prod a SET id=1, a.name='klaus', a.ctrct=\" where has\" ,date=21-3 WHERE id=?",
            new String[] { "id", "name", "ctrct", "date" }, new String[] { "1", "klaus", "\" where has\"", "21 - 3" });

      testparseUpdateColumns("UPDATE Prod a SET id=1, a.name='klaus', a.ctrct=\" where has\" ,date=21-3 ",
            new String[] { "id", "name", "ctrct", "date" }, new String[] { "1", "klaus", "\" where has\"", "21 - 3" });

      testparseUpdateColumns("UPDATE Prod SET id=1, fluf = to_String('hase') + ratze() , name='klaus';",
            new String[] { "id", "fluf", "name" }, new String[] { "1", "to_String('hase') + ratze", "klaus" });

      testparseUpdateColumns("UPDATE Prod a SET id=1, a.name='klaus', a.ctrct=\" where has\" ,date=21-3 ; ",
            new String[] { "id", "name", "ctrct", "date" }, new String[] { "1", "klaus", "\" where has\"", "21 - 3" });
   }

}
