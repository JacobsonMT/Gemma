/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.loader.expression.geo.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Represents a set of GEO samples that were submitted together. In many cases this corresponds to a full study, but for
 * studies that used more than one type of microarray (e.g., A and B chips in Affy sets), there will be two series.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoSeries extends GeoData {

    String title;
    String summary;
    String overallDesign;
    Collection<String> keywords;
    Collection<String> pubmedIds;
    Collection<String> webLinks;
    Collection<String> contributers;
    Map<Integer, GeoVariable> variables;
    Map<Integer, GeoReplication> replicates;

    Collection<GeoSample> samples;

    public GeoSeries() {
        keywords = new HashSet<String>();
        pubmedIds = new HashSet<String>();
        variables = new HashMap<Integer, GeoVariable>();
        replicates = new HashMap<Integer, GeoReplication>();
        webLinks = new HashSet<String>();
        contributers = new HashSet<String>();
        samples = new HashSet<GeoSample>();
    }

    public void addSample( GeoSample sample ) {
        this.samples.add( sample );
    }

    public Collection<GeoSample> getSamples() {
        return this.samples;
    }

    /**
     * @return Returns the contributers.
     */
    public Collection<String> getContributers() {
        return this.contributers;
    }

    /**
     * @param contributers The contributers to set.
     */
    public void setContributers( Collection<String> contributers ) {
        this.contributers = contributers;
    }

    /**
     * @return Returns the overallDesign.
     */
    public String getOverallDesign() {
        return this.overallDesign;
    }

    /**
     * @param overallDesign The overallDesign to set.
     */
    public void setOverallDesign( String overallDesign ) {
        this.overallDesign = overallDesign;
    }

    /**
     * @return Returns the pubmedIds.
     */
    public Collection<String> getPubmedIds() {
        return this.pubmedIds;
    }

    /**
     * @param pubmedIds The pubmedIds to set.
     */
    public void setPubmedIds( Collection<String> pubmedIds ) {
        this.pubmedIds = pubmedIds;
    }

    /**
     * @return Returns the seriesTitle.
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * @param seriesTitle The seriesTitle to set.
     */
    public void setTitle( String seriesTitle ) {
        this.title = seriesTitle;
    }

    /**
     * @return Returns the summaries.
     */
    public String getSummaries() {
        return this.summary;
    }

    /**
     * @param summaries The summaries to set.
     */
    public void setSummaries( String summary ) {
        this.summary = summary;
    }

    /**
     * @param text to add onto the summary. A space is added to the end of the previous summary first.
     */
    public void addToSummary( String text ) {
        this.summary = this.summary + " " + text;
    }

    /**
     * @return Returns the type.
     */
    public Collection<String> getKeywords() {
        return this.keywords;
    }

    /**
     * @param type The type to set.
     */
    public void setKeywords( Collection<String> type ) {
        this.keywords = type;
    }

    /**
     * @return Returns the variables.
     */
    public Map<Integer, GeoVariable> getVariables() {
        return this.variables;
    }

    /**
     * @param keyword
     */
    public void addToKeywords( String keyword ) {
        this.keywords.add( keyword );
    }

    public void addToPubmedIds( String id ) {
        this.pubmedIds.add( id );
    }

    public void addToVariables( Integer number, GeoVariable variable ) {
        this.variables.put( number, variable );
    }

    /**
     * @return Returns the webLinks.
     */
    public Collection<String> getWebLinks() {
        return this.webLinks;
    }

    /**
     * @param webLinks The webLinks to set.
     */
    public void setWebLinks( Collection<String> webLinks ) {
        this.webLinks = webLinks;
    }

    /**
     * @param contact The contact to set.
     */
    public void setContact( GeoContact contact ) {
        this.contact = contact;
    }

    /**
     * @return Returns the replicates.
     */
    public Map<Integer, GeoReplication> getReplicates() {
        return this.replicates;
    }

    /**
     * @param replicates The replicates to set.
     */
    public void setReplicates( Map<Integer, GeoReplication> replicates ) {
        this.replicates = replicates;
    }

}
