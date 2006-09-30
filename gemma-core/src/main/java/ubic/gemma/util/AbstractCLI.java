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
package ubic.gemma.util;

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
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Base Command Line Interface. Provides some default functionality.
 * <p>
 * To use this, in your concrete subclass, implement a main method. You must implement buildOptions and processOptions
 * to handle any application-specific options (they can be no-ops).
 * <p>
 * To facilitate testing of your subclass, your main method must call a non-static 'doWork' method, that will be exposed
 * for testing. In that method call processCommandline. You should return any non-null return value from
 * processCommandLine.
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class AbstractCLI {

    public enum ErrorCode {
        NORMAL, MISSING_OPTION, INVALID_OPTION, MISSING_ARGUMENT, FATAL_ERROR, AUTHENTICATION_ERROR
    }

    private static final char PASSWORD_CONSTANT = 'p';
    private static final char USERNAME_OPTION = 'u';
    private static final char PORT_OPTION = 'P';
    private static final char HOST_OPTION = 'H';

    private static final char VERBOSITY_OPTION = 'v';
    private static final String HEADER = "Options:";
    private static final String FOOTER = "The Gemma project, Copyright (c) 2006 University of British Columbia.";
    private static final int DEFAULT_PORT = 3306;
    private static int DEFAULT_VERBOSITY = 2;
    private Options options = new Options();

    private CommandLine commandLine;

    protected Log log = LogFactory.getLog( this.getClass() );

    /* support for convenience options */

    private String DEFAULT_HOST = "localhost";
    protected String host = DEFAULT_HOST;
    protected int port = DEFAULT_PORT;
    protected String username;
    protected String password;
    private int verbosity = DEFAULT_VERBOSITY; // corresponds to "Error".
    private Level originalLoggingLevel;

    public AbstractCLI() {
        this.buildStandardOptions();
        this.buildOptions();
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
     * Stop exeucting the CLI.
     */
    protected void bail( ErrorCode errorCode ) {
        // do something, but not System.exit.
        log.debug( "Bailing with error code " + errorCode );
        resetLogging();
        throw new IllegalStateException( errorCode.toString() );
    }

    /**
     * Implement this method to add options to your command line, using the OptionBuilder.
     * 
     * @see OptionBuilder
     */
    protected abstract void buildOptions();

    @SuppressWarnings("static-access")
    protected void buildStandardOptions() {
        log.debug( "Creating standard options" );
        Option helpOpt = new Option( "h", "help", false, "Print this message" );
        Option testOpt = new Option( "testing", false, "Use the test environment" );
        Option logOpt = new Option( "v", "verbosity", true,
                "Set verbosity level (0=silent, 5=very verbose; default is " + DEFAULT_VERBOSITY + ")" );

        options.addOption( logOpt );
        options.addOption( helpOpt );
        options.addOption( testOpt );
    }

    /**
     * Set up logging according to the user-selected (or default) verbosity level.
     */
    private void configureLogging() {

        // This only configures the base logger.

        String loggerName = "ubic.gemma";
        Logger log4jLogger = LogManager.exists( loggerName );

        if ( log4jLogger == null ) {
            log.warn( "No logger of name '" + loggerName + "'" );
            return;
        }

        this.originalLoggingLevel = log4jLogger.getLevel();

        switch ( verbosity ) {
            case 0:
                log4jLogger.setLevel( Level.OFF );
                break;
            case 1:
                log4jLogger.setLevel( Level.FATAL );
                break;
            case 2:
                log4jLogger.setLevel( Level.ERROR );
                break;
            case 3:
                log4jLogger.setLevel( Level.INFO );
                break;
            case 4:
                log4jLogger.setLevel( Level.DEBUG );
                break;
            case 5:
                log4jLogger.setLevel( Level.ALL );
                break;
            default:
                // Don't change the logging.
                break;
        }

        log.debug( "Logging level is at " + log4jLogger.getEffectiveLevel() );
    }

    /**
     * @param args
     * @return
     * @throws Exception
     */
    protected abstract Exception doWork( String[] args );

    public List getArgList() {
        return commandLine.getArgList();
    }

    public String[] getArgs() {
        return commandLine.getArgs();
    }

    protected final double getDoubleOptionValue( char option ) {
        try {
            return Double.parseDouble( commandLine.getOptionValue( option ) );
        } catch ( NumberFormatException e ) {
            System.out.println( invalidOptionString( "" + option ) + ", not a valid double" );
            bail( ErrorCode.INVALID_OPTION );
        }
        return 0.0;
    }

    protected final double getDoubleOptionValue( String option ) {
        try {
            return Double.parseDouble( commandLine.getOptionValue( option ) );
        } catch ( NumberFormatException e ) {
            System.out.println( invalidOptionString( option ) + ", not a valid double" );
            bail( ErrorCode.INVALID_OPTION );
        }
        return 0.0;
    }

    /**
     * @param c
     * @return
     */
    protected final String getFileNameOptionValue( char c ) {
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
    protected final String getFileNameOptionValue( String c ) {
        String fileName = commandLine.getOptionValue( c );
        File f = new File( fileName );
        if ( !f.canRead() ) {
            System.out.println( invalidOptionString( "" + c ) + ", cannot read from file" );
            bail( ErrorCode.INVALID_OPTION );
        }
        return fileName;
    }

    protected final int getIntegerOptionValue( char option ) {
        try {
            return Integer.parseInt( commandLine.getOptionValue( option ) );
        } catch ( NumberFormatException e ) {
            System.out.println( invalidOptionString( "" + option ) + ", not a valid integer" );
            bail( ErrorCode.INVALID_OPTION );
        }
        return 0;
    }

    protected final int getIntegerOptionValue( String option ) {
        try {
            return Integer.parseInt( commandLine.getOptionValue( option ) );
        } catch ( NumberFormatException e ) {
            System.out.println( invalidOptionString( option ) + ", not a valid integer" );
            bail( ErrorCode.INVALID_OPTION );
        }
        return 0;
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

    public Object getOptionObject( char opt ) {
        return commandLine.getOptionObject( opt );
    }

    public Object getOptionObject( String opt ) {
        return commandLine.getOptionObject( opt );
    }

    /**
     * @return
     * @see org.apache.commons.cli.Options#getOptions()
     */
    public final Collection getOptions() {
        return this.options.getOptions();
    }

    public String getOptionValue( char opt ) {
        return commandLine.getOptionValue( opt );
    }

    public String getOptionValue( char opt, String defaultValue ) {
        return commandLine.getOptionValue( opt, defaultValue );
    }

    public String getOptionValue( String opt ) {
        return commandLine.getOptionValue( opt );
    }

    public String getOptionValue( String opt, String defaultValue ) {
        return commandLine.getOptionValue( opt, defaultValue );
    }

    public String[] getOptionValues( char opt ) {
        return commandLine.getOptionValues( opt );
    }

    public String[] getOptionValues( String opt ) {
        return commandLine.getOptionValues( opt );
    }

    /**
     * @return
     * @see org.apache.commons.cli.Options#getRequiredOptions()
     */
    public final List getRequiredOptions() {
        return this.options.getRequiredOptions();
    }

    public boolean hasOption( char opt ) {
        return commandLine.hasOption( opt );
    }

    public boolean hasOption( String opt ) {
        return commandLine.hasOption( opt );
    }

    private String invalidOptionString( String option ) {
        return "Invalid value '" + commandLine.getOptionValue( option ) + " for option " + option;
    }

    /**
     * @param command The name of the command as used at the command line.
     */
    protected void printHelp( String command ) {
        HelpFormatter h = new HelpFormatter();
        h.printHelp( command + " [options]", HEADER, options, FOOTER );
    }

    /**
     * This must be called in your main method. It triggers parsing of the command line and processing of the options.
     * Check the error code to decide whether execution of your program should proceed.
     * 
     * @param args
     * @return Exception; null if nothing went wrong.
     * @throws ParseException
     */
    protected final Exception processCommandLine( String commandName, String[] args ) {
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
                System.out.println( "Unrecognized option: " + e.getMessage() );
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
     * Implement this to provide processing of options. It is called at the end of processCommandLine.
     */
    protected abstract void processOptions();

    /**
     * FIXME this causes subclasses to be unable to safely use 'h', 'p', 'u' and 'P' etc for their own purposes.
     */
    private void processStandardOptions() {

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

        if ( commandLine.hasOption( VERBOSITY_OPTION ) ) {
            this.verbosity = getIntegerOptionValue( VERBOSITY_OPTION );
            if ( verbosity < 1 || verbosity > 5 ) {
                throw new RuntimeException( "Verbosity must be from 1 to 5" );
            }
        }

        configureLogging();

    }

    /**
     * This is needed for CLIs that run in tests, so the logging settings get reset.
     */
    public void resetLogging() {
        String loggerName = "ubic.gemma";
        Logger log4jLogger = LogManager.exists( loggerName );

        if ( log4jLogger == null ) {
            log.warn( "No logger of name '" + loggerName + "'" );
            return;
        }

        log4jLogger.setLevel( this.originalLoggingLevel );
    }
}
