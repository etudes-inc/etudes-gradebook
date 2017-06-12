/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-impl/impl/src/java/org/etudes/gradebook/impl/GradingStorage.java $
 * $Id: GradingStorage.java 12452 2016-01-06 00:29:51Z murthyt $
 ***********************************************************************************
 *
 * Copyright (c) 2014, 2015, 2016 Etudes, Inc.
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
package org.etudes.gradebook.impl;

import java.util.List;
import java.util.Map;

import org.etudes.gradebook.api.Gradebook;
import org.etudes.gradebook.api.Gradebook.CategoryType;
import org.etudes.gradebook.api.GradebookCategory;
import org.etudes.gradebook.api.GradebookCategoryItemMap;
import org.etudes.gradebook.api.GradingScale;
import org.etudes.gradebook.api.Notes;
import org.etudes.gradebook.api.UserGrade;

public interface GradingStorage
{
	/**
	 * Adds new categories, modifies existing, and deletes existing categories that are not part of the list
	 * 
	 * @param context	Context
	 * 
	 * @param categoryType			Category type of the categories that is to be modified
	 * 
	 * @param gradebookCategories	Gradebook categories list
	 * 
	 * @param modifiedByUserId	Modified by user id 
	 */
	void addModifyDeleteGradebookCategories(String context, Gradebook.CategoryType categoryType, List<GradebookCategory> gradebookCategories, String modifiedByUserId);
	
	/**
	 * Adds new categories, modifies existing, and deletes existing categories that are not part of the list
	 * 
	 * @param context	Context
	 * 
	 * @param gradebookCategories	Gradebook categories list
	 * 
	 * @param modifiedByUserId	Modified by user id 
	 */
	//void addModifyDeleteGradebookCategories(String context, List<GradebookCategory> gradebookCategories, String modifiedByUserId);
	
	/**
	 * Adds, deletes or updates category type mapped items
	 * 
	 * @param context				Context
	 * 
	 * @param categoryType			Gradebook category type of the list of the mapped items to be updated
	 * 
	 * @param contextCategoryItems	Context category items
	 */
	void addModifyDeleteGradebookCategoryMappedItems(String context, Gradebook.CategoryType categoryType, List<GradebookCategoryItemMap> contextCategoryItems);
	
	/**
	 * Adds, deletes or updates selected or current gradebook category type mapped items
	 * 
	 * @param context		Context
	 * 
	 * @param contextCategoryItems	Context category items
	 */
	//void addModifyDeleteGradebookCategoryMappedItems(String context, List<GradebookCategoryItemMap> contextCategoryItems);
	
	/**
	 * Initialize.
	 */
	void init();
	
	/**
	 * Inserts if note is not existing or updates if already existing
	 * 
	 * @param gradebookId	Gradebook id
	 * 
	 * @param instructorUserNotes	Notes with previous added date if note is existing
	 */
	void insertUpdateInstructorUserNotes(int gradebookId, Notes instructorUserNotes);
	
	/**
	 * Gets the gradebook if existing
	 * 
	 * @param gradebookId	Gradebook id
	 * 
	 * @return	The gradebook if existing or null
	 */
	Gradebook selectGradebook(int gradebookId);
	
	/**
	 * Gets the context gradebook. If the gradebook is not existing for the context it's created
	 * 
	 * @param context	Context
	 * 
	 * @param userId	user id
	 * 
	 * @return	The context gradebook
	 * 
	 */
	Gradebook selectGradebook(String context, String userId);
	
	/**
	 * Gets the gradebook category
	 * 
	 * @param categoryId	Category id
	 * 
	 * @return	The gradebook category if exising or null
	 */
	GradebookCategory selectGradebookCategory(int categoryId);
	
	/**
	 * Get's the Gradebook Category and Item Map list
	 * 
	 * @param gradebookId	Gradebook id
	 * 
	 * @param gradebookCategoryType Gradebook category type
	 * 
	 * @return	The Gradebook Category and Item Map list
	 */
	List<GradebookCategoryItemMap> selectGradebookCategoryMappedItems(int gradebookId, CategoryType gradebookCategoryType);
	
	/**
	 * Gets the gradebook category mapped items count
	 * 
	 * @param gradebookId	Gradebook id
	 * 
	 * @param gradebookCategoryType Gradebook category type
	 * 
	 * @return	The gradebook category mapped items count
	 */
	int selectGradebookCategoryMappedItemsCount(int gradebookId, CategoryType gradebookCategoryType);
	
	/**
	 * Gets notes added by instructor for student
	 * 
	 * @param gradebookId	Gradebook id
	 * 	
	 * @param studentId	Student id
	 * 
	 * @return	Notes or null if no notes
	 */
	Notes selectInstructorUserNotes(int gradebookId, String studentId);
	
