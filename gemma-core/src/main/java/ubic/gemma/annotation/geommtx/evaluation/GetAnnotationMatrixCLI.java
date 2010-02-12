package ubic.gemma.annotation.geommtx.evaluation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ubic.GEOMMTx.ParentFinder;
import ubic.GEOMMTx.evaluation.MakeHistogramData;
import ubic.basecode.dataStructure.StringToStringSetMap;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * A class to extract Gemma experiment annotations and write them to a file in R data format.
 * 
 * @author leon
 */
public class GetAnnotationMatrixCLI extends AbstractSpringAwareCLI {

    @Override
    protected void buildOptions() {
        // TODO Auto-generated method stub
        // options for:
        // * manual vrs automatic
        // * parents
        // * taxon
        // * ontology
        // * fileoutput name
        // * URI vrs label
        // * experiment tilte vrs shortname

    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "GEOMMTx ", args );
        if ( err != null ) return err;

        StringToStringSetMap result = new StringToStringSetMap();
        StringToStringSetMap URIresult = new StringToStringSetMap();

        ExpressionExperimentService ees = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
        Collection<ExpressionExperiment> experiments = ees.loadAll();
        ParentFinder parentFinder = new ParentFinder();
        try {
            parentFinder.init();
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        int c = 0;
        for ( ExpressionExperiment experiment : experiments ) {
            c++;
            log.info( "Experiment number:" + c + " of " + experiments.size() + " ID:" + experiment.getId()
                    + " Shortname:" + experiment.getShortName() );

            ees.thawLite( experiment );

            Collection<Characteristic> characters = experiment.getCharacteristics();

            Set<String> currentLabelSet = new HashSet<String>();
            Set<String> currentURISet = new HashSet<String>();

            result.put( experiment.getShortName() + "", currentLabelSet );

            URIresult.put( experiment.getShortName() + "", currentURISet );

            for ( Characteristic ch : characters ) {
                if ( ch instanceof VocabCharacteristic ) {
                    VocabCharacteristic vc = ( VocabCharacteristic ) ch;
                    // log.info( vc.getValue() + " " + vc.getValueUri() );
                    if ( vc.getValueUri() != null && vc.getValueUri().startsWith( "http" ) ) {
                        currentLabelSet.add( vc.getValue() );
                        currentURISet.add( vc.getValueUri() );

                        // get parents
                        Set<String> parentURLs = parentFinder.allParents( vc.getValueUri() );
                        for ( String parentURI : parentURLs ) {
                            currentURISet.add( parentURI );
                            if ( parentFinder.getTerm( parentURI ) != null ) {
                                String parentLabel = parentFinder.getTerm( parentURI ).getLabel();
                                // log.info( vc.getValue() + "->" + parentLabel );
                                if ( parentLabel != null ) currentLabelSet.add( parentLabel );
                            }
                        }
                    } else {
                        log.info( "NON-URI:" + vc.getValue() + " " + vc.getValueUri() );
                    }

                }
            }
        }

        DoubleMatrix<String, String> resultLabelMatrix = StringToStringSetMap.setMapToMatrix( result );
        DoubleMatrix<String, String> resultURIMatrix = StringToStringSetMap.setMapToMatrix( URIresult );

        try {
            MakeHistogramData.writeRTable( "/grp/java/workspace/GEOMMTxRefactor/ForRaymond.txt", resultLabelMatrix );
            MakeHistogramData.writeRTable( "/grp/java/workspace/GEOMMTxRefactor/ForRaymondURI.txt", resultURIMatrix );
        } catch ( Exception e ) {
            e.printStackTrace();
            System.exit( 1 );
        }

        return null;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        GetAnnotationMatrixCLI p = new GetAnnotationMatrixCLI();

        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

}
