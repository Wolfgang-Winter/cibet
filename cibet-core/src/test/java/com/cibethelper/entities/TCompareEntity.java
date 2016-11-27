/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Germany
 *
 * All rights reserved
 *
 *******************************************************************************
 */

package com.cibethelper.entities;

import java.util.LinkedList;
import java.util.List;

import com.logitags.cibet.sensor.pojo.CibetIntercept;
import com.logitags.cibet.sensor.pojo.PojoInvoker;

/**
 *
 */
@CibetIntercept(factoryClass = PojoInvoker.class, param = "Hasenfu�")
public class TCompareEntity extends AbstractTEntity {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private long id;

   private String nameValue;

   private int counter;

   private String owner;

   private int[] intArray = new int[] { 1, 3, 5, 6 };

   private List<TEntity> entList = new LinkedList<TEntity>();

   private static String constrValue;

   private static String statValue = "garnix";

   public TCompareEntity() {
   }

   public TCompareEntity(String value) {
      constrValue = value;
   }

   public TCompareEntity(String nameValue, int counter, String owner) {
      this.nameValue = nameValue;
      this.counter = counter;
      this.owner = owner;
   }

   /**
    * 
    * @return the id
    */
   public long getId() {
      return id;
   }

   /**
    * 
    * @param id
    *           the id to set
    */
   public void setId(long id) {
      this.id = id;
   }

   /**
    * 
    * @return the nameValue
    */
   public String getNameValue() {
      return nameValue;
   }

   /**
    * 
    * @param nameValue
    *           the nameValue to set
    */
   public void setNameValue(String nameValue) {
      this.nameValue = nameValue;
   }

   /**
    * 
    * @return the counter
    */
   public int getCounter() {
      return counter;
   }

   /**
    * 
    * @param counter
    *           the counter to set
    */
   public void setCounter(int counter) {
      this.counter = counter;
   }

   public String getOwner() {
      return owner;
   }

   public void setOwner(String owner) {
      this.owner = owner;
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof TCompareEntity))
         return false;
      TCompareEntity t = (TCompareEntity) obj;

      return (id == t.getId() && counter == t.getCounter() && nameValue.equals(t.getNameValue())
            && owner.equals(t.getOwner()));
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
    * @return the entList
    */
   public List<TEntity> getEntList() {
      return entList;
   }

   /**
    * @param entList
    *           the entList to set
    */
   public void setEntList(List<TEntity> entList) {
      this.entList = entList;
   }

   /**
    * @return the constrValue
    */
   public String getConstrValue() {
      return constrValue;
   }

   /**
    * @param constrValue
    *           the constrValue to set
    */
   public void setConstrValue(String constrValue) {
      this.constrValue = constrValue;
   }

   /**
    * @return the statValue
    */
   public String getStatValue() {
      return statValue;
   }

   /**
    * @param statValue
    *           the statValue to set
    */
   public void setStatValue(String statValue) {
      TCompareEntity.statValue = statValue;
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      if (intArray != null) {
         for (int i : intArray) {
            b.append(i);
         }
      } else {
         b.append("intArray null");
      }

      return b.toString();
   }
}
