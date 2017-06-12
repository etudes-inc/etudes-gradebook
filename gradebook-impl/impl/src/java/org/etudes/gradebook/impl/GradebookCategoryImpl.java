/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-impl/impl/src/java/org/etudes/gradebook/impl/GradebookCategoryImpl.java $
 * $Id: GradebookCategoryImpl.java 12185 2015-12-02 19:16:26Z murthyt $
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

import java.util.List;

import org.etudes.gradebook.api.Gradebook.CategoryType;
import org.etudes.gradebook.api.GradebookCategory;
import org.etudes.gradebook.api.GradebookItem;
import org.etudes.gradebook.api.GradebookService;

public class GradebookCategoryImpl implements GradebookCategory
{
	protected CategoryType categoryType;
	protected String context = null;
	protected String createdByUserId;
	protected int dropNumberLowestScores;
	protected boolean extraCredit;
	protected String fetchedByUserId = null;
	protected int gradebookId;
	protected int id;
	protected int itemCount;
	protected boolean itemCountFetched = false;
	protected Float itemsTotalpoints;
	protected boolean itemsTotalPointsFetched = false;
	protected String modifiedByUserId;
	protected int order;
	protected int standardCategoryCode;
	protected String title;
	protected Float weight;
	protected WeightDistribution weightDistribution;
	transient GradebookService gradebookService = null;
	
	GradebookCategoryImpl()
	{
	}

	GradebookCategoryImpl(GradebookCategoryImpl other)
	{
		this.categoryType = other.categoryType;
		this.createdByUserId = other.createdByUserId;
		this.dropNumberLowestScores = other.dropNumberLowestScores;
		this.extraCredit = other.extraCredit;
		this.gradebookId = other.gradebookId;
		this.id = other.id;
		this.itemCount = other.itemCount;
		this.itemCountFetched = other.itemCountFetched;
		this.modifiedByUserId = other.modifiedByUserId;
		this.order = other.order;
		this.standardCategoryCode = other.standardCategoryCode;
		this.title = other.title;		
		this.weight = other.weight;		
		this.weightDistribution = other.weightDistribution;
		this.itemsTotalpoints = other.itemsTotalpoints;
		this.itemsTotalPointsFetched = other.itemsTotalPointsFetched;
		this.context = other.context;
		this.fetchedByUserId = other.fetchedByUserId;
		this.gradebookService = other.gradebookService;
	}
	
	GradebookCategoryImpl(GradebookService gradebookService)
	{
		this.gradebookService = gradebookService;
	}
	
	GradebookCategoryImpl(int id, String title, Float weight, WeightDistribution weightDistribution, int order)
	{
		this.id = id;
		this.title = title;
		this.weight = weight;
		this.weightDistribution = weightDistribution;
		this.order = order;
	}

	GradebookCategoryImpl(int id, String title, Float weight, WeightDistribution weightDistribution, int order, CategoryType categoryType)
	{
		this.id = id;
		this.title = title;
		this.weight = weight;
		this.weightDistribution = weightDistribution;
		this.order = order;
		this.categoryType = categoryType;
		
	}
	
	@Override
	public boolean equals(Object object)
	{
	  if ( this == object ) return true;
	  if ( !(object instanceof GradebookCategory) ) return false;
	  GradebookCategory that = (GradebookCategory)object;
	  return this.id == that.getId();		   
	}

