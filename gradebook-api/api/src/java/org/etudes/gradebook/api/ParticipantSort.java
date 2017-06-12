/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-api/api/src/java/org/etudes/gradebook/api/ParticipantSort.java $
 * $Id: ParticipantSort.java 11202 2015-07-07 17:47:19Z rashmim $
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

/**
 * ParticipantSort are the sort options for the student's grades views
 */
public enum ParticipantSort implements Comparator<Participant>
{
	name_a("0", "A"), name_d("0", "D"), group_title_a("1", "A"), group_title_d("1", "D"), status_a("2", "A"), status_d("2", "D"), score_a("3","A"), score_d("3", "D"),final_a("4","A"),final_d("4","D") ;

	private String sortCode = null;

	private String sortDirection = null;

	private ParticipantSort(String sortCode, String sortDirection)
	{
		this.sortCode = sortCode;
		this.sortDirection = sortDirection;
	}

	public int compare(Participant arg0, Participant arg1)
	{
		int rv = 0;

		// primary sort
		switch (this)
		{
			case name_a:
			case name_d:
			{
				rv = CompareHelper.stringCompare(arg0.getSortName(), arg1.getSortName());
				break;
			}
			case status_a:
			case status_d:
			{
				rv = CompareHelper.participantStatusCompare(arg0.getStatus(), arg1.getStatus());
				break;
			}
			case group_title_a:
			case group_title_d:
			{
				rv = CompareHelper.stringCompare(arg0.getGroupTitle(), arg1.getGroupTitle());
				break;
			}
			case score_a:
			case score_d:
			{
				rv = CompareHelper.floatCompare(arg0.getTotalScore(), arg1.getTotalScore());
				break;
			}
			case final_a:
			case final_d:
			{
				rv = CompareHelper.floatCompare(arg0.getGrade().getAveragePercent(), arg1.getGrade().getAveragePercent());
				break;
			}
		}

		// secondary sort (on name)
		if (rv == 0)
		{
			rv = CompareHelper.stringCompare(arg0.getSortName(), arg1.getSortName());
		}

		// descending, reverse
		if ("D".equals(getSortDirection()))
		{
			rv = -1 * rv;
		}

		return rv;
	}

	public String getSortCode()
	{
		return this.sortCode;
	}

	public String getSortDirection()
	{
		return this.sortDirection;
	}
}
