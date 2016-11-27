/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2014 Dr. Wolfgang Winter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************
 */
package com.logitags.cibet.sensor.jdbc.bridge;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.TemporalType;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;

public class JdbcBridgeTypedQuery<X> extends JdbcBridgeQuery implements
      TypedQuery<X> {

   public JdbcBridgeTypedQuery(JdbcBridgeEntityManager em, String queryName,
         Class<X> resultType) {
      super(em, queryName);
      if (resultType == Object.class || resultType == Tuple.class) {
         additionalQueryValue = null;
      } else {
         additionalQueryValue = resultType;
      }
   }

   @Override
   public List<X> getResultList() {
      return (List<X>) super.getResultList();
   }

   @Override
   public X getSingleResult() {
      return (X) super.getSingleResult();
   }

   @Override
   public TypedQuery<X> setFirstResult(int arg0) {
      return (TypedQuery<X>) super.setFirstResult(arg0);
   }

   @Override
   public TypedQuery<X> setFlushMode(FlushModeType arg0) {
      return (TypedQuery<X>) super.setFlushMode(arg0);
   }

   @Override
   public TypedQuery<X> setHint(String arg0, Object arg1) {
      return (TypedQuery<X>) super.setHint(arg0, arg1);
   }

   @Override
   public TypedQuery<X> setLockMode(LockModeType arg0) {
      return (TypedQuery<X>) super.setLockMode(arg0);
   }

   @Override
   public TypedQuery<X> setMaxResults(int arg0) {
      return (TypedQuery<X>) super.setMaxResults(arg0);
   }

   @Override
   public <T> TypedQuery<X> setParameter(Parameter<T> arg0, T arg1) {
      return (TypedQuery<X>) super.setParameter(arg0, arg1);
   }

   @Override
   public TypedQuery<X> setParameter(String arg0, Object arg1) {
      return (TypedQuery<X>) super.setParameter(arg0, arg1);
   }

   @Override
   public TypedQuery<X> setParameter(int arg0, Object arg1) {
      return (TypedQuery<X>) super.setParameter(arg0, arg1);
   }

   @Override
   public TypedQuery<X> setParameter(Parameter<Calendar> arg0, Calendar arg1,
         TemporalType arg2) {
      return (TypedQuery<X>) super.setParameter(arg0, arg1, arg2);
   }

   @Override
   public TypedQuery<X> setParameter(Parameter<Date> arg0, Date arg1,
         TemporalType arg2) {
      return (TypedQuery<X>) super.setParameter(arg0, arg1, arg2);
   }

   @Override
   public TypedQuery<X> setParameter(String arg0, Calendar arg1,
         TemporalType arg2) {
      return (TypedQuery<X>) super.setParameter(arg0, arg1, arg2);
   }

   @Override
   public TypedQuery<X> setParameter(String arg0, Date arg1, TemporalType arg2) {
      return (TypedQuery<X>) super.setParameter(arg0, arg1, arg2);
   }

   @Override
   public TypedQuery<X> setParameter(int arg0, Calendar arg1, TemporalType arg2) {
      return (TypedQuery<X>) super.setParameter(arg0, arg1, arg2);
   }

   @Override
   public TypedQuery<X> setParameter(int arg0, Date arg1, TemporalType arg2) {
      return (TypedQuery<X>) super.setParameter(arg0, arg1, arg2);
   }

}
