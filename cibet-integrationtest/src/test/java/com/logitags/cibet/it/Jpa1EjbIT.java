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
package com.logitags.cibet.it;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.persistence.LockModeType;

import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.cibethelper.base.CoreTestBase;
import com.cibethelper.ejb.CibetTestEJB;
import com.cibethelper.ejb.RemoteEJB;
import com.cibethelper.ejb.RemoteEJBImpl;
import com.cibethelper.ejb.SecuredRemoteEJBImpl;
import com.cibethelper.ejb.SimpleEjb;
import com.cibethelper.entities.AbstractTEntity;
import com.cibethelper.entities.ITComplexEntity;
import com.cibethelper.entities.TCompareEntity;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TComplexEntity2;
import com.cibethelper.entities.TEntity;
import com.cibethelper.servlet.ArquillianTestServlet1;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.actuator.common.InvalidUserException;
import com.logitags.cibet.actuator.common.PostponedEjbException;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.actuator.dc.FourEyesActuator;
import com.logitags.cibet.actuator.dc.UnapprovedResourceException;
import com.logitags.cibet.actuator.info.InfoLogActuator;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.ConfigurationService;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.jpa.JpaResource;

@RunWith(Arquillian.class)
public class Jpa1EjbIT extends AbstractArquillian {

   private static Logger log = Logger.getLogger(Jpa1EjbIT.class);

   @EJB
   private CibetTestEJB ejb;

   private Setpoint sp = null;

   @Deployment
   public static WebArchive createDeployment() {
      String warName = Jpa1EjbIT.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("it/web2.xml");

      archive.addClasses(AbstractArquillian.class, CoreTestBase.class, AbstractTEntity.class, TEntity.class,
            TComplexEntity.class, TComplexEntity2.class, ITComplexEntity.class, TCompareEntity.class,
            CibetTestEJB.class, ArquillianTestServlet1.class, RemoteEJB.class, RemoteEJBImpl.class,
            SecuredRemoteEJBImpl.class, SimpleEjb.class);

      File[] cibet = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-jpa").withTransitivity()
            .asFile();
      archive.addAsLibraries(cibet);

      archive.addAsWebInfResource("META-INF/persistence-it.xml", "classes/META-INF/persistence.xml");
      archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

      log.debug(archive.toString(true));
      archive.as(ZipExporter.class).exportTo(new File("target/" + warName), true);

      return archive;
   }

   @Before
   public void beforeJpa1EjbIT() {
      log.debug("execute before()");
      new ConfigurationService().initialise();
      Context.start();
      Context.sessionScope().setUser(USER);
      Context.sessionScope().setTenant(TENANT);
      log.debug("end execute before()");
   }

   @After
   public void afterJpa1EjbIT() {
      Context.end();
      if (sp != null) {
         Configuration.instance().unregisterSetpoint(sp.getId());
      }
   }

   private class TEntityComparator implements Comparator<TEntity> {
      @Override
      public int compare(TEntity o1, TEntity o2) {
         return o1.getCounter() - o2.getCounter();
      }
   }

   private TEntityComparator comparator = new TEntityComparator();

   private void persistAndCheck(CibetTestEJB ejb) {
      TEntity entity = createTEntity(5, "valuexx");
      entity = ejb.persist(entity);

      TEntity selEnt = ejb.findTEntity(entity.getId());
      Assert.assertNotNull(selEnt);
      Assert.assertEquals(5, selEnt.getCounter());

      List<Archive> list = ejb.queryArchive(TENANT, TEntity.class.getName(), String.valueOf(entity.getId()));
      Assert.assertEquals(1, list.size());
   }

   private void checkLists(TComplexEntity decObj) {
      TEntity[] eagers = decObj.getEagerList().toArray(new TEntity[] {});
      Arrays.sort(eagers, comparator);
      TEntity[] lazies = decObj.getLazyList().toArray(new TEntity[] {});
      Arrays.sort(lazies, comparator);

      Assert.assertEquals(6, eagers[0].getCounter());
      Assert.assertEquals(7, eagers[1].getCounter());
      Assert.assertEquals(4, lazies[0].getCounter());
      Assert.assertEquals(5, lazies[1].getCounter());
      Assert.assertEquals(6, lazies[2].getCounter());
      Assert.assertEquals(3, decObj.getTen().getCounter());
   }

