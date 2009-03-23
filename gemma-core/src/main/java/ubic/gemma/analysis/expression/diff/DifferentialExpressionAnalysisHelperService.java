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
package ubic.gemma.analysis.expression.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * A helper class for the differential expression analyzers. This class contains helper methods commonly needed when
 * performing an analysis.
 * 
 * @spring.bean id="differentialExpressionAnalysisHelperService"
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalysisHelperService {

    private static Log log = LogFactory.getLog( DifferentialExpressionAnalysisHelperService.class );

    /**
     * Returns a List of all the different types of biomaterials across all bioassays in the experiment. If there is
     * more than one biomaterial per bioassay, a {@link RuntimeException} is thrown.
     * 
     * @param expressionExperiment
     * @return
     * @throws Exception
     */
    public static List<BioMaterial> getBioMaterialsForBioAssays( ExpressionDataMatrix<?> matrix ) {

        List<BioMaterial> biomaterials = new ArrayList<BioMaterial>();

        Collection<BioAssay> assays = new ArrayList<BioAssay>();
        for ( int i = 0; i < matrix.columns(); i++ ) {
            Collection<BioAssay> bioassays = matrix.getBioAssaysForColumn( i );
            /*
             * Note: we could use addAll here. The mapping of bioassays to biomaterials is many-to-one (e.g., when the
             * HGU133A and B arrays were both used on the same set of samples).
             */
            assays.add( bioassays.iterator().next() );
        }

        for ( BioAssay assay : assays ) {
            Collection<BioMaterial> materials = assay.getSamplesUsed();
            if ( materials.size() != 1 ) {
                throw new RuntimeException( "Invalid number of biomaterials. Expecting 1 biomaterial/bioassay, got "
                        + materials.size() + "." );
            }

            biomaterials.addAll( materials );

        }

        return biomaterials;
    }

    /**
     * Returns the factors that can be used by R for a one way anova. This can also be used for t-tests. There
     * requirement here is that there is only one factor value per biomaterial, and all factor values are from the same
     * experimental factor.
     * <p>
     * FIXME use the ExperimentalFactor as the input, not the FactorValues.
     * 
     * @param factorValues
     * @param samplesUsed
     * @return list of strings representing the factor, in the same order as the supplied samplesUsed.
     */
    public static List<String> getRFactorsFromFactorValuesForOneWayAnova( Collection<FactorValue> factorValues,
            List<BioMaterial> samplesUsed ) {

        List<String> rFactors = new ArrayList<String>();

        for ( BioMaterial sampleUsed : samplesUsed ) {
            Collection<FactorValue> factorValuesFromBioMaterial = sampleUsed.getFactorValues();

            if ( factorValuesFromBioMaterial.size() != 1 ) {
                throw new RuntimeException( "Only supports 1 factor value per biomaterial." );
            }

            FactorValue fv = factorValuesFromBioMaterial.iterator().next();

            for ( FactorValue f : factorValues ) {
                if ( f.equals( fv ) ) {
                    rFactors.add( fv.getId().toString() );
                    break;
                }
            }
        }
        return rFactors;
    }

    /**
     * Returns the factors that can be used by R for a two way anova. Each sample must have a factor value equal to one
     * of the supplied factor values. This assumes that "equals" works correctly on the factor values.
     * 
     * @param experimentalFactor
     * @param samplesUsed the samples we want to assign to the various factors
     * @return R factor representation, in the same order as the given samplesUsed.
     */
    public static List<String> getRFactorsFromFactorValuesForTwoWayAnova( ExperimentalFactor experimentalFactor,
            List<BioMaterial> samplesUsed ) {

        List<String> rFactors = new ArrayList<String>();

        for ( BioMaterial sampleUsed : samplesUsed ) {
            Collection<FactorValue> factorValuesFromBioMaterial = sampleUsed.getFactorValues();
            boolean match = false;

            for ( FactorValue factorValue : factorValuesFromBioMaterial ) {
                for ( FactorValue candidateMatch : experimentalFactor.getFactorValues() ) {
                    if ( candidateMatch.equals( factorValue ) ) {
                        rFactors.add( factorValue.getId().toString() );
                        match = true;
                        break;
                    }
                }
            }
            if ( !match )
                throw new IllegalStateException(
                        "None of the Factor values of the biomaterial match the supplied factor values." );
        }

        return rFactors;
    }

    /**
     * Generates all possible factor value pairings for the given experimental factors.
     * 
     * @param experimentalFactors
     * @return A collection of hashsets, where each hashset is a pairing.
     */
    protected static Collection<Set<FactorValue>> generateFactorValuePairings(
            Collection<ExperimentalFactor> experimentalFactors ) {
        /* set up the possible pairings */
        Collection<FactorValue> allFactorValues = new HashSet<FactorValue>();
        for ( ExperimentalFactor experimentalFactor : experimentalFactors ) {
            allFactorValues.addAll( experimentalFactor.getFactorValues() );
        }

        Collection<FactorValue> allFactorValuesCopy = allFactorValues;

        Collection<Set<FactorValue>> factorValuePairings = new HashSet<Set<FactorValue>>();

        for ( FactorValue factorValue : allFactorValues ) {
            for ( FactorValue f : allFactorValuesCopy ) {
                if ( f.getExperimentalFactor().equals( factorValue.getExperimentalFactor() ) ) continue;

                HashSet<FactorValue> factorValuePairing = new HashSet<FactorValue>();
                factorValuePairing.add( factorValue );
                factorValuePairing.add( f );

                if ( !factorValuePairings.contains( factorValuePairing ) ) {
                    factorValuePairings.add( factorValuePairing );
                }
            }
        }
        return factorValuePairings;
    }

    /**
     * Returns true if the block design is complete and there are at least 2 biological replicates for each "group",
     * false otherwise.
     * 
     * @param expressionExperiment
     * @return boolean
     */
    public boolean blockComplete( ExpressionExperiment expressionExperiment ) {

        boolean completeBlock = checkBlockDesign( expressionExperiment );
        boolean hasAllReps = checkBiologicalReplicates( expressionExperiment, expressionExperiment
                .getExperimentalDesign().getExperimentalFactors() );

        return completeBlock && hasAllReps;
    }

    /**
     * Returns true if the block design is complete and there are at least 2 biological replicates for each "group",
     * false otherwise. When determining completeness, a biomaterial's factor values are only considered if they are
     * equivalent to one of the input experimental factors.
     * 
     * @param expressionExperiment
     * @param factors to consider completeness for.
     * @return boolean
     */
    public boolean blockComplete( ExpressionExperiment expressionExperiment, Collection<ExperimentalFactor> factors ) {

        Collection<BioMaterial> biomaterials = getBioMaterials( expressionExperiment );

        /*
         * Get biomaterials with only those factor values equal to the factor values in the input factors. Only these
         * factor values in each biomaterial will be used to determine completeness.
         */
        Collection<BioMaterial> biomaterialsWithGivenFactorValues = filterFactorValuesFromBiomaterials( factors,
                biomaterials );

        boolean completeBlock = checkBlockDesign( biomaterialsWithGivenFactorValues, factors );

        boolean hasAllReps = checkBiologicalReplicates( expressionExperiment, factors );

        return completeBlock && hasAllReps;
    }

    /**
     * Returns biomaterials with 'filtered' factor values. That is, each biomaterial will only contain those factor
     * values equivalent to a factor value from one of the input experimental factors.
     * 
     * @param factors
     * @param biomaterials
     * @return Collection<BioMaterial>
     */
    private Collection<BioMaterial> filterFactorValuesFromBiomaterials( Collection<ExperimentalFactor> factors,
            Collection<BioMaterial> biomaterials ) {
        Collection<FactorValue> allFactorValuesFromGivenFactors = new HashSet<FactorValue>();
        for ( ExperimentalFactor ef : factors ) {
            allFactorValuesFromGivenFactors.addAll( ef.getFactorValues() );
        }

        Collection<BioMaterial> biomaterialsWithGivenFactorValues = new HashSet<BioMaterial>();
        for ( BioMaterial b : biomaterials ) {
            Collection<FactorValue> biomaterialFactorValues = b.getFactorValues();
            Collection<FactorValue> factorValuesToConsider = new HashSet<FactorValue>();
            factorValuesToConsider.addAll( biomaterialFactorValues );
            for ( FactorValue biomaterialFactorValue : biomaterialFactorValues ) {
                if ( !allFactorValuesFromGivenFactors.contains( biomaterialFactorValue ) ) {
                    factorValuesToConsider.remove( biomaterialFactorValue );
                }
            }
            b.setFactorValues( factorValuesToConsider );
            biomaterialsWithGivenFactorValues.add( b );
        }

        return biomaterialsWithGivenFactorValues;
    }

    /**
     * See if there are at least two samples for each factor value combination.
     * 
     * @param expressionExperiment
     * @param factors
     * @return
     */
    protected boolean checkBiologicalReplicates( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> factors ) {

        Collection<BioMaterial> biomaterials = getBioMaterials( expressionExperiment );

        for ( BioMaterial firstBm : biomaterials ) {

            Collection<FactorValue> factorValuesToCheck = getRelevantFactorValues( factors, firstBm );

            boolean match = false;
            for ( BioMaterial secondBm : biomaterials ) {

                if ( firstBm.equals( secondBm ) ) continue;

                Collection<FactorValue> factorValuesToCompareTo = getRelevantFactorValues( factors, secondBm );

                if ( factorValuesToCheck.size() == factorValuesToCompareTo.size()
                        && factorValuesToCheck.containsAll( factorValuesToCompareTo ) ) {
                    log.debug( "Replicate found for biomaterial " + firstBm + "." );
                    match = true;
                    break;
                }
            }
            if ( !match ) {
                log.warn( "No replicate found for biomaterial " + firstBm + "." );
                return false;
            }
        }

        return true;

    }

    /**
     * Returns true if all of the following conditions hold true: each biomaterial has more than 2 factor values, each
     * biomaterial has a factor value from one of the input factors paired with a factor value from the other input
     * factors, and all factor values from 1 factor have been paired with all factor values from the other factors,
     * across all biomaterials.
     * 
     * @param biomaterials
     * @param factorValues
     * @return false if not a complete block design.
     */
    protected boolean checkBlockDesign( Collection<BioMaterial> biomaterials,
            Collection<ExperimentalFactor> experimentalFactors ) {

        Collection<Set<FactorValue>> factorValuePairings = generateFactorValuePairings( experimentalFactors );

        /* check to see if the biomaterial's factor value pairing is one of the possible combinations */
        Map<Collection<FactorValue>, BioMaterial> seenPairings = new HashMap<Collection<FactorValue>, BioMaterial>();
        for ( BioMaterial m : biomaterials ) {

            Collection<FactorValue> factorValuesFromBioMaterial = m.getFactorValues();

            if ( !factorValuePairings.contains( factorValuesFromBioMaterial ) ) {
                throw new RuntimeException(
                        "Biomaterial's factor values are not in one of the possible factor value pairings from the experimental factors.  Block design is neither complete nor incomplete.  It is just incorrect." );
            }

            if ( factorValuesFromBioMaterial.size() < 2 ) {
                log.warn( "Biomaterial must have more than 1 factor value.  Incomplete block design." );
                return false;
            }

            seenPairings.put( factorValuesFromBioMaterial, m );
        }
        if ( seenPairings.size() != factorValuePairings.size() ) {
            log.warn( "Biomaterial not paired with all factor values for each experimental factor.  Found "
                    + seenPairings.size() + " but should have " + factorValuePairings.size()
                    + ".  Incomplete block design." );
            return false;
        }
        return true;

    }

    /**
     * Determines if each biomaterial in the expression experiment for the given quantitation type for the given
     * bioassay dimension has a factor value from each of the experimental factors.
     * 
     * @param expressionExperiment
     * @return
     * @throws Exception
     */
    protected boolean checkBlockDesign( ExpressionExperiment expressionExperiment ) {

        Collection<BioMaterial> biomaterials = getBioMaterials( expressionExperiment );

        /*
         * second, make sure each biomaterial has factor values from one experimental factor paired with factor values
         * from the other experimental factors
         */
        Collection<ExperimentalFactor> efs = expressionExperiment.getExperimentalDesign().getExperimentalFactors();
        return checkBlockDesign( biomaterials, efs );
    }

    /**
     * Returns a collection of all the different types of biomaterials across all bioassays in the experiment. If there
     * is more than one biomaterial per bioassay, a {@link RuntimeException} is thrown.
     * 
     * @param expressionExperiment
     * @return
     * @throws Exception
     */
    private List<BioMaterial> getBioMaterials( ExpressionExperiment ee ) {

        List<BioMaterial> biomaterials = new ArrayList<BioMaterial>();

        /* look for 1 bioassay/matrix column and 1 biomaterial/bioassay */
        for ( BioAssay assay : ee.getBioAssays() ) {
            Collection<BioMaterial> materials = assay.getSamplesUsed();
            if ( materials.size() != 1 ) {
                throw new RuntimeException( "Invalid number of biomaterials. Expecting 1 biomaterial/bioassay, got "
                        + materials.size() + "." );
            }

            biomaterials.addAll( materials );
        }

        return biomaterials;
    }

    /**
     * Isolate a biomaterial's factor values for a specific factor(s).
     * 
     * @param factors
     * @param biomaterial
     * @return the factor values the biomaterial has for the given factors.
     */
    private Collection<FactorValue> getRelevantFactorValues( Collection<ExperimentalFactor> factors,
            BioMaterial biomaterial ) {
        Collection<FactorValue> factorValues = biomaterial.getFactorValues();

        Collection<FactorValue> factorValuesToCheck = new HashSet<FactorValue>();
        for ( FactorValue factorValue : factorValues ) {
            if ( factors.contains( factorValue.getExperimentalFactor() ) ) {
                factorValuesToCheck.add( factorValue );
            }
        }
        return factorValuesToCheck;
    }

}
