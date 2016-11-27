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
package com.logitags.cibet.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Configuration for the proxy used as HTTP(CLIENT) sensor.
 * 
 * @author Wolfgang
 * 
 */
public class ProxyConfig implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public static final String PROXYCONFIG = "PROXYCONFIG";

   public enum ProxyMode {
      /**
       * Proxy is started as Man-in-the-middle. SSL requests can be sniffed but no upstream proxy can be addressed. This
       * should be the default for https
       */
      MITM,

      /**
       * Proxy is started in proxy mode. SSL requests are opaque. An upstream proxy can be configured.
       */
      CHAINEDPROXY,

      /**
       * no proxy is started
       */
      NO_PROXY,

   }

   private String name;

   private int port = 8078;

   private ProxyMode mode = ProxyMode.NO_PROXY;

   private int bufferSize = 1024 * 1024;

   private String chainedProxyHost;

   private int chainedProxyPort = 0;

   private String clientKeystore;

   private String clientKeystorePassword;

   private List<Pattern> excludePattern = new ArrayList<>();

   private String cibetEEContextEJBJndiName;

   /**
    * the timeout for connecting to the upstream server on a new connection, in milliseconds. Default 5000
    */
   private int timeout = 5000;

   /**
    * Proxy port. Default is 8078
    * 
    * @return the port
    */
   public int getPort() {
      return port;
   }

   /**
    * @param port
    *           the port to set
    */
   public void setPort(int port) {
      this.port = port;
   }

   /**
    * The mode the proxy is started in.
    * 
    * @return the mode
    */
   public ProxyMode getMode() {
      return mode;
   }

   /**
    * @param mode
    *           the mode to set
    */
   public void setMode(ProxyMode mode) {
      this.mode = mode;
   }

   /**
    * Chunked requests are aggregated in a buffer. The default size of the buffer is 1024*1024
    * 
    * @return the bufferSize
    */
   public int getBufferSize() {
      return bufferSize;
   }

   /**
    * @param bufferSize
    *           the bufferSize to set
    */
   public void setBufferSize(int bufferSize) {
      this.bufferSize = bufferSize;
   }

   /**
    * optional host name of an upstream proxy
    * 
    * @return the chainedProxyHost
    */
   public String getChainedProxyHost() {
      return chainedProxyHost;
   }

   /**
    * @param chainedProxyHost
    *           the chainedProxyHost to set
    */
   public void setChainedProxyHost(String chainedProxyHost) {
      this.chainedProxyHost = chainedProxyHost;
   }

   /**
    * optional port of an upstream proxy
    * 
    * @return the chainedProxyPort
    */
   public int getChainedProxyPort() {
      return chainedProxyPort;
   }

   /**
    * @param chainedProxyPort
    *           the chainedProxyPort to set
    */
   public void setChainedProxyPort(int chainedProxyPort) {
      this.chainedProxyPort = chainedProxyPort;
   }

   /**
    * optional name of a keystore in the classpath for SSL with client certificate authentication
    * 
    * @return the clientKeystore
    */
   public String getClientKeystore() {
      return clientKeystore;
   }

   /**
    * @param clientKeystore
    *           the clientKeystore to set
    */
   public void setClientKeystore(String clientKeystore) {
      this.clientKeystore = clientKeystore;
   }

   /**
    * optional password of a keystore in the classpath for SSL with client certificate authentication
    * 
    * @return the clientKeystorePassword
    */
   public String getClientKeystorePassword() {
      return clientKeystorePassword;
   }

   /**
    * @param clientKeystorePassword
    *           the clientKeystorePassword to set
    */
   public void setClientKeystorePassword(String clientKeystorePassword) {
      this.clientKeystorePassword = clientKeystorePassword;
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("ProxyConfig with mode ");
      b.append(mode);
      b.append(", name: ");
      b.append(name);
      b.append(", port: ");
      b.append(port);
      b.append(", timeout: ");
      b.append(timeout);
      b.append(", buffer size: ");
      b.append(bufferSize);
      if (chainedProxyHost != null) {
         b.append(", chained proxy: ");
         b.append(chainedProxyHost);
         b.append(": ");
         b.append(chainedProxyPort);
      }
      b.append(", buffer size: ");
      b.append(bufferSize);
      if (clientKeystore != null) {
         b.append(", client keystore: ");
         b.append(clientKeystore);
      }
      return b.toString();
   }

   /**
    * a list of regex patterns for URLs that shall be excluded from sensoring.
    * 
    * @return the excludePattern
    */
   public List<Pattern> getExcludePattern() {
      return excludePattern;
   }

   /**
    * @param excludePattern
    *           the excludePattern to set
    */
   public void setExcludePattern(List<Pattern> excludePattern) {
      this.excludePattern = excludePattern;
   }

   /**
    * JNDI name of CibetEEContextEJB. Must be set only if Cibet can not detect it by itself.
    * 
    * @return the cibetEEContextEJBJndiName
    */
   public String getCibetEEContextEJBJndiName() {
      return cibetEEContextEJBJndiName;
   }

   /**
    * @param cibetEEContextEJBJndiName
    *           the cibetEEContextEJBJndiName to set
    */
   public void setCibetEEContextEJBJndiName(String cibetEEContextEJBJndiName) {
      this.cibetEEContextEJBJndiName = cibetEEContextEJBJndiName;
   }

   /**
    * @return the timeout
    */
   public int getTimeout() {
      return timeout;
   }

   /**
    * @param timeout
    *           the timeout to set
    */
   public void setTimeout(int timeout) {
      this.timeout = timeout;
   }

   /**
    * a unique name for this proxy
    * 
    * @return the name
    */
   public String getName() {
      return name;
   }

   /**
    * a unique name for this proxy
    * 
    * @param name
    *           the name to set
    */
   public void setName(String name) {
      this.name = name;
   }

}
