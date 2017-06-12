/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-api/api/src/java/org/etudes/gradebook/api/GradebookItem.java $
 * $Id: GradebookItem.java 10389 2015-04-01 21:17:26Z murthyt $
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
package org.etudes.gradebook.api;

import java.util.Date;
import java.util.Map;

public interface GradebookItem
{
	/**
	 * @return the accessStatus
	 */
	GradebookItemAccessStatus getAccessStatus();	

	/**
	 * @return the class average Percent
	 */
	Float getAveragePercent();

	/**
	 * @return the closeDate
	 */
	Date getCloseDate();

	/**
	 * @return the description
	 */
	String getDescription();

	/**
	 * @return the displayOrder
	 */
	int getDisplayOrder();

	/**
	 * @return the dueDate
	 */
	Date getDueDate();

	/**
	 * @return the gradebookCategory
	 */
	GradebookCategory getGradebookCategory();	

	/**
	 * @return the id
	 */
	String getId();
	
	/**
	 * @return the itemRealId
	 */
	Integer getItemRealId();
	
	/**
	 * @return the openDate
	 */
	Date getOpenDate();
	
	/**
	 * @return the points
	 */
	Float getPoints();
	
	/**
	 * @return the scores
	 */
	Map<String, Float> getScores();
	
	/**
	 * @return the submittedCount. For mneme submission count and for JForum number of users who posted one or more posts
	 */
	Integer getSubmittedCount();
	
	/**
	 * @return the title
	 */
	String getTitle();
	
	/**
	 * @return the toolId
	 */
	String getToolId();
	
	/**
	 * @return the type
	 */
	GradebookItemType getType();
}
