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

package ch.puzzle.itc.mobiliar.presentation.deploymentparameter;

import ch.puzzle.itc.mobiliar.business.deploymentparameter.boundary.DeploymentParameterBoundary;
import ch.puzzle.itc.mobiliar.business.deploymentparameter.entity.Key;
import ch.puzzle.itc.mobiliar.business.security.boundary.PermissionBoundary;
import ch.puzzle.itc.mobiliar.business.security.entity.Permission;
import ch.puzzle.itc.mobiliar.common.exception.ValidationException;
import ch.puzzle.itc.mobiliar.presentation.util.GlobalMessageAppender;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

@Named
@RequestScoped
public class DeploymentParameterView {

    @Inject
    DeploymentParameterBoundary deploymentParameterBoundary;

    @Inject
    PermissionBoundary permissionBoundary;

    @Getter
    @Setter
    private String deployParameterKeyName;

    private List<Key> allKeys = new ArrayList<>();

    @PostConstruct
    public void init() {
        refresh();
    }

    private void refresh() {
        this.allKeys = deploymentParameterBoundary.findAllKeys();
        this.deployParameterKeyName = null;
    }

    public List<Key> getAllKeys() {
        return allKeys;
    }

    public void deleteKey(Key keyToRemove) {
        try {
            deploymentParameterBoundary.deleteDeployParameterKey(keyToRemove);
            GlobalMessageAppender.addSuccessMessage("Deployment parameter key " + keyToRemove.getName() + " successfully deleted");
            refresh();
        } catch (RuntimeException e) {
            GlobalMessageAppender.addErrorMessage("Could not delete deployment parameter key " + keyToRemove.getName());
        }
    }

    public void createNewKey() {
        try {
            deploymentParameterBoundary.createDeployParameterKey(deployParameterKeyName);
            GlobalMessageAppender.addSuccessMessage("Deployment parameter key successfully created");
            refresh();
        } catch (ValidationException | RuntimeException e) {
            GlobalMessageAppender.addErrorMessage("Could not create new deployment parameter key " + deployParameterKeyName + " Reason: " + e.getMessage());
        }
    }

    public boolean canManageDeploymentPropertyKeys() {
        return permissionBoundary.hasPermission(Permission.MANAGE_DEPLOYMENT_PARAMETER);
    }
}
