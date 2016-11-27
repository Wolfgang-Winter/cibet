package com.logitags.cibet.actuator.lock;


/**
 * This exception is thrown when a lock operation fails because the item to lock
 * is already locked.
 */
public class AlreadyLockedException extends Exception {

   /**
    * 
    */
   private static final long serialVersionUID = -6232214604025449320L;

   /**
    * the already existing LockedObject
    */
   private LockedObject lockedObject;

   public AlreadyLockedException(LockedObject lo) {
      lockedObject = lo;
   }

   /**
    * @return the lockedObject
    */
   public LockedObject getLockedObject() {
      return lockedObject;
   }

}
