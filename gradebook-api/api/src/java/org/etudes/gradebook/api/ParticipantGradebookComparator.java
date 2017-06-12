/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-api/api/src/java/org/etudes/gradebook/api/ParticipantGradebookComparator.java $
 * $Id: ParticipantGradebookComparator.java 11036 2015-06-03 19:28:05Z murthyt $
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

import java.util.Comparator;

import org.etudes.gradebook.api.Gradebook.GradebookSortType;

public class ParticipantGradebookComparator implements Comparator<ParticipantGradebookItem>
{
	// the criteria - asc
	boolean asc = true;

	// the criteria
	GradebookSortType sortType = null;

	public ParticipantGradebookComparator(GradebookSortType sortType, boolean asc)
	{
		this.sortType = sortType;
		this.asc = asc;
	} // constructor

	public int compare(ParticipantGradebookItem one, ParticipantGradebookItem two) 
	{
    	int comp = -1;
    	
    	if (sortType == GradebookSortType.Title)
		{
			// sorted item title
			comp = one.getGradebookItem().getTitle().compareToIgnoreCase(two.getGradebookItem().getTitle());
		}
		else if (sortType == GradebookSortType.DueDate)
		{
			// Sort by name if no date on either
			if (one.getGradebookItem().getDueDate() == null && two.getGradebookItem().getDueDate() == null)
			{
				return one.getGradebookItem().getTitle().compareTo(two.getGradebookItem().getTitle());
			}
	
			// Null dates are last
			if (one.getGradebookItem().getDueDate() == null)
			{
				return 1;
			}
			if (two.getGradebookItem().getDueDate() == null)
			{
				return -1;
			}
	
			// Sort by name if both assignments have the same date
			comp = (one.getGradebookItem().getDueDate().compareTo(two.getGradebookItem().getDueDate()));
			
			if (comp == 0)
			{
				comp = one.getGradebookItem().getTitle().compareTo(two.getGradebookItem().getTitle());
			}
			else
			{
				//return comp;
			}
		}
		else if (sortType == GradebookSortType.OpenDate)
		{
			// Sort by name if no date on either
			if (one.getGradebookItem().getOpenDate() == null && two.getGradebookItem().getOpenDate() == null)
			{
				return one.getGradebookItem().getTitle().compareTo(two.getGradebookItem().getTitle());
			}
	
			// Null dates are last
			if (one.getGradebookItem().getOpenDate() == null)
			{
				return 1;
			}
			if (two.getGradebookItem().getOpenDate() == null)
			{
				return -1;
			}
	
			// Sort by name if both assignments have the same date
			comp = (one.getGradebookItem().getOpenDate().compareTo(two.getGradebookItem().getOpenDate()));
			
			if (comp == 0)
			{
				comp = one.getGradebookItem().getTitle().compareTo(two.getGradebookItem().getTitle());
			}
			else
			{
				//return comp;			
			}
		}
		else if (sortType == GradebookSortType.Category)
		{
			// sort by category type/order(if sorted by category order it's like sorted by category) as primary sort and display order as second sort
			if (one.getGradebookItem().getGradebookCategory() == null && two.getGradebookItem().getGradebookCategory() == null)
			{
				return Integer.valueOf(one.getGradebookItem().getDisplayOrder()).compareTo(Integer.valueOf(two.getGradebookItem().getDisplayOrder()));
			}
			
			// Null categories are placed last
			if (one.getGradebookItem().getGradebookCategory() == null)
			{
				return 1;
			}
			if (two.getGradebookItem().getGradebookCategory() == null)
			{
				return -1;
			}
			
			// Sort by display order if both have the same category 
			comp = Integer.valueOf(one.getGradebookItem().getGradebookCategory().getOrder()).compareTo(Integer.valueOf(two.getGradebookItem().getGradebookCategory().getOrder()));
			
			if (comp == 0)
			{
				//comp = one.getGradebookItem().getTitle().compareTo(two.getGradebookItem().getTitle());
				comp = Integer.valueOf(one.getGradebookItem().getDisplayOrder()).compareTo(Integer.valueOf(two.getGradebookItem().getDisplayOrder()));
			}
			else
			{
				//return comp;
			}
		}
    	
    	// sort ascending or descending
		if (!asc)
		{
			comp = -comp;
		}
    				
		return comp;
	}

}
