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
package com.logitags.cibet.sensor.ejb;

import java.lang.reflect.Proxy;
import java.util.Hashtable;
import java.util.Map.Entry;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.spi.NamingManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CibetRemoteContext implements Context {

   private Log log = LogFactory.getLog(CibetRemoteContext.class);

   public static final String NATIVE_INITIAL_CONTEXT_FACTORY = "com.logitags.cibet.naming.factory.initial";
   public static final String NATIVE_URL_PKG_PREFIXES = "com.logitags.cibet.java.naming.factory.url.pkgs";

   private Hashtable<?, ?> environment;

   private Hashtable<String, Object> nativeEnv;

   private Context defaultNativeContext;

   public CibetRemoteContext(Hashtable<?, ?> env) throws NamingException {
      if (env == null) {
         throw new IllegalArgumentException(
               "Failed to create CibetRemoteContext: Constructor parameter env must not be null");
      }
      environment = env;

      nativeEnv = (Hashtable<String, Object>) env.clone();
      String nativeContextClassname = (String) nativeEnv.get(NATIVE_INITIAL_CONTEXT_FACTORY);
      if (nativeContextClassname != null) {
         log.info("create native initial context with " + nativeContextClassname);
         nativeEnv.put(Context.INITIAL_CONTEXT_FACTORY, nativeContextClassname);
      }

      String nativePrefixes = (String) environment.get(CibetRemoteContext.NATIVE_URL_PKG_PREFIXES);
      if (nativePrefixes != null) {
         nativeEnv.put(Context.URL_PKG_PREFIXES, nativePrefixes);
      }

      if (log.isDebugEnabled()) {
         for (Entry<?, ?> e : nativeEnv.entrySet()) {
            log.debug(e);
         }
      }

      defaultNativeContext = NamingManager.getInitialContext(nativeEnv);
      log.debug("CibetRemoteContext initialised");
   }

   @Override
   public Object lookup(Name name) throws NamingException {
      log.warn("lookup with " + name);

      Context ctx = getNativeURLOrDefaultInitCtx(name);

      Object original = ctx.lookup(name);
      Class<?> clazz = original.getClass();
      log.debug("make proxy from: " + original);
      Object proxy = Proxy.newProxyInstance(clazz.getClassLoader(), clazz.getInterfaces(),
            new RemoteEjbInvocationHandler(original, environment, name));
      return proxy;
   }

   @Override
   public Object lookup(String name) throws NamingException {
      log.warn("lookup with " + name);

      Context ctx = getNativeURLOrDefaultInitCtx(name);

      Object original = ctx.lookup(name);
      Class<?> clazz = original.getClass();
      Class[] interfaces = clazz.getInterfaces();
      for (Class intf : interfaces) {
         log.debug("interface: " + intf);
      }

      log.debug("make proxy from: " + original);
      Object proxy = Proxy.newProxyInstance(clazz.getClassLoader(), clazz.getInterfaces(),
            new RemoteEjbInvocationHandler(original, environment, name));
      return proxy;
   }

   protected Context getNativeURLOrDefaultInitCtx(Name name) throws NamingException {
      if (NamingManager.hasInitialContextFactoryBuilder()) {
         return defaultNativeContext;
      }
      if (name.size() > 0) {
         String first = name.get(0);
         String scheme = getURLScheme(first);
         if (scheme != null) {
            Context ctx = NamingManager.getURLContext(scheme, nativeEnv);
            if (ctx != null) {
               return ctx;
            }
         }
      }
      return defaultNativeContext;
   }

   protected Context getNativeURLOrDefaultInitCtx(String name) throws NamingException {
      if (NamingManager.hasInitialContextFactoryBuilder()) {
         return defaultNativeContext;
      }
      String scheme = getURLScheme(name);
      if (scheme != null) {
         Context ctx = NamingManager.getURLContext(scheme, nativeEnv);
         if (ctx != null) {
            return ctx;
         }
      }
      return defaultNativeContext;
   }

   private String getURLScheme(String str) {
      int colon_posn = str.indexOf(':');
      int slash_posn = str.indexOf('/');

      if (colon_posn > 0 && (slash_posn == -1 || colon_posn < slash_posn)) return str.substring(0, colon_posn);
      return null;
   }

   @Override
   public void bind(Name name, Object obj) throws NamingException {
      defaultNativeContext.bind(name, obj);
   }

   @Override
   public void bind(String name, Object obj) throws NamingException {
      defaultNativeContext.bind(name, obj);
   }

   @Override
   public void rebind(Name name, Object obj) throws NamingException {
      defaultNativeContext.rebind(name, obj);
   }

   @Override
   public void rebind(String name, Object obj) throws NamingException {
      defaultNativeContext.rebind(name, obj);
   }

   @Override
   public void unbind(Name name) throws NamingException {
      defaultNativeContext.unbind(name);
   }

   @Override
   public void unbind(String name) throws NamingException {
      defaultNativeContext.unbind(name);
   }

   @Override
   public void rename(Name oldName, Name newName) throws NamingException {
      defaultNativeContext.rename(oldName, newName);
   }

   @Override
   public void rename(String oldName, String newName) throws NamingException {
      defaultNativeContext.rename(oldName, newName);
   }

   @Override
   public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
      return defaultNativeContext.list(name);
   }

   @Override
   public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
      return defaultNativeContext.list(name);
   }

   @Override
   public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
      return defaultNativeContext.listBindings(name);
   }

   @Override
   public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
      return defaultNativeContext.listBindings(name);
   }

   @Override
   public void destroySubcontext(Name name) throws NamingException {
      defaultNativeContext.destroySubcontext(name);
   }

   @Override
   public void destroySubcontext(String name) throws NamingException {
      defaultNativeContext.destroySubcontext(name);
   }

   @Override
   public Context createSubcontext(Name name) throws NamingException {
      return defaultNativeContext.createSubcontext(name);
   }

   @Override
   public Context createSubcontext(String name) throws NamingException {
      return defaultNativeContext.createSubcontext(name);
   }

   @Override
   public Object lookupLink(Name name) throws NamingException {
      return defaultNativeContext.lookupLink(name);
   }

   @Override
   public Object lookupLink(String name) throws NamingException {
      return defaultNativeContext.lookupLink(name);
   }

   @Override
   public NameParser getNameParser(Name name) throws NamingException {
      return defaultNativeContext.getNameParser(name);
   }

   @Override
   public NameParser getNameParser(String name) throws NamingException {
      return defaultNativeContext.getNameParser(name);
   }

   @Override
   public Name composeName(Name name, Name prefix) throws NamingException {
      return defaultNativeContext.composeName(name, prefix);
   }

   @Override
   public String composeName(String name, String prefix) throws NamingException {
      return defaultNativeContext.composeName(name, prefix);
   }

   @Override
   public Object addToEnvironment(String propName, Object propVal) throws NamingException {
      return defaultNativeContext.addToEnvironment(propName, propVal);
   }

   @Override
   public Object removeFromEnvironment(String propName) throws NamingException {
      return defaultNativeContext.removeFromEnvironment(propName);
   }

   @Override
   public Hashtable<?, ?> getEnvironment() throws NamingException {
      return environment;
   }

   @Override
   public void close() throws NamingException {
      defaultNativeContext.close();
   }

   @Override
   public String getNameInNamespace() throws NamingException {
      return defaultNativeContext.getNameInNamespace();
   }

}
