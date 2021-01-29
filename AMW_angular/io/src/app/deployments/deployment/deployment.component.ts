import { Component, OnInit, AfterViewInit } from '@angular/core';
import { Location } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { Deployment } from './deployment';
import { DeploymentParameter } from './deployment-parameter';
import { DeploymentService } from './deployment.service';
import { EnvironmentService } from './environment.service';
import { Environment } from './environment';
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
import { NavigationService } from '@core/services';

@Component({
  selector: 'amw-deployment',
  templateUrl: './deployment.component.html',
})
export class DeploymentComponent implements OnInit, AfterViewInit {
  // from url
  appserverName: string = '';
  releaseName: string = '';
  // redeploy only
  deploymentId: number;

  // these are valid for all (loaded ony once)
  appservers: Resource[] = [];
  environments: Environment[] = [];
  groupedEnvironments: { [key: string]: Environment[] } = {};
  deploymentParameters: DeploymentParameter[] = [];
  defaultResourceTag: ResourceTag = { label: 'HEAD' } as ResourceTag;
  isRedeployment: boolean = false;

  // per appserver/deployment request
  selectedAppserver: Resource = null;
  releases: Release[] = [];
  selectedRelease: Release = null;
  runtime: Relation = null;
  resourceTags: ResourceTag[] = [this.defaultResourceTag];
  selectedResourceTag: ResourceTag = this.defaultResourceTag;
  deploymentDate: DateTimeModel = null;
  appsWithVersion: AppWithVersion[] = [];
  transDeploymentParameter: DeploymentParameter = {} as DeploymentParameter;
  transDeploymentParameters: DeploymentParameter[] = [];
  deploymentResponse: any = {};
  hasPermissionShakedownTest: boolean = false;
  hasPermissionToDeploy: boolean = false;
  hasPermissionToRequestDeployment: boolean = false;

  // redeploy only
  selectedDeployment: Deployment = {} as Deployment;
  appsWithVersionForRedeployment: AppWithVersion[] = [];

  simulate: boolean = false;
  requestOnly: boolean = false;
  doSendEmail: boolean = false;
  doExecuteShakedownTest: boolean = false;
  // may only be enabled if above is true
  doNeighbourhoodTest: boolean = false;

  bestForSelectedRelease: Release = null;

  errorMessage: string = '';
  successMessage: string = '';
  isLoading: boolean = false;
  isDeploymentBlocked: boolean = false;

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

  ngOnInit() {
    this.initEnvironments();
    this.activatedRoute.params.subscribe((param: any) => {
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

  ngAfterViewInit() {
    // we dont need this right away
    this.loadDeploymentParameters();
  }

  initAppservers() {
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

  onChangeAppserver() {
    this.resetVars();
    this.loadReleases();
    this.canCreateShakedownTest();
    this.canDeploy();
  }

  onChangeRelease() {
    if (!this.selectedRelease) {
      this.selectedRelease = this.releases[0];
    }
    this.getRelatedForRelease();
    this.goTo(this.selectedAppserver.name + '/' + this.selectedRelease.release);
  }

  onChangeEnvironment() {
    if (!this.isRedeployment) {
      this.getAppVersions();
    } else {
      this.verifyRedeployment();
    }
    this.canDeploy();
  }

  onAddParam() {
    _.remove(this.transDeploymentParameters, {
      key: this.transDeploymentParameter.key,
    });
    this.transDeploymentParameters.push(this.transDeploymentParameter);
    this.transDeploymentParameter = {} as DeploymentParameter;
  }

  onRemoveParam(deParam: DeploymentParameter) {
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

  requestDeployment() {
    this.requestOnly = true;
    this.prepareDeployment();
  }

  createDeployment() {
    this.requestOnly = false;
    this.prepareDeployment();
  }

  getEnvironmentGroups() {
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
    this.transDeploymentParameter = {} as DeploymentParameter;
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

  private loadDeploymentParameters() {
    this.deploymentService.getAllDeploymentParameterKeys().subscribe(
      /* happy path */ (r) =>
        (this.deploymentParameters = r.sort(function (a, b) {
          return a.key.localeCompare(b.key, undefined, { sensitivity: 'base' });
        })),
      /* error path */ (e) => (this.errorMessage = e)
    );
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
