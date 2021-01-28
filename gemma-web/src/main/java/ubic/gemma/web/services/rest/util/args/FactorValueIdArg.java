package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;

/**
 * Maps a long identifier to a {@link FactorValue}.
 * @author poirigui
 */
public class FactorValueIdArg extends FactorValueArg<Long> {
    @Override
    public FactorValue getPersistentObject( FactorValueService service ) {
        return service.load( this.value );
    }

    @Override
    public String getPropertyName( FactorValueService service ) {
        return "factorValueId";
    }
}
