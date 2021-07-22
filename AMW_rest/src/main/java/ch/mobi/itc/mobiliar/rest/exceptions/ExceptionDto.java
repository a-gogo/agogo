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

package ch.mobi.itc.mobiliar.rest.exceptions;

import lombok.Data;

@Data
public class ExceptionDto {
	
	private String message;
	private String detail;
	
	public ExceptionDto(String message) {
		this(message, null);
	}
	
	public ExceptionDto(String message, String detail) {
		this.message = message;
		this.detail = detail;
	}
	
	public ExceptionDto(Throwable throwable) {		
		this.message = throwable.getMessage();
	}
	
	public ExceptionDto(Throwable throwable, String detail) {		
		this.message = throwable.getMessage();
		this.detail = detail;
	}

}
