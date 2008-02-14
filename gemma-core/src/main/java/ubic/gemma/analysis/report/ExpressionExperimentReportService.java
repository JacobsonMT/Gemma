/*
 * The Gemma project
 * 
 * Copyright (c) 2007 Columbia University
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
package ubic.gemma.analysis.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import ubic.basecode.util.FileTools;
import ubic.basecode.util.benchmark.Timed;
import ubic.gemma.grid.javaspaces.SpacesCommand;
import ubic.gemma.grid.javaspaces.SpacesResult;
import ubic.gemma.grid.javaspaces.expression.experiment.ExpressionExperimentReportTask;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignGeneMappingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.LinkAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.MissingValueAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.RankComputationEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.TroubleStatusFlagEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ValidatedFlagEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.progress.TaskRunningService;

/**
 * @author jsantos
 * @spring.bean name="expressionExperimentReportService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="auditTrailService" ref="auditTrailService"
 * @spring.property name="auditEventService" ref="auditEventService"
 * @spring.property name="probe2ProbeCoexpressionService" ref="probe2ProbeCoexpressionService"
 */
public class ExpressionExperimentReportService implements ExpressionExperimentReportTask, InitializingBean {
    private Log log = LogFactory.getLog( this.getClass() );

    private String EE_LINK_SUMMARY = "AllExpressionLinkSummary";
    private String EE_REPORT_DIR = "ExpressionExperimentReports";
    private String HOME_DIR = ConfigUtils.getString( "gemma.appdata.home" );
    private ExpressionExperimentService expressionExperimentService;
    private Probe2ProbeCoexpressionService probe2ProbeCoexpressionService;
    private AuditTrailService auditTrailService;
    private AuditEventService auditEventService;
    private String taskId = null;

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        this.taskId = TaskRunningService.generateTaskId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.grid.javaspaces.expression.experiment.ExpressionExperimentReportTask#execute()
     */
    public SpacesResult execute( SpacesCommand spacesCommand ) {
        this.generateSummaryObjects();
        return null;
    }

    /**
     * Populate information about how many annotations there are, and how many factor values there are.
     * 
     * @param vos
     */
    @SuppressWarnings("unchecked")
    public void fillAnnotationInformation( Collection<ExpressionExperimentValueObject> vos ) {
        Collection<Long> ids = new HashSet<Long>();
        for ( ExpressionExperimentValueObject eeVo : vos ) {
            Long id = eeVo.getId();
            ids.add( id );
        }

        Map<Long, Integer> annotationCounts = expressionExperimentService.getAnnotationCounts( ids );

        Map<Long, Integer> factorCounts = expressionExperimentService.getPopulatedFactorCounts( ids );

        for ( ExpressionExperimentValueObject eeVo : vos ) {
            Long id = eeVo.getId();
            eeVo.setNumAnnotations( annotationCounts.get( id ) );
            eeVo.setNumPopulatedFactors( factorCounts.get( id ) );
        }

    }

    /**
     * @param vos
     */
    @SuppressWarnings("unchecked")
    public void fillDifferentialInformation( Collection<ExpressionExperimentValueObject> vos ) {
        Collection<Long> ids = new HashSet<Long>();
        for ( ExpressionExperimentValueObject eeVo : vos ) {
            Long id = eeVo.getId();
            ids.add( id );
        }

        Collection<ExpressionExperiment> ees = expressionExperimentService.loadMultiple( ids );
        Map<Long, AuditEvent> differentialAnalysisEvents = getEvents( ees, DifferentialExpressionAnalysisEvent.Factory
                .newInstance() );

        for ( ExpressionExperimentValueObject eeVo : vos ) {
            Long id = eeVo.getId();
            if ( differentialAnalysisEvents.containsKey( id ) ) {
                AuditEvent event = differentialAnalysisEvents.get( id );
                if ( event != null ) {
                    eeVo.setDataDifferentialAnalysis( event.getDate() );
                    eeVo.setDifferentialAnalysisEventType( event.getEventType() );
                }
            }
        }
    }

