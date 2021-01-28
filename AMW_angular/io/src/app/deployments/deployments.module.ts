import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DeploymentsComponent } from './deployments.component';
import { DeploymentsListComponent } from './deployments-list.component';
import { FormsModule } from '@angular/forms';
import { SharedModule } from '../shared/shared.module';
import { DeploymentsEditModalComponent } from './deployments-edit-modal.component';
import { DeploymentService } from '../deployment/deployment.service';
import { EnvironmentService } from '../deployment/environment.service';
import { DeploymentsRoutingModule } from './deployments-routing.module';
import { DeploymentContainerComponent } from './deployment-container/deployment-container.component';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { CodemirrorModule } from '@ctrl/ngx-codemirror';
import { DeploymentLogsComponent } from './logs/deployment-logs.component';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { DeploymentsStore } from './deployment-container/deployments.store';

@NgModule({
  imports: [CommonModule, FormsModule, DeploymentsRoutingModule, SharedModule, NgbModule, CodemirrorModule],
  declarations: [
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
