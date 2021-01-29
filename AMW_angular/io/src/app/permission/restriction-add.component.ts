import { Component, Input, Output, EventEmitter, OnChanges, ChangeDetectorRef, AfterViewChecked } from '@angular/core';
import { Action } from './action';
import { Restriction } from './restriction';
import { RestrictionsCreation } from './restrictions-creation';
import { Permission } from './permission';
import { Resource } from '../resource/resource';
import { ResourceType } from '../resource/resource-type';
import * as _ from 'lodash';
import { Environment } from '@core/services';

@Component({
  selector: 'amw-restriction-add',
  templateUrl: './restriction-add.component.html',
})
export class RestrictionAddComponent implements OnChanges, AfterViewChecked {
  actions: Action[] = [
    { name: 'ALL' } as Action,
    { name: 'CREATE' } as Action,
    { name: 'DELETE' } as Action,
    { name: 'READ' } as Action,
    { name: 'UPDATE' } as Action,
  ];
  resourceTypePermissions: string[] = ['ANY', 'DEFAULT_ONLY', 'NON_DEFAULT_ONLY'];

  selectedPermissionNames: string[] = [];
  selectedResourceTypeNames: string[] = [];
  selectedResourceTypePermission: string = null;
  selectedResourceGroupNames: string[] = [];
  selectedContextNames: string[] = [];

  @Input() roleName: string;
  @Input() userNames: any[] = [];
  @Input() permissions: Permission[] = [];
  @Input() groupedEnvironments: { [key: string]: Environment[] } = {};
  @Input() resourceGroups: Resource[] = [];
  @Input() resourceTypes: ResourceType[] = [];
  @Input() availableRestrictions: Restriction[] = [];
  @Input() delegationMode: boolean = false;
  @Output() cancelEdit: EventEmitter<boolean> = new EventEmitter<boolean>();
  @Output() saveRestrictions: EventEmitter<RestrictionsCreation> = new EventEmitter<RestrictionsCreation>();

  similarRestrictions: Restriction[] = [];

  availableEnvironments: string[] = [];

  onlyGlobal: boolean = true;

  constructor(private cdRef: ChangeDetectorRef) {}

  ngAfterViewChecked() {
    // explicit change detection to avoid "expression-has-changed-after-it-was-checked-error"
    this.cdRef.detectChanges();
  }

  ngOnChanges() {
    if (!this.delegationMode) {
      this.deSelectAllEnvironments();
    }
  }

  getTitle(): string {
    return 'Add';
  }

  cancel() {
    this.cancelEdit.emit(true);
  }

  persistRestriction() {
    const restrictionsCreation: RestrictionsCreation = {
      roleName: this.roleName,
      userNames: this.getSelectedUserNames(),
      permissionNames: this.selectedPermissionNames,
      resourceGroupIds: this.getSelectedGroupIds(),
      resourceTypeNames: this.selectedResourceTypeNames,
      resourceTypePermission: this.selectedResourceTypePermission,
      contextNames: this.getSelectedEnvNames(),
      actions: this.getSelectedActionNames(),
    };
    this.saveRestrictions.emit(restrictionsCreation);
    this.resetSelections();
  }

  isValidForm(): boolean {
    return this.selectedPermissionNames.length > 0;
  }

  isResourceTypePermissionAssignable(): boolean {
    return this.selectedResourceTypeNames.length < 1 && this.selectedResourceGroupNames.length < 1;
  }

  isResourceTypeAssignable(): boolean {
    return (
      (this.selectedResourceTypePermission === 'ANY' || !this.selectedResourceTypePermission) &&
      this.selectedResourceGroupNames.length < 1
    );
  }

  isResourceGroupAssignable(): boolean {
    return (
      (this.selectedResourceTypePermission === 'ANY' || !this.selectedResourceTypePermission) &&
      this.selectedResourceTypeNames.length < 1
    );
  }

  clearTypeAndGroup() {
    if (this.selectedResourceTypePermission !== 'ANY') {
      this.selectedResourceTypeNames = [];
      this.selectedResourceGroupNames = [];
    }
  }

  defineAvailableOptions() {
    if (this.selectedPermissionNames.length > 0) {
      this.onlyGlobal = true;
      this.selectedPermissionNames.forEach((permissionName) => {
        if (this.onlyGlobal && !this.permissions.find((permission) => permission.name === permissionName).old) {
          this.onlyGlobal = false;
        }
      });
      if (this.onlyGlobal) {
        this.deSelectAllActions();
        this.selectedResourceTypeNames = [];
        this.selectedResourceTypePermission = 'ANY';
        this.selectedResourceGroupNames = [];
        this.selectedContextNames = [];
      } else if (this.delegationMode) {
        this.populateSimilarRestrictions();
        this.extractAvailableActions();
      }
    }
  }

