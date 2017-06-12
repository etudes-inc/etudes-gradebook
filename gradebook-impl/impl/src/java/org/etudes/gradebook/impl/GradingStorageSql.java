/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-impl/impl/src/java/org/etudes/gradebook/impl/GradingStorageSql.java $
 * $Id: GradingStorageSql.java 12452 2016-01-06 00:29:51Z murthyt $
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.gradebook.api.Gradebook;
import org.etudes.gradebook.api.Gradebook.CategoryType;
import org.etudes.gradebook.api.Gradebook.ReleaseGrades;
import org.etudes.gradebook.api.GradebookCategory;
import org.etudes.gradebook.api.GradebookCategory.WeightDistribution;
import org.etudes.gradebook.api.GradebookCategoryItemMap;
import org.etudes.gradebook.api.GradebookService;
import org.etudes.gradebook.api.GradingScale;
import org.etudes.gradebook.api.GradingScale.GradingScaleType;
import org.etudes.gradebook.api.GradingScalePercent;
import org.etudes.gradebook.api.Notes;
import org.etudes.gradebook.api.UserGrade;
import org.sakaiproject.db.api.SqlReader;
import org.sakaiproject.db.api.SqlService;

public abstract class GradingStorageSql implements GradingStorage
{
	/** logger **/
	private static Log logger = LogFactory.getLog(GradingStorageSql.class);
	
	/** Configuration: to run the ddl on init or not. */
	protected boolean autoDdl = false;
	
	/** Dependency: GradebookService. */
	protected GradebookService gradebookService = null;
	
	/** Dependency: SqlService. */
	protected SqlService sqlService = null;
	
	
	/**
	 * {@inheritDoc}
	 */
	/*public void addModifyDeleteGradebookCategories(String context, List<GradebookCategory> gradebookCategories, String modifiedByUserId)
	{
		 //if all gradebookCategories are needs to be removed gradebookCategories size should be zero, modified category will be updated, 
		 //if existing category is to be deleted remove from the gradebookCategories list, 
		 //if id is -1(newGradebookCategory(String title, Float weight, WeightDistribution weightDistribution, int order)) or zero new category will be created 
		
		if ((context == null || context.trim().length() == 0) || (gradebookCategories == null) || (modifiedByUserId == null || modifiedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing");
		}
		
		Gradebook gradebook = selectContextGradebook(context);
		
		if (gradebook != null)
		{
			addModifyDeleteGradebookCategoriesTx(gradebook, gradebookCategories, modifiedByUserId);
		}		
	}*/
	
	/**
	 * {@inheritDoc}
	 */
	public void addModifyDeleteGradebookCategories(String context, Gradebook.CategoryType categoryType, List<GradebookCategory> gradebookCategories, String modifiedByUserId)
	{
		/* if all gradebookCategories are needs to be removed gradebookCategories size should be zero, modified category will be updated, 
		 * if existing category is to be deleted remove from the gradebookCategories list, 
		 * if id is -1(newGradebookCategory(String title, Float weight, WeightDistribution weightDistribution, int order)) or zero new category will be created */
		
		if ((context == null || context.trim().length() == 0) || (categoryType == null) || (gradebookCategories == null) || (modifiedByUserId == null || modifiedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing");
		}
		
		Gradebook gradebook = selectContextGradebook(context);
		
		if (gradebook != null)
		{
			addModifyDeleteGradebookCategoriesTx(gradebook, categoryType, gradebookCategories, modifiedByUserId);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void addModifyDeleteGradebookCategoryMappedItems(String context, Gradebook.CategoryType categoryType, List<GradebookCategoryItemMap> contextCategoryItems)
	{
		if ((context == null || context.trim().length() == 0) || (categoryType == null) || (contextCategoryItems == null))
		{
			throw new IllegalArgumentException("information is missing");
		}
		
		Gradebook gradebook = selectContextGradebook(context);
		
		if (gradebook != null)
		{
			addUpdateDeleteGradebookCategoriesItemsMapTx(gradebook.getId(), categoryType, contextCategoryItems);
		}
	}
	
	/**
	 * Returns to uninitialized state.
	 */
	public void destroy()
	{
		logger.info("destroy()");
	}
	
	/**
	 * {@inheritDoc}
	 */
	/*
	public void addModifyDeleteGradebookCategoryMappedItems(String context, List<GradebookCategoryItemMap> contextCategoryItems)
	{
		if ((context == null || context.trim().length() == 0) || (contextCategoryItems == null))
		{
			throw new IllegalArgumentException("information is missing");
		}
		
		Gradebook gradebook = selectContextGradebook(context);
		
		if (gradebook != null)
		{
			addUpdateDeleteGradebookCategoriesItemsMapTx(gradebook.getId(), gradebook.getCategoryType(), contextCategoryItems);
		}
	}
	*/
	

	/**
	 * {@inheritDoc}
	 */
	public void insertUpdateInstructorUserNotes(final int gradebookId, final Notes instructorUserNotes)
	{
		if ((instructorUserNotes == null) || (instructorUserNotes.getUserId() == null || instructorUserNotes.getUserId().trim().length() == 0)
				|| (instructorUserNotes.getAddedByUserId() == null || instructorUserNotes.getAddedByUserId().trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing");
		}
		
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{				
				final Notes exisUserNote = selectUserNotes(gradebookId, instructorUserNotes.getUserId());
				
				if (exisUserNote == null)
				{
					if (instructorUserNotes.getNotes() == null || instructorUserNotes.getNotes().trim().length() == 0)
					{
						return;
					}
					insertInstructorUserNotes(gradebookId, instructorUserNotes.getUserId(), instructorUserNotes.getNotes(), instructorUserNotes.getAddedByUserId());
				}
				else
				{
					// log and update
					try
					{
						// first check the dates, both should be the same else stale data(already updated)
						if (exisUserNote.getDateModified() == null)
						{
							if (instructorUserNotes.getDateAdded() != null && (instructorUserNotes.getDateAdded().compareTo(exisUserNote.getDateAdded()) == 0))
							{
								updateDeleteInstructorUserNotes(gradebookId, instructorUserNotes, exisUserNote);
							}
						}
						else if (instructorUserNotes.getDateAdded() != null && (instructorUserNotes.getDateAdded().compareTo(exisUserNote.getDateModified()) == 0))
						{
							updateDeleteInstructorUserNotes(gradebookId, instructorUserNotes, exisUserNote);
						}
					}
					catch (Exception e)
					{
						if (logger.isErrorEnabled())
						{
							logger.error(e.toString(), e);
						}
						
						throw new RuntimeException("Error while updating user note.", e);
					}
				}				
			}
		}, "insertUpdateInstructorUserNotes(final int gradebookId, final Notes instructorUserNotes): " + gradebookId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Gradebook selectGradebook(int gradebookId)
	{
		if (gradebookId <= 0)
		{
			return null;
		}
		
		return selectContextGradebook(gradebookId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Gradebook selectGradebook(String context, String userId)
	{
		if ((context == null || context.trim().length() == 0) || (userId == null || userId.trim().length() == 0))
		{
			throw new IllegalArgumentException("context or user information is missing");
		}
		
		Gradebook gradebook = selectContextGradebook(context);
		
		if (gradebook == null)
		{
			// add context gradebook, default grading scale and standard types if not existing for the context
			gradebook = insertContextGradebookTx(context, userId);
		}

		// add context and user id to gradebook category
		List<GradebookCategory> gradebookCategories = gradebook.getGradebookCategories();
		for (GradebookCategory gradebookCategory : gradebookCategories)
		{
			((GradebookCategoryImpl)gradebookCategory).fetchedByUserId = userId;
			((GradebookCategoryImpl)gradebookCategory).context = context;
		}

		return gradebook;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public GradebookCategory selectGradebookCategory(int categoryId)
	{
		if (categoryId <= 0)
		{
			return null;
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ID, GRADEBOOK_ID, TITLE, WEIGHT, WEIGHT_DISTRIBUTION, DROP_NUMBER_OF_LOWEST_SCORES, IS_EXTRA_CREDIT, CATEGORY_ORDER, STANDARD_CATEGORY_CODE, CREATED_BY_USER, CREATED_DATE, MODIFIED_BY_USER, MODIFIED_DATE ");
		sql.append("FROM gradebook_context_categories ");
		sql.append("WHERE ID = ? ");
		Object[] fields = new Object[1];
		int i = 0;
		fields[i++] = categoryId;
		
		final List<GradebookCategory> gradebookCategoryList = new ArrayList<GradebookCategory>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					GradebookCategoryImpl gradebookCategory = fillGradebookCategory(result);
					
					gradebookCategoryList.add(gradebookCategory);
					
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectGradebookCategory: " + e, e);
					}
					return null;
				}
			}
		});
		
		GradebookCategory gradebookCategory = null;
		
		if (gradebookCategoryList.size() == 1)
		{
			gradebookCategory = gradebookCategoryList.get(0);
		}
		
		return gradebookCategory;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<GradebookCategoryItemMap> selectGradebookCategoryMappedItems(int gradebookId, CategoryType gradebookCategoryType)
	{
		if (gradebookId <= 0 || gradebookCategoryType == null)
		{
			throw new IllegalArgumentException("id or category type is missing");
		}
		
		StringBuilder sql = new StringBuilder();
		
		sql.append("SELECT gbcm.ID , gbcm.CATEGORY_ID, gbcm.ITEM_ID, gbcm.ITEM_ORDER ");
		sql.append("FROM gradebook_context_category_item_map gbcm, gradebook_context_categories gbc ");
		sql.append("WHERE gbcm.CATEGORY_ID = gbc.ID ");
		sql.append("AND gbc.GRADEBOOK_ID = ? AND gbc.CATEGORY_TYPE = ?");
		
		Object[] fields = new Object[2];
		int i = 0;
		fields[i++] = gradebookId;
		fields[i++] = gradebookCategoryType.getCode();
		
		final List<GradebookCategoryItemMap> gradebookCategoryItemMapList = new ArrayList<GradebookCategoryItemMap>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					GradebookCategoryItemMapImpl gradebookCatItemMap = new GradebookCategoryItemMapImpl();
					
					gradebookCatItemMap.setId(result.getInt("ID"));
					gradebookCatItemMap.setCategoryId(result.getInt("CATEGORY_ID"));
					gradebookCatItemMap.setItemId(result.getString("ITEM_ID"));
					gradebookCatItemMap.setDisplayOrder(result.getInt("ITEM_ORDER"));					
					
					gradebookCategoryItemMapList.add(gradebookCatItemMap);
										
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectGradebookCategoryMappedItems: " + e, e);
					}
					return null;
				}
			}
		});
		
