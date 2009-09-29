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
import ubic.gemma.ontology.MgedOntologyService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Test for import that results in multiple factor values for the same factor on a single biomaterial.
 * 
 * @author paul
 * @version $Id$
 */
public class ExperimentalDesignImportDuplicateValueTest extends BaseSpringContextTest {
    private static Log log = LogFactory.getLog( ExperimentalDesignImportDuplicateValueTest.class.getName() );
    MgedOntologyService mos;
    ExpressionExperiment ee;
    ExpressionExperimentService eeService;

    public final void testParse() throws Exception {

        ExperimentalDesignImporter parser = ( ExperimentalDesignImporter ) this.getBean( "experimentalDesignImporter" );
        // parser.setMgedOntologyService( mos );

        InputStream is = this.getClass().getResourceAsStream( "/data/loader/expression/expdesign.import.testfull.txt" );

        parser.importDesign( ee, is, false );

        Collection<BioMaterial> bms = new HashSet<BioMaterial>();
        for ( BioAssay ba : ee.getBioAssays() ) {
            for ( BioMaterial bm : ba.getSamplesUsed() ) {
                bms.add( bm );
            }
        }

        checkResults( bms );
    }

    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        endTransaction();

        SimpleExpressionDataLoaderService s = ( SimpleExpressionDataLoaderService ) this
                .getBean( "simpleExpressionDataLoaderService" );

        eeService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" ); 

        InputStream data = this.getClass().getResourceAsStream(
                "/data/loader/expression/expdesign.import.testfull.data.txt" );

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
        ad.setShortName( "gfoobly" );
        ad.setName( "foobly doo loo" );

        metaData.getArrayDesigns().add( ad );

        ee = s.load( metaData, data );
        eeService.thawLite( ee );
    }

    /**
     * @param bms
     */
    private void checkResults( Collection<BioMaterial> bms ) {

        assertEquals( 17, ee.getExperimentalDesign().getExperimentalFactors().size() );

        for ( BioMaterial bm : bms ) {
            Collection<ExperimentalFactor> seenExperimentalFactors = new HashSet<ExperimentalFactor>();
            for ( FactorValue fv : bm.getFactorValues() ) {

                if ( seenExperimentalFactors.contains( fv.getExperimentalFactor() ) ) {
                    for ( FactorValue ff : bm.getFactorValues() ) {
                        assertNotNull( ff.getId() );
                        if ( ff.getExperimentalFactor().equals( fv.getExperimentalFactor() ) ) {
                            log.info( bm + " : " + ff );
                        }
                    }

                    fail( fv.getExperimentalFactor() + " has more than one value for " + bm );
                }
                seenExperimentalFactors.add( fv.getExperimentalFactor() );
            }

        }
    }

}
