/*
 * The Gemma project
 *
 * Copyright (c) 2013 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.loader.association.phenotype;

import ubic.basecode.util.FileTools;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.model.genome.Gene;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class RgdDatabaseImporter extends ExternalDatabaseEvidenceImporterAbstractCLI {

    private static final String RGD_FILE_HUMAN = "homo_genes_rdo";

    private static final String RGD_FILE_MOUSE = "mus_genes_rdo";
    private static final String RGD_FILE_RAT = "rattus_genes_rdo";
    // path of files to download
    private static final String RGD_URL_PATH = "ftp://rgd.mcw.edu/pub/data_release/annotated_rgd_objects_by_ontology/";
    // name of the external database
    private static final String RGD = "RGD";

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public RgdDatabaseImporter( String[] args ) throws Exception {
        super( args );
    }

    public static void main( String[] args ) throws Exception {

        RgdDatabaseImporter importEvidence = new RgdDatabaseImporter( args );
        Exception e = importEvidence.doWork( args );
        if ( e != null ) {
            e.printStackTrace();
        }

    }

    @Override
    public String getCommandName() {
        return "rgdImport";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return null;
    }

    @Override
    protected void buildOptions() {
        super.buildOptions();
    }

    @Override
    protected Exception doWork( String[] args ) {

        try {
            // creates the folder where to place the file web downloaded files and final output files
            this.createWriteFolderWithDate( RgdDatabaseImporter.RGD );

            String rgdHuman = this
                    .downloadFileFromWeb( RgdDatabaseImporter.RGD_URL_PATH, RgdDatabaseImporter.RGD_FILE_HUMAN );
            String rgdMouse = this
                    .downloadFileFromWeb( RgdDatabaseImporter.RGD_URL_PATH, RgdDatabaseImporter.RGD_FILE_MOUSE );
            String rgdRat = this
                    .downloadFileFromWeb( RgdDatabaseImporter.RGD_URL_PATH, RgdDatabaseImporter.RGD_FILE_RAT );

            // find the OMIM and Mesh terms from the disease ontology file
            this.findOmimAndMeshMappingUsingOntologyFile();

            // process the rgd files
            this.processRGDFiles( rgdHuman, rgdMouse, rgdRat );
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public String getShortDesc() {
        return "Creates a .tsv file of lines of evidence from RGD, to be used with EvidenceImporterCLI.java to import into Phenocarta.";
    }

    @Override
    protected void processOptions() {
        super.processOptions();
    }

    private void processRGDFile( String taxon, String fileName ) throws Exception {

        BufferedReader br = new BufferedReader(
                new InputStreamReader( FileTools.getInputStreamFromPlainOrCompressedFile( fileName ) ) );

        String line;

        // reads the manual file and put the data in a structure
        while ( ( line = br.readLine() ) != null ) {
            if ( line.indexOf( '!' ) != -1 ) {
                continue;
            }

            String[] tokens = line.split( "\t" );

            String geneSymbol = this.removeSpecialSymbol( tokens[2] ).trim();

            String pubmed = ( tokens[5].substring( tokens[5].indexOf( "PMID:" ) + 5, tokens[5].length() ) ).trim();
            String evidenceCode = tokens[6].trim();
            String comment = tokens[3].trim();
            String databaseLink = "?term=" + tokens[4].trim() + "&id=" + tokens[1].trim();
            String meshOrOmimId = tokens[10].trim();

            if ( !evidenceCode.equalsIgnoreCase( "ISS" ) && !evidenceCode.equalsIgnoreCase( "NAS" ) && !evidenceCode
                    .equalsIgnoreCase( "IEA" ) && !meshOrOmimId.equals( "" ) && !pubmed.equals( "" ) ) {

                Gene gene = this.findGeneUsingSymbolandTaxon( geneSymbol, taxon );

                if ( gene != null ) {

                    this.findMapping( meshOrOmimId, gene, pubmed, evidenceCode, comment, null, RgdDatabaseImporter.RGD,
                            databaseLink );
                }
            }
        }
    }

    private void processRGDFiles( String rgdHuman, String rgdMouse, String rgdRat ) throws Exception {
        this.processRGDFile( "human", rgdHuman );
        this.processRGDFile( "mouse", rgdMouse );
        this.processRGDFile( "rat", rgdRat );
        this.writeBuffersAndCloseFiles();
    }

    private String removeSpecialSymbol( String geneId ) {
        int index1 = geneId.indexOf( "<sup>" );

        if ( index1 != -1 ) {
            return geneId.substring( 0, index1 );
        }
        return geneId;
    }
}