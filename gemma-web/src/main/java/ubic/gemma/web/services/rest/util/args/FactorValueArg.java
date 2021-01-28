package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;

/**
 * Represents an API arguments that maps to a {@link FactorValue}.
 * @author poirigui
 */
public abstract class FactorValueArg<A> extends MutableArg<A, FactorValue, FactorValueValueObject, FactorValueService> {
}
