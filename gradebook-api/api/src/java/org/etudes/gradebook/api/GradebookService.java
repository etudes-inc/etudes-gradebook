/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-api/api/src/java/org/etudes/gradebook/api/GradebookService.java $
 * $Id: GradebookService.java 12452 2016-01-06 00:29:51Z murthyt $
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
package org.etudes.gradebook.api;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.etudes.gradebook.api.Gradebook.BoostUserGradesType;
import org.etudes.gradebook.api.Gradebook.CategoryType;
import org.etudes.gradebook.api.Gradebook.GradebookSortType;
import org.etudes.gradebook.api.Gradebook.ReleaseGrades;
import org.etudes.gradebook.api.GradebookCategory.WeightDistribution;

public interface GradebookService
{
	/**	Creates if category is not existing, modifies existing categories weights, weight distribution, order, and title. If existing category is not part 
	 * of the list it is deleted. For standard categories title cannot be changed. Standard type categories cannot be deleted
	 * 
	 * @param context	Context
	 * 
	 * @param categoryType		Category type of the categories that is to be modified
	 * 
	 * @param gradebookCategories	Modified or added gradebook categories. If existing categories are not part of the list they are deleted. Standard type categories cannot be deleted
	 * 
	 * @param modifiedByUserId	Modified by user id
	 */
	void addModifyDeleteContextGradebookCategories(String context, Gradebook.CategoryType categoryType, List<GradebookCategory> gradebookCategories, String modifiedByUserId);
	
	/**
	 * For the gradebook current category type creates if category is not existing, modifies existing categories weights, weight distribution, order, and title. If existing category is not part 
	 * of the list it is deleted. For standard categories title cannot be changed. Standard type categories cannot be deleted
	 * 
	 * @param context	Context
	 * 
	 * @param gradebookCategories	Modified or added gradebook categories. If existing categories are not part of the list they are deleted. Standard type categories cannot be deleted
	 * 
	 * @param modifiedByUserId	Modified by user id
	 */
	void addModifyDeleteContextGradebookCategories(String context, List<GradebookCategory> gradebookCategories, String modifiedByUserId);

	/**
	 * Adds new not if not existing else updates existing note about user
	 * 
	 * @param context	Context
	 * 
	 * @param instructorUserNotes	Instructor added note about user with previous added note date if existing
	 */
	void addModifyInstructorUserNotes(String context, Notes instructorUserNotes);
	
	/**
	 * Check if the given user is allowed to edit the gradebook.
	 * 
	 * @param context
	 *        The site id.
	 * @param userId
	 *        The user id.
	 * @return TRUE if the user has access, FALSE if not.
	 */
	Boolean allowEditGradebook(String context, String userId);
	
	/**
	 * Check if the given user is allowed to access gradebook for the site and user.
	 * 
	 * @param context
	 *        The site id.
	 * @param userId
	 *        The user id.
	 * @return TRUE if the user has access, FALSE if not.
	 */
	Boolean allowGetGradebook(String context, String userId);
	
	/**
	 * Gets the class total points and average. nonExtraCreditCategoriesTotalPoints, extraCreditCategoryTotalPoints and classAveragePercent are the keys in the map. works with methods {@link GradebookService#getUsersGradebookSummary} and {@link GradebookService#getUsersGradebookSummaryAndGradeBookItems}
	 * 
	 * @param context	Context
	 * 
	 * @param userId	User id
	 * 
	 * @return	The class total points and average
	 */
	Map<String, Float>getClassPointsAverage(String context, String userId);
	
	/**
	 * Gets the context gradebook
	 * 
	 * @param context	Context
	 * 
	 * @param fetchedByUserId	User id fetching the gradebook, should be valid user of the context
	 * 
	 * @return The context gradebook or null if user has no access to site
	 */
	Gradebook getContextGradebook(String context, String fetchedByUserId);
	
	/**
	 * Gets the gradebook item with scores
	 * 
	 * @param itemId	Item id
	 * 
	 * @param context	Context
	 * 
	 * @param groupId 		Group id or section id. if null fetches all the participants
	 * 
	 * @param userId	Fetched by user id
	 * 
	 * @return	The gradable item
	 */
	GradebookItem getGradebookItem(String itemId, String context, String groupId, String fetchedByUserId);
	
