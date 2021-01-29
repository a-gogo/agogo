import { Component, OnInit } from '@angular/core';
import { Location } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { Deployment } from './deployment';
import { DeploymentRequest } from './deployment-request';
import { AppWithVersion } from './app-with-version';
import { Subscription } from 'rxjs';
import * as _ from 'lodash';
import { DeploymentsStore } from 'src/app/core/deployments.store';
import { Relation } from 'src/app/resource/relation';
import { Release } from 'src/app/resource/release';
import { Resource } from 'src/app/resource/resource';
import { ResourceTag } from 'src/app/resource/resource-tag';
import { ResourceService } from 'src/app/resource/resource.service';
import { DateTimeModel } from '@shared/components';
import { Environment, EnvironmentService, NavigationService } from '@core/services';
import { DeploymentService } from './deployment.service';
import { DeploymentParameter } from '@deployments/types';

@Component({
  selector: 'amw-deployment',
  templateUrl: './deployment.component.html',
})
export class DeploymentComponent implements OnInit {
  // from url
  appserverName = '';
  releaseName = '';
  // redeploy only
  deploymentId: number;

  // these are valid for all (loaded ony once)
  appservers: Resource[] = [];
  environments: Environment[] = [];
  groupedEnvironments: { [key: string]: Environment[] } = {};
  defaultResourceTag: ResourceTag = { label: 'HEAD' } as ResourceTag;
  isRedeployment = false;

  // per appserver/deployment request
  selectedAppserver: Resource = null;
  releases: Release[] = [];
  selectedRelease: Release = null;
  runtime: Relation = null;
  resourceTags: ResourceTag[] = [this.defaultResourceTag];
  selectedResourceTag: ResourceTag = this.defaultResourceTag;
  deploymentDate: DateTimeModel = null;
  appsWithVersion: AppWithVersion[] = [];
  transDeploymentParameters: DeploymentParameter[] = [];
  deploymentResponse: Deployment = {} as Deployment;
  hasPermissionShakedownTest = false;
  hasPermissionToDeploy = false;
  hasPermissionToRequestDeployment = false;

  // redeploy only
  selectedDeployment: Deployment = {} as Deployment;
  appsWithVersionForRedeployment: AppWithVersion[] = [];

  simulate = false;
  requestOnly = false;
  doSendEmail = false;
  doExecuteShakedownTest = false;
  // may only be enabled if above is true
  doNeighbourhoodTest = false;

  bestForSelectedRelease: Release = null;

  errorMessage = '';
  successMessage = '';
  isLoading = false;
  isDeploymentBlocked = false;

  constructor(
    private resourceService: ResourceService,
    private environmentService: EnvironmentService,
    private deploymentService: DeploymentService,
    private activatedRoute: ActivatedRoute,
    private location: Location,
    public navigationStore: NavigationService,
    public deploymentsStore: DeploymentsStore
  ) {
    this.navigationStore.setVisible(false);
    this.navigationStore.setCurrent('Deployments');
  }

  ngOnInit(): void {
    this.initEnvironments();
    this.activatedRoute.params.subscribe((param: string[]) => {
      this.appserverName = param['appserverName'];
      this.releaseName = param['releaseName'];
      this.deploymentId = param['deploymentId'];

      if (this.deploymentId && Number(this.deploymentId)) {
        this.prepareRedeploy();
      } else {
        this.prepareNewDeployment();
      }
    });
  }

  initAppservers(): void {
    this.isLoading = true;
    this.resourceService.getByType('APPLICATIONSERVER').subscribe(
      /* happy path */ (r) =>
        (this.appservers = r.sort(function (a, b) {
          return a.name.localeCompare(b.name, undefined, {
            sensitivity: 'base',
          });
        })),
      /* error path */ (e) => (this.errorMessage = e),
      /* onComplete */ () => {
        this.setPreselected();
        this.isLoading = false;
      }
    );
  }

  onChangeAppserver(): void {
    this.resetVars();
    this.loadReleases();
    this.canCreateShakedownTest();
    this.canDeploy();
  }

  onChangeRelease(): void {
    if (!this.selectedRelease) {
      this.selectedRelease = this.releases[0];
    }
    this.getRelatedForRelease();
    this.goTo(this.selectedAppserver.name + '/' + this.selectedRelease.release);
  }

  onChangeEnvironment(): void {
    if (this.isRedeployment) {
      this.verifyRedeployment();
    } else {
      this.getAppVersions();
    }
    this.canDeploy();
  }

  onAddParam(deploymentParam: DeploymentParameter): void {
    this.transDeploymentParameters = this.transDeploymentParameters.filter((d) => d.key !== deploymentParam.key);
    this.transDeploymentParameters.push(deploymentParam);
  }

  onRemoveParam(deParam: DeploymentParameter): void {
    _.pull(this.transDeploymentParameters, deParam);
  }

  isReadyForDeployment(): boolean {
    return (
      !this.isDeploymentBlocked &&
      this.selectedRelease &&
      this.appsWithVersion.length > 0 &&
      _.filter(this.environments, 'selected').length > 0
    );
  }

