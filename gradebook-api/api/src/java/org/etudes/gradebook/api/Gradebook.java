/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-api/api/src/java/org/etudes/gradebook/api/Gradebook.java $
 * $Id: Gradebook.java 10582 2015-04-23 22:28:12Z murthyt $
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

import java.util.List;

public interface Gradebook
{
	public enum BoostUserGradesType
	{
		percent(2), points(1);

		private final int code;

		private BoostUserGradesType(int code)
		{
			this.code = Integer.valueOf(code);
		}

		public Integer getCode()
		{
			return code;
		}
	}
	
	/*category type. Can be set weighted averages*/
	public enum CategoryType
	{
		Custom(2), Standard(1);
		
		private final int code;

		private CategoryType(int code)
		{
			this.code = Integer.valueOf(code);
		}

		public Integer getCode()
		{
			return code;			
		}
	}
	
	public enum GradebookSortType
	{
		Category(0), DueDate(1), OpenDate(3), Title(2);
		
		private final int code;

		private GradebookSortType(int code)
		{
			this.code = Integer.valueOf(code);
		}

		public Integer getCode()
		{
			return code;			
		}
	}
	
	public enum ReleaseGrades
	{
		All(1), Released(2);
		
		private final int code;

		private ReleaseGrades(int code)
		{
			this.code = Integer.valueOf(code);
		}

		public Integer getCode()
		{
			return code;			
		}
	}
	
	/*standard/default categories*/
	public enum StandardCategory
	{
		// only one extra category is allowed
		assignment(1, "Assignments", false, 1), discussions(4, "Discussions", false, 4), extracredit(5, "Extra Credit", true, 5), offline(3, "Offline Items", false, 3), test(2, "Tests", false, 2);

		private final int code;

		private final boolean extraCreditCategory;

		private final int order;

		private final String title;

		private StandardCategory(int code, String title, boolean extraCreditCategory, int order)
		{
			this.code = code;
			this.title = title;
			this.extraCreditCategory = extraCreditCategory;
			this.order = order;
		}

		/**
		 * @return the code
		 */
		public int getCode()
		{
			return code;
		}
		
		/**
		 * @return the order
		 */
		public int getOrder()
		{
			return order;
		}

		/**
		 * @return the title
		 */
		public String getTitle()
		{
			return title;
		}

		/**
		 * @return the extraCreditType. There should be only one extra credit category 
		 * 			true - extra credit category 
		 *         	false - not an extra credit category
		 */
		public boolean isExtraCreditCategory()
		{
			return extraCreditCategory;
		}
	}
	
	/**
	 * @return the modifiedByUserId
	 */
	public String getModifiedByUserId();	
	
	/**
	 * @return the boostUserGradesBy
	 */
	Float getBoostUserGradesBy();
	
	/**
	 * @return the boostUserGradesType
	 */
	BoostUserGradesType getBoostUserGradesType();
	
	/**
	 * @return the category type
	 */
	CategoryType getCategoryType();
	
	/**
	 * @return the context
	 */
	String getContext();
	
	/**
	 * @return the contextGradingScales
	 */
	List<GradingScale> getContextGradingScales();
	
	/**
	 * @return the createdByUserId
	 */
	String getCreatedByUserId();
	
	/**
	 * @return the gradebookCategories
	 */
	List<GradebookCategory> getGradebookCategories();
	
	/**
	 * @return the gradingScale
	 */
	GradingScale getGradingScale();
	
	/**
	 * @return the id
	 */
	int getId();

	/**
	 * @param gradebookTypes the gradebookTypes to set
	 */
	//void setGradebookTypes(List<GradebookType> gradebookTypes);
	
	/**
	 * @return the releaseGrades
	 */
	ReleaseGrades getReleaseGrades();
	
	/**
	 * @return the sortType
	 */
	GradebookSortType getSortType();
	
	/**
	 * @return the dropLowestScore
	 */
	Boolean isDropLowestScore();

	/**
	 * @return the showLetterGrade
	 */
	Boolean isShowLetterGrade();
}
