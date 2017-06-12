/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-api/api/src/java/org/etudes/gradebook/api/GradebookCategoryItemMap.java $
 * $Id: GradebookCategoryItemMap.java 12172 2015-12-01 01:19:22Z murthyt $
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
package org.etudes.gradebook.api;

public interface GradebookCategoryItemMap
{
	/**
	 * @return the categoryId
	 */
	public int getCategoryId();
	
	/**
	 * @return the displayOrder
	 */
	public int getDisplayOrder();
	
	/**
	 * @param displayOrder the displayOrder to set
	 */
	public void setDisplayOrder(int displayOrder);

	/**
	 * @return the itemId
	 */
	public String getItemId();
	
	/**
	 * @return the category
	 */
	GradebookCategory getCategory();
	
	/**
	 * @return the id
	 */
	int getId();
	
	//public String getStatus();
	
	//public void setStatus(String status);

	public void setCategory(GradebookCategory toCategory);
}
