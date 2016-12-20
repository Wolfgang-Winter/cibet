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
package com.cibethelper;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;

import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.context.CibetContext;
import com.logitags.cibet.context.CibetContextAspectTest;
import com.logitags.cibet.context.Context;

@CibetContext
public class CibetContextAspectTestHelper {

   private CibetContextAspectTest testClass;

   public CibetContextAspectTestHelper(CibetContextAspectTest t) {
      testClass = t;
   }

   private static Logger log = Logger.getLogger(CibetContextAspectTestHelper.class);

   public void releasePersistAspect() throws Exception {
      log.info("start releasePersistAspectClassAspect()");

      TEntity ent = testClass.persistTEntity();
      Assert.assertEquals(0, ent.getId());

      List<DcControllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());
      DcControllable co = l.get(0);
      Assert.assertEquals(CibetContextAspectTest.AUSER, co.getCreateUser());

      Context.sessionScope().setUser("test2");
      Object res = co.release(CibetContextAspectTest.getApplEman(), null);
      Assert.assertNotNull(res);
      Assert.assertTrue(res instanceof TEntity);
      Assert.assertTrue(((TEntity) res).getId() != 0);

      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      Context.sessionScope().setUser(CibetContextAspectTest.AUSER);
      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      TEntity te = CibetContextAspectTest.getApplEman().find(TEntity.class, ((TEntity) res).getId());
      Assert.assertNotNull(te);
      Context.requestScope().setRollbackOnly(true);
   }

}
