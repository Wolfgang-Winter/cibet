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

import java.util.List;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 *
 */
// @Entity
// @Table(name = "CIB_Syntetic2Entity")
public class Syntetic2Entity {

   @Id
   @GeneratedValue(strategy = GenerationType.AUTO)
   private String id;

   private List intList;

   /**
    * @return the id
    */
   public String getId() {
      return id;
   }

   /**
    * @param id
    *           the id to set
    */
   public void setId(String id) {
      this.id = id;
   }

   /**
    * @return the intList
    */
   public List getIntList() {
      return intList;
   }

   /**
    * @param intList
    *           the intList to set
    */
   public void setIntList(List intList) {
      this.intList = intList;
   }

}
