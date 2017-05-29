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

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import com.cibethelper.SpringTestAuthenticationManager;
import com.cibethelper.base.JdbcHelper;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.archive.ArchiveLoader;
import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.actuator.common.DeniedException;
import com.logitags.cibet.actuator.common.InvalidUserException;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.actuator.dc.FourEyesActuator;
import com.logitags.cibet.actuator.dc.SixEyesActuator;
import com.logitags.cibet.actuator.dc.TwoManRuleActuator;
import com.logitags.cibet.actuator.lock.LockActuator;
import com.logitags.cibet.actuator.lock.Locker;
import com.logitags.cibet.actuator.springsecurity.SpringSecurityActuator;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalSessionScope;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.sensor.jdbc.bridge.JdbcBridgeEntityManager;

public class CibetPreparedStatementExUpIntegrationTest extends JdbcHelper {

   private static Logger log = Logger.getLogger(CibetPreparedStatementExUpIntegrationTest.class);

   private static final String INSERT = "insert into cib_testentity (id, nameValue, counter, userid, owner) "
         + "values (?, ?, ?, 'Klaus', ?)";

   private static final String UPDATE = "update cib_testentity set nameValue=?, counter=? "
         + "WHERE id=? and owner='Lalla'";

   private static final String DELETE = "delete from cib_testentity WHERE id=? ";

   private static final String SELECT = "select * from cib_testentity WHERE id=? ";

   @Before
   @Override
   public void before() throws IOException, SQLException {
      Context.start();
      Context.sessionScope().setUser(USER);
      connection = dataSource.getConnection();
   }

   @After
   public void afterJdbcBridgeEntityManagerIntegrationTest() throws Exception {
      Context.end();
   }

   protected void insert(int expectedCount) throws Exception {
      PreparedStatement ps = connection.prepareStatement(INSERT);
      ps.setShort(1, (short) 5);
      ps.setString(2, "rosen");
      ps.setInt(3, 255);
      ps.setString(4, "Lalla");

      int count = ps.executeUpdate();
      Assert.assertEquals(expectedCount, count);
   }

   protected void insert(short id, PreparedStatement ps) throws Exception {
      ps.setShort(1, id);
      ps.setString(2, "rosen");
      ps.setInt(3, 255);
      ps.setString(4, "Lalla");
      int count = ps.executeUpdate();
      Assert.assertEquals(1, count);
   }

   protected void update(int expectedCount) throws Exception {
      PreparedStatement ps = connection.prepareStatement(UPDATE);
      ps.setString(1, "Röschen");
      ps.setInt(2, 99);
      ps.setInt(3, 5);

      int count = ps.executeUpdate();
      Assert.assertEquals(expectedCount, count);
   }

   protected void delete(int expectedCount) throws Exception {
      PreparedStatement ps = connection.prepareStatement(DELETE);
      ps.setInt(1, 5);

      int count = ps.executeUpdate();
      Assert.assertEquals(expectedCount, count);
   }

   private ResultSet query(String sql) throws Exception {
      PreparedStatement st = connection.prepareStatement(sql);
      boolean b = st.execute();
      Assert.assertEquals(true, b);
      return st.getResultSet();
   }

   protected void authenticate(String... roles) throws AuthenticationException {
      SpringTestAuthenticationManager authManager = new SpringTestAuthenticationManager();
      for (String role : roles) {
         authManager.addAuthority(role);
      }

      Authentication request = new UsernamePasswordAuthenticationToken("test", "test");
      Authentication result = authManager.authenticate(request);
      SecurityContextHolder.getContext().setAuthentication(result);
   }

   protected void authenticateSecond(String... roles) throws AuthenticationException {
      SpringTestAuthenticationManager authManager = new SpringTestAuthenticationManager();
      for (String role : roles) {
         authManager.addAuthority(role);
      }

      Authentication request = new UsernamePasswordAuthenticationToken("test", "test");
      Authentication result = authManager.authenticate(request);
      Context.internalSessionScope().setProperty(InternalSessionScope.SECOND_PRINCIPAL, result);
   }

   @Test
   public void executeUpdateNoSetpointPS() throws Exception {
      log.debug("start executeUpdateNoSetpointPS()");
      PreparedStatement ps = connection
            .prepareStatement("insert into cib_testentity (id, nameValue, counter, userid, owner) "
                  + "values (5, 'rosen', 255, 'Klaus', 'Lalla')");
      int count = ps.executeUpdate();
      Assert.assertEquals(1, count);
      ps = connection.prepareStatement("insert into cib_testentity (id, nameValue, counter, userid, owner) "
            + "values (6, 'rosen', 255, 'Klaus', 'Lalla')");
      boolean b = ps.execute();
      Assert.assertEquals(false, b);
      Assert.assertEquals(1, ps.getUpdateCount());
   }

   @Test
   public void executeUpdateNoSetpoint2PS() throws Exception {
      log.debug("start executeUpdateNoSetpoint2PS() with database");
      insert(1);
   }

