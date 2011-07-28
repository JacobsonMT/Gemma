package ubic.gemma.model.association.phenotype;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashSet;

import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.association.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.association.phenotype.valueObject.UrlEvidenceValueObject;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.testing.BaseSpringContextTest;

public class PhenotypeAssociationTest extends BaseSpringContextTest {

    @Autowired
    private UrlEvidenceDao urlDao;

    @Autowired
    private PhenotypeAssociationManagerService phenoAssoService;;

    private String geneNCBI = "";
    private UrlEvidenceValueObject evidence = null;
    private String phenotypeValue = null;
    private Collection<String> phenotypes = null;

    @Before
    public void setup() throws Exception {

        // 1- Gene we will use for test
        geneNCBI = "1";

        // 2 - Phenotype
        phenotypeValue = "GOTestPhenotype";
        phenotypes = new HashSet<String>();
        phenotypes.add( phenotypeValue );

        // 3- Evidence
        evidence = new UrlEvidenceValueObject( "test_name", "test_description", false,
                GOEvidenceCode.fromString( "IC" ), phenotypes, "www.test.com" );
    }

    @Test
    public void testPhenotypeAssociation() {

        // ********************************************************************************************
        // 1 - call the service to add the phenotype association and save the results to the database
        // ********************************************************************************************
        GeneValueObject geneValue = phenoAssoService.linkGeneToPhenotype( geneNCBI, evidence );
        assertTrue( geneValue.getEvidences() != null && geneValue.getEvidences().size() >= 1 );
        // ********************************************************************************************
        // 2 - call the service to find all gene for a given phenotype
        // ********************************************************************************************
        Collection<GeneValueObject> geneInfoValueObjects = phenoAssoService.findCandidateGenes( phenotypeValue );
        assertTrue( geneInfoValueObjects != null && geneInfoValueObjects.size() >= 1 );
        // ********************************************************************************************
        // 3 - call the service to find all evidences and phenotypes for a gene
        // ********************************************************************************************
        GeneValueObject geneInfoValueObject = phenoAssoService.findPhenotypeAssociations( geneNCBI );
        assertNotNull( geneInfoValueObject.getEvidences() );
        // ********************************************************************************************
        // 4 - Delete the Association
        // ********************************************************************************************
        for ( EvidenceValueObject evidenceValueObject : geneInfoValueObject.getEvidences() ) {
            phenoAssoService.removePhenotypeAssociation( evidenceValueObject.getDatabaseId() );
        }
        assertTrue( phenoAssoService.findCandidateGenes( phenotypeValue ).size() == 0 );
    }

    @Test
    public void testDaoUrlEvidence() {

        // create
        UrlEvidence urlEvidence = new UrlEvidenceImpl();
        urlEvidence.setDescription( "testDescription" );
        urlEvidence.setName( "testname" );
        urlEvidence.setUrl( "www.test.com" );
        UrlEvidence entityReturn = urlDao.create( urlEvidence );
        assertNotNull( entityReturn.getId() );

        // update
        urlEvidence.setUrl( "www.testupdate.com" );
        urlDao.update( urlEvidence );

        // load
        UrlEvidence urlEvidenceLoad = urlDao.load( entityReturn.getId() );
        assertNotNull( urlEvidenceLoad );
        assertTrue( urlEvidenceLoad.getUrl().equals( "www.testupdate.com" ) );

        // remove
        urlDao.remove( entityReturn.getId() );
        assertNull( urlDao.load( entityReturn.getId() ) );
    }

    // not a junit test, used to check values
    public void testFindPhenotypeAssociations() {

        // call to the service
        GeneValueObject geneInfoValueObject = phenoAssoService.findPhenotypeAssociations( "2" );

        System.out.println( "" );
        System.out.println( "Gene name: " + geneInfoValueObject.getName() );
        System.out.println( "" );
        System.out.println();
        System.out.println();

        for ( EvidenceValueObject evidence : geneInfoValueObject.getEvidences() ) {

            System.out.println( "Found evidence: " + evidence.getDatabaseId() + "   " + evidence.getName() + "   "
                    + evidence.getDescription() );
            System.out.println( "With phenotypes: " );

            for ( String value : evidence.getPhenotypes() ) {
                System.out.println( "Value :" + value );

            }
            System.out.println();
            System.out.println();
        }
    }

    // not a junit test, used to check values
    public void testFindCandidateGenes() {

        Collection<GeneValueObject> geneInfoValueObjects = phenoAssoService.findCandidateGenes( "CANCER" );

        for ( GeneValueObject geneInfoValueObject : geneInfoValueObjects ) {

            System.out.println( "" );
            System.out.println( "Gene name: " + geneInfoValueObject.getName() );
            System.out.println( "" );
            System.out.println();
            System.out.println();

            for ( EvidenceValueObject evidence : geneInfoValueObject.getEvidences() ) {

                System.out.println( "Found evidence: " + evidence.getDatabaseId() + "   " + evidence.getName() + "   "
                        + evidence.getDescription() );
                System.out.println( "With phenotypes: " );

                for ( String value : evidence.getPhenotypes() ) {
                    System.out.println( "Value :" + value );

                }
                System.out.println();
                System.out.println();
            }
        }
    }

}
