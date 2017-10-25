/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.core.apps;

import java.io.File;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.loader.entrez.pubmed.PubMedService;
import ubic.gemma.core.util.AbstractCLIContextCLI;

/**
 * Load PubMed files from XML files -- not used routinely!
 * 
 * @author pavlidis
 *
 */
public class PubMedLoaderCli extends AbstractCLIContextCLI {

    /**
     * @param args
     */
    public static void main( String[] args ) {
        PubMedLoaderCli p = new PubMedLoaderCli();
        Exception exception = p.doWork( args );
        if ( exception != null ) {
            log.error( exception, exception );
        }
    }

    private String directory;
    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.MISC;
    }
    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.util.AbstractCLI#getCommandName()
     */
    @Override
    public String getCommandName() {
        return "pubmedLoad";
    }

    @Override
    public String getShortDesc() {
        return "Loads PubMed records into the database from XML files";
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option fileOption = OptionBuilder.isRequired().hasArg().withArgName( "Directory" )
                .withDescription( "Directory of PubMed XML files to load" ).withLongOpt( "dir" ).create( 'd' );
        this.addOption( fileOption );

    }

    @Override
    protected Exception doWork( String[] args ) {
        processCommandLine( args );
        PubMedService pms = this.getBean( PubMedService.class );
        pms.loadFromDirectory( new File( directory ) );
        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 'd' ) ) {
            this.directory = this.getOptionValue( 'd' );
        }
    }

}
