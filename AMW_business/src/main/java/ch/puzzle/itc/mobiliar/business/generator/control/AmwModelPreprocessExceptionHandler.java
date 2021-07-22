/*
 * AMW - Automated Middleware allows you to manage the configurations of
 * your Java EE applications on an unlimited number of different environments
 * with various versions, including the automated deployment of those apps.
 * Copyright (C) 2013-2016 by Puzzle ITC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ch.puzzle.itc.mobiliar.business.generator.control;

import java.util.ArrayList;
import java.util.List;

import ch.puzzle.itc.mobiliar.common.exception.TemplatePropertyException;
import ch.puzzle.itc.mobiliar.common.exception.TemplatePropertyException.CAUSE;
import freemarker.core.Environment;
import freemarker.template.TemplateException;

/**
 * Custom exception handler for freemarker that does not throw the exception and therefore template processing
 * will be continued<br/>
 * See also http://freemarker.org/docs/pgui_config_errorhandling.html
 * 
 */
public class AmwModelPreprocessExceptionHandler extends AMWTemplateExceptionHandler {

	@Override
	public void handleTemplateException(TemplateException te, Environment env, java.io.Writer out) throws TemplateException {
		errorMessages.add(new TemplatePropertyException(te.getMessage(),	CAUSE.INVALID_PROPERTY));
	}

	/**
	 * @return list with error message strings
	 */
	public List<TemplatePropertyException> getErrorMessages() {
		return errorMessages;
	}

	public void addTemplatePropertyException(TemplatePropertyException e) {
		errorMessages.add(e);
	}

	public boolean isSuccess() {
		return errorMessages.isEmpty();
	}

	/**
	 * Reset the Handler
	 */
	public void reset() {
		errorMessages = new ArrayList<TemplatePropertyException>();
	}

}
