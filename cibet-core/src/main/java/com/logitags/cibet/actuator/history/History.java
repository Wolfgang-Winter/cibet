/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2014 Dr. Wolfgang Winter
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
package com.logitags.cibet.actuator.history;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.diff.Difference;

@Entity
@Table(name = "CIB_HISTORY")
@NamedQueries({
		@NamedQuery(name = History.SEL_ALL_BY_CASEID, query = "SELECT a FROM History a WHERE a.tenant LIKE :tenant AND a.caseId = :caseId ORDER BY a.createDate"),
		@NamedQuery(name = History.SEL_ALL_BY_TARGET_PK, query = "SELECT h FROM History h WHERE h.tenant LIKE :tenant AND h.target = :target AND h.primaryKeyId = :primaryKeyId ORDER BY h.createDate") })

public class History implements Serializable {

	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	private transient Log log = LogFactory.getLog(History.class);

	public final static String SEL_ALL_BY_CASEID = "com.logitags.cibet.actuator.history.History.SEL_ALL_BY_CASEID";
	public final static String SEL_ALL_BY_TARGET_PK = "com.logitags.cibet.actuator.history.History.SEL_ALL_BY_TARGET_PK";

	@Transient
	private ObjectMapper mapper = new ObjectMapper();

	/**
	 * unique ID
	 * 
	 */
	@Id
	private String historyId;

	/**
	 * optional comment by the user who is responsible for this History creation. (e.g. why a controlled object has been
	 * rejected)
	 */
	private String remark;

	/**
	 * the type of event that is requested on the resource.
	 */
	@Column(length = 50)
	@Enumerated(EnumType.STRING)
	private ControlEvent controlEvent;

	/**
	 * user id who initiated the control event
	 */
	@Column(length = 50)
	private String createUser;

	/**
	 * Date when the user initiated the control event
	 */
	@Temporal(TemporalType.TIMESTAMP)
	private Date createDate = new Date();

	/**
	 * tenant
	 */
	private String tenant;

	/**
	 * unique id that identifies the case. A case consists of related dual control data, INVOKE ... event and
	 * RELEASE/REJECT events on the same object / method invocation.
	 */
	@Column(length = 60)
	private String caseId;

	/**
	 * the execution status of the business case.
	 */
	@Enumerated(EnumType.STRING)
	@Column(length = 50)
	private ExecutionStatus executionStatus;

	@Lob
	private String differences;

	@Transient
	private List<Difference> diffList;

	/**
	 * the type of the target of the resource. Could be a class name, a URL, a table name, an entity name etc.
	 */
	private String target;

	/**
	 * the primary key of a JPA or JDBC resource in String format.
	 */
	@Column(length = 50)
	private String primaryKeyId;

	@Version
	private int version;

	@PrePersist
	public void prePersist() {
		historyId = UUID.randomUUID().toString();
	}

	/**
	 * @return the historyId
	 */
	public String getHistoryId() {
		return historyId;
	}

	/**
	 * @param historyId
	 *            the historyId to set
	 */
	public void setHistoryId(String historyId) {
		this.historyId = historyId;
	}

	/**
	 * @return the remark
	 */
	public String getRemark() {
		return remark;
	}

	/**
	 * @param remark
	 *            the remark to set
	 */
	public void setRemark(String remark) {
		this.remark = remark;
	}

	/**
	 * @return the controlEvent
	 */
	public ControlEvent getControlEvent() {
		return controlEvent;
	}

	/**
	 * @param controlEvent
	 *            the controlEvent to set
	 */
	public void setControlEvent(ControlEvent controlEvent) {
		this.controlEvent = controlEvent;
	}

	/**
	 * @return the createUser
	 */
	public String getCreateUser() {
		return createUser;
	}

	/**
	 * @param createUser
	 *            the createUser to set
	 */
	public void setCreateUser(String createUser) {
		this.createUser = createUser;
	}

	/**
	 * @return the createDate
	 */
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param createDate
	 *            the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @return the tenant
	 */
	public String getTenant() {
		return tenant;
	}

	/**
	 * @param tenant
	 *            the tenant to set
	 */
	public void setTenant(String tenant) {
		this.tenant = tenant;
	}

	/**
	 * @return the caseId
	 */
	public String getCaseId() {
		return caseId;
	}

	/**
	 * @param caseId
	 *            the caseId to set
	 */
	public void setCaseId(String caseId) {
		this.caseId = caseId;
	}

	/**
	 * @return the executionStatus
	 */
	public ExecutionStatus getExecutionStatus() {
		return executionStatus;
	}

	/**
	 * @param executionStatus
	 *            the executionStatus to set
	 */
	public void setExecutionStatus(ExecutionStatus executionStatus) {
		this.executionStatus = executionStatus;
	}

	/**
	 * @return the version
	 */
	public int getVersion() {
		return version;
	}

	/**
	 * @param version
	 *            the version to set
	 */
	public void setVersion(int version) {
		this.version = version;
	}

	/**
	 * @return the differences
	 */
	public String getDifferences() {
		return differences;
	}

	/**
	 * @param differences
	 *            the differences to set
	 */
	public void setDifferences(String differences) {
		this.differences = differences;
	}

	/**
	 * @return the target
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * @param target
	 *            the target to set
	 */
	public void setTarget(String target) {
		this.target = target;
	}

	/**
	 * @return the primaryKeyId
	 */
	public String getPrimaryKeyId() {
		return primaryKeyId;
	}

	/**
	 * @param primaryKeyId
	 *            the primaryKeyId to set
	 */
	public void setPrimaryKeyId(String primaryKeyId) {
		this.primaryKeyId = primaryKeyId;
	}

	/**
	 * @return the diffList
	 */
	public List<Difference> getDiffList() {
		if (diffList == null) {
			if (differences != null) {
				try {
					diffList = mapper.readValue(differences, new TypeReference<List<Difference>>() {
					});
				} catch (IOException e) {
					log.error(e.getMessage(), e);
					diffList = new ArrayList<>();
				}
			} else {
				diffList = new ArrayList<>();
			}
		}
		return diffList;
	}

	/**
	 * @param diffList
	 *            the diffList to set
	 */
	public void setDiffList(List<Difference> diffList) {
		this.diffList = diffList;
		if (diffList != null && !diffList.isEmpty()) {
			try {
				differences = mapper.writeValueAsString(diffList);
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		} else {
			differences = null;
		}
	}

	public String toString() {
		try {
			StringBuffer b = new StringBuffer();
			b.append("[");
			b.append(controlEvent);
			b.append(", ");
			b.append(createDate);
			b.append(", ");
			b.append(executionStatus);
			b.append(", ");
			b.append(primaryKeyId);
			b.append(", ");
			b.append(target);

			if (controlEvent == ControlEvent.UPDATE) {
				b.append("\n");
				b.append(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(differences));
				b.append("\n");
			}
			b.append("]");
			return b.toString();

		} catch (JsonProcessingException e) {
			log.error(e.getMessage(), e);
			return "";
		}
	}

}
