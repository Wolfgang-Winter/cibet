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
package com.logitags.cibet.resource;

import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.core.CibetUtil;

/**
 * Method or HTTP parameter.
 * 
 */
@Entity
@Table(name = "CIB_RESOURCEPARAMETER")
public class ResourceParameter implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 881896023854621131L;

   private static Log log = LogFactory.getLog(ResourceParameter.class);

   /**
    * unique internal ID
    */
   @Id
   private String parameterId;

   /**
    * The parameter value as String. Only set when JPA search attributes are configured in an actuator
    */
   private String stringValue;

   /**
    * parameter name
    */
   private String name;

   /**
    * class name of the parameter
    */
   private String classname;

   /**
    * the normalised encoded parameter value.
    */
   @Lob
   private byte[] encodedValue;

   /**
    * the parameter value.
    */
   @Transient
   private transient Object unencodedValue;

   /**
    * parameter sequence is important for method signatures. Oracle loads parameters sometimes not in the correct order.
    */
   private int sequence;

   /**
    * type of the parameter
    */
   @Enumerated(EnumType.STRING)
   @Column(length = 50)
   private ParameterType parameterType;

   public ResourceParameter() {
   }

   public ResourceParameter(ResourceParameter par) {
      setEncodedValue(par.getEncodedValue());
      setName(par.getName());
      setClassname(par.getClassname());
      setParameterType(par.getParameterType());
      setSequence(par.getSequence());
      setStringValue(par.getStringValue());
   }

   public ResourceParameter(String name, String type, Object value, ParameterType pt, int sequence) {
      if (name == null || type == null || pt == null) {
         throw new IllegalArgumentException("Parameter name or class type or parameterType is null");
      }
      this.name = name;
      this.classname = type;
      setUnencodedValue(value);
      parameterType = pt;
      this.sequence = sequence;
   }

   @PrePersist
   @PreUpdate
   public void prePersist() {
      if (encodedValue == null && unencodedValue != null) {
         try {
            encodedValue = CibetUtil.encode(unencodedValue);
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      }

      if (parameterId == null) {
         parameterId = UUID.randomUUID().toString();
         log.debug("PREPERSIST: " + parameterId);
      }
   }

   /**
    * parameter name
    * 
    * @return the name
    */
   public String getName() {
      return name;
   }

   /**
    * parameter name
    * 
    * @param name
    *           the name to set
    */
   public void setName(String name) {
      this.name = name;
   }

   /**
    * class name of the parameter
    * 
    * @return the type
    */
   public String getClassname() {
      return classname;
   }

   /**
    * class name of the parameter
    * 
    * @param type
    *           the type to set
    */
   public void setClassname(String type) {
      this.classname = type;
   }

   /**
    * the normalised encoded parameter value.
    * 
    * @return the encodedValue
    */
   public byte[] getEncodedValue() {
      prePersist();
      return encodedValue;
   }

   /**
    * the normalised encoded parameter value.
    * 
    * @param encodedValue
    *           the encodedValue to set
    */
   public void setEncodedValue(byte[] encodedValue) {
      this.encodedValue = encodedValue;
   }

   /**
    * the parameter value.
    * 
    * @return the value
    */
   public Object getUnencodedValue() {
      if (unencodedValue == null) {
         unencodedValue = CibetUtil.decode(encodedValue);
      }
      return unencodedValue;
   }

   /**
    * the parameter value.
    * 
    * @param value
    *           the value to set
    */
   public void setUnencodedValue(Object value) {
      this.unencodedValue = value;
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("Parameter id: ");
      b.append(parameterId);
      b.append(", name: ");
      b.append(name);
      b.append(", sequence: ");
      b.append(sequence);
      b.append(", classname: ");
      b.append(classname);
      b.append(", value: ");
      b.append(getUnencodedValue());
      return b.toString();
   }

   /**
    * @return the parameterType
    */
   public ParameterType getParameterType() {
      return parameterType;
   }

   /**
    * @param parameterType
    *           the parameterType to set
    */
   public void setParameterType(ParameterType parameterType) {
      this.parameterType = parameterType;
   }

   /**
    * parameter sequence is important for method signatures. Oracle loads parameters sometimes not in the correct order.
    * 
    * @return the sequence
    */
   public int getSequence() {
      return sequence;
   }

   /**
    * parameter sequence is important for method signatures. Oracle loads parameters sometimes not in the correct order.
    * 
    * @param sequence
    *           the sequence to set
    */
   public void setSequence(int sequence) {
      this.sequence = sequence;
   }

   /**
    * @return the parameterId
    */
   public String getParameterId() {
      return parameterId;
   }

   /**
    * @param parameterId
    *           the parameterId to set
    */
   public void setParameterId(String parameterId) {
      this.parameterId = parameterId;
   }

   /**
    * The parameter value as String. Only set when JPA search attributes are configured in an actuator
    * 
    * @return the value
    */
   public String getStringValue() {
      return stringValue;
   }

   /**
    * The parameter value as String. Only set when JPA search attributes are configured in an actuator
    * 
    * @param uniqueId
    *           the value to set
    */
   public void setStringValue(String uniqueId) {
      this.stringValue = uniqueId;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((parameterType == null) ? 0 : parameterType.hashCode());
      result = prime * result + ((stringValue == null) ? 0 : stringValue.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      ResourceParameter other = (ResourceParameter) obj;
      if (name == null) {
         if (other.name != null)
            return false;
      } else if (!name.equals(other.name))
         return false;
      if (parameterType != other.parameterType)
         return false;
      if (stringValue == null) {
         if (other.stringValue != null)
            return false;
      } else if (!stringValue.equals(other.stringValue))
         return false;
      return true;
   }

}
