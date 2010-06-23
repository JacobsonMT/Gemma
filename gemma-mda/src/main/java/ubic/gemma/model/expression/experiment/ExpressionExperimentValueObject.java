/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
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

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionSummaryValueObject;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventValueObject;

/**
 * @author kelsey
 * @version
 */
public class ExpressionExperimentValueObject implements java.io.Serializable {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -5678747537830051610L;

    private String accession;

    private Integer arrayDesignCount;

    private Date autoTagDate;

    private Integer bioAssayCount;

    private Integer bioMaterialCount = null;

    private String clazz;

    private Integer coexpressionLinkCount = null;

    private Boolean currentUserHasWritePermission = false;

    private Date dateArrayDesignLastUpdated;

    private Date dateCached;

    private Date dateCreated;

    private Date dateDifferentialAnalysis;

    private Date dateLastUpdated;

    private Date dateLinkAnalysis;

    private Date dateMissingValueAnalysis;

    private Date dateProcessedDataVectorComputation;

    private Integer designElementDataVectorCount;

    private String differentialAnalysisEventType;

    private Long differentialExpressionAnalysisId;

    private Collection<DifferentialExpressionSummaryValueObject> diffExpressedProbes;

    private Long experimentalDesign;

    private String externalDatabase;

    private String externalUri;

    private Boolean hasBothIntensities = false;

    private Boolean hasEitherIntensity = false;

    private Boolean hasProbeSpecificForQueryGene;

    private Long id;

    private String investigators;

    private Boolean isPublic = true;

    private boolean isShared = false;

    private String linkAnalysisEventType;

    private Double minPvalue;

    private String missingValueAnalysisEventType;

    private String name;

    private Integer numAnnotations;

    private Integer numPopulatedFactors;

    private String owner;

    private String processedDataVectorComputationEventType;

    private Integer processedExpressionVectorCount = null;

    private Integer pubmedId;

    private Integer rawCoexpressionLinkCount = null;

    private Collection<AuditEventValueObject> sampleRemovedFlags;

    private String shortName;

    private String source;

    private Long sourceExperiment;

    private String taxon;

    private Long taxonId;

    private String technologyType;

    private AuditEventValueObject troubleFlag;

    private AuditEventValueObject validatedFlag;

    private AuditEventValueObject validatedAnnotations;

    public ExpressionExperimentValueObject() {
    }

    /**
     * Copies constructor from other ExpressionExperimentValueObject
     * 
     * @param otherBean, cannot be <code>null</code>
     * @throws NullPointerException if the argument is <code>null</code>
     */
    public ExpressionExperimentValueObject( ExpressionExperimentValueObject otherBean ) {
        this( otherBean.getId(), otherBean.getName(), otherBean.getExternalDatabase(), otherBean.getExternalUri(),
                otherBean.getSource(), otherBean.getAccession(), otherBean.getBioAssayCount(), otherBean.getTaxon(),
                otherBean.getTaxonId(), otherBean.getBioMaterialCount(), otherBean.getDesignElementDataVectorCount(),
                otherBean.getArrayDesignCount(), otherBean.getShortName(), otherBean.getLinkAnalysisEventType(),
                otherBean.getDateArrayDesignLastUpdated(), otherBean.getValidatedFlag(), otherBean.getTechnologyType(),
                otherBean.isHasBothIntensities(), otherBean.getNumAnnotations(), otherBean.getNumPopulatedFactors(),
                otherBean.getDateDifferentialAnalysis(), otherBean.getDifferentialAnalysisEventType(), otherBean
                        .getSampleRemovedFlags(), otherBean.isIsPublic(), otherBean.isCurrentUserHasWritePermission(),
                otherBean.getClazz(), otherBean.getSourceExperiment(), otherBean.getDifferentialExpressionAnalysisId(),
                otherBean.getPubmedId(), otherBean.getInvestigators(), otherBean.getOwner(),
                otherBean.getDateCreated(), otherBean.getTroubleFlag(), otherBean.getCoexpressionLinkCount(), otherBean
                        .getProcessedDataVectorComputationEventType(), otherBean.getMissingValueAnalysisEventType(),
                otherBean.getDateLinkAnalysis(), otherBean.getRawCoexpressionLinkCount(), otherBean
                        .getDateProcessedDataVectorComputation(), otherBean.getDateMissingValueAnalysis(), otherBean
                        .getProcessedExpressionVectorCount(), otherBean.getDateLastUpdated(),
                otherBean.getDateCached(), otherBean.getHasProbeSpecificForQueryGene(), otherBean.getMinPvalue(),
                otherBean.getHasEitherIntensity(), otherBean.getDiffExpressedProbes(), otherBean
                        .getExperimentalDesign(), otherBean.getAutoTagDate(), otherBean.getValidatedAnnotations() );
    }

