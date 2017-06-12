/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-plugin/plugin/src/java/org/etudes/gradebook/plugin/JForumItemProvider.java $
 * $Id: JForumItemProvider.java 12631 2016-02-17 22:37:16Z murthyt $
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
package org.etudes.gradebook.plugin;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.api.app.jforum.Category;
import org.etudes.api.app.jforum.Evaluation;
import org.etudes.api.app.jforum.Forum;
import org.etudes.api.app.jforum.Grade;
import org.etudes.api.app.jforum.JForumAccessException;
import org.etudes.api.app.jforum.JForumCategoryService;
import org.etudes.api.app.jforum.JForumForumService;
import org.etudes.api.app.jforum.JForumGradeService;
import org.etudes.api.app.jforum.JForumPostService;
import org.etudes.api.app.jforum.JForumSecurityService;
import org.etudes.api.app.jforum.JForumService;
import org.etudes.api.app.jforum.JForumSpecialAccessService;
import org.etudes.api.app.jforum.SpecialAccess;
import org.etudes.api.app.jforum.Topic;
import org.etudes.gradebook.api.GradebookItem;
import org.etudes.gradebook.api.GradebookItemAccessStatus;
import org.etudes.gradebook.api.GradebookItemType;
import org.etudes.gradebook.api.GradebookService;
import org.etudes.gradebook.api.Participant;
import org.etudes.gradebook.api.ParticipantGradebookItem;
import org.etudes.gradebook.api.ParticipantItemDetails;
import org.etudes.gradebook.api.UserItemSpecialAccess;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;

public class JForumItemProvider extends PluginItemProvider
{
	/** Our log. */
	private static Log logger = LogFactory.getLog(JForumItemProvider.class);

	protected static final String CATEGORY = "CAT";
	
	protected static final String FORUM = "FORUM";
	
	protected static final String TOPIC = "TOPIC";
	
	/** Dependency: GradebookService. */
	protected GradebookService gradebookService = null;
	
	/** Dependency: JForumCategoryService. */
	protected JForumCategoryService jforumCategoryService = null;
	
	/** Dependency: JForumForumService*/
	protected JForumForumService jforumForumService = null;
	
	/** Dependency: JForumGradeService. */
	protected JForumGradeService jforumGradeService = null;
	
	/** Dependency: JForumPostService*/
	protected JForumPostService jforumPostService = null;
	
	/** Dependency: JForumSecurityService. */
	protected JForumSecurityService jforumSecurityService = null;
	
	/** Dependency: JForumService. */
	protected JForumService jforumService = null;
	
	/** Dependency: JForumSpecialAccessService */
	protected JForumSpecialAccessService jforumSpecialAccessService = null;
	
	/** Dependency: SiteService. */
	protected SiteService siteService = null;
	
