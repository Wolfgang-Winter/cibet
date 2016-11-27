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
package com.cibethelper.entities;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Transient;

/**
 *
 */
public abstract class AbstractTEntity implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private int transi;

   private boolean boolProp;

   private static String statString = "Werner";

   private TEntity superEnt;

   private Map<String, TEntity> map = new HashMap<String, TEntity>();

   /**
    * @return the transi
    */
   @Transient
   public int getTransi() {
      return transi;
   }

   /**
    * @param transi
    *           the transi to set
    */
   public void setTransi(int transi) {
      this.transi = transi;
   }

   /**
    * @return the boolProp
    */
   public boolean isBoolProp() {
      return boolProp;
   }

   /**
    * @param boolProp
    *           the boolProp to set
    */
   public void setBoolProp(boolean boolProp) {
      this.boolProp = boolProp;
   }

   /**
    * @return the statString
    */
   public static String getStatString() {
      return statString;
   }

   /**
    * @param statString
    *           the statString to set
    */
   public static void setStatString(String statString) {
      AbstractTEntity.statString = statString;
   }

   /**
    * @return the superEnt
    */
   public TEntity getSuperEnt() {
      return superEnt;
   }

   /**
    * @param superEnt
    *           the superEnt to set
    */
   public void setSuperEnt(TEntity superEnt) {
      this.superEnt = superEnt;
   }

   /**
    * @return the map
    */
   public Map<String, TEntity> getMap() {
      return map;
   }

   /**
    * @param map
    *           the map to set
    */
   public void setMap(Map<String, TEntity> map) {
      this.map = map;
   }

}
