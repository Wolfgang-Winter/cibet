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

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import javax.persistence.ColumnResult;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 */
@Entity
// @Audited
@Table(name = "CIB_TESTENTITY")
@NamedQueries({ @NamedQuery(name = TEntity.SEL_BY_OWNER, query = "SELECT a FROM TEntity a WHERE a.owner = :owner"),
      @NamedQuery(name = TEntity.DEL_ALL, query = "DELETE FROM TEntity") })
@SqlResultSetMappings({ @SqlResultSetMapping(name = "TEntityRSMapping", columns = {
      @ColumnResult(name = "mapped_counter"), @ColumnResult(name = "mapped_owner") }) })
public class TEntity implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;
   public final static String SEL_BY_OWNER = "com.logitags.cibet.helper.TEntity.SEL_BY_OWNER";
   public final static String DEL_ALL = "com.logitags.cibet.helper.TEntity.DEL_ALL";

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

   // @Temporal(TemporalType.TIME)
   // private Calendar xCaltime;

   @Temporal(TemporalType.TIMESTAMP)
   private Calendar xCaltimestamp;

   public TEntity() {
   }

   public TEntity(String nameValue, int counter, String owner) {
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
      if (!(obj instanceof TEntity))
         return false;
      TEntity t = (TEntity) obj;

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

   // /**
   // * @return the xCaltime
   // */
   // public Calendar getXCaltime() {
   // return xCaltime;
   // }

   // /**
   // * @param xCaltime
   // * the xCaltime to set
   // */
   // public void setXCaltime(Calendar xCaltime) {
   // this.xCaltime = xCaltime;
   // }

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
