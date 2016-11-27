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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;

import com.logitags.cibet.context.CibetContext;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalSessionScope;

@CibetContext
public abstract class ShiroService {

   private static Log log = LogFactory.getLog(ShiroService.class);

   /**
    * authenticates a second user while the main user is already authenticated. The authentication information for the
    * second user are not stored in the Shiro security context but in Cibet context. A second authentication is
    * necessary for the Two-man-rule actuator.
    * 
    * @param auth
    *           Credentials of the second user
    */
   public static void logonSecondUser(AuthenticationToken auth) {
      DefaultSecurityManager utilsSecMan = null;
      try {
         utilsSecMan = (DefaultSecurityManager) SecurityUtils.getSecurityManager();
      } catch (ClassCastException e) {
         String msg = "+++++++++++++++++++++++++++++++++\n"
               + "It seems you use a custom SecurityManager implementation."
               + " Method ShiroManagerImpl.logonSecondUser() works only when the DefaultSecuritymanager "
               + "is configured\n" + "+++++++++++++++++++++++++++++++++";
         log.fatal(msg);
         throw e;
      }

      SecurityManager secMan = new CibetSecurityManager(utilsSecMan.getRealms());
      Subject subject = new Subject.Builder(secMan).sessionCreationEnabled(false).buildSubject();

      subject.login(auth);
      String user = subject.getPrincipal() == null ? "second user" : subject.getPrincipal().toString();
      Context.internalSessionScope().setSecondUser(user);
      Context.internalSessionScope().setProperty(InternalSessionScope.SECOND_PRINCIPAL, subject);
   }

   /**
    * removes the authentication credentials of the second user from Cibet context.
    */
   public static void logoffSecondUser() {
      Context.internalSessionScope().setSecondUser(null);
      Context.internalSessionScope().setProperty(InternalSessionScope.SECOND_PRINCIPAL, null);
   }

   /**
    * returns the Shiro session id.
    * 
    * @return Shiro session id
    */
   public static String securityContext() {
      Subject currentUser = SecurityUtils.getSubject();
      String ssid = currentUser.getSession().getId().toString();
      return ssid;
   }

}
