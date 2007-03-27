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
package ubic.gemma.apps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Remove tabs from strings stored in the database.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class StringVectorCleanup extends ExpressionExperimentManipulatingCli {

    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {
        Exception e = processCommandLine( "remove tabs from strings", args );
        if ( e != null ) return e;

        QuantitationTypeService qts = ( QuantitationTypeService ) this.getBean( "quantitationTypeService" );

        DesignElementDataVectorService dedvs = ( DesignElementDataVectorService ) this
                .getBean( "designElementDataVectorService" );

        Collection<QuantitationType> types;
        if ( this.getExperimentShortName() != null ) {
            ExpressionExperiment ee = this.locateExpressionExperiment( this.getExperimentShortName() );
            types = this.expressionExperimentService.getQuantitationTypes( ee );
        } else {
            types = qts.loadAll();
        }

        ByteArrayConverter converter = new ByteArrayConverter();

        qtype: for ( QuantitationType type : types ) {
            if ( !type.getRepresentation().equals( PrimitiveType.STRING ) ) continue;

            log.info( "Processing " + type );
            Collection<DesignElementDataVector> vecs = dedvs.find( type );
            dedvs.thaw( vecs );

            int count = 0;
            for ( DesignElementDataVector vector : vecs ) {
                byte[] dat = vector.getData();
                int numBioAssays = vector.getBioAssayDimension().getBioAssays().size();
                String[] rawStrings = converter.byteArrayToStrings( dat );
                List<String> updated = new ArrayList<String>();
                for ( String string : rawStrings ) {
                    if ( !string.equals( "\t" ) ) {
                        updated.add( string );
                    }
                }

                if ( updated.size() != numBioAssays ) {
                    dedvs.thaw( vector );
                    log.error( "Vector " + vector.getId()
                            + " did not have right number of values after 'tab' removal for " + type + "; expected "
                            + numBioAssays + " got " + updated.size() );
                    continue qtype;
                }

                byte[] newDat = converter.toBytes( updated.toArray( new String[] {} ) );
                vector.setData( newDat );

                if ( ++count % 10000 == 0 ) {
                    log.info( "Processed " + count + " vectors for " + type );
                }
            }

            log.info( "Updating " + vecs.size() + " vectors" );
            dedvs.update( vecs );

        }
        return null;

    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        StringVectorCleanup c = new StringVectorCleanup();
        Exception e = c.doWork( args );
        if ( e != null ) {
            log.fatal( e, e );
        }

    }
}
