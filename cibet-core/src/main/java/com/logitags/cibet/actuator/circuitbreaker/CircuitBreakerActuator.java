package com.logitags.cibet.actuator.circuitbreaker;

import com.logitags.cibet.actuator.common.AbstractActuator;
import com.logitags.cibet.core.EventMetadata;

public class CircuitBreakerActuator extends AbstractActuator {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public static final String TIMEOUT_KEY = "__TIMEOUT_KEY";
   public static final String THROWTIMEOUTEXCEPTION_KEY = "__THROWTIMEOUTEXCEPTION_KEY";

   public static final String DEFAULTNAME = "CIRCUITBREAKER";

   /**
    * timeout in milliseconds
    */
   private long timeout;

   private boolean throwTimeoutException = false;

   public CircuitBreakerActuator() {
      setName(DEFAULTNAME);
   }

   public CircuitBreakerActuator(String name) {
      setName(name);
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * com.logitags.cibet.actuator.AbstractActuator#beforeEvent(com.logitags.
    * cibet.core.EventMetadata)
    */
   @Override
   public void beforeEvent(EventMetadata ctx) {
      ctx.getProperties().put(TIMEOUT_KEY, timeout);
      if (throwTimeoutException) {
         ctx.getProperties().put(THROWTIMEOUTEXCEPTION_KEY, true);
      }
   }

   /**
    * @return the timeout
    */
   public long getTimeout() {
      return timeout;
   }

   /**
    * @param timeout
    *           the timeout to set
    */
   public void setTimeout(long timeout) {
      this.timeout = timeout;
   }

   /**
    * @return the throwTimeoutException
    */
   public boolean isThrowTimeoutException() {
      return throwTimeoutException;
   }

   /**
    * @param throwTimeoutException
    *           the throwTimeoutException to set
    */
   public void setThrowTimeoutException(boolean throwTimeoutException) {
      this.throwTimeoutException = throwTimeoutException;
   }

}
