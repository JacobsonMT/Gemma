/*
 * The gemma project
 * 
 * Copyright (c) 2014 University of British Columbia
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

package ubic.gemma.model.analysis.expression.coexpression;

import java.util.Collection;

/**
 * Important: this is slightly misnamed, since it potentially includes links that have support of zero. <strong>It
 * cannot be used to get coexpression information for genes</strong>. It is only used to speed up analysis of data sets,
 * when deciding which links are new and which are updated. See CoexpressionDaoImpl.
 * 
 * @author Paul
 * @version $Id$
 */
public class GeneCoexpressedGenes extends IdArray {

    /**
     * 
     */
    private static final long serialVersionUID = 6715464097628664297L;
    private Long geneId;

    // needed to fulfil javabean contract
    public GeneCoexpressedGenes() {
    }

    public GeneCoexpressedGenes( Long geneId ) {
        this.geneId = geneId;
    }

    @Override
    public synchronized void addEntities( Collection<Long> ds ) {
        super.addEntities( ds );
    }

    @Override
    public synchronized void addEntity( Long ds ) {
        super.addEntity( ds );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        GeneCoexpressedGenes other = ( GeneCoexpressedGenes ) obj;

        if ( geneId != null ) return this.geneId.equals( other.geneId );

        // really the geneId has to be non-null
        if ( geneId == null ) {
            if ( other.geneId != null ) return false;
        }

        return true;
    }

    public Long getGeneId() {
        return geneId;
    }

    @Override
    public int hashCode() {
        int result = 1;

        if ( geneId != null ) return geneId.hashCode();

        return result;
    }

    public void setGeneId( Long geneId ) {
        this.geneId = geneId;
    }

}
