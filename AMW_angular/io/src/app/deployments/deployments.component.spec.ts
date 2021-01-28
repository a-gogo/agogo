import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, Subject } from 'rxjs';
import { ResourceService } from '../resource/resource.service';
import { DeploymentsComponent } from './deployments.component';
import { SharedModule } from '../shared/shared.module';
import { DeploymentsListComponent } from './deployments-list.component';
import { DeploymentsEditModalComponent } from './deployments-edit-modal.component';
import { PaginationComponent } from '../shared/pagination/pagination.component';
import { NavigationStoreService } from '../navigation/navigation-store.service';
import { ComparatorFilterOption } from './deployment/comparator-filter-option';
import { Deployment } from './deployment/deployment';
import { DeploymentFilter } from './deployment/deployment-filter';
import { DeploymentFilterType } from './deployment/deployment-filter-type';
import { DeploymentService } from './deployment/deployment.service';

declare var $: any;

describe('DeploymentsComponent (with query params)', () => {
  let component: DeploymentsComponent;
  let fixture: ComponentFixture<DeploymentsComponent>;

  let mockRoute: any = { snapshot: {} };

  mockRoute.params = new Subject<any>();
  mockRoute.queryParams = new Subject<any>();

  const filter: string = JSON.stringify([
    { name: 'Application', val: 'test' },
    { name: 'Confirmed on', comp: 'lt', val: '12.12.2012 12:12' },
  ]);
  let deploymentService: DeploymentService;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FormsModule, HttpClientTestingModule, RouterTestingModule.withRoutes([]), SharedModule],
      providers: [
        DeploymentService,
        ResourceService,
        NavigationStoreService,
        { provide: ActivatedRoute, useValue: mockRoute },
      ],
      declarations: [
        DeploymentsComponent,
        DeploymentsListComponent,
        PaginationComponent,
        DeploymentsEditModalComponent,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DeploymentsComponent);
    component = fixture.componentInstance;
    deploymentService = TestBed.inject(DeploymentService);
  });

  it('should extract filters from param on ngOnInit', () => {
    // given
    const deploymentFilters: DeploymentFilterType[] = [
      { name: 'Application', type: 'StringType' },
      { name: 'Confirmed on', type: 'DateType' },
    ];
    spyOn(deploymentService, 'getAllDeploymentFilterTypes').and.returnValue(of(deploymentFilters));
    spyOn(deploymentService, 'getAllComparatorFilterOptions').and.returnValue(of([]));
    spyOn(deploymentService, 'canRequestDeployments').and.returnValue(of(true));

    // when
    component.ngOnInit();
    mockRoute.queryParams.next({ filters: filter });

    // then
    expect(component.paramFilters.length).toEqual(2);
    expect(deploymentService.canRequestDeployments).toHaveBeenCalled();
  });

  it('should enhance filters with the right comparator and comparator options on ngOnInit', () => {
    // given
    const deploymentFilters: DeploymentFilterType[] = [
      { name: 'Application', type: 'StringType' },
      { name: 'Confirmed on', type: 'DateType' },
    ];
    const comparatorOptions: ComparatorFilterOption[] = [
      { name: 'lt', displayName: '<' },
      { name: 'eq', displayName: 'is' },
      { name: 'neq', displayName: 'is not' },
    ];
    spyOn(deploymentService, 'getAllDeploymentFilterTypes').and.returnValue(of(deploymentFilters));
    spyOn(deploymentService, 'getAllComparatorFilterOptions').and.returnValue(of(comparatorOptions));
    spyOn(deploymentService, 'canRequestDeployments').and.returnValue(of(true));

    // when
    component.ngOnInit();
    mockRoute.queryParams.next({ filters: filter });

    // then
    expect(component.paramFilters[0].compOptions.length).toEqual(1);
    expect(component.paramFilters[1].compOptions.length).toEqual(3);
    expect(component.paramFilters[0].comp).toEqual('eq');
    expect(component.paramFilters[1].comp).toEqual('lt');
  });

  it('should enhance filters with the right option values on ngOnInit', () => {
    // given
    const deploymentFilters: DeploymentFilterType[] = [
      { name: 'Application', type: 'StringType' },
      { name: 'Confirmed on', type: 'DateType' },
    ];
    spyOn(deploymentService, 'getAllDeploymentFilterTypes').and.returnValue(of(deploymentFilters));
    spyOn(deploymentService, 'getAllComparatorFilterOptions').and.returnValue(of([]));
    spyOn(deploymentService, 'getFilterOptionValues').and.callFake((param: string) => {
      const optionValues: { [key: string]: string[] } = {
        Application: ['app1', 'app2'],
        'Confirmed on': [],
      };
      return of(optionValues[param]);
    });
    spyOn(deploymentService, 'canRequestDeployments').and.returnValue(of(true));

    // when
    component.ngOnInit();
    mockRoute.queryParams.next({ filters: filter });

    // then
    expect(component.paramFilters[0].valOptions.length).toEqual(2);
    expect(component.paramFilters[1].valOptions.length).toEqual(0);
  });

  it('should apply filters ngOnInit ', () => {
    // given
    const deploymentFilters: DeploymentFilterType[] = [
      { name: 'Application', type: 'StringType' },
      { name: 'Confirmed on', type: 'DateType' },
    ];
    spyOn(deploymentService, 'getAllDeploymentFilterTypes').and.returnValue(of(deploymentFilters));
    spyOn(deploymentService, 'getAllComparatorFilterOptions').and.returnValue(of([]));
    spyOn(deploymentService, 'canRequestDeployments').and.returnValue(of(true));
    spyOn(deploymentService, 'getFilteredDeployments').and.returnValue(of({ deployments: [], total: 0 }));

    // when
    component.ngOnInit();
    mockRoute.queryParams.next({ filters: filter });

    // then
    expect(component.autoload).toBeTruthy();
    expect(deploymentService.getFilteredDeployments).toHaveBeenCalled();
  });

  it('should call the right service method on exportCSV ', () => {
    // given
    const deploymentFilters: DeploymentFilterType[] = [
      { name: 'Application', type: 'StringType' },
      { name: 'Confirmed on', type: 'DateType' },
    ];
    var buffer = new ArrayBuffer(8);
    spyOn(deploymentService, 'getFilteredDeploymentsForCsvExport').and.returnValue(of(buffer));

    // when
    component.exportCSV();

    // then
    expect(deploymentService.getFilteredDeploymentsForCsvExport).toHaveBeenCalledWith(
      JSON.stringify(component.filtersForBackend),
      'd.deploymentDate',
      'DESC'
    );
  });
});

