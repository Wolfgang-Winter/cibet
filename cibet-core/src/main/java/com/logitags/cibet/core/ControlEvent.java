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

package com.logitags.cibet.core;

/**
 * defines types of control events
 */
public enum ControlEvent {

   /**
    * all events.
    */
   ALL(null),

   /**
    * all INSERT, UPDATE, DELETE, RESTORE events
    */
   PERSIST(ALL),

   /**
    * new instantiation (no dc, only audit)
    */
   INSERT(PERSIST),
   /**
    * update (no dc, only audit)
    */
   UPDATE(PERSIST),

   /**
    * removal (no dc, only audit)
    */
   DELETE(PERSIST),

   SELECT(PERSIST),

   /**
    * method execution (no dc, only audit)
    */
   INVOKE(ALL),

   /**
    * when JPA query executeUpdate() is called
    */
   UPDATEQUERY(PERSIST),

   /**
    * summary of RELEASE, FIRST_RELEASE and REJECT
    */
   DC_CONTROL(ALL),
   /**
    * approval (4-eyes) or second approval (6-eyes)
    */
   RELEASE(DC_CONTROL),
   /**
    * approval (4-eyes) or second approval (6-eyes)
    */
   RELEASE_INVOKE(RELEASE),
   /**
    * approval (4-eyes) or second approval (6-eyes)
    */
   RELEASE_INSERT(RELEASE),
   /**
    * approval (4-eyes) or second approval (6-eyes)
    */
   RELEASE_DELETE(RELEASE),
   /**
    * approval (4-eyes) or second approval (6-eyes)
    */
   RELEASE_UPDATE(RELEASE),

   RELEASE_SELECT(RELEASE),

   RELEASE_UPDATEQUERY(RELEASE),

   /**
    * first approval in 6-eyes mode
    */
   FIRST_RELEASE(DC_CONTROL),
   /**
    * first approval in 6-eyes mode
    */
   FIRST_RELEASE_INVOKE(FIRST_RELEASE),
   /**
    * first approval in 6-eyes mode
    */
   FIRST_RELEASE_INSERT(FIRST_RELEASE),
   /**
    * first approval in 6-eyes mode
    */
   FIRST_RELEASE_DELETE(FIRST_RELEASE),
   /**
    * first approval in 6-eyes mode
    */
   FIRST_RELEASE_UPDATE(FIRST_RELEASE),

   FIRST_RELEASE_SELECT(FIRST_RELEASE),

   FIRST_RELEASE_UPDATEQUERY(FIRST_RELEASE),

   /**
    * rejection
    */
   REJECT(DC_CONTROL),
   /**
    * rejection of invocation
    */
   REJECT_INVOKE(REJECT),
   /**
    * rejection of delete
    */
   REJECT_DELETE(REJECT),
   /**
    * rejection of insert
    */
   REJECT_INSERT(REJECT),
   /**
    * rejection of update
    */
   REJECT_UPDATE(REJECT),

   REJECT_SELECT(REJECT),

   REJECT_UPDATEQUERY(REJECT),

   PASSBACK(DC_CONTROL),

   PASSBACK_INVOKE(PASSBACK),

   PASSBACK_DELETE(PASSBACK),

   PASSBACK_INSERT(PASSBACK),

   PASSBACK_UPDATE(PASSBACK),

   PASSBACK_SELECT(PASSBACK),

   PASSBACK_UPDATEQUERY(PASSBACK),

   SUBMIT(DC_CONTROL),

   SUBMIT_INVOKE(SUBMIT),

   SUBMIT_DELETE(SUBMIT),

   SUBMIT_INSERT(SUBMIT),

   SUBMIT_UPDATE(SUBMIT),

   SUBMIT_SELECT(SUBMIT),

   SUBMIT_UPDATEQUERY(SUBMIT),

   /**
    * redo of a method execution (no dc, only audit)
    */
   REDO(ALL),
   /**
    * restore of an object state
    */
   RESTORE(ALL),

   RESTORE_INSERT(RESTORE),

   RESTORE_UPDATE(RESTORE),

   ;

   private ControlEvent parent;

   private ControlEvent(ControlEvent p) {
      parent = p;
   }

   public ControlEvent getParent() {
      return parent;
   }

   public boolean isChildOf(ControlEvent... parents) {
      for (ControlEvent par : parents) {
         ControlEvent p = this;
         while (p != null) {
            if (par == p) return true;
            p = p.getParent();
         }
      }
      return false;
   }

}
