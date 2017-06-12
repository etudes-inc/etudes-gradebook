/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-api/api/src/java/org/etudes/gradebook/api/ParticipantJforumItemDetail.java $
 * $Id: ParticipantJforumItemDetail.java 9378 2014-11-27 01:02:00Z murthyt $
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

/**
 * ParticipantItemDetails shows Gradeable item (category, forum or topic, test or offline or assignment) specific detail information for a participant.
 */
public interface ParticipantItemDetails extends Participant
{
	/**
	 * @return the count. Submission count in mneme and posts count in jforum
	 */
	public Integer getCount();
	
	/**
	 * @return the isAutoSubmission
	 */
	public Boolean getIsAutoSubmission();

	/**
	 * @return the isSubmissionLate
	 */
	public Boolean getIsSubmissionLate();

	/**
	 * @return The date of the latest evaluation of the submission, or null if not evaluated.
	 */
	Date getEvaluatedDate();

	/**
	 * @return TRUE if there is an evaluation date, and there was no review, or the review was before the evaluation.
	 */
	Boolean getEvaluationNotReviewed();

	/**
	 * @return TRUE if there is an evaluation date, and a reviewed date, and the review was after the evaluation.
	 */
	Boolean getEvaluationReviewed();

	/**
	 * @return The date the user completed the "best" submission, or null if there is no submission Or the first last date in jforum.
	 */
	Date getFinishedDate();

	/**
	 * @return the gradingLink or null
	 */
	String getGradingLink();

	/**
	 * @return The user id.
	 */
	String getId();

	/**
	 * @return TRUE if the user is in-progress with a submission to this assessment, FALSE if not.
	 */
	Boolean getInProgress();
	
	/**
	 * @return the is best mneme submission
	 */
	Boolean getIsBestSubmission();

	/**
	 * @return True is submission is released
	 */
	Boolean getIsReleased();
	
	
	/**
	 * @return TRUE to suppress display of the started and finished dates, FALSE otherwise.
	 */
	// Boolean getSuppressDates();
	
	/**
	 * @return true if this is lowest dropped score else false
	 */
	Boolean getIsScoreDropped();
	
	/**
	 * @return The date the user has reviewed the evaluated submission, or null if not reviewed.
	 */
	Date getReviewedDate();
	
	/**
	 * @return The link to review the best submission or null
	 */
	String getReviewLink();
	
	/**
	 * @return the score
	 */
	Float getScore();
	
	/**
	 * @return The date the user started the "best" submission, or null if there is no submission Or the first post date in jforum.
	 */
	Date getStartedDate();
	
	/**
	 * @return the mneme submissionId
	 */
	String getSubmissionId();
	
	/**
	 * @return the userItemSpecialAccess
	 */
	UserItemSpecialAccess getUserItemSpecialAccess();
	
	/**
	 * @param isReleased the isReleased to set
	 */
	void setIsReleased(Boolean isReleased);
	
	/**
	 * @param score the score to set
	 */
	void setScore(Float score);

	/**
	 * @param userItemSpecialAccess the userItemSpecialAccess to set
	 */
	void setUserItemSpecialAccess(UserItemSpecialAccess userItemSpecialAccess);
}
