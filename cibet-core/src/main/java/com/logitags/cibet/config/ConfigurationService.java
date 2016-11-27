/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2012 Dr. Wolfgang Winter
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
package com.logitags.cibet.config;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.config.ProxyConfig.ProxyMode;
import com.udojava.jmx.wrapper.JMXBean;
import com.udojava.jmx.wrapper.JMXBeanOperation;
import com.udojava.jmx.wrapper.JMXBeanParameter;

@JMXBean(description = "JMX bean for initilizing Cibet")
public class ConfigurationService {

   private static Log log = LogFactory.getLog(ConfigurationService.class);

   private String exceptionToString(Throwable e, String pretext) {
      String txt = pretext + e.getMessage();
      log.error(txt, e);
      StringWriter w = new StringWriter();
      PrintWriter printer = new PrintWriter(w);
      e.printStackTrace(printer);
      return txt + "\n" + w.toString();
   }

   @JMXBeanOperation(name = "initialise", description = "re-initilizes Cibet from the config file. Overwrites any configuration done in code")
   public String initialise() {
      try {
         Configuration.instance().initialise();
      } catch (Throwable e) {
         return exceptionToString(e, "failed to start Configuration: ");
      }
      return "Configuration started successfully.";
   }

   @JMXBeanOperation(name = "re-init AuthenticationProvider", description = "re-initilizes Cibet AuthenticationProvider from the config file. Overwrites any AuthenticationProvider set in code")
   public String reinitAuthenticationProvider() {
      try {
         Configuration.instance().reinitAuthenticationProvider(null);
      } catch (Throwable e) {
         return exceptionToString(e, "failed to reinit AuthenticationProvider: ");
      }
      return "AuthenticationProvider successfully reinitialized.";
   }

   @JMXBeanOperation(name = "re-init NotificationProvider", description = "re-initilizes Cibet NotificationProvider from the config file. Overwrites any NotificationProvider set in code")
   public String reinitNotificationProvider() {
      try {
         Configuration.instance().reinitNotificationProvider(null);
      } catch (Throwable e) {
         return exceptionToString(e, "failed to reinit NotificationProvider: ");
      }
      return "NotificationProvider successfully reinitialized.";
   }

   @JMXBeanOperation(name = "re-init SecurityProvider", description = "re-initilizes Cibet SecurityProvider from the config file. Overwrites any SecurityProvider set in code")
   public String reinitSecurityProvider() {
      try {
         Configuration.instance().reinitSecurityProvider(null);
      } catch (Throwable e) {
         return exceptionToString(e, "failed to reinit SecurityProvider: ");
      }
      return "SecurityProvider successfully reinitialized.";
   }

   @JMXBeanOperation(name = "re-init Actuators", description = "re-initilizes Cibet Actuators from the config file. Overwrites any Actuator defined in code. Setpoints are also reinitialized")
   public String reinitActuators() {
      try {
         Configuration.instance().reinitActuators();
      } catch (Throwable e) {
         return exceptionToString(e, "failed to reinit Actuators: ");
      }
      return "Actuators successfully reinitialized. Setpoints have also been reinitialized";
   }

   @JMXBeanOperation(name = "re-init Controls", description = "re-initilizes Cibet Controls from the config file. Overwrites any Control defined in code. Setpoints are also reinitialized")
   public String reinitControls() {
      try {
         Configuration.instance().reinitControls(null);
      } catch (Throwable e) {
         return exceptionToString(e, "failed to reinit Controls: ");
      }
      return "Controls successfully reinitialized. Setpoints are also reinitialized";
   }

   @JMXBeanOperation(name = "re-init Setpoints", description = "re-initilizes Cibet Setpoints from the config file. Overwrites any Setpoint defined in code")
   public String reinitSetpoints() {
      try {
         Configuration.instance().reinitSetpoints(null);
      } catch (Throwable e) {
         return exceptionToString(e, "failed to reinit Setpoints: ");
      }
      return "Setpoints successfully reinitialized.";
   }

   @JMXBeanOperation(name = "start a proxy", description = "starts or restarts a proxy. System or other configured properties are not taken into account.")
   public String startProxy(
         @JMXBeanParameter(name = "proxy name", description = "a unique name for this proxy. Mandatory") String name,
         @JMXBeanParameter(name = "proxy mode", description = "proxy mode for this proxy. One of MITM or CHAINEDPROXY") String mode,
         @JMXBeanParameter(name = "proxy port", description = "port on which the proxy shall run. Mandatory") int port,
         @JMXBeanParameter(name = "buffer size", description = "Chunked requests are aggregated in a buffer. If set to 0 the default size of the buffer is taken (1024*1024)") int bufferSize,
         @JMXBeanParameter(name = "timeout", description = "timeout in ms. If set to 0 the default timeout is taken (5000 ms)") int timeout,
         @JMXBeanParameter(name = "chained proxy host", description = "host name of upstream proxy server. Optional") String chainedHost,
         @JMXBeanParameter(name = "chained proxy port", description = "port of upstream proxy server. Optional") int chainedPort,
         @JMXBeanParameter(name = "keystore name", description = "name of a keystore in classpath that contains the private key for SSL with client authentication. Optional") String clientKeystore,
         @JMXBeanParameter(name = "keystore password", description = "password for the keystore") String keystorePassword,
         @JMXBeanParameter(name = "exclude patterns", description = "a comma separated list of regex patterns for URLs that shall be excluded from sensoring") String excludes) {
      try {
         ProxyConfig proxyConfig = new ProxyConfig();
         if (name == null || name.equalsIgnoreCase("String")) {
            throw new IllegalArgumentException("name is mandatory");
         }
         proxyConfig.setName(name);
         proxyConfig.setMode(ProxyMode.valueOf(mode));
         proxyConfig.setPort(port);
         if (bufferSize != 0) {
            proxyConfig.setBufferSize(bufferSize);
         }
         if (timeout != 0) {
            proxyConfig.setTimeout(timeout);
         }
         if (chainedHost != null && !chainedHost.equalsIgnoreCase("String")) {
            proxyConfig.setChainedProxyHost(chainedHost);
            proxyConfig.setChainedProxyPort(chainedPort);
         }
         if (clientKeystore != null && !clientKeystore.equalsIgnoreCase("String")) {
            proxyConfig.setClientKeystore(clientKeystore);
            proxyConfig.setClientKeystorePassword(keystorePassword);
         }
         if (excludes != null && !excludes.equalsIgnoreCase("String")) {
            List<Pattern> patterns = new ArrayList<>();
            StringTokenizer tok = new StringTokenizer(excludes, ",");
            while (tok.hasMoreTokens()) {
               String excl = tok.nextToken();
               Pattern p = Pattern.compile(excl);
               patterns.add(p);
            }
            proxyConfig.setExcludePattern(patterns);
         }

         Configuration.instance().startProxy(proxyConfig);
         return "Proxy for HTTP-PROXY sensor successfully started. ";
      } catch (Exception e) {
         return exceptionToString(e, "failed to start proxy server for HTTP-PROXY sensor: ");
      }
   }

}
