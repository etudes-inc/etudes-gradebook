/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-impl/impl/src/java/org/etudes/gradebook/impl/GradebookImportServiceImpl.java $
 * $Id: GradebookImportServiceImpl.java 11657 2015-09-17 22:27:42Z rashmim $
 ***********************************************************************************
 *
 * Copyright (c) 2015 Etudes, Inc.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.gradebook.api.Gradebook;
import org.etudes.gradebook.api.Gradebook.CategoryType;
import org.etudes.gradebook.api.Gradebook.GradebookSortType;
import org.etudes.gradebook.api.Gradebook.ReleaseGrades;
import org.etudes.gradebook.api.GradebookCategory.WeightDistribution;
import org.etudes.gradebook.api.GradebookCategory;
import org.etudes.gradebook.api.GradebookCategoryItemMap;
import org.etudes.gradebook.api.GradebookCategoryItemMapComparator;
import org.etudes.gradebook.api.GradebookImportService;
import org.etudes.gradebook.api.GradebookItem;
import org.etudes.gradebook.api.GradebookService;
import org.etudes.gradebook.api.GradingScale;
import org.etudes.gradebook.api.GradingScalePercent;
import org.etudes.mneme.api.Assessment;
import org.etudes.mneme.api.AssessmentPermissionException;
import org.etudes.mneme.api.AssessmentPolicyException;
import org.etudes.mneme.api.AssessmentService;
import org.etudes.mneme.api.AssessmentType;
import org.etudes.siteimport.api.SiteImporter;
import org.sakaiproject.db.api.SqlReader;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;

public class GradebookImportServiceImpl implements GradebookImportService, SiteImporter
{
	/** logger **/
	private static Log logger = LogFactory.getLog(GradebookImportServiceImpl.class);

	/** Dependency: AssessmentService. */
	protected AssessmentService assessmentService = null;
	
	/** Dependency: GradebookService */
	protected GradebookService gradebookService = null;
	
	/** Dependency: SiteService. */
	protected SiteService siteService = null;
	
	/** Dependency: SqlService. */
	protected SqlService sqlService = null;
	
	/**
	 * Check if a site has E3 Gradebook tool
	 * @param siteId
	 * @return
	 */
	protected boolean checkE3GradebookTool(String siteId)
	{
		try
		{
			Site site = siteService.getSite(siteId);
			if (site.getToolForCommonId("e3.gradebook") != null) return true;			
		}
		catch (Exception e)
		{
			//do nothing
		}
		return false;
	}
	
	/**
	 * Shutdown.
	 */
	public void destroy()
	{			
		if (logger.isInfoEnabled())
		{
			logger.info("destroy()");
		}
	}
	
	/* (non-Javadoc)
	 * @see org.etudes.gradebook.impl.GradebookImportService#findExistingCategory(org.etudes.gradebook.api.Gradebook.CategoryType, int, java.lang.String, java.util.List)
	 */
	public GradebookCategory findExistingCategory(CategoryType categoryType, int standardCategoryCode, String title, List<GradebookCategory> gcsList)
	{
		GradebookCategory found = null;
		
		for (GradebookCategory gc: gcsList)
		{					
			// standard categories can have title change
			if (standardCategoryCode > 0 && gc.getCategoryType().getCode().intValue() == categoryType.getCode().intValue() && gc.getStandardCategoryCode() == standardCategoryCode)
			{
				return gc;
			}
			// added custom categories have to match all
			else if (standardCategoryCode == 0 && gc.getCategoryType().getCode().intValue() == categoryType.getCode().intValue() && gc.getStandardCategoryCode() == standardCategoryCode && gc.getTitle().trim().equals(title))
			{
				return gc;							
			}
		}
		return found;
	}	
	
	/**
	 * Find category based on id for same site
	 * @param id
	 * @param gcsList
	 * @return
	 */
	public GradebookCategory findExistingCategory(int id, List<GradebookCategory> gcsList)
	{
		GradebookCategory found = null;
		
		for (GradebookCategory gc: gcsList)
		{					
			if (gc.getId() == id) return gc;
		}
		return found;
	}	
	