    public ExpressionExperimentValueObject( Long id, String name, String externalDatabase, String externalUri,
            String source, String accession, Integer bioAssayCount, String taxon, Long taxonId,
            Integer bioMaterialCount, Integer designElementDataVectorCount, Integer arrayDesignCount, String shortName,
            String linkAnalysisEventType, Date dateArrayDesignLastUpdated, AuditEventValueObject validatedFlag,
            String technologyType, boolean hasBothIntensities, Integer numAnnotations, Integer numPopulatedFactors,
            Date dateDifferentialAnalysis, String differentialAnalysisEventType,
            Collection<AuditEventValueObject> sampleRemovedFlags, boolean isPublic,
            boolean currentUserHasWritePermission, String clazz, Long sourceExperiment,
            Long differentialExpressionAnalysisId, Integer pubmedId, String investigators, String owner,
            Date dateCreated, AuditEventValueObject troubleFlag, Integer coexpressionLinkCount,
            String processedDataVectorComputationEventType, String missingValueAnalysisEventType,
            Date dateLinkAnalysis, Integer rawCoexpressionLinkCount, Date dateProcessedDataVectorComputation,
            Date dateMissingValueAnalysis, Integer processedExpressionVectorCount, Date dateLastUpdated,
            Date dateCached, Boolean hasProbeSpecificForQueryGene, Double minPvalue, Boolean hasEitherIntensity,
            Collection<DifferentialExpressionSummaryValueObject> probeIds, Long experimentalDesign, Date autoTagDate,
            AuditEventValueObject validatedAnnotations ) {
        this.id = id;
        this.name = name;
        this.externalDatabase = externalDatabase;
        this.externalUri = externalUri;
        this.source = source;
        this.accession = accession;
        this.bioAssayCount = bioAssayCount;
        this.taxon = taxon;
        this.taxonId = taxonId;
        this.bioMaterialCount = bioMaterialCount;
        this.designElementDataVectorCount = designElementDataVectorCount;
        this.arrayDesignCount = arrayDesignCount;
        this.shortName = shortName;
        this.linkAnalysisEventType = linkAnalysisEventType;
        this.dateArrayDesignLastUpdated = dateArrayDesignLastUpdated;
        this.validatedFlag = validatedFlag;
        this.technologyType = technologyType;
        this.hasBothIntensities = hasBothIntensities;
        this.numAnnotations = numAnnotations;
        this.numPopulatedFactors = numPopulatedFactors;
        this.dateDifferentialAnalysis = dateDifferentialAnalysis;
        this.differentialAnalysisEventType = differentialAnalysisEventType;
        this.sampleRemovedFlags = sampleRemovedFlags;
        this.isPublic = isPublic;
        this.currentUserHasWritePermission = currentUserHasWritePermission;
        this.clazz = clazz;
        this.sourceExperiment = sourceExperiment;
        this.differentialExpressionAnalysisId = differentialExpressionAnalysisId;
        this.pubmedId = pubmedId;
        this.investigators = investigators;
        this.owner = owner;
        this.dateCreated = dateCreated;
        this.troubleFlag = troubleFlag;
        this.coexpressionLinkCount = coexpressionLinkCount;
        this.processedDataVectorComputationEventType = processedDataVectorComputationEventType;
        this.missingValueAnalysisEventType = missingValueAnalysisEventType;
        this.dateLinkAnalysis = dateLinkAnalysis;
        this.rawCoexpressionLinkCount = rawCoexpressionLinkCount;
        this.dateProcessedDataVectorComputation = dateProcessedDataVectorComputation;
        this.dateMissingValueAnalysis = dateMissingValueAnalysis;
        this.processedExpressionVectorCount = processedExpressionVectorCount;
        this.dateLastUpdated = dateLastUpdated;
        this.dateCached = dateCached;
        this.hasProbeSpecificForQueryGene = hasProbeSpecificForQueryGene;
        this.minPvalue = minPvalue;
        this.hasEitherIntensity = hasEitherIntensity;
        this.diffExpressedProbes = probeIds;
        this.experimentalDesign = experimentalDesign;
        this.validatedAnnotations = validatedAnnotations;
        this.autoTagDate = autoTagDate;
    }

