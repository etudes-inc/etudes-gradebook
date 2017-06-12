/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-webapp/webapp/src/java/org/etudes/gradebook/cdp/GradebookCdpHandler.java $
 * $Id: GradebookCdpHandler.java 12452 2016-01-06 00:29:51Z murthyt $
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

package org.etudes.gradebook.cdp;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.cdp.api.CdpHandler;
import org.etudes.cdp.api.CdpStatus;
import org.etudes.cdp.util.CdpResponseHelper;
import org.etudes.coursemap.api.CourseMapItem;
import org.etudes.coursemap.api.CourseMapItemProgressStatus;
import org.etudes.gradebook.api.Gradebook;
import org.etudes.gradebook.api.Gradebook.BoostUserGradesType;
import org.etudes.gradebook.api.Gradebook.GradebookSortType;
import org.etudes.gradebook.api.Gradebook.ReleaseGrades;
import org.etudes.gradebook.api.GradebookCategory;
import org.etudes.gradebook.api.GradebookCategoryItemMap;
import org.etudes.gradebook.api.GradebookItem;
import org.etudes.gradebook.api.GradebookItemType;
import org.etudes.gradebook.api.GradebookService;
import org.etudes.gradebook.api.GradingScale;
import org.etudes.gradebook.api.GradingScalePercent;
import org.etudes.gradebook.api.Notes;
import org.etudes.gradebook.api.Participant;
import org.etudes.gradebook.api.ParticipantGradebookItem;
import org.etudes.gradebook.api.ParticipantItemDetails;
import org.etudes.gradebook.api.ParticipantJforumItemDetailsSort;
import org.etudes.gradebook.api.ParticipantMnemeItemDetailsSort;
import org.etudes.gradebook.api.ParticipantSort;
import org.etudes.gradebook.api.UserCategoryGrade;
import org.etudes.gradebook.api.UserGrade;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.util.StringUtil;
import org.sakaiproject.util.Validator;

/**
 */
public class GradebookCdpHandler implements CdpHandler
{
	// easy to capture assessments table last sort on
	enum GradebookLastSort
	{
		column0("Category"), column1("Title"), column3("OpenDate"), column4("DueDate"), 
		dcolumn0("Name"), dcolumn4("Status"), dcolumn5("Finished"), dcolumn6("Reviewed"),dcolumn8("Score"), 
		gcolumn0("Name"), gcolumn3("Section"), gcolumn4("Status"), gcolumn5("Score"), gcolumn8("Overall Grade");
		String name;

		GradebookLastSort(String n)
		{
			name = n;
		}

		String showName()
		{
			return name;
		}
	}

	/** Our log (commons). */
	private static Log logger = LogFactory.getLog(GradebookCdpHandler.class);

	/**
	 * prefix of our tool
	 */
	public String getPrefix()
	{
		return "etudesgradebook";
	}

	/**
	 *  Handles request
	 */
	public Map<String, Object> handle(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String requestPath, String path, String authenticatedUserId) throws ServletException, IOException
	{
		// if no authenticated user, we reject all requests
		if (authenticatedUserId == null)
		{
			Map<String, Object> rv = new HashMap<String, Object>();
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.notLoggedIn.getId());
			return rv;
		}

		// get the site id parameter
		String siteId = (String) parameters.get("siteId");
		if (siteId == null)
		{
			Map<String, Object> rv = new HashMap<String, Object>();
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		Map<String, Object> rv = new HashMap<String, Object>();
		
		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		Site site = null;
		try
		{
			site = siteService().getSite(siteId);
			rv.put("siteTitle", site.getTitle());
		}
		catch (IdUnusedException e)
		{
		}

		rv.put("gradebookSiteId", siteId);

		Boolean allowEdit = GradebookService().allowEditGradebook(siteId, authenticatedUserId);
				
		if (requestPath.equals("assessments"))
		{
			if (allowEdit)
			{				
				return dispatchOverview(req, res, parameters, path, authenticatedUserId);
			}
			else
			{
				Boolean allowGet = GradebookService().allowGetGradebook(siteId, authenticatedUserId);

				if (allowGet)
				{
					return dispatchUserOverview(req, res, parameters, path, authenticatedUserId, false, null);
				}
			}
		}
		else if (requestPath.equals("assessmentDetails"))
		{
			if (allowEdit) return dispatchItemOverview(req, res, parameters, path, authenticatedUserId);
		}
		else if (requestPath.equals("studentGrades"))
		{
			if (allowEdit) return dispatchStudentsOverallGrades(req, res, parameters, path, authenticatedUserId);
		}
		else if (requestPath.equals("indvidualStudentGrades"))
		{
			if (allowEdit)
			{
				Map<String, Object> rvs = new HashMap<String, Object>();
				rvs = findParticipantIndex(parameters, authenticatedUserId, rvs);
				rvs = dispatchUserOverview(req, res, parameters, path, authenticatedUserId, true, rvs);
				return rvs;
			}
		}
		else if (requestPath.equals("categories"))
		{
			if (allowEdit)
			{
				String action = (String) parameters.get("action");

				if (action != null && action.equalsIgnoreCase("grading_manage_type_changed"))
				{
					return dispatchCategoriesSaveType(req, res, parameters, path, authenticatedUserId);
				}
				else if (action != null && action.equalsIgnoreCase("categories_changed"))
				{
					return dispatchCategoriesSave(req, res, parameters, path, authenticatedUserId);
				}
				else
				{
					return dispatchCategories(req, res, parameters, path, authenticatedUserId);
				}
			}
		}
		else if (requestPath.equals("gradeoptions"))
		{
			if (allowEdit)
			{
				String action = (String) parameters.get("action");

				if (action != null && action.equalsIgnoreCase("grading_scale_changed"))
				{
					return dispatchGradeOptionsChangeGradingScale(req, res, parameters, path, authenticatedUserId);
				}
				else if (action != null && action.equalsIgnoreCase("overallGrades_type_changed"))
				{
					dispatchGradeOptionsOverAllGradesType(req, res, parameters, path, authenticatedUserId);
				}
				else if (action != null && action.equalsIgnoreCase("grading_scale_save"))
				{
					return dispatchGradeOptionsSaveGradingScale(req, res, parameters, path, authenticatedUserId);
				}
				else if (action != null && action.equalsIgnoreCase("grading_dropLowest_save"))
				{
					return dispatchGradeOptionsSaveLowestDrop(req, res, parameters, path, authenticatedUserId);
				}
				else if (action != null && action.equalsIgnoreCase("grading_dropLowest_set"))
				{
					return dispatchGradeOptionsOverAllGradesType(req, res, parameters, path, authenticatedUserId);
				}				
				else
				{
					return dispatchGradeOptions(req, res, parameters, path, authenticatedUserId);
				}
			}
		}
		else if (requestPath.equals("boostGrades"))
		{
			return dispatchSaveBoostGrades(req, res, parameters, path, authenticatedUserId);
		}

		return rv;
	}
	
	/**
	 * 
	 * @param assessmentMap
	 * @param category
	 * @return
	 */
	protected boolean categoryHasWeights(List<GradebookCategory> categories)
	{
		boolean showWeight = false;
	
		if (categories != null)
		{			
			for (GradebookCategory c : categories)
			{
				if (c.getWeight() != null && c.getWeight() > 0)
				{
					showWeight = true;
					break;
				}
			}				
		}
		return showWeight;
	}
	
	/**
	 * Check if the request is valid or not. if returns true then request is bad.
	 * 
	 * @param parameters
	 * @param rv
	 * @param authenticatedUserId
	 * @return
	 */
	protected boolean checkBadRequest(Map<String, Object> parameters, Map<String, Object> rv, String authenticatedUserId)
	{
		boolean foundBad = false;
		if (authenticatedUserId == null || authenticatedUserId.trim().length() == 0)
		{
			foundBad = true;
			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
		}

		// get the site id parameter
		String siteId = (String) parameters.get("siteId");

		if (siteId == null)
		{
			logger.warn("dispatchItemOverview - no siteId parameter");
			foundBad = true;
			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
		}
		return foundBad;
	}

	/**
	 * 
	 * @param req
	 * @param res
	 * @param parameters
	 * @param path
	 * @param authenticatedUserId
	 * @return
	 */
	protected Map<String, Object> dispatchCategories(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,	String authenticatedUserId)
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		if (checkBadRequest(parameters, rv, authenticatedUserId)) return rv;

		// get the site id parameter
		String siteId = (String) parameters.get("siteId");

		Boolean allowEdit = GradebookService().allowEditGradebook(siteId, authenticatedUserId);

		if (!allowEdit) return rv;		

		Map<String, Object> gradebookMap = new HashMap<String, Object>();
		
		rv.put(siteId, siteId);
		
		rv.put("gradebook", gradebookMap);

		// gradebook categories type
		Gradebook gradebook = GradebookService().getContextGradebook(siteId, authenticatedUserId);
		loadCategories(gradebook, gradebookMap, siteId, authenticatedUserId);
	
