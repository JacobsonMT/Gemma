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
package ubic.gemma.model.common.auditAndSecurity;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import junit.framework.TestCase;

/**
 * @author pavlidis
 * @version $Id$
 */
public class UserServiceImplTest extends TestCase {
    private UserServiceImpl userService = new UserServiceImpl();
    private UserDao userDaoMock;
    private User testUser = User.Factory.newInstance();

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        userDaoMock = createMock( UserDao.class );
        userService.setUserDao( userDaoMock );
        testUser.setEmail( "foo@bar" );
        testUser.setFirstName( "Foo" );
        testUser.setLastName( "Bar" );
        testUser.setMiddleName( "" );
        testUser.setUserName( "foobar" );
        testUser.setPassword( "aija" );
        testUser.setConfirmPassword( "aija" );
        testUser.setPasswordHint( "I am an idiot" );
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testHandleGetUser() {
        userDaoMock.findByUserName( "foobar" );
        expectLastCall().andReturn( testUser );
        replay( userDaoMock );
        userService.getUser( "foobar" );
        verify( userDaoMock );
    }

    public void testHandleGetUsers() {
    }

    public void testHandleSaveUser() throws Exception {
        userDaoMock.findByUserName( "foobar" );
        expectLastCall().andReturn( null );
        userDaoMock.findByEmail( "foo@bar" );
        expectLastCall().andReturn( null );
        userDaoMock.create( testUser );
        expectLastCall().andReturn( testUser );
        replay( userDaoMock );
        userService.saveUser( testUser );
        verify( userDaoMock );
    }

    public void testHandleRemoveUser() {
        userDaoMock.remove( testUser );
        // setVoidCallable();
        userDaoMock.findByUserName( "foobar" );
        expectLastCall().andReturn( testUser ); // this should get called to find the user to remove.
        replay( userDaoMock );
        userService.removeUser( "foobar" );
        verify( userDaoMock );
    }

}
