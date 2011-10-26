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
package ubic.gemma.association.phenotype;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.DiseaseOntologyService;
import ubic.basecode.ontology.providers.HumanPhenotypeOntologyService;
import ubic.basecode.ontology.providers.MammalianPhenotypeOntologyService;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.association.phenotype.service.PhenotypeAssociationService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidencesValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.TreeCharacteristicValueObject;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;

/** High Level Service used to add Candidate Gene Management System capabilities */
@Component
public class PhenotypeAssociationManagerServiceImpl implements PhenotypeAssociationManagerService {

    @Autowired
    private PhenotypeAssociationService associationService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private PhenotypeAssoManagerServiceHelper phenotypeAssoManagerServiceHelper;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private TaxonService taxonService;

    /**
     * Links an Evidence to a Gene
     * 
     * @param geneNCBI The Gene NCBI we want to add the evidence
     * @param evidence The evidence
     * @return The Gene updated with the new evidence and phenotypes
     */
    @Override
    public GeneEvidencesValueObject create( String geneNCBI, EvidenceValueObject evidence ) {

        // find the gene we wish to add the evidence and phenotype
        Gene gene = this.geneService.findByNCBIId( geneNCBI );

        // convert all evidence for this gene to valueObject
        Collection<EvidenceValueObject> evidenceValueObjects = EvidenceValueObject.convert2ValueObjects( gene
                .getPhenotypeAssociations() );

        // verify that the evidence is not a duplicate
        for ( EvidenceValueObject evidenceFound : evidenceValueObjects ) {
            if ( evidenceFound.equals( evidence ) ) {
                // the evidence already exists, no need to create it again
                return new GeneEvidencesValueObject( gene );
            }
        }

        // convert the valueObject received to the corresponding entity
        PhenotypeAssociation pheAsso = this.phenotypeAssoManagerServiceHelper.valueObject2Entity( evidence );

        pheAsso.setGene( gene ); // Important.

        pheAsso = associationService.create( pheAsso );

        // add the entity to the gene
        gene.getPhenotypeAssociations().add( pheAsso );

        // return the saved gene result
        return new GeneEvidencesValueObject( gene );
    }

    /**
     * Return all evidences for a specific gene NCBI
     * 
     * @param geneNCBI The Evidence id
     * @return The Gene we are interested in
     */
    @Override
    public Collection<EvidenceValueObject> findEvidencesByGeneNCBI( String geneNCBI ) {

        Gene gene = geneService.findByNCBIId( geneNCBI );

        if ( gene == null ) {
            return null;
        }
        return EvidenceValueObject.convert2ValueObjects( gene.getPhenotypeAssociations() );
    }

    /**
     * Return all evidences for a specific gene id
     * 
     * @param geneId The Evidence id
     * @return The Gene we are interested in
     */
    @Override
    public Collection<EvidenceValueObject> findEvidencesByGeneId( Long geneId ) {

        Gene gene = geneService.load( geneId );

        if ( gene == null ) {
            return null;
        }
        return EvidenceValueObject.convert2ValueObjects( gene.getPhenotypeAssociations() );
    }

    private Set<CharacteristicValueObject> findUniquePhenotpyesForGeneId( Long geneId ) {

        Set<CharacteristicValueObject> phenotypes = new TreeSet<CharacteristicValueObject>();

        Collection<EvidenceValueObject> evidences = findEvidencesByGeneId( geneId );

        for ( EvidenceValueObject evidenceVO : evidences ) {
            phenotypes.addAll( evidenceVO.getPhenotypes() );
        }
        return phenotypes;
    }

