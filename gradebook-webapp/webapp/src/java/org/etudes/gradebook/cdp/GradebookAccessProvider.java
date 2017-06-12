/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-webapp/webapp/src/java/org/etudes/gradebook/cdp/GradebookAccessProvider.java $
 * $Id: GradebookAccessProvider.java 12474 2016-01-06 21:43:32Z rashmim $
 ***********************************************************************************
 *
 * Copyright (c) 2015, 2016 Etudes, Inc.
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

import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.gradebook.api.GradebookItem;
import org.etudes.gradebook.api.GradebookItemType;
import org.etudes.gradebook.api.GradebookService;
import org.etudes.gradebook.api.Participant;
import org.etudes.gradebook.api.ParticipantGradebookItem;
import org.etudes.gradebook.api.ParticipantItemDetails;
import org.etudes.gradebook.api.ParticipantSort;
import org.etudes.gradebook.api.UserGrade;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.EntityAccessOverloadException;
import org.sakaiproject.entity.api.EntityCopyrightException;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.EntityNotDefinedException;
import org.sakaiproject.entity.api.EntityPermissionException;
import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.entity.api.HttpAccess;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.exception.CopyrightException;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.util.BaseResourcePropertiesEdit;
import org.sakaiproject.util.StringUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * EvalManagerAccessProvider implements EntityProducer for the evaluation manager.
 */
