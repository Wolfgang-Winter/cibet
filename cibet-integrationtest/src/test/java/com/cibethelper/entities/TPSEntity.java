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
package com.cibethelper.entities;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class TPSEntity {

   @Id
   @GeneratedValue(strategy = GenerationType.AUTO)
   private long id;

   private String langString;

   private byte[] bytes;

   @Column(name = "thedate")
   private Date date;

   private Time time;

   private float floatValue;

   private double doubleValue;

   private Timestamp timestamp;

   private byte oneByte;

   private boolean bool;

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
    * @return the langString
    */
   public String getLangString() {
      return langString;
   }

   /**
    * @param langString
    *           the langString to set
    */
   public void setLangString(String langString) {
      this.langString = langString;
   }

   /**
    * @return the bytes
    */
   public byte[] getBytes() {
      return bytes;
   }

   /**
    * @param bytes
    *           the bytes to set
    */
   public void setBytes(byte[] bytes) {
      this.bytes = bytes;
   }

   /**
    * @return the date
    */
   public Date getDate() {
      return date;
   }

   /**
    * @param date
    *           the date to set
    */
   public void setDate(Date date) {
      this.date = date;
   }

   /**
    * @return the time
    */
   public Time getTime() {
      return time;
   }

   /**
    * @param time
    *           the time to set
    */
   public void setTime(Time time) {
      this.time = time;
   }

   /**
    * @return the floatValue
    */
   public float getFloatValue() {
      return floatValue;
   }

   /**
    * @param floatValue
    *           the floatValue to set
    */
   public void setFloatValue(float floatValue) {
      this.floatValue = floatValue;
   }

   /**
    * @return the doubleValue
    */
   public double getDoubleValue() {
      return doubleValue;
   }

   /**
    * @param doubleValue
    *           the doubleValue to set
    */
   public void setDoubleValue(double doubleValue) {
      this.doubleValue = doubleValue;
   }

   /**
    * @return the timestamp
    */
   public Timestamp getTimestamp() {
      return timestamp;
   }

   /**
    * @param timestamp
    *           the timestamp to set
    */
   public void setTimestamp(Timestamp timestamp) {
      this.timestamp = timestamp;
   }

   /**
    * @return the oneByte
    */
   public byte getOneByte() {
      return oneByte;
   }

   /**
    * @param oneByte
    *           the oneByte to set
    */
   public void setOneByte(byte oneByte) {
      this.oneByte = oneByte;
   }

   /**
    * @return the bool
    */
   public boolean isBool() {
      return bool;
   }

   /**
    * @param bool
    *           the bool to set
    */
   public void setBool(boolean bool) {
      this.bool = bool;
   }

}