   @Test
   public void executeUpdateArchivePS() throws Exception {
      log.debug("start executeUpdateArchivePS() with database");

      Setpoint sp = registerSetpoint("cib_testentity", ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT,
            ControlEvent.UPDATE, ControlEvent.DELETE);

      insert(1);
      Context.end();
      Context.start();

      ResultSet rs = query("select * from cib_testentity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5L, rs.getLong(1));

      rs = query(
            "select a.*, r.primarykeyid, r.targettype from cib_archive a, cib_resource r where a.resourceid = r.resourceid");
      Assert.assertTrue(rs.next());
      String archiveId = rs.getString("ARCHIVEID");
      Assert.assertEquals("INSERT", rs.getString("CONTROLEVENT"));
      Assert.assertEquals("5", rs.getString("PRIMARYKEYID"));
      Assert.assertEquals("cib_testentity", rs.getString("TARGETTYPE"));

      int count = 0;
      rs = query("select * from cib_resourceparameter where resourceid = '" + rs.getString("resourceid") + "'");
      while (rs.next()) {
         count++;
         log.debug(rs.getString("NAME") + ", " + rs.getString("CLASSNAME") + ", " + rs.getString("PARAMETERTYPE") + ", "
               + rs.getInt("SEQUENCE"));
         Assert.assertEquals(count, rs.getInt("SEQUENCE"));
      }
      Assert.assertEquals(5, count);

      Configuration.instance().unregisterSetpoint(sp.getId());
   }

   @Test
   public void executeUpdateArchivePS_Update() throws Exception {
      log.debug("start executeUpdateArchivePS_Update() with database");

      Setpoint sp = registerSetpoint("cib_testentity", ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT,
            ControlEvent.UPDATE, ControlEvent.DELETE);

      insert(1);
      Thread.sleep(2);
      update(1);

      Context.end();
      Context.start();

      ResultSet rs = query("select * from cib_testentity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5L, rs.getLong(1));
      Assert.assertEquals("Röschen", rs.getString(2));
      Assert.assertEquals(99, rs.getInt(3));

      rs = query(
            "select a.*, r.primarykeyid, r.targettype, r.target from cib_archive a, cib_resource r where a.resourceid = r.resourceid order by a.createdate");
      Assert.assertTrue(rs.next());
      Assert.assertEquals("INSERT", rs.getString("CONTROLEVENT"));
      Assert.assertEquals("5", rs.getString("PRIMARYKEYID"));
      Assert.assertEquals("cib_testentity", rs.getString("TARGETTYPE"));
      Assert.assertEquals(INSERT, CibetUtil.decode(rs.getBytes("TARGET")));

      Assert.assertTrue(rs.next());
      Assert.assertEquals("UPDATE", rs.getString("CONTROLEVENT"));
      Assert.assertEquals("5", rs.getString("PRIMARYKEYID"));
      Assert.assertEquals("cib_testentity", rs.getString("TARGETTYPE"));
      Assert.assertEquals(UPDATE, CibetUtil.decode(rs.getBytes("TARGET")));

      Configuration.instance().unregisterSetpoint(sp.getId());
   }

   @Test
   public void executeUpdateArchivePS_Delete() throws Exception {
      log.debug("start executeUpdateArchivePS_Delete() with database");

      Setpoint sp = registerSetpoint("cib_testentity", ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT,
            ControlEvent.UPDATE, ControlEvent.DELETE);

      insert(1);
      Thread.sleep(2);
      delete(1);

      Context.end();
      Context.start();

      ResultSet rs = query("select * from cib_testentity");
      Assert.assertTrue(!rs.next());

      rs = query(
            "select a.*, r.primarykeyid, r.targettype, r.target from cib_archive a, cib_resource r where a.resourceid = r.resourceid order by a.createdate");
      Assert.assertTrue(rs.next());
      Assert.assertEquals("INSERT", rs.getString("CONTROLEVENT"));
      Assert.assertEquals("5", rs.getString("PRIMARYKEYID"));
      Assert.assertEquals("cib_testentity", rs.getString("TARGETTYPE"));
      Assert.assertEquals(INSERT, CibetUtil.decode(rs.getBytes("TARGET")));

      Assert.assertTrue(rs.next());
      Assert.assertEquals("DELETE", rs.getString("CONTROLEVENT"));
      Assert.assertEquals("5", rs.getString("PRIMARYKEYID"));
      Assert.assertEquals("cib_testentity", rs.getString("TARGETTYPE"));
      Assert.assertEquals(DELETE, CibetUtil.decode(rs.getBytes("TARGET")));

      Configuration.instance().unregisterSetpoint(sp.getId());
   }

   @Test
   public void executeUpdateArchive4ePS() throws Exception {
      log.debug("start executeUpdateArchive4ePS() with database");

      Setpoint sp = registerSetpoint("cib_testentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT,
            ControlEvent.UPDATE, ControlEvent.DELETE);

      insert(0);

      Context.end();
      Context.start();

      ResultSet rs = query("select * from cib_testentity");
      Assert.assertTrue(!rs.next());

      rs = query(
            "select d.*, r.primarykeyid, r.targettype from cib_controllable d, cib_resource r where d.resourceid = r.resourceid");
      Assert.assertTrue(rs.next());
      Assert.assertEquals("INSERT", rs.getString("CONTROLEVENT"));
      Assert.assertEquals("5", rs.getString("PRIMARYKEYID"));
      Assert.assertEquals("cib_testentity", rs.getString("TARGETTYPE"));

      log.debug("now release");
      List<Controllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());
      Controllable co = l.get(0);

      Context.sessionScope().setUser("test2");
      int res = (Integer) co.release(new JdbcBridgeEntityManager(connection), null);
      Assert.assertEquals(1, res);

      Context.end();
      Context.start();

      Context.sessionScope().setUser(USER);
      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      rs = query("select * from cib_testentity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5L, rs.getLong(1));

      Configuration.instance().unregisterSetpoint(sp.getId());
   }

   @Test
   public void executeUpdateArchive_Update4ePS() throws Exception {
      log.debug("start executeUpdateArchive_Update4ePS() with database");

      Setpoint sp = registerSetpoint("cib_testentity", FourEyesActuator.DEFAULTNAME, ControlEvent.UPDATE,
            ControlEvent.DELETE);

      insert(1);
      update(0);

      Context.end();
      Context.start();

      ResultSet rs = query("select * from cib_testentity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5L, rs.getLong(1));
      Assert.assertEquals("rosen", rs.getString(2));
      Assert.assertEquals(255, rs.getInt(3));

      rs = query(
            "select d.*, r.primarykeyid, r.targettype, r.target from cib_controllable d, cib_resource r where d.resourceid = r.resourceid");
      Assert.assertTrue(rs.next());

      Assert.assertEquals("UPDATE", rs.getString("CONTROLEVENT"));
      Assert.assertEquals("5", rs.getString("PRIMARYKEYID"));
      Assert.assertEquals("cib_testentity", rs.getString("TARGETTYPE"));
      Assert.assertEquals(UPDATE, CibetUtil.decode(rs.getBytes("TARGET")));

      log.debug("now release");
      List<Controllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());
      Controllable co = l.get(0);

      Context.sessionScope().setUser("test2");
      int res = (Integer) co.release(Context.requestScope().getEntityManager(), null);
      Assert.assertEquals(1, res);

      Context.end();
      Context.start();

      Context.sessionScope().setUser(USER);
      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      rs = query("select namevalue, counter from cib_testentity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals("Röschen", rs.getString(1));
      Assert.assertEquals(99, rs.getLong(2));

      Configuration.instance().unregisterSetpoint(sp.getId());
   }

   @Test
   public void executeUpdateArchive_Delete4ePS() throws Exception {
      log.debug("start executeUpdateArchive_Delete4ePS() with database");

      Setpoint sp = registerSetpoint("cib_testentity", FourEyesActuator.DEFAULTNAME, ControlEvent.UPDATE,
            ControlEvent.DELETE);

      insert(1);
      delete(0);

      Context.end();
      Context.start();

      ResultSet rs = query("select * from cib_testentity");
      Assert.assertTrue(rs.next());

      rs = query(
            "select d.*, r.primarykeyid, r.targettype, r.target from cib_controllable d, cib_resource r where d.resourceid = r.resourceid");
      Assert.assertTrue(rs.next());
      Assert.assertEquals("DELETE", rs.getString("CONTROLEVENT"));
      Assert.assertEquals("5", rs.getString("PRIMARYKEYID"));
      Assert.assertEquals("cib_testentity", rs.getString("TARGETTYPE"));
      Assert.assertEquals(DELETE, CibetUtil.decode(rs.getBytes("TARGET")));

      log.debug("now release");
      List<Controllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());
      Controllable co = l.get(0);

      Context.sessionScope().setUser("test2");
      int res = (Integer) co.release(Context.requestScope().getEntityManager(), null);
      Assert.assertEquals(1, res);

      Context.end();
      Context.start();

      Context.sessionScope().setUser(USER);
      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      rs = query("select namevalue, counter from cib_testentity where id=5");
      Assert.assertTrue(!rs.next());

      Configuration.instance().unregisterSetpoint(sp.getId());
   }

   @Test
   public void persistWith2ManRuleJdbcPS() throws Exception {
      log.info("start persistWith2ManRuleJdbcPS()");

      Context.sessionScope().setSecondUser(null);
      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      Setpoint sp = registerSetpoint("cib_testentity", schemes, ControlEvent.INSERT);

      insert(0);
      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      Context.end();
      Context.start();

      ResultSet rs = query("select * from cib_testentity");
      Assert.assertTrue(!rs.next());

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId("cib_testentity", "5");
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Assert.assertEquals(ControlEvent.INSERT, ar.getControlEvent());
      Assert.assertEquals("5", ((JdbcResource) ar.getResource()).getPrimaryKeyId());

      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Controllable dcOb = list1.get(0);
      Assert.assertEquals(ControlEvent.INSERT, dcOb.getControlEvent());

      Configuration.instance().unregisterSetpoint(sp.getId());
   }

   @Test
   public void persistWith2ManRuleDirectReleaseJdbcPS() throws Exception {
      log.info("start persistWith2ManRuleDirectReleaseJdbcPS()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      Setpoint sp = registerSetpoint("cib_testentity", schemes, ControlEvent.INSERT, ControlEvent.RELEASE_INSERT);

      Context.sessionScope().setSecondUser("secondUser");
      insert(1);

      Context.sessionScope().setSecondUser(null);
      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      Context.end();
      Context.start();

      ResultSet rs = query("select * from cib_testentity");
      Assert.assertTrue(rs.next());

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId("cib_testentity", "5");
      Assert.assertEquals(2, list.size());
      Archive ar = list.get(0);
      Assert.assertTrue(
            ControlEvent.INSERT == ar.getControlEvent() || ControlEvent.RELEASE_INSERT == ar.getControlEvent());

      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());

      Configuration.instance().unregisterSetpoint(sp.getId());
   }

   @Test
   public void persistWith2ManRuleLaterReleaseJdbcPS() throws Exception {
      log.info("start persistWith2ManRuleLaterReleaseJdbcPS()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      Setpoint sp = registerSetpoint("cib_testentity", schemes, ControlEvent.INSERT, ControlEvent.RELEASE);

      insert(0);
      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      Context.end();
      Context.start();

      log.info("now release");
      Context.sessionScope().setUser(USER);
      Context.sessionScope().setSecondUser("secondUser");
      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Controllable dcOb = list1.get(0);
      Assert.assertEquals(ControlEvent.INSERT, dcOb.getControlEvent());
      int count = (Integer) dcOb.release(new JdbcBridgeEntityManager(connection), "2man rule test");
      Assert.assertEquals(1, count);
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      Context.end();
      Context.start();

      ResultSet rs = query("select id, namevalue, counter from cib_testentity where id=5");
      Assert.assertTrue(rs.next());

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId("cib_testentity", "5");

      Assert.assertEquals(2, list.size());
      Archive ar = list.get(0);
      Assert.assertEquals(ControlEvent.INSERT, ar.getControlEvent());
      Assert.assertTrue(!((JdbcResource) ar.getResource()).getPrimaryKeyId().equals("0"));

      list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());

      Configuration.instance().unregisterSetpoint(sp.getId());
   }

   @Test
   public void deleteWith2ManRuleJdbcPS() throws Exception {
      log.info("start deleteWith2ManRuleJdbcPS()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      Setpoint sp = registerSetpoint("cib_testentity", schemes, ControlEvent.DELETE);

      insert(1);
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      ResultSet rs = query("select namevalue, counter from cib_testentity where id=5");
      Assert.assertTrue(rs.next());

      delete(0);
      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      Context.end();
      Context.start();

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId("cib_testentity", "5");
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Assert.assertEquals(ControlEvent.DELETE, ar.getControlEvent());

      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Controllable dcOb = list1.get(0);
      Assert.assertEquals(ControlEvent.DELETE, dcOb.getControlEvent());

      Configuration.instance().unregisterSetpoint(sp.getId());
   }

   @Test
   public void deleteWith2ManRuleDirectReleaseJdbcPS() throws Exception {
      log.info("start deleteWith2ManRuleDirectReleaseJdbcPS()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      Setpoint sp = registerSetpoint("cib_testentity", schemes, ControlEvent.DELETE, ControlEvent.RELEASE_DELETE);

      insert(1);

      Context.sessionScope().setSecondUser("secondUser");
      delete(1);
      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      Context.end();
      Context.start();

      ResultSet rs = query("select namevalue, counter from cib_testentity where id=5");
      Assert.assertTrue(!rs.next());

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId("cib_testentity", "5");
      Assert.assertEquals(2, list.size());
      Archive ar = list.get(0);
      Assert.assertTrue(
            ControlEvent.DELETE == ar.getControlEvent() || ControlEvent.RELEASE_DELETE == ar.getControlEvent());

      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());

      Configuration.instance().unregisterSetpoint(sp.getId());
   }

   @Test
   public void deleteWith2ManRuleLaterReleaseJdbcPS() throws Exception {
      log.info("start deleteWith2ManRuleLaterReleaseJdbcPS()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      Setpoint sp = registerSetpoint("cib_testentity", schemes, ControlEvent.DELETE, ControlEvent.RELEASE);

      insert(1);
      delete(0);
      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      Context.end();
      Context.start();

      ResultSet rs = query("select namevalue, counter from cib_testentity where id=5");
      Assert.assertTrue(rs.next());

      log.info("now release");
      Context.sessionScope().setSecondUser("secondUser");
      Context.sessionScope().setUser(USER);

      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Controllable dcOb = list1.get(0);
      Assert.assertEquals(ControlEvent.DELETE, dcOb.getControlEvent());
      dcOb.release(new JdbcBridgeEntityManager(connection), "2man rule test");
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      Context.end();
      Context.start();

      rs = query("select namevalue, counter from cib_testentity where id=5");
      Assert.assertTrue(!rs.next());

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId("cib_testentity", "5");
      Assert.assertEquals(2, list.size());
      Archive ar = list.get(0);
      Assert.assertTrue(
            ControlEvent.DELETE == ar.getControlEvent() || ControlEvent.RELEASE_DELETE == ar.getControlEvent());

      list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());

      Configuration.instance().unregisterSetpoint(sp.getId());
   }

   @Test
   public void updateWith2ManRuleJdbcPS() throws Exception {
      log.info("start updateWith2ManRuleJdbcPS()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      Setpoint sp = registerSetpoint("cib_testentity", schemes, ControlEvent.UPDATE);

      insert(1);
      update(0);
      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      Context.end();
      Context.start();

      ResultSet rs = query("select namevalue, counter from cib_testentity where id=5");
      Assert.assertTrue(rs.next());
      Assert.assertEquals("rosen", rs.getString(1));

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId("cib_testentity", "5");
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Assert.assertEquals(ControlEvent.UPDATE, ar.getControlEvent());

      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Controllable dcOb = list1.get(0);
      Assert.assertEquals(ControlEvent.UPDATE, dcOb.getControlEvent());

      Configuration.instance().unregisterSetpoint(sp.getId());
   }

   @Test
   public void updateWith2ManRuleDirectReleaseJdbcPS() throws Exception {
      log.info("start updateWith2ManRuleDirectReleaseJdbcPS()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      Setpoint sp = registerSetpoint("cib_testentity", schemes, ControlEvent.UPDATE, ControlEvent.RELEASE_UPDATE);

      Context.sessionScope().setSecondUser("secondUser");
      insert(1);
      update(1);
      Context.sessionScope().setSecondUser(null);
      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      Context.end();
      Context.start();

      ResultSet rs = query("select namevalue, counter from cib_testentity where id=5");
      Assert.assertTrue(rs.next());
      Assert.assertEquals("Röschen", rs.getString(1));

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId("cib_testentity", "5");
      Assert.assertEquals(2, list.size());
      Archive ar = list.get(0);
      Assert.assertTrue(
            ControlEvent.UPDATE == ar.getControlEvent() || ControlEvent.RELEASE_UPDATE == ar.getControlEvent());

      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());

      Configuration.instance().unregisterSetpoint(sp.getId());
   }

   @Test
   public void updateWith2ManRuleLaterReleaseJdbcPS() throws Exception {
      log.info("start updateWith2ManRuleLaterReleaseJdbcPS()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      Setpoint sp = registerSetpoint("cib_testentity", schemes, ControlEvent.UPDATE, ControlEvent.RELEASE);

      insert(1);
      update(0);

      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      Context.end();
      Context.start();

      ResultSet rs = query("select namevalue, counter from cib_testentity where id=5");
      Assert.assertTrue(rs.next());
      Assert.assertEquals("rosen", rs.getString(1));

      log.info("now release");
      Context.sessionScope().setSecondUser("secondUser");
      Context.sessionScope().setUser(USER);

      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Controllable dcOb = list1.get(0);
      Assert.assertEquals(ControlEvent.UPDATE, dcOb.getControlEvent());
      dcOb.release(new JdbcBridgeEntityManager(connection), "2man rule test");
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      Context.end();
      Context.start();

      rs = query("select namevalue, counter from cib_testentity where id=5");
      Assert.assertTrue(rs.next());
      Assert.assertEquals("Röschen", rs.getString(1));

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId("cib_testentity", "5");
      Assert.assertEquals(2, list.size());
      Archive ar = list.get(0);
      Assert.assertEquals(ControlEvent.UPDATE, ar.getControlEvent());

      list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());

      Configuration.instance().unregisterSetpoint(sp.getId());
   }

   @Test(expected = DeniedException.class)
   public void persistWith2ManRuleDirectReleaseDeniedJdbcPS() throws Exception {
      log.info("start persistWith2ManRuleDirectReleaseDeniedJdbcPS()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context_1.xml" });

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");
      act.setThrowDeniedException(true);

      List<String> schemes = new ArrayList<String>();
      schemes.add(SpringSecurityActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      Setpoint sp = registerSetpoint("cib_testentity", schemes, ControlEvent.INSERT);
      Setpoint sp2 = null;

      try {
         SpringSecurityActuator releaseSec = new SpringSecurityActuator("RELEASESEC");
         releaseSec.setPreAuthorize("hasAnyRole('Wooo')");
         releaseSec.setThrowDeniedException(true);
         releaseSec.setSecondPrincipal(true);
         Configuration.instance().registerActuator(releaseSec);

         Thread.sleep(50);
         List<String> schemes2 = new ArrayList<String>();
         schemes2.add(releaseSec.getName());
         schemes2.add(TwoManRuleActuator.DEFAULTNAME);
         sp2 = registerSetpoint("cib_testentity", schemes2, ControlEvent.RELEASE);

         authenticate("Heinz");
         authenticateSecond();

         Context.sessionScope().setSecondUser("secondUser");
         insert(1);

      } finally {
         Configuration.instance().unregisterSetpoint(sp.getId());
         Configuration.instance().unregisterSetpoint(sp2.getId());
      }
   }

   @Test
   public void persistWith2ManRuleDirectReleaseGranted_jdbcPS() throws Exception {
      log.info("start persistWith2ManRuleDirectReleaseGranted_jdbcPS()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context_1.xml" });

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");
      act.setThrowDeniedException(true);

      List<String> schemes = new ArrayList<String>();
      schemes.add(SpringSecurityActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      Setpoint sp = registerSetpoint("cib_testentity", schemes, ControlEvent.INSERT);

      SpringSecurityActuator releaseSec = new SpringSecurityActuator("RELEASESEC");
      releaseSec.setPreAuthorize("hasAnyRole('Wooo')");
      releaseSec.setThrowDeniedException(true);
      releaseSec.setSecondPrincipal(true);
      Configuration.instance().registerActuator(releaseSec);

      Thread.sleep(30);
      List<String> schemes2 = new ArrayList<String>();
      schemes2.add(releaseSec.getName());
      schemes2.add(TwoManRuleActuator.DEFAULTNAME);
      Setpoint sp2 = registerSetpoint("cib_testentity", schemes2, ControlEvent.RELEASE);

      authenticate("Heinz");
      authenticateSecond("Wooo");
      Context.sessionScope().setSecondUser("secondUser");

      insert(1);

      Context.sessionScope().setSecondUser(null);
      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      ResultSet rs = query("select namevalue, counter from cib_testentity where id=5");
      Assert.assertTrue(rs.next());

      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());

      Configuration.instance().unregisterSetpoint(sp.getId());
      Configuration.instance().unregisterSetpoint(sp2.getId());
   }

   @Test
   public void releaseRemove6Eyes_jdbcPS() throws Exception {
      log.info("start releaseRemove6Eyes_jdbcPS()");

      Setpoint sp = registerSetpoint("cib_testentity", SixEyesActuator.DEFAULTNAME, ControlEvent.DELETE,
            ControlEvent.RELEASE_DELETE, ControlEvent.FIRST_RELEASE_DELETE);

      insert(1);
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      ResultSet rs = query("select namevalue, counter from cib_testentity where id=5");
      Assert.assertTrue(rs.next());

      delete(0);

      Context.end();
      Context.start();

      // first release
      List<Controllable> l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      Controllable co = l1.get(0);
      Context.sessionScope().setUser("tester2");
      co.release(new JdbcBridgeEntityManager(connection), "blabla1");

      Context.end();
      Context.start();
      Context.sessionScope().setUser(USER);

      l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      co = l1.get(0);
      Assert.assertEquals("tester2", co.getFirstApprovalUser());
      Assert.assertEquals(ControlEvent.DELETE, co.getControlEvent());
      Assert.assertEquals(USER, co.getCreateUser());

      // still not removed
      rs = query("select namevalue, counter from cib_testentity where owner='Lalla'");
      Assert.assertTrue(rs.next());

      // 2. release
      try {
         // invalid user
         co.release(new JdbcBridgeEntityManager(connection), "blabla2");
         Assert.fail();
      } catch (InvalidUserException e) {
      }

      try {
         // invalid user
         Context.sessionScope().setUser("tester2");
         co.release(new JdbcBridgeEntityManager(connection), "blabla2");
         Assert.fail();
      } catch (InvalidUserException e) {
      }

      Context.sessionScope().setUser("tester3");
      co.release(new JdbcBridgeEntityManager(connection), "blabla2");

      Context.end();
      Context.start();

      // now it is removed
      rs = query("select namevalue, counter from cib_testentity where owner='Lalla'");
      Assert.assertTrue(!rs.next());

      Configuration.instance().unregisterSetpoint(sp.getId());
   }

   @Test
   public void insertDenied_jdbcPS() throws Exception {
      log.debug("start insertDenied_jdbcPS()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context_1.xml" });

      Setpoint sp = registerSetpoint("cib_testentity", SpringSecurityActuator.DEFAULTNAME, ControlEvent.INSERT);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");
      act.setThrowDeniedException(true);

      authenticate("WILLI");

      try {
         insert(0);
         Assert.fail();
      } catch (DeniedException e) {
      }
      Assert.assertEquals(ExecutionStatus.DENIED, Context.requestScope().getExecutedEventResult().getExecutionStatus());
      ResultSet rs = query("select namevalue, counter from cib_testentity where id=5");
      Assert.assertTrue(!rs.next());

      Configuration.instance().unregisterSetpoint(sp.getId());
   }

   @Test
   public void insertOk_jdbcPS() throws Exception {
      log.debug("start insertOk_jdbcPS()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context_1.xml" });

      Setpoint sp = registerSetpoint("cib_testentity", SpringSecurityActuator.DEFAULTNAME, ControlEvent.INSERT);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");

      authenticate("Heinz");

      insert(1);

      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      ResultSet rs = query("select namevalue, counter from cib_testentity where id=5");
      Assert.assertTrue(rs.next());

      Configuration.instance().unregisterSetpoint(sp.getId());
   }

   @Test
   public void lockObjectLockedSameUser_jdbcPS() throws Exception {
      log.info("start lockObjectLockedSameUser_jdbcPS()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      Setpoint sp = registerSetpoint("cib_testentity", schemes, ControlEvent.UPDATE);

      insert(1);
      ResultSet rs = query("select namevalue, counter from cib_testentity where id=5");
      Assert.assertTrue(rs.next());
      Controllable lo = Locker.lock("cib_testentity", "5", ControlEvent.UPDATE, "testremark");
      Assert.assertNotNull(lo);

      Context.end();
      Context.start();
      Context.sessionScope().setUser(USER);

      log.debug("now update");
      update(1);
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      Context.end();
      Context.start();

      rs = query("select namevalue, counter from cib_testentity where id=5");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(99, rs.getLong(2));

      List<Archive> list = ArchiveLoader.loadArchives("cib_testentity");
      Assert.assertEquals(1, list.size());
      log.debug("ARCHIVE: " + list.get(0));
      Assert.assertEquals(ControlEvent.UPDATE, list.get(0).getControlEvent());
      Assert.assertEquals(USER, list.get(0).getCreateUser());
      Assert.assertNull(list.get(0).getRemark());
      Assert.assertNotNull(list.get(0).getResource().getTarget());

      List<Controllable> l2 = Locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());
      log.debug("LOCKEDOBJECT: " + l2.get(0));

      Configuration.instance().unregisterSetpoint(sp.getId());
   }

   @Test
   public void lockClassLockedOtherUser_jdbcPS() throws Exception {
      log.info("start lockClassLockedOtherUser_jdbcPS()");

      insert(1);

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      Setpoint sp = registerSetpoint("cib_testentity", schemes, ControlEvent.PERSIST);

      Controllable lo = Locker.lock("cib_testentity", (String) null, ControlEvent.UPDATE, "testremark");
      Assert.assertNotNull(lo);

      Context.end();
      Context.start();

      Context.sessionScope().setUser("otherUser");
      update(0);
      Assert.assertEquals(ExecutionStatus.DENIED, Context.requestScope().getExecutedEventResult().getExecutionStatus());

      Context.end();
      Context.start();

      List<Archive> list = ArchiveLoader.loadArchives("cib_testentity");
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(ExecutionStatus.DENIED, list.get(0).getExecutionStatus());

      List<Controllable> l2 = Locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());
      log.debug("LOCKEDOBJECT: " + l2.get(0));

      Configuration.instance().unregisterSetpoint(sp.getId());
   }

