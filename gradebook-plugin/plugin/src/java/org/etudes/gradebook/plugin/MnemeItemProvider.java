/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-plugin/plugin/src/java/org/etudes/gradebook/plugin/MnemeItemProvider.java $
 * $Id: MnemeItemProvider.java 12628 2016-02-16 19:25:36Z murthyt $
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
import org.etudes.gradebook.api.GradebookItem;
import org.etudes.gradebook.api.GradebookItemAccessStatus;
import org.etudes.gradebook.api.GradebookItemType;
import org.etudes.gradebook.api.GradebookService;
import org.etudes.gradebook.api.Participant;
import org.etudes.gradebook.api.ParticipantGradebookItem;
import org.etudes.gradebook.api.ParticipantItemDetails;
import org.etudes.gradebook.api.UserItemSpecialAccess;
import org.etudes.mneme.api.Assessment;
import org.etudes.mneme.api.AssessmentAccess;
import org.etudes.mneme.api.AssessmentPermissionException;
import org.etudes.mneme.api.AssessmentService;
import org.etudes.mneme.api.AssessmentService.AssessmentsSort;
import org.etudes.mneme.api.AssessmentSpecialAccess;
import org.etudes.mneme.api.AssessmentType;
import org.etudes.mneme.api.MnemeService;
import org.etudes.mneme.api.SecurityService;
import org.etudes.mneme.api.Submission;
import org.etudes.mneme.api.SubmissionService;
import org.etudes.mneme.api.SubmissionService.FindAssessmentSubmissionsSort;
import org.etudes.mneme.api.SubmissionService.GetUserContextSubmissionsSort;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.user.cover.UserDirectoryService;

public class MnemeItemProvider extends PluginItemProvider //implements GradebookItemProvider
{
	/** Our log. */
	private static Log logger = LogFactory.getLog(JForumItemProvider.class);

	/** Dependency: AssessmentService. */
	protected AssessmentService assessmentService = null;
	
	/** Dependency: GradebookService. */
	protected GradebookService gradebookService = null;
	
	/** Dependency: mneme SecurityService */
	protected SecurityService mnemeSecurityService = null;
	
	/** Dependency: SiteService. */
	protected SiteService siteService = null;
	
