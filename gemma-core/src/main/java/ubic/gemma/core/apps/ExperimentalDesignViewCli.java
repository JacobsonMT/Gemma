package ubic.gemma.core.apps;

import ubic.gemma.core.util.AbstractCLIContextCLI;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.EntityUtils;

import java.util.*;

/**
 * @author paul
 */
public class ExperimentalDesignViewCli extends AbstractCLIContextCLI {

    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.ANALYSIS;
    }

    @Override
    public String getCommandName() {
        return "viewExpDesigns";
    }

    @Override
    public String getShortDesc() {
        return "Dump a view of experimental design(s)";
    }

    @Override
    protected void buildOptions() {
    }

    @Override
    protected void processOptions() throws Exception {

    }

    @Override
    protected void doWork() throws Exception {
        ExperimentalDesignService eds = getBean( ExperimentalDesignService.class );

        ExpressionExperimentService ees = getBean( ExpressionExperimentService.class );
        Collection<ExpressionExperimentValueObject> experiments = ees.loadValueObjects(
                EntityUtils.getIds( ees.loadAll() ), false );

        Map<Long, ExpressionExperimentValueObject> ed2ee = new HashMap<>();

        for ( ExpressionExperimentValueObject expressionExperiment : experiments ) {
            ed2ee.put( expressionExperiment.getExperimentalDesign(), expressionExperiment );
        }

        Collection<ExperimentalDesign> designs = eds.loadAll();

        Map<Long, Long> factor2Design = new HashMap<>();

        Map<String, Map<String, Collection<FactorValueBasicValueObject>>> categoryMap = new TreeMap<>();

        for ( ExperimentalDesign experimentalDesign : designs ) {

            if ( !ed2ee.containsKey( experimentalDesign.getId() ) ) continue;

            for ( ExperimentalFactor factor : experimentalDesign.getExperimentalFactors() ) {

                factor2Design.put( factor.getId(), experimentalDesign.getId() );

                String category;
                if ( factor.getCategory() != null )
                    category = factor.getCategory().getValue();
                else
                    category = " ** NO CATEGORY ** ";

                if ( !categoryMap.containsKey( category ) ) {
                    categoryMap.put( category, new TreeMap<String, Collection<FactorValueBasicValueObject>>() );
                }

                for ( FactorValue f : factor.getFactorValues() ) {
                    if ( f.getMeasurement() != null ) continue; // don't list individual quantitative values.

                    if ( f.getCharacteristics().size() > 0 ) {
                        for ( Characteristic c : f.getCharacteristics() ) {
                            if ( c.getCategory().equals( category ) ) {

                                String value = c.getValue();

                                if ( value == null ) continue;

                                if ( !categoryMap.get( category ).containsKey( value ) ) {
                                    categoryMap.get( category ).put( value, new HashSet<FactorValueBasicValueObject>() );
                                }

                                categoryMap.get( category ).get( value ).add( new FactorValueBasicValueObject( f ) );
                            }
                        }
                    } else if ( f.getValue() != null ) {
                        if ( !categoryMap.get( category ).containsKey( f.getValue() ) ) {
                            categoryMap.get( category ).put( f.getValue(), new HashSet<FactorValueBasicValueObject>() );
                        }
                        categoryMap.get( category ).get( f.getValue() ).add( new FactorValueBasicValueObject( f ) );
                    }

                }
            }

        }

        for ( String category : categoryMap.keySet() ) {

            log.info( "Category: " + category );

            if ( category.equals( "Time" ) || category.equals( "SamplingTimePoint" ) || category.equals( "Age" ) ) {
                log.info( " *****  Details not shown for this category" );
            }

            for ( String value : categoryMap.get( category ).keySet() ) {

                log.info( "     Value: " + value );

                for ( FactorValueBasicValueObject fv : categoryMap.get( category ).get( value ) ) {
                    if ( fv.getMeasurement() != null ) continue; // don't list individual values.

                    Long factor = fv.getId();
                    ExpressionExperimentValueObject expressionExperimentValueObject = ed2ee.get( factor2Design
                            .get( factor ) );

                    if ( expressionExperimentValueObject == null ) {
                        log.warn( "       NO EE for Factor=" + factor );
                        continue;
                    }

                    log.info( "           " + fv.getFvSummary() );

                }
            }
        }
    }

}
