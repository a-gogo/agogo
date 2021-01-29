import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DeploymentParameterService } from '@deployments/services';
import { DeploymentParameter } from '@deployments/types';

@Component({
  selector: 'app-parameter',
  template: `
    <div class="form-group row">
      <label class="col-sm-2 font-weight-bold text-right">Deployment parameter</label>
      <div class="col-sm-2">
        <input class="form-control" list="depParamList" type="text" [(ngModel)]="newDeploymentParameter.key" />
        <datalist id="depParamList">
          <option *ngFor="let deploymentParameter of allDeploymentParameters$ | async">{{
            deploymentParameter.key
          }}</option>
        </datalist>
      </div>
      <div class="col-sm-3">
        <input class="form-control" type="text" [(ngModel)]="newDeploymentParameter.value" />
      </div>
      <div class="col-sm-2">
        <button
          type="button"
          class="btn btn-secondary"
          *ngIf="newDeploymentParameter.key && newDeploymentParameter.value"
          (click)="addParam()"
        >
          <app-icon icon="plus"></app-icon>
        </button>
      </div>
    </div>
  `,
  styles: [],
})
export class ParameterComponent {
  newDeploymentParameter: DeploymentParameter = {} as DeploymentParameter;

  allDeploymentParameters$ = this.deploymentParameterService.allDeploymentParameters$;

  constructor(private deploymentParameterService: DeploymentParameterService) {}

  @Input()
  deploymentParameters: DeploymentParameter[];

  @Output()
  addParamEvent = new EventEmitter<DeploymentParameter>();

  addParam(): void {
    this.addParamEvent.emit(this.newDeploymentParameter);
    this.newDeploymentParameter = {} as DeploymentParameter;
  }
}
