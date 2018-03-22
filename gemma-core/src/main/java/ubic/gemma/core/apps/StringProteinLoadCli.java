/*
 * The Gemma project
 *
 * Copyright (c) 2010 University of British Columbia
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

package ubic.gemma.core.apps;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.loader.protein.StringProteinInteractionLoader;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.AbstractCLIContextCLI;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Cli to load protein protein interaction data from STRING using biomart as an ensembl gene entrez gene mapper. String
 * data files uses ensembl identifiers whereas Gemma uses ncbi/entrez gene identifiers biomart file translates between
 * the two. Up to 4 optional input parameters can be supplied to run the cli. If no input parameters are supplied then
 * all files are fetched from remote sites based on default file names and all valid taxon in gemma are processed. Login
 * details should be supplied for authentication on the command line.
 * These 4 parameters are optional:
 * <ul>
 * <li>Taxon: If a taxon is given only protein data is retrieved for that taxon, if not supplied then all protein data
 * for all eligible taxon held in system are run.This has to be supplied if biomart file is given.
 * <li>isStringFileRemote: Gives the option to indicate if the stringProteinProteinFileName provided is to be fetched
 * from a remote site
 * <li>stringProteinProteinFileName: Name of string protein file to download
 * <li>biomartFileName: name of Local biomart file
 * </ul>
 * Example usage: Import all protein protein interactions for zebrafish using a local copy of the string file and local
 * copy of the biomart file -u username -p password -t zebrafish -b biomartFile.txt -s
 * /gemmaData/string.embl.de/newstring_download_protein.links.detailed.v8.2.txt.gz Import all protein protein
 * interactions for all taxon using a remote named copy of the string file and downloading the biomart file -u username
 * -p password -r remote -s /gemmaData/string.embl.de/newstring_download_protein.links.detailed.v8.2.txt.gz Import all
 * protein protein interactions for all taxon using a remote string and biomart file -u username -p password
 *
 * @author ldonnison
 */
public class StringProteinLoadCli extends AbstractCLIContextCLI {
    /**
     * Name of local biomart file to process if null then biomart files are retrieved from biomart service
     */
    private File biomartFileName = null;
    /**
     * Name of local stringProteinProteinFileName to process which avoids fetching file from string site
     */
    private File stringProteinProteinFileNameLocal = null;
    /**
     * Name of remote stringProteinProteinFileName to fetch if not supplied then fetch file as specified in properties
     * file
     */
    private String stringProteinProteinFileNameRemote = null;
    /**
     * Taxon name of which to process protein protein interactions for
     */
    private String taxonName = null;

    /**
     * Main method
     *
     * @param args optional
     */
    public static void main( String[] args ) {
        StringProteinLoadCli p = new StringProteinLoadCli();
        Exception exception = p.doWork( args );
        if ( exception != null ) {
            AbstractCLI.log.error( exception, exception );
        }
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.MISC;
    }

    @Override
    public String getCommandName() {
        return "updatePPIs";
    }

    @Override
    @SuppressWarnings("static-access")
    protected void buildOptions() {
        // taxon
        Option taxonNameOption = OptionBuilder.hasArg().withDescription(
                "Taxon short name e.g. 'mouse' (use with --genefile, or alone to process all "
                        + "known genes for the taxon, or with --all-arrays to process all arrays for the taxon." )
                .withLongOpt( "taxon" ).create( 't' );
        this.addOption( taxonNameOption );
        //
        Option isStringFileRemote = OptionBuilder.hasArg().withDescription(
                "Flag to indicate whether given string protein interaction file is remote or local (use with --stringProteinProteinFileName  "
                        + "this is given as the remote file may change name and it is faster to use a local copy of the file" )
                .withLongOpt( "isStringFileRemote" ).create( 'r' );
        this.addOption( isStringFileRemote );

        Option stringProteinProteinFileName = OptionBuilder.hasArg().withDescription(
                "Input File Path for string protein interaction file  "
                        + "Optional path to the protein.links.detailed file" )
                .withLongOpt( "stringProteinProteinFileName" ).create( 's' );
        this.addOption( stringProteinProteinFileName );

        Option biomartFileNameLocal = OptionBuilder.hasArg().withDescription(
                "Input File Path for biomart file should only be supplied when taxon supplied "
                        + "Optional can only be " ).withLongOpt( "biomartFileName" ).create( 'b' );
        this.addOption( biomartFileNameLocal );

    }

    /**
     * Main doWork method validates input and calls the loader
     */
    @Override
    protected Exception doWork( String[] args ) {
        // should at least have login info
        Exception err = this.processCommandLine( args );
        if ( err != null )
            return err;
        // call the loader
        try {
            this.loadProteinProteinInteractions();
        } catch ( IOException e ) {
            return e;
        }

        return null;

    }

