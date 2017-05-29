package com.logitags.cibet.actuator.lock;

import com.logitags.cibet.actuator.dc.DcControllable;

/**
 * This exception is thrown when a lock operation fails because the item to lock is already locked.
 */
public class AlreadyLockedException extends Exception {

   /**
    * 
    */
   private static final long serialVersionUID = -6232214604025449320L;

   /**
    * the already existing LockedObject
    */
   private DcControllable lockedControl;

   public AlreadyLockedException(DcControllable lo) {
      lockedControl = lo;
   }

   /**
    * @return the lockedObject
    */
   public DcControllable getLockedControl() {
      return lockedControl;
   }

}
