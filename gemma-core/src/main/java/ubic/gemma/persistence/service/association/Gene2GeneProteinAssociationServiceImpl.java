package ubic.gemma.persistence.service.association;

import java.util.Collection;

import org.springframework.stereotype.Service;

import ubic.gemma.model.association.Gene2GeneProteinAssociation;
import ubic.gemma.model.genome.Gene;

/**
 * Gene2GeneProteinAssociationService class providing functionality for handling Gene2geneProteinAssociations
 * 
 * @author ldonnison
 *
 */
@Service
public class Gene2GeneProteinAssociationServiceImpl
        extends Gene2GeneProteinAssociationServiceBase {

    @Override
    protected Gene2GeneProteinAssociation handleCreate( Gene2GeneProteinAssociation gene2GeneProteinAssociation ) {
        return this.gene2GeneProteinAssociationDao().create( gene2GeneProteinAssociation );
    }

    @Override
    protected void handleDelete( Gene2GeneProteinAssociation association ) {
        this.gene2GeneProteinAssociationDao().remove( association );

    }

    @Override
    protected void handleDeleteAll( Collection<Gene2GeneProteinAssociation> associations ) {
        this.gene2GeneProteinAssociationDao().remove( associations );
    }

    @Override
    protected Gene2GeneProteinAssociation handleFind( Gene2GeneProteinAssociation gene2GeneProteinAssociation ) {
        return this.gene2GeneProteinAssociationDao().find( gene2GeneProteinAssociation );
    }

    @Override
    protected Collection<Gene2GeneProteinAssociation> handleFindProteinInteractionsForGene( Gene gene ) {
        return this.gene2GeneProteinAssociationDao().findProteinInteractionsForGene( gene );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Collection<Gene2GeneProteinAssociation> handleLoadAll() {
        return ( Collection<Gene2GeneProteinAssociation> ) this.gene2GeneProteinAssociationDao().loadAll();
    }

    @Override
    protected void handleThaw( Gene2GeneProteinAssociation gene2GeneProteinAssociation ) {
        this.gene2GeneProteinAssociationDao().thaw( gene2GeneProteinAssociation );
    }

}