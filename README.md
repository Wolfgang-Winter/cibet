Summary
----------------------------
Cibet is a framework for easily introducing cross-cutting functionality in a non-intrusive way. The framework is based on control 
theory principles and allows adding numerous pre-defined cross-cutting functionality at various integration points of an application, e.g
on methods, EJBs, http requests, database queries, JPA on client and server side.
It integrates and extends frameworks like JPA, Spring Security, Apache Shiro, Hibernate.

Pre-defined functionality includes topics as

- archiving and historization
- security
- various dual control mechanisms like four-eyes control
- monitoring
- resilience patterns
- scheduling
- locking  


Please visit web site http://www.logitags.com/cibet for detailed information.

Releases
----------------------------
Release 2.0 (2.7.2017)

  Eventually a major release which restructures the project into several sub-projects. This has been done to better support different 
  versions of integrated frameworks. Furthermore, the Cibet API and the database model have been streamlined, the integration tests have 
  been completely migrated
  to Arquillian and the project has been moved to {{{https://github.com/Wolfgang-Winter/cibet}Github}}. Thus, this release incorporates
  only few new features and bugfixes, but does change a lot behind the scenes which justifies an increase in the major release number.   
