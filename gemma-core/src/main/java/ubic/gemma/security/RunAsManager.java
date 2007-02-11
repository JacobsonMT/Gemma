/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.security;

import org.acegisecurity.Authentication;
import org.acegisecurity.ConfigAttributeDefinition;
import org.acegisecurity.SecurityConfig;
import org.acegisecurity.runas.RunAsManagerImpl;

/**
 * @author keshav
 * @version $Id$
 */
public class RunAsManager {

    /**
     * Run as the recipient.
     * 
     * @param object The target object.
     * @param authentication The authentication object.
     * @param recipient The user to run as.
     * @return Authentication
     */
    public Authentication buildRunAs( Object object, Authentication authentication, String recipient ) {
        RunAsManagerImpl runAsManager = new RunAsManagerImpl();
        runAsManager.setKey( recipient );
        ConfigAttributeDefinition attributeDefinition = new ConfigAttributeDefinition();
        attributeDefinition.addConfigAttribute( new SecurityConfig( "RUN_AS_" + recipient ) );
        return runAsManager.buildRunAs( authentication, object, attributeDefinition );
    }

}
