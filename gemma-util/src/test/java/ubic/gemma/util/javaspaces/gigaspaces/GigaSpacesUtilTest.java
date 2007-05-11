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
package ubic.gemma.util.javaspaces.gigaspaces;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;

import ubic.gemma.javaspaces.gigaspaces.GemmaSpacesEnum;
import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.util.SpringContextUtil;

/**
 * A test class for {@link GigaSpacesUtil}.
 * 
 * @author keshav
 * @version $Id$
 */
public class GigaSpacesUtilTest extends BaseSpringContextTest {

    /**
     * Tests if space is running.
     */
    public void testIsSpaceRunning() {

        boolean isRunning = GigaSpacesUtil.isSpaceRunning( GemmaSpacesEnum.DEFAULT_SPACE.getSpaceUrl() );

        if ( isRunning )
            assertTrue( isRunning );

        else
            assertFalse( isRunning );

    }

    /**
     * Tests gigaspaces beans to the {@link org.springframework.beans.factory.BeanFactory}
     */
    public void testAddGigaspacesToBeanFactory() {

        ApplicationContext withoutGigaspacesCtx = ( ApplicationContext ) SpringContextUtil.getApplicationContext( true,
                false, false, false );

        String gigaspacesTemplate = "gigaspacesTemplate";
        assertFalse( withoutGigaspacesCtx.containsBean( gigaspacesTemplate ) );

        GigaSpacesUtil gigaspacesUtil = new GigaSpacesUtil();

        gigaspacesUtil.setApplicationContext( withoutGigaspacesCtx );

        BeanFactory updatedCtx = gigaspacesUtil.addGigaspacesToApplicationContext( GemmaSpacesEnum.DEFAULT_SPACE
                .getSpaceUrl() );

        /* verify that we have the new gigaspaces beans */
        if ( !GigaSpacesUtil.isSpaceRunning( GemmaSpacesEnum.DEFAULT_SPACE.getSpaceUrl() ) )
            assertFalse( updatedCtx.containsBean( gigaspacesTemplate ) );
        else {
            assertTrue( updatedCtx.containsBean( gigaspacesTemplate ) );
        }
        /* make sure we haven't lost the other beans */
        assertTrue( updatedCtx.containsBean( "sessionFactory" ) );
    }

    /**
     * Test logging space statistics.
     */
    public void testGetSpaceInformation() {
        GigaSpacesUtil.logSpaceStatistics( GemmaSpacesEnum.DEFAULT_SPACE.getSpaceUrl() );
    }

}