describe('DeploymentsComponent (with illegal query params)', () => {
  let component: DeploymentsComponent;
  let fixture: ComponentFixture<DeploymentsComponent>;

  let mockRoute: any = { snapshot: {} };

  mockRoute.params = new Subject<any>();
  mockRoute.queryParams = new Subject<any>();

  const filter: string = JSON.stringify([
    { name: 'Application', val: 'test' },
    { name: 'Confirmed on', comp: 'lt', val: '12.12.2012 12:12' },
  ]);
  let deploymentService: DeploymentService;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FormsModule, HttpClientTestingModule, RouterTestingModule.withRoutes([]), SharedModule],
      providers: [
        DeploymentService,
        ResourceService,
        NavigationStoreService,
        { provide: ActivatedRoute, useValue: mockRoute },
      ],
      declarations: [
        DeploymentsComponent,
        DeploymentsListComponent,
        PaginationComponent,
        DeploymentsEditModalComponent,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DeploymentsComponent);
    component = fixture.componentInstance;
    deploymentService = TestBed.inject(DeploymentService);
  });

  it('should display error message on faulty filters params on ngOnInit', () => {
    // given
    const deploymentFilters: DeploymentFilterType[] = [
      { name: 'Application', type: 'StringType' },
      { name: 'Confirmed on', type: 'DateType' },
    ];
    spyOn(deploymentService, 'getAllDeploymentFilterTypes').and.returnValue(of(deploymentFilters));
    spyOn(deploymentService, 'getAllComparatorFilterOptions').and.returnValue(of([]));

    // when
    component.ngOnInit();
    mockRoute.queryParams.next({ filters: 'invalid filter' });

    // then
    expect(component.autoload).toBeFalsy();
    expect(component.errorMessage).toEqual('Error parsing filter');
    expect(component.paramFilters.length).toEqual(0);
  });
});

