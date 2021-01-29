import { Component, TemplateRef } from '@angular/core';
import { DeploymentsStore } from '@core/deployments.store';
import { Environment, EnvironmentService, NavigationService } from '@core/services';
import { DeploymentParameter } from '@deployments/types';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs';
import { Deployment } from '../deployment/deployment';

@Component({
  selector: 'app-deployment-redeploy',
  templateUrl: './deployment-redeploy.component.html',
  styleUrls: ['./deployment-redeploy.component.scss'],
})
export class DeploymentRedeployComponent {
  selectedDeployments$: Observable<Deployment[]> = this.deploymentStore.selectedDeployments$;

  groupedEnvironments$: Observable<Map<string, Environment[]>> = this.environmentService.groupedEnvironments$;

  // TODO: deploymentParameters are a property of Deployments - what are deploymentParameters for multiple deployments?

  deploymentParameters: DeploymentParameter[] = [];

  constructor(
    public deploymentStore: DeploymentsStore,
    private navigationService: NavigationService,
    private environmentService: EnvironmentService,
    private modalService: NgbModal
  ) {
    this.navigationService.setPageTitle('Redeploy');
  }

  showAppserverNames(appServerNamesModal: TemplateRef<unknown>): void {
    this.modalService.open(appServerNamesModal);
  }

  // TODO: exact copy from DeploymentComponent - dry it up - move it to the deploymentStore/ Service and use an Observable?
  onAddParam(deploymentParam: DeploymentParameter): void {
    this.deploymentParameters = this.deploymentParameters.filter((d) => d.key !== deploymentParam.key);
    this.deploymentParameters.push(deploymentParam);
  }

  onChangeEnvironment(): void {
    // TODO: the original component checked if deployment is possible...
  }

  // TODO: calculate values
  hasPermissionShakedownTest = true;
  doExecuteShakedownTest = true;
}
