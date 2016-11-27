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

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;

import com.logitags.cibet.config.ProxyConfig;

public class CibetHttpFiltersSource extends HttpFiltersSourceAdapter {

   private static Log log = LogFactory.getLog(CibetHttpFiltersSource.class);

   private ProxyConfig proxyConfig;

   public CibetHttpFiltersSource(ProxyConfig config) {
      proxyConfig = config;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.littleshoot.proxy.HttpFiltersSourceAdapter#filterRequest(io.netty.handler.codec.http.HttpRequest,
    * io.netty.channel.ChannelHandlerContext)
    */
   @Override
   public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
      return new CibetProxy(originalRequest, ctx, proxyConfig);
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.littleshoot.proxy.HttpFiltersSourceAdapter#getMaximumRequestBufferSizeInBytes()
    */
   @Override
   public int getMaximumRequestBufferSizeInBytes() {
      return proxyConfig.getBufferSize();
   }

}
