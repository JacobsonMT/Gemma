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
package ubic.gemma.web.controller.genome.gene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.image.aba.AllenBrainAtlasService;
import ubic.gemma.image.aba.Image;
import ubic.gemma.image.aba.ImageSeries;
import ubic.gemma.loader.genome.gene.ncbi.homology.HomologeneService;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.ontology.providers.GeneOntologyService;
import ubic.gemma.web.controller.BaseController;
import ubic.gemma.web.controller.expression.experiment.AnnotationValueObject;

/**
 * @author daq2101
 * @author pavlidis
 * @author joseph
 * @version $Id$
 */
@Controller
@RequestMapping("/gene")
public class GeneController extends BaseController {

    private static Log log = LogFactory.getLog( GeneController.class );

    @Autowired
    private AllenBrainAtlasService allenBrainAtlasService = null;

    @Autowired
    private Gene2GOAssociationService gene2GOAssociationService = null;

    @Autowired
    private GeneOntologyService geneOntologyService;

    @Autowired
    private HomologeneService homologeneService = null;

    @Autowired
    private TaxonService taxonService = null;

    @Autowired
    private GeneService geneService = null;

    /**
     * For ajax
     * 
     * @param geneDelegator
     * @return
     */
    public Collection<AnnotationValueObject> findGOTerms( Long geneId ) {
        if ( geneId == null ) throw new IllegalArgumentException( "Null id for gene" );
        Collection<AnnotationValueObject> ontos = new HashSet<AnnotationValueObject>();
        Gene g = geneService.load( geneId );

        if ( g == null ) {
            throw new IllegalArgumentException( "No such gene could be loaded with id=" + geneId );
        }

        Collection<Gene2GOAssociation> associations = gene2GOAssociationService.findAssociationByGene( g );

        for ( Gene2GOAssociation assoc : associations ) {

            if ( assoc.getOntologyEntry() == null ) continue;

            AnnotationValueObject annot = new AnnotationValueObject();

            annot.setId( assoc.getOntologyEntry().getId() );
            annot.setTermName( geneOntologyService.getTermName( assoc.getOntologyEntry().getValue() ) );
            annot.setTermUri( assoc.getOntologyEntry().getValue() );
            annot.setEvidenceCode( assoc.getEvidenceCode().getValue() );
            annot.setDescription( assoc.getOntologyEntry().getDescription() );
            annot.setClassUri( assoc.getOntologyEntry().getCategoryUri() );
            annot.setClassName( assoc.getOntologyEntry().getCategory() );

            ontos.add( annot );
        }
        cleanup( ontos );
        return ontos;
    }

    /**
     * For ajax.
     * 
     * @param geneDelegator
     * @return
     */
    public Collection<GeneProduct> getProducts( Long geneId ) {
        if ( geneId == null ) throw new IllegalArgumentException( "Null id for gene" );
        Gene gene = geneService.load( geneId );

        if ( gene == null ) throw new IllegalArgumentException( "No gene with id " + geneId );

        return gene.getProducts();
    }

    public void setAllenBrainAtlasService( AllenBrainAtlasService allenBrainAtlasService ) {
        this.allenBrainAtlasService = allenBrainAtlasService;
    }

    /**
     * @param gene2GOAssociationService the gene2GOAssociationService to set
     */
    public void setGene2GOAssociationService( Gene2GOAssociationService gene2GOAssociationService ) {
        this.gene2GOAssociationService = gene2GOAssociationService;
    }

    public void setGeneOntologyService( GeneOntologyService geneOntologyService ) {
        this.geneOntologyService = geneOntologyService;
    }