describe('DeploymentsComponent (without query params)', () => {
  // provide our implementations or mocks to the dependency injector
  let component: DeploymentsComponent;
  let fixture: ComponentFixture<DeploymentsComponent>;

  let mockRoute: any = { snapshot: {} };
  mockRoute.queryParams = new Subject<any>();

  const filter: string = JSON.stringify([
    { name: 'Application', val: 'test' },
    { name: 'Confirmed on', comp: 'lt', val: '12.12.2012 12:12' },
  ]);
  let deploymentService: DeploymentService;
  let resourceService: ResourceService;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FormsModule, HttpClientTestingModule, RouterTestingModule.withRoutes([]), SharedModule],
      providers: [
        DeploymentService,
        ResourceService,
        NavigationStoreService,
        { provide: ActivatedRoute, useValue: mockRoute },
      ],
      declarations: [
        DeploymentsComponent,
        DeploymentsListComponent,
        PaginationComponent,
        DeploymentsEditModalComponent,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DeploymentsComponent);
    component = fixture.componentInstance;
    deploymentService = TestBed.inject(DeploymentService);
    resourceService = TestBed.inject(ResourceService);
  });

  it('should check permission on ngOnInit', () => {
    // given
    spyOn(deploymentService, 'canRequestDeployments').and.returnValue(of(true));

    // when
    component.ngOnInit();
    mockRoute.queryParams.next({});

    // then
    expect(deploymentService.canRequestDeployments).toHaveBeenCalled();
  });

  // I don't know how to create/ get hold of the ngModel in a simple manner. How much concepts do you have to know about in order to write some tests? It's mind blowing!
  // it('should add a filter', () => {
  //   // given
  //   component.selectModel = selectModel;
  //   component.filters = [
  //     {
  //       name: 'Confirmed',
  //       comp: 'eq',
  //       val: 'true',
  //       type: 'booleanType'
  //     } as DeploymentFilter
  //   ];
  //   component.selectedFilterType = {
  //     name: 'Application',
  //     type: 'StringType'
  //   };
  //   const deploymentFilters: DeploymentFilterType[] = [
  //     { name: 'Application', type: 'StringType' },
  //     { name: 'Confirmed on', type: 'DateType' }
  //   ];
  //   const comparatorOptions: ComparatorFilterOption[] = [
  //     { name: 'lt', displayName: '<' },
  //     { name: 'eq', displayName: 'is' },
  //     { name: 'neq', displayName: 'is not' }
  //   ];
  //   spyOn(deploymentService, 'getAllDeploymentFilterTypes').and.returnValue(
  //     of(deploymentFilters)
  //   );
  //   spyOn(deploymentService, 'getAllComparatorFilterOptions').and.returnValue(
  //     of(comparatorOptions)
  //   );
  //   expect(component.filters.length).toEqual(1);

  //   // when
  //   component.addFilter();

  //   // then
  //   expect(component.filters.length).toEqual(2);
  // });

  // it('should reset offset and selectedFilterType on addFilter', inject(
  //   [DeploymentsComponent, DeploymentService, NgModel],
  //   (
  //     component: DeploymentsComponent,
  //     deploymentService: DeploymentService,
  //     selectModel: NgModel
  //   ) => {
  //     // given
  //     component.selectModel = selectModel;
  //     component.offset = 10;
  //     component.filters = [
  //       {
  //         name: 'Confirmed',
  //         comp: 'eq',
  //         val: 'true',
  //         type: 'booleanType'
  //       } as DeploymentFilter
  //     ];
  //     component.selectedFilterType = {
  //       name: 'Application',
  //       type: 'StringType'
  //     };
  //     const deploymentFilters: DeploymentFilterType[] = [
  //       { name: 'Application', type: 'StringType' },
  //       { name: 'Confirmed on', type: 'DateType' }
  //     ];
  //     const comparatorOptions: ComparatorFilterOption[] = [
  //       { name: 'lt', displayName: '<' },
  //       { name: 'eq', displayName: 'is' },
  //       { name: 'neq', displayName: 'is not' }
  //     ];
  //     spyOn(deploymentService, 'getAllDeploymentFilterTypes').and.returnValue(
  //       of(deploymentFilters)
  //     );
  //     spyOn(deploymentService, 'getAllComparatorFilterOptions').and.returnValue(
  //       of(comparatorOptions)
  //     );

  //     // when
  //     component.addFilter();

  //     // then
  //     expect(component.offset).toEqual(0);
  //     expect(component.selectedFilterType).toBeNull();
  //   }
  // ));

  it('should remove filter and reset offset on removeFilter', () => {
    // given
    component.offset = 10;
    component.filters = [
      {
        name: 'Confirmed',
        comp: 'eq',
        val: 'true',
        type: 'booleanType',
      } as DeploymentFilter,
    ];
    const deploymentFilters: DeploymentFilterType[] = [
      { name: 'Application', type: 'StringType' },
      { name: 'Confirmed on', type: 'DateType' },
    ];
    const comparatorOptions: ComparatorFilterOption[] = [
      { name: 'lt', displayName: '<' },
      { name: 'eq', displayName: 'is' },
      { name: 'neq', displayName: 'is not' },
    ];
    spyOn(deploymentService, 'getAllDeploymentFilterTypes').and.returnValue(of(deploymentFilters));
    spyOn(deploymentService, 'getAllComparatorFilterOptions').and.returnValue(of(comparatorOptions));

    // when
    component.removeFilter(component.filters[0]);

    // then
    expect(component.filters.length).toEqual(0);
    expect(component.offset).toEqual(0);
  });

  it('should reset offset on setMaxResultsPerPage', () => {
    // given
    component.offset = 10;
    component.filters = [
      {
        name: 'Confirmed',
        comp: 'eq',
        val: 'true',
        type: 'booleanType',
      } as DeploymentFilter,
    ];

    // when
    component.setMaxResultsPerPage(20);

    // then
    expect(component.offset).toEqual(0);
  });

  // it('should not add latest deployment job filter more than once', () => {
  //   // given
  //   component.selectModel = selectModel;
  //   component.filters = [
  //     {
  //       name: 'Confirmed',
  //       comp: 'eq',
  //       val: 'true',
  //       type: 'booleanType'
  //     } as DeploymentFilter
  //   ];
  //   component.selectedFilterType = {
  //     name: 'Latest deployment job for App Server and Env',
  //     type: 'SpecialFilterType'
  //   };
  //   const deploymentFilters: DeploymentFilterType[] = [
  //     {
  //       name: 'Latest deployment job for App Server and Env',
  //       type: 'SpecialFilterType'
  //     },
  //     { name: 'Confirmed on', type: 'DateType' }
  //   ];
  //   const comparatorOptions: ComparatorFilterOption[] = [
  //     { name: 'lt', displayName: '<' },
  //     { name: 'eq', displayName: 'is' },
  //     { name: 'neq', displayName: 'is not' }
  //   ];
  //   spyOn(deploymentService, 'getAllDeploymentFilterTypes').and.returnValue(
  //     of(deploymentFilters)
  //   );
  //   spyOn(deploymentService, 'getAllComparatorFilterOptions').and.returnValue(
  //     of(comparatorOptions)
  //   );
  //   expect(component.filters.length).toEqual(1);

  //   // when
  //   component.addFilter();

  //   // then
  //   expect(component.filters.length).toEqual(2);

  //   // when
  //   component.selectedFilterType = {
  //     name: 'Latest deployment job for App Server and Env',
  //     type: 'SpecialFilterType'
  //   };
  //   component.addFilter();

  //   // then
  //   expect(component.filters.length).toEqual(2);
  // });

  it('should apply filters and add them to the sessionStorage on applyFilters', () => {
    // given
    sessionStorage.setItem('deploymentFilters', null);
    const expectedFilters: DeploymentFilter[] = [
      { name: 'Confirmed', comp: 'eq', val: 'true' } as DeploymentFilter,
      { name: 'Application', comp: 'eq', val: 'TestApp' } as DeploymentFilter,
    ];
    const deploymentFilters: DeploymentFilterType[] = [
      { name: 'Application', type: 'StringType' },
      { name: 'Confirmed on', type: 'DateType' },
    ];
    const comparatorOptions: ComparatorFilterOption[] = [
      { name: 'lt', displayName: '<' },
      { name: 'eq', displayName: 'is' },
      { name: 'neq', displayName: 'is not' },
    ];
    spyOn(deploymentService, 'getAllDeploymentFilterTypes').and.returnValue(of(deploymentFilters));
    spyOn(deploymentService, 'getAllComparatorFilterOptions').and.returnValue(of(comparatorOptions));
    spyOn(deploymentService, 'getFilteredDeployments').and.returnValue(of({ deployments: [], total: 0 }));

    // given
    component.filters = [
      {
        name: 'Confirmed',
        comp: 'eq',
        val: 'true',
        type: 'booleanType',
      } as DeploymentFilter,
      {
        name: 'Application',
        comp: 'eq',
        val: 'TestApp',
        type: 'StringType',
      } as DeploymentFilter,
    ];

    // when
    component.applyFilters();

    // then
    expect(sessionStorage.getItem('deploymentFilters')).toEqual(JSON.stringify(expectedFilters));
    expect(deploymentService.getFilteredDeployments).toHaveBeenCalledWith(
      JSON.stringify(expectedFilters),
      'd.deploymentDate',
      'DESC',
      0,
      10
    );
  });

  it('should sort out filters without a value on apply filters and add the remaining to the sessionStorage on applyFilters', () => {
    // given
    sessionStorage.setItem('deploymentFilters', null);
    const expectedFilters: DeploymentFilter[] = [
      { name: 'Confirmed', comp: 'eq', val: 'false' } as DeploymentFilter,
      { name: 'Application', comp: 'eq', val: 'TestApp' } as DeploymentFilter,
      { name: 'Latest deployment', comp: 'eq', val: '' } as DeploymentFilter,
    ];
    const deploymentFilters: DeploymentFilterType[] = [
      { name: 'Application', type: 'StringType' },
      { name: 'Application Server', type: 'StringType' },
      { name: 'Confirmed on', type: 'DateType' },
      { name: 'Latest deployment', type: 'SpecialFilterType' },
    ];
    const comparatorOptions: ComparatorFilterOption[] = [
      { name: 'lt', displayName: '<' },
      { name: 'eq', displayName: 'is' },
      { name: 'neq', displayName: 'is not' },
    ];
    spyOn(deploymentService, 'getAllDeploymentFilterTypes').and.returnValue(of(deploymentFilters));
    spyOn(deploymentService, 'getAllComparatorFilterOptions').and.returnValue(of(comparatorOptions));
    spyOn(deploymentService, 'getFilteredDeployments').and.returnValue(of({ deployments: [], total: 0 }));

    component.filters = [
      {
        name: 'Confirmed',
        comp: 'eq',
        val: 'false',
        type: 'booleanType',
      } as DeploymentFilter,
      {
        name: 'Application',
        comp: 'eq',
        val: 'TestApp',
        type: 'StringType',
      } as DeploymentFilter,
      {
        name: 'Application Server',
        comp: 'eq',
        val: '',
        type: 'StringType',
      } as DeploymentFilter,
      {
        name: 'Latest deployment',
        comp: 'eq',
        val: '',
        type: 'SpecialFilterType',
      } as DeploymentFilter,
    ];

    // when
    component.applyFilters();

    // then
    expect(component.filters.length).toEqual(3);
    expect(sessionStorage.getItem('deploymentFilters')).toEqual(JSON.stringify(expectedFilters));
    expect(deploymentService.getFilteredDeployments).toHaveBeenCalledWith(
      JSON.stringify(expectedFilters),
      'd.deploymentDate',
      'DESC',
      0,
      10
    );
  });

  it('should clear filters and session storage', () => {
    // given
    sessionStorage.setItem('deploymentFilters', "{name: 'Confirmed', comp: 'eq', val: 'true'}");

    // when
    component.clearFilters();

    // then
    expect(sessionStorage.getItem('deploymentFilters')).toBe('null');
  });

  it('should invoke service with right params on sort', () => {
    // given
    const expectedFilters: DeploymentFilter[] = [
      { name: 'Confirmed', comp: 'eq', val: 'true' } as DeploymentFilter,
      { name: 'Application', comp: 'eq', val: 'TestApp' } as DeploymentFilter,
    ];
    component.filters = [
      {
        name: 'Confirmed',
        comp: 'eq',
        val: 'true',
        type: 'booleanType',
      } as DeploymentFilter,
      {
        name: 'Application',
        comp: 'eq',
        val: 'TestApp',
        type: 'StringType',
      } as DeploymentFilter,
    ];
    const deploymentFilters: DeploymentFilterType[] = [
      { name: 'Application', type: 'StringType' },
      { name: 'Confirmed on', type: 'DateType' },
    ];
    const comparatorOptions: ComparatorFilterOption[] = [
      { name: 'lt', displayName: '<' },
      { name: 'eq', displayName: 'is' },
      { name: 'neq', displayName: 'is not' },
    ];
    spyOn(deploymentService, 'getAllDeploymentFilterTypes').and.returnValue(of(deploymentFilters));
    spyOn(deploymentService, 'getAllComparatorFilterOptions').and.returnValue(of(comparatorOptions));
    spyOn(deploymentService, 'getFilteredDeployments').and.returnValue(of({ deployments: [], total: 0 }));

    // when
    component.sortDeploymentsBy('d.trackingId');

    // then
    expect(deploymentService.getFilteredDeployments).toHaveBeenCalledWith(
      JSON.stringify(expectedFilters),
      'd.trackingId',
      'DESC',
      0,
      10
    );

    // when
    component.sortDeploymentsBy('d.trackingId');

    // then
    expect(deploymentService.getFilteredDeployments).toHaveBeenCalledWith(
      JSON.stringify(expectedFilters),
      'd.trackingId',
      'ASC',
      0,
      10
    );
  });

  it('should check permission on showEdit', () => {
    // given
    component.deployments = [
      { id: 1, appServerId: 12, selected: false } as Deployment,
      { id: 21, appServerId: 22, selected: true } as Deployment,
    ];
    spyOn(resourceService, 'canCreateShakedownTest').and.returnValue(of(true));

    // when
    component.showEdit();

    // then
    expect(resourceService.canCreateShakedownTest).toHaveBeenCalledWith(22);
  });

  it('should confirm a deployment and reload it', () => {
    // given
    const deployment: Deployment = { id: 1 } as Deployment;
    spyOn(deploymentService, 'confirmDeployment').and.returnValue(of());
    spyOn(deploymentService, 'getWithActions').and.returnValue(of(deployment));

    // when
    component.confirmDeployment(deployment);

    // then
    expect(deploymentService.confirmDeployment).toHaveBeenCalledWith(deployment);
    expect(deploymentService.getWithActions).toHaveBeenCalledWith(deployment.id);
  });

  it('should reject a deployment and reload it', () => {
    // given
    const deployment: Deployment = { id: 2 } as Deployment;
    spyOn(deploymentService, 'rejectDeployment').and.returnValue(of());
    spyOn(deploymentService, 'getWithActions').and.returnValue(of(deployment));

    // when
    component.rejectDeployment(deployment);

    // then
    expect(deploymentService.rejectDeployment).toHaveBeenCalledWith(deployment.id);
    expect(deploymentService.getWithActions).toHaveBeenCalledWith(deployment.id);
  });

  it('should cancel a deployment and reload it', () => {
    // given
    const deployment: Deployment = { id: 3 } as Deployment;
    spyOn(deploymentService, 'cancelDeployment').and.returnValue(of());
    spyOn(deploymentService, 'getWithActions').and.returnValue(of(deployment));

    // when
    component.cancelDeployment(deployment);

    // then
    expect(deploymentService.cancelDeployment).toHaveBeenCalledWith(deployment.id);
    expect(deploymentService.getWithActions).toHaveBeenCalledWith(deployment.id);
  });

  it('should invoke the right deploymentService methods with right arguments on changeDeploymentDate', () => {
    // given
    const deployment: Deployment = {
      id: 3,
      deploymentDate: 1253765123,
    } as Deployment;
    component.deployments = [
      { id: 1, deploymentDate: 121212121 } as Deployment,
      { id: 3, deploymentDate: 121212111 } as Deployment,
      { id: 5, deploymentDate: 121212122 } as Deployment,
    ];

    spyOn(deploymentService, 'setDeploymentDate').and.returnValue(of());
    spyOn(deploymentService, 'getWithActions').and.returnValue(of(deployment));

    // when
    component.changeDeploymentDate(deployment);

    // then
    expect(deploymentService.setDeploymentDate).toHaveBeenCalledWith(deployment.id, deployment.deploymentDate);
    expect(deploymentService.getWithActions).toHaveBeenCalledWith(deployment.id);
    expect(component.deployments[1].deploymentDate).toEqual(deployment.deploymentDate);
  });
});
