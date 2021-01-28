import { Component, OnInit, OnDestroy, AfterViewInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { PermissionService } from './permission.service';
import { Resource } from '../resource/resource';
import { ResourceType } from '../resource/resource-type';
import { ResourceService } from '../resource/resource.service';
import { Restriction } from './restriction';
import { RestrictionsCreation } from './restrictions-creation';
import { Permission } from './permission';
import * as _ from 'lodash';
import { Environment } from '../deployments/deployment/environment';
import { EnvironmentService } from '../deployments/deployment/environment.service';
import { NavigationStoreService } from '@core/services';

@Component({
  selector: 'amw-permission',
  templateUrl: './permission.component.html',
})
export class PermissionComponent implements OnInit, OnDestroy, AfterViewInit {
  // loaded only once
  roleNames: string[] = [];
  userNames: string[] = [];
  permissions: Permission[] = [];
  environments: Environment[] = [{ id: null, name: null, parent: 'All', selected: false } as Environment];
  groupedEnvironments: { [key: string]: Environment[] } = {
    All: [],
    Global: [],
  };
  resourceGroups: Resource[] = [];
  resourceTypes: ResourceType[] = [{ id: null, name: null }];

  defaultNavItem: string = 'Roles';
  // role | user
  restrictionType: string = 'role';
  delegationMode: boolean = false;
  assignedRestrictions: Restriction[] = [];
  selectedRoleName: string = null;
  selectedUserNames: string[] = [];
  actingUserName: string = null;
  assignableRestrictions: Restriction[] = [];
  assignablePermissions: Permission[] = [];

  // edit restriction
  restriction: Restriction = null;
  // backup for cancel
  backupRestriction: Restriction = null;
  // create new restrictions if true
  create: boolean = false;

  errorMessage: string = null;
  successMessage: string = null;
  isLoading: boolean = false;

  constructor(
    private permissionService: PermissionService,
    private environmentService: EnvironmentService,
    private resourceService: ResourceService,
    private activatedRoute: ActivatedRoute,
    public navigationStore: NavigationStoreService
  ) {
    this.navigationStore.setPageTitle('Permissions');
    this.navigationStore.setVisible(true);
    this.navigationStore.setItems([
      { title: 'Roles', target: '/permission/role' },
      { title: 'Users', target: '/permission/user' },
    ]);

    if (!['Roles', 'Users'].includes(this.navigationStore.navigation.current)) {
      this.navigationStore.setCurrent(this.defaultNavItem);
    }

    this.activatedRoute.params.subscribe((param: any) => {
      if (param['actingUser']) {
        this.delegationMode = true;
        this.restrictionType = 'user';
        this.onChangeActingUser(param['actingUser']);
      } else {
        if (param['restrictionType']) {
          this.restrictionType = param['restrictionType'];
        }
        this.delegationMode = false;
        this.getAllPermissions();
        this.onChangeType(this.restrictionType);
      }
      this.navigationStore.setCurrent(this.restrictionType === 'user' ? 'Users' : 'Roles');
    });
  }

  ngOnInit() {
    this.getAllEnvironments();
    this.getAllResourceGroups();
    this.getAllResourceTypes();
  }

  ngAfterViewInit(): void {}

  ngOnDestroy() {
    this.navigationStore.setItems([]);
  }

  onChangeRole() {
    this.selectedRoleName = this.selectedRoleName.trim();
    if (this.isExistingRole(this.selectedRoleName)) {
      this.getRoleWithRestrictions(this.selectedRoleName);
    } else {
      this.assignedRestrictions = [];
    }
    this.restriction = null;
  }

  onChangeUser(users: any) {
    this.convertToSelectedUserNames(users);
    if (this.selectedUserNames.length === 1 && this.isExistingUser(this.selectedUserNames[0])) {
      this.getUserWithRestrictions(this.selectedUserNames[0]);
    } else {
      this.assignedRestrictions = [];
    }
    this.restriction = null;
  }

  removeRestriction(id: number) {
    this.clearMessages();
    if (id) {
      this.permissionService.removeRestriction(id).subscribe(
        /* happy path */ (r) => '',
        /* error path */ (e) => (this.errorMessage = e),
        /* onComplete */ () => _.remove(this.assignedRestrictions, { id })
      );
    } else {
      this.restriction = null;
    }
  }

  cancel() {
    // reset restriction list, rollback to the last persisted state
    this.clearMessages();
    this.resetPermissionList();
    this.restriction = null;
    this.backupRestriction = null;
    this.create = false;
  }

  modifyRestriction(restriction: Restriction) {
    // reset restriction list, discard unsaved changes
    this.clearMessages();
    this.resetPermissionList();
    this.backupRestriction = { ...restriction };
    this.restriction = restriction;
  }

  persistRestriction() {
    this.clearMessages();
    this.isLoading = true;
    if (this.restriction.id != null) {
      this.permissionService.updateRestriction(this.restriction).subscribe(
        /* happy path */ (r) => '',
        /* error path */ (e) => (this.errorMessage = e),
        /* onComplete */ () => {
          this.updatePermissions(this.restriction);
          this.updateNamesLists();
          this.restriction = null;
          this.backupRestriction = null;
          this.isLoading = false;
          this.successMessage = 'Restriction updated successfully';
        }
      );
    } else {
      this.permissionService.createRestriction(this.restriction, this.delegationMode).subscribe(
        /* happy path */ (r) => (this.restriction = r),
        /* error path */ (e) => (this.errorMessage = e),
        /* onComplete */ () => {
          this.updatePermissions(this.restriction);
          this.updateNamesLists();
          this.restriction = null;
          this.isLoading = false;
          this.successMessage = 'Restriction created successfully';
        }
      );
    }
  }

  createRestrictions(restrictionsCreation: RestrictionsCreation) {
    this.clearMessages();
    this.isLoading = true;
    this.permissionService.createRestrictions(restrictionsCreation, this.delegationMode).subscribe(
      /* happy path */ (r) => '',
      /* error path */ (e) => (this.errorMessage = e),
      /* onComplete */ () => {
        this.create = false;
        this.updateExistingNamesLists(restrictionsCreation);
        this.reloadAssignedRestrictions(restrictionsCreation);
        this.isLoading = false;
        this.successMessage = 'Restriction(s) created successfully';
      }
    );
  }

  addRestriction() {
    this.clearMessages();
    if (this.selectedRoleName || this.selectedUserNames.length > 0) {
      this.create = true;
    }
  }

  getPermissions(): Permission[] {
    return this.delegationMode ? this.assignablePermissions : this.permissions;
  }

  private convertToSelectedUserNames(users: any) {
    this.selectedUserNames = [];
    users.forEach((user) => {
      if (user.label) {
        this.selectedUserNames.push(user.label.trim());
      } else {
        this.selectedUserNames.push(user.trim());
      }
    });
  }

  private reloadAssignedRestrictions(restrictionsCreation: RestrictionsCreation) {
    if (restrictionsCreation.roleName) {
      this.getRoleWithRestrictions(restrictionsCreation.roleName);
    } else if (restrictionsCreation.userNames.length === 1) {
      this.getUserWithRestrictions(restrictionsCreation.userNames[0]);
    }
  }

  private updateNamesLists() {
    if (this.restriction) {
      if (this.restriction.roleName && !this.isExistingRole(this.restriction.roleName)) {
        this.roleNames.push(this.restriction.roleName.toLowerCase());
      } else if (this.restriction.userName && !this.isExistingUser(this.restriction.userName)) {
        this.userNames.push(this.restriction.userName.toLowerCase());
      }
    }
  }

  private updateExistingNamesLists(restrictionsCreation: RestrictionsCreation) {
    if (restrictionsCreation.roleName && !this.isExistingRole(restrictionsCreation.roleName)) {
      this.roleNames.push(restrictionsCreation.roleName.toLowerCase());
    } else if (restrictionsCreation.userNames.length > 0) {
      restrictionsCreation.userNames.forEach((userName) => {
        if (!this.isExistingUser(userName)) {
          this.userNames.push(userName.toLowerCase());
        }
      });
    }
  }

  private isExistingRole(roleName: string) {
    return roleName !== null && this.roleNames.indexOf(roleName.toLowerCase()) > -1;
  }

  private isExistingUser(userName: string) {
    return userName !== null && this.userNames.indexOf(userName.toLowerCase()) > -1;
  }

  private onChangeType(type: string) {
    this.clearMessages();
    this.assignedRestrictions = [];
    this.restrictionType = type === 'role' || type === 'user' ? type : 'role';
    if (this.restrictionType === 'user') {
      this.selectedRoleName = null;
      if (this.userNames.length < 1) {
        this.getAllUserNames();
      }
    } else {
      this.selectedUserNames = [];
      if (this.roleNames.length < 1) {
        this.getAllRoleNames();
      }
    }
  }

  private onChangeActingUser(userName: string) {
    this.clearMessages();
    this.assignedRestrictions = [];
    this.actingUserName = userName;
    this.navigationStore.setItems([
      {
        title: this.actingUserName,
        target: '/permission/delegation/' + this.actingUserName,
      },
    ]);
    this.navigationStore.setCurrent(this.actingUserName);
    this.selectedUserNames = [];
    this.selectedRoleName = null;
    this.getAllAssignableUserNames();
    this.getAllAssignableRestrictions();
  }

  private getAllRoleNames() {
    this.isLoading = true;
    this.permissionService.getAllRoleNames().subscribe(
      /* happy path */ (r) => (this.roleNames = r),
      /* error path */ (e) => (this.errorMessage = e),
      /* onComplete */ () => (this.isLoading = false)
    );
  }

  private getAllUserNames() {
    this.isLoading = true;
    this.permissionService.getAllUserRestrictionNames().subscribe(
      /* happy path */ (r) => (this.userNames = r),
      /* error path */ (e) => (this.errorMessage = e),
      /* onComplete */ () => (this.isLoading = false)
    );
  }

  private getAllPermissions() {
    this.isLoading = true;
    this.permissionService.getAllPermissionEnumValues().subscribe(
      /* happy path */ (r) =>
        (this.permissions = _.sortBy(r, function (s: Permission) {
          return s.name.replace(/[_]/, '');
        })),
      /* error path */ (e) => (this.errorMessage = e),
      /* onComplete */ () => {
        this.markGlobalPermissions(this.permissions);
        this.isLoading = false;
      }
    );
  }

  private markGlobalPermissions(permissions: Permission[]) {
    permissions.forEach((permission) => {
      permission.longName = permission.old ? permission.name + ' (GLOBAL)' : permission.name;
    });
  }

  private getAllEnvironments() {
    this.isLoading = true;
    this.environmentService.getAllIncludingGroups().subscribe(
      /* happy path */ (r) => (this.environments = this.environments.concat(r)),
      /* error path */ (e) => (this.errorMessage = e),
      /* onComplete */ () => this.extractEnvironmentGroups()
    );
  }

  private getAllAssignableUserNames() {
    this.isLoading = true;
    this.permissionService.getAllUserRestrictionNames().subscribe(
      /* happy path */ (r) => (this.userNames = _.pull(r, this.actingUserName)),
      /* error path */ (e) => (this.errorMessage = e),
      /* onComplete */ () => (this.isLoading = false)
    );
  }

  private getAllAssignableRestrictions() {
    this.isLoading = true;
    this.permissionService.getOwnUserAndRoleRestrictions().subscribe(
      /* happy path */ (r) => (this.assignableRestrictions = r),
      /* error path */ (e) => (this.errorMessage = e),
      /* onComplete */ () => {
        this.extractAllAssignablePermissions();
        this.markGlobalPermissions(this.assignablePermissions);
        this.isLoading = false;
      }
    );
  }

  private extractAllAssignablePermissions() {
    this.assignablePermissions = [];
    this.assignableRestrictions.forEach((restriction) => {
      if (!_.some(this.assignablePermissions, restriction.permission)) {
        this.assignablePermissions.push(restriction.permission);
      }
    });
    this.assignablePermissions = _.sortBy(this.assignablePermissions, function (s: Permission) {
      return s.name.replace(/[_]/, '');
    });
  }

  private getAllResourceGroups() {
    this.isLoading = true;
    this.resourceService.getAllResourceGroups().subscribe(
      /* happy path */ (r) => (this.resourceGroups = r),
      /* error path */ (e) => (this.errorMessage = e),
      /* onComplete */ () => (this.isLoading = false)
    );
  }

  private getAllResourceTypes() {
    this.isLoading = true;
    this.resourceService.getAllResourceTypes().subscribe(
      /* happy path */ (r) => (this.resourceTypes = this.resourceTypes.concat(r)),
      /* error path */ (e) => (this.errorMessage = e),
      /* onComplete */ () => (this.isLoading = false)
    );
  }

  private getRoleWithRestrictions(roleName: string) {
    this.isLoading = true;
    this.permissionService.getRoleWithRestrictions(roleName).subscribe(
      /* happy path */ (r) => this.reorderRestrictions(r),
      /* error path */ (e) => (this.errorMessage = e),
      /* onComplete */ () => (this.isLoading = false)
    );
  }

  private getUserWithRestrictions(userName: string) {
    this.isLoading = true;
    this.permissionService.getUserWithRestrictions(userName).subscribe(
      /* happy path */ (r) => this.reorderRestrictions(r),
      /* error path */ (e) => (this.errorMessage = e),
      /* onComplete */ () => (this.isLoading = false)
    );
  }

  private resetPermissionList() {
    if (this.backupRestriction) {
      this.updatePermissions(this.backupRestriction);
    }
  }

  private updatePermissions(restriction: Restriction) {
    const i: number = _.findIndex(this.assignedRestrictions, _.pick(restriction, 'id'));
    if (i !== -1) {
      this.assignedRestrictions.splice(i, 1, restriction);
    } else {
      this.assignedRestrictions.push(restriction);
    }
    this.reorderRestrictions(this.assignedRestrictions);
  }

  private reorderRestrictions(restrictions: Restriction[]) {
    this.assignedRestrictions = _.sortBy(restrictions, [
      function (s: Restriction) {
        return s.permission.name.replace(/[_]/, '');
      },
      'action',
    ]);
  }

  private extractEnvironmentGroups() {
    this.environments.forEach((environment) => {
      environment.selected = false;
      if (!this.groupedEnvironments[environment['parent']]) {
        this.groupedEnvironments[environment['parent']] = [];
      }
      this.groupedEnvironments[environment['parent']].push(environment);
    });
    this.isLoading = false;
  }

  private clearMessages() {
    this.errorMessage = null;
    this.successMessage = null;
  }
}
