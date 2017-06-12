/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-api/api/src/java/org/etudes/gradebook/api/GradebookItemProvider.java $
 * $Id: GradebookItemProvider.java 12172 2015-12-01 01:19:22Z murthyt $
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
import java.util.Map;
import java.util.Set;

public interface GradebookItemProvider
{
	
	/**
	 * Get gradable items
	 * 
	 * @param context	Context
	 * 
	 * @param fetchedByUserId	Fetched by user id
	 * 
	 * @param activeParticipantIds
	 *        The user ids for active users.
	 * 
	 * @param includeScores	true - scores are included
	 * 						false - score not included
	 * 
	 * @param itemType 	Filter by item type
	 * 
	 * @return	The gradable items
	 */
	List<GradebookItem> getGradableItems(String context, String fetchedByUserId, Set<String> activeParticipantIds, boolean includeScores, boolean includeUnpublish, GradebookItemType itemType);
	
	/**
	 * Get jforum gradable item
	 * 
	 * @param context	Context
	 * 
	 * @param itemId	Item id
	 * 
	 * @param fetchedByUserId	Fetched by user id
	 * 
	 * @param activeParticipantIds	The user ids for active users.
	 * 
	 * @param includeScores		true - scores are included
	 * 						false - score not included
	 * 
	 * @return	The gradable item
	 */
	GradebookItem getJForumGradableItem(String context, String itemId, String fetchedByUserId, Set<String> activeParticipantIds, boolean includeScores);
	
	/**
	 * Gets users jforum item details 
	 * 
	 * @param context		Context
	 * 
	 * @param itemId		Item id
	 * 
	 * @param fetchedByUserId	Fetched by user id
	 * 
	 * @param participants	Participants
	 * 
	 * @return	Users jforum post evaluations
	 */
	// List<ParticipantItemDetails> getJforumItemPostsEvaluations(String context, String itemId, String fetchedByUserId, List<Participant> participants);
	
	
	/**
	 * Gets users jforum item details
	 * 
	 * @param context		Context
	 * 
	 * @param itemId		Item id
	 * 
	 * @param fetchedByUserId	Fetched by user id
	 * 
	 * @param participants	Participants
	 * 
	 * @param allEvaluations	If true all evaluations including not released
	 * 
	 * @return	Users jforum post evaluations
	 */
	List<ParticipantItemDetails> getJforumItemPostsEvaluations(String context, String itemId, String fetchedByUserId, List<Participant> participants, boolean allEvaluations);
	
	/**
	 * Get mneme gradable item
	 * 
	 * @param context	Context
	 * 
	 * @param itemId	Item id
	 * 
	 * @param userId	User id
	 * 
	 * @param activeParticipantIds	The user ids for active users.
	 * 
	 * @param includeScores		true - scores are included
	 * 						false - score not included
	 * 
	 * @return	The gradable item
	 */
	GradebookItem getMnemeGradableItem(String context, String itemId, String userId, Set<String> activeParticipantIds, boolean includeScores);
	
	/**
	 * Gets users mneme item details
	 * 
	 * @param context	Context
	 * 
	 * @param itemId	ItemId
	 * 
	 * @param participants	Participants
	 * 
	 * @return	Users mneme item details
	 */
	// List<ParticipantItemDetails> getMnemeItemDetails(String context, String itemId, List<Participant> participants);
	
	/**
	 * Gets users mneme item details
	 * 
	 * @param context	Context
	 * 
	 * @param itemId	Itemid
	 * 
	 * @param participants	Participants
	 * 
	 * @param allSubmissions If true all submissions else only released
	 * 
	 * @return	Users mneme item details
	 */
	List<ParticipantItemDetails> getMnemeItemDetails(String context, String itemId, List<Participant> participants, boolean allSubmissions);
	
	/**
	 * Gets the users gradable items
	 * 
	 * @param context	Context
	 * 
	 * @param fetchedByUserId	Fetched by user id
	 * 
	 * @param participantIds	Participants
	 * 
	 * @param itemType	Item type. If null gets all the items else only items of the item type
	 * 
	 * @param allScores	true - All scores
	 * 					false - Released scores
	 * 
	 * @return	The users gradable items with participant id as key and participant gradebook items as list
	 */
	Map<String, List<ParticipantGradebookItem>> getUsersGradableItems(String context, String fetchedByUserId, List<String> participantIds, GradebookItemType itemType, boolean allScores);
	
	/**
	 * Gets the user gradebook items
	 * 
	 * @param context	Context
	 * 
	 * @param userId	User id
	 * 
	 * @param itemType Item type. If null gets all the items else only items of the item type
	 * 
	 * @param allScores	true - All scores
	 * 					false - Released scores
	 * 
	 * @return	The user gradebook items
	 */
	List<ParticipantGradebookItem> getUserToolGradebookItems(String context, String userId, GradebookItemType itemType, boolean allScores);
	
	/**
	 * Modifies jforum scores and released status. If scores fetched time is not null the scores are checked with last updated time to avoid saving the stale data
	 * 
	 * @param context	Context
	 * 
	 * @param itemId	Item id
	 * 
	 * @param participantItemDetails	List of participant item details with updated score and released status
	 * 
	 * @param modifiedByUserId	Modified by user id
	 * 
	 * @param scoresFetchedTime Scores fetched time
	 * 
	 * @return	true if modified else false if user have no access to grade
	 */
	Boolean modifyJforumScores(String context, String itemId, List<ParticipantItemDetails> participantItemDetails, String modifiedByUserId, Date scoresFetchedTime);
	
	/**
	 * Modifies user jforum item score and released status. If score fetched time is not null the scores are checked with last updated time to avoid saving the stale data
	 * 
	 * @param context	Context
	 * 
	 * @param itemId	Item id
	 * 
	 * @param participantItemDetails	User participant item details with updated score and released status
	 * 
	 * @param modifiedByUserId	Modified by user id
	 * 
	 * @param scoreFetchedTime	Score fetched time if scores fetched time is not null the score is checked with last updated time to avoid saving the stale data
	 * 
	 * @return	true if modified else false if user have no access to grade
	 */
	Boolean modifyJforumUserScore(String context, String itemId, ParticipantItemDetails participantItemDetails, String modifiedByUserId, Date scoreFetchedTime);

	/**
	 * Modifies mneme user submission's scores. If scores fetched time is not null the scores are checked with last updated time to avoid saving the stale data
	 * 
	 * @param context	Context
	 * 
	 * @param itemId	Item id
	 * 
	 * @param participantItemDetails	List of participant item details with updated score and released status
	 * 
	 * @param modifiedByUserId	Modified by user id
	 * 
	 * @param scoresFetchedTime Scores fetched time if scores fetched time is not null the score is checked with last updated time to avoid saving the stale data
	 * 
	 * @return	true if modified else false if user have no access to grade
	 */
	Boolean modifyMnemeScores(String context, String itemId, List<ParticipantItemDetails> participantItemDetails, String modifiedByUserId, Date scoresFetchedTime);

	/**
	 * Modifies user mneme item score and released status
	 * 
	 * @param context	Context
	 * 
	 * @param userId	User id
	 * 
	 * @param participantItemDetails	User participant item details with updated score and released status
	 * 
	 * @param modifiedByUserId	Modified by user id
	 * 
	 * @param scoresFetchedTime Scores fetched time if scores fetched time is not null the score is checked with last updated time to avoid saving the stale data
	 * 
	 * @return	true if modified else false if user have no access to grade
	 */
	Boolean modifyMnemeUserScores(String context, String userId, List<ParticipantItemDetails> participantItemDetails, String modifiedByUserId, Date scoresFetchedTime);
}
