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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ExecutionStatus;

/**
 * NotificationProvider that sends notifications as emails.
 * 
 * @author Wolfgang
 * 
 */
public class EmailNotificationProvider implements NotificationProvider, Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = -1872983240172846704L;

   /**
    * logger for tracing
    */
   private static Log log = LogFactory.getLog(EmailNotificationProvider.class);

   private static final String SUBJECT_TEMPLATE_SUFFIX = "-emailsubject.vm";
   private static final String BODY_TEMPLATE_SUFFIX = "-emailbody.vm";
   private static final String SUBJECT_DEFAULT_TEMPLATE_SUFFIX = "default-emailsubject.vm";
   private static final String BODY_DEFAULT_TEMPLATE_SUFFIX = "-default-emailbody.vm";

   private String smtpHost;

   private String smtpPort;

   private String smtpUser;

   private String smtpPassword;

   private String from;

   private transient Session session;

   private transient VelocityEngine velocity = new VelocityEngine();

   private static Map<String, String> templates = Collections.synchronizedMap(new HashMap<String, String>());

   public EmailNotificationProvider() {
      velocity = new VelocityEngine();
      velocity.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM_CLASS, VelocityLogger.class.getName());
      velocity.init();
      initTemplates();
   }

   @Override
   public void notify(ExecutionStatus status, Controllable controllable) {
      createSession();
      try {
         MimeMessage msg = new MimeMessage(session);
         msg.setSentDate(new Date());
         msg.setFrom(new InternetAddress(from));

         switch (status) {
         case POSTPONED:
            msg.setRecipients(Message.RecipientType.TO, controllable.getReleaseAddress());
            break;
         case FIRST_POSTPONED:
            msg.setRecipients(Message.RecipientType.TO, controllable.getFirstApprovalAddress());
            break;
         case FIRST_RELEASED:
         case REJECTED:
         case EXECUTED:
         case PASSEDBACK:
            msg.setRecipients(Message.RecipientType.TO, controllable.getCreateAddress());
            break;
         default:
            log.info("no notification for event " + controllable.getExecutionStatus());
            return;
         }

         VelocityContext ctx = createVelocityContext(status, controllable);
         msg.setSubject(createContent(status.name() + SUBJECT_TEMPLATE_SUFFIX, ctx));
         msg.setText(createContent(status.name() + BODY_TEMPLATE_SUFFIX, ctx));

         Transport.send(msg);

      } catch (NoSuchProviderException e) {
         log.fatal(e.getMessage(), e);
         throw new RuntimeException(e);
      } catch (MessagingException e) {
         log.error(e.getMessage(), e);
         throw new RuntimeException(e);
      }

   }

   private void createSession() {
      if (session == null) {
         Properties props = new Properties();
         props.put("mail.smtp.host", smtpHost);
         props.put("mail.smtp.port", smtpPort);
         Authenticator authenticator = null;
         if (smtpUser != null) {
            props.put("mail.smtp.auth", true);
            authenticator = new Authenticator() {
               private PasswordAuthentication pa = new PasswordAuthentication(smtpUser, smtpPassword);

               @Override
               public PasswordAuthentication getPasswordAuthentication() {
                  return pa;
               }
            };
         }
         session = Session.getInstance(props, authenticator);
      }
   }

   private String createContent(String fileName, VelocityContext ctx) {
      String template = getTemplate(fileName);
      StringWriter writer = new StringWriter();
      velocity.evaluate(ctx, writer, fileName, template);
      return writer.toString();
   }

   private String getTemplate(String fileName) {
      String template = templates.get(fileName);
      if (template == null) {
         String err = "failed to find template for " + fileName + " in classpath";
         log.fatal(err);
         throw new RuntimeException(err);
      }

      return template;
   }

   public void initTemplates() {
      templates.clear();
      try {
         ClassLoader loader = Thread.currentThread().getContextClassLoader();
         InputStream in = loader.getResourceAsStream(SUBJECT_DEFAULT_TEMPLATE_SUFFIX);
         if (in == null) {
            String err = "failed to find " + SUBJECT_DEFAULT_TEMPLATE_SUFFIX + " in classpath";
            log.fatal(err);
            throw new RuntimeException(err);
         }
         String defaultSubject = IOUtils.toString(in);
         in.close();

         for (ExecutionStatus type : ExecutionStatus.values()) {
            String subjectName = type.name() + SUBJECT_TEMPLATE_SUFFIX;
            in = loader.getResourceAsStream(subjectName);
            if (in != null) {
               log.info("register subject file " + subjectName + " for " + subjectName);
               templates.put(subjectName, IOUtils.toString(in));
               in.close();
            } else {
               log.info("register subject file " + SUBJECT_DEFAULT_TEMPLATE_SUFFIX + " for " + subjectName);
               templates.put(subjectName, defaultSubject);
            }

            String template = "No email template found for event " + type;
            String bodyName = type.name() + BODY_TEMPLATE_SUFFIX;
            in = loader.getResourceAsStream(bodyName);
            if (in != null) {
               log.info("register body file " + bodyName + " for " + bodyName);
               template = IOUtils.toString(in);

            } else {
               in = loader.getResourceAsStream(type.name() + BODY_DEFAULT_TEMPLATE_SUFFIX);
               if (in == null) {
                  String err = "failed to find neither " + bodyName + " nor " + type.name()
                        + BODY_DEFAULT_TEMPLATE_SUFFIX + " in classpath";
                  log.info(err);
               } else {
                  log.info("register body file " + type.name() + BODY_DEFAULT_TEMPLATE_SUFFIX + " for " + bodyName);
                  template = IOUtils.toString(in);
               }
            }

            templates.put(bodyName, template);
            if (in != null)
               in.close();
         }

      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new RuntimeException(e);
      }
   }

   private VelocityContext createVelocityContext(ExecutionStatus type, Controllable c) {
      VelocityContext ctx = new VelocityContext();
      ctx.put("controllableId", c.getControllableId());
      ctx.put("firstApprovalUser", c.getFirstApprovalUser());
      ctx.put("firstApprovalDate", c.getFirstApprovalDate());
      ctx.put("firstApprovalAddress", c.getFirstApprovalAddress());
      ctx.put("firstApprovalRemark", c.getFirstApprovalRemark());
      ctx.put("releaseUser", c.getReleaseUser());
      ctx.put("releaseAddress", c.getReleaseAddress());
      ctx.put("releaseDate", c.getReleaseDate());
      ctx.put("releaseRemark", c.getReleaseRemark());
      ctx.put("actuator", c.getActuator());
      ctx.put("controlEvent", c.getControlEvent().name());
      ctx.put("createUser", c.getCreateUser());
      ctx.put("createDate", c.getCreateDate());
      ctx.put("createAddress", c.getCreateAddress());
      ctx.put("createRemark", c.getCreateRemark());
      ctx.put("tenant", c.getTenant());
      ctx.put("caseId", c.getCaseId());

      for (Entry<String, Object> entry : c.getResource().getNotificationAttributes().entrySet()) {
         ctx.put(entry.getKey(), entry.getValue());
      }

      ctx.put("sessionScope", Context.sessionScope());
      ctx.put("requestScope", Context.requestScope());
      ctx.put("applicationScope", Context.applicationScope());
      ctx.put("notificationType", type.name());
      return ctx;
   }

   /**
    * @return the smtpHost
    */
   public String getSmtpHost() {
      return smtpHost;
   }

   /**
    * @param smtpHost
    *           the smtpHost to set
    */
   public void setSmtpHost(String smtpHost) {
      this.smtpHost = smtpHost;
   }

   /**
    * @return the smtpPort
    */
   public String getSmtpPort() {
      return smtpPort;
   }

   /**
    * @param smtpPort
    *           the smtpPort to set
    */
   public void setSmtpPort(String smtpPort) {
      this.smtpPort = smtpPort;
   }

   /**
    * @return the smtpUser
    */
   public String getSmtpUser() {
      return smtpUser;
   }

   /**
    * @param smtpUser
    *           the smtpUser to set
    */
   public void setSmtpUser(String smtpUser) {
      this.smtpUser = smtpUser;
   }

   /**
    * @return the smtpPassword
    */
   public String getSmtpPassword() {
      return smtpPassword;
   }

   /**
    * @param smtpPassword
    *           the smtpPassword to set
    */
   public void setSmtpPassword(String smtpPassword) {
      this.smtpPassword = smtpPassword;
   }

   /**
    * @return the from
    */
   public String getFrom() {
      return from;
   }

   /**
    * @param from
    *           the from to set
    */
   public void setFrom(String from) {
      this.from = from;
   }
}
