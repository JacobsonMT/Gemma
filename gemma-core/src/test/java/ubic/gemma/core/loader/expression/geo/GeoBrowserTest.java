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
package ubic.gemma.core.loader.expression.geo;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.loader.expression.geo.service.GeoBrowser;

/**
 * @author pavlidis
 *
 */
public class GeoBrowserTest {

    private static Log log = LogFactory.getLog( GeoBrowserTest.class );

    @Test
    public void testGetRecentGeoRecords() throws Exception {
        GeoBrowser b = new GeoBrowser();

        try {
            Collection<GeoRecord> res = b.getRecentGeoRecords( 10, 10 );
            assertTrue( res.size() > 0 );
        } catch ( IOException e ) {
            if ( e.getMessage().contains( "GEO returned an error" ) ) {
                log.warn( "GEO returned an error, skipping test." );
                return;
            }
            throw e;
        }
    }
    
    @Test
    public void testGetGeoRecordsBySearchTerm() throws Exception {
    	GeoBrowser b = new GeoBrowser();
    	
    	try {
            Collection<GeoRecord> res = b.getGeoRecordsBySearchTerm( "Homo+sapiens[orgn]", 10, 10 );
            // Check that the search has returned at least one record
            assertTrue( res.size() > 0 );
            
            Iterator<GeoRecord> iterator = res.iterator();
            
            // Print out accession numbers etc.; check that the records returned match the search term
            while ( iterator.hasNext() ) {
               	GeoRecord record = iterator.next();
               	System.out.println( "Accession: " + record.getGeoAccession() );
               	System.out.println( "Title : " + record.getTitle() );
               	System.out.println( "Number of samples: " + record.getNumSamples() );
               	System.out.println( "Date: " + record.getReleaseDate() );
               	assertTrue(record.getOrganisms().contains( "Homo sapiens" ));
               }
    	
    	 } catch ( IOException e ) {
             if ( e.getMessage().contains( "GEO returned an error" ) ) {
                 log.warn( "GEO returned an error, skipping test." );
                 return;
             }
             throw e;
    	 }
    }
    
    /* Make the method public to run this test */
//    @Test
//    public void testGetTaxonCollection() throws Exception {
//    	GeoBrowser b = new GeoBrowser();
//    	Collection<String> oneTaxon = b.getTaxonCollection( "Homo sapiens" );
//    	assertTrue(oneTaxon.size() == 1);
//    	Collection<String> twoTaxa = b.getTaxonCollection( "Homo sapiens; Mus musculus" );
//    	assertTrue(twoTaxa.size() == 2);
//    	assertTrue(twoTaxa.contains( "Homo sapiens" ));
//    	assertTrue(twoTaxa.contains( "Mus musculus" ));
//    }

}
