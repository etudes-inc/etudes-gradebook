/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-api/api/src/java/org/etudes/gradebook/api/Participant.java $
 * $Id: Participant.java 12452 2016-01-06 00:29:51Z murthyt $
 ***********************************************************************************
 *
 * Copyright (c) 2014, 2015, 2016 Etudes, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/
package org.etudes.gradebook.api;


public interface Participant
{

	/**
	 * @return the displayId
	 */
	String getDisplayId();

	/**
	 * @return the grade
	 */
	UserGrade getGrade();

	/**
	 * @return the groupTitle
	 */
	String getGroupTitle();

	/**
	 * @return the instructorNotes
	 */
	Notes getInstructorNotes();

	/**
	 * @return the overriddenLetterGrade
	 */
	UserGrade getOverriddenLetterGrade();

	/**
	 * @return the overriddenLetterGradeLog
	 */
	UserGrade getOverriddenLetterGradeLog();

	/**
	 * @return the privateMessageLink
	 */
	String getPrivateMessageLink();

	/**
	 * @return the sortName
	 */
	String getSortName();

	/**
	 * @return the status
	 */
	ParticipantStatus getStatus();
	
	/**
	 * @return the totalPoints
	 */
	Float getTotalPoints();

	/**
	 * @return the totalScore
	 */
	Float getTotalScore();

	/**
	 * @return the userId
	 */
	String getUserId();
	
	/**
	 * @param displayId the displayId to set
	 */
	void setDisplayId(String displayId);
	
	/**
	 * @param userId the userId to set
	 */
	// void setUserId(String userId);
	
	/**
	 * @param groupTitle the groupTitle to set
	 */
	void setGroupTitle(String groupTitle);
	
	/**
	 * @param privateMessageLink the privateMessageLink to set
	 */
	void setPrivateMessageLink(String privateMessageLink);
	
	/**
	 * @param sortName the sortName to set
	 */
	void setSortName(String sortName);
	
	/**
	 * @param status the status to set
	 */
	void setStatus(ParticipantStatus status);
}