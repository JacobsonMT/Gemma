/*

 * The Gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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

package ubic.gemma.model.expression.experiment;

import gemma.gsec.model.Securable;
import gemma.gsec.model.SecureValueObject;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisValueObject;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.AbstractCuratableValueObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

/**
 * @author kelsey
 */
@SuppressWarnings("unused") // ValueObject accessed from JS
public class ExpressionExperimentValueObject extends AbstractCuratableValueObject<ExpressionExperiment>
        implements Comparable<ExpressionExperimentValueObject>, SecureValueObject {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -5678747537830051610L;

    protected Long sourceExperiment;

    private Boolean isSubset = false;
    private String accession;
    private Integer arrayDesignCount;
    private String batchFetchEventType;
    private Integer bioAssayCount;
    private Integer bioMaterialCount = null;
    private Integer coexpressionLinkCount = null;
    private Boolean currentUserHasWritePermission = null;
    private Boolean currentUserIsOwner = null;
    private Date dateArrayDesignLastUpdated;
    private Date dateBatchFetch;
    private Date dateCached;
    private Date dateDifferentialAnalysis;
    private Date dateLinkAnalysis;
    private Date dateMissingValueAnalysis;
    private Date datePcaAnalysis;
    private Date dateProcessedDataVectorComputation;
    private Integer designElementDataVectorCount;
    private Collection<DifferentialExpressionAnalysisValueObject> differentialExpressionAnalyses = new HashSet<>();
    private Long experimentalDesign;
    private String externalDatabase;
    private String externalUri;
    private Boolean hasBothIntensities = null;
    private Boolean hasCoexpressionAnalysis = null;
    private Boolean hasDifferentialExpressionAnalysis = null;
    private Boolean hasEitherIntensity = null;
    private Boolean hasProbeSpecificForQueryGene;
    private String investigators;
    private boolean isPublic;
    private Boolean isShared = false;
    private String linkAnalysisEventType;
    private Double minPvalue;
    private String missingValueAnalysisEventType;
    private String name;
    private Integer numAnnotations;
    private Integer numPopulatedFactors;
    private Long parentTaxonId;
    private String pcaAnalysisEventType;
    private String processedDataVectorComputationEventType;
    private Integer processedExpressionVectorCount = null;
    private Integer pubmedId;
    private Collection<AuditEventValueObject> sampleRemovedFlags;
    private String shortName;
    private String source;
    private String taxon;
    private Long taxonId;
    private String technologyType;
    private String metadata;
    private String batchConfound;
    private String batchEffect;
    private String description;

    /**
     * Required when using the class as a spring bean.
     */
    public ExpressionExperimentValueObject() {
    }

    public ExpressionExperimentValueObject( Long id ) {
        super( id );
    }

    /**
     * Constructor using this VO for EESubSets - does not populate most of VO properties, only source experiment and the isSubset property.
     */
    public ExpressionExperimentValueObject( ExpressionExperimentSubSet ee ) {
        super( ee.getId() );
        this.isSubset = true;
        this.sourceExperiment = ee.getSourceExperiment().getId();
    }

    /**
     * Populates curation details properties, accession, bio assay count, name, short name, experimental design and source.
     *
     * @param ee the experiment to load the values from.
     */
    public ExpressionExperimentValueObject( ExpressionExperiment ee ) {
        this( ee, false );
    }

    /**
     * Populates curation details properties, accession, bio assay count, name, short name, experimental design and source.
     *
     * @param ee   the experiment to load the values from.
     * @param lite if set to true, does not populate most of values - only curation info, name, source and short name.
     */
    public ExpressionExperimentValueObject( ExpressionExperiment ee, boolean lite ) {
        super( ee );
        this.shortName = ee.getShortName();
        this.name = ee.getName();
        this.source = ee.getSource();
        if ( !lite ) {
            this.bioAssayCount = ee.getBioAssays() != null ? ee.getBioAssays().size() : null;
            this.accession = ee.getAccession() != null ? ee.getAccession().toString() : null;
            this.experimentalDesign = ee.getExperimentalDesign() != null ? ee.getExperimentalDesign().getId() : null;
        }
    }

    /**
     * Creates a copy of given ExpressionExperimentValueObject
     *
     * @param otherBean the bean to create copy of.
     */
    public ExpressionExperimentValueObject( ExpressionExperimentValueObject otherBean ) {
        this( otherBean.lastUpdated, otherBean.troubled, otherBean.lastTroubledEvent, otherBean.needsAttention,
                otherBean.lastNeedsAttentionEvent, otherBean.curationNote, otherBean.lastNoteUpdateEvent,
                otherBean.accession, otherBean.arrayDesignCount, otherBean.batchFetchEventType, otherBean.bioAssayCount,
                otherBean.bioMaterialCount, otherBean.coexpressionLinkCount, otherBean.currentUserHasWritePermission,
                otherBean.currentUserIsOwner, otherBean.dateArrayDesignLastUpdated, otherBean.dateBatchFetch,
                otherBean.dateCached, otherBean.dateDifferentialAnalysis, otherBean.dateLinkAnalysis,
                otherBean.dateMissingValueAnalysis, otherBean.datePcaAnalysis,
                otherBean.dateProcessedDataVectorComputation, otherBean.designElementDataVectorCount,
                otherBean.differentialExpressionAnalyses, otherBean.experimentalDesign, otherBean.externalDatabase,
                otherBean.externalUri, otherBean.hasBothIntensities, otherBean.hasCoexpressionAnalysis,
                otherBean.hasDifferentialExpressionAnalysis, otherBean.hasEitherIntensity,
                otherBean.hasProbeSpecificForQueryGene, otherBean.id, otherBean.investigators, otherBean.isPublic,
                otherBean.isShared, otherBean.isSubset, otherBean.linkAnalysisEventType, otherBean.minPvalue,
                otherBean.missingValueAnalysisEventType, otherBean.name, otherBean.numAnnotations,
                otherBean.numPopulatedFactors, otherBean.parentTaxonId, otherBean.pcaAnalysisEventType,
                otherBean.processedDataVectorComputationEventType, otherBean.processedExpressionVectorCount,
                otherBean.pubmedId, otherBean.sampleRemovedFlags, otherBean.shortName, otherBean.source,
                otherBean.sourceExperiment, otherBean.taxon, otherBean.taxonId, otherBean.technologyType,
                otherBean.metadata );
    }

    private ExpressionExperimentValueObject( Date lastUpdated, Boolean troubled, AuditEventValueObject troubledEvent,
            Boolean needsAttention, AuditEventValueObject needsAttentionEvent, String curationNote,
            AuditEventValueObject noteEvent, String accession, Integer arrayDesignCount, String batchFetchEventType,
            Integer bioAssayCount, Integer bioMaterialCount, Integer coexpressionLinkCount,
            Boolean currentUserHasWritePermission, Boolean currentUserIsOwner, Date dateArrayDesignLastUpdated,
            Date dateBatchFetch, Date dateCached, Date dateDifferentialAnalysis, Date dateLinkAnalysis,
            Date dateMissingValueAnalysis, Date datePcaAnalysis, Date dateProcessedDataVectorComputation,
            Integer designElementDataVectorCount,
            Collection<DifferentialExpressionAnalysisValueObject> differentialExpressionAnalyses,
            Long experimentalDesign, String externalDatabase, String externalUri, Boolean hasBothIntensities,
            Boolean hasCoexpressionAnalysis, Boolean hasDifferentialExpressionAnalysis, Boolean hasEitherIntensity,
            Boolean hasProbeSpecificForQueryGene, Long id, String investigators, Boolean isPublic, Boolean isShared,
            Boolean isSubset, String linkAnalysisEventType, Double minPvalue, String missingValueAnalysisEventType,
            String name, Integer numAnnotations, Integer numPopulatedFactors, Long parentTaxonId,
            String pcaAnalysisEventType, String processedDataVectorComputationEventType,
            Integer processedExpressionVectorCount, Integer pubmedId,
            Collection<AuditEventValueObject> sampleRemovedFlags, String shortName, String source,
            Long sourceExperiment, String taxon, Long taxonId, String technologyType, String metadata ) {
        super( id, lastUpdated, troubled, troubledEvent, needsAttention, needsAttentionEvent, curationNote, noteEvent );
        this.accession = accession;
        this.arrayDesignCount = arrayDesignCount;
        this.batchFetchEventType = batchFetchEventType;
        this.bioAssayCount = bioAssayCount;
        this.bioMaterialCount = bioMaterialCount;
        this.coexpressionLinkCount = coexpressionLinkCount;
        this.currentUserHasWritePermission = currentUserHasWritePermission;
        this.currentUserIsOwner = currentUserIsOwner;
        this.dateArrayDesignLastUpdated = dateArrayDesignLastUpdated;
        this.dateBatchFetch = dateBatchFetch;
        this.dateCached = dateCached;
        this.dateDifferentialAnalysis = dateDifferentialAnalysis;
        this.dateLinkAnalysis = dateLinkAnalysis;
        this.dateMissingValueAnalysis = dateMissingValueAnalysis;
        this.datePcaAnalysis = datePcaAnalysis;
        this.dateProcessedDataVectorComputation = dateProcessedDataVectorComputation;
        this.designElementDataVectorCount = designElementDataVectorCount;
        this.differentialExpressionAnalyses = differentialExpressionAnalyses;
        this.experimentalDesign = experimentalDesign;
        this.externalDatabase = externalDatabase;
        this.externalUri = externalUri;
        this.hasBothIntensities = hasBothIntensities;
        this.hasCoexpressionAnalysis = hasCoexpressionAnalysis;
        this.hasDifferentialExpressionAnalysis = hasDifferentialExpressionAnalysis;
        this.hasEitherIntensity = hasEitherIntensity;
        this.hasProbeSpecificForQueryGene = hasProbeSpecificForQueryGene;
        this.investigators = investigators;
        this.isPublic = isPublic;
        this.isShared = isShared;
        this.isSubset = isSubset;
        this.linkAnalysisEventType = linkAnalysisEventType;
        this.minPvalue = minPvalue;
        this.missingValueAnalysisEventType = missingValueAnalysisEventType;
        this.name = name;
        this.numAnnotations = numAnnotations;
        this.numPopulatedFactors = numPopulatedFactors;
        this.parentTaxonId = parentTaxonId;
        this.pcaAnalysisEventType = pcaAnalysisEventType;
        this.processedDataVectorComputationEventType = processedDataVectorComputationEventType;
        this.processedExpressionVectorCount = processedExpressionVectorCount;
        this.pubmedId = pubmedId;
        this.sampleRemovedFlags = sampleRemovedFlags;
        this.shortName = shortName;
        this.source = source;
        this.sourceExperiment = sourceExperiment;
        this.taxon = taxon;
        this.taxonId = taxonId;
        this.technologyType = technologyType;
        this.metadata = metadata;
    }

    /**
     * Creates a value object for either EE or EESubSet.
     *
     * @param bioAssaySet either EE or EESubSet instance.
     * @return value object that represents the subset, or lite version of this VO for EE.
     * @see this#ExpressionExperimentValueObject(ExpressionExperimentSubSet) for subSet VO description
     * @see this#ExpressionExperimentValueObject(ExpressionExperiment, boolean) for lite VO description
     */
    public static ExpressionExperimentValueObject createValueObject( BioAssaySet bioAssaySet ) {
        if ( bioAssaySet instanceof ExpressionExperiment ) {
            return new ExpressionExperimentValueObject( ( ExpressionExperiment ) bioAssaySet, true );
        } else {
            return new ExpressionExperimentValueObject( ( ExpressionExperimentSubSet ) bioAssaySet );
        }
    }

    public static Collection<ExpressionExperimentValueObject> convert2ValueObjects(
            Collection<ExpressionExperiment> collection ) {
        Collection<ExpressionExperimentValueObject> result = new ArrayList<>();
        for ( ExpressionExperiment ee : collection ) {
            result.add( new ExpressionExperimentValueObject( ee ) );
        }
        return result;
    }

    @Override
    public int compareTo( ExpressionExperimentValueObject arg0 ) {
        return this.getId().compareTo( arg0.getId() );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        ExpressionExperimentValueObject other = ( ExpressionExperimentValueObject ) obj;
        if ( id == null ) {
            if ( other.id != null )
                return false;
        } else if ( !id.equals( other.id ) )
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * ( result + ( ( id == null ) ? 0 : id.hashCode() ) );
        return result;
    }

    @Override
    public String toString() {
        return this.getShortName() + " (id = " + this.getId() + ")";
    }

    public void auditEvents2SampleRemovedFlags( Collection<AuditEvent> s ) {
        Collection<AuditEventValueObject> converted = new HashSet<>();

        for ( AuditEvent ae : s ) {
            converted.add( new AuditEventValueObject( ae ) );
        }

        this.sampleRemovedFlags = converted;
    }

    public String getAccession() {
        return this.accession;
    }

    public void setAccession( String accession ) {
        this.accession = accession;
    }

    public Integer getArrayDesignCount() {
        return this.arrayDesignCount;
    }

    public void setArrayDesignCount( Integer arrayDesignCount ) {
        this.arrayDesignCount = arrayDesignCount;
    }

    public String getBatchFetchEventType() {
        return batchFetchEventType;
    }

    public void setBatchFetchEventType( String batchFetchEventType ) {
        this.batchFetchEventType = batchFetchEventType;
    }

    public Integer getBioAssayCount() {
        return this.bioAssayCount;
    }

    public void setBioAssayCount( Integer bioAssayCount ) {
        this.bioAssayCount = bioAssayCount;
    }

    public Integer getBioMaterialCount() {
        return this.bioMaterialCount;
    }

    public void setBioMaterialCount( Integer bioMaterialCount ) {
        this.bioMaterialCount = bioMaterialCount;
    }

    public Integer getCoexpressionLinkCount() {
        return this.coexpressionLinkCount;
    }

    public void setCoexpressionLinkCount( Integer coexpressionLinkCount ) {
        this.coexpressionLinkCount = coexpressionLinkCount;
    }

    /**
     * The date the platform associated with the experiment was last updated. If there are multiple platforms this
     * should be the date of the most recent modification of any of them. This is used to help flag experiments that
     * need re-analysis due to changes in the underlying array design(s)
     */
    public Date getDateArrayDesignLastUpdated() {
        return this.dateArrayDesignLastUpdated;
    }

    public void setDateArrayDesignLastUpdated( Date dateArrayDesignLastUpdated ) {
        this.dateArrayDesignLastUpdated = dateArrayDesignLastUpdated;
    }

    public Date getDateBatchFetch() {
        return dateBatchFetch;
    }

    public void setDateBatchFetch( Date dateBatchFetch ) {
        this.dateBatchFetch = dateBatchFetch;
    }

    /**
     * The date this object was generated.
     */
    public Date getDateCached() {
        return this.dateCached;
    }

    public void setDateCached( Date dateCached ) {
        this.dateCached = dateCached;
    }

    public Date getDateDifferentialAnalysis() {
        return this.dateDifferentialAnalysis;
    }

    public void setDateDifferentialAnalysis( Date dateDifferentialAnalysis ) {
        this.dateDifferentialAnalysis = dateDifferentialAnalysis;
    }

    public Date getDateLinkAnalysis() {
        return this.dateLinkAnalysis;
    }

    public void setDateLinkAnalysis( Date dateLinkAnalysis ) {
        this.dateLinkAnalysis = dateLinkAnalysis;
    }

    public Date getDateMissingValueAnalysis() {
        return this.dateMissingValueAnalysis;
    }

    public void setDateMissingValueAnalysis( Date dateMissingValueAnalysis ) {
        this.dateMissingValueAnalysis = dateMissingValueAnalysis;
    }

    public Date getDatePcaAnalysis() {
        return datePcaAnalysis;
    }

    public void setDatePcaAnalysis( Date datePcaAnalysis ) {
        this.datePcaAnalysis = datePcaAnalysis;
    }

    public Date getDateProcessedDataVectorComputation() {
        return this.dateProcessedDataVectorComputation;
    }

    public void setDateProcessedDataVectorComputation( Date dateProcessedDataVectorComputation ) {
        this.dateProcessedDataVectorComputation = dateProcessedDataVectorComputation;
    }

    public Integer getDesignElementDataVectorCount() {
        return this.designElementDataVectorCount;
    }

    public void setDesignElementDataVectorCount( Integer designElementDataVectorCount ) {
        this.designElementDataVectorCount = designElementDataVectorCount;
    }

    public Collection<DifferentialExpressionAnalysisValueObject> getDifferentialExpressionAnalyses() {
        return differentialExpressionAnalyses;
    }

    public void setDifferentialExpressionAnalyses(
            Collection<DifferentialExpressionAnalysisValueObject> differentialExpressionAnalyses ) {
        this.differentialExpressionAnalyses = differentialExpressionAnalyses;
    }

    public Long getExperimentalDesign() {
        return experimentalDesign;
    }

    public void setExperimentalDesign( Long experimentalDesign ) {
        this.experimentalDesign = experimentalDesign;
    }

    public String getExternalDatabase() {
        return this.externalDatabase;
    }

    public void setExternalDatabase( String externalDatabase ) {
        this.externalDatabase = externalDatabase;
    }

    public String getExternalUri() {
        return this.externalUri;
    }

    public void setExternalUri( String externalUri ) {
        this.externalUri = externalUri;
    }

    public Boolean getHasBothIntensities() {
        return hasBothIntensities;
    }

    public void setHasBothIntensities( boolean hasBothIntensities ) {
        this.hasBothIntensities = hasBothIntensities;
    }

    public Boolean getHasCoexpressionAnalysis() {
        return hasCoexpressionAnalysis;
    }

    public void setHasCoexpressionAnalysis( Boolean hasCoexpressionAnalysis ) {
        this.hasCoexpressionAnalysis = hasCoexpressionAnalysis;
    }

    public Boolean getHasDifferentialExpressionAnalysis() {
        return hasDifferentialExpressionAnalysis;
    }

    public void setHasDifferentialExpressionAnalysis( Boolean hasDifferentialExpressionAnalysis ) {
        this.hasDifferentialExpressionAnalysis = hasDifferentialExpressionAnalysis;
    }

    /**
     * @return true if the experiment has any intensity information available. Relevant for two-channel studies.
     */
    public Boolean getHasEitherIntensity() {
        return hasEitherIntensity;
    }

    public void setHasEitherIntensity( Boolean hasEitherIntensity ) {
        this.hasEitherIntensity = hasEitherIntensity;
    }

    /**
     * Used in display of gene-wise analysis results.
     */
    public Boolean getHasProbeSpecificForQueryGene() {
        return this.hasProbeSpecificForQueryGene;
    }

    public void setHasProbeSpecificForQueryGene( Boolean hasProbeSpecificForQueryGene ) {
        this.hasProbeSpecificForQueryGene = hasProbeSpecificForQueryGene;
    }

    public String getInvestigators() {
        return this.investigators;
    }

    public void setInvestigators( String investigators ) {
        this.investigators = investigators;
    }

    @Override
    public boolean getIsPublic() {
        return this.isPublic;
    }

    @Override
    public void setIsPublic( boolean isPublic ) {
        this.isPublic = isPublic;
    }

    @Override
    public boolean getIsShared() {
        return this.isShared;
    }

    @Override
    public void setIsShared( boolean isShared ) {
        this.isShared = isShared;
    }

    public String getLinkAnalysisEventType() {
        return this.linkAnalysisEventType;
    }

    public void setLinkAnalysisEventType( String linkAnalysisEventType ) {
        this.linkAnalysisEventType = linkAnalysisEventType;
    }

    public Double getMinPvalue() {
        return this.minPvalue;
    }

    public void setMinPvalue( Double minPvalue ) {
        this.minPvalue = minPvalue;
    }

    public String getMissingValueAnalysisEventType() {
        return this.missingValueAnalysisEventType;
    }

    public void setMissingValueAnalysisEventType( String missingValueAnalysisEventType ) {
        this.missingValueAnalysisEventType = missingValueAnalysisEventType;
    }

    public String getName() {
        return this.name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    /**
     * The number of terms (Characteristics) the experiment has to describe it.
     */
    public Integer getNumAnnotations() {
        return this.numAnnotations;
    }

    public void setNumAnnotations( Integer numAnnotations ) {
        this.numAnnotations = numAnnotations;
    }

    /**
     * The number of experimental factors the experiment has (counting those that are populated with biomaterials)
     */
    public Integer getNumPopulatedFactors() {
        return this.numPopulatedFactors;
    }

    public void setNumPopulatedFactors( Integer numPopulatedFactors ) {
        this.numPopulatedFactors = numPopulatedFactors;
    }

    public Long getParentTaxonId() {
        return parentTaxonId;
    }

    public void setParentTaxonId( Long parentTaxonId ) {
        this.parentTaxonId = parentTaxonId;
    }

    public String getPcaAnalysisEventType() {
        return pcaAnalysisEventType;
    }

    public void setPcaAnalysisEventType( String pcaAnalysisEventType ) {
        this.pcaAnalysisEventType = pcaAnalysisEventType;
    }

    public String getProcessedDataVectorComputationEventType() {
        return this.processedDataVectorComputationEventType;
    }

    public void setProcessedDataVectorComputationEventType( String processedDataVectorComputationEventType ) {
        this.processedDataVectorComputationEventType = processedDataVectorComputationEventType;
    }

    public Integer getProcessedExpressionVectorCount() {
        return this.processedExpressionVectorCount;
    }

    public void setProcessedExpressionVectorCount( Integer processedExpressionVectorCount ) {
        this.processedExpressionVectorCount = processedExpressionVectorCount;
    }

    public Integer getPubmedId() {
        return this.pubmedId;
    }

    public void setPubmedId( Integer pubmedId ) {
        this.pubmedId = pubmedId;
    }

    /**
     * Details of samples that were removed (or marked as outliers). This can happen multiple times in the life of a
     * data set, so this is a collection of AuditEvents.
     */
    public Collection<AuditEventValueObject> getSampleRemovedFlags() {
        return this.sampleRemovedFlags;
    }

    public void setSampleRemovedFlags( Collection<AuditEventValueObject> sampleRemovedFlags ) {

        this.sampleRemovedFlags = sampleRemovedFlags;
    }

    @Override
    public Class<? extends Securable> getSecurableClass() {
        if ( this.isSubset ) {
            return ExpressionExperimentSubSet.class;
        }
        return ExpressionExperiment.class;
    }

    public String getShortName() {
        return this.shortName;
    }

    public void setShortName( String shortName ) {
        this.shortName = shortName;
    }

    public String getSource() {
        return this.source;
    }

    public void setSource( String source ) {
        this.source = source;
    }

    /**
     * <p>
     * The ID of the source experiment, if this is an ExpressionExperimentSubSet; otherwise will be null.
     * </p>
     */
    public Long getSourceExperiment() {
        return this.sourceExperiment;
    }

    public void setSourceExperiment( Long sourceExperiment ) {
        this.sourceExperiment = sourceExperiment;
    }

    public String getTaxon() {
        return this.taxon;
    }

    public void setTaxon( String taxon ) {
        this.taxon = taxon;
    }

    public Long getTaxonId() {
        return taxonId;
    }

    public void setTaxonId( Long taxonId ) {
        this.taxonId = taxonId;
    }

    public String getTechnologyType() {
        return this.technologyType;
    }

    public void setTechnologyType( String technologyType ) {
        this.technologyType = technologyType;
    }

    @Override
    public boolean getUserCanWrite() {
        // FIXME consider making return type Boolean
        if ( this.currentUserHasWritePermission == null )
            return false;
        return this.currentUserHasWritePermission;
    }

    @Override
    public void setUserCanWrite( boolean userCanWrite ) {
        this.currentUserHasWritePermission = userCanWrite;
    }

    @Override
    public boolean getUserOwned() {
        // FIXME consider making return type Boolean
        if ( this.currentUserIsOwner == null )
            return false;
        return this.currentUserIsOwner;
    }

    @Override
    public void setUserOwned( boolean isUserOwned ) {
        this.currentUserIsOwner = isUserOwned;
    }

    public void setSubset( boolean isSubset ) {
        this.isSubset = isSubset;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata( String metadata ) {
        this.metadata = metadata;
    }

    public String getBatchConfound() {
        return batchConfound;
    }

    public void setBatchConfound( String batchConfound ) {
        this.batchConfound = batchConfound;
    }

    public String getBatchEffect() {
        return batchEffect;
    }

    public void setBatchEffect( String batchEffect ) {
        this.batchEffect = batchEffect;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }
}
