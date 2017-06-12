/**********************************************************************************
 * $URL: $
 * $Id: $
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

import org.etudes.coursemap.api.CourseMapItem;
import org.etudes.gradebook.api.GradebookItem;
import org.etudes.gradebook.api.ParticipantItemDetails;
import org.etudes.gradebook.api.ParticipantGradebookItem;

public class ParticipantGradebookItemImpl implements ParticipantGradebookItem
{
	protected CourseMapItem courseMapItem;
	
	protected GradebookItem gradebookItem;
	
	protected String id;
	
	protected ParticipantItemDetails participantItemDetails;
	
	public ParticipantGradebookItemImpl(String id, GradebookItem gradebookItem, ParticipantItemDetails participantItemDetails)
	{
		this.id = id;
		this.gradebookItem = gradebookItem;
		this.participantItemDetails = participantItemDetails;
	}

	public CourseMapItem getCourseMapItem()
	{
		return this.courseMapItem;
	}

	public GradebookItem getGradebookItem()
	{
		return this.gradebookItem;
	}

	public String getId()
	{
		return this.id;
	}
	
	public ParticipantItemDetails getParticipantItemDetails()
	{
		return participantItemDetails;
	}

}