  checkAvailableEnvironments() {
    if (this.delegationMode) {
      this.preSelectEnvironment();
    }
  }

  getAvailableActions(): Action[] {
    return this.delegationMode ? this.extractAvailableActions() : this.actions;
  }

  hasSelectedActions(): boolean {
    return _.some(this.actions, { selected: true });
  }

  getAvailableResourceGroups(): Resource[] {
    return this.delegationMode ? this.extractAvailableResourceGroups() : this.resourceGroups;
  }

  getAvailableResourceTypePermissions(): string[] {
    return this.delegationMode ? this.extractAvailableResourceTypePermissions() : this.resourceTypePermissions;
  }

  getAvailableResourceTypes(): ResourceType[] {
    return this.delegationMode ? this.extractAvailableResourceTypes() : this.resourceTypes;
  }

  checkUnique(env: Environment) {
    if (this.delegationMode) {
      if (env.selected) {
        this.selectedContextNames.push(env.name);
      } else {
        _.pull(this.selectedContextNames, env.name);
      }
    }
  }

  getEnvironmentGroups(): string[] {
    return Object.keys(this.groupedEnvironments);
  }

  private populateSimilarRestrictions() {
    this.similarRestrictions = [];
    this.selectedPermissionNames.forEach((permissionName) => {
      this.similarRestrictions.push(..._.filter(this.availableRestrictions, ['permission.name', permissionName]));
    });
  }

  private extractAvailableActions(): Action[] {
    this.disableAllActions();
    if (this.similarRestrictions.length > 0) {
      this.similarRestrictions.forEach((restriction) => {
        if (restriction.action === 'ALL') {
          this.enableAllActions();
        } else if (!_.some(this.actions, { name: restriction.action, disabled: false })) {
          this.actions[_.findIndex(this.actions, { name: restriction.action })].disabled = false;
        }
      });
    }
    this.actions.forEach((action) => {
      if (action.disabled && action.selected) {
        action.selected = false;
      }
    });
    return this.actions;
  }

  private preSelectEnvironment() {
    this.availableEnvironments = [];
    this.disableAllEnvironments();
    this.selectedResourceGroupNames = [];
    this.similarRestrictions.forEach((restriction) => {
      if (restriction.action === 'ALL' || _.some(this.actions, { name: restriction.action, selected: true })) {
        this.availableEnvironments.push(restriction.contextName);
        if (!restriction.contextName) {
          // null = All
          this.enableAllEnvironments();
        } else {
          this.enableEnvironment(restriction.contextName);
        }
      }
    });
    if (this.availableEnvironments.length === 1) {
      this.preSelectEnv(this.availableEnvironments[0]);
    }
  }

  private resetSelections() {
    this.deSelectAllActions();
    this.deSelectAllEnvironments();
    this.selectedPermissionNames = [];
    this.selectedResourceTypeNames = [];
    this.selectedResourceTypePermission = null;
    this.selectedResourceGroupNames = [];
    this.selectedContextNames = [];
  }

  private extractAvailableResourceGroups(): Resource[] {
    const groups: Resource[] = [];
    let addAll: boolean;
    if (this.similarRestrictions.length > 0) {
      this.similarRestrictions.forEach((restriction) => {
        if (
          !addAll &&
          (restriction.action === 'ALL' || _.some(this.actions, { name: restriction.action, selected: true })) &&
          (restriction.contextName === null ||
            this.selectedContextNames.indexOf(restriction.contextName) > -1 ||
            this.isChildContextOf(restriction.contextName, this.selectedContextNames))
        ) {
          if (restriction.resourceGroupId === null) {
            addAll = true;
          } else if (!_.some(groups, ['id', restriction.resourceGroupId])) {
            groups.push(_.find(this.resourceGroups, ['id', restriction.resourceGroupId]));
          }
        }
      });
    }
    if (addAll) {
      return this.resourceGroups;
    }
    if (groups.length === 1) {
      this.selectedResourceGroupNames = [groups[0].name];
    }
    return groups;
  }

  private extractAvailableResourceTypePermissions(): string[] {
    const resourceTypePermissions: string[] = [];
    let addAll: boolean;
    if (this.similarRestrictions.length > 0) {
      this.similarRestrictions.forEach((restriction) => {
        if (
          restriction.action === 'ALL' ||
          (_.some(this.actions, { name: restriction.action, selected: true }) &&
            restriction.resourceGroupId === null &&
            (restriction.contextName === null ||
              this.selectedContextNames.indexOf(restriction.contextName) > -1 ||
              this.isChildContextOf(restriction.contextName, this.selectedContextNames)))
        ) {
          if (!addAll && resourceTypePermissions.indexOf(restriction.resourceTypePermission) < 0) {
            if (restriction.resourceTypePermission === 'ANY') {
              addAll = true;
            } else {
              resourceTypePermissions.push(restriction.resourceTypePermission);
            }
          }
        }
      });
    }
    if (addAll) {
      return this.resourceTypePermissions;
    } else if (resourceTypePermissions.length === 1) {
      this.selectedResourceTypePermission = resourceTypePermissions[0];
    }
    return resourceTypePermissions;
  }

