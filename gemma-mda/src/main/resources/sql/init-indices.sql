-- Add some indices that are not included in the generated gemma-ddl.sql. Some of these are very important to performance
-- $Id$
alter table BIO_SEQUENCE add index name (NAME);
alter table ALTERNATE_NAME add index name (NAME);
alter table INVESTIGATION add index name (NAME);
alter table INVESTIGATION add INDEX shortname (SHORT_NAME);
alter table DATABASE_ENTRY add index acc_ex (ACCESSION, EXTERNAL_DATABASE_FK);
alter table CHROMOSOME_FEATURE add index symbol_tax (OFFICIAL_SYMBOL, TAXON_FK);
alter table CHROMOSOME_FEATURE add index ncbigeneid (NCBI_GENE_ID);
alter table CHROMOSOME_FEATURE add index ncbigi (NCBI_GI);
alter table CHROMOSOME_FEATURE add index previous_ncbiid (PREVIOUS_NCBI_ID);
alter table CHROMOSOME_FEATURE add index ensemblid (ENSEMBL_ID);
alter table CHROMOSOME_FEATURE add index name (NAME);
alter table CHROMOSOME_FEATURE add index class (class);
alter table CHROMOSOME_FEATURE add index type (TYPE);
alter table GENE_ALIAS add index `alias` (`ALIAS`);
alter table COMPOSITE_SEQUENCE add index name (NAME);
alter table PHYSICAL_LOCATION ADD INDEX BIN_KEY (BIN);
alter table AUDIT_EVENT_TYPE ADD INDEX class (class);
alter table ANALYSIS ADD INDEX class (class);
alter table CHARACTERISTIC ADD INDEX value (VALUE);
alter table CHARACTERISTIC ADD INDEX category (CATEGORY);
alter table CHARACTERISTIC ADD INDEX valueUri (VALUE_URI);
alter table CHARACTERISTIC ADD INDEX categoryUri (CATEGORY_URI);
alter table GENE_SET ADD INDEX name (NAME);
alter table PROCESSED_EXPRESSION_DATA_VECTOR ADD INDEX experimentProcessedVectorProbes (EXPRESSION_EXPERIMENT_FK,DESIGN_ELEMENT_FK);
alter table DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT ADD INDEX resultSetProbes (RESULT_SET_FK,PROBE_FK);
alter table TAXON ADD INDEX taxonncbiid (NCBI_ID);
alter table TAXON ADD INDEX taxonsecondncbiid (SECONDARY_NCBI_ID);
alter table TAXON ADD INDEX taxoncommonname (COMMON_NAME);
alter table TAXON ADD INDEX taxonscientificname (SCIENTIFIC_NAME);
alter table LOCAL_FILE ADD INDEX REMOTE_URL (REMOTE_U_R_L);
alter table CONTACT add INDEX fullname (NAME, LAST_NAME);

-- candidates for removal
alter table DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT ADD INDEX corrpvalbin (CORRECTED_P_VALUE_BIN);
alter table HIT_LIST_SIZE ADD INDEX direction (DIRECTION);
