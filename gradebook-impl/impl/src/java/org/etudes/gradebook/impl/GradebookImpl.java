/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-impl/impl/src/java/org/etudes/gradebook/impl/GradebookImpl.java $
 * $Id: GradebookImpl.java 10678 2015-05-01 22:28:17Z murthyt $
 ***********************************************************************************
 *
 * Copyright (c) 2014 Etudes, Inc.
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

import java.util.ArrayList;
import java.util.List;

import org.etudes.gradebook.api.Gradebook;
import org.etudes.gradebook.api.GradebookCategory;
import org.etudes.gradebook.api.GradingScale;

public class GradebookImpl implements Gradebook
{
	protected Float boostUserGradesBy;
	protected BoostUserGradesType boostUserGradesType = null;
	protected CategoryType categoryType;
	protected String context;
	// site grading scales
	protected List<GradingScale> contextGradingScales = new ArrayList<GradingScale>();
	protected String createdByUserId;
	protected Boolean dropLowestScore = Boolean.FALSE;
	protected List<GradebookCategory> gradebookCategories = new ArrayList<GradebookCategory>();
	protected GradingScale gradingScale;
	protected int id;
	protected String modifiedByUserId;
	protected ReleaseGrades releaseGrades;
	protected Boolean showLetterGrade = Boolean.FALSE;
	protected GradebookSortType sortType =  GradebookSortType.Category;
	
	GradebookImpl(){};
	
	GradebookImpl(GradebookImpl other)
	{
		this.boostUserGradesBy =  other.boostUserGradesBy;		
		this.boostUserGradesType = other.boostUserGradesType;		
		this.categoryType = other.categoryType;
		
		this.context = other.context;
		
		if (other.contextGradingScales != null && other.contextGradingScales.size() > 0)
		{
			for (GradingScale gradingScale : other.contextGradingScales)
			{
				this.contextGradingScales.add(new GradingScaleImpl((GradingScaleImpl)gradingScale));
			}
		}
		
		this.createdByUserId = other.createdByUserId;		
		this.dropLowestScore = other.dropLowestScore;
		
		if (other.gradebookCategories != null && other.gradebookCategories.size() > 0)
		{
			for (GradebookCategory gradebookCategory : other.gradebookCategories)
			{
				this.gradebookCategories.add(new GradebookCategoryImpl((GradebookCategoryImpl)gradebookCategory));
			}
		}
		
		if (other.gradingScale != null)
		{
			this.gradingScale = new GradingScaleImpl((GradingScaleImpl)other.gradingScale);
		}
		this.id = other.id;
		this.modifiedByUserId = other.modifiedByUserId;
		this.releaseGrades = other.releaseGrades;
		this.showLetterGrade = other.showLetterGrade;
		this.sortType = other.sortType;
	};
	
	
	/**
	 * @return the boostUserGradesBy
	 */
	public Float getBoostUserGradesBy()
	{
		return boostUserGradesBy;
	}

	/**
	 * @return the boostUserGradesType
	 */
	public BoostUserGradesType getBoostUserGradesType()
	{
		return boostUserGradesType;
	}

	/**
	 * {@inheritDoc}
	 */
	public CategoryType getCategoryType()
	{
		return categoryType;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getContext()
	{
		return context;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<GradingScale> getContextGradingScales()
	{
		return contextGradingScales;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getCreatedByUserId()
	{
		return createdByUserId;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<GradebookCategory> getGradebookCategories()
	{
		return gradebookCategories;
	}

	/**
	 * {@inheritDoc}
	 */
	public GradingScale getGradingScale()
	{
		return gradingScale;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getId()
	{
		return id;
	}

	/**
	 * @return the modifiedByUserId
	 */
	public String getModifiedByUserId()
	{
		return modifiedByUserId;
	}

	/**
	 * {@inheritDoc}
	 */
	public ReleaseGrades getReleaseGrades()
	{
		return releaseGrades;
	}

	/**
	 * {@inheritDoc}
	 */
	public GradebookSortType getSortType()
	{
		return sortType;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean isDropLowestScore()
	{
		return dropLowestScore;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean isShowLetterGrade()
	{
		return showLetterGrade;
	}

	/**
	 * @param boostUserGradesBy the boostUserGradesBy to set
	 */
	public void setBoostUserGradesBy(Float boostUserGradesBy)
	{
		this.boostUserGradesBy = boostUserGradesBy;
	}

	/**
	 * @param boostUserGradesType the boostUserGradesType to set
	 */
	public void setBoostUserGradesType(BoostUserGradesType boostUserGradesType)
	{
		this.boostUserGradesType = boostUserGradesType;
	}

	/**
	 * @param dropLowestScore the dropLowestScore to set
	 */
	public void setDropLowestScore(Boolean dropLowestScore)
	{
		this.dropLowestScore = dropLowestScore;
	}
	
	/**
	 * @param gradebookCategories the gradebookCategories to set
	 */
	public void setGradebookTypes(List<GradebookCategory> gradebookCategories)
	{
		this.gradebookCategories = gradebookCategories;
	}
	
	/**
	 * @param releaseGrades the releaseGrades to set
	 */
	public void setReleaseGrades(ReleaseGrades releaseGrades)
	{
		this.releaseGrades = releaseGrades;
	}
	
	/**
	 * @param showLetterGrade the showLetterGrade to set
	 */
	public void setShowLetterGrade(boolean showLetterGrade)
	{
		this.showLetterGrade = showLetterGrade;
	}
	
	/**
	 * @param category the category type to set
	 */
	void setCategoryType(CategoryType categoryType)
	{
		this.categoryType = categoryType;
	}
	
	
	/**
	 * @param context the context to set
	 */
	void setContext(String context)
	{
		this.context = context;
	}
	
	/**
	 * @param createdByUserId the createdByUserId to set
	 */
	void setCreatedByUserId(String createdByUserId)
	{
		this.createdByUserId = createdByUserId;
	}
	
	/**
	 * @param gradingScale the gradingScale to set
	 */
	void setGradingScale(GradingScale gradingScale)
	{
		this.gradingScale = gradingScale;
	}
	
	/**
	 * @param id the id to set
	 */
	void setId(int id)
	{
		this.id = id;
	}
	
	/**
	 * @param modifiedByUserId the modifiedByUserId to set
	 */
	void setModifiedByUserId(String modifiedByUserId)
	{
		this.modifiedByUserId = modifiedByUserId;
	}	
		
	/**
	 * @param sortType the sortType to set
	 */
	void setSortType(GradebookSortType sortType)
	{
		this.sortType = sortType;
	}
}
