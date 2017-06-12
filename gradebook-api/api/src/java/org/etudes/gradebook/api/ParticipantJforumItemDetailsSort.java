/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-api/api/src/java/org/etudes/gradebook/api/ParticipantJforumItemDetailsSort.java $
 * $Id: ParticipantJforumItemDetailsSort.java 11193 2015-07-06 18:01:52Z rashmim $
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
 * ParticipantJforumItemDetailsSort are the sort options for the OverviewService's getParticipantJforumItemDetails.
 */
public enum ParticipantJforumItemDetailsSort implements Comparator<ParticipantItemDetails>
{
	name_a("0", "A"), name_d("0", "D"), posts_a("2", "A"), posts_d("2", "D"), status_a("1", "A"), status_d("1", "D"), reviewed_a("3", "A"), reviewed_d("3", "D"), finished_a("4", "A"), finished_d("4",
			"D"), started_a("5", "A"), started_d("5", "D"), score_a("6", "A"), score_d("6", "D");

	static public ParticipantJforumItemDetailsSort sortFromCodes(String sortCode, String sortDirection)
	{
		for (ParticipantJforumItemDetailsSort s : ParticipantJforumItemDetailsSort.values())
		{
			if (s.getSortCode().equals(sortCode) && s.getSortDirection().equals(sortDirection))
			{
				return s;
			}
		}
		return null;
	}

	private String sortCode = null;

	private String sortDirection = null;

	private ParticipantJforumItemDetailsSort(String sortCode, String sortDirection)
	{
		this.sortCode = sortCode;
		this.sortDirection = sortDirection;
	}

	public int compare(ParticipantItemDetails arg0, ParticipantItemDetails arg1)
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
			case posts_a:
			case posts_d:
			{
				rv = CompareHelper.integerCompare(arg0.getCount(), arg1.getCount());
				break;
			}
			case reviewed_a:
			case reviewed_d:
			{
				rv = CompareHelper.dateCompare(arg0.getReviewedDate(), arg1.getReviewedDate());
				break;
			}
			case started_a:
			case started_d:
			{
				rv = CompareHelper.dateCompare(arg0.getStartedDate(), arg1.getStartedDate());
				break;
			}
			case finished_a:
			case finished_d:
			{
				rv = CompareHelper.dateCompare(arg0.getFinishedDate(), arg1.getFinishedDate());
				break;
			}
			case score_a:
			case score_d:
			{
				rv = CompareHelper.floatCompare(arg0.getScore(), arg1.getScore());
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
