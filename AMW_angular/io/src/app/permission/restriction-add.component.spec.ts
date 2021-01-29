import { inject, TestBed } from '@angular/core/testing';
import { ChangeDetectorRef } from '@angular/core';
import { Permission } from './permission';
import { RestrictionAddComponent } from './restriction-add.component';
import { Restriction } from './restriction';
import { Resource } from '../resource/resource';
import * as _ from 'lodash';
import { Environment } from '@core/services';

describe('RestrictionAddComponent', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEach(() =>
    TestBed.configureTestingModule({
      providers: [RestrictionAddComponent, ChangeDetectorRef],
    })
  );

  it('should return the right title', inject(
    [RestrictionAddComponent],
    (restrictionComponent: RestrictionAddComponent) => {
      // when then
      expect(restrictionComponent.getTitle()).toBe('Add');
    }
  ));

  it('should reset all selected values on defineAvailableOptions if all selected permission names are old (global)', inject(
    [RestrictionAddComponent],
    (restrictionComponent: RestrictionAddComponent) => {
      // given
      restrictionComponent.selectedResourceTypePermission = 'DEFAULT_ONLY';
      restrictionComponent.selectedPermissionNames = ['OLD_GLOBAL', 'OLDER_GLOBAL'];
      restrictionComponent.permissions = [
        { name: 'OLD_GLOBAL', old: true } as Permission,
        { name: 'OLDER_GLOBAL', old: true } as Permission,
      ];
      // when
      restrictionComponent.defineAvailableOptions();
      // then
      expect(restrictionComponent.onlyGlobal).toBeTruthy();
      expect(_.some(restrictionComponent.actions, { selected: true })).toBeFalsy();
      expect(restrictionComponent.selectedContextNames.length).toBe(0);
      expect(restrictionComponent.selectedResourceGroupNames.length).toBe(0);
      expect(restrictionComponent.selectedResourceTypeNames.length).toBe(0);
      expect(restrictionComponent.selectedResourceTypePermission).toBe('ANY');
    }
  ));

  it('should not reset selected values on defineAvailableOptions if not all selected permission names are old (global)', inject(
    [RestrictionAddComponent],
    (restrictionComponent: RestrictionAddComponent) => {
      // given
      restrictionComponent.selectedResourceTypePermission = 'DEFAULT_ONLY';
      restrictionComponent.selectedPermissionNames = ['OLD_GLOBAL', 'NEO'];
      restrictionComponent.permissions = [
        { name: 'OLD_GLOBAL', old: true } as Permission,
        { name: 'NEO', old: false } as Permission,
      ];
      // when
      restrictionComponent.defineAvailableOptions();
      // then
      expect(restrictionComponent.onlyGlobal).toBeFalsy();
      expect(restrictionComponent.selectedResourceTypePermission).toBe('DEFAULT_ONLY');
    }
  ));

  it('should prepare available options on defineAvailableOptions if in delegationMode and not all selected Permission are old (global)', inject(
    [RestrictionAddComponent],
    (restrictionComponent: RestrictionAddComponent) => {
      // given
      restrictionComponent.delegationMode = true;
      restrictionComponent.selectedPermissionNames = ['OLD_GLOBAL', 'NEO'];
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
      expect(_.some(restrictionComponent.actions, { name: 'ALL', disabled: true })).toBeTruthy();
      expect(_.some(restrictionComponent.actions, { name: 'DELETE', disabled: true })).toBeTruthy();
      expect(_.some(restrictionComponent.actions, { name: 'READ', disabled: true })).toBeTruthy();
      expect(_.some(restrictionComponent.actions, { name: 'CREATE', disabled: true })).toBeFalsy();
      expect(_.some(restrictionComponent.actions, { name: 'UPDATE', disabled: true })).toBeFalsy();
    }
  ));

  it('should return all resource groups on getAvailableResourceGroups if not in delegationMode', inject(
    [RestrictionAddComponent],
    (restrictionComponent: RestrictionAddComponent) => {
      // given
      restrictionComponent.resourceGroups = [{ id: 1 } as Resource, { id: 9 } as Resource, { id: 10 } as Resource];
      restrictionComponent.selectedPermissionNames = ['NEO'];
      // when
      const groups = restrictionComponent.getAvailableResourceGroups();
      // then
      expect(groups.length).toEqual(3);
    }
  ));

  it('should disable some actions on getAvailableActions if in delegationMode and none of the similar restrictions has action ALL', inject(
    [RestrictionAddComponent],
    (restrictionComponent: RestrictionAddComponent) => {
      // given
      restrictionComponent.delegationMode = true;
      restrictionComponent.selectedPermissionNames = ['NEO'];
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
      restrictionComponent.getAvailableActions();
      // then
      expect(_.some(restrictionComponent.actions, { disabled: true })).toBeTruthy();
    }
  ));

  it('should leave all actions enabled on getAvailableActions if in delegationMode and one of the similar restrictions has action ALL', inject(
    [RestrictionAddComponent],
    (restrictionComponent: RestrictionAddComponent) => {
      // given
      restrictionComponent.delegationMode = true;
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
      restrictionComponent.getAvailableActions();
      // then
      expect(_.some(restrictionComponent.actions, { disabled: true })).toBeFalsy();
    }
  ));

  it('should leave all actions enabled on getAvailableActions if not in delegationMode', inject(
    [RestrictionAddComponent],
    (restrictionComponent: RestrictionAddComponent) => {
      // given // when
      restrictionComponent.getAvailableActions();
      // then
      expect(_.some(restrictionComponent.actions, { disabled: true })).toBeFalsy();
    }
  ));

  it('should return true on hasSelectedActions if there are selected actions', inject(
    [RestrictionAddComponent],
    (restrictionComponent: RestrictionAddComponent) => {
      // given
      expect(restrictionComponent.hasSelectedActions()).toBeFalsy();
      restrictionComponent.actions.forEach((action) => {
        if (action.name === 'CREATE') {
          action.selected = true;
        }
      });
      // when // then
      expect(restrictionComponent.hasSelectedActions()).toBeTruthy();
    }
  ));

  it('should return filtered resource types on getAvailableResourceTypes if in delegationMode', inject(
    [RestrictionAddComponent],
    (restrictionComponent: RestrictionAddComponent) => {
      // given
      restrictionComponent.delegationMode = true;
      restrictionComponent.resourceTypes = [
        { id: 1, name: 'APP' },
        { id: 2, name: 'AS' },
        { id: 3, name: 'FOO' },
      ];
      restrictionComponent.selectedPermissionNames = ['NEO'];
      restrictionComponent.selectedContextNames = ['T', 'S'];
      restrictionComponent.selectedResourceTypePermission = 'ANY';
      restrictionComponent.actions.forEach((action) => {
        if (action.name === 'CREATE') {
          action.selected = true;
        }
      });
      restrictionComponent.similarRestrictions = [
        {
          action: 'ALL',
          contextName: 'T',
          resourceGroupId: null,
          resourceTypeName: 'FOO',
          resourceTypePermission: 'ANY',
          permission: { name: 'NEO' } as Permission,
        } as Restriction,
        {
          action: 'READ',
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
    [RestrictionAddComponent],
    (restrictionComponent: RestrictionAddComponent) => {
      // given
      restrictionComponent.delegationMode = true;
      restrictionComponent.resourceTypes = [
        { id: 1, name: 'APP' },
        { id: 2, name: 'AS' },
        { id: 3, name: 'FOO' },
      ];
      restrictionComponent.selectedPermissionNames = ['NEO'];
      restrictionComponent.selectedContextNames = ['T'];
      restrictionComponent.selectedResourceTypePermission = 'ANY';
      restrictionComponent.selectedResourceTypeNames = ['APP'];
      restrictionComponent.actions.forEach((action) => {
        if (action.name === 'CREATE') {
          action.selected = true;
        }
      });
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
    [RestrictionAddComponent],
    (restrictionComponent: RestrictionAddComponent) => {
      // given
      restrictionComponent.delegationMode = true;
      restrictionComponent.resourceTypes = [
        { id: 1, name: 'APP' },
        { id: 2, name: 'AS' },
        { id: 3, name: 'FOO' },
      ];
      restrictionComponent.selectedPermissionNames = ['NEO'];
      restrictionComponent.selectedContextNames = ['T'];
      restrictionComponent.selectedResourceTypePermission = 'ANY';
      restrictionComponent.selectedResourceTypeNames = ['APP'];
      restrictionComponent.actions.forEach((action) => {
        if (action.name === 'CREATE') {
          action.selected = true;
        }
      });
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

  it('should allow to assign ResourceType if selectedResourceTypePermission is ANY and selectedResourceGroupNames is empty', inject(
    [RestrictionAddComponent],
    (restrictionComponent: RestrictionAddComponent) => {
      // given
      restrictionComponent.selectedResourceTypePermission = 'ANY';
      restrictionComponent.actions.forEach((action) => {
        if (action.name === 'CREATE') {
          action.selected = true;
        }
      });
      // when
      const possible: boolean = restrictionComponent.isResourceTypeAssignable();
      // then
      expect(possible).toBeTruthy();
    }
  ));

  it('should not allow to assign ResourceType if selectedResourceGroupNames is not empty', inject(
    [RestrictionAddComponent],
    (restrictionComponent: RestrictionAddComponent) => {
      // given
      restrictionComponent.selectedResourceTypePermission = 'ANY';
      restrictionComponent.selectedResourceGroupNames = ['TEST_GROUP'];
      // when
      const possible: boolean = restrictionComponent.isResourceTypeAssignable();
      // then
      expect(possible).toBeFalsy();
    }
  ));

  it('should not allow to assign ResourceType if selectedResourceTypePermission is not ANY', inject(
    [RestrictionAddComponent],
    (restrictionComponent: RestrictionAddComponent) => {
      // given
      restrictionComponent.selectedResourceTypePermission = 'DEFAULT_ONLY';
      // when
      const possible: boolean = restrictionComponent.isResourceTypeAssignable();
      // then
      expect(possible).toBeFalsy();
    }
  ));

  it('should allow to assign ResourceGroup if selectedResourceTypePermission is ANY and selectedResourceTypeNames is empty', inject(
    [RestrictionAddComponent],
    (restrictionComponent: RestrictionAddComponent) => {
      // given
      restrictionComponent.selectedResourceTypePermission = 'ANY';
      // when
      const possible: boolean = restrictionComponent.isResourceGroupAssignable();
      // then
      expect(possible).toBeTruthy();
    }
  ));

  it('should not allow to assign ResourceGroup if selectedResourceTypeNames is not empty', inject(
    [RestrictionAddComponent],
    (restrictionComponent: RestrictionAddComponent) => {
      // given
      restrictionComponent.selectedResourceTypePermission = 'ANY';
      restrictionComponent.selectedResourceTypeNames = ['APP'];
      // when
      const possible: boolean = restrictionComponent.isResourceGroupAssignable();
      // then
      expect(possible).toBeFalsy();
    }
  ));

  it('should not allow to assign ResourceGroup if selectedResourceTypePermission is not ANY', inject(
    [RestrictionAddComponent],
    (restrictionComponent: RestrictionAddComponent) => {
      // given
      restrictionComponent.selectedResourceTypePermission = 'DEFAULT_ONLY';
      // when
      const possible: boolean = restrictionComponent.isResourceGroupAssignable();
      // then
      expect(possible).toBeFalsy();
    }
  ));

  it('should not allow to assign ResourceTypePermission if selectedResourceTypeNames is not empty', inject(
    [RestrictionAddComponent],
    (restrictionComponent: RestrictionAddComponent) => {
      // given
      restrictionComponent.selectedResourceTypeNames = ['APP'];
      // when
      const possible: boolean = restrictionComponent.isResourceTypePermissionAssignable();
      // then
      expect(possible).toBeFalsy();
    }
  ));

  it('should not allow to assign ResourceTypePermission if selectedResourceGroupNames is not empty', inject(
    [RestrictionAddComponent],
    (restrictionComponent: RestrictionAddComponent) => {
      // given
      restrictionComponent.selectedResourceGroupNames = ['TEST'];
      // when
      const possible: boolean = restrictionComponent.isResourceTypePermissionAssignable();
      // then
      expect(possible).toBeFalsy();
    }
  ));

  it('should clear selectedResourceGroupNames and selectedResourceTypeNames on clearTypeAndGroup if selectedResourceTypePermission is ANY', inject(
    [RestrictionAddComponent],
    (restrictionComponent: RestrictionAddComponent) => {
      // given
      restrictionComponent.selectedResourceTypePermission = 'DEFAULT_ONLY';
      // when
      restrictionComponent.clearTypeAndGroup();
      // then
      expect(restrictionComponent.selectedResourceGroupNames.length).toBe(0);
      expect(restrictionComponent.selectedResourceTypeNames.length).toBe(0);
    }
  ));

  it('should reset all selections on persistRestriction', inject(
    [RestrictionAddComponent],
    (restrictionComponent: RestrictionAddComponent) => {
      // given
      restrictionComponent.userNames = ['TesterA', 'TesterB'];
      restrictionComponent.selectedPermissionNames = ['NEO'];
      restrictionComponent.selectedContextNames = ['T', 'S'];
      restrictionComponent.selectedResourceTypePermission = 'ANY';
      restrictionComponent.actions.forEach((action) => {
        if (action.name === 'CREATE') {
          action.selected = true;
        }
      });
      // when
      restrictionComponent.persistRestriction();
      // then
      expect(_.some(restrictionComponent.actions, { selected: true })).toBeFalsy();
      expect(restrictionComponent.selectedPermissionNames.length).toEqual(0);
      expect(restrictionComponent.selectedContextNames.length).toEqual(0);
      expect(restrictionComponent.selectedResourceTypePermission).toBeNull();
    }
  ));

  it('should pre select environment if in delegation mode and only one environment is available for the selected action', inject(
    [RestrictionAddComponent],
    (restrictionComponent: RestrictionAddComponent) => {
      // given
      const emptyEnvironment: Environment[] = [
        { id: null, name: null, nameAlias: null, parent: 'All', selected: false, disabled: false },
      ];
      const devEnvironments: Environment[] = [
        { id: 1, name: 'B', nameAlias: 'Test', parent: 'Dev', selected: false, disabled: false },
        { id: 2, name: 'C', nameAlias: null, parent: 'Dev', selected: false, disabled: false },
      ];
      restrictionComponent.groupedEnvironments = { All: emptyEnvironment, Dev: devEnvironments };
      restrictionComponent.delegationMode = true;
      restrictionComponent.userNames = ['TesterA', 'TesterB'];
      restrictionComponent.selectedPermissionNames = ['NEO'];
      restrictionComponent.selectedContextNames = ['B', 'C'];
      restrictionComponent.selectedResourceTypePermission = 'ANY';
      restrictionComponent.actions.forEach((action) => {
        if (action.name === 'CREATE') {
          action.selected = true;
        }
      });
      restrictionComponent.similarRestrictions = [
        {
          action: 'CREATE',
          contextName: 'B',
          resourceGroupId: null,
          resourceTypeName: 'FOO',
          resourceTypePermission: 'ANY',
          permission: { name: 'NEO' } as Permission,
        } as Restriction,
        {
          action: 'READ',
          contextName: 'C',
          resourceGroupId: null,
          resourceTypeName: 'AS',
          resourceTypePermission: 'ANY',
          permission: { name: 'NEO' } as Permission,
        } as Restriction,
      ];
      // when
      restrictionComponent.checkAvailableEnvironments();
      // then
      expect(restrictionComponent.availableEnvironments.length).toBe(1);
      expect(restrictionComponent.selectedContextNames.length).toBe(1);
      expect(restrictionComponent.selectedContextNames[0]).toBe('B');
    }
  ));
});
