/*
 * The Gemma-ONT_REV project
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

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.common.description.VocabCharacteristic;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ChainedStatementObjectImpl implements ChainedStatementObject {

    private OntologyTerm subject = null;

    Collection<CharacteristicStatement> statements;

    public void addStatement( CharacteristicStatement s ) {
        if ( !subject.equals( s.getSubject() ) ) {
            throw new IllegalArgumentException( "Statement must have same subject as all others" );
        }
        this.statements.add( s );
    }

    public ChainedStatementObjectImpl( OntologyTerm subject ) {
        this.subject = subject;
        this.statements = new HashSet<CharacteristicStatement>();
    }

    public VocabCharacteristic toVocabCharacteristic() {
        if ( statements.size() == 0 ) throw new IllegalStateException( "No statements!" );

        VocabCharacteristic v = null;
        for ( CharacteristicStatement s : statements ) {
            if ( v == null ) {
                v = s.toCharacteristic();
            } else {
                s.addToCharacteristic( v );
            }

        }
        return v;
    }

}
