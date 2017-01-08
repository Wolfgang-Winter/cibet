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
package com.logitags.cibet.notification;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cibethelper.entities.TEntity;
import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.resource.ResourceHandler;
import com.logitags.cibet.sensor.ejb.EjbResourceHandler;
import com.logitags.cibet.sensor.jpa.JpaResourceHandler;
import com.logitags.cibet.sensor.pojo.MethodResourceHandler;

public class EmailNotificationProviderTest {

   private static Logger log = Logger.getLogger(EmailNotificationProviderTest.class);

   private static Calendar NOW = Calendar.getInstance();
   private static Calendar NOW_3 = Calendar.getInstance();
   private static Calendar NOW_5 = Calendar.getInstance();

   @BeforeClass
   public static void setup() {
      NOW.set(Calendar.YEAR, 2013);
      NOW.set(Calendar.MONTH, 8);
      NOW.set(Calendar.DATE, 25);
      NOW.set(Calendar.HOUR_OF_DAY, 16);
      NOW.set(Calendar.MINUTE, 13);
      NOW.set(Calendar.SECOND, 22);
      NOW.set(Calendar.MILLISECOND, 0);

      NOW_3.setTime(NOW.getTime());
      NOW_3.add(Calendar.DATE, -3);

      NOW_5.setTime(NOW.getTime());
      NOW_5.add(Calendar.DATE, -5);
   }

   protected DcControllable createDcControllable(Class<? extends ResourceHandler> handler, ExecutionStatus status) {
      DcControllable c = new DcControllable();
      c.setActuator("FOUR_EYES");
      c.setApprovalAddress("approv@test.de");
      c.setApprovalDate(NOW.getTime());
      c.setExecutionStatus(status);
      c.setApprovalUser("approvalUser");
      c.setCaseId("test-caseid");
      c.setControlEvent(ControlEvent.DELETE);
      c.setCreateAddress("create@test.de");
      c.setCreateDate(NOW_5.getTime());
      c.setCreateUser("userId");
      c.setDcControllableId("123");
      c.setFirstApprovalAddress("firstApp@test.de");
      c.setFirstApprovalDate(NOW_3.getTime());
      c.setFirstApprovalUser("firstApprovalUserId");
      c.setTenant("tenant");

      Resource res = new Resource(handler, new TEntity());
      c.setResource(res);

      return c;
   }

   @Test
   public void notifyAssign() {
      log.debug("start notifyAssign()");
      DcControllable c = createDcControllable(JpaResourceHandler.class, ExecutionStatus.POSTPONED);
      EmailNotificationProvider prov = new EmailNotificationProvider();
      prov.setFrom("from@test.de");
      prov.setSmtpHost("localhost");
      prov.setSmtpPort("8854");

      SimpleSmtpServer server = SimpleSmtpServer.start(8854);
      try {
         prov.notify(ExecutionStatus.POSTPONED, c);

         server.stop();

         Assert.assertTrue(server.getReceivedEmailSize() == 1);
         Iterator<SmtpMessage> emailIter = server.getReceivedEmail();
         SmtpMessage email = emailIter.next();
         Assert.assertEquals("Cibet Notification: POSTPONED", email.getHeaderValue("Subject"));
         log.debug(email.getBody());

         String exp = "Hello approvalUser,\n\n"
               + "A business case under dual control has been assigned to you for final approval. You may release or reject the case. Please visit the dialogue for releasing/rejecting.\n"
               + "The dual controlled business case is registered under id: 123 (case id: test-caseid)\n\n"
               + "control event:             DELETE\n"
               + "controlled target:         com.cibethelper.entities.TEntity \n"
               + "initiated by:              userId on Fri Sep 20 16:13:22 CEST 2013\n"
               + "first released by:         firstApprovalUserId on Sun Sep 22 16:13:22 CEST 2013\n\n\n"
               + "thanks for your attention\n" + "Cibet (http://www.logitags.com/cibet)";
         Assert.assertEquals(exp, email.getBody());
      } finally {
         server.stop();
      }
   }

