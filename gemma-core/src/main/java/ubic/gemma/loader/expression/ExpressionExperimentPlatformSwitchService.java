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
package ubic.gemma.loader.expression;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.analysis.service.ExpressionExperimentVectorManipulatingService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.ExpressionExperimentPlatformSwitchEvent;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * Switch an expression experiment from one array design to another. This is valuable when the EE uses more than on AD,
 * and a merged AD exists. The following steps are needed:
 * <ul>
 * <li>For each array design, for each probe, identify the matching probe on the merged AD. Have to deal with situation
 * <li>where more than one occurrence of each sequence is found.
 * <li>all DEDVs must be switched to use the new AD's design elements
 * <li>all bioassays must be switched to the new AD.
 * <li>update the EE description
 * <li>commit changes.
 * </ul>
 * 
 * @author pavlidis
 * @version $Id$
 */
@Service
public class ExpressionExperimentPlatformSwitchService extends ExpressionExperimentVectorManipulatingService {

    /**
     * Used to identify design elements that have no sequence associated with them.
     */
    private static BioSequence NULL_BIOSEQUENCE;

    static {
        NULL_BIOSEQUENCE = BioSequence.Factory.newInstance();
        NULL_BIOSEQUENCE.setName( "______NULL______" );
        NULL_BIOSEQUENCE.setId( -1L );
    }

    private static Log log = LogFactory.getLog( ExpressionExperimentPlatformSwitchService.class.getName() );

    @Autowired
    ExpressionExperimentService expressionExperimentService;

    @Autowired
    ArrayDesignService arrayDesignService;

    @Autowired
    BioAssayService bioAssayService;

    @Autowired
    private AuditTrailService auditTrailService;

    public void setAuditTrailService( AuditTrailService auditTrailService ) {
        this.auditTrailService = auditTrailService;
    }

    /**
     * @param arrayDesign
     */
    private void audit( ExpressionExperiment ee, String note ) {
        AuditEventType eventType = ExpressionExperimentPlatformSwitchEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( ee, eventType, note );
    }

    /**
     * @param expExp
     * @param arrayDesign
     */
    public void switchExperimentToMergedPlatform( ExpressionExperiment expExp ) {
        ArrayDesign arrayDesign = locateMergedDesign( expExp );
        if ( arrayDesign == null )
            throw new IllegalArgumentException( "Experiment has no merged design to switch to" );
        this.switchExperimentToArrayDesign( expExp, arrayDesign );
    }

    /**
     * If you know the arraydesigns are already in a merged state, you should use switchExperimentToMergedPlatform
     * 
     * @param expExp
     * @param arrayDesign The array design to switch to. If some samples already use that array design, nothing will be
     *        changed for them.
     */
    @SuppressWarnings("unchecked")
    public void switchExperimentToArrayDesign( ExpressionExperiment expExp, ArrayDesign arrayDesign ) {
        assert arrayDesign != null;

        // get relation between sequence and designelements.
        Map<BioSequence, Collection<DesignElement>> designElementMap = new HashMap<BioSequence, Collection<DesignElement>>();
        Collection<DesignElement> elsWithNoSeq = new HashSet<DesignElement>();
        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            BioSequence bs = cs.getBiologicalCharacteristic();
            if ( bs == null ) {
                elsWithNoSeq.add( cs );
            } else {
                if ( !designElementMap.containsKey( bs ) ) {
                    designElementMap.put( bs, new HashSet<DesignElement>() );
                }
                designElementMap.get( bs ).add( cs );
            }
        }

        log.info( elsWithNoSeq.size()
                + " composite sequences on the new array design have no biologicalcharacteristic." );
        designElementMap.put( NULL_BIOSEQUENCE, elsWithNoSeq );