  private extractAvailableResourceTypes(): ResourceType[] {
    const resourceTypes: ResourceType[] = [];
    let addAll: boolean;
    if (this.similarRestrictions.length > 0) {
      this.similarRestrictions.forEach((restriction) => {
        if (
          restriction.action === 'ALL' ||
          (_.some(this.actions, { name: restriction.action, selected: true }) &&
            restriction.resourceGroupId === null &&
            (restriction.contextName === null ||
              this.selectedContextNames.indexOf(restriction.contextName) > -1 ||
              this.isChildContextOf(restriction.contextName, this.selectedContextNames)))
        ) {
          if (!addAll && restriction.resourceTypeName === null) {
            addAll = true;
          }
          if (!addAll && !_.some(resourceTypes, ['name', restriction.resourceTypeName])) {
            resourceTypes.push(_.find(this.resourceTypes, ['name', restriction.resourceTypeName]));
          }
        }
      });
    }
    if (addAll) {
      return this.resourceTypes;
    }
    return resourceTypes;
  }

  private deSelectAllEnvironments() {
    this.getEnvironmentGroups().forEach((group) => {
      this.groupedEnvironments[group].forEach((environment) => (environment.selected = false));
    });
  }

  private preSelectEnv(contextName: string) {
    this.deSelectAllEnvironments();
    this.getEnvironmentGroups().forEach((group) => {
      this.groupedEnvironments[group].forEach((environment) => {
        if (environment.name === contextName) {
          environment.selected = true;
          this.selectedContextNames = [contextName];
          return;
        }
      });
    });
  }

  private deSelectAllActions() {
    this.actions.forEach((action) => (action.selected = false));
  }

  private disableAllActions() {
    this.actions.forEach((action) => (action.disabled = true));
  }

  private enableAllActions() {
    this.actions.forEach((action) => (action.disabled = false));
  }

  private getSelectedUserNames(): string[] {
    const userNames: string[] = [];
    this.userNames.forEach((user) => {
      if (user.label) {
        userNames.push(user.label);
      } else {
        userNames.push(user);
      }
    });
    return userNames;
  }

  private getSelectedEnvNames(): string[] {
    const contextNames: string[] = [];
    this.getEnvironmentGroups().forEach((group) => {
      this.groupedEnvironments[group].forEach((environment) => {
        if (environment.selected === true) {
          contextNames.push(environment.name);
        }
      });
    });
    return contextNames;
  }

  private getSelectedActionNames(): string[] {
    const actionNames: string[] = [];
    this.actions.forEach((action) => {
      if (action.selected === true) {
        actionNames.push(action.name);
      }
    });
    if (actionNames.length === 0) {
      actionNames.push('ALL');
    }
    return actionNames;
  }

  private getSelectedGroupIds(): number[] {
    const groupIds: number[] = [];
    this.selectedResourceGroupNames.forEach((groupName) => {
      groupIds.push(this.resourceGroups.find((rg) => rg.name.toLowerCase() === groupName.toLowerCase()).id);
    });
    return groupIds;
  }

  private disableAllEnvironments() {
    this.getEnvironmentGroups().forEach((group) => {
      this.groupedEnvironments[group].forEach((environment) => (environment.disabled = true));
    });
  }

  private enableAllEnvironments() {
    this.getEnvironmentGroups().forEach((group) => {
      this.groupedEnvironments[group].forEach((environment) => (environment.disabled = false));
    });
  }

  private enableEnvironment(contextName: string) {
    this.getEnvironmentGroups().forEach((group) => {
      this.groupedEnvironments[group].forEach((environment) => {
        if (environment.name === contextName) {
          environment.disabled = false;
          return;
        }
      });
    });
  }

  private isChildContextOf(potentialParent: string, contextNames: string[]): boolean {
    let isParent: boolean = false;
    contextNames.forEach((contextName) => {
      if (!isParent && this.getParentContextName(contextName) === potentialParent) {
        isParent = true;
      }
    });
    return isParent;
  }

  private getParentContextName(contextName: string): string {
    const keys = this.getEnvironmentGroups();
    const len: number = keys.length;
    for (let i = 0; i < len; i++) {
      if (_.some(this.groupedEnvironments[keys[i]], ['name', contextName])) {
        return keys[i];
      }
    }
    return null;
  }
}
