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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cibethelper.base.JdbcHelper;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.actuator.dc.FourEyesActuator;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InitializationService;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.sensor.jdbc.bridge.JdbcBridgeEntityManager;

public class CibetPreparedStatementSetParameterIntegrationTest extends JdbcHelper {

   private static Logger log = Logger.getLogger(CibetPreparedStatementSetParameterIntegrationTest.class);

   private static final String INSERT = "insert into tpsentity (id, langstring, bytes, datevalue, timevalue, floatvalue, doublevalue, timestampvalue, onebyte, bool, decimalvalue) "
         + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
   private static final String INSERT2 = "insert into tpsentity (id, langstring, bytes) " + "values (?, ?, ?)";
   private static final String INSERT3 = "insert into tpsentity (id, clobvalue, bytes) " + "values (?, ?, ?)";
   private static final String INSERT4 = "insert into tpsentity (id, nclobvalue, bytes) " + "values (?, ?, ?)";

   private Setpoint sp;

   @Before
   @Override
   public void before() throws IOException, SQLException {
      InitializationService.instance().startContext();
      Context.sessionScope().setUser(USER);
      connection = dataSource.getConnection();
   }

   @After
   public void afterJdbcBridgeEntityManagerIntegrationTest() throws Exception {
      InitializationService.instance().endContext();
      if (sp != null) {
         Configuration.instance().unregisterSetpoint(sp.getId());
      }
   }

   private ResultSet query(String sql) throws Exception {
      PreparedStatement st = connection.prepareStatement(sql);
      boolean b = st.execute();
      Assert.assertEquals(true, b);
      return st.getResultSet();
   }

   private void checkRelease(String sql) throws Exception {
      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      ResultSet rs = query("select * from tpsEntity");
      Assert.assertTrue(!rs.next());

      InitializationService.instance().endContext();
      InitializationService.instance().startContext();

      rs = query("select * from cib_dccontrollable");
      Assert.assertTrue(rs.next());
      Assert.assertEquals("INSERT", rs.getString("CONTROLEVENT"));
      Assert.assertEquals("5", rs.getString("PRIMARYKEYID"));
      Assert.assertEquals("tpsentity", rs.getString("TARGETTYPE"));
      Assert.assertEquals(sql, CibetUtil.decode(rs.getBytes("TARGET")));

      log.debug("now release");
      List<DcControllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());
      DcControllable co = l.get(0);

      Context.sessionScope().setUser("test2");
      int res = (Integer) co.release(new JdbcBridgeEntityManager(connection), null);
      Assert.assertEquals(1, res);