    /**
     * @param sampleRemovedFlags
     */
    public void auditEvents2SampleRemovedFlags( Collection<AuditEvent> s ) {
        Collection<AuditEventValueObject> converted = new HashSet<AuditEventValueObject>();

        for ( AuditEvent ae : s ) {
            converted.add( new AuditEventValueObject( ae ) );
        }

        this.sampleRemovedFlags = converted;
    }

    /**
     * Copies all properties from the argument value object into this value object.
     */
    public void copy( ExpressionExperimentValueObject otherBean ) {
        if ( otherBean != null ) {
            this.setId( otherBean.getId() );
            this.setName( otherBean.getName() );
            this.setExternalDatabase( otherBean.getExternalDatabase() );
            this.setExternalUri( otherBean.getExternalUri() );
            this.setSource( otherBean.getSource() );
            this.setAccession( otherBean.getAccession() );
            this.setBioAssayCount( otherBean.getBioAssayCount() );
            this.setTaxon( otherBean.getTaxon() );
            this.setBioMaterialCount( otherBean.getBioMaterialCount() );
            this.setDesignElementDataVectorCount( otherBean.getDesignElementDataVectorCount() );
            this.setArrayDesignCount( otherBean.getArrayDesignCount() );
            this.setShortName( otherBean.getShortName() );
            this.setLinkAnalysisEventType( otherBean.getLinkAnalysisEventType() );
            this.setDateArrayDesignLastUpdated( otherBean.getDateArrayDesignLastUpdated() );
            this.setValidatedFlag( otherBean.getValidatedFlag() );
            this.setTechnologyType( otherBean.getTechnologyType() );
            this.setHasBothIntensities( otherBean.isHasBothIntensities() );
            this.setNumAnnotations( otherBean.getNumAnnotations() );
            this.setNumPopulatedFactors( otherBean.getNumPopulatedFactors() );
            this.setDateDifferentialAnalysis( otherBean.getDateDifferentialAnalysis() );
            this.setDifferentialAnalysisEventType( otherBean.getDifferentialAnalysisEventType() );
            this.setSampleRemovedFlags( otherBean.getSampleRemovedFlags() );
            this.setIsPublic( otherBean.isIsPublic() );
            this.setClazz( otherBean.getClazz() );
            this.setSourceExperiment( otherBean.getSourceExperiment() );
            this.setDifferentialExpressionAnalysisId( otherBean.getDifferentialExpressionAnalysisId() );
            this.setPubmedId( otherBean.getPubmedId() );
            this.setInvestigators( otherBean.getInvestigators() );
            this.setOwner( otherBean.getOwner() );
            this.setDateCreated( otherBean.getDateCreated() );
            this.setTroubleFlag( otherBean.getTroubleFlag() );
            this.setCoexpressionLinkCount( otherBean.getCoexpressionLinkCount() );
            this.setProcessedDataVectorComputationEventType( otherBean.getProcessedDataVectorComputationEventType() );
            this.setMissingValueAnalysisEventType( otherBean.getMissingValueAnalysisEventType() );
            this.setDateLinkAnalysis( otherBean.getDateLinkAnalysis() );
            this.setRawCoexpressionLinkCount( otherBean.getRawCoexpressionLinkCount() );
            this.setDateProcessedDataVectorComputation( otherBean.getDateProcessedDataVectorComputation() );
            this.setDateMissingValueAnalysis( otherBean.getDateMissingValueAnalysis() );
            this.setProcessedExpressionVectorCount( otherBean.getProcessedExpressionVectorCount() );
            this.setDateLastUpdated( otherBean.getDateLastUpdated() );
            this.setDateCached( otherBean.getDateCached() );
            this.setHasProbeSpecificForQueryGene( otherBean.getHasProbeSpecificForQueryGene() );
            this.setMinPvalue( otherBean.getMinPvalue() );
            this.setDiffExpressedProbes( otherBean.getDiffExpressedProbes() );
        }
    }

    /**
     * 
     */
    public String getAccession() {
        return this.accession;
    }

    /**
     * 
     */
    public Integer getArrayDesignCount() {
        return this.arrayDesignCount;
    }

    /**
     * @return the autoTagDate
     */
    public Date getAutoTagDate() {
        return autoTagDate;
    }

    /**
     * 
     */
    public Integer getBioAssayCount() {
        return this.bioAssayCount;
    }

