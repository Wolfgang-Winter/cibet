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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.cibethelper.base.CoreTestBase;
import com.cibethelper.ejb.CibetTest2EJB;
import com.cibethelper.ejb.CibetTest2MappedNameEJBImpl;
import com.cibethelper.entities.AbstractTEntity;
import com.cibethelper.entities.ITComplexEntity;
import com.cibethelper.entities.TCompareEntity;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TComplexEntity2;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.circuitbreaker.CircuitBreakerActuator;
import com.logitags.cibet.actuator.info.InfoLogActuator;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.ConfigurationService;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InitializationService;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;

@RunWith(Arquillian.class)
public class CircuitBreakerIT extends CoreTestBase {

   private static Logger log = Logger.getLogger(CircuitBreakerIT.class);

   @EJB(mappedName = "CibetTest2EJBImpl_w_m")
   private CibetTest2EJB ejb;

   @Deployment
   public static WebArchive createDeployment() {
      String warName = CircuitBreakerIT.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("it/web-noDB.xml");

      archive.addClasses(AbstractArquillian.class, CoreTestBase.class, AbstractTEntity.class, TEntity.class,
            TComplexEntity.class, TComplexEntity2.class, ITComplexEntity.class, TCompareEntity.class,
            CibetTest2EJB.class, CibetTest2MappedNameEJBImpl.class);

      File[] cibet = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-jpa").withTransitivity()
            .asFile();
      archive.addAsLibraries(cibet);

      archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

      log.debug(archive.toString(true));
      archive.as(ZipExporter.class).exportTo(new File("target/" + warName), true);

      return archive;
   }

   @Before
   public void beforeCircuitBreakerIT() {
      new ConfigurationService().initialise();
      InitializationService.instance().startContext();
      log.debug("end execute before()");
   }

   @After
   public void afterCircuitBreakerIT() {
      InitializationService.instance().endContext();
   }

   @Ignore
   @Test
   public void interceptTimeout() throws Exception {
      log.info("start interceptTimeout()");

      CircuitBreakerActuator cbreaker = (CircuitBreakerActuator) Configuration.instance()
            .getActuator(CircuitBreakerActuator.DEFAULTNAME);
      cbreaker.setTimeout(2000L);

      List<String> schemes = new ArrayList<String>();
      schemes.add(CircuitBreakerActuator.DEFAULTNAME);
      schemes.add(InfoLogActuator.DEFAULTNAME);
      registerSetpoint(CibetTest2MappedNameEJBImpl.class, schemes, "longCalculation", ControlEvent.INVOKE);

      String result = ejb.longCalculation(1200000);
      log.info("duration::: " + result);
      Assert.assertNull(result);
      EventResult er = Context.requestScope().getExecutedEventResult();
      Assert.assertEquals(ExecutionStatus.TIMEOUT, er.getExecutionStatus());
   }

   @Test
   public void interceptNoTimeout() throws Exception {
      log.info("start interceptNoTimeout()");

      CircuitBreakerActuator cbreaker = (CircuitBreakerActuator) Configuration.instance()
            .getActuator(CircuitBreakerActuator.DEFAULTNAME);
      cbreaker.setTimeout(5000L);

      List<String> schemes = new ArrayList<String>();
      schemes.add(CircuitBreakerActuator.DEFAULTNAME);
      schemes.add(InfoLogActuator.DEFAULTNAME);
      registerSetpoint(CibetTest2MappedNameEJBImpl.class, schemes, "longCalculation", ControlEvent.INVOKE);

      String result = ejb.longCalculation(1200000);
      log.info("duration::: " + result);
      Assert.assertNotNull(result);
      Assert.assertTrue(result.startsWith("DURATIONRESULT"));
      EventResult er = Context.requestScope().getExecutedEventResult();
      Assert.assertEquals(ExecutionStatus.EXECUTED, er.getExecutionStatus());
   }

}
