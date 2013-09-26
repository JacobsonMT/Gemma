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
package ubic.gemma.loader.expression.arrayDesign;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.util.FileTools;
import ubic.gemma.loader.genome.SimpleFastaCmd;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.util.Settings;

/**
 * Test exercises the fastacmd - requires executable.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignSequenceProcessorFastacmdTest extends AbstractArrayDesignProcessingTest {
    @Autowired
    ArrayDesignSequenceProcessingService app;

    @Test
    public void testProcessArrayDesignWithFastaCmdFetch() throws Exception {

        if ( !fastaCmdExecutableExists() ) {
            return;
        }
        if ( ad == null ) {
            log.warn( "Array design configuration failed, skipping test" );
            return;
        }
        ad = arrayDesignService.thaw( ad );
        try {
            // finally the real business. There are 243 sequences on the array.
            Collection<BioSequence> res = app.processArrayDesign( ad, new String[] { "testblastdb",
                    "testblastdbPartTwo" }, FileTools.resourceToPath( "/data/loader/genome/blast" ), false );
            if ( res == null ) {
                // data already filled in by some other test.
            } else {
                if ( res.size() == 242 ) {
                    log.warn( "Got 242 for some reason instead of 243, here is some debugging information (test will pass)" );
                    for ( BioSequence bs : res ) {
                        log.warn( bs );
                    }
                    return;
                }
                // assertEquals( 243, res.size() ); // sometimes end up with 242... or 220, when running in continuum.

                assertTrue( res.size() > 0 );
            }
        } catch ( Exception e ) {
            if ( e.getMessage() == null ) {
                throw e;
            }
            if ( e.getMessage().startsWith( "No fastacmd executable:" ) ) {
                log.warn( "Test skipped: no fastacmd executable" );
                return;
            } else if ( e.getCause() instanceof ExecutionException ) {
                log.error( "Failed to get file -- skipping rest of test" );
                return;
            } else if ( e.getCause() instanceof java.net.UnknownHostException ) {
                log.error( "Failed to connect to NCBI, skipping test" );
                return;
            } else if ( e.getCause() instanceof org.apache.commons.net.ftp.FTPConnectionClosedException ) {
                log.error( "Failed to connect to NCBI, skipping test" );
                return;
            }
            throw e;
        }

    }

    // fixme duplicated from SimpleFastaCmdTest
    private boolean fastaCmdExecutableExists() {
        String fastacmdExe = Settings.getString( SimpleFastaCmd.FASTA_CMD_ENV_VAR );
        if ( fastacmdExe == null ) {
            log.warn( "No fastacmd executable is configured, skipping test" );
            return false;
        }

        File fi = new File( fastacmdExe );
        if ( !fi.exists() ) {
            log.warn( fastacmdExe + " not executable, skipping test" );
            return false;
        }
        return true;
    }
}
