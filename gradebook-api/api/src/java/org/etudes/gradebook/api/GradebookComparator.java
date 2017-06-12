/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-api/api/src/java/org/etudes/gradebook/api/GradebookComparator.java $
 * $Id: GradebookComparator.java 10416 2015-04-03 17:33:45Z murthyt $
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

import java.util.Comparator;

import org.etudes.gradebook.api.Gradebook.GradebookSortType;

public class GradebookComparator implements Comparator<GradebookItem>
{
	// the criteria - asc
	boolean asc = true;

	// the criteria
	GradebookSortType sortType = null;

	public GradebookComparator(GradebookSortType sortType, boolean asc)
	{
		this.sortType = sortType;
		this.asc = asc;
	} // constructor

	public int compare(GradebookItem one, GradebookItem two) 
	{
    	int comp = -1;
    	
    	if (sortType == GradebookSortType.Title)
		{
			// sorted item title
			comp = one.getTitle().compareToIgnoreCase(two.getTitle());
		}
		else if (sortType == GradebookSortType.DueDate)
		{
			// Sort by name if no date on either
			if (one.getDueDate() == null && two.getDueDate() == null)
			{
				return one.getTitle().compareTo(two.getTitle());
			}
	
			// Null dates are last
			if (one.getDueDate() == null)
			{
				return 1;
			}
			if (two.getDueDate() == null)
			{
				return -1;
			}
	
			// Sort by name if both assignments have the same date
			comp = (one.getDueDate().compareTo(two.getDueDate()));
			
			if (comp == 0)
			{
				comp = one.getTitle().compareTo(two.getTitle());
			}
			else
			{
				//return comp;
			}
		}
		else if (sortType == GradebookSortType.OpenDate)
		{
			// Sort by name if no date on either
			if (one.getOpenDate() == null && two.getOpenDate() == null)
			{
				return one.getTitle().compareTo(two.getTitle());
			}
	
			// Null dates are last
			if (one.getOpenDate() == null)
			{
				return 1;
			}
			if (two.getOpenDate() == null)
			{
				return -1;
			}
	
			// Sort by name if both assignments have the same date
			comp = (one.getOpenDate().compareTo(two.getOpenDate()));
			
			if (comp == 0)
			{
				comp = one.getTitle().compareTo(two.getTitle());
			}
			else
			{
				//return comp;
			}
		}
		else if (sortType == GradebookSortType.Category)
		{
			// sort by category type/order(if sorted by category order it's like sorted by category) as primary sort and display order as second sort
			if (one.getGradebookCategory() == null && two.getGradebookCategory() == null)
			{
				return Integer.valueOf(one.getDisplayOrder()).compareTo(Integer.valueOf(two.getDisplayOrder()));
			}
			
			// Null categories are placed last
			if (one.getGradebookCategory() == null)
			{
				return 1;
			}
			if (two.getGradebookCategory() == null)
			{
				return -1;
			}
			
			// Sort by display order if both have the same category 
			comp = Integer.valueOf(one.getGradebookCategory().getOrder()).compareTo(Integer.valueOf(two.getGradebookCategory().getOrder()));
			
			if (comp == 0)
			{
				comp = Integer.valueOf(one.getDisplayOrder()).compareTo(Integer.valueOf(two.getDisplayOrder()));
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
