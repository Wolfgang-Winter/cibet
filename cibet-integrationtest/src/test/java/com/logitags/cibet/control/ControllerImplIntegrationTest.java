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
/**
 * 
 */
package com.logitags.cibet.control;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cibethelper.base.CoreTestBase;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InitializationService;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.sensor.jpa.JpaResourceHandler;

import junit.framework.Assert;

/**
 * -javaagent:${project_loc}\..\cibet-material\technics\aspectjweaver-1.6.9.jar
 */
public class ControllerImplIntegrationTest extends CoreTestBase {

   private static EntityManager applEman;
   private static EntityManagerFactory fac;

   @BeforeClass
   public static void beforeClass() throws Exception {
      fac = Persistence.createEntityManagerFactory("localTest");
      applEman = fac.createEntityManager();
   }

   @Before
   public void doBefore() throws Exception {
      log.debug("CibetContextAspectTest.doBefore");
      applEman.getTransaction().begin();
   }

   /**
    * logger for tracing
    */
   private static Logger log = Logger.getLogger(ControllerImplIntegrationTest.class);

   @After
   public void delete() {
      log.debug("after test: delete");
      applEman.getTransaction().rollback();

      applEman.getTransaction().begin();

      Context.internalRequestScope().getEntityManager().flush();
      Query q = applEman.createNamedQuery(TComplexEntity.SEL_ALL);
      List<TComplexEntity> l = q.getResultList();
      for (TComplexEntity tComplexEntity : l) {
         applEman.remove(tComplexEntity);
      }

      Query q2 = applEman.createNamedQuery(TEntity.DEL_ALL);
      q2.executeUpdate();

      Query q3 = Context.internalRequestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL_BY_CLASS);
      q3.setParameter("tenant", "ten%");
      q3.setParameter("targetType", "com.logitags.cibet.helper.TComplexEntity");
      List<Archive> alist = q3.getResultList();
      for (Archive ar : alist) {
         applEman.remove(ar);
      }

      applEman.getTransaction().commit();
      InitializationService.instance().endContext();
   }

   @Test
   public void evaluateNoMatch() throws Exception {
      log.info("start evaluateNoMatch()");
      initConfiguration("config_controller.xml");
      InitializationService.instance().startContext();

      TEntity te = createTEntity(5, "Hase");
      TComplexEntity cte = new TComplexEntity();
      cte.setTen(te);
      cte.setCompValue(23);
      cte.setOwner("Igel");

      Resource res = new Resource(JpaResourceHandler.class, cte);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      Controller ev = new DefaultController();
      ev.evaluate(md);
      Assert.assertEquals(0, md.getActuators().size());
   }

   @Test
   public void evaluateInsert() throws Exception {
      log.info("start evaluateInsert()");
      initConfiguration("config_controller.xml");
      InitializationService.instance().startContext();

      TComplexEntity cte = persistTComplexEntity();

      Resource res = new Resource(JpaResourceHandler.class, cte);
      EventMetadata md = new EventMetadata(ControlEvent.INSERT, res);
      Controller ev = new DefaultController();
      ev.evaluate(md);
      Assert.assertEquals(1, md.getActuators().size());
      Assert.assertEquals("INFOLOG", md.getActuators().get(0).getName());
   }

   @Test
   public void evaluateUpdateTenant() throws Exception {
      log.info("start evaluateUpdateTenant()");
      initConfiguration("config_controller.xml");
      InitializationService.instance().startContext();

      if (Context.internalRequestScope().getApplicationEntityManager2() == null) {
         Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());
      }
      TComplexEntity cte = persistTComplexEntity();
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      Context.sessionScope().setTenant("ten2|x");
      Resource res = new Resource(JpaResourceHandler.class, cte);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      Controller ev = new DefaultController();
      ev.evaluate(md);
      Assert.assertEquals(1, md.getActuators().size());
      ArchiveActuator act = (ArchiveActuator) md.getActuators().get(0);
      Assert.assertEquals("ARCH2", act.getName());
      Assert.assertEquals("Value of prop1-A3", act.getJndiName());
   }

   @Test
   public void evaluateInsertStateChange() throws Exception {
      log.info("start evaluateInsertStateChange()");
      initConfiguration("config_controller.xml");
      InitializationService.instance().startContext();

      TComplexEntity cte = persistTComplexEntity();

      TComplexEntity cte2 = applEman.find(TComplexEntity.class, cte.getId());
      cte2.setOwner("Hase");

      Resource res = new Resource(JpaResourceHandler.class, cte2);
      EventMetadata md = new EventMetadata(ControlEvent.INSERT, res);
      Controller ev = new DefaultController();
      ev.evaluate(md);
      Assert.assertEquals(1, md.getActuators().size());
      Assert.assertEquals("INFOLOG", md.getActuators().get(0).getName());
   }

   @Test
   public void evaluatorImpl1() throws Exception {
      log.info("start evaluatorImpl1()");
      initConfiguration("config_controller.xml");
      InitializationService.instance().startContext();

      if (Context.internalRequestScope().getApplicationEntityManager2() == null) {
         Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());
      }
      TComplexEntity cte = persistTComplexEntity();
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      TComplexEntity cte2 = applEman.find(TComplexEntity.class, cte.getId());
      cte2.setOwner("Hase");

      Context.sessionScope().setTenant("ten2|x|cc");
      Resource res = new Resource(JpaResourceHandler.class, cte2);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      Controller ev = new DefaultController();
      ev.evaluate(md);
      Assert.assertEquals(1, md.getActuators().size());
      ArchiveActuator act = (ArchiveActuator) md.getActuators().get(0);
      Assert.assertEquals("ARCH2", act.getName());
      Assert.assertEquals("Value of prop1-A3", act.getJndiName());
      log.debug(md);
   }

   private TComplexEntity persistTComplexEntity() {
      TEntity te = createTEntity(5, "Hase");
      TComplexEntity cte = new TComplexEntity();
      cte.setTen(te);
      cte.setCompValue(23);
      cte.setOwner("Igel");

      Context.sessionScope().setTenant("ten1");
      applEman.persist(cte);
      applEman.flush();
      applEman.clear();
      return cte;
   }

   @Test
   public void customControl() throws Exception {
      log.debug("start customControl()");
      initConfiguration("config_customControl.xml");
      InitializationService.instance().startContext();

      TComplexEntity cte = new TComplexEntity();
      cte.setOwner("new1");

      EventResult evi = Context.requestScope().getExecutedEventResult();
      log.debug(evi);
      Assert.assertEquals("INFOLOG", evi.getActuators());
      Assert.assertEquals("K2, K3", evi.getSetpoints());
   }

}
