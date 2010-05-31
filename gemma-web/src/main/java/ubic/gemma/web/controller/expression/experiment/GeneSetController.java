/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.web.controller.expression.experiment;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetImpl;
import ubic.gemma.model.genome.gene.GeneSetMember;
import ubic.gemma.model.genome.gene.GeneSetService;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.search.GeneSetSearch;
import ubic.gemma.security.SecurityService;
import ubic.gemma.web.controller.common.auditAndSecurity.GeneSetValueObject;

/**
 * Exposes GeneServices methods over ajax
 * 
 * @author kelsey
 * @version $Id
 */
@Controller
public class GeneSetController {

    private static Log log = LogFactory.getLog( GeneSetController.class );

    private static final Double DEFAULT_SCORE = 0.0;

    @Autowired
    private GeneService geneService = null;

    @Autowired
    private GeneSetSearch geneSetSearch;

    @Autowired
    private GeneSetService geneSetService = null;

    @Autowired
    private SecurityService securityService = null;

    @Autowired
    private TaxonService taxonService = null;

    /**
     * AJAX Creates a new gene group given a name for the group and the genes in the group
     * 
     * @param geneSetVo value object constructed on the client.
     * @return id of the new gene group
     */
    public Collection<GeneSetValueObject> create( Collection<GeneSetValueObject> geneSetVos ) {

        Collection<GeneSetValueObject> results = new HashSet<GeneSetValueObject>();
        for ( GeneSetValueObject geneSetVo : geneSetVos ) {

            if ( StringUtils.isBlank( geneSetVo.getName() ) ) {
                throw new IllegalArgumentException( "Gene group name cannot be blank" );
            }

            GeneSet gset = create( geneSetVo );
            results.add( new GeneSetValueObject( gset ) );

        }
        return results;
    }

    /**
     * Given a Gemma Gene Id will find all gene groups that the current user is allowed to use
     * 
     * @param geneId
     * @return collection of geneSetValueObject
     */
    public Collection<GeneSetValueObject> findGeneSetsByGene( Long geneId ) {

        Gene gene = geneService.load( geneId );

        Collection<GeneSet> genesets = this.geneSetSearch.findByGene( gene );

        return GeneSetValueObject.convert2ValueObjects( genesets, false );
    }

    /**
     * @param query string to match to a gene set.
     * @param taxonId
     * @return
     */
    public Collection<GeneSetValueObject> findGeneSetsByName( String query, Long taxonId ) {

        if ( StringUtils.isBlank( query ) ) {
            return new HashSet<GeneSetValueObject>();
        }

        if ( taxonId == null ) {
            throw new IllegalArgumentException( "Taxon must not be null" );
        }

        Taxon tax = taxonService.load( taxonId );

        if ( tax == null ) {
            throw new IllegalArgumentException( "Can't locate taxon with id=" + taxonId );
        }

        Collection<GeneSet> foundGeneSets = this.geneSetSearch.findByName( query, tax );

        /*
         * Behaviour implemented here (easy to change): If we have a match in our system we stop here. Otherwise, we go
         * on to search the Gene Ontology.
         */

        if ( foundGeneSets.isEmpty() ) {
            if ( query.toUpperCase().startsWith( "GO" ) ) {
                GeneSet goSet = this.geneSetSearch.findByGoId( query, tax );
                if ( goSet != null ) foundGeneSets.add( goSet );
            } else {
                foundGeneSets.addAll( geneSetSearch.findByGoTermName( query, tax ) );
            }
        }

        return GeneSetValueObject.convert2ValueObjects( foundGeneSets, false );
    }

    /**
     * AJAX If the current user has access to given gene group will return the gene ids in the gene group
     * 
     * @param groupId
     * @return
     */
    public Collection<GeneValueObject> getGenesInGroup( Long groupId ) {

        Collection<GeneValueObject> results = null;

        GeneSet gs = geneSetService.load( groupId );
        if ( gs == null ) return null; // FIXME: Send and error code/feedback?

        results = GeneValueObject.convertMembers2GeneValueObjects( gs.getMembers() );

        return results;

    }

    /**
     * AJAX Returns just the current users gene sets
     * 
     * @param privateOnly
     * @return
     */
    public Collection<GeneSetValueObject> getUsersGeneGroups( boolean privateOnly ) {

        Collection<GeneSet> geneSets = new HashSet<GeneSet>();
        if ( privateOnly ) {
            try {
                geneSets = geneSetService.loadMyGeneSets();
                geneSets.retainAll( securityService.choosePrivate( geneSets ) );
            } catch ( AccessDeniedException e ) {
                // okay, they just aren't allowed to see those.
            }

        } else {
            geneSets = geneSetService.loadAll();
        }

        Collection<GeneSetValueObject> result = makeValueObects( geneSets );
        return result;
    }

