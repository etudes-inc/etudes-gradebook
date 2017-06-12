/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-api/api/src/java/org/etudes/gradebook/api/CompareHelper.java $
 * $Id: CompareHelper.java 11193 2015-07-06 18:01:52Z rashmim $
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

import java.util.Date;

/**
 * CompareHelper defines some safe comparison operations.
 */
public class CompareHelper
{
	public static int booleanCompare(Boolean arg0, Boolean arg1)
	{
		if ((arg0 == null) && (arg1 == null)) return 0;
		if (arg0 == null) return -1;
		if (arg1 == null) return 1;
		return (arg0.compareTo(arg1));
	}

	public static int dateCompare(Date arg0, Date arg1)
	{
		if ((arg0 == null) && (arg1 == null)) return 0;
		if (arg0 == null) return -1;
		if (arg1 == null) return 1;
		return (arg0.compareTo(arg1));
	}

	public static int floatCompare(Float arg0, Float arg1)
	{
		if ((arg0 == null) && (arg1 == null)) return 0;
		if (arg0 == null) return -1;
		if (arg1 == null) return 1;
		return (arg0.compareTo(arg1));
	}
	
	public static int integerCompare(Integer arg0, Integer arg1)
	{
		if ((arg0 == null) && (arg1 == null)) return 0;
		if (arg0 == null) return -1;
		if (arg1 == null) return 1;
		return (arg0.compareTo(arg1));
	}

	public static int participantStatusCompare(ParticipantStatus arg0, ParticipantStatus arg1)
	{
		if ((arg0 == null) && (arg1 == null)) return 0;
		if (arg0 == null) return -1;
		if (arg1 == null) return 1;
		return (arg0.getSortValue().compareTo(arg1.getSortValue()));
	}

	public static int stringCompare(String arg0, String arg1)
	{
		if ((arg0 == null) && (arg1 == null)) return 0;
		if (arg0 == null) return -1;
		if (arg1 == null) return 1;
		return (arg0.compareToIgnoreCase(arg1));
	}
}
