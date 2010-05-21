/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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

package ubic.gemma.tasks.analysis.expression;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.analysis.service.SampleRemoveService;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskMethod;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;

/**
 * @author paul
 * @version $Id$
 */
@Service
public class BioAssayOutlierProcessingTaskImpl implements BioAssayOutlierProcessingTask {

    @Autowired
    BioAssayService bioAssayService;

    @Autowired
    SampleRemoveService sampleRemoveService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.tasks.analysis.expression.BioAssayOutlierProcessingTask#execute(ubic.gemma.job.TaskCommand)
     */
    @TaskMethod
    public TaskResult execute( TaskCommand command ) {
        BioAssay bioAssay = bioAssayService.load( command.getEntityId() );
        if ( bioAssay == null ) {
            throw new RuntimeException( "BioAssay with id=" + command.getEntityId() + " not found" );
        }
        sampleRemoveService.markAsMissing( bioAssay );
        return new TaskResult( command, bioAssay );
    }

}
