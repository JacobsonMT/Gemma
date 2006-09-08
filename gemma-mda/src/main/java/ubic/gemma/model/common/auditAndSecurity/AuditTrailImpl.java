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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.AuditTrail
 * @author pavlidis
 * @version $Id$
 */
public class AuditTrailImpl extends ubic.gemma.model.common.auditAndSecurity.AuditTrail {

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrail#addEvent(ubic.gemma.model.common.auditAndSecurity.AuditEvent)
     */
    @SuppressWarnings("unchecked")
    public void addEvent( AuditEvent event ) {
        if ( event == null ) throw new IllegalArgumentException( "AuditEvent cannot be null" );
        assert this.getEvents() != null;
        this.getEvents().add( event );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrail#getCreationEvent()
     */
    @Override
    public AuditEvent getCreationEvent() {
        assert this.getEvents() != null;
        if ( this.getEvents().size() == 0 ) {
            return null;
        }
        return ( ( List<AuditEvent> ) this.getEvents() ).get( 0 );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrail#getLast()
     */
    @Override
    public AuditEvent getLast() {
        assert this.getEvents() != null;
        if ( this.getEvents().size() == 0 ) {
            return null;
        }
        return ( ( List<AuditEvent> ) this.getEvents() ).get( this.getEvents().size() - 1 );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrail#start()
     */
    @Override
    public void start() {
        this.start( null );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrail#start(java.lang.String)
     */
    @Override
    public void start( String note ) {
        this.start( note, null );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrail#start(java.lang.String, Person)
     */
    @Override
    public void start( String note, User actor ) {
        this.initialize(); // ensure that this is the first event.
        AuditEvent newEvent = AuditEvent.Factory.newInstance();
        newEvent.setAction( AuditAction.CREATE );
        newEvent.setDate( new Date() );
        newEvent.setPerformer( actor );
        if ( note != null ) newEvent.setNote( note );

        this.addEvent( newEvent );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrail#update()
     */
    @Override
    public void update() {
        this.update( null );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrail#update(java.lang.String)
     */
    @Override
    public void update( String note ) {
        this.update( note, null );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrail#update(java.lang.String, Person)
     */
    @Override
    public void update( String note, User actor ) {
        assert this.getEvents() != null;
        assert this.getEvents().size() > 0; // can't have update as the first
        // event.

        AuditEvent newEvent = AuditEvent.Factory.newInstance();
        newEvent.setAction( AuditAction.UPDATE );
        newEvent.setPerformer( actor );
        newEvent.setDate( new Date() );
        if ( note != null ) newEvent.setNote( note );

        this.addEvent( newEvent );
    }

    /**
     * If this AuditTrail's list is empty or null, initialize it. Otherwise, clear any events.
     */
    private void initialize() {
        if ( trailIsNull() ) {
            this.setEvents( new ArrayList<AuditEvent>() );
        } else {
            this.getEvents().clear();
        }
    }

    /**
     * Check whether the list of events doesn't exist yet.
     * 
     * @return
     */
    private boolean trailIsNull() {
        return this.getEvents() == null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrail#read()
     */
    @Override
    public void read() {
        this.read( null );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrail#read(java.lang.String)
     */
    @Override
    public void read( String note ) {
        this.read( note, null );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrail#read(java.lang.String,
     *      ubic.gemma.model.common.auditAndSecurity.User)
     */
    @Override
    public void read( String note, User actor ) {
        assert this.getEvents() != null;
        assert this.getEvents().size() > 0; // can't have read as the first
        // event.

        AuditEvent newEvent = AuditEvent.Factory.newInstance();
        newEvent.setAction( AuditAction.READ );
        newEvent.setPerformer( actor );
        newEvent.setDate( new Date() );
        if ( note != null ) newEvent.setNote( note );

        this.addEvent( newEvent );

    }

    // todo this has to be added to the model.F
//    /*
//     * (non-Javadoc)
//     * 
//     * @see java.lang.Object#toString()
//     */
//    @Override
//    public String toString() {
//        return "AuditTrail: Id=" + getId()
//                + ( this.getEvents() == null ? "" : ( ", " + this.getEvents().size() + " events" ) );
//    }

}