/*
 * The Gemma project
 *
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.core.loader.protein;

import org.junit.Before;
import org.junit.Test;
import ubic.gemma.core.loader.protein.biomart.BiomartEnsemblNcbiObjectGenerator;
import ubic.gemma.core.loader.protein.biomart.model.Ensembl2NcbiValueObject;
import ubic.gemma.core.loader.protein.string.model.StringProteinProteinInteraction;
import ubic.gemma.model.association.Gene2GeneProteinAssociation;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test of string biomart protein converter, ensuring that if given 3 string interactions they map correctly to biomart
 * file with many to many relationships.
 *
 * @author     ldonnison
 * @deprecated use the stored ensembl ids instead in Gemma
 */
@Deprecated
public class StringBiomartProteinConverterTest {

    private final Collection<Taxon> taxa = new ArrayList<>();
    private final Collection<StringProteinProteinInteraction> stringProteinProteinInteractions = new ArrayList<>();
    private StringProteinProteinInteractionConverter stringBiomartProteinConverter = null;
    private StringProteinProteinInteraction stringProteinProteinInteractionOne;

    @Before
    public void setUp() {
        String fileNameBiomartmouse = "/data/loader/protein/biomart/biomartmmusculusShort.txt";
        URL fileNameBiomartmouseURL = this.getClass().getResource( fileNameBiomartmouse );
        File taxonBiomartFile = new File( fileNameBiomartmouseURL.getFile() );

        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setIsGenesUsable( true );
        taxon.setNcbiId( 10090 );
        taxon.setScientificName( "Mus musculus" );
        taxa.add( taxon );
        try {
            BiomartEnsemblNcbiObjectGenerator biomartEnsemblNcbiObjectGenerator = new BiomartEnsemblNcbiObjectGenerator();
            biomartEnsemblNcbiObjectGenerator.setBioMartFileName( taxonBiomartFile );
            Map<String, Ensembl2NcbiValueObject> map = biomartEnsemblNcbiObjectGenerator.generate( taxa );
            stringBiomartProteinConverter = new StringProteinProteinInteractionConverter( map );
        } catch ( Exception e ) {
            e.printStackTrace();
            fail();
        }

        stringProteinProteinInteractionOne = new StringProteinProteinInteraction( "ENSMUSP00000111623",
                "ENSMUSP00000100396" );

        StringProteinProteinInteraction stringProteinProteinInteractionTwo = new StringProteinProteinInteraction(
                "ENSMUSP00000100395", "ENSMUSP00000100396" );

        StringProteinProteinInteraction stringProteinProteinInteractionThree = new StringProteinProteinInteraction(
                "ENSMUSP00000100407", "ENSMUSP00000100395" );

        // add them to array
        stringProteinProteinInteractions.add( stringProteinProteinInteractionOne );
        stringProteinProteinInteractions.add( stringProteinProteinInteractionTwo );
        stringProteinProteinInteractions.add( stringProteinProteinInteractionThree );

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConvertCollectionOfObject() {

        try {
            Object object = stringBiomartProteinConverter.convert( stringProteinProteinInteractions );

            if ( object != null ) {
                // method returns a collection of arraylists
                // these arraylist represent one string line which can get mapped to multiple
                // Gene2GeneProteinAssociation
                // if the ensemble peptide id maps to more than one ncbi id
                Collection<ArrayList<Gene2GeneProteinAssociation>> objects = ( Collection<ArrayList<Gene2GeneProteinAssociation>> ) object;
                assertEquals( 3, objects.size() );
                for ( ArrayList<Gene2GeneProteinAssociation> gene2GeneProteins : objects ) {

                    for ( Gene2GeneProteinAssociation gene2GeneProteinAssociation : gene2GeneProteins ) {
                        assertNotNull( gene2GeneProteinAssociation.getDatabaseEntry().getAccession() );
                        assertNotNull( gene2GeneProteinAssociation.getFirstGene() );
                        assertNotNull( gene2GeneProteinAssociation.getSecondGene() );
                    }
                }
            }

        } catch ( Exception e ) {
            e.printStackTrace();
            fail();
        }

    }

    /*
     * ENSMUSP00000111623 is mapped to two ncbi ids (100040601 and 245269). ENSMUSP00000100396: is mapped to three ncbi
     * ids 100044026, 100043270, 100043197 Should result in returning a collection of 6 which is 2 *3 =6 Test method for
     * {@link
     * ubic.gemma.core.loader.protein.StringProteinProteinInteractionConverter#convert(ubic.gemma.core.loader.protein.
     * string.model.StringProteinProteinInteraction)}
     */
    @Test
    public void testConvertStringProteinProteinInteraction() {
        try {

            Collection<Gene2GeneProteinAssociation> objects = stringBiomartProteinConverter
                    .convert( stringProteinProteinInteractionOne );
            if ( objects != null ) {
                assertEquals( 6, objects.size() );
                for ( Gene2GeneProteinAssociation gene2GeneProteinAssociation : objects ) {
                    assertNotNull( gene2GeneProteinAssociation.getDatabaseEntry().getAccession() );
                    assertNotNull( gene2GeneProteinAssociation.getFirstGene() );
                    assertNotNull( gene2GeneProteinAssociation.getSecondGene() );
                }
            } else {
                fail();
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            fail();
        }
    }

    /*
     * Test that if given one ensembl peptide id ENSMUSP00000111623 then get back two ncbi genes
     */
    @Test
    public void testGetNcbiGene() {
        // peptide maps to two genes in the file biomartmmusculusShort
        // 100040601 or 245269
        Collection<Gene> genes = stringBiomartProteinConverter.getNcbiGene( "ENSMUSP00000111623" );
        assertEquals( 2, genes.size() );
        for ( Gene gene : genes ) {
            assertTrue( ( gene.getNcbiGeneId().equals( 100040601 ) ) || ( gene.getNcbiGeneId().equals( 245269 ) ) );
        }
    }

}