    /**
     * fills in event information from the database. This will only retrieve the latest event (if any).
     * 
     * @return the filled out value objects
     */
    @SuppressWarnings("unchecked")
    @Timed(minimumTimeToReport = 1000)
    public void fillEventInformation( Collection<ExpressionExperimentValueObject> vos ) {

        Collection<Long> ids = new ArrayList<Long>();
        for ( Object object : vos ) {
            ExpressionExperimentValueObject eeVo = ( ExpressionExperimentValueObject ) object;
            ids.add( eeVo.getId() );
        }

        // do this ahead to avoid round trips.
        Collection<ExpressionExperiment> ees = expressionExperimentService.loadMultiple( ids );

        // This is substantially faster than expressionExperimentService.getLastLinkAnalysis( ids ).
        Map<Long, AuditEvent> linkAnalysisEvents = getEvents( ees, LinkAnalysisEvent.Factory.newInstance() );
        Map<Long, AuditEvent> missingValueAnalysisEvents = getEvents( ees, MissingValueAnalysisEvent.Factory
                .newInstance() );
        Map<Long, AuditEvent> rankComputationEvents = getEvents( ees, RankComputationEvent.Factory.newInstance() );
        Map<Long, AuditEvent> troubleEvents = getEvents( ees, TroubleStatusFlagEvent.Factory.newInstance() );
        Map<Long, AuditEvent> validationEvents = getEvents( ees, ValidatedFlagEvent.Factory.newInstance() );
        Map<Long, AuditEvent> arrayDesignEvents = getEvents( ees, ArrayDesignGeneMappingEvent.Factory.newInstance() );

        Map<Long, Collection<AuditEvent>> sampleRemovalEvents = getSampleRemovalEvents( ees );

        // add in the last events of interest for all eeVos
        for ( ExpressionExperimentValueObject eeVo : vos ) {
            Long id = eeVo.getId();
            if ( linkAnalysisEvents.containsKey( id ) ) {
                AuditEvent event = linkAnalysisEvents.get( id );
                if ( event != null ) {
                    eeVo.setDateLinkAnalysis( event.getDate() );
                    eeVo.setLinkAnalysisEventType( event.getEventType() );
                }
            }

            if ( missingValueAnalysisEvents.containsKey( id ) ) {
                AuditEvent event = missingValueAnalysisEvents.get( id );
                if ( event != null ) {
                    eeVo.setDateMissingValueAnalysis( ( event.getDate() ) );
                    eeVo.setMissingValueAnalysisEventType( event.getEventType() );
                }
            }

            if ( rankComputationEvents.containsKey( id ) ) {
                AuditEvent event = rankComputationEvents.get( id );
                if ( event != null ) {
                    eeVo.setDateRankComputation( event.getDate() );
                    eeVo.setRankComputationEventType( event.getEventType() );
                }
            }

            if ( arrayDesignEvents.containsKey( id ) ) {
                AuditEvent event = arrayDesignEvents.get( id );
                if ( event != null ) {
                    eeVo.setDateArrayDesignLastUpdated( event.getDate() );
                }
            }

            if ( sampleRemovalEvents.containsKey( id ) ) {
                Collection<AuditEvent> e = sampleRemovalEvents.get( id );
                // we find we are getting lazy-load exceptions from this guy.
                eeVo.setSampleRemovedFlags( e );
            }

            if ( troubleEvents.containsKey( id ) ) {
                AuditEvent trouble = troubleEvents.get( id );
                // we find we are getting lazy-load exceptions from this guy.
                auditEventService.thaw( trouble );
                eeVo.setTroubleFlag( trouble );
            }
            if ( validationEvents.containsKey( id ) ) {
                AuditEvent validated = validationEvents.get( id );
                auditEventService.thaw( validated );
                eeVo.setValidatedFlag( validated );
            }
        }
    }

    /**
     * @return the filled out value objects fills the link statistics from the cache. If it is not in the cache, the
     *         values will be null.
     */
    public void fillLinkStatsFromCache( Collection vos ) {
        for ( Object object : vos ) {
            ExpressionExperimentValueObject eeVo = ( ExpressionExperimentValueObject ) object;
            ExpressionExperimentValueObject cacheVo = retrieveValueObject( eeVo.getId() );
            if ( cacheVo != null ) {
                eeVo.setBioMaterialCount( cacheVo.getBioMaterialCount() );
                eeVo.setPreferredDesignElementDataVectorCount( cacheVo.getPreferredDesignElementDataVectorCount() );
                eeVo.setCoexpressionLinkCount( cacheVo.getCoexpressionLinkCount() );
                eeVo.setDateCached( cacheVo.getDateCached() );
                eeVo.setDateCreated( cacheVo.getDateCreated() );
                eeVo.setDateLastUpdated( cacheVo.getDateLastUpdated() );
            }
        }
    }

    /**
     * generates a collection of value objects that contain summary information about links, biomaterials, and
     * datavectors
     */
    @SuppressWarnings("unchecked")
    public void generateSummaryObject( Long id ) {
        Collection ids = new ArrayList<Long>();
        ids.add( id );

        generateSummaryObjects( ids );
    }

    /**
     * generates a collection of value objects that contain summary information about links, biomaterials, and
     * datavectors
     */
    public void generateSummaryObjects() {
        initDirectories( false );
        // first, load all expression experiment value objects
        // this will have no stats filled in

        // for each expression experiment, load in stats
        Collection vos = expressionExperimentService.loadAllValueObjects();
        getStats( vos );

        // save the collection
        saveValueObjects( vos );
        log.info( "Stats completed." );
    }

