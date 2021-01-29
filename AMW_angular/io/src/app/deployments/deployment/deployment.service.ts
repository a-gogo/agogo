import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { ComparatorFilterOption } from './comparator-filter-option';
import { Deployment } from './deployment';
import { DeploymentFilterType } from './deployment-filter-type';
import { DeploymentRequest } from './deployment-request';
import { BaseService } from '../../base/base.service';

@Injectable()
export class DeploymentService extends BaseService {
  constructor(private http: HttpClient) {
    super();
  }

  getFilteredDeployments(
    filterString: string,
    sortCol: string,
    sortDir: string,
    offset: number,
    maxResults: number
  ): Observable<{ deployments: Deployment[]; total: number }> {
    const paramObj = {
      filters: filterString,
      colToSort: sortCol,
      sortDirection: sortDir,
      offset: String(offset),
      maxResults: String(maxResults),
    };
    const params = new HttpParams({ fromObject: paramObj });
    return this.http
      .get<Deployment[]>(`${this.getBaseUrl()}/deployments/filter`, {
        headers: new HttpHeaders({
          Accept: 'application/json',
        }),
        params: params,
        observe: 'response',
      })
      .pipe(
        map((data) => this.extractDeploymentsAndTotalCount(data)),
        catchError(this.handleError)
      );
  }

  getFilteredDeploymentsForCsvExport(filterString: string, sortCol: string, sortDir: string): Observable<ArrayBuffer> {
    const params = new HttpParams()
      .append('filters', filterString)
      .append('colToSort', sortCol)
      .append('sortDirection', sortDir);

    const headers = this.csvHeaders();

    return this.http
      .get(`${this.getBaseUrl()}/deployments/filter`, {
        params,
        headers,
        responseType: 'arraybuffer',
      })
      .pipe(catchError(this.handleError));
  }

  get(deploymentId: number): Observable<Deployment> {
    return this.http
      .get<Deployment>(`${this.getBaseUrl()}/deployments/${deploymentId}`, {
        headers: this.getHeaders(),
      })
      .pipe(catchError(this.handleError));
  }

  getWithActions(deploymentId: number): Observable<Deployment> {
    return this.http
      .get<Deployment>(`${this.getBaseUrl()}/deployments/${deploymentId}/withActions`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  createDeployment(deploymentRequest: DeploymentRequest): Observable<Deployment> {
    return this.http
      .post<Deployment>(`${this.getBaseUrl()}/deployments`, deploymentRequest, {
        headers: this.postHeaders(),
      })
      .pipe(catchError(this.handleError));
  }

  cancelDeployment(deploymentId: number): Observable<unknown> {
    return this.http
      .put(`${this.getBaseUrl()}/deployments/${deploymentId}/updateState`, 'canceled', { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  confirmDeployment(deployment: Deployment): Observable<unknown> {
    return this.http
      .put(`${this.getBaseUrl()}/deployments/${deployment.id}/confirm`, deployment, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  rejectDeployment(deploymentId: number): Observable<unknown> {
    return this.http
      .put(`${this.getBaseUrl()}/deployments/${deploymentId}/updateState`, 'rejected', { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  canDeploy(resourceGroupId: number, contextIds: number[]): Observable<boolean> {
    let params = new HttpParams();
    contextIds.forEach((key) => (params = params.append('contextId', String(key))));
    const headers = this.getHeaders();

    return this.http
      .get<boolean>(`${this.getBaseUrl()}/deployments/canDeploy/${resourceGroupId}`, {
        params,
        headers,
      })
      .pipe(catchError(this.handleError));
  }

  canRequestDeployment(resourceGroupId: number, contextIds: number[]): Observable<boolean> {
    let params = new HttpParams();

    contextIds.forEach((key) => (params = params.append('contextId', String(key))));
    const headers = this.getHeaders();
    return this.http
      .get<boolean>(`${this.getBaseUrl()}/deployments/canRequestDeployment/${resourceGroupId}`, {
        params,
        headers,
      })
      .pipe(catchError(this.handleError));
  }

  canRequestDeployments(): Observable<boolean> {
    return this.http
      .get<boolean>(`${this.getBaseUrl()}/deployments/canRequestDeployment/`, {
        headers: this.getHeaders(),
      })
      .pipe(catchError(this.handleError));
  }

  getAllDeploymentFilterTypes(): Observable<DeploymentFilterType[]> {
    return this.http
      .get<DeploymentFilterType[]>(`${this.getBaseUrl()}/deployments/deploymentFilterTypes/`, {
        headers: this.getHeaders(),
      })
      .pipe(catchError(this.handleError));
  }

  getAllComparatorFilterOptions(): Observable<ComparatorFilterOption[]> {
    return this.http
      .get<ComparatorFilterOption[]>(`${this.getBaseUrl()}/deployments/comparatorFilterOptions/`, {
        headers: this.getHeaders(),
      })
      .pipe(catchError(this.handleError));
  }

  getFilterOptionValues(filterName: string): Observable<string[]> {
    const params = new HttpParams().append('filterName', filterName);
    const headers = this.getHeaders();
    return this.http
      .get<string[]>(`${this.getBaseUrl()}/deployments/filterOptionValues`, {
        params,
        headers,
      })
      .pipe(catchError(this.handleError));
  }

  setDeploymentDate(deploymentId: number, deploymentDate: number): Observable<unknown> {
    return this.http
      .put(`${this.getBaseUrl()}/deployments/${deploymentId}/date`, deploymentDate, { headers: this.postHeaders() })
      .pipe(catchError(this.handleError));
  }

  isAngularDeploymentsGuiActive(): Observable<boolean> {
    return this.http
      .get<boolean>(`${this.getBaseUrl()}/deployments/isAngularDeploymentsGuiActive/`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  private csvHeaders() {
    return new HttpHeaders().append('Accept', 'text/csv');
  }

  private extractDeploymentsAndTotalCount(res: HttpResponse<Deployment[]>) {
    const headerField = 'X-Total-Count';
    const ob: { deployments: Deployment[]; total: number } = {
      deployments: [],
      total: 0,
    };
    ob.deployments = res.body;
    ob.total = res.headers.get(headerField) ? parseInt(res.headers.get(headerField), 10) : 0;
    return ob;
  }
}
