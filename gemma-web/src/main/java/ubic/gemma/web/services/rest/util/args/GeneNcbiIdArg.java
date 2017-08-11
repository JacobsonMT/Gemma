package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.genome.Gene;

/**
 * Created by tesarst on 16/05/17.
 * Long argument type for Gene API, referencing the Gene NCBI ID.
 */
public class GeneNcbiIdArg extends GeneAnyIdArg<Integer> {

    private static final String ID_NAME = "NCBI ID";

    /**
     * @param l intentionally primitive type, so the value property can never be null.
     */
    GeneNcbiIdArg( int l ) {
        this.value = l;
        this.nullCause = getDefaultError();
    }

    @Override
    public Gene getPersistentObject( GeneService service ) {
        return service.findByNCBIId( this.value );
    }

    @Override
    String getIdentifierName() {
        return ID_NAME;
    }
}