   @Test
   public void notifyAssign2() {
      log.debug("start notifyAssign2()");
      DcControllable c = createDcControllable(JpaResourceHandler.class, ExecutionStatus.POSTPONED);
      c.setFirstApprovalUser(null);
      c.setFirstApprovalAddress(null);
      c.setFirstApprovalDate(null);
      EmailNotificationProvider prov = new EmailNotificationProvider();
      prov.setFrom("from@test.de");
      prov.setSmtpHost("localhost");
      prov.setSmtpPort("8854");

      SimpleSmtpServer server = SimpleSmtpServer.start(8854);
      try {
         prov.notify(ExecutionStatus.POSTPONED, c);

         server.stop();

         Assert.assertTrue(server.getReceivedEmailSize() == 1);
         Iterator<SmtpMessage> emailIter = server.getReceivedEmail();
         SmtpMessage email = emailIter.next();
         Assert.assertEquals("Cibet Notification: POSTPONED", email.getHeaderValue("Subject"));
         log.debug(email.getBody());

         String exp = "Hello approvalUser,\n\n"
               + "A business case under dual control has been assigned to you for final approval. You may release or reject the case. Please visit the dialogue for releasing/rejecting.\n"
               + "The dual controlled business case is registered under id: 123 (case id: test-caseid)\n\n"
               + "control event:             DELETE\n"
               + "controlled target:         com.cibethelper.entities.TEntity \n"
               + "initiated by:              userId on Fri Sep 20 16:13:22 CEST 2013\n\n\n\n"
               + "thanks for your attention\n" + "Cibet (http://www.logitags.com/cibet)";
         Assert.assertEquals(exp, email.getBody());
      } finally {
         server.stop();
      }
   }

   @Test
   public void notifyAssign3() {
      log.debug("start notifyAssign3()");
      DcControllable c = createDcControllable(MethodResourceHandler.class, ExecutionStatus.POSTPONED);
      c.getResource().setMethod("executePay");
      EmailNotificationProvider prov = new EmailNotificationProvider();
      prov.setFrom("from@test.de");
      prov.setSmtpHost("localhost");
      prov.setSmtpPort("8854");

      SimpleSmtpServer server = SimpleSmtpServer.start(8854);
      try {
         prov.notify(ExecutionStatus.POSTPONED, c);

         server.stop();

         Assert.assertTrue(server.getReceivedEmailSize() == 1);
         Iterator<SmtpMessage> emailIter = server.getReceivedEmail();
         SmtpMessage email = emailIter.next();
         Assert.assertEquals("Cibet Notification: POSTPONED", email.getHeaderValue("Subject"));
         Assert.assertEquals("from@test.de", email.getHeaderValue("From"));
         Assert.assertEquals("approv@test.de", email.getHeaderValue("To"));

         log.debug(email.getBody());

         String exp = "Hello approvalUser,\n\n"
               + "A business case under dual control has been assigned to you for final approval. You may release or reject the case. Please visit the dialogue for releasing/rejecting.\n"
               + "The dual controlled business case is registered under id: 123 (case id: test-caseid)\n\n"
               + "control event:             DELETE\n"
               + "controlled target:         com.cibethelper.entities.TEntity (executePay)\n"
               + "initiated by:              userId on Fri Sep 20 16:13:22 CEST 2013\n"
               + "first released by:         firstApprovalUserId on Sun Sep 22 16:13:22 CEST 2013\n\n\n"
               + "thanks for your attention\n" + "Cibet (http://www.logitags.com/cibet)";
         Assert.assertEquals(exp, email.getBody());
      } finally {
         server.stop();
      }
   }

