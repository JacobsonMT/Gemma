/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
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
package ubic.gemma.core.loader.expression;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.basecode.util.FileTools;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.util.Settings;
import ubic.gemma.core.util.TimeUtil;
import ubic.gemma.core.util.concurrent.GenericStreamConsumer;

/**
 * @author paul
 */
public class AffyPowerToolsProbesetSummarize {

    private static final long AFFY_UPDATE_INTERVAL_MS = 1000 * 30;

    /*
     * Current as of May 2012
     */
    private static final String h = "HuEx-1_0-st-v2.r2";

    /*
     * These are supplied by Affymetrix. Current as of May 2012
     */
    private static final String hg = "hg18";
    private static Log log = LogFactory.getLog( AffyPowerToolsProbesetSummarize.class );
    private static final String m = "MoEx-1_0-st-v1.r2";

    private static final String METHOD = "rma";

    private static final String mm = "mm9";
    private static final String r = "RaEx-1_0-st-v1.r2";
    private static final String rn = "rn4";

    /**
     * @return
     */
    public static QuantitationType makeAffyQuantitationType() {
        QuantitationType result = QuantitationType.Factory.newInstance();

        result.setGeneralType( GeneralType.QUANTITATIVE );
        result.setRepresentation( PrimitiveType.DOUBLE ); // no choice here
        result.setIsPreferred( Boolean.TRUE );
        result.setIsNormalized( Boolean.TRUE );
        result.setIsBackgroundSubtracted( Boolean.TRUE );
        result.setIsBackground( false );
        result.setName( METHOD + " value" );
        result.setDescription( "Computed in Gemma by apt-probeset-summarize" );
        result.setType( StandardQuantitationType.AMOUNT );
        result.setIsMaskedPreferred( false ); // this is raw data.
        result.setScale( ScaleType.LOG2 );
        result.setIsRatio( false );
        result.setIsRecomputedFromRawData( true );

        return result;
    }

    private QuantitationType quantitationType;

    /**
     * 
     */
    public AffyPowerToolsProbesetSummarize() {
        this.quantitationType = makeAffyQuantitationType();
    }

    /**
     * This constructor is used for multiplatform situations where the same QT must be used for each platform.
     * 
     * @param qt
     */
    public AffyPowerToolsProbesetSummarize( QuantitationType qt ) {
        this.quantitationType = qt;
    }

