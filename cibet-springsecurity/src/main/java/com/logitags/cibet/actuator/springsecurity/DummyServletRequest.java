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
package com.logitags.cibet.actuator.springsecurity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import com.logitags.cibet.resource.HttpRequestData;

/**
 * A serializable wrapper of the HttpServletRequest
 */
public class DummyServletRequest implements Serializable, HttpServletRequest {

   /**
    * 
    */
   private static final long serialVersionUID = -7782292252291989953L;

   public static class IteratorEnumeration implements Enumeration {

      private Iterator<?> iter;

      public IteratorEnumeration(Iterator<?> it) {
         iter = it;
      }

      @Override
      public boolean hasMoreElements() {
         return iter.hasNext();
      }

      @Override
      public Object nextElement() {
         return iter.next();
      }
   }

   private String serverName;

   private String remoteAddr;

   private String queryString = "";

   private String requestURI = "";

   private String scheme;

   private String contextPath;

   private String pathInfo;

   private StringBuffer requestURL;

   private String servletPath;

   public DummyServletRequest(HttpRequestData data, HttpServletRequest orig) {
      if (data != null) {
         serverName = data.getServerName();
         remoteAddr = data.getRemoteAddr();
         scheme = data.getScheme();
         queryString = data.getQueryString();
         requestURI = data.getRequestURI();
         contextPath = data.getContextPath();
         pathInfo = data.getPathInfo();
         requestURL = data.getRequestURL();
         servletPath = data.getServletPath();
      } else {
         serverName = orig.getServerName();
         remoteAddr = orig.getRemoteAddr();
         scheme = orig.getScheme();
         queryString = orig.getQueryString();
         requestURI = orig.getRequestURI();
         contextPath = orig.getContextPath();
         pathInfo = orig.getPathInfo();
         requestURL = orig.getRequestURL();
         servletPath = orig.getServletPath();
      }
   }

   @Override
   public Object getAttribute(String name) {
      return null;
   }

   @Override
   public Enumeration<String> getAttributeNames() {
      return new IteratorEnumeration(new LinkedList().iterator());
   }

   @Override
   public String getCharacterEncoding() {
      return null;
   }

   @Override
   public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
   }

   @Override
   public int getContentLength() {
      return 0;
   }

   @Override
   public String getContentType() {
      return null;
   }

   @Override
   public ServletInputStream getInputStream() throws IOException {
      return null;
   }

   @Override
   public String getParameter(String name) {
      return null;
   }

   @Override
   public Enumeration<String> getParameterNames() {
      return new IteratorEnumeration(new LinkedList().iterator());
   }

   @Override
   public String[] getParameterValues(String name) {
      return null;
   }

   @Override
   public Map<String, String[]> getParameterMap() {
      return new HashMap<String, String[]>();
   }

   @Override
   public String getProtocol() {
      return null;
   }

   @Override
   public String getScheme() {
      return scheme;
   }

   @Override
   public String getServerName() {
      return serverName;
   }

   @Override
   public int getServerPort() {
      return 0;
   }

   @Override
   public BufferedReader getReader() throws IOException {
      return null;
   }

   @Override
   public String getRemoteAddr() {
      return remoteAddr;
   }

   @Override
   public String getRemoteHost() {
      return null;
   }

   @Override
   public void setAttribute(String name, Object o) {
   }

   @Override
   public void removeAttribute(String name) {
   }

   @Override
   public Locale getLocale() {
      return null;
   }

   @Override
   public Enumeration<Locale> getLocales() {
      return new IteratorEnumeration(new LinkedList().iterator());
   }

   @Override
   public boolean isSecure() {
      return false;
   }

   @Override
   public RequestDispatcher getRequestDispatcher(String path) {
      return null;
   }

   @Override
   public String getRealPath(String path) {
      return null;
   }

   @Override
   public int getRemotePort() {
      return 0;
   }

   @Override
   public String getLocalName() {
      return null;
   }

   @Override
   public String getLocalAddr() {
      return null;
   }

   @Override
   public int getLocalPort() {
      return 0;
   }

   @Override
   public ServletContext getServletContext() {
      return null;
   }

   @Override
   public AsyncContext startAsync() throws IllegalStateException {
      return null;
   }

   @Override
   public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
         throws IllegalStateException {
      return null;
   }

   @Override
   public boolean isAsyncStarted() {
      return false;
   }

   @Override
   public boolean isAsyncSupported() {
      return false;
   }

   @Override
   public AsyncContext getAsyncContext() {
      return null;
   }

   @Override
   public DispatcherType getDispatcherType() {
      return null;
   }

   @Override
   public String getAuthType() {
      return null;
   }

   @Override
   public Cookie[] getCookies() {
      return null;
   }

   @Override
   public long getDateHeader(String name) {
      return 0;
   }

   @Override
   public String getHeader(String name) {
      return null;
   }

   @Override
   public Enumeration<String> getHeaders(String name) {
      return new IteratorEnumeration(new LinkedList().iterator());
   }

   @Override
   public Enumeration<String> getHeaderNames() {
      return new IteratorEnumeration(new LinkedList().iterator());
   }

   @Override
   public int getIntHeader(String name) {
      return 0;
   }

   @Override
   public String getMethod() {
      return null;
   }

   @Override
   public String getPathInfo() {
      return pathInfo;
   }

   @Override
   public String getPathTranslated() {
      return null;
   }

   @Override
   public String getContextPath() {
      return contextPath;
   }

   @Override
   public String getQueryString() {
      return queryString;
   }

   @Override
   public String getRemoteUser() {
      return null;
   }

   @Override
   public boolean isUserInRole(String role) {
      return false;
   }

   @Override
   public Principal getUserPrincipal() {
      return null;
   }

   @Override
   public String getRequestedSessionId() {
      return null;
   }

   @Override
   public String getRequestURI() {
      return requestURI;
   }

   @Override
   public StringBuffer getRequestURL() {
      return requestURL;
   }

   @Override
   public String getServletPath() {
      return servletPath;
   }

   @Override
   public HttpSession getSession(boolean create) {
      return null;
   }

   @Override
   public HttpSession getSession() {
      return null;
   }

   @Override
   public boolean isRequestedSessionIdValid() {
      return false;
   }

   @Override
   public boolean isRequestedSessionIdFromCookie() {
      return false;
   }

   @Override
   public boolean isRequestedSessionIdFromURL() {
      return false;
   }

   @Override
   public boolean isRequestedSessionIdFromUrl() {
      return false;
   }

   @Override
   public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
      return false;
   }

   @Override
   public void login(String username, String password) throws ServletException {
   }

   @Override
   public void logout() throws ServletException {
   }

   @Override
   public Collection<Part> getParts() throws IOException, ServletException {
      return null;
   }

   @Override
   public Part getPart(String name) throws IOException, ServletException {
      return null;
   }

}
