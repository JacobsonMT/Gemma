/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.analysis.diff;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Tests the {@link DifferentialExpessionAnalysis} tool.
 * 
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpessionAnalysisTest extends BaseAnalyzerTest {

    private Log log = LogFactory.getLog( this.getClass() );

    DifferentialExpressionAnalysis analysis = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.diff.BaseAnalyzerTest#onSetUpInTransaction()
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        analysis = new DifferentialExpressionAnalysis();
    }

    /**
     * Tests determineAnalysis.
     * <p>
     * 2 experimental factors
     * <p>
     * 2 factor value / experimental factor
     * <p>
     * Expected analyzer: {@link TwoWayAnovaWithoutInteractionsAnalyzer}
     */
    public void testDetermineAnalysis() {
        AbstractAnalyzer analyzer = analysis.determineAnalysis( ee, quantitationTypeToUse, bioAssayDimension );
        assertTrue( analyzer instanceof TwoWayAnovaWithoutInteractionsAnalyzer );
    }

    // /**
    // * Tests analyze.
    // * <p>
    // * Expected Result: UnsupportedOperationException not null.
    // *
    // * @throws Exception
    // */
    // public void testAnalyze() {
    // TODO FIXME
    // UnsupportedOperationException ex = null;
    // try {
    // analysis.analyze( ee, quantitationTypeToUse, bioAssayDimension );
    // } catch ( UnsupportedOperationException e ) {
    // e.printStackTrace();
    // ex = e;
    // } finally {
    // assertNull( ex );
    // }
    // }

}
