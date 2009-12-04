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
package ubic.gemma.model.common.auditAndSecurity;

import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class AuditEventDaoImplTest extends BaseSpringContextTest {

    @Before
    public void setup() throws Exception {

        ArrayDesignService ads = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
        for ( int i = 0; i < 5; i++ ) {
            ArrayDesign ad = ArrayDesign.Factory.newInstance();
            ad.setName( "ffoo " + i );
            ad.setPrimaryTaxon( this.getTaxon( "mouse" ) );

            ad = ads.create( ad );

            ad.setDescription( "arrrgh" );
            ads.update( ad );
        }
    }

    @Test
    public void testHandleGetNewSinceDate() {
        AuditEventDao aed = ( AuditEventDao ) this.getBean( "auditEventDao" );
        Calendar c = Calendar.getInstance();
        c.set( 2006, Calendar.DECEMBER, 1 );
        Date d = c.getTime();
        Collection<Auditable> objs = aed.getNewSinceDate( d );
        assertTrue( objs.size() > 0 );
        // for ( Auditable auditable : objs ) {
        // if ( objs instanceof ArrayDesign ) {
        // }
        // }
    }

    @Test
    public void testHandleGetUpdatedSinceDate() {
        AuditEventDao aed = ( AuditEventDao ) this.getBean( "auditEventDao" );
        Calendar c = Calendar.getInstance();
        c.set( 2006, Calendar.DECEMBER, 1 );
        Date d = c.getTime();
        Collection<Auditable> objs = aed.getUpdatedSinceDate( d );
        assertTrue( objs.size() > 0 );
        // for ( Auditable auditable : objs ) {
        // if ( objs instanceof ArrayDesign ) {
        // }
        // }
    }

}
