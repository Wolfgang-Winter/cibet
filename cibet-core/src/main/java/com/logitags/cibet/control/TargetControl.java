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
/**
 * 
 */
package com.logitags.cibet.control;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.sensor.http.HttpRequestResource;

/**
 * evaluates the given class against configured setpoints. Accepts the following patterns as class name in the class
 * tag:
 * <p>
 * concrete class name: com.logitags.cibet.TEntity
 * <p>
 * all classes of the root package: *
 * <p>
 * all classes: **
 * <p>
 * all classes of a package: com.logitags.cibet.*
 * <p>
 * all classes of a package including subpackages: com.logitags.cibet.**
 */
public class TargetControl implements Serializable, Control {

   /**
    * 
    */
   private static final long serialVersionUID = -6583802959373661240L;

   private static Log log = LogFactory.getLog(TargetControl.class);

   public static final String NAME = "target";

   @Override
   public String getName() {
      return NAME;
   }

   @Override
   public Boolean evaluate(Set<String> values, EventMetadata metadata) {
      if (metadata == null) {
         String msg = "failed to execute target evaluation: metadata is null";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }

      if (metadata.getResource().getTarget() == null) {
         return null;
      }

      if (values == null || values.isEmpty()) return null;

      List<String> added = new ArrayList<>();
      List<String> list = new ArrayList<String>(values);
      for (String cl : list) {
         if (cl.startsWith("http://")) {
            added.add(cl.substring(7));
         } else if (cl.startsWith("https://")) {
            String noScheme = cl.substring(8);
            added.add(noScheme);

            if (metadata.getResource() instanceof HttpRequestResource
                  && "CONNECT".equals(((HttpRequestResource) metadata.getResource()).getMethod())) {
               String host = null;
               int occ = noScheme.indexOf("/");
               if (occ != -1) {
                  host = noScheme.substring(0, occ);
               } else {
                  occ = noScheme.indexOf("?");
                  if (occ != -1) {
                     host = noScheme.substring(0, occ);
                  } else {
                     occ = noScheme.indexOf("#");
                     if (occ != -1) {
                        host = noScheme.substring(0, occ);
                     }
                  }
               }

               if (host != null) {
                  added.add(host);
               } else {
                  host = noScheme;
               }

               occ = host.indexOf(":");
               if (occ == -1) {
                  added.add(host + ":443");
               }
            }
         }
      }
      list.addAll(added);

      for (String cl : list) {
         if (log.isDebugEnabled()) {
            log.debug("Target control of " + metadata.getResource().getTarget() + " against: " + cl);
         }
         if (cl.length() == 0 || metadata.getResource().getTarget().equals(cl)) {
            return true;

         } else if (cl.endsWith("*")) {
            String pack = cl.substring(0, cl.length() - 1);
            if (metadata.getResource().getTarget().startsWith(pack)) {
               return true;
            }
         }
      }
      return false;
   }

}