   @Test
   public void notifyAssign3WithRemarks() {
      log.debug("start notifyAssign3WithRemarks()");
      DcControllable c = createDcControllable(EjbResourceHandler.class, ExecutionStatus.POSTPONED);
      c.setCreateRemark("Hase");
      c.setFirstApprovalRemark("Igel");
      c.getResource().setMethod("executePay");
      EmailNotificationProvider prov = new EmailNotificationProvider();
      prov.setFrom("from@test.de");
      prov.setSmtpHost("localhost");
      prov.setSmtpPort("8854");

      SimpleSmtpServer server = SimpleSmtpServer.start(8854);
      try {
         prov.notify(ExecutionStatus.POSTPONED, c);

         server.stop();

         Assert.assertTrue(server.getReceivedEmailSize() == 1);
         Iterator<SmtpMessage> emailIter = server.getReceivedEmail();
         SmtpMessage email = emailIter.next();
         Assert.assertEquals("Cibet Notification: POSTPONED", email.getHeaderValue("Subject"));
         Assert.assertEquals("from@test.de", email.getHeaderValue("From"));
         Assert.assertEquals("approv@test.de", email.getHeaderValue("To"));

         log.debug(email.getBody());

         String exp = "Hello approvalUser,\n\n"
               + "A business case under dual control has been assigned to you for final approval. You may release or reject the case. Please visit the dialogue for releasing/rejecting.\n"
               + "The dual controlled business case is registered under id: 123 (case id: test-caseid)\n\n"
               + "control event:             DELETE\n"
               + "controlled target:         com.cibethelper.entities.TEntity (executePay)\n"
               + "initiated by:              userId on Fri Sep 20 16:13:22 CEST 2013\n"
               + "creation remark:           Hase\n"
               + "first released by:         firstApprovalUserId on Sun Sep 22 16:13:22 CEST 2013\n"
               + "remark:                    Igel\n\n\n" + "thanks for your attention\n"
               + "Cibet (http://www.logitags.com/cibet)";
         Assert.assertEquals(exp, email.getBody());
      } finally {
         server.stop();
      }
   }

   @Test
   public void notifyFirstAssign() {
      log.debug("start notifyFirstAssign()");
      DcControllable c = createDcControllable(JpaResourceHandler.class, ExecutionStatus.FIRST_POSTPONED);
      EmailNotificationProvider prov = new EmailNotificationProvider();
      prov.setFrom("from@test.de");
      prov.setSmtpHost("localhost");
      prov.setSmtpPort("8854");

      SimpleSmtpServer server = SimpleSmtpServer.start(8854);
      try {
         prov.notify(ExecutionStatus.FIRST_POSTPONED, c);

         server.stop();

         Assert.assertTrue(server.getReceivedEmailSize() == 1);
         Iterator<SmtpMessage> emailIter = server.getReceivedEmail();
         SmtpMessage email = emailIter.next();
         Assert.assertEquals("Cibet Notification: FIRST_POSTPONED", email.getHeaderValue("Subject"));
         Iterator<String> it = email.getHeaderNames();
         while (it.hasNext()) {
            String key = it.next();
            log.debug(key + " = " + email.getHeaderValue(key));
         }
         Assert.assertEquals("from@test.de", email.getHeaderValue("From"));
         Assert.assertEquals("firstApp@test.de", email.getHeaderValue("To"));
         log.debug(email.getBody());

         String exp = "Hello approvalUser,\n\n"
               + "A business case under 6-eyes dual control has been assigned to you for a first approval. You may release or reject the case. Please visit the dialogue for releasing/rejecting.\n"
               + "The dual controlled business case is registered under id: 123 (case id: test-caseid)\n\n"
               + "control event:             DELETE\n"
               + "controlled target:         com.cibethelper.entities.TEntity \n"
               + "initiated by:              userId on Fri Sep 20 16:13:22 CEST 2013\n\n\n"
               + "thanks for your attention\n" + "Cibet (http://www.logitags.com/cibet)";
         Assert.assertEquals(exp, email.getBody());
      } finally {
         server.stop();
      }
   }

