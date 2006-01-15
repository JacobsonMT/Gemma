/*
 * The Gemma project.
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
package edu.columbia.gemma.common.description;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 */
public class LocalFileServiceImpl extends edu.columbia.gemma.common.description.LocalFileServiceBase {

    /**
     * @see edu.columbia.gemma.common.description.LocalFileService#deleteFile(edu.columbia.gemma.common.description.LocalFile)
     */
    @Override
    protected void handleDeleteFile( edu.columbia.gemma.common.description.LocalFile localFile )
            throws java.lang.Exception {

        if ( localFile == null ) return;
        File file = localFile.asFile();
        if ( file == null ) throw new IOException( "Could not convert LocalFile into java.io.File" );

        boolean success = false;
        if ( file.exists() ) {
            success = file.delete();
        }

        if ( file.exists() || !success ) {
            throw new IOException( "Cannot delete file" );
        }
        this.getLocalFileDao().remove( localFile );
    }

    /**
     * @see edu.columbia.gemma.common.description.LocalFileService#copyFile(edu.columbia.gemma.common.description.LocalFile,
     *      edu.columbia.gemma.common.description.LocalFile)
     */
    @Override
    protected edu.columbia.gemma.common.description.LocalFile handleCopyFile(
            edu.columbia.gemma.common.description.LocalFile sourceFile,
            edu.columbia.gemma.common.description.LocalFile targetFile ) throws java.lang.Exception {
        File src = sourceFile.asFile();
        if ( src == null ) throw new IOException( "Could not convert LocalFile into java.io.File" );

        File dst = targetFile.asFile();
        if ( dst == null ) throw new IOException( "Could not convert LocalFile into java.io.File" );

        InputStream in = new FileInputStream( src );
        OutputStream out = new FileOutputStream( dst );

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        long size = 0;
        while ( ( len = in.read( buf ) ) > 0 ) {
            out.write( buf, 0, len );
            size += len;
        }
        in.close();
        out.close();

        targetFile.setSize( size );

        this.getLocalFileDao().create( targetFile );
        return targetFile;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.common.description.LocalFileServiceBase#handleFindByPath(java.lang.String)
     */
    @Override
    protected LocalFile handleFindByPath( String path ) throws Exception {
        File f = new File( path );
        LocalFile seek = LocalFile.Factory.newInstance();
        seek.setLocalURI( f.toURI().toString() );
        return this.getLocalFileDao().find( seek );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.common.description.LocalFileServiceBase#handleFind(edu.columbia.gemma.common.description.LocalFile)
     */
    @Override
    protected LocalFile handleFind( LocalFile localFile ) throws Exception {
        return this.getLocalFileDao().find( localFile );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.common.description.LocalFileServiceBase#handleSave(edu.columbia.gemma.common.description.LocalFile)
     */
    @Override
    protected LocalFile handleSave( LocalFile localFile ) throws Exception {
        return this.getLocalFileDao().create( localFile );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.common.description.LocalFileServiceBase#handleUpdate(edu.columbia.gemma.common.description.LocalFile)
     */
    @Override
    protected void handleUpdate( LocalFile localFile ) throws Exception {
        this.getLocalFileDao().update( localFile );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.common.description.LocalFileServiceBase#handleFindOrCreate(edu.columbia.gemma.common.description.LocalFile)
     */
    @Override
    protected LocalFile handleFindOrCreate( LocalFile localFile ) throws Exception {
        return this.getLocalFileDao().findOrCreate( localFile );
    }

}