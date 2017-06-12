/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-impl/impl/src/java/org/etudes/gradebook/impl/GradebookCategoryItemMapImpl.java $
 * $Id: GradebookCategoryItemMapImpl.java 11544 2015-09-02 22:22:17Z murthyt $
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

import org.etudes.gradebook.api.GradebookCategory;
import org.etudes.gradebook.api.GradebookCategoryItemMap;

public class GradebookCategoryItemMapImpl implements GradebookCategoryItemMap
{
	protected GradebookCategory category;
	protected int categoryId;
	protected int displayOrder;
	protected int id;
	protected String itemId;
	// protected String status;
	
	GradebookCategoryItemMapImpl()
	{		
	}

	GradebookCategoryItemMapImpl(String itemId, int categoryId, int displayOrder)
	{
		this.itemId = itemId;
		this.categoryId = categoryId;
		this.displayOrder = displayOrder;
	}

	/**
	 * @return the category
	 */
	public GradebookCategory getCategory()
	{
		return category;
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
	public int getDisplayOrder()
	{
		return displayOrder;
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
	public String getItemId()
	{
		return itemId;
	}
	
	/*
	public String getStatus() {
		return status;
	}*/
	
	/**
	 * @param category the category to set
	 */
	public void setCategory(GradebookCategory category)
	{
		this.category = category;
	}
	
	/**
	 * @param categoryId the categoryId to set
	 */
	public void setCategoryId(int categoryId)
	{
		this.categoryId = categoryId;
	}
	
	/**
	 * @param displayOrder the displayOrder to set
	 */
	public void setDisplayOrder(int displayOrder)
	{
		this.displayOrder = displayOrder;
	}
	
	/**
	 * @param id the id to set
	 */
	public void setId(int id)
	{
		this.id = id;
	}
	
	/**
	 * @param itemId the itemId to set
	 */
	public void setItemId(String itemId)
	{
		this.itemId = itemId;
	}
	
	/*
	public void setStatus(String status) {
		this.status = status;
	}*/

	@Override
	public int hashCode() 
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + categoryId;
		result = prime * result + ((itemId == null) ? 0 : itemId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		GradebookCategoryItemMapImpl other = (GradebookCategoryItemMapImpl) obj;
		if (categoryId != other.categoryId)	return false;
		if (itemId == null) 
		{
			if (other.itemId != null) return false;
		}
		else if (!itemId.equals(other.itemId)) return false;
		return true;
	}

	@Override
	public String toString() {
		return "GradebookCategoryItemMapImpl [categoryId=" + categoryId
				+ ", displayOrder=" + displayOrder + ", itemId=" + itemId + "]";
	}
}
