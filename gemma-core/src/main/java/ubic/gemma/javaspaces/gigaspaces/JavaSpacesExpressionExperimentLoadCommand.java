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
package ubic.gemma.javaspaces.gigaspaces;

import java.io.Serializable;

/**
 * @author keshav
 * @version $Id$
 */
public class JavaSpacesExpressionExperimentLoadCommand extends JavaSpacesCommand implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private boolean loadPlatformOnly;

    /**
     * Used to turn off 'bioassay to biomaterial' matching.
     */
    private boolean suppressMatching;

    private String accession;

    /**
     * Set to true to attempt to remove all unneeded quantitation types during parsing.
     */
    private boolean aggressiveQtRemoval;

    public String getAccession() {
        return accession;
    }

    public void setAccession( String accession ) {
        this.accession = accession;
    }

    public boolean isAggressiveQtRemoval() {
        return aggressiveQtRemoval;
    }

    public void setAggressiveQtRemoval( boolean aggressiveQtRemoval ) {
        this.aggressiveQtRemoval = aggressiveQtRemoval;
    }

    public boolean isLoadPlatformOnly() {
        return loadPlatformOnly;
    }

    public void setLoadPlatformOnly( boolean loadPlatformOnly ) {
        this.loadPlatformOnly = loadPlatformOnly;
    }

    public boolean isSuppressMatching() {
        return suppressMatching;
    }

    public void setSuppressMatching( boolean suppressMatching ) {
        this.suppressMatching = suppressMatching;
    }

    public JavaSpacesExpressionExperimentLoadCommand() {
        super();
    }

}
