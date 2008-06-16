/*
 * The Gemma project
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
package ubic.gemma.web.controller.diff;

import java.util.Collection;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.web.controller.expression.experiment.ExperimentalFactorValueObject;

/**
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionValueObject {

    private Gene gene;
    private ExpressionExperimentValueObject expressionExperiment;
    private String probe;
    private Long probeId;
    private Collection<ExperimentalFactorValueObject> experimentalFactors;
    private Double p;
    private String sortKey;
    private Boolean metThreshold = false;
    private Boolean fisherContribution = false;

    public Boolean getMetThreshold() {
        return metThreshold;
    }

    public void setMetThreshold( Boolean metThreshold ) {
        this.metThreshold = metThreshold;
    }

    public String getSortKey() {
        return sortKey;
    }

    public void setSortKey() {
        this.sortKey = String.format( "%06f%s", p, gene.getOfficialSymbol() );
    }

    public Collection<ExperimentalFactorValueObject> getExperimentalFactors() {
        return experimentalFactors;
    }

    public ExpressionExperimentValueObject getExpressionExperiment() {
        return expressionExperiment;
    }

    public Double getP() {
        return p;
    }

    public String getProbe() {
        return probe;
    }

    public Long getProbeId() {
        return probeId;
    }

    public void setExperimentalFactors( Collection<ExperimentalFactorValueObject> experimentalFactors ) {
        this.experimentalFactors = experimentalFactors;
    }

    public void setExpressionExperiment( ExpressionExperimentValueObject expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    public void setP( Double p ) {
        this.p = p;
    }

    public void setProbe( String probe ) {
        this.probe = probe;
    }

    public void setProbeId( Long probeId ) {
        this.probeId = probeId;
    }

    public Gene getGene() {
        return gene;
    }

    public void setGene( Gene gene ) {
        this.gene = gene;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();

        if ( gene == null ) buf.append( "-\t" );

        if ( StringUtils.isNotBlank( gene.getOfficialSymbol() ) ) {
            buf.append( gene.getOfficialSymbol() );
        } else if ( StringUtils.isNotBlank( gene.getOfficialName() ) ) {
            buf.append( gene.getOfficialName() );
        } else {
            buf.append( gene.getName() );
        }
        buf.append( "\t" );

        if ( StringUtils.isNotBlank( expressionExperiment.getShortName() ) ) {
            buf.append( expressionExperiment.getShortName() + "\t" );
        }

        buf.append( probe + "\t" );

        int i = 0;
        for ( ExperimentalFactorValueObject f : experimentalFactors ) {
            buf.append( f.getName() );
            if ( i < ( experimentalFactors.size() - 1 ) ) {
                buf.append( ", " );
            } else {
                buf.append( "\t" );
            }
            i++;
        }

        buf.append( p );

        return buf.toString();
    }

    public Boolean getFisherContribution() {
        return fisherContribution;
    }

    public void setFisherContribution( Boolean fisherContribution ) {
        this.fisherContribution = fisherContribution;
    }

}
