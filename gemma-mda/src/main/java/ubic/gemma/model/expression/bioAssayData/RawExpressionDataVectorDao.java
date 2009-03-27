/*
 * The Gemma project Copyright (c) 2009 University of British Columbia Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package ubic.gemma.model.expression.bioAssayData;

import ubic.gemma.model.common.quantitationtype.QuantitationType;

/**
 * TODO Document Me
 * 
 * @author paul
 * @version $Id$
 * @param <T>
 */
public interface RawExpressionDataVectorDao extends DesignElementDataVectorDao<RawExpressionDataVector> {
    /**
     * <p>
     * remove Design Element Data Vectors and Probe2ProbeCoexpression entries for a specified CompositeSequence.
     * </p>
     */
    public void removeDataForCompositeSequence(
            ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence );

    /**
     * <p>
     * Removes the DesignElementDataVectors and Probe2ProbeCoexpressions for a quantitation type, given a
     * QuantitationType (which always comes from a specific ExpressionExperiment)
     * </p>
     */
    public void removeDataForQuantitationType(
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType );

    /**
     * 
     */
    public RawExpressionDataVector find( RawExpressionDataVector designElementDataVector );

    /**
     * 
     */
    public java.util.Collection<RawExpressionDataVector> find( java.util.Collection<QuantitationType> quantitationTypes );

    /**
     * 
     */
    public java.util.Collection<RawExpressionDataVector> find(
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType );

    /**
     * 
     */
    public java.util.Collection<RawExpressionDataVector> find(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign,
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType );
}