   @Test
   public void prepareStatement() throws Exception {
      PreparedStatement ps = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS);
      insert((short) 6, ps);

      ps = connection.prepareStatement(INSERT, new int[] { 1 });
      insert((short) 7, ps);

      ps = connection.prepareStatement(INSERT, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
      insert((short) 8, ps);

      ps = connection.prepareStatement(INSERT, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE,
            ResultSet.CLOSE_CURSORS_AT_COMMIT);
      insert((short) 9, ps);

      ps = connection.prepareStatement(INSERT, new String[] { "id" });
      insert((short) 10, ps);
   }

   @Test
   public void execute() throws Exception {
      PreparedStatement ps = connection.prepareStatement(INSERT);
      ps.setShort(1, (short) 5);
      ps.setString(2, "rosen");
      ps.setInt(3, 255);
      ps.setString(4, "Lalla");

      boolean b = ps.execute();
      Assert.assertEquals(false, b);
      Assert.assertEquals(1, ps.getUpdateCount());
   }

   @Test(expected = SQLException.class)
   public void executeNoSQL() throws Exception {
      log.debug("start executeNoSQL() with database");
      PreparedStatement ps = connection.prepareStatement(null);
      ps.execute();
   }

   @Test(expected = SQLException.class)
   public void executeUpdateNoSQL() throws Exception {
      log.debug("start executeNoSQL() with database");
      PreparedStatement ps = connection.prepareStatement(null);
      ps.executeUpdate();
   }