   @Test
   public void notifyFirstAssign2() {
      log.debug("start notifyFirstAssign2()");
      DcControllable c = createDcControllable(MethodResourceHandler.class, ExecutionStatus.FIRST_POSTPONED);
      c.getResource().setMethod("executePay");

      EmailNotificationProvider prov = new EmailNotificationProvider();
      prov.setFrom("from@test.de");
      prov.setSmtpHost("localhost");
      prov.setSmtpPort("8854");

      SimpleSmtpServer server = SimpleSmtpServer.start(8854);
      try {
         prov.notify(ExecutionStatus.FIRST_POSTPONED, c);

         server.stop();

         Assert.assertTrue(server.getReceivedEmailSize() == 1);
         Iterator<SmtpMessage> emailIter = server.getReceivedEmail();
         SmtpMessage email = emailIter.next();
         Assert.assertEquals("Cibet Notification: FIRST_POSTPONED", email.getHeaderValue("Subject"));
         Iterator<String> it = email.getHeaderNames();
         while (it.hasNext()) {
            String key = it.next();
            log.debug(key + " = " + email.getHeaderValue(key));
         }
         Assert.assertEquals("from@test.de", email.getHeaderValue("From"));
         Assert.assertEquals("firstApp@test.de", email.getHeaderValue("To"));
         log.debug(email.getBody());

         String exp = "Hello approvalUser,\n\n"
               + "A business case under 6-eyes dual control has been assigned to you for a first approval. You may release or reject the case. Please visit the dialogue for releasing/rejecting.\n"
               + "The dual controlled business case is registered under id: 123 (case id: test-caseid)\n\n"
               + "control event:             DELETE\n"
               + "controlled target:         com.cibethelper.entities.TEntity (executePay)\n"
               + "initiated by:              userId on Fri Sep 20 16:13:22 CEST 2013\n\n\n"
               + "thanks for your attention\n" + "Cibet (http://www.logitags.com/cibet)";
         Assert.assertEquals(exp, email.getBody());
      } finally {
         server.stop();
      }
   }

   @Test
   public void notifyFirstReleased() {
      log.debug("start notifyFirstReleased()");
      DcControllable c = createDcControllable(JpaResourceHandler.class, ExecutionStatus.FIRST_RELEASED);
      EmailNotificationProvider prov = new EmailNotificationProvider();
      prov.setFrom("from@test.de");
      prov.setSmtpHost("localhost");
      prov.setSmtpPort("8854");

      SimpleSmtpServer server = SimpleSmtpServer.start(8854);
      try {
         prov.notify(ExecutionStatus.FIRST_RELEASED, c);

         server.stop();

         Assert.assertTrue(server.getReceivedEmailSize() == 1);
         Iterator<SmtpMessage> emailIter = server.getReceivedEmail();
         SmtpMessage email = emailIter.next();
         Assert.assertEquals("Cibet Notification: FIRST_RELEASED", email.getHeaderValue("Subject"));
         Assert.assertEquals("from@test.de", email.getHeaderValue("From"));
         Assert.assertEquals("create@test.de", email.getHeaderValue("To"));
         log.debug(email.getBody());

         String exp = "Hello userId,\n\n"
               + "A business case under 6-eyes dual control has been released by the first approval user.\n"
               + "The dual controlled business case is registered under id: 123 (case id: test-caseid)\n\n"
               + "control event:             DELETE\n"
               + "controlled target:         com.cibethelper.entities.TEntity \n"
               + "initiated by:              userId on Fri Sep 20 16:13:22 CEST 2013\n"
               + "first released by:         firstApprovalUserId on Sun Sep 22 16:13:22 CEST 2013\n\n\n"
               + "thanks for your attention\n" + "Cibet (http://www.logitags.com/cibet)";
         Assert.assertEquals(exp, email.getBody());
      } finally {
         server.stop();
      }
   }

   @Test
   public void notifyFirstReleased2() {
      log.debug("start notifyFirstReleased2()");
      DcControllable c = createDcControllable(EjbResourceHandler.class, ExecutionStatus.FIRST_RELEASED);
      c.getResource().setMethod("executePay");

      EmailNotificationProvider prov = new EmailNotificationProvider();
      prov.setFrom("from@test.de");
      prov.setSmtpHost("localhost");
      prov.setSmtpPort("8854");

      SimpleSmtpServer server = SimpleSmtpServer.start(8854);
      try {
         prov.notify(ExecutionStatus.FIRST_RELEASED, c);

         server.stop();

         Assert.assertTrue(server.getReceivedEmailSize() == 1);
         Iterator<SmtpMessage> emailIter = server.getReceivedEmail();
         SmtpMessage email = emailIter.next();
         Assert.assertEquals("Cibet Notification: FIRST_RELEASED", email.getHeaderValue("Subject"));
         Assert.assertEquals("from@test.de", email.getHeaderValue("From"));
         Assert.assertEquals("create@test.de", email.getHeaderValue("To"));
         log.debug(email.getBody());

         String exp = "Hello userId,\n\n"
               + "A business case under 6-eyes dual control has been released by the first approval user.\n"
               + "The dual controlled business case is registered under id: 123 (case id: test-caseid)\n\n"
               + "control event:             DELETE\n"
               + "controlled target:         com.cibethelper.entities.TEntity (executePay)\n"
               + "initiated by:              userId on Fri Sep 20 16:13:22 CEST 2013\n"
               + "first released by:         firstApprovalUserId on Sun Sep 22 16:13:22 CEST 2013\n\n\n"
               + "thanks for your attention\n" + "Cibet (http://www.logitags.com/cibet)";
         Assert.assertEquals(exp, email.getBody());
      } finally {
         server.stop();
      }
   }

