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

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.logitags.cibet.core.ControlEvent;

public class SqlParserTest {

   private static Logger log = Logger.getLogger(SqlParserTest.class);

   @Test
   public void parseControlEvent() throws Exception {
      SqlParser parser = new SqlParser(null, "INSERT INTO Prod Values(2) ");
      Assert.assertEquals(ControlEvent.INSERT, parser.getControlEvent());

      parser = new SqlParser(null, "InsERT INTO Prod Values(2) ");
      Assert.assertEquals(ControlEvent.INSERT, parser.getControlEvent());

      parser = new SqlParser(null, " UpdATE\n\nProd  SET Vacation = Vacation * 1.25 where dd = 34; ");
      Assert.assertNull(parser.getControlEvent());

      parser = new SqlParser(null, " UpdATE\n\nProd  SET Vacation = Vacation * 1.25; ");
      Assert.assertNull(parser.getControlEvent());

      parser = new SqlParser(null, "DROP table sd; ");
      Assert.assertEquals(null, parser.getControlEvent());

      parser = new SqlParser(null, "create table sd; ");
      Assert.assertEquals(null, parser.getControlEvent());

      parser = new SqlParser(null, "select * from  sd; ");
      Assert.assertEquals(null, parser.getControlEvent());

      parser = new SqlParser(null, "TRUNCATE TABLE \"table_name\";");
      Assert.assertEquals(null, parser.getControlEvent());
   }

   @Test
   public void parseTargetInsert() throws Exception {
      SqlParser parser = new SqlParser(null, "INSERT INTO Prod Values(2) ");
      Assert.assertEquals("Prod", parser.getTargetType());

      parser = new SqlParser(null, " INseRT  InTO Prod2 (asd,fgh)Values(4,6) ");
      Assert.assertEquals("Prod2", parser.getTargetType());

      parser = new SqlParser(null, "INSERT INTO\n Prod3 (asd,fgh)\nValues\n('',2) ");
      Assert.assertEquals("Prod3", parser.getTargetType());

      parser = new SqlParser(null, "INSERT INTO\n Prod4\t (asd,fgh)Values\n('',234) ");
      Assert.assertEquals("Prod4", parser.getTargetType());

      parser = new SqlParser(null, "INSERT INTO\n wasser.Prod5   Values\n('',234) ");
      Assert.assertEquals("Prod5", parser.getTargetType());

      parser = new SqlParser(null, "INSERT  INTO\n wasser.Prod6   Values\n('',234) ");
      Assert.assertEquals("Prod6", parser.getTargetType());

      parser = new SqlParser(null, "INSERT\nINTO\n wasser.Prod7   Values\n('',234) ");
      Assert.assertEquals("Prod7", parser.getTargetType());

      parser = new SqlParser(null, "INSERT\n\n  INTO\n wasser.Prod8    Values\n('',234) ");
      Assert.assertEquals("Prod8", parser.getTargetType());
   }

   @Test
   public void parseTargetUpdate() throws Exception {
      SqlParser parser = new SqlParser(null, "UPDATE Prod SET Vacation = Vacation * 1.25; ");
      Assert.assertEquals("Prod", parser.getTargetType());

      parser = new SqlParser(null, " UpdATE\n\nProd  SET Vacation = Vacation * 1.25; ");
      Assert.assertEquals("Prod", parser.getTargetType());

      parser = new SqlParser(null, " UpdATE\n\nProd\nSET Vacation = Vacation * 1.25; ");
      Assert.assertEquals("Prod", parser.getTargetType());

      parser = new SqlParser(null, "UpdATE \n\nw.Prod\n SET Vacation = Vacation * 1.25; ");
      Assert.assertEquals("Prod", parser.getTargetType());
   }

