import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DeploymentsComponent } from './deployments.component';
import { DeploymentsListComponent } from './deployments-list.component';
import { SharedModule } from '@shared/shared.module';
import { DeploymentsEditModalComponent } from './deployments-edit-modal.component';
import { DeploymentsRoutingModule } from './deployments-routing.module';
import { DeploymentContainerComponent } from './deployment-container/deployment-container.component';
import { NgbActiveModal, NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { CodemirrorModule } from '@ctrl/ngx-codemirror';
import { DeploymentLogsComponent } from './logs/deployment-logs.component';
import { DeploymentComponent } from './deployment/deployment.component';
import { DeploymentRedeployComponent } from './deployment-redeploy/deployment-redeploy.component';
import { DeploymentService } from './deployment/deployment.service';
import { CoreModule } from '@core/core.module';
import { AppServerDisplayNameComponent } from './appserver-displayname/appserver-displayname.component';
import { ParameterComponent } from './parameter/parameter.component';

@NgModule({
  imports: [CommonModule, CoreModule, DeploymentsRoutingModule, SharedModule, NgbModule, CodemirrorModule],
  declarations: [
    AppServerDisplayNameComponent,
    DeploymentComponent,
    DeploymentsComponent,
    DeploymentsListComponent,
    DeploymentsEditModalComponent,
    DeploymentLogsComponent,
    DeploymentContainerComponent,
    DeploymentRedeployComponent,
    ParameterComponent,
  ],
  providers: [DeploymentService, NgbActiveModal],
  entryComponents: [DeploymentsEditModalComponent],
})
export class DeploymentsModule {}
