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
package ubic.gemma.web.taglib.common.auditAndSecurity;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * @jsp.tag name="exception" body-content="empty"
 * @author pavlidis
 * @version $Id$
 */
public class ExceptionTag extends TagSupport {

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @SuppressWarnings("unused")
    @Override
    public int doEndTag() throws JspException {
        return EVAL_PAGE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException {
        final StringBuilder buf = new StringBuilder();
        if ( this.exception == null ) {
            buf.append( "Error was not recovered" );
        } else {
            buf.append( "<p>" );
            buf.append( exception.getMessage() );
            buf.append( "</p>" );
            buf
                    .append( "<textarea readonly=\"true\" class=\"stacktrace\" name=\"stacktrace\" rows=\"20\" columns=\"120\" >" );
            buf.append( ExceptionUtils.getFullStackTrace( exception ) );
            buf.append( "</textarea>" );
        }

        try {
            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            throw new JspException( "ContactTag: " + ex.getMessage() );
        }
        return SKIP_BODY;
    }

    Exception exception;

    /**
     * @jsp.attribute description="The exception" required="true" rtexprvalue="true"
     * @param exception
     */
    public void setException( Exception exception ) {
        this.exception = exception;
    }

}
