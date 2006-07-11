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
package ubic.gemma.loader.util;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.apache.commons.cli.AlreadySelectedException;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author pavlidis
 * @version $Id$
 */
public abstract class AbstractCLI {

    private static final char PASSWORD_CONSTANT = 'p';
    private static final char USERNAME_OPTION = 'u';
    private static final char PORT_OPTION = 'P';
    private static final char HOST_OPTION = 'H';
    private static final String HEADER = "Options:";
    private static final String FOOTER = "The Gemma project, Copyright (c) 2006 University of British Columbia.";
    private Options options = new Options();
    private CommandLine commandLine;
    protected static final Log log = LogFactory.getLog( AbstractSpringAwareCLI.class );
    private static final int DEFAULT_PORT = 3306;

    private String DEFAULT_HOST = "localhost";

    public enum ErrorCode {
        NORMAL, MISSING_OPTION, INVALID_OPTION, MISSING_ARGUMENT, FATAL_ERROR, AUTHENITCATION_ERROR
    };

    /* support for convenience options */

    protected String host = DEFAULT_HOST;
    protected int port = DEFAULT_PORT;
    protected String username;
    protected String password;

    public AbstractCLI() {
        this.buildStandardOptions();
        this.buildOptions();
    }

    @SuppressWarnings("static-access")
    protected void buildStandardOptions() {
        log.debug( "Creating standard options" );
        Option helpOpt = new Option( "h", "help", false, "Print this message" );
        Option testOpt = new Option( "testing", false, "Use the test environment" );

        options.addOption( helpOpt );
        options.addOption( testOpt );
    }

    protected abstract void buildOptions();

    protected abstract Exception doWork( String[] args ) throws Exception;

    /**
     * This must be called in your main method. It triggers parsing of the command line and processing of the options.
     * Check the error code to decide whether execution of your program should proceed.
     * 
     * @param args
     * @return Exception; null if nothing went wrong.
     * @throws ParseException
     */
    protected final Exception processCommandLine( String commandName, String[] args ) throws Exception {
        /* COMMAND LINE PARSER STAGE */
        BasicParser parser = new BasicParser();

        if ( args == null ) {
            printHelp( commandName );
            return new Exception( "No arguments" );
        }

        try {
            commandLine = parser.parse( options, args );
        } catch ( ParseException e ) {
            if ( e instanceof MissingOptionException ) {
                System.out.println( "Required option(s) were not supplied: " + e.getMessage() );

            } else if ( e instanceof AlreadySelectedException ) {
                System.out.println( "The option(s) " + e.getMessage() + " were already selected" );
            } else if ( e instanceof MissingArgumentException ) {
                System.out.println( "Missing argument: " + e.getMessage() );
            } else if ( e instanceof UnrecognizedOptionException ) {
                System.out.println( e.getMessage() );
            } else {
                e.printStackTrace();
            }

            printHelp( commandName );

            if ( log.isDebugEnabled() ) {
                log.debug( e );
            }

            return e;
        }

        /* INTERROGATION STAGE */
        if ( commandLine.hasOption( 'h' ) ) {
            printHelp( commandName );
            return new Exception( "Asked for help" );
        }

        processStandardOptions();
        processOptions();

        return null;

    }

    /**
     * Stop exeucting the CLI.
     */
    protected void bail( ErrorCode errorCode ) throws Exception {
        throw new Exception( errorCode.toString() );
    }

    /**
     * FIXME this causes subclasses to be unable to safely use 'h', 'p', 'u' and 'P' for their own purposes.
     */
    private void processStandardOptions() throws Exception {

        if ( commandLine.hasOption( HOST_OPTION ) ) {
            this.host = commandLine.getOptionValue( HOST_OPTION );
        } else {
            this.host = DEFAULT_HOST;
        }

        if ( commandLine.hasOption( PORT_OPTION ) ) {
            this.port = getIntegerOptionValue( PORT_OPTION );
        } else {
            this.port = DEFAULT_PORT;
        }

        if ( commandLine.hasOption( USERNAME_OPTION ) ) {
            this.username = commandLine.getOptionValue( USERNAME_OPTION );
        }

        if ( commandLine.hasOption( PASSWORD_CONSTANT ) ) {
            this.password = commandLine.getOptionValue( PASSWORD_CONSTANT );
        }

    }

