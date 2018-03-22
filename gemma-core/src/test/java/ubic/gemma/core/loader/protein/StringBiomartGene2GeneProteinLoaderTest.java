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

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.testing.BaseSpringContextTest;
import ubic.gemma.core.testing.PersistentDummyObjectHelper;
import ubic.gemma.model.association.Gene2GeneProteinAssociation;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.persistence.service.association.Gene2GeneProteinAssociationService;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import static org.junit.Assert.*;

/**
 * Tests the loader for string protein interactions: Test 3 out of the 4 scenarios for loading protein protein
 * interactions namely:
 * <ul>
 * <li>Local biomart file, local string file, one taxon
 * <li>Remote biomart file, local string file one taxon
 * <li>Remote biomart file, local string file multi taxon
 * </ul>
 * The only scenario not tested is downloading from string website simply too long recommended usage is to use local
 * file. As is downloads a file from biomart any changes in biomart interface will be picked up. Should add some more
 * error scenarios.
 *
 * @author ldonnison
 */
public class StringBiomartGene2GeneProteinLoaderTest extends BaseSpringContextTest {

    private final Collection<Gene> genesZebra = new HashSet<>();
    @Autowired
    private Gene2GeneProteinAssociationService gene2GeneProteinAssociationService;
    @Autowired
    private GeneService geneService = null;
    private Taxon rat = null;
    private StringProteinInteractionLoader stringBiomartGene2GeneProteinAssociationLoader = null;

    private Collection<Taxon> taxa = null;
    private Taxon zebraFish = null;

    /**
     * Set up and save some taxa.
     *
     * @return taxa
     */
    public Collection<Taxon> getTaxonToProcess() {
        taxa = new ArrayList<>();

        zebraFish = Taxon.Factory.newInstance();
        zebraFish.setIsGenesUsable( true );
        zebraFish.setNcbiId( 7955 );
        zebraFish.setScientificName( "Danio rerio" );
        zebraFish.setIsSpecies( true );

        persisterHelper.persist( zebraFish );
        taxa.add( ( Taxon ) persisterHelper.persist( zebraFish ) );

        rat = Taxon.Factory.newInstance();
        rat.setIsGenesUsable( true );
        rat.setNcbiId( 10116 );
        rat.setScientificName( "Rattus norvegicus" );
        rat.setIsSpecies( true );
        persisterHelper.persist( rat );
        taxa.add( ( Taxon ) persisterHelper.persist( rat ) );

        return taxa;
    }

    public void getTestPeristentGenesRat() {

        //noinspection MismatchedQueryAndUpdateOfCollection
        Collection<Gene> genesRat = new ArrayList<>();

        Gene geneRatOne = this.makeGene( rat, RandomStringUtils.randomAlphabetic( 4 ).toUpperCase(), "679739" );
        genesRat.add( geneRatOne );

        Gene geneRatTwo = this.makeGene( rat, RandomStringUtils.randomAlphabetic( 4 ).toUpperCase(), "297433" );
        genesRat.add( geneRatTwo );
        Gene geneRatThree = this.makeGene( rat, RandomStringUtils.randomAlphabetic( 4 ).toUpperCase(), "399475" );
        genesRat.add( geneRatThree );

        genesRat.add( this.makeGene( rat, RandomStringUtils.randomAlphabetic( 4 ).toUpperCase(), "123445" ) );
        genesRat.add( this.makeGene( rat, "FOO2", "85494" ) );
        genesRat.add( this.makeGene( rat, "FOO3", "252899" ) );
        Gene2GeneProteinAssociation existingGene2GeneProteinAssociationOne = Gene2GeneProteinAssociation.Factory
                .newInstance( geneRatOne, geneRatThree, null, null, null );

        Gene2GeneProteinAssociation existingGene2GeneProteinAssociationTwo = Gene2GeneProteinAssociation.Factory
                .newInstance( geneRatOne, geneRatTwo, null, null, null );

        //noinspection MismatchedQueryAndUpdateOfCollection
        Collection<Gene2GeneProteinAssociation> geneAssociationRat = new ArrayList<>();
        geneAssociationRat.add( existingGene2GeneProteinAssociationTwo );
        geneAssociationRat.add( existingGene2GeneProteinAssociationOne );

    }

