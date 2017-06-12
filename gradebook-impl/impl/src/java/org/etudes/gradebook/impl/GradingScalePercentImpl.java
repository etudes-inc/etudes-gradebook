/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-impl/impl/src/java/org/etudes/gradebook/impl/GradingScalePercentImpl.java $
 * $Id: GradingScalePercentImpl.java 10987 2015-06-01 18:43:22Z murthyt $
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

import org.etudes.gradebook.api.GradingScalePercent;


public class GradingScalePercentImpl implements GradingScalePercent
{
	protected int id;
	protected String letterGrade;
	protected Float percent;
	protected int scaleId;
	protected int sequenceNumber;
	
	GradingScalePercentImpl() {};
	
	GradingScalePercentImpl(GradingScalePercentImpl other)
	{
		this.id = other.id;
		this.letterGrade = other.letterGrade;
		this.percent = other.percent;
		this.scaleId = other.scaleId;
		this.sequenceNumber = other.sequenceNumber;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getLetterGrade()
	{
		return letterGrade;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Float getPercent()
	{
		return roundToTwoDecimals(percent);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getScaleId()
	{
		return scaleId;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getSequenceNumber()
	{
		return sequenceNumber;
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
	 * @param id the id to set
	 */
	void setId(int id)
	{
		this.id = id;
	}
	
	/**
	 * @param letterGrade the letterGrade to set
	 */
	void setLetterGrade(String letterGrade)
	{
		this.letterGrade = letterGrade;
	}
	
	/**
	 * @param percent the percent to set
	 */
	void setPercent(Float percent)
	{
		this.percent = percent;
	}
	
	/**
	 * @param scaleId the scaleId to set
	 */
	void setScaleId(int scaleId)
	{
		this.scaleId = scaleId;
	}
	/**
	 * @param sequenceNumber the sequenceNumber to set
	 */
	void setSequenceNumber(int sequenceNumber)
	{
		this.sequenceNumber = sequenceNumber;
	}
}
