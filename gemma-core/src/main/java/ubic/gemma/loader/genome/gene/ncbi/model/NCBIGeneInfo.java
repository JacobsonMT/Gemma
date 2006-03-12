package ubic.gemma.loader.genome.gene.ncbi.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * <p>
 * See ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/README
 * </p>
 * 
 * <pre>
 *                        ===========================================================================
 *                         gene_info
 *                         ---------------------------------------------------------------------------
 *                         This file can be considered as the logical equivalent of
 *                        
 *                         ftp://ftp.ncbi.nih.gov/refseq/LocusLink/LL.out
 *                        
 *                         tab-delimited
 *                         one line per GeneID
 *                         ---------------------------------------------------------------------------
 *                        
 *                         tax_id:
 *                         the unique identifier provided by NCBI Taxonomy
 *                         for the species or strain/isolate
 *                        
 *                         GeneID:
 *                         the unique identifier for a gene
 *                         ASN1:  geneid
 *                         --note:  for genomes previously available from LocusLink,
 *                         the identifiers are equivalent
 *                        
 *                         Symbol:
 *                         the default symbol for the gene
 *                         ASN1:  gene-&gt;locus
 *                        
 *                         LocusTag:
 *                         the LocusTag value
 *                         ASN1:  gene-&gt;locus-tag
 *                        
 *                         Synonyms:
 *                         bar-delimited set of unoffical symbols for the gene
 *                        
 *                         dbXrefs:
 *                         bar-delimited set of identifiers in other databases
 *                         for this gene.  The unit of the set is database:value.
 *                        
 *                         chromosome:
 *                         the chromosome on which this gene is placed
 *                        
 *                         map location:
 *                         the map location for this gene
 *                        
 *                         description
 *                         a descriptive name for this gene
 *                        
 *                         type of gene:
 *                         the type assigned to the gene according to the list of options
 *                         provided in http://www.ncbi.nlm.nih.gov/IEB/ToolBox/CPP_DOC/lxr/source/src/objects/entrezgene/entrezgene.asn
 *                        
 *                        
 *                         Symbol from nomenclature authority:
 *                         when not '-', indicates that this symbol is from a
 *                         a nomenclature authority
 *                        
 *                         Full name from nomenclature authority:
 *                         when not '-', indicates that this full name is from a
 *                         a nomenclature authority
 *                        
 *                         Nomenclature status
 *                         when not '-', indicates the status of the name from the 
 *                         nomenclature authority (O for official, I for interim)
 * </pre>
 * 
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 */
public class NCBIGeneInfo {

    private int taxId;
    private String geneId;
    private String defaultSymbol;
    private String locusTag;
    private Collection<String> synonyms = new HashSet<String>();
    private Map<String, String> dbXrefs = new HashMap<String, String>();
    private String chromosome;
    private String mapLocation;
    private String description;
    private GeneType geneType;
    private boolean symbolIsFromAuthority;
    private boolean nameIsFromAuthority;
    private NomenclatureStatus nomenclatureStatus;

    /**
     * See http://www.ncbi.nlm.nih.gov/IEB/ToolBox/CPP_DOC/lxr/source/src/objects/entrezgene/entrezgene.asn unknown (0) ,
     * 36
     */
    public enum GeneType {
        UNKNOWN, TRNA, RRNA, SNRNA, SCRNA, SNORNA, PROTEINCODING, PSEUDO, TRANSPOSON, MISCRNA, OTHER
    };

    public enum NomenclatureStatus {
        OFFICIAL, INTERIM, UNKNOWN
    };

    /**
     * Convert string to GeneType. See
     * http://www.ncbi.nlm.nih.gov/IEB/ToolBox/CPP_DOC/lxr/source/src/objects/entrezgene/entrezgene.asn
     * 
     * @param typeString
     * @return
     */
    public static GeneType typeStringToGeneType( String typeString ) {
        if ( typeString.equals( "unknown" ) ) {
            return GeneType.UNKNOWN;
        } else if ( typeString.equals( "tRNA" ) ) {
            return GeneType.TRNA;
        } else if ( typeString.equals( "rRNA" ) ) {
            return GeneType.RRNA;
        } else if ( typeString.equals( "snRNA" ) ) {
            return GeneType.SNRNA;
        } else if ( typeString.equals( "scRNA" ) ) {
            return GeneType.SCRNA;
        } else if ( typeString.equals( "snoRNA" ) ) {
            return GeneType.SNORNA;
        } else if ( typeString.equals( "protein-coding" ) ) {
            return GeneType.PROTEINCODING;
        } else if ( typeString.equals( "pseudo" ) ) {
            return GeneType.PSEUDO;
        } else if ( typeString.equals( "transposon" ) ) {
            return GeneType.TRANSPOSON;
        } else if ( typeString.equals( "miscRNA" ) ) {
            return GeneType.MISCRNA;
        } else if ( typeString.equals( "other" ) ) {
            return GeneType.OTHER;
        } else {
            throw new IllegalArgumentException( "Unknown gene type '" + typeString + "'" );
        }
    }

