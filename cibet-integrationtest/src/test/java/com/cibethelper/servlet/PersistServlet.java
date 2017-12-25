/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2016 Dr. Wolfgang Winter
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
package com.cibethelper.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.apache.log4j.Logger;

import com.cibethelper.ejb.EjbService;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.context.Context;

public class PersistServlet extends ShiroServlet {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private static Logger log = Logger.getLogger(PersistServlet.class);

   @PersistenceContext(unitName = "APPL-UNIT")
   protected EntityManager appEM;

   @Resource
   protected UserTransaction ut;

   @EJB
   private EjbService ejb;

   protected void persist(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      log.debug("persist: " + req.getSession().getId());
      log.debug("User: " + Context.sessionScope().getUser());

      try {
         ut.begin();
         TEntity te = new TEntity("Hansi name", 34, "hansis owner");
         appEM.persist(te);
         ut.commit();
      } catch (Exception e) {
         log.error(e.getMessage(), e);
      }

      PrintWriter writer = resp.getWriter();
      writer.print("Persist done");
      writer.close();
   }

   protected void logout(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      log.debug("invalidate session: " + req.getSession().getId());
      log.debug("User: " + Context.sessionScope().getUser());
      req.getSession().invalidate();
      Context.internalRequestScope().clear();
      Context.internalSessionScope().clear();

      PrintWriter writer = resp.getWriter();
      writer.print("Logout done");
      writer.close();
   }

   protected void persist2(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      log.debug("persist2: " + req.getSession().getId());
      log.debug("EM: " + Context.internalRequestScope().getOrCreateEntityManager(true));
      TEntity te = new TEntity("Rudi", 344, "Ganz");
      ejb.persist(te);

      PrintWriter writer = resp.getWriter();
      writer.print("Persist2 done");
      writer.close();
   }

}