    /**
     * Given an array of phenotypes returns the genes that have all those phenotypes
     * 
     * @param 1 to many phenotypes
     * @return A collection of the genes found
     */
    @Override
    public Collection<GeneEvidencesValueObject> findCandidateGenes( String... phenotypesValues ) {

        if ( phenotypesValues.length == 0 ) {
            return null;
        }

        Collection<GeneEvidencesValueObject> genesVO = new HashSet<GeneEvidencesValueObject>();

        // find all the Genes with the first phenotype
        Collection<Gene> genes = this.associationService.findPhenotypeAssociations( phenotypesValues[0] );

        Collection<GeneEvidencesValueObject> genesWithFirstPhenotype = GeneEvidencesValueObject
                .convert2GeneEvidencesValueObjects( genes );

        if ( phenotypesValues.length == 1 ) {
            genesVO = genesWithFirstPhenotype;
        }
        // there is more than 1 phenotype, lets filter the content
        else {
            for ( GeneEvidencesValueObject gene : genesWithFirstPhenotype ) {

                // contains all phenotypes for one gene
                HashSet<String> allPhenotypes = new HashSet<String>();

                for ( EvidenceValueObject evidence : gene.getEvidences() ) {
                    for ( CharacteristicValueObject phenotype : evidence.getPhenotypes() ) {
                        allPhenotypes.add( phenotype.getValue() );
                    }
                }

                boolean containAllPhenotypes = true;

                // verify if all phenotypes we are looking for are present in the gene
                for ( int i = 1; i < phenotypesValues.length; i++ ) {

                    if ( !allPhenotypes.contains( phenotypesValues[i].toLowerCase() ) ) {
                        containAllPhenotypes = false;
                    }
                }

                // if the gene had all phenotypes
                if ( containAllPhenotypes ) {
                    genesVO.add( gene );
                }
            }
        }

        // for each evidence on the gene, lets put a flag if that evidence got the chosen phenotype
        for ( GeneEvidencesValueObject gene : genesVO ) {

            for ( EvidenceValueObject evidence : gene.getEvidences() ) {

                boolean evidenceHasPhenotype = false;

                for ( CharacteristicValueObject phenotype : evidence.getPhenotypes() ) {

                    for ( int i = 0; i < phenotypesValues.length; i++ ) {

                        if ( phenotype.getValue().equalsIgnoreCase( phenotypesValues[i] ) ) {
                            evidenceHasPhenotype = true;
                        }
                    }
                }

                if ( evidenceHasPhenotype ) {
                    // score between 0 and 1
                    evidence.setRelevance( 1.0 );
                }
            }
        }
        return genesVO;
    }

    /**
     * Get all phenotypes linked to genes and count how many genes are link to each phenotype
     * 
     * @return A collection of the phenotypes with the gene occurence
     */
    @Override
    public Collection<CharacteristicValueObject> loadAllPhenotypes() {

        // find of all the phenotypes present in Gemma
        Collection<CharacteristicValueObject> phenotypes = this.associationService.loadAllPhenotypes();

        // for each of them, find the occurence
        for ( CharacteristicValueObject phenotype : phenotypes ) {

            phenotype.setOccurence( this.associationService.countGenesWithPhenotype( phenotype.getValue() ) );

            // TODO for now lets use lowerCase until we have a tree
            phenotype.setValue( phenotype.getValue().toLowerCase() );
        }

        return phenotypes;
    }

    /**
     * Removes an evidence
     * 
     * @param id The Evidence database id
     */
    @Override
    public void remove( Long id ) {
        PhenotypeAssociation loaded = this.associationService.load( id );
        if ( loaded != null ) this.associationService.remove( loaded );
    }

    /**
     * Modify an existing evidence
     * 
     * @param evidenceValueObject the evidence with modified fields
     */
    @Override
    public void update( EvidenceValueObject evidenceValueObject ) {

        Long id = evidenceValueObject.getDatabaseId();

        if ( evidenceValueObject.getDatabaseId() != null ) {

            // load the phenotypeAssociation
            PhenotypeAssociation phenotypeAssociation = this.associationService.load( id );

            if ( phenotypeAssociation != null ) {

                // change field in the phenotypeAssociation using the valueObject
                this.phenotypeAssoManagerServiceHelper.populatePhenotypeAssociation( phenotypeAssociation,
                        evidenceValueObject );

                // update changes to database
                this.associationService.update( phenotypeAssociation );
            }
        }
    }

    /**
     * Giving a phenotype searchQuery, return a selection choice to the user
     * 
     * @param termUsed is what the user typed
     * @return Collection<CharacteristicValueObject> list of choices returned
     */
    @Override
    public Collection<CharacteristicValueObject> searchOntologyForPhenotype( String searchQuery ) {
        return searchOntologyForPhenotype( searchQuery, null );
    }

