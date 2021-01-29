import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DeploymentRedeployComponent } from './deployment-redeploy.component';

xdescribe('DeploymentRedeployComponent', () => {
  let component: DeploymentRedeployComponent;
  let fixture: ComponentFixture<DeploymentRedeployComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [DeploymentRedeployComponent],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DeploymentRedeployComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