    /**
     * @param geneService The geneService to set.
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    public void setHomologeneService( HomologeneService homologeneService ) {
        this.homologeneService = homologeneService;
    }

    public void setTaxonService( TaxonService ts ) {
        this.taxonService = ts;
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @RequestMapping(value = "/showGene.html", method = RequestMethod.GET)
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {

        Long id = null;

        String ncbiId = null;

        Gene gene = null;

        try {
            id = Long.parseLong( request.getParameter( "id" ) );
            gene = geneService.load( id );

            if ( gene == null ) {
                addMessage( request, "object.notfound", new Object[] { "Gene " + id } );
                return new ModelAndView( "index" );
            }
        } catch ( NumberFormatException e ) {
            ncbiId = request.getParameter( "ncbiid" );

            if ( StringUtils.isNotBlank( ncbiId ) ) {
                gene = geneService.findByNCBIId( ncbiId );
            } else {
                addMessage( request, "object.notfound", new Object[] { "Gene" } );
                return new ModelAndView( "index" );
            }
        }

        if ( gene == null ) {
            addMessage( request, "object.notfound", new Object[] { "Gene " + id } );
            return new ModelAndView( "index" );
        }

        id = gene.getId();

        assert id != null;

        ModelAndView mav = new ModelAndView( "gene.detail" );
        mav.addObject( "gene", gene );

        // Get the composite sequences
        Long compositeSequenceCount = geneService.getCompositeSequenceCountById( id );
        mav.addObject( "compositeSequenceCount", compositeSequenceCount );

        getAllenBrainImages( gene, mav );

        Collection<Gene> homologues = homologeneService.getHomologues( gene );

        if ( homologues != null && !homologues.isEmpty() ) {
            mav.addObject( "homologues", homologues );
        }

        return mav;
    }

    /**
     * @param request
     * @param response
     * @return ModelAndView
     */
    @RequestMapping(value = "/showGenes.html", method = RequestMethod.GET)
    public ModelAndView showMultiple( HttpServletRequest request, HttpServletResponse response ) {

        String sId = request.getParameter( "id" );
        Collection<Gene> genes = new ArrayList<Gene>();
        // if no IDs are specified, then show an error message
        if ( sId == null ) {
            addMessage( request, "object.notfound", new Object[] { "All genes cannot be listed. Genes " } );
        }

        // if ids are specified, then display only those genes
        else {
            String[] idList = StringUtils.split( sId, ',' );

            for ( int i = 0; i < idList.length; i++ ) {
                Long id = Long.parseLong( idList[i] );
                Gene gene = geneService.load( id );
                if ( gene == null ) {
                    addMessage( request, "object.notfound", new Object[] { "Gene " + id } );
                }
                genes.add( gene );
            }
        }
        /*
         * FIXME this view doesn't exist!
         */
        return new ModelAndView( "genes" ).addObject( "genes", genes );

    }

    /**
     * @param request
     * @param response
     * @return ModelAndView
     */
    @RequestMapping("/showCompositeSequences.html")
    public ModelAndView showCompositeSequences( HttpServletRequest request, HttpServletResponse response ) {

        /*
         * FIXME is this used?
         */

        // gene id.
        Long id = Long.parseLong( request.getParameter( "id" ) );
        Gene gene = geneService.load( id );
        if ( gene == null ) {
            addMessage( request, "object.notfound", new Object[] { "Gene " + id } );
            StringBuffer requestURL = request.getRequestURL();
            log.info( requestURL );
            return new ModelAndView( "mainMenu.html" );
        }
        Collection<CompositeSequence> compositeSequences = geneService.getCompositeSequencesById( id );

        ModelAndView mav = new ModelAndView( "compositeSequences.geneMap" );
        mav.addObject( "numCompositeSequences", compositeSequences.size() );

        // fill in by ajax instead.
        // Collection<Object[]> rawSummaries = compositeSequenceService.getRawSummary( compositeSequences, 0 );
        // Collection<CompositeSequenceMapValueObject> summaries = arrayDesignMapResultService
        // .getSummaryMapValueObjects( rawSummaries );
        //
        // if ( summaries == null || summaries.size() == 0 ) {
        // // / FIXME, return error or do something else intelligent.
        // }
        // mav.addObject( "sequenceData", summaries );

        StringBuilder buf = new StringBuilder();
        for ( CompositeSequence sequence : compositeSequences ) {
            buf.append( sequence.getId() );
            buf.append( "," );
        }
        mav.addObject( "compositeSequenceIdList", buf.toString().replaceAll( ",$", "" ) );

        mav.addObject( "gene", gene );

        return mav;
    }

    /**
     * Remove root terms.
     * 
     * @param associations
     */
    private void cleanup( Collection<AnnotationValueObject> associations ) {
        for ( Iterator<AnnotationValueObject> it = associations.iterator(); it.hasNext(); ) {
            String term = it.next().getTermName();
            if ( term == null ) continue;
            if ( term.equals( "molecular_function" ) || term.equals( "biological_process" )
                    || term.equals( "cellular_component" ) ) {
                it.remove();
            }
        }
    }

    /**
     * @param gene
     * @param mav
     */
    private void getAllenBrainImages( Gene gene, ModelAndView mav ) {
        final Taxon mouseTaxon = this.taxonService.findByCommonName( "mouse" );
        Gene mouseGene = gene;
        // Get alan brain atalas represntative images
        if ( gene.getTaxon().getId().equals( mouseTaxon.getId() ) ) {
            mouseGene = this.homologeneService.getHomologue( gene, mouseTaxon );
        }

        if ( mouseGene != null ) {
            Collection<ImageSeries> imageSeries = null;

            try {
                imageSeries = allenBrainAtlasService.getRepresentativeSaggitalImages( mouseGene.getOfficialSymbol() );
                String abaGeneUrl = allenBrainAtlasService.getGeneUrl( mouseGene.getOfficialSymbol() );

                Collection<Image> representativeImages = allenBrainAtlasService.getImagesFromImageSeries( imageSeries );

                if ( !representativeImages.isEmpty() ) {
                    mav.addObject( "representativeImages", representativeImages );
                    mav.addObject( "abaGeneUrl", abaGeneUrl );
                    mav.addObject( "homologousMouseGene", mouseGene );
                }
            } catch ( IOException e ) {
                log.warn( "Could not get ABA data: " + e );
            }

        }
    }

}