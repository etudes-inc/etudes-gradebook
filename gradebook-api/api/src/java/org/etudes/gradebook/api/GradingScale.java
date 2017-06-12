/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-api/api/src/java/org/etudes/gradebook/api/GradingScale.java $
 * $Id: GradingScale.java 9259 2014-11-19 20:36:34Z murthyt $
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

import java.util.List;


public interface GradingScale
{
	public enum GradingScaleType
	{
		LetterGrade(1), LetterGradePlusMinus(2), PassNotPass(3);
		
		private final int scaleType;

		private GradingScaleType(int scaleType)
		{
			this.scaleType = Integer.valueOf(scaleType);
		}

		public Integer getScaleType()
		{
			return scaleType;			
		}
	}
	
	/**
	 * @return the id
	 */
	int getId();
	
	/**
	 * @return the name
	 */
	String getName();
		
	/**
	 * @return the scaleCode
	 */
	String getScaleCode();
	
		
	/**
	 * @return the type
	 */
	GradingScaleType getType();
	
	/**
	 * @return the version
	 */
	int getVersion();
	
	/**
	 * @return the locked
	 */
	boolean isLocked();
	
	/**
	 * @return the gradingScalePercent
	 */
	List<GradingScalePercent> getGradingScalePercent();
}