	/**
	 * Shutdown.
	 */
	public void destroy()
	{
		this.gradebookService.unregisterProvider(this);
		
		if (logger.isInfoEnabled())
		{
			logger.info("destroy()");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public List<GradebookItem> getGradableItems(String context, String fetchedByUserId, Set<String> activeParticipantIds, boolean includeScores, boolean fetchUnpublish, GradebookItemType itemType)
	{
		if (context == null || context.trim().length() == 0)
		{
			throw new IllegalArgumentException("context is missing.");
		}
		
		//TODO check user access
		
		if (!(itemType == null || itemType == GradebookItemType.category || itemType == GradebookItemType.forum || itemType == GradebookItemType.topic))
		{
			return null;
		}
		
		List<GradebookItem> gradebookItems = new ArrayList<GradebookItem>();
		
		String toolId = null;
		try
		{
			Site site = this.siteService.getSite(context);
			
			ToolConfiguration config = site.getToolForCommonId("sakai.jforum.tool");
			if (config != null) toolId = config.getId();
		}
		catch (IdUnusedException e)
		{
			logger.warn("getGradableItems: missing site: " + context);
		}

		// no tool id? No JForum in site!
		if (toolId == null) return gradebookItems;
		
		List<Category> categories = this.jforumService.getGradableItemsByContext(context);
		
		String id = null;
		String title = null;
		Float points = null;
		Date open = null;
		Boolean isHideUntilOpen = Boolean.FALSE;
		Date due = null;
		Date close = null;
		GradebookItemAccessStatus accessStatus = GradebookItemAccessStatus.published;
		
		for (Category category : categories)
		{
			if (category.isGradable())
			{
				Grade grade = category.getGrade();
				
				if (grade == null || !grade.isAddToGradeBook())
				{
					continue;
				}
				
				id = null;
				title = null;
				points = null;
				open = null;
				isHideUntilOpen = Boolean.FALSE;
				due = null;
				close = null;
				accessStatus = GradebookItemAccessStatus.published;
				
				id = JForumItemProvider.CATEGORY + "-" + String.valueOf(category.getId());
				title = category.getTitle();
				points = grade.getPoints();
				
				if (category.getAccessDates() != null)
				{
					open = category.getAccessDates().getOpenDate();
					due = category.getAccessDates().getDueDate();
					close = category.getAccessDates().getAllowUntilDate();
					
					if (open != null)
					{
						isHideUntilOpen = category.getAccessDates().isHideUntilOpen();
					}
				}
				
				// check for invalid (Note: categories cannot have "deny access"
				if (((open != null) || (due != null) || (close != null)) && (!category.getAccessDates().isDatesValid()))
				{
					accessStatus = GradebookItemAccessStatus.invalid;
					// ignore items invalid dates
					continue;
				}
				
				Date now = new Date();
				
				// ignore not open items
				/*
				if (isHideUntilOpen && open != null && now.before(open))
				{
					continue;
				}
				*/

				// access status
				if (category.getForums().size() == 0)
				{
					accessStatus = GradebookItemAccessStatus.published_hidden;
				}
				// access status - if category have dates
				else if ((open != null) || (due != null) || (close != null))
				{
					if (accessStatus == GradebookItemAccessStatus.published)
					{
						if ((open != null) && (now.before(open)))
						{
							if (isHideUntilOpen.booleanValue())
							{
								accessStatus = GradebookItemAccessStatus.published_hidden;
							}
							else
							{
								accessStatus = GradebookItemAccessStatus.published_not_yet_open;
							}
						}
						else if (close != null)
						{
							if (now.after(close))
							{
								accessStatus = GradebookItemAccessStatus.published_closed;
							}
						}
						else if (due != null)
						{
							if (now.after(due))
							{
								accessStatus = GradebookItemAccessStatus.published_closed;
							}
						}
					}
				}
				else
				{
					if (accessStatus == GradebookItemAccessStatus.published)
					{
						accessStatus = getCategoryAccessStatus(category);
					}
				}
				
				// number of posters
				int posters = 0;
				Map<String, Float> scores = new HashMap<String, Float>();
				Float averagePercent = null;
				
				if (includeScores)
				{
					Category evalCategory = this.jforumService.getEvaluationsByCategory(context, category.getId());
					
					List<Evaluation> evaluations = evalCategory.getEvaluations();
					
					if (evaluations != null && evaluations.size() > 0)
					{
						if (activeParticipantIds != null && activeParticipantIds.size() > 0)
						{
							Float totalScores = 0.0f;
							
							for (Evaluation evaluation : evaluations)
							{
								if (!activeParticipantIds.contains(evaluation.getSakaiUserId()))
								{
									continue;
								}
								
								if (evaluation.isReleased())
								{
									scores.put(evaluation.getSakaiUserId(), evaluation.getScore());
									
									if (evaluation.getScore() != null)
									{
										totalScores += evaluation.getScore();
									}
								}
								else
								{
									/* In the averages include zero assigned by gradebook to the non-submitters after closing date. 
									If the item is closed assign zero is there are no posts and no score*/
									
									if (close != null)
									{
										if (now.after(close) && evaluation.getTotalPosts() <= 0)
										{
											scores.put(evaluation.getSakaiUserId(), 0.0f);
											// totalScores += 0.0f;
										}
									}
									else if (due != null)
									{
										if (now.after(due) && evaluation.getTotalPosts() <= 0)
										{
											scores.put(evaluation.getSakaiUserId(), 0.0f);
											// totalScores += 0.0f;
										}
									}
								}
								
								if (evaluation.getTotalPosts() > 0)
								{
									posters++;
								}
							}
							
							if ((scores.size() > 0) && (points != null && points > 0))
							{
								averagePercent = ((totalScores/new Float(scores.size())) * 100) / (points);
							}
							
							if (averagePercent != null && averagePercent > 0)
							{
								averagePercent = Math.round(averagePercent * 100.0f) / 100.0f;
							}
						}
					}
				}
				
				Integer submittedCount = null;
				
				if (scores.size() > 0)
				{
					submittedCount = scores.size();
				}
				
				// make the item
				GradebookItem item = this.gradebookService.newGradebookItem(id, title, points, averagePercent, due, open, close, GradebookItemType.category, accessStatus, submittedCount);
				
				if (includeScores)
				{
					//scores
					Map<String, Float> itemScores = item.getScores();
					itemScores.clear();
					itemScores.putAll(scores);
				}
				
				gradebookItems.add(item);
			}
			else if (category.getForums().size() > 0)
			{
				for (Forum forum : category.getForums())
				{
					if (forum.getGradeType() == Grade.GradeType.FORUM.getType())
					{
						Grade grade = forum.getGrade();
						
						if (grade == null || !grade.isAddToGradeBook())
						{
							continue;
						}
						
						id = null;
						title = null;
						points = null;
						open = null;
						due = null;
						close = null;
						accessStatus = GradebookItemAccessStatus.published;
						boolean validDates = true;
						accessStatus = GradebookItemAccessStatus.published;
						
						id = JForumItemProvider.FORUM + "-" + String.valueOf(forum.getId());
						title = forum.getName();
						points = grade.getPoints();
						
						if (forum.getAccessDates() != null)
						{
							open = forum.getAccessDates().getOpenDate();
							due = forum.getAccessDates().getDueDate();
							close = forum.getAccessDates().getAllowUntilDate();
							validDates = forum.getAccessDates().isDatesValid();
							
							if (open != null)
							{
								isHideUntilOpen = forum.getAccessDates().isHideUntilOpen();
							}
						}
						
						if ((open == null && due == null && close == null) && (category.getAccessDates() != null) )
						{
							open = category.getAccessDates().getOpenDate();
							due = category.getAccessDates().getDueDate();
							close = category.getAccessDates().getAllowUntilDate();
							validDates = forum.getAccessDates().isDatesValid();
							
							if (open != null)
							{
								isHideUntilOpen = category.getAccessDates().isHideUntilOpen();
							}
						}
						
						// access status
						// check for invalid
						if (((open != null) || (due != null) || (close != null)) && (!validDates))
						{
							accessStatus = GradebookItemAccessStatus.invalid;
							// ignore items with invalid dates
							continue;
						}
						// otherwise check for deny access
						else
						{
							if (forum.getAccessType() == Forum.ACCESS_DENY)
							{
								// ignore access deny items
								accessStatus = GradebookItemAccessStatus.unpublished;
								continue;
							}
						}
						
						Date now = new Date();
						
						// ignore not open items
						/*
						if (isHideUntilOpen && open != null && now.before(open))
						{
							continue;
						}
						*/
						
						if ((open != null) || (due != null) || (close != null))
						{
							if (accessStatus == GradebookItemAccessStatus.published)
							{
								if ((open != null) && (now.before(open)))
								{
									if (isHideUntilOpen.booleanValue())
									{
										accessStatus = GradebookItemAccessStatus.published_hidden;
									}
									else
									{
										accessStatus = GradebookItemAccessStatus.published_not_yet_open;
									}
								}
								else if (close != null)
								{
									if (now.after(close))
									{
										accessStatus = GradebookItemAccessStatus.published_closed;
									}
								}
								else if (due != null)
								{
									if (now.after(due))
									{
										accessStatus = GradebookItemAccessStatus.published_closed;
									}
								}
							}
						}
						
						// number of posters
						int posters = 0;
						
						Map<String, Float> scores = new HashMap<String, Float>();
						Float averagePercent = null;
						
						if (includeScores)
						{
							// get users special access details
							Map<Integer, UserItemSpecialAccess> forumUserItemSpecialAccessList = new HashMap<Integer, UserItemSpecialAccess>();
							
							if (forum.getAccessDates() != null)
							{
								Date forumOpen = forum.getAccessDates().getOpenDate();
								Date forumDue = forum.getAccessDates().getDueDate();
								Date forumClose = forum.getAccessDates().getAllowUntilDate();
								boolean forumValidDates = forum.getAccessDates().isDatesValid();
								
								if (((forumOpen != null) || (forumDue != null) || (forumClose != null)) && (forumValidDates))
								{
									List<SpecialAccess> forumSpecialAccessList = this.jforumSpecialAccessService.getByForum(forum.getId());
									
									Date openDate = null; 
									Date dueDate = null; 
									Date acceptUntilDate = null;
									Boolean hideUntilOpen = null;
									Boolean overrideOpenDate = null;
									Boolean overrideDueDate = null;
									Boolean overrideAcceptUntilDate = null;
									Boolean overrideHideUntilOpen = null;
									Boolean datesValid = Boolean.TRUE;
									
									if (forumSpecialAccessList != null && !forumSpecialAccessList.isEmpty())
									{
										UserItemSpecialAccess userItemSpecialAccess = null;
																
										for (SpecialAccess forumSpecialAccess : forumSpecialAccessList)
										{
											if (forumSpecialAccess.getAccessDates() != null)
											{
												// forum special access
												if (forumSpecialAccess.getForumId() > 0 && forumSpecialAccess.getTopicId() == 0)
												{
													openDate = forumSpecialAccess.getAccessDates().getOpenDate();
													dueDate = forumSpecialAccess.getAccessDates().getDueDate();
													acceptUntilDate = forumSpecialAccess.getAccessDates().getAllowUntilDate();
													hideUntilOpen =  forumSpecialAccess.getAccessDates().isHideUntilOpen();
													overrideOpenDate = forumSpecialAccess.isOverrideStartDate();
													overrideDueDate = forumSpecialAccess.isOverrideEndDate();
													overrideAcceptUntilDate = forumSpecialAccess.isOverrideAllowUntilDate();
													overrideHideUntilOpen = forumSpecialAccess.isOverrideHideUntilOpen();
													datesValid = forumSpecialAccess.isForumSpecialAccessDatesValid(forum);
													
													for (Integer userId : forumSpecialAccess.getUserIds())
													{
														userItemSpecialAccess = this.gradebookService.newUserItemSpecialAccessImpl(openDate, dueDate, acceptUntilDate, hideUntilOpen, overrideOpenDate, overrideDueDate, overrideAcceptUntilDate, overrideHideUntilOpen, datesValid);
														
														forumUserItemSpecialAccessList.put(userId, userItemSpecialAccess);
													}
												}
											}
										}
									}
								}
							}
							
							Category forumCategory = this.jforumService.getEvaluationsByForum(context, forum.getId());
							
							if (forumCategory.getForums() != null)
							{
								List<Evaluation> evaluations = forumCategory.getForums().get(0).getEvaluations();
								
								if (activeParticipantIds != null && activeParticipantIds.size() > 0)
								{
									if (evaluations != null && evaluations.size() > 0)
									{
										Float totalScores = 0.0f;
										
										for (Evaluation evaluation : evaluations)
										{
											if (!activeParticipantIds.contains(evaluation.getSakaiUserId()))
											{
												continue;
											}
											
											if (evaluation.isReleased())
											{
												scores.put(evaluation.getSakaiUserId(), evaluation.getScore());
												
												if (evaluation.getScore() != null)
												{
													totalScores += evaluation.getScore();
												}
											}
											else
											{
												/* In the averages include zero assigned by gradebook to the non-submitters after closing date. Check special access dates.
												If the item is closed assign zero is there are no posts and no score*/
												
												Date itemLastSubmitDate = null;
												
												if (!forumUserItemSpecialAccessList.isEmpty())
												{
													UserItemSpecialAccess userItemSpecialAccess = forumUserItemSpecialAccessList.get(evaluation.getUserId());
													
													Date itemDueDate = null;
													Date itemCloseDate = null;
													
													// user special access dates
													Date itemUserSpecialAccessDueDate = null;
													Date itemUserSpecialAccessCloseDate = null;
													
													itemDueDate = due;
													itemCloseDate = close;
																										
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
												}
												else
												{
													if (close != null)
													{
														itemLastSubmitDate = close;
													}
													else if (due != null)
													{
														itemLastSubmitDate = due;
													}													
												}
												
												if (itemLastSubmitDate != null && now.after(itemLastSubmitDate) && evaluation.getTotalPosts() <= 0)
												{
													scores.put(evaluation.getSakaiUserId(), 0.0f);
													// totalScores += 0.0f;						
												}
											}
											
											if (evaluation.getTotalPosts() > 0)
											{
												posters++;
											}
										}
										
										if ((scores.size() > 0) && (points != null && points > 0))
										{
											averagePercent = ((totalScores/new Float(scores.size())) * 100) / (points);
										}
										
										if (averagePercent != null && averagePercent > 0)
										{
											averagePercent = Math.round(averagePercent * 100.0f) / 100.0f;
										}
									}
								}
							}
						}
						
						Integer submittedCount = null;
						
						if (scores.size() > 0)
						{
							submittedCount = scores.size();
						}
						
						// make the item
						GradebookItem item = this.gradebookService.newGradebookItem(id, title, points, averagePercent, due, open, close, GradebookItemType.forum, accessStatus, submittedCount);
						
						if (includeScores)
						{
							//scores
							Map<String, Float> itemScores = item.getScores();
							itemScores.clear();
							itemScores.putAll(scores);
						}
						
						gradebookItems.add(item);
					}
					else if (forum.getTopics().size() > 0)
					{
						for (Topic topic : forum.getTopics())
						{
							if (topic.isGradeTopic())
							{
								Grade grade = topic.getGrade();
								
								if (grade == null || !grade.isAddToGradeBook())
								{
									continue;
								}
								
								id = null;
								title = null;
								points = null;
								open = null;
								due = null;
								close = null;
								accessStatus = GradebookItemAccessStatus.published;
								
								id = JForumItemProvider.TOPIC + "-" + String.valueOf(topic.getId());
								title = topic.getTitle();
								points = grade.getPoints();
								isHideUntilOpen = Boolean.FALSE;
								boolean validDates = true;
								
								if (topic.getAccessDates() != null)
								{
									open = topic.getAccessDates().getOpenDate();
									due = topic.getAccessDates().getDueDate();
									close = topic.getAccessDates().getAllowUntilDate();
									validDates = topic.getAccessDates().isDatesValid();
									
									if (open != null)
									{
										isHideUntilOpen = topic.getAccessDates().isHideUntilOpen();
									}
								}
								
								if ((open == null && due == null && close == null) && (forum.getAccessDates() != null) )
								{
									open = forum.getAccessDates().getOpenDate();
									due = forum.getAccessDates().getDueDate();
									close = forum.getAccessDates().getAllowUntilDate();
									validDates = category.getAccessDates().isDatesValid();
									
									if (open != null)
									{
										isHideUntilOpen = forum.getAccessDates().isHideUntilOpen();
									}
								}
								
								if ((open == null && due == null && close == null) && (category.getAccessDates() != null) )
								{
									open = category.getAccessDates().getOpenDate();
									due = category.getAccessDates().getDueDate();
									close = category.getAccessDates().getAllowUntilDate();
									validDates = category.getAccessDates().isDatesValid();
									
									if (open != null)
									{
										isHideUntilOpen = category.getAccessDates().isHideUntilOpen();
									}
								}
								
								// access status
								if (((open != null) || (due != null) || (close != null)) && (!validDates))
								{
									accessStatus = GradebookItemAccessStatus.invalid;
									// ignore items with invalid dates
									continue;
								}
								// otherwise check for deny access
								else
								{
									if (forum.getAccessType() == Forum.ACCESS_DENY)
									{
										accessStatus = GradebookItemAccessStatus.unpublished;
										// ignore access deny items
										continue;
									}
								}
								
								Date now = new Date();
								
								// ignore not open items
								/*
								if (isHideUntilOpen && open != null && now.before(open))
								{
									continue;
								}
								*/
								
								// access status
								if ((open != null) || (due != null) || (close != null))
								{
									if (accessStatus == GradebookItemAccessStatus.published)
									{
										if ((open != null) && (now.before(open)))
										{
											if (isHideUntilOpen.booleanValue())
											{
												accessStatus = GradebookItemAccessStatus.published_hidden;
											}
											else
											{
												accessStatus = GradebookItemAccessStatus.published_not_yet_open;
											}
										}
										else if (close != null)
										{
											if (now.after(close))
											{
												accessStatus = GradebookItemAccessStatus.published_closed;
											}
										}
										else if (due != null)
										{
											if (now.after(due))
											{
												accessStatus = GradebookItemAccessStatus.published_closed;
											}
										}
									}
								}
								
								// number of posters
								int posters = 0;
								
								Map<String, Float> scores = new HashMap<String, Float>();
								Float averagePercent = null;
								
								if (includeScores)
								{
									// get users special access details
									Map<Integer, UserItemSpecialAccess> topicUserItemSpecialAccessList = new HashMap<Integer, UserItemSpecialAccess>();
									
									if (topic.getAccessDates() != null)
									{
										Date tipicOpen = topic.getAccessDates().getOpenDate();
										Date topicDue = topic.getAccessDates().getDueDate();
										Date topicClose = topic.getAccessDates().getAllowUntilDate();
										boolean topicValidDates = topic.getAccessDates().isDatesValid();
										
										if (((tipicOpen != null) || (topicDue != null) || (topicClose != null)) && (topicValidDates))
										{
											List<SpecialAccess> topicSpecialAccessList = this.jforumSpecialAccessService.getByTopic(topic.getForumId(), topic.getId());
											
											Date openDate = null; 
											Date dueDate = null; 
											Date acceptUntilDate = null;
											Boolean hideUntilOpen = null;
											Boolean overrideOpenDate = null;
											Boolean overrideDueDate = null;
											Boolean overrideAcceptUntilDate = null;
											Boolean overrideHideUntilOpen = null;
											Boolean datesValid = Boolean.TRUE;
											
											if (topicSpecialAccessList != null && !topicSpecialAccessList.isEmpty())
											{
												UserItemSpecialAccess userItemSpecialAccess = null;
																		
												for (SpecialAccess topicSpecialAccess : topicSpecialAccessList)
												{
													if (topicSpecialAccess.getAccessDates() != null)
													{
														// forum special access
														if (topicSpecialAccess.getForumId() > 0 && topicSpecialAccess.getTopicId() > 0)
														{
															openDate = topicSpecialAccess.getAccessDates().getOpenDate();
															dueDate = topicSpecialAccess.getAccessDates().getDueDate();
															acceptUntilDate = topicSpecialAccess.getAccessDates().getAllowUntilDate();
															hideUntilOpen =  topicSpecialAccess.getAccessDates().isHideUntilOpen();
															overrideOpenDate = topicSpecialAccess.isOverrideStartDate();
															overrideDueDate = topicSpecialAccess.isOverrideEndDate();
															overrideAcceptUntilDate = topicSpecialAccess.isOverrideAllowUntilDate();
															overrideHideUntilOpen = topicSpecialAccess.isOverrideHideUntilOpen();
															datesValid = topicSpecialAccess.isTopicSpecialAccessDatesValid(topic);
															
															for (Integer userId : topicSpecialAccess.getUserIds())
															{
																userItemSpecialAccess = this.gradebookService.newUserItemSpecialAccessImpl(openDate, dueDate, acceptUntilDate, hideUntilOpen, overrideOpenDate, overrideDueDate, overrideAcceptUntilDate, overrideHideUntilOpen, datesValid);
																
																topicUserItemSpecialAccessList.put(userId, userItemSpecialAccess);
															}
														}
													}
												}
											}
										}
									}
									
									Category topicCategory = this.jforumService.getEvaluationsByTopic(context, topic.getId());
									
									if (topicCategory.getForums() != null && topicCategory.getForums().get(0).getTopics() != null)
									{
										List<Evaluation> evaluations = topicCategory.getForums().get(0).getTopics().get(0).getEvaluations();
										
										if (activeParticipantIds != null && activeParticipantIds.size() > 0)
										{
											if (evaluations != null && evaluations.size() > 0)
											{
												Float totalScores = 0.0f;
												
												for (Evaluation evaluation : evaluations)
												{
													if (!activeParticipantIds.contains(evaluation.getSakaiUserId()))
													{
														continue;
													}
													
													if (evaluation.isReleased())
													{
														scores.put(evaluation.getSakaiUserId(), evaluation.getScore());
														
														if (evaluation.getScore() != null)
														{
															totalScores += evaluation.getScore();
														}
													}
													else
													{
														/* In the averages include zero assigned by gradebook to the non-submitters after closing date. Check special access dates. 
														If the item is closed assign zero is there are no posts and no score*/
														Date itemLastSubmitDate = null;
														
														if (!topicUserItemSpecialAccessList.isEmpty())
														{
															UserItemSpecialAccess userItemSpecialAccess = topicUserItemSpecialAccessList.get(evaluation.getUserId());
															
															Date itemDueDate = null;
															Date itemCloseDate = null;
															
															// user special access dates
															Date itemUserSpecialAccessDueDate = null;
															Date itemUserSpecialAccessCloseDate = null;
															
															itemDueDate = due;
															itemCloseDate = close;
																												
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
														}
														else
														{
															if (close != null)
															{
																itemLastSubmitDate = close;
															}
															else if (due != null)
															{
																itemLastSubmitDate = due;
															}													
														}
														
														if (itemLastSubmitDate != null && now.after(itemLastSubmitDate) && evaluation.getTotalPosts() <= 0)
														{
															scores.put(evaluation.getSakaiUserId(), 0.0f);
															// totalScores += 0.0f;						
														}
													}
													
													if (evaluation.getTotalPosts() > 0)
													{
														posters++;
													}
												}
												
												if ((scores.size() > 0) && (points != null && points > 0))
												{
													averagePercent = ((totalScores/new Float(scores.size())) * 100) / (points);
												}
												
												if (averagePercent != null && averagePercent > 0)
												{
													averagePercent = Math.round(averagePercent * 100.0f) / 100.0f;
												}
											}
										}
									}
								}
								
								Integer submittedCount = null;
								
								if (scores.size() > 0)
								{
									submittedCount = scores.size();
								}
								
								// make the item
								GradebookItem item = this.gradebookService.newGradebookItem(id, title, points, averagePercent, due, open, close, GradebookItemType.topic, accessStatus, submittedCount);
								
								if (includeScores)
								{
									//scores
									Map<String, Float> itemScores = item.getScores();
									itemScores.clear();
									itemScores.putAll(scores);
								}
								gradebookItems.add(item);
							}
						}
					}
				}
			}
		}
	
		return gradebookItems;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public GradebookItem getJForumGradableItem(String context, String itemId, String fetchedByUserId, Set<String> activeParticipantIds, boolean includeScores)
	{
		if ((itemId == null || itemId.trim().length() == 0) || (context == null || context.trim().length() == 0))
		{
			throw new IllegalArgumentException("item information or context is missing.");
		}
		
		//TODO check user access
		
		boolean categoryItem = itemId.startsWith(CATEGORY + "-");
		boolean forumItem = itemId.startsWith(FORUM + "-");
		boolean topicItem = itemId.startsWith(TOPIC + "-");
		
		GradebookItem gradebookItem = null;
		
		String toolId = null;
		try
		{
			Site site = this.siteService.getSite(context);
			
			ToolConfiguration config = site.getToolForCommonId("sakai.jforum.tool");
			if (config != null) toolId = config.getId();
		}
		catch (IdUnusedException e)
		{
			logger.warn("getJForumGradableItem: missing site: " + context);
		}

		// no tool id? No JForum in site!
		if (toolId == null) return gradebookItem;
		
		int id;
		try
		{
			id = Integer.parseInt(itemId.substring(itemId.indexOf("-") + 1));

		}
		catch (NumberFormatException e)
		{
			logger.warn("error in parsing of item id.", e);
			return null;
		}
		
		String title = null;
		Float points = null;
		Date open = null;
		Date due = null;
		Date close = null;
		Boolean isHideUntilOpen = Boolean.FALSE;
		GradebookItemAccessStatus accessStatus = GradebookItemAccessStatus.published;
		boolean validDates = true;
		
		if (categoryItem)
		{
			// get item and evaluations
			//List<org.etudes.api.app.jforum.Evaluation> evaluations = jforumGradeService.getCategoryEvaluationsWithPosts(categoryId, evalSort, UserDirectoryService.getCurrentUser().getId(), true);
			
			Category category = this.jforumService.getEvaluationsByCategory(context, id);
			
			if (category == null)
			{
				return null;
			}
			
			if (!category.isGradable())
			{
				return null;
			}
			
			title = null;
			points = null;
			open = null;
			due = null;
			close = null;
			
			title = category.getTitle();
			points = category.getGrade().getPoints();
			
			if (category.getAccessDates() != null)
			{
				open = category.getAccessDates().getOpenDate();
				due = category.getAccessDates().getDueDate();
				close = category.getAccessDates().getAllowUntilDate();
				validDates = category.getAccessDates().isDatesValid();
				
				if (open != null)
				{
					isHideUntilOpen = category.getAccessDates().isHideUntilOpen();
				}
			}
			
			Date now = new Date();
			
			// access status
			if ((open != null) || (due != null) || (close != null))
			{
				if (accessStatus == GradebookItemAccessStatus.published)
				{
					if ((open != null) && (now.before(open)))
					{
						if (isHideUntilOpen.booleanValue())
						{
							accessStatus = GradebookItemAccessStatus.published_hidden;
						}
						else
						{
							accessStatus = GradebookItemAccessStatus.published_not_yet_open;
						}
					}
					else if (close != null)
					{
						if (now.after(close))
						{
							accessStatus = GradebookItemAccessStatus.published_closed;
						}
					}
					else if (due != null)
					{
						if (now.after(due))
						{
							accessStatus = GradebookItemAccessStatus.published_closed;
						}
					}
				}
			}
			
			if(!validDates)
			{
				accessStatus = GradebookItemAccessStatus.invalid;
			}
			
			Float averagePercent = null;
			Map<String, Float> scoresMap = null;
			
			if (includeScores)
			{
				scoresMap = new HashMap<String, Float>();
				
				if (activeParticipantIds != null && activeParticipantIds.size() > 0)
				{
					for (String participantId : activeParticipantIds)
					{
						scoresMap.put(participantId, null);
					}
				}
				
				List<Evaluation> evaluations = category.getEvaluations();
				
				if (evaluations != null && evaluations.size() > 0)
				{
					if (activeParticipantIds != null && activeParticipantIds.size() > 0)
					{
						Float totalScores = 0.0f;
						
						for (Evaluation evaluation : evaluations)
						{
							if (!activeParticipantIds.contains(evaluation.getSakaiUserId()))
							{
								continue;
							}
							
							if (evaluation.isReleased())
							{
								scoresMap.put(evaluation.getSakaiUserId(), evaluation.getScore());
								
								if (evaluation.getScore() != null)
								{
									totalScores += evaluation.getScore();
								}
							}
						}
						
						if ((scoresMap.size() > 0) && (points != null && points > 0))
						{
							averagePercent = ((totalScores/new Float(scoresMap.size())) * 100) / (points);
							
							averagePercent = Math.round(averagePercent * 100.0f) / 100.0f;
						} 
					}
				}
			}
			
			// make the item
			gradebookItem = this.gradebookService.newGradebookItem(itemId, title, points, averagePercent, due, open, close, GradebookItemType.category, accessStatus, null);
			
			if (includeScores)
			{
				Map<String, Float> scores = gradebookItem.getScores();
				scores.clear();
				scores.putAll(scoresMap);
			}
		}
		else if (forumItem)
		{
			// get item and evaluations
			Category category = this.jforumService.getEvaluationsByForum(context, id);
			
			if (category == null)
			{
				return null;
			}
			
			if (category.getForums().isEmpty() || category.getForums().size() > 1)
			{
				return null;
			}
			
			Forum forum = category.getForums().get(0);
			
			if (forum.getGradeType() != Grade.GradeType.FORUM.getType())
			{
				return null;
			}
			
			title = null;
			points = null;
			open = null;
			due = null;
			close = null;
			
			title = forum.getName();
			points = forum.getGrade().getPoints();
			
			if (forum.getAccessDates() != null)
			{
				open = forum.getAccessDates().getOpenDate();
				due = forum.getAccessDates().getDueDate();
				close = forum.getAccessDates().getAllowUntilDate();				
				validDates = forum.getAccessDates().isDatesValid();
				
				if (open != null)
				{
					isHideUntilOpen = forum.getAccessDates().isHideUntilOpen();
				}
			}
			
			if ((open == null && due == null && close == null) && (category.getAccessDates() != null))
			{
				open = category.getAccessDates().getOpenDate();
				due = category.getAccessDates().getDueDate();
				close = category.getAccessDates().getAllowUntilDate();
				validDates = category.getAccessDates().isDatesValid();
				
				if (open != null)
				{
					isHideUntilOpen = category.getAccessDates().isHideUntilOpen();
				}
			}
			
			Date now = new Date();
			
			// access status
			if ((open != null) || (due != null) || (close != null))
			{
				if (accessStatus == GradebookItemAccessStatus.published)
				{
					if ((open != null) && (now.before(open)))
					{
						if (isHideUntilOpen.booleanValue())
						{
							accessStatus = GradebookItemAccessStatus.published_hidden;
						}
						else
						{
							accessStatus = GradebookItemAccessStatus.published_not_yet_open;
						}
					}
					else if (close != null)
					{
						if (now.after(close))
						{
							accessStatus = GradebookItemAccessStatus.published_closed;
						}
					}
					else if (due != null)
					{
						if (now.after(due))
						{
							accessStatus = GradebookItemAccessStatus.published_closed;
						}
					}
				}
			}
			
			if(!validDates)
			{
				accessStatus = GradebookItemAccessStatus.invalid;
			}
			
			Float averagePercent = null;
			Map<String, Float> scoresMap = null;
			
			if (includeScores)
			{
				scoresMap = new HashMap<String, Float>();
				
				if (activeParticipantIds != null && activeParticipantIds.size() > 0)
				{
					for (String participantId : activeParticipantIds)
					{
						scoresMap.put(participantId, null);
					}
				}
				
				List<Evaluation> evaluations = category.getEvaluations();
				
				if (evaluations != null && evaluations.size() > 0)
				{
					if (activeParticipantIds != null && activeParticipantIds.size() > 0)
					{
						Float totalScores = 0.0f;
						
						for (Evaluation evaluation : evaluations)
						{
							if (!activeParticipantIds.contains(evaluation.getSakaiUserId()))
							{
								continue;
							}
							
							if (evaluation.isReleased())
							{
								scoresMap.put(evaluation.getSakaiUserId(), evaluation.getScore());
								
								if (evaluation.getScore() != null)
								{
									totalScores += evaluation.getScore();
								}
							}
						}
						
						if ((scoresMap.size() > 0) && (points != null && points > 0))
						{
							averagePercent = ((totalScores/new Float(scoresMap.size())) * 100) / (points);
							
							averagePercent = Math.round(averagePercent * 100.0f) / 100.0f;
						} 
					}
				}
			}
			
			// make the item
			gradebookItem = this.gradebookService.newGradebookItem(itemId, title, points, averagePercent, due, open, close, GradebookItemType.forum, accessStatus, null);
			
			if (includeScores)
			{
				Map<String, Float> scores = gradebookItem.getScores();
				scores.clear();
				scores.putAll(scoresMap);
			}
		}
		else if (topicItem)
		{
			// get item and evaluations
			Category category = this.jforumService.getEvaluationsByTopic(context, id);
			
			if (category == null)
			{
				return null;
			}
			
			if (category.getForums().isEmpty() || category.getForums().size() > 1)
			{
				return null;
			}
			
			Forum forum = category.getForums().get(0);
			
			if (forum.getGradeType() != Grade.GradeType.TOPIC.getType())
			{
				return null;
			}
			
			if (forum.getTopics().isEmpty() || forum.getTopics().size() > 1)
			{
				return null;
			}
			
			Topic topic = forum.getTopics().get(0);
			
			if (!topic.isGradeTopic())
			{
				return null;
			}
			
			title = null;
			points = null;
			open = null;
			due = null;
			close = null;
			
			title = topic.getTitle();
			points = topic.getGrade().getPoints();
			
			if (topic.getAccessDates() != null)
			{
				open = topic.getAccessDates().getOpenDate();
				due = topic.getAccessDates().getDueDate();
				close = topic.getAccessDates().getAllowUntilDate();
				validDates = topic.getAccessDates().isDatesValid();
				
				if (open != null)
				{
					isHideUntilOpen = topic.getAccessDates().isHideUntilOpen();
				}
			}
			
			if ((open == null && due == null && close == null) && (forum.getAccessDates() != null))
			{
				open = forum.getAccessDates().getOpenDate();
				due = forum.getAccessDates().getDueDate();
				close = forum.getAccessDates().getAllowUntilDate();
				validDates = forum.getAccessDates().isDatesValid();
				
				if (open != null)
				{
					isHideUntilOpen = forum.getAccessDates().isHideUntilOpen();
				}
			}
			
			if ((open == null && due == null && close == null) && (category.getAccessDates() != null))
			{
				open = category.getAccessDates().getOpenDate();
				due = category.getAccessDates().getDueDate();
				close = category.getAccessDates().getAllowUntilDate();
				validDates = category.getAccessDates().isDatesValid();
				
				if (open != null)
				{
					isHideUntilOpen = category.getAccessDates().isHideUntilOpen();
				}
			}
			
			Date now = new Date();
			
			// access status
			if ((open != null) || (due != null) || (close != null))
			{
				if (accessStatus == GradebookItemAccessStatus.published)
				{
					if ((open != null) && (now.before(open)))
					{
						if (isHideUntilOpen.booleanValue())
						{
							accessStatus = GradebookItemAccessStatus.published_hidden;
						}
						else
						{
							accessStatus = GradebookItemAccessStatus.published_not_yet_open;
						}
					}
					else if (close != null)
					{
						if (now.after(close))
						{
							accessStatus = GradebookItemAccessStatus.published_closed;
						}
					}
					else if (due != null)
					{
						if (now.after(due))
						{
							accessStatus = GradebookItemAccessStatus.published_closed;
						}
					}
				}
			}
			
			if(!validDates)
			{
				accessStatus = GradebookItemAccessStatus.invalid;
			}
			
			Float averagePercent = null;
			Map<String, Float> scoresMap = null;
			
			if (includeScores)
			{
				scoresMap = new HashMap<String, Float>();
				
				if (activeParticipantIds != null && activeParticipantIds.size() > 0)
				{
					for (String participantId : activeParticipantIds)
					{
						scoresMap.put(participantId, null);
					}
				}
				
				List<Evaluation> evaluations = category.getEvaluations();
				
				if (evaluations != null && evaluations.size() > 0)
				{
					if (activeParticipantIds != null && activeParticipantIds.size() > 0)
					{
						Float totalScores = 0.0f;
						
						for (Evaluation evaluation : evaluations)
						{
							if (!activeParticipantIds.contains(evaluation.getSakaiUserId()))
							{
								continue;
							}
							
							if (evaluation.isReleased())
							{
								scoresMap.put(evaluation.getSakaiUserId(), evaluation.getScore());
								
								if (evaluation.getScore() != null)
								{
									totalScores += evaluation.getScore();
								}
							}
						}
						
						if ((scoresMap.size() > 0) && (points != null && points > 0))
						{
							averagePercent = ((totalScores/new Float(scoresMap.size())) * 100) / (points);
							
							averagePercent = Math.round(averagePercent * 100.0f) / 100.0f;
						} 
					}
				}
			}
			
			// make the item
			gradebookItem = this.gradebookService.newGradebookItem(itemId, title, points, averagePercent, due, open, close, GradebookItemType.topic, accessStatus, null);
			
			if (includeScores)
			{
				Map<String, Float> scores = gradebookItem.getScores();
				scores.clear();
				scores.putAll(scoresMap);
			}			
		}
		
		return gradebookItem;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<ParticipantItemDetails> getJforumItemPostsEvaluations(String context, String itemId, String fetchedByUserId, List<Participant> participants, boolean allEvaluations)
	{

		if ((context == null || context.trim().length() == 0) || (itemId == null || itemId.trim().length() == 0) || (fetchedByUserId == null || fetchedByUserId.trim().length() == 0) || (participants == null))
		{
			return new ArrayList<ParticipantItemDetails>();
		}
		
		if (!allEvaluations)
		{
			return getJforumItemPostsEvaluations(context, itemId, fetchedByUserId, participants);
		}

		boolean categoryItem = itemId.startsWith(CATEGORY + "-");
		boolean forumItem = itemId.startsWith(FORUM + "-");
		boolean topicItem = itemId.startsWith(TOPIC + "-");

		int id;
		try
		{
			id = Integer.parseInt(itemId.substring(itemId.indexOf("-") + 1));

		}
		catch (NumberFormatException e)
		{
			logger.warn("error in parsing of item id.", e);
			return null;
		}

		ArrayList<ParticipantItemDetails> rv = new ArrayList<ParticipantItemDetails>();

		if (categoryItem)
		{
			Category category = this.jforumService.getUsersPostCountByCategory(context, id);

			if (category != null)
			{
				if (category.isGradable() && (category.getForums() != null) && (category.getForums().isEmpty()))
				{
					// Map<String, Integer> posters = category.getUserPostCount();

					Category catEval = this.jforumService.getEvaluationsByCategory(context, id, true);

					List<Evaluation> catEvaluations = null;
					if (catEval != null)
					{
						catEvaluations = catEval.getEvaluations();
					}

					Map<String, Evaluation> userEvaluations = new HashMap<String, Evaluation>();

					if (catEvaluations != null)
					{
						for (Evaluation eval : catEvaluations)
						{
							userEvaluations.put(eval.getSakaiUserId(), eval);
						}
					}

					for (Participant p : participants)
					{
						Date evaluated = null;
						Date reviewed = null;
						Integer posts = Integer.valueOf(0);
						Float score = null;
						Date lastPostTime = null;
						Date firstPostTime = null;
						Boolean evaluationNotReviewed = null;
						Boolean released = null;
						String evaluationId = null;
						String reviewLink = null;
						String gradingLink = null;
						Boolean isLate = Boolean.FALSE; 

						if (userEvaluations.containsKey(p.getUserId()))
						{
							Evaluation userEvaluation = userEvaluations.get(p.getUserId());
									
							score = userEvaluation.getScore();
							posts = userEvaluation.getTotalPosts();
							lastPostTime = userEvaluation.getLastPostTime();
							firstPostTime = userEvaluation.getFirstPostTime();
							evaluated = userEvaluation.getEvaluatedDate();
							reviewed = userEvaluation.getReviewedDate();
							released = userEvaluation.isReleased();
							evaluationId = String.valueOf(userEvaluation.getId());
							isLate = userEvaluation.isLate();
							
							evaluationNotReviewed = Boolean.FALSE;
							
							if (userEvaluation.getReviewedDate() != null)
							{
								if (userEvaluation.getReviewedDate().before(userEvaluation.getEvaluatedDate()))
								{
									evaluationNotReviewed = Boolean.TRUE;
								}
							}
							else if (userEvaluation.getEvaluatedDate() != null)
							{
								evaluationNotReviewed = Boolean.TRUE;
							}
							
							if (jforumSecurityService.isUserFacilitator(context, fetchedByUserId))
							{
								gradingLink = getJforumReviewLink(context, category.getId(), "viewUserCategoryReplies", p.getUserId(), "assessmentDetails");
								if (userEvaluation.isReleased())
								{
									reviewLink = getJforumReviewLink(context, category.getId(), "viewUserCategoryGrade", p.getUserId() , "assessmentDetails");
								}
							}		
							else if (userEvaluation.isReleased())
							{
								reviewLink = getJforumReviewLink(context, category.getId(), "viewUserCategoryGrade", p.getUserId() , "assessmentDetails");
							}											
						}
						/*if (posters.containsKey(p.getUserId()))
						{
							posts = posters.get(p.getUserId());
						}*/
						
						ParticipantItemDetails pjid = this.gradebookService.newParticipantItemDetails(p.getUserId(), p.getDisplayId(), p.getGroupTitle(), p.getSortName(), p.getStatus(), posts, evaluated, reviewed, score, lastPostTime, firstPostTime, evaluationNotReviewed, released, reviewLink, gradingLink, Boolean.FALSE, evaluationId, isLate, null);
						
						// private message link
						pjid.setPrivateMessageLink(p.getPrivateMessageLink());
						
						rv.add(pjid);
					}
				}
			}
		}
		else if (forumItem)
		{
			Category category = this.jforumService.getUsersPostCountByForum(context, id);

			if ((category != null) && (category.getForums().size() == 1))
			{
				Forum forum = category.getForums().get(0);

				// Map<String, Integer> posters = forum.getUserPostCount();

				Category catEval = this.jforumService.getEvaluationsByForum(context, id, true);

				List<Evaluation> forumEvaluations = null;
				if (catEval != null)
				{
					if (catEval.getForums().size() == 1)
					{
						Forum forumEval = catEval.getForums().get(0);

						forumEvaluations = forumEval.getEvaluations();
					}
				}

				Map<String, Evaluation> userEvaluations = new HashMap<String, Evaluation>();

				if (forumEvaluations != null)
				{
					for (Evaluation eval : forumEvaluations)
					{
						userEvaluations.put(eval.getSakaiUserId(), eval);
					}
				}

				for (Participant p : participants)
				{
					Date evaluated = null;
					Date reviewed = null;
					Integer posts = Integer.valueOf(0);
					Float score = null;
					Date lastPostTime = null;
					Date firstPostTime = null;
					Boolean evaluationNotReviewed = null;
					Boolean released = null;
					String evaluationId = null;
					String reviewLink = null;
					String gradingLink = null;
					Boolean isLate = Boolean.FALSE;

					if (userEvaluations.containsKey(p.getUserId()))
					{
						Evaluation userEvaluation = userEvaluations.get(p.getUserId());

						//if (userEvaluation.isReleased())
						//{
							score = userEvaluation.getScore();
							posts = userEvaluation.getTotalPosts();
							lastPostTime = userEvaluation.getLastPostTime();
							firstPostTime = userEvaluation.getFirstPostTime();
							evaluated = userEvaluation.getEvaluatedDate();
							reviewed = userEvaluation.getReviewedDate();
							released = userEvaluation.isReleased();
							evaluationId = String.valueOf(userEvaluation.getId());
							isLate = userEvaluation.isLate();
									
							evaluationNotReviewed = Boolean.FALSE;
							
							if (userEvaluation.getReviewedDate() != null)
							{
								if (userEvaluation.getReviewedDate().before(userEvaluation.getEvaluatedDate()))
								{
									evaluationNotReviewed = Boolean.TRUE;
								}
							}
							else if (userEvaluation.getEvaluatedDate() != null)
							{
								evaluationNotReviewed = Boolean.TRUE;
							}
							
							if (jforumSecurityService.isUserFacilitator(context, fetchedByUserId))
							{
								gradingLink = getJforumReviewLink(context, forum.getId(), "viewUserForumReplies", p.getUserId(), "assessmentDetails");
								if (userEvaluation.isReleased())
								{
									reviewLink = getJforumReviewLink(context, forum.getId(), "viewUserForumGrade", p.getUserId(), "assessmentDetails");
								}
							}		
							else if (userEvaluation.isReleased())
							{
								reviewLink = getJforumReviewLink(context, forum.getId(), "viewUserForumGrade", p.getUserId(), "assessmentDetails");
							}						
						
						//}
					}

					/*if (posters.containsKey(p.getUserId()))
					{
						posts = posters.get(p.getUserId());
					}*/
					
					ParticipantItemDetails pjid = this.gradebookService.newParticipantItemDetails(p.getUserId(), p.getDisplayId(), p.getGroupTitle(), p.getSortName(), p.getStatus(), posts, evaluated, reviewed, score, lastPostTime, firstPostTime, evaluationNotReviewed, released, reviewLink, gradingLink, Boolean.FALSE, evaluationId, isLate, null);
					
					// private message link
					pjid.setPrivateMessageLink(p.getPrivateMessageLink());
					
					rv.add(pjid);
				}
			}
		}
		else if (topicItem)
		{
			Category category = this.jforumService.getUsersPostCountByTopic(context, id);

			if ((category.getForums().size() == 1) && (category.getForums().get(0).getTopics().size() == 1))
			{
				Topic topic = category.getForums().get(0).getTopics().get(0);

				//Map<String, Integer> posters = topic.getUserPostCount();

				Category catEval = this.jforumService.getEvaluationsByTopic(context, id, true);

				List<Evaluation> topicEvaluations = null;
				if (catEval != null)
				{
					if ((catEval.getForums().size() == 1) && (catEval.getForums().get(0).getTopics().size() == 1))
					{
						Topic topicEval = catEval.getForums().get(0).getTopics().get(0);

						topicEvaluations = topicEval.getEvaluations();
					}
				}

				Map<String, Evaluation> userEvaluations = new HashMap<String, Evaluation>();

				if (topicEvaluations != null)
				{
					for (Evaluation eval : topicEvaluations)
					{
						userEvaluations.put(eval.getSakaiUserId(), eval);
					}
				}

				for (Participant p : participants)
				{
					Date evaluated = null;
					Date reviewed = null;
					Integer posts = Integer.valueOf(0);
					Float score = null;
					Date lastPostTime = null;
					Date firstPostTime = null;
					Boolean evaluationNotReviewed = null;
					Boolean released = null;
					String evaluationId = null;
					String reviewLink = null;
					String gradingLink = null;
					Boolean isLate = Boolean.FALSE;
					
					if (userEvaluations.containsKey(p.getUserId()))
					{
						Evaluation userEvaluation = userEvaluations.get(p.getUserId());

						//if (userEvaluation.isReleased())
						//{
							score = userEvaluation.getScore();
							posts = userEvaluation.getTotalPosts();
							lastPostTime = userEvaluation.getLastPostTime();
							firstPostTime = userEvaluation.getFirstPostTime();
							evaluated = userEvaluation.getEvaluatedDate();
							reviewed = userEvaluation.getReviewedDate();
							released = userEvaluation.isReleased();
							evaluationId = String.valueOf(userEvaluation.getId());
							isLate = userEvaluation.isLate();
									
							evaluationNotReviewed = Boolean.FALSE;
							
							if (userEvaluation.getReviewedDate() != null)
							{
								if (userEvaluation.getReviewedDate().before(userEvaluation.getEvaluatedDate()))
								{
									evaluationNotReviewed = Boolean.TRUE;
								}
							}
							else if (userEvaluation.getEvaluatedDate() != null)
							{
								evaluationNotReviewed = Boolean.TRUE;
							}
							
							if (jforumSecurityService.isUserFacilitator(context, fetchedByUserId))
							{
								gradingLink = getJforumReviewLink(context, topic.getId(), "viewUserTopicReplies", p.getUserId(), "assessmentDetails");
								if (userEvaluation.isReleased())
								{
									reviewLink = getJforumReviewLink(context, topic.getId(), "viewUserTopicGrade", p.getUserId(), "assessmentDetails");
								}
							}		
							else if (userEvaluation.isReleased())
							{
								reviewLink = getJforumReviewLink(context, topic.getId(), "viewUserTopicGrade", p.getUserId(), "assessmentDetails");
							}				
							
						//}
					}

					/*if (posters.containsKey(p.getUserId()))
					{
						posts = posters.get(p.getUserId());
					}*/
					
					ParticipantItemDetails pjid = this.gradebookService.newParticipantItemDetails(p.getUserId(), p.getDisplayId(), p.getGroupTitle(), p.getSortName(), p.getStatus(), posts, evaluated, reviewed, score, lastPostTime, firstPostTime, evaluationNotReviewed, released, reviewLink, gradingLink, Boolean.FALSE, evaluationId, isLate, null);
					
					// private message link
					pjid.setPrivateMessageLink(p.getPrivateMessageLink());
					
					rv.add(pjid);
				}
			}
		}

		return rv;		
	}
	
	/**
	 * {@inheritDoc} 
	 */
	@Override
	public Map<String, List<ParticipantGradebookItem>> getUsersGradableItems(String context, String fetchedByUserId, List<String> participantIds, GradebookItemType itemType, boolean allScores)
	{
		if (context == null || context.trim().length() == 0)
		{
			throw new IllegalArgumentException("context is missing.");
		}
		
		if (!(itemType == null || itemType == GradebookItemType.category || itemType == GradebookItemType.forum || itemType == GradebookItemType.topic))
		{
			return null;
		}
		
		//TODO check user access
		
		// List<GradebookItem> gradebookItems = new ArrayList<GradebookItem>();
		
		String toolId = null;
		try
		{
			Site site = this.siteService.getSite(context);
			
			ToolConfiguration config = site.getToolForCommonId("sakai.jforum.tool");
			if (config != null) toolId = config.getId();
		}
		catch (IdUnusedException e)
		{
			logger.warn("getGradableItems: missing site: " + context);
		}

		// no tool id? No JForum in site!
		if (toolId == null) return null;
		
		Map<String, List<ParticipantGradebookItem>> userGradableItems = new HashMap<String, List<ParticipantGradebookItem>>();
		
		for (String particpantId : participantIds)
		{
			userGradableItems.put(particpantId, new ArrayList<ParticipantGradebookItem>());
		}
		
		List<Category> categories = this.jforumService.getGradableItemsByContext(context);
		
		String id = null;
		String title = null;
		Float points = null;
		Date open = null;
		Boolean isHideUntilOpen = Boolean.FALSE;
		Date due = null;
		Date close = null;
		GradebookItemAccessStatus accessStatus = GradebookItemAccessStatus.published;
		Float score = null;
		Integer count = 0;
		
		Date startedDate;
		Date finishedDate;
		Date evaluatedDate;
		Date reviewedDate; 
		Boolean evaluationNotReviewed;
	
		for (Category category : categories)
		{
			if (category.isGradable())
			{
				Grade grade = category.getGrade();
				
				if (grade == null || !grade.isAddToGradeBook())
				{
					continue;
				}
				
				id = null;
				title = null;
				points = null;
				open = null;
				isHideUntilOpen = Boolean.FALSE;
				due = null;
				close = null;
				accessStatus = GradebookItemAccessStatus.published;
				
				id = JForumItemProvider.CATEGORY + "-" + String.valueOf(category.getId());
				title = category.getTitle();
				points = grade.getPoints();
				
				score = null;
				count = 0;				
				startedDate = null;
				finishedDate = null;
				evaluatedDate = null;
				reviewedDate = null;
				evaluationNotReviewed = null;
							
				if (category.getAccessDates() != null)
				{
					open = category.getAccessDates().getOpenDate();
					due = category.getAccessDates().getDueDate();
					close = category.getAccessDates().getAllowUntilDate();
					
					if (open != null)
					{
						isHideUntilOpen = category.getAccessDates().isHideUntilOpen();
					}
				}
				
				// check for invalid (Note: categories cannot have "deny access"
				if (((open != null) || (due != null) || (close != null)) && (!category.getAccessDates().isDatesValid()))
				{
					accessStatus = GradebookItemAccessStatus.invalid;
					// ignore items with invalid dates
					continue;
				}
				
				Date now = new Date();
				
				// ignore not open items
				/*
				if (isHideUntilOpen && open != null && now.before(open))
				{
					continue;
				}
				*/
				
				// access status
				if (category.getForums().size() == 0)
				{
					accessStatus = GradebookItemAccessStatus.published_hidden;
				}
				// access status - if category have dates
				else if ((open != null) || (due != null) || (close != null))
				{
					if (accessStatus == GradebookItemAccessStatus.published)
					{
						if ((open != null) && (now.before(open)))
						{
							if (isHideUntilOpen.booleanValue())
							{
								accessStatus = GradebookItemAccessStatus.published_hidden;
							}
							else
							{
								accessStatus = GradebookItemAccessStatus.published_not_yet_open;
							}
						}
						else if (close != null)
						{
							if (now.after(close))
							{
								accessStatus = GradebookItemAccessStatus.published_closed;
							}
						}
						else if (due != null)
						{
							if (now.after(due))
							{
								accessStatus = GradebookItemAccessStatus.published_closed;
							}
						}
					}
				}
				else
				{
					if (accessStatus == GradebookItemAccessStatus.published)
					{
						accessStatus = getCategoryAccessStatus(category);
					}
				}
				
				Category evalCategory = this.jforumService.getEvaluationsByCategory(context, category.getId(), true);
				
				List<Evaluation> evaluations = evalCategory.getEvaluations();
				
				if (evaluations != null && evaluations.size() > 0)
				{
					if (participantIds != null && participantIds.size() > 0)
					{
						for (Evaluation evaluation : evaluations)
						{
							score = null;
							count = 0;
							ParticipantItemDetails pItem = null;
							
							if (!participantIds.contains(evaluation.getSakaiUserId()))
							{
								continue;
							}
							
							count = evaluation.getTotalPosts();
							
							if (evaluation.isReleased())
							{
								startedDate = evaluation.getFirstPostTime();
								finishedDate = evaluation.getLastPostTime();
								if (startedDate !=null && finishedDate != null && (startedDate.compareTo(finishedDate) == 0))
								{
									finishedDate = null;
								}
								
								evaluatedDate = evaluation.getEvaluatedDate();
								reviewedDate = evaluation.getReviewedDate();
								// count = evaluation.getTotalPosts();
							
								score = evaluation.getScore();
								
								evaluationNotReviewed = Boolean.FALSE;
			
								if (evaluation.getReviewedDate() != null)
								{
									if (evaluation.getReviewedDate().before(evaluation.getEvaluatedDate()))
									{
										evaluationNotReviewed = Boolean.TRUE;
									}
								}
								else if (evaluation.getEvaluatedDate() != null)
								{
									evaluationNotReviewed = Boolean.TRUE;
								}
							}
							
							// pItem = this.gradebookService.newParticipantItemDetails(startedDate, finishedDate, score, evaluatedDate, reviewedDate, evaluationNotReviewed, count);
							pItem = this.gradebookService.newParticipantItemDetails(null, null, null, null, null, count, evaluatedDate, reviewedDate, score, startedDate, finishedDate, evaluationNotReviewed, null, null, null, null, null, null, null);
							
							// make the item
							GradebookItem gItem = this.gradebookService.newGradebookItem(id, title, points, null, due, open, close, GradebookItemType.category, accessStatus, null);
							
							ParticipantGradebookItem uItem = this.gradebookService.newParticipantGradebookItem(id, gItem, pItem);
							
							userGradableItems.get(evaluation.getSakaiUserId()).add(uItem);	
						}
					}
				}
				
			}
			else if (category.getForums().size() > 0)
			{
				for (Forum forum : category.getForums())
				{
					if (forum.getGradeType() == Grade.GradeType.FORUM.getType())
					{
						Grade grade = forum.getGrade();
						
						if (grade == null || !grade.isAddToGradeBook())
						{
							continue;
						}
						
						id = null;
						title = null;
						points = null;
						open = null;
						due = null;
						close = null;
						accessStatus = GradebookItemAccessStatus.published;
						boolean validDates = true;
						accessStatus = GradebookItemAccessStatus.published;
						
						id = JForumItemProvider.FORUM + "-" + String.valueOf(forum.getId());
						title = forum.getName();
						points = grade.getPoints();
						
						score = null;
						count = 0;				
						startedDate = null;
						finishedDate = null;
						evaluatedDate = null;
						reviewedDate = null;
						evaluationNotReviewed = null;
					
						// consider special access
						if (forum.getAccessDates() != null)
						{
							open = forum.getAccessDates().getOpenDate();
							due = forum.getAccessDates().getDueDate();
							close = forum.getAccessDates().getAllowUntilDate();
							validDates = forum.getAccessDates().isDatesValid();
							
							if (open != null)
							{
								isHideUntilOpen = forum.getAccessDates().isHideUntilOpen();
							}
						}
						
						if ((open == null && due == null && close == null) && (category.getAccessDates() != null) )
						{
							open = category.getAccessDates().getOpenDate();
							due = category.getAccessDates().getDueDate();
							close = category.getAccessDates().getAllowUntilDate();
							validDates = forum.getAccessDates().isDatesValid();
							
							if (open != null)
							{
								isHideUntilOpen = category.getAccessDates().isHideUntilOpen();
							}
						}
						
						// access status
						// check for invalid
						if (((open != null) || (due != null) || (close != null)) && (!validDates))
						{
							accessStatus = GradebookItemAccessStatus.invalid;
							// ignore items with invalid dates
							continue;
						}
						// otherwise check for deny access
						else
						{
							if (forum.getAccessType() == Forum.ACCESS_DENY)
							{
								// ignore access deny items
								accessStatus = GradebookItemAccessStatus.unpublished;
								continue;
							}
						}
						
						Date now = new Date();
						
						// ignore not open items
						/*
						if (isHideUntilOpen && open != null && now.before(open))
						{
							continue;
						}
						*/
						
						if ((open != null) || (due != null) || (close != null))
						{
							if (accessStatus == GradebookItemAccessStatus.published)
							{
								if ((open != null) && (now.before(open)))
								{
									if (isHideUntilOpen.booleanValue())
									{
										accessStatus = GradebookItemAccessStatus.published_hidden;
									}
									else
									{
										accessStatus = GradebookItemAccessStatus.published_not_yet_open;
									}
								}
								else if (close != null)
								{
									if (now.after(close))
									{
										accessStatus = GradebookItemAccessStatus.published_closed;
									}
								}
								else if (due != null)
								{
									if (now.after(due))
									{
										accessStatus = GradebookItemAccessStatus.published_closed;
									}
								}
							}
						}
						
						// get users special access details
						Map<Integer, UserItemSpecialAccess> forumUserItemSpecialAccessList = new HashMap<Integer, UserItemSpecialAccess>();
						
						if (forum.getAccessDates() != null)
						{
							Date forumOpen = forum.getAccessDates().getOpenDate();
							Date forumDue = forum.getAccessDates().getDueDate();
							Date forumClose = forum.getAccessDates().getAllowUntilDate();
							boolean forumValidDates = forum.getAccessDates().isDatesValid();
							
							if (((forumOpen != null) || (forumDue != null) || (forumClose != null)) && (forumValidDates))
							{
								List<SpecialAccess> forumSpecialAccessList = this.jforumSpecialAccessService.getByForum(forum.getId());
								
								Date openDate = null; 
								Date dueDate = null; 
								Date acceptUntilDate = null;
								Boolean hideUntilOpen = null;
								Boolean overrideOpenDate = null;
								Boolean overrideDueDate = null;
								Boolean overrideAcceptUntilDate = null;
								Boolean overrideHideUntilOpen = null;
								Boolean datesValid = Boolean.TRUE;
								
								if (forumSpecialAccessList != null && !forumSpecialAccessList.isEmpty())
								{
									UserItemSpecialAccess userItemSpecialAccess = null;
															
									for (SpecialAccess forumSpecialAccess : forumSpecialAccessList)
									{
										if (forumSpecialAccess.getAccessDates() != null)
										{
											// forum special access
											if (forumSpecialAccess.getForumId() > 0 && forumSpecialAccess.getTopicId() == 0)
											{
												openDate = forumSpecialAccess.getAccessDates().getOpenDate();
												dueDate = forumSpecialAccess.getAccessDates().getDueDate();
												acceptUntilDate = forumSpecialAccess.getAccessDates().getAllowUntilDate();
												hideUntilOpen =  forumSpecialAccess.getAccessDates().isHideUntilOpen();
												overrideOpenDate = forumSpecialAccess.isOverrideStartDate();
												overrideDueDate = forumSpecialAccess.isOverrideEndDate();
												overrideAcceptUntilDate = forumSpecialAccess.isOverrideAllowUntilDate();
												overrideHideUntilOpen = forumSpecialAccess.isOverrideHideUntilOpen();
												datesValid = forumSpecialAccess.isForumSpecialAccessDatesValid(forum);
												
												for (Integer userId : forumSpecialAccess.getUserIds())
												{
													userItemSpecialAccess = this.gradebookService.newUserItemSpecialAccessImpl(openDate, dueDate, acceptUntilDate, hideUntilOpen, overrideOpenDate, overrideDueDate, overrideAcceptUntilDate, overrideHideUntilOpen, datesValid);
													
													forumUserItemSpecialAccessList.put(userId, userItemSpecialAccess);
												}
											}
										}
									}
								}
							}
						}
						
						Category forumCategory = this.jforumService.getEvaluationsByForum(context, forum.getId(), true);
						
						if (forumCategory.getForums() != null)
						{
							List<Evaluation> evaluations = forumCategory.getForums().get(0).getEvaluations();
							
							if (evaluations != null && evaluations.size() > 0)
							{
								if (participantIds != null && participantIds.size() > 0)
								{
									for (Evaluation evaluation : evaluations)
									{
										score = null;
										count = 0;
										ParticipantItemDetails pItem = null;
										
										if (!participantIds.contains(evaluation.getSakaiUserId()))
										{
											continue;
										}
										
										count = evaluation.getTotalPosts();
										
										if (evaluation.isReleased())
										{
											startedDate = evaluation.getFirstPostTime();
											finishedDate = evaluation.getLastPostTime();
											if (startedDate !=null && finishedDate != null && (startedDate.compareTo(finishedDate) == 0))
											{
												finishedDate = null;
											}
											
											evaluatedDate = evaluation.getEvaluatedDate();
											reviewedDate = evaluation.getReviewedDate();
											// count = evaluation.getTotalPosts();
																				
											score = evaluation.getScore();
											
											evaluationNotReviewed = Boolean.FALSE;
						
											if (evaluation.getReviewedDate() != null)
											{
												if (evaluation.getReviewedDate().before(evaluation.getEvaluatedDate()))
												{
													evaluationNotReviewed = Boolean.TRUE;
												}
											}
											else if (evaluation.getEvaluatedDate() != null)
											{
												evaluationNotReviewed = Boolean.TRUE;
											}
										}
										
										// pItem = this.gradebookService.newParticipantItemDetails(startedDate, finishedDate, score, evaluatedDate, reviewedDate, evaluationNotReviewed, count);
										pItem = this.gradebookService.newParticipantItemDetails(null, null, null, null, null, count, evaluatedDate, reviewedDate, score, startedDate, finishedDate, evaluationNotReviewed, null, null, null, null, null, null, null);
										
										if (!forumUserItemSpecialAccessList.isEmpty())
										{
											UserItemSpecialAccess userItemSpecialAccess = forumUserItemSpecialAccessList.get(evaluation.getUserId());
											
											pItem.setUserItemSpecialAccess(userItemSpecialAccess);
										}
										
										// make the item
										GradebookItem gItem = this.gradebookService.newGradebookItem(id, title, points, null, due, open, close, GradebookItemType.forum, accessStatus, null);
										
										ParticipantGradebookItem uItem = this.gradebookService.newParticipantGradebookItem(id, gItem, pItem);
										
										userGradableItems.get(evaluation.getSakaiUserId()).add(uItem);
									}
								}
							}
						}
					}
					else if (forum.getTopics().size() > 0)
					{
						for (Topic topic : forum.getTopics())
						{
							if (topic.isGradeTopic())
							{
								Grade grade = topic.getGrade();
								
								if (grade == null || !grade.isAddToGradeBook())
								{
									continue;
								}
								
								id = null;
								title = null;
								points = null;
								open = null;
								due = null;
								close = null;
								accessStatus = GradebookItemAccessStatus.published;
								
								id = JForumItemProvider.TOPIC + "-" + String.valueOf(topic.getId());
								title = topic.getTitle();
								points = grade.getPoints();
								isHideUntilOpen = Boolean.FALSE;
								boolean validDates = true;
								
								score = null;
								count = 0;				
								startedDate = null;
								finishedDate = null;
								evaluatedDate = null;
								reviewedDate = null;
								evaluationNotReviewed = null;
								
								// TODO consider special access
								
								if (topic.getAccessDates() != null)
								{
									open = topic.getAccessDates().getOpenDate();
									due = topic.getAccessDates().getDueDate();
									close = topic.getAccessDates().getAllowUntilDate();
									validDates = topic.getAccessDates().isDatesValid();
									
									if (open != null)
									{
										isHideUntilOpen = topic.getAccessDates().isHideUntilOpen();
									}
								}
								
								if ((open == null && due == null && close == null) && (forum.getAccessDates() != null) )
								{
									open = forum.getAccessDates().getOpenDate();
									due = forum.getAccessDates().getDueDate();
									close = forum.getAccessDates().getAllowUntilDate();
									validDates = category.getAccessDates().isDatesValid();
									
									if (open != null)
									{
										isHideUntilOpen = forum.getAccessDates().isHideUntilOpen();
									}
								}
								
								if ((open == null && due == null && close == null) && (category.getAccessDates() != null) )
								{
									open = category.getAccessDates().getOpenDate();
									due = category.getAccessDates().getDueDate();
									close = category.getAccessDates().getAllowUntilDate();
									validDates = category.getAccessDates().isDatesValid();
									
									if (open != null)
									{
										isHideUntilOpen = category.getAccessDates().isHideUntilOpen();
									}
								}
								
								// access status
								if (((open != null) || (due != null) || (close != null)) && (!validDates))
								{
									accessStatus = GradebookItemAccessStatus.invalid;
									// ignore items with invalid dates
									continue;
								}
								// otherwise check for deny access
								else
								{
									if (forum.getAccessType() == Forum.ACCESS_DENY)
									{
										accessStatus = GradebookItemAccessStatus.unpublished;
										// ignore access deny items
										continue;
									}
								}
								
								Date now = new Date();
								
								// ignore not open items
								/*
								if (isHideUntilOpen && open != null && now.before(open))
								{
									continue;
								}
								*/
								
								// access status
								if ((open != null) || (due != null) || (close != null))
								{
									if (accessStatus == GradebookItemAccessStatus.published)
									{
										if ((open != null) && (now.before(open)))
										{
											if (isHideUntilOpen.booleanValue())
											{
												accessStatus = GradebookItemAccessStatus.published_hidden;
											}
											else
											{
												accessStatus = GradebookItemAccessStatus.published_not_yet_open;
											}
										}
										else if (close != null)
										{
											if (now.after(close))
											{
												accessStatus = GradebookItemAccessStatus.published_closed;
											}
										}
										else if (due != null)
										{
											if (now.after(due))
											{
												accessStatus = GradebookItemAccessStatus.published_closed;
											}
										}
									}
								}
								
								// get users special access details
								Map<Integer, UserItemSpecialAccess> topicUserItemSpecialAccessList = new HashMap<Integer, UserItemSpecialAccess>();
								
								if (topic.getAccessDates() != null)
								{
									Date tipicOpen = topic.getAccessDates().getOpenDate();
									Date topicDue = topic.getAccessDates().getDueDate();
									Date topicClose = topic.getAccessDates().getAllowUntilDate();
									boolean topicValidDates = topic.getAccessDates().isDatesValid();
									
									if (((tipicOpen != null) || (topicDue != null) || (topicClose != null)) && (topicValidDates))
									{
										List<SpecialAccess> topicSpecialAccessList = this.jforumSpecialAccessService.getByTopic(topic.getForumId(), topic.getId());
										
										Date openDate = null; 
										Date dueDate = null; 
										Date acceptUntilDate = null;
										Boolean hideUntilOpen = null;
										Boolean overrideOpenDate = null;
										Boolean overrideDueDate = null;
										Boolean overrideAcceptUntilDate = null;
										Boolean overrideHideUntilOpen = null;
										Boolean datesValid = Boolean.TRUE;
										
										if (topicSpecialAccessList != null && !topicSpecialAccessList.isEmpty())
										{
											UserItemSpecialAccess userItemSpecialAccess = null;
																	
											for (SpecialAccess topicSpecialAccess : topicSpecialAccessList)
											{
												if (topicSpecialAccess.getAccessDates() != null)
												{
													// forum special access
													if (topicSpecialAccess.getForumId() > 0 && topicSpecialAccess.getTopicId() > 0)
													{
														openDate = topicSpecialAccess.getAccessDates().getOpenDate();
														dueDate = topicSpecialAccess.getAccessDates().getDueDate();
														acceptUntilDate = topicSpecialAccess.getAccessDates().getAllowUntilDate();
														hideUntilOpen =  topicSpecialAccess.getAccessDates().isHideUntilOpen();
														overrideOpenDate = topicSpecialAccess.isOverrideStartDate();
														overrideDueDate = topicSpecialAccess.isOverrideEndDate();
														overrideAcceptUntilDate = topicSpecialAccess.isOverrideAllowUntilDate();
														overrideHideUntilOpen = topicSpecialAccess.isOverrideHideUntilOpen();
														datesValid = topicSpecialAccess.isTopicSpecialAccessDatesValid(topic);
														
														for (Integer userId : topicSpecialAccess.getUserIds())
														{
															userItemSpecialAccess = this.gradebookService.newUserItemSpecialAccessImpl(openDate, dueDate, acceptUntilDate, hideUntilOpen, overrideOpenDate, overrideDueDate, overrideAcceptUntilDate, overrideHideUntilOpen, datesValid);
															
															topicUserItemSpecialAccessList.put(userId, userItemSpecialAccess);
														}
													}
												}
											}
										}
									}
								}
								
								Category topicCategory = this.jforumService.getEvaluationsByTopic(context, topic.getId(), true);
								
								if (topicCategory.getForums() != null && topicCategory.getForums().get(0).getTopics() != null)
								{
									List<Evaluation> evaluations = topicCategory.getForums().get(0).getTopics().get(0).getEvaluations();
									
									if (evaluations != null && evaluations.size() > 0)
									{
										if (participantIds != null && participantIds.size() > 0)
										{
											for (Evaluation evaluation : evaluations)
											{
												score = null;
												count = 0;
												
												ParticipantItemDetails pItem = null;
												
												if (!participantIds.contains(evaluation.getSakaiUserId()))
												{
													continue;
												}
												
												count = evaluation.getTotalPosts();
												
												if (evaluation.isReleased())
												{
													startedDate = evaluation.getFirstPostTime();
													finishedDate = evaluation.getLastPostTime();
													if (startedDate !=null && finishedDate != null && (startedDate.compareTo(finishedDate) == 0))
													{
														finishedDate = null;
													}
													
													evaluatedDate = evaluation.getEvaluatedDate();
													reviewedDate = evaluation.getReviewedDate();
													// count = evaluation.getTotalPosts();												
												
													score = evaluation.getScore();
													
													evaluationNotReviewed = Boolean.FALSE;
								
													if (evaluation.getReviewedDate() != null)
													{
														if (evaluation.getReviewedDate().before(evaluation.getEvaluatedDate()))
														{
															evaluationNotReviewed = Boolean.TRUE;
														}
													}
													else if (evaluation.getEvaluatedDate() != null)
													{
														evaluationNotReviewed = Boolean.TRUE;
													}
												}
												
												// pItem = this.gradebookService.newParticipantItemDetails(startedDate, finishedDate, score, evaluatedDate, reviewedDate, evaluationNotReviewed, count);
												pItem = this.gradebookService.newParticipantItemDetails(null, null, null, null, null, count, evaluatedDate, reviewedDate, score, startedDate, finishedDate, evaluationNotReviewed, null, null, null, null, null, null, null);
												
												if (!topicUserItemSpecialAccessList.isEmpty())
												{
													UserItemSpecialAccess userItemSpecialAccess = topicUserItemSpecialAccessList.get(evaluation.getUserId());
													
													pItem.setUserItemSpecialAccess(userItemSpecialAccess);
												}
												
												// make the item
												GradebookItem gItem = this.gradebookService.newGradebookItem(id, title, points, null, due, open, close, GradebookItemType.topic, accessStatus, null);
												
												ParticipantGradebookItem uItem = this.gradebookService.newParticipantGradebookItem(id, gItem, pItem);
												
												userGradableItems.get(evaluation.getSakaiUserId()).add(uItem);
											}
										}
									}
								}								
							}
						}
					}
				}
			}
		}
		
		return userGradableItems;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<ParticipantGradebookItem> getUserToolGradebookItems(String context, String userId, GradebookItemType itemType, boolean allScores)
	{
		if ((context == null || context.trim().length() == 0)|| (userId == null || userId.trim().length() == 0))
		{
			throw new IllegalArgumentException("context or user information is missing.");
		}
		
		if (!(itemType == null || itemType == GradebookItemType.category || itemType == GradebookItemType.forum || itemType == GradebookItemType.topic))
		{
			return null;
		}
		
		//TODO check user access		
		List<ParticipantGradebookItem> gradebookItems = new ArrayList<ParticipantGradebookItem>();
		
		String toolId = null;
		try
		{
			Site site = this.siteService.getSite(context);
			
			ToolConfiguration config = site.getToolForCommonId("sakai.jforum.tool");
			if (config != null) toolId = config.getId();
		}
		catch (IdUnusedException e)
		{
			logger.warn("getUserToolGradebookItems: missing site: " + context);
		}

		// no tool id? No JForum in site!
		if (toolId == null) return gradebookItems;
		
		List<Category> categories = this.jforumService.getUserAccessibleGradableItemsByContext(context, userId);
		
		String id = null;
		String title = null;
		Float points = null;
		Date open = null;
		Boolean isHideUntilOpen = Boolean.FALSE;
		Date due = null;
		Date close = null;
		Float score = null;
		Date startedDate;
		Date finishedDate;
		Date evaluatedDate;
		Date reviewedDate; 
		Boolean evaluationNotReviewed; 
		Integer count;
		String reviewLink = null;
		String gradingLink = null;
		Boolean released = null;
		String evaluationId = null;
		Boolean isLate = Boolean.FALSE;
		
		for (Category category : categories)
		{
			if (category.isGradable())
			{
				Grade grade = category.getGrade();
				
				if (grade == null || !grade.isAddToGradeBook())
				{
					continue;
				}
				
				id = null;
				title = null;
				points = null;
				open = null;
				due = null;
				close = null;
				score = null;
				GradebookItemAccessStatus accessStatus = GradebookItemAccessStatus.published;
				startedDate = null;
				finishedDate = null;
				evaluatedDate = null;
				reviewedDate = null;
				evaluationNotReviewed = null;
				count = 0;
				isHideUntilOpen = Boolean.FALSE;
				reviewLink = null;
				gradingLink = null;
				released = null;
				evaluationId = null;
				isLate = Boolean.FALSE;
				
				id = JForumItemProvider.CATEGORY + "-" + String.valueOf(category.getId());
				title = category.getTitle();
				points = grade.getPoints();
				
				if (category.getAccessDates() != null)
				{
					open = category.getAccessDates().getOpenDate();
					due = category.getAccessDates().getDueDate();
					close = category.getAccessDates().getAllowUntilDate();
					
					if (open != null)
					{
						isHideUntilOpen = category.getAccessDates().isHideUntilOpen();
					}
				}
				
				// check for invalid (Note: categories cannot have "deny access"
				if (((open != null) || (due != null) || (close != null)) && (!category.getAccessDates().isDatesValid()))
				{
					accessStatus = GradebookItemAccessStatus.invalid;
					// ignore items invalid dates
					continue;
				}
				
				Date now = new Date();
				
				/*
				// ignore not open items
				if (isHideUntilOpen && open != null && now.before(open))
				{
					continue;
				}
				*/
				
				Evaluation evaluation = category.getUserEvaluation();
				
				//if (evaluation != null && evaluation.isReleased())
				if (evaluation != null)
				{
					count = evaluation.getTotalPosts();
					
					// is late
					isLate = evaluation.isLate();
					
					if (allScores || evaluation.isReleased())
					{
						startedDate = evaluation.getFirstPostTime();
						finishedDate = evaluation.getLastPostTime();
						if (startedDate !=null && finishedDate != null && (startedDate.compareTo(finishedDate) == 0))
						{
							finishedDate = null;
						}
						evaluatedDate = evaluation.getEvaluatedDate();
						reviewedDate = evaluation.getReviewedDate();
												
						if (jforumSecurityService.isJForumFacilitator(context))
						{
							gradingLink = getJforumReviewLink(context, category.getId(), "viewUserCategoryReplies", userId, "indvidualStudentGrades");
							if (evaluation.isReleased())
							{
								reviewLink = getJforumReviewLink(context, category.getId(), "viewUserCategoryGrade", userId, "indvidualStudentGrades");
							}
						}
						else if (evaluation.isReleased())
						{
							reviewLink = getJforumReviewLink(context, category.getId(), "viewUserCategoryGrade", userId, "indvidualStudentGrades");
						}
						
						score = evaluation.getScore();
						
						evaluationNotReviewed = Boolean.FALSE;
	
						if (evaluation.getReviewedDate() != null)
						{
							if (evaluation.getReviewedDate().before(evaluation.getEvaluatedDate()))
							{
								evaluationNotReviewed = Boolean.TRUE;
							}
						}
						else if (evaluation.getEvaluatedDate() != null)
						{
							evaluationNotReviewed = Boolean.TRUE;
						}
						
						released = evaluation.isReleased();
						evaluationId = String.valueOf(evaluation.getId());
					}
				}
				
				// access status
				accessStatus = GradebookItemAccessStatus.published;
				/*	accessStatus needs to be fleshed out to possibly set:
				 	published_hidden - if now is before the open date for the item (or the earliest open date for any dates in this item's children if they have the dates)
					published_closed - if now is after the close date and we lock on close (consider the dates and lock on close of children if needed)
					published_closed_access - if now is after the close date, and we still let the student have access to the forum (consider the dates and lock on close of children if needed)
					must consider forums in categories when dates are on forums, as well as the dates on the item itself
					Note also that "accessAfterClose" is not directly used anymore
				 */
				// access status
				if ((open != null) || (due != null) || (close != null))
				{
					if (accessStatus == GradebookItemAccessStatus.published)
					{
						if ((open != null) && (now.before(open)))
						{
							if (isHideUntilOpen.booleanValue())
							{
								accessStatus = GradebookItemAccessStatus.published_hidden;
								// score = null;
							}
							else
							{
								accessStatus = GradebookItemAccessStatus.published_not_yet_open;
								// score = null;
							}
						}
						else if (close != null)
						{
							if (now.after(close))
							{
								accessStatus = GradebookItemAccessStatus.published_closed;
							}
						}
						else if (due != null)
						{
							if (now.after(due))
							{
								accessStatus = GradebookItemAccessStatus.published_closed;
							}
						}
					}
				}
				else
				{
					if (accessStatus == GradebookItemAccessStatus.published)
					{
						accessStatus = getUserCategoryAccessStatus(category);
					}
				}
								
				// make the item
				GradebookItem gItem = this.gradebookService.newGradebookItem(id, title, points, null, due, open, close, GradebookItemType.category, accessStatus, null);
				ParticipantItemDetails pItem = this.gradebookService.newParticipantItemDetails(userId, null, null, null, null, count, evaluatedDate, reviewedDate, score, startedDate, finishedDate, evaluationNotReviewed, released, reviewLink, gradingLink, null, evaluationId, isLate, null);
				
				ParticipantGradebookItem uItem = this.gradebookService.newParticipantGradebookItem(id, gItem, pItem);
				gradebookItems.add(uItem);
			}
			else if ((category.getForums() != null) && (!category.getForums().isEmpty()))
			{
				for (Forum forum : category.getForums())
				{
					// gradable forums
					if (forum.getGradeType() == Grade.GRADE_BY_FORUM)
					{
						Grade grade = forum.getGrade();
						
						if (grade == null || !grade.isAddToGradeBook())
						{
							continue;
						}
						
						id = null;
						title = null;
						points = null;
						open = null;
						due = null;
						close = null;
						score = null;
						GradebookItemAccessStatus accessStatus = GradebookItemAccessStatus.published;
						startedDate = null;
						finishedDate = null;
						evaluatedDate = null;
						reviewedDate = null;
						evaluationNotReviewed = null;
						count = 0;
						isHideUntilOpen = Boolean.FALSE;
						boolean validDates = true;
						reviewLink = null;
						gradingLink = null;
						released = null;
						evaluationId = null;
						isLate = Boolean.FALSE;
						
						id = JForumItemProvider.FORUM + "-" + String.valueOf(forum.getId());
						title = forum.getName();
						points = grade.getPoints();
						
						// TODO consider special access
						if (forum.getAccessDates() != null)
						{
							open = forum.getAccessDates().getOpenDate();
							due = forum.getAccessDates().getDueDate();
							close = forum.getAccessDates().getAllowUntilDate();
							validDates = forum.getAccessDates().isDatesValid();
							
							if (open != null)
							{
								isHideUntilOpen = forum.getAccessDates().isHideUntilOpen();
							}
						}
						
						if ((open == null && due == null && close == null) && (category.getAccessDates() != null) )
						{
							open = category.getAccessDates().getOpenDate();
							due = category.getAccessDates().getDueDate();
							close = category.getAccessDates().getAllowUntilDate();
							validDates = category.getAccessDates().isDatesValid();
							
							if (open != null)
							{
								isHideUntilOpen = category.getAccessDates().isHideUntilOpen();
							}
						}
						
						// check for invalid
						if (((open != null) || (due != null) || (close != null)) && (!validDates))
						{
							accessStatus = GradebookItemAccessStatus.invalid;
							// ignore items with invalid dates
							continue;
						}
						// otherwise check for deny access
						else
						{
							if (forum.getAccessType() == Forum.ACCESS_DENY)
							{
								// ignore access deny items
								accessStatus = GradebookItemAccessStatus.unpublished;
								continue;
							}
						}
						
						// user special access
						List<SpecialAccess> userForumSpecialAccessList = null;
						SpecialAccess userForumSpecialAccess = null;
						UserItemSpecialAccess userItemSpecialAccess = null;
						if (open != null || due != null || close != null)
						{
							userForumSpecialAccessList = forum.getSpecialAccess();
							
							if (userForumSpecialAccessList != null && userForumSpecialAccessList.size() == 1)
							{
								userForumSpecialAccess = userForumSpecialAccessList.get(0);
								Date openDate = userForumSpecialAccess.getAccessDates().getOpenDate();
								Date dueDate = userForumSpecialAccess.getAccessDates().getDueDate();
								Date acceptUntilDate = userForumSpecialAccess.getAccessDates().getAllowUntilDate();
								Boolean hideUntilOpen =  userForumSpecialAccess.getAccessDates().isHideUntilOpen();
								Boolean overrideOpenDate = userForumSpecialAccess.isOverrideStartDate();
								Boolean overrideDueDate = userForumSpecialAccess.isOverrideEndDate();
								Boolean overrideAcceptUntilDate = userForumSpecialAccess.isOverrideAllowUntilDate();
								Boolean overrideHideUntilOpen = userForumSpecialAccess.isOverrideHideUntilOpen();
								Boolean datesValid = userForumSpecialAccess.isForumSpecialAccessDatesValid(forum);
								userItemSpecialAccess = this.gradebookService.newUserItemSpecialAccessImpl(openDate, dueDate, acceptUntilDate, hideUntilOpen, overrideOpenDate, overrideDueDate, overrideAcceptUntilDate, overrideHideUntilOpen, datesValid);
							}
						}
						
						Date now = new Date();
						
						/*
						// ignore not open items
						if (isHideUntilOpen && open != null && now.before(open))
						{
							continue;
						}
						*/
						
						Evaluation evaluation = forum.getUserEvaluation();

						//if (evaluation != null && evaluation.isReleased())
						if (evaluation != null)
						{
							count = evaluation.getTotalPosts();
							
							// is late
							isLate = evaluation.isLate();
							
							if (allScores || evaluation.isReleased())
							{
								startedDate = evaluation.getFirstPostTime();
								finishedDate = evaluation.getLastPostTime();
								if (startedDate !=null && finishedDate != null && (startedDate.compareTo(finishedDate) == 0))
								{
									finishedDate = null;
								}
								evaluatedDate = evaluation.getEvaluatedDate();
								reviewedDate = evaluation.getReviewedDate();
								evaluationNotReviewed = null;
								// count = evaluation.getTotalPosts();
								
								if (jforumSecurityService.isJForumFacilitator(context))
								{
									gradingLink = getJforumReviewLink(context, forum.getId(), "viewUserForumReplies", userId, "indvidualStudentGrades");
									if (evaluation.isReleased())
									{
										reviewLink = getJforumReviewLink(context, forum.getId(), "viewUserForumGrade", userId, "indvidualStudentGrades");
									}
								}
								else if (evaluation.isReleased())
								{
									reviewLink = getJforumReviewLink(context, forum.getId(), "viewUserForumGrade", userId, "indvidualStudentGrades");
								}
								
								score = evaluation.getScore();
								
								evaluationNotReviewed = Boolean.FALSE;
			
								if (evaluation.getReviewedDate() != null)
								{
									if (evaluation.getReviewedDate().before(evaluation.getEvaluatedDate()))
									{
										evaluationNotReviewed = Boolean.TRUE;
									}
								}
								else if (evaluation.getEvaluatedDate() != null)
								{
									evaluationNotReviewed = Boolean.TRUE;
								}
								
								released = evaluation.isReleased();
								evaluationId = String.valueOf(evaluation.getId());
							}
						}
						
						// access status
						if ((open != null) || (due != null) || (close != null))
						{
							if (accessStatus == GradebookItemAccessStatus.published)
							{
								if ((open != null) && now.before(open))
								{
									if (isHideUntilOpen.booleanValue())
									{
										accessStatus = GradebookItemAccessStatus.published_hidden;
										// score = null;
									}
									else
									{
										accessStatus = GradebookItemAccessStatus.published_not_yet_open;
										// score = null;
									}
								}
								else if (close != null)
								{
									if (now.after(close))
									{
										accessStatus = GradebookItemAccessStatus.published_closed;
									}
								}
								else if (due != null)
								{
									if (now.after(due))
									{
										accessStatus = GradebookItemAccessStatus.published_closed;
									}
								}
							}
						}
						
						// make the item
						GradebookItem gItem = this.gradebookService.newGradebookItem(id, title, points, null, due, open, close, GradebookItemType.forum, accessStatus, null);
						ParticipantItemDetails pItem = this.gradebookService.newParticipantItemDetails(userId, null, null, null, null, count, evaluatedDate, reviewedDate, score, startedDate, finishedDate, evaluationNotReviewed, released, reviewLink, gradingLink, null, evaluationId, isLate, null);
						if (userItemSpecialAccess != null)
						{
							pItem.setUserItemSpecialAccess(userItemSpecialAccess);
						}						
						ParticipantGradebookItem uItem = this.gradebookService.newParticipantGradebookItem(id, gItem, pItem);
						gradebookItems.add(uItem);
					}
					else if (forum.getTopics().size() > 0)
					{
						for (Topic topic : forum.getTopics())
						{
							if (topic.isGradeTopic())
							{
								Grade grade = topic.getGrade();
								
								if (grade == null || !grade.isAddToGradeBook())
								{
									continue;
								}
								
								id = null;
								title = null;
								points = null;
								open = null;
								due = null;
								close = null;
								score = null;
								GradebookItemAccessStatus accessStatus = GradebookItemAccessStatus.published;
								startedDate = null;
								finishedDate = null;
								evaluatedDate = null;
								reviewedDate = null;
								evaluationNotReviewed = null;
								count = 0;
								isHideUntilOpen = Boolean.FALSE;
								boolean validDates = true;
								reviewLink = null;
								gradingLink = null;
								released = null;
								evaluationId = null;
								isLate = Boolean.FALSE;
								
								id = JForumItemProvider.TOPIC + "-" + String.valueOf(topic.getId());
								title = topic.getTitle();
								points = grade.getPoints();
								
								// TODO consider special access
								if (topic.getAccessDates() != null)
								{
									open = topic.getAccessDates().getOpenDate();
									due = topic.getAccessDates().getDueDate();
									close = topic.getAccessDates().getAllowUntilDate();
									validDates = topic.getAccessDates().isDatesValid();
									
									if (open != null)
									{
										isHideUntilOpen = topic.getAccessDates().isHideUntilOpen();
									}
								}
								
								if ((open == null && due == null && close == null) && (forum.getAccessDates() != null) )
								{
									open = forum.getAccessDates().getOpenDate();
									due = forum.getAccessDates().getDueDate();
									close = forum.getAccessDates().getAllowUntilDate();
									validDates = forum.getAccessDates().isDatesValid();
									
									if (open != null)
									{
										isHideUntilOpen = forum.getAccessDates().isHideUntilOpen();
									}
								}
								
								if ((open == null && due == null && close == null) && (category.getAccessDates() != null) )
								{
									open = category.getAccessDates().getOpenDate();
									due = category.getAccessDates().getDueDate();
									close = category.getAccessDates().getAllowUntilDate();
									validDates = category.getAccessDates().isDatesValid();
									
									if (open != null)
									{
										isHideUntilOpen = category.getAccessDates().isHideUntilOpen();
									}
								}
								
								// access status
								if (((open != null) || (due != null) || (close != null)) && (!validDates))
								{
									accessStatus = GradebookItemAccessStatus.invalid;
									// ignore items with invalid dates
									continue;
								}
								// otherwise check for deny access
								else
								{
									if (forum.getAccessType() == Forum.ACCESS_DENY)
									{
										accessStatus = GradebookItemAccessStatus.unpublished;
										// ignore access deny items
										continue;
									}
								}
								
								// user special access
								List<SpecialAccess> userTopicSpecialAccessList = null;
								SpecialAccess userTopicSpecialAccess = null;
								UserItemSpecialAccess userItemSpecialAccess = null;
								if (open != null || due != null || close != null)
								{
									userTopicSpecialAccessList = topic.getSpecialAccess();
									
									if (userTopicSpecialAccessList != null && userTopicSpecialAccessList.size() == 1)
									{
										userTopicSpecialAccess = userTopicSpecialAccessList.get(0);
										Date openDate = userTopicSpecialAccess.getAccessDates().getOpenDate();
										Date dueDate = userTopicSpecialAccess.getAccessDates().getDueDate();
										Date acceptUntilDate = userTopicSpecialAccess.getAccessDates().getAllowUntilDate();
										Boolean hideUntilOpen =  userTopicSpecialAccess.getAccessDates().isHideUntilOpen();
										Boolean overrideOpenDate = userTopicSpecialAccess.isOverrideStartDate();
										Boolean overrideDueDate = userTopicSpecialAccess.isOverrideEndDate();
										Boolean overrideAcceptUntilDate = userTopicSpecialAccess.isOverrideAllowUntilDate();
										Boolean overrideHideUntilOpen = userTopicSpecialAccess.isOverrideHideUntilOpen();
										Boolean datesValid = userTopicSpecialAccess.isForumSpecialAccessDatesValid(forum);
										userItemSpecialAccess = this.gradebookService.newUserItemSpecialAccessImpl(openDate, dueDate, acceptUntilDate, hideUntilOpen, overrideOpenDate, overrideDueDate, overrideAcceptUntilDate, overrideHideUntilOpen, datesValid);
									}
								}
								
								Date now = new Date();
								
								/*
								// ignore not open items
								if (isHideUntilOpen && open != null && now.before(open))
								{
									continue;
								}
								*/
								
								Evaluation evaluation = topic.getUserEvaluation();

								// if (evaluation != null && evaluation.isReleased())
								if (evaluation != null)
								{
									count = evaluation.getTotalPosts();
									
									// is late
									isLate = evaluation.isLate();
									
									if (allScores || evaluation.isReleased())
									{
										startedDate = evaluation.getFirstPostTime();
										finishedDate = evaluation.getLastPostTime();
										if (startedDate !=null && finishedDate != null && (startedDate.compareTo(finishedDate) == 0))
										{
											finishedDate = null;
										}
										evaluatedDate = evaluation.getEvaluatedDate();
										reviewedDate = evaluation.getReviewedDate();
										evaluationNotReviewed = null;
										// count = evaluation.getTotalPosts();
										
										if (jforumSecurityService.isJForumFacilitator(context))
										{
											gradingLink = getJforumReviewLink(context, topic.getId(), "viewUserTopicReplies", userId, "indvidualStudentGrades");
											if (evaluation.isReleased())
											{
												reviewLink = getJforumReviewLink(context, topic.getId(), "viewUserTopicGrade", userId, "indvidualStudentGrades");
											}
										}
										else if (evaluation.isReleased())
										{
											reviewLink = getJforumReviewLink(context, topic.getId(), "viewUserTopicGrade", userId, "indvidualStudentGrades");
										}
										
										score = evaluation.getScore();
										
										evaluationNotReviewed = Boolean.FALSE;
					
										if (evaluation.getReviewedDate() != null)
										{
											if (evaluation.getReviewedDate().before(evaluation.getEvaluatedDate()))
											{
												evaluationNotReviewed = Boolean.TRUE;
											}
										}
										else if (evaluation.getEvaluatedDate() != null)
										{
											evaluationNotReviewed = Boolean.TRUE;
										}
										
										released = evaluation.isReleased();
										evaluationId = String.valueOf(evaluation.getId());
									}
								}
								
								// access status
								if ((open != null) || (due != null) || (close != null))
								{
									if (accessStatus == GradebookItemAccessStatus.published)
									{
										if ((open != null) && (now.before(open)))
										{
											if (isHideUntilOpen.booleanValue())
											{
												accessStatus = GradebookItemAccessStatus.published_hidden;
												// score = null;
											}
											else
											{
												accessStatus = GradebookItemAccessStatus.published_not_yet_open;
												// score = null;
											}
										}
										else if (close != null)
										{
											if (now.after(close))
											{
												accessStatus = GradebookItemAccessStatus.published_closed;
											}
										}
										else if (due != null)
										{
											if (now.after(due))
											{
												accessStatus = GradebookItemAccessStatus.published_closed;
											}
										}
									}
								}
												
								// make the item
								GradebookItem gItem = this.gradebookService.newGradebookItem(id, title, points, null, due, open, close, GradebookItemType.topic, accessStatus, null);
								ParticipantItemDetails pItem = this.gradebookService.newParticipantItemDetails(userId, null, null, null, null, count, evaluatedDate, reviewedDate, score, startedDate, finishedDate, evaluationNotReviewed, released, reviewLink, gradingLink, null, evaluationId, isLate, null);
								if (userItemSpecialAccess != null)
								{
									pItem.setUserItemSpecialAccess(userItemSpecialAccess);
								}								
								ParticipantGradebookItem uItem = this.gradebookService.newParticipantGradebookItem(id, gItem, pItem);
								gradebookItems.add(uItem);
							}
						}
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
		this.gradebookService.registerProvider(this);
		
		if (logger.isInfoEnabled())
		{
			logger.info("init()");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean modifyJforumScores(String context, String itemId, List<ParticipantItemDetails> participantItemDetails, String modifiedByUserId, Date scoresFetchedTime)
	{
		if ((context == null || context.trim().length() == 0) || (itemId == null || itemId.trim().length() == 0) || (participantItemDetails == null || participantItemDetails.size() == 0) || (modifiedByUserId == null || modifiedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		if (!jforumSecurityService.isJForumFacilitator(context, modifiedByUserId))
		{
			//throw new IllegalArgumentException("User not authorized to perform this function.");
			return false;
		}
		
		boolean categoryItem = itemId.startsWith(CATEGORY + "-");
		boolean forumItem = itemId.startsWith(FORUM + "-");
		boolean topicItem = itemId.startsWith(TOPIC + "-");
		
		int id;
		try
		{
			id = Integer.parseInt(itemId.substring(itemId.indexOf("-") + 1));

		}
		catch (NumberFormatException e)
		{
			logger.warn("error in parsing of item id.", e);
			return false;
		}
		
		// update scores and released status
		List<Evaluation> exisEvaluations = null;
		List<org.etudes.api.app.jforum.Evaluation> gradeEvaluations = null;
		
		if (categoryItem)
		{
			Category category = jforumCategoryService.getCategory(id);
			
			if (category != null && category.isGradable())
			{
				// Grade grade = category.getGrade();
				Grade grade = jforumGradeService.getByCategoryId(id);
				
				if (grade != null)
				{
					category.setGrade(grade);
					gradeEvaluations = new ArrayList<Evaluation>();
					exisEvaluations = jforumGradeService.getCategoryEvaluations(id);
					
					// existing scores
					Map<String, Evaluation> exisEvalMap = new HashMap<String, Evaluation>();
					
					for(org.etudes.api.app.jforum.Evaluation eval : exisEvaluations)
					{
						exisEvalMap.put(eval.getSakaiUserId(), eval);
					}
					
					String userId = null;
					Evaluation evaluation = null;
					
					for (ParticipantItemDetails participantItemDetail :participantItemDetails)
					{			
						userId = participantItemDetail.getUserId();
						
						evaluation = null;
												
						checkAndSetModifiedItemEvaluation(userId, participantItemDetail, grade, modifiedByUserId, scoresFetchedTime, gradeEvaluations, exisEvalMap);						
					}
					
					// add not modified evaluations
					for (Evaluation exisEval : exisEvalMap.values())
					{
						gradeEvaluations.add(exisEval);
					}
					
					category.getEvaluations().clear();
					category.getEvaluations().addAll(gradeEvaluations);
					
					try
					{
						jforumCategoryService.evaluateCategory(category);
					}
					catch (JForumAccessException e)
					{
						if (logger.isErrorEnabled())
						{
							logger.warn("Error while evaluating category", e);
						}
					}
				}
			}
		}
		else if (forumItem)
		{
			Forum forum = jforumForumService.getForum(id);
			
			if (forum != null && forum.getGradeType() == Grade.GradeType.FORUM.getType())
			{
				gradeEvaluations = new ArrayList<Evaluation>();
				
				org.etudes.api.app.jforum.Grade grade = forum.getGrade();
				
				if (grade != null)
				{
					exisEvaluations = jforumGradeService.getForumEvaluations(id);
					
					// existing scores
					Map<String, Evaluation> exisEvalMap = new HashMap<String, Evaluation>();
					
					for(org.etudes.api.app.jforum.Evaluation eval : exisEvaluations)
					{
						exisEvalMap.put(eval.getSakaiUserId(), eval);
					}
					
					String userId = null;
					Evaluation evaluation = null;
					
					for (ParticipantItemDetails participantItemDetail :participantItemDetails)
					{			
						userId = participantItemDetail.getUserId();
						
						evaluation = null;
						
						checkAndSetModifiedItemEvaluation(userId, participantItemDetail, grade, modifiedByUserId, scoresFetchedTime, gradeEvaluations, exisEvalMap);
					}
					
					// add not modified evaluations
					for (Evaluation exisEval : exisEvalMap.values())
					{
						gradeEvaluations.add(exisEval);
					}
					
					forum.getEvaluations().clear();
					forum.getEvaluations().addAll(gradeEvaluations);
					
					try
					{
						jforumForumService.evaluateForum(forum);
					}
					catch (JForumAccessException e)
					{
						if (logger.isErrorEnabled())
						{
							logger.warn("Error while evaluating forum", e);
						}
					}
				}
			}
		}
		else if (topicItem)
		{
			Topic topic = jforumPostService.getTopic(id);
			
			if (topic != null && topic.isGradeTopic())
			{
				gradeEvaluations = new ArrayList<Evaluation>();
				
				org.etudes.api.app.jforum.Grade grade = topic.getGrade();
				
				if (grade != null)
				{
					exisEvaluations = jforumGradeService.getTopicEvaluations(topic.getForumId(), id);
					
					// existing scores
					Map<String, Evaluation> exisEvalMap = new HashMap<String, Evaluation>();
					
					for(org.etudes.api.app.jforum.Evaluation eval : exisEvaluations)
					{
						exisEvalMap.put(eval.getSakaiUserId(), eval);
					}
					
					String userId = null;
					Evaluation evaluation = null;
					
					for (ParticipantItemDetails participantItemDetail :participantItemDetails)
					{			
						userId = participantItemDetail.getUserId();
						
						evaluation = null;
						
						checkAndSetModifiedItemEvaluation(userId, participantItemDetail, grade, modifiedByUserId, scoresFetchedTime, gradeEvaluations, exisEvalMap);
					}
					
					// add not modified evaluations
					for (Evaluation exisEval : exisEvalMap.values())
					{
						gradeEvaluations.add(exisEval);
					}
					
					topic.getEvaluations().clear();
					topic.getEvaluations().addAll(gradeEvaluations);
					
					try
					{
						jforumPostService.evaluateTopic(topic);
					}
					catch (JForumAccessException e)
					{
						if (logger.isErrorEnabled())
						{
							logger.warn("Error while evaluating topic", e);
						}
					}
				}
			}			
		}
		
		return true;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean modifyJforumUserScore(String context, String itemId, ParticipantItemDetails participantItemDetails, String modifiedByUserId, Date scoreFetchedTime)
	{
		// update score
		if ((context == null || context.trim().length() == 0) || (itemId == null || itemId.trim().length() == 0) || (participantItemDetails == null) || (modifiedByUserId == null || modifiedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		if (!jforumSecurityService.isJForumFacilitator(context, modifiedByUserId))
		{
			return false;
		}
		
		boolean categoryItem = itemId.startsWith(CATEGORY + "-");
		boolean forumItem = itemId.startsWith(FORUM + "-");
		boolean topicItem = itemId.startsWith(TOPIC + "-");
		
		int id;
		try
		{
			id = Integer.parseInt(itemId.substring(itemId.indexOf("-") + 1));

		}
		catch (NumberFormatException e)
		{
			logger.warn("error in parsing of item id.", e);
			return false;
		}
		
		// update user score and released status
		if (categoryItem && participantItemDetails.getUserId() != null && participantItemDetails.getUserId().trim().length() > 0)
		{
			Category category = jforumCategoryService.getCategory(id);
			
			if (category != null && category.isGradable())
			{
				Grade grade = jforumGradeService.getByCategoryId(id);
				
				if (grade != null)
				{
					Evaluation evaluation = jforumGradeService.getUserCategoryEvaluation(id, participantItemDetails.getUserId());
					
					evaluateUser(participantItemDetails, modifiedByUserId, grade, evaluation, scoreFetchedTime);
				}
			}
		}
		else if (forumItem && participantItemDetails.getUserId() != null && participantItemDetails.getUserId().trim().length() > 0)
		{
			Forum forum = jforumForumService.getForum(id);
			
			if (forum != null && forum.getGradeType() == Grade.GradeType.FORUM.getType())
			{
				org.etudes.api.app.jforum.Grade grade = forum.getGrade();
				
				if (grade != null)
				{
					Evaluation evaluation = jforumGradeService.getUserForumEvaluation(id, participantItemDetails.getUserId());
					
					evaluateUser(participantItemDetails, modifiedByUserId, grade, evaluation, scoreFetchedTime);
				}
			}
		}
		else if (topicItem && participantItemDetails.getUserId() != null && participantItemDetails.getUserId().trim().length() > 0)
		{
			Topic topic = jforumPostService.getTopic(id);
			
			if (topic != null && topic.isGradeTopic())
			{
				org.etudes.api.app.jforum.Grade grade = topic.getGrade();
				
				if (grade != null)
				{
					Evaluation evaluation = jforumGradeService.getUserTopicEvaluation(id, participantItemDetails.getUserId());
					
					evaluateUser(participantItemDetails, modifiedByUserId, grade, evaluation, scoreFetchedTime);
				}
			}
		}
		
		return true;
	}

	/**
	 * @param gradebookService the gradebookService to set
	 */
	public void setGradebookService(GradebookService gradebookService)
	{
		this.gradebookService = gradebookService;
	}
	
	/**
	 * @param jforumCategoryService the jforumCategoryService to set
	 */
	public void setJforumCategoryService(JForumCategoryService jforumCategoryService)
	{
		this.jforumCategoryService = jforumCategoryService;
	}

	
	/**
	 * @param jforumForumService the jforumForumService to set
	 */
	public void setJforumForumService(JForumForumService jforumForumService)
	{
		this.jforumForumService = jforumForumService;
	}
	
	/**
	 * @param jforumGradeService the jforumGradeService to set
	 */
	public void setJforumGradeService(JForumGradeService jforumGradeService)
	{
		this.jforumGradeService = jforumGradeService;
	}
	
	/**
	 * @param jforumPostService the jforumPostService to set
	 */
	public void setJforumPostService(JForumPostService jforumPostService)
	{
		this.jforumPostService = jforumPostService;
	}
	
	/**
	 * @param jforumSecurityService the jforumSecurityService to set
	 */
	public void setJforumSecurityService(JForumSecurityService jforumSecurityService)
	{
		this.jforumSecurityService = jforumSecurityService;
	}
	
	/**
	 * @param jforumService
	 *        the jforumService to set
	 */
	public void setJforumService(JForumService jforumService)
	{
		this.jforumService = jforumService;
	}
	
	/**
	 * @param jforumSpecialAccessService the jforumSpecialAccessService to set
	 */
	public void setJforumSpecialAccessService(JForumSpecialAccessService jforumSpecialAccessService)
	{
		this.jforumSpecialAccessService = jforumSpecialAccessService;
	}
	
	/**
	 * Set the SiteService.
	 * 
	 * @param service
	 *        The SiteService.
	 */
	public void setSiteService(SiteService service)
	{
		this.siteService = service;
	}
	
	/**
	 * check and set score and released status of modified item evaluation
	 * 
	 * @param userId	User id
	 * 
	 * @param participantItemDetail	Participant item details
	 * 
	 * @param modifiedByUserId	Modified by user id
	 * 
	 * @param scoresFetchedTime	Scores fetched time
	 * 
	 * @param gradeEvaluations	Grade evaluations
	 * 
	 * @param grade		Grade
	 * 
	 * @param exisEvalMap	Existing evaluations maps
	 */
	protected void checkAndSetModifiedItemEvaluation(String userId, ParticipantItemDetails participantItemDetail, Grade grade, String modifiedByUserId, Date scoresFetchedTime, List<Evaluation> gradeEvaluations, Map<String, Evaluation> exisEvalMap)
	{
		if ((userId == null || userId.trim().length() == 0) || (participantItemDetail == null) || (modifiedByUserId == null || modifiedByUserId.trim().length() == 0) || (gradeEvaluations == null) || (grade == null) || (exisEvalMap == null))
		{
			return;
		}
		
		Evaluation evaluation = null;
		evaluation = exisEvalMap.get(userId);
		
		if (evaluation != null)
		{
			boolean evaluationChanged = false;
			if (scoresFetchedTime != null)
			{
				if (evaluation.getEvaluatedDate().before(scoresFetchedTime))
				{
					evaluationChanged = true;
				}
			}
			else
			{
				evaluationChanged = true;
			}
			
			if (evaluationChanged)
			{
				evaluation = exisEvalMap.remove(userId);
				evaluation.setEvaluatedBySakaiUserId(modifiedByUserId);								
				evaluation.setScore(participantItemDetail.getScore());
				evaluation.setReleased(participantItemDetail.getIsReleased());								
				gradeEvaluations.add(evaluation);
			}
		}
		else
		{
			evaluation = jforumGradeService.newEvaluation(grade.getId(), modifiedByUserId, userId);							
			evaluation.setScore(participantItemDetail.getScore());
			evaluation.setReleased(participantItemDetail.getIsReleased());							
			gradeEvaluations.add(evaluation);
		}
	}
	
	/**
	 * Evaluate user
	 * 
	 * @param participantItemDetails	Participant item details
	 * 
	 * @param modifiedByUserId	Modified by user id
	 * 
	 * @param grade			Grade
	 * 
	 * @param evaluation	Evaluation
	 */
	protected void evaluateUser(ParticipantItemDetails participantItemDetails, String modifiedByUserId, Grade grade, Evaluation evaluation, Date scoreFetchedTime)
	{
		if ((participantItemDetails == null) || (modifiedByUserId == null || modifiedByUserId.trim().length() == 0) || (grade == null))
		{
			return;
		}
		
		if (evaluation != null)
		{
			if (scoreFetchedTime != null)
			{
				if (evaluation.getEvaluatedDate().after(scoreFetchedTime))
				{
					return;
				}
			}
			evaluation.setEvaluatedBySakaiUserId(modifiedByUserId);	
		}
		else
		{
			evaluation = jforumGradeService.newEvaluation(grade.getId(), modifiedByUserId, participantItemDetails.getUserId());	
		}				

		try
		{
			evaluation.setScore(participantItemDetails.getScore());
			evaluation.setReleased(participantItemDetails.getIsReleased());
			
			jforumGradeService.addModifyUserEvaluation(evaluation);
		}
		catch (JForumAccessException e)
		{
			if (logger.isErrorEnabled())
			{
				logger.warn("Error while evaluating user", e);
			}
		}
	}
	
	/**
	 * gets the category access status if category has no dates based on category forum dates
	 * 
	 * @param category
	 *        The category
	 * @param accessStatus
	 *        Access status
	 * @return
	 */
	protected GradebookItemAccessStatus getCategoryAccessStatus(Category category)
	{
		GradebookItemAccessStatus accessStatus = GradebookItemAccessStatus.published;
		Date now = new Date();

		List<Forum> forums = category.getForums();
		int forumDenyAccess = 0;

		for (Forum forum : forums)
		{
			if (forum.getAccessType() != Forum.ACCESS_DENY)
			{
				if ((forum.getAccessDates() != null)
						&& ((forum.getAccessDates().getDueDate() != null) || (forum.getAccessDates().getOpenDate() != null) || (forum
								.getAccessDates().getAllowUntilDate() != null)))
				{
					Date forumOpen = forum.getAccessDates().getOpenDate();
					Boolean isHideUntilOpen = Boolean.FALSE;
					Date forumDue = forum.getAccessDates().getDueDate();
					Date forumAllowUntil = forum.getAccessDates().getAllowUntilDate();

					if (forumOpen != null)
					{
						isHideUntilOpen = forum.getAccessDates().isHideUntilOpen();
					}

					// invalid dates
					if (((forumOpen != null) || (forumDue != null) || (forumAllowUntil != null)) && !forum.getAccessDates().isDatesValid())
					{
						continue;
					}

					if (forumOpen == null)
					{
						// no forum open date
						accessStatus = GradebookItemAccessStatus.published;

						if (forumAllowUntil != null)
						{
							if (now.after(forumAllowUntil))
							{
								accessStatus = GradebookItemAccessStatus.published_closed;
							}
						}
						else if (forumDue != null)
						{
							if (now.after(forumDue))
							{
								accessStatus = GradebookItemAccessStatus.published_closed;
							}
						}
					}
					else
					{
						if (now.before(forumOpen) && isHideUntilOpen.booleanValue())
						{
							if (accessStatus != GradebookItemAccessStatus.published_closed)
							{
								accessStatus = GradebookItemAccessStatus.published_hidden;
							}
						}
						else
						{
							accessStatus = GradebookItemAccessStatus.published;

							if (forumAllowUntil != null)
							{
								if (now.after(forumAllowUntil))
								{
									accessStatus = GradebookItemAccessStatus.published_closed;
								}
							}
							else if (forumDue != null)
							{
								if (now.after(forumDue))
								{
									accessStatus = GradebookItemAccessStatus.published_closed;
								}
							}
						}
					}

					if (accessStatus == GradebookItemAccessStatus.published)
					{
						break;
					}
				}
				else
				{
					// no forum dates
					accessStatus = GradebookItemAccessStatus.published;
					break;
				}
			}
			else
			{
				forumDenyAccess++;
			}
		}

		if (forums.size() == forumDenyAccess)
		{
			accessStatus = GradebookItemAccessStatus.published_hidden;
		}
		
		return accessStatus;
	}
	
	/**
	 * {@inheritDoc}
	 */
	// @Override
	protected List<ParticipantItemDetails> getJforumItemPostsEvaluations(String context, String itemId, String fetchedByUserId, List<Participant> participants)
	{
		if ((context == null || context.trim().length() == 0) || (itemId == null || itemId.trim().length() == 0) || (fetchedByUserId == null || fetchedByUserId.trim().length() == 0) || (participants == null))
		{
			return new ArrayList<ParticipantItemDetails>();
		}

		boolean categoryItem = itemId.startsWith(CATEGORY + "-");
		boolean forumItem = itemId.startsWith(FORUM + "-");
		boolean topicItem = itemId.startsWith(TOPIC + "-");

		int id;
		try
		{
			id = Integer.parseInt(itemId.substring(itemId.indexOf("-") + 1));

		}
		catch (NumberFormatException e)
		{
			logger.warn("error in parsing of item id.", e);
			return null;
		}

		ArrayList<ParticipantItemDetails> rv = new ArrayList<ParticipantItemDetails>();
		
		if (categoryItem)
		{
			Category category = this.jforumService.getUsersPostCountByCategory(context, id);

			if (category != null)
			{
				if (category.isGradable() && (category.getForums() != null) && (category.getForums().isEmpty()))
				{
					// Map<String, Integer> posters = category.getUserPostCount();

					Category catEval = this.jforumService.getEvaluationsByCategory(context, id, true);

					List<Evaluation> catEvaluations = null;
					if (catEval != null)
					{
						catEvaluations = catEval.getEvaluations();
					}

					Map<String, Evaluation> userEvaluations = new HashMap<String, Evaluation>();

					if (catEvaluations != null)
					{
						for (Evaluation eval : catEvaluations)
						{
							userEvaluations.put(eval.getSakaiUserId(), eval);
						}
					}

					for (Participant p : participants)
					{
						Date evaluated = null;
						Date reviewed = null;
						Integer posts = Integer.valueOf(0);
						Float score = null;
						Date lastPostTime = null;
						Date firstPostTime = null;
						Boolean evaluationNotReviewed = null;
						Boolean released = null;
						String reviewLink = null;
						String gradingLink = null;
						Boolean isLate = Boolean.FALSE;

						if (userEvaluations.containsKey(p.getUserId()))
						{
							Evaluation userEvaluation = userEvaluations.get(p.getUserId());
							
							posts = userEvaluation.getTotalPosts();
							
							if (userEvaluation.isReleased())
							{
								score = userEvaluation.getScore();
								// posts = userEvaluation.getTotalPosts();
								lastPostTime = userEvaluation.getLastPostTime();
								firstPostTime = userEvaluation.getFirstPostTime();
								evaluated = userEvaluation.getEvaluatedDate();
								reviewed = userEvaluation.getReviewedDate();
								released = Boolean.TRUE;
								isLate = userEvaluation.isLate();
								
								evaluationNotReviewed = Boolean.FALSE;
								
								if (userEvaluation.getReviewedDate() != null)
								{
									if (userEvaluation.getReviewedDate().before(userEvaluation.getEvaluatedDate()))
									{
										evaluationNotReviewed = Boolean.TRUE;
									}
								}
								else if (userEvaluation.getEvaluatedDate() != null)
								{
									evaluationNotReviewed = Boolean.TRUE;
								}
								
								if (jforumSecurityService.isUserFacilitator(context, fetchedByUserId))
								{
									gradingLink = getJforumReviewLink(context, category.getId(), "viewUserCategoryReplies", p.getUserId(), "assessmentDetails");
									reviewLink = getJforumReviewLink(context, category.getId(), "viewUserCategoryGrade", p.getUserId(), "assessmentDetails");
								}
								else
								{
									reviewLink = getJforumReviewLink(context, category.getId(), "viewUserCategoryGrade", p.getUserId(), "assessmentDetails");
								}
							}
						}

						/*if (posters.containsKey(p.getUserId()))
						{
							posts = posters.get(p.getUserId());
						}*/
						
						// ParticipantItemDetails pjid = this.gradebookService.newParticipantItemDetails(p.getUserId(), p.getDisplayId(), p.getGroupTitle(), p.getSortName(), p.getStatus(), posts, evaluated, reviewed, score, lastPostTime, firstPostTime, evaluationNotReviewed, released);
						ParticipantItemDetails pjid = this.gradebookService.newParticipantItemDetails(p.getUserId(), p.getDisplayId(), p.getGroupTitle(), p.getSortName(), p.getStatus(), posts, evaluated, reviewed, score, lastPostTime, firstPostTime, evaluationNotReviewed, released, reviewLink, gradingLink, null, null, isLate, null);
						
						// private message link
						pjid.setPrivateMessageLink(p.getPrivateMessageLink());
						
						rv.add(pjid);
					}
				}
			}
		}
		else if (forumItem)
		{
			Category category = this.jforumService.getUsersPostCountByForum(context, id);

			if ((category != null) && (category.getForums().size() == 1))
			{
				Forum forum = category.getForums().get(0);

				// Map<String, Integer> posters = forum.getUserPostCount();

				Category catEval = this.jforumService.getEvaluationsByForum(context, id, true);

				List<Evaluation> forumEvaluations = null;
				if (catEval != null)
				{
					if (catEval.getForums().size() == 1)
					{
						Forum forumEval = catEval.getForums().get(0);

						forumEvaluations = forumEval.getEvaluations();
					}
				}

				Map<String, Evaluation> userEvaluations = new HashMap<String, Evaluation>();

				if (forumEvaluations != null)
				{
					for (Evaluation eval : forumEvaluations)
					{
						userEvaluations.put(eval.getSakaiUserId(), eval);
					}
				}
				
				// get users special access details
				Map<Integer, UserItemSpecialAccess> forumUserItemSpecialAccessList = new HashMap<Integer, UserItemSpecialAccess>();
				
				if (forum.getAccessDates() != null)
				{
					Date open = forum.getAccessDates().getOpenDate();
					Date due = forum.getAccessDates().getDueDate();
					Date close = forum.getAccessDates().getAllowUntilDate();
					boolean validDates = forum.getAccessDates().isDatesValid();
					
					if (((open != null) || (due != null) || (close != null)) && (validDates))
					{
						List<SpecialAccess> forumSpecialAccessList = this.jforumSpecialAccessService.getByForum(forum.getId());
						
						Date openDate = null; 
						Date dueDate = null; 
						Date acceptUntilDate = null;
						Boolean hideUntilOpen = null;
						Boolean overrideOpenDate = null;
						Boolean overrideDueDate = null;
						Boolean overrideAcceptUntilDate = null;
						Boolean overrideHideUntilOpen = null;
						Boolean datesValid = Boolean.TRUE;
												
						if (forumSpecialAccessList != null && !forumSpecialAccessList.isEmpty())
						{
							UserItemSpecialAccess userItemSpecialAccess = null;
													
							for (SpecialAccess forumSpecialAccess : forumSpecialAccessList)
							{
								if (forumSpecialAccess.getAccessDates() != null)
								{
									// forum special access
									if (forumSpecialAccess.getForumId() > 0 && forumSpecialAccess.getTopicId() == 0)
									{
										openDate = forumSpecialAccess.getAccessDates().getOpenDate();
										dueDate = forumSpecialAccess.getAccessDates().getDueDate();
										acceptUntilDate = forumSpecialAccess.getAccessDates().getAllowUntilDate();
										hideUntilOpen =  forumSpecialAccess.getAccessDates().isHideUntilOpen();
										overrideOpenDate = forumSpecialAccess.isOverrideStartDate();
										overrideDueDate = forumSpecialAccess.isOverrideEndDate();
										overrideAcceptUntilDate = forumSpecialAccess.isOverrideAllowUntilDate();
										overrideHideUntilOpen = forumSpecialAccess.isOverrideHideUntilOpen();
										datesValid = forumSpecialAccess.isForumSpecialAccessDatesValid(forum);
										
										for (Integer userId : forumSpecialAccess.getUserIds())
										{
											userItemSpecialAccess = this.gradebookService.newUserItemSpecialAccessImpl(openDate, dueDate, acceptUntilDate, hideUntilOpen, overrideOpenDate, overrideDueDate, overrideAcceptUntilDate, overrideHideUntilOpen, datesValid);
											
											forumUserItemSpecialAccessList.put(userId, userItemSpecialAccess);
										}
									}
								}
							}
						}
					}
				}

				for (Participant p : participants)
				{
					Date evaluated = null;
					Date reviewed = null;
					Integer posts = Integer.valueOf(0);
					Float score = null;
					Date lastPostTime = null;
					Date firstPostTime = null;
					Boolean evaluationNotReviewed = null;
					Boolean released = null;
					String reviewLink = null;
					String gradingLink = null;
					UserItemSpecialAccess userItemSpecialAccess = null;
					Boolean isLate = Boolean.FALSE;
					
					if (userEvaluations.containsKey(p.getUserId()))
					{
						Evaluation userEvaluation = userEvaluations.get(p.getUserId());
						posts = userEvaluation.getTotalPosts();
						
						if (userEvaluation.isReleased())
						{
							score = userEvaluation.getScore();
							// posts = userEvaluation.getTotalPosts();
							lastPostTime = userEvaluation.getLastPostTime();
							firstPostTime = userEvaluation.getFirstPostTime();
							evaluated = userEvaluation.getEvaluatedDate();
							reviewed = userEvaluation.getReviewedDate();
							released = Boolean.TRUE;
							evaluationNotReviewed = Boolean.FALSE;
							isLate = userEvaluation.isLate();
							
							if (userEvaluation.getReviewedDate() != null)
							{
								if (userEvaluation.getReviewedDate().before(userEvaluation.getEvaluatedDate()))
								{
									evaluationNotReviewed = Boolean.TRUE;
								}
							}
							else if (userEvaluation.getEvaluatedDate() != null)
							{
								evaluationNotReviewed = Boolean.TRUE;
							}
							
							if (jforumSecurityService.isUserFacilitator(context, fetchedByUserId))
							{
								gradingLink = getJforumReviewLink(context, forum.getId(), "viewUserForumReplies", p.getUserId(), "assessmentDetails");
								reviewLink = getJforumReviewLink(context, forum.getId(), "viewUserForumGrade", p.getUserId(), "assessmentDetails");
							}
							else
							{
								reviewLink = getJforumReviewLink(context, forum.getId(), "viewUserForumGrade", p.getUserId(), "assessmentDetails");
							}
						}
						
						// user special access
						if (!forumUserItemSpecialAccessList.isEmpty())
						{
							userItemSpecialAccess = forumUserItemSpecialAccessList.get(userEvaluation.getUserId());
						}
					}

					/*if (posters.containsKey(p.getUserId()))
					{
						posts = posters.get(p.getUserId());
					}*/
					
					//ParticipantItemDetails pjid = this.gradebookService.newParticipantItemDetails(p.getUserId(), p.getDisplayId(), p.getGroupTitle(), p.getSortName(), p.getStatus(), posts, evaluated, reviewed, score, lastPostTime, firstPostTime, evaluationNotReviewed, released);
					ParticipantItemDetails pjid = this.gradebookService.newParticipantItemDetails(p.getUserId(), p.getDisplayId(), p.getGroupTitle(), p.getSortName(), p.getStatus(), posts, evaluated, reviewed, score, lastPostTime, firstPostTime, evaluationNotReviewed, released, reviewLink, gradingLink, null, null, isLate, null);
					if (userItemSpecialAccess != null)
					{
						pjid.setUserItemSpecialAccess(userItemSpecialAccess);
					}
					
					// private message link
					pjid.setPrivateMessageLink(p.getPrivateMessageLink());
					
					rv.add(pjid);
				}
			}
		}
		else if (topicItem)
		{
			Category category = this.jforumService.getUsersPostCountByTopic(context, id);

			if ((category.getForums().size() == 1) && (category.getForums().get(0).getTopics().size() == 1))
			{
				Topic topic = category.getForums().get(0).getTopics().get(0);

				//Map<String, Integer> posters = topic.getUserPostCount();

				Category catEval = this.jforumService.getEvaluationsByTopic(context, id, true);

				List<Evaluation> topicEvaluations = null;
				if (catEval != null)
				{
					if ((catEval.getForums().size() == 1) && (catEval.getForums().get(0).getTopics().size() == 1))
					{
						Topic topicEval = catEval.getForums().get(0).getTopics().get(0);

						topicEvaluations = topicEval.getEvaluations();
					}
				}

				Map<String, Evaluation> userEvaluations = new HashMap<String, Evaluation>();

				if (topicEvaluations != null)
				{
					for (Evaluation eval : topicEvaluations)
					{
						userEvaluations.put(eval.getSakaiUserId(), eval);
					}
				}
				
				// get users special access details
				Map<Integer, UserItemSpecialAccess> topicUserItemSpecialAccessList = new HashMap<Integer, UserItemSpecialAccess>();
				if (topic.getAccessDates() != null)
				{
					Date open = topic.getAccessDates().getOpenDate();
					Date due = topic.getAccessDates().getDueDate();
					Date close = topic.getAccessDates().getAllowUntilDate();
					boolean validDates = topic.getAccessDates().isDatesValid();
					
					if (((open != null) || (due != null) || (close != null)) && (validDates))
					{
						List<SpecialAccess> topicSpecialAccessList = this.jforumSpecialAccessService.getByTopic(topic.getForumId(), topic.getId());
						
						Date openDate = null; 
						Date dueDate = null; 
						Date acceptUntilDate = null;
						Boolean hideUntilOpen = null;
						Boolean overrideOpenDate = null;
						Boolean overrideDueDate = null;
						Boolean overrideAcceptUntilDate = null;
						Boolean overrideHideUntilOpen = null;
						Boolean datesValid = Boolean.TRUE;
						
						if (topicSpecialAccessList != null && !topicSpecialAccessList.isEmpty())
						{
							UserItemSpecialAccess userItemSpecialAccess = null;
													
							for (SpecialAccess topicSpecialAccess : topicSpecialAccessList)
							{
								if (topicSpecialAccess.getAccessDates() != null)
								{
									// forum special access
									if (topicSpecialAccess.getForumId() > 0 && topicSpecialAccess.getTopicId() > 0)
									{
										openDate = topicSpecialAccess.getAccessDates().getOpenDate();
										dueDate = topicSpecialAccess.getAccessDates().getDueDate();
										acceptUntilDate = topicSpecialAccess.getAccessDates().getAllowUntilDate();
										hideUntilOpen =  topicSpecialAccess.getAccessDates().isHideUntilOpen();
										overrideOpenDate = topicSpecialAccess.isOverrideStartDate();
										overrideDueDate = topicSpecialAccess.isOverrideEndDate();
										overrideAcceptUntilDate = topicSpecialAccess.isOverrideAllowUntilDate();
										overrideHideUntilOpen = topicSpecialAccess.isOverrideHideUntilOpen();
										datesValid = topicSpecialAccess.isTopicSpecialAccessDatesValid(topic);
										
										for (Integer userId : topicSpecialAccess.getUserIds())
										{
											userItemSpecialAccess = this.gradebookService.newUserItemSpecialAccessImpl(openDate, dueDate, acceptUntilDate, hideUntilOpen, overrideOpenDate, overrideDueDate, overrideAcceptUntilDate, overrideHideUntilOpen, datesValid);
											
											topicUserItemSpecialAccessList.put(userId, userItemSpecialAccess);
										}
									}
								}
							}
						}
					}
				}
				
				// user special access
				UserItemSpecialAccess userItemSpecialAccess = null;
				
				for (Participant p : participants)
				{
					Date evaluated = null;
					Date reviewed = null;
					Integer posts = Integer.valueOf(0);
					Float score = null;
					Date lastPostTime = null;
					Date firstPostTime = null;
					Boolean evaluationNotReviewed = null;
					Boolean released = null;
					String reviewLink = null;
					String gradingLink = null;
					Boolean isLate = Boolean.FALSE;

					if (userEvaluations.containsKey(p.getUserId()))
					{
						Evaluation userEvaluation = userEvaluations.get(p.getUserId());
						posts = userEvaluation.getTotalPosts();
						
						userItemSpecialAccess = topicUserItemSpecialAccessList.get(userEvaluation.getUserId());
						
						if (userEvaluation.isReleased())
						{
							score = userEvaluation.getScore();
							// posts = userEvaluation.getTotalPosts();
							lastPostTime = userEvaluation.getLastPostTime();
							firstPostTime = userEvaluation.getFirstPostTime();
							evaluated = userEvaluation.getEvaluatedDate();
							reviewed = userEvaluation.getReviewedDate();
							released = Boolean.TRUE;
							isLate = userEvaluation.isLate();
							
							evaluationNotReviewed = Boolean.FALSE;
							
							if (userEvaluation.getReviewedDate() != null)
							{
								if (userEvaluation.getReviewedDate().before(userEvaluation.getEvaluatedDate()))
								{
									evaluationNotReviewed = Boolean.TRUE;
								}
							}
							else if (userEvaluation.getEvaluatedDate() != null)
							{
								evaluationNotReviewed = Boolean.TRUE;
							}
							
							if (jforumSecurityService.isUserFacilitator(context, fetchedByUserId))
							{
								gradingLink = getJforumReviewLink(context, topic.getId(), "viewUserTopicReplies", p.getUserId(), "assessmentDetails");
								reviewLink = getJforumReviewLink(context, topic.getId(), "viewUserTopicGrade", p.getUserId(), "assessmentDetails");
							}
							else
							{
								reviewLink = getJforumReviewLink(context, topic.getId(), "viewUserTopicGrade", p.getUserId(), "assessmentDetails");
							}
						}
					}

					/*if (posters.containsKey(p.getUserId()))
					{
						posts = posters.get(p.getUserId());
					}*/
					
					// ParticipantItemDetails pjid = this.gradebookService.newParticipantItemDetails(p.getUserId(), p.getDisplayId(), p.getGroupTitle(), p.getSortName(), p.getStatus(), posts, evaluated, reviewed, score, lastPostTime, firstPostTime, evaluationNotReviewed, released);
					ParticipantItemDetails pjid = this.gradebookService.newParticipantItemDetails(p.getUserId(), p.getDisplayId(), p.getGroupTitle(), p.getSortName(), p.getStatus(), posts, evaluated, reviewed, score, lastPostTime, firstPostTime, evaluationNotReviewed, released, reviewLink, gradingLink, null, null, isLate, null);
					if (userItemSpecialAccess != null)
					{
						pjid.setUserItemSpecialAccess(userItemSpecialAccess);
					}
					
					// private message link
					pjid.setPrivateMessageLink(p.getPrivateMessageLink());
					
					rv.add(pjid);
				}
			}
		}

		return rv;
	}
	
	/**
	 * Get jforum user grade pop up link
	 * 
	 * @param context		Context
	 * 
	 * @param id			Id
	 * 
	 * @param gradeLink		Grade link
	 * 
	 * @param userId		User id
	 * 
	 * @return	Jforum user grade pop up link
	 */
	/*
	protected String getJforumGradeUserPopupLink(String context, int id, String gradeLink, String userId)
	{
		if ((context == null || context.trim().length() == 0) || (context == null || context.trim().length() == 0) || (context == null || context.trim().length() == 0))
		{
			return null;
		}
		
		String jforumToolId = null;
		String popupLink = null;
		
		try
		{
			Site site = this.siteService.getSite(context);
			ToolConfiguration config = site.getToolForCommonId("sakai.jforum.tool");
			if (config != null) jforumToolId = config.getId();
		}
		catch (IdUnusedException e)
		{
			logger.warn("getJforumGradeUserPopupLink: missing site: " + context);
		}
		
		// no tool id? No E3 GB in site!
		if (jforumToolId == null) return null;
			
		// link to review the jforum item
		if (id != 0)
		{
			popupLink = "/portal/directtool/" +jforumToolId + "/gradeForum/" + gradeLink + "/" + id + "/" + userId + JForumService.SERVLET_EXTENSION;
		}

		return popupLink;		
	}
	*/
	
	/**
	 * Get JForum item's review link
	 * 
	 * @param context	Context
	 * 
	 * @param id		Item id
	 * 
	 * @param userId	user id
	 * 
	 * @param returnViewParam return view parameter
	 * 
	 * @return null if not found else review link
	 */
	protected String getJforumReviewLink(String context, int id, String gradeLink, String userId, String returnViewParam)
	{
		if ((context == null || context.trim().length() == 0) || (context == null || context.trim().length() == 0) || (context == null || context.trim().length() == 0))
		{
			return null;
		}
		
		String gbToolId = null;
		String jforumToolId = null;
		String reviewLink = null;
		
		try
		{
			Site site = this.siteService.getSite(context);
			ToolConfiguration config = site.getToolForCommonId("sakai.jforum.tool");
			if (config != null) jforumToolId = config.getId();
			config = site.getToolForCommonId("e3.gradebook");
			if (config != null) gbToolId = config.getId();
		}
		catch (IdUnusedException e)
		{
			logger.warn("getJforumReviewLink: missing site: " + context);
		}
		
		// no tool id? No E3 GB in site!
		if (gbToolId == null || jforumToolId == null) return null;
			
		// link to review the jforum item
		if (gbToolId != null && id != 0)
		{
			reviewLink = "/portal/directtool/" +jforumToolId + "/gradeForum/" + gradeLink + "/" + id + "/" + userId + "/" + gbToolId + JForumService.SERVLET_EXTENSION;
			
			if (returnViewParam == null || returnViewParam.trim().length() == 0)
			{
				reviewLink = reviewLink + "?return_params=/indvidualStudentGrades/"+ userId;
			}
			else
			{
				reviewLink = reviewLink + "?return_params=/"+ returnViewParam +"/"+ userId; //assessmentDetails or indvidualStudentGrades
			}
		}

		return reviewLink;
	}
	
	/**
	 * gets the category access status if category has no dates based on category forum dates for the user as user may have special access
	 * 
	 * @param category
	 *        The category
	 * 
	 * @return The category access status
	 */
	protected GradebookItemAccessStatus getUserCategoryAccessStatus(Category category)
	{
		// - published_hidden - if now is before the open date for the item (or the earliest open date for any dates in this item's children if they have the dates)
		// - published_closed - if now is after the close date and we lock on close (consider the dates and lock on close of children if needed)
		// - published_closed_access - if now is after the close date, and we still let the student have access to the forum (consider the dates and lock on close of children if needed)

		GradebookItemAccessStatus accessStatus = GradebookItemAccessStatus.published;
		Date now = new Date();

		List<Forum> forums = category.getForums();
		int forumDenyAccess = 0;

		for (Forum forum : forums)
		{
			if (forum.getAccessType() != Forum.ACCESS_DENY)
			{
				if ((forum.getAccessDates() != null)
						&& ((forum.getAccessDates().getDueDate() != null) || (forum.getAccessDates().getOpenDate() != null)))
				{
					Date forumOpen = null;
					Boolean forumIsHideUntilOpen = Boolean.FALSE;
					Date forumDue = null;
					Date forumAllowUntilDate = null;
					// boolean forumLockOnDue = false;

					if (forum.getSpecialAccess() != null && !forum.getSpecialAccess().isEmpty())
					{
						// user special access
						if (forum.getSpecialAccess().size() == 1)
						{
							SpecialAccess specialAccess = forum.getSpecialAccess().get(0);

							forumOpen = specialAccess.getAccessDates().getOpenDate();
							if (forumOpen != null)
							{
								forumIsHideUntilOpen = specialAccess.getAccessDates().isHideUntilOpen();
							}
							forumDue = specialAccess.getAccessDates().getDueDate();
							forumAllowUntilDate = specialAccess.getAccessDates().getAllowUntilDate();
							/*
							 * if (forumDue != null) { if (specialAccess.getAccessDates().isLocked()) {
							 * 
							 * forumLockOnDue = true; } }
							 */
						}
					}
					else
					{
						forumOpen = forum.getAccessDates().getOpenDate();
						if (forumOpen != null)
						{
							forumIsHideUntilOpen = forum.getAccessDates().isHideUntilOpen();
						}
						forumDue = forum.getAccessDates().getDueDate();
						forumAllowUntilDate = forum.getAccessDates().getAllowUntilDate();

						/*
						 * if (forumDue != null) { if (forum.getAccessDates().isLocked()) { forumLockOnDue = true; } }
						 */
					}

					// if ((forumOpen != null) && (forumDue != null) && forumOpen.after(forumDue))
					if (((forumOpen != null) || (forumDue != null) || (forumAllowUntilDate != null)) && !forum.getAccessDates().isDatesValid())
					{
						continue;
					}

					if (forumOpen == null)
					{
						// no forum open date
						accessStatus = GradebookItemAccessStatus.published;

						/*
						 * if (forumDue != null) { if (now.after(forumDue)) { if (forumLockOnDue) { accessStatus = GradebookItemAccessStatus.published_closed; } else { accessStatus = GradebookItemAccessStatus.published_closed_access; } } }
						 */
						if (forumAllowUntilDate != null)
						{
							if (now.after(forumAllowUntilDate))
							{
								accessStatus = GradebookItemAccessStatus.published_closed;
							}
						}
						else if (forumDue != null)
						{
							if (now.after(forumDue))
							{
								accessStatus = GradebookItemAccessStatus.published_closed;
							}
						}
					}
					else
					{
						if (now.before(forumOpen) && forumIsHideUntilOpen.booleanValue())
						{
							if (accessStatus != GradebookItemAccessStatus.published_closed)
							{
								accessStatus = GradebookItemAccessStatus.published_hidden;
							}
						}
						else
						{
							accessStatus = GradebookItemAccessStatus.published;

							/*
							 * if (forumDue != null) { if (now.after(forumDue)) { if (forumLockOnDue) { accessStatus = GradebookItemAccessStatus.published_closed; } else { accessStatus = GradebookItemAccessStatus.published_closed_access; } } }
							 */
							if (forumAllowUntilDate != null)
							{
								if (now.after(forumAllowUntilDate))
								{
									accessStatus = GradebookItemAccessStatus.published_closed;
								}
							}
							else if (forumDue != null)
							{
								if (now.after(forumDue))
								{
									accessStatus = GradebookItemAccessStatus.published_closed;
								}
							}
						}
					}

					if ((accessStatus == GradebookItemAccessStatus.published) || (accessStatus == GradebookItemAccessStatus.published_closed_access))
					{
						break;
					}
				}
				else
				{
					// no forum dates
					accessStatus = GradebookItemAccessStatus.published;
					break;
				}
			}
			else
			{
				forumDenyAccess++;
			}
		}

		if (forums.size() == forumDenyAccess)
		{
			accessStatus = GradebookItemAccessStatus.published_hidden;
		}
		return accessStatus;
	}

}
