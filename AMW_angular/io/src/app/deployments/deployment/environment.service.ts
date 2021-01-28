import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Environment } from './environment';
import { BaseService } from '../../base/base.service';

@Injectable()
export class EnvironmentService extends BaseService {
  constructor(private http: HttpClient) {
    super();
  }

  getAll(): Observable<Environment[]> {
    return this.getEnvironments(false);
  }

  getAllIncludingGroups(): Observable<Environment[]> {
    return this.getEnvironments(true);
  }

  private getEnvironments(includingGroups: boolean): Observable<Environment[]> {
    let params = new HttpParams();
    const headers = this.getHeaders();

    if (includingGroups) {
      params = params.set('includingGroups', 'true');
    }
    return this.http
      .get<Environment[]>(`${this.getBaseUrl()}/environments`, {
        params,
        headers,
      })
      .pipe(catchError(this.handleError));
  }
}
