/*
 * The Gemma project.
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
package ubic.gemma.apps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;

import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.common.Describable;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignAnnotationFileEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PredictedGene;
import ubic.gemma.model.genome.ProbeAlignedRegion;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.ontology.GeneOntologyService;
import ubic.gemma.ontology.OntologyTerm;
import ubic.gemma.ontology.OntologyTools;

/**
 * Given an array design creates a Gene Ontology Annotation file
 * 
 * @author klc
 * @versio $Id$
 */
public class ArrayDesignGOAnnotationGeneratorCli extends ArrayDesignSequenceManipulatingCli {

    // constants
    final String SHORT = "short";
    final String LONG = "long";
    final String BIOPROCESS = "biologicalprocess";
    final String BIOLOGICAL_PROCESS = "biological_process";

    // services
    Gene2GOAssociationService gene2GoAssociationService;
    GeneService geneService;
    CompositeSequenceService compositeSequenceService;
    GeneOntologyService goService;

    // file info
    String batchFileName;

    String fileName = null;

    // types
    boolean shortAnnotations;
    boolean longAnnotations;
    boolean biologicalProcessAnnotations;

    boolean includeGemmaGenes;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();

        Option annotationFileOption = OptionBuilder.hasArg().withArgName( "Annotation file name" ).withDescription(
                "The name of the Annotation file to be generated [Default = stdout]" ).withLongOpt( "annotation" )
                .create( 'f' );

        Option genesIncludedOption = OptionBuilder.hasArg().withArgName( "Genes to include" ).withDescription(
                "The type of genes that will be included: all or standard."
                        + " All includes predicted genes and probe aligned regions. "
                        + "Standard mode only includes known genes [Default = standard]" ).withLongOpt( "genes" )
                .create( 'g' );

        Option annotationType = OptionBuilder.hasArg().withArgName( "Type of annotation file" ).withDescription(
                "Which GO terms to add to the annotation file:  short, long, biologicalprocess "
                        + "[Default=short (no parents)]. If you select biologialprocess, parents are not included." )
                .withLongOpt( "type" ).create( 't' );

        Option fileLoading = OptionBuilder
                .hasArg()
                .withArgName( "Batch Generating of annotation files" )
                .withDescription(
                        "Use specified file for batch generating annotation files. file format: (per line) "
                                + "GPL,outputFileName,[short|long|biologicalprocess] Note:  Overrides -a,-t,-f command line options " )
                .withLongOpt( "load" ).create( 'l' );

