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
package ubic.gemma.persistence.persister;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.persistence.service.association.Gene2GOAssociationDao;
import ubic.gemma.persistence.service.association.Gene2GeneProteinAssociationDao;
import ubic.gemma.persistence.service.association.TfGeneAssociationDao;
import ubic.gemma.persistence.service.analysis.expression.ExpressionExperimentSetDao;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis;
import ubic.gemma.persistence.service.analysis.expression.coexpression.CoexpressionAnalysisDao;
import ubic.gemma.model.association.*;
import ubic.gemma.model.expression.experiment.BioAssaySet;

import java.util.Collection;
import java.util.HashSet;

/**
 * Persist objects like Gene2GOAssociation.
 *
 * @author pavlidis
 */
public abstract class RelationshipPersister extends ExpressionPersister {

    @Autowired
    private Gene2GOAssociationDao gene2GoAssociationDao;

    @Autowired
    private CoexpressionAnalysisDao probeCoexpressionAnalysisDao;

    @Autowired
    private TfGeneAssociationDao tfGeneAssociationDao;

    @Autowired
    private ExpressionExperimentSetDao expressionExperimentSetDao;

    @Autowired
    private Gene2GeneProteinAssociationDao gene2GeneProteinAssociationDao;

    @Override
    @Transactional
    public Object persist( Object entity ) {
        if ( entity == null )
            return null;

        if ( entity instanceof Gene2GOAssociation ) {
            return persistGene2GOAssociation( ( Gene2GOAssociation ) entity );
        } else if ( entity instanceof CoexpressionAnalysis ) {
            return persistProbeCoexpressionAnalysis( ( CoexpressionAnalysis ) entity );
        } else if ( entity instanceof ExpressionExperimentSet ) {
            return persistExpressionExperimentSet( ( ExpressionExperimentSet ) entity );
        } else if ( entity instanceof Gene2GeneProteinAssociation ) {
            return persistGene2GeneProteinAssociation( ( Gene2GeneProteinAssociation ) entity );
        } else if ( entity instanceof TfGeneAssociation ) {
            return persistTfGeneAssociation( ( TfGeneAssociation ) entity );
        }
        return super.persist( entity );

    }

    @Override
    @Transactional
    public Object persistOrUpdate( Object entity ) {
        if ( entity == null )
            return null;
        return super.persistOrUpdate( entity );
    }

    private ExpressionExperimentSet persistExpressionExperimentSet( ExpressionExperimentSet entity ) {
        if ( !isTransient( entity ) )
            return entity;

        Collection<BioAssaySet> setMembers = new HashSet<>();

        for ( BioAssaySet baSet : entity.getExperiments() ) {
            if ( isTransient( baSet ) ) {
                baSet = ( BioAssaySet ) persist( baSet );
            }
            setMembers.add( baSet );
        }
        entity.getExperiments().clear();
        entity.getExperiments().addAll( setMembers );

        return expressionExperimentSetDao.create( entity );
    }

    private Gene2GOAssociation persistGene2GOAssociation( Gene2GOAssociation association ) {
        if ( association == null )
            return null;
        if ( !isTransient( association ) )
            return association;
        try {
            FieldUtils.writeField( association, "gene", persistGene( association.getGene() ), true );
        } catch ( IllegalAccessException e ) {
            e.printStackTrace();
        }
        return gene2GoAssociationDao.create( association );
    }

    private TfGeneAssociation persistTfGeneAssociation( TfGeneAssociation entity ) {
        if ( entity == null )
            return null;
        if ( !isTransient( entity ) )
            return entity;

        if ( isTransient( entity.getFirstGene() ) || isTransient( entity.getSecondGene() ) ) {
            throw new IllegalArgumentException(
                    "Associations can only be made between genes that already exist in the system" );
        }

        return tfGeneAssociationDao.create( entity );

    }

    private CoexpressionAnalysis persistProbeCoexpressionAnalysis( CoexpressionAnalysis entity ) {
        if ( entity == null )
            return null;
        if ( !isTransient( entity ) )
            return entity;
        entity.setProtocol( persistProtocol( entity.getProtocol() ) );
        if ( isTransient( entity.getExperimentAnalyzed() ) ) {
            throw new IllegalArgumentException( "Persist the experiment before running analyses on it" );
        }

        return probeCoexpressionAnalysisDao.create( entity );
    }

    /**
     * The persisting method for Gene2GeneProteinAssociation which validates the the Gene2GeneProteinAssociation does
     * not already exist in the system. If it does then the persisted object is returned
     *
     * @param gene2GeneProteinAssociation the object to persist
     * @return the persisted object
     */
    private Gene2GeneProteinAssociation persistGene2GeneProteinAssociation(
            Gene2GeneProteinAssociation gene2GeneProteinAssociation ) {
        if ( gene2GeneProteinAssociation == null )
            return null;
        if ( !isTransient( gene2GeneProteinAssociation ) )
            return gene2GeneProteinAssociation;

        // Deletes any old existing one.
        return gene2GeneProteinAssociationDao.create( gene2GeneProteinAssociation );
    }

}
