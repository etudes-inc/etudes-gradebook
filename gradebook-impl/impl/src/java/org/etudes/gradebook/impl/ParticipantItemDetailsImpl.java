/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-impl/impl/src/java/org/etudes/gradebook/impl/ParticipantJforumItemDetailImpl.java $
 * $Id: ParticipantJforumItemDetailImpl.java 9378 2014-11-27 01:02:00Z murthyt $
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

import java.util.Date;

import org.etudes.gradebook.api.ParticipantItemDetails;
import org.etudes.gradebook.api.ParticipantStatus;
import org.etudes.gradebook.api.UserItemSpecialAccess;

/**
 * ParticipantItemDetailsImpl implements ParticipantItemDetails.
 */
public class ParticipantItemDetailsImpl extends ParticipantImpl implements ParticipantItemDetails
{
	/** Our logger. */
	// private static Log M_log = LogFactory.getLog(ParticipantJforumItemDetailImpl.class);

	/** submissions count or posts count */
	protected Integer count;
	
	/** The evaluated date. */
	protected Date evaluatedDate = null;

	/** When there is an evaluation later than the last review. */
	protected Boolean evaluationNotReviewed = Boolean.FALSE;

	/** The finished date or last post date */
	protected Date finishedDate = null;

	/** The grading link*/
	protected String gradingLink = null;

	/** The user id. */
	protected String id = null;

	/** The in-progress indicator. */
	protected Boolean inProgress = Boolean.FALSE;

	/** Is auto submission  **/
	protected Boolean isAutoSubmission = Boolean.FALSE;
	
	/** Is best mneme submission **/
	protected Boolean isBestSubmission = null;
	
	/** To suppress started and finished dates being displayed. */
	//protected Boolean suppressDates = Boolean.FALSE;
	
	protected Boolean isReleased = null;

	/** The sort name. */
	// protected String sortName = null;

	protected Boolean isScoreDropped = Boolean.FALSE;
	
	/** Is submission late **/
	protected Boolean isSubmissionLate = Boolean.FALSE;
	
	/** The reviewed date. */
	protected Date reviewedDate = null;
	
	/** The review link*/
	protected String reviewLink = null;
	
	/** The status. */
	// protected ParticipantStatus status = null;
	protected Float score = null;
	
	/** The started date or first post date */
	protected Date startedDate = null;
	
	/** The mneme submission id **/
	protected String submissionId = null;
	
	protected UserItemSpecialAccess userItemSpecialAccess = null;
	
	/**
	 * jforum or mneme participant item
	 * 
	 * @param id				User id
	 * 
	 * @param displayId			User display id
	 * 
	 * @param groupTitle		Group title
	 * 
	 * @param sortName			Sort name
	 * 
	 * @param status			Partidpant status
	 * 
	 * @param count				Jforum posts count
	 * 
	 * @param evaluatedDate		Evaluated date
	 * 
	 * @param reviewedDate		Reviewed date
	 * 
	 * @param score				Score
	 * 
	 * @param startedDate		Started date
	 * 
	 * @param finishedDate		Finished date
	 * 
	 * @param evaluationNotReviewed	true if evaluation not reviewed
	 * 
	 * @param released			True is released
	 * 
	 * @param reviewLink		Review link
	 * 
	 * @param gradingLink		Grading link
	 * 
	 * @param inProgress		true if in progress
	 * 
	 * @param submissionId		Mneme Submission id
	 */
	public ParticipantItemDetailsImpl(String id, String displayId, String groupTitle, String sortName, ParticipantStatus status, Integer count, Date evaluatedDate, Date reviewedDate, 
													Float score, Date startedDate, Date finishedDate, Boolean evaluationNotReviewed, Boolean released, String reviewLink, 
													String gradingLink, Boolean inProgress, String submissionId, Boolean isSubmissionLate, Boolean isAutoSubmission)
	{
		this.id = id;
		this.userId = id;
		this.displayId = displayId;
		this.groupTitle = groupTitle;
		this.sortName = sortName;
		this.status = status;
		this.count = count;
		this.evaluatedDate = evaluatedDate;
		this.reviewedDate = reviewedDate;
		this.score = score;
		this.startedDate = startedDate;
		this.finishedDate = finishedDate;
		this.evaluationNotReviewed = evaluationNotReviewed;
		this.isReleased = released;
		this.reviewLink = reviewLink;
		this.gradingLink = gradingLink;
		this.inProgress = inProgress;
		this.submissionId = submissionId;
		this.isSubmissionLate = isSubmissionLate;
		this.isAutoSubmission = isAutoSubmission;
	}

