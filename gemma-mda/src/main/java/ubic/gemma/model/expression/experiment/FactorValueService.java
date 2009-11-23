/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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

import java.util.Collection;

import org.springframework.security.access.annotation.Secured;

/**
 * @author kelsey
 * @version $Id$
 */
public interface FactorValueService {

    @Secured( { "GROUP_USER" })
    public Collection<FactorValue> create( Collection<FactorValue> factors );

    /**
     * @param factorValue
     * @return
     */
    @Secured( { "GROUP_USER" })
    public ubic.gemma.model.expression.experiment.FactorValue create(
            ubic.gemma.model.expression.experiment.FactorValue factorValue );

    /**
     * @param factorValue
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void delete( ubic.gemma.model.expression.experiment.FactorValue factorValue );

    /**
     * @param valuePrefix
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<FactorValue> findByValue( String valuePrefix );

    /**
     * @param factorValue
     * @return
     */
    @Secured( { "GROUP_USER", "AFTER_ACL_READ" })
    public ubic.gemma.model.expression.experiment.FactorValue findOrCreate(
            ubic.gemma.model.expression.experiment.FactorValue factorValue );

    /**
     * @param id
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ubic.gemma.model.expression.experiment.FactorValue load( java.lang.Long id );

    /**
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<FactorValue> loadAll();

    /**
     * @param factorValues
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_COLLECTION_EDIT" })
    public void update( java.util.Collection<FactorValue> factorValues );

    /**
     * @param factorValue
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void update( ubic.gemma.model.expression.experiment.FactorValue factorValue );

}
