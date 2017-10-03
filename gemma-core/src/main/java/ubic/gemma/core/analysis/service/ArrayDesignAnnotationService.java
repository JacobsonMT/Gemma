/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.analysis.service;

import ubic.gemma.core.analysis.service.ArrayDesignAnnotationServiceImpl.OutputType;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.util.Settings;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

/**
 * Methods to generate annotations for array designs, based on information already in the database. This can be used to
 * generate annotation files used for ermineJ, for example. The file format:
 * <ul>
 * <li>The file is tab-delimited text. Comma-delimited files or Excel spreadsheets (for example) are not supported.</li>
 * <li>There is a one-line header included in the file for readability.</li>
 * <li>The first column contains the probe identifier</li>
 * <li>The second column contains a gene symbol(s). Clusters are delimited by '|' and genes within clusters are
 * delimited by ','</li>
 * <li>The third column contains the gene names (or description). Clusters are delimited by '|' and names within
 * clusters are delimited by '$'</li>
 * <li>The fourth column contains a delimited list of GO identifiers. These include the "GO:" prefix. Thus they read
 * "GO:00494494" and not "494494". Delimited by '|'.</li>
 * </ul>
 * <p>
 * Note that for backwards compatibility, GO terms are not segregated by gene cluster.
 * </p>
 *
 * @author paul
 */
public interface ArrayDesignAnnotationService {

    String ANNOTATION_FILE_SUFFIX = ".an.txt.gz";
    String BIO_PROCESS_FILE_SUFFIX = "_bioProcess";
    String NO_PARENTS_FILE_SUFFIX = "_noParents";
    /**
     * String included in file names for standard (default) annotation files. These include GO terms and all parents.
     */
    String STANDARD_FILE_SUFFIX = "";
    String ANNOT_DATA_DIR =
            Settings.getString( "gemma.appdata.home" ) + File.separatorChar + "microAnnots" + File.separatorChar;

    void deleteExistingFiles( ArrayDesign arrayDesign ) throws IOException;

    /**
     * Generate an annotation for a list of genes, instead of probes. The second column will contain the NCBI id, if
     * available.
     */
    int generateAnnotationFile( Writer writer, Collection<Gene> genes, OutputType type );

    /**
     * Format details:
     * There is a one-line header. The columns are:
     * <ol>
     * <li>Probe name
     * <li>Gene symbol. Genes located at different genome locations are delimited by "|"; multiple genes at the same
     * location are delimited by ",". Both can happen simultaneously.
     * <li>Gene name, delimited as for the symbol except '$' is used instead of ','.
     * <li>GO terms, delimited by '|'; multiple genes are not handled specially (for compatibility with ermineJ)
     * <li>Gemma's gene ids, delimited by '|'
     * <li>NCBI gene ids, delimited by '|'
     * </ol>
     *
     * @param genesWithSpecificity map of cs -&gt;* physical location -&gt;* ( blat association -&gt;* gene product -&gt; gene)
     * @param ty                   whether to include parents (OutputType.LONG); only use biological process (OutputType.BIOPROCESS) or
     *                             'standard' output (OutputType.SHORT).
     * @return number processed.
     */
    int generateAnnotationFile( Writer writer,
            Map<CompositeSequence, Collection<BioSequence2GeneProduct>> genesWithSpecificity, OutputType ty )
            throws IOException;

    /**
     * Opens a file for writing and adds the header.
     *
     * @param fileBaseName if Null, output will be written to standard output.
     * @param overWrite    clobber existing file. Otherwise returns null.
     * @return writer to use
     */
    Writer initOutputFile( ArrayDesign arrayDesign, String fileBaseName, boolean overWrite ) throws IOException;

}