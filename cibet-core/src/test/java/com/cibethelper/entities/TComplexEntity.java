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
import javax.persistence.Transient;
import javax.persistence.Version;

import com.logitags.cibet.sensor.pojo.CibetIntercept;
import com.logitags.cibet.sensor.pojo.PojoInvoker;

//@Audited
@Entity
@Table(name = "CIB_COMPLEXTESTENTITY")
// @CibetIntercept(factoryClass = PojoInvoker.class)
@NamedQueries({
      @NamedQuery(name = TComplexEntity.SEL_BY_OWNER, query = "SELECT a FROM TComplexEntity a WHERE a.owner = :owner"),
      @NamedQuery(name = TComplexEntity.SEL_ALL, query = "SELECT a FROM TComplexEntity a"),
      @NamedQuery(name = TComplexEntity.DEL_ALL, query = "DELETE FROM TComplexEntity") })
public class TComplexEntity implements Serializable, ITComplexEntity {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;
   public final static String SEL_BY_OWNER = "com.logitags.cibet.helper.TComplexEntity.SEL_BY_OWNER";
   public final static String SEL_ALL = "com.logitags.cibet.helper.TComplexEntity.SEL_ALL";
   public final static String DEL_ALL = "com.logitags.cibet.helper.TComplexEntity.DEL_ALL";

   private static int statValue = 55;

   @Id
   @GeneratedValue(strategy = GenerationType.AUTO)
   private long id;

   @OneToOne(fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE, CascadeType.PERSIST, CascadeType.MERGE })
   private TEntity ten;

   @OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE, CascadeType.PERSIST, CascadeType.MERGE })
   @JoinTable(name = "CIB_TCOMPLEXENTITY_LAZY", joinColumns = { @JoinColumn(name = "id") }, inverseJoinColumns = {
         @JoinColumn(name = "lazy_id") })
   private Set<TEntity> lazyList = new LinkedHashSet<TEntity>();

   @OneToMany(fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
   @JoinTable(name = "CIB_TCOMPLEXENTITY_EAGER", joinColumns = { @JoinColumn(name = "id") }, inverseJoinColumns = {
         @JoinColumn(name = "eager_id") })
   private Set<TEntity> eagerList = new LinkedHashSet<TEntity>();

   private int compValue;

   private String owner;

   @Transient
   private TCompareEntity compareEntity = new TCompareEntity();

   @Version
   private int version;

   @CibetIntercept(factoryClass = PojoInvoker.class)
   public long getId() {
      return id;
   }

   public void setId(long id) {
      this.id = id;
   }

   public TEntity getTen() {
      return ten;
   }

   @CibetIntercept(factoryClass = PojoInvoker.class)
   public void setTen(TEntity ten) {
      this.ten = ten;
   }

   public Set<TEntity> getLazyList() {
      return lazyList;
   }

   public void setLazyList(Set<TEntity> lazyList) {
      this.lazyList = lazyList;
   }

   @CibetIntercept(factoryClass = PojoInvoker.class)
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

   @CibetIntercept(factoryClass = PojoInvoker.class)
   public void setCompValue(int compValue) {
      this.compValue = compValue;
   }

   @CibetIntercept(factoryClass = PojoInvoker.class)
   public int setAndGetCompValue(int compValue) {
      this.compValue = compValue;
      return this.compValue;
   }

   public String getOwner() {
      return owner;
   }

   @CibetIntercept(factoryClass = PojoInvoker.class)
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
   @CibetIntercept(factoryClass = PojoInvoker.class)
   public static void setStaticStatValue(int statValue) {
      TComplexEntity.statValue = statValue;
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
   @CibetIntercept(factoryClass = PojoInvoker.class)
   public void setStatValue(int statValue) {
      TComplexEntity.statValue = statValue;
   }

   /**
    * @return the compareEntity
    */
   public TCompareEntity getCompareEntity() {
      return compareEntity;
   }

   /**
    * @param compareEntity
    *           the compareEntity to set
    */
   public void setCompareEntity(TCompareEntity compareEntity) {
      this.compareEntity = compareEntity;
   }

   @CibetIntercept(factoryClass = PojoInvoker.class)
   public List<String> giveCollection() {
      List<String> l = new ArrayList<String>();
      l.add("eins");
      l.add("zwei");
      l.add("drei");
      l.add("vier");
      return l;
   }

   @CibetIntercept(factoryClass = PojoInvoker.class)
   public List<String> giveCollection2(List<String> in) {
      return in;
   }

   @CibetIntercept(factoryClass = PojoInvoker.class)
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
      b.append(" ; compareEntity: ");
      b.append(compareEntity);
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

}