	/**
	 * Gets the gradebook category of the item for current gradebook category type 
	 * 
	 * @param itemId		Item id
	 * 
	 * @param context		Context
	 * 
	 * @param fetchedByUserId	Fetched by user id
	 * 
	 * @return	Gradebook category of the item for current gradebook category type or null if item not existing
	 */
	GradebookCategory getGradebookItemCategory(String itemId, String context, String fetchedByUserId);
	
	
	/**
	 * Get detail information for a JForum item (category, topic, or forum) for all "Student" participants in the site.
	 * 
	 * @param context	The site id.
	 * 
	 * @param itemId    The JForum item id.
	 *        
	 * @param groupId 	Group id or section id. if null fetches all the participants
	 * 
	 * @param sort		sort criteria.
	 * 
	 * @param userId	Fetched by user id
	 * 
	 * @return a List of ParticipantJforumItemOverview.
	 */
	// List<ParticipantItemDetails> getJforumItemDetails(String context, String itemId, String groupId, ParticipantJforumItemDetailsSort sort, String fetchedByUserId);
	
	/**
	 * Get all of the tool items with no merging and no other processing.
	 * @param context
	 * @param userId
	 * @param fetchScores
	 * @param fetchUnpublish
	 * @param sortType
	 * @param sortAscending
	 * @param itemType
	 * @return
	 */
	List<GradebookItem> getImportGradebookItems(String context, String userId, boolean fetchScores, boolean fetchUnpublish, GradebookSortType sortType, Boolean sortAscending, GradebookItemType itemType);
	
	/**
	 * Gets participants mneme item details
	 * 
	 * @param context	Context
	 * 
	 * @param itemId	Item id
	 * 
	 * @param groupId 	Group id or section id. if null fetches all the participants
	 * 
	 * @param sort sort
	 * 
	 * @param fetchedByUserId	Fetched by user id
	 * 
	 * @return	A List of ParticipantMnemeItemDetail.
	 */
	// List<ParticipantItemDetails> getMnemeItemDetails(String context, String itemId, String groupId, ParticipantMnemeItemDetailsSort sort, String fetchedByUserId);
	
	/**
	 * Get detail information for a JForum item (category, topic, or forum) for all "Student" participants in the site.
	 * 
	 * @param context		The site id
	 * 
	 * @param itemId		The JForum item id
	 * 
	 * @param groupId		Group id or section id. if null fetches all the participants
	 * 
	 * @param sort			Sort criteria
	 * 
	 * @param fetchedByUserId	Fetched by user id
	 * 
	 * @param allEvaluations	If true all evaluations including not released else only released evaluations
	 * 
	 * @return	A List of ParticipantJforumItemOverview
	 */
	List<ParticipantItemDetails> getJforumItemDetails(String context, String itemId, String groupId, ParticipantJforumItemDetailsSort sort, String fetchedByUserId, boolean allEvaluations);

	/**
	 * Gets participants mneme item details
	 * 
	 * @param context	Context
	 * 
	 * @param itemId	Item id
	 * 
	 * @param groupId	Group id or section id. if null fetches all the participants
	 * 
	 * @param sort		Sort
	 * 
	 * @param fetchedByUserId	Fetched by user id
	 * 
	 * @param allSubmissions	If true all submissions of the user else only released and the best submission
	 * 
	 * @return	A List of ParticipantMnemeItemDetail
	 */
	List<ParticipantItemDetails> getMnemeItemDetails(String context, String itemId, String groupId, ParticipantMnemeItemDetailsSort sort, String fetchedByUserId, boolean allSubmissions);
	
	/**
	 * Gets participant basic details(userId, status, sortName, displayId, groupTitle)
	 * 
	 * @param context	Context
	 * 
	 * @param userId	User id
	 * 
	 * @return	Participant or null if user doesn't exist
	 */
	Participant getParticipant(String context, String userId);
	
	/**
	 * Gets participant basic details(userId, status, sortName, displayId, groupTitle) and instructor notes
	 * 
	 * @param context	Context
	 * 
	 * @param userId	User id
	 * 
	 * @param fetchInstrucorNotes	If true fetches instructor added notes for student
	 * 
	 * @return	Participant or null if user doesn't exist
	 */
	Participant getParticipant(String context, String userId, boolean fetchInstrucorNotes);
	