    /**
     * generates a collection of value objects that contain summary information about links, biomaterials, and
     * datavectors
     */
    public void generateSummaryObjects( Collection ids ) {
        initDirectories( false );
        // first, load all expression experiment value objects
        // this will have no stats filled in

        // for each expression experiment, load in stats
        Collection vos = expressionExperimentService.loadValueObjects( ids );
        getStats( vos );

        // save the collection

        saveValueObjects( vos );
        log.info( "Stats completed." );
    }

    /**
     * @return the expressionExperimentService
     */
    public ExpressionExperimentService getExpressionExperimentService() {
        return expressionExperimentService;
    }

    /**
     * @return the probe2ProbeCoexpressionService
     */
    public Probe2ProbeCoexpressionService getProbe2ProbeCoexpressionService() {
        return probe2ProbeCoexpressionService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.grid.javaspaces.SpacesTask#getTaskId()
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * retrieves a collection of cached value objects containing summary information
     * 
     * @return a collection of cached value objects
     */
    public Collection retrieveSummaryObjects() {
        return retrieveValueObjects();
    }

    /**
     * retrieves a collection of cached value objects containing summary information
     * 
     * @return a collection of cached value objects
     */
    public Collection retrieveSummaryObjects( Collection ids ) {
        return retrieveValueObjects( ids );
    }

    public void setAuditEventService( AuditEventService auditEventService ) {
        this.auditEventService = auditEventService;
    }

    /**
     * @param auditTrailService the auditTrailService to set
     */
    public void setAuditTrailService( AuditTrailService auditTrailService ) {
        this.auditTrailService = auditTrailService;
    }

    /**
     * @param expressionExperimentService the expressionExperimentService to set
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param probe2ProbeCoexpressionService the probe2ProbeCoexpressionService to set
     */
    public void setProbe2ProbeCoexpressionService( Probe2ProbeCoexpressionService probe2ProbeCoexpressionService ) {
        this.probe2ProbeCoexpressionService = probe2ProbeCoexpressionService;
    }

    @SuppressWarnings("unchecked")
    @Timed(minimumTimeToReport = 200)
    private Map<Long, AuditEvent> getEvents( Collection<ExpressionExperiment> ees, AuditEventType type ) {
        Map<Long, AuditEvent> result = new HashMap<Long, AuditEvent>();
        Map<Auditable, AuditEvent> events = null;
        if ( type instanceof ArrayDesignAnalysisEvent ) {
            events = expressionExperimentService.getLastArrayDesignUpdate( ees, type.getClass() );
        } else {
            events = expressionExperimentService.getLastAuditEvent( ees, type );
        }

        for ( Auditable a : events.keySet() ) {
            result.put( ( ( ExpressionExperiment ) a ).getId(), events.get( a ) );
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Timed(minimumTimeToReport = 200)
    private Map<Long, Collection<AuditEvent>> getSampleRemovalEvents( Collection<ExpressionExperiment> ees ) {
        Map<Long, Collection<AuditEvent>> result = new HashMap<Long, Collection<AuditEvent>>();
        Map<ExpressionExperiment, Collection<AuditEvent>> rawr = expressionExperimentService
                .getSampleRemovalEvents( ees );
        for ( ExpressionExperiment e : rawr.keySet() ) {
            result.put( e.getId(), rawr.get( e ) );
        }
        return result;
    }

    /**
     * @param id
     * @return
     */
    private String getReportPath( long id ) {
        return HOME_DIR + File.separatorChar + EE_REPORT_DIR + File.separatorChar + EE_LINK_SUMMARY + "." + id;
    }

    /**
     * @param vos
     */
    private void getStats( Collection vos ) {
        log.info( "Getting stats for " + vos.size() + " value objects." );
        String timestamp = DateFormatUtils.format( new Date( System.currentTimeMillis() ), "yyyy.MM.dd HH:mm" );
        for ( Object object : vos ) {
            ExpressionExperimentValueObject eeVo = ( ExpressionExperimentValueObject ) object;
            ExpressionExperiment tempEe = expressionExperimentService.load( eeVo.getId() );

            eeVo.setBioMaterialCount( expressionExperimentService.getBioMaterialCount( tempEe ) );
            eeVo.setPreferredDesignElementDataVectorCount( expressionExperimentService
                    .getPreferredDesignElementDataVectorCount( tempEe ) );

            long numLinks = probe2ProbeCoexpressionService.countLinks( tempEe ).longValue();
            log.info( numLinks + " links." );
            eeVo.setCoexpressionLinkCount( numLinks );

            eeVo.setDateCached( timestamp );

            auditTrailService.thaw( tempEe.getAuditTrail() );
            if ( tempEe.getAuditTrail() != null ) {
                eeVo.setDateCreated( tempEe.getAuditTrail().getCreationEvent().getDate().toString() );
            }
            eeVo.setDateLastUpdated( tempEe.getAuditTrail().getLast().getDate() );
            log.info( "Generated report for " + eeVo.getShortName() );

        }
    }

    /**
     * Check to see if the top level report storage directory exists. If it doesn't, create it, Check to see if the
     * reports directory exists. If it doesn't, create it.
     * 
     * @param deleteFiles
     */
    private void initDirectories( boolean deleteFiles ) {

        FileTools.createDir( HOME_DIR );
        FileTools.createDir( HOME_DIR + File.separatorChar + EE_REPORT_DIR );
        File f = new File( HOME_DIR + File.separatorChar + EE_REPORT_DIR );
        Collection<File> files = new ArrayList<File>();
        File[] fileArray = f.listFiles();
        for ( File file : fileArray ) {
            files.add( file );
        }
        // clear out all files
        if ( deleteFiles ) {
            FileTools.deleteFiles( files );
        }
    }

    // Methods needed to allow this to be used in a space.

    /**
     * @return the serialized value object
     */
    private ExpressionExperimentValueObject retrieveValueObject( long id ) {

        ExpressionExperimentValueObject eeVo = null;
        try {
            File f = new File( getReportPath( id ) );
            if ( f.exists() ) {
                FileInputStream fis = new FileInputStream( getReportPath( id ) );
                ObjectInputStream ois = new ObjectInputStream( fis );
                eeVo = ( ExpressionExperimentValueObject ) ois.readObject();
                ois.close();
                fis.close();
            }
        } catch ( IOException e ) {
            log.warn( "Unable to read report object for id =" + id, e );
            return null;
        } catch ( ClassNotFoundException e ) {
            log.warn( "Unable to read report object for id =" + id, e );
            return null;
        }
        return eeVo;
    }

    /**
     * @return the serialized value objects
     */
    private Collection retrieveValueObjects() {
        Collection eeValueObjects = null;
        // load all files that start with EE_LINK_SUMMARY
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept( File dir, String name ) {
                return ( name.startsWith( EE_LINK_SUMMARY + "." ) );
            }
        };
        File fDir = new File( HOME_DIR + File.separatorChar + EE_REPORT_DIR );
        String[] filenames = fDir.list( filter );
        for ( String objectFile : filenames ) {
            try {
                FileInputStream fis = new FileInputStream( HOME_DIR + File.separatorChar + EE_REPORT_DIR
                        + File.separatorChar + objectFile );
                ObjectInputStream ois = new ObjectInputStream( fis );
                eeValueObjects = ( Collection ) ois.readObject();
                ois.close();
                fis.close();
            } catch ( IOException e ) {
                log.warn( "Unable to read report object from " + objectFile, e );
                continue;
            } catch ( ClassNotFoundException e ) {
                log.warn( "Unable to read report object from " + objectFile, e );
                continue;
            }
        }
        return eeValueObjects;
    }

    /**
     * @return the serialized value objects
     */
    @SuppressWarnings("unchecked")
    private Collection retrieveValueObjects( Collection ids ) {
        Collection eeValueObjects = new ArrayList<ExpressionExperiment>();

        for ( Object object : ids ) {
            Long id = ( Long ) object;

            try {
                File f = new File( getReportPath( id ) );
                if ( f.exists() ) {
                    FileInputStream fis = new FileInputStream( getReportPath( id ) );
                    ObjectInputStream ois = new ObjectInputStream( fis );
                    eeValueObjects.add( ois.readObject() );
                    ois.close();
                    fis.close();
                } else {
                    continue;
                }
            } catch ( IOException e ) {
                log.warn( "Unable to read report object for id =" + id, e );
                continue;
            } catch ( ClassNotFoundException e ) {
                log.warn( "Unable to read report object for id =" + id, e );
                continue;
            }
        }
        return eeValueObjects;
    }

    /**
     * @param eeValueObjects the collection of Expression Experiment value objects to serialize
     * @return true if successful, false otherwise serialize value objects
     */
    private boolean saveValueObjects( Collection eeValueObjects ) {
        for ( Object object : eeValueObjects ) {
            ExpressionExperimentValueObject eeVo = ( ExpressionExperimentValueObject ) object;

            try {
                // remove file first
                File f = new File( getReportPath( eeVo.getId() ) );
                if ( f.exists() ) {
                    f.delete();
                }
                FileOutputStream fos = new FileOutputStream( getReportPath( eeVo.getId() ) );
                ObjectOutputStream oos = new ObjectOutputStream( fos );
                oos.writeObject( eeVo );
                oos.flush();
                oos.close();
            } catch ( Throwable e ) {
                return false;
            }
        }
        return true;
    }

}
