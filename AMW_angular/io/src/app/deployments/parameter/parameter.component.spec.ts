import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DeploymentService } from '../deployment/deployment.service';
import { DeploymentParameterService } from '../services/deployment-parameter.service';

import { ParameterComponent } from './parameter.component';

describe('ParameterComponent', () => {
  let component: ParameterComponent;
  let fixture: ComponentFixture<ParameterComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ParameterComponent],
      imports: [HttpClientTestingModule],
      providers: [DeploymentParameterService],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ParameterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
