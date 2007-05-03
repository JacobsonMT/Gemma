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
package ubic.gemma.expression.arrayDesign;

import ubic.gemma.analysis.report.ArrayDesignReportService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignGeneMappingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceUpdateEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignReportServiceTest extends BaseSpringContextTest {

    AuditTrailService ads;

    ArrayDesign ad;

    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        ad = this.getTestPersistentArrayDesign( 10, true );
        ads = ( AuditTrailService ) this.getBean( "auditTrailService" );

        ads.addUpdateEvent( ad, ArrayDesignSequenceUpdateEvent.Factory.newInstance(), "sequences" );

        ads.addUpdateEvent( ad, ArrayDesignSequenceAnalysisEvent.Factory.newInstance(), "alignment" );

        ads.addUpdateEvent( ad, ArrayDesignGeneMappingEvent.Factory.newInstance(), "mapping" );

        Thread.sleep( 100 );

        ads.addUpdateEvent( ad, ArrayDesignSequenceAnalysisEvent.Factory.newInstance(), "alignment 2" );

        ads.addUpdateEvent( ad, ArrayDesignGeneMappingEvent.Factory.newInstance(), "mapping 2" );

        Thread.sleep( 100 );

        endTransaction();
    }

    public void testGenerateArrayDesignSequenceAnalysisEvent() {
        ArrayDesignReportService adrs = ( ArrayDesignReportService ) this.getBean( "arrayDesignReportService" );

        String report = adrs.getLastSequenceAnalysisEvent( ad.getId() );

        log.info( report );
        assertTrue( !report.equals( "[None]" ) );
        assertNotNull( report );
    }

    public void testGenerateArrayDesignSequenceUpdateEvent() {
        ArrayDesignReportService adrs = ( ArrayDesignReportService ) this.getBean( "arrayDesignReportService" );

        String report = adrs.getLastSequenceUpdateEvent( ad.getId() );

        log.info( report );
        assertTrue( !report.equals( "[None]" ) );
        assertNotNull( report );
    }

    public void testGenerateArrayDesignGeneMappingEvent() {
        ArrayDesignReportService adrs = ( ArrayDesignReportService ) this.getBean( "arrayDesignReportService" );

        String report = adrs.getLastGeneMappingEvent( ad.getId() );

        log.info( report );
        assertTrue( !report.equals( "[None]" ) );
        assertNotNull( report );
    }

}
