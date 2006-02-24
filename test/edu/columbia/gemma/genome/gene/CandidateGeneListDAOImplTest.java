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
package edu.columbia.gemma.genome.gene;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.BaseTransactionalSpringContextTest;
import edu.columbia.gemma.common.auditAndSecurity.AuditTrail;
import edu.columbia.gemma.common.auditAndSecurity.Person;
import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.GeneDao;
import edu.columbia.gemma.genome.Taxon;
import edu.columbia.gemma.genome.TaxonDao;
import edu.columbia.gemma.loader.loaderutils.PersisterHelper;

/**
 * @author daq2101
 * @version $Id$
 */
public class CandidateGeneListDAOImplTest extends BaseTransactionalSpringContextTest {
    private final Log log = LogFactory.getLog( CandidateGeneListDAOImplTest.class );

    private CandidateGeneListDao candidateGeneListDao;
    private TaxonDao taxonDao;
    private GeneDao geneDao;
    private PersisterHelper persisterHelper;
    private Gene g;
    private Gene g2;
    private Taxon t;
    private CandidateGeneList candidateGeneList;

    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        candidateGeneList = CandidateGeneList.Factory.newInstance();
        AuditTrail ad = AuditTrail.Factory.newInstance();
        ad = ( AuditTrail ) persisterHelper.persist( ad );
        candidateGeneList.setAuditTrail( ad );

        t = Taxon.Factory.newInstance();
        t.setCommonName( "mouse" );
        t.setScientificName( "Mus musculus" );
        t = taxonDao.findOrCreate( t );
        ad = AuditTrail.Factory.newInstance();
        g = Gene.Factory.newInstance();
        g.setName( "testmygene" );
        g.setOfficialSymbol( "foo" );
        g.setOfficialName( "testmygene" );
        g.setTaxon( t );
        ad = AuditTrail.Factory.newInstance();
        ad = ( AuditTrail ) persisterHelper.persist( ad );
        g.setAuditTrail( ad );
        g = geneDao.findOrCreate( g );

        g2 = Gene.Factory.newInstance();
        g2.setName( "testmygene2" );
        g2.setOfficialSymbol( "foo2" );
        g2.setOfficialName( "testmygene2" );
        g2.setTaxon( t );
        ad = AuditTrail.Factory.newInstance();
        ad = ( AuditTrail ) persisterHelper.persist( ad );
        g2.setAuditTrail( ad );
        g2 = geneDao.findOrCreate( g2 );

        ad = AuditTrail.Factory.newInstance();
        ad = ( AuditTrail ) persisterHelper.persist( ad );

        candidateGeneList.setAuditTrail( ad );

        Person u = Person.Factory.newInstance();
        u.setName( "Joe Blow" );
        u = ( Person ) persisterHelper.persist( u );
        assert u != null;

        candidateGeneList.setOwner( u );

        candidateGeneList = ( CandidateGeneList ) candidateGeneListDao.create( candidateGeneList );

    }

    public final void testAddGeneToList() throws Exception {
        log.info( "testing adding gene to list" );
        assert candidateGeneList != null;
        CandidateGene cg = candidateGeneList.addCandidate( g );
        assertEquals( candidateGeneList.getCandidates().size(), 1 );
        assertEquals( cg.getGene().getName(), "testmygene" );
    }

    public final void testRemoveGeneFromList() throws Exception {
        log.info( "testing removing gene from list" );
        assert candidateGeneList != null;
        CandidateGene cg = candidateGeneList.addCandidate( g2 );
        log.info( candidateGeneList.getCandidates().size() + " candidates to start" );
        cg = candidateGeneList.getCandidates().iterator().next(); // get the persistent object.
        log.info( candidateGeneList.getCandidates().size() + " candidates just before deleting" );
        candidateGeneList.removeCandidate( cg );
        log.info( candidateGeneList.getCandidates().size() + " candidates left" );
        assert ( candidateGeneList.getCandidates().size() == 0 );
    }

    public final void testRankingChanges() throws Exception {
        log.info( "testing ranking changes" );
        assert candidateGeneList != null;
        CandidateGene cg1 = candidateGeneList.addCandidate( g );
        CandidateGene cg2 = candidateGeneList.addCandidate( g2 );
        candidateGeneList.increaseRanking( cg2 );
        Collection c = candidateGeneList.getCandidates();
        for ( Iterator iter = c.iterator(); iter.hasNext(); ) {
            cg1 = ( CandidateGene ) iter.next();
            if ( cg1.getGene().getName().matches( "testmygene2" ) )
                assertEquals( cg1.getRank().intValue(), 0 );
            else
                assertEquals( cg1.getRank().intValue(), 1 );
        }
    }

    /**
     * @param candidateGeneListDao The candidateGeneListDao to set.
     */
    public void setCandidateGeneListDao( CandidateGeneListDao candidateGeneListDao ) {
        this.candidateGeneListDao = candidateGeneListDao;
    }

    /**
     * @param geneDao The geneDao to set.
     */
    public void setGeneDao( GeneDao geneDao ) {
        this.geneDao = geneDao;
    }

    /**
     * @param persisterHelper The persisterHelper to set.
     */
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /**
     * @param taxonDao The taxonDao to set.
     */
    public void setTaxonDao( TaxonDao taxonDao ) {
        this.taxonDao = taxonDao;
    }
}
