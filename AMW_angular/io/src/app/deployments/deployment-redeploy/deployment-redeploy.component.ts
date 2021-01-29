import { Component, TemplateRef } from '@angular/core';
import { DeploymentsStore } from '@core/deployments.store';
import { EnvironmentService, NavigationService } from '@core/services';
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

  environments$ = this.environmentService.getAllIncludingGroups();

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
}
