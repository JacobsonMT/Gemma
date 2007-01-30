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
package ubic.gemma.analysis.report;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Basically a value object to hold onto the 'new' objects. TODO: supply information on other types of objects.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class WhatsNew {

    private Collection<Auditable> newObjects;
    private Collection<Auditable> updatedObjects;
    private Date date;

    public WhatsNew( Date date ) {
        this.date = date;
    }

    /**
     * @return
     */
    public Collection<ArrayDesign> getUpdatedArrayDesigns() {
        Collection<ArrayDesign> result = new HashSet<ArrayDesign>();
        for ( Auditable auditable : updatedObjects ) {
            if ( auditable instanceof ArrayDesign ) {
                result.add( ( ArrayDesign ) auditable );
            }
        }
        return result;
    }

    /**
     * @return collection of ArrayDesigns that are new since this.Date
     */
    public Collection<ArrayDesign> getNewArrayDesigns() {
        Collection<ArrayDesign> result = new HashSet<ArrayDesign>();
        for ( Auditable auditable : newObjects ) {
            if ( auditable instanceof ArrayDesign ) {
                result.add( ( ArrayDesign ) auditable );
            }
        }
        return result;
    }

    /**
     * @return
     */
    public Collection<ExpressionExperiment> getUpdatedExpressionExperiments() {
        Collection<ExpressionExperiment> result = new HashSet<ExpressionExperiment>();
        for ( Auditable auditable : updatedObjects ) {
            if ( auditable instanceof ExpressionExperiment ) {
                result.add( ( ExpressionExperiment ) auditable );
            }
        }
        return result;
    }

    /**
     * @return
     */
    public Collection<ExpressionExperiment> getNewExpressionExperiments() {
        Collection<ExpressionExperiment> result = new HashSet<ExpressionExperiment>();
        for ( Auditable auditable : newObjects ) {
            if ( auditable instanceof ExpressionExperiment ) {
                result.add( ( ExpressionExperiment ) auditable );
            }
        }
        return result;
    }

    /**
     * @return all the new objects regardless of class.
     */
    public Collection<Auditable> getNewObjects() {
        return newObjects;
    }

    /**
     * @param newObjects
     */
    public void setNewObjects( Collection<Auditable> newObjects ) {
        this.newObjects = newObjects;
    }

    /**
     * @return all the updated objects, regardless of class.
     */
    public Collection<Auditable> getUpdatedObjects() {
        return updatedObjects;
    }

    public void setUpdatedObjects( Collection<Auditable> updatedObjects ) {
        this.updatedObjects = updatedObjects;
    }

    public Date getDate() {
        return date;
    }

    public void setDate( Date date ) {
        this.date = date;
    }

}
