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
package ubic.gemma.loader.description;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author pavlidis
 * @version $Id$
 */
public class AllDescriptionLoaderTests {

    public static Test suite() {
        TestSuite suite = new TestSuite( "Test for ubic.gemma.loader.description" );
        // $JUnit-BEGIN$
        suite.addTestSuite( OntologyEntryLoaderIntegrationTest.class );
        // suite.addTestSuite( GeneOntologyLoaderIntegrationTest.class ); this test is too big to run routinely.
        suite.addTestSuite( GeneOntologyEntryParserTest.class );
        // $JUnit-END$
        return suite;
    }

}