    /**
     * Implement this to provide processing of options. It is called at the end of processCommandLine.
     */
    protected abstract void processOptions() throws Exception;

    /**
     * @param command The name of the command as used at the command line.
     */
    protected void printHelp( String command ) {
        HelpFormatter h = new HelpFormatter();
        h.printHelp( command + " [options]", HEADER, options, FOOTER );
    }

    public List getArgList() {
        return commandLine.getArgList();
    }

    public String[] getArgs() {
        return commandLine.getArgs();
    }

    public boolean hasOption( char opt ) {
        return commandLine.hasOption( opt );
    }

    public boolean hasOption( String opt ) {
        return commandLine.hasOption( opt );
    }

    public Object getOptionObject( char opt ) {
        return commandLine.getOptionObject( opt );
    }

    public Object getOptionObject( String opt ) {
        return commandLine.getOptionObject( opt );
    }

    public String getOptionValue( char opt, String defaultValue ) {
        return commandLine.getOptionValue( opt, defaultValue );
    }

    public String getOptionValue( char opt ) {
        return commandLine.getOptionValue( opt );
    }

    public String getOptionValue( String opt, String defaultValue ) {
        return commandLine.getOptionValue( opt, defaultValue );
    }

    public String getOptionValue( String opt ) {
        return commandLine.getOptionValue( opt );
    }

    public String[] getOptionValues( char opt ) {
        return commandLine.getOptionValues( opt );
    }

    public String[] getOptionValues( String opt ) {
        return commandLine.getOptionValues( opt );
    }

    /**
     * Convenience method to add a standard pair of (required) options to intake a user name and password.
     */
    @SuppressWarnings("static-access")
    protected void addUserNameAndPasswordOptions() {
        Option usernameOpt = OptionBuilder.withArgName( "user" ).isRequired().withLongOpt( "user" ).hasArg()
                .withDescription( "User name for accessing the system" ).create( USERNAME_OPTION );

        Option passwordOpt = OptionBuilder.withArgName( "passwd" ).isRequired().withLongOpt( "password" ).hasArg()
                .withDescription( "Password for accessing the system" ).create( PASSWORD_CONSTANT );
        options.addOption( usernameOpt );
        options.addOption( passwordOpt );
    }

    /**
     * Convenience method to add a standard pair of options to intake a host name and port number. *
     * 
     * @param hostRequired Whether the host name is required
     * @param portRequired Whether the port is required
     */
    @SuppressWarnings("static-access")
    protected void addHostAndPortOptions( boolean hostRequired, boolean portRequired ) {
        Option hostOpt = OptionBuilder.withArgName( "host" ).withLongOpt( "host" ).hasArg().withDescription(
                "Hostname to use (Default = " + DEFAULT_HOST + ")" ).create( HOST_OPTION );

        hostOpt.setRequired( hostRequired );

        Option portOpt = OptionBuilder.withArgName( "port" ).withLongOpt( "port" ).hasArg().withDescription(
                "Port to use on host (Default = " + DEFAULT_PORT + ")" ).create( PORT_OPTION );

        portOpt.setRequired( portRequired );

        options.addOption( hostOpt );
        options.addOption( portOpt );
    }

    protected final double getDoubleOptionValue( String option ) throws Exception {
        try {
            return Double.parseDouble( commandLine.getOptionValue( option ) );
        } catch ( NumberFormatException e ) {
            System.out.println( invalidOptionString( option ) + ", not a valid double" );
            bail( ErrorCode.INVALID_OPTION );
        }
        return 0.0;
    }