	/**
	 * {@inheritDoc}
	 */
	public CategoryType getCategoryType()
	{
		return categoryType;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getCreatedByUserId()
	{
		return createdByUserId;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getDropNumberLowestScores()
	{
		return dropNumberLowestScores;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getGradebookId()
	{
		return gradebookId;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getId()
	{
		return id;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getItemCount()
	{
		if (!itemCountFetched)
		{
			if (id > 0 && this.gradebookService != null && gradebookId > 0 && (context != null && context.trim().length() > 0) && (fetchedByUserId != null && fetchedByUserId.trim().length() > 0))
			{
				Boolean userAccess = this.gradebookService.allowEditGradebook(context, fetchedByUserId);
				
				if (!userAccess)
				{
					userAccess = this.gradebookService.allowGetGradebook(context, fetchedByUserId);
					
					if (!userAccess)
					{
						return 0;
					}
				}
				
				List<GradebookItem> gradebookItems = ((GradebookServiceImpl)this.gradebookService).fetchGradebookToolItems(context, fetchedByUserId, false, false, null);
				itemCount = 0;
				if (gradebookItems != null && gradebookItems.size() > 0)
				{
					for (GradebookItem gradebookItem : gradebookItems)
					{
						if (gradebookItem.getGradebookCategory().getId() == this.id)
						{
							itemCount++;
						}
					}			
				}
				else
				{
					itemCount = 0;
				}				
			}			
			itemCountFetched = true;
		}
		
		return itemCount;	
	}

	/**
	 * @return the itemsTotalpoints
	 */
	public Float getItemsTotalpoints()
	{
		if (!itemsTotalPointsFetched)
		{
			if (id > 0 && this.gradebookService != null && gradebookId > 0 && (context != null && context.trim().length() > 0) && (fetchedByUserId != null && fetchedByUserId.trim().length() > 0))
			{
				Boolean userAccess = this.gradebookService.allowEditGradebook(context, fetchedByUserId);
				
				if (!userAccess)
				{
					userAccess = this.gradebookService.allowGetGradebook(context, fetchedByUserId);
					
					if (!userAccess)
					{
						return null;
					}
				}
				
				List<GradebookItem> gradebookItems = ((GradebookServiceImpl)this.gradebookService).fetchGradebookToolItems(context, fetchedByUserId, false, false, null);
				
				if (gradebookItems != null && gradebookItems.size() > 0)
				{
					for (GradebookItem gradebookItem : gradebookItems)
					{
						if (gradebookItem.getGradebookCategory().getId() == this.id)
						{
							if (itemsTotalpoints == null)
							{
								itemsTotalpoints = 0.0f;
							}
							
							if (gradebookItem.getPoints() != null && gradebookItem.getPoints() > 0)
							{
								itemsTotalpoints += gradebookItem.getPoints();
							}
						}
					}			
				}
			}			
			itemsTotalPointsFetched = true;
		}
		
		return roundToTwoDecimals(itemsTotalpoints);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getModifiedByUserId()
	{
		return modifiedByUserId;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getOrder()
	{
		return order;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getStandardCategoryCode()
	{
		return standardCategoryCode;
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
	public Float getWeight()
	{
		return roundToTwoDecimals(weight);
	}

	/**
	 * {@inheritDoc}
	 */
	public WeightDistribution getWeightDistribution()
	{
		return weightDistribution;
	}

	@Override
	public int hashCode()
	{
		return this.id;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isExtraCredit()
	{
		return extraCredit;
	}

	/**
	 * @param categoryType the categoryType to set
	 */
	public void setCategoryType(CategoryType categoryType)
	{
		this.categoryType = categoryType;
	}

	/**
	 * @param createdByUserId the createdByUserId to set
	 */
	public void setCreatedByUserId(String createdByUserId)
	{
		this.createdByUserId = createdByUserId;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDropNumberLowestScores(int dropNumberLowestScores)
	{
		this.dropNumberLowestScores = dropNumberLowestScores;
	}

	/**
	 * @param extraCredit the extraCredit to set
	 */
	public void setExtraCredit(boolean extraCredit)
	{
		this.extraCredit = extraCredit;
	}

	/**
	 * @param itemCount the itemCount to set
	 */
	public void setItemCount(int itemCount)
	{
		this.itemCount = itemCount;
	}

	/**
	 * @param modifiedByUserId the modifiedByUserId to set
	 */
	public void setModifiedByUserId(String modifiedByUserId)
	{
		this.modifiedByUserId = modifiedByUserId;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setOrder(int order)
	{
		this.order = order;
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(String title)
	{
		this.title = title;
	}

	/**
	 * @param weight the weight to set
	 */
	public void setWeight(Float weight)
	{
		this.weight = weight;
	}

	/**
	 * @param weightDistribution the weightDistribution to set
	 */
	public void setWeightDistribution(WeightDistribution weightDistribution)
	{
		this.weightDistribution = weightDistribution;
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
	 * @param gradebookId the gradebookId to set
	 */
	void setGradebookId(int gradebookId)
	{
		this.gradebookId = gradebookId;
	}
	
	/**
	 * @param id the id to set
	 */
	void setId(int id)
	{
		this.id = id;
	}
	
	/**
	 * @param standardCategoryCode the standardCategoryCode to set
	 */
	void setStadardCategoryCode(int standardCategoryCode)
	{
		this.standardCategoryCode = standardCategoryCode;
	}
}
