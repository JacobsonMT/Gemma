// license-header java merge-point
//
// Attention: Generated code! Do not modify by hand!
// Generated by: Gemma Enumeration.vsl in andromda-java-cartridge.
// $Id$
package ubic.gemma.model.common.measurement;

import java.util.*;

public class MeasurementType implements java.io.Serializable, Comparable<MeasurementType> {
    public static final MeasurementType ABSOLUTE = new MeasurementType( "ABSOLUTE" );
    public static final MeasurementType CHANGE = new MeasurementType( "CHANGE" );
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -7855184420857229497L;
    private static final Map<String, MeasurementType> values = new LinkedHashMap<>( 2, 1 );
    private static List<String> literals = new ArrayList<>( 2 );
    private static List<String> names = new ArrayList<>( 2 );
    private static List<MeasurementType> valueList = new ArrayList<>( 2 );

    static {
        values.put( ABSOLUTE.value, ABSOLUTE );
        valueList.add( ABSOLUTE );
        literals.add( ABSOLUTE.value );
        names.add( "ABSOLUTE" );
        values.put( CHANGE.value, CHANGE );
        valueList.add( CHANGE );
        literals.add( CHANGE.value );
        names.add( "CHANGE" );
        valueList = Collections.unmodifiableList( valueList );
        literals = Collections.unmodifiableList( literals );
        names = Collections.unmodifiableList( names );
    }

    private String value;

    /**
     * The default constructor allowing super classes to access it.
     */
    protected MeasurementType() {
    }

    private MeasurementType( String value ) {
        this.value = value;
    }

    /**
     * Creates an instance of MeasurementType from <code>value</code>.
     *
     * @param value the value to create the MeasurementType from.
     * @return measurement type
     */
    public static MeasurementType fromString( String value ) {
        final MeasurementType typeValue = values.get( value );
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
    public static List<String> literals() {
        return literals;
    }

    /**
     * Returns an unmodifiable list containing the names of the literals that are known by this enumeration.
     *
     * @return A List containing the actual names of the literals defined by this enumeration, this list can not be
     * modified.
     */
    public static List<String> names() {
        return names;
    }

    /**
     * Returns an unmodifiable list containing the actual enumeration instance values.
     *
     * @return A List containing the actual enumeration instance values.
     */
    public static List<MeasurementType> values() {
        return valueList;
    }

    @Override
    public int compareTo( MeasurementType that ) {
        return ( this == that ) ? 0 : this.getValue().compareTo( ( that ).getValue() );
    }

    @Override
    public boolean equals( Object object ) {
        return ( this == object ) || ( object instanceof MeasurementType && ( ( MeasurementType ) object ).getValue()
                .equals( this.getValue() ) );
    }

    /**
     * Gets the underlying value of this type safe enumeration.
     *
     * @return the underlying value.
     */
    public String getValue() {
        return this.value;
    }

    @Override
    public int hashCode() {
        return this.getValue().hashCode();
    }

    @Override
    public String toString() {
        return String.valueOf( value );
    }

    /**
     * This method allows the deserialization of an instance of this enumeration type to return the actual instance that
     * will be the singleton for the JVM in which the current thread is running.
     * Doing this will allow users to safely use the equality operator <code>==</code> for enumerations because a
     * regular deserialized object is always a newly constructed instance and will therefore never be an existing
     * reference; it is this <code>readResolve()</code> method which will intercept the deserialization process in order
     * to return the proper singleton reference.
     * This method is documented here: <a
     * href="http://java.sun.com/j2se/1.3/docs/guide/serialization/spec/input.doc6.html">Java Object Serialization
     * Specification</a>
     *
     * @return object
     */
    private Object readResolve() {
        return MeasurementType.fromString( this.value );
    }
}