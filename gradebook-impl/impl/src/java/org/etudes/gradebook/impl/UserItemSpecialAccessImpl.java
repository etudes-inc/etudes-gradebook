package org.etudes.gradebook.impl;

import java.util.Date;

import org.etudes.gradebook.api.UserItemSpecialAccess;

public class UserItemSpecialAccessImpl implements UserItemSpecialAccess
{
	protected Date acceptUntilDate = null;

	protected Date dueDate = null;

	protected Boolean hideUntilOpen = Boolean.FALSE;

	protected String id = null;

	protected Date openDate = null;

	protected Boolean overrideAcceptUntilDate = Boolean.FALSE;

	protected Boolean overrideDueDate = Boolean.FALSE;

	protected Boolean overrideHideUntilOpen = Boolean.FALSE;

	protected Boolean overrideOpenDate = Boolean.FALSE;
	
	protected Boolean datesValid = Boolean.TRUE;

	UserItemSpecialAccessImpl(Date openDate, Date dueDate, Date acceptUntilDate, Boolean hideUntilOpen, Boolean overrideOpenDate, Boolean overrideDueDate, Boolean overrideAcceptUntilDate, Boolean overrideHideUntilOpen, Boolean datesValid)
	{
		this.openDate = openDate;
		this.dueDate = dueDate;
		this.acceptUntilDate = acceptUntilDate;
		this.hideUntilOpen = hideUntilOpen;
		this.overrideOpenDate = overrideOpenDate;
		this.overrideDueDate = overrideDueDate;
		this.overrideAcceptUntilDate = overrideAcceptUntilDate;
		this.overrideHideUntilOpen = overrideHideUntilOpen;
		this.datesValid = datesValid;
	}
	
	/**
	 * @return the acceptUntilDate
	 */
	public Date getAcceptUntilDate()
	{
		return acceptUntilDate;
	}

	/**
	 * @return the dueDate
	 */
	public Date getDueDate()
	{
		return dueDate;
	}

	/**
	 * @return the hideUntilOpen
	 */
	public Boolean getHideUntilOpen()
	{
		return hideUntilOpen;
	}

	/**
	 * @return the id
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * @return the openDate
	 */
	public Date getOpenDate()
	{
		return openDate;
	}

	/**
	 * @return the overrideAcceptUntilDate
	 */
	public Boolean getOverrideAcceptUntilDate()
	{
		return overrideAcceptUntilDate;
	}

	/**
	 * @return the overrideDueDate
	 */
	public Boolean getOverrideDueDate()
	{
		return overrideDueDate;
	}

	/**
	 * @return the overrideHideUntilOpen
	 */
	public Boolean getOverrideHideUntilOpen()
	{
		return overrideHideUntilOpen;
	}

	/**
	 * @return the overrideOpenDate
	 */
	public Boolean getOverrideOpenDate()
	{
		return overrideOpenDate;
	}

	/**
	 * @param acceptUntilDate the acceptUntilDate to set
	 */
	public void setAcceptUntilDate(Date acceptUntilDate)
	{
		this.acceptUntilDate = acceptUntilDate;
	}

	/**
	 * @param dueDate the dueDate to set
	 */
	public void setDueDate(Date dueDate)
	{
		this.dueDate = dueDate;
	}

	/**
	 * @param hideUntilOpen the hideUntilOpen to set
	 */
	public void setHideUntilOpen(Boolean hideUntilOpen)
	{
		this.hideUntilOpen = hideUntilOpen;
	}
	
	/**
	 * @param id the id to set
	 */
	public void setId(String id)
	{
		this.id = id;
	}

	/**
	 * @param openDate the openDate to set
	 */
	public void setOpenDate(Date openDate)
	{
		this.openDate = openDate;
	}

	/**
	 * @param overrideAcceptUntilDate the overrideAcceptUntilDate to set
	 */
	public void setOverrideAcceptUntilDate(Boolean overrideAcceptUntilDate)
	{
		this.overrideAcceptUntilDate = overrideAcceptUntilDate;
	}

	/**
	 * @param overrideDueDate the overrideDueDate to set
	 */
	public void setOverrideDueDate(Boolean overrideDueDate)
	{
		this.overrideDueDate = overrideDueDate;
	}

	/**
	 * @param overrideHideUntilOpen the overrideHideUntilOpen to set
	 */
	public void setOverrideHideUntilOpen(Boolean overrideHideUntilOpen)
	{
		this.overrideHideUntilOpen = overrideHideUntilOpen;
	}

	/**
	 * @param overrideOpenDate the overrideOpenDate to set
	 */
	public void setOverrideOpenDate(Boolean overrideOpenDate)
	{
		this.overrideOpenDate = overrideOpenDate;
	}
	
	public Boolean getIsValid()
	{
		// TODO check dates
		
		return datesValid;
	}
}
