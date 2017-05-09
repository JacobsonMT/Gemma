/*
 * The Gemma project
 * 
 * Copyright (c) 2006-2012 University of British Columbia
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

package ubic.gemma.web.controller.coexpressionSearch;

import ubic.gemma.core.analysis.expression.coexpression.CoexpressionSearchCommand;
import ubic.gemma.core.job.TaskCommand;

/**
 * @author cmcdonald
 */
public class CoexSearchTaskCommand extends TaskCommand {

    private static final long serialVersionUID = 1L;

    CoexpressionSearchCommand searchOptions;

    public CoexSearchTaskCommand( CoexpressionSearchCommand searchOptions ) {
        super();
        this.searchOptions = searchOptions;
        this.setPersistJobDetails( false );
    }

    public CoexpressionSearchCommand getSearchOptions() {
        return searchOptions;
    }

    public void setSearchOptions( CoexpressionSearchCommand searchOptions ) {
        this.searchOptions = searchOptions;
    }
}
