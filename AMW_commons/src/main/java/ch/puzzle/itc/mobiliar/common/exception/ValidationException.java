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

package ch.puzzle.itc.mobiliar.common.exception;


import lombok.Getter;

/**
 * Thrown it validation of an object/resource fails (string validation, business logic validation, etc.).
 */
public class ValidationException extends AMWException {

    /**
     * Use the causing object to pass information of failure
     */
    @Getter
    private Object causingObject;

    public ValidationException(String message) {
        this(message, null);
    }

    public ValidationException(String message, Object causingObject) {
        super(message);
        this.causingObject = causingObject;
    }

    public boolean hasCausingObject(){
        return causingObject != null;
    }
}
