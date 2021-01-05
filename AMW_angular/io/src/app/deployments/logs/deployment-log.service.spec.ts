import { HttpErrorResponse } from '@angular/common/http';
import { defer } from 'rxjs';
import { DeploymentLog } from './deployment-log';
import { DeploymentLogService } from './deployment-log.service';

/** Create async observable that emits-once and completes
 *  after a JS engine turn */
export function asyncData<T>(data: T) {
  return defer(() => Promise.resolve(data));
}

/**
 * Create async observable error that errors
 * after a JS engine turn
 */
export function asyncError<T>(errorObject: any) {
  return defer(() => Promise.reject(errorObject));
}

describe('DeploymentLogService', () => {
  let service: DeploymentLogService;
  let httpClientSpy: { get: jasmine.Spy };

  beforeEach(() => {
    httpClientSpy = jasmine.createSpyObj('HttpClient', ['get']);
    service = new DeploymentLogService(httpClientSpy as any);
  });

  it('should return expected deployment logs', () => {
    const expectedDeploymentLogs: DeploymentLog[] = [
      { deploymentId: 1, content: 'content 1', filename: 'log1.log' },
      { deploymentId: 1, content: 'content 2', filename: 'log2.log' },
    ];

    httpClientSpy.get.and.returnValue(asyncData(expectedDeploymentLogs));

    service
      .get(1)
      .subscribe(
        (deploymentLogs) =>
          expect(deploymentLogs).toEqual(
            expectedDeploymentLogs,
            'expected deploymentLogs'
          ),
        fail
      );

    expect(httpClientSpy.get.calls.count()).toBe(1, 'called once');
  });

  it('should handle 404', () => {
    const errorResponse = new HttpErrorResponse({
      error: { message: 'no deployment logs found' },
      status: 404,
      statusText: 'Not Found',
    });

    httpClientSpy.get.and.returnValue(asyncError(errorResponse));

    service.get(1).subscribe(
      (_) => fail('expected an error, not a list of deploymentLogs'),
      (error) => expect(error).toContain('no deployment logs found')
    );
  });
});
