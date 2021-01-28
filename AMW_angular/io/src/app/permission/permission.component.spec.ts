import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, Subject } from 'rxjs';
import { ResourceService } from '../resource/resource.service';
import { Permission } from './permission';
import { PermissionComponent } from './permission.component';
import { PermissionService } from './permission.service';
import { RestrictionAddComponent } from './restriction-add.component';
import { RestrictionEditComponent } from './restriction-edit.component';
import { RestrictionListComponent } from './restriction-list.component';
import { Restriction } from './restriction';
import { Tag } from './tag';
import { SharedModule } from '@shared/shared.module';
import { NavigationStoreService } from '../navigation/navigation-store.service';
import { Environment } from '../deployments/deployment/environment';
import { EnvironmentService } from '../deployments/deployment/environment.service';

describe('PermissionComponent without any params (default: type Role)', () => {
  let fixture: ComponentFixture<PermissionComponent>;
  let component: PermissionComponent;

  let permissionService: PermissionService;
  let environmentService: EnvironmentService;
  let resourceService: ResourceService;

  let mockRoute: any = { snapshot: {} };
  mockRoute.params = new Subject<any>();
  mockRoute.queryParams = new Subject<any>();

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [CommonModule, FormsModule, HttpClientTestingModule, RouterTestingModule.withRoutes([])],
      providers: [
        EnvironmentService,
        PermissionService,
        ResourceService,
        NavigationStoreService,
        { provide: ActivatedRoute, useValue: mockRoute },
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
      declarations: [PermissionComponent, RestrictionEditComponent, RestrictionAddComponent, RestrictionListComponent],
    });

    fixture = TestBed.createComponent(PermissionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    permissionService = TestBed.inject(PermissionService);
    environmentService = TestBed.inject(EnvironmentService);
    resourceService = TestBed.inject(ResourceService);
  });

  it('should have default data', () => {
    // given when then
    expect(component.roleNames).toEqual([]);
    expect(component.userNames).toEqual([]);
    expect(component.permissions).toEqual([]);

    expect(component.environments).toEqual([
      {
        id: null,
        name: null,
        parent: 'All',
        selected: false,
      } as Environment,
    ]);
    expect(component.resourceGroups).toEqual([]);
    expect(component.resourceTypes).toEqual([{ id: null, name: null }]);
    expect(component.restrictionType).toEqual('role');
  });

  it('should invoke some services on ngOnInt', () => {
    // given
    const permissions: Permission[] = [
      { name: 'RESOURCE', old: false } as Permission,
      { name: 'RESOURCE_TYPE', old: false } as Permission,
    ];
    const environments: Environment[] = [
      { id: 1, name: 'U', parent: 'Dev' } as Environment,
      { id: 2, name: 'V', parent: 'Dev' } as Environment,
      { id: 3, name: 'T', parent: 'Test' } as Environment,
    ];
    spyOn(permissionService, 'getAllPermissionEnumValues').and.returnValue(of(permissions));
    spyOn(environmentService, 'getAllIncludingGroups').and.returnValue(of(environments));
    spyOn(resourceService, 'getAllResourceGroups').and.callThrough();
    spyOn(resourceService, 'getAllResourceTypes').and.callThrough();
    // when
    component.ngOnInit();
    mockRoute.params.next({ restrictionType: 'role' });

    // then
    expect(component.delegationMode).toBeFalsy();
    expect(component.restrictionType).toEqual('role');
    expect(permissionService.getAllPermissionEnumValues).toHaveBeenCalled();
    expect(environmentService.getAllIncludingGroups).toHaveBeenCalled();
    expect(resourceService.getAllResourceGroups).toHaveBeenCalled();
    expect(resourceService.getAllResourceTypes).toHaveBeenCalled();
    expect(component.permissions).toEqual(permissions);
    expect(component.restriction).toBeNull();
    expect(component.groupedEnvironments['All']).toContain({
      id: null,
      name: null,
      parent: 'All',
      selected: false,
    } as Environment);
    expect(component.groupedEnvironments['Dev']).toContain(
      { id: 1, name: 'U', parent: 'Dev', selected: false } as Environment,
      { id: 2, name: 'V', parent: 'Dev', selected: false } as Environment
    );
    expect(component.groupedEnvironments['Test']).toContain({
      id: 3,
      name: 'T',
      parent: 'Test',
      selected: false,
    } as Environment);
  });

  it('should invoke PermissionService and sort the Restrictions by Permission.name, action on changeRole if selected Role exists', () => {
    // given
    const restrictions: Restriction[] = [
      {
        id: 21,
        action: 'ALL',
        permission: { name: 'RESOURCE' } as Permission,
      } as Restriction,
      {
        id: 22,
        action: 'ALL',
        permission: { name: 'DEPLOYMENT' } as Permission,
      } as Restriction,
      {
        id: 23,
        action: 'CREATE',
        permission: { name: 'RESOURCE' } as Permission,
      } as Restriction,
    ];
    component.selectedRoleName = 'TESTER';
    component.roleNames = ['tester', 'role'];
    spyOn(permissionService, 'getRoleWithRestrictions').and.returnValue(of(restrictions));
    // when
    component.onChangeRole();
    // then
    expect(permissionService.getRoleWithRestrictions).toHaveBeenCalledWith('TESTER');
    expect(component.assignedRestrictions.length).toBe(3);
    expect(component.assignedRestrictions[0].id).toBe(22);
    expect(component.assignedRestrictions[1].id).toBe(21);
    expect(component.assignedRestrictions[2].id).toBe(23);
  });

  it('should trim selectedRoleName on changeRole', () => {
    // given
    component.selectedRoleName = ' TESTER ';
    // when
    component.onChangeRole();
    // then
    expect(component.selectedRoleName).toBe('TESTER');
  });

  it('should not invoke PermissionService on changeRole if selected Role does not exist', () => {
    // given
    component.assignedRestrictions = [{ id: 31 } as Restriction, { id: 32 } as Restriction];
    component.selectedRoleName = 'TESTER';
    component.roleNames = ['role'];
    spyOn(permissionService, 'getRoleWithRestrictions').and.callThrough();
    // when
    component.onChangeRole();
    // then
    expect(permissionService.getRoleWithRestrictions).not.toHaveBeenCalled();
    expect(component.assignedRestrictions).toEqual([]);
  });

  it('should convert and trim users (provided as string or Tag) to selectedUserNames on changeUser', () => {
    // given // when
    component.onChangeUser(['stringToBeTrimmed ', ' stringToBeTrimmedToo', { label: ' tagToBeTrimmed ' } as Tag]);
    // then
    expect(component.selectedUserNames[0]).toBe('stringToBeTrimmed');
    expect(component.selectedUserNames[1]).toBe('stringToBeTrimmedToo');
    expect(component.selectedUserNames[2]).toBe('tagToBeTrimmed');
  });

  it('should invoke PermissionService and sort the Restrictions by Permission.name, action on changeUser if selected User exists', () => {
    // given
    const restrictions: Restriction[] = [
      {
        id: 41,
        action: 'DELETE',
        permission: { name: 'SAME' } as Permission,
      } as Restriction,
      {
        id: 42,
        action: 'CREATE',
        permission: { name: 'SAME' } as Permission,
      } as Restriction,
    ];
    component.selectedUserNames = ['Tester'];
    component.userNames = ['tester', 'user'];
    spyOn(permissionService, 'getUserWithRestrictions').and.returnValue(of(restrictions));
    // when
    component.onChangeUser(['Tester']);
    // then
    expect(permissionService.getUserWithRestrictions).toHaveBeenCalledWith('Tester');
    expect(component.assignedRestrictions.length).toBe(2);
    expect(component.assignedRestrictions[0].id).toBe(42);
    expect(component.assignedRestrictions[1].id).toBe(41);
  });
  it('should invoke PermissionService and sort the Restrictions by Permission.name, action on changeUser if selected User (provided as Tag) exists', () => {
    // given
    const restrictions: Restriction[] = [
      {
        id: 41,
        action: 'DELETE',
        permission: { name: 'SAME' } as Permission,
      } as Restriction,
      {
        id: 42,
        action: 'CREATE',
        permission: { name: 'SAME' } as Permission,
      } as Restriction,
    ];
    component.selectedUserNames = ['Tester'];
    component.userNames = ['tester', 'user'];
    spyOn(permissionService, 'getUserWithRestrictions').and.returnValue(of(restrictions));
    // when
    component.onChangeUser([{ label: 'Tester' } as Tag]);
    // then
    expect(permissionService.getUserWithRestrictions).toHaveBeenCalledWith('Tester');
    expect(component.assignedRestrictions.length).toBe(2);
    expect(component.assignedRestrictions[0].id).toBe(42);
    expect(component.assignedRestrictions[1].id).toBe(41);
  });

  it('should not invoke PermissionService on changeUser if selected User does not exist', () => {
    // given
    component.assignedRestrictions = [{ id: 51 } as Restriction, { id: 52 } as Restriction];
    component.selectedUserNames = ['Tester'];
    component.userNames = ['user'];
    spyOn(permissionService, 'getUserWithRestrictions').and.callThrough();
    // when
    component.onChangeUser(['tester']);
    // then
    expect(permissionService.getUserWithRestrictions).not.toHaveBeenCalled();
    expect(component.assignedRestrictions).toEqual([]);
  });

  it('should not invoke PermissionService on changeUser if selected User (provided as Tag) does not exist', () => {
    // given
    component.assignedRestrictions = [{ id: 51 } as Restriction, { id: 52 } as Restriction];
    component.selectedUserNames = ['Tester'];
    component.userNames = ['user'];
    spyOn(permissionService, 'getUserWithRestrictions').and.callThrough();
    // when
    component.onChangeUser([{ label: 'Tester' } as Tag]);
    // then
    expect(permissionService.getUserWithRestrictions).not.toHaveBeenCalled();
    expect(component.assignedRestrictions).toEqual([]);
  });

  it('should invoke PermissionService on removeRestriction', () => {
    // given
    component.assignedRestrictions = [
      { id: 121, contextName: 'T' } as Restriction,
      { id: 122, contextName: 'B' } as Restriction,
    ];
    component.restriction = {
      id: 122,
      contextName: 'B',
    } as Restriction;
    spyOn(permissionService, 'removeRestriction').and.callThrough();
    // when
    component.removeRestriction(122);
    // then
    expect(permissionService.removeRestriction).toHaveBeenCalledWith(122);
    expect(component.assignedRestrictions).toContain({
      id: 121,
      contextName: 'T',
    } as Restriction);
  });

  it('should reset the Restriction and error message on cancel', () => {
    // given
    component.restriction = {
      id: 111,
      contextName: 'T',
      permission: { name: 'aPermission' },
    } as Restriction;
    component.backupRestriction = {
      id: 111,
      contextName: 'B',
      permission: { name: 'aPermission' },
    } as Restriction;
    component.errorMessage = 'Error';
    // when
    component.cancel();
    // then
    expect(component.restriction).toBeNull();
    expect(component.backupRestriction).toBeNull();
    expect(component.errorMessage).toBeNull();
  });

  it('should create a copy of the original Restriction on modifyRestriction', () => {
    // given
    expect(component.restriction).toBeNull();
    expect(component.backupRestriction).toBeNull();
    // when
    component.modifyRestriction({ id: 123 } as Restriction);
    // then
    expect(component.restriction).not.toBeNull();
    expect(component.restriction.id).toBe(123);
    expect(component.backupRestriction).not.toBeNull();
    expect(component.backupRestriction.id).toBe(123);
  });

  it('should invoke PermissionService.updatePermission on persistRestriction for an existing Restriction', () => {
    // given
    component.restriction = {
      id: 111,
      contextName: 'T',
    } as Restriction;
    spyOn(permissionService, 'updateRestriction').and.callThrough();
    // when
    component.persistRestriction();
    // then
    expect(permissionService.updateRestriction).toHaveBeenCalledWith(component.restriction);
  });

  it('should invoke PermissionService.createPermission on persistRestriction for a new Restriction', () => {
    // given
    component.restriction = {
      id: null,
      contextName: 'S',
    } as Restriction;
    spyOn(permissionService, 'createRestriction').and.callThrough();
    // when
    component.persistRestriction();
    // then
    expect(permissionService.createRestriction).toHaveBeenCalledWith(component.restriction, false);
  });

  it('should not set create to true and not initialize a Restriction on addRestriction if no User or Role is selected', () => {
    // given
    expect(component.create).toBeFalsy();
    expect(component.restriction).toBeNull();
    // when
    component.addRestriction();
    // then
    expect(component.create).toBeFalsy();
    expect(component.restriction).toBeNull();
  });

  it('should set create to true but not initialize a Restriction on addRestriction if a RoleName is selected', () => {
    // given
    component.selectedRoleName = 'Test';
    expect(component.create).toBeFalsy();
    expect(component.restriction).toBeNull();
    // when
    component.addRestriction();
    // then
    expect(component.create).toBeTruthy();
    expect(component.restriction).toBeNull();
  });

  it('should set create to true but not initialize a Restriction on addRestriction if a UserName is selected', () => {
    // given
    component.selectedUserNames = ['Tester'];
    expect(component.create).toBeFalsy();
    expect(component.restriction).toBeNull();
    // when
    component.addRestriction();
    // then
    expect(component.create).toBeTruthy();
    expect(component.restriction).toBeNull();
  });
});

