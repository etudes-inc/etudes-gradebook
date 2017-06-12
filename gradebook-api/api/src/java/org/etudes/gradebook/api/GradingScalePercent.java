/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-api/api/src/java/org/etudes/gradebook/api/GradingScalePercent.java $
 * $Id: GradingScalePercent.java 9561 2014-12-17 17:49:38Z murthyt $
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

public interface GradingScalePercent
{
	/**
	 * @return the letterGrade
	 */
	String getLetterGrade();
	
	/**
	 * @return the percent
	 */
	Float getPercent();
	
	
	/**
	 * @return the scaleId
	 */
	int getScaleId();
	
	/**
	 * @return the sequenceNumber
	 */
	int getSequenceNumber();
}
