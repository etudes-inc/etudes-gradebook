/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-impl/impl/src/java/org/etudes/gradebook/impl/GradingScaleImpl.java $
 * $Id: GradingScaleImpl.java 10678 2015-05-01 22:28:17Z murthyt $
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

import org.etudes.gradebook.api.GradingScale;
import org.etudes.gradebook.api.GradingScalePercent;


public class GradingScaleImpl implements GradingScale
{
	protected List<GradingScalePercent> gradingScalePercent = new ArrayList<GradingScalePercent>();
	protected int id;
	protected boolean locked;
	protected String name;
	protected String scaleCode;
	protected GradingScaleType type;
	protected int version;
	
	GradingScaleImpl(){};
	
	GradingScaleImpl(GradingScaleImpl other)
	{
		// gradingScalePercent
		if (other.gradingScalePercent != null && other.gradingScalePercent.size() > 0)
		{
			for (GradingScalePercent gradingScalePercent : other.gradingScalePercent)
			{
				this.gradingScalePercent.add(new GradingScalePercentImpl((GradingScalePercentImpl)gradingScalePercent));
			}
		}
		
		this.id = other.id;
		this.locked = other.locked;
		this.name = other.name;
		this.scaleCode = other.scaleCode;		
		this.type = other.type;		
		this.version = other.version;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<GradingScalePercent> getGradingScalePercent()
	{
		return gradingScalePercent;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getId()
	{
		return id;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getName()
	{
		return name;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getScaleCode()
	{
		return scaleCode;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public GradingScaleType getType()
	{
		return type;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getVersion()
	{
		return version;
	}	
	
	
	/**
	 * {@inheritDoc}
	 */
	public boolean isLocked()
	{
		return locked;
	}
	
	/**
	 * @param id the id to set
	 */
	void setId(int id)
	{
		this.id = id;
	}
	
	/**
	 * @param locked the locked to set
	 */
	void setLocked(boolean locked)
	{
		this.locked = locked;
	}
	
	/**
	 * @param name the name to set
	 */
	void setName(String name)
	{
		this.name = name;
	}

	/**
	 * @param scaleCode the scaleCode to set
	 */
	void setScaleCode(String scaleCode)
	{
		this.scaleCode = scaleCode;
	}

	/**
	 * @param type the type to set
	 */
	void setType(GradingScaleType type)
	{
		this.type = type;
	}

	/**
	 * @param version the version to set
	 */
	void setVersion(int version)
	{
		this.version = version;
	}
}