	/**
	 * Gets the instructor notes added about the user
	 * 
	 * @param gradebookId	Gradebook id
	 * 
	 * @return	Map of instructor notes 
	 */
	Map<String, Notes> selectInstructorUsersNotes(int gradebookId);
	
	/**
	 * Gets the item category for the current gradebook category type
	 * 
	 * @param itemId	Item id
	 * 
	 * @param context	Context
	 * 
	 * @return	The item category or null if item doesn't exist
	 */
	GradebookCategory selectItemGradebookCategory(String itemId, String context);
	
	/**
	 * Gets student's overridden or assigned grade
	 * 
	 * @param gradebookId		Gradebook id
	 * 
	 * @param studentId			Student id
	 * 
	 * @return Student's overridden or assigned grade or null
	 */
	UserGrade selectUserGrade(int gradebookId, String studentId);
	
	/**
	 * Get's student overridden grade history
	 * 
	 * @param gradebookId	Gradebook id
	 * 
	 * @param studentId		Student id
	 * 
	 * @return 	Student's overridden or assigned grade history if exists or null
	 */
	UserGrade selectUserGradeHistory(int gradebookId, String studentId);
	
	/**
	 * Gets the overridden  or assigned user grades
	 * 
	 * @param gradebookId	Gradebook id
	 * 
	 * @return	Map of the assigned user grades
	 */
	Map<String, UserGrade> selectUserGrades(int gradebookId);
	
	/**
	 * Gets the overridden or assigned grades count
	 * 
	 * @param gradebookId	Gradebook id
	 * 
	 * @return	The count of overridden or assigned grade
	 */
	int selectUserGradesCount(int gradebookId);
	
	
	/**
	 * Gets the assigned user grades history
	 * 
	 * @param gradebookId	Gradebook id
	 * 
	 * @return	Map of the assigned user grades history. Only one record for each user if exists
	 */
	Map<String, UserGrade> selectUserGradesHistory(int gradebookId);
	
	/**
	 * Updates the gradebook and it's default grading scale
	 * 
	 * @param gradebook		Gradebook
	 */
	void update(Gradebook gradebook);
	
	/**
	 * Updates gradebook attributes (sort type, release grades type, show letter grade, boost user grades type, boost user grades by)
	 * 
	 * @param gradebook	Gradebook
	 */
	void updateGradebookAttributes(Gradebook gradebook);
	
	/**
	 * Updates existing gradebook categories
	 * 
	 * @param context	Context
	 * 
	 * @param gradebookCategories	Modified existing categories
	 * 
	 * @param modifiedByUserId	Modified user id
	 */
	void updateGradebookCategories(String context, List<GradebookCategory> gradebookCategories, String modifiedByUserId);
	
	/**
	 * updates gradebook categories drop lowest scores number
	 * '
	 * @param context	Context
	 * 
	 * @param gradebookCategories	Categories with modified drop lowest scores number
	 * 
	 * @param modifiedByUserId	Modified user id
	 */
	void updateGradebookCategoriesDropLowestScoresNumber(String context, List<GradebookCategory> gradebookCategories, String modifiedByUserId);
	
	/**
	 * If type is changed updates gradebook category type and removes all existing categories and add default categories
	 * 
	 * @param context		Context
	 * 
	 * @param categoryType	Category type
	 * 
	 * @param modifiedByUserId	Modified by user id
	 */
	void updateGradebookCategoryType(String context, Gradebook.CategoryType categoryType, String modifiedByUserId);
	
	/**
	 * Updates grading scale percents
	 * 
	 * @param gradebookId		Gradebook id
	 * 
	 * @param gradingScale		Modified grading scale with percentages
	 */
	void updateGradingScale(int gradebookId, GradingScale gradingScale);
	
	/**
	 * Updates category id, display order of current gradebook category type's mapped item
	 * 
	 * @param context	Context
	 * 
	 * @param itemId	Item id
	 * 
	 * @param categoryId	Category id
	 */
	void updateItemCategoryMap(String context, String itemId, int categoryId);
	
	/**
	 * Adds or updates user assigned grades. Send user grades with fetched assigned dates from database to avoid saving stale data(data already saved after fetching). For new record there will be no previous assigned date.
	 * 
	 * @param userLetterGrades	Modified or added letter grades. For modified letter grades previous assigned date should be present to avoid saving stale date(data already saved after fetching)
	 * 
	 * @param assignedByUserId	Assigned by user id
	 */
	void updateUserGrades(int gradebookId, List<UserGrade> userLetterGrades, String assignedByUserId);
	
	/**
	 * Adds or updates user assigned grades 
	 * 
	 * @param gradebookId			Gradebook id
	 * 
	 * @param userLetterGrades		map of studemt id's with letter grades
	 * 
	 * @param assignedByUserId		Assigned by user id
	 */
	void updateUserGrades(int gradebookId, Map<String, String> userLetterGrades, String assignedByUserId);
}
