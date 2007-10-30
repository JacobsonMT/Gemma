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
package ubic.gemma.loader.expression.arrayExpress;

import java.util.Collection;

import junit.framework.TestCase;

import ubic.gemma.loader.util.fetcher.FtpArchiveFetcher;
import ubic.gemma.model.common.description.LocalFile;

/**
 * @author pavlidis
 * @version $Id$
 */
public class DataFileFetcherIntegrationTest extends TestCase {
    FtpArchiveFetcher faf;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        faf = new DataFileFetcher();
    }

    /*
     * Test method for 'ubic.gemma.loader.expression.arrayExpress.DataFileFetcher.fetch(String)'
     */
    public void testFetch() throws Exception {
        Collection<LocalFile> files = faf.fetch( "E-SMDB-14" );
        assertTrue( files.size() > 0 );
    }
}
