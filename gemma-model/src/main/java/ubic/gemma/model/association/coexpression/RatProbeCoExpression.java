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
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.experiment.BioAssaySet;

/**
 * 
 */
public abstract class RatProbeCoExpression extends Probe2ProbeCoexpression {

    /**
     * Constructs new instances of {@link RatProbeCoExpression}.
     */
    public static final class Factory {

        public static RatProbeCoExpression newInstance( Analysis sourceAnalysis, Double score,
                BioAssaySet expressionBioAssaySet, ProcessedExpressionDataVector firstVector,
                ProcessedExpressionDataVector secondVector ) {
            final RatProbeCoExpression entity = new RatProbeCoExpressionImpl();
            try {
                FieldUtils.writeField( entity, "expressionBioAssaySet", expressionBioAssaySet, true );
                FieldUtils.writeField( entity, "secondVector", secondVector, true );
                FieldUtils.writeField( entity, "score", score, true );
                FieldUtils.writeField( entity, "firstVector", firstVector, true );
                FieldUtils.writeField( entity, "sourceAnalysis", sourceAnalysis, true );

            } catch ( IllegalAccessException e ) {
                System.err.println( e );
            }
            return entity;
        }
    }

}