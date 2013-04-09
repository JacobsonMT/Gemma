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
package ubic.gemma.web.feed;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author sshao
 *
 */
public class TwitterOutboundTest extends BaseSpringContextTest {
	@Autowired TwitterOutbound twitterOutbound;
	@Autowired ExpressionExperimentService experimentService;
	
	@Before
	public void setup() {
	    ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setDescription( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) );
        ee.setName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) );
        
        experimentService.create( ee );
	}
	
	@Test
	public void testTweetLength() {
		String status = twitterOutbound.generateDailyFeed();
		assertNotNull(status);
		assertTrue((status.length() <= 140));
	}
}
