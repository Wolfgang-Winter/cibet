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
package com.cibethelper;

import org.springframework.security.access.prepost.PreAuthorize;

public interface SpringTestInterface {

   @PreAuthorize("hasRole('ROLE_WALTER')")
   String giveFive();

}