   @Test
   public void parseTargetDelete() throws Exception {
      SqlParser parser = new SqlParser(null, "DELETE FROM Prod  ");
      Assert.assertEquals("Prod", parser.getTargetType());

      parser = new SqlParser(null, "DELETE FROM\n wasser.Prod   \n ");
      Assert.assertEquals("Prod", parser.getTargetType());

      parser = new SqlParser(null, "DELETE FROM Prode Where x=8 ");
      Assert.assertEquals("Prode", parser.getTargetType());

      parser = new SqlParser(null, "xDELETE FROM Prod Values() ");
      try {
         parser.getTargetType();
         Assert.fail();
      } catch (CibetJdbcException e) {
      }

      parser = new SqlParser(null, " DeleTE FROM Proda where xx=77 ");
      Assert.assertEquals("Proda", parser.getTargetType());

      parser = new SqlParser(null, "DELETE FROM\n Prod1 \nwhere 6=7 ");
      Assert.assertEquals("Prod1", parser.getTargetType());

      parser = new SqlParser(null, "DELETE FROM\n Prod2\t \n ");
      Assert.assertEquals("Prod2", parser.getTargetType());

      parser = new SqlParser(null, "DELETE  FROM\n wasser.Prod3   \nWhere gh='asddfa' ");
      Assert.assertEquals("Prod3", parser.getTargetType());

      parser = new SqlParser(null, "DELETE\nFROM\n wasser.Prod   \n ");
      Assert.assertEquals("Prod", parser.getTargetType());

      parser = new SqlParser(null, "DELETE\n \n  FROM\n wasser.Prods   \n ");
      Assert.assertEquals("Prods", parser.getTargetType());
   }

   @Test
   public void parseInsertUpdateColumnsInsert1() throws Exception {
      SqlParser parser = new SqlParser(null,
            "INSert into Prod (num1, num2, num3) values(1, to_String('hase') + ratze(2) , 'klaus');");
      List<SqlParameter> l = parser.getInsertUpdateColumns();
      Assert.assertEquals(3, l.size());
      Iterator<SqlParameter> it = l.iterator();
      SqlParameter e = it.next();
      Assert.assertEquals("num1", e.getColumn());
      Assert.assertEquals(1L, e.getValue());

      e = it.next();
      Assert.assertEquals("num2", e.getColumn());
      Assert.assertEquals("to_String('hase') + ratze(2)", e.getValue());

      e = it.next();
      Assert.assertEquals("num3", e.getColumn());
      Assert.assertEquals("klaus", e.getValue());
   }

   @Test
   public void parseInsertUpdateColumnsInsert2() throws Exception {
      SqlParser parser = new SqlParser(null, "INSert into Prod (num1, num2, num3, num4, num5) values"
            + "(null, to_String('hase') , ?, 3.6, {d'2012-07-23'});");
      List<SqlParameter> l = parser.getInsertUpdateColumns();
      Assert.assertEquals(5, l.size());
      Iterator<SqlParameter> it = l.iterator();
      SqlParameter e = it.next();
      Assert.assertEquals("num1", e.getColumn());
      Assert.assertEquals(null, e.getValue());

      e = it.next();
      Assert.assertEquals("num2", e.getColumn());
      Assert.assertEquals("to_String('hase')", e.getValue());

      e = it.next();
      Assert.assertEquals("num3", e.getColumn());
      Assert.assertEquals("?", e.getValue());

      e = it.next();
      Assert.assertEquals("num4", e.getColumn());
      Assert.assertEquals(3.6, e.getValue());

      e = it.next();
      Assert.assertEquals("num5", e.getColumn());
      Calendar cal = Calendar.getInstance();
      cal.setTime((Date) e.getValue());
      Assert.assertEquals(23, cal.get(Calendar.DATE));
      Assert.assertEquals(6, cal.get(Calendar.MONTH));
      Assert.assertEquals(2012, cal.get(Calendar.YEAR));
   }

