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
package ubic.gemma.util;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.ui.velocity.VelocityEngineUtils;

/**
 * From Appfuse.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class MailEngine {
    protected static final Log log = LogFactory.getLog( MailEngine.class );
    private MailSender mailSender;
    private VelocityEngine velocityEngine;

    /**
     * @param mailSender
     */
    public void setMailSender( MailSender mailSender ) {
        this.mailSender = mailSender;
    }

    /**
     * @param velocityEngine
     */
    public void setVelocityEngine( VelocityEngine velocityEngine ) {
        this.velocityEngine = velocityEngine;
    }

    /**
     * @param msg
     * @param templateName
     * @param model
     */
    public void sendMessage( SimpleMailMessage msg, String templateName, Map model ) {
        String result = null;

        try {
            result = VelocityEngineUtils.mergeTemplateIntoString( velocityEngine, templateName, model );
        } catch ( VelocityException e ) {
            e.printStackTrace();
        }

        msg.setText( result );
        send( msg );
    }

    /**
     * @param msg
     */
    public void send( SimpleMailMessage msg ) {
        try {
            mailSender.send( msg );
        } catch ( MailException ex ) {
            // log it and go on
            log.error( ex.getMessage() );
        }
    }
}
