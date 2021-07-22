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

package ch.puzzle.itc.mobiliar.presentation.deploy;

import lombok.Getter;

import javax.enterprise.event.Event;
import java.util.ArrayList;
import java.util.List;



/**
 */
public class Domain {

    @Getter
    private Event<DomainEvent> domainEvent;

    @Getter
    private String name;

    @Getter
    private List<String> selectedContextIds = new ArrayList<>();

    public Domain(String name, Event<DomainEvent> domainEvent) {
        this.name = name;
        this.domainEvent = domainEvent;
    }

    public void setSelectedContextIds(List<String> list){
        selectedContextIds = list;
        domainEvent.fire(new DomainEvent());
    }

    public void addSelectedContextIds(String s) {
        if(selectedContextIds == null){
            selectedContextIds = new ArrayList<>();
        }
        selectedContextIds.add(s);
    }
}
