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
/**
 * 
 */
package com.logitags.cibet.actuator.history;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.common.AbstractActuator;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.diff.Difference;
import com.logitags.cibet.resource.PersistenceUtil;
import com.logitags.cibet.sensor.jpa.JpaResource;

/**
 * creates an archive entry in the database.
 */
public class HistoryActuator extends AbstractActuator {

   /**
    * 
    */
   private static final long serialVersionUID = -5221476168284114855L;

   private transient Log log = LogFactory.getLog(HistoryActuator.class);

   public static final String DEFAULTNAME = "HISTORY";

   public HistoryActuator() {
      setName(DEFAULTNAME);
   }

   public HistoryActuator(String name) {
      setName(name);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.actuator.common.Actuator#beforeEvent(com.logitags. cibet.core.EventMetadata)
    */
   @Override
   public void beforeEvent(EventMetadata ctx) {
      log.debug("beforeEvent HistoryActuator");
      if (ctx.getExecutionStatus() == ExecutionStatus.DENIED) {
         log.info("EventProceedStatus is DENIED. Skip afterEvent of " + this.getClass().getSimpleName());
         return;
      }

      if (ctx.getControlEvent() == ControlEvent.UPDATE) {
         PersistenceUtil.getDirtyUpdates(ctx);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.actuator.common.Actuator#afterEvent(com.logitags.cibet.core. EventMetadata)
    */
   @Override
   public void afterEvent(EventMetadata ctx) {
      log.debug("afterEvent");
      if (ctx.getExecutionStatus() == ExecutionStatus.DENIED) {
         log.info("EventProceedStatus is DENIED. Skip afterEvent of " + this.getClass().getSimpleName());
         return;
      }

      if (Context.requestScope().isPlaying()) {
         return;
      }

      List<Difference> diffs = null;

      switch (ctx.getControlEvent()) {
      case INVOKE:
      case UPDATEQUERY:
      case RELEASE_INVOKE:
      case RELEASE_UPDATEQUERY:
      case FIRST_RELEASE_INVOKE:
      case FIRST_RELEASE_UPDATEQUERY:
      case REJECT_INVOKE:
      case REJECT_UPDATEQUERY:
      case PASSBACK_INVOKE:
      case PASSBACK_UPDATEQUERY:
      case SUBMIT_INVOKE:
      case SUBMIT_UPDATEQUERY:
      case REDO:
      case RESTORE_INSERT:
      case RESTORE_UPDATE:
         return;

      case RELEASE_INSERT:
         if (ctx.getExecutionStatus() == ExecutionStatus.EXECUTED) {
            updatePrimaryKey(ctx);
         }
         break;

      case UPDATE:
         diffs = PersistenceUtil.getDirtyUpdates(ctx);
         break;

      default:
         break;
      }

      History his = createHistory(ctx, diffs);
      Context.internalRequestScope().getOrCreateEntityManager(true).persist(his);
   }

   private History createHistory(EventMetadata ctx, List<Difference> diffs) {
      History his = new History();
      his.setCaseId(ctx.getCaseId());
      his.setControlEvent(ctx.getControlEvent());
      his.setCreateDate(new Date());
      his.setCreateUser(Context.sessionScope().getUser());
      his.setDiffList(diffs);
      his.setExecutionStatus(ctx.getExecutionStatus());

      if (ctx.getResource() instanceof JpaResource) {
         his.setPrimaryKeyId(((JpaResource) ctx.getResource()).getPrimaryKeyId());
      }

      his.setRemark(Context.requestScope().getRemark());
      his.setTarget(ctx.getResource().getTarget());
      his.setTenant(Context.sessionScope().getTenant());
      return his;
   }

   private void updatePrimaryKey(EventMetadata ctx) {
      if (!(ctx.getResource() instanceof JpaResource)) {
         return;
      }
      JpaResource jpaRes = (JpaResource) ctx.getResource();

      if (jpaRes.getPrimaryKeyId() == null) {
         String msg = "no value for primary key found in history " + ctx.getResource();
         log.warn(msg);
         return;
      }

      // set primary key of previous histories of this business case
      EntityManager em = Context.internalRequestScope().getOrCreateEntityManager(true);
      TypedQuery<History> q = em.createNamedQuery(History.SEL_ALL_BY_CASEID, History.class);
      q.setParameter("tenant", Context.sessionScope().getTenant());
      q.setParameter("caseId", ctx.getCaseId());
      List<History> list = q.getResultList();
      for (History arch : list) {
         arch.setPrimaryKeyId(jpaRes.getPrimaryKeyId());
         em.merge(arch);
         log.info(arch.getHistoryId() + " history primarykey updated with " + jpaRes.getPrimaryKeyId());
      }
   }

}