    /**
     * @return Returns the chromosome.
     */
    public String getChromosome() {
        return this.chromosome;
    }

    /**
     * @param chromosome The chromosome to set.
     */
    public void setChromosome( String chromosome ) {
        this.chromosome = chromosome;
    }

    /**
     * @return Returns the defaultSymbol.
     */
    public String getDefaultSymbol() {
        return this.defaultSymbol;
    }

    /**
     * @param defaultSymbol The defaultSymbol to set.
     */
    public void setDefaultSymbol( String defaultSymbol ) {
        this.defaultSymbol = defaultSymbol;
    }

    /**
     * @return Returns the description.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * @param description The description to set.
     */
    public void setDescription( String description ) {
        this.description = description;
    }

    /**
     * @return Returns the geneId.
     */
    public String getGeneId() {
        return this.geneId;
    }

    /**
     * @param geneId The geneId to set.
     */
    public void setGeneId( String geneId ) {
        this.geneId = geneId;
    }

    /**
     * @return Returns the geneType.
     */
    public GeneType getGeneType() {
        return this.geneType;
    }

    /**
     * @param geneType The geneType to set.
     */
    public void setGeneType( GeneType geneType ) {
        this.geneType = geneType;
    }

    /**
     * @return Returns the locusTag.
     */
    public String getLocusTag() {
        return this.locusTag;
    }

    /**
     * @param locusTag The locusTag to set.
     */
    public void setLocusTag( String locusTag ) {
        this.locusTag = locusTag;
    }

    /**
     * @return Returns the mapLocation.
     */
    public String getMapLocation() {
        return this.mapLocation;
    }

    /**
     * @param mapLocation The mapLocation to set.
     */
    public void setMapLocation( String mapLocation ) {
        this.mapLocation = mapLocation;
    }

    /**
     * @return Returns the nameIsFromAuthority.
     */
    public boolean isNameIsFromAuthority() {
        return this.nameIsFromAuthority;
    }

    /**
     * @param nameIsFromAuthority The nameIsFromAuthority to set.
     */
    public void setNameIsFromAuthority( boolean nameIsFromAuthority ) {
        this.nameIsFromAuthority = nameIsFromAuthority;
    }

    /**
     * @return Returns the nomenclatureStatus.
     */
    public NomenclatureStatus getNomenclatureStatus() {
        return this.nomenclatureStatus;
    }

    /**
     * @param nomenclatureStatus The nomenclatureStatus to set.
     */
    public void setNomenclatureStatus( NomenclatureStatus nomenclatureStatus ) {
        this.nomenclatureStatus = nomenclatureStatus;
    }

    /**
     * @return Returns the symbolIsFromAuthority.
     */
    public boolean isSymbolIsFromAuthority() {
        return this.symbolIsFromAuthority;
    }

    /**
     * @param symbolIsFromAuthority The symbolIsFromAuthority to set.
     */
    public void setSymbolIsFromAuthority( boolean symbolIsFromAuthority ) {
        this.symbolIsFromAuthority = symbolIsFromAuthority;
    }

    /**
     * @return Returns the taxId.
     */
    public int getTaxId() {
        return this.taxId;
    }

    /**
     * @param taxId The taxId to set.
     */
    public void setTaxId( int taxId ) {
        this.taxId = taxId;
    }

    /**
     * @return Returns the dbXrefs.
     */
    public Map<String, String> getDbXrefs() {
        return this.dbXrefs;
    }

    /**
     * @return Returns the synonyms.
     */
    public Collection<String> getSynonyms() {
        return this.synonyms;
    }

    /**
     * @param synonym
     */
    public void addToSynonyms( String synonym ) {
        this.synonyms.add( synonym );
    }

    /**
     * @param dbName
     * @param identifier
     */
    public void addToDbXRefs( String dbName, String identifier ) {
        this.dbXrefs.put( dbName, identifier );
    }

}