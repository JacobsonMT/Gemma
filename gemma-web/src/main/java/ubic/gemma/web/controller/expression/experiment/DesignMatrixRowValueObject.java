/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.web.controller.expression.experiment;

import org.apache.commons.lang3.StringUtils;
import ubic.basecode.dataStructure.CountingMap;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchInfoPopulationServiceImpl;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.util.FactorValueVector;

import java.io.Serializable;
import java.util.*;

/**
 * For the display of a summary table about experimental design.
 *
 * @author luke
 * @author paul fixed to handle incomplete designs
 */
@SuppressWarnings("WeakerAccess") // Used in front end
public class DesignMatrixRowValueObject implements Serializable {

    private static final long serialVersionUID = 1;
    private int count;
    private List<String> factors;
    private List<String> factorTypes;
    private Map<String, String> factorValueMap;

    public DesignMatrixRowValueObject( FactorValueVector factorValues, int n ) {
        factors = new ArrayList<>();
        factorTypes = new ArrayList<>();
        factorValueMap = new HashMap<>();
        for ( ExperimentalFactor factor : factorValues.getFactors() ) {
            factors.add( getFactorString( factor ) );
            factorTypes.add( factor.getType().toString().toUpperCase() );

            factorValueMap.put( getFactorString( factor ),
                    getFactorValueString( factorValues.getValuesForFactor( factor ) ) );

        }
        count = n;
    }

    /**
     * @return the count
     */
    public int getCount() {
        return count;
    }

    /**
     * @param count the count to set
     */
    public void setCount( int count ) {
        this.count = count;
    }

    /**
     * @return the factors
     */
    public List<String> getFactors() {
        return factors;
    }

    /**
     * @param factors the factors to set
     */
    public void setFactors( List<String> factors ) {
        this.factors = factors;
    }

    public List<String> getFactorTypes() {
        return factorTypes;
    }

    /**
     * @return the factorValues
     */
    public Map<String, String> getFactorValueMap() {
        return factorValueMap;
    }

    public void setFactorValueMap( Map<String, String> factorValueMap ) {
        this.factorValueMap = factorValueMap;
    }

    /**
     * @param factor factor
     * @return A unique string for the factor, but hopefully human-readable.
     */
    private String getFactorString( ExperimentalFactor factor ) {
        if ( StringUtils.isBlank( factor.getDescription() ) || factor.getDescription().equals( factor.getName() ) ) {
            return factor.getName();
        }
        // Note: the use of stringUtils.abbreviate here can cause json parsing problems for DWR, due to the '...', and
        // no means of escaping seems to fix this.
        String result =
                factor.getName() + " (" + StringUtils.abbreviate( factor.getDescription(), 25 ).replace( "...", "" )
                        + ")";

        if ( factorValueMap.containsKey( result ) ) {
            result = result + " [" + factor.getId() + "]";
        }
        return result;

    }

    private String getFactorValueString( Collection<FactorValue> factorValues ) {
        StringBuilder buf = new StringBuilder();
        for ( Iterator<FactorValue> i = factorValues.iterator(); i.hasNext(); ) {
            FactorValue fv = i.next();
            buf.append( getFactorValueString( fv ) );
            if ( i.hasNext() )
                buf.append( ", " );
        }
        return buf.toString();
    }

    private String getFactorValueString( FactorValue factorValue ) {

        // missing data.
        if ( factorValue == null )
            return "";

        StringBuilder buf = new StringBuilder();
        if ( !factorValue.getCharacteristics().isEmpty() ) {
            for ( Iterator<Characteristic> i = factorValue.getCharacteristics().iterator(); i.hasNext(); ) {
                Characteristic characteristic = i.next();

                /*
                 * Note we don't use toString here because it includes the category, uri, etc.
                 */
                buf.append( characteristic.getValue() );
                if ( i.hasNext() )
                    buf.append( " " );
            }
        } else if ( StringUtils.isNotBlank( factorValue.getValue() ) ) {
            buf.append( factorValue.getValue() );
        } else if ( factorValue.getMeasurement() != null ) {
            buf.append( factorValue.getMeasurement().getValue() );
        }
        return buf.toString();
    }

    public static final class Factory {

        /**
         * @param expressionExperiment ee
         * @param removeBatchFactor    if true, any factor(s) that look like "batch information" will be ignored.
         * @return collection
         */
        public static Collection<DesignMatrixRowValueObject> getDesignMatrix( ExpressionExperiment expressionExperiment,
                boolean removeBatchFactor ) {

            Collection<ExperimentalFactor> factors = expressionExperiment.getExperimentalDesign()
                    .getExperimentalFactors();

            for ( Iterator<ExperimentalFactor> iterator = factors.iterator(); iterator.hasNext(); ) {
                ExperimentalFactor experimentalFactor = iterator.next();
                if ( BatchInfoPopulationServiceImpl.isBatchFactor( experimentalFactor ) ) {
                    iterator.remove();
                }
            }

            CountingMap<FactorValueVector> assayCount = new CountingMap<>();
            for ( BioAssay assay : expressionExperiment.getBioAssays() ) {
                BioMaterial sample = assay.getSampleUsed();
                assayCount.increment( new FactorValueVector( factors, sample.getFactorValues() ) );
            }

            Collection<DesignMatrixRowValueObject> matrix = new ArrayList<>();
            List<FactorValueVector> keys = assayCount.sortedKeyList( true );
            for ( FactorValueVector key : keys ) {
                matrix.add( new DesignMatrixRowValueObject( key, assayCount.get( key ) ) );
            }
            return matrix;

        }

    }

}