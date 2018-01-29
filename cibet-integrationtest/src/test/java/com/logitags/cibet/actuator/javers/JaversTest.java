/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2014 Dr. Wolfgang Winter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************
 */
package com.logitags.cibet.actuator.javers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.javers.repository.sql.ConnectionProvider;
import org.javers.repository.sql.DialectName;
import org.javers.repository.sql.JaversSqlRepository;
import org.javers.repository.sql.SqlRepositoryBuilder;
import org.junit.Ignore;
import org.junit.Test;

import com.cibethelper.entities.TCompareEntity;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TEntity;

public class JaversTest {

   private static Logger log = Logger.getLogger(JaversTest.class);

   // @Test
   public void test() {
      Javers javers = JaversBuilder.javers().build();

      TEntity t1 = new TEntity(1, "Stung1", 12, "owner1");
      TEntity t2 = new TEntity(1, "Stung1", 13, "owner1");
      Diff diff = javers.compare(t1, t2);
      log.info(diff);

      TComplexEntity t3 = new TComplexEntity();
      TComplexEntity t4 = new TComplexEntity();
      t3.setTen(t1);

      diff = javers.compare(t1, t2);
      log.info(diff);
      log.info(diff.getChanges().get(0).getAffectedLocalId());
      log.info(diff.getChanges().get(0).getAffectedGlobalId());

      diff = javers.compare(t3, t4);
      log.info(diff);

      t4.setTen(t2);
      diff = javers.compare(t3, t4);
      log.info(diff);

      TCompareEntity c1 = new TCompareEntity("Stung1", 12, "owner1");
      TCompareEntity c2 = new TCompareEntity("Stung1", 12, "owner1");

      int[] iarray = new int[] { 1, 2, 3, 4 };
      c1.setIntArray(iarray);
      c2.setIntArray(null);
      diff = javers.compare(c1, c2);
      log.info(diff);

      TEntity e1 = new TEntity(1, "Karl", 5, "Putz");
      TEntity e2 = new TEntity(2, "Karl2", 5, "Putz");
      TEntity e3 = new TEntity(3, "Karl3", 5, "Putz");
      TEntity e4 = new TEntity(4, "Karl4", 5, "Putz");
      c1.setEntList(Arrays.asList(e1, e2, e3));
      c2.setEntList(Arrays.asList(e1, e2, e3, e4));
      diff = javers.compare(c1, c2);
      log.info(diff);

      c1.setIntArray(null);
      c1.setEntList(Arrays.asList(e1, e4, e3));
      c2.setEntList(Arrays.asList(e1, e2, e3));
      diff = javers.compare(c1, c2);
      log.info(diff);

      TEntity e21 = new TEntity(2, "Karl3", 5, "Putz");
      c1.setEntList(Arrays.asList(e1, e2, e3));
      c2.setEntList(Arrays.asList(e1, e21, e3));
      diff = javers.compare(c1, c2);
      log.info(diff);
   }

   @Ignore
   @Test
   public void testPersistence() throws SQLException {

      Connection dbConnection = DriverManager.getConnection("jdbc:derby://localhost:1527/cibettest", "APP", "x");

      ConnectionProvider connectionProvider = new ConnectionProvider() {
         @Override
         public Connection getConnection() {
            // suitable only for testing!
            return dbConnection;
         }
      };

      // derby not supported
      JaversSqlRepository sqlRepository = SqlRepositoryBuilder.sqlRepository().withSchema("APP")
            .withConnectionProvider(connectionProvider).withDialect(DialectName.MYSQL).build();
      Javers javers = JaversBuilder.javers().registerJaversRepository(sqlRepository).build();
   }

}
