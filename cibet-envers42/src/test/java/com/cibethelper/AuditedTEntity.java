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

package com.cibethelper;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

/**
 *
 */
@Entity
@Audited
@Table(name = "CIB_TESTENTITY")
public class AuditedTEntity implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   @Id
   @GeneratedValue(strategy = GenerationType.AUTO)
   private long id;

   private String nameValue;

   private int counter;

   private String owner;

   @Temporal(TemporalType.DATE)
   private Date xdate;

   @Temporal(TemporalType.TIME)
   private Date xtime;

   @Temporal(TemporalType.TIMESTAMP)
   private Date xtimestamp;

   @Temporal(TemporalType.DATE)
   private Calendar xCaldate;

   @Temporal(TemporalType.TIMESTAMP)
   private Calendar xCaltimestamp;

   public AuditedTEntity() {
   }

   public AuditedTEntity(String nameValue, int counter, String owner) {
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

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("TEntity id: ");
      b.append(id);
      b.append(", counter: ");
      b.append(counter);
      b.append(", owner: ");
      b.append(owner);
      b.append(", xCaltimestamp: ");
      b.append(xCaltimestamp);
      return b.toString();
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof AuditedTEntity))
         return false;
      AuditedTEntity t = (AuditedTEntity) obj;

      return (id == t.getId() && counter == t.getCounter() && nameValue.equals(t.getNameValue())
            && owner.equals(t.getOwner()));
   }

   /**
    * @return the xdate
    */
   public Date getXdate() {
      return xdate;
   }

   /**
    * @param xdate
    *           the xdate to set
    */
   public void setXdate(Date xdate) {
      this.xdate = xdate;
   }

   /**
    * @return the xtime
    */
   public Date getXtime() {
      return xtime;
   }

   /**
    * @param xtime
    *           the xtime to set
    */
   public void setXtime(Date xtime) {
      this.xtime = xtime;
   }

   /**
    * @return the xtimestamp
    */
   public Date getXtimestamp() {
      return xtimestamp;
   }

   /**
    * @param xtimestamp
    *           the xtimestamp to set
    */
   public void setXtimestamp(Date xtimestamp) {
      this.xtimestamp = xtimestamp;
   }

   /**
    * @return the xCaldate
    */
   public Calendar getXCaldate() {
      return xCaldate;
   }

   /**
    * @param xCaldate
    *           the xCaldate to set
    */
   public void setXCaldate(Calendar xCaldate) {
      this.xCaldate = xCaldate;
   }

   /**
    * @return the xCaltimestamp
    */
   public Calendar getXCaltimestamp() {
      return xCaltimestamp;
   }

   /**
    * @param xCaltimestamp
    *           the xCaltimestamp to set
    */
   public void setXCaltimestamp(Calendar xCaltimestamp) {
      this.xCaltimestamp = xCaltimestamp;
   }

}