    /**
     * Giving a phenotype searchQuery, return a selection choice to the user
     * 
     * @param termUsed is what the user typed
     * @param geneId the id of the gene chosen
     * @return Collection<CharacteristicValueObject> list of choices returned
     */
    // TO DO test and discuss more
    @Override
    public Collection<CharacteristicValueObject> searchOntologyForPhenotype( String searchQuery, Long geneId ) {

        Collection<CharacteristicValueObject> phenotypesFound = new ArrayList<CharacteristicValueObject>();

        boolean geneProvided = true;

        if ( geneId == null ) {
            geneProvided = false;
        }

        String[] tokens = searchQuery.split( " " );

        searchQuery = "";

        for ( int i = 0; i < tokens.length; i++ ) {

            searchQuery = searchQuery + tokens[i] + "* ";

            // last one
            if ( i != tokens.length - 1 ) {
                searchQuery = searchQuery + "AND ";
            }
        }

        DiseaseOntologyService diseaseOntologyService = ontologyService.getDiseaseOntologyService();
        MammalianPhenotypeOntologyService mammalianPhenotypeOntologyService = ontologyService
                .getMammalianPhenotypeOntologyService();
        HumanPhenotypeOntologyService humanPhenotypeOntologyService = ontologyService
                .getHumanPhenotypeOntologyService();

        Set<CharacteristicValueObject> phenotypes = new HashSet<CharacteristicValueObject>();

        // search disease ontology
        phenotypes.addAll( this.phenotypeAssoManagerServiceHelper.ontology2CharacteristicValueObject(
                diseaseOntologyService.findTerm( searchQuery ), PhenotypeAssociationConstants.DISEASE ) );

        // search mp ontology
        phenotypes.addAll( this.phenotypeAssoManagerServiceHelper.ontology2CharacteristicValueObject(
                mammalianPhenotypeOntologyService.findTerm( searchQuery ),
                PhenotypeAssociationConstants.MAMMALIAN_PHENOTYPE ) );

        // search hp ontology
        phenotypes.addAll( this.phenotypeAssoManagerServiceHelper.ontology2CharacteristicValueObject(
                humanPhenotypeOntologyService.findTerm( searchQuery ), PhenotypeAssociationConstants.HUMAN_PHENOTYPE ) );

        // This list will contain exact match found in the Ontology search result
        Collection<CharacteristicValueObject> phenotypesFound1 = new ArrayList<CharacteristicValueObject>();
        // This list will contain phenotypes that are already present for that gene
        Collection<CharacteristicValueObject> phenotypesFound2 = new ArrayList<CharacteristicValueObject>();
        // This list will contain phenotypes that are a substring of the searchQuery
        Collection<CharacteristicValueObject> phenotypesFound3 = new ArrayList<CharacteristicValueObject>();
        // Phenotypes present in the database already and found in the ontology search
        Collection<CharacteristicValueObject> phenotypesFound4 = new ArrayList<CharacteristicValueObject>();
        // others
        Collection<CharacteristicValueObject> phenotypesFound5 = new ArrayList<CharacteristicValueObject>();

        // Set of all the phenotypes present on the gene
        Set<CharacteristicValueObject> phenotypesOnGene = null;

        if ( geneProvided ) {
            phenotypesOnGene = findUniquePhenotpyesForGeneId( geneId );
        }

        // lets loas all phenotypes presents in the database
        Collection<CharacteristicValueObject> allPhenotypes = loadAllPhenotypes();

        /*
         * for each CharacteristicVO made from the Ontology search lets filter them and add them to a specific list if
         * they satisfied the condition
         */
        for ( CharacteristicValueObject cha : phenotypes ) {

            // Case 1, exact match
            if ( cha.getValue().equalsIgnoreCase( searchQuery ) ) {

                // if also already present on that gene
                if ( geneProvided && phenotypesOnGene.contains( cha ) ) {
                    cha.setAlreadyPresentOnGene( true );
                }
                phenotypesFound1.add( cha );
            }
            // Case 2, phenotpye already present on Gene
            else if ( geneProvided && phenotypesOnGene.contains( cha ) ) {
                cha.setAlreadyPresentOnGene( true );
                phenotypesFound2.add( cha );
            }
            // Case 3, contains a substring of the word
            else if ( searchQuery.toLowerCase().indexOf( cha.getValue().toLowerCase() ) != -1 ) {
                phenotypesFound3.add( cha );
            }
            // Case 4, phenotypes already in Gemma database
            else if ( allPhenotypes.contains( cha ) ) {
                phenotypesFound4.add( cha );
            } else {
                phenotypesFound5.add( cha );
            }
        }

        // place them in the correct order to display
        phenotypesFound.addAll( phenotypesFound1 );
        phenotypesFound.addAll( phenotypesFound2 );
        phenotypesFound.addAll( phenotypesFound3 );
        phenotypesFound.addAll( phenotypesFound4 );
        phenotypesFound.addAll( phenotypesFound5 );

        return phenotypesFound;
    }

