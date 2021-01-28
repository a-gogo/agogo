import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { NavigationItem } from './navigation-item';

export interface Navigation {
  pageTitle: string;
  current: string;
  items: NavigationItem[];
  logoutUrl: string;
  visible: boolean;
}

const initial = {
  pageTitle: '',
  current: '',
  items: [],
  logoutUrl: '',
  visible: false,
};

@Injectable({
  providedIn: 'root',
})
export class NavigationService {
  private readonly _navigation = new BehaviorSubject<Navigation>(initial);

  readonly navigation$ = this._navigation.asObservable();

  get navigation(): Navigation {
    return this._navigation.getValue();
  }

  set navigation(nav: Navigation) {
    this._navigation.next(nav);
  }

  setPageTitle(pageTitle: string): void {
    this.navigation = { ...this.navigation, pageTitle };
  }

  setCurrent(current: string): void {
    this.navigation = { ...this.navigation, current };
  }

  setItems(items: NavigationItem[]): void {
    this.navigation = { ...this.navigation, items };
  }

  setLogoutUrl(logoutUrl: string): void {
    this.navigation = { ...this.navigation, logoutUrl };
  }

  setVisible(visible: boolean): void {
    this.navigation = { ...this.navigation, visible };
  }
}
