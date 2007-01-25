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
package ubic.gemma.apps;

import java.io.InputStream;
import java.util.Collection;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.basecode.util.FileTools;
import ubic.gemma.loader.genome.GffParser;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Import genes from MirBASE files (http://microrna.sanger.ac.uk/sequences/ftp.shtml). You have to download the file.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class MirBaseLoader extends AbstractSpringAwareCLI {

    private PersisterHelper persisterHelper;
    private String fileName;
    private TaxonService taxonService;

    private String taxonName = null;

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option fileOption = OptionBuilder.hasArg().isRequired().withArgName( "GFF file" ).withDescription(
                "Path to GFF file" ).withLongOpt( "file" ).create( 'f' );

        addOption( fileOption );
        Option taxonOption = OptionBuilder.hasArg().withArgName( "taxon" ).isRequired().withDescription(
                "Taxon common name (e.g., human) for genes to be loaded" ).create( 't' );

        addOption( taxonOption );

    }

    public static void main( String[] args ) {
        MirBaseLoader p = new MirBaseLoader();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {
        try {
            Exception err = processCommandLine( "Mir loader", args );
            if ( err != null ) return err;

            GffParser parser = new GffParser();
            InputStream gffFileIs = FileTools.getInputStreamFromPlainOrCompressedFile( fileName );

            if ( gffFileIs == null ) {
                log.error( "No file " + fileName + " was readable" );
                bail( ErrorCode.INVALID_OPTION );
            }

            Taxon taxon = null;
            taxon = taxonService.findByCommonName( this.taxonName );
            if ( taxon == null ) {
                throw new IllegalArgumentException( "No taxon named " + taxonName );
            }
            parser.setTaxon( taxon );
            parser.parse( gffFileIs );
            gffFileIs.close();
            Collection<Gene> res = parser.getResults();

            persisterHelper.persist( res );

        } catch ( Exception e ) {
            log.error( e, e );
            return e;
        }
        return null;

    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 'f' ) ) {
            this.fileName = this.getOptionValue( 'f' );
        }
        if ( this.hasOption( 't' ) ) {
            this.taxonName = this.getOptionValue( 't' );
        }
        this.taxonService = ( TaxonService ) this.getBean( "taxonService" );
        persisterHelper = ( PersisterHelper ) this.getBean( "persisterHelper" );
    }

}