	/** Dependency: SubmissionService. */
	protected SubmissionService submissionService = null;
	
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
	public List<GradebookItem> getGradableItems(String context, String fetchedByUserId, Set<String> activeParticipantIds, boolean includeScores, boolean includeUnpublish, GradebookItemType itemType)
	{
		if (context == null || context.trim().length() == 0)
		{
			throw new IllegalArgumentException("context is missing.");
		}
		
		//TODO check user access
		
		if (!(itemType == null || itemType == GradebookItemType.assignment || itemType == GradebookItemType.test || itemType == GradebookItemType.survey || itemType == GradebookItemType.offline))
		{
			return null;
		}
		
		List<GradebookItem> mnemeGradableItems = new ArrayList<GradebookItem>();
		
		String toolId = null;
		try
		{
			Site site = this.siteService.getSite(context);
			ToolConfiguration config = site.getToolForCommonId("sakai.mneme");
			if (config != null) toolId = config.getId();
		}
		catch (IdUnusedException e)
		{
			logger.warn("getGradableItems: missing site: " + context);
		}
		
		// no tool id? No Mneme in site!
		if (toolId == null) return mnemeGradableItems;
		
		
		List<Assessment> assessments = new ArrayList<Assessment>();
		if (includeUnpublish) 
		{
			assessments = this.assessmentService.getContextAssessments(context, AssessmentsSort.odate_a, Boolean.FALSE);
		}
		else 
		{
			assessments = this.assessmentService.getContextAssessments(context, AssessmentsSort.odate_a, Boolean.TRUE);
		}
		
		GradebookItemType type = null;
		Float points = null;
		GradebookItem item = null;
		
		for (Assessment assessment : assessments)
		{
			// ignore not published, invalid dates assessments and surveys
			if (!assessment.getDates().getIsValid() || assessment.getType() == AssessmentType.survey || !assessment.getGradebookIntegration())
			{
				continue;
			}
			
			// reset to initial valuse
			type = null;
			points = null;
			item = null;
			
			// figure the type
			type = getType(assessment);
			
			// filter by item type
			if (itemType != null && itemType != type)
			{
				continue;
			}
			
			// points			
			if (type == GradebookItemType.offline)
			{
				points = assessment.getPoints();
			}
			else
			{
				points = assessment.getParts().getTotalPoints();
			}
			
			// set status - invalid trumps unpublished
			GradebookItemAccessStatus accessStatus = getAccessStatus(assessment);
			
			if (accessStatus == GradebookItemAccessStatus.invalid)
			{
				continue;
			}
			
			if (includeScores)
			{
				//  in the averages include zero assigned by gradebook to the non-submitters after closing date
				Map<String, Float> assementScores = null;
				assementScores = getAssessmentScoresAndAssignZeroToNonSubmitters(assessment, activeParticipantIds);
				
				Float averagePercent = null;
				
				// get the scores to show average score
				// Map<String, Float> assementScores = this.submissionService.getAssessmentHighestScores(assessment, Boolean.TRUE);
				Map<String, Float> submittedScores =  new HashMap<String, Float>();
				if (assementScores != null && assementScores.size() > 0)
				{
					if (activeParticipantIds != null && activeParticipantIds.size() > 0)
					{
						Float totalScores = 0.0f;
						
						for (Map.Entry<String, Float> entry : assementScores.entrySet()) 
						{
							if (!activeParticipantIds.contains(entry.getKey()))
							{
								continue;
							}
							
							if (entry.getValue() != null)
							{
								submittedScores.put(entry.getKey(), entry.getValue());
								
								totalScores += entry.getValue();
							}
						}
						
						if ((submittedScores.size() > 0) && (points != null && points > 0))
						{
							averagePercent = ((totalScores/new Float(submittedScores.size())) * 100) / (points);
						}
					}
				}
				
				Integer submittedCount = null;
				
				if (submittedScores.size() > 0)
				{
					submittedCount = submittedScores.size();
				}
				
				if (averagePercent != null && averagePercent > 0)
				{
					averagePercent = Math.round(averagePercent * 100.0f) / 100.0f;
				}
				
				item = this.gradebookService.newGradebookItem("MNEME-"+assessment.getId(), assessment.getTitle(), points, averagePercent, assessment.getDates().getDueDate(), assessment.getDates().getOpenDate(), assessment.getDates().getAcceptUntilDate(), type, accessStatus, submittedCount);
				
				Map<String, Float> scores = item.getScores();
				scores.clear();
				scores.putAll(submittedScores);
			}
			else
			{
				item = this.gradebookService.newGradebookItem("MNEME-"+assessment.getId(), assessment.getTitle(), points, null, assessment.getDates().getDueDate(), assessment.getDates().getOpenDate(), assessment.getDates().getAcceptUntilDate(), type, accessStatus, null);
			}
			
			if (item != null)
			{
				mnemeGradableItems.add(item);
			}
		}
				
		return mnemeGradableItems;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public GradebookItem getMnemeGradableItem(String context, String itemId, String fetchedByUserId, Set<String> activeParticipantIds, boolean includeScores)
	{
		if ((itemId == null || itemId.trim().length() == 0) || (context == null || context.trim().length() == 0))
		{
			throw new IllegalArgumentException("item information or context is missing.");
		}
		
		//TODO check user access
		
		GradebookItem gradebookItem = null;
		
		String toolId = null;
		
		try
		{
			Site site = this.siteService.getSite(context);
			ToolConfiguration config = site.getToolForCommonId("sakai.mneme");
			if (config != null) toolId = config.getId();
		}
		catch (IdUnusedException e)
		{
			logger.warn("getGradableItems: missing site: " + context);
		}
		
		String id;
		id = itemId.substring(itemId.indexOf("-") + 1);
		
		Assessment assessment = this.assessmentService.getAssessment(id);
		
		if (assessment == null)
		{
			return null;
		}
		
		String title = null;
		Float points = null;
		Date open = null;
		Date due = null;
		Date close = null;
		GradebookItemType type = null;
		
		type = getType(assessment);
		
		title = assessment.getTitle();

		// points			
		if (type == GradebookItemType.offline)
		{
			points = assessment.getPoints();
		}
		else
		{
			points = assessment.getParts().getTotalPoints();
		}
		open = assessment.getDates().getOpenDate();
		due = assessment.getDates().getDueDate();
		close = assessment.getDates().getAcceptUntilDate();
		
		GradebookItemAccessStatus accessStatus = getAccessStatus(assessment);
		
		Float averagePercent = null;
		Map<String, Float> scoresMap = null;
		
		// get the scores
		if (includeScores)
		{
			Map<String, Float> assementScores = this.submissionService.getAssessmentHighestScores(assessment, Boolean.TRUE);			
			
			scoresMap = new HashMap<String, Float>();
			
			if (activeParticipantIds != null && activeParticipantIds.size() > 0)
			{
				for (String participantId : activeParticipantIds)
				{
					scoresMap.put(participantId, null);
				}
			}
			
			if (assementScores != null && assementScores.size() > 0)
			{
				if (activeParticipantIds != null && activeParticipantIds.size() > 0)
				{
					Float totalScores = 0.0f;
					
					for (Map.Entry<String, Float> entry : assementScores.entrySet()) 
					{
						if (!activeParticipantIds.contains(entry.getKey()))
						{
							continue;
						}
						
						if (entry.getValue() != null)
						{
							scoresMap.put(entry.getKey(), entry.getValue());
							
							totalScores += entry.getValue();
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
		gradebookItem = this.gradebookService.newGradebookItem(itemId, title, points, averagePercent, due, open, close, type, accessStatus, null);
		
		if (includeScores)
		{
			Map<String, Float> scores = gradebookItem.getScores();
			scores.clear();
			scores.putAll(scoresMap);
		}
		
		return gradebookItem;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<ParticipantItemDetails> getMnemeItemDetails(String context, String itemId, List<Participant> participants, boolean allSubmissions)
	{
		if ((context == null || context.trim().length() == 0) || (itemId == null || itemId.trim().length() == 0))
		{
			return new ArrayList<ParticipantItemDetails>();
		}
		
		if (!allSubmissions)
		{
			return getMnemeItemDetails(context, itemId, participants);
		}
		
		String id;
		id = itemId.substring(itemId.indexOf("-") + 1);
		
		// get Assessment
		Assessment assessment = this.assessmentService.getAssessment(id);
		if (assessment == null)
		{
			throw new IllegalArgumentException();
		}
				
		// get all mneme submission of the users
		Map<String, ArrayList<Submission>> usersSubmissionsMap = new HashMap<String, ArrayList<Submission>>();
		
		// get all submissions
		List<Submission> submissions = this.submissionService.findAssessmentSubmissions(assessment, FindAssessmentSubmissionsSort.userName_a, Boolean.FALSE, null, null, null, Boolean.FALSE);

		for (Submission s : submissions)
		{
			// if (s.getIsPhantom() || !s.getIsReleased()) continue;			
			if (usersSubmissionsMap.containsKey(s.getUserId()))
			{
				usersSubmissionsMap.get(s.getUserId()).add(s);
			}
			else
			{
				ArrayList<Submission> userSubmissions = new ArrayList<Submission>();
				userSubmissions.add(s);				
				usersSubmissionsMap.put(s.getUserId(), userSubmissions);
			}
		}
		
		List<ParticipantItemDetails> rv = new ArrayList<ParticipantItemDetails>();
		
		for (Participant p : participants)
		{
			// find this user's submissions
			boolean found = false;
			
			if (usersSubmissionsMap.containsKey(p.getUserId()))
			{
				ArrayList<Submission> userSubmissions = usersSubmissionsMap.get(p.getUserId());
				
				if (userSubmissions != null && !userSubmissions.isEmpty())
				{
					// create ParticipantItemDetails for each submission					
					for (Submission s : userSubmissions)
					{
						Date started = null;
						Date finished = null;
						Date reviewed = null;
						Date evaluated = null;
						Boolean inProgress = Boolean.FALSE;
						Float score = null;
						Boolean released = Boolean.FALSE;
						Boolean isLate = Boolean.FALSE;
						Boolean isAutoSubmitted = Boolean.FALSE;
						
						if (s.getIsStarted() && !s.getIsNonSubmit() && (s.getAssessment().getType() != AssessmentType.offline))
						{
							started = s.getStartDate();
						}
						
						if (s.getIsComplete() && !s.getIsNonSubmit() && (s.getAssessment().getType() != AssessmentType.offline))
						{
							finished = s.getSubmittedDate();
						}
						
						reviewed = s.getReviewedDate();
						evaluated = s.getEvaluatedDate();
						score = s.getTotalScore();
						released = s.getIsReleased();
						inProgress = !s.getIsComplete() && !s.getIsPhantom();
						
						if (finished != null)
						{
							isLate = s.getIsCompletedLate();
						}
						isAutoSubmitted = s.getIsAutoCompleted();
						
						//review Link
						String reviewLink = null;
						String gradingLink = null;
						if (s.getIsReleased())
						{
							if (mnemeSecurityService.checkSecurity(UserDirectoryService.getCurrentUser().getId(), MnemeService.GRADE_PERMISSION, context))
							{
								gradingLink = getBestSubmissionReviewLink(context, assessment, s, "assessmentDetails", p.getUserId(), true, true);
								reviewLink = getBestSubmissionReviewLink(context, assessment, s, "assessmentDetails", p.getUserId(), false, true);
							}
							else
							{
								reviewLink = getBestSubmissionReviewLink(context, assessment, s, "assessmentDetails", p.getUserId(), false, false);
							}						
						}
								
						// ParticipantItemDetails pmid = this.gradebookService.newParticipantItemDetails(p.getUserId(), p.getDisplayId(), p.getGroupTitle(), p.getSortName(), p.getStatus(), evaluated, finished, reviewed, score, started, reviewLink, inProgress, (s.getCompletionStatus() == SubmissionCompletionStatus.evaluationNonSubmit), s.getEvaluationNotReviewed(), released);
						ParticipantItemDetails pmid = null;
						
						// not released submissions doesn't get best submission
						Submission bestSubmission = s.getBest();
						
						if (bestSubmission != null)
						{
							if (bestSubmission.getId().equalsIgnoreCase(s.getId()))
							{
								pmid = this.gradebookService.newParticipantItemDetails(p.getUserId(), p.getDisplayId(), p.getGroupTitle(), p.getSortName(), p.getStatus(), null, evaluated, reviewed, score, started, finished, s.getEvaluationNotReviewed(), released, reviewLink, gradingLink, inProgress, s.getId(), Boolean.TRUE, isLate, isAutoSubmitted);
							}
							else
							{
								pmid = this.gradebookService.newParticipantItemDetails(p.getUserId(), p.getDisplayId(), p.getGroupTitle(), p.getSortName(), p.getStatus(), null, evaluated, reviewed, score, started, finished, s.getEvaluationNotReviewed(), released, reviewLink, gradingLink, inProgress, s.getId(), isLate, isAutoSubmitted);
							}
						}
						else
						{
							pmid = this.gradebookService.newParticipantItemDetails(p.getUserId(), p.getDisplayId(), p.getGroupTitle(), p.getSortName(), p.getStatus(), null, evaluated, reviewed, score, started, finished, s.getEvaluationNotReviewed(), released, reviewLink, gradingLink, inProgress, s.getId(), isLate, isAutoSubmitted);
						}
						
						rv.add(pmid);
						found = true;
					}
					
				}				
			}
			
			// if none found, make one
			if (!found)
			{
				// ParticipantItemDetails pmid = this.gradebookService.newParticipantItemDetails(p.getUserId(), p.getDisplayId(), p.getGroupTitle(), p.getSortName(), p.getStatus(), null, null, null, null, null, null, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null);
				ParticipantItemDetails pmid = this.gradebookService.newParticipantItemDetails(p.getUserId(), p.getDisplayId(), p.getGroupTitle(), p.getSortName(), p.getStatus(), null, null, null, null, null, null, null, null, null, null, null, null, null, null);
				rv.add(pmid);
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
		
		if (!(itemType == null || itemType == GradebookItemType.assignment || itemType == GradebookItemType.test || itemType == GradebookItemType.survey || itemType == GradebookItemType.offline))
		{
			return null;
		}
		
		Map<String, List<ParticipantGradebookItem>> userGradableItems = new HashMap<String, List<ParticipantGradebookItem>>();
		
		//TODO check user access
		
		//List<GradebookItem> mnemeGradableItems = new ArrayList<GradebookItem>();
		
		String toolId = null;
		try
		{
			Site site = this.siteService.getSite(context);
			ToolConfiguration config = site.getToolForCommonId("sakai.mneme");
			if (config != null) toolId = config.getId();
		}
		catch (IdUnusedException e)
		{
			logger.warn("getGradableItems: missing site: " + context);
		}
		
		// no tool id? No Mneme in site!
		if (toolId == null) return null;
		
		for (String particpantId : participantIds)
		{
			userGradableItems.put(particpantId, new ArrayList<ParticipantGradebookItem>());
		}
		
		List<Assessment> assessments = this.assessmentService.getContextAssessments(context, AssessmentsSort.odate_a, Boolean.TRUE);
		
		if (assessments.size() == 0)
		{
			return null;
		}
		
		/*
		// get all the submissions - for ALL possible users (no filtering by existence or permissions)
		List<? extends Submission> submissions = this.submissionService.getContextSubmissions(context);
		Map<String, Map<String, Submission>> submissionMap = new HashMap<String, Map<String, Submission>>();
		
		for (Submission submission : submissions)
		{
			if (submission.getIsComplete())
			{
				Map<String, Submission> userSubmissionsMap = submissionMap.get(submission.getUserId());
				
				if (userSubmissionsMap == null)
				{
					userSubmissionsMap = new HashMap<String, Submission>();
					submissionMap.put(submission.getUserId(), userSubmissionsMap);
				}
				
				userSubmissionsMap.put(submission.getAssessment().getId(), submission);
			}
		}
		*/
		
		GradebookItemType type = null;
		Float points = null;
		//Submission userSubmission = null;
		Float score = null;
		
		for (Assessment assessment : assessments)
		{
			// ignore not published, invalid dates assessments and surveys
			if (!assessment.getPublished() || !assessment.getDates().getIsValid() || assessment.getType() == AssessmentType.survey || !assessment.getGradebookIntegration())
			{
				continue;
			}
						
			// reset to initial valuse
			type = null;
			points = null;
			//userSubmission = null;
			score = null;
			
			// figure the type
			type = getType(assessment);
			
			// filter by item type
			if (itemType != null && itemType != type)
			{
				continue;
			}
			
			// points			
			if (type == GradebookItemType.offline)
			{
				points = assessment.getPoints();
			}
			else
			{
				points = assessment.getParts().getTotalPoints();
			}
			
			/*
			// for tests and assignments with all essay type questions should also be evaluated and released
			boolean allEssayTypeQuestions = true;
			if ((assessment.getType() == AssessmentType.assignment) || (assessment.getType() == AssessmentType.test))
			{
				List<Question> questions = assessment.getParts().getQuestions();
				
				if (questions.size() > 0)
				{
					for (Question question : questions)
					{
						if (!question.getType().equalsIgnoreCase("mneme:Essay"))
						{
							allEssayTypeQuestions = false;
							break;
						}
					}
				}
				else
				{
					allEssayTypeQuestions = false;
				}
			}
			*/
			
			Map<String, Float> assementScores = this.submissionService.getAssessmentHighestScores(assessment, Boolean.TRUE);
			Map<String, Float> assementScoresMap = new HashMap<String, Float>();
			
			Map<String, ArrayList<Submission>> usersSubmissionsMap = getUsersAllAssessmentSubmissions(assessment, false);
			
			if (assementScores != null && assementScores.size() > 0)
			{
				if (participantIds != null && participantIds.size() > 0)
				{
					for (Map.Entry<String, Float> entry : assementScores.entrySet()) 
					{
						if (!participantIds.contains(entry.getKey()))
						{
							continue;
						}
						
						if (entry.getValue() != null)
						{
							assementScoresMap.put(entry.getKey(), entry.getValue());
						}
					}
				}
			}
			
			// set status - invalid trumps unpublished
			GradebookItemAccessStatus accessStatus = getAccessStatus(assessment);
			
			for (String particpantId : participantIds)
			{
				score = null;
				// get particpant submission
				ParticipantItemDetails pItem = null;
				
				/*
				Map<String, Submission> userSubmissionsMap = submissionMap.get(particpantId);
				
				if (userSubmissionsMap != null)
				{
					userSubmission = userSubmissionsMap.get(assessment.getId());
					
					if (userSubmission != null)
					{
						if (allScores || userSubmission.getIsReleased())
						{
							score = userSubmission.getTotalScore();
						}
						pItem = this.gradebookService.newParticipantItemDetails(userSubmission.getStartDate(), userSubmission.getSubmittedDate(), score, userSubmission.getEvaluatedDate(), userSubmission.getReviewedDate(), userSubmission.getEvaluationNotReviewed(), userSubmission.getSiblingCount());
					}
				}
				*/
				
				/*
				// for tests and assignments with all essay type questions should also be evaluated and released
				if (allEssayTypeQuestions)
				{
					// check for user submissions
					List<Submission> userSubmissions = usersSubmissionsMap.get(particpantId);
					
					if (userSubmissions != null && !userSubmissions.isEmpty())
					{
						// only released submissions gets the best submission
						for (Submission userSubmission : userSubmissions)
						{
							if (!userSubmission.getIsPhantom())
							{
								if (userSubmission.getIsReleased())
								{
									Submission userBestSubmission = userSubmission.getBest();
									
									if (userBestSubmission.getEvaluation() != null && userBestSubmission.getEvaluation().getEvaluated())
									{
										score = assementScoresMap.get(particpantId);
										break;
									}
								}
							}
						}
					}
				}
				else
				{
					score = assementScoresMap.get(particpantId);
				}
				*/
				score = assementScoresMap.get(particpantId);
				
				Integer siblingCount = 0;
				
				// check for no submissions
				if (score == null)
				{
					if (usersSubmissionsMap != null && usersSubmissionsMap.size() > 0)
					{
						Boolean inProgress = Boolean.FALSE;
						siblingCount = 0;
						
						// check for user submissions
						List<Submission> userSubmissions = usersSubmissionsMap.get(particpantId);
						
						if (userSubmissions != null && userSubmissions.size() > 0)
						{
							for (Submission userSubmission : userSubmissions)
							{
								inProgress = !userSubmission.getIsComplete() && !userSubmission.getIsPhantom();
								
								if (inProgress)
								{
									siblingCount = null;
									break;
								}
								
								if (!userSubmission.getIsPhantom())
								{
									/* this submission may be completed but sibling count fetched is zero. To fix the sibling count added below condition. 
									Not released submissions may be not fetching sibling count*/
									if (userSubmission.getSiblingCount() > 0)
									{
										siblingCount = userSubmission.getSiblingCount();
									}
									else
									{
										siblingCount = 1;
									}
									break;
								}
							}
						}
					}
				}
				
				// only best score is needed
				pItem = this.gradebookService.newParticipantItemDetails(particpantId, null, null, null, null, siblingCount, null, null, score, null, null, null, null, null, null, null, null, null, null);
				
				// add user special access
				pItem.setUserItemSpecialAccess(getUserSpecialAccess(assessment, particpantId));
				
				// make the item
				GradebookItem gItem = this.gradebookService.newGradebookItem("MNEME-"+assessment.getId(), assessment.getTitle(), points, null,
											assessment.getDates().getDueDate(), assessment.getDates().getOpenDate(), assessment.getDates().getAcceptUntilDate(), type, accessStatus, null);
								
				ParticipantGradebookItem uItem = this.gradebookService.newParticipantGradebookItem("MNEME-"+assessment.getId(), gItem, pItem);
				
				userGradableItems.get(particpantId).add(uItem);				
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
		
		if (!(itemType == null || itemType == GradebookItemType.assignment || itemType == GradebookItemType.test || itemType == GradebookItemType.survey || itemType == GradebookItemType.offline))
		{
			return null;
		}
		
		List<Assessment> assessments = this.assessmentService.getContextAssessments(context, AssessmentsSort.odate_a, Boolean.TRUE);
		
		List<ParticipantGradebookItem> mnemeGradableItems = new ArrayList<ParticipantGradebookItem>();
		if (assessments.size() == 0)
		{
			return mnemeGradableItems;
		}
		
		if (allScores)
		{
			GradebookItemType type = null;
			Float points = null;
			
			// get all user submissions for each assessment
			for (Assessment contextAssessment : assessments)
			{
				// ignore not published, invalid dates assessments and surveys
				if (!contextAssessment.getPublished() || !contextAssessment.getDates().getIsValid() || contextAssessment.getType() == AssessmentType.survey || !contextAssessment.getGradebookIntegration())
				{
					continue;
				}
				
				// figure the type
				type = getType(contextAssessment);
				
				// filter by item type
				if (itemType != null && itemType != type)
				{
					continue;
				}
				
				// points			
				if (type == GradebookItemType.offline)
				{
					points = contextAssessment.getPoints();
				}
				else
				{
					points = contextAssessment.getParts().getTotalPoints();
				}
				
				GradebookItemAccessStatus accessStatus = getAccessStatus(contextAssessment);
				
				// make the item
				GradebookItem gItem = null;
				// get all user submissions for the assessment
				
				// create ParticipantItemDetails for each user submission
				Map<String, ArrayList<Submission>> usersSubmissionsMap = getUsersAllAssessmentSubmissions(contextAssessment, false);
				
				// there will at least one submission (may be phantom if there is none)
				if (usersSubmissionsMap != null && usersSubmissionsMap.size() > 0)
				{
					List<Submission> userSubmissions = usersSubmissionsMap.get(userId);
					
					if (userSubmissions != null)
					{
						for (Submission userSubmission : userSubmissions)
						{
							Date started = null;
							Date finished = null;
							Date reviewed = null;
							Date evaluated = null;
							Boolean inProgress = Boolean.FALSE;
							Float score = null;
							Boolean released = Boolean.FALSE;
							Integer siblingCount = 0;
							Boolean isLate = Boolean.FALSE;
							Boolean isAutoSubmitted = Boolean.FALSE;
							
							if (userSubmission.getIsStarted() && !userSubmission.getIsNonSubmit() && (userSubmission.getAssessment().getType() != AssessmentType.offline))
							{
								started = userSubmission.getStartDate();
							}
							
							if (userSubmission.getIsComplete() && !userSubmission.getIsNonSubmit() && (userSubmission.getAssessment().getType() != AssessmentType.offline))
							{
								finished = userSubmission.getSubmittedDate();
							}
							
							reviewed = userSubmission.getReviewedDate();
							evaluated = userSubmission.getEvaluatedDate();
							score = userSubmission.getTotalScore();
							released = userSubmission.getIsReleased();
							inProgress = !userSubmission.getIsComplete() && !userSubmission.getIsPhantom();
							
							if (finished != null)
							{
								isLate = userSubmission.getIsCompletedLate();
							}
							
							isAutoSubmitted = userSubmission.getIsAutoCompleted();
							
							//review Link
							String reviewLink = null;
							String gradingLink = null;
							
							if (!userSubmission.getIsPhantom())
							{
								if (mnemeSecurityService.checkSecurity(UserDirectoryService.getCurrentUser().getId(), MnemeService.GRADE_PERMISSION, context))
								{
									gradingLink = getBestSubmissionReviewLink(context, contextAssessment, userSubmission, "indvidualStudentGrades", userId, true, true);
									reviewLink = getBestSubmissionReviewLink(context, contextAssessment, userSubmission, "indvidualStudentGrades", userId, false, true);
								}
								else
								{
									reviewLink = getBestSubmissionReviewLink(context, contextAssessment, userSubmission, "indvidualStudentGrades", userId, false, false);
								}
							}
							
							gItem = this.gradebookService.newGradebookItem("MNEME-"+contextAssessment.getId(), contextAssessment.getTitle(), points, null,
									contextAssessment.getDates().getDueDate(), contextAssessment.getDates().getOpenDate(), contextAssessment.getDates().getAcceptUntilDate(), type, accessStatus, null);
							
							// set released, in-progress, best....
							ParticipantItemDetails pItem = null;
							Submission bestSubmission = userSubmission.getBest();
							if (bestSubmission != null)
							{
								siblingCount = userSubmission.getSiblingCount();
								if (bestSubmission.getId().equalsIgnoreCase(userSubmission.getId()))
								{
									pItem = this.gradebookService.newParticipantItemDetails(userId, null, null, null, null, siblingCount, evaluated, reviewed, score, null, finished, userSubmission.getEvaluationNotReviewed(), released, reviewLink, gradingLink, inProgress, userSubmission.getId(), Boolean.TRUE, isLate, isAutoSubmitted);
								}
								else
								{
									pItem = this.gradebookService.newParticipantItemDetails(userId, null, null, null, null, siblingCount, evaluated, reviewed, score, null, finished, userSubmission.getEvaluationNotReviewed(), released, reviewLink, gradingLink, inProgress, userSubmission.getId(), isLate, isAutoSubmitted);
								}
							}
							else
							{
								pItem = this.gradebookService.newParticipantItemDetails(userId, null, null, null, null, siblingCount, evaluated, reviewed, score, null, finished, userSubmission.getEvaluationNotReviewed(), released, reviewLink, gradingLink, inProgress, userSubmission.getId(), isLate, isAutoSubmitted);
							}
							ParticipantGradebookItem uItem = this.gradebookService.newParticipantGradebookItem("MNEME-"+contextAssessment.getId(), gItem, pItem);
							
							mnemeGradableItems.add(uItem);
						}
					}
					else
					{
						gItem = this.gradebookService.newGradebookItem("MNEME-"+contextAssessment.getId(), contextAssessment.getTitle(), points, null,
								contextAssessment.getDates().getDueDate(), contextAssessment.getDates().getOpenDate(), contextAssessment.getDates().getAcceptUntilDate(), type, accessStatus, null);
						
						ParticipantItemDetails pItem = this.gradebookService.newParticipantItemDetails(userId, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
						 
						ParticipantGradebookItem uItem = this.gradebookService.newParticipantGradebookItem("MNEME-"+contextAssessment.getId(), gItem, pItem);
						
						mnemeGradableItems.add(uItem);
					}
				}
			}
		}
		else
		{
			//List<Submission> submissions = this.submissionService.getUserContextSubmissions(context, userId, GetUserContextSubmissionsSort.dueDate_d);
			List<Submission> submissions = this.submissionService.getUnfilteredUserContextSubmissions(context, userId, GetUserContextSubmissionsSort.dueDate_d);
			
			Assessment assessment = null;
			GradebookItemType type = null;
			Float points = null;
			Date startDate = null;
			Date finished = null;
			Date submittedDate = null;
			Date reviewedDate = null;
			Date evaluatedDate = null;
			Boolean inProgress = Boolean.FALSE;
			Boolean isLate = Boolean.FALSE;
			Boolean isAutoSubmitted = Boolean.FALSE;
						
			Boolean evaluationNotReviewed = Boolean.FALSE;
			
			Submission userSubmission = null;
			String reviewLink = null;
			String gradingLink = null;
			
			String submissionId = null;
			// Date now = new Date();
			
			Map<String, ParticipantItemDetails> userSubmissionParticipantItemDetails = new HashMap<String, ParticipantItemDetails>();
			
			for (Submission submission : submissions)
			{	
				assessment = null;
				
				assessment = submission.getAssessment();
				
				// reset to initial values
				type = null;
				points = null;
				startDate = null;
				finished = null;
				submittedDate = null;
				reviewedDate = null;
				evaluatedDate = null;
				inProgress = Boolean.FALSE;
				evaluationNotReviewed = Boolean.FALSE;
				userSubmission = null;
				reviewLink = null;
				gradingLink = null;
				submissionId = null;
				Integer siblingCount = 0;
				isLate = Boolean.FALSE;
				isAutoSubmitted = Boolean.FALSE;
				
				// figure the type
				type = getType(assessment);
				
				// filter by item type
				if (itemType != null && itemType != type)
				{
					continue;
				}
				
				// points			
				if (type == GradebookItemType.offline)
				{
					points = assessment.getPoints();
				}
				else
				{
					points = assessment.getParts().getTotalPoints();
				}
				
				// ignore not published, invalid dates assessments and surveys
				if (!assessment.getPublished() || !assessment.getDates().getIsValid() || assessment.getType() == AssessmentType.survey || !assessment.getGradebookIntegration())
				{
					continue;
				}
				
				Float score = null;
				
				GradebookItemAccessStatus accessStatus = getAccessStatus(submission.getAssessment());
				
				/*
				if (submission.getBest() != null && submission.getBest().getIsReleased())
				{
					userSubmission = submission.getBest();
				}
				else if (submission.getIsReleased())
				{
					userSubmission = submission;
				}
				*/
				if (submission.getBest() != null)
				{
					userSubmission = submission.getBest();
				}
				else
				{
					userSubmission = submission;
				}
				
				if (userSubmission != null)
				{
					
					if (userSubmission.getIsStarted() && !userSubmission.getIsNonSubmit() && (type != GradebookItemType.offline))
					{
						startDate = userSubmission.getStartDate();
					}
					
					if (userSubmission.getIsComplete() && !userSubmission.getIsNonSubmit() && (type != GradebookItemType.offline))
					{
						submittedDate = userSubmission.getSubmittedDate();
					}
					
					inProgress = !userSubmission.getIsComplete() && !userSubmission.getIsPhantom();
					
					if (userSubmission.getIsReleased() && userSubmission.getTotalScore() != null)
					{
						score = userSubmission.getTotalScore();
						/*
						// for tests and assignments with all essay type questions should also be evaluated and released
						if ((type == GradebookItemType.assignment) || (type == GradebookItemType.test))
						{
							List<Question> questions = userSubmission.getAssessment().getParts().getQuestions();
							
							if (userSubmission.getAssessment().getParts().getQuestions().size() > 0)
							{
								// all the questions must be essay type
								boolean allEssayTypeQuestions = true;
								
								for (Question question : questions)
								{
									if (!question.getType().equalsIgnoreCase("mneme:Essay"))
									{
										allEssayTypeQuestions = false;
										break;
									}
								}
								
								if (allEssayTypeQuestions)
								{
									if (userSubmission.getEvaluation() != null && userSubmission.getEvaluation().getEvaluated())
									{
										score = userSubmission.getTotalScore();
									}
								}
								else
								{
									score = userSubmission.getTotalScore();
								}
							}
							else
							{
								score = userSubmission.getTotalScore();
							}
						}
						else
						{
							score = userSubmission.getTotalScore();
						}
						*/
					}
					
					// show review date if it is after evaluated date
					/*if ((userSubmission.getReviewedDate() != null) && (userSubmission.getEvaluatedDate() != null) && (userSubmission.getReviewedDate().after(userSubmission.getEvaluatedDate())))
					{
						reviewedDate = userSubmission.getReviewedDate();
						evaluatedDate = userSubmission.getEvaluatedDate();
						// evaluationNotReviewed = Boolean.TRUE;
					}*/
					reviewedDate = userSubmission.getReviewedDate();
					evaluatedDate = userSubmission.getEvaluatedDate();
					
					if (userSubmission.getEvaluationNotReviewed())
					{
						evaluationNotReviewed = Boolean.TRUE;
					}
					
					//review Link
					if (!userSubmission.getIsPhantom())
					{
						if (mnemeSecurityService.checkSecurity(UserDirectoryService.getCurrentUser().getId(), MnemeService.GRADE_PERMISSION, context))
						{
							gradingLink = getBestSubmissionReviewLink(context, assessment, userSubmission, "indvidualStudentGrades", userId, true, true);
							reviewLink = getBestSubmissionReviewLink(context, assessment, userSubmission, "indvidualStudentGrades", userId, false, true);
						}
						else
						{
							reviewLink = getBestSubmissionReviewLink(context, assessment, userSubmission, "indvidualStudentGrades", userId, false, false);
						}
					}
					
					submissionId = userSubmission.getId();
					siblingCount = userSubmission.getSiblingCount();
					
					if (userSubmission.getIsComplete() && !userSubmission.getIsNonSubmit() && (userSubmission.getAssessment().getType() != AssessmentType.offline))
					{
						finished = userSubmission.getSubmittedDate();
					}
					
					if (finished != null)
					{
						isLate = userSubmission.getIsCompletedLate();
					}
					
					isAutoSubmitted = userSubmission.getIsAutoCompleted();
				}			
				
				// make the item
				//GradebookItem gItem = this.gradebookService.newGradebookItem("MNEME-"+assessment.getId(), assessment.getTitle(), points, null,
				//							assessment.getDates().getDueDate(), assessment.getDates().getOpenDate(), assessment.getDates().getAcceptUntilDate(), type, accessStatus, null);
				
				// ParticipantItemDetails pItem = this.gradebookService.newParticipantItemDetails(startDate, submittedDate, score, evaluatedDate, reviewedDate, evaluationNotReviewed, reviewLink, count, inProgress);
				ParticipantItemDetails pItem = this.gradebookService.newParticipantItemDetails(userId, null, null, null, null, siblingCount, evaluatedDate, reviewedDate, score, startDate, submittedDate , evaluationNotReviewed, null, reviewLink, gradingLink, inProgress, submissionId, isLate, isAutoSubmitted);
				userSubmissionParticipantItemDetails.put(assessment.getId(), pItem);
				// ParticipantGradebookItem uItem = this.gradebookService.newParticipantGradebookItem("MNEME-"+assessment.getId(), gItem, pItem);
				
				//mnemeGradableItems.add(uItem);
			}
					
			
			for (Assessment contextAssessment : assessments)
			{
				// ignore not published, invalid dates assessments and surveys
				if (!contextAssessment.getPublished() || !contextAssessment.getDates().getIsValid() || contextAssessment.getType() == AssessmentType.survey || !contextAssessment.getGradebookIntegration())
				{
					continue;
				}
				
				// figure the type
				type = getType(contextAssessment);
				
				// filter by item type
				if (itemType != null && itemType != type)
				{
					continue;
				}
				
				// points			
				if (type == GradebookItemType.offline)
				{
					points = contextAssessment.getPoints();
				}
				else
				{
					points = contextAssessment.getParts().getTotalPoints();
				}
				
				GradebookItemAccessStatus accessStatus = getAccessStatus(contextAssessment);
				
				// make the item
				GradebookItem gItem = this.gradebookService.newGradebookItem("MNEME-"+contextAssessment.getId(), contextAssessment.getTitle(), points, null,
											contextAssessment.getDates().getDueDate(), contextAssessment.getDates().getOpenDate(), contextAssessment.getDates().getAcceptUntilDate(), type, accessStatus, null);
				
				//ParticipantItemDetails pItem = this.gradebookService.newParticipantItemDetails(startDate, submittedDate, score, evaluatedDate, reviewedDate, evaluationNotReviewed, count, inProgress);
				ParticipantItemDetails pItem = userSubmissionParticipantItemDetails.get(contextAssessment.getId());
				
				if (pItem == null)
				{
					// pItem = this.gradebookService.newParticipantItemDetails(null, null, null, null, null, false, null, null, false);
					pItem = this.gradebookService.newParticipantItemDetails(userId, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
				}
				
				// add user special access
				pItem.setUserItemSpecialAccess(getUserSpecialAccess(contextAssessment, userId));
				
				ParticipantGradebookItem uItem = this.gradebookService.newParticipantGradebookItem("MNEME-"+contextAssessment.getId(), gItem, pItem);
				
				mnemeGradableItems.add(uItem);
			}
		}
		
		return mnemeGradableItems;
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
	public Boolean modifyMnemeScores(String context, String itemId, List<ParticipantItemDetails> participantItemDetails, String modifiedByUserId, Date scoresFetchedTime)
	{
		if ((context == null || context.trim().length() == 0) || (itemId == null || itemId.trim().length() == 0) || (participantItemDetails == null || participantItemDetails.size() == 0) || (modifiedByUserId == null || modifiedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		
		// is it mneme itemid?
		if (!itemId.startsWith("MNEME-"))
		{
			return false;
		}
		
		String id;
		id = itemId.substring(itemId.indexOf("-") + 1);
		
		// user must have grade permission in the context of the assessment for this submission
		if (!mnemeSecurityService.checkSecurity(modifiedByUserId, MnemeService.GRADE_PERMISSION, context))
		{
			return false;
		}
		
		// get Assessment
		Assessment assessment = this.assessmentService.getAssessment(id);
		if (assessment == null)
		{
			throw new IllegalArgumentException();
		}
		
		Map<String, ArrayList<Submission>> usersSubmissionsMap = getUsersAllAssessmentSubmissions(assessment, false);
		
		String userId = null;
		Boolean inProgress = null;
		for (ParticipantItemDetails participantItemDetail :participantItemDetails)
		{			
			if (participantItemDetail.getSubmissionId() != null)
			{
				userId = participantItemDetail.getUserId();
				List<Submission> userSubmissions = usersSubmissionsMap.get(userId);
				
				if (userSubmissions != null)
				{
					for (Submission submission : userSubmissions)
					{
						inProgress = !submission.getIsComplete() && !submission.getIsPhantom();
						
						if (submission.getId().equalsIgnoreCase(participantItemDetail.getSubmissionId()) && !inProgress)
						{
							// avoid saving stale data
							if (scoresFetchedTime != null && submission.getEvaluatedDate().after(scoresFetchedTime))
							{
								continue;
							}
							
							submission.setTotalScore(participantItemDetail.getScore());
							
							submission.setIsReleased(participantItemDetail.getIsReleased());
							
							try
							{
								this.submissionService.evaluateSubmission(submission);
							}
							catch (AssessmentPermissionException e)
							{
								if (logger.isWarnEnabled())
								{
									logger.warn("Error while updating user mneme submission score: " + e);
								}
							}
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
	public Boolean modifyMnemeUserScores(String context, String userId, List<ParticipantItemDetails> participantItemDetails, String modifiedByUserId, Date scoresFetchedTime)
	{
		if ((context == null || context.trim().length() == 0) || (participantItemDetails == null || participantItemDetails.size() == 0) || (modifiedByUserId == null || modifiedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing.");
		}
		// update user score and released status
		Map<String, Submission> userSubmissionsMap = new HashMap<String, Submission>();
		
		List<Assessment> assessments = this.assessmentService.getContextAssessments(context, AssessmentsSort.odate_a, Boolean.TRUE);
		
		for (Assessment assessment : assessments)
		{
			// ignore not published, invalid dates assessments and surveys
			if (!assessment.getDates().getIsValid() || assessment.getType() == AssessmentType.survey || !assessment.getGradebookIntegration())
			{
				continue;
			}
			
			GradebookItemAccessStatus accessStatus = getAccessStatus(assessment);
			
			if (accessStatus == GradebookItemAccessStatus.invalid)
			{
				continue;
			}
			
			Map<String, ArrayList<Submission>> usersSubmissionsMap = getUsersAllAssessmentSubmissions(assessment, false);
			
			// there will at least one submission (may be phantom if there is none)
			if (usersSubmissionsMap != null && usersSubmissionsMap.size() > 0)
			{
				List<Submission> userSubmissions = usersSubmissionsMap.get(userId);
				
				if (userSubmissions != null)
				{
					for (Submission userSubmission : userSubmissions)
					{
						userSubmissionsMap.put(userSubmission.getId(), userSubmission);
					}
				}
			}
		}
		
		Submission submission = null;
		Boolean inProgress = null;
		for (ParticipantItemDetails participantItemDetail : participantItemDetails)
		{
			if (participantItemDetails != null && participantItemDetail.getUserId() != null && participantItemDetail.getUserId().equalsIgnoreCase(userId))
			{
				if (participantItemDetail.getSubmissionId() != null && participantItemDetail.getSubmissionId().trim().length() > 0)
				{
					submission = userSubmissionsMap.get(participantItemDetail.getSubmissionId());
					
					if (submission != null && submission.getUserId() != null && submission.getUserId().equalsIgnoreCase(userId))
					{
						inProgress = !submission.getIsComplete() && !submission.getIsPhantom();
						
						if (!inProgress)
						{
							// avoid saving stale data
							if (scoresFetchedTime != null && submission.getEvaluatedDate().after(scoresFetchedTime))
							{
								continue;
							}
							
							submission.setTotalScore(participantItemDetail.getScore());
							
							submission.setIsReleased(participantItemDetail.getIsReleased());
							
							try
							{
								this.submissionService.evaluateSubmission(submission);
							}
							catch (AssessmentPermissionException e)
							{
								if (logger.isWarnEnabled())
								{
									logger.warn("modifyMnemeUserScores(...): Error while updating user mneme score: " + e);
								}
							}
						}
					}
				}
			}
		}
		return true;
	}
	
	/**
	 * Set the AssessmentService.
	 * 
	 * @param service
	 *        The AssessmentService.
	 */
	public void setAssessmentService(AssessmentService service)
	{
		this.assessmentService = service;
	}
	
	/**
	 * @param gradebookService the gradebookService to set
	 */
	public void setGradebookService(GradebookService gradebookService)
	{
		this.gradebookService = gradebookService;
	}
	
	/**
	 * @param mnemeSecurityService the mnemeSecurityService to set
	 */
	public void setMnemeSecurityService(SecurityService mnemeSecurityService)
	{
		this.mnemeSecurityService = mnemeSecurityService;
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
	 * Set the SubmissionService.
	 * 
	 * @param service
	 *        The SubmissionService.
	 */
	public void setSubmissionService(SubmissionService service)
	{
		this.submissionService = service;
	}
	
	/**
	 * Figure the access status for the assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @return The access status for the assessment.
	 */
	protected GradebookItemAccessStatus getAccessStatus(Assessment assessment)
	{
		// set status - invalid trumps unpublished
		GradebookItemAccessStatus accessStatus = GradebookItemAccessStatus.published;
		if (!assessment.getIsValid())
		{
			accessStatus = GradebookItemAccessStatus.invalid;
		}
		else if (!assessment.getPublished())
		{
			accessStatus = GradebookItemAccessStatus.unpublished;
		}
		// valid and published, lets see if we are not yet open or if we are closed
		else
		{
			if (!assessment.getDates().getIsOpen(Boolean.FALSE))
			{
				if (assessment.getDates().getIsClosed())
				{
					accessStatus = GradebookItemAccessStatus.published_closed;
				}
				else
				{
					Date now = new Date();
					if ((assessment.getDates().getOpenDate() != null) && (now.before(assessment.getDates().getOpenDate())))
					{
						if (assessment.getDates().getHideUntilOpen().booleanValue())
						{
							accessStatus = GradebookItemAccessStatus.published_hidden;
						}
						else
						{
							accessStatus = GradebookItemAccessStatus.published_not_yet_open;
						}
					}
				}
			}
		}

		return accessStatus;
	}
	
	/**
	 * get the review Link
	 * @param context
	 * @param assessment
	 * @param best submission
	 * @return
	 */
	
	/**
	 * Get assessment scores and assign zero to non-submitters for non offline assessments after the closing date
	 * 
	 * @param assessment		Assessment
	 * 
	 * @param activeParticipantIds	Active participant id's
	 * 
	 * @return Map of user id and score
	 */
	protected Map<String, Float> getAssessmentScoresAndAssignZeroToNonSubmitters(Assessment assessment, Set<String> activeParticipantIds)
	{
		if (assessment == null || activeParticipantIds.size() == 0)
		{
			return new HashMap<String, Float>();
		}
		
		Map<String, Float> usersAssementScores = new HashMap<String, Float>();
		
		if (assessment.getType() != AssessmentType.offline)
		{
			/*
			// for tests and assignments with all essay type questions should also be evaluated and released
			boolean allEssayTypeQuestions = true;
			if ((assessment.getType() == AssessmentType.assignment) || (assessment.getType() == AssessmentType.test))
			{
				List<Question> questions = assessment.getParts().getQuestions();
				
				if (questions.size() > 0)
				{
					for (Question question : questions)
					{
						if (!question.getType().equalsIgnoreCase("mneme:Essay"))
						{
							allEssayTypeQuestions = false;
							break;
						}
					}
				}
				else
				{
					allEssayTypeQuestions = false;
				}
			}
			*/
			
			Date itemDueDate = null;
			Date itemCloseDate = null;
			Date itemLastSubmitDate = null;
			Date itemUserSpecialAccessDueDate = null;
			Date itemUserSpecialAccessCloseDate = null;
			
			Date now = new Date();
			
			Map<String, Float> assementScores = this.submissionService.getAssessmentHighestScores(assessment, Boolean.TRUE);
			Map<String, Float> assementScoresMap = new HashMap<String, Float>();
			
			Map<String, ArrayList<Submission>> usersSubmissionsMap = getUsersAllAssessmentSubmissions(assessment, false);
			
			if (assementScores != null && assementScores.size() > 0)
			{
				if (activeParticipantIds != null && activeParticipantIds.size() > 0)
				{
					for (Map.Entry<String, Float> entry : assementScores.entrySet()) 
					{
						if (!activeParticipantIds.contains(entry.getKey()))
						{
							continue;
						}
						
						if (entry.getValue() != null)
						{
							assementScoresMap.put(entry.getKey(), entry.getValue());
						}
					}
				}
			}
			
			Float score = null;
			
			for (String activeParticipantId : activeParticipantIds)
			{
				// if item is closed and user has no submissions zero assign score to user
				itemDueDate = assessment.getDates().getDueDate();
				itemCloseDate = assessment.getDates().getAcceptUntilDate();
				itemLastSubmitDate = null;
				
				// user special access dates
				itemUserSpecialAccessDueDate = null;
				itemUserSpecialAccessCloseDate = null;
				
				score = null;
				
				UserItemSpecialAccess userItemSpecialAccess = getUserSpecialAccess(assessment, activeParticipantId);

				if (userItemSpecialAccess != null)
				{
					if (userItemSpecialAccess.getDueDate() != null)
					{
						itemUserSpecialAccessDueDate = userItemSpecialAccess.getDueDate();
					}
					
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
				
				/*
				// for tests and assignments with all essay type questions should also be evaluated and released
				if (allEssayTypeQuestions)
				{
					// check for user submissions
					List<Submission> userSubmissions = usersSubmissionsMap.get(activeParticipantId);
					if (userSubmissions != null)
					{
						Submission userBestSubmission = userSubmissions.get(0).getBest();
						
						if (userBestSubmission != null)
						{
							if (userBestSubmission.getEvaluation() != null && userBestSubmission.getEvaluation().getEvaluated())
							{
								score = assementScoresMap.get(activeParticipantId);
							}
						}
					}
				}
				else
				{
					score = assementScoresMap.get(activeParticipantId);
				}
				*/
				
				score = assementScoresMap.get(activeParticipantId);
				
				Integer siblingCount = 0;
							
				// check for no submissions
				if (score == null)
				{
					if (usersSubmissionsMap != null && usersSubmissionsMap.size() > 0)
					{
						Boolean inProgress = Boolean.FALSE;
						siblingCount = 0;
						
						// check for user submissions
						List<Submission> userSubmissions = usersSubmissionsMap.get(activeParticipantId);
						
						if (userSubmissions != null && userSubmissions.size() > 0)
						{
							for (Submission userSubmission : userSubmissions)
							{
								inProgress = !userSubmission.getIsComplete() && !userSubmission.getIsPhantom();
								
								if (inProgress)
								{
									siblingCount = null;
									break;
								}
								
								if (!userSubmission.getIsPhantom())
								{
									/* this submission may be completed but sibling count fetched is zero. To fix the sibling count added below condition. 
									Not released submissions may be not fetching sibling count */
									if (userSubmission.getSiblingCount() > 0)
									{
										siblingCount = userSubmission.getSiblingCount();
									}
									else
									{
										siblingCount = 1;
									}
									break;
								}
							}
						}
					}
				}
				
				// assign zero if no submissions by the user
				if (score == null && itemLastSubmitDate != null && now.after(itemLastSubmitDate) && siblingCount != null && siblingCount == 0)
				{
					score = 0.0f;
				}
				
				usersAssementScores.put(activeParticipantId, score);
			}
		}
		else
		{
			usersAssementScores = this.submissionService.getAssessmentHighestScores(assessment, Boolean.TRUE);			
		}
		
		return usersAssementScores;
	}
	
	/**
	 * get the review Link
	 * 
	 * @param context		Context
	 * 
	 * @param assessment	Assessment
	 * 
	 * @param best			Best submission
	 * 
	 * @param returnPage	Return page
	 * 
	 * @param userId		Student user id
	 * 
	 * @param fetchInstructorLink 	If true fetch grading link else student's review link
	 * 
	 * @return				Returns grading link or student's review link or null
	 */
	protected String getBestSubmissionReviewLink(String context, Assessment assessment, Submission best, String returnPage, String userId, boolean fetchInstructorLink, boolean instructor)
	{
		String gbToolId = null;
		String mnemeToolId = null;
		String reviewLink = null;
		
		try
		{
			Site site = this.siteService.getSite(context);
			ToolConfiguration config = site.getToolForCommonId("sakai.mneme");
			if (config != null) mnemeToolId = config.getId();
			config = site.getToolForCommonId("e3.gradebook");
			if (config != null) gbToolId = config.getId();
		}
		catch (IdUnusedException e)
		{
			logger.warn("getBestSubmissionReviewLink: missing site: " + context);
		}
		
		// no tool id? No E3 GB in site!
		if (gbToolId == null || mnemeToolId == null) return null;
		
		if (best == null) return null;
		
		if (fetchInstructorLink)
		{
			//for instructors
			// user must have grade permission in the context of the assessment for this submission
			if (gbToolId != null)
			{
				reviewLink = "/portal/directtool/" + mnemeToolId +"/grade_submission/" + best.getId() + "/1-1/-/!portal!/" + gbToolId + "/" + returnPage + "/" +userId;
			}
		}
		else if (instructor)
		{
			Boolean mayReview = getStudentMayReview(best);
			
			if (gbToolId != null &&  mayReview != null && mayReview && best.getIsNonEvalOrCommented())
			{
				// link to review the best submission: /review/<sid>
				reviewLink = "/portal/directtool/" + mnemeToolId +"/review/" + best.getId() + "/!portal!/" + gbToolId + "/" + returnPage + "/" +userId;
			}
		}
		else if (gbToolId != null && best.getMayReview() && best.getIsNonEvalOrCommented())
		{
			// link to review the best submission: /review/<sid>
			reviewLink = "/portal/directtool/" + mnemeToolId +"/review/" + best.getId() + "/!portal!/" + gbToolId ;
		}
		
		return reviewLink;
	}
	
	/**
	 * Get detail information for a Mneme item (assessment) for all who submitted.
	 * 
	 * @param context
	 *        The site id.
	 * @param itemId
	 *        The assessment id.
	 * @param participants
	 * 
	 * @return a List of ParticipantMnemeItemOverview.
	 */
	//@Override
	protected List<ParticipantItemDetails> getMnemeItemDetails(String context, String itemId, List<Participant> participants)
	{
		if ((context == null || context.trim().length() == 0) || (itemId == null || itemId.trim().length() == 0))
		{
			return new ArrayList<ParticipantItemDetails>();
		}
		
		List<ParticipantItemDetails> rv = new ArrayList<ParticipantItemDetails>();

		String id;
		id = itemId.substring(itemId.indexOf("-") + 1);
		
		// get Assessment
		Assessment assessment = this.assessmentService.getAssessment(id);
		if (assessment == null)
		{
			throw new IllegalArgumentException();
		}
		
		/*
		// for tests and assignments with all essay type questions should also be evaluated and released
		boolean allEssayTypeQuestions = true;
		if ((assessment.getType() == AssessmentType.assignment) || (assessment.getType() == AssessmentType.test))
		{
			List<Question> questions = assessment.getParts().getQuestions();
			
			if (questions.size() > 0)
			{
				for (Question question : questions)
				{
					if (!question.getType().equalsIgnoreCase("mneme:Essay"))
					{
						allEssayTypeQuestions = false;
						break;
					}
				}
			}
			else
			{
				allEssayTypeQuestions = false;
			}
		}
		*/

		Map<String, ArrayList<Submission>> usersSubmissionsMap = getUsersAllAssessmentSubmissions(assessment, true);
		
		Map<String, ArrayList<Submission>> usersAllSubmissionsMap = getUsersAllAssessmentSubmissions(assessment, false);
		
		// get best released scores
		Map<String, Float> assessmentHighestScores = this.submissionService.getAssessmentHighestScores(assessment, Boolean.TRUE);
		
		// create the ParticipantMnemeItemDetail with all needed information
		for (Participant p : participants)
		{
			// find this user's submissions
			boolean found = false;
			// this.submissionService.getSubmissionOfficialScore(Assessment assessment, String userId)
			if (usersSubmissionsMap.containsKey(p.getUserId()))
			{
				ArrayList<Submission> userSubmissions = usersSubmissionsMap.get(p.getUserId());
				
				if (userSubmissions != null && !userSubmissions.isEmpty())
				{
					Submission s = null;
					if (userSubmissions.size() > 1)
					{
						// get the best released submission
						for (Submission submission : userSubmissions)
						{
							Float highestScore = assessmentHighestScores.get(p.getUserId());
							
							if (submission.getTotalScore() != null && highestScore != null && submission.getTotalScore().equals(highestScore))
							{
								s = submission;
								break;
							}						
						}
					}
					else
					{
						s = userSubmissions.get(0);
						
						Float highestScore = assessmentHighestScores.get(p.getUserId());
						
						if (s.getTotalScore() != null && highestScore != null && s.getTotalScore().equals(highestScore))
						{
						}
						else
						{
							s = null;
						}
					}
				
					if (s != null)
					{
						Date started = null;
						Date finished = null;
						Date reviewed = null;
						Date evaluated = null;
						Boolean inProgress = Boolean.FALSE;
						Float score = null;
						Boolean released = Boolean.FALSE;
						Boolean isLate = Boolean.FALSE;
						Boolean isAutoSubmitted = Boolean.FALSE;
						Integer siblingCount = 0;
						
						if (s.getIsStarted() && !s.getIsNonSubmit() && (s.getAssessment().getType() != AssessmentType.offline))
						{
							started = s.getStartDate();
						}
						
						if (s.getIsComplete() && !s.getIsNonSubmit() && (s.getAssessment().getType() != AssessmentType.offline))
						{
							finished = s.getSubmittedDate();
						}
						
						reviewed = s.getReviewedDate();
						evaluated = s.getEvaluatedDate();
						released = s.getIsReleased();
						
						/*
						// for tests and assignments with all essay type questions should also be evaluated and released
						if (allEssayTypeQuestions)
						{
							if (s.getEvaluation() != null && s.getEvaluation().getEvaluated())
							{
								score = s.getTotalScore();
							}
						}
						else
						{
							score = s.getTotalScore();
						}
						*/
						score = s.getTotalScore();
						
						String reviewLink = null;
						String gradingLink = null;
						
						if (finished != null)
						{
							isLate = s.getIsCompletedLate();
						}
						isAutoSubmitted = s.getIsAutoCompleted();
						
						//review Link
						if (!s.getIsPhantom())
						{
							if (mnemeSecurityService.checkSecurity(UserDirectoryService.getCurrentUser().getId(), MnemeService.GRADE_PERMISSION, context))
							{
								gradingLink = getBestSubmissionReviewLink(context, assessment, s, "assessmentDetails", p.getUserId(), true, true);
								reviewLink = getBestSubmissionReviewLink(context, assessment, s, "assessmentDetails", p.getUserId(), false, true);
							}
							else
							{
								reviewLink = getBestSubmissionReviewLink(context, assessment, s, "assessmentDetails", p.getUserId(), false, false);
							}
						}
						
						// this submission is released but sibling count fetched is zero. To fix the sibling count added below condition
						if (s.getSiblingCount() > 0)
						{
							siblingCount = s.getSiblingCount();
						}
						else
						{
							siblingCount = 1;
						}
								
						ParticipantItemDetails pmid = this.gradebookService.newParticipantItemDetails(p.getUserId(), p.getDisplayId(), p.getGroupTitle(), p.getSortName(), p.getStatus(), siblingCount, evaluated, reviewed, score, null, finished, s.getEvaluationNotReviewed(), released, reviewLink, gradingLink, inProgress, s.getId(), isLate, isAutoSubmitted);
						
						// add user special access
						pmid.setUserItemSpecialAccess(getUserSpecialAccess(assessment, p.getUserId()));
						
						// private message link
						pmid.setPrivateMessageLink(p.getPrivateMessageLink());
						
						rv.add(pmid);
						found = true;
					}
				}
			}
			
			// if none found, make one
			if (!found)
			{
				Integer siblingCount = 0;
				Boolean inProgress = Boolean.FALSE;
				
				// check any submission in progress
				ArrayList<Submission> userAllSubmissions = usersAllSubmissionsMap.get(p.getUserId());
				if (userAllSubmissions != null && userAllSubmissions.size() > 0)
				{
					for (Submission userSubmission : userAllSubmissions)
					{
						inProgress = !userSubmission.getIsComplete() && !userSubmission.getIsPhantom();
						
						if (inProgress)
						{
							siblingCount = null;
							break;
						}
						
						if (!userSubmission.getIsPhantom())
						{
							/* this submission may be completed but sibling count fetched is zero. To fix the sibling count added below condition. 
							Not released submissions may be not fetching sibling count*/
							if (userSubmission.getSiblingCount() > 0)
							{
								siblingCount = userSubmission.getSiblingCount();
							}
							else
							{
								siblingCount = 1;
							}
							break;
						}
					}
				}
				
				ParticipantItemDetails pmid = this.gradebookService.newParticipantItemDetails(p.getUserId(), p.getDisplayId(), p.getGroupTitle(), p.getSortName(), p.getStatus(), siblingCount, null, null, null, null, null, null, null, null, null, null, null, null, null);
				
				// add user special access
				pmid.setUserItemSpecialAccess(getUserSpecialAccess(assessment, p.getUserId()));
				
				// private message link
				pmid.setPrivateMessageLink(p.getPrivateMessageLink());
				
				rv.add(pmid);
			}
		}

		return rv;
	}
	
	/**
	 * Copied from org.etudes.mneme.impl.SubmissionImpl -> protected Boolean getStudentMayReview() and updated
	 * 
	 * @return getMayReview test
	 */
	protected Boolean getStudentMayReview(Submission submission)
	{
		if (submission == null)
		{
			return null;
		}
		
		// submission complete
		if (!submission.getIsComplete()) return Boolean.FALSE;

		// published (test drive need not be)
		if (!submission.getIsTestDrive())
		{
			if (!submission.getAssessment().getPublished()) return Boolean.FALSE;
		}

		// valid
		if (!submission.getAssessment().getIsValid()) return Boolean.FALSE;

		// assessment review enabled
		if (!submission.getAssessment().getReview().getNowAvailable()) return Boolean.FALSE;

		// TODO: permission?

		return Boolean.TRUE;
	}
	
	/**
	 * Figure the course map item type from the assessment type.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @return The course map item type for this assessment.
	 */
	protected GradebookItemType getType(Assessment assessment)
	{
		switch (assessment.getType())
		{
			case test:
				return GradebookItemType.test;
			case survey:
				return GradebookItemType.survey;
			case assignment:
				return GradebookItemType.assignment;
			case offline:
				return GradebookItemType.offline;
		}

		logger.warn("getType: not recognied: " + assessment.getType());
		
		return GradebookItemType.test;
	}
	
	/**
	 * Gets all submissions of the users
	 * 
	 * @param assessment	Assessment
	 * 
	 * @param released 		If true gets only released else all the submissions
	 * 
	 * @return	The map of user id and submissions list
	 */
	protected Map<String, ArrayList<Submission>> getUsersAllAssessmentSubmissions(Assessment assessment, boolean released)
	{		
		if (assessment == null)
		{
			new HashMap<String, ArrayList<Submission>>();
		}
		
		// get all submissions
		List<Submission> submissions = this.submissionService.findAssessmentSubmissions(assessment, FindAssessmentSubmissionsSort.userName_a, Boolean.FALSE, null, null, null, Boolean.FALSE);

		// show the best released submission if there are more than one for user
		Map<String, ArrayList<Submission>> usersSubmissionsMap = new HashMap<String, ArrayList<Submission>>();
		for (Submission s : submissions)
		{
			if (released && (s.getIsPhantom() || !s.getIsReleased())) continue;
			
			if (usersSubmissionsMap.containsKey(s.getUserId()))
			{
				usersSubmissionsMap.get(s.getUserId()).add(s);
			}
			else
			{
				ArrayList<Submission> userSubmissions = new ArrayList<Submission>();
				userSubmissions.add(s);				
				usersSubmissionsMap.put(s.getUserId(), userSubmissions);
			}
		}
		return usersSubmissionsMap;
	}
	
	/**
	 * get user special access for the assessment
	 * 
	 * @param assessment	Assessment
	 * 
	 * @param userId		User id
	 * 
	 * @return	User special access if exists or null
	 */
	protected UserItemSpecialAccess getUserSpecialAccess(Assessment assessment, String userId)
	{
		if (assessment == null || (userId == null || userId.trim().length() == 0))
		{
			return null;
		}
		
		UserItemSpecialAccess userItemSpecialAccess = null;
		
		AssessmentSpecialAccess assessmentSpecialAccess = assessment.getSpecialAccess();
		AssessmentAccess userAssessmentAccess = assessmentSpecialAccess.getUserAccess(userId);
		if (userAssessmentAccess != null)
		{
			Date openDate = null; 
			Date dueDate = null; 
			Date acceptUntilDate = null;
			Boolean hideUntilOpen = null;
			Boolean overrideOpenDate = null;
			Boolean overrideDueDate = null;
			Boolean overrideAcceptUntilDate = null;
			Boolean overrideHideUntilOpen = null;
			Boolean datesValid = Boolean.TRUE;
			
			openDate = userAssessmentAccess.getOpenDate();
			dueDate = userAssessmentAccess.getDueDate();
			acceptUntilDate = userAssessmentAccess.getAcceptUntilDate();
			hideUntilOpen = userAssessmentAccess.getHideUntilOpen();
			overrideOpenDate = userAssessmentAccess.getOverrideOpenDate();
			overrideDueDate = userAssessmentAccess.getOverrideDueDate();
			overrideAcceptUntilDate = userAssessmentAccess.getOverrideAcceptUntilDate();
			overrideHideUntilOpen = userAssessmentAccess.getOverrideHideUntilOpen();
			datesValid = userAssessmentAccess.getIsValid();
					
			userItemSpecialAccess = this.gradebookService.newUserItemSpecialAccessImpl(openDate, dueDate, acceptUntilDate, hideUntilOpen, overrideOpenDate, overrideDueDate, overrideAcceptUntilDate, overrideHideUntilOpen, datesValid);
		}
		
		return userItemSpecialAccess;
	}
}
