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
package ubic.gemma.loader.genome.llnl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.loader.util.QueuingParser;
import ubic.gemma.loader.util.parser.BasicLineParser;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * <code>
 *  100     LLAM    5753    a       7       381     human   AA17697
 * </code>
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ImageCumulativePlatesParser extends BasicLineParser implements QueuingParser {

    BlockingQueue<BioSequence> results = new ArrayBlockingQueue<BioSequence>( 10000 );
    private ExternalDatabase genbank;

    public ImageCumulativePlatesParser() {
        super();
        initGenbank();
    }

    @Override
    protected void addResult( Object obj ) {
        results.add( ( BioSequence ) obj );
    }

    @Override
    public Collection<BioSequence> getResults() {
        return results;
    }

    private void initGenbank() {
        // if ( externalDatabaseService != null ) {
        // genbank = externalDatabaseService.find( "Genbank" );
        // } else {
        genbank = ExternalDatabase.Factory.newInstance();
        genbank.setName( "Genbank" );
        genbank.setType( DatabaseType.SEQUENCE );
        // }
    }

    public Object parseOneLine( String line ) {
        String[] fields = StringUtils.splitPreserveAllTokens( line, '\t' );

        if ( StringUtils.isBlank( fields[7] ) ) {
            return null;
        }

        BioSequence seq = BioSequence.Factory.newInstance();

        seq.setName( fields[7] );

        StringBuilder buf = new StringBuilder();

        buf.append( "IMAGE clone" );

        if ( fields.length > 8 ) {
            buf.append( "Other accession:" );
            for ( int i = 8; i < fields.length; i++ ) {
                buf.append( " " + fields[i] );
            }
        }

        Taxon t = Taxon.Factory.newInstance();
        t.setCommonName( fields[6] );

        seq.setTaxon( t );

        seq.setDescription( buf.toString() );

        DatabaseEntry acc = DatabaseEntry.Factory.newInstance();
        acc.setAccession( fields[7] );
        acc.setExternalDatabase( genbank );

        seq.setSequenceDatabaseEntry( acc );

        return seq;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.QueuingParser#parse(java.io.InputStream, java.util.concurrent.BlockingQueue)
     */
    @SuppressWarnings("unchecked")
    public void parse( InputStream inputStream, BlockingQueue queue ) throws IOException {
        this.results = queue;
        parse( inputStream );

    }
}