describe('PermissionComponent with param restrictionType (type User)', () => {
  let fixture: ComponentFixture<PermissionComponent>;
  let component: PermissionComponent;

  let permissionService: PermissionService;
  let environmentService: EnvironmentService;
  let resourceService: ResourceService;

  let mockRoute: any = { snapshot: {} };
  mockRoute.params = new Subject<any>();
  mockRoute.queryParams = new Subject<any>();

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [CommonModule, FormsModule, HttpClientTestingModule, RouterTestingModule.withRoutes([]), SharedModule],
      providers: [
        EnvironmentService,
        PermissionService,
        ResourceService,
        NavigationStoreService,
        { provide: ActivatedRoute, useValue: mockRoute },
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
      declarations: [PermissionComponent, RestrictionEditComponent, RestrictionAddComponent, RestrictionListComponent],
    });
    fixture = TestBed.createComponent(PermissionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    permissionService = TestBed.inject(PermissionService);
    environmentService = TestBed.inject(EnvironmentService);
    resourceService = TestBed.inject(ResourceService);
  });

  it('should invoke some services on ngOnInt', () => {
    // given
    const permissions: Permission[] = [
      { name: 'RESOURCE', old: false } as Permission,
      { name: 'RESOURCE_TYPE', old: false } as Permission,
    ];
    const environments: Environment[] = [
      { id: 1, name: 'U', parent: 'Dev' } as Environment,
      { id: 2, name: 'V', parent: 'Dev' } as Environment,
      { id: 3, name: 'T', parent: 'Test' } as Environment,
    ];
    spyOn(permissionService, 'getAllPermissionEnumValues').and.returnValue(of(permissions));
    spyOn(environmentService, 'getAllIncludingGroups').and.returnValue(of(environments));
    spyOn(resourceService, 'getAllResourceGroups').and.callThrough();
    spyOn(resourceService, 'getAllResourceTypes').and.callThrough();
    spyOn(permissionService, 'getAllUserRestrictionNames').and.callThrough();
    // when
    component.ngOnInit();
    mockRoute.params.next({ restrictionType: 'user' });
    // then
    expect(component.delegationMode).toBeFalsy();
    expect(component.restrictionType).toEqual('user');
    expect(permissionService.getAllPermissionEnumValues).toHaveBeenCalled();
    expect(environmentService.getAllIncludingGroups).toHaveBeenCalled();
    expect(resourceService.getAllResourceGroups).toHaveBeenCalled();
    expect(resourceService.getAllResourceTypes).toHaveBeenCalled();
    expect(permissionService.getAllUserRestrictionNames).toHaveBeenCalled();
    expect(component.permissions).toEqual(permissions);
    expect(component.restriction).toBeNull();

    expect(component.groupedEnvironments['All']).toContain({
      id: null,
      name: null,
      parent: 'All',
      selected: false,
    } as Environment);
    expect(component.groupedEnvironments['Dev']).toContain(
      { id: 1, name: 'U', parent: 'Dev', selected: false } as Environment,
      { id: 2, name: 'V', parent: 'Dev', selected: false } as Environment
    );
    expect(component.groupedEnvironments['Test']).toContain({
      id: 3,
      name: 'T',
      parent: 'Test',
      selected: false,
    } as Environment);
  });
});

