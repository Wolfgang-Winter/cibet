/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2012 Dr. Wolfgang Winter
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
package com.logitags.cibet.sensor.jdbc.bridge;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.cibethelper.entities.TEntity;
import com.logitags.cibet.sensor.jdbc.def.EntityDefinition;
import com.logitags.cibet.sensor.jdbc.driver.CibetJdbcException;

@RunWith(MockitoJUnitRunner.class)
public class JdbcBridgeQueryTest {

   private static Logger log = Logger.getLogger(JdbcBridgeQueryTest.class);

   @Mock
   private EntityDefinition ed;

   @Mock
   private JdbcBridgeEntityManager em;

   @Test(expected = CibetJdbcException.class)
   public void getResultList() {
      Map<String, String> map = new HashMap<String, String>();
      map.put("queryName", null);
      Mockito.when(ed.getQueries()).thenReturn(map);

      JdbcBridgeEntityManager.registerEntityDefinition(TEntity.class, ed);
      JdbcBridgeQuery query = new JdbcBridgeQuery(em, "queryName");
      query.getResultList();
   }

   @Test(expected = CibetJdbcException.class)
   public void executeUpdate() {
      Map<String, String> map = new HashMap<String, String>();
      map.put("queryName", null);
      Mockito.when(ed.getQueries()).thenReturn(map);

      JdbcBridgeEntityManager.registerEntityDefinition(TEntity.class, ed);
      JdbcBridgeQuery query = new JdbcBridgeQuery(em, "queryName");
      query.executeUpdate();
   }

}
