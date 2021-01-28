import { RouterModule } from '@angular/router';
import { DeploymentsComponent } from './deployments.component';
import { NgModule } from '@angular/core';
import { DeploymentContainerComponent } from './deployment-container/deployment-container.component';
import { DeploymentLogsComponent } from './logs/deployment-logs.component';
import { DeploymentComponent } from './deployment/deployment.component';

@NgModule({
  imports: [
    RouterModule.forChild([
      {
        path: 'deployments',
        component: DeploymentContainerComponent,
        children: [
          { path: '', component: DeploymentsComponent },
          { path: ':deploymentId/logs', component: DeploymentLogsComponent },
          { path: ':deploymentId/logs/:fileName', component: DeploymentLogsComponent },
        ],
      },
      { path: 'deployment', component: DeploymentComponent },
      { path: 'deployment/:deploymentId', component: DeploymentComponent },
      {
        path: 'deployment/:appserverName/:releaseName',
        component: DeploymentComponent,
      },
    ]),
  ],
  exports: [RouterModule],
})
export class DeploymentsRoutingModule {}
