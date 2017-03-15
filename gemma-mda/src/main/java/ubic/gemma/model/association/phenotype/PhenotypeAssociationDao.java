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
package ubic.gemma.model.association.phenotype;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ExternalDatabaseStatisticsValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.PhenotypeValueObject;
import ubic.gemma.persistence.BaseDao;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author nicolas
 */
public interface PhenotypeAssociationDao extends BaseDao<PhenotypeAssociation> {

    /**
     * counts the evidence that from neurocarta that came from a specific MetaAnalysis
     */
    Long countEvidenceWithGeneDifferentialExpressionMetaAnalysis( Long geneDifferentialExpressionMetaAnalysisId );

    /**
     * find category terms currently used in the database by evidence
     */
    Collection<CharacteristicValueObject> findEvidenceCategoryTerms();

    /**
     * return the list of the owners that have evidence in the system
     */
    Collection<String> findEvidenceOwners();

    /**
     * find all evidences from a specific external database
     */
    Collection<PhenotypeAssociation> findEvidencesWithExternalDatabaseName( String externalDatabaseName,
            Integer limit );

    /**
     * find all evidence that doesn't come from an external course
     */
    Collection<PhenotypeAssociation> findEvidencesWithoutExternalDatabaseName();

    /**
     * Gets all External Databases that are used with evidence
     */
    Collection<ExternalDatabase> findExternalDatabasesWithEvidence();

    /**
     * @param includeIEA if false, electronic annotations will be omitted
     * @return map of gene value objects to the exact phenotype the gene was annotated to. (gives no indication of 'bag
     * of terms')
     */
    Map<GeneValueObject, OntologyTerm> findGenesForPhenotype( OntologyTerm term, Long taxon, boolean includeIEA );

    /**
     * find Genes link to a phenotype
     */
    Collection<GeneEvidenceValueObject> findGenesWithPhenotypes( Set<String> phenotypesValueUri, Taxon taxon,
            boolean showOnlyEditable, Collection<Long> externalDatabaseIds );

    /**
     * find all PhenotypeAssociation for a specific gene id
     */
    Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneId( Long geneId );

    /**
     * find all PhenotypeAssociation for a specific gene id and external Databases ids
     */
    Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneIdAndDatabases( Long geneId,
            Collection<Long> externalDatabaseIds );

    /**
     * find all PhenotypeAssociation for a specific NCBI id
     */
    Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneNCBI( Integer geneNCBI );

    /**
     * find all PhenotypeAssociation for a specific NCBI id and phenotypes valueUri
     */
    Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneNCBI( Integer geneNCBI, Set<String> phenotype );

    /**
     * find PhenotypeAssociations satisfying the given filters: ids, taxonId and limit
     */
    Collection<PhenotypeAssociation> findPhenotypeAssociationWithIds( Collection<Long> ids );

    /**
     * find PhenotypeAssociations associated with a BibliographicReference
     */
    Collection<PhenotypeAssociation> findPhenotypesForBibliographicReference( String pubMedID );

    /**
     * find private evidence id that the user can modify or owns
     */
    Set<Long> findPrivateEvidenceId( Long taxonId, Integer limit );

    /**
     * find all private phenotypes associated with genes on a specific taxon and containing the valuesUri
     */
    Map<String, Set<Integer>> findPrivatePhenotypesGenesAssociations( Taxon taxon, Set<String> valuesUri,
            boolean showOnlyEditable, Collection<Long> externalDatabaseIds, boolean noElectronicAnnotation );

    /**
     * find all public phenotypes associated with genes on a specific taxon and containing the valuesUri
     */
    Map<String, Set<Integer>> findPublicPhenotypesGenesAssociations( Taxon taxon, Set<String> valuesUri,
            boolean showOnlyEditable, Collection<Long> externalDatabaseIds, boolean noElectronicAnnotation );

    Collection<String> loadAllDescription();

    /**
     * find all phenotypes in Neurocarta
     */
    Collection<PhenotypeValueObject> loadAllNeurocartaPhenotypes();

    /**
     * load all valueURI of Phenotype in the database
     */
    Set<String> loadAllPhenotypesUri();

    /**
     * returns a Collection of DifferentialExpressionEvidence for a geneDifferentialExpressionMetaAnalysisId if one
     * exists (can be used to find the threshold and phenotypes for a GeneDifferentialExpressionMetaAnalysis)
     */
    Collection<DifferentialExpressionEvidence> loadEvidenceWithGeneDifferentialExpressionMetaAnalysis(
            Long geneDifferentialExpressionMetaAnalysisId, Long maxResults );

    /**
     * find statistics all evidences
     */
    ExternalDatabaseStatisticsValueObject loadStatisticsOnAllEvidence( String filePath );

    /**
     * finds all external databases statistics used in neurocarta
     */
    Collection<ExternalDatabaseStatisticsValueObject> loadStatisticsOnExternalDatabases( String folderPath );

    /**
     * find statistics for a neurocarta manual curation (numGene, numPhenotypes, etc.)
     */
    ExternalDatabaseStatisticsValueObject loadStatisticsOnManualCuration( String filePath );

    /**
     * remove a PhenotypeAssociationPublication
     **/
    void removePhenotypePublication( Long phenotypeAssociationPublicationId );

}