    /**
     * 
     */
    public Integer getBioMaterialCount() {
        return this.bioMaterialCount;
    }

    /**
     * <p>
     * The type of BioAssaySet this represents.
     * </p>
     */
    public String getClazz() {
        return this.clazz;
    }

    /**
     * 
     */
    public Integer getCoexpressionLinkCount() {
        return this.coexpressionLinkCount;
    }

    /**
     * <p>
     * The date the array design associated with the experiment was last updated. If there are multiple array designs
     * this should be the date of the most recent modification of any of them. This is used to help flag experiments
     * that need re-analysis due to changes in the underlying array design(s)
     * </p>
     */
    public Date getDateArrayDesignLastUpdated() {
        return this.dateArrayDesignLastUpdated;
    }

    /**
     * <p>
     * The date this object was generated.
     * </p>
     */
    public Date getDateCached() {
        return this.dateCached;
    }

    /**
     * 
     */
    public Date getDateCreated() {
        return this.dateCreated;
    }

    /**
     * 
     */
    public Date getDateDifferentialAnalysis() {
        return this.dateDifferentialAnalysis;
    }

    /**
     * 
     */
    public Date getDateLastUpdated() {
        return this.dateLastUpdated;
    }

    /**
     * 
     */
    public Date getDateLinkAnalysis() {
        return this.dateLinkAnalysis;
    }

    /**
     * 
     */
    public Date getDateMissingValueAnalysis() {
        return this.dateMissingValueAnalysis;
    }

    /**
     * 
     */
    public Date getDateProcessedDataVectorComputation() {
        return this.dateProcessedDataVectorComputation;
    }

    /**
     * 
     */
    public Integer getDesignElementDataVectorCount() {
        return this.designElementDataVectorCount;
    }

    /**
     * 
     */
    public String getDifferentialAnalysisEventType() {
        return this.differentialAnalysisEventType;
    }

    /**
     * 
     */
    public Long getDifferentialExpressionAnalysisId() {
        return this.differentialExpressionAnalysisId;
    }

    public Collection<DifferentialExpressionSummaryValueObject> getDiffExpressedProbes() {
        return diffExpressedProbes;
    }

    public Long getExperimentalDesign() {
        return experimentalDesign;
    }

    /**
     * 
     */
    public String getExternalDatabase() {
        return this.externalDatabase;
    }

    /**
     * 
     */
    public String getExternalUri() {
        return this.externalUri;
    }

    /**
     * @return true if the experiment has any intensity information available. Relevant for two-channel studies.
     */
    public Boolean getHasEitherIntensity() {
        return hasEitherIntensity;
    }

    /**
     * <p>
     * Used in display of gene-wise analysis results.
     * </p>
     */
    public Boolean getHasProbeSpecificForQueryGene() {
        return this.hasProbeSpecificForQueryGene;
    }

    /**
     * 
     */
    public Long getId() {
        return this.id;
    }

    /**
     * 
     */
    public String getInvestigators() {
        return this.investigators;
    }

    /**
     * 
     */
    public String getLinkAnalysisEventType() {
        return this.linkAnalysisEventType;
    }

    /**
     * 
     */
    public Double getMinPvalue() {
        return this.minPvalue;
    }

    /**
     * 
     */
    public String getMissingValueAnalysisEventType() {
        return this.missingValueAnalysisEventType;
    }

    /**
     * 
     */
    public String getName() {
        return this.name;
    }

    /**
     * <p>
     * The number of terms (Characteristics) the experiment has to describe it.
     * </p>
     */
    public Integer getNumAnnotations() {
        return this.numAnnotations;
    }

    /**
     * <p>
     * The number of experimental factors the experiment has (counting those that are populated with biomaterials)
     * </p>
     */
    public Integer getNumPopulatedFactors() {
        return this.numPopulatedFactors;
    }

    /**
     * <p>
     * The user name of the experiment's owner, if any.
     * </p>
     */
    public String getOwner() {
        return this.owner;
    }

    /**
     * 
     */
    public String getProcessedDataVectorComputationEventType() {
        return this.processedDataVectorComputationEventType;
    }

    /**
     * 
     */
    public Integer getProcessedExpressionVectorCount() {
        return this.processedExpressionVectorCount;
    }

    /**
     * 
     */
    public Integer getPubmedId() {
        return this.pubmedId;
    }

    /**
     * <p>
     * The amount of raw links that the EE contributed to any of the coexpressed genes. by raw we mean before
     * filtering/stringency was applied.
     * </p>
     */
    public Integer getRawCoexpressionLinkCount() {
        return this.rawCoexpressionLinkCount;
    }

