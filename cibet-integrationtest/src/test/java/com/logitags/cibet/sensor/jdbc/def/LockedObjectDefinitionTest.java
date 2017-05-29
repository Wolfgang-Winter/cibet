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

package com.logitags.cibet.sensor.jdbc.def;

import org.junit.Test;

import com.cibethelper.base.JdbcHelper;

/**
 *
 */
public class LockedObjectDefinitionTest extends JdbcHelper {

   @Test
   public void c() {

   }

   //
   // /**
   // * logger for tracing
   // */
   // private static Logger log = Logger.getLogger(LockedObjectDefinitionTest.class);
   //
   // @Before
   // public void beforeJdbcBridgeEntityManagerIntegrationTest() throws Exception {
   // Context.start();
   // }
   //
   // @After
   // public void afterJdbcBridgeEntityManagerIntegrationTest() throws Exception {
   // Context.end();
   // }
   //
   // private LockedObject createLockedObject() throws IOException {
   // LockedObject sa = new LockedObject();
   // sa.setLockDate(new Date());
   // sa.setLockedBy(USER);
   // sa.setLockedEvent(ControlEvent.INSERT);
   // sa.setLockRemark("lrem");
   // sa.setTargetType("class");
   // sa.setTenant(TENANT);
   // sa.setLockState(LockState.LOCKED);
   // sa.setObject(CibetUtil.encode(sa));
   // sa.setMethod("haha");
   // return sa;
   // }
   //
   // @Test
   // public void persistLockedObject() throws Exception {
   // log.info("start persistLockedObject()");
   // connection.setAutoCommit(false);
   //
   // try {
   // LockedObject sa = createLockedObject();
   // EntityDefinition def = LockedObjectDefinition.getInstance();
   // def.persist(connection, sa);
   //
   // PreparedStatement ps = connection
   // .prepareStatement("SELECT lockedobjectid, targettype, object FROM CIB_LOCKEDOBJECT");
   // ResultSet rs = ps.executeQuery();
   // Assert.assertTrue(rs.next());
   // Assert.assertNotNull(rs.getString(1));
   // Assert.assertEquals("class", rs.getString(2));
   // byte[] r2 = rs.getBytes(3);
   // Assert.assertNotNull(r2);
   // Object obj = CibetUtil.decode(r2);
   // Assert.assertEquals(LockedObject.class, obj.getClass());
   //
   // connection.commit();
   // log.info("end persistLockedObject()");
   // } finally {
   // if (connection != null) {
   // connection.rollback();
   // connection.setAutoCommit(true);
   // }
   // }
   // }
   //
   // @Test
   // public void persistAndFindLockedObject() throws Exception {
   // log.info("start persistAndFindLockedObject()");
   // connection.setAutoCommit(false);
   //
   // try {
   // LockedObject sa = createLockedObject();
   // JdbcBridgeEntityManager jdbcEM = new JdbcBridgeEntityManager(connection);
   //
   // jdbcEM.persist(sa);
   // connection.commit();
   // Assert.assertTrue(sa.getLockedObjectId() != null);
   //
   // LockedObject ar2 = jdbcEM.find(LockedObject.class, sa.getLockedObjectId());
   // Assert.assertNotNull(ar2);
   // Assert.assertEquals("class", ar2.getTargetType());
   // Assert.assertNotNull(ar2.getObject());
   // Object obj = CibetUtil.decode(ar2.getObject());
   // Assert.assertEquals(LockedObject.class, obj.getClass());
   //
   // connection.commit();
   // log.info("end persistAndFindLockedObject()");
   // } finally {
   // if (connection != null) {
   // connection.rollback();
   // connection.setAutoCommit(true);
   // }
   // }
   // }
   //
   // @Test
   // public void mergeLockedObject() throws Exception {
   // log.info("start mergeLockedObject()");
   // connection.setAutoCommit(false);
   //
   // try {
   // LockedObject sa = createLockedObject();
   // JdbcBridgeEntityManager jdbcEM = new JdbcBridgeEntityManager(connection);
   //
   // jdbcEM.persist(sa);
   // connection.commit();
   // Assert.assertTrue(sa.getLockedObjectId() != null);
   //
   // sa.setLockState(LockState.UNLOCKED);
   // sa.setTargetType(null);
   //
   // LockedObject sa2 = jdbcEM.merge(sa);
   // connection.commit();
   //
   // LockedObject ar2 = jdbcEM.find(LockedObject.class, sa.getLockedObjectId());
   // Assert.assertNotNull(ar2);
   // Assert.assertNull(ar2.getTargetType());
   // Assert.assertEquals(LockState.UNLOCKED, ar2.getLockState());
   // Assert.assertNotNull(ar2.getObject());
   // Object obj = CibetUtil.decode(ar2.getObject());
   // Assert.assertEquals(LockedObject.class, obj.getClass());
   //
   // connection.commit();
   // log.info("end mergeLockedObject()");
   // } finally {
   // if (connection != null) {
   // connection.rollback();
   // connection.setAutoCommit(true);
   // }
   // }
   // }
   //
   // @Test
   // public void createNamedQueryLockedObject() throws Exception {
   // log.info("start createNamedQueryLockedObject()");
   // connection.setAutoCommit(false);
   //
   // try {
   // LockedObject sa = createLockedObject();
   // Thread.sleep(100);
   // LockedObject sa2 = createLockedObject();
   // JdbcBridgeEntityManager jdbcEM = new JdbcBridgeEntityManager(connection);
   //
   // jdbcEM.persist(sa);
   // jdbcEM.persist(sa2);
   // connection.commit();
   // Assert.assertTrue(sa.getLockedObjectId() != null);
   // Assert.assertTrue(sa2.getLockedObjectId() != null);
   //
   // Query q = jdbcEM.createNamedQuery(LockedObject.SEL_LOCKED_ALL);
   // q.setParameter("tenant", TENANT);
   // List<LockedObject> list = q.getResultList();
   // Assert.assertEquals(2, list.size());
   // // Assert.assertEquals(sa.getLockedObjectId(), list.get(0)
   // // .getLockedObjectId());
   // // Assert.assertEquals(sa2.getLockedObjectId(), list.get(1)
   // // .getLockedObjectId());
   //
   // q = jdbcEM.createNamedQuery(LockedObject.SEL_LOCKED_BY_USER);
   // q.setParameter("tenant", TENANT);
   // q.setParameter("tenant2", USER);
   // list = q.getResultList();
   // Assert.assertEquals(2, list.size());
   // // Assert.assertEquals(sa.getLockedObjectId(), list.get(0)
   // // .getLockedObjectId());
   // // Assert.assertEquals(sa2.getLockedObjectId(), list.get(1)
   // // .getLockedObjectId());
   //
   // q = jdbcEM.createNamedQuery(LockedObject.SEL_LOCKED_BY_TARGETTYPE);
   // q.setParameter("tenant", TENANT);
   // q.setParameter("tenant2", "class");
   // list = q.getResultList();
   // Assert.assertEquals(2, list.size());
   // // Assert.assertEquals(sa.getLockedObjectId(), list.get(0)
   // // .getLockedObjectId());
   // // Assert.assertEquals(sa2.getLockedObjectId(), list.get(1)
   // // .getLockedObjectId());
   //
   // q = jdbcEM.createNamedQuery(LockedObject.SEL_LOCKED_BY_TARGETTYPE_METHOD);
   // q.setParameter("tenant", TENANT);
   // q.setParameter("tenant2", "class");
   // q.setParameter("tenant3", "haha");
   // list = q.getResultList();
   // Assert.assertEquals(2, list.size());
   // // Assert.assertEquals(sa.getLockedObjectId(), list.get(0)
   // // .getLockedObjectId());
   // // Assert.assertEquals(sa2.getLockedObjectId(), list.get(1)
   // // .getLockedObjectId());
   //
   // connection.commit();
   // log.info("end createNamedQueryLockedObject()");
   // } finally {
   // if (connection != null) {
   // connection.rollback();
   // connection.setAutoCommit(true);
   // }
   // }
   // }
   //
   // @Test
   // public void persistAndRemoveLockedObject() throws Exception {
   // log.info("start persistAndRemoveLockedObject()");
   // connection.setAutoCommit(false);
   //
   // try {
   // LockedObject sa = createLockedObject();
   // JdbcBridgeEntityManager jdbcEM = new JdbcBridgeEntityManager(connection);
   //
   // jdbcEM.persist(sa);
   // connection.commit();
   // Assert.assertTrue(sa.getLockedObjectId() != null);
   //
   // LockedObject ar2 = jdbcEM.find(LockedObject.class, sa.getLockedObjectId());
   // Assert.assertNotNull(ar2);
   //
   // jdbcEM.remove(ar2);
   // connection.commit();
   //
   // try {
   // ar2 = jdbcEM.find(LockedObject.class, sa.getLockedObjectId());
   // Assert.fail();
   // } catch (NoResultException e) {
   // }
   //
   // connection.commit();
   // log.info("end persistAndRemoveLockedObject()");
   // } finally {
   // if (connection != null) {
   // connection.rollback();
   // connection.setAutoCommit(true);
   // }
   // }
   // }
   //
}
