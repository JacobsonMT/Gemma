package edu.columbia.gemma;

import org.springframework.webflow.test.AbstractFlowExecutionTests;

import edu.columbia.gemma.util.SpringContextUtil;

public class BaseFlowTestCase extends AbstractFlowExecutionTests {

    protected String flowId() {
        return null;
    }

    protected String[] getConfigLocations() {
        return SpringContextUtil.getConfigLocations( true );
    }

}
