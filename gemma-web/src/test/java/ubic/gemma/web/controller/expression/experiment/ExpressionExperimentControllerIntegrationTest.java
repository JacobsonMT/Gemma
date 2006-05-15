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
package ubic.gemma.web.controller.expression.experiment;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.ContactService;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialDao;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.testing.BaseTransactionalSpringContextTest;

/**
 * Tests the ExpressionExperimentController.
 * 
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentControllerIntegrationTest extends BaseTransactionalSpringContextTest {
    private static Log log = LogFactory.getLog( ExpressionExperimentControllerIntegrationTest.class.getName() );

    /**
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Override
    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

    }

    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection getCompositeSequences() {
        Collection<CompositeSequence> csCol = new HashSet();
        for ( int i = 0; i < testNumCollectionElements; i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance();
            cs.setName( "Composite Sequence " + i );
            csCol.add( cs );
        }
        return csCol;
    }

    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection getArrayDesignsUsed() {
        Collection<ArrayDesign> adCol = new HashSet();
        ArrayDesignService adService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
        for ( int i = 0; i < testNumCollectionElements; i++ ) {
            ArrayDesign ad = ArrayDesign.Factory.newInstance();
            ad.setName( "Array Design " + i );
            ad.setDescription( i + ": A test array design." );
            ad.setAdvertisedNumberOfDesignElements( i + 100 );
            ad.setCompositeSequences( getCompositeSequences() );
            
            ad = adService.findOrCreate(ad);
            
            adCol.add( ad );
        }
        return adCol;
    }

    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection getBioMaterials() {
        BioMaterial bm = BioMaterial.Factory.newInstance();

        Taxon t = Taxon.Factory.newInstance();
        t.setScientificName( "Mus musculus" );
        TaxonService ts = ( TaxonService ) getBean( "taxonService" );
        t = ts.findOrCreate( t );
        bm.setSourceTaxon( t );

        ExternalDatabaseService eds = ( ExternalDatabaseService ) getBean( "externalDatabaseService" );
        ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
        ed.setName( "PubMed" );
        ed = eds.findOrCreate( ed );

        DatabaseEntry de = DatabaseEntry.Factory.newInstance();
        de.setAccession( " Biomaterial accession " );
        de.setExternalDatabase( ed );
        bm.setExternalAccession( de );

        bm.setName( " BioMaterial " );
        bm.setDescription( " A test biomaterial" );

        /*
         * FIXME - change to use the service, not the dao. will not do until merging Gemma V01_MVN1 because I am trying
         * to reduce the number of model changes.
         */
        BioMaterialDao bmDao = ( BioMaterialDao ) this.getBean( "bioMaterialDao" );
        // bmDao.findOrCreate( bm ); - FIXME this is what I want to use, but there is a problem with this:
        // TransientObjectException. The error is consistent. If you have A => B -> C where => is composition and -> is
        // association
        bm = ( BioMaterial ) bmDao.create( bm );
        Collection<BioMaterial> bmCol = new HashSet();
        bmCol.add( bm );
        return bmCol;
    }

    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection getBioAssays() {
        Collection<BioAssay> baCol = new HashSet();
        for ( int i = 0; i < testNumCollectionElements; i++ ) {
            BioAssay ba = BioAssay.Factory.newInstance();
            ba.setName( "Bioassay " + i );
            ba.setDescription( i + ": A test bioassay." );
            ba.setSamplesUsed( getBioMaterials() );
            ba.setArrayDesignsUsed( getArrayDesignsUsed() );

            if ( i < ( testNumCollectionElements - 5 ) ) {
                ExternalDatabaseService eds = ( ExternalDatabaseService ) getBean( "externalDatabaseService" );
                ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
                ed.setName( "PubMed" );
                ed = eds.findOrCreate( ed );

                DatabaseEntry de = DatabaseEntry.Factory.newInstance();
                de.setExternalDatabase( ed );
                de.setAccession( i + ": Accession added from ExpressionExperimentControllerIntegrationTest" );
                ba.setAccession( de );
            }

            baCol.add( ba );
        }

        return baCol;
    }

    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection getFactorValues() {
        Collection<FactorValue> fvCol = new HashSet();
        for ( int i = 0; i < testNumCollectionElements; i++ ) {
            FactorValue fv = FactorValue.Factory.newInstance();
            fv.setValue( "Factor value " + i );
            fvCol.add( fv );
        }
        return fvCol;
    }

    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection getExperimentalFactors() {
        Collection<ExperimentalFactor> efCol = new HashSet();
        for ( int i = 0; i < testNumCollectionElements; i++ ) {
            ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
            ef.setName( "Experimental Factor " + i );
            ef.setDescription( i + ": A test experimental factor" );
            // FIXME - another
            // OntologyEntry oe = OntologyEntry.Factory.newInstance();
            // oe.setAccession( "oe:" + i );
            // oe.setDescription( "Ontology Entry " + i );
            // log.debug( "ontology entry => experimental factor." );
            // ef.setCategory( oe );
            // ef.setAnnotations(oeCol);
            log.debug( "experimental factor => factor values" );
            ef.setFactorValues( getFactorValues() );
            efCol.add( ef );
        }
        return efCol;
    }

    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection getExperimentalDesigns() {
        Collection<ExperimentalDesign> edCol = new HashSet();
        for ( int i = 0; i < testNumCollectionElements; i++ ) {
            ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
            ed.setName( "Experimental Design " + i );
            ed.setDescription( i + ": A test experimental design." );

            log.debug( "experimental design => experimental factors" );
            ed.setExperimentalFactors( getExperimentalFactors() ); // set test experimental factors

            edCol.add( ed ); // add experimental designs
        }
        return edCol;
    }

    /**
     * Add an expressionExperiment to the database for testing purposes. Includes associations.
     */
    @SuppressWarnings("unchecked")
    public void setExpressionExperimentDependencies() {
        /*
         * Create & Persist the expression experiment and dependencies
         */
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setName( "Expression Experiment" );

        ee.setDescription( "A test expression experiment" );

        ee.setSource( "http://www.ncbi.nlm.nih.gov/geo/" );

        ExternalDatabaseService eds = ( ExternalDatabaseService ) getBean( "externalDatabaseService" );
        ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
        ed.setName( "PubMed" );
        ed = eds.findOrCreate( ed );

        DatabaseEntry de1 = DatabaseEntry.Factory.newInstance();
        de1.setUri( "http://www.ncbi.nlm.nih.gov/geo/" );
        de1.setAccession( "GDS1234" );

        log.debug( "database entry -> external database" );
        de1.setExternalDatabase( ed );

        log.debug( "expression experiment => database entry" );
        ee.setAccession( de1 );

        log.debug( "expression experiment => bioassays" );
        ee.setBioAssays( getBioAssays() );

        log.debug( "expression experiment => experimental designs" );
        ee.setExperimentalDesigns( getExperimentalDesigns() );

        log.debug( "expression experiment -> owner " );
        Contact c = Contact.Factory.newInstance();
        c.setName( "Foo Bar" );
        c.setAddress( "414 West 120th Street Apt. 402, New York, NY, 10027" );
        c.setPhone( "917 363 6904" );
        c.setFax( "212 851 4664" );
        c.setEmail( "keshav@cu-genome.org" );
        ContactService cs = ( ContactService ) getBean( "contactService" );
        c = cs.findOrCreate( c );
        ee.setOwner( c );

        ExpressionExperimentService ees = ( ExpressionExperimentService ) getBean( "expressionExperimentService" );
        log.debug( "Loading test expression experiment." );
        ee = ees.create( ee ); // FIXME - again, I would like to use findOrCreate
    }

    /**
     * Tests getting all the expressionExperiments, which is implemented in
     * {@link ubic.gemma.web.controller.expression.experiment.ExpressionExperimentController} in method
     * {@link #handleRequest(HttpServletRequest request, HttpServletResponse response)}.
     * 
     * @throws Exception
     */
    public void testGetExpressionExperiments() throws Exception {
        log.debug( "-> (association), => (composition)" );
        /* set to avoid using stale data (data from previous tests */
        setFlushModeCommit();

        /* uncomment to use prod environment instead of test environment */
        // this.setDisableTestEnv( true );
        onSetUpInTransaction();

        setExpressionExperimentDependencies();

        ExpressionExperimentController c = ( ExpressionExperimentController ) getBean( "expressionExperimentController" );

        MockHttpServletRequest req = new MockHttpServletRequest( "GET",
                "/expressionExperiment/showAllExpressionExperiments.html" );
        req.setRequestURI( "/expressionExperiment/showAllExpressionExperiments.html" );

        ModelAndView mav = c.handleRequest( req, ( HttpServletResponse ) null );

        ExpressionExperimentDao eeDao = ( ExpressionExperimentDao ) getBean( "expressionExperimentDao" );
        ExpressionExperiment expressionExperiment = eeDao.findByName( "Expression Experiment" );
        log.debug( "Contact: " + expressionExperiment.getOwner() );

        Map m = mav.getModel();

        assertNotNull( m.get( "expressionExperiments" ) );
        assertEquals( mav.getViewName(), "expressionExperiments" );

        /* uncomment to persist and leave data in database */
        //setComplete();
    }

    /**
     * @throws Exception
     */
    // @SuppressWarnings("unchecked")
    // public void testGetExperimentalDesigns() throws Exception {
    //
    // ExpressionExperimentController c = ( ExpressionExperimentController ) ctx
    // .getBean( "expressionExperimentController" );
    //
    // MockHttpServletRequest req = new MockHttpServletRequest( "GET", "Gemma/experimentalDesigns.htm" );
    // req.setRequestURI( "/Gemma/experimentalDesigns.htm" );
    // // cannot set parameter (setParmeter does not exist) so I had to set the attribute. On the server side,
    // // I have used a getAttribute as opposed to a getParameter - difference?
    // req.setAttribute( "name", "Expression Experiment" );
    //
    // ModelAndView mav = c.handleRequest( req, ( HttpServletResponse ) null );
    //
    // /*
    // * In this case, the map contains 1 element of type Collection. That is, a collection of experimental designs.
    // */
    // Map<String, Object> m = mav.getModel();
    //
    // Collection<ExperimentalDesign> col = ( Collection<ExperimentalDesign> ) m.get( "experimentalDesigns" );
    // log.debug( new Integer( col.size() ) );
    //
    // assertNotNull( m.get( "experimentalDesigns" ) );
    // assertEquals( mav.getViewName(), "experimentalDesign.GetAll.results.view" );
    // }
}
