import { Component, Input, Output, EventEmitter, NgZone } from '@angular/core';
import { Deployment } from './deployment';
import { DeploymentDetail } from './deployment-detail';
import { DeploymentService } from './deployment.service';
import { Datetimepicker } from 'eonasdan-bootstrap-datetimepicker';
import * as _ from 'lodash';
import * as moment from 'moment';
import {ResourceService} from "../resource/resource.service";

declare var $: any;

@Component({
  selector: 'amw-deployments-list',
  templateUrl: './deployments-list.component.html'
})

export class DeploymentsListComponent {

  @Input() deployments: Deployment[] = [];
  @Output() editDeploymentDate: EventEmitter<Deployment> = new EventEmitter<Deployment>();
  @Output() selectAllDeployments: EventEmitter<boolean> = new EventEmitter<boolean>();
  @Output() doCancelDeployment: EventEmitter<Deployment> = new EventEmitter<Deployment>();
  @Output() doRejectDeployment: EventEmitter<Deployment> = new EventEmitter<Deployment>();
  @Output() doConfirmDeployment: EventEmitter<DeploymentDetail> = new EventEmitter<DeploymentDetail>();

  deployment: Deployment;

  deploymentDate: number;

  deploymentDetail: DeploymentDetail;

  hasPermissionShakedownTest: boolean = null;

  errorMessage: string = '';

  allSelected: boolean = false;

  constructor(private ngZone: NgZone,
              private deploymentService: DeploymentService,
              private resourceService: ResourceService) {
  }

  showDetails(deploymentId: number) {
    delete this.deploymentDetail;
    this.deployment = _.find(this.deployments, ['id', deploymentId]);
    this.getDeploymentDetail(deploymentId);
    $('#deploymentDetails').modal('show');
  }

  showDateChange(deploymentId: number) {
    this.deployment = _.find(this.deployments, ['id', deploymentId]);
    $('#deploymentDateChange').modal('show');
    this.ngZone.onMicrotaskEmpty.first().subscribe(() => {
      $('.datepicker').datetimepicker({format: 'DD.MM.YYYY HH:mm'});
    });
  }

  showConfirm(deploymentId: number) {
    delete this.deploymentDetail;
    this.deployment = _.find(this.deployments, ['id', deploymentId]);
    if (this.hasPermissionShakedownTest == null) {
      this.resourceService.canCreateShakedownTest(this.deployment.appServerId).subscribe(
        /* happy path */ (r) => this.hasPermissionShakedownTest = r,
        /* error path */ (e) => this.errorMessage = e);
    }
    this.deploymentService.getDeploymentDetail(deploymentId).subscribe(
      /* happy path */ (r) => this.deploymentDetail = r,
      /* error path */ (e) => this.errorMessage = e,
                        () => $('#deploymentConfirmation').modal('show'));
  }

  showReject(deploymentId: number) {
    this.deployment = _.find(this.deployments, ['id', deploymentId]);
    $('#deploymentRejection').modal('show');
  }

  showCancel(deploymentId: number) {
    this.deployment = _.find(this.deployments, ['id', deploymentId]);
    $('#deploymentCancelation').modal('show');
  }

  doDateChange() {
    if (this.deployment) {
      this.errorMessage = '';
      let dateTime = moment(this.deploymentDate, 'DD.MM.YYYY hh:mm');
      if (!dateTime || !dateTime.isValid()) {
        this.errorMessage = 'Invalid date';
      } else {
        this.deployment.deploymentDate = dateTime.valueOf();
        this.editDeploymentDate.emit(this.deployment);
        $('#deploymentDateChange').modal('hide');
        delete this.deployment;
        delete this.deploymentDate;
      }
    }
  }

  doReject() {
    if (this.deployment) {
      this.doRejectDeployment.emit(this.deployment);
      $('#deploymentRejection').modal('hide');
      delete this.deployment;
    }
  }

  doCancel() {
    if (this.deployment) {
      this.doCancelDeployment.emit(this.deployment);
      $('#deploymentCancelation').modal('hide');
      delete this.deployment;
    }
  }

  doConfirm() {
    if (this.deployment && this.deploymentDetail) {
      this.doConfirmDeployment.emit(this.deploymentDetail);
      $('#deploymentConfirmation').modal('hide');
      delete this.deployment;
      delete this.deploymentDetail;
    }
  }

  switchAllDeployments() {
    this.allSelected = !this.allSelected;
    this.selectAllDeployments.emit(this.allSelected);
  }

  private getDeploymentDetail(deploymentId: number) {
    this.deploymentService.getDeploymentDetail(deploymentId).subscribe(
      /* happy path */ (r) => this.deploymentDetail = r,
      /* error path */ (e) => this.errorMessage = e);
  }

}
