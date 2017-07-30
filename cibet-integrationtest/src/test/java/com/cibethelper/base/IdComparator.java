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
package com.cibethelper.base;

import java.io.Serializable;
import java.util.Comparator;

import javax.persistence.Id;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.core.AnnotationNotFoundException;
import com.logitags.cibet.core.AnnotationUtil;

public class IdComparator implements Comparator<Object>, Serializable {

   private static Log log = LogFactory.getLog(IdComparator.class);

   /**
    * 
    */
   private static final long serialVersionUID = -492150864778027307L;

   @Override
   public int compare(Object o1, Object o2) {
      if (o1 == null) return -1;
      if (o2 == null) return 1;
      if (!o1.getClass().equals(o2.getClass())) {
         log.warn("Failed to sort list objects by @Id value: objects are not of the same type: "
               + o1.getClass().getName() + " and " + o2.getClass().getName());
         return 0;
      }
      try {
         Object id1 = AnnotationUtil.getValueOfAnnotatedFieldOrMethod(o1, Id.class);
         Object id2 = AnnotationUtil.getValueOfAnnotatedFieldOrMethod(o2, Id.class);

         if (id1 instanceof Comparable) {
            return ((Comparable) id1).compareTo(id2);
         } else {
            log.warn("Failed to sort list objects by @Id value: Id values are not comparable: "
                  + id1 + " and " + id2);
            return 0;
         }
      } catch (AnnotationNotFoundException e) {
         log.warn("Failed to sort list objects by @Id value: " + e.getMessage());
         return 0;
      }
   }
}
