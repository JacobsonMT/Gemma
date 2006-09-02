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
package ubic.gemma.security.principal;

import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.userdetails.UserDetailsService;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.springframework.dao.DataAccessException;

import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserService;

/**
 * Implementation for Acegi
 * 
 * @author pavlidis
 * @version $Id$
 */
public class UserDetailsServiceImpl implements UserDetailsService {

    UserService userService;

    /**
     * @param userService the userService to set
     */
    public void setUserService( UserService userService ) {
        this.userService = userService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.acegisecurity.userdetails.UserDetailsService#loadUserByUsername(java.lang.String)
     */
    public UserDetails loadUserByUsername( String username ) throws UsernameNotFoundException, DataAccessException {

        User u = userService.findByUserName( username );

        if ( u == null ) {
            throw new UsernameNotFoundException( username + " not found" );
        }
        return new UserDetailsImpl( u );
    }

    /**
     * Returns a String username (the principal).
     * 
     * @return
     */
    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assert auth != null;
        assert auth.isAuthenticated();

        if ( auth.getPrincipal() instanceof UserDetails ) {
            return ( ( UserDetails ) auth.getPrincipal() ).getUsername();
        }
        return auth.getPrincipal().toString();

    }
}
