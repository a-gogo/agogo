import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DeploymentsComponent } from './deployments.component';
import { DeploymentsListComponent } from './deployments-list.component';
import { SharedModule } from '../shared/shared.module';
import { DeploymentsEditModalComponent } from './deployments-edit-modal.component';
import { DeploymentsRoutingModule } from './deployments-routing.module';
import { DeploymentContainerComponent } from './deployment-container/deployment-container.component';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { CodemirrorModule } from '@ctrl/ngx-codemirror';
import { DeploymentLogsComponent } from './logs/deployment-logs.component';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { DeploymentComponent } from './deployment/deployment.component';
import { DeploymentService } from './deployment/deployment.service';
import { EnvironmentService } from './deployment/environment.service';
import { CoreModule } from '../core/core.module';

@NgModule({
  imports: [CommonModule, CoreModule, DeploymentsRoutingModule, SharedModule, NgbModule, CodemirrorModule],
  declarations: [
    DeploymentComponent,
    DeploymentsComponent,
    DeploymentsListComponent,
    DeploymentsEditModalComponent,
    DeploymentLogsComponent,
    DeploymentContainerComponent,
  ],
  providers: [DeploymentService, EnvironmentService, NgbActiveModal],
  entryComponents: [DeploymentsEditModalComponent],
})
export class DeploymentsModule {}
