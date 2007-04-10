/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package ubic.gemma.model.common.auditAndSecurity;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.common.auditAndSecurity.Person
 */
public class PersonDaoImpl extends ubic.gemma.model.common.auditAndSecurity.PersonDaoBase {

    @Override
    public Person find( Person person ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( Person.class );

            if ( person.getEmail() != null ) {
                queryObject.add( Restrictions.eq( "email", person.getEmail() ) );
            } else {
                if ( person.getName() != null ) queryObject.add( Restrictions.eq( "name", person.getName() ) );
                if ( person.getLastName() != null )
                    queryObject.add( Restrictions.eq( "lastName", person.getLastName() ) );
                if ( person.getPhone() != null ) queryObject.add( Restrictions.eq( "phone", person.getPhone() ) );
                if ( person.getAddress() != null )
                    queryObject.add( Restrictions.eq( "address", person.getAddress() ) );
                if ( person.getName() != null ) queryObject.add( Restrictions.eq( "name", person.getName() ) );
            }

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '"
                                    + ubic.gemma.model.common.auditAndSecurity.Person.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( Person ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @Override
    public Person findOrCreate( Person person ) {
        if ( person == null
                || ( person.getLastName() == null && person.getAddress() == null && person.getEmail() == null
                        && person.getPhone() == null && person.getName() == null ) ) {
            throw new IllegalArgumentException( "Person did not have sufficient information for business key." );
        }
        Person newPerson = find( person );
        if ( newPerson != null ) {
            return newPerson;
        }
        return ( Person ) create( person );
    }

}