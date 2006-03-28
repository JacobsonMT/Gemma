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
package ubic.gemma.loader.association;

import java.util.Collection;

import ubic.basecode.util.StringUtil;
import ubic.gemma.loader.util.parser.BasicLineParser;
import ubic.gemma.model.association.ProteinProteinInteractionDao;
import ubic.gemma.model.association.ProteinProteinInteractionImpl;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseDao;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductDao;

/**
 * Class to parse a file of protein-protein interactions (retrieved from BIND). Format: (read whole row)
 * 
 * <pre>
 *        pl_ncbiid\tp2_ncbiid\t external_db\t db_id\t numMentions\t action\t
 * </pre>
 * 
 * @author anshu
 * @version $Id$
 */
public class PPIFileParser extends BasicLineParser /* implements Persister */{

    public static final int PPI_FIELDS_PER_ROW = 6;
    public static final int PERSIST_CONCURRENTLY = 1;
    public static final int DO_NOT_PERSIST_CONCURRENTLY = 0;
    public static final int PERSIST_DEFAULT = 0;

    private int mPersist = PERSIST_DEFAULT;
    private GeneProductDao gpDao;
    private ProteinProteinInteractionDao ppiDao;
    private ExternalDatabaseDao dbDao;

    public PPIFileParser( int persistType, GeneProductDao gdao, ProteinProteinInteractionDao ldao,
            ExternalDatabaseDao dDao ) {
        this.mPersist = persistType;
        this.gpDao = gdao;
        this.ppiDao = ldao;
        this.dbDao = dDao;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.LineParser#parseOneLine(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public Object parseOneLine( String line ) {
        System.out.println( line );
        String[] fields = StringUtil.splitPreserveAllTokens( line, '\t' );

        if ( fields.length != PPI_FIELDS_PER_ROW ) {
            throw new IllegalArgumentException( "Line is not in the right format: has " + fields.length
                    + " fields, expected " + PPI_FIELDS_PER_ROW );
        }

        Collection<GeneProduct> c;
        ProteinProteinInteractionImpl assoc = new ProteinProteinInteractionImpl();
        GeneProduct g1 = null;
        GeneProduct g2 = null;
        String id = null;
        ExternalDatabase db;
        try {
            id = fields[1];
            c = gpDao.findByNcbiId( id );
            if ( ( c != null ) && ( c.size() == 1 ) ) {
                g1 = ( c.iterator() ).next();
            } else
                throw new Exception( "gene product " + id + " not found. Entry skipped." );

            id = fields[2];
            c = gpDao.findByNcbiId( id );
            if ( ( c != null ) && ( c.size() == 1 ) ) {
                g2 = ( c.iterator() ).next();
            } else
                throw new Exception( "gene " + id + " not found. Entry skipped." );

            assoc.setFirstProduct( g1 );
            assoc.setSecondProduct( g2 );
            db = ExternalDatabase.Factory.newInstance();
            db.setName( fields[3] );
            db = dbDao.findOrCreate( db );
            // db=dbDao.findByName(fields[8]); //calls fior external db to be pre-loaded
            assoc.setSource( db );

            // if ( mPersist == PERSIST_CONCURRENTLY ) {
            // ppiDao.create( assoc ); // FIXME parser should not persist
            // }
        } catch ( Exception e ) {
            log.error( e.toString() );
        }
        return null;
    }

    /**
     * 
     */
    public void removeAll() {
        Collection col = ppiDao.loadAll();
        ppiDao.remove( col );
    }

}
