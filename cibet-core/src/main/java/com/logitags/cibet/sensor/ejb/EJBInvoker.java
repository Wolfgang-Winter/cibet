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
package com.logitags.cibet.sensor.ejb;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.jndi.EjbLookup;
import com.logitags.cibet.sensor.common.Invoker;
import com.logitags.cibet.sensor.common.MethodInvoker;

/**
 * creates an EJB on which to invoke a method.
 * <p>
 * Following JNDI- names are used to look up the EJB:
 * <p>
 * Standard: $InterfaceName<br>
 * $BeanName
 * <p>
 * JBoss: [$ejbSimpleName, StatefulAnnotation.name,
 * StatelessAnnotation.name]/[remote,local]
 * <p>
 * Weblogic:
 * 
 * \@Stateless(name = "TestBean", mappedName = "TestBean")<br>
 * public class TestBean implements Test { ...
 * JNDI="TestBean#com.ejb.sessions.Test"<br>
 * = $mappedName#$InterfaceName<br>
 * or<br>
 * $mappedName#ejb.$SimpleInterfaceName
 * <p>
 * Geronimo: bean name + type of interface [Remote, Local]: MyBeanRemote
 * <p>
 * OpenEJB: {ejbName}{interfaceType.annotationName}
 * <p>
 * Glassfish: $MappedName<br>
 * fully qualified name of the remote business interface
 * 
 */
public class EJBInvoker extends MethodInvoker implements Invoker {

   private Log log = LogFactory.getLog(EJBInvoker.class);

   private static Invoker instance = null;

   public static synchronized Invoker createInstance() {
      if (instance == null) {
         instance = new EJBInvoker();
      }
      return instance;
   }

   protected EJBInvoker() {
   }

   /**
    * OpenEJB does not work in Jetty or Tomcat environment with default loading
    * of jndi.properties.
    * 
    * @return
    */
   protected <T> T createObject(String jndiName, Class<T> clazz) {
      T ejb = EjbLookup.lookupEjb(jndiName, clazz);
      if (ejb != null) {
         return ejb;
      } else {
         throw new RuntimeException("Failed to lookup EJB " + clazz.getName());
      }
   }

}
