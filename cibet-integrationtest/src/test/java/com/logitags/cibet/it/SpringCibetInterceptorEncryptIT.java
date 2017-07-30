/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 *******************************************************************************
 */
package com.logitags.cibet.it;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;

import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import com.cibethelper.SpringTestAuthenticationManager;
import com.cibethelper.base.CoreTestBase;
import com.cibethelper.ejb.CibetTestEJB;
import com.cibethelper.ejb.RemoteEJB;
import com.cibethelper.ejb.RemoteEJBImpl;
import com.cibethelper.ejb.SecuredRemoteEJBImpl;
import com.cibethelper.ejb.SimpleEjb;
import com.cibethelper.entities.AbstractTEntity;
import com.cibethelper.entities.ITComplexEntity;
import com.cibethelper.entities.TCompareEntity;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TComplexEntity2;
import com.cibethelper.entities.TEntity;
import com.cibethelper.servlet.ArquillianTestServlet1;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.actuator.common.DeniedException;
import com.logitags.cibet.actuator.dc.FourEyesActuator;
import com.logitags.cibet.actuator.springsecurity.SpringSecurityActuator;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.ConfigurationService;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.security.DefaultSecurityProvider;

@RunWith(Arquillian.class)
public class SpringCibetInterceptorEncryptIT extends AbstractArquillian {

   private static Logger log = Logger.getLogger(SpringCibetInterceptorEncryptIT.class);

   @EJB
   private CibetTestEJB ejb;

   @Deployment
   public static WebArchive createDeployment() {
      String warName = SpringCibetInterceptorEncryptIT.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("it/web2.xml");

      archive.addClasses(AbstractArquillian.class, CoreTestBase.class, AbstractTEntity.class, TEntity.class,
            TComplexEntity.class, TComplexEntity2.class, ITComplexEntity.class, TCompareEntity.class,
            CibetTestEJB.class, SpringTestAuthenticationManager.class, ArquillianTestServlet1.class, RemoteEJB.class,
            RemoteEJBImpl.class, SecuredRemoteEJBImpl.class, SimpleEjb.class);

      File[] cibet = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-jpa").withoutTransitivity()
            .asFile();
      archive.addAsLibraries(cibet);
      File[] spring = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-springsecurity")
            .withTransitivity().asFile();
      archive.addAsLibraries(spring);

      archive.addAsWebInfResource("META-INF/persistence-it.xml", "classes/META-INF/persistence.xml");
      archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
      archive.addAsWebInfResource("spring-context_1.xml", "classes/spring-context.xml");

      log.debug(archive.toString(true));
      archive.as(ZipExporter.class).exportTo(new File("target/" + warName), true);

      return archive;
   }

   @Before
   public void beforeSpringCibetInterceptorEncryptIT() {
      Context.start();
      Context.sessionScope().setUser(USER);
      Context.sessionScope().setTenant(TENANT);
      cman = Configuration.instance();

      DefaultSecurityProvider sprov = new DefaultSecurityProvider();
      sprov.getSecrets().put("checkIntegrityMethodArchive", "2366Au37nBB.0ya?");
      sprov.setCurrentSecretKey("checkIntegrityMethodArchive");
      cman.registerSecurityProvider(sprov);

      ArchiveActuator archa = (ArchiveActuator) cman.getActuator(ArchiveActuator.DEFAULTNAME);
      archa.setEncrypt(true);

      FourEyesActuator ff = (FourEyesActuator) cman.getActuator(FourEyesActuator.DEFAULTNAME);
      ff.setEncrypt(true);
      new ClassPathXmlApplicationContext(new String[] { "spring-context.xml" });
   }

   @After
   public void afterSpringCibetInterceptorEncryptIT() {
      Context.end();
      SecurityContextHolder.getContext().setAuthentication(null);
      new ConfigurationService().initialise();

      ArchiveActuator archa = (ArchiveActuator) cman.getActuator(ArchiveActuator.DEFAULTNAME);
      archa.setEncrypt(false);

      FourEyesActuator ff = (FourEyesActuator) cman.getActuator(FourEyesActuator.DEFAULTNAME);
      ff.setEncrypt(false);
   }

   protected void authenticate(String... roles) throws AuthenticationException {
      SpringTestAuthenticationManager authManager = new SpringTestAuthenticationManager();
      for (String role : roles) {
         if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
         }
         authManager.addAuthority(role);
      }

