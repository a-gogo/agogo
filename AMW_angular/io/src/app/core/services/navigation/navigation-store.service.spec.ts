import { TestBed } from '@angular/core/testing';
import { NavigationStoreService } from '..';

describe('NavigationStoreService', () => {
  let service: NavigationStoreService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(NavigationStoreService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
