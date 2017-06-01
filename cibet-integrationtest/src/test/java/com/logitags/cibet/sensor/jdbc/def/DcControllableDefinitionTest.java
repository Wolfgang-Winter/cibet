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
import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.ejb.EJBInvoker;
import com.logitags.cibet.sensor.ejb.EjbResource;
import com.logitags.cibet.sensor.jdbc.bridge.JdbcBridgeEntityManager;
import com.logitags.cibet.sensor.jpa.JpaResource;

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

   private Controllable createStateControllable() throws IOException {
      Controllable sa = new Controllable();
      JpaResource r = new JpaResource();
      sa.setResource(r);

      sa.setCaseId("caseisxx");
      sa.setControlEvent(ControlEvent.INSERT);
      sa.setCreateDate(new Date());
      sa.setCreateUser(USER);
      r.setPrimaryKeyId("25");
      r.setTarget("class");
      sa.setTenant(TENANT);
      sa.setActuator("actuator");
      sa.setApprovalAddress("testaddress");
      sa.setExecutionStatus(ExecutionStatus.POSTPONED);
      byte[] r1 = CibetUtil.encode(sa);
      r.setTargetObject(r1);
      sa.setActuator("thisact");
      return sa;
   }

   private Controllable createServiceControllable() throws IOException {
      Controllable sa = new Controllable();
      EjbResource r = new EjbResource();
      sa.setResource(r);

      sa.setCaseId("caseisxx");
      sa.setControlEvent(ControlEvent.INSERT);
      sa.setCreateDate(new Date());
      sa.setCreateUser(USER);
      r.setTarget("class");
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
         r.addParameter(ap);
      }

      byte[] r1 = CibetUtil.encode(sa);
      r.setTargetObject(r1);
      return sa;
   }

   @Test
   public void persistStateControllable() throws Exception {
      log.info("start persistStateControllable()");
      connection.setAutoCommit(false);

      try {
         Controllable sa = createStateControllable();
         EntityDefinition def = ControllableDefinition.getInstance();
         def.persist(connection, sa);

         PreparedStatement ps = connection.prepareStatement(
               "SELECT d.controllableid, d.caseid, r.targetobject FROM CIB_CONTROLLABLE d, CIB_RESOURCE r where d.resourceid = r.resourceid");
         ResultSet rs = ps.executeQuery();
         Assert.assertTrue(rs.next());
         Assert.assertNotNull(rs.getString(1));
         Assert.assertEquals("caseisxx", rs.getString(2));
         byte[] r2 = rs.getBytes(3);
         Assert.assertNotNull(r2);
         Object obj = CibetUtil.decode(r2);
         Assert.assertEquals(Controllable.class, obj.getClass());

         connection.commit();
         log.info("end persistStateControllable()");
      } finally {
         if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
         }
      }
   }

   @Test
   public void persistServiceControllable() throws Exception {
      log.info("start persistServiceControllable()");
      connection.setAutoCommit(false);

      try {
         Controllable sa = createServiceControllable();
         EntityDefinition def = ControllableDefinition.getInstance();
         def.persist(connection, sa);

         PreparedStatement ps = connection.prepareStatement(
               "SELECT d.controllableid, d.caseid, r.targetobject, r.method FROM CIB_CONTROLLABLE d, CIB_RESOURCE r where d.resourceid = r.resourceid");
         ResultSet rs = ps.executeQuery();
         Assert.assertTrue(rs.next());
         Assert.assertNotNull(rs.getString(1));
         Assert.assertEquals("caseisxx", rs.getString(2));
         byte[] r2 = rs.getBytes(3);
         Assert.assertNotNull(r2);
         Object obj = CibetUtil.decode(r2);
         Assert.assertEquals(Controllable.class, obj.getClass());
         Assert.assertEquals(3, ((Controllable) obj).getResource().getParameters().size());
         Assert.assertEquals("methodname", rs.getString(4));

         ps = connection.prepareStatement("SELECT * FROM CIB_RESOURCEPARAMETER");
         rs = ps.executeQuery();
         Assert.assertTrue(rs.next());

         connection.commit();
         log.info("end persistControllable()");
      } finally {
         if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
         }
      }
   }

   @Test
   public void persistAndFindStateControllable() throws Exception {
      log.info("start persistAndFindStateControllable()");
      connection.setAutoCommit(false);

      try {
         Controllable sa = createStateControllable();
         JdbcBridgeEntityManager jdbcEM = new JdbcBridgeEntityManager(connection);

         jdbcEM.persist(sa);
         connection.commit();
         Assert.assertTrue(sa.getControllableId() != null);

         Controllable ar2 = jdbcEM.find(Controllable.class, sa.getControllableId());
         Assert.assertNotNull(ar2);
         Assert.assertEquals("caseisxx", ar2.getCaseId());
         Assert.assertNotNull(ar2.getResource().getTargetObject());
         Object obj = CibetUtil.decode(ar2.getResource().getTargetObject());
         Assert.assertEquals(Controllable.class, obj.getClass());

         connection.commit();
         log.info("end persistAndFindStateControllable()");
      } finally {
         if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
         }
      }
   }

   @Test
   public void persistAndFindServiceControllable() throws Exception {
      log.info("start persistAndFindServiceControllable()");
      connection.setAutoCommit(false);

      try {
         Controllable sa = createServiceControllable();
         JdbcBridgeEntityManager jdbcEM = new JdbcBridgeEntityManager(connection);

         jdbcEM.persist(sa);
         connection.commit();
         Assert.assertTrue(sa.getControllableId() != null);

         Controllable ar2 = jdbcEM.find(Controllable.class, sa.getControllableId());
         Assert.assertNotNull(ar2);
         Assert.assertEquals("caseisxx", ar2.getCaseId());
         Assert.assertEquals("methodname", ((EjbResource) ar2.getResource()).getMethod());
         Assert.assertNotNull(ar2.getResource().getTargetObject());
         Object obj = CibetUtil.decode(ar2.getResource().getTargetObject());
         Assert.assertEquals(Controllable.class, obj.getClass());

         connection.commit();
         log.info("end persistAndFindServiceControllable()");
      } finally {
         if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
         }
      }
   }

   @Test
   public void mergeStateControllable() throws Exception {
      log.info("start mergeStateControllable()");
      connection.setAutoCommit(false);

      try {
         Controllable sa = createStateControllable();
         JdbcBridgeEntityManager jdbcEM = new JdbcBridgeEntityManager(connection);

         jdbcEM.persist(sa);
         connection.commit();
         Assert.assertTrue(sa.getControllableId() != null);

         sa.setActuator("newact");
         ((JpaResource) sa.getResource()).setPrimaryKeyId(null);

         Controllable sa2 = jdbcEM.merge(sa);
         connection.commit();

         Controllable ar2 = jdbcEM.find(Controllable.class, sa.getControllableId());
         Assert.assertNotNull(ar2);
         Assert.assertEquals("newact", ar2.getActuator());
         Assert.assertNotNull(ar2.getResource().getTargetObject());
         Object obj = CibetUtil.decode(ar2.getResource().getTargetObject());
         Assert.assertEquals(Controllable.class, obj.getClass());

         connection.commit();
         log.info("end mergeStateControllable()");
      } finally {
         if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
         }
      }
   }

   @Test
   public void mergeServiceControllable() throws Exception {
      log.info("start mergeServiceControllable()");
      connection.setAutoCommit(false);

      try {
         Controllable sa = createServiceControllable();
         JdbcBridgeEntityManager jdbcEM = new JdbcBridgeEntityManager(connection);

         jdbcEM.persist(sa);
         connection.commit();
         Assert.assertTrue(sa.getControllableId() != null);

         sa.setActuator("newact");
         ((EjbResource) sa.getResource()).setMethod("Halleluja");
         jdbcEM.merge(sa);
         connection.commit();

         Controllable ar2 = jdbcEM.find(Controllable.class, sa.getControllableId());
         Assert.assertNotNull(ar2);
         Assert.assertEquals("caseisxx", ar2.getCaseId());
         Assert.assertEquals("newact", ar2.getActuator());
         Assert.assertNotNull(ar2.getResource().getTargetObject());
         Object obj = CibetUtil.decode(ar2.getResource().getTargetObject());
         Assert.assertEquals(Controllable.class, obj.getClass());
         Assert.assertEquals(3, ((Controllable) obj).getResource().getParameters().size());

         connection.commit();
         log.info("end mergeServiceControllable()");
      } finally {
         if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
         }
      }
   }

   @Test
   public void createNamedQueryServiceControllable() throws Exception {
      log.info("start createNamedQueryServiceControllable()");
      connection.setAutoCommit(false);

      try {
         Controllable sa = createServiceControllable();
         Thread.sleep(100);
         Controllable sa2 = createServiceControllable();
         JdbcBridgeEntityManager jdbcEM = new JdbcBridgeEntityManager(connection);

         jdbcEM.persist(sa);
         jdbcEM.persist(sa2);
         connection.commit();
         Assert.assertTrue(sa.getControllableId() != null);
         Assert.assertTrue(sa2.getControllableId() != null);

         Query q = jdbcEM.createNamedQuery(Controllable.SEL_BY_TENANT);
         q.setParameter("tenant", TENANT);
         List<Controllable> list = q.getResultList();
         Assert.assertEquals(2, list.size());
         // Assert.assertEquals(sa.getControllableId(), list.get(0)
         // .getControllableId());
         // Assert.assertEquals(sa2.getControllableId(), list.get(1)
         // .getControllableId());

         q = jdbcEM.createNamedQuery(Controllable.SEL_BY_TENANT_CLASS);
         q.setParameter("tenant", TENANT);
         q.setParameter("tenant2", "class");
         list = q.getResultList();
         Assert.assertEquals(2, list.size());
         // Assert.assertEquals(sa.getControllableId(), list.get(0)
         // .getControllableId());
         // Assert.assertEquals(sa2.getControllableId(), list.get(1)
         // .getControllableId());

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
   public void createNamedQueryStateControllable() throws Exception {
      log.info("start createNamedQueryStateControllable()");
      connection.setAutoCommit(false);

      try {
         Controllable sa = createStateControllable();
         Thread.sleep(100);
         Controllable sa2 = createStateControllable();
         JdbcBridgeEntityManager jdbcEM = new JdbcBridgeEntityManager(connection);

         jdbcEM.persist(sa);
         jdbcEM.persist(sa2);
         connection.commit();
         Assert.assertTrue(sa.getControllableId() != null);
         Assert.assertTrue(sa2.getControllableId() != null);

         Query q = jdbcEM.createNamedQuery(Controllable.SEL_BY_ID_CLASS);
         q.setParameter("tenant", "25");
         q.setParameter("tt", "class");
         List<Controllable> list = q.getResultList();
         Assert.assertEquals(2, list.size());

         connection.commit();
         log.info("end createNamedQueryStateControllable()");
      } finally {
         if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
         }
      }
   }

   @Test
   public void persistAndRemoveServiceControllable() throws Exception {
      log.info("start persistAndRemoveServiceControllable()");
      connection.setAutoCommit(false);

      try {
         Controllable sa = createServiceControllable();
         JdbcBridgeEntityManager jdbcEM = new JdbcBridgeEntityManager(connection);

         jdbcEM.persist(sa);
         connection.commit();
         Assert.assertTrue(sa.getControllableId() != null);

         Controllable ar2 = jdbcEM.find(Controllable.class, sa.getControllableId());
         Assert.assertNotNull(ar2);

         jdbcEM.remove(ar2);
         connection.commit();

         try {
            ar2 = jdbcEM.find(Controllable.class, sa.getControllableId());
            Assert.fail();
         } catch (NoResultException e) {
         }

         connection.commit();
         log.info("end persistAndRemoveServiceControllable()");
      } finally {
         if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
         }
      }
   }

}
