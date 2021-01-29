import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { BaseService } from '@base/base.service';

export interface Environment {
  id: number;
  name: string;
  nameAlias: string;
  parent: string;
  selected: boolean;
  disabled: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class EnvironmentService extends BaseService {
  groupedEnvironments$: Observable<Map<string, Environment[]>> = this.getAllIncludingGroups().pipe(
    map((environments) => {
      const groupNames = environments
        .filter((environment) => environment.parent === 'Global')
        .map((environment) => environment.name);
      const map = new Map();
      groupNames.forEach((groupName) => {
        map.set(
          groupName,
          environments.filter((environment) => environment.parent === groupName)
        );
      });
      return map;
    })
  );

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
