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

import ubic.gemma.model.common.description.LocalFile;

import junit.framework.TestCase;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignFetcherTest extends TestCase {

    /**
     * Test method for {@link ubic.gemma.loader.expression.arrayExpress.ArrayDesignFetcher#fetch(java.lang.String)}.
     */
    public final void testFetch() {
        ArrayDesignFetcher fetcher = new ArrayDesignFetcher();
     //   fetcher.setForce( true );
        Collection<LocalFile> results = fetcher.fetch( "A-AFFY-6" );
        assertEquals( 3, results.size() );
    }

    public final void testFetchNoCompositeSequences() {
        ArrayDesignFetcher fetcher = new ArrayDesignFetcher();
        Collection<LocalFile> results = fetcher.fetch( "A-FPMI-3" );
        assertEquals( 2, results.size() );
    }

}