   @Test
   public void parseInsertUpdateColumnsInsert3() throws Exception {
      SqlParser parser = new SqlParser(null, "INSert into Prod (num1, num2, num3, num4, num5) values"
            + "(56*9, 5/4 , (asa + 5*12), {ts'2012-07-23 16:45:13'}, {t'16:44:12'});");
      List<SqlParameter> l = parser.getInsertUpdateColumns();
      Assert.assertEquals(5, l.size());
      Iterator<SqlParameter> it = l.iterator();
      SqlParameter e = it.next();
      Assert.assertEquals("num1", e.getColumn());
      Assert.assertEquals("56 * 9", e.getValue());

      e = it.next();
      Assert.assertEquals("num2", e.getColumn());
      Assert.assertEquals("5 / 4", e.getValue());

      e = it.next();
      Assert.assertEquals("num3", e.getColumn());
      Assert.assertEquals("(asa + 5 * 12)", e.getValue());

      e = it.next();
      Assert.assertEquals("num4", e.getColumn());
      Calendar cal = Calendar.getInstance();
      cal.setTime((Timestamp) e.getValue());
      Assert.assertEquals(23, cal.get(Calendar.DATE));
      Assert.assertEquals(6, cal.get(Calendar.MONTH));
      Assert.assertEquals(2012, cal.get(Calendar.YEAR));
      Assert.assertEquals(16, cal.get(Calendar.HOUR_OF_DAY));
      Assert.assertEquals(45, cal.get(Calendar.MINUTE));
      Assert.assertEquals(13, cal.get(Calendar.SECOND));

      e = it.next();
      Assert.assertEquals("num5", e.getColumn());
      cal.setTime((Time) e.getValue());
      Assert.assertEquals(16, cal.get(Calendar.HOUR_OF_DAY));
      Assert.assertEquals(44, cal.get(Calendar.MINUTE));
      Assert.assertEquals(12, cal.get(Calendar.SECOND));
   }

   @Test
   public void parseInsertUpdateColumnsInsert4() throws Exception {
      SqlParser parser = new SqlParser(null, "INSert into Prod (num1, num2) values" + "(2-5,  (select a from gg));");
      List<SqlParameter> l = parser.getInsertUpdateColumns();
      Assert.assertEquals(2, l.size());
      Iterator<SqlParameter> it = l.iterator();
      SqlParameter e = it.next();
      Assert.assertEquals("num1", e.getColumn());
      Assert.assertEquals("2 - 5", e.getValue());

      e = it.next();
      Assert.assertEquals("num2", e.getColumn());
      Assert.assertEquals("(SELECT a FROM gg)", e.getValue());
   }

   @Test(expected = CibetJdbcException.class)
   public void parseInsertUpdateColumnsInsertError() throws Exception {
      SqlParser parser = new SqlParser(null,
            "INSert into Prod (num1, num2,num3) values" + "(2-5,  (select a from gg));");
      parser.getInsertUpdateColumns();
   }

   @Test
   public void parseInsertUpdateColumnsInsert5() throws Exception {
      SqlParser parser = new SqlParser(null, "INSert into Prod  values" + "(2-4,  (select a from gg));");
      List<SqlParameter> l = parser.getInsertUpdateColumns();
      Assert.assertEquals(2, l.size());
      Iterator<SqlParameter> it = l.iterator();
      SqlParameter e = it.next();
      Assert.assertEquals("?1", e.getColumn());
      Assert.assertEquals("2 - 4", e.getValue());

      e = it.next();
      Assert.assertEquals("?2", e.getColumn());
      Assert.assertEquals("(SELECT a FROM gg)", e.getValue());
   }

   @Test
   public void parseInsertUpdateColumnsUpdate1() throws Exception {
      SqlParser parser = new SqlParser(null, "Update Prod  SET " + " num1=2\n-4,  num2 = (select a from gg);");
      List<SqlParameter> l = parser.getInsertUpdateColumns();
      Assert.assertEquals(2, l.size());
      Iterator<SqlParameter> it = l.iterator();
      SqlParameter e = it.next();
      Assert.assertEquals("num1", e.getColumn());
      Assert.assertEquals("2 - 4", e.getValue());

      e = it.next();
      Assert.assertEquals("num2", e.getColumn());
      Assert.assertEquals("(SELECT a FROM gg)", e.getValue());
   }

   @Test(expected = CibetJdbcException.class)
   public void parseInsertUpdateColumnsUpdateError() throws Exception {
      SqlParser parser = new SqlParser(null, "Update Prod  SET " + " num1=2\n-4,  (select a from gg);");
      parser.getInsertUpdateColumns();
   }

}
