import { Component, Input, OnInit } from '@angular/core';
import { Deployment } from '../deployment/deployment';

@Component({
  selector: 'app-appserver-displayname',
  template: ` <div *ngIf="deployment">
      <h5 class="appservername">{{ deployment.appServerName }} ({{ deployment.releaseName }})</h5>
      <div *ngFor="let appWithVersion of deployment.appsWithVersion">
        <h6>{{ appWithVersion.applicationName }} ({{ appWithVersion.version }})</h6>
      </div>
    </div>
    <div *ngIf="!deployment">
      no deployment selected
    </div>`,
  styles: [],
})
export class AppServerDisplayNameComponent {
  @Input()
  deployment: Deployment;
}
