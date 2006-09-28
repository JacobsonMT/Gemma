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
package ubic.gemma.loader.genome.taxon;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.loader.util.parser.BasicLineMapParser;
import ubic.gemma.model.genome.Taxon;

/**
 * Parse the "names.dmp" file from NCBI, ftp://ftp.ncbi.nih.gov/pub/taxonomy/.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class TaxonParser extends BasicLineMapParser {

    Map<Integer, Taxon> results = new HashMap<Integer, Taxon>();

    @Override
    public boolean containsKey( Object key ) {
        return results.containsKey( key );
    }

    @Override
    public Taxon get( Object key ) {
        Taxon t = results.get( key );
        //
        // // some are not really usable or taxa, like Phylum names. We identify these by the fact that they lack common
        // // names.
        // if ( t.getCommonName() == null ) {
        // return null;
        // }
        return t;
    }

    @Override
    protected Object getKey( Object newItem ) {
        return ( ( Taxon ) newItem ).getNcbiId();
    }

    @Override
    public Collection<Taxon> getResults() {
        // Collection<Taxon> cleanedResults = new HashSet<Taxon>();
        // for ( Taxon taxon : results.values() ) {
        // Taxon toAdd = this.get( taxon.getNcbiId() );
        // if ( toAdd == null ) continue;
        // cleanedResults.add( toAdd );
        // }
        // return cleanedResults;
        return results.values();
    }

    @Override
    public Object parseOneLine( String line ) {
        String[] fields = StringUtils.splitPreserveAllTokens( line, '|' );

        int ncbiid = Integer.parseInt( StringUtils.strip( fields[0] ) );

        if ( !results.containsKey( ncbiid ) ) {
            Taxon t = Taxon.Factory.newInstance();
            t.setNcbiId( ncbiid );
            results.put( ncbiid, t );
        }

        String tag = StringUtils.strip( fields[3] );
        if ( tag.equals( "scientific name" ) ) {
            results.get( ncbiid ).setScientificName( StringUtils.strip( fields[1] ) );
        } else if ( tag.equals( "genbank common name" ) ) {
            results.get( ncbiid ).setCommonName( fields[1] );
        }

        return results.get( ncbiid );

    }

    @Override
    protected void put( Object key, Object value ) {
        results.put( ( Integer ) key, ( Taxon ) value );
    }

}
