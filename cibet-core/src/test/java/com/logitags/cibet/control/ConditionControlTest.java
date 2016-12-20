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
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.http.HttpRequestResourceHandler;
import com.logitags.cibet.sensor.pojo.MethodResourceHandler;

public class ConditionControlTest extends CoreTestBase {

   private static Logger log = Logger.getLogger(ConditionControlTest.class);

   private List<Setpoint> evaluate(EventMetadata md, List<Setpoint> spoints) {
      Control eval = new ConditionControl();
      List<Setpoint> list = new ArrayList<Setpoint>();
      for (Setpoint spi : spoints) {
         Map<String, Object> controlValues = new TreeMap<String, Object>(new ControlComparator());
         spi.getEffectiveControlValues(controlValues);
         Object value = controlValues.get("condition");
         if (value == null) {
            // list.add(spi);
         } else {
            log.debug("evaluate " + value + " on " + md);
            boolean okay = eval.evaluate(value, md);
            if (okay) {
               list.add(spi);
            }
         }
      }
      return list;
   }

   @Test
   public void evaluate1() throws Exception {
      log.info("start evaluate1(): condition_test1.xml");
      initConfiguration("cibet-config.xml");

      TEntity ent = createTEntity(7, "Stingel");
      Context.sessionScope().setTenant("ten");
      Context.sessionScope().setUser("Werner");
      Context.sessionScope().setProperty("Klaus", ent);
      Context.sessionScope().setProperty("Emil", new Integer(2));

      Resource res = new Resource(MethodResourceHandler.class, ent, null, null);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints());

      Assert.assertEquals(1, list.size());
      Assert.assertEquals("ConditionControlTest-1", list.get(0).getId());
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

