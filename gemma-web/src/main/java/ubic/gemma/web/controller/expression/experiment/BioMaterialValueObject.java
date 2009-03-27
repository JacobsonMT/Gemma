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
package ubic.gemma.web.controller.expression.experiment;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * @version $Id$
 * @author lukem
 */
public class BioMaterialValueObject {

    private long id;
    private String name;
    private String description;
    private String characteristics;
    private String assayName;
    private String assayDescription;
    private Map<String, String> factors;
    private Map<String, String> factorValues;
    private Map<String, String> factorIdToFactorValueId;

    public BioMaterialValueObject() {
    }

    public BioMaterialValueObject( BioMaterial bm, BioAssay ba ) {
        this.id = bm.getId();
        this.name = bm.getName();
        this.description = bm.getDescription();
        this.characteristics = getCharacteristicString( bm.getCharacteristics() );
        this.assayName = ba.getName();
        this.assayDescription = ba.getDescription();

        this.factors = new HashMap<String, String>();
        this.factorValues = new HashMap<String, String>();
        this.factorIdToFactorValueId = new HashMap<String, String>();
        for ( FactorValue fv : bm.getFactorValues() ) {
            ExperimentalFactor factor = fv.getExperimentalFactor();
            String factorId = String.format( "factor%d", factor.getId() );
            String factorValueId = String.format( "fv%d", fv.getId() );
            this.factors.put( factorId, getExperimentalFactorString( factor ) );
            this.factorValues.put( factorValueId, getFactorValueString( fv ) );
            this.factorIdToFactorValueId.put( factorId, factorValueId );
        }
    }

    /**
     * @param characters
     * @return
     */
    private String getCharacteristicString( Collection<Characteristic> characters ) {
        StringBuffer buf = new StringBuffer();
        for ( Iterator<Characteristic> iter = characters.iterator(); iter.hasNext(); ) {
            Characteristic c = iter.next();
            buf.append( c.getCategory() );
            buf.append( ": " );
            buf.append( c.getValue() == null ? "no value" : c.getValue() );
            if ( iter.hasNext() ) buf.append( ", " );
        }
        return buf.length() > 0 ? buf.toString() : "no characteristics";
    }

    private String getExperimentalFactorString( ExperimentalFactor factor ) {
        return factor.getName();
    }

    private String getFactorValueString( FactorValue value ) {
        return getCharacteristicString( value.getCharacteristics() );
    }

    public long getId() {
        return id;
    }

    public void setId( long id ) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public String getCharacteristics() {
        return characteristics;
    }

    public void setCharacteristics( String characteristics ) {
        this.characteristics = characteristics;
    }

    public String getAssayName() {
        return assayName;
    }

    public void setAssayName( String assayName ) {
        this.assayName = assayName;
    }

    public String getAssayDescription() {
        return assayDescription;
    }

    public void setAssayDescription( String assayDescription ) {
        this.assayDescription = assayDescription;
    }

    public Map<String, String> getFactors() {
        return factors;
    }

    public void setFactors( Map<String, String> factors ) {
        this.factors = factors;
    }

    public Map<String, String> getFactorValues() {
        return factorValues;
    }

    public void setFactorValues( Map<String, String> factorValues ) {
        this.factorValues = factorValues;
    }

    public Map<String, String> getFactorIdToFactorValueId() {
        return factorIdToFactorValueId;
    }

    public void setFactorIdToFactorValueId( Map<String, String> factorIdToFactorValueId ) {
        this.factorIdToFactorValueId = factorIdToFactorValueId;
    }

}
