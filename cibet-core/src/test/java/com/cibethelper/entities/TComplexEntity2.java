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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "CIB_COMPLEXTESTENTITY2")
@NamedQueries({
      @NamedQuery(name = TComplexEntity2.SEL_BY_OWNER, query = "SELECT a FROM TComplexEntity2 a WHERE a.owner = :owner"),
      @NamedQuery(name = TComplexEntity2.SEL_ALL, query = "SELECT a FROM TComplexEntity2 a"),
      @NamedQuery(name = TComplexEntity2.DEL_ALL, query = "DELETE FROM TComplexEntity2") })
public class TComplexEntity2 implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;
   public final static String SEL_BY_OWNER = "com.logitags.cibet.helper.TComplexEntity2.SEL_BY_OWNER";
   public final static String SEL_ALL = "com.logitags.cibet.helper.TComplexEntity2.SEL_ALL";
   public final static String DEL_ALL = "com.logitags.cibet.helper.TComplexEntity2.DEL_ALL";

   @Id
   @GeneratedValue(strategy = GenerationType.AUTO)
   private long id;

   @OneToOne(fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE, CascadeType.PERSIST, CascadeType.MERGE })
   private TEntity ten;

   @OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE, CascadeType.PERSIST, CascadeType.MERGE })
   @JoinTable(name = "CIB_TCOMPLEXENTITY2_LAZY", joinColumns = { @JoinColumn(name = "id") }, inverseJoinColumns = {
         @JoinColumn(name = "lazy_id") })
   private Set<TEntity> lazyList = new LinkedHashSet<TEntity>();

   @OneToMany(fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
   @JoinTable(name = "CIB_TCOMPLEXENTITY2_EAGER", joinColumns = { @JoinColumn(name = "id") }, inverseJoinColumns = {
         @JoinColumn(name = "eager_id") })
   private Set<TEntity> eagerList = new LinkedHashSet<TEntity>();

   private int compValue;

   private String owner;

   @Column(name = "selfowner2")
   private String self_owner;

   @Version
   private int version;

   public long getId() {
      return id;
   }

   public void setId(long id) {
      this.id = id;
   }

   public TEntity getTen() {
      return ten;
   }

   public void setTen(TEntity ten) {
      this.ten = ten;
   }

   public Set<TEntity> getLazyList() {
      return lazyList;
   }

   public void setLazyList(Set<TEntity> lazyList) {
      this.lazyList = lazyList;
   }

   public void addLazyList(TEntity t) {
      lazyList.add(t);
   }

   public Set<TEntity> getEagerList() {
      return eagerList;
   }

   public void setEagerList(Set<TEntity> eagerList) {
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

   public String getOwner2() {
      return self_owner;
   }

   public void setOwner2(String owner) {
      this.self_owner = owner;
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
      for (TEntity t : getLazyList()) {
         b.append(t.getId());
         b.append(", ");
      }
      b.append("\neagerList: ");
      if (getEagerList() != null) {
         for (TEntity t : getEagerList()) {
            b.append(t);
            b.append("\n");
         }
      }
      return b.toString();
   }

   /**
    * @return the self_owner
    */
   public String getSelf_owner() {
      return self_owner;
   }

   /**
    * @param self_owner
    *           the self_owner to set
    */
   public void setSelf_owner(String self_owner) {
      this.self_owner = self_owner;
   }

}
