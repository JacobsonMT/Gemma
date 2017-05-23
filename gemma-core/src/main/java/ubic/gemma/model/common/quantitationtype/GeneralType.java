/*
 * The gemma-core project
 *
 * Copyright (c) 2017 University of British Columbia
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
package ubic.gemma.model.common.quantitationtype;

/**
 *
 */
public class GeneralType implements java.io.Serializable, Comparable<GeneralType> {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 2881229542950441811L;

    /**
     *
     */
    public static final GeneralType QUANTITATIVE = new GeneralType( "QUANTITATIVE" );

    /**
     *
     */
    public static final GeneralType CATEGORICAL = new GeneralType( "CATEGORICAL" );

    /**
     *
     */
    public static final GeneralType UNKNOWN = new GeneralType( "UNKNOWN" );

    /**
     * Creates an instance of GeneralType from <code>value</code>.
     *
     * @param value the value to create the GeneralType from.
     */
    public static GeneralType fromString( String value ) {
        final GeneralType typeValue = values.get( value );
        if ( typeValue == null ) {
            /*
             * Customization to permit database values to change before code does. Previously this would throw an
             * exception.
             */
            // throw new IllegalArgumentException("invalid value '" + value + "', possible values are: " + literals);
            return null;
        }
        return typeValue;
    }

    /**
     * Returns an unmodifiable list containing the literals that are known by this enumeration.
     *
     * @return A List containing the actual literals defined by this enumeration, this list can not be modified.
     */
    public static java.util.List<String> literals() {
        return literals;
    }

    /**
     * Returns an unmodifiable list containing the names of the literals that are known by this enumeration.
     *
     * @return A List containing the actual names of the literals defined by this enumeration, this list can not be
     *         modified.
     */
    public static java.util.List<String> names() {
        return names;
    }

    /**
     * Returns an unmodifiable list containing the actual enumeration instance values.
     *
     * @return A List containing the actual enumeration instance values.
     */
    public static java.util.List<GeneralType> values() {
        return valueList;
    }

    private String value;

    private static final java.util.Map<String, GeneralType> values = new java.util.LinkedHashMap<>(
            3, 1 );

    private static java.util.List<String> literals = new java.util.ArrayList<>( 3 );

    private static java.util.List<String> names = new java.util.ArrayList<>( 3 );

    private static java.util.List<GeneralType> valueList = new java.util.ArrayList<>( 3 );

    /**
     * Initializes the values.
     */
    static {
        values.put( QUANTITATIVE.value, QUANTITATIVE );
        valueList.add( QUANTITATIVE );
        literals.add( QUANTITATIVE.value );
        names.add( "QUANTITATIVE" );
        values.put( CATEGORICAL.value, CATEGORICAL );
        valueList.add( CATEGORICAL );
        literals.add( CATEGORICAL.value );
        names.add( "CATEGORICAL" );
        values.put( UNKNOWN.value, UNKNOWN );
        valueList.add( UNKNOWN );
        literals.add( UNKNOWN.value );
        names.add( "UNKNOWN" );
        valueList = java.util.Collections.unmodifiableList( valueList );
        literals = java.util.Collections.unmodifiableList( literals );
        names = java.util.Collections.unmodifiableList( names );
    }

    /**
     * The default constructor allowing super classes to access it.
     */
    protected GeneralType() {
    }

    private GeneralType( String value ) {
        this.value = value;
    }

    /**
     * @see Comparable#compareTo(Object)
     */
    @Override
    public int compareTo( GeneralType that ) {
        return ( this == that ) ? 0 : this.getValue().compareTo( ( that ).getValue() );
    }

    /**
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals( Object object ) {
        return ( this == object )
                || ( object instanceof GeneralType && ( ( GeneralType ) object ).getValue().equals( this.getValue() ) );
    }

    /**
     * Gets the underlying value of this type safe enumeration.
     *
     * @return the underlying value.
     */
    public String getValue() {
        return this.value;
    }

    /**
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.getValue().hashCode();
    }

    /**
     * @see Object#toString()
     */
    @Override
    public String toString() {
        return String.valueOf( value );
    }

    /**
     * This method allows the deserialization of an instance of this enumeration type to return the actual instance that
     * will be the singleton for the JVM in which the current thread is running.
     * <p>
     * Doing this will allow users to safely use the equality operator <code>==</code> for enumerations because a
     * regular deserialized object is always a newly constructed instance and will therefore never be an existing
     * reference; it is this <code>readResolve()</code> method which will intercept the deserialization process in order
     * to return the proper singleton reference.
     * <p>
     * This method is documented here: <a
     * href="http://java.sun.com/j2se/1.3/docs/guide/serialization/spec/input.doc6.html">Java Object Serialization
     * Specification</a>
     */
    private Object readResolve() {
        return GeneralType.fromString( this.value );
    }
}