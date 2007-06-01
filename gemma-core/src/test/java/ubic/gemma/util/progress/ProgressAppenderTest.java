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
package ubic.gemma.util.progress;

import java.util.Enumeration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import ubic.gemma.testing.BaseSpringContextTest;

/**
 * For this test to work you should have the appender configured in log4j.properties. If not it will be set up
 * programatically.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ProgressAppenderTest extends BaseSpringContextTest {

    ProgressJob job;

    // used to put things back as they were after the test.
    Level oldLevel;

    Logger log4jLogger;

    // important for this test!
    private static Log log = LogFactory.getLog( ProgressAppenderTest.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.testing.BaseSpringContextTest#onSetUpInTransaction()
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        String loggerName = "ubic.gemma";
        log4jLogger = LogManager.exists( loggerName );

        Enumeration<Appender> appenders = log4jLogger.getAllAppenders();

        Appender progressAppender = null;
        for ( ; appenders.hasMoreElements(); ) {
            Appender appender = appenders.nextElement();
            if ( appender instanceof ProgressAppender ) {
                progressAppender = appender;
            }
        }

        if ( progressAppender == null ) {
            log.warn( "There is no progress appender configured; adding one for test" );
            log4jLogger.addAppender( new ProgressAppender() );
        }

        oldLevel = log4jLogger.getLevel();

        log4jLogger.setLevel( Level.INFO );

        job = ProgressManager.createProgressJob( null, "test", "testing" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.testing.BaseSpringContextTest#onTearDownInTransaction()
     */
    @Override
    protected void onTearDownInTransaction() throws Exception {
        super.onTearDownInTransaction();
        ProgressManager.destroyProgressJob( job );
        log4jLogger.setLevel( oldLevel );
    }

    public void testProgressLogging() throws Exception {

        String expectedValue = "la de da";
        log.info( expectedValue );

        //assertEquals( expectedValue, job.getProgressData().getDescription() );

        log.debug( "pay no attention" ); // should not update the description.
   //     assertEquals( expectedValue, job.getProgressData().getDescription() );

        log.warn( "listenToMe" );
    //    assertEquals( "listenToMe", job.getProgressData().getDescription() );
    }

}
