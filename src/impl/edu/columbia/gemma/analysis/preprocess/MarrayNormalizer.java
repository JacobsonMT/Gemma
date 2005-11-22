/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.analysis.preprocess;

import baseCode.dataStructure.matrix.DoubleMatrixNamed;
import baseCode.util.RCommand;
import edu.columbia.gemma.tools.MArrayRaw;
import edu.columbia.gemma.tools.RCommander;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class MarrayNormalizer extends RCommander {

    public MarrayNormalizer() {
        super();
        rc.voidEval( "library(marray)" );
    }

    public MarrayNormalizer( RCommand rc ) {
        super( rc );
        rc.voidEval( "library(marray)" );
    }

    /**
     * Apply a normalization method from the marray BioConductor package. This method yields normalized log ratios, so
     * the summarization step is included as well.
     * 
     * @param channelOneSignal
     * @param channelTwoSignal
     * @param channelOneBackground
     * @param channelTwoBackground
     * @param weights
     * @param method Name of the method (or its valid abbreviation), such as "median", "loess", "printtiploess".
     * @return
     */
    protected DoubleMatrixNamed mNorm( DoubleMatrixNamed channelOneSignal, DoubleMatrixNamed channelTwoSignal,
            DoubleMatrixNamed channelOneBackground, DoubleMatrixNamed channelTwoBackground, DoubleMatrixNamed weights,
            String method ) {
        MArrayRaw mRaw = new MArrayRaw( this.rc );
        mRaw.makeMArrayLayout( channelOneSignal.rows() );
        String mRawVarName = mRaw.makeMArrayRaw( channelOneSignal, channelTwoSignal, channelOneBackground,
                channelTwoBackground, weights );

        String normalizedMatrixVarName = "normalized." + channelOneSignal.hashCode();
        rc.voidEval( normalizedMatrixVarName + "<-maM(maNorm(" + mRawVarName + ", norm=\"" + method + "\" ))" );
        log.info( "Done normalizing" );

        // the normalized
        DoubleMatrixNamed resultObject = rc.retrieveMatrix( normalizedMatrixVarName );

        // clean up.
        rc.remove( mRawVarName );
        rc.remove( normalizedMatrixVarName );
        return resultObject;
    }

    protected DoubleMatrixNamed mNorm( DoubleMatrixNamed channelOneSignal, DoubleMatrixNamed channelTwoSignal,
            String method ) {
        MArrayRaw mRaw = new MArrayRaw( this.rc );
        mRaw.makeMArrayLayout( channelOneSignal.rows() );
        String mRawVarName = mRaw.makeMArrayRaw( channelOneSignal, channelTwoSignal, null, null, null );

        String normalizedMatrixVarName = "normalized." + channelOneSignal.hashCode();
        rc.voidEval( normalizedMatrixVarName + "<-maM(maNorm(" + mRawVarName + ", norm=\"" + method + "\" ))" );
        log.info( "Done normalizing" );

        // the normalized
        DoubleMatrixNamed resultObject = rc.retrieveMatrix( normalizedMatrixVarName );

        // clean up.
        rc.remove( mRawVarName );
        rc.remove( normalizedMatrixVarName );
        return resultObject;
    }

}
