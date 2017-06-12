/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-api/api/src/java/org/etudes/gradebook/api/GradebookItemType.java $
 * $Id: GradebookItemType.java 12212 2015-12-04 04:40:05Z rashmim $
 ***********************************************************************************
 *
 * Copyright (c) 2014 Etudes, Inc.
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

import org.etudes.gradebook.api.Gradebook.StandardCategory;

public enum GradebookItemType
{
	assignment(1, 1, "Assignment", "AT&S", "MNEME"), 
	category(2, 2, "Category", "Discussions", "CAT"), 
	forum(3, 2, "Forum", "Discussions", "FORUM"), 
	offline(7, 1, "Offline Item", "AT&S", "MNEME"), 
	survey(4, 1, "Survey", "AT&S", "MNEME"), 
	test(5, 1, "Test", "AT&S", "MNEME"),
	topic(6, 2, "Topic", "Discussions", "TOPIC");
	
	static public Gradebook.StandardCategory getCategoryGradebookItemType(GradebookItemType gradebookItemType)
	{
		if (gradebookItemType == null)
		{
			return null;
		}
		
		Gradebook.StandardCategory standardCategory = null;
		
		if (gradebookItemType == GradebookItemType.assignment)
		{
			standardCategory = StandardCategory.assignment;
		}
		else if (gradebookItemType == GradebookItemType.category || gradebookItemType == GradebookItemType.forum || gradebookItemType == GradebookItemType.topic)
		{
			standardCategory = StandardCategory.discussions;
		}
		else if (gradebookItemType == GradebookItemType.test)
		{
			standardCategory = StandardCategory.test;
		}
		else if (gradebookItemType == GradebookItemType.offline)
		{
			standardCategory = StandardCategory.offline;
		}
		
		return standardCategory;
	}

	private final Integer appCode;

	private final String displayString;

	private final Integer id;

	private final String itemIdCode;
	
	private final String toolTitle;

	private GradebookItemType(int id, int appCode, String displayString, String toolTitle, String itemIdCode) 
	{
        this.id = id;
        this.appCode = appCode;
        this.displayString = displayString;
        this.toolTitle = toolTitle;
        this.itemIdCode = itemIdCode;
    }

	/**
	 * @return the appCode
	 */
	public Integer getAppCode()
	{
		return appCode;
	}
	
	/**
	 * @return the displayString
	 */
	public String getDisplayString()
	{
		return displayString;
	}
	
	/**
	 * @return the id
	 */
	public Integer getId()
	{
		return id;
	}
	
	public Boolean getIsJforum()
	{
		return Boolean.valueOf(this.appCode == 2);
	}
	
	public Boolean getIsMneme()
	{
		return Boolean.valueOf(this.appCode == 1);		
	}
	
	public String getItemIdCode()
	{
		return itemIdCode;
	}
	
	/*
	static public Gradebook.StandardCategory getCategoryGradebookItemType(int gradebookItemTypeId)
	{
		Gradebook.StandardCategory standardCategory = null;
		
		switch (gradebookItemTypeId) 
		{
			case 1:
				standardCategory = StandardCategory.assignment;
				break;
			case 2: case 3: case 6:
				standardCategory = StandardCategory.discussions;
				break;
			case 4:
				break;
			case 5:
				standardCategory = StandardCategory.test;
				break;
			case 7:
				standardCategory = StandardCategory.offline;
				break;
			default:
				standardCategory = null;
				break;
		}
		
		return standardCategory;
	}
	*/
	
	/**
	 * @return the toolTitle
	 */
	public String getToolTitle()
	{
		return toolTitle;
	}
}
