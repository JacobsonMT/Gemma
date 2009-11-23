/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.web.listener;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * UserCounterListener class used to count the current number of active users for the applications. Does this by
 * counting how many user objects are stuffed into the session. It also grabs these users and exposes them in the
 * servlet context.
 * <p>
 * Orignal idea from Appfuse.
 * 
 * @author keshav
 * @author pavlidis
 * @version $Id$
 */
public class UserCounterListener implements ServletContextListener, HttpSessionListener {

    public static final String COUNT_KEY = "activeUsers";
    public static final String AUTH_KEY = "authenticatedUsers";

    private ServletContext servletContext;

    public synchronized void contextInitialized( ServletContextEvent sce ) {
        this.servletContext = sce.getServletContext();
        servletContext.setAttribute( ( COUNT_KEY ), Integer.toString( 0 ) );
        servletContext.setAttribute( ( AUTH_KEY ), Integer.toString( 0 ) );
    }

    public synchronized void contextDestroyed( ServletContextEvent event ) {
        servletContext = null;
    }

    public void sessionCreated( HttpSessionEvent arg0 ) {
        UserTracker.incrementSessions();
        servletContext.setAttribute( COUNT_KEY, Integer.toString( UserTracker.getActiveSessions() ) );

    }

    public void sessionDestroyed( HttpSessionEvent arg0 ) {
        UserTracker.decrementSessions();
        servletContext.setAttribute( COUNT_KEY, Integer.toString( UserTracker.getActiveSessions() ) );
    }

}
