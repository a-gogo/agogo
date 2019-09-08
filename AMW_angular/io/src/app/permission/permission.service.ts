import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { flatMap, catchError } from 'rxjs/operators';
import { Permission } from './permission';
import { Restriction } from './restriction';
import { RestrictionsCreation } from './restrictions-creation';
import { BaseService } from '../base/base.service';

@Injectable()
export class PermissionService extends BaseService {

  constructor(private http: HttpClient) {
    super();
  }

  getAllRoleNames(): Observable<string[]> {
    return this.http
      .get<string[]>(`${this.getBaseUrl()}/permissions/restrictions/roleNames`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  getAllUserRestrictionNames(): Observable<string[]> {
    return this.http
      .get<string[]>(`${this.getBaseUrl()}/permissions/restrictions/userRestrictionNames`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  getAllPermissionEnumValues(): Observable<Permission[]> {
    return this.http
      .get<Permission[]>(`${this.getBaseUrl()}/permissions/restrictions/permissionEnumValues`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  getRoleWithRestrictions(roleName: string): Observable<Restriction[]> {
    return this.http
      .get<Restriction[]>(`${this.getBaseUrl()}/permissions/restrictions/roles/${roleName}`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  getUserWithRestrictions(userName: string): Observable<Restriction[]> {
    return this.http
      .get<Restriction[]>(`${this.getBaseUrl()}/permissions/restrictions/users/${userName}`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  getOwnUserAndRoleRestrictions(): Observable<Restriction[]> {
    return this.http
      .get<Restriction[]>(`${this.getBaseUrl()}/permissions/restrictions/ownRestrictions/`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  removeRestriction(id: number) {
    return this.http
      .delete(`${this.getBaseUrl()}/permissions/restrictions/${id}`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  updateRestriction(restriction: Restriction) {
    return this.http.put(`${this.getBaseUrl()}/permissions/restrictions/${restriction.id}`, restriction, { headers: this.postHeaders() })
      .pipe(catchError(this.handleError));
  }

  createRestriction(restriction: Restriction, delegation: boolean): Observable<Restriction> {
    const params = new HttpParams();
    params.set('delegation', delegation ? 'true' : 'false');
    return this.http.post<Restriction>(`${this.getBaseUrl()}/permissions/restrictions/`, restriction,
      {
        params: params,
        headers: this.postHeaders(),
        observe: 'response'
      })
      .pipe(
        flatMap((res: HttpResponse<Restriction>) => {
          return this.http.get<Restriction>(this.getBaseUrl() + res.headers.get('Location'));
        }
         // catchError(this.handleError) // todo: how to fix???
        )
      );
  }

  createRestrictions(restrictionsCreation: RestrictionsCreation, delegation: boolean) {
    const params = new HttpParams();
    params.set('delegation', delegation ? 'true' : 'false');
    return this.http.post(`${this.getBaseUrl()}/permissions/restrictions/multi/`, restrictionsCreation,
      {
        params: params,
        headers: this.postHeaders()
      })
      .pipe(catchError(this.handleError));
  }

}

