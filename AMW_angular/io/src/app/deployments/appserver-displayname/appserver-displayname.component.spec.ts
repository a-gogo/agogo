import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { AppWithVersion } from '../deployment/app-with-version';
import { Deployment } from '../deployment/deployment';

import { AppServerDisplayNameComponent } from './appserver-displayname.component';

describe('AppserverDisplaynameComponent', () => {
  let component: AppServerDisplayNameComponent;
  let fixture: ComponentFixture<AppServerDisplayNameComponent>;

  beforeEach(
    waitForAsync(() => {
      TestBed.configureTestingModule({ declarations: [AppServerDisplayNameComponent] }).compileComponents();
    })
  );

  beforeEach(() => {
    fixture = TestBed.createComponent(AppServerDisplayNameComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the component', () => {
    expect(component).toBeDefined();
  });

  it('should render messeage if deployment is undefined', () => {
    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).toContain('no deployment selected');
  });

  it('should render appserver name and release name for given deployment, without appversions', () => {
    // given
    const deployment: Deployment = { appServerName: 'ApplicationServerName', releaseName: 'Release-1' } as Deployment;
    component.deployment = deployment;

    // when
    fixture.detectChanges();

    // then
    const element: HTMLElement = fixture.nativeElement;
    const appserverWithRelease: HTMLElement = element.querySelector('h5');
    expect(appserverWithRelease.textContent).toContain('ApplicationServerName (Release-1)');
    expect(element.querySelector('h6')).toBeNull();
  });

  it('should render appversions', () => {
    // given
    const appsWithVersion: AppWithVersion[] = [
      { applicationName: 'application-1', version: 'version-1' } as AppWithVersion,
      { applicationName: 'application-2', version: '1.1.0' } as AppWithVersion,
    ];
    const deployment: Deployment = {
      appServerName: 'ApplicationServerName',
      releaseName: 'Release-1',
      appsWithVersion: appsWithVersion,
    } as Deployment;

    component.deployment = deployment;

    // when
    fixture.detectChanges();

    // then
    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('h5').textContent).toBe('ApplicationServerName (Release-1)');
    const appsWithVersionElements: NodeListOf<HTMLHeadingElement> = element.querySelectorAll('h6');
    expect(appsWithVersionElements.length).toBe(2);
    expect(appsWithVersionElements.item(0).textContent).toBe('application-1 (version-1)');
    expect(appsWithVersionElements.item(1).textContent).toBe('application-2 (1.1.0)');
  });
});
