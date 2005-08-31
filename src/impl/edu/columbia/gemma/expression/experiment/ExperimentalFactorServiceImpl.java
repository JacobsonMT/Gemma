/*
 * The Gemma project.
 * 
 * Copyright (c) 2005 Columbia University
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package edu.columbia.gemma.expression.experiment;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 * @see edu.columbia.gemma.expression.experiment.ExperimentalFactorService
 */
public class ExperimentalFactorServiceImpl extends
        edu.columbia.gemma.expression.experiment.ExperimentalFactorServiceBase {

    /**
     * @see edu.columbia.gemma.expression.experiment.ExperimentalFactorService#getAllExperimentalFactors()
     */
    protected java.util.Collection handleGetAllExperimentalFactors() throws java.lang.Exception {
        return this.getExperimentalFactorDao().loadAll();
    }

    /**
     * @see edu.columbia.gemma.expression.experiment.ExperimentalFactorService#saveExperimentalFactor(edu.columbia.gemma.expression.experiment.ExperimentalFactor)
     */
    protected void handleSaveExperimentalFactor(
            edu.columbia.gemma.expression.experiment.ExperimentalFactor experimentalFactor ) throws java.lang.Exception {
        this.getExperimentalFactorDao().create( experimentalFactor );
    }

}