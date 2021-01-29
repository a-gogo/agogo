import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { BaseService } from '@base/base.service';
import { DeploymentParameter } from '@deployments/types';

@Injectable({
  providedIn: 'root',
})
export class DeploymentParameterService extends BaseService {
  constructor(private http: HttpClient) {
    super();
  }

  allDeploymentParameters$: Observable<DeploymentParameter[]> = this.getAllDeploymentParameterKeys().pipe(
    map((parameters) => parameters.sort((a, b) => a.key.localeCompare(b.key, undefined, { sensitivity: 'base' })))
  );

  private getAllDeploymentParameterKeys(): Observable<DeploymentParameter[]> {
    return this.http
      .get<DeploymentParameter[]>(`${this.getBaseUrl()}/deployments/deploymentParameterKeys/`, {
        headers: this.getHeaders(),
      })
      .pipe(catchError(this.handleError));
  }
}
