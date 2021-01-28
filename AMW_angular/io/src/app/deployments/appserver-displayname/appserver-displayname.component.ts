import { Component, Input, OnInit } from '@angular/core';
import { Deployment } from '../deployment/deployment';

@Component({
  selector: 'app-appserver-displayname',
  template: ` <h5>{{ deployment.appServerName }} ({{ deployment.releaseName }})</h5>
    <div *ngFor="let appWithVersion of deployment.appsWithVersion">
      <h6>{{ appWithVersion.applicationName }} ({{ appWithVersion.version }})</h6>
    </div>`,
  styles: [],
})
export class AppServerDisplayNameComponent implements OnInit {
  @Input()
  deployment: Deployment;

  constructor() {
    console.log(this.deployment);
  }
  ngOnInit(): void {
    console.log(this.deployment);
  }
}