	/**
	 * Gets the context sections
	 * 
	 * @param context	Context
	 * 
	 * @return	The context sections with id as key and title as value of the map
	 */
	HashMap<String, String> getSections(String context);
	
	/**
	 * Gets tools gradable items as per display order of gradebook categories. If the category has no items the gradebook item list will be empty.
	 * 
	 * @param context	Context
	 * 
	 * @param userId	user id
	 * 
	 * @param 	fetchScores true - fetches scores
	 * 					  	false - scores not fetched
	 * 
	 * @return	Tools gradable items as per display order of gradebook categories
	 */
	Map<GradebookCategory, List<GradebookItem>> getToolGradebookItems(String context, String userId, boolean fetchScores, boolean fetchUnpublish);
	
	/**
	 * Gets the gradable items in the tools
	 * 
	 * @param 	context	Context
	 * 
	 * @param 	userId	User id
	 * 
	 * @param 	fetchScores true - fetches scores
	 * 					  	false - scores not fetched
	 * 
	 * @param	sortType	Sort type, null if to be sorted by gradebook preference
	 * 
	 * @param	sortAscending	If true sorted by ascending else descending, null if to be sorted by gradebook sort preference
	 * 
	 * @param	itemType 	Filters by item type
	 * 
	 * @return	The gradable items in the tools
	 */
	List<GradebookItem> getToolGradebookItems(String context, String userId, boolean fetchScores, boolean fetchUnpublish, GradebookSortType sortType, Boolean sortAscending, GradebookItemType itemType);
	
	/**
	 * Gets the user letter grade and average percent scored
	 * 
	 * @param context	Context
	 * 
	 * @param userId	User id
	 * 
	 * @return	The user letter grade and average percent scored
	 */
	UserGrade getUserGrade(String context, String userId);
	
	/**
	 * Gets the user overridden letter grade of the last change
	 * 
	 * @param context	Context
	 * 
	 * @param userId	user id
	 * 
	 * @return	The user overridden letter grade of the last change
	 */
	UserGrade getUserGradeLog(String context, String userId);

	/**
	 * Gets users gradebook summary (student name, section, status, total score out of total points, over all grade, and overridden grade etc)
	 * 
	 * @param context		Context
	 * 
	 * @param groupId 		Group id or section id. if null fetches all the participants
	 * 
	 * @param userId		User id
	 * 
	 * @param sort			Sort by name or group or status 
	 * 
	 * @return	Users gradebook summary (student name, section, status, total score out of total points, over all grade, and overridden grade etc)
	 */
	List<Participant> getUsersGradebookSummary(String context, String groupId, String userId, ParticipantSort sort);
	
	/**
	 * Gets the user gradebook summary(student name, section, status, total score out of total points, over all grade, and overridden grade etc) and users gradebook items list
	 * 
	 * @param context	Context
	 * 
	 * @param groupId	Group/section id
	 * 
	 * @param userId	user id
	 * 
	 * @param sort		Sort by name or group or status
	 * 
	 * @param itemType 	Filter by item type. If null gets all the items else only items of the item type
	 * 
	 * @return	The user gradebook summary(student name, section, status, total score out of total points, over all grade, and overridden grade etc) and users gradebook items list
	 */
	Map<Participant, List<ParticipantGradebookItem>> getUsersGradebookSummaryAndGradeBookItems(String context, String groupId, String userId, ParticipantSort sort, GradebookItemType itemType);
	
	/**
	 * Gets the user overridden grades count
	 *  
	 * @param context	Context
	 * 
	 * @param userId	Authenticated user id
	 * 
	 * @return	The count of overridden grades
	 */
	int getUsersGradesCount(String context, String userId);
	
	/**
	 * Gets the user gradebook items
	 * 
	 * @param context	Context
	 * 
	 * @param userId	User id
	 * 
	 * @param computeUserGrade 	If true computes user grade and sets lowest dropped grade else user grade is not computed
	 * 
	 * @return	The user gradebook items
	 */
	List<ParticipantGradebookItem> getUserToolGradebookItems(String context, String userId, boolean computeUserGrade);
	
