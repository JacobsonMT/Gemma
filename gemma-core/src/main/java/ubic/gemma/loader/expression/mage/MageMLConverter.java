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
package ubic.gemma.loader.expression.mage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;

import ubic.gemma.loader.util.converter.Converter;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.DesignElement;

/**
 * Class to parse MAGE-ML files and convert them into Gemma domain objects SDO.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="mageMLConverter" singleton="false"
 */
public class MageMLConverter extends AbstractMageTool implements Converter {

    private Collection<Object> convertedResult;
    private boolean isConverted = false;

    private MageMLConverterHelper mageConverterHelper;

    /**
     * @param simplifiedXml The simplifiedXml to set.
     */
    public void setSimplifiedXml( Document simplifiedXml ) {
        assert mageConverterHelper != null;
        this.simplifiedXml = simplifiedXml;
        mageConverterHelper.setSimplifiedXml( this.simplifiedXml );
    }

    /**
     * default constructor
     */
    public MageMLConverter() {
        super();
        this.mageConverterHelper = new MageMLConverterHelper();
    }

    /**
     * This is provided for tests.
     * 
     * @param path
     */
    protected void addLocalExternalDataPath( String path ) {
        mageConverterHelper.addLocalExternalDataPath( path );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.Converter#convert(java.util.Collection)
     */
    public Collection<Object> convert( Collection<Object> Objects ) {
        Package[] allPackages = Package.getPackages();
        if ( convertedResult == null ) {
            convertedResult = new ArrayList<Object>();
        } else {
            convertedResult.clear();
        }

        // this is a little inefficient because it tries every possible package and class. - fix is to get just
        // the mage
        // packages!
        for ( int i = 0; i < allPackages.length; i++ ) {

            String name = allPackages[i].getName();
            if ( !name.startsWith( "org.biomage." ) || name.startsWith( "org.biomage.tools." )
                    || name.startsWith( "org.biomage.Interface" ) ) continue;

            for ( int j = 0; j < mageClasses.length; j++ ) {
                try {
                    Class c = Class.forName( name + "." + mageClasses[j] );
                    Collection<Object> convertedObjects = getConvertedDataForType( c, Objects );
                    if ( convertedObjects != null && convertedObjects.size() > 0 ) {
                        log.info( "Adding " + convertedObjects.size() + " converted " + name + "." + mageClasses[j]
                                + "s" );
                        convertedResult.addAll( convertedObjects );
                    }
                } catch ( ClassNotFoundException ignored ) {
                }
            }
        }
        this.isConverted = true;
        return convertedResult;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.expression.mage.MageMLConverter#getBioAssayDesignElementDimension(org.biomage.BioAssay.BioAssay)
     */
    public List<DesignElement> getBioAssayDesignElementDimension( BioAssay bioAssay ) {
        assert isConverted;
        return this.mageConverterHelper.getBioAssayDesignElementDimension( bioAssay );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.expression.mage.MageMLConverter#getBioAssayQuantitationTypeDimension(org.biomage.BioAssay.BioAssay)
     */
    public List<ubic.gemma.model.common.quantitationtype.QuantitationType> getBioAssayQuantitationTypeDimension(
            BioAssay bioAssay ) {
        assert isConverted;
        return this.mageConverterHelper.getBioAssayQuantitationTypeDimension( bioAssay );
    }

    /**
     * @return all the converted BioAssay objects.
     */
    public List<BioAssay> getConvertedBioAssays() {
        assert isConverted;
        List<BioAssay> result = new ArrayList<BioAssay>();
        for ( Object object : convertedResult ) {
            if ( object instanceof BioAssay ) {
                result.add( ( BioAssay ) object );
            }
        }
        log.info( "Found " + result.size() + " bioassays" );
        return result;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        Map<String, Integer> tally = new HashMap<String, Integer>();
        for ( Object element : convertedResult ) {
            String clazz = element.getClass().getName();
            if ( !tally.containsKey( clazz ) ) {
                tally.put( clazz, new Integer( 0 ) );
            }
            tally.put( clazz, new Integer( ( tally.get( clazz ) ).intValue() + 1 ) );
        }

        for ( String clazz : tally.keySet() ) {
            buf.append( tally.get( clazz ) + " " + clazz + "s\n" );
        }

        return buf.toString();
    }

    /**
     * Generic method to extract desired data, converted to the Gemma domain objects.
     * 
     * @param type
     * @return
     */
    private Collection<Object> getConvertedDataForType( Class type, Collection<Object> mageDomainObjects ) {
        if ( mageDomainObjects == null ) return null;

        Collection<Object> localResult = new ArrayList<Object>();

        for ( Object element : mageDomainObjects ) {
            if ( element == null ) continue;
            if ( !( element.getClass().isAssignableFrom( type ) ) ) continue;

            Object converted = convert( element );
            if ( converted != null ) localResult.add( mageConverterHelper.convert( element ) );
        }
        return localResult;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.Converter#convert(java.lang.Object)
     */
    public Object convert( Object mageObject ) {
        if ( mageObject == null ) return null;
        return mageConverterHelper.convert( mageObject );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.expression.mage.MageMLConverterHelper#getBioAssayDimensions()
     */
    public BioAssayDimensions getBioAssayDimensions() {
        return this.mageConverterHelper.getBioAssayDimensions();
    }

}
