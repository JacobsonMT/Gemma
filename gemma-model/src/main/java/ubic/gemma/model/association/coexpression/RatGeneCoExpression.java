/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.association.coexpression;

import org.apache.commons.lang3.reflect.FieldUtils;

import ubic.gemma.model.analysis.Analysis;
import ubic.gemma.model.genome.Gene;

/**
 * 
 */
public abstract class RatGeneCoExpression extends Gene2GeneCoexpression {

    /**
     * Constructs new instances of {@link RatGeneCoExpression}.
     */
    public static final class Factory {

        /**
         * @param sourceAnalysis
         * @param secondGene
         * @param firstGene
         * @param effect
         * @param numDataSets
         * @param datasetsTestedVector
         * @param datasetsSupportingVector
         * @param specificityVector
         * @return
         */
        public static RatGeneCoExpression newInstance( Analysis sourceAnalysis, Gene secondGene, Gene firstGene,
                Double effect, Integer numDataSets, byte[] datasetsTestedVector, byte[] datasetsSupportingVector,
                byte[] specificityVector ) {
            final RatGeneCoExpression entity = new RatGeneCoExpressionImpl();

            try {
                FieldUtils.writeField( entity, "sourceAnalysis", sourceAnalysis, true );
                FieldUtils.writeField( entity, "secondGene", secondGene, true );
                FieldUtils.writeField( entity, "firstGene", firstGene, true );
                FieldUtils.writeField( entity, "effect", effect, true );
                FieldUtils.writeField( entity, "numDataSets", numDataSets, true );
                FieldUtils.writeField( entity, "datasetsTestedVector", datasetsTestedVector, true );
                FieldUtils.writeField( entity, "datasetsSupportingVector", datasetsSupportingVector, true );
                FieldUtils.writeField( entity, "specificityVector", specificityVector, true );

            } catch ( IllegalAccessException e ) {
                System.err.println( e );
            }

            return entity;
        }
    }

}