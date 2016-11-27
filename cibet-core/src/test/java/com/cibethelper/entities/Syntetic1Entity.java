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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

/**
 *
 */
@Entity
@Table(name = "CIB_Syntetic1Entity")
public class Syntetic1Entity {

   @Id
   @GeneratedValue(strategy = GenerationType.AUTO)
   private long id;

   @Version
   private Date version;

   @Lob
   private int[] intArray = { 3, 6 };

   @Transient
   private TEntity[] entArray;

   @Transient
   private Map<String, TEntity> map = new HashMap<String, TEntity>();

   /**
    * @return the id
    */
   public long getId() {
      return id;
   }

   /**
    * @param id
    *           the id to set
    */
   public void setId(long id) {
      this.id = id;
   }

   /**
    * @return the version
    */
   public Date getVersion() {
      return version;
   }

   /**
    * @param version
    *           the version to set
    */
   public void setVersion(Date version) {
      this.version = version;
   }

   /**
    * @return the intArray
    */
   public int[] getIntArray() {
      return intArray;
   }

   /**
    * @param intArray
    *           the intArray to set
    */
   public void setIntArray(int[] intArray) {
      this.intArray = intArray;
   }

   /**
    * @return the entArray
    */
   public TEntity[] getEntArray() {
      return entArray;
   }

   /**
    * @param entArray
    *           the entArray to set
    */
   public void setEntArray(TEntity[] entArray) {
      this.entArray = entArray;
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

   public void addMap(String key, TEntity t) {
      map.put(key, t);
   }

}