	/**
	 * jforum or mneme participant item
	 * 
	 * @param id				User id
	 * 
	 * @param displayId			User display id
	 * 
	 * @param groupTitle		Group title
	 * 
	 * @param sortName			Sort name
	 * 
	 * @param status			Partidpant status
	 * 
	 * @param count				Jforum posts count
	 * 
	 * @param evaluatedDate		Evaluated date
	 * 
	 * @param reviewedDate		Reviewed date
	 * 
	 * @param score				Score
	 * 
	 * @param startedDate		Started date
	 * 
	 * @param finishedDate		Finished date
	 * 
	 * @param evaluationNotReviewed	true if evaluation not reviewed
	 * 
	 * @param released			True is released
	 * 
	 * @param reviewLink		Review link
	 * 
	 * @param gradingLink		Grading link
	 * 
	 * @param inProgress		true if in progress
	 * 
	 * @param submissionId		Mneme Submission id
	 * 
	 * @param isBestSubmission	True if mneme submission is best
	 */
	public ParticipantItemDetailsImpl(String id, String displayId, String groupTitle, String sortName, ParticipantStatus status, Integer count, Date evaluatedDate, Date reviewedDate, 
			Float score, Date startedDate, Date finishedDate, Boolean evaluationNotReviewed, Boolean released, String reviewLink, String gradingLink, Boolean inProgress, String submissionId, 
			Boolean isBestSubmission, Boolean isSubmissionLate, Boolean isAutoSubmission)
	{
		this.id = id;
		this.userId = id;
		this.displayId = displayId;
		this.groupTitle = groupTitle;
		this.sortName = sortName;
		this.status = status;
		this.count = count;
		this.evaluatedDate = evaluatedDate;
		this.reviewedDate = reviewedDate;
		this.score = score;
		this.startedDate = startedDate;
		this.finishedDate = finishedDate;
		this.evaluationNotReviewed = evaluationNotReviewed;
		this.isReleased = released;
		this.reviewLink = reviewLink;
		this.gradingLink = gradingLink;
		this.inProgress = inProgress;
		this.submissionId = submissionId;
		this.isBestSubmission = isBestSubmission;
		this.isSubmissionLate = isSubmissionLate;
		this.isAutoSubmission = isAutoSubmission;
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer getCount()
	{
		return count;
	}

	/**
	 * {@inheritDoc}
	 */
	public Date getEvaluatedDate()
	{
		return this.evaluatedDate;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getEvaluationNotReviewed()
	{
		return this.evaluationNotReviewed;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getEvaluationReviewed()
	{
		if ((this.evaluatedDate == null) || (this.reviewedDate == null)) return Boolean.FALSE;

		return this.reviewedDate.after(this.evaluatedDate);
	}

	/**
	 * {@inheritDoc}
	 */
	public Date getFinishedDate()
	{
		return this.finishedDate;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getGradingLink()
	{
		return gradingLink;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getId()
	{
		return this.id;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Boolean getInProgress()
	{
		return inProgress;
	}


	/**
	 * @return the isAutoSubmission
	 */
	public Boolean getIsAutoSubmission()
	{
		return isAutoSubmission;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsBestSubmission()
	{
		return isBestSubmission;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsReleased()
	{
		return isReleased;
	}
	
	/**
	 * @return the isScoreDropped
	 */
	public Boolean getIsScoreDropped()
	{
		return isScoreDropped;
	}

	/**
	 * @return the isSubmissionLate
	 */
	public Boolean getIsSubmissionLate()
	{
		return isSubmissionLate;
	}

	/**
	 * {@inheritDoc}
	 */
	public Date getReviewedDate()
	{
		return this.reviewedDate;
	}

	
	
	/**
	 * {@inheritDoc}
	 */
	public String getReviewLink()
	{
		return this.reviewLink;
	}

	/**
	 * @return the score
	 */
	public Float getScore()
	{
		return roundToTwoDecimals(score);
	}

	/**
	 * {@inheritDoc}
	 */
	public Date getStartedDate()
	{
		return startedDate;
	}
	/**
	 * {@inheritDoc}
	 */
	public String getSubmissionId()
	{
		return submissionId;
	}

	/**
	 * @return the userItemSpecialAccess
	 */
	public UserItemSpecialAccess getUserItemSpecialAccess()
	{
		return userItemSpecialAccess;
	}

	/**
	 * @param isAutoSubmission the isAutoSubmission to set
	 */
	public void setIsAutoSubmission(Boolean isAutoSubmission)
	{
		this.isAutoSubmission = isAutoSubmission;
	}

	/**
	 * @param isBestSubmission the isBestSubmission to set
	 */
	public void setIsBestSubmission(Boolean isBestSubmission)
	{
		this.isBestSubmission = isBestSubmission;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setIsReleased(Boolean isReleased)
	{
		this.isReleased = isReleased;
	}

	/**
	 * @param isSubmissionLate the isSubmissionLate to set
	 */
	public void setIsSubmissionLate(Boolean isSubmissionLate)
	{
		this.isSubmissionLate = isSubmissionLate;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setScore(Float score)
	{
		this.score = score;
	}
	
	/**
	 * @param userItemSpecialAccess the userItemSpecialAccess to set
	 */
	public void setUserItemSpecialAccess(UserItemSpecialAccess userItemSpecialAccess)
	{
		this.userItemSpecialAccess = userItemSpecialAccess;
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
	 * {@inheritDoc}
	 */
	/*
	public Boolean getSuppressDates()
	{
		return suppressDates;
	}
	*/

	/**
	 * @param submissionId the submissionId to set
	 */
	/*
	void setSubmissionId(String submissionId)
	{
		this.submissionId = submissionId;
	}
	*/
	
	/**
	 * @param isScoreDropped the isScoreDropped to set
	 */
	void setIsScoreDropped(Boolean isScoreDropped)
	{
		this.isScoreDropped = isScoreDropped;
	}
}
