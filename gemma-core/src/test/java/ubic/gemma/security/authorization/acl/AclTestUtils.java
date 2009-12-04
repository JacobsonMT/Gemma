package ubic.gemma.security.authorization.acl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.stereotype.Service;

import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * Methods for checking ACLs
 * 
 * @author paul
 * @version $Id$
 */
@Service
public class AclTestUtils {

    private static Log log = LogFactory.getLog( AclTestUtils.class );

    @Autowired
    private AclService aclService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    /**
     * Make sure object f has no ACLs
     * 
     * @param f
     */
    public void checkDeletedAcl( Object f ) {
        try {
            Acl acl = aclService.readAclById( new ObjectIdentityImpl( f ) );
            fail( "Failed to  delete ACL for " + f + ", got " + acl );
        } catch ( NotFoundException okaye ) {
            // okay
            log.debug( "Deleted acl for " + f );
        }
    }

    /**
     * CHeck the entire entity graph of an ee for ACL deletion.
     * 
     * @param ee
     */
    public void checkDeleteEEAcls( ExpressionExperiment ee ) {
        checkDeletedAcl( ee );

        checkDeletedAcl( ee.getRawDataFile() );

        checkDeletedAcl( ee.getExperimentalDesign() );

        for ( ExperimentalFactor f : ee.getExperimentalDesign().getExperimentalFactors() ) {
            checkDeletedAcl( f );

            for ( FactorValue fv : f.getFactorValues() ) {
                checkDeletedAcl( fv );
            }
        }

        assertTrue( ee.getBioAssays().size() > 0 );
        for ( BioAssay ba : ee.getBioAssays() ) {
            checkDeletedAcl( ba );

            LocalFile rawDataFile = ba.getRawDataFile();

            for ( LocalFile f : ba.getDerivedDataFiles() ) {
                checkDeletedAcl( f );
            }

            if ( rawDataFile != null ) {
                checkDeletedAcl( rawDataFile );
            }

            for ( BioMaterial bm : ba.getSamplesUsed() ) {
                checkDeletedAcl( bm );
            }

        }
    }

    /**
     * Validate ACLs on EE
     * 
     * @param ee
     */
    public void checkEEAcls( ExpressionExperiment ee ) {
        this.expressionExperimentService.thaw( ee );
        checkHasAcl( ee );
        checkHasAces( ee );

        ExperimentalDesign experimentalDesign = ee.getExperimentalDesign();
        checkHasAcl( experimentalDesign );
        checkHasAclParent( experimentalDesign, ee );
        checkLacksAces( experimentalDesign );

        if ( ee.getRawDataFile() != null ) {
            checkHasAcl( ee.getRawDataFile() );
            checkHasAclParent( ee.getRawDataFile(), ee );
            checkLacksAces( ee.getRawDataFile() );
        }

        for ( ExperimentalFactor f : experimentalDesign.getExperimentalFactors() ) {
            checkHasAcl( f );
            checkHasAclParent( f, ee );
            checkLacksAces( f );

            for ( FactorValue fv : f.getFactorValues() ) {
                checkHasAcl( fv );
                checkHasAclParent( fv, ee );
                checkLacksAces( fv );
            }
        }

        // make sure ACLs for the child objects are there
        assertTrue( ee.getBioAssays().size() > 0 );
        for ( BioAssay ba : ee.getBioAssays() ) {
            checkHasAcl( ba );
            checkHasAclParent( ba, ee );
            checkLacksAces( ba );

            LocalFile rawDataFile = ba.getRawDataFile();

            if ( rawDataFile != null ) {
                checkHasAcl( rawDataFile );
                checkHasAclParent( rawDataFile, null );
                checkLacksAces( rawDataFile );
            }

            for ( LocalFile f : ba.getDerivedDataFiles() ) {
                checkHasAcl( f );
                checkHasAclParent( f, null );
                checkLacksAces( f );
            }

            for ( BioMaterial bm : ba.getSamplesUsed() ) {
                checkHasAcl( bm );
                checkHasAclParent( bm, ee );
                checkLacksAces( bm );
            }
        }
    }

    public void checkHasAcl( Object f ) {
        try {
            aclService.readAclById( new ObjectIdentityImpl( f ) );
            log.debug( "Have acl for " + f );
        } catch ( NotFoundException okaye ) {
            fail( "Failed to create ACL for " + f );
        }
    }

    public void checkHasAces( Object f ) {
        Acl a = aclService.readAclById( new ObjectIdentityImpl( f ) );
        assertTrue( a + " doesn't have ACEs, it should", a.getEntries().size() > 0 );
    }

    public void checkLacksAces( Object f ) {
        Acl a = aclService.readAclById( new ObjectIdentityImpl( f ) );
        assertTrue( f + " has ACEs, it shouldn't", a.getEntries().size() == 0 );
    }

    public void checkHasAclParent( Object f, Object parent ) {
        Acl a = aclService.readAclById( new ObjectIdentityImpl( f ) );
        assertNotNull( a.getParentAcl() );

        if ( parent != null ) {
            Acl b = aclService.readAclById( new ObjectIdentityImpl( parent ) );
            assertEquals( b, a.getParentAcl() );
        }

        assertNotNull( a.getParentAcl() );

        log.debug( "ACL has correct parent for " + f + " <----- " + a.getParentAcl().getObjectIdentity() );
    }

}
