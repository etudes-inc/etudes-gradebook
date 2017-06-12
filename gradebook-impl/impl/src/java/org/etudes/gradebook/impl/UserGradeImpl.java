/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-impl/impl/src/java/org/etudes/gradebook/impl/UserGradeImpl.java $
 * $Id: UserGradeImpl.java 12630 2016-02-17 02:26:19Z murthyt $
 ***********************************************************************************
 *
 * Copyright (c) 2014, 2015 Etudes, Inc.
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.etudes.gradebook.api.UserCategoryGrade;
import org.etudes.gradebook.api.UserGrade;

public class UserGradeImpl implements UserGrade
{
	protected String assignedByUserId;
	protected Date assignedDate;
	protected Float averagePercent;
	protected Float extraCreditPercent;
	protected Float extraCreditScore;
	protected int gradebookId;
	protected int id;
	protected String letterGrade;
	protected Float points;
	protected Float score;
	protected Float totalScore;
	protected List<UserCategoryGrade> userCategoryGrade = new ArrayList<UserCategoryGrade>();
	protected String userId;
	
	UserGradeImpl()
	{		
	}

	/**
	 * Constructor for overriding grade
	 * 
	 * @param userId		User id
	 * 
	 * @param letterGrade	Letter grade
	 * 
	 * @param prevGradeAssignedDate	Previous overridden grade assigned date 
	 */
	UserGradeImpl(String userId, String letterGrade, Date prevGradeAssignedDate)
	{
		this.userId = userId;
		this.letterGrade = letterGrade;
		this.assignedDate = prevGradeAssignedDate;
	}

	UserGradeImpl(UserGradeImpl other)
	{		
		this.assignedByUserId = other.assignedByUserId;
		this.assignedDate = other.assignedDate;
		this.averagePercent = other.averagePercent;
		this.extraCreditPercent = other.extraCreditPercent;
		this.gradebookId = other.gradebookId;
		this.id = other.id;
		this.letterGrade = other.letterGrade;
		this.points = other.points;
		this.score = other.score;
		this.extraCreditScore = other.extraCreditScore;
		
		for (UserCategoryGrade ucg : other.userCategoryGrade)
		{
			this.userCategoryGrade.add(new UserCategoryGradeImpl((UserCategoryGradeImpl)ucg));
		}
		this.userId = other.userId;
		this.totalScore = other.totalScore;
	}
	
	/**
	 * @return the assignedByUserId
	 */
	public String getAssignedByUserId()
	{
		return assignedByUserId;
	}

	/**
	 * @return the assignedDate
	 */
	public Date getAssignedDate()
	{
		return assignedDate;
	}

	/**
	 * @return the averagePercent
	 */
	public Float getAveragePercent()
	{
		return roundToTwoDecimals(averagePercent);
	}

	/**
	 * @return the extraCreditPercent
	 */
	public Float getExtraCreditPercent()
	{
		return roundToTwoDecimals(extraCreditPercent);
	}
	
	/**
	 * @return the extraCreditScore
	 */
	public Float getExtraCreditScore()
	{
		return extraCreditScore;
	}
	
	
	/**
	 * @return the gradebookId
	 */
	public int getGradebookId()
	{
		return gradebookId;
	}

	/**
	 * @return the id
	 */
	public int getId()
	{
		return id;
	}

	/**
	 * @return the assignedLetterGrade
	 */
	public String getLetterGrade()
	{
		return letterGrade;
	}

	/**
	 * @return the points
	 */
	public Float getPoints()
	{
		return roundToTwoDecimals(points);
	}

	/**
	 * @return the score
	 */
	public Float getScore()
	{
		return roundToTwoDecimals(score);
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getTotalScore()
	{
		if (score != null)
		{
			if (extraCreditScore != null)
			{
				return roundToTwoDecimals(score + extraCreditScore);
			}
			else
			{
				return roundToTwoDecimals(score);
			}
		}
		else
		{
			if (extraCreditScore != null)
			{
				return extraCreditScore;
			}			
		}
		
		return score; 
	}

	/**
	 * @return the userCategoryGrade
	 */
	public List<UserCategoryGrade> getUserCategoryGrade()
	{
		return userCategoryGrade;
	}

	/**
	 * @return the userId
	 */
	public String getUserId()
	{
		return userId;
	}

	/**
	 * @param assignedByUserId the assignedByUserId to set
	 */
	public void setAssignedByUserId(String assignedByUserId)
	{
		this.assignedByUserId = assignedByUserId;
	}
	
	/**
	 * @param assignedDate the assignedDate to set
	 */
	public void setAssignedDate(Date assignedDate)
	{
		this.assignedDate = assignedDate;
	}
	
	/**
	 * @param gradebookId the gradebookId to set
	 */
	public void setGradebookId(int gradebookId)
	{
		this.gradebookId = gradebookId;
	}
	
	/**
	 * @param id the id to set
	 */
	public void setId(int id)
	{
		this.id = id;
	}
	
	/**
	 * @param letterGrade the letterGrade to set
	 */
	public void setLetterGrade(String letterGrade)
	{
		this.letterGrade = letterGrade;
	}
	
	/**
	 * @param userId the userId to set
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
	 * @param averagePercent the averagePercent to set
	 */
	void setAveragePercent(Float averagePercent)
	{
		this.averagePercent = averagePercent;
	}
	
	/**
	 * @param extraCreditPercent the extraCreditPercent to set
	 */
	void setExtraCreditPercent(Float extraCreditPercent)
	{
		this.extraCreditPercent = extraCreditPercent;
	}
	
	/**
	 * @param extraCreditScore the extraCreditScore to set
	 */
	void setExtraCreditScore(Float extraCreditScore)
	{
		this.extraCreditScore = extraCreditScore;
	}
	
	/**
	 * @param points the points to set
	 */
	void setPoints(Float points)
	{
		this.points = points;
	}
	
	/**
	 * @param score the score to set
	 */
	void setScore(Float score)
	{
		this.score = score;
	}
}
