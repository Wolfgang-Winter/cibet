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

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.littleshoot.proxy.MitmManager;

import io.netty.handler.codec.http.HttpRequest;

public class CibetMitmManager implements MitmManager {

   private static Log log = LogFactory.getLog(CibetMitmManager.class);

   private static final String CIBETKEYSTORE = "mitmKeystore.jks";
   private static final String PASSWORD = "cibet";

   private CibetSslEngineSource serverSsl;

   public CibetMitmManager() {
      serverSsl = new CibetSslEngineSource();
   }

   public CibetMitmManager(String clientKeystore, String password) {
      serverSsl = new CibetSslEngineSource(clientKeystore, password);
   }

   @Override
   public SSLEngine serverSslEngine(String peerHost, int peerPort) {
      log.debug("call serverSslEngine to " + peerHost + ":" + peerPort);
      return serverSsl.newSslEngine(peerHost, peerPort);
   }

   @Override
   public SSLEngine clientSslEngineFor(HttpRequest httpRequest, SSLSession serverSslSession) {
      log.debug("call clientSslEngineFor for " + serverSslSession);
      CibetSslEngineSource clientSource = new CibetSslEngineSource(CIBETKEYSTORE, PASSWORD);
      return clientSource.newSslEngine();
   }

   @Override
   public SSLEngine serverSslEngine() {
      log.debug("call serverSslEngine");
      return serverSsl.newSslEngine();
   }

}
