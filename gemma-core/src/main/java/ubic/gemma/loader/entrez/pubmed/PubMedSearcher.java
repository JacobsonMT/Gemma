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
package ubic.gemma.loader.entrez.pubmed;

import java.io.IOException;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;

import org.springframework.beans.factory.BeanFactory;
import org.xml.sax.SAXException;

import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Simple application to perform pubmed searches from a list of terms, and persist the results in the database.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class PubMedSearcher extends AbstractSpringAwareCLI {
    protected static BeanFactory ctx = null;
    static PubMedSearch pms = new PubMedSearch();

    public PubMedSearcher() {
        super();
    }

    public static void main( String[] args ) {
        PubMedSearcher p = new PubMedSearcher();
        try {
            p.doWork( args );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( "pubmed [options] searchterm1 searchterm2 ... searchtermN", args );

        if ( err != null ) return err;

        try {
            Collection<BibliographicReference> refs = pms.searchAndRetriveByHTTP( getArgList() );

            System.out.println( refs.size() + " references found" );

            if ( hasOption( "d" ) ) {
                getPersisterHelper().persist( refs );
            }

        } catch ( IOException e ) {
            e.printStackTrace();
        } catch ( SAXException e ) {
            e.printStackTrace();
        } catch ( ParserConfigurationException e ) {
            e.printStackTrace();
        }
        resetLogging();
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.AbstractSpringAwareCLI#buildOptions()
     */
    @Override
    protected void buildOptions() {
        addOption( "d", "persist", false, "Persist the results. Otherwise just search." );
    }

}
