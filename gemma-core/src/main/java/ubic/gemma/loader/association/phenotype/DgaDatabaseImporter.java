package ubic.gemma.loader.association.phenotype;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.apps.GemmaCLI.CommandGroup;

/* this importer cannot automatically download files it expects the files to already be there */
/**
 * TODO Document Me
 *
 * @version $Id$
 */
public class DgaDatabaseImporter extends ExternalDatabaseEvidenceImporterAbstractCLI {

    // to find to file go to : http://dga.nubic.northwestern.edu/pages/download.php

    public static final String DGA_FILE_NAME = "IDMappings.rdf";

    // name of the external database
    private static final String DGA = "DGA";

    public static void main( String[] args ) throws Exception {
        DgaDatabaseImporter databaseImporter = new DgaDatabaseImporter( args );
        databaseImporter.doWork( args );
    }

    private HashMap<String, HashSet<OntologyTerm>> commonLines = new HashMap<String, HashSet<OntologyTerm>>();

    private File dgaFile = null;

    private HashSet<String> linesToExclude = new HashSet<String>();

    public DgaDatabaseImporter( String[] args ) throws Exception {
        super( args );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#getCommandName()
     */
    @Override
    public String getCommandName() {
        return "dgaImport";
    }

    /* this importer cannot automatically download files it expects the files to already be there */
    private void checkForDGAFile() throws Exception {

        writeFolder = WRITE_FOLDER + File.separator + DGA;

        File folder = new File( writeFolder );

        if ( !folder.exists() ) {
            throw new Exception( "cannot find the DGA Folder" + folder.getAbsolutePath() );
        }

        // file expected
        dgaFile = new File( writeFolder + File.separator + DGA_FILE_NAME );
        if ( !dgaFile.exists() ) {
            throw new Exception( "cannot find file: " + dgaFile.getAbsolutePath() );
        }
    }

    private int findHowManyParents( OntologyTerm o, int increment ) {

        if ( o.getParents( true ).size() != 0 ) {
            return findHowManyParents( o.getParents( true ).iterator().next(), ++increment );
        }

        return increment;
    }

    // find string between first > and <
    private String findStringBetweenSpecialCharacter( String line ) {
        String newLine = line.substring( line.indexOf( ">" ) + 1, line.length() );
        if ( newLine.indexOf( "<" ) != -1 ) {
            newLine = newLine.substring( 0, newLine.indexOf( "<" ) );
        }
        return newLine;
    }

    private String findStringBetweenSpecialCharacter( String line, String keyword ) throws Exception {

        if ( line.indexOf( keyword ) == -1 ) {
            throw new Exception( keyword + " not found in File ??? " + line );
        }

        return findStringBetweenSpecialCharacter( line );
    }

    // extra step to take out redundant terms, if a child term is more specific dont keep the parent, if 2 lines share
    // same pubmed, gene and gene RIF take the most specific uri
    // example: same pubed, same gene 1-leukemia 2-myeloid leukemia 3-acute myeloid leukemia, keep only 3-
    private void findTermsWithParents() throws NumberFormatException, IOException, Exception {

        try (BufferedReader dgaReader = new BufferedReader( new FileReader( dgaFile ) );) {
            String line = "";

            while ( ( line = dgaReader.readLine() ) != null ) {

                // found a term
                if ( line.indexOf( "DOID" ) != -1 ) {
                    // this being of the url could change make sure its still correct if something doesn't work
                    String valueUri = "http://purl.obolibrary.org/obo/DOID_"
                            + findStringBetweenSpecialCharacter( line );

                    String geneId = findStringBetweenSpecialCharacter( dgaReader.readLine(), "GeneID" );
                    String pubMedID = findStringBetweenSpecialCharacter( dgaReader.readLine(), "PubMedID" );
                    String geneRIF = findStringBetweenSpecialCharacter( dgaReader.readLine(), "GeneRIF" );

                    OntologyTerm o = findOntologyTermExistAndNotObsolote( valueUri );

                    if ( o != null ) {

                        String key = geneId + pubMedID + geneRIF;
                        HashSet<OntologyTerm> valuesUri = new HashSet<OntologyTerm>();

                        if ( commonLines.get( key ) != null ) {
                            valuesUri = commonLines.get( key );
                        }

                        valuesUri.add( o );

                        commonLines.put( key, valuesUri );
                    }
                }
            }
            dgaReader.close();
        }

        for ( String key : commonLines.keySet() ) {

            log.info( "Checking for lines that are ontology duplicated: " + key );

            HashSet<OntologyTerm> ontologyTerms = commonLines.get( key );

            HashSet<String> allUri = new HashSet<String>();

            for ( OntologyTerm o : ontologyTerms ) {
                allUri.add( o.getUri() );
            }

            for ( OntologyTerm o : ontologyTerms ) {

                // get all kids terms
                Collection<OntologyTerm> childrenOntology = o.getChildren( false );

                for ( OntologyTerm onChil : childrenOntology ) {

                    // then this line is a parent dont keep it there is more specific child term
                    if ( allUri.contains( onChil.getUri() ) ) {
                        // result of this method, set of lines to exclude in the checkForDGAFile() step :
                        linesToExclude.add( key + o.getUri() );
                    }
                }
            }
        }
    }

    private boolean lineToExclude( String key ) {

        if ( linesToExclude.contains( key ) ) {
            return true;
        }
        return false;
    }

    private void processDGAFile() throws Exception {

        initFinalOutputFile( false, true );

        try (BufferedReader dgaReader = new BufferedReader( new FileReader( dgaFile ) );) {
            String line = "";

            while ( ( line = dgaReader.readLine() ) != null ) {

                // found a term
                if ( line.indexOf( "DOID" ) != -1 ) {
                    // this being of the url could change make sure its still correct if something doesn't work
                    String valueUri = "http://purl.obolibrary.org/obo/DOID_"
                            + findStringBetweenSpecialCharacter( line );

                    String geneId = findStringBetweenSpecialCharacter( dgaReader.readLine(), "GeneID" );
                    String pubMedID = findStringBetweenSpecialCharacter( dgaReader.readLine(), "PubMedID" );
                    String geneRIF = findStringBetweenSpecialCharacter( dgaReader.readLine(), "GeneRIF" );

                    OntologyTerm o = findOntologyTermExistAndNotObsolote( valueUri );

                    if ( o != null ) {

                        String geneSymbol = geneToSymbol( new Integer( geneId ) );
                        // gene do exist
                        if ( geneSymbol != null ) {

                            String key = geneId + pubMedID + geneRIF + o.getUri();

                            // if deep >3 always keep
                            int howDeepIdTerm = findHowManyParents( o, 0 );

                            // keep leaf or deep enough or uri=DOID_162(cancer)
                            if ( !( ( o.getChildren( true ).size() != 0 && howDeepIdTerm < 2 )
                                    || o.getUri().indexOf( "DOID_162" ) != -1 ) ) {

                                // negative
                                if ( ( geneRIF.indexOf( " is not " ) != -1
                                        || geneRIF.indexOf( " not associated " ) != -1
                                        || geneRIF.indexOf( " no significant " ) != -1
                                        || geneRIF.indexOf( " no association " ) != -1
                                        || geneRIF.indexOf( " not significant " ) != -1
                                        || geneRIF.indexOf( " not expressed " ) != -1 )
                                        && geneRIF.indexOf( "is associated" ) == -1
                                        && geneRIF.indexOf( "is significant" ) == -1
                                        && geneRIF.indexOf( "is not only" ) == -1
                                        && geneRIF.indexOf( "is expressed" ) == -1 ) {

                                    if ( !lineToExclude( key ) ) {
                                        outFinalResults.write( geneSymbol + "\t" + geneId + "\t" + pubMedID + "\t"
                                                + "IEA" + "\t" + "GeneRIF: " + geneRIF + "\t" + DGA + "\t" + "" + "\t"
                                                + "" + "\t" + "" + "\t" + o.getUri() + "\t" + "1" + "\n" );
                                    }

                                }
                                // positive
                                else {
                                    if ( !lineToExclude( key ) ) {
                                        outFinalResults.write( geneSymbol + "\t" + geneId + "\t" + pubMedID + "\t"
                                                + "IEA" + "\t" + "GeneRIF: " + geneRIF + "\t" + DGA + "\t" + "" + "\t"
                                                + "" + "\t" + "" + "\t" + o.getUri() + "\t" + "" + "\n" );
                                    }
                                }

                                outFinalResults.flush();
                            }
                        } else {
                            log.info( "gene NCBI no found in Gemma discard this eidence: ncbi: " + geneId );
                        }

                    } else {
                        log.info( "Ontology term not found in Ontology or obsolete : " + valueUri
                                + " (normal that this happen sometimes)" );
                    }
                }
            }
            dgaReader.close();
            outFinalResults.close();
        }
    }

    @Override
    public CommandGroup getCommandGroup() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void buildOptions() {
        super.buildOptions();
    }

    @Override
    protected void processOptions() {
        super.processOptions();
    }

    @Override
    public String getShortDesc() {
        return "Creates a .tsv file of lines of evidence from DGA, to be used with EvidenceImporterCLI.java to import into Phenocarta.";
    }

    @Override
    protected Exception doWork( String[] args ) {

        try {
            checkForDGAFile();
            findTermsWithParents();
            processDGAFile();
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        return null;
    }
}