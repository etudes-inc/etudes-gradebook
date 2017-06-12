/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-plugin/plugin/src/java/org/etudes/gradebook/plugin/PluginItemProvider.java $
 * $Id: PluginItemProvider.java 12172 2015-12-01 01:19:22Z murthyt $
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
package org.etudes.gradebook.plugin;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.etudes.gradebook.api.GradebookItem;
import org.etudes.gradebook.api.GradebookItemProvider;
import org.etudes.gradebook.api.GradebookItemType;
import org.etudes.gradebook.api.Participant;
import org.etudes.gradebook.api.ParticipantGradebookItem;
import org.etudes.gradebook.api.ParticipantItemDetails;

abstract public class PluginItemProvider implements GradebookItemProvider
{

	/**
	 * {@inheritDoc}
	 */
	public List<GradebookItem> getGradableItems(String context, String fetchedByUserId, Set<String> activeParticipantIds, boolean includeScores, GradebookItemType itemType)
	{
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public GradebookItem getJForumGradableItem(String context, String fetchedByUserId, String userId, Set<String> activeParticipantIds, boolean includeScores)
	{
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	/*
	public List<ParticipantItemDetails> getJforumItemPostsEvaluations(String context, String itemId, String fetchedByUserId, List<Participant> participants)
	{
		return null;
	}
	*/
	
	/**
	 * {@inheritDoc}
	 */
	public List<ParticipantItemDetails> getJforumItemPostsEvaluations(String context, String itemId, String fetchedByUserId, List<Participant> participants, boolean allEvaluations)
	{
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public GradebookItem getMnemeGradableItem(String context, String itemId, String userId, Set<String> activeParticipantIds, boolean includeScores)
	{
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	/*
	public List<ParticipantItemDetails> getMnemeItemDetails(String context, String itemId, List<Participant> participants)
	{
		return null;
	}
	*/
	
	/**
	 * {@inheritDoc}
	 */
	public List<ParticipantItemDetails> getMnemeItemDetails(String context, String itemId, List<Participant> participants, boolean allSubmissions)
	{
		return null;
	}
	
	/**
	 * {@inheritDoc} 
	 */
	public Map<String, List<ParticipantGradebookItem>> getUsersGradableItems(String context, String fetchedByUserId, List<String> participantIds, GradebookItemType itemType, boolean allScores)
	{
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<ParticipantGradebookItem> getUserToolGradebookItems(String context, String userId, GradebookItemType itemType, boolean allScores)
	{
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Boolean modifyJforumScores(String context, String itemId, List<ParticipantItemDetails> participantItemDetails, String modifiedByUserId, Date scoresFetchedTime)
	{
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Boolean modifyJforumUserScore(String context, String itemId, ParticipantItemDetails participantItemDetails, String modifiedByUserId, Date scoreFetchedTime)
	{
		// do nothing here and update score in jforum provider
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Boolean modifyMnemeScores(String context, String itemId, List<ParticipantItemDetails> participantItemDetails, String modifiedByUserId, Date scoresFetchedTime)
	{
		// do nothing here and update scores in mneme provider
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Boolean modifyMnemeUserScores(String context, String userId, List<ParticipantItemDetails> participantItemDetails, String modifiedByUserId, Date scoresFetchedTime)
	{
		// do nothing here and update score in mneme provider
		return null;
	}
}
