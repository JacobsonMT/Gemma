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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.dataStructure.matrix.StringMatrix2DNamed;
import ubic.basecode.gui.ColorMap;
import ubic.basecode.gui.ColorMatrix;
import ubic.basecode.gui.JMatrixDisplay;
import ubic.basecode.io.reader.StringMatrixReader;
import ubic.gemma.analysis.coexpression.GeneCoExpressionAnalysis;
import ubic.gemma.model.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * "Global" coexpression analysis: for a set of coexpressed genes, get all the data for all the genes it is coexpressed
 * with and construct a summary matrix.
 * 
 * @author xwan
 * @version $Id$
 */
public class CoExpressionAnalysisCli extends AbstractSpringAwareCLI {

    private DesignElementDataVectorService dedvService;
    private ExpressionExperimentService eeService;
    private GeneService geneService;
    private String geneList = null;
    private String taxonName = null;
    private String outputFile = null;
	private int stringency = 3;
	private static String DIVIDOR = "-----";

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option geneFileOption = OptionBuilder.hasArg().isRequired().withArgName( "geneFile" ).withDescription(
                "Short names of the genes to analyze" ).withLongOpt( "geneFile" ).create( 'g' );
        addOption( geneFileOption );
        Option taxonOption = OptionBuilder.hasArg().isRequired().withArgName( "Taxon" ).withDescription(
                "the taxon of the genes to analyze" ).withLongOpt( "Taxon" ).create( 't' );
        addOption( taxonOption );
        Option outputFileOption = OptionBuilder.hasArg().isRequired().withArgName( "outFile" ).withDescription(
                "File for saving the corelation data" ).withLongOpt( "outFile" ).create( 'o' );
        addOption( outputFileOption );
        Option stringencyFileOption = OptionBuilder.hasArg().withArgName( "stringency" ).withDescription(
        "The stringency for the number of co-expression link(Default 3)" )
        .withLongOpt( "stringency" ).create( 's' );
        addOption( stringencyFileOption );
    }

    /**
     * 
     */
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'g' ) ) {
            this.geneList = getOptionValue( 'g' );
        }
        if ( hasOption( 't' ) ) {
            this.taxonName = getOptionValue( 't' );
        }
        if ( hasOption( 'o' ) ) {
            this.outputFile = getOptionValue( 'o' );
        }
        if ( hasOption( 's' ) ) {
            this.stringency = Integer.parseInt(getOptionValue( 's' ));
        }
        dedvService = ( DesignElementDataVectorService ) this.getBean( "designElementDataVectorService" );
        eeService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
        geneService = ( GeneService ) this.getBean( "geneService" );
    }

    /**
     * @param geneService
     * @param geneNames
     * @param taxon
     * @return
     */
    private Collection<Gene> getGenes( GeneService geneService, Object[] geneNames, Taxon taxon ) {
        HashSet<Gene> genes = new HashSet<Gene>();
        for ( int i = 0; i < geneNames.length; i++ ) {
            Gene gene = getGene( geneService, ( String ) geneNames[i], taxon );
            if ( gene != null ) genes.add( gene );
        }
        return genes;
    }

    /**
     * @param geneService
     * @param geneName
     * @param taxon
     * @return
     */
    private Gene getGene( GeneService geneService, String geneName, Taxon taxon ) {
        Gene gene = Gene.Factory.newInstance();
        gene.setOfficialSymbol( geneName.trim() );
        gene.setTaxon( taxon );
        gene = geneService.find( gene );
        if ( gene == null ) {
            log.info( "Can't Load gene " + geneName );
        }
        return gene;
    }

    /**
     * @param name
     * @return
     */
    private Taxon getTaxon( String name ) {
        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( name );
        TaxonService taxonService = ( TaxonService ) this.getBean( "taxonService" );
        taxon = taxonService.find( taxon );
        if ( taxon == null ) {
            log.info( "NO Taxon found!" );
        }
        return taxon;
    }
	
	Collection<Gene> getCoExpressedGenes(Collection<Gene> queryGenes){
		HashSet<Gene> coExpressedGenes = new HashSet<Gene>();
		Collection<Long> geneIds = new HashSet<Long>();
    	for(Gene gene:queryGenes){
    		log.info("Get co-expressed genes for " + gene.getName());
    		CoexpressionCollectionValueObject coexpressed = (CoexpressionCollectionValueObject)geneService.getCoexpressedGenes(gene, null, this.stringency);
    		Map<Long, Collection<Long>> geneEEMap = coexpressed.getGeneCoexpressionType().getSpecificExpressionExperiments();
    		for(Long geneId:geneEEMap.keySet()){
    			Collection<Long> ees = geneEEMap.get(geneId);
    			if(ees.size() >= this.stringency) geneIds.add(geneId);
    		}
     	}
    	log.info(" Got " + geneIds.size() + " genes for the CoExpression analysis");
    	if(geneIds.size() > 0)
    		coExpressedGenes.addAll(geneService.load(geneIds));
		return coExpressedGenes;
	}
    /**
     * Retrieve all the expression data for a bunch of genes in a bunch of expression experiments.
     * 
     * @param allEE
     * @param queryGenes
     * @return
     */
	Map<DesignElementDataVector, Collection<Gene>> getDedv2GenesMap(Collection<Gene> allGenes, Collection<ExpressionExperiment> allEEs){
    	Map<DesignElementDataVector, Collection<Gene>> dedv2genes = new HashMap<DesignElementDataVector, Collection<Gene>>();
    	int count = 0;
    	int CHUNK_LIMIT = 30;
    	int total = allGenes.size();
    	Collection<Gene> genesInOneChunk = new HashSet<Gene>();

    	log.info("Start the Query for "+ allGenes.size() + " genes");
        StopWatch qWatch = new StopWatch();
        qWatch.start();
    	for(Gene gene:allGenes){
    		genesInOneChunk.add(gene);
    		count++;
    		total--;
    		if(count == CHUNK_LIMIT || total == 0){
    			dedv2genes.putAll(dedvService.getVectors(allEEs, genesInOneChunk));
    			count = 0;
    			genesInOneChunk.clear();
    			System.out.print(".");
    		}
    	}
        qWatch.stop();
        log.info("\nQuery takes " + qWatch.getTime() + " to get " + dedv2genes.size() + " DEDVs for " + allGenes.size() + " genes");
        
        return dedv2genes;
	}

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "CoExpression Analysis", args );
        if ( err != null ) {
            return err;
        }

        Taxon taxon = getTaxon();
        Collection<ExpressionExperiment> allEE = eeService.findByTaxon( taxon );
		Collection<String> queryGeneNames = new HashSet<String>();
		Collection<String> coExpressedGeneNames = new HashSet<String>();
		boolean readingQueryGene = true;
		/*
		 * The gene input file could contain query genes and co-expressed genes divided by DIVIDOR;
		 * If user doesn't provide the co-expressed genes, then use the service to find the co-expressed genes in database.
		 */
		try {
			InputStream is = new FileInputStream( this.geneList );
			BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
			String shortName = null;
			while ( ( shortName = br.readLine() ) != null ) {
				if ( StringUtils.isBlank( shortName ) ) continue;
				if ( shortName.trim().contains( DIVIDOR ) ) {
					readingQueryGene = false;
					continue;
				}
				if ( readingQueryGene ) {
					queryGeneNames.add( shortName.trim() );
				} else {
					coExpressedGeneNames.add( shortName.trim() );
				}
			}
		} catch ( Exception e ) {
			return e;
		}

		if(queryGeneNames.size() == 0){
			log.info( "No gene is read from the input file" );
			return null;
		}
		Collection<Gene> queryGenes = this.getGenes(geneService, queryGeneNames.toArray(), taxon);
		if(queryGenes.size() == 0){
			log.info( "Can't load any of genes" + queryGeneNames );
			return null;
		}
		Collection<Gene> coExpressedGenes = null;
		if(coExpressedGeneNames.size() != 0){
			coExpressedGenes = this.getGenes(geneService, coExpressedGeneNames.toArray(), taxon);
		}else{			
			coExpressedGenes = this.getCoExpressedGenes(queryGenes);
		}
		Collection<Gene> allGenes = new HashSet<Gene>();
        allGenes.addAll(queryGenes);
        allGenes.addAll(coExpressedGenes);
        log.info( "Start the Query for " + queryGenes.size() + " genes" );
        Map<DesignElementDataVector, Collection<Gene>> dedv2genes = getDedv2GenesMap(allGenes, allEE);
        if(dedv2genes.size() == 0 || queryGenes.size() == 0 || coExpressedGenes.size() == 0 || allEE.size() == 0) return null;
        GeneCoExpressionAnalysis coExperssion = new GeneCoExpressionAnalysis( (Set)queryGenes, (Set)coExpressedGenes, (Set)new HashSet(allEE));

        coExperssion.setDedv2Genes(dedv2genes);
        coExperssion.setExpressionExperimentService( eeService );
        coExperssion.analysis( ( Set ) dedv2genes.keySet() );

        try {
            makeClusterGrams( coExperssion );
        } catch ( Exception e ) {
            return e;
        }

        return null;
    }

    /**
     * @return
     */
    private Taxon getTaxon() {
        Taxon taxon = getTaxon( this.taxonName );
        if ( taxon == null ) {
            log.error( "No taxon is found " + this.taxonName );
            bail( ErrorCode.INVALID_OPTION );
        }
        return taxon;
    }

    /**
     * @param coExpression
     * @throws FileNotFoundException
     * @throws IOException
     * @throws InterruptedException
     */
    private void makeClusterGrams( GeneCoExpressionAnalysis coExpression ) throws FileNotFoundException, IOException,
            InterruptedException {
        // Generate the data file for Cluster3
        PrintStream output = new PrintStream( new FileOutputStream( new File( this.outputFile ) ) );
        double presencePercent = 0.5;
        coExpression.output( output, presencePercent );
        output.close();

        // Running Cluster3 to geneate .cdt file
        Runtime rt = Runtime.getRuntime();
        Process clearOldFiles = rt.exec( "rm *.cdt -f" );
        clearOldFiles.waitFor();

        String clusterCmd = "cluster";
        String commonOptions = "-g 7 -e 7 -m c";
        Process cluster = rt.exec( clusterCmd + " -f " + this.outputFile + " " + commonOptions );
        cluster.waitFor();

        DoubleMatrixNamed dataMatrix = getClusteredMatrix();

        // Get the rank Matrix
        DoubleMatrixNamed rankMatrix = coExpression.getRankMatrix( dataMatrix );

        // generate the png figures
        ColorMatrix dataColorMatrix = new ColorMatrix( dataMatrix );
        dataColorMatrix.setColorMap( ColorMap.GREENRED_COLORMAP );
        ColorMatrix rankColorMatrix = new ColorMatrix( rankMatrix );
        rankColorMatrix.setColorMap( ColorMap.GREENRED_COLORMAP );

        JMatrixDisplay dataMatrixDisplay = new JMatrixDisplay( dataColorMatrix );
        JMatrixDisplay rankMatrixDisplay = new JMatrixDisplay( rankColorMatrix );

        dataMatrixDisplay.saveImage( "dataMatrix.png", true );
        rankMatrixDisplay.saveImage( "rankMatrix.png", true );
    }

    /**
     * @return
     * @throws IOException
     */
    private DoubleMatrixNamed getClusteredMatrix() throws IOException {
        // Read the generated file into a String Matrix
        StringMatrixReader mReader = new StringMatrixReader();
        int dotIndex = this.outputFile.lastIndexOf( '.' );
        String CDTMatrixFile = this.outputFile.substring( 0, dotIndex );
        StringMatrix2DNamed cdtMatrix = ( StringMatrix2DNamed ) mReader.read( CDTMatrixFile + ".cdt" );

        // Read String Matrix and convert into DenseDoubleMatrix
        int extra_rows = 2, extra_cols = 3;
        double[][] data = new double[cdtMatrix.rows() - extra_rows][];
        List<String> rowLabels = new ArrayList<String>();
        List<String> colLabels = new ArrayList<String>();

        List colNames = cdtMatrix.getColNames();
        for ( int i = extra_cols; i < colNames.size(); i++ )
            colLabels.add( ( String ) colNames.get( i ) );

        int rowIndex = 0;
        for ( int i = extra_rows; i < cdtMatrix.rows(); i++ ) {
            Object row[] = cdtMatrix.getRow( i );
            rowLabels.add( ( String ) row[0] );
            data[rowIndex] = new double[row.length - extra_cols];
            for ( int j = extra_cols; j < row.length; j++ )
                try {
                    data[rowIndex][j - extra_cols] = Double.valueOf( ( String ) row[j] );
                } catch ( Exception e ) {
                    data[rowIndex][j - extra_cols] = Double.NaN;
                    continue;
                }
            rowIndex++;
        }
        DoubleMatrixNamed dataMatrix = new DenseDoubleMatrix2DNamed( data );
        dataMatrix.setRowNames( rowLabels );
        dataMatrix.setColumnNames( colLabels );
        return dataMatrix;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        CoExpressionAnalysisCli analysis = new CoExpressionAnalysisCli();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Exception ex = analysis.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            watch.stop();
            log.info( watch.getTime() );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

}
