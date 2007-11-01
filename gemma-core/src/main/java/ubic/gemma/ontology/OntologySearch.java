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
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.impl.IndividualImpl;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.query.larq.IndexLARQ;
import com.hp.hpl.jena.query.larq.LARQ;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;

/**
 * @author pavlidis
 * @version $Id$
 */
public class OntologySearch {

    private static Log log = LogFactory.getLog( OntologySearch.class.getName() );

    public static void performQuery( OntModel model, IndexLARQ index, String queryString ) {
        // Make globally available
        LARQ.setDefaultIndex( index );

        Query query = QueryFactory.create( queryString );
        query.serialize( System.out );
        System.out.println();

        QueryExecution qExec = QueryExecutionFactory.create( query, model );
        // LARQ.setDefaultIndex(qExec.getContext(), index) ;
        ResultSetFormatter.out( System.out, qExec.execSelect(), query );
        qExec.close();
    }

    /**
     * Find classes that match the query string
     * 
     * @param model that goes with the index
     * @param index to search
     * @param queryString
     * @return Collection of OntologyTerm objects
     */
    public static Collection<OntologyTerm> matchClasses( OntModel model, IndexLARQ index, String queryString ) {

        Set<OntologyTerm> results = new HashSet<OntologyTerm>();
        NodeIterator iterator = index.searchModelByIndex( model, queryString );

        while ( iterator.hasNext() ) {
            RDFNode r = ( RDFNode ) iterator.next();
            r = r.inModel( model );
            log.info( "Search results: " + r );
            if ( r.isURIResource() ) {
                try {
                    OntClass cl = ( OntClass ) r.as( OntClass.class );
                    OntologyTermImpl impl2 = new OntologyTermImpl( cl, null );
                    results.add( impl2 );
                    log.debug( impl2 );
                } catch ( Exception e ) {
                    log.error( e, e );
                }
            }

        }
        return results;
        //

    }

    /**
     * Find individuals that match the query string
     * 
     * @param model that goes with the index
     * @param index to search
     * @param queryString
     * @return Collection of OntologyTerm objects
     */
    public static Collection<OntologyIndividual> matchIndividuals( OntModel model, IndexLARQ index, String queryString ) {

        Set<OntologyIndividual> results = new HashSet<OntologyIndividual>();
        NodeIterator iterator = index.searchModelByIndex( model, queryString );

        while ( iterator.hasNext() ) {
            RDFNode r = ( RDFNode ) iterator.next();
            r = r.inModel( model );
            log.info( "Search results: " + r );
            if ( r.isResource() && ( r instanceof IndividualImpl ) ) {
                try {
                    Individual cl = ( Individual ) r.as( Individual.class );
                    OntologyIndividual impl2 = new OntologyIndividualImpl( cl, null );
                    results.add( impl2 );
                    log.debug( impl2 );
                } catch ( Exception e ) {
                    log.error( e, e );
                }
            }

        }
        return results;

    }

    /**
     * Find OntologyIndividuals and OntologyTerms that match the query string
     * 
     * @param model that goes with the index
     * @param index to search
     * @param queryString
     * @return Collection of OntologyResource objects
     */
    public static Collection<OntologyResource> matchResources( OntModel model, IndexLARQ index, String queryString ) {

        Set<OntologyResource> results = new HashSet<OntologyResource>();
        NodeIterator iterator = index.searchModelByIndex( model, queryString );

        while ( iterator.hasNext() ) {
            RDFNode r = ( RDFNode ) iterator.next();
            r = r.inModel( model );
            log.info( "Search results: " + r );
            if ( r.isURIResource() ) {
                try {
                    OntClass cl = ( OntClass ) r.as( OntClass.class );
                    OntologyTermImpl impl2 = new OntologyTermImpl( cl, null );
                    results.add( impl2 );
                    log.debug( impl2 );
                } catch ( Exception e ) {
                    log.error( e, e );
                }
            } else if ( r.isResource() && ( r instanceof IndividualImpl ) ) {
                try {
                    Individual cl = ( Individual ) r.as( Individual.class );
                    OntologyIndividual impl2 = new OntologyIndividualImpl( cl, null );
                    results.add( impl2 );
                    log.debug( impl2 );
                } catch ( Exception e ) {
                    log.error( e, e );
                }
            } else
                log.debug( "This search term not included in the results: " + r );

        }
        return results;

    }
}
