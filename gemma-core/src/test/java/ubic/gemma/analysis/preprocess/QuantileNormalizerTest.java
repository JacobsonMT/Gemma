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
/*
 * The Gemma project
 * 
 * Copyright (c) 2008 Columbia University
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
package ubic.gemma.analysis.preprocess;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.reader.DoubleMatrixReader;

/**
 * @author pavlidis
 * @version $Id$
 */
public class QuantileNormalizerTest extends TestCase {
    private static Log log = LogFactory.getLog( QuantileNormalizerTest.class.getName() );

    DoubleMatrix<String, String> tester;
    QuantileNormalizer<String, String> qn;

    private boolean connected = false;

    @Override
    public void setUp() throws Exception {
        DoubleMatrixReader reader = new DoubleMatrixReader();
        tester = reader.read( this.getClass().getResourceAsStream( "/data/testdata.txt" ) );
        assert tester != null;

        try {
            qn = new QuantileNormalizer<String, String>();
            connected = true;
        } catch ( Exception e ) {
            log.error( e );
            connected = false;
        }

        log.debug( "Setup done" );
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        tester = null;
        if ( connected ) qn.cleanup();
    }

    /*
     * Test method for 'ubic.gemma.analysis.preprocess.QuantileNormalizer.normalize(DenseDoubleMatrix2DNamed)'
     */
    public void testNormalize() {
        if ( !connected ) {
            log.warn( "Could not access R, skipping test." );
            return;
        }
        DoubleMatrix<String, String> result = qn.normalize( tester );
        assertEquals( -0.525, result.get( 0, 9 ), 0.001 );

        for ( int i = 0; i < tester.columns(); i++ ) {
            assertEquals( tester.getColName( i ), result.getColName( i ) );
        }
        for ( int i = 0; i < tester.rows(); i++ ) {
            assertEquals( tester.getRowName( i ), result.getRowName( i ) );
        }
    }
}
