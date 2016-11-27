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
package com.logitags.cibet.core;

public enum ExecutionStatus {

	/**
	 * the event is currently executing and has not finished yet.
	 */
	EXECUTING,

	/**
	 * the event has been executed
	 */
	EXECUTED,

	/**
	 * the event is not executed but postponed for a first release by a releasing
	 * user. A 6-eyes business case has been postponed and assigned to a first
	 * user for approval.
	 */
	FIRST_POSTPONED,

	/**
	 * the event is not executed but postponed.
	 */
	POSTPONED,

	/**
	 * a postponed event is not executed due to a user rejection
	 */
	REJECTED,

	/**
	 * a postponed event is not executed but passed back to the originator of the
	 * event
	 */
	PASSEDBACK,

	/**
	 * a postponed event has been released by a first user but is still postponed
	 */
	FIRST_RELEASED,

	/**
	 * the event is not executed. Authorization denied.
	 */
	DENIED,

	/**
	 * the event could not be executed due to an error.
	 */
	ERROR,

	/**
	 * the event has been scheduled for a later time
	 */
	SCHEDULED,

	/**
	 * the event has been shed due to system overload
	 */
	SHED,

	/**
	 * the event has been timed out.
	 */
	TIMEOUT,

}