    public void getTestPeristentGenesZebra() {

        Gene geneZebraOne = this.makeGene( zebraFish, "zgc.153184", "751652" );
        Gene geneZebraTwo = this.makeGene( zebraFish, "appl1", "571540" );
        Gene geneZebraThree = this.makeGene( zebraFish, "LOC568371", "568371" );
        Gene geneZebraFour = this.makeGene( zebraFish, "TESTGENE", "30425" );
        Gene geneZebraFive = this.makeGene( zebraFish, "TESTGENE2", "503936" );

        genesZebra.add( this.makeGene( zebraFish, "FOO1", "562059" ) );

        genesZebra.add( geneZebraOne );

        genesZebra.add( geneZebraTwo );
        genesZebra.add( geneZebraThree );
        genesZebra.add( geneZebraFour );
        genesZebra.add( geneZebraFive );

        Gene2GeneProteinAssociation existingGene2GeneProteinAssociationOne = Gene2GeneProteinAssociation.Factory
                .newInstance( geneZebraOne, geneZebraThree, null, null, null );

        Gene2GeneProteinAssociation existingGene2GeneProteinAssociationTwo = Gene2GeneProteinAssociation.Factory
                .newInstance( geneZebraOne, geneZebraTwo, null, null, null );

        //noinspection MismatchedQueryAndUpdateOfCollection
        Collection<Gene2GeneProteinAssociation> geneAssociationZebra = new ArrayList<>();
        geneAssociationZebra.add( existingGene2GeneProteinAssociationTwo );
        geneAssociationZebra.add( existingGene2GeneProteinAssociationOne );

    }

    @Before
    public void setUp() {

        stringBiomartGene2GeneProteinAssociationLoader = new StringProteinInteractionLoader();
        stringBiomartGene2GeneProteinAssociationLoader.setPersisterHelper( super.persisterHelper );

        stringBiomartGene2GeneProteinAssociationLoader.setGeneService( geneService );
        stringBiomartGene2GeneProteinAssociationLoader.setExternalDatabaseService( super.externalDatabaseService );

        taxa = this.getTaxonToProcess();
        this.getTestPeristentGenesZebra();
        this.getTestPeristentGenesRat();

        // make sure all the data is cleared out before starting
        this.gene2GeneProteinAssociationService.removeAll( gene2GeneProteinAssociationService.loadAll() );
        assertTrue( gene2GeneProteinAssociationService.loadAll().isEmpty() );

    }

    /*
     * tests that given a local biomart and local string file data is processed
     */
    @Test
    public void testDoLoadLocalBiomartLocalStringOneTaxon() throws Exception {

        String fileNameStringZebraFish = "/data/loader/protein/string/protein.links.zebrafish.txt";
        URL fileNameStringZebraFishURL = this.getClass().getResource( fileNameStringZebraFish );

        String fileNameStringZebra = "/data/loader/protein/biomart/biomartzebrafish.txt";
        URL fileNameBiomartZebraURL = this.getClass().getResource( fileNameStringZebra );

        Collection<Taxon> taxaZebraFish = new ArrayList<>();
        taxaZebraFish.add( zebraFish );

        stringBiomartGene2GeneProteinAssociationLoader.load( new File( fileNameStringZebraFishURL.getFile() ), null,
                new File( fileNameBiomartZebraURL.getFile() ), taxaZebraFish );

        Collection<Gene2GeneProteinAssociation> associations = gene2GeneProteinAssociationService.loadAll();
        assertEquals( 2, associations.size() );

        for ( Gene gene : genesZebra ) {
            Collection<Gene2GeneProteinAssociation> interactionsForGene = this.gene2GeneProteinAssociationService
                    .findProteinInteractionsForGene( gene );

            if ( gene.getName().equals( "zgc:153184" ) ) {
                assertEquals( 2, interactionsForGene.size() );
            }
            if ( gene.getName().equals( "appl1" ) ) {
                assertEquals( 1, interactionsForGene.size() );
            }
            if ( gene.getName().equals( "LOC568371" ) ) {
                assertEquals( 1, interactionsForGene.size() );
            }
        }

        this.gene2GeneProteinAssociationService.removeAll( associations );
        associations = gene2GeneProteinAssociationService.loadAll();
        assertTrue( associations.isEmpty() );

    }