   @Test
   public void notifyRejected() {
      log.debug("start notifyRejected()");
      DcControllable c = createDcControllable(JpaResourceHandler.class, ExecutionStatus.REJECTED);
      EmailNotificationProvider prov = new EmailNotificationProvider();
      prov.setFrom("from@test.de");
      prov.setSmtpHost("localhost");
      prov.setSmtpPort("8854");

      SimpleSmtpServer server = SimpleSmtpServer.start(8854);
      try {
         prov.notify(ExecutionStatus.REJECTED, c);

         server.stop();

         Assert.assertTrue(server.getReceivedEmailSize() == 1);
         Iterator<SmtpMessage> emailIter = server.getReceivedEmail();
         SmtpMessage email = emailIter.next();
         Assert.assertEquals("Cibet Notification: REJECTED", email.getHeaderValue("Subject"));
         Assert.assertEquals("from@test.de", email.getHeaderValue("From"));
         Assert.assertEquals("create@test.de", email.getHeaderValue("To"));
         log.debug(email.getBody());

         String exp = "Hello userId,\n\n" + "A business case under dual control has been rejected.\n"
               + "The dual controlled business case is registered under id: 123 (case id: test-caseid)\n\n"
               + "control event:             DELETE\n"
               + "controlled target:         com.cibethelper.entities.TEntity \n"
               + "initiated by:              userId on Fri Sep 20 16:13:22 CEST 2013\n"
               + "first released by:         firstApprovalUserId on Sun Sep 22 16:13:22 CEST 2013\n"
               + "rejected by:               approvalUser on Wed Sep 25 16:13:22 CEST 2013\n\n\n"
               + "thanks for your attention\n" + "Cibet (http://www.logitags.com/cibet)";
         Assert.assertEquals(exp, email.getBody());
      } finally {
         server.stop();
      }
   }

   @Test
   public void notifyRejected2() {
      log.debug("start notifyRejected2()");
      DcControllable c = createDcControllable(MethodResourceHandler.class, ExecutionStatus.REJECTED);
      c.setFirstApprovalUser(null);
      c.setFirstApprovalAddress(null);
      c.setFirstApprovalDate(null);
      c.getResource().setMethod("executePay");

      EmailNotificationProvider prov = new EmailNotificationProvider();
      prov.setFrom("from@test.de");
      prov.setSmtpHost("localhost");
      prov.setSmtpPort("8854");

      SimpleSmtpServer server = SimpleSmtpServer.start(8854);
      try {
         prov.notify(ExecutionStatus.REJECTED, c);

         server.stop();

         Assert.assertTrue(server.getReceivedEmailSize() == 1);
         Iterator<SmtpMessage> emailIter = server.getReceivedEmail();
         SmtpMessage email = emailIter.next();
         Assert.assertEquals("Cibet Notification: REJECTED", email.getHeaderValue("Subject"));
         Assert.assertEquals("from@test.de", email.getHeaderValue("From"));
         Assert.assertEquals("create@test.de", email.getHeaderValue("To"));
         log.debug(email.getBody());

         String exp = "Hello userId,\n\n" + "A business case under dual control has been rejected.\n"
               + "The dual controlled business case is registered under id: 123 (case id: test-caseid)\n\n"
               + "control event:             DELETE\n"
               + "controlled target:         com.cibethelper.entities.TEntity (executePay)\n"
               + "initiated by:              userId on Fri Sep 20 16:13:22 CEST 2013\n"
               + "rejected by:               approvalUser on Wed Sep 25 16:13:22 CEST 2013\n\n\n"
               + "thanks for your attention\n" + "Cibet (http://www.logitags.com/cibet)";
         Assert.assertEquals(exp, email.getBody());
      } finally {
         server.stop();
      }
   }

