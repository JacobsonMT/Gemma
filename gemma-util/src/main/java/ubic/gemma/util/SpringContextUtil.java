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
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import ubic.gemma.util.javaspaces.gigaspaces.GigaspacesUtil;

/**
 * Methods to create Spring contexts. For tests see ubic.gemma.testing.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class SpringContextUtil {
    private static Log log = LogFactory.getLog( SpringContextUtil.class.getName() );
    private static BeanFactory ctx = null;

    /**
     * @param testing If true, it will get a test configured-BeanFactory
     * @return BeanFactory
     */
    public static BeanFactory getApplicationContext( boolean testing, boolean compassOn, boolean gigaspacesOn,
            boolean isWebApp ) {
        if ( ctx == null ) {
            String[] paths = getConfigLocations( testing, compassOn, gigaspacesOn, isWebApp );
            ctx = new ClassPathXmlApplicationContext( paths );
            if ( ctx != null ) {
                log.info( "Got context" );
            } else {
                log.error( "Failed to load context" );
            }
        }
        return ctx;
    }

    /**
     * Find the configuration file locations. The files must be in your class path for this to work.
     * 
     * @return
     */
    public static String[] getConfigLocations() {
        return getConfigLocations( false, false, false, true );
    }

    /**
     * Find the configuration file locations. The files must be in your class path for this to work.
     * 
     * @param testing - if true, it will use the test configuration.
     * @return
     */
    public static String[] getConfigLocations( boolean testing, boolean compassOn, boolean gigaspacesOn,
            boolean isWebapp ) {
        if ( testing ) {
            return getTestConfigLocations( compassOn, isWebapp );
        }
        return getStandardConfigLocations( compassOn, gigaspacesOn, isWebapp );

    }

    /**
     * @return
     */
    private static String getGemmaHomeProperty() {
        String gemmaHome = ConfigUtils.getString( "gemma.home" );
        if ( gemmaHome == null ) {
            throw new RuntimeException( "You must set 'gemma.home' in your build.properties" );
        }
        return gemmaHome;
    }

    /**
     * @param isWebapp
     * @param paths
     */
    private static void addCommonConfig( boolean isWebapp, List<String> paths ) {
        paths.add( "classpath*:ubic/gemma/applicationContext-security.xml" );
        paths.add( "classpath*:ubic/gemma/applicationContext-hibernate.xml" );
        paths.add( "classpath*:ubic/gemma/applicationContext-serviceBeans.xml" );
        paths.add( "classpath*:ubic/gemma/applicationContext-persisterBeans.xml" );
        File f = new File( getGemmaHomeProperty() );
        try {
            if ( isWebapp ) {
                paths.add( "classpath*:ubic/gemma/applicationContext-validation.xml" );
                paths.add( f.toURL() + "gemma-web/target/Gemma/WEB-INF/" + "action-servlet.xml" );
            }
        } catch ( MalformedURLException e ) {
            throw new RuntimeException( "Could not form valid URL for " + f.getAbsolutePath(), e );
        }
    }

    /**
     * @param compassOn
     * @param isWebapp
     * @return
     */
    private static String[] getStandardConfigLocations( boolean compassOn, boolean gigaspacesOn, boolean isWebapp ) {
        List<String> paths = new ArrayList<String>();
        paths.add( "classpath*:ubic/gemma/localDataSource.xml" );

        if ( compassOn ) {
            CompassUtils.turnOnCompass( false, paths );
        }

        if ( gigaspacesOn ) {
            GigaspacesUtil.addGigaspacesContextToPaths( paths );
        }

        addCommonConfig( isWebapp, paths );
        return paths.toArray( new String[] {} );
    }

    /**
     * @param compassOn
     * @param isWebapp
     * @return
     */
    private static String[] getTestConfigLocations( boolean compassOn, boolean isWebapp ) {
        List<String> paths = new ArrayList<String>();
        paths.add( "classpath*:ubic/gemma/localTestDataSource.xml" );

        if ( compassOn ) {
            CompassUtils.turnOnCompass( true, paths );
        }
        addCommonConfig( isWebapp, paths );
        return paths.toArray( new String[] {} );
    }
}
