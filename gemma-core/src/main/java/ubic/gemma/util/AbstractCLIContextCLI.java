/*
 * The gemma project
 * 
 * Copyright (c) 2013 University of British Columbia
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
package ubic.gemma.util;

import ubic.gemma.apps.GemmaCLI.CommandGroup;

/**
 * Spring configuration for CLI.
 *
 * @author anton date: 18/02/13
 */
public abstract class AbstractCLIContextCLI extends AbstractSpringAwareCLI {

    protected static void tryDoWork( AbstractCLIContextCLI p, String[] args ) {
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            System.exit( 0 );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    public abstract CommandGroup getCommandGroup();

    @Override
    protected String[] getAdditionalSpringConfigLocations() {
        return new String[] { "classpath*:ubic/gemma/cliContext-component-scan.xml" };
    }

}
