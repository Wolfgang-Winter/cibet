package com.logitags.cibet.actuator.lock;

import com.logitags.cibet.actuator.common.Controllable;

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
   private Controllable lockedControl;

   public AlreadyLockedException(Controllable lo) {
      lockedControl = lo;
   }

   /**
    * @return the lockedObject
    */
   public Controllable getLockedControl() {
      return lockedControl;
   }

}
