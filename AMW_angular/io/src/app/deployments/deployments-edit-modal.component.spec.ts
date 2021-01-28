import { DeploymentsEditModalComponent } from './deployments-edit-modal.component';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { DateTimeModel } from '../shared/date-time-picker/date-time.model';
import { Deployment } from './deployment/deployment';

describe('DeploymentsEditModalComponent (with query params)', () => {
  let component: DeploymentsEditModalComponent;
  var activeModal = new NgbActiveModal();
  beforeEach(() => {
    component = new DeploymentsEditModalComponent(activeModal);
  });

  it('should log unknown edit actions on doEdit', () => {
    // given
    component.selectedEditAction = 'test';
    component.deployments = [{ id: 1, selected: true } as Deployment];
    spyOn(console, 'error');
    spyOn(activeModal, 'close');

    // when
    component.doEdit();

    // then
    expect(console.error).toHaveBeenCalled();
    expect(activeModal.close).toHaveBeenCalled();
  });

  it('should apply date for confirmation', () => {
    // given
    const newDeploymentDate = DateTimeModel.fromLocalString('30.11.2017 09:19');

    component.deploymentDate = newDeploymentDate;
    component.selectedEditAction = 'Confirm';
    component.deployments = [
      { id: 1, selected: true, deploymentDate: 5555 } as Deployment,
      { id: 1, selected: true, deploymentDate: 6666 } as Deployment,
    ];
    spyOn(console, 'error');
    spyOn(activeModal, 'close');

    // when
    component.doEdit();

    // then
    const deployment1: Deployment = component.deployments[0];
    const deployment2: Deployment = component.deployments[1];
    expect(deployment1.deploymentDate).toEqual(newDeploymentDate.toEpoch());
    expect(deployment2.deploymentDate).toEqual(newDeploymentDate.toEpoch());
    expect(activeModal.close).toHaveBeenCalled();
  });

  it('should clear data after doEdit()', () => {
    // given
    const newDeploymentDate = DateTimeModel.fromLocalString('30.11.2017 09:19');

    component.deploymentDate = newDeploymentDate;
    component.selectedEditAction = 'Confirm';
    component.deployments = [
      { id: 1, selected: true, deploymentDate: 5555 } as Deployment,
      { id: 1, selected: true, deploymentDate: 6666 } as Deployment,
    ];
    spyOn(activeModal, 'close');
    // when
    component.doEdit();

    // then
    expect(component.confirmationAttributes).toEqual({} as Deployment);
    expect(component.selectedEditAction).toEqual('');
    expect(component.deploymentDate).toEqual(undefined);
    expect(activeModal.close).toHaveBeenCalled();
  });
});
