package ubic.gemma.core.security.authorization.acl;

import gemma.gsec.acl.domain.AclObjectIdentity;
import gemma.gsec.acl.domain.AclService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.stereotype.Component;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import static org.junit.Assert.*;

/**
 * Methods for checking ACLs.
 *
 * @author paul
 *
 */
@Component
public class AclTestUtils {

    private static Log log = LogFactory.getLog( AclTestUtils.class );

    @Autowired
    private AclService aclService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    /**
     * Make sure object f has no ACLs
     *
     * @param f f
     */
    public void checkDeletedAcl( Object f ) {
        try {
            Acl acl = getAcl( f );
            fail( "Failed to  remove ACL for " + f + ", got " + acl );
        } catch ( NotFoundException okaye ) {
            // okay
            if ( log.isDebugEnabled() )
                log.debug( "As expected, there was no acl for " + f.getClass().getSimpleName() );
        }
    }

    /**
     * CHeck the entire entity graph of an ee for ACL deletion.
     *
     * @param ee ee
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

            BioMaterial bm = ba.getSampleUsed();
            checkDeletedAcl( bm );
        }

    }

    /**
     * Validate ACLs on EE
     *
     * @param ee ee
     */
    public void checkEEAcls( ExpressionExperiment ee ) {
        ee = expressionExperimentService.thawLite( ee );
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

            BioMaterial bm = ba.getSampleUsed();
            checkHasAcl( bm );
            checkHasAclParent( bm, ee );
            checkLacksAces( bm );

            ArrayDesign arrayDesign = ba.getArrayDesignUsed();
            checkHasAcl( arrayDesign );
            assertTrue( getParentAcl( arrayDesign ) == null );

            // make sure the localfiles are associated with the array design, not the ee.
            arrayDesign = arrayDesignService.thawLite( arrayDesign );
            for ( LocalFile lf : arrayDesign.getLocalFiles() ) {
                checkHasAcl( lf );
                checkLacksAces( lf );
                checkHasAclParent( lf, arrayDesign );
            }

        }
    }

    public void checkEESubSetAcls( ExpressionExperimentSubSet eeset ) {
        checkEEAcls( eeset.getSourceExperiment() );
        checkHasAcl( eeset );
        checkLacksAces( eeset );
        checkHasAclParent( eeset, eeset.getSourceExperiment() );
    }

    public void checkHasAces( Object f ) {
        Acl a = getAcl( f );
        assertTrue( "For object " + f + " with ACL " + a + ":doesn't have ACEs, it should", a.getEntries().size() > 0 );
    }

    public void checkHasAcl( Object f ) {
        try {
            aclService.readAclById( new AclObjectIdentity( f ) );
            log.debug( "Have acl for " + f );
        } catch ( NotFoundException okaye ) {
            fail( "Failed to create ACL for " + f );
        }
    }

    public void checkHasAclParent( Object f, Object parent ) {
        Acl parentAcl = getParentAcl( f );
        assertNotNull( "No ACL for parent of " + f + "; the parent is " + parent, parentAcl );

        if ( parent != null ) {
            Acl b = getAcl( parent );
            assertEquals( b, parentAcl );
        }

        assertNotNull( parentAcl );

        log.debug( "ACL has correct parent for " + f + " <----- " + parentAcl.getObjectIdentity() );
    }

    public void checkLacksAces( Object f ) {
        Acl a = getAcl( f );
        assertTrue( f + " has ACEs, it shouldn't: " + a, a.getEntries().size() == 0 );
    }

    public void checkLacksAcl( Object f ) {

        try {

            aclService.readAclById( new AclObjectIdentity( f ) );
            fail( "Should not have found an ACL" );

        } catch ( NotFoundException okaye ) {
            // good
        }
    }

    public void update( MutableAcl acl ) {
        this.aclService.updateAcl( acl );
    }

    public MutableAcl getAcl( Object f ) {
        Acl a = aclService.readAclById( new AclObjectIdentity( f ) );
        return ( MutableAcl ) a;
    }

    private Acl getParentAcl( Object f ) {
        Acl a = getAcl( f );
        Acl parentAcl = a.getParentAcl();
        return parentAcl;
    }

}
