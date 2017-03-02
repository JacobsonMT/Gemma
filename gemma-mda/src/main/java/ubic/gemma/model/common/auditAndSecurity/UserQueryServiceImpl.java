/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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

import java.sql.Date;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Paul
 * @version $Id$
 */
@Service
public class UserQueryServiceImpl implements UserQueryService {

    @Autowired
    private UserQueryDao userQueryDao;

    @Override
    @Transactional
    public UserQuery create( UserQuery userQuery ) {
        return userQueryDao.create( userQuery );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<UserQuery> findByUser( User user ) {
        return userQueryDao.findByUser( user );
    }

    @Override
    @Transactional(readOnly = true)
    public UserQuery findMostRecentForUser( User user ) {
        return userQueryDao.findMostRecentForUser( user );
    }

    @Override
    @Transactional(readOnly = true)
    public UserQuery load( Long id ) {
        return userQueryDao.load( id );
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public Collection<UserQuery> loadAll() {
        return ( Collection<UserQuery> ) userQueryDao.loadAll();
    }

    @Override
    @Transactional
    public void remove( UserQuery userQuery ) {
        userQueryDao.remove( userQuery );
    }

    @Override
    @Transactional
    public void removeAllForUser( User user ) {
        userQueryDao.removeAllForUser( user );
    }

    @Override
    @Transactional
    public void removeOldForUser( User user, Date staleDate ) {
        userQueryDao.removeOldForUser( user, staleDate );
    }

    public void setUserQueryDao( UserQueryDao userQueryDao ) {
        this.userQueryDao = userQueryDao;
    }

}