   @Test
   public void useMultiplePrepareStatement() throws Exception {
      log.info("start useMultiplePrepareStatement");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      Setpoint sp = registerSetpoint("cib_testentity", schemes, ControlEvent.INSERT);

      PreparedStatement ps = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS);
      ps.setShort(1, (short) 6);
      ps.setString(2, "rosen");
      ps.setInt(3, 255);
      ps.setString(4, "Lalla");
      int count = ps.executeUpdate();
      Assert.assertEquals(1, count);
      Thread.sleep(2);

      ps.setShort(1, (short) 7);
      ps.setString(2, "rosen");
      ps.setInt(3, 444);
      ps.setString(4, "Kuller");
      count = ps.executeUpdate();
      Assert.assertEquals(1, count);
      Thread.sleep(2);

      PreparedStatement sel = connection.prepareStatement(SELECT);
      sel.setShort(1, (short) 6);
      ResultSet rs = sel.executeQuery();
      Assert.assertTrue(rs.next());
      Assert.assertEquals(255, rs.getInt(3));

      sel = connection.prepareStatement(SELECT);
      sel.setShort(1, (short) 7);
      rs = sel.executeQuery();
      Assert.assertTrue(rs.next());
      Assert.assertEquals(444, rs.getInt(3));

      Context.end();
      Context.start();

