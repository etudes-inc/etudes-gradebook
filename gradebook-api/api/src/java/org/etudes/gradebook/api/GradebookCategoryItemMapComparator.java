/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-api/api/src/java/org/etudes/gradebook/api/GradebookCategoryItemMapComparator.java $
 * $Id: GradebookCategoryItemMapComparator.java 11554 2015-09-03 21:39:57Z murthyt $
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

/**
 * compares with category order
 * 
 * @author murthyt
 *
 */
public class GradebookCategoryItemMapComparator implements Comparator<GradebookCategoryItemMap>
{
	// the criteria - asc
	boolean asc = true;
	
	public GradebookCategoryItemMapComparator(boolean asc)
	{
		this.asc = asc;
	} // constructor
	
	public int compare(GradebookCategoryItemMap one, GradebookCategoryItemMap two) 
	{
    	int comp = -1;
    	
    	/* sort by category */
		
		// Null categories are placed last
		if (one.getCategory() == null)
		{
			return 1;
		}
		if (two.getCategory() == null)
		{
			return -1;
		}
		
		// Sort by display order if both have the same category 
		comp = Integer.valueOf(one.getCategory().getOrder()).compareTo(Integer.valueOf(two.getCategory().getOrder()));
		
		// existing item map has display order for new item maps the display order is zero
		if (comp == 0)
		{
			if (one.getDisplayOrder() == 0)
			{
				return 1;
			}
			if (two.getDisplayOrder() == 0)
			{
				return -1;
			}
			
			comp = Integer.valueOf(one.getDisplayOrder()).compareTo(Integer.valueOf(two.getDisplayOrder()));
		}
		else
		{
			//return comp;
		}	
		
		// sort ascending or descending
		if (!asc)
		{
			comp = -comp;
		}
    				
		return comp;
	}

}
