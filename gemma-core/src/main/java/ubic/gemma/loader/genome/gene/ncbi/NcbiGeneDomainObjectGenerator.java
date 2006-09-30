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
package ubic.gemma.loader.genome.gene.ncbi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.genome.gene.ncbi.model.NCBIGene2Accession;
import ubic.gemma.loader.genome.gene.ncbi.model.NCBIGeneInfo;
import ubic.gemma.loader.util.sdo.SourceDomainObjectGenerator;
import ubic.gemma.model.common.description.LocalFile;

/**
 * Combines information from the gene2accession and gene_info files from NCBI Gene.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class NcbiGeneDomainObjectGenerator implements SourceDomainObjectGenerator {

    private static Log log = LogFactory.getLog( NcbiGeneDomainObjectGenerator.class.getName() );
    private AtomicBoolean producerDone = new AtomicBoolean( false );
    AtomicBoolean infoProducerDone = new AtomicBoolean( false );;

    /**
     * @return a collection of NCBIGene2Accession
     * @see ubic.gemma.loader.loaderutils.SourceDomainObjectGenerator#generate(java.lang.String)
     */
    public Collection<NCBIGene2Accession> generate( final BlockingQueue<NcbiGeneData> queue ) {

        log.info( "Fetching..." );
        NCBIGeneFileFetcher fetcher = new NCBIGeneFileFetcher();
        LocalFile geneInfoFile = fetcher.fetch( "gene_info" ).iterator().next();
        LocalFile gene2AccessionFile = fetcher.fetch( "gene2accession" ).iterator().next();

        return processLocalFiles( geneInfoFile, gene2AccessionFile, queue, true );
    }

    /**
     * Primarily for testing.
     * 
     * @param geneInfoFilePath
     * @param gene2AccesionFilePath
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<NCBIGene2Accession> generateLocal( String geneInfoFilePath, String gene2AccesionFilePath,
            BlockingQueue queue, boolean filter ) {

        try {
            URL geneInfoUrl = ( new File( geneInfoFilePath ) ).toURI().toURL();
            URL gene2AccesionUrl = ( new File( gene2AccesionFilePath ) ).toURI().toURL();

            log.info( "Fetching..." );
            NCBIGeneFileFetcher fetcher = new NCBIGeneFileFetcher();
            LocalFile geneInfoFile = fetcher.fetch( geneInfoUrl ).iterator().next();
            LocalFile gene2AccessionFile = fetcher.fetch( gene2AccesionUrl ).iterator().next();

            return processLocalFiles( geneInfoFile, gene2AccessionFile, queue, filter );

        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    public boolean isProducerDone() {
        return producerDone.get();
    }

    public void setProducerDoneFlag( AtomicBoolean flag ) {
        this.producerDone = flag;
    }

    /**
     * @param geneInfoFile
     * @param gene2AccessionFile
     * @param geneDataQueue
     * @return
     */
    private Collection<NCBIGene2Accession> processLocalFiles( final LocalFile geneInfoFile,
            LocalFile gene2AccessionFile, final BlockingQueue<NcbiGeneData> geneDataQueue, boolean filter ) {
        log.info( "Parsing geneinfo=" + geneInfoFile.asFile().getAbsolutePath() + " and gene2accession="
                + gene2AccessionFile.asFile().getAbsolutePath() );

        final NcbiGeneInfoParser infoParser = new NcbiGeneInfoParser();
        infoParser.setFilter( filter );
        final NcbiGene2AccessionParser accParser = new NcbiGene2AccessionParser();
        final File gene2accessionFileHandle = gene2AccessionFile.asFile();

        // // parse GeneInfo file into Hashtable (initialization)
        // final BlockingQueue<String> geneInfoNameQueue = new ArrayBlockingQueue<String>( 30000 );
        //
        // new Thread( new Runnable() {
        // public void run() {
        try {
            InputStream is = new FileInputStream( geneInfoFile.asFile() );
            infoParser.parse( is );
            is.close();
        } catch ( IOException e ) {
            // infoProducerDone.set( true );
            throw new RuntimeException( e );
        }
        // }
        //
        // } ).start();

        Collection<NCBIGeneInfo> geneInfoList = infoParser.getResults();
        // put into HashMap
        final Map<String, NCBIGeneInfo> geneInfoMap = new HashMap<String, NCBIGeneInfo>();
        Map<Integer, Integer> taxCount = new HashMap<Integer, Integer>();
        for ( NCBIGeneInfo o : geneInfoList ) {
            if ( !taxCount.containsKey( o.getTaxId() ) ) {
                taxCount.put( new Integer( o.getTaxId() ), new Integer( 0 ) );
            }
            taxCount.put( new Integer( o.getTaxId() ), taxCount.get( o.getTaxId() ) + 1 );
            geneInfoMap.put( o.getGeneId(), o );
        }

        for ( Integer taxId : taxCount.keySet() ) {
            log.info( "Taxon " + taxId + ": " + taxCount.get( taxId ) + " genes" );
        }

        // 1) use a producer-consumer model for Gene2Accession conversion
        // 1a) Parse Gene2Accession until the gene id changes. This means that all accessions for the gene are done.
        // 1b) Create a Collection<Gene2Accession>, and push into BlockingQueue

        Thread parseThread = new Thread( new Runnable() {
            public void run() {
                try {
                    accParser.parse( gene2accessionFileHandle, geneDataQueue, geneInfoMap );
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
                producerDone.set( true );
                log.debug( "Domain object generator done" );
            }
        } );

        parseThread.start();

        // 1c) As elements get added to BlockingQueue, NCBIGeneConverter consumes
        // and creates Gene/GeneProduct/DatabaseEntry objects.
        // 1d) Push Gene to another BlockingQueue genePersistence

        // 2) use producer-consumer model for Gene persistence
        // 2a) as elements get added to genePersistence, persist Gene and associated entries.
        /*
         * Collection<NCBIGene2Accession> ncbiGenes = accParser.getResults(); for ( NCBIGene2Accession o : ncbiGenes ) {
         * NCBIGeneInfo info = infoParser.get( o.getGeneId() ); o.setInfo( info ); }
         */
        return null;
    }

    // not used at all
    public Collection<?> generate( String accession ) {
        return null;
    }

}