  requestDeployment(): void {
    this.requestOnly = true;
    this.prepareDeployment();
  }

  createDeployment(): void {
    this.requestOnly = false;
    this.prepareDeployment();
  }

  getEnvironmentGroups(): string[] {
    return Object.keys(this.groupedEnvironments);
  }

  private getDeployment(): Subscription {
    return this.deploymentService.get(this.deploymentId).subscribe(
      /* happy path */ (r) => (this.selectedDeployment = r),
      /* error path */ (e) => (this.errorMessage = e),
      /* onComplete */ () => this.initRedeploymentValues()
    );
  }

  private initRedeploymentValues() {
    this.isLoading = false;
    this.transDeploymentParameters = this.selectedDeployment.deploymentParameters;
    this.appsWithVersion = this.selectedDeployment.appsWithVersion;
    this.selectedAppserver = {
      id: this.selectedDeployment.appServerId,
      name: this.selectedDeployment.appServerName,
    } as Resource;
    this.loadReleases();
    this.setPreSelectedEnvironment();
    this.canDeploy();
  }

  private setPreSelectedEnvironment() {
    const env = _.find(this.environments, {
      name: this.selectedDeployment.environmentName,
    });
    if (env) {
      env.selected = true;
    }
  }

  private setSelectedRelease(): Subscription {
    return this.resourceService.getMostRelevantRelease(this.selectedAppserver.id).subscribe(
      /* happy path */ (r) => (this.selectedRelease = this.releases.find((release) => release.release === r.release)),
      /* error path */ (e) => (this.errorMessage = e),
      /* onComplete */ () => this.onChangeRelease()
    );
  }

  private setSelectedReleaseForRedeployment() {
    this.selectedRelease = this.releases.find((release) => release.release === this.selectedDeployment.releaseName);
    // will perform verifyRedeployment()
    this.getAppVersions();
  }

  private loadReleases(): Subscription {
    return this.resourceService.getDeployableReleases(this.selectedAppserver.id).subscribe(
      /* happy path */ (r) => (this.releases = r),
      /* error path */ (e) => (this.errorMessage = e),
      /* onComplete */ () =>
        this.isRedeployment ? this.setSelectedReleaseForRedeployment() : this.setSelectedRelease()
    );
  }

  private getRelatedForRelease() {
    this.resourceService.getLatestForRelease(this.selectedAppserver.id, this.selectedRelease.id).subscribe(
      /* happy path */ (r) => (this.bestForSelectedRelease = r),
      /* error path */ (e) => (this.errorMessage = e),
      /* onComplete */ () => this.extractFromRelations()
    );
  }

  private extractFromRelations() {
    this.runtime = _.filter(this.bestForSelectedRelease.relations, {
      type: 'RUNTIME',
    }).pop();
    this.resourceTags = this.resourceTags.concat(this.bestForSelectedRelease.resourceTags);
    this.appsWithVersion = [];
    this.getAppVersions();
  }

  private getAppVersions() {
    this.resourceService
      .getAppsWithVersions(
        this.selectedAppserver.id,
        this.selectedRelease.id,
        _.filter(this.environments, 'selected').map((val) => val.id)
      )
      .subscribe(
        /* happy path */ (r) =>
          this.isRedeployment ? (this.appsWithVersionForRedeployment = r) : (this.appsWithVersion = r),
        /* error path */ (e) => (this.errorMessage = e),
        /* onComplete */ () => (this.isRedeployment ? this.verifyRedeployment() : '')
      );
  }

  private resetVars() {
    this.errorMessage = '';
    this.successMessage = '';
    this.isDeploymentBlocked = false;
    this.hasPermissionShakedownTest = false;
    this.selectedRelease = null;
    this.bestForSelectedRelease = null;
    this.resourceTags = [this.defaultResourceTag];
    this.selectedResourceTag = this.defaultResourceTag;
    this.deploymentDate = null;
    this.simulate = false;
    this.doSendEmail = false;
    this.doExecuteShakedownTest = false;
    this.doNeighbourhoodTest = false;
    this.appsWithVersion = [];
    this.transDeploymentParameters = [];
  }

  private canCreateShakedownTest() {
    this.resourceService.canCreateShakedownTest(this.selectedAppserver.id).subscribe(
      /* happy path */ (r) => (this.hasPermissionShakedownTest = r),
      /* error path */ (e) => (this.errorMessage = e)
    );
  }

  private canDeploy() {
    if (this.selectedAppserver != null) {
      this.hasPermissionToDeploy = false;
      const contextIds: number[] = _.filter(this.environments, 'selected').map((val) => val.id);
      if (contextIds.length > 0) {
        this.deploymentService.canDeploy(this.selectedAppserver.id, contextIds).subscribe(
          /* happy path */ (r) => (this.hasPermissionToDeploy = r),
          /* error path */ (e) => (this.errorMessage = e),
          /* onComplete */ () => this.canRequestDeployment(contextIds)
        );
      }
    }
  }

