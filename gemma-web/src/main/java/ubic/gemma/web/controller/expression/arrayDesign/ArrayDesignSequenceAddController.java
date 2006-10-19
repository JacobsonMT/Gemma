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
package ubic.gemma.web.controller.expression.arrayDesign;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.basecode.util.FileTools;
import ubic.gemma.loader.expression.arrayDesign.ArrayDesignSequenceProcessingService;
import ubic.gemma.loader.genome.taxon.SupportedTaxa;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.BackgroundProcessingFormController;
import ubic.gemma.web.controller.common.auditAndSecurity.FileUpload;
import ubic.gemma.web.propertyeditor.ArrayDesignPropertyEditor;
import ubic.gemma.web.propertyeditor.TaxonPropertyEditor;
import ubic.gemma.web.util.ConfigurationCookie;
import ubic.gemma.web.util.upload.CommonsMultipartFile;
import ubic.gemma.web.util.upload.FileUploadUtil;

/**
 * Controller for associating sequences with an existing arrayDesign.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="arrayDesignSequenceAddController"
 * @spring.property name="commandName" value="arrayDesignSequenceAddCommand"
 * @spring.property name="commandClass"
 *                  value="ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignSequenceAddCommand"
 * @spring.property name="formView" value="arrayDesignSequenceAdd"
 * @spring.property name="successView" value="redirect:/arrayDesign/associateSequences.html"
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 * @spring.property name="validator" ref="arrayDesignSequenceAddValidator"
 * @spring.property name="taxonService" ref="taxonService"
 * @spring.property name="arrayDesignSequenceProcessingService" ref="arrayDesignSequenceProcessingService"
 */
public class ArrayDesignSequenceAddController extends BackgroundProcessingFormController {

    private static final String COOKIE_NAME = "arrayDesignSequenceAddCookie";

    TaxonService taxonService;

    ArrayDesignService arrayDesignService;

