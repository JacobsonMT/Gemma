/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.web.controller.monitoring;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.job.grid.util.SpaceMonitor;
import ubic.gemma.job.grid.util.SpacesUtil;
import ubic.gemma.job.grid.worker.SpacesRegistrationEntry;
import ubic.gemma.util.monitor.CacheMonitor;
import ubic.gemma.util.monitor.HibernateMonitor;

/**
 * Provide statistics about the system: hibernate, caches etc.
 * 
 * @author paul
 * @version $Id$
 */
@Controller
public class SystemMonitorController {

    @Autowired
    CacheMonitor cacheMonitor;

    @Autowired
    HibernateMonitor hibernateMonitor;

    @Autowired
    SpaceMonitor spaceMonitor;

    @Autowired
    SpacesUtil spacesUtil;

    /**
     * Flush (clear) all caches. Expose to AJAX
     */
    public void flushAllCaches() {
        this.cacheMonitor.flushAllCaches();
    }

    /**
     * Flush (clear) a cache.
     * 
     * @param cache name Expose to AJAX
     */
    public void flushCache( String name ) {
        this.cacheMonitor.flushCache( name );
    }

    public String getCacheStatus() {
        return cacheMonitor.getStats();
    }

    /**
     * Expose to AJAX
     * 
     * @return
     */
    public String getHibernateStatus() {
        return this.hibernateMonitor.getStats( true, true, true );
    }

    /**
     * Expose to AJAX
     * 
     * @return
     */

    public String getSpaceStatus() {
        StringBuilder buf = new StringBuilder();

        List<SpacesRegistrationEntry> registeredWorkers = spacesUtil.getRegisteredWorkers();

        String lastStatusMessage = spaceMonitor.getLastStatusMessage();
        Boolean lastStatusWasOK = spaceMonitor.getLastStatusWasOK();

        buf.append( "<p>" );
        if ( !lastStatusWasOK ) {
            buf.append( "<span style='color:red'>Grid error; monitor reports: " + lastStatusMessage + "</span>" );
        } else {
            buf.append( "Grid status is nominal " + lastStatusMessage );
        }

        buf.append( "</p>" );

        if ( registeredWorkers != null ) {

            // List<SpacesBusyEntry> busyWorkers = spacesUtil.getBusyWorkers();
            //
            // Set<String> status = new HashSet<String>();
            // for ( SpacesBusyEntry e : busyWorkers ) {
            // status.add( e.message );
            // }

            buf.append( "\n<h2>Workers</h2>" );
            for ( SpacesRegistrationEntry e : registeredWorkers ) {
                buf.append( e.message + "</br>\n" );
            }
        }

        buf.append( "\n<h2>Statistics</h2>" );
        buf.append( "<pre>" + SpacesUtil.logSpaceStatistics() + "</pre>" );

        return buf.toString();
    }

    /**
     * Used for external monitoring.
     * 
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/gridStatus.html")
    public ModelAndView gridStatus( HttpServletRequest request, HttpServletResponse response ) {
        String spaceStatus = getSpaceStatus();
        return new ModelAndView( "systemNotices" ).addObject( "status", spaceStatus );
    }

    /**
     * @param cacheMonitor the cacheMonitor to set
     */
    public void setCacheMonitor( CacheMonitor cacheMonitor ) {
        this.cacheMonitor = cacheMonitor;
    }

    /**
     * @param hibernateMonitor the hibernateMonitor to set
     */
    public void setHibernateMonitor( HibernateMonitor hibernateMonitor ) {
        this.hibernateMonitor = hibernateMonitor;
    }

    @RequestMapping(value = "/admin/systemStats.html", method = RequestMethod.GET)
    public String show() {
        return "/admin/systemStats";
    }
}
