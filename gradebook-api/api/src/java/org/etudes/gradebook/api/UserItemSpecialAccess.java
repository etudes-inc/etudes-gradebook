package org.etudes.gradebook.api;

import java.util.Date;
import java.util.List;

public interface UserItemSpecialAccess
{
	/**
	 * @return the acceptUntilDate
	 */
	Date getAcceptUntilDate();

	/**
	 * @return the dueDate
	 */
	Date getDueDate();

	/**
	 * @return the hideUntilOpen
	 */
	Boolean getHideUntilOpen();

	/**
	 * @return the id
	 */
	// String getId();

	/**
	 * @return the openDate
	 */
	Date getOpenDate();

	/**
	 * @return the overrideAcceptUntilDate
	 */
	Boolean getOverrideAcceptUntilDate();

	/**
	 * @return the overrideDueDate
	 */
	Boolean getOverrideDueDate();

	/**
	 * @return the overrideHideUntilOpen
	 */
	Boolean getOverrideHideUntilOpen();

	/**
	 * @return the overrideOpenDate
	 */
	Boolean getOverrideOpenDate();
	
	Boolean getIsValid();
}
