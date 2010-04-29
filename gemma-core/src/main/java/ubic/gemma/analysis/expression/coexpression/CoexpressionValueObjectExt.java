/*
 * The Gemma project
 * 
 * Copyright (c) 2006 Columbia University
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
package ubic.gemma.analysis.expression.coexpression;

import java.util.Collection;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.model.genome.Gene;

/**
 * Implementation note: This has very abbreviated field names to reduce the size of strings sent to browsers.
 * 
 * @author luke
 * @version $Id$
 */
public class CoexpressionValueObjectExt implements Comparable<CoexpressionValueObjectExt> {

    private Gene queryGene;
    private Gene foundGene;
    private String sortKey;
    private Integer supportKey;
    private Integer posSupp;
    private Integer negSupp;
    private Integer nonSpecPosSupp;
    private Integer nonSpecNegSupp;
    // private Boolean hybWQuery;
    private Integer numTestedIn;
    private Integer goSim;
    private Integer maxGoSim;
    private String datasetVector;
    private Boolean containsMyData;
    private Collection<Long> supportingExperiments;
    private String gene2GeneProteinAssociationStringUrl;
    private String gene2GeneProteinInteractionConfidenceScore;
    /**
     * @return the gene2GeneProteinInteractionConfidenceScore
     */
    public String getGene2GeneProteinInteractionConfidenceScore() {
        return gene2GeneProteinInteractionConfidenceScore;
    }

    /**
     * @param gene2GeneProteinInteractionConfidenceScore the gene2GeneProteinInteractionConfidenceScore to set
     */
    public void setGene2GeneProteinInteractionConfidenceScore( String gene2GeneProteinInteractionConfidenceScore ) {
        this.gene2GeneProteinInteractionConfidenceScore = gene2GeneProteinInteractionConfidenceScore;
    }

    /**
     * @return the gene2GeneProteinInteractionEvidence
     */
    public String getGene2GeneProteinInteractionEvidence() {
        return gene2GeneProteinInteractionEvidence;
    }

    /**
     * @param gene2GeneProteinInteractionEvidence the gene2GeneProteinInteractionEvidence to set
     */
    public void setGene2GeneProteinInteractionEvidence( String gene2GeneProteinInteractionEvidence ) {
        this.gene2GeneProteinInteractionEvidence = gene2GeneProteinInteractionEvidence;
    }

    private String gene2GeneProteinInteractionEvidence;
    
   
        

    public Gene getQueryGene() {
        return queryGene;
    }

    public void setQueryGene( Gene queryGene ) {
        this.queryGene = queryGene;
    }

    /**
     * @return the coexpressed gene.
     */
    public Gene getFoundGene() {
        return foundGene;
    }

    public void setFoundGene( Gene foundGene ) {
        this.foundGene = foundGene;
    }

    public String getSortKey() {
        return sortKey;
    }

    public void setSortKey() {
        this.sortKey = String.format( "%06f%s", 1.0 / Math.abs( getSupportKey() ), getFoundGene().getOfficialSymbol() );
    }

    public Integer getSupportKey() {
        return supportKey;
    }

    public void setSupportKey( Integer supportKey ) {
        this.supportKey = supportKey;
    }

    public String getDatasetVector() {
        return datasetVector;
    }

    public void setDatasetVector( String datasetVector ) {
        this.datasetVector = datasetVector;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if ( this.getPosSupp() > 0 ) {
            buf.append( getSupportRow( getPosSupp(), "+" ) );
        }
        if ( getNegSupp() > 0 ) {
            if ( buf.length() > 0 ) buf.append( "\n" );
            buf.append( getSupportRow( getNegSupp(), "-" ) );
        }
        return buf.toString();
    }

    private String getSupportRow( Integer links, String sign ) {
        String[] fields = new String[] { queryGene.getOfficialSymbol(), foundGene.getOfficialSymbol(),
                links.toString(), sign };
        return StringUtils.join( fields, "\t" );
    }

    public Integer getPosSupp() {
        return posSupp;
    }

    public void setPosSupp( Integer posSupp ) {
        this.posSupp = posSupp;
    }

    public Integer getNegSupp() {
        return negSupp;
    }

    public void setNegSupp( Integer negSupp ) {
        this.negSupp = negSupp;
    }

    public Integer getNonSpecPosSupp() {
        return nonSpecPosSupp;
    }

    public void setNonSpecPosSupp( Integer nonSpecPosSupp ) {
        this.nonSpecPosSupp = nonSpecPosSupp;
    }

    public Integer getNonSpecNegSupp() {
        return nonSpecNegSupp;
    }

    public void setNonSpecNegSupp( Integer nonSpecNegSupp ) {
        this.nonSpecNegSupp = nonSpecNegSupp;
    }

    public Integer getNumTestedIn() {
        return numTestedIn;
    }

    public void setNumTestedIn( Integer numTestedIn ) {
        this.numTestedIn = numTestedIn;
    }

    public Integer getGoSim() {
        return goSim;
    }

    public void setGoSim( Integer goSim ) {
        this.goSim = goSim;
    }

    public Integer getMaxGoSim() {
        return maxGoSim;
    }

    public void setMaxGoSim( Integer maxGoSim ) {
        this.maxGoSim = maxGoSim;
    }

    public void setSortKey( String sortKey ) {
        this.sortKey = sortKey;
    }

    public Collection<Long> getSupportingExperiments() {
        return supportingExperiments;
    }

    public void setSupportingExperiments( Collection<Long> supportingExperiments ) {
        this.supportingExperiments = supportingExperiments;
    }

    public Boolean getContainsMyData() {
        return containsMyData;
    }

    public void setContainsMyData( Boolean containsMyData ) {
        this.containsMyData = containsMyData;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( sortKey == null ) ? 0 : sortKey.hashCode() );
        return result;
    }
    
    /**
     * @return the gene2GeneProteinAssociationStringUrl
     */
    public String getGene2GeneProteinAssociationStringUrl() {
        return gene2GeneProteinAssociationStringUrl;
    }

    /**
     * @param gene2GeneProteinAssociationStringUrl the gene2GeneProteinAssociationStringUrl to set
     */
    public void setGene2GeneProteinAssociationStringUrl( String gene2GeneProteinAssociationStringUrl ) {
        this.gene2GeneProteinAssociationStringUrl = gene2GeneProteinAssociationStringUrl;
    }
    
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        CoexpressionValueObjectExt other = ( CoexpressionValueObjectExt ) obj;
        if ( sortKey == null ) {
            if ( other.sortKey != null ) return false;
        } else if ( !sortKey.equals( other.sortKey ) ) return false;
        return true;
    }

    public int compareTo( CoexpressionValueObjectExt arg0 ) {
        return this.getSortKey().compareTo( arg0.getSortKey() );
    }
    
   
    
    

}
