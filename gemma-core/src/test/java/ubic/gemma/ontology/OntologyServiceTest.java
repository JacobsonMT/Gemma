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
package ubic.gemma.ontology;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author paul
 * @version $Id$
 */
public class OntologyServiceTest extends BaseSpringContextTest {

    @Autowired
    OntologyService os;

    /**
     * This test can fail if the db isn't initialized public void testListAvailableOntologies() throws Exception {
     * Collection<Ontology> name = OntologyService.listAvailableOntologies(); assertTrue( name.size() > 0 ); }
     */
    @Test
    public final void testFindExactMatch() throws Exception {
        os.getMgedOntologyService().init( true );
        while ( !os.getMgedOntologyService().isOntologyLoaded() ) {
            Thread.sleep( 1000 );
            log.info( "Waiting for Ontology to load" );
        }
        Collection<Characteristic> name = os.findExactTerm( "male",
                "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#Sex", null );
        for ( Characteristic characteristic : name ) {
            log.info( characteristic );
        }
        assertTrue( name.size() > 0 );
    }

    /**
     * Test for problem with birnlex, which returns 'organ' as a _subclass_ of 'liver'.
     * 
     * @throws Exception
     */
    @Test
    public final void testFindTerm() throws Exception {
        os.getBirnLexOntologyService().init( true );
        while ( !os.getBirnLexOntologyService().isOntologyLoaded() ) {
            Thread.sleep( 5000 );
            log.info( "Waiting for Ontology to load" );
        }
        Collection<OntologyTerm> terms = os.findTerms( "liver" );
        assertTrue( terms.size() > 0 );
        //
        for ( OntologyTerm term : terms ) {
            if ( term.getLabel().contains( "Organ" ) ) {
                fail( "Should get organ as a child of liver" );
            }
        }
    }

}