    /**
     * For either 3' or Exon arrays.
     * 
     * @param ee
     * @param aptOutputFileToRead
     * @param targetPlatform deal with data from this platform (call multiple times if there is more than one platform)
     * @return
     * @throws IOException
     * @throws FileNotFoundException
     */
    public Collection<RawExpressionDataVector> processData( ExpressionExperiment ee, String aptOutputFileToRead,
            ArrayDesign targetPlatform ) throws IOException, FileNotFoundException {

        log.info( "Parsing " + aptOutputFileToRead );

        try (InputStream is = new FileInputStream( aptOutputFileToRead )) {
            DoubleMatrix<String, String> matrix = parse( is );

            if ( matrix.rows() == 0 ) {
                throw new IllegalStateException( "Matrix from APT had no rows" );
            }
            if ( matrix.columns() == 0 ) {
                throw new IllegalStateException( "Matrix from APT had no columns" );
            }

            Collection<BioAssay> allBioAssays = ee.getBioAssays();

            Collection<BioAssay> bioAssaysToUse = new HashSet<>();
            for ( BioAssay bioAssay : allBioAssays ) {
                if ( bioAssay.getArrayDesignUsed().equals( targetPlatform ) ) {
                    bioAssaysToUse.add( bioAssay );
                }
            }

            if ( allBioAssays.size() > bioAssaysToUse.size() ) {
                log.info( "Using " + bioAssaysToUse.size() + "/" + allBioAssays.size() + " bioassays (those on " + targetPlatform.getShortName()
                        + ")" );
            }

            if ( matrix.columns() < bioAssaysToUse.size() ) {
                // having > is okay, there can be extra.
                throw new IllegalStateException(
                        "Matrix from APT had the wrong number of colummns: expected " + bioAssaysToUse.size() + ", got " + matrix.columns() );
            }

            log.info( "Read " + matrix.rows() + " x " + matrix.columns() + ", matching with " + bioAssaysToUse.size()
                    + " samples on " + targetPlatform );

            BioAssayDimension bad = BioAssayDimension.Factory.newInstance();
            bad.setName( "For " + ee.getShortName() + " on " + targetPlatform );
            bad.setDescription( "Generated from output of apt-probeset-summarize" );

            /*
             * Add them ...
             */

            Map<String, BioAssay> bmap = new HashMap<>();
            for ( BioAssay bioAssay : bioAssaysToUse ) {
                assert bioAssay.getArrayDesignUsed().equals( targetPlatform );
                if ( bmap.containsKey( bioAssay.getAccession().getAccession() )
                        || bmap.containsKey( bioAssay.getName() ) ) {
                    throw new IllegalStateException( "Duplicate" );
                }
                bmap.put( bioAssay.getAccession().getAccession(), bioAssay );
                bmap.put( bioAssay.getName(), bioAssay );
            }

            if ( log.isDebugEnabled() )
                log.debug( "Will match result data file columns to bioassays referred to by any of the following strings:\n"
                        + StringUtils.join( bmap.keySet(), "\n" ) );

            int found = 0;
            List<String> columnsToKeep = new ArrayList<>();
            for ( int i = 0; i < matrix.columns(); i++ ) {
                String columnName = matrix.getColName( i );

                String sampleName = columnName.replaceAll( ".(CEL|cel)$", "" );

                /*
                 * Look for patterns like GSM476194_SK_09-BALBcJ_622.CEL
                 */
                BioAssay assay = null;
                if ( sampleName.matches( "^GSM[0-9]+_.+" ) ) {
                    String geoAcc = sampleName.split( "_" )[0];

                    log.info( "Found column for " + geoAcc );
                    if ( bmap.containsKey( geoAcc ) ) {
                        assay = bmap.get( geoAcc );
                    } else {
                        log.warn( "No bioassay for " + geoAcc );
                    }
                } else {

                    /*
                     * Sometimes column names are like Aud_19L.CEL or
                     */
                    assay = bmap.get( sampleName );
                }

                if ( assay == null ) {
                    /*
                     * This is okay, if we have extras
                     */
                    if ( matrix.columns() == bioAssaysToUse.size() ) {
                        throw new IllegalStateException( "No bioassay could be matched to CEL file identified by "
                                + sampleName );
                    }
                    log.warn( "No bioassay for " + sampleName );
                    continue;
                }

                log.info( "Matching CEL sample " + sampleName + " to bioassay " + assay + " ["
                        + assay.getAccession().getAccession() + "]" );

                columnsToKeep.add( columnName );
                assert assay.getArrayDesignUsed().equals( targetPlatform );
                bad.getBioAssays().add( assay );
                found++;
            }

            if ( found != bioAssaysToUse.size() ) {
                throw new IllegalStateException( "Failed to find a data column for every bioassay on the given platform " + targetPlatform );
            }

            if ( columnsToKeep.size() < matrix.columns() ) {
                matrix = matrix.subsetColumns( columnsToKeep );
            }

            if ( quantitationType == null ) {
                quantitationType = makeAffyQuantitationType();
            }
            return convertDesignElementDataVectors( ee, bad, targetPlatform, matrix );
        }
    }

