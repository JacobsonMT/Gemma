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
package ubic.gemma.web.controller.expression.experiment;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import ubic.gemma.annotation.geommtx.ExpressionExperimentAnnotator;
import ubic.gemma.job.AbstractTaskService;
import ubic.gemma.job.BackgroundJob;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicService;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.tasks.analysis.expression.AutoTaggerTask;

/**
 * Controller for methods involving annotation of experiments (and potentially other things); delegates to
 * OntologyService and the ExpressionExperimentAnnotator
 * 
 * @author paul
 * @version $Id$
 * @see ubic.gemma.web.controller.common.CharacteristicBrowserController for related methods.
 */
@Controller
public class AnnotationController extends AbstractTaskService {

    private class TaggerJob extends BackgroundJob<TaskCommand> {

        public TaggerJob( TaskCommand commandObj ) {
            super( commandObj );
        }

        @Override
        protected TaskResult processJob() {
            return autoTagTask.execute( this.command );
        }
    }

    @Autowired
    private AutoTaggerTask autoTagTask;

    @Autowired
    private BioMaterialService bioMaterialService;

    @Autowired
    private CharacteristicService characteristicService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private TaxonService taxonService;

    /**
     * @param eeId
     * @return taskId
     */
    public String autoTag( Long eeId ) {

        if ( eeId == null ) {
            throw new IllegalArgumentException( "Id cannot be null" );
        }

        if ( !ExpressionExperimentAnnotator.ready() ) {
            throw new RuntimeException( "Sorry, the auto-tagger is not available." );
        }

        return this.run( new TaskCommand( eeId ) );
    }

    public void createBiomaterialTag( Characteristic vc, Long id ) {
        BioMaterial bm = bioMaterialService.load( id );
        if ( bm == null ) {
            throw new IllegalArgumentException( "No such BioMaterial with id=" + id );
        }
        ontologyService.saveBioMaterialStatement( vc, bm );
    }

    /**
     * Ajax
     * 
     * @param vc . If the evidence code is null, it will be filled in with IC. A category and value must be provided.
     * @param id of the expression experiment
     */
    public void createExperimentTag( Characteristic vc, Long id ) {
        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            throw new IllegalArgumentException( "No such experiment with id=" + id );
        }
        ontologyService.saveExpressionExperimentStatement( vc, ee );

    }

    /**
     * @param givenQueryString
     * @param categoryUri
     * @param taxonId
     * @return
     */
    public Collection<Characteristic> findTerm( String givenQueryString, String categoryUri, Long taxonId ) {
        if ( StringUtils.isBlank( givenQueryString ) ) {
            return new HashSet<Characteristic>();
        }
        Taxon taxon = null;
        if ( taxonId != null ) {
            taxon = taxonService.load( taxonId );
        }
        return ontologyService.findExactTerm( givenQueryString, categoryUri, taxon );
    }

    public void removeBiomaterialTag( Characteristic vc, Long id ) {
        BioMaterial bm = bioMaterialService.load( id );
        if ( bm == null ) {
            throw new IllegalArgumentException( "No such BioMaterial with id=" + id );
        }
        ontologyService.removeBioMaterialStatement( vc.getId(), bm );
    }

    /**
     * Ajax.
     * 
     * @param characterIds
     * @param eeId
     */
    public void removeExperimentTag( Collection<Long> characterIds, Long eeId ) {

        ExpressionExperiment ee = expressionExperimentService.load( eeId );

        if ( ee == null ) {
            return;
        }

        Collection<Characteristic> current = ee.getCharacteristics();

        Collection<Characteristic> found = new HashSet<Characteristic>();

        for ( Characteristic characteristic : current ) {
            if ( characterIds.contains( characteristic.getId() ) ) found.add( characteristic );

        }

        for ( Characteristic characteristic : found ) {
            log.info( "Removing characteristic  from " + ee + " : " + characteristic );
        }

        current.removeAll( found );
        ee.setCharacteristics( current );
        expressionExperimentService.update( ee );

        for ( Long id : characterIds ) {
            characteristicService.delete( id );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.job.AbstractTaskService#getInProcessRunner(ubic.gemma.job.TaskCommand)
     */
    @Override
    protected BackgroundJob<?> getInProcessRunner( TaskCommand command ) {
        return new TaggerJob( command );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.job.AbstractTaskService#getSpaceRunner(ubic.gemma.job.TaskCommand)
     */
    @Override
    protected BackgroundJob<?> getSpaceRunner( TaskCommand command ) {
        return null;
    }

}
