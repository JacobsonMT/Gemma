package ubic.gemma.tasks.analysis.expression;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.analysis.service.SampleRemoveService;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;

/**
 * Handle 'flagging' a sample as an outlier. The sample will not be used in analyses.
 *
 * @author paul
 * @version $Id$
 */
@Component
@Scope("prototype")
public class BioAssayOutlierProcessingTaskImpl implements BioAssayOutlierProcessingTask {

    @Autowired BioAssayService bioAssayService;
    @Autowired SampleRemoveService sampleRemoveService;

    private TaskCommand command;

    @Override
    public void setCommand( TaskCommand command ) {
        this.command = command;
    }

    @Override
    public TaskResult execute() {
        BioAssay bioAssay = bioAssayService.load( command.getEntityId() );
        if ( bioAssay == null ) {
            throw new RuntimeException( "BioAssay with id=" + command.getEntityId() + " not found" );
        }
        sampleRemoveService.markAsMissing( bioAssay );
        return new TaskResult( command, bioAssay );
    }

}
