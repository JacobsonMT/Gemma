/*
 * The Gemma project.
 * 
 * Copyright (c) 2005 Columbia University
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package edu.columbia.gemma.common.description;

import java.util.Collection;

import edu.columbia.gemma.loader.entrez.pubmed.PubMedXMLFetcher;

/**
 * Implementation of BibliographicReferenceService.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @see edu.columbia.gemma.common.description.BibliographicReferenceService
 */
public class BibliographicReferenceServiceImpl extends
        edu.columbia.gemma.common.description.BibliographicReferenceServiceBase {

    /**
     * Check to see if the reference already exists
     * 
     * @see edu.columbia.gemma.common.description.BibliographicReferenceService#alreadyExists(edu.columbia.gemma.common.description.BibliographicReference)
     */
    protected boolean handleAlreadyExists(
            edu.columbia.gemma.common.description.BibliographicReference bibliographicReference )
            throws java.lang.Exception {

        boolean exists = false;
        // Collection col = getBibliographicReferenceDao().findByTitle( bibliographicReference.getTitle() );
        // if ( col.size() > 0 ) exists = true;
        BibliographicReference br = getBibliographicReferenceDao().findByTitle( bibliographicReference.getTitle() );
        if ( br != null ) exists = true;

        return exists;
    }

    /**
     * @see edu.columbia.gemma.common.description.BibliographicReferenceService#findByExternalId(java.lang.String)
     */
    protected edu.columbia.gemma.common.description.BibliographicReference handleFindByExternalId( java.lang.String id )
            throws java.lang.Exception {

        return this.getBibliographicReferenceDao().findByExternalId( id, "PubMed" );

    }

    /**
     * @see edu.columbia.gemma.common.description.BibliographicReferenceService#findByExternalId(java.lang.String,
     *      java.lang.String)
     */
    protected edu.columbia.gemma.common.description.BibliographicReference handleFindByExternalId( java.lang.String id,
            java.lang.String databaseName ) throws java.lang.Exception {

        return this.getBibliographicReferenceDao().findByExternalId( id, databaseName );
    }

    protected BibliographicReference handleSaveBibliographicReferenceByLookup( java.lang.String id,
            java.lang.String databaseName ) throws java.lang.Exception {

        ExternalDatabase ed = this.getExternalDatabaseDao().findByName( databaseName );
        DatabaseEntry dbe = DatabaseEntry.Factory.newInstance();
        dbe.setAccession( id );
        dbe.setExternalDatabase( ed ); // should be saved by composition
        PubMedXMLFetcher fetch = new PubMedXMLFetcher();
        int pubmedID = new Integer( id ).intValue();
        BibliographicReference br = fetch.retrieveByHTTP( pubmedID );
        br.setPubAccession( dbe );
        return ( BibliographicReference ) this.getBibliographicReferenceDao().create( br );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.common.description.BibliographicReferenceServiceBase#handleGetBibliographicReferenceByTitle(java.lang.String)
     */
    protected BibliographicReference handleGetBibliographicReferenceByTitle( String title ) throws Exception {

        return getBibliographicReferenceDao().findByTitle( title );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.common.description.BibliographicReferenceServiceBase#handleLoadBibliographicReference(java.lang.Long)
     */
    protected BibliographicReference handleLoadBibliographicReference( Long id ) throws Exception {
        return ( BibliographicReference ) getBibliographicReferenceDao().load( id );
    }

    /**
     * @see edu.columbia.gemma.common.description.BibliographicReferenceService#saveBibliographicReference(edu.columbia.gemma.common.description.BibliographicReference)
     */
    protected void handleSaveBibliographicReference(
            edu.columbia.gemma.common.description.BibliographicReference bibliographicReference )
            throws java.lang.Exception {
        getBibliographicReferenceDao().create( bibliographicReference );
    }

    /**
     * @see edu.columbia.gemma.common.description.BibliographicReferenceService#saveBibliographicReference(edu.columbia.gemma.common.description.BibliographicReference)
     */
    protected void handleUpdateBibliographicReference(
            edu.columbia.gemma.common.description.BibliographicReference BibliographicReference )
            throws java.lang.Exception {
        getBibliographicReferenceDao().update( BibliographicReference );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.common.description.BibliographicReferenceServiceBase#handleGetAllBibliographicReferences()
     */
    @Override
    protected Collection handleGetAllBibliographicReferences() throws Exception {
        return this.getBibliographicReferenceDao().loadAll();
    }

    @Override
    protected void handleRemoveBibliographicReference( BibliographicReference bibliographicReference ) throws Exception {
        this.getBibliographicReferenceDao().remove( bibliographicReference );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.common.description.BibliographicReferenceServiceBase#handleAddDocument(byte[],
     *      edu.columbia.gemma.common.description.BibliographicReference)
     */
    @Override
    protected void handleAddPDF( LocalFile pdfFile, BibliographicReference bibliographicReference ) throws Exception {
        bibliographicReference.setFullTextPDF( pdfFile );
        this.getBibliographicReferenceDao().update( bibliographicReference );

    }

}