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
package com.logitags.cibet.sensor.jpa;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.spi.LoadState;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.logitags.cibet.context.EntityManagerType;

@RunWith(MockitoJUnitRunner.class)
public class JpaTest {

   private static Logger log = Logger.getLogger(JpaTest.class);

   @Mock
   protected EntityManager em;

   @Mock
   protected EntityManagerFactory emfMock;

   @Mock
   private PersistenceProvider ppMock;

   @Mock
   PersistenceUnitInfo puiMock;

   @Test(expected = IllegalArgumentException.class)
   public void createCibetEntityManagerFactory() {
      new CibetEntityManagerFactory(null, true, null);
   }

   @Test
   public void createEntityManager() {
      Mockito.when(emfMock.createEntityManager()).thenReturn(em);

      EntityManagerFactory emf = new CibetEntityManagerFactory(emfMock, true, EntityManagerType.RESOURCE_LOCAL);
      EntityManager eman = emf.createEntityManager();
      Assert.assertTrue(eman instanceof CibetEntityManager);
   }

   @Test
   public void createEntityManagerMap() {
      Map arg0 = null;
      Mockito.when(emfMock.createEntityManager(arg0)).thenReturn(em);

      EntityManagerFactory emf = new CibetEntityManagerFactory(emfMock, true, EntityManagerType.RESOURCE_LOCAL);
      EntityManager eman = emf.createEntityManager(arg0);
      Assert.assertTrue(eman instanceof CibetEntityManager);
   }

   @Test
   public void getProviderUtil() {
      Provider p = new Provider();
      ProviderUtil pu = p.getProviderUtil();
      Assert.assertEquals(LoadState.UNKNOWN, pu.isLoaded(null));
   }

   @Test
   public void createContainerEntityManagerFactory() {
      Map<String, Object> map = new HashMap<String, Object>();
      map.put("com.logitags.cibet.persistence.provider", "com.logitags.cibet.sensor.jpa.CibetTestPersistenceProvider");
      map.put("EMF", emfMock);
      Provider p = new Provider();
      CibetEntityManagerFactory emf = (CibetEntityManagerFactory) p.createContainerEntityManagerFactory(puiMock, map);
      Assert.assertNotNull(emf);
      log.debug("EMF: " + emf.getNativeEntityManagerFactory().getClass());
      Assert.assertNotNull(emf.getNativeEntityManagerFactory());
   }

   @Test(expected = PersistenceException.class)
   public void createContainerEntityManagerFactoryNoPP() {
      Map<String, Object> map = new HashMap<String, Object>();
      map.put("EMF", emfMock);
      Provider p = new Provider();
      p.createContainerEntityManagerFactory(puiMock, map);
   }

   @Test(expected = PersistenceException.class)
   public void createContainerEntityManagerFactorySameProvider() {
      Map<String, Object> map = new HashMap<String, Object>();
      map.put("com.logitags.cibet.persistence.provider", Provider.class.getName());
      map.put("EMF", emfMock);
      Provider p = new Provider();
      p.createContainerEntityManagerFactory(puiMock, map);

   }

   @Test
   public void createContainerEntityManagerFactoryFromPUI() {
      Properties props = new Properties();
      props.put("com.logitags.cibet.persistence.provider",
            "com.logitags.cibet.sensor.jpa.CibetTestPersistenceProvider");
      Mockito.when(puiMock.getProperties()).thenReturn(props);

      Map<String, Object> map = new HashMap<String, Object>();
      map.put("EMF", emfMock);
      Provider p = new Provider();
      CibetEntityManagerFactory emf = (CibetEntityManagerFactory) p.createContainerEntityManagerFactory(puiMock, map);
      Assert.assertNotNull(emf);
      log.debug("EMF: " + emf.getNativeEntityManagerFactory().getClass());
      Assert.assertNotNull(emf.getNativeEntityManagerFactory());
   }

   @Test
   public void createContainerEntityManagerFactoryFromPUI2() {
      Mockito.when(puiMock.getPersistenceProviderClassName())
            .thenReturn("com.logitags.cibet.sensor.jpa.CibetTestPersistenceProvider");

      Map<String, Object> map = new HashMap<String, Object>();
      map.put("EMF", emfMock);
      Provider p = new Provider();
      CibetEntityManagerFactory emf = (CibetEntityManagerFactory) p.createContainerEntityManagerFactory(puiMock, map);
      Assert.assertNotNull(emf);
      log.debug("EMF: " + emf.getNativeEntityManagerFactory().getClass());
      Assert.assertNotNull(emf.getNativeEntityManagerFactory());
   }

}