      rs = query(
            "select a.*, r.primarykeyid, r.targettype from cib_archive a, cib_resource r where a.resourceid = r.resourceid order by a.createdate");
      Assert.assertTrue(rs.next());
      String archiveId = rs.getString("ARCHIVEID");
      Assert.assertEquals("INSERT", rs.getString("CONTROLEVENT"));
      Assert.assertEquals("6", rs.getString("PRIMARYKEYID"));
      Assert.assertEquals("cib_testentity", rs.getString("TARGETTYPE"));

      int count2 = 0;
      ResultSet rs2 = query(
            "select * from cib_resourceparameter where resourceid = '" + rs.getString("resourceid") + "'");
      while (rs2.next()) {
         count2++;
         log.debug(rs2.getString("NAME") + ", " + rs2.getString("PARAMETERTYPE") + ", " + rs2.getString("CLASSNAME")
               + ", " + rs2.getInt("SEQUENCE"));
         Object value = CibetUtil.decode(rs2.getBytes("ENCODEDVALUE"));
         log.debug("VALUE: " + value);
         if (count2 == 2)
            Assert.assertEquals("rosen", value);
         if (count2 == 3)
            Assert.assertEquals(255, value);
         if (count2 == 4)
            Assert.assertEquals("Lalla", value);
         Assert.assertEquals(count2, rs2.getInt("SEQUENCE"));
      }
      Assert.assertEquals(5, count2);

