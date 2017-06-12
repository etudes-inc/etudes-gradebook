/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-impl/impl/src/java/org/etudes/gradebook/impl/NotesImpl.java $
 * $Id: NotesImpl.java 12452 2016-01-06 00:29:51Z murthyt $
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
package org.etudes.gradebook.impl;

import java.util.Date;

import org.etudes.gradebook.api.Notes;

public class NotesImpl implements Notes
{
	protected String addedByUserId;
	protected Date dateAdded;
	protected Date dateModified;
	protected int gradebookId;
	protected int id;
	protected String modifiedByUserId;
	protected String notes;
	protected String userId;
	
	
	NotesImpl()
	{		
	}

	/**
	 * Constructor for notes
	 * 
	 * @param userId	User id
	 * 
	 * @param notes		Notes
	 * 
	 * @param prevNoteAddedDate	Previous note added date
	 * 
	 * addedByUserId Added by user id
	 */
	NotesImpl(String userId, String notes, Date prevNoteAddedDate, String addedByUserId)
	{
		this.userId = userId;
		this.notes = notes;
		this.dateAdded = prevNoteAddedDate;
		this.addedByUserId = addedByUserId;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getAddedByUserId()
	{
		return addedByUserId;
	}

	/**
	 * {@inheritDoc}
	 */
	public Date getDateAdded()
	{
		return dateAdded;
	}

	/**
	 * @return the dateModified
	 */
	public Date getDateModified()
	{
		return dateModified;
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
	 * @return the modifiedByUserId
	 */
	public String getModifiedByUserId()
	{
		return modifiedByUserId;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getNotes()
	{
		return notes;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getUserId()
	{
		return userId;
	}
	
	/**
	 * @param addedByUserId the addedByUserId to set
	 */
	public void setAddedByUserId(String addedByUserId)
	{
		this.addedByUserId = addedByUserId;
	}
	
	/**
	 * @param dateAdded the dateAdded to set
	 */
	public void setDateAdded(Date dateAdded)
	{
		this.dateAdded = dateAdded;
	}
	
	/**
	 * @param dateModified the dateModified to set
	 */
	public void setDateModified(Date dateModified)
	{
		this.dateModified = dateModified;
	}
	
	/**
	 * @param gradebookId the gradebookId to set
	 */
	public void setGradebookId(int gradebookId)
	{
		this.gradebookId = gradebookId;
	}
	
	/**
	 * @param id the id to set
	 */
	public void setId(int id)
	{
		this.id = id;
	}
	
	/**
	 * @param modifiedByUserId the modifiedByUserId to set
	 */
	public void setModifiedByUserId(String modifiedByUserId)
	{
		this.modifiedByUserId = modifiedByUserId;
	}
	
	/**
	 * @param notes the notes to set
	 */
	public void setNotes(String notes)
	{
		this.notes = notes;
	}
	
	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId)
	{
		this.userId = userId;
	}
}
