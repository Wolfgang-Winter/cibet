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
package com.logitags.cibet.sensor.jdbc.driver;

import java.io.Serializable;

/**
 * Represents a parameter in the SQL statement. This is either an insert, update
 * parameter or a parameter in the WHERE clause.
 * 
 */
public class SqlParameter implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = -6934872254604626571L;

   /**
    * coumn name
    */
   private String column;

   /**
    * column value
    */
   private transient Object value;

   /**
    * sequence in a PreparedStatement
    */
   private int sequence;

   public SqlParameter(SqlParameter copy) {
      column = copy.column;
      value = copy.value;
      sequence = copy.sequence;
   }

   public SqlParameter(String col, Object val) {
      column = col;
      value = val;
   }

   /**
    * @return the column
    */
   public String getColumn() {
      return column;
   }

   /**
    * @param column
    *           the column to set
    */
   public void setColumn(String column) {
      this.column = column;
   }

   /**
    * @return the value
    */
   public Object getValue() {
      return value;
   }

   /**
    * @param value
    *           the value to set
    */
   public void setValue(Object value) {
      this.value = value;
   }

   /**
    * @return the sequence
    */
   public int getSequence() {
      return sequence;
   }

   /**
    * @param sequence
    *           the sequence to set
    */
   public void setSequence(int sequence) {
      this.sequence = sequence;
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append(sequence);
      b.append(": ");
      b.append(column);
      b.append("=");
      b.append(value);
      return b.toString();
   }

}