		return rv;
	}

	/**
	 * Save Categories title, weight distribution and order
	 * 
	 * @param req
	 * @param res
	 * @param parameters
	 * @param path
	 * @param authenticatedUserId
	 * @return
	 */
	protected Map<String, Object> dispatchCategoriesSave(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters,
			String path, String authenticatedUserId)
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// get the site id parameter
		String siteId = (String) parameters.get("siteId");

		if (siteId == null || siteId.trim().length() == 0)
		{
			return rv;
		}

		Boolean allowEdit = GradebookService().allowEditGradebook(siteId, authenticatedUserId);

		if (!allowEdit)
		{
			return rv;
		}
		String assessmentsLastSortData = (String) parameters.get("assessmentsLastSortData"); 
		if (assessmentsLastSortData != null && assessmentsLastSortData.length() > 0) rv.put("assessmentsLastSortData", assessmentsLastSortData);
		
		List<GradebookCategory> gcsList = GradebookService().getContextGradebook(siteId, authenticatedUserId).getGradebookCategories();
		// Set categories
		String categoriesCount = (String) parameters.get("categoriesCount");
		if (categoriesCount != null && !categoriesCount.equals("undefined"))
		{
			List<GradebookCategory> categories = new ArrayList<GradebookCategory>();

			int categoriesLength = Integer.parseInt(categoriesCount);
			for (int i = 0; i < categoriesLength; i++)
			{
				String newTitleFound = StringUtil.trimToNull((String) parameters.get("newTitle" + i));
				newTitleFound = (newTitleFound == null || newTitleFound.equals("undefined")) ? "untitled" : newTitleFound;

				String newWeightFound = StringUtil.trimToNull((String) parameters.get("newWeight" + i));
				Float newWeight = (newWeightFound == null || newWeightFound.equals("undefined")) ? null : new Float(newWeightFound);

				String newDistribution = StringUtil.trimToNull((String) parameters.get("newDistribution" + i));
				GradebookCategory.WeightDistribution distribution = (newDistribution == null || newDistribution.equals("undefined")) ? GradebookCategory.WeightDistribution.Points
						: GradebookCategory.WeightDistribution.getWeightDistribution(Integer.parseInt(newDistribution));

				String categoryId = StringUtil.trimToNull((String) parameters.get("categoryId" + i));
				int id = (categoryId == null || categoryId.equals("undefined")) ? 0 : Integer.parseInt(categoryId);

				String categoryOrder = StringUtil.trimToNull((String) parameters.get("order" + i));
				int order = (categoryOrder == null || categoryOrder.equals("undefined")) ? i : Integer.parseInt(categoryOrder);

				// create category object and modifyCtaegories
				GradebookCategory gc = null;
				if (id == 0)
				{
					gc = GradebookService().newGradebookCategory(newTitleFound, newWeight, distribution, order);
				}
				else
				{
					gc = findExistingCategory(id, gcsList);
					if (gc != null)
					{
						gc.setTitle(newTitleFound);
						gc.setWeight(newWeight);
						gc.setWeightDistribution(distribution);
						gc.setOrder(order);
					}
				}
				categories.add(gc);
			}
			if (categories.size() > 0) GradebookService().addModifyDeleteContextGradebookCategories(siteId, categories, authenticatedUserId);
		}

		return rv;
	}

	/**
	 * Save preferences sort type
	 * 
	 * @param req
	 * @param res
	 * @param parameters
	 * @param path
	 * @param authenticatedUserId
	 * @return
	 */
	protected Map<String, Object> dispatchCategoriesSaveType(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path, String authenticatedUserId)
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// get the site id parameter
		String siteId = (String) parameters.get("siteId");

		if (siteId == null || siteId.trim().length() == 0)
		{
			return rv;
		}

		Boolean allowEdit = GradebookService().allowEditGradebook(siteId, authenticatedUserId);

		if (!allowEdit)
		{
			return rv;
		}

		Gradebook.CategoryType sortType = Gradebook.CategoryType.Standard;

		String sortTypeVal = (String) parameters.get("sortType");
		if (sortTypeVal != null)
		{
			try
			{
				int sortTypeSelected = Integer.parseInt(sortTypeVal);

				if (sortTypeSelected == 2)
				{
					sortType = Gradebook.CategoryType.Custom;
				}				
			}
			catch (NumberFormatException e)
			{
				// do nothing
			}
		}
		GradebookService().modifyContextGradebookCategoryType(siteId, sortType, authenticatedUserId); 

		return rv;
	}

	/**
	 * 
	 * @param req
	 * @param res
	 * @param parameters
	 * @param path
	 * @param authenticatedUserId
	 * @return
	 */
	protected Map<String, Object> dispatchGradeOptions(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path, String authenticatedUserId)
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		if (checkBadRequest(parameters, rv, authenticatedUserId)) return rv;

		// get the site id parameter
		String siteId = (String) parameters.get("siteId");

		Boolean allowEdit = GradebookService().allowEditGradebook(siteId, authenticatedUserId);

		if (!allowEdit)
		{
			return rv;
		}

		Gradebook gradebook = GradebookService().getContextGradebook(siteId, authenticatedUserId);

		Map<String, Object> gradebookMap = new HashMap<String, Object>();

		rv.put("gradebook", gradebookMap);

		loadGradeOptions(gradebook, gradebookMap);
		
		Map<String, Object> CategoriesMap = new HashMap<String, Object>();
		rv.put("categories", CategoriesMap);
		loadCategories(gradebook, CategoriesMap, siteId, authenticatedUserId);

		return rv;
	}

	/**
	 * Preferences grading scale changes
	 * 
	 * @param req
	 * @param res
	 * @param parameters
	 * @param path
	 * @param authenticatedUserId
	 * @return
	 */
	protected Map<String, Object> dispatchGradeOptionsChangeGradingScale(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path, String authenticatedUserId)
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		String siteId = (String) parameters.get("siteId");
		if (siteId == null)
		{
			logger.warn("dispatchGradeOptionsChangeGradingScale - no siteId parameter");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		String gradingScaleIdParam = (String) parameters.get("gradingScaleId");

		int gradingScaleId = 0;
		try
		{
			gradingScaleId = Integer.parseInt(gradingScaleIdParam);
		}
		catch (NumberFormatException e)
		{
			logger.warn("dispatchGradeOptionsChangeGradingScale - error with grading sclae id parameter");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// get the gradebook and check for the selected grading scale
		Gradebook gradebook = GradebookService().getContextGradebook(siteId, authenticatedUserId);

		Map<String, Object> gradebookMap = new HashMap<String, Object>();
		rv.put("gradebook", gradebookMap);

		for (GradingScale gradingScale : gradebook.getContextGradingScales())
		{
			if (gradingScale.getId() == gradingScaleId)
			{
				gradebookMap.put("id", gradebook.getId());
				gradebookMap.put("context", gradebook.getContext());
				gradebookMap.put("sortCode", gradebook.getSortType().getCode());
				gradebookMap.put("showLetterGrade", gradebook.isShowLetterGrade() ? 1 : 0);

				loadSelectedGradingScale(gradebookMap, gradingScale);

				break;
			}
		}
		return rv;
	}

	/**
	 * Save release grades type
	 * 
	 * @param req
	 * @param res
	 * @param parameters
	 * @param path
	 * @param authenticatedUserId
	 * @return
	 */
	protected Map<String, Object> dispatchGradeOptionsOverAllGradesType(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path, String authenticatedUserId)
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// get the site id parameter
		String siteId = (String) parameters.get("siteId");

		if (siteId == null || siteId.trim().length() == 0)
		{
			return rv;
		}

		Boolean allowEdit = GradebookService().allowEditGradebook(siteId, authenticatedUserId);

		if (!allowEdit)
		{
			return rv;
		}

		ReleaseGrades releaseGrades = ReleaseGrades.Released;

		String releaseGradesTypeVal = (String) parameters.get("releaseGradesType");
		if (releaseGradesTypeVal == null) releaseGrades = null;
		if ("1".equalsIgnoreCase(releaseGradesTypeVal))	releaseGrades = ReleaseGrades.All;
	
		Boolean showLetterGrade = Boolean.FALSE;
		String showLetterGradesVal = (String) parameters.get("showLetterGrades");
		if (showLetterGradesVal == null) showLetterGrade = null;
		if ("1".equalsIgnoreCase(showLetterGradesVal)) showLetterGrade = Boolean.TRUE;
		
		Boolean dropLowest = Boolean.FALSE;
		String dropLowestVal = (String) parameters.get("setLowest");
		if (dropLowestVal == null) dropLowest = null;
		if ("1".equalsIgnoreCase(dropLowestVal) || "true".equalsIgnoreCase(dropLowestVal)) dropLowest = Boolean.TRUE;

		GradebookService().modifyContextGradebookAttributes(siteId, showLetterGrade, releaseGrades, dropLowest, authenticatedUserId);

		return rv;
	}

	/**
	 * 
	 * @param req
	 * @param res
	 * @param parameters
	 * @param path
	 * @param authenticatedUserId
	 * @return
	 */
	protected Map<String, Object> dispatchGradeOptionsSaveGradingScale(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path, String authenticatedUserId)
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		if (checkBadRequest(parameters, rv, authenticatedUserId)) return rv;

		// get the site id parameter
		String siteId = (String) parameters.get("siteId");
		
		String gradingScaleIdParam = (String) parameters.get("gradingScaleId");

		int gradingScaleId = 0;
		try
		{
			gradingScaleId = Integer.parseInt(gradingScaleIdParam);
		}
		catch (NumberFormatException e)
		{
			logger.warn("dispatchGradeOptionsSaveGradingScale - error with grading sclae id parameter");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		
		// set the changes
		Map<String, Float> gradingScalePercent = new HashMap<String, Float>();

		String prefix = "grade_scale_";
		int beginIndex = "grade_scale_".length();

		for (Map.Entry<String, Object> entry : parameters.entrySet())
		{
			if (entry.getKey().startsWith(prefix))
			{
				if (logger.isDebugEnabled())
				{
					logger.debug("Letter Grade: " + entry.getKey().substring(beginIndex));
				}

				if (entry.getKey().substring(beginIndex).length() > 0)
				{
					String scalePercentValue = null;
					if (entry.getValue() != null) scalePercentValue = ((String)entry.getValue()).trim();
					if (scalePercentValue != null && scalePercentValue.length() > 0)
					{
						gradingScalePercent.put(entry.getKey().substring(beginIndex), Float.valueOf(scalePercentValue));
					}
					else
					{
						gradingScalePercent.put(entry.getKey().substring(beginIndex), Float.valueOf(0.0f));
					}
				}
			}
		}
		
		// validate for existing overridden letter grade - if there are over ridden letter grades don't allow to change grading scale
		Gradebook gradebook = GradebookService().getContextGradebook(siteId, authenticatedUserId);		
		if (gradebook.getGradingScale().getId() != gradingScaleId)
		{
			int usersOverriddenGradesCount = GradebookService().getUsersGradesCount(siteId, authenticatedUserId);
			
			if (usersOverriddenGradesCount > 0)
			{
				rv.put("dataChange", "CanNotModify");
				
				return rv;
			}
			else
			{
				rv.put("dataChange", "CanBeModified");
			}
		}
				
		// TODO check for descending scores

		GradebookService().modifyContextGradebookGradingScale(siteId, gradingScaleId, gradingScalePercent, authenticatedUserId);

		return rv;
	}

	/**
	 * Save lowest drop numbers from dialog box.
	 * @param req
	 * @param res
	 * @param parameters
	 * @param path
	 * @param authenticatedUserId
	 * @return
	 */
	protected Map<String, Object> dispatchGradeOptionsSaveLowestDrop(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path, String authenticatedUserId)
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// get the site id parameter
		String siteId = (String) parameters.get("siteId");

		if (siteId == null || siteId.trim().length() == 0)
		{
			return rv;
		}

		Boolean allowEdit = GradebookService().allowEditGradebook(siteId, authenticatedUserId);

		if (!allowEdit)
		{
			return rv;
		}
		
		rv.put("callDropLowestFrom", parameters.containsKey("callDropLowestFrom") ? (String)parameters.get("callDropLowestFrom") : "options");

		List<GradebookCategory> gcsList = GradebookService().getContextGradebook(siteId, authenticatedUserId).getGradebookCategories();
		// Set categories
		String categoriesCount = (String) parameters.get("categoriesCount");
		if (categoriesCount != null && !categoriesCount.equals("undefined"))
		{
			List<GradebookCategory> categories = new ArrayList<GradebookCategory>();

			int categoriesLength = Integer.parseInt(categoriesCount);
			for (int i = 0; i < categoriesLength; i++)
			{
				String categoryId = StringUtil.trimToNull((String) parameters.get("categoryId" + i));
				int id = (categoryId == null || categoryId.equals("undefined")) ? 0 : Integer.parseInt(categoryId);

				String dropLowest = StringUtil.trimToNull((String) parameters.get("dropLowest" + i));
				int drop = (dropLowest == null || dropLowest.equals("undefined")) ? 0 : Integer.parseInt(dropLowest);

				GradebookCategory gc = findExistingCategory(id, gcsList);
				if (gc != null)
				{
					gc.setDropNumberLowestScores(drop);
					categories.add(gc);
				}				
			}			
			
			if (categories.size() > 0) GradebookService().modifyContextGradebookCategoriesDropLowestScoresNumber(siteId, categories, authenticatedUserId);
		}
		
		Boolean dropLowest = Boolean.FALSE;
		String dropLowestVal = (String) parameters.get("setLowest");
		if (dropLowestVal == null) dropLowest = null;
		if ("1".equalsIgnoreCase(dropLowestVal) || "true".equalsIgnoreCase(dropLowestVal)) dropLowest = Boolean.TRUE;

		GradebookService().modifyContextGradebookAttributes(siteId, null, null, dropLowest, authenticatedUserId);

		return rv;
	}

	/**
	 * Drill down details of an assessment. Finds information of all participants of an assessment.
	 * 
	 * @param req
	 * @param res
	 * @param parameters
	 * @param path
	 * @param authenticatedUserId
	 * @return
	 */
	protected Map<String, Object> dispatchItemOverview(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path, String authenticatedUserId)
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		if (checkBadRequest(parameters, rv, authenticatedUserId)) return rv;

		// get the site id parameter
		String siteId = (String) parameters.get("siteId");
		Site site = null;
		try
		{
			site = siteService().getSite(siteId);
			rv.put("siteTitle", site.getTitle());
		}
		catch (IdUnusedException e)
		{
		}

		rv.put("userRole", "instructor");
		rv.put("pageTitle", "Item Overview");

		// sort overview assessments for next/prev links		
		Map<String, Object> sortRv = new HashMap<String, Object>();
		findAssessmentsOverviewSort((String) parameters.get("sortOverviewPreference"), "asc", (String) parameters.get("lastOverviewSortData"), sortRv, siteId, authenticatedUserId);
		String sortOverviewPreference = (String)sortRv.get("sortPreference");
		String sortOverviewOrder = (String)sortRv.get("sortOrder");
		String lastOverviewSortPreference = (String)sortRv.get("lastSortData");
		
		// get all assessments
		Boolean sortType = ("asc".equals(sortOverviewOrder)) ? new Boolean("true") : new Boolean("false");

		List<GradebookItem> toolGradebookItems = GradebookService().getToolGradebookItems(siteId, authenticatedUserId, true, false, GradebookSortType.valueOf(sortOverviewPreference), sortType, null);
		rv.put("assessmentsLength", toolGradebookItems.size());
		rv.put("lastOverviewSortData", lastOverviewSortPreference);
		// get the id
		Map<String, Object> itemDetailsMap = new HashMap<String, Object>();
		String itemId = (String) parameters.get("itemId");

		// load sections
		String selectedSection = (String) parameters.get("selectedSection");
		if ("All".equalsIgnoreCase(selectedSection)) selectedSection = null;
		loadSections(siteId, selectedSection, rv);

		// fetch item details
		GradebookItem itemDetails = GradebookService().getGradebookItem(itemId, siteId, selectedSection, authenticatedUserId);
		rv.put("assessmentItem", itemDetailsMap);
		
		// loadParticipants
		String sortPreference = "Status";
		String sortOrder = "_a";

		String lastSortPreference = (String) parameters.get("lastSortData");
		if (lastSortPreference != null && !"null".equals(lastSortPreference))
		{
			String[] parts = StringUtil.split(lastSortPreference, ",");
			try
			{
				sortPreference = GradebookLastSort.valueOf("dcolumn" + parts[0]).showName();
				sortOrder = ("0".equals(parts[1])) ? "_a" : "_d";
			}
			catch (Exception e)
			{
				// do nothing..sort column not found go for default.
			}
		}
		else
		{
			lastSortPreference = "2,0";
		}
		rv.put("lastSortData", lastSortPreference);
		rv.put("sortPreference", sortPreference);
		rv.put("sortOrder", sortOrder);
		String sortBy = sortPreference + sortOrder;
		
		String bestSubmissionOnly = (String) parameters.get("showBestSubmissionOnly");
		bestSubmissionOnly = (bestSubmissionOnly == null || bestSubmissionOnly == "undefined") ? "true" : bestSubmissionOnly;
		
		//save notes
		modifyInstructorNotes(parameters, siteId, authenticatedUserId);
		
		//save score override 
		String gradesCount = (String) parameters.get("gradesOverrideCount");
		if (gradesCount != null && !gradesCount.equals("undefined") && !gradesCount.equals("0"))
		{
			String scoreLoadTime = StringUtil.trimToNull((String) parameters.get("loadTime0"));
			Date scoreLoadDateTime = (scoreLoadTime == null || scoreLoadTime.equals("undefined")) ? null : new Date(new Long(scoreLoadTime));
			
			List<ParticipantItemDetails> itemParticipants = findItemParticipants(siteId, itemDetails.getId(), selectedSection, authenticatedUserId, sortBy.toLowerCase(), !Boolean.valueOf(bestSubmissionOnly));
			modifyUserScores(parameters,"itemDetails" ,itemParticipants, null);	
			if (itemParticipants.size() > 0) GradebookService().modifyItemScores(siteId, itemDetails.getId(), itemDetails.getType(), itemParticipants, authenticatedUserId, scoreLoadDateTime);
			itemParticipants = null;
		}	

		// find item_id index
		int itemIndex = toolGradebookItems.indexOf(itemDetails);

		// check to find its next/prev
		String nextPrevAction = (String) parameters.get("nextPrevAction");
		if (nextPrevAction != null)
		{
			bestSubmissionOnly = "true";
			if ("Next".equalsIgnoreCase(nextPrevAction))
			{
				// if last item then circle back
				if (itemIndex >= (toolGradebookItems.size() - 1))
					itemIndex = 0;
				else
					itemIndex = itemIndex + 1;
				String next_id = toolGradebookItems.get(itemIndex).getId();
				itemDetails = GradebookService().getGradebookItem(next_id, siteId, selectedSection, authenticatedUserId);
			}
			if ("Prev".equalsIgnoreCase(nextPrevAction))
			{
				// if first item then circle back
				if (itemIndex <= 0)
					itemIndex = toolGradebookItems.size() - 1;
				else
					itemIndex = itemIndex - 1;
				String prev_id = toolGradebookItems.get(itemIndex).getId();
				itemDetails = GradebookService().getGradebookItem(prev_id, siteId, selectedSection, authenticatedUserId);
			}
		}

		rv.put("showBestSubmissionOnly", Boolean.valueOf(bestSubmissionOnly));
		
		if (itemDetails != null)
			rv.put("itemId", itemDetails.getId());
		else
			rv.put("itemId", itemId);

		String showCount = (itemIndex + 1) + " of " + toolGradebookItems.size();
		itemDetailsMap.put("showAssessmentCount", showCount);
		loadGradebookItem(itemDetails, itemDetailsMap, true);

		List<ParticipantItemDetails> itemParticipants = loadItemParticipants(siteId, itemDetails.getId(), selectedSection, authenticatedUserId, sortBy.toLowerCase(), Boolean.valueOf(bestSubmissionOnly), rv);

		String escapeSiteTitle = Validator.escapeResourceName((String)rv.get("siteTitle"));
		rv.put("exportFileName", escapeSiteTitle + "_" + itemDetails.getTitle() + "_grades");

		// if call from grades view 2 then resend the params
		String callFromPage = (String) parameters.get("callFromPage");
		if (callFromPage != null)
		{
			rv.put("callFromPage", callFromPage);
			rv.put("lastGradesSortData", (String) parameters.get("lastGradesSortData"));
			rv.put("gradesAssessmentType", (String) parameters.get("gradesAssessmentType"));
			rv.put("gradesSection", (String) parameters.get("gradesSection"));
		}
		return rv;
	}
	
	/**
	 * Called from Assessments mode. Fetches all assessment items with overview data.
	 * 
	 * @param req
	 * @param res
	 * @param parameters
	 * @param path
	 * @param authenticatedUserId
	 * @return
	 */
	protected Map<String, Object> dispatchOverview(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path, String authenticatedUserId)
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// check user
		if (checkBadRequest(parameters, rv, authenticatedUserId)) return rv;

		// get the site id parameter
		String siteId = (String) parameters.get("siteId");
		String lastSortPreference = ((String) parameters.get("lastOverviewSortData") != null) ? (String) parameters.get("lastOverviewSortData") :(String) parameters.get("lastSortData");
	//	String lastSortPreference = (String) parameters.get("lastSortData");
		
		// general properties
		rv.put("userRole", "instructor");
		rv.put("title", "Etudes Gradebook");
		rv.put("pageTitle", "Assessments");

		// redirect Student page
		if (path.indexOf("indvidualStudentGrades") != -1)
		{
			String[] parts = path.split("/");
			if (parts != null && parts.length > 2)
			{					
				if (parts[3] != null && !"undefined".equals(parts[3].trim()))rv.put("studentId", parts[3].trim());
				if (parts[4] != null && !"undefined".equals(parts[4].trim()))rv.put("viewAssessmentGrades", parts[4].trim());
				if (parts[5] != null && !"undefined".equals(parts[5].trim()))rv.put("lastGradeSortData", parts[5].trim());
				if (parts[6] != null && !"undefined".equals(parts[6].trim()))rv.put("selectedSection", parts[6].trim());
				if (parts[7] != null && !"undefined".equals(parts[7].trim()))rv.put("selectedType", parts[7].trim());
				rv.put("redirectStudentGrade", "yes");
			}
		}
		
		// redirect assessment detail page
		if (path.indexOf("assessmentDetails") != -1)
		{
			String[] parts = path.split("/");
			if (parts != null && parts.length > 2)
			{					
				if (parts[4] != null && !"undefined".equals(parts[4].trim()))rv.put("itemId", parts[4].trim());
			
				if (parts[5] != null && !"undefined".equals(parts[5].trim()))
				{
					lastSortPreference = parts[5].trim();
					rv.put("lastOverviewSortData", parts[5].trim());
				}
				if (parts[6] != null && !"undefined".equals(parts[6].trim()))rv.put("lastItemDetailsSortData", parts[6].trim());
				if (parts[7] != null && !"undefined".equals(parts[7].trim()))rv.put("selectedSection", parts[7].trim());
				if (parts[8] != null && !"undefined".equals(parts[8].trim()))rv.put("callFromPage", parts[8].trim());
				rv.put("redirectStudentGrade", "yes");
			}
		}
				
		// get the sort preference
		Map<String, Object> sortRv = new HashMap<String, Object>();
		findAssessmentsOverviewSort((String) parameters.get("sortPreference"), "asc", lastSortPreference, sortRv, siteId, authenticatedUserId);
		String sortPreference = (String)sortRv.get("sortPreference");
		String sortOrder = (String)sortRv.get("sortOrder");
		lastSortPreference = (String)sortRv.get("lastSortData");

		rv.put("sortPreference", sortPreference.replace("Date", ""));
		rv.put("sortOrder", sortOrder);
		rv.put("lastSortData", lastSortPreference);

		Boolean sortType = ("asc".equals(sortOrder)) ? new Boolean("true") : new Boolean("false");
		List<GradebookItem> toolGradebookItems = GradebookService().getToolGradebookItems(siteId, authenticatedUserId, false, false, GradebookSortType.valueOf(sortPreference), sortType, null);
		Gradebook gradebook = GradebookService().getContextGradebook(siteId, authenticatedUserId);

		//save reorder map
		saveAssessmentsMap(siteId, authenticatedUserId, parameters, gradebook, toolGradebookItems);
			
		// get all gradeable items
		List<Map<String, Object>> allAssessmentsList = new ArrayList<Map<String, Object>>();
		rv.put("assessments", allAssessmentsList);
			rv.put("assessmentsLength", (toolGradebookItems != null) ? toolGradebookItems.size() : 0);
		
		if ("Category".equalsIgnoreCase(sortPreference))
		{
			// mix categories and tool items			
			Map<GradebookCategory, List<GradebookItem>> assessmentMap = GradebookService().getToolGradebookItems(siteId, authenticatedUserId, true, false); 
			loadCategoriesAndGradebookItem(assessmentMap, allAssessmentsList);		
		}
		else
		{
			toolGradebookItems = GradebookService().getToolGradebookItems(siteId, authenticatedUserId, true, false, GradebookSortType.valueOf(sortPreference), sortType, null);
			for (GradebookItem gradebookItem : toolGradebookItems)
			{	
				Map<String, Object> gradebookItemMap = new HashMap<String, Object>();
				allAssessmentsList.add(gradebookItemMap);
				loadGradebookItem(gradebookItem, gradebookItemMap, false);			
			}
		}
		
		//for categories edit
		gradebook = GradebookService().getContextGradebook(siteId, authenticatedUserId);
		Map<String, Object> CategoriesMap = new HashMap<String, Object>();
		rv.put("categories", CategoriesMap);
		//fetch that type's categories
		List<GradebookCategory> categories = gradebook.getGradebookCategories();
		loadCategories(gradebook, CategoriesMap, siteId, authenticatedUserId);
		
		getClassPoints(siteId, rv, authenticatedUserId);
		getCategoryWeightTotal(categories, rv);
		
		rv.put("releasedGrades", gradebook.getReleaseGrades().getCode());
				
		return rv;
	}

	/**
	 * Saves user grades boosted data
	 * 
	 * @param req		Request
	 * 
	 * @param res		Response
	 * 
	 * @param parameters	Parameters
	 * 
	 * @param path			path
	 * 
	 * @param authenticatedUserId	Authenticated user id
	 * 
	 * @return	Map of data
	 */
	protected Map<String, Object> dispatchSaveBoostGrades(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path, String authenticatedUserId)
	{
		Map<String, Object> rv = new HashMap<String, Object>();
		
		String siteId = (String) parameters.get("siteId");
		
		String boostGradesNumber = StringUtil.trimToNull((String) parameters.get("boostNumber"));
		String boostGradesType = StringUtil.trimToNull((String) parameters.get("boostType"));
		
		Float boostUserGradesBy = null;
		
		
			BoostUserGradesType boostUserGradesType = null;
			if (boostGradesType != null)
			{
				if (boostGradesType.equals("1"))
				{
					boostUserGradesType = BoostUserGradesType.points;
				}
				else if (boostGradesType.equals("2"))
				{
					boostUserGradesType = BoostUserGradesType.percent;
				}
			}
			if (boostGradesNumber != null)
			{
				try
				{
					boostUserGradesBy = Float.parseFloat(boostGradesNumber);
				}
				catch (NumberFormatException e)
				{					
				}
			}
			GradebookService().modifyContextGradebookBoostByAttributes(siteId, boostUserGradesType, boostUserGradesBy, authenticatedUserId);
		
		return rv;
	}

	/**
	 * Called from Student Grades mode. Fetches all enrolled/dropped students with their overall score.
	 * 
	 * @param req
	 * @param res
	 * @param parameters
	 * @param path
	 * @param authenticatedUserId
	 * @return
	 */
	protected Map<String, Object> dispatchStudentsOverallGrades(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path, String authenticatedUserId)
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		if (checkBadRequest(parameters, rv, authenticatedUserId)) return rv;

		// get the site id parameter
		String siteId = (String) parameters.get("siteId");

		Site site = null;
		try
		{
			site = siteService().getSite(siteId);
			rv.put("siteTitle", site.getTitle());
		}
		catch (IdUnusedException e)
		{
		}

		// general properties
		rv.put("userRole", "instructor");
		rv.put("title", "Etudes Gradebook");
		rv.put("pageTitle", "Students Grades");

		ParticipantSort sort = findStudentGradesSort(parameters, rv);
		
		Gradebook gradebook = GradebookService().getContextGradebook(siteId, authenticatedUserId);
		Integer overallOptions = gradebook.getReleaseGrades().getCode();
		rv.put("overallCalculationOptions", (overallOptions.intValue() == 2) ? "released assessments only" : "all assessments");
		
		// boost user grades by
		rv.put("boostUserGradesBy", (gradebook.getBoostUserGradesBy() == null || gradebook.getBoostUserGradesBy() <= 0) ? "" : gradebook.getBoostUserGradesBy());
		rv.put("boostUserGradesTypeCode", (gradebook.getBoostUserGradesBy() == null || gradebook.getBoostUserGradesBy() <= 0) ? "" : gradebook.getBoostUserGradesType().getCode());
		
		//low score drop
		List<GradebookCategory> findDrops = gradebook.getGradebookCategories();
		
		int dropLowestNumber = 0;
		
		if (findDrops != null)
		{			
			for (GradebookCategory g: findDrops)
				dropLowestNumber += g.getDropNumberLowestScores();				
		}
		rv.put("dropLowestNumber", dropLowestNumber);
		
		Map<String, Object> CategoriesMap = new HashMap<String, Object>();
		rv.put("categories", CategoriesMap);
		loadCategories(gradebook, CategoriesMap, siteId, authenticatedUserId);
		rv.put("dropScore", gradebook.isDropLowestScore());
		rv.put("showWeight", categoryHasWeights(findDrops));
		getCategoryWeightTotal(findDrops, rv);
				
		//extra credit column shows
		boolean showExtraCredit = false;
		for (GradebookCategory findec: findDrops)
		{
			if (findec.isExtraCredit() && findec.getItemCount() > 0) 
			{
				showExtraCredit = true;
				break;
			}
		}
		rv.put("showExtraCredit", showExtraCredit);
		
		//save notes
		modifyInstructorNotes(parameters, siteId, authenticatedUserId);
		
		// Grades override
		String gradesCount = (String) parameters.get("gradesOverrideCount");
		if (gradesCount != null && !gradesCount.equals("undefined") && !gradesCount.equals("0"))
		{
			List<UserGrade> userLetterGrades = new ArrayList<UserGrade>();

			int gradesLength = Integer.parseInt(gradesCount);
			for (int i = 0; i < gradesLength; i++)
			{
				String newGradeFound = StringUtil.trimToNull((String) parameters.get("newGrade" + i));
				newGradeFound = (newGradeFound == null || newGradeFound.equals("undefined")) ? null : newGradeFound;

				String gradeLoadTime = StringUtil.trimToNull((String) parameters.get("loadTime" + i));
				Date gradeLoadDateTime = (gradeLoadTime == null || gradeLoadTime.equals("undefined")) ? null : new Date(new Long(gradeLoadTime));

				String studentId = StringUtil.trimToNull((String) parameters.get("user" + i));
				studentId = (studentId == null || studentId.equals("undefined")) ? null : studentId;
				
				if (studentId != null)
				{
				UserGrade ug = GradebookService().newUserGrade( studentId, newGradeFound, gradeLoadDateTime);
				userLetterGrades.add(ug);
				}
			}
			if (userLetterGrades.size() > 0) GradebookService().modifyUserGrades(siteId, userLetterGrades, authenticatedUserId);
		}

		// all sections
		String selectedSection = (String) parameters.get("selectedSection");
		if ("All".equalsIgnoreCase(selectedSection) || "undefined".equalsIgnoreCase(selectedSection)) selectedSection = null;
		loadSections(siteId, selectedSection, rv);

		// assessmentType filter
		String selectedType = (String) parameters.get("selectedType");
		if ("All".equalsIgnoreCase(selectedType) || "undefined".equalsIgnoreCase(selectedType)) selectedType = null;
		GradebookItemType selectedItemType = getSelectedGradebookItemType(selectedType);
	
		rv.put("selectedType", selectedType);

		// fetch assessment for headers
		List<Map<String, Object>> assessmentColHeaders = new ArrayList<Map<String, Object>>();
		String viewAssessments = (String) parameters.get("viewAssessmentGrades");
		if ("2".equals(viewAssessments))
		{
			rv.put("viewAssessmentGrades", "2");

			// assessmentType filter
			loadAssessmentTypes(selectedType, rv);

			// fixed headers
			String fixedHeaders[] =	{ "Name", "", "", "Section", "Status", "Score", "Out of"};
			rv.put("view2FixedHeaders", fixedHeaders);

			// collect all assessment names
			List<GradebookItem> allAssessments = GradebookService().getToolGradebookItems(siteId, authenticatedUserId, false, false, null, null, selectedItemType);
			for (GradebookItem a : allAssessments)
			{
				Map<String, Object> headerMap = new HashMap<String, Object>();
				headerMap.put("id", a.getId());
				headerMap.put("title", a.getTitle());
				headerMap.put("points", "(" + a.getPoints() + ")");
				assessmentColHeaders.add(headerMap);
			}
			rv.put("assessmentColHeaders", assessmentColHeaders);
		}

		// fetch all students
		List<Map<String, Object>> participants = new ArrayList<Map<String, Object>>();
		rv.put("participants", participants);
		fetchParticipantsForStudentGradesView(siteId, participants, sort, selectedItemType, viewAssessments, selectedSection, authenticatedUserId);

		// class avg
		getClassPoints(siteId, rv, authenticatedUserId);
				
		// export csv
		String escapeSiteTitle = Validator.escapeResourceName((String)rv.get("siteTitle"));
		String exportFileName = ("2".equals(viewAssessments)) ?  escapeSiteTitle + "_detailed_overall_grades" : escapeSiteTitle + "_overall_grades";
		rv.put("exportFileName", exportFileName);

		return rv;
	}

	/**
	 * Student Landing page
	 * 
	 * @param req
	 * @param res
	 * @param parameters
	 * @param path
	 * @param authenticatedUserId
	 * @return
	 */
	protected Map<String, Object> dispatchUserOverview(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path, String authenticatedUserId, boolean isInstructor, Map<String, Object> rv)
	{
		if (rv == null) rv = new HashMap<String, Object>();

		if (checkBadRequest(parameters, rv, authenticatedUserId)) return rv;

		// get the site id parameter
		String siteId = (String) parameters.get("siteId");

		if (!isInstructor) rv.put("userRole", "student");
		rv.put("userId", authenticatedUserId);

		User user = null;
		try
		{
			user = UserDirectoryService().getUser(authenticatedUserId);
			rv.put("userName", user.getLastName() + ", " + user.getFirstName());
		}
		catch (UserNotDefinedException e)
		{
			rv.put("userName", "");
		}

		if (user == null)
		{
			logger.warn("dispatchItemOverview - user not defined");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// if from indv student summary
		String studentId = (String) parameters.get("studentId");
		Participant student = null;
		if (studentId != null)
		{
			rv.put("studentId", studentId);
			student = GradebookService().getParticipant(siteId, studentId, true);
			rv.put("studentName", (student != null) ? student.getSortName() : "");
			rv.put("studentIid", (student != null) ? student.getDisplayId() : "");
			rv.put("studentStatus", (student != null) ? student.getStatus().getDisplayName() : "");
			// instructor notes on a student
			if (student.getInstructorNotes() != null) rv.put("notes", student.getInstructorNotes().getNotes());
			fillNotesHistory(rv, student.getInstructorNotes(), authenticatedUserId);
			// private message link		
			if (student.getPrivateMessageLink() != null) rv.put("sendPMLink", student.getPrivateMessageLink());
		}
		String studentUserId = (studentId != null) ? studentId : authenticatedUserId;
		List<Map<String, Object>> userGradebookList = new ArrayList<Map<String, Object>>();
		rv.put("userGradebookList", userGradebookList);

		Gradebook gradebookPrefs = GradebookService().getContextGradebook(siteId, authenticatedUserId);
		List<GradebookCategory> categories = gradebookPrefs.getGradebookCategories();
		Map<GradebookCategory, List<GradebookItem>> assessmentMap = GradebookService().getToolGradebookItems(siteId, authenticatedUserId, false, false);
			
		// Get user's gradable items and sort by due date asc
		String bestSubmissionOnly = (String) parameters.get("showBestSubmissionOnly");
		bestSubmissionOnly = (bestSubmissionOnly == null || bestSubmissionOnly == "undefined") ? "true" : bestSubmissionOnly;
		List<ParticipantGradebookItem> userGradebookItems = GradebookService().getUserToolGradebookItems(siteId, studentUserId, authenticatedUserId, !Boolean.valueOf(bestSubmissionOnly)); 
		
		//show inprogress score to instructors and not to students
		boolean addInProgressScore = (studentId == null) ? false : true;
		
		// mix categories and tool items
		loadCategoriesAndUserGradebookItem(addInProgressScore, assessmentMap, categories, userGradebookItems, userGradebookList);
	
		// check preference for showing total score or alphabet	
		boolean showGrade = gradebookPrefs.isShowLetterGrade();
		rv.put("releasedGrades", gradebookPrefs.getReleaseGrades().getCode());
		String sortPreference = gradebookPrefs.getSortType().name();
		rv.put("sortTypePreference", sortPreference);
		if (parameters.get("viewAssessmentGrades") != null) rv.put("viewAssessmentGrades", (String) parameters.get("viewAssessmentGrades"));
		if (parameters.get("lastGradeSortData") != null) rv.put("lastGradeSortData", (String) parameters.get("lastGradeSortData"));		
		if (parameters.get("lastSortData") != null) rv.put("lastSortData", (String) parameters.get("lastSortData"));
	
		// Total points and avg for a student
		UserGrade userGrade = GradebookService().getUserGrade(siteId, studentUserId);
		
		Float totalScore = (userGrade != null) ? userGrade.getScore() : null;	
		Float totalPoints = (userGrade != null) ? userGrade.getPoints() : new Float(0.0f);
		String letterGrade = (userGrade != null) ? userGrade.getLetterGrade() : null;
		Float averagePercent = (userGrade != null) ? userGrade.getAveragePercent() : null;
		Float extraCreditScore = (userGrade != null) ? userGrade.getExtraCreditScore() : new Float(0.0f);
		
		// if option is to show grade then 98%/A+ else just percentage  
		if (letterGrade != null && averagePercent != null)
		{
			letterGrade = (showGrade) ? averagePercent + "%/" + letterGrade : averagePercent + "%";
		}
		
		//calculate how much of course is graded
		Float totalCategoryPoints = new Float(0.0f);
		Float toGradePercentage = new Float(0.0f);
		if (userGrade != null)
		{
			List<UserCategoryGrade> allGrades = userGrade.getUserCategoryGrade();
			for (UserCategoryGrade ucg : allGrades)
			{
				if (ucg.getGradebookCategory() != null && !ucg.getGradebookCategory().isExtraCredit() && ucg.getGradebookCategory().getItemsTotalpoints() != null) 
				  totalCategoryPoints += ucg.getGradebookCategory().getItemsTotalpoints();
			}
			if (totalCategoryPoints != null && totalCategoryPoints > 0 && totalPoints != null && totalPoints > 0)
			{
				toGradePercentage = totalPoints / totalCategoryPoints;
				toGradePercentage *= 100;	
				toGradePercentage = extract2DecimalPlaces(toGradePercentage);
			}
		}
		rv.put("toGradePercentage", (toGradePercentage > 0) ? toGradePercentage.toString() + "%" : "-");
		rv.put("totalCategoryPoints", totalCategoryPoints);
		rv.put("letterGrade", letterGrade);
		
		//total points 
		if (totalPoints == null || "null".equals(totalPoints) || totalPoints == 0.0)
			rv.put("totalPoints", "0");
		else
			rv.put("totalPoints",  totalPoints);
					
		rv.put("averageScore", (averagePercent == null) ? "-" : String.valueOf(averagePercent.floatValue()) + "%");
		
		StringBuffer pointsText = new StringBuffer("Not Available");
		if (totalPoints != null && totalPoints != 0)
		{
			pointsText = new StringBuffer(((totalScore == null) ? "-" : totalScore ) + " out of a possible " + totalPoints);
			pointsText = pointsText.append(new Integer(1).equals(gradebookPrefs.getReleaseGrades().getCode()) ? " points for the entire course" : " points graded and released");
			
			if (extraCreditScore != null && extraCreditScore > 0) pointsText = pointsText.append(", plus " + extraCreditScore +" extra credit points.");
			else pointsText = pointsText.append(".");
		}
			
		rv.put("pointsText", pointsText.toString());
		
		String pointsHeaderText = (new Integer(1).equals(gradebookPrefs.getReleaseGrades().getCode())) ? "Total Score:" : "Grade to Date:";
		rv.put("pointsHeaderText", pointsHeaderText);
		
		// export csv for instructor's view of indv student summary
		if (student != null)
		{
			rv.put("exportIndvStudentFileName", student.getSortName() + "_grades");
		}

		return rv;
	}

	/**
	 * If the value contains the separator, escape it with quotes.
	 * 
	 * @param value
	 *        The value to escape.
	 * @param separator
	 *        The separator.
	 * @return The escaped value.
	 */
	protected String escape(String value, String separator)
	{
		if (value == null) return value;
		
		// any quotes in here need to be doubled
		value = value.replaceAll("\""," ");

		return value;
	}

	/**
	 * Format a date for inclusion in the csv file
	 * 
	 * @param date
	 *        The date.
	 * @return The formatted date.
	 */
	protected String exportFormatDate(Date date)
	{
		if (date == null) return "-";
		DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.US);
		return format.format(date);
	}

	/**
	 * Format an Float for inclusion in the csv.
	 * 
	 * @param value
	 *        The value.
	 * @return The value formatted.
	 */
	protected String exportFormatFloat(Float value)
	{
		if (value == null) return "-";
		return value.toString();
	}

	/**
	 * Format a String for inclusion in the csv.
	 * 
	 * @param value
	 *        The value.
	 * @return The value formatted.
	 */
	protected String exportFormatString(String value)
	{
		if (value == null) return "-";
		value = escape(value,",");
		return value;
	}

	/**
	 * 
	 * @param number
	 * @return
	 */	
	protected float extract2DecimalPlaces(Float number)
	{
		if (number == null) return 0;
		BigDecimal bd = new BigDecimal(Float.toString(number));
		bd = bd.setScale(2, BigDecimal.ROUND_UP);
		return bd.floatValue();
	}
	
	/**
	 * Fetch participants for students grade views
	 * 
	 * @param siteId
	 * @param participants
	 * @param viewAssessments
	 * @param selectedSection
	 * @param authenticatedUserId
	 * @return
	 */
	protected Collection<Participant> fetchParticipantsForStudentGradesView(String siteId, List<Map<String, Object>> participants,
			ParticipantSort sort, GradebookItemType selectedType, String viewAssessments, String selectedSection, String authenticatedUserId)
	{
		Collection<Participant> filterParticipants = null;
		Map<Participant, List<ParticipantGradebookItem>> all = null;
		// view overall grade
		if (viewAssessments == null || viewAssessments.equals("undefined"))
		{
			filterParticipants = GradebookService().getUsersGradebookSummary(siteId, selectedSection, authenticatedUserId, sort);
		}
		else
		{
			// view 2 with all assessments
			all = GradebookService().getUsersGradebookSummaryAndGradeBookItems(siteId, selectedSection, authenticatedUserId, sort, selectedType);
			filterParticipants = all.keySet();
		}

		for (Participant participant : filterParticipants)
		{
			Map<String, Object> ParticipantItemDetailsMap = new HashMap<String, Object>();
			participants.add(ParticipantItemDetailsMap);
			loadParticipant(ParticipantItemDetailsMap, participant, authenticatedUserId);
			if ("2".equals(viewAssessments))
			{
				List<ParticipantGradebookItem> gbs = all.get(participant);
				for (ParticipantGradebookItem pg : gbs)
				{				    
					String score = "-";
					if (pg.getParticipantItemDetails() != null && pg.getParticipantItemDetails().getScore() != null)
						score = Float.toString(pg.getParticipantItemDetails().getScore());
					
					if (pg.getParticipantItemDetails() != null && pg.getParticipantItemDetails().getIsScoreDropped())
						score = "*" + score + "*";
					
					ParticipantItemDetailsMap.put(pg.getGradebookItem().getId(), score);
				}
			}
		}
		return filterParticipants;
	}

	/**
	 * Get notes made by history
	 * @param notes
	 * @param instructorId
	 * @return
	 */
	protected void fillNotesHistory(Map<String, Object> rv, Notes notes, String instructorId)
	{
		// from history last updated
		String instructorNotesBy = "Notes added";
		long showTime = new Date().getTime();
		long notesDate = 0;
		
		// if note has modified date then updated by else added by information
		if (notes != null && notes.getNotes() != null && notes.getNotes().length() > 0)
		{
			instructorNotesBy = (notes.getDateModified() != null) ? "Notes updated" : "Notes added";
			showTime = (notes.getDateModified() != null) ? notes.getDateModified().getTime() : notes.getDateAdded().getTime();
			instructorId = (notes.getModifiedByUserId() != null) ? notes.getModifiedByUserId() : notes.getAddedByUserId();
			notesDate = showTime;
		}
		
		String userInfo = null;
		try
		{
			User user = UserDirectoryService().getUser(instructorId);
			userInfo = user.getDisplayName();
		}
		catch (UserNotDefinedException e)
		{
			userInfo = null;
		}
		
		if(userInfo != null) instructorNotesBy = instructorNotesBy.concat(" by " + userInfo);
		instructorNotesBy = instructorNotesBy.concat(" on " + CdpResponseHelper.dateTimeDisplayInUserZone(showTime));
		
		rv.put("notesLog", instructorNotesBy);
		rv.put("notesDate", Long.toString(notesDate));
	}
	
	/**
	 * 
	 * @param ParticipantItemDetailsMap
	 * @param participant
	 */
	protected void fillLogHistoryDetails(Map<String, Object> ParticipantItemDetailsMap, Participant participant)
	{
		// from history last updated
		UserGrade userGradeLog = participant.getOverriddenLetterGradeLog();
		long overrideTime = (userGradeLog != null) ? userGradeLog.getAssignedDate().getTime() : 0;
		String byId = (userGradeLog != null) ? userGradeLog.getAssignedByUserId() : "";
		String gradeHistory = (userGradeLog != null) ? userGradeLog.getLetterGrade() : null;

		// current grade
		UserGrade userGrade = participant.getOverriddenLetterGrade();
		String currentGrade = (userGrade != null) ? userGrade.getLetterGrade() : null;
		if (userGrade != null)
		{
			overrideTime = userGrade.getAssignedDate().getTime();
			byId = userGrade.getAssignedByUserId();
		}

		if (userGrade == null && userGradeLog == null) return;

		// add history info to ParticipantItemDetailsMap
		List<Map<String, String>> logComments = new ArrayList<Map<String, String>>();
		HashMap<String, String> comments = new HashMap<String, String>();
		logComments.add(comments);
		comments.put("overrideTime", CdpResponseHelper.dateTimeDisplayInUserZone(overrideTime));
		ParticipantItemDetailsMap.put("loadTime", Long.toString(overrideTime));
		String userInfo = null;
		try
		{
			User user = UserDirectoryService().getUser(byId);
			userInfo = user.getDisplayName();
		}
		catch (UserNotDefinedException e)
		{
			userInfo = "Unknown User";
		}

		// newly set
		if (gradeHistory == null && currentGrade != null)
		{
			comments.put("newGradeActionTitle", " Grade Set ");
			comments.put("newGradeInfo", " to " + currentGrade + " by " + userInfo);
		}
		// modified again
		if (gradeHistory != null && currentGrade != null)
		{
			comments.put("newGradeActionTitle", " Grade Changed ");
			comments.put("newGradeInfo", "from " + gradeHistory + " to " + currentGrade + " by " + userInfo);
		}
		// when grade is removed
		if (currentGrade == null && gradeHistory != null)
		{
			comments.put("newGradeActionTitle", " Grade Override Removed ");
			comments.put("newGradeInfo", " from " + userGradeLog.getLetterGrade() + " by " + userInfo);
		}
		ParticipantItemDetailsMap.put("logComment", logComments);
	}
	
	/**
	 * 
	 * @param sortOverviewPreference
	 * @param sortOverviewOrder
	 * @param lastOverviewSortPreference
	 * @param rv
	 * @param siteId
	 * @param authenticatedUserId
	 */
	protected void findAssessmentsOverviewSort(String sortPreference, String sortOrder, String lastSortPreference, Map<String, Object> sortRv, String siteId, String authenticatedUserId)
	{
		if (lastSortPreference != null && !"null".equals(lastSortPreference) && !"undefined".equals(lastSortPreference))
		{
			String[] parts = StringUtil.split(lastSortPreference, ",");
			try
			{
				sortPreference = GradebookLastSort.valueOf("column" + parts[0]).showName();
				sortOrder = ("0".equals(parts[1])) ? "asc" : "desc";
			}
			catch (Exception e)
			{
				sortPreference = null;
			}
		}
		
		// no longer need sorting preference. by default its Type
		
		if (sortPreference == null || "null".equals(sortPreference) || "undefined".equals(sortPreference))
		{
			sortPreference = "Category";
			lastSortPreference = "0,0";
			sortOrder = "asc";
		}
		
		sortRv.put("lastSortData", lastSortPreference);
		sortRv.put("sortPreference", sortPreference);
		sortRv.put("sortOrder", sortOrder);
	}
	
	/**
	 * 
	 * @param id
	 * @param gcsList
	 * @return
	 */
	protected GradebookCategory findExistingCategory(int id, List<GradebookCategory> gcsList)
	{
		GradebookCategory found = null;
		
		for (GradebookCategory gc: gcsList)
		{
			if (gc.getId() == id) return gc;				
		}
		return found;
	}

	/**
	 * Find the participants of an item
	 * @param context
	 * @param itemId
	 * @param groupId
	 * @param userId
	 * @param sortby
	 * @param bestSubmission
	 * @return
	 */
	protected List<ParticipantItemDetails> findItemParticipants(String context, String itemId, String groupId, String userId, String sortby, boolean bestSubmission)
	{
		List<ParticipantItemDetails> itemParticipants = null;
	
		if (itemId.startsWith("MNEME-"))
			itemParticipants = this.GradebookService().getMnemeItemDetails(context, itemId, groupId, ParticipantMnemeItemDetailsSort.valueOf(sortby), userId, bestSubmission);
		else
			itemParticipants = this.GradebookService().getJforumItemDetails(context, itemId, groupId, ParticipantJforumItemDetailsSort.valueOf(sortby), userId, bestSubmission);
			
		return itemParticipants;
	}

	/**
	 * 
	 * @param parameters
	 * @param authenticatedUserId
	 * @param rv
	 * @return
	 */
	protected Map<String, Object> findParticipantIndex(Map<String, Object> parameters, String authenticatedUserId, Map<String, Object> rv)
	{
		ParticipantSort sort = findStudentGradesSort(parameters, rv);
		String siteId = (String) parameters.get("siteId");
		String selectedSection = (String) parameters.get("selectedSection");
		if ("All".equalsIgnoreCase(selectedSection)) selectedSection = null;
		if (selectedSection != null) rv.put("selectedSection", selectedSection);
		String selectedType = (String) parameters.get("selectedType");
		if (selectedType != null) rv.put("selectedType", selectedType);

		List<Participant> filterParticipants = GradebookService().getUsersGradebookSummary(siteId, selectedSection, authenticatedUserId, sort);
		String studentId = (String) parameters.get("studentId");
		String showStudentIndex = "1 of 1";
		int index = 0;
		for (Participant p : filterParticipants)
		{
			if (p.getUserId().equals(studentId))
				break;
			else
				index = index + 1;
		}
		
		//save notes
		modifyInstructorNotes(parameters, siteId, authenticatedUserId);
		
		//save score override 
		String bestSubmissionOnly = (String) parameters.get("showBestSubmissionOnly");
		bestSubmissionOnly = (bestSubmissionOnly == null || bestSubmissionOnly == "undefined") ? "true" : bestSubmissionOnly;
		String gradesCount = (String) parameters.get("gradesOverrideCount");
		if (gradesCount != null && !gradesCount.equals("undefined") && !gradesCount.equals("0"))
		{
			String scoreLoadTime = StringUtil.trimToNull((String) parameters.get("loadTime0"));
			Date scoreLoadDateTime = (scoreLoadTime == null || scoreLoadTime.equals("undefined")) ? null : new Date(new Long(scoreLoadTime));
			
			List<ParticipantGradebookItem> userGradebookItems = GradebookService().getUserToolGradebookItems(siteId, studentId, authenticatedUserId, !Boolean.valueOf(bestSubmissionOnly));
			modifyUserScores(parameters, "studentSummary", null, userGradebookItems);
			if (userGradebookItems.size() > 0) GradebookService().modifyUserScores(siteId, studentId, userGradebookItems, authenticatedUserId, scoreLoadDateTime);
			userGradebookItems = null;
		}	
		
		String nextPrevAction = (String) parameters.get("prevNextAction");
		if (nextPrevAction != null)
		{
			bestSubmissionOnly = "true";
			if ("Next".equals(nextPrevAction))
			{
				if (index >= (filterParticipants.size() - 1))
					index = 0;
				else
					index = index + 1;
			}

			if ("Prev".equals(nextPrevAction))
			{
				// if first item then circle back
				if (index <= 0)
					index = filterParticipants.size() - 1;
				else
					index = index - 1;
			}
			studentId = filterParticipants.get(index).getUserId();
			parameters.put("studentId", studentId);
		}
		showStudentIndex = (index + 1) + " of " + filterParticipants.size();
		rv.put("showStudentIndex", showStudentIndex);
		
		rv.put("showBestSubmissionOnly", Boolean.valueOf(bestSubmissionOnly));
		return rv;
	}

	/**
	 * 
	 * @param parameters
	 * @param rv
	 * @return
	 */
	protected ParticipantSort findStudentGradesSort(Map<String, Object> parameters, Map<String, Object> rv)
	{
		String sortPreference = "Status";
		String sortOrder = "asc";
		ParticipantSort sort = ParticipantSort.status_a;
		String lastSortPreference = (String) parameters.get("lastSortData");
		if (lastSortPreference != null && !"null".equals(lastSortPreference))
		{
			String[] parts = StringUtil.split(lastSortPreference, ",");
			try
			{
				sortPreference = GradebookLastSort.valueOf("gcolumn" + parts[0]).showName();
				sortOrder = ("0".equals(parts[1])) ? "asc" : "desc";
			}
			catch (Exception e)
			{
				sortPreference = "Status";
			}
			sort = getParticipantSort(sortPreference, sortOrder);
		}
		else
			lastSortPreference = "2,0";
		rv.put("sortPreference", sortPreference);
		rv.put("sortOrder", sortOrder);
		rv.put("lastSortData", lastSortPreference);
		return sort;
	}
	
	/**
	 * Calculate total Weight of Categories
	 * @param categories
	 * @param rv
	 */
	protected void getCategoryWeightTotal(List<GradebookCategory> categories, Map<String, Object> rv)
	{
		Float total = 0.0f;
		for (GradebookCategory c : categories)
		{
			if (c.getWeight() != null) total += c.getWeight();
		}
		rv.put("totalMaxWeight", (total != 0.0f) ? total : "-");
	}
	
	/**
	 * 
	 * @param siteId
	 * @param rv
	 * @param authenticatedUserId
	 */
	protected void getClassPoints(String siteId, Map<String, Object> rv, String authenticatedUserId)
	{
		Map<String, Float> classPoints = GradebookService().getClassPointsAverage(siteId, authenticatedUserId);
		Float classpartPoints = (classPoints.get("nonExtraCreditCategoriesTotalPoints") != null) ? classPoints.get("nonExtraCreditCategoriesTotalPoints") : null;
		Float extrapartPoints = (classPoints.get("extraCreditCategoryTotalPoints") != null) ? classPoints.get("extraCreditCategoryTotalPoints") : 0.0f;
		extrapartPoints = (classpartPoints != null) ? (extrapartPoints + classpartPoints) : extrapartPoints;
	 
		rv.put("classAvg", (classPoints.get("classAveragePercent") != null) ? classPoints.get("classAveragePercent") : "- ");
		rv.put("classMax", (classpartPoints != null) ?  classpartPoints : "-");
		rv.put("extraCreditMax", (extrapartPoints != 0.0f) ? extrapartPoints : "-");
	}
	
	/**
	 * to get sort column from Grades screen
	 * @param sortBy
	 * @param sortOrder
	 * @return
	 */
	public ParticipantSort getParticipantSort(String sortBy, String sortOrder)
	{
		String sortType = "status";
		if ("Name".equalsIgnoreCase(sortBy)) sortType = "name";
		if ("Section".equalsIgnoreCase(sortBy)) sortType = "group_title";
		if ("Score".equalsIgnoreCase(sortBy)) sortType = "score";
		if ("Overall Grade".equalsIgnoreCase(sortBy)) sortType = "final";	
		return ParticipantSort.valueOf(("asc".equals(sortOrder)) ? sortType.concat("_a") : sortType.concat("_d"));
	}
	
	/**
	 * Get gradebook item type from student grades view 2
	 * @param selectedType
	 * @return
	 */
	public GradebookItemType getSelectedGradebookItemType(String selectedType)
	{
		// Discussions
		GradebookItemType selectedItemType = (selectedType != null && "2".equals(selectedType)) ? GradebookItemType.category : null;
		selectedItemType = (selectedType != null && "1".equals(selectedType)) ? GradebookItemType.assignment : selectedItemType;
		selectedItemType = (selectedType != null && "4".equals(selectedType)) ? GradebookItemType.survey : selectedItemType;
		selectedItemType = (selectedType != null && "5".equals(selectedType)) ? GradebookItemType.test : selectedItemType;
		selectedItemType = (selectedType != null && "7".equals(selectedType)) ? GradebookItemType.offline : selectedItemType;
		return selectedItemType;
	}
	
	/**
	 * @return The SiteService, via the component manager.
	 */
	protected GradebookService GradebookService()
	{
		return (GradebookService) ComponentManager.get(GradebookService.class);
	}

	/**
	 * 
	 * @param selectedType
	 * @param rv
	 */
	protected void loadAssessmentTypes(String selectedType, Map<String, Object> rv)
	{
		List<Map<String, String>> types = new ArrayList<Map<String, String>>();
		rv.put("itemTypes", types);

		HashMap<String, String> all = new HashMap<String, String>();
		all.put("title", "All");
		all.put("id", "All");
		types.add(all);

		HashMap<String, String> gbTypes = new HashMap<String, String>();
		gbTypes.put("title", "Discussions");
		gbTypes.put("id", "2");
		types.add(gbTypes);

		GradebookItemType[] itemTypes = GradebookItemType.values();
		for (GradebookItemType g : itemTypes)
		{
			HashMap<String, String> gTypes = new HashMap<String, String>();
			// just one entry for discussions
			if (g.getAppCode() == 2) continue;
			// skip surveys
			if (g.getId() == 4) continue;
			gTypes.put("title", g.getDisplayString());
			gTypes.put("id", g.getId().toString());
			types.add(gTypes);
		}

		if (selectedType == null || selectedType.equals("undefined"))
			rv.put("selectedType", "All");
		else
			rv.put("selectedType", selectedType);
	}

	/**
	 * 
	 * @param gradebook
	 * @param gradebookMap
	 */
	protected void loadCategories(Gradebook gradebook, Map<String, Object> gradebookMap, String siteId, String authenticatedUserId)
	{
		if (gradebook == null || gradebookMap == null)
		{
			gradebookMap.put("manageType", 1);
			return;
		}
		gradebookMap.put("id", gradebook.getId());
		gradebookMap.put("context", gradebook.getContext());
		gradebookMap.put("manageType", gradebook.getCategoryType().getCode());
			
		//fetch that type's categories
		List<GradebookCategory> categories = gradebook.getGradebookCategories();
		
		List<Map<String, Object>> categoriesList = new ArrayList<Map<String, Object>>();
		gradebookMap.put("categoriesList", categoriesList);

		if (categories == null || categories.size() == 0) return;

		boolean showWeights = false;
		
		for (GradebookCategory category : categories)
		{
			Float categoryPoints = category.getItemsTotalpoints();
			
			Map<String, Object> categoryDetailsMap = new HashMap<String, Object>();
			categoriesList.add(categoryDetailsMap);
			categoryDetailsMap.put("id", category.getId());
			categoryDetailsMap.put("code", category.getStandardCategoryCode());
			categoryDetailsMap.put("title", category.getTitle());
			if (category.getWeight() != null && category.getWeight() != 0.0f)
			{
				showWeights = true;
				categoryDetailsMap.put("weight", category.getWeight());
			}
			else 
			{
				categoryDetailsMap.put("weight", "");
			}
			
			if (categoryPoints != null && categoryPoints > 0.0) categoryDetailsMap.put("categoryPoints", categoryPoints);
			else categoryDetailsMap.put("categoryPoints", "");
			
			if (category.getWeight() != null && category.getWeight() != 0.0f && category.getWeightDistribution() != null)
				categoryDetailsMap.put("distribution", category.getWeightDistribution().getCode());
			else 
				categoryDetailsMap.put("distribution", 2);
			categoryDetailsMap.put("dropLowest", category.getDropNumberLowestScores());
			categoryDetailsMap.put("emptyCategory", category.getItemCount());
			categoryDetailsMap.put("extraCategory", category.isExtraCredit());			
		}
		gradebookMap.put("showWeight",showWeights);
	}

	/**
	 * load categories and tool items together for assessments overview screen
	 * @param categories
	 * @param gradebookItems
	 * @param allAssessmentsList
	 */
	protected void loadCategoriesAndGradebookItem(Map<GradebookCategory, List<GradebookItem>> map, List<Map<String, Object>> allAssessmentsList)
	{
		if (allAssessmentsList == null || map == null) return;
		
		Set<GradebookCategory> foundCategories = map.keySet();
		
		for (GradebookCategory aCategory : foundCategories)
		{
			List<GradebookItem> gradebookItems = map.get(aCategory);
		
			Float categoryPoints = aCategory.getItemsTotalpoints();
					
			Map<String, Object> gradebookItemMap = new HashMap<String, Object>();
			allAssessmentsList.add(gradebookItemMap);
			gradebookItemMap.put("isCategory", "yes");
			gradebookItemMap.put("categoryTitle", aCategory.getTitle());
			gradebookItemMap.put("categoryId", aCategory.getId());
			gradebookItemMap.put("categoryWeight",(aCategory.getWeight() == null || aCategory.getWeight() == 0.0f) ? "-" : String.valueOf(aCategory.getWeight()) + "%");
			gradebookItemMap.put("categoryPoints", (categoryPoints == null || categoryPoints == 0.0f) ? "-" : String.valueOf(categoryPoints.floatValue()) + " points");
			gradebookItemMap.put("emptyCategory", aCategory.getItemCount());
			gradebookItemMap.put("extraCategory", aCategory.isExtraCredit());	
			// add items
			for (GradebookItem gradebookItem : gradebookItems)
			{
				// load tool item
				gradebookItemMap = new HashMap<String, Object>();
				allAssessmentsList.add(gradebookItemMap);
				loadGradebookItem(gradebookItem, gradebookItemMap);

				// submission count
				if (gradebookItem.getSubmittedCount() != null && gradebookItem.getSubmittedCount() > 0)
					gradebookItemMap.put("submissionCount",	gradebookItem.getSubmittedCount());
				else
					gradebookItemMap.put("submissionCount", "-");
				
				// compute average
				gradebookItemMap.put("averageScore",(gradebookItem.getAveragePercent() == null) ? "-" : String.valueOf(gradebookItem.getAveragePercent().intValue()) + "%");
			}			
		}
		return;
	}
	
	/**
	 * Student landing screen and indv student summary
	 * @param categories
	 * @param gradebookItems
	 * @param allAssessmentsList
	 */
	protected void loadCategoriesAndUserGradebookItem(boolean addInProgressScore, Map<GradebookCategory, List<GradebookItem>> assessmentMap, List<GradebookCategory> categories, List<ParticipantGradebookItem> userGradebookItems, List<Map<String, Object>> userGradebookList)
	{
		if (userGradebookList == null || userGradebookItems == null) return;
		
		List<GradebookCategory> foundCategories = new ArrayList<GradebookCategory>();
		boolean showWeight = categoryHasWeights(categories);
		
		CourseMapItem courseMapItem = null;
		CourseMapItemProgressStatus courseMapItemProgressStatus = null;
		
		for (ParticipantGradebookItem userGradebookItem : userGradebookItems)
		{		
			GradebookItem gradebookItem = userGradebookItem.getGradebookItem();
			// category details
			GradebookCategory aCategory = gradebookItem.getGradebookCategory();
			if (aCategory != null && !foundCategories.contains(aCategory)) 
			{
				foundCategories.add(aCategory);
				
				Map<String, Object> gradebookItemMap = new HashMap<String, Object>();
				userGradebookList.add(gradebookItemMap);
				gradebookItemMap.put("isCategory", "yes");
				gradebookItemMap.put("categoryTitle", aCategory.getTitle());
				gradebookItemMap.put("categoryId", aCategory.getId());
				gradebookItemMap.put("categoryWeight", (aCategory.getWeight() == null || aCategory.getWeight() == 0.0f) ? "-" : String.valueOf(aCategory.getWeight()) + "%");
				Float aCategoryPoints = aCategory.getItemsTotalpoints();
				gradebookItemMap.put("categoryPoints", (aCategoryPoints == null || aCategoryPoints == 0.0f) ? "-" : String.valueOf(aCategoryPoints.floatValue()) + " points");
				gradebookItemMap.put("showWeight", showWeight);
				gradebookItemMap.put("emptyCategory", aCategory.getItemCount());
			}
			
			// gb item details along with participant start date etc
			Map<String, Object> gradebookItemMap = new HashMap<String, Object>();
			userGradebookList.add(gradebookItemMap);

			ParticipantItemDetails participantDetails = userGradebookItem.getParticipantItemDetails();
			loadGradebookItem(gradebookItem, gradebookItemMap);

			// TODO:completion status icon
			if (participantDetails.getStartedDate() != null)
				gradebookItemMap.put("startDate", CdpResponseHelper.dateTimeDisplayInUserZone(participantDetails.getStartedDate().getTime()));
			else
				gradebookItemMap.put("startDate", "-");

			if (participantDetails.getFinishedDate() != null)
				gradebookItemMap.put("finishDate", CdpResponseHelper.dateTimeDisplayInUserZone(participantDetails.getFinishedDate().getTime()));
			else
				gradebookItemMap.put("finishDate", "-");

			// reviewed date
			if (participantDetails.getReviewedDate() != null)
				gradebookItemMap.put("reviewDate", CdpResponseHelper.dateTimeDisplayInUserZone(participantDetails.getReviewedDate().getTime()));
			else
				gradebookItemMap.put("reviewDate", "-");

			// review status
			if (participantDetails.getEvaluationNotReviewed() != null && participantDetails.getEvaluationNotReviewed())
			{
				gradebookItemMap.put("reviewStatus", false);
			}
			else if (participantDetails.getEvaluationReviewed() != null && participantDetails.getEvaluationReviewed())
			{
				gradebookItemMap.put("reviewStatus", true);
			}
			//late or auto submission
			if (participantDetails.getIsSubmissionLate() != null) 
			{
				gradebookItemMap.put("lateSubmission", participantDetails.getIsSubmissionLate().toString());
			}
		
			if (participantDetails.getIsAutoSubmission() != null) 
			{
				gradebookItemMap.put("autoSubmission", participantDetails.getIsAutoSubmission().toString());
			}
			
			// submission Id for all these details
			gradebookItemMap.put("submissionId", participantDetails.getSubmissionId());
			
			// inProgress status
			if (participantDetails.getInProgress() != null)
				gradebookItemMap.put("inProgressStatus", participantDetails.getInProgress().booleanValue());
						
			// release score
			if (participantDetails.getIsReleased() != null)
				gradebookItemMap.put("releasedScore", participantDetails.getIsReleased().booleanValue());
						
			// review Link
			if (participantDetails.getReviewLink() != null)
				gradebookItemMap.put("reviewLink", participantDetails.getReviewLink());
			else 
				gradebookItemMap.put("reviewLink", "-");
			
			// grading Link
			if (participantDetails.getGradingLink() != null)
				gradebookItemMap.put("gradeLink", participantDetails.getGradingLink());
			else 
				gradebookItemMap.put("gradeLink", "-");
			
			// student dont see inprogress score 
			if (!addInProgressScore && participantDetails.getInProgress() != null && participantDetails.getInProgress().booleanValue())
				gradebookItemMap.put("userScore", "-");
			// instructor see all scores
			else if (participantDetails.getScore() != null)
				gradebookItemMap.put("userScore", participantDetails.getScore());	
			else
				gradebookItemMap.put("userScore", "-");
			
			gradebookItemMap.put("dropScore", participantDetails.getIsScoreDropped());
			
			// progress/completion status
			courseMapItemProgressStatus = null;
					
			courseMapItem = userGradebookItem.getCourseMapItem();
			if (courseMapItem != null)
			{
				courseMapItemProgressStatus = courseMapItem.getProgressStatus();
				gradebookItemMap.put("progressStatus", courseMapItemProgressStatus.name());
			}
		}
	
		return;
	}
	
	/**
	 * 
	 * @param gradebookItem
	 * @param gradebookItemMap
	 */
	protected void loadGradebookItem(GradebookItem gradebookItem, Map<String, Object> gradebookItemMap)
	{
		// title
		gradebookItemMap.put("title", gradebookItem.getTitle());

		// id
		gradebookItemMap.put("id", gradebookItem.getId());

		// points
		gradebookItemMap.put("points", gradebookItem.getPoints());

		// status
		String status = (gradebookItem.getAccessStatus() != null) ? gradebookItem.getAccessStatus().name() : "";
		if (status.contains("hidden")) status = "Hidden Until Open"; 
		if (status.contains("not_yet_open")) status = "Not Yet Open";
		if (status.contains("closed")) status = "Closed";
		if (status.equals("published")) status = "Open";
		gradebookItemMap.put("status", status);

		// open date "-" if not defined
		if (gradebookItem.getOpenDate() != null)
			gradebookItemMap.put("open", CdpResponseHelper.dateTimeDisplayInUserZone(gradebookItem.getOpenDate().getTime()));
		else
			gradebookItemMap.put("open", "-");

		// due date "-" if not defined
		if (gradebookItem.getDueDate() != null)
			gradebookItemMap.put("due", CdpResponseHelper.dateTimeDisplayInUserZone(gradebookItem.getDueDate().getTime()));
		else
			gradebookItemMap.put("due", "-");

		// accept until date "-" if not defined
		if (gradebookItem.getCloseDate() != null)
			gradebookItemMap.put("accept", CdpResponseHelper.dateTimeDisplayInUserZone(gradebookItem.getCloseDate().getTime()));
		else
			gradebookItemMap.put("accept", "-");

		// Forum, category, survey etc. used to show icons next to title
		gradebookItemMap.put("toolDisplayTitle", gradebookItem.getType().getDisplayString());

		// ATS or JForum
		gradebookItemMap.put("toolTitle", gradebookItem.getType().getToolTitle());
		
		// category
		gradebookItemMap.put("categoryItem", gradebookItem.getGradebookCategory());
		
		// display order
		gradebookItemMap.put("displayOrder", gradebookItem.getDisplayOrder());
	}

	/**
	 * Populate map with assessment item details.
	 * 
	 * @param gradebookItem
	 * @param gradebookItemMap
	 * @param includeScores
	 */
	protected void loadGradebookItem(GradebookItem gradebookItem, Map<String, Object> gradebookItemMap, boolean includeScores)
	{
		if (gradebookItem == null || gradebookItemMap == null) return;

		loadGradebookItem(gradebookItem, gradebookItemMap);

		List<Map<String, Object>> gradebookItemScoresList = new ArrayList<Map<String, Object>>();
		if (includeScores)
		{
			gradebookItemMap.put("scores", gradebookItemScoresList);
		}

		Map<String, Float> scores = gradebookItem.getScores();

		// submission count
		if (gradebookItem.getSubmittedCount() != null && gradebookItem.getSubmittedCount() > 0)
			gradebookItemMap.put("submissionCount", gradebookItem.getSubmittedCount());
		else
			gradebookItemMap.put("submissionCount", "-");

		// compute average
		gradebookItemMap.put("averageScore",
				(gradebookItem.getAveragePercent() == null) ? "-" : String.valueOf(gradebookItem.getAveragePercent().intValue()) + "%");

		return;
	}

	/**
	 * 
	 * @param gradebook
	 * @param gradebookMap
	 */
	protected void loadGradeOptions(Gradebook gradebook, Map<String, Object> gradebookMap)
	{
		if (gradebook == null || gradebookMap == null)
		{
			return;
		}

		gradebookMap.put("id", gradebook.getId());
		gradebookMap.put("context", gradebook.getContext());
		gradebookMap.put("sortCode", gradebook.getSortType().getCode());
		gradebookMap.put("showLetterGrade", gradebook.isShowLetterGrade() ? 1 : 0);
		gradebookMap.put("releaseGrades", gradebook.getReleaseGrades().getCode());
		gradebookMap.put("dropScore", gradebook.isDropLowestScore() ? 1 : 0);
		
		// current selected grading scale with percentages
		GradingScale selectedGradebookScale = gradebook.getGradingScale();
		loadSelectedGradingScale(gradebookMap, selectedGradebookScale);

		/* all context grading scales with scale percentages */
		List<GradingScale> contextGradingScales = gradebook.getContextGradingScales();

		List<Map<String, Object>> contextGradingScalesList = new ArrayList<Map<String, Object>>();
		gradebookMap.put("contextGradingScalesList", contextGradingScalesList);

		for (GradingScale contextGradingScale : contextGradingScales)
		{
			Map<String, Object> contextGradebookScaleMap = new HashMap<String, Object>();
			contextGradingScalesList.add(contextGradebookScaleMap);

			contextGradebookScaleMap.put("id", contextGradingScale.getId());
			contextGradebookScaleMap.put("name", contextGradingScale.getName());
			contextGradebookScaleMap.put("scaleCode", contextGradingScale.getScaleCode());
			contextGradebookScaleMap.put("version", contextGradingScale.getVersion());
			contextGradebookScaleMap.put("scaleType", contextGradingScale.getType().getScaleType());

			// selected grading scale percentages - GradingScalePercent
			List<Map<String, Object>> contextGradebookScalePercentList = new ArrayList<Map<String, Object>>();
			contextGradebookScaleMap.put("contextGradebookScalePercentList", contextGradebookScalePercentList);

			// List<Map<String, Object>> gradebookItemScoresList = new ArrayList<Map<String, Object>>();

			for (GradingScalePercent GradingScalePercent : contextGradingScale.getGradingScalePercent())
			{
				Map<String, Object> GradingScalePercentMap = new HashMap<String, Object>();
				contextGradebookScalePercentList.add(GradingScalePercentMap);

				GradingScalePercentMap.put("scaleId", GradingScalePercent.getScaleId());
				GradingScalePercentMap.put("letterGrade", GradingScalePercent.getLetterGrade());
				GradingScalePercentMap.put("percent", GradingScalePercent.getPercent());
			}
		}
	}

	/**
	 * 
	 * @param context
	 * @param itemId
	 * @param userId
	 * @param rv
	 */
	public List<ParticipantItemDetails> loadItemParticipants(String context, String itemId, String groupId, String userId, String sortby, boolean bestSubmission, Map<String, Object> rv)
	{			
		List<ParticipantItemDetails> itemParticipants = findItemParticipants(context, itemId, groupId, userId, sortby, !bestSubmission);

		List<Map<String, Object>> itemParticipantsList = new ArrayList<Map<String, Object>>();
	
		if (rv != null) rv.put("itemParticipants", itemParticipantsList);

		for (ParticipantItemDetails itemParticipant : itemParticipants)
		{
			Map<String, Object> ParticipantItemDetailsMap = new HashMap<String, Object>();
			itemParticipantsList.add(ParticipantItemDetailsMap);

			loadParticipant(ParticipantItemDetailsMap, itemParticipant, userId);
		
			// start date
			if (itemParticipant.getStartedDate() != null)
				ParticipantItemDetailsMap.put("startDate", CdpResponseHelper.dateTimeDisplayInUserZone(itemParticipant.getStartedDate().getTime()));
			else
				ParticipantItemDetailsMap.put("startDate", "-");

			// finish date
			if (itemParticipant.getFinishedDate() != null)
				ParticipantItemDetailsMap.put("finishDate", CdpResponseHelper.dateTimeDisplayInUserZone(itemParticipant.getFinishedDate().getTime()));
			else
				ParticipantItemDetailsMap.put("finishDate", "-");

			// reviewed date
			if (itemParticipant.getReviewedDate() != null)
				ParticipantItemDetailsMap.put("reviewDate", CdpResponseHelper.dateTimeDisplayInUserZone(itemParticipant.getReviewedDate().getTime()));
			else
				ParticipantItemDetailsMap.put("reviewDate", "-");

			//ParticipantItemDetailsMap.put("reviewStatus", itemParticipant.getEvaluationReviewed().booleanValue());
			if (itemParticipant.getEvaluationNotReviewed() != null && itemParticipant.getEvaluationNotReviewed())
			{
				ParticipantItemDetailsMap.put("reviewStatus", false);
			}
			else if (itemParticipant.getEvaluationReviewed() != null && itemParticipant.getEvaluationReviewed())
			{
				ParticipantItemDetailsMap.put("reviewStatus", true);
			}
			
			if (itemParticipant.getGradingLink() != null)
			{
				ParticipantItemDetailsMap.put("gradeLink", itemParticipant.getGradingLink());
			}
			
			if (itemParticipant.getIsSubmissionLate() != null) 
			{
				ParticipantItemDetailsMap.put("lateSubmission", itemParticipant.getIsSubmissionLate().toString());
			}
		
			if (itemParticipant.getIsAutoSubmission() != null) 
			{
				ParticipantItemDetailsMap.put("autoSubmission", itemParticipant.getIsAutoSubmission().toString());
			}
			
			if (itemParticipant.getInProgress() != null)
			{
				ParticipantItemDetailsMap.put("inProgressStatus", itemParticipant.getInProgress().booleanValue());
			}
			
			if (itemParticipant.getIsReleased() != null)
			{
				ParticipantItemDetailsMap.put("scoreReleased", itemParticipant.getIsReleased().booleanValue());
			}
			
			if (itemParticipant.getSubmissionId() != null)
			{
				ParticipantItemDetailsMap.put("submissionId", itemParticipant.getSubmissionId());
			}
				
			// user total score
			if (itemParticipant.getScore() != null)
			{
				ParticipantItemDetailsMap.put("userScore", itemParticipant.getScore());
			}
			else
				ParticipantItemDetailsMap.put("userScore", "-");
		}
		return itemParticipants;
	}
	
	/**
	 * Load participant details
	 * 
	 * @param ParticipantItemDetailsMap
	 * @param participant
	 */
	protected void loadParticipant(Map<String, Object> ParticipantItemDetailsMap, Participant participant, String authenticatedUserId)
	{
		// id
		ParticipantItemDetailsMap.put("id", participant.getUserId());

		// pm link
		if (participant.getPrivateMessageLink() != null) ParticipantItemDetailsMap.put("sendPMLink", participant.getPrivateMessageLink());
		
		// name and display iid
		ParticipantItemDetailsMap.put("name", participant.getSortName() + " (" + participant.getDisplayId() + ")");

		// instructor notes on a student
		if (participant.getInstructorNotes() != null) ParticipantItemDetailsMap.put("notes", participant.getInstructorNotes().getNotes());
		fillNotesHistory(ParticipantItemDetailsMap, participant.getInstructorNotes(), authenticatedUserId);
		
		// enrolled status
		ParticipantItemDetailsMap.put("enrolledStatus", participant.getStatus().getDisplayName());

		// section
		if (participant.getGroupTitle() != null)
		{
			String groupTitle = participant.getGroupTitle();
			ParticipantItemDetailsMap.put("section", groupTitle);
		}
		else
			ParticipantItemDetailsMap.put("section", "");

		// total score
		if (participant.getTotalScore() != null && !"null".equals(participant.getTotalScore()))
			ParticipantItemDetailsMap.put("totalScore", participant.getTotalScore());
		else
			ParticipantItemDetailsMap.put("totalScore", "-");

		// total points
		if (participant.getTotalPoints() != null && !"null".equals(participant.getTotalPoints()))
		{
			ParticipantItemDetailsMap.put("totalPoints",participant.getTotalPoints());			
		}
		else
			ParticipantItemDetailsMap.put("totalPoints", "-");
		
		// final score - score plus extra credit
		if (participant.getGrade() != null && participant.getGrade().getTotalScore() != null)
			ParticipantItemDetailsMap.put("finalScore", participant.getGrade().getTotalScore());
		else ParticipantItemDetailsMap.put("finalScore", "-");
		
		// extra credit score
		if (participant.getGrade() != null && participant.getGrade().getExtraCreditScore() != null)
			ParticipantItemDetailsMap.put("extraScore", participant.getGrade().getExtraCreditScore());
		else ParticipantItemDetailsMap.put("extraScore", "-");

		// overall grade
		if (participant.getGrade() != null && participant.getGrade().getLetterGrade() != null && participant.getGrade().getAveragePercent() != null)
			ParticipantItemDetailsMap.put("totalGrade", participant.getGrade().getLetterGrade() + " (" + participant.getGrade().getAveragePercent()
					+ "%)");
		else
			ParticipantItemDetailsMap.put("totalGrade", "-");

		// override
		if (participant.getOverriddenLetterGrade() != null && participant.getOverriddenLetterGrade().getLetterGrade() != null)
			ParticipantItemDetailsMap.put("gradeOverride", participant.getOverriddenLetterGrade().getLetterGrade());
		else
			ParticipantItemDetailsMap.put("gradeOverride", "");

		// log history if any
		fillLogHistoryDetails(ParticipantItemDetailsMap, participant);
	}

	/**
	 * Add sections to the screen
	 * 
	 * @param siteId
	 * @param selectedSection
	 * @param rv
	 */
	protected void loadSections(String siteId, String selectedSection, Map<String, Object> rv)
	{
		Map<String, String> siteSections = GradebookService().getSections(siteId);
		List<Map<String, String>> sections = new ArrayList<Map<String, String>>();
		rv.put("itemSections", sections);

		Map<String, String> mapSections = new HashMap<String, String>();
		mapSections.put("sectionId", "All");
		mapSections.put("sectionTitle", "All Sections");
		sections.add(mapSections);

		if (siteSections != null)
		{
			for (String key : siteSections.keySet())
			{
				mapSections = new HashMap<String, String>();
				mapSections.put("sectionId", key);
				mapSections.put("sectionTitle", siteSections.get(key));
				sections.add(mapSections);
			}
		}

		if (selectedSection == null || selectedSection.equals("undefined"))
			rv.put("selectedSection", "All");
		else
			rv.put("selectedSection", selectedSection);
	}

	/**
	 * Load the map with selected grading scale
	 * 
	 * @param gradebookMap
	 *        Gradebook map
	 * 
	 * @param selectedGradebookScale
	 *        Selected Gradebookscale
	 */
	protected void loadSelectedGradingScale(Map<String, Object> gradebookMap, GradingScale selectedGradebookScale)
	{
		if (gradebookMap != null && selectedGradebookScale != null)
		{
			Map<String, Object> selectedGradebookScaleMap = new HashMap<String, Object>();
			gradebookMap.put("selectedGradebookScale", selectedGradebookScaleMap);

			selectedGradebookScaleMap.put("id", selectedGradebookScale.getId());
			selectedGradebookScaleMap.put("name", selectedGradebookScale.getName());
			selectedGradebookScaleMap.put("scaleCode", selectedGradebookScale.getScaleCode());
			selectedGradebookScaleMap.put("version", selectedGradebookScale.getVersion());
			selectedGradebookScaleMap.put("scaleType", selectedGradebookScale.getType().getScaleType());

			// selected grading scale percentages - GradingScalePercent
			List<Map<String, Object>> selectedGradebookScalePercentList = new ArrayList<Map<String, Object>>();
			selectedGradebookScaleMap.put("selectedGradebookScalePercentList", selectedGradebookScalePercentList);

			// List<Map<String, Object>> gradebookItemScoresList = new ArrayList<Map<String, Object>>();

			for (GradingScalePercent GradingScalePercent : selectedGradebookScale.getGradingScalePercent())
			{
				Map<String, Object> GradingScalePercentMap = new HashMap<String, Object>();
				selectedGradebookScalePercentList.add(GradingScalePercentMap);

				GradingScalePercentMap.put("scaleId", GradingScalePercent.getScaleId());
				GradingScalePercentMap.put("letterGrade", GradingScalePercent.getLetterGrade());
				GradingScalePercentMap.put("percent", GradingScalePercent.getPercent());
			}
		}
	}

	/**
	 * save instructor's note on a student from various screens
	 * @param parameters
	 * @param siteId
	 * @param authenticatedUserId
	 */
	protected void modifyInstructorNotes(Map<String, Object> parameters, String siteId, String authenticatedUserId)
	{
		String instructorNotes = (String) parameters.get("instructorNotes");
		if (instructorNotes != null) instructorNotes = instructorNotes.trim();
		
		String instructorNotesDate = (String) parameters.get("notesDate");
		Date instructorNotesUpdateDate = (instructorNotesDate == null || instructorNotesDate.equals("undefined") || instructorNotesDate.equals("0")) ? null : new Date(new Long(instructorNotesDate));
	
		String instructorNotesStudentId = (String) parameters.get("notesStudentId");
		if (instructorNotes == null && instructorNotesUpdateDate == null) return;
		if (instructorNotesStudentId == null) return;
		Notes instructorUserNotes = GradebookService().newUserNote(instructorNotesStudentId, instructorNotes, instructorNotesUpdateDate, authenticatedUserId);
		GradebookService().addModifyInstructorUserNotes(siteId, instructorUserNotes);		
	}
	
	/**
	 * Method to fill override score in map from request parameters
	 * @param parameters
	 * @param callFrom
	 * @param overrideScores
	 */
	protected void modifyUserScores(Map<String, Object> parameters, String callFrom, List<ParticipantItemDetails> itemParticipants, List<ParticipantGradebookItem> participantGradebook)
	{
		String gradesCount = (String) parameters.get("gradesOverrideCount");
		int gradesLength = Integer.parseInt(gradesCount);
		if (gradesCount == null || gradesCount.equals("undefined") || gradesCount.equals("0")) return;
			
		for (int i = 0; i < gradesLength; i++)
		{
			String newScoreFound = StringUtil.trimToNull((String) parameters.get("newScore" + i));
			newScoreFound = (newScoreFound == null || newScoreFound.equals("undefined")) ? null : newScoreFound;
			if ("-".equals(newScoreFound)) continue;
			Float overrideScore = (newScoreFound == null) ? null : Float.valueOf(newScoreFound);
			
			String scoreLoadTime = StringUtil.trimToNull((String) parameters.get("loadTime" + i));
			Date scoreLoadDateTime = (scoreLoadTime == null || scoreLoadTime.equals("undefined")) ? null : new Date(new Long(scoreLoadTime));
	
			String studentId = StringUtil.trimToNull((String) parameters.get("user" + i));
			studentId = (studentId == null || studentId.equals("undefined")) ? null : studentId;
			
			String assessmentId = StringUtil.trimToNull((String) parameters.get("overrideAssessmentId" + i));
			assessmentId = (assessmentId == null || assessmentId.equals("undefined")) ? null : assessmentId;
			
			String submissionId = StringUtil.trimToNull((String) parameters.get("overrideSubmissionId" + i));
			submissionId = (submissionId == null || submissionId.equals("undefined")) ? null : submissionId;
			
			String newScoreRelease = StringUtil.trimToNull((String) parameters.get("overrideRelease" + i));
			newScoreRelease = (newScoreRelease == null || newScoreRelease.equals("undefined")) ? null : newScoreRelease;
			
			if ("itemDetails".equals(callFrom) && studentId != null && itemParticipants != null) 
			{					
				for (ParticipantItemDetails p : itemParticipants)
				{
					if (submissionId != null && p.getUserId().equals(studentId) && p.getSubmissionId().equals(submissionId))
					{
						p.setIsReleased(("true".equals(newScoreRelease)) ? true : false);
						p.setScore(overrideScore);
						break;
					}
				}
			}
			if ("studentSummary".equals(callFrom) && studentId != null && participantGradebook != null) 
			{
				for (ParticipantGradebookItem p : participantGradebook)
				{
					ParticipantItemDetails pd= p.getParticipantItemDetails();
					if (submissionId != null && p.getId().equals(assessmentId) && pd.getSubmissionId().equals(submissionId))
					{
						pd.setIsReleased(("true".equals(newScoreRelease)) ? true : false);
						pd.setScore(overrideScore);
						break;
					}
				}
			}
		}		
	}
	
	/**
	 * 
	 * @param siteId
	 * @param authenticatedUserId
	 * @param parameters
	 * @param gradebook
	 * @param toolGradebookItems
	 */
	protected void saveAssessmentsMap(String siteId, String authenticatedUserId, Map<String, Object> parameters, Gradebook gradebook, List<GradebookItem> toolGradebookItems)
	{
		// Save map
		String mapItemsCount = (String) parameters.get("mapCount");
		if (mapItemsCount != null && !mapItemsCount.equals("undefined"))
		{
		    // initialize map
			 List<GradebookCategoryItemMap> itemMap = new ArrayList<GradebookCategoryItemMap>();
			 String lastCategoryId = null;
			 int displayOrder = 1;
			int mapLength = Integer.parseInt(mapItemsCount);
			for (int i = 1; i < mapLength; i++)
			{
				String id = StringUtil.trimToNull((String) parameters.get("saveMap" + i));
				if (id == null || id.length() == 0) continue;
				if (id.startsWith("Category"))	
				{
					lastCategoryId = id.replace("Category-", "");
				}
				if (id.startsWith("Item"))	
				{
					id = id.replace("Item-", "");
					if (lastCategoryId == null || lastCategoryId.length() == 0 || id.length() == 0) continue;
					GradebookCategoryItemMap gitem = GradebookService().newGradebookCategoryItemMap(id, Integer.parseInt(lastCategoryId), displayOrder++); 
					itemMap.add(gitem);					
				}
			}
			// save map
			if (itemMap.size() > 0) GradebookService().modifyItemMapping(siteId, itemMap, authenticatedUserId);
		}
		
		String action = (String) parameters.get("action");
		if (action != null && !action.equals("undefined") && action.equals("sort_assessment"))
		{
			String oldIdx = (String) parameters.get("oldIdx");
			int old_order = Integer.parseInt(oldIdx);
			
			String newDisplayOrder = (String) parameters.get("newIdx");
			int new_order = Integer.parseInt(newDisplayOrder);
					
			List<Map<String, Object>> allAssessmentsList = new ArrayList<Map<String, Object>>();
			
			// mix categories and tool items			
			Map<GradebookCategory, List<GradebookItem>> assessmentMap = GradebookService().getToolGradebookItems(siteId, authenticatedUserId, false, false); 
			loadCategoriesAndGradebookItem(assessmentMap, allAssessmentsList);
			
			int old_index = 0;
			for (Map<String, Object> obj : allAssessmentsList)
			{
				if (obj.get("displayOrder") != null && ((Integer)obj.get("displayOrder")).intValue() == old_order)
				{
					break;
				}
				else old_index++;
			}
		
			int new_index = 0;
			for (Map<String, Object> obj : allAssessmentsList)
			{
				if (obj.get("displayOrder") != null && ((Integer)obj.get("displayOrder")).intValue() == new_order)
				{
					new_index++;
					break;
				}
				else new_index++;
			}
			allAssessmentsList.add(new_index-1, allAssessmentsList.remove(old_index));
			
			 List<GradebookCategoryItemMap> itemMap = new ArrayList<GradebookCategoryItemMap>();
			 Integer lastCategoryId = null;
			 int displayOrder = 1;
			 for (Map<String, Object> obj : allAssessmentsList)
			 {
				 if (obj.get("isCategory") != null)
					 lastCategoryId = (Integer) obj.get("categoryId");
				 else 
				 {
					 String id = (String) obj.get("id");
					 GradebookCategoryItemMap gitem = GradebookService().newGradebookCategoryItemMap(id, lastCategoryId.intValue(), displayOrder++); 
					 itemMap.add(gitem);	
				 }
			 }
			// save map
			if (itemMap.size() > 0) GradebookService().modifyItemMapping(siteId, itemMap, authenticatedUserId);
		}
	
	}
	
	/**
	 * @return The SiteService, via the component manager.
	 */
	protected SiteService siteService()
	{
		return (SiteService) ComponentManager.get(SiteService.class);
	}

	/**
	 * @return The SiteService, via the component manager.
	 */
	protected UserDirectoryService UserDirectoryService()
	{
		return (UserDirectoryService) ComponentManager.get(UserDirectoryService.class);
	}
}
