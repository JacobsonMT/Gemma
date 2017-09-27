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
package ubic.gemma.core.loader.genome;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.core.util.concurrent.GenericStreamConsumer;
import ubic.gemma.core.util.concurrent.ParsingStreamConsumer;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.util.Settings;

import java.io.*;
import java.util.Collection;

/**
 * Simple implementation of methods for fetching sequences from blast-formatted databases, using blastdbcmd (aka
 * fastacmd)
 *
 * @author pavlidis
 */
public class SimpleFastaCmd implements FastaCmd {

    // this name should be eventually changed to blastdbCmd.exe, since NCBI BLAST changed the name of the program.
    public static final String FASTA_CMD_ENV_VAR = "fastaCmd.exe";

    private static Log log = LogFactory.getLog( SimpleFastaCmd.class.getName() );

    private static String fastaCmdExecutable = Settings.getString( FASTA_CMD_ENV_VAR );

    private static String blastDbHome = System.getenv( "BLASTDB" );

    private String dbOption = "d";
    private String queryOption = "s";
    private String entryBatchOption = "i";

    public SimpleFastaCmd() {
        super();

        if ( System.getProperty( "os.name" ) != null && System.getProperty( "os.name" ).startsWith( "Windows" )
                && !fastaCmdExecutable.endsWith( "\"" ) ) {
            fastaCmdExecutable = StringUtils.strip( fastaCmdExecutable, "\"\'" );
            fastaCmdExecutable = "\"" + fastaCmdExecutable + "\"";
        }

        if ( fastaCmdExecutable.contains( "blastdbcmd" ) ) {
            dbOption = "db";
            queryOption = "entry";
            entryBatchOption = "entry_batch";
        }
    }

    @Override
    public Collection<BioSequence> getBatchAccessions( Collection<String> accessions, String database,
            String blastHome ) {
        try {
            return this.getMultiple( accessions, database, blastHome );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    public Collection<BioSequence> getBatchIdentifiers( Collection<Integer> identifiers, String database,
            String blastHome ) {
        try {
            return this.getMultiple( identifiers, database, blastHome );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

    }

    @Override
    public BioSequence getByAccession( String accession, String database, String blastHome ) {
        try {
            return getSingle( accession, database, blastHome );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    private Collection<BioSequence> getMultiple( Collection<? extends Object> keys, String database, String blastHome )
            throws IOException {

        if ( StringUtils.isBlank( fastaCmdExecutable ) )
            throw new IllegalStateException(
                    "No fastacmd executable: You must set " + FASTA_CMD_ENV_VAR + " in your environment." );

        if ( blastHome == null ) {
            throw new IllegalArgumentException(
                    "No blast database location specified, you must set this in your environment" );
        }
        File tmp = File.createTempFile( "sequenceIds", ".txt" );
        try (Writer tmpOut = new FileWriter( tmp );) {

            for ( Object object : keys ) {
                tmpOut.write( object.toString() + "\n" );
            }
        }
        String[] opts = new String[] { "BLASTDB=" + blastHome };
        String command = fastaCmdExecutable + " -" + dbOption + " " + database + " -" + entryBatchOption + " " + tmp
                .getAbsolutePath();
        log.debug( command );
        Process pr = null;
        log.debug( "BLASTDB=" + blastHome );
        pr = Runtime.getRuntime().exec( command, opts );

        Collection<BioSequence> sequences = getSequencesFromFastaCmdOutput( pr );
        tmp.delete();
        return sequences;

    }

    private Collection<BioSequence> getSequencesFromFastaCmdOutput( Process pr ) throws IOException {

        try (final InputStream is = new BufferedInputStream( pr.getInputStream() );
                InputStream err = pr.getErrorStream();) {

            final FastaParser parser = new FastaParser();

            ParsingStreamConsumer<BioSequence> sg = new ParsingStreamConsumer<BioSequence>( parser, is );
            GenericStreamConsumer gsc = new GenericStreamConsumer( err );
            sg.start();
            gsc.start();

            try {
                int exitVal = pr.waitFor();
                Thread.sleep( 200 ); // Makes sure results are flushed.
                log.debug( "fastacmd exit value=" + exitVal ); // often nonzero if some sequences are not found.

                return parser.getResults();
            } catch ( InterruptedException e ) {
                throw new RuntimeException( e );
            }
        }
    }

    /**
     * @param key,     which is normally either a String (ACC) or an Integer (GID)
     * @param database db
     * @throws IOException io problems
     */
    private BioSequence getSingle( Object key, String database, String blastHome ) throws IOException {
        if ( blastHome == null ) {
            blastHome = blastDbHome;
        }
        String[] opts = new String[] { "BLASTDB=" + blastHome };
        String command = fastaCmdExecutable + " -" + dbOption + " " + database + " -" + queryOption + " " + key;
        Process pr = Runtime.getRuntime().exec( command, opts );
        if ( log.isDebugEnabled() )
            log.debug( command + " ( " + opts[0] + ")" );
        Collection<BioSequence> sequences = getSequencesFromFastaCmdOutput( pr );
        if ( sequences.size() == 0 ) {
            return null;
        }
        if ( sequences.size() == 1 ) {
            return sequences.iterator().next();
        }
        throw new IllegalStateException( "Got more than one sequence!" );
    }

    @Override
    public BioSequence getByIdentifier( int identifier, String database ) {
        try {
            return getSingle( identifier, database, blastDbHome );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    public BioSequence getByIdentifier( int identifier, String database, String blastHome ) {
        try {
            return getSingle( identifier, database, blastHome );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    public Collection<BioSequence> getBatchAccessions( Collection<String> accessions, String database ) {
        return this.getBatchAccessions( accessions, database, blastDbHome );
    }

    @Override
    public Collection<BioSequence> getBatchIdentifiers( Collection<Integer> identifiers, String database ) {
        return this.getBatchIdentifiers( identifiers, database, blastDbHome );
    }

    @Override
    public BioSequence getByAccession( String accession, String database ) {
        return this.getByAccession( accession, database, blastDbHome );
    }

}