      Authentication request = new UsernamePasswordAuthenticationToken("test", "test");
      Authentication result = authManager.authenticate(request);
      SecurityContextHolder.getContext().setAuthentication(result);
   }

   @Test
   public void interceptReleaseDenied() throws Exception {
      log.info("start interceptReleaseDenied()");
      ApplicationContext context = new ClassPathXmlApplicationContext(new String[] { "spring-context.xml" });

      SpringSecurityActuator act = (SpringSecurityActuator) cman.getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(SpringSecurityActuator.DEFAULTNAME);
      registerSetpoint(CibetTestEJB.class, schemes, "testInvoke", ControlEvent.INVOKE);

      Thread.sleep(100);
      SpringSecurityActuator act2 = new SpringSecurityActuator("SPRING2");
      act2.setPreAuthorize("hasRole(ADMIN)");
      cman.registerActuator(act2);

      List<String> schemes2 = new ArrayList<String>();
      schemes2.add(FourEyesActuator.DEFAULTNAME);
      schemes2.add(act2.getName());
      registerSetpoint(CibetTestEJB.class, schemes2, "testInvoke", ControlEvent.RELEASE);

      authenticate("WALTER");

      TEntity entity = createTEntity(12, "�sal");
      byte[] bytes = "Pausenclown".getBytes();
      List<Object> list = ejb.testInvoke("H�ls", -34, 456, bytes, entity, new Long(43));
      Assert.assertNull(list);

      // release
      List<Controllable> l = ejb.findUnreleased();
      Assert.assertEquals(1, l.size());

      Context.sessionScope().setUser("test2");
      List<Object> resultList = (List<Object>) ejb.release(l.get(0), "Happy New Year");
      EventResult evResult = Context.requestScope().getExecutedEventResult();
      Assert.assertNull(evResult.getParentResult());
      List<EventResult> childs = evResult.getChildResults();
      Assert.assertEquals(1, childs.size());
      Assert.assertEquals(ExecutionStatus.DENIED, childs.get(0).getExecutionStatus());
      Context.sessionScope().setUser(USER);
      Assert.assertNull(resultList);

      List<Controllable> list2 = ejb.queryControllable();
      Assert.assertEquals(1, list2.size());
   }

   @Test
   public void interceptReleaseDeniedException() throws Exception {
      log.info("start interceptReleaseDeniedException()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context.xml" });

      SpringSecurityActuator act = (SpringSecurityActuator) cman.getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(SpringSecurityActuator.DEFAULTNAME);
      registerSetpoint(CibetTestEJB.class, schemes, "testInvoke", ControlEvent.INVOKE);

      Thread.sleep(100);
      SpringSecurityActuator act2 = new SpringSecurityActuator("SPRING2");
      act2.setPreAuthorize("hasRole(ADMIN)");
      act2.setThrowDeniedException(true);
      cman.registerActuator(act2);

      List<String> schemes2 = new ArrayList<String>();
      schemes2.add(FourEyesActuator.DEFAULTNAME);
      schemes2.add(act2.getName());
      registerSetpoint(CibetTestEJB.class, schemes2, "testInvoke", ControlEvent.RELEASE);

      authenticate("WALTER");

      TEntity entity = createTEntity(45, "Ober");
      byte[] bytes = "Pausenclown".getBytes();
      List<Object> list = ejb.testInvoke("H�ls", -34, 456, bytes, entity, new Long(43));
      Assert.assertNull(list);

      // release
      List<Controllable> l = ejb.findUnreleased();
      Assert.assertEquals(1, l.size());

      Context.sessionScope().setUser("test2");
      List<Object> resultList;
      try {
         resultList = (List<Object>) ejb.release(l.get(0), "Happy New Year");
         Assert.fail();
      } catch (DeniedException e) {
         log.warn("error: " + e.getMessage());
      }
      EventResult evResult = Context.requestScope().getExecutedEventResult();
      log.debug("evResult==" + evResult);

      Assert.assertTrue(evResult.getResource().startsWith("[EjbResource] "));
      Assert.assertTrue(evResult.getResource().indexOf("target: com.cibethelper.ejb.CibetTestEJB") > 0);
      Assert.assertTrue(evResult.getResource().indexOf("; method: release") > 0);
      Assert.assertEquals(1, evResult.getChildResults().size());
      Assert.assertEquals(ExecutionStatus.DENIED, evResult.getChildResults().get(0).getExecutionStatus());
      Context.sessionScope().setUser(USER);

      List<Controllable> list2 = ejb.queryControllable();
      Assert.assertEquals(1, list2.size());
   }

   @Test
   public void playInterceptReleaseAccepted() throws Exception {
      log.info("start playInterceptReleaseAccepted()");
      ApplicationContext context = new ClassPathXmlApplicationContext(new String[] { "spring-context.xml" });

      SpringSecurityActuator act = (SpringSecurityActuator) cman.getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(SpringSecurityActuator.DEFAULTNAME);
      registerSetpoint(CibetTestEJB.class, schemes, "testInvoke", ControlEvent.INVOKE);

      Thread.sleep(100);
      SpringSecurityActuator act2 = new SpringSecurityActuator("SPRING2");
      act2.setPreAuthorize("hasRole('WALTER')");
      cman.registerActuator(act2);

      List<String> schemes2 = new ArrayList<String>();
      schemes2.add(FourEyesActuator.DEFAULTNAME);
      schemes2.add(act2.getName());
      registerSetpoint(CibetTestEJB.class, schemes2, "testInvoke", ControlEvent.RELEASE);

      authenticate("WALTER");

      TEntity entity = createTEntity(12, "�sal");
      byte[] bytes = "Pausenclown".getBytes();
      List<Object> list = ejb.testInvoke("H�ls", -34, 456, bytes, entity, new Long(43));
      Assert.assertNull(list);

      // release
      List<Controllable> l = ejb.findUnreleased();
      Assert.assertEquals(1, l.size());

      Context.sessionScope().setUser("test2");
      List<Object> resultList = (List<Object>) ejb.playRelease(l.get(0), "Happy New Year");
      EventResult evResult = Context.requestScope().getExecutedEventResult();
      Assert.assertNull(evResult.getParentResult());
      List<EventResult> childs = evResult.getChildResults();
      Assert.assertEquals(1, childs.size());
      Assert.assertEquals(ExecutionStatus.EXECUTED, childs.get(0).getExecutionStatus());
      Context.sessionScope().setUser(USER);
      Assert.assertNull(resultList);

      List<Controllable> list2 = ejb.queryControllable();
      Assert.assertEquals(1, list2.size());
   }

}
