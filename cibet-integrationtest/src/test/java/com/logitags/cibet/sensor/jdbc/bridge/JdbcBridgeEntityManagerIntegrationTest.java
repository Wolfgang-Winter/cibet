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

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cibethelper.base.CibetTestDataSource;
import com.cibethelper.base.JdbcHelper;
import com.cibethelper.entities.TPSEntity;
import com.cibethelper.entities.TPSEntityDefinition;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.sensor.jdbc.def.PseudoEntityDefinition;
import com.logitags.cibet.sensor.jdbc.driver.CibetJdbcException;

public class JdbcBridgeEntityManagerIntegrationTest extends JdbcHelper {

   private static Logger log = Logger.getLogger(JdbcBridgeEntityManagerIntegrationTest.class);

   @BeforeClass
   public static void beforeClassJdbcBridgeEntityManagerIntegrationTest() {
      log.info("before class beforeClassJdbcBridgeEntityManagerIntegrationTest()");
   }

   @Before
   public void beforeJdbcBridgeEntityManagerIntegrationTest() throws Exception {
      Context.start();
   }

   @After
   public void afterJdbcBridgeEntityManagerIntegrationTest() throws Exception {
      Context.end();
   }

   @Test(expected = IllegalArgumentException.class)
   public void registerEntityDefinition() {
      JdbcBridgeEntityManager.registerEntityDefinition(null, null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void constructJdbcBridgeEntityManager() {
      new JdbcBridgeEntityManager((DataSource) null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void constructJdbcBridgeEntityManager2() {
      new JdbcBridgeEntityManager((Connection) null);
   }

   @Test
   public void constructJdbcBridgeEntityManagerDoubles() throws Exception {
      Field f = JdbcBridgeEntityManager.class.getDeclaredField("isRegistered");
      f.setAccessible(true);

      // Connection conn = (Connection) Context.requestScope().getEntityManager().getDelegate();
      EntityManager em = new JdbcBridgeEntityManager(connection);
      Assert.assertTrue(f.getBoolean(em));

      EntityManager em2 = new JdbcBridgeEntityManager(connection);
      Assert.assertTrue(f.getBoolean(em2));
   }

   @Test
   public void createNativeQuery() throws Exception {
      EntityManager eman = Context.internalRequestScope().getOrCreateEntityManager(false);
      Query q = eman.createNativeQuery("INSERT INTO cib_syntetic2entity " + "(ID) VALUES (?)");
      q.setParameter(1, "sasa121");
      int count = q.executeUpdate();
      Assert.assertEquals(1, count);

      Connection conn = (Connection) Context.internalRequestScope().getOrCreateEntityManager(true).getDelegate();
      Statement st = conn.createStatement();
      ResultSet rs = st.executeQuery("select id from cib_syntetic2entity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals("sasa121", rs.getString(1));

      Query q2 = eman.createNativeQuery("INSERT INTO cib_syntetic2entity " + "(ID) VALUES (?)", String.class);
      q2.setParameter(1, "sasa122");
      count = q2.executeUpdate();
      Assert.assertEquals(1, count);

      Query q3 = eman.createNativeQuery("INSERT INTO cib_syntetic2entity " + "(ID) VALUES (?)", "String");
      q3.setParameter(1, "sasa123");
      count = q3.executeUpdate();
      Assert.assertEquals(1, count);
   }

   @Test(expected = IllegalArgumentException.class)
   public void constructJdbcBridgeEntityManager3() throws SQLException {
      DataSource ds = new CibetTestDataSource();
      Connection conn = ds.getConnection();
      conn.close();
      new JdbcBridgeEntityManager(conn);
   }

   @Test(expected = CibetJdbcException.class)
   public void find() throws SQLException {
      Connection conn = (Connection) Context.internalRequestScope().getOrCreateEntityManager(false).getDelegate();
      EntityManager em = new JdbcBridgeEntityManager(conn);
      em.find(this.getClass(), "xx");
   }

   @Test(expected = CibetJdbcException.class)
   public void merge() throws SQLException {
      Connection conn = (Connection) Context.internalRequestScope().getOrCreateEntityManager(false).getDelegate();
      EntityManager em = new JdbcBridgeEntityManager(conn);
      em.merge(this);
   }

   @Test(expected = IllegalArgumentException.class)
   public void merge2() throws SQLException {
      Connection conn = (Connection) Context.internalRequestScope().getOrCreateEntityManager(false).getDelegate();
      EntityManager em = new JdbcBridgeEntityManager(conn);
      em.merge(null);
   }

   @Test(expected = CibetJdbcException.class)
   public void persist() throws SQLException {
      Connection conn = (Connection) Context.internalRequestScope().getOrCreateEntityManager(false).getDelegate();
      EntityManager em = new JdbcBridgeEntityManager(conn);
      em.persist(this);
   }

   @Test(expected = IllegalArgumentException.class)
   public void persist2() throws SQLException {
      Connection conn = (Connection) Context.internalRequestScope().getOrCreateEntityManager(false).getDelegate();
      EntityManager em = new JdbcBridgeEntityManager(conn);
      em.persist(null);
   }

   @Test(expected = CibetJdbcException.class)
   public void remove() throws SQLException {
      Connection conn = (Connection) Context.internalRequestScope().getOrCreateEntityManager(false).getDelegate();
      EntityManager em = new JdbcBridgeEntityManager(conn);
      em.remove(this);
   }

   @Test(expected = IllegalArgumentException.class)
   public void remove2() throws SQLException {
      Connection conn = (Connection) Context.internalRequestScope().getOrCreateEntityManager(false).getDelegate();
      EntityManager em = new JdbcBridgeEntityManager(conn);
      em.remove(null);
   }

   @Test(expected = CibetJdbcException.class)
   public void getTransactionCommit() throws SQLException {
      DataSource ds = new CibetTestDataSource();
      EntityManager em = new JdbcBridgeEntityManager(ds);
      EntityTransaction tr = em.getTransaction();
      tr.commit();
   }

   @Test
   public void getTransactionIsActive() throws SQLException {
      DataSource ds = new CibetTestDataSource();
      EntityManager em = new JdbcBridgeEntityManager(ds);
      EntityTransaction tr = em.getTransaction();
      boolean b = tr.isActive();
      Assert.assertEquals(false, b);
   }

   @Test(expected = CibetJdbcException.class)
   public void getTransactionRollback() throws SQLException {
      DataSource ds = new CibetTestDataSource();
      EntityManager em = new JdbcBridgeEntityManager(ds);
      EntityTransaction tr = em.getTransaction();
      tr.rollback();
   }

   @Test
   public void getResultListQuery() throws SQLException {
      DataSource ds = new CibetTestDataSource();
      JdbcBridgeEntityManager em = new JdbcBridgeEntityManager(ds);
      Query q = new JdbcBridgeQuery(em, "xxx");
      try {
         q.getResultList();
         Assert.fail();
      } catch (CibetJdbcException e) {
         Assert.assertTrue(e.getMessage().startsWith("no EntityDefinition registered for name"));
      }
   }

   @Test
   public void getResultListQuery2() throws SQLException {
      Connection conn = (Connection) Context.internalRequestScope().getOrCreateEntityManager(false).getDelegate();

      Statement st = conn.createStatement();
      int count = st.executeUpdate(
            "INSERT into cib_resource(resourceid, resourcetype, encrypted) VALUES ('X1', 'JpaResource', 0)");
      Assert.assertEquals(1, count);

      count = st.executeUpdate(
            "INSERT INTO cib_archive (archiveid, caseid, controlevent, executionstatus, createuser, tenant, version, resourceid)"
                  + " VALUES ('5', null, 'UPDATE', 'EXECUTED', 'ichtest', 'tenich', 1, 'X1')");
      Assert.assertEquals(1, count);

      JdbcBridgeEntityManager em = new JdbcBridgeEntityManager(conn);
      Query q = em.createNamedQuery(Archive.SEL_ALL_BY_CASEID);
      q.setParameter("tenant", "tenich");
      q.setParameter("caseId", null);
      List<?> list = q.getResultList();
      Assert.assertEquals(0, list.size());
   }

   @Test
   public void getResultListQueryDate() throws SQLException {
      Date date = new Date(1350856800000L);
      Time time = new Time(70602000);
      Calendar cal = Calendar.getInstance();
      cal.set(Calendar.MILLISECOND, 0);
      Timestamp timestamp = new Timestamp(cal.getTimeInMillis());

      String sql = "INSERT INTO tpsentity (id, datevalue, timevalue, timestampvalue) " + "VALUES (?,?,?,?)";
      Connection conn = (Connection) Context.internalRequestScope().getOrCreateEntityManager(false).getDelegate();
      JdbcBridgeEntityManager em = new JdbcBridgeEntityManager(conn);
      Query q1 = em.createNativeQuery(sql);
      q1.setParameter("1", 445);
      q1.setParameter("2", date);
      q1.setParameter("3", time);
      q1.setParameter("4", timestamp);
      int count = q1.executeUpdate();
      Assert.assertEquals(1, count);

      String selSQL = "SELECT id, datevalue, timevalue, timestampvalue from tpsentity" + " WHERE timestampvalue = ?";
      JdbcBridgeEntityManager.registerEntityDefinition(TPSEntity.class, new TPSEntityDefinition());
      Query q = em.createNativeQuery(selSQL);
      q.setParameter("t", timestamp);
      List<TPSEntity> list = q.getResultList();
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(timestamp, list.get(0).getTimestamp());

      selSQL = "SELECT id, datevalue, timevalue, timestampvalue from tpsentity" + " WHERE timevalue = ?";
      q = em.createNativeQuery(selSQL);
      q.setParameter("t", time);
      list = q.getResultList();
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(time.getTime(), list.get(0).getTime().getTime());

      selSQL = "SELECT id, datevalue, timevalue, timestampvalue from tpsentity" + " WHERE datevalue = ?";
      q = em.createNativeQuery(selSQL);
      q.setParameter("t", date);
      list = q.getResultList();
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(date.getTime(), list.get(0).getDate().getTime());
   }

   @Test(expected = NonUniqueResultException.class)
   public void getSingleResult() throws SQLException {
      Date date = new Date(1350856800000L);
      Time time = new Time(70602000);
      Timestamp timestamp = new Timestamp(new java.util.Date().getTime());

      String sql = "INSERT INTO tpsentity (id, datevalue, timevalue, timestampvalue) " + "VALUES (?,?,?,?)";
      Connection conn = (Connection) Context.internalRequestScope().getOrCreateEntityManager(false).getDelegate();

      PreparedStatement st = conn.prepareStatement(sql);
      st.setLong(1, 445);
      st.setDate(2, date);
      st.setTime(3, time);
      st.setTimestamp(4, timestamp);
      int count = st.executeUpdate();
      Assert.assertEquals(1, count);
      st.setLong(1, 446);
      st.setDate(2, date);
      st.setTime(3, time);
      st.setTimestamp(4, timestamp);
      count = st.executeUpdate();
      Assert.assertEquals(1, count);

      String selSQL = "SELECT id, datevalue, timevalue, timestampvalue from tpsentity" + " WHERE timestampvalue = ?";
      JdbcBridgeEntityManager.registerEntityDefinition(TPSEntity.class, new TPSEntityDefinition());
      JdbcBridgeEntityManager em = new JdbcBridgeEntityManager(conn);
      Query q = em.createNativeQuery(selSQL);
      q.setParameter("t", timestamp);
      q.getSingleResult();
   }

   @Test(expected = NoResultException.class)
   public void getSingleResultNoResult() throws SQLException {
      Timestamp timestamp = new Timestamp(new java.util.Date().getTime());

      Connection conn = (Connection) Context.internalRequestScope().getOrCreateEntityManager(false).getDelegate();

      String selSQL = "SELECT id, datevalue, timevalue, timestampvalue from tpsentity" + " WHERE timestampvalue = ?";
      JdbcBridgeEntityManager.registerEntityDefinition(TPSEntity.class, new TPSEntityDefinition());
      JdbcBridgeEntityManager em = new JdbcBridgeEntityManager(conn);
      Query q = em.createNativeQuery(selSQL);
      q.setParameter("t", timestamp);
      q.getSingleResult();
   }

   @Test
   public void executeUpdateNull() throws SQLException {
      String sql2 = "INSERT into cib_resource(resourceid, resourcetype, encrypted) VALUES ('X1', 'JpaResource', 0)";
      String sql = "INSERT INTO cib_archive (archiveid, caseid, controlevent, executionstatus, createuser, tenant, version, resourceid)"
            + " VALUES ('5', ?, 'UPDATE', 'EXECUTED', 'ichtest', 'tenich', 1, 'X1')";
      JdbcBridgeEntityManager.registerEntityDefinition(this.getClass(), new PseudoEntityDefinition(sql));
      JdbcBridgeEntityManager.registerEntityDefinition(this.getClass(), new PseudoEntityDefinition(sql2));

      Connection conn = (Connection) Context.internalRequestScope().getOrCreateEntityManager(false).getDelegate();
      JdbcBridgeEntityManager em = new JdbcBridgeEntityManager(conn);
      Query q1 = em.createNativeQuery(sql2);
      int count = q1.executeUpdate();
      Assert.assertEquals(1, count);

      q1 = em.createNativeQuery(sql);
      q1.setParameter("1", null);
      count = q1.executeUpdate();
      Assert.assertEquals(1, count);

      Query q = em.createNamedQuery(Archive.SEL_ALL_BY_CASEID);
      q.setParameter("tenant", "tenich");
      q.setParameter("caseId", null);
      List<?> list = q.getResultList();
      Assert.assertEquals(0, list.size());
   }

   @Test
   public void executeUpdate() throws SQLException {
      DataSource ds = new CibetTestDataSource();
      JdbcBridgeEntityManager em = new JdbcBridgeEntityManager(ds);
      Query q = new JdbcBridgeQuery(em, "xxx");
      try {
         q.executeUpdate();
         Assert.fail();
      } catch (CibetJdbcException e) {
         Assert.assertTrue(e.getMessage().startsWith("no EntityDefinition registered for name"));
      }
   }

   @Test
   public void getResultListQueryBytes() throws SQLException {
      String sql = "INSERT INTO tpsentity (id, bytes) VALUES (?,?)";
      StringBuffer b = new StringBuffer();
      for (int i = 1; i < 20; i++) {
         b.append(sql + i);
      }
      String teststring = b.toString();
      byte[] bytes = teststring.getBytes();

      JdbcBridgeEntityManager.registerEntityDefinition(TPSEntity.class, new TPSEntityDefinition());
      Connection conn = (Connection) Context.internalRequestScope().getOrCreateEntityManager(false).getDelegate();
      JdbcBridgeEntityManager em = new JdbcBridgeEntityManager(conn);
      Query q1 = em.createNativeQuery(sql);
      q1.setParameter("1", 448);
      q1.setParameter("2", bytes);
      int count = q1.executeUpdate();
      Assert.assertEquals(1, count);

      String selSQL = "SELECT bytes from tpsentity WHERE id = 448";
      Statement st = conn.createStatement();
      ResultSet rs = st.executeQuery(selSQL);
      Assert.assertTrue(rs.next());
      byte[] bytes2 = rs.getBytes(1);
      String str2 = new String(bytes2);
      Assert.assertEquals(teststring, str2);
   }
}
