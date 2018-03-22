/*
 * The Gemma project
 *
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.core.loader.genome;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import ubic.gemma.core.loader.util.parser.BasicLineMapParser;
import ubic.gemma.core.loader.util.parser.LineParser;
import ubic.gemma.core.loader.util.parser.Parser;
import ubic.gemma.model.genome.biosequence.BioSequence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Parse probes from a tabular file. First columnn = probe id; Second column = sequence name; Third column = sequence.
 * This is designed primarily to deal with oligonucleotide arrays that have sequence names different from the probe
 * names.
 *
 * @author paul
 */
public class ProbeSequenceParser extends BasicLineMapParser<String, BioSequence> {

    private final Map<String, BioSequence> results = new HashMap<>();

    @Override
    public boolean containsKey( String key ) {
        return results.containsKey( key );
    }

    @Override
    public BioSequence get( String key ) {
        return results.get( key );
    }

    @Override
    public Collection<String> getKeySet() {
        return results.keySet();
    }

    @Override
    public Collection<BioSequence> getResults() {
        return results.values();
    }

    @Override
    public void parse( InputStream is ) throws IOException {

        if ( is == null )
            throw new IllegalArgumentException( "InputStream was null" );
        try (BufferedReader br = new BufferedReader( new InputStreamReader( is ) )) {
            StopWatch timer = new StopWatch();
            timer.start();
            int nullLines = 0;
            String line;
            int linesParsed = 0;
            while ( ( line = br.readLine() ) != null ) {

                BioSequence newItem = this.parseOneLine( line );

                if ( ++linesParsed % Parser.PARSE_ALERT_FREQUENCY == 0
                        && timer.getTime() > LineParser.PARSE_ALERT_TIME_FREQUENCY_MS ) {
                    String message = "Parsed " + linesParsed + " lines ";
                    log.info( message );
                    timer.reset();
                    timer.start();
                }

                if ( newItem == null ) {
                    nullLines++;
                }

            }
            log.info( "Parsed " + linesParsed + " lines. " + ( nullLines > 0 ?
                    nullLines + " yielded no parse result (they may have been filtered)." :
                    "" ) );

        }
    }

    @Override
    public BioSequence parseOneLine( String line ) {

        if ( line.startsWith( ">" ) ) {
            throw new RuntimeException(
                    "FASTA format not supported - please use the tabular format for oligonucleotides" );
        }

        if ( StringUtils.isBlank( line ) ) {
            return null;
        }

        String[] sArray = StringUtils.splitPreserveAllTokens( line );

        if ( sArray.length == 0 ) {
            return null;
        }

        if ( sArray.length != 3 ) {
            throw new IllegalArgumentException(
                    "Expected 3 fields: probe name, sequence name, sequence; line=" + line );
        }

        String probeId = sArray[0].trim();

        if ( StringUtils.isBlank( probeId ) ) {
            return null;
        }

        String sequenceName = sArray[1].trim();

        String sequence = sArray[2].trim();

        // Rarely there are extra junk characters. See bug 2719
        sequence = sequence.replaceAll( "[^a-yA-Y]", "" );

        // A Adenine
        // C Cytosine
        // G Guanine
        // T Thymine
        // U Uracil
        // R Purine (A or G)
        // Y Pyrimidine (C, T, or U)
        // M C or A
        // K T, U, or G
        // W T, U, or A
        // S C or G
        // B C, T, U, or G (not A)
        // D A, T, U, or G (not C)
        // H A, T, U, or C (not G)
        // V A, C, or G (not T, not U)
        // N Any base (A, C, G, T, or U)

        if ( StringUtils.isBlank( sequence ) ) {
            return null;
        }

        BioSequence seq = BioSequence.Factory.newInstance();
        seq.setSequence( sequence );
        seq.setLength( ( long ) sequence.length() );
        seq.setIsCircular( false );
        seq.setIsApproximateLength( false );
        seq.setName( sequenceName );

        if ( this.results.containsKey( probeId ) ) {
            log.warn( "Duplicated probe id: " + probeId );
        }
        this.put( probeId, seq );

        return seq;
    }

    @Override
    protected String getKey( BioSequence newItem ) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void put( String key, BioSequence value ) {
        results.put( key, value );
    }
}
