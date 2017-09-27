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
package ubic.gemma.core.analysis.expression.diff;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.persistence.util.Settings;

import java.io.File;

/**
 * @author keshav
 */
public class DifferentialExpressionFileUtils {

    public static final String PVALUE_DIST_SUFFIX = ".dist.txt";
    private static final String PVALUE_DIST = "diff" + File.separatorChar + "diffExStatDistributions";
    private static Log log = LogFactory.getLog( DifferentialExpressionFileUtils.class );
    private static String analysisStoragePath = Settings.getAnalysisStoragePath();

    public static File getBaseDifferentialDirectory( String shortName ) {
        File f = null;
        if ( shortName == null ) {
            f = new File( analysisStoragePath + File.separatorChar + PVALUE_DIST + File.separatorChar );
            log.debug( "No experiment name provided ... returning directory: " + f.toString() );
        } else {
            f = new File( analysisStoragePath + File.separatorChar + PVALUE_DIST + File.separatorChar + shortName );
            log.debug( "Returning directory: " + f.toString() );
        }

        return f;
    }

}
