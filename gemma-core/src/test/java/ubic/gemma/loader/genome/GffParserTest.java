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
package ubic.gemma.loader.genome;

import java.io.InputStream;
import java.util.Collection;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneImpl;
import ubic.gemma.model.genome.Taxon;

import junit.framework.TestCase;

/**
 * @author pavlidis
 * @version $Id$
 */
public class GffParserTest extends TestCase {

    InputStream is;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        is = GffParserTest.class.getResourceAsStream( "/data/loader/genome/microrna-mmu.gff" );
    }

    @Override
    protected void tearDown() throws Exception {
        if (is != null) is.close();
        super.tearDown();
    }

    @SuppressWarnings("unchecked")
    public void testParseInputStream() throws Exception {
        GffParser parser = new GffParser();
        Taxon t = Taxon.Factory.newInstance();
        t.setCommonName( "mouse" );
        t.setScientificName( "Mus musculus" );
        parser.setTaxon( t );
        parser.parse( is );
        Collection<Object> res = parser.getResults();
        assertEquals(382, res.size());
        for ( Object object : res ) {
            assertEquals( GeneImpl.class, object.getClass() );
            Gene gene = ( Gene ) object;
            assertTrue( gene.getName() != null );
            assertFalse( gene.getName().contains( "\"" ) );
            assertEquals( 1, gene.getProducts().size() );
        }
    }

}
