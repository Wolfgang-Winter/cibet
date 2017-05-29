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
package com.logitags.cibet.it;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;

import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.cibethelper.base.CoreTestBase;
import com.cibethelper.ejb.CibetTestEJB;
import com.cibethelper.ejb.RemoteEJB;
import com.cibethelper.ejb.RemoteEJBImpl;
import com.cibethelper.ejb.SimpleEjb;
import com.cibethelper.entities.AbstractTEntity;
import com.cibethelper.entities.ITComplexEntity;
import com.cibethelper.entities.TCompareEntity;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TComplexEntity2;
import com.cibethelper.entities.TEntity;
import com.cibethelper.servlet.ArquillianTestServlet1;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.actuator.dc.FourEyesActuator;
import com.logitags.cibet.actuator.info.InfoLogActuator;
import com.logitags.cibet.actuator.info.TrackerActuator;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.ConfigurationService;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.sensor.jpa.JpaResource;
import com.logitags.cibet.sensor.pojo.MethodResource;

/**
 * add -javaagent:${project_loc}\..\cibet-material\technics\aspectjweaver-1.6.9. jar to java command
 */
@RunWith(Arquillian.class)
public class ChainedActuatorIT extends AbstractArquillian {

   private static Logger log = Logger.getLogger(ChainedActuatorIT.class);

   @EJB
   private CibetTestEJB ejb;

   private Setpoint sp = null;

   @Deployment
   public static WebArchive createDeployment() {
      String warName = ChainedActuatorIT.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("it/web2.xml");

      archive.addClasses(AbstractArquillian.class, CoreTestBase.class, AbstractTEntity.class, TEntity.class,
            TComplexEntity.class, TComplexEntity2.class, ITComplexEntity.class, TCompareEntity.class,
            CibetTestEJB.class, ArquillianTestServlet1.class, RemoteEJB.class, RemoteEJBImpl.class, SimpleEjb.class);

      File[] cibet = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-jpa").withTransitivity()
            .asFile();
      archive.addAsLibraries(cibet);

      archive.addAsWebInfResource("META-INF/persistence-it.xml", "classes/META-INF/persistence.xml");
      archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

      log.debug(archive.toString(true));
      archive.as(ZipExporter.class).exportTo(new File("target/" + warName), true);

      return archive;
   }

   @Before
   public void beforeChainedActuatorIT() {
      log.debug("execute before()");
      new ConfigurationService().initialise();
      Context.start();
      Context.sessionScope().setUser(USER);
      Context.sessionScope().setTenant(TENANT);
      log.debug("end execute before()");
   }

   @After
   public void afterChainedActuatorIT() {
      Context.end();
      if (sp != null) {
         Configuration.instance().unregisterSetpoint(sp.getId());
      }
   }