   @Test
   public void notifyReleased() {
      log.debug("start notifyReleased()");
      DcControllable c = createDcControllable(JpaResourceHandler.class, ExecutionStatus.EXECUTED);

      EmailNotificationProvider prov = new EmailNotificationProvider();
      prov.setFrom("from@test.de");
      prov.setSmtpHost("localhost");
      prov.setSmtpPort("8854");

      SimpleSmtpServer server = SimpleSmtpServer.start(8854);
      try {
         prov.notify(ExecutionStatus.EXECUTED, c);

         server.stop();

         Assert.assertTrue(server.getReceivedEmailSize() == 1);
         Iterator<SmtpMessage> emailIter = server.getReceivedEmail();
         SmtpMessage email = emailIter.next();
         Assert.assertEquals("Cibet Notification: EXECUTED", email.getHeaderValue("Subject"));
         Assert.assertEquals("from@test.de", email.getHeaderValue("From"));
         Assert.assertEquals("create@test.de", email.getHeaderValue("To"));
         log.debug(email.getBody());

         String exp = "Hello userId,\n\n" + "A business case under dual control has been finally released.\n"
               + "The dual controlled business case is registered under id: 123 (case id: test-caseid)\n\n"
               + "control event:             DELETE\n"
               + "controlled target:         com.cibethelper.entities.TEntity \n"
               + "initiated by:              userId on Fri Sep 20 16:13:22 CEST 2013\n"
               + "first released by:         firstApprovalUserId on Sun Sep 22 16:13:22 CEST 2013\n"
               + "released by:               approvalUser on Wed Sep 25 16:13:22 CEST 2013\n\n\n"
               + "thanks for your attention\n" + "Cibet (http://www.logitags.com/cibet)";
         Assert.assertEquals(exp, email.getBody());
      } finally {
         server.stop();
      }
   }

   @Test
   public void notifyReleased2() {
      log.debug("start notifyReleased2()");
      DcControllable c = createDcControllable(EjbResourceHandler.class, ExecutionStatus.EXECUTED);
      c.setFirstApprovalUser(null);
      c.setFirstApprovalAddress(null);
      c.setFirstApprovalDate(null);
      c.getResource().setMethod("executePay");

      EmailNotificationProvider prov = new EmailNotificationProvider();
      prov.setFrom("from@test.de");
      prov.setSmtpHost("localhost");
      prov.setSmtpPort("8854");

      SimpleSmtpServer server = SimpleSmtpServer.start(8854);
      try {
         prov.notify(ExecutionStatus.EXECUTED, c);

         server.stop();

         Assert.assertTrue(server.getReceivedEmailSize() == 1);
         Iterator<SmtpMessage> emailIter = server.getReceivedEmail();
         SmtpMessage email = emailIter.next();
         Assert.assertEquals("Cibet Notification: EXECUTED", email.getHeaderValue("Subject"));
         Assert.assertEquals("from@test.de", email.getHeaderValue("From"));
         Assert.assertEquals("create@test.de", email.getHeaderValue("To"));
         log.debug(email.getBody());

         String exp = "Hello userId,\n\n" + "A business case under dual control has been finally released.\n"
               + "The dual controlled business case is registered under id: 123 (case id: test-caseid)\n\n"
               + "control event:             DELETE\n"
               + "controlled target:         com.cibethelper.entities.TEntity (executePay)\n"
               + "initiated by:              userId on Fri Sep 20 16:13:22 CEST 2013\n"
               + "released by:               approvalUser on Wed Sep 25 16:13:22 CEST 2013\n\n\n"
               + "thanks for your attention\n" + "Cibet (http://www.logitags.com/cibet)";
         Assert.assertEquals(exp, email.getBody());
      } finally {
         server.stop();
      }
   }

