A. Build cibet web site
-----------------
1. execute on parent project: mvn post-site with profile=sitegeneration


D. Build cibet release
--------------------------------
1. in pom.xml of cibet-parent, cibet and ejbwar, site.xml: set correct version
2. in release-notes.apt: set correct version date, in site.xml set correct version of reference guide
3. copy reference guide pdf with correct version number from cibet-material to src/main/site/resources
4. svn commit --> commit everything to svn
5. set svn tag   
6. execute A.
7. copy target/site/cobertura to a temp dir
8. execute install with profile sign (SIGN - install Cibet (no test))
9. execute assemble Cibet (goal: initialize) with profile assembly
10. deploy bundle to Maven Central
    (see explanation on http://central.sonatype.org/pages/apache-maven.html)
    (https://oss.sonatype.org/index.html)
    mvn deploy with profile sign
11. Release Nexus staging repository: 
    mvn nexus-staging:release 
12. mvn clean install with profie jbossITJacoco (IT Test JBoss Jacoco)
13. mvn post-site with profiles sitegeneration, jbossITJacoco (site jacoco)
14. copy cobertura from temp to target/site
15. ftp upload of site
16. Upload zip to Sourceforge
17. Tell us about an update on Softpedia http://www.softpedia.com/get/Programming/Other-Programming-Files/Cibet.shtml


E. Execute external JMeter Performance test
--------------------------------
1. mvn clean install jmetertest
2. copy jmetertest-x.jar to C:\Java\jakarta-jmeter-2.5.1\lib\ext
3. start jmeter, load tests and execute 

F. Arquillian Tests
--------------------------------
1. JBoss/Hibernate/Derby

- start JBoss eap-6.4
- run mvn clean install with profile jboss, skipTests=true, db=derby on project cibet
- run mvn clean install with profile jboss, db=derby on project cibet-integration
      VM args: -Dslf4j=false -Dlog4j.configuration=log4j.properties

2. Tomcat/Eclipselink/MySQL/

- start Tomcat 7.0.34
- run mvn clean install with profile tomcat, skipTests=true, db=mysql on project cibet
- run mvn clean install with profile tomcat, db=mysql on project cibet-integration
      VM args: -Dslf4j=false -Dlog4j.configuration=log4j.properties
       
3. [Glassfish/EclipseLink/Oracle: start Glassfish on Bugarach]
   (Glassfish 3 not working with java 1.8) 
- run mvn clean install with profile glassfish3, skipTests=true, db=oracle on project cibet
- run mvn clean install with profile glassfish3, db=oracle on project cibet-integration
      VM args: -Dslf4j=false -Dlog4j.configuration=log4j.properties

   Execute mvn clean install with profile glassfishIT
   and VM args: -Dslf4j=false -Dlog4j.configuration=file:./target/test-classes/log4j.properties
   log files in c:/Java/glassfish3/glassfish/domains/cibet/logs
   If app is not redeployable due to message: Application with name ejbwar is already registered.
   - http://192.168.1.64:4848/
   - login with admin/x
   - undeploy application
   - copy ejbwar.war to Bugarach/c:/Java/glassfish3/glassfish/bin/
   - redeploy local file / Force Redeploy   
   stop Glassfish on Bugarach

3b. [Glassfish 4.1.1 on Lenny]
   (Glassfish 4 uses JPA 2.1, therefore AbstractMethodException CibetEntityManagerFactory.createEntityManager)
   - https://192.168.1.63:4848/

 
2. Tomee/OpenJPA/Oracle:
   - start Tomee D:\appserver\apache-tomee-webprofile-1.7.4\bin
   - execute clean test with profile tomeeIT 
      VM args: -javaagent:D:\Java\maven-repository\org\apache\openjpa\openjpa-all\2.4.2\openjpa-all-2.4.2.jar




G. Run non-junit test in debug mode
--------------------------------
create mvn configuration task with 
goals: -Dmaven.surefire.debug test
profiles: tomcatIT
parameter: forkMode never
in profile tomcatIT: define surefire plugin with <include>**/TomcatAIT.java</include>
start in Eclipse with Debug as ...


H. Generate JAXB classes
--------------------------------
1. Adapt schemaFiles cibet-config_??.xsd in cibet/pom.xml/jaxb2-maven-plugin
2. Delete classes in bindings package in Windows Explorer 
3. Execute Maven goal jaxb2:xjc on cibet

I. Oracle XE Database
--------------------------------
- log in with system / sys at localhost:8080
- log in to Windows as admin to use sqlplus console with
  CONNECT /as sysdba
- Start/Stop of Oracle: 
   - login to bugarach as admin
   - open Dienste
   - start/stop OracleServiceXE
     
J. Debug Arquillian JBoss JaCoCo Tests   
--------------------------------
- uncomment in arquillian.xml javaVmArguments property with agentlib:jdwp=transport=dt_socket,address=8787
- execute mvn clean install with profile jbossITJacoco as usual
- execute Remote Java Application 'Debug JBoss'   

K. Build cibet web site with JaCoCo reports
-----------------
1. execute B.
2. copy target/site/cobertura to a temp dir
3. mvn clean install with profil jbossITJacoco (IT Test JBoss Jacoco)
4. mvn post-site with profiles sitegeneration, jbossITJacoco (site jacoco)
5. copy cobertura from temp to target/site
