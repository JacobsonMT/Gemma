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

import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import junit.framework.TestCase;

/**
 * @author pavlidis
 * @version $Id$
 */
public class GoldenPathQueryTest extends TestCase {

    GoldenPathQuery queryer;

    @Override
    protected void setUp() throws Exception {
        Taxon t = Taxon.Factory.newInstance();
        t.setCommonName( "human" );
        queryer = new GoldenPathQuery( t );
    }

    public final void testQueryEst() throws Exception {
        Collection<BlatResult> actualValue = queryer.findAlignments( "AA411542" );
        assertEquals( 5, actualValue.size() );
    }

    public final void testQueryMrna() throws Exception {
        Collection<BlatResult> actualValue = queryer.findAlignments( "CR609160" );
        assertEquals( 1, actualValue.size() );
        BlatResult r = actualValue.iterator().next();
        assertEquals( "CR609160", ( r.getQuerySequence().getName() ) );
    }

    public final void testQueryNoResult() throws Exception {
        Collection<BlatResult> actualValue = queryer.findAlignments( "YYYYYUUYUYUYUY" );
        assertEquals( 0, actualValue.size() );
    }

}
