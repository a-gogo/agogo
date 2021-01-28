import { Component, OnInit } from '@angular/core';
import { NavigationStoreService } from '@core/services';

@Component({
  selector: 'app-deployment-container',
  template: ` <router-outlet></router-outlet> `,
  styles: [],
})
export class DeploymentContainerComponent implements OnInit {
  constructor(public navigationStore: NavigationStoreService) {
    this.navigationStore.setPageTitle('Deployments');
    this.navigationStore.setCurrent('Deployments');
  }

  ngOnInit(): void {}
}