    protected final double getDoubleOptionValue( char option ) throws Exception {
        try {
            return Double.parseDouble( commandLine.getOptionValue( option ) );
        } catch ( NumberFormatException e ) {
            System.out.println( invalidOptionString( "" + option ) + ", not a valid double" );
            bail( ErrorCode.INVALID_OPTION );
        }
        return 0.0;
    }

    protected final int getIntegerOptionValue( String option ) throws Exception {
        try {
            return Integer.parseInt( commandLine.getOptionValue( option ) );
        } catch ( NumberFormatException e ) {
            System.out.println( invalidOptionString( option ) + ", not a valid integer" );
            bail( ErrorCode.INVALID_OPTION );
        }
        return 0;
    }

    private String invalidOptionString( String option ) throws Exception {
        return "Invalid value '" + commandLine.getOptionValue( option ) + " for option " + option;
    }

    protected final int getIntegerOptionValue( char option ) throws Exception {
        try {
            return Integer.parseInt( commandLine.getOptionValue( option ) );
        } catch ( NumberFormatException e ) {
            System.out.println( invalidOptionString( "" + option ) + ", not a valid integer" );
            bail( ErrorCode.INVALID_OPTION );
        }
        return 0;
    }

    /**
     * @param c
     * @return
     */
    protected final String getFileNameOptionValue( char c ) throws Exception {
        String fileName = commandLine.getOptionValue( c );
        File f = new File( fileName );
        if ( !f.canRead() ) {
            System.out.println( invalidOptionString( "" + c ) + ", cannot read from file" );
            bail( ErrorCode.INVALID_OPTION );
        }
        return fileName;
    }

    /**
     * @param c
     * @return
     */
    protected final String getFileNameOptionValue( String c ) throws Exception {
        String fileName = commandLine.getOptionValue( c );
        File f = new File( fileName );
        if ( !f.canRead() ) {
            System.out.println( invalidOptionString( "" + c ) + ", cannot read from file" );
            bail( ErrorCode.INVALID_OPTION );
        }
        return fileName;
    }

    /**
     * @param opt
     * @return
     * @see org.apache.commons.cli.Options#addOption(org.apache.commons.cli.Option)
     */
    public final Options addOption( Option opt ) {
        return this.options.addOption( opt );
    }

    /**
     * @param opt
     * @param hasArg
     * @param description
     * @return
     * @see org.apache.commons.cli.Options#addOption(java.lang.String, boolean, java.lang.String)
     */
    public final Options addOption( String opt, boolean hasArg, String description ) {
        return this.options.addOption( opt, hasArg, description );
    }

    /**
     * @param opt
     * @param longOpt
     * @param hasArg
     * @param description
     * @return
     * @see org.apache.commons.cli.Options#addOption(java.lang.String, java.lang.String, boolean, java.lang.String)
     */
    public final Options addOption( String opt, String longOpt, boolean hasArg, String description ) {
        return this.options.addOption( opt, longOpt, hasArg, description );
    }

    /**
     * @param group
     * @return
     * @see org.apache.commons.cli.Options#addOptionGroup(org.apache.commons.cli.OptionGroup)
     */
    public final Options addOptionGroup( OptionGroup group ) {
        return this.options.addOptionGroup( group );
    }

    /**
     * @param opt
     * @return
     * @see org.apache.commons.cli.Options#getOption(java.lang.String)
     */
    public final Option getOption( String opt ) {
        return this.options.getOption( opt );
    }

    /**
     * @param opt
     * @return
     * @see org.apache.commons.cli.Options#getOptionGroup(org.apache.commons.cli.Option)
     */
    public final OptionGroup getOptionGroup( Option opt ) {
        return this.options.getOptionGroup( opt );
    }

    /**
     * @return
     * @see org.apache.commons.cli.Options#getOptions()
     */
    public final Collection getOptions() {
        return this.options.getOptions();
    }

    /**
     * @return
     * @see org.apache.commons.cli.Options#getRequiredOptions()
     */
    public final List getRequiredOptions() {
        return this.options.getRequiredOptions();
    }
}
