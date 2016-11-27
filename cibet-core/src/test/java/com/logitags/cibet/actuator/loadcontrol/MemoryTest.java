package com.logitags.cibet.actuator.loadcontrol;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;

import org.apache.log4j.Logger;
import org.junit.Test;

public class MemoryTest {

	private static Logger log = Logger.getLogger(MemoryTest.class);

	@Test
	public void test() {
		log.info("max Memory: " + Runtime.getRuntime().maxMemory());
		log.info("free Memory: " + Runtime.getRuntime().freeMemory());

		MemoryMXBean memoryMX = ManagementFactory.getMemoryMXBean();
		MemoryUsage memUsage = memoryMX.getHeapMemoryUsage();
		MemoryUsage nonHeapMem = memoryMX.getNonHeapMemoryUsage();
		log.info("heap Memory: " + memUsage);
		log.info("non-heap Memory: " + nonHeapMem);

		for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
			log.info("pool name: " + pool.getName());
			log.info("pool type: " + pool.getType());
			log.info("usage: " + pool.getUsage());
			log.info("collectionUsage: " + pool.getCollectionUsage());
			log.info("-----------------------");
		}

	}

}