      List<ResourceParameter> paramList = new LinkedList<ResourceParameter>();
      paramList.add(new ResourceParameter("PARAM0", int.class.getName(), 2, ParameterType.METHOD_PARAMETER, 1));
      paramList.add(new ResourceParameter("PARAM1", TEntity.class.getName(), ent2, ParameterType.METHOD_PARAMETER, 2));
      paramList.add(new ResourceParameter("PARAM2", Date.class.getName(), now, ParameterType.METHOD_PARAMETER, 3));
      paramList.add(new ResourceParameter("PARAM3", Date.class.getName(), null, ParameterType.METHOD_PARAMETER, 4));
      Resource res = new Resource(MethodResourceHandler.class, ent, String.class.getDeclaredMethod("getBytes"),
            paramList);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);

      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints());

      Assert.assertEquals(2, list.size());
      Assert.assertEquals("ConditionControlTest-2", list.get(0).getId());
      Assert.assertEquals("ConditionControlTest-3", list.get(1).getId());
   }

   @Test(expected = IllegalArgumentException.class)
   public void evaluateNullSetpoints() {
      log.info("start evaluateNullSetpoints()");
      Control eval = new ConditionControl();
      TEntity ent = createTEntity(7, "Sting");
      eval.evaluate(ent, (EventMetadata) null);
   }

   @Test
   public void evaluateParams() throws IOException {
      log.info("start evaluateParams()");

      List<Setpoint> spB = new ArrayList<Setpoint>();
      Setpoint sp = new Setpoint("conditionParams");
      sp.setCondition("$HTTPATTRIBUTES.get('p1')=='Hase' " + "&& $HTTPHEADERS.get('head1') == true "
            + "&& $HTTPPARAMETERS.get('param1') == 67");
      spB.add(sp);

      Resource res = new Resource(HttpRequestResourceHandler.class, "targ", "POST", null, null);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);
      res.getParameters()
            .add(new ResourceParameter("p1", String.class.getName(), "Hase", ParameterType.HTTP_ATTRIBUTE, 1));
      res.getParameters()
            .add(new ResourceParameter("head1", boolean.class.getName(), true, ParameterType.HTTP_HEADER, 2));
      res.getParameters().add(
            new ResourceParameter("param1", Integer.class.getName(), new Integer(67), ParameterType.HTTP_PARAMETER, 3));

      List<Setpoint> list = evaluate(md, spB);
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("conditionParams", list.get(0).getId());
   }

   @Test
   public void evaluateParamsError() throws IOException {
      log.info("start evaluateParamsError()");

      List<Setpoint> spB = new ArrayList<Setpoint>();
      Setpoint sp = new Setpoint("conditionParams");
      sp.setCondition("imyInt = 5;");
      spB.add(sp);

      Resource res = new Resource(HttpRequestResourceHandler.class, "targ", "POST", null, null);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);
      res.getParameters()
            .add(new ResourceParameter("p1", String.class.getName(), "Hase", ParameterType.HTTP_ATTRIBUTE, 1));
      res.getParameters()
            .add(new ResourceParameter("head1", boolean.class.getName(), true, ParameterType.HTTP_HEADER, 2));
      res.getParameters().add(
            new ResourceParameter("param1", Integer.class.getName(), new Integer(67), ParameterType.HTTP_PARAMETER, 3));

      try {
         Control eval = new ConditionControl();
         eval.evaluate(sp.getControlValue("condition"), md);
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
      sp.setCondition("imyInt = 5sdd");
      spB.add(sp);

      Resource res = new Resource(HttpRequestResourceHandler.class, "targ", "POST", null, null);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);

      try {
         Control eval = new ConditionControl();
         eval.evaluate(sp.getControlValue("condition"), md);
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
      sp.setCondition("$APPLICATIONSCOPE.getProperty('p1')=='Hase' ");
      spB.add(sp);

      Resource res = new Resource(HttpRequestResourceHandler.class, "targ", "POST", null, null);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);

      List<Setpoint> list = evaluate(md, spB);
      Assert.assertEquals(0, list.size());

      Context.applicationScope().setProperty("p1", "Hase");
      list = evaluate(md, spB);
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("conditionParams", list.get(0).getId());
      Assert.assertEquals("Hase", Context.applicationScope().getProperty("p1"));
      Context.applicationScope().removeProperty("p1");
      list = evaluate(md, spB);
      Assert.assertEquals(0, list.size());
   }

   @Test
   public void evaluateRequestScope() throws IOException {
      log.info("start evaluateRequestScope()");

      List<Setpoint> spB = new ArrayList<Setpoint>();
      Setpoint sp = new Setpoint("conditionParams");
      sp.setCondition("$REQUESTSCOPE.getProperty('pxp')=='Hase' ");
      spB.add(sp);

      Resource res = new Resource(HttpRequestResourceHandler.class, "targ", "POST", null, null);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);

      List<Setpoint> list = evaluate(md, spB);
      Assert.assertEquals(0, list.size());

      Context.requestScope().setProperty("pxp", "Hase");
      list = evaluate(md, spB);
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("conditionParams", list.get(0).getId());
      Assert.assertEquals("Hase", Context.requestScope().getProperty("pxp"));
      Context.requestScope().removeProperty("pxp");
      list = evaluate(md, spB);
      Assert.assertEquals(0, list.size());
   }

   @Test
   public void resolveCondition() throws IOException {
      log.info("start resolveCondition()");
      List<Setpoint> spB = new ArrayList<Setpoint>();
      Setpoint sp = new Setpoint("conditionParams");
      sp.setCondition(
            "$REQUESTSCOPE.getProperty('psx')=='Hase' &amp;&amp; $xyParam =='ann' &amp;&amp; $ZZParam.get('dubi2')!=null;");
      spB.add(sp);

      Resource res = new Resource(HttpRequestResourceHandler.class, "targ", "POST", null, null);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);

      Context.requestScope().setProperty("psx", "Hase");
      List<Setpoint> list = evaluate(md, spB);
      Assert.assertEquals(0, list.size());
   }

}
