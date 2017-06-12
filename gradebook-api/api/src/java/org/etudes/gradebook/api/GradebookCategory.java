/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-api/api/src/java/org/etudes/gradebook/api/GradebookCategory.java $
 * $Id: GradebookCategory.java 10867 2015-05-15 23:21:59Z murthyt $
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

import org.etudes.gradebook.api.Gradebook.CategoryType;


public interface GradebookCategory
{
	public enum WeightDistribution
	{
		Equally(1), Points(2);

		static public WeightDistribution getWeightDistribution(int code)
		{
			if (code == WeightDistribution.Equally.getCode())
			{
				return WeightDistribution.Equally;
			}
			else if (code == WeightDistribution.Points.getCode())
			{
				return WeightDistribution.Points;
			}
			
			return null;
		}

		private final int code;

		private WeightDistribution(int code)
		{
			this.code = Integer.valueOf(code);
		}
		
		public Integer getCode()
		{
			return code;
		}
	}
	
	//public boolean equals(Object object);

	/**
	 * @return the categoryType
	 */
	CategoryType getCategoryType();

	/**
	 * @return the createdByUserId
	 */
	String getCreatedByUserId();

	/**
	 * @return the dropNumberLowestScores
	 */
	int getDropNumberLowestScores();

	/**
	 * @return the gradebookId
	 */
	int getGradebookId();

	/**
	 * @return the id
	 */
	int getId();

	/**
	 * @return the itemCount
	 */
	int getItemCount();

	/**
	 * @return the itemsTotalpoints
	 */
	Float getItemsTotalpoints();

	/**
	 * @return the modifiedByUserId
	 */
	String getModifiedByUserId();
	
	/**
	 * @return the order
	 */
	int getOrder();

	/**
	 * @return the standardCategoryCode. For default category created the standard category code has value for custom categories created the value is 0.
	 */
	int getStandardCategoryCode();
	
	/**
	 * @return the title
	 */
	String getTitle();
	
	/**
	 * @param extraCredit the extraCredit to set
	 */
	// void setExtraCredit(boolean extraCredit);

	/**
	 * @return the weight
	 */
	Float getWeight();

	/**
	 * @return the weightDistribution
	 */
	WeightDistribution getWeightDistribution();

	/**
	 * @return the extraCredit
	 */
	boolean isExtraCredit();

	/**
	 * @param dropNumberLowestScores the dropNumberLowestScores to set
	 */
	void setDropNumberLowestScores(int dropNumberLowestScores);
	
	/**
	 * @param order the order to set
	 */
	void setOrder(int order);
	
	/**
	 * @param title the title to set
	 */
	void setTitle(String title);
	
	/**
	 * @param weight the weight to set
	 */
	void setWeight(Float weight);
	
	/**
	 * @param weightDistribution the weightDistribution to set
	 */
	void setWeightDistribution(WeightDistribution weightDistribution);
}
