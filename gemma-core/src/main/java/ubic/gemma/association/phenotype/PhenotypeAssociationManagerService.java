/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.association.phenotype;

import java.util.Collection;
import java.util.Set;

import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.TreeCharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ValidateEvidenceValueObject;

/**
 * High Level Service used to add Candidate Gene Management System capabilities
 * 
 * @author paul
 * @version $Id$
 */
public interface PhenotypeAssociationManagerService {

    /**
     * Links an Evidence to a Gene
     * 
     * @param geneNCBI The Gene NCBI we want to add the evidence
     * @param evidence The evidence
     * @return Status of the operation
     */
    public abstract ValidateEvidenceValueObject create( EvidenceValueObject evidence );

    /**
     * Return all evidence for a specific gene NCBI
     * 
     * @param geneNCBI The Evidence id
     * @return The Gene we are interested in
     */
    public abstract Collection<EvidenceValueObject> findEvidenceByGeneNCBI( Integer geneNCBI );

    /**
     * Return all evidence for a specific gene id
     * 
     * @param geneId The Evidence id
     * @return The Gene we are interested in
     */
    public abstract Collection<EvidenceValueObject> findEvidenceByGeneId( Long geneId );

    /**
     * Return all evidence for a specific gene id with evidence flagged, indicating more information
     * 
     * @param geneId The Evidence id
     * @param phenotypesValuesUri the chosen phenotypes
     * @return The Gene we are interested in
     */
    public abstract Collection<EvidenceValueObject> findEvidenceByGeneId( Long geneId, Set<String> phenotypesValuesUri );

    /**
     * Given an set of phenotypes returns the genes that have all those phenotypes or children phenotypes
     * 
     * @param phenotypesValuesUri the roots phenotype of the query
     * @return A collection of the genes found
     */
    public abstract Collection<GeneValueObject> findCandidateGenes( Set<String> phenotypesValuesUri );

    /**
     * Get all phenotypes linked to genes and count how many genes are link to each phenotype
     * 
     * @return A collection of the phenotypes with the gene occurence
     */
    public abstract Collection<CharacteristicValueObject> loadAllPhenotypes();

    /**
     * Removes an evidence
     * 
     * @param id The Evidence database id
     */
    public abstract ValidateEvidenceValueObject remove( Long id );

    /**
     * Load an evidence
     * 
     * @param id The Evidence database id
     */
    public abstract EvidenceValueObject load( Long id );

    /**
     * Modify an existing evidence
     * 
     * @param evidenceValueObject the evidence with modified fields
     * @return Status of the operation
     */
    public abstract ValidateEvidenceValueObject update( EvidenceValueObject evidenceValueObject );

    /**
     * Giving a phenotype searchQuery, returns a selection choice to the user
     * 
     * @param searchQuery query typed by the user
     * @param geneId the id of the chosen gene
     * @return Collection<CharacteristicValueObject> list of choices returned
     */
    public abstract Collection<CharacteristicValueObject> searchOntologyForPhenotypes( String searchQuery, Long geneId );

    /**
     * Using all the phenotypes in the database, builds a tree structure using the Ontology
     * 
     * @return Collection<TreeCharacteristicValueObject> list of all phenotypes in gemma represented as trees
     */
    public abstract Collection<TreeCharacteristicValueObject> findAllPhenotypesByTree();

    /**
     * Does a Gene search (by name or symbol) for a query and return only Genes with evidence
     * 
     * @param query
     * @param taxonId, can be null to not constrain by taxon
     * @return Collection<GeneEvidenceValueObject> list of Genes
     */
    public abstract Collection<GeneEvidenceValueObject> findGenesWithEvidence( String query, Long taxonId );

    /**
     * Find all phenotypes associated to a pubmedID
     * 
     * @param pubMedId
     * @return BibliographicReferenceValueObject
     */
    public abstract BibliographicReferenceValueObject findBibliographicReference( String pubMedId );

    /**
     * Validate an Evidence before we create it
     * 
     * @param geneNCBI The Gene NCBI we want to add the evidence
     * @param evidence The evidence
     * @return ValidateEvidenceValueObject flags of information to show user messages
     */
    public abstract ValidateEvidenceValueObject validateEvidence( EvidenceValueObject evidence );

    /**
     * Find mged category term that were used in the database, used to annotated Experiments
     * 
     * @return Collection<CharacteristicValueObject> the terms found
     */
    public abstract Collection<CharacteristicValueObject> findExperimentMgedCategory();

    /**
     * for a given search string look in the database and Ontology for matches
     * 
     * @param givenQueryString the search query
     * @param categoryUri the mged category (can be null)
     * @param taxonId the taxon id (can be null)
     * @return Collection<CharacteristicValueObject> the terms found
     */
    public abstract Collection<CharacteristicValueObject> findExperimentOntologyValue(
            String givenQueryString, String categoryUri, Long taxonId );

    public abstract void setOntologyHelper( PhenotypeAssoOntologyHelper ontologyHelper );

}
