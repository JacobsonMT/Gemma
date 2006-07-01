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
import java.net.URL;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

import ubic.gemma.model.common.description.BibliographicReference;

/**
 * Class that can retrieve pubmed records (in XML format) via HTTP. The url used is configured via a resource.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="pubMedXmlFetcher"
 */
public class PubMedXMLFetcher {

    protected static final Log log = LogFactory.getLog( PubMedXMLFetcher.class );
    private String uri;

    public PubMedXMLFetcher() {
        try {
            Configuration config = new PropertiesConfiguration( "Gemma.properties" );
            String baseURL = ( String ) config.getProperty( "entrez.efetch.baseurl" );
            String db = ( String ) config.getProperty( "entrez.efetch.pubmed.db" );
            String idtag = ( String ) config.getProperty( "entrez.efetch.pubmed.idtag" );
            String retmode = ( String ) config.getProperty( "entrez.efetch.pubmed.retmode" );
            String rettype = ( String ) config.getProperty( "entrez.efetch.pubmed.rettype" );
            uri = baseURL + "&" + db + "&" + retmode + "&" + rettype + "&" + idtag;
        } catch ( ConfigurationException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * For an integer pubmed id
     * 
     * @param pubMedId
     * @return BibliographicReference for the id given.
     * @throws IOException
     */
    public BibliographicReference retrieveByHTTP( int pubMedId ) throws IOException, SAXException,
            ParserConfigurationException {
        URL toBeGotten = new URL( uri + pubMedId );
        log.info( "Fetching " + toBeGotten );
        PubMedXMLParser pmxp = new PubMedXMLParser();
        Collection<BibliographicReference> results = pmxp.parse( toBeGotten.openStream() );
        if ( results == null || results.size() == 0 ) {
            return null;
        }
        assert results.size() == 1;
        return results.iterator().next();
    }

    /**
     * For collection of integer pubmed ids.
     * 
     * @param pubMedIds
     * @return Collection<BibliographicReference>
     * @throws IOException
     */
    public Collection<BibliographicReference> retrieveByHTTP( Collection<Integer> pubMedIds ) throws IOException,
            SAXException, ParserConfigurationException {
        StringBuilder buf = new StringBuilder();
        for ( Integer integer : pubMedIds ) {
            buf.append( integer + "," );
        }
        URL toBeGotten = new URL( uri + StringUtils.chomp( buf.toString() ) );
        log.info( "Fetching " + toBeGotten );
        PubMedXMLParser pmxp = new PubMedXMLParser();
        return pmxp.parse( toBeGotten.openStream() );
    }
}
