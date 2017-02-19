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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.method.MethodSecurityMetadataSource;
import org.springframework.util.ObjectUtils;

public class CibetDelegatingMethodSecurityMetadataSource implements
      InitializingBean, MethodSecurityMetadataSource {

   private static Log log = LogFactory
         .getLog(CibetDelegatingMethodSecurityMetadataSource.class);

   private final static List<ConfigAttribute> NULL_CONFIG_ATTRIBUTE = Collections
         .emptyList();

   private MethodSecurityMetadataSource originalMetadataSource;

   private static List<MethodSecurityMetadataSource> cibetMetadataSources = new ArrayList<MethodSecurityMetadataSource>();

   private Map<DefaultCacheKey, Collection<ConfigAttribute>> attributeCache = new HashMap<DefaultCacheKey, Collection<ConfigAttribute>>();

   static {
      cibetMetadataSources
            .add(SetpointSecuredSecurityMetadataSource.instance());
      cibetMetadataSources.add(SetpointJsr250SecurityMetadataSource.instance());
      cibetMetadataSources.add(SetpointExpressionSecurityMetadataSource
            .instance());
   }

   // ~ Methods
   // ========================================================================================================

   public void afterPropertiesSet() throws Exception {
      if (originalMetadataSource instanceof InitializingBean) {
         ((InitializingBean) originalMetadataSource).afterPropertiesSet();
      }
   }

   public final Collection<ConfigAttribute> getAttributes(Object object) {
      if (object instanceof CibetMethodInvocation) {
         CibetMethodInvocation mi = (CibetMethodInvocation) object;
         return getCibetAttributes(mi);
      } else {
         return originalMetadataSource.getAttributes(object);
      }
   }

   public final boolean supports(Class<?> clazz) {
      return originalMetadataSource.supports(clazz);
   }

   public Collection<ConfigAttribute> getAttributes(Method method,
         Class<?> targetClass) {
      return originalMetadataSource.getAttributes(method, targetClass);
   }

   protected Collection<ConfigAttribute> getCibetAttributes(
         CibetMethodInvocation mi) {
      DefaultCacheKey cacheKey = new DefaultCacheKey(null, null,
            mi.getSetpointFingerprint());
      synchronized (attributeCache) {
         Collection<ConfigAttribute> cached = attributeCache.get(cacheKey);
         // Check for canonical value indicating there is no config
         // attribute,
         if (cached == NULL_CONFIG_ATTRIBUTE) {
            return null;
         }

         if (cached != null) {
            log.debug("ConfigAttributes from cache for fingerprint "
                  + mi.getSetpointFingerprint());
            return cached;
         }

         // No cached value, so query the sources to find a result
         log.debug("create new ConfigAttributes for fingerprint "
               + mi.getSetpointFingerprint());
         Collection<ConfigAttribute> attributes = null;
         for (MethodSecurityMetadataSource s : cibetMetadataSources) {
            attributes = s.getAttributes(mi);
            if (attributes != null) {
               break;
            }
         }

         // Put it in the cache.
         if (attributes == null) {
            this.attributeCache.put(cacheKey, NULL_CONFIG_ATTRIBUTE);
            return null;
         }

         if (log.isDebugEnabled()) {
            log.debug("Adding security method [" + cacheKey
                  + "] with attributes " + attributes);
         }

         this.attributeCache.put(cacheKey, attributes);

         return attributes;
      }
   }

   public Collection<ConfigAttribute> getAllConfigAttributes() {
      return originalMetadataSource.getAllConfigAttributes();
   }

   // ~ Inner Classes
   // ==================================================================================================

   private static class DefaultCacheKey {
      private final Method method;
      private final Class<?> targetClass;
      private final String setpointFingerprint;

      public DefaultCacheKey(Method method, Class<?> targetClass,
            String setpointFingerprint) {
         this.method = method;
         this.targetClass = targetClass;
         this.setpointFingerprint = setpointFingerprint;
      }

      public boolean equals(Object other) {
         if (this == other) {
            return true;
         }
         if (!(other instanceof DefaultCacheKey)) {
            return false;
         }
         DefaultCacheKey otherKey = (DefaultCacheKey) other;
         return (ObjectUtils.nullSafeEquals(this.method, otherKey.method)
               && ObjectUtils.nullSafeEquals(this.targetClass,
                     otherKey.targetClass) && ObjectUtils.nullSafeEquals(
               this.setpointFingerprint, otherKey.setpointFingerprint));
      }

      public int hashCode() {
         return (this.method != null ? this.method.hashCode() * 21 : 0)
               + (this.targetClass != null ? this.targetClass.hashCode() : 0)
               + (this.setpointFingerprint != null ? this.setpointFingerprint
                     .hashCode() : 0);
      }

      public String toString() {
         return "CacheKey["
               + (targetClass == null ? "-" : targetClass.getName()) + "; "
               + method + "; SP: " + setpointFingerprint + "]";
      }
   }

   /**
    * @return the originalMetadataSource
    */
   public MethodSecurityMetadataSource getOriginalMetadataSource() {
      return originalMetadataSource;
   }

   /**
    * @param originalMetadataSource
    *           the originalMetadataSource to set
    */
   public void setOriginalMetadataSource(
         MethodSecurityMetadataSource originalMetadataSource) {
      this.originalMetadataSource = originalMetadataSource;
   }

}
