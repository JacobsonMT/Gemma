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

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.VocabCharacteristic;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.larq.IndexLARQ;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;

/**
 * Methods to help deal with MESH (Medical Subject Heading) terms. These are provided in PubMed entries as text, but
 * represented in our system as a formal Ontology.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class MeshService {

    private static Log log = LogFactory.getLog( MeshService.class.getName() );
    private final static String MESH_ONT_URL = "http://www.berkeleybop.org/ontologies/obo-all/mesh/mesh.owl";
    private static final String MESH_INDEX_NAME = "mesh";
    private static OntModel model;
    private static IndexLARQ index;
    private static ExternalDatabase meshdb;
    static {
        model = OntologyLoader.loadPersistentModel( MESH_ONT_URL, false );
        index = OntologyIndexer.getSubjectIndex( MESH_INDEX_NAME );
        meshdb = ExternalDatabase.Factory.newInstance();
        meshdb.setName( "mesh" );
        meshdb.setWebUri( MESH_ONT_URL );
    }

    /**
     * Locate OntologyTerm for given plain text
     * 
     * @param plainText such as "Microsatellite Repeats"
     * @return term that exactly matches the given text, or null if nothing is found that matches exactly. If multiple
     *         terms match exactly, only the first one found in the search is returned (consistent ordering not
     *         guaranteed!)
     */
    public static OntologyTerm find( String plainText ) {
        String munged = munge( plainText );
        Collection<OntologyTerm> name = OntologySearch.matchClasses( model, index, munged );
        log.debug( munged );
        for ( OntologyTerm term : name ) {
            if ( term.getLabel().equals( munged ) ) {
                return term;
            }
        }
        return null;
    }

    private static String munge( String plainText ) {
        String[] fields = plainText.split( "," );
        if ( fields.length == 1 ) {
            return plainText.toLowerCase().trim().replaceAll( " ", "_" );
        } else if ( fields.length == 2 ) {
            // swap them around
            return fields[1].toLowerCase().trim().replaceAll( " ", "_" ) + "_"
                    + fields[0].toLowerCase().trim().replaceAll( " ", "_" );
        } else {
            return plainText.toLowerCase().trim().replaceAll( "[, ]", "_" );
        }
    }

    /**
     * @return the has_qualifier ObjectProperty that can be used to form statements about MESH term instances.
     */
    public static ubic.gemma.ontology.ObjectProperty hasQualifier() {
        Property property = model.createProperty( "http://purl.org/obo/owl/MESH#hasQualifier" );
        RDFNode node = property.inModel( model );
        model.setStrictMode( false ); // FIXME this probably isn't such a good idea, but allows the conversion to
        // proceed.
        return new ObjectPropertyImpl( ( com.hp.hpl.jena.ontology.ObjectProperty ) node
                .as( com.hp.hpl.jena.ontology.ObjectProperty.class ), meshdb );
    }

    /**
     * @return the isMajorHeading ObjectProperty that can be used to form statements about MESH term instances.
     */
    public static ubic.gemma.ontology.DatatypeProperty isMajorHeading() {
        Property property = model.createProperty( "http://purl.org/obo/owl/MESH#isMajorHeading" );
        RDFNode node = property.inModel( model );
        model.setStrictMode( false ); // FIXME this probably isn't such a good idea, but allows the conversion to
        // proceed.
        return new ubic.gemma.ontology.DatatypePropertyImpl( ( com.hp.hpl.jena.ontology.DatatypeProperty ) node
                .as( com.hp.hpl.jena.ontology.DatatypeProperty.class ), meshdb );
    }

    /**
     * Convert a term to a Characteristic
     * 
     * @param term
     * @param isMajorHeading
     * @return
     */
    public static VocabCharacteristic getCharacteristic( OntologyTerm term, boolean isMajorHeading ) {
        VocabCharacteristic vc = VocabCharacteristicBuilder.makeInstance( term );

        if ( isMajorHeading ) {
            DataStatement mh = new DataStatementImpl( term, MeshService.isMajorHeading(), "true" );
            VocabCharacteristicBuilder.addStatement( vc, mh );
        }
        return vc;
    }

    /**
     * @param term that the qualifier is attached to
     * @param qualTerm the qualifier term
     * @param qualIsMajorHeading if the qualifier is a major heading for the instance
     * @return
     */
    public static CharacteristicStatement getQualifierStatement( OntologyTerm term, OntologyTerm qualTerm,
            boolean qualIsMajorHeading ) {
        CharacteristicStatement cs;
        if ( qualIsMajorHeading ) {
            cs = new ChainedStatementImpl( term, MeshService.hasQualifier() );
            ChainedStatementObject cso = new ChainedStatementObjectImpl( qualTerm );
            DataStatement mh = new DataStatementImpl( qualTerm, MeshService.isMajorHeading(), "true" );
            cso.addStatement( mh );
            ( ( ChainedStatementImpl ) cs ).setObject( cso );
        } else {
            cs = new ClassStatementImpl( term, MeshService.hasQualifier(), qualTerm );
        }
        return cs;
    }

}