    /**
     * @param ee
     * @param targetPlatform target platform; call multiple times if there is more than one platform (though that should
     *        not happen for exon arrays)
     * @param files list of CEL files (any other files included will be ignored)
     * 
     * @return
     */
    public Collection<RawExpressionDataVector> processExonArrayData( ExpressionExperiment ee,
            ArrayDesign targetPlatform, Collection<LocalFile> files ) {

        Collection<BioAssay> bioAssays = ee.getBioAssays();

        if ( bioAssays.isEmpty() ) {
            throw new IllegalArgumentException( "Experiment had no assays" );
        }

        if ( targetPlatform.getCompositeSequences().isEmpty() ) {
            throw new IllegalArgumentException( "Target design had no elements" );
        }

        List<String> celfiles = getCelFiles( files, null );
        log.info( celfiles.size() + " cel files" );

        String outputPath = getOutputFilePath( ee, "apt-output" );

        String cmd = getCommand( targetPlatform, celfiles, outputPath );

        log.info( "Running: " + cmd );

        int exitVal = Integer.MIN_VALUE;

        StopWatch overallWatch = new StopWatch();
        overallWatch.start();
        try {
            final Process run = Runtime.getRuntime().exec( cmd );
            GenericStreamConsumer gscErr = new GenericStreamConsumer( run.getErrorStream() );
            GenericStreamConsumer gscIn = new GenericStreamConsumer( run.getInputStream() );
            gscErr.start();
            gscIn.start();

            while ( exitVal == Integer.MIN_VALUE ) {
                try {
                    exitVal = run.exitValue();
                } catch ( IllegalThreadStateException e ) {
                    // okay, still
                    // waiting.
                }
                Thread.sleep( AFFY_UPDATE_INTERVAL_MS );

                synchronized ( outputPath ) {
                    File outputFile = new File( outputPath + File.separator + "apt-probeset-summarize.log" );
                    Long size = outputFile.length();

                    String minutes = TimeUtil.getMinutesElapsed( overallWatch );
                    log.info( String.format( "apt-probeset-summarize logging output so far: %.2f", size / 1024.0 )
                            + " kb (" + minutes + " minutes elapsed)" );
                }
            }

            overallWatch.stop();
            String minutes = TimeUtil.getMinutesElapsed( overallWatch );
            log.info( "apt-probeset-summarize took a total of " + minutes + " minutes" );

            return processData( ee, outputPath + File.separator + METHOD + ".summary.txt", targetPlatform );

        } catch ( InterruptedException e ) {
            throw new RuntimeException( e );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * Call once for each platform used by the experiment.
     * 
     * @param ee
     * @param cdfFileName e.g. HG_U95Av2.CDF. Path configured by - can be null, we will try to guess (?)
     * @param targetPlatform to match the CDF file
     * @param files
     * @return
     */
    public Collection<RawExpressionDataVector> processThreeprimeArrayData( ExpressionExperiment ee, String cdfFileName,
            ArrayDesign targetPlatform, Collection<LocalFile> files ) {

        Collection<BioAssay> bioAssays = ee.getBioAssays();

        if ( bioAssays.isEmpty() ) {
            throw new IllegalArgumentException( "Experiment had no assays" );
        }

        if ( targetPlatform.getCompositeSequences().isEmpty() ) {
            throw new IllegalArgumentException( "Target design had no elements" );
        }

        /*
         * we may have multiple platforms; we need to get only the bioassays of interest.
         */
        Collection<String> accessionsOfInterest = new HashSet<>();
        for ( BioAssay ba : ee.getBioAssays() ) {
            if ( ba.getArrayDesignUsed().equals( targetPlatform ) ) {
                accessionsOfInterest.add( ba.getAccession().getAccession() );
            }
        }

        List<String> celfiles = getCelFiles( files, accessionsOfInterest );

        log.info( "Located " + celfiles.size() + " cel files" );

        String outputPath = getOutputFilePath( ee, "apt-output" );

        String cmd = getThreePrimeSummarizationCommand( targetPlatform, cdfFileName, celfiles, outputPath );

        log.info( "Running: " + cmd );

        int exitVal = Integer.MIN_VALUE;

        StopWatch overallWatch = new StopWatch();
        overallWatch.start();
        try {
            final Process run = Runtime.getRuntime().exec( cmd );
            GenericStreamConsumer gscErr = new GenericStreamConsumer( run.getErrorStream() );
            GenericStreamConsumer gscIn = new GenericStreamConsumer( run.getInputStream() );
            gscErr.start();
            gscIn.start();

            while ( exitVal == Integer.MIN_VALUE ) {
                try {
                    exitVal = run.exitValue();
                } catch ( IllegalThreadStateException e ) {
                    // okay, still
                    // waiting.
                }
                Thread.sleep( AFFY_UPDATE_INTERVAL_MS );

                synchronized ( outputPath ) {
                    File outputFile = new File( outputPath + File.separator + "apt-probeset-summarize.log" );
                    Long size = outputFile.length();

                    String minutes = TimeUtil.getMinutesElapsed( overallWatch );
                    log.info( String.format( "apt-probeset-summarize logging output so far: %.2f", size / 1024.0 )
                            + " kb (" + minutes + " minutes elapsed)" );
                }
            }

            overallWatch.stop();
            String minutes = TimeUtil.getMinutesElapsed( overallWatch );
            log.info( "apt-probeset-summarize took a total of " + minutes + " minutes" );

            return processData( ee, outputPath + File.separator + METHOD + ".summary.txt", targetPlatform );

        } catch ( InterruptedException e ) {
            throw new RuntimeException( e );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * @param f
     */
    private void checkFileReadable( String f ) {
        if ( !new File( f ).canRead() ) {
            throw new IllegalArgumentException( f + " could not be read" );
        }
    }

    /**
     * Stolen from SimpleExpressionDataLoaderService
     * 
     * @param expressionExperiment
     * @param bioAssayDimension
     * @param arrayDesign target design
     * @param matrix
     * @return Collection<DesignElementDataVector>
     */
    private Collection<RawExpressionDataVector> convertDesignElementDataVectors(
            ExpressionExperiment expressionExperiment, BioAssayDimension bioAssayDimension, ArrayDesign arrayDesign,
            DoubleMatrix<String, String> matrix ) {
        ByteArrayConverter bArrayConverter = new ByteArrayConverter();

        Collection<RawExpressionDataVector> vectors = new HashSet<>();

        Map<String, CompositeSequence> csMap = new HashMap<>();
        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            csMap.put( cs.getName(), cs );
        }

        for ( int i = 0; i < matrix.rows(); i++ ) {
            byte[] bdata = bArrayConverter.doubleArrayToBytes( matrix.getRow( i ) );

            RawExpressionDataVector vector = RawExpressionDataVector.Factory.newInstance();
            vector.setData( bdata );

            CompositeSequence cs = csMap.get( matrix.getRowName( i ) );
            if ( cs == null ) {
                continue;
            }
            vector.setDesignElement( cs );
            vector.setQuantitationType( this.quantitationType );
            vector.setExpressionExperiment( expressionExperiment );
            vector.setBioAssayDimension( bioAssayDimension );
            vectors.add( vector );

        }
        log.info( "Setup " + vectors.size() + " data vectors for " + matrix.rows() + " results from APT" );
        return vectors;
    }

    /**
     * @param files
     * @param accessionsOfInterest Used for multiplatform studies; if null, ignored
     * @return
     */
    private List<String> getCelFiles( Collection<LocalFile> files, Collection<String> accessionsOfInterest ) {

        Set<String> celfiles = new HashSet<>();
        for ( LocalFile f : files ) {
            try {
                File fi = new File( f.getLocalURL().toURI() );

                // FIXME if both unpacked and packed files are there, it looks at both of them. No major problem - the dups are resolved - just a little ugly.
                if ( fi.canRead()
                        && ( fi.getName().toUpperCase().endsWith( ".CEL" ) || fi.getName().toUpperCase()
                                .endsWith( ".CEL.GZ" ) ) ) {

                    if ( accessionsOfInterest != null ) {
                        String acc = fi.getName().replaceAll( "(GSM[0-9]+).+", "$1" );
                        if ( !accessionsOfInterest.contains( acc ) ) {
                            continue;
                        }
                    }

                    if ( FileTools.isGZipped( fi.getName() ) ) {
                        log.info( "Found CEL file " + fi + ", unzipping" );
                        try {
                            String unGzipFile = FileTools.unGzipFile( fi.getAbsolutePath() );
                            celfiles.add( unGzipFile );
                        } catch ( IOException e ) {
                            throw new RuntimeException( e );
                        }
                    } else {
                        log.info( "Found CEL file " + fi );
                        celfiles.add( fi.getAbsolutePath() );
                    }
                }
            } catch ( URISyntaxException e ) {
                throw new RuntimeException( e );
            }
        }

        if ( celfiles.isEmpty() ) {
            throw new IllegalArgumentException( "No valid CEL files were found" );
        }
        return new ArrayList<>( celfiles );
    }

    /**
     * For exon arrays. Like
     * 
     * <pre>
     * apt-probeset-summarize -a rma -p HuEx-1_0-st-v2.r2.pgf -c HuEx-1_0-st-v2.r2.clf -m
     * HuEx-1_0-st-v2.r2.dt1.hg18.core.mps -qc-probesets HuEx-1_0-st-v2.r2.qcc -o GSE13344.genelevel.data
     * /bigscratch/GSE13344/*.CEL
     * </pre>
     * 
     * http://media.affymetrix.com/support/developer/powertools/changelog/apt-probeset-summarize.html
     * http://bib.oxfordjournals.org/content/early/2011/04/15/bib.bbq086.full
     * 
     * @param ad
     * @param celfiles
     * @param outputPath directory
     * @return
     */
    private String getCommand( ArrayDesign ad, List<String> celfiles, String outputPath ) {
        /*
         * Get the pgf, clf, mps file for this platform. qc probesets: optional.
         */
        String toolPath = Settings.getString( "affy.power.tools.exec" );
        String refPath = Settings.getString( "affy.power.tools.ref.path" );

        checkFileReadable( toolPath );

        if ( !new File( refPath ).isDirectory() ) {
            throw new IllegalStateException( refPath + " is not a valid directory" );
        }

        Taxon primaryTaxon = ad.getPrimaryTaxon();

        String base = h;
        String genome = hg;
        if ( primaryTaxon.getCommonName().equals( "human" ) ) {
            base = h;
            genome = hg;
        } else if ( primaryTaxon.getCommonName().equals( "mouse" ) ) {
            base = m;
            genome = mm;
        } else if ( primaryTaxon.getCommonName().equals( "rat" ) ) {
            base = r;
            genome = rn;
        } else {
            throw new IllegalArgumentException( "Cannot use " + primaryTaxon );
        }

        String pgf = refPath + File.separator + base + ".pgf";
        String clf = refPath + File.separator + base + ".clf";
        String mps = refPath + File.separator + base + ".dt1." + genome + ".core.mps";
        String qcc = refPath + File.separator + base + ".qcc";

        checkFileReadable( pgf );
        checkFileReadable( clf );
        checkFileReadable( mps );
        checkFileReadable( qcc );

        String cmd = toolPath + " -a " + METHOD + " -p " + pgf + " -c " + clf + " -m " + mps + " -o " + outputPath
                + " --qc-probesets " + qcc + " " + StringUtils.join( celfiles, " " );
        return cmd;
    }

    /**
     * @param ee
     * @param base
     * @return
     */
    private String getOutputFilePath( ExpressionExperiment ee, String base ) {
        File tmpdir = new File( Settings.getDownloadPath() );
        return tmpdir + File.separator + ee.getId() + "_" + RandomStringUtils.randomAlphanumeric( 4 ) + "_" + base;
    }

    /**
     * For 3' arrays. Run RMA with quantile normalization.
     * 
     * <pre>
     * apt-probeset-summarize -a rma  -d HG-U133A_2.cdf -o GSE123.genelevel.data
     * /bigscratch/GSE123/*.CEL
     * </pre>
     * 
     * @param targetPlatform
     * @param cdfFileName e g. HG-U133A_2.cdf
     * @param celfiles
     * @param outputPath
     * @return
     */
    private String getThreePrimeSummarizationCommand( ArrayDesign targetPlatform, String cdfFileName,
            List<String> celfiles, String outputPath ) {
        String toolPath = Settings.getString( "affy.power.tools.exec" );

        /*
         * locate the CDF file
         */
        String cdfPath = Settings.getString( "affy.power.tools.cdf.path" );
        String cdfName;
        if ( cdfFileName != null ) {
            cdfName = cdfFileName;
        } else {
            String shortName = targetPlatform.getShortName();
            // probably won't work ...
            cdfName = shortName + ".cdf";
        }
        String cdfFullPath = null;
        if ( !cdfName.contains( cdfPath ) ) {
            cdfFullPath = cdfPath + File.separator + cdfName; // might be .cdf or .cdf.gz
        } else {
            cdfFullPath = cdfName;
        }
        checkFileReadable( cdfFullPath );

        /*
         * HG_U95C.CDF.gz, Mouse430_2.cdf.gz etc.
         */

        String cmd = toolPath + " -a " + METHOD + " -d " + cdfFullPath + " -o " + outputPath + " "
                + StringUtils.join( celfiles, " " );
        return cmd;
    }

    /**
     * @param data
     * @return
     * @throws IOException
     */
    private DoubleMatrix<String, String> parse( InputStream data ) throws IOException {
        DoubleMatrixReader reader = new DoubleMatrixReader();
        return reader.read( data );
    }

}
