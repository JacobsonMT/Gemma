/**
 * 
 */
package ubic.gemma.loader.genome.gene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.util.ConfigUtils;

/**
 * Test that Gemma can load genes from an external gene file with format : #GeneSymbol GeneName Uniprot ZYX ZYXIN Q15942
 * ZXDC ZXD FAMILY ZINC FINGER C Q8C8V1
 * 
 * @author ldonnison
 * @version $Id$
 */
public class ExternalFileGeneLoaderServiceTest extends BaseSpringContextTest {

    /*
     * This doesn't matter for test so long as it's in the system. Actual examples are fish genes
     */
    private static final String TAXON_NAME = "mouse";

    @Autowired
    ExternalFileGeneLoaderService externalFileGeneLoaderService = null;

    String geneFile = null;

    @Before
    public void setup() throws Exception {
        geneFile = ( ConfigUtils.getString( "gemma.home" ) )
                .concat( "/gemma-core/src/test/resources/data/loader/genome/gene/externalGeneFileLoadTest.txt" );
    }

    /**
     * Tests that if the file is not in the correct format of 3 tab delimited fields exception thrown.
     */
    @Test
    public void testFileIncorrectFormatIllegalArgumentExceptionException() {
        try {
            String ncbiFile = ( ConfigUtils.getString( "gemma.home" ) )
                    .concat( "/gemma-core/src/test/resources/data/loader/genome/gene/geneloadtest.txt" );
            externalFileGeneLoaderService.load( ncbiFile, TAXON_NAME );
        } catch ( IOException e ) {
            assertEquals( "Illegal format, expected three columns, got 13", e.getMessage() );
        } catch ( Exception e ) {
            fail();
        }
    }

    /**
     * Test method for
     * {@link ubic.gemma.loader.genome.gene.ExternalFileGeneLoaderService#load(java.lang.String, java.lang.String)}.
     * Tests that 2 genes are loaded sucessfully into Gemma.
     */
    @Test
    public void testLoad() throws Exception {
        GeneService geneService = ( GeneService ) getBean( "geneService" );
        externalFileGeneLoaderService.load( geneFile, TAXON_NAME );
        int numbersGeneLoaded = externalFileGeneLoaderService.getLoadedGeneCount();
        assertEquals( 2, numbersGeneLoaded );
        Collection<Gene> geneCollection = geneService.findByOfficialName( "ZYXIN" );
        Gene gene = geneCollection.iterator().next();

        geneService.thaw( gene );

        Collection<GeneProduct> geneProducts = gene.getProducts();

        assertEquals( TAXON_NAME, gene.getTaxon().getCommonName() );
        assertEquals( "ZYX", gene.getName() );
        assertEquals( "ZYX", gene.getOfficialSymbol() );
        assertEquals( "Imported from external gene file with uniprot id of Q15942", gene.getDescription() );

        assertEquals( 1, geneProducts.size() );
        GeneProduct prod = geneProducts.iterator().next();
        assertEquals( "Gene product placeholder", prod.getDescription() );

    }

    /**
     * Tests that if file can not be found file not found exception thrown.
     */
    @Test
    public void testLoadGeneFileNotFoundIOException() throws Exception {
        try {
            externalFileGeneLoaderService.load( "blank", TAXON_NAME );
        } catch ( IOException e ) {
            assertEquals( "Cannot read from blank", e.getMessage() );
        }
    }

    /**
     * Tests that if taxon not stored in system IllegalArgumentExceptionThrown
     */
    @Test
    public void testTaxonNotFoundIllegalArgumentExceptionException() {
        try {
            externalFileGeneLoaderService.load( geneFile, "fishy" );
        } catch ( IllegalArgumentException e ) {
            assertEquals( "No taxon with common name fishy found", e.getMessage() );
        } catch ( Exception e ) {
            fail();
        }
    }

}
