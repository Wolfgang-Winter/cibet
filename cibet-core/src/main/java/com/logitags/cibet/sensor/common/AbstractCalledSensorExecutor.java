package com.logitags.cibet.sensor.common;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.circuitbreaker.CircuitBreakerActuator;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.ExecutionStatus;

public abstract class AbstractCalledSensorExecutor {

   private Log log = LogFactory.getLog(AbstractCalledSensorExecutor.class);

   private ExecutorService executor = Executors.newCachedThreadPool();

   protected void call(EventMetadata metadata, Callable<Void> callable) throws Throwable {
      long timeout = (long) metadata.getProperties().get(CircuitBreakerActuator.TIMEOUT_KEY);
      long starttime = System.currentTimeMillis();
      Future<Void> future = executor.submit(callable);
      try {
         future.get(timeout, TimeUnit.MILLISECONDS);
      } catch (TimeoutException e) {
         boolean okay = future.cancel(true);
         log.debug("future cancelled due to timeout: " + okay);
         metadata.setExecutionStatus(ExecutionStatus.TIMEOUT);
         if (metadata.getProperties().containsKey(CircuitBreakerActuator.THROWTIMEOUTEXCEPTION_KEY)) {
            throw e;
         }

      } catch (ExecutionException e) {
         log.debug("future task throws Exception: " + e.getCause());
         boolean okay = future.cancel(true);
         log.debug("future cancelled: " + okay);
         throw e.getCause();
      } finally {
         // executor.shutdownNow();
         long endtime = System.currentTimeMillis();
         long duration = endtime - starttime;
         log.debug("duration: " + duration);
      }
   }

}