        addOption( annotationFileOption );
        addOption( annotationType );
        addOption( fileLoading );
        addOption( genesIncludedOption );

    }

    public static void main( String[] args ) {
        ArrayDesignGOAnnotationGeneratorCli p = new ArrayDesignGOAnnotationGeneratorCli();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Array design probe ontology annotation ", args );
        if ( err != null ) return err;

        try {
            if ( batchFileName == null ) {
                ArrayDesign arrayDesign = locateArrayDesign( arrayDesignName );
                processAD( arrayDesign, this.fileName );
            } else {
                processBatchFile( this.batchFileName );
            }

        } catch ( Exception e ) {
            return e;
        }

        return null;
    }

    /**
     * @throws IOException process the current AD
     */
    protected void processAD( ArrayDesign arrayDesign, String fileName ) throws IOException {
        unlazifyArrayDesign( arrayDesign );

        Collection<CompositeSequence> compositeSequences = arrayDesign.getCompositeSequences();

        log.info( arrayDesignName + " has " + compositeSequences.size() + " composite sequences" );

        Writer writer = initOutputFile( fileName );

        int numProcessed = generateAnnotationFile( writer, compositeSequences );

        writer.flush();
        writer.close();

        log.info( "Finished processing platform: " + arrayDesignName );

        successObjects.add( ( ( Describable ) arrayDesign ).getName() + " ("
                + ( ( ArrayDesign ) arrayDesign ).getExternalReferences().iterator().next().getAccession() + ")" );

        if ( StringUtils.isBlank( fileName ) ) {
            log.info( "Processed " + numProcessed + " composite sequences" );
            audit( arrayDesign, "Processed " + numProcessed + " composite sequences" );
        } else {
            log.info( "Created file:  " + fileName + " with " + numProcessed + " values" );
            audit( arrayDesign, "Created file: " + fileName + " with " + numProcessed + " values" );
        }
    }

    /**
     * @param arrayDesign
     */
    private void audit( ArrayDesign arrayDesign, String note ) {
        AuditEventType eventType = ArrayDesignAnnotationFileEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( arrayDesign, eventType, note );
    }

    /**
     * @param fileName
     * @throws IOException used for batch processing
     */
    protected void processBatchFile( String fileName ) throws IOException {

        log.info( "Loading platforms to annotate from " + fileName );
        InputStream is = new FileInputStream( fileName );
        BufferedReader br = new BufferedReader( new InputStreamReader( is ) );

        String line = null;
        int lineNumber = 0;
        while ( ( line = br.readLine() ) != null ) {
            lineNumber++;
            if ( StringUtils.isBlank( line ) ) {
                continue;
            }

            String[] arguments = StringUtils.split( line, ',' );

            String gpl = arguments[0];
            String annotationFileName = arguments[1];
            String type = arguments[2];

            // Check the syntax of the given line
            if ( ( gpl == null ) || StringUtils.isBlank( gpl ) ) {
                log.warn( "Incorrect line format in Batch Annotation file: Line " + lineNumber
                        + "Platform is required: " + line );
                log.warn( "Unable to process that line. Skipping to next." );
                continue;
            }
            if ( ( annotationFileName == null ) || StringUtils.isBlank( annotationFileName ) ) {
                annotationFileName = gpl;
                log.warn( "No annotation file name specified on line: " + lineNumber
                        + " Using platform name as default annotation file name" );
            }
            if ( ( type == null ) || StringUtils.isBlank( type ) ) {
                type = SHORT;
                log.warn( "No type specifed for line: " + lineNumber + " Defaulting to short" );
            }

            // need to set these so processing ad works correctly (todo: make processtype take all 3 parameter)
            this.arrayDesignName = gpl;
            processType( type );
            ArrayDesign arrayDesign = locateArrayDesign( arrayDesignName );

            try {
                processAD( arrayDesign, annotationFileName );
            } catch ( Exception e ) {
                log.error( "**** Exception while processing " + arrayDesignName + ": " + e.getMessage() + " ********" );
                log.error( e, e );
                cacheException( e );
                errorObjects.add( arrayDesignName + ": " + e.getMessage() );
                continue;
            }

        }

        summarizeProcessing();

    }

    /**
     * Opens a file for writing anda adds the header.
     * 
     * @param fileName if Null, output will be written to standard output.
     * @throws IOException
     */
    protected Writer initOutputFile( String fileName ) throws IOException {

        Writer writer;
        if ( StringUtils.isBlank( fileName ) ) {
            log.info( "Output to stdout" );
            writer = new PrintWriter( System.out );
        } else {

            // write into file
            log.info( "Creating new annotation file " + fileName + " \n" );

            File f = new File( fileName + ".an.txt" );

            if ( f.exists() ) {
                log.warn( "Will overwrite existing file " + f );
                f.delete();
            }

            f.createNewFile();
            writer = new FileWriter( f );
        }

        writer.write( "Probe ID \t Gene \t Description \t GO Terms \n" );

        return writer;
    }

    /**
     * @param compositeSequences
     * @throws IOException Gets the file ready for printing
     */
    @SuppressWarnings("unchecked")
    protected int generateAnnotationFile( Writer writer, Collection<CompositeSequence> compositeSequences )
            throws IOException {

        int compositeSequencesProcessed = 0;

        for ( CompositeSequence sequence : compositeSequences ) {

            Collection<Gene> genes = compositeSequenceService.getGenes( sequence );

            ++compositeSequencesProcessed;

            if ( ( genes == null ) || ( genes.isEmpty() ) ) {
                writeAnnotationLine( writer, sequence.getName(), "", "", null );
                continue;
            }

            // actually the collection gotten back is a collection of proxies which causes issues. Need to reload the
            // genes from the db.
            Collection<Long> geneIds = new ArrayList<Long>();

            for ( Gene g : genes ) {
                geneIds.add( g.getId() );
            }

            genes = geneService.load( geneIds );

            String geneNames = null;
            String geneDescriptions = null;
            Collection<OntologyTerm> goTerms = new ArrayList<OntologyTerm>();

            // Might be mulitple genes for a given cs. Need to hash it into one.
            for ( Gene gene : genes ) {

                if ( gene == null ) continue;

                // Add PARs or predicted gene info to annotation file?
                if ( ( !includeGemmaGenes )
                        && ( ( gene instanceof ProbeAlignedRegion ) || ( gene instanceof PredictedGene ) ) ) {
                    log.debug( "Gene:  " + gene.getOfficialSymbol()
                            + "  not included in annotations because it is a probeAligedRegion or predictedGene" );
                    continue;
                }

                if ( log.isDebugEnabled() )
                    log.debug( "Adding gene: " + gene.getOfficialSymbol() + " of type: " + gene.getClass() );

                addGoTerms( goTerms, gene );
                geneNames = addGeneSymbol( geneNames, gene );
                geneDescriptions = addGeneName( geneDescriptions, gene );

            }

            writeAnnotationLine( writer, sequence.getName(), geneNames, geneDescriptions, goTerms );

            if ( compositeSequencesProcessed % 500 == 0 && log.isInfoEnabled() ) {
                log.info( "Processed " + compositeSequencesProcessed + "/" + compositeSequences.size()
                        + " compositeSequences " );
            }

        }
        return compositeSequencesProcessed;
    }

    private Collection<OntologyTerm> addGoTerms( Collection<OntologyTerm> goTerms, Gene gene ) {
        Collection<OntologyTerm> terms = getGoTerms( gene );
        goTerms.addAll( terms );
        return terms;
    }

    private String addGeneName( String geneDescriptions, Gene gene ) {
        if ( gene.getOfficialName() != null ) {
            if ( geneDescriptions == null )
                geneDescriptions = gene.getOfficialName();
            else
                geneDescriptions += "|" + gene.getOfficialName();
        }
        return geneDescriptions;
    }

    private String addGeneSymbol( String geneNames, Gene gene ) {
        if ( gene.getOfficialSymbol() != null ) {
            if ( geneNames == null )
                geneNames = gene.getOfficialSymbol();
            else
                geneNames += "|" + gene.getOfficialSymbol();
        }
        return geneNames;
    }

    /**
     * @param probeId
     * @param gene
     * @param description
     * @param goTerms
     * @throws IOException Adds one line at a time to the annotation file
     */
    protected void writeAnnotationLine( Writer writer, String probeId, String gene, String description,
            Collection<OntologyTerm> goTerms ) throws IOException {

        if ( log.isDebugEnabled() ) log.debug( "Generating line for annotation file  \n" );

        if ( gene == null ) gene = "";

        if ( description == null ) description = "";

        writer.write( probeId + "\t" + gene + "\t" + description + "\t" );

        if ( ( goTerms == null ) || goTerms.isEmpty() ) {
            writer.write( "\n" );
            writer.flush();
            return;
        }

        boolean wrote = false;

        for ( OntologyTerm oe : goTerms ) {

            if ( oe == null ) continue;

            if ( wrote )
                writer.write( "|" + GeneOntologyService.asRegularGoId( oe ) );
            else
                writer.write( GeneOntologyService.asRegularGoId( oe ) );

            wrote = true;

        }

        writer.write( "\n" );
        writer.flush();

    }

    /**
     * @param gene
     * @return the goTerms for a given gene, as configured
     */
    @SuppressWarnings("unchecked")
    protected Collection<OntologyTerm> getGoTerms( Gene gene ) {

        Collection<VocabCharacteristic> ontos = new HashSet<VocabCharacteristic>( gene2GoAssociationService
                .findByGene( gene ) );

        Collection<OntologyTerm> results = OntologyTools.getOntologyTerms( ontos );

        if ( ( ontos == null ) || ( ontos.size() == 0 ) ) return results;

        if ( this.shortAnnotations ) return results;

        if ( this.longAnnotations ) {
            Collection<OntologyTerm> oes = goService.getAllParents( results );
            results.addAll( oes );
        } else if ( this.biologicalProcessAnnotations ) {
            Collection<OntologyTerm> toRemove = new HashSet<OntologyTerm>();

            for ( OntologyTerm ont : results ) {
                if ( ( ont == null ) ) continue; // / shouldn't happen!

                if ( !goService.isBiologicalProcess( ont ) ) toRemove.add( ont );
            }

            for ( OntologyTerm toRemoveOnto : toRemove ) {
                results.remove( toRemoveOnto );
            }
        }

        return results;
    }

    /**
     * @param type Intilizes variables depending on they type for file that is needed
     */
    private void processType( String type ) {

        shortAnnotations = false;
        longAnnotations = false;
        biologicalProcessAnnotations = false;

        if ( type.equalsIgnoreCase( LONG ) )
            longAnnotations = true;
        else if ( type.equalsIgnoreCase( BIOPROCESS ) )
            biologicalProcessAnnotations = true;
        else
            // ( type.equalsIgnoreCase( SHORT ) )
            shortAnnotations = true;

    }

    /**
     * @param genesToInclude
     */
    private void processGenesIncluded( String genesToInclude ) {
        includeGemmaGenes = false;

        if ( genesToInclude.equalsIgnoreCase( "all" ) ) includeGemmaGenes = true;

    }

    @Override
    protected void processOptions() {
        super.processOptions();

        if ( this.hasOption( 'f' ) ) {
            this.fileName = this.getOptionValue( 'f' );
        }

        if ( this.hasOption( 't' ) ) {
            processType( this.getOptionValue( 't' ) );
        }

        if ( this.hasOption( 'l' ) ) {
            this.batchFileName = this.getOptionValue( 'l' );
        }

        if ( this.hasOption( 'g' ) ) processGenesIncluded( this.getOptionValue( 'g' ) );

        gene2GoAssociationService = ( Gene2GOAssociationService ) this.getBean( "gene2GOAssociationService" );

        compositeSequenceService = ( CompositeSequenceService ) this.getBean( "compositeSequenceService" );
        geneService = ( GeneService ) this.getBean( "geneService" );

        goService = ( GeneOntologyService ) this.getBean( "geneOntologyService" );

    }

}