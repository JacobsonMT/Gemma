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
package ubic.gemma.loader.expression.smd.model;

import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

import org.xml.sax.SAXException;

/**
 * @author pavlidis
 * @version $Id$
 */
public class PublicationMetaTest extends TestCase {
    InputStream testStream;
    SMDPublication pubtest;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testStream = PublicationMetaTest.class.getResourceAsStream( "/data/smd.pub-meta.test.txt" );
        pubtest = new SMDPublication();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Class under test for void read(InputStream)
     */
    public void testReadInputStream() throws IOException, SAXException {
        pubtest.read( testStream );
        String expectedReturn = "Diversity of gene expression in adenocarcinoma of the lung.";
        String actualReturn = pubtest.getTitle();
        assertEquals( expectedReturn, actualReturn );
    }

}