	/**
	 * Gets the user gradebook items
	 * 
	 * @param context	Context
	 * 
	 * @param userId	User id
	 * 
	 * @param fetchedByUserId	User id fetching the items, only instructor can fetch all the submissions
	 * 
	 * @param allSubmissions If true gets all user mneme submissions and jforum evaluations else only released and best mneme submission or best jforum evaluation
	 * 
	 * @return	The user gradebook items
	 */
	List<ParticipantGradebookItem> getUserToolGradebookItems(String context, String userId, String fetchedByUserId, boolean allSubmissions);
	
	/**
	 * Checks for available title
	 * 
	 * 
	 * @param context	Context
	 * 
	 * @param userId	User id
	 * 
	 * @param title		Title
	 * 
	 * @return	True if available else false
	 */
	boolean isTitleAvailable(String context, String userId, String title);
	
	/**
	 * Checks for available title with the given id and item type
	 * 
	 * @param context	Context
	 * 
	 * @param userId	User id
	 * 
	 * @param title		Title
	 * 
	 * @param id		Item id
	 * 
	 * @param itemType	Item type
	 * 
	 * @return	True if available else false
	 */
	boolean isTitleDefined(String context, String userId, String title, String id, GradebookItemType itemType);
	
	/**
	 * Checks for the tools availability in the site
	 * 
	 * @param context	Context.
	 * 
	 * @return	True if true is tool is added to site else false
	 */
	boolean isToolAvailable(String context);
	
	/**
	 * Modifies the gradebook of the site
	 * 
	 * @param context				Context
	 * 
	 * @param showLetterGrade		Show letter grade
	 * 
	 * @param releaseGrades			Release grades type
	 * 
	 * @param gradingScaleId		Modified grading scale id of the gradebook
	 * 	
	 * @param gradingScalePercent	Modified grading scale percentages
	 * 
	 * @param modifiedByUserId				Modified by user id
	 */
	void modifyContextGradebook(String context, Boolean showLetterGrade, ReleaseGrades releaseGrades, int gradingScaleId, Map<String, Float> gradingScalePercent, String modifiedByUserId);
	
	/**
	 * Modifies grade book attributes excluding grading scale
	 * 
	 * @param context			Context
	 * 
	 * @param showLetterGrade	Show letter grade	Changed value or null if not changed
	 * 
	 * @param releaseGrades		Release grades type Changed value or null if not changed
	 * 
	 * @param dropLowestScore	Drop lowest score	Changed value or null if not changed
	 * 
	 * @param modifiedByUserId	Modified by used id
	 */
	void modifyContextGradebookAttributes(String context, Boolean showLetterGrade, ReleaseGrades releaseGrades, Boolean dropLowestScore, String modifiedByUserId);
	
	/**
	 * Modifies gradebook boost by attributes
	 * 
	 * @param context				Context
	 * 
	 * @param boostUserGradesType	Boost user grades type or null
	 * 
	 * @param boostUserGradesBy		Boost user grades by value, should be greater than zero
	 * 
	 * @param modifiedByUserId		Modified by used id
	 */
	void modifyContextGradebookBoostByAttributes(String context, BoostUserGradesType boostUserGradesType, Float boostUserGradesBy, String modifiedByUserId);
	
	/**
	 * Modifies existing categories weights, weight distribution, order, and title. For standard categories title cannot be changed.
	 * 
	 * @param context	Context
	 * 
	 * @param gradebookCategories	Modified gradebook categories
	 * 
	 * @param modifiedByUserId	Modified by user id
	 */
	void modifyContextGradebookCategories(String context, List<GradebookCategory> gradebookCategories, String modifiedByUserId);
	
	/**
	 * Modified existing categories drop lowest scores number
	 * 
	 * @param context			Context
	 * 
	 * @param gradebookCategories	Categories with modified drop lowest scores number
	 * 
	 * @param modifiedByUserId	Modified by user id
	 */
	void modifyContextGradebookCategoriesDropLowestScoresNumber(String context, List<GradebookCategory> gradebookCategories, String modifiedByUserId);
	
