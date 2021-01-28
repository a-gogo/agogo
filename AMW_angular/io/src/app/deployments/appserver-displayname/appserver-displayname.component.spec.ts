import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AppServerDisplayNameComponent } from './appserver-displayname.component';

describe('AppserverDisplaynameComponent', () => {
  let component: AppServerDisplayNameComponent;
  let fixture: ComponentFixture<AppServerDisplayNameComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [AppServerDisplayNameComponent],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(AppServerDisplayNameComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
