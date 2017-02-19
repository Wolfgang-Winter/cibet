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

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;

public class SpringTestAuthenticationManager implements AuthenticationProvider {

   private List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();

   public SpringTestAuthenticationManager(List<GrantedAuthority> auths) {
      authorities = auths;
   }

   public SpringTestAuthenticationManager() {
   }

   public void addAuthority(String role) {
      authorities.add(new GrantedAuthorityImpl(role));
   }

   public Authentication authenticate(Authentication auth) throws AuthenticationException {
      if (auth.getName().equals(auth.getCredentials())) {
         return new UsernamePasswordAuthenticationToken(auth.getName(), auth.getCredentials(), authorities);
      }
      throw new BadCredentialsException("Bad Credentials");
   }

   @Override
   public boolean supports(Class<? extends Object> authentication) {
      return true;
   }

}
