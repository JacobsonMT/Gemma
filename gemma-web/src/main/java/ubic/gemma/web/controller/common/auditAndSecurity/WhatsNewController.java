/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.web.controller.common.auditAndSecurity;

import java.util.Calendar;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.DateUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.analysis.report.WhatsNew;
import ubic.gemma.analysis.report.WhatsNewService;
import ubic.gemma.web.controller.BaseMultiActionController;

/**
 * Controller to provide information on "what's new" in the system
 * 
 * @spring.bean id="whatsNewController"
 * @spring.property name="whatsNewService" ref="whatsNewService"
 * @author pavlidis
 * @version $Id$
 */
public class WhatsNewController extends BaseMultiActionController {

    WhatsNewService whatsNewService;

    public void setWhatsNewService( WhatsNewService whatsNewService ) {
        this.whatsNewService = whatsNewService;
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @SuppressWarnings("unused")
    public ModelAndView weekly( HttpServletRequest request, HttpServletResponse response ) {
        ModelAndView mav = new ModelAndView( "wnWeek" );
        Calendar c = Calendar.getInstance();
        Date date = c.getTime();
        date = DateUtils.addWeeks( date, -1 );
        WhatsNew wn = whatsNewService.getReport( date );
        mav.addObject( "whatsnew", wn );
        mav.addObject( "timeSpan", "In the past week" );
        return mav;
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @SuppressWarnings("unused")
    public ModelAndView daily( HttpServletRequest request, HttpServletResponse response ) {
        ModelAndView mav = new ModelAndView( "wnDay" );
        Calendar c = Calendar.getInstance();
        Date date = c.getTime();
        date = DateUtils.addDays( date, -1 );
        WhatsNew wn = whatsNewService.getReport( date );
        mav.addObject( "whatsnew", wn );
        mav.addObject( "timeSpan", "In the past day" );
        return mav;
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @SuppressWarnings("unused")
    public ModelAndView generateCache( HttpServletRequest request, HttpServletResponse response ) {
        ModelAndView mav = new ModelAndView( new RedirectView( "/Gemma/mainMenu.html" ) );

        Calendar c = Calendar.getInstance();
        Date date = c.getTime();
        date = DateUtils.addDays( date, -7 );
        // save a report for a week's duration
        whatsNewService.saveReport( date );

        return mav;
    }

}
