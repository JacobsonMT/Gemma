/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.model.common.description;

import java.util.Collection;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author kelsey
 * @version $Id$
 */
public interface BibliographicReferenceService {

    /**
     * <p>
     * Adds a document (in PDF format) for the reference.
     * </p>
     */
    @Secured( { "GROUP_USER" })
    public void addPDF( LocalFile pdfFile, BibliographicReference bibliographicReference );

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public BibliographicReference create( BibliographicReference bibliographicReference );

    /**
     * <p>
     * check to see if the object already exists
     * </p>
     */
    public BibliographicReference find( BibliographicReference bibliographicReference );

    /**
     * <p>
     * Get a reference by the unqualified external id.
     * </p>
     */
    public BibliographicReference findByExternalId( java.lang.String id );

    /**
     * Retrieve a reference by identifier, qualified by the database name (such as 'pubmed').
     */
    public BibliographicReference findByExternalId( java.lang.String id, java.lang.String databaseName );

    /**
     * F
     * 
     * @param accession
     * @return
     */
    public BibliographicReference findByExternalId( DatabaseEntry accession );

    /**
     * 
     */
    public BibliographicReference findByTitle( java.lang.String title );

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public BibliographicReference findOrCreate( BibliographicReference BibliographicReference );

    /**
     * Return all the BibRefs that are linked to ExpressionExperiments.
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_MAP_READ" })
    public java.util.Map<ExpressionExperiment, BibliographicReference> getAllExperimentLinkedReferences();

    /**
     * Get the ExpressionExperiments, if any, that are linked to the given reference.
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<ExpressionExperiment> getRelatedExperiments(
            BibliographicReference bibliographicReference );

    /**
     * 
     */
    public BibliographicReference load( java.lang.Long id );

    /**
     * 
     */
    public java.util.Collection<BibliographicReference> loadMultiple( java.util.Collection<Long> ids );

    /**
     * 
     */
    @Secured( { "GROUP_ADMIN" })
    public void remove( BibliographicReference BibliographicReference );

    /**
     * 
     */
    @Secured( { "GROUP_ADMIN" })
    public void update( BibliographicReference bibliographicReference );

    public BibliographicReference thaw( BibliographicReference bibliographicReference );

    public Collection<BibliographicReference> thaw( Collection<BibliographicReference> bibliographicReferences );

}