	/**
	 * Modifies category type. Removes existing categories and adds default(standard) categories
	 * 
	 * @param context		Context
	 * 
	 * @param categoryType		Category type
	 * 
	 * @param modifiedByUserId	Modified by user id
	 */
	void modifyContextGradebookCategoryType(String context, Gradebook.CategoryType categoryType, String modifiedByUserId);
	
	/**
	 * Modifies the grade book default grading scale of the site
	 * 
	 * @param context				Context
	 * 
	 * @param gradingScaleId		Modified grading scale id of the gradebook
	 * 	
	 * @param gradingScalePercent	Modified grading scale percentages
	 * 
	 * @param modifiedByUserId		Modified by user id
	 */
	void modifyContextGradebookGradingScale(String context, int gradingScaleId, Map<String, Float> gradingScalePercent, String modifiedByUserId);
	
	/**
	 * Modifies the grading scale percentages
	 * 
	 * @param gradebookId			Gradebook id
	 * 
	 * @param gradingScaleId		Grading scale id
	 * 
	 * @param gradingScalePercent	Modified grading scale percentages
	 * 
	 * @param modifiedByUserId		Modified by user id
	 */
	void modifyGradingScale(int gradebookId, int gradingScaleId, Map<String, Float> gradingScalePercent, String modifiedByUserId);
	
	/**
	 * Saves item mapping found in import process.
	 * 
	 * @param context			Context
	 * 
	 * @param categoryType		Category type of the mapping list that is modified
	 * 
	 * @param categoryItemMap	Category item map list
	 * 		The list contains unpublished items mapping. no need to check for unpublished.
	 * 
	 * @param modifiedByUserId	Modified by user id
	 */
	void modifyImportItemMapping(String context, Gradebook.CategoryType categoryType, List<GradebookCategoryItemMap> categoryItemMaps, String modifiedByUserId);
	
	/**
	 * Modifies item category mapping for the current gradebook category type
	 * 
	 * @param context		Context
	 * 
	 * @param itemToolId	Item id from the tool
	 * 
	 * @param categoryId	Category id to be mapped to
	 * 
	 * @param itemtype		Item type
	 * 
	 * @param modifiedByUserId	Modified by user id
	 */
	void modifyItemCategoryMapping(String context, String itemToolId, int categoryId, GradebookItemType itemtype, String modifiedByUserId);
	
	/**
	 * Modify mapping of items with categories and their display order of the gradebook current category type.
	 * 
	 * @param context			Context
	 * 
	 * @param categoryItemMap	Category item map list
	 * 
	 * @param modifiedByUserId	Modified by user id
	 */
	void modifyItemMapping(String context, List<GradebookCategoryItemMap> categoryItemMaps, String modifiedByUserId);
	
	/**
	 * Modifies item user submission scores and released status. Submission id cannot be null in the participantItemDetail. If scores fetched time is not null 
	 * the scores are checked with last updated time to avoid saving the stale data
	 * 
	 * @param context	Context
	 * 
	 * @param itemId	Item id
	 * 
	 * @param itemType	Item type
	 * 
	 * @param participantItemDetails	ParticipantItemDetails with updated score and released status
	 * 
	 * @param modifiedByUserId	Modified by user id
	 * 
	 * @param scoresFetchedTime Scores fetched time if scores fetched time is not null the scores are checked with last updated time to avoid saving the stale data
	 */
	void modifyItemScores(String context, String itemId, GradebookItemType itemType, List<ParticipantItemDetails> participantItemDetails, String modifiedByUserId, Date scoresFetchedTime);
	
	/**
	 * Modify user assigned or overridden grades, previously assigned dates are needed to existing overridden letter grades to avoid saving the stale data
	 * 
	 * @param context			Context
	 * 
	 * @param userLetterGrades	List of overridden letter grades
	 * 
	 * @param modifiedByUserId	User id who is modifying the user grades
	 */
	void modifyUserGrades(String context, List<UserGrade> userLetterGrades, String modifiedByUserId);
	