describe('PermissionComponent with param actingUser (delegation mode)', () => {
  let fixture: ComponentFixture<PermissionComponent>;
  let component: PermissionComponent;

  let permissionService: PermissionService;
  let environmentService: EnvironmentService;
  let resourceService: ResourceService;

  let mockRoute: any = { snapshot: {} };
  mockRoute.params = new Subject<any>();
  mockRoute.queryParams = new Subject<any>();

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [CommonModule, FormsModule, HttpClientTestingModule, RouterTestingModule.withRoutes([]), SharedModule],
      providers: [
        EnvironmentService,
        PermissionService,
        ResourceService,
        NavigationStoreService,
        { provide: ActivatedRoute, useValue: mockRoute },
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
      declarations: [PermissionComponent, RestrictionEditComponent, RestrictionAddComponent, RestrictionListComponent],
    });
    fixture = TestBed.createComponent(PermissionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    permissionService = TestBed.inject(PermissionService);
    environmentService = TestBed.inject(EnvironmentService);
    resourceService = TestBed.inject(ResourceService);
  });
  it('should invoke some services on ngOnInt', () => {
    // given
    const userNames: string[] = ['someUserName', 'anotherUserName', 'testUser'];
    const restrictions: Restriction[] = [
      {
        id: 1,
        action: 'CREATE',
        permission: { name: 'RESOURCE', old: false },
      } as Restriction,
      {
        id: 2,
        action: 'UPDATE',
        permission: { name: 'RESOURCE_TYPE', old: false },
      } as Restriction,
    ];
    const permissions: Permission[] = [
      { name: 'RESOURCE', old: false, longName: 'RESOURCE' },
      { name: 'RESOURCE_TYPE', old: false, longName: 'RESOURCE_TYPE' },
    ];
    const environments: Environment[] = [
      { id: 1, name: 'U', parent: 'Dev' } as Environment,
      { id: 2, name: 'V', parent: 'Dev' } as Environment,
      { id: 3, name: 'T', parent: 'Test' } as Environment,
    ];
    spyOn(permissionService, 'getAllUserRestrictionNames').and.returnValue(of(userNames));
    spyOn(permissionService, 'getOwnUserAndRoleRestrictions').and.returnValue(of(restrictions));
    spyOn(environmentService, 'getAllIncludingGroups').and.returnValue(of(environments));
    spyOn(resourceService, 'getAllResourceGroups').and.callThrough();
    spyOn(resourceService, 'getAllResourceTypes').and.callThrough();
    // when
    component.ngOnInit();
    mockRoute.params.next({ actingUser: 'testUser' });
    // then
    expect(component.delegationMode).toBeTruthy();
    expect(component.restrictionType).toEqual('user');
    expect(component.actingUserName).toEqual('testUser');
    expect(permissionService.getAllUserRestrictionNames).toHaveBeenCalled();
    expect(component.userNames.length).toEqual(2);
    expect(component.userNames).not.toContain('testUser');
    expect(environmentService.getAllIncludingGroups).toHaveBeenCalled();
    expect(resourceService.getAllResourceGroups).toHaveBeenCalled();
    expect(resourceService.getAllResourceTypes).toHaveBeenCalled();
    expect(permissionService.getOwnUserAndRoleRestrictions).toHaveBeenCalled();
    expect(component.assignableRestrictions).toEqual(restrictions);
    expect(component.assignablePermissions).toEqual(permissions);
    expect(component.restriction).toBeNull();
  });
});
