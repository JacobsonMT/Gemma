/*
 * The Gemma project
 *
 * Copyright (c) 2010 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.apps;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.loader.expression.geo.service.GeoBrowserService;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.AbstractCLIContextCLI;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Scans GEO for experiments that are not in Gemma.
 *
 * @author paul
 */
public class GeoGrabberCli extends AbstractCLIContextCLI {

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.ANALYSIS;
    }

    @Override
    public String getCommandName() {
        return "listGEOData";
    }

    @Override
    protected void buildOptions() {
    }

    @Override
    protected void processOptions() throws Exception {

    }

    @Override
    protected void doWork() throws Exception {
        Set<String> seen = new HashSet<>();
        GeoBrowserService gbs = this.getBean( GeoBrowserService.class );
        ExpressionExperimentService ees = this.getBean( ExpressionExperimentService.class );

        int start = 0;
        int numfails = 0;
        int chunksize = 100;

        while ( true ) {
            List<GeoRecord> recs = gbs.getRecentGeoRecords( start, chunksize );

            if ( recs.isEmpty() ) {
                AbstractCLI.log.info( "No records received for start=" + start );
                numfails++;

                if ( numfails > 10 ) {
                    AbstractCLI.log.info( "Giving up" );
                    break;
                }

                try {
                    Thread.sleep( 500 );
                } catch ( InterruptedException ignored ) {
                }

                start++;
                continue;
            }

            start++;

            for ( GeoRecord geoRecord : recs ) {
                if ( seen.contains( geoRecord.getGeoAccession() ) ) {
                    continue;
                }

                if ( ees.findByShortName( geoRecord.getGeoAccession() ) != null ) {
                    continue;
                }

                if ( !ees.findByAccession( geoRecord.getGeoAccession() ).isEmpty() ) {
                    continue;
                }

                System.out.println(
                        geoRecord.getGeoAccession() + "\t" + geoRecord.getOrganisms().iterator().next() + "\t"
                                + geoRecord.getNumSamples() + "\t" + geoRecord.getTitle() + "\t" + StringUtils
                                .join( geoRecord.getCorrespondingExperiments(), "," ) + "\t" + geoRecord
                                .getSeriesType() );
                seen.add( geoRecord.getGeoAccession() );
            }
        }
    }

    @Override
    public String getShortDesc() {
        return "Grab information on GEO data sets not yet in the system";
    }
}