    /*
     * Tests that two taxons can be processed at same time.
     */
    @Test
    public void testDoLoadRemoteBiomartFileLocalStringFileMultipleTaxon() {
        String testPPis = "/data/loader/protein/string/protein.links.multitaxon.txt";
        URL testPPisURL = this.getClass().getResource( testPPis );

        String biomartTestfile = "/data/loader/protein/biomart/biomart.drerio.and.rat.test.txt";
        URL biomartTestfileURL = this.getClass().getResource( biomartTestfile );

        int counterAssociationsSavedZebra = 0;
        int counterAssociationsSavedRat = 0;

        try {
            stringBiomartGene2GeneProteinAssociationLoader
                    .load( new File( testPPisURL.getFile() ), null, new File( biomartTestfileURL.getFile() ),
                            this.getTaxonToProcess() );
        } catch ( ConnectException e ) {
            log.warn( "Connection error, skipping test" );
        } catch ( IOException e ) {
            if ( e.getMessage().startsWith( "Error from BioMart" ) ) {
                log.warn( e.getMessage() );
                return;
            }
        }

        Collection<Gene2GeneProteinAssociation> associations = gene2GeneProteinAssociationService.loadAll();

        assertEquals( 6, associations.size() );

        for ( Gene2GeneProteinAssociation association : associations ) {
            gene2GeneProteinAssociationService.thaw( association );
            Gene secondGene = association.getSecondGene();
            secondGene = this.geneService.thaw( secondGene );
            String taxonScientificName = secondGene.getTaxon().getScientificName();
            if ( taxonScientificName.equals( zebraFish.getScientificName() ) ) {
                counterAssociationsSavedZebra++;
            } else if ( taxonScientificName.equals( rat.getScientificName() ) ) {
                counterAssociationsSavedRat++;
            } else {
                fail();
            }

        }
        assertEquals( "Wrong number of rat PPIs", 3, counterAssociationsSavedRat );
        assertEquals( "Wrong number of fish PPIs", 3, counterAssociationsSavedZebra );

    }

    /*
     * Test given a local string file and remote biomart file for one taxon can be processed
     */
    @Test
    public void testDoLoadRemoteBiomartFileLocalStringFileOneTaxon() {

        // test pass in files so do not trigger fetcher
        // but make sure they get
        String fileNameStringZebraFish = "/data/loader/protein/string/protein.links.zebrafish.txt";
        URL fileNameStringZebraFishURL = this.getClass().getResource( fileNameStringZebraFish );
        Collection<Taxon> taxaZebraFish = new ArrayList<>();
        taxaZebraFish.add( zebraFish );

        Collection<Gene2GeneProteinAssociation> associationsBefore = gene2GeneProteinAssociationService.loadAll();
        assertEquals( 0, associationsBefore.size() );
        try {
            stringBiomartGene2GeneProteinAssociationLoader
                    .load( new File( fileNameStringZebraFishURL.getFile() ), null, null, taxaZebraFish );
        } catch ( ConnectException e ) {
            log.warn( "Connection error, skipping test" );
            return;
        } catch ( IOException e ) {
            if ( e.getMessage().startsWith( "Error from BioMart" ) ) {
                log.warn( e.getMessage() );
                return;
            }
        }
        Collection<Gene2GeneProteinAssociation> associations = gene2GeneProteinAssociationService.loadAll();
        assertEquals( 2, associations.size() );

        this.gene2GeneProteinAssociationService.removeAll( associations );
        associations = gene2GeneProteinAssociationService.loadAll();
        assertTrue( associations.isEmpty() );

    }

    private Gene makeGene( Taxon t, String name, String ncbiId ) {
        Gene g = Gene.Factory.newInstance();
        g.setName( name );
        g.setOfficialName( name );
        g.setOfficialSymbol( name );
        g.setNcbiGeneId( Integer.parseInt( ncbiId ) );
        g.setTaxon( t );
        Collection<GeneProduct> ggg = new HashSet<>();
        ggg.add( PersistentDummyObjectHelper.getTestNonPersistentGeneProduct( g ) );
        g.getProducts().addAll( ggg );
        g = ( Gene ) persisterHelper.persist( g );
        return g;
    }

}