   @Test
   public void notifyCustomTemplate() throws Exception {
      log.debug("start notifyCustomTemplate()");
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      URL url = loader.getResource("ASSIGNED-emailbody_.vm");
      URI uri = url.toURI();
      File f = new File(uri);

      url = loader.getResource("ASSIGNED-emailsubject_.vm");
      uri = url.toURI();
      File f2 = new File(uri);
      Path subjPath = null;
      Path bodyPath = null;

      SimpleSmtpServer server = SimpleSmtpServer.start(8854);
      try {
         File newBody = new File(f.getParentFile(), "POSTPONED-emailbody.vm");
         File newSubject = new File(f2.getParentFile(), "POSTPONED-emailsubject.vm");
         log.debug("path: " + newSubject.getPath());
         subjPath = newSubject.toPath();
         bodyPath = newBody.toPath();
         Files.copy(f.toPath(), bodyPath);
         Files.copy(f2.toPath(), subjPath);

         DcControllable c = createDcControllable(JpaResourceHandler.class, ExecutionStatus.POSTPONED);
         EmailNotificationProvider prov = new EmailNotificationProvider();
         prov.setFrom("from@test.de");
         prov.setSmtpHost("localhost");
         prov.setSmtpPort("8854");
         Context.sessionScope().setUser("Willi");
         prov.notify(ExecutionStatus.POSTPONED, c);

         server.stop();

         Assert.assertTrue(server.getReceivedEmailSize() == 1);
         Iterator<SmtpMessage> emailIter = server.getReceivedEmail();
         SmtpMessage email = emailIter.next();
         Assert.assertEquals("Custom Cibet Notification", email.getHeaderValue("Subject"));
         log.debug(email.getBody());

         String exp = "actuator: FOUR_EYES\n" + "controlEvent: DELETE\n" + "tenant: tenant\n"
               + "sessionScope.user: Willi\n" + "notificationType: POSTPONED";
         Assert.assertEquals(exp, email.getBody());

      } finally {
         log.debug(Files.deleteIfExists(subjPath));
         log.debug(Files.deleteIfExists(bodyPath));
         server.stop();
      }
   }

   @Test
   public void notifyReleased2WithPassword() {
      log.debug("start notifyReleased2WithPassword()");
      DcControllable c = createDcControllable(EjbResourceHandler.class, ExecutionStatus.EXECUTED);
      c.setFirstApprovalUser(null);
      c.setFirstApprovalAddress(null);
      c.setFirstApprovalDate(null);
      c.getResource().setMethod("executePay");

      EmailNotificationProvider prov = new EmailNotificationProvider();
      prov.setFrom("from@test.de");
      prov.setSmtpHost("localhost");
      prov.setSmtpPort("8854");
      prov.setSmtpUser("Jacky");
      prov.setSmtpPassword("secret");
      Assert.assertEquals("Jacky", prov.getSmtpUser());
      Assert.assertEquals("secret", prov.getSmtpPassword());

      SimpleSmtpServer server = SimpleSmtpServer.start(8854);
      try {
         prov.notify(ExecutionStatus.EXECUTED, c);

         server.stop();

         Assert.assertTrue(server.getReceivedEmailSize() == 1);
         Iterator<SmtpMessage> emailIter = server.getReceivedEmail();
         SmtpMessage email = emailIter.next();
         Assert.assertEquals("Cibet Notification: EXECUTED", email.getHeaderValue("Subject"));
         Assert.assertEquals("from@test.de", email.getHeaderValue("From"));
         Assert.assertEquals("create@test.de", email.getHeaderValue("To"));
         log.debug(email.getBody());

         String exp = "Hello userId,\n\n" + "A business case under dual control has been finally released.\n"
               + "The dual controlled business case is registered under id: 123 (case id: test-caseid)\n\n"
               + "control event:             DELETE\n"
               + "controlled target:         com.cibethelper.entities.TEntity (executePay)\n"
               + "initiated by:              userId on Fri Sep 20 16:13:22 CEST 2013\n"
               + "released by:               approvalUser on Wed Sep 25 16:13:22 CEST 2013\n\n\n"
               + "thanks for your attention\n" + "Cibet (http://www.logitags.com/cibet)";
         Assert.assertEquals(exp, email.getBody());
      } finally {
         server.stop();
      }
   }

}
