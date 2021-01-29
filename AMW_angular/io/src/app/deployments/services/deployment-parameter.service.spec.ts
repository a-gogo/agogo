import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DeploymentParameterService } from './deployment-parameter.service';

describe('DeploymentParameterService', () => {
  let service: DeploymentParameterService;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(DeploymentParameterService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
