/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-api/api/src/java/org/etudes/gradebook/api/ParticipantStatus.java $
 * $Id: ParticipantStatus.java 9992 2015-02-03 01:01:31Z murthyt $
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

/**
 * ParticipantStatus captures a user's relationship to the site.
 */
public enum ParticipantStatus
{
	blocked(2), dropped(3), enrolled(0), ta(4), instructor(5), observer(6), added(1);

	private final int sortOrder;

	private ParticipantStatus(int sortOrder)
	{
		this.sortOrder = Integer.valueOf(sortOrder);
	}

	public Integer getSortValue()
	{
		return sortOrder;
	}
	
	public String getDisplayName()
	{
		String displayName = name().toUpperCase();
		displayName = displayName.substring(0,1) + name().substring(1).toLowerCase();
		return displayName;
	}
}
