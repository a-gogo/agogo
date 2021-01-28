import { Component, OnInit } from '@angular/core';
import { NavigationService } from '@core/services';

@Component({
  selector: 'app-deployment-container',
  template: ` <router-outlet></router-outlet> `,
  styles: [],
})
export class DeploymentContainerComponent implements OnInit {
  constructor(public navigationStore: NavigationService) {
    this.navigationStore.setPageTitle('Deployments');
    this.navigationStore.setCurrent('Deployments');
  }

  ngOnInit(): void {}
}
