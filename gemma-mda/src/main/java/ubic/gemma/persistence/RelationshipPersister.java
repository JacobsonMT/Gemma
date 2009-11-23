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
package ubic.gemma.persistence;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSetService;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService;
import ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.association.Gene2GOAssociationService;

/**
 * Persist objects like Gene2GOAssociation.
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class RelationshipPersister extends ExpressionPersister {

    @Autowired
    private Gene2GOAssociationService gene2GoAssociationService;

    @Autowired
    private ProbeCoexpressionAnalysisService probeCoexpressionAnalysisService;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    private GeneCoexpressionAnalysisService geneCoexpressionAnalysisService;

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;

    public RelationshipPersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.loader.util.persister.Persister#persist(java.lang.Object)
     */
    @Override
    public Object persist( Object entity ) {
        if ( entity == null ) return null;

        if ( entity instanceof Gene2GOAssociation ) {
            return persistGene2GOAssociation( ( Gene2GOAssociation ) entity );
        } else if ( entity instanceof ProbeCoexpressionAnalysis ) {
            return persistProbeCoexpressionAnalysis( ( ProbeCoexpressionAnalysis ) entity );
        } else if ( entity instanceof DifferentialExpressionAnalysis ) {
            return persistDifferentialExpressionAnalysis( ( DifferentialExpressionAnalysis ) entity );
        } else if ( entity instanceof GeneCoexpressionAnalysis ) {
            return persistGeneCoexpressionAnalysis( ( GeneCoexpressionAnalysis ) entity );
        } else if ( entity instanceof ExpressionExperimentSet ) {
            return persistExpressionExperimentSet( ( ExpressionExperimentSet ) entity );
        }
        return super.persist( entity );

    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.persistence.CommonPersister#persistOrUpdate(java.lang.Object)
     */
    @Override
    public Object persistOrUpdate( Object entity ) {
        if ( entity == null ) return null;
        return super.persistOrUpdate( entity );
    }

    public void setDifferentialExpressionAnalysisService(
            DifferentialExpressionAnalysisService differentialExpressionAnalysisService ) {
        this.differentialExpressionAnalysisService = differentialExpressionAnalysisService;
    }

    public void setExpressionExperimentSetService( ExpressionExperimentSetService expressionExperimentSetService ) {
        this.expressionExperimentSetService = expressionExperimentSetService;
    }

    /**
     * @param gene2GoAssociationService the gene2GoAssociationService to set
     */
    public void setGene2GoAssociationService( Gene2GOAssociationService gene2GoAssociationService ) {
        this.gene2GoAssociationService = gene2GoAssociationService;
    }

    public void setGeneCoexpressionAnalysisService( GeneCoexpressionAnalysisService geneCoexpressionAnalysisService ) {
        this.geneCoexpressionAnalysisService = geneCoexpressionAnalysisService;
    }

    public void setProbeCoexpressionAnalysisService( ProbeCoexpressionAnalysisService probeCoexpressionAnalysisService ) {
        this.probeCoexpressionAnalysisService = probeCoexpressionAnalysisService;
    }

    /**
     * @param entity
     * @return
     */
    protected DifferentialExpressionAnalysis persistDifferentialExpressionAnalysis(
            DifferentialExpressionAnalysis entity ) {
        if ( entity == null ) return null;
        if ( !isTransient( entity ) ) return entity;
        entity.setProtocol( persistProtocol( entity.getProtocol() ) );
        entity.setExpressionExperimentSetAnalyzed( persistExpressionExperimentSet( entity
                .getExpressionExperimentSetAnalyzed() ) );
        return differentialExpressionAnalysisService.create( entity );
    }

    protected ExpressionExperimentSet persistExpressionExperimentSet( ExpressionExperimentSet entity ) {
        if ( !isTransient( entity ) ) return entity;
        if ( entity.getExperiments().size() == 0 ) {
            throw new IllegalArgumentException( "Attempt to create an empty ExpressionExperimentSet." );
        }
        return expressionExperimentSetService.create( entity );
    }

    /**
     * @param association
     * @return
     */
    protected Gene2GOAssociation persistGene2GOAssociation( Gene2GOAssociation association ) {
        if ( association == null ) return null;
        if ( !isTransient( association ) ) return association;

        association.setGene( persistGene( association.getGene() ) );
        return gene2GoAssociationService.findOrCreate( association );
    }

    /**
     * @param entity
     * @return
     */
    protected GeneCoexpressionAnalysis persistGeneCoexpressionAnalysis( GeneCoexpressionAnalysis entity ) {
        if ( entity == null ) return null;
        if ( !isTransient( entity ) ) return entity;
        entity.setProtocol( persistProtocol( entity.getProtocol() ) );
        entity.setExpressionExperimentSetAnalyzed( persistExpressionExperimentSet( entity
                .getExpressionExperimentSetAnalyzed() ) );

        return geneCoexpressionAnalysisService.create( entity );
    }

    /**
     * @param entity
     * @return
     */
    protected ProbeCoexpressionAnalysis persistProbeCoexpressionAnalysis( ProbeCoexpressionAnalysis entity ) {
        if ( entity == null ) return null;
        if ( !isTransient( entity ) ) return entity;
        entity.setProtocol( persistProtocol( entity.getProtocol() ) );

        entity.setExpressionExperimentSetAnalyzed( persistExpressionExperimentSet( entity
                .getExpressionExperimentSetAnalyzed() ) );

        return probeCoexpressionAnalysisService.create( entity );
    }

}