		return gradebookCategoryItemMapList;	
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int selectGradebookCategoryMappedItemsCount(int gradebookId, CategoryType gradebookCategoryType)
	{
		if (gradebookId <= 0 || gradebookCategoryType == null)
		{
			throw new IllegalArgumentException("id or category type is missing");
		}
		
		StringBuilder sql = new StringBuilder();
		
		sql.append("SELECT COUNT(1) AS GRADEBOOK_CAT_ITEM_MAP_COUNT ");
		sql.append("FROM gradebook_context_category_item_map gbcm, gradebook_context_categories gbc ");
		sql.append("WHERE gbcm.CATEGORY_ID = gbc.ID ");
		sql.append("AND gbc.GRADEBOOK_ID = ? AND gbc.CATEGORY_TYPE = ?");
		
		Object[] fields = new Object[2];
		int i = 0;
		fields[i++] = gradebookId;
		fields[i++] = gradebookCategoryType.getCode();
		
		final List<Integer> count = new ArrayList<Integer>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					count.add(result.getInt("GRADEBOOK_CAT_ITEM_MAP_COUNT"));
										
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectGradebookCategoryMappedItemsCount: " + e, e);
					}
					return null;
				}
			}
		});
		
		int mappedItemsCount = 0;
		
		if (count.size() == 1)
		{
			mappedItemsCount = count.get(0);
		}
		
		return mappedItemsCount;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Notes selectInstructorUserNotes(int gradebookId, String studentId)
	{
		if (gradebookId <= 0 || studentId == null || studentId.trim().length() == 0)
		{
			return null;
		}
		
		return selectUserNotes(gradebookId, studentId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Map<String, Notes> selectInstructorUsersNotes(int gradebookId)
	{
		String sql = "SELECT ID, GRADEBOOK_ID, STUDENT_ID, NOTES, ADDED_BY_USER, MODIFED_BY_USER, ADDED_DATE, MODIFIED_DATE FROM gradebook_instructor_student_notes WHERE GRADEBOOK_ID = ?";
		
		Object[] fields = new Object[1];
		int i = 0;
		fields[i++] = gradebookId;
		
		final Map<String, Notes> instructorUserNotes =  new HashMap<String, Notes>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					Notes notes = fillNotes(result);
					
					instructorUserNotes.put(notes.getUserId(), notes);
					
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectInstructorUserNotes: " + e, e);
					}
					return null;
				}
			}
		});
		
		return instructorUserNotes;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public GradebookCategory selectItemGradebookCategory(String itemId, String context)
	{
		if ((itemId == null || itemId.trim().length() == 0) || (context == null || context.trim().length() == 0))
		{
			return null;
		}
		
		Gradebook gradebook = selectContextGradebook(context);
		
		if (gradebook == null)
		{
			return null;
		}
		
		GradebookCategoryItemMap gradebookCategoryItemMap = selectGradebookCategoryMapItem(gradebook.getId(), gradebook.getCategoryType(), itemId);
		
		if (gradebookCategoryItemMap != null)
		{
			return selectGradebookCategory(gradebookCategoryItemMap.getCategoryId());
		}
		
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public UserGrade selectUserGrade(int gradebookId, String studentId)
	{
		if (gradebookId <= 0 || studentId == null || studentId.trim().length() == 0)
		{
			return null;
		}
		
		String sql = "SELECT ID, GRADEBOOK_ID, STUDENT_ID, ASSIGNED_LETTER_GRADE, ASSIGNED_BY_USER, ASSIGNED_DATE FROM gradebook_user_grades WHERE GRADEBOOK_ID = ? AND STUDENT_ID = ?";
		
		Object[] fields = new Object[2];
		int i = 0;
		fields[i++] = gradebookId;
		fields[i++] = studentId;
		
		final List<UserGrade> userGrades =  new ArrayList<UserGrade>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					UserGradeImpl userGrade = new UserGradeImpl();
					
					userGrade.setId(result.getInt("ID"));
					userGrade.setGradebookId(result.getInt("GRADEBOOK_ID"));
					userGrade.setUserId(result.getString("STUDENT_ID"));
					userGrade.setLetterGrade(result.getString("ASSIGNED_LETTER_GRADE"));
					userGrade.setAssignedByUserId(result.getString("ASSIGNED_BY_USER"));
					
					if (result.getDate("ASSIGNED_DATE") != null)
					{
						Timestamp assignedDate = result.getTimestamp("ASSIGNED_DATE");
						userGrade.setAssignedDate(assignedDate);
					}
					else
					{
						userGrade.setAssignedDate(null);
					}
					
					userGrades.add(userGrade);
					
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectUserGrade: " + e, e);
					}
					return null;
				}
			}
		});
		
		if (userGrades.size() == 1)
		{
			return userGrades.get(0);
		}
		
		return null;
	}
	
	/**
	 * Selects user grade history
	 * 
	 * @param gradebookId	Gradebook id
	 * 
	 * @param studentId	Student id
	 * 
	 * @return	The user grade history or null
	 */
	public UserGrade selectUserGradeHistory(int gradebookId, String studentId)
	{
		if (gradebookId <= 0 || studentId == null || studentId.trim().length() == 0)
		{
			return null;
		}
		
		String sql = "SELECT ID, GRADEBOOK_ID, STUDENT_ID, ASSIGNED_LETTER_GRADE, ASSIGNED_BY_USER, ASSIGNED_DATE FROM gradebook_user_grades_history WHERE GRADEBOOK_ID = ? AND STUDENT_ID = ?";
		
		Object[] fields = new Object[2];
		int i = 0;
		fields[i++] = gradebookId;
		fields[i++] = studentId;
		
		final List<UserGrade> userGrades =  new ArrayList<UserGrade>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					UserGradeImpl userGrade = new UserGradeImpl();
					
					userGrade.setId(result.getInt("ID"));
					userGrade.setGradebookId(result.getInt("GRADEBOOK_ID"));
					userGrade.setUserId(result.getString("STUDENT_ID"));
					userGrade.setLetterGrade(result.getString("ASSIGNED_LETTER_GRADE"));
					userGrade.setAssignedByUserId(result.getString("ASSIGNED_BY_USER"));
					
					if (result.getDate("ASSIGNED_DATE") != null)
					{
						Timestamp assignedDate = result.getTimestamp("ASSIGNED_DATE");
						userGrade.setAssignedDate(assignedDate);
					}
					else
					{
						userGrade.setAssignedDate(null);
					}
					
					userGrades.add(userGrade);
					
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectUserGradeHistory: " + e, e);
					}
					return null;
				}
			}
		});
		
		if (userGrades.size() == 1)
		{
			return userGrades.get(0);
		}
		
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Map<String, UserGrade> selectUserGrades(int gradebookId)
	{
		String sql = "SELECT ID, GRADEBOOK_ID, STUDENT_ID, ASSIGNED_LETTER_GRADE, ASSIGNED_BY_USER, ASSIGNED_DATE FROM gradebook_user_grades WHERE GRADEBOOK_ID = ?";
		
		Object[] fields = new Object[1];
		int i = 0;
		fields[i++] = gradebookId;
		
		final Map<String, UserGrade> userGrades =  new HashMap<String, UserGrade>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					UserGradeImpl userGrade = new UserGradeImpl();
					
					userGrade.setId(result.getInt("ID"));
					userGrade.setGradebookId(result.getInt("GRADEBOOK_ID"));
					userGrade.setUserId(result.getString("STUDENT_ID"));
					userGrade.setLetterGrade(result.getString("ASSIGNED_LETTER_GRADE"));
					userGrade.setAssignedByUserId(result.getString("ASSIGNED_BY_USER"));
					
					if (result.getDate("ASSIGNED_DATE") != null)
					{
						Timestamp assignedDate = result.getTimestamp("ASSIGNED_DATE");
						userGrade.setAssignedDate(assignedDate);
					}
					else
					{
						userGrade.setAssignedDate(null);
					}
					
					userGrades.put(userGrade.getUserId(), userGrade);
					
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectUserGrades: " + e, e);
					}
					return null;
				}
			}
		});
		
		return userGrades;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int selectUserGradesCount(int gradebookId)
	{
		String sql = "SELECT count(1) AS ASSIGNED_LETTER_GRADE_COUNT FROM gradebook_user_grades WHERE GRADEBOOK_ID = ? AND ASSIGNED_LETTER_GRADE IS NOT NULL";
		
		Object[] fields = new Object[1];
		int i = 0;
		fields[i++] = gradebookId;
		
		final List<Integer> count = new ArrayList<Integer>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					count.add(result.getInt("ASSIGNED_LETTER_GRADE_COUNT"));
										
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectUserGradesCount: " + e, e);
					}
					return null;
				}
			}
		});
		
		int userGradesCount = 0;
		
		if (count.size() == 1)
		{
			userGradesCount = count.get(0);
		}
		
		return userGradesCount;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Map<String, UserGrade> selectUserGradesHistory(int gradebookId)
	{
		String sql = "SELECT ID, GRADEBOOK_ID, STUDENT_ID, ASSIGNED_LETTER_GRADE, ASSIGNED_BY_USER, ASSIGNED_DATE FROM gradebook_user_grades_history WHERE GRADEBOOK_ID = ?";
		
		Object[] fields = new Object[1];
		int i = 0;
		fields[i++] = gradebookId;
		
		final Map<String, UserGrade> userGrades =  new HashMap<String, UserGrade>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					UserGradeImpl userGrade = new UserGradeImpl();
					
					userGrade.setId(result.getInt("ID"));
					userGrade.setGradebookId(result.getInt("GRADEBOOK_ID"));
					userGrade.setUserId(result.getString("STUDENT_ID"));
					userGrade.setLetterGrade(result.getString("ASSIGNED_LETTER_GRADE"));
					userGrade.setAssignedByUserId(result.getString("ASSIGNED_BY_USER"));
					
					if (result.getDate("ASSIGNED_DATE") != null)
					{
						Timestamp assignedDate = result.getTimestamp("ASSIGNED_DATE");
						userGrade.setAssignedDate(assignedDate);
					}
					else
					{
						userGrade.setAssignedDate(null);
					}
					
					userGrades.put(userGrade.getUserId(), userGrade);
					
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectUserGradesHistory: " + e, e);
					}
					return null;
				}
			}
		});
		
		return userGrades;
	}
	
	/**
	 * Configuration: to run the ddl on init or not.
	 * 
	 * @param value
	 *        the auto ddl value.
	 */
	public void setAutoDdl(String value)
	{
		autoDdl = new Boolean(value).booleanValue();
	}
	
	/**
	 * @param gradebookService the gradebookService to set
	 */
	public void setGradebookService(GradebookService gradebookService)
	{
		this.gradebookService = gradebookService;
	}
	
	/**
	 * Sets SqlService
	 * 
	 * @param service SqlService
	 */
	public void setSqlService(SqlService service)
	{
		this.sqlService = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public void update(Gradebook gradebook)
	{
		if (gradebook == null|| (gradebook.getId() <= 0))
		{
			throw new IllegalArgumentException("gradebook information is missing");
		}
		
		// update gradebook and grading scale
		updateTx(gradebook);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void updateGradebookAttributes(Gradebook gradebook)
	{
		if (gradebook == null|| (gradebook.getId() <= 0))
		{
			throw new IllegalArgumentException("gradebook information is missing");
		}
		
		editContextGradebookAttibutes(gradebook);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void updateGradebookCategories(String context, List<GradebookCategory> gradebookCategories, String modifiedByUserId)
	{
		if ((context == null || context.trim().length() == 0) || (gradebookCategories == null || gradebookCategories.size() == 0) || (modifiedByUserId == null || modifiedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing");
		}
		
		Gradebook gradebook = selectContextGradebook(context);
		
		if (gradebook != null)
		{
			if (gradebook.getCategoryType() == CategoryType.Standard)
			{
				gradebook.getGradebookCategories().clear();
				gradebook.getGradebookCategories().addAll(gradebookCategories);
				
				// update weights, weight distribution, order.
				editGradebookStandardCategories(gradebook, modifiedByUserId);
			}
			else if (gradebook.getCategoryType() == CategoryType.Custom)
			{
				gradebook.getGradebookCategories().clear();
				gradebook.getGradebookCategories().addAll(gradebookCategories);
				
				// update title, weights and weight distribution
				editGradebookCustomCategories(gradebook, modifiedByUserId);
			} 
			
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void updateGradebookCategoriesDropLowestScoresNumber(String context, List<GradebookCategory> gradebookCategories, String modifiedByUserId)
	{
		if ((context == null || context.trim().length() == 0) || (gradebookCategories == null || gradebookCategories.size() == 0) || (modifiedByUserId == null || modifiedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing");
		}
		
		Gradebook gradebook = selectContextGradebook(context);
		
		if (gradebook != null)
		{
			gradebook.getGradebookCategories().clear();
			gradebook.getGradebookCategories().addAll(gradebookCategories);
			
			// update number of drop lowest scores
			editGradebookCategoryDropLowerstScoresNumber(gradebook, modifiedByUserId);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void updateGradebookCategoryType(String context, Gradebook.CategoryType categoryType, String modifiedByUserId)
	{
		/* if category type is changed from standard to custom keep all existing standard categories and add default standard categories for custom type to begin with if they don't exist. If category type 
		 * is changed from custom to standard keep all the existing custom categories*/
		if ((context == null || context.trim().length() == 0) || (categoryType == null) || (modifiedByUserId == null || modifiedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing");
		}
		
		Gradebook gradebook = selectContextGradebook(context);
		if (gradebook == null || gradebook.getCategoryType() == categoryType)
		{
			return;
		}
		
		updateCategoryTypeTx(gradebook, categoryType, modifiedByUserId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void updateGradingScale(int gradebookId, GradingScale gradingScale)
	{
		if (gradebookId <= 0 || (gradingScale == null || gradingScale.getId() <= 0 || gradingScale.getGradingScalePercent().size() == 0))
		{
			throw new IllegalArgumentException("gradebook information is missing");
		}
		
		updateGradingScaleTx(gradebookId, gradingScale);;
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void updateItemCategoryMap(String context, String itemId, int categoryId)
	{
		if ((context == null || context.trim().length() == 0) || (itemId == null || itemId.trim().length() == 0) || (categoryId <= 0))
		{
			return;
		}
		
		Gradebook gradebook = selectContextGradebook(context);
		
		if (gradebook != null)
		{
			boolean foundCategory = false;
			CategoryType categoryType = null;
			
			List<GradebookCategory> gradebookCategories = gradebook.getGradebookCategories();
			
			for (GradebookCategory gradebookCategory : gradebookCategories)
			{
				if (gradebookCategory.getId() == categoryId)
				{
					foundCategory = true;
					categoryType = gradebookCategory.getCategoryType();
					break;
				}
			}
			
			// category id should belong to the gradebook
			if (foundCategory)
			{
				GradebookCategoryItemMap exisGradebookCategoryItemMap = selectGradebookCategoryMapItem(gradebook.getId(), categoryType, itemId);
				
				if (exisGradebookCategoryItemMap != null)
				{
					// update is changes are made
					if (exisGradebookCategoryItemMap.getCategoryId() != categoryId)
					{
						// count items in the category items and give max order
						int displayOrder = selectCategoryMappedItemsCount(categoryId);
						
						updateGradebookCategoryMappedItem(exisGradebookCategoryItemMap.getId(), categoryId, displayOrder++);
					}
				}
			}
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void updateUserGrades(final int gradebookId, List<UserGrade> userLetterGrades, final String assignedByUserId)
	{
		if ((userLetterGrades == null || userLetterGrades.size() == 0) || (assignedByUserId == null || assignedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing");
		}
		
		// save the letter grade if the assigned date is same as in database for existing record. If the date in database is different(later date) the record may be updated after fetching
		Map<String, UserGrade> exisUserGrades = selectUserGrades(gradebookId);
		
		Set<String> exisUserGradeUserIds = exisUserGrades.keySet();
		
		for (final UserGrade userGrade : userLetterGrades)
		{
			String userId = userGrade.getUserId();
			
			if (exisUserGradeUserIds.contains(userId))
			{
				final UserGrade exisUserGrade = exisUserGrades.get(userId);
				
				// first check the dates, both should be the same else stale data(already updated)
				if (userGrade.getAssignedDate() != null && (userGrade.getAssignedDate().compareTo(exisUserGrade.getAssignedDate()) == 0))
				{
					// if grade is changed log the current grade and update
					if (userGrade.getLetterGrade() == null && exisUserGrade.getLetterGrade() == null)
					{
						continue;
					}
					
					if ((userGrade.getLetterGrade() != null && exisUserGrade.getLetterGrade() == null)
						|| (userGrade.getLetterGrade() == null && exisUserGrade.getLetterGrade() != null)
						|| (!userGrade.getLetterGrade().equalsIgnoreCase(exisUserGrade.getLetterGrade())))
					{
						this.sqlService.transact(new Runnable()
						{
							public void run()
							{
								try
								{
									// log and update
									insertUpdateUserGradesHistory(exisUserGrade.getGradebookId(), exisUserGrade.getUserId(), exisUserGrade.getLetterGrade(), exisUserGrade.getAssignedByUserId(), exisUserGrade.getAssignedDate());
									String sql = new String("UPDATE gradebook_user_grades SET ASSIGNED_LETTER_GRADE = ?, ASSIGNED_BY_USER = ?, ASSIGNED_DATE = ? WHERE GRADEBOOK_ID = ? AND STUDENT_ID = ?");
									Object[] fields = null;
									
									fields = new Object[5];
									int i = 0;
									fields[i++] = userGrade.getLetterGrade();
									fields[i++] = assignedByUserId;
									fields[i++] = new Timestamp(new Date().getTime());
									fields[i++] = exisUserGrade.getGradebookId();
									fields[i++] = userGrade.getUserId();
									
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
										
										throw new RuntimeException("updateUserGrades(int gradebookId, List<UserGrade> userLetterGrades, String assignedByUserId): dbWrite failed");
									}
								}
								catch (Exception e)
								{
									if (logger.isErrorEnabled())
									{
										logger.error(e.toString(), e);
									}
									
									throw new RuntimeException("Error while inserting user grades.", e);
								}
							}
						}, "updateUserGrades(int gradebookId, List<UserGrade> userLetterGrades, String assignedByUserId): " + gradebookId);
							
					}
				}
			}
			else
			{
				// save(insert) the letter grade
				if (userGrade.getLetterGrade() != null && userGrade.getLetterGrade().trim().length() > 0)
				{
					insertUserGrades(gradebookId, userId, userGrade.getLetterGrade(), assignedByUserId);
				}
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void updateUserGrades(int gradebookId, Map<String, String> userLetterGrades, String assignedByUserId)
	{
		if ((gradebookId <= 0) || (assignedByUserId == null || assignedByUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("information is missing");
		}
		
		editUserGradesTx(gradebookId, userLetterGrades, assignedByUserId);
	}
	
	/**
	 * Adds new categories, updates existing categories and deletes existing catgories if they are not in gradebookCategories
	 * 
	 * @param gradebook		Gradebook
	 * 
	 * @param gradebookCategories	Gradebook Categories
	 * 
	 * @param modifiedByUserId	Modified by userid
	 */
	/*protected void addModifyDeleteGradebookCategoriesTx(final Gradebook gradebook, final List<GradebookCategory> gradebookCategories, final String modifiedByUserId)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				try
				{
					List<GradebookCategory> existingGradebookCategories = gradebook.getGradebookCategories();
					Map<Integer, GradebookCategory> existingGradebookCategoriesMap = new HashMap<Integer, GradebookCategory>();
					Set<Integer> existingGradebookCategoryIds = new HashSet<Integer>();
					
					for (GradebookCategory exisGradebookCategory : existingGradebookCategories)
					{
						existingGradebookCategoriesMap.put(exisGradebookCategory.getId(), exisGradebookCategory);
						existingGradebookCategoryIds.add(exisGradebookCategory.getId());
					}
					
					Map<String, Integer> standardCategoriesMap = new HashMap<String, Integer>();
					Set<Integer> modGradebookCategoriesSet = new HashSet<Integer>();
					if (gradebook.getCategoryType() == CategoryType.Custom)
					{
						// to make newly added category as standard category if added with same as standard category title and there is no similar standard category in the gradebook categories
						// standard categories
						for (Gradebook.StandardCategory standardCategory : Gradebook.StandardCategory.values())
						{
							standardCategoriesMap.put(standardCategory.getTitle(), standardCategory.getCode());
						}
						
						// existing standard categories in the gradebook
						for (GradebookCategory gradebookCategory : gradebookCategories)
						{
							if (gradebookCategory.getId() > 0 && gradebookCategory.getStandardCategoryCode() > 0)
							{
								modGradebookCategoriesSet.add(gradebookCategory.getStandardCategoryCode());
							}
						}
						
					}
					
					for (GradebookCategory gradebookCategory : gradebookCategories)
					{
						// blank titles not allowed
						if (gradebookCategory.getTitle() == null || gradebookCategory.getTitle().trim().length() == 0)
						{
							continue;
						}
						
						// Standard categories cannot be added or deleted but can be modified. Custom categories can be added or deleted, can be modified
						if ((gradebookCategory.getId() == -1 || gradebookCategory.getId() == 0) && (gradebook.getCategoryType() == CategoryType.Custom || gradebookCategory.getCategoryType()  == CategoryType.Custom))
						{
							((GradebookCategoryImpl)gradebookCategory).setGradebookId(gradebook.getId());
							((GradebookCategoryImpl)gradebookCategory).setCreatedByUserId(modifiedByUserId);
							((GradebookCategoryImpl)gradebookCategory).setCategoryType(CategoryType.Custom);
							
							 sets standard category code - check for title and if standard category with that title is not existing in the gradebook categories check for the other titles with same standard code then set 
								standard category code if gradebook has no such categories with this standard code
							if (standardCategoriesMap.size() > 0)
							{
								if (standardCategoriesMap.containsKey(gradebookCategory.getTitle()))
								{
									int standardCode = standardCategoriesMap.get(gradebookCategory.getTitle());
									
									if (!modGradebookCategoriesSet.contains(standardCode))
									{
										((GradebookCategoryImpl)gradebookCategory).setStadardCategoryCode(standardCategoriesMap.get(gradebookCategory.getTitle()));
									}
								}
							}
							
							// insert gradebook category
							insertContextCategory(gradebookCategory);
						}
						else if (gradebookCategory.getId() > 0)
						{
							if (existingGradebookCategoriesMap.containsKey(gradebookCategory.getId()))
							{
								existingGradebookCategoryIds.remove(gradebookCategory.getId());
								
								// update category
								if (gradebook.getCategoryType() == CategoryType.Standard)
								{
									editGradebookCategory(gradebookCategory);
								}
								else if (gradebook.getCategoryType() == CategoryType.Custom)
								{
									editGradebookCategory(gradebookCategory);
								}
							}
						}						
					}
					
					// delete categories. Standard categories cannot be deleted.
					if (existingGradebookCategoryIds.size() > 0)
					{
						if (gradebook.getCategoryType() == CategoryType.Custom)
						{
							for(Integer deleteExistingCategoryId : existingGradebookCategoryIds)
							{
								GradebookCategory deleteExistingCategory = existingGradebookCategoriesMap.get(deleteExistingCategoryId);
								
								// Extra Credit category cannot be deleted and category with mapped items cannot be deleted 
								if (deleteExistingCategory != null && !deleteExistingCategory.isExtraCredit() && deleteExistingCategory.getItemCount() == 0)
								{
									deleteGradebookCategory(deleteExistingCategoryId.intValue());
								}
							}
						}
					}
				}
				catch (Exception e)
				{
					if (logger.isErrorEnabled())
					{
						logger.error(e.toString(), e);
					}
					
					throw new RuntimeException("Error while addModifyDelete gradebook categories.", e);
				}
			}
		}, "addModifyDeleteGradebookCategoriesTx(Gradebook gradebook, List<GradebookCategory> gradebookCategories, String modifiedByUserId): " + gradebook.getId());
		
	}*/
	
	protected void addModifyDeleteGradebookCategoriesTx(final Gradebook gradebook, final Gradebook.CategoryType categoryType, final List<GradebookCategory> gradebookCategories, final String modifiedByUserId)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				try
				{
					List<GradebookCategory> existingGradebookCategories = selectGradebookCategoriesByType(gradebook.getId(), categoryType.getCode());
					Map<Integer, GradebookCategory> existingGradebookCategoriesMap = new HashMap<Integer, GradebookCategory>();
					Set<Integer> existingGradebookCategoryIds = new HashSet<Integer>();
					
					for (GradebookCategory exisGradebookCategory : existingGradebookCategories)
					{
						existingGradebookCategoriesMap.put(exisGradebookCategory.getId(), exisGradebookCategory);
						existingGradebookCategoryIds.add(exisGradebookCategory.getId());
					}
					
					Map<String, Integer> standardCategoriesMap = new HashMap<String, Integer>();
					Set<Integer> modGradebookCategoriesSet = new HashSet<Integer>();
					if (categoryType == CategoryType.Custom)
					{
						// To make newly added category as standard category if added with same as standard category title and there is no similar standard category in the gradebook categories*/
						// standard categories
						for (Gradebook.StandardCategory standardCategory : Gradebook.StandardCategory.values())
						{
							standardCategoriesMap.put(standardCategory.getTitle(), standardCategory.getCode());
						}
						
						// existing standard categories in the gradebook
						for (GradebookCategory gradebookCategory : gradebookCategories)
						{
							if (gradebookCategory.getId() > 0 && gradebookCategory.getStandardCategoryCode() > 0)
							{
								modGradebookCategoriesSet.add(gradebookCategory.getStandardCategoryCode());
							}
						}
						
					}
					
					for (GradebookCategory gradebookCategory : gradebookCategories)
					{
						// blank titles not allowed
						if (gradebookCategory.getTitle() == null || gradebookCategory.getTitle().trim().length() == 0)
						{
							continue;
						}
						
						// Standard categories cannot be added or deleted but can be modified. Custom categories can be added or deleted, can be modified
						if ((gradebookCategory.getId() == -1 || gradebookCategory.getId() == 0) && (categoryType == CategoryType.Custom || gradebookCategory.getCategoryType()  == CategoryType.Custom))
						{
							((GradebookCategoryImpl)gradebookCategory).setGradebookId(gradebook.getId());
							((GradebookCategoryImpl)gradebookCategory).setCreatedByUserId(modifiedByUserId);
							((GradebookCategoryImpl)gradebookCategory).setCategoryType(CategoryType.Custom);
							
							/* sets standard category code - check for title and if standard category with that title is not existing in the gradebook categories check for the other titles with same standard code then set 
								standard category code if gradebook has no such categories with this standard code*/
							if (standardCategoriesMap.size() > 0)
							{
								if (standardCategoriesMap.containsKey(gradebookCategory.getTitle()))
								{
									int standardCode = standardCategoriesMap.get(gradebookCategory.getTitle());
									
									if (!modGradebookCategoriesSet.contains(standardCode))
									{
										((GradebookCategoryImpl)gradebookCategory).setStadardCategoryCode(standardCategoriesMap.get(gradebookCategory.getTitle()));
									}
								}
							}
							
							// insert gradebook category
							insertContextCategory(gradebookCategory);
						}
						else if (gradebookCategory.getId() > 0)
						{
							if (existingGradebookCategoriesMap.containsKey(gradebookCategory.getId()))
							{
								existingGradebookCategoryIds.remove(gradebookCategory.getId());
								
								// update category
								if (categoryType == CategoryType.Standard)
								{
									if (gradebookCategory.getCategoryType() != null && gradebookCategory.getCategoryType() == CategoryType.Standard)
									{
										editGradebookCategory(gradebookCategory);
									}
								}
								else if (categoryType == CategoryType.Custom)
								{
									if (gradebookCategory.getCategoryType() != null && gradebookCategory.getCategoryType() == CategoryType.Custom)
									{
										editGradebookCategory(gradebookCategory);
									}
								}
							}
						}						
					}
					
					// delete categories. Standard categories cannot be deleted.
					if (existingGradebookCategoryIds.size() > 0)
					{
						if (categoryType == CategoryType.Custom)
						{
							for(Integer deleteExistingCategoryId : existingGradebookCategoryIds)
							{
								GradebookCategory deleteExistingCategory = existingGradebookCategoriesMap.get(deleteExistingCategoryId);
								
								// Extra Credit category cannot be deleted and category with mapped items cannot be deleted 
								if (deleteExistingCategory != null && !deleteExistingCategory.isExtraCredit() && deleteExistingCategory.getItemCount() == 0)
								{
									deleteGradebookCategory(deleteExistingCategoryId.intValue());
								}
							}
						}
					}
				}
				catch (Exception e)
				{
					if (logger.isErrorEnabled())
					{
						logger.error(e.toString(), e);
					}
					
					throw new RuntimeException("Error while addModifyDelete gradebook categories.", e);
				}
			}
		}, "addModifyDeleteGradebookCategoriesTx(Gradebook gradebook, Gradebook.CategoryType categoryType, List<GradebookCategory> gradebookCategories, String modifiedByUserId): " + gradebook.getId());
		
	}
	
	/**
	 * Inserts new mappings, updates existing mappings if changed, deletes if mapping is not in the updated mapping list
	 * 
	 * @param gradebookId				Gradebook id
	 * 
	 * @param categoryType				Category type
	 * 
	 * @param gradebookCategoryMapItems	Updated mapping list
	 */
	// protected void addUpdateDeleteGradebookCategoriesItemsMapTx(final Gradebook gradebook, final List<GradebookCategoryItemMap> gradebookCategoryMapItems)
	protected void addUpdateDeleteGradebookCategoriesItemsMapTx(final int gradebookId, final Gradebook.CategoryType categoryType, final List<GradebookCategoryItemMap> gradebookCategoryMapItems)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				try
				{
					// existing map list in the database
					List<GradebookCategoryItemMap> exisGradebookCategoryItemMaps = selectGradebookCategoryMappedItems(gradebookId, categoryType);
					
					Map<String, GradebookCategoryItemMap> exisGradebookCategoryItemMapsMap = new HashMap<String, GradebookCategoryItemMap>();
					
					// convert existing map list in the database to map
					for (GradebookCategoryItemMap exisGradebookCategoryItemMap : exisGradebookCategoryItemMaps)
					{
						exisGradebookCategoryItemMapsMap.put(exisGradebookCategoryItemMap.getItemId(), exisGradebookCategoryItemMap);
					}
					
					// convert updated map list to map
					Map<String, GradebookCategoryItemMap> modGradebookCategoryItemMapsMap = new HashMap<String, GradebookCategoryItemMap>();
					
					// convert existing map list in the database to map
					for (GradebookCategoryItemMap modGradebookCategoryItemMap : gradebookCategoryMapItems)
					{
						modGradebookCategoryItemMapsMap.put(modGradebookCategoryItemMap.getItemId(), modGradebookCategoryItemMap);
					}
					
					GradebookCategoryItemMap exisGradebookCategoryItemMap = null;
					GradebookCategoryItemMap modGradebookCategoryItemMap = null;
					for (Map.Entry<String, GradebookCategoryItemMap> entry : modGradebookCategoryItemMapsMap.entrySet()) 
					{
						if (logger.isDebugEnabled())
						{
							logger.debug("modGradebookCategoryItemMapsMap - Key : " + entry.getKey() + " Value : " + entry.getValue());
						}
						
						modGradebookCategoryItemMap = entry.getValue();
						
						if (modGradebookCategoryItemMap != null)
						{
							if (exisGradebookCategoryItemMapsMap.containsKey(entry.getKey()))
							{
								// update if changed
								exisGradebookCategoryItemMap = exisGradebookCategoryItemMapsMap.get(entry.getKey());
								if (exisGradebookCategoryItemMap != null)
								{
									if ((modGradebookCategoryItemMap.getCategoryId() != exisGradebookCategoryItemMap.getCategoryId()) 
											|| (modGradebookCategoryItemMap.getDisplayOrder() != exisGradebookCategoryItemMap.getDisplayOrder()))
									{
										updateGradebookCategoryMappedItem(exisGradebookCategoryItemMap.getId(), modGradebookCategoryItemMap.getCategoryId(), modGradebookCategoryItemMap.getDisplayOrder());
									}
								}
								exisGradebookCategoryItemMapsMap.remove(entry.getKey());
							}
							else
							{
								// insert new item
								insertGradebookCategoryItemMap(modGradebookCategoryItemMap);
							}
						}
					}
					
					// delete if not in the updated mapping list
					if (exisGradebookCategoryItemMapsMap.size() > 0)
					{
						for (Map.Entry<String, GradebookCategoryItemMap> entry : exisGradebookCategoryItemMapsMap.entrySet()) 
						{
							if (logger.isDebugEnabled())
							{
								logger.debug("exisGradebookCategoryItemMapsMap - Key : " + entry.getKey() + " Value : " + entry.getValue());
							}
							
							exisGradebookCategoryItemMap = entry.getValue();
							
							if (exisGradebookCategoryItemMap != null)
							{
								deleteGradebookCategoryMappedItem(exisGradebookCategoryItemMap.getId());
							}
						}
					}
					
					/*
					 * get the mappings in the database, update the changed items(category id, item display order) and delete the item that is not in the latest listing
					 */
					
					/*
					 					 
					// delete existing mapped gradebook context category items of current gradebook category type
					deleteGradebookCategoryMappedItems(gradebook);
					
					// check the gradebook category id's with gradebookCategoryMapItems category id's
					List<GradebookCategory> gradebookCategories = gradebook.getGradebookCategories();
					Set<Integer> gradebookCategoryIds = new HashSet<Integer>();
					
					if (gradebookCategories.size() == 0)
					{
						return;
					}
					
					for (GradebookCategory gradebookCategory : gradebookCategories)
					{
						gradebookCategoryIds.add(gradebookCategory.getId());
					}
									
					// insert category item map
					for (GradebookCategoryItemMap gradebookCategoryItemMap : gradebookCategoryMapItems)
					{
						// if category id maps with one of gradebook category id's else item is not mapped
						if (gradebookCategoryIds.contains(gradebookCategoryItemMap.getCategoryId()))
						{
							if (gradebookCategoryItemMap.getItemId() != null || gradebookCategoryItemMap.getItemId().trim().length() > 0)
							{
								insertGradebookCategoryItemMap(gradebookCategoryItemMap);
							}
						}
					}
					
					*/
					
				}
				catch (Exception e)
				{
					if (logger.isErrorEnabled())
					{
						logger.error(e.toString(), e);
					}
					
					throw new RuntimeException("Error while inserting category item map.", e);
				}
			}
		}, "addGradebookCategoriesItemsMapTx: gradebookId : " + gradebookId +" , categoryType : "+ categoryType);
		
	}
	
	/**
	 * Deletes gradebook category
	 * 
	 * @param categoryId	Category id
	 */
	protected void deleteGradebookCategory(int categoryId)
	{
		// delete mapped items with order equal to zero
		String sql = "DELETE FROM gradebook_context_category_item_map WHERE CATEGORY_ID = ?";
		
		Object[] fields = new Object[1];
		int i = 0;
		fields[i++] = categoryId;
		
		try
		{
			this.sqlService.dbWrite(sql, fields);
		}
		catch (Exception e)
		{
			if (logger.isErrorEnabled())
			{
				logger.error(e, e);
			}			
			
			throw new RuntimeException("deleteGradebookCategory: delete category mapped data with item order equals zero. dbWrite failed");			
		}
		
		sql = "DELETE FROM gradebook_context_categories WHERE ID = ?";
				
		try
		{
			this.sqlService.dbWrite(sql, fields);
		}
		catch (Exception e)
		{
			if (logger.isErrorEnabled())
			{
				logger.error(e, e);
			}
			
			throw new RuntimeException("deleteGradebookCategory: dbWrite failed");
		}
	}
	
	/**
	 * Deletes the item mapping
	 * 
	 * @param id	Database id of the item mapping that is to be deleted
	 */
	protected void deleteGradebookCategoryMappedItem(int id)
	{

		String sql = "DELETE FROM gradebook_context_category_item_map WHERE ID = ?";
				
		Object[] fields = new Object[1];
		int i = 0;
		fields[i++] = id;
		
		try
		{
			this.sqlService.dbWrite(sql.toString(), fields);
		}
		catch (Exception e)
		{
			if (logger.isErrorEnabled())
			{
				logger.error(e, e);
			}
			
			throw new RuntimeException("deleteGradebookCategoryMappedItem: dbWrite failed");
		}
	}
	
	/**
	 * Modifies the gradebook and it's grading scale
	 * 
	 * @param gradebook		Grade book
	 */
	protected void editContextGradebook(Gradebook gradebook)
	{
		String sql = "UPDATE gradebook_gradebook SET SELECTED_GRADING_SCALE_ID = ?, SHOW_LETTER_GRADE = ?, RELEASE_GRADES_TYPE = ?, VERSION = VERSION + 1 , MODIFIED_BY_USER = ?, MODIFIED_DATE = ? WHERE ID = ?";
		
		Date now = new Date();
		
		Object[] fields = new Object[6];
		int i = 0;
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
		fields[i++] = gradebook.getModifiedByUserId();
		fields[i++] = new Timestamp(now.getTime());
		fields[i++] = gradebook.getId();
		
		// edit grading scale
		editGradingScale(gradebook);
						
		try
		{
			this.sqlService.dbWrite(sql, fields);
		}
		catch (Exception e)
		{
			if (logger.isErrorEnabled())
			{
				logger.error(e, e);
			}
			
			throw new RuntimeException("editGradebook: dbWrite failed");
		}
		
	}
	
	/** 
	 * Modifies the gradebook attributes not the grading scale
	 * 
	 * @param gradebook		Gradebook
	 */
	protected void editContextGradebookAttibutes(Gradebook gradebook)
	{
		StringBuilder sql = new StringBuilder();
	
		sql.append("UPDATE gradebook_gradebook SET SHOW_LETTER_GRADE = ?, RELEASE_GRADES_TYPE = ?, DROP_LOWEST_SCORE = ?,  BOOST_USER_GRADES_TYPE = ?, BOOST_USER_GRADES_BY = ?, VERSION = VERSION + 1 , MODIFIED_BY_USER = ?, MODIFIED_DATE = ? WHERE ID = ?");
		Date now = new Date();
		
		Object[] fields = new Object[8];
		int i = 0;
		
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
		
		// drop lowest score
		fields[i++] = gradebook.isDropLowestScore() ? 1 : 0;
		
		// boost user grades
		if (gradebook.getBoostUserGradesType() == null)
		{
			fields[i++] = null;
			fields[i++] = null;
		}
		else
		{
			if (gradebook.getBoostUserGradesType() == Gradebook.BoostUserGradesType.points && gradebook.getBoostUserGradesBy() != null && gradebook.getBoostUserGradesBy() > 0)
			{
				fields[i++] = Gradebook.BoostUserGradesType.points.getCode();
				fields[i++] = gradebook.getBoostUserGradesBy();
			}
			else if (gradebook.getBoostUserGradesType() == Gradebook.BoostUserGradesType.percent && gradebook.getBoostUserGradesBy() != null && gradebook.getBoostUserGradesBy() > 0)
			{
				fields[i++] = Gradebook.BoostUserGradesType.percent.getCode();
				fields[i++] = gradebook.getBoostUserGradesBy();
			}
			else
			{
				fields[i++] = null;
				fields[i++] = null;				
			}
		}
		
		fields[i++] = gradebook.getModifiedByUserId();
		fields[i++] = new Timestamp(now.getTime());
		fields[i++] = gradebook.getId();
		
		try
		{
			this.sqlService.dbWrite(sql.toString(), fields);
		}
		catch (Exception e)
		{
			if (logger.isErrorEnabled())
			{
				logger.error(e, e);
			}
			
			throw new RuntimeException("editContextGradebookAttibutes: dbWrite failed");
		}
	}
	
	/**
	 * Updates title, weight, weight distribution, order of the category. For standard categories title cannot be changed
	 * 
	 * @param gradebookCategory		Gradebook category
	 */
	protected void editGradebookCategory(GradebookCategory gradebookCategory)
	{
		if (gradebookCategory == null || gradebookCategory.getTitle() == null  || gradebookCategory.getTitle().trim().length() == 0)
		{
			return;
		}
		Date now = new Date();
	
		String sql = "UPDATE gradebook_context_categories SET TITLE = ?, WEIGHT = ?, WEIGHT_DISTRIBUTION = ?, DROP_NUMBER_OF_LOWEST_SCORES = ?, CATEGORY_ORDER = ?, MODIFIED_BY_USER = ?, MODIFIED_DATE = ? WHERE GRADEBOOK_ID = ? AND ID = ?";
		Object[] fields;
		int i;
		
		if (gradebookCategory.isExtraCredit())
		{
			sql = "UPDATE gradebook_context_categories SET WEIGHT = ?, WEIGHT_DISTRIBUTION = ?, DROP_NUMBER_OF_LOWEST_SCORES = ?, CATEGORY_ORDER = ?, MODIFIED_BY_USER = ?, MODIFIED_DATE = ? WHERE GRADEBOOK_ID = ? AND ID = ?";
			fields = new Object[8];
		}
		else
		{
			sql = "UPDATE gradebook_context_categories SET TITLE = ?, WEIGHT = ?, WEIGHT_DISTRIBUTION = ?, DROP_NUMBER_OF_LOWEST_SCORES = ?, CATEGORY_ORDER = ?, MODIFIED_BY_USER = ?, MODIFIED_DATE = ? WHERE GRADEBOOK_ID = ? AND ID = ?";
			fields = new Object[9];
		}
		
		i = 0;
		
		if (!gradebookCategory.isExtraCredit())
		{
			fields[i++] = gradebookCategory.getTitle().trim();
		}
		
		if (gradebookCategory.getWeight() != null && gradebookCategory.getWeight() > 0)
		{
			fields[i++] = gradebookCategory.getWeight();
		}
		else
		{
			fields[i++] = null;
		}
		
		if (gradebookCategory.getWeight() != null && gradebookCategory.getWeight() > 0)
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
		if (gradebookCategory.getDropNumberLowestScores() > 0)
		{
			fields[i++] = gradebookCategory.getDropNumberLowestScores();
		}
		else
		{
			fields[i++] = 0;
		}
		fields[i++] = gradebookCategory.getOrder();
		fields[i++] = gradebookCategory.getModifiedByUserId();;
		fields[i++] = new Timestamp(now.getTime());
		fields[i++] = gradebookCategory.getGradebookId();
		fields[i++] = gradebookCategory.getId();
		
		try
		{
			this.sqlService.dbWrite(sql, fields);
		}
		catch (Exception e)
		{
			if (logger.isErrorEnabled())
			{
				logger.error(e, e);
			}
			
			throw new RuntimeException("editGradebookCategory(GradebookCategory gradebookCategory): dbWrite failed");
		}
	}
	
	/**
	 * Deletes the mapped items of gradebook of current category type
	 * 
	 * @param gradebook
	 */
	/*
	protected void deleteGradebookCategoryMappedItems(Gradebook gradebook)
	{
		StringBuilder sql = new StringBuilder();
		
		sql.append("DELETE gbcm ");
		sql.append("FROM gradebook_context_category_item_map gbcm ");
		sql.append("INNER JOIN gradebook_context_categories  gcc ON gbcm.CATEGORY_ID = gcc.ID ");
		sql.append("WHERE gcc.GRADEBOOK_ID = ? AND gcc.CATEGORY_TYPE = ? ");
				
		Object[] fields = new Object[2];
		int i = 0;
		fields[i++] = gradebook.getId();
		fields[i++] = gradebook.getCategoryType().getCode();
		
		try
		{
			this.sqlService.dbWrite(sql.toString(), fields);
		}
		catch (Exception e)
		{
			if (logger.isErrorEnabled())
			{
				logger.error(e, e);
			}
			
			throw new RuntimeException("deleteGradebookCategoryMappedItems: dbWrite failed");
		}
	}
	*/
	
	/**
	 * Modifies the grade book category drop lowest scores number
	 * 
	 * @param gradebook	Gradebook
	 * 
	 * @param modifiedByUserId	Modified by user id
	 */
	protected void editGradebookCategoryDropLowerstScoresNumber(Gradebook gradebook, String modifiedByUserId)
	{
		Gradebook exisGradebook = selectContextGradebook(gradebook.getId());
		
		if (exisGradebook == null)
		{
			return;
		}
		
		Map<Integer, GradebookCategory> exisGradebookCategoryMap = new HashMap<Integer, GradebookCategory>();
		for(GradebookCategory exisGradebookCategory : exisGradebook.getGradebookCategories())
		{
			exisGradebookCategoryMap.put(exisGradebookCategory.getId(), exisGradebookCategory);
		}
		
		Date now = new Date();
		
		String sql = "UPDATE gradebook_context_categories SET DROP_NUMBER_OF_LOWEST_SCORES = ?, MODIFIED_BY_USER = ?, MODIFIED_DATE = ? WHERE GRADEBOOK_ID = ? AND ID = ?";
		Object[] fields;
		int i;
		
		for (GradebookCategory gradebookCategory : gradebook.getGradebookCategories())
		{
			// update only changed
			if (!exisGradebookCategoryMap.containsKey(gradebookCategory.getId()))
			{
				continue;
			}
			
			GradebookCategory exisGradebookCategory = exisGradebookCategoryMap.get(gradebookCategory.getId());
			if (exisGradebookCategory.getDropNumberLowestScores() == gradebookCategory.getDropNumberLowestScores())
			{
				continue;
			}
			
			fields = new Object[5];
			i = 0;
			
			if (gradebookCategory.getDropNumberLowestScores() > 0)
			{
				fields[i++] = gradebookCategory.getDropNumberLowestScores();
			}
			else
			{
				fields[i++] = 0;
			}
			fields[i++] = modifiedByUserId;
			fields[i++] = new Timestamp(now.getTime());
			fields[i++] = gradebook.getId();
			fields[i++] = gradebookCategory.getId();
			
			try
			{
				this.sqlService.dbWrite(sql, fields);
			}
			catch (Exception e)
			{
				if (logger.isErrorEnabled())
				{
					logger.error(e, e);
				}
				
				throw new RuntimeException("editGradebookCategoryDropLowerstScoresNumber: dbWrite failed");
			}
		}
	}
	
	/**
	 * Updates catgory type. Removes existing categories and adds default categories
	 * 
	 * @param gradebook			Gradebook
	 * 
	 * @param categoryType		Category type
	 * 
	 * @param modifiedByUserId	Modified by user id
	 */
	protected void editGradebookCategoryType(Gradebook gradebook, Gradebook.CategoryType categoryType, String modifiedByUserId)
	{
		if (categoryType == null)
		{
			return;
		}
		String sql = "UPDATE gradebook_gradebook SET CATEGORY_TYPE = ?, VERSION = VERSION + 1, MODIFIED_BY_USER = ?, MODIFIED_DATE = ? WHERE ID = ?";
		
		Date now = new Date();
		
		Object[] fields = new Object[4];
		int i = 0;
		fields[i++] = categoryType.getCode();
		fields[i++] = gradebook.getModifiedByUserId();
		fields[i++] = new Timestamp(now.getTime());
		fields[i++] = gradebook.getId();
		
		try
		{
			this.sqlService.dbWrite(sql, fields);
		}
		catch (Exception e)
		{
			if (logger.isErrorEnabled())
			{
				logger.error(e, e);
			}
			
			throw new RuntimeException("editGradebookCategoryType: dbWrite failed");
		}
		
		// from standard to custom. Add standard default categories if there are no existing custom categories
		if (categoryType == Gradebook.CategoryType.Custom)
		{
			List<GradebookCategory> gradebookCategories = selectGradebookCategoriesByType(gradebook.getId(), Gradebook.CategoryType.Custom.getCode());
			
			if (gradebookCategories != null && gradebookCategories.size() == 0)
			{
				gradebook.getGradebookCategories().clear();
				
				// insert default categories
				for (Gradebook.StandardCategory standardCategory : Gradebook.StandardCategory.values())
				{
					GradebookCategoryImpl gradebookCategory = new GradebookCategoryImpl();
					gradebookCategory.setTitle(standardCategory.getTitle());
					gradebookCategory.setExtraCredit(standardCategory.isExtraCreditCategory());
					gradebookCategory.setOrder(standardCategory.getOrder());
					gradebookCategory.setCategoryType(Gradebook.CategoryType.Custom);
					gradebookCategory.setStadardCategoryCode(standardCategory.getCode());
					gradebookCategory.setCreatedByUserId(modifiedByUserId);
					
					gradebook.getGradebookCategories().add(gradebookCategory);
				}
				insertContextCategories(gradebook);
			}
		}
		else if (categoryType == Gradebook.CategoryType.Standard)
		{
			// do nothing as there will be existing standard categories for the gradebook
		}
	}
	
	/**
	 * Modifies the custom category weights, weight distribution and title
	 * 
	 * @param gradebook	Gradebook with modified standard category weights, weight distribution, order and title
	 * 
	 * @param modifiedByUserId	Modified by user id
	 */
	protected void editGradebookCustomCategories(Gradebook gradebook, String modifiedByUserId)
	{
		// update only changed
		if (gradebook.getCategoryType() != Gradebook.CategoryType.Custom)
		{
			return;
		}
		
		Gradebook exisGradebook = selectContextGradebook(gradebook.getId());
		if (exisGradebook == null)
		{
			return;
		}
		
		Map<Integer, GradebookCategory> exisGradebookCategoryMap = new HashMap<Integer, GradebookCategory>();
		for(GradebookCategory exisGradebookCategory : exisGradebook.getGradebookCategories())
		{
			exisGradebookCategoryMap.put(exisGradebookCategory.getId(), exisGradebookCategory);
		}
		
		Date now = new Date();
		
		for (GradebookCategory gradebookCategory : gradebook.getGradebookCategories())
		{
			GradebookCategory exisGradebookCategory = exisGradebookCategoryMap.get(gradebookCategory.getId());
			if (exisGradebookCategory == null)
			{
				continue;
			}
			
			// blank titles not allowed
			if (gradebookCategory.getTitle() == null || gradebookCategory.getTitle().trim().length() == 0)
			{
				continue;
			}
			
			if (!isGradebookCategoryChanged(gradebookCategory, exisGradebookCategory, true))
			{
				continue;
			}
			
			if (!exisGradebookCategoryMap.containsKey(gradebookCategory.getId()))
			{
				continue;
			}
			((GradebookCategoryImpl)gradebookCategory).setGradebookId(gradebook.getId());
			((GradebookCategoryImpl)gradebookCategory).setModifiedByUserId(modifiedByUserId);
			editGradebookCategory(exisGradebookCategory);
			
			/*String sql = "UPDATE gradebook_context_categories SET TITLE = ?, WEIGHT = ?, WEIGHT_DISTRIBUTION = ?, CATEGORY_ORDER = ?, MODIFIED_BY_USER = ?, MODIFIED_DATE = ? WHERE GRADEBOOK_ID = ? AND ID = ?";
			Object[] fields;
			int i;
			
			if (gradebookCategory.isExtraCredit())
			{
				sql = "UPDATE gradebook_context_categories SET WEIGHT = ?, WEIGHT_DISTRIBUTION = ?, CATEGORY_ORDER = ?, MODIFIED_BY_USER = ?, MODIFIED_DATE = ? WHERE GRADEBOOK_ID = ? AND ID = ?";
				fields = new Object[7];
			}
			else
			{
				sql = "UPDATE gradebook_context_categories SET TITLE = ?, WEIGHT = ?, WEIGHT_DISTRIBUTION = ?, CATEGORY_ORDER = ?, MODIFIED_BY_USER = ?, MODIFIED_DATE = ? WHERE GRADEBOOK_ID = ? AND ID = ?";
				fields = new Object[8];
			}
			
			if (!exisGradebookCategoryMap.containsKey(gradebookCategory.getId()))
			{
				continue;
			}
			
			i = 0;
			
			if (!gradebookCategory.isExtraCredit())
			{
				fields[i++] = gradebookCategory.getTitle();
			}
			
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
			fields[i++] = gradebookCategory.getOrder();
			fields[i++] = modifiedByUserId;
			fields[i++] = new Timestamp(now.getTime());
			fields[i++] = gradebook.getId();
			fields[i++] = gradebookCategory.getId();
			
			try
			{
				this.sqlService.dbWrite(sql, fields);
			}
			catch (Exception e)
			{
				if (logger.isErrorEnabled())
				{
					logger.error(e, e);
				}
				
				throw new RuntimeException("editGradebookCustomCategories: dbWrite failed");
			}*/
		}
	}
	
	/**
	 * Modifies the standard type weights and weight distribution
	 * 
	 * @param gradebook Gradebook with modified standard type weights, weight distribution, and order
	 *
	 * @param modifiedByUserId	Modified by user id
	 */
	protected void editGradebookStandardCategories(Gradebook gradebook, String modifiedByUserId)
	{
		if (gradebook.getCategoryType() != Gradebook.CategoryType.Standard)
		{
			return;
		}
		
		Gradebook exisGradebook = selectContextGradebook(gradebook.getId());
		if (exisGradebook == null)
		{
			return;
		}
		
		Map<Integer, GradebookCategory> exisGradebookCategoryMap = new HashMap<Integer, GradebookCategory>();
		for(GradebookCategory exisGradebookCategory : exisGradebook.getGradebookCategories())
		{
			exisGradebookCategoryMap.put(exisGradebookCategory.getId(), exisGradebookCategory);
		}
		
		for (GradebookCategory gradebookCategory : gradebook.getGradebookCategories())
		{
			GradebookCategory exisGradebookCategory = exisGradebookCategoryMap.get(gradebookCategory.getId());
			if (exisGradebookCategory == null)
			{
				continue;
			}
			
			// update if category is changed
			if (!isGradebookCategoryChanged(gradebookCategory, exisGradebookCategory, false))
			{
				continue;
			}
			
			((GradebookCategoryImpl)gradebookCategory).setGradebookId(gradebook.getId());
			((GradebookCategoryImpl)gradebookCategory).setModifiedByUserId(modifiedByUserId);
			editGradebookCategory(gradebookCategory);
			
			/*Date now = new Date();
			
			String sql = "UPDATE gradebook_context_categories SET WEIGHT = ?, WEIGHT_DISTRIBUTION = ?, CATEGORY_ORDER = ?, MODIFIED_BY_USER = ?, MODIFIED_DATE = ? WHERE GRADEBOOK_ID = ? AND ID = ?";
			Object[] fields;
			int i;
			
			fields = new Object[7];
			i = 0;
			
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
			fields[i++] = gradebookCategory.getOrder();
			fields[i++] = modifiedByUserId;
			fields[i++] = new Timestamp(now.getTime());
			fields[i++] = gradebook.getId();
			fields[i++] = gradebookCategory.getId();
			
			try
			{
				this.sqlService.dbWrite(sql, fields);
			}
			catch (Exception e)
			{
				if (logger.isErrorEnabled())
				{
					logger.error(e, e);
				}
				
				throw new RuntimeException("editGradebookStandardCategory: dbWrite failed");
			}*/
		}
	}
	
	/**
	 * Modifies the percentages of the grading scale
	 * 
	 * @param gradebook	Gradebook with modified grading scale percentages
	 */
	protected void editGradingScale(Gradebook gradebook)
	{
		/* match the letter grades and update existing data */
		
		
		// selected existing context grading scale and update
		GradingScale contextGradebookGradingScale = selectGradingScaleByType(gradebook.getGradingScale().getType());
		
		// fill grading scale percentages
		selectContextGradingScalePercentages(gradebook.getId(), contextGradebookGradingScale);
		
		List<GradingScalePercent> modGradingScalePercentList = gradebook.getGradingScale().getGradingScalePercent();
		
		Map<String, Float> modGradingScalePercentMap = new HashMap<String, Float>();
		for(GradingScalePercent modGradingScalePercent : modGradingScalePercentList)
		{
			modGradingScalePercentMap.put(modGradingScalePercent.getLetterGrade(), modGradingScalePercent.getPercent());
		}
		
		String sql = "UPDATE gradebook_context_grading_scale_grades SET PERCENT = ? WHERE GRADEBOOK_ID = ? AND GRADING_SCALE_ID = ? AND LETTER_GRADE = ?";
		Object[] fields;
		int i;
		
		// modify existing letter grade percents
		for (GradingScalePercent exisGradingScalePercent : contextGradebookGradingScale.getGradingScalePercent())
		{
			Float modGradingPercent = modGradingScalePercentMap.get(exisGradingScalePercent.getLetterGrade());
			
			if (modGradingPercent != null)
			{
				// update the percentage
				fields = new Object[4];
				i = 0;
				
				if (modGradingPercent > 0)
				{
					fields[i++] = modGradingPercent;
				}
				else
				{
					fields[i++] = 0;
				}
				fields[i++] = gradebook.getId();
				fields[i++] = contextGradebookGradingScale.getId();
				fields[i++] = exisGradingScalePercent.getLetterGrade();
				
				try
				{
					this.sqlService.dbWrite(sql, fields);
				}
				catch (Exception e)
				{
					if (logger.isErrorEnabled())
					{
						logger.error(e, e);
					}
					
					throw new RuntimeException("editContextGradebookGradingScalePercents: dbWrite failed");
				}
				
			}
		}
	}
	
	/**
	 * Update grading scale percentages
	 * 
	 * @param gradebookId		Gradebook id
	 * 
	 * @param gradingScale		Grading scale with modified grading scale percentages
	 */
	protected void editGradingScale(int gradebookId, GradingScale gradingScale)
	{
		/* match the letter grades and update existing data */
		Gradebook gradebook = selectGradebook(gradebookId);
		
		// selected existing grading scale that is to be modified  and update
		GradingScale contextGradebookGradingScale = selectGradingScaleById(gradingScale.getId());
		
		// fill grading scale percentages
		selectContextGradingScalePercentages(gradebookId, contextGradebookGradingScale);
		
		if (contextGradebookGradingScale.getGradingScalePercent().size() == 0)
		{
			return;
		}
		
		List<GradingScalePercent> modGradingScalePercentList = gradingScale.getGradingScalePercent();
		
		Map<String, Float> modGradingScalePercentMap = new HashMap<String, Float>();
		for(GradingScalePercent modGradingScalePercent : modGradingScalePercentList)
		{
			modGradingScalePercentMap.put(modGradingScalePercent.getLetterGrade(), modGradingScalePercent.getPercent());
		}
		
		String sql = "UPDATE gradebook_context_grading_scale_grades SET PERCENT = ? WHERE GRADEBOOK_ID = ? AND GRADING_SCALE_ID = ? AND LETTER_GRADE = ?";
		Object[] fields;
		int i;
		
		// modify existing letter grade percents
		for (GradingScalePercent exisGradingScalePercent : contextGradebookGradingScale.getGradingScalePercent())
		{
			Float modGradingPercent = modGradingScalePercentMap.get(exisGradingScalePercent.getLetterGrade());
			
			if (modGradingPercent != null)
			{
				// update the percentage
				fields = new Object[4];
				i = 0;
				
				if (modGradingPercent > 0)
				{
					fields[i++] = modGradingPercent;
				}
				else
				{
					fields[i++] = 0;
				}
				fields[i++] = gradebook.getId();
				fields[i++] = contextGradebookGradingScale.getId();
				fields[i++] = exisGradingScalePercent.getLetterGrade();
				
				try
				{
					this.sqlService.dbWrite(sql, fields);
				}
				catch (Exception e)
				{
					if (logger.isErrorEnabled())
					{
						logger.error(e, e);
					}
					
					throw new RuntimeException("editGradingScale - editing gradingscale percents: dbWrite failed");
				}				
			}
		}
	}
	
	/**
	 *  Adds user grade if there is no entry in the database or modifies it. Deletes if it is removed. 
	 * 
	 * @param gradebookId			Gradebook id
	 * 
	 * @param userLetterGrades		Modified or added letter grades
	 * 
	 * @param assignedByUserId		Assigned by user id
	 */
	protected void editUserGradesTx(final int gradebookId, final Map<String, String> userLetterGrades, final String assignedByUserId)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				try
				{
					/*add history of one last record*/
					
					Map<String, UserGrade> userGrades = selectUserGrades(gradebookId);
					
					String sql = null;
					Date now = new Date();
					
					if (userLetterGrades == null || userLetterGrades.size() == 0)
					{
						if (userGrades != null && userGrades.size() > 0)
						{
							// update existing records to null
							sql = new String("UPDATE gradebook_user_grades SET ASSIGNED_LETTER_GRADE = NULL, ASSIGNED_DATE = ? WHERE GRADEBOOK_ID = ? AND STUDENT_ID = ?");
							
							Object[] fields = null;
							int i = 0;
							
							for (Map.Entry<String, UserGrade> entry : userGrades.entrySet())
							{
								String studentId = entry.getKey();
								UserGrade userGrade = entry.getValue();
								
								if (studentId == null || studentId.trim().length() == 0 || userGrade.getLetterGrade() == null)
								{
									continue;
								}
								
								// store current record as history item before updating to NULL
								insertUpdateUserGradesHistory(userGrade.getGradebookId(), userGrade.getUserId(), userGrade.getLetterGrade(), userGrade.getAssignedByUserId(), userGrade.getAssignedDate());
								
								fields = new Object[3];
								i = 0;
								fields[i++] = new Timestamp(now.getTime());
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
									
									throw new RuntimeException("update user letter grade to NULL: dbWrite failed");
								}
							}
						}
						return;
					}
					 
					// update if record exists or create a new record
					sql = new String("UPDATE gradebook_user_grades SET ASSIGNED_LETTER_GRADE = ?, ASSIGNED_BY_USER = ?, ASSIGNED_DATE = ? WHERE GRADEBOOK_ID = ? AND STUDENT_ID = ?");					
							
					Object[] fields = null;
					
					Set<String> addModifedUsers = new HashSet<String>();
					
					for (Map.Entry<String, String> entry : userLetterGrades.entrySet())
					{
						String studentId = entry.getKey();
						String letterGrade = entry.getValue();
						
						if ((studentId == null || studentId.trim().length() == 0) || (letterGrade == null || letterGrade.trim().length() == 0))
						{
							continue;
						}
						
						addModifedUsers.add(studentId);
						
						UserGrade exisUserGrade = userGrades.get(studentId);
						
						if (exisUserGrade != null)
						{
							// update if changed
							if (exisUserGrade.getLetterGrade() != null && exisUserGrade.getLetterGrade().equalsIgnoreCase(letterGrade))
							{
								continue;
							}
							
							// store current record as history item before updating
							insertUpdateUserGradesHistory(exisUserGrade.getGradebookId(), exisUserGrade.getUserId(), exisUserGrade.getLetterGrade(), exisUserGrade.getAssignedByUserId(), exisUserGrade.getAssignedDate());
							
							fields = new Object[5];
							int i = 0;
							fields[i++] = letterGrade;
							fields[i++] = assignedByUserId;
							fields[i++] = new Timestamp(now.getTime());
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
								
								throw new RuntimeException("updateUserGrades: dbWrite failed");
							}
						}
						else
						{
							insertUserGrades(gradebookId, studentId, letterGrade, assignedByUserId);
						}
					}
					
					// sql = new String("DELETE FROM gradebook_user_grades WHERE GRADEBOOK_ID = ? AND STUDENT_ID = ?");
					sql = new String("UPDATE gradebook_user_grades SET ASSIGNED_LETTER_GRADE = NULL, ASSIGNED_DATE = ? WHERE GRADEBOOK_ID = ? AND STUDENT_ID = ?");
					
					// update existing entries if removed the given letter grade
					for (String exisUserId : userGrades.keySet())
					{
						if (!addModifedUsers.contains(exisUserId))
						{
							try
							{
								UserGrade exisUserGrade = userGrades.get(exisUserId);
								
								// update if changed
								if (exisUserGrade.getLetterGrade() == null)
								{
									continue;
								}
								
								// store current record as history item before updating to NULL
								insertUpdateUserGradesHistory(exisUserGrade.getGradebookId(), exisUserGrade.getUserId(), exisUserGrade.getLetterGrade(), exisUserGrade.getAssignedByUserId(), exisUserGrade.getAssignedDate());
								
								fields = new Object[3];
								int i = 0;
								
								fields[i++] = new Timestamp(now.getTime());
								fields[i++] = gradebookId;
								fields[i++] = exisUserId;
								
								sqlService.dbWrite(sql, fields);
							}
							catch (Exception e)
							{
								if (logger.isErrorEnabled())
								{
									logger.error(e, e);
								}
								
								throw new RuntimeException("update each user letter grade to NULL: dbWrite failed");
							}
						}
					}
				}
				catch (Exception e)
				{
					if (logger.isErrorEnabled())
					{
						logger.error(e.toString(), e);
					}
					
					throw new RuntimeException("Error while editing user grades.", e);
				}
			}
		}, "editUserGrades: " + gradebookId);
	}
	
	
	/**
	 * Create Gradebook object from resultset
	 * 
	 * @param rs	Resultset
	 * 
	 * @return	Gradebook object
	 * 
	 * @throws SQLException
	 */
	protected Gradebook fillGradebook(ResultSet rs) throws SQLException
	{
		GradebookImpl gradebook = new GradebookImpl();
		
		// ID, CONTEXT, GRADING_SCALE_ID, SHOW_CURRENT_GRADE, SHOW_COURSE_GRADE, VERSION, CREATED_BY_USER, CREATED_DATE, MODIFIED_BY_USER, MODIFIED_DATE
		gradebook.setId(rs.getInt("ID"));
		gradebook.setGradingScale(selectGradingScaleById(rs.getInt("SELECTED_GRADING_SCALE_ID")));
		gradebook.setContext(rs.getString("CONTEXT"));
		gradebook.setShowLetterGrade(rs.getInt("SHOW_LETTER_GRADE") == 1 ? true : false);
		
		int releaseGradesType = rs.getInt("RELEASE_GRADES_TYPE");
		if (releaseGradesType == ReleaseGrades.Released.getCode())
		{
			gradebook.setReleaseGrades(ReleaseGrades.Released);
		}
		else if (releaseGradesType == ReleaseGrades.All.getCode())
		{
			gradebook.setReleaseGrades(ReleaseGrades.All);
		}
		else
		{
			gradebook.setReleaseGrades(ReleaseGrades.Released);
		}
		
		int selectedType = rs.getInt("CATEGORY_TYPE");
		switch (selectedType)
		{
			case 1:
				gradebook.setCategoryType(Gradebook.CategoryType.Standard);
				break;
			case 2:
				gradebook.setCategoryType(Gradebook.CategoryType.Custom);
				break;
			default:
				gradebook.setCategoryType(Gradebook.CategoryType.Standard);
				break;
		}
		
		gradebook.setDropLowestScore(rs.getInt("DROP_LOWEST_SCORE") == 1 ? true : false);
		
		if (rs.getInt("BOOST_USER_GRADES_TYPE") > 0)
		{
			if (rs.getInt("BOOST_USER_GRADES_TYPE") == Gradebook.BoostUserGradesType.points.getCode())
			{
				if (rs.getFloat("BOOST_USER_GRADES_BY") > 0)
				{
					gradebook.setBoostUserGradesType(Gradebook.BoostUserGradesType.points);
					gradebook.setBoostUserGradesBy(rs.getFloat("BOOST_USER_GRADES_BY"));
				}
			}
			else if (rs.getInt("BOOST_USER_GRADES_TYPE") == Gradebook.BoostUserGradesType.percent.getCode())
			{
				if (rs.getFloat("BOOST_USER_GRADES_BY") > 0)
				{
					gradebook.setBoostUserGradesType(Gradebook.BoostUserGradesType.percent);
					gradebook.setBoostUserGradesBy(rs.getFloat("BOOST_USER_GRADES_BY"));
				}
			}
		}
		
		return gradebook;
	}
	
	/**
	 * Fills the category from the resultset
	 * 
	 * @param result	Resultset
	 * 
	 * @return	GradebookCategory object
	 * 
	 * @throws SQLException SQL error while reading from resultset
	 */
	protected GradebookCategoryImpl fillGradebookCategory(ResultSet result) throws SQLException
	{
		GradebookCategoryImpl gradebookCategory = new GradebookCategoryImpl(this.gradebookService);
		
		gradebookCategory.setId(result.getInt("ID"));
		gradebookCategory.setGradebookId(result.getInt("GRADEBOOK_ID"));
		gradebookCategory.setTitle(result.getString("TITLE"));
		
		Float weight = result.getFloat("WEIGHT"); //Returns 0 even if the value should be null
		if (result.wasNull()) 
		{
			weight = null;
		}
		gradebookCategory.setWeight(weight);
		
		Integer weightDistribution = result.getInt("WEIGHT_DISTRIBUTION"); //Returns 0 even if the value should be null
		if (result.wasNull()) 
		{
			weightDistribution = null;
			gradebookCategory.setWeightDistribution(null);
		}
		else
		{
			if (weightDistribution == 1)
			{
				gradebookCategory.setWeightDistribution(WeightDistribution.Equally);
			}
			else if (weightDistribution == 2)
			{
				gradebookCategory.setWeightDistribution(WeightDistribution.Points);
			}
			else
			{
				gradebookCategory.setWeightDistribution(null);
			}
		}
		gradebookCategory.setDropNumberLowestScores(result.getInt("DROP_NUMBER_OF_LOWEST_SCORES"));
		gradebookCategory.setExtraCredit(result.getInt("IS_EXTRA_CREDIT") == 1 ? true : false);
		gradebookCategory.setOrder(result.getInt("CATEGORY_ORDER"));
		int selectedType = result.getInt("CATEGORY_TYPE");
		if (selectedType == CategoryType.Standard.getCode())
		{
			gradebookCategory.setCategoryType(CategoryType.Standard);
		}
		else if (selectedType == CategoryType.Custom.getCode())
		{
			gradebookCategory.setCategoryType(CategoryType.Custom);
		}
		gradebookCategory.setStadardCategoryCode(result.getInt("STANDARD_CATEGORY_CODE"));
		gradebookCategory.setCreatedByUserId(result.getString("CREATED_BY_USER"));
		
		return gradebookCategory;
	}
	
	/**
	 * Create GradingScale object from resultset
	 * 
	 * @param rs	Resultset
	 * 
	 * @return	GradingScale object
	 * 
	 * @throws SQLException
	 */
	protected GradingScale fillGradingScale(ResultSet rs) throws SQLException
	{
		GradingScaleImpl gradingScale = new GradingScaleImpl();
		
		gradingScale.setId(rs.getInt("ID"));
		gradingScale.setName(rs.getString("NAME"));
		gradingScale.setScaleCode(rs.getString("CODE"));
				
		int type = rs.getInt("TYPE");
		switch (type)
		{
			case 1:
				gradingScale.setType(GradingScaleType.LetterGrade);
				break;
			case 2:
				gradingScale.setType(GradingScaleType.LetterGradePlusMinus);
				break;
			case 3:
				gradingScale.setType(GradingScaleType.PassNotPass);
				break;
			default:
				gradingScale.setType(GradingScaleType.LetterGradePlusMinus);
				break;
		}
		
		gradingScale.setVersion(rs.getInt("VERSION"));
		gradingScale.setLocked(rs.getInt("LOCKED") == 1 ? Boolean.TRUE : Boolean.FALSE);
        
		return gradingScale;
	}
	
	/**
	 * Create GradingScalePercent from resultset
	 * 
	 * @param rs	resultset
	 * 
	 * @return	GradingScalePercent
	 * 
	 * @throws SQLException
	 */
	protected GradingScalePercent fillGradingScalePercent(ResultSet rs) throws SQLException
	{
		GradingScalePercentImpl gradingScalePercent = new GradingScalePercentImpl();
		
		gradingScalePercent.setId(rs.getInt("ID"));
		gradingScalePercent.setScaleId(rs.getInt("GRADING_SCALE_ID"));
		gradingScalePercent.setPercent(rs.getFloat("PERCENT"));
		gradingScalePercent.setLetterGrade(rs.getString("LETTER_GRADE"));
		gradingScalePercent.setSequenceNumber(rs.getInt("SEQUENCE"));
		
		return gradingScalePercent;
	}
	
	/**
	 * Create Notes object from resultset
	 * 
	 * @param result	Result set
	 * 
	 * @return			Notes object
	 * 
	 * @throws SQLException
	 */
	protected Notes fillNotes(ResultSet result) throws SQLException
	{
		NotesImpl notes = new NotesImpl();
		
		notes.setId(result.getInt("ID"));
		notes.setGradebookId(result.getInt("GRADEBOOK_ID"));
		notes.setUserId(result.getString("STUDENT_ID"));
		notes.setNotes(result.getString("NOTES"));
		notes.setAddedByUserId(result.getString("ADDED_BY_USER"));
		notes.setModifiedByUserId(result.getString("MODIFED_BY_USER"));
		
		if (result.getDate("ADDED_DATE") != null)
		{
			Timestamp dateAdded = result.getTimestamp("ADDED_DATE");
			notes.setDateAdded(dateAdded);
		}
		else
		{
			notes.setDateAdded(null);
		}
		
		if (result.getDate("MODIFIED_DATE") != null)
		{
			Timestamp dateModified = result.getTimestamp("MODIFIED_DATE");
			notes.setDateModified(dateModified);
		}
		else
		{
			notes.setDateModified(null);
		}
		return notes;
	}
	
	/**
	 * Inserts context categories
	 * 
	 * @param gradebook	Gradebook with context categories
	 */
	protected abstract void insertContextCategories(Gradebook gradebook);
	
	/**
	 * Insert context category
	 * 
	 * @param gradebookCategory	Gradebook category
	 * 
	 * @return Id of the new category
	 */
	protected abstract int insertContextCategory(GradebookCategory gradebookCategory);
	
	/**
	 * Modifies the standard type weights and weight distribution
	 * 
	 * @param gradebook Gradebook with modified standard type weights and weight distribution
	 */
	
	/**
	 * Creates gradebook with  default grading scale for the context
	 * 
	 * @param context	Context
	 * 
	 * @param userId	User id
	 * 
	 * @return	The gradebook
	 */
	abstract protected Gradebook insertContextGradebookTx(String context, String userId);
	
	/**
	 * Insert context grading scales
	 * 
	 * @param gradebook	Gradebook
	 */
	protected abstract void insertContextGradingScales(Gradebook gradebook);
		
	/**
	 * Insert gradebook
	 * 
	 * @param gradebook	Gradebook
	 * 
	 * @return	Newly created gradebook id
	 */
	protected abstract int insertGradebook(Gradebook gradebook);
	
	/**
	 * Insert gradebook category item map
	 * 
	 * @param gradebookCategoryItemMap	Gradebook category item map
	 * 
	 * @return	Id of new gradebook item category map
	 */
	protected abstract int insertGradebookCategoryItemMap(GradebookCategoryItemMap gradebookCategoryItemMap);
	
	/**
	 * Insert default or updated grading scale
	 * 
	 * @param gradebook	Gradebook
	 */
	protected abstract void insertGradingScale(Gradebook gradebook);
	
	/**
	 * Insert instructor note about user
	 * 
	 * @param gradebookId	Gradebook id
	 * 
	 * @param studentId		Student id
	 * 
	 * @param notes			Notes
	 * 
	 * @param addedByUserId	Added by user id
	 * 
	 * @return	Id of the created note
	 */
	protected abstract int insertInstructorUserNotes(int gradebookId, String studentId, String notes, String addedByUserId);
	
	/**
	 * Insert history of the user grade change
	 * 
	 * @param gradebookId		Gradebook id
	 * 
	 * @param studentId			Student id
	 * 
	 * @param letterGrade		Letter grade
	 * 
	 * @param assignedByUserId	Assigned by userid
	 * 	
	 * @param assignedDate		Assigned date
	 */
	protected abstract void insertUpdateUserGradesHistory(int gradebookId, String studentId, String letterGrade, String assignedByUserId, Date assignedDate);
	
	/**
	 * insert user grades
	 * 
	 * @param gradebookId		Gradebook id
	 * 
	 * @param studentId			Student id
	 * 
	 * @param letterGrade		Lettergrade
	 * 
	 * @param assignedByUserId	Assigned by userid
	 * 
	 * @return	Created user grade id
	 */
	protected abstract int insertUserGrades(int gradebookId, String studentId, String letterGrade, String assignedByUserId);
	
	/**
	 * Check if category is changed or not
	 * 
	 * @param gradebookCategory		Modified gradebook category
	 * 
	 * @param exisGradebookCategory	Existing gradebook category
	 * 
	 * @param checkTitle	true - if title also needs to be compared
	 * 						false - if title is not needed to compare
	 * 
	 * @ true if categories weight or weight distribution or title changed else false
	 */
	protected boolean isGradebookCategoryChanged(GradebookCategory gradebookCategory, GradebookCategory exisGradebookCategory, boolean checkTitle)
	{
		// update only changed
		if ((exisGradebookCategory.getWeight() == null &&  gradebookCategory.getWeight() != null) || (exisGradebookCategory.getWeight() != null ||  gradebookCategory.getWeight() == null))
		{
			return true;
		}
		
		if (exisGradebookCategory.getWeight() != null &&  gradebookCategory.getWeight() != null)
		{
			if (exisGradebookCategory.getWeight().equals(gradebookCategory.getWeight()))
			{
				if ((exisGradebookCategory.getWeightDistribution() == null ||  gradebookCategory.getWeightDistribution() != null) && (exisGradebookCategory.getWeightDistribution() != null ||  gradebookCategory.getWeightDistribution() == null))
				{
					return true;
				}
				else
				{
					if (exisGradebookCategory.getWeightDistribution() != null &&  gradebookCategory.getWeightDistribution() != null)
					{
						if (exisGradebookCategory.getWeightDistribution() != gradebookCategory.getWeightDistribution())
						{
							return true;
						}
					}
				}
			}
			else
			{
				return true;
			}
		}
		
		if (exisGradebookCategory.getOrder() != gradebookCategory.getOrder())
		{
			return true;
		}
		
		if (checkTitle)
		{
			// title should not be blank
			if (!exisGradebookCategory.getTitle().equalsIgnoreCase(gradebookCategory.getTitle()))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Gets the count of mapped items count of the category
	 * 
	 * @param categoryId Category id
	 * 
	 * @return	The count of mapped items count of the category
	 */
	protected int selectCategoryMappedItemsCount(int categoryId)
	{
		if (categoryId <= 0)
		{
			throw new IllegalArgumentException("id  is missing");
		}
		
		StringBuilder sql = new StringBuilder();
		
		sql.append("SELECT COUNT(1) AS CAT_ITEM_MAP_COUNT FROM gradebook_context_category_item_map gccim, gradebook_context_categories gcc ");
		sql.append("WHERE gcc.ID = ? AND gcc.ID = gccim.CATEGORY_ID");
		
		Object[] fields = new Object[1];
		int i = 0;
		fields[i++] = categoryId;
		
		final List<Integer> count = new ArrayList<Integer>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					count.add(result.getInt("CAT_ITEM_MAP_COUNT"));
										
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectCategoryMappedItemsCount: " + e, e);
					}
					return null;
				}
			}
		});
		
		int mappedItemsCount = 0;
		
		if (count.size() == 1)
		{
			mappedItemsCount = count.get(0);
		}
		
		return mappedItemsCount;
	}
	
	/**
	 * Selects context gradebook and it's grading scales, categories
	 * 
	 * @param gradebookId	Gradebook id
	 * 
	 * @return	Context gradebook and it's grading scales, categories or null
	 */
	protected Gradebook selectContextGradebook(int gradebookId)
	{
		String sql = "SELECT ID, CONTEXT, SELECTED_GRADING_SCALE_ID, SHOW_LETTER_GRADE, RELEASE_GRADES_TYPE, CATEGORY_TYPE, DROP_LOWEST_SCORE, BOOST_USER_GRADES_TYPE, BOOST_USER_GRADES_BY, VERSION, CREATED_BY_USER, CREATED_DATE, MODIFIED_BY_USER, MODIFIED_DATE FROM gradebook_gradebook WHERE ID = ?";
		
		Object[] fields = new Object[1];
		int i = 0;
		fields[i++] = gradebookId;
		
		final List<Gradebook> gradebooks =  new ArrayList<Gradebook>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					Gradebook gradebook = fillGradebook(result);
					gradebooks.add(gradebook);
					
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectGradebook(int gradebookId): " + e, e);
					}
					return null;
				}
			}
		});
		
		if (gradebooks.size() == 1)
		{
			Gradebook gradebook = gradebooks.get(0);
			
			// assigned or selected grading scale with grading scale percentages
			((GradebookImpl)gradebook).setGradingScale(selectGradebookGradingScale(gradebook));
			
			// all context grading scales
			gradebook.getContextGradingScales().clear();
			gradebook.getContextGradingScales().addAll(selectContextGradingScales(gradebook.getId()));
			
			// get gradebook categories
			gradebook.getGradebookCategories().clear();
			gradebook.getGradebookCategories().addAll(selectGradebookCategoriesByType(gradebook.getId(), gradebook.getCategoryType().getCode()));
			
			// TODO get category mapped items. Is it good place to add here or fetch when needed
			
			return gradebook;
		}
		
		return null;
	}
	
	/**
	 * Gets the context gradebook
	 * 
	 * @param context	Context
	 * 
	 * @return	Context gradebook or null if no context gradebook
	 */
	protected Gradebook selectContextGradebook(String context)
	{
		String sql = "SELECT ID, CONTEXT, SELECTED_GRADING_SCALE_ID, SHOW_LETTER_GRADE, RELEASE_GRADES_TYPE, CATEGORY_TYPE, DROP_LOWEST_SCORE, BOOST_USER_GRADES_TYPE, BOOST_USER_GRADES_BY, VERSION, CREATED_BY_USER, CREATED_DATE, MODIFIED_BY_USER, MODIFIED_DATE FROM gradebook_gradebook WHERE CONTEXT = ?";
		
		Object[] fields = new Object[1];
		int i = 0;
		fields[i++] = context;
		
		final List<Gradebook> gradebooks =  new ArrayList<Gradebook>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					Gradebook gradebook = fillGradebook(result);
					gradebooks.add(gradebook);
					
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectGradebook: " + e, e);
					}
					return null;
				}
			}
		});
		
		if (gradebooks.size() == 1)
		{
			Gradebook gradebook = gradebooks.get(0);
			
			// assigned or selected grading scale with grading scale percentages
			((GradebookImpl)gradebook).setGradingScale(selectGradebookGradingScale(gradebook));
			
			// all context grading scales
			gradebook.getContextGradingScales().clear();
			gradebook.getContextGradingScales().addAll(selectContextGradingScales(gradebook.getId()));
			
			// get gradebook categories
			gradebook.getGradebookCategories().clear();
			gradebook.getGradebookCategories().addAll(selectGradebookCategoriesByType(gradebook.getId(), gradebook.getCategoryType().getCode()));
			
			// TODO get category mapped items. Is it good place to add here or fetch when needed
			
			return gradebook;
		}
		
		return null;
	}
	
	/**
	 * Select context grading scale percentages
	 * 
	 * @param gradebookId	Gradebook id
	 * 
	 * @param contextGradingScale	Grading scale
	 */
	protected void selectContextGradingScalePercentages(int gradebookId, final GradingScale contextGradingScale)
	{
		String sql = "SELECT ID, GRADEBOOK_ID, GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE FROM gradebook_context_grading_scale_grades WHERE GRADEBOOK_ID = ? AND GRADING_SCALE_ID = ? ORDER BY SEQUENCE ASC";
		Object[] fields = new Object[2];
		int i = 0;
		fields[i++] = gradebookId;
		fields[i++] = contextGradingScale.getId();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					GradingScalePercent GradingScalePercent = fillGradingScalePercent(result);
					contextGradingScale.getGradingScalePercent().add(GradingScalePercent);
					
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectContextGradingScalePercentages: " + e, e);
					}
					return null;
				}
			}
		});
	}
	
	/**
	 * Selects context grading scales with scale percentages
	 *  
	 * @param gradebookId	Gradebook id
	 * 
	 * @return	Context grading scales with scale percentages
	 */
	protected List<GradingScale> selectContextGradingScales(int gradebookId)
	{
		List<GradingScale> contextGradingScales = new ArrayList<GradingScale>();
		
		/*select all grading scale types for the context*/
		
		//GradingScale.GradingScaleType.LetterGrade
		GradingScale letterGradeGradingScale = selectGradingScaleByType(GradingScale.GradingScaleType.LetterGrade);
		// fill grading scale percentages
		selectContextGradingScalePercentages(gradebookId, letterGradeGradingScale);
		contextGradingScales.add(letterGradeGradingScale);
		
		// GradingScale.GradingScaleType.LetterGradePlusMinus
		GradingScale letterGradePlusMinusGradingScale = selectGradingScaleByType(GradingScale.GradingScaleType.LetterGradePlusMinus);
		// fill grading scale percentages
		selectContextGradingScalePercentages(gradebookId, letterGradePlusMinusGradingScale);
		contextGradingScales.add(letterGradePlusMinusGradingScale);
		
		// GradingScale.GradingScaleType.PassNotPass
		GradingScale passNotPassGradingScale = selectGradingScaleByType(GradingScale.GradingScaleType.PassNotPass);
		// fill grading scale percentages
		selectContextGradingScalePercentages(gradebookId, passNotPassGradingScale);
		contextGradingScales.add(passNotPassGradingScale);
		
		return contextGradingScales;
	}
	
	/**
	 * Default grading scale(GradingScaleType.LetterGradePlusMinus) with grades
	 * 
	 * @return	The Default grading scale with grades or null if not existing
	 */
	protected GradingScale selectDefaultGradingScale()
	{
		// grading scale
		String sql = "SELECT ID, NAME, CODE, TYPE, VERSION, LOCKED FROM gradebook_grading_scale WHERE ID = ?";
				
		Object[] fields = new Object[1];
		int i = 0;
		fields[i++] = GradingScaleType.LetterGradePlusMinus.getScaleType();
		
		final List<GradingScale> gradingScales =  new ArrayList<GradingScale>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					GradingScale GradingScale = fillGradingScale(result);
					gradingScales.add(GradingScale);
					
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectDefaultGradingScale: grading scale : " + e, e);
					}
					return null;
				}
			}
		});
		
		if (gradingScales.size() == 1)
		{
			final GradingScale gradingScale = gradingScales.get(0);
			
			// fill grading scale percentages
			sql = "SELECT ID, GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE FROM gradebook_grading_scale_grades WHERE GRADING_SCALE_ID = ? ORDER BY PERCENT DESC, LETTER_GRADE ASC";
			fields = new Object[1];
			i = 0;
			fields[i++] = gradingScale.getId();
			
			this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
			{
				public Object readSqlResultRecord(ResultSet result)
				{
					try
					{
						GradingScalePercent GradingScalePercent = fillGradingScalePercent(result);
						gradingScale.getGradingScalePercent().add(GradingScalePercent);
						
						return null;
					}
					catch (SQLException e)
					{
						if (logger.isWarnEnabled())
						{
							logger.warn("selectDefaultGradingScale: grading scale percentages: " + e, e);
						}
						return null;
					}
				}
			});
			
			return gradingScale;
		}
		
		return null;
	}
	
	/**
	 * gets the default grading scale by type
	 *  
	 * @param gradingScaleType	Grading scale type
	 * 
	 * @return	The default grading scale by type
	 */
	protected GradingScale selectDefaultGradingScaleByType(int gradingScaleType)
	{
		// grading scale
		String sql = "SELECT ID, NAME, CODE, TYPE, VERSION, LOCKED FROM gradebook_grading_scale WHERE TYPE = ?";
				
		Object[] fields = new Object[1];
		int i = 0;
		fields[i++] = gradingScaleType;
		
		final List<GradingScale> gradingScales =  new ArrayList<GradingScale>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					GradingScale GradingScale = fillGradingScale(result);
					gradingScales.add(GradingScale);
					
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectDefaultGradingScale: grading scale : " + e, e);
					}
					return null;
				}
			}
		});
		
		if (gradingScales.size() == 1)
		{
			final GradingScale gradingScale = gradingScales.get(0);
			
			// fill grading scale percentages
			sql = "SELECT ID, GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE FROM gradebook_grading_scale_grades WHERE GRADING_SCALE_ID = ? ORDER BY PERCENT DESC, LETTER_GRADE ASC";
			fields = new Object[1];
			i = 0;
			fields[i++] = gradingScale.getType().getScaleType();
			
			this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
			{
				public Object readSqlResultRecord(ResultSet result)
				{
					try
					{
						GradingScalePercent GradingScalePercent = fillGradingScalePercent(result);
						gradingScale.getGradingScalePercent().add(GradingScalePercent);
						
						return null;
					}
					catch (SQLException e)
					{
						if (logger.isWarnEnabled())
						{
							logger.warn("selectDefaultGradingScale: grading scale percentages: " + e, e);
						}
						return null;
					}
				}
			});
			
			return gradingScale;
		}
		
		return null;
	}
	
	/**
	 * Selects gradebook categories
	 * 
	 * @param gradebookId	Gradebook id
	 * 
	 * @return	The gradebook categories
	 */
	/*
	protected List<GradebookCategory> selectGradebookCategories(int gradebookId)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ID, GRADEBOOK_ID, TITLE, WEIGHT, WEIGHT_DISTRIBUTION, DROP_NUMBER_OF_LOWEST_SCORES, IS_EXTRA_CREDIT, CATEGORY_ORDER, STANDARD_CATEGORY_CODE, CREATED_BY_USER, CREATED_DATE, MODIFIED_BY_USER, MODIFIED_DATE ");
		sql.append("FROM gradebook_context_categories ");
		sql.append("WHERE GRADEBOOK_ID = ? ORDER BY CATEGORY_ORDER ASC ");
		Object[] fields = new Object[1];
		int i = 0;
		fields[i++] = gradebookId;
		
		final List<GradebookCategory> gradebookCategoryList = new ArrayList<GradebookCategory>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					GradebookCategoryImpl gradebookCategory = new GradebookCategoryImpl();
					
					gradebookCategory = fillGradebookCategory(result);
					
					gradebookCategoryList.add(gradebookCategory);
					
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectGradebookCategories: " + e, e);
					}
					return null;
				}
			}
		});
		
		return gradebookCategoryList;
	}
	*/
	
	protected List<GradebookCategory> selectGradebookCategoriesByType(int gradebookId, int categoryType)
	{
		if (gradebookId <= 0 || categoryType <= 0)
		{
			return null;
		}
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ID, GRADEBOOK_ID, TITLE, WEIGHT, WEIGHT_DISTRIBUTION, DROP_NUMBER_OF_LOWEST_SCORES, IS_EXTRA_CREDIT, CATEGORY_ORDER, CATEGORY_TYPE, STANDARD_CATEGORY_CODE, CREATED_BY_USER, CREATED_DATE, MODIFIED_BY_USER, MODIFIED_DATE ");
		sql.append("FROM gradebook_context_categories ");
		sql.append("WHERE GRADEBOOK_ID = ? AND CATEGORY_TYPE = ? ORDER BY CATEGORY_ORDER ASC ");
		Object[] fields = new Object[2];
		int i = 0;
		fields[i++] = gradebookId;
		fields[i++] = categoryType;
		
		final List<GradebookCategory> gradebookCategoryList = new ArrayList<GradebookCategory>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					GradebookCategoryImpl gradebookCategory = new GradebookCategoryImpl();
					
					gradebookCategory = fillGradebookCategory(result);
					
					gradebookCategoryList.add(gradebookCategory);
					
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectGradebookCategoriesByType: " + e, e);
					}
					return null;
				}
			}
		});
		
		return gradebookCategoryList;
	}
	
	/**
	 * Gets the existing gradebook category item map
	 * 
	 * @param gradeBookId	Gradebook id
	 * 
	 * @param categoryType	Category type
	 * 
	 * @param itemId	Item id
	 * 
	 * @return	Existing gradebook category item map or null
	 */
	protected GradebookCategoryItemMap selectGradebookCategoryMapItem(int gradeBookId, CategoryType categoryType, String itemId)
	{
		if (gradeBookId == 0 || categoryType == null || itemId == null || itemId.trim().length() == 0)
		{
			return null;
		}
		
		StringBuilder sql = new StringBuilder();
		
		sql.append("SELECT gccim.ID, gccim.CATEGORY_ID, gccim.ITEM_ID, gccim.ITEM_ORDER ");
		sql.append("FROM gradebook_context_category_item_map gccim, gradebook_context_categories gcc ");
		sql.append("WHERE gcc.ID = gccim.CATEGORY_ID AND gcc.GRADEBOOK_ID = ? AND gcc.CATEGORY_TYPE = ? AND gccim.ITEM_ID = ? ");
		
		Object[] fields = new Object[3];
		int i = 0;
		fields[i++] = gradeBookId;
		fields[i++] = categoryType.getCode();
		fields[i++] = itemId;
		
		final List<GradebookCategoryItemMap> gradebookCategoryItemMapList = new ArrayList<GradebookCategoryItemMap>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					GradebookCategoryItemMapImpl gradebookCatItemMap = new GradebookCategoryItemMapImpl();
					
					gradebookCatItemMap.setId(result.getInt("ID"));
					gradebookCatItemMap.setCategoryId(result.getInt("CATEGORY_ID"));
					gradebookCatItemMap.setItemId(result.getString("ITEM_ID"));
					gradebookCatItemMap.setDisplayOrder(result.getInt("ITEM_ORDER"));					
					
					gradebookCategoryItemMapList.add(gradebookCatItemMap);
										
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectGradebookCategoryMapItem: " + e, e);
					}
					return null;
				}
			}
		});
		
		GradebookCategoryItemMap gradebookCategoryItemMap = null;
		if (gradebookCategoryItemMapList.size() == 1)
		{
			gradebookCategoryItemMap = gradebookCategoryItemMapList.get(0);
		}
		
		return gradebookCategoryItemMap;	
	}
	
	/**
	 * Gets the grading scale with grades of the gradebook
	 * 
	 * @param gradebook	Gradebook
	 * 
	 * @return	The grading scale with grades of the gradebook or null
	 */
	protected GradingScale selectGradebookGradingScale(Gradebook gradebook)
	{
		// grading scale percentages
		String sql = "SELECT ID, GRADEBOOK_ID, GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE FROM gradebook_context_grading_scale_grades WHERE GRADEBOOK_ID = ? AND GRADING_SCALE_ID = ? ORDER BY SEQUENCE ASC";
				
		Object[] fields = new Object[2];
		int i = 0;
		fields[i++] = gradebook.getId();
		fields[i++] = ((GradebookImpl)gradebook).getGradingScale().getId();
		
		final List<GradingScalePercent> gradingScalePercentages =  new ArrayList<GradingScalePercent>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					GradingScalePercent gradingScalePercent = fillGradingScalePercent(result);
					gradingScalePercentages.add(gradingScalePercent);
					
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectGradebookGradingScalePercentages: grading scale : " + e, e);
					}
					return null;
				}
			}
		});
		
		if (gradingScalePercentages.size() > 0)
		{
			GradingScalePercent gradingScalePercent = gradingScalePercentages.get(0);
			
			GradingScale gradingScale = selectGradingScaleById(gradingScalePercent.getScaleId());
			
			gradingScale.getGradingScalePercent().addAll(gradingScalePercentages);
			
			return gradingScale;
		}
		else
		{
			return selectGradingScaleById(((GradebookImpl)gradebook).getGradingScale().getId());
		}
	}
	
	/**
	 * Gets the grading scale
	 * 
	 * @param gradingScaleId	Grading scale id
	 * 
	 * @return	The grading scale or null
	 */
	protected GradingScale selectGradingScaleById(int gradingScaleId)
	{
		// grading scale
		String sql = "SELECT ID, NAME, CODE, TYPE, VERSION, LOCKED FROM gradebook_grading_scale WHERE ID = ?";
				
		Object[] fields = new Object[1];
		int i = 0;
		fields[i++] = gradingScaleId;
		
		final List<GradingScale> gradingScales =  new ArrayList<GradingScale>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					GradingScale GradingScale = fillGradingScale(result);
					gradingScales.add(GradingScale);
					
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectGradingScaleById: " + e, e);
					}
					return null;
				}
			}
		});
		
		if (gradingScales.size() == 1)
		{
			GradingScale gradingScale = gradingScales.get(0);
			
			return gradingScale;
		}
		
		return null;
	}
	
	/**
	 * Gets the grading scale by type
	 * 
	 * @param gradingScaleType	Grading scale type
	 * 
	 * @return	The grading scale or null
	 */
	protected GradingScale selectGradingScaleByType(GradingScale.GradingScaleType gradingScaleType)
	{
		// grading scale
		String sql = "SELECT ID, NAME, CODE, TYPE, VERSION, LOCKED FROM gradebook_grading_scale WHERE TYPE = ?";
				
		Object[] fields = new Object[1];
		int i = 0;
		fields[i++] = gradingScaleType.getScaleType();
		
		final List<GradingScale> gradingScales =  new ArrayList<GradingScale>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					GradingScale GradingScale = fillGradingScale(result);
					gradingScales.add(GradingScale);
					
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectGradingScaleByType: " + e, e);
					}
					return null;
				}
			}
		});
		
		if (gradingScales.size() == 1)
		{
			GradingScale gradingScale = gradingScales.get(0);
			
			return gradingScale;
		}
		
		return null;
	}
	
	/**
	 * Gets the current existing note about user added by user or null if not existing
	 * 
	 * @param gradebookId	Gradebook id
	 * 
	 * @param userId		User id
	 * 
	 * @return	Existing note or null
	 */
	protected Notes selectUserNotes(int gradebookId, String userId)
	{
		String sql = "SELECT ID, GRADEBOOK_ID, STUDENT_ID, NOTES, ADDED_BY_USER, MODIFED_BY_USER, ADDED_DATE, MODIFIED_DATE FROM gradebook_instructor_student_notes WHERE GRADEBOOK_ID = ? AND STUDENT_ID = ?";
		
		Object[] fields = new Object[2];
		int i = 0;
		fields[i++] = gradebookId;
		fields[i++] = userId;
		
		final List<Notes> instructorUserNotes =  new ArrayList<Notes>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					Notes notes = fillNotes(result);
					
					instructorUserNotes.add(notes);
					
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectUserNotes: " + e, e);
					}
					return null;
				}
			}
		});
		
		Notes instructorUserNote = null;
		
		if (instructorUserNotes.size() == 1)
		{
			instructorUserNote = instructorUserNotes.get(0);
		}
		
		return instructorUserNote;
	}
	
	/**
	 * updates gradebook category type
	 *  
	 * @param gradebook		Gradebook
	 * 
	 * @param categoryType	Category type
	 * 
	 * @param modifiedByUserId	Modified by user id
	 */
	protected void updateCategoryTypeTx(final Gradebook gradebook, final Gradebook.CategoryType categoryType, final String modifiedByUserId)
	{
		
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				try
				{
					if (gradebook == null)
					{
						return;
					}
					
					editGradebookCategoryType(gradebook, categoryType, modifiedByUserId);
				}
				catch (Exception e)
				{
					if (logger.isErrorEnabled())
					{
						logger.error(e.toString(), e);
					}
					
					throw new RuntimeException("Error while updating gradeboook type.", e);
				}
			}
		}, "updateCategoryType: " + gradebook.getContext());
	}
	
	/**
	 * Sets items mapped count of the category
	 * 
	 * @param gradebook	Gradebook
	 */
	/*
	protected void setCategoryMappedItemsCount(Gradebook gradebook)
	{
		if (gradebook == null)
		{
			return;
		}
		int categoriesCount = gradebook.getGradebookCategories().size();
		
		if (categoriesCount <= 0)
		{
			return;
		}
		
		StringBuilder params = new StringBuilder();
		
		for (int i = 0; i < categoriesCount; i++)
		{
			params.append(" ?");
			
			if (i != (categoriesCount -1))
			{
				params.append(", ");
			}
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT  CATEGORY_ID, COUNT(1) As ITEMS_COUNT FROM gradebook_context_category_item_map gccm, gradebook_context_categories gcc ");
		sql.append("WHERE CATEGORY_ID IN ("+ params +") AND gcc.GRADEBOOK_ID = ? AND gcc.ID = gccm.CATEGORY_ID AND gccm.ITEM_ORDER > 0 GROUP BY CATEGORY_ID");
		
		int fieldsCount = categoriesCount + 1;
		Object[] fields = new Object[fieldsCount];
		int i = 0;
		
		final Map <Integer, GradebookCategory> gradebookCategoriesMap = new HashMap<Integer, GradebookCategory>();
		
		for (GradebookCategory gradebookCategory : gradebook.getGradebookCategories())
		{
			fields[i++] = gradebookCategory.getId();
			gradebookCategoriesMap.put(gradebookCategory.getId(), gradebookCategory);
		}
		
		fields[i++] = gradebook.getId();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					int count = result.getInt("ITEMS_COUNT");
					if (count > 0)
					{
						int categoryId = result.getInt("CATEGORY_ID");
						if (gradebookCategoriesMap.get(categoryId) != null)
						{
							((GradebookCategoryImpl)gradebookCategoriesMap.get(categoryId)).setItemCount(count);
						}
					}
										
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectCategoryMappedItemsCount: " + e, e);
					}
					return null;
				}
			}
		});
	}
	*/
	
	/**
	 * Updates existing notes if null or empty deletes the note
	 * 
	 * @param gradebookId	Gradebook id
	 * 
	 * @param instructorUserNotes	Modified instructor notes
	 * 
	 * @param exisUserNote	Existing notes
	 */
	protected void updateDeleteInstructorUserNotes(final int gradebookId, final Notes instructorUserNotes, final Notes exisUserNote)
	{
		if (instructorUserNotes == null || exisUserNote == null)
		{
			return;
		}
		
		// if not changed ignore
		if ((instructorUserNotes.getNotes() != null && exisUserNote.getNotes() == null)
				|| (instructorUserNotes.getNotes() == null && exisUserNote.getNotes() != null)
				|| (!instructorUserNotes.getNotes().equalsIgnoreCase(exisUserNote.getNotes())))
		{
			if (instructorUserNotes.getNotes() == null || instructorUserNotes.getNotes().trim().length() == 0)
			{
				// delete the note
				String sql = "DELETE FROM gradebook_instructor_student_notes WHERE ID = ?";
				
				Object[] fields = new Object[1];
				int i = 0;
				fields[i++] = exisUserNote.getId();
				
				try
				{
					this.sqlService.dbWrite(sql, fields);
				}
				catch (Exception e)
				{
					if (logger.isErrorEnabled())
					{
						logger.error(e, e);
					}			
					
					throw new RuntimeException("delete existing note: existing note dbWrite failed");			
				}
				
				return;
			}
			
			// update existing notes
			String sql = new String("UPDATE gradebook_instructor_student_notes SET NOTES = ?, MODIFED_BY_USER = ?, MODIFIED_DATE = ? WHERE GRADEBOOK_ID = ? AND STUDENT_ID = ?");					
					
			Object[] fields = new Object[5];
			
			Date now = new Date();
			
			int i = 0;
			fields[i++] = instructorUserNotes.getNotes();
			fields[i++] = instructorUserNotes.getAddedByUserId();
			fields[i++] = new Timestamp(now.getTime());
			fields[i++] = gradebookId;
			fields[i++] = instructorUserNotes.getUserId();
			
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
				
				throw new RuntimeException("insertUpdateInstructorUserNotes: update existing record: dbWrite failed");
			}
		}
	}
	
	/**
	 * Updates the item mapping, item's category id and item order
	 * 
	 * @param id			Database id of the item mapping
	 * 	
	 * @param categoryId	Category id of the item to be updated
	 * 
	 * @param displayOrder	Display order to the item to be updated
	 */
	protected void updateGradebookCategoryMappedItem(int id, int categoryId, int displayOrder)
	{

		String sql = "UPDATE gradebook_context_category_item_map SET CATEGORY_ID = ?, ITEM_ORDER = ? WHERE ID = ?";
				
		Object[] fields = new Object[3];
		int i = 0;
		fields[i++] = categoryId;
		fields[i++] = displayOrder;
		fields[i++] = id;
		
		try
		{
			this.sqlService.dbWrite(sql.toString(), fields);
		}
		catch (Exception e)
		{
			if (logger.isErrorEnabled())
			{
				logger.error(e, e);
			}
			
			throw new RuntimeException("updateGradebookCategoryMappedItem: dbWrite failed");
		}
	}
	
	/**
	 * Update grading scale percentages
	 * 
	 * @param gradebookId		Gradebook id
	 * 
	 * @param gradingScale		Grading scale with modified percentages
	 */
	protected void updateGradingScaleTx(final int gradebookId, final GradingScale gradingScale)
	{
		// update grading scale
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				try
				{
					editGradingScale(gradebookId, gradingScale);
				}
				catch (Exception e)
				{
					if (logger.isErrorEnabled())
					{
						logger.error(e.toString(), e);
					}
					
					throw new RuntimeException("Error while editing grading scale", e);
				}
			}
		}, "updateGradingScaleTx(int gradebookId, GradingScale gradingScale): " + gradebookId +" : gradingScale id :"+ gradingScale.getId());	
	}
	
	/**
	 * updates the gradebook for it's preferences and grading scale.
	 * 
	 * @param gradebook	Gradebook
	 */
	protected void updateTx(final Gradebook gradebook)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				try
				{
					
					// check for existing gradebook and update gradebook and grading scale
					Gradebook exisGradebook = selectContextGradebook(gradebook.getContext());
					
					if (gradebook == null || exisGradebook == null || gradebook.getContext() == null || exisGradebook.getContext() == null)
					{
						return;
					}
					
					// check for same gradebook
					if (gradebook.getId() != exisGradebook.getId() || !gradebook.getContext().equalsIgnoreCase(exisGradebook.getContext()) || gradebook.getGradingScale() == null || (gradebook.getGradingScale().getGradingScalePercent() == null || gradebook.getGradingScale().getGradingScalePercent().size() == 0))
					{
						return;
					}
					
					editContextGradebook(gradebook);
				}
				catch (Exception e)
				{
					if (logger.isErrorEnabled())
					{
						logger.error(e.toString(), e);
					}
					
					throw new RuntimeException("Error while updating gradeboook.", e);
				}
			}
		}, "editGradebook: " + gradebook.getContext());
	}

}