   @Test
   public void persistWithArchiveAndInfoLog() throws Exception {
      log.info("start persistWithArchiveAndInfoLog()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(InfoLogActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE);

      persistAndCheck(ejb);
   }

   @Test
   public void persistWithArchive() throws Exception {
      log.info("start persistWithArchive()");

      sp = registerSetpoint(TEntity.class.getName(), ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT,
            ControlEvent.UPDATE, ControlEvent.DELETE);

      persistAndCheck(ejb);
   }

   @Test
   public void persistNoControl() throws Exception {
      log.info("start persistNoControl()");
      // if (skip) return;
      TEntity entity = createTEntity(5, "valueX");
      entity = ejb.persist(entity);

      TEntity selEnt = ejb.findTEntity(entity.getId());
      Assert.assertNotNull(selEnt);
      Assert.assertEquals(5, selEnt.getCounter());
   }

   @Test
   public void persistWith4Eyes() throws Exception {
      log.info("start persistWith4Eyes()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE);

      TEntity entity = createTEntity(5, "valueY");
      entity = ejb.persist(entity);

      Assert.assertEquals(0, entity.getId());
      TEntity selEnt = ejb.findTEntity(entity.getId());
      Assert.assertNull(selEnt);
      List<Archive> list = ejb.queryArchiveByTenant();
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(ControlEvent.INSERT, list.get(0).getControlEvent());
      JpaResource res = (JpaResource) list.get(0).getResource();
      Assert.assertEquals("0", res.getPrimaryKeyId());

      List<Controllable> list1 = ejb.queryControllable();
      Assert.assertEquals(1, list1.size());
      Assert.assertEquals(ControlEvent.INSERT, list1.get(0).getControlEvent());
   }

   @Test
   public void removeWith4Eyes() throws Exception {
      log.info("start removeWith4Eyes()");
      TEntity entity = createTEntity(5, "valc");
      entity = ejb.persist(entity);

      TEntity selEnt = ejb.findTEntity(entity.getId());
      Assert.assertNotNull(selEnt);
      Assert.assertEquals(5, selEnt.getCounter());

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE);

      ejb.remove(entity);

      selEnt = ejb.findTEntity(entity.getId());
      Assert.assertNotNull(selEnt);
      List<Archive> list = ejb.queryArchiveByTenant();
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(ControlEvent.DELETE, list.get(0).getControlEvent());

      List<Controllable> list1 = ejb.queryControllable();
      Assert.assertEquals(1, list1.size());
      Assert.assertEquals(ControlEvent.DELETE, list1.get(0).getControlEvent());
   }

   @Test
   public void updateWithArchive() throws Exception {
      log.info("start updateWithArchive()");

      TEntity entity = createTEntity(6, "valm");
      entity = ejb.persist(entity);

      TEntity selEnt = ejb.findTEntity(entity.getId());
      Assert.assertNotNull(selEnt);
      Assert.assertEquals(6, selEnt.getCounter());

      sp = registerSetpoint(TEntity.class.getName(), ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT,
            ControlEvent.UPDATE, ControlEvent.DELETE);

      selEnt.setCounter(12);
      ejb.merge(selEnt);

      selEnt = ejb.findTEntity(entity.getId());
      Assert.assertNotNull(selEnt);
      Assert.assertEquals(12, selEnt.getCounter());

      List<Archive> list = ejb.queryArchive(TENANT, TEntity.class.getName(), String.valueOf(entity.getId()));
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(ControlEvent.UPDATE, list.get(0).getControlEvent());
   }

   @Test
   public void persistWithArchiveOnServer() throws Exception {
      log.info("start persistWithArchiveOnServer()");

      Long[] res = ejb.persistWithArchiveRollback(TENANT);

      TEntity selEnt1 = ejb.findTEntity(res[0]);
      Assert.assertNotNull(selEnt1);
      Assert.assertEquals(25, selEnt1.getCounter());

      TEntity selEnt = ejb.findTEntity(res[1]);
      Assert.assertNotNull(selEnt);
      Assert.assertEquals(5, selEnt.getCounter());

      List<Archive> list = ejb.queryArchive(TENANT, TEntity.class.getName(), String.valueOf(res[1]));
      Assert.assertEquals(1, list.size());
   }

   @Test
   public void persistWithQueryInBetween() throws Exception {
      log.info("start persistWithQueryInBetween()");

      TEntity entity = createTEntity(5, "bb");
      entity = ejb.persist(entity);

      long id2 = ejb.persistWithQueryInBetween(TENANT, entity.getId());

      TEntity selEnt1 = ejb.findTEntity(id2);
      Assert.assertNotNull(selEnt1);
      Assert.assertEquals(25, selEnt1.getCounter());

      TEntity selEnt = ejb.findTEntity(entity.getId());
      Assert.assertNotNull(selEnt);
      Assert.assertEquals(5, selEnt.getCounter());
   }

   @Test
   public void persistWithQueryInBetweenRollback() throws Exception {
      log.info("start persistWithQueryInBetweenRollback()");

      TEntity entity = createTEntity(5, "mm");
      entity = ejb.persist(entity);

      long id2 = ejb.persistWithQueryInBetweenRollback(TENANT, entity.getId());

      TEntity selEnt1 = ejb.findTEntity(id2);
      Assert.assertNull(selEnt1);

      TEntity selEnt = ejb.findTEntity(entity.getId());
      Assert.assertNotNull(selEnt);
      Assert.assertEquals(5, selEnt.getCounter());
   }

   @Test
   public void persistWith4EyesPostponedException() throws Exception {
      log.info("start persistWith4EyesPostponedException()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE);
      FourEyesActuator fe = (FourEyesActuator) Configuration.instance().getActuator(FourEyesActuator.DEFAULTNAME);
      fe.setThrowPostponedException(true);

      TEntity entity = createTEntity(5, "hh");
      try {
         entity = ejb.persist(entity);
         Assert.fail();
      } catch (PostponedEjbException e) {
         log.warn(e.getMessage());
      }

      TEntity selEnt = ejb.findTEntity(entity.getId());
      Assert.assertNull(selEnt);
      List<Archive> list = ejb.queryArchiveByTenant();
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(ControlEvent.INSERT, list.get(0).getControlEvent());
      JpaResource res = (JpaResource) list.get(0).getResource();
      Assert.assertEquals("0", res.getPrimaryKeyId());

      List<Controllable> list1 = ejb.queryControllable();
      Assert.assertEquals(1, list1.size());
      Assert.assertEquals(ControlEvent.INSERT, list1.get(0).getControlEvent());
   }

   @Test
   public void findUnreleased() throws Exception {
      log.info("start findUnreleased()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT);

      TEntity entity = createTEntity(5, "ggh");
      entity = ejb.persist(entity);

      List<Controllable> l = ejb.findUnreleased();
      Assert.assertEquals(1, l.size());

      l = ejb.findUnreleased();
      Assert.assertEquals(1, l.size());

      l = ejb.findUnreleased(TEntity.class);
      Assert.assertEquals(1, l.size());
   }

   @Test
   public void releasePersist() throws Exception {
      log.info("start releasePersist()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.RELEASE);

      TEntity entity = createTEntity(5, "lkaus");
      entity = ejb.persist(entity);

      List<Controllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());

      Context.sessionScope().setUser("test2");
      ejb.release();

      Context.sessionScope().setUser(USER);
      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      List<Archive> list = ejb.queryArchiveByTenant();
      Assert.assertEquals(2, list.size());
      JpaResource res1 = (JpaResource) list.get(0).getResource();
      JpaResource res2 = (JpaResource) list.get(1).getResource();
      Assert.assertEquals(res1.getPrimaryKeyId(), res2.getPrimaryKeyId());

      TEntity te = ejb.findTEntity(Long.parseLong(res1.getPrimaryKeyId()));
      Assert.assertNotNull(te);
   }

   @Test
   public void releasePersistInvalidUser() throws Exception {
      log.info("start releasePersistInvalidUser()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT);

      TEntity entity = createTEntity(5, "iii");
      entity = ejb.persist(entity);

      List<Controllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());
      try {
         ejb.release();
         Assert.fail();
      } catch (EJBException e) {
         Assert.assertEquals(InvalidUserException.class, e.getCause().getClass());
      }
   }

   @Test
   public void releaseUpdate() throws Exception {
      log.info("start EEApiTest-releaseUpdate()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.UPDATE);

      TComplexEntity ce = createTComplexEntity();
      ce = ejb.persist(ce);

      ejb.querySingleResult("SELECT a FROM TComplexEntity a");

      List<TComplexEntity> l = (List<TComplexEntity>) ejb.queryResultSet("SELECT a FROM TComplexEntity a");
      Assert.assertEquals(1, l.size());
      TComplexEntity tce = l.get(0);
      Assert.assertEquals(3, tce.getLazyList().size());
      TEntity t1 = tce.getLazyList().iterator().next();
      tce.getLazyList().remove(t1);

      ejb.merge(tce);

      List<Controllable> l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());

      Context.sessionScope().setUser("tester2");
      ejb.release();

      l1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, l1.size());

      l = (List<TComplexEntity>) ejb.queryResultSet("SELECT a FROM TComplexEntity a");
      Assert.assertEquals(1, l.size());
      tce = (TComplexEntity) ejb.querySingleResult("SELECT a FROM TComplexEntity a");
      Assert.assertEquals(2, tce.getLazyList().size());
   }

   /**
    * update after persist on same object in archive mode leads to direct update in database and creation of 2 archive
    * records.
    */
   @Test
   public void persistUpdateArchive() throws Exception {
      log.info("start persistUpdateArchive()");

      sp = registerSetpoint(TEntity.class.getName(), ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT,
            ControlEvent.UPDATE);

      TEntity entity = createTEntity(7, "dfdf");
      entity = ejb.persist(entity);

      entity.setCounter(13);
      ejb.merge(entity);

      TEntity selEnt = ejb.findTEntity(entity.getId());
      Assert.assertNotNull(selEnt);
      Assert.assertEquals(13, selEnt.getCounter());

      List<Archive> list = ejb.queryArchive(TENANT, TEntity.class.getName(), String.valueOf(entity.getId()));
      Assert.assertEquals(2, list.size());
      Assert.assertEquals(ControlEvent.INSERT, list.get(0).getControlEvent());
      Assert.assertEquals(ControlEvent.UPDATE, list.get(1).getControlEvent());
   }

   /**
    * 2 consecutive calls to persist on the same object leads to: with 4-eyes: two instances in Controllable and ARCHIVE
    */
   @Test
   public void persistPersistWith4Eyes() throws Exception {
      log.info("start persistPersistWith4Eyes()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT);

      TEntity entity = createTEntity(34, "Hase");
      entity = ejb.persist(entity);

      entity.setCounter(13);
      ejb.persist(entity);

      List<Archive> list = ejb.queryArchiveByTenant();
      Assert.assertEquals(2, list.size());
      JpaResource res = (JpaResource) list.get(0).getResource();
      JpaResource res1 = (JpaResource) list.get(1).getResource();
      Assert.assertEquals(ControlEvent.INSERT, list.get(0).getControlEvent());
      Assert.assertEquals("0", res.getPrimaryKeyId());
      Assert.assertEquals(ControlEvent.INSERT, list.get(1).getControlEvent());
      Assert.assertEquals("0", res1.getPrimaryKeyId());

      log.debug("CEM: " + Context.requestScope().getEntityManager());

      List<Controllable> list1 = (List<Controllable>) ejb.queryControllable();
      Assert.assertEquals(2, list1.size());
      Assert.assertEquals(ControlEvent.INSERT, list1.get(0).getControlEvent());
      Assert.assertEquals(ControlEvent.INSERT, list1.get(1).getControlEvent());
      log.debug("CEM: " + Context.requestScope().getEntityManager());
   }

   @Test
   public void persistRemoveNoControl() throws Exception {
      log.info("start persistRemoveNoControl()");

      TEntity entity = createTEntity(12, "zu");
      entity = ejb.persist(entity);

      ejb.remove(entity);

      TEntity selEnt = ejb.findTEntity(entity.getId());
      Assert.assertNull(selEnt);
   }

   @Test
   public void persistRemoveWith4Eyes() throws Exception {
      log.info("start persistRemoveWith4Eyes()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE);

      TEntity entity = createTEntity(78, "l�senburg");
      entity = ejb.persist(entity);

      entity.setCounter(13);
      try {
         ejb.merge(entity);
         Assert.fail();
      } catch (EJBException e) {
         Exception nested = e.getCausedByException();
         Assert.assertTrue(
               nested instanceof UnapprovedResourceException || e.getCause() instanceof UnapprovedResourceException);
      }
      List<Archive> list = ejb.queryArchiveByTenant();
      Assert.assertEquals(1, list.size());
      JpaResource res = (JpaResource) list.get(0).getResource();
      Assert.assertEquals(ControlEvent.INSERT, list.get(0).getControlEvent());
      Assert.assertEquals("0", res.getPrimaryKeyId());

      Controllable dcOb = (Controllable) ejb.querySingleResult("SELECT a FROM Controllable a");
      Assert.assertEquals(ControlEvent.INSERT, dcOb.getControlEvent());
   }

   @Test
   public void assocNoControl() throws Exception {
      log.info("start assocNoControl()");

      TComplexEntity ce = createTComplexEntity();
      ce = ejb.persist(ce);

      TComplexEntity selEnt = ejb.findTComplexEntity(ce.getId());
      Assert.assertNotNull(selEnt);
      Assert.assertEquals(12, selEnt.getCompValue());

      selEnt.setCompValue(13);
      ejb.merge(selEnt);

      selEnt = ejb.findTComplexEntity(ce.getId());
      Assert.assertNotNull(selEnt);
      Assert.assertEquals(13, selEnt.getCompValue());
      Assert.assertEquals(2, selEnt.getEagerList().size());
      Assert.assertEquals(3, selEnt.getLazyList().size());
      Assert.assertNotNull(selEnt.getTen());
   }

   @Test
   public void assocArchive() throws Exception {
      log.info("start assocArchive()");

      sp = registerSetpoint(TComplexEntity.class.getName(), ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT,
            ControlEvent.UPDATE, ControlEvent.DELETE);

      TComplexEntity ce = createTComplexEntity();
      ce = ejb.persist(ce);

      List<Archive> list = ejb.queryArchive(TENANT, TComplexEntity.class.getName(), String.valueOf(ce.getId()));
      Assert.assertEquals(1, list.size());
      JpaResource res = (JpaResource) list.get(0).getResource();
      TComplexEntity decObj = (TComplexEntity) res.getObject();
      Assert.assertEquals(2, decObj.getEagerList().size());
      Assert.assertEquals(3, decObj.getLazyList().size());
      Assert.assertNotNull(decObj.getTen());
      checkLists(decObj);

      TComplexEntity selEnt = ejb.findTComplexEntity(ce.getId());
      selEnt.setCompValue(13);
      ejb.merge(selEnt);

      list = ejb.queryArchive(TENANT, TComplexEntity.class.getName(), String.valueOf(ce.getId()));
      Assert.assertEquals(2, list.size());
      JpaResource res1 = (JpaResource) list.get(0).getResource();
      JpaResource res2 = (JpaResource) list.get(1).getResource();
      TComplexEntity decObj1 = (TComplexEntity) res1.getObject();
      TComplexEntity decObj2 = (TComplexEntity) res2.getObject();
      Assert.assertEquals(2, decObj1.getEagerList().size());
      Assert.assertEquals(3, decObj1.getLazyList().size());
      Assert.assertNotNull(decObj1.getTen());
      Assert.assertEquals(2, decObj2.getEagerList().size());
      Assert.assertEquals(3, decObj2.getLazyList().size());
      Assert.assertNotNull(decObj2.getTen());
      Assert.assertEquals(12, decObj1.getCompValue());
      Assert.assertEquals(13, decObj2.getCompValue());

      checkLists(decObj1);
      checkLists(decObj2);
   }

   @Test
   public void selectWith4Eyes() throws Exception {
      log.info("start selectWith4Eyes()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.SELECT);

      TEntity entity = createTEntity(34, "Hase");
      entity = ejb.persist(entity);

      TEntity ent2 = ejb.findTEntity(entity.getId());
      Assert.assertNull(ent2);

      List<Archive> list = ejb.queryArchiveByTenant();
      Assert.assertEquals(1, list.size());
      JpaResource res = (JpaResource) list.get(0).getResource();
      Assert.assertEquals(ControlEvent.SELECT, list.get(0).getControlEvent());
      Assert.assertEquals(String.valueOf(entity.getId()), res.getPrimaryKeyId());
      Assert.assertEquals(entity.getId(), res.getPrimaryKeyObject());

      List<Controllable> list1 = (List<Controllable>) ejb.queryControllable();
      Assert.assertEquals(1, list1.size());
      Assert.assertEquals(ControlEvent.SELECT, list1.get(0).getControlEvent());

      Context.sessionScope().setUser("test2");
      TEntity result = (TEntity) ejb.release();
      Assert.assertEquals(entity.getId(), result.getId());
      Assert.assertEquals(entity.getNameValue(), result.getNameValue());

      Context.sessionScope().setUser(USER);
      list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());
   }

   @Test
   public void selectWith4EyesLockMode() throws Exception {
      log.info("start selectWith4EyesLockMode()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.SELECT);

      TEntity entity = createTEntity(34, "Hase");
      entity = ejb.persist(entity);

      TEntity ent2 = ejb.findTEntity(entity.getId(), LockModeType.PESSIMISTIC_READ, null);
      Assert.assertNull(ent2);

      List<Archive> list = ejb.queryArchiveByTenant();
      Assert.assertEquals(1, list.size());
      JpaResource res = (JpaResource) list.get(0).getResource();
      Assert.assertEquals(ControlEvent.SELECT, list.get(0).getControlEvent());
      Assert.assertEquals(String.valueOf(entity.getId()), res.getPrimaryKeyId());
      Assert.assertEquals(entity.getId(), res.getPrimaryKeyObject());
      Assert.assertEquals(1, res.getParameters().size());
      Assert.assertEquals(LockModeType.PESSIMISTIC_READ, res.getParameters().iterator().next().getUnencodedValue());

      List<Controllable> list1 = (List<Controllable>) ejb.queryControllable();
      Assert.assertEquals(1, list1.size());
      Assert.assertEquals(ControlEvent.SELECT, list1.get(0).getControlEvent());

      Context.sessionScope().setUser("test2");
      TEntity result = (TEntity) ejb.release();
      Assert.assertEquals(entity.getId(), result.getId());
      Assert.assertEquals(entity.getNameValue(), result.getNameValue());

      Context.sessionScope().setUser(USER);
      list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());
   }

   @Test
   public void selectWith4EyesProps() throws Exception {
      log.info("start selectWith4EyesProps()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.SELECT);

      TEntity entity = createTEntity(34, "Hase");
      entity = ejb.persist(entity);

      Map<String, Object> props = new HashMap<String, Object>();
      props.put("hint1", createTEntity(35, "Igel"));
      TEntity ent2 = ejb.findTEntity(entity.getId(), LockModeType.PESSIMISTIC_READ, props);
      Assert.assertNull(ent2);

      List<Archive> list = ejb.queryArchiveByTenant();
      Assert.assertEquals(1, list.size());
      JpaResource res = (JpaResource) list.get(0).getResource();
      Assert.assertEquals(ControlEvent.SELECT, list.get(0).getControlEvent());
      Assert.assertEquals(String.valueOf(entity.getId()), res.getPrimaryKeyId());
      Assert.assertEquals(entity.getId(), res.getPrimaryKeyObject());
      Assert.assertEquals(2, res.getParameters().size());

      TEntity propsTE = null;
      Iterator<ResourceParameter> iter = res.getParameters().iterator();
      ResourceParameter p0 = iter.next();
      ResourceParameter p1 = iter.next();

      if (p0.getUnencodedValue() instanceof TEntity) {
         propsTE = (TEntity) p0.getUnencodedValue();
         Assert.assertEquals(LockModeType.PESSIMISTIC_READ, p1.getUnencodedValue());
      } else {
         propsTE = (TEntity) p1.getUnencodedValue();
         Assert.assertEquals(LockModeType.PESSIMISTIC_READ, p0.getUnencodedValue());
      }
      Assert.assertEquals("Igel", propsTE.getNameValue());

      List<Controllable> list1 = (List<Controllable>) ejb.queryControllable();
      Assert.assertEquals(1, list1.size());
      Assert.assertEquals(ControlEvent.SELECT, list1.get(0).getControlEvent());

      Context.sessionScope().setUser("test2");
      TEntity result = (TEntity) ejb.release();
      Assert.assertEquals(entity.getId(), result.getId());
      Assert.assertEquals(entity.getNameValue(), result.getNameValue());

      Context.sessionScope().setUser(USER);
      list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());
   }

   @Test
   public void selectWith4EyesLockModeProps() throws Exception {
      log.info("start selectWith4EyesLockModeProps()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.SELECT);

      TEntity entity = createTEntity(34, "Hase");
      entity = ejb.persist(entity);

      Map<String, Object> props = new HashMap<String, Object>();
      props.put("hint1", createTEntity(35, "Igel"));
      TEntity ent2 = ejb.findTEntity(entity.getId(), null, props);
      Assert.assertNull(ent2);

      List<Archive> list = ejb.queryArchiveByTenant();
      Assert.assertEquals(1, list.size());
      JpaResource res = (JpaResource) list.get(0).getResource();
      Assert.assertEquals(ControlEvent.SELECT, list.get(0).getControlEvent());
      Assert.assertEquals(String.valueOf(entity.getId()), res.getPrimaryKeyId());
      Assert.assertEquals(entity.getId(), res.getPrimaryKeyObject());
      Assert.assertEquals(1, res.getParameters().size());
      TEntity propsTE = (TEntity) res.getParameters().iterator().next().getUnencodedValue();
      Assert.assertEquals("Igel", propsTE.getNameValue());

      List<Controllable> list1 = (List<Controllable>) ejb.queryControllable();
      Assert.assertEquals(1, list1.size());
      Assert.assertEquals(ControlEvent.SELECT, list1.get(0).getControlEvent());

      Context.sessionScope().setUser("test2");
      TEntity result = (TEntity) ejb.release();
      Assert.assertEquals(entity.getId(), result.getId());
      Assert.assertEquals(entity.getNameValue(), result.getNameValue());

      Context.sessionScope().setUser(USER);
      list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());
   }

   @Test
   public void doubleInsertWith4Eyes() throws Exception {
      log.info("start doubleInsertWith4Eyes()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.RELEASE);

      TEntity entity = createTEntity(5, "valueY");
      entity = ejb.persist(entity);

      Assert.assertEquals(0, entity.getId());
      TEntity selEnt = ejb.findTEntity(entity.getId());
      Assert.assertNull(selEnt);
      List<Archive> list = ejb.queryArchiveByTenant();
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(ControlEvent.INSERT, list.get(0).getControlEvent());
      JpaResource res = (JpaResource) list.get(0).getResource();
      Assert.assertEquals("0", res.getPrimaryKeyId());

      List<Controllable> list1 = ejb.queryControllable();
      Assert.assertEquals(1, list1.size());
      Assert.assertEquals(ControlEvent.INSERT, list1.get(0).getControlEvent());

      TEntity entity2 = createTEntity(33, "L�mmel");
      entity2 = ejb.persist(entity2);
      log.debug(":" + entity2);

      List<Controllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(2, l.size());

      Context.sessionScope().setUser("test2");
      ejb.release(l.get(0));

      Context.sessionScope().setUser("test3");
      ejb.release(l.get(1));

      Context.sessionScope().setUser(USER);
      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      list = ejb.queryArchiveByTenant();
      Assert.assertEquals(4, list.size());
      JpaResource res0 = (JpaResource) list.get(0).getResource();
      JpaResource res1 = (JpaResource) list.get(1).getResource();
      JpaResource res2 = (JpaResource) list.get(2).getResource();
      JpaResource res3 = (JpaResource) list.get(3).getResource();
      Assert.assertEquals(res0.getPrimaryKeyId(), res2.getPrimaryKeyId());
      Assert.assertEquals(res1.getPrimaryKeyId(), res3.getPrimaryKeyId());
      Assert.assertEquals(res0.getUniqueId(), res2.getUniqueId());
      Assert.assertEquals(res1.getUniqueId(), res3.getUniqueId());
   }

}
