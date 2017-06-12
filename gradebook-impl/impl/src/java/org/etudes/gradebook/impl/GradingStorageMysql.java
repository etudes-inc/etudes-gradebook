/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-impl/impl/src/java/org/etudes/gradebook/impl/GradingStorageMysql.java $
 * $Id: GradingStorageMysql.java 12452 2016-01-06 00:29:51Z murthyt $
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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.gradebook.api.Gradebook;
import org.etudes.gradebook.api.Gradebook.ReleaseGrades;
import org.etudes.gradebook.api.GradebookCategory;
import org.etudes.gradebook.api.GradebookCategory.WeightDistribution;
import org.etudes.gradebook.api.GradebookCategoryItemMap;
import org.etudes.gradebook.api.GradingScale;
import org.etudes.gradebook.api.GradingScalePercent;
import org.etudes.gradebook.api.UserGrade;

public class GradingStorageMysql extends GradingStorageSql
{
	/** Our logger. */
	private static Log logger = LogFactory.getLog(GradingStorageMysql.class);
	
	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		// if we are auto-creating our schema, check and create
		if (autoDdl)
		{
			// database tables
			this.sqlService.ddl(this.getClass().getClassLoader(), "gradebook_mysql_db");
			
			// default data
			this.sqlService.ddl(this.getClass().getClassLoader(), "gradebook_default_data");
		}
		
