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

package com.cibethelper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.envers.Audited;

@Audited
@Entity
@Table(name = "CIB_COMPLEXTESTENTITY")
public class AuditedTComplexEntity implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private static int statValue = 55;

   @Id
   @GeneratedValue(strategy = GenerationType.AUTO)
   private long id;

   @OneToOne(fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE, CascadeType.PERSIST, CascadeType.MERGE })
   private AuditedTEntity ten;

   @OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE, CascadeType.PERSIST, CascadeType.MERGE })
   @JoinTable(name = "CIB_TCOMPLEXENTITY_LAZY", joinColumns = { @JoinColumn(name = "id") }, inverseJoinColumns = {
         @JoinColumn(name = "lazy_id") })
   private Set<AuditedTEntity> lazyList = new LinkedHashSet<AuditedTEntity>();

   @OneToMany(fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
   @JoinTable(name = "CIB_TCOMPLEXENTITY_EAGER", joinColumns = { @JoinColumn(name = "id") }, inverseJoinColumns = {
         @JoinColumn(name = "eager_id") })
   private Set<AuditedTEntity> eagerList = new LinkedHashSet<AuditedTEntity>();

   private int compValue;

   private String owner;

   @Version
   private int version;

   public long getId() {
      return id;
   }

   public void setId(long id) {
      this.id = id;
   }

   public AuditedTEntity getTen() {
      return ten;
   }

   public void setTen(AuditedTEntity ten) {
      this.ten = ten;
   }

   public Set<AuditedTEntity> getLazyList() {
      return lazyList;
   }

   public void setLazyList(Set<AuditedTEntity> lazyList) {
      this.lazyList = lazyList;
   }

   public void addLazyList(AuditedTEntity t) {
      lazyList.add(t);
   }

   public Set<AuditedTEntity> getEagerList() {
      return eagerList;
   }

   public void setEagerList(Set<AuditedTEntity> eagerList) {
      this.eagerList = eagerList;
   }

   public int getCompValue() {
      return compValue;
   }

   public void setCompValue(int compValue) {
      this.compValue = compValue;
   }

   public int setAndGetCompValue(int compValue) {
      this.compValue = compValue;
      return this.compValue;
   }

   public String getOwner() {
      return owner;
   }

   public void setOwner(String owner) {
      this.owner = owner;
   }

   /**
    * @return the version
    */
   public int getVersion() {
      return version;
   }

   /**
    * @param version
    *           the version to set
    */
   public void setVersion(int version) {
      this.version = version;
   }

   /**
    * @return the statValue
    */
   public static int getStaticStatValue() {
      return statValue;
   }

   /**
    * @param statValue
    *           the statValue to set
    */
   public static void setStaticStatValue(int statValue) {
      AuditedTComplexEntity.statValue = statValue;
   }

   /**
    * @return the statValue
    */
   public int getStatValue() {
      return statValue;
   }

   /**
    * @param statValue
    *           the statValue to set
    */
   public void setStatValue(int statValue) {
      AuditedTComplexEntity.statValue = statValue;
   }

   public List<String> giveCollection() {
      List<String> l = new ArrayList<String>();
      l.add("eins");
      l.add("zwei");
      l.add("drei");
      l.add("vier");
      return l;
   }

   public List<String> giveCollection2(List<String> in) {
      return in;
   }

   public List<String> giveCollection3(List<String> in, List<String> in2) {
      return in;
   }

   public List<String> giveCollection4(List<String> in) {
      for (int i = 0; i < in.size(); i++) {
         in.set(i, in.get(i) + "X");
      }
      return in;
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("id: ");
      b.append(id);
      b.append(" ; compValue: ");
      b.append(compValue);
      b.append(" ; owner: ");
      b.append(owner);
      b.append(" ; ten: [");
      b.append(ten);
      b.append("] ; version: ");
      b.append(version);

      b.append("\nlazyList: ");
      for (AuditedTEntity t : getLazyList()) {
         b.append(t.getId());
         b.append(", ");
      }
      b.append("\neagerList: ");
      if (getEagerList() != null) {
         for (AuditedTEntity t : getEagerList()) {
            b.append(t);
            b.append("\n");
         }
      }
      return b.toString();
   }

}