	/**
	 * Modifies user item scores. If scores fetched time is not null the scores are checked with last updated time to avoid saving the stale data
	 * 
	 * @param context	Context
	 * 
	 * @param userId	User id
	 * 
	 * @param participantGradebooItems	Updated user scores and released status
	 * 
	 * @param modifiedByUserId	Modified by user id
	 * 
	 * @param scoresFetchedTime	Scores fetched time if scores fetched time is not null the scores are checked with last updated time to avoid saving the stale data
	 */
	void modifyUserScores(String context, String userId, List<ParticipantGradebookItem> participantGradebookItems, String modifiedByUserId, Date scoresFetchedTime);
	
	/**
	 * Creates new instance of Gradebook Category
	 * 
	 * @param title			Title
	 * 
	 * @param weight		Weight
	 * 
	 * @param weightDistribution	Weight distribution
	 * 
	 * @param order			Order
	 * 
	 * @return	New instance of Gradebook Category
	 */
	GradebookCategory newGradebookCategory(String title, Float weight, WeightDistribution weightDistribution, int order);
	
	/**
	 * 
	 * @param title
	 * @param weight
	 * @param weightDistribution
	 * @param order
	 * @return
	 */
	GradebookCategory newGradebookCategory(String title, Float weight, WeightDistribution weightDistribution, int order, CategoryType categoryType);
	
	/**
	 * Create new instance of GradebookCategoryItemMap
	 *  
	 * @param itemId		Item id
	 * 
	 * @param categoryId	Category id
	 * 
	 * @param displayOrder	Display order
	 * 
	 * @return New instance of GradebookCategoryItemMap
	 */
	GradebookCategoryItemMap newGradebookCategoryItemMap(String itemId, int categoryId, int displayOrder);
	
	/**
	 * Creates new instance of GradebookItem
	 * 
	 * @param id		Id
	 * 
	 * @param title		Title
	 * 
	 * @param points	Points
	 * 
	 * @param dueDate	Due date
	 * 
	 * @param openDate	Open date
	 * 
	 * @param type		Gradebook item Type
	 * 
	 * @return			New instance of GradebookItem
	 */
	GradebookItem newGradebookItem(String id, String title, Float points, Date dueDate, Date openDate, GradebookItemType type);
	
	/**
	 * Creates new instance of GradebookItem
	 * 
	 * @param id		Id
	 * 
	 * @param title		Title
	 * 
	 * @param points	Points
	 * 
	 * @param averageScore	Averagescore
	 * 
	 * @param due		Due date
	 * 
	 * @param open		Open date
	 * 
	 * @param type		Gradebook item Type
	 * 
	 * @param submittedCount Mneme submission count and jforum number of users who posted one or more posts
	 * 
	 * @return	New instance of GradebookItem
	 */
	GradebookItem newGradebookItem(String id, String title, Float points, Float averageScore, Date dueDate, Date openDate, Date closeDate, GradebookItemType type, GradebookItemAccessStatus accessStatus, Integer submittedCount);
	
	/**
	 * 
	 * @param id
	 * @param gradebookItem
	 * @param participantItemDetails
	 * @return
	 */
	ParticipantGradebookItem newParticipantGradebookItem(String id, GradebookItem gradebookItem, ParticipantItemDetails participantItemDetails);
	
	/**
	 * Called for student's landing page
	 * @param startedDate
	 * @param finishedDate
	 * @param score
	 * @param evaluatedDate
	 * @param reviewedDate
	 * @param evaulationNotReviewed
	 * @param count
	 * @return
	 */
	// ParticipantItemDetails newParticipantItemDetails(Date startedDate, Date finishedDate, Float score, Date evaluatedDate, Date reviewedDate, Boolean evaulationNotReviewed, Integer count);
	
	/**
	 * Creates new instance of ParticipantItemDetails
	 * 
	 * @param startedDate		Started date
	 * 
	 * @param finishedDate		Finished date
	 * 
	 * @param score				Score
	 * 
	 * @param evaluatedDate		Evaluated date
	 * 
	 * @param reviewedDate		Reviewed date
	 * 
	 * @param evaulationNotReviewed			true - if evaluation not reviewed else false
	 * 
	 * @param reviewLink		Review Link
	 * 
	 * @param count				Count
	 * 
	 * @param inProgress		True - if in-progress else not
	 * 
	 * @return	Instance of ParticipantItemDetails
	 */
	// ParticipantItemDetails newParticipantItemDetails(Date startedDate, Date finishedDate, Float score, Date evaluatedDate, Date reviewedDate, Boolean evaulationNotReviewed, String reviewLink, Integer count, Boolean inProgress);
	
