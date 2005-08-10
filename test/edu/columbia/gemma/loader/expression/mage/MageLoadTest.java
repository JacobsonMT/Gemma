/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.loader.expression.mage;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.BaseDAOTestCase;
import edu.columbia.gemma.common.auditAndSecurity.PersonDao;
import edu.columbia.gemma.common.description.ExternalDatabaseDao;
import edu.columbia.gemma.common.description.OntologyEntryDao;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignDao;
import edu.columbia.gemma.expression.biomaterial.BioMaterialDao;
import edu.columbia.gemma.expression.designElement.DesignElementDao;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentDao;
import edu.columbia.gemma.loader.expression.ExpressionLoaderImpl;
import edu.columbia.gemma.loader.expression.mage.MageMLParser;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class MageLoadTest extends BaseDAOTestCase {
    private static Log log = LogFactory.getLog( MageLoadTest.class.getName() );
    ExpressionLoaderImpl ml;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ml = new ExpressionLoaderImpl();
        ml.setBioMaterialDao( ( BioMaterialDao ) ctx.getBean( "bioMaterialDao" ) );
        ml.setExpressionExperimentDao( ( ExpressionExperimentDao ) ctx.getBean( "expressionExperimentDao" ) );
        ml.setPersonDao( ( PersonDao ) ctx.getBean( "personDao" ) );
        ml.setOntologyEntryDao( ( OntologyEntryDao ) ctx.getBean( "ontologyEntryDao" ) );
        ml.setArrayDesignDao( ( ArrayDesignDao ) ctx.getBean( "arrayDesignDao" ) );
        // ml.setTreatmentDao( ( TreatmentDao ) ctx.getBean( "treatmentDao" ) );
        ml.setExternalDatabaseDao( ( ExternalDatabaseDao ) ctx.getBean( "externalDatabaseDao" ) );
        ml.setDesignElementDao( ( DesignElementDao ) ctx.getBean( "designElementDao" ) );
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Class under test for void create(Collection)
     */
    
    public void testCreateCollection() throws Exception {
        log.debug( "Parsing MAGE Jamboree example" );
        ZipInputStream istMageExamples = new ZipInputStream( MageMLParserTest.class
                .getResourceAsStream( "/data/mage/mageml-example.zip" ) );
        istMageExamples.getNextEntry();
        ZipInputStream istMageXml = new ZipInputStream( MageMLParserTest.class
                .getResourceAsStream( "/data/mage/mageml-example.zip" ) );
        InputStream istXsl = MageMLParserTest.class.getResourceAsStream("/data/mage/simplify.xsl");
        MageMLParser mlp = new MageMLParser();
        mlp.createSimplifiedXml(istMageXml, istXsl);
        mlp.parse( istMageExamples );
        
        Collection result = mlp.getConvertedData();
        log.info( result.size() + " Objects parsed from the MAGE file." );
        log.info( "Tally:\n" + mlp );
        istMageExamples.close();
        ml.create( result );
        
    }

    /**
     * A real example of an experimental package.
     * 
     * @throws Exception
     */
    public void testCreateCollectionRealA() throws Exception {
        log.debug( "Parsing MAGE from ArrayExpress (AFMX)" );
        InputStream istMageExamples = MageMLParserTest.class.getResourceAsStream( "/data/mage/E-AFMX-13.xml" );
        InputStream istMageXml = MageMLParserTest.class.getResourceAsStream( "/data/mage/E-AFMX-13.xml" );
        InputStream istXsl = MageMLParserTest.class.getResourceAsStream("/data/mage/simplify.xsl");
        
        MageMLParser mlp = new MageMLParser();
        mlp.createSimplifiedXml(istMageXml, istXsl);
        mlp.parse( istMageExamples );
        Collection result = mlp.getConvertedData();
        log.info( result.size() + " Objects parsed from the MAGE file." );
        log.info( "Tally:\n" + mlp );
        istMageExamples.close();
        ml.create( result );
    }

    /**
     * A real example of an experimental package.
     * 
     * @throws Exception
     */
    
    public void testCreateCollectionRealB() throws Exception {
        log.debug( "Parsing MAGE from ArrayExpress (WMIT)" );
        InputStream istMageExamples = MageMLParserTest.class.getResourceAsStream( "/data/mage/E-WMIT-4.xml" );
        InputStream istMageXml = MageMLParserTest.class.getResourceAsStream( "/data/mage/E-WMIT-4.xml" );
        InputStream istXsl = MageMLParserTest.class.getResourceAsStream("/data/mage/simplify.xsl");
        
        MageMLParser mlp = new MageMLParser();
        mlp.createSimplifiedXml(istMageXml, istXsl);
        mlp.parse( istMageExamples );
        Collection result = mlp.getConvertedData();
        log.info( result.size() + " Objects parsed from the MAGE file." );
        log.info( "Tally:\n" + mlp );
        istMageExamples.close();
        ml.create( result );
    }

}
