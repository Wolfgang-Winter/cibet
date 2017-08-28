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
package com.logitags.cibet.control;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.cibethelper.base.CoreTestBase;
import com.cibethelper.base.Sub4EyesController;
import com.cibethelper.base.SubSub4EyesController;
import com.cibethelper.base.TrueCustomControl;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.common.Actuator;
import com.logitags.cibet.actuator.dc.FourEyesActuator;
import com.logitags.cibet.actuator.dc.SixEyesActuator;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.resource.ParameterSequenceComparator;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.jpa.JpaResource;
import com.logitags.cibet.sensor.pojo.MethodResource;

@RunWith(MockitoJUnitRunner.class)
public class ControllerImplTest extends CoreTestBase {

   private static Logger log = Logger.getLogger(ControllerImplTest.class);

   @Mock
   private EntityManager em;

   @Mock
   private EntityManager em2;

   @Before
   public void before() {
      Context.start();
   }

   @After
   public void after() {
      Context.end();
   }

   private TComplexEntity createEntity() {
      TEntity te = createTEntity(5, "Hase");
      TComplexEntity cte = new TComplexEntity();
      cte.setId(1);
      cte.setTen(te);
      cte.setCompValue(23);
      cte.setOwner("Igel");
      Mockito.when(em.find(TComplexEntity.class, 1l)).thenReturn(cte);
      Mockito.when(em2.find(TComplexEntity.class, 1l)).thenReturn(cte);
      return cte;
   }

   @Test
   public void evaluateNoMatch() throws Exception {
      log.info("start evaluateNoMatch()");
      initConfiguration("config_controller.xml");
      Context.sessionScope().setTenant(null);
      Context.requestScope().setEntityManager(em);
      Context.internalRequestScope().setApplicationEntityManager2(em2);

      TComplexEntity cte = createEntity();

      JpaResource res = new JpaResource(cte);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      Controller.evaluate(md);
      Assert.assertEquals(0, md.getActuators().size());
   }

   @Test
   public void evaluateTenantMatch() throws Exception {
      log.info("start evaluateTenantMatch()");
      Context.sessionScope().setTenant("ten1");
      initConfiguration("config_controller.xml");
      Context.requestScope().setEntityManager(em);
      Context.internalRequestScope().setApplicationEntityManager2(em2);

      TComplexEntity cte = createEntity();
      JpaResource res = new JpaResource(cte);
      EventMetadata md = new EventMetadata(ControlEvent.INSERT, res);
      Controller.evaluate(md);
      Assert.assertEquals(1, md.getActuators().size());
      Assert.assertEquals("INFOLOG", md.getActuators().get(0).getName());
   }

   @Test
   public void evaluateTenantMatch2() throws Exception {
      log.info("start evaluateTenantMatch2()");
      Context.sessionScope().setTenant("ten2|x");
      initConfiguration("config_controller.xml");
      Context.requestScope().setEntityManager(em);
      Context.internalRequestScope().setApplicationEntityManager2(em2);

      TComplexEntity cte = createEntity();
      JpaResource res = new JpaResource(cte);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      Controller.evaluate(md);
      Assert.assertEquals(1, md.getActuators().size());
      ArchiveActuator act = (ArchiveActuator) md.getActuators().get(0);
      Assert.assertEquals("ARCH2", act.getName());
      Assert.assertEquals("Value of prop1-A3", act.getJndiName());
      log.debug(md);
   }

   @Test
   public void evaluateStateChange() throws Exception {
      log.info("start evaluateStateChange()");
      initConfiguration("config_controller.xml");
      Context.sessionScope().setTenant("ten1");

      TEntity te = createTEntity(5, "Hase");
      TComplexEntity cte = new TComplexEntity();
      cte.setId(1);
      cte.setTen(te);
      cte.setCompValue(23);
      cte.setOwner("Hase");

      createEntity();
      log.debug("xxx");
      JpaResource res = new JpaResource(cte);
      EventMetadata md = new EventMetadata(ControlEvent.INSERT, res);
      Controller.evaluate(md);
      Assert.assertEquals(1, md.getActuators().size());
      Assert.assertEquals("INFOLOG", md.getActuators().get(0).getName());
   }

   @Test
   public void evaluateStateChangeTenant() throws Exception {
      log.info("start evaluateStateChangeTenant()");
      Context.sessionScope().setTenant("ten2|x|cc");
      initConfiguration("config_controller.xml");
      Context.requestScope().setEntityManager(em);
      Context.internalRequestScope().setApplicationEntityManager2(em2);

      TEntity te = createTEntity(5, "Hase");
      TComplexEntity cte = new TComplexEntity();
      cte.setId(1);
      cte.setTen(te);
      cte.setCompValue(23);
      cte.setOwner("Igel");

      createEntity();

      JpaResource res = new JpaResource(cte);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      Controller.evaluate(md);
      Assert.assertEquals(1, md.getActuators().size());
      ArchiveActuator act = (ArchiveActuator) md.getActuators().get(0);
      Assert.assertEquals("ARCH2", act.getName());
      Assert.assertEquals("Value of prop1-A3", act.getJndiName());
   }

