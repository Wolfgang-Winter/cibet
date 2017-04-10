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

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cibethelper.base.JdbcHelper;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.ejb.EJBInvoker;
import com.logitags.cibet.sensor.ejb.EjbResourceHandler;
import com.logitags.cibet.sensor.jdbc.bridge.JdbcBridgeEntityManager;
import com.logitags.cibet.sensor.jpa.JpaResourceHandler;

/**
 *
 */
public class DcControllableDefinitionTest extends JdbcHelper {

   /**
    * logger for tracing
    */
   private static Logger log = Logger.getLogger(DcControllableDefinitionTest.class);

   @Before
   public void beforeJdbcBridgeEntityManagerIntegrationTest() throws Exception {
      Context.start();
   }

   @After
   public void afterJdbcBridgeEntityManagerIntegrationTest() throws Exception {
      Context.end();
   }

   private DcControllable createStateDcControllable() throws IOException {
      DcControllable sa = new DcControllable();
      Resource r = new Resource();
      sa.setResource(r);
      r.setResourceHandlerClass(JpaResourceHandler.class.getName());

      sa.setCaseId("caseisxx");
      sa.setControlEvent(ControlEvent.INSERT);
      sa.setCreateDate(new Date());
      sa.setCreateUser(USER);
      r.setPrimaryKeyId("25");
      r.setTargetType("class");
      sa.setTenant(TENANT);
      sa.setActuator("actuator");
      sa.setApprovalAddress("testaddress");
      sa.setExecutionStatus(ExecutionStatus.POSTPONED);
      byte[] r1 = CibetUtil.encode(sa);
      r.setTarget(r1);
      sa.setActuator("thisact");
      return sa;
   }

   private DcControllable createServiceDcControllable() throws IOException {
      DcControllable sa = new DcControllable();
      Resource r = new Resource();
      sa.setResource(r);
      r.setResourceHandlerClass(EjbResourceHandler.class.getName());

      sa.setCaseId("caseisxx");
      sa.setControlEvent(ControlEvent.INSERT);
      sa.setCreateDate(new Date());
      sa.setCreateUser(USER);
      r.setTargetType("class");
      sa.setTenant(TENANT);
      r.setMethod("methodname");
      r.setInvokerClass(EJBInvoker.class.getName());
      sa.setActuator("actuator");
      sa.setApprovalUser("testaddress");
      sa.setApprovalDate(new Date());
      sa.setExecutionStatus(ExecutionStatus.POSTPONED);

      for (int i = 1; i < 4; i++) {
         ResourceParameter ap = new ResourceParameter();
         ap.setClassname("clazzname" + i);
         ap.setName("myname" + i);
         ap.setParameterType(ParameterType.METHOD_PARAMETER);
         ap.setSequence(i);
         ap.setUnencodedValue(ap);
         r.getParameters().add(ap);
      }

      byte[] r1 = CibetUtil.encode(sa);
      r.setTarget(r1);
      return sa;
   }

