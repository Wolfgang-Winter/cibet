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
package com.logitags.cibet.actuator.shiro;

import java.util.Collection;

import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.Subject;

public class CibetSecurityManager extends DefaultSecurityManager {

   public CibetSecurityManager(Collection<Realm> realms) {
      super(realms);
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * org.apache.shiro.mgt.DefaultSecurityManager#save(org.apache.shiro.subject
    * .Subject)
    */
   @Override
   protected void save(Subject subject) {
   }

}
