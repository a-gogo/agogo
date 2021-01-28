import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { Resource } from './resource';
import { ResourceType } from './resource-type';
import { Release } from './release';
import { Relation } from './relation';
import { Property } from './property';
import { BaseService } from '../base/base.service';
import { AppWithVersion } from '../deployments/deployment/app-with-version';

interface Named {
  name: string;
}

@Injectable()
export class ResourceService extends BaseService {
  constructor(private http: HttpClient) {
    super();
  }

  getAll(): Observable<Resource[]> {
    return this.http
      .get<Resource[]>(`${this.getBaseUrl()}/resources`, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((resources) => resources.map(toResource)),
        catchError(this.handleError)
      );
  }

  getResourceName(resourceId: number): Observable<Named> {
    return this.http
      .get<Named>(`${this.getBaseUrl()}/resources/name/${resourceId}`, {
        headers: this.getHeaders(),
      })
      .pipe(catchError(this.handleError));
  }

  get(resourceGroupName: string): Observable<Resource> {
    return this.http
      .get(`${this.getBaseUrl()}/resources/${resourceGroupName}`, {
        headers: this.getHeaders(),
      })
      .pipe(map(toResource), catchError(this.handleError));
  }

  resourceExists(resourceId: number): Observable<Resource> {
    return this.http
      .get<Resource>(`${this.getBaseUrl()}/resources/exists/${resourceId}`, {
        headers: this.getHeaders(),
      })
      .pipe(catchError(this.handleError));
  }

  getAllResourceGroups(): Observable<Resource[]> {
    return this.http
      .get<Resource[]>(`${this.getBaseUrl()}/resources/resourceGroups`, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((resources) => resources.map(toResource)),
        catchError(this.handleError)
      );
  }

  getAllResourceTypes(): Observable<ResourceType[]> {
    return this.http
      .get<ResourceType[]>(`${this.getBaseUrl()}/resources/resourceTypes`, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((resources) => resources.map(toResource)),
        catchError(this.handleError)
      );
  }

  getByType(type: string): Observable<Resource[]> {
    return this.http
      .get<Resource[]>(`${this.getBaseUrl()}/resources?type=${type}`, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((resources) => resources.map(toResource)),
        catchError(this.handleError)
      );
  }

  getLatestForRelease(resourceGroupId: number, releaseId: number): Observable<Release> {
    return this.http
      .get<Release>(`${this.getBaseUrl()}/resources/resourceGroups/${resourceGroupId}/releases/${releaseId}`, {
        headers: this.getHeaders(),
      })
      .pipe(catchError(this.handleError));
  }

  getRuntime(resourceGroupName: string, releaseName: string): Observable<Relation[]> {
    const params = new HttpParams().set('type', 'RUNTIME');
    const headers = this.getHeaders();
    return this.http
      .get<Relation[]>(`${this.getBaseUrl()}/resources/${resourceGroupName}/${releaseName}/relations`, {
        params,
        headers,
      })
      .pipe(catchError(this.handleError));
  }

  getProperty(resourceGroupName: string, releaseName: string, propertyName: string): Observable<Property> {
    return this.http
      .get<Property>(`${this.getBaseUrl()}/resources/${resourceGroupName}/${releaseName}/properties/${propertyName}`, {
        headers: this.getHeaders(),
      })
      .pipe(catchError(this.handleError));
  }

  getDeployableReleases(resourceGroupId: number): Observable<Release[]> {
    return this.http
      .get<Release[]>(`${this.getBaseUrl()}/resources/resourceGroups/${resourceGroupId}/releases/`, {
        headers: this.getHeaders(),
      })
      .pipe(catchError(this.handleError));
  }

  getMostRelevantRelease(resourceGroupId: number): Observable<Release> {
    return this.http
      .get<Release>(`${this.getBaseUrl()}/resources/resourceGroups/${resourceGroupId}/releases/mostRelevant/`, {
        headers: this.getHeaders(),
      })
      .pipe(catchError(this.handleError));
  }

  getAppsWithVersions(
    resourceGroupId: number,
    releaseId: number,
    environmentIds: number[]
  ): Observable<AppWithVersion[]> {
    let params = new HttpParams();
    environmentIds.forEach((id) => (params = params.append('context', String(id))));
    return this.http
      .get<AppWithVersion[]>(
        `${this.getBaseUrl()}/resources/resourceGroups/${resourceGroupId}/releases/${releaseId}/appWithVersions/`,
        {
          params: params,
          headers: this.getHeaders(),
        }
      )
      .pipe(
        map((apps) => apps.map(toAppWithVersion)),
        catchError(this.handleError)
      );
  }

  canCreateShakedownTest(resourceGroupId: number): Observable<boolean> {
    return this.http
      .get<boolean>(`${this.getBaseUrl()}/resources/resourceGroups/${resourceGroupId}/canCreateShakedownTest`, {
        headers: this.getHeaders(),
      })
      .pipe(catchError(this.handleError));
  }
}

function toAppWithVersion(r: any): AppWithVersion {
  delete r.mavenVersion;
  return r;
}

function toResource(r: Resource): Resource {
  r.release = r.release && toRelease(r.release);
  r.releases = (r.releases || []).map(toRelease);
  return r;
}

function toRelease(r: any): Release {
  delete r.templates;
  return r;
}
