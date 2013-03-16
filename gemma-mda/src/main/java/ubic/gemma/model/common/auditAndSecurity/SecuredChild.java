/*
 * The Gemma_sec1 project
 * 
 * Copyright (c) 2009 University of British Columbia
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
package ubic.gemma.model.common.auditAndSecurity;

/**
 * Indicates a securable that must have a parent that holds the permissons. For example, BioAssays are given the same
 * permissions as the holding Experiment, and no object should have the BioAssay's ACL as its parent.
 * 
 * @author paul
 * @version $Id$
 */
public interface SecuredChild extends Securable {

    public Securable getSecurityOwner();

}
