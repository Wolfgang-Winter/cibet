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

import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cibethelper.base.JdbcHelper;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.context.InitializationService;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.ejb.EJBInvoker;
import com.logitags.cibet.sensor.jdbc.bridge.JdbcBridgeEntityManager;
import com.logitags.cibet.sensor.jdbc.driver.JdbcResourceHandler;

/**
 *
 */
public class ArchiveDefinitionTest extends JdbcHelper {

   /**
    * logger for tracing
    */
   private static Logger log = Logger.getLogger(ArchiveDefinitionTest.class);

   @Before
   public void beforeJdbcBridgeEntityManagerIntegrationTest() throws Exception {
      InitializationService.instance().startContext();
   }

   @After
   public void afterJdbcBridgeEntityManagerIntegrationTest() throws Exception {
      InitializationService.instance().endContext();
   }

   private Archive createStateArchive() throws IOException {
      Archive sa = new Archive();
      Resource r = new Resource();
      r.setResourceHandlerClass(JdbcResourceHandler.class.getName());
      sa.setResource(r);
      sa.setCaseId("caseisxx");
      sa.setControlEvent(ControlEvent.INSERT);
      sa.setExecutionStatus(ExecutionStatus.EXECUTED);
      sa.setCreateDate(new Date());
      sa.setCreateUser(USER);
      r.setPrimaryKeyId("25");
      sa.setRemark("no");
      r.setTargetType("class");
      sa.setTenant(TENANT);
      byte[] r1 = CibetUtil.encode(sa);
      r.setTarget(r1);
      byte[] r2 = CibetUtil.encode(sa);
      r.setResult(r2);
      sa.setVersion(1);
      return sa;
   }

   private Archive createServiceArchive() throws IOException {
      Archive sa = new Archive();
      Resource r = new Resource();
      r.setResourceHandlerClass(JdbcResourceHandler.class.getName());
      sa.setResource(r);
      sa.setCaseId("caseisxx");
      sa.setControlEvent(ControlEvent.INSERT);
      sa.setExecutionStatus(ExecutionStatus.EXECUTED);
      sa.setCreateDate(new Date());
      sa.setCreateUser(USER);
      sa.setRemark("no");
      r.setTargetType("class");
      sa.setTenant(TENANT);
      r.setMethod("methodname");
      r.setInvokerClass(EJBInvoker.class.getName());
      sa.setVersion(1);

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
      log.debug("byte array length=" + r1.length);
      r.setTarget(r1);
      byte[] r2 = CibetUtil.encode(sa);
      r.setResult(r2);
      return sa;
   }

