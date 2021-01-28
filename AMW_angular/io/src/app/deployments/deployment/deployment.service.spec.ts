import { HttpClient } from '@angular/common/http';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Deployment } from './deployment';
import { DeploymentService } from './deployment.service';

describe('DeploymentService', () => {
  let httpClient: HttpClient;
  let httpTestingController: HttpTestingController;
  let service: DeploymentService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [DeploymentService],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    httpClient = TestBed.inject(HttpClient);
    service = TestBed.inject(DeploymentService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should getAllDeploymentParamterKeys() a list of deploymentPerameters', () => {
    const deploymentParamter = {
      key: 'key',
      value: 'value',
    };

    service
      .getAllDeploymentParameterKeys()
      .subscribe((deploymentParameters) => {
        expect(deploymentParameters).toEqual([deploymentParamter]);
      });

    const req = httpTestingController.expectOne(
      '/AMW_rest/resources/deployments/deploymentParameterKeys/'
    );

    expect(req.request.method).toEqual('GET');

    req.flush([deploymentParamter]);
  });

  it('should get() a deployment', () => {
    service.get(123).subscribe((deployment) => {
      expect(deployment).toEqual(mockDeployment);
    });

    const req = httpTestingController.expectOne(
      '/AMW_rest/resources/deployments/123'
    );

    expect(req.request.method).toEqual('GET');
    req.flush(mockDeployment);
  });

  const mockDeployment: Deployment = {
    id: 1,
    trackingId: 123,
    state: 'final',
    deploymentDate: 1254689765564,
    deploymentJobCreationDate: 1254689765512,
    deploymentConfirmationDate: 125468976545,
    deploymentCancelDate: 1254679765564,
    reason: 'because of reasons',
    appServerName: 'haskell-backend',
    appServerId: 1,
    resourceId: 1234,
    appsWithVersion: [],
    deploymentParameters: [],
    environmentName: 'production',
    environmentNameAlias: 'prod',
    releaseName: 'alpha-release',
    runtimeName: 'ghc',
    requestUser: 'reto',
    confirmUser: 'yves',
    cancelUser: 'max',
    deploymentDelayed: false,
    selected: true,
    actions: null,
    statusMessage: 'msg',
    buildSuccess: true,
    executed: true,
    deploymentConfirmed: true,
    stateToDeploy: 1,
    sendEmailWhenDeployed: true,
    simulateBeforeDeployment: true,
    shakedownTestsWhenDeployed: true,
    neighbourhoodTest: false,
  };
});