  private canRequestDeployment(contextIds: number[]) {
    if (this.selectedAppserver != null) {
      this.hasPermissionToRequestDeployment = false;
      if (contextIds.length > 0) {
        this.deploymentService.canRequestDeployment(this.selectedAppserver.id, contextIds).subscribe(
          /* happy path */ (r) => (this.hasPermissionToRequestDeployment = r),
          /* error path */ (e) => (this.errorMessage = e)
        );
      }
    }
  }

  private verifyRedeployment() {
    this.errorMessage = '';
    this.isDeploymentBlocked = false;
    this.appsWithVersion.forEach((originApp: AppWithVersion) => {
      const actualApp: AppWithVersion = _.find(this.appsWithVersionForRedeployment, [
        'applicationName',
        originApp.applicationName,
      ]);
      if (!this.isDeploymentBlocked && !actualApp) {
        this.errorMessage = 'Application <strong>' + originApp.applicationName + '</strong> does not exist anymore';
        this.isDeploymentBlocked = true;
      }
    });
  }

  private prepareDeployment() {
    if (this.isReadyForDeployment()) {
      const contextIds: number[] = _.filter(this.environments, 'selected').map((val) => val.id);
      this.createDeploymentRequest(contextIds);
    }
  }

  private createDeploymentRequest(contextIds: number[]) {
    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';
    const deploymentRequest: DeploymentRequest = {} as DeploymentRequest;
    deploymentRequest.appServerName = this.selectedAppserver.name;
    deploymentRequest.releaseName = this.selectedRelease.release;
    deploymentRequest.contextIds = contextIds;
    deploymentRequest.simulate = this.simulate;
    deploymentRequest.sendEmail = this.doSendEmail;
    deploymentRequest.executeShakedownTest = this.doExecuteShakedownTest;
    deploymentRequest.neighbourhoodTest = this.doNeighbourhoodTest;
    deploymentRequest.requestOnly = this.requestOnly;
    deploymentRequest.appsWithVersion = this.appsWithVersion;
    if (!this.isRedeployment) {
      deploymentRequest.stateToDeploy =
        this.selectedResourceTag && this.selectedResourceTag.tagDate
          ? this.selectedResourceTag.tagDate
          : new Date().getTime();
    }
    if (this.deploymentDate) {
      deploymentRequest.deploymentDate = this.deploymentDate.toEpoch();
    }
    if (this.transDeploymentParameters.length > 0) {
      deploymentRequest.deploymentParameters = this.transDeploymentParameters;
    }
    this.deploymentService.createDeployment(deploymentRequest).subscribe(
      /* happy path */ (r) => (this.deploymentResponse = r),
      /* error path */ (e) => (this.errorMessage = e),
      /* onComplete */ () => {
        this.isLoading = false;
        this.composeSuccessMessage();
      }
    );
  }

  private composeSuccessMessage() {
    let link: string;
    if (this.deploymentService.isAngularDeploymentsGuiActive()) {
      link =
        '<a href="#/deployments?filters=[{%22name%22:%22Tracking%20Id%22,%22val%22:%22' +
        this.deploymentResponse.trackingId +
        '%22}]">';
    } else {
      link = '<a href="/AMW_web/pages/deploy.xhtml?tracking_id=' + this.deploymentResponse.trackingId + '">';
    }
    link += 'Tracking Id ' + this.deploymentResponse.trackingId + '</a>';
    this.successMessage = 'Deployment created: <strong>' + link + '</strong>';
  }

  private initEnvironments() {
    this.isLoading = true;
    this.environmentService.getAll().subscribe(
      /* happy path */ (r) => {
        this.environments = r;
        this.extractEnvironmentGroups();
      },
      /* error path */ (e) => (this.errorMessage = e),
      /* onComplete */ () => (this.isLoading = false)
    );
  }

  private extractEnvironmentGroups() {
    this.environments.forEach((environment) => {
      if (!this.groupedEnvironments[environment['parent']]) {
        this.groupedEnvironments[environment['parent']] = [];
      }
      this.groupedEnvironments[environment['parent']].push(environment);
    });
  }

  // for url params only
  private setPreselected() {
    if (this.appserverName) {
      this.selectedAppserver = _.find(this.appservers, {
        name: this.appserverName,
      });
      if (this.selectedAppserver) {
        this.resourceService.getDeployableReleases(this.selectedAppserver.id).subscribe(
          /* happy path */ (r) => (this.releases = r),
          /* error path */ (e) => (this.errorMessage = e),
          /* onComplete */ () => this.setRelease()
        );
      }
    }
  }

  private prepareNewDeployment() {
    if (this.deploymentId) {
      this.appserverName = this.deploymentId.toString();
      delete this.deploymentId;
    }
    this.navigationStore.setPageTitle('Create new deployment');
    this.initAppservers();
  }

  private prepareRedeploy() {
    this.navigationStore.setPageTitle('Redeploy');
    this.isRedeployment = true;
    this.getDeployment();
  }

  // for url params only
  private setRelease() {
    if (this.releaseName) {
      this.selectedRelease = this.releases.find((release) => release.release === this.releaseName);
      this.onChangeRelease();
    }
  }

  private goTo(destination: string) {
    this.location.go('/deployment/' + destination);
  }
}
