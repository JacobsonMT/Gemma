package ubic.gemma.loader.expression.arrayDesign;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.RandomStringUtils;

import ubic.gemma.loader.genome.FastaCmd;
import ubic.gemma.loader.util.parser.ExternalDatabaseUtils;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.PolymerType;

class MockFastaCmd implements FastaCmd {

    private Taxon taxon;

    public MockFastaCmd( Taxon t ) {
        this.taxon = t;
    }

    @Override
    public BioSequence getByAccession( String accession, String database ) {
        return getSingle( accession, database, null );
    }

    @Override
    public BioSequence getByIdentifier( int identifier, String database ) {
        return getSingle( new Integer( identifier ), database, null );
    }

    @Override
    public Collection<BioSequence> getBatchAccessions( Collection<String> accessions, String database ) {
        return getMultiple( accessions, database, null );
    }

    @Override
    public Collection<BioSequence> getBatchIdentifiers( Collection<Integer> identifiers, String database ) {
        return getMultiple( identifiers, database, null );
    }

    @Override
    public BioSequence getByAccession( String accession, String database, String blastHome ) {
        return getSingle( accession, database, blastHome );
    }

    @SuppressWarnings("unused")
    private BioSequence getSingle( Object accession, String database, String blastHome ) {
        BioSequence result = makeSequence( accession );
        return result;
    }

    @SuppressWarnings("unused")
    private Collection<BioSequence> getMultiple( Collection<? extends Object> accessions, String database,
            String blastHome ) {
        Collection<BioSequence> results = new HashSet<BioSequence>();
        for ( Object object : accessions ) {
            BioSequence result = makeSequence( object );

            results.add( result );
        }
        return results;
    }

    /**
     * @param object
     * @return
     */
    private BioSequence makeSequence( Object object ) {
        BioSequence result = BioSequence.Factory.newInstance( taxon );
        result.setName( object.toString() );
        result.setLength( 100L );
        result.setPolymerType( PolymerType.DNA );
        result.setIsApproximateLength( false );
        result.setIsCircular( false );
        result.setFractionRepeats( 0.0 );
        result.setSequence( RandomStringUtils.random( 100, "ATGC" ) );
        DatabaseEntry genbank = ExternalDatabaseUtils.getGenbankAccession( object.toString() );
        result.setSequenceDatabaseEntry( genbank );
        return result;
    }

    @Override
    public BioSequence getByIdentifier( int identifier, String database, String blastHome ) {
        return getSingle( new Integer( identifier ), database, blastHome );
    }

    @Override
    public Collection<BioSequence> getBatchAccessions( Collection<String> accessions, String database, String blastHome ) {
        return getMultiple( accessions, database, blastHome );
    }

    @Override
    public Collection<BioSequence> getBatchIdentifiers( Collection<Integer> identifiers, String database,
            String blastHome ) {
        return getMultiple( identifiers, database, blastHome );
    }
}