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

import java.util.ArrayList;
import java.util.List;

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
public class TargetControl extends AbstractControl {

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
   public boolean evaluate(Object controlValue, EventMetadata metadata) {
      if (metadata.getResource().getTargetType() == null) {
         return true;
      }

      List<String> added = new ArrayList<>();
      List<String> list = new ArrayList<String>((List<String>) controlValue);
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

               // int occ = noScheme.indexOf("/");
               // if (occ != -1) {
               // added.add(noScheme.substring(0, occ));
               // continue;
               // }
               // occ = noScheme.indexOf("?");
               // if (occ != -1) {
               // added.add(noScheme.substring(0, occ));
               // continue;
               // }
               // occ = noScheme.indexOf("#");
               // if (occ != -1) {
               // added.add(noScheme.substring(0, occ));
               // }
            }
         }
      }
      list.addAll(added);
      if (log.isDebugEnabled()) {
         log.debug("Target control of " + metadata.getResource().getTargetType() + " against:");
         for (String cl : list) {
            log.debug(cl);
         }
      }

      for (String cl : list) {
         // log.debug(metadata.getResource().getTargetType() + " : " + cl);
         if (cl.length() == 0 || metadata.getResource().getTargetType().equals(cl)) {
            return true;

         } else if (cl.endsWith("*")) {
            String pack = cl.substring(0, cl.length() - 1);
            if (metadata.getResource().getTargetType().startsWith(pack)) {
               return true;
            }
         }
      }
      return false;
   }
}
