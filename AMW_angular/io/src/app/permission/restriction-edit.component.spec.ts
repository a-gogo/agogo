import { inject, TestBed } from '@angular/core/testing';
import { RestrictionEditComponent } from './restriction-edit.component';
import { Restriction } from './restriction';
import { Resource } from '../resource/resource';
import { Permission } from './permission';
import { ChangeDetectorRef } from '@angular/core';
import { Environment } from '../deployments/deployment/environment';

describe('RestrictionEditComponent', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEach(() =>
    TestBed.configureTestingModule({
      providers: [RestrictionEditComponent, ChangeDetectorRef],
    })
  );

  it('should preSelect the right Environment on ngOnChanges', inject(
    [RestrictionEditComponent],
    (restrictionComponent: RestrictionEditComponent) => {
      // given
      const emptyEnvironment: Environment[] = [
        {
          id: null,
          name: null,
          nameAlias: null,
          parent: 'All',
          selected: false,
          disabled: false,
        },
      ];
      const devEnvironments: Environment[] = [
        {
          id: 1,
          name: 'B',
          nameAlias: 'Test',
          parent: 'Dev',
          selected: false,
          disabled: false,
        },
        {
          id: 2,
          name: 'C',
          nameAlias: null,
          parent: 'Dev',
          selected: false,
          disabled: false,
        },
      ];
      restrictionComponent.groupedEnvironments = {
        All: emptyEnvironment,
        Dev: devEnvironments,
      };
      restrictionComponent.restriction = { contextName: 'C' } as Restriction;
      // when
      restrictionComponent.ngOnChanges();
      // then
      expect(restrictionComponent.groupedEnvironments['All'][0]['selected']).toBeFalsy();
      expect(restrictionComponent.groupedEnvironments['Dev'][0]['selected']).toBeFalsy();
      expect(restrictionComponent.groupedEnvironments['Dev'][1]['selected']).toBeTruthy();
    }
  ));

  it('should return the right title', inject(
    [RestrictionEditComponent],
    (restrictionComponent: RestrictionEditComponent) => {
      // given
      restrictionComponent.restriction = { id: 1 } as Restriction;
      // when then
      expect(restrictionComponent.getTitle()).toBe('Edit');
    }
  ));

  it('should return false if ResourceGroup has a name which is not available', inject(
    [RestrictionEditComponent],
    (restrictionComponent: RestrictionEditComponent) => {
      // given
      restrictionComponent.resourceGroups = [{ id: 21, name: 'Test' } as Resource];
      restrictionComponent.resourceGroup = {
        id: null,
        name: 'West',
      } as Resource;
      // when then
      expect(restrictionComponent.checkGroup()).toBeFalsy();
    }
  ));

  it('should return true if ResourceGroup has a name which is available', inject(
    [RestrictionEditComponent],
    (restrictionComponent: RestrictionEditComponent) => {
      // given
      restrictionComponent.resourceGroups = [
        { id: 21, name: 'Test' } as Resource,
        { id: 42, name: 'Rest' } as Resource,
      ];
      restrictionComponent.resourceGroup = {
        id: null,
        name: 'rest',
      } as Resource;
      restrictionComponent.restriction = {} as Restriction;
      // when then
      expect(restrictionComponent.checkGroup()).toBeTruthy();
    }
  ));

  it('should return invalid if ResourceType is not available', inject(
    [RestrictionEditComponent],
    (restrictionComponent: RestrictionEditComponent) => {
      // given
      restrictionComponent.resourceTypes = [
        { id: 1, name: 'APP' },
        { id: 2, name: 'APPSERVER' },
      ];
      restrictionComponent.restriction = {
        resourceTypeName: 'INVALID',
      } as Restriction;
      // when then
      expect(restrictionComponent.isValidForm()).toBeFalsy();
    }
  ));

  it('should return valid if ResourceType is available', inject(
    [RestrictionEditComponent],
    (restrictionComponent: RestrictionEditComponent) => {
      // given
      restrictionComponent.resourceTypes = [
        { id: 1, name: 'APP' },
        { id: 2, name: 'APPSERVER' },
      ];
      restrictionComponent.restriction = {
        resourceTypeName: 'APPSERVER',
      } as Restriction;
      // when then
      expect(restrictionComponent.isValidForm()).toBeTruthy();
    }
  ));

  it('should set ResourceTypeName to null if its value is empty', inject(
    [RestrictionEditComponent],
    (restrictionComponent: RestrictionEditComponent) => {
      // given
      restrictionComponent.restriction = {
        resourceTypeName: '',
      } as Restriction;
      // when
      restrictionComponent.persistRestriction();
      // then
      expect(restrictionComponent.restriction.resourceTypeName).toBeNull();
    }
  ));

  it('should preserve Restriction values on defineAvailableOptions if selected Permission is not old (not global)', inject(
    [RestrictionEditComponent],
    (restrictionComponent: RestrictionEditComponent) => {
      // given
      restrictionComponent.restriction = {
        action: 'CREATE',
        contextName: 'T',
        resourceGroupId: 9,
        resourceTypeName: null,
        resourceTypePermission: 'ANY',
        permission: { name: 'NEO' },
      } as Restriction;
      restrictionComponent.permissions = [
        { name: 'NEO', old: false } as Permission,
        { name: 'OLD_GLOBAL', old: true } as Permission,
      ];
      // when
      restrictionComponent.defineAvailableOptions();
      // then
      expect(restrictionComponent.restriction.action).toBe('CREATE');
      expect(restrictionComponent.restriction.contextName).toBe('T');
      expect(restrictionComponent.restriction.resourceGroupId).toBe(9);
      expect(restrictionComponent.restriction.resourceTypeName).toBeNull();
      expect(restrictionComponent.restriction.resourceTypePermission).toBe('ANY');
    }
  ));

  it('should reset Restriction values on defineAvailableOptions if selected Permission is old (global)', inject(
    [RestrictionEditComponent],
    (restrictionComponent: RestrictionEditComponent) => {
      // given
      restrictionComponent.restriction = {
        action: 'CREATE',
        contextName: 'T',
        resourceGroupId: 9,
        resourceTypeName: null,
        resourceTypePermission: 'ANY',
        permission: { name: 'OLD_GLOBAL' } as Permission,
      } as Restriction;
      restrictionComponent.permissions = [
        { name: 'NEO', old: false } as Permission,
        { name: 'OLD_GLOBAL', old: true } as Permission,
      ];
      // when
      restrictionComponent.defineAvailableOptions();
      // then
      expect(restrictionComponent.restriction.action).toBe('ALL');
      expect(restrictionComponent.restriction.contextName).toBeNull();
      expect(restrictionComponent.restriction.resourceGroupId).toBeNull();
      expect(restrictionComponent.restriction.resourceTypeName).toBeNull();
      expect(restrictionComponent.restriction.resourceTypePermission).toBe('ANY');
    }
  ));

  it('should prepare available options on defineAvailableOptions if in delegationMode and selected Permission is not old (not global)', inject(
    [RestrictionEditComponent],
    (restrictionComponent: RestrictionEditComponent) => {
      // given
      restrictionComponent.delegationMode = true;
      restrictionComponent.restriction = {
        action: 'CREATE',
        contextName: 'T',
        resourceGroupId: 9,
        resourceTypeName: null,
        resourceTypePermission: 'ANY',
        permission: { name: 'NEO' } as Permission,
      } as Restriction;
      restrictionComponent.permissions = [
        { name: 'NEO', old: false } as Permission,
        { name: 'OLD_GLOBAL', old: true } as Permission,
      ];
      restrictionComponent.availableRestrictions = [
        {
          action: 'CREATE',
          contextName: 'T',
          resourceGroupId: 9,
          resourceTypeName: null,
          resourceTypePermission: 'ANY',
          permission: { name: 'NEO' } as Permission,
        } as Restriction,
        {
          action: 'UPDATE',
          contextName: 'S',
          resourceGroupId: 10,
          resourceTypeName: null,
          resourceTypePermission: 'ANY',
          permission: { name: 'NEO' } as Permission,
        } as Restriction,
        {
          action: 'UPDATE',
          contextName: 'T',
          resourceGroupId: 11,
          resourceTypeName: null,
          resourceTypePermission: 'ANY',
          permission: { name: 'nada' } as Permission,
        } as Restriction,
      ];
      // when
      restrictionComponent.defineAvailableOptions();
      // then
      expect(restrictionComponent.similarRestrictions.length).toEqual(2);
      expect(restrictionComponent.similarRestrictions[0].permission.name).toBe('NEO');
      expect(restrictionComponent.similarRestrictions[1].permission.name).toBe('NEO');
      expect(restrictionComponent.restriction.contextName).toBeNull();
      expect(restrictionComponent.restriction.resourceGroupId).toBeNull();
      expect(restrictionComponent.restriction.resourceTypeName).toBeNull();
      expect(restrictionComponent.restriction.resourceTypePermission).toBeNull();
    }
  ));

  it('should return filtered resource groups on getAvailableResourceGroups if in delegationMode', inject(
    [RestrictionEditComponent],
    (restrictionComponent: RestrictionEditComponent) => {
      // given
      restrictionComponent.delegationMode = true;
      const emptyEnvironment: Environment[] = [{ id: null, name: null, parent: 'All' } as Environment];
      const devEnvironments: Environment[] = [
        { id: 1, name: 'B', parent: 'Dev' } as Environment,
        { id: 2, name: 'C', parent: 'Dev' } as Environment,
      ];
      const prodEnvironments: Environment[] = [
        { id: 12, name: 'P', parent: 'Dev' } as Environment,
        { id: 22, name: 'S', parent: 'Dev' } as Environment,
      ];
      restrictionComponent.groupedEnvironments = {
        All: emptyEnvironment,
        Dev: devEnvironments,
        Pro: prodEnvironments,
      };
      restrictionComponent.resourceGroups = [{ id: 1 } as Resource, { id: 9 } as Resource, { id: 10 } as Resource];
      // should match parent Dev
      restrictionComponent.restriction = {
        action: 'CREATE',
        contextName: 'B',
        resourceGroupId: 9,
        resourceTypeName: null,
        resourceTypePermission: 'ALL',
        permission: { name: 'NEO' } as Permission,
      } as Restriction;
      restrictionComponent.similarRestrictions = [
        {
          action: 'CREATE',
          contextName: 'Dev',
          resourceGroupId: 9,
          resourceTypeName: null,
          resourceTypePermission: 'ANY',
          permission: { name: 'NEO' } as Permission,
        } as Restriction,
        {
          action: 'CREATE',
          contextName: 'P',
          resourceGroupId: 10,
          resourceTypeName: null,
          resourceTypePermission: 'ANY',
          permission: { name: 'NEO' } as Permission,
        } as Restriction,
      ];
      // when
      const groups = restrictionComponent.getAvailableResourceGroups();
      // then
      expect(groups.length).toEqual(1);
      expect(groups[0].id).toEqual(9);
    }
  ));

  it('should return all resource groups on getAvailableResourceGroups if not in delegationMode', inject(
    [RestrictionEditComponent],
    (restrictionComponent: RestrictionEditComponent) => {
      // given
      restrictionComponent.resourceGroups = [{ id: 1 } as Resource, { id: 9 } as Resource, { id: 10 } as Resource];
      restrictionComponent.restriction = {
        action: 'CREATE',
        contextName: 'T',
        resourceGroupId: 9,
        resourceTypeName: null,
        resourceTypePermission: 'ALL',
        permission: { name: 'NEO' } as Permission,
      } as Restriction;
      // when
      const groups = restrictionComponent.getAvailableResourceGroups();
      // then
      expect(groups.length).toEqual(3);
    }
  ));

  it('should return filtered actions on getAvailableActions if in delegationMode', inject(
    [RestrictionEditComponent],
    (restrictionComponent: RestrictionEditComponent) => {
      // given
      restrictionComponent.delegationMode = true;
      restrictionComponent.restriction = {
        action: 'CREATE',
        contextName: 'T',
        resourceGroupId: 9,
        resourceTypeName: null,
        resourceTypePermission: 'ALL',
        permission: { name: 'NEO' } as Permission,
      } as Restriction;
      restrictionComponent.similarRestrictions = [
        {
          action: 'CREATE',
          contextName: 'T',
          resourceGroupId: 9,
          resourceTypeName: null,
          resourceTypePermission: 'ANY',
          permission: { name: 'NEO' } as Permission,
        } as Restriction,
        {
          action: 'READ',
          contextName: 'S',
          resourceGroupId: 10,
          resourceTypeName: null,
          resourceTypePermission: 'ANY',
          permission: { name: 'NEO' } as Permission,
        } as Restriction,
      ];
      // when
      const actions = restrictionComponent.getAvailableActions();
      // then
      expect(actions.length).toEqual(2);
    }
  ));

  it('should return all actions on getAvailableActions if in delegationMode and one of the similar restrictions has action ALL', inject(
    [RestrictionEditComponent],
    (restrictionComponent: RestrictionEditComponent) => {
      // given
      restrictionComponent.delegationMode = true;
      restrictionComponent.restriction = {
        action: 'CREATE',
        contextName: 'T',
        resourceGroupId: 9,
        resourceTypeName: null,
        resourceTypePermission: 'ALL',
        permission: { name: 'NEO' } as Permission,
      } as Restriction;
      restrictionComponent.similarRestrictions = [
        {
          action: 'CREATE',
          contextName: 'T',
          resourceGroupId: 9,
          resourceTypeName: null,
          resourceTypePermission: 'ANY',
          permission: { name: 'NEO' } as Permission,
        } as Restriction,
        {
          action: 'ALL',
          contextName: 'S',
          resourceGroupId: 10,
          resourceTypeName: null,
          resourceTypePermission: 'ANY',
          permission: { name: 'NEO' } as Permission,
        } as Restriction,
      ];
      // when
      const actions = restrictionComponent.getAvailableActions();
      // then
      expect(actions.length).toEqual(5);
    }
  ));

  it('should return all actions on getAvailableActions if not in delegationMode', inject(
    [RestrictionEditComponent],
    (restrictionComponent: RestrictionEditComponent) => {
      // given // when
      const actions = restrictionComponent.getAvailableActions();
      // then
      expect(actions.length).toEqual(5);
    }
  ));

  it('should return filtered resource types on getAvailableResourceTypes if in delegationMode', inject(
    [RestrictionEditComponent],
    (restrictionComponent: RestrictionEditComponent) => {
      // given
      restrictionComponent.delegationMode = true;
      restrictionComponent.resourceTypes = [
        { id: 1, name: 'APP' },
        { id: 2, name: 'AS' },
        { id: 3, name: 'FOO' },
      ];
      restrictionComponent.restriction = {
        action: 'CREATE',
        contextName: 'T',
        resourceGroupId: null,
        resourceTypeName: 'APP',
        resourceTypePermission: 'ALL',
        permission: { name: 'NEO' } as Permission,
      } as Restriction;
      restrictionComponent.similarRestrictions = [
        {
          action: 'CREATE',
          contextName: 'T',
          resourceGroupId: null,
          resourceTypeName: 'FOO',
          resourceTypePermission: 'ANY',
          permission: { name: 'NEO' } as Permission,
        } as Restriction,
        {
          action: 'CREATE',
          contextName: 'S',
          resourceGroupId: null,
          resourceTypeName: 'AS',
          resourceTypePermission: 'ANY',
          permission: { name: 'NEO' } as Permission,
        } as Restriction,
      ];
      // when
      const types = restrictionComponent.getAvailableResourceTypes();
      // then
      expect(types.length).toEqual(1);
      expect(types[0].name).toBe('FOO');
    }
  ));

  it('should return empty filtered resource types on getAvailableResourceTypes if in delegationMode', inject(
    [RestrictionEditComponent],
    (restrictionComponent: RestrictionEditComponent) => {
      // given
      restrictionComponent.delegationMode = true;
      restrictionComponent.resourceTypes = [
        { id: 1, name: 'APP' },
        { id: 2, name: 'AS' },
        { id: 3, name: 'FOO' },
      ];
      restrictionComponent.restriction = {
        action: 'CREATE',
        contextName: 'T',
        resourceGroupId: null,
        resourceTypeName: 'APP',
        resourceTypePermission: 'ALL',
        permission: { name: 'NEO' } as Permission,
      } as Restriction;
      restrictionComponent.similarRestrictions = [
        {
          action: 'CREATE',
          contextName: 'X',
          resourceGroupId: null,
          resourceTypeName: 'FOO',
          resourceTypePermission: 'ANY',
          permission: { name: 'NEO' } as Permission,
        } as Restriction,
        {
          action: 'CREATE',
          contextName: 'S',
          resourceGroupId: null,
          resourceTypeName: 'AS',
          resourceTypePermission: 'ANY',
          permission: { name: 'NEO' } as Permission,
        } as Restriction,
      ];
      // when
      const types = restrictionComponent.getAvailableResourceTypes();
      // then
      expect(types.length).toEqual(0);
    }
  ));

  it('should return all resource types on getAvailableResourceTypes if in delegationMode and similar restriction has resource type null', inject(
    [RestrictionEditComponent],
    (restrictionComponent: RestrictionEditComponent) => {
      // given
      restrictionComponent.delegationMode = true;
      restrictionComponent.resourceTypes = [
        { id: 1, name: 'APP' },
        { id: 2, name: 'AS' },
        { id: 3, name: 'FOO' },
      ];
      restrictionComponent.restriction = {
        action: 'CREATE',
        contextName: 'T',
        resourceGroupId: null,
        resourceTypeName: 'APP',
        resourceTypePermission: 'ALL',
        permission: { name: 'NEO' } as Permission,
      } as Restriction;
      restrictionComponent.similarRestrictions = [
        {
          action: 'CREATE',
          contextName: 'T',
          resourceGroupId: null,
          resourceTypeName: null,
          resourceTypePermission: 'ANY',
          permission: { name: 'NEO' } as Permission,
        } as Restriction,
        {
          action: 'CREATE',
          contextName: 'S',
          resourceGroupId: null,
          resourceTypeName: 'AS',
          resourceTypePermission: 'ANY',
          permission: { name: 'NEO' } as Permission,
        } as Restriction,
      ];
      // when
      const types = restrictionComponent.getAvailableResourceTypes();
      // then
      expect(types.length).toEqual(3);
    }
  ));

  it('should allow to assign ResourceType if ResourceTypePermission is ANY and ResourceGroup is null', inject(
    [RestrictionEditComponent],
    (restrictionComponent: RestrictionEditComponent) => {
      // given
      restrictionComponent.restriction = {
        action: 'CREATE',
        contextName: 'T',
        resourceGroupId: null,
        resourceTypeName: null,
        resourceTypePermission: 'ANY',
        permission: { name: 'test' } as Permission,
      } as Restriction;
      // when
      const possible: boolean = restrictionComponent.isResourceTypeAssignable();
      // then
      expect(possible).toBeTruthy();
    }
  ));

  it('should not allow to assign ResourceType if ResourceGroup is not null', inject(
    [RestrictionEditComponent],
    (restrictionComponent: RestrictionEditComponent) => {
      // given
      restrictionComponent.restriction = {
        action: 'CREATE',
        contextName: 'T',
        resourceGroupId: 8,
        resourceTypeName: null,
        resourceTypePermission: 'ANY',
        permission: { name: 'test' } as Permission,
      } as Restriction;
      // when
      const possible: boolean = restrictionComponent.isResourceTypeAssignable();
      // then
      expect(possible).toBeFalsy();
    }
  ));

  it('should not allow to assign ResourceType if ResourceTypePermission is not ANY', inject(
    [RestrictionEditComponent],
    (restrictionComponent: RestrictionEditComponent) => {
      // given
      restrictionComponent.restriction = {
        action: 'CREATE',
        contextName: 'T',
        resourceGroupId: null,
        resourceTypeName: null,
        resourceTypePermission: 'DEFAULT_ONLY',
        permission: { name: 'test' } as Permission,
      } as Restriction;
      // when
      const possible: boolean = restrictionComponent.isResourceTypeAssignable();
      // then
      expect(possible).toBeFalsy();
    }
  ));

  it('should allow to assign ResourceGroup if ResourceTypePermission is ANY and ResourceTypeName is null', inject(
    [RestrictionEditComponent],
    (restrictionComponent: RestrictionEditComponent) => {
      // given
      restrictionComponent.restriction = {
        action: 'CREATE',
        contextName: 'T',
        resourceGroupId: null,
        resourceTypeName: null,
        resourceTypePermission: 'ANY',
        permission: { name: 'test' } as Permission,
      } as Restriction;
      // when
      const possible: boolean = restrictionComponent.isResourceGroupAssignable();
      // then
      expect(possible).toBeTruthy();
    }
  ));

  it('should not allow to assign ResourceGroup if ResourceTypeName is not null', inject(
    [RestrictionEditComponent],
    (restrictionComponent: RestrictionEditComponent) => {
      // given
      restrictionComponent.restriction = {
        action: 'CREATE',
        contextName: 'T',
        resourceGroupId: null,
        resourceTypeName: 'test',
        resourceTypePermission: 'ANY',
        permission: { name: 'test' } as Permission,
      } as Restriction;
      // when
      const possible: boolean = restrictionComponent.isResourceGroupAssignable();
      // then
      expect(possible).toBeFalsy();
    }
  ));

  it('should not allow to assign ResourceGroup if ResourceTypePermission is not ANY', inject(
    [RestrictionEditComponent],
    (restrictionComponent: RestrictionEditComponent) => {
      // given
      restrictionComponent.restriction = {
        action: 'CREATE',
        contextName: 'T',
        resourceGroupId: null,
        resourceTypeName: null,
        resourceTypePermission: 'DEFAULT_ONLY',
        permission: { name: 'test' } as Permission,
      } as Restriction;
      // when
      const possible: boolean = restrictionComponent.isResourceGroupAssignable();
      // then
      expect(possible).toBeFalsy();
    }
  ));
});
