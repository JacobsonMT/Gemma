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
package ubic.gemma.loader.expression.simple;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.RandomStringUtils;

import ubic.gemma.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class SimpleExpressionDataLoaderServiceTest extends BaseSpringContextTest {

    /**
     * Test method for
     * {@link ubic.gemma.loader.expression.simple.SimpleExpressionDataLoaderService#load(ubic.gemma.loader.expression.simple.model.ExpressionExperimentMetaData, java.io.InputStream)}.
     */
    public final void testLoad() throws Exception {
        SimpleExpressionDataLoaderService service = ( SimpleExpressionDataLoaderService ) this
                .getBean( "simpleExpressionDataLoaderService" );

        SimpleExpressionExperimentMetaData metaData = new SimpleExpressionExperimentMetaData();
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( RandomStringUtils.randomAlphabetic( 5 ) );
        Collection<ArrayDesign> ads = new HashSet<ArrayDesign>();
        ads.add( ad );
        metaData.setArrayDesigns( ads );

        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( "mouse" );
        metaData.setTaxon( taxon );
        metaData.setName( RandomStringUtils.randomAlphabetic( 5 ) );
        metaData.setQuantitationTypeName( "testing" );
        metaData.setGeneralType( GeneralType.QUANTITATIVE );
        metaData.setScale( ScaleType.LOG2 );
        metaData.setType( StandardQuantitationType.RATIO );

        InputStream data = this.getClass().getResourceAsStream( "/data/testdata.txt" );

        ExpressionExperiment ee = service.load( metaData, data );

        ExpressionExperimentService eeService = ( ExpressionExperimentService ) this
                .getBean( "expressionExperimentService" );
        eeService.thaw( ee );

        assertNotNull( ee );
        assertEquals( 30, ee.getDesignElementDataVectors().size() );
        assertEquals( 12, ee.getBioAssays().size() );
    }

    /**
     * @throws Exception
     *         {@link ubic.gemma.loader.expression.simple.SimpleExpressionDataLoaderService#load(ubic.gemma.loader.expression.simple.model.ExpressionExperimentMetaData, java.io.InputStream)}.
     */
    public final void testLoadB() throws Exception {
        SimpleExpressionDataLoaderService service = ( SimpleExpressionDataLoaderService ) this
                .getBean( "simpleExpressionDataLoaderService" );

        SimpleExpressionExperimentMetaData metaData = new SimpleExpressionExperimentMetaData();
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( RandomStringUtils.randomAlphabetic( 5 ) );
        Collection<ArrayDesign> ads = new HashSet<ArrayDesign>();
        ads.add( ad );
        metaData.setArrayDesigns( ads );

        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( "mouse" );
        metaData.setTaxon( taxon );
        metaData.setName( RandomStringUtils.randomAlphabetic( 5 ) );
        metaData.setQuantitationTypeName( "testing" );
        metaData.setGeneralType( GeneralType.QUANTITATIVE );
        metaData.setScale( ScaleType.LOG2 );
        metaData.setType( StandardQuantitationType.RATIO );

        InputStream data = this.getClass().getResourceAsStream(
                "/data/loader/aov.results-2-monocyte-data-bytime.bypat.data.sort" );

        ExpressionExperiment ee = service.load( metaData, data );

        ExpressionExperimentService eeService = ( ExpressionExperimentService ) this
                .getBean( "expressionExperimentService" );
        eeService.thaw( ee );

        assertNotNull( ee );
        assertEquals( 200, ee.getDesignElementDataVectors().size() );
        assertEquals( 59, ee.getBioAssays().size() );
        // setComplete();
    }

    public final void testLoadImageCloneDesign() throws Exception {
        SimpleExpressionDataLoaderService service = ( SimpleExpressionDataLoaderService ) this
                .getBean( "simpleExpressionDataLoaderService" );

        SimpleExpressionExperimentMetaData metaData = new SimpleExpressionExperimentMetaData();
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( RandomStringUtils.randomAlphabetic( 5 ) );
        Collection<ArrayDesign> ads = new HashSet<ArrayDesign>();
        ads.add( ad );
        metaData.setArrayDesigns( ads );

        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( "human" );
        metaData.setTaxon( taxon );
        metaData.setName( RandomStringUtils.randomAlphabetic( 5 ) );
        metaData.setQuantitationTypeName( "testing" );
        metaData.setGeneralType( GeneralType.QUANTITATIVE );
        metaData.setScale( ScaleType.LOG2 );
        metaData.setType( StandardQuantitationType.RATIO );
        metaData.setProbeIdsAreImageClones( true );

        InputStream data = this.getClass().getResourceAsStream( "/data/loader/expression/luo-prostate.sample.txt" );

        ExpressionExperiment ee = service.load( metaData, data );

        ExpressionExperimentService eeService = ( ExpressionExperimentService ) this
                .getBean( "expressionExperimentService" );
        eeService.thaw( ee );

        assertNotNull( ee );

        for ( DesignElementDataVector vector : ee.getDesignElementDataVectors() ) {
            assertTrue( ( ( CompositeSequence ) vector.getDesignElement() ).getBiologicalCharacteristic().getName()
                    .startsWith( "IMAGE:" ) );
        }

        assertEquals( 173, ee.getDesignElementDataVectors().size() );
        assertEquals( 25, ee.getBioAssays().size() );
    }

}
