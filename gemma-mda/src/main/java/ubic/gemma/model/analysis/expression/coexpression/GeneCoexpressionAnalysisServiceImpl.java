/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.model.analysis.expression.coexpression;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

/**
 * @see ubic.gemma.model.analysis.GeneCoexpressionAnalysisService
 * @author paul
 * @version $Id$
 */
@Service
public class GeneCoexpressionAnalysisServiceImpl implements GeneCoexpressionAnalysisService {

    @Autowired
    private GeneCoexpressionAnalysisDao geneCoexpressionAnalysisDao;

    /*
     * @see
     * ubic.gemma.model.analysis.GeneCoexpressionAnalysisService#create(ubic.gemma.model.analysis.GeneCoexpressionAnalysis
     * )
     */
    @Override
    @Transactional
    public GeneCoexpressionAnalysis create( GeneCoexpressionAnalysis analysis ) {
        return this.getGeneCoexpressionAnalysisDao().create( analysis );
    }

    @Override
    @Transactional
    public void delete( GeneCoexpressionAnalysis toDelete ) {
        this.getGeneCoexpressionAnalysisDao().remove( toDelete );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneCoexpressionAnalysis> findByInvestigation( Investigation investigation ) {
        return this.getGeneCoexpressionAnalysisDao().findByInvestigation( investigation );
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public Map<Investigation, Collection<GeneCoexpressionAnalysis>> findByInvestigations(
            Collection<? extends Investigation> investigations ) {
        return this.getGeneCoexpressionAnalysisDao()
                .findByInvestigations( ( Collection<Investigation> ) investigations );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneCoexpressionAnalysis> findByName( String name ) {
        return this.getGeneCoexpressionAnalysisDao().findByName( name );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.GeneCoexpressionAnalysisServiceBase#handleFindByParentTaxon(ubic.gemma.model.genome
     * .Taxon)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<GeneCoexpressionAnalysis> findByParentTaxon( Taxon taxon ) {
        return this.getGeneCoexpressionAnalysisDao().findByParentTaxon( taxon );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.GeneCoexpressionAnalysisServiceBase#handleFindByTaxon(ubic.gemma.model.genome.Taxon)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<GeneCoexpressionAnalysis> findByTaxon( Taxon taxon ) {
        return this.getGeneCoexpressionAnalysisDao().findByTaxon( taxon );
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public GeneCoexpressionAnalysis findByUniqueInvestigations( Collection<? extends Investigation> investigations ) {

        Map<Investigation, Collection<GeneCoexpressionAnalysis>> anas = this.getGeneCoexpressionAnalysisDao()
                .findByInvestigations( ( Collection<Investigation> ) investigations );

        /*
         * Find an analysis that uses all the investigations.
         */

        for ( Investigation ee : investigations ) {

            if ( !anas.containsKey( ee ) ) {
                return null; // then there can be none meeting the criterion.
            }

            Collection<GeneCoexpressionAnalysis> analyses = anas.get( ee );
            for ( GeneCoexpressionAnalysis a : analyses ) {
                if ( a.getExpressionExperimentSetAnalyzed().getExperiments().size() == investigations.size()
                        && a.getExpressionExperimentSetAnalyzed().getExperiments().containsAll( investigations ) ) {
                    return a;
                }
            }
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public GeneCoexpressionAnalysis findCurrent( Taxon taxon ) {
        Collection<GeneCoexpressionAnalysis> analyses = null;
        if ( taxon.getIsSpecies() ) {
            analyses = findByTaxon( taxon );
        } else {
            analyses = findByParentTaxon( taxon );
        }

        if ( analyses.size() == 0 ) {
            return null;
        }

        for ( GeneCoexpressionAnalysis a : analyses ) {
            if ( a.getEnabled() ) {
                return a;
            }
        }

        return null;

    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> getDatasetsAnalyzed( GeneCoexpressionAnalysis analysis ) {
        return this.getGeneCoexpressionAnalysisDao().getDatasetsAnalyzed( analysis );
    }

    @Override
    @Transactional(readOnly = true)
    public int getNumDatasetsAnalyzed( GeneCoexpressionAnalysis analysis ) {
        return this.getGeneCoexpressionAnalysisDao().getNumDatasetsAnalyzed( analysis );
    }

    @Override
    @Transactional(readOnly = true)
    public GeneCoexpressionAnalysis load( Long id ) {
        return this.getGeneCoexpressionAnalysisDao().load( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.AnalysisServiceImpl#handleLoadAll()
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<GeneCoexpressionAnalysis> loadAll() {
        return ( Collection<GeneCoexpressionAnalysis> ) this.getGeneCoexpressionAnalysisDao().loadAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.AnalysisService#loadMyAnalyses()
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<GeneCoexpressionAnalysis> loadMyAnalyses() {
        return loadEnabled();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.AnalysisService#loadMySharedAnalyses()
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<GeneCoexpressionAnalysis> loadMySharedAnalyses() {
        return loadEnabled();
    }

    @Override
    @Transactional(readOnly = true)
    public void thaw( GeneCoexpressionAnalysis geneCoexpressionAnalysis ) {
        this.getGeneCoexpressionAnalysisDao().thaw( geneCoexpressionAnalysis );

    }

    @Override
    @Transactional
    public void update( GeneCoexpressionAnalysis geneCoExpressionAnalysis ) {
        this.getGeneCoexpressionAnalysisDao().update( geneCoExpressionAnalysis );
    }

    /**
     * Gets the reference to <code>geneCoexpressionAnalysis</code>'s DAO.
     */
    protected GeneCoexpressionAnalysisDao getGeneCoexpressionAnalysisDao() {
        return this.geneCoexpressionAnalysisDao;
    }

    /**
     * @return
     */
    @Transactional(readOnly = true)
    private Collection<GeneCoexpressionAnalysis> loadEnabled() {
        Collection<GeneCoexpressionAnalysis> all = ( Collection<GeneCoexpressionAnalysis> ) this
                .getGeneCoexpressionAnalysisDao().loadAll();

        for ( Iterator<GeneCoexpressionAnalysis> it = all.iterator(); it.hasNext(); ) {
            if ( !it.next().getEnabled() ) {
                it.remove();
            }
        }

        return all;
    }

}