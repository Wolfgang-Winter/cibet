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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.cibethelper.base.CoreTestBase;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.resource.ParameterSequenceComparator;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.http.HttpRequestResource;
import com.logitags.cibet.sensor.pojo.MethodResource;

public class ConditionControlTest extends CoreTestBase {

   private static Logger log = Logger.getLogger(ConditionControlTest.class);

   @Test
   public void evaluate1() throws Exception {
      log.info("start evaluate1(): condition_test1.xml");
      initConfiguration("cibet-config.xml");

      TEntity ent = createTEntity(7, "Stingel");
      Context.sessionScope().setTenant("ten");
      Context.sessionScope().setUser("Werner");
      Context.sessionScope().setProperty("Klaus", ent);
      Context.sessionScope().setProperty("Emil", new Integer(2));

      MethodResource res = new MethodResource(ent, null, null);
      EventMetadata md = new EventMetadata(null, ControlEvent.INVOKE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints(), new ConditionControl());

      Assert.assertEquals(6, list.size());
      boolean iss = true;
      for (Setpoint s : list) {
         if (s.getId().equals("ConditionControlTest-3")) {
            iss = false;
         }
         if (s.getId().equals("ConditionControlTest-2")) {
            iss = false;
         }
      }
      Assert.assertEquals(true, iss);
      Context.end();
   }

   @Test
   public void evaluate2() throws Exception {
      Date now = new Date();
      Thread.sleep(1);
      log.info("start evaluate2(): condition_test2.xml");
      initConfiguration("cibet-config.xml");

      TEntity ent = createTEntity(7, "Sting");
      ent.setOwner("Sting");
      TEntity ent2 = createTEntity(17, "Bremse");

      Context.sessionScope().setTenant("ten");
      Context.sessionScope().setUser("Werner");
      Context.sessionScope().setProperty("Klaus", ent);
      Context.sessionScope().setProperty("Emil", new Integer(4));

      Set<ResourceParameter> paramList = new TreeSet<ResourceParameter>(new ParameterSequenceComparator());
      paramList.add(new ResourceParameter("PARAM0", int.class.getName(), 2, ParameterType.METHOD_PARAMETER, 1));
      paramList.add(new ResourceParameter("PARAM1", TEntity.class.getName(), ent2, ParameterType.METHOD_PARAMETER, 2));
      paramList.add(new ResourceParameter("PARAM2", Date.class.getName(), now, ParameterType.METHOD_PARAMETER, 3));
      paramList.add(new ResourceParameter("PARAM3", Date.class.getName(), null, ParameterType.METHOD_PARAMETER, 4));
      MethodResource res = new MethodResource(ent, String.class.getDeclaredMethod("getBytes"), paramList);
      EventMetadata md = new EventMetadata(null, ControlEvent.INVOKE, res);

      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints(), new ConditionControl());

