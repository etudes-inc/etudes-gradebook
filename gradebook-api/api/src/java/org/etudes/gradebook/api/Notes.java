/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-api/api/src/java/org/etudes/gradebook/api/Notes.java $
 * $Id: Notes.java 12452 2016-01-06 00:29:51Z murthyt $
 ***********************************************************************************
 *
 * Copyright (c) 2015, 2016 Etudes, Inc.
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

public interface Notes
{
	/**
	 * @return the addedByUserId
	 */
	String getAddedByUserId();	
	
	/**
	 * @return the dateAdded
	 */
	Date getDateAdded();
	
	/**
	 * @return the dateModified
	 */
	Date getDateModified();
	
	/**
	 * @return the gradebookId
	 */
	int getGradebookId();
	
	/**
	 * @return the id
	 */
	int getId();
	
	/**
	 * @return the modifiedByUserId
	 */
	String getModifiedByUserId();
	
	/**
	 * @return the notes
	 */
	String getNotes();

	/**
	 * @return the userId
	 */
	String getUserId();
}