   @Test
   public void persistStateArchive() throws Exception {
      log.info("start persistStateArchive()");
      connection.setAutoCommit(false);

      try {
         Archive sa = createStateArchive();
         EntityDefinition def = ArchiveDefinition.getInstance();
         def.persist(connection, sa);

         PreparedStatement ps = connection.prepareStatement("SELECT archiveid, caseid, result FROM CIB_ARCHIVE");
         ResultSet rs = ps.executeQuery();
         Assert.assertTrue(rs.next());
         Assert.assertNotNull(rs.getString(1));
         Assert.assertEquals("caseisxx", rs.getString(2));
         byte[] r2 = rs.getBytes(3);
         Assert.assertNotNull(r2);
         Object obj = CibetUtil.decode(r2);
         Assert.assertEquals(Archive.class, obj.getClass());

         connection.commit();
         log.info("end persist()");
      } finally {
         if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
         }
      }
   }

   @Test
   public void persistServiceArchive() throws Exception {
      log.info("start persistServiceArchive()");
      connection.setAutoCommit(false);

      try {
         Archive sa = createServiceArchive();
         EntityDefinition def = ArchiveDefinition.getInstance();
         def.persist(connection, sa);

         PreparedStatement ps = connection
               .prepareStatement("SELECT archiveid, caseid, result, method FROM CIB_ARCHIVE");
         ResultSet rs = ps.executeQuery();
         Assert.assertTrue(rs.next());
         Assert.assertNotNull(rs.getString(1));
         Assert.assertEquals("caseisxx", rs.getString(2));
         byte[] r2 = rs.getBytes(3);
         Assert.assertNotNull(r2);
         Object obj = CibetUtil.decode(r2);
         Assert.assertEquals(Archive.class, obj.getClass());
         Assert.assertEquals(3, ((Archive) obj).getResource().getParameters().size());
         Assert.assertEquals("methodname", rs.getString(4));

         ps = connection.prepareStatement("SELECT * FROM CIB_RESOURCEPARAMETER");
         rs = ps.executeQuery();
         Assert.assertTrue(rs.next());

         connection.commit();
         log.info("end persist()");
      } finally {
         if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
         }
      }
   }

   @Test
   public void persistAndFindStateArchive() throws Exception {
      log.info("start persistAndFindStateArchive()");
      connection.setAutoCommit(false);

      try {
         Archive sa = createStateArchive();
         JdbcBridgeEntityManager jdbcEM = new JdbcBridgeEntityManager(connection);

         jdbcEM.persist(sa);
         connection.commit();
         Assert.assertTrue(sa.getArchiveId() != null);

         Archive ar2 = jdbcEM.find(Archive.class, sa.getArchiveId());
         Assert.assertNotNull(ar2);
         Assert.assertEquals("caseisxx", ar2.getCaseId());
         Assert.assertNotNull(ar2.getResource().getTarget());
         Object obj = CibetUtil.decode(ar2.getResource().getTarget());
         Assert.assertEquals(Archive.class, obj.getClass());

         connection.commit();
         log.info("end persistAndFindStateArchive()");
      } finally {
         if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
         }
      }
   }

   @Test
   public void persistAndFindServiceArchive() throws Exception {
      log.info("start persistAndFindServiceArchive()");
      connection.setAutoCommit(false);

      try {
         Archive sa = createServiceArchive();
         JdbcBridgeEntityManager jdbcEM = new JdbcBridgeEntityManager(connection);

         jdbcEM.persist(sa);
         connection.commit();
         Assert.assertTrue(sa.getArchiveId() != null);

         Archive ar2 = jdbcEM.find(Archive.class, sa.getArchiveId());
         Assert.assertNotNull(ar2);
         Assert.assertEquals("caseisxx", ar2.getCaseId());
         Assert.assertEquals("methodname", ar2.getResource().getMethod());
         Assert.assertNotNull(ar2.getResource().getTarget());
         Object obj = CibetUtil.decode(ar2.getResource().getTarget());
         Assert.assertEquals(Archive.class, obj.getClass());

         connection.commit();
         log.info("end persistAndFindServiceArchive()");
      } finally {
         if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
         }
      }
   }

   @Test
   public void mergeStateArchive() throws Exception {
      log.info("start mergeStateArchive()");
      connection.setAutoCommit(false);

      try {
         Archive sa = createStateArchive();
         JdbcBridgeEntityManager jdbcEM = new JdbcBridgeEntityManager(connection);

         jdbcEM.persist(sa);
         connection.commit();
         Assert.assertTrue(sa.getArchiveId() != null);

         sa.setRemark("WilliBert");
         sa.setCaseId(null);

         Archive sa2 = jdbcEM.merge(sa);
         connection.commit();

         Archive ar2 = jdbcEM.find(Archive.class, sa.getArchiveId());
         Assert.assertNotNull(ar2);
         Assert.assertNull(ar2.getCaseId());
         Assert.assertEquals("WilliBert", ar2.getRemark());
         Assert.assertNotNull(ar2.getResource().getTarget());
         Object obj = CibetUtil.decode(ar2.getResource().getTarget());
         Assert.assertEquals(Archive.class, obj.getClass());

         connection.commit();
         log.info("end mergeStateArchive()");
      } finally {
         if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
         }
      }
   }

   @Test
   public void mergeServiceArchive() throws Exception {
      log.info("start mergeServiceArchive()");
      connection.setAutoCommit(false);

      try {
         Archive sa = createServiceArchive();
         JdbcBridgeEntityManager jdbcEM = new JdbcBridgeEntityManager(connection);

         jdbcEM.persist(sa);
         connection.commit();
         Assert.assertTrue(sa.getArchiveId() != null);

         sa.setRemark("WilliBert");
         sa.getResource().setMethod("Halleluja");
         jdbcEM.merge(sa);
         connection.commit();

         Archive ar2 = jdbcEM.find(Archive.class, sa.getArchiveId());
         Assert.assertNotNull(ar2);
         Assert.assertEquals("caseisxx", ar2.getCaseId());
         Assert.assertEquals("WilliBert", ar2.getRemark());
         Assert.assertEquals("Halleluja", ar2.getResource().getMethod());
         Assert.assertNotNull(ar2.getResource().getTarget());
         Object obj = CibetUtil.decode(ar2.getResource().getTarget());
         Assert.assertEquals(Archive.class, obj.getClass());
         Assert.assertEquals(3, ((Archive) obj).getResource().getParameters().size());

         connection.commit();
         log.info("end mergeServiceArchive()");
      } finally {
         if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
         }
      }
   }

   @Test
   public void createNamedQueryServiceArchive() throws Exception {
      log.info("start createNamedQueryServiceArchive()");
      connection.setAutoCommit(false);

      try {
         Archive sa = createServiceArchive();
         Thread.sleep(100);
         Archive sa2 = createServiceArchive();
         JdbcBridgeEntityManager jdbcEM = new JdbcBridgeEntityManager(connection);

         jdbcEM.persist(sa);
         jdbcEM.persist(sa2);
         connection.commit();
         Assert.assertTrue(sa.getArchiveId() != null);
         Assert.assertTrue(sa2.getArchiveId() != null);

         Query q = jdbcEM.createNamedQuery(Archive.SEL_ALL);
         List<Archive> list = q.getResultList();
         Assert.assertEquals(2, list.size());
         Assert.assertEquals(sa.getArchiveId(), list.get(0).getArchiveId());
         Assert.assertEquals(sa2.getArchiveId(), list.get(1).getArchiveId());

         q = jdbcEM.createNamedQuery(Archive.SEL_ALL_BY_TENANT);
         q.setParameter("tenant", TENANT);
         list = q.getResultList();
         Assert.assertEquals(2, list.size());
         Assert.assertEquals(sa.getArchiveId(), list.get(0).getArchiveId());
         Assert.assertEquals(sa2.getArchiveId(), list.get(1).getArchiveId());

         q = jdbcEM.createNamedQuery(Archive.SEL_ALL_BY_CLASS);
         q.setParameter("tenant", TENANT);
         q.setParameter("tt", "class");
         list = q.getResultList();
         Assert.assertEquals(2, list.size());
         Assert.assertEquals(sa.getArchiveId(), list.get(0).getArchiveId());
         Assert.assertEquals(sa2.getArchiveId(), list.get(1).getArchiveId());

         q = jdbcEM.createNamedQuery(Archive.SEL_BY_METHODNAME);
         q.setParameter("tenant", TENANT);
         q.setParameter("tt", "class");
         q.setParameter("mn", "methodname");
         list = q.getResultList();
         Assert.assertEquals(2, list.size());
         Assert.assertEquals(sa.getArchiveId(), list.get(0).getArchiveId());
         Assert.assertEquals(sa2.getArchiveId(), list.get(1).getArchiveId());

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
   public void createNamedQueryStateArchive() throws Exception {
      log.info("start createNamedQueryStateArchive()");
      connection.setAutoCommit(false);

      try {
         Archive sa = createStateArchive();
         sa.setControlEvent(ControlEvent.INSERT);
         Thread.sleep(100);
         Archive sa2 = createStateArchive();
         sa2.setControlEvent(ControlEvent.INSERT);
         JdbcBridgeEntityManager jdbcEM = new JdbcBridgeEntityManager(connection);

         jdbcEM.persist(sa);
         jdbcEM.persist(sa2);
         connection.commit();
         Assert.assertTrue(sa.getArchiveId() != null);
         Assert.assertTrue(sa2.getArchiveId() != null);

         Query q = jdbcEM.createNamedQuery(Archive.SEL_ALL);
         List<Archive> list = q.getResultList();
         Assert.assertEquals(2, list.size());
         Assert.assertEquals(sa.getArchiveId(), list.get(0).getArchiveId());
         Assert.assertEquals(sa2.getArchiveId(), list.get(1).getArchiveId());

         q = jdbcEM.createNamedQuery(Archive.SEL_BY_PRIMARYKEYID);
         q.setParameter("c", "class");
         q.setParameter("c", 25);
         list = q.getResultList();
         Assert.assertEquals(2, list.size());
         Assert.assertEquals(sa.getArchiveId(), list.get(0).getArchiveId());
         Assert.assertEquals(sa2.getArchiveId(), list.get(1).getArchiveId());

         connection.commit();
         log.info("end createNamedQueryStateArchive()");
      } finally {
         if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
         }
      }
   }

   @Test
   public void persistBigServiceArchive() throws Exception {
      log.info("start persistBigServiceArchive()");
      connection.setAutoCommit(false);

      try {
         Archive sa = createServiceArchive();
         StringBuilder builder = new StringBuilder();
         for (int i = 0; i < 5200; i++) {
            builder.append(i);
         }

         byte[] result = builder.toString().getBytes();
         log.info("set big result object of size " + result.length);
         sa.getResource().setResult(result);

         EntityDefinition def = ArchiveDefinition.getInstance();
         def.persist(connection, sa);
         connection.commit();

         JdbcBridgeEntityManager em = new JdbcBridgeEntityManager(connection);
         Query q = em.createNamedQuery(Archive.SEL_ALL_BY_TENANT);
         q.setParameter(1, TENANT);
         Archive ar = (Archive) q.getSingleResult();
         Assert.assertEquals(result.length, ar.getResource().getResult().length);

         connection.commit();
         log.info("end persist()");
      } finally {
         if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
         }
      }
   }

}