    /**
     * <p>
     * Details of samples that were removed (or marked as outliers). This can happen multiple times in the life of a
     * data set, so this is a collection of AuditEvents.
     * </p>
     */
    public Collection<AuditEventValueObject> getSampleRemovedFlags() {
        return this.sampleRemovedFlags;
    }

    /**
     * 
     */
    public String getShortName() {
        return this.shortName;
    }

    /**
     * 
     */
    public String getSource() {
        return this.source;
    }

    /**
     * <p>
     * The ID of the source experiment, if this is an ExpressionExperimentSubSet
     * </p>
     */
    public Long getSourceExperiment() {
        return this.sourceExperiment;
    }

    /**
     * 
     */
    public String getTaxon() {
        return this.taxon;
    }

    /**
     * @return the taxonId
     */
    public Long getTaxonId() {
        return taxonId;
    }

    /**
     * 
     */
    public String getTechnologyType() {
        return this.technologyType;
    }

    /**
     * 
     */
    public AuditEventValueObject getTroubleFlag() {
        return this.troubleFlag;
    }

    /**
     * 
     */
    public AuditEventValueObject getValidatedFlag() {
        return this.validatedFlag;
    }

    /**
     * @return the currentUserHasWritePermission
     */
    public boolean isCurrentUserHasWritePermission() {
        return currentUserHasWritePermission;
    }

    /**
     * 
     */
    public boolean isHasBothIntensities() {
        return this.hasBothIntensities;
    }

    /**
     * <p>
     * If true, this data set has been made public. If false, it is private and is only viewable by some users.
     * </p>
     */
    public boolean isIsPublic() {
        return this.isPublic;
    }

    public boolean isIsShared() {
        return this.isShared;
    }

    /**
     * @return the isPublic
     */
    public boolean isPublic() {
        return isPublic;
    }

    public boolean isShared() {
        return isShared;
    }

    public void setAccession( String accession ) {
        this.accession = accession;
    }

    public void setArrayDesignCount( Integer arrayDesignCount ) {
        this.arrayDesignCount = arrayDesignCount;
    }

    /**
     * @param date
     */
    public void setAutoTagDate( Date date ) {
        this.autoTagDate = date;
    }

    public void setBioAssayCount( Integer bioAssayCount ) {
        this.bioAssayCount = bioAssayCount;
    }

    public void setBioMaterialCount( Integer bioMaterialCount ) {
        this.bioMaterialCount = bioMaterialCount;
    }

    public void setClazz( String clazz ) {
        this.clazz = clazz;
    }

    public void setCoexpressionLinkCount( Integer coexpressionLinkCount ) {
        this.coexpressionLinkCount = coexpressionLinkCount;
    }

    /**
     * @param currentUserHasWritePermission the currentUserHasWritePermission to set
     */
    public void setCurrentUserHasWritePermission( boolean currentUserHasWritePermission ) {
        this.currentUserHasWritePermission = currentUserHasWritePermission;
    }

    public void setDateArrayDesignLastUpdated( Date dateArrayDesignLastUpdated ) {
        this.dateArrayDesignLastUpdated = dateArrayDesignLastUpdated;
    }

    public void setDateCached( Date dateCached ) {
        this.dateCached = dateCached;
    }

    public void setDateCreated( Date dateCreated ) {
        this.dateCreated = dateCreated;
    }

    public void setDateDifferentialAnalysis( Date dateDifferentialAnalysis ) {
        this.dateDifferentialAnalysis = dateDifferentialAnalysis;
    }

    public void setDateLastUpdated( Date dateLastUpdated ) {
        this.dateLastUpdated = dateLastUpdated;
    }

    public void setDateLinkAnalysis( Date dateLinkAnalysis ) {
        this.dateLinkAnalysis = dateLinkAnalysis;
    }

    public void setDateMissingValueAnalysis( Date dateMissingValueAnalysis ) {
        this.dateMissingValueAnalysis = dateMissingValueAnalysis;
    }

    public void setDateProcessedDataVectorComputation( Date dateProcessedDataVectorComputation ) {
        this.dateProcessedDataVectorComputation = dateProcessedDataVectorComputation;
    }

    public void setDesignElementDataVectorCount( Integer designElementDataVectorCount ) {
        this.designElementDataVectorCount = designElementDataVectorCount;
    }

