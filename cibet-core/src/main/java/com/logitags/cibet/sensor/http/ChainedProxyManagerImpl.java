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

import io.netty.handler.codec.http.HttpRequest;

import java.util.Queue;

import org.littleshoot.proxy.ChainedProxy;
import org.littleshoot.proxy.ChainedProxyManager;

public class ChainedProxyManagerImpl implements ChainedProxyManager {

   private String proxyHost;

   private int proxyPort;

   public ChainedProxyManagerImpl(String host, int port) {
      proxyHost = host;
      proxyPort = port;
   }

   @Override
   public void lookupChainedProxies(HttpRequest httpRequest, Queue<ChainedProxy> chainedProxies) {
      ProxyAdapter proxy = new ProxyAdapter(proxyHost, proxyPort);
      chainedProxies.add(proxy);
   }

}