public class GradebookAccessProvider implements EntityProducer
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(GradebookAccessProvider.class);

	/** The reference type. */
	static final String REF_TYPE = "etudes:gradebook";

	/** This string starts the references to download requests. */
	static final String REFERENCE_ROOT = "/etudesgb";

	static final String SEPARATOR = ",";

	/** The cdp handler we use for help getting data. */
	GradebookCdpHandler cdpHandler = null;

	public GradebookAccessProvider() {		
	}

	/**
	 * Construct
	 * 
	 * @param cdpHandler
	 *        The cdp handler we use for help getting data.
	 */
	public GradebookAccessProvider(GradebookCdpHandler cdpHandler)
	{
		this.cdpHandler = cdpHandler;

		// register as an entity producer
		entityManager().registerEntityProducer(this, REFERENCE_ROOT);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("rawtypes")
	public String archive(String siteId, Document doc, Stack stack, String archivePath, List attachments)
	{
		return null;
	}

	/**
	 * @return The EntityManager, via the component manager.
	 */
	private EntityManager entityManager()
	{
		return (EntityManager) ComponentManager.get(EntityManager.class);
	}

	/**
	 * 
	 * @param value
	 * @param separator
	 * @return
	 */
	protected String escape(String value)
	{
		if (value.indexOf(SEPARATOR) != -1)
		{
			// any quotes in here need to be doubled
			value = value.replaceAll("\"", "\"\"");

			return "\"" + value + "\"";
		}

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
		return escape(removeSeconds(format.format(date)));
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
		return escape(value.toString());
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
		value = escape(value);
		return value;
	}

	protected String exportNotesString(String value)
	{		
		if (value == null) return "-";
		value = escape(value);
		value = value.replaceAll("\r\n", "\n");
		value = value.replaceAll("\n", ".");
		return value;
	}
	
	/**
	 * Export assessment participation details. called from item details screen.
	 * @param printer
	 * @param req
	 */
	protected void 	exportItemDetails(PrintStream printer, HttpServletRequest req, String siteId)
	{
		String itemId = (String)req.getParameter("item_id");
		String sortBy = (req.getParameter("sortBy") != null) ? (String)req.getParameter("sortBy") : "status_a";
		String section = (req.getParameter("section") != null) ? (String)req.getParameter("section") : null;
		if ("All".equalsIgnoreCase(section)) section = null;
		
		if (itemId != null)
		{
			List<ParticipantItemDetails> itemParticipants = cdpHandler.loadItemParticipants(siteId, itemId, section, sessionManager().getCurrentSessionUserId(), sortBy.toLowerCase(), true, null);
			
			printer.println("Name" + SEPARATOR + "Student ID" + SEPARATOR + "Section" + SEPARATOR + "Status" + SEPARATOR + "Finished" + SEPARATOR + "Reviewed" + SEPARATOR + "Score" + SEPARATOR +"Notes");
			
			for (ParticipantItemDetails item : itemParticipants)
			{
				printer.print(exportFormatString(item.getSortName()) + SEPARATOR);
				printer.print(exportFormatString(item.getDisplayId()) + SEPARATOR);
				printer.print(exportFormatString(item.getGroupTitle()) + SEPARATOR);
				printer.print(exportFormatString(item.getStatus().getDisplayName()) + SEPARATOR);
				printer.print(exportFormatDate(item.getFinishedDate()) + SEPARATOR);
				printer.print(exportFormatDate(item.getReviewedDate()) + SEPARATOR);
				printer.print(exportFormatFloat(item.getScore()) + SEPARATOR);
				printer.println(exportNotesString((item.getInstructorNotes() == null) ? "" : item.getInstructorNotes().getNotes()));
			}
		}
	}
	
	/**
	 * /etudesgb/indv_student_grades/siteId/exportFileName/?studentUserId=&pointsHeaderText&pointsText
	 * Export Individual Student grades
	 * @param printer
	 * @param req
	 * @param siteId
	 */	
	protected void exportIndvidualParticipantGrades(PrintStream printer, HttpServletRequest req, String siteId)
	{
		String studentUserId = (String)req.getParameter("studentUserId");
		if (studentUserId == null) return;
		
		// Get user's gradable items and sort by due date asc
		List<ParticipantGradebookItem> userGradebookItems = GradebookService().getUserToolGradebookItems(siteId, studentUserId, true);
		if (userGradebookItems == null || userGradebookItems.size() == 0)
		{
			exportParticipantUserData(printer, req, siteId, studentUserId);
			return;
		}

		// headers
		printer.println("Title" + SEPARATOR + "Open" + SEPARATOR + "Due" + SEPARATOR + "Finished" + SEPARATOR + "Reviewed" + SEPARATOR + "Points" + SEPARATOR + "Score");
		
		// grades data
		for (ParticipantGradebookItem item : userGradebookItems)
		{
			GradebookItem gbItem = item.getGradebookItem();
			ParticipantItemDetails indvParticipant = item.getParticipantItemDetails();

			printer.print(exportFormatString(gbItem.getTitle()) + SEPARATOR);
			printer.print(exportFormatDate(gbItem.getOpenDate())  + SEPARATOR);
			printer.print(exportFormatDate(gbItem.getDueDate())  + SEPARATOR);
			printer.print(exportFormatDate(indvParticipant.getFinishedDate()) + SEPARATOR);
			printer.print(exportFormatDate(indvParticipant.getReviewedDate()) + SEPARATOR);
			printer.print(exportFormatFloat(gbItem.getPoints()) + SEPARATOR);
			String score = exportFormatFloat(indvParticipant.getScore());
			if (indvParticipant.getIsScoreDropped()) score = "*" + score + "*";
			printer.println(score );
		}

		// user data
		exportParticipantUserData(printer, req, siteId, studentUserId);
	}

	/**
	 * export user's grade data
	 * @param printer
	 * @param req
	 * @param siteId
	 * @param studentUserId
	 */
	protected void exportParticipantUserData(PrintStream printer, HttpServletRequest req, String siteId, String studentUserId)
	{
		Participant student = GradebookService().getParticipant(siteId, studentUserId, true);
		String pointsHeaderText = (String)req.getParameter("pointsHeaderText");
		String pointsText = (String)req.getParameter("pointsText");
		
		printer.println(exportFormatString(student.getSortName()));
		
		// add total score
		UserGrade userGrade = GradebookService().getUserGrade(siteId, studentUserId);		
		Float totalScore = (userGrade != null) ? userGrade.getTotalScore() : null;	
		Float totalPoints = (userGrade != null) ? userGrade.getPoints() : new Float(0.0f);
		String letterGrade = (userGrade != null) ? userGrade.getLetterGrade() : null;
		Float averagePercent = (userGrade != null) ? userGrade.getAveragePercent() : null;
		
		printer.print("Grades as of " + new Date() +  SEPARATOR + SEPARATOR + SEPARATOR + SEPARATOR + SEPARATOR);
		printer.print(exportFormatFloat(totalPoints) +  SEPARATOR);
		printer.println(exportFormatFloat(totalScore) );
			
		String gradeData = "";
		gradeData = gradeData.concat((letterGrade != null) ? exportFormatString(letterGrade) : "-");
		gradeData = gradeData.concat("/(" + ((averagePercent != null) ? exportFormatFloat(averagePercent) : "-") + "%)");
		printer.println(exportFormatString(pointsHeaderText) + exportFormatString(gradeData));
		printer.println(exportFormatString(pointsText));	
		
		//notes
		printer.println("");
		String notes = "";
		if (student.getInstructorNotes() != null) notes = student.getInstructorNotes().getNotes(); 
		if (notes.length() > 0)
		{
			printer.println("Notes");
			printer.println(exportFormatString(notes));
		}
	}
	
	/**
	 * Export student grades view 1 and view 2	
	 * // /etudesgb/student_grades/siteId/exportFileName/?itemType=&sortBy=&sortOrder=&section=&viewAssessments&showExtraCredit=
	 * @param printer
	 * @param req
	 */
	protected void 	exportStudentGrades(PrintStream printer, HttpServletRequest req, String siteId)
	{
		String sortBy = (req.getParameter("sortBy") != null) ? (String)req.getParameter("sortBy") : "status";
		String sortOrder = (req.getParameter("sortOrder") != null) ? (String)req.getParameter("sortOrder") : "a";
		String section = (req.getParameter("section") != null) ? (String)req.getParameter("section") : null;
		String itemType = (req.getParameter("itemType") != null) ? (String)req.getParameter("itemType") : null;
		boolean showExtraCredit = (req.getParameter("showExtraCredit") != null) ? Boolean.getBoolean(req.getParameter("showExtraCredit")): false;
		
		if ("All".equalsIgnoreCase(section)) section = null;
		if ("All".equalsIgnoreCase(itemType)) itemType = null;
		
		ParticipantSort sort = cdpHandler.getParticipantSort(sortBy, sortOrder);
		
		Collection<Participant> filterParticipants = GradebookService().getUsersGradebookSummary(siteId, section, sessionManager().getCurrentSessionUserId(), sort);
		if (filterParticipants == null || filterParticipants.size() == 0) return;
		
		// csv headers
		if (showExtraCredit)
			printer.println("Name" + SEPARATOR + "Student ID" + SEPARATOR + "Section" + SEPARATOR + "Status" + SEPARATOR + "Score" + SEPARATOR +"Out of" + SEPARATOR + "Extra Credit" + SEPARATOR + "Overall Grade" + SEPARATOR + "Grade Override" + SEPARATOR + "Notes");
		else 
			printer.println("Name" + SEPARATOR + "Student ID" + SEPARATOR + "Section" + SEPARATOR + "Status" + SEPARATOR + "Score" + SEPARATOR +"Out of" + SEPARATOR + "Overall Grade" + SEPARATOR + "Grade Override" + SEPARATOR + "Notes");
			
		// csv data
		for (Participant item : filterParticipants)
		{
			printer.print(exportFormatString(item.getSortName()) + SEPARATOR);
			printer.print(exportFormatString(item.getDisplayId()) + SEPARATOR);
			printer.print(exportFormatString(item.getGroupTitle()) + SEPARATOR);
			printer.print(exportFormatString(item.getStatus().getDisplayName()) + SEPARATOR);	
			String totalScore = (item.getGrade() != null && item.getGrade().getTotalScore() != null) ? exportFormatFloat(item.getGrade().getTotalScore()) : "-";
			printer.print(totalScore + SEPARATOR);			
			printer.print(exportFormatFloat(item.getTotalPoints()) + SEPARATOR);
			
			//Over all grades data		
			String extraC = (item.getGrade() != null && item.getGrade().getExtraCreditScore() != null) ? exportFormatFloat(item.getGrade().getExtraCreditScore()) : "-";
			if (showExtraCredit) printer.print(extraC  + SEPARATOR);
			
			if (item.getGrade() != null && item.getGrade().getAveragePercent() != null)
				printer.print(exportFormatString(item.getGrade().getLetterGrade() + " (" + item.getGrade().getAveragePercent() + "%)")  + SEPARATOR);
			else
				printer.print(exportFormatString(null)  + SEPARATOR);

			if (item.getOverriddenLetterGrade() != null)
				printer.print(exportFormatString(item.getOverriddenLetterGrade().getLetterGrade())  + SEPARATOR);
			else
				printer.print("-"  + SEPARATOR);
			
			printer.println(exportNotesString((item.getInstructorNotes() == null) ? "" : item.getInstructorNotes().getNotes()));
		}
	}

	/**
	 * export student grades view 2 - indv assessment score breakup
	 * @param printer
	 * @param req
	 * @param siteId
	 */
	protected void 	exportStudentAssessmentGrades(PrintStream printer, HttpServletRequest req, String siteId)
	{
		String sortBy = (req.getParameter("sortBy") != null) ? (String)req.getParameter("sortBy") : "status";
		String sortOrder = (req.getParameter("sortOrder") != null) ? (String)req.getParameter("sortOrder") : "a";
		String section = (req.getParameter("section") != null) ? (String)req.getParameter("section") : null;
		String itemType = (req.getParameter("itemType") != null) ? (String)req.getParameter("itemType") : null;
		
		if ("All".equalsIgnoreCase(section)) section = null;
		if ("All".equalsIgnoreCase(itemType)) itemType = null;
		
		ParticipantSort sort = cdpHandler.getParticipantSort(sortBy, sortOrder);
		GradebookItemType gItemType = cdpHandler.getSelectedGradebookItemType(itemType);
		
		//all participants and their score
		Map<Participant, List<ParticipantGradebookItem>> all = null;
		all = GradebookService().getUsersGradebookSummaryAndGradeBookItems(siteId, section, sessionManager().getCurrentSessionUserId(), sort, gItemType);
		
		Collection<Participant> filterParticipants = all.keySet();		
		if (filterParticipants == null || filterParticipants.size() == 0) return;
		
		// csv headers	
		StringBuffer view2Headers = new StringBuffer("Name" + SEPARATOR + "Student ID" + SEPARATOR + "Section" + SEPARATOR + "Status" + SEPARATOR + "Score" + SEPARATOR +"Out of");
		
		// collect all assessment names
		List<String> assessmentHeaders = new ArrayList<String>();
		List<GradebookItem> allAssessments = GradebookService().getToolGradebookItems(siteId, sessionManager().getCurrentSessionUserId(), false, false, null, null, gItemType);
			
		// add assessment headers with points like Essay{{100}}
		for (GradebookItem a : allAssessments)
		{					
			view2Headers = view2Headers.append(SEPARATOR + exportFormatString(a.getTitle()) + "{{" + a.getPoints() + "}}");
			assessmentHeaders.add(a.getId());
		}
		view2Headers = view2Headers.append(SEPARATOR + "Notes");
		printer.println(view2Headers.toString());
		
		// csv data
		for (Participant item : filterParticipants)
		{
			printer.print(exportFormatString(item.getSortName()) + SEPARATOR);
			printer.print(exportFormatString(item.getDisplayId()) + SEPARATOR);
			printer.print(exportFormatString(item.getGroupTitle()) + SEPARATOR);
			printer.print(exportFormatString(item.getStatus().getDisplayName()) + SEPARATOR);	
			String totalScore = (item.getGrade() != null && item.getGrade().getTotalScore() != null) ? exportFormatFloat(item.getGrade().getTotalScore()) : "-";
			printer.print(totalScore + SEPARATOR);			
			if (allAssessments.size() > 0)
			{
				printer.print(exportFormatFloat(item.getTotalPoints()) + SEPARATOR);

				// add each test scores
				List<ParticipantGradebookItem> gbs = all.get(item);
				for (String assessmentHeader : assessmentHeaders)
				{
					String score = "-";
					for (ParticipantGradebookItem pg : gbs)
					{
						if (pg.getGradebookItem().getId().equals(assessmentHeader) && pg.getParticipantItemDetails() != null
								&& pg.getParticipantItemDetails().getScore() != null)
						{
							score = Float.toString(pg.getParticipantItemDetails().getScore());							
							if (pg.getParticipantItemDetails().getIsScoreDropped()) score = "*" + score + "*"; 	
						}
					}

					printer.print(exportFormatString(score) + SEPARATOR);
				}	
			}
			else
			{
				// =out of becomes last column if no assessments
				printer.print(exportFormatFloat(item.getTotalPoints()) + SEPARATOR);
			}
			printer.println(exportNotesString((item.getInstructorNotes() == null) ? "" : item.getInstructorNotes().getNotes()));
		}		
	}

	/**
	 * {@inheritDoc}
	 */
	public Entity getEntity(Reference ref)
	{
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("rawtypes")
	public Collection getEntityAuthzGroups(Reference ref, String userId)
	{
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getEntityDescription(Reference ref)
	{
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public ResourceProperties getEntityResourceProperties(Reference ref)
	{
		// decide on security
		ResourcePropertiesEdit props = new BaseResourcePropertiesEdit();

		props.addProperty(ResourceProperties.PROP_CONTENT_TYPE, "text/csv");
		props.addProperty(ResourceProperties.PROP_IS_COLLECTION, "FALSE");
		props.addProperty("DAV:displayname", "Gradebook Exports");

		return props;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getEntityUrl(Reference ref)
	{
		return serverConfigurationService().getAccessUrl() + ref.getReference();
	}

	/**
	 * {@inheritDoc}
	 */
	public HttpAccess getHttpAccess()
	{
		return new HttpAccess()
		{
			@SuppressWarnings("rawtypes")
			public void handleAccess(HttpServletRequest req, HttpServletResponse res, Reference ref, Collection copyrightAcceptedRefs)
					throws EntityPermissionException, EntityNotDefinedException, EntityAccessOverloadException, EntityCopyrightException
			{
					handleAccessDownload(req, res, ref, copyrightAcceptedRefs);
			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	public String getLabel()
	{
		return null;
	}
	
	/**
	 * Process the access request for a download (not CHS private docs).
	 * 
	 * @param req
	 * @param res
	 * @param ref
	 * @param copyrightAcceptedRefs
	 * @throws PermissionException
	 * @throws IdUnusedException
	 * @throws ServerOverloadException
	 * @throws CopyrightException
	 */
	@SuppressWarnings("rawtypes")
	protected void handleAccessDownload(HttpServletRequest req, HttpServletResponse res, Reference ref, Collection copyrightAcceptedRefs)
			throws EntityPermissionException, EntityNotDefinedException, EntityAccessOverloadException, EntityCopyrightException
	{
		PrintStream printer = null;
		try
		{
			res.setContentType("text/csv");
			res.addHeader("Content-Disposition", "attachment; filename=\"" + ref.getId()+".csv" + "\"");

			OutputStream out = res.getOutputStream();
			printer = new PrintStream(out, true, "UTF-8");
			// export from item details 
			if ("item_detail".equals(ref.getSubType()))
			{
				// /etudesgb/item_detail/siteId/exportFileName/?item_id=&sortBy=&section=
				exportItemDetails(printer, req, ref.getContext());		
			}
			else if ("student_grades".equals(ref.getSubType()))
			{
				//  /etudesgb/student_grades/siteId/exportFileName/?itemType=&sortBy=&sortOrder=&section=&viewAssessments&showExtraCredit=
				String viewAssessments = (req.getParameter("viewAssessments") != null) ? (String)req.getParameter("viewAssessments") : null;
				if (viewAssessments == null || !"2".equals(viewAssessments))
					exportStudentGrades(printer, req, ref.getContext());	
				else
					exportStudentAssessmentGrades(printer, req, ref.getContext());				
			}
			else if ("indv_student_grades".equals(ref.getSubType()))
			{
				// /etudesgb/indv_student_grades/siteId/exportFileName/?studentUserId=&pointsHeaderText&pointsText
				exportIndvidualParticipantGrades(printer, req, ref.getContext());
			}
			
			printer.flush();
		}
		catch (Throwable e)
		{
			M_log.warn("handleAccessDownload: ", e);
		}
		finally
		{
			if (printer != null)
			{
				try
				{
					printer.close();
				}
				catch (Throwable e)
				{
					M_log.warn("closing printer: " + e.toString());
				}
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("rawtypes")
	public String merge(String siteId, Element root, String archivePath, String fromSiteId, Map attachmentNames, Map userIdTrans,
			Set userListAllowImport)
	{
		return null;
	}


	/**
	 * {@inheritDoc}
	 */
	public boolean parseEntityReference(String reference, Reference ref)
	{
		if (reference.startsWith(REFERENCE_ROOT))
		{	
			String id = null;
			String context = null;
			String subType = null;
			String[] parts = StringUtil.split(reference, Entity.SEPARATOR);
			String container = null;
			if (parts.length == 5)
			{
				subType = parts[2];
				
				// setup site id
				context = parts[3];
	
				// term id
				id = parts[4];
			}
	
			ref.set(REF_TYPE, subType, id, container, context);

			return true;
		}

		return false;
	}

	/**
	 * Remove the ":xx" seconds part of a MEDIUM date format display.
	 * 
	 * @param display
	 *        The MEDIUM formatted date.
	 * @return The MEDIUM formatted date with the seconds removed.
	 */
	protected String removeSeconds(String display)
	{
		int i = display.lastIndexOf(":");
		if ((i == -1) || ((i + 3) >= display.length())) return display;

		String rv = display.substring(0, i) + display.substring(i + 3);
		return rv;
	}

	/**
	 * @return The SiteService, via the component manager.
	 */
	protected GradebookService GradebookService()
	{
		return (GradebookService) ComponentManager.get(GradebookService.class);
	}
	
	/**
	 * @return The ServerConfigurationService, via the component manager.
	 */
	private ServerConfigurationService serverConfigurationService()
	{
		return (ServerConfigurationService) ComponentManager.get(ServerConfigurationService.class);
	}

	/**
	 * @return The SessionManager, via the component manager.
	 */
	private SessionManager sessionManager()
	{
		return (SessionManager) ComponentManager.get(SessionManager.class);
	}
	
	/**
	 * @return The SiteService, via the component manager.
	 */
	private SiteService siteService()
	{
		return (SiteService) ComponentManager.get(SiteService.class);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean willArchiveMerge()
	{
		return false;
	}
}