   @Test
   public void chainArchive() throws Exception {
      log.info("start chainArchive()");
      log.debug("EVRESZLT: " + Context.requestScope().getExecutedEventResult());

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(InfoLogActuator.DEFAULTNAME);
      sp = registerSetpoint(CibetTestEJB.class, schemes, "insertTComplexEntity", ControlEvent.INVOKE,
            ControlEvent.RELEASE_INVOKE);
      Thread.sleep(50);
      Setpoint sp2 = registerSetpoint(TComplexEntity.class, schemes, "setStatValue", ControlEvent.INVOKE,
            ControlEvent.RELEASE_INVOKE);

      log.debug("EVRESZLT: " + Context.requestScope().getExecutedEventResult());
      List<EventResult> list = ejb.insertTComplexEntity();
      log.debug("EVRESZLT: " + Context.requestScope().getExecutedEventResult());
      Assert.assertEquals(7, list.size());
      Assert.assertNull(list.get(0));
      Assert.assertTrue(list.get(1).getResource().indexOf("setOwner") > 0);
      Assert.assertTrue(list.get(2).getResource().indexOf("addLazyList") > 0);
      Assert.assertTrue(list.get(3).getResource().indexOf("setTen") > 0);
      Assert.assertTrue(list.get(4).getResource().indexOf("setStatValue") > 0);
      Assert.assertEquals(ControlEvent.INSERT, list.get(5).getEvent());
      Assert.assertTrue(list.get(6).getResource().indexOf("getId") > 0);

      for (int i = 1; i < 7; i++) {
         log.debug(list.get(i));
         Assert.assertEquals(ExecutionStatus.EXECUTED, list.get(i).getExecutionStatus());
         if (i == 5) {
            Assert.assertEquals("JPA", list.get(i).getSensor());
            // if (database.getName().startsWith("JDBC-")) {
            // // JDBC calls all the getId() getter of TComplexEntity
            // // which is under control
            // Assert.assertEquals(3, list.get(i).getChildResults().size());
            //
            // } else {
            Assert.assertEquals(0, list.get(i).getChildResults().size());
            // }
         } else {
            Assert.assertEquals("ASPECT", list.get(i).getSensor());
            Assert.assertEquals(0, list.get(i).getChildResults().size());
         }
         Assert.assertTrue(list.get(i).getParentResult().getResource().indexOf("insertTComplexEntity") > 0);
         Assert.assertEquals("EJB", list.get(i).getParentResult().getSensor());
      }

      EventResult firstResult = Context.requestScope().getExecutedEventResult();
      log.debug("FirstExecutedEventResult: " + firstResult);
      Assert.assertTrue(firstResult.getResource().indexOf("insertTComplexEntity") > 0);
      Assert.assertEquals(ExecutionStatus.EXECUTED, firstResult.getExecutionStatus());

      List<EventResult> childs = Context.requestScope().getExecutedEventResult().getChildResults();
      Assert.assertEquals(6, childs.size());
      Assert.assertTrue(childs.get(0).getResource().indexOf("setOwner") > 0);
      Assert.assertTrue(childs.get(1).getResource().indexOf("addLazyList") > 0);
      Assert.assertTrue(childs.get(2).getResource().indexOf("setTen") > 0);
      Assert.assertTrue(childs.get(3).getResource().indexOf("setStatValue") > 0);
      // Assert.assertTrue(childs.get(4).getResource().indexOf("toString") > 0);

      log.debug(childs.get(4).getResource());
      Assert.assertTrue(childs.get(4).getResource().startsWith("[JpaResource] "));
      Assert.assertTrue(childs.get(4).getResource().indexOf("; primaryKeyId: 0") > 0);
      Assert.assertTrue(childs.get(4).getResource().indexOf("targetType: com.cibethelper.entities.TComplexEntity") > 0);

      Assert.assertEquals(ControlEvent.INSERT, childs.get(4).getEvent());
      Assert.assertTrue(childs.get(5).getResource().indexOf("getId") > 0);
      for (EventResult child : childs) {
         Assert.assertEquals(ExecutionStatus.EXECUTED, child.getExecutionStatus());
      }

      // check
      List<Archive> list1 = ejb.queryArchiveByTenant();
      Assert.assertEquals(2, list1.size());
      Archive ar = list1.get(0);
      log.debug(ar);
      MethodResource res = (MethodResource) ar.getResource();
      Assert.assertEquals(TComplexEntity.class.getName(), res.getTargetType());
      Assert.assertEquals(ControlEvent.INVOKE, ar.getControlEvent());
      Assert.assertEquals("setStatValue", res.getMethod());

      Archive ar2 = list1.get(1);
      MethodResource res2 = (MethodResource) ar2.getResource();
      log.debug(ar2);
      Assert.assertEquals(CibetTestEJB.class.getName(), res2.getTargetType());
      Assert.assertEquals(ControlEvent.INVOKE, ar2.getControlEvent());
      Assert.assertEquals("insertTComplexEntity", res2.getMethod());

      Configuration.instance().unregisterSetpoint(sp2.getId());
   }

