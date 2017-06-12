/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-webapp/webapp/src/java/org/etudes/gradebook/cdp/GradebookServlet.java $
 * $Id: GradebookServlet.java 11407 2015-07-30 19:38:12Z rashmim $
 ***********************************************************************************
 *
 * Copyright (c) 2013 Etudes, Inc.
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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.cdp.api.CdpHandler;
import org.etudes.cdp.api.CdpService;
import org.etudes.gradebook.api.*;
import org.etudes.siteimport.api.SiteImportService;
import org.etudes.siteimport.api.SiteImporter;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.entity.api.EntityManager;

/**
 */
public class GradebookServlet extends HttpServlet
{
	/** Our log (commons). */
	private static Log logger = LogFactory.getLog(GradebookServlet.class);

	private static final long serialVersionUID = 1L;

	protected CdpHandler handler = null;
	
	/**
	 * Shutdown the servlet.
	 */
	public void destroy()
	{
		if (logger.isInfoEnabled())
		{
			logger.info("destroy()");
		}

		if (this.handler != null)
		{
			CdpService cdpService = (CdpService) ComponentManager.get(CdpService.class);
			if (cdpService != null)
			{
				cdpService.UnregisterCdpHandler(handler);
				
				logger.info("destroy(): unregistered handler");
			}
		}

		super.destroy();
	}

	/**
	 * Access the Servlet's information display.
	 * 
	 * @return servlet information.
	 */
	public String getServletInfo()
	{
		return "Etudesgradebook";
	}

	/**
	 * Initialize the servlet.
	 * 
	 * @param config
	 *        The servlet config.
	 * @throws ServletException
	 */
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);

		// create and register the gradebook cdp handler - run when the CdpService is available
		final CdpHandler handler = new GradebookCdpHandler();
	
		this.handler = handler;
		ComponentManager.whenAvailable(CdpService.class, new Runnable()
		{
			public void run()
			{
				CdpService cdpService = (CdpService) ComponentManager.get(CdpService.class);
				cdpService.registerCdpHandler(handler);				
				logger.info("init(): registered handler");
			}
		});	
		
		// create and register the gradebook cdp handler - run when the CdpService is available
		ComponentManager.whenAvailable(SiteImportService.class, new Runnable()
		{
			public void run()
			{
				ComponentManager.whenAvailable(GradebookImportService.class, new Runnable()
				{
					public void run()
					{
						SiteImporter gbImportService = (SiteImporter) ComponentManager.get(GradebookImportService.class);
						siteImportService().registerImporter(gbImportService);						
						logger.info("init(): registered site importer handler");
					}
				});
			}
		});	
		
		ComponentManager.whenAvailable(EntityManager.class, new Runnable()
		{
			public void run()
			{
				// create the access provider
				new GradebookAccessProvider((GradebookCdpHandler) handler);
				logger.info("init(): registered e3 gradebook access provider");
			}
		});
	}
	
	/**
	 * @return The EntityManager, via the component manager.
	 */
	private EntityManager entityManager()
	{
		return (EntityManager) ComponentManager.get(EntityManager.class);
	}
	
	/**
	 * @return The SiteImportService, via the component manager.
	 */
	private SiteImportService siteImportService()
	{
		return (SiteImportService) ComponentManager.get(SiteImportService.class);
	}
}
