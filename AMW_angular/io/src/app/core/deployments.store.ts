import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Deployment } from '../deployments/deployment/deployment';

@Injectable({ providedIn: 'root' })
export class DeploymentsStore {
  private _selectedDeployments$: BehaviorSubject<Deployment[]> = new BehaviorSubject([]);

  public selectedDeployments$ = this._selectedDeployments$.asObservable();

  // TODO: would be nice if I could get rid of this
  clear(): void {
    this._selectedDeployments$.next([]);
  }

  select(deployment: Deployment): void {
    const currentlySelected = this._selectedDeployments$.value;
    const selected = [...new Set([...currentlySelected, deployment])];

    this._selectedDeployments$.next(selected);
  }

  deselect(deployment: Deployment): void {
    const alreadySelected = this._selectedDeployments$.value;
    this._selectedDeployments$.next(alreadySelected.filter((d) => d.id !== deployment.id));
  }
}