   @Test
   public void chainArchiveEyes() throws Exception {
      log.info("start chainArchiveEyes()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(InfoLogActuator.DEFAULTNAME);
      schemes.add(TrackerActuator.DEFAULTNAME);
      sp = registerSetpoint(CibetTestEJB.class, schemes, "insertTComplexEntity", ControlEvent.INVOKE,
            ControlEvent.RELEASE_INVOKE);
      Thread.sleep(50);
      List<String> schemes3 = new ArrayList<String>();
      schemes3.add(ArchiveActuator.DEFAULTNAME);
      Setpoint sp2 = registerSetpoint(TComplexEntity.class, schemes3, "setStatValue", ControlEvent.INVOKE,
            ControlEvent.RELEASE_INVOKE);

      Thread.sleep(50);
      List<String> schemes2 = new ArrayList<String>();
      schemes2.add(ArchiveActuator.DEFAULTNAME);
      schemes2.add(FourEyesActuator.DEFAULTNAME);
      // schemes2.add(TrackerActuator.DEFAULTNAME);
      Setpoint sp3 = registerSetpoint(TComplexEntity.class.getName(), schemes2, ControlEvent.INSERT,
            ControlEvent.RELEASE_INVOKE);

      ejb.insertTComplexEntity();

      EventResult firstResult = Context.requestScope().getExecutedEventResult();
      log.debug("FirstExecutedEventResult: " + firstResult);
      Assert.assertTrue(firstResult.getResource().indexOf("insertTComplexEntity") > 0);
      Assert.assertEquals(ExecutionStatus.EXECUTED, firstResult.getExecutionStatus());

      List<EventResult> childs = Context.requestScope().getExecutedEventResult().getChildResults();
      Assert.assertEquals(6, childs.size());
      Assert.assertTrue(childs.get(0).getResource().indexOf("setOwner") > 0);
      Assert.assertTrue(childs.get(1).getResource().indexOf("addLazyList") > 0);
      Assert.assertTrue(childs.get(2).getResource().indexOf("setTen") > 0);
      Assert.assertTrue(childs.get(3).getResource().indexOf("setStatValue") > 0);
      // Assert.assertTrue(childs.get(4).getResource().indexOf("toString") > 0);

      log.debug("childs.get(5).getResource(): " + childs.get(4).getResource());

      log.debug(childs.get(4).getResource());
      Assert.assertTrue(childs.get(4).getResource().startsWith("[JpaResource] "));
      Assert.assertTrue(childs.get(4).getResource().indexOf("; primaryKeyId: 0") > 0);
      Assert.assertTrue(childs.get(4).getResource().indexOf("targetType: com.cibethelper.entities.TComplexEntity") > 0);

      Assert.assertEquals(ControlEvent.INSERT, childs.get(4).getEvent());
      Assert.assertTrue(childs.get(5).getResource().indexOf("getId") > 0);
      for (EventResult child : childs) {
         if (child.getSensor().equals("JPA")) {
            Assert.assertEquals(ExecutionStatus.POSTPONED, child.getExecutionStatus());

         } else {
            Assert.assertEquals(ExecutionStatus.EXECUTED, child.getExecutionStatus());
         }
      }

      List<EventResult> elist = ejb.selectEventResults();
      Assert.assertEquals(1, elist.size());
      EventResult er = elist.get(0);

      Assert.assertTrue(er.getResource().indexOf("insertTComplexEntity") > 0);
      childs = er.getChildResults();
      Assert.assertEquals(6, childs.size());
      Assert.assertTrue(childs.get(0).getResource().indexOf("setOwner") > 0);
      Assert.assertTrue(childs.get(1).getResource().indexOf("addLazyList") > 0);
      Assert.assertTrue(childs.get(2).getResource().indexOf("setTen") > 0);
      Assert.assertTrue(childs.get(3).getResource().indexOf("setStatValue") > 0);
      // Assert.assertTrue(childs.get(4).getResource().indexOf("toString") > 0);

      log.debug(childs.get(4).getResource());
      Assert.assertTrue(childs.get(4).getResource().startsWith("[JpaResource] "));
      Assert.assertTrue(childs.get(4).getResource().indexOf("; primaryKeyId: 0") > 0);
      Assert.assertTrue(childs.get(4).getResource().indexOf("targetType: com.cibethelper.entities.TComplexEntity") > 0);

      Assert.assertEquals(ControlEvent.INSERT, childs.get(4).getEvent());
      Assert.assertTrue(childs.get(5).getResource().indexOf("getId") > 0);
      for (EventResult child : childs) {
         if (child.getSensor().equals("JPA")) {
            Assert.assertEquals(ExecutionStatus.POSTPONED, child.getExecutionStatus());

         } else {
            Assert.assertEquals(ExecutionStatus.EXECUTED, child.getExecutionStatus());
         }
      }

      // check
      List<Archive> list1 = ejb.queryArchiveByTenant();
      Assert.assertEquals(3, list1.size());
      Archive ar = list1.get(0);
      log.debug(ar);
      MethodResource res = (MethodResource) ar.getResource();
      Assert.assertEquals(TComplexEntity.class.getName(), res.getTargetType());
      Assert.assertEquals(ControlEvent.INVOKE, ar.getControlEvent());
      Assert.assertEquals("setStatValue", res.getMethod());

      Archive ar2 = list1.get(1);
      log.debug(ar2);
      JpaResource res2 = (JpaResource) ar2.getResource();
      Assert.assertEquals(TComplexEntity.class.getName(), res2.getTargetType());
      Assert.assertEquals(ControlEvent.INSERT, ar2.getControlEvent());

      Archive ar3 = list1.get(2);
      log.debug(ar3);
      MethodResource res3 = (MethodResource) ar3.getResource();
      Assert.assertEquals(CibetTestEJB.class.getName(), res3.getTargetType());
      Assert.assertEquals(ControlEvent.INVOKE, ar3.getControlEvent());
      Assert.assertEquals("insertTComplexEntity", res3.getMethod());

      List<Controllable> dcs = ejb.queryControllable();
      Assert.assertEquals(1, dcs.size());

      Configuration.instance().unregisterSetpoint(sp2.getId());
      Configuration.instance().unregisterSetpoint(sp3.getId());
   }

}
