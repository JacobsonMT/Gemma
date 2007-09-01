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
package ubic.gemma.externalDb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.ConfigUtils;

/**
 * Perform useful queries against GoldenPath (UCSC) databases.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GoldenPath {

    protected static final Log log = LogFactory.getLog( GoldenPath.class );

    private ExternalDatabase searchedDatabase;

    private ExternalDatabaseService externalDatabaseService;

    protected DriverManagerDataSource dataSource;

    protected JdbcTemplate jt;

    protected Connection conn;

    protected QueryRunner qr;

    private String databaseName = null;

    private Taxon taxon;

    public Taxon getTaxon() {
        return taxon;
    }

    /**
     * Get golden path for the default database (human);
     */
    public GoldenPath() throws SQLException {
        this.taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( "human" );
        init();
    }

    /**
     * @param databaseName
     * @param host
     * @param user
     * @param password
     * @throws SQLException
     */
    public GoldenPath( int port, String databaseName, String host, String user, String password ) throws SQLException {
        this.databaseName = databaseName;

        getTaxonForDbName( databaseName );

        init( port, host, user, password );
    }

    private void getTaxonForDbName( String databaseName ) {
        // This is a little dumb
        this.taxon = Taxon.Factory.newInstance();
        if ( databaseName.startsWith( "hg" ) ) {
            taxon.setCommonName( "human" );
        } else if ( databaseName.startsWith( "mm" ) ) {
            taxon.setCommonName( "mouse" );
        } else if ( databaseName.startsWith( "rn" ) ) {
            taxon.setCommonName( "rat" );
        } else {
            throw new IllegalArgumentException( "Cannot infer taxon for " + databaseName );
        }
    }

    /**
     * Get a GoldenPath instance for a given taxon, using configured database settings.
     * 
     * @param taxon
     */
    public GoldenPath( Taxon taxon ) throws SQLException {
        this.taxon = taxon;
        init();
    }

    /**
     * @param databaseName hg18, rn4 etc.
     * @throws SQLException
     */
    public GoldenPath( String databaseName ) throws SQLException {
        getTaxonForDbName( databaseName );
        init();
    }

    /**
     * @return
     */
    public String getDatabaseName() {
        return databaseName;
    }

    protected void init( int port, String host, String user, String password ) throws SQLException {
        assert databaseName != null;
        dataSource = new DriverManagerDataSource();
        jt = new JdbcTemplate( dataSource );

        String url = "jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?relaxAutoCommit=true";
        log.info( "Connecting to " + databaseName );
        log.debug( "Connecting to Golden Path : " + url + " as " + user );

        dataSource.setDriverClassName( ConfigUtils.getString( "gemma.goldenpath.db.driver" ) );
        dataSource.setUrl( url );
        dataSource.setUsername( user );
        dataSource.setPassword( password );

        jt.setFetchSize( 50 );
        jt.setDataSource( dataSource );

        try {
            Class.forName( dataSource.getDriverClassName() ).newInstance();
            conn = DriverManager.getConnection( url, user, password );
        } catch ( InstantiationException e ) {
            throw new RuntimeException( e );
        } catch ( IllegalAccessException e ) {
            throw new RuntimeException( e );
        } catch ( ClassNotFoundException e ) {
            throw new RuntimeException( e );
        }

        qr = new QueryRunner();
    }

    private void init() throws SQLException {
        String commonName = taxon.getCommonName();
        if ( commonName.equals( "mouse" ) ) {
            databaseName = ConfigUtils.getString( "gemma.goldenpath.db.mouse" ); // FIXME get these names from an external source - e.g., the taxon service.
        } else if ( commonName.equals( "human" ) ) {
            databaseName = ConfigUtils.getString( "gemma.goldenpath.db.human" );
        } else if ( commonName.equals( "rat" ) ) {
            databaseName = ConfigUtils.getString( "gemma.goldenpath.db.rat" );
        } else {
            throw new IllegalArgumentException( "No GoldenPath database for  " + taxon );
        }

        String databaseHost = ConfigUtils.getString( "gemma.goldenpath.db.host" );
        int databasePort = Integer.valueOf( ConfigUtils.getString( "gemma.goldenpath.db.port" ) );
        String databaseUser = ConfigUtils.getString( "gemma.goldenpath.db.user" );
        String databasePassword = ConfigUtils.getString( "gemma.goldenpath.db.password" );
        
        searchedDatabase = ExternalDatabase.Factory.newInstance();
        searchedDatabase.setName( databaseName );
        searchedDatabase.setType( DatabaseType.SEQUENCE );

        this.init( databasePort, databaseHost, databaseUser, databasePassword );
    }

    /**
     * @param externalDatabaseService
     */
    public void setExternalDatabaseService( ExternalDatabaseService externalDatabaseService ) {
        this.externalDatabaseService = externalDatabaseService;
    }

    public ExternalDatabaseService getExternalDatabaseService() {
        return externalDatabaseService;
    }

    public ExternalDatabase getSearchedDatabase() {
        return searchedDatabase;
    }

}