    // TODO NOT TO BE USED WAS COMMITED BECAUSE OF OTHER CODE, FAST DRAFT, WILL BE CHANGED SOON
    public Collection<TreeCharacteristicValueObject> findAllPhenotypesByTree() {

        // represents each phenotype as a tree of terms
        TreeSet<TreeCharacteristicValueObject> col = new TreeSet<TreeCharacteristicValueObject>();

        // all phenotypes in the Gemma database
        Collection<CharacteristicValueObject> allPhenotypes = loadAllPhenotypes();

        DiseaseOntologyService diseaseOntologyService = ontologyService.getDiseaseOntologyService();
        MammalianPhenotypeOntologyService mammalianPhenotypeOntologyService = ontologyService
                .getMammalianPhenotypeOntologyService();
        HumanPhenotypeOntologyService humanPhenotypeOntologyService = ontologyService
                .getHumanPhenotypeOntologyService();

        for ( CharacteristicValueObject c : allPhenotypes ) {

            OntologyTerm ontologyTerm = diseaseOntologyService.getTerm( c.getValueUri() );

            if ( ontologyTerm == null ) {
                ontologyTerm = mammalianPhenotypeOntologyService.getTerm( c.getValueUri() );
            }

            if ( ontologyTerm == null ) {
                ontologyTerm = humanPhenotypeOntologyService.getTerm( c.getValueUri() );
            }

            // create a tree for the term
            TreeCharacteristicValueObject treeCharacteristicValueObject = this.phenotypeAssoManagerServiceHelper
                    .ontology2TreeCharacteristicValueObjects( ontologyTerm );
            // set flag that this node represents a phenotype in the database
            treeCharacteristicValueObject.setWasFound( true );

            col.add( treeCharacteristicValueObject );
        }

        // map of all phenotypes found in the tree
        HashMap<String, TreeCharacteristicValueObject> hs = new HashMap<String, TreeCharacteristicValueObject>();

        // this will be the final result of combining all trees found into less trees without the terms that are not in
        // the database
        Collection<TreeCharacteristicValueObject> finalTrees = new TreeSet<TreeCharacteristicValueObject>();

        // the deepest tree is the first one, was order by deep
        TreeCharacteristicValueObject treeC = col.first();
        col.remove( treeC );
        finalTrees.add( treeC );
        this.phenotypeAssoManagerServiceHelper.addTermsToHash( treeC, hs );

        for ( TreeCharacteristicValueObject treeVO : col ) {

            // look if the phenotype is present in a previous tree
            TreeCharacteristicValueObject treeExist = hs.get( treeVO.getValueUri() );

            // look first if can be place in one tree
            if ( treeExist != null ) {
                treeExist.setWasFound( true );
            }
            // if not add terms to hashmap
            else {
                finalTrees.add( treeVO );
                this.phenotypeAssoManagerServiceHelper.addTermsToHash( treeVO, hs );
            }
        }

        // remove unused nodes
        for ( TreeCharacteristicValueObject tc : finalTrees ) {
            tc.removeUnused();
        }

        return finalTrees;
    }

    /**
     * Does a Gene search (by name or symbol) for a query and return only Genes with evidences
     * 
     * @param query
     * @param taxonId, can be null to not constrain by taxon
     * @return Collection<GeneEvidencesValueObject> list of Genes
     */
    @Override
    public Collection<GeneEvidencesValueObject> findGenesWithEvidences( String query, Long taxonId ) {
        Taxon taxon = null;
        if ( taxonId != null ) {
            taxon = taxonService.load( taxonId );
        }
        SearchSettings settings = SearchSettings.geneSearch( query, taxon );
        List<SearchResult> geneSearchResults = searchService.search( settings ).get( Gene.class );

        Collection<Gene> genes = new HashSet<Gene>();
        if ( geneSearchResults == null || geneSearchResults.isEmpty() ) {
            return new HashSet<GeneEvidencesValueObject>();
        }

        for ( SearchResult sr : geneSearchResults ) {
            genes.add( ( Gene ) sr.getResultObject() );
        }

        Collection<GeneEvidencesValueObject> geneEvidencesValueObjects = GeneEvidencesValueObject
                .convert2GeneEvidencesValueObjects( genes );

        Collection<GeneEvidencesValueObject> geneValueObjectsFilter = new ArrayList<GeneEvidencesValueObject>();

        for ( GeneEvidencesValueObject gene : geneEvidencesValueObjects ) {

            if ( gene.getEvidences() != null && gene.getEvidences().size() != 0 ) {

                geneValueObjectsFilter.add( gene );
            }
        }

        return geneValueObjectsFilter;
    }

}
