/*
 * The Gemma project.
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
package ubic.gemma.model.expression.experiment;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.eventType.LinkAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.MissingValueAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.RankComputationEvent;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

/**
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService
 */
public class ExpressionExperimentServiceImpl extends
        ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindByExpressedGene(ubic.gemma.model.genome.Gene,
     *      double)
     */
    @Override
    protected Collection handleFindByExpressedGene( Gene gene, double rank ) throws Exception {
        return this.getExpressionExperimentDao().findByExpressedGene( gene, rank );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindByGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Collection handleFindByGene( Gene gene ) throws Exception {
        return this.getExpressionExperimentDao().findByGene( gene );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindByTaxon(ubic.gemma.model.genome.Taxon)
     */
    @Override
    protected Collection handleFindByTaxon( Taxon taxon ) throws Exception {
        return this.getExpressionExperimentDao().findByTaxon( taxon );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleLoadAllValueObjects()
     */
    @Override
    protected Collection handleLoadAllValueObjects() throws Exception {
        return this.getExpressionExperimentDao().loadAllValueObjects();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindByInvestigator(ubic.gemma.model.common.auditAndSecurity.Contact)
     */
    @Override
    protected Collection handleFindByInvestigator( Contact investigator ) throws Exception {
        return this.getExpressionExperimentDao().findByInvestigator( investigator );
    }

    @Override
    protected Integer handleCountAll() throws Exception {
        return this.getExpressionExperimentDao().countAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindByAccession(ubic.gemma.model.common.description.DatabaseEntry)
     */
    @Override
    protected ExpressionExperiment handleFindByAccession( DatabaseEntry accession ) throws Exception {
        return this.getExpressionExperimentDao().findByAccession( accession );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindByName(java.lang.String)
     */
    @Override
    protected ExpressionExperiment handleFindByName( String name ) throws Exception {
        return this.getExpressionExperimentDao().findByName( name );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindById(java.lang.Long)
     */
    @Override
    protected ExpressionExperiment handleFindById( Long id ) throws Exception {
        return this.getExpressionExperimentDao().findById( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindOrCreate(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected ExpressionExperiment handleFindOrCreate( ExpressionExperiment expressionExperiment ) throws Exception {
        return this.getExpressionExperimentDao().findOrCreate( expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleUpdate(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected void handleUpdate( ExpressionExperiment expressionExperiment ) throws Exception {
        this.getExpressionExperimentDao().update( expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleLoadAll()
     */
    @Override
    protected Collection handleLoadAll() throws Exception {
        return this.getExpressionExperimentDao().loadAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleDelete(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected void handleDelete( ExpressionExperiment ee ) throws Exception {
        this.getExpressionExperimentDao().remove( ee );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleLoad(java.lang.Long)
     */
    @Override
    protected ExpressionExperiment handleLoad( Long id ) throws Exception {
        return ( ExpressionExperiment ) this.getExpressionExperimentDao().load( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleCreate(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected ExpressionExperiment handleCreate( ExpressionExperiment expressionExperiment ) throws Exception {
        return ( ExpressionExperiment ) this.getExpressionExperimentDao().create( expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFind(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected ExpressionExperiment handleFind( ExpressionExperiment expressionExperiment ) throws Exception {
        return this.getExpressionExperimentDao().find( expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetQuantitationTypeCountById(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected Map handleGetQuantitationTypeCountById( Long Id ) throws Exception {
        return this.getExpressionExperimentDao().getQuantitationTypeCountById( Id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetArrayDesignsUsed(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected Collection<ArrayDesign> handleGetArrayDesignsUsed( ExpressionExperiment expressionExperiment ) {
        Collection<ArrayDesign> result = new HashSet<ArrayDesign>();
        this.getExpressionExperimentDao().thawBioAssays( expressionExperiment );
        for ( BioAssay ba : expressionExperiment.getBioAssays() ) {
            result.add( ba.getArrayDesignUsed() );
        }
        return result;
    }

    @Override
    protected long handleGetDesignElementDataVectorCountById( long id ) throws Exception {
        return this.getExpressionExperimentDao().getDesignElementDataVectorCountById( id );
    }

    @Override
    protected ExpressionExperimentValueObject handleToExpressionExperimentValueObject(
            ExpressionExperiment expressionExperiment ) throws Exception {
        return this.getExpressionExperimentDao().toExpressionExperimentValueObject( expressionExperiment );
    }

    @Override
    protected ExpressionExperiment handleFindByShortName( String shortName ) throws Exception {
        return this.getExpressionExperimentDao().findByShortName( shortName );
    }

    @Override
    protected void handleThaw( ExpressionExperiment expressionExperiment ) throws Exception {
        this.getExpressionExperimentDao().thaw( expressionExperiment );
    }

    @Override
    protected void handleThawLite( ExpressionExperiment expressionExperiment ) throws Exception {
        this.getExpressionExperimentDao().thawBioAssays( expressionExperiment );
    }

    /*
     * (non-Javadoc) This only returns 1 taxon, the 1st taxon as decided by the join which ever that is. The good news
     * is as a buisness rule we only allow 1 taxon per EE.
     */

    @Override
    protected Taxon handleGetTaxon( Long id ) {
        return this.getExpressionExperimentDao().getTaxon( id );
    }

    @Override
    protected Collection handleGetQuantitationTypes( ExpressionExperiment expressionExperiment ) throws Exception {
        return this.getExpressionExperimentDao().getQuantitationTypes( expressionExperiment );
    }

    @Override
    protected Collection handleGetSamplingOfVectors( ExpressionExperiment expressionExperiment,
            QuantitationType quantitationType, Integer limit ) throws Exception {
        return this.getExpressionExperimentDao().getSamplingOfVectors( expressionExperiment, quantitationType, limit );
    }

    @Override
    protected Collection handleGetDesignElementDataVectors( ExpressionExperiment expressionExperiment,
            Collection designElements, QuantitationType quantitationType ) throws Exception {
        return this.getExpressionExperimentDao().getDesignElementDataVectors( expressionExperiment, designElements,
                quantitationType );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetPerTaxonCount()
     */
    @Override
    protected Map handleGetPerTaxonCount() throws Exception {
        return this.getExpressionExperimentDao().getPerTaxonCount();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleLoadValueObjects(java.util.Collection)
     */
    @Override
    protected Collection handleLoadValueObjects( Collection ids ) throws Exception {
        return this.getExpressionExperimentDao().loadValueObjects( ids );
    }

    @Override
    protected Collection handleGetQuantitationTypes( ExpressionExperiment expressionExperiment, ArrayDesign arrayDesign )
            throws Exception {
        return this.getExpressionExperimentDao().getQuantitationTypes( expressionExperiment, arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetBioMaterialCount(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected long handleGetBioMaterialCount( ExpressionExperiment expressionExperiment ) throws Exception {
        return this.getExpressionExperimentDao().getBioMaterialCount( expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetPreferredDesignElementDataVectorCount(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected long handleGetPreferredDesignElementDataVectorCount( ExpressionExperiment expressionExperiment )
            throws Exception {
        return this.getExpressionExperimentDao().getPreferredDesignElementDataVectorCount( expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleLoad(java.util.Collection)
     */
    @Override
    protected Collection handleLoad( Collection ids ) throws Exception {
        return this.getExpressionExperimentDao().load( ids );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetDesignElementDataVectors(ubic.gemma.model.expression.experiment.ExpressionExperiment,
     *      java.util.Collection)
     */
    @Override
    protected Collection handleGetDesignElementDataVectors( ExpressionExperiment expressionExperiment,
            Collection quantitationTypes ) throws Exception {
        return this.getExpressionExperimentDao().getDesignElementDataVectors( expressionExperiment, quantitationTypes );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetLastLinkAnalysis(java.util.Collection)
     */
    @Override
    protected Map handleGetLastLinkAnalysis( Collection ids ) throws Exception {
        Map<Long, Collection<AuditEvent>> eventMap = this.getExpressionExperimentDao().getAuditEvents( ids );
        Map<Long, AuditEvent> lastEventMap = new HashMap<Long, AuditEvent>();
        // remove all AuditEvents that are not LinkAnalysis events
        Set<Long> keys = eventMap.keySet();
        for ( Long key : keys ) {
            Collection<AuditEvent> events = eventMap.get( key );
            AuditEvent lastEvent = null;
            if ( events == null ) {
                lastEventMap.put( key, null );
            } else {
                for ( AuditEvent event : events ) {
                    if ( event.getEventType() != null && event.getEventType() instanceof LinkAnalysisEvent ) {
                        if ( lastEvent == null ) {
                            lastEvent = event;
                            continue;
                        } else if ( lastEvent.getDate().before( event.getDate() ) ) {
                            lastEvent = event;
                        }
                    }
                }
                lastEventMap.put( key, lastEvent );
            }
        }
        return lastEventMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetLastMissingValueAnalysis(java.util.Collection)
     */
    @Override
    protected Map handleGetLastMissingValueAnalysis( Collection ids ) throws Exception {
        Map<Long, Collection<AuditEvent>> eventMap = this.getExpressionExperimentDao().getAuditEvents( ids );
        Map<Long, AuditEvent> lastEventMap = new HashMap<Long, AuditEvent>();
        // remove all AuditEvents that are not LinkAnalysis events
        Set<Long> keys = eventMap.keySet();
        for ( Long key : keys ) {
            Collection<AuditEvent> events = eventMap.get( key );
            AuditEvent lastEvent = null;
            if ( events == null ) {
                lastEventMap.put( key, null );
            } else {
                for ( AuditEvent event : events ) {
                    if ( event.getEventType() != null && event.getEventType() instanceof MissingValueAnalysisEvent ) {
                        if ( lastEvent == null ) {
                            lastEvent = event;
                            continue;
                        } else if ( lastEvent.getDate().before( event.getDate() ) ) {
                            lastEvent = event;
                        }
                    }
                }
                lastEventMap.put( key, lastEvent );
            }
        }
        return lastEventMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetLastRankComputation(java.util.Collection)
     */
    @Override
    protected Map handleGetLastRankComputation( Collection ids ) throws Exception {
        Map<Long, Collection<AuditEvent>> eventMap = this.getExpressionExperimentDao().getAuditEvents( ids );
        Map<Long, AuditEvent> lastEventMap = new HashMap<Long, AuditEvent>();
        // remove all AuditEvents that are not LinkAnalysis events
        Set<Long> keys = eventMap.keySet();
        for ( Long key : keys ) {
            Collection<AuditEvent> events = eventMap.get( key );
            AuditEvent lastEvent = null;
            if ( events == null ) {
                lastEventMap.put( key, null );
            } else {
                for ( AuditEvent event : events ) {
                    if ( event.getEventType() != null && event.getEventType() instanceof RankComputationEvent ) {
                        if ( lastEvent == null ) {
                            lastEvent = event;
                            continue;
                        } else if ( lastEvent.getDate().before( event.getDate() ) ) {
                            lastEvent = event;
                        }
                    }
                }
                lastEventMap.put( key, lastEvent );
            }
        }
        return lastEventMap;
    }

    /* (non-Javadoc)
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindByBibliographicReference(ubic.gemma.model.common.description.BibliographicReference)
     */
    @Override
    protected Collection handleFindByBibliographicReference( BibliographicReference bibRef ) throws Exception {
        return this.getExpressionExperimentDao().findByBibliographicReference( bibRef.getId() );
    }

}