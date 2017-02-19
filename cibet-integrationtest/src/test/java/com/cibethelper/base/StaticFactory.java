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
public abstract class StaticFactory implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = -5309491886469992064L;

   public static ITComplexEntity create() {
      return StaticFactoryService.instance();
   }

   public static StaticFactoryService2 create2() {
      return StaticFactoryService2.instance();
   }

   public static TEntity create3() {
      return new TEntity();
   }

}
