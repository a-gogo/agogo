import { Location } from '@angular/common';
import { Component, OnDestroy, OnInit, ViewEncapsulation } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { combineLatest, merge, Observable, of, Subject } from 'rxjs';
import { catchError, distinctUntilChanged, map, shareReplay, switchMap, takeUntil } from 'rxjs/operators';
import { NavigationStoreService } from 'src/app/navigation/navigation-store.service';
import { Deployment } from '../deployment/deployment';
import { DeploymentService } from '../deployment/deployment.service';
import { DeploymentLog, DeploymentLogContent } from './deployment-log';
import { DeploymentLogsService } from './deployment-logs.service';

declare var CodeMirror: any;

type Failed = 'failed';

function failed(): Observable<Failed> {
  return of('failed');
}

@Component({
  selector: 'app-logs',
  styleUrls: ['./deployment-logs.component.scss'],
  templateUrl: './deployment-logs.component.html',
  encapsulation: ViewEncapsulation.None,
})
export class DeploymentLogsComponent implements OnInit, OnDestroy {
  constructor(
    private deploymentLogsService: DeploymentLogsService,
    private deploymentService: DeploymentService,
    private route: ActivatedRoute,
    private location: Location,
    private navigationStore: NavigationStoreService
  ) {
    this.pagetitle$.subscribe((title) => this.navigationStore.setPageTitle(title));
  }

  selectDeploymentLog$: Subject<DeploymentLog> = new Subject();

  deploymentId$: Observable<number> = this.route.paramMap.pipe(
    map((params) => +params.get('deploymentId')),
    distinctUntilChanged()
  );

  fileName$: Observable<string> = this.route.paramMap.pipe(
    map((params) => params.get('fileName')),
    distinctUntilChanged()
  );

  deployment$: Observable<Deployment | Failed> = this.deploymentId$.pipe(
    switchMap(this.loadDeployment.bind(this)),
    shareReplay(1)
  );

  deploymentLogMetaData$: Observable<DeploymentLog[]> = this.deployment$.pipe(
    switchMap(this.loadDeploymentLogs.bind(this)),
    shareReplay(1)
  );

  currentDeploymentLog$: Observable<DeploymentLog> = merge(
    combineLatest([this.fileName$, this.deploymentLogMetaData$]).pipe(
      map(([filename, meta]) => (!filename ? meta[0] : meta.find((m) => m.filename === filename)))
    ),
    this.selectDeploymentLog$
  ).pipe(distinctUntilChanged());

  currentDeploymentLogContent$: Observable<DeploymentLogContent> = this.currentDeploymentLog$.pipe(
    switchMap(this.loadDeploymentLogContent.bind(this))
  );

  pagetitle$: Observable<string> = this.deployment$.pipe(
    map((deployment) =>
      deployment === 'failed'
        ? ``
        : `Log file for ${deployment.id} (${deployment.appServerName}
          ${deployment.releaseName})`
    )
  );

  private destroy$ = new Subject();
  ngOnInit(): void {
    this.currentDeploymentLog$
      .pipe(takeUntil(this.destroy$))
      .subscribe((current) =>
        this.location.replaceState(`/deployments/${current.deploymentId}/logs/${current.filename}`)
      );
    CodeMirror.defineSimpleMode('simplemode', {
      start: [
        {
          regex: /^.*\b(error|failure|failed|fatal|not found)\b.*$/i,
          token: 'error',
        },
        {
          regex: /^.*\b(warn.*)\b.*$/i,
          token: 'warning',
        },
      ],
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
  }

  selectFile(deploymentLogMetaData: DeploymentLog) {
    this.selectDeploymentLog$.next(deploymentLogMetaData);
  }

  loadDeployment(deploymentId) {
    return this.deploymentService.get(deploymentId).pipe(catchError(() => failed()));
  }

  loadDeploymentLogs(deployment: Deployment | Failed) {
    return deployment === 'failed' || deployment === undefined
      ? of([])
      : this.deploymentLogsService.getLogFileMetaData(deployment.id).pipe(catchError(() => failed()));
  }

  loadDeploymentLogContent(deploymentLog: DeploymentLog | Failed) {
    return deploymentLog === 'failed' || deploymentLog === undefined
      ? of('')
      : this.deploymentLogsService.getLogFileContent(deploymentLog).pipe(
          map((content) => {
            return { content };
          }),
          catchError(() => failed())
        );
  }
}