    ArrayDesignSequenceProcessingService arrayDesignSequenceProcessingService;

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
     */
    @Override
    @SuppressWarnings("unused")
    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        ArrayDesignSequenceAddCommand adsac = new ArrayDesignSequenceAddCommand();
        loadCookie( request, adsac );
        return adsac;
    }

    /**
     * @param request
     * @param adsac
     */
    private void loadCookie( HttpServletRequest request, ArrayDesignSequenceAddCommand adsac ) {
        for ( Cookie cook : request.getCookies() ) {
            if ( cook.getName().equals( COOKIE_NAME ) ) {
                try {
                    ConfigurationCookie cookie = new ConfigurationCookie( cook );
                    TaxonPropertyEditor taxed = new TaxonPropertyEditor( taxonService );
                    taxed.setAsText( cookie.getString( "taxon" ) );

                    adsac.setSequenceType( ( SequenceType.fromString( cookie.getString( "sequenceType" ) ) ) );
                    adsac.setTaxon( ( Taxon ) taxed.getValue() );
                } catch ( Exception e ) {
                    log.warn( "Cookie could not be loaded: " + e.getMessage() );
                    // that's okay, we just don't get a cookie.
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.BaseFormController#initBinder(javax.servlet.http.HttpServletRequest,
     *      org.springframework.web.bind.ServletRequestDataBinder)
     */
    @Override
    protected void initBinder( HttpServletRequest request, ServletRequestDataBinder binder ) {
        super.initBinder( request, binder );
        binder.registerCustomEditor( ArrayDesign.class, new ArrayDesignPropertyEditor( this.arrayDesignService ) );
        binder.registerCustomEditor( Taxon.class, new TaxonPropertyEditor( this.taxonService ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(java.lang.Object,
     *      org.springframework.validation.BindException)
     */
    @Override
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {
        log.info( "Entering onSubmit" );
        ArrayDesignSequenceAddCommand commandObject = ( ArrayDesignSequenceAddCommand ) command;
        Cookie cookie = new ArrayDesignSequenceAddCookie( commandObject );
        response.addCookie( cookie );

        ArrayDesign arrayDesign = commandObject.getArrayDesign();

        if ( arrayDesignService.getCompositeSequenceCount( arrayDesign ) == 0 ) {
            errors.rejectValue( "arrayDesign", "arrayDesign.nocompositesequences",
                    "Array design did not have any compositesequences" );
            return showForm( request, response, errors );
        }
        ProgressJob job = ProgressManager.createProgressJob( SecurityContextHolder.getContext().getAuthentication()
                .getName(), "Processing " + arrayDesign );

        log.info( "thawing " + arrayDesign );
        arrayDesignService.thaw( arrayDesign );
        log.info( "done thawing" );
        // validate a file was entered
        FileUpload fileUpload = commandObject.getSequenceFile();
        if ( fileUpload.getName() != null && fileUpload.getFile().length == 0 ) {
            Object[] args = new Object[] { getText( "arrayDesignSequenceAddCommand.file", request.getLocale() ) };
            errors.rejectValue( "file", "errors.required", args, "File" );
            return showForm( request, response, errors );
        }

        ProgressManager.updateCurrentThreadsProgressJob( "Copying file" );
        File file = FileUploadUtil.copyUploadedFile( request, fileUpload, "sequenceFile.file" );

        if ( !file.canRead() ) {
            errors.rejectValue( "file", "errors.required", "File was not uploaded successfully?" );
            return showForm( request, response, errors );
        }

        ProgressManager.destroyProgressJob( job );

        startJob( commandObject, request, "Loading sequences for " + commandObject.getArrayDesign().getName() );

        return new ModelAndView( new RedirectView( "/Gemma/processProgress.html" ) );
    }

    class ArrayDesignSequenceAddCookie extends ConfigurationCookie {

        public ArrayDesignSequenceAddCookie( ArrayDesignSequenceAddCommand command ) {
            super( COOKIE_NAME );
            this.setProperty( "sequenceType", command.getSequenceType().toString() );
            this.setProperty( "taxon", command.getTaxon().getScientificName() );
            this.setMaxAge( 100000 );
            this.setComment( "Information for the Array Design sequence association form" );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest,
     *      java.lang.Object, org.springframework.validation.Errors)
     */
    @SuppressWarnings( { "unchecked", "unused" })
    @Override
    protected Map referenceData( HttpServletRequest request ) throws Exception {

        Map<String, List<? extends Object>> mapping = new HashMap<String, List<? extends Object>>();

        List<ArrayDesign> arrayDesignNames = new ArrayList<ArrayDesign>();

        for ( ArrayDesign arrayDesign : ( Collection<ArrayDesign> ) arrayDesignService.loadAll() ) {
            arrayDesignNames.add( arrayDesign );
        }

        List<Taxon> taxonNames = new ArrayList<Taxon>();

        for ( Taxon taxon : ( Collection<Taxon> ) taxonService.loadAll() ) {
            if ( !SupportedTaxa.contains( taxon ) ) {
                continue;
            }
            taxonNames.add( taxon );
        }

        Collections.sort( arrayDesignNames, new Comparator() {
            public int compare( Object o1, Object o2 ) {
                return ( ( ArrayDesign ) o1 ).getName().compareTo( ( ( ArrayDesign ) o2 ).getName() );
            }
        } );
        Collections.sort( taxonNames, new Comparator() {
            public int compare( Object o1, Object o2 ) {
                return ( ( Taxon ) o1 ).getScientificName().compareTo( ( ( Taxon ) o2 ).getScientificName() );
            }
        } );

        mapping.put( "arrayDesigns", arrayDesignNames );

        mapping.put( "sequenceTypes", new ArrayList<String>( SequenceType.literals() ) );

        mapping.put( "taxa", taxonNames );

        return mapping;

    }

    public void setArrayDesignSequenceProcessingService(
            ArrayDesignSequenceProcessingService arrayDesignSequenceProcessingService ) {
        this.arrayDesignSequenceProcessingService = arrayDesignSequenceProcessingService;
    }

    /**
     * @param arrayDesignService the arrayDesignService to set
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @param taxonService the taxonService to set
     */
    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.BackgroundProcessingFormController#getRunner(org.acegisecurity.context.SecurityContext,
     *      java.lang.Object, java.lang.String)
     */
    @Override
    protected BackgroundControllerJob getRunner( SecurityContext securityContext, HttpServletRequest request,
            Object command, String jobDescription ) {
        return new ArrayDesignSequenceAddJob( securityContext, request, command, jobDescription );
    }

    class ArrayDesignSequenceAddJob extends BackgroundControllerJob {

        public ArrayDesignSequenceAddJob( SecurityContext securityContext, HttpServletRequest request, Object command,
                String jobDescription ) {

            init( securityContext, request, command, jobDescription );
        }

        public void run() {
            SecurityContextHolder.setContext( securityContext );

            ArrayDesignSequenceAddCommand commandObject = ( ArrayDesignSequenceAddCommand ) command;

            FileUpload fileUpload = commandObject.getSequenceFile();

            ArrayDesign arrayDesign = commandObject.getArrayDesign();
            SequenceType sequenceType = commandObject.getSequenceType();
            Taxon taxon = commandObject.getTaxon();

            ProgressJob job = ProgressManager.createProgressJob( securityContext.getAuthentication().getName(),
                    "Loading data from " + fileUpload.getName() );

            job.setForwardingURL( "/Gemma/arrayDesign/associateSequences.html" );

            try {

                File file = fileUpload.getLocalPath();

                assert file != null;

                InputStream stream = FileTools.getInputStreamFromPlainOrCompressedFile( file.getAbsolutePath() );

                Collection<BioSequence> bioSequences = arrayDesignSequenceProcessingService.processArrayDesign(
                        arrayDesign, stream, sequenceType, taxon );

                stream.close();

                this.saveMessage( this.session, "Successfully loaded " + bioSequences.size() + " sequences for "
                        + arrayDesign );

            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }

            ProgressManager.destroyProgressJob( job );
        }
    }
}
