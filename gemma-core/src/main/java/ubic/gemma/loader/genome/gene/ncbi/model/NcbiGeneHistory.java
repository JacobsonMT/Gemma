/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.loader.genome.gene.ncbi.model;

import java.util.LinkedList;

import org.apache.commons.lang.StringUtils;

/**
 * Represents the information from the "gene_history" file from NCBI (for one gene's history).
 * 
 * @author paul
 * @version $Id$
 */
public class NcbiGeneHistory {

    LinkedList<String> history;

    public NcbiGeneHistory( String startingId ) {
        history = new LinkedList<String>();
        history.add( startingId );
    }

    public String getCurrentId() {
        return history.getLast();
    }

    /**
     * If the id was ever changed, give the <em>previous</em> id from the current. Otherwise return null.
     * 
     * @return
     */
    public String getPreviousId() {
        if ( history.size() == 1 ) {
            return null;
        } else {
            return history.get( history.size() - 2 );
        }
    }

    public boolean usedToBe( String oldId ) {
        return history.contains( oldId );
    }

    /**
     * @param newId
     */
    public void update( String oldId, String newId ) {
        if ( history.contains( newId ) ) {
            throw new IllegalArgumentException( "History already contains " + newId );
        }
        if ( !history.contains( oldId ) ) {
            throw new IllegalArgumentException( "History doesn't contain " + oldId );
        }
        this.history.add( history.indexOf( oldId ) + 1, newId );
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        return ( ( NcbiGeneHistory ) obj ).getCurrentId().equals( this.getCurrentId() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.getCurrentId().hashCode();
    }

    @Override
    public String toString() {
        return StringUtils.join( history, "->" );
    }

}
