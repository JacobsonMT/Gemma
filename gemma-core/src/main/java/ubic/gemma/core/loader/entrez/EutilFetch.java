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
package ubic.gemma.core.loader.entrez;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ubic.gemma.core.loader.entrez.pubmed.XMLUtils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author paul
 */
@SuppressWarnings("FieldCanBeLocal") // Constants are better for readability
public class EutilFetch {

    static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private static final String ESEARCH = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=";
    // private static String EFETCH =
    // "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=";
    private static final String EFETCH = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=";

    public static String fetch( String db, String searchString, int limit ) throws IOException {
        return fetch( db, searchString, Mode.TEXT, limit );
    }

    /**
     * see <a href="http://www.ncbi.nlm.nih.gov/corehtml/query/static/esummary_help.html">ncbi help</a>
     *
     * @param db           e.g., gds.
     * @param searchString search string
     * @param mode         HTML,TEXT or XML FIXME only provides XML.
     * @param limit        - Maximum number of records to return.
     * @return XML
     * @throws IOException if there is a problem while manipulating the file
     */
    public static String fetch( String db, String searchString, Mode mode, int limit ) throws IOException {

        URL searchUrl = new URL( ESEARCH + db + "&usehistory=y&term=" + searchString );
        URLConnection conn = searchUrl.openConnection();
        conn.connect();

        try (InputStream is = conn.getInputStream()) {

            factory.setIgnoringComments( true );
            factory.setValidating( false );

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse( is );

            NodeList countNode = document.getElementsByTagName( "Count" );
            Node countEl = countNode.item( 0 );

            int count;
            try {
                count = Integer.parseInt( XMLUtils.getTextValue( ( Element ) countEl ) );
            } catch ( NumberFormatException e ) {
                throw new IOException( "Could not parse count from: " + searchUrl );
            }

            if ( count == 0 )
                throw new IOException( "Got no records from: " + searchUrl );

            NodeList qnode = document.getElementsByTagName( "QueryKey" );

            Element queryIdEl = ( Element ) qnode.item( 0 );

            NodeList cknode = document.getElementsByTagName( "WebEnv" );
            Element cookieEl = ( Element ) cknode.item( 0 );

            String queryId = XMLUtils.getTextValue( queryIdEl );
            String cookie = XMLUtils.getTextValue( cookieEl );

            URL fetchUrl = new URL(
                    EFETCH + db + "&mode=" + mode.toString().toLowerCase() + "&query_key=" + queryId + "&WebEnv="
                            + cookie + "&retmax=" + limit );

            conn = fetchUrl.openConnection();
            conn.connect();
        } catch ( ParserConfigurationException | SAXException e1 ) {
            throw new RuntimeException( "Failed to parse XML: " + e1.getMessage(), e1 );
        }

        try (InputStream is = conn.getInputStream()) {

            try (BufferedReader br = new BufferedReader( new InputStreamReader( is ) )) {
                StringBuilder buf = new StringBuilder();
                String line;
                while ( ( line = br.readLine() ) != null ) {
                    buf.append( line );
                    // if ( !line.endsWith( " " ) ) {
                    // buf.append( " " );
                    // }
                }

                return buf.toString();
            }
        }

    }

    static void printElements( Document doc ) {

        NodeList nodelist = doc.getElementsByTagName( "*" );
        Node node;

        for ( int i = 0; i < nodelist.getLength(); i++ ) {
            node = nodelist.item( i );
            System.out.print( node.getNodeName() + " " );
        }

        System.out.println();

    }

    public enum Mode {
        HTML, TEXT, XML
    }

}