    @Override
    public String getShortDesc() {
        return "Loads protein protein interaction data";
    }

    /**
     * Validate input parameters. If a biomart file is provided then a taxon should be provided. If a
     * stringProteinProteinFileName is provided then the isStringFileRemote should be set to indicate whether the named
     * file is remote or local (The file is big so to save time good to use a current up to date copy). If local files
     * are to be processed check that they are readable.
     */
    @Override
    protected void processOptions() {

        super.processOptions();

        // if a biomart file name is given then the taxon should be specified
        if ( this.hasOption( "biomartFileName" ) ) {
            if ( this.getOptionValue( 't' ) == null ) {
                throw new IllegalArgumentException( "Please specify the taxon when specifying the biomart file name" );
            }
        }

        if ( this.getOptionValue( 't' ) != null ) {
            taxonName = this.getOptionValue( 't' );
            AbstractCLI.log.info( "Getting file for taxon " + taxonName );
        }

        if ( this.getOptionValue( 's' ) != null ) {
            String stringProteinProteinFileName = this.getOptionValue( 's' );
            if ( this.getOptionValue( 'r' ) != null ) {
                this.stringProteinProteinFileNameRemote = stringProteinProteinFileName;
                AbstractCLI.log.info( "Processing string file from remote site" );
            } else {
                // validate the file
                this.stringProteinProteinFileNameLocal = new File( stringProteinProteinFileName );
                AbstractCLI.log.info( "Processing string file from local site" );
                if ( !stringProteinProteinFileNameLocal.canRead() ) {
                    throw new IllegalArgumentException(
                            "The specified local stringProteinProteinFileName can not be read "
                                    + stringProteinProteinFileNameLocal );
                }
            }
        }
        if ( this.getOptionValue( 'b' ) != null ) {
            AbstractCLI.log.info( "Processing biomart file from local site" );
            biomartFileName = new File( this.getOptionValue( 'b' ) );
            if ( !biomartFileName.canRead() ) {
                throw new IllegalArgumentException( "The specified biomart file can not be read " + biomartFileName );
            }
        }
        // only authenticated users run this code
        this.requireLogin();
    }

    /**
     * Method to wrap call to loader. Ensures that all spring beans are configured.
     *
     * @throws IOException IO problems
     */
    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public void loadProteinProteinInteractions() throws IOException {
        StringProteinInteractionLoader loader = new StringProteinInteractionLoader();

        GeneService geneService = this.getBean( GeneService.class );
        ExternalDatabaseService externalDatabaseService = this.getBean( ExternalDatabaseService.class );
        // set all the loaders
        if ( this.getPersisterHelper() == null || geneService == null || externalDatabaseService == null ) {
            throw new RuntimeException( "Spring configuration problem" );
        }
        loader.setPersisterHelper( this.getPersisterHelper() );
        loader.setGeneService( geneService );
        loader.setExternalDatabaseService( externalDatabaseService );

        Collection<Taxon> taxa = this.getValidTaxon();
        // some of these parameters can be null
        loader.load( stringProteinProteinFileNameLocal, stringProteinProteinFileNameRemote, biomartFileName, taxa );
    }

    /**
     * If a taxon is supplied on the command line then process protein interactions for that taxon. If no taxon is
     * supplied then create a list of valid taxon to process from those stored in gemma: Criteria are does this taxon
     * have usable genes and is it a species.
     *
     * @return Collection of Taxa to process
     */
    private Collection<Taxon> getValidTaxon() {
        Taxon taxon;
        TaxonService taxonService = this.getBean( TaxonService.class );
        Collection<Taxon> taxa = new ArrayList<>();

        if ( taxonName != null && StringUtils.isNotBlank( taxonName ) ) {
            taxon = taxonService.findByCommonName( taxonName );

            if ( taxon == null || !( taxon.getIsSpecies() ) || !( taxon.getIsGenesUsable() ) ) {
                throw new IllegalArgumentException( "The taxon common name supplied: " + taxonName
                        + " Either does not match anything in GEMMA, or is not a species or does have usable genes" );
            }
            taxa.add( taxon );
        } else {
            for ( Taxon taxonGemma : taxonService.loadAll() ) {
                // only those taxon that are species and have usable genes should be processed
                if ( taxonGemma != null && taxonGemma.getIsSpecies() && taxonGemma.getIsGenesUsable() && (
                        taxonGemma.getCommonName() != null ) && !( taxonGemma.getCommonName().isEmpty() ) ) {
                    taxa.add( taxonGemma );
                }
            }

            if ( taxa.isEmpty() ) {
                throw new RuntimeException(
                        "There are no valid taxa in GEMMA to process. Valid taxon are those that are species and have usable genes." );
            }
            AbstractCLI.log.info( "Processing " + taxa.size() + "taxa " );
        }
        return taxa;
    }

}