    /**
     * AJAX Given a valid gene group will remove it from db (if the user has permissons to do so).
     * 
     * @param groups
     */
    public Collection<GeneSetValueObject> remove( Collection<GeneSetValueObject> vos ) {
        for ( GeneSetValueObject geneSetValueObject : vos ) {
            GeneSet gset = geneSetService.load( geneSetValueObject.getId() );
            if ( gset != null ) geneSetService.remove( gset );
        }
        return new HashSet<GeneSetValueObject>();
    }

    /**
     * AJAX Updates the given gene group (permission permitting) with the given list of geneIds Will not allow the same
     * gene to be added to the gene set twice.
     * 
     * @param groupId
     * @param geneIds
     */
    public Collection<GeneSetValueObject> update( Collection<GeneSetValueObject> geneSetVos ) {

        Collection<GeneSet> updated = new HashSet<GeneSet>();
        for ( GeneSetValueObject geneSetVo : geneSetVos ) {

            Long groupId = geneSetVo.getId();
            GeneSet gset = geneSetService.load( groupId );
            if ( gset == null ) {
                throw new IllegalArgumentException( "No gene set with id=" + groupId + " could be loaded" );
            }
            Collection<GeneSetMember> updatedGenelist = new HashSet<GeneSetMember>();

            Collection<Long> geneIds = geneSetVo.getGeneIds();

            if ( !geneIds.isEmpty() ) {
                Collection<Gene> genes = geneService.loadMultiple( geneIds );

                if ( genes.isEmpty() ) {
                    throw new IllegalArgumentException( "None of the gene ids were valid (out of " + geneIds.size()
                            + " provided)" );
                }
                if ( genes.size() < geneIds.size() ) {
                    throw new IllegalArgumentException( "Some of the gene ids were invalid: only found " + genes.size()
                            + " out of " + geneIds.size() + " provided)" );
                }

                assert genes.size() == geneIds.size();

                for ( Gene g : genes ) {

                    GeneSetMember gsm = GeneSetImpl.containsGene( g, gset );

                    // Gene not in list create memember and add it.
                    if ( gsm == null ) {
                        GeneSetMember gmember = GeneSetMember.Factory.newInstance();
                        gmember.setGene( g );
                        gmember.setScore( DEFAULT_SCORE );
                        gset.getMembers().add( gmember );
                        updatedGenelist.add( gmember );
                    } else {
                        updatedGenelist.add( gsm );
                    }

                }
            }

            gset.getMembers().clear();
            gset.getMembers().addAll( updatedGenelist );
            gset.setDescription( geneSetVo.getDescription() );
            gset.setName( geneSetVo.getName() );
            geneSetService.update( gset );

            /*
             * Make sure we return the latest.
             */
            updated.add( geneSetService.load( gset.getId() ) );
        }
        return makeValueObects( updated );

    }

    private GeneSet create( GeneSetValueObject geneSetVo ) {
        GeneSet newGeneSet = GeneSet.Factory.newInstance();
        newGeneSet.setName( geneSetVo.getName() );
        newGeneSet.setDescription( geneSetVo.getDescription() );

        // If no gene Ids just create group and return.
        GeneSet gset = geneSetService.create( newGeneSet );
        this.securityService.makePrivate( gset );

        Collection<Long> geneIds = geneSetVo.getGeneIds();

        if ( geneIds != null && !geneIds.isEmpty() ) {
            Collection<Gene> genes = geneService.loadMultiple( geneIds );
            for ( Gene g : genes ) {
                GeneSetMember gmember = GeneSetMember.Factory.newInstance();
                gmember.setGene( g );
                gmember.setScore( DEFAULT_SCORE );
                gset.getMembers().add( gmember );
            }

            geneSetService.update( gset );
        }
        return geneSetService.load( gset.getId() );
    }

    /**
     * @param secs
     * @return
     */
    private Collection<GeneSetValueObject> makeValueObects( Collection<GeneSet> secs ) {
        // Create valueobject (need to add security info or would move this out into the valueobject...
        Collection<GeneSetValueObject> result = new HashSet<GeneSetValueObject>();
        for ( GeneSet gs : secs ) {

            GeneSetValueObject gsvo = new GeneSetValueObject( gs );

            gsvo.setCurrentUserHasWritePermission( securityService.isEditable( gs ) );
            gsvo.setPublik( securityService.isPublic( gs ) );
            gsvo.setShared( securityService.isShared( gs ) );

            result.add( gsvo );
        }
        return result;
    }

}
