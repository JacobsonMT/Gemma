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
package ubic.gemma.externalDb;

import java.util.Collection;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

/**
 * These tests require a populated Human database. Valid as of 1/2008.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GoldenPathQueryTest extends TestCase {

    private static Log log = LogFactory.getLog( GoldenPathQueryTest.class.getName() );
    GoldenPathQuery queryer;
    private boolean hasDb = true;;

    @Override
    protected void setUp() throws Exception {
        Taxon t = Taxon.Factory.newInstance();
        t.setCommonName( "human" );
        try {
            queryer = new GoldenPathQuery( t );
        } catch ( java.sql.SQLException e ) {
            if ( e.getMessage().contains( "Unknown database" ) ) {
                hasDb = false;
            } else if ( e.getMessage().contains( "Access denied" ) ) {
                hasDb = false;
            }
            throw e;
        }
    }

    public final void testQueryEst() throws Exception {
        if ( !hasDb ) {
            log.warn( "Skipping test because hg18 could not be configured" );
            return;
        }
        Collection<BlatResult> actualValue = queryer.findAlignments( "AA411542" );
        assertEquals( 5, actualValue.size() );
    }

    public final void testQueryMrna() throws Exception {
        if ( !hasDb ) {
            log.warn( "Skipping test because hg18 could not be configured" );
            return;
        }
        Collection<BlatResult> actualValue = queryer.findAlignments( "AK095183" );
      //  assertEquals( 3, actualValue.size() );
        assertTrue(actualValue.size() > 0); // value used to be 3, now 2; this should be safer.
        BlatResult r = actualValue.iterator().next();
        assertEquals( "AK095183", ( r.getQuerySequence().getName() ) );
    }

    public final void testQueryNoResult() throws Exception {
        if ( !hasDb ) {
            log.warn( "Skipping test because hg18 could not be configured" );
            return;
        }
        Collection<BlatResult> actualValue = queryer.findAlignments( "YYYYYUUYUYUYUY" );
        assertEquals( 0, actualValue.size() );
    }

}