    public void setDifferentialAnalysisEventType( String differentialAnalysisEventType ) {
        this.differentialAnalysisEventType = differentialAnalysisEventType;
    }

    public void setDifferentialExpressionAnalysisId( Long differentialExpressionAnalysisId ) {
        this.differentialExpressionAnalysisId = differentialExpressionAnalysisId;
    }

    public void setDiffExpressedProbes( Collection<DifferentialExpressionSummaryValueObject> diffExpressedProbes ) {
        this.diffExpressedProbes = diffExpressedProbes;
    }

    public void setExperimentalDesign( Long experimentalDesign ) {
        this.experimentalDesign = experimentalDesign;
    }

    public void setExternalDatabase( String externalDatabase ) {
        this.externalDatabase = externalDatabase;
    }

    public void setExternalUri( String externalUri ) {
        this.externalUri = externalUri;
    }

    public void setHasBothIntensities( boolean hasBothIntensities ) {
        this.hasBothIntensities = hasBothIntensities;
    }

    public void setHasEitherIntensity( Boolean hasEitherIntensity ) {
        this.hasEitherIntensity = hasEitherIntensity;
    }

    public void setHasProbeSpecificForQueryGene( Boolean hasProbeSpecificForQueryGene ) {
        this.hasProbeSpecificForQueryGene = hasProbeSpecificForQueryGene;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setInvestigators( String investigators ) {
        this.investigators = investigators;
    }

    public void setIsPublic( boolean isPublic ) {
        this.isPublic = isPublic;
    }

    public void setLinkAnalysisEventType( String linkAnalysisEventType ) {
        this.linkAnalysisEventType = linkAnalysisEventType;
    }

    public void setMinPvalue( Double minPvalue ) {
        this.minPvalue = minPvalue;
    }

    public void setMissingValueAnalysisEventType( String missingValueAnalysisEventType ) {
        this.missingValueAnalysisEventType = missingValueAnalysisEventType;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public void setNumAnnotations( Integer numAnnotations ) {
        this.numAnnotations = numAnnotations;
    }

    public void setNumPopulatedFactors( Integer numPopulatedFactors ) {
        this.numPopulatedFactors = numPopulatedFactors;
    }

    public void setOwner( String owner ) {
        this.owner = owner;
    }

    public void setProcessedDataVectorComputationEventType( String processedDataVectorComputationEventType ) {
        this.processedDataVectorComputationEventType = processedDataVectorComputationEventType;
    }

    public void setProcessedExpressionVectorCount( Integer processedExpressionVectorCount ) {
        this.processedExpressionVectorCount = processedExpressionVectorCount;
    }

    /**
     * @param isPublic the isPublic to set
     */
    public void setPublic( boolean isPublic ) {
        this.isPublic = isPublic;
    }

    public void setPubmedId( Integer pubmedId ) {
        this.pubmedId = pubmedId;
    }

    public void setRawCoexpressionLinkCount( Integer rawCoexpressionLinkCount ) {
        this.rawCoexpressionLinkCount = rawCoexpressionLinkCount;
    }

    public void setSampleRemovedFlags( Collection<AuditEventValueObject> sampleRemovedFlags ) {

        this.sampleRemovedFlags = sampleRemovedFlags;
    }

    public void setShared( boolean isShared ) {
        this.isShared = isShared;
    }

    public void setShortName( String shortName ) {
        this.shortName = shortName;
    }

    public void setSource( String source ) {
        this.source = source;
    }

    public void setSourceExperiment( Long sourceExperiment ) {
        this.sourceExperiment = sourceExperiment;
    }

    public void setTaxon( String taxon ) {
        this.taxon = taxon;
    }

    /**
     * @param taxonId the taxonId to set
     */
    public void setTaxonId( Long taxonId ) {
        this.taxonId = taxonId;
    }

    public void setTechnologyType( String technologyType ) {
        this.technologyType = technologyType;
    }

    public void setTroubleFlag( AuditEventValueObject troubleFlag ) {
        this.troubleFlag = troubleFlag;
    }

    public void setValidatedFlag( AuditEventValueObject validatedFlag ) {
        this.validatedFlag = validatedFlag;
    }

    /**
     * @param validatedAnnotations the validatedAnnotations to set
     */
    public void setValidatedAnnotations( AuditEventValueObject validatedAnnotations ) {
        this.validatedAnnotations = validatedAnnotations;
    }

    /**
     * @return the validatedAnnotations
     */
    public AuditEventValueObject getValidatedAnnotations() {
        return validatedAnnotations;
    }

}