   @Test
   public void persistStateDcControllable() throws Exception {
      log.info("start persistStateDcControllable()");
      connection.setAutoCommit(false);

      try {
         DcControllable sa = createStateDcControllable();
         EntityDefinition def = DcControllableDefinition.getInstance();
         def.persist(connection, sa);

         PreparedStatement ps = connection
               .prepareStatement("SELECT dccontrollableid, caseid, target FROM CIB_DCCONTROLLABLE");
         ResultSet rs = ps.executeQuery();
         Assert.assertTrue(rs.next());
         Assert.assertNotNull(rs.getString(1));
         Assert.assertEquals("caseisxx", rs.getString(2));
         byte[] r2 = rs.getBytes(3);
         Assert.assertNotNull(r2);
         Object obj = CibetUtil.decode(r2);
         Assert.assertEquals(DcControllable.class, obj.getClass());

         connection.commit();
         log.info("end persistStateDcControllable()");
      } finally {
         if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
         }
      }
   }

   @Test
   public void persistServiceDcControllable() throws Exception {
      log.info("start persistServiceDcControllable()");
      connection.setAutoCommit(false);

      try {
         DcControllable sa = createServiceDcControllable();
         EntityDefinition def = DcControllableDefinition.getInstance();
         def.persist(connection, sa);

         PreparedStatement ps = connection
               .prepareStatement("SELECT dccontrollableid, caseid, target, method FROM CIB_DCCONTROLLABLE");
         ResultSet rs = ps.executeQuery();
         Assert.assertTrue(rs.next());
         Assert.assertNotNull(rs.getString(1));
         Assert.assertEquals("caseisxx", rs.getString(2));
         byte[] r2 = rs.getBytes(3);
         Assert.assertNotNull(r2);
         Object obj = CibetUtil.decode(r2);
         Assert.assertEquals(DcControllable.class, obj.getClass());
         Assert.assertEquals(3, ((DcControllable) obj).getResource().getParameters().size());
         Assert.assertEquals("methodname", rs.getString(4));

         ps = connection.prepareStatement("SELECT * FROM CIB_RESOURCEPARAMETER");
         rs = ps.executeQuery();
         Assert.assertTrue(rs.next());

         connection.commit();
         log.info("end persistDcControllable()");
      } finally {
         if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
         }
      }
   }

   @Test
   public void persistAndFindStateDcControllable() throws Exception {
      log.info("start persistAndFindStateDcControllable()");
      connection.setAutoCommit(false);

      try {
         DcControllable sa = createStateDcControllable();
         JdbcBridgeEntityManager jdbcEM = new JdbcBridgeEntityManager(connection);

         jdbcEM.persist(sa);
         connection.commit();
         Assert.assertTrue(sa.getDcControllableId() != null);

         DcControllable ar2 = jdbcEM.find(DcControllable.class, sa.getDcControllableId());
         Assert.assertNotNull(ar2);
         Assert.assertEquals("caseisxx", ar2.getCaseId());
         Assert.assertNotNull(ar2.getResource().getTarget());
         Object obj = CibetUtil.decode(ar2.getResource().getTarget());
         Assert.assertEquals(DcControllable.class, obj.getClass());

         connection.commit();
         log.info("end persistAndFindStateDcControllable()");
      } finally {
         if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
         }
      }
   }

   @Test
   public void persistAndFindServiceDcControllable() throws Exception {
      log.info("start persistAndFindServiceDcControllable()");
      connection.setAutoCommit(false);

      try {
         DcControllable sa = createServiceDcControllable();
         JdbcBridgeEntityManager jdbcEM = new JdbcBridgeEntityManager(connection);

         jdbcEM.persist(sa);
         connection.commit();
         Assert.assertTrue(sa.getDcControllableId() != null);

         DcControllable ar2 = jdbcEM.find(DcControllable.class, sa.getDcControllableId());
         Assert.assertNotNull(ar2);
         Assert.assertEquals("caseisxx", ar2.getCaseId());
         Assert.assertEquals("methodname", ar2.getResource().getMethod());
         Assert.assertNotNull(ar2.getResource().getTarget());
         Object obj = CibetUtil.decode(ar2.getResource().getTarget());
         Assert.assertEquals(DcControllable.class, obj.getClass());

         connection.commit();
         log.info("end persistAndFindServiceDcControllable()");
      } finally {
         if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
         }
      }
   }

   @Test
   public void mergeStateDcControllable() throws Exception {
      log.info("start mergeStateDcControllable()");
      connection.setAutoCommit(false);

      try {
         DcControllable sa = createStateDcControllable();
         JdbcBridgeEntityManager jdbcEM = new JdbcBridgeEntityManager(connection);

         jdbcEM.persist(sa);
         connection.commit();
         Assert.assertTrue(sa.getDcControllableId() != null);

         sa.setActuator("newact");
         sa.getResource().setPrimaryKeyId(null);

         DcControllable sa2 = jdbcEM.merge(sa);
         connection.commit();

         DcControllable ar2 = jdbcEM.find(DcControllable.class, sa.getDcControllableId());
         Assert.assertNotNull(ar2);
         Assert.assertNull(ar2.getResource().getPrimaryKeyId());
         Assert.assertEquals("newact", ar2.getActuator());
         Assert.assertNotNull(ar2.getResource().getTarget());
         Object obj = CibetUtil.decode(ar2.getResource().getTarget());
         Assert.assertEquals(DcControllable.class, obj.getClass());

         connection.commit();
         log.info("end mergeStateDcControllable()");
      } finally {
         if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
         }
      }
   }

   @Test
   public void mergeServiceDcControllable() throws Exception {
      log.info("start mergeServiceDcControllable()");
      connection.setAutoCommit(false);

      try {
         DcControllable sa = createServiceDcControllable();
         JdbcBridgeEntityManager jdbcEM = new JdbcBridgeEntityManager(connection);

         jdbcEM.persist(sa);
         connection.commit();
         Assert.assertTrue(sa.getDcControllableId() != null);

         sa.setActuator("newact");
         sa.getResource().setMethod("Halleluja");
         jdbcEM.merge(sa);
         connection.commit();

         DcControllable ar2 = jdbcEM.find(DcControllable.class, sa.getDcControllableId());
         Assert.assertNotNull(ar2);
         Assert.assertEquals("caseisxx", ar2.getCaseId());
         Assert.assertEquals("newact", ar2.getActuator());
         Assert.assertEquals("Halleluja", ar2.getResource().getMethod());
         Assert.assertNotNull(ar2.getResource().getTarget());
         Object obj = CibetUtil.decode(ar2.getResource().getTarget());
         Assert.assertEquals(DcControllable.class, obj.getClass());
         Assert.assertEquals(3, ((DcControllable) obj).getResource().getParameters().size());

         connection.commit();
         log.info("end mergeServiceDcControllable()");
      } finally {
         if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
         }
      }
   }

   @Test
   public void createNamedQueryServiceDcControllable() throws Exception {
      log.info("start createNamedQueryServiceDcControllable()");
      connection.setAutoCommit(false);

      try {
         DcControllable sa = createServiceDcControllable();
         Thread.sleep(100);
         DcControllable sa2 = createServiceDcControllable();
         JdbcBridgeEntityManager jdbcEM = new JdbcBridgeEntityManager(connection);

         jdbcEM.persist(sa);
         jdbcEM.persist(sa2);
         connection.commit();
         Assert.assertTrue(sa.getDcControllableId() != null);
         Assert.assertTrue(sa2.getDcControllableId() != null);

         Query q = jdbcEM.createNamedQuery(DcControllable.SEL_BY_TENANT);
         q.setParameter("tenant", TENANT);
         List<DcControllable> list = q.getResultList();
         Assert.assertEquals(2, list.size());
         // Assert.assertEquals(sa.getDcControllableId(), list.get(0)
         // .getDcControllableId());
         // Assert.assertEquals(sa2.getDcControllableId(), list.get(1)
         // .getDcControllableId());

         q = jdbcEM.createNamedQuery(DcControllable.SEL_BY_TENANT_CLASS);
         q.setParameter("tenant", TENANT);
         q.setParameter("tenant2", "class");
         list = q.getResultList();
         Assert.assertEquals(2, list.size());
         // Assert.assertEquals(sa.getDcControllableId(), list.get(0)
         // .getDcControllableId());
         // Assert.assertEquals(sa2.getDcControllableId(), list.get(1)
         // .getDcControllableId());

         connection.commit();
         log.info("end createNamedQueryServiceArchive()");
      } finally {
         if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
         }
      }
   }

   @Test
   public void createNamedQueryStateDcControllable() throws Exception {
      log.info("start createNamedQueryStateDcControllable()");
      connection.setAutoCommit(false);

      try {
         DcControllable sa = createStateDcControllable();
         Thread.sleep(100);
         DcControllable sa2 = createStateDcControllable();
         JdbcBridgeEntityManager jdbcEM = new JdbcBridgeEntityManager(connection);

         jdbcEM.persist(sa);
         jdbcEM.persist(sa2);
         connection.commit();
         Assert.assertTrue(sa.getDcControllableId() != null);
         Assert.assertTrue(sa2.getDcControllableId() != null);

         Query q = jdbcEM.createNamedQuery(DcControllable.SEL_BY_ID_CLASS);
         q.setParameter("tenant", "25");
         q.setParameter("tt", "class");
         List<DcControllable> list = q.getResultList();
         Assert.assertEquals(2, list.size());
         // Assert.assertEquals(sa.getDcControllableId(), list.get(0)
         // .getDcControllableId());
         // Assert.assertEquals(sa2.getDcControllableId(), list.get(1)
         // .getDcControllableId());

         connection.commit();
         log.info("end createNamedQueryStateDcControllable()");
      } finally {
         if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
         }
      }
   }

   @Test
   public void persistAndRemoveServiceDcControllable() throws Exception {
      log.info("start persistAndRemoveServiceDcControllable()");
      connection.setAutoCommit(false);

      try {
         DcControllable sa = createServiceDcControllable();
         JdbcBridgeEntityManager jdbcEM = new JdbcBridgeEntityManager(connection);

         jdbcEM.persist(sa);
         connection.commit();
         Assert.assertTrue(sa.getDcControllableId() != null);

         DcControllable ar2 = jdbcEM.find(DcControllable.class, sa.getDcControllableId());
         Assert.assertNotNull(ar2);

         jdbcEM.remove(ar2);
         connection.commit();

         try {
            ar2 = jdbcEM.find(DcControllable.class, sa.getDcControllableId());
            Assert.fail();
         } catch (NoResultException e) {
         }

         connection.commit();
         log.info("end persistAndRemoveServiceDcControllable()");
      } finally {
         if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
         }
      }
   }

}
