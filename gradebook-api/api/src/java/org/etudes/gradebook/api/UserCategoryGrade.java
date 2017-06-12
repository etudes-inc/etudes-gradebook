/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-api/api/src/java/org/etudes/gradebook/api/UserCategoryGrade.java $
 * $Id: UserCategoryGrade.java 10462 2015-04-13 23:38:30Z murthyt $
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

public interface UserCategoryGrade
{
	/**
	 * @return the averagePercent
	 */
	Float getAveragePercent();

	/**
	 * @return the categoryId
	 */
	int getCategoryId();

	/**
	 * @return the gradebookCategory
	 */
	GradebookCategory getGradebookCategory();

	/**
	 * @return the points
	 */
	Float getPoints();
	
	/**
	 * @return the score
	 */
	Float getScore();
}
