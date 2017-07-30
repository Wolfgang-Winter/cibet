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
package com.logitags.cibet.actuator;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.hibernate.ejb.HibernateQuery;
import org.junit.After;
import org.junit.Test;

import com.cibethelper.base.DBHelper;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.Context;

/**
 * tests CibetEntityManager with Archive and FourEyes- actuators.
 * 
 * @author test
 * 
 */
public class HibernateIntegrationTest extends DBHelper {

   private static Logger log = Logger.getLogger(HibernateIntegrationTest.class);

   private Setpoint sp = null;

   @After
   public void afterHibernateIntegrationTest() {
      if (sp != null) {
         Configuration.instance().unregisterSetpoint(sp.getId());
      }
   }

   @Test
   public void persistWithArchive() {
      log.info("start persistWithArchive()");

      log.info("**********");
      EntityManager emm = Context.requestScope().getEntityManager();
      log.info(emm);
      log.info(emm.getDelegate());
      log.info(emm.getDelegate().getClass());

      EntityManager em2 = emm.unwrap(EntityManager.class);
      log.info(em2);

      Query q = emm.createNamedQuery(Archive.SEL_ALL);
      log.info(q);
      org.hibernate.Query q2 = q.unwrap(HibernateQuery.class).getHibernateQuery();
      log.info("querystring: " + q2.getQueryString());

      // sp = registerSetpoint(TEntity.class.getName(), ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT,
      // ControlEvent.UPDATE, ControlEvent.DELETE);
      //
      // TEntity entity = persistTEntity();
      // TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      // Assert.assertNotNull(selEnt);
      // Assert.assertEquals(5, selEnt.getCounter());
      //
      // List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(entity.getClass().getName(),
      // String.valueOf(entity.getId()));
      // Assert.assertEquals(1, list.size());
      // Archive ar = list.get(0);
      // JpaResource res = (JpaResource) ar.getResource();
      //
      // Assert.assertEquals(TEntity.class.getName(), res.getTarget());
      // TEntity en = (TEntity) res.getUnencodedTargetObject();
      // Assert.assertTrue("expected: " + entity.getId() + ", actual: " + en.getId(), en.getId() == entity.getId());
   }

}
