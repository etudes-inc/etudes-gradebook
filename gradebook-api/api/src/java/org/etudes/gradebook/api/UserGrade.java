/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-api/api/src/java/org/etudes/gradebook/api/UserGrade.java $
 * $Id: UserGrade.java 11269 2015-07-15 20:40:49Z murthyt $
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

import java.util.Date;
import java.util.List;

/** overridden or assigned user grades may also include average percent, score, total points */
public interface UserGrade
{
	/**
	 * @return the assignedByUserId
	 */
	 String getAssignedByUserId();
	
	/**
	 * @return the assignedDate
	 */
	 Date getAssignedDate();
	
	/**
	 * @return the averagePercent
	 */
	 Float getAveragePercent();
	
	/**
	 * @return the extraCreditPercent
	 */
	Float getExtraCreditPercent();
	
	/**
	 * @return the extraCreditScore
	 */
	Float getExtraCreditScore();
	
	/**
	 * @return the gradebookId
	 */
	 int getGradebookId();

	/**
	 * @return the assignedLetterGrade
	 */
	 String getLetterGrade();

	/**
	 * @return the points
	 */
	 Float getPoints();
	
	/**
	 * @return the score
	 */
	 Float getScore();
	 
	 /**
	  * @return the totalScore (score plus extra credit score)
	 */
	Float getTotalScore();
	 
	 /**
	 * @return the userCategoryGrade
	 */
	 List<UserCategoryGrade> getUserCategoryGrade();
	 
	 /**
	 * @return the userId
	 */
	 String getUserId();
}
