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
package com.logitags.cibet.sensor.http;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.littleshoot.proxy.SslEngineSource;

public class CibetSslEngineSource implements SslEngineSource {

   private static Log log = LogFactory.getLog(CibetSslEngineSource.class);

   private SSLContext sslContext;

   private String clientKeystore;

   private String password;

   public CibetSslEngineSource() {
      init();
   }

   public CibetSslEngineSource(String clientKeystore, String password) {
      this.clientKeystore = clientKeystore;
      this.password = password;
      init();
   }

   @Override
   public SSLEngine newSslEngine() {
      SSLEngine sslEngine = sslContext.createSSLEngine();
      return sslEngine;
   }

   @Override
   public SSLEngine newSslEngine(String peerHost, int peerPort) {
      SSLEngine sslEngine = sslContext.createSSLEngine(peerHost, peerPort);
      return sslEngine;
   }

   private void init() {
      log.debug("init CibetSslEngineSource");
      TrustManager[] trustManagers = new TrustManager[] { new X509TrustManager() {
         // TrustManager that trusts all servers
         @Override
         public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
            log.debug("call checkClientTrusted");
         }

         @Override
         public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
            log.debug("call checkServerTrusted");
         }

         @Override
         public X509Certificate[] getAcceptedIssuers() {
            log.debug("call getAcceptedIssuers()");
            return null;
         }
      } };

      try {
         KeyManager[] keymanagers = null;
         if (clientKeystore != null) {
            if (password == null) {
               throw new IllegalArgumentException("password of keystore " + clientKeystore + " is null");
            }

            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            KeyManagerFactory keyManagerFactory = null;
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(loader.getResourceAsStream(clientKeystore), password.toCharArray());
            keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, password.toCharArray());
            keymanagers = keyManagerFactory.getKeyManagers();
         }

         sslContext = SSLContext.getInstance("TLS");
         sslContext.init(keymanagers, trustManagers, null);
         log.debug("SslContext initialised");
      } catch (GeneralSecurityException | IOException e) {
         log.error(e.getMessage(), e);
         throw new RuntimeException(e);
      }
   }

}
