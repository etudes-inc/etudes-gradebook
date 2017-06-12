/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-impl/impl/src/java/org/etudes/gradebook/impl/UserCategoryGradeImpl.java $
 * $Id: UserCategoryGradeImpl.java 10987 2015-06-01 18:43:22Z murthyt $
 ***********************************************************************************
 *
 * Copyright (c) 2015 Etudes, Inc.
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
import java.util.List;

import org.etudes.gradebook.api.GradebookCategory;
import org.etudes.gradebook.api.UserCategoryGrade;

public class UserCategoryGradeImpl implements UserCategoryGrade
{
	class PointsScore
	{
		protected String itemId = null;
		protected final Float points;
		protected final Float score;

		PointsScore(Float points, Float score)
		{
			this.points = points;
			this.score = score;
		}
		
		PointsScore(Float points, Float score, String itemId)
		{
			this.points = points;
			this.score = score;
			this.itemId = itemId;
		}
		
		String getItemId()
		{
			return this.itemId;
		}
		
		Float getPoints()
		{
			return roundToTwoDecimals(this.points);
		}
		
		Float getScore()
		{
			return roundToTwoDecimals(this.score);
		}
		
		Float getScorePointPercent()
		{
			if (this.score == null || this.points == null || this.points <= 0)
			{
				return 0f;
			}
			return Math.round(this.score * 100.0f/ this.points * 100.0f) /100.0f;
		}
	}
	
	protected Float averagePercent;
	
	protected int categoryId;
	
	protected GradebookCategory gradebookCategory;
	
	protected Float points;
	
	protected List<PointsScore> pointsScores = new ArrayList<PointsScore>();

	protected Float score;
	
	UserCategoryGradeImpl()
	{
	}
	
	UserCategoryGradeImpl(UserCategoryGradeImpl other)
	{
		this.averagePercent = other.averagePercent;
		this.categoryId = other.categoryId;
		this.gradebookCategory = new GradebookCategoryImpl((GradebookCategoryImpl)other.gradebookCategory);
		this.points = other.points;
		this.score = other.score;
		
		for (PointsScore pointsScore : pointsScores)
		{
			this.pointsScores.add(new PointsScore(pointsScore.points, pointsScore.score, pointsScore.itemId));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getAveragePercent()
	{
		return roundToTwoDecimals(averagePercent);
	}
		  
	/**
	 * {@inheritDoc}
	 */
	public int getCategoryId()
	{
		return categoryId;
	}

	/**
	 * {@inheritDoc}
	 */
	public GradebookCategory getGradebookCategory()
	{
		return gradebookCategory;
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getPoints()
	{
		return roundToTwoDecimals(points);
	}

	/**
	 * @return the pointsScore
	 */
	public List<PointsScore> getPointsScores()
	{
		return pointsScores;
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getScore()
	{
		return roundToTwoDecimals(score);
	}

	/**
	 * @param averagePercent the averagePercent to set
	 */
	public void setAveragePercent(Float averagePercent)
	{
		this.averagePercent = averagePercent;
	}

	/**
	 * @param categoryId the categoryId to set
	 */
	public void setCategoryId(int categoryId)
	{
		this.categoryId = categoryId;
	}

	/**
	 * @param gradebookCategory the gradebookCategory to set
	 */
	public void setGradebookCategory(GradebookCategory gradebookCategory)
	{
		this.gradebookCategory = gradebookCategory;
	}

	/**
	 * @param points the points to set
	 */
	public void setPoints(Float points)
	{
		this.points = points;
	}

	/**
	 * @param score the score to set
	 */
	public void setScore(Float score)
	{
		this.score = score;
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
}
