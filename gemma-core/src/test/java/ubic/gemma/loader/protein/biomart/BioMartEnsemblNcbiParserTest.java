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
package ubic.gemma.loader.protein.biomart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import ubic.gemma.loader.protein.biomart.model.BioMartEnsembleNcbi;
import ubic.gemma.model.genome.Taxon;

/**
 * Tests the parsing of a BioMart file. Tests one line can be parsed and whole files. Some error conditions are
 * tested for too.
 * 
 * @author ldonnison
 * @version $Id$
 */
public class BioMartEnsemblNcbiParserTest {

    private BiomartEnsembleNcbiParser parser = null;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {

        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setNcbiId( 10 );
        // standard attributes for all taxon
        String[] attributesToGet = new String[] { "ensembl_gene_id", "ensembl_transcript_id", "entrezgene",
                "ensembl_peptide_id", "" };
        parser = new BiomartEnsembleNcbiParser( taxon, attributesToGet );
    }

    /**
     * Test method for {@link ubic.gemma.loader.protein.string.BiomartEnsembleNcbiParser#parseOneLine(java.lang.String)}
     * . Tests that a standard taxon line can be parsed
     */
    @Test
    public void testParseOneValidLineNonHuman() {
        String line = "ENSG00000220023" + "\t" + "ENST00000418749" + "\t" + "100134091" + "\t" + "ENST00000418749";

        BioMartEnsembleNcbi bioMartEnsembleNcbi = parser.parseOneLine( line );

        assertTrue( bioMartEnsembleNcbi.getSpecies_ncbi_taxon_id().equals( 10 ) );
        assertEquals( "ENSG00000220023", bioMartEnsembleNcbi.getEnsembl_gene_id() );
        assertEquals( "ENST00000418749", bioMartEnsembleNcbi.getEnsembl_transcript_id() );
        Collection<String> genes = bioMartEnsembleNcbi.getEntrezgenes();
        assertTrue( genes.contains( "100134091" ) );
        assertEquals( "ENST00000418749", bioMartEnsembleNcbi.getEnsembl_peptide_id() );

    }

    /**
     * Test method for {@link ubic.gemma.loader.protein.string.BiomartEnsembleNcbiParser#parseOneLine(java.lang.String)}
     * . Tests that a standard taxon line with an extra field throws an error
     */
    @Test
    public void testParseOneInValidLineNonHuman() {

        try {
            String line = "ENSG00000220023" + "\t" + "ENST00000418749" + "\t" + "100134091" + "\t" + "ENST00000418749"
                    + "\tasasas";
            parser.parseOneLine( line );
            fail();
        } catch ( Exception e ) {
            assertTrue( e.getMessage().contains( "Line" ) );
        }

    }

    /**
     * Tests that the number of attributes are counted corrrectly
     */
    @Test
    public void testGetBioMartFieldsPerRow() {

        String[] attributesToGet = new String[] { "ensembl_gene_id", "ensembl_transcript_id", "entrezgene",
                "ensembl_peptide_id", "" };
        parser.setBioMartFields( attributesToGet );
        assertTrue( parser.getBioMartFieldsPerRow() == 4 );

    }

    /**
     * Test method for {@link ubic.gemma.loader.protein.string.BiomartEnsembleNcbiParser#parseOneLine(java.lang.String)}
     * . Tests that a standard human taxon line can be parsed
     */
    @Test
    public void testParseOneValidLineHuman() {
        String[] attributesToGet = new String[] { "ensembl_gene_id", "ensembl_transcript_id", "entrezgene",
                "ensembl_peptide_id", "hgnc_id" };
        parser.setBioMartFields( attributesToGet );
        String line = "ENSG00000220023" + "\t" + "ENST00000418749" + "\t" + "10013421" + "\t" + "ENST00000418749"
                + "\t" + "12123";

        BioMartEnsembleNcbi bioMartEnsembleNcbi = parser.parseOneLine( line );

        assertTrue( bioMartEnsembleNcbi.getSpecies_ncbi_taxon_id().equals( 10 ) );
        assertEquals( "ENSG00000220023", bioMartEnsembleNcbi.getEnsembl_gene_id() );
        assertEquals( "ENST00000418749", bioMartEnsembleNcbi.getEnsembl_transcript_id() );
        Collection<String> genes = bioMartEnsembleNcbi.getEntrezgenes();
        assertTrue( genes.contains( "10013421" ) );
        assertEquals( "ENST00000418749", bioMartEnsembleNcbi.getEnsembl_peptide_id() );
        assertEquals( "12123", bioMartEnsembleNcbi.getHgnc_id() );

    }

    /**
     * Tests that a biomart mouse file can be parsed
     */
    @Test
    public void testParseValidFileMouse() {

        String[] attributesToGet = new String[] { "ensembl_gene_id", "ensembl_transcript_id", "entrezgene",
                "ensembl_peptide_id", "" };
        String fileNameStringmouse = "/data/loader/protein/biomart/biomartmmusculus.txt";
        URL myurl = this.getClass().getResource( fileNameStringmouse );
        try {
            parser.setBioMartFields( attributesToGet );
            parser.parse( new File( myurl.getFile() ) );
            Collection<BioMartEnsembleNcbi> items = parser.getResults();
            boolean isItemThereOne = false;
            boolean isItemThereTwo = false;
            // 27 unique peptide ids but only 20 which have entrez genes other get filtered out
            assertEquals( 20, items.size() );

            for ( BioMartEnsembleNcbi item : items ) {
                if ( item.getEnsembl_gene_id().equals( "ENSMUSG00000064341" ) ) {
                    assertEquals(2,  ( item.getEntrezgenes().size() )  );
                    isItemThereOne = true;
                }
                if ( item.getEnsembl_gene_id().equals( "ENSMUSG00000057782" ) ) {
                    assertEquals(  item.getEntrezgenes().size(),  1 );
                    isItemThereTwo = true;
                }

            }
            assertTrue( isItemThereTwo );
            assertTrue( isItemThereOne );

        } catch ( Exception e ) {
            e.printStackTrace();
            fail();

        }
    }

    /**
     * Test method for {@link ubic.gemma.loader.protein.string.BiomartEnsembleNcbiParser#parseOneLine(java.lang.String)}
     * . Tests that a standard human taxon line can be parsed
     */
    @Test
    public void testParseValidFileHuman() {

        String[] attributesToGet = new String[] { "ensembl_gene_id", "ensembl_transcript_id", "entrezgene",
                "ensembl_peptide_id", "hgnc_id" };
        String fileName = "/data/loader/protein/biomart/biomartsapiens.txt";
        URL myurl = this.getClass().getResource( fileName );

        try {
            parser.setBioMartFields( attributesToGet );
            parser.parse( new File( myurl.getFile() ) );
            Collection<BioMartEnsembleNcbi> items = parser.getResults();
            // 39 unique proteins and 36 unique genes
            assertEquals( 10, items.size() );
            for ( BioMartEnsembleNcbi item : items ) {
                if ( item.getEnsembl_gene_id().equals( "ENSG00000215764" ) ) {
                    assertEquals( 1, item.getEntrezgenes().size() );
                    assertEquals( "6330", item.getHgnc_id() );
                }
            }

        }

        catch ( Exception e ) {
            e.printStackTrace();
            fail();
        }
    }

}
