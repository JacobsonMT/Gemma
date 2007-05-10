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

import ubic.gemma.model.common.description.DataProperty;
import ubic.gemma.model.common.description.CharacteristicProperty;

/**
 * @author pavlidis
 * @version $Id$
 */
public class DataStatementImpl extends AbstractStatement implements DataStatement {

    private String data;

    public String getObject() {
        return data;
    }

    public DataStatementImpl( OntologyTerm term, DatatypeProperty property, String data ) {
        super( property, term );
        this.data = data;
    }

    protected CharacteristicProperty makeProperty() {
        DataProperty prop = DataProperty.Factory.newInstance();

        prop.setData( data );
        prop.setValue( this.getProperty().getLabel() );
        prop.setTermUri( this.getProperty().getUri() );
        prop.setType( ( ( DatatypeProperty ) this.getProperty() ).getType() );
        return prop;
    }

}
