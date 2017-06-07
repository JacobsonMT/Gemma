/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.common.description;

import ubic.gemma.model.common.Identifiable;

import java.io.File;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.Collection;

public abstract class LocalFile implements Identifiable, Serializable, gemma.gsec.model.Securable {

    private static final long serialVersionUID = 5057142607188347151L;
    private java.net.URL localURL;
    private java.net.URL remoteURL;
    private String version;
    private Long size;
    private Long id;
    private FileFormat format;
    private Collection<LocalFile> sourceFiles = new java.util.HashSet<>();

    /**
     * Attempt to create a java.io.File from the local URI. If it doesn't look like a URI, it is just treated as a path.
     *
     * @see ubic.gemma.model.common.description.LocalFile#asFile()
     */
    public java.io.File asFile() {

        if ( this.getLocalURL() == null ) {
            return null;
        }

        try {
            return new File( this.getLocalURL().toURI() );
        } catch ( URISyntaxException e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * @see ubic.gemma.model.common.description.LocalFile#canRead()
     */
    public Boolean canRead() {
        return this.asFile().canRead();
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFile#canWrite()
     */
    public Boolean canWrite() {
        return this.asFile().canWrite();
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        LocalFile other = ( LocalFile ) obj;
        if ( id == null ) {
            if ( other.id != null )
                return false;
        } else if ( !id.equals( other.id ) )
            return false;

        if ( localURL == null ) {
            if ( other.localURL != null )
                return false;
        } else if ( !localURL.equals( other.localURL ) )
            return false;
        return true;
    }

    public FileFormat getFormat() {
        return this.format;
    }

    public void setFormat( FileFormat format ) {
        this.format = format;
    }

    @Override
    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    /**
     * The location of the file on a local server
     */
    public java.net.URL getLocalURL() {
        return this.localURL;
    }

    public void setLocalURL( java.net.URL localURL ) {
        this.localURL = localURL;
    }

    /**
     * Source where the file was downloaded from.
     */
    public java.net.URL getRemoteURL() {
        return this.remoteURL;
    }

    public void setRemoteURL( java.net.URL remoteURL ) {
        this.remoteURL = remoteURL;
    }

    public Long getSize() {
        return this.size;
    }

    public void setSize( Long size ) {
        this.size = size;
    }

    /**
     * Any files which were used to create this file.
     */
    public Collection<ubic.gemma.model.common.description.LocalFile> getSourceFiles() {
        return this.sourceFiles;
    }

    public void setSourceFiles( Collection<LocalFile> sourceFiles ) {
        this.sourceFiles = sourceFiles;
    }

    /**
     * The version identifier for the file; this could be as simple as the date of creation
     */
    public String getVersion() {
        return this.version;
    }

    public void setVersion( String version ) {
        this.version = version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );

        if ( id != null )
            result = prime * result + ( ( localURL == null ) ? 0 : localURL.hashCode() );
        return result;
    }

    /**
     * Constructs new instances of {@link ubic.gemma.model.common.description.LocalFile}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.common.description.LocalFile}.
         */
        public static LocalFile newInstance() {
            return new LocalFileImpl();
        }

    }

}