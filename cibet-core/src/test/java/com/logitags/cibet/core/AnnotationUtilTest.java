package com.logitags.cibet.core;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import com.cibethelper.base.CoreTestBase;
import com.cibethelper.base.Sub4EyesController;
import com.cibethelper.base.SubSub4EyesController;
import com.logitags.cibet.config.ConfigurationTest;

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

public class AnnotationUtilTest {

   private static Logger log = Logger.getLogger(AnnotationUtilTest.class);

   @Test
   public void typeFromAnnotation() {
      log.debug("start AnnotationUtilTest");
      Class<?> a = AnnotationUtil.typeFromAnnotation(Sub4EyesController.class, XmlValue.class);
      Assert.assertTrue(a == long.class);

      Class<?> b = AnnotationUtil.typeFromAnnotation(Sub4EyesController.class, XmlTransient.class);
      Assert.assertTrue(b == boolean.class);

      Class<?> b1 = AnnotationUtil.typeFromAnnotation(CoreTestBase.class, AfterClass.class);
      Assert.assertTrue(b1 == void.class);

      try {
         AnnotationUtil.typeFromAnnotation(Sub4EyesController.class, XmlSchemaType.class);
         Assert.fail();
      } catch (AnnotationNotFoundException e) {
         // okay, annotation is not present
      }
   }

   @Test
   public void valueFromAnnotation() {
      Sub4EyesController contr = new Sub4EyesController();
      Object o1 = AnnotationUtil.valueFromAnnotation(contr, XmlValue.class);
      Assert.assertTrue((Long) o1 == 43);

      Object o2 = AnnotationUtil.valueFromAnnotation(contr, XmlTransient.class);
      Assert.assertTrue((Boolean) o2 == true);

      ConfigurationTest test = new ConfigurationTest();
      Object o4 = AnnotationUtil.valueFromAnnotation(test, AfterClass.class);
      Assert.assertTrue(o4 == null);

      try {
         AnnotationUtil.valueFromAnnotation(contr, XmlSchemaType.class);
         Assert.fail();
      } catch (AnnotationNotFoundException e) {
         // okay, annotation is not present
      }
   }

   @Test
   public void setValueFromAnnotation() {
      Sub4EyesController co = new Sub4EyesController();
      AnnotationUtil.setValueFromAnnotation(co, XmlValue.class, 5);
      Assert.assertTrue(co.getDummy1() == 5);

      AnnotationUtil.setValueFromAnnotation(co, XmlElement.class, 33);
      Assert.assertTrue(co.getDummy1() == 33);

      try {
         AnnotationUtil.setValueFromAnnotation(co, XmlValue.class, null);
         Assert.fail();
      } catch (IllegalArgumentException e) {
         // okay
      }

      AnnotationUtil.setValueFromAnnotation(co, XmlTransient.class, true);
      Assert.assertTrue(co.isDummy2() == true);
      AnnotationUtil.setValueFromAnnotation(co, XmlTransient.class, false);
      Assert.assertTrue(co.isDummy2() == false);

   }

   @Test
   public void setValueFromAnnotationSub() {
      SubSub4EyesController co = new SubSub4EyesController();
      AnnotationUtil.setValueFromAnnotation(co, XmlValue.class, 5);
      Assert.assertTrue(co.getDummy1() == 5);

      AnnotationUtil.setValueFromAnnotation(co, XmlElement.class, 33);
      Assert.assertTrue(co.getDummy1() == 33);

      try {
         AnnotationUtil.setValueFromAnnotation(co, XmlValue.class, null);
         Assert.fail();
      } catch (IllegalArgumentException e) {
         // okay
      }

      AnnotationUtil.setValueFromAnnotation(co, XmlTransient.class, true);
      Assert.assertTrue(co.isDummy2() == true);
      AnnotationUtil.setValueFromAnnotation(co, XmlTransient.class, false);
      Assert.assertTrue(co.isDummy2() == false);

      AnnotationUtil.setValueFromAnnotation(co, XmlAnyElement.class, 55);
      Assert.assertTrue(co.getDummy1() == 55);
   }

   @Test
   public void isAnnotationPresent() throws Exception {
      boolean flag = AnnotationUtil.isAnnotationPresent(Sub4EyesController.class, XmlValue.class);
      Assert.assertTrue(flag);

      flag = AnnotationUtil.isAnnotationPresent(SubSub4EyesController.class, XmlValue.class);
      Assert.assertTrue(flag);

      flag = AnnotationUtil.isAnnotationPresent(SubSub4EyesController.class, XmlElement.class);
      Assert.assertTrue(flag);
   }

   @Test
   public void isAnnotationPresentField() throws Exception {
      boolean flag = AnnotationUtil.isAnnotationPresent(Sub4EyesController.class,
            Sub4EyesController.class.getDeclaredField("dummy1"), XmlValue.class);
      Assert.assertTrue(flag);

      try {
         flag = AnnotationUtil.isAnnotationPresent(Sub4EyesController.class,
               Sub4EyesController.class.getDeclaredField("noSuchField"), XmlValue.class);
         Assert.fail();
      } catch (Exception e1) {
         Assert.assertTrue(e1 instanceof NoSuchFieldException);
      }

      flag = AnnotationUtil.isAnnotationPresent(Sub4EyesController.class,
            Sub4EyesController.class.getDeclaredField("dummy2"), XmlTransient.class);
      Assert.assertTrue(flag);

      flag = AnnotationUtil.isAnnotationPresent(Sub4EyesController.class,
            Sub4EyesController.class.getDeclaredField("dummy2"), XmlValue.class);
      Assert.assertTrue(!flag);

      flag = AnnotationUtil.isAnnotationPresent(Sub4EyesController.class,
            Sub4EyesController.class.getDeclaredField("dummy1"), XmlElement.class);
      Assert.assertTrue(flag);
   }

}
