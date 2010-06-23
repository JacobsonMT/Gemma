/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.web.controller.common.description.bibref;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.web.controller.BaseController;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * This controller is responsible for showing a list of all bibliographic references, as well sending the user to the
 * pubMed.Detail.view when they click on a specific link in that list.
 * 
 * @author keshav
 */
@Controller
@RequestMapping("/bibRef")
public class BibliographicReferenceController extends BaseController {
    private static Log log = LogFactory.getLog( BibliographicReferenceController.class.getName() );

    @Autowired
    private BibliographicReferenceService bibliographicReferenceService = null;
    @Autowired
    private PersisterHelper persisterHelper;
    private final String messagePrefix = "Reference with PubMed Id";
    private PubMedXMLFetcher pubMedXmlFetcher = new PubMedXMLFetcher();

    /**
     * Add or update a record.
     * 
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/bibRefAdd.html")
    public ModelAndView add( HttpServletRequest request, HttpServletResponse response ) {
        String pubMedId = request.getParameter( "acc" ); // FIXME: allow use of the primary key as well.

        if ( pubMedId == null ) {
            throw new EntityNotFoundException( "Must provide a PubMed Id" );
        }

        BibliographicReference bibRef = bibliographicReferenceService.findByExternalId( pubMedId );
        if ( bibRef == null ) {
            bibRef = this.pubMedXmlFetcher.retrieveByHTTP( Integer.parseInt( pubMedId ) );
            if ( bibRef == null ) {
                throw new EntityNotFoundException( "Could not locate reference with pubmed id=" + pubMedId );
            }
            bibRef = ( BibliographicReference ) persisterHelper.persist( bibRef );
            saveMessage( request, "Added " + pubMedId + " to the system." );
        } else if ( StringUtils.isNotBlank( request.getParameter( "refresh" ) ) ) {

            this.update( bibRef.getId() );
            bibRef = bibliographicReferenceService.load( bibRef.getId() );

            saveMessage( request, "Updated record for pubmed id " + pubMedId );
        }

        return new ModelAndView( "bibRefView" ).addObject( "bibliographicReference", bibRef ).addObject(
                "existsInSystem", Boolean.TRUE );
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/deleteBibRef.html")
    public ModelAndView delete( HttpServletRequest request, HttpServletResponse response ) {
        String pubMedId = request.getParameter( "acc" );

        if ( pubMedId == null ) {
            // should be a validation error.
            throw new EntityNotFoundException( "Must provide a PubMed Id" );
        }

        BibliographicReference bibRef = bibliographicReferenceService.findByExternalId( pubMedId );
        if ( bibRef == null ) {
            String message = "There is no reference with accession=" + pubMedId + " in the system any more.";
            saveMessage( request, message );
            return new ModelAndView( "bibRefView" ).addObject( "errors", message );
        }

        return doDelete( request, bibRef );
    }

    /**
     * @param bibliographicReferenceService The bibliographicReferenceService to set.
     */
    public void setBibliographicReferenceService( BibliographicReferenceService bibliographicReferenceService ) {
        this.bibliographicReferenceService = bibliographicReferenceService;
    }

    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/bibRefView.html")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {
        String pubMedId = request.getParameter( "accession" );

        // FIXME: allow use of the primary key as well.

        if ( StringUtils.isBlank( pubMedId ) ) {
            throw new EntityNotFoundException( "Must provide a PubMed Id" );
        }

        BibliographicReference bibRef = bibliographicReferenceService.findByExternalId( pubMedId );
        if ( bibRef == null ) {
            bibRef = this.pubMedXmlFetcher.retrieveByHTTP( Integer.parseInt( pubMedId ) );
            if ( bibRef == null ) {
                throw new EntityNotFoundException( "Could not locate reference with pubmed id=" + pubMedId
                        + ", either in Gemma or at NCBI" );
            }
        }

        bibRef = bibliographicReferenceService.thaw( bibRef );

        boolean isIncomplete = bibRef.getPublicationDate() == null;
        addMessage( request, "object.found", new Object[] { messagePrefix, pubMedId } );
        return new ModelAndView( "bibRefView" ).addObject( "bibliographicReference", bibRef ).addObject(
                "existsInSystem", Boolean.TRUE ).addObject( "incompleteEntry", isIncomplete );
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/showAllEeBibRefs.html")
    public ModelAndView showAllForExperiments( HttpServletRequest request, HttpServletResponse response ) {
        Map<ExpressionExperiment, BibliographicReference> allExperimentLinkedReferences = bibliographicReferenceService
                .getAllExperimentLinkedReferences();

        Collection<BibliographicReferenceValueObject> vos = new HashSet<BibliographicReferenceValueObject>();
        for ( ExpressionExperiment e : allExperimentLinkedReferences.keySet() ) {
            BibliographicReference b = allExperimentLinkedReferences.get( e );

            // no thaw needed as the fetch method does partial thaw.
            // b = bibliographicReferenceService.thaw( b );
            BibliographicReferenceValueObject vo = new BibliographicReferenceValueObject( b );

            if ( !vos.contains( vo ) ) {
                vos.add( vo );
            }

            vo.getExperiments().add( e );
        }

        return new ModelAndView( "bibRefList" ).addObject( "bibliographicReferences", vos );
    }

    /**
     * For AJAX calls. Refresh the Gemma entry based on information from PubMed.
     * 
     * @param id
     */
    public void update( Long id ) {
        BibliographicReference bibRef = bibliographicReferenceService.load( id );
        if ( bibRef == null ) {
            throw new EntityNotFoundException( "Could not locate reference with that id" );
        }
        bibRef = bibliographicReferenceService.thaw( bibRef );

        String pubMedId = bibRef.getPubAccession().getAccession();
        BibliographicReference fresh = this.pubMedXmlFetcher.retrieveByHTTP( Integer.parseInt( pubMedId ) );

        if ( fresh == null || fresh.getPublicationDate() == null ) {
            throw new IllegalStateException( "Unable to retrive record from pubmed for id=" + pubMedId );
        }

        assert fresh.getPubAccession().getAccession().equals( pubMedId );

        bibRef.setPublicationDate( fresh.getPublicationDate() );
        bibRef.setAuthorList( fresh.getAuthorList() );
        bibRef.setAbstractText( fresh.getAbstractText() );
        bibRef.setIssue( fresh.getIssue() );
        bibRef.setCitation( fresh.getCitation() );
        bibRef.setPublication( fresh.getPublication() );
        bibRef.setMeshTerms( fresh.getMeshTerms() );
        bibRef.setChemicals( fresh.getChemicals() );
        bibRef.setKeywords( fresh.getKeywords() );

        bibliographicReferenceService.update( bibRef );
    }

    /**
     * @param request
     * @param locale
     * @param bibRef
     * @return
     */
    private ModelAndView doDelete( HttpServletRequest request, BibliographicReference bibRef ) {
        bibliographicReferenceService.remove( bibRef );
        log.info( "Bibliographic reference with pubMedId: " + bibRef.getPubAccession().getAccession() + " deleted" );
        addMessage( request, "object.deleted", new Object[] { messagePrefix, bibRef.getPubAccession().getAccession() } );
        return new ModelAndView( "bibRefView", "bibliographicReference", bibRef );
    }

}
