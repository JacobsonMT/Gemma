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
package ubic.gemma.security;

import java.util.Collection;
import java.util.HashSet;

import org.acegisecurity.acl.basic.BasicAclExtendedDao;

import ubic.gemma.model.common.SecurableDao;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Tests the SecurityService.
 * 
 * @author keshav
 * @version $Id$
 */
public class SecurityServiceTest extends BaseSpringContextTest {

    private ArrayDesignService arrayDesignService;

    ArrayDesign arrayDesign;
    String arrayDesignName = "Array Design Foo";
    String compositeSequenceName1 = "Design Element Bar1";
    String compositeSequenceName2 = "Design Element Bar2";

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.BaseDependencyInjectionSpringContextTest#onSetUpInTransaction()
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {
        /*
         * Note: You will not see the acl_permission and acl_object_identity in the database unless you add the method
         * invocaction to setComplete() at the end of this onSetUpInTransaction.
         */
        log.info( "Turn up the logging levels to DEBUG on the acegi and gemma security packages" );

        super.onSetUpInTransaction(); // admin

        arrayDesign = ArrayDesign.Factory.newInstance();
        arrayDesign.setName( arrayDesignName );
        arrayDesign.setDescription( "A test ArrayDesign from " + this.getClass().getName() );

        CompositeSequence cs1 = CompositeSequence.Factory.newInstance();
        cs1.setName( compositeSequenceName1 );

        CompositeSequence cs2 = CompositeSequence.Factory.newInstance();
        cs2.setName( compositeSequenceName2 );

        Collection<CompositeSequence> col = new HashSet<CompositeSequence>();
        col.add( cs1 );
        col.add( cs2 );

        /*
         * Note this sequence. Remember, inverse="true" if using this. If you do not make an explicit call to
         * cs1(2).setArrayDesign(arrayDesign), then inverse="false" must be set.
         */
        cs1.setArrayDesign( arrayDesign );
        cs2.setArrayDesign( arrayDesign );
        arrayDesign.setCompositeSequences( col );

        arrayDesignService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
        arrayDesign = arrayDesignService.findOrCreate( arrayDesign );
    }

    /**
     * Tests changing object level security on the ArrayDesign from public to private.
     * 
     * @throws Exception
     */
    public void testMakePrivate() throws Exception {
        ArrayDesign ad = arrayDesignService.findArrayDesignByName( arrayDesignName );
        SecurityService securityService = new SecurityService();

        securityService.setBasicAclExtendedDao( ( BasicAclExtendedDao ) this.getBean( "basicAclExtendedDao" ) );
        securityService.setSecurableDao( ( SecurableDao ) this.getBean( "securableDao" ) );
        securityService.makePrivate( ad, 6 );
        /*
         * uncomment so you can see the acl permission has been changed in the database.
         */
        this.setComplete();
    }

    /**
     * Tests changing object level security on the ArrayDesign from public to private WITHOUT the correct permission
     * (You need to be administrator).
     * 
     * @throws Exception
     */
    public void testMakePrivateWithoutPermission() throws Exception {

        this.onSetUpInTransactionGrantingUserAuthority( "unauthorizedTestUser" );

        ArrayDesign ad = arrayDesignService.findArrayDesignByName( arrayDesignName );
        SecurityService securityService = new SecurityService();

        securityService.setBasicAclExtendedDao( ( BasicAclExtendedDao ) this.getBean( "basicAclExtendedDao" ) );
        securityService.setSecurableDao( ( SecurableDao ) this.getBean( "securableDao" ) );

        boolean fail = false;
        try {
            securityService.makePrivate( ad, 0 );
        } catch ( Exception e ) {
            fail = true;
            log.error( "TEST SUCCESSFULLY FAILED WITH: " );
            e.printStackTrace();
        } finally {
            assertTrue( fail );
        }
    }
}
