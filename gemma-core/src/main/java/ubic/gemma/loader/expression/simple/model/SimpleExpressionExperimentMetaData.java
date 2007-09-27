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
package ubic.gemma.loader.expression.simple.model;

import java.util.Collection;

import org.biomage.AuditAndSecurity.Contact;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.genome.Taxon;

/**
 * Represents the basic data to enter about an expression experiment when starting from a delimited file of data
 * 
 * @author pavlidis
 * @version $Id$
 */
public class SimpleExpressionExperimentMetaData {

    private String name;

    private String shortName;

    private String description;

    /**
     * The person who loaded this data.
     */
    private Contact user;

    private String quantitationTypeName;

    private String quantitationTypeDescription;

    private String experimentalDesignName = "Unknown";

    private String experimentalDesignDescription = "No information available";

    private ScaleType scale;

    private GeneralType generalType;

    private StandardQuantitationType type;

    private Boolean isRatio = Boolean.FALSE;

    private DatabaseEntry externalReference;

    private String sourceUrl;

    private boolean probeIdsAreImageClones;

    private TechnologyType technologyType;

    /**
     * Publication reference.
     */
    private int pubMedId;

    Collection<ArrayDesign> arrayDesigns;

    Taxon taxon;

    public Collection<ArrayDesign> getArrayDesigns() {
        return this.arrayDesigns;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * @return the externalReference
     */
    public DatabaseEntry getExternalReference() {
        return this.externalReference;
    }

    /**
     * @return the generalType
     */
    public GeneralType getGeneralType() {
        return this.generalType;
    }

    /**
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the pubMedId
     */
    public Integer getPubMedId() {
        return this.pubMedId;
    }

    /**
     * @return the quantitationTypeDescription
     */
    public String getQuantitationTypeDescription() {
        return this.quantitationTypeDescription;
    }

    /**
     * @return the quantitationTypeName
     */
    public String getQuantitationTypeName() {
        return this.quantitationTypeName;
    }

    /**
     * @return the scale
     */
    public ScaleType getScale() {
        return this.scale;
    }

    public Taxon getTaxon() {
        return this.taxon;
    }

    /**
     * @return the type
     */
    public StandardQuantitationType getType() {
        return this.type;
    }

    /**
     * @return the user
     */
    public Contact getUser() {
        return this.user;
    }

    public void setArrayDesigns( Collection<ArrayDesign> arrayDesigns ) {
        this.arrayDesigns = arrayDesigns;
    }

    /**
     * @param description the description to set
     */
    public void setDescription( String description ) {
        this.description = description;
    }

    /**
     * @param externalReference the externalReference to set
     */
    public void setExternalReference( DatabaseEntry externalReference ) {
        this.externalReference = externalReference;
    }

    /**
     * @param generalType the generalType to set
     */
    public void setGeneralType( GeneralType generalType ) {
        this.generalType = generalType;
    }

    /**
     * @param name the name to set
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * @param pubMedId the pubMedId to set
     */
    public void setPubMedId( int pubMedId ) {
        this.pubMedId = pubMedId;
    }

    /**
     * @param quantitationTypeDescription the quantitationTypeDescription to set
     */
    public void setQuantitationTypeDescription( String quantitationTypeDescription ) {
        this.quantitationTypeDescription = quantitationTypeDescription;
    }

    /**
     * @param quantitationTypeName the quantitationTypeName to set
     */
    public void setQuantitationTypeName( String quantitationTypeName ) {
        this.quantitationTypeName = quantitationTypeName;
    }

    /**
     * @param scale the scale to set
     */
    public void setScale( ScaleType scale ) {
        this.scale = scale;
    }

    public void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
    }

    /**
     * @param type the type to set
     */
    public void setType( StandardQuantitationType type ) {
        this.type = type;
    }

    /**
     * @param user the user to set
     */
    public void setUser( Contact user ) {
        this.user = user;
    }

    public boolean isProbeIdsAreImageClones() {
        return probeIdsAreImageClones;
    }

    public void setProbeIdsAreImageClones( boolean probeIdsAreImageClones ) {
        this.probeIdsAreImageClones = probeIdsAreImageClones;
    }

    public String getExperimentalDesignDescription() {
        return experimentalDesignDescription;
    }

    public void setExperimentalDesignDescription( String experimentalDesignDescription ) {
        this.experimentalDesignDescription = experimentalDesignDescription;
    }

    public String getExperimentalDesignName() {
        return experimentalDesignName;
    }

    public void setExperimentalDesignName( String experimentalDesignName ) {
        this.experimentalDesignName = experimentalDesignName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl( String sourceUrl ) {
        this.sourceUrl = sourceUrl;
    }

    public TechnologyType getTechnologyType() {
        return technologyType;
    }

    public void setTechnologyType( TechnologyType technologyType ) {
        this.technologyType = technologyType;
    }

    public Boolean getIsRatio() {
        return isRatio;
    }

    public void setIsRatio( Boolean isRatio ) {
        this.isRatio = isRatio;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName( String shortName ) {
        this.shortName = shortName;
    }

}
