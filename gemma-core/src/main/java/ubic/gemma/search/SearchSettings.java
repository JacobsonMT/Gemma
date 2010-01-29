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
package ubic.gemma.search;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Taxon;

/**
 * @author paul
 * @version $Id$
 */
public class SearchSettings {

    @Override
    public String toString() {
        return query;
    }

    private boolean searchProbes = true;
    private Taxon taxon;
    private int maxResults = 500;
    private String query;
    private String uri;

    private boolean searchGenes = true;
    private boolean searchExperiments = true;
    private boolean searchBioSequences = true;
    private boolean searchArrays = true;
    private boolean searchBibrefs = false;
    private boolean searchGenesByGO = false;

    private boolean useIndices = true;
    private boolean useDatabase = true;
    private boolean useCharacteristics = true;

    /**
     * If the search is 'specific', we can use that to refine the search criteria
     */
    boolean generalSearch = true;

    /**
     * Only relevant if the search can be restricted to a certain array design.
     */
    private ArrayDesign arrayDesign;

    public SearchSettings() {
    }

    /**
     * Convenience method to get pre-configured settings.
     * 
     * @param query
     * @return
     */
    public static SearchSettings ArrayDesignSearch( String query ) {
        SearchSettings s = new SearchSettings( query );
        s.setGeneralSearch( false );
        s.noSearches();
        s.setSearchArrays( true );
        return s;
    }

    /**
     * Convenience method to get pre-configured settings.
     * 
     * @param query
     * @return
     */
    public static SearchSettings GeneSearch( String query, Taxon taxon ) {
        SearchSettings s = new SearchSettings( query );
        s.setGeneralSearch( false );
        s.noSearches();
        s.setSearchGenes( true );
        s.setTaxon( taxon );
        return s;
    }

    /**
     * Convenience method to get pre-configured settings.
     * 
     * @param query
     * @return
     */
    public static SearchSettings CompositeSequenceSearch( String query, ArrayDesign arrayDesign ) {
        SearchSettings s = new SearchSettings( query );
        s.setGeneralSearch( false );
        s.noSearches();
        s.setSearchProbes( true );
        s.setArrayDesign( arrayDesign );
        return s;
    }

    /**
     * Convenience method to get pre-configured settings.
     * 
     * @param query
     * @return
     */
    public static SearchSettings ExpressionExperimentSearch( String query ) {
        SearchSettings s = new SearchSettings( query );
        s.setGeneralSearch( false );
        s.noSearches();
        s.setSearchExperiments( true );
        return s;
    }

    /**
     * NOTE the query is trim()'ed, no need to do that later.
     * 
     * @param query
     */
    public SearchSettings( String query ) {
        this.query = query.trim();
    }

    public int getMaxResults() {
        return maxResults;
    }

    public String getQuery() {
        return query;
    }

    public Taxon getTaxon() {
        return taxon;
    }

    public boolean isSearchArrays() {
        return searchArrays;
    }

    public boolean isSearchBibrefs() {
        return searchBibrefs;
    }

    public boolean isSearchBioSequences() {
        return searchBioSequences;
    }

    public boolean isSearchExperiments() {
        return searchExperiments;
    }

    public boolean isSearchGenes() {
        return searchGenes;
    }

    public boolean isSearchGenesByGO() {
        return searchGenesByGO;
    }

    public boolean isSearchProbes() {
        return this.searchProbes;
    }

    public void setMaxResults( int maxResults ) {
        this.maxResults = maxResults;
    }

    public void setQuery( String query ) {
        this.query = query.trim();
    }

    public void setSearchArrays( boolean searchArrays ) {
        this.searchArrays = searchArrays;
    }

    public void setSearchBibrefs( boolean searchBibrefs ) {
        this.searchBibrefs = searchBibrefs;
    }

    public void setSearchBioSequences( boolean searchBioSequences ) {
        this.searchBioSequences = searchBioSequences;
    }

    public void setSearchExperiments( boolean searchExperiments ) {
        this.searchExperiments = searchExperiments;
    }

    public void setSearchGenes( boolean searchGenes ) {
        this.searchGenes = searchGenes;
    }

    public void setSearchGenesByGO( boolean searchGenesByGO ) {
        this.searchGenesByGO = searchGenesByGO;
    }

    public void setSearchProbes( boolean searchProbes ) {
        this.searchProbes = searchProbes;
    }

    public void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
    }

    public boolean isGeneralSearch() {
        return generalSearch;
    }

    public void setGeneralSearch( boolean generalSearch ) {
        this.generalSearch = generalSearch;
    }

    /**
     * Reset all search criteria to false.
     */
    public void noSearches() {
        this.searchArrays = false;
        this.searchBibrefs = false;
        this.searchBioSequences = false;
        this.searchGenesByGO = false;
        this.searchExperiments = false;
        this.searchProbes = false;
    }

    public ArrayDesign getArrayDesign() {
        return this.arrayDesign;
    }

    public void setArrayDesign( ArrayDesign arrayDesign ) {
        this.arrayDesign = arrayDesign;
    }

    public boolean isUseIndices() {
        return useIndices;
    }

    public void setUseIndices( boolean useIndices ) {
        this.useIndices = useIndices;
    }

    public boolean isUseDatabase() {
        return useDatabase;
    }

    public void setUseDatabase( boolean useDatabase ) {
        this.useDatabase = useDatabase;
    }

    public boolean isUseCharacteristics() {
        return useCharacteristics;
    }

    public void setUseCharacteristics( boolean useCharacteristics ) {
        this.useCharacteristics = useCharacteristics;
    }

    public void setUri( String uri ) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

}