        Collection<ArrayDesign> oldArrayDesigns = expressionExperimentService.getArrayDesignsUsed( expExp );
        Collection<DesignElement> usedDesignElements = new HashSet<DesignElement>();
        for ( ArrayDesign oldAd : oldArrayDesigns ) {
            if ( oldAd.equals( arrayDesign ) ) continue; // no need to switch

            oldAd = arrayDesignService.thaw( oldAd );

            if ( oldAd.getCompositeSequences().size() == 0 ) {
                throw new IllegalStateException( oldAd + " has no composite sequences" );
            }

            Collection<QuantitationType> qts = expressionExperimentService.getQuantitationTypes( expExp, oldAd );
            log.info( "Processing " + qts.size() + " quantitation types for vectors on " + oldAd );
            for ( QuantitationType type : qts ) {

                log.info( "Processing " + type + " for vectors run on " + oldAd );

                // use each design element only once per quantitation type per array design.
                usedDesignElements.clear();

                Collection<? extends DesignElementDataVector> vectorsForQt = getVectorsForOneQuantitationType( oldAd,
                        type );

                if ( vectorsForQt == null || vectorsForQt.size() == 0 ) {
                    /*
                     * This can happen when the quantitation types vary for the array designs.
                     */
                    log.info( "No vectors for " + type + " on " + oldAd );
                    continue;
                }

                int count = 0;
                Class<? extends DesignElementDataVector> vectorClass = null;
                for ( DesignElementDataVector vector : vectorsForQt ) {

                    if ( vectorClass == null ) {
                        vectorClass = vector.getClass();
                    }

                    if ( !vector.getClass().equals( vectorClass ) ) {
                        throw new IllegalStateException( "Two types of vector for one quantitationtype: " + type );
                    }

                    // we're doing this by array design; nice to have a method to fetch those only, oh well.
                    if ( !vector.getDesignElement().getArrayDesign().equals( oldAd ) ) {
                        continue;
                    }

                    processVector( designElementMap, usedDesignElements, vector );

                    if ( ++count % 20000 == 0 ) {
                        log.info( "Switched " + count + " vectors for " + type );
                    }
                }

                log.info( "Updating " + count + " vectors for " + type );
                if ( vectorClass != null ) {
                    if ( vectorClass.equals( RawExpressionDataVector.class ) ) {
                        designElementDataVectorService.update( vectorsForQt );
                    } else {
                        processedExpressionDataVectorService
                                .update( ( Collection<ProcessedExpressionDataVector> ) vectorsForQt );
                    }
                }
            }
        }

        log.info( "Updating bioAssays ... " );
        for ( BioAssay assay : expExp.getBioAssays() ) {
            assay.setArrayDesignUsed( arrayDesign );
            bioAssayService.update( assay );
        }

        expExp.setDescription( expExp.getDescription() + " [Switched to use " + arrayDesign.getShortName()
                + " by Gemma]" );
        expressionExperimentService.update( expExp );
        log.info( "Done switching " + expExp );

        audit( expExp, "Switch to use " + arrayDesign.getShortName() );
    }

    /**
     * @param expExp
     * @return
     */
    private ArrayDesign locateMergedDesign( ExpressionExperiment expExp ) {
        // get the array designs for this EE
        ArrayDesign arrayDesign = null;
        Collection<ArrayDesign> oldArrayDesigns = expressionExperimentService.getArrayDesignsUsed( expExp );

        // find the AD they have been merged into, make sure it is exists and they are all merged into the same AD.
        for ( ArrayDesign design : oldArrayDesigns ) {
            ArrayDesign mergedInto = design.getMergedInto();
            mergedInto = arrayDesignService.thaw( mergedInto );

            if ( mergedInto == null ) {
                throw new IllegalArgumentException( design + " used by " + expExp
                        + " is not merged into another design" );
            }

            if ( arrayDesign == null ) {
                arrayDesign = mergedInto;
                arrayDesignService.thaw( arrayDesign );
            }

            if ( !mergedInto.equals( arrayDesign ) ) {
                throw new IllegalArgumentException( design + " used by " + expExp + " is not merged into "
                        + arrayDesign );
            }

        }
        return arrayDesign;
    }

    /**
     * @param designElementMap
     * @param usedDesignElements
     * @param vector
     * @throw IllegalStateException if there is no design element matching the vector's biosequence
     */
    private boolean processVector( Map<BioSequence, Collection<DesignElement>> designElementMap,
            Collection<DesignElement> usedDesignElements, DesignElementDataVector vector ) {
        CompositeSequence oldDe = ( CompositeSequence ) vector.getDesignElement();

        Collection<DesignElement> newElCandidates = null;
        BioSequence seq = oldDe.getBiologicalCharacteristic();
        if ( seq == null ) {
            newElCandidates = designElementMap.get( NULL_BIOSEQUENCE );
        } else {
            newElCandidates = designElementMap.get( seq );
        }

        boolean found = false;

        if ( newElCandidates != null ) {
            for ( DesignElement newEl : newElCandidates ) {
                if ( !usedDesignElements.contains( newEl ) ) {
                    vector.setDesignElement( newEl );
                    usedDesignElements.add( newEl );
                    found = true;
                    break;
                }
            }
        }

        if ( !found ) {
            throw new IllegalStateException( "No new design element available to match " + oldDe + " (" + seq + ")" );
        }

        return true;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setBioAssayService( BioAssayService bioAssayService ) {
        this.bioAssayService = bioAssayService;
    }

    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }
}