	/**
	 * Creates new instance for mneme item details
	 * 
	 * @param id				Id
	 * 
	 * @param displayId			Display id
	 * 
	 * @param groupTitle		Group title
	 * 
	 * @param sortName			Sort name
	 * 
	 * @param status			Status
	 * 
	 * @param evaluatedDate		Evaluated date
	 * 
	 * @param finishedDate		Finished date
	 * 
	 * @param reviewed			Reviewed date
	 * 
	 * @param score				Score
	 * 
	 * @param startedDate		Started date
	 * 
	 * @param inProgress		Inprogress
	 * 
	 * @param suppressDates		Supress dates show or avoid
	 * 
	 * @param evaulationNotReviewed		Evaluation reviewed?
	 * 
	 * @return	New instance of ParticipantMnemeItemDetail
	 */
	// ParticipantItemDetails newParticipantItemDetails(String id, String displayId, String groupTitle, String sortName, ParticipantStatus status, Date evaluatedDate, Date finishedDate, Date reviewed, Float score, Date startedDate, String reviewLink, Boolean inProgress, Boolean suppressDates, Boolean evaulationNotReviewed, Boolean released);
	
	/**
	 * Creates new instance for Jforum item details
	 * 
	 * @param id			User id
	 * 
	 * @param displayId		Display id
	 * 
	 * @param groupTitle	Group Title
	 * 
	 * @param sortName		Sort name
	 * 
	 * @param status		Participant status
	 * 
	 * @param posts			Number of posts
	 * 
	 * @param evaluated		evaluated
	 * 
	 * @param reviewedDate 
	 * 
	 * @param score			score
	 * 
	 * @param lastPostTime 	Last post time
	 * 
	 * @param firstPostTime	First post time
	 * 
	 * @param evaluationNotReviewed true if evaluation reviewed else false
	 * 
	 * @return	New instance of ParticipantJforumItemDetail
	 */
	// ParticipantItemDetails newParticipantItemDetails(String id, String displayId, String groupTitle, String sortName, ParticipantStatus status, Integer posts, Date evaluatedDate, Date reviewedDate, Float score, Date lastPostTime, Date firstPostTime, Boolean evaluationNotReviewed, Boolean released);
		
	/**
	 * Creates new instance for Jforum and mneme participant item details
	 * 
	 * @param id				User id
	 * 
	 * @param displayId			Display id
	 * 
	 * @param groupTitle		Group title
	 * 
	 * @param sortName			Sort name
	 * 
	 * @param status			Participant status
	 * 
	 * @param count				Jforum posts count
	 * 
	 * @param evaluatedDate		Evaluated date
	 * 
	 * @param reviewedDate		Reviewed date
	 * 
	 * @param score				Score
	 * 
	 * @param startedDate		Started date or of first post time
	 * 
	 * @param finishedDate		Finished date of last post time
	 * 
	 * @param evaluationNotReviewed		True if evaluation not reviewed else false
	 * 
	 * @param released			True if score is released else false
	 * 
	 * @param reviewLink		Review link
	 * 
	 * @param gradingLink		Grading link
	 * 
	 * @param inProgress		True is test/quiz/assignment etc in progress else false
	 * 
	 * @param submissionId		Mneme submission id
	 * 
	 * @param isSubmissionLate  TRUE if the submission is completed after the due date, FALSE if not.
	 * 
	 * @param isAutoSubmission  TRUE if the submission was automatically completed, FALSE if is was completed by the user (or not even completed).
	 * 
	 * @return		New instance for Jforum and mneme participant item details
	 */
	ParticipantItemDetails newParticipantItemDetails(String id, String displayId, String groupTitle, String sortName, ParticipantStatus status, Integer count, Date evaluatedDate, Date reviewedDate, Float score, Date startedDate, Date finishedDate, Boolean evaluationNotReviewed, Boolean released, String reviewLink, String gradingLink, Boolean inProgress, String submissionId, Boolean isSubmissionLate, Boolean isAutoSubmission);
	
