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
package com.logitags.cibet.context;

public interface SessionScope extends ApplicationScope {

	/**
	 * Set the actual user
	 * 
	 * @param userId
	 */
	void setUser(String userId);

	/**
	 * Return the current user
	 * 
	 * @return
	 */
	String getUser();

	/**
	 * Return the address of the current user
	 * 
	 * @return
	 */
	String getUserAddress();

	/**
	 * Set the address of the current user.
	 * 
	 * @param address
	 */
	void setUserAddress(String address);

	/**
	 * Set the second authenticated user for TWO-MAN-RULE
	 * 
	 * @param user
	 */
	void setSecondUser(String user);

	/**
	 * Return the second authenticated user for TWO-MAN-RULE
	 * 
	 * @return
	 */
	String getSecondUser();

	/**
	 * Set the actual client / tenant.
	 * 
	 * @param tenant
	 */
	void setTenant(String tenant);

	/**
	 * Return the actual client / tenant.
	 * 
	 * @return
	 */
	String getTenant();

	/**
	 * get the user for release/reject
	 * 
	 * @return
	 */
	String getApprovalUser();

	/**
	 * set the user for release/reject
	 * 
	 * @param appUser
	 */
	void setApprovalUser(String appUser);

	/**
	 * get the address of the user for release/reject
	 * 
	 * @return
	 */
	String getApprovalAddress();

	/**
	 * set the address of the user for release/reject
	 * 
	 * @param appUser
	 */
	void setApprovalAddress(String appUser);

}
