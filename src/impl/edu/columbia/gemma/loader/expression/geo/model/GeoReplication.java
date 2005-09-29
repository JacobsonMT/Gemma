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

/**
 * Represents a group of samples which were replicated.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoReplication {
    ReplicationType type;
    String description = "";

    /**
     * The samples which are replicates.
     */
    Collection<GeoSample> samples;

    /**
     * @param d
     */
    public void addToDescription( String d ) {
        this.description += d;
    }

    /**
     * @return Returns the description.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * @param description The description to set.
     */
    public void setDescription( String description ) {
        this.description = description;
    }

    /**
     * @return Returns the repeatsSampleList.
     */
    public Collection<GeoSample> getSamples() {
        return this.samples;
    }

    /**
     * @param repeatsSampleList The repeatsSampleList to set.
     */
    public void setSamples( Collection<GeoSample> repeatsSampleList ) {
        this.samples = repeatsSampleList;
    }

    /**
     * @param sample
     */
    public void addToRepeatsSampleList( GeoSample sample ) {
        this.samples.add( sample );
    }

    /**
     * Permitted types of replication.
     */
    public enum ReplicationType {
        biologicalReplicate, technicalReplicateExtract, technicalReplicateLabeledExtract
    };

    /**
     * Convert a string e.g. "biological Replicate" into the corresponding ReplicationType.
     * 
     * @param string
     * @return
     */
    public static ReplicationType convertStringToRepeatType( String string ) {
        if ( string.equals( "biological Replicate" ) ) {
            return ReplicationType.biologicalReplicate;
        } else if ( string.equals( "technical replicate - extract" ) ) {
            return ReplicationType.technicalReplicateExtract;
        } else if ( string.equals( "technical replicate - labeled extract" ) ) {
            return ReplicationType.technicalReplicateLabeledExtract;
        } else {
            throw new IllegalArgumentException( "Unknown replication type " + string );
        }
    }

    /**
     * @return Returns the repeats.
     */
    public ReplicationType getType() {
        return this.type;
    }

    /**
     * @param repeats The repeats to set.
     */
    public void setRepeats( ReplicationType repeats ) {
        this.type = repeats;
    }
}
