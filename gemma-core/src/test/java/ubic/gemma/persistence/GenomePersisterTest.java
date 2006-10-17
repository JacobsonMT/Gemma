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
package ubic.gemma.persistence;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.RandomStringUtils;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.testing.BaseTransactionalSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class GenomePersisterTest extends BaseTransactionalSpringContextTest {

    public void testPersistGene() throws Exception {
        endTransaction();
        Gene gene = Gene.Factory.newInstance();
        gene.setName( RandomStringUtils.randomAlphabetic( 10 ) );
        gene.setNcbiId( RandomStringUtils.randomAlphabetic( 10 ) );

        Collection<GeneProduct> gps = new HashSet<GeneProduct>();
        for ( int i = 0; i < 10; i++ ) {
            GeneProduct gp = GeneProduct.Factory.newInstance();
            gp.setName( RandomStringUtils.randomAlphabetic( 10 ) );
            gp.setGene( gene );
            gp.setNcbiId( RandomStringUtils.randomAlphabetic( 10 ) );
            gps.add( gp );
        }

        gene.setProducts( gps );

        gene = ( Gene ) this.persisterHelper.persist( gene );

        assertNotNull( gene.getId() );
        for ( GeneProduct product : gene.getProducts() ) {
            assertNotNull( product.getId() );
        }
    }

    /**
     * Going the opposite way as the other test.
     * 
     * @throws Exception
     */
    public void testPersistGeneProduct() throws Exception {
        Gene gene = Gene.Factory.newInstance();
        gene.setName( RandomStringUtils.randomAlphabetic( 10 ) );
        gene.setNcbiId( RandomStringUtils.randomAlphabetic( 10 ) );

        GeneProduct gp = GeneProduct.Factory.newInstance();
        gp.setName( RandomStringUtils.randomAlphabetic( 10 ) );
        gp.setGene( gene );
        gp.setNcbiId( RandomStringUtils.randomAlphabetic( 10 ) );
        gene.getProducts().add( gp );

        gp = ( GeneProduct ) this.persisterHelper.persist( gp );

        assertNotNull( gp.getId() );
        assertNotNull( gp.getGene().getId() );
    }

}
