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
package ubic.gemma.ontology;

import ubic.gemma.model.common.description.VocabCharacteristic;

/**
 * @author pavlidis
 * @version $Id$
 */
public abstract interface CharacteristicStatement<T extends Object> {

    public OntologyTerm getSubject();

    public OntologyProperty getProperty();

    public T getObject();

    public VocabCharacteristic toCharacteristic();

    /**
     * Add this statement to the given characteristic. The subject of the characteristic must be the same as that of
     * this.
     * 
     * @param v
     * @throws IllegalArgumentException if the subject of v is not the same as the subject of this.
     */
    public void addToCharacteristic( VocabCharacteristic v );

}