      Assert.assertEquals(7, list.size());
      boolean iss = true;
      for (Setpoint s : list) {
         if (s.getId().equals("ConditionControlTest-1")) {
            iss = false;
         }
      }
      Assert.assertEquals(true, iss);
      Context.end();
   }

   @Test(expected = IllegalArgumentException.class)
   public void evaluateNullSetpoints() {
      log.info("start evaluateNullSetpoints()");
      Control eval = new ConditionControl();
      eval.evaluate(Collections.emptySet(), (EventMetadata) null);
   }

   @Test
   public void evaluateParams() throws IOException {
      log.info("start evaluateParams()");

      List<Setpoint> spB = new ArrayList<Setpoint>();
      Setpoint sp = new Setpoint("conditionParams");
      sp.addConditionIncludes("$HTTPATTRIBUTES.get('p1')=='Hase' " + "&& $HTTPHEADERS.get('head1') == true "
            + "&& $HTTPPARAMETERS.get('param1') == 67");
      spB.add(sp);

      HttpRequestResource res = new HttpRequestResource("targ", "POST", (HttpServletRequest) null, null);
      EventMetadata md = new EventMetadata(null, ControlEvent.INVOKE, res);
      res.addParameter(new ResourceParameter("p1", String.class.getName(), "Hase", ParameterType.HTTP_ATTRIBUTE, 1));
      res.addParameter(new ResourceParameter("head1", boolean.class.getName(), true, ParameterType.HTTP_HEADER, 2));
      res.addParameter(
            new ResourceParameter("param1", Integer.class.getName(), new Integer(67), ParameterType.HTTP_PARAMETER, 3));

      List<Setpoint> list = evaluate(md, spB, new ConditionControl());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("conditionParams", list.get(0).getId());
   }

   @Test
   public void evaluateParamsError() throws IOException {
      log.info("start evaluateParamsError()");

      List<Setpoint> spB = new ArrayList<Setpoint>();
      Setpoint sp = new Setpoint("conditionParams");
      sp.addConditionIncludes("imyInt = 5;");
      spB.add(sp);

      HttpRequestResource res = new HttpRequestResource("targ", "POST", (HttpServletRequest) null, null);
      EventMetadata md = new EventMetadata(null, ControlEvent.INVOKE, res);
      res.addParameter(new ResourceParameter("p1", String.class.getName(), "Hase", ParameterType.HTTP_ATTRIBUTE, 1));
      res.addParameter(new ResourceParameter("head1", boolean.class.getName(), true, ParameterType.HTTP_HEADER, 2));
      res.addParameter(
            new ResourceParameter("param1", Integer.class.getName(), new Integer(67), ParameterType.HTTP_PARAMETER, 3));

      try {
         Control eval = new ConditionControl();
         eval.evaluate(sp.getControls().get(ConditionControl.NAME).getIncludes(), md);
         Assert.fail();
      } catch (RuntimeException e) {
         Assert.assertTrue(e.getMessage()
               .startsWith("failed to execute Condition evaluation: condition must return a Boolean value"));
      }
   }

   @Test
   public void evaluateScriptError() {
      log.info("start evaluateScriptError()");

      List<Setpoint> spB = new ArrayList<Setpoint>();
      Setpoint sp = new Setpoint("conditionParams");
      sp.addConditionIncludes("imyInt = 5sdd");
      spB.add(sp);

      HttpRequestResource res = new HttpRequestResource("targ", "POST", (HttpServletRequest) null, null);
      EventMetadata md = new EventMetadata(null, ControlEvent.INVOKE, res);

      try {
         Control eval = new ConditionControl();
         eval.evaluate(sp.getControls().get(ConditionControl.NAME).getIncludes(), md);
         Assert.fail();
      } catch (RuntimeException e) {
         Assert.assertTrue(e.getMessage().startsWith("failed to execute Condition evaluation: Java script error"));
      }
   }

   @Test
   public void evaluateApplicationScope() throws IOException {
      log.info("start evaluateApplicationScope()");

      List<Setpoint> spB = new ArrayList<Setpoint>();
      Setpoint sp = new Setpoint("conditionParams");
      sp.addConditionIncludes("$APPLICATIONSCOPE.getProperty('p1')=='Hase' ");
      spB.add(sp);

      HttpRequestResource res = new HttpRequestResource("targ", "POST", (HttpServletRequest) null, null);
      EventMetadata md = new EventMetadata(null, ControlEvent.INVOKE, res);

      List<Setpoint> list = evaluate(md, spB, new ConditionControl());
      Assert.assertEquals(0, list.size());

      Context.applicationScope().setProperty("p1", "Hase");
      list = evaluate(md, spB, new ConditionControl());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("conditionParams", list.get(0).getId());
      Assert.assertEquals("Hase", Context.applicationScope().getProperty("p1"));
      Context.applicationScope().removeProperty("p1");
      list = evaluate(md, spB, new ConditionControl());
      Assert.assertEquals(0, list.size());
   }

   @Test
   public void evaluateRequestScope() throws IOException {
      log.info("start evaluateRequestScope()");

      List<Setpoint> spB = new ArrayList<Setpoint>();
      Setpoint sp = new Setpoint("conditionParams");
      sp.addConditionIncludes("$REQUESTSCOPE.getProperty('pxp')=='Hase' ");
      spB.add(sp);

      HttpRequestResource res = new HttpRequestResource("targ", "POST", (HttpServletRequest) null, null);
      EventMetadata md = new EventMetadata(null, ControlEvent.INVOKE, res);

      List<Setpoint> list = evaluate(md, spB, new ConditionControl());
      Assert.assertEquals(0, list.size());

      Context.requestScope().setProperty("pxp", "Hase");
      list = evaluate(md, spB, new ConditionControl());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("conditionParams", list.get(0).getId());
      Assert.assertEquals("Hase", Context.requestScope().getProperty("pxp"));
      Context.requestScope().removeProperty("pxp");
      list = evaluate(md, spB, new ConditionControl());
      Assert.assertEquals(0, list.size());
   }

   @Test
   public void resolveCondition() throws IOException {
      log.info("start resolveCondition()");
      List<Setpoint> spB = new ArrayList<Setpoint>();
      Setpoint sp = new Setpoint("conditionParams");
      sp.addConditionIncludes(
            "$REQUESTSCOPE.getProperty('psx')=='Hase' &amp;&amp; $xyParam =='ann' &amp;&amp; $ZZParam.get('dubi2')!=null;");
      spB.add(sp);

      HttpRequestResource res = new HttpRequestResource("targ", "POST", (HttpServletRequest) null, null);
      EventMetadata md = new EventMetadata(null, ControlEvent.INVOKE, res);

      Context.requestScope().setProperty("psx", "Hase");
      List<Setpoint> list = evaluate(md, spB, new ConditionControl());
      Assert.assertEquals(0, list.size());
   }

   @Test
   public void evaluateEx1() throws Exception {
      log.info("start evaluateEx1(): condition_test1.xml");
      initConfiguration("cibet-config-exclude.xml");

      TEntity ent = createTEntity(7, "Stingel");
      Context.sessionScope().setTenant("ten");
      Context.sessionScope().setUser("Werner");
      Context.sessionScope().setProperty("Klaus", ent);
      Context.sessionScope().setProperty("Emil", new Integer(2));

      MethodResource res = new MethodResource(ent, null, null);
      EventMetadata md = new EventMetadata(null, ControlEvent.INVOKE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints(), new ConditionControl());

      Assert.assertEquals(1, list.size());
      Context.end();
   }

   @Test
   public void evaluateEx2() throws Exception {
      log.info("start evaluateEx2(): condition_test1.xml");
      initConfiguration("cibet-config-exclude.xml");

      TEntity ent = createTEntity(7, "Stingel");
      Context.sessionScope().setTenant("ten");
      Context.sessionScope().setUser("Werner");
      Context.sessionScope().setProperty("Emil", new Integer(2));

      MethodResource res = new MethodResource(ent, null, null);
      EventMetadata md = new EventMetadata(null, ControlEvent.INVOKE, res);
      try {
         List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints(), new ConditionControl());
         Assert.fail();
      } catch (RuntimeException e) {
         log.warn(e.getMessage());
         Assert.assertTrue(e.getMessage().startsWith("failed to execute Condition evaluation: Java script error"));
      }
      Context.end();
   }

}
