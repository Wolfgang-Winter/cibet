/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2016 Dr. Wolfgang Winter
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

import java.io.Serializable;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

public class HttpRequestData implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private String serverName;

   private String remoteAddr;

   private String queryString = "";

   private String requestURI = "";

   private String scheme;

   private String contextPath;

   private String pathInfo;

   private StringBuffer requestURL;

   private String servletPath;

   public HttpRequestData(String host, String queryString, String uri) {
      int index = host.indexOf(":");
      if (index == -1) {
         this.serverName = host;
      } else {
         this.serverName = host.substring(0, index);
      }

      remoteAddr = "localhost";

      scheme = "https";
      if (uri.startsWith("http:") || uri.startsWith("https:")) {
         index = uri.indexOf("://");
         scheme = uri.substring(0, index);
         uri = uri.substring(index + 3);
      }

      this.queryString = queryString;
      requestURI = uri;
      index = uri.indexOf("/", 1);
      if (index == -1) {
         contextPath = uri;
         servletPath = "";
      } else {
         contextPath = uri.substring(0, index);
         servletPath = uri.substring(index);
      }

      requestURL = new StringBuffer(scheme + "://" + host + "/" + uri);
   }

   public HttpRequestData(ServletRequest orig) {
      serverName = orig.getServerName();
      remoteAddr = orig.getRemoteAddr();
      scheme = orig.getScheme();
      if (orig instanceof HttpServletRequest) {
         HttpServletRequest h = (HttpServletRequest) orig;
         queryString = h.getQueryString();
         requestURI = h.getRequestURI();
         contextPath = h.getContextPath();
         pathInfo = h.getPathInfo();
         requestURL = h.getRequestURL();
         servletPath = h.getServletPath();
      }
   }

   public String getServerName() {
      return serverName;
   }

   public String getRemoteAddr() {
      return remoteAddr;
   }

   public String getQueryString() {
      return queryString;
   }

   public String getRequestURI() {
      return requestURI;
   }

   public String getScheme() {
      return scheme;
   }

   public String getContextPath() {
      return contextPath;
   }

   public String getPathInfo() {
      return pathInfo;
   }

   public StringBuffer getRequestURL() {
      return requestURL;
   }

   public String getServletPath() {
      return servletPath;
   }

}
