package org.etudes.gradebook.api;

import java.util.List;

import org.etudes.gradebook.api.Gradebook.CategoryType;
import org.etudes.gradebook.api.GradebookCategory.WeightDistribution;

public interface GradebookImportService {

	/**
	 * Add categories to transfer list. Find existing category and set its values or adds new category to the list.
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
			int categoryCode, String title, Float weight, WeightDistribution weightDistribution, int dropNumber, int order);

	/**
	 * Find all categories of both category types.
	 * @param id
	 * @return
	 */
	public abstract List<GradebookCategory> findE3ContextCategories(int id);
	
	/**
	 * Finds similar category based on type, code and title.
	 * @param categoryType
	 * @param standardCategoryCode
	 * @param title
	 * @param gcsList
	 * @return null if not found.
	 */
	public abstract GradebookCategory findExistingCategory(CategoryType categoryType, int standardCategoryCode, String title, List<GradebookCategory> gcsList);
	
	/**
	 * Iterate over the list to find the category with given id
	 * @param id
	 * @param gcsList
	 * @return
	 */
	public GradebookCategory findExistingCategory(int id, List<GradebookCategory> gcsList);
	
	/**
	 * Iterate over the list to find the gradebook item with given id
	 * @param find
	 * @param fromGradebookItems
	 * @return
	 */
	public GradebookItem findGradebookItem(String find, List<GradebookItem> fromGradebookItems);
	
	/**
	 * 
	 * @param fromContext
	 * @return
	 */
	public List<GradebookCategoryItemMap> findItemMapping(String fromContext);
}