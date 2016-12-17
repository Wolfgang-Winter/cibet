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
package com.cibethelper.base;

import java.io.Serializable;

import com.cibethelper.entities.ITComplexEntity;
import com.cibethelper.entities.TEntity;

/**
 *
 */
public class SingletonFactory implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = -9066544324144406750L;
   private static SingletonFactory inst;

   public static synchronized SingletonFactory getInstance() {
      if (inst == null) {
         inst = new SingletonFactory();
      }
      return inst;
   }

   protected SingletonFactory() {
   }

   public ITComplexEntity create() {
      return SingletonFactoryService.instance();
   }

   public SingletonFactoryService2 create2() {
      return SingletonFactoryService2.instance();
   }

   public static TEntity create3() {
      return new TEntity();
   }

   public static String withException() {
      throw new IllegalArgumentException("Jason");
   }

}