		logger.info("init()");
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected void insertContextCategories(Gradebook gradebook)
	{
		if (gradebook == null)
		{
			return;
		}
		
		for (GradebookCategory gradebookCategory : gradebook.getGradebookCategories())
		{
			((GradebookCategoryImpl)gradebookCategory).setGradebookId(gradebook.getId());
			insertContextCategory(gradebookCategory);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected int insertContextCategory(GradebookCategory gradebookCategory)
	{
		if (gradebookCategory.getGradebookId() <= 0)
		{
			throw new IllegalArgumentException("gradebook id is missing");
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO gradebook_context_categories (GRADEBOOK_ID, TITLE, WEIGHT, WEIGHT_DISTRIBUTION, DROP_NUMBER_OF_LOWEST_SCORES, IS_EXTRA_CREDIT, CATEGORY_ORDER, CATEGORY_TYPE, STANDARD_CATEGORY_CODE, CREATED_BY_USER, CREATED_DATE, MODIFIED_BY_USER, MODIFIED_DATE) ");
		sql.append("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		
		Date now = new Date();
		
		Object[] fields = new Object[13];
		int i = 0;		
		Long id = null;
		

		i = 0;
		id = null;
		
		fields[i++] = gradebookCategory.getGradebookId();
		fields[i++] = gradebookCategory.getTitle();
		fields[i++] = gradebookCategory.getWeight();			
		if (gradebookCategory.getWeight() != null)
		{
			if (gradebookCategory.getWeightDistribution() != null)
			{
				fields[i++] = gradebookCategory.getWeightDistribution().getCode();
			}
			else
			{
				fields[i++] = WeightDistribution.Points.getCode();
			}
		}
		else
		{
			fields[i++] = null;
		}
		fields[i++] = gradebookCategory.getDropNumberLowestScores();
		fields[i++] = (gradebookCategory.isExtraCredit() ? 1: 0);
		fields[i++] = gradebookCategory.getOrder();
		fields[i++] = gradebookCategory.getCategoryType().getCode();
		fields[i++] = gradebookCategory.getStandardCategoryCode();
		fields[i++] = gradebookCategory.getCreatedByUserId();
		fields[i++] = new Timestamp(now.getTime());
		fields[i++] = null;
		fields[i++] = null;			
		
		try
		{
			id = this.sqlService.dbInsert(null, sql.toString(), fields, "ID");
		}
		catch (Exception e)
		{
			if (logger.isErrorEnabled())
			{
				logger.error(e, e);
			}
		}
		
		if (id == null)
		{
			throw new RuntimeException("insertContextCategory: dbInsert failed");
		}
		
		((GradebookCategoryImpl)gradebookCategory).setId(id.intValue());
		
		return id.intValue();
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected Gradebook insertContextGradebookTx(final String context, final String userId)
	{
		final ArrayList<Gradebook> gradebooks = new ArrayList<Gradebook>();
		
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				/* add context gradebook and default grading scale if not existing */				
				
				// check for existing gradebook record in the database for the context
				Gradebook gradebook = selectContextGradebook(context);
				
				if (gradebook == null)
				{
					GradingScale defaultGradingScale = selectDefaultGradingScale();
					
					if (defaultGradingScale == null)
					{
						new RuntimeException("Default grading scale is missing.");
					}
					
					// add gradebook and default/assigned grading scale
					gradebook = new GradebookImpl();
					((GradebookImpl)gradebook).setContext(context);
					((GradebookImpl)gradebook).setShowLetterGrade(true);
					((GradebookImpl)gradebook).setReleaseGrades(ReleaseGrades.Released);
					((GradebookImpl)gradebook).setCreatedByUserId(userId);
					
					// set default scaling grade
					((GradebookImpl)gradebook).setGradingScale(defaultGradingScale);
					
					/* add other available grading scales */
					// grading scale - LetterGrade
					GradingScale gradingScaleLetterGrade = selectDefaultGradingScaleByType(GradingScale.GradingScaleType.LetterGrade.getScaleType());
					((GradebookImpl)gradebook).getContextGradingScales().add(gradingScaleLetterGrade);
					
					// grading scale - PassNotPass
					GradingScale gradingScalePassNoPass = selectDefaultGradingScaleByType(GradingScale.GradingScaleType.PassNotPass.getScaleType());
					((GradebookImpl)gradebook).getContextGradingScales().add(gradingScalePassNoPass);
					
					// add standard types
					((GradebookImpl)gradebook).setCategoryType(Gradebook.CategoryType.Standard);
					for (Gradebook.StandardCategory standardCategory : Gradebook.StandardCategory.values())
					{
						GradebookCategoryImpl gradebookCategory = new GradebookCategoryImpl();
						gradebookCategory.setTitle(standardCategory.getTitle().trim());
						gradebookCategory.setExtraCredit(standardCategory.isExtraCreditCategory());
						gradebookCategory.setCategoryType(Gradebook.CategoryType.Standard);
						gradebookCategory.setStadardCategoryCode(standardCategory.getCode());
						gradebookCategory.setOrder(standardCategory.getOrder());
						gradebookCategory.setCreatedByUserId(userId);
						
						gradebook.getGradebookCategories().add(gradebookCategory);
					}
					
					for (Gradebook.StandardCategory standardCategory : Gradebook.StandardCategory.values())
					{
						GradebookCategoryImpl gradebookCategory = new GradebookCategoryImpl();
						gradebookCategory.setTitle(standardCategory.getTitle().trim());
						gradebookCategory.setExtraCredit(standardCategory.isExtraCreditCategory());
						gradebookCategory.setCategoryType(Gradebook.CategoryType.Custom);
						gradebookCategory.setStadardCategoryCode(standardCategory.getCode());
						gradebookCategory.setOrder(standardCategory.getOrder());
						gradebookCategory.setCreatedByUserId(userId);
						
						gradebook.getGradebookCategories().add(gradebookCategory);
					}
					int id = insertGradebook(gradebook);
					
					gradebooks.add(selectContextGradebook(id));
				}					
			}		
		}, "insertContextGradebookTx: " + context);
		
		if (gradebooks.size() == 1)
		{
			return gradebooks.get(0);
		}
		
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected void insertContextGradingScales(Gradebook gradebook)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO gradebook_context_grading_scale_grades (GRADEBOOK_ID, GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE) ");
		sql.append("VALUES (?, ?, ?, ?, ?)");
		
		Object[] fields = new Object[5];
		int i = 0;		
		Long id = null;
		
		for (GradingScale contextGradingScale : gradebook.getContextGradingScales())
		{
			for (GradingScalePercent gradingScalePercent : contextGradingScale.getGradingScalePercent())
			{
				i = 0;
				id = null;
				
				fields[i++] = gradebook.getId();
				fields[i++] = contextGradingScale.getId();
				fields[i++] = gradingScalePercent.getPercent();
				fields[i++] = gradingScalePercent.getLetterGrade();
				fields[i++] = gradingScalePercent.getSequenceNumber();
				
				try
				{
					id = this.sqlService.dbInsert(null, sql.toString(), fields, "ID");
				}
				catch (Exception e)
				{
					if (logger.isErrorEnabled())
					{
						logger.error(e, e);
					}
				}
				
				if (id == null)
				{
					throw new RuntimeException("insertContextGradingScales: dbInsert failed");
				}
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected int insertGradebook(Gradebook gradebook)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO gradebook_gradebook (CONTEXT, SELECTED_GRADING_SCALE_ID, SHOW_LETTER_GRADE, RELEASE_GRADES_TYPE, CATEGORY_TYPE, VERSION, CREATED_BY_USER, CREATED_DATE, MODIFIED_BY_USER, MODIFIED_DATE) ");
		sql.append("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		
		Date now = new Date();
		
		Object[] fields = new Object[10];
		int i = 0;
		fields[i++] = gradebook.getContext();
		fields[i++] = gradebook.getGradingScale().getId();
		fields[i++] = gradebook.isShowLetterGrade() ? 1 : 0;
		if (gradebook.getReleaseGrades() == null)
		{
			fields[i++] = ReleaseGrades.Released.getCode();
		}
		else if (gradebook.getReleaseGrades() == ReleaseGrades.All)
		{
			fields[i++] = ReleaseGrades.All.getCode();
		}
		else if (gradebook.getReleaseGrades() == ReleaseGrades.Released)
		{
			fields[i++] = ReleaseGrades.Released.getCode();
		}
		fields[i++] = gradebook.getCategoryType().getCode();
		fields[i++] = 1;
		fields[i++] = gradebook.getCreatedByUserId();
		fields[i++] = new Timestamp(now.getTime());
		fields[i++] = null;
		fields[i++] = null;
		
		Long id = null;
		try
		{
			id = this.sqlService.dbInsert(null, sql.toString(), fields, "ID");
		}
		catch (Exception e)
		{
			if (logger.isErrorEnabled())
			{
				logger.error(e, e);
			}
		}
		
		if (id == null)
		{
			throw new RuntimeException("insertGradebook: dbInsert failed");
		}
		
		((GradebookImpl)gradebook).setId(id.intValue());
		
		// context default grading scale
		insertGradingScale(gradebook);
		
		// add other default grading scales (context default grading scale already added)
		insertContextGradingScales(gradebook);
		
		// insert standard categories
		insertContextCategories(gradebook);
		
		return id.intValue();
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected int insertGradebookCategoryItemMap(GradebookCategoryItemMap gradebookCategoryItemMap)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO gradebook_context_category_item_map (CATEGORY_ID, ITEM_ID, ITEM_ORDER) ");
		sql.append("VALUES (?, ?, ?)");
		
		Object[] fields = new Object[3];
		int i = 0;		
		Long id = null;
		
		fields[i++] = gradebookCategoryItemMap.getCategoryId();
		fields[i++] = gradebookCategoryItemMap.getItemId();
		fields[i++] = gradebookCategoryItemMap.getDisplayOrder();
		
		try
		{
			id = this.sqlService.dbInsert(null, sql.toString(), fields, "ID");
		}
		catch (Exception e)
		{
			if (logger.isErrorEnabled())
			{
				logger.error(e, e);
			}
		}
		
		if (id == null)
		{
			throw new RuntimeException("insertGradebookCategoryItemMap: dbInsert failed");
		}
		
		return id.intValue();
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected void insertGradingScale(Gradebook gradebook)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO gradebook_context_grading_scale_grades (GRADEBOOK_ID, GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE) ");
		sql.append("VALUES (?, ?, ?, ?, ?)");
		
		Object[] fields = new Object[5];
		int i = 0;		
		Long id = null;
		
		for (GradingScalePercent gradingScalePercent : gradebook.getGradingScale().getGradingScalePercent())
		{
			i = 0;
			id = null;
			
			if ((gradebook.getId() <=  0) || (gradebook.getGradingScale().getId() <= 0) || (gradingScalePercent.getPercent() == null) || (gradingScalePercent.getLetterGrade() == null))
			{
				continue;		
			}
			
			fields[i++] = gradebook.getId();
			fields[i++] = gradebook.getGradingScale().getId();
			fields[i++] = gradingScalePercent.getPercent();
			fields[i++] = gradingScalePercent.getLetterGrade();
			fields[i++] = gradingScalePercent.getSequenceNumber();
			
			try
			{
				id = this.sqlService.dbInsert(null, sql.toString(), fields, "ID");
			}
			catch (Exception e)
			{
				if (logger.isErrorEnabled())
				{
					logger.error(e, e);
				}
			}
			
			if (id == null)
			{
				throw new RuntimeException("insertDefaultGradingScale: dbInsert failed");
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected int insertInstructorUserNotes(int gradebookId, String studentId, String notes, String addedByUserId)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO gradebook_instructor_student_notes(GRADEBOOK_ID, STUDENT_ID, NOTES, ADDED_BY_USER, ADDED_DATE) ");
		sql.append("VALUES (?, ?, ?, ?, ?)");
		
		Date now = new Date();
		
		Object[] fields = new Object[5];
		int i = 0;
		fields[i++] = gradebookId;
		fields[i++] = studentId;
		fields[i++] = notes;
		fields[i++] = addedByUserId;
		fields[i++] = new Timestamp(now.getTime());
		
		Long id = null;
		try
		{
			id = this.sqlService.dbInsert(null, sql.toString(), fields, "ID");
		}
		catch (Exception e)
		{
			if (logger.isErrorEnabled())
			{
				logger.error(e, e);
			}
		}
		
		if (id == null)
		{
			throw new RuntimeException("insertIntructorUserNotes: dbInsert failed");
		}
		
		return id.intValue();
	}	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void insertUpdateUserGradesHistory(int gradebookId, String studentId, String letterGrade, String assignedByUserId, Date assignedDate)
	{
		
		if ((gradebookId <= 0) || (studentId == null || studentId.trim().length() == 0) || (assignedByUserId == null || assignedByUserId.trim().length() == 0))
		{
			return;
		}
		// update if there is existing record else add new		
		UserGrade exisUserGradeHist = selectUserGradeHistory(gradebookId, studentId);
		
		Date now = new Date();
		
		if (exisUserGradeHist != null)
		{
			// update
			String sql = new String("UPDATE gradebook_user_grades_history SET ASSIGNED_LETTER_GRADE = ?, ASSIGNED_BY_USER = ?, ASSIGNED_DATE = ? WHERE GRADEBOOK_ID = ? AND STUDENT_ID = ?");					
					
			Object[] fields = new Object[5];
			
			int i = 0;
			fields[i++] = letterGrade;
			fields[i++] = assignedByUserId;
			if (assignedDate == null)
			{
				fields[i++] = new Timestamp(now.getTime());
			}
			else
			{
				fields[i++] = new Timestamp(assignedDate.getTime());
			}
			fields[i++] = gradebookId;
			fields[i++] = studentId;
			
			try
			{
				sqlService.dbWrite(sql, fields);
			}
			catch (Exception e)
			{
				if (logger.isErrorEnabled())
				{
					logger.error(e, e);
				}
				
				throw new RuntimeException("insertUpdateUserGradesHistory: update existing record: dbWrite failed");
			}
		}
		else
		{
			StringBuilder sql = new StringBuilder();
			sql.append("INSERT INTO gradebook_user_grades_history(GRADEBOOK_ID, STUDENT_ID, ASSIGNED_LETTER_GRADE, ASSIGNED_BY_USER, ASSIGNED_DATE) ");
			sql.append("VALUES (?, ?, ?, ?, ?)");
						
			Object[] fields = new Object[5];
			int i = 0;
			fields[i++] = gradebookId;
			fields[i++] = studentId;
			fields[i++] = letterGrade;
			fields[i++] = assignedByUserId;
			if (assignedDate == null)
			{
				fields[i++] = new Timestamp(now.getTime());
			}
			else
			{
				fields[i++] = new Timestamp(assignedDate.getTime());
			}
			
			Long id = null;
			try
			{
				id = this.sqlService.dbInsert(null, sql.toString(), fields, "ID");
			}
			catch (Exception e)
			{
				if (logger.isErrorEnabled())
				{
					logger.error(e, e);
				}
			}
			
			if (id == null)
			{
				throw new RuntimeException("insertUpdateUserGradesHistory: dbInsert failed");
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected int insertUserGrades(int gradebookId, String studentId, String letterGrade, String assignedByUserId)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO gradebook_user_grades(GRADEBOOK_ID, STUDENT_ID, ASSIGNED_LETTER_GRADE, ASSIGNED_BY_USER, ASSIGNED_DATE) ");
		sql.append("VALUES (?, ?, ?, ?, ?)");
		
		Date now = new Date();
		
		Object[] fields = new Object[5];
		int i = 0;
		fields[i++] = gradebookId;
		fields[i++] = studentId;
		fields[i++] = letterGrade;
		fields[i++] = assignedByUserId;
		fields[i++] = new Timestamp(now.getTime());
		
		Long id = null;
		try
		{
			id = this.sqlService.dbInsert(null, sql.toString(), fields, "ID");
		}
		catch (Exception e)
		{
			if (logger.isErrorEnabled())
			{
				logger.error(e, e);
			}
		}
		
		if (id == null)
		{
			throw new RuntimeException("insertUserGrades: dbInsert failed");
		}
		
		return id.intValue();
	}
}
