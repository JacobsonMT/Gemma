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
package ubic.gemma.persistence.hibernate.usertypes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

/**
 * Converts strings in the database into java.net.URL objects.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class HibernateURLType implements UserType {

    private static final long serialVersionUID = 100002L;

    /*
     * (non-Javadoc)
     * 
     * @see org.hibernate.usertype.UserType#assemble(java.io.Serializable, java.lang.Object)
     */
    @SuppressWarnings("unused")
    public Object assemble( Serializable cached, Object owner ) throws HibernateException {
        return this.deepCopy( cached );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.hibernate.usertype.UserType#deepCopy(java.lang.Object)
     */
    public Object deepCopy( Object value ) throws HibernateException {
        String ret = null;
        if ( value == null ) {
            return null;
        }
        java.net.URL in = ( java.net.URL ) value;
        String s = in.toString();
        int len = s.length();
        char[] buf = new char[len];

        for ( int i = 0; i < len; i++ ) {
            buf[i] = s.charAt( i );
        }
        ret = new String( buf );
        java.net.URL result;
        try {
            result = new java.net.URL( ret );
        } catch ( MalformedURLException e ) {
            throw new HibernateException( "Malformed url", e );
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.hibernate.usertype.UserType#disassemble(java.lang.Object)
     */
    public Serializable disassemble( Object value ) throws HibernateException {
        return ( java.io.Serializable ) value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.hibernate.usertype.UserType#equals(java.lang.Object, java.lang.Object)
     */
    public boolean equals( Object x, Object y ) throws HibernateException {
        boolean equal = false;
        if ( x == null || y == null ) {
            equal = false;
        } else if ( !( x instanceof java.net.URL ) || !( y instanceof java.net.URL ) ) {
            equal = false;
        } else {
            equal = ( ( java.net.URL ) x ).equals( y );
        }
        return equal;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.hibernate.usertype.UserType#hashCode(java.lang.Object)
     */
    public int hashCode( Object x ) throws HibernateException {
        return x.hashCode();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.hibernate.usertype.UserType#isMutable()
     */
    public boolean isMutable() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.hibernate.usertype.UserType#nullSafeGet(java.sql.ResultSet, java.lang.String[], java.lang.Object)
     */
    @SuppressWarnings("unused")
    public Object nullSafeGet( ResultSet rs, String[] names, Object owner ) throws HibernateException, SQLException {
        final StringBuffer buffer = new StringBuffer();
        try {
            // First we get the stream
            InputStream inputStream = rs.getAsciiStream( names[0] );
            if ( inputStream == null ) {
                return null;
            }
            byte[] buf = new byte[1024];
            int read = -1;

            while ( ( read = inputStream.read( buf ) ) > 0 ) {
                buffer.append( new String( buf, 0, read ) );
            }
            inputStream.close();
        } catch ( IOException exception ) {
            throw new HibernateException( "Unable to read from resultset", exception );
        }
        try {
            return new java.net.URL( buffer.toString() );
        } catch ( MalformedURLException e ) {
            throw new HibernateException( "Malformed url", e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.hibernate.usertype.UserType#nullSafeSet(java.sql.PreparedStatement, java.lang.Object, int)
     */
    public void nullSafeSet( PreparedStatement preparedStatement, Object data, int index ) throws HibernateException,
            SQLException {
        if ( data == null ) {
            preparedStatement.setString( index, null );
        } else {
            java.net.URL in = ( java.net.URL ) data;
            String s = in.toString();
            byte[] buf = s.getBytes();
            int len = buf.length;
            ByteArrayInputStream bais = new ByteArrayInputStream( buf );
            preparedStatement.setAsciiStream( index, bais, len );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.hibernate.usertype.UserType#replace(java.lang.Object, java.lang.Object, java.lang.Object)
     */
    @SuppressWarnings("unused")
    public Object replace( Object original, Object target, Object owner ) throws HibernateException {
        return this.deepCopy( original );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.hibernate.usertype.UserType#returnedClass()
     */
    public Class returnedClass() {
        return java.net.URL.class;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.hibernate.usertype.UserType#sqlTypes()
     */
    public int[] sqlTypes() {
        return new int[] { Types.CLOB };
    }

}
