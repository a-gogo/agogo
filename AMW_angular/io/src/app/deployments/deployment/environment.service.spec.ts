import { HttpClient } from '@angular/common/http';
import {
  HttpClientTestingModule,
  HttpTestingController
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Environment } from './environment';
import { EnvironmentService } from './environment.service';

describe('DeploymentService', () => {
  let httpClient: HttpClient;
  let httpTestingController: HttpTestingController;
  let service: EnvironmentService;

  let environment: Environment = {
    id: 1,
    name: 'env',
    nameAlias: 'env-alias',
    parent: 'parens',
    selected: true,
    disabled: false
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [EnvironmentService]
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    httpClient = TestBed.inject(HttpClient);
    service = TestBed.inject(EnvironmentService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should invoke the correct endpoint on getAll()', () => {
    service.getAll().subscribe(environments => {
      expect(environments).toEqual([environment]);
    });

    const req = httpTestingController.expectOne(
      '/AMW_rest/resources/environments'
    );
    httpTestingController.expectNone(
      '/AMW_rest/resources/environments?includingGroups=true'
    );

    expect(req.request.method).toEqual('GET');
    req.flush([environment]);
  });

  it('should invoke the correct endpoint on getAllIncludingGroups ', () => {
    service.getAllIncludingGroups().subscribe(environmentIncludingGroups => {
      expect(environmentIncludingGroups).toEqual([environment]);
    });

    httpTestingController.expectNone('/AMW_rest/resources/environments');
    const req = httpTestingController.expectOne(
      '/AMW_rest/resources/environments?includingGroups=true'
    );

    expect(req.request.method).toEqual('GET');
    req.flush([environment]);
  });
});