      Context.sessionScope().setUser(USER);
      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());
   }

   private void checkReleaseNull() throws Exception {
      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      ResultSet rs = query("select * from tpsEntity");
      Assert.assertTrue(!rs.next());

      InitializationService.instance().endContext();
      InitializationService.instance().startContext();

      rs = query("select * from cib_dccontrollable");
      Assert.assertTrue(rs.next());
      Assert.assertEquals("INSERT", rs.getString("CONTROLEVENT"));
      Assert.assertEquals("8", rs.getString("PRIMARYKEYID"));
      Assert.assertEquals("tpsentity", rs.getString("TARGETTYPE"));
      Assert.assertEquals(INSERT, CibetUtil.decode(rs.getBytes("TARGET")));

      log.debug("now release");
      List<DcControllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());
      DcControllable co = l.get(0);

      Context.sessionScope().setUser("test2");
      int res = (Integer) co.release(new JdbcBridgeEntityManager(connection), null);
      Assert.assertEquals(1, res);

      Context.sessionScope().setUser(USER);
      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      rs = query("select * from tpsEntity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(8, rs.getLong(1));
      Assert.assertNull(rs.getString(2));
      Assert.assertNull(rs.getBytes(3));
      Assert.assertNull(rs.getDate(4));
      Assert.assertNull(rs.getTime(5));

      log.debug("rs.getFloat(6)=" + rs.getFloat(6));
      Assert.assertTrue(rs.wasNull());
      log.debug("rs.getDouble(7)=" + rs.getDouble(7));
      Assert.assertTrue(rs.wasNull());
      log.debug("timestamp: " + rs.getTimestamp(8));
      log.debug("was NULL: " + rs.wasNull());
      // MySql doen't allow NULL values in Timestamp columns
      // Assert.assertNull(rs.getTimestamp(8));
      log.debug("rs.getByte(9)=" + rs.getByte(9));
      Assert.assertTrue(rs.wasNull());
      log.debug("rs.getBoolean(10)=" + rs.getBoolean(10));
      Assert.assertTrue(rs.wasNull());
      Assert.assertNull(rs.getBigDecimal(11));
   }

   @Test
   public void createDropTable() throws Exception {
      log.debug("start createDropTable() with database");

      sp = registerSetpoint("tasc", FourEyesActuator.DEFAULTNAME, ControlEvent.UPDATE, ControlEvent.DELETE);

      PreparedStatement ps = connection.prepareStatement("create table tasc (id varchar(50) not null)");
      ps.execute();

      ps = connection.prepareStatement("select id from tasc");
      ResultSet rs = ps.executeQuery();
      Assert.assertTrue(!rs.next());

      ps = connection.prepareStatement("insert into tasc (id) values ('Hase')");
      int count = ps.executeUpdate();
      Assert.assertEquals(1, count);

      ps = connection.prepareStatement("select id from tasc");
      rs = ps.executeQuery();
      Assert.assertTrue(rs.next());

      try {
         ps = connection.prepareStatement("truncate table tasc");
         ps.execute();

         ps = connection.prepareStatement("select id from tasc");
         rs = ps.executeQuery();
         Assert.assertTrue(!rs.next());
      } catch (SQLFeatureNotSupportedException e) {
         log.error("SQLFeature truncate not supported: " + e.getMessage(), e);
      }

      ps = connection.prepareStatement("drop table tasc");
      ps.execute();
   }

   @Test
   public void executeUpdate() throws Exception {
      log.debug("start executeUpdate() with database");
      Calendar now = Calendar.getInstance();
      now.set(Calendar.MILLISECOND, 0);

      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      PreparedStatement ps = connection.prepareStatement(INSERT);
      ps.setLong(1, 5);
      ps.setString(2, "l�sen");
      ps.setBytes(3, "Klaus\nKaspar".getBytes());
      ps.setDate(4, new Date(2001, 5, 8));
      ps.setTime(5, new Time(13, 4, 34));
      ps.setFloat(6, (float) 3.67);
      ps.setDouble(7, 123.23);
      ps.setTimestamp(8, new Timestamp(now.getTimeInMillis()));
      ps.setByte(9, "x".getBytes()[0]);
      ps.setBoolean(10, true);
      ps.setBigDecimal(11, new BigDecimal("12.0"));

      int count = ps.executeUpdate();
      Assert.assertEquals(0, count);
      checkRelease(INSERT);

      ResultSet rs = query("select * from tpsEntity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5, rs.getLong(1));
      Assert.assertEquals("l�sen", rs.getString(2));
      Assert.assertEquals("Klaus\nKaspar", new String(rs.getBytes(3)));
      Assert.assertEquals(new Date(2001, 5, 8), rs.getDate(4));
      Assert.assertEquals(new Time(13, 4, 34), rs.getTime(5));
      Assert.assertEquals((float) 3.67, rs.getFloat(6), 0);
      Assert.assertEquals(123.23, rs.getDouble(7), 0);
      // MySql don't stores milliseconds!
      Assert.assertEquals(new Timestamp(now.getTimeInMillis()), rs.getTimestamp(8));
      Assert.assertEquals("x".getBytes()[0], rs.getByte(9));
      Assert.assertEquals(true, rs.getBoolean(10));
      Assert.assertEquals(12.0, rs.getBigDecimal(11).doubleValue(), 0);
   }

   @Test
   public void executeUpdateNull() throws Exception {
      log.debug("start executeUpdateNull() with database");

      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      PreparedStatement ps = connection.prepareStatement(INSERT);
      ps.setLong(1, 8);
      ps.setNull(2, Types.VARCHAR);
      ps.setNull(3, Types.BLOB);
      ps.setNull(4, Types.DATE);
      ps.setNull(5, Types.TIME);
      ps.setNull(6, Types.FLOAT);
      ps.setNull(7, Types.DOUBLE);
      ps.setNull(8, Types.TIMESTAMP);
      ps.setNull(9, Types.SMALLINT);
      ps.setNull(10, Types.CHAR);
      ps.setNull(11, Types.DECIMAL);

      int count = ps.executeUpdate();
      Assert.assertEquals(0, count);
      checkReleaseNull();
   }

   @Test
   public void executeUpdateNullType() throws Exception {
      log.debug("start executeUpdateNullType() with database");

      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      PreparedStatement ps = connection.prepareStatement(INSERT);
      ps.setLong(1, 8);
      ps.setNull(2, Types.VARCHAR, "xx");
      ps.setNull(3, Types.BLOB, "xx");
      ps.setNull(4, Types.DATE, "xx");
      ps.setNull(5, Types.TIME, "xx");
      ps.setNull(6, Types.FLOAT, "xx");
      ps.setNull(7, Types.DOUBLE, "xx");
      ps.setNull(8, Types.TIMESTAMP, "xx");
      ps.setNull(9, Types.SMALLINT, "xx");
      ps.setNull(10, Types.CHAR, "xx");
      ps.setNull(11, Types.DECIMAL, "xx");

      int count = ps.executeUpdate();
      Assert.assertEquals(0, count);
      checkReleaseNull();
   }

   @Test
   public void executeUpdateAsciiStream() throws Exception {
      log.debug("start executeUpdateAsciiStream() with database");
      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      PreparedStatement ps = connection.prepareStatement(INSERT2);
      ps.setLong(1, 5);
      InputStream bais = new ByteArrayInputStream("losen".getBytes("UTF-8"));
      ps.setAsciiStream(2, bais);
      InputStream bais2 = new ByteArrayInputStream("Klaus\nKaspar".getBytes());
      ps.setBinaryStream(3, bais2);

      int count = ps.executeUpdate();
      Assert.assertEquals(0, count);
      checkRelease(INSERT2);

      ResultSet rs = query("select * from tpsEntity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5, rs.getLong(1));
      Assert.assertEquals("losen", IOUtils.toString(rs.getAsciiStream(2)));
      Assert.assertEquals("Klaus\nKaspar", new String(rs.getBytes(3)));
   }

   @Test
   public void executeUpdateAsciiStreamInt() throws Exception {
      log.debug("start executeUpdateAsciiStreamInt() with database");
      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      PreparedStatement ps = connection.prepareStatement(INSERT2);
      ps.setLong(1, 5);
      InputStream bais = new ByteArrayInputStream("losen".getBytes("UTF-8"));
      ps.setAsciiStream(2, bais, 3);
      InputStream bais2 = new ByteArrayInputStream("Klaus\nKaspar".getBytes());
      ps.setBinaryStream(3, bais2, 5);

      int count = ps.executeUpdate();
      Assert.assertEquals(0, count);
      checkRelease(INSERT2);

      ResultSet rs = query("select * from tpsEntity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5, rs.getLong(1));
      log.debug("ascii: " + rs.getString(2));
      log.debug("binary: " + new String(rs.getBytes(3)));
      Assert.assertEquals("los", IOUtils.toString(rs.getAsciiStream(2), "UTF-8"));
      Assert.assertEquals("Klaus", new String(rs.getBytes(3)));
   }

   @Test
   public void executeUpdateAsciiStreamIntNull() throws Exception {
      log.debug("start executeUpdateAsciiStreamIntNull() with database");
      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      PreparedStatement ps = connection.prepareStatement(INSERT2);
      ps.setLong(1, 5);
      ps.setAsciiStream(2, null, 0);
      ps.setBinaryStream(3, null, 0);

      int count = ps.executeUpdate();
      Assert.assertEquals(0, count);
      checkRelease(INSERT2);

      ResultSet rs = query("select * from tpsEntity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5, rs.getLong(1));
      log.debug("ascii: " + rs.getString(2));
      Assert.assertTrue(rs.wasNull());
      log.debug("binary: " + rs.getBytes(3));
      Assert.assertTrue(rs.wasNull());
   }

   @Test
   public void executeUpdateAsciiStreamlong() throws Exception {
      log.debug("start executeUpdateAsciiStreamLong() with database");
      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      PreparedStatement ps = connection.prepareStatement(INSERT2);
      ps.setLong(1, 5);
      InputStream bais = new ByteArrayInputStream("losen".getBytes("UTF-8"));
      ps.setAsciiStream(2, bais, 4L);
      InputStream bais2 = new ByteArrayInputStream("Klaus\nKaspar".getBytes());
      ps.setBinaryStream(3, bais2, 7L);

      int count = ps.executeUpdate();
      Assert.assertEquals(0, count);
      checkRelease(INSERT2);

      ResultSet rs = query("select * from tpsEntity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5, rs.getLong(1));
      log.debug("ascii: " + rs.getString(2));
      log.debug("binary: " + new String(rs.getBytes(3)));
      Assert.assertEquals("lose", IOUtils.toString(rs.getAsciiStream(2), "UTF-8"));
      Assert.assertEquals("Klaus\nK", new String(rs.getBytes(3)));
   }

   @Test
   public void executeUpdateDateCal() throws Exception {
      log.debug("start executeUpdateDateCal() with database");
      java.util.Date now = new java.util.Date();
      Date date = new Date(2001, 5, 8);
      Time time = new Time(13, 4, 34);
      Calendar cal2 = Calendar.getInstance();
      cal2.set(Calendar.HOUR, 13);
      cal2.set(Calendar.MINUTE, 4);
      cal2.set(Calendar.SECOND, 34);
      cal2.setTimeZone(TimeZone.getTimeZone("GMT-8:00"));
      log.debug(cal2.getTime());
      Timestamp ts = new Timestamp(now.getTime());

      log.debug(date + ":::" + date.getTimezoneOffset());
      log.debug(time + ":::" + time.getTimezoneOffset());
      log.debug(ts + ":::" + ts.getTimezoneOffset());
      Calendar cal = Calendar.getInstance();
      cal.setTimeZone(TimeZone.getTimeZone("GMT-8:00"));

      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      PreparedStatement ps = connection.prepareStatement(INSERT);
      ps.setLong(1, 5);
      ps.setString(2, "l�sen");
      ps.setBytes(3, "Klaus\nKaspar".getBytes());
      ps.setDate(4, date, cal);
      ps.setTime(5, time, cal);
      ps.setFloat(6, (float) 3.67);
      ps.setDouble(7, 123.23);
      ps.setTimestamp(8, ts, cal);
      ps.setByte(9, "x".getBytes()[0]);
      ps.setBoolean(10, true);
      ps.setBigDecimal(11, new BigDecimal("12.0"));

      int count = ps.executeUpdate();
      Assert.assertEquals(0, count);
      checkRelease(INSERT);

      ResultSet rs = query("select * from tpsEntity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5, rs.getLong(1));
      Assert.assertEquals("l�sen", rs.getString(2));
      Assert.assertEquals("Klaus\nKaspar", new String(rs.getBytes(3)));
      Date d = (Date) rs.getDate(4);
      // MySql returns other value here:
      log.debug(d + ":::" + d.getTimezoneOffset());
      Assert.assertTrue(new Date(2001, 5, 7).equals(d) || new Date(2001, 5, 8).equals(d));
      Time t = rs.getTime(5);
      log.debug(t + ":::" + t.getTimezoneOffset());
      Assert.assertTrue(t.getTime() <= time.getTime());
      Assert.assertEquals((float) 3.67, rs.getFloat(6), 0);
      Assert.assertEquals(123.23, rs.getDouble(7), 0);
      Timestamp ts2 = rs.getTimestamp(8);
      log.debug(ts2 + ":::" + ts2.getTimezoneOffset());
      Assert.assertTrue(ts2.getTime() < ts.getTime());
      Assert.assertEquals("x".getBytes()[0], rs.getByte(9));
      Assert.assertEquals(true, rs.getBoolean(10));
   }

   @Test
   public void executeUpdateDateCalNull() throws Exception {
      log.debug("start executeUpdateDateCalNull() with database");

      Calendar cal = Calendar.getInstance();
      cal.setTimeZone(TimeZone.getTimeZone("GMT-8:00"));

      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      PreparedStatement ps = connection.prepareStatement(INSERT);
      ps.setLong(1, 5);
      ps.setString(2, "l�sen");
      ps.setBytes(3, "Klaus\nKaspar".getBytes());
      try {
         ps.setDate(4, null, cal);
      } catch (NullPointerException e) {
         ps.setNull(4, Types.DATE);
      }
      try {
         ps.setTime(5, null, cal);
      } catch (NullPointerException e) {
         ps.setNull(5, Types.TIME);
      }
      ps.setFloat(6, (float) 3.67);
      ps.setDouble(7, 123.23);
      try {
         ps.setTimestamp(8, null, cal);
      } catch (NullPointerException e) {
         ps.setNull(8, Types.TIMESTAMP);
      }
      ps.setByte(9, "x".getBytes()[0]);
      ps.setBoolean(10, true);
      ps.setBigDecimal(11, new BigDecimal("12.0"));

      int count = ps.executeUpdate();
      Assert.assertEquals(0, count);
      checkRelease(INSERT);

      ResultSet rs = query("select * from tpsEntity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5, rs.getLong(1));

      Assert.assertNull(rs.getDate(4));
      Assert.assertTrue(rs.wasNull());
      Assert.assertNull(rs.getTime(5));
      Assert.assertTrue(rs.wasNull());
      // if (!database.getName().startsWith("JDBC-RemoteMySql")) {
      // Assert.assertNull(rs.getTimestamp(8));
      // Assert.assertTrue(rs.wasNull());
      // }
   }

   @Test
   public void executeUpdateBlob() throws Exception {
      log.debug("start executeUpdateBlob() with database");
      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      PreparedStatement ps = connection.prepareStatement(INSERT2);
      ps.setLong(1, 5);
      try {
         ps.setNString(2, "l�sen");
      } catch (SQLFeatureNotSupportedException e) {
         log.error("SQLFeature setNString not supported: " + e.getMessage(), e);
         ps.setString(2, "l�sen");
      }
      InputStream bais2 = new ByteArrayInputStream("Klaus\nKaspar".getBytes());
      ps.setBlob(3, bais2);

      int count = ps.executeUpdate();
      Assert.assertEquals(0, count);
      checkRelease(INSERT2);

      ResultSet rs = query("select * from tpsEntity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5, rs.getLong(1));
      log.debug("ascii: " + rs.getString(2));
      log.debug("binary: " + new String(rs.getBytes(3)));
      try {
         Assert.assertEquals("l�sen", rs.getNString(2));
      } catch (SQLFeatureNotSupportedException e) {
      }
      Assert.assertEquals("Klaus\nKaspar", IOUtils.toString(rs.getBlob(3).getBinaryStream()));
   }

   @Test
   public void executeUpdateBlobLong() throws Exception {
      log.debug("start executeUpdateBlobLong() with database");
      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      PreparedStatement ps = connection.prepareStatement(INSERT2);
      ps.setInt(1, 5);
      ps.setObject(2, "l�sen");
      InputStream bais2 = new ByteArrayInputStream("Klaus\nKaspar".getBytes());
      ps.setBlob(3, bais2, 5L);

      int count = ps.executeUpdate();
      Assert.assertEquals(0, count);
      checkRelease(INSERT2);

      ResultSet rs = query("select * from tpsEntity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5, rs.getInt(1));
      log.debug("ascii: " + rs.getString(2));
      log.debug("binary: " + new String(rs.getBytes(3)));
      Assert.assertEquals("l�sen", rs.getObject(2));
      Assert.assertEquals("Klaus", IOUtils.toString(rs.getBlob(3).getBinaryStream()));
   }

   @Test
   public void executeUpdateBlobLongNull() throws Exception {
      log.debug("start executeUpdateBlobLongNull() with database");
      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      PreparedStatement ps = connection.prepareStatement(INSERT2);
      ps.setInt(1, 5);
      ps.setObject(2, "l�sen");
      ps.setBlob(3, null, 0L);

      int count = ps.executeUpdate();
      Assert.assertEquals(0, count);
      checkRelease(INSERT2);

      ResultSet rs = query("select * from tpsEntity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5, rs.getInt(1));
      Assert.assertNull(rs.getBlob(3));
   }

   @Test
   public void executeUpdateObject() throws Exception {
      log.debug("start executeUpdateObject() with database");
      Calendar now = Calendar.getInstance();
      now.set(Calendar.MILLISECOND, 0);

      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      PreparedStatement ps = connection.prepareStatement(INSERT);
      ps.setShort(1, (short) 5);
      InputStream bais = new ByteArrayInputStream("l�sen".getBytes("UTF-8"));
      try {
         ps.setUnicodeStream(2, bais, 6);
      } catch (SQLFeatureNotSupportedException e) {
         log.error("SQLFeature setUnicodeStream not supported: " + e.getMessage(), e);
         ps.setObject(2, "l�sen", Types.VARCHAR, 3);
      }
      ps.setObject(3, "Klaus\nKaspar".getBytes(), Types.BINARY);
      ps.setObject(4, new Date(2001, 5, 8), Types.DATE);
      ps.setTime(5, new Time(13, 4, 34));
      ps.setFloat(6, (float) 3.67);
      ps.setObject(7, 123.23, Types.DOUBLE);
      ps.setTimestamp(8, new Timestamp(now.getTimeInMillis()));
      ps.setByte(9, "x".getBytes()[0]);
      ps.setBoolean(10, true);
      ps.setObject(11, new BigDecimal("12.0456"), Types.DECIMAL, 1);

      int count = ps.executeUpdate();
      Assert.assertEquals(0, count);
      checkRelease(INSERT);

      ResultSet rs = query("select * from tpsEntity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5, rs.getShort(1));
      try {
         Assert.assertEquals("l�sen", IOUtils.toString(rs.getUnicodeStream(2), "UTF-8"));
      } catch (SQLFeatureNotSupportedException e) {
      }
      Assert.assertEquals("Klaus\nKaspar", new String(rs.getBytes(3)));
      Assert.assertEquals(new Date(2001, 5, 8), rs.getDate(4));
      Assert.assertEquals(new Time(13, 4, 34), rs.getTime(5));
      Assert.assertEquals((float) 3.67, rs.getFloat(6), 0);
      Object o = rs.getObject(7);
      if (o instanceof BigDecimal) {
         Assert.assertEquals(123.23, ((BigDecimal) o).doubleValue(), 0);
      } else {
         Assert.assertEquals(123.23, rs.getObject(7));
      }
      Assert.assertEquals(new Timestamp(now.getTimeInMillis()), rs.getTimestamp(8));
      Assert.assertEquals("x".getBytes()[0], rs.getByte(9));
      Assert.assertEquals(true, rs.getBoolean(10));
      Assert.assertEquals(12, ((BigDecimal) rs.getObject(11)).intValue());
   }

   @Test
   public void executeUpdateUnicodeStreamNull() throws Exception {
      log.debug("start executeUpdateUnicodeStreamNull() with database");
      Calendar now = Calendar.getInstance();
      now.set(Calendar.MILLISECOND, 0);

      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      PreparedStatement ps = connection.prepareStatement(INSERT);
      ps.setShort(1, (short) 5);
      try {
         ps.setUnicodeStream(2, null, 0);
      } catch (SQLFeatureNotSupportedException e) {
         log.error("SQLFeature setUnicodeStream not supported: " + e.getMessage(), e);
         ps.setObject(2, null, Types.VARCHAR, 3);
      }
      ps.setObject(3, "Klaus\nKaspar".getBytes(), Types.BINARY);
      ps.setObject(4, new Date(2001, 5, 8), Types.DATE);
      ps.setTime(5, new Time(13, 4, 34));
      ps.setFloat(6, (float) 3.67);
      ps.setObject(7, 123.23, Types.DOUBLE);
      ps.setTimestamp(8, new Timestamp(now.getTimeInMillis()));
      ps.setByte(9, "x".getBytes()[0]);
      ps.setBoolean(10, true);
      ps.setObject(11, new BigDecimal("12.0456"), Types.DECIMAL, 1);

      int count = ps.executeUpdate();
      Assert.assertEquals(0, count);
      checkRelease(INSERT);

      ResultSet rs = query("select * from tpsEntity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5, rs.getShort(1));
      try {
         InputStream rein = rs.getUnicodeStream(2);
         Assert.assertTrue(rs.wasNull());
      } catch (SQLFeatureNotSupportedException e) {
      }
   }

   @Test
   public void executeUpdateUrl() throws Exception {
      log.debug("start executeUpdateUrl() with database");
      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      PreparedStatement ps = connection.prepareStatement(INSERT2);
      ps.setInt(1, 5);
      try {
         ps.setURL(2, new URL("http://www.googleyyyyyyyyyyyyyyyyyy.de?param=aa"));
      } catch (SQLFeatureNotSupportedException e) {
         log.error("SQLFeature setURL not supported: " + e.getMessage(), e);
         ps.setString(2, "http://www.googleyyyyyyyyyyyyyyyyyy.de?param=aa");
      }
      InputStream bais2 = new ByteArrayInputStream("Klaus\nKaspar".getBytes());
      ps.setBlob(3, bais2, 5L);

      int count = ps.executeUpdate();
      Assert.assertEquals(0, count);
      checkRelease(INSERT2);

      ResultSet rs = query("select * from tpsEntity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5, rs.getInt(1));
      log.debug("ascii: " + rs.getString(2));
      log.debug("binary: " + new String(rs.getBytes(3)));
      Assert.assertEquals("http://www.googleyyyyyyyyyyyyyyyyyy.de?param=aa", rs.getObject(2));
      Assert.assertEquals("Klaus", IOUtils.toString(rs.getBlob(3).getBinaryStream()));
   }

   @Test
   public void executeUpdateCharacterStream() throws Exception {
      log.debug("start executeUpdateCharacterStream() with database");
      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      PreparedStatement ps = connection.prepareStatement(INSERT2);
      ps.setLong(1, 5);
      Reader bais = new StringReader("l�sen");
      ps.setCharacterStream(2, bais);
      InputStream bais2 = new ByteArrayInputStream("Klaus\nKaspar".getBytes());
      ps.setBinaryStream(3, bais2);

      int count = ps.executeUpdate();
      Assert.assertEquals(0, count);
      checkRelease(INSERT2);

      ResultSet rs = query("select * from tpsEntity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5, rs.getLong(1));
      Assert.assertEquals("l�sen", IOUtils.toString(rs.getCharacterStream(2)));
      Assert.assertEquals("Klaus\nKaspar", new String(rs.getBytes(3)));
   }

   @Test
   public void executeUpdateCharacterStreamInt() throws Exception {
      log.debug("start executeUpdateCharacterStreamInt() with database");
      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      PreparedStatement ps = connection.prepareStatement(INSERT2);
      ps.setLong(1, 5);
      Reader bais = new StringReader("l�sen");
      ps.setCharacterStream(2, bais, 5);
      InputStream bais2 = new ByteArrayInputStream("Klaus\nKaspar".getBytes());
      ps.setBinaryStream(3, bais2);

      int count = ps.executeUpdate();
      Assert.assertEquals(0, count);
      checkRelease(INSERT2);

      ResultSet rs = query("select * from tpsEntity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5, rs.getLong(1));
      Assert.assertEquals("l�sen", IOUtils.toString(rs.getCharacterStream(2)));
      Assert.assertEquals("Klaus\nKaspar", new String(rs.getBytes(3)));
   }

   @Test
   public void executeUpdateCharacterStreamIntNull() throws Exception {
      log.debug("start executeUpdateCharacterStreamIntNull() with database");
      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      PreparedStatement ps = connection.prepareStatement(INSERT2);
      ps.setLong(1, 5);
      ps.setCharacterStream(2, null, 0);
      ps.setBinaryStream(3, null);

      int count = ps.executeUpdate();
      Assert.assertEquals(0, count);
      checkRelease(INSERT2);

      ResultSet rs = query("select * from tpsEntity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5, rs.getLong(1));
      rs.getCharacterStream(2);
      Assert.assertTrue(rs.wasNull());
      rs.getBytes(3);
      Assert.assertTrue(rs.wasNull());
   }

   @Test
   public void executeUpdateCharacterStreamLong() throws Exception {
      log.debug("start executeUpdateCharacterStreamLong() with database");
      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      PreparedStatement ps = connection.prepareStatement(INSERT2);
      ps.setLong(1, 5);
      Reader bais = new StringReader("l�sen");
      ps.setCharacterStream(2, bais, 5L);
      InputStream bais2 = new ByteArrayInputStream("Klaus\nKaspar".getBytes());
      ps.setBinaryStream(3, bais2);

      int count = ps.executeUpdate();
      Assert.assertEquals(0, count);
      checkRelease(INSERT2);

      ResultSet rs = query("select * from tpsEntity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5, rs.getLong(1));
      Assert.assertEquals("l�sen", IOUtils.toString(rs.getCharacterStream(2)));
      Assert.assertEquals("Klaus\nKaspar", new String(rs.getBytes(3)));
   }

   @Test
   public void executeUpdateClob() throws Exception {
      log.debug("start executeUpdateClob() with database");
      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      PreparedStatement ps = connection.prepareStatement(INSERT3);
      ps.setLong(1, 5);
      Reader bais = new StringReader("l�sen");
      ps.setClob(2, bais);
      InputStream bais2 = new ByteArrayInputStream("Klaus\nKaspar".getBytes());
      ps.setBinaryStream(3, bais2);

      int count = ps.executeUpdate();
      Assert.assertEquals(0, count);
      checkRelease(INSERT3);

      ResultSet rs = query("select * from tpsEntity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5, rs.getLong(1));
      Assert.assertEquals("l�sen", IOUtils.toString(rs.getClob(12).getCharacterStream()));
      Assert.assertEquals("Klaus\nKaspar", new String(rs.getBytes(3)));
   }

   @Test
   public void executeUpdateClobLong() throws Exception {
      log.debug("start executeUpdateClobLong() with database");
      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      PreparedStatement ps = connection.prepareStatement(INSERT3);
      ps.setLong(1, 5);
      Reader bais = new StringReader("l�sen");
      ps.setClob(2, bais, 5L);
      InputStream bais2 = new ByteArrayInputStream("Klaus\nKaspar".getBytes());
      ps.setBinaryStream(3, bais2);

      int count = ps.executeUpdate();
      Assert.assertEquals(0, count);
      checkRelease(INSERT3);

      ResultSet rs = query("select * from tpsEntity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5, rs.getLong(1));
      Assert.assertEquals("l�sen", IOUtils.toString(rs.getClob(12).getCharacterStream()));
      Assert.assertEquals("Klaus\nKaspar", new String(rs.getBytes(3)));
   }

   @Test
   public void executeUpdateClobLongNull() throws Exception {
      log.debug("start executeUpdateClobLongNull() with database");
      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      PreparedStatement ps = connection.prepareStatement(INSERT3);
      ps.setLong(1, 5);
      ps.setClob(2, null, 0L);
      InputStream bais2 = new ByteArrayInputStream("Klaus\nKaspar".getBytes());
      ps.setBinaryStream(3, bais2);

      int count = ps.executeUpdate();
      Assert.assertEquals(0, count);
      checkRelease(INSERT3);

      ResultSet rs = query("select * from tpsEntity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5, rs.getLong(1));
      Clob clob = rs.getClob(12);
      if (clob != null) {
         log.debug("clob12=" + IOUtils.toString(clob.getCharacterStream()));
         Assert.assertEquals(0, IOUtils.toString(clob.getCharacterStream()).length());
      } else {
         Assert.assertNull(clob);
         Assert.assertTrue(rs.wasNull());
      }
   }

   @Test
   public void executeUpdateNCharacterStream() throws Exception {
      log.debug("start executeUpdateNCharacterStream() with database");
      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      try {
         PreparedStatement ps = connection.prepareStatement(INSERT2);
         ps.setLong(1, 5);
         Reader bais = new StringReader("l�sen");
         ps.setNCharacterStream(2, bais);
         InputStream bais2 = new ByteArrayInputStream("Klaus\nKaspar".getBytes());
         ps.setBinaryStream(3, bais2);

         int count = ps.executeUpdate();
         Assert.assertEquals(0, count);
         checkRelease(INSERT2);

         ResultSet rs = query("select * from tpsEntity");
         Assert.assertTrue(rs.next());
         Assert.assertEquals(5, rs.getLong(1));
         Assert.assertEquals("l�sen", IOUtils.toString(rs.getNCharacterStream(2)));
         Assert.assertEquals("Klaus\nKaspar", new String(rs.getBytes(3)));
      } catch (SQLFeatureNotSupportedException e) {
         log.error("SQLFeature setNCharacterStream not supported: " + e.getMessage(), e);
      }
   }

   @Test
   public void executeUpdateNCharacterStreamLong() throws Exception {
      log.debug("start executeUpdateNCharacterStreamLong() with database");
      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      try {
         PreparedStatement ps = connection.prepareStatement(INSERT2);
         ps.setLong(1, 5);
         Reader bais = new StringReader("l�sen");
         ps.setNCharacterStream(2, bais, 5);
         InputStream bais2 = new ByteArrayInputStream("Klaus\nKaspar".getBytes());
         ps.setBinaryStream(3, bais2);

         int count = ps.executeUpdate();
         Assert.assertEquals(0, count);
         checkRelease(INSERT2);

         ResultSet rs = query("select * from tpsEntity");
         Assert.assertTrue(rs.next());
         Assert.assertEquals(5, rs.getLong(1));
         Assert.assertEquals("l�sen", IOUtils.toString(rs.getNCharacterStream(2)));
         Assert.assertEquals("Klaus\nKaspar", new String(rs.getBytes(3)));
      } catch (SQLFeatureNotSupportedException e) {
         log.error("SQLFeature setNCharacterStream long not supported: " + e.getMessage(), e);
      }
   }

   @Test
   public void executeUpdateNClob() throws Exception {
      log.debug("start executeUpdateNClob() with database");
      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      try {
         PreparedStatement ps = connection.prepareStatement(INSERT4);
         ps.setLong(1, 5);
         Reader bais = new StringReader("l�sen");
         ps.setNClob(2, bais);
         InputStream bais2 = new ByteArrayInputStream("Klaus\nKaspar".getBytes());
         ps.setBinaryStream(3, bais2);

         int count = ps.executeUpdate();
         Assert.assertEquals(0, count);
         checkRelease(INSERT4);

         ResultSet rs = query("select * from tpsEntity");
         Assert.assertTrue(rs.next());
         Assert.assertEquals(5, rs.getLong(1));
         Assert.assertEquals("l�sen", IOUtils.toString(rs.getNClob(13).getCharacterStream()));
         Assert.assertEquals("Klaus\nKaspar", new String(rs.getBytes(3)));
      } catch (SQLFeatureNotSupportedException e) {
         log.error("SQLFeature setNClob not supported: " + e.getMessage(), e);
      }
   }

   @Test
   public void executeUpdateNClobLong() throws Exception {
      log.debug("start executeUpdateNClobLong() with database");
      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      try {
         PreparedStatement ps = connection.prepareStatement(INSERT4);
         ps.setLong(1, 5);
         Reader bais = new StringReader("l�sen");
         ps.setNClob(2, bais, 5L);
         InputStream bais2 = new ByteArrayInputStream("Klaus\nKaspar".getBytes());
         ps.setBinaryStream(3, bais2);

         int count = ps.executeUpdate();
         Assert.assertEquals(0, count);
         checkRelease(INSERT4);

         ResultSet rs = query("select * from tpsEntity");
         Assert.assertTrue(rs.next());
         Assert.assertEquals(5, rs.getLong(1));
         Assert.assertEquals("l�sen", IOUtils.toString(rs.getNClob(13).getCharacterStream()));
         Assert.assertEquals("Klaus\nKaspar", new String(rs.getBytes(3)));
      } catch (SQLFeatureNotSupportedException e) {
         log.error("SQLFeature setNClob long not supported: " + e.getMessage(), e);
      }
   }

   @Test
   public void executeUpdateNClobLongNull() throws Exception {
      log.debug("start executeUpdateNClobLongNull() with database");
      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      try {
         PreparedStatement ps = connection.prepareStatement(INSERT4);
         ps.setLong(1, 5);
         ps.setNClob(2, null, 0L);
         InputStream bais2 = new ByteArrayInputStream("Klaus\nKaspar".getBytes());
         ps.setBinaryStream(3, bais2);

         int count = ps.executeUpdate();
         Assert.assertEquals(0, count);
         checkRelease(INSERT4);

         ResultSet rs = query("select * from tpsEntity");
         Assert.assertTrue(rs.next());
         Assert.assertEquals(5, rs.getLong(1));
         Assert.assertNull(rs.getNClob(13));
      } catch (SQLFeatureNotSupportedException e) {
         log.error("SQLFeature setNClob long not supported: " + e.getMessage(), e);
      }
   }

   @Test
   public void executeUpdateBlobSimple() throws Exception {
      log.debug("start executeUpdateBlobSimple() with database");
      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      PreparedStatement ps = connection.prepareStatement(INSERT2);
      ps.setLong(1, 5);
      ps.setString(2, "l�sen");

      StringBuffer b = new StringBuffer();
      for (int i = 0; i < 20; i++) {
         b.append(INSERT);
         b.append(INSERT2);
         b.append(INSERT3);
         b.append(INSERT4);
      }
      byte[] bytes = b.toString().getBytes();
      log.debug("bytes.length=" + bytes.length);
      String str1 = new String(bytes);
      log.debug(str1);
      Blob blob = new SerialBlob(bytes);
      ps.setBlob(3, blob);

      int count = ps.executeUpdate();
      Assert.assertEquals(0, count);
      checkRelease(INSERT2);

      ResultSet rs = query("select * from tpsEntity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5, rs.getLong(1));
      log.debug("ascii: " + rs.getString(2));
      Assert.assertEquals("l�sen", rs.getString(2));
      byte bytes2[] = new byte[bytes.length];
      InputStream b2 = rs.getBlob(3).getBinaryStream();
      b2.read(bytes2);
      String str2 = new String(bytes2);
      log.debug(str2);
      Assert.assertEquals(str1, str2);
   }

   @Test
   public void executeUpdateBlobSimpleNull() throws Exception {
      log.debug("start executeUpdateBlobSimpleNull() with database");
      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      PreparedStatement ps = connection.prepareStatement(INSERT2);
      ps.setLong(1, 5);
      ps.setString(2, "l�sen");
      ps.setBlob(3, (Blob) null);

      int count = ps.executeUpdate();
      Assert.assertEquals(0, count);
      checkRelease(INSERT2);

      ResultSet rs = query("select * from tpsEntity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5, rs.getLong(1));
      log.debug("ascii: " + rs.getString(2));
      Assert.assertEquals("l�sen", rs.getString(2));
      Assert.assertNull(rs.getBlob(3));
      Assert.assertTrue(rs.wasNull());
   }

   @Test
   public void executeUpdateClobSimple() throws Exception {
      log.debug("start executeUpdateClobSimple() with database");
      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      PreparedStatement ps = connection.prepareStatement(INSERT3);
      ps.setLong(1, 5);

      StringBuffer b = new StringBuffer();
      for (int i = 0; i < 20; i++) {
         b.append(INSERT);
         b.append(INSERT2);
         b.append(INSERT3);
         b.append(INSERT4);
      }
      char[] chars = b.toString().toCharArray();
      log.debug("chars.length=" + chars.length);
      String str1 = new String(chars);
      log.debug(str1);
      Clob clob = new SerialClob(chars);
      ps.setClob(2, clob);

      InputStream bais2 = new ByteArrayInputStream("Klaus\nKaspar".getBytes());
      ps.setBinaryStream(3, bais2);

      int count = ps.executeUpdate();
      Assert.assertEquals(0, count);
      checkRelease(INSERT3);

      ResultSet rs = query("select * from tpsEntity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5, rs.getLong(1));

      char char2[] = new char[chars.length];
      Reader reader = rs.getClob(12).getCharacterStream();
      reader.read(char2);
      String str2 = new String(char2);
      log.debug(str2);
      Assert.assertEquals(str1, str2);

      Assert.assertEquals("Klaus\nKaspar", new String(rs.getBytes(3)));
   }

   @Test
   public void executeUpdateClobSimpleNull() throws Exception {
      log.debug("start executeUpdateClobSimpleNull() with database");
      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      PreparedStatement ps = connection.prepareStatement(INSERT3);
      ps.setLong(1, 5);
      ps.setClob(2, (Clob) null);

      InputStream bais2 = new ByteArrayInputStream("Klaus\nKaspar".getBytes());
      ps.setBinaryStream(3, bais2);

      int count = ps.executeUpdate();
      Assert.assertEquals(0, count);
      checkRelease(INSERT3);

      ResultSet rs = query("select * from tpsEntity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5, rs.getLong(1));
      Assert.assertNull(rs.getClob(12));
      Assert.assertTrue(rs.wasNull());
   }

   @Test
   public void executeUpdateNClobSimple() throws Exception {
      log.debug("start executeUpdateNClobSimple() with database");
      sp = registerSetpoint("tpsentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT);

      PreparedStatement ps = connection.prepareStatement(INSERT4);
      ps.setLong(1, 5);

      StringBuffer b = new StringBuffer();
      for (int i = 0; i < 20; i++) {
         b.append(INSERT);
         b.append(INSERT2);
         b.append(INSERT3);
         b.append(INSERT4);
      }
      char[] chars = b.toString().toCharArray();
      log.debug("chars.length=" + chars.length);
      String str1 = new String(chars);
      log.debug(str1);
      try {
         NClob clob = connection.createNClob();
         Writer out = clob.setCharacterStream(1L);
         out.write(chars);
         out.flush();
         out.close();
         ps.setNClob(2, clob);

         InputStream bais2 = new ByteArrayInputStream("Klaus\nKaspar".getBytes());
         ps.setBinaryStream(3, bais2);

         int count = ps.executeUpdate();
         Assert.assertEquals(0, count);
         checkRelease(INSERT4);

         ResultSet rs = query("select * from tpsEntity");
         Assert.assertTrue(rs.next());
         Assert.assertEquals(5, rs.getLong(1));

         char char2[] = new char[chars.length];
         Reader reader = rs.getNClob(13).getCharacterStream();
         reader.read(char2);
         String str2 = new String(char2);
         log.debug(str2);
         Assert.assertEquals(str1, str2);

         Assert.assertEquals("Klaus\nKaspar", new String(rs.getBytes(3)));
      } catch (SQLFeatureNotSupportedException e) {
      }
   }

}
