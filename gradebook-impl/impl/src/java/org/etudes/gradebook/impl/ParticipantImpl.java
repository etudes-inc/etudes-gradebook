/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-impl/impl/src/java/org/etudes/gradebook/impl/ParticipantImpl.java $
 * $Id: ParticipantImpl.java 12452 2016-01-06 00:29:51Z murthyt $
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
package org.etudes.gradebook.impl;

import org.etudes.gradebook.api.Notes;
import org.etudes.gradebook.api.Participant;
import org.etudes.gradebook.api.ParticipantStatus;
import org.etudes.gradebook.api.UserGrade;

/**
 * Holds basic participant information
 */
public class ParticipantImpl implements Participant
{
	protected String displayId;
	
	protected UserGrade grade;
	
	protected String groupTitle;
	
	protected Notes instructorNotes;
	
	protected UserGrade overriddenLetterGrade;
	
	protected UserGrade overriddenLetterGradeLog;
	
	protected String privateMessageLink;
	
	protected String sortName;
	
	protected ParticipantStatus status;
	
	protected Float totalPoints;
	
	protected Float totalScore;
	
	protected String userId;
	
	
	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj)
	{
		// two are equals if they have the same userId
		if (this == obj) return true;
		if ((obj == null) || (obj.getClass() != this.getClass())) return false;
		if ((this.userId == null) || (((ParticipantImpl) obj).userId == null)) return false;
		return this.userId.equals(((ParticipantImpl) obj).userId);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDisplayId()
	{
		return displayId;
	}

	/**
	 * @return the grade
	 */
	public UserGrade getGrade()
	{
		return grade;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getGroupTitle()
	{
		return groupTitle;
	}

	/**
	 * {@inheritDoc}
	 */
	public Notes getInstructorNotes()
	{
		return instructorNotes;
	}

	/**
	 * @return the overriddenLetterGrade
	 */
	public UserGrade getOverriddenLetterGrade()
	{
		return overriddenLetterGrade;
	}

	/**
	 * @return the overriddenLetterGradeLog
	 */
	public UserGrade getOverriddenLetterGradeLog()
	{
		return overriddenLetterGradeLog;
	}

	/**
	 * @return the privateMessageLink
	 */
	public String getPrivateMessageLink()
	{
		return privateMessageLink;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getSortName()
	{
		return sortName;
	}

	/**
	 * {@inheritDoc}
	 */
	public ParticipantStatus getStatus()
	{
		return status;
	}

	/**
	 * @return the totalPoints
	 */
	public Float getTotalPoints()
	{
		return roundToTwoDecimals(totalPoints);
	}

	/**
	 * @return the totalScore
	 */
	public Float getTotalScore()
	{
		return roundToTwoDecimals(totalScore);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getUserId()
	{
		return userId;
	}

	/**
	 * {@inheritDoc}
	 */
	public int hashCode()
	{
		return this.userId == null ? "null".hashCode() : this.userId.hashCode();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setDisplayId(String displayId)
	{
		this.displayId = displayId;
	}
	
	/**
	 * @param grade the grade to set
	 */
	public void setGrade(UserGrade grade)
	{
		this.grade = grade;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setGroupTitle(String groupTitle)
	{
		this.groupTitle = groupTitle;
	}
	
	/**
	 * @param instructorNotes the instructorNotes to set
	 */
	public void setInstructorNotes(Notes instructorNotes)
	{
		this.instructorNotes = instructorNotes;
	}
	
	/**
	 * @param overriddenLetterGrade the overriddenLetterGrade to set
	 */
	public void setOverriddenLetterGrade(UserGrade overriddenLetterGrade)
	{
		this.overriddenLetterGrade = overriddenLetterGrade;
	}
	
	/**
	 * @param privateMessageLink the privateMessageLink to set
	 */
	public void setPrivateMessageLink(String privateMessageLink)
	{
		this.privateMessageLink = privateMessageLink;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setSortName(String sortName)
	{
		this.sortName = sortName;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setStatus(ParticipantStatus status)
	{
		this.status = status;
	}
	
	/**
	 * @param totalPoints the totalPoints to set
	 */
	public void setTotalPoints(Float totalPoints)
	{
		this.totalPoints = totalPoints;
	}
	
	/**
	 * @param totalScore the totalScore to set
	 */
	public void setTotalScore(Float totalScore)
	{
		this.totalScore = totalScore;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setUserId(String userId)
	{
		this.userId = userId;
	}

	/**
	 * Round to two decimals
	 * 
	 * @param number	The number that is to be rounded
	 * 
	 * @return	Rounded number
	 */
	protected Float roundToTwoDecimals(Float number)
	{
		if (number == null)
		{
			return null;
		}
		
		return Math.round(number * 100.0f) / 100.0f;		
	}

	/**
	 * @param overriddenLetterGradeLog the overriddenLetterGradeLog to set
	 */
	void setOverriddenLetterGradeLog(UserGrade overriddenLetterGradeLog)
	{
		this.overriddenLetterGradeLog = overriddenLetterGradeLog;
	}
}
