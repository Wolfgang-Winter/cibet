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
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.sensor.jdbc.bridge.JdbcBridgeEntityManager;

public class CibetStatementExUpIntegrationTest extends JdbcHelper {

   private static Logger log = Logger.getLogger(CibetStatementExUpIntegrationTest.class);

   private Setpoint sp = null;

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
      if (sp != null) {
         Configuration.instance().unregisterSetpoint(sp.getId());
      }
   }

   protected void execute(String sql, int expectedCount) throws Exception {
      Statement st = connection.createStatement();
      int count = st.executeUpdate(sql);
      Assert.assertEquals(expectedCount, count);
   }

   private ResultSet query(String sql) throws Exception {
      Statement st = connection.createStatement();
      return st.executeQuery(sql);
   }

   protected void authenticate(String... roles) throws AuthenticationException {
      SpringTestAuthenticationManager authManager = new SpringTestAuthenticationManager();
      for (String role : roles) {
         if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
         }
         authManager.addAuthority(role);
      }

      Authentication request = new UsernamePasswordAuthenticationToken("test", "test");
      Authentication result = authManager.authenticate(request);
      SecurityContextHolder.getContext().setAuthentication(result);
   }

   protected void authenticateSecond(String... roles) throws AuthenticationException {
      SpringTestAuthenticationManager authManager = new SpringTestAuthenticationManager();
      for (String role : roles) {
         if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
         }
         authManager.addAuthority(role);
      }

      Authentication request = new UsernamePasswordAuthenticationToken("test", "test");
      Authentication result = authManager.authenticate(request);
      Context.internalSessionScope().setProperty(InternalSessionScope.SECOND_PRINCIPAL, result);
   }

   @Test
   public void executeUpdateNoSetpoint() throws Exception {
      log.debug("start executeUpdateNoSetpoint() with database");
      execute("insert into cib_testentity (id, nameValue, counter, userid, owner) "
            + "values (5, 'rosen', 255, 'Klaus', 'Lalla')", 1);
   }

   @Test
   public void executeUpdateArchive() throws Exception {
      log.debug("start executeUpdateArchive() with database");

      sp = registerSetpoint("cib_testentity", ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE);

      execute("insert into cib_testentity (id, nameValue, counter, userid, owner) "
            + "values (5, 'rosen', 255, 'Klaus', 'Lalla')", 1);

      Context.end();
      Context.start();

      ResultSet rs = query("select * from cib_testentity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5L, rs.getLong(1));

      rs = query(
            "select a.*, r.primarykeyid, r.target from cib_archive a, cib_resource r where a.resourceid = r.resourceid");
      Assert.assertTrue(rs.next());
      Assert.assertEquals("INSERT", rs.getString("CONTROLEVENT"));
      Assert.assertEquals("5", rs.getString("PRIMARYKEYID"));
      Assert.assertEquals("cib_testentity", rs.getString("TARGET"));
   }

   @Test
   public void executeUpdateArchive_Update() throws Exception {
      log.debug("start executeUpdateArchive_Update() with database");

      sp = registerSetpoint("cib_testentity", ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE);

      execute("insert into cib_testentity (id, nameValue, counter, userid, owner) "
            + "values (5, 'rosen', 255, 'Klaus', 'Lalla')", 1);
      Thread.sleep(2);
      execute("update cib_testentity set nameValue='R�schen', counter=99 " + "WHERE id=5 and owner='Lalla'", 1);

      Context.end();
      Context.start();

      ResultSet rs = query("select * from cib_testentity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5L, rs.getLong(1));
      Assert.assertEquals("R�schen", rs.getString(2));
      Assert.assertEquals(99, rs.getInt(3));

      rs = query(
            "select a.*, r.primarykeyid, r.target, r.targetobject from cib_archive a, cib_resource r where a.resourceid = r.resourceid order by a.createdate");
      Assert.assertTrue(rs.next());
      Assert.assertEquals("INSERT", rs.getString("CONTROLEVENT"));
      Assert.assertEquals("5", rs.getString("PRIMARYKEYID"));
      Assert.assertEquals("cib_testentity", rs.getString("TARGET"));
      Assert.assertEquals("insert into cib_testentity (id, nameValue, counter, userid, owner) "
            + "values (5, 'rosen', 255, 'Klaus', 'Lalla')", CibetUtil.decode(rs.getBytes("TARGETOBJECT")));

      Assert.assertTrue(rs.next());
      Assert.assertEquals("UPDATE", rs.getString("CONTROLEVENT"));
      Assert.assertEquals("5", rs.getString("PRIMARYKEYID"));
      Assert.assertEquals("cib_testentity", rs.getString("TARGET"));
      Assert.assertEquals("update cib_testentity set nameValue='R�schen', counter=99 " + "WHERE id=5 and owner='Lalla'",
            CibetUtil.decode(rs.getBytes("TARGETOBJECT")));
   }

   @Test
   public void executeUpdateArchive_Delete() throws Exception {
      log.debug("start executeUpdateArchive_Delete() with database");

      sp = registerSetpoint("cib_testentity", ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE);

      execute("insert into cib_testentity (id, nameValue, counter, userid, owner) "
            + "values (5, 'rosen', 255, 'Klaus', 'Lalla')", 1);
      Thread.sleep(2);
      execute("delete from cib_testentity WHERE id=5 ", 1);

      Context.end();
      Context.start();

      ResultSet rs = query("select * from cib_testentity");
      Assert.assertTrue(!rs.next());

      rs = query(
            "select a.*, r.primarykeyid, r.target, r.targetobject from cib_archive a, cib_resource r where a.resourceid = r.resourceid order by a.createdate");
      Assert.assertTrue(rs.next());
      Assert.assertEquals("INSERT", rs.getString("CONTROLEVENT"));
      Assert.assertEquals("5", rs.getString("PRIMARYKEYID"));
      Assert.assertEquals("cib_testentity", rs.getString("TARGET"));
      Assert.assertEquals("insert into cib_testentity (id, nameValue, counter, userid, owner) "
            + "values (5, 'rosen', 255, 'Klaus', 'Lalla')", CibetUtil.decode(rs.getBytes("TARGETOBJECT")));

      Assert.assertTrue(rs.next());
      Assert.assertEquals("DELETE", rs.getString("CONTROLEVENT"));
      Assert.assertEquals("5", rs.getString("PRIMARYKEYID"));
      Assert.assertEquals("cib_testentity", rs.getString("TARGET"));
      Assert.assertEquals("delete from cib_testentity WHERE id=5 ", CibetUtil.decode(rs.getBytes("TARGETOBJECT")));
   }

   @Test
   public void executeUpdateArchive4e() throws Exception {
      log.debug("start executeUpdateArchive4e() with database");

      sp = registerSetpoint("cib_testentity", FourEyesActuator.DEFAULTNAME, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE);

      execute("insert into cib_testentity (id, nameValue, counter, userid, owner) "
            + "values (5, 'rosen', 255, 'Klaus', 'Lalla')", 0);

      Context.end();
      Context.start();

      ResultSet rs = query("select * from cib_testentity");
      Assert.assertTrue(!rs.next());

      rs = query(
            "select d.*, r.primarykeyid, r.target from cib_controllable d, cib_resource r where d.resourceid = r.resourceid");
      Assert.assertTrue(rs.next());
      Assert.assertEquals("INSERT", rs.getString("CONTROLEVENT"));
      Assert.assertEquals("5", rs.getString("PRIMARYKEYID"));
      Assert.assertEquals("cib_testentity", rs.getString("TARGET"));

      log.debug("now release");
      List<Controllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());
      Controllable co = l.get(0);

      Context.sessionScope().setUser("test2");
      Object res = co.release(Context.requestScope().getEntityManager(), null);
      checkResult(res);

      Context.end();
      Context.start();

      Context.sessionScope().setUser(USER);
      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      rs = query("select namevalue, counter from cib_testentity where id = 5");
      Assert.assertTrue(rs.next());
   }

   protected void checkResult(Object res) {
      Assert.assertEquals(Integer.class, res.getClass());
      Assert.assertEquals(1, res);
   }

   @Test
   public void executeUpdateArchive_Update4e() throws Exception {
      log.debug("start executeUpdateArchive_Update4e() with database");

      sp = registerSetpoint("cib_testentity", FourEyesActuator.DEFAULTNAME, ControlEvent.UPDATE, ControlEvent.DELETE);

      execute("insert into cib_testentity (id, nameValue, counter, userid, owner) "
            + "values (5, 'rosen', 255, 'Klaus', 'Lalla')", 1);

      execute("update cib_testentity set nameValue='R�schen', counter=99 " + "WHERE id=5 and owner='Lalla'", 0);

      Context.end();
      Context.start();

      ResultSet rs = query("select * from cib_testentity");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(5L, rs.getLong(1));
      Assert.assertEquals("rosen", rs.getString(2));
      Assert.assertEquals(255, rs.getInt(3));

      rs = query(
            "select d.*, r.primarykeyid, r.target, r.targetobject from cib_controllable d, cib_resource r where d.resourceid = r.resourceid");
      Assert.assertTrue(rs.next());
      Assert.assertEquals("UPDATE", rs.getString("CONTROLEVENT"));
      Assert.assertEquals("5", rs.getString("PRIMARYKEYID"));
      Assert.assertEquals("cib_testentity", rs.getString("TARGET"));
      Assert.assertEquals("update cib_testentity set nameValue='R�schen', counter=99 " + "WHERE id=5 and owner='Lalla'",
            CibetUtil.decode(rs.getBytes("TARGETOBJECT")));

      log.debug("now release");
      List<Controllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());
      Controllable co = l.get(0);

      Context.sessionScope().setUser("test2");
      Object res = co.release(new JdbcBridgeEntityManager(connection), null);
      checkResult(res);

      Context.sessionScope().setUser(USER);
      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      rs = query("select namevalue, counter from cib_testentity where id = 5");
      Assert.assertTrue(rs.next());
      Assert.assertEquals("R�schen", rs.getString(1));
      Assert.assertEquals(99, rs.getLong(2));
   }

   @Test
   public void executeUpdateArchive_Delete4e() throws Exception {
      log.debug("start executeUpdateArchive_Delete4e() with database");

      sp = registerSetpoint("cib_testentity", FourEyesActuator.DEFAULTNAME, ControlEvent.UPDATE, ControlEvent.DELETE);

      execute("insert into cib_testentity (id, nameValue, counter, userid, owner) "
            + "values (5, 'rosen', 255, 'Klaus', 'Lalla')", 1);

      execute("delete from cib_testentity WHERE id=5 ", 0);

      Context.end();
      Context.start();

      ResultSet rs = query("select * from cib_testentity");
      Assert.assertTrue(rs.next());

      rs = query(
            "select d.*, r.primarykeyid, r.target, r.targetobject from cib_controllable d, cib_resource r where d.resourceid = r.resourceid");
      Assert.assertTrue(rs.next());
      Assert.assertEquals("DELETE", rs.getString("CONTROLEVENT"));
      Assert.assertEquals("5", rs.getString("PRIMARYKEYID"));
      Assert.assertEquals("cib_testentity", rs.getString("TARGET"));
      Assert.assertEquals("delete from cib_testentity WHERE id=5 ", CibetUtil.decode(rs.getBytes("TARGETOBJECT")));

      log.debug("now release");
      List<Controllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());
      Controllable co = l.get(0);

      Context.sessionScope().setUser("test2");
      Object res = co.release(new JdbcBridgeEntityManager(connection), null);
      checkResult(res);

      Context.sessionScope().setUser(USER);
      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      rs = query("select namevalue, counter from cib_testentity where id = 5");
      Assert.assertTrue(!rs.next());
   }

   @Test
   public void persistWith2ManRuleJdbc() throws Exception {
      log.info("start persistWith2ManRuleJdbc()");

      Context.sessionScope().setSecondUser(null);
      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint("cib_testentity", schemes, ControlEvent.INSERT);

      execute("insert into cib_testentity (id, nameValue, counter, userid, owner) "
            + "values (5, 'rosen', 255, 'Klaus', 'Lalla')", 0);
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
   }

   @Test
   public void persistWith2ManRuleDirectReleaseJdbc() throws Exception {
      log.info("start persistWith2ManRuleDirectReleaseJdbc()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      sp = registerSetpoint("cib_testentity", schemes, ControlEvent.INSERT, ControlEvent.RELEASE_INSERT);

      Context.sessionScope().setSecondUser("secondUser");
      execute("insert into cib_testentity (id, nameValue, counter, userid, owner) "
            + "values (5, 'rosen', 255, 'Klaus', 'Lalla')", 1);
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
   }

   @Test
   public void persistWith2ManRuleLaterReleaseJdbc() throws Exception {
      log.info("start persistWith2ManRuleLaterReleaseJdbc()");
      Context.sessionScope().setSecondUser(null);

      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint("cib_testentity", schemes, ControlEvent.INSERT, ControlEvent.RELEASE);

      execute("insert into cib_testentity (id, nameValue, counter, userid, owner) "
            + "values (5, 'rosen', 255, 'Klaus', 'Lalla')", 0);
      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      Context.end();
      Context.start();
      Context.sessionScope().setUser(USER);

      log.info("now release");
      Context.sessionScope().setSecondUser("secondUser");
      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Controllable dcOb = list1.get(0);
      Assert.assertEquals(ControlEvent.INSERT, dcOb.getControlEvent());
      Object res = dcOb.release(new JdbcBridgeEntityManager(connection), "2man rule test");
      checkResult(res);
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      Context.sessionScope().setSecondUser(null);

      ResultSet rs = query("select namevalue, counter from cib_testentity where id = 5");
      Assert.assertTrue(rs.next());

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId("cib_testentity", "5");
      Assert.assertEquals(2, list.size());
      Archive ar = list.get(0);
      Assert.assertTrue(
            ControlEvent.INSERT == ar.getControlEvent() || ControlEvent.RELEASE_INSERT == ar.getControlEvent());
      Assert.assertTrue(!((JdbcResource) ar.getResource()).getPrimaryKeyId().equals("0"));

      list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());
   }

   @Test
   public void deleteWith2ManRuleJdbc() throws Exception {
      log.info("start deleteWith2ManRuleJdbc()");

      Context.sessionScope().setSecondUser(null);
      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint("cib_testentity", schemes, ControlEvent.DELETE);

      execute("insert into cib_testentity (id, nameValue, counter, userid, owner) "
            + "values (5, 'rosen', 255, 'Klaus', 'Lalla')", 1);
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      Context.end();
      Context.start();

      ResultSet rs = query("select namevalue, counter from cib_testentity where id = 5");
      Assert.assertTrue(rs.next());

      execute("delete from cib_testentity WHERE id=5 ", 0);
      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId("cib_testentity", "5");
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Assert.assertEquals(ControlEvent.DELETE, ar.getControlEvent());

      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Controllable dcOb = list1.get(0);
      Assert.assertEquals(ControlEvent.DELETE, dcOb.getControlEvent());
   }

   @Test
   public void deleteWith2ManRuleDirectReleaseJdbc() throws Exception {
      log.info("start deleteWith2ManRuleDirectReleaseJdbc()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      sp = registerSetpoint("cib_testentity", schemes, ControlEvent.DELETE, ControlEvent.RELEASE_DELETE);

      execute("insert into cib_testentity (id, nameValue, counter, userid, owner) "
            + "values (5, 'rosen', 255, 'Klaus', 'Lalla')", 1);

      Context.sessionScope().setSecondUser("secondUser");
      execute("delete from cib_testentity WHERE id=5 ", 1);
      Context.sessionScope().setSecondUser(null);
      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      Context.end();
      Context.start();

      ResultSet rs = query("select namevalue, counter from cib_testentity where id = 5");
      Assert.assertTrue(!rs.next());

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId("cib_testentity", "5");
      Assert.assertEquals(2, list.size());
      Archive ar = list.get(0);
      Assert.assertTrue(
            ControlEvent.DELETE == ar.getControlEvent() || ControlEvent.RELEASE_DELETE == ar.getControlEvent());

      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());
   }

   @Test
   public void deleteWith2ManRuleLaterReleaseJdbc() throws Exception {
      log.info("start deleteWith2ManRuleLaterReleaseJdbc()");
      Context.sessionScope().setSecondUser(null);

      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint("cib_testentity", schemes, ControlEvent.DELETE, ControlEvent.RELEASE);

      execute("insert into cib_testentity (id, nameValue, counter, userid, owner) "
            + "values (5, 'rosen', 255, 'Klaus', 'Lalla')", 1);

      execute("delete from cib_testentity WHERE id=5 ", 0);
      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      Context.end();
      Context.start();
      Context.sessionScope().setUser(USER);

      ResultSet rs = query("select namevalue, counter from cib_testentity where id = 5");
      Assert.assertTrue(rs.next());

      log.info("now release");
      Context.sessionScope().setSecondUser("secondUser");
      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Controllable dcOb = list1.get(0);
      Assert.assertEquals(ControlEvent.DELETE, dcOb.getControlEvent());
      Object res = dcOb.release(new JdbcBridgeEntityManager(connection), "2man rule test");
      checkResult(res);
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      Context.sessionScope().setSecondUser(null);

      rs = query("select namevalue, counter from cib_testentity where id = 5");
      Assert.assertTrue(!rs.next());

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId("cib_testentity", "5");
      Assert.assertEquals(2, list.size());
      Archive ar = list.get(0);
      Assert.assertTrue(
            ControlEvent.DELETE == ar.getControlEvent() || ControlEvent.RELEASE_DELETE == ar.getControlEvent());

      list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());
   }

   @Test
   public void updateWith2ManRuleJdbc() throws Exception {
      log.info("start updateWith2ManRuleJdbc()");

      Context.sessionScope().setSecondUser(null);
      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint("cib_testentity", schemes, ControlEvent.UPDATE);

      execute("insert into cib_testentity (id, nameValue, counter, userid, owner) "
            + "values (5, 'rosen', 255, 'Klaus', 'Lalla')", 1);

      execute("update cib_testentity set nameValue='R�schen', counter=99 " + "WHERE id=5 and owner='Lalla'", 0);
      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      Context.end();
      Context.start();

      ResultSet rs = query("select namevalue, counter from cib_testentity where id = 5");
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
   }

   @Test
   public void updateWith2ManRuleDirectReleaseJdbc() throws Exception {
      log.info("start updateWith2ManRuleDirectReleaseJdbc()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      sp = registerSetpoint("cib_testentity", schemes, ControlEvent.UPDATE, ControlEvent.RELEASE_UPDATE);

      Context.sessionScope().setSecondUser("secondUser");
      execute("insert into cib_testentity (id, nameValue, counter, userid, owner) "
            + "values (5, 'rosen', 255, 'Klaus', 'Lalla')", 1);

      execute("update cib_testentity set nameValue='R�schen', counter=99 " + "WHERE id=5 and owner='Lalla'", 1);
      Context.sessionScope().setSecondUser(null);
      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      Context.end();
      Context.start();

      ResultSet rs = query("select namevalue, counter from cib_testentity where id = 5");
      Assert.assertTrue(rs.next());
      Assert.assertEquals("R�schen", rs.getString(1));

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId("cib_testentity", "5");
      Assert.assertEquals(2, list.size());
      Archive ar = list.get(0);
      Assert.assertTrue(
            ControlEvent.UPDATE == ar.getControlEvent() || ControlEvent.RELEASE_UPDATE == ar.getControlEvent());

      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());
   }

   @Test
   public void updateWith2ManRuleLaterReleaseJdbc() throws Exception {
      log.info("start updateWith2ManRuleLaterReleaseJdbc()");

      Context.sessionScope().setSecondUser(null);
      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint("cib_testentity", schemes, ControlEvent.UPDATE, ControlEvent.RELEASE);

      execute("insert into cib_testentity (id, nameValue, counter, userid, owner) "
            + "values (5, 'rosen', 255, 'Klaus', 'Lalla')", 1);

      execute("update cib_testentity set nameValue='R�schen', counter=99 " + "WHERE id=5 and owner='Lalla'", 0);

      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      Context.end();
      Context.start();
      Context.sessionScope().setUser(USER);

      ResultSet rs = query("select namevalue, counter from cib_testentity where id = 5");
      Assert.assertTrue(rs.next());
      Assert.assertEquals("rosen", rs.getString(1));

      log.info("now release");
      Context.sessionScope().setSecondUser("secondUser");
      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Controllable dcOb = list1.get(0);
      Assert.assertEquals(ControlEvent.UPDATE, dcOb.getControlEvent());
      dcOb.release(new JdbcBridgeEntityManager(connection), "2man rule test");
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      Context.sessionScope().setSecondUser(null);

      rs = query("select namevalue, counter from cib_testentity where id = 5");
      Assert.assertTrue(rs.next());
      Assert.assertEquals("R�schen", rs.getString(1));

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId("cib_testentity", "5");
      Assert.assertEquals(2, list.size());
      Archive ar = list.get(0);
      Assert.assertTrue(
            ControlEvent.UPDATE == ar.getControlEvent() || ControlEvent.RELEASE_UPDATE == ar.getControlEvent());

      list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());
   }

   @Test(expected = DeniedException.class)
   public void persistWith2ManRuleDirectReleaseDeniedJdbc() throws Exception {
      log.info("start persistWith2ManRuleDirectReleaseDeniedJdbc()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context_1.xml" });

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");
      act.setThrowDeniedException(true);

      List<String> schemes = new ArrayList<String>();
      schemes.add(SpringSecurityActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      sp = registerSetpoint("cib_testentity", schemes, ControlEvent.INSERT);

      SpringSecurityActuator releaseSec = new SpringSecurityActuator("RELEASESEC");
      releaseSec.setPreAuthorize("hasAnyRole('Wooo')");
      releaseSec.setThrowDeniedException(true);
      releaseSec.setSecondPrincipal(true);
      Configuration.instance().registerActuator(releaseSec);

      Thread.sleep(50);
      List<String> schemes2 = new ArrayList<String>();
      schemes2.add(releaseSec.getName());
      schemes2.add(TwoManRuleActuator.DEFAULTNAME);
      Setpoint sp2 = registerSetpoint("cib_testentity", schemes2, ControlEvent.RELEASE);

      authenticate("Heinz");

      Context.sessionScope().setSecondUser("secondUser");
      try {
         execute("insert into cib_testentity (id, nameValue, counter, userid, owner) "
               + "values (5, 'rosen', 255, 'Klaus', 'Lalla')", 1);
      } finally {
         Context.sessionScope().setSecondUser(null);
         Configuration.instance().unregisterSetpoint(sp2.getId());
      }
   }

   @Test
   public void persistWith2ManRuleDirectReleaseGranted_jdbc() throws Exception {
      log.info("start persistWith2ManRuleDirectReleaseGranted_jdbc()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context_1.xml" });

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");
      act.setThrowDeniedException(true);

      List<String> schemes = new ArrayList<String>();
      schemes.add(SpringSecurityActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      sp = registerSetpoint("cib_testentity", schemes, ControlEvent.INSERT);

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

      execute("insert into cib_testentity (id, nameValue, counter, userid, owner) "
            + "values (5, 'rosen', 255, 'Klaus', 'Lalla')", 1);

      Context.sessionScope().setSecondUser(null);
      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      ResultSet rs = query("select namevalue, counter from cib_testentity where id = 5");
      Assert.assertTrue(rs.next());

      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());
      Configuration.instance().unregisterSetpoint(sp2.getId());
   }

   @Test
   public void releaseRemove6Eyes_jdbc() throws Exception {
      log.info("start releaseRemove6Eyes_jdbc()");

      sp = registerSetpoint("cib_testentity", SixEyesActuator.DEFAULTNAME, ControlEvent.DELETE,
            ControlEvent.RELEASE_DELETE, ControlEvent.FIRST_RELEASE_DELETE);

      execute("insert into cib_testentity (id, nameValue, counter, userid, owner) "
            + "values (5, 'rosen', 255, 'Klaus', 'Lalla')", 1);
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      ResultSet rs = query("select namevalue, counter from cib_testentity where id = 5");
      Assert.assertTrue(rs.next());

      execute("delete from cib_testentity WHERE id=5 ", 0);

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
      rs = query("select namevalue, counter from cib_testentity where owner = 'Lalla'");
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

      // now it is removed
      rs = query("select namevalue, counter from cib_testentity where owner = 'Lalla'");
      Assert.assertTrue(!rs.next());
   }

   @Test
   public void insertDenied_jdbc() throws Exception {
      log.debug("start insertDenied_jdbc()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context_1.xml" });

      sp = registerSetpoint("cib_testentity", SpringSecurityActuator.DEFAULTNAME, ControlEvent.INSERT);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");
      act.setThrowDeniedException(true);

      authenticate("WILLI");

      try {
         execute("insert into cib_testentity (id, nameValue, counter, userid, owner) "
               + "values (5, 'rosen', 255, 'Klaus', 'Lalla')", 0);
         Assert.fail();
      } catch (DeniedException e) {
      }
      Assert.assertEquals(ExecutionStatus.DENIED, Context.requestScope().getExecutedEventResult().getExecutionStatus());
      ResultSet rs = query("select namevalue, counter from cib_testentity where id = 5");
      Assert.assertTrue(!rs.next());
   }

   @Test
   public void insertOk_jdbc() throws Exception {
      log.debug("start insertOk_jdbc()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context_1.xml" });

      sp = registerSetpoint("cib_testentity", SpringSecurityActuator.DEFAULTNAME, ControlEvent.INSERT);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");

      authenticate("Heinz");

      execute("insert into cib_testentity (id, nameValue, counter, userid, owner) "
            + "values (5, 'rosen', 255, 'Klaus', 'Lalla')", 1);

      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      ResultSet rs = query("select namevalue, counter from cib_testentity where id = 5");
      Assert.assertTrue(rs.next());
   }

   @Test
   public void lockObjectLockedSameUser_jdbc() throws Exception {
      log.info("start lockObjectLockedSameUser_jdbc()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint("cib_testentity", schemes, ControlEvent.UPDATE);

      execute("insert into cib_testentity (id, nameValue, counter, userid, owner) "
            + "values (5, 'rosen', 255, 'Klaus', 'Lalla')", 1);
      ResultSet rs = query("select namevalue, counter from cib_testentity where id = 5");
      Assert.assertTrue(rs.next());
      Controllable lo = Locker.lock("cib_testentity", "5", ControlEvent.UPDATE, "testremark");
      Assert.assertNotNull(lo);

      log.debug("now update");
      execute("update cib_testentity set nameValue='R�schen', counter=99 " + "WHERE id=5 and owner='Lalla'", 1);
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      rs = query("select namevalue, counter from cib_testentity where id = 5");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(99, rs.getLong(2));

      Context.end();
      Context.start();

      List<Archive> list = ArchiveLoader.loadArchives("cib_testentity");
      Assert.assertEquals(1, list.size());
      log.debug("ARCHIVE: " + list.get(0));
      Assert.assertEquals(ControlEvent.UPDATE, list.get(0).getControlEvent());
      Assert.assertEquals(USER, list.get(0).getCreateUser());
      Assert.assertNull(list.get(0).getRemark());
      Assert.assertNotNull(list.get(0).getResource().getTargetObject());

      List<Controllable> l2 = Locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());
      log.debug("LOCKEDOBJECT: " + l2.get(0));
   }

   @Test
   public void lockClassLockedOtherUser_jdbc() throws Exception {
      log.info("start lockClassLockedOtherUser_jdbc()");

      execute("insert into cib_testentity (id, nameValue, counter, userid, owner) "
            + "values (5, 'rosen', 255, 'Klaus', 'Lalla')", 1);

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint("cib_testentity", schemes, ControlEvent.PERSIST);

      Controllable lo = Locker.lock("cib_testentity", (String) null, ControlEvent.UPDATE, "testremark");
      Assert.assertNotNull(lo);

      Context.sessionScope().setUser("otherUser");
      execute("update cib_testentity set nameValue='R�schen', counter=99 " + "WHERE id=5 and owner='Lalla'", 0);
      Assert.assertEquals(ExecutionStatus.DENIED, Context.requestScope().getExecutedEventResult().getExecutionStatus());

      Context.end();
      Context.start();

      List<Archive> list = ArchiveLoader.loadArchives("cib_testentity");
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(ExecutionStatus.DENIED, list.get(0).getExecutionStatus());

      List<Controllable> l2 = Locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());
      log.debug("LOCKEDOBJECT: " + l2.get(0));
   }

   @Test(expected = SQLException.class)
   public void executeUpdateAlter() throws Exception {
      log.debug("start executeUpdateAlter() with database");
      execute("Alter table xxxxxx add columnvv varchar(20)", 1);
   }

   @Test
   public void executeUpdateNull() throws Exception {
      log.debug("start executeUpdateNull() with database");
      try {
         execute(null, 1);
         Assert.fail();
      } catch (Exception e) {
         log.debug(e.getMessage(), e);
         Assert.assertTrue(e instanceof IllegalArgumentException);
      }
   }

   @Test(expected = SQLException.class)
   public void executeUpdateDrop() throws Exception {
      log.debug("start executeUpdateDrop() with database");
      execute("drop table xxxxxx", 1);
   }

   @Test
   public void executeUpdateWithException() throws Exception {
      log.debug("start executeUpdateWithException() with database");

      sp = registerSetpoint("cib_testentity", ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE);

      try {
         execute("insert into cib_testentity (id, nameValue, counter, userid, owner) "
               + "values ('5xxx', 'rosen', 255, 'Klaus', 'Lalla')", 1);
         Assert.fail();
      } catch (SQLException e) {
      }

      Context.end();
      Context.start();

      ResultSet rs = query(
            "select a.*, r.primarykeyid, r.target from cib_archive a, cib_resource r where a.resourceid = r.resourceid");
      Assert.assertTrue(rs.next());
      Assert.assertEquals("INSERT", rs.getString("CONTROLEVENT"));
      Assert.assertEquals("5xxx", rs.getString("PRIMARYKEYID"));
      Assert.assertEquals("cib_testentity", rs.getString("TARGET"));
      Assert.assertEquals(ExecutionStatus.ERROR.name(), rs.getString("EXECUTIONSTATUS"));
   }

}
