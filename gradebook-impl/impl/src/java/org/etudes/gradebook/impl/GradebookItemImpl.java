/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-impl/impl/src/java/org/etudes/gradebook/impl/GradebookItemImpl.java $
 * $Id: GradebookItemImpl.java 11114 2015-06-16 19:50:53Z rashmim $
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

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.etudes.gradebook.api.GradebookCategory;
import org.etudes.gradebook.api.GradebookItem;
import org.etudes.gradebook.api.GradebookItemAccessStatus;
import org.etudes.gradebook.api.GradebookItemType;

public class GradebookItemImpl implements GradebookItem
{
	protected GradebookItemAccessStatus accessStatus;	
	protected Float averagePercent;	
	protected Date closeDate = null;	
	protected String description = null;
	protected int displayOrder;	
	protected Date dueDate = null;	
	protected GradebookCategory gradebookCategory;	
	protected String id;	
	// protected String context = null;	
	// id in the tool
	protected Integer itemRealId;	
	protected Date openDate = null;	
	protected Float points = null;	
	protected Map<String, Float> scores = new LinkedHashMap<String, Float>();	
	/* mneme submission count and jforum number of users who posted one or more posts*/
	protected Integer submittedCount;	
	protected String title = null;	
	protected String toolId = null;	
	protected GradebookItemType type;
	
	GradebookItemImpl(GradebookItemImpl other)
	{
		this.accessStatus = other.accessStatus;
		this.averagePercent = other.averagePercent;
		this.closeDate = other.closeDate;
		this.description = other.description;
		this.displayOrder = other.displayOrder;
		this.dueDate = other.dueDate;
		
		if (other.gradebookCategory != null)
		{
			this.gradebookCategory = new GradebookCategoryImpl((GradebookCategoryImpl)other.gradebookCategory);
		}
		this.id = other.id;
		this.itemRealId = other.itemRealId;
		this.openDate = other.openDate;
		this.points = other.points;
		if (other.scores != null && other.scores.size() > 0)
		{
			this.scores = new LinkedHashMap<String, Float>(scores);
		}
		this.submittedCount = other.submittedCount;
		this.title = other.title;
		this.toolId = other.toolId;
		this.type = other.type;				
	}
	
	GradebookItemImpl(String id, String title, Float points, Date dueDate, Date openDate, GradebookItemType type)	
	{
		this.id = id;
		this.title = title;
		this.points = points;
		this.dueDate = dueDate;
		this.openDate = openDate;
		this.type = type;
	}

	GradebookItemImpl(String id, String title, Float points, Float averagePercent, Date dueDate, Date openDate, Date closeDate, GradebookItemType type, GradebookItemAccessStatus accessStatus, Integer submittedCount)	
	{
		this.id = id;
		this.title = title;
		this.points = points;
		this.averagePercent = averagePercent;
		this.dueDate = dueDate;
		this.openDate = openDate;
		this.closeDate = closeDate;
		this.type = type;
		this.accessStatus = accessStatus;
		this.submittedCount = submittedCount;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		GradebookItemImpl other = (GradebookItemImpl) obj;
		if (id == null)
		{
			if (other.id != null) return false;
		}
		else if (!id.equals(other.id)) return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "GradebookItemImpl [id=" + id + ", title=" + title + "]";
	}

	/**
	 * {@inheritDoc}
	 */
	public GradebookItemAccessStatus getAccessStatus()
	{
		return accessStatus;
	}

	/**
	 * @return the averagePercent
	 */
	public Float getAveragePercent()
	{
		return roundToTwoDecimals(averagePercent);
	}

	/**
	 * {@inheritDoc}
	 */
	public Date getCloseDate()
	{
		return closeDate;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDescription()
	{
		return description;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getDisplayOrder()
	{
		return displayOrder;
	}

	/**
	 * {@inheritDoc}
	 */
	public Date getDueDate()
	{
		return dueDate;
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
	public String getId()
	{
		return id;
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer getItemRealId()
	{
		return itemRealId;
	}

	/**
	 * {@inheritDoc}
	 */
	public Date getOpenDate()
	{
		return openDate;
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getPoints()
	{
		return roundToTwoDecimals(points);
	}

	/**
	 * {@inheritDoc}
	 */
	public Map<String, Float> getScores()
	{
		return scores;
	}

	/**
	 * @return the submittedCount
	 */
	public Integer getSubmittedCount()
	{
		return submittedCount;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getTitle()
	{
		return title;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getToolId()
	{
		return toolId;
	}

	/**
	 * {@inheritDoc}
	 */
	public GradebookItemType getType()
	{
		return type;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description)
	{
		this.description = description;
	}

	/**
	 * @param displayOrder the displayOrder to set
	 */
	public void setDisplayOrder(int displayOrder)
	{
		this.displayOrder = displayOrder;
	}

	/**
	 * @param dueDate the dueDate to set
	 */
	public void setDueDate(Date dueDate)
	{
		this.dueDate = dueDate;
	}

	/**
	 * @param gradebookCategory the gradebookCategory to set
	 */
	public void setGradebookCategory(GradebookCategory gradebookCategory)
	{
		this.gradebookCategory = gradebookCategory;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id)
	{
		this.id = id;
	}

	/**
	 * @param itemRealId the itemRealId to set
	 */
	public void setItemRealId(Integer itemRealId)
	{
		this.itemRealId = itemRealId;
	}

	/**
	 * @param openDate the openDate to set
	 */
	public void setOpenDate(Date openDate)
	{
		this.openDate = openDate;
	}

	/**
	 * @param points the points to set
	 */
	public void setPoints(Float points)
	{
		this.points = points;
	}

	/**
	 * @param submittedCount the submittedCount to set
	 */
	public void setSubmittedCount(Integer submittedCount)
	{
		this.submittedCount = submittedCount;
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(String title)
	{
		this.title = title;
	}

	/**
	 * @param toolId the toolId to set
	 */
	public void setToolId(String toolId)
	{
		this.toolId = toolId;
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
}
