/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-api/api/src/java/org/etudes/gradebook/api/GradebookItem.java $
 * $Id: GradebookItem.java 9367 2014-11-26 02:10:20Z murthyt $
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

import org.etudes.coursemap.api.CourseMapItem;

public interface ParticipantGradebookItem
{
	/**
	 * @return	CourseMapItem	
	 */
	CourseMapItem getCourseMapItem();	

	/**
	 * @return gradebookItem
	 */
	GradebookItem getGradebookItem();
	
	/**
	 * @return the id
	 */
	String getId();
	
	/**
	 * @return ParticipantItemDetails
	 */
	ParticipantItemDetails getParticipantItemDetails();
}
