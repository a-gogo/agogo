import { Component, Input, Output, EventEmitter } from '@angular/core';
import { ResourceService } from '../resource/resource.service';
import * as _ from 'lodash';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { DATE_FORMAT_ANGULAR } from '../core/amw-constants';
import { DeploymentsStore } from '../core/deployments.store';
import { Deployment } from './deployment/deployment';
import { DeploymentFilter } from './deployment/deployment-filter';
import { DateTimeModel } from '../shared/components';

@Component({
  selector: 'amw-deployments-list',
  templateUrl: './deployments-list.component.html',
})
export class DeploymentsListComponent {
  @Input() deployments: Deployment[] = [];
  @Input() sortCol: string;
  @Input() sortDirection: string;
  @Input() filtersForParam: DeploymentFilter[];
  @Output() editDeploymentDate: EventEmitter<Deployment> = new EventEmitter<Deployment>();
  @Output() selectAllDeployments: EventEmitter<boolean> = new EventEmitter<boolean>();
  @Output() doCancelDeployment: EventEmitter<Deployment> = new EventEmitter<Deployment>();
  @Output() doRejectDeployment: EventEmitter<Deployment> = new EventEmitter<Deployment>();
  @Output() doConfirmDeployment: EventEmitter<Deployment> = new EventEmitter<Deployment>();
  @Output() doSort: EventEmitter<string> = new EventEmitter<string>();

  deployment: Deployment;
  deploymentDate: DateTimeModel = new DateTimeModel();
  hasPermissionShakedownTest: boolean = null;
  allSelected: boolean = false;
  // TODO: show this error somewhere?
  errorMessage = '';
  dateFormat = DATE_FORMAT_ANGULAR;

  failureReason: { [key: string]: string } = {
    PRE_DEPLOYMENT_GENERATION: 'pre deployment generation failed',
    DEPLOYMENT_GENERATION: 'deployment generation failed',
    PRE_DEPLOYMENT_SCRIPT: 'pre deployment script failed',
    DEPLOYMENT_SCRIPT: 'deployment script failed',
    NODE_MISSING: 'no nodes enabled',
    TIMEOUT: 'timeout',
    UNEXPECTED_ERROR: 'unexpected error',
    RUNTIME_ERROR: 'runtime error',
  };

  constructor(
    public deploymentsStore: DeploymentsStore,
    private resourceService: ResourceService,
    private modalService: NgbModal
  ) {}

  ngOnInit() {}

  showDetails(content, deploymentId: number) {
    this.deployment = _.find(this.deployments, ['id', deploymentId]);
    this.modalService.open(content);
  }

  showDateChange(content, deploymentId: number) {
    this.deployment = _.find(this.deployments, ['id', deploymentId]);
    this.deploymentDate = DateTimeModel.fromEpoch(this.deployment.deploymentDate);
    this.modalService.open(content).result.then(
      (result) => {
        this.deployment.deploymentDate = this.deploymentDate.toEpoch();
        this.editDeploymentDate.emit(this.deployment);
        delete this.deploymentDate;
      },
      (reason) => {
        delete this.deploymentDate;
      }
    );
  }

  showConfirm(content, deploymentId: number) {
    this.deployment = _.find(this.deployments, ['id', deploymentId]);
    this.resourceService.canCreateShakedownTest(this.deployment.appServerId).subscribe(
      /* happy path */ (r) => (this.hasPermissionShakedownTest = r),
      /* error path */ (e) => (this.errorMessage = e),
      /* onComplete */ () =>
        this.modalService.open(content).result.then(
          (result) => {
            this.doConfirmDeployment.emit(this.deployment);
          },
          (reason) => {}
        )
    );
  }

  showReject(content, deploymentId: number) {
    this.deployment = _.find(this.deployments, ['id', deploymentId]);
    this.modalService.open(content).result.then(
      (result) => {
        this.doRejectDeployment.emit(this.deployment);
      },
      (reason) => {}
    );
  }

  showCancel(content, deploymentId: number) {
    this.deployment = _.find(this.deployments, ['id', deploymentId]);
    this.modalService.open(content).result.then(
      (result) => {
        this.doCancelDeployment.emit(this.deployment);
      },
      (reason) => {}
    );
  }

  reSort(col: string) {
    this.doSort.emit(col);
  }

  switchAllDeployments() {
    this.allSelected = !this.allSelected;
    this.selectAllDeployments.emit(this.allSelected);
  }

  appServerLink(appServerId: number) {
    if (appServerId) {
      window.location.href = '/AMW_web/pages/editResourceView.xhtml?id=' + appServerId + '&ctx=1';
    }
  }

  appLink(appId: number) {
    this.resourceService.resourceExists(appId).subscribe(
      /* happy path */ (r) => {
        if (r) {
          window.location.href = '/AMW_web/pages/editResourceView.xhtml?id=' + appId + '&ctx=1';
        }
      }
    );
  }

  toggle(deployment: Deployment) {
    if (deployment.selected) {
      this.deploymentsStore.deselect(deployment);
    } else {
      this.deploymentsStore.select(deployment);
    }
  }
}