	/**
	 * Iterate over the list to get gradebookItem which matches id
	 * @param find
	 * @param fromGradebookItems
	 * @return
	 */
	public GradebookItem findGradebookItem(String find, List<GradebookItem> fromGradebookItems)
	{		
		for (GradebookItem gitem : fromGradebookItems)
		{
			if (gitem.getId().equals(find)) return gitem;			
		}
		return null;
	}
	
	/**
	 * Iterate over the list to get itemmap which matches itemId- categorytype _ category code
	 * @param find
	 * @param findCategoryId
	 * @param findStandardCode
	 * @param fromGradebookItems
	 * @return
	 */
	protected GradebookCategoryItemMap findGradebookItemMap(String find, Integer findCategoryId, int findStandardCode, List<GradebookCategoryItemMap> fromGradebookItems)
	{		
		for (GradebookCategoryItemMap gitem : fromGradebookItems)
		{
			if (gitem.getItemId().equals(find) && gitem.getCategory().getCategoryType().getCode() == findCategoryId && gitem.getCategory().getStandardCategoryCode() == findStandardCode) return gitem;			
		}
		return null;
	}
	
	/**
	 * 
	 * @param result
	 * @return
	 * @throws SQLException
	 */
	protected GradebookCategoryImpl fillGradebookCategory(ResultSet result) throws SQLException
	{
		GradebookCategoryImpl gradebookCategory = new GradebookCategoryImpl();
		
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
	
	/* (non-Javadoc)
	 * @see org.etudes.gradebook.impl.GradebookImportService#findE3ContextCategories(int)
	 */
	public List<GradebookCategory> findE3ContextCategories(int id)
	{
		
		String sql = "SELECT ID, GRADEBOOK_ID, TITLE, WEIGHT, WEIGHT_DISTRIBUTION, DROP_NUMBER_OF_LOWEST_SCORES, IS_EXTRA_CREDIT, CATEGORY_ORDER, CATEGORY_TYPE, STANDARD_CATEGORY_CODE, CREATED_BY_USER, CREATED_DATE, MODIFIED_BY_USER, MODIFIED_DATE ";
		sql = sql.concat("FROM gradebook_context_categories WHERE GRADEBOOK_ID = ? ");
	
		Object[] fields = new Object[1];
		fields[0] = id;
		
		final  List<GradebookCategory> foundItems = new ArrayList<GradebookCategory>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					GradebookCategoryImpl gradebookCategory = new GradebookCategoryImpl();
					
					gradebookCategory = fillGradebookCategory(result);
					
					foundItems.add(gradebookCategory);
					
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
		return foundItems;		
	}
	
	/**
	 * Find list of unpublish mapping with item order as zero
	 * @param fromContext
	 * @return
	 */
	public List<GradebookCategoryItemMap> findItemMapping(String fromContext)
	{
		String sql = "SELECT gbcm.ID , gbcm.CATEGORY_ID, gbcm.ITEM_ID, gbcm.ITEM_ORDER ";
		sql = sql.concat(" FROM gradebook_context_category_item_map gbcm ");
		sql = sql.concat(" where gbcm.category_id in (select c.id from gradebook_context_categories c, gradebook_gradebook gb");
		sql = sql.concat(" where c.gradebook_id = gb.id and gb.context=? )");
	
		Object[] fields = new Object[1];
		fields[0] = fromContext;
		
		final  List<GradebookCategoryItemMap> foundItems = new ArrayList<GradebookCategoryItemMap>();
		
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
					
					foundItems.add(gradebookCatItemMap);
										
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("findUnpublishedItemMapping: " + e, e);
					}
					return null;
				}
			}
		});
		
		return foundItems;		
	}
	
	/**
	 * Check if from site has UCB's gradebook. If yes, return gradebook id.
	 * @param fromContext
	 * @return
	 */
	protected List<Map<String, Object>> findUCBGradebook(String fromContext)
	{
		// add course_grade_displayed boolean
		String sql = "select id, course_grade_displayed, selected_grade_mapping_id from gb_gradebook_t where gradebook_uid=?";
		Object[] fields = new Object[1];
		fields[0] = fromContext;
		
		final List<Map<String, Object>> foundGbs = new ArrayList<Map<String, Object>>(); 
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					Long id = result.getLong("id");
					if (id != null && id != 0) 
					{
						Map<String, Object> item = new HashMap<String, Object>();
						item.put("id", id);
						item.put("all_assessment", result.getBoolean("course_grade_displayed"));
						item.put("selectedGradeScale", result.getInt("selected_grade_mapping_id"));
						foundGbs.add(item);
					}
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("findUCBGradebook: " + e, e);
					}
					return null;
				}
			}
		});
		return foundGbs;		
	}
	
	/**
	 * Find manual offline tasks added in UCB gradebook.
	 * @param id
	 * @return
	 */
	protected List<Map<String, Object>> findUCBGradebookManualItems(Long id)
	{
		String sql = "select name, points_possible, due_date from gb_gradable_object_t where gradebook_id=? and removed=false and object_type_id=1 and external_Id is null and external_App_Name is null";
		Object[] fields = new Object[1];
		fields[0] = id;
		
		final List<Map<String, Object>> foundItems = new ArrayList<Map<String, Object>>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					Map<String, Object> item = new HashMap<String, Object>();
					item.put("name", result.getString("name"));
					item.put("points", result.getFloat("points_possible"));
					item.put("due", result.getDate("due_date"));
					foundItems.add(item);
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("findUCBGradebookManualItems: " + e, e);
					}
					return null;
				}
			}
		});
		return foundItems;
	}
	
	/**
	 * 
	 * @param context
	 * @param scaleId
	 * @return
	 */
	protected List<Map<String, Object>> findUCBGradingScaleChanges(String context, Integer scaleId)
	{
		// SELECT * FROM gb_grade_to_percent_mapping_t g where g.grade_map_id IN ( SELECT id FROM gb_grade_map_t where gradebook_id = 1 ) order by g.grade_map_id asc, g.letter_grade asc;
		String sql = "SELECT percent, letter_grade FROM gb_grade_to_percent_mapping_t where grade_map_id = ?";
		Object[] fields = new Object[1];
		fields[0] = scaleId;
		
		final List<Map<String, Object>> foundItems = new ArrayList<Map<String, Object>>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					Map<String, Object> item = new HashMap<String, Object>();
					item.put("percent", result.getFloat("percent"));
					item.put("letterGrade", result.getString("letter_grade"));
					foundItems.add(item);
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("findUCBGradingScaleChanges: " + e, e);
					}
					return null;
				}
			}
		});
		return foundItems;
	}
	
	/**
	 * 
	 * @param context
	 * @param id
	 * @return
	 */
	protected Map<Integer, Integer> findUCBGradingScaleIds(String context, Long id)
	{
		String sql = "SELECT id, gb_grading_scale_t FROM gb_grade_map_t where gradebook_id = ? ";
		Object[] fields = new Object[1];
		fields[0] = id;
		
		final Map<Integer, Integer> foundItems = new HashMap<Integer, Integer>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					foundItems.put(result.getInt("gb_grading_scale_t"), result.getInt("id"));				
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("findUCBGradingScaleIds: " + e, e);
					}
					return null;
				}
			}
		});
		return foundItems;
	}

	/**
	 * @inherited
	 */
	public void importFromSite(String userId, String fromSite, String toSite)
	{		
		boolean toE3GradebookExists = checkE3GradebookTool(toSite);		
		if (!toE3GradebookExists) return;
		
		Gradebook gb = gradebookService.getContextGradebook(toSite, userId);
			
		//bad user or toSite doesn't have e3 gradebook then return
		if (gb == null || !toE3GradebookExists) return;
		
		// determine if fromContext has UCB GB
		List<Map<String, Object>> oldGB = findUCBGradebook(fromSite);
		
		// if yes get data from UCB to Etudes GB
		if (oldGB != null && oldGB.size() > 0 )
		{
			//Data from UCB to Etudes
			Map<String, Object> o = oldGB.get(0);
			Long oldGBId = (Long) o.get("id");		
			Boolean allAssessmentsGradeOption = (Boolean) o.get("all_assessment");
			
			ReleaseGrades releaseGrades = ReleaseGrades.Released;
			if (allAssessmentsGradeOption != null && allAssessmentsGradeOption.booleanValue()) releaseGrades = ReleaseGrades.All;
			Boolean showLetterGrade = Boolean.FALSE;
			Boolean dropLowest = Boolean.FALSE;
			
			gradebookService.modifyContextGradebookAttributes(toSite, showLetterGrade, releaseGrades, dropLowest, userId);
			
			//grading scale 
			Integer ucbSelectedGradingScale = (Integer) o.get("selectedGradeScale");
			Map<Integer, Integer> ucbIds = findUCBGradingScaleIds(fromSite, oldGBId);
			Set<Integer> idKeys = ucbIds.keySet();
			for (Integer scaleType : idKeys)
			{	
				Integer ucbScaleId =  ucbIds.get(scaleType);
				List<Map<String, Object>> oldGradeScales = findUCBGradingScaleChanges(fromSite, ucbScaleId);
				if (oldGradeScales.size() > 0) transferUCBGradingScales(gb, oldGradeScales, scaleType, ucbScaleId, ucbSelectedGradingScale, toSite, userId);
			}		
			
			// manual assignments
			List<Map<String, Object>> manualItems = findUCBGradebookManualItems(oldGBId);
			if (manualItems.size() > 0) transferOfflineTasks(manualItems, toSite);		
		}

		//if fromSite has etudes GB
		boolean fromE3GradebookExists = checkE3GradebookTool(fromSite);
		if (gb == null || !fromE3GradebookExists) return;
						
		//Transfer from Etudes GB - Etudes GB
		transferE3Settings(gb, fromSite, toSite, userId);	
	}
	
	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		if (logger.isInfoEnabled())
		{
			logger.info("init()");
		}	
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
	
	/*
	 * Set GradebookService
	 */
	public void setGradebookService(GradebookService service)
	{
		this.gradebookService = service;
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
	 * Sets SqlService
	 * 
	 * @param service SqlService
	 */
	public void setSqlService(SqlService service)
	{
		this.sqlService = service;
	}
	
	/**
	 * 
	 * @param fromGb
	 * @param gb
	 * @param toContext
	 * @param modifiedByUserId
	 */
	protected void transferE3Categories(Gradebook fromGb, Gradebook gb,	String toContext, String modifiedByUserId)
	{
		gradebookService.modifyContextGradebookCategoryType(toContext, fromGb.getCategoryType(), modifiedByUserId);
		//refesh gradebook object
		gb = gradebookService.getContextGradebook(toContext, modifiedByUserId);
		
		List<GradebookCategory> gradebookCategories = findE3ContextCategories(fromGb.getId());
		List<GradebookCategory> toGradebookCategories = findE3ContextCategories(gb.getId());
		List<GradebookCategory> transferCategories = new ArrayList<GradebookCategory>();
		
		for (GradebookCategory gc : gradebookCategories)
		{
			addCategoryforTransfer(toGradebookCategories,transferCategories, gc.getCategoryType(), gc.getStandardCategoryCode(), gc.getTitle(),
					gc.getWeight(), gc.getWeightDistribution(), gc.getDropNumberLowestScores(), gc.getOrder());		
		}
		
		//retain toSite additional categories
		toGradebookCategories.removeAll(transferCategories);
		if (toGradebookCategories != null && toGradebookCategories.size() > 0)
		{
			transferCategories.addAll(toGradebookCategories);
		}
		
		//separate them to 2 arrays as api don't support sending as one
		List<GradebookCategory> standardCategories = new ArrayList<GradebookCategory>();
		List<GradebookCategory> customCategories = new ArrayList<GradebookCategory>();
		
		for (GradebookCategory gc : transferCategories)
		{
			if (gc.getCategoryType() == Gradebook.CategoryType.Standard) standardCategories.add(gc);
			else if (gc.getCategoryType() == Gradebook.CategoryType.Custom) customCategories.add(gc);
		}
		
		if (standardCategories.size() > 0)
			gradebookService.addModifyDeleteContextGradebookCategories(toContext, Gradebook.CategoryType.Standard, standardCategories, modifiedByUserId);
		
		if (customCategories.size() > 0)
			gradebookService.addModifyDeleteContextGradebookCategories(toContext, Gradebook.CategoryType.Custom, customCategories, modifiedByUserId);
	}
	
	/**
	 * Find matching gradable item w.r.t item title and tool title.
	 * @param title
	 * @param typeDisplayString
	 * @param siteGradableItems
	 * @return null if not found, item if found
	 */
	protected GradebookItem findMatchingItem(String title, String typeDisplayString, List<GradebookItem> siteGradableItems)
	{
		for (GradebookItem item : siteGradableItems)
		{
			if (item.getTitle().equals(title) && item.getType().getDisplayString().equals(typeDisplayString)) return item;
		}
		return null;
	}
	
	/**
	 * 
	 * @param fromContext
	 * @param toContext
	 * @param modifiedByUserId
	 */
	protected void transferE3ItemMapping(String fromContext, String toContext, String modifiedByUserId)
	{
		//get all fromsite items
		List<GradebookItem> fromGradebookItems = gradebookService.getImportGradebookItems(fromContext, modifiedByUserId, false, true, null, true, null);
		Gradebook fromGb = gradebookService.getContextGradebook(fromContext, modifiedByUserId);
		List<GradebookCategory> fromGradebookCategories = findE3ContextCategories(fromGb.getId());
		List<GradebookCategoryItemMap> fromItemsMap = findItemMapping(fromContext);
		if (fromItemsMap == null || fromItemsMap.size() == 0) return;
		
		//get all tosite items
		List<GradebookItem> toGradebookItems = gradebookService.getImportGradebookItems(toContext, modifiedByUserId, false, true, null, true, null);
		Gradebook gb = gradebookService.getContextGradebook(toContext, modifiedByUserId);
		List<GradebookCategory> toGradebookCategories = findE3ContextCategories(gb.getId());	
		List<GradebookCategoryItemMap> toItemsMap = findItemMapping(toContext);

		// associate item map elements with their category. Need for comparator below
		if (toItemsMap == null) toItemsMap = new ArrayList<GradebookCategoryItemMap>();
	
		// sperate out standard and custom categories
		List<GradebookCategoryItemMap> standardItemMap = new ArrayList<GradebookCategoryItemMap>();
		List<GradebookCategoryItemMap> customItemMap = new ArrayList<GradebookCategoryItemMap>();	
		
		for (GradebookCategoryItemMap associateCategory : toItemsMap)
		{
			GradebookCategory toCategory = findExistingCategory(associateCategory.getCategoryId(), toGradebookCategories);
			if (toCategory == null) toItemsMap.remove(associateCategory);
			else 
			{
				((GradebookCategoryItemMapImpl)associateCategory).setCategory(toCategory);
				if (toCategory.getCategoryType().getCode() == 1) standardItemMap.add(associateCategory);
				else if (toCategory.getCategoryType().getCode() == 2) customItemMap.add(associateCategory);
			}
		}	
		
		// transfer mapping		
		for (GradebookCategoryItemMap item : fromItemsMap)
		{
			GradebookItem fromItem = findGradebookItem(item.getItemId(), fromGradebookItems);
			
			GradebookCategory fromCategory = findExistingCategory(item.getCategoryId(), fromGradebookCategories);
							
			if (fromItem == null || fromCategory == null) continue;
		
			GradebookItem findToItem = findMatchingItem(fromItem.getTitle(), fromItem.getType().getDisplayString(), toGradebookItems);
			GradebookCategory findToCategory = findExistingCategory(fromCategory.getCategoryType(), fromCategory.getStandardCategoryCode(), fromCategory.getTitle(), toGradebookCategories);
			if(findToItem == null || findToCategory == null) continue;
			
			GradebookCategoryItemMap gitem = gradebookService.newGradebookCategoryItemMap(findToItem.getId(), findToCategory.getId(), item.getDisplayOrder()); 
			((GradebookCategoryItemMapImpl)gitem).setCategory(findToCategory);
			((GradebookCategoryItemMapImpl)gitem).setDisplayOrder(item.getDisplayOrder());
			
			// if to site doesn't have itemId - categoryId combination then add
			if (findToCategory.getCategoryType().getCode() == 1 && !(standardItemMap.contains(gitem)))
				standardItemMap.add(gitem);
			
			if (findToCategory.getCategoryType().getCode() == 2 && !(customItemMap.contains(gitem)))
				customItemMap.add(gitem);	
		}	
		
		//check display number or re-sort			
		Collections.sort(standardItemMap, new GradebookCategoryItemMapComparator(true));
	
		//create standard new map
		List<GradebookCategoryItemMap> itemMap = new ArrayList<GradebookCategoryItemMap>();
		int loopCount = 0;
		for (GradebookCategoryItemMap mapItem : standardItemMap)
		{
			int displayNumber = (mapItem.getDisplayOrder() == 0) ? 0 : ++loopCount;
			mapItem.setDisplayOrder(displayNumber);
			itemMap.add(mapItem);
		}
		
		//create new custom map
		Collections.sort(customItemMap, new GradebookCategoryItemMapComparator(true));
		
		if (itemMap.size() > 0) gradebookService.modifyImportItemMapping(toContext, Gradebook.CategoryType.Standard, itemMap, modifiedByUserId);
		
		itemMap = new ArrayList<GradebookCategoryItemMap>();
		loopCount = 0;
		for (GradebookCategoryItemMap mapItem : customItemMap)
		{
			int displayNumber = (mapItem.getDisplayOrder() == 0) ? 0 : ++loopCount;
			mapItem.setDisplayOrder(displayNumber);
			itemMap.add(mapItem);
		}

		if (itemMap.size() > 0) gradebookService.modifyImportItemMapping(toContext, Gradebook.CategoryType.Custom, itemMap, modifiedByUserId);
	}
	
	/**
	 * 
	 * @param toGradebookCategories
	 * @param transferCategories
	 * @param type
	 * @param categoryCode
	 * @param title
	 * @param weight
	 * @param weightDistribution
	 * @param dropNumber
	 * @param order
	 */
	public void addCategoryforTransfer(List<GradebookCategory> toGradebookCategories, List<GradebookCategory> transferCategories, CategoryType type,
			int categoryCode, String title, Float weight, WeightDistribution weightDistribution, int dropNumber, int order) 
	{
		GradebookCategory existsToCategory = findExistingCategory(type, categoryCode, title, toGradebookCategories);
		if (existsToCategory != null) 
		{
			existsToCategory.setTitle(title.trim());
			existsToCategory.setWeight(weight);
			existsToCategory.setWeightDistribution(weightDistribution);
			existsToCategory.setOrder(order);
			existsToCategory.setDropNumberLowestScores(dropNumber);
			transferCategories.add(existsToCategory);
		} 
		else
		{
			GradebookCategory newCategory = gradebookService.newGradebookCategory(title, weight, weightDistribution, order, type);
			newCategory.setDropNumberLowestScores(dropNumber);
			transferCategories.add(newCategory);
		}
	}
	
	/**
	 * Import E3 Gradebook's Grade options from Site A to site B
	 * @param fromGb
	 * @param gb
	 * @param toContext
	 * @param modifiedByUserId
	 */
	protected void transferE3GradeOptions(Gradebook fromGb, Gradebook gb, String toContext, String modifiedByUserId)
	{
		List<GradingScale> fromScales = fromGb.getContextGradingScales();
		List<GradingScale> toScales = gb.getContextGradingScales();
		for (GradingScale gs : fromScales)
		{
			Integer scaleId = null;

			for (GradingScale t : toScales)
			{
				if (t.getType().getScaleType() == gs.getType().getScaleType())
				{
					scaleId = t.getId();
					break;
				}
			}
			if (scaleId == null) continue;

			// letter grade mapping
			List<GradingScalePercent> fromScalePercent = gs.getGradingScalePercent();
			Map<String, Float> transferLetterGrades = new HashMap<String, Float>();
			for (GradingScalePercent p : fromScalePercent)
				transferLetterGrades.put(p.getLetterGrade(), p.getPercent());
			
			// save in toSite
			gradebookService.modifyGradingScale(gb.getId(), scaleId.intValue(),transferLetterGrades, modifiedByUserId);
		}

		// set other grade options properties
		GradingScale fromDefaultGS = fromGb.getGradingScale();
		Map<String, Float> gradingScalePercent = new HashMap<String, Float>();
		for (GradingScalePercent p : fromDefaultGS.getGradingScalePercent())
			gradingScalePercent.put(p.getLetterGrade(), p.getPercent());
		
		gradebookService.modifyContextGradebook(toContext, fromGb.isShowLetterGrade(), fromGb.getReleaseGrades(), fromDefaultGS.getId(), gradingScalePercent, modifiedByUserId);

		return;
	}
	
	/**
	 * Transfer all e3 gradebook Settings
	 * @param gb
	 * @param toContext
	 * @param modifiedByUserId
	 */
	protected void transferE3Settings(Gradebook gb, String fromContext, String toContext, String modifiedByUserId)
	{
		Gradebook fromGb = gradebookService.getContextGradebook(fromContext, modifiedByUserId);
		if (fromGb == null) return;
		
		//all grade options
		transferE3GradeOptions(fromGb, gb, toContext, modifiedByUserId);		
		
		//categories
		transferE3Categories(fromGb, gb, toContext, modifiedByUserId);
			
		//item mapping	
		transferE3ItemMapping(fromContext, toContext, modifiedByUserId);
		
		//grade booster 
		gradebookService.modifyContextGradebookBoostByAttributes(toContext, fromGb.getBoostUserGradesType(), fromGb.getBoostUserGradesBy(), modifiedByUserId);
	
		return;
 	}	
	
	/**
	 * Transfer UCB manual tasks as Mneme offline items
	 * @param manualItems
	 * @param toContext
	 */
	protected void transferOfflineTasks(List<Map<String,Object>> manualItems, String toContext)
	{
		for (Map<String,Object> item : manualItems)
		{
			try
			{
				Date dueDate = (Date)item.get("due");
				String title = (String)item.get("name");
				Assessment alreadyExists = this.assessmentService.assessmentExists(toContext, title);
				if (alreadyExists != null) continue;
				Assessment assessment = this.assessmentService.newAssessment(toContext);
				assessment.setType(AssessmentType.offline);
				assessment.setTitle(title.trim());
				assessment.setPoints((Float)item.get("points"));
				if (dueDate != null) assessment.getDates().setDueDate(dueDate);
				this.assessmentService.saveAssessment(assessment);
			}
			catch (AssessmentPermissionException e)
			{
				logger.warn("transferCopyEntities: " + e.toString());
			}
			catch (AssessmentPolicyException e)
			{
				logger.warn("transferCopyEntities: " + e.toString());
			}
		}			
	}

	/**
	 * Transfer UCB grading scales
	 * @param scales
	 * @param toContext
	 * @param modifiedByUserId
	 */
	protected void transferUCBGradingScales(Gradebook gb, List<Map<String,Object>> scales, Integer scaleType, Integer ucbScaleId, Integer selectedGradingScale, String toContext, String modifiedByUserId)
	{
		try
		{
			// get e3 gradebook scales
			List<GradingScale> gScales = gb.getContextGradingScales();

			int scale_id = scaleType.intValue();

			for (GradingScale gs : gScales)
			{
				if (gs.getType().getScaleType() == scaleType)
				{
					scale_id = gs.getId();
					break;
				}
			}

			// each UCB scale type transfer letter grade - percent map
			Map<String, Float> transferLetterGrades = new HashMap<String, Float>();	
			for (Map<String, Object> item : scales)
			{
				transferLetterGrades.put((String) item.get("letterGrade"),(Float) item.get("percent"));
			}
			
			// save in e3 gradebook
			if (ucbScaleId != null && selectedGradingScale != null && ucbScaleId.intValue() == selectedGradingScale.intValue())
				gradebookService.modifyContextGradebookGradingScale(toContext, scale_id, transferLetterGrades, modifiedByUserId);
			else gradebookService.modifyGradingScale(gb.getId(), scale_id, transferLetterGrades, modifiedByUserId);
		}
		catch (Exception e)
		{
			logger.warn("transferCopyEntities: " + e.toString());
		}
	}
	
	/**
	 * @inherited
	 */
	public String getToolId()
	{
		return "e3.gradebook";
	}

}
