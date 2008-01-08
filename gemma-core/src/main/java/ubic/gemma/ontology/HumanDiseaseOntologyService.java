/*
 * The Gemma21 project
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

import java.io.IOException;

import com.hp.hpl.jena.ontology.OntModel;

/**
 * Holds a copy of the OBO Disese Ontology on disk. This gets loaded on startup.
 * 
 * @author klc
 * @version $Id: OBODiseaseOntologyService.java
 * @spring.bean id="diseaseOntologyService"
 */
public class HumanDiseaseOntologyService extends AbstractOntologyService {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.AbstractOntologyService#getOntologyName()
     */
    @Override
    protected String getOntologyName() {
        return "diseaseOntology";
    }

    @Override
    protected String getOntologyUrl() {
        return "http://www.berkeleybop.org/ontologies/obo-all/disease_ontology/disease_ontology.owl";
        // http://purl.org/obo/owl/DOID
    }

    @Override
    protected OntModel loadModel( String url ) {
        return OntologyLoader.loadPersistentModel( url, false );
    }

}
