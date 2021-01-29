import { Component } from '@angular/core';
import { DeploymentsStore } from '@core/deployments.store';
import { NavigationService } from '@core/services';

@Component({
  selector: 'app-deployment-container',
  template: ` <router-outlet></router-outlet> `,
  styles: [],
})
export class DeploymentContainerComponent {
  constructor(public navigationStore: NavigationService, private deploymentsStore: DeploymentsStore) {
    this.navigationStore.setPageTitle('Deployments');
    this.navigationStore.setCurrent('Deployments');
  }
}
