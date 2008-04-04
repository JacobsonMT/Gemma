/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.loader.expression.simple;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.ontology.MgedOntologyService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author Paul
 * @version $Id$
 */
public class ExperimentalDesignImporterTest extends BaseSpringContextTest {
    MgedOntologyService mos;
    ExpressionExperiment ee;
    ExpressionExperimentService eeService;
    private static Log log = LogFactory.getLog( ExperimentalDesignImporterTest.class.getName() );

    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        endTransaction();

        SimpleExpressionDataLoaderService s = ( SimpleExpressionDataLoaderService ) this
                .getBean( "simpleExpressionDataLoaderService" );

        eeService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );

        InputStream data = this.getClass().getResourceAsStream(
                "/data/loader/expression/experimentalDesignTestData.txt" );

        TaxonService taxonService = ( TaxonService ) this.getBean( "taxonService" );

        SimpleExpressionExperimentMetaData metaData = new SimpleExpressionExperimentMetaData();

        mos = ( MgedOntologyService ) this.getBean( "mgedOntologyService" );
        mos.init( true );
        while ( !mos.isOntologyLoaded() ) {
            Thread.sleep( 5000 );
            log.info( "Waiting for mgedontology to load" );
        }

        Taxon human = taxonService.findByCommonName( "human" );

        metaData.setShortName( RandomStringUtils.randomAlphabetic( 10 ) );
        metaData.setDescription( "bar" );
        metaData.setIsRatio( false );
        metaData.setTaxon( human );
        metaData.setQuantitationTypeName( "rma" );
        metaData.setScale( ScaleType.LOG2 );
        metaData.setType( StandardQuantitationType.AMOUNT );

        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setShortName( "foobly" );
        ad.setName( "foobly foo" );

        metaData.getArrayDesigns().add( ad );

        ee = s.load( metaData, data );
        eeService.thawLite( ee );
    }

    public final void testParseFailedDryRun() throws Exception {

        ExperimentalDesignImporter parser = ( ExperimentalDesignImporter ) this.getBean( "experimentalDesignImporter" );
        parser.setMgedOntologyService( mos );

        InputStream is = this.getClass().getResourceAsStream( "/data/loader/expression/experimentalDesignTestBad.txt" );

        try {
            parser.importDesign( ee, is, true );
            fail( "Should have gotten an Exception" );
        } catch ( Exception e ) {
            // ok
        }

    }

    public final void testParseDryRun() throws Exception {

        ExperimentalDesignImporter parser = ( ExperimentalDesignImporter ) this.getBean( "experimentalDesignImporter" );
        parser.setMgedOntologyService( mos );

        InputStream is = this.getClass().getResourceAsStream( "/data/loader/expression/experimentalDesignTest.txt" );

        parser.importDesign( ee, is, true );

        // / confirm we didn't save anything.
        assertEquals( 4, ee.getExperimentalDesign().getExperimentalFactors().size() );
        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
            assertNull( ef.getId() );
        }
    }

    /**
     * Test method for {@link ubic.gemma.loader.expression.simple.ExperimentalDesignImporter#parse(java.io.InputStream)}.
     */
    public final void testParse() throws Exception {

        ExperimentalDesignImporter parser = ( ExperimentalDesignImporter ) this.getBean( "experimentalDesignImporter" );
        parser.setMgedOntologyService( mos );

        InputStream is = this.getClass().getResourceAsStream( "/data/loader/expression/experimentalDesignTest.txt" );

        parser.importDesign( ee, is, false );

        Collection<BioMaterial> bms = new HashSet<BioMaterial>();
        for ( BioAssay ba : ee.getBioAssays() ) {
            for ( BioMaterial bm : ba.getSamplesUsed() ) {
                bms.add( bm );
            }
        }

        // check.
        assertEquals( 4, ee.getExperimentalDesign().getExperimentalFactors().size() );

        Collection<Long> seenFactorValueIds = new HashSet<Long>();
        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
            for ( FactorValue fv : ef.getFactorValues() ) {
                if ( fv.getCharacteristics().size() > 0 ) {
                    VocabCharacteristic c = ( VocabCharacteristic ) fv.getCharacteristics().iterator().next();
                    assertNotNull( c.getValue() );
                    assertNotNull( c.getCategoryUri() );
                } else {
                    assertNotNull( fv.getValue() + " should have a measurement or a characteristic", fv
                            .getMeasurement() );
                }
                seenFactorValueIds.add( fv.getId() );
            }
        }

        assertEquals( 23, seenFactorValueIds.size() );

        for ( BioMaterial bm : bms ) {
            assertEquals( 4, bm.getFactorValues().size() );
            for ( FactorValue fv : bm.getFactorValues() ) {
                assertTrue( seenFactorValueIds.contains( fv.getId() ) );
            }

        }
    }

}