	/**
	 * Creates new instance for mneme participant item details
	 * 
	 * @param id				User id
	 * 
	 * @param displayId			Display id
	 * 
	 * @param groupTitle		Group title
	 * 
	 * @param sortName			Sort name
	 * 
	 * @param status			Participant status
	 * 
	 * @param count				Jforum posts count
	 * 
	 * @param evaluatedDate		Evaluated date
	 * 
	 * @param reviewedDate		Reviewed date
	 * 
	 * @param score				Score
	 * 
	 * @param startedDate		Started date or of first post time
	 * 
	 * @param finishedDate		Finished date of last post time
	 * 
	 * @param evaluationNotReviewed		True if evaluation not reviewed else false
	 * 
	 * @param released			True if score is released else false
	 * 
	 * @param reviewLink		Review link
	 * 
	 * @param gradingLink		Grading link
	 * 
	 * @param inProgress		True is test/quiz/assignment etc in progress else false
	 * 
	 * @param submissionId		Mneme submission id
	 * 
	 * @param isBestSubmission	Ture if the submission is best
	 * 
	 * @param isSubmissionLate  TRUE if the submission is completed after the due date, FALSE if not.
	 * 
	 * @param isAutoSubmission  TRUE if the submission was automatically completed, FALSE if is was completed by the user (or not even completed).
	 * 
	 * @return	New instance for mneme participant item details
	 */
	ParticipantItemDetails newParticipantItemDetails(String id, String displayId, String groupTitle, String sortName, ParticipantStatus status, Integer count, Date evaluatedDate, Date reviewedDate, Float score, Date startedDate, Date finishedDate, Boolean evaluationNotReviewed, Boolean released, String reviewLink, String gradingLink, Boolean inProgress, String submissionId, Boolean isBestSubmission, Boolean isSubmissionLate, Boolean isAutoSubmission);
	
	/***
	 * Creates new instance of UserGrade for overridden letter grade
	 * 
	 * @param userId		User id
	 * 
	 * @param letterGrade	Letter grade
	 * 
	 * @param prevGradeAssignedDate		Previous overridden grade assgined date
	 * 
	 * @return	New instance of UserGrade for overridden letter grade
	 */
	UserGrade newUserGrade(String userId, String letterGrade, Date prevGradeAssignedDate);
	
	/**
	 * Creates new instance for user item special access
	 * 
	 * @param openDate			Open date
	 * 
	 * @param dueDate			Due date
	 * 
	 * @param acceptUntilDate	Accept until date
	 * 
	 * @param hideUntilOpen		Hide until open
	 * 
	 * @param overrideOpenDate	True if override open date
	 * 
	 * @param overrideDueDate	True if override due date
	 * 
	 * @param overrideAcceptUntilDate	True if override accept until date
	 * 
	 * @param overrideHideUntilOpen		True if override hide until open
	 * 
	 * @return	New instance for user item special access
	 */
	UserItemSpecialAccess newUserItemSpecialAccessImpl(Date openDate, Date dueDate, Date acceptUntilDate, Boolean hideUntilOpen, Boolean overrideOpenDate, Boolean overrideDueDate, Boolean overrideAcceptUntilDate, Boolean overrideHideUntilOpen, Boolean datesValid);
	
	/***
	 * Creates new instance of Notes for instructor notes about student
	 * 
	 * @param userId		Student id
	 * 
	 * @param notes			Instructor notes of student
	 * 
	 * @param prevNoteAddedDate		Previous note modified date if exists or added date
	 * 
	 * @param addedByUserId		Added by user id
	 * 
	 * @return	New instance of Notes for instructor notes of student
	 */
	Notes newUserNote(String userId, String notes, Date prevNoteAddedDate, String addedByUserId);
	
	/**
	 * Register as an item provider.
	 * 
	 * @param provider
	 *        The provider to register.
	 */
	void registerProvider(GradebookItemProvider provider);
	
	/**
	 * Unregister as an item provider.
	 * 
	 * @param provider
	 *        The provider to unregister.
	 */
	void unregisterProvider(GradebookItemProvider provider);
}
