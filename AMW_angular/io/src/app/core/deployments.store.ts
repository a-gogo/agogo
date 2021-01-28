import { Injectable } from '@angular/core';
import { BehaviorSubject, of, Subject } from 'rxjs';
import { Deployment } from '../deployments/deployment/deployment';

@Injectable({ providedIn: 'root' })
export class DeploymentsStore {
  private _selectedDeploymentIds$: BehaviorSubject<Deployment[]> = new BehaviorSubject([]);

  public selectedDeploymentIds$ = this._selectedDeploymentIds$.asObservable();

  constructor() {}

  select(deployment: Deployment) {
    const currentlySelected = this._selectedDeploymentIds$.value;
    this._selectedDeploymentIds$.next([...currentlySelected, deployment]);
  }

  deselect(deployment: Deployment) {
    const alreadySelected = this._selectedDeploymentIds$.value;
    this._selectedDeploymentIds$.next(alreadySelected.filter((d) => d.id !== deployment.id));
  }
}
