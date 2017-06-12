/**********************************************************************************
 * $Id: GradebookServiceImpl.java 12631 2016-02-17 22:37:16Z murthyt $
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.coursemap.api.CourseMapItem;
import org.etudes.coursemap.api.CourseMapMap;
import org.etudes.coursemap.api.CourseMapService;
import org.etudes.gradebook.api.Gradebook;
import org.etudes.gradebook.api.Gradebook.BoostUserGradesType;
import org.etudes.gradebook.api.Gradebook.CategoryType;
import org.etudes.gradebook.api.Gradebook.GradebookSortType;
import org.etudes.gradebook.api.Gradebook.ReleaseGrades;
import org.etudes.gradebook.api.Gradebook.StandardCategory;
import org.etudes.gradebook.api.GradebookCategory;
import org.etudes.gradebook.api.GradebookCategory.WeightDistribution;
import org.etudes.gradebook.api.GradebookCategoryItemMap;
import org.etudes.gradebook.api.GradebookCategoryItemMapComparator;
import org.etudes.gradebook.api.GradebookComparator;
import org.etudes.gradebook.api.GradebookItem;
import org.etudes.gradebook.api.GradebookItemAccessStatus;
import org.etudes.gradebook.api.GradebookItemProvider;
import org.etudes.gradebook.api.GradebookItemType;
import org.etudes.gradebook.api.GradebookService;
import org.etudes.gradebook.api.GradingScale;
import org.etudes.gradebook.api.GradingScalePercent;
import org.etudes.gradebook.api.Notes;
import org.etudes.gradebook.api.Participant;
import org.etudes.gradebook.api.ParticipantGradebookComparator;
import org.etudes.gradebook.api.ParticipantGradebookItem;
import org.etudes.gradebook.api.ParticipantItemDetails;
import org.etudes.gradebook.api.ParticipantJforumItemDetailsSort;
import org.etudes.gradebook.api.ParticipantMnemeItemDetailsSort;
import org.etudes.gradebook.api.ParticipantSort;
import org.etudes.gradebook.api.ParticipantStatus;
import org.etudes.gradebook.api.UserCategoryGrade;
import org.etudes.gradebook.api.UserGrade;
import org.etudes.gradebook.api.UserItemSpecialAccess;
import org.etudes.gradebook.impl.UserCategoryGradeImpl.PointsScore;
import org.sakaiproject.authz.api.Member;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

public class GradebookServiceImpl implements GradebookService
{
	/** logger **/
	private static Log logger = LogFactory.getLog(GradebookServiceImpl.class);
	
	protected static final String CATEGORY = "CAT";
	
	protected static final String FORUM = "FORUM";
	
	protected static final String JFORUM_TOOL_ID = "sakai.jforum.tool";
	
	protected static final String TOPIC = "TOPIC";
	
	/** Dependency: CourseMapService. */
	protected CourseMapService courseMapService = null;
	
	/** Our registered providers. */
	protected Set<GradebookItemProvider> providers = new HashSet<GradebookItemProvider>();
	
	/** Dependency: SecurityService */
	protected SecurityService securityService = null;
	
	/** Dependency: SiteService. */
	protected SiteService siteService = null;
	
	/** Dependency: SqlService */
	protected SqlService sqlService = null;
	
	/** Storage handler. */
	protected GradingStorage storage = null;
	
	/** Storage option map key for the option to use. */
	protected String storageKey = null;
	
	/** Map of registered PoolStorage options. */
	protected Map<String, GradingStorage> storgeOptions;
	
	/** Dependency: ThreadLocalManager. */
	protected ThreadLocalManager threadLocalManager = null;
	
	/** Dependency: UserDirectoryService. */
	protected UserDirectoryService userDirectoryService = null;
	
	/**
	 * {@inheritDoc}
	 */
	public void addModifyDeleteContextGradebookCategories(String context, Gradebook.CategoryType categoryType, List<GradebookCategory> gradebookCategories, String modifiedByUserId)
	{

		if ((context == null || context.trim().length() == 0) || (categoryType == null) || (gradebookCategories == null) || (modifiedByUserId == null || modifiedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		// access check
		Boolean userAccess = allowEditGradebook(context, modifiedByUserId);
		
		if (!userAccess)
		{
			return;
		}
		
		Set<String> StandardGradebookTitles = new HashSet<String>();
		Set<String> CustomGradebookTitles = new HashSet<String>();
		// don't allow duplicates. If there is an existing category with same title append "(Copy 1)", "(Copy 2)", etc.

		// Gradebook gradebook = getContextGradebook(context, modifiedByUserId);
		// String defaultCategoryType = gradebook.getCategoryType().name();
		
		// existing category titles
		for (GradebookCategory gradebookCategory : gradebookCategories)
		{			
			String title = gradebookCategory.getTitle();
			if (title != null) title = title.trim();
			if (title == null || title.length() == 0) title = "untitled";
			
			boolean duplicateFound = false;
			String categoryName = (gradebookCategory.getCategoryType() != null) ? gradebookCategory.getCategoryType().name() : categoryType.name();
	
			if (categoryName.equals("Custom"))
				duplicateFound = CustomGradebookTitles.contains(title);
			else if (categoryName.equals("Standard"))
				duplicateFound = StandardGradebookTitles.contains(title);
	
			if (duplicateFound)
			{
				int i = 1;
				while (true)
				{
				    String newtitle = title +" (Copy "+ i++ +")";
				    
				    if (categoryName.equals("Custom"))
				    {
						if (!CustomGradebookTitles.contains(newtitle.trim()))
						{
							((GradebookCategoryImpl)gradebookCategory).setTitle(newtitle);
							CustomGradebookTitles.add(newtitle.trim());
							break;
						}
				    }		
				    else if (categoryName.equals("Standard"))
				    {
						if (!StandardGradebookTitles.contains(newtitle.trim()))
						{
							((GradebookCategoryImpl)gradebookCategory).setTitle(newtitle);
							StandardGradebookTitles.add(newtitle.trim());
							break;
						}
				    }					
				} //while end	
			} // if duplicate found end		
			else
			{
				  if (categoryName.equals("Custom")) CustomGradebookTitles.add(title);
				  else if (categoryName.equals("Standard")) StandardGradebookTitles.add(title);
			}
		}
			
		// TODO check and fix the order
		
		/* if all gradebookCategories are needs to be removed gradebookCategories size should be zero, modified category will be updated, 
		 * if existing category is to be deleted remove from the gradebookCategories list, 
		 * if id is -1(newGradebookCategory(String title, Float weight, WeightDistribution weightDistribution, int order)) or zero new category will be created */
		this.storage.addModifyDeleteGradebookCategories(context, categoryType, gradebookCategories, modifiedByUserId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void addModifyDeleteContextGradebookCategories(String context, List<GradebookCategory> gradebookCategories, String modifiedByUserId)
	{
		if ((context == null || context.trim().length() == 0) || (gradebookCategories == null) || (modifiedByUserId == null || modifiedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		Gradebook gradebook = getContextGradebook(context, modifiedByUserId);
		addModifyDeleteContextGradebookCategories(context, gradebook.getCategoryType(), gradebookCategories, modifiedByUserId);
		
		/*
		// access check
		Boolean userAccess = allowEditGradebook(context, modifiedByUserId);
		
		if (!userAccess)
		{
			return;
		}
		
		Set<String> StandardGradebookTitles = new HashSet<String>();
		Set<String> CustomGradebookTitles = new HashSet<String>();
		// don't allow duplicates. If there is an existing category with same title append "(Copy 1)", "(Copy 2)", etc.

		Gradebook gradebook = getContextGradebook(context, modifiedByUserId);
		String defaultCategoryType = gradebook.getCategoryType().name();
		
		// existing category titles
		for (GradebookCategory gradebookCategory : gradebookCategories)
		{			
			String title = gradebookCategory.getTitle();
			if (title != null) title = title.trim();
			if (title == null || title.length() == 0) title = "untitled";
			
			boolean duplicateFound = false;
			String categoryName = (gradebookCategory.getCategoryType() != null) ? gradebookCategory.getCategoryType().name() : defaultCategoryType;
	
			if (categoryName.equals("Custom"))
				duplicateFound = CustomGradebookTitles.contains(title);
			else if (categoryName.equals("Standard"))
				duplicateFound = StandardGradebookTitles.contains(title);
	
			if (duplicateFound)
			{
				int i = 1;
				while (true)
				{
				    String newtitle = title +" (Copy "+ i++ +")";
				    
				    if (categoryName.equals("Custom"))
				    {
						if (!CustomGradebookTitles.contains(newtitle.trim()))
						{
							((GradebookCategoryImpl)gradebookCategory).setTitle(newtitle);
							CustomGradebookTitles.add(newtitle.trim());
							break;
						}
				    }		
				    else if (categoryName.equals("Standard"))
				    {
						if (!StandardGradebookTitles.contains(newtitle.trim()))
						{
							((GradebookCategoryImpl)gradebookCategory).setTitle(newtitle);
							StandardGradebookTitles.add(newtitle.trim());
							break;
						}
				    }					
				} //while end	
			} // if duplicate found end		
			else
			{
				  if (categoryName.equals("Custom")) CustomGradebookTitles.add(title);
				  else if (categoryName.equals("Standard")) StandardGradebookTitles.add(title);
			}
		}
			
		// TODO check and fix the order
		
		// if all gradebookCategories are needs to be removed gradebookCategories size should be zero, modified category will be updated, 
		// if existing category is to be deleted remove from the gradebookCategories list, 
		// if id is -1(newGradebookCategory(String title, Float weight, WeightDistribution weightDistribution, int order)) or zero new category will be created 
		this.storage.addModifyDeleteGradebookCategories(context, gradebook.getCategoryType(), gradebookCategories, modifiedByUserId);
		*/
	}

	/**
	 * {@inheritDoc}
	 */
	public void addModifyInstructorUserNotes(String context, Notes instructorUserNotes)
	{
		if ((context == null || context.trim().length() == 0) || (instructorUserNotes == null) || (instructorUserNotes.getUserId() == null || instructorUserNotes.getUserId().trim().length() == 0)
				|| (instructorUserNotes.getAddedByUserId() == null || instructorUserNotes.getAddedByUserId().trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing");
		}
		
		// access check
		Boolean userAccess = allowEditGradebook(context, instructorUserNotes.getAddedByUserId());
		
		if (!userAccess)
		{
			return;
		}
		
		Gradebook gradebook = getContextGradebook(context, instructorUserNotes.getAddedByUserId());
		
		this.storage.insertUpdateInstructorUserNotes(gradebook.getId(), instructorUserNotes);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Boolean allowEditGradebook(String context, String userId)
	{
		if (context == null) throw new IllegalArgumentException();
		if (userId == null) return Boolean.FALSE;

		if (logger.isDebugEnabled()) logger.debug("allowEditGradebook: " + context + ": " + userId);
		
		String key = "etudesgradebook:allowEditGradebook:"+ context +":"+ userId;
		
		Boolean ok = (Boolean) this.threadLocalManager.get(key);
		if (ok != null)
		{
			return ok;
		}

		// check permission - user must have "site.upd" in the context
		ok = checkSecurity(userId, "site.upd", context);
		
		this.threadLocalManager.set(key, ok);

		return ok;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Boolean allowGetGradebook(String context, String userId)
	{
		if (context == null) throw new IllegalArgumentException();
		if (userId == null) return Boolean.FALSE;

		if (logger.isDebugEnabled()) logger.debug("allowGetGradebook: " + context + ": " + userId);

		String key = "etudesgradebook:allowGetGradebook:"+ context +":"+ userId;
		
		Boolean ok = (Boolean) this.threadLocalManager.get(key);
		if (ok != null)
		{
			return ok;
		}
		
		// check permission - user must have "site.visit" in the context
		ok = checkSecurity(userId, "site.visit", context);
		
		this.threadLocalManager.set(key, ok);

		// and cannot be an evaluator - use Mneme's permission
		/*if (ok)
		{
			ok = !checkSecurity(userId, "mneme.course.eval", context);
		}*/

		return ok;
	}
	
	/**
	 * Returns to uninitialized state.
	 */
	public void destroy()
	{
		if (logger.isInfoEnabled())
		{
			logger.info("destroy()");
		}
	}
		
	/**
	 * {@inheritDoc}
	 */
	public Map<String, Float>getClassPointsAverage(String context, String userId)
	{
		if (context == null || context.trim().length() == 0 || userId == null || userId.trim().length() == 0)
		{
			return new HashMap<String, Float>();
		}
		
		Float nonExtraCreditCategoriesTotalPoints = null;
		Float nonExtraCreditWeightedCategoriesTotalPoints = null;
		boolean nonExtraCreditWeighted = false;
		Float extraCreditCategoryTotalPoints = null;
		Float extraCreditWeightedCategoryTotalPoints = null;
		boolean extraCreditWeighted = false;
		GradebookCategory gradebookCategory = null;
		List<GradebookItem> gradebookItems = null;
		
		Map<GradebookCategory, List<GradebookItem>> toolGradebookItems = getToolGradebookItems(context, userId, false, false);
		for (Map.Entry<GradebookCategory, List<GradebookItem>> entry : toolGradebookItems.entrySet()) 
		{
			gradebookCategory = entry.getKey();
			gradebookItems = entry.getValue();
			
			if (gradebookCategory.isExtraCredit())
			{
				boolean weighted = false;
				if (gradebookCategory.getWeight() != null && gradebookCategory.getWeight() > 0)
				{
					weighted = true;
					extraCreditWeighted = true;
				}
				
				if (gradebookItems != null && gradebookItems.size() > 0)
				{
					for (GradebookItem gradebookItem : gradebookItems)
					{
						if (gradebookItem.getPoints() != null)
						{
							if (extraCreditCategoryTotalPoints == null)
							{
								extraCreditCategoryTotalPoints = 0.0f;
							}
							
							extraCreditCategoryTotalPoints += gradebookItem.getPoints();
							
							if (weighted)
							{
								if (extraCreditWeightedCategoryTotalPoints == null)
								{
									extraCreditWeightedCategoryTotalPoints = 0.0f;
								}
								
								extraCreditWeightedCategoryTotalPoints += gradebookItem.getPoints();
							}
						}
					}
				}
			}
			else
			{
				boolean weighted = false;
				if (gradebookCategory.getWeight() != null && gradebookCategory.getWeight() > 0)
				{
					weighted = true;
					nonExtraCreditWeighted = true;
				}
				
				if (gradebookItems != null && gradebookItems.size() > 0)
				{
					for (GradebookItem gradebookItem : gradebookItems)
					{
						if (gradebookItem.getPoints() != null)
						{
							if (nonExtraCreditCategoriesTotalPoints == null)
							{
								nonExtraCreditCategoriesTotalPoints = 0.0f;
							}
							
							nonExtraCreditCategoriesTotalPoints += gradebookItem.getPoints();
							
							if (weighted)
							{
								if (nonExtraCreditWeightedCategoriesTotalPoints == null)
								{
									nonExtraCreditWeightedCategoriesTotalPoints = 0.0f;
								}
								
								nonExtraCreditWeightedCategoriesTotalPoints += gradebookItem.getPoints();
							}
						}
					}
				}
			}
		}
		
		Float classAveragePercent= (Float)this.threadLocalManager.get(classAveragePercentCacheKey(context, userId));
		
		Map<String, Float> pointsAverageMap = new HashMap<String, Float>();

		if (nonExtraCreditWeighted)
		{
			pointsAverageMap.put("nonExtraCreditCategoriesTotalPoints", nonExtraCreditWeightedCategoriesTotalPoints);
		}
		else 
		{
			pointsAverageMap.put("nonExtraCreditCategoriesTotalPoints", nonExtraCreditCategoriesTotalPoints);
		}
		
		if (extraCreditWeighted)
		{
			pointsAverageMap.put("extraCreditCategoryTotalPoints", extraCreditWeightedCategoryTotalPoints);
		}
		else
		{
			pointsAverageMap.put("extraCreditCategoryTotalPoints", extraCreditCategoryTotalPoints);
		}
		
		pointsAverageMap.put("classAveragePercent", classAveragePercent);
		
		return pointsAverageMap;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Gradebook getContextGradebook(String context, String fetchedByUserId)
	{
		if ((context == null || context.trim().length() == 0) || (fetchedByUserId == null || fetchedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		// user access check
		Boolean userAccess = allowEditGradebook(context, fetchedByUserId);
		
		if (!userAccess)
		{
			userAccess = allowGetGradebook(context, fetchedByUserId);
			
			if (!userAccess)
			{
				// check current user access
				User currentUser = this.userDirectoryService.getCurrentUser();
				
				userAccess = allowEditGradebook(context, currentUser.getId());
				
				if (!userAccess)
				{
					userAccess = allowGetGradebook(context, fetchedByUserId);
					
					if (!userAccess)
					{
						return null;
					}
					else
					{
						fetchedByUserId = currentUser.getId();
					}
				}
				else
				{
					fetchedByUserId = currentUser.getId();
				}
			}
		}
		
		// get from threadlocal
		String key = "etudesgradebook:getContextGradebook:"+ context +":"+ fetchedByUserId;
		
		Gradebook gb = (Gradebook) this.threadLocalManager.get(key);
		if (gb != null)
		{
			// make a copy and return
			return new GradebookImpl((GradebookImpl)gb);
		}
		
		// check and add gradebook for context if not existing and fetches the context gradebook
		Gradebook gradebook = this.storage.selectGradebook(context, fetchedByUserId);
		
		if (gradebook != null)
		{
			this.threadLocalManager.set(key, gradebook);
		}
		
		return gradebook;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public GradebookItem getGradebookItem(String itemId, String context, String groupId, String fetchedByUserId)
	{
		if ((itemId == null || itemId.trim().length() == 0) || (context == null || context.trim().length() == 0) || (fetchedByUserId == null || fetchedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		Boolean userAccess = allowEditGradebook(context, fetchedByUserId);
		
		if (!userAccess)
		{
			return null;
		}
		
		GradebookItem gradebookItem = null;
		
		if (itemId.startsWith(CATEGORY + "-") || itemId.startsWith(FORUM + "-") || itemId.startsWith(TOPIC + "-"))
		{
			gradebookItem = getJForumGradebookItem(context, itemId, groupId, fetchedByUserId);
		}
		else if (itemId.startsWith("MNEME-"))
		{
			gradebookItem = getMnemeGradebookItem(context, itemId, groupId, fetchedByUserId);
		}
		
		return gradebookItem;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public GradebookCategory getGradebookItemCategory(String itemId, String context, String fetchedByUserId)
	{
		if ((itemId == null || itemId.trim().length() == 0) || (context == null || context.trim().length() == 0) || (fetchedByUserId == null || fetchedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		Boolean userAccess = allowEditGradebook(context, fetchedByUserId);
		
		if (!userAccess)
		{
			return null;
		}
		
		return this.storage.selectItemGradebookCategory(itemId, context);
	}

	/**
	 * {@inheritDoc}
	 */
	/*
	public List<ParticipantItemDetails> getJforumItemDetails(String context, String itemId, String groupId, ParticipantJforumItemDetailsSort sort, String fetchedByUserId)
	{
		if ((itemId == null || itemId.trim().length() == 0) || (context == null || context.trim().length() == 0) || (fetchedByUserId == null || fetchedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		// user access check
		Boolean userAccess = allowEditGradebook(context, fetchedByUserId);
		
		if (!userAccess)
		{
			return null;
		}
		
		if (itemId.startsWith(CATEGORY + "-") || itemId.startsWith(FORUM + "-") || itemId.startsWith(TOPIC + "-"))
		{
			ArrayList<ParticipantItemDetails> rv = new ArrayList<ParticipantItemDetails>();
	
			// get the users
			List<Participant> participants = null;
			
			if (groupId == null || groupId.trim().length() == 0)
			{
				participants = getParticipants(context, false);
			}
			else
			{
				participants = getParticipants(context, groupId, false);
			}
	
			// rv.addAll(this.jforumConnector.getNumJforumItemPostsEvaluations(context, itemId, participants));
			for (GradebookItemProvider provider : this.providers)
			{
				List<ParticipantItemDetails> JforumItemDetail = provider.getJforumItemPostsEvaluations(context, itemId, fetchedByUserId, participants);
				
				if (JforumItemDetail != null)
				{
					rv.addAll(JforumItemDetail);
					break;
				}
			}
			
			// sort
			Collections.sort(rv, sort);

			return rv;
		}
		else
		{
			return new ArrayList<ParticipantItemDetails>();
		}
		
	}
	*/
		
	/**
	 * 
	 * @param context
	 * @param userId
	 * @param fetchScores
	 * @param fetchUnpublish
	 * @param sortType
	 * @param sortAscending
	 * @param itemType
	 * @return
	 */
	public List<GradebookItem> getImportGradebookItems(String context, String userId, boolean fetchScores, boolean fetchUnpublish, GradebookSortType sortType, Boolean sortAscending, GradebookItemType itemType)
	{
		if ((context == null || context.trim().length() == 0) || (userId == null || userId.trim().length() == 0))
		{
			new ArrayList<GradebookItem>();
		}
		
		// access check
		Boolean userAccess = allowEditGradebook(context, userId);
		
		if (!userAccess)
		{
			return new ArrayList<GradebookItem>();
		}
		
		Set<String> actives = new HashSet<String>();
		List<GradebookItem> gradebookItems = new ArrayList<GradebookItem>();
		
		// get the items from the providers				
		for (GradebookItemProvider provider : this.providers)
		{
			// get the items from this provider
			List<GradebookItem> gradableItems = provider.getGradableItems(context, userId, actives, fetchScores, fetchUnpublish, itemType);
			
			if (gradableItems != null)
			{
				gradebookItems.addAll(gradableItems);
			}
		}
		return gradebookItems;
	}
	
	/**
	 * {@inheritDoc}
	 */
	/*
	public List<ParticipantItemDetails> getMnemeItemDetails(String context, String itemId, String groupId, ParticipantMnemeItemDetailsSort sort, String fetchedByUserId)
	{
		if ((itemId == null || itemId.trim().length() == 0) || (context == null || context.trim().length() == 0) || (fetchedByUserId == null || fetchedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		// user access check
		Boolean userAccess = allowEditGradebook(context, fetchedByUserId);
		
		if (!userAccess)
		{
			return null;
		}
		
		if (itemId.startsWith("MNEME-"))
		{
			ArrayList<ParticipantItemDetails> rv = new ArrayList<ParticipantItemDetails>();
			
			// get the users
			List<Participant> participants = null;
			
			if (groupId == null || groupId.trim().length() == 0)
			{
				participants = getParticipants(context, false);
			}
			else
			{
				participants = getParticipants(context, groupId, false);
			}
	
			for (GradebookItemProvider provider : this.providers)
			{
				List<ParticipantItemDetails> mnemeItemDetail = provider.getMnemeItemDetails(context, itemId, participants);
				
				if (mnemeItemDetail != null)
				{
					rv.addAll(mnemeItemDetail);
					break;
				}
			}
			
			// sort
			Collections.sort(rv, sort);

			return rv;
			
		}
		else
		{
			return new ArrayList<ParticipantItemDetails>();
		}
	}
	*/
	
	/**
	 * {@inheritDoc}
	 */
	public List<ParticipantItemDetails> getJforumItemDetails(String context, String itemId, String groupId, ParticipantJforumItemDetailsSort sort, String fetchedByUserId, boolean allEvaluations)
	{
		if ((itemId == null || itemId.trim().length() == 0) || (context == null || context.trim().length() == 0) || (fetchedByUserId == null || fetchedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		// user access check
		Boolean userAccess = allowEditGradebook(context, fetchedByUserId);
		
		if (!userAccess)
		{
			return null;
		}
		
		if (itemId.startsWith(CATEGORY + "-") || itemId.startsWith(FORUM + "-") || itemId.startsWith(TOPIC + "-"))
		{
			ArrayList<ParticipantItemDetails> rv = new ArrayList<ParticipantItemDetails>();
	
			// get the users
			List<Participant> participants = null;
			
			if (groupId == null || groupId.trim().length() == 0)
			{
				participants = getParticipants(context, false);
			}
			else
			{
				participants = getParticipants(context, groupId, false);
			}
	
			// rv.addAll(this.jforumConnector.getNumJforumItemPostsEvaluations(context, itemId, participants));
			for (GradebookItemProvider provider : this.providers)
			{
				List<ParticipantItemDetails> JforumItemDetail = provider.getJforumItemPostsEvaluations(context, itemId, fetchedByUserId, participants, allEvaluations);
				
				if (JforumItemDetail != null)
				{
					rv.addAll(JforumItemDetail);
					break;
				}
			}
			
			// don't add zero while fetching all evaluations
			if (!allEvaluations)
			{
				/* add zero to score if item is closed(consider user special access) */
				GradebookItem gradebookItem = getJForumGradebookItem(context, itemId, groupId, fetchedByUserId);
				
				checkAndAssignZeroScoreNonSubmitters(rv, gradebookItem);
			}
			
			// instructor notes about students
			getInstructorUserNotes(context, fetchedByUserId, rv);
			
			// sort
			Collections.sort(rv, sort);

			return rv;
		}
		else
		{
			return new ArrayList<ParticipantItemDetails>();
		}
		
	}

	/**
	 * {@inheritDoc}
	 */
	public List<ParticipantItemDetails> getMnemeItemDetails(String context, String itemId, String groupId, ParticipantMnemeItemDetailsSort sort, String fetchedByUserId, boolean allSubmissions)
	{
		if ((itemId == null || itemId.trim().length() == 0) || (context == null || context.trim().length() == 0) || (fetchedByUserId == null || fetchedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		/* get all submission of the users */
		// user access check
		Boolean userAccess = allowEditGradebook(context, fetchedByUserId);
		
		if (!userAccess)
		{
			return null;
		}
		
		if (itemId.startsWith("MNEME-"))
		{
			ArrayList<ParticipantItemDetails> rv = new ArrayList<ParticipantItemDetails>();
			
			// get the users
			List<Participant> participants = null;
			
			if (groupId == null || groupId.trim().length() == 0)
			{
				participants = getParticipants(context, false);
			}
			else
			{
				participants = getParticipants(context, groupId, false);
			}
	
			for (GradebookItemProvider provider : this.providers)
			{
				List<ParticipantItemDetails> mnemeItemDetail = provider.getMnemeItemDetails(context, itemId, participants, allSubmissions);
				
				if (mnemeItemDetail != null)
				{
					rv.addAll(mnemeItemDetail);
					break;
				}
			}
			
			/* add zero to score if item is closed(consider user special access) */
			if (!allSubmissions)
			{
				/* add zero to score if item is closed(consider user special access) */
				GradebookItem gradebookItem = getMnemeGradebookItem(context, itemId, groupId, fetchedByUserId);
				
				checkAndAssignZeroScoreNonSubmitters(rv, gradebookItem);
			}
			
			// instructor notes about students
			getInstructorUserNotes(context, fetchedByUserId, rv);
			
			// sort
			Collections.sort(rv, sort);

			return rv;
			
		}
		else
		{
			return new ArrayList<ParticipantItemDetails>();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Participant getParticipant(String context, String userId)
	{
		return fetchParticipant(context, userId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Participant getParticipant(String context, String userId, boolean fetchInstrucorNotes)
	{
		Participant participant = fetchParticipant(context, userId);
		
		if (participant != null && fetchInstrucorNotes)
		{
			((ParticipantImpl)participant).setInstructorNotes(getInstructorUserNotes(context, userId));
		}
		
		return participant;
	}

	/**
	 * {@inheritDoc}
	 */
	public HashMap<String, String> getSections(String context)
	{
		if (context == null || context.trim().length() == 0)
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		HashMap<String, String> siteGroups = new HashMap<String, String>();
		Site site;
		try
		{
			site = this.siteService.getSite(context); 
			
			Collection groups = site.getGroups();
			for (Object groupO : groups)
			{
				Group g = (Group) groupO;
				
				siteGroups.put(g.getId(), g.getTitle());
			}
		}
		catch (IdUnusedException e)
		{
		}
		
		return siteGroups;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Map<GradebookCategory, List<GradebookItem>> getToolGradebookItems(String context, String userId, boolean fetchScores, boolean fetchUnpublish)
	{
		if ((context == null || context.trim().length() == 0) || (userId == null || userId.trim().length() == 0))
		{
			return new HashMap<GradebookCategory, List<GradebookItem>>();
		}
		
		// access check
		Boolean userAccess = allowEditGradebook(context, userId);
		
		if (!userAccess)
		{
			return new HashMap<GradebookCategory, List<GradebookItem>>();
		}
		
		Map<GradebookCategory, List<GradebookItem>> toolGradebookItemsMap = new LinkedHashMap<GradebookCategory, List<GradebookItem>>();
		
		List<GradebookItem> gradebookItems = getToolGradebookItems(context, userId, fetchScores, fetchUnpublish, Gradebook.GradebookSortType.Category, false, null);
		
		Gradebook gradebook = getContextGradebook(context, userId);
		
		if (gradebook.getGradebookCategories() != null && gradebook.getGradebookCategories().size() > 0)
		{
			for (GradebookCategory gradebookCategory : gradebook.getGradebookCategories())
			{
				toolGradebookItemsMap.put(gradebookCategory, new ArrayList<GradebookItem>());
			}
		}
		
		//List<GradebookItem> catGradebookItemList = null;
		if (gradebookItems != null && gradebookItems.size() > 0)
		{
			for (GradebookItem gradebookItem : gradebookItems)
			{
				if (toolGradebookItemsMap.containsKey(gradebookItem.getGradebookCategory()))
				{
					toolGradebookItemsMap.get(gradebookItem.getGradebookCategory()).add(gradebookItem);
				}
			}			
		}
		
		return toolGradebookItemsMap;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<GradebookItem> getToolGradebookItems(String context, String userId, boolean fetchScores, boolean fetchUnpublish, GradebookSortType sortType, Boolean sortAscending, GradebookItemType itemType)
	{
		if ((context == null || context.trim().length() == 0) || (userId == null || userId.trim().length() == 0))
		{
			new ArrayList<GradebookItem>();
		}
		
		// access check
		Boolean userAccess = allowEditGradebook(context, userId);
		
		if (!userAccess)
		{
			return new ArrayList<GradebookItem>();
		}
		
		List<GradebookItem> gradebookItems = new ArrayList<GradebookItem>();
		
		gradebookItems.addAll(fetchGradebookToolItems(context, userId, fetchScores, fetchUnpublish, itemType));
		
		if (sortType != null && sortAscending != null)
		{
			if (sortType == Gradebook.GradebookSortType.Category)
			{
				checkAndSetDisplayOrder(context, userId, gradebookItems);
			}
			else
			{
				Collections.sort(gradebookItems, new GradebookComparator(sortType, sortAscending));
			}
		}
		else
		{
			// sort as per preferences
			Gradebook gradebook = getContextGradebook(context, userId);
			
			if (gradebook != null)
			{
				if (gradebook.getSortType() == Gradebook.GradebookSortType.Category)
				{
					checkAndSetDisplayOrder(context, userId, gradebookItems);
				}
				else
				{
					Collections.sort(gradebookItems, new GradebookComparator(gradebook.getSortType(), true));
				}
			}
		}
		
		return gradebookItems;		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public UserGrade getUserGrade(String context, String userId)
	{
		if ((context == null || context.trim().length() == 0) || (userId == null || userId.trim().length() == 0))
		{
			return null;
		}
		
		String userGradesKey = getUserGradeKey(context, userId);
		
		if (userGradesKey != null)
		{
			UserGrade userGrade = (UserGrade)this.threadLocalManager.get(userGradesKey);
			
			if (userGrade != null)
			{
				return new UserGradeImpl((UserGradeImpl)userGrade);
			}
		}
		
		String key = getUserToolGradebookItemsKey(context, userId);
		
		List<ParticipantGradebookItem> userToolGradebookItems = null;
		
		if (key != null)
		{
			userToolGradebookItems = (List<ParticipantGradebookItem>) this.threadLocalManager.get(key);
		}
		
		if (userToolGradebookItems == null)
		{
			userToolGradebookItems = getUserToolGradebookItems(context, userId, false);
		}
		
		Gradebook gradebook = getContextGradebook(context, userId);
		
				
		// overridden/assigned letter grade
		UserGrade userGrade = this.storage.selectUserGrade(gradebook.getId(), userId);
		
		UserGradeImpl actualUserGrade = new UserGradeImpl();
		
		if (userGrade != null)
		{
			actualUserGrade.setLetterGrade(userGrade.getLetterGrade());
		}				
		
		if (userToolGradebookItems != null && userToolGradebookItems.size() > 0)
		{
			computeUsergrade(userToolGradebookItems, gradebook, actualUserGrade);
		}
				
		return actualUserGrade;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public UserGrade getUserGradeLog(String context, String userId)
	{
		if ((context == null || context.trim().length() == 0) || (userId == null || userId.trim().length() == 0))
		{
			return null;
		}
		
		Gradebook gradebook = getContextGradebook(context, userId);
		
		// overridden/assigned letter grade log
		return this.storage.selectUserGradeHistory(gradebook.getId(), userId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<Participant> getUsersGradebookSummary(String context, String groupId, String userId, ParticipantSort sort)
	{
		if ((context == null || context.trim().length() == 0) || (userId == null || userId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		Boolean userAccess = allowEditGradebook(context, userId);
		
		if (!userAccess)
		{
			return new ArrayList<Participant>();
		}
		
		Gradebook gradebook = getContextGradebook(context, userId);
		
		// get the users
		List<Participant> participants = null;
		
		if (groupId == null || groupId.trim().length() == 0)
		{
			participants = getParticipants(context, false);
		}
		else
		{
			participants = getParticipants(context, groupId, false);
		}
		
		if (participants.size() == 0)
		{
			return new ArrayList<Participant>();
		}
		
		// assigned or overridden user grades
		Map<String, UserGrade> userAssignedGrades = this.storage.selectUserGrades(gradebook.getId());
		
		// instructor notes
		Map<String, Notes> instructorUserNotes = this.storage.selectInstructorUsersNotes(gradebook.getId());
		
		// assigned or overridden user grades log
		Map<String, UserGrade> userAssignedGradesLog = this.storage.selectUserGradesHistory(gradebook.getId());
		
		// fetches and adds user gradable items with scores (List of ParticipantGradebookItems for each participant)
		Map<String, List<ParticipantGradebookItem>> userParticipantGradebookItemsScores = fetchUsersGradebookSummaryItems(context, userId, participants, null, gradebook.getReleaseGrades());
		
		Float classTotalScore = 0.0f;
		Float classTotalPoints = 0.0f;
		Float classAveragePercent = null;
		Float totalGradebookItemPoints = 0.0f;
		Float extraCreditPercent = 0.0f;
		int extraCreditScoredUsersCount = 0;
		// set overridden grades
		for (Participant participant : participants)
		{
			List<ParticipantGradebookItem> userToolGradebookItems = userParticipantGradebookItemsScores.get(participant.getUserId());
			// map items to categories
			mapStudentItems(context, userId, userToolGradebookItems);
			
			UserGradeImpl actualUserGrade = new UserGradeImpl();
			
			if (userToolGradebookItems != null && userToolGradebookItems.size() > 0)
			{
				computeUsergrade(userToolGradebookItems, gradebook, actualUserGrade);
				((ParticipantImpl)participant).setGrade(actualUserGrade);
				
				if ((participant.getStatus() != ParticipantStatus.dropped) && (actualUserGrade.getPoints() != null))
				{
					totalGradebookItemPoints = totalGradebookItemPoints + actualUserGrade.getPoints();
					
					// class totals
					if (actualUserGrade.getScore() != null)
					{
						classTotalScore += actualUserGrade.getScore();
					}
					
					if (actualUserGrade.getExtraCreditPercent() != null && actualUserGrade.getExtraCreditPercent() > 0)
					{
						extraCreditPercent += actualUserGrade.getExtraCreditPercent();
						extraCreditScoredUsersCount++;
					}					
					classTotalPoints += actualUserGrade.getPoints();
				}
			}
			
			((ParticipantImpl)participant).setTotalPoints(actualUserGrade.getPoints());
			((ParticipantImpl)participant).setTotalScore(actualUserGrade.getScore());
			
			// assigned or overridden user grade
			UserGrade userGrade = userAssignedGrades.get(participant.getUserId());
			if (userGrade != null)
			{
				((ParticipantImpl)participant).setOverriddenLetterGrade(userGrade);
				
				UserGrade userGradeLog = userAssignedGradesLog.get(participant.getUserId());
				
				if (userGradeLog != null)
				{
					((ParticipantImpl)participant).setOverriddenLetterGradeLog(userGradeLog);
				}
			}
			
			// instructor notes
			Notes instructorNotes = instructorUserNotes.get(participant.getUserId());
			if (instructorNotes != null)
			{
				((ParticipantImpl)participant).setInstructorNotes(instructorNotes);			
			}
		}
			
		// class average
		if (classTotalPoints > 0)
		{
			classAveragePercent = (classTotalScore / classTotalPoints) * 100;
			classAveragePercent = roundToTwoDecimals(classAveragePercent);
			
			// extra credit percent include in the class average
			if (extraCreditPercent > 0 && extraCreditScoredUsersCount > 0)
			{
				Float averageExtraCreditPercent = (extraCreditPercent)/(float)extraCreditScoredUsersCount;
				
				averageExtraCreditPercent = roundToTwoDecimals(averageExtraCreditPercent);
				
				classAveragePercent += averageExtraCreditPercent;
				
				classAveragePercent = roundToTwoDecimals(classAveragePercent);
			}
			
			this.threadLocalManager.set(classAveragePercentCacheKey(context, userId), classAveragePercent);
		}
		
		// class total gradebook item points
		// this.threadLocalManager.set(classTotalPointsCacheKey(context, userId), totalGradebookItemPoints);
		
		
		if (sort != null)
		{
			Collections.sort(participants, sort);
		}
		
		return participants;
	}	
	
	/**
	 * {@inheritDoc}
	 */
	public Map<Participant, List<ParticipantGradebookItem>> getUsersGradebookSummaryAndGradeBookItems(String context, String groupId, String userId, ParticipantSort sort, GradebookItemType itemType)
	{
		if ((context == null || context.trim().length() == 0) || (userId == null || userId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		Boolean userAccess = allowEditGradebook(context, userId);
		
		if (!userAccess)
		{
			return new HashMap<Participant, List<ParticipantGradebookItem>>();
		}
		
		/* get user summary and items */		
		LinkedHashMap<Participant, List<ParticipantGradebookItem>> userSummaryGradebookItems = new LinkedHashMap<Participant, List<ParticipantGradebookItem>>();
		
		Gradebook gradebook = getContextGradebook(context, userId);
		
		// get the users
		List<Participant> participants = null;
		
		if (groupId == null || groupId.trim().length() == 0)
		{
			participants = getParticipants(context, false);
		}
		else
		{
			participants = getParticipants(context, groupId, false);
		}
		
		if (sort != null)
		{
			Collections.sort(participants, sort);
		}
		
		// Date now = new Date();
		
		if (participants.size() == 0)
		{
			return new LinkedHashMap<Participant, List<ParticipantGradebookItem>>();
		}
		
		// instructor notes
		Map<String, Notes> instructorUserNotes = this.storage.selectInstructorUsersNotes(gradebook.getId());

		Float classTotalScore = 0.0f;
		Float classTotalPoints = 0.0f;
		Float classAveragePercent = null;
		Float totalGradebookItemPoints = 0.0f;
		Float extraCreditPercent = 0.0f;
		int extraCreditScoredUsersCount = 0;
		
		// fetches and adds user gradable items with scores (List of ParticipantGradebookItems for each participant)
		Map<String, List<ParticipantGradebookItem>> userParticipantGradebookItemsScores = fetchUsersGradebookSummaryItems(context, userId, participants, itemType, gradebook.getReleaseGrades());
		
		for (Participant participant : participants)
		{
			// List<ParticipantGradebookItem> userToolGradebookItems = getUserToolGradebookItems(context, participant.getUserId(), itemType);
			List<ParticipantGradebookItem> userToolGradebookItems = userParticipantGradebookItemsScores.get(participant.getUserId());

			if (userToolGradebookItems != null && userToolGradebookItems.size() > 0)
			{
				// map items to categories
				mapStudentItems(context, userId, userToolGradebookItems);
				
				UserGradeImpl actualUserGrade = new UserGradeImpl();
				
				if (userToolGradebookItems != null && userToolGradebookItems.size() > 0)
				{
					computeUsergrade(userToolGradebookItems, gradebook, actualUserGrade);
					((ParticipantImpl)participant).setGrade(actualUserGrade);
					
					if ((participant.getStatus() != ParticipantStatus.dropped) && (actualUserGrade.getPoints() != null))
					{
						totalGradebookItemPoints = totalGradebookItemPoints + actualUserGrade.getPoints();
						
						// class totals
						if (actualUserGrade.getScore() != null)
						{
							classTotalScore += actualUserGrade.getScore();
						}
						
						if (actualUserGrade.getExtraCreditPercent() != null && actualUserGrade.getExtraCreditPercent() > 0)
						{
							extraCreditPercent += actualUserGrade.getExtraCreditPercent();
							extraCreditScoredUsersCount++;
						}
						
						classTotalPoints += actualUserGrade.getPoints();
					}
				}
				
				((ParticipantImpl)participant).setTotalPoints(actualUserGrade.getPoints());
				((ParticipantImpl)participant).setTotalScore(actualUserGrade.getScore());				
			}
			
			// instructor notes
			Notes instructorNotes = instructorUserNotes.get(participant.getUserId());
			if (instructorNotes != null)
			{
				((ParticipantImpl)participant).setInstructorNotes(instructorNotes);
			}
			
			userSummaryGradebookItems.put(participant, userToolGradebookItems);
		}
		
		// class average
		if (classTotalPoints > 0)
		{
			classAveragePercent = (classTotalScore / classTotalPoints) * 100;
			classAveragePercent = roundToTwoDecimals(classAveragePercent);
			
			// extra credit percent include in the class average
			if (extraCreditPercent > 0 && extraCreditScoredUsersCount > 0)
			{
				Float averageExtraCreditPercent = (extraCreditPercent)/(float)extraCreditScoredUsersCount;
				
				averageExtraCreditPercent = roundToTwoDecimals(averageExtraCreditPercent);
				
				classAveragePercent += averageExtraCreditPercent;
				
				classAveragePercent = roundToTwoDecimals(classAveragePercent);
			}
					
			this.threadLocalManager.set(classAveragePercentCacheKey(context, userId), classAveragePercent);
		}
		
		// class total gradebook item points
		// this.threadLocalManager.set(classTotalPointsCacheKey(context, userId), totalGradebookItemPoints);
		
		if (sort != null)
		{
			if (sort == ParticipantSort.score_a || sort == ParticipantSort.score_d)
			{
				ArrayList<Participant> participantsList = new ArrayList<Participant>();
	
				for(Map.Entry<Participant, List<ParticipantGradebookItem>> userSummaryGradebookItem : userSummaryGradebookItems.entrySet())
				{
	
					participantsList.add(userSummaryGradebookItem.getKey());
	
				}
				//Collections.sort(participants, sort);
				Collections.sort(participantsList, sort);
				
				LinkedHashMap<Participant, List<ParticipantGradebookItem>> userSummaryGradebookItemsMap = new LinkedHashMap<Participant, List<ParticipantGradebookItem>>();
				
				for (Participant participant : participantsList)
				{
					List<ParticipantGradebookItem> participantGradebookItemList= userSummaryGradebookItems.get(participant);
					userSummaryGradebookItemsMap.put(participant, participantGradebookItemList);
				}
				
				userSummaryGradebookItems.clear();
				userSummaryGradebookItems.putAll(userSummaryGradebookItemsMap);
			}			
		}
				
		return userSummaryGradebookItems;
	}	

	/**
	 * {@inheritDoc}
	 */
	public int getUsersGradesCount(String context, String userId)
	{
		if ((context == null || context.trim().length() == 0) || (userId == null || userId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		Gradebook gradebook = getContextGradebook(context, userId);
		
		return this.storage.selectUserGradesCount(gradebook.getId());
	}

	/**
	 * {@inheritDoc}
	 */
	public List<ParticipantGradebookItem> getUserToolGradebookItems(String context, String userId, boolean computeUserGrade)
	{
		if ((context == null || context.trim().length() == 0) || (userId == null || userId.trim().length() == 0))
		{
			return new ArrayList<ParticipantGradebookItem>();
		}
		
		/*
		// TODO instructor should have access to dropped and blocked user items
		Boolean userAccess = allowEditGradebook(context, userId);
		
		if (!userAccess)
		{
			userAccess = allowGetGradebook(context, userId);
			
			if (!userAccess)
			{
				return new ArrayList<ParticipantGradebookItem>();
			}
		}
		*/
		
		// sort as per preferences
		Gradebook gradebook = getContextGradebook(context, userId);
		
		List<ParticipantGradebookItem> gradebookItems = new ArrayList<ParticipantGradebookItem>();
		
		// get the items from the providers				
		for (GradebookItemProvider provider : this.providers)
		{
			// get the items from this provider
			List<ParticipantGradebookItem> userGradableItems = provider.getUserToolGradebookItems(context, userId, null, false);
			
			if (userGradableItems != null)
			{
				gradebookItems.addAll(userGradableItems);
			}
		}
		
		// map items to categories
		mapStudentItems(context, userId, gradebookItems);
		
		if (gradebook != null)
		{
			Collections.sort(gradebookItems, new ParticipantGradebookComparator(gradebook.getSortType(), true));
		}
		
		String key = getUserToolGradebookItemsKey(context, userId);
		
		if (key != null)
		{
			this.threadLocalManager.set(key, gradebookItems);
		}
		
		if (computeUserGrade)
		{
			/*compute user grade*/
			// overridden/assigned letter grade
			UserGrade userGrade = this.storage.selectUserGrade(gradebook.getId(), userId);
			
			UserGradeImpl actualUserGrade = new UserGradeImpl();
			
			if (userGrade != null)
			{
				actualUserGrade.setLetterGrade(userGrade.getLetterGrade());
			}		
			
			if (gradebookItems != null && gradebookItems.size() > 0)
			{
				computeUsergrade(gradebookItems, gradebook, actualUserGrade);
			}
			
			String userGradesKey = getUserGradeKey(context, userId);
			
			if (userGradesKey != null)
			{
				this.threadLocalManager.set(userGradesKey, actualUserGrade);
			}
		}
		
		// get completion status from coursemap
		Map<String, ParticipantGradebookItem> participantGradebookItemsMap = new HashMap<String, ParticipantGradebookItem>();
		for (ParticipantGradebookItem participantGradebookItem : gradebookItems)
		{
			/* id's 
			 	"MNEME-"+ assessment.getId(), JForumItemProvider.CATEGORY + "-" + String.valueOf(category.getId())
			 	JForumItemProvider.FORUM + "-" + String.valueOf(forum.getId()),
				JForumItemProvider.TOPIC + "-" + String.valueOf(topic.getId());
			*/
			
			if (participantGradebookItem.getId().startsWith("MNEME-"))
			{
				participantGradebookItemsMap.put(participantGradebookItem.getId().substring("MNEME-".length()), participantGradebookItem);
			}
			else
			{
				participantGradebookItemsMap.put(participantGradebookItem.getId(), participantGradebookItem);
			}
		}
		// get the map
		CourseMapMap map = this.courseMapService.getMap(context, userId);
		if (map != null)
		{
			List<CourseMapItem> userCourseMapItems = map.getItems();
			
			if (userCourseMapItems != null && !userCourseMapItems.isEmpty())
			{
				for (CourseMapItem courseMapItem : userCourseMapItems)
				{
					// map coursemap items with gradebook items
					if (participantGradebookItemsMap.containsKey(courseMapItem.getId()))
					{
						ParticipantGradebookItem participantGradebookItem = participantGradebookItemsMap.get(courseMapItem.getId());
						
						((ParticipantGradebookItemImpl)participantGradebookItem).courseMapItem = courseMapItem;
					}					
				}
			}
		}
		
		return gradebookItems;
	}


	/**
	 * {@inheritDoc}
	 */
	public List<ParticipantGradebookItem> getUserToolGradebookItems(String context, String userId, String fetchedByUserId, boolean allSubmissions)
	{
		if ((context == null || context.trim().length() == 0) || (userId == null || userId.trim().length() == 0) || (fetchedByUserId == null || fetchedByUserId.trim().length() == 0))
		{
			return new ArrayList<ParticipantGradebookItem>();
		}
		
		if (!allSubmissions)
		{
			return getUserToolGradebookItems(context, userId, true); 
		}
		
		// access check. Only instructor can access all submissions
		if (!allowEditGradebook(context, fetchedByUserId))
		{
			return new ArrayList<ParticipantGradebookItem>(); 
		}
		
		List<ParticipantGradebookItem> gradebookItems = new ArrayList<ParticipantGradebookItem>();
		
		// get all submissions/evaluations and compute grade. While computing grade use best released score
		// get the items from the providers				
		for (GradebookItemProvider provider : this.providers)
		{
			// get the items from this provider
			List<ParticipantGradebookItem> userGradableItems = provider.getUserToolGradebookItems(context, userId, null, true);
			
			if (userGradableItems != null)
			{
				gradebookItems.addAll(userGradableItems);
			}
		}
		
		// sort as per preferences
		Gradebook gradebook = getContextGradebook(context, userId);
		
		// map items to categories
		mapStudentItems(context, userId, gradebookItems);
		
		if (gradebook != null)
		{
			Collections.sort(gradebookItems, new ParticipantGradebookComparator(gradebook.getSortType(), true));
		}
		
		// get completion status from coursemap
		Map<String, ParticipantGradebookItem> participantGradebookItemsMap = new HashMap<String, ParticipantGradebookItem>();
		for (ParticipantGradebookItem participantGradebookItem : gradebookItems)
		{
			/* id's 
			 	"MNEME-"+ assessment.getId(), JForumItemProvider.CATEGORY + "-" + String.valueOf(category.getId())
			 	JForumItemProvider.FORUM + "-" + String.valueOf(forum.getId()),
				JForumItemProvider.TOPIC + "-" + String.valueOf(topic.getId());
			*/
			
			if (participantGradebookItem.getId().startsWith("MNEME-"))
			{
				participantGradebookItemsMap.put(participantGradebookItem.getId().substring("MNEME-".length()), participantGradebookItem);
			}
			else
			{
				participantGradebookItemsMap.put(participantGradebookItem.getId(), participantGradebookItem);
			}
		}
		
		// get the map
		CourseMapMap map = this.courseMapService.getMap(context, userId);
		if (map != null)
		{
			List<CourseMapItem> userCourseMapItems = map.getItems();
			
			if (userCourseMapItems != null && !userCourseMapItems.isEmpty())
			{
				for (CourseMapItem courseMapItem : userCourseMapItems)
				{
					// map coursemap items with gradebook items
					if (participantGradebookItemsMap.containsKey(courseMapItem.getId()))
					{
						ParticipantGradebookItem participantGradebookItem = participantGradebookItemsMap.get(courseMapItem.getId());
						
						((ParticipantGradebookItemImpl)participantGradebookItem).courseMapItem = courseMapItem;
					}					
				}
			}
		}
		
		return gradebookItems;
	}
	
	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		if (logger.isInfoEnabled())
		{
			logger.info("init....");
		}
		
		try
		{

			// storage - as configured
			if (this.storageKey != null)
			{
				// if set to "SQL", replace with the current SQL vendor
				if ("SQL".equals(this.storageKey))
				{
					this.storageKey = sqlService.getVendor();
				}

				this.storage = this.storgeOptions.get(this.storageKey);
			}

			if (storage == null) logger.warn("no storage set: " + this.storageKey);

			storage.init();

			logger.info("init(): storage: " + this.storage);
		}
		catch (Throwable t)
		{
			if (logger.isWarnEnabled())
			{
				logger.warn("init(): ", t);
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean isTitleAvailable(String context, String userId, String title)
	{
		if ((context == null || context.trim().length() == 0) || (userId == null || userId.trim().length() == 0) || (title == null || title.trim().length() == 0))
		{
			throw new IllegalArgumentException("title is missing.");
		}
		
		// user access check
		if (!allowEditGradebook(context, userId))
		{
			throw new IllegalArgumentException("user has no access.");
		}
		
		title = title.trim();
		
		// get all gradable items and check the title
		List<GradebookItem> gradebookItems = fetchGradebookItems(context, userId, false, false, null, false);
				
		if (gradebookItems != null && gradebookItems.size() > 0)
		{
			String existingTitle = null;
			
			for (GradebookItem gradebookItem : gradebookItems)
			{
				existingTitle = gradebookItem.getTitle();
				
				if (existingTitle != null && existingTitle.trim().length() > 0)
				{
					if (existingTitle.trim().equalsIgnoreCase(title))
					{
						return false;
					}
				}
			}
		}
		
		return true;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean isTitleDefined(String context, String userId, String title, String id, GradebookItemType itemType)
	{
		if ((context == null || context.trim().length() == 0) || (userId == null || userId.trim().length() == 0) || (title == null || title.trim().length() == 0) || (id == null || id.trim().length() == 0) || (itemType == null))
		{
			throw new IllegalArgumentException("context, title, id or itemType is missing.");
		}
		
		// user access check
		if (!allowEditGradebook(context, userId))
		{
			throw new IllegalArgumentException("user has no access.");
		}
		
		title = title.trim();
		
		// get all gradable items and check the title
		List<GradebookItem> gradebookItems = fetchGradebookItems(context, userId, false, false, null, false);		
		
		if (gradebookItems != null && gradebookItems.size() > 0)
		{
			String existingTitle = null;
			
			for (GradebookItem gradebookItem : gradebookItems)
			{
				existingTitle = gradebookItem.getTitle();
				
				if (existingTitle != null && existingTitle.trim().length() > 0)
				{
					if (existingTitle.trim().equalsIgnoreCase(title))
					{ 
						String itemId = null;
						
						if (itemType == GradebookItemType.assignment || itemType == GradebookItemType.offline || itemType == GradebookItemType.test || itemType == GradebookItemType.survey)
						{
							itemId = "MNEME-"+ id;
						}
						else if (itemType == GradebookItemType.category)
						{
							itemId = "CAT-" + id;
						}
						else if (itemType == GradebookItemType.forum)
						{
							itemId = "FORUM-" + id;
						}
						else if (itemType == GradebookItemType.topic)
						{
							itemId = "TOPIC-" + id;
						}
						
						if (gradebookItem.getId().equalsIgnoreCase(itemId))
						{
							return true;
						}
					}
				}
			}
		}
		
		return false;		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean isToolAvailable(String context)
	{
		if (context == null || context.trim().length() == 0)
		{
			return false;
		}
		
		try
		{
			Site site = this.siteService.getSite(context);
			
			if (site != null)
			{
				return (site.getToolForCommonId("e3.gradebook") != null);
			}
		}
		catch (IdUnusedException e)
		{
		}		
		
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public void modifyContextGradebook(String context, Boolean showLetterGrade, ReleaseGrades releaseGrades, int gradingScaleId, Map<String, Float> gradingScalePercent, String modifiedByUserId)
	{
		if ((context == null || context.trim().length() == 0) || (modifiedByUserId == null || modifiedByUserId.trim().length() == 0) || (gradingScalePercent == null))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		// user access check
		Boolean userAccess = allowEditGradebook(context, modifiedByUserId);
		
		if (!userAccess)
		{
			userAccess = allowGetGradebook(context, modifiedByUserId);
			
			if (!userAccess)
			{
				return;
			}
		}
		
		// update gradebook
		Gradebook gradebook = this.storage.selectGradebook(context, modifiedByUserId);
		
		((GradebookImpl)gradebook).setShowLetterGrade(showLetterGrade);
		((GradebookImpl)gradebook).setReleaseGrades(releaseGrades);
		
		// should be one of the context grading scales
		for (GradingScale gradingScale : gradebook.getContextGradingScales())
		{
			if (gradingScaleId == gradingScale.getId())
			{
				((GradebookImpl)gradebook).setGradingScale(gradingScale);
				
				List<GradingScalePercent> selectedContextgradingScalePercent = gradingScale.getGradingScalePercent();
				selectedContextgradingScalePercent.clear();
				
				// for (Map<String, Float> selectedGradingScalePercent : gradingScalePercent)
				for (Map.Entry<String, Float> selectedGradingScalePercent : gradingScalePercent.entrySet())
				{
					GradingScalePercentImpl modifiedGradingScalePercent = new GradingScalePercentImpl();
					modifiedGradingScalePercent.setLetterGrade(selectedGradingScalePercent.getKey());
					modifiedGradingScalePercent.setPercent(selectedGradingScalePercent.getValue());
					
					selectedContextgradingScalePercent.add(modifiedGradingScalePercent);
				}
				
				break;
			}
		}
		
		((GradebookImpl)gradebook).setModifiedByUserId(modifiedByUserId);
		
		this.storage.update(gradebook);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void modifyContextGradebookAttributes(String context, Boolean showLetterGrade, ReleaseGrades releaseGrades, Boolean dropLowestScore, String modifiedByUserId)
	{
		if ((context == null || context.trim().length() == 0) || (modifiedByUserId == null || modifiedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		if (showLetterGrade == null && releaseGrades == null && dropLowestScore == null)
		{
			return;
		}
		
		// user access check
		Boolean userAccess = allowEditGradebook(context, modifiedByUserId);
		
		if (!userAccess)
		{
			userAccess = allowGetGradebook(context, modifiedByUserId);
			
			if (!userAccess)
			{
				return;
			}
		}
		
		// update gradebook attributes
		Gradebook gradebook = this.storage.selectGradebook(context, modifiedByUserId);
		
		if (showLetterGrade != null)
		{
			((GradebookImpl)gradebook).setShowLetterGrade(showLetterGrade);
		}
		
		if (releaseGrades != null)
		{
			((GradebookImpl)gradebook).setReleaseGrades(releaseGrades);
		}
		
		if (dropLowestScore != null)
		{
			((GradebookImpl)gradebook).setDropLowestScore(dropLowestScore);
		}
		
		((GradebookImpl)gradebook).setModifiedByUserId(modifiedByUserId);
		
		this.storage.updateGradebookAttributes(gradebook);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void modifyContextGradebookBoostByAttributes(String context, BoostUserGradesType boostUserGradesType, Float boostUserGradesBy, String modifiedByUserId)
	{
		if ((context == null || context.trim().length() == 0) || (modifiedByUserId == null || modifiedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		// user access check
		Boolean userAccess = allowEditGradebook(context, modifiedByUserId);
		
		if (!userAccess)
		{
			userAccess = allowGetGradebook(context, modifiedByUserId);
			
			if (!userAccess)
			{
				return;
			}
		}
		
		// update gradebook boost by attributes
		Gradebook gradebook = this.storage.selectGradebook(context, modifiedByUserId);
		
		if (boostUserGradesType == null)
		{
			((GradebookImpl)gradebook).setBoostUserGradesType(null);
			((GradebookImpl)gradebook).setBoostUserGradesBy(null);
		}
		else
		{
			if (boostUserGradesBy == null || boostUserGradesBy <= 0)
			{
				((GradebookImpl)gradebook).setBoostUserGradesType(null);
				((GradebookImpl)gradebook).setBoostUserGradesBy(null);
			}
			else
			{
				((GradebookImpl)gradebook).setBoostUserGradesType(boostUserGradesType);
				((GradebookImpl)gradebook).setBoostUserGradesBy(boostUserGradesBy);
			}
		}
		
		((GradebookImpl)gradebook).setModifiedByUserId(modifiedByUserId);
		
		this.storage.updateGradebookAttributes(gradebook);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void modifyContextGradebookCategories(String context, List<GradebookCategory> gradebookCategories, String modifiedByUserId)
	{
		if ((context == null || context.trim().length() == 0) || (gradebookCategories == null || gradebookCategories.size() == 0) || (modifiedByUserId == null || modifiedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		// access check
		Boolean userAccess = allowEditGradebook(context, modifiedByUserId);
		
		if (!userAccess)
		{
			return;
		}
		
		// TODO check and fix the order
		
		this.storage.updateGradebookCategories(context, gradebookCategories, modifiedByUserId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void modifyContextGradebookCategoriesDropLowestScoresNumber(String context, List<GradebookCategory> gradebookCategories, String modifiedByUserId)
	{
		if ((context == null || context.trim().length() == 0) || (modifiedByUserId == null || modifiedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		// access check
		Boolean userAccess = allowEditGradebook(context, modifiedByUserId);
		
		if (!userAccess)
		{
			return;
		}
		
		this.storage.updateGradebookCategoriesDropLowestScoresNumber(context, gradebookCategories, modifiedByUserId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void modifyContextGradebookCategoryType(String context, Gradebook.CategoryType categoryType, String modifiedByUserId)
	{
		if ((context == null || context.trim().length() == 0) || (categoryType == null) || (modifiedByUserId == null || modifiedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing");
		}
		
		// access check
		Boolean userAccess = allowEditGradebook(context, modifiedByUserId);
		
		if (!userAccess)
		{
			return;
		}
		
		/* if category type is changed from standard to custom remove all the existing standard categories and add default standard categories. If category type is changed 
		 * from custom to standard remove all the existing custom categories and add default standard categories */
		this.storage.updateGradebookCategoryType(context, categoryType, modifiedByUserId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void modifyContextGradebookGradingScale(String context, int gradingScaleId, Map<String, Float> gradingScalePercent, String modifiedByUserId)
	{
		if ((context == null || context.trim().length() == 0) || (modifiedByUserId == null || modifiedByUserId.trim().length() == 0) || (gradingScalePercent == null))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		// user access check
		Boolean userAccess = allowEditGradebook(context, modifiedByUserId);
		
		if (!userAccess)
		{
			userAccess = allowGetGradebook(context, modifiedByUserId);
			
			if (!userAccess)
			{
				return;
			}
		}
		
		// update gradebook
		Gradebook gradebook = this.storage.selectGradebook(context, modifiedByUserId);
		
		// should be one of the context grading scales
		for (GradingScale gradingScale : gradebook.getContextGradingScales())
		{
			if (gradingScaleId == gradingScale.getId())
			{
				((GradebookImpl)gradebook).setGradingScale(gradingScale);
				
				List<GradingScalePercent> selectedContextgradingScalePercent = gradingScale.getGradingScalePercent();
				selectedContextgradingScalePercent.clear();
				
				// for (Map<String, Float> selectedGradingScalePercent : gradingScalePercent)
				for (Map.Entry<String, Float> selectedGradingScalePercent : gradingScalePercent.entrySet())
				{
					GradingScalePercentImpl modifiedGradingScalePercent = new GradingScalePercentImpl();
					modifiedGradingScalePercent.setLetterGrade(selectedGradingScalePercent.getKey());
					modifiedGradingScalePercent.setPercent(selectedGradingScalePercent.getValue());
					
					selectedContextgradingScalePercent.add(modifiedGradingScalePercent);
				}
				
				break;
			}
		}
		
		((GradebookImpl)gradebook).setModifiedByUserId(modifiedByUserId);
		
		this.storage.update(gradebook);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void modifyGradingScale(int gradebookId, int gradingScaleId, Map<String, Float> gradingScalePercent, String modifiedByUserId)
	{
		if ((gradebookId <= 0) || (modifiedByUserId == null || modifiedByUserId.trim().length() == 0) || (gradingScalePercent == null))
		{
			throw new IllegalArgumentException("gradebook information is missing.");
		}
		
		Gradebook gradebook = this.storage.selectGradebook(gradebookId);
		
		if (gradebook == null)
		{
			return;
		}
				
		// user access check
		Boolean userAccess = allowEditGradebook(gradebook.getContext(), modifiedByUserId);
		
		if (!userAccess)
		{
			userAccess = allowGetGradebook(gradebook.getContext(), modifiedByUserId);
			
			if (!userAccess)
			{
				return;
			}
		}
		
		/*update grading sacle of the gradebook may or may not be the default grading scale*/		
		// should be one of the context grading scales
		for (GradingScale gradingScale : gradebook.getContextGradingScales())
		{
			if (gradingScaleId == gradingScale.getId())
			{
				List<GradingScalePercent> selectedContextgradingScalePercent = gradingScale.getGradingScalePercent();
				selectedContextgradingScalePercent.clear();
				
				for (Map.Entry<String, Float> selectedGradingScalePercent : gradingScalePercent.entrySet())
				{
					GradingScalePercentImpl modifiedGradingScalePercent = new GradingScalePercentImpl();
					modifiedGradingScalePercent.setLetterGrade(selectedGradingScalePercent.getKey());
					modifiedGradingScalePercent.setPercent(selectedGradingScalePercent.getValue());
					
					selectedContextgradingScalePercent.add(modifiedGradingScalePercent);
				}
				
				// modify grading percentages
				this.storage.updateGradingScale(gradebook.getId(), gradingScale);
				
				break;
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void modifyImportItemMapping(String context, Gradebook.CategoryType categoryType, List<GradebookCategoryItemMap> categoryItemMaps, String modifiedByUserId)
	{
		if ((context == null || context.trim().length() == 0) || (categoryType == null) || (categoryItemMaps == null) || (modifiedByUserId == null || modifiedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		// access check
		Boolean userAccess = allowEditGradebook(context, modifiedByUserId);
		
		if (!userAccess)
		{
			return;
		}
		
		// verify item map and save the map.
		Iterator<GradebookCategoryItemMap> iterator = categoryItemMaps.iterator();
		
		GradebookCategoryItemMap gradebookCategoryItemMap = null;
		while (iterator.hasNext()) 
		{
			gradebookCategoryItemMap = iterator.next();
			
			if (gradebookCategoryItemMap.getCategoryId() == 0 || gradebookCategoryItemMap.getItemId() == null || gradebookCategoryItemMap.getItemId().trim().length() == 0)
			{
				iterator.remove();
			}
		}
		
		this.storage.addModifyDeleteGradebookCategoryMappedItems(context, categoryType, categoryItemMaps);		
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void modifyItemCategoryMapping(String context, String itemToolId, int categoryId, GradebookItemType itemtype, String modifiedByUserId)
	{
		if ((context == null || context.trim().length() == 0) || (itemToolId == null || itemToolId.trim().length() == 0) || (categoryId <= 0) || (itemtype == null) || (modifiedByUserId == null || modifiedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		if (!allowEditGradebook(context, modifiedByUserId))
		{
			return;
		}
		
		String itemId = null;
		
		if (itemtype == GradebookItemType.assignment || itemtype == GradebookItemType.offline || itemtype == GradebookItemType.survey || itemtype == GradebookItemType.test)
		{
			itemId = GradebookItemType.assignment.getItemIdCode() + "-" + itemToolId;
		}
		else if (itemtype == GradebookItemType.category)
		{
			itemId = GradebookItemType.category.getItemIdCode() + "-" + itemToolId;
		}
		else if (itemtype == GradebookItemType.forum)
		{
			itemId = GradebookItemType.forum.getItemIdCode() + "-" + itemToolId;
		}
		else if (itemtype == GradebookItemType.topic)
		{
			itemId = GradebookItemType.topic.getItemIdCode() + "-" + itemToolId;
		}
		
		this.storage.updateItemCategoryMap(context, itemId, categoryId);
		
		// re-order the display order
		fetchToolGradableItemsAndResetItemMap(context, modifiedByUserId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void modifyItemMapping(String context, List<GradebookCategoryItemMap> categoryItemMaps, String modifiedByUserId)
	{
		if ((context == null || context.trim().length() == 0) || (categoryItemMaps == null) || (modifiedByUserId == null || modifiedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		// access check
		Boolean userAccess = allowEditGradebook(context, modifiedByUserId);
		
		if (!userAccess)
		{
			return;
		}
		
		// verify item map and save the map.
		Iterator<GradebookCategoryItemMap> iterator = categoryItemMaps.iterator();
		
		GradebookCategoryItemMap gradebookCategoryItemMap = null;
		while (iterator.hasNext()) 
		{
			gradebookCategoryItemMap = iterator.next();
			
			if (gradebookCategoryItemMap.getCategoryId() == 0 || gradebookCategoryItemMap.getItemId() == null || gradebookCategoryItemMap.getItemId().trim().length() == 0)
			{
				iterator.remove();
			}
		}
		
		// if not changed don't save
		Gradebook gradebook = getContextGradebook(context, modifiedByUserId);
		
		// check existing map and save if changed
		List<GradebookCategoryItemMap> exisGradebookCategoryItemMapList = this.storage.selectGradebookCategoryMappedItems(gradebook.getId(), gradebook.getCategoryType());
		
		//map of mapped items already mapped
		Map<String, GradebookCategoryItemMap> exisGradebookCategoryItemMapListMap = new HashMap<String, GradebookCategoryItemMap>();
		for (GradebookCategoryItemMap gradebookCategoryMapItem : exisGradebookCategoryItemMapList)
		{
			exisGradebookCategoryItemMapListMap.put(gradebookCategoryMapItem.getItemId(), gradebookCategoryMapItem);
		}
		
		boolean changed = false;
		GradebookCategoryItemMap mappedItemCategory = null;
		for (GradebookCategoryItemMap modCategoryItemMap : categoryItemMaps)
		{
			if (exisGradebookCategoryItemMapListMap.containsKey(modCategoryItemMap.getItemId()))
			{
				mappedItemCategory = exisGradebookCategoryItemMapListMap.get(modCategoryItemMap.getItemId());
				
				if ((mappedItemCategory.getCategoryId() != modCategoryItemMap.getCategoryId())|| (mappedItemCategory.getDisplayOrder() != modCategoryItemMap.getDisplayOrder()))
				{
					changed = true;
				}
			}
			else
			{
				// changed = true;
			}
		}
		
		// retain unpublished items
		for (GradebookCategoryItemMap existMapItem : exisGradebookCategoryItemMapList)
		{
			if (existMapItem.getDisplayOrder() == 0)
			{
				 GradebookCategoryItemMap unpublishItem = newGradebookCategoryItemMap(existMapItem.getItemId(), existMapItem.getCategoryId(), 0);
				 if (!categoryItemMaps.contains(unpublishItem))
				 {
					 logger.debug("Modify Item Mapping adding unpublished" + unpublishItem.getItemId());
					 categoryItemMaps.add(unpublishItem);
				 }
			}
		}
		
		if (changed)
		{
			this.storage.addModifyDeleteGradebookCategoryMappedItems(context, gradebook.getCategoryType(), categoryItemMaps);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void modifyItemScores(String context, String itemId, GradebookItemType itemType, List<ParticipantItemDetails> participantItemDetails, String modifiedByUserId, Date scoresFetchedTime)
	{
		if ((context == null || context.trim().length() == 0) || (itemId == null || itemId.trim().length() == 0) || (itemType == null) || (participantItemDetails == null || participantItemDetails.size() == 0) || (modifiedByUserId == null || modifiedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		if (!allowEditGradebook(context, modifiedByUserId))
		{
			return;
		}
		
		if (itemId.startsWith(CATEGORY + "-") || itemId.startsWith(FORUM + "-") || itemId.startsWith(TOPIC + "-"))
		{
			// update jforum scores and released status
			for (GradebookItemProvider provider : this.providers)
			{
				// only jforum provider should have this method
				if (provider.modifyJforumScores(context, itemId, participantItemDetails, modifiedByUserId, scoresFetchedTime) != null)
				{
					break;
				}
			}
		}		
		// update mneme submission scores
		else if (itemId.startsWith("MNEME-"))
		{
			for (GradebookItemProvider provider : this.providers)
			{
				// only mneme provider should have this method
				if (provider.modifyMnemeScores(context, itemId, participantItemDetails, modifiedByUserId, scoresFetchedTime) != null)
				{
					break;
				}
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void modifyUserGrades(String context, List<UserGrade> userLetterGrades, String modifiedByUserId)
	{		
		if ((context == null || context.trim().length() == 0) || (userLetterGrades == null || userLetterGrades.size() == 0) || (modifiedByUserId == null || modifiedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		// access check
		Boolean userAccess = allowEditGradebook(context, modifiedByUserId);
		
		if (!userAccess)
		{
			return;
		}
		
		Gradebook gradebook = getContextGradebook(context, modifiedByUserId);
		
		// validate - letterGrade values with gradebook grading scale letter grade
		GradingScale gradingScale = gradebook.getGradingScale();
		Set<String> contextGradingScalePercentLetterGrades = new HashSet<String>();
		if (gradingScale != null)
		{
			List<GradingScalePercent> gradingScalePercentList = gradingScale.getGradingScalePercent();
			
			if (gradingScalePercentList != null)
			{
				for (GradingScalePercent gradingScalePercent : gradingScalePercentList)
				{
					String letterGrade = gradingScalePercent.getLetterGrade();
					
					if (letterGrade != null && letterGrade.trim().length() > 0)
					{
						contextGradingScalePercentLetterGrades.add(letterGrade);
					}
				}
			}
		}
		
		Set<String> exisUserGradeUserIds = new HashSet<String>();
		for (Iterator<UserGrade> it = userLetterGrades.iterator(); it.hasNext();)
		{
			UserGrade userGrade = it.next();
			
			String studentId = userGrade.getUserId();
			String letterGrade = userGrade.getLetterGrade();

			if (studentId == null || studentId.trim().length() == 0)
			{
				it.remove(); 
				continue;
			}
			
			// avoid any duplicate userId's
			if (exisUserGradeUserIds.contains(studentId))
			{
				it.remove(); 
				continue;
			}
			
			// removed check to allow letter grades to blocked users
			// if user is not existing in the site remove the entry
			/*
			if (!allowGetGradebook(context, studentId.trim()))
			{
				it.remove(); 
				continue;
			}
			*/
			
			if (letterGrade == null || letterGrade.trim().length() == 0)
			{
				continue;
			}
			
			((UserGradeImpl)userGrade).setLetterGrade(userGrade.getLetterGrade().toUpperCase());
			if (!contextGradingScalePercentLetterGrades.contains(userGrade.getLetterGrade()))
			{
				((UserGradeImpl)userGrade).setLetterGrade(null);
			}
			
			exisUserGradeUserIds.add(studentId);
		}
		
		if (userLetterGrades != null && userLetterGrades.size() > 0)
		{
			this.storage.updateUserGrades(gradebook.getId(), userLetterGrades, modifiedByUserId);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void modifyUserScores(String context, String userId, List<ParticipantGradebookItem> participantGradebookItems, String modifiedByUserId, Date scoresFetchedTime)
	{
		if ((context == null || context.trim().length() == 0) || (userId == null || userId.trim().length() == 0) || (participantGradebookItems == null || participantGradebookItems.size() == 0) || (modifiedByUserId == null || modifiedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		if (!allowEditGradebook(context, modifiedByUserId))
		{
			return;
		}
		
		// update user score for each item
		GradebookItem gradebookItem = null;
		String itemId = null;
		ParticipantItemDetails participantItemDetails = null;
		
		List<ParticipantItemDetails> mnemeParticipantItemDetails = new ArrayList<ParticipantItemDetails>();
		
		for (ParticipantGradebookItem participantGradebookItem : participantGradebookItems)
		{
			gradebookItem = participantGradebookItem.getGradebookItem();
			participantItemDetails = participantGradebookItem.getParticipantItemDetails();
			
			if (gradebookItem != null && participantItemDetails != null && participantItemDetails.getUserId() != null && participantItemDetails.getUserId().equalsIgnoreCase(userId))
			{
				itemId = gradebookItem.getId();
								
				if (itemId.startsWith(CATEGORY + "-") || itemId.startsWith(FORUM + "-") || itemId.startsWith(TOPIC + "-"))
				{
					// update user jforum score and released status
					for (GradebookItemProvider provider : this.providers)
					{
						// only jforum provider should have this method
						if (provider.modifyJforumUserScore(context, itemId, participantItemDetails, modifiedByUserId, scoresFetchedTime) != null)
						{
							break;
						}
					}
				}		
				// update mneme user submission score
				else if (itemId.startsWith("MNEME-"))
				{
					mnemeParticipantItemDetails.add(participantItemDetails);
				}
				
			}
		}
		
		if (mnemeParticipantItemDetails.size() > 0)
		{
			for (GradebookItemProvider provider : this.providers)
			{
				// only mneme provider should have this method
				if (provider.modifyMnemeUserScores(context, userId, mnemeParticipantItemDetails, modifiedByUserId, scoresFetchedTime) != null)
				{
					break;
				}
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public GradebookCategory newGradebookCategory(String title, Float weight, WeightDistribution weightDistribution, int order)
	{
		return new GradebookCategoryImpl(-1, title, weight, weightDistribution, order);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public GradebookCategory newGradebookCategory(String title, Float weight, WeightDistribution weightDistribution, int order, CategoryType categoryType)
	{
		return new GradebookCategoryImpl(-1, title, weight, weightDistribution, order, categoryType);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public GradebookCategoryItemMap newGradebookCategoryItemMap(String itemId, int categoryId, int displayOrder)
	{
		return new GradebookCategoryItemMapImpl(itemId, categoryId, displayOrder);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public GradebookItem newGradebookItem(String id, String title, Float points, Date dueDate, Date openDate, GradebookItemType type)
	{
		return new GradebookItemImpl(id, title, points, dueDate, openDate, type);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public GradebookItem newGradebookItem(String id, String title, Float points, Float averageScore, Date dueDate, Date openDate, Date closeDate, GradebookItemType type, GradebookItemAccessStatus accessStatus, Integer submittedCount)
	{
		return new GradebookItemImpl(id, title, points, averageScore, dueDate, openDate, closeDate, type, accessStatus, submittedCount);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public ParticipantGradebookItem newParticipantGradebookItem(String id, GradebookItem gradebookItem, ParticipantItemDetails participantItemDetails)
	{
		
		return new ParticipantGradebookItemImpl(id,gradebookItem, participantItemDetails );
	}
	
	/**
	 * {@inheritDoc}
	 */
	public ParticipantItemDetails newParticipantItemDetails(String id, String displayId, String groupTitle, String sortName, ParticipantStatus status, Integer count, Date evaluatedDate, Date reviewedDate, Float score, Date startedDate, Date finishedDate, Boolean evaluationNotReviewed, Boolean released, String reviewLink, String gradingLink, Boolean inProgress, String submissionId, Boolean isSubmissionLate, Boolean isAutoSubmission)
	{
		return new ParticipantItemDetailsImpl(id, displayId, groupTitle, sortName, status, count, evaluatedDate, reviewedDate, score, startedDate, finishedDate, evaluationNotReviewed, released, reviewLink, gradingLink, inProgress, submissionId, isSubmissionLate, isAutoSubmission);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public ParticipantItemDetails newParticipantItemDetails(String id, String displayId, String groupTitle, String sortName, ParticipantStatus status, Integer count, Date evaluatedDate, Date reviewedDate, Float score, Date startedDate, Date finishedDate, Boolean evaluationNotReviewed, Boolean released, String reviewLink, String gradingLink, Boolean inProgress, String submissionId, Boolean isBestSubmission, Boolean isSubmissionLate, Boolean isAutoSubmission)
	{
		return new ParticipantItemDetailsImpl(id, displayId, groupTitle, sortName, status, count, evaluatedDate, reviewedDate, score, startedDate, finishedDate, evaluationNotReviewed, released, reviewLink, gradingLink, inProgress, submissionId, isBestSubmission, isSubmissionLate, isAutoSubmission);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public UserGrade newUserGrade(String userId, String letterGrade, Date prevGradeAssignedDate)
	{
		return new UserGradeImpl(userId, letterGrade, prevGradeAssignedDate);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public UserItemSpecialAccess newUserItemSpecialAccessImpl(Date openDate, Date dueDate, Date acceptUntilDate, Boolean hideUntilOpen, Boolean overrideOpenDate, Boolean overrideDueDate, Boolean overrideAcceptUntilDate, Boolean overrideHideUntilOpen, Boolean datesValid)
	{
		return new UserItemSpecialAccessImpl(openDate, dueDate, acceptUntilDate, hideUntilOpen, overrideOpenDate, overrideDueDate, overrideAcceptUntilDate, overrideHideUntilOpen, datesValid);
	}
	
	/**
	 * {@inheritDoc} JforumItemProvider for students screen
	 */
	/*
	public ParticipantItemDetails newParticipantItemDetails(Date startedDate, Date finishedDate, Float score, Date evaluatedDate, Date reviewedDate, Boolean evaulationNotReviewed, Integer count)
	{
		return new ParticipantItemDetailsImpl(startedDate, finishedDate, score, evaluatedDate, reviewedDate, evaulationNotReviewed, count);
	}
	*/
	
	/**
	 * {@inheritDoc}
	 */
	/*
	public ParticipantItemDetails newParticipantItemDetails(Date startedDate, Date finishedDate, Float score, Date evaluatedDate, Date reviewedDate, Boolean evaulationNotReviewed, String reviewLink, Integer count, Boolean inProgress)
	{
		return new ParticipantItemDetailsImpl(startedDate, finishedDate, score, evaluatedDate, reviewedDate, evaulationNotReviewed, reviewLink, count, inProgress);
	}
	*/
	
	/**
	 * {@inheritDoc}
	 */
	/*
	public ParticipantItemDetails newParticipantItemDetails(String id, String displayId, String groupTitle, String sortName, ParticipantStatus status, Date evaluatedDate, Date finishedDate, Date reviewed, Float score, Date startedDate, String reviewLink, Boolean inProgress, Boolean suppressDates, Boolean evaulationNotReviewed, Boolean released)
	{
		return new ParticipantItemDetailsImpl(id, sortName, displayId, groupTitle, status, evaluatedDate, finishedDate, reviewed, score, startedDate, reviewLink, inProgress, suppressDates, evaulationNotReviewed, released);
	}
	*/
	
	/**
	 * 
	 */
	/*
	public ParticipantItemDetails newParticipantItemDetails(String id, String displayId, String groupTitle, String sortName, ParticipantStatus status, Integer posts, Date evaluated, Date reviewed, Float score, Date lastPostTime, Date firstPostTime, Boolean evaluationNotReviewed, Boolean released)
	{
		return new ParticipantItemDetailsImpl(id, displayId, groupTitle, sortName, status,  posts, evaluated, reviewed, score, lastPostTime, firstPostTime, evaluationNotReviewed, released);
	}
	*/
	
	/**
	 * {@inheritDoc}
	 */
	public Notes newUserNote(String userId, String notes, Date prevNoteAddedDate, String addedByUserId)
	{
		return new NotesImpl(userId, notes, prevNoteAddedDate, addedByUserId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void registerProvider(GradebookItemProvider provider)
	{
		this.providers.add(provider);		
	}
	
	/**
	 * @param courseMapService the courseMapService to set
	 */
	public void setCourseMapService(CourseMapService courseMapService)
	{
		this.courseMapService = courseMapService;
	}
	
	/**
	 * Dependency: SecurityService.
	 * 
	 * @param service
	 *        The SecurityService.
	 */
	public void setSecurityService(SecurityService service)
	{
		this.securityService = service;
	}
	
	/**
	 * Dependency: SiteService
	 * 
	 * @param service
	 *        The SiteService.
	 */
	public void setSiteService(SiteService service)
	{
		this.siteService = service;
	}

	/**
	 * Dependency: SqlService.
	 * 
	 * @param service
	 *        The SqlService.
	 */
	public void setSqlService(SqlService service)
	{
		sqlService = service;
	}
	
	/**
	 * Set the storage class options.
	 * 
	 * @param options
	 *        The PoolStorage options.
	 */
	@SuppressWarnings(
	{ "unchecked", "rawtypes" })
	public void setStorage(Map options)
	{
		this.storgeOptions = options;
	}
	
	/**
	 * Set the storage option key to use, selecting which GradingStorage to use.
	 * 
	 * @param key
	 *        The storage option key.
	 */
	public void setStorageKey(String key)
	{
		this.storageKey = key;
	}
	
	/**
	 * @param threadLocalManager 
	 * 			The threadLocalManager to set
	 */
	public void setThreadLocalManager(ThreadLocalManager threadLocalManager)
	{
		this.threadLocalManager = threadLocalManager;
	}
	
	/**
	 * Dependency: UserDirectoryService
	 * 
	 * @param service
	 *        The UserDirectoryService.
	 */
	public void setUserDirectoryService(UserDirectoryService service)
	{
		this.userDirectoryService = service;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void unregisterProvider(GradebookItemProvider provider)
	{
		this.providers.remove(provider);		
	}
	
	/**
	 * Check and assign zero for non submitters
	 * 
	 * @param rv	List of participant item details
	 * 
	 * @param gradebookItem		Gradebook item
	 */
	protected void checkAndAssignZeroScoreNonSubmitters(ArrayList<ParticipantItemDetails> rv, GradebookItem gradebookItem)
	{
		if (gradebookItem.getType() != GradebookItemType.offline && gradebookItem != null && (gradebookItem.getDueDate() != null || gradebookItem.getCloseDate() != null))
		{
			Date itemDueDate = null;
			Date itemCloseDate = null;
			Date itemLastSubmitDate = null;
			Date itemUserSpecialAccessDueDate = null;
			Date itemUserSpecialAccessCloseDate = null;
			
			for (ParticipantItemDetails participantItemDetails : rv)
			{
				itemDueDate = gradebookItem.getDueDate();
				itemCloseDate = gradebookItem.getCloseDate();
													
				// user special access dates
				itemUserSpecialAccessDueDate = null;
				itemUserSpecialAccessCloseDate = null;
				
				UserItemSpecialAccess userItemSpecialAccess = participantItemDetails.getUserItemSpecialAccess();
				if (userItemSpecialAccess != null)
				{
					if (userItemSpecialAccess.getOverrideDueDate() != null && userItemSpecialAccess.getOverrideDueDate())
					{
						itemUserSpecialAccessDueDate = userItemSpecialAccess.getDueDate();
					}
					
					if (userItemSpecialAccess.getOverrideAcceptUntilDate() != null && userItemSpecialAccess.getOverrideAcceptUntilDate())
					{
						itemUserSpecialAccessCloseDate = userItemSpecialAccess.getAcceptUntilDate();
					}
				}
				
				itemLastSubmitDate = null;
				
				if (itemCloseDate != null)
				{
					if (itemUserSpecialAccessCloseDate != null)
					{
						itemLastSubmitDate = itemUserSpecialAccessCloseDate;
					}
					else
					{
						itemLastSubmitDate = itemCloseDate;
					}
				}
				else if (itemUserSpecialAccessCloseDate != null)
				{
					itemLastSubmitDate = itemUserSpecialAccessCloseDate;
				}
				else if(itemDueDate != null)
				{
					if (itemUserSpecialAccessDueDate != null)
					{
						itemLastSubmitDate = itemUserSpecialAccessDueDate;
					}
					else
					{
						itemLastSubmitDate = itemDueDate;
					}
				}
				else if (itemUserSpecialAccessDueDate != null)
				{
					itemLastSubmitDate = itemUserSpecialAccessDueDate;
				}
				
				Date now = new Date();
				
				if (now.after(itemLastSubmitDate))
				{
					Float score = null;
					Integer count = null;
					
					score = participantItemDetails.getScore();
					count = participantItemDetails.getCount();
					if (score == null && (count != null && count == 0))
					{
						((ParticipantItemDetailsImpl)participantItemDetails).score = 0.0f;
					}							
				}
			}
		}
	}
	
	/**
	 * Check all the items are associated with category else set the display order
	 * 
	 * @param context	Context
	 * 
	 * @param userId	User id
	 * 
	 * @param gradebookItems	Gradebook items
	 */
	protected void checkAndSetDisplayOrder(String context, String userId, List<GradebookItem> gradebookItems)
	{
		Collections.sort(gradebookItems, new GradebookComparator(Gradebook.GradebookSortType.Category, true));
		
		// set the display order if the item is not associated with category as they are listed last in the list
		int count = 0;
		boolean resetMap = false;
		for (GradebookItem gradebookItem : gradebookItems)
		{
			count++;
			if (gradebookItem.getDisplayOrder() == 0)
			{
				((GradebookItemImpl)gradebookItem).setDisplayOrder(count);
			}
			else if (gradebookItem.getDisplayOrder() != count)
			{
				// reset map if items are not in correct display order as category display order might have changed
				mergeItems(context, userId, gradebookItems, true, true);
				resetMap = true;
				break;
			}				
		}
		
		// if map reset sort and set display order for items that are not associated with category 
		if (resetMap)
		{
			Collections.sort(gradebookItems, new GradebookComparator(Gradebook.GradebookSortType.Category, true));
			count = 0;
			
			for (GradebookItem gradebookItem : gradebookItems)
			{
				count++;
				if (gradebookItem.getDisplayOrder() == 0)
				{
					((GradebookItemImpl)gradebookItem).setDisplayOrder(count);
				}				
			}
		}
	}
	
	/**
	 * Check the security for this user doing this function within this context.
	 * 
	 * @param userId
	 *        the user id.
	 * @param function
	 *        the function.
	 * @param context
	 *        The context.
	 * @param ref
	 *        The entity reference.
	 * @return true if the user has permission, false if not.
	 */
	protected boolean checkSecurity(String userId, String function, String context)
	{
		// check for super user
		if (securityService.isSuperUser(userId)) return true;

		// check for the user / function / context-as-site-authz
		// use the site ref for the security service (used to cache the security calls in the security service)
		String siteRef = siteService.siteReference(context);

		// form the azGroups for a context-as-implemented-by-site
		Collection<String> azGroups = new ArrayList<String>(2);
		azGroups.add(siteRef);
		azGroups.add("!site.helper");

		boolean rv = securityService.unlock(userId, function, siteRef, azGroups);
		return rv;
	}
	
	protected String classAveragePercentCacheKey(String context, String userId)
	{
		return "getUsersGradebookSummary:"+ context +":"+ userId +":classAveragePercent";
	}

	/*
	protected String classTotalPointsCacheKey(String context, String userId)
	{
		return "getUsersGradebookSummary:"+ context +":"+ userId +":classTotalPoints";
	}
	*/
	
	/**
	 * Computes non weighted categories user grade
	 * 
	 * @param userToolGradebookItems	User tool gradebook items
	 * 
	 * @param gradebook					Gradebook
	 * 
	 * @param actualUserGrade			Actual user grade
	 * 
	 * @param gradebookCategories		Gradebook categories
	 */
	protected void computeNonWeightedCategoriesUsergrade(List<ParticipantGradebookItem> userToolGradebookItems, Gradebook gradebook, UserGradeImpl actualUserGrade, List<GradebookCategory> gradebookCategories)
	{
		Map<Integer, UserCategoryGrade> userCategoryGrades = new HashMap<Integer, UserCategoryGrade>();
		for (GradebookCategory gradebookCategory : gradebookCategories)
		{
			UserCategoryGrade userCategoryGrade = new UserCategoryGradeImpl();
			((UserCategoryGradeImpl)userCategoryGrade).setGradebookCategory(gradebookCategory);
			((UserCategoryGradeImpl)userCategoryGrade).setCategoryId(gradebookCategory.getId());
			
			((UserGradeImpl)actualUserGrade).getUserCategoryGrade().add(userCategoryGrade);
			userCategoryGrades.put(gradebookCategory.getId(), userCategoryGrade);
		}
		
		Float score = null;
		Float points = null;
		Float releasedItemsTotalPoints = null;
		Date itemDueDate = null;
		Date itemCloseDate = null;		
		Date itemLastSubmitDate = null;
		UserItemSpecialAccess userItemSpecialAccess = null;
		Date itemUserSpecialAccessDueDate = null;
		Date itemUserSpecialAccessCloseDate = null;
		
		Date now = new Date();
		Map<String, ParticipantItemDetails> userToolGradebookItemsMap = new HashMap<String, ParticipantItemDetails>();
		
		for (ParticipantGradebookItem participantGradebookItem : userToolGradebookItems)
		{
			//get scores points
			if (participantGradebookItem != null)
			{
				GradebookItem gradebookItem = participantGradebookItem.getGradebookItem();
							
				if (gradebookItem != null)
				{
					ParticipantItemDetails participantDetails = participantGradebookItem.getParticipantItemDetails();
					score = null;
					points = null;
					
					userToolGradebookItemsMap.put(gradebookItem.getId(), participantDetails);
					
					// points and score - ignore dates
					if (gradebook.getReleaseGrades().getCode() == Gradebook.ReleaseGrades.All.getCode())
					{
						if (gradebookItem.getPoints() != null)
						{
							points = gradebookItem.getPoints();
							
							if (participantDetails != null)
							{
								if (participantDetails.getScore() != null)
								{
									score = participantDetails.getScore();
								}
								else
								{
									((ParticipantItemDetailsImpl)participantDetails).score = 0.0f;
									score = 0.0f;
								}
								
								if (gradebookItem.getGradebookCategory() != null)
								{
									UserCategoryGrade userCategoryGrade = userCategoryGrades.get(gradebookItem.getGradebookCategory().getId());
									UserCategoryGradeImpl.PointsScore pointsScores = ((UserCategoryGradeImpl)userCategoryGrade).new PointsScore(points, score, gradebookItem.getId());
									((UserCategoryGradeImpl)userCategoryGrade).getPointsScores().add(pointsScores);
								}
							}
						}
					}
					else if (gradebook.getReleaseGrades().getCode() == Gradebook.ReleaseGrades.Released.getCode())
					{
						if (gradebookItem.getPoints() != null)
						{
							points = gradebookItem.getPoints();
							
							if (releasedItemsTotalPoints == null)
							{
								releasedItemsTotalPoints = 0.0f;
							}
							
							releasedItemsTotalPoints += points;
							
							if (participantDetails != null)
							{
								if (participantDetails.getScore() != null)
								{
									score = participantDetails.getScore();
									
									if (gradebookItem.getGradebookCategory() != null)
									{
										UserCategoryGrade userCategoryGrade = userCategoryGrades.get(gradebookItem.getGradebookCategory().getId());
										UserCategoryGradeImpl.PointsScore pointsScores = ((UserCategoryGradeImpl)userCategoryGrade).new PointsScore(points, score, gradebookItem.getId());
										((UserCategoryGradeImpl)userCategoryGrade).getPointsScores().add(pointsScores);
									}
								}
								// don't assign zero's for mneme offline items after closing date
								else if (participantGradebookItem.getGradebookItem().getType() != GradebookItemType.offline)
								{
									// if item is closed and user has no submissions zero assign score to user
									itemDueDate = gradebookItem.getDueDate();
									itemCloseDate = gradebookItem.getCloseDate();
									itemLastSubmitDate = null;
									
									// user special access dates
									itemUserSpecialAccessDueDate = null;
									itemUserSpecialAccessCloseDate = null;
									
									userItemSpecialAccess = participantDetails.getUserItemSpecialAccess();
									if (userItemSpecialAccess != null)
									{
										// if (userItemSpecialAccess.getOverrideDueDate() != null && userItemSpecialAccess.getOverrideDueDate())
										if (userItemSpecialAccess.getDueDate() != null)
										{
											itemUserSpecialAccessDueDate = userItemSpecialAccess.getDueDate();
										}
										
										// if (userItemSpecialAccess.getOverrideHideUntilOpen() != null && userItemSpecialAccess.getOverrideHideUntilOpen())
										if (userItemSpecialAccess.getAcceptUntilDate() != null)
										{
											itemUserSpecialAccessCloseDate = userItemSpecialAccess.getAcceptUntilDate();
										}
									}
									
									itemLastSubmitDate = null;
									
									if (itemCloseDate != null)
									{
										if (itemUserSpecialAccessCloseDate != null)
										{
											itemLastSubmitDate = itemUserSpecialAccessCloseDate;
										}
										else
										{
											itemLastSubmitDate = itemCloseDate;
										}
									}
									else if (itemUserSpecialAccessCloseDate != null)
									{
										itemLastSubmitDate = itemUserSpecialAccessCloseDate;
									}
									else if(itemDueDate != null)
									{
										if (itemUserSpecialAccessDueDate != null)
										{
											itemLastSubmitDate = itemUserSpecialAccessDueDate;
										}
										else
										{
											itemLastSubmitDate = itemDueDate;
										}
									}
									else if (itemUserSpecialAccessDueDate != null)
									{
										itemLastSubmitDate = itemUserSpecialAccessDueDate;
									}
									
									if (itemLastSubmitDate != null && now.after(itemLastSubmitDate) && participantDetails.getCount() != null && participantDetails.getCount() == 0)
									{
										((ParticipantItemDetailsImpl)participantDetails).score = 0.0f;
										score = 0.0f;
										
										if (gradebookItem.getGradebookCategory() != null)
										{
											UserCategoryGrade userCategoryGrade = userCategoryGrades.get(gradebookItem.getGradebookCategory().getId());
											UserCategoryGradeImpl.PointsScore pointsScores = ((UserCategoryGradeImpl)userCategoryGrade).new PointsScore(points, score, gradebookItem.getId());
											((UserCategoryGradeImpl)userCategoryGrade).getPointsScores().add(pointsScores);
										}
									}
								}
							}
						}
					}					
				}
			}
		}
		
		/* calculate category averages also use drop lowest scores if available. Categories have no weights and weight distribution and extra credit category may have weights and weight distribution. 
		 	Extra credit category with weights and weight distribution calculation is different from with out weights and weight distribution*/
				
		UserCategoryGrade userCategoryGrade = null;
		GradebookCategory gradebookCategory = null;
		Float catTotalScore = null;
		Float catTotalpoints = null;
		Float catAverageScorePercent = null;
		Float extraCreditCatTotalScore = null;
		Float extraCreditCatTotalPoints = null;
		Float extraCreditCatWeight = null;
		Float extraCreditCatWeightDistEquallyTotalScoresPercent = null;
		WeightDistribution extraCreditCatWeightDistribution = null;
		Float catWeight = null;
		int dropNumLowestScores = 0;
		boolean dropScores = false;
		boolean keepOnlyHighest = false;

		for (Map.Entry<Integer, UserCategoryGrade> entry : userCategoryGrades.entrySet()) 
		{
			catTotalScore = null;
			catTotalpoints = null;
			catAverageScorePercent = null;
			catWeight = null;
			dropNumLowestScores = 0;
			dropScores = false;
			keepOnlyHighest = false;
			
			userCategoryGrade = entry.getValue();
			gradebookCategory = userCategoryGrade.getGradebookCategory();
			
			if (gradebook.isDropLowestScore())
			{
				dropNumLowestScores = gradebookCategory.getDropNumberLowestScores();
			}
			
			List<PointsScore> pointsScores = ((UserCategoryGradeImpl)userCategoryGrade).getPointsScores();
			
			// check for drop lowest scoresdropNumLowestScores
			if (dropNumLowestScores > 0)
			{
				if (pointsScores.size() > dropNumLowestScores)
				{
					dropScores = true;
				}
				else
				{
					// keep the highest and drop other lowest scores
					keepOnlyHighest = true;
				}
			}
			
			if (pointsScores.size() > 0)
			{					
				if (gradebookCategory.getWeight() == null && gradebookCategory.getWeightDistribution() == null)
				{
					//drop lowest scores(lowest scores are the scores the percentage of score and points is high i.e. score/points number is highest.
					if (dropScores)
					{
						dropLowestScores(userToolGradebookItemsMap, dropNumLowestScores, pointsScores);
					}
					else if (keepOnlyHighest)
					{
						dropLowestScores(userToolGradebookItemsMap, pointsScores);
					}
												
					for (PointsScore pointsScore : pointsScores)
					{
						if ((pointsScore.points != null) && (pointsScore.score != null))
						{
							if (catTotalpoints == null)
							{
								catTotalpoints = 0.0f;
							}
							catTotalpoints += pointsScore.points;
							
							if (catTotalScore == null)
							{
								catTotalScore = 0.0f;
							}
							catTotalScore += pointsScore.score;
						}
					}
					
					if (catTotalScore != null && catTotalpoints != null)
					{
						// Don't add extra credit points to total points calculate percent and add to final percent 
						if (gradebookCategory.isExtraCredit())
						{
							if (extraCreditCatTotalScore == null)
							{
								extraCreditCatTotalScore = 0.0f;
							}
							extraCreditCatTotalScore = extraCreditCatTotalScore + catTotalScore;
							
							if (extraCreditCatTotalPoints == null)
							{
								extraCreditCatTotalPoints = 0.0f;
							}
							extraCreditCatTotalPoints = extraCreditCatTotalPoints + catTotalpoints;
							
							// set user category grade with points, score, average
							((UserCategoryGradeImpl)userCategoryGrade).setPoints(catTotalpoints);
							((UserCategoryGradeImpl)userCategoryGrade).setScore(catTotalScore);
							((UserCategoryGradeImpl)userCategoryGrade).setAveragePercent(null);							
						}
						else
						{
							if (actualUserGrade.getPoints() == null)
							{
								actualUserGrade.setPoints(0.0f);
							}
							actualUserGrade.setPoints(actualUserGrade.getPoints() + catTotalpoints);
							
							if (actualUserGrade.getScore() == null)
							{
								actualUserGrade.setScore(0.0f);
							}
							actualUserGrade.setScore(actualUserGrade.getScore() + catTotalScore);
							
							// set user category grade with points, score, average
							((UserCategoryGradeImpl)userCategoryGrade).setPoints(catTotalpoints);
							((UserCategoryGradeImpl)userCategoryGrade).setScore(catTotalScore);
							((UserCategoryGradeImpl)userCategoryGrade).setAveragePercent(null);
						}
					}
				}
				else if (gradebookCategory.isExtraCredit())
				{
					// extra credit with weight and weight distribution
					if (gradebookCategory.getWeight() != null && gradebookCategory.getWeightDistribution() != null && gradebookCategory.getWeightDistribution() == WeightDistribution.Points)
					{
						extraCreditCatWeight = gradebookCategory.getWeight();
						extraCreditCatWeightDistribution = gradebookCategory.getWeightDistribution();
					
						if (dropScores)
						{
							dropLowestScores(userToolGradebookItemsMap, dropNumLowestScores, pointsScores);
						}
						else if (keepOnlyHighest)
						{
							dropLowestScores(userToolGradebookItemsMap, pointsScores);
						}
													
						for (PointsScore pointsScore : pointsScores)
						{
							if ((pointsScore.points != null) && (pointsScore.score != null))
							{
								if (catTotalpoints == null)
								{
									catTotalpoints = 0.0f;
								}
								catTotalpoints += pointsScore.points;
								
								if (catTotalScore == null)
								{
									catTotalScore = 0.0f;
								}
								catTotalScore += pointsScore.score;
							}
						}
						
						if (catTotalScore != null && catTotalpoints != null)
						{
							if (extraCreditCatTotalScore == null)
							{
								extraCreditCatTotalScore = 0.0f;
							}
							extraCreditCatTotalScore = extraCreditCatTotalScore + catTotalScore;
							
							if (extraCreditCatTotalPoints == null)
							{
								extraCreditCatTotalPoints = 0.0f;
							}
							extraCreditCatTotalPoints = extraCreditCatTotalPoints + catTotalpoints;
							
							// set user category grade with points, score, average
							((UserCategoryGradeImpl)userCategoryGrade).setPoints(catTotalpoints);
							((UserCategoryGradeImpl)userCategoryGrade).setScore(catTotalScore);
							((UserCategoryGradeImpl)userCategoryGrade).setAveragePercent(null);
						}
					}
					else if (gradebookCategory.getWeight() != null && gradebookCategory.getWeightDistribution() != null && gradebookCategory.getWeightDistribution() == WeightDistribution.Equally)
					{
						extraCreditCatWeight = gradebookCategory.getWeight();
						extraCreditCatWeightDistribution = gradebookCategory.getWeightDistribution();
						
						// check for drop lowest scores and drop lowest scores. Drop the lowest of average distribution						
						catWeight = gradebookCategory.getWeight();
						Float averageDistibution = roundToTwoDecimals(catWeight/pointsScores.size());
						
						// Drop the lowest catAverageScorePercent scores
						if (dropScores)
						{
							dropLowestScores(userToolGradebookItemsMap, dropNumLowestScores, pointsScores, averageDistibution);
						}
						else if (keepOnlyHighest)
						{
							dropLowestScores(userToolGradebookItemsMap, pointsScores);
						}
						
						// reset the average distribution if dropped scores
						if (dropScores || keepOnlyHighest)
						{
							averageDistibution = roundToTwoDecimals(catWeight/pointsScores.size());
						}
						
						Float extraCreditCatTotalpoints = null;
						Float extraCreditcatTotalScore = null;
						for (PointsScore pointsScore : pointsScores)
						{
							catAverageScorePercent = null;
							
							if ((pointsScore.points != null) && (pointsScore.score != null))
							{
								if (extraCreditCatTotalpoints == null)
								{
									extraCreditCatTotalpoints = 0.0f;
								}
								extraCreditCatTotalpoints += pointsScore.points;
								
								if (extraCreditcatTotalScore == null)
								{
									extraCreditcatTotalScore = 0.0f;
								}
								extraCreditcatTotalScore += pointsScore.score;
								
								if (extraCreditCatWeightDistEquallyTotalScoresPercent == null)
								{
									extraCreditCatWeightDistEquallyTotalScoresPercent = 0.0f;
								}
								
								// calculate average for each score as per average distribution
								extraCreditCatWeightDistEquallyTotalScoresPercent = extraCreditCatWeightDistEquallyTotalScoresPercent + (((pointsScore.score/pointsScore.points) * 100) * (averageDistibution/100));
							}
						}
						
						// set user category grade with points, score, average
						((UserCategoryGradeImpl)userCategoryGrade).setPoints(extraCreditCatTotalpoints);
						((UserCategoryGradeImpl)userCategoryGrade).setScore(extraCreditcatTotalScore);
						((UserCategoryGradeImpl)userCategoryGrade).setAveragePercent(extraCreditCatWeightDistEquallyTotalScoresPercent);
					}
					
				}
			}
		}
		
		Float averagePercent = null;
		if (actualUserGrade.getScore() != null && actualUserGrade.getPoints() != null && actualUserGrade.getPoints() > 0)
		{
			// extra credit with weights
			if (extraCreditCatWeight != null && extraCreditCatWeight > 0 && extraCreditCatWeightDistribution != null && 
											(extraCreditCatWeightDistribution == WeightDistribution.Points || extraCreditCatWeightDistribution == WeightDistribution.Equally))
			{
				// boost by number and points/percent
				if (gradebook.getBoostUserGradesBy() != null && gradebook.getBoostUserGradesBy() > 0 && (gradebook.getBoostUserGradesType() == BoostUserGradesType.points || gradebook.getBoostUserGradesType() == BoostUserGradesType.percent))
				{
					if (gradebook.getBoostUserGradesType() == BoostUserGradesType.points)
					{
						// add boosted points to user score
						Float boostedScore = actualUserGrade.getScore() + gradebook.getBoostUserGradesBy();
						actualUserGrade.setScore(roundToTwoDecimals(boostedScore));
						
						averagePercent = (actualUserGrade.getScore() / actualUserGrade.getPoints()) * 100;
						averagePercent = roundToTwoDecimals(averagePercent);
					}
					else if (gradebook.getBoostUserGradesType() == BoostUserGradesType.percent)
					{
						averagePercent = (actualUserGrade.getScore() / actualUserGrade.getPoints()) * 100;
						averagePercent = averagePercent + gradebook.getBoostUserGradesBy();
						averagePercent = roundToTwoDecimals(averagePercent);
						
						// add boosted points to user score
						Float boostedScore = (averagePercent * actualUserGrade.getPoints()) /100;
						actualUserGrade.setScore(roundToTwoDecimals(boostedScore));
					}
				}
				else
				{
					averagePercent = ((actualUserGrade.getScore()) / actualUserGrade.getPoints()) * 100;
					averagePercent = roundToTwoDecimals(averagePercent);					
				}
				
				if (extraCreditCatWeightDistribution == WeightDistribution.Points)
				{
					//averagePercent = ((actualUserGrade.getScore()) / actualUserGrade.getPoints()) * 100;
					//averagePercent = Math.round(averagePercent * 100.0f) / 100.0f;
					
					if (extraCreditCatTotalPoints != null && extraCreditCatTotalPoints > 0 && extraCreditCatTotalScore != null && extraCreditCatTotalScore > 0)
					{
						Float extraCreditAveragePercent = ((extraCreditCatTotalScore/extraCreditCatTotalPoints) * 100) * (extraCreditCatWeight / 100.0f);
						extraCreditAveragePercent = roundToTwoDecimals(extraCreditAveragePercent);
						
						averagePercent = averagePercent + extraCreditAveragePercent;
						
						// extra credit percent
						actualUserGrade.setExtraCreditPercent(extraCreditAveragePercent);
						actualUserGrade.setExtraCreditScore(extraCreditCatTotalScore);
					}
				}
				else if (extraCreditCatWeightDistribution == WeightDistribution.Equally)
				{
					//averagePercent = ((actualUserGrade.getScore()) / actualUserGrade.getPoints()) * 100;
					//averagePercent = Math.round(averagePercent * 100.0f) / 100.0f;
					
					if (extraCreditCatWeightDistEquallyTotalScoresPercent != null && extraCreditCatWeightDistEquallyTotalScoresPercent > 0)
					{
						extraCreditCatWeightDistEquallyTotalScoresPercent = roundToTwoDecimals(extraCreditCatWeightDistEquallyTotalScoresPercent);
						averagePercent = averagePercent + extraCreditCatWeightDistEquallyTotalScoresPercent;
						
						// extra credit percent
						actualUserGrade.setExtraCreditPercent(extraCreditCatWeightDistEquallyTotalScoresPercent);
						actualUserGrade.setExtraCreditScore(extraCreditCatTotalScore);
					}
				}
			}
			else
			{
				// boost by number and points/percent
				if (gradebook.getBoostUserGradesBy() != null && gradebook.getBoostUserGradesBy() > 0 && (gradebook.getBoostUserGradesType() == BoostUserGradesType.points || gradebook.getBoostUserGradesType() == BoostUserGradesType.percent))
				{
					if (gradebook.getBoostUserGradesType() == BoostUserGradesType.points)
					{
						// add boosted points to user score
						Float boostedScore = actualUserGrade.getScore() + gradebook.getBoostUserGradesBy();
						actualUserGrade.setScore(roundToTwoDecimals(boostedScore));
						
						averagePercent = (actualUserGrade.getScore() / actualUserGrade.getPoints()) * 100;
						averagePercent = roundToTwoDecimals(averagePercent);
					}
					else if (gradebook.getBoostUserGradesType() == BoostUserGradesType.percent)
					{
						averagePercent = (actualUserGrade.getScore() / actualUserGrade.getPoints()) * 100;
						averagePercent = averagePercent + gradebook.getBoostUserGradesBy();
						averagePercent = roundToTwoDecimals(averagePercent);
						
						// add boosted points to user score
						Float boostedScore = (averagePercent * actualUserGrade.getPoints()) /100;
						actualUserGrade.setScore(roundToTwoDecimals(boostedScore));
					}
				}
				else
				{
					averagePercent = (actualUserGrade.getScore() / actualUserGrade.getPoints()) * 100;
					averagePercent = roundToTwoDecimals(averagePercent);
				}
				
				// add extra credit with out weights if any
				if (extraCreditCatTotalScore != null && extraCreditCatTotalScore > 0 && extraCreditCatTotalPoints != null && extraCreditCatTotalPoints > 0)
				{
					/*
					Float extraCreditCatPercent;
					
					extraCreditCatPercent = (extraCreditCatTotalPoints/actualUserGrade.getPoints()) * 100.0f;
					
					Float extraCreditAveragePercent = (extraCreditCatPercent / 100.0f) * extraCreditCatTotalScore;
					extraCreditAveragePercent = roundToTwoDecimals(extraCreditAveragePercent);
					*/
					Float extraCreditAveragePercent;
					
					if (actualUserGrade.getPoints() != null && actualUserGrade.getPoints() > 0)
					{
						extraCreditAveragePercent = (extraCreditCatTotalScore/actualUserGrade.getPoints()) * 100.0f;
					}
					else
					{
						extraCreditAveragePercent = (extraCreditCatTotalScore/extraCreditCatTotalPoints) * 100.0f;
					}
					
					averagePercent = averagePercent + extraCreditAveragePercent;
					
					// extra credit percent
					actualUserGrade.setExtraCreditPercent(extraCreditAveragePercent);
					actualUserGrade.setExtraCreditScore(extraCreditCatTotalScore);
				}
			}
			
			// extra credit points and scores are not included
			actualUserGrade.setAveragePercent(averagePercent);
			actualUserGrade.setPoints(actualUserGrade.getPoints());
			actualUserGrade.setScore(actualUserGrade.getScore());
		}
		else
		{
			// extra credit with weights
			if (extraCreditCatWeight != null && extraCreditCatWeight > 0 && extraCreditCatWeightDistribution != null && 
											(extraCreditCatWeightDistribution == WeightDistribution.Points || extraCreditCatWeightDistribution == WeightDistribution.Equally))
			{
				if (extraCreditCatWeightDistribution == WeightDistribution.Points)
				{
					//averagePercent = ((actualUserGrade.getScore()) / actualUserGrade.getPoints()) * 100;
					//averagePercent = Math.round(averagePercent * 100.0f) / 100.0f;
					
					if (extraCreditCatTotalPoints != null && extraCreditCatTotalPoints > 0 && extraCreditCatTotalScore != null && extraCreditCatTotalScore > 0)
					{
						Float extraCreditAveragePercent = ((extraCreditCatTotalScore/extraCreditCatTotalPoints) * 100) * (extraCreditCatWeight / 100.0f);
						extraCreditAveragePercent = roundToTwoDecimals(extraCreditAveragePercent);
						
						averagePercent = extraCreditAveragePercent;
						
						// extra credit percent
						actualUserGrade.setExtraCreditPercent(extraCreditAveragePercent);
						actualUserGrade.setExtraCreditScore(extraCreditCatTotalScore);
						
						// extra credit points and scores are not included
						actualUserGrade.setAveragePercent(averagePercent);
					}
				}
				else if (extraCreditCatWeightDistribution == WeightDistribution.Equally)
				{
					//averagePercent = ((actualUserGrade.getScore()) / actualUserGrade.getPoints()) * 100;
					//averagePercent = Math.round(averagePercent * 100.0f) / 100.0f;
					
					if (extraCreditCatWeightDistEquallyTotalScoresPercent != null && extraCreditCatWeightDistEquallyTotalScoresPercent > 0)
					{
						extraCreditCatWeightDistEquallyTotalScoresPercent = roundToTwoDecimals(extraCreditCatWeightDistEquallyTotalScoresPercent);
						averagePercent = extraCreditCatWeightDistEquallyTotalScoresPercent;
						
						// extra credit percent
						actualUserGrade.setExtraCreditPercent(extraCreditCatWeightDistEquallyTotalScoresPercent);
						actualUserGrade.setExtraCreditScore(extraCreditCatTotalScore);
						
						// extra credit points and scores are not included
						actualUserGrade.setAveragePercent(averagePercent);
					}
				}		
			}
			// add extra credit with out weights if any
			else if (extraCreditCatTotalScore != null && extraCreditCatTotalScore > 0 && extraCreditCatTotalPoints != null && extraCreditCatTotalPoints > 0)
			{
				// add extra credit with out weights if any
				//Float extraCreditCatPercent;
				Float extraCreditAveragePercent = null;
				
				if (releasedItemsTotalPoints != null && releasedItemsTotalPoints > 0)
				{
					//extraCreditCatPercent = (extraCreditCatTotalPoints/releasedItemsTotalPoints) * 100.0f;
					if (actualUserGrade.getPoints() != null && actualUserGrade.getPoints() > 0)
					{
						extraCreditAveragePercent = (extraCreditCatTotalScore / releasedItemsTotalPoints) * 100.0f;
						extraCreditAveragePercent = roundToTwoDecimals(extraCreditAveragePercent);
					}
					else
					{
						extraCreditAveragePercent = (extraCreditCatTotalScore / extraCreditCatTotalPoints) * 100.0f;
						extraCreditAveragePercent = roundToTwoDecimals(extraCreditAveragePercent);
					}
					
				}
				else
				{
					//extraCreditCatPercent = (extraCreditCatTotalPoints/extraCreditCatTotalPoints) * 100.0f;
					
					extraCreditAveragePercent = (extraCreditCatTotalScore / extraCreditCatTotalPoints) * 100.0f;
					extraCreditAveragePercent = roundToTwoDecimals(extraCreditAveragePercent);
				}
				
				averagePercent = extraCreditAveragePercent;
				
				// extra credit percent
				actualUserGrade.setExtraCreditPercent(extraCreditAveragePercent);
				actualUserGrade.setExtraCreditScore(extraCreditCatTotalScore);
				
				// extra credit points and scores are not included
				actualUserGrade.setAveragePercent(averagePercent);
			}
		}
	}

	/**
	 * computes user grade
	 * 
	 * @param userToolGradebookItems	User gradebook items with mapped category
	 * 
	 * @param gradebook			Gradebook
	 * 
	 * @param actualUserGrade	Actual user grade
	 */
	protected void computeUsergrade(List<ParticipantGradebookItem> userToolGradebookItems, Gradebook gradebook, UserGradeImpl actualUserGrade)
	{

		/* calculate as per gradebook categories weight% and distribution. Use same method to calculate user grade. User grade is also calculated in getUsersGradebookSummary(....)
		 * 1. If gradebook has no categories use points and scores of each item
		 * 2. If gradebook has categories then check for weight% and distribution. 
		 * 		If no weight% and distribution use points of mapped and unmapped items. Same as 1
		 * 		If weight% and no distribution - use points
		 * 		If weight% and distribution(points/equally) - calculate as per weight% and distribution
		 * 			Distribution - points - Example
		 * 				CAT A - weight% 40
		 * 				
		 * 				Item A 5 points, Item B 10 points, Item C 15 points, Item D 5 points
		 * 				Item A 4 score, Item B 10 score, Item C 12 score, Item D 3 score
		 * 				
		 * 				Total points = Item A + Item B + Item C + Item D = 5 + 10 + 15 + 5 = 35
		 * 				Total score =  Item A + Item B + Item C + Item D = 4 + 10 + 12 + 3 = 29
		 * 				
		 * 				score % = ((total score/total points) * 100 ) * (weight/100)
		 * 						= ((29/35) * 100 ) * (40/100)
		 * 						= 82.86 * (40/100) 
		 * 						= 33.14%
		 *
		 * 			Distribution - equally - Example
		 * 				CAT A - weight% 40
		 * 				Distribute weight% by number of items to get weight percent of each item and calculate each item score and add all
		 * 
		 * 				Item A 5 points, Item B 10 points, Item C 15 points, Item D 5 points
		 * 				Item A 4 score, Item B 10 score, Item C 12 score, Item D 3 score
		 * 				
		 * 				As there are 4 items 40% / 4 = 10% each item
		 * 				distributed equal weight - Item A 10 10%, Item B 10%, Item C 10%, Item D 10%
		 * 
		 * 				score % of each item = ((score/points) * 100 ) * (equal weight/100)
		 * 				score A = ((4/5) * 100 ) * (10/100) = 8%
		 * 				score B = ((10/10) * 100 ) * (10/100) = 10%
		 * 				score C = ((12/15) * 100 ) * (10/100) = 8%
		 * 				score D = ((3/5) * 100 ) * (10/100) = 6%
		 * 				score % of category = 8 + 10 + 8 + 6 = 32%
		 * 
		 * 3. Use drop number of lowest score if category has one
		 * 
		 * 
		 * 	EXTRCREDIT CALCULATION with points
		 	Example 1: 

			 Total points possible for a course is 100 (without extra credit). 
			 Student earns 8 in extra credit, and 84 out of 100 from other categories required work. 
			
			 The grade WITHOUT extra credit is 84%. 
			 The extra credit is 8 * 100 / 100 = 8%. 
			
			 Grade to Date WITH extra credit is 92% / A 
			
			Example 2: 
			
			 Total points possible for the course is 450 (without extra credit). 
			 Student earns 25 points in extra credit, and 425 in other categories required work. 
			
			 The grade WITHOUT extra credit is 425 out of 450 or 94.4%. 
			 The extra credit is 25 * 100 / 450 = 5.55% 
			
			 Grade to Date WITH extra credit is 99.95% 
		*/
		boolean categoryWeightsSet = false;
		
		List<GradebookCategory> gradebookCategories = gradebook.getGradebookCategories();
		if (gradebookCategories != null && gradebookCategories.size() > 0)
		{
			for (GradebookCategory gradebookCategory : gradebookCategories)
			{
				if (gradebookCategory.getWeight() != null && gradebookCategory.getWeightDistribution() != null && !gradebookCategory.isExtraCredit())
				{
					categoryWeightsSet = true;
				}
			}			
		}
		
		if (categoryWeightsSet)
		{
			/* weighted categories(extra credit category not included) scores and grade computation. Extra credit category may have weights */
			// calculate as per weights and weigh distribution. Include Extra credit after calculation of category score average 
			computeWeightedCategoriesUsergrade(userToolGradebookItems, gradebook, actualUserGrade, gradebookCategories);
					
		}
		else
		{
			
			/* non weighted categories(extra credit category not included) scores and grade computation. Extra credit category may have weights */
			// calculate as per weights and weigh distribution. Include Extra credit after calculation of category score average 
			computeNonWeightedCategoriesUsergrade(userToolGradebookItems, gradebook, actualUserGrade, gradebookCategories);
		}
		
		// round off average
		if (actualUserGrade != null && actualUserGrade.getAveragePercent() != null && actualUserGrade.getAveragePercent() > 0)
		{
			Float averagePercent = actualUserGrade.getAveragePercent();
			averagePercent = roundToTwoDecimals(averagePercent);
			
			actualUserGrade.setAveragePercent(averagePercent);
		}
		
		// round off score
		if (actualUserGrade != null && actualUserGrade.getScore() != null)
		{
			Float userScore = actualUserGrade.getScore();
			
			userScore = roundToTwoDecimals(userScore);
			
			actualUserGrade.setScore(userScore);			
		}
		
		// map user average to grading scale
		mapUsergradeToGradingScale(gradebook, actualUserGrade);
	}

	/**
	 * Computes weighted categories user grade
	 * 
	 * @param userToolGradebookItems	User tool gradebook items
	 * 
	 * @param gradebook					Gradebook
	 * 
	 * @param actualUserGrade			Actual user grade
	 * 
	 * @param gradebookCategories		Gradebook categories
	 */
	protected void computeWeightedCategoriesUsergrade(List<ParticipantGradebookItem> userToolGradebookItems, Gradebook gradebook, UserGradeImpl actualUserGrade, List<GradebookCategory> gradebookCategories)
	{
		Map<Integer, UserCategoryGrade> userCategoryGrades = new HashMap<Integer, UserCategoryGrade>();
		for (GradebookCategory gradebookCategory : gradebookCategories)
		{
			UserCategoryGrade userCategoryGrade = new UserCategoryGradeImpl();
			((UserCategoryGradeImpl)userCategoryGrade).setGradebookCategory(gradebookCategory);
			((UserCategoryGradeImpl)userCategoryGrade).setCategoryId(gradebookCategory.getId());
			
			((UserGradeImpl)actualUserGrade).getUserCategoryGrade().add(userCategoryGrade);
			userCategoryGrades.put(gradebookCategory.getId(), userCategoryGrade);
		}
		
		Float score = null;
		Float points = null;				
		Date itemDueDate = null;
		Date itemCloseDate = null;		
		Date itemLastSubmitDate = null;
		UserItemSpecialAccess userItemSpecialAccess = null;
		Date itemUserSpecialAccessDueDate = null;
		Date itemUserSpecialAccessCloseDate = null;
		
		Date now = new Date();
		
		Map<String, ParticipantItemDetails> userToolGradebookItemsMap = new HashMap<String, ParticipantItemDetails>();
		
		for (ParticipantGradebookItem participantGradebookItem : userToolGradebookItems)
		{
			//get scores points
			if (participantGradebookItem != null)
			{
				GradebookItem gradebookItem = participantGradebookItem.getGradebookItem();
							
				if (gradebookItem != null)
				{
					ParticipantItemDetails participantDetails = participantGradebookItem.getParticipantItemDetails();
					score = null;
					points = null;
					
					userToolGradebookItemsMap.put(gradebookItem.getId(), participantDetails);
					
					// points and score - ignore dates
					if (gradebook.getReleaseGrades().getCode() == Gradebook.ReleaseGrades.All.getCode())
					{
						if (gradebookItem.getPoints() != null)
						{
							points = gradebookItem.getPoints();
							
							if (participantDetails != null && participantDetails.getScore() != null)
							{
								score = participantDetails.getScore();
							}
							else
							{
								((ParticipantItemDetailsImpl)participantDetails).score = 0.0f;
								score = 0.0f;
							}
							
							if (gradebookItem.getGradebookCategory() != null)
							{
								UserCategoryGrade userCategoryGrade = userCategoryGrades.get(gradebookItem.getGradebookCategory().getId());
								UserCategoryGradeImpl.PointsScore pointsScores = ((UserCategoryGradeImpl)userCategoryGrade).new PointsScore(points, score, gradebookItem.getId());
								((UserCategoryGradeImpl)userCategoryGrade).getPointsScores().add(pointsScores);
							}
						}
					}
					else if (gradebook.getReleaseGrades().getCode() == Gradebook.ReleaseGrades.Released.getCode())
					{
						if (gradebookItem.getPoints() != null)
						{
							points = gradebookItem.getPoints();
							
							if (participantDetails != null)
							{
								if (participantDetails.getScore() != null)
								{
									score = participantDetails.getScore();
									
									if (gradebookItem.getGradebookCategory() != null)
									{
										UserCategoryGrade userCategoryGrade = userCategoryGrades.get(gradebookItem.getGradebookCategory().getId());
										UserCategoryGradeImpl.PointsScore pointsScores = ((UserCategoryGradeImpl)userCategoryGrade).new PointsScore(points, score, gradebookItem.getId());
										((UserCategoryGradeImpl)userCategoryGrade).getPointsScores().add(pointsScores);
									}
								}
								// don't assign zero's for mneme offline items after closing date
								else if (participantGradebookItem.getGradebookItem().getType() != GradebookItemType.offline)
								{
									// if item is closed and user has no submissions zero assign score to user
									itemDueDate = gradebookItem.getDueDate();
									itemCloseDate = gradebookItem.getCloseDate();
									itemLastSubmitDate = null;
									
									// user special access dates
									itemUserSpecialAccessDueDate = null;
									itemUserSpecialAccessCloseDate = null;
									
									userItemSpecialAccess = participantDetails.getUserItemSpecialAccess();
									if (userItemSpecialAccess != null)
									{
										if (userItemSpecialAccess.getOverrideDueDate() != null && userItemSpecialAccess.getOverrideDueDate())
										{
											itemUserSpecialAccessDueDate = userItemSpecialAccess.getDueDate();
										}
										
										if (userItemSpecialAccess.getOverrideHideUntilOpen() != null && userItemSpecialAccess.getOverrideHideUntilOpen())
										{
											itemUserSpecialAccessCloseDate = userItemSpecialAccess.getAcceptUntilDate();
										}
									}
									
									itemLastSubmitDate = null;
									
									if (itemCloseDate != null)
									{
										if (itemUserSpecialAccessCloseDate != null)
										{
											itemLastSubmitDate = itemUserSpecialAccessCloseDate;
										}
										else
										{
											itemLastSubmitDate = itemCloseDate;
										}
									}
									else if (itemUserSpecialAccessCloseDate != null)
									{
										itemLastSubmitDate = itemUserSpecialAccessCloseDate;
									}
									else if(itemDueDate != null)
									{
										if (itemUserSpecialAccessDueDate != null)
										{
											itemLastSubmitDate = itemUserSpecialAccessDueDate;
										}
										else
										{
											itemLastSubmitDate = itemDueDate;
										}
									}
									else if (itemUserSpecialAccessDueDate != null)
									{
										itemLastSubmitDate = itemUserSpecialAccessDueDate;
									}
									
									if (itemLastSubmitDate != null && now.after(itemLastSubmitDate) && participantDetails.getCount() != null && participantDetails.getCount() == 0)
									{
										((ParticipantItemDetailsImpl)participantDetails).score = 0.0f;
										score = 0.0f;
										
										if (gradebookItem.getGradebookCategory() != null)
										{
											UserCategoryGrade userCategoryGrade = userCategoryGrades.get(gradebookItem.getGradebookCategory().getId());
											UserCategoryGradeImpl.PointsScore pointsScores = ((UserCategoryGradeImpl)userCategoryGrade).new PointsScore(points, score, gradebookItem.getId());
											((UserCategoryGradeImpl)userCategoryGrade).getPointsScores().add(pointsScores);
										}
									}
								}
							}
						}
					}					
				}
			}
		}
		
		/* calculate category averages also use drop lowest scores if available. Categories have weights and weight distribution and extra credit category may have weights and weight distribution. 
	 	Extra credit category with weights and weight distribution calculation is different from with out weights and weight distribution*/		
		UserCategoryGrade userCategoryGrade = null;
		GradebookCategory gradebookCategory = null;
		Float catTotalScore = null;
		Float catTotalpoints = null;
		Float catAverageScorePercent = null;
		Float catWeight = null;
		Float extraCreditCatTotalScore = null;
		Float extraCreditCatTotalPoints = null;
		Float extraCreditCatWeight = null;
		Float extraCreditCatWeightDistEquallyTotalScoresPercent = null;
		WeightDistribution extraCreditCatWeightDistribution = null;
		int dropNumLowestScores = 0;
		boolean dropScores = false;
		boolean keepOnlyHighest = false;

		for (Map.Entry<Integer, UserCategoryGrade> entry : userCategoryGrades.entrySet()) 
		{
			catTotalScore = null;
			catTotalpoints = null;
			catAverageScorePercent = null;
			catWeight = null;
			dropNumLowestScores = 0;
			dropScores = false;
			keepOnlyHighest = false;
			
			userCategoryGrade = entry.getValue();
			gradebookCategory = userCategoryGrade.getGradebookCategory();
			
			if (gradebook.isDropLowestScore())
			{
				dropNumLowestScores = gradebookCategory.getDropNumberLowestScores();
			}
			
			//drop lowest scores(lowest scores are the scores the percentage of score and points is high i.e. score/points number is highest.
			List<PointsScore> pointsScores = ((UserCategoryGradeImpl)userCategoryGrade).getPointsScores();
			
			// check for drop lowest scoresdropNumLowestScores
			if (dropNumLowestScores > 0)
			{
				if (pointsScores.size() > dropNumLowestScores)
				{
					dropScores = true;
				}
				else
				{
					// keep the highest and drop other lowest scores
					keepOnlyHighest = true;
				}
			}
			
			if (pointsScores.size() > 0)
			{					
				if (!gradebookCategory.isExtraCredit() && (gradebookCategory.getWeight() != null && gradebookCategory.getWeightDistribution() != null && gradebookCategory.getWeightDistribution() == WeightDistribution.Points))
				{
					if (dropScores)
					{
						dropLowestScores(userToolGradebookItemsMap, dropNumLowestScores, pointsScores);						
					}
					else if (keepOnlyHighest)
					{
						dropLowestScores(userToolGradebookItemsMap, pointsScores);
					}
												
					for (PointsScore pointsScore : pointsScores)
					{
						if ((pointsScore.points != null) && (pointsScore.score != null))
						{
							if (catTotalpoints == null)
							{
								catTotalpoints = 0.0f;
							}
							catTotalpoints += pointsScore.points;
							
							if (catTotalScore == null)
							{
								catTotalScore = 0.0f;
							}
							catTotalScore += pointsScore.score;
						}
					}
					
					if (catTotalScore != null && catTotalpoints != null)
					{
						// Don't add extra credit points to total points
						if (!gradebookCategory.isExtraCredit())
						{
							if (actualUserGrade.getPoints() == null)
							{
								actualUserGrade.setPoints(0.0f);
							}
							actualUserGrade.setPoints(actualUserGrade.getPoints() + catTotalpoints);
							((UserCategoryGradeImpl)userCategoryGrade).setPoints(catTotalpoints);
						}
						
						if (actualUserGrade.getScore() == null)
						{
							actualUserGrade.setScore(0.0f);
						}
						actualUserGrade.setScore(actualUserGrade.getScore() + catTotalScore);
						
						// average percent
						if (gradebookCategory.getWeight() != null)
						{
							
							// grades boost by
							if (gradebook.getBoostUserGradesBy() != null && gradebook.getBoostUserGradesBy() > 0 && gradebook.getBoostUserGradesType() != null && gradebook.getBoostUserGradesType() == BoostUserGradesType.points)
							{
								Float catBoostByPoints = gradebook.getBoostUserGradesBy();
								
								catBoostByPoints = catBoostByPoints * (gradebookCategory.getWeight()/100);
								
								catAverageScorePercent  = (((catTotalScore + catBoostByPoints)/catTotalpoints) * 100 ) * (gradebookCategory.getWeight()/100);
								
								// add boost by score to user score
								actualUserGrade.setScore(actualUserGrade.getScore() + catBoostByPoints);								
							}
							else
							{
								catAverageScorePercent  = ((catTotalScore/catTotalpoints) * 100 ) * (gradebookCategory.getWeight()/100);
							}
						}
						else
						{
							// will not go here as weight null check is already done
							catAverageScorePercent  = ((catTotalScore/catTotalpoints) * 100 );
						}
						
						if (actualUserGrade.getAveragePercent() == null)
						{
							actualUserGrade.setAveragePercent(0.0f);
						}
						actualUserGrade.setAveragePercent(actualUserGrade.getAveragePercent() + catAverageScorePercent);
						
						// set user category grade with points, score, average
						((UserCategoryGradeImpl)userCategoryGrade).setPoints(catTotalpoints);
						((UserCategoryGradeImpl)userCategoryGrade).setScore(catTotalScore);
						((UserCategoryGradeImpl)userCategoryGrade).setAveragePercent(catAverageScorePercent);
					}
				}
				else if (!gradebookCategory.isExtraCredit() && gradebookCategory.getWeight() != null && gradebookCategory.getWeightDistribution() != null && gradebookCategory.getWeightDistribution() == WeightDistribution.Equally)
				{
					/* check for drop lowest scores and drop lowest scores. Drop the lowest of average distribution */
					
					catWeight = gradebookCategory.getWeight();
					Float averageDistibution = roundToTwoDecimals(catWeight/pointsScores.size());
					
					// drop the lowest catAverageScorePercent scores
					if (dropScores)
					{
						dropLowestScores(userToolGradebookItemsMap, dropNumLowestScores, pointsScores, averageDistibution);						
					}
					else if (keepOnlyHighest)
					{
						dropLowestScores(userToolGradebookItemsMap, pointsScores);
					}
					
					// reset the average distribution if dropped scores
					if (dropScores || keepOnlyHighest)
					{
						averageDistibution = roundToTwoDecimals(catWeight/pointsScores.size());
					}
					
					Float weightedCatTotalpoints = null;
					Float weightedCatTotalScore = null;
					Float weightedCatAverageScorePercent = null;
					
					Float catBoostByPoints = null;
					// grades boost by
					if (gradebook.getBoostUserGradesBy() != null && gradebook.getBoostUserGradesBy() > 0 && gradebook.getBoostUserGradesType() != null && gradebook.getBoostUserGradesType() == BoostUserGradesType.points)
					{
						catBoostByPoints = gradebook.getBoostUserGradesBy();
						
						catBoostByPoints = catBoostByPoints * (averageDistibution/100);						
					}
					
					for (PointsScore pointsScore : pointsScores)
					{
						// Don't add extra credit points to total points
						if (!gradebookCategory.isExtraCredit())
						{
							if (actualUserGrade.getPoints() == null)
							{
								actualUserGrade.setPoints(0.0f);
							}
							actualUserGrade.setPoints(actualUserGrade.getPoints() + pointsScore.points);
						}
						
						if (actualUserGrade.getScore() == null)
						{
							actualUserGrade.setScore(0.0f);
						}
						actualUserGrade.setScore(actualUserGrade.getScore() + pointsScore.score);
						
						catAverageScorePercent = null;
						
						if ((pointsScore.points != null) && (pointsScore.score != null))
						{
							if (gradebook.getBoostUserGradesBy() != null && gradebook.getBoostUserGradesBy() > 0 && gradebook.getBoostUserGradesType() != null 
											&& gradebook.getBoostUserGradesType() == BoostUserGradesType.points && catBoostByPoints != null && catBoostByPoints > 0)
							{
								// calculate average for each score as per average distribution inlcudes boost by
								catAverageScorePercent  = (((pointsScore.score + catBoostByPoints)/pointsScore.points) * 100 ) * (averageDistibution/100);
								
								if (weightedCatTotalScore == null)
								{
									weightedCatTotalScore = 0.0f;
								}
								
								// add boost by score
								weightedCatTotalScore += (catBoostByPoints * (averageDistibution/100));
							}
							else
							{
								// calculate average for each score as per average distribution
								catAverageScorePercent  = ((pointsScore.score/pointsScore.points) * 100 ) * (averageDistibution/100);
							}
							
							if (actualUserGrade.getAveragePercent() == null)
							{
								actualUserGrade.setAveragePercent(0.0f);
							}
							actualUserGrade.setAveragePercent(actualUserGrade.getAveragePercent() + catAverageScorePercent);
							
							// set category points, scores , average
							if (weightedCatTotalpoints == null)
							{
								weightedCatTotalpoints = 0.0f;
							}
							weightedCatTotalpoints += pointsScore.points;

							if (weightedCatTotalScore == null)
							{
								weightedCatTotalScore = 0.0f;
							}
							weightedCatTotalScore += pointsScore.score;
							
							if (weightedCatAverageScorePercent == null)
							{
								weightedCatAverageScorePercent = 0.0f;
							}
							weightedCatAverageScorePercent += catAverageScorePercent;
						}
					}
					
					// set user category grade with points, score, average
					((UserCategoryGradeImpl)userCategoryGrade).setPoints(weightedCatTotalpoints);
					((UserCategoryGradeImpl)userCategoryGrade).setScore(weightedCatTotalScore);
					((UserCategoryGradeImpl)userCategoryGrade).setAveragePercent(weightedCatAverageScorePercent);
				}
				else if (gradebookCategory.isExtraCredit())
				{
					// calculate extra credit percent
					if (gradebookCategory.getWeight() == null && gradebookCategory.getWeightDistribution() == null)
					{

						//drop lowest scores(lowest scores are the scores the percentage of score and points is high i.e. score/points number is highest.
						if (dropScores)
						{
							dropLowestScores(userToolGradebookItemsMap, dropNumLowestScores, pointsScores);
						}
						else if (keepOnlyHighest)
						{
							dropLowestScores(userToolGradebookItemsMap, pointsScores);
						}
													
						for (PointsScore pointsScore : pointsScores)
						{
							if ((pointsScore.points != null) && (pointsScore.score != null))
							{
								if (catTotalpoints == null)
								{
									catTotalpoints = 0.0f;
								}
								catTotalpoints += pointsScore.points;
								
								if (catTotalScore == null)
								{
									catTotalScore = 0.0f;
								}
								catTotalScore += pointsScore.score;
							}
						}
						
						if (catTotalScore != null && catTotalpoints != null)
						{
							// extra credit total points and total score, calculate percent and add to final percent 
							if (extraCreditCatTotalScore == null)
							{
								extraCreditCatTotalScore = 0.0f;
							}
							extraCreditCatTotalScore = extraCreditCatTotalScore + catTotalScore;
							
							if (extraCreditCatTotalPoints == null)
							{
								extraCreditCatTotalPoints = 0.0f;
							}
							extraCreditCatTotalPoints = extraCreditCatTotalPoints + catTotalpoints;
							
							// set user category grade with points, score, average
							((UserCategoryGradeImpl)userCategoryGrade).setPoints(catTotalpoints);
							((UserCategoryGradeImpl)userCategoryGrade).setScore(catTotalScore);
							//((UserCategoryGradeImpl)userCategoryGrade).setAveragePercent();
						}
					}
					// extra credit with weight and weight distribution
					else if (gradebookCategory.getWeight() != null && gradebookCategory.getWeightDistribution() != null && gradebookCategory.getWeightDistribution() == WeightDistribution.Points)
					{
						extraCreditCatWeight = gradebookCategory.getWeight();
						extraCreditCatWeightDistribution = gradebookCategory.getWeightDistribution();
					
						if (dropScores)
						{
							dropLowestScores(userToolGradebookItemsMap, dropNumLowestScores, pointsScores);
						}
						else if (keepOnlyHighest)
						{
							dropLowestScores(userToolGradebookItemsMap, pointsScores);
						}
													
						for (PointsScore pointsScore : pointsScores)
						{
							if ((pointsScore.points != null) && (pointsScore.score != null))
							{
								if (catTotalpoints == null)
								{
									catTotalpoints = 0.0f;
								}
								catTotalpoints += pointsScore.points;
								
								if (catTotalScore == null)
								{
									catTotalScore = 0.0f;
								}
								catTotalScore += pointsScore.score;
							}
						}
						
						if (catTotalScore != null && catTotalpoints != null)
						{
							if (extraCreditCatTotalScore == null)
							{
								extraCreditCatTotalScore = 0.0f;
							}
							extraCreditCatTotalScore = extraCreditCatTotalScore + catTotalScore;
							
							if (extraCreditCatTotalPoints == null)
							{
								extraCreditCatTotalPoints = 0.0f;
							}
							extraCreditCatTotalPoints = extraCreditCatTotalPoints + catTotalpoints;
							
							// set user category grade with points, score, average
							((UserCategoryGradeImpl)userCategoryGrade).setPoints(catTotalpoints);
							((UserCategoryGradeImpl)userCategoryGrade).setScore(catTotalScore);
							//((UserCategoryGradeImpl)userCategoryGrade).setAveragePercent();
						}
					}
					else if (gradebookCategory.getWeight() != null && gradebookCategory.getWeightDistribution() != null && gradebookCategory.getWeightDistribution() == WeightDistribution.Equally)
					{
						extraCreditCatWeight = gradebookCategory.getWeight();
						extraCreditCatWeightDistribution = gradebookCategory.getWeightDistribution();
						
						// check for drop lowest scores and drop lowest scores. Drop the lowest of average distribution						
						catWeight = gradebookCategory.getWeight();
						Float averageDistibution = roundToTwoDecimals(catWeight/pointsScores.size());
						
						// Drop the lowest catAverageScorePercent scores
						if (dropScores)
						{
							dropLowestScores(userToolGradebookItemsMap, dropNumLowestScores, pointsScores, averageDistibution);
						}
						else if (keepOnlyHighest)
						{
							dropLowestScores(userToolGradebookItemsMap, pointsScores);							
						}
						
						// reset the average distribution if dropped scores
						if (dropScores || keepOnlyHighest)
						{
							averageDistibution = roundToTwoDecimals(catWeight/pointsScores.size());
						}
						
						for (PointsScore pointsScore : pointsScores)
						{
							catAverageScorePercent = null;
							
							if ((pointsScore.points != null) && (pointsScore.score != null))
							{
								if (extraCreditCatWeightDistEquallyTotalScoresPercent == null)
								{
									extraCreditCatWeightDistEquallyTotalScoresPercent = 0.0f;
								}
								
								// calculate average for each score as per average distribution
								extraCreditCatWeightDistEquallyTotalScoresPercent = extraCreditCatWeightDistEquallyTotalScoresPercent + (((pointsScore.score/pointsScore.points) * 100) * (averageDistibution/100));
								
								if (extraCreditCatTotalPoints == null)
								{
									extraCreditCatTotalPoints = 0.0f;
								}
								extraCreditCatTotalPoints += pointsScore.points;

								if (extraCreditCatTotalScore == null)
								{
									extraCreditCatTotalScore = 0.0f;
								}
								extraCreditCatTotalScore += pointsScore.score;
							}
						}
						
						
						// set user category grade with points, score, average
						((UserCategoryGradeImpl)userCategoryGrade).setPoints(extraCreditCatTotalPoints);
						((UserCategoryGradeImpl)userCategoryGrade).setScore(extraCreditCatTotalScore);
						((UserCategoryGradeImpl)userCategoryGrade).setAveragePercent(extraCreditCatWeightDistEquallyTotalScoresPercent);
					}
				}
			}
		}
		
		Float averagePercent = actualUserGrade.getAveragePercent();
		
		// if category has weights and grade option is "released only" user average percent should be calculated proportionately to 100%
		if (gradebook.getReleaseGrades() == ReleaseGrades.Released)
		{
			
			Float userCatTotalWeight = 0.0f;
			Float userCatGradeAveragePercentTotal = null;
			
			for (UserCategoryGrade userCatGrade : actualUserGrade.getUserCategoryGrade())
			{
				// don't include extra credit
				if (userCatGrade.getGradebookCategory().isExtraCredit())
				{
					continue;
				}
				
				Float userCatGradeAveragePercent = userCatGrade.getAveragePercent();
				GradebookCategory actualGradebookCategory = userCatGrade.getGradebookCategory();
				
				if (userCatGradeAveragePercent != null && userCatGradeAveragePercent >= 0 && actualGradebookCategory.getWeight() > 0)
				{
					if (userCatGradeAveragePercentTotal == null)
					{
						userCatGradeAveragePercentTotal = 0.0f;
					}
					userCatTotalWeight += actualGradebookCategory.getWeight();
					userCatGradeAveragePercentTotal += userCatGradeAveragePercent;
				}
			}
			
			if (userCatTotalWeight > 0 && userCatGradeAveragePercentTotal != null && userCatGradeAveragePercentTotal >= 0)
			{
				Float updatedAveragePercent = (userCatGradeAveragePercentTotal / userCatTotalWeight) * 100f;
				
				updatedAveragePercent = roundToTwoDecimals(updatedAveragePercent);
				
				actualUserGrade.setAveragePercent(updatedAveragePercent);
				
				averagePercent = actualUserGrade.getAveragePercent();
			}
		}
				
		if (actualUserGrade.getScore() != null && actualUserGrade.getPoints() != null && actualUserGrade.getPoints() > 0)
		{
			if (averagePercent != null && averagePercent > 0)
			{
				averagePercent = roundToTwoDecimals(averagePercent);
			}
			else
			{
				averagePercent = 0.0f;
			}
			
			if (gradebook.getBoostUserGradesBy() != null && gradebook.getBoostUserGradesBy() > 0 && (gradebook.getBoostUserGradesType() == BoostUserGradesType.points || gradebook.getBoostUserGradesType() == BoostUserGradesType.percent))
			{
				if (gradebook.getBoostUserGradesType() == BoostUserGradesType.points)
				{
					// weighted distribution how to distribute if getBoostUserGradesBy is points???? Distributed while calculating categories. Check the calculation.
				}
				else if (gradebook.getBoostUserGradesType() == BoostUserGradesType.percent)
				{
					averagePercent = averagePercent + gradebook.getBoostUserGradesBy();
					averagePercent = roundToTwoDecimals(averagePercent);
					actualUserGrade.setAveragePercent(averagePercent);
					
					//add boost by points to score by percent.
					actualUserGrade.setScore(actualUserGrade.getPoints() * (averagePercent + gradebook.getBoostUserGradesBy()) / 100);
					
					actualUserGrade.setScore(roundToTwoDecimals(actualUserGrade.getScore()));
				}
			}
			
			/*
			if (extraCreditCatWeight == null && extraCreditCatWeightDistribution == null)
			{
				// add extra credit if any
				if (extraCreditCatTotalScore != null && extraCreditCatTotalScore > 0 && extraCreditCatTotalPoints != null && extraCreditCatTotalPoints > 0)
				{
					Float extraCreditCatPercent;
					
					extraCreditCatPercent = (extraCreditCatTotalPoints/actualUserGrade.getPoints()) * 100.0f;
					
					Float extraCreditAveragePercent = (extraCreditCatPercent / 100.0f) * extraCreditCatTotalScore;
					extraCreditAveragePercent = roundToTwoDecimals(extraCreditAveragePercent);
					
					averagePercent = averagePercent + extraCreditAveragePercent;
					
					// extra credit percent
					actualUserGrade.setExtraCreditPercent(extraCreditAveragePercent);
				}
				
			}
			else if (extraCreditCatWeight != null && extraCreditCatWeight > 0 && extraCreditCatWeightDistribution != null && 
											(extraCreditCatWeightDistribution == WeightDistribution.Points || extraCreditCatWeightDistribution == WeightDistribution.Equally))
			{
				
				if (extraCreditCatWeightDistribution == WeightDistribution.Points)
				{
					if (extraCreditCatTotalScore != null && extraCreditCatTotalScore > 0)
					{
						Float extraCreditAveragePercent = (extraCreditCatWeight / 100.0f) * extraCreditCatTotalScore;
						extraCreditAveragePercent = roundToTwoDecimals(extraCreditAveragePercent);
						
						averagePercent = averagePercent + extraCreditAveragePercent;
						
						// may be not needed as already extra credit percent added
						actualUserGrade.setAveragePercent(averagePercent);
						
						// extra credit percent
						actualUserGrade.setExtraCreditPercent(extraCreditAveragePercent);
					}
				}
				else if (extraCreditCatWeightDistribution == WeightDistribution.Equally)
				{
					if (extraCreditCatWeightDistEquallyTotalScoresPercent != null && extraCreditCatWeightDistEquallyTotalScoresPercent > 0)
					{
						extraCreditCatWeightDistEquallyTotalScoresPercent = roundToTwoDecimals(extraCreditCatWeightDistEquallyTotalScoresPercent);
						averagePercent = averagePercent + extraCreditCatWeightDistEquallyTotalScoresPercent;
						
						// may be not needed as already extra credit percent added
						actualUserGrade.setAveragePercent(averagePercent);
						
						// extra credit percent
						actualUserGrade.setExtraCreditPercent(extraCreditCatWeightDistEquallyTotalScoresPercent);
					}
				}
			}
			*/		
		}

		// check for extra credit
		if (extraCreditCatWeight == null && extraCreditCatWeightDistribution == null)
		{
			// add extra credit if any
			if (extraCreditCatTotalScore != null && extraCreditCatTotalScore > 0 && extraCreditCatTotalPoints != null && extraCreditCatTotalPoints > 0)
			{
				//Float extraCreditCatPercent;
				
				//extraCreditCatPercent = (extraCreditCatTotalPoints/actualUserGrade.getPoints()) * 100.0f;
				
				Float extraCreditAveragePercent;
				
				if (actualUserGrade.getPoints() != null && actualUserGrade.getPoints() > 0)
				{
					extraCreditAveragePercent = (extraCreditCatTotalScore / actualUserGrade.getPoints()) * 100.0f;
				}
				else
				{
					extraCreditAveragePercent = (extraCreditCatTotalScore / extraCreditCatTotalPoints) * 100.0f;
				}
				extraCreditAveragePercent = roundToTwoDecimals(extraCreditAveragePercent);
				
				if (averagePercent == null)
				{
					averagePercent = 0.0f;
				}
				
				averagePercent = averagePercent + extraCreditAveragePercent;
				
				// may be not needed as already extra credit percent added
				actualUserGrade.setAveragePercent(averagePercent);
				
				// extra credit percent
				actualUserGrade.setExtraCreditPercent(extraCreditAveragePercent);
				actualUserGrade.setExtraCreditScore(extraCreditCatTotalScore);
			}
			
		}
		else if (extraCreditCatWeight != null && extraCreditCatWeight > 0 && extraCreditCatWeightDistribution != null && 
										(extraCreditCatWeightDistribution == WeightDistribution.Points || extraCreditCatWeightDistribution == WeightDistribution.Equally))
		{
			
			if (extraCreditCatWeightDistribution == WeightDistribution.Points)
			{
				if (extraCreditCatTotalPoints != null && extraCreditCatTotalPoints > 0 && extraCreditCatTotalScore != null && extraCreditCatTotalScore > 0)
				{
					Float extraCreditAveragePercent = ((extraCreditCatTotalScore/extraCreditCatTotalPoints) * 100) * (extraCreditCatWeight / 100.0f);
					extraCreditAveragePercent = roundToTwoDecimals(extraCreditAveragePercent);
					
					if (averagePercent == null)
					{
						averagePercent = 0.0f;
					}
					averagePercent = averagePercent + extraCreditAveragePercent;
					
					// may be not needed as already extra credit percent added
					actualUserGrade.setAveragePercent(averagePercent);
					
					// extra credit percent
					actualUserGrade.setExtraCreditPercent(extraCreditAveragePercent);				
					actualUserGrade.setExtraCreditScore(extraCreditCatTotalScore);
				}
			}
			else if (extraCreditCatWeightDistribution == WeightDistribution.Equally)
			{
				if (extraCreditCatWeightDistEquallyTotalScoresPercent != null && extraCreditCatWeightDistEquallyTotalScoresPercent > 0)
				{
					extraCreditCatWeightDistEquallyTotalScoresPercent = roundToTwoDecimals(extraCreditCatWeightDistEquallyTotalScoresPercent);
					
					if (averagePercent == null)
					{
						averagePercent = 0.0f;
					}
					
					averagePercent = averagePercent + extraCreditCatWeightDistEquallyTotalScoresPercent;
					
					// may be not needed as already extra credit percent added
					actualUserGrade.setAveragePercent(averagePercent);
					
					// extra credit percent
					actualUserGrade.setExtraCreditPercent(extraCreditCatWeightDistEquallyTotalScoresPercent);
					actualUserGrade.setExtraCreditScore(extraCreditCatTotalScore);
				}
			}
		}
	}

	/**
	 * Creates new map item if mapped with standard category code
	 * 
	 * @param gradebookCategoryCodeMap
	 * 
	 * @param gradebookItem
	 * 
	 * @return
	 */
	protected GradebookCategoryItemMap createNewMapStandardItem(Map<Integer, GradebookCategory> gradebookCategoryCodeMap, GradebookItem gradebookItem)
	{
		if (gradebookItem == null || gradebookCategoryCodeMap == null)
		{
			return null;
		}
		
		GradebookCategoryItemMap gradebookCategoryItemMap = new GradebookCategoryItemMapImpl();
		
		// get matching standard category code based on item type. If not matched not mapped with category
		StandardCategory standardCategory = GradebookItemType.getCategoryGradebookItemType(gradebookItem.getType());
		
		if (standardCategory == null)
		{
			return null;
		}
		
		GradebookCategory gradebookCategory = gradebookCategoryCodeMap.get(standardCategory.getCode());
		
		if (gradebookCategory == null)
		{
			// return null;
			gradebookCategory = gradebookCategoryCodeMap.get(StandardCategory.extracredit.getCode());
		}
		
		if (gradebookCategory == null)
		{
			return null;
		}
		
		((GradebookCategoryItemMapImpl)gradebookCategoryItemMap).setCategoryId(gradebookCategory.getId());
		
		((GradebookCategoryItemMapImpl)gradebookCategoryItemMap).setItemId(gradebookItem.getId());
		
		((GradebookCategoryItemMapImpl)gradebookCategoryItemMap).setCategory(gradebookCategory);
		
		return gradebookCategoryItemMap;
	}

	/**
	 * Drops lowest scores
	 * 
	 * @param userToolGradebookItemsMap	User tool gradebook items map
	 * 
	 * @param dropNumLowestScores	Number of lowest scores to be dropped
	 * 
	 * @param pointsScores	List of points scores
	 */
	protected void dropLowestScores(Map<String, ParticipantItemDetails> userToolGradebookItemsMap, int dropNumLowestScores, List<PointsScore> pointsScores)
	{
		if (pointsScores == null || pointsScores.size() == 0 || dropNumLowestScores <= 0)
		{
			return;
		}
		
		// loop and remove the lowest scores - one at a time
		for (int i = 0; i < dropNumLowestScores; i++)
		{
			Float smallest = new Float(Float.MAX_VALUE);
			int index = -1;
			int position = 0;
			for (PointsScore pointsScore : pointsScores)
			{
				if(smallest.compareTo(pointsScore.getScorePointPercent()) == 0)
				{
					// drop the highest points item
					PointsScore smallestPointsScore = pointsScores.get(index);
					if (smallestPointsScore != null)
					{
						Float smallestPoints = smallestPointsScore.getPoints();
						
						if (smallestPoints != null)
						{
							if (smallestPoints.compareTo(pointsScore.getPoints()) < 0)
							{
								smallest = pointsScore.getScorePointPercent();
								index = position;
							}											
						}
					}
					
				}
				else if(smallest.compareTo(pointsScore.getScorePointPercent()) > 0)
				{
					smallest = pointsScore.getScorePointPercent();
					index = position;
				}
				position++;
			}
			
			if (index > -1)
			{
				if (userToolGradebookItemsMap != null && userToolGradebookItemsMap.size() > 0)
				{
					PointsScore pointsScore = pointsScores.get(index);
					
					ParticipantItemDetails participantItemDetails = userToolGradebookItemsMap.get(pointsScore.getItemId());
					if (participantItemDetails != null)
					{
						((ParticipantItemDetailsImpl)participantItemDetails).setIsScoreDropped(Boolean.TRUE);
					}
				}
				pointsScores.remove(index);
			}
		}
	}

	/**
	 * Drops lowest scores with weights
	 * 
	 * @param userToolGradebookItemsMap		User tool gradebook items map
	 * 
	 * @param dropNumLowestScores			Number of lowest scores to be dropped
	 * 
	 * @param pointsScores					List of points scores
	 * 
	 * @param averageDistibution			Average distribution
	 */
	protected void dropLowestScores(Map<String, ParticipantItemDetails> userToolGradebookItemsMap, int dropNumLowestScores, List<PointsScore> pointsScores, Float averageDistibution)
	{
		if (pointsScores == null || pointsScores.size() == 0 || dropNumLowestScores <= 0 || averageDistibution == null || averageDistibution < 0)
		{
			return;
		}
		
		Float catAverageScorePercent;
		
		// loop and remove the lowest scores - one at a time
		for (int i = 0; i < dropNumLowestScores; i++)
		{
			Float smallest = new Float(Float.MAX_VALUE);
			int index = -1;
			int position = 0;
			for (PointsScore pointsScore : pointsScores)
			{
				catAverageScorePercent = null;
				if ((pointsScore.points != null) && (pointsScore.score != null && pointsScore.points > 0))
				{
					// calculate average for each score as per average distribution
					catAverageScorePercent  = ((pointsScore.score/pointsScore.points) * 100 ) * (averageDistibution/100);
					
					if(smallest.compareTo(catAverageScorePercent) == 0)
					{
						// drop the highest points item
						PointsScore smallestPointsScore = pointsScores.get(index);
						if (smallestPointsScore != null)
						{
							Float smallestPoints = smallestPointsScore.getPoints();
							
							if (smallestPoints != null)
							{
								if (smallestPoints.compareTo(pointsScore.getPoints()) < 0)
								{
									smallest = catAverageScorePercent;
									index = position;
								}											
							}
						}						
					}
					else if(smallest.compareTo(catAverageScorePercent) > 0)
					{
						smallest = catAverageScorePercent;
						index = position;
					}
				}
				position++;
			}
						
			if (index > -1)
			{
				if (userToolGradebookItemsMap != null && userToolGradebookItemsMap.size() > 0)
				{
					PointsScore pointsScore = pointsScores.get(index);
					ParticipantItemDetails participantItemDetails = userToolGradebookItemsMap.get(pointsScore.getItemId());
					if (participantItemDetails != null)
					{
						((ParticipantItemDetailsImpl)participantItemDetails).setIsScoreDropped(Boolean.TRUE);
					}
				}
				pointsScores.remove(index);
			}
		}
	}
	
	/**
	 * Drop all lowest scores
	 * 
	 * @param userToolGradebookItemsMap	User tool gradebok items map
	 * 
	 * @param pointsScores	List points scores
	 */
	protected void dropLowestScores(Map<String, ParticipantItemDetails> userToolGradebookItemsMap, List<PointsScore> pointsScores)
	{
		// if the items or scores are less than drop lowest scores number drop all the scores. 
		if (pointsScores != null)
		{
			for (PointsScore pointsScore : pointsScores)
			{
				if (userToolGradebookItemsMap != null)
				{
					ParticipantItemDetails participantItemDetails = userToolGradebookItemsMap.get(pointsScore.getItemId());
					if (participantItemDetails != null)
					{
						((ParticipantItemDetailsImpl)participantItemDetails).setIsScoreDropped(Boolean.TRUE);
					}
				}
			}
			
			pointsScores.clear();
		}
	}

	/**
	 * Fetches gradebook tool items
	 * 
	 * @param context	Context
	 * 
	 * @param userId	User id
	 * 
	 * @param fetchScores	If true fetches scores else not
	 * 
	 * @param fetchUnpublish	If true fetches unpublished items else not
	 * 
	 * @param itemType	Gradebook itemtype
	 * 
	 * @param resetDisplayOrder true - resets the display order else not
	 * 
	 * @return	List of gradebook tool items
	 */
	protected List<GradebookItem> fetchGradebookItems(String context, String userId, boolean fetchScores, boolean fetchUnpublish, GradebookItemType itemType, boolean resetDisplayOrder)
	{
		List<GradebookItem> gradebookItems = new ArrayList<GradebookItem>();
		
		Set<String> actives = new HashSet<String>();
		// get the active users
		if (fetchScores)
		{
			List<Participant> participants = getParticipants(context, false);
			actives = getActiveParticipants(participants);
		}
		
		// get the items from the providers				
		for (GradebookItemProvider provider : this.providers)
		{
			// get the items from this provider
			List<GradebookItem> gradableItems = provider.getGradableItems(context, userId, actives, fetchScores, fetchUnpublish, itemType);
			
			if (gradableItems != null)
			{
				gradebookItems.addAll(gradableItems);
			}
		}
		
		// merge items. Don't merger or map when filter itemType is used as all items are not fetched 
		if (itemType == null && resetDisplayOrder)
		{
			mergeItems(context, userId, gradebookItems, false, resetDisplayOrder);
		}
		return gradebookItems;
	}

	/**
	 * Fetches tool gradebook items
	 * 
	 * @param context		Context
	 * 
	 * @param userId		User id'
	 * 
	 * @param fetchScores	If true fetches scores
	 * 
	 * @param itemType 		Filter by item type
	 * 
	 * @return	List of tool gradebook items
	 */
	protected List<GradebookItem> fetchGradebookToolItems(String context, String userId, boolean fetchScores, boolean fetchUnpublish, GradebookItemType itemType)
	{
		if ((context == null || context.trim().length() == 0) || (userId == null || userId.trim().length() == 0))
		{
			new ArrayList<GradebookItem>();
		}
		
		String key = "etudesgradebook:fetchGradebookToolItems:"+ context +":"+ userId +":"+ fetchScores +":"+ fetchUnpublish +":"+ itemType;
		
		@SuppressWarnings("unchecked")
		List<GradebookItem> toolGradebookItems = (List<GradebookItem>) this.threadLocalManager.get(key);
		
		if (toolGradebookItems != null)
		{
			List<GradebookItem> toolGradebookItemsCopy = new ArrayList<GradebookItem>();
			
			for (GradebookItem  toolGradebookItem : toolGradebookItems)
			{
				toolGradebookItemsCopy.add(new GradebookItemImpl((GradebookItemImpl)toolGradebookItem));
			}
			return toolGradebookItemsCopy;
		}
		
		List<GradebookItem> gradebookItems = fetchGradebookItems(context, userId, fetchScores, fetchUnpublish, itemType, true);
		
		this.threadLocalManager.set(key, gradebookItems);
		
		return gradebookItems;
	}

	/**
	 * Gets participant in the site or null
	 * 
	 * @param context	Context
	 * 
	 * @param userId	User id
	 * 
	 * @return	Participant or null
	 */
	protected Participant fetchParticipant(String context, String userId)
	{
		if (context == null || context.trim().length() == 0 || userId == null || userId.trim().length() == 0)
		{
			return null;
		}
		
		try
		{
			Site site = this.siteService.getSite(context);
			
			Member m = site.getMember(userId);
			
			ParticipantStatus status = null;
			//String userId = m.getUserId();
			String sortName = null;
			String displayId = null;
			try
			{
				User user = this.userDirectoryService.getUser(userId);
				sortName = user.getSortName();
			//	displayId = user.getDisplayId().trim();
				displayId = user.getIidInContext(context);
				if (displayId == null) displayId = user.getEid();
				
				// outright skip guests
				if (m.getRole().getId().equalsIgnoreCase("guest"))
				{
					return null;
				}

				if (m.getRole().getId().equals("Blocked"))
				{
					status = ParticipantStatus.blocked;
				}

				else if (m.getRole().getId().equals("Observer"))
				{
					status = ParticipantStatus.observer;
				}

				else if (site.isAllowed(userId, "section.role.student"))
				{
					if (!m.isProvided())
					{
						if (m.isActive())
						{
							status = ParticipantStatus.added;
						}
						else
						{
							status = ParticipantStatus.blocked;
						}
						
					}
					else
					{
						status = ParticipantStatus.enrolled;
					}
				}

				else if (site.isAllowed(userId, "section.role.instructor"))
				{
					status = ParticipantStatus.instructor;
				}

				else if (site.isAllowed(userId, "section.role.ta"))
				{
					status = ParticipantStatus.ta;
				}

				else if (!m.isActive())
				{
					// check for inactive users of the role that has this access
					Set roles = site.getRolesIsAllowed("section.role.student");
					if (roles.contains(m.getRole().getId()))
					{
						// inactive could-be-students that are provided are "dropped", but if they are not provided, they are "blocked"
						if (m.isProvided())
						{
							status = ParticipantStatus.dropped;
						}
						else
						{
							status = ParticipantStatus.blocked;
						}
					}
				}
				
				// which section is the user in? If in multiple, pick the one that is active, if any. Otherwise, just pick any.
				String groupTitle = null;
				String titleActive = null;
				String titleInactive = null;
				Collection groups = site.getGroups();
				for (Object groupO : groups)
				{
					Group g = (Group) groupO;

					// skip non-section groups
					if (g.getProperties().getProperty("sections_category") == null) continue;

					// we want to find the user even if not active, so we cannot use g.getUsers(), which only returns active users -ggolden
					// if (g.getUsers().contains(userId))
					Set<Member> groupMemebers = g.getMembers();
					for (Member gm : groupMemebers)
					{
						if (gm.getUserId().equals(userId))
						{
							if (gm.isActive())
							{
								if (titleActive == null) titleActive = g.getTitle();
							}
							else
							{
								if (titleInactive == null) titleInactive = g.getTitle();
							}
						}
					}
				}
				if (titleActive != null)
				{
					groupTitle = titleActive;
				}
				else if (titleInactive != null)
				{
					groupTitle = titleInactive;
				}

				// take only those who have a status set (this skips the non-students)
				if (status != null)
				{
					ParticipantImpl p = new ParticipantImpl();
					p.userId = userId;
					p.status = status;
					p.sortName = sortName;
					p.displayId = displayId;
					p.groupTitle = groupTitle;
					
					// private message link
					String toolId = null;
					ToolConfiguration config = site.getToolForCommonId(JFORUM_TOOL_ID);
					if (config != null)
					{
						toolId = config.getId();
					}
					
					if (toolId != null && (toolId.trim().length() > 0))
					{
						setPrivateMessageLink(site.getId(), toolId, p);
					}
					
					return p;
				}
			}
			catch (UserNotDefinedException e)
			{
				return null;
			}
		}
		catch (IdUnusedException e)
		{
			logger.warn("getParticipant: missing site: " + context);
		}
		
		return null;
	}
	
	/**
	 * Fetches all grdable items from tools and resets the map
	 * 
	 * @param context	Context
	 * 
	 * @param userId	User id
	 */
	protected void fetchToolGradableItemsAndResetItemMap(String context, String userId)
	{
		// user access check
		Boolean userAccess = allowEditGradebook(context, userId);
				
		if (!userAccess)
		{
			userAccess = allowGetGradebook(context, userId);
			
			if (!userAccess)
			{
				return;
			}
		}
		
		List<GradebookItem> gradebookItems = new ArrayList<GradebookItem>();
		
		gradebookItems.addAll(fetchGradebookToolItems(context, userId, false, false, null));
		
		checkAndSetDisplayOrder(context, userId, gradebookItems);
		
		Collections.sort(gradebookItems, new GradebookComparator(GradebookSortType.Category, true));
	}

	/**
	 * Fetches the users gradable items
	 * 
	 * @param context	Context
	 * 
	 * @param fetchedByUserId	Fetched by user id
	 * 
	 * @param activeParticipants	Active participants
	 * 
	 * @return	The map with user id as key and list of user gradable items as list
	 */
	protected Map<String, List<ParticipantGradebookItem>> fetchUsersGradebookSummaryItems(String context, String fetchedByUserId, List<Participant> participants, GradebookItemType itemType, Gradebook.ReleaseGrades gradebookReleaseGrades)
	{
		if ((context == null || context.trim().length() == 0) || (fetchedByUserId == null || fetchedByUserId.trim().length() == 0))
		{
			return new HashMap<String, List<ParticipantGradebookItem>>();
		}
		
		Map<String, List<ParticipantGradebookItem>> userGradableItemsScores = new HashMap<String, List<ParticipantGradebookItem>>();
		List<String> participantIds = new ArrayList<String>();
		
		for (Participant participant : participants)
		{
			userGradableItemsScores.put(participant.getUserId(), new ArrayList<ParticipantGradebookItem>());
			participantIds.add(participant.getUserId());
		}
				
		boolean allScores = false;
		/*
		if (gradebookReleaseGrades == Gradebook.ReleaseGrades.All)
		{
			allScores = true;
		}
		else if (gradebookReleaseGrades == Gradebook.ReleaseGrades.Released)
		{
			allScores = false;
		}
		*/
		// get the items from the providers				
		for (GradebookItemProvider provider : this.providers)
		{
			// get the items from this provider. For mneme allscores doesn't matter always gets best scores of completed submissions 
			Map<String, List<ParticipantGradebookItem>> userParticipantGradookItems = provider.getUsersGradableItems(context, fetchedByUserId, participantIds, itemType, allScores);
			
			if (userParticipantGradookItems != null)
			{
				for (String userId : userGradableItemsScores.keySet()) 
				{
					List<ParticipantGradebookItem> userParticipantGradebookItemsScores = userParticipantGradookItems.get(userId);
					
					List<ParticipantGradebookItem> participantGradebookItemList = userGradableItemsScores.get(userId);
					
					if (participantGradebookItemList != null && userParticipantGradebookItemsScores != null)
					{
						participantGradebookItemList.addAll(userParticipantGradebookItemsScores);
					}
				}
			}
		}
		
		return userGradableItemsScores;
	}
		
	/**
	 * Build a set containing the user ids of the active participants.
	 * 
	 * @param participants
	 *        The full list of participants.
	 * @return A Set of userId strings for those participants that are active.
	 */
	protected Set<String> getActiveParticipants(List<Participant> participants)
	{
		Set<String> rv = new HashSet<String>();
		for (Participant p : participants)
		{
			if (((ParticipantImpl)p).status == ParticipantStatus.enrolled)
			{
				rv.add(p.getUserId());
			}
			else if (((ParticipantImpl)p).status == ParticipantStatus.added)
			{
				rv.add(p.getUserId());
			}
		}

		return rv;
	}
	
	/**
	 * Gets notes added by instructor for student
	 * 
	 * @param gradebookId	Gradebook id
	 * 
	 * @param studentId		Student id
	 * 
	 * @return	Notes or null if no notes
	 */
	protected Notes getInstructorUserNotes(String context, String studentId)
	{
		if ((context == null || context.trim().length() == 0) || (studentId == null || studentId.trim().length() == 0))
		{
			return null;
		}
		
		User currentUser = this.userDirectoryService.getCurrentUser();
		
		if (currentUser == null)
		{
			return null;
		}
		
		// user access check
		Boolean userAccess = allowEditGradebook(context, currentUser.getId());
		
		// only instructors should have access to instructor added notes
		if (!userAccess)
		{
			return null;
		}
		
		Gradebook gradebook = getContextGradebook(context, currentUser.getId());
		
		if (gradebook == null)
		{
			return null;
		}
		
		return this.storage.selectInstructorUserNotes(gradebook.getId(), studentId);
	}
	
	/**
	 * Gets and sets instructor user notes
	 * 
	 * @param context		Context
	 * 
	 * @param fetchedByUserId	Fetched by user id
	 * 
	 * @param rv	Participant Item Details
	 */
	protected void getInstructorUserNotes(String context, String fetchedByUserId, ArrayList<ParticipantItemDetails> rv)
	{
		if ((context == null || context.trim().length() == 0) || (fetchedByUserId == null || fetchedByUserId.trim().length() == 0) || (rv == null))
		{
			return;
		}
		
		Gradebook gradebook = getContextGradebook(context, fetchedByUserId);
		
		// instructor notes
		Map<String, Notes> instructorUserNotes = this.storage.selectInstructorUsersNotes(gradebook.getId());

		// instructor user notes
		for (ParticipantItemDetails participantItemDetails : rv)
		{
			// instructor notes
			Notes instructorNotes = instructorUserNotes.get(participantItemDetails.getUserId());
			if (instructorNotes != null)
			{
				((ParticipantImpl)participantItemDetails).setInstructorNotes(instructorNotes);
			}				
		}
	}
	
	/**
	 * Gets jforum gradable item
	 * 
	 * @param context	Context
	 * 
	 * @param itemId	Item id	 
	 * 
	 * @param userId	Fetched by user id
	 * 
	 * @return	The jforum gradable item
	 */
	protected GradebookItem getJForumGradebookItem(String context, String itemId, String groupId, String fetchedByUserId)
	{
		if ((itemId == null || itemId.trim().length() == 0) || (context == null || context.trim().length() == 0) || (fetchedByUserId == null || fetchedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		GradebookItem gradebookItem = null;
		
		// get the users
		List<Participant> participants = null;
		
		if (groupId == null || groupId.trim().length() == 0)
		{
			participants = getParticipants(context, false);
		}
		else
		{
			participants = getParticipants(context, groupId, false);
		}
		
		// get the active users
		Set<String> actives = getActiveParticipants(participants);
		
		for (GradebookItemProvider provider : this.providers)
		{
			gradebookItem = provider.getJForumGradableItem(context, itemId, fetchedByUserId, actives, true);
			
			if (gradebookItem != null)
			{
				break;
			}
		}
		
		return gradebookItem;
	}
	
	/**
	 * Gets the mneme gradebook item
	 * 
	 * @param context	Context
	 * 
	 * @param itemId	Item id
	 * 
	 * @param groupId	Group id
	 * 
	 * @param userId	User id
	 * 
	 * @return	The mneme gradebook item
	 */
	protected GradebookItem getMnemeGradebookItem(String context, String itemId, String groupId, String userId)
	{
		GradebookItem gradebookItem = null;
		
		// get the users
		List<Participant> participants = null;
		
		if (groupId == null || groupId.trim().length() == 0)
		{
			participants = getParticipants(context, false);
		}
		else
		{
			participants = getParticipants(context, groupId, false);
		}
		
		// get the active users
		Set<String> actives = getActiveParticipants(participants);
		
		for (GradebookItemProvider provider : this.providers)
		{
			gradebookItem = provider.getMnemeGradableItem(context, itemId, userId, actives, true);
			
			if (gradebookItem != null)
			{
				break;
			}
		}
		
		return gradebookItem;
	}
	
	/**
	 * Get a list of basic participant information for the site.
	 * 
	 * @param context
	 *        The site id.
	 * @param includeAll
	 *        if true, include instructors and ta's too, else leave them out.
	 * @return The List of Participants for qualified users in the site.
	 */
	protected List<Participant> getParticipants(String context, boolean includeAll)
	{
		ArrayList<Participant> rv = new ArrayList<Participant>();
		try
		{
			Site site = this.siteService.getSite(context);
			
			String toolId = null;
			ToolConfiguration config = site.getToolForCommonId(JFORUM_TOOL_ID);
			if (config != null)
			{
				toolId = config.getId();
			}

			Set<Member> members = site.getMembers();
			for (Member m : members)
			{
				ParticipantStatus status = null;
				String userId = m.getUserId();
				String sortName = null;
				String displayId = null;
				try
				{
					User user = this.userDirectoryService.getUser(userId);
					sortName = user.getSortName();
					// displayId = user.getDisplayId().trim();
					
					displayId = user.getIidInContext(context);
					if (displayId == null) displayId = user.getEid();
				}
				catch (UserNotDefinedException e)
				{
					// skip deleted users
					continue;
				}

				// outright skip guests
				if (m.getRole().getId().equalsIgnoreCase("guest"))
				{
					continue;
				}

				if (m.getRole().getId().equals("Blocked"))
				{
					status = ParticipantStatus.blocked;
				}

				else if (m.getRole().getId().equals("Observer"))
				{
					if (includeAll) status = ParticipantStatus.observer;
				}

				else if (site.isAllowed(userId, "section.role.student"))
				{
					if (!m.isProvided())
					{
						if (m.isActive())
						{
							status = ParticipantStatus.added;
						}
						else
						{
							status = ParticipantStatus.blocked;
						}
						
					}
					else
					{
						status = ParticipantStatus.enrolled;
					}
				}

				else if (site.isAllowed(userId, "section.role.instructor"))
				{
					if (includeAll) status = ParticipantStatus.instructor;
				}

				else if (site.isAllowed(userId, "section.role.ta"))
				{
					if (includeAll) status = ParticipantStatus.ta;
				}

				else if (!m.isActive())
				{
					// check for inactive users of the role that has this access
					Set roles = site.getRolesIsAllowed("section.role.student");
					if (roles.contains(m.getRole().getId()))
					{
						// inactive could-be-students that are provided are "dropped", but if they are not provided, they are "blocked"
						if (m.isProvided())
						{
							status = ParticipantStatus.dropped;
						}
						else
						{
							status = ParticipantStatus.blocked;
						}
					}
				}

				// which section is the user in? If in multiple, pick the one that is active, if any. Otherwise, just pick any.
				String groupTitle = null;
				String titleActive = null;
				String titleInactive = null;
				Collection groups = site.getGroups();
				for (Object groupO : groups)
				{
					Group g = (Group) groupO;

					// skip non-section groups
					if (g.getProperties().getProperty("sections_category") == null) continue;

					// we want to find the user even if not active, so we cannot use g.getUsers(), which only returns active users -ggolden
					// if (g.getUsers().contains(userId))
					Set<Member> groupMemebers = g.getMembers();
					for (Member gm : groupMemebers)
					{
						if (gm.getUserId().equals(userId))
						{
							if (gm.isActive())
							{
								if (titleActive == null) titleActive = g.getTitle();
							}
							else
							{
								if (titleInactive == null) titleInactive = g.getTitle();
							}
						}
					}
				}
				if (titleActive != null)
				{
					groupTitle = titleActive;
				}
				else if (titleInactive != null)
				{
					groupTitle = titleInactive;
				}

				// take only those who have a status set (this skips the non-students)
				if (status != null)
				{
					ParticipantImpl p = new ParticipantImpl();
					p.userId = userId;
					p.status = status;
					p.sortName = sortName;
					p.displayId = displayId;
					p.groupTitle = groupTitle;
					
					if (toolId != null && (toolId.trim().length() > 0))
					{
						setPrivateMessageLink(site.getId(), toolId, p);
					}
					
					rv.add(p);
				}
			}
		}
		catch (IdUnusedException e)
		{
			logger.warn("getParticipants: missing site: " + context);
		}
		
		// sort by status
		Collections.sort(rv, new Comparator<Participant>()
		{
			public int compare(Participant arg0, Participant arg1)
			{
				if ((arg0.getStatus() == null) && (arg1.getStatus() == null)) return 0;
				if (arg0.getStatus() == null) return -1;
				if (arg1.getStatus() == null) return 1;
				return (arg0.getStatus().getSortValue().compareTo(arg1.getStatus().getSortValue()));
			}
		});
				
		return rv;
	}
	
	/**
	 * Get a list of basic participant information for the site who is in the group
	 * 
	 * @param context
	 *        The site id.
	 *        
	 * @param groupId
	 * 		  The group id
	 *        
	 * @param includeAll
	 *        if true, include instructors and ta's too, else leave them out.
	 *        
	 * @return The List of Participants for qualified users in the site who is in the group
	 */
	protected List<Participant> getParticipants(String context, String groupId, boolean includeAll)
	{
		ArrayList<Participant> rv = new ArrayList<Participant>();
		
		if (groupId == null || groupId.trim().length() == 0)
		{
			return rv;
		}
		
		try
		{
			Site site = this.siteService.getSite(context);
			
			String toolId = null;
			ToolConfiguration config = site.getToolForCommonId(JFORUM_TOOL_ID);
			if (config != null)
			{
				toolId = config.getId();
			}
			
			Set<Member> members = site.getMembers();
			for (Member m : members)
			{
				ParticipantStatus status = null;
				String userId = m.getUserId();
				String sortName = null;
				String displayId = null;
				try
				{
					User user = this.userDirectoryService.getUser(userId);
					sortName = user.getSortName();
				//	displayId = user.getDisplayId().trim();
					displayId = user.getIidInContext(context);
					if (displayId == null) displayId = user.getEid();
				}
				catch (UserNotDefinedException e)
				{
					// skip deleted users
					continue;
				}

				// outright skip guests
				if (m.getRole().getId().equalsIgnoreCase("guest"))
				{
					continue;
				}

				if (m.getRole().getId().equals("Blocked"))
				{
					status = ParticipantStatus.blocked;
				}

				else if (m.getRole().getId().equals("Observer"))
				{
					if (includeAll) status = ParticipantStatus.observer;
				}

				else if (site.isAllowed(userId, "section.role.student"))
				{
					if (!m.isProvided())
					{
						if (m.isActive())
						{
							status = ParticipantStatus.added;
						}
						else
						{
							status = ParticipantStatus.blocked;
						}
						
					}
					else
					{
						status = ParticipantStatus.enrolled;
					}
				}

				else if (site.isAllowed(userId, "section.role.instructor"))
				{
					if (includeAll) status = ParticipantStatus.instructor;
				}

				else if (site.isAllowed(userId, "section.role.ta"))
				{
					if (includeAll) status = ParticipantStatus.ta;
				}

				else if (!m.isActive())
				{
					// check for inactive users of the role that has this access
					Set roles = site.getRolesIsAllowed("section.role.student");
					if (roles.contains(m.getRole().getId()))
					{
						// inactive could-be-students that are provided are "dropped", but if they are not provided, they are "blocked"
						if (m.isProvided())
						{
							status = ParticipantStatus.dropped;
						}
						else
						{
							status = ParticipantStatus.blocked;
						}
					}
				}

				// which section is the user in? If in multiple, pick the one that is active, if any. Otherwise, just pick any.
				String groupTitle = null;
				String titleActive = null;
				String titleInactive = null;
				Collection groups = site.getGroups();
				for (Object groupO : groups)
				{
					Group g = (Group) groupO;

					// skip non-section groups
					// if (g.getProperties().getProperty("sections_category") == null) continue;

					if (g.getId().equalsIgnoreCase(groupId))
					{
						// we want to find the user even if not active, so we cannot use g.getUsers(), which only returns active users -ggolden
						// if (g.getUsers().contains(userId))
						Set<Member> groupMemebers = g.getMembers();
						
						for (Member gm : groupMemebers)
						{
							if (gm.getUserId().equals(userId))
							{
								if (gm.isActive())
								{
									if (titleActive == null) titleActive = g.getTitle();
								}
								else
								{
									if (titleInactive == null) titleInactive = g.getTitle();
								}
							}
						}
					}
				}
				
				if (titleActive != null)
				{
					groupTitle = titleActive;
				}
				else if (titleInactive != null)
				{
					groupTitle = titleInactive;
				}
				
				if (groupTitle == null || groupTitle.trim().length() == 0)
				{
					continue;
				}

				// take only those who have a status set (this skips the non-students)
				if (status != null)
				{
					ParticipantImpl p = new ParticipantImpl();
					p.userId = userId;
					p.status = status;
					p.sortName = sortName;
					p.displayId = displayId;
					p.groupTitle = groupTitle;
					
					if (toolId != null && (toolId.trim().length() > 0))
					{
						setPrivateMessageLink(site.getId(), toolId, p);
					}
					
					rv.add(p);
				}
			}
		}
		catch (IdUnusedException e)
		{
			logger.warn("getParticipants: missing site: " + context);
		}
		
		// sort by status
		Collections.sort(rv, new Comparator<Participant>()
		{
			public int compare(Participant arg0, Participant arg1)
			{
				if ((arg0.getStatus() == null) && (arg1.getStatus() == null)) return 0;
				if (arg0.getStatus() == null) return -1;
				if (arg1.getStatus() == null) return 1;
				return (arg0.getStatus().getSortValue().compareTo(arg1.getStatus().getSortValue()));
			}
		});
		
		return rv;
	}

	/**
	 * Key name to store user grade item in the thread
	 * 
	 * @param context	Context
	 * 
	 * @param userId	User id
	 * 
	 * @return	Key name
	 */
	protected String getUserGradeKey(String context, String userId)
	{
		if (context == null || context.trim().length() == 0 || userId == null || userId.trim().length() == 0 || userId == null)
		{
			return null;
		}
		
		return "etudesgradebook:getUserGradeKey:"+ context +":"+ userId;
	}
	
	/**
	 * Key name to store user tool gradebook items by type in thread
	 * 
	 * @param context	Context
	 * 
	 * @param userId	User id
	 * 
	 * @param itemType	Item type
	 * 
	 * @return	Key name
	 */
	protected String getUserToolGradebookItemsByTypeKey(String context, String userId, GradebookItemType itemType)
	{
		if (context == null || context.trim().length() == 0 || userId == null || userId.trim().length() == 0 || itemType == null)
		{
			return null;
		}
		return "etudesgradebook:userToolGradebookItems:"+ context +":"+ userId +":"+ itemType.getId();
	}
	
	/**
	 * Key name to store user tool gradebook items in thread
	 * 
	 * @param context	Context
	 * 
	 * @param userId	User id
	 * 
	 * @return	Key name
	 */
	protected String getUserToolGradebookItemsKey(String context, String userId)
	{
		if (context == null || context.trim().length() == 0 || userId == null || userId.trim().length() == 0 || userId == null)
		{
			return null;
		}
		
		return "etudesgradebook:userToolGradebookItems:"+ context +":"+ userId;
	}
	
	/**
	 * Maps items to categories
	 * 
	 * @param context	Context
	 * 
	 * @param userId	User id
	 * 
	 * @param participantGradebookItems	Participant gradebook items
	 */
	protected void mapStudentItems(String context, String userId, List<ParticipantGradebookItem> participantGradebookItems)
	{
		if ((context == null || context.trim().length() == 0) || (userId == null || userId.trim().length() == 0) || (participantGradebookItems == null || participantGradebookItems.size() == 0))
		{
			return;
		}

		Gradebook gradebook = getContextGradebook(context, userId);
		int mappedItemsCount = this.storage.selectGradebookCategoryMappedItemsCount(gradebook.getId(), gradebook.getCategoryType());

		List<GradebookCategory> gradebookCategories = gradebook.getGradebookCategories();
		Map<Integer, GradebookCategory> gradebookCategoryCodeMap = new HashMap<Integer, GradebookCategory>();
		Map<Integer, GradebookCategory> gradebookCategoryIdMap = new HashMap<Integer, GradebookCategory>();

		if (gradebookCategories != null && gradebookCategories.size() > 0)
		{
			for (GradebookCategory gradebookCategory : gradebookCategories)
			{
				// each gradebook has only one category type in use
				gradebookCategoryCodeMap.put(gradebookCategory.getStandardCategoryCode(), gradebookCategory);
				gradebookCategoryIdMap.put(gradebookCategory.getId(), gradebookCategory);
			}
		}

		// mapped items
		List<GradebookCategoryItemMap> gradebookCategoryItemMapList = this.storage.selectGradebookCategoryMappedItems(gradebook.getId(), gradebook.getCategoryType());
				
		// do new mapping to be created as user may not have access to all items
		if (mappedItemsCount == 0)
		{
			// user items may not be all gradable items but create mapping as
			// items are available
			List<GradebookCategoryItemMap> contextCategoryMapItems = new ArrayList<GradebookCategoryItemMap>();

			for (ParticipantGradebookItem participantGradebookItem : participantGradebookItems)
			{
				if (participantGradebookItem.getGradebookItem() != null)
				{
					GradebookCategoryItemMap gradebookCategoryItemMap = createNewMapStandardItem(gradebookCategoryCodeMap, participantGradebookItem.getGradebookItem());

					if (gradebookCategoryItemMap != null)
					{
						contextCategoryMapItems.add(gradebookCategoryItemMap);
					}
				}
			}
			
			int order = 1;
			Collections.sort(contextCategoryMapItems, new GradebookCategoryItemMapComparator(true));
			for (GradebookCategoryItemMap gradebookCategoryItemMap : contextCategoryMapItems)
			{
				((GradebookCategoryItemMapImpl)gradebookCategoryItemMap).setDisplayOrder(order++);
			}

			// create map
			this.storage.addModifyDeleteGradebookCategoryMappedItems(context, gradebook.getCategoryType(), contextCategoryMapItems);
		}
		else
		{
			Map<String, GradebookCategoryItemMap> gradebookCategoryItemMapMap = new HashMap<String, GradebookCategoryItemMap>();
			for (GradebookCategoryItemMap gradebookCategoryItemMap : gradebookCategoryItemMapList)
			{
				gradebookCategoryItemMapMap.put(gradebookCategoryItemMap.getItemId(), gradebookCategoryItemMap);		
			}
			
			/* new items may get added after map was updated and accessed by instructor. If instructor doesn't access after new items are added the map is stale.*/
			// get existing map from database and check if any item is not in the map thenreset the map
			GradebookItem gradebookItem = null;
			for (ParticipantGradebookItem participantGradebookItem : participantGradebookItems)
			{
				if (participantGradebookItem.getGradebookItem() != null)
				{
					gradebookItem = participantGradebookItem.getGradebookItem();
					
					if (!gradebookCategoryItemMapMap.containsKey(gradebookItem.getId()))
					{
						// fetch all gradable items and reset the map
						fetchToolGradableItemsAndResetItemMap(context, userId);
						
						// get updated map list
						gradebookCategoryItemMapList = this.storage.selectGradebookCategoryMappedItems(gradebook.getId(), gradebook.getCategoryType());
						break;
					}
				}				
			}
		}

		// add category to gradebookItem
		if (gradebookCategoryItemMapList.size() > 0)
		{
			Map<String, GradebookCategoryItemMap> gradebookCategoryItemsMap = new HashMap<String, GradebookCategoryItemMap>();
			for (GradebookCategoryItemMap gradebookCategoryItemMap : gradebookCategoryItemMapList)
			{
				gradebookCategoryItemsMap.put(gradebookCategoryItemMap.getItemId(), gradebookCategoryItemMap);
			}

			GradebookCategoryItemMap mappedItem = null;
			GradebookItem gradebookItem = null;
			for (ParticipantGradebookItem participantGradebookItem : participantGradebookItems)
			{
				if (participantGradebookItem.getGradebookItem() != null)
				{
					gradebookItem = participantGradebookItem.getGradebookItem();
					mappedItem = gradebookCategoryItemsMap.get(gradebookItem.getId());

					GradebookCategory mappedItemCategory = null;
					if (mappedItem != null)
					{
						mappedItemCategory = gradebookCategoryIdMap.get(mappedItem.getCategoryId());

						if (mappedItemCategory != null)
						{
							((GradebookItemImpl) gradebookItem).setGradebookCategory(mappedItemCategory);
							((GradebookItemImpl) gradebookItem).setDisplayOrder(mappedItem.getDisplayOrder());
						}
					}
				}
			}
		}

	}

	/**
	 * @param gradebook
	 * @param actualUserGrade
	 */
	protected void mapUsergradeToGradingScale(Gradebook gradebook, UserGradeImpl actualUserGrade)
	{
		Float averagePercent;
		// get the letter grade
		GradingScale gradingScale = gradebook.getGradingScale();
		averagePercent = actualUserGrade.getAveragePercent();
		
		if (gradingScale != null && averagePercent != null)
		{
			if (averagePercent < 0)
			{
				averagePercent = 0f;
			}
			
			List<GradingScalePercent> gradingScalePercentList = gradingScale.getGradingScalePercent();
			
			// assuming gradingScalePercentList from highest to lowest
			for (GradingScalePercent gradingScalePercent : gradingScalePercentList)
			{
				/* the value 0 if anotherFloat is numerically equal to this Float; a value less than 0 if this Float is numerically less 
				than anotherFloat; and a value greater than 0 if this Float is numerically greater than anotherFloat.*/
				if (averagePercent.compareTo(gradingScalePercent.getPercent()) == 0)
				{
					// actualUserGrade.setAveragePercent(averagePercent);
					
					if (actualUserGrade.getLetterGrade() == null)
					{
						actualUserGrade.setLetterGrade(gradingScalePercent.getLetterGrade());
					}
					return;						
				}
				else if (averagePercent.compareTo(gradingScalePercent.getPercent()) < 0)
				{
				}
				else if (averagePercent.compareTo(gradingScalePercent.getPercent()) > 0)
				{
					// actualUserGrade.setAveragePercent(averagePercent);
					
					if (actualUserGrade.getLetterGrade() == null)
					{
						actualUserGrade.setLetterGrade(gradingScalePercent.getLetterGrade());
					}
					
					return;
				}
			}
		}
	}

	/**
	 *	Map items to categories. May need to update when fetched. When items are not mapped use standard category code of category to map to the gradebook item type to map item with category. 
				
	 * @param context		Context
	 * 
	 * @param userId		User id
	 * 
	 * @param gradebookItems	Gradebook items
	 */
	protected void mergeItems(String context, String userId, List<GradebookItem> gradebookItems, boolean changed, boolean resetDisplayOrder)
	{
		if ((context == null || context.trim().length() == 0) ||  (userId == null || userId.trim().length() == 0) ||  (gradebookItems == null))
		{
			return;
		}
		
		Gradebook gradebook = getContextGradebook(context, userId);
		
		if (gradebookItems.size() > 0)
		{
			int mappedItemsCount = this.storage.selectGradebookCategoryMappedItemsCount(gradebook.getId(), gradebook.getCategoryType());
			
			List<GradebookCategory> gradebookCategories = gradebook.getGradebookCategories();
			Map<Integer, GradebookCategory> gradebookCategoryCodeMap = new HashMap<Integer, GradebookCategory>();
			Map<Integer, GradebookCategory> gradebookCategoryIdMap = new HashMap<Integer, GradebookCategory>();
			
			if (gradebookCategories != null && gradebookCategories.size() > 0)
			{
				for (GradebookCategory gradebookCategory : gradebookCategories)
				{
					// each gradebook has only one category type in use
					gradebookCategoryCodeMap.put(gradebookCategory.getStandardCategoryCode(), gradebookCategory);
					gradebookCategoryIdMap.put(gradebookCategory.getId(), gradebookCategory);
				}
			}
			
			if (mappedItemsCount == 0)
			{
				// sort by due date before map is created
				Collections.sort(gradebookItems, new GradebookComparator(Gradebook.GradebookSortType.DueDate, false));
				
				List<GradebookCategoryItemMap> contextCategoryMapItems = new ArrayList<GradebookCategoryItemMap>();
				
				// display order should be items belong to first in the category order then second etc
				for (GradebookItem gradebookItem : gradebookItems)
				{
					GradebookCategoryItemMap gradebookCategoryItemMap = createNewMapStandardItem(gradebookCategoryCodeMap, gradebookItem);
					
					if (gradebookCategoryItemMap != null)
					{
						// ((GradebookCategoryItemMapImpl)gradebookCategoryItemMap).setDisplayOrder(order++);						
						contextCategoryMapItems.add(gradebookCategoryItemMap);
					}
				}
				
				// set the display order. Display order should be items belong to first in the category order then second etc
				int order = 1;
				Collections.sort(contextCategoryMapItems, new GradebookCategoryItemMapComparator(true));
				for (GradebookCategoryItemMap gradebookCategoryItemMap : contextCategoryMapItems)
				{
					((GradebookCategoryItemMapImpl)gradebookCategoryItemMap).setDisplayOrder(order++);
				}
				
				// create map
				this.storage.addModifyDeleteGradebookCategoryMappedItems(context, gradebook.getCategoryType(), contextCategoryMapItems);				
			}
			else
			{
				// check existing map and update(add or remove from existing mapped items with gradebook categories) if changed
				List<GradebookCategoryItemMap> gradebookCategoryItemMapList = this.storage.selectGradebookCategoryMappedItems(gradebook.getId(), gradebook.getCategoryType());
				
				//map of mapped items already mapped
				Map<String, GradebookCategoryItemMap> gradebookCategoryItemMapListMap = new HashMap<String, GradebookCategoryItemMap>();
				for (GradebookCategoryItemMap gradebookCategoryMapItem : gradebookCategoryItemMapList)
				{
					gradebookCategoryItemMapListMap.put(gradebookCategoryMapItem.getItemId(), gradebookCategoryMapItem);
				}
				
				//changed = false;
				
				List<GradebookCategoryItemMap> gradebookCategoryItemMapNewList = new ArrayList<GradebookCategoryItemMap>();
				GradebookCategory mappedItemCategory = null;
				for (GradebookItem gradebookItem : gradebookItems)
				{
					if (gradebookCategoryItemMapListMap.containsKey(gradebookItem.getId()))
					{
						GradebookCategoryItemMap exisGradebookCategoryMapItem = gradebookCategoryItemMapListMap.remove(gradebookItem.getId());
						
						mappedItemCategory = null;
						mappedItemCategory = gradebookCategoryIdMap.get(exisGradebookCategoryMapItem.getCategoryId());
						
						if (mappedItemCategory != null)
						{
							((GradebookCategoryItemMapImpl)exisGradebookCategoryMapItem).setCategory(mappedItemCategory);
						}
						gradebookCategoryItemMapNewList.add(exisGradebookCategoryMapItem);
					}
					else
					{
						// add new item
						GradebookCategoryItemMap gradebookCategoryItemMap = createNewMapStandardItem(gradebookCategoryCodeMap, gradebookItem);
						
						if (gradebookCategoryItemMap != null)
						{
							gradebookCategoryItemMapNewList.add(gradebookCategoryItemMap);
							changed = true;
						}
					}
				}
				
				// remove already unpublished mapped item
				for (Iterator<Map.Entry<String, GradebookCategoryItemMap>> iter = gradebookCategoryItemMapListMap.entrySet().iterator(); iter.hasNext();)
				{
					Map.Entry<String, GradebookCategoryItemMap> entry = iter.next();
					// if (entry.getValue().getDisplayOrder() == 0)
					if (entry.getValue().getDisplayOrder() > 0)
					{
						iter.remove();
					}
				}
				
				// retain unpublished items
				Map<String, GradebookCategoryItemMap> unpublishedOrRemovedItems = new HashMap<String, GradebookCategoryItemMap>();
				
				for (GradebookCategoryItemMap existMapItem : gradebookCategoryItemMapList)
				{
					if (!gradebookCategoryItemMapNewList.contains(existMapItem))
					{
						logger.debug("add unpublished item to new list" + existMapItem.toString());
						//existMapItem.setStatus("unpublish");
						unpublishedOrRemovedItems.put(existMapItem.getItemId(), existMapItem);
						gradebookCategoryItemMapNewList.add(existMapItem);						
					}
				}
				
				if (gradebookCategoryItemMapListMap.size() > 0)
				{
					changed = true;
				}
				
				if (changed)
				{
					// set the display order. Display order should be items belong to first in the category order then second etc
					Collections.sort(gradebookCategoryItemMapNewList, new GradebookCategoryItemMapComparator(true));
					
					int loopCount = 0;
					for (GradebookCategoryItemMap gradebookCategoryItemMap : gradebookCategoryItemMapNewList)
					{
						loopCount++;
						// if ("unpublish".equals(gradebookCategoryItemMap.getStatus()))
						if (unpublishedOrRemovedItems.containsKey(gradebookCategoryItemMap.getItemId()))
						{
							//gradebookCategoryItemMap.setDisplayOrder(0);
						}
						else
						{
							gradebookCategoryItemMap.setDisplayOrder(loopCount);
						}
					}
										
					// save updated merged list
					this.storage.addModifyDeleteGradebookCategoryMappedItems(context, gradebook.getCategoryType(), gradebookCategoryItemMapNewList);
				}
			}
			
			// add category to gradebookItem
			List<GradebookCategoryItemMap> gradebookCategoryItemMapList = this.storage.selectGradebookCategoryMappedItems(gradebook.getId(), gradebook.getCategoryType());
			if (gradebookCategoryItemMapList.size() > 0)
			{
				Map<String, GradebookCategoryItemMap> gradebookCategoryItemsMap = new HashMap<String, GradebookCategoryItemMap>();
				for (GradebookCategoryItemMap gradebookCategoryItemMap : gradebookCategoryItemMapList)
				{
					gradebookCategoryItemsMap.put(gradebookCategoryItemMap.getItemId(), gradebookCategoryItemMap);
				}
				
				GradebookCategoryItemMap mappedItem = null;
				GradebookCategory mappedItemCategory = null;
				for (GradebookItem gradebookItem : gradebookItems)
				{
					mappedItem = gradebookCategoryItemsMap.get(gradebookItem.getId());
					
					mappedItemCategory = null;
					if (mappedItem != null)
					{
						mappedItemCategory = gradebookCategoryIdMap.get(mappedItem.getCategoryId());
						
						if (mappedItemCategory != null)
						{
							((GradebookItemImpl)gradebookItem).setGradebookCategory(mappedItemCategory);
							((GradebookItemImpl)gradebookItem).setDisplayOrder(mappedItem.getDisplayOrder());
						}
					}
				}
			}
		}
		else
		{	
			// delete all mappings as there are no items
			this.storage.addModifyDeleteGradebookCategoryMappedItems(context, gradebook.getCategoryType(), new ArrayList<GradebookCategoryItemMap>());
		}
	}
	
	/**
	 * Round to two decimals
	 * 
	 * @param number	The number that is to be rounded
	 * 
	 * @return	Rounded number
	 */
	protected Float roundToTwoDecimals(Float number)
	{
		if (number == null)
		{
			return null;
		}
		
		return Math.round(number * 100.0f) / 100.0f;		
	}
	
	/**
	 * Sets private message link
	 * 
	 * @param siteId	Site id
	 * 
	 * @param toolId	Tool id
	 * 
	 * @param userId	User id
	 * 
	 * @param participant	Participant
	 */
	protected void setPrivateMessageLink(String siteId, String toolId, Participant participant)
	{
		if ((siteId == null || siteId.trim().length() == 0) || (toolId == null || toolId.trim().length() == 0) || 
									(participant == null || participant.getUserId() == null || participant.getUserId().trim().length() == 0) 
									|| (participant.getStatus() == null || participant.getStatus() == ParticipantStatus.dropped || participant.getStatus() == ParticipantStatus.blocked ))
		{
			return;
		}
		// add private message link
		/*
		 	/portal/tool/toolId//pm/amSendTo/0/S:siteId:U:userId.page
		 	/portal/tool/835d1a21-77b6-4785-80cb-5d47c1d1bb8a/pm/amSendTo/0/S:2f6b4270-6919-408b-0069-821c95f2048a:U:618a98f7-82a5-4cb4-0028-dbd6d49fcc73.page

			"S" for site
			"U" is the single user id
		 */
		StringBuilder privateMessageLink = new StringBuilder();
		privateMessageLink.append("/portal/tool/");
		privateMessageLink.append(toolId); 					// tool id
		privateMessageLink.append("/pm/amSendTo/0/");
		privateMessageLink.append("S:"); 	
		privateMessageLink.append(siteId);		// site id
		privateMessageLink.append(":U:");
		privateMessageLink.append(participant.getUserId());
		privateMessageLink.append(".page");
		
		((ParticipantImpl)participant).setPrivateMessageLink(privateMessageLink.toString());
	}
}