      Assert.assertTrue(rs.next());
      archiveId = rs.getString("ARCHIVEID");
      Assert.assertEquals("INSERT", rs.getString("CONTROLEVENT"));
      Assert.assertEquals("7", rs.getString("PRIMARYKEYID"));
      Assert.assertEquals("cib_testentity", rs.getString("TARGETTYPE"));

      count2 = 0;
      rs2 = query("select * from cib_resourceparameter where resourceid = '" + rs.getString("resourceid") + "'");
      while (rs2.next()) {
         count2++;
         log.debug(rs2.getString("NAME") + ", " + rs2.getString("PARAMETERTYPE") + ", " + rs2.getString("CLASSNAME")
               + ", " + rs2.getInt("SEQUENCE"));
         Assert.assertEquals(count2, rs2.getInt("SEQUENCE"));
         Object value = CibetUtil.decode(rs2.getBytes("ENCODEDVALUE"));
         if (count2 == 2)
            Assert.assertEquals("rosen", value);
         if (count2 == 3)
            Assert.assertEquals(444, value);
         if (count2 == 4)
            Assert.assertEquals("Kuller", value);
      }
      Assert.assertEquals(5, count2);

      Configuration.instance().unregisterSetpoint(sp.getId());
   }

   @Test
   public void alterTable() throws Exception {
      String sql;
      PreparedStatement ps;
      try {
         log.debug("start alterTable()");
         sql = "ALTER table CIB_SYNTETIC1ENTITY ADD xxxx VARCHAR(20)";
         ps = connection.prepareStatement(sql);

         boolean flag = ps.execute();
         Assert.assertEquals(false, flag);
         // Assert.assertEquals(1, ps.getUpdateCount());
         EventResult er = Context.requestScope().getExecutedEventResult();
         log.debug(er);
         Assert.assertEquals(ExecutionStatus.EXECUTED, er.getExecutionStatus());
         Assert.assertTrue(er.getResource().indexOf(sql) > 0);
      } finally {
         try {
            sql = "ALTER table CIB_SYNTETIC1ENTITY drop column xxxx";
            ps = connection.prepareStatement(sql);
            ps.execute();
         } catch (Exception e) {
            log.warn(e.getMessage(), e);
         }
      }
   }

   @Test
   public void playUpdateWith2ManRuleDirectReleaseJdbcPS() throws Exception {
      log.info("start playUpdateWith2ManRuleDirectReleaseJdbcPS()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      Setpoint sp = registerSetpoint("cib_testentity", schemes, ControlEvent.UPDATE, ControlEvent.RELEASE_UPDATE);

      Context.sessionScope().setSecondUser("secondUser");
      Context.requestScope().startPlay();
      insert(0);
      EventResult er = Context.requestScope().stopPlay();
      Assert.assertEquals(ExecutionStatus.EXECUTED, er.getExecutionStatus());

      ResultSet rs = query("select namevalue, counter from cib_testentity where id=5");
      Assert.assertTrue(!rs.next());

      insert(1);

      Context.requestScope().startPlay();
      update(0);
      er = Context.requestScope().stopPlay();
      Assert.assertEquals(ExecutionStatus.POSTPONED, er.getExecutionStatus());
      Assert.assertEquals(ExecutionStatus.EXECUTED, er.getChildResults().get(0).getExecutionStatus());

      Context.end();
      Context.start();

      rs = query("select namevalue, counter from cib_testentity where id=5");
      Assert.assertTrue(rs.next());
      Assert.assertEquals("rosen", rs.getString(1));

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId("cib_testentity", "5");
      Assert.assertEquals(0, list.size());

      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());

      Configuration.instance().unregisterSetpoint(sp.getId());
   }

   @Test
   public void playUpdateWith2ManRuleLaterReleaseJdbcPS() throws Exception {
      log.info("start playUpdateWith2ManRuleLaterReleaseJdbcPS()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      Setpoint sp = registerSetpoint("cib_testentity", schemes, ControlEvent.UPDATE, ControlEvent.RELEASE);

      insert(1);

      Context.requestScope().startPlay();
      update(0);
      EventResult er = Context.requestScope().stopPlay();
      Context.sessionScope().setSecondUser(null);
      Assert.assertEquals(ExecutionStatus.POSTPONED, er.getExecutionStatus());
      Assert.assertEquals(0, er.getChildResults().size());

      update(0);

      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      Context.end();
      Context.start();

      ResultSet rs = query("select namevalue, counter from cib_testentity where id=5");
      Assert.assertTrue(rs.next());
      Assert.assertEquals("rosen", rs.getString(1));

      log.info("now release");
      Context.sessionScope().setSecondUser("secondUser");
      Context.sessionScope().setUser(USER);
      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Controllable dcOb = list1.get(0);
      Assert.assertEquals(ControlEvent.UPDATE, dcOb.getControlEvent());

      Context.requestScope().startPlay();
      dcOb.release(new JdbcBridgeEntityManager(connection), "2man rule test");
      er = Context.requestScope().stopPlay();

      Assert.assertEquals(ExecutionStatus.EXECUTED, er.getExecutionStatus());

      Context.end();
      Context.start();

      rs = query("select namevalue, counter from cib_testentity where id=5");
      Assert.assertTrue(rs.next());
      Assert.assertEquals("rosen", rs.getString(1));

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId("cib_testentity", "5");
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Assert.assertEquals(ControlEvent.UPDATE, ar.getControlEvent());

      list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());

      Configuration.instance().unregisterSetpoint(sp.getId());
   }

   @Test
   public void executeUpdateWithException() throws Exception {
      log.debug("start executeUpdateWithException() with database");

      Setpoint sp = registerSetpoint("cib_testentity", ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT,
            ControlEvent.UPDATE, ControlEvent.DELETE);

      PreparedStatement ps = connection.prepareStatement(INSERT);
      ps.setString(1, "error");
      ps.setString(2, "rosen");
      ps.setInt(3, 255);
      ps.setString(4, "Lalla");
      try {
         ps.executeUpdate();
         Assert.fail();
      } catch (SQLException e) {
      }

      Context.end();
      Context.start();

      ResultSet rs = query(
            "select a.*, r.targettype from cib_archive a, cib_resource r where a.resourceid = r.resourceid");
      Assert.assertTrue(rs.next());
      String archiveId = rs.getString("ARCHIVEID");
      Assert.assertEquals("INSERT", rs.getString("CONTROLEVENT"));
      Assert.assertEquals("cib_testentity", rs.getString("TARGETTYPE"));
      Assert.assertEquals(ExecutionStatus.ERROR.name(), rs.getString("EXECUTIONSTATUS"));

      int count = 0;
      rs = query("select * from cib_resourceparameter where resourceid = '" + rs.getString("resourceid") + "'");
      while (rs.next()) {
         count++;
         log.debug(rs.getString("NAME") + ", " + rs.getString("CLASSNAME") + ", " + rs.getString("PARAMETERTYPE") + ", "
               + rs.getInt("SEQUENCE"));
         Assert.assertEquals(count, rs.getInt("SEQUENCE"));
      }
      Assert.assertEquals(5, count);

      Configuration.instance().unregisterSetpoint(sp.getId());
   }

   @Test
   public void executeWithException() throws Exception {
      log.debug("start executeWithException() with database");
      Setpoint sp = registerSetpoint("cib_testentity", ArchiveActuator.DEFAULTNAME, ControlEvent.PERSIST);

      PreparedStatement ps = connection.prepareStatement(INSERT);
      ps.setString(1, "error");
      ps.setString(2, "rosen");
      ps.setInt(3, 255);
      ps.setString(4, "Lalla");

      try {
         ps.execute();
         Assert.fail();
      } catch (SQLException e) {
         log.debug(e.getMessage(), e);
      }
      Assert.assertTrue(ps.getUpdateCount() <= 0);

      Context.end();
      Context.start();

      ResultSet rs = query(
            "select a.*, r.targettype from cib_archive a, cib_resource r where a.resourceid = r.resourceid");
      Assert.assertTrue(rs.next());
      String archiveId = rs.getString("ARCHIVEID");
      Assert.assertEquals("INSERT", rs.getString("CONTROLEVENT"));
      Assert.assertEquals("cib_testentity", rs.getString("TARGETTYPE"));
      Assert.assertEquals(ExecutionStatus.ERROR.name(), rs.getString("EXECUTIONSTATUS"));

      int count = 0;
      rs = query("select * from cib_resourceparameter where resourceid = '" + rs.getString("resourceid") + "'");
      while (rs.next()) {
         count++;
         log.debug(rs.getString("NAME") + ", " + rs.getString("CLASSNAME") + ", " + rs.getString("PARAMETERTYPE") + ", "
               + rs.getInt("SEQUENCE"));
         Assert.assertEquals(count, rs.getInt("SEQUENCE"));
      }
      Assert.assertEquals(5, count);

      Configuration.instance().unregisterSetpoint(sp.getId());
   }

}
