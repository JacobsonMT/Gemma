/*
 * The Gemma project
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

package ubic.gemma.model.expression.experiment;

import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.core.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.core.testing.BaseSpringContextTest;

/**
 * @author kelsey
 * @version $Id$
 */
public class ExpressionExperimentDeleteTest extends BaseSpringContextTest {

    @Autowired
    private ExpressionExperimentService svc;

    @Autowired
    BioAssayService bioAssayService;

    @Test
    public final void testRemove()   {
        ExpressionExperiment ee = getTestPersistentCompleteExpressionExperiment( false );
        List<Long> ids = new ArrayList<Long>();
        for ( BioAssay ba : ee.getBioAssays() ) {
            ids.add( ba.getId() );
        }

        svc.delete( ee );

        assertNull( svc.load( ee.getId() ) );

        // sure bioassays are gone.
        for ( Long id : ids ) {
            BioAssay ba = bioAssayService.load( id );
            assertNull( ba );
        }
    }

}
