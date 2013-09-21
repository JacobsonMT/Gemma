/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.association;

import java.io.Serializable;

import ubic.gemma.model.analysis.Analysis;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.ThreePrimeDistanceMethod;

/**
 * An association between a BioSequence and a Gene Product. This class is abstract and is variously subclassed with
 * BlatAssocation in order to capture the scores and other parameters that document why we think there is a connection
 * between a given sequence and a gene product.
 */
public abstract class BioSequence2GeneProduct implements Serializable {

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    private Long id = null;

    private Analysis sourceAnalysis = null;

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 2445048138750980972L;

    private Integer overlap = null;

    private Double score = null;

    private Long threePrimeDistance = null;

    private ThreePrimeDistanceMethod threePrimeDistanceMeasurementMethod = null;

    private Double specificity = null;

    private BioSequence bioSequence = null;

    private GeneProduct geneProduct = null;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public BioSequence2GeneProduct() {
    }

    /**
     * 
     */
    public BioSequence getBioSequence() {
        return this.bioSequence;
    }

    /**
     * A collection of GeneProducts that this BioSequence2GeneProduct corresponds to. A BioSequence can align to one or
     * more GeneProducts.
     */
    public GeneProduct getGeneProduct() {
        return this.geneProduct;
    }

    public Long getId() {
        return id;
    }

    /**
     * Degree to which the sequence overlaps with the gene product. This is often the overlap of a DNA sequence with the
     * exons encoding the mRNA for the GeneProduct, but could have other interpretations
     */
    public Integer getOverlap() {
        return this.overlap;
    }

    /**
     * The score for the association between the biosequence and the gene product. This could be a BLAT similarity or
     * other score.
     */
    public Double getScore() {
        return this.score;
    }

    public Analysis getSourceAnalysis() {
        return sourceAnalysis;
    }

    /**
     * A measure of how specific this association is compared to others that were obtained in the same analysis. This
     * can be misleading if the same sequence was analyzed multiple times with different algorithms, databases, or
     * parameters. High values are "better" but the exactly interpretation is implementation-specific.
     */
    public Double getSpecificity() {
        return this.specificity;
    }

    /**
     * The distance from the 3' end where this BioSequence aligns with respect to the Gene Product. This is often the
     * location of the alignment with respect to an mRNA 3' end.
     */
    public Long getThreePrimeDistance() {
        return this.threePrimeDistance;
    }

    /**
     * Specifies the method used to measure the distance from the threePrimeEnd.
     */
    public ThreePrimeDistanceMethod getThreePrimeDistanceMeasurementMethod() {
        return this.threePrimeDistanceMeasurementMethod;
    }

    public void setBioSequence( BioSequence bioSequence ) {
        this.bioSequence = bioSequence;
    }

    public void setGeneProduct( GeneProduct geneProduct ) {
        this.geneProduct = geneProduct;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setOverlap( Integer overlap ) {
        this.overlap = overlap;
    }

    public void setScore( Double score ) {
        this.score = score;
    }

    public void setSourceAnalysis( Analysis sourceAnalysis ) {
        this.sourceAnalysis = sourceAnalysis;
    }

    public void setSpecificity( Double specificity ) {
        this.specificity = specificity;
    }

    public void setThreePrimeDistance( Long threePrimeDistance ) {
        this.threePrimeDistance = threePrimeDistance;
    }

    public void setThreePrimeDistanceMeasurementMethod( ThreePrimeDistanceMethod threePrimeDistanceMeasurementMethod ) {
        this.threePrimeDistanceMeasurementMethod = threePrimeDistanceMeasurementMethod;
    }

}