   @Test
   public void evaluateTenantTarget() throws Exception {
      log.info("start evaluateTenantTarget()");
      initConfiguration("cibet-config.xml");
      Context.sessionScope().setTenant("ApiTest-loadXMLConfiguration-Owner");

      JpaResource res = new JpaResource(new TEntity());
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      Controller.evaluate(md);
      Assert.assertTrue(md.getActuators().size() == 2);
      Iterator<Actuator> it = md.getActuators().iterator();
      Assert.assertTrue("FOUR_EYES".equals(it.next().getName()));
      Assert.assertTrue("ARCHIVE".equals(it.next().getName()));
   }

   @Test
   public void evaluateTarget() throws Exception {
      log.info("start evaluateTarget()");
      initConfiguration("cibet-config.xml");
      Context.sessionScope().setTenant("ApiTest-loadXMLConfiguration-Owner");

      JpaResource res = new JpaResource(new TComplexEntity());
      EventMetadata md = new EventMetadata(ControlEvent.INSERT, res);
      Controller.evaluate(md);
      Assert.assertTrue(md.getActuators().size() == 1);
      Iterator<Actuator> it = md.getActuators().iterator();
      Assert.assertTrue("ARCHIVE".equals(it.next().getName()));
   }

   @Test
   public void evaluateMethod() throws Exception {
      log.info("start evaluateMethod()");
      initConfiguration("cibet-config.xml");
      Context.sessionScope().setTenant("ApiTest-loadXMLConfiguration-Owner");

      Set<ResourceParameter> paramList = new TreeSet<ResourceParameter>(new ParameterSequenceComparator());
      paramList.add(new ResourceParameter("PARAM_0", String.class.getName(), "XYZ", ParameterType.METHOD_PARAMETER, 1));
      Method m = TrueCustomControl.class.getDeclaredMethod("setGaga", new Class[] { String.class });
      MethodResource res = new MethodResource(new TrueCustomControl(), m, paramList);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);
      Controller.evaluate(md);
      Assert.assertTrue(md.getActuators().size() == 1);
      Iterator<Actuator> it = md.getActuators().iterator();
      Assert.assertTrue("ARCHIVE".equals(it.next().getName()));
   }

   @Test
   public void evaluateMethod2() throws Exception {
      log.info("start evaluateMethod2()");
      initConfiguration("cibet-config.xml");
      Context.sessionScope().setTenant("ApiTest-loadXMLConfiguration-Owner");

      Method m = TrueCustomControl.class.getDeclaredMethod("getName");
      MethodResource res = new MethodResource(new TrueCustomControl(), m, null);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);
      Controller.evaluate(md);
      log.debug("** md:" + md);

      Assert.assertEquals(1, md.getActuators().size());
      Iterator<Actuator> it = md.getActuators().iterator();
      Actuator act = it.next();
      Assert.assertTrue("ARCHIVE2".equals(act.getName()));
      ArchiveActuator aract = (ArchiveActuator) act;
      Assert.assertEquals("CibetEntityManager.jndiName", aract.getJndiName());
   }

   @Test
   public void evaluateRegisteredSetpoint() throws Exception {
      log.info("start evaluateRegisteredSetpoint()");
      initConfiguration("cibet-config.xml");
      Context.sessionScope().setTenant("c");

      Setpoint sb = new Setpoint("Test-G");
      sb.addTargetIncludes(TEntity.class.getName());
      sb.addEventIncludes(ControlEvent.INSERT);
      sb.addActuator(Configuration.instance().getActuator(ArchiveActuator.DEFAULTNAME));
      sb.addActuator(Configuration.instance().getActuator(SixEyesActuator.DEFAULTNAME));
      Configuration.instance().registerSetpoint(sb);

      JpaResource res = new JpaResource(new TEntity());
      EventMetadata md = new EventMetadata(ControlEvent.INSERT, res);
      Controller.evaluate(md);

      Assert.assertEquals(2, md.getActuators().size());
      Iterator<Actuator> it = md.getActuators().iterator();
      Assert.assertTrue("ARCHIVE".equals(it.next().getName()));
      Assert.assertTrue("SIX_EYES".equals(it.next().getName()));
   }

   @Test
   public void evaluateRegisteredSetpointActuator() throws Exception {
      log.info("start evaluateRegisteredSetpointActuator");
      initConfiguration("cibet-config.xml");
      Context.sessionScope().setTenant("xx");

      Configuration.instance().registerActuator(new Sub4EyesController());

      Setpoint sb = new Setpoint("Test-H");
      sb.addTargetIncludes(TEntity.class.getName());
      sb.addEventIncludes(ControlEvent.INSERT);
      sb.addActuator(Configuration.instance().getActuator("Sub4EyesController"));
      sb.addActuator(Configuration.instance().getActuator(SixEyesActuator.DEFAULTNAME));
      Configuration.instance().registerSetpoint(sb);

      JpaResource res = new JpaResource(new TEntity());
      EventMetadata md = new EventMetadata(ControlEvent.INSERT, res);
      Controller.evaluate(md);
      Assert.assertEquals(2, md.getActuators().size());
      Iterator<Actuator> it = md.getActuators().iterator();
      Assert.assertEquals("Sub4EyesController", it.next().getName());
      Assert.assertTrue("SIX_EYES".equals(it.next().getName()));
   }

   @Test
   public void evaluateRegisteredSetpointActuator2() throws Exception {
      log.info("start evaluateRegisteredSetpointActuator");
      initConfiguration("cibet-config.xml");
      Context.sessionScope().setTenant("xxy");

      Configuration.instance().registerActuator(new Sub4EyesController());
      Configuration.instance().registerActuator(new SubSub4EyesController("Willi"));

      Setpoint sb = new Setpoint("Test-I");
      sb.addTargetIncludes(TEntity.class.getName());
      sb.addEventIncludes(ControlEvent.INSERT);
      sb.addActuator(Configuration.instance().getActuator("Sub4EyesController"));
      sb.addActuator(Configuration.instance().getActuator("Willi"));
      Configuration.instance().registerSetpoint(sb);

      JpaResource res = new JpaResource(new TEntity());
      EventMetadata md = new EventMetadata(ControlEvent.INSERT, res);
      Controller.evaluate(md);

      Assert.assertEquals(2, md.getActuators().size());
      Iterator<Actuator> it = md.getActuators().iterator();
      Assert.assertTrue("Sub4EyesController".equals(it.next().getName()));
      Assert.assertTrue("Willi".equals(it.next().getName()));
   }

   @Test
   public void configureSub4Eyes2() throws Exception {
      log.info("start ");
      initConfiguration("cibet-config.xml");
      Context.sessionScope().setTenant("xxc");
      Configuration cman = Configuration.instance();
      cman.registerActuator(new Sub4EyesController());
      cman.registerActuator(new SubSub4EyesController("Willi"));

      Setpoint sb = new Setpoint("Test-J");
      sb.addTargetIncludes(TEntity.class.getName());
      sb.addEventIncludes(ControlEvent.INSERT);
      sb.addActuator(cman.getActuator("Sub4EyesController"));
      sb.addActuator(cman.getActuator("Willi"));
      sb.addActuator(cman.getActuator("Willi"));
      sb.addActuator(cman.getActuator(FourEyesActuator.DEFAULTNAME));
      sb.addActuator(cman.getActuator("Willi"));
      cman.registerSetpoint(sb);

      JpaResource res = new JpaResource(new TEntity());
      EventMetadata md = new EventMetadata(ControlEvent.INSERT, res);
      Controller.evaluate(md);

      Assert.assertEquals(3, md.getActuators().size());
      Iterator<Actuator> it = md.getActuators().iterator();
      Assert.assertTrue("Sub4EyesController".equals(it.next().getName()));
      Assert.assertTrue("Willi".equals(it.next().getName()));
      Assert.assertTrue(FourEyesActuator.DEFAULTNAME.equals(it.next().getName()));
   }

   @Test
   public void evaluateWithParents() throws Exception {
      log.info("start evaluateWithParents()");
      Context.sessionScope().setTenant("ten");
      initConfiguration("config_parents.xml");

      Method m = ControllerImplTest.class.getDeclaredMethod("evaluateWithParents");
      MethodResource res = new MethodResource(this, m, null);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);
      Controller.evaluate(md);

      Assert.assertEquals(3, md.getSetpoints().size());
      Assert.assertEquals("A", md.getSetpoints().get(0).getId());
      Assert.assertEquals("B1", md.getSetpoints().get(1).getId());
      Assert.assertEquals("B2", md.getSetpoints().get(2).getId());
   }

   @Test(expected = IllegalArgumentException.class)
   public void evaluateNullEventMetadata() {
      log.info("start evaluateNullEventMetadata()");
      Controller.evaluate((EventMetadata) null);
   }